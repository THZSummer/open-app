# 审批流程设计方案

> **版本**: 2.2.0  
> **创建时间**: 2026-04-24  
> **状态**: 设计完成

---

## 📋 目录

- [1. 概述](#1-概述)
- [2. 核心概念](#2-核心概念)
- [3. 数据库设计](#3-数据库设计)
- [4. 三级审批配置](#4-三级审批配置)
- [5. 组合逻辑](#5-组合逻辑)
- [6. 审批执行流程](#6-审批执行流程)
- [7. 实现方案](#7-实现方案)
- [8. 示例场景](#8-示例场景)

---

## 1. 概述

### 1.1 业务场景

开放平台涉及两种核心审批场景：

1. **资源注册审批**（FR-026）
   - 提供方注册 API/事件/回调后，需经平台运营审批
   - 审批通过后资源变为「已上架」状态

2. **权限申请审批**（FR-027）
   - 消费方申请权限后，需经资源提供方审批
   - 审批通过后自动激活订阅关系

### 1.2 审批流程要求

**三级审批组合流程**：

```
用户提交申请 → 系统组合三级审批人 → 生成完整审批流程 → 依次执行审批

三级审批人（按顺序组合）：
第一级：全局审批人（平台运营审核）
第二级：场景审批人（业务场景审核）
第三级：资源审批人（资源提供方审核）
```

---

## 2. 核心概念

### 2.1 三级审批的定义

| 审批级别 | 名称 | 配置来源 | 适用范围 | 审批职责 |
|---------|------|---------|---------|---------|
| **第一级** | 全局审批 | `approval_flow_t` (code='global') | 所有申请 | 平台运营层面审核 |
| **第二级** | 场景审批 | `approval_flow_t` (code='场景编码') | 特定业务场景 | 业务场景层面审核 |
| **第三级** | 资源审批 | `permission_t` (need_approval + approval_flow_id) | 特定权限资源 | 资源提供方审核 |

### 2.2 关键概念：组合而非选择

```
❌ 错误理解：
根据优先级选择一个审批流程执行

✅ 正确理解：
将三级审批人按顺序串联组合成一个完整流程，依次执行
```

### 2.3 审批节点组合顺序

```
完整审批流程 = 全局审批节点 + 场景审批节点 + 资源审批节点

示例：
用户申请"支付API"权限：

节点1：系统管理员（全局审批，level='global')
    ↓ 审批通过
节点2：权限管理员（场景审批，level='scene')
    ↓ 审批通过  
节点3：支付团队负责人（资源审批，level='resource')
    ↓ 审批通过
节点4：财务管理员（资源审批，level='resource')
    ↓ 审批通过
权限申请成功
```

---

## 3. 数据库设计

### 3.1 表结构关系图

```
┌─────────────────────────────────────────────────────────────┐
│           openplatform_v2_approval_flow_t                    │
│              (审批流程模板表)                                 │
│                                                              │
│  用于创建审批流程时读取配置                                   │
│  ├─ 全局流程：code='global'                                   │
│  ├─ 场景流程：code='permission_apply'                        │
│  └─ 资源流程：id=1001（支付API审批）                          │
│                                                              │
│  关键字段：nodes (JSON) - 审批节点配置                        │
└─────────────────────────────────────────────────────────────┘
          ↓ 创建审批记录时读取（仅用于初始化）
          ↓ 之后不再关联
┌─────────────────────────────────────────────────────────────┐
│           openplatform_v2_approval_record_t                  │
│              (审批记录表)                                     │
│                                                              │
│  ✅ 直接存储完整审批流程（combined_nodes）                    │
│  ✅ 不关联审批流程表（无 flow_id 字段）                       │
│                                                              │
│  combined_nodes = [                                          │
│    {userId:'admin001', level:'global', ...},                 │
│    {userId:'perm_admin', level:'scene', ...},                │
│    {userId:'payment_leader', level:'resource', ...}          │
│  ]                                                            │
│                                                              │
│  current_node = 0（当前审批节点索引）                         │
│  status = 0（待审）                                           │
└─────────────────────────────────────────────────────────────┘
          ↓ 记录审批操作
┌─────────────────────────────────────────────────────────────┐
│           openplatform_v2_approval_log_t                     │
│              (审批日志表)                                     │
│                                                              │
│  level='global/scene/resource'                               │
│  action + comment                                            │
└─────────────────────────────────────────────────────────────┘
```

**说明**：
- `approval_flow_t` 只用于创建审批记录时读取配置
- `approval_record_t` 直接存储完整审批流程，不再关联流程表
- 审批记录数据完全独立，不受流程模板修改影响

### 3.2 审批流程模板表

```sql
CREATE TABLE `openplatform_v2_approval_flow_t` (
    `id` BIGINT(20) PRIMARY KEY,
    `name_cn` VARCHAR(100) NOT NULL COMMENT '中文名称',
    `name_en` VARCHAR(100) NOT NULL COMMENT '英文名称',
    
    -- ✅ 流程编码（唯一标识审批流程级别）
    `code` VARCHAR(50) NOT NULL UNIQUE COMMENT '流程编码：
        global=全局审批流程，
        api_register=API注册审批流程，
        event_register=事件注册审批流程，
        callback_register=回调注册审批流程，
        permission_apply=权限申请审批流程，
        payment_api=支付API审批流程（资源级自定义）',
    
    `description_cn` TEXT COMMENT '中文描述',
    `description_en` TEXT COMMENT '英文描述',
    
    -- ✅ 移除 is_default 字段，用 code='global' 标识全局审批
    
    -- ✅ 核心：审批节点配置（JSON格式）
    `nodes` JSON NOT NULL COMMENT '审批节点配置',
    
    `status` TINYINT(10) DEFAULT 1 COMMENT '状态：0=禁用, 1=启用',
    `create_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    `last_update_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    `create_by` VARCHAR(100),
    `last_update_by` VARCHAR(100),
    
    UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批流程模板表';
```

**设计说明**：

移除 `is_default` 字段，改用 `code` 字段标识审批流程级别的原因：

1. **消除冗余** - `is_default=1` 和 `code='default'` 表达相同含义，存在字段冗余
2. **命名清晰** - `code='global'` 比 `code='default'` 更能表达"全局审批"的语义
3. **简化查询** - 直接查询 `WHERE code='global'` 替代 `WHERE is_default=1`
4. **统一规范** - 所有审批流程都用 `code` 标识，统一且直观

**code 字段命名规范**：

| code 值 | 审批级别 | 适用场景 |
|---------|---------|---------|
| `global` | 全局审批 | 所有申请的第一道审核 |
| `api_register` | 场景审批 | API 注册场景 |
| `event_register` | 场景审批 | 事件注册场景 |
| `callback_register` | 场景审批 | 回调注册场景 |
| `permission_apply` | 场景审批 | 权限申请场景 |
| `{自定义}` | 资源审批 | 特定资源（如 `payment_api`） |

**nodes 字段 JSON 结构**：

```json
[
  {
    "type": "approver",           // 节点类型：approver=审批人
    "userId": "admin001",         // 审批人用户ID
    "userName": "系统管理员",      // 审批人姓名（用于显示）
    "order": 1                    // 节点顺序（1表示第一个节点）
  },
  {
    "type": "approver",
    "userId": "admin002",
    "userName": "平台管理员",
    "order": 2
  }
]
```

### 3.3 审批记录表（组合流程）

```sql
CREATE TABLE `openplatform_v2_approval_record_t` (
    `id` BIGINT(20) PRIMARY KEY,
    
    -- ✅ 核心：直接存储组合后的完整审批流程（JSON格式）
    `combined_nodes` JSON NOT NULL COMMENT '组合后的完整审批节点配置（包含所有审批人信息）',
    
    -- ✅ 移除 flow_id 字段，不再关联审批流程表
    -- 原因：
    -- 1. combined_nodes 已经包含完整的审批节点信息
    -- 2. 审批记录数据独立，不受审批流程模板修改影响
    -- 3. 查询效率更高，无需 JOIN 操作
    
    `business_type` VARCHAR(50) NOT NULL COMMENT '业务类型：api_register, event_register, permission_apply',
    `business_id` BIGINT(20) NOT NULL COMMENT '业务对象ID（订阅记录ID或资源ID）',
    `applicant_id` VARCHAR(100) NOT NULL COMMENT '申请人ID',
    `applicant_name` VARCHAR(100) COMMENT '申请人姓名',
    
    -- ✅ 审批状态和当前节点
    `status` TINYINT(10) DEFAULT 0 COMMENT '状态：0=待审, 1=已通过, 2=已拒绝, 3=已撤销',
    `current_node` INT DEFAULT 0 COMMENT '当前审批节点索引（0=第一个节点）',
    
    `create_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    `last_update_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    `create_by` VARCHAR(100),
    `last_update_by` VARCHAR(100),
    `completed_at` DATETIME(3) COMMENT '审批完成时间',
    
    KEY `idx_business` (`business_type`, `business_id`),
    KEY `idx_applicant` (`applicant_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批记录表';
```

**设计说明**：

移除 `flow_id` 字段，只保留 `combined_nodes` 字段的原因：

1. **数据完整性** - `combined_nodes` 已经包含完整的审批节点信息（userId、userName、level等）
2. **数据独立性** - 审批记录不受审批流程模板修改影响，历史审批记录保持原始配置
3. **查询效率** - 直接查询单表，无需 JOIN approval_flow_t 表，性能更高
4. **简化表结构** - 移除冗余字段，表结构更清晰简洁
5. **审计追溯** - 审批记录包含审批时的完整配置，便于历史追溯

**combined_nodes 字段示例**：

```json
[
  {
    "type": "approver",
    "userId": "admin001",
    "userName": "系统管理员",
    "order": 1,
    "level": "global"  // 第一级：全局审批
  },
  {
    "type": "approver",
    "userId": "perm_admin",
    "userName": "权限管理员",
    "order": 2,
    "level": "scene"   // 第二级：场景审批
  },
  {
    "type": "approver",
    "userId": "payment_leader",
    "userName": "支付团队负责人",
    "order": 3,
    "level": "resource" // 第三级：资源审批
  },
  {
    "type": "approver",
    "userId": "finance_admin",
    "userName": "财务管理员",
    "order": 4,
    "level": "resource" // 第三级：资源审批（第二个节点）
  }
]
```

### 3.4 设计方案对比

#### 方案一：关联审批流程表（已废弃）

```
approval_record_t 存储 flow_id → 关联 approval_flow_t 查询 nodes
```

**缺点**：
- 需要 JOIN 操作，查询效率低
- 审批流程模板修改会影响历史审批记录
- 数据分散在多个表，维护复杂
- 无法追溯审批时的原始审批人配置

#### 方案二：直接存储完整节点（当前采用）

```
approval_record_t 直接存储 combined_nodes → 无需关联其他表
```

**优点**：
1. **查询效率高** - 单表查询，无需 JOIN 操作
2. **数据独立性** - 审批记录不受审批流程模板修改影响
3. **审计追溯** - 保留审批时的完整审批人配置
4. **表结构简洁** - 移除冗余字段，结构清晰
5. **代码简化** - 不需要关联查询逻辑

**示例对比**：

```sql
-- ❌ 方案一：需要 JOIN 多表
SELECT r.id, r.current_node, f.nodes, ...
FROM approval_record_t r
JOIN approval_flow_t f1 ON r.global_flow_id = f1.id
JOIN approval_flow_t f2 ON r.scene_flow_id = f2.id
JOIN approval_flow_t f3 ON r.resource_flow_id = f3.id
WHERE r.id = 9001;

-- ✅ 方案二：单表查询，性能高
SELECT r.id, r.combined_nodes, r.current_node, ...
FROM approval_record_t r
WHERE r.id = 9001;
```

### 3.5 审批日志表

```sql
CREATE TABLE `openplatform_v2_approval_log_t` (
    `id` BIGINT(20) PRIMARY KEY,
    `record_id` BIGINT(20) NOT NULL COMMENT '审批记录ID',
    `node_index` INT NOT NULL COMMENT '审批节点索引',
    
    -- ✅ 新增：审批级别标识
    `level` VARCHAR(20) COMMENT '审批级别：global=全局, scene=场景, resource=资源',
    
    `operator_id` VARCHAR(100) NOT NULL COMMENT '操作人ID',
    `operator_name` VARCHAR(100) COMMENT '操作人姓名',
    `action` TINYINT(10) NOT NULL COMMENT '操作：0=同意, 1=拒绝, 2=撤销, 3=转交',
    `comment` TEXT COMMENT '审批意见',
    `create_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    `last_update_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    `create_by` VARCHAR(100),
    `last_update_by` VARCHAR(100),
    
    KEY `idx_record_id` (`record_id`),
    KEY `idx_level` (`level`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批操作日志表';
```

### 3.5 权限资源主表（增加审批字段）

```sql
CREATE TABLE `openplatform_v2_permission_t` (
    `id` BIGINT(20) PRIMARY KEY,
    `name_cn` VARCHAR(100) NOT NULL COMMENT '中文名称',
    `name_en` VARCHAR(100) NOT NULL COMMENT '英文名称',
    `scope` VARCHAR(100) NOT NULL UNIQUE COMMENT '权限标识',
    `resource_type` VARCHAR(20) NOT NULL COMMENT 'api, event, callback',
    `resource_id` BIGINT(20) NOT NULL COMMENT '关联的 API/Event/Callback ID',
    `category_id` BIGINT(20) NOT NULL COMMENT '所属分类ID',
    
    -- ✅ 新增：审批相关字段（从属性表移到主表）
    `need_approval` TINYINT(10) DEFAULT 1 COMMENT '是否需要审批：0=否, 1=是',
    `approval_flow_id` BIGINT(20) COMMENT '资源级审批流程ID',
    
    `status` TINYINT(10) DEFAULT 1 COMMENT '状态：0=禁用, 1=启用',
    `create_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    `last_update_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    `create_by` VARCHAR(100),
    `last_update_by` VARCHAR(100),
    
    KEY `idx_resource` (`resource_type`, `resource_id`),
    KEY `idx_category_id` (`category_id`),
    KEY `idx_scope` (`scope`),
    KEY `idx_status` (`status`),
    KEY `idx_need_approval` (`need_approval`)  -- ✅ 支持按审批需求过滤
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限资源主表';
```

**设计说明**：

将 `need_approval` 和 `approval_flow_id` 从属性表移到主表的原因：

1. **查询效率优化**：权限列表查询时可直接按 `need_approval` 过滤，避免关联属性表
2. **高频查询字段**：权限申请场景需要频繁查询"是否需要审批"的权限列表
3. **简化业务逻辑**：减少表关联，简化查询代码
4. **索引支持**：可为 `need_approval` 字段建立索引，提升查询性能

---

## 4. 三级审批配置

### 4.1 配置总览表

| 审批级别 | 配置位置 | 关键字段 | 配置方式 | 说明 |
|---------|---------|---------|---------|------|
| **第一级** | `approval_flow_t` | `code='global'` | 创建审批流程模板 | 创建审批记录时读取配置 |
| **第二级** | `approval_flow_t` | `code='场景编码'` | 创建审批流程模板 | 创建审批记录时读取配置 |
| **第三级** | `permission_t` | `need_approval`<br/>`approval_flow_id` | 权限主表直接配置 | 创建审批记录时读取配置 |

注意：
- approval_flow_t 只用于创建审批记录时读取配置
- approval_record_t 直接存储完整审批流程（combined_nodes）
- 审批记录创建后，不再依赖审批流程表

### 4.2 第一级：全局审批配置

**配置位置**：`approval_flow_t` 表

**配置示例**：

```sql
-- 创建全局审批流程（✅ 用 code='global' 标识）
INSERT INTO openplatform_v2_approval_flow_t (
    id, 
    name_cn, 
    name_en, 
    code,       -- ✅ 关键：用 'global' 标识全局审批
    nodes,      -- ✅ 关键：审批节点配置
    status,
    create_time, 
    last_update_time, 
    create_by, 
    last_update_by
) VALUES (
    1,
    '全局审批流程',
    'Global Approval Flow',
    'global',  -- ✅ 顾名思义：全局审批流程
    '[{"type":"approver","userId":"admin001","userName":"系统管理员","order":1}]',
    1,
    NOW(3),
    NOW(3),
    'system',
    'system'
);
```

**查询全局审批流程**（✅ 直接用 code 查询）：

```sql
-- 查询全局审批流程
SELECT * FROM openplatform_v2_approval_flow_t 
WHERE code = 'global' AND status = 1;
```

**注意事项**：
- 系统中只能有一个 `code='global'` 的审批流程
- 通过唯一索引 `uk_code` 保证编码唯一性

### 4.3 第二级：场景审批配置

**配置位置**：`approval_flow_t` 表

**场景编码规范**：

| 场景类型 | code 值 | 适用业务 |
|---------|---------|---------|
| API 注册 | `api_register` | 提供方注册 API |
| 事件注册 | `event_register` | 提供方注册事件 |
| 回调注册 | `callback_register` | 提供方注册回调 |
| 权限申请 | `permission_apply` | 消费方申请权限 |

**配置示例**：

```sql
-- 为权限申请场景配置审批流程
INSERT INTO openplatform_v2_approval_flow_t (
    id, 
    name_cn, 
    name_en, 
    code,       -- ✅ 关键：场景编码
    nodes,      -- ✅ 关键：审批节点配置
    status,
    create_time, 
    last_update_time, 
    create_by, 
    last_update_by
) VALUES (
    3,
    '权限申请场景审批流程',
    'Permission Apply Approval Flow',
    'permission_apply',  -- ✅ 权限申请场景
    '[{"type":"approver","userId":"perm_admin","userName":"权限管理员","order":1}]',
    1,
    NOW(3),
    NOW(3),
    'system',
    'system'
);
```

### 4.4 第三级：资源审批配置

**配置位置**：`approval_flow_t` + `permission_t`

**配置步骤**：

```
步骤1：在 approval_flow_t 中创建资源级审批流程
步骤2：在 permission_t 中直接配置两个字段：
       - need_approval = 1（是否需要审批）
       - approval_flow_id = 流程ID（审批人配置）
```

**配置示例**：

```sql
-- 步骤1：创建资源级审批流程（支付API）
INSERT INTO openplatform_v2_approval_flow_t (
    id, name_cn, name_en, code, nodes, status, ...
) VALUES (
    1001,
    '支付API审批流程',
    'Payment API Approval Flow',
    'payment_api',
    '[
        {"type":"approver","userId":"payment_leader","userName":"支付团队负责人","order":1},
        {"type":"approver","userId":"finance_admin","userName":"财务管理员","order":2}
    ]',
    1,
    NOW(3), NOW(3), 'system', 'system'
);

-- 步骤2：创建权限时直接配置审批字段（✅ 直接在主表中配置）
INSERT INTO openplatform_v2_permission_t (
    id, 
    name_cn, 
    name_en, 
    scope, 
    resource_type, 
    resource_id, 
    category_id,
    
    -- ✅ 审批配置字段（从属性表移到主表）
    need_approval,      -- 是否需要审批
    approval_flow_id,   -- 审批流程ID
    
    status, 
    create_time, 
    last_update_time, 
    create_by, 
    last_update_by
) VALUES (
    200,                    -- 权限ID
    '支付权限',              -- 中文名称
    'Payment Permission',   -- 英文名称
    'payment_write',        -- 权限标识
    'api',                  -- 资源类型
    100,                    -- 关联的API ID
    1,                      -- 分类ID
    
    -- ✅ 审批配置
    1,                      -- need_approval：需要审批
    1001,                   -- approval_flow_id：指向审批流程
    
    1,                      -- 状态：启用
    NOW(3),
    NOW(3),
    'system',
    'system'
);
```

**查询示例**（✅ 直接在主表查询，无需关联属性表）：

```sql
-- 查询需要审批的权限列表
SELECT * FROM openplatform_v2_permission_t 
WHERE need_approval = 1 AND status = 1;

-- 查询不需要审批的权限列表
SELECT * FROM openplatform_v2_permission_t 
WHERE need_approval = 0 AND status = 1;

-- 查询某权限的审批流程配置
SELECT 
    p.id,
    p.name_cn,
    p.need_approval,
    p.approval_flow_id,
    f.name_cn AS flow_name,
    f.nodes AS flow_nodes
FROM openplatform_v2_permission_t p
LEFT JOIN openplatform_v2_approval_flow_t f ON p.approval_flow_id = f.id
WHERE p.id = 200;
```

---

## 5. 组合逻辑

### 5.1 组合流程图

```
用户提交权限申请
    ↓
┌───────────────────────────────────────────────────────┐
│ 系统组合三级审批流程                                    │
├───────────────────────────────────────────────────────┤
│                                                        │
│ 第一级：全局审批                                        │
│   SELECT * FROM approval_flow_t WHERE code='global'   │
│   结果：nodes=[系统管理员]                              │
│                                                        │
├───────────────────────────────────────────────────────┤
│                                                        │
│ 第二级：场景审批                                        │
│   SELECT * FROM approval_flow_t                       │
│   WHERE code='permission_apply'                       │
│   结果：nodes=[权限管理员]                              │
│                                                        │
├───────────────────────────────────────────────────────┤
│                                                        │
│ 第三级：资源审批                                        │
│   SELECT need_approval, approval_flow_id              │
│   FROM permission_t WHERE id=200                      │
│   结果：need_approval=1, flow_id=1001                 │
│                                                        │
│   SELECT * FROM approval_flow_t WHERE id=1001         │
│   结果：nodes=[支付负责人, 财务管理员]                  │
│                                                        │
├───────────────────────────────────────────────────────┤
│                                                        │
│ 组合结果：                                              │
│   combined_nodes = [                                  │
│     {order:1, level:'global', userId:'admin001'},     │
│     {order:2, level:'scene', userId:'perm_admin'},    │
│     {order:3, level:'resource', userId:'payment_...'},│
│     {order:4, level:'resource', userId:'finance_...'} │
│   ]                                                    │
│                                                        │
└───────────────────────────────────────────────────────┘
    ↓
创建审批记录（✅ 直接存储完整流程）
INSERT INTO approval_record_t (
    combined_nodes = 组合后的完整JSON,
    -- ✅ 不存储 flow_id，不关联流程表
    business_type = 'permission_apply',
    business_id = 订阅记录ID,
    applicant_id = 申请人ID,
    applicant_name = 申请人姓名,
    current_node = 0,
    status = 0（待审）
)

说明：
1. combined_nodes 包含所有审批节点的完整信息
2. 无需存储 global_flow_id、scene_flow_id、resource_flow_id
3. 审批记录创建后，不再依赖审批流程表
4. 查询审批节点直接从 combined_nodes 解析即可
```

### 5.2 组合逻辑伪代码

```java
// 组合审批流程
List<ApprovalNodeDto> combinedNodes = composeApprovalFlow(permissionId, "permission_apply");

// 创建审批记录
ApprovalRecord record = new ApprovalRecord();
record.setId(idGenerator.nextId());

// ✅ 直接存储完整审批节点配置
record.setCombinedNodes(serializeNodes(combinedNodes));

// ✅ 不存储 flow_id 字段，不关联审批流程表
// 原因：combined_nodes 已包含完整信息，审批记录数据独立

record.setBusinessType("permission_apply");
record.setBusinessId(subscription.getId());
record.setApplicantId(applicantId);
record.setApplicantName(applicantName);
record.setStatus(0);  // 待审
record.setCurrentNode(0);  // 从第一个节点开始

recordMapper.insert(record);
```

**关键说明**：
- 只需要存储 `combined_nodes`，不需要存储 `flow_id`
- `combined_nodes` 包含完整的审批节点信息（userId、userName、level、order）
- 审批记录创建后，数据完全独立，不再依赖审批流程表

**组合审批流程方法**：

```java
public List<ApprovalNodeDto> composeApprovalFlow(Long permissionId, String businessType) {
    
    List<ApprovalNodeDto> combinedNodes = new ArrayList<>();
    int order = 1;
    
    // 第一级：全局审批节点
    ApprovalFlow globalFlow = flowMapper.selectByCode("global");  // ✅ 直接用 code 查询
    List<ApprovalNodeDto> globalNodes = parseNodes(globalFlow.getNodes());
    for (ApprovalNodeDto node : globalNodes) {
        node.setOrder(order++);
        node.setLevel("global");  // ✅ 标记审批级别
        combinedNodes.add(node);
    }
    
    // 第二级：场景审批节点
    ApprovalFlow sceneFlow = flowMapper.selectByCode(businessType);
    List<ApprovalNodeDto> sceneNodes = parseNodes(sceneFlow.getNodes());
    for (ApprovalNodeDto node : sceneNodes) {
        node.setOrder(order++);
        node.setLevel("scene");  // ✅ 标记审批级别
        combinedNodes.add(node);
    }
    
    // 第三级：资源审批节点
    // ✅ 直接从主表查询，无需查询属性表
    Permission permission = permissionMapper.selectById(permissionId);
    
    if (permission.getNeedApproval() == 1) {  // ✅ 直接判断主表字段
        Long flowId = permission.getApprovalFlowId();  // ✅ 直接获取主表字段
        if (flowId != null) {
            ApprovalFlow resourceFlow = flowMapper.selectById(flowId);
            List<ApprovalNodeDto> resourceNodes = parseNodes(resourceFlow.getNodes());
            for (ApprovalNodeDto node : resourceNodes) {
                node.setOrder(order++);
                node.setLevel("resource");
                combinedNodes.add(node);
            }
        }
    }
    
    return combinedNodes;
}
```

**查询逻辑变化**：

```java
// ❌ 之前的查询方式
ApprovalFlow globalFlow = flowMapper.selectDefaultFlow();
// SQL: WHERE is_default = 1

// ✅ 现在的查询方式
ApprovalFlow globalFlow = flowMapper.selectByCode("global");
// SQL: WHERE code = 'global'
```

---

## 6. 审批执行流程

### 6.1 审批状态流转图

```
初始状态：
approval_record_t.current_node = 0
approval_record_t.status = 0（待审）
approval_record_t.combined_nodes = [节点0, 节点1, 节点2, 节点3]

审批执行过程：

┌─────────────────────────────────────────┐
│ 节点0（全局审批）                        │
│ userId=admin001                          │
│ 系统管理员登录 → 点击"同意"              │
├─────────────────────────────────────────┤
│ 系统处理：                               │
│ 1. 解析 combined_nodes                  │
│ 2. 判断 current_node=0 是否最后节点     │
│    0 >= nodes.size()-1 (0>=3)？否       │
│ 3. 记录审批日志：                        │
│    INSERT approval_log_t (              │
│      level='global',                    │
│      action=0（同意）                   │
│    )                                    │
│ 4. 更新审批记录：                        │
│    current_node=1                       │
└─────────────────────────────────────────┘
    ↓
┌─────────────────────────────────────────┐
│ 节点1（场景审批）                        │
│ userId=perm_admin                        │
│ 权限管理员登录 → 点击"同意"              │
├─────────────────────────────────────────┤
│ 系统处理：                               │
│ 1. current_node=1                       │
│ 2. 1 >= 3？否                           │
│ 3. 记录审批日志（level='scene'）        │
│ 4. 更新审批记录：current_node=2         │
└─────────────────────────────────────────┘
    ↓
┌─────────────────────────────────────────┐
│ 节点2（资源审批）                        │
│ userId=payment_leader                    │
│ 支付负责人登录 → 点击"同意"              │
├─────────────────────────────────────────┤
│ 系统处理：                               │
│ 1. current_node=2                       │
│ 2. 2 >= 3？否                           │
│ 3. 记录审批日志（level='resource'）     │
│ 4. 更新审批记录：current_node=3         │
└─────────────────────────────────────────┘
    ↓
┌─────────────────────────────────────────┐
│ 节点3（资源审批）                        │
│ userId=finance_admin                     │
│ 财务管理员登录 → 点击"同意"              │
├─────────────────────────────────────────┤
│ 系统处理：                               │
│ 1. current_node=3                       │
│ 2. 3 >= 3？是 ✅                         │
│ 3. 记录审批日志（level='resource'）     │
│ 4. 更新审批记录：                        │
│    status=1（已通过）                   │
│    completed_at=NOW()                   │
│ 5. 更新订阅记录：                        │
│    status=1（已授权）                   │
└─────────────────────────────────────────┘
    ↓
审批完成，权限激活
```

### 6.2 审批操作判断逻辑

```java
// ApprovalEngine.approve() 方法核心逻辑

public ApprovalRecord approve(Long recordId, String operatorId, String comment) {
    
    // 1. 查询审批记录
    ApprovalRecord record = recordMapper.selectById(recordId);
    
    // 2. ✅ 直接从 combined_nodes 解析审批节点（无需关联流程表）
    List<ApprovalNodeDto> nodes = parseNodes(record.getCombinedNodes());
    
    // 3. 获取当前节点索引
    int currentNodeIndex = record.getCurrentNode();
    
    // 4. 记录审批日志
    ApprovalLog log = new ApprovalLog();
    log.setRecordId(recordId);
    log.setNodeIndex(currentNodeIndex);
    log.setLevel(nodes.get(currentNodeIndex).getLevel());  // 直接从节点获取 level
    log.setOperatorId(operatorId);
    log.setAction(0);  // 同意
    log.setComment(comment);
    logMapper.insert(log);
    
    // 5. 判断是否最后一个节点
    if (currentNodeIndex >= nodes.size() - 1) {
        // 审批通过
        record.setStatus(1);
        record.setCompletedAt(new Date());
        // 更新订阅状态...
    } else {
        // 进入下一节点
        record.setCurrentNode(currentNodeIndex + 1);
    }
    
    // 6. 更新审批记录
    recordMapper.update(record);
    
    return record;
}
```

**关键说明**：
- 直接从 `record.getCombinedNodes()` 解析审批节点
- 不需要查询 `approval_flow_t` 表
- 所有审批节点信息都在一条记录中，查询效率高

### 6.3 拒绝审批处理

```java
public ApprovalRecord reject(Long recordId, String operatorId, String reason) {
    
    // 1. 查询审批记录
    ApprovalRecord record = recordMapper.selectById(recordId);
    
    // 2. 解析审批节点
    List<ApprovalNodeDto> nodes = parseNodes(record.getCombinedNodes());
    int currentNodeIndex = record.getCurrentNode();
    
    // 3. 记录审批日志
    ApprovalLog log = new ApprovalLog();
    log.setRecordId(recordId);
    log.setNodeIndex(currentNodeIndex);
    log.setLevel(nodes.get(currentNodeIndex).getLevel());
    log.setOperatorId(operatorId);
    log.setAction(1);  // 拒绝
    log.setComment(reason);
    logMapper.insert(log);
    
    // 4. ✅ 拒绝审批 → 直接结束流程
    record.setStatus(2);  // 已拒绝
    record.setCompletedAt(new Date());
    
    // 5. 更新订阅状态
    if ("permission_apply".equals(record.getBusinessType())) {
        Subscription subscription = subscriptionMapper.selectById(record.getBusinessId());
        subscription.setStatus(2);  // 已拒绝
        subscriptionMapper.update(subscription);
    }
    
    // 6. 更新审批记录
    recordMapper.update(record);
    
    return record;
}
```

---

## 7. 实现方案

### 7.1 ApprovalRecord 实体类

```java
@Data
public class ApprovalRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    
    /**
     * ✅ 核心：组合后的审批节点配置（JSON格式）
     * 
     * 直接存储完整的审批节点信息，不关联审批流程表
     * 包含所有审批人的 userId、userName、level、order 信息
     * 
     * 格式示例：
     * [
     *   {"type":"approver","userId":"admin001","userName":"系统管理员","order":1,"level":"global"},
     *   {"type":"approver","userId":"perm_admin","userName":"权限管理员","order":2,"level":"scene"},
     *   {"type":"approver","userId":"payment_leader","userName":"支付团队负责人","order":3,"level":"resource"}
     * ]
     */
    private String combinedNodes;
    
    // ✅ 移除 flow_id 字段，不关联审批流程表
    
    private String businessType;
    private Long businessId;
    private String applicantId;
    private String applicantName;
    private Integer status;
    private Integer currentNode;
    private Date createTime;
    private Date lastUpdateTime;
    private Date completedAt;
    private String createBy;
    private String lastUpdateBy;
}
```

### 7.3 ApprovalNodeDto 增强

```java
@Data
public class ApprovalNodeDto {
    
    private String type;      // 节点类型
    private String userId;    // 审批人ID
    private String userName;  // 审批人姓名
    private Integer order;    // 节点顺序
    
    // ✅ 新增：审批级别标识
    private String level;     // global, scene, resource
    
    // 节点状态（用于显示）
    private Integer status;   // null=未处理, 0=待审批, 1=已通过, 2=已拒绝
}
```

### 7.4 ApprovalFlowComposer 工具类

**核心职责**：组合三级审批流程

```java
@Component
public class ApprovalFlowComposer {
    
    @Autowired
    private ApprovalFlowMapper flowMapper;
    
    @Autowired
    private PermissionMapper permissionMapper;
    
    /**
     * 为权限申请组合审批流程
     * 
     * @param permissionId 权限ID
     * @param businessType 业务类型
     * @return 组合后的审批节点列表
     */
    public List<ApprovalNodeDto> composeApprovalFlow(Long permissionId, String businessType) {
        
        List<ApprovalNodeDto> combinedNodes = new ArrayList<>();
        int order = 1;
        
        // 第一级：全局审批
        List<ApprovalNodeDto> globalNodes = getGlobalApprovalNodes();
        for (ApprovalNodeDto node : globalNodes) {
            node.setOrder(order++);
            node.setLevel("global");
            combinedNodes.add(node);
        }
        
        // 第二级：场景审批
        List<ApprovalNodeDto> sceneNodes = getSceneApprovalNodes(businessType);
        for (ApprovalNodeDto node : sceneNodes) {
            node.setOrder(order++);
            node.setLevel("scene");
            combinedNodes.add(node);
        }
        
        // 第三级：资源审批
        List<ApprovalNodeDto> resourceNodes = getResourceApprovalNodes(permissionId);
        for (ApprovalNodeDto node : resourceNodes) {
            node.setOrder(order++);
            node.setLevel("resource");
            combinedNodes.add(node);
        }
        
        return combinedNodes;
    }
    
    /**
     * 获取全局审批节点
     */
    private List<ApprovalNodeDto> getGlobalApprovalNodes() {
        ApprovalFlow globalFlow = flowMapper.selectByCode("global");  // ✅ 用 code 查询
        if (globalFlow == null || globalFlow.getStatus() != 1) {
            return Collections.emptyList();
        }
        return parseNodes(globalFlow.getNodes());
    }
    
    /**
     * 获取场景审批节点
     */
    private List<ApprovalNodeDto> getSceneApprovalNodes(String businessType) {
        ApprovalFlow sceneFlow = flowMapper.selectByCode(businessType);
        if (sceneFlow == null || sceneFlow.getStatus() != 1) {
            return Collections.emptyList();
        }
        return parseNodes(sceneFlow.getNodes());
    }
    
    /**
     * 获取资源审批节点
     */
    private List<ApprovalNodeDto> getResourceApprovalNodes(Long permissionId) {
        Permission permission = permissionMapper.selectById(permissionId);
        if (permission == null || permission.getNeedApproval() != 1) {
            return Collections.emptyList();
        }
        
        Long flowId = permission.getApprovalFlowId();
        if (flowId == null) {
            return Collections.emptyList();
        }
        
        ApprovalFlow resourceFlow = flowMapper.selectById(flowId);
        if (resourceFlow == null || resourceFlow.getStatus() != 1) {
            return Collections.emptyList();
        }
        
        return parseNodes(resourceFlow.getNodes());
    }
    
    /**
     * 解析 JSON 格式的审批节点配置
     */
    private List<ApprovalNodeDto> parseNodes(String nodesJson) {
        return JSON.parseArray(nodesJson, ApprovalNodeDto.class);
    }
}
```

### 7.5 ApprovalFlowMapper 接口

```java
public interface ApprovalFlowMapper {
    
    /**
     * 根据流程编码查询审批流程
     * 
     * ✅ 统一使用此方法查询所有审批流程（全局、场景、资源）
     */
    ApprovalFlow selectByCode(@Param("code") String code);
    
    /**
     * 根据ID查询审批流程
     */
    ApprovalFlow selectById(@Param("id") Long id);
    
    // ... 其他方法
}
```

**使用示例**：

```java
// 查询全局审批流程
ApprovalFlow globalFlow = flowMapper.selectByCode("global");

// 查询场景审批流程
ApprovalFlow sceneFlow = flowMapper.selectByCode("permission_apply");

// 查询资源审批流程
ApprovalFlow resourceFlow = flowMapper.selectByCode("payment_api");
// 或通过ID查询
ApprovalFlow resourceFlow = flowMapper.selectById(1001L);
```
```

### 7.6 PermissionService 集成

```java
@Service
public class PermissionService {
    
    @Autowired
    private ApprovalFlowComposer approvalFlowComposer;
    
    @Autowired
    private ApprovalEngine approvalEngine;
    
    @Autowired
    private ApprovalFlowMapper flowMapper;
    
    /**
     * 申请权限
     */
    public PermissionSubscribeResponse subscribePermission(Long appId, Long permissionId) {
        
        // 1. 创建订阅记录
        Subscription subscription = new Subscription();
        subscription.setId(idGenerator.nextId());
        subscription.setAppId(appId);
        subscription.setPermissionId(permissionId);
        subscription.setStatus(0);  // 待审
        subscriptionMapper.insert(subscription);
        
        // 2. ✅ 组合三级审批流程
        List<ApprovalNodeDto> combinedNodes = approvalFlowComposer.composeApprovalFlow(
            permissionId, 
            "permission_apply"
        );
        
        // 3. 创建审批记录
        ApprovalRecord record = new ApprovalRecord();
        record.setId(idGenerator.nextId());
        record.setCombinedNodes(serializeNodes(combinedNodes));  // ✅ 存储完整审批节点配置
        record.setBusinessType("permission_apply");
        record.setBusinessId(subscription.getId());
        record.setApplicantId(currentUserId);
        record.setApplicantName(currentUserName);
        record.setStatus(0);  // 待审
        record.setCurrentNode(0);  // 从第一个节点开始
        
        // ✅ 不记录 flow_id 字段
        // 原因：combined_nodes 已包含完整信息，审批记录数据独立
        
        recordMapper.insert(record);
        
        return response;
    }
    
    /**
     * 序列化审批节点为 JSON
     */
    private String serializeNodes(List<ApprovalNodeDto> nodes) {
        return JSON.toJSONString(nodes);
    }
}
```

---

## 8. 示例场景

### 8.1 完整审批流程示例

**场景**：用户申请"支付API"权限

**配置数据**：

```
全局审批流程：
  id=1, code='global'
  nodes=[系统管理员]

场景审批流程：
  id=3, code='permission_apply'
  nodes=[权限管理员]

资源审批流程：
  id=1001
  nodes=[支付负责人, 财务管理员]
  
权限主表配置：
  permission_id=200
  need_approval=1
  approval_flow_id=1001
```

**组合后的审批流程**：

```
节点1：系统管理员（全局审批）
节点2：权限管理员（场景审批）
节点3：支付团队负责人（资源审批）
节点4：财务管理员（资源审批）
```

**审批记录创建（✅ 直接存储完整流程）**：

```sql
-- 创建审批记录（✅ 直接存储完整流程）
INSERT INTO openplatform_v2_approval_record_t (
    id,
    combined_nodes,     -- ✅ 完整审批节点配置（JSON）
    business_type,      -- 业务类型
    business_id,        -- 业务对象ID
    applicant_id,       -- 申请人ID
    applicant_name,     -- 申请人姓名
    status,             -- 审批状态
    current_node,       -- 当前节点索引
    create_time,
    last_update_time
) VALUES (
    9001,
    '[  -- ✅ 完整审批流程（包含所有审批人）
        {"type":"approver","userId":"admin001","userName":"系统管理员","order":1,"level":"global"},
        {"type":"approver","userId":"perm_admin","userName":"权限管理员","order":2,"level":"scene"},
        {"type":"approver","userId":"payment_leader","userName":"支付团队负责人","order":3,"level":"resource"},
        {"type":"approver","userId":"finance_admin","userName":"财务管理员","order":4,"level":"resource"}
    ]',
    'permission_apply',
    5001,
    'user001',
    '张三',
    0,  -- 待审
    0,  -- 第一个节点
    NOW(3),
    NOW(3)
);

-- 注意：
-- ✅ combined_nodes 包含完整的审批节点信息
-- ✅ 不存储 global_flow_id、scene_flow_id、resource_flow_id
-- ✅ 审批记录创建后，数据完全独立
```

**审批执行**：

```
系统管理员审批 → 同意 → current_node=1
权限管理员审批 → 同意 → current_node=2
支付负责人审批 → 同意 → current_node=3
财务管理员审批 → 同意 → status=1（通过）→ 订阅激活
```

### 8.2 数据查询示例

**查询审批记录（✅ 单表查询，无需 JOIN）**：

```sql
-- ✅ 查询审批记录（单表查询，无需 JOIN）
SELECT 
    r.id,
    r.combined_nodes,  -- ✅ 完整审批流程（包含所有审批人信息）
    r.current_node,    -- 当前节点索引
    r.status,
    r.business_type,
    r.business_id
FROM approval_record_t r
WHERE r.id = 9001;

-- ✅ 解析审批节点（直接从 combined_nodes 获取）
-- JSON 示例：
-- [
--   {"userId":"admin001","userName":"系统管理员","order":1,"level":"global"},
--   {"userId":"perm_admin","userName":"权限管理员","order":2,"level":"scene"},
--   {"userId":"payment_leader","userName":"支付团队负责人","order":3,"level":"resource"}
-- ]

-- ❌ 之前的查询方式（需要 JOIN 流程表）
-- SELECT r.*, f.nodes FROM approval_record_t r JOIN approval_flow_t f ...
```

**查询审批日志**：

```sql
SELECT 
    l.node_index,
    l.level,  -- 审批级别
    l.operator_name,
    CASE l.action 
        WHEN 0 THEN '同意' 
        WHEN 1 THEN '拒绝' 
    END AS action_name,
    l.comment,
    l.create_time
FROM approval_log_t l
WHERE l.record_id = 9001
ORDER BY l.node_index;
```

---

## 9. 总结

### 9.1 核心设计要点

| 设计要点 | 说明 |
|---------|------|
| **组合而非选择** | 三级审批人按顺序串联成一个完整流程 |
| **combined_nodes** | 存储组合后的完整审批节点JSON |
| **level字段** | 标记节点属于哪一级（global/scene/resource） |
| **current_node** | 当前审批节点索引（0, 1, 2...） |
| **审批顺序** | 全局 → 场景 → 资源 |
| **拒绝处理** | 拒绝后直接结束流程，不进入下一节点 |
| **code字段** | 统一用code标识审批流程级别（global/场景编码/资源编码） |

### 9.2 字段优化说明

**移除 `is_default` 字段的优化效果**：

| 优化维度 | 优化前 | 优化后 | 改进效果 |
|---------|--------|--------|---------|
| **字段设计** | `is_default` + `code` 双字段 | 仅 `code` 单字段 | 消除字段冗余 |
| **语义清晰度** | `is_default=1` 表示全局审批 | `code='global'` 表示全局审批 | 命名更直观 |
| **查询方式** | `WHERE is_default=1` | `WHERE code='global'` | 查询更统一 |
| **方法调用** | `selectDefaultFlow()` | `selectByCode("global")` | 接口更统一 |
| **唯一性保证** | 应用层逻辑控制 | 数据库唯一索引 `uk_code` | 更安全可靠 |

### 9.2 实施步骤

1. ✅ 创建审批流程模板数据（全局、场景、资源）
2. ✅ 在权限主表中配置审批字段（need_approval、approval_flow_id）
3. ✅ 实现 ApprovalFlowComposer 组合逻辑
4. ✅ 修改 PermissionService 使用组合流程
5. ✅ 修改 ApprovalEngine 支持组合审批
6. ✅ 测试完整审批流程

---

## 10. 相关文档

- [需求文档](spec.md) - FR-025, FR-026, FR-027
- [数据库设计](plan-db.md) - 审批相关表结构
- [API设计](plan-api.md) - 审批管理接口
- [ADR-001](ADR-001.md) - 审批流程设计决策

---

## 11. 版本更新记录

### v2.2.0 (2026-04-24)

**优化内容**：移除审批记录表中的 `flow_id` 字段，只保留 `combined_nodes` 字段

**优化原因**：
1. **数据完整性** - `combined_nodes` 已经包含完整的审批节点信息（userId、userName、level等）
2. **数据独立性** - 审批记录不受审批流程模板修改影响，历史审批记录保持原始配置
3. **查询效率** - 直接查询单表，无需 JOIN approval_flow_t 表，性能更高
4. **简化表结构** - 移除冗余字段，表结构更清晰简洁
5. **审计追溯** - 审批记录包含审批时的完整配置，便于历史追溯

**主要变更**：

| 变更项 | 变更前 | 变更后 |
|--------|--------|--------|
| 审批记录表字段 | `global_flow_id`, `scene_flow_id`, `resource_flow_id`, `flow_id` | 仅保留 `combined_nodes` |
| 查询审批节点 | 需要 JOIN approval_flow_t 表 | 直接从 combined_nodes 解析 |
| 审批记录创建 | 存储 flow_id + combined_nodes | 仅存储 combined_nodes |
| 数据独立性 | 受审批流程模板修改影响 | 数据完全独立 |

**影响范围**：
- ✅ 数据库表结构：`openplatform_v2_approval_record_t`
- ✅ 查询逻辑：审批记录查询不再需要 JOIN 流程表
- ✅ 业务代码：`ApprovalRecord` 实体类，`PermissionService`，`ApprovalEngine`
- ✅ 配置示例：审批记录创建SQL

**向后兼容性**：
- ❌ 不兼容旧版本数据，需要数据迁移
- ❌ 需要更新所有使用 `flow_id` 字段的查询和代码

### v2.1.0 (2026-04-24)

**优化内容**：移除 `is_default` 字段，改用 `code` 字段标识审批流程级别

**优化原因**：
1. **消除冗余** - `is_default=1` 和 `code='default'` 表达相同含义，存在字段冗余
2. **语义清晰** - `code='global'` 比 `is_default=1` 更能表达"全局审批"的语义
3. **统一规范** - 所有审批流程都用 `code` 标识，查询和接口调用更统一
4. **安全可靠** - 通过数据库唯一索引保证编码唯一性

**主要变更**：

| 变更项 | 变更前 | 变更后 |
|--------|--------|--------|
| 表结构 | `is_default TINYINT` 字段 | 移除该字段 |
| 全局流程标识 | `is_default=1, code='default'` | `code='global'` |
| 查询全局流程 | `WHERE is_default=1` | `WHERE code='global'` |
| Mapper方法 | `selectDefaultFlow()` | `selectByCode("global")` |

**影响范围**：
- ✅ 数据库表结构：`openplatform_v2_approval_flow_t`
- ✅ 查询逻辑：全局审批流程查询
- ✅ 业务代码：`ApprovalFlowMapper`, `ApprovalFlowComposer`, `PermissionService`
- ✅ 配置示例：全局审批流程配置SQL

**向后兼容性**：
- ❌ 不兼容旧版本数据，需要数据迁移
- ❌ 需要更新所有使用 `is_default` 字段的查询和代码