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
| 9 | 审批数据 | openplatform_eflow_t + openplatform_eflow_log_t | openplatform_v2_approval_record_t + openplatform_v2_approval_log_t | **双向同步** |
| 9.1 | 审批流程模板 | - | openplatform_v2_approval_flow_t | **平台后台新建**（新增功能表） |

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

### 2.3 数据导入操作流程

当需要在新系统导入静态数据时，按以下流程操作：

#### 步骤1：手动查询现有数据

通过以下方式获取旧系统现有数据：

```sql
-- 查询分类数据
SELECT * FROM openplatform_module_node_t WHERE status = 1;

-- 查询API数据
SELECT * FROM openplatform_permission_api_t WHERE status = 1;

-- 查询事件数据
SELECT * FROM openplatform_event_t WHERE status = 1;

-- 查询权限数据
SELECT * FROM openplatform_permission_t WHERE status = 1;
```

**说明**：可通过数据库查询或旧系统管理界面导出数据。

---

#### 步骤2：Excel构造接口参数

根据查询的数据，在Excel中构造新系统接口的请求参数：

| 数据类型 | 接口 | 必填参数 | 示例 |
|----------|------|----------|------|
| 分类数据 | POST /api/v2/categories | name_cn, name_en, parent_id | {"name_cn":"消息API","name_en":"Message API","parent_id":1} |
| API数据 | POST /api/v2/apis | name_cn, name_en, path, method, auth_type | {"name_cn":"发送消息","name_en":"Send Message","path":"/api/send","method":"POST","auth_type":5} |
| 事件数据 | POST /api/v2/events | name_cn, name_en, topic | {"name_cn":"消息接收","name_en":"Message Received","topic":"message.received"} |
| 回调数据 | POST /api/v2/callbacks | name_cn, name_en | {"name_cn":"审批完成","name_en":"Approval Completed"} |
| 权限数据 | POST /api/v2/permissions | scope, resource_type, resource_id, category_id | {"scope":"api:im:send","resource_type":"api","resource_id":1,"category_id":1} |

**Excel列设计**：
- A列：数据ID
- B列：接口路径
- C列：请求参数（JSON格式）
- D列：执行状态
- E列：备注

---

#### 步骤3：调现有接口按批次执行

使用工具（如Postman、curl）按批次调用新系统接口：

**批量执行方式**：
1. 使用Postman Collection Runner批量执行
2. 使用curl脚本循环执行
3. 使用Python/Shell脚本批量调用

**示例（curl批量执行）**：
```bash
# 执行分类数据导入
for row in $(cat categories.csv); do
    curl -X POST "$API_URL/api/v2/categories" \
         -H "Authorization: Bearer $TOKEN" \
         -H "Content-Type: application/json" \
         -d "$row"
done

# 执行API数据导入
for row in $(cat apis.csv); do
    curl -X POST "$API_URL/api/v2/apis" \
         -H "Authorization: Bearer $TOKEN" \
         -H "Content-Type: application/json" \
         -d "$row"
done
```

**注意事项**：
- 建议按模块分批次执行（如每批50条）
- 执行前先验证参数格式
- 记录执行结果和失败原因

---

#### 步骤4：数据库脚本一键验证

导入完成后，执行验证脚本确认数据完整性：

```sql
-- ============================================
-- 数据导入验证脚本
-- ============================================

-- 验证分类数据
SELECT 
    '分类数据' AS module,
    COUNT(*) AS count
FROM openplatform_v2_category_t
WHERE status = 1;

-- 验证API数据
SELECT 
    'API数据' AS module,
    COUNT(*) AS count
FROM openplatform_v2_api_t
WHERE status IN (1, 2);

-- 验证事件数据
SELECT 
    '事件数据' AS module,
    COUNT(*) AS count
FROM openplatform_v2_event_t
WHERE status IN (1, 2);

-- 验证权限数据
SELECT 
    '权限数据' AS module,
    COUNT(*) AS count,
    SUM(CASE WHEN resource_type = 'api' THEN 1 ELSE 0 END) AS api_permission,
    SUM(CASE WHEN resource_type = 'event' THEN 1 ELSE 0 END) AS event_permission,
    SUM(CASE WHEN resource_type = 'callback' THEN 1 ELSE 0 END) AS callback_permission
FROM openplatform_v2_permission_t
WHERE status = 1;

-- 验证权限与资源关联完整性
SELECT 
    '权限-资源关联缺失' AS issue,
    p.id,
    p.scope,
    p.resource_type,
    p.resource_id
FROM openplatform_v2_permission_t p
LEFT JOIN openplatform_v2_api_t a ON p.resource_type = 'api' AND p.resource_id = a.id
LEFT JOIN openplatform_v2_event_t e ON p.resource_type = 'event' AND p.resource_id = e.id
LEFT JOIN openplatform_v2_callback_t c ON p.resource_type = 'callback' AND p.resource_id = c.id
WHERE p.status = 1
AND (p.resource_type = 'api' AND a.id IS NULL)
OR (p.resource_type = 'event' AND e.id IS NULL)
OR (p.resource_type = 'callback' AND c.id IS NULL);
```

