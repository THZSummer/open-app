# 审批流程设计方案

> **版本**: 2.6.0  
> **创建时间**: 2026-04-24  
> **状态**: 设计完成

---

## 📋 目录

- [1. 概述](#1-概述)
- [2. 核心概念](#2-核心概念)
- [3. 审批流程配置方案](#3-审批流程配置方案)
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
第一级：资源审批人（资源提供方审核）
第二级：场景审批人（业务场景审核）
第三级：全局审批人（平台运营审核）
```

---

## 2. 核心概念

### 2.1 三级审批的定义

| 审批级别 | 名称 | 配置来源 | 配置时机 | 配置位置 | 配置权限 | 适用范围 |
|---------|------|---------|---------|---------|---------|---------|
| **第一级** | 资源审批 | `permission_t` (resource_nodes) | 资源注册/更新时 | 资源详情页面 | 资源提供方 | 特定权限资源 |
| **第二级** | 场景审批 | `approval_flow_t` (code='场景编码') | 业务场景需要时 | 平台管理后台 | 平台运营管理员 | 特定业务场景 |
| **第三级** | 全局审批 | `approval_flow_t` (code='global') | 平台初始化/调整时 | 平台管理后台 | 平台运营管理员 | 所有申请 |

### 2.2 关键概念：组合而非选择

```
❌ 错误理解：
根据优先级选择一个审批流程执行

✅ 正确理解：
将三级审批人按顺序串联组合成一个完整流程，依次执行
```

### 2.3 审批节点组合顺序

```
完整审批流程 = 资源审批节点 + 场景审批节点 + 全局审批节点

组合顺序（从具体到一般）：
1. 第一级：资源审批（资源提供方审核）
2. 第二级：场景审批（业务场景审核）
3. 第三级：全局审批（平台运营审核）

示例：
用户申请"支付API"权限：

节点1：支付团队负责人（资源审批，level='resource')
    ↓ 审批通过
节点2：财务管理员（资源审批，level='resource')
    ↓ 审批通过
节点3：权限管理员（场景审批，level='scene')
    ↓ 审批通过
节点4：系统管理员（全局审批，level='global')
    ↓ 审批通过
权限申请成功
```

**设计原理**：

审批顺序从具体到一般，确保：
1. **资源审批**（第一级）：资源提供方最了解资源，先审核是否授权
2. **场景审批**（第二级）：业务场景层面审核是否符合业务规范
3. **全局审批**（第三级）：平台运营最终审核，确保符合平台规范

---

## 3. 数据库设计

### 3.1 表结构关系图

```
┌─────────────────────────────────────────────────────────────┐
│           openplatform_v2_approval_flow_t                    │
│              (审批流程模板表)                                 │
│                                                              │
│  用于全局审批和场景审批配置                                   │
│  ├─ 全局流程：code='global'                                   │
│  └─ 场景流程：code='permission_apply'                        │
│                                                              │
│  ❌ 不再用于资源审批配置                                      │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│           openplatform_v2_permission_t                       │
│              (权限资源主表)                                   │
│                                                              │
│  ✅ 直接存储资源级审批节点配置                                │
│  resource_nodes = [                                          │
│    {"userId":"payment_leader","userName":"支付团队负责人"},  │
│    {"userId":"finance_admin","userName":"财务管理员"}        │
│  ]                                                            │
│                                                              │
│  ✅ 不关联审批流程表（无 approval_flow_id 字段）              │
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
    
    -- ✅ 审批节点配置（使用 VARCHAR 存储 JSON 字符串）
    `nodes` VARCHAR(2000) NOT NULL COMMENT '审批节点配置（JSON格式字符串）',
    
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

**注意**：字段使用 VARCHAR 类型存储 JSON 格式字符串，由应用层负责解析

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
    
    -- ✅ 组合后的完整审批节点配置（使用 VARCHAR 存储 JSON 字符串）
    -- 长度设为 4000，因为可能包含多级审批节点（最多约 10-15 个节点）
    `combined_nodes` VARCHAR(4000) NOT NULL COMMENT '组合后的完整审批节点配置（JSON格式字符串）',
    
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

**注意**：字段使用 VARCHAR 类型存储 JSON 格式字符串，由应用层负责解析

```json
[
  {
    "type": "approver",
    "userId": "payment_leader",
    "userName": "支付团队负责人",
    "order": 1,
    "level": "resource"  // ✅ 第一级：资源审批
  },
  {
    "type": "approver",
    "userId": "finance_admin",
    "userName": "财务管理员",
    "order": 2,
    "level": "resource"  // ✅ 第一级：资源审批（第二个节点）
  },
  {
    "type": "approver",
    "userId": "perm_admin",
    "userName": "权限管理员",
    "order": 3,
    "level": "scene"     // ✅ 第二级：场景审批
  },
  {
    "type": "approver",
    "userId": "admin001",
    "userName": "系统管理员",
    "order": 4,
    "level": "global"    // ✅ 第三级：全局审批
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
    
    -- ✅ 审批相关字段（直接存储审批节点）
    `need_approval` TINYINT(10) DEFAULT 1 COMMENT '是否需要审批：0=否, 1=是',
    
    -- ✅ 资源级审批节点配置（使用 VARCHAR 存储 JSON 字符串）
    `resource_nodes` VARCHAR(2000) COMMENT '资源级审批节点配置（JSON格式字符串）',
    
    `status` TINYINT(10) DEFAULT 1 COMMENT '状态：0=禁用, 1=启用',
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
```

