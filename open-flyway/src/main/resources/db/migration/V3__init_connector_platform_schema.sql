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
-- 初始化完成标记
-- ============================================================================
-- 连接器平台 (Connector Platform) 4张表创建完毕
--   connector_t, connector_version_t, flow_t, flow_version_t
-- 注：execution_record_t / execution_step_t / storage_blob_ref_t 由 V3 负责
-- ============================================================================
