# 数据迁移方案

## 1. 数据对象分类

### 1.1 静态数据（1-7）- 平台配置数据

| 序号 | 数据对象 | 旧表 | 新表 |
|------|----------|------|------|
| 1 | 分类数据 | openplatform_module_node_t | openplatform_v2_category_t |
| 2 | API数据 | openplatform_permission_api_t | openplatform_v2_api_t |
| 3 | 事件数据 | openplatform_event_t | openplatform_v2_event_t |
| 4 | 回调数据 | - | openplatform_v2_callback_t |
| 5 | API权限数据 | openplatform_permission_t | openplatform_v2_permission_t |
| 6 | 事件权限数据 | openplatform_permission_t | openplatform_v2_permission_t |
| 7 | 回调权限数据 | - | openplatform_v2_permission_t |

**处理方式**：新旧系统独立运行，各自使用各自的接口操作各自的数据表

**无需同步**：有数据差异时按正常流程各自处理，不需要专门的数据同步机制

### 1.2 动态数据（8-9）- 用户操作数据

| 序号 | 数据对象 | 旧表 | 新表 | 同步方式 |
|------|----------|------|------|----------|
| 8 | 订阅关系数据 | openplatform_app_permission_t | openplatform_v2_subscription_t | **双向同步** |
| 9 | 审批数据 | openplatform_eflow_t + openplatform_eflow_log_t | openplatform_v2_approval_flow_t + openplatform_v2_approval_record_t + openplatform_v2_approval_log_t | **双向同步** |

**处理方式**：需要双向同步
- 迁移场景：旧→新（用户在旧系统操作的数据同步到新系统）
- 回退场景：新→旧（用户在新系统操作的数据同步到旧系统）

**需要开发同步接口**

---

## 2. 静态数据处理说明

### 2.1 独立运行原则

```
旧系统：使用旧接口操作旧表
新系统：使用新接口操作新表

两套系统独立维护各自的数据
```

**核心原则**：两套系统独立运行，无需同步

### 2.2 数据差异处理

- 数据差异是正常现象
- 各自按照正常的操作流程处理
- 不需要专门的数据同步机制

**示例**：
- 旧系统管理员在旧系统创建了一个新API分类
- 新系统管理员在新系统创建了一个新API分类
- 两个分类可以不同，各自独立维护
- 不需要进行数据同步

### 2.3 表结构对照（仅作参考）

| 序号 | 数据对象 | 旧表 | 新表 | 关键字段 |
|------|----------|------|------|----------|
| 1 | 分类数据 | openplatform_module_node_t | openplatform_v2_category_t | name_cn, name_en, parent_id, category_alias |
| 2 | API数据 | openplatform_permission_api_t | openplatform_v2_api_t | name_cn, name_en, path, method, auth_type |
| 3 | 事件数据 | openplatform_event_t | openplatform_v2_event_t | name_cn, name_en, topic |
| 4 | 回调数据 | - | openplatform_v2_callback_t | name_cn, name_en |
| 5 | API权限数据 | openplatform_permission_t | openplatform_v2_permission_t | scope, resource_type, resource_id, category_id |
| 6 | 事件权限数据 | openplatform_permission_t | openplatform_v2_permission_t | scope, resource_type, resource_id, category_id |
| 7 | 回调权限数据 | - | openplatform_v2_permission_t | scope, resource_type, resource_id, category_id |

**说明**：上表仅作参考，不涉及数据同步

---

## 3. 动态数据同步方案

### 3.1 订阅关系同步

#### 旧表结构
```sql
CREATE TABLE `openplatform_app_permission_t` (
    `id` BIGINT(20) PRIMARY KEY,
    `app_id` BIGINT(20) COMMENT '应用ID',
    `permission_id` BIGINT(20) COMMENT '权限ID',
    `status` TINYINT(10) COMMENT '状态',
    `channel_type` TINYINT(10) COMMENT '通道类型',
    `channel_address` VARCHAR(500) COMMENT '通道地址',
    `auth_type` TINYINT(10) COMMENT '认证类型',
    `create_time` DATETIME(3),
    `last_update_time` DATETIME(3),
    `create_by` VARCHAR(100),
    `last_update_by` VARCHAR(100)
);
```

#### 新表结构
```sql
CREATE TABLE `openplatform_v2_subscription_t` (
    `id` BIGINT(20) PRIMARY KEY,
    `app_id` BIGINT(20) NOT NULL,
    `permission_id` BIGINT(20) NOT NULL,
    `status` TINYINT(10) COMMENT '0=待审,1=已授权,2=已拒绝,3=已取消',
    `channel_type` TINYINT(10),
    `channel_address` VARCHAR(500),
    `auth_type` TINYINT(10),
    `approved_at` DATETIME(3),
    `approved_by` VARCHAR(100),
    `create_time` DATETIME(3),
    `last_update_time` DATETIME(3),
    `create_by` VARCHAR(100),
    `last_update_by` VARCHAR(100)
);
```