**设计说明**：

将 `approval_flow_id` 改为 `resource_nodes` 直接存储审批节点配置，原因：

1. **数据独立性** - 权限审批配置不受审批流程模板修改影响
2. **查询效率** - 直接从权限表读取审批节点，无需 JOIN approval_flow_t 表
3. **配置灵活性** - 每个权限可以有独特的审批节点配置，不依赖预设流程
4. **表结构简化** - 移除关联字段，减少表关联复杂度
5. **一致性设计** - 与审批记录表的 combined_nodes 设计理念一致

**resource_nodes 字段示例**：

**注意**：字段使用 VARCHAR 类型存储 JSON 格式字符串，由应用层负责解析

```json
[
  {
    "type": "approver",
    "userId": "payment_leader",
    "userName": "支付团队负责人",
    "order": 1
  },
  {
    "type": "approver",
    "userId": "finance_admin",
    "userName": "财务管理员",
    "order": 2
  }
]
```

### 3.6 字段类型说明

#### JSON 数据存储方式

本系统使用 **VARCHAR** 类型存储 JSON 格式的数据，而不使用数据库原生的 JSON 类型，原因如下：

1. **数据库兼容性**
   - VARCHAR 在所有数据库版本中都支持
   - JSON 类型在 MySQL 5.6 及以下版本不支持
   - 便于数据库迁移和升级

2. **性能考虑**
   - VARCHAR 在简单查询场景下性能更好
   - 避免数据库 JSON 解析开销
   - JSON 解析由应用层处理，更灵活

3. **存储可控**
   - VARCHAR 有明确的长度限制
   - 避免数据过长导致的存储问题
   - 更容易进行数据验证

4. **应用层处理**
   - Java 应用层使用 Jackson/Gson 解析 JSON
   - 解析逻辑统一，便于维护
   - 可以添加自定义序列化/反序列化逻辑

#### 字段长度设计

| 字段 | 长度 | 说明 |
|------|------|------|
| `nodes` | VARCHAR(2000) | 单个审批流程节点，通常 2-5 个节点 |
| `combined_nodes` | VARCHAR(4000) | 组合后的完整流程，可能 10-15 个节点 |
| `resource_nodes` | VARCHAR(2000) | 资源级审批节点，通常 1-3 个节点 |

**JSON 字符串示例**（约 500 字符）：

```json
[
  {"type":"approver","userId":"payment_leader","userName":"支付团队负责人","order":1},
  {"type":"approver","userId":"finance_admin","userName":"财务管理员","order":2}
]
```

**应用层处理代码**：

```java
// 序列化：Java 对象 → JSON 字符串
String nodesJson = objectMapper.writeValueAsString(nodes);

// 反序列化：JSON 字符串 → Java 对象
List<ApprovalNodeDto> nodes = objectMapper.readValue(
    nodesJson, 
    new TypeReference<List<ApprovalNodeDto>>() {}
);
```

---

## 4. 审批流程配置方案

### 4.1 三级审批配置总览

三级审批流程都支持**动态可配置**，不同级别的配置权限和配置位置不同：

| 审批级别 | 配置时机 | 配置位置 | 配置权限 | 配置方式 |
|---------|---------|---------|---------|---------|
| **全局审批** | 平台初始化/运营调整时 | 平台管理后台 - 审批流程管理 | 平台运营管理员 | 创建/编辑审批流程模板 |
| **场景审批** | 业务场景需要时 | 平台管理后台 - 审批流程管理 | 平台运营管理员 | 创建/编辑审批流程模板 |
| **资源审批** | 资源注册/更新时 | 资源详情页面 - 审批配置 | 资源提供方 | 配置审批节点 |

---

### 4.2 全局审批配置

#### 配置时机
- **平台初始化时**：设置系统默认的全局审批流程
- **运营调整时**：当平台运营策略变更时调整审批流程

#### 配置位置
- **界面位置**：平台管理后台 → 审批流程管理 → 全局审批流程
- **数据库表**：`approval_flow_t`，`code='global'`

#### 配置权限
- **权限要求**：平台运营管理员（超级管理员）
- **权限控制**：只有拥有 `admin:approval_flow:manage` 权限的用户可以配置

#### 配置界面示例

```
审批流程管理 > 全局审批流程

审批流程名称：[全局审批流程] _______________
流程编码：global（系统固定，不可修改）

审批节点配置：
  ┌───────────────────────────────────────────┐
  │ 节点1：                                   │
  │   审批人：[系统管理员] [选择用户 ▼]       │
  │   用户ID：admin001                        │
  │                                           │
  │ 节点2：                                   │
  │   审批人：[平台管理员] [选择用户 ▼]       │
  │   用户ID：admin002                        │
  │                                           │
  │ [+ 添加审批节点]                          │
  │ [- 删除节点]                              │
  └───────────────────────────────────────────┘

流程状态：[启用 ▼]

[保存] [取消]
```

#### 配置示例（SQL）

