# 已有数据库表结构（历史）

> 本文档为 `plan.md` 的子文档，记录能力开放平台重构前的已有表结构，供迁移对照参考。本文件中的表为旧版设计，新表设计见 `plan-db.md`。

## 表清单

| 表名 | 模块 | 说明 | 迁移目标(v2) |
|------|------|------|--------------|
| `openplatform_module_node_t` | 分类 | 分组节点表 | `openplatform_v2_category_t` |
| `openplatform_permission_t` | 权限 | 权限主表 | `openplatform_v2_permission_t` |
| `openplatform_permission_p_t` | 权限 | 权限属性表 | `openplatform_v2_permission_p_t` |
| `openplatform_permission_api_t` | API | API权限主表 | `openplatform_v2_api_t` + `openplatform_v2_permission_t` |
| `openplatform_permission_api_p_t` | API | API权限属性表 | `openplatform_v2_api_p_t` |
| `openplatform_event_t` | 事件 | 事件主表 | `openplatform_v2_event_t` |
| `openplatform_event_p_t` | 事件 | 事件属性表 | `openplatform_v2_event_p_t` |
| `openplatform_eflow_t` | 审批 | 审批流程表 | `openplatform_v2_approval_flow_t` + `openplatform_v2_approval_record_t` |
| `openplatform_eflow_log_t` | 审批 | 审批流程日志表 | `openplatform_v2_approval_log_t` |
| `openplatform_eflow_log_doc_t` | 审批 | 审批流程日志文档关联表 | 合并到 `openplatform_v2_approval_record_t` |

**总计**：10 张表（历史版本，重构前）

---

## 表结构定义

### 分组节点表

```sql
-- ============================================
-- 分组节点表（分类分组）
-- ============================================
CREATE TABLE `openplatform_module_node_t` (
    `id` BIGINT(20) PRIMARY KEY,
    `node_name_cn` VARCHAR(100) COMMENT '节点中文名称',
    `node_name_en` VARCHAR(100) COMMENT '节点英文名称',
    `parent_Node_id` BIGINT(20) COMMENT '父节点ID（⚠️ 字段命名不一致：大写N）',
    `is_parent_node` TINYINT(10) COMMENT '是否父节点',
    `is_leaf_node` TINYINT(10) COMMENT '是否叶子节点',
    `order_num` INT COMMENT '排序号',
    `auth_type` TINYINT(10) COMMENT '认证方式: 0=Cookie, 1=SOA, 2=APIG, 3=IAM, 4=免认证, 5=AKSK, 6=CLITOKEN',
    `status` TINYINT(10) DEFAULT 1 COMMENT '0=禁用, 1=启用',
    `create_by` VARCHAR(100),
    `create_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    `last_update_by` VARCHAR(100),
    `last_update_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    KEY `idx_parent_node_id` (`parent_Node_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分组节点表';