#### 字段映射关系
| 旧字段 | 新字段 | 说明 |
|--------|--------|------|
| id | id | 主键保持不变 |
| app_id | app_id | 应用ID |
| permission_id | permission_id | 需要映射到新权限ID |
| status | status | 状态值保持一致 |
| channel_type | channel_type | 通道类型 |
| channel_address | channel_address | 通道地址 |
| auth_type | auth_type | 认证类型 |
| - | approved_at | 新增：审批通过时间 |
| - | approved_by | 新增：审批人 |
| create_time | create_time | 创建时间 |
| last_update_time | last_update_time | 更新时间 |
| create_by | create_by | 创建人 |
| last_update_by | last_update_by | 更新人 |

#### 同步接口设计
```
POST /api/v1/sync/subscription/migrate   # 旧→新（迁移）
POST /api/v1/sync/subscription/rollback  # 新→旧（回退）
POST /api/v1/sync/subscription/batch     # 批量同步
GET  /api/v1/sync/subscription/status    # 同步状态查询
```

---

### 3.2 审批数据同步

#### 旧表结构

**审批流程表**：
```sql
CREATE TABLE `openplatform_eflow_t` (
    `eflow_id` BIGINT(20) PRIMARY KEY,
    `eflow_type` VARCHAR(50) COMMENT '审批流程类型',
    `eflow_status` TINYINT(10) COMMENT '审批状态',
    `eflow_submit_user` VARCHAR(100) COMMENT '提交用户',
    `eflow_submit_message` TEXT COMMENT '提交消息',
    `eflow_audit_user` VARCHAR(100) COMMENT '审批用户',
    `eflow_audit_message` TEXT COMMENT '审批消息',
    `resource_type` VARCHAR(50) COMMENT '资源类型',
    `resource_id` BIGINT(20) COMMENT '资源ID',
    `resource_info` TEXT COMMENT '资源信息',
    `resource_delta` TEXT COMMENT '资源变更信息',
    `tenant_id` VARCHAR(100),
    `create_time` DATETIME(3),
    `last_update_time` DATETIME(3)
);
```

**审批日志表**：
```sql
CREATE TABLE `openplatform_eflow_log_t` (
    `eflow_log_id` BIGINT(20) PRIMARY KEY,
    `eflow_log_trace_id` BIGINT(20) COMMENT '追踪ID',
    `eflow_log_type` VARCHAR(50) COMMENT '日志类型',
    `eflow_log_user` VARCHAR(100) COMMENT '操作用户',
    `eflow_log_message` TEXT COMMENT '日志消息',
    `create_time` DATETIME(3)
);
```

#### 新表结构

**审批流程模板表**：
```sql
CREATE TABLE `openplatform_v2_approval_flow_t` (
    `id` BIGINT(20) PRIMARY KEY,
    `name_cn` VARCHAR(100),
    `name_en` VARCHAR(100),
    `code` VARCHAR(50) UNIQUE,
    `description_cn` TEXT,
    `description_en` TEXT,
    `nodes` VARCHAR(2000) COMMENT '审批节点配置JSON',
    `status` TINYINT(10),
    `create_time` DATETIME(3),
    `last_update_time` DATETIME(3)
);
```

**审批记录表**：
```sql
CREATE TABLE `openplatform_v2_approval_record_t` (
    `id` BIGINT(20) PRIMARY KEY,
    `combined_nodes` VARCHAR(4000) COMMENT '组合审批节点配置JSON',
    `business_type` VARCHAR(50) COMMENT '业务类型',
    `business_id` BIGINT(20) COMMENT '业务ID',
    `applicant_id` VARCHAR(100) COMMENT '申请人ID',
    `applicant_name` VARCHAR(100),
    `status` TINYINT(10) COMMENT '0=待审,1=已通过,2=已拒绝,3=已撤销',
    `current_node` INT COMMENT '当前审批节点索引',
    `create_time` DATETIME(3),
    `last_update_time` DATETIME(3),
    `completed_at` DATETIME(3)
);
```

**审批操作日志表**：
```sql
CREATE TABLE `openplatform_v2_approval_log_t` (
    `id` BIGINT(20) PRIMARY KEY,
    `record_id` BIGINT(20) COMMENT '审批记录ID',
    `node_index` INT COMMENT '节点索引',
    `level` VARCHAR(20) COMMENT '审批级别',
    `operator_id` VARCHAR(100) COMMENT '操作人ID',
    `operator_name` VARCHAR(100),
    `action` TINYINT(10) COMMENT '0=同意,1=拒绝,2=撤销,3=转交',
    `comment` TEXT,
    `create_time` DATETIME(3)
);
```

#### 字段映射关系

**模板数据**：从 `openplatform_eflow_t` 中提取模板数据，生成 `openplatform_v2_approval_flow_t`