```sql
-- 创建/更新全局审批流程
INSERT INTO approval_flow_t (
    id, name_cn, name_en, code, nodes, status, ...
) VALUES (
    1,
    '全局审批流程',
    'Global Approval Flow',
    'global',  -- ✅ 固定编码，标识全局审批
    '[
        {"type":"approver","userId":"admin001","userName":"系统管理员","order":1},
        {"type":"approver","userId":"admin002","userName":"平台管理员","order":2}
    ]',
    1,  -- 启用
    ...
);
```

---

### 4.3 场景审批配置

#### 配置时机
- **业务场景需要时**：当某个业务场景需要专门的审批流程时
- **场景策略调整时**：当场景审批策略变更时

#### 配置位置
- **界面位置**：平台管理后台 → 审批流程管理 → 场景审批流程
- **数据库表**：`approval_flow_t`，`code='场景编码'`

#### 配置权限
- **权限要求**：平台运营管理员
- **权限控制**：只有拥有 `admin:approval_flow:manage` 权限的用户可以配置

#### 场景类型

| 场景编码 | 场景名称 | 说明 |
|---------|---------|------|
| `api_register` | API注册审批 | 提供方注册API时的审批流程 |
| `event_register` | 事件注册审批 | 提供方注册事件时的审批流程 |
| `callback_register` | 回调注册审批 | 提供方注册回调时的审批流程 |
| `permission_apply` | 权限申请审批 | 消费方申请权限时的审批流程 |

#### 配置界面示例

```
审批流程管理 > 场景审批流程列表

场景类型：[权限申请审批 flow ▼]
  ├─ API 注册审批流程 (api_register)
  ├─ 事件注册审批流程 (event_register)
  ├─ 回调注册审批流程 (callback_register)
  └─ 权限申请审批流程 (permission_apply) ✓

审批流程名称：[权限申请审批流程] _______________
流程编码：permission_apply（系统固定，不可修改）

审批节点配置：
  ┌───────────────────────────────────────────┐
  │ 节点1：                                   │
  │   审批人：[权限管理员] [选择用户 ▼]       │
  │   用户ID：perm_admin                      │
  │                                           │
  │ [+ 添加审批节点]                          │
  └───────────────────────────────────────────┘

流程状态：[启用 ▼]

[保存] [取消]
```

#### 配置示例（SQL）

```sql
-- 创建/更新场景审批流程
INSERT INTO approval_flow_t (
    id, name_cn, name_en, code, nodes, status, ...
) VALUES (
    3,
    '权限申请审批流程',
    'Permission Apply Approval Flow',
    'permission_apply',  -- ✅ 场景编码，标识权限申请场景
    '[
        {"type":"approver","userId":"perm_admin","userName":"权限管理员","order":1}
    ]',
    1,  -- 启用
    ...
);
```

---

### 4.4 资源审批配置

#### 配置时机
- **资源注册时**：提供方注册API/事件/回调时配置审批流程
- **资源更新时**：提供方更新资源信息时调整审批流程

#### 配置位置
- **界面位置**：资源详情页面 → 审批配置
- **数据库表**：`permission_t`，字段 `need_approval` + `resource_nodes`

#### 配置权限
- **权限要求**：资源提供方（资源创建者或所属团队）
- **权限控制**：只有资源的提供方可以配置审批流程

#### 配置界面示例

```
资源详情 > API详情 > 审批配置

API名称：支付API
API路径：/api/payment
权限标识：payment_write

是否需要审批：[✓ 是] [ 否 ]

审批节点配置：
  ┌───────────────────────────────────────────┐
  │ 节点1：                                   │
  │   审批人：[支付团队负责人] [选择用户 ▼]   │
  │   用户ID：payment_leader                  │
  │                                           │
  │ 节点2：                                   │
  │   审批人：[财务管理员] [选择用户 ▼]       │
  │   用户ID：finance_admin                   │
  │                                           │
  │ [+ 添加审批节点]                          │
  │ [- 删除节点]                              │
  └───────────────────────────────────────────┘

[保存] [取消]
```

#### 配置示例（SQL）

```sql
-- 创建权限时配置审批节点
INSERT INTO permission_t (
    id, name_cn, scope, resource_type, resource_id, category_id,
    need_approval,  -- ✅ 是否需要审批
    resource_nodes, -- ✅ 审批节点配置
    status, ...
) VALUES (
    200, '支付权限', 'payment_write', 'api', 100, 1,
    1,  -- 需要审批
    '[
        {"type":"approver","userId":"payment_leader","userName":"支付团队负责人","order":1},
        {"type":"approver","userId":"finance_admin","userName":"财务管理员","order":2}
    ]',
    1, ...
);
```

---

### 4.5 配置权限管理

#### 权限矩阵

| 配置操作 | 资源提供方 | 平台运营管理员 | 超级管理员 |
|---------|-----------|---------------|-----------|
| 配置全局审批流程 | ❌ | ❌ | ✅ |
| 配置场景审批流程 | ❌ | ✅ | ✅ |
| 配置资源审批流程 | ✅（自己的资源） | ✅（所有资源） | ✅ |

#### 权限说明

