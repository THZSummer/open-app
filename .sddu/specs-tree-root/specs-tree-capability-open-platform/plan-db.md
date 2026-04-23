# 数据库表结构设计

> 本文档为 `plan.md` 的子文档，定义能力开放平台的详细表结构。

## 表清单

| 表名 | 类型 | 说明 |
|------|------|------|
| `openplatform_v2_category_t` | 主表 | 分类表 |
| `openplatform_v2_category_owner_t` | 关联表 | 分类责任人关联表 |
| `openplatform_v2_api_t` | 主表 | API资源主表 |
| `openplatform_v2_api_p_t` | 属性表 | API资源属性表 |
| `openplatform_v2_event_t` | 主表 | 事件资源主表 |
| `openplatform_v2_event_p_t` | 属性表 | 事件资源属性表 |
| `openplatform_v2_callback_t` | 主表 | 回调资源主表 |
| `openplatform_v2_callback_p_t` | 属性表 | 回调资源属性表 |
| `openplatform_v2_permission_t` | 主表 | 权限资源主表 |
| `openplatform_v2_permission_p_t` | 属性表 | 权限资源属性表 |
| `openplatform_v2_subscription_t` | 主表 | 订阅关系表 |
| `openplatform_v2_approval_flow_t` | 主表 | 审批流程模板表 |
| `openplatform_v2_approval_record_t` | 主表 | 审批记录表 |
| `openplatform_v2_approval_log_t` | 主表 | 审批操作日志表 |
| `openplatform_v2_user_authorization_t` | 主表 | 用户授权表 |

**总计**：15 张表（10 张主表 + 4 张属性表 + 1 张关联表）

---

## 表结构定义

### 分类表

```sql
-- ============================================
-- 分类表（扩展现有 openplatform_mode_node_t）
-- ============================================
CREATE TABLE `openplatform_v2_category_t` (
    `id` BIGINT(20) PRIMARY KEY,
    `category_alias` VARCHAR(50) COMMENT '分类别名（仅根分类需要）：app_type_a/app_type_b/personal_aksk',
    `name_cn` VARCHAR(100) NOT NULL COMMENT '中文名称',
    `name_en` VARCHAR(100) NOT NULL COMMENT '英文名称',
    `parent_id` BIGINT(20),
    `path` VARCHAR(500) COMMENT '路径：/根ID/父ID/当前ID/，用于子树查询优化',
    `sort_order` INT DEFAULT 0,
    `status` TINYINT(10) DEFAULT 1 COMMENT '0=禁用, 1=启用',
    `create_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    `last_update_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    `create_by` VARCHAR(100),
    `last_update_by` VARCHAR(100),
    KEY `idx_alias_parent` (`category_alias`, `parent_id`),
    KEY `idx_parent_id` (`parent_id`),
    KEY `idx_path` (`path`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分类表';

-- 分类责任人关联表
CREATE TABLE `openplatform_v2_category_owner_t` (
    `id` BIGINT(20) PRIMARY KEY,
    `category_id` BIGINT(20) NOT NULL,
    `user_id` VARCHAR(100) NOT NULL,
    `create_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    `last_update_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    `create_by` VARCHAR(100),
    `last_update_by` VARCHAR(100),
    UNIQUE KEY `uk_category_user` (`category_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分类责任人关联表';