| 旧字段 | 新字段 | 说明 |
|--------|--------|------|
| eflow_type | code | 流程编码 |
| - | name_cn | 流程名称（新增） |
| - | nodes | 审批节点配置（新增） |

**记录数据**：从 `openplatform_eflow_t` 中提取记录数据，生成 `openplatform_v2_approval_record_t`

| 旧字段 | 新字段 | 说明 |
|--------|--------|------|
| eflow_id | id | 主键 |
| resource_type | business_type | 业务类型 |
| resource_id | business_id | 业务ID |
| eflow_submit_user | applicant_id | 申请人 |
| - | applicant_name | 申请人姓名（新增） |
| eflow_status | status | 审批状态 |
| - | combined_nodes | 组合节点配置（新增） |
| - | current_node | 当前节点索引（新增） |
| eflow_audit_message | - | 拆分到日志表 |

**日志数据**：从 `openplatform_eflow_log_t` 迁移到 `openplatform_v2_approval_log_t`

| 旧字段 | 新字段 | 说明 |
|--------|--------|------|
| eflow_log_id | id | 主键 |
| eflow_log_trace_id | record_id | 关联审批记录 |
| - | node_index | 节点索引（新增） |
| - | level | 审批级别（新增） |
| eflow_log_user | operator_id | 操作人 |
| eflow_log_type | action | 操作类型（需转换） |
| eflow_log_message | comment | 备注 |

#### 同步接口设计
```
POST /api/v1/sync/approval/migrate       # 旧→新（迁移）
POST /api/v1/sync/approval/rollback      # 新→旧（回退）
POST /api/v1/sync/approval/batch         # 批量同步
GET  /api/v1/sync/approval/status        # 同步状态查询
```

---

## 4. 同步接口设计

### 4.1 接口列表

**订阅关系同步接口**：
```
POST /api/v1/sync/subscription/migrate   # 迁移（旧→新）
POST /api/v1/sync/subscription/rollback  # 回退（新→旧）
POST /api/v1/sync/subscription/batch     # 批量同步
GET  /api/v1/sync/subscription/status    # 状态查询
```

**审批数据同步接口**：
```
POST /api/v1/sync/approval/migrate       # 迁移（旧→新）
POST /api/v1/sync/approval/rollback      # 回退（新→旧）
POST /api/v1/sync/approval/batch         # 批量同步
GET  /api/v1/sync/approval/status        # 状态查询
```

### 4.2 接口示例

**批量同步接口**：
```java
@PostMapping("/sync/{module}/batch")
public SyncResult syncBatch(
    @PathVariable String module,
    @RequestBody SyncRequest request) {
    // request.ids: 要同步的数据ID列表
    // request.direction: "migrate" 或 "rollback"
}

// 请求示例
{
    "ids": [1, 2, 3],
    "direction": "migrate"  // 或 "rollback"
}

// 响应示例
{
    "success": 10,
    "failed": 2,
    "details": [
        {"id": 1, "status": "success"},
        {"id": 5, "status": "failed", "error": "权限ID映射失败"}
    ]
}
```

**状态查询接口**：
```java
@GetMapping("/sync/{module}/status")
public SyncStatus getStatus(@PathVariable String module) {
    // 返回：源数据数量、目标数据数量、同步进度
}

// 响应示例
{
    "sourceCount": 100,
    "targetCount": 95,
    "pending": 5,
    "lastSyncTime": "2024-01-15 10:30:00"
}
```

### 4.3 同步日志表

```sql
CREATE TABLE `openplatform_sync_log_t` (
    `id` BIGINT(20) PRIMARY KEY,
    `module` VARCHAR(50) NOT NULL COMMENT '模块：subscription/approval',
    `direction` VARCHAR(20) NOT NULL COMMENT '方向：migrate/rollback',
    `source_id` BIGINT(20) COMMENT '源数据ID',
    `target_id` BIGINT(20) COMMENT '目标数据ID',
    `status` TINYINT(10) COMMENT '状态：0=失败,1=成功',
    `error_message` TEXT COMMENT '错误信息',
    `create_time` DATETIME(3),
    KEY `idx_module` (`module`),
    KEY `idx_source_id` (`source_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据同步日志表';
```

### 4.4 Java代码示例

```java
@RestController
@RequestMapping("/api/v1/sync")
public class SyncController {
    
    @PostMapping("/{module}/batch")
    public SyncResult syncBatch(
        @PathVariable String module,
        @RequestBody SyncRequest request) {
        // 批量同步
    }
    
    @GetMapping("/{module}/status")
    public SyncStatus getStatus(@PathVariable String module) {
        // 状态查询
    }
}
```

---

## 5. HTML工具

HTML同步工具只支持动态数据同步：
- 订阅关系同步
- 审批数据同步

**不支持静态数据同步**（静态数据由新旧系统独立维护）