```markdown
**全局审批配置权限**：
- 只有超级管理员可以配置全局审批流程
- 全局审批流程影响所有申请，需要最高权限

**场景审批配置权限**：
- 平台运营管理员可以配置场景审批流程
- 场景审批流程影响特定业务场景的所有申请

**资源审批配置权限**：
- 资源提供方可以配置自己资源的审批流程
- 平台运营管理员可以配置所有资源的审批流程
- 资源审批配置只影响特定资源的申请
```

---

### 4.6 配置变更影响

#### 变更影响范围

| 配置变更类型 | 影响范围 | 是否影响历史审批记录 |
|-------------|---------|-------------------|
| 全局审批流程变更 | 所有新申请 | ❌ 不影响（历史记录独立） |
| 场景审批流程变更 | 该场景的所有新申请 | ❌ 不影响（历史记录独立） |
| 资源审批配置变更 | 该资源的所有新申请 | ❌ 不影响（历史记录独立） |

#### 关键说明

```markdown
**审批记录数据独立性设计**：
- 审批记录创建时，直接存储完整的审批节点配置（combined_nodes）
- 审批记录不关联审批流程表，数据完全独立
- 审批流程模板的修改不影响已创建的审批记录
- 历史审批记录保持原始审批配置，便于审计追溯

**变更生效时机**：
- 全局/场景审批流程变更：立即生效，影响新创建的审批记录
- 资源审批配置变更：立即生效，影响新创建的审批记录
- 已创建的审批记录：保持原始配置，继续执行原审批流程
```

---

## 5. 三级审批配置

### 5.1 配置总览表

| 审批级别 | 配置位置 | 关键字段 | 配置方式 | 说明 |
|---------|---------|---------|---------|------|
| **第一级** | `permission_t` | `need_approval`<br/>`resource_nodes` | 权限主表直接配置审批节点 | 特定权限资源审核 |
| **第二级** | `approval_flow_t` | `code='场景编码'` | 创建审批流程模板 | 创建审批记录时读取配置 |
| **第三级** | `approval_flow_t` | `code='global'` | 创建审批流程模板 | 创建审批记录时读取配置 |

注意：
- 全局审批和场景审批使用审批流程模板（approval_flow_t）
- 资源审批直接在权限主表配置（resource_nodes），不关联审批流程表
- 权限配置独立，不受审批流程模板修改影响

### 5.2 第一级：全局审批配置

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

### 5.3 第二级：场景审批配置

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

### 5.4 第三级：资源审批配置

**配置位置**：`permission_t`（权限主表）

**配置步骤**：

```
步骤：创建权限时直接配置审批节点（无需创建审批流程模板）
```

**配置示例**：

```sql
-- ✅ 直接在权限表中配置审批节点（不关联审批流程表）
INSERT INTO openplatform_v2_permission_t (
    id, 
    name_cn, 
    name_en, 
    scope, 
    resource_type, 
    resource_id, 
    category_id,
    
    -- ✅ 审批配置字段（直接存储审批节点）
    need_approval,      -- 是否需要审批
    resource_nodes,     -- ✅ 直接存储审批节点配置
    
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
    
    -- ✅ 审批配置（直接配置审批节点，无需创建流程模板）
    1,                      -- need_approval：需要审批
    '[                      -- resource_nodes：审批节点配置
        {"type":"approver","userId":"payment_leader","userName":"支付团队负责人","order":1},
        {"type":"approver","userId":"finance_admin","userName":"财务管理员","order":2}
    ]',
    
    1,                      -- 状态：启用
    NOW(3),
    NOW(3),
    'system',
    'system'
);
```

**查询示例**（✅ 直接在主表查询，无需关联审批流程表）：

```sql
-- 查询需要审批的权限列表（含审批节点配置）
SELECT 
    id,
    name_cn,
    need_approval,
    resource_nodes  -- ✅ 直接包含审批节点配置
FROM openplatform_v2_permission_t 
WHERE need_approval = 1 AND status = 1;

-- 查询某权限的审批节点配置
SELECT 
    id,
    name_cn,
    resource_nodes  -- ✅ 直接获取审批节点，无需 JOIN
FROM openplatform_v2_permission_t 
WHERE id = 200;
```

**优势对比**：

#### 方案一：关联审批流程表（已废弃）

```
permission_t.approval_flow_id → 关联 approval_flow_t 查询 nodes
```

**缺点**：
- 需要 JOIN 操作，查询效率低
- 审批流程模板修改会影响权限配置
- 需要先创建审批流程模板，配置步骤繁琐

#### 方案二：直接存储审批节点（当前采用）

```
permission_t.resource_nodes → 直接存储审批节点配置
```

**优点**：
1. **查询效率高** - 单表查询，无需 JOIN 操作
2. **数据独立性** - 权限配置不受审批流程模板修改影响
3. **配置灵活性** - 每个权限可以有独特的审批节点配置
4. **配置简化** - 无需先创建审批流程模板

---

## 6. 组合逻辑

### 6.1 组合流程图