**验证要点**：
- 数据数量是否符合预期
- 关联关系是否完整（权限→资源）
- 字段值是否正确（如scope格式）

---

#### 操作流程总结

```
┌─────────────────────────────────────────────────────────┐
│  步骤1：手动查询现有数据                                  │
│  ├─ SQL查询或界面导出                                    │
│  └─ 导出为CSV/Excel格式                                 │
└─────────────────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────────────────┐
│  步骤2：Excel构造接口参数                                 │
│  ├─ 设计Excel列结构                                      │
│  ├─ 构造JSON请求参数                                     │
│  └─ 验证参数格式                                         │
└─────────────────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────────────────┐
│  步骤3：调现有接口按批次执行                              │
│  ├─ 选择执行工具（Postman/curl/脚本）                    │
│  ├─ 分批次执行（每批50条）                               │
│  └─ 记录执行结果                                         │
└─────────────────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────────────────┐
│  步骤4：数据库脚本一键验证                                │
│  ├─ 执行验证SQL脚本                                      │
│  ├─ 检查数据数量和关联                                   │
│  └─ 处理异常数据                                         │
└─────────────────────────────────────────────────────────┘
```

### 2.4 表结构对照（仅作参考）

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
    `app_id` BIGINT(20) NOT NULL COMMENT '应用ID',
    `permission_id` BIGINT(20) NOT NULL COMMENT '权限ID',
    `tenant_id` VARCHAR(100) COMMENT '租户ID',
    `permisssion_type` VARCHAR(20) COMMENT '权限类型（⚠️ 拼写错误：3个s）',
    `status` TINYINT(10) DEFAULT 1 COMMENT '0=禁用, 1=启用',
    `create_by` VARCHAR(100),
    `create_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    `last_update_by` VARCHAR(100),
    `last_update_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    KEY `idx_app_id` (`app_id`),
    KEY `idx_permission_id` (`permission_id`),
    KEY `idx_tenant_id` (`tenant_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='应用权限关联表';
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
| tenant_id | tenant_id | 租户ID（直接迁移） |
| permisssion_type | - | 移除（通过permission获取resource_type） |
| status | status | 状态值映射 |
| - | channel_type | 新增：通道类型（需补充） |
| - | channel_address | 新增：通道地址（需补充） |
| - | auth_type | 新增：认证类型（需补充） |
| - | approved_at | 新增：审批通过时间（需补充） |
| - | approved_by | 新增：审批人（需补充） |
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

#### 重要说明

**审批流程模板表（`openplatform_v2_approval_flow_t`）为新增功能表**：
- 旧系统没有审批流程模板的概念
- 该表需要在平台后台新建配置
- **不涉及数据同步**

**需要同步的数据**：
- `openplatform_v2_approval_record_t` - 审批记录（从旧表提取）
- `openplatform_v2_approval_log_t` - 审批日志（从旧表迁移）

#### 旧表结构

**审批流程表**：
```sql
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

**审批日志表**：
```sql
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
| tenant_id | - | 租户ID（迁移时记录） |
| status | - | 记录状态（启用/禁用） |
| create_by | - | 创建人（迁移时记录） |
| create_time | create_time | 创建时间 |
| last_update_by | - | 更新人（迁移时记录） |
| last_update_time | last_update_time | 更新时间 |

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
| status | - | 记录状态（启用/禁用） |
| create_by | - | 创建人（迁移时记录） |
| create_time | create_time | 创建时间 |
| last_update_by | - | 更新人（迁移时记录） |
| last_update_time | - | 更新时间（迁移时记录） |

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
