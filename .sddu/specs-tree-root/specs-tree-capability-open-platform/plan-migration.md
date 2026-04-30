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
| 8 | 订阅关系数据 | openplatform_app_permission_t | openplatform_v2_subscription_t | **双向同步（手动触发）** |
| 9 | 审批数据 | openplatform_eflow_t + openplatform_eflow_log_t | openplatform_v2_approval_record_t + openplatform_v2_approval_log_t | **双向同步（手动触发）** |
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

#### 验证原则

1. **分类数据**：验证数量一致即可
2. **API数据**：验证全量API数量一致即可
3. **事件数据**：验证数量即可
4. **权限数据**：不做单独验证（API/事件/回调注册接口会同步注册权限，保证接口功能前提下无需单独验证）
5. **权限与资源关联**：无需单独验证完整性

---

#### 步骤4：数据库脚本一键验证

导入完成后，执行验证脚本确认数据完整性：

```sql
-- ============================================
-- 数据导入验证脚本（简化版）
-- ============================================

-- 验证分类数据数量
SELECT 
    '分类数据' AS module,
    (SELECT COUNT(*) FROM openplatform_module_node_t) AS old_count,
    (SELECT COUNT(*) FROM openplatform_v2_category_t) AS new_count;

-- 验证API数据数量
SELECT 
    'API数据' AS module,
    (SELECT COUNT(*) FROM openplatform_permission_api_t) AS old_count,
    (SELECT COUNT(*) FROM openplatform_v2_api_t) AS new_count;

-- 验证事件数据数量
SELECT 
    '事件数据' AS module,
    (SELECT COUNT(*) FROM openplatform_event_t) AS old_count,
    (SELECT COUNT(*) FROM openplatform_v2_event_t) AS new_count;

-- 验证回调数据数量（新增功能）
SELECT 
    '回调数据' AS module,
    0 AS old_count,
    (SELECT COUNT(*) FROM openplatform_v2_callback_t) AS new_count;
```

**验证要点**：
- 数据数量是否符合预期（新旧数据量一致或符合业务预期）
- 回调数据为新增功能，旧表无对应数据

---

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
    `permisssion_type` VARCHAR(20) COMMENT '权限类型：0=API权限（对应permission_id为API权限ID）、1=事件（对应permission_id为事件ID）',
    `status` TINYINT(10) DEFAULT 1 COMMENT '0=待审核、1=已开通、2=驳回、3=关闭',
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

---

## 4. 同步接口设计

### 4.1 接口列表

**订阅关系同步接口**（自动同步关联的审批数据）：

| 接口 | 说明 |
|------|------|
| `POST /api/v1/sync/subscription/migrate` | 旧表→新表（迁移） |
| `POST /api/v1/sync/subscription/rollback` | 新表→旧表（回退） |

**说明**：
- 两个接口都支持批量同步（传入ID列表）和全量同步（不传ID或传空数组）
- 同步订阅关系时，自动同步关联的审批记录和审批日志
- 同步执行，无需状态查询

### 4.2 同步原则

#### 原则1：订阅关系优先
- 优先保证订阅关系同步成功
- 审批数据同步失败**不影响**订阅关系
- 审批数据同步失败时记录错误信息，返回给调用方

#### 原则2：支持重复执行
- 接口支持重复调用
- 已存在的数据自动跳过（通过ID判断）
- 只同步新增的数据

#### 原则3：同步顺序
```
订阅关系同步
    ↓
审批记录同步（失败不影响订阅关系）
    ↓
审批日志同步（失败不影响订阅关系）
```

#### 原则4：数据存在性判断
- 订阅关系：通过 `id` 判断是否已存在
- 审批记录：通过 `id` 判断是否已存在
- 审批日志：通过 `id` 判断是否已存在

#### 执行逻辑伪代码

```java
// 同步订阅关系（核心数据）
for (subscription : subscriptions) {
    if (exists(subscription.id)) {
        skip("订阅关系已存在");
        continue;
    }
    save(subscription);
    success++;
}

// 同步审批记录（辅助数据，失败不影响订阅关系）
for (approvalRecord : approvalRecords) {
    try {
        if (exists(approvalRecord.id)) {
            skip("审批记录已存在");
            continue;
        }
        save(approvalRecord);
    } catch (Exception e) {
        log.error("审批记录同步失败", e);
        // 不抛出异常，继续处理其他数据
    }
}

// 同步审批日志（辅助数据，失败不影响订阅关系）
for (approvalLog : approvalLogs) {
    try {
        if (exists(approvalLog.id)) {
            skip("审批日志已存在");
            continue;
        }
        save(approvalLog);
    } catch (Exception e) {
        log.error("审批日志同步失败", e);
        // 不抛出异常，继续处理其他数据
    }
}
```

### 4.3 接口示例