```
用户提交权限申请
    ↓
┌───────────────────────────────────────────────────────┐
│ 系统组合三级审批流程                                    │
├───────────────────────────────────────────────────────┤
│                                                        │
│ 第一级：资源审批（✅ 最具体）                           │
│   SELECT need_approval, resource_nodes                │
│   FROM permission_t WHERE id=200                      │
│   结果：need_approval=1, resource_nodes=[支付负责人, 财务管理员] │
│                                                        │
│   ✅ 直接获取审批节点，无需查询审批流程表               │
│                                                        │
├───────────────────────────────────────────────────────┤
│                                                        │
│ 第二级：场景审批（✅ 业务层）                           │
│   SELECT * FROM approval_flow_t                       │
│   WHERE code='permission_apply'                       │
│   结果：nodes=[权限管理员]                              │
│                                                        │
├───────────────────────────────────────────────────────┤
│                                                        │
│ 第三级：全局审批（✅ 平台层）                           │
│   SELECT * FROM approval_flow_t WHERE code='global'   │
│   结果：nodes=[系统管理员]                              │
│                                                        │
├───────────────────────────────────────────────────────┤
│                                                        │
│ 组合结果：                                              │
│   combined_nodes = [                                  │
│     {order:1, level:'resource', userId:'payment_...'},│
│     {order:2, level:'resource', userId:'finance_...'},│
│     {order:3, level:'scene', userId:'perm_admin'},    │
│     {order:4, level:'global', userId:'admin001'}      │
│   ]                                                    │
│                                                        │
└───────────────────────────────────────────────────────┘
    ↓
创建审批记录（✅ 直接存储完整流程）
INSERT INTO approval_record_t (
    combined_nodes = 组合后的完整审批节点JSON字符串,
    -- ✅ 不存储 flow_id，不关联流程表
    business_type = 'permission_apply',
    business_id = 订阅记录ID,
    applicant_id = 申请人ID,
    applicant_name = 申请人姓名,
    current_node = 0,
    status = 0（待审）
)

说明：
1. combined_nodes 使用 VARCHAR(4000) 存储 JSON 字符串，应用层负责解析
2. 资源审批节点直接从 permission_t.resource_nodes 获取，无需 JOIN
3. 审批记录创建后，不再依赖审批流程表
4. 查询审批节点直接从 combined_nodes 解析即可
```

### 6.2 组合逻辑伪代码

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
    
    // ✅ 第一级：资源审批节点（直接从权限表读取）
    Permission permission = permissionMapper.selectById(permissionId);
    
    if (permission.getNeedApproval() == 1) {  // 需要审批
        String resourceNodesJson = permission.getResourceNodes();  // ✅ 直接获取审批节点
        if (resourceNodesJson != null && !resourceNodesJson.isEmpty()) {
            List<ApprovalNodeDto> resourceNodes = parseNodes(resourceNodesJson);
            for (ApprovalNodeDto node : resourceNodes) {
                node.setOrder(order++);
                node.setLevel("resource");
                combinedNodes.add(node);
            }
        }
    }
    
    // ✅ 第二级：场景审批节点（业务层）
    ApprovalFlow sceneFlow = flowMapper.selectByCode(businessType);
    List<ApprovalNodeDto> sceneNodes = parseNodes(sceneFlow.getNodes());
    for (ApprovalNodeDto node : sceneNodes) {
        node.setOrder(order++);
        node.setLevel("scene");  // ✅ 标记审批级别
        combinedNodes.add(node);
    }
    
    // ✅ 第三级：全局审批节点（平台层）
    ApprovalFlow globalFlow = flowMapper.selectByCode("global");  // ✅ 直接用 code 查询
    List<ApprovalNodeDto> globalNodes = parseNodes(globalFlow.getNodes());
    for (ApprovalNodeDto node : globalNodes) {
        node.setOrder(order++);
        node.setLevel("global");  // ✅ 标记审批级别
        combinedNodes.add(node);
    }
    
    return combinedNodes;
}
```

**关键说明**：
- 资源审批节点直接从 `permission.getResourceNodes()` 获取
- 不需要查询 `approval_flow_t` 表
- 权限审批配置完全独立

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

## 7. 审批执行流程

### 7.1 审批状态流转图

```
初始状态：
approval_record_t.current_node = 0
approval_record_t.status = 0（待审）
approval_record_t.combined_nodes = [节点0, 节点1, 节点2, 节点3]

审批执行过程：

┌─────────────────────────────────────────┐
│ 节点0（资源审批）                        │
│ userId=payment_leader                    │
│ 支付团队负责人登录 → 点击"同意"          │
├─────────────────────────────────────────┤
│ 系统处理：                               │
│ 1. 解析 combined_nodes                  │
│ 2. 判断 current_node=0 是否最后节点     │
│    0 >= nodes.size()-1 (0>=3)？否       │
│ 3. 记录审批日志：                        │
│    INSERT approval_log_t (              │
│      level='resource',                  │
│      action=0（同意）                   │
│    )                                    │
│ 4. 更新审批记录：                        │
│    current_node=1                       │
└─────────────────────────────────────────┘
    ↓
┌─────────────────────────────────────────┐
│ 节点1（资源审批）                        │
│ userId=finance_admin                     │
│ 财务管理员登录 → 点击"同意"              │
├─────────────────────────────────────────┤
│ 系统处理：                               │
│ 1. current_node=1                       │
│ 2. 1 >= 3？否                           │
│ 3. 记录审批日志（level='resource'）     │
│ 4. 更新审批记录：current_node=2         │
└─────────────────────────────────────────┘
    ↓
