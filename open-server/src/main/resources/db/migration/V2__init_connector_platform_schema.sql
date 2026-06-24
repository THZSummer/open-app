-- ============================================================================
-- 连接器平台 (Connector Platform) DDL
-- 版本: V2 (V1 = 能力开放平台基础表)
-- 创建日期: 2026-05-24
-- 规范版本: spec.md v5.0 / plan-db.md v2.8.1
-- 设计原则:
--   - 所有表前缀: openplatform_v2_cp_ (openplatform=开放平台 / v2=第二代 / cp=connector platform)
--   - 所有表后缀: _t
--   - 主键: BIGINT(20) 雪花ID (应用层生成, 非自增)
--   - 无物理外键 (逻辑外键)
--   - 4审计字段: create_time, last_update_time, create_by, last_update_by
--   - 枚举: TINYINT(10)
--   - 时间精度: DATETIME(3)
--   - 索引命名: idx_xxx / uk_xxx
--   - 双语字段: name_cn, name_en, description_cn, description_en
--   - V1预留表: 定义保留, MVP不写入
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1. 连接器基本信息表 (Connector)
--    存储连接器的基本信息 (名称、图标、描述、类型)
--    连接器 = 纯出站端点定义, 类比 import 的模块/库
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `openplatform_v2_cp_connector_t` (
    -- 主键
    `id`                BIGINT(20)   NOT NULL COMMENT '雪花ID (应用层生成)',

    -- 业务字段
    `name_cn`           VARCHAR(128) NOT NULL COMMENT '中文名称',
    `name_en`           VARCHAR(128) NOT NULL COMMENT '英文名称',
    `description_cn`    VARCHAR(512)  DEFAULT NULL COMMENT '中文描述',
    `description_en`    VARCHAR(512)  DEFAULT NULL COMMENT '英文描述',
    `icon_file_id`      VARCHAR(128)  DEFAULT NULL COMMENT '图标文件ID',
    `connector_type`    TINYINT(10)  NOT NULL DEFAULT 1 COMMENT '连接器类型: 1=HTTP (MVP仅支持HTTP)',
    `status`            TINYINT(10)  NOT NULL DEFAULT 0 COMMENT '状态: 预留字段 (MVP不使用)',

    -- 审计字段 (4个)
    `create_time`       DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `last_update_time`  DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '最后更新时间',
    `create_by`         VARCHAR(50)  NOT NULL COMMENT '创建人',
    `last_update_by`    VARCHAR(50)  NOT NULL COMMENT '最后更新人',

    -- 主键约束
    PRIMARY KEY (`id`),

    -- 索引
    INDEX `idx_name_cn`            (`name_cn`)             COMMENT '按中文名称搜索',
    INDEX `idx_name_en`            (`name_en`)             COMMENT '按英文名称搜索',
    INDEX `idx_connector_type`     (`connector_type`)      COMMENT '按类型过滤',
    INDEX `idx_create_time`        (`create_time`)         COMMENT '按创建时间排序',
    INDEX `idx_last_update_time`   (`last_update_time`)    COMMENT '按更新时间排序'

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='连接器基本信息表 - Connector Platform';


-- ----------------------------------------------------------------------------
-- 2. 连接器版本/配置表 (Connector Version)
--    1:1 关联 connector_t, 存储连接配置 JSON
--    MVP单版本模型: 每 connector 仅一条记录, 编辑即生效
--    连接配置: 协议类型/地址/认证类型Schema/入参Schema/出参Schema/超时/限流
--    注意: 仅声明认证类型Schema (含sensitive标记), 不存储凭证值
--    无物理外键 (逻辑外键 connector_id)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `openplatform_v2_cp_connector_version_t` (
    -- 主键
    `id`                BIGINT(20)   NOT NULL COMMENT '雪花ID (应用层生成)',

    -- 业务字段
    `connector_id`      BIGINT(20)   NOT NULL COMMENT '关联连接器ID (逻辑外键 → connector_t.id)',
    `connection_config` MEDIUMTEXT   NOT NULL COMMENT '连接配置JSON: {protocol,protocolConfig,authTypeSchema,inputSchema,outputSchema,timeoutMs,rateLimit}',

    -- 审计字段 (4个)
    `create_time`       DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `last_update_time`  DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '最后更新时间',
    `create_by`         VARCHAR(50)  NOT NULL COMMENT '创建人',
    `last_update_by`    VARCHAR(50)  NOT NULL COMMENT '最后更新人',

    -- 主键约束
    PRIMARY KEY (`id`),

    -- 索引
    INDEX `idx_connector_id`        (`connector_id`)        COMMENT '按连接器ID查配置',
    INDEX `idx_create_time`         (`create_time`)         COMMENT '按创建时间排序'

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='连接器版本/配置表 - 1:1关联connector_t, MVP单版本, 编辑即生效';


-- ----------------------------------------------------------------------------
-- 3. 连接流基本信息表 (Flow)
--    存储连接流的基本信息 (名称、描述、状态)
--    连接流 = 编排层: 入口节点→逻辑节点→出口节点
--    lifecycle_status: 1=running (默认), 2=stopped
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `openplatform_v2_cp_flow_t` (
    -- 主键
    `id`                BIGINT(20)   NOT NULL COMMENT '雪花ID (应用层生成)',

    -- 业务字段
    `name_cn`           VARCHAR(128) NOT NULL COMMENT '中文名称',
    `name_en`           VARCHAR(128) NOT NULL COMMENT '英文名称',
    `description_cn`    VARCHAR(512)  DEFAULT NULL COMMENT '中文描述',
    `description_en`    VARCHAR(512)  DEFAULT NULL COMMENT '英文描述',
    `icon_file_id`      VARCHAR(128)  DEFAULT NULL COMMENT '图标文件ID',
    `lifecycle_status`  TINYINT(10)  NOT NULL DEFAULT 1 COMMENT '生命周期状态: 1=running(运行中), 2=stopped(已停止); MVP创建后默认running',

    -- 审计字段 (4个)
    `create_time`       DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `last_update_time`  DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '最后更新时间',
    `create_by`         VARCHAR(50)  NOT NULL COMMENT '创建人',
    `last_update_by`    VARCHAR(50)  NOT NULL COMMENT '最后更新人',

    -- 主键约束
    PRIMARY KEY (`id`),

    -- 索引
    INDEX `idx_name_cn`            (`name_cn`)             COMMENT '按中文名称搜索',
    INDEX `idx_name_en`            (`name_en`)             COMMENT '按英文名称搜索',
    INDEX `idx_lifecycle_status`   (`lifecycle_status`)    COMMENT '按状态过滤',
    INDEX `idx_create_time`        (`create_time`)         COMMENT '按创建时间排序',
    INDEX `idx_last_update_time`   (`last_update_time`)    COMMENT '按更新时间排序'

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='连接流基本信息表 - Connector Platform';


-- ----------------------------------------------------------------------------
-- 4. 连接流版本/配置表 (Flow Version)
--    1:1 关联 flow_t, 存储编排配置 JSON
--    MVP单版本模型: 每 flow 仅一条记录, 编辑即生效
--    编排配置 JSON: {trigger, nodes[], edges[]} 完整 DAG
--    触发器配置内嵌于 orchestration_config.trigger (不单独建表)
--    无物理外键 (逻辑外键 flow_id)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `openplatform_v2_cp_flow_version_t` (
    -- 主键
    `id`                    BIGINT(20)   NOT NULL COMMENT '雪花ID (应用层生成)',

    -- 业务字段
    `flow_id`               BIGINT(20)   NOT NULL COMMENT '关联连接流ID (逻辑外键 → flow_t.id)',
    `orchestration_config`  MEDIUMTEXT   NOT NULL COMMENT '编排配置JSON: {trigger:{authTypeSchema,inputSchema,rateLimit}, nodes:[{id,type,labelCn,labelEn,...}], edges:[{id,sourceNodeId,targetNodeId}]}',

    -- 审计字段 (4个)
    `create_time`           DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `last_update_time`      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '最后更新时间',
    `create_by`             VARCHAR(50)  NOT NULL COMMENT '创建人',
    `last_update_by`        VARCHAR(50)  NOT NULL COMMENT '最后更新人',

    -- 主键约束
    PRIMARY KEY (`id`),

    -- 索引
    INDEX `idx_flow_id`             (`flow_id`)             COMMENT '按连接流ID查配置',
    INDEX `idx_create_time`         (`create_time`)         COMMENT '按创建时间排序'

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='连接流版本/配置表 - 1:1关联flow_t, MVP单版本, 编辑即生效';


-- ============================================================================
-- V1 设计保留表
-- 以下 3 张表仅保留表结构定义, MVP 阶段不写入数据
-- 执行结果仅同步返回调用方, 不持久化
-- V1 阶段启用: 按 create_time 月度分区 + 30 天冷归档
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 5. 执行记录表 (Execution Record) - V1预留
--    记录每次连接流执行的元数据
--    预留计量字段: operations_count, data_in_bytes, data_out_bytes
--    状态枚举: 1=pending, 2=running, 3=success, 4=failed, 5=timeout
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `openplatform_v2_cp_execution_record_t` (
    -- 主键
    `id`                BIGINT(20)   NOT NULL COMMENT '雪花ID (应用层生成)',

    -- 业务字段
    `flow_id`           BIGINT(20)   NOT NULL COMMENT '关联连接流ID (逻辑外键 → flow_t.id)',
    `flow_version_id`   BIGINT(20)   DEFAULT NULL COMMENT '关联连接流版本ID (逻辑外键 → flow_version_t.id)',
    `trigger_type`      TINYINT(10)  NOT NULL COMMENT '触发类型: 1=HTTP触发, 2=测试执行, 3=手动触发',
    `correlation_id`    VARCHAR(128)  DEFAULT NULL COMMENT '链路追踪ID',
    `execution_status`  TINYINT(10)  NOT NULL DEFAULT 1 COMMENT '执行状态: 1=pending(待执行), 2=running(执行中), 3=success(执行成功), 4=failed(执行失败), 5=timeout(已超时)',
    `result_data`       MEDIUMTEXT   DEFAULT NULL COMMENT '执行结果数据JSON (含各步骤详情)',
    `error_message`     TEXT         DEFAULT NULL COMMENT '错误信息',
    `duration_ms`       INT(11)      DEFAULT NULL COMMENT '总执行耗时(毫秒)',

    -- 预留计量字段 (V1启用计费时使用)
    `operations_count`  INT(11)      NOT NULL DEFAULT 0 COMMENT '操作计数 (预留计量)',
    `data_in_bytes`     BIGINT(20)   NOT NULL DEFAULT 0 COMMENT '入站数据量/字节 (预留计量)',
    `data_out_bytes`    BIGINT(20)   NOT NULL DEFAULT 0 COMMENT '出站数据量/字节 (预留计量)',

    -- 审计字段 (4个)
    `create_time`       DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `last_update_time`  DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '最后更新时间',
    `create_by`         VARCHAR(50)  NOT NULL COMMENT '创建人',
    `last_update_by`    VARCHAR(50)  NOT NULL COMMENT '最后更新人',

    -- 主键约束
    PRIMARY KEY (`id`),

    -- 索引
    INDEX `idx_flow_id`             (`flow_id`)             COMMENT '按连接流ID查执行记录',
    INDEX `idx_correlation_id`      (`correlation_id`)      COMMENT '链路追踪查询',
    INDEX `idx_execution_status`    (`execution_status`)    COMMENT '按执行状态过滤',
    INDEX `idx_create_time`         (`create_time`)         COMMENT '按执行时间排序/分区'

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='执行记录表 - V1预留, MVP不写入';


-- ----------------------------------------------------------------------------
-- 6. 执行步骤详情表 (Execution Step) - V1预留
--    记录连接流单次执行中每个节点的输入/输出/耗时/状态
--    大字段(input_data/output_data)默认写MySQL内嵌,
--    当 size > 64KB 时自动外置到对象存储, 表中存 *_uri/*_size/*_hash 引用
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `openplatform_v2_cp_execution_step_t` (
    -- 主键
    `id`                BIGINT(20)   NOT NULL COMMENT '雪花ID (应用层生成)',

    -- 业务字段
    `execution_id`      BIGINT(20)   NOT NULL COMMENT '关联执行记录ID (逻辑外键 → execution_record_t.id)',
    `step_order`        INT(11)      NOT NULL COMMENT '步骤序号 (从1开始)',
    `node_id`           VARCHAR(64)  NOT NULL COMMENT '节点ID (对应编排配置中的节点ID)',
    `node_type`         VARCHAR(32)  NOT NULL COMMENT '节点类型: entry/connector/data_processor/exit',
    `node_label_cn`     VARCHAR(128)  DEFAULT NULL COMMENT '节点中文名称 (执行时快照)',
    `node_label_en`     VARCHAR(128)  DEFAULT NULL COMMENT '节点英文名称 (执行时快照)',
    `step_status`       TINYINT(10)  NOT NULL DEFAULT 1 COMMENT '步骤状态: 1=pending, 2=running, 3=success, 4=failed, 5=timeout',
    `input_data`        MEDIUMTEXT   DEFAULT NULL COMMENT '步骤输入数据JSON',
    `output_data`       MEDIUMTEXT   DEFAULT NULL COMMENT '步骤输出数据JSON',
    `error_message`     TEXT         DEFAULT NULL COMMENT '步骤错误信息',
    `duration_ms`       INT(11)      DEFAULT NULL COMMENT '步骤耗时(毫秒)',

    -- 对象存储引用 (V1启用外置时使用)
    `input_uri`         VARCHAR(512)  DEFAULT NULL COMMENT '输入数据外置URI (V1)',
    `input_size`        INT(11)      DEFAULT NULL COMMENT '输入数据大小/字节 (V1)',
    `input_hash`        VARCHAR(64)  DEFAULT NULL COMMENT '输入数据SHA256 (V1)',
    `output_uri`        VARCHAR(512)  DEFAULT NULL COMMENT '输出数据外置URI (V1)',
    `output_size`       INT(11)      DEFAULT NULL COMMENT '输出数据大小/字节 (V1)',
    `output_hash`       VARCHAR(64)  DEFAULT NULL COMMENT '输出数据SHA256 (V1)',

    -- 审计字段 (4个)
    `create_time`       DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `last_update_time`  DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '最后更新时间',
    `create_by`         VARCHAR(50)  NOT NULL COMMENT '创建人',
    `last_update_by`    VARCHAR(50)  NOT NULL COMMENT '最后更新人',

    -- 主键约束
    PRIMARY KEY (`id`),

    -- 索引
    INDEX `idx_execution_id_step_order` (`execution_id`, `step_order`) COMMENT '按执行记录ID+步骤序号查询',
    INDEX `idx_create_time`             (`create_time`)                COMMENT '按创建时间排序/分区'

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='执行步骤详情表 - V1预留, MVP不写入';


-- ----------------------------------------------------------------------------
-- 7. 对象存储引用元数据表 (Storage Blob Ref) - V1预留
--    记录已外置到对象存储的大字段引用信息
--    用于 V1 的 GC 任务扫描清理
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `openplatform_v2_cp_storage_blob_ref_t` (
    -- 主键
    `id`                BIGINT(20)   NOT NULL COMMENT '雪花ID (应用层生成)',

    -- 业务字段
    `owner_type`        VARCHAR(64)  NOT NULL COMMENT '所有者类型: execution_step_input / execution_step_output / execution_result_data',
    `owner_id`          BIGINT(20)   NOT NULL COMMENT '所有者记录ID (逻辑外键)',
    `blob_uri`          VARCHAR(512) NOT NULL COMMENT '对象存储URI',
    `blob_size`         INT(11)      DEFAULT NULL COMMENT '数据大小/字节',
    `blob_hash`         VARCHAR(64)  DEFAULT NULL COMMENT '数据SHA256',
    `storage_status`    TINYINT(10)  NOT NULL DEFAULT 1 COMMENT '存储状态: 1=active(活跃), 2=archived(已归档), 3=deleted(已删除)',
    `expire_time`       DATETIME(3)  DEFAULT NULL COMMENT '过期时间 (用于自动清理)',

    -- 审计字段 (4个)
    `create_time`       DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `last_update_time`  DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '最后更新时间',
    `create_by`         VARCHAR(50)  NOT NULL COMMENT '创建人',
    `last_update_by`    VARCHAR(50)  NOT NULL COMMENT '最后更新人',

    -- 主键约束
    PRIMARY KEY (`id`),

    -- 索引
    INDEX `idx_owner_type_owner_id` (`owner_type`, `owner_id`) COMMENT '按所有者类型+ID查询/GC扫描',
    INDEX `idx_storage_status`      (`storage_status`)         COMMENT '按存储状态过滤',
    INDEX `idx_expire_time`         (`expire_time`)            COMMENT '按过期时间扫描清理'

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='对象存储引用元数据表 - V1预留, MVP不写入';


-- ============================================================================
-- 初始化完成标记
-- ============================================================================
-- 连接器平台 (Connector Platform) 7张表创建完毕
-- 活跃表 (4张): connector_t, connector_version_t, flow_t, flow_version_t
-- V1预留表 (3张): execution_record_t, execution_step_t, storage_blob_ref_t
-- ============================================================================