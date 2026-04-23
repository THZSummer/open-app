-- ============================================================================
-- 能力开放平台数据库表结构初始化脚本
-- 版本: v1.0
-- 创建日期: 2026-04-23
-- 说明: 创建 15 张表（10 张主表 + 4 张属性表 + 1 张关联表）
-- 执行顺序: 01（表结构初始化）
-- ============================================================================

-- 设置字符集
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================================
-- 1. 分类表（主表）
-- ============================================================================
DROP TABLE IF EXISTS `openplatform_v2_category_t`;
CREATE TABLE `openplatform_v2_category_t` (
    `id` BIGINT(20) PRIMARY KEY COMMENT '主键ID（雪花ID）',
    `category_alias` VARCHAR(50) COMMENT '分类别名（仅根分类需要）：app_type_a/app_type_b/personal_aksk',
    `name_cn` VARCHAR(100) NOT NULL COMMENT '中文名称',
    `name_en` VARCHAR(100) NOT NULL COMMENT '英文名称',
    `parent_id` BIGINT(20) COMMENT '父分类ID',
    `path` VARCHAR(500) COMMENT '路径：/根ID/父ID/当前ID/，用于子树查询优化',
    `sort_order` INT DEFAULT 0 COMMENT '排序号',
    `status` TINYINT(10) DEFAULT 1 COMMENT '状态：0=禁用, 1=启用',
    `create_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `last_update_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '最后更新时间',
    `create_by` VARCHAR(100) COMMENT '创建人账号',
    `last_update_by` VARCHAR(100) COMMENT '最后更新人账号',
    KEY `idx_alias_parent` (`category_alias`, `parent_id`),
    KEY `idx_parent_id` (`parent_id`),
    KEY `idx_path` (`path`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分类表';

-- ============================================================================
-- 2. 分类责任人关联表（关联表）
-- ============================================================================
DROP TABLE IF EXISTS `openplatform_v2_category_owner_t`;
CREATE TABLE `openplatform_v2_category_owner_t` (
    `id` BIGINT(20) PRIMARY KEY COMMENT '主键ID（雪花ID）',
    `category_id` BIGINT(20) NOT NULL COMMENT '分类ID',
    `user_id` VARCHAR(100) NOT NULL COMMENT '用户ID',
    `user_name` VARCHAR(100) COMMENT '用户姓名',
    `create_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `last_update_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '最后更新时间',
    `create_by` VARCHAR(100) COMMENT '创建人账号',
    `last_update_by` VARCHAR(100) COMMENT '最后更新人账号',
    UNIQUE KEY `uk_category_user` (`category_id`, `user_id`),
    KEY `idx_category_id` (`category_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分类责任人关联表';

-- ============================================================================
-- 3. API 资源主表（主表）
-- ============================================================================
DROP TABLE IF EXISTS `openplatform_v2_api_t`;
CREATE TABLE `openplatform_v2_api_t` (
    `id` BIGINT(20) PRIMARY KEY COMMENT '主键ID（雪花ID）',
    `name_cn` VARCHAR(100) NOT NULL COMMENT '中文名称',
    `name_en` VARCHAR(100) NOT NULL COMMENT '英文名称',
    `category_id` BIGINT(20) NOT NULL COMMENT '所属分类ID',
    `path` VARCHAR(500) NOT NULL COMMENT 'API路径',
    `method` VARCHAR(10) NOT NULL COMMENT 'HTTP方法：GET/POST/PUT/DELETE',
    `auth_type` TINYINT(10) NOT NULL DEFAULT 1 COMMENT '认证方式: 0=Cookie, 1=SOA, 2=APIG, 3=IAM, 4=免认证, 5=AKSK, 6=CLITOKEN',
    `status` TINYINT(10) DEFAULT 0 COMMENT '状态：0=草稿, 1=待审, 2=已发布, 3=已下线',
    `create_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `last_update_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '最后更新时间',
    `create_by` VARCHAR(100) COMMENT '创建人账号',
    `last_update_by` VARCHAR(100) COMMENT '最后更新人账号',
    KEY `idx_category_id` (`category_id`),
    KEY `idx_status` (`status`),
    KEY `idx_path_method` (`path`, `method`),
    KEY `idx_auth_type` (`auth_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='API资源主表';

-- ============================================================================
-- 4. API 资源属性表（属性表）
-- ============================================================================
DROP TABLE IF EXISTS `openplatform_v2_api_p_t`;
CREATE TABLE `openplatform_v2_api_p_t` (
    `id` BIGINT(20) PRIMARY KEY COMMENT '主键ID（雪花ID）',
    `parent_id` BIGINT(20) NOT NULL COMMENT '关联API主表ID',
    `property_name` VARCHAR(100) NOT NULL COMMENT '属性名称',
    `property_value` TEXT COMMENT '属性值',
    `status` TINYINT(10) DEFAULT 1 COMMENT '状态：0=禁用, 1=启用',
    `create_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `last_update_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '最后更新时间',
    `create_by` VARCHAR(100) COMMENT '创建人账号',
    `last_update_by` VARCHAR(100) COMMENT '最后更新人账号',
    KEY `idx_parent_id` (`parent_id`),
    KEY `idx_property_name` (`property_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='API资源属性表';

-- ============================================================================
-- 5. 事件资源主表（主表）
-- ============================================================================
DROP TABLE IF EXISTS `openplatform_v2_event_t`;
CREATE TABLE `openplatform_v2_event_t` (
    `id` BIGINT(20) PRIMARY KEY COMMENT '主键ID（雪花ID）',
    `name_cn` VARCHAR(100) NOT NULL COMMENT '中文名称',
    `name_en` VARCHAR(100) NOT NULL COMMENT '英文名称',
    `category_id` BIGINT(20) NOT NULL COMMENT '所属分类ID',
    `topic` VARCHAR(200) NOT NULL COMMENT 'Topic主题',
    `status` TINYINT(10) DEFAULT 0 COMMENT '状态：0=草稿, 1=待审, 2=已发布, 3=已下线',
    `create_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `last_update_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '最后更新时间',
    `create_by` VARCHAR(100) COMMENT '创建人账号',
    `last_update_by` VARCHAR(100) COMMENT '最后更新人账号',
    UNIQUE KEY `uk_topic` (`topic`),
    KEY `idx_category_id` (`category_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='事件资源主表';

-- ============================================================================
-- 6. 事件资源属性表（属性表）
-- ============================================================================
DROP TABLE IF EXISTS `openplatform_v2_event_p_t`;
CREATE TABLE `openplatform_v2_event_p_t` (
    `id` BIGINT(20) PRIMARY KEY COMMENT '主键ID（雪花ID）',
    `parent_id` BIGINT(20) NOT NULL COMMENT '关联事件主表ID',
    `property_name` VARCHAR(100) NOT NULL COMMENT '属性名称',
    `property_value` TEXT COMMENT '属性值',
    `status` TINYINT(10) DEFAULT 1 COMMENT '状态：0=禁用, 1=启用',
    `create_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `last_update_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '最后更新时间',
    `create_by` VARCHAR(100) COMMENT '创建人账号',
    `last_update_by` VARCHAR(100) COMMENT '最后更新人账号',
    KEY `idx_parent_id` (`parent_id`),
    KEY `idx_property_name` (`property_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='事件资源属性表';

-- ============================================================================
-- 7. 回调资源主表（主表）
-- ============================================================================
DROP TABLE IF EXISTS `openplatform_v2_callback_t`;
CREATE TABLE `openplatform_v2_callback_t` (
    `id` BIGINT(20) PRIMARY KEY COMMENT '主键ID（雪花ID）',
    `name_cn` VARCHAR(100) NOT NULL COMMENT '中文名称',
    `name_en` VARCHAR(100) NOT NULL COMMENT '英文名称',
    `category_id` BIGINT(20) NOT NULL COMMENT '所属分类ID',
    `status` TINYINT(10) DEFAULT 0 COMMENT '状态：0=草稿, 1=待审, 2=已发布, 3=已下线',
    `create_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `last_update_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '最后更新时间',
    `create_by` VARCHAR(100) COMMENT '创建人账号',
    `last_update_by` VARCHAR(100) COMMENT '最后更新人账号',
    KEY `idx_category_id` (`category_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='回调资源主表';

-- ============================================================================
-- 8. 回调资源属性表（属性表）
-- ============================================================================
DROP TABLE IF EXISTS `openplatform_v2_callback_p_t`;
CREATE TABLE `openplatform_v2_callback_p_t` (
    `id` BIGINT(20) PRIMARY KEY COMMENT '主键ID（雪花ID）',
    `parent_id` BIGINT(20) NOT NULL COMMENT '关联回调主表ID',
    `property_name` VARCHAR(100) NOT NULL COMMENT '属性名称',
    `property_value` TEXT COMMENT '属性值',
    `status` TINYINT(10) DEFAULT 1 COMMENT '状态：0=禁用, 1=启用',
    `create_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `last_update_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '最后更新时间',
    `create_by` VARCHAR(100) COMMENT '创建人账号',
    `last_update_by` VARCHAR(100) COMMENT '最后更新人账号',
    KEY `idx_parent_id` (`parent_id`),
    KEY `idx_property_name` (`property_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='回调资源属性表';

-- ============================================================================
-- 9. 权限资源主表（主表）
-- ============================================================================
DROP TABLE IF EXISTS `openplatform_v2_permission_t`;
CREATE TABLE `openplatform_v2_permission_t` (
    `id` BIGINT(20) PRIMARY KEY COMMENT '主键ID（雪花ID）',
    `name_cn` VARCHAR(100) NOT NULL COMMENT '中文名称',
    `name_en` VARCHAR(100) NOT NULL COMMENT '英文名称',
    `scope` VARCHAR(100) NOT NULL COMMENT '权限标识，如 api:im:send-message',
    `resource_type` VARCHAR(20) NOT NULL COMMENT '资源类型：api, event, callback',
    `resource_id` BIGINT(20) NOT NULL COMMENT '关联的资源ID（API/Event/Callback）',
    `category_id` BIGINT(20) NOT NULL COMMENT '所属分类ID',
    `status` TINYINT(10) DEFAULT 1 COMMENT '状态：0=禁用, 1=启用',
    `create_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `last_update_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '最后更新时间',
    `create_by` VARCHAR(100) COMMENT '创建人账号',
    `last_update_by` VARCHAR(100) COMMENT '最后更新人账号',
    UNIQUE KEY `uk_scope` (`scope`),
    KEY `idx_resource` (`resource_type`, `resource_id`),
    KEY `idx_category_id` (`category_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限资源主表';

-- ============================================================================
-- 10. 权限资源属性表（属性表）
-- ============================================================================
DROP TABLE IF EXISTS `openplatform_v2_permission_p_t`;
CREATE TABLE `openplatform_v2_permission_p_t` (
    `id` BIGINT(20) PRIMARY KEY COMMENT '主键ID（雪花ID）',
    `parent_id` BIGINT(20) NOT NULL COMMENT '关联权限主表ID',
    `property_name` VARCHAR(100) NOT NULL COMMENT '属性名称',
    `property_value` TEXT COMMENT '属性值',
    `status` TINYINT(10) DEFAULT 1 COMMENT '状态：0=禁用, 1=启用',
    `create_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `last_update_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '最后更新时间',
    `create_by` VARCHAR(100) COMMENT '创建人账号',
    `last_update_by` VARCHAR(100) COMMENT '最后更新人账号',
    KEY `idx_parent_id` (`parent_id`),
    KEY `idx_property_name` (`property_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限资源属性表';

-- ============================================================================
-- 11. 订阅关系表（主表）
-- ============================================================================
DROP TABLE IF EXISTS `openplatform_v2_subscription_t`;
CREATE TABLE `openplatform_v2_subscription_t` (
    `id` BIGINT(20) PRIMARY KEY COMMENT '主键ID（雪花ID）',
    `app_id` BIGINT(20) NOT NULL COMMENT '应用ID',
    `permission_id` BIGINT(20) NOT NULL COMMENT '权限ID',
    `status` TINYINT(10) DEFAULT 0 COMMENT '状态：0=待审, 1=已授权, 2=已拒绝, 3=已取消',
    `channel_type` TINYINT(10) COMMENT '通道类型：0=内部消息队列, 1=WebHook, 2=SSE, 3=WebSocket',
    `channel_address` VARCHAR(500) COMMENT '通道地址',
    `auth_type` TINYINT(10) NOT NULL DEFAULT 1 COMMENT '认证方式: 0=Cookie, 1=SOA, 2=APIG, 3=IAM, 4=免认证, 5=AKSK, 6=CLITOKEN',
    `approved_at` DATETIME(3) COMMENT '审批通过时间',
    `approved_by` VARCHAR(100) COMMENT '审批人账号',
    `create_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `last_update_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '最后更新时间',
    `create_by` VARCHAR(100) COMMENT '创建人账号',
    `last_update_by` VARCHAR(100) COMMENT '最后更新人账号',
    UNIQUE KEY `uk_app_permission` (`app_id`, `permission_id`),
    KEY `idx_app_id` (`app_id`),
    KEY `idx_permission_id` (`permission_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订阅关系表';

-- ============================================================================
-- 12. 审批流程模板表（主表）
-- ============================================================================
DROP TABLE IF EXISTS `openplatform_v2_approval_flow_t`;
CREATE TABLE `openplatform_v2_approval_flow_t` (
    `id` BIGINT(20) PRIMARY KEY COMMENT '主键ID（雪花ID）',
    `name_cn` VARCHAR(100) NOT NULL COMMENT '中文名称',
    `name_en` VARCHAR(100) NOT NULL COMMENT '英文名称',
    `code` VARCHAR(50) NOT NULL COMMENT '编码：default, api_register, permission_apply',
    `description_cn` TEXT COMMENT '中文描述',
    `description_en` TEXT COMMENT '英文描述',
    `is_default` TINYINT(10) DEFAULT 0 COMMENT '是否默认：0=否, 1=是',
    `nodes` JSON NOT NULL COMMENT '审批节点配置（JSON数组）',
    `status` TINYINT(10) DEFAULT 1 COMMENT '状态：0=禁用, 1=启用',
    `create_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `last_update_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '最后更新时间',
    `create_by` VARCHAR(100) COMMENT '创建人账号',
    `last_update_by` VARCHAR(100) COMMENT '最后更新人账号',
    UNIQUE KEY `uk_code` (`code`),
    KEY `idx_is_default` (`is_default`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批流程模板表';

-- ============================================================================
-- 13. 审批记录表（主表）
-- ============================================================================
DROP TABLE IF EXISTS `openplatform_v2_approval_record_t`;
CREATE TABLE `openplatform_v2_approval_record_t` (
    `id` BIGINT(20) PRIMARY KEY COMMENT '主键ID（雪花ID）',
    `flow_id` BIGINT(20) NOT NULL COMMENT '审批流程ID',
    `business_type` VARCHAR(50) NOT NULL COMMENT '业务类型：api_register, event_register, permission_apply',
    `business_id` BIGINT(20) NOT NULL COMMENT '业务ID',
    `applicant_id` VARCHAR(100) NOT NULL COMMENT '申请人ID',
    `applicant_name` VARCHAR(100) COMMENT '申请人姓名',
    `status` TINYINT(10) DEFAULT 0 COMMENT '状态：0=待审, 1=已通过, 2=已拒绝, 3=已撤销',
    `current_node` INT DEFAULT 0 COMMENT '当前节点索引',
    `completed_at` DATETIME(3) COMMENT '完成时间',
    `create_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `last_update_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '最后更新时间',
    `create_by` VARCHAR(100) COMMENT '创建人账号',
    `last_update_by` VARCHAR(100) COMMENT '最后更新人账号',
    KEY `idx_flow_id` (`flow_id`),
    KEY `idx_business` (`business_type`, `business_id`),
    KEY `idx_applicant` (`applicant_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批记录表';

-- ============================================================================
-- 14. 审批操作日志表（主表）
-- ============================================================================
DROP TABLE IF EXISTS `openplatform_v2_approval_log_t`;
CREATE TABLE `openplatform_v2_approval_log_t` (
    `id` BIGINT(20) PRIMARY KEY COMMENT '主键ID（雪花ID）',
    `record_id` BIGINT(20) NOT NULL COMMENT '审批记录ID',
    `node_index` INT NOT NULL COMMENT '节点索引',
    `operator_id` VARCHAR(100) NOT NULL COMMENT '操作人ID',
    `operator_name` VARCHAR(100) COMMENT '操作人姓名',
    `action` TINYINT(10) NOT NULL COMMENT '操作类型：0=同意, 1=拒绝, 2=撤销, 3=转交',
    `comment` TEXT COMMENT '审批意见',
    `create_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `last_update_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '最后更新时间',
    `create_by` VARCHAR(100) COMMENT '创建人账号',
    `last_update_by` VARCHAR(100) COMMENT '最后更新人账号',
    KEY `idx_record_id` (`record_id`),
    KEY `idx_operator` (`operator_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批操作日志表';

-- ============================================================================
-- 15. 用户授权表（主表）
-- ============================================================================
DROP TABLE IF EXISTS `openplatform_v2_user_authorization_t`;
CREATE TABLE `openplatform_v2_user_authorization_t` (
    `id` BIGINT(20) PRIMARY KEY COMMENT '主键ID（雪花ID）',
    `user_id` VARCHAR(100) NOT NULL COMMENT '用户ID',
    `app_id` BIGINT(20) NOT NULL COMMENT '应用ID',
    `scopes` JSON NOT NULL COMMENT '权限范围数组（JSON）',
    `expires_at` DATETIME(3) COMMENT '过期时间',
    `revoked_at` DATETIME(3) COMMENT '撤销时间',
    `create_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `last_update_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '最后更新时间',
    `create_by` VARCHAR(100) COMMENT '创建人账号',
    `last_update_by` VARCHAR(100) COMMENT '最后更新人账号',
    UNIQUE KEY `uk_user_app` (`user_id`, `app_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_app_id` (`app_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户授权表';

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================================
-- 初始化完成
-- 共创建 15 张表：
-- - 主表：10 张
-- - 属性表：4 张
-- - 关联表：1 张
-- ============================================================================