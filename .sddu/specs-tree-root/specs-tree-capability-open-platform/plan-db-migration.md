# 数据迁移方案文档

> 本文档定义从旧版数据库表结构到 v2 版本的完整数据迁移方案。

---

## 1. 迁移策略概述

### 1.1 迁移原则

| 原则 | 说明 |
|------|------|
| **数据完整性** | 确保迁移过程中数据不丢失、不损坏，所有历史数据完整迁移 |
| **可回滚** | 每个迁移阶段都提供回滚脚本，确保迁移失败时可快速恢复 |
| **分阶段执行** | 按模块依赖关系分阶段迁移，每个阶段独立验证 |
| **最小停机** | 优先使用在线迁移，减少业务中断时间 |

### 1.2 迁移方式

采用 **停机迁移** 方式：

- **原因**：旧版 API 与权限强耦合，无法在线增量同步
- **停机窗口**：建议凌晨 2:00-6:00（4小时）
- **前提条件**：提前通知所有接入方，发布停机公告

### 1.3 迁移顺序

```
┌─────────────────────────────────────────────────────────────────┐
│  第1阶段：分类模块（无依赖）                                       │
│  openplatform_module_node_t → openplatform_v2_category_t       │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│  第2阶段：资源模块（依赖分类）                                     │
│  - API资源迁移                                                   │
│  - 事件资源迁移                                                  │
│  - 回调资源初始化（新增）                                         │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│  第3阶段：权限模块（依赖资源）                                     │
│  openplatform_permission_t → openplatform_v2_permission_t       │
│  核心：API与权限解耦                                              │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│  第4阶段：审批模块（依赖权限）                                     │
│  - 审批模板迁移                                                  │
│  - 审批记录迁移                                                  │
│  核心：模板与记录分离                                             │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│  第5阶段：业务关联模块                                             │
│  - 订阅关系迁移                                                  │
│  - 用户授权迁移                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### 1.4 时间窗口规划

| 阶段 | 模块 | 预估时间 | 累计时间 |
|------|------|----------|----------|
| 准备阶段 | 备份、建表、工具准备 | 30分钟 | 00:30 |
| 第1阶段 | 分类模块迁移 | 15分钟 | 00:45 |
| 第2阶段 | 资源模块迁移 | 30分钟 | 01:15 |
| 第3阶段 | 权限模块迁移 | 45分钟 | 02:00 |
| 第4阶段 | 审批模块迁移 | 45分钟 | 02:45 |
| 第5阶段 | 业务关联迁移 | 30分钟 | 03:15 |
| 验证阶段 | 数据验证、功能验证 | 30分钟 | 03:45 |
| 缓冲时间 | 异常处理 | 15分钟 | 04:00 |

---

## 2. 迁移前置准备

### 2.1 数据备份方案

```sql
-- ============================================
-- 备份所有历史表
-- 执行时间：迁移前30分钟
-- ============================================

-- 创建备份库
CREATE DATABASE IF NOT EXISTS `openplatform_backup_v1`;

-- 备份分类表
CREATE TABLE `openplatform_backup_v1`.`openplatform_module_node_t` 
AS SELECT * FROM `openplatform`.`openplatform_module_node_t`;

-- 备份权限相关表
CREATE TABLE `openplatform_backup_v1`.`openplatform_permission_t` 
AS SELECT * FROM `openplatform`.`openplatform_permission_t`;

CREATE TABLE `openplatform_backup_v1`.`openplatform_permission_p_t` 
AS SELECT * FROM `openplatform`.`openplatform_permission_p_t`;

-- 备份API相关表
CREATE TABLE `openplatform_backup_v1`.`openplatform_permission_api_t` 
AS SELECT * FROM `openplatform`.`openplatform_permission_api_t`;

CREATE TABLE `openplatform_backup_v1`.`openplatform_permission_api_p_t` 
AS SELECT * FROM `openplatform`.`openplatform_permission_api_p_t`;

-- 备份事件相关表
CREATE TABLE `openplatform_backup_v1`.`openplatform_event_t` 
AS SELECT * FROM `openplatform`.`openplatform_event_t`;

CREATE TABLE `openplatform_backup_v1`.`openplatform_event_p_t` 
AS SELECT * FROM `openplatform`.`openplatform_event_p_t`;

-- 备份审批相关表
CREATE TABLE `openplatform_backup_v1`.`openplatform_eflow_t` 
AS SELECT * FROM `openplatform`.`openplatform_eflow_t`;

CREATE TABLE `openplatform_backup_v1`.`openplatform_eflow_log_t` 
AS SELECT * FROM `openplatform`.`openplatform_eflow_log_t`;

CREATE TABLE `openplatform_backup_v1`.`openplatform_eflow_log_doc_t` 
AS SELECT * FROM `openplatform`.`openplatform_eflow_log_doc_t`;

-- 验证备份数据量
SELECT 
    'openplatform_module_node_t' AS table_name, 
    COUNT(*) AS backup_count 
FROM `openplatform_backup_v1`.`openplatform_module_node_t`
UNION ALL
SELECT 'openplatform_permission_t', COUNT(*) FROM `openplatform_backup_v1`.`openplatform_permission_t`
UNION ALL
SELECT 'openplatform_permission_api_t', COUNT(*) FROM `openplatform_backup_v1`.`openplatform_permission_api_t`
UNION ALL
SELECT 'openplatform_event_t', COUNT(*) FROM `openplatform_backup_v1`.`openplatform_event_t`
UNION ALL
SELECT 'openplatform_eflow_t', COUNT(*) FROM `openplatform_backup_v1`.`openplatform_eflow_t`;
```

### 2.2 新表结构创建

```sql
-- ============================================
-- 创建 v2 版本所有表
-- 执行时间：迁移前1天（预发布环境验证）
-- ============================================