┌─────────────────────────────────────────┐
│ 节点2（场景审批）                        │
│ userId=perm_admin                        │
│ 权限管理员登录 → 点击"同意"              │
├─────────────────────────────────────────┤
│ 系统处理：                               │
│ 1. current_node=2                       │
│ 2. 2 >= 3？否                           │
│ 3. 记录审批日志（level='scene'）        │
│ 4. 更新审批记录：current_node=3         │
└─────────────────────────────────────────┘
    ↓
┌─────────────────────────────────────────┐
│ 节点3（全局审批）                        │
│ userId=admin001                          │
│ 系统管理员登录 → 点击"同意"              │
├─────────────────────────────────────────┤
│ 系统处理：                               │
│ 1. current_node=3                       │
│ 2. 3 >= 3？是 ✅                         │
│ 3. 记录审批日志（level='global'）       │
│ 4. 更新审批记录：                        │
│    status=1（已通过）                   │
│    completed_at=NOW()                   │
│ 5. 更新订阅记录：                        │
│    status=1（已授权）                   │
└─────────────────────────────────────────┘
    ↓
审批完成，权限激活
```

### 7.2 审批操作判断逻辑

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

### 7.3 拒绝审批处理

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

## 8. 实现方案

### 8.1 ApprovalRecord 实体类

```java
@Data
public class ApprovalRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    
    /**
     * ✅ 核心：组合后的审批节点配置（VARCHAR存储的JSON字符串）
     * 
     * 数据库字段：VARCHAR(4000)，存储 JSON 格式字符串
     * 应用层负责 JSON 解析和序列化
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

### 8.2 Permission 实体类

```java
@Data
public class Permission implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String nameCn;
    private String nameEn;
    private String scope;
    private String resourceType;
    private Long resourceId;
    private Long categoryId;
    
    /**
     * 是否需要审批：0=否, 1=是
     */
    private Integer needApproval;
    
    /**
     * ✅ 资源级审批节点配置（VARCHAR存储的JSON字符串）
     * 
     * 数据库字段：VARCHAR(2000)，存储 JSON 格式字符串
     * 应用层负责 JSON 解析和序列化
     * 
     * 直接存储审批节点信息，不关联审批流程表
     * 
     * 格式示例：
     * [
     *   {"type":"approver","userId":"payment_leader","userName":"支付团队负责人","order":1},
     *   {"type":"approver","userId":"finance_admin","userName":"财务管理员","order":2}
     * ]
     */
    private String resourceNodes;
    
    private Integer status;
    private Date createTime;
    private Date lastUpdateTime;
    private String createBy;
    private String lastUpdateBy;
}
```

### 8.3 ApprovalNodeDto 增强

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

### 8.4 ApprovalFlowComposer 工具类

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
        
        // ✅ 第一级：资源审批（最具体）
        List<ApprovalNodeDto> resourceNodes = getResourceApprovalNodes(permissionId);
        for (ApprovalNodeDto node : resourceNodes) {
            node.setOrder(order++);
            node.setLevel("resource");
            combinedNodes.add(node);
        }
        
        // ✅ 第二级：场景审批（业务层）
        List<ApprovalNodeDto> sceneNodes = getSceneApprovalNodes(businessType);
        for (ApprovalNodeDto node : sceneNodes) {
            node.setOrder(order++);
            node.setLevel("scene");
            combinedNodes.add(node);
        }
        
        // ✅ 第三级：全局审批（平台层）
        List<ApprovalNodeDto> globalNodes = getGlobalApprovalNodes();
        for (ApprovalNodeDto node : globalNodes) {
            node.setOrder(order++);
            node.setLevel("global");
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
     * 获取资源审批节点（✅ 直接从权限表读取）
     */
    private List<ApprovalNodeDto> getResourceApprovalNodes(Long permissionId) {
        Permission permission = permissionMapper.selectById(permissionId);
        if (permission == null || permission.getNeedApproval() != 1) {
            return Collections.emptyList();
        }
        
        // ✅ 直接从权限表获取审批节点配置
        String resourceNodesJson = permission.getResourceNodes();
        if (resourceNodesJson == null || resourceNodesJson.isEmpty()) {
            return Collections.emptyList();
        }
        
        return parseNodes(resourceNodesJson);
    }
    
    /**
     * 解析 JSON 格式的审批节点配置
     */
    private List<ApprovalNodeDto> parseNodes(String nodesJson) {
        return JSON.parseArray(nodesJson, ApprovalNodeDto.class);
    }
}
```

### 8.5 ApprovalFlowMapper 接口

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

### 8.6 PermissionService 集成

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

## 9. 示例场景

### 9.1 完整审批流程示例

**场景**：用户申请"支付API"权限

**配置数据**：

```
全局审批流程：
  id=1, code='global'
  nodes=[系统管理员]

场景审批流程：
  id=3, code='permission_apply'
  nodes=[权限管理员]

权限配置：
  permission_id=200
  need_approval=1
  resource_nodes=[
    {"type":"approver","userId":"payment_leader","userName":"支付团队负责人","order":1},
    {"type":"approver","userId":"finance_admin","userName":"财务管理员","order":2}
  ]
  
  ✅ 直接存储审批节点，不关联审批流程表
```

**组合后的审批流程**：

