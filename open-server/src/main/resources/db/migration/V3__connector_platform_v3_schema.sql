-- ============================================================================
-- 连接器平台 V3 数据库 Schema 迁移
-- 版本: V3
-- 创建日期: 2026-06-22
-- 规范版本: spec.md v3.0 / plan-db.md v2.0
-- 设计原则:
--   - V3 多版本模型: connector_t/flow_t 1:N connector_version_t/flow_version_t
--   - 移除 1:1 约束 (idx_connector_id / idx_flow_id)
--   - 新增连接器版本引用中间表 (connector_version_ref_t)
--   - 启用执行记录和执行步骤表 (V3 新增)
--   - 所有枚举: TINYINT(10) + COMMENT 注释数字→含义映射
--   - 无物理外键
--   - 审计字段: create_time, last_update_time, create_by, last_update_by
--   - 幂等设计: 所有 DDL 通过存储过程安全判断，支持重复执行不报错
-- ============================================================================

-- ============================================================================
-- 第 0 部分: 公共存储过程（幂等 DDL 安全执行）
--   同一类逻辑抽取公共存储过程，通过 information_schema 判断对象是否存在
--   满足条件才执行，不满足则跳过，实现无条件可重复执行
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 0.1 safe_add_column: 列不存在时才添加
--     参数: p_table=表名, p_column=列名, p_definition=列定义(类型+约束+COMMENT)
-- ----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS safe_add_column;
DELIMITER $$
CREATE PROCEDURE safe_add_column(
    IN p_table  VARCHAR(128),
    IN p_column VARCHAR(128),
    IN p_definition TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = p_table
          AND column_name = p_column
    ) THEN
        SET @sql = CONCAT('ALTER TABLE `', p_table, '` ADD COLUMN `', p_column, '` ', p_definition);
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

-- ----------------------------------------------------------------------------
-- 0.2 safe_modify_column: 列存在时才修改
--     参数: p_table=表名, p_column=列名, p_definition=列定义(类型+约束+COMMENT)
-- ----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS safe_modify_column;
DELIMITER $$
CREATE PROCEDURE safe_modify_column(
    IN p_table  VARCHAR(128),
    IN p_column VARCHAR(128),
    IN p_definition TEXT
)
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = p_table
          AND column_name = p_column
    ) THEN
        SET @sql = CONCAT('ALTER TABLE `', p_table, '` MODIFY COLUMN `', p_column, '` ', p_definition);
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

-- ----------------------------------------------------------------------------
-- 0.3 safe_add_index: 索引不存在时才添加（支持普通索引和唯一索引）
--     参数: p_table=表名, p_index=索引名, p_index_type='INDEX'|'UNIQUE KEY',
--           p_columns=列定义如'(app_id, status)', p_comment=注释或NULL
-- ----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS safe_add_index;
DELIMITER $$
CREATE PROCEDURE safe_add_index(
    IN p_table      VARCHAR(128),
    IN p_index      VARCHAR(128),
    IN p_index_type VARCHAR(20),
    IN p_columns    TEXT,
    IN p_comment    VARCHAR(500)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = p_table
          AND index_name = p_index
    ) THEN
        IF p_comment IS NOT NULL AND p_comment != '' THEN
            SET @sql = CONCAT('ALTER TABLE `', p_table, '` ADD ', p_index_type, ' `', p_index, '` ', p_columns, ' COMMENT ''', p_comment, '''');
        ELSE
            SET @sql = CONCAT('ALTER TABLE `', p_table, '` ADD ', p_index_type, ' `', p_index, '` ', p_columns);
        END IF;
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

-- ----------------------------------------------------------------------------
-- 0.4 safe_drop_index: 索引存在时才删除
--     参数: p_table=表名, p_index=索引名
-- ----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS safe_drop_index;
DELIMITER $$
CREATE PROCEDURE safe_drop_index(
    IN p_table VARCHAR(128),
    IN p_index VARCHAR(128)
)
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = p_table
          AND index_name = p_index
    ) THEN
        SET @sql = CONCAT('ALTER TABLE `', p_table, '` DROP INDEX `', p_index, '`');
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;


-- ============================================================================
-- 第 1 部分: 已有表结构变更 (5 张表 / 32 条 DDL)
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1.1 connector_t: 新增 app_id，启用 status 4 状态
-- ----------------------------------------------------------------------------
CALL safe_add_column('openplatform_v2_cp_connector_t', 'app_id', 'BIGINT(20) NOT NULL DEFAULT 0 COMMENT ''归属应用ID（0=全局，迁移前数据默认0）''');
CALL safe_modify_column('openplatform_v2_cp_connector_t', 'status', 'TINYINT(10) NOT NULL DEFAULT 1 COMMENT ''状态：1=有效不可用（无已发布版本）, 2=有效可用（有已发布版本）, 3=已失效, 4=物理删除''');
CALL safe_add_index('openplatform_v2_cp_connector_t', 'idx_app_status', 'INDEX', '(app_id, status)', NULL);
CALL safe_add_index('openplatform_v2_cp_connector_t', 'idx_app_name_cn', 'INDEX', '(app_id, name_cn)', '按应用+中文名称查询');
CALL safe_add_index('openplatform_v2_cp_connector_t', 'idx_app_name_en', 'INDEX', '(app_id, name_en)', '按应用+英文名称查询');