-- 分类表
CREATE TABLE IF NOT EXISTS `openplatform_v2_category_t` (
    `id` BIGINT(20) PRIMARY KEY,
    `category_alias` VARCHAR(50) COMMENT '分类别名（仅根分类需要）',
    `name_cn` VARCHAR(100) NOT NULL COMMENT '中文名称',
    `name_en` VARCHAR(100) NOT NULL COMMENT '英文名称',
    `parent_id` BIGINT(20),
    `path` VARCHAR(500) COMMENT '路径：/根ID/父ID/当前ID/',
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
CREATE TABLE IF NOT EXISTS `openplatform_v2_category_owner_t` (
    `id` BIGINT(20) PRIMARY KEY,
    `category_id` BIGINT(20) NOT NULL,
    `user_id` VARCHAR(100) NOT NULL,
    `create_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    `last_update_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    `create_by` VARCHAR(100),
    `last_update_by` VARCHAR(100),
    UNIQUE KEY `uk_category_user` (`category_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分类责任人关联表';

-- API资源主表
CREATE TABLE IF NOT EXISTS `openplatform_v2_api_t` (
    `id` BIGINT(20) PRIMARY KEY,
    `name_cn` VARCHAR(100) NOT NULL COMMENT '中文名称',
    `name_en` VARCHAR(100) NOT NULL COMMENT '英文名称',
    `path` VARCHAR(500) NOT NULL COMMENT 'API路径',
    `method` VARCHAR(10) NOT NULL COMMENT 'HTTP方法',
    `auth_type` TINYINT(10) NOT NULL DEFAULT 1 COMMENT '认证方式',
    `status` TINYINT(10) DEFAULT 0 COMMENT '0=草稿, 1=待审, 2=已发布, 3=已下线',
    `create_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    `last_update_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    `create_by` VARCHAR(100),
    `last_update_by` VARCHAR(100),
    KEY `idx_auth_type` (`auth_type`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='API资源主表';

-- API资源属性表
CREATE TABLE IF NOT EXISTS `openplatform_v2_api_p_t` (
    `id` BIGINT(20) PRIMARY KEY,
    `parent_id` BIGINT(20) NOT NULL,
    `property_name` VARCHAR(100) NOT NULL,
    `property_value` TEXT,
    `status` TINYINT(10) DEFAULT 1,
    `create_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    `last_update_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    `create_by` VARCHAR(100),
    `last_update_by` VARCHAR(100),
    KEY `idx_parent_id` (`parent_id`),
    KEY `idx_property_name` (`property_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='API资源属性表';

-- 事件资源主表
CREATE TABLE IF NOT EXISTS `openplatform_v2_event_t` (
    `id` BIGINT(20) PRIMARY KEY,
    `name_cn` VARCHAR(100) NOT NULL,
    `name_en` VARCHAR(100) NOT NULL,
    `topic` VARCHAR(200) NOT NULL UNIQUE,
    `status` TINYINT(10) DEFAULT 0,
    `create_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    `last_update_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    `create_by` VARCHAR(100),
    `last_update_by` VARCHAR(100),
    KEY `idx_topic` (`topic`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='事件资源主表';

-- 事件资源属性表
CREATE TABLE IF NOT EXISTS `openplatform_v2_event_p_t` (
    `id` BIGINT(20) PRIMARY KEY,
    `parent_id` BIGINT(20) NOT NULL,
    `property_name` VARCHAR(100) NOT NULL,
    `property_value` TEXT,
    `status` TINYINT(10) DEFAULT 1,
    `create_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    `last_update_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    `create_by` VARCHAR(100),
    `last_update_by` VARCHAR(100),
    KEY `idx_parent_id` (`parent_id`),
    KEY `idx_property_name` (`property_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='事件资源属性表';

-- 回调资源主表
CREATE TABLE IF NOT EXISTS `openplatform_v2_callback_t` (
    `id` BIGINT(20) PRIMARY KEY,
    `name_cn` VARCHAR(100) NOT NULL,
    `name_en` VARCHAR(100) NOT NULL,
    `status` TINYINT(10) DEFAULT 0,
    `create_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    `last_update_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    `create_by` VARCHAR(100),
    `last_update_by` VARCHAR(100),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='回调资源主表';

-- 回调资源属性表
CREATE TABLE IF NOT EXISTS `openplatform_v2_callback_p_t` (
    `id` BIGINT(20) PRIMARY KEY,
    `parent_id` BIGINT(20) NOT NULL,
    `property_name` VARCHAR(100) NOT NULL,
    `property_value` TEXT,
    `status` TINYINT(10) DEFAULT 1,
    `create_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    `last_update_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    `create_by` VARCHAR(100),
    `last_update_by` VARCHAR(100),
    KEY `idx_parent_id` (`parent_id`),
    KEY `idx_property_name` (`property_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='回调资源属性表';

-- 权限资源主表
CREATE TABLE IF NOT EXISTS `openplatform_v2_permission_t` (
    `id` BIGINT(20) PRIMARY KEY,
    `name_cn` VARCHAR(100) NOT NULL,
    `name_en` VARCHAR(100) NOT NULL,
    `scope` VARCHAR(100) NOT NULL UNIQUE,
    `resource_type` VARCHAR(20) NOT NULL COMMENT 'api, event, callback',
    `resource_id` BIGINT(20) NOT NULL,
    `category_id` BIGINT(20) NOT NULL,
    `need_approval` TINYINT(10) DEFAULT 1,
    `resource_nodes` VARCHAR(2000),
    `status` TINYINT(10) DEFAULT 1,
    `create_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    `last_update_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    `create_by` VARCHAR(100),
    `last_update_by` VARCHAR(100),
    KEY `idx_resource` (`resource_type`, `resource_id`),
    KEY `idx_category_id` (`category_id`),
    KEY `idx_scope` (`scope`),
    KEY `idx_status` (`status`),
    KEY `idx_need_approval` (`need_approval`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限资源主表';

-- 权限资源属性表
CREATE TABLE IF NOT EXISTS `openplatform_v2_permission_p_t` (
    `id` BIGINT(20) PRIMARY KEY,
    `parent_id` BIGINT(20) NOT NULL,
    `property_name` VARCHAR(100) NOT NULL,
    `property_value` TEXT,
    `status` TINYINT(10) DEFAULT 1,
    `create_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    `last_update_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    `create_by` VARCHAR(100),
    `last_update_by` VARCHAR(100),
    KEY `idx_parent_id` (`parent_id`),
    KEY `idx_property_name` (`property_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限资源属性表';

-- 订阅关系表
CREATE TABLE IF NOT EXISTS `openplatform_v2_subscription_t` (
    `id` BIGINT(20) PRIMARY KEY,
    `app_id` BIGINT(20) NOT NULL,
    `permission_id` BIGINT(20) NOT NULL,
    `status` TINYINT(10) DEFAULT 0,
    `channel_type` TINYINT(10),
    `channel_address` VARCHAR(500),
    `auth_type` TINYINT(10),
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

-- 审批流程模板表
CREATE TABLE IF NOT EXISTS `openplatform_v2_approval_flow_t` (
    `id` BIGINT(20) PRIMARY KEY,
    `name_cn` VARCHAR(100) NOT NULL,
    `name_en` VARCHAR(100) NOT NULL,
    `code` VARCHAR(50) NOT NULL UNIQUE,
    `description_cn` TEXT,
    `description_en` TEXT,
    `nodes` VARCHAR(2000) NOT NULL,
    `status` TINYINT(10) DEFAULT 1,
    `create_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    `last_update_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    `create_by` VARCHAR(100),
    `last_update_by` VARCHAR(100),
    UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批流程模板表';

-- 审批记录表
CREATE TABLE IF NOT EXISTS `openplatform_v2_approval_record_t` (
    `id` BIGINT(20) PRIMARY KEY,
    `combined_nodes` VARCHAR(4000) NOT NULL,
    `business_type` VARCHAR(50) NOT NULL,
    `business_id` BIGINT(20) NOT NULL,
    `applicant_id` VARCHAR(100) NOT NULL,
    `applicant_name` VARCHAR(100),
    `status` TINYINT(10) DEFAULT 0,
    `current_node` INT DEFAULT 0,
    `create_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    `last_update_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    `create_by` VARCHAR(100),
    `last_update_by` VARCHAR(100),
    `completed_at` DATETIME(3),
    KEY `idx_business` (`business_type`, `business_id`),
    KEY `idx_applicant` (`applicant_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批记录表';

-- 审批操作日志表
CREATE TABLE IF NOT EXISTS `openplatform_v2_approval_log_t` (
    `id` BIGINT(20) PRIMARY KEY,
    `record_id` BIGINT(20) NOT NULL,
    `node_index` INT NOT NULL,
    `level` VARCHAR(20),
    `operator_id` VARCHAR(100) NOT NULL,
    `operator_name` VARCHAR(100),
    `action` TINYINT(10) NOT NULL,
    `comment` TEXT,
    `create_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    `last_update_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    `create_by` VARCHAR(100),
    `last_update_by` VARCHAR(100),
    KEY `idx_record_id` (`record_id`),
    KEY `idx_level` (`level`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批操作日志表';

-- 用户授权表
CREATE TABLE IF NOT EXISTS `openplatform_v2_user_authorization_t` (
    `id` BIGINT(20) PRIMARY KEY,
    `user_id` VARCHAR(100) NOT NULL,
    `app_id` BIGINT(20) NOT NULL,
    `scopes` JSON NOT NULL,
    `expires_at` DATETIME(3),
    `create_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    `last_update_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    `create_by` VARCHAR(100),
    `last_update_by` VARCHAR(100),
    `revoked_at` DATETIME(3),
    UNIQUE KEY `uk_user_app` (`user_id`, `app_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户授权表';
```

### 2.3 迁移工具准备

```bash
#!/bin/bash
# ============================================
# 迁移工具检查脚本
# ============================================

# 检查 MySQL 客户端
mysql --version
if [ $? -ne 0 ]; then
    echo "ERROR: MySQL client not installed"
    exit 1
fi

# 检查备份库是否存在
mysql -e "SELECT SCHEMA_NAME FROM information_schema.SCHEMATA WHERE SCHEMA_NAME='openplatform_backup_v1'"
if [ $? -ne 0 ]; then
    echo "ERROR: Backup database not found"
    exit 1
fi

# 检查 v2 表是否创建
TABLE_COUNT=$(mysql -N -e "
    SELECT COUNT(*) FROM information_schema.TABLES 
    WHERE TABLE_SCHEMA='openplatform' 
    AND TABLE_NAME LIKE 'openplatform_v2_%'
")

if [ $TABLE_COUNT -lt 15 ]; then
    echo "ERROR: v2 tables not fully created. Expected 15, found $TABLE_COUNT"
    exit 1
fi

echo "Migration tools check passed"
```

### 2.4 验证脚本准备

```sql
-- ============================================
-- 数据验证基础脚本模板
-- ============================================

-- 验证记录表（迁移过程中记录）
CREATE TABLE IF NOT EXISTS `openplatform_migration_record_t` (
    `id` BIGINT(20) PRIMARY KEY,
    `module` VARCHAR(50) NOT NULL COMMENT '模块名称',
    `source_table` VARCHAR(100) NOT NULL COMMENT '源表名',
    `target_table` VARCHAR(100) NOT NULL COMMENT '目标表名',
    `source_count` INT NOT NULL COMMENT '源表数据量',
    `target_count` INT NOT NULL COMMENT '目标表数据量',
    `status` TINYINT(10) DEFAULT 0 COMMENT '0=待验证, 1=通过, 2=失败',
    `error_message` TEXT COMMENT '错误信息',
    `create_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    KEY `idx_module` (`module`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='迁移验证记录表';
```

---

## 3. 各模块迁移方案

### 3.1 分类模块迁移

#### 3.1.1 迁移概述

| 项目 | 说明 |
|------|------|
| 源表 | `openplatform_module_node_t` |
| 目标表 | `openplatform_v2_category_t` + `openplatform_v2_category_owner_t` |
| 数据量预估 | 根据实际业务确定 |
| 迁移时长预估 | 15分钟 |

#### 3.1.2 字段映射关系

| 源字段 | 目标字段 | 转换规则 |
|--------|----------|----------|
| `id` | `id` | 直接映射 |
| `node_name_cn` | `name_cn` | 直接映射 |
| `node_name_en` | `name_en` | 直接映射 |
| `parent_Node_id` | `parent_id` | 修正大小写问题 |
| `order_num` | `sort_order` | 字段重命名 |
| `status` | `status` | 直接映射（0=禁用, 1=启用） |
| `create_by` | `create_by` | 直接映射 |
| `create_time` | `create_time` | 直接映射 |
| `last_update_by` | `last_update_by` | 直接映射 |
| `last_update_time` | `last_update_time` | 直接映射 |
| - | `category_alias` | **新增字段**：仅根分类填充，根据业务规则生成 |
| - | `path` | **新增字段**：计算路径字符串 |

#### 3.1.3 新字段填充规则

**category_alias 填充规则**：
```sql
-- 仅根分类（parent_id IS NULL 或 parent_id = 0）需要填充 category_alias
-- 根据业务规则，基于 name_en 或业务约定生成唯一别名
-- 示例：app_type_a, app_type_b, personal_aksk

-- 自动生成规则（可根据实际业务调整）：
-- 1. 如果 name_en 已有明确标识，转换为小写下划线格式
-- 2. 否则，基于 id 生成：category_{id}
```

**path 填充规则**：
```sql
-- 路径格式：/根ID/父ID/当前ID/
-- 示例：/1/2/3/ 表示 ID=3 的分类，其根分类ID=1，父分类ID=2

-- 计算逻辑：
-- 1. 根分类：path = CONCAT('/', id, '/')
-- 2. 子分类：path = CONCAT(父分类.path, id, '/')
```

#### 3.1.4 迁移SQL脚本

```sql
-- ============================================
-- 分类模块迁移脚本
-- 执行顺序：1
-- ============================================

-- 步骤1：清空目标表（如已有测试数据）
TRUNCATE TABLE `openplatform_v2_category_t`;
TRUNCATE TABLE `openplatform_v2_category_owner_t`;

-- 步骤2：迁移基础数据（不包含新字段）
INSERT INTO `openplatform_v2_category_t` (
    `id`,
    `name_cn`,
    `name_en`,
    `parent_id`,
    `sort_order`,
    `status`,
    `create_by`,
    `create_time`,
    `last_update_by`,
    `last_update_time`
)
SELECT 
    `id`,
    `node_name_cn`,
    `node_name_en`,
    `parent_Node_id`,  -- 注意大小写修正
    `order_num`,
    `status`,
    `create_by`,
    `create_time`,
    `last_update_by`,
    `last_update_time`
FROM `openplatform_module_node_t`
WHERE `status` = 1;  -- 仅迁移有效数据

-- 步骤3：填充 category_alias（仅根分类）
-- 方案A：基于业务规则手动指定（推荐）
UPDATE `openplatform_v2_category_t` 
SET `category_alias` = 'app_type_a'
WHERE `parent_id` IS NULL OR `parent_id` = 0
AND `name_cn` LIKE '%应用类型A%';

UPDATE `openplatform_v2_category_t` 
SET `category_alias` = 'app_type_b'
WHERE `parent_id` IS NULL OR `parent_id` = 0
AND `name_cn` LIKE '%应用类型B%';

-- 方案B：自动生成（备选）
UPDATE `openplatform_v2_category_t` 
SET `category_alias` = LOWER(REPLACE(REPLACE(`name_en`, ' ', '_'), '-', '_'))
WHERE (`parent_id` IS NULL OR `parent_id` = 0)
AND `category_alias` IS NULL;

-- 步骤4：计算并填充 path 字段（递归更新）
-- 注意：MySQL 5.7 不支持递归 CTE，需要使用存储过程或多次更新

-- 4.1 先更新根分类的 path
UPDATE `openplatform_v2_category_t`
SET `path` = CONCAT('/', `id`, '/')
WHERE `parent_id` IS NULL OR `parent_id` = 0;

-- 4.2 更新第2层分类
UPDATE `openplatform_v2_category_t` c
INNER JOIN `openplatform_v2_category_t` p ON c.`parent_id` = p.`id`
SET c.`path` = CONCAT(p.`path`, c.`id`, '/')
WHERE p.`parent_id` IS NULL OR p.`parent_id` = 0;

-- 4.3 更新第3层分类
UPDATE `openplatform_v2_category_t` c
INNER JOIN `openplatform_v2_category_t` p ON c.`parent_id` = p.`id`
INNER JOIN `openplatform_v2_category_t` gp ON p.`parent_id` = gp.`id`
SET c.`path` = CONCAT(p.`path`, c.`id`, '/')
WHERE gp.`parent_id` IS NULL OR gp.`parent_id` = 0;

-- 4.4 更新第4层分类（如有需要继续）
UPDATE `openplatform_v2_category_t` c
INNER JOIN `openplatform_v2_category_t` p ON c.`parent_id` = p.`id`
INNER JOIN `openplatform_v2_category_t` gp ON p.`parent_id` = gp.`id`
INNER JOIN `openplatform_v2_category_t` ggp ON gp.`parent_id` = ggp.`id`
SET c.`path` = CONCAT(p.`path`, c.`id`, '/')
WHERE ggp.`parent_id` IS NULL OR ggp.`parent_id` = 0;

-- 步骤5：处理分类责任人（如有源数据）
-- 注意：旧表结构中没有分类责任人数据，需要根据业务规则手动配置
-- 或从其他数据源导入

-- 示例：根据审批用户配置责任人
INSERT INTO `openplatform_v2_category_owner_t` (
    `id`,
    `category_id`,
    `user_id`,
    `create_time`,
    `last_update_time`
)
SELECT 
    ROW_NUMBER() OVER (ORDER BY c.id) AS id,
    c.`id` AS category_id,
    pp.`property_value` AS user_id,
    NOW(3) AS create_time,
    NOW(3) AS last_update_time
FROM `openplatform_v2_category_t` c
INNER JOIN `openplatform_permission_t` p ON p.`module_id` = c.`id`
INNER JOIN `openplatform_permission_p_t` pp ON pp.`parent_id` = p.`id` AND pp.`property_name` = 'audit_user'
WHERE pp.`status` = 1
GROUP BY c.`id`, pp.`property_value`;
```

#### 3.1.5 数据验证SQL

```sql
-- ============================================
-- 分类模块验证脚本
-- ============================================

-- 验证1：数据量对比
SELECT 
    '源表数据量' AS type,
    COUNT(*) AS count 
FROM `openplatform_module_node_t` WHERE `status` = 1
UNION ALL
SELECT 
    '目标表数据量',
    COUNT(*) 
FROM `openplatform_v2_category_t`;

-- 验证2：检查 category_alias 是否填充
SELECT 
    '根分类数量' AS type,
    COUNT(*) AS count 
FROM `openplatform_v2_category_t`
WHERE `parent_id` IS NULL OR `parent_id` = 0
UNION ALL
SELECT 
    '已填充alias的根分类',
    COUNT(*) 
FROM `openplatform_v2_category_t`
WHERE (`parent_id` IS NULL OR `parent_id` = 0)
AND `category_alias` IS NOT NULL;

-- 验证3：检查 path 字段是否填充
SELECT 
    '已填充path的分类数量' AS type,
    COUNT(*) AS count 
FROM `openplatform_v2_category_t`
WHERE `path` IS NOT NULL AND `path` != '';

-- 验证4：path 格式验证
SELECT 
    'path格式异常的分类' AS type,
    `id`,
    `name_cn`,
    `parent_id`,
    `path`
FROM `openplatform_v2_category_t`
WHERE `path` NOT LIKE '/%/' OR `path` IS NULL;

-- 验证5：数据一致性校验
SELECT 
    o.`id` AS old_id,
    o.`node_name_cn` AS old_name_cn,
    n.`id` AS new_id,
    n.`name_cn` AS new_name_cn,
    CASE 
        WHEN o.`node_name_cn` = n.`name_cn` THEN '一致'
        ELSE '不一致'
    END AS check_result
FROM `openplatform_module_node_t` o
LEFT JOIN `openplatform_v2_category_t` n ON o.`id` = n.`id`
WHERE o.`status` = 1
HAVING check_result = '不一致';

-- 记录验证结果
INSERT INTO `openplatform_migration_record_t` (
    `module`, `source_table`, `target_table`, 
    `source_count`, `target_count`, `status`
)
SELECT 
    '分类模块',
    'openplatform_module_node_t',
    'openplatform_v2_category_t',
    (SELECT COUNT(*) FROM `openplatform_module_node_t` WHERE `status` = 1),
    (SELECT COUNT(*) FROM `openplatform_v2_category_t`),
    CASE 
        WHEN (SELECT COUNT(*) FROM `openplatform_module_node_t` WHERE `status` = 1) = 
             (SELECT COUNT(*) FROM `openplatform_v2_category_t`) 
        THEN 1 
        ELSE 2 
    END;
```

---

### 3.2 API资源迁移

#### 3.2.1 迁移概述

| 项目 | 说明 |
|------|------|
| 源表 | `openplatform_permission_api_t` + `openplatform_permission_api_p_t` |
| 目标表 | `openplatform_v2_api_t` + `openplatform_v2_api_p_t` |
| 核心变化 | API 与权限解耦，API 成为独立资源 |
| 数据量预估 | 根据实际业务确定 |
| 迁移时长预估 | 20分钟 |

#### 3.2.2 字段映射关系

| 源字段 | 目标字段 | 转换规则 |
|--------|----------|----------|
| `id` | `id` | 直接映射 |
| `api_name_cn` | `name_cn` | 直接映射 |
| `api_name_en` | `name_en` | 直接映射 |
| `api_path` | `path` | 去除前缀冗余 |
| `api_method` | `method` | 去除前缀冗余 |
| `auth_type` | `auth_type` | 直接映射 |
| `status` | `status` | **状态映射**（见下表） |
| `create_by` | `create_by` | 直接映射 |
| `create_time` | `create_time` | 直接映射 |
| `last_update_by` | `last_update_by` | 直接映射 |
| `last_update_time` | `last_update_time` | 直接映射 |
| `permission_id` | - | **废弃**：不再与权限强关联 |

#### 3.2.3 状态映射规则

| 源 status | 含义 | 目标 status | 含义 |
|-----------|------|-------------|------|
| 0 | 禁用 | 3 | 已下线 |
| 1 | 启用 | 2 | 已发布 |

**说明**：旧版 status 只有启用/禁用两种状态，v2 扩展为草稿/待审/已发布/已下线四种状态。迁移时：
- 旧版 status=1（启用）→ v2 status=2（已发布）
- 旧版 status=0（禁用）→ v2 status=3（已下线）

#### 3.2.4 迁移SQL脚本

```sql
-- ============================================
-- API资源迁移脚本
-- 执行顺序：2
-- ============================================

-- 步骤1：清空目标表
TRUNCATE TABLE `openplatform_v2_api_t`;
TRUNCATE TABLE `openplatform_v2_api_p_t`;

-- 步骤2：迁移API主表数据
INSERT INTO `openplatform_v2_api_t` (
    `id`,
    `name_cn`,
    `name_en`,
    `path`,
    `method`,
    `auth_type`,
    `status`,
    `create_by`,
    `create_time`,
    `last_update_by`,
    `last_update_time`
)
SELECT 
    `id`,
    `api_name_cn`,
    `api_name_en`,
    `api_path`,
    `api_method`,
    `auth_type`,
    CASE 
        WHEN `status` = 1 THEN 2  -- 启用 → 已发布
        WHEN `status` = 0 THEN 3  -- 禁用 → 已下线
        ELSE 2
    END AS status,
    `create_by`,
    `create_time`,
    `last_update_by`,
    `last_update_time`
FROM `openplatform_permission_api_t`;

-- 步骤3：迁移API属性表数据
INSERT INTO `openplatform_v2_api_p_t` (
    `id`,
    `parent_id`,
    `property_name`,
    `property_value`,
    `status`,
    `create_by`,
    `create_time`,
    `last_update_by`,
    `last_update_time`
)
SELECT 
    `id`,
    `parent_id`,
    `property_name`,
    `property_value`,
    `status`,
    `create_by`,
    `create_time`,
    `last_update_by`,
    `last_update_time`
FROM `openplatform_permission_api_p_t`;

-- 步骤4：建立旧API ID与新API ID的映射关系（供权限迁移使用）
CREATE TEMPORARY TABLE IF NOT EXISTS `tmp_api_id_mapping` (
    `old_api_id` BIGINT(20),
    `new_api_id` BIGINT(20),
    `permission_id` BIGINT(20)
);

INSERT INTO `tmp_api_id_mapping` (`old_api_id`, `new_api_id`, `permission_id`)
SELECT 
    old.`id` AS old_api_id,
    new.`id` AS new_api_id,
    old.`permission_id`
FROM `openplatform_permission_api_t` old
INNER JOIN `openplatform_v2_api_t` new ON old.`id` = new.`id`;
```

#### 3.2.5 数据验证SQL

```sql
-- ============================================
-- API资源验证脚本
-- ============================================

-- 验证1：数据量对比
SELECT 
    '源API主表数据量' AS type,
    COUNT(*) AS count 
FROM `openplatform_permission_api_t`
UNION ALL
SELECT 
    '目标API主表数据量',
    COUNT(*) 
FROM `openplatform_v2_api_t`
UNION ALL
SELECT 
    '源API属性表数据量',
    COUNT(*) 
FROM `openplatform_permission_api_p_t`
UNION ALL
SELECT 
    '目标API属性表数据量',
    COUNT(*) 
FROM `openplatform_v2_api_p_t`;

-- 验证2：状态映射验证
SELECT 
    '状态分布对比' AS type,
    old_status,
    new_status,
    COUNT(*) AS count
FROM (
    SELECT 
        old.`status` AS old_status,
        new.`status` AS new_status
    FROM `openplatform_permission_api_t` old
    INNER JOIN `openplatform_v2_api_t` new ON old.`id` = new.`id`
) t
GROUP BY old_status, new_status;

-- 验证3：字段映射一致性
SELECT 
    '字段映射异常的API' AS type,
    old.`id`,
    old.`api_name_cn` AS old_name_cn,
    new.`name_cn` AS new_name_cn
FROM `openplatform_permission_api_t` old
INNER JOIN `openplatform_v2_api_t` new ON old.`id` = new.`id`
WHERE old.`api_name_cn` != new.`name_cn`
   OR old.`api_path` != new.`path`
   OR old.`api_method` != new.`method`;

-- 记录验证结果
INSERT INTO `openplatform_migration_record_t` (
    `module`, `source_table`, `target_table`, 
    `source_count`, `target_count`, `status`
)
VALUES (
    'API资源模块',
    'openplatform_permission_api_t',
    'openplatform_v2_api_t',
    (SELECT COUNT(*) FROM `openplatform_permission_api_t`),
    (SELECT COUNT(*) FROM `openplatform_v2_api_t`),
    CASE 
        WHEN (SELECT COUNT(*) FROM `openplatform_permission_api_t`) = 
             (SELECT COUNT(*) FROM `openplatform_v2_api_t`) 
        THEN 1 
        ELSE 2 
    END
);
```

---

### 3.3 事件资源迁移

#### 3.3.1 迁移概述

| 项目 | 说明 |
|------|------|
| 源表 | `openplatform_event_t` + `openplatform_event_p_t` |
| 目标表 | `openplatform_v2_event_t` + `openplatform_v2_event_p_t` |
| 核心变化 | 移除 event_type 冗余字段，事件与权限建立关联 |
| 数据量预估 | 根据实际业务确定 |
| 迁移时长预估 | 10分钟 |

#### 3.3.2 字段映射关系

| 源字段 | 目标字段 | 转换规则 |
|--------|----------|----------|
| `id` | `id` | 直接映射 |
| `event_name_cn` | `name_cn` | 直接映射 |
| `event_name_en` | `name_en` | 直接映射 |
| `topic` | `topic` | 直接映射 |
| `status` | `status` | 状态映射（同API） |
| `create_by` | `create_by` | 直接映射 |
| `create_time` | `create_time` | 直接映射 |
| `last_update_by` | `last_update_by` | 直接映射 |
| `last_update_time` | `last_update_time` | 直接映射 |
| `module_id` | - | **废弃**：分类关联移至权限表 |
| `event_type` | - | **废弃**：冗余字段移除 |
| `is_approval_required` | - | **废弃**：审批配置移至权限表 |

#### 3.3.3 迁移SQL脚本

```sql
-- ============================================
-- 事件资源迁移脚本
-- 执行顺序：3
-- ============================================

-- 步骤1：清空目标表
TRUNCATE TABLE `openplatform_v2_event_t`;
TRUNCATE TABLE `openplatform_v2_event_p_t`;

-- 步骤2：迁移事件主表数据
INSERT INTO `openplatform_v2_event_t` (
    `id`,
    `name_cn`,
    `name_en`,
    `topic`,
    `status`,
    `create_by`,
    `create_time`,
    `last_update_by`,
    `last_update_time`
)
SELECT 
    `id`,
    `event_name_cn`,
    `event_name_en`,
    `topic`,
    CASE 
        WHEN `status` = 1 THEN 2  -- 启用 → 已发布
        WHEN `status` = 0 THEN 3  -- 禁用 → 已下线
        ELSE 2
    END AS status,
    `create_by`,
    `create_time`,
    `last_update_by`,
    `last_update_time`
FROM `openplatform_event_t`;

-- 步骤3：迁移事件属性表数据
INSERT INTO `openplatform_v2_event_p_t` (
    `id`,
    `parent_id`,
    `property_name`,
    `property_value`,
    `status`,
    `create_by`,
    `create_time`,
    `last_update_by`,
    `last_update_time`
)
SELECT 
    `id`,
    `parent_id`,
    `property_name`,
    `property_value`,
    `status`,
    `create_by`,
    `create_time`,
    `last_update_by`,
    `last_update_time`
FROM `openplatform_event_p_t`;

-- 步骤4：建立旧事件ID与新事件ID的映射关系（供权限迁移使用）
CREATE TEMPORARY TABLE IF NOT EXISTS `tmp_event_id_mapping` (
    `old_event_id` BIGINT(20),
    `new_event_id` BIGINT(20),
    `module_id` BIGINT(20)
);

INSERT INTO `tmp_event_id_mapping` (`old_event_id`, `new_event_id`, `module_id`)
SELECT 
    old.`id` AS old_event_id,
    new.`id` AS new_event_id,
    old.`module_id`
FROM `openplatform_event_t` old
INNER JOIN `openplatform_v2_event_t` new ON old.`id` = new.`id`;
```

#### 3.3.4 数据验证SQL

```sql
-- ============================================
-- 事件资源验证脚本
-- ============================================

-- 验证1：数据量对比
SELECT 
    '源事件主表数据量' AS type,
    COUNT(*) AS count 
FROM `openplatform_event_t`
UNION ALL
SELECT 
    '目标事件主表数据量',
    COUNT(*) 
FROM `openplatform_v2_event_t`;

-- 验证2：topic唯一性验证
SELECT 
    '重复的topic' AS type,
    `topic`,
    COUNT(*) AS count
FROM `openplatform_v2_event_t`
GROUP BY `topic`
HAVING COUNT(*) > 1;

-- 验证3：数据一致性校验
SELECT 
    old.`id`,
    old.`event_name_cn` AS old_name_cn,
    new.`name_cn` AS new_name_cn,
    old.`topic` AS old_topic,
    new.`topic` AS new_topic
FROM `openplatform_event_t` old
INNER JOIN `openplatform_v2_event_t` new ON old.`id` = new.`id`
WHERE old.`event_name_cn` != new.`name_cn`
   OR old.`topic` != new.`topic`;

-- 记录验证结果
INSERT INTO `openplatform_migration_record_t` (
    `module`, `source_table`, `target_table`, 
    `source_count`, `target_count`, `status`
)
VALUES (
    '事件资源模块',
    'openplatform_event_t',
    'openplatform_v2_event_t',
    (SELECT COUNT(*) FROM `openplatform_event_t`),
    (SELECT COUNT(*) FROM `openplatform_v2_event_t`),
    CASE 
        WHEN (SELECT COUNT(*) FROM `openplatform_event_t`) = 
             (SELECT COUNT(*) FROM `openplatform_v2_event_t`) 
        THEN 1 
        ELSE 2 
    END
);
```

---

### 3.4 回调资源初始化

#### 3.4.1 迁移说明

**回调资源为 v2 新增功能，无历史数据迁移需求。**

需要根据业务需求初始化默认的回调资源配置。

#### 3.4.2 初始化建议

```sql
-- ============================================
-- 回调资源初始化脚本
-- 执行顺序：4
-- ============================================

-- 清空目标表（如有测试数据）
TRUNCATE TABLE `openplatform_v2_callback_t`;
TRUNCATE TABLE `openplatform_v2_callback_p_t`;

-- 初始化默认回调资源（根据实际业务配置）
INSERT INTO `openplatform_v2_callback_t` (
    `id`, `name_cn`, `name_en`, `status`, 
    `create_time`, `last_update_time`, `create_by`, `last_update_by`
) VALUES 
(1, '订单状态变更回调', 'order_status_changed', 2, NOW(3), NOW(3), 'system', 'system'),
(2, '支付结果通知回调', 'payment_result_notify', 2, NOW(3), NOW(3), 'system', 'system'),
(3, '用户信息变更回调', 'user_info_changed', 2, NOW(3), NOW(3), 'system', 'system');

-- 初始化回调属性
INSERT INTO `openplatform_v2_callback_p_t` (
    `id`, `parent_id`, `property_name`, `property_value`, `status`,
    `create_time`, `last_update_time`, `create_by`, `last_update_by`
) VALUES 
(1, 1, 'description_cn', '当订单状态发生变更时触发此回调', 1, NOW(3), NOW(3), 'system', 'system'),
(2, 1, 'description_en', 'Triggered when order status changes', 1, NOW(3), NOW(3), 'system', 'system'),
(3, 2, 'description_cn', '当支付完成时触发此回调', 1, NOW(3), NOW(3), 'system', 'system'),
(4, 2, 'description_en', 'Triggered when payment is completed', 1, NOW(3), NOW(3), 'system', 'system');
```

---

### 3.5 权限资源迁移（重点）

#### 3.5.1 迁移概述

| 项目 | 说明 |
|------|------|
| 源表 | `openplatform_permission_t` + `openplatform_permission_p_t` + `openplatform_permission_api_t` |
| 目标表 | `openplatform_v2_permission_t` + `openplatform_v2_permission_p_t` |
| 核心变化 | API与权限解耦，需要重建关联关系 |
| 迁移难度 | ⭐⭐⭐⭐⭐（最高） |
| 数据量预估 | 根据实际业务确定 |
| 迁移时长预估 | 45分钟 |

#### 3.5.2 核心迁移逻辑

**旧设计问题**：
- API 和权限通过 `permission_id` 强耦合
- 权限类型字段 `permisssion_type` 有拼写错误
- 缺少明确的资源关联关系

**v2 设计改进**：
- API 作为独立资源管理
- 权限通过 `resource_type` 和 `resource_id` 关联资源
- 支持多级审批配置

**迁移策略**：
1. 迁移原有权限主表数据
2. 为每个 API 创建对应的权限记录（resource_type='api'）
3. 为每个事件创建对应的权限记录（resource_type='event'）
4. 转换审批配置字段

#### 3.5.3 字段映射关系

| 源字段 | 目标字段 | 转换规则 |
|--------|----------|----------|
| `id` | `id` | 直接映射（但需重新分配） |
| `permission_name_cn` | `name_cn` | 直接映射 |
| `permission_name_en` | `name_en` | 直接映射 |
| `scope_id` | `scope` | 直接映射 |
| `permisssion_type` | `resource_type` | 拼写修正，值转换 |
| `module_id` | `category_id` | 字段重命名 |
| `status` | `status` | 直接映射 |
| `create_by` | `create_by` | 直接映射 |
| `create_time` | `create_time` | 直接映射 |
| `last_update_by` | `last_update_by` | 直接映射 |
| `last_update_time` | `last_update_time` | 直接映射 |
| - | `resource_id` | **新增**：关联的资源ID |
| `is_approval_required` | `need_approval` | 字段重命名 |
| - | `resource_nodes` | **新增**：从属性表转换 |

#### 3.5.4 resource_type 值转换规则

| 源 permisssion_type | 目标 resource_type | 说明 |
|---------------------|-------------------|------|
| 'api' | 'api' | API权限 |
| 'event' | 'event' | 事件权限 |
| 其他 | 根据关联资源判断 | 根据业务逻辑确定 |

#### 3.5.5 need_approval 和 resource_nodes 转换逻辑

**need_approval 转换**：
```sql
-- 从旧表的 is_approval_required 字段直接映射
need_approval = is_approval_required
```

**resource_nodes 转换**：
```sql
-- 从属性表的 audit_user 转换为 resource_nodes JSON格式
-- 旧格式：property_name='audit_user', property_value='user_id'
-- 新格式：resource_nodes = '[{"type":"approver","userId":"xxx","userName":"xxx","order":1}]'

-- 需要关联用户服务获取用户姓名
```

#### 3.5.6 迁移SQL脚本（分步骤）

```sql
-- ============================================
-- 权限资源迁移脚本
-- 执行顺序：5
-- 核心难点：API与权限解耦，重建关联关系
-- ============================================

-- 步骤1：清空目标表
TRUNCATE TABLE `openplatform_v2_permission_t`;
TRUNCATE TABLE `openplatform_v2_permission_p_t`;

-- 步骤2：创建ID映射临时表
CREATE TEMPORARY TABLE IF NOT EXISTS `tmp_permission_id_mapping` (
    `old_permission_id` BIGINT(20),
    `new_permission_id` BIGINT(20),
    `resource_type` VARCHAR(20),
    `resource_id` BIGINT(20)
);

-- 步骤3：迁移API权限
-- 为每个API创建对应的权限记录
INSERT INTO `openplatform_v2_permission_t` (
    `id`,
    `name_cn`,
    `name_en`,
    `scope`,
    `resource_type`,
    `resource_id`,
    `category_id`,
    `need_approval`,
    `resource_nodes`,
    `status`,
    `create_by`,
    `create_time`,
    `last_update_by`,
    `last_update_time`
)
SELECT 
    -- 使用新的雪花ID（实际使用时需应用层生成）
    -- 这里简化为使用 原permission_id * 10 + 1 作为新ID
    p.`id` * 10 + 1 AS id,
    api.`api_name_cn` AS name_cn,
    api.`api_name_en` AS name_en,
    -- scope 生成规则：api:{path}:{method}
    CONCAT('api:', REPLACE(REPLACE(api.`api_path`, '/', ':'), ':', ''), ':', LOWER(api.`api_method`)) AS scope,
    'api' AS resource_type,
    api.`id` AS resource_id,
    p.`module_id` AS category_id,
    p.`is_approval_required` AS need_approval,
    -- resource_nodes 从属性表构建（简化示例）
    NULL AS resource_nodes,
    p.`status`,
    p.`create_by`,
    p.`create_time`,
    p.`last_update_by`,
    p.`last_update_time`
FROM `openplatform_permission_api_t` api
INNER JOIN `openplatform_permission_t` p ON api.`permission_id` = p.`id`;

-- 步骤4：迁移事件权限
-- 为每个事件创建对应的权限记录
INSERT INTO `openplatform_v2_permission_t` (
    `id`,
    `name_cn`,
    `name_en`,
    `scope`,
    `resource_type`,
    `resource_id`,
    `category_id`,
    `need_approval`,
    `resource_nodes`,
    `status`,
    `create_by`,
    `create_time`,
    `last_update_by`,
    `last_update_time`
)
SELECT 
    -- 使用新的雪花ID
    e.`id` * 10 + 2 AS id,
    e.`event_name_cn` AS name_cn,
    e.`event_name_en` AS name_en,
    -- scope 生成规则：event:{topic}
    CONCAT('event:', REPLACE(e.`topic`, '.', ':')) AS scope,
    'event' AS resource_type,
    e.`id` AS resource_id,
    e.`module_id` AS category_id,
    e.`is_approval_required` AS need_approval,
    NULL AS resource_nodes,
    e.`status`,
    e.`create_by`,
    e.`create_time`,
    e.`last_update_by`,
    e.`last_update_time`
FROM `openplatform_event_t` e;

-- 步骤5：更新 resource_nodes 字段
-- 从属性表的 audit_user 构建 JSON 格式的审批节点配置
UPDATE `openplatform_v2_permission_t` p
SET `resource_nodes` = (
    SELECT JSON_ARRAYAGG(
        JSON_OBJECT(
            'type', 'approver',
            'userId', pp.`property_value`,
            'userName', pp.`property_value`,  -- 简化，实际需关联用户服务
            'order', ROW_NUMBER() OVER (PARTITION BY pp.`parent_id` ORDER BY pp.`id`)
        )
    )
    FROM `openplatform_permission_p_t` pp
    WHERE pp.`parent_id` = FLOOR((p.`id` - 1) / 10)  -- 反推原 permission_id
    AND pp.`property_name` = 'audit_user'
    AND pp.`status` = 1
)
WHERE p.`need_approval` = 1;

-- 步骤6：迁移权限属性表（仅保留扩展属性）
INSERT INTO `openplatform_v2_permission_p_t` (
    `id`,
    `parent_id`,
    `property_name`,
    `property_value`,
    `status`,
    `create_by`,
    `create_time`,
    `last_update_by`,
    `last_update_time`
)
SELECT 
    pp.`id`,
    -- 映射到新的 permission_id
    (p.`id` * 10 + 1) AS parent_id,
    pp.`property_name`,
    pp.`property_value`,
    pp.`status`,
    pp.`create_by`,
    pp.`create_time`,
    pp.`last_update_by`,
    pp.`last_update_time`
FROM `openplatform_permission_p_t` pp
INNER JOIN `openplatform_permission_t` p ON pp.`parent_id` = p.`id`
WHERE pp.`property_name` NOT IN ('audit_user');  -- 排除已迁移到主表的属性

-- 步骤7：记录ID映射关系
INSERT INTO `tmp_permission_id_mapping` (`old_permission_id`, `new_permission_id`, `resource_type`, `resource_id`)
SELECT 
    p.`id` AS old_permission_id,
    p.`id` * 10 + 1 AS new_permission_id,
    'api' AS resource_type,
    api.`id` AS resource_id
FROM `openplatform_permission_t` p
INNER JOIN `openplatform_permission_api_t` api ON api.`permission_id` = p.`id`;

INSERT INTO `tmp_permission_id_mapping` (`old_permission_id`, `new_permission_id`, `resource_type`, `resource_id`)
SELECT 
    FLOOR((p.`id` - 2) / 10) AS old_permission_id,
    p.`id` AS new_permission_id,
    'event' AS resource_type,
    p.`resource_id`
FROM `openplatform_v2_permission_t` p
WHERE p.`resource_type` = 'event';
```

#### 3.5.7 数据验证SQL

```sql
-- ============================================
-- 权限资源验证脚本
-- ============================================

-- 验证1：数据量对比
SELECT 
    '源API权限数据量' AS type,
    COUNT(DISTINCT api.`permission_id`) AS count 
FROM `openplatform_permission_api_t` api
UNION ALL
SELECT 
    '目标API权限数据量',
    COUNT(*) 
FROM `openplatform_v2_permission_t`
WHERE `resource_type` = 'api'
UNION ALL
SELECT 
    '源事件数据量',
    COUNT(*) 
FROM `openplatform_event_t`
UNION ALL
SELECT 
    '目标事件权限数据量',
    COUNT(*) 
FROM `openplatform_v2_permission_t`
WHERE `resource_type` = 'event';

-- 验证2：scope 唯一性检查
SELECT 
    '重复的scope' AS type,
    `scope`,
    COUNT(*) AS count
FROM `openplatform_v2_permission_t`
GROUP BY `scope`
HAVING COUNT(*) > 1;

-- 验证3：资源关联完整性检查
SELECT 
    '缺少资源关联的权限' AS type,
    p.`id`,
    p.`name_cn`,
    p.`resource_type`,
    p.`resource_id`
FROM `openplatform_v2_permission_t` p
LEFT JOIN `openplatform_v2_api_t` api ON p.`resource_type` = 'api' AND p.`resource_id` = api.`id`
LEFT JOIN `openplatform_v2_event_t` evt ON p.`resource_type` = 'event' AND p.`resource_id` = evt.`id`
WHERE (p.`resource_type` = 'api' AND api.`id` IS NULL)
   OR (p.`resource_type` = 'event' AND evt.`id` IS NULL);

-- 验证4：分类关联完整性检查
SELECT 
    '缺少分类关联的权限' AS type,
    p.`id`,
    p.`name_cn`,
    p.`category_id`
FROM `openplatform_v2_permission_t` p
LEFT JOIN `openplatform_v2_category_t` c ON p.`category_id` = c.`id`
WHERE c.`id` IS NULL;

-- 验证5：审批配置检查
SELECT 
    '需要审批但无审批节点配置的权限' AS type,
    `id`,
    `name_cn`,
    `need_approval`,
    `resource_nodes`
FROM `openplatform_v2_permission_t`
WHERE `need_approval` = 1
AND (`resource_nodes` IS NULL OR `resource_nodes` = '');

-- 记录验证结果
INSERT INTO `openplatform_migration_record_t` (
    `module`, `source_table`, `target_table`, 
    `source_count`, `target_count`, `status`
)
VALUES (
    '权限资源模块',
    'openplatform_permission_t',
    'openplatform_v2_permission_t',
    (SELECT COUNT(*) FROM `openplatform_permission_t`),
    (SELECT COUNT(*) FROM `openplatform_v2_permission_t`),
    1
);
```

---

### 3.6 审批流程迁移（重点）

#### 3.6.1 迁移概述

| 项目 | 说明 |
|------|------|
| 源表 | `openplatform_eflow_t` + `openplatform_eflow_log_t` + `openplatform_eflow_log_doc_t` |
| 目标表 | `openplatform_v2_approval_flow_t` + `openplatform_v2_approval_record_t` + `openplatform_v2_approval_log_t` |
| 核心变化 | 模板与记录分离，支持多级审批 |
| 迁移难度 | ⭐⭐⭐⭐⭐（最高） |
| 数据量预估 | 根据实际业务确定 |
| 迁移时长预估 | 45分钟 |

#### 3.6.2 核心迁移逻辑

**旧设计问题**：
- 审批模板和审批记录混在同一张表
- 缺少节点索引，难以支持多级审批
- 字段命名不统一

**v2 设计改进**：
- 模板存储在 `approval_flow_t`
- 记录存储在 `approval_record_t`
- 支持组合节点配置（global + scene + resource）

**迁移策略**：
1. 从旧表数据中识别模板和记录
2. 模板数据迁移到 `approval_flow_t`
3. 记录数据迁移到 `approval_record_t`
4. 日志数据迁移到 `approval_log_t`

#### 3.6.3 如何区分模板数据和记录数据

**判断规则**：

```sql
-- 模板数据特征：
-- 1. eflow_type 表示流程类型（如 'api_register', 'permission_apply'）
-- 2. eflow_status 为初始状态
-- 3. 资源信息为空或为模板配置

-- 记录数据特征：
-- 1. eflow_status 为具体审批状态（待审、已通过、已拒绝等）
-- 2. resource_id 指向具体资源
-- 3. 有关联的日志记录

-- 实际区分需要根据业务规则判断
-- 建议：旧表中的数据大部分为记录，模板需要重新定义
```

#### 3.6.4 nodes 和 combined_nodes 转换规则

**nodes（模板节点配置）**：
```json
// 旧格式（如果存在）：可能存储在 resource_info 或其他字段
// 新格式：
[
    {"type": "approver", "userId": "user1", "userName": "审批人1", "order": 1},
    {"type": "approver", "userId": "user2", "userName": "审批人2", "order": 2}
]
```

**combined_nodes（记录组合节点）**：
```json
// 组合 global + scene + resource 三级审批节点
{
    "global": [...],
    "scene": [...],
    "resource": [...]
}
// 或扁平化数组：
[
    {"level": "global", "type": "approver", "userId": "xxx", "order": 1},
    {"level": "resource", "type": "approver", "userId": "yyy", "order": 2}
]
```

#### 3.6.5 current_node 计算逻辑

```sql
-- current_node 表示当前审批到达的节点索引
-- 计算规则：
-- 1. 新建记录：current_node = 0
-- 2. 根据审批日志中的 node_index 最大值 + 1
-- 3. 已完成记录：current_node = -1 或节点总数

-- 迁移时根据审批状态和日志反推
```

#### 3.6.6 迁移SQL脚本（分步骤）

```sql
-- ============================================
-- 审批流程迁移脚本
-- 执行顺序：6
-- 核心难点：模板与记录分离
-- ============================================

-- 步骤1：清空目标表
TRUNCATE TABLE `openplatform_v2_approval_flow_t`;
TRUNCATE TABLE `openplatform_v2_approval_record_t`;
TRUNCATE TABLE `openplatform_v2_approval_log_t`;

-- 步骤2：初始化审批流程模板
-- 注意：旧表中可能没有明确的模板数据，需要根据业务规则初始化
INSERT INTO `openplatform_v2_approval_flow_t` (
    `id`, `name_cn`, `name_en`, `code`, 
    `description_cn`, `description_en`, `nodes`, `status`,
    `create_time`, `last_update_time`, `create_by`, `last_update_by`
) VALUES 
(1, 'API注册审批流程', 'API Register Approval', 'api_register', 
 'API资源注册的审批流程', 'Approval flow for API registration', 
 '[{"type":"approver","userId":"api_admin","userName":"API管理员","order":1}]', 1,
 NOW(3), NOW(3), 'system', 'system'),
(2, '事件注册审批流程', 'Event Register Approval', 'event_register',
 '事件资源注册的审批流程', 'Approval flow for event registration',
 '[{"type":"approver","userId":"event_admin","userName":"事件管理员","order":1}]', 1,
 NOW(3), NOW(3), 'system', 'system'),
(3, 'API权限申请审批流程', 'API Permission Apply Approval', 'api_permission_apply',
 'API权限申请的审批流程', 'Approval flow for API permission application',
 '[{"type":"approver","userId":"permission_admin","userName":"权限管理员","order":1}]', 1,
 NOW(3), NOW(3), 'system', 'system'),
(4, '事件权限申请审批流程', 'Event Permission Apply Approval', 'event_permission_apply',
 '事件权限申请的审批流程', 'Approval flow for event permission application',
 '[{"type":"approver","userId":"permission_admin","userName":"权限管理员","order":1}]', 1,
 NOW(3), NOW(3), 'system', 'system');

-- 步骤3：迁移审批记录
-- 从旧表的审批记录数据迁移
INSERT INTO `openplatform_v2_approval_record_t` (
    `id`,
    `combined_nodes`,
    `business_type`,
    `business_id`,
    `applicant_id`,
    `applicant_name`,
    `status`,
    `current_node`,
    `create_time`,
    `last_update_time`,
    `create_by`,
    `last_update_by`,
    `completed_at`
)
SELECT 
    e.`eflow_id` AS id,
    -- combined_nodes 从 eflow_audit_user 构建（简化）
    CONCAT('[{"type":"approver","userId":"', e.`eflow_audit_user`, '","userName":"', e.`eflow_audit_user`, '","order":1}]') AS combined_nodes,
    -- business_type 从 eflow_type 映射
    CASE e.`eflow_type`
        WHEN 'api_register' THEN 'api_register'
        WHEN 'event_register' THEN 'event_register'
        WHEN 'api_permission' THEN 'api_permission_apply'
        WHEN 'event_permission' THEN 'event_permission_apply'
        ELSE 'api_permission_apply'
    END AS business_type,
    e.`resource_id` AS business_id,
    e.`eflow_submit_user` AS applicant_id,
    e.`eflow_submit_user` AS applicant_name,  -- 简化，实际需关联用户服务
    e.`eflow_status` AS status,
    -- current_node 根据状态计算
    CASE 
        WHEN e.`eflow_status` = 0 THEN 0  -- 待审
        WHEN e.`eflow_status` IN (1, 2, 3) THEN -1  -- 已完成
        ELSE 0
    END AS current_node,
    e.`create_time`,
    e.`last_update_time`,
    e.`create_by`,
    e.`last_update_by`,
    -- completed_at 根据状态设置
    CASE WHEN e.`eflow_status` IN (1, 2, 3) THEN e.`last_update_time` ELSE NULL END AS completed_at
FROM `openplatform_eflow_t` e
WHERE e.`eflow_status` IS NOT NULL;  -- 仅迁移记录数据

-- 步骤4：迁移审批操作日志
INSERT INTO `openplatform_v2_approval_log_t` (
    `id`,
    `record_id`,
    `node_index`,
    `level`,
    `operator_id`,
    `operator_name`,
    `action`,
    `comment`,
    `create_time`,
    `last_update_time`,
    `create_by`,
    `last_update_by`
)
SELECT 
    l.`eflow_log_id` AS id,
    l.`eflow_log_trace_id` AS record_id,
    0 AS node_index,  -- 旧表无节点索引，默认为0
    'global' AS level,  -- 默认为全局级别
    l.`eflow_log_user` AS operator_id,
    l.`eflow_log_user` AS operator_name,
    -- action 从 eflow_log_type 映射
    CASE 
        WHEN l.`eflow_log_type` LIKE '%同意%' OR l.`eflow_log_type` LIKE '%通过%' THEN 0
        WHEN l.`eflow_log_type` LIKE '%拒绝%' THEN 1
        WHEN l.`eflow_log_type` LIKE '%撤销%' THEN 2
        WHEN l.`eflow_log_type` LIKE '%转交%' THEN 3
        ELSE 0
    END AS action,
    l.`eflow_log_message` AS comment,
    l.`create_time`,
    l.`last_update_time`,
    l.`create_by`,
    l.`last_update_by`
FROM `openplatform_eflow_log_t` l;

-- 步骤5：更新审批记录的 current_node
-- 根据日志记录更新
UPDATE `openplatform_v2_approval_record_t` r
SET `current_node` = (
    SELECT COALESCE(MAX(l.`node_index`) + 1, 0)
    FROM `openplatform_v2_approval_log_t` l
    WHERE l.`record_id` = r.`id`
)
WHERE r.`status` = 0;  -- 仅更新待审记录
```

#### 3.6.7 数据验证SQL

```sql
-- ============================================
-- 审批流程验证脚本
-- ============================================

-- 验证1：数据量对比
SELECT 
    '源审批记录数据量' AS type,
    COUNT(*) AS count 
FROM `openplatform_eflow_t`
UNION ALL
SELECT 
    '目标审批记录数据量',
    COUNT(*) 
FROM `openplatform_v2_approval_record_t`
UNION ALL
SELECT 
    '源审批日志数据量',
    COUNT(*) 
FROM `openplatform_eflow_log_t`
UNION ALL
SELECT 
    '目标审批日志数据量',
    COUNT(*) 
FROM `openplatform_v2_approval_log_t`;

-- 验证2：模板数据检查
SELECT 
    '审批流程模板数量' AS type,
    COUNT(*) AS count 
FROM `openplatform_v2_approval_flow_t`
WHERE `status` = 1;

-- 验证3：记录与日志关联完整性
SELECT 
    '缺少日志的审批记录' AS type,
    r.`id`,
    r.`business_type`,
    r.`status`
FROM `openplatform_v2_approval_record_t` r
LEFT JOIN `openplatform_v2_approval_log_t` l ON r.`id` = l.`record_id`
WHERE r.`status` IN (1, 2)  -- 已通过或已拒绝
AND l.`id` IS NULL;

-- 验证4：状态映射验证
SELECT 
    '状态分布对比' AS type,
    old_status,
    new_status,
    COUNT(*) AS count
FROM (
    SELECT 
        e.`eflow_status` AS old_status,
        r.`status` AS new_status
    FROM `openplatform_eflow_t` e
    INNER JOIN `openplatform_v2_approval_record_t` r ON e.`eflow_id` = r.`id`
) t
GROUP BY old_status, new_status;

-- 记录验证结果
INSERT INTO `openplatform_migration_record_t` (
    `module`, `source_table`, `target_table`, 
    `source_count`, `target_count`, `status`
)
VALUES (
    '审批流程模块',
    'openplatform_eflow_t',
    'openplatform_v2_approval_record_t',
    (SELECT COUNT(*) FROM `openplatform_eflow_t`),
    (SELECT COUNT(*) FROM `openplatform_v2_approval_record_t`),
    1
);
```

---

### 3.7 订阅关系迁移

#### 3.7.1 迁移概述

| 项目 | 说明 |
|------|------|
| 源表 | `openplatform_app_permission_t` |
| 目标表 | `openplatform_v2_subscription_t` |
| 数据量预估 | 根据实际业务确定 |
| 迁移时长预估 | 15分钟 |

#### 3.7.2 旧表结构分析

根据 spec.md 中的信息，现有的 `openplatform_app_permission_t` 表存储应用对权限的订阅关系。

**推断的旧表结构**：
```sql
-- 应用权限关联表（推断结构，实际需要确认）
CREATE TABLE `openplatform_app_permission_t` (
    `id` BIGINT(20) PRIMARY KEY,
    `app_id` BIGINT(20) NOT NULL COMMENT '应用ID',
    `permission_id` BIGINT(20) COMMENT '权限ID（关联旧权限表）',
    `resource_type` VARCHAR(20) COMMENT '资源类型：api/event',
    `resource_id` BIGINT(20) COMMENT '资源ID（API ID 或 Event ID）',
    `status` TINYINT(10) COMMENT '状态',
    `channel_type` TINYINT(10) COMMENT '通道类型（事件订阅才有）',
    `channel_address` VARCHAR(500) COMMENT '通道地址',
    `auth_type` TINYINT(10) COMMENT '认证类型',
    `create_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    `last_update_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    `create_by` VARCHAR(100),
    `last_update_by` VARCHAR(100),
    KEY `idx_app_id` (`app_id`),
    KEY `idx_permission_id` (`permission_id`),
    KEY `idx_resource` (`resource_type`, `resource_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='应用权限关联表';
```

**现状说明**：
- 仅存储 API 权限与应用关系
- 事件与应用关系
- 回调暂时没有订阅数据

#### 3.7.3 字段映射关系

| 旧字段 | 新字段 | 映射规则 | 说明 |
|--------|--------|----------|------|
| `id` | `id` | 直接迁移 | 主键保持不变 |
| `app_id` | `app_id` | 直接迁移 | 应用ID不变 |
| `permission_id` | `permission_id` | **需要映射** | 通过临时映射表转换为新的权限ID |
| `status` | `status` | 状态映射 | 0=待审, 1=已授权, 2=已拒绝, 3=已取消 |
| `channel_type` | `channel_type` | 直接迁移 | 事件订阅的通道类型 |
| `channel_address` | `channel_address` | 直接迁移 | 事件订阅的通道地址 |
| `auth_type` | `auth_type` | 直接迁移 | 认证类型 |
| - | `approved_at` | 新增字段 | 审批通过时间，需要从审批日志推断 |
| - | `approved_by` | 新增字段 | 审批人，需要从审批日志推断 |
| `create_time` | `create_time` | 直接迁移 | 创建时间 |
| `last_update_time` | `last_update_time` | 直接迁移 | 更新时间 |
| `create_by` | `create_by` | 直接迁移 | 创建人 |
| `last_update_by` | `last_update_by` | 直接迁移 | 更新人 |

#### 3.7.4 迁移前置条件

1. **权限ID映射表已创建**：在第3阶段权限迁移时已创建 `tmp_permission_id_mapping`
2. **新权限表已填充数据**：`openplatform_v2_permission_t` 已包含所有权限记录
3. **应用表存在**：确保应用主表数据完整

#### 3.7.5 详细迁移步骤

**步骤1：创建权限ID映射临时表**

```sql
-- ============================================
-- 创建权限ID映射临时表
-- 用于将旧权限ID映射到新权限ID
-- ============================================

-- 如果在第3阶段未创建，需要创建映射表
CREATE TABLE IF NOT EXISTS `tmp_permission_id_mapping` (
    `old_permission_id` BIGINT(20) NOT NULL COMMENT '旧权限ID',
    `new_permission_id` BIGINT(20) NOT NULL COMMENT '新权限ID',
    `resource_type` VARCHAR(20) COMMENT '资源类型：api/event',
    `resource_id` BIGINT(20) COMMENT '资源ID',
    `old_scope_id` VARCHAR(200) COMMENT '旧权限标识',
    `new_scope` VARCHAR(200) COMMENT '新权限标识',
    PRIMARY KEY (`old_permission_id`),
    KEY `idx_new_permission_id` (`new_permission_id`),
    KEY `idx_resource` (`resource_type`, `resource_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限ID映射临时表';
```

**步骤2：构建权限ID映射关系**

```sql
-- ============================================
-- 构建权限ID映射关系
-- 核心：将旧的 permission_id 映射到新的 permission_id
-- ============================================

-- 2.1 API权限映射
INSERT INTO `tmp_permission_id_mapping` (
    `old_permission_id`, `new_permission_id`, `resource_type`, 
    `resource_id`, `old_scope_id`, `new_scope`
)
SELECT 
    old_perm.`id` AS `old_permission_id`,
    new_perm.`id` AS `new_permission_id`,
    'api' AS `resource_type`,
    new_perm.`resource_id`,
    old_perm.`scope_id` AS `old_scope_id`,
    new_perm.`scope` AS `new_scope`
FROM `openplatform_permission_t` old_perm
INNER JOIN `openplatform_v2_permission_t` new_perm 
    ON new_perm.`resource_type` = 'api'
    AND new_perm.`resource_id` IN (
        SELECT `id` FROM `openplatform_v2_api_t`
    )
WHERE old_perm.`permisssion_type` = 'api'
ON DUPLICATE KEY UPDATE `new_permission_id` = VALUES(`new_permission_id`);

-- 2.2 事件权限映射
INSERT INTO `tmp_permission_id_mapping` (
    `old_permission_id`, `new_permission_id`, `resource_type`, 
    `resource_id`, `old_scope_id`, `new_scope`
)
SELECT 
    old_perm.`id` AS `old_permission_id`,
    new_perm.`id` AS `new_permission_id`,
    'event' AS `resource_type`,
    new_perm.`resource_id`,
    old_perm.`scope_id` AS `old_scope_id`,
    new_perm.`scope` AS `new_scope`
FROM `openplatform_permission_t` old_perm
INNER JOIN `openplatform_v2_permission_t` new_perm 
    ON new_perm.`resource_type` = 'event'
    AND new_perm.`resource_id` IN (
        SELECT `id` FROM `openplatform_v2_event_t`
    )
WHERE old_perm.`permisssion_type` = 'event'
ON DUPLICATE KEY UPDATE `new_permission_id` = VALUES(`new_permission_id`);

-- 2.3 验证映射关系完整性
SELECT 
    'API权限映射' AS type,
    COUNT(*) AS mapped_count
FROM `tmp_permission_id_mapping`
WHERE `resource_type` = 'api'
UNION ALL
SELECT 
    '事件权限映射' AS type,
    COUNT(*) AS mapped_count
FROM `tmp_permission_id_mapping`
WHERE `resource_type` = 'event';
```

**步骤3：创建审批通过信息临时表**

```sql
-- ============================================
-- 创建审批通过信息临时表
-- 用于填充 approved_at 和 approved_by 字段
-- ============================================

CREATE TABLE IF NOT EXISTS `tmp_subscription_approval_info` (
    `subscription_id` BIGINT(20) PRIMARY KEY COMMENT '订阅ID',
    `approved_at` DATETIME(3) COMMENT '审批通过时间',
    `approved_by` VARCHAR(100) COMMENT '审批人',
    KEY `idx_subscription_id` (`subscription_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订阅审批通过信息临时表';

-- 从审批日志推断审批通过信息
INSERT INTO `tmp_subscription_approval_info` (
    `subscription_id`, `approved_at`, `approved_by`
)
SELECT 
    eflow.`resource_id` AS `subscription_id`,
    eflow.`last_update_time` AS `approved_at`,
    eflow.`eflow_audit_user` AS `approved_by`
FROM `openplatform_eflow_t` eflow
WHERE eflow.`resource_type` = 'subscription'  -- 假设审批记录中存储的是 subscription 类型
AND eflow.`eflow_status` = 1  -- 已通过
ON DUPLICATE KEY UPDATE 
    `approved_at` = VALUES(`approved_at`),
    `approved_by` = VALUES(`approved_by`);
```

**步骤4：迁移订阅数据**

```sql
-- ============================================
-- 迁移订阅数据
-- 执行顺序：7
-- 预估数据量：根据实际业务确定
-- ============================================

-- 4.1 清空目标表
TRUNCATE TABLE `openplatform_v2_subscription_t`;

-- 4.2 迁移订阅数据（通过权限ID映射）
INSERT INTO `openplatform_v2_subscription_t` (
    `id`,
    `app_id`,
    `permission_id`,
    `status`,
    `channel_type`,
    `channel_address`,
    `auth_type`,
    `approved_at`,
    `approved_by`,
    `create_time`,
    `last_update_time`,
    `create_by`,
    `last_update_by`
)
SELECT 
    old_sub.`id`,
    old_sub.`app_id`,
    mapping.`new_permission_id`,  -- 使用映射后的新权限ID
    -- 状态映射：确保状态值正确
    CASE old_sub.`status`
        WHEN 1 THEN 1  -- 已授权
        WHEN 0 THEN 0  -- 待审
        WHEN 2 THEN 2  -- 已拒绝
        WHEN 3 THEN 3  -- 已取消
        ELSE 0         -- 默认待审
    END AS `status`,
    old_sub.`channel_type`,
    old_sub.`channel_address`,
    old_sub.`auth_type`,
    approval.`approved_at`,
    approval.`approved_by`,
    old_sub.`create_time`,
    old_sub.`last_update_time`,
    old_sub.`create_by`,
    old_sub.`last_update_by`
FROM `openplatform_app_permission_t` old_sub
INNER JOIN `tmp_permission_id_mapping` mapping 
    ON old_sub.`permission_id` = mapping.`old_permission_id`
LEFT JOIN `tmp_subscription_approval_info` approval 
    ON old_sub.`id` = approval.`subscription_id`;

-- 4.3 记录迁移数量
SET @subscription_migrated = ROW_COUNT();

SELECT 
    '订阅数据迁移完成' AS status,
    @subscription_migrated AS migrated_count;
```

**步骤5：处理无法映射的订阅记录**

```sql
-- ============================================
-- 处理无法映射的订阅记录
-- 这些记录可能是数据不一致导致的
-- ============================================

-- 5.1 查找无法映射的订阅记录
SELECT 
    '无法映射的订阅记录' AS type,
    old_sub.`id`,
    old_sub.`app_id`,
    old_sub.`permission_id`,
    old_sub.`resource_type`,
    old_sub.`resource_id`,
    old_sub.`create_time`
FROM `openplatform_app_permission_t` old_sub
LEFT JOIN `tmp_permission_id_mapping` mapping 
    ON old_sub.`permission_id` = mapping.`old_permission_id`
WHERE mapping.`old_permission_id` IS NULL;

-- 5.2 将无法映射的记录存入异常表（供后续人工处理）
CREATE TABLE IF NOT EXISTS `openplatform_migration_unmapped_subscription_t` (
    `id` BIGINT(20) PRIMARY KEY,
    `app_id` BIGINT(20),
    `permission_id` BIGINT(20) COMMENT '旧权限ID',
    `resource_type` VARCHAR(20),
    `resource_id` BIGINT(20),
    `status` TINYINT(10),
    `create_time` DATETIME(3),
    `reason` VARCHAR(500) COMMENT '无法映射原因',
    `create_by` VARCHAR(100),
    KEY `idx_app_id` (`app_id`),
    KEY `idx_permission_id` (`permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='无法映射的订阅记录表';

INSERT INTO `openplatform_migration_unmapped_subscription_t`
SELECT 
    old_sub.`id`,
    old_sub.`app_id`,
    old_sub.`permission_id`,
    old_sub.`resource_type`,
    old_sub.`resource_id`,
    old_sub.`status`,
    old_sub.`create_time`,
    '权限ID无法映射到新权限表' AS `reason`,
    old_sub.`create_by`
FROM `openplatform_app_permission_t` old_sub
LEFT JOIN `tmp_permission_id_mapping` mapping 
    ON old_sub.`permission_id` = mapping.`old_permission_id`
WHERE mapping.`old_permission_id` IS NULL;

-- 5.3 统计无法映射的记录数
SELECT 
    '无法映射的订阅记录数' AS type,
    COUNT(*) AS count
FROM `openplatform_migration_unmapped_subscription_t`;
```

#### 3.7.6 数据验证SQL

```sql
-- ============================================
-- 订阅关系验证脚本
-- ============================================

-- 验证1：数据量对比
SELECT 
    '旧表订阅总数' AS type,
    COUNT(*) AS count
FROM `openplatform_app_permission_t`
UNION ALL
SELECT 
    '新表订阅总数',
    COUNT(*)
FROM `openplatform_v2_subscription_t`
UNION ALL
SELECT 
    '无法映射的订阅数',
    COUNT(*)
FROM `openplatform_migration_unmapped_subscription_t`;

-- 验证2：状态分布对比
SELECT 
    '旧表状态分布' AS type,
    CASE `status`
        WHEN 0 THEN '待审'
        WHEN 1 THEN '已授权'
        WHEN 2 THEN '已拒绝'
        WHEN 3 THEN '已取消'
        ELSE '未知'
    END AS status_name,
    COUNT(*) AS count
FROM `openplatform_app_permission_t`
GROUP BY `status`
UNION ALL
SELECT 
    '新表状态分布',
    CASE `status`
        WHEN 0 THEN '待审'
        WHEN 1 THEN '已授权'
        WHEN 2 THEN '已拒绝'
        WHEN 3 THEN '已取消'
        ELSE '未知'
    END,
    COUNT(*)
FROM `openplatform_v2_subscription_t`
GROUP BY `status`;

-- 验证3：权限关联完整性
SELECT 
    '缺少权限关联的订阅' AS type,
    s.`id`,
    s.`app_id`,
    s.`permission_id`
FROM `openplatform_v2_subscription_t` s
LEFT JOIN `openplatform_v2_permission_t` p ON s.`permission_id` = p.`id`
WHERE p.`id` IS NULL;

-- 验证4：应用关联完整性
SELECT 
    '缺少应用关联的订阅' AS type,
    s.`id`,
    s.`app_id`,
    s.`permission_id`
FROM `openplatform_v2_subscription_t` s
LEFT JOIN `openplatform_app_t` a ON s.`app_id` = a.`id`  -- 假设应用表名为 openplatform_app_t
WHERE a.`id` IS NULL;

-- 验证5：唯一性约束检查
SELECT 
    '重复的订阅关系（同一应用订阅同一权限）' AS type,
    `app_id`,
    `permission_id`,
    COUNT(*) AS duplicate_count
FROM `openplatform_v2_subscription_t`
GROUP BY `app_id`, `permission_id`
HAVING COUNT(*) > 1;

-- 验证6：按资源类型统计订阅数量
SELECT 
    p.`resource_type` AS resource_type,
    COUNT(s.`id`) AS subscription_count
FROM `openplatform_v2_subscription_t` s
INNER JOIN `openplatform_v2_permission_t` p ON s.`permission_id` = p.`id`
GROUP BY p.`resource_type`;

-- 验证7：审批通过时间填充率
SELECT 
    '有审批通过时间的订阅数' AS type,
    COUNT(*) AS count
FROM `openplatform_v2_subscription_t`
WHERE `approved_at` IS NOT NULL
UNION ALL
SELECT 
    '无审批通过时间的订阅数',
    COUNT(*)
FROM `openplatform_v2_subscription_t`
WHERE `approved_at` IS NULL;

-- 验证8：记录验证结果
INSERT INTO `openplatform_migration_record_t` (
    `module`, `source_table`, `target_table`, 
    `source_count`, `target_count`, `unmapped_count`, `status`
)
VALUES (
    '订阅关系模块',
    'openplatform_app_permission_t',
    'openplatform_v2_subscription_t',
    (SELECT COUNT(*) FROM `openplatform_app_permission_t`),
    (SELECT COUNT(*) FROM `openplatform_v2_subscription_t`),
    (SELECT COUNT(*) FROM `openplatform_migration_unmapped_subscription_t`),
    1
);
```

#### 3.7.7 迁移后数据清理

```sql
-- ============================================
-- 迁移后清理临时表
-- 注意：保留临时表用于回滚和数据核对
-- ============================================

-- 建议在迁移验证通过后，延迟1周再清理临时表
-- DROP TABLE IF EXISTS `tmp_permission_id_mapping`;
-- DROP TABLE IF EXISTS `tmp_subscription_approval_info`;
-- DROP TABLE IF EXISTS `openplatform_migration_unmapped_subscription_t`;
```

#### 3.7.8 回滚脚本

```sql
-- ============================================
-- 订阅关系迁移回滚脚本
-- 执行时机：如果订阅迁移失败
-- ============================================

-- 回滚操作：清空新表数据
TRUNCATE TABLE `openplatform_v2_subscription_t`;

-- 验证回滚结果
SELECT 
    '回滚后新表数据量' AS type,
    COUNT(*) AS count
FROM `openplatform_v2_subscription_t`;

-- 记录回滚操作
INSERT INTO `openplatform_migration_record_t` (
    `module`, `source_table`, `target_table`, 
    `source_count`, `target_count`, `status`, `remark`
)
VALUES (
    '订阅关系模块',
    'openplatform_app_permission_t',
    'openplatform_v2_subscription_t',
    0,
    0,
    0,
    '迁移失败，已回滚'
);
```

---

### 3.8 用户授权迁移

#### 3.8.1 迁移概述

| 项目 | 说明 |
|------|------|
| 源表 | 查看是否有现有的用户授权表 |
| 目标表 | `openplatform_v2_user_authorization_t` |
| 数据量预估 | 根据实际业务确定 |
| 迁移时长预估 | 15分钟 |

#### 3.8.2 迁移SQL脚本

```sql
-- ============================================
-- 用户授权迁移脚本
-- 执行顺序：8
-- 说明：如果存在旧的用户授权表，需要根据实际表结构调整迁移脚本
-- ============================================

-- 步骤1：清空目标表
TRUNCATE TABLE `openplatform_v2_user_authorization_t`;

-- 步骤2：检查是否存在旧的用户授权表
SELECT TABLE_NAME 
FROM information_schema.TABLES 
WHERE TABLE_SCHEMA = 'openplatform'
AND TABLE_NAME LIKE '%user%author%';

-- 如果存在旧表，根据实际结构迁移
-- 以下为示例迁移脚本（需根据实际表结构调整）：

-- 假设存在 openplatform_user_scope_t 表
-- INSERT INTO `openplatform_v2_user_authorization_t` (
--     `id`,
--     `user_id`,
--     `app_id`,
--     `scopes`,
--     `expires_at`,
--     `create_time`,
--     `last_update_time`,
--     `create_by`,
--     `last_update_by`
-- )
-- SELECT 
--     `id`,
--     `user_id`,
--     `app_id`,
--     -- scopes 转换为 JSON 格式
--     JSON_ARRAY(`scope`) AS scopes,
--     `expires_at`,
--     `create_time`,
--     `last_update_time`,
--     `create_by`,
--     `last_update_by`
-- FROM `openplatform_user_scope_t`;

-- 如果不存在旧表，用户授权需要在业务上线后通过OAuth流程建立
```

#### 3.8.3 数据验证SQL

```sql
-- ============================================
-- 用户授权验证脚本
-- ============================================

-- 验证1：数据量统计
SELECT 
    '用户授权总数' AS type,
    COUNT(*) AS count 
FROM `openplatform_v2_user_authorization_t`
UNION ALL
SELECT 
    '有效授权数',
    COUNT(*) 
FROM `openplatform_v2_user_authorization_t`
WHERE `revoked_at` IS NULL
AND (`expires_at` IS NULL OR `expires_at` > NOW());

-- 验证2：scopes 格式验证
SELECT 
    'scopes格式异常的授权' AS type,
    `id`,
    `user_id`,
    `scopes`
FROM `openplatform_v2_user_authorization_t`
WHERE JSON_VALID(`scopes`) = 0;

-- 记录验证结果
INSERT INTO `openplatform_migration_record_t` (
    `module`, `source_table`, `target_table`, 
    `source_count`, `target_count`, `status`
)
VALUES (
    '用户授权模块',
    '无（新增）',
    'openplatform_v2_user_authorization_t',
    0,
    (SELECT COUNT(*) FROM `openplatform_v2_user_authorization_t`),
    1
);
```

---

## 4. 迁移执行流程

### 4.1 分阶段执行步骤

```
┌─────────────────────────────────────────────────────────────────────┐
│ 准备阶段（-30分钟）                                                   │
├─────────────────────────────────────────────────────────────────────┤
│ □ 停止应用服务                                                       │
│ □ 执行数据备份                                                       │
│ □ 验证备份完整性                                                     │
│ □ 检查迁移工具                                                       │
│ 验证点：备份库数据量与源库一致                                         │
└─────────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────────┐
│ 第1阶段：分类模块迁移（预计15分钟）                                     │
├─────────────────────────────────────────────────────────────────────┤
│ □ 执行分类迁移脚本                                                   │
│ □ 填充 category_alias 字段                                          │
│ □ 计算并填充 path 字段                                               │
│ □ 执行验证脚本                                                       │
│ 验证点：数据量一致，path格式正确，无孤儿数据                            │
└─────────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────────┐
│ 第2阶段：资源模块迁移（预计30分钟）                                     │
├─────────────────────────────────────────────────────────────────────┤
│ □ 执行API迁移脚本                                                    │
│ □ 执行事件迁移脚本                                                   │
│ □ 初始化回调资源                                                     │
│ □ 执行验证脚本                                                       │
│ 验证点：数据量一致，状态映射正确，属性完整                              │
└─────────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────────┐
│ 第3阶段：权限模块迁移（预计45分钟）                                     │
├─────────────────────────────────────────────────────────────────────┤
│ □ 执行API权限迁移                                                    │
│ □ 执行事件权限迁移                                                   │
│ □ 更新 resource_nodes 字段                                          │
│ □ 迁移权限属性                                                       │
│ □ 执行验证脚本                                                       │
│ 验证点：scope唯一，资源关联完整，审批配置正确                           │
└─────────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────────┐
│ 第4阶段：审批模块迁移（预计45分钟）                                     │
├─────────────────────────────────────────────────────────────────────┤
│ □ 初始化审批流程模板                                                 │
│ □ 迁移审批记录                                                       │
│ □ 迁移审批日志                                                       │
│ □ 更新 current_node 字段                                            │
│ □ 执行验证脚本                                                       │
│ 验证点：模板数据完整，记录与日志关联正确                                │
└─────────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────────┐
│ 第5阶段：业务关联迁移（预计30分钟）                                     │
├─────────────────────────────────────────────────────────────────────┤
│ □ 迁移订阅关系（如有）                                               │
│ □ 迁移用户授权（如有）                                               │
│ □ 执行验证脚本                                                       │
│ 验证点：关联数据完整，无孤儿数据                                       │
└─────────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────────┐
│ 验证阶段（预计30分钟）                                                 │
├─────────────────────────────────────────────────────────────────────┤
│ □ 执行全局数据验证                                                   │
│ □ 执行业务功能验证                                                   │
│ □ 执行性能验证                                                       │
│ □ 检查应用启动状态                                                   │
│ 验证点：所有验证通过                                                  │
└─────────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────────┐
│ 切换阶段（预计15分钟）                                                 │
├─────────────────────────────────────────────────────────────────────┤
│ □ 修改应用配置指向新表                                                │
│ □ 启动应用服务                                                       │
│ □ 冒烟测试                                                           │
│ □ 发布恢复公告                                                       │
│ 验证点：应用正常运行，核心功能可用                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 4.2 每个阶段的验证点

| 阶段 | 验证项 | 验证标准 | 验证方法 |
|------|--------|----------|----------|
| 准备阶段 | 备份完整性 | 备份库数据量 = 源库数据量 | 执行备份验证SQL |
| 第1阶段 | 分类迁移 | 数据量一致，path正确 | 执行分类验证SQL |
| 第2阶段 | 资源迁移 | 数据量一致，状态正确 | 执行资源验证SQL |
| 第3阶段 | 权限迁移 | scope唯一，关联完整 | 执行权限验证SQL |
| 第4阶段 | 审批迁移 | 模板完整，记录关联正确 | 执行审批验证SQL |
| 第5阶段 | 业务迁移 | 关联数据完整 | 执行业务验证SQL |
| 验证阶段 | 全局验证 | 所有数据一致 | 执行全局验证SQL |

### 4.3 时间预估

| 阶段 | 开始时间 | 结束时间 | 时长 |
|------|----------|----------|------|
| 准备阶段 | 02:00 | 02:30 | 30分钟 |
| 第1阶段 | 02:30 | 02:45 | 15分钟 |
| 第2阶段 | 02:45 | 03:15 | 30分钟 |
| 第3阶段 | 03:15 | 04:00 | 45分钟 |
| 第4阶段 | 04:00 | 04:45 | 45分钟 |
| 第5阶段 | 04:45 | 05:15 | 30分钟 |
| 验证阶段 | 05:15 | 05:45 | 30分钟 |
| 切换阶段 | 05:45 | 06:00 | 15分钟 |

---

## 5. 数据验证方案

### 5.1 数据量对比SQL

```sql
-- ============================================
-- 全局数据量对比
-- ============================================

SELECT 
    '分类' AS module,
    (SELECT COUNT(*) FROM `openplatform_module_node_t` WHERE `status` = 1) AS source_count,
    (SELECT COUNT(*) FROM `openplatform_v2_category_t`) AS target_count,
    CASE 
        WHEN (SELECT COUNT(*) FROM `openplatform_module_node_t` WHERE `status` = 1) = 
             (SELECT COUNT(*) FROM `openplatform_v2_category_t`) 
        THEN '通过' 
        ELSE '失败' 
    END AS result
UNION ALL
SELECT 
    'API资源',
    (SELECT COUNT(*) FROM `openplatform_permission_api_t`),
    (SELECT COUNT(*) FROM `openplatform_v2_api_t`),
    CASE 
        WHEN (SELECT COUNT(*) FROM `openplatform_permission_api_t`) = 
             (SELECT COUNT(*) FROM `openplatform_v2_api_t`) 
        THEN '通过' 
        ELSE '失败' 
    END
UNION ALL
SELECT 
    '事件资源',
    (SELECT COUNT(*) FROM `openplatform_event_t`),
    (SELECT COUNT(*) FROM `openplatform_v2_event_t`),
    CASE 
        WHEN (SELECT COUNT(*) FROM `openplatform_event_t`) = 
             (SELECT COUNT(*) FROM `openplatform_v2_event_t`) 
        THEN '通过' 
        ELSE '失败' 
    END
UNION ALL
SELECT 
    '权限资源',
    (SELECT COUNT(*) FROM `openplatform_permission_t`),
    (SELECT COUNT(*) FROM `openplatform_v2_permission_t`),
    '人工验证'  -- 权限数量可能因解耦而变化
UNION ALL
SELECT 
    '审批记录',
    (SELECT COUNT(*) FROM `openplatform_eflow_t`),
    (SELECT COUNT(*) FROM `openplatform_v2_approval_record_t`),
    CASE 
        WHEN (SELECT COUNT(*) FROM `openplatform_eflow_t`) = 
             (SELECT COUNT(*) FROM `openplatform_v2_approval_record_t`) 
        THEN '通过' 
        ELSE '失败' 
    END;
```

### 5.2 数据一致性校验SQL

```sql
-- ============================================
-- 数据一致性详细校验
-- ============================================

-- 校验1：分类数据一致性
SELECT 
    '分类数据一致性' AS check_item,
    COUNT(*) AS diff_count
FROM (
    SELECT o.`id`, o.`node_name_cn`, n.`name_cn`
    FROM `openplatform_module_node_t` o
    LEFT JOIN `openplatform_v2_category_t` n ON o.`id` = n.`id`
    WHERE o.`status` = 1
    AND (o.`node_name_cn` != n.`name_cn` OR n.`id` IS NULL)
) t;

-- 校验2：API数据一致性
SELECT 
    'API数据一致性' AS check_item,
    COUNT(*) AS diff_count
FROM (
    SELECT o.`id`, o.`api_name_cn`, n.`name_cn`
    FROM `openplatform_permission_api_t` o
    LEFT JOIN `openplatform_v2_api_t` n ON o.`id` = n.`id`
    WHERE o.`api_name_cn` != n.`name_cn` OR n.`id` IS NULL
) t;

-- 校验3：事件数据一致性
SELECT 
    '事件数据一致性' AS check_item,
    COUNT(*) AS diff_count
FROM (
    SELECT o.`id`, o.`event_name_cn`, n.`name_cn`
    FROM `openplatform_event_t` o
    LEFT JOIN `openplatform_v2_event_t` n ON o.`id` = n.`id`
    WHERE o.`event_name_cn` != n.`name_cn` OR n.`id` IS NULL
) t;

-- 校验4：权限scope唯一性
SELECT 
    '权限scope唯一性' AS check_item,
    COUNT(*) AS duplicate_count
FROM (
    SELECT `scope`, COUNT(*) AS cnt
    FROM `openplatform_v2_permission_t`
    GROUP BY `scope`
    HAVING COUNT(*) > 1
) t;

-- 校验5：资源关联完整性
SELECT 
    'API权限关联完整性' AS check_item,
    COUNT(*) AS missing_count
FROM `openplatform_v2_permission_t` p
LEFT JOIN `openplatform_v2_api_t` api ON p.`resource_type` = 'api' AND p.`resource_id` = api.`id`
WHERE p.`resource_type` = 'api' AND api.`id` IS NULL;
```

### 5.3 业务功能验证清单

| 验证项 | 验证方法 | 预期结果 |
|--------|----------|----------|
| 分类树查询 | 查询分类树接口 | 返回完整的分类树结构 |
| API列表查询 | 查询API列表接口 | 返回所有已发布的API |
| 事件列表查询 | 查询事件列表接口 | 返回所有已发布的事件 |
| 权限查询 | 按scope查询权限 | 返回正确的权限信息和关联资源 |
| 审批流程查询 | 查询审批模板列表 | 返回所有审批流程模板 |
| 审批记录查询 | 按业务ID查询审批记录 | 返回正确的审批历史 |
| 订阅关系查询 | 按应用ID查询订阅 | 返回应用的权限订阅列表 |

### 5.4 性能验证方案

```sql
-- ============================================
-- 性能验证SQL
-- ============================================

-- 验证1：分类树查询性能（使用path索引）
EXPLAIN SELECT * FROM `openplatform_v2_category_t` 
WHERE `path` LIKE '/1/%';

-- 执行时间应 < 100ms
SELECT NOW(6) AS start_time;
SELECT * FROM `openplatform_v2_category_t` WHERE `path` LIKE '/1/%';
SELECT NOW(6) AS end_time;

-- 验证2：权限查询性能
EXPLAIN SELECT * FROM `openplatform_v2_permission_t` 
WHERE `resource_type` = 'api' AND `resource_id` = 1;

-- 验证3：审批记录查询性能
EXPLAIN SELECT * FROM `openplatform_v2_approval_record_t` 
WHERE `business_type` = 'api_permission_apply' AND `business_id` = 1;
```

---

## 6. 回滚方案

### 6.1 各阶段回滚脚本

```sql
-- ============================================
-- 回滚脚本
-- 执行条件：迁移验证失败，需要恢复数据
-- ============================================

-- 阶段1：分类模块回滚
TRUNCATE TABLE `openplatform_v2_category_t`;
TRUNCATE TABLE `openplatform_v2_category_owner_t`;

-- 阶段2：资源模块回滚
TRUNCATE TABLE `openplatform_v2_api_t`;
TRUNCATE TABLE `openplatform_v2_api_p_t`;
TRUNCATE TABLE `openplatform_v2_event_t`;
TRUNCATE TABLE `openplatform_v2_event_p_t`;
TRUNCATE TABLE `openplatform_v2_callback_t`;
TRUNCATE TABLE `openplatform_v2_callback_p_t`;

-- 阶段3：权限模块回滚
TRUNCATE TABLE `openplatform_v2_permission_t`;
TRUNCATE TABLE `openplatform_v2_permission_p_t`;

-- 阶段4：审批模块回滚
TRUNCATE TABLE `openplatform_v2_approval_flow_t`;
TRUNCATE TABLE `openplatform_v2_approval_record_t`;
TRUNCATE TABLE `openplatform_v2_approval_log_t`;

-- 阶段5：业务关联回滚
TRUNCATE TABLE `openplatform_v2_subscription_t`;
TRUNCATE TABLE `openplatform_v2_user_authorization_t`;

-- 全量回滚（从备份库恢复）
-- 注意：以下脚本仅作参考，实际恢复需要根据业务情况调整

-- 从备份库恢复分类表
INSERT INTO `openplatform`.`openplatform_module_node_t`
SELECT * FROM `openplatform_backup_v1`.`openplatform_module_node_t`;

-- 从备份库恢复权限表
INSERT INTO `openplatform`.`openplatform_permission_t`
SELECT * FROM `openplatform_backup_v1`.`openplatform_permission_t`;

-- ... 其他表类似
```

### 6.2 回滚触发条件

| 条件 | 说明 | 处理方式 |
|------|------|----------|
| 数据量不一致 | 源表和目标表数据量差异超过阈值 | 阶段回滚，重新执行该阶段 |
| 数据内容错误 | 关键字段映射错误 | 阶段回滚，修正脚本后重新执行 |
| 外键关联缺失 | 关联数据缺失导致孤儿数据 | 阶段回滚，检查依赖关系 |
| 验证脚本失败 | 任何验证脚本返回失败结果 | 阶段回滚，排查问题 |
| 应用启动失败 | 应用无法正常启动 | 全量回滚，恢复备份 |

### 6.3 回滚验证方法

```sql
-- ============================================
-- 回滚验证脚本
-- ============================================

-- 验证1：检查v2表是否已清空
SELECT 
    'v2_category_t' AS table_name,
    COUNT(*) AS count
FROM `openplatform_v2_category_t`
UNION ALL
SELECT 'v2_api_t', COUNT(*) FROM `openplatform_v2_api_t`
UNION ALL
SELECT 'v2_event_t', COUNT(*) FROM `openplatform_v2_event_t`
UNION ALL
SELECT 'v2_permission_t', COUNT(*) FROM `openplatform_v2_permission_t`
UNION ALL
SELECT 'v2_approval_record_t', COUNT(*) FROM `openplatform_v2_approval_record_t`;

-- 验证2：检查备份库数据完整性
SELECT 
    TABLE_NAME,
    TABLE_ROWS
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'openplatform_backup_v1';

-- 验证3：源表数据是否恢复
SELECT COUNT(*) FROM `openplatform_module_node_t`;
```

### 6.4 数据恢复步骤

1. **停止所有应用服务**
2. **执行回滚脚本**，清空v2版本表
3. **从备份库恢复数据**（如需要）
4. **验证数据完整性**
5. **修改应用配置**，指向旧版本表
6. **启动应用服务**
7. **执行冒烟测试**

---

## 7. 风险评估与应对

### 7.1 潜在风险点

| 风险 | 等级 | 影响 | 概率 |
|------|------|------|------|
| 数据量超预期 | 高 | 迁移时间延长 | 中 |
| 字段映射错误 | 高 | 数据不一致 | 中 |
| API-权限关联丢失 | 高 | 权限功能异常 | 高 |
| 审批流程配置错误 | 中 | 审批功能异常 | 中 |
| 性能下降 | 中 | 系统响应慢 | 低 |
| 应用兼容性问题 | 中 | 功能异常 | 中 |
| 迁移脚本错误 | 高 | 迁移失败 | 低 |

### 7.2 风险应对措施

| 风险 | 应对措施 |
|------|----------|
| 数据量超预期 | 提前在预发布环境进行迁移演练，预估时间 |
| 字段映射错误 | 编写详细的验证脚本，逐字段对比 |
| API-权限关联丢失 | 迁移前备份关联关系，迁移后逐一验证 |
| 审批流程配置错误 | 制定审批配置检查清单，人工复核 |
| 性能下降 | 提前创建必要的索引，进行性能测试 |
| 应用兼容性问题 | 在预发布环境进行全量回归测试 |
| 迁移脚本错误 | 多人审核脚本，在预发布环境验证 |

### 7.3 应急预案

```
┌─────────────────────────────────────────────────────────────────┐
│ 应急响应流程                                                     │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  发现问题 ─→ 评估影响 ─→ 决策处理方式 ─→ 执行处理 ─→ 验证结果    │
│                                                                  │
│  处理方式：                                                      │
│  1. 小问题：修正后继续迁移                                        │
│  2. 中等问题：阶段回滚，修正后重新执行                             │
│  3. 大问题：全量回滚，恢复备份                                    │
│                                                                  │
│  紧急联系人：                                                    │
│  - 迁移负责人：[姓名] [电话]                                      │
│  - DBA：[姓名] [电话]                                            │
│  - 应用负责人：[姓名] [电话]                                      │
│                                                                  │
│  决策时间窗口：发现问题后30分钟内决策                              │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

## 8. 附录

### 8.1 完整的迁移SQL脚本

```bash
#!/bin/bash
# ============================================
# 一键执行迁移脚本
# 使用方式：mysql -u root -p openplatform < migration_all.sql
# ============================================

# 执行顺序：
# 1. 备份数据（已在准备阶段完成）
# 2. 创建新表（已在准备阶段完成）
# 3. 分类模块迁移
# 4. API资源迁移
# 5. 事件资源迁移
# 6. 回调资源初始化
# 7. 权限资源迁移
# 8. 审批流程迁移
# 9. 订阅关系迁移
# 10. 用户授权迁移
# 11. 执行验证

# 注意：实际执行时建议分步骤执行，每个步骤验证后再执行下一步
```

### 8.2 验证SQL脚本汇总

```sql
-- ============================================
-- 全量验证脚本
-- 执行时间：所有迁移完成后
-- ============================================

-- 创建验证结果汇总表
CREATE TABLE IF NOT EXISTS `openplatform_migration_summary_t` (
    `id` BIGINT(20) PRIMARY KEY AUTO_INCREMENT,
    `check_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    `check_item` VARCHAR(200),
    `result` VARCHAR(20),
    `detail` TEXT
);

-- 执行所有验证
INSERT INTO `openplatform_migration_summary_t` (`check_item`, `result`, `detail`)
SELECT 
    '分类数据量',
    CASE WHEN source_count = target_count THEN '通过' ELSE '失败' END,
    CONCAT('源：', source_count, '，目标：', target_count)
FROM (
    SELECT 
        (SELECT COUNT(*) FROM `openplatform_module_node_t` WHERE `status` = 1) AS source_count,
        (SELECT COUNT(*) FROM `openplatform_v2_category_t`) AS target_count
) t;

INSERT INTO `openplatform_migration_summary_t` (`check_item`, `result`, `detail`)
SELECT 
    'API数据量',
    CASE WHEN source_count = target_count THEN '通过' ELSE '失败' END,
    CONCAT('源：', source_count, '，目标：', target_count)
FROM (
    SELECT 
        (SELECT COUNT(*) FROM `openplatform_permission_api_t`) AS source_count,
        (SELECT COUNT(*) FROM `openplatform_v2_api_t`) AS target_count
) t;

INSERT INTO `openplatform_migration_summary_t` (`check_item`, `result`, `detail`)
SELECT 
    '事件数据量',
    CASE WHEN source_count = target_count THEN '通过' ELSE '失败' END,
    CONCAT('源：', source_count, '，目标：', target_count)
FROM (
    SELECT 
        (SELECT COUNT(*) FROM `openplatform_event_t`) AS source_count,
        (SELECT COUNT(*) FROM `openplatform_v2_event_t`) AS target_count
) t;

INSERT INTO `openplatform_migration_summary_t` (`check_item`, `result`, `detail`)
SELECT 
    '权限scope唯一性',
    CASE WHEN duplicate_count = 0 THEN '通过' ELSE '失败' END,
    CONCAT('重复数量：', duplicate_count)
FROM (
    SELECT COUNT(*) AS duplicate_count
    FROM (
        SELECT `scope`, COUNT(*) AS cnt
        FROM `openplatform_v2_permission_t`
        GROUP BY `scope`
        HAVING COUNT(*) > 1
    ) t
) t;

INSERT INTO `openplatform_migration_summary_t` (`check_item`, `result`, `detail`)
SELECT 
    '审批记录数据量',
    CASE WHEN source_count = target_count THEN '通过' ELSE '失败' END,
    CONCAT('源：', source_count, '，目标：', target_count)
FROM (
    SELECT 
        (SELECT COUNT(*) FROM `openplatform_eflow_t`) AS source_count,
        (SELECT COUNT(*) FROM `openplatform_v2_approval_record_t`) AS target_count
) t;

-- 查看验证结果
SELECT 
    `check_time`,
    `check_item`,
    `result`,
    `detail`
FROM `openplatform_migration_summary_t`
ORDER BY `id`;

-- 检查是否有失败的验证项
SELECT 
    CASE 
        WHEN COUNT(*) = 0 THEN '所有验证通过'
        ELSE CONCAT('存在 ', COUNT(*), ' 项验证失败')
    END AS summary
FROM `openplatform_migration_summary_t`
WHERE `result` = '失败';
```

### 8.3 回滚SQL脚本汇总

```sql
-- ============================================
-- 全量回滚脚本
-- 使用场景：迁移失败，需要完全恢复
-- 执行前提：备份库 openplatform_backup_v1 存在且完整
-- ============================================

-- 步骤1：清空所有v2版本表
SET FOREIGN_KEY_CHECKS = 0;

TRUNCATE TABLE `openplatform_v2_category_t`;
TRUNCATE TABLE `openplatform_v2_category_owner_t`;
TRUNCATE TABLE `openplatform_v2_api_t`;
TRUNCATE TABLE `openplatform_v2_api_p_t`;
TRUNCATE TABLE `openplatform_v2_event_t`;
TRUNCATE TABLE `openplatform_v2_event_p_t`;
TRUNCATE TABLE `openplatform_v2_callback_t`;
TRUNCATE TABLE `openplatform_v2_callback_p_t`;
TRUNCATE TABLE `openplatform_v2_permission_t`;
TRUNCATE TABLE `openplatform_v2_permission_p_t`;
TRUNCATE TABLE `openplatform_v2_subscription_t`;
TRUNCATE TABLE `openplatform_v2_approval_flow_t`;
TRUNCATE TABLE `openplatform_v2_approval_record_t`;
TRUNCATE TABLE `openplatform_v2_approval_log_t`;
TRUNCATE TABLE `openplatform_v2_user_authorization_t`;

SET FOREIGN_KEY_CHECKS = 1;

-- 步骤2：验证备份库数据
SELECT 
    '备份库数据验证' AS check_item,
    COUNT(*) AS table_count
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'openplatform_backup_v1';

-- 步骤3：从备份恢复（如源表数据被修改）
-- 注意：仅在源表数据被意外修改时执行
-- TRUNCATE TABLE `openplatform`.`openplatform_module_node_t`;
-- INSERT INTO `openplatform`.`openplatform_module_node_t`
-- SELECT * FROM `openplatform_backup_v1`.`openplatform_module_node_t`;

-- ... 其他表类似

-- 步骤4：验证回滚结果
SELECT 
    '回滚验证' AS check_item,
    COUNT(*) AS v2_table_count
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'openplatform'
AND TABLE_NAME LIKE 'openplatform_v2_%'
AND TABLE_ROWS > 0;

-- 结果应为0，表示所有v2表已清空
```

---

## 迁移完成检查清单

```
□ 备份数据已验证完整
□ 分类模块迁移完成并验证通过
□ API资源迁移完成并验证通过
□ 事件资源迁移完成并验证通过
□ 回调资源初始化完成
□ 权限资源迁移完成并验证通过
□ 审批流程迁移完成并验证通过
□ 订阅关系迁移完成（如有）
□ 用户授权迁移完成（如有）
□ 全局数据验证通过
□ 业务功能验证通过
□ 性能验证通过
□ 应用配置已更新
□ 应用启动成功
□ 冒烟测试通过
□ 发布恢复公告
```

---

**文档版本**：v1.0  
**创建时间**：2024年  
**最后更新**：2024年  
**负责人**：待填写  
**审核人**：待填写