```
节点1：支付团队负责人（资源审批）
节点2：财务管理员（资源审批）
节点3：权限管理员（场景审批）
节点4：系统管理员（全局审批）
```

**审批记录创建（✅ 直接存储完整流程）**：

```sql
-- 创建审批记录（✅ 直接存储完整流程）
INSERT INTO openplatform_v2_approval_record_t (
    id,
    combined_nodes,     -- ✅ 完整审批节点配置（VARCHAR存储的JSON字符串）
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
    '[  -- ✅ 完整审批流程（VARCHAR存储的JSON字符串）
        {"type":"approver","userId":"payment_leader","userName":"支付团队负责人","order":1,"level":"resource"},
        {"type":"approver","userId":"finance_admin","userName":"财务管理员","order":2,"level":"resource"},
        {"type":"approver","userId":"perm_admin","userName":"权限管理员","order":3,"level":"scene"},
        {"type":"approver","userId":"admin001","userName":"系统管理员","order":4,"level":"global"}
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
-- ✅ combined_nodes 使用 VARCHAR(4000) 存储 JSON 字符串，由应用层解析
-- ✅ 不存储 global_flow_id、scene_flow_id、resource_flow_id
-- ✅ 审批记录创建后，数据完全独立
```

**审批执行**：

```
支付团队负责人审批 → 同意 → current_node=1
财务管理员审批 → 同意 → current_node=2
权限管理员审批 → 同意 → current_node=3
系统管理员审批 → 同意 → status=1（通过）→ 订阅激活
```

### 9.2 数据查询示例

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
--   {"userId":"payment_leader","userName":"支付团队负责人","order":1,"level":"resource"},
--   {"userId":"finance_admin","userName":"财务管理员","order":2,"level":"resource"},
--   {"userId":"perm_admin","userName":"权限管理员","order":3,"level":"scene"},
--   {"userId":"admin001","userName":"系统管理员","order":4,"level":"global"}
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

## 10. 总结

### 10.1 核心设计要点

| 设计要点 | 说明 |
|---------|------|
| **组合而非选择** | 三级审批人按顺序串联成一个完整流程 |
| **combined_nodes** | 存储组合后的完整审批节点JSON |
| **level字段** | 标记节点属于哪一级（global/scene/resource） |
| **current_node** | 当前审批节点索引（0, 1, 2...） |
| **审批顺序** | 资源 → 场景 → 全局（从具体到一般） |
| **拒绝处理** | 拒绝后直接结束流程，不进入下一节点 |
| **code字段** | 统一用code标识审批流程级别（global/场景编码/资源编码） |

### 10.2 字段优化说明

**移除 `is_default` 字段的优化效果**：

| 优化维度 | 优化前 | 优化后 | 改进效果 |
|---------|--------|--------|---------|
| **字段设计** | `is_default` + `code` 双字段 | 仅 `code` 单字段 | 消除字段冗余 |
| **语义清晰度** | `is_default=1` 表示全局审批 | `code='global'` 表示全局审批 | 命名更直观 |
| **查询方式** | `WHERE is_default=1` | `WHERE code='global'` | 查询更统一 |
| **方法调用** | `selectDefaultFlow()` | `selectByCode("global")` | 接口更统一 |
| **唯一性保证** | 应用层逻辑控制 | 数据库唯一索引 `uk_code` | 更安全可靠 |

### 10.3 实施步骤

1. ✅ 创建审批流程模板数据（全局、场景）
2. ✅ 在权限主表中配置审批字段（need_approval、resource_nodes）
3. ✅ 实现 ApprovalFlowComposer 组合逻辑
4. ✅ 修改 PermissionService 使用组合流程
5. ✅ 修改 ApprovalEngine 支持组合审批
6. ✅ 测试完整审批流程

### 10.4 审批顺序设计原理

**审批顺序设计原理**：

采用"从具体到一般"的审批顺序，原因如下：

1. **资源审批优先**（第一级）
   - 资源提供方最了解资源的价值和风险
   - 先确认是否愿意授权给申请方
   - 如果资源提供方拒绝，无需进行后续审批

2. **场景审批居中**（第二级）
   - 业务场景层面审核申请是否符合业务规范
   - 确保申请符合场景特定的安全和管理要求
   - 场景审批可以设置场景级别的安全策略

3. **全局审批兜底**（第三级）
   - 平台运营层面最终审核
   - 确保申请符合平台整体规范
   - 平台可以统一管理和控制审批流程

**优势**：
- 提高审批效率：资源提供方拒绝后，无需进行后续审批
- 降低沟通成本：资源提供方直接审核，减少信息传递
- 符合业务逻辑：从具体到一般，层层把关

---

## 11. 相关文档

- [需求文档](spec.md) - FR-025, FR-026, FR-027
- [数据库设计](plan-db.md) - 审批相关表结构
- [API设计](plan-api.md) - 审批管理接口
- [ADR-001](ADR-001.md) - 审批流程设计决策

---

## 12. 版本更新记录

### v2.6.0 (2026-04-24)

**新增内容**：
- 补充三级审批的完整配置方案
- 说明全局审批、场景审批的配置位置和配置权限
- 新增配置界面示例

**关键说明**：
- 全局审批：平台管理后台配置，超级管理员权限
- 场景审批：平台管理后台配置，平台运营管理员权限
- 资源审批：资源详情页面配置，资源提供方权限