```java
// 迁移接口：旧表 → 新表
@PostMapping("/sync/subscription/migrate")
public SyncResult migrate(@RequestBody SyncRequest request) {
    // request.ids: 要迁移的订阅关系ID列表
    // - 传入ID列表：批量迁移指定数据
    // - 不传或传空数组：全量迁移
    
    // 同步逻辑：
    // 1. 从旧表读取订阅关系数据，写入新表
    // 2. 自动同步关联的审批记录
    // 3. 自动同步关联的审批日志
}

// 请求示例 - 批量迁移
{
    "ids": [1, 2, 3]
}

// 请求示例 - 全量迁移
{
    "ids": null  // 或 "ids": []
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

// 回退接口：新表 → 旧表
@PostMapping("/sync/subscription/rollback")
public SyncResult rollback(@RequestBody SyncRequest request) {
    // request.ids: 要回退的订阅关系ID列表
    // - 传入ID列表：批量回退指定数据
    // - 不传或传空数组：全量回退
    
    // 同步逻辑：
    // 1. 从新表读取订阅关系数据，写入旧表
    // 2. 自动同步关联的审批记录
    // 3. 自动同步关联的审批日志
}

// 请求示例 - 批量回退
{
    "ids": [1, 2, 3]
}

// 请求示例 - 全量回退
{
    "ids": null
}

// 响应示例
{
    "success": 10,
    "failed": 2,
    "details": [...]
}
```

### 4.4 Java代码示例

```java
@RestController
@RequestMapping("/api/v1/sync")
public class SyncController {
    
    @PostMapping("/subscription/migrate")
    public SyncResult migrate(@RequestBody SyncRequest request) {
        // 旧表 → 新表（批量/全量）
    }
    
    @PostMapping("/subscription/rollback")
    public SyncResult rollback(@RequestBody SyncRequest request) {
        // 新表 → 旧表（批量/全量）
    }
}

@Data
public class SyncRequest {
    private List<Long> ids;  // null或空数组=全量同步
}

@Data
public class SyncResult {
    private int success;
    private int failed;
    private List<SyncDetail> details;
}
```

### 4.5 接口调用命令

#### 迁移接口（旧表→新表）

**批量迁移指定订阅关系**：
```bash
curl -X POST "http://localhost:8080/api/v1/sync/subscription/migrate" \
  -H "Content-Type: application/json" \
  -H "Cookie: SESSIONID=your-session-id" \
  -d '{"ids": [1, 2, 3]}'
```

**全量迁移**：
```bash
curl -X POST "http://localhost:8080/api/v1/sync/subscription/migrate" \
  -H "Content-Type: application/json" \
  -H "Cookie: SESSIONID=your-session-id" \
  -d '{"ids": null}'
```

#### 回退接口（新表→旧表）

**批量回退指定订阅关系**：
```bash
curl -X POST "http://localhost:8080/api/v1/sync/subscription/rollback" \
  -H "Content-Type: application/json" \
  -H "Cookie: SESSIONID=your-session-id" \
  -d '{"ids": [1, 2, 3]}'
```

**全量回退**：
```bash
curl -X POST "http://localhost:8080/api/v1/sync/subscription/rollback" \
  -H "Content-Type: application/json" \
  -H "Cookie: SESSIONID=your-session-id" \
  -d '{"ids": null}'
```

#### 响应格式

```json
{
  "success": 10,
  "failed": 2,
  "details": [
    {"id": 1, "status": "success"},
    {"id": 5, "status": "failed", "error": "权限ID映射失败"}
  ]
}
```

### 4.6 同步逻辑说明

**订阅关系同步时的数据处理**：

1. **订阅关系数据同步**
   - 旧表：`openplatform_app_permission_t`
   - 新表：`openplatform_v2_subscription_t`

2. **审批记录数据同步（自动）**
   - 根据 `app_permission.id` 关联查询 `openplatform_eflow_t`（resource_id = app_permission.id）
   - 旧表：`openplatform_eflow_t`
   - 新表：`openplatform_v2_approval_record_t`

3. **审批日志数据同步（自动）**
   - 根据 `eflow_id` 关联查询 `openplatform_eflow_log_t`
   - 旧表：`openplatform_eflow_log_t`
   - 新表：`openplatform_v2_approval_log_t`

**同步流程**：
```
订阅关系ID → 查询审批记录 → 查询审批日志 → 批量同步
```



### 4.7 详细实现逻辑

#### 4.7.1 权限ID查找链路（不建映射表）

**迁移（旧→新）查找流程**：

```
旧订阅关系.permission_id
        ↓
旧权限表 (openplatform_permission_t)
    → 得到 resource_type, resource_id
        ↓
┌─ 如果是 API：
│   旧API表(resource_id) → path + method
│       ↓
│   新API表(openplatform_v2_api_t) → 匹配 path + method → 新API.id
│       ↓
│   新权限表(openplatform_v2_permission_t) 
│       WHERE resource_type='api' AND resource_id=新API.id
│       → 新权限ID
│
└─ 如果是事件：
    旧事件表(resource_id) → topic
        ↓
    新事件表(openplatform_v2_event_t) → 匹配 topic → 新事件.id
        ↓
    新权限表(openplatform_v2_permission_t)
        WHERE resource_type='event' AND resource_id=新事件.id
        → 新权限ID
```