-- ----------------------------------------------------------------------------
-- 1.2 connector_version_t: 移除 1:1 约束，新增版本号/状态/发布时间字段，connection_config 改为可空（草稿无需配置）
-- ----------------------------------------------------------------------------
CALL safe_drop_index('openplatform_v2_cp_connector_version_t', 'idx_connector_id');
CALL safe_modify_column('openplatform_v2_cp_connector_version_t', 'connection_config', 'MEDIUMTEXT NULL COMMENT ''连接配置JSON（V3多版本：草稿可为空，发布时必填）''');
CALL safe_add_column('openplatform_v2_cp_connector_version_t', 'version_number', 'INT NOT NULL DEFAULT 1 COMMENT ''版本号，实体内从1递增''');
CALL safe_add_column('openplatform_v2_cp_connector_version_t', 'status', 'TINYINT(10) NOT NULL DEFAULT 1 COMMENT ''状态：1=草稿, 2=已发布, 3=已失效, 4=物理删除''');
CALL safe_add_column('openplatform_v2_cp_connector_version_t', 'published_time', 'DATETIME(3) NULL COMMENT ''发布时间（首次发布时刻）''');
CALL safe_add_column('openplatform_v2_cp_connector_version_t', 'published_by', 'VARCHAR(100) NULL COMMENT ''发布人（发布操作人）''');
CALL safe_add_index('openplatform_v2_cp_connector_version_t', 'idx_connector_version', 'INDEX', '(connector_id, version_number)', NULL);
CALL safe_add_index('openplatform_v2_cp_connector_version_t', 'idx_connector_status', 'INDEX', '(connector_id, status)', NULL);

-- ----------------------------------------------------------------------------
-- 1.3 flow_t: 新增部署版本指针/app_id，扩展 lifecycle_status 4 状态
-- ----------------------------------------------------------------------------
CALL safe_add_column('openplatform_v2_cp_flow_t', 'deployed_version_id', 'BIGINT(20) NULL COMMENT ''当前部署的版本ID（运行时按此指针读取编排快照）''');
CALL safe_add_column('openplatform_v2_cp_flow_t', 'deployed_version_number', 'INT NULL COMMENT ''当前部署的版本号（冗余，避免列表查询 JOIN flow_version_t）''');
CALL safe_add_column('openplatform_v2_cp_flow_t', 'app_id', 'BIGINT(20) NOT NULL DEFAULT 0 COMMENT ''归属应用ID''');
CALL safe_modify_column('openplatform_v2_cp_flow_t', 'lifecycle_status', 'TINYINT(10) NOT NULL DEFAULT 1 COMMENT ''生命周期：1=已停止, 2=运行中, 3=已失效, 4=物理删除''');
CALL safe_add_index('openplatform_v2_cp_flow_t', 'idx_deployed_version', 'INDEX', '(deployed_version_id)', NULL);
CALL safe_add_index('openplatform_v2_cp_flow_t', 'idx_app_status', 'INDEX', '(app_id, lifecycle_status)', NULL);
CALL safe_add_index('openplatform_v2_cp_flow_t', 'idx_app_name_cn', 'INDEX', '(app_id, name_cn)', '按应用+中文名称查询');
CALL safe_add_index('openplatform_v2_cp_flow_t', 'idx_app_name_en', 'INDEX', '(app_id, name_en)', '按应用+英文名称查询');