**配置权限矩阵**：
- 资源提供方：只能配置自己资源的审批流程
- 平台运营管理员：可以配置场景审批流程和所有资源的审批流程
- 超级管理员：可以配置全局审批流程

**主要变更**：

| 变更项 | 变更内容 |
|--------|---------|
| 章节 2.1 | 审批级别定义表格新增配置时机、配置位置、配置权限列 |
| 新增章节 4 | 审批流程配置方案（6个子章节） |
| 章节编号调整 | 原第4-11章顺延为第5-12章 |

**新增章节内容**：
- 4.1 三级审批配置总览
- 4.2 全局审批配置（配置时机、配置位置、配置权限、配置界面示例）
- 4.3 场景审批配置（配置时机、配置位置、配置权限、场景类型、配置界面示例）
- 4.4 资源审批配置（配置时机、配置位置、配置权限、配置界面示例）
- 4.5 配置权限管理（权限矩阵、权限说明）
- 4.6 配置变更影响（变更影响范围、关键说明）

**向后兼容性**：
- ✅ 数据库设计无变更
- ✅ 业务逻辑无变更
- ✅ 仅文档补充，不影响现有实现

### v2.5.0 (2026-04-24)

**优化内容**：
- 将所有 JSON 类型字段改为 VARCHAR 类型

**优化原因**：
1. 数据库兼容性：VARCHAR 在所有数据库版本中都支持
2. 性能考虑：VARCHAR 在简单查询场景下性能更好
3. 存储可控：VARCHAR 有明确的长度限制
4. 迁移便利：数据库迁移时更方便

**主要变更**：

| 表 | 字段 | 变更 |
|---|------|------|
| approval_flow_t | nodes | JSON → VARCHAR(2000) |
| approval_record_t | combined_nodes | JSON → VARCHAR(4000) |
| permission_t | resource_nodes | JSON → VARCHAR(2000) |

**向后兼容性**：
- ✅ 应用层代码无需修改（已使用字符串处理）
- ✅ JSON 解析逻辑由应用层处理，数据库层变更无影响

### v2.4.0 (2026-04-24)

**优化内容**：
- 资源审批配置改为直接存储审批节点（resource_nodes）
- 移除 approval_flow_id 字段，不再关联审批流程表

**优化原因**：
1. 数据独立性：权限配置不受审批流程模板修改影响
2. 查询效率：直接从权限表读取审批节点，无需 JOIN
3. 配置灵活性：每个权限可以有独特的审批节点配置
4. 表结构简化：移除关联字段，减少表关联复杂度

**主要变更**：

| 项目 | 之前 | 现在 |
|------|------|------|
| 资源审批配置 | approval_flow_id → 关联流程表 | resource_nodes → 直接存储节点 |
| 查询方式 | JOIN approval_flow_t | 单表查询 |
| 配置步骤 | 2步（创建流程+配置权限） | 1步（配置权限） |
| 数据独立性 | 受流程模板修改影响 | 完全独立 |

**影响范围**：
- ✅ 数据库表结构：`openplatform_v2_permission_t`（移除 approval_flow_id，新增 resource_nodes）
- ✅ 权限实体类：`Permission`（移除 approvalFlowId，新增 resourceNodes）
- ✅ 组合逻辑：`ApprovalFlowComposer.getResourceApprovalNodes()`（直接读取 resourceNodes）
- ✅ 配置示例：权限配置SQL（直接配置审批节点）
- ✅ 查询示例：单表查询，无需 JOIN

**向后兼容性**：
- ❌ 不兼容旧版本数据，需要数据迁移
- ❌ 需要更新所有使用 `approval_flow_id` 字段的查询和代码
- ❌ 需要将历史审批流程配置迁移到权限表的 resource_nodes 字段

### v2.3.0 (2026-04-24)

**优化内容**：调整三级审批组合顺序为"从具体到一般"

**优化原因**：

采用"从具体到一般"的审批顺序（资源审批 → 场景审批 → 全局审批），原因如下：

1. **提高审批效率** - 资源提供方拒绝后，无需进行后续审批
2. **降低沟通成本** - 资源提供方直接审核，减少信息传递
3. **符合业务逻辑** - 从具体到一般，层层把关

**主要变更**：

| 变更项 | 变更前 | 变更后 |
|--------|--------|--------|
| 审批顺序 | 全局 → 场景 → 资源 | 资源 → 场景 → 全局 |
| 第一级审批 | 全局审批 | 资源审批 |
| 第二级审批 | 场景审批 | 场景审批 |
| 第三级审批 | 资源审批 | 全局审批 |
| 组合逻辑代码 | globalNodes → sceneNodes → resourceNodes | resourceNodes → sceneNodes → globalNodes |

**影响范围**：
- ✅ 审批流程配置表：审批级别定义调整
- ✅ 组合逻辑代码：ApprovalFlowComposer 顺序调整
- ✅ 审批执行流程：审批节点顺序调整
- ✅ 示例场景：所有示例中的审批顺序调整
- ✅ 文档说明：设计原理章节新增

**向后兼容性**：
- ❌ 不兼容旧版本数据，需要数据迁移
- ❌ 需要更新所有审批流程配置和组合逻辑

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