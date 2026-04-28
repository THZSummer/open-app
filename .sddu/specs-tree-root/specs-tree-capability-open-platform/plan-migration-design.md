# API驱动的数据迁移方案设计文档

> 本文档定义基于REST API控制的在线数据迁移方案，实现读写分离、渐进式迁移。

---

## 1. 迁移方案概述

### 1.1 设计原则

| 原则 | 说明 |
|------|------|
| **在线迁移** | 迁移过程中业务不停机，v1和v2并行运行 |
| **读写分离** | 旧代码读写v1表，新代码读写v2表，迁移API负责同步 |
| **渐进式迁移** | 支持单条、批量、全量三种粒度，可控制进度 |
| **手动触发** | 通过API调用触发迁移，运维可控 |
| **仅记录日志** | 不维护复杂状态，仅记录迁移操作日志 |

### 1.2 迁移方式：API驱动

```
┌──────────────────────────────────────────────────────────────────────────┐
│                          API驱动迁移架构                                   │
├──────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│   ┌─────────────┐        ┌─────────────────────┐                       │
│   │  旧版代码    │        │    新版代码           │                       │
│   │  (v1)       │        │    (v2)              │                       │
│   └──────┬──────┘        └──────────┬──────────┘                       │
│          │                          │                                   │
│          ▼                          ▼                                   │
│   ┌─────────────┐        ┌─────────────────────┐                       │
│   │  v1表        │        │    v2表              │                       │
│   │  (旧表)      │        │    (新表)            │                       │
│   └──────┬──────┘        └──────────┬──────────┘                       │
│          │                          ▲                                   │
│          │                          │                                   │
│          └──────────────────────────┘                                   │
│                     迁移API                                             │
│              (数据同步转换)                                              │
│                                                                          │
└──────────────────────────────────────────────────────────────────────────┘
```

### 1.3 核心流程（5个阶段）

```
阶段1：分类模块迁移（无依赖）
  ├── 单条迁移API
  ├── 批量迁移API
  ├── 全量迁移API
  └── 验证API
           ↓
阶段2：资源模块迁移（依赖分类）
  ├── API资源迁移
  ├── 事件资源迁移
  └── 回调资源初始化
           ↓
阶段3：权限模块迁移（依赖资源）
  ├── API权限迁移（核心：API与权限解耦）
  ├── 事件权限迁移
  └── 权限属性迁移
           ↓
阶段4：审批模块迁移（依赖权限）
  ├── 审批模板初始化
  ├── 审批记录迁移
  └── 审批日志迁移
           ↓
阶段5：业务关联迁移
  ├── 订阅关系迁移（核心：权限ID映射）
  └── 用户授权迁移
```

### 1.4 读写分离策略

```
旧版本代码（v1）:
├── CategoryV1Repository → openplatform_module_node_t
├── PermissionV1Repository → openplatform_permission_t
├── ApiV1Repository → openplatform_permission_api_t
├── EventV1Repository → openplatform_event_t
└── SubscriptionV1Repository → openplatform_app_permission_t

新版本代码（v2）:
├── CategoryV2Repository → openplatform_v2_category_t
├── PermissionV2Repository → openplatform_v2_permission_t
├── ApiV2Repository → openplatform_v2_api_t
├── EventV2Repository → openplatform_v2_event_t
└── SubscriptionV2Repository → openplatform_v2_subscription_t

迁移API:
├── 从v1表读取数据
├── 转换为v2格式
├── 写入v2表
└── 记录迁移日志
```

---

## 2. 迁移API设计

### 2.1 通用API规范

#### 2.1.1 基础路径

```
/api/v1/migration
```

#### 2.1.2 通用响应格式

```java
public class ApiResponse<T> {
    private int code;
    private String message;
    private T data;
    private long timestamp;
}

public class MigrationResult {
    private Long sourceId;
    private Long targetId;
    private boolean success;
    private String errorMessage;
    private String migrationTime;
}

public class BatchMigrationResult {
    private int totalCount;
    private int successCount;
    private int failureCount;
    private List<MigrationResult> details;
}

public class ValidationResult {
    private Long sourceId;
    private Long targetId;
    private boolean valid;
    private List<String> errors;
}

public class BatchValidationResult {
    private int totalCount;
    private int validCount;
    private int invalidCount;
    private List<ValidationResult> details;
}
```

#### 2.1.3 错误码定义

| 错误码 | 说明 |
|--------|------|
| 0 | 成功 |
| 1001 | 源数据不存在 |
| 1002 | 目标数据已存在 |
| 1003 | 数据转换失败 |
| 1004 | 关联数据缺失 |
| 1005 | 迁移执行异常 |
| 2001 | 验证失败-数据不一致 |
| 2002 | 验证失败-字段缺失 |
| 2003 | 验证失败-关联异常 |
| 9999 | 系统异常 |

### 2.2 分类模块迁移API

#### 2.2.1 单条迁移

```
POST /api/v1/migration/category/{id}
```

**请求示例**：
```bash
curl -X POST "http://localhost:8080/api/v1/migration/category/123"
```

**响应示例**：
```json
{
    "code": 0,
    "message": "success",
    "data": {
        "sourceId": 123,
        "targetId": 123,
        "success": true,
        "errorMessage": null,
        "migrationTime": "2024-04-28T10:30:00.000Z"
    },
    "timestamp": 1714295400000
}
```

#### 2.2.2 批量迁移

```
POST /api/v1/migration/category/batch
```

**请求示例**：
```json
{
    "ids": [1, 2, 3, 4, 5]
}
```

**响应示例**：
```json
{
    "code": 0,
    "message": "success",
    "data": {
        "totalCount": 5,
        "successCount": 5,
        "failureCount": 0,
        "details": [
            {
                "sourceId": 1,
                "targetId": 1,
                "success": true,
                "errorMessage": null,
                "migrationTime": "2024-04-28T10:30:01.000Z"
            }
        ]
    },
    "timestamp": 1714295401000
}
```

#### 2.2.3 全量迁移

```
POST /api/v1/migration/category/all
```

**请求示例**：
```bash
curl -X POST "http://localhost:8080/api/v1/migration/category/all"
```

**响应示例**：
```json
{
    "code": 0,
    "message": "Migration task started",
    "data": {
        "taskId": "migration_category_20240428_103000",
        "totalCount": 100,
        "successCount": 100,
        "failureCount": 0,
        "details": null
    },
    "timestamp": 1714295400000
}
```

#### 2.2.4 单条验证

```
POST /api/v1/migration/category/{id}/validate
```

#### 2.2.5 批量验证

```
POST /api/v1/migration/category/batch/validate
```

**请求示例**：
```json
{
    "ids": [1, 2, 3]
}
```

#### 2.2.6 全量验证

```
POST /api/v1/migration/category/all/validate
```

### 2.3 API资源迁移API

```
POST /api/v1/migration/api/{id}              # 单条迁移
POST /api/v1/migration/api/batch             # 批量迁移
POST /api/v1/migration/api/all               # 全量迁移
POST /api/v1/migration/api/{id}/validate     # 单条验证
POST /api/v1/migration/api/batch/validate    # 批量验证
POST /api/v1/migration/api/all/validate      # 全量验证
```

### 2.4 事件资源迁移API

```
POST /api/v1/migration/event/{id}              # 单条迁移
POST /api/v1/migration/event/batch             # 批量迁移
POST /api/v1/migration/event/all               # 全量迁移
POST /api/v1/migration/event/{id}/validate     # 单条验证
POST /api/v1/migration/event/batch/validate    # 批量验证
POST /api/v1/migration/event/all/validate      # 全量验证
```

### 2.5 权限资源迁移API

```
POST /api/v1/migration/permission/{id}              # 单条迁移
POST /api/v1/migration/permission/batch             # 批量迁移
POST /api/v1/migration/permission/all               # 全量迁移
POST /api/v1/migration/permission/{id}/validate     # 单条验证
POST /api/v1/migration/permission/batch/validate    # 批量验证
POST /api/v1/migration/permission/all/validate      # 全量验证
POST /api/v1/migration/permission/build-mapping     # 构建权限ID映射表
```

### 2.6 审批流程迁移API

```
POST /api/v1/migration/approval/{id}              # 单条迁移
POST /api/v1/migration/approval/batch             # 批量迁移
POST /api/v1/migration/approval/all               # 全量迁移
POST /api/v1/migration/approval/{id}/validate     # 单条验证
POST /api/v1/migration/approval/batch/validate    # 批量验证
POST /api/v1/migration/approval/all/validate      # 全量验证
POST /api/v1/migration/approval/init-templates    # 初始化审批模板
```

### 2.7 订阅关系迁移API

```
POST /api/v1/migration/subscription/{id}              # 单条迁移
POST /api/v1/migration/subscription/batch             # 批量迁移
POST /api/v1/migration/subscription/all               # 全量迁移
POST /api/v1/migration/subscription/{id}/validate     # 单条验证
POST /api/v1/migration/subscription/batch/validate    # 批量验证
POST /api/v1/migration/subscription/all/validate      # 全量验证
```

### 2.8 迁移状态查询API

```
GET /api/v1/migration/status                    # 查询所有模块迁移状态
GET /api/v1/migration/status/{module}           # 查询指定模块迁移状态
GET /api/v1/migration/logs                      # 查询迁移日志
GET /api/v1/migration/logs/{module}             # 查询指定模块迁移日志
```

**响应示例**：
```json
{
    "code": 0,
    "message": "success",
    "data": {
        "category": {
            "sourceCount": 100,
            "targetCount": 100,
            "migratedCount": 100,
            "lastMigrationTime": "2024-04-28T10:30:00.000Z"
        },
        "api": {
            "sourceCount": 50,
            "targetCount": 50,
            "migratedCount": 50,
            "lastMigrationTime": "2024-04-28T10:35:00.000Z"
        }
    },
    "timestamp": 1714295400000
}
```

---

## 3. 数据模型设计

### 3.1 迁移日志表