-- ----------------------------------------------------------------------------
-- 1.4 flow_version_t: 移除 1:1 约束，新增版本号/7状态/发布时间字段，orchestration_config 改为可空（草稿无需编排）
-- ----------------------------------------------------------------------------
CALL safe_drop_index('openplatform_v2_cp_flow_version_t', 'idx_flow_id');
CALL safe_modify_column('openplatform_v2_cp_flow_version_t', 'orchestration_config', 'MEDIUMTEXT NULL COMMENT ''编排配置JSON（V3多版本：草稿可为空，发布时必填）''');
CALL safe_add_column('openplatform_v2_cp_flow_version_t', 'version_number', 'INT NOT NULL DEFAULT 1 COMMENT ''版本号，实体内从1递增''');
CALL safe_add_column('openplatform_v2_cp_flow_version_t', 'status', 'TINYINT(10) NOT NULL DEFAULT 1 COMMENT ''状态：1=草稿, 2=待审批, 3=已撤回, 4=已驳回, 5=已发布, 6=已失效, 7=物理删除''');
CALL safe_add_column('openplatform_v2_cp_flow_version_t', 'published_time', 'DATETIME(3) NULL COMMENT ''发布时间（审批通过的时刻）''');
CALL safe_add_column('openplatform_v2_cp_flow_version_t', 'published_by', 'VARCHAR(100) NULL COMMENT ''发布人（提交审批的人）''');
CALL safe_add_index('openplatform_v2_cp_flow_version_t', 'idx_flow_version', 'INDEX', '(flow_id, version_number)', NULL);
CALL safe_add_index('openplatform_v2_cp_flow_version_t', 'idx_flow_status', 'INDEX', '(flow_id, status)', NULL);

-- ----------------------------------------------------------------------------
-- 1.5 approval_flow_t: 新增 app_id，uk_code → uk_code_app
-- ----------------------------------------------------------------------------
CALL safe_drop_index('openplatform_v2_approval_flow_t', 'uk_code');
CALL safe_add_column('openplatform_v2_approval_flow_t', 'app_id', 'BIGINT(20) NULL COMMENT ''应用ID（NULL=全局配置，非NULL=指定应用配置）''');
CALL safe_add_index('openplatform_v2_approval_flow_t', 'uk_code_app', 'UNIQUE KEY', '(code, app_id)', NULL);

