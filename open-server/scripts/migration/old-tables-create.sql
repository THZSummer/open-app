-- 1. 分类节点表
CREATE TABLE `openplatform_module_node_t` (
    `id` BIGINT(20) PRIMARY KEY,
    `node_name_cn` VARCHAR(100) COMMENT '节点中文名称',
    `node_name_en` VARCHAR(100) COMMENT '节点英文名称',
    `parent_Node_id` BIGINT(20) COMMENT '父节点ID',
    `is_parent_node` TINYINT(10) COMMENT '是否父节点',
    `is_leaf_node` TINYINT(10) COMMENT '是否叶子节点',
    `order_num` INT COMMENT '排序号',
    `auth_type` TINYINT(10) COMMENT '认证方式',
    `status` TINYINT(10) DEFAULT 1,
    `create_by` VARCHAR(100),
    `create_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    `last_update_by` VARCHAR(100),
    `last_update_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    KEY `idx_parent_node_id` (`parent_Node_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. 权限主表
CREATE TABLE `openplatform_permission_t` (
    `id` BIGINT(20) PRIMARY KEY,
    `permission_name_cn` VARCHAR(100),
    `permission_name_en` VARCHAR(100),
    `module_id` BIGINT(20),
    `scope_id` VARCHAR(200),
    `permisssion_type` VARCHAR(20) COMMENT '权限类型（拼写错误保留）',
    `is_approval_required` TINYINT(10),
    `auth_type` TINYINT(10),
    `status` TINYINT(10) DEFAULT 1,
    `create_by` VARCHAR(100),
    `create_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    `last_update_by` VARCHAR(100),
    `last_update_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    KEY `idx_module_id` (`module_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. API权限表
CREATE TABLE `openplatform_permission_api_t` (
    `id` BIGINT(20) PRIMARY KEY,
    `api_name_cn` VARCHAR(100),
    `api_name_en` VARCHAR(100),
    `permission_id` BIGINT(20),
    `api_path` VARCHAR(500),
    `api_method` VARCHAR(10),
    `auth_type` TINYINT(10),
    `status` TINYINT(10) DEFAULT 1,
    `create_by` VARCHAR(100),
    `create_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    `last_update_by` VARCHAR(100),
    `last_update_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    KEY `idx_permission_id` (`permission_id`),
    KEY `idx_api_path` (`api_path`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4. 事件表
CREATE TABLE `openplatform_event_t` (
    `id` BIGINT(20) PRIMARY KEY,
    `event_name_cn` VARCHAR(100),
    `event_name_en` VARCHAR(100),
    `module_id` BIGINT(20),
    `topic` VARCHAR(200),
    `event_type` VARCHAR(50),
    `is_approval_required` TINYINT(10),
    `status` TINYINT(10) DEFAULT 1,
    `create_by` VARCHAR(100),
    `create_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    `last_update_by` VARCHAR(100),
    `last_update_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    KEY `idx_module_id` (`module_id`),
    KEY `idx_topic` (`topic`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 5. 应用属性表（用于获取通道配置）
CREATE TABLE `openplatform_app_p_t` (
    `id` BIGINT(20) PRIMARY KEY,
    `parent_id` BIGINT(20) COMMENT '关联应用ID',
    `property_name` VARCHAR(100),
    `property_value` TEXT,
    `status` TINYINT(10) DEFAULT 1,
    `create_by` VARCHAR(100),
    `create_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    `last_update_by` VARCHAR(100),
    `last_update_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 6. 订阅关系表（旧）
CREATE TABLE `openplatform_app_permission_t` (
    `id` BIGINT(20) PRIMARY KEY,
    `app_id` BIGINT(20) NOT NULL,
    `permission_id` BIGINT(20) NOT NULL,
    `tenant_id` VARCHAR(100),
    `permisssion_type` VARCHAR(20) COMMENT '0=API权限, 1=事件',
    `status` TINYINT(10) DEFAULT 1 COMMENT '0=待审核, 1=已开通, 2=驳回, 3=关闭',
    `create_by` VARCHAR(100),
    `create_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    `last_update_by` VARCHAR(100),
    `last_update_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    KEY `idx_app_id` (`app_id`),
    KEY `idx_permission_id` (`permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 7. 审批流程表（旧）
CREATE TABLE `openplatform_eflow_t` (
    `eflow_id` BIGINT(20) PRIMARY KEY,
    `eflow_type` VARCHAR(50),
    `eflow_status` TINYINT(10) COMMENT '0=待审, 1=已通过, 2=已拒绝, 3=已撤销',
    `eflow_submit_user` VARCHAR(100),
    `eflow_submit_message` TEXT,
    `eflow_audit_user` VARCHAR(100),
    `eflow_audit_message` TEXT,
    `resource_type` VARCHAR(50),
    `resource_id` BIGINT(20),
    `resource_info` TEXT,
    `resource_delta` TEXT,
    `tenant_id` VARCHAR(100),
    `status` TINYINT(10) DEFAULT 1,
    `create_by` VARCHAR(100),
    `create_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    `last_update_by` VARCHAR(100),
    `last_update_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    KEY `idx_resource` (`resource_type`, `resource_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 8. 审批日志表（旧）
CREATE TABLE `openplatform_eflow_log_t` (
    `eflow_log_id` BIGINT(20) PRIMARY KEY,
    `eflow_log_trace_id` BIGINT(20) COMMENT '关联审批流程ID',
    `eflow_log_type` VARCHAR(50),
    `eflow_log_user` VARCHAR(100),
    `eflow_log_message` TEXT,
    `status` TINYINT(10) DEFAULT 1,
    `create_by` VARCHAR(100),
    `create_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    `last_update_by` VARCHAR(100),
    `last_update_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    KEY `idx_eflow_log_trace_id` (`eflow_log_trace_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