```

### API 资源表

```sql
-- ============================================
-- API 资源主表（扩展现有 openplatform_permission_api_t）
-- ============================================
CREATE TABLE `openplatform_v2_api_t` (
    `id` BIGINT(20) PRIMARY KEY,
    `name_cn` VARCHAR(100) NOT NULL COMMENT '中文名称',
    `name_en` VARCHAR(100) NOT NULL COMMENT '英文名称',
    `path` VARCHAR(500) NOT NULL COMMENT 'API路径',
    `method` VARCHAR(10) NOT NULL COMMENT 'HTTP方法',
    `auth_type` TINYINT(10) NOT NULL DEFAULT 1 COMMENT '认证方式: 0=Cookie, 1=SOA, 2=APIG, 3=IAM, 4=免认证, 5=AKSK, 6=CLITOKEN',
    `status` TINYINT(10) DEFAULT 0 COMMENT '0=草稿, 1=待审, 2=已发布, 3=已下线',
    `create_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    `last_update_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    `create_by` VARCHAR(100),
    `last_update_by` VARCHAR(100),
    KEY `idx_auth_type` (`auth_type`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='API资源主表';

-- API 资源属性表
CREATE TABLE `openplatform_v2_api_p_t` (
    `id` BIGINT(20) PRIMARY KEY,
    `parent_id` BIGINT(20) NOT NULL COMMENT '关联 API 主表 ID',
    `property_name` VARCHAR(100) NOT NULL COMMENT '属性名称',
    `property_value` TEXT COMMENT '属性值',
    `status` TINYINT(10) DEFAULT 1 COMMENT '0=禁用, 1=启用',
    `create_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    `last_update_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    `create_by` VARCHAR(100),
    `last_update_by` VARCHAR(100),
    KEY `idx_parent_id` (`parent_id`),
    KEY `idx_property_name` (`property_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='API资源属性表';
```

### 事件资源表

```sql
-- ============================================
-- 事件资源主表（扩展现有 openplatform_event_t）
-- ============================================
CREATE TABLE `openplatform_v2_event_t` (
    `id` BIGINT(20) PRIMARY KEY,
    `name_cn` VARCHAR(100) NOT NULL COMMENT '中文名称',
    `name_en` VARCHAR(100) NOT NULL COMMENT '英文名称',
    `topic` VARCHAR(200) NOT NULL UNIQUE COMMENT 'Topic',
    `status` TINYINT(10) DEFAULT 0 COMMENT '0=草稿, 1=待审, 2=已发布, 3=已下线',
    `create_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    `last_update_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    `create_by` VARCHAR(100),
    `last_update_by` VARCHAR(100),
    KEY `idx_topic` (`topic`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='事件资源主表';

-- 事件资源属性表
CREATE TABLE `openplatform_v2_event_p_t` (
    `id` BIGINT(20) PRIMARY KEY,
    `parent_id` BIGINT(20) NOT NULL COMMENT '关联事件主表 ID',
    `property_name` VARCHAR(100) NOT NULL COMMENT '属性名称',
    `property_value` TEXT COMMENT '属性值',
    `status` TINYINT(10) DEFAULT 1 COMMENT '0=禁用, 1=启用',
    `create_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    `last_update_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    `create_by` VARCHAR(100),
    `last_update_by` VARCHAR(100),
    KEY `idx_parent_id` (`parent_id`),
    KEY `idx_property_name` (`property_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='事件资源属性表';
```

### 回调资源表

```sql
-- ============================================
-- 回调资源主表（新建）
-- ============================================
CREATE TABLE `openplatform_v2_callback_t` (
    `id` BIGINT(20) PRIMARY KEY,
    `name_cn` VARCHAR(100) NOT NULL COMMENT '中文名称',
    `name_en` VARCHAR(100) NOT NULL COMMENT '英文名称',
    `status` TINYINT(10) DEFAULT 0 COMMENT '0=草稿, 1=待审, 2=已发布, 3=已下线',
    `create_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    `last_update_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    `create_by` VARCHAR(100),
    `last_update_by` VARCHAR(100),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='回调资源主表';

-- 回调资源属性表
CREATE TABLE `openplatform_v2_callback_p_t` (
    `id` BIGINT(20) PRIMARY KEY,
    `parent_id` BIGINT(20) NOT NULL COMMENT '关联回调主表 ID',
    `property_name` VARCHAR(100) NOT NULL COMMENT '属性名称',
    `property_value` TEXT COMMENT '属性值',
    `status` TINYINT(10) DEFAULT 1 COMMENT '0=禁用, 1=启用',
    `create_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    `last_update_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    `create_by` VARCHAR(100),
    `last_update_by` VARCHAR(100),
    KEY `idx_parent_id` (`parent_id`),
    KEY `idx_property_name` (`property_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='回调资源属性表';
```

### 权限资源表

```sql
-- ============================================
-- 权限资源主表（新建）
-- ============================================
CREATE TABLE `openplatform_v2_permission_t` (
    `id` BIGINT(20) PRIMARY KEY,
    `name_cn` VARCHAR(100) NOT NULL COMMENT '中文名称',
    `name_en` VARCHAR(100) NOT NULL COMMENT '英文名称',
    `scope` VARCHAR(100) NOT NULL UNIQUE COMMENT '权限标识，如 api:im:send-message',
    `resource_type` VARCHAR(20) NOT NULL COMMENT 'api, event, callback',
    `resource_id` BIGINT(20) NOT NULL COMMENT '关联的 API/Event/Callback ID',
    `category_id` BIGINT(20) NOT NULL COMMENT '所属分类ID',
    `status` TINYINT(10) DEFAULT 1 COMMENT '0=禁用, 1=启用',
    `create_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    `last_update_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    `create_by` VARCHAR(100),
    `last_update_by` VARCHAR(100),
    KEY `idx_resource` (`resource_type`, `resource_id`),
    KEY `idx_category_id` (`category_id`),
    KEY `idx_scope` (`scope`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限资源主表';

-- 权限资源属性表
CREATE TABLE `openplatform_v2_permission_p_t` (
    `id` BIGINT(20) PRIMARY KEY,
    `parent_id` BIGINT(20) NOT NULL COMMENT '关联权限主表 ID',
    `property_name` VARCHAR(100) NOT NULL COMMENT '属性名称',
    `property_value` TEXT COMMENT '属性值',
    `status` TINYINT(10) DEFAULT 1 COMMENT '0=禁用, 1=启用',
    `create_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    `last_update_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    `create_by` VARCHAR(100),
    `last_update_by` VARCHAR(100),
    KEY `idx_parent_id` (`parent_id`),
    KEY `idx_property_name` (`property_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限资源属性表';
```

### 属性表示例数据

```sql
-- API 属性表示例
INSERT INTO openplatform_v2_api_p_t (id, parent_id, property_name, property_value, status, ...) 
VALUES (1, 100, 'description_cn', '发送消息API的中文描述', 1, ...);
INSERT INTO openplatform_v2_api_p_t (id, parent_id, property_name, property_value, status, ...) 
VALUES (2, 100, 'description_en', 'Send message API description', 1, ...);
INSERT INTO openplatform_v2_api_p_t (id, parent_id, property_name, property_value, status, ...) 
VALUES (3, 100, 'doc_url', 'https://docs.example.com/api/send-message', 1, ...);

-- 权限属性表示例
INSERT INTO openplatform_v2_permission_p_t (id, parent_id, property_name, property_value, status, ...)
VALUES (1, 200, 'description_cn', '发送消息权限的中文描述', 1, ...);
INSERT INTO openplatform_v2_permission_p_t (id, parent_id, property_name, property_value, status, ...)
VALUES (2, 200, 'description_en', 'Send message permission description', 1, ...);
INSERT INTO openplatform_v2_permission_p_t (id, parent_id, property_name, property_value, status, ...)
VALUES (3, 200, 'approval_flow_id', '1001', 1, ...);
```

### 订阅关系表

```sql
-- ============================================
-- 订阅关系表（扩展现有 openplatform_app_permission_t）
-- ============================================
CREATE TABLE `openplatform_v2_subscription_t` (
    `id` BIGINT(20) PRIMARY KEY,
    `app_id` BIGINT(20) NOT NULL,
    `permission_id` BIGINT(20) NOT NULL,
    `status` TINYINT(10) DEFAULT 0 COMMENT '0=待审, 1=已授权, 2=已拒绝, 3=已取消',
    `channel_type` TINYINT(10) COMMENT '0=内部消息队列, 1=WebHook, 2=SSE, 3=WebSocket',
    `channel_address` VARCHAR(500),
    `auth_type` TINYINT(10) COMMENT '认证方式: 0=Cookie, 1=SOA, 2=APIG, 3=IAM, 4=免认证, 5=AKSK, 6=CLITOKEN',
    `create_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    `last_update_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    `create_by` VARCHAR(100),
    `last_update_by` VARCHAR(100),
    `approved_at` DATETIME(3),
    `approved_by` VARCHAR(100),
    UNIQUE KEY `uk_app_permission` (`app_id`, `permission_id`),
    KEY `idx_app_id` (`app_id`),
    KEY `idx_permission_id` (`permission_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订阅关系表';
```

### 审批相关表

```sql
-- ============================================
-- 审批流程模板表（扩展现有 openplatform_eflow_t）
-- ============================================
CREATE TABLE `openplatform_v2_approval_flow_t` (
    `id` BIGINT(20) PRIMARY KEY,
    `name_cn` VARCHAR(100) NOT NULL COMMENT '中文名称',
    `name_en` VARCHAR(100) NOT NULL COMMENT '英文名称',
    `code` VARCHAR(50) NOT NULL UNIQUE COMMENT 'default, api_register, permission_apply',
    `description_cn` TEXT COMMENT '中文描述',
    `description_en` TEXT COMMENT '英文描述',
    `is_default` TINYINT(10) DEFAULT 0 COMMENT '0=否, 1=是',
    `nodes` JSON NOT NULL COMMENT '审批节点配置',
    `status` TINYINT(10) DEFAULT 1 COMMENT '0=禁用, 1=启用',
    `create_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    `last_update_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    `create_by` VARCHAR(100),
    `last_update_by` VARCHAR(100)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批流程模板表';

-- ============================================
-- 审批记录表（扩展现有 openplatform_eflow_log_t）
-- ============================================
CREATE TABLE `openplatform_v2_approval_record_t` (
    `id` BIGINT(20) PRIMARY KEY,
    `flow_id` BIGINT(20) NOT NULL,
    `business_type` VARCHAR(50) NOT NULL COMMENT 'api_register, event_register, permission_apply',
    `business_id` BIGINT(20) NOT NULL,
    `applicant_id` VARCHAR(100) NOT NULL,
    `status` TINYINT(10) DEFAULT 0 COMMENT '0=待审, 1=已通过, 2=已拒绝, 3=已撤销',
    `current_node` INT DEFAULT 0,
    `create_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    `last_update_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    `create_by` VARCHAR(100),
    `last_update_by` VARCHAR(100),
    `completed_at` DATETIME(3),
    KEY `idx_flow_id` (`flow_id`),
    KEY `idx_business` (`business_type`, `business_id`),
    KEY `idx_applicant` (`applicant_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批记录表';

-- ============================================
-- 审批操作日志表
-- ============================================
CREATE TABLE `openplatform_v2_approval_log_t` (
    `id` BIGINT(20) PRIMARY KEY,
    `record_id` BIGINT(20) NOT NULL,
    `node_index` INT NOT NULL,
    `operator_id` VARCHAR(100) NOT NULL,
    `action` TINYINT(10) NOT NULL COMMENT '0=同意, 1=拒绝, 2=撤销, 3=转交',
    `comment` TEXT,
    `create_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    `last_update_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    `create_by` VARCHAR(100),
    `last_update_by` VARCHAR(100),
    KEY `idx_record_id` (`record_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批操作日志表';
```

### 用户授权表

```sql
-- ============================================
-- 用户授权表（Scope 授权）
-- ============================================
CREATE TABLE `openplatform_v2_user_authorization_t` (
    `id` BIGINT(20) PRIMARY KEY,
    `user_id` VARCHAR(100) NOT NULL,
    `app_id` BIGINT(20) NOT NULL,
    `scopes` JSON NOT NULL COMMENT '权限范围数组',
    `expires_at` DATETIME(3),
    `create_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    `last_update_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    `create_by` VARCHAR(100),
    `last_update_by` VARCHAR(100),
    `revoked_at` DATETIME(3),
    UNIQUE KEY `uk_user_app` (`user_id`, `app_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户授权表';
```

---

## 设计规范

### 命名规范

| 规则 | 说明 | 示例 |
|------|------|------|
| **表前缀** | 统一使用 `openplatform_v2_` 前缀，表示AI重写的新业务对象表 | `openplatform_v2_permission_api_t` |
| **表后缀** | 统一使用 `_t` 后缀表示表 | `openplatform_v2_eflow_t` |
| **属性表后缀** | 扩展属性表使用 `_p_t` 后缀 | `openplatform_v2_permission_api_p_t` |
| **命名风格** | 小写字母 + 下划线分隔 | `user_authorizations` → `openplatform_v2_user_authorization_t` |

### 字段规范

| 字段类型 | 说明 |
|----------|------|
| 主键 | `BIGINT(20)`，雪花ID，应用层生成 |
| 时间字段 | `DATETIME(3)` |
| 状态枚举 | `TINYINT(10)` |
| 人账号字段 | `VARCHAR(100)` |
| 名称/描述 | 中英文双语（`name_cn`/`name_en`，`description_cn`/`description_en`） |

### 枚举字段规范

本系统使用 `TINYINT(10)` 存储枚举值，遵循以下规范：

| 枚举字段 | 说明 | 枚举值定义 |
|----------|------|-----------|
| `status` | 资源状态 | 0=草稿, 1=待审, 2=已发布, 3=已下线 |
| `auth_type` | 认证方式 | 0=Cookie, 1=SOA, 2=APIG, 3=IAM, 4=免认证, 5=AKSK, 6=CLITOKEN |
| `channel_type` | 渠道类型 | 0=内部消息队列, 1=WebHook, 2=SSE, 3=WebSocket |
| `action` | 审批动作 | 0=同意, 1=拒绝, 2=撤销, 3=转交 |

**设计原则**：
- ✅ 使用数字编码（0, 1, 2...）而非字符串
- ✅ 在 COMMENT 中完整说明枚举值含义
- ✅ 枚举值连续，便于扩展
- ✅ 对应 Java 枚举类，提供类型安全访问

**枚举字段索引**：
- 频繁查询的枚举字段应添加索引（如 `idx_auth_type`）
- 使用 `KEY idx_xxx (field_name)` 而非 UNIQUE KEY

### 审计字段（必备）

每张表必备以下四个审计字段：

```sql
`create_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
`last_update_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
`create_by` VARCHAR(100),
`last_update_by` VARCHAR(100)
```