```sql
CREATE TABLE `openplatform_migration_log_t` (
    `id` BIGINT(20) PRIMARY KEY COMMENT '主键ID',
    `module` VARCHAR(50) NOT NULL COMMENT '模块：category/api/event/permission/approval/subscription',
    `migration_type` VARCHAR(20) NOT NULL COMMENT '迁移类型：single/batch/all',
    `source_id` BIGINT(20) COMMENT '源数据ID',
    `target_id` BIGINT(20) COMMENT '目标数据ID',
    `status` TINYINT(10) COMMENT '状态：0=失败, 1=成功, 2=部分成功',
    `error_message` TEXT COMMENT '错误信息',
    `migration_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '迁移时间',
    `create_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    `last_update_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    KEY `idx_module` (`module`),
    KEY `idx_source_id` (`source_id`),
    KEY `idx_target_id` (`target_id`),
    KEY `idx_status` (`status`),
    KEY `idx_migration_time` (`migration_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据迁移日志表';
```

### 3.2 权限ID映射表

```sql
CREATE TABLE `openplatform_migration_permission_mapping_t` (
    `id` BIGINT(20) PRIMARY KEY COMMENT '主键ID',
    `old_permission_id` BIGINT(20) NOT NULL COMMENT '旧权限ID',
    `new_permission_id` BIGINT(20) NOT NULL COMMENT '新权限ID',
    `resource_type` VARCHAR(20) NOT NULL COMMENT '资源类型：api/event/callback',
    `resource_id` BIGINT(20) NOT NULL COMMENT '资源ID',
    `old_scope` VARCHAR(200) COMMENT '旧权限标识',
    `new_scope` VARCHAR(200) COMMENT '新权限标识',
    `create_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    UNIQUE KEY `uk_old_permission_id` (`old_permission_id`),
    KEY `idx_new_permission_id` (`new_permission_id`),
    KEY `idx_resource` (`resource_type`, `resource_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限ID映射表';
```

### 3.3 迁移统计表

```sql
CREATE TABLE `openplatform_migration_summary_t` (
    `id` BIGINT(20) PRIMARY KEY COMMENT '主键ID',
    `module` VARCHAR(50) NOT NULL COMMENT '模块名称',
    `source_table` VARCHAR(100) NOT NULL COMMENT '源表名',
    `target_table` VARCHAR(100) NOT NULL COMMENT '目标表名',
    `source_count` INT NOT NULL DEFAULT 0 COMMENT '源表数据量',
    `target_count` INT NOT NULL DEFAULT 0 COMMENT '目标表数据量',
    `migrated_count` INT NOT NULL DEFAULT 0 COMMENT '已迁移数量',
    `validated_count` INT NOT NULL DEFAULT 0 COMMENT '已验证数量',
    `last_migration_time` DATETIME(3) COMMENT '最后迁移时间',
    `last_validation_time` DATETIME(3) COMMENT '最后验证时间',
    `status` TINYINT(10) DEFAULT 0 COMMENT '状态：0=未开始, 1=进行中, 2=已完成, 3=已验证',
    `create_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    `last_update_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    UNIQUE KEY `uk_module` (`module`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='迁移统计表';
```

---

## 4. 各模块迁移逻辑

### 4.1 分类模块迁移逻辑

#### 4.1.1 单条迁移逻辑

```java
public MigrationResult migrateCategory(Long sourceId) {
    MigrationResult result = new MigrationResult();
    result.setSourceId(sourceId);
    
    try {
        // 1. 从v1表读取数据
        CategoryV1 categoryV1 = categoryV1Repository.findById(sourceId);
        if (categoryV1 == null) {
            result.setSuccess(false);
            result.setErrorMessage("Source category not found: " + sourceId);
            logMigration("category", "single", sourceId, null, 0, "Source not found");
            return result;
        }
        
        // 2. 检查目标表是否已存在
        if (categoryV2Repository.existsById(sourceId)) {
            result.setSuccess(true);
            result.setTargetId(sourceId);
            result.setErrorMessage("Target already exists, skipped");
            return result;
        }
        
        // 3. 转换为v2格式
        CategoryV2 categoryV2 = convertCategory(categoryV1);
        
        // 4. 填充新增字段
        if (categoryV2.getParentId() == null || categoryV2.getParentId() == 0) {
            // 根分类生成 category_alias
            categoryV2.setCategoryAlias(generateCategoryAlias(categoryV1));
        }
        
        // 5. 计算 path
        categoryV2.setPath(calculateCategoryPath(categoryV2));
        
        // 6. 写入v2表
        categoryV2Repository.save(categoryV2);
        
        // 7. 记录日志
        result.setSuccess(true);
        result.setTargetId(sourceId);
        logMigration("category", "single", sourceId, sourceId, 1, null);
        
    } catch (Exception e) {
        result.setSuccess(false);
        result.setErrorMessage(e.getMessage());
        logMigration("category", "single", sourceId, null, 0, e.getMessage());
    }
    
    return result;
}
```

#### 4.1.2 字段映射规则

| 源字段 | 目标字段 | 转换规则 |
|--------|----------|----------|
| `id` | `id` | 直接映射 |
| `node_name_cn` | `name_cn` | 直接映射 |
| `node_name_en` | `name_en` | 直接映射 |
| `parent_Node_id` | `parent_id` | 修正大小写 |
| `order_num` | `sort_order` | 字段重命名 |
| `status` | `status` | 直接映射 |
| - | `category_alias` | **新增**：仅根分类填充 |
| - | `path` | **新增**：计算路径 |

#### 4.1.3 批量迁移逻辑

```java
public BatchMigrationResult migrateCategories(List<Long> ids) {
    BatchMigrationResult result = new BatchMigrationResult();
    result.setTotalCount(ids.size());
    
    List<MigrationResult> details = new ArrayList<>();
    int successCount = 0;
    int failureCount = 0;
    
    for (Long id : ids) {
        MigrationResult singleResult = migrateCategory(id);
        details.add(singleResult);
        
        if (singleResult.isSuccess()) {
            successCount++;
        } else {
            failureCount++;
        }
    }
    
    result.setSuccessCount(successCount);
    result.setFailureCount(failureCount);
    result.setDetails(details);
    
    return result;
}
```

#### 4.1.4 全量迁移逻辑

```java
@Transactional
public BatchMigrationResult migrateAllCategories() {
    // 1. 获取所有分类ID
    List<Long> allIds = categoryV1Repository.findAllIds();
    
    // 2. 分批处理
    int batchSize = 100;
    BatchMigrationResult result = new BatchMigrationResult();
    result.setTotalCount(allIds.size());
    
    int successCount = 0;
    int failureCount = 0;
    List<MigrationResult> details = new ArrayList<>();
    
    for (int i = 0; i < allIds.size(); i += batchSize) {
        List<Long> batch = allIds.subList(i, Math.min(i + batchSize, allIds.size()));
        
        for (Long id : batch) {
            MigrationResult singleResult = migrateCategory(id);
            details.add(singleResult);
            
            if (singleResult.isSuccess()) {
                successCount++;
            } else {
                failureCount++;
            }
        }
        
        // 更新迁移统计
        updateMigrationSummary("category", successCount);
    }
    
    result.setSuccessCount(successCount);
    result.setFailureCount(failureCount);
    result.setDetails(details);
    
    return result;
}
```

#### 4.1.5 验证逻辑

```java
public ValidationResult validateCategory(Long sourceId) {
    ValidationResult result = new ValidationResult();
    result.setSourceId(sourceId);
    
    List<String> errors = new ArrayList<>();
    
    try {
        // 1. 检查源数据
        CategoryV1 v1 = categoryV1Repository.findById(sourceId);
        if (v1 == null) {
            errors.add("Source category not found");
            result.setValid(false);
            result.setErrors(errors);
            return result;
        }
        
        // 2. 检查目标数据
        CategoryV2 v2 = categoryV2Repository.findById(sourceId);
        if (v2 == null) {
            errors.add("Target category not found");
            result.setValid(false);
            result.setErrors(errors);
            return result;
        }
        
        result.setTargetId(sourceId);
        
        // 3. 字段一致性验证
        if (!Objects.equals(v1.getNodeNameCn(), v2.getNameCn())) {
            errors.add("name_cn mismatch: " + v1.getNodeNameCn() + " vs " + v2.getNameCn());
        }
        
        if (!Objects.equals(v1.getNodeNameEn(), v2.getNameEn())) {
            errors.add("name_en mismatch");
        }
        
        if (!Objects.equals(normalizeParentId(v1.getParentNodeId()), v2.getParentId())) {
            errors.add("parent_id mismatch");
        }
        
        // 4. 新字段验证
        if (v2.getParentId() == null || v2.getParentId() == 0) {
            if (v2.getCategoryAlias() == null || v2.getCategoryAlias().isEmpty()) {
                errors.add("Root category missing category_alias");
            }
        }
        
        if (v2.getPath() == null || v2.getPath().isEmpty()) {
            errors.add("path is null or empty");
        }
        
        // 5. 设置验证结果
        result.setValid(errors.isEmpty());
        result.setErrors(errors);
        
    } catch (Exception e) {
        errors.add("Validation exception: " + e.getMessage());
        result.setValid(false);
        result.setErrors(errors);
    }
    
    return result;
}
```

### 4.2 API资源迁移逻辑

#### 4.2.1 单条迁移逻辑

```java
public MigrationResult migrateApi(Long sourceId) {
    MigrationResult result = new MigrationResult();
    result.setSourceId(sourceId);
    
    try {
        // 1. 从v1表读取数据
        ApiV1 apiV1 = apiV1Repository.findById(sourceId);
        if (apiV1 == null) {
            result.setSuccess(false);
            result.setErrorMessage("Source API not found: " + sourceId);
            logMigration("api", "single", sourceId, null, 0, "Source not found");
            return result;
        }
        
        // 2. 检查目标表是否已存在
        if (apiV2Repository.existsById(sourceId)) {
            result.setSuccess(true);
            result.setTargetId(sourceId);
            return result;
        }
        
        // 3. 转换为v2格式
        ApiV2 apiV2 = new ApiV2();
        apiV2.setId(apiV1.getId());
        apiV2.setNameCn(apiV1.getApiNameCn());
        apiV2.setNameEn(apiV1.getApiNameEn());
        apiV2.setPath(apiV1.getApiPath());
        apiV2.setMethod(apiV1.getApiMethod());
        apiV2.setAuthType(apiV1.getAuthType());
        
        // 4. 状态映射
        apiV2.setStatus(convertApiStatus(apiV1.getStatus()));
        
        // 5. 保存主表
        apiV2Repository.save(apiV2);
        
        // 6. 迁移属性表
        migrateApiProperties(sourceId);
        
        result.setSuccess(true);
        result.setTargetId(sourceId);
        logMigration("api", "single", sourceId, sourceId, 1, null);
        
    } catch (Exception e) {
        result.setSuccess(false);
        result.setErrorMessage(e.getMessage());
        logMigration("api", "single", sourceId, null, 0, e.getMessage());
    }
    
    return result;
}
```

#### 4.2.2 状态映射规则

```java
private Integer convertApiStatus(Integer oldStatus) {
    if (oldStatus == null) {
        return 2; // 默认已发布
    }
    switch (oldStatus) {
        case 1: return 2; // 启用 → 已发布
        case 0: return 3; // 禁用 → 已下线
        default: return 2;
    }
}
```

### 4.3 事件资源迁移逻辑

```java
public MigrationResult migrateEvent(Long sourceId) {
    MigrationResult result = new MigrationResult();
    result.setSourceId(sourceId);
    
    try {
        EventV1 eventV1 = eventV1Repository.findById(sourceId);
        if (eventV1 == null) {
            result.setSuccess(false);
            result.setErrorMessage("Source event not found");
            return result;
        }
        
        EventV2 eventV2 = new EventV2();
        eventV2.setId(eventV1.getId());
        eventV2.setNameCn(eventV1.getEventNameCn());
        eventV2.setNameEn(eventV1.getEventNameEn());
        eventV2.setTopic(eventV1.getTopic());
        eventV2.setStatus(convertStatus(eventV1.getStatus()));
        
        eventV2Repository.save(eventV2);
        
        migrateEventProperties(sourceId);
        
        result.setSuccess(true);
        result.setTargetId(sourceId);
        logMigration("event", "single", sourceId, sourceId, 1, null);
        
    } catch (Exception e) {
        result.setSuccess(false);
        result.setErrorMessage(e.getMessage());
        logMigration("event", "single", sourceId, null, 0, e.getMessage());
    }
    
    return result;
}
```

### 4.4 权限资源迁移逻辑（重点：API与权限解耦）

#### 4.4.1 核心迁移逻辑

```java
public MigrationResult migratePermission(Long sourceId) {
    MigrationResult result = new MigrationResult();
    result.setSourceId(sourceId);
    
    try {
        PermissionV1 permV1 = permissionV1Repository.findById(sourceId);
        if (permV1 == null) {
            result.setSuccess(false);
            result.setErrorMessage("Source permission not found");
            return result;
        }
        
        // 根据权限类型分别处理
        String permissionType = permV1.getPermisssionType(); // 注意拼写错误
        
        if ("api".equalsIgnoreCase(permissionType)) {
            return migrateApiPermission(permV1);
        } else if ("event".equalsIgnoreCase(permissionType)) {
            return migrateEventPermission(permV1);
        } else {
            result.setSuccess(false);
            result.setErrorMessage("Unknown permission type: " + permissionType);
            return result;
        }
        
    } catch (Exception e) {
        result.setSuccess(false);
        result.setErrorMessage(e.getMessage());
        logMigration("permission", "single", sourceId, null, 0, e.getMessage());
    }
    
    return result;
}
```

#### 4.4.2 API权限迁移

```java
private MigrationResult migrateApiPermission(PermissionV1 permV1) {
    MigrationResult result = new MigrationResult();
    
    // 1. 获取关联的API列表
    List<ApiV1> apis = apiV1Repository.findByPermissionId(permV1.getId());
    
    if (apis.isEmpty()) {
        result.setSuccess(false);
        result.setErrorMessage("No API associated with permission: " + permV1.getId());
        return result;
    }
    
    List<MigrationResult> details = new ArrayList<>();
    
    // 2. 为每个API创建权限记录
    for (ApiV1 api : apis) {
        try {
            PermissionV2 permV2 = new PermissionV2();
            permV2.setId(generateNewPermissionId(permV1.getId(), api.getId()));
            permV2.setNameCn(api.getApiNameCn());
            permV2.setNameEn(api.getApiNameEn());
            permV2.setScope(generateApiScope(api));
            permV2.setResourceType("api");
            permV2.setResourceId(api.getId());
            permV2.setCategoryId(permV1.getModuleId());
            permV2.setNeedApproval(permV1.getIsApprovalRequired());
            permV2.setStatus(permV1.getStatus());
            
            // 转换审批节点
            permV2.setResourceNodes(convertApprovalNodes(permV1.getId()));
            
            permissionV2Repository.save(permV2);
            
            // 保存映射关系
            savePermissionMapping(permV1.getId(), permV2.getId(), "api", api.getId());
            
            details.add(new MigrationResult(api.getId(), permV2.getId(), true, null, null));
            
        } catch (Exception e) {
            details.add(new MigrationResult(api.getId(), null, false, e.getMessage(), null));
        }
    }
    
    result.setSuccess(details.stream().allMatch(MigrationResult::isSuccess));
    result.setErrorMessage(details.stream()
        .filter(d -> !d.isSuccess())
        .map(MigrationResult::getErrorMessage)
        .collect(Collectors.joining("; ")));
    
    return result;
}
```

#### 4.4.3 Scope生成规则

```java
private String generateApiScope(ApiV1 api) {
    // 格式: api:{path}:{method}
    String normalizedPath = api.getApiPath()
        .replaceAll("^/+", "")
        .replaceAll("/+$", "")
        .replace("/", ":");
    return String.format("api:%s:%s", normalizedPath, api.getApiMethod().toLowerCase());
}

private String generateEventScope(EventV1 event) {
    // 格式: event:{topic}
    return String.format("event:%s", event.getTopic().replace(".", ":"));
}
```

#### 4.4.4 审批节点转换

```java
private String convertApprovalNodes(Long permissionId) {
    List<PermissionPropertyV1> properties = permissionPropertyV1Repository
        .findByParentIdAndPropertyName(permissionId, "audit_user");
    
    if (properties.isEmpty()) {
        return null;
    }
    
    List<Map<String, Object>> nodes = new ArrayList<>();
    int order = 1;
    
    for (PermissionPropertyV1 prop : properties) {
        Map<String, Object> node = new HashMap<>();
        node.put("type", "approver");
        node.put("userId", prop.getPropertyValue());
        node.put("userName", prop.getPropertyValue()); // 实际应关联用户服务
        node.put("order", order++);
        nodes.add(node);
    }
    
    return JsonUtils.toJson(nodes);
}
```

### 4.5 审批流程迁移逻辑（重点：模板与记录分离）

#### 4.5.1 初始化审批模板

```java
public void initApprovalTemplates() {
    List<ApprovalFlowTemplate> templates = Arrays.asList(
        ApprovalFlowTemplate.builder()
            .id(1L)
            .nameCn("API注册审批流程")
            .nameEn("API Register Approval")
            .code("api_register")
            .nodes("[{\"type\":\"approver\",\"userId\":\"api_admin\",\"order\":1}]")
            .status(1)
            .build(),
        ApprovalFlowTemplate.builder()
            .id(2L)
            .nameCn("事件注册审批流程")
            .nameEn("Event Register Approval")
            .code("event_register")
            .nodes("[{\"type\":\"approver\",\"userId\":\"event_admin\",\"order\":1}]")
            .status(1)
            .build(),
        ApprovalFlowTemplate.builder()
            .id(3L)
            .nameCn("API权限申请审批流程")
            .nameEn("API Permission Apply Approval")
            .code("api_permission_apply")
            .nodes("[{\"type\":\"approver\",\"userId\":\"permission_admin\",\"order\":1}]")
            .status(1)
            .build(),
        ApprovalFlowTemplate.builder()
            .id(4L)
            .nameCn("事件权限申请审批流程")
            .nameEn("Event Permission Apply Approval")
            .code("event_permission_apply")
            .nodes("[{\"type\":\"approver\",\"userId\":\"permission_admin\",\"order\":1}]")
            .status(1)
            .build()
    );
    
    approvalFlowTemplateRepository.saveAll(templates);
    logMigration("approval", "init", null, null, 1, "Templates initialized");
}
```

#### 4.5.2 审批记录迁移

```java
public MigrationResult migrateApprovalRecord(Long sourceId) {
    MigrationResult result = new MigrationResult();
    result.setSourceId(sourceId);
    
    try {
        EflowV1 eflow = eflowV1Repository.findById(sourceId);
        if (eflow == null) {
            result.setSuccess(false);
            result.setErrorMessage("Source eflow not found");
            return result;
        }
        
        ApprovalRecordV2 record = new ApprovalRecordV2();
        record.setId(eflow.getEflowId());
        record.setBusinessType(convertBusinessType(eflow.getEflowType()));
        record.setBusinessId(eflow.getResourceId());
        record.setApplicantId(eflow.getEflowSubmitUser());
        record.setApplicantName(eflow.getEflowSubmitUser());
        record.setStatus(eflow.getEflowStatus());
        
        // 转换组合节点
        record.setCombinedNodes(convertCombinedNodes(eflow.getEflowAuditUser()));
        
        // 计算当前节点
        record.setCurrentNode(calculateCurrentNode(eflow));
        
        // 设置完成时间
        if (eflow.getEflowStatus() != 0) {
            record.setCompletedAt(eflow.getLastUpdateTime());
        }
        
        approvalRecordV2Repository.save(record);
        
        // 迁移审批日志
        migrateApprovalLogs(sourceId, record.getId());
        
        result.setSuccess(true);
        result.setTargetId(sourceId);
        logMigration("approval", "single", sourceId, sourceId, 1, null);
        
    } catch (Exception e) {
        result.setSuccess(false);
        result.setErrorMessage(e.getMessage());
        logMigration("approval", "single", sourceId, null, 0, e.getMessage());
    }
    
    return result;
}

private String convertBusinessType(String eflowType) {
    Map<String, String> typeMapping = new HashMap<>();
    typeMapping.put("api_register", "api_register");
    typeMapping.put("event_register", "event_register");
    typeMapping.put("api_permission", "api_permission_apply");
    typeMapping.put("event_permission", "event_permission_apply");
    return typeMapping.getOrDefault(eflowType, "api_permission_apply");
}

private String convertCombinedNodes(String auditUser) {
    List<Map<String, Object>> nodes = new ArrayList<>();
    Map<String, Object> node = new HashMap<>();
    node.put("type", "approver");
    node.put("userId", auditUser);
    node.put("userName", auditUser);
    node.put("order", 1);
    nodes.add(node);
    return JsonUtils.toJson(nodes);
}
```

### 4.6 订阅关系迁移逻辑（重点：权限ID映射）

#### 4.6.1 核心迁移逻辑

```java
public MigrationResult migrateSubscription(Long sourceId) {
    MigrationResult result = new MigrationResult();
    result.setSourceId(sourceId);
    
    try {
        SubscriptionV1 subV1 = subscriptionV1Repository.findById(sourceId);
        if (subV1 == null) {
            result.setSuccess(false);
            result.setErrorMessage("Source subscription not found");
            return result;
        }
        
        // 1. 查询权限ID映射
        PermissionMapping mapping = permissionMappingRepository
            .findByOldPermissionId(subV1.getPermissionId());
        
        if (mapping == null) {
            result.setSuccess(false);
            result.setErrorMessage("Permission mapping not found for: " + subV1.getPermissionId());
            return result;
        }
        
        // 2. 转换订阅数据
        SubscriptionV2 subV2 = new SubscriptionV2();
        subV2.setId(subV1.getId());
        subV2.setAppId(subV1.getAppId());
        subV2.setPermissionId(mapping.getNewPermissionId()); // 使用新权限ID
        subV2.setStatus(subV1.getStatus());
        subV2.setChannelType(subV1.getChannelType());
        subV2.setChannelAddress(subV1.getChannelAddress());
        subV2.setAuthType(subV1.getAuthType());
        
        // 3. 填充审批信息
        fillApprovalInfo(subV2, sourceId);
        
        subscriptionV2Repository.save(subV2);
        
        result.setSuccess(true);
        result.setTargetId(sourceId);
        logMigration("subscription", "single", sourceId, sourceId, 1, null);
        
    } catch (Exception e) {
        result.setSuccess(false);
        result.setErrorMessage(e.getMessage());
        logMigration("subscription", "single", sourceId, null, 0, e.getMessage());
    }
    
    return result;
}
```

#### 4.6.2 构建权限ID映射

```java
@Transactional
public void buildPermissionMapping() {
    // 1. 清空旧映射
    permissionMappingRepository.deleteAll();
    
    // 2. 构建API权限映射
    List<PermissionV2> apiPermissions = permissionV2Repository
        .findByResourceType("api");
    
    for (PermissionV2 perm : apiPermissions) {
        ApiV1 apiV1 = apiV1Repository.findById(perm.getResourceId());
        if (apiV1 != null && apiV1.getPermissionId() != null) {
            PermissionMapping mapping = new PermissionMapping();
            mapping.setOldPermissionId(apiV1.getPermissionId());
            mapping.setNewPermissionId(perm.getId());
            mapping.setResourceType("api");
            mapping.setResourceId(perm.getResourceId());
            mapping.setNewScope(perm.getScope());
            permissionMappingRepository.save(mapping);
        }
    }
    
    // 3. 构建事件权限映射
    List<PermissionV2> eventPermissions = permissionV2Repository
        .findByResourceType("event");
    
    for (PermissionV2 perm : eventPermissions) {
        EventV1 eventV1 = eventV1Repository.findById(perm.getResourceId());
        if (eventV1 != null) {
            PermissionMapping mapping = new PermissionMapping();
            mapping.setOldPermissionId(eventV1.getId()); // 事件权限可能没有单独的permission_id
            mapping.setNewPermissionId(perm.getId());
            mapping.setResourceType("event");
            mapping.setResourceId(perm.getResourceId());
            mapping.setNewScope(perm.getScope());
            permissionMappingRepository.save(mapping);
        }
    }
    
    logMigration("permission", "mapping", null, null, 1, "Mapping built");
}
```

---

## 5. 迁移服务实现指南

### 5.1 服务层设计

```java
public interface MigrationService {
    
    // 分类模块
    MigrationResult migrateCategory(Long id);
    BatchMigrationResult migrateCategories(List<Long> ids);
    BatchMigrationResult migrateAllCategories();
    ValidationResult validateCategory(Long id);
    BatchValidationResult validateCategories(List<Long> ids);
    BatchValidationResult validateAllCategories();
    
    // API资源模块
    MigrationResult migrateApi(Long id);
    BatchMigrationResult migrateApis(List<Long> ids);
    BatchMigrationResult migrateAllApis();
    ValidationResult validateApi(Long id);
    BatchValidationResult validateApis(List<Long> ids);
    BatchValidationResult validateAllApis();
    
    // ... 其他模块类似
}
```

### 5.2 Repository层设计

```java
// V1版本Repository
public interface CategoryV1Repository {
    CategoryV1 findById(Long id);
    List<Long> findAllIds();
    int count();
}

public interface CategoryV2Repository {
    CategoryV2 findById(Long id);
    void save(CategoryV2 entity);
    boolean existsById(Long id);
    int count();
}

// 迁移相关Repository
public interface MigrationLogRepository {
    void save(MigrationLog log);
    List<MigrationLog> findByModule(String module);
    List<MigrationLog> findByModuleAndStatus(String module, int status);
}

public interface PermissionMappingRepository {
    void save(PermissionMapping mapping);
    PermissionMapping findByOldPermissionId(Long oldPermissionId);
    void deleteAll();
}
```

### 5.3 事务处理

```java
@Service
public class MigrationServiceImpl implements MigrationService {
    
    @Transactional
    public MigrationResult migrateCategory(Long id) {
        // 整个迁移操作在一个事务中
        // 如果失败，自动回滚
    }
    
    @Transactional
    public BatchMigrationResult migrateAllCategories() {
        // 全量迁移使用事务
        // 注意：大批量数据可能导致事务超时
        // 建议分批处理
    }
    
    // 对于大批量数据，建议使用编程式事务
    @Autowired
    private PlatformTransactionManager transactionManager;
    
    public void migrateInBatches(List<Long> ids) {
        TransactionStatus status = transactionManager.getTransaction(new DefaultTransactionDefinition());
        
        try {
            for (Long id : ids) {
                migrateCategory(id);
            }
            transactionManager.commit(status);
        } catch (Exception e) {
            transactionManager.rollback(status);
            throw e;
        }
    }
}
```

### 5.4 异常处理

```java
@Service
public class MigrationServiceImpl implements MigrationService {
    
    private static final Logger logger = LoggerFactory.getLogger(MigrationServiceImpl.class);
    
    public MigrationResult migrateCategory(Long id) {
        MigrationResult result = new MigrationResult();
        result.setSourceId(id);
        
        try {
            // 迁移逻辑
            
        } catch (DataAccessException e) {
            logger.error("Database error during migration: id={}", id, e);
            result.setSuccess(false);
            result.setErrorMessage("Database error: " + e.getMessage());
            logMigration("category", "single", id, null, 0, e.getMessage());
            
        } catch (JsonProcessingException e) {
            logger.error("JSON conversion error during migration: id={}", id, e);
            result.setSuccess(false);
            result.setErrorMessage("JSON conversion error: " + e.getMessage());
            logMigration("category", "single", id, null, 0, e.getMessage());
            
        } catch (Exception e) {
            logger.error("Unexpected error during migration: id={}", id, e);
            result.setSuccess(false);
            result.setErrorMessage("Unexpected error: " + e.getMessage());
            logMigration("category", "single", id, null, 0, e.getMessage());
        }
        
        return result;
    }
}
```

### 5.5 日志记录

```java
@Service
public class MigrationLogService {
    
    @Autowired
    private MigrationLogRepository migrationLogRepository;
    
    public void logMigration(String module, String migrationType, 
                            Long sourceId, Long targetId, 
                            int status, String errorMessage) {
        MigrationLog log = new MigrationLog();
        log.setId(generateId());
        log.setModule(module);
        log.setMigrationType(migrationType);
        log.setSourceId(sourceId);
        log.setTargetId(targetId);
        log.setStatus(status);
        log.setErrorMessage(errorMessage);
        log.setMigrationTime(LocalDateTime.now());
        
        migrationLogRepository.save(log);
    }
    
    public List<MigrationLog> getMigrationLogs(String module, int limit) {
        return migrationLogRepository.findByModuleOrderByMigrationTimeDesc(module)
            .stream()
            .limit(limit)
            .collect(Collectors.toList());
    }
    
    public MigrationStatistics getStatistics(String module) {
        MigrationStatistics stats = new MigrationStatistics();
        stats.setModule(module);
        stats.setTotalCount(migrationLogRepository.countByModule(module));
        stats.setSuccessCount(migrationLogRepository.countByModuleAndStatus(module, 1));
        stats.setFailureCount(migrationLogRepository.countByModuleAndStatus(module, 0));
        return stats;
    }
}
```

---

## 6. 迁移执行流程

### 6.1 阶段划分

```
┌──────────────────────────────────────────────────────────────────────────┐
│ 准备阶段                                                                  │
├──────────────────────────────────────────────────────────────────────────┤
│ 前置条件：                                                                 │
│ □ v2表结构已创建                                                          │
│ □ 迁移API已部署                                                           │
│ □ 数据已备份                                                              │
│                                                                          │
│ 执行步骤：                                                                 │
│ □ 调用 GET /api/v1/migration/status 检查当前状态                           │
│ □ 确认v1表数据量                                                          │
│ □ 确认v2表为空                                                            │
└──────────────────────────────────────────────────────────────────────────┘
                               ↓
┌──────────────────────────────────────────────────────────────────────────┐
│ 阶段1：分类模块迁移                                                        │
├──────────────────────────────────────────────────────────────────────────┤
│ 前置条件：无依赖                                                           │
│                                                                          │
│ 执行步骤：                                                                 │
│ □ POST /api/v1/migration/category/all                                     │
│ □ POST /api/v1/migration/category/all/validate                           │
│                                                                          │
│ 验证步骤：                                                                 │
│ □ GET /api/v1/migration/status/category 检查迁移数量                       │
│ □ 对比v1和v2数据量                                                        │
│ □ 抽查数据一致性                                                          │
│                                                                          │
│ 回滚策略：TRUNCATE TABLE openplatform_v2_category_t                        │
└──────────────────────────────────────────────────────────────────────────┘
                               ↓
┌──────────────────────────────────────────────────────────────────────────┐
│ 阶段2：资源模块迁移                                                        │
├──────────────────────────────────────────────────────────────────────────┤
│ 前置条件：分类模块迁移完成                                                  │
│                                                                          │
│ 执行步骤：                                                                 │
│ □ POST /api/v1/migration/api/all                                         │
│ □ POST /api/v1/migration/api/all/validate                                │
│ □ POST /api/v1/migration/event/all                                       │
│ □ POST /api/v1/migration/event/all/validate                              │
│                                                                          │
│ 验证步骤：                                                                 │
│ □ GET /api/v1/migration/status/api                                        │
│ □ GET /api/v1/migration/status/event                                      │
│                                                                          │
│ 回滚策略：                                                                 │
│ □ TRUNCATE TABLE openplatform_v2_api_t                                    │
│ □ TRUNCATE TABLE openplatform_v2_api_p_t                                  │
│ □ TRUNCATE TABLE openplatform_v2_event_t                                  │
│ □ TRUNCATE TABLE openplatform_v2_event_p_t                                │
└──────────────────────────────────────────────────────────────────────────┘
                               ↓
┌──────────────────────────────────────────────────────────────────────────┐
│ 阶段3：权限模块迁移（核心）                                                 │
├──────────────────────────────────────────────────────────────────────────┤
│ 前置条件：资源模块迁移完成                                                  │
│                                                                          │
│ 执行步骤：                                                                 │
│ □ POST /api/v1/migration/permission/all                                   │
│ □ POST /api/v1/migration/permission/build-mapping                         │
│ □ POST /api/v1/migration/permission/all/validate                          │
│                                                                          │
│ 验证步骤：                                                                 │
│ □ 检查权限数量是否符合预期（可能比v1多）                                     │
│ □ 检查scope唯一性                                                         │
│ □ 检查资源关联完整性                                                       │
│                                                                          │
│ 回滚策略：                                                                 │
│ □ TRUNCATE TABLE openplatform_v2_permission_t                             │
│ □ TRUNCATE TABLE openplatform_v2_permission_p_t                           │
│ □ TRUNCATE TABLE openplatform_migration_permission_mapping_t              │
└──────────────────────────────────────────────────────────────────────────┘
                               ↓
┌──────────────────────────────────────────────────────────────────────────┐
│ 阶段4：审批模块迁移                                                        │
├──────────────────────────────────────────────────────────────────────────┤
│ 前置条件：权限模块迁移完成                                                  │
│                                                                          │
│ 执行步骤：                                                                 │
│ □ POST /api/v1/migration/approval/init-templates                          │
│ □ POST /api/v1/migration/approval/all                                     │
│ □ POST /api/v1/migration/approval/all/validate                            │
│                                                                          │
│ 验证步骤：                                                                 │
│ □ 检查审批模板数量                                                         │
│ □ 检查审批记录数量                                                         │
│ □ 检查审批日志数量                                                         │
│                                                                          │
│ 回滚策略：                                                                 │
│ □ TRUNCATE TABLE openplatform_v2_approval_flow_t                          │
│ □ TRUNCATE TABLE openplatform_v2_approval_record_t                        │
│ □ TRUNCATE TABLE openplatform_v2_approval_log_t                           │
└──────────────────────────────────────────────────────────────────────────┘
                               ↓
┌──────────────────────────────────────────────────────────────────────────┐
│ 阶段5：业务关联迁移                                                        │
├──────────────────────────────────────────────────────────────────────────┤
│ 前置条件：权限模块迁移完成，权限ID映射已构建                                  │
│                                                                          │
│ 执行步骤：                                                                 │
│ □ POST /api/v1/migration/subscription/all                                 │
│ □ POST /api/v1/migration/subscription/all/validate                        │
│                                                                          │
│ 验证步骤：                                                                 │
│ □ 检查订阅数量                                                            │
│ □ 检查权限关联完整性                                                       │
│ □ 检查应用关联完整性                                                       │
│                                                                          │
│ 回滚策略：TRUNCATE TABLE openplatform_v2_subscription_t                    │
└──────────────────────────────────────────────────────────────────────────┘
                               ↓
┌──────────────────────────────────────────────────────────────────────────┐
│ 验证阶段                                                                  │
├──────────────────────────────────────────────────────────────────────────┤
│ 全局验证：                                                                 │
│ □ GET /api/v1/migration/status                                            │
│ □ 对比所有模块数据量                                                       │
│ □ 执行业务功能验证                                                         │
│                                                                          │
│ 功能验证：                                                                 │
│ □ 测试v2版本API调用                                                       │
│ □ 测试v2版本权限校验                                                       │
│ □ 测试v2版本审批流程                                                       │
└──────────────────────────────────────────────────────────────────────────┘
```

### 6.2 每个阶段的验证点

| 阶段 | 验证项 | 验证标准 | 验证方法 |
|------|--------|----------|----------|
| 准备阶段 | 表结构完整性 | v2表已创建 | DESCRIBE TABLE |
| 准备阶段 | API可用性 | 迁移API可访问 | curl测试 |
| 阶段1 | 数据量一致 | v1数量 = v2数量 | COUNT(*) |
| 阶段1 | path正确性 | 所有分类path已填充 | IS NOT NULL检查 |
| 阶段2 | API数量一致 | v1数量 = v2数量 | COUNT(*) |
| 阶段2 | 事件数量一致 | v1数量 = v2数量 | COUNT(*) |
| 阶段3 | scope唯一性 | 无重复scope | GROUP BY HAVING |
| 阶段3 | 资源关联完整 | 所有权限关联有效资源 | LEFT JOIN检查 |
| 阶段3 | 映射表完整 | 所有旧权限ID已映射 | COUNT对比 |
| 阶段4 | 模板数量正确 | 至少4个模板 | COUNT(*) |
| 阶段4 | 记录关联完整 | 所有记录有关联日志 | LEFT JOIN检查 |
| 阶段5 | 订阅数量一致 | v1数量 = v2数量 | COUNT(*) |
| 阶段5 | 权限关联完整 | 所有订阅关联有效权限 | LEFT JOIN检查 |

### 6.3 回滚策略

#### 6.3.1 单模块回滚

```sql
-- 分类模块回滚
TRUNCATE TABLE `openplatform_v2_category_t`;
TRUNCATE TABLE `openplatform_v2_category_owner_t`;
DELETE FROM `openplatform_migration_log_t` WHERE `module` = 'category';

-- API资源回滚
TRUNCATE TABLE `openplatform_v2_api_t`;
TRUNCATE TABLE `openplatform_v2_api_p_t`;
DELETE FROM `openplatform_migration_log_t` WHERE `module` = 'api';

-- 权限模块回滚
TRUNCATE TABLE `openplatform_v2_permission_t`;
TRUNCATE TABLE `openplatform_v2_permission_p_t`;
TRUNCATE TABLE `openplatform_migration_permission_mapping_t`;
DELETE FROM `openplatform_migration_log_t` WHERE `module` = 'permission';

-- 审批模块回滚
TRUNCATE TABLE `openplatform_v2_approval_flow_t`;
TRUNCATE TABLE `openplatform_v2_approval_record_t`;
TRUNCATE TABLE `openplatform_v2_approval_log_t`;
DELETE FROM `openplatform_migration_log_t` WHERE `module` = 'approval';

-- 订阅关系回滚
TRUNCATE TABLE `openplatform_v2_subscription_t`;
DELETE FROM `openplatform_migration_log_t` WHERE `module` = 'subscription';
```

#### 6.3.2 全量回滚

```sql
-- 全量回滚脚本
TRUNCATE TABLE `openplatform_v2_category_t`;
TRUNCATE TABLE `openplatform_v2_category_owner_t`;
TRUNCATE TABLE `openplatform_v2_api_t`;
TRUNCATE TABLE `openplatform_v2_api_p_t`;
TRUNCATE TABLE `openplatform_v2_event_t`;
TRUNCATE TABLE `openplatform_v2_event_p_t`;
TRUNCATE TABLE `openplatform_v2_permission_t`;
TRUNCATE TABLE `openplatform_v2_permission_p_t`;
TRUNCATE TABLE `openplatform_v2_approval_flow_t`;
TRUNCATE TABLE `openplatform_v2_approval_record_t`;
TRUNCATE TABLE `openplatform_v2_approval_log_t`;
TRUNCATE TABLE `openplatform_v2_subscription_t`;
TRUNCATE TABLE `openplatform_migration_permission_mapping_t`;
TRUNCATE TABLE `openplatform_migration_log_t`;
TRUNCATE TABLE `openplatform_migration_summary_t`;
```

---

## 7. 示例代码（Java/Spring Boot）

### 7.1 MigrationController示例

```java
@RestController
@RequestMapping("/api/v1/migration")
@Api(tags = "数据迁移API")
public class MigrationController {
    
    @Autowired
    private MigrationService migrationService;
    
    @Autowired
    private MigrationStatusService statusService;
    
    // ==================== 分类模块迁移 ====================
    
    @PostMapping("/category/{id}")
    @ApiOperation("单条分类迁移")
    public ApiResponse<MigrationResult> migrateCategory(@PathVariable Long id) {
        MigrationResult result = migrationService.migrateCategory(id);
        return ApiResponse.success(result);
    }
    
    @PostMapping("/category/batch")
    @ApiOperation("批量分类迁移")
    public ApiResponse<BatchMigrationResult> migrateCategories(
            @RequestBody BatchMigrationRequest request) {
        BatchMigrationResult result = migrationService.migrateCategories(request.getIds());
        return ApiResponse.success(result);
    }
    
    @PostMapping("/category/all")
    @ApiOperation("全量分类迁移")
    public ApiResponse<BatchMigrationResult> migrateAllCategories() {
        BatchMigrationResult result = migrationService.migrateAllCategories();
        return ApiResponse.success(result);
    }
    
    @PostMapping("/category/{id}/validate")
    @ApiOperation("单条分类验证")
    public ApiResponse<ValidationResult> validateCategory(@PathVariable Long id) {
        ValidationResult result = migrationService.validateCategory(id);
        return ApiResponse.success(result);
    }
    
    @PostMapping("/category/batch/validate")
    @ApiOperation("批量分类验证")
    public ApiResponse<BatchValidationResult> validateCategories(
            @RequestBody BatchMigrationRequest request) {
        BatchValidationResult result = migrationService.validateCategories(request.getIds());
        return ApiResponse.success(result);
    }
    
    @PostMapping("/category/all/validate")
    @ApiOperation("全量分类验证")
    public ApiResponse<BatchValidationResult> validateAllCategories() {
        BatchValidationResult result = migrationService.validateAllCategories();
        return ApiResponse.success(result);
    }
    
    // ==================== API资源迁移 ====================
    
    @PostMapping("/api/{id}")
    @ApiOperation("单条API迁移")
    public ApiResponse<MigrationResult> migrateApi(@PathVariable Long id) {
        MigrationResult result = migrationService.migrateApi(id);
        return ApiResponse.success(result);
    }
    
    @PostMapping("/api/batch")
    @ApiOperation("批量API迁移")
    public ApiResponse<BatchMigrationResult> migrateApis(
            @RequestBody BatchMigrationRequest request) {
        BatchMigrationResult result = migrationService.migrateApis(request.getIds());
        return ApiResponse.success(result);
    }
    
    @PostMapping("/api/all")
    @ApiOperation("全量API迁移")
    public ApiResponse<BatchMigrationResult> migrateAllApis() {
        BatchMigrationResult result = migrationService.migrateAllApis();
        return ApiResponse.success(result);
    }
    
    @PostMapping("/api/{id}/validate")
    @ApiOperation("单条API验证")
    public ApiResponse<ValidationResult> validateApi(@PathVariable Long id) {
        ValidationResult result = migrationService.validateApi(id);
        return ApiResponse.success(result);
    }
    
    @PostMapping("/api/batch/validate")
    @ApiOperation("批量API验证")
    public ApiResponse<BatchValidationResult> validateApis(
            @RequestBody BatchMigrationRequest request) {
        BatchValidationResult result = migrationService.validateApis(request.getIds());
        return ApiResponse.success(result);
    }
    
    @PostMapping("/api/all/validate")
    @ApiOperation("全量API验证")
    public ApiResponse<BatchValidationResult> validateAllApis() {
        BatchValidationResult result = migrationService.validateAllApis();
        return ApiResponse.success(result);
    }
    
    // ==================== 事件资源迁移 ====================
    
    @PostMapping("/event/{id}")
    @ApiOperation("单条事件迁移")
    public ApiResponse<MigrationResult> migrateEvent(@PathVariable Long id) {
        MigrationResult result = migrationService.migrateEvent(id);
        return ApiResponse.success(result);
    }
    
    @PostMapping("/event/batch")
    @ApiOperation("批量事件迁移")
    public ApiResponse<BatchMigrationResult> migrateEvents(
            @RequestBody BatchMigrationRequest request) {
        BatchMigrationResult result = migrationService.migrateEvents(request.getIds());
        return ApiResponse.success(result);
    }
    
    @PostMapping("/event/all")
    @ApiOperation("全量事件迁移")
    public ApiResponse<BatchMigrationResult> migrateAllEvents() {
        BatchMigrationResult result = migrationService.migrateAllEvents();
        return ApiResponse.success(result);
    }
    
    @PostMapping("/event/{id}/validate")
    @ApiOperation("单条事件验证")
    public ApiResponse<ValidationResult> validateEvent(@PathVariable Long id) {
        ValidationResult result = migrationService.validateEvent(id);
        return ApiResponse.success(result);
    }
    
    @PostMapping("/event/batch/validate")
    @ApiOperation("批量事件验证")
    public ApiResponse<BatchValidationResult> validateEvents(
            @RequestBody BatchMigrationRequest request) {
        BatchValidationResult result = migrationService.validateEvents(request.getIds());
        return ApiResponse.success(result);
    }
    
    @PostMapping("/event/all/validate")
    @ApiOperation("全量事件验证")
    public ApiResponse<BatchValidationResult> validateAllEvents() {
        BatchValidationResult result = migrationService.validateAllEvents();
        return ApiResponse.success(result);
    }
    
    // ==================== 权限资源迁移 ====================
    
    @PostMapping("/permission/{id}")
    @ApiOperation("单条权限迁移")
    public ApiResponse<MigrationResult> migratePermission(@PathVariable Long id) {
        MigrationResult result = migrationService.migratePermission(id);
        return ApiResponse.success(result);
    }
    
    @PostMapping("/permission/batch")
    @ApiOperation("批量权限迁移")
    public ApiResponse<BatchMigrationResult> migratePermissions(
            @RequestBody BatchMigrationRequest request) {
        BatchMigrationResult result = migrationService.migratePermissions(request.getIds());
        return ApiResponse.success(result);
    }
    
    @PostMapping("/permission/all")
    @ApiOperation("全量权限迁移")
    public ApiResponse<BatchMigrationResult> migrateAllPermissions() {
        BatchMigrationResult result = migrationService.migrateAllPermissions();
        return ApiResponse.success(result);
    }
    
    @PostMapping("/permission/build-mapping")
    @ApiOperation("构建权限ID映射表")
    public ApiResponse<Void> buildPermissionMapping() {
        migrationService.buildPermissionMapping();
        return ApiResponse.success(null);
    }
    
    @PostMapping("/permission/{id}/validate")
    @ApiOperation("单条权限验证")
    public ApiResponse<ValidationResult> validatePermission(@PathVariable Long id) {
        ValidationResult result = migrationService.validatePermission(id);
        return ApiResponse.success(result);
    }
    
    @PostMapping("/permission/batch/validate")
    @ApiOperation("批量权限验证")
    public ApiResponse<BatchValidationResult> validatePermissions(
            @RequestBody BatchMigrationRequest request) {
        BatchValidationResult result = migrationService.validatePermissions(request.getIds());
        return ApiResponse.success(result);
    }
    
    @PostMapping("/permission/all/validate")
    @ApiOperation("全量权限验证")
    public ApiResponse<BatchValidationResult> validateAllPermissions() {
        BatchValidationResult result = migrationService.validateAllPermissions();
        return ApiResponse.success(result);
    }
    
    // ==================== 审批流程迁移 ====================
    
    @PostMapping("/approval/init-templates")
    @ApiOperation("初始化审批模板")
    public ApiResponse<Void> initApprovalTemplates() {
        migrationService.initApprovalTemplates();
        return ApiResponse.success(null);
    }
    
    @PostMapping("/approval/{id}")
    @ApiOperation("单条审批记录迁移")
    public ApiResponse<MigrationResult> migrateApproval(@PathVariable Long id) {
        MigrationResult result = migrationService.migrateApproval(id);
        return ApiResponse.success(result);
    }
    
    @PostMapping("/approval/batch")
    @ApiOperation("批量审批记录迁移")
    public ApiResponse<BatchMigrationResult> migrateApprovals(
            @RequestBody BatchMigrationRequest request) {
        BatchMigrationResult result = migrationService.migrateApprovals(request.getIds());
        return ApiResponse.success(result);
    }
    
    @PostMapping("/approval/all")
    @ApiOperation("全量审批记录迁移")
    public ApiResponse<BatchMigrationResult> migrateAllApprovals() {
        BatchMigrationResult result = migrationService.migrateAllApprovals();
        return ApiResponse.success(result);
    }
    
    @PostMapping("/approval/{id}/validate")
    @ApiOperation("单条审批记录验证")
    public ApiResponse<ValidationResult> validateApproval(@PathVariable Long id) {
        ValidationResult result = migrationService.validateApproval(id);
        return ApiResponse.success(result);
    }
    
    @PostMapping("/approval/batch/validate")
    @ApiOperation("批量审批记录验证")
    public ApiResponse<BatchValidationResult> validateApprovals(
            @RequestBody BatchMigrationRequest request) {
        BatchValidationResult result = migrationService.validateApprovals(request.getIds());
        return ApiResponse.success(result);
    }
    
    @PostMapping("/approval/all/validate")
    @ApiOperation("全量审批记录验证")
    public ApiResponse<BatchValidationResult> validateAllApprovals() {
        BatchValidationResult result = migrationService.validateAllApprovals();
        return ApiResponse.success(result);
    }
    
    // ==================== 订阅关系迁移 ====================
    
    @PostMapping("/subscription/{id}")
    @ApiOperation("单条订阅迁移")
    public ApiResponse<MigrationResult> migrateSubscription(@PathVariable Long id) {
        MigrationResult result = migrationService.migrateSubscription(id);
        return ApiResponse.success(result);
    }
    
    @PostMapping("/subscription/batch")
    @ApiOperation("批量订阅迁移")
    public ApiResponse<BatchMigrationResult> migrateSubscriptions(
            @RequestBody BatchMigrationRequest request) {
        BatchMigrationResult result = migrationService.migrateSubscriptions(request.getIds());
        return ApiResponse.success(result);
    }
    
    @PostMapping("/subscription/all")
    @ApiOperation("全量订阅迁移")
    public ApiResponse<BatchMigrationResult> migrateAllSubscriptions() {
        BatchMigrationResult result = migrationService.migrateAllSubscriptions();
        return ApiResponse.success(result);
    }
    
    @PostMapping("/subscription/{id}/validate")
    @ApiOperation("单条订阅验证")
    public ApiResponse<ValidationResult> validateSubscription(@PathVariable Long id) {
        ValidationResult result = migrationService.validateSubscription(id);
        return ApiResponse.success(result);
    }
    
    @PostMapping("/subscription/batch/validate")
    @ApiOperation("批量订阅验证")
    public ApiResponse<BatchValidationResult> validateSubscriptions(
            @RequestBody BatchMigrationRequest request) {
        BatchValidationResult result = migrationService.validateSubscriptions(request.getIds());
        return ApiResponse.success(result);
    }
    
    @PostMapping("/subscription/all/validate")
    @ApiOperation("全量订阅验证")
    public ApiResponse<BatchValidationResult> validateAllSubscriptions() {
        BatchValidationResult result = migrationService.validateAllSubscriptions();
        return ApiResponse.success(result);
    }
    
    // ==================== 迁移状态查询 ====================
    
    @GetMapping("/status")
    @ApiOperation("查询所有模块迁移状态")
    public ApiResponse<Map<String, ModuleMigrationStatus>> getAllMigrationStatus() {
        Map<String, ModuleMigrationStatus> status = statusService.getAllModuleStatus();
        return ApiResponse.success(status);
    }
    
    @GetMapping("/status/{module}")
    @ApiOperation("查询指定模块迁移状态")
    public ApiResponse<ModuleMigrationStatus> getModuleMigrationStatus(
            @PathVariable String module) {
        ModuleMigrationStatus status = statusService.getModuleStatus(module);
        return ApiResponse.success(status);
    }
    
    @GetMapping("/logs")
    @ApiOperation("查询迁移日志")
    public ApiResponse<List<MigrationLog>> getMigrationLogs(
            @RequestParam(required = false) String module,
            @RequestParam(defaultValue = "100") int limit) {
        List<MigrationLog> logs = statusService.getMigrationLogs(module, limit);
        return ApiResponse.success(logs);
    }
}
```

### 7.2 MigrationService示例

```java
@Service
@Slf4j
public class MigrationServiceImpl implements MigrationService {
    
    @Autowired
    private CategoryV1Repository categoryV1Repository;
    
    @Autowired
    private CategoryV2Repository categoryV2Repository;
    
    @Autowired
    private ApiV1Repository apiV1Repository;
    
    @Autowired
    private ApiV2Repository apiV2Repository;
    
    @Autowired
    private EventV1Repository eventV1Repository;
    
    @Autowired
    private EventV2Repository eventV2Repository;
    
    @Autowired
    private PermissionV1Repository permissionV1Repository;
    
    @Autowired
    private PermissionV2Repository permissionV2Repository;
    
    @Autowired
    private SubscriptionV1Repository subscriptionV1Repository;
    
    @Autowired
    private SubscriptionV2Repository subscriptionV2Repository;
    
    @Autowired
    private PermissionMappingRepository permissionMappingRepository;
    
    @Autowired
    private MigrationLogService migrationLogService;
    
    @Autowired
    private MigrationSummaryService summaryService;
    
    @Autowired
    private IdGenerator idGenerator;
    
    // ==================== 分类模块迁移 ====================
    
    @Override
    public MigrationResult migrateCategory(Long id) {
        MigrationResult result = new MigrationResult();
        result.setSourceId(id);
        
        try {
            CategoryV1 v1 = categoryV1Repository.findById(id);
            if (v1 == null) {
                result.setSuccess(false);
                result.setErrorMessage("Source category not found: " + id);
                migrationLogService.log("category", "single", id, null, 0, "Source not found");
                return result;
            }
            
            if (categoryV2Repository.existsById(id)) {
                result.setSuccess(true);
                result.setTargetId(id);
                result.setErrorMessage("Already migrated, skipped");
                return result;
            }
            
            CategoryV2 v2 = convertCategory(v1);
            categoryV2Repository.save(v2);
            
            result.setSuccess(true);
            result.setTargetId(id);
            result.setMigrationTime(LocalDateTime.now().toString());
            migrationLogService.log("category", "single", id, id, 1, null);
            summaryService.incrementMigrated("category");
            
        } catch (Exception e) {
            log.error("Failed to migrate category: id={}", id, e);
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            migrationLogService.log("category", "single", id, null, 0, e.getMessage());
        }
        
        return result;
    }
    
    @Override
    @Transactional
    public BatchMigrationResult migrateCategories(List<Long> ids) {
        BatchMigrationResult result = new BatchMigrationResult();
        result.setTotalCount(ids.size());
        
        List<MigrationResult> details = new ArrayList<>();
        int successCount = 0;
        int failureCount = 0;
        
        for (Long id : ids) {
            MigrationResult single = migrateCategory(id);
            details.add(single);
            
            if (single.isSuccess()) {
                successCount++;
            } else {
                failureCount++;
            }
        }
        
        result.setSuccessCount(successCount);
        result.setFailureCount(failureCount);
        result.setDetails(details);
        
        return result;
    }
    
    @Override
    @Transactional
    public BatchMigrationResult migrateAllCategories() {
        List<Long> allIds = categoryV1Repository.findAllIds();
        return migrateCategories(allIds);
    }
    
    @Override
    public ValidationResult validateCategory(Long id) {
        ValidationResult result = new ValidationResult();
        result.setSourceId(id);
        
        List<String> errors = new ArrayList<>();
        
        try {
            CategoryV1 v1 = categoryV1Repository.findById(id);
            if (v1 == null) {
                errors.add("Source category not found");
                result.setValid(false);
                result.setErrors(errors);
                return result;
            }
            
            CategoryV2 v2 = categoryV2Repository.findById(id);
            if (v2 == null) {
                errors.add("Target category not found");
                result.setValid(false);
                result.setErrors(errors);
                return result;
            }
            
            result.setTargetId(id);
            
            if (!Objects.equals(v1.getNodeNameCn(), v2.getNameCn())) {
                errors.add("name_cn mismatch");
            }
            
            if (!Objects.equals(v1.getNodeNameEn(), v2.getNameEn())) {
                errors.add("name_en mismatch");
            }
            
            if (v2.getPath() == null || v2.getPath().isEmpty()) {
                errors.add("path is null or empty");
            }
            
            result.setValid(errors.isEmpty());
            result.setErrors(errors);
            
        } catch (Exception e) {
            errors.add("Validation exception: " + e.getMessage());
            result.setValid(false);
            result.setErrors(errors);
        }
        
        return result;
    }
    
    @Override
    public BatchValidationResult validateCategories(List<Long> ids) {
        BatchValidationResult result = new BatchValidationResult();
        result.setTotalCount(ids.size());
        
        List<ValidationResult> details = new ArrayList<>();
        int validCount = 0;
        int invalidCount = 0;
        
        for (Long id : ids) {
            ValidationResult single = validateCategory(id);
            details.add(single);
            
            if (single.isValid()) {
                validCount++;
            } else {
                invalidCount++;
            }
        }
        
        result.setValidCount(validCount);
        result.setInvalidCount(invalidCount);
        result.setDetails(details);
        
        return result;
    }
    
    @Override
    public BatchValidationResult validateAllCategories() {
        List<Long> allIds = categoryV1Repository.findAllIds();
        return validateCategories(allIds);
    }
    
    // ==================== 分类转换逻辑 ====================
    
    private CategoryV2 convertCategory(CategoryV1 v1) {
        CategoryV2 v2 = new CategoryV2();
        v2.setId(v1.getId());
        v2.setNameCn(v1.getNodeNameCn());
        v2.setNameEn(v1.getNodeNameEn());
        v2.setParentId(normalizeParentId(v1.getParentNodeId()));
        v2.setSortOrder(v1.getOrderNum());
        v2.setStatus(v1.getStatus());
        v2.setCreateBy(v1.getCreateBy());
        v2.setCreateTime(v1.getCreateTime());
        v2.setLastUpdateBy(v1.getLastUpdateBy());
        v2.setLastUpdateTime(v1.getLastUpdateTime());
        
        if (v2.getParentId() == null || v2.getParentId() == 0) {
            v2.setCategoryAlias(generateCategoryAlias(v1));
        }
        
        v2.setPath(calculateCategoryPath(v2));
        
        return v2;
    }
    
    private Long normalizeParentId(Long parentId) {
        if (parentId == null || parentId == 0) {
            return null;
        }
        return parentId;
    }
    
    private String generateCategoryAlias(CategoryV1 v1) {
        if (v1.getNodeNameEn() != null && !v1.getNodeNameEn().isEmpty()) {
            return v1.getNodeNameEn().toLowerCase()
                .replace(" ", "_")
                .replace("-", "_");
        }
        return "category_" + v1.getId();
    }
    
    private String calculateCategoryPath(CategoryV2 v2) {
        if (v2.getParentId() == null || v2.getParentId() == 0) {
            return "/" + v2.getId() + "/";
        }
        
        CategoryV2 parent = categoryV2Repository.findById(v2.getParentId());
        if (parent == null) {
            return "/" + v2.getId() + "/";
        }
        
        return parent.getPath() + v2.getId() + "/";
    }
    
    // ==================== API资源迁移（类似实现） ====================
    
    @Override
    public MigrationResult migrateApi(Long id) {
        MigrationResult result = new MigrationResult();
        result.setSourceId(id);
        
        try {
            ApiV1 v1 = apiV1Repository.findById(id);
            if (v1 == null) {
                result.setSuccess(false);
                result.setErrorMessage("Source API not found");
                migrationLogService.log("api", "single", id, null, 0, "Source not found");
                return result;
            }
            
            if (apiV2Repository.existsById(id)) {
                result.setSuccess(true);
                result.setTargetId(id);
                return result;
            }
            
            ApiV2 v2 = convertApi(v1);
            apiV2Repository.save(v2);
            
            result.setSuccess(true);
            result.setTargetId(id);
            migrationLogService.log("api", "single", id, id, 1, null);
            summaryService.incrementMigrated("api");
            
        } catch (Exception e) {
            log.error("Failed to migrate API: id={}", id, e);
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            migrationLogService.log("api", "single", id, null, 0, e.getMessage());
        }
        
        return result;
    }
    
    @Override
    public BatchMigrationResult migrateApis(List<Long> ids) {
        BatchMigrationResult result = new BatchMigrationResult();
        result.setTotalCount(ids.size());
        
        List<MigrationResult> details = new ArrayList<>();
        int successCount = 0;
        int failureCount = 0;
        
        for (Long id : ids) {
            MigrationResult single = migrateApi(id);
            details.add(single);
            
            if (single.isSuccess()) {
                successCount++;
            } else {
                failureCount++;
            }
        }
        
        result.setSuccessCount(successCount);
        result.setFailureCount(failureCount);
        result.setDetails(details);
        
        return result;
    }
    
    @Override
    @Transactional
    public BatchMigrationResult migrateAllApis() {
        List<Long> allIds = apiV1Repository.findAllIds();
        return migrateApis(allIds);
    }
    
    @Override
    public ValidationResult validateApi(Long id) {
        ValidationResult result = new ValidationResult();
        result.setSourceId(id);
        
        List<String> errors = new ArrayList<>();
        
        try {
            ApiV1 v1 = apiV1Repository.findById(id);
            if (v1 == null) {
                errors.add("Source API not found");
                result.setValid(false);
                result.setErrors(errors);
                return result;
            }
            
            ApiV2 v2 = apiV2Repository.findById(id);
            if (v2 == null) {
                errors.add("Target API not found");
                result.setValid(false);
                result.setErrors(errors);
                return result;
            }
            
            result.setTargetId(id);
            
            if (!Objects.equals(v1.getApiNameCn(), v2.getNameCn())) {
                errors.add("name_cn mismatch");
            }
            
            if (!Objects.equals(v1.getApiPath(), v2.getPath())) {
                errors.add("path mismatch");
            }
            
            if (!Objects.equals(v1.getApiMethod(), v2.getMethod())) {
                errors.add("method mismatch");
            }
            
            result.setValid(errors.isEmpty());
            result.setErrors(errors);
            
        } catch (Exception e) {
            errors.add("Validation exception: " + e.getMessage());
            result.setValid(false);
            result.setErrors(errors);
        }
        
        return result;
    }
    
    @Override
    public BatchValidationResult validateApis(List<Long> ids) {
        BatchValidationResult result = new BatchValidationResult();
        result.setTotalCount(ids.size());
        
        List<ValidationResult> details = new ArrayList<>();
        int validCount = 0;
        int invalidCount = 0;
        
        for (Long id : ids) {
            ValidationResult single = validateApi(id);
            details.add(single);
            
            if (single.isValid()) {
                validCount++;
            } else {
                invalidCount++;
            }
        }
        
        result.setValidCount(validCount);
        result.setInvalidCount(invalidCount);
        result.setDetails(details);
        
        return result;
    }
    
    @Override
    public BatchValidationResult validateAllApis() {
        List<Long> allIds = apiV1Repository.findAllIds();
        return validateApis(allIds);
    }
    
    private ApiV2 convertApi(ApiV1 v1) {
        ApiV2 v2 = new ApiV2();
        v2.setId(v1.getId());
        v2.setNameCn(v1.getApiNameCn());
        v2.setNameEn(v1.getApiNameEn());
        v2.setPath(v1.getApiPath());
        v2.setMethod(v1.getApiMethod());
        v2.setAuthType(v1.getAuthType());
        v2.setStatus(convertStatus(v1.getStatus()));
        v2.setCreateBy(v1.getCreateBy());
        v2.setCreateTime(v1.getCreateTime());
        v2.setLastUpdateBy(v1.getLastUpdateBy());
        v2.setLastUpdateTime(v1.getLastUpdateTime());
        return v2;
    }
    
    private Integer convertStatus(Integer oldStatus) {
        if (oldStatus == null) return 2;
        return oldStatus == 1 ? 2 : 3;
    }
    
    // ... 其他模块迁移方法实现类似
}
```

### 7.3 MigrationExecutor示例

```java
@Component
@Slf4j
public class MigrationExecutor {
    
    @Autowired
    private MigrationService migrationService;
    
    @Autowired
    private MigrationSummaryService summaryService;
    
    public void executeFullMigration() {
        log.info("Starting full migration...");
        
        // 阶段1：分类模块
        log.info("Phase 1: Migrating categories...");
        BatchMigrationResult categoryResult = migrationService.migrateAllCategories();
        log.info("Category migration completed: {}/{} successful", 
            categoryResult.getSuccessCount(), categoryResult.getTotalCount());
        
        if (categoryResult.getFailureCount() > 0) {
            log.warn("Category migration has failures, but continuing...");
        }
        
        // 阶段2：资源模块
        log.info("Phase 2: Migrating resources...");
        BatchMigrationResult apiResult = migrationService.migrateAllApis();
        log.info("API migration completed: {}/{} successful", 
            apiResult.getSuccessCount(), apiResult.getTotalCount());
        
        BatchMigrationResult eventResult = migrationService.migrateAllEvents();
        log.info("Event migration completed: {}/{} successful", 
            eventResult.getSuccessCount(), eventResult.getTotalCount());
        
        // 阶段3：权限模块
        log.info("Phase 3: Migrating permissions...");
        BatchMigrationResult permissionResult = migrationService.migrateAllPermissions();
        log.info("Permission migration completed: {}/{} successful", 
            permissionResult.getSuccessCount(), permissionResult.getTotalCount());
        
        log.info("Building permission mapping...");
        migrationService.buildPermissionMapping();
        
        // 阶段4：审批模块
        log.info("Phase 4: Migrating approvals...");
        migrationService.initApprovalTemplates();
        BatchMigrationResult approvalResult = migrationService.migrateAllApprovals();
        log.info("Approval migration completed: {}/{} successful", 
            approvalResult.getSuccessCount(), approvalResult.getTotalCount());
        
        // 阶段5：订阅关系
        log.info("Phase 5: Migrating subscriptions...");
        BatchMigrationResult subscriptionResult = migrationService.migrateAllSubscriptions();
        log.info("Subscription migration completed: {}/{} successful", 
            subscriptionResult.getSuccessCount(), subscriptionResult.getTotalCount());
        
        log.info("Full migration completed!");
        printSummary();
    }
    
    private void printSummary() {
        Map<String, ModuleMigrationStatus> allStatus = summaryService.getAllModuleStatus();
        
        log.info("Migration Summary:");
        log.info("========================================");
        
        for (Map.Entry<String, ModuleMigrationStatus> entry : allStatus.entrySet()) {
            ModuleMigrationStatus status = entry.getValue();
            log.info("{}: {}/{} migrated", 
                entry.getKey(), 
                status.getMigratedCount(), 
                status.getSourceCount());
        }
        
        log.info("========================================");
    }
}
```

### 7.4 MigrationValidator示例

```java
@Component
@Slf4j
public class MigrationValidator {
    
    @Autowired
    private MigrationService migrationService;
    
    @Autowired
    private CategoryV1Repository categoryV1Repository;
    
    @Autowired
    private CategoryV2Repository categoryV2Repository;
    
    @Autowired
    private ApiV1Repository apiV1Repository;
    
    @Autowired
    private ApiV2Repository apiV2Repository;
    
    public void validateAll() {
        log.info("Starting full validation...");
        
        validateCategories();
        validateApis();
        validateEvents();
        validatePermissions();
        validateApprovals();
        validateSubscriptions();
        
        log.info("Full validation completed!");
    }
    
    private void validateCategories() {
        log.info("Validating categories...");
        
        int sourceCount = categoryV1Repository.count();
        int targetCount = categoryV2Repository.count();
        
        log.info("Category count: source={}, target={}", sourceCount, targetCount);
        
        if (sourceCount != targetCount) {
            log.warn("Category count mismatch!");
        }
        
        BatchValidationResult result = migrationService.validateAllCategories();
        log.info("Category validation: {}/{} valid", 
            result.getValidCount(), result.getTotalCount());
        
        if (result.getInvalidCount() > 0) {
            log.warn("Invalid categories: {}", 
                result.getDetails().stream()
                    .filter(d -> !d.isValid())
                    .map(d -> d.getSourceId() + ": " + d.getErrors())
                    .collect(Collectors.joining(", ")));
        }
    }
    
    private void validateApis() {
        log.info("Validating APIs...");
        
        int sourceCount = apiV1Repository.count();
        int targetCount = apiV2Repository.count();
        
        log.info("API count: source={}, target={}", sourceCount, targetCount);
        
        if (sourceCount != targetCount) {
            log.warn("API count mismatch!");
        }
        
        BatchValidationResult result = migrationService.validateAllApis();
        log.info("API validation: {}/{} valid", 
            result.getValidCount(), result.getTotalCount());
    }
    
    // ... 其他验证方法
}
```

---

## 8. 迁移执行清单

### 8.1 迁移前检查清单

```
□ 环境检查
  ├── □ 数据库连接正常
  ├── □ v2表结构已创建
  ├── □ 迁移API已部署并可访问
  └── □ 应用服务运行正常

□ 数据检查
  ├── □ v1表数据完整性检查
  ├── □ 数据备份已完成
  └── □ v2表为空（无残留数据）

□ API检查
  ├── □ GET /api/v1/migration/status 返回正常
  ├── □ 单条迁移API测试通过
  └── □ 验证API测试通过

□ 配置检查
  ├── □ 批量迁移每批数量配置合理
  ├── □ 事务超时时间配置合理
  └── □ 日志级别配置正确
```

### 8.2 迁移中监控清单

```
□ 分类模块迁移
  ├── □ POST /api/v1/migration/category/all 执行成功
  ├── □ GET /api/v1/migration/status/category 数据量正确
  ├── □ 迁移日志无异常
  └── □ POST /api/v1/migration/category/all/validate 验证通过

□ 资源模块迁移
  ├── □ API资源迁移成功
  ├── □ 事件资源迁移成功
  └── □ 验证通过

□ 权限模块迁移
  ├── □ 权限迁移成功
  ├── □ 权限ID映射构建成功
  └── □ 验证通过

□ 审批模块迁移
  ├── □ 审批模板初始化成功
  ├── □ 审批记录迁移成功
  └── □ 验证通过

□ 订阅关系迁移
  ├── □ 订阅迁移成功
  ├── □ 权限关联完整
  └── □ 验证通过
```

### 8.3 迁移后验证清单

```
□ 数据量验证
  ├── □ 分类数量一致
  ├── □ API数量一致
  ├── □ 事件数量一致
  ├── □ 权限数量符合预期（可能比v1多）
  ├── □ 审批记录数量一致
  └── □ 订阅数量一致

□ 数据一致性验证
  ├── □ 字段值一致性抽查
  ├── □ 关联关系完整性检查
  └── □ 新字段填充正确性检查

□ 业务功能验证
  ├── □ v2版本API调用测试
  ├── □ v2版本权限校验测试
  ├── □ v2版本审批流程测试
  └── □ v2版本订阅流程测试

□ 性能验证
  ├── □ 查询性能满足要求
  ├── □ 写入性能满足要求
  └── □ 索引效率正常

□ 日志验证
  ├── □ 迁移日志完整
  ├── □ 无异常错误日志
  └── □ 统计数据正确
```

---

## 附录

### A. API响应示例

#### A.1 单条迁移成功响应

```json
{
    "code": 0,
    "message": "success",
    "data": {
        "sourceId": 123,
        "targetId": 123,
        "success": true,
        "errorMessage": null,
        "migrationTime": "2024-04-28T10:30:00.000Z"
    },
    "timestamp": 1714295400000
}
```

#### A.2 单条迁移失败响应

```json
{
    "code": 1001,
    "message": "Source data not found",
    "data": {
        "sourceId": 999,
        "targetId": null,
        "success": false,
        "errorMessage": "Source category not found: 999",
        "migrationTime": null
    },
    "timestamp": 1714295400000
}
```

#### A.3 批量迁移响应

```json
{
    "code": 0,
    "message": "success",
    "data": {
        "totalCount": 5,
        "successCount": 4,
        "failureCount": 1,
        "details": [
            {
                "sourceId": 1,
                "targetId": 1,
                "success": true,
                "errorMessage": null,
                "migrationTime": "2024-04-28T10:30:01.000Z"
            },
            {
                "sourceId": 2,
                "targetId": 2,
                "success": true,
                "errorMessage": null,
                "migrationTime": "2024-04-28T10:30:02.000Z"
            },
            {
                "sourceId": 3,
                "targetId": null,
                "success": false,
                "errorMessage": "Source data not found",
                "migrationTime": null
            }
        ]
    },
    "timestamp": 1714295400000
}
```

#### A.4 验证响应

```json
{
    "code": 0,
    "message": "success",
    "data": {
        "sourceId": 123,
        "targetId": 123,
        "valid": true,
        "errors": []
    },
    "timestamp": 1714295400000
}
```

#### A.5 迁移状态响应

```json
{
    "code": 0,
    "message": "success",
    "data": {
        "category": {
            "sourceCount": 100,
            "targetCount": 100,
            "migratedCount": 100,
            "validatedCount": 100,
            "lastMigrationTime": "2024-04-28T10:30:00.000Z",
            "lastValidationTime": "2024-04-28T10:35:00.000Z",
            "status": 3
        },
        "api": {
            "sourceCount": 50,
            "targetCount": 50,
            "migratedCount": 50,
            "validatedCount": 50,
            "lastMigrationTime": "2024-04-28T10:40:00.000Z",
            "lastValidationTime": "2024-04-28T10:45:00.000Z",
            "status": 3
        }
    },
    "timestamp": 1714295400000
}
```

### B. 错误码详细说明

| 错误码 | 错误名称 | 说明 | 处理建议 |
|--------|----------|------|----------|
| 0 | SUCCESS | 成功 | - |
| 1001 | SOURCE_NOT_FOUND | 源数据不存在 | 检查源ID是否正确 |
| 1002 | TARGET_ALREADY_EXISTS | 目标数据已存在 | 已迁移，可跳过 |
| 1003 | DATA_CONVERSION_ERROR | 数据转换失败 | 检查数据格式 |
| 1004 | RELATED_DATA_MISSING | 关联数据缺失 | 先迁移关联数据 |
| 1005 | MIGRATION_EXECUTION_ERROR | 迁移执行异常 | 查看详细错误信息 |
| 2001 | VALIDATION_DATA_MISMATCH | 数据不一致 | 重新迁移该记录 |
| 2002 | VALIDATION_FIELD_MISSING | 字段缺失 | 检查字段填充逻辑 |
| 2003 | VALIDATION_RELATION_ERROR | 关联异常 | 检查关联数据 |
| 9999 | SYSTEM_ERROR | 系统异常 | 联系管理员 |

### C. 迁移执行命令参考

```bash
# 1. 检查迁移状态
curl -X GET "http://localhost:8080/api/v1/migration/status"

# 2. 执行分类迁移
curl -X POST "http://localhost:8080/api/v1/migration/category/all"

# 3. 验证分类迁移
curl -X POST "http://localhost:8080/api/v1/migration/category/all/validate"

# 4. 执行API资源迁移
curl -X POST "http://localhost:8080/api/v1/migration/api/all"

# 5. 执行事件资源迁移
curl -X POST "http://localhost:8080/api/v1/migration/event/all"

# 6. 执行权限迁移
curl -X POST "http://localhost:8080/api/v1/migration/permission/all"

# 7. 构建权限映射
curl -X POST "http://localhost:8080/api/v1/migration/permission/build-mapping"

# 8. 初始化审批模板
curl -X POST "http://localhost:8080/api/v1/migration/approval/init-templates"

# 9. 执行审批迁移
curl -X POST "http://localhost:8080/api/v1/migration/approval/all"

# 10. 执行订阅迁移
curl -X POST "http://localhost:8080/api/v1/migration/subscription/all"

# 11. 最终验证
curl -X GET "http://localhost:8080/api/v1/migration/status"
```

---

**文档版本**: v1.0
**创建日期**: 2024-04-28
**最后更新**: 2024-04-28