-- ============================================================================
-- 第 2 部分: 新建表 (3 CREATE) — CREATE TABLE IF NOT EXISTS 天然幂等
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 2.1 连接器版本引用中间表 (connector_version_ref_t)
--     编排中 connector 节点引用特定 ConnectorVersion
--     用于「标记版本失效/删除」的前置「被引用」校验
--     flow_id/connector_id 冗余避免 JOIN 穿透版本表
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS openplatform_v2_cp_connector_version_ref_t (
    id BIGINT(20) NOT NULL COMMENT '雪花ID',
    flow_id BIGINT(20) NOT NULL COMMENT '连接流ID（冗余，避免 JOIN flow_version_t）',
    flow_version_id BIGINT(20) NOT NULL COMMENT '连接流版本ID',
    node_id VARCHAR(64) NOT NULL COMMENT '流程编排中的连接器节点ID（React Flow node.id）',
    connector_id BIGINT(20) NOT NULL COMMENT '连接器ID（冗余，避免 JOIN connector_version_t）',
    connector_version_id BIGINT(20) NOT NULL COMMENT '连接器版本ID',
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    last_update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    create_by VARCHAR(100) NOT NULL DEFAULT '' COMMENT '创建人账号',
    last_update_by VARCHAR(100) NOT NULL DEFAULT '' COMMENT '更新人账号',
    PRIMARY KEY (id),
    INDEX idx_flow_version_node (flow_version_id, node_id) COMMENT '按流版本+节点ID定位唯一引用',
    INDEX idx_flow (flow_id) COMMENT '按连接流查询其全部引用',
    INDEX idx_connector_version_flow_ver (connector_version_id, flow_id, flow_version_id) COMMENT '核心：按连接器版本查询哪些连接流的哪些版本引用了它',
    INDEX idx_connector_flow_ver (connector_id, flow_id, flow_version_id) COMMENT '核心：按连接器查询哪些连接流的哪些版本引用了它'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='连接器版本引用中间表（M:N，编排保存时同步维护）';

-- ----------------------------------------------------------------------------
-- 2.2 执行记录表 (execution_record_t)
--     V3 全新启用，记录每次连接流执行的元数据
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS openplatform_v2_cp_execution_record_t (
    id                      BIGINT(20)   NOT NULL COMMENT '雪花ID (应用层生成)',
    app_id                  BIGINT(20)   NOT NULL DEFAULT 0 COMMENT '归属应用ID（与连接流 app_id 一致，冗余避免 JOIN）',
    flow_id                 BIGINT(20)   NOT NULL COMMENT '关联连接流ID',
    flow_version_id         BIGINT(20)   DEFAULT NULL COMMENT '关联连接流版本ID',
    flow_version_number     INT          DEFAULT NULL COMMENT '关联连接流版本号（冗余，避免列表 JOIN）',
    flow_version_snapshot   MEDIUMTEXT   DEFAULT NULL COMMENT '执行时版本完整快照JSON（orchestrationConfig + flowConfig），版本删除后仍可还原执行现场',
    flow_name_cn            VARCHAR(128) NOT NULL COMMENT '连接流中文名称（触发时快照）',
    flow_name_en            VARCHAR(128) NOT NULL COMMENT '连接流英文名称（触发时快照）',
    trigger_type            TINYINT(10)  NOT NULL DEFAULT 1 COMMENT '触发方式：1=http（HTTP触发）, 2=debug（调试触发）',
    trigger_account         VARCHAR(100) DEFAULT NULL COMMENT '触发账号（HTTP=调用方凭证标识）',
    status                  TINYINT(10)  NOT NULL DEFAULT 0 COMMENT '执行状态：0=success, 1=failed',
    rate_limit_status       TINYINT(10)  NOT NULL DEFAULT 0 COMMENT '限流状态：0=未触发限流, 1=触发限流（429）',
    cache_status            TINYINT(10)  NOT NULL DEFAULT 0 COMMENT '缓存状态：0=未命中（正常执行）, 1=全流命中, 2=部分命中（V3）',
    cache_key               VARCHAR(500) DEFAULT NULL COMMENT '命中的缓存键（全流命中时有值，调试用）',
    cache_ttl_remaining     INT          DEFAULT NULL COMMENT '命中时缓存剩余 TTL（秒）',
    error_code              VARCHAR(20)  DEFAULT NULL COMMENT '错误码（4xx=下游客户端, 5xx=下游服务端, 6xxxx=引擎内部）',
    error_message           VARCHAR(1000) DEFAULT NULL COMMENT '错误信息（整体摘要，节点级详情在 execution_step_t）',
    duration_ms             INT(11)      DEFAULT NULL COMMENT '总执行耗时(毫秒)',
    trigger_time            DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '触发时间',
    create_time             DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    last_update_time        DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '最后更新时间',
    create_by               VARCHAR(100) NOT NULL DEFAULT 'SYSTEM' COMMENT '创建人（系统自动生成）',
    last_update_by          VARCHAR(100) NOT NULL DEFAULT 'SYSTEM' COMMENT '最后更新人（系统自动生成）',
    PRIMARY KEY (id),
    INDEX idx_app_id_status          (app_id, id, status)          COMMENT '核心：按应用+执行记录+状态',
    INDEX idx_app_flow_status        (app_id, flow_id, status)     COMMENT '核心：按应用+连接流+状态',
    INDEX idx_app_flow_name_cn_status (app_id, flow_name_cn, status) COMMENT '核心：按应用+连接流中文名称+状态',
    INDEX idx_app_flow_name_en_status (app_id, flow_name_en, status) COMMENT '核心：按应用+连接流英文名称+状态',
    INDEX idx_app_status             (app_id, status)               COMMENT '核心：按应用+状态',
    INDEX idx_trigger_time           (trigger_time)                   COMMENT '定时清理时按时间范围扫描',
    INDEX idx_flow_id                (flow_id)                        COMMENT '按连接流ID查运行记录'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='执行记录表';

-- ----------------------------------------------------------------------------
-- 2.3 执行步骤详情表 (execution_step_t)
--     V3 全新启用，node_type VARCHAR→TINYINT
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS openplatform_v2_cp_execution_step_t (
    id                BIGINT(20)   NOT NULL COMMENT '雪花ID (应用层生成)',
    execution_id      BIGINT(20)   NOT NULL COMMENT '关联执行记录ID',
    node_id           VARCHAR(64)  NOT NULL COMMENT '节点ID（对应 flow_version_snapshot.nodes[].id）',
    node_type         TINYINT(10)  NOT NULL COMMENT '节点类型：1=trigger, 2=connector, 3=script, 4=parallel, 5=exit',
    node_label_cn     VARCHAR(128) DEFAULT NULL COMMENT '节点中文名称 (执行时快照)',
    node_label_en     VARCHAR(128) DEFAULT NULL COMMENT '节点英文名称 (执行时快照)',
    iteration         INT(11)      NOT NULL DEFAULT 0 COMMENT '循环轮次（0=首次或非循环，>0=第N轮循环）',
    status            TINYINT(10)  NOT NULL DEFAULT 0 COMMENT '步骤状态：0=success, 1=failed',
    cache_status      TINYINT(10)  NOT NULL DEFAULT 0 COMMENT '节点级缓存状态：0=未命中（正常执行）, 1=节点级命中（V3 启用，V3 始终为0）',
    cache_key         VARCHAR(500) DEFAULT NULL COMMENT '命中的节点级缓存键（V3 启用，调试用）',
    cache_ttl_remaining INT        DEFAULT NULL COMMENT '命中时缓存剩余 TTL（秒，V3 启用）',
    input_data        MEDIUMTEXT   DEFAULT NULL COMMENT '步骤输入数据JSON',
    output_data       MEDIUMTEXT   DEFAULT NULL COMMENT '步骤输出数据JSON',
    error_message     TEXT         DEFAULT NULL COMMENT '步骤错误信息',
    error_code        VARCHAR(20)  DEFAULT NULL COMMENT '错误码（4xx=下游客户端, 5xx=下游服务端, 6xxxx=引擎内部）',
    duration_ms       INT(11)      DEFAULT NULL COMMENT '步骤耗时(毫秒)',
    create_time       DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    last_update_time  DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '最后更新时间',
    create_by         VARCHAR(100) NOT NULL DEFAULT 'SYSTEM' COMMENT '创建人（系统自动生成）',
    last_update_by    VARCHAR(100) NOT NULL DEFAULT 'SYSTEM' COMMENT '最后更新人（系统自动生成）',
    PRIMARY KEY (id),
    INDEX idx_execution_id (execution_id) COMMENT '按执行记录ID查询全部步骤'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='执行步骤详情表';

-- ============================================================================
-- 第 3 部分: 清理存储过程
-- ============================================================================
DROP PROCEDURE IF EXISTS safe_add_column;
DROP PROCEDURE IF EXISTS safe_modify_column;
DROP PROCEDURE IF EXISTS safe_add_index;
DROP PROCEDURE IF EXISTS safe_drop_index;

-- ============================================================================
-- 迁移完成标记
-- ============================================================================
-- 连接器平台 V3 Schema 迁移完成（幂等版，支持重复执行）
-- 变更汇总:
--   公共存储过程 (4): safe_add_column, safe_modify_column, safe_add_index, safe_drop_index
--   ALTER (5 表 / 32 条): connector_t(5), connector_version_t(8), flow_t(8), flow_version_t(8), approval_flow_t(3)
--   CREATE (3): connector_version_ref_t, execution_record_t, execution_step_t
-- ============================================================================