**回退（新→旧）查找流程**：反向查找

---

#### 4.7.2 通道配置获取（仅事件订阅）

**数据来源**：`openplatform_app_p_t`（应用属性表）

**关联方式**：`parent_id` = 订阅关系的 `app_id`

**字段映射**：

| 新表字段 | 旧表字段 | 枚举值说明 |
|----------|----------|------------|
| channel_type | event_msg_recive_mode | 1=WebHook, 2=企业内部消息通道 |
| channel_address | event_push_url | 推送地址 |
| auth_type | event_push_auth_type | 固定值1（SOA） |

**SQL查询示例**：

```sql
SELECT 
    event_msg_recive_mode AS channel_type,
    event_push_url AS channel_address,
    event_push_auth_type AS auth_type
FROM openplatform_app_p_t
WHERE parent_id = #{app_id};
```

**重要说明**：
- 历史配置是**应用级别**的统一配置
- 迁移到新表时，**每个事件订阅都要写入通道配置**
- 回退时**不需要处理**（旧系统在应用属性表统一管理）

---

#### 4.7.3 combined_nodes 构造方法

**来源**：`openplatform_eflow_t.eflow_audit_user`（历史审批人字段）

**构造规则**：创建一个单节点审批配置

**JSON格式**：

```json
{
  "nodes": [
    {
      "level": "审批人",
      "approver": "历史审批人ID"
    }
  ]
}
```

**实现示例**：

```java
String constructCombinedNodes(String auditUser) {
    if (StringUtils.isEmpty(auditUser)) {
        return null;
    }
    JSONObject node = new JSONObject();
    node.put("level", "审批人");
    node.put("approver", auditUser);
    
    JSONArray nodes = new JSONArray();
    nodes.add(node);
    
    JSONObject combinedNodes = new JSONObject();
    combinedNodes.put("nodes", nodes);
    
    return combinedNodes.toJSONString();
}
```

---

#### 4.7.4 异常处理

**场景**：找不到对应的新API/事件

**处理方式**：

```
找不到对应新API/事件
    ↓
标记该订阅关系为失败
    ↓
记录失败原因："未找到对应的API/事件"
    ↓
跳过该条，继续处理下一条
    ↓
用户后续在新系统注册API/事件后
    ↓
重新执行同步接口（幂等性保证）
```

**代码示例**：

```java
try {
    Long newPermissionId = findNewPermissionId(oldPermission);
    // ... 同步逻辑
} catch (ResourceNotFoundException e) {
    SyncDetail detail = new SyncDetail();
    detail.setId(subscription.getId());
    detail.setStatus("failed");
    detail.setError("未找到对应的" + e.getResourceType() + "，请先在新系统注册");
    failedList.add(detail);
    continue;  // 继续处理下一条
}
```

---

#### 4.7.5 完整迁移逻辑（旧→新）

```
for each 订阅关系 in 旧表:
    
    1️⃣ 幂等性检查
    └─ if exists(订阅关系.id): 跳过
    
    2️⃣ 查找新权限ID
    ├─ 旧permission_id → 旧权限 → 确定resource_type
    ├─ API: resource_id → 旧API(path,method) → 新API → 新权限ID
    ├─ 事件: resource_id → 旧事件(topic) → 新事件 → 新权限ID
    └─ 找不到 → 记录失败，跳过
    
    3️⃣ 获取通道配置（仅事件订阅）
    └─ app_id → 应用属性表(parent_id=app_id) → channel_type, channel_address, auth_type
    
    4️⃣ 构造新订阅关系
    ├─ 如果是事件订阅：写入通道配置
    └─ 如果是API订阅：通道配置为空
    
    5️⃣ 写入新订阅关系表
    
    6️⃣ 同步审批记录（如存在，失败不影响订阅关系）
    ├─ resource_id = 订阅关系.id → 查询旧审批记录
    ├─ 构造 combined_nodes（用 eflow_audit_user）
    └─ 写入新审批记录表
    
    7️⃣ 同步审批日志（如存在，失败不影响订阅关系）
    └─ eflow_log.trace_id = eflow_id → 写入新审批日志表
```

---

#### 4.7.6 完整回退逻辑（新→旧）

```
for each 订阅关系 in 新表:
    
    1️⃣ 幂等性检查
    └─ if exists(订阅关系.id): 跳过
    
    2️⃣ 反向查找旧权限ID
    ├─ 新permission_id → 新权限 → 确定resource_type
    ├─ API: resource_id → 新API(path,method) → 旧API → 旧权限ID
    └─ 事件: resource_id → 新事件(topic) → 旧事件 → 旧权限ID
    
    3️⃣ 构造旧订阅关系
    └─ 通道配置不处理（旧系统在应用属性表统一管理）
    
    4️⃣ 写入旧订阅关系表
    
    5️⃣ 同步审批记录（如存在）
    └─ 从新审批记录 → 旧审批记录
    
    6️⃣ 同步审批日志（如存在）
    └─ 从新审批日志 → 旧审批日志
```

---