```

#### 与 v2 差异

- ⚠️ **字段命名不一致**：`parent_Node_id` 使用大写 `N`，其他字段为小写下划线风格
- ⚠️ **命名风格**：`node_name_cn/en` vs v2 的 `name_cn/en`
- ⚠️ **缺少分类别名**：v2 新增 `category_alias` 字段，用于根分类标识
- ⚠️ **缺少路径字段**：v2 新增 `path` 字段，用于子树查询优化
- ⚠️ **缺少责任人关联**：v2 新增 `openplatform_v2_category_owner_t` 表管理分类责任人
- **字段简化**：`is_parent_node`/`is_leaf_node` 在 v2 中通过 `parent_id` 逻辑推断

---

### 权限主表

```sql
-- ============================================
-- 权限主表
-- ============================================
CREATE TABLE `openplatform_permission_t` (
    `id` BIGINT(20) PRIMARY KEY,
    `permission_name_cn` VARCHAR(100) COMMENT '权限中文名称',
    `permission_name_en` VARCHAR(100) COMMENT '权限英文名称',
    `module_id` BIGINT(20) COMMENT '所属模块ID',
    `scope_id` VARCHAR(200) COMMENT '权限标识',
    `permisssion_type` VARCHAR(20) COMMENT '权限类型（⚠️ 拼写错误：3个s）',
    `is_approval_required` TINYINT(10) COMMENT '是否需要审批',
    `auth_type` TINYINT(10) COMMENT '认证方式: 0=Cookie, 1=SOA, 2=APIG, 3=IAM, 4=免认证, 5=AKSK, 6=CLITOKEN',
    `status` TINYINT(10) DEFAULT 1 COMMENT '0=禁用, 1=启用',
    `create_by` VARCHAR(100),
    `create_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    `last_update_by` VARCHAR(100),
    `last_update_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    KEY `idx_module_id` (`module_id`),
    KEY `idx_scope_id` (`scope_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限主表';
```

#### 与 v2 差异

- ⚠️ **拼写错误**：`permisssion_type`（3个s）vs v2 的 `resource_type`
- ⚠️ **命名风格**：`permission_name_cn/en` vs v2 的 `name_cn/en`
- ⚠️ **字段语义**：`module_id` vs v2 的 `category_id`（模块→分类）
- ⚠️ **字段语义**：`scope_id` vs v2 的 `scope`（标识字段简化）
- ⚠️ **缺少资源关联**：v2 新增 `resource_type`/`resource_id` 字段，明确权限与资源的关联关系
- ⚠️ **缺少审批节点配置**：v2 新增 `need_approval`/`resource_nodes` 字段，支持多级审批

---

### 权限属性表

```sql
-- ============================================
-- 权限属性表
-- ============================================
CREATE TABLE `openplatform_permission_p_t` (
    `id` BIGINT(20) PRIMARY KEY,
    `parent_id` BIGINT(20) NOT NULL COMMENT '关联权限主表 ID',
    `property_name` VARCHAR(100) NOT NULL COMMENT '属性名称',
    `property_value` TEXT COMMENT '属性值',
    `status` TINYINT(10) DEFAULT 1 COMMENT '0=禁用, 1=启用',
    `create_by` VARCHAR(100),
    `create_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    `last_update_by` VARCHAR(100),
    `last_update_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    KEY `idx_parent_id` (`parent_id`),
    KEY `idx_property_name` (`property_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限属性表';
```

#### 主要属性名称

- `audit_user`：审批用户

#### 与 v2 差异

- **结构基本一致**：v2 沿用相同的设计模式
- **审批字段提升**：v2 将 `need_approval`/`resource_nodes` 提升到主表，属性表仅存储扩展属性

---

### API权限主表

```sql
-- ============================================
-- API权限主表
-- ============================================
CREATE TABLE `openplatform_permission_api_t` (
    `id` BIGINT(20) PRIMARY KEY,
    `api_name_cn` VARCHAR(100) COMMENT 'API中文名称',
    `api_name_en` VARCHAR(100) COMMENT 'API英文名称',
    `permission_id` BIGINT(20) COMMENT '关联权限ID',
    `api_path` VARCHAR(500) COMMENT 'API路径',
    `api_method` VARCHAR(10) COMMENT 'HTTP方法',
    `auth_type` TINYINT(10) COMMENT '认证方式: 0=Cookie, 1=SOA, 2=APIG, 3=IAM, 4=免认证, 5=AKSK, 6=CLITOKEN',
    `status` TINYINT(10) DEFAULT 1 COMMENT '0=禁用, 1=启用',
    `create_by` VARCHAR(100),
    `create_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    `last_update_by` VARCHAR(100),
    `last_update_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    KEY `idx_permission_id` (`permission_id`),
    KEY `idx_api_path` (`api_path`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='API权限主表';
```

#### 与 v2 差异

- ⚠️ **API与权限耦合**：旧设计通过 `permission_id` 将 API 和权限强耦合，v2 解耦为独立的 `openplatform_v2_api_t` 和 `openplatform_v2_permission_t`
- ⚠️ **命名风格**：`api_name_cn/en` vs v2 的 `name_cn/en`
- ⚠️ **字段命名**：`api_path`/`api_method` vs v2 的 `path`/`method`（字段前缀冗余）
- ⚠️ **资源独立管理**：v2 将 API 作为独立资源管理，通过 `permission_t.resource_type='api'` 和 `resource_id` 关联

---

### API权限属性表

```sql
-- ============================================
-- API权限属性表
-- ============================================
CREATE TABLE `openplatform_permission_api_p_t` (
    `id` BIGINT(20) PRIMARY KEY,
    `parent_id` BIGINT(20) NOT NULL COMMENT '关联 API权限主表 ID',
    `property_name` VARCHAR(100) NOT NULL COMMENT '属性名称',
    `property_value` TEXT COMMENT '属性值',
    `status` TINYINT(10) DEFAULT 1 COMMENT '0=禁用, 1=启用',
    `create_by` VARCHAR(100),
    `create_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    `last_update_by` VARCHAR(100),
    `last_update_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    KEY `idx_parent_id` (`parent_id`),
    KEY `idx_property_name` (`property_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='API权限属性表';
```

#### 主要属性名称

- `doc_url`：文档URL
- `apim_endpoint_id`：API网关端点ID
- `apig_sign_key`：API网关签名密钥

#### 与 v2 差异

- **结构基本一致**：v2 沿用相同的设计模式
- **父表变更**：v2 关联独立的 `openplatform_v2_api_t` 主表

---

### 事件主表

```sql
-- ============================================
-- 事件主表
-- ============================================
CREATE TABLE `openplatform_event_t` (
    `id` BIGINT(20) PRIMARY KEY,
    `event_name_cn` VARCHAR(100) COMMENT '事件中文名称',
    `event_name_en` VARCHAR(100) COMMENT '事件英文名称',
    `module_id` BIGINT(20) COMMENT '所属模块ID',
    `topic` VARCHAR(200) COMMENT 'Topic',
    `event_type` VARCHAR(50) COMMENT '事件类型',
    `is_approval_required` TINYINT(10) COMMENT '是否需要审批',
    `status` TINYINT(10) DEFAULT 1 COMMENT '0=禁用, 1=启用',
    `create_by` VARCHAR(100),
    `create_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    `last_update_by` VARCHAR(100),
    `last_update_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    KEY `idx_module_id` (`module_id`),
    KEY `idx_topic` (`topic`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='事件主表';
```

#### 与 v2 差异

- ⚠️ **命名风格**：`event_name_cn/en` vs v2 的 `name_cn/en`
- ⚠️ **模块关联**：`module_id` vs v2 的 `category_id`（模块→分类）
- ⚠️ **冗余字段**：`event_type` 在 v2 中已移除
- ⚠️ **审批字段位置**：`is_approval_required` 在 v2 中移到 `permission_t` 主表
- ⚠️ **缺少事件-权限关联**：旧设计中事件和权限无直接关联关系，v2 通过 `permission_t.resource_type='event'` 建立关联

---

### 事件属性表

```sql
-- ============================================
-- 事件属性表
-- ============================================
CREATE TABLE `openplatform_event_p_t` (
    `id` BIGINT(20) PRIMARY KEY,
    `parent_id` BIGINT(20) NOT NULL COMMENT '关联事件主表 ID',
    `property_name` VARCHAR(100) NOT NULL COMMENT '属性名称',
    `property_value` TEXT COMMENT '属性值',
    `status` TINYINT(10) DEFAULT 1 COMMENT '0=禁用, 1=启用',
    `create_by` VARCHAR(100),
    `create_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    `last_update_by` VARCHAR(100),
    `last_update_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    KEY `idx_parent_id` (`parent_id`),
    KEY `idx_property_name` (`property_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='事件属性表';
```

#### 主要属性名称

- `audit_user`：审批用户

#### 与 v2 差异

- **结构基本一致**：v2 沿用相同的设计模式

---

### 审批流程表

```sql
-- ============================================
-- 审批流程表
-- ============================================
CREATE TABLE `openplatform_eflow_t` (
    `eflow_id` BIGINT(20) PRIMARY KEY,
    `eflow_type` VARCHAR(50) COMMENT '审批流程类型',
    `eflow_status` TINYINT(10) COMMENT '审批状态: 0=待审, 1=已通过, 2=已拒绝, 3=已撤销',
    `eflow_submit_user` VARCHAR(100) COMMENT '提交用户',
    `eflow_submit_message` TEXT COMMENT '提交消息',
    `eflow_audit_user` VARCHAR(100) COMMENT '审批用户',
    `eflow_audit_message` TEXT COMMENT '审批消息',
    `resource_type` VARCHAR(50) COMMENT '资源类型',
    `resource_id` BIGINT(20) COMMENT '资源ID',
    `resource_info` TEXT COMMENT '资源信息',
    `resource_delta` TEXT COMMENT '资源变更信息',
    `resource_type` VARCHAR(50) COMMENT '资源类型（⚠️ 字段重复定义）',
    `tenant_id` VARCHAR(100) COMMENT '租户ID',
    `status` TINYINT(10) DEFAULT 1 COMMENT '0=禁用, 1=启用',
    `create_by` VARCHAR(100),
    `create_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    `last_update_by` VARCHAR(100),
    `last_update_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    KEY `idx_eflow_type` (`eflow_type`),
    KEY `idx_eflow_status` (`eflow_status`),
    KEY `idx_resource` (`resource_type`, `resource_id`),
    KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批流程表';
```

#### 与 v2 差异

- ⚠️ **字段重复定义**：`resource_type` 在表定义中出现两次
- ⚠️ **模板与记录混合**：旧设计将审批模板和审批记录混在同一张表，v2 分离为 `openplatform_v2_approval_flow_t`（模板）和 `openplatform_v2_approval_record_t`（记录）
- ⚠️ **主键命名**：`eflow_id` vs v2 的 `id`（统一命名风格）
- ⚠️ **业务关联简化**：`resource_info`/`resource_delta` vs v2 的 `business_type`+`business_id`（结构化存储）
- ⚠️ **缺少节点配置**：v2 新增 `combined_nodes`/`current_node` 字段，支持多级审批
- ⚠️ **缺少申请人信息**：v2 新增 `applicant_id`/`applicant_name` 字段

---

### 审批流程日志表

```sql
-- ============================================
-- 审批流程日志表
-- ============================================
CREATE TABLE `openplatform_eflow_log_t` (
    `eflow_log_id` BIGINT(20) PRIMARY KEY,
    `eflow_log_trace_id` BIGINT(20) COMMENT '追踪ID（关联审批流程）',
    `eflow_log_type` VARCHAR(50) COMMENT '日志类型（文本描述）',
    `eflow_log_user` VARCHAR(100) COMMENT '操作用户',
    `eflow_log_message` TEXT COMMENT '日志消息',
    `status` TINYINT(10) DEFAULT 1 COMMENT '0=禁用, 1=启用',
    `create_by` VARCHAR(100),
    `create_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    `last_update_by` VARCHAR(100),
    `last_update_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    KEY `idx_eflow_log_trace_id` (`eflow_log_trace_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批流程日志表';
```

#### 与 v2 差异

- ⚠️ **主键命名**：`eflow_log_id` vs v2 的 `id`（统一命名风格）
- ⚠️ **关联字段命名**：`eflow_log_trace_id` vs v2 的 `record_id`（语义更清晰）
- ⚠️ **类型存储**：`eflow_log_type` 使用文本描述，v2 的 `action` 使用枚举（0=同意, 1=拒绝, 2=撤销, 3=转交）
- ⚠️ **缺少节点索引**：v2 新增 `node_index` 字段，标识当前审批节点
- ⚠️ **缺少审批级别**：v2 新增 `level` 字段（global=全局, scene=场景, resource=资源）

---

### 审批流程日志文档关联表

```sql
-- ============================================
-- 审批流程日志文档关联表
-- ============================================
CREATE TABLE `openplatform_eflow_log_doc_t` (
    `eflow_log_doc_id` BIGINT(20) PRIMARY KEY,
    `doc_type` VARCHAR(50) COMMENT '文档类型',
    `doc_id` BIGINT(20) COMMENT '文档ID',
    `eflow_id` BIGINT(20) COMMENT '审批流程ID',
    `eflow_log_id` BIGINT(20) COMMENT '审批流程日志ID',
    `status` TINYINT(10) DEFAULT 1 COMMENT '0=禁用, 1=启用',
    `create_by` VARCHAR(100),
    `create_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    `last_update_by` VARCHAR(100),
    `last_update_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    KEY `idx_eflow_id` (`eflow_id`),
    KEY `idx_eflow_log_id` (`eflow_log_id`),
    KEY `idx_doc` (`doc_type`, `doc_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批流程日志文档关联表';
```

#### 与 v2 差异

- ⚠️ **表设计废弃**：v2 不再需要独立的文档关联表
- ⚠️ **功能合并**：v2 将文档关联信息直接存储在 `openplatform_v2_approval_record_t` 的业务字段中

---

## 命名对比总结

| 旧版命名 | v2 命名 | 说明 |
|----------|---------|------|
| `node_name_cn/en` | `name_cn/en` | 去除前缀，统一命名风格 |
| `permission_name_cn/en` | `name_cn/en` | 去除前缀，统一命名风格 |
| `api_name_cn/en` | `name_cn/en` | 去除前缀，统一命名风格 |
| `event_name_cn/en` | `name_cn/en` | 去除前缀，统一命名风格 |
| `parent_Node_id` | `parent_id` | 修正大小写不一致问题 |
| `module_id` | `category_id` | 模块→分类，语义更准确 |
| `scope_id` | `scope` | 去除后缀，简化命名 |
| `permisssion_type` | `resource_type` | 修正拼写错误，语义更准确 |
| `api_path/api_method` | `path/method` | 去除前缀冗余 |
| `eflow_id` | `id` | 统一主键命名风格 |
| `eflow_log_id` | `id` | 统一主键命名风格 |
| `eflow_log_trace_id` | `record_id` | 语义更清晰 |

---

## 核心重构点

### 1. API与权限解耦 ⚠️

**旧设计**：API和权限通过 `permission_id` 强耦合
```
openplatform_permission_api_t
└── permission_id → openplatform_permission_t
```

**v2 设计**：API和权限独立管理，通过资源类型关联
```
openplatform_v2_api_t (独立资源)
openplatform_v2_permission_t
└── resource_type='api', resource_id → openplatform_v2_api_t
```

### 2. 审批流程重构 ⚠️

**旧设计**：模板和记录混在同一张表
```
openplatform_eflow_t (模板 + 记录)
openplatform_eflow_log_t (日志)
openplatform_eflow_log_doc_t (文档关联)
```

**v2 设计**：模板和记录分离，支持多级审批
```
openplatform_v2_approval_flow_t (模板)
openplatform_v2_approval_record_t (记录 + 组合节点)
openplatform_v2_approval_log_t (日志 + 节点索引)
```

### 3. 事件-权限关联 ⚠️

**旧设计**：事件和权限无直接关联，通过 `module_id` 间接关联

**v2 设计**：事件作为资源，通过 `permission_t.resource_type='event'` 建立直接关联

---

## 迁移注意事项

1. **字段命名规范化**：所有 `*_name_cn/en` 改为 `name_cn/en`
2. **大小写修正**：`parent_Node_id` 改为 `parent_id`
3. **拼写错误修正**：`permisssion_type` 改为 `resource_type`
4. **API-权限解耦**：需要重新建立 API 和权限的关联关系
5. **审批流程拆分**：需要区分模板数据和记录数据
6. **新增字段填充**：`category_alias`、`path`、`resource_nodes` 等新字段需要数据迁移脚本计算填充
