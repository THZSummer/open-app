# 审批流程设计方案

> **版本**: 2.8.0  
> **创建时间**: 2026-04-24  
> **状态**: 设计完成

---

## 📋 目录

- [1. 概述](#1-概述)
- [2. 核心概念](#2-核心概念)
- [3. 审批流程配置方案](#3-审批流程配置方案)
- [4. 三级审批配置](#4-三级审批配置)
- [5. 组合逻辑](#5-组合逻辑)
- [6. 审批状态流转记录机制](#6-审批状态流转记录机制)
- [7. 审批执行流程](#7-审批执行流程)
- [8. 实现方案](#8-实现方案)
- [9. 示例场景](#9-示例场景)
- [10. 总结](#10-总结)
- [11. 相关文档](#11-相关文档)
- [12. 版本更新记录](#12-版本更新记录)

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
│  └─ 场景流程：code='api_permission_apply'（根据资源类型）     │
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
        api_permission_apply=API权限申请审批流程，
        event_permission_apply=事件权限申请审批流程，
        callback_permission_apply=回调权限申请审批流程，
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
| `api_permission_apply` | 场景审批 | API 权限申请场景 |
| `event_permission_apply` | 场景审批 | 事件权限申请场景 |
| `callback_permission_apply` | 场景审批 | 回调权限申请场景 |
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
    
    `business_type` VARCHAR(50) NOT NULL COMMENT '业务类型：
        api_register = API注册审批，
        event_register = 事件注册审批，
        callback_register = 回调注册审批，
        api_permission_apply = API权限申请审批，
        event_permission_apply = 事件权限申请审批，
        callback_permission_apply = 回调权限申请审批',
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

| 场景编码 | 场景名称 | 适用场景 | 说明 |
|---------|---------|---------|------|
| `global` | 全局审批 | 所有申请 | 平台级审批流程 |
| `api_register` | API注册审批 | API注册场景 | 提供方注册API时的审批 |
| `event_register` | 事件注册审批 | 事件注册场景 | 提供方注册事件时的审批 |
| `callback_register` | 回调注册审批 | 回调注册场景 | 提供方注册回调时的审批 |
| `api_permission_apply` | **API权限申请审批** | API权限申请场景 | ✅ 消费方申请API权限时的审批 |
| `event_permission_apply` | **事件权限申请审批** | 事件权限申请场景 | ✅ 消费方申请事件权限时的审批 |
| `callback_permission_apply` | **回调权限申请审批** | 回调权限申请场景 | ✅ 消费方申请回调权限时的审批 |

**说明**：
- ❌ 废弃：`permission_apply`（通用权限申请审批）
- ✅ 新增：`api_permission_apply`、`event_permission_apply`、`callback_permission_apply`
- 原因：不同资源类型的权限申请应该有独立的审批流程，便于精细化管理

#### 配置界面示例

```
审批流程管理 > 场景审批流程列表

场景类型：[API权限申请审批 ▼]
  ├─ API 注册审批流程 (api_register)
  ├─ 事件注册审批流程 (event_register)
  ├─ 回调注册审批流程 (callback_register)
  ├─ ───────────────────────
  ├─ API 权限申请审批流程 (api_permission_apply) ✓
  ├─ 事件权限申请审批流程 (event_permission_apply)
  └─ 回调权限申请审批流程 (callback_permission_apply)

审批流程名称：[API权限申请审批流程] _______________
流程编码：api_permission_apply（系统固定，不可修改）

审批节点配置：
  ┌───────────────────────────────────────────┐
  │ 节点1：                                   │
  │   审批人：[API管理员] [选择用户 ▼]        │
  │   用户ID：api_admin                       │
  │                                           │
  │ [+ 添加审批节点]                          │
  └───────────────────────────────────────────┘

流程状态：[启用 ▼]

[保存] [取消]
```

#### 配置示例（SQL）

```sql
-- ✅ API 权限申请审批流程
INSERT INTO approval_flow_t (
    id, name_cn, name_en, code, nodes, status, ...
) VALUES (
    3,
    'API权限申请审批流程',
    'API Permission Apply Approval Flow',
    'api_permission_apply',  -- ✅ API 权限申请场景
    '[
        {"type":"approver","userId":"api_admin","userName":"API管理员","order":1}
    ]',
    1,  -- 启用
    ...
);

-- ✅ 事件权限申请审批流程
INSERT INTO approval_flow_t (
    id, name_cn, name_en, code, nodes, status, ...
) VALUES (
    4,
    '事件权限申请审批流程',
    'Event Permission Apply Approval Flow',
    'event_permission_apply',  -- ✅ 事件权限申请场景
    '[
        {"type":"approver","userId":"event_admin","userName":"事件管理员","order":1}
    ]',
    1,  -- 启用
    ...
);

-- ✅ 回调权限申请审批流程
INSERT INTO approval_flow_t (
    id, name_cn, name_en, code, nodes, status, ...
) VALUES (
    5,
    '回调权限申请审批流程',
    'Callback Permission Apply Approval Flow',
    'callback_permission_apply',  -- ✅ 回调权限申请场景
    '[
        {"type":"approver","userId":"callback_admin","userName":"回调管理员","order":1}
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
| 全局审批 | `global` | 所有申请 |
| API 注册 | `api_register` | 提供方注册 API |
| 事件注册 | `event_register` | 提供方注册事件 |
| 回调注册 | `callback_register` | 提供方注册回调 |
| **API 权限申请** | `api_permission_apply` | **消费方申请 API 权限** |
| **事件权限申请** | `event_permission_apply` | **消费方申请事件权限** |
| **回调权限申请** | `callback_permission_apply` | **消费方申请回调权限** |

**配置示例**：

```sql
-- ✅ 为 API 权限申请场景配置审批流程
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
    'API权限申请场景审批流程',
    'API Permission Apply Approval Flow',
    'api_permission_apply',  -- ✅ API 权限申请场景
    '[{"type":"approver","userId":"api_admin","userName":"API管理员","order":1}]',
    1,
    NOW(3),
    NOW(3),
    'system',
    'system'
);

-- ✅ 为事件权限申请场景配置审批流程
INSERT INTO openplatform_v2_approval_flow_t (
    id, 
    name_cn, 
    name_en, 
    code, 
    nodes, 
    status,
    create_time, 
    last_update_time, 
    create_by, 
    last_update_by
) VALUES (
    4,
    '事件权限申请场景审批流程',
    'Event Permission Apply Approval Flow',
    'event_permission_apply',  -- ✅ 事件权限申请场景
    '[{"type":"approver","userId":"event_admin","userName":"事件管理员","order":1}]',
    1,
    NOW(3),
    NOW(3),
    'system',
    'system'
);

-- ✅ 为回调权限申请场景配置审批流程
INSERT INTO openplatform_v2_approval_flow_t (
    id, 
    name_cn, 
    name_en, 
    code, 
    nodes, 
    status,
    create_time, 
    last_update_time, 
    create_by, 
    last_update_by
) VALUES (
    5,
    '回调权限申请场景审批流程',
    'Callback Permission Apply Approval Flow',
    'callback_permission_apply',  -- ✅ 回调权限申请场景
    '[{"type":"approver","userId":"callback_admin","userName":"回调管理员","order":1}]',
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
│   WHERE code='api_permission_apply'  -- ✅ 根据资源类型│
│   结果：nodes=[API管理员]                              │
│                                                        │
│   ✅ sceneCode 可以是：                                │
│      - api_permission_apply (API权限申请)             │
│      - event_permission_apply (事件权限申请)          │
│      - callback_permission_apply (回调权限申请)       │
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
│     {order:3, level:'scene', userId:'api_admin'},     │
│     {order:4, level:'global', userId:'admin001'}      │
│   ]                                                    │
│                                                        │
└───────────────────────────────────────────────────────┘
    ↓
创建审批记录（✅ 直接存储完整流程）
INSERT INTO approval_record_t (
    combined_nodes = 组合后的完整审批节点JSON字符串,
    -- ✅ 不存储 flow_id，不关联流程表
    business_type = 'api_permission_apply',  -- ✅ 根据资源类型选择
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
// ✅ 根据权限的资源类型确定场景审批编码
String sceneCode = getSceneCodeByResourceType(permission.getResourceType());

// 组合审批流程
List<ApprovalNodeDto> combinedNodes = composeApprovalFlow(permissionId, sceneCode);

// 创建审批记录
ApprovalRecord record = new ApprovalRecord();
record.setId(idGenerator.nextId());

// ✅ 直接存储完整审批节点配置
record.setCombinedNodes(serializeNodes(combinedNodes));

// ✅ 不存储 flow_id 字段，不关联审批流程表
// 原因：combined_nodes 已包含完整信息，审批记录数据独立

record.setBusinessType(sceneCode);  // ✅ 使用对应的场景审批编码
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
public List<ApprovalNodeDto> composeApprovalFlow(Long permissionId, String sceneCode) {
    
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
    
    // ✅ 第二级：场景审批节点（根据 sceneCode 查询）
    // sceneCode 可以是：
    //   - api_permission_apply (API权限申请)
    //   - event_permission_apply (事件权限申请)
    //   - callback_permission_apply (回调权限申请)
    ApprovalFlow sceneFlow = flowMapper.selectByCode(sceneCode);
    if (sceneFlow != null) {
        List<ApprovalNodeDto> sceneNodes = parseNodes(sceneFlow.getNodes());
        for (ApprovalNodeDto node : sceneNodes) {
            node.setOrder(order++);
            node.setLevel("scene");  // ✅ 标记审批级别
            combinedNodes.add(node);
        }
    }
    
    // ✅ 第三级：全局审批节点（平台层）
    ApprovalFlow globalFlow = flowMapper.selectByCode("global");  // ✅ 直接用 code 查询
    if (globalFlow != null) {
        List<ApprovalNodeDto> globalNodes = parseNodes(globalFlow.getNodes());
        for (ApprovalNodeDto node : globalNodes) {
            node.setOrder(order++);
            node.setLevel("global");  // ✅ 标记审批级别
            combinedNodes.add(node);
        }
    }
    
    return combinedNodes;
}

/**
 * 根据资源类型获取场景审批编码
 */
private String getSceneCodeByResourceType(String resourceType) {
    switch (resourceType) {
        case "api":
            return "api_permission_apply";      // ✅ API 权限申请审批
        case "event":
            return "event_permission_apply";    // ✅ 事件权限申请审批
        case "callback":
            return "callback_permission_apply"; // ✅ 回调权限申请审批
        default:
            return "api_permission_apply";      // 默认使用 API 审批
    }
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

## 6. 审批状态流转记录机制

### 6.1 核心概念

审批状态流转涉及两个核心字段和一个日志表：

#### 审批记录表（approval_record_t）核心字段

| 字段名 | 类型 | 说明 | 作用 |
|--------|------|------|------|
| `status` | TINYINT(10) | 审批整体状态 | 记录审批流程的整体结果（待审/通过/拒绝/撤销） |
| `current_node` | INT | 当前审批节点索引 | 记录审批流程当前执行到哪个节点 |
| `combined_nodes` | VARCHAR(4000) | 完整审批节点配置 | 定义审批流程的所有节点信息 |

#### 审批日志表（approval_log_t）

| 字段名 | 类型 | 说明 | 作用 |
|--------|------|------|------|
| `record_id` | BIGINT(20) | 审批记录ID | 关联到具体的审批记录 |
| `node_index` | INT | 审批节点索引 | 记录本次操作是哪个节点 |
| `level` | VARCHAR(20) | 审批级别 | 标记本次操作属于哪一级审批 |
| `operator_id` | VARCHAR(100) | 操作人ID | 记录谁执行了审批操作 |
| `operator_name` | VARCHAR(100) | 操作人姓名 | 显示审批人姓名 |
| `action` | TINYINT(10) | 审批动作 | 0=同意, 1=拒绝, 2=撤销, 3=转交 |
| `comment` | TEXT | 审批意见 | 记录审批人的意见或原因 |
| `create_time` | DATETIME(3) | 操作时间 | 记录审批操作发生的时间 |

---

### 6.2 审批状态定义

#### status 字段值定义

| 值 | 状态名称 | 说明 | 后续操作 |
|----|---------|------|---------|
| `0` | 待审 | 审批流程正在执行中 | 可以继续审批、拒绝、撤销 |
| `1` | 已通过 | 所有审批节点都已同意 | 审批流程结束，业务对象激活 |
| `2` | 已拒绝 | 有审批节点拒绝了申请 | 审批流程结束，业务对象拒绝 |
| `3` | 已撤销 | 申请人撤销了申请 | 审批流程结束，业务对象撤销 |

#### action 字段值定义（审批日志表）

| 值 | 动作名称 | 说明 | status 影响 |
|----|---------|------|------------|
| `0` | 同意 | 审批人同意本次申请 | 进入下一节点或审批通过 |
| `1` | 拒绝 | 审批人拒绝本次申请 | status=2（已拒绝） |
| `2` | 撤销 | 申请人撤销申请 | status=3（已撤销） |
| `3` | 转交 | 审批人转交给其他人审批 | 不影响 status |

---

### 6.3 审批节点索引（current_node）

#### combined_nodes 与 current_node 的关系

```
combined_nodes（审批节点数组）:
[
  节点0: {"userId":"payment_leader","level":"resource","order":1},
  节点1: {"userId":"finance_admin","level":"resource","order":2},
  节点2: {"userId":"perm_admin","level":"scene","order":3},
  节点3: {"userId":"admin001","level":"global","order":4}
]

current_node（当前节点索引）:
  0 → 节点0审批（支付团队负责人）
  1 → 节点1审批（财务管理员）
  2 → 节点2审批（权限管理员）
  3 → 节点3审批（系统管理员）
```

#### current_node 流转逻辑

| current_node 值 | 当前状态 | 执行操作 | current_node 变化 | status 变化 |
|----------------|---------|---------|------------------|------------|
| 0 | 待审 | 节点0同意 | current_node = 1 | 不变（status=0） |
| 1 | 待审 | 节点1同意 | current_node = 2 | 不变（status=0） |
| 2 | 待审 | 节点2同意 | current_node = 3 | 不变（status=0） |
| 3 | 待审 | 节点3同意 | 不变（current_node=3） | status = 1（已通过） ✅ |
| 任意 | 待审 | 节点拒绝 | 不变 | status = 2（已拒绝） |
| 任意 | 待审 | 申请撤销 | 不变 | status = 3（已撤销） |

---

### 6.4 完整审批流转示例

#### 示例场景：用户申请"支付API"权限

**审批流程配置**：
```
combined_nodes = [
  节点0: 支付团队负责人（resource级）
  节点1: 财务管理员（resource级）
  节点2: 权限管理员（scene级）
  节点3: 系统管理员（global级）
]
```

---

#### 阶段1：创建审批记录（初始状态）

**申请人提交申请时**：

```sql
-- 创建审批记录
INSERT INTO approval_record_t (
    id,
    combined_nodes,
    business_type,
    business_id,
    applicant_id,
    applicant_name,
    status,         -- ✅ 初始状态：待审
    current_node,   -- ✅ 初始节点：0（第一个节点）
    create_time,
    last_update_time
) VALUES (
    9001,
    '[
        {"userId":"payment_leader","userName":"支付团队负责人","level":"resource","order":1},
        {"userId":"finance_admin","userName":"财务管理员","level":"resource","order":2},
        {"userId":"perm_admin","userName":"权限管理员","level":"scene","order":3},
        {"userId":"admin001","userName":"系统管理员","level":"global","order":4}
    ]',
    'api_permission_apply',
    5001,
    'user001',
    '张三',
    0,  -- ✅ 待审状态
    0,  -- ✅ 当前节点索引=0（第一个节点）
    NOW(3),
    NOW(3)
);
```

**状态说明**：
```
approval_record_t:
  id = 9001
  status = 0（待审）
  current_node = 0（节点0：支付团队负责人）

审批流程：
节点0 [待审] → 节点1 [未执行] → 节点2 [未执行] → 节点3 [未执行]
   ↑
当前节点
```

---

#### 阶段2：节点0审批（支付团队负责人同意）

**操作**：支付团队负责人登录系统，点击"同意"，填写意见"符合业务需求"

**系统处理**：

```java
// ApprovalEngine.approve() 方法

public ApprovalRecord approve(Long recordId, String operatorId, String operatorName, String comment) {
    
    // 1. 查询审批记录
    ApprovalRecord record = recordMapper.selectById(9001);
    // record.current_node = 0
    // record.status = 0
    
    // 2. 解析审批节点
    List<ApprovalNodeDto> nodes = parseNodes(record.getCombinedNodes());
    // nodes.size() = 4
    // nodes[0] = {userId:"payment_leader", level:"resource"}
    
    // 3. ✅ 记录审批日志（重要：记录本次审批操作）
    ApprovalLog log = new ApprovalLog();
    log.setId(idGenerator.nextId());
    log.setRecordId(9001);
    log.setNodeIndex(0);  // ✅ 当前节点索引
    log.setLevel("resource");  // ✅ 审批级别：资源审批
    log.setOperatorId("payment_leader");
    log.setOperatorName("支付团队负责人");
    log.setAction(0);  // ✅ 动作：同意
    log.setComment("符合业务需求");
    log.setCreateTime(NOW(3));
    
    approvalLogMapper.insert(log);
    
    // 4. 判断是否最后一个节点
    if (record.getCurrentNode() >= nodes.size() - 1) {
        // 0 >= 3？否 → 不是最后一个节点
        // 进入下一节点
        record.setCurrentNode(1);  // ✅ 更新当前节点为1
        record.setStatus(0);       // ✅ 状态保持待审
    }
    
    // 5. 更新审批记录
    recordMapper.update(record);
    
    return record;
}
```

**审批日志记录**：
```sql
-- ✅ 记录审批日志（记录节点0的审批操作）
INSERT INTO approval_log_t (
    id,
    record_id,
    node_index,
    level,
    operator_id,
    operator_name,
    action,
    comment,
    create_time
) VALUES (
    10001,
    9001,
    0,              -- ✅ 节点索引：节点0
    'resource',     -- ✅ 审批级别：资源审批
    'payment_leader',
    '支付团队负责人',
    0,              -- ✅ 动作：同意
    '符合业务需求',
    NOW(3)
);
```

**审批记录更新**：
```sql
-- ✅ 更新审批记录（进入下一节点）
UPDATE approval_record_t 
SET current_node = 1,  -- ✅ 当前节点索引更新为1
    last_update_time = NOW(3)
WHERE id = 9001;
```

**当前状态**：
```
approval_record_t:
  id = 9001
  status = 0（待审）
  current_node = 1（节点1：财务管理员）

approval_log_t:
  node_index=0, action=0（同意）, level='resource'

审批流程：
节点0 [已通过 ✓] → 节点1 [待审] → 节点2 [未执行] → 节点3 [未执行]
                        ↑
                    当前节点
```

---

#### 阶段3：节点1审批（财务管理员同意）

**操作**：财务管理员登录系统，点击"同意"，填写意见"财务合规"

**系统处理**：
```java
// 同样的审批逻辑，只是 current_node=1

// 记录审批日志
log.setNodeIndex(1);  // ✅ 节点索引：节点1
log.setLevel("resource");  // ✅ 审批级别：资源审批
log.setAction(0);  // ✅ 动作：同意

// 判断是否最后一个节点
// 1 >= 3？否 → 不是最后一个节点
// 进入下一节点
record.setCurrentNode(2);  // ✅ 更新当前节点为2
```

**审批日志记录**：
```sql
-- ✅ 记录审批日志（记录节点1的审批操作）
INSERT INTO approval_log_t VALUES (
    10002,
    9001,
    1,              -- ✅ 节点索引：节点1
    'resource',     -- ✅ 审批级别：资源审批
    'finance_admin',
    '财务管理员',
    0,              -- ✅ 动作：同意
    '财务合规',
    NOW(3)
);
```

**审批记录更新**：
```sql
-- ✅ 更新审批记录（进入下一节点）
UPDATE approval_record_t 
SET current_node = 2,
    last_update_time = NOW(3)
WHERE id = 9001;
```

**当前状态**：
```
approval_record_t:
  id = 9001
  status = 0（待审）
  current_node = 2（节点2：权限管理员）

approval_log_t:
  node_index=0, action=0（同意）
  node_index=1, action=0（同意）

审批流程：
节点0 [已通过 ✓] → 节点1 [已通过 ✓] → 节点2 [待审] → 节点3 [未执行]
                                        ↑
                                    当前节点
```

---

#### 阶段4：节点2审批（权限管理员同意）

**操作**：权限管理员登录系统，点击"同意"

**审批日志记录**：
```sql
INSERT INTO approval_log_t VALUES (
    10003,
    9001,
    2,              -- ✅ 节点索引：节点2
    'scene',        -- ✅ 审批级别：场景审批
    'perm_admin',
    '权限管理员',
    0,              -- ✅ 动作：同意
    '同意',
    NOW(3)
);
```

**审批记录更新**：
```sql
UPDATE approval_record_t 
SET current_node = 3,
    last_update_time = NOW(3)
WHERE id = 9001;
```

**当前状态**：
```
approval_record_t:
  status = 0（待审）
  current_node = 3（节点3：系统管理员）

审批流程：
节点0 [已通过 ✓] → 节点1 [已通过 ✓] → 节点2 [已通过 ✓] → 节点3 [待审]
                                                            ↑
                                                        当前节点
```

---

#### 阶段5：节点3审批（系统管理员同意 - 最后一个节点）

**操作**：系统管理员登录系统，点击"同意"

**系统处理**：
```java
// ApprovalEngine.approve() 方法

// 判断是否最后一个节点
if (record.getCurrentNode() >= nodes.size() - 1) {
    // 3 >= 3？是 ✅ → 是最后一个节点
    
    // ✅ 审批通过！
    record.setStatus(1);  // ✅ 状态：已通过
    record.setCompletedAt(NOW(3));
    
    // ✅ 更新业务对象状态（激活订阅关系）
    Subscription subscription = subscriptionMapper.selectById(5001);
    subscription.setStatus(1);  // ✅ 已授权
    subscription.setApprovedAt(NOW(3));
    subscriptionMapper.update(subscription);
}
```

**审批日志记录**：
```sql
INSERT INTO approval_log_t VALUES (
    10004,
    9001,
    3,              -- ✅ 节点索引：节点3
    'global',       -- ✅ 审批级别：全局审批
    'admin001',
    '系统管理员',
    0,              -- ✅ 动作：同意
    '同意',
    NOW(3)
);
```

**审批记录更新**（✅ 审批通过）：
```sql
-- ✅ 审批通过！更新审批记录状态
UPDATE approval_record_t 
SET status = 1,         -- ✅ 状态：已通过
    completed_at = NOW(3),
    last_update_time = NOW(3)
WHERE id = 9001;

-- ✅ 激活订阅关系
UPDATE subscription_t 
SET status = 1,         -- ✅ 已授权
    approved_at = NOW(3),
    last_update_time = NOW(3)
WHERE id = 5001;
```

**最终状态**：
```
approval_record_t:
  id = 9001
  status = 1（已通过） ✅
  current_node = 3
  completed_at = 2026-04-24 10:30:00

approval_log_t:
  node_index=0, action=0（同意）, level='resource'
  node_index=1, action=0（同意）, level='resource'
  node_index=2, action=0（同意）, level='scene'
  node_index=3, action=0（同意）, level='global' ✅

subscription_t:
  id = 5001
  status = 1（已授权） ✅
  approved_at = 2026-04-24 10:30:00

审批流程：
节点0 [已通过 ✓] → 节点1 [已通过 ✓] → 节点2 [已通过 ✓] → 节点3 [已通过 ✓]
                                                            ✅ 审批完成
```

---

### 6.5 拒绝审批流转示例

#### 示例：节点1拒绝审批

**初始状态**：
```
approval_record_t:
  status = 0（待审）
  current_node = 1（节点1：财务管理员）
```

**操作**：财务管理员点击"拒绝"，填写原因"不符合财务规范"

**系统处理**：
```java
// ApprovalEngine.reject() 方法

public ApprovalRecord reject(Long recordId, String operatorId, String reason) {
    
    // 1. 查询审批记录
    ApprovalRecord record = recordMapper.selectById(recordId);
    
    // 2. ✅ 记录审批日志
    ApprovalLog log = new ApprovalLog();
    log.setNodeIndex(record.getCurrentNode());  // ✅ 当前节点索引
    log.setAction(1);  // ✅ 动作：拒绝
    log.setComment(reason);  // ✅ 拒绝原因
    approvalLogMapper.insert(log);
    
    // 3. ✅ 直接结束审批流程（拒绝状态）
    record.setStatus(2);  // ✅ 状态：已拒绝
    record.setCompletedAt(NOW(3));
    
    // 4. ✅ 更新业务对象状态（拒绝订阅）
    Subscription subscription = subscriptionMapper.selectById(5001);
    subscription.setStatus(2);  // ✅ 已拒绝
    subscriptionMapper.update(subscription);
    
    // 5. 更新审批记录
    recordMapper.update(record);
    
    return record;
}
```

**审批日志记录**：
```sql
INSERT INTO approval_log_t VALUES (
    10002,
    9001,
    1,              -- ✅ 节点索引：节点1
    'resource',
    'finance_admin',
    '财务管理员',
    1,              -- ✅ 动作：拒绝
    '不符合财务规范', -- ✅ 拒绝原因
    NOW(3)
);
```

**审批记录更新**：
```sql
-- ✅ 审批拒绝！直接结束流程
UPDATE approval_record_t 
SET status = 2,         -- ✅ 状态：已拒绝
    completed_at = NOW(3),
    last_update_time = NOW(3)
WHERE id = 9001;

-- ✅ 拒绝订阅
UPDATE subscription_t 
SET status = 2,         -- ✅ 已拒绝
    last_update_time = NOW(3)
WHERE id = 5001;
```

**最终状态**：
```
approval_record_t:
  status = 2（已拒绝） ✅
  current_node = 1（审批流程在这里停止）

approval_log_t:
  node_index=0, action=0（同意）
  node_index=1, action=1（拒绝） ✅

审批流程：
节点0 [已通过 ✓] → 节点1 [已拒绝 ✗] → 节点2 [未执行] → 节点3 [未执行]
                    ↑ ✅ 审批流程在此停止
                当前节点
```

---

### 6.6 撤销审批流转示例

#### 示例：申请人撤销申请

**初始状态**：
```
approval_record_t:
  status = 0（待审）
  current_node = 1（节点1）
```

**操作**：申请人张三点击"撤销"

**系统处理**：
```java
// ApprovalEngine.cancel() 方法

public ApprovalRecord cancel(Long recordId, String operator) {
    
    // 1. 查询审批记录
    ApprovalRecord record = recordMapper.selectById(recordId);
    
    // 2. ✅ 记录审批日志
    ApprovalLog log = new ApprovalLog();
    log.setNodeIndex(record.getCurrentNode());
    log.setOperatorId(record.getApplicantId());  // ✅ 操作人：申请人
    log.setAction(2);  // ✅ 动作：撤销
    approvalLogMapper.insert(log);
    
    // 3. ✅ 直接结束审批流程（撤销状态）
    record.setStatus(3);  // ✅ 状态：已撤销
    record.setCompletedAt(NOW(3));
    
    // 4. ✅ 更新业务对象状态
    Subscription subscription = subscriptionMapper.selectById(5001);
    subscription.setStatus(3);  // ✅ 已取消
    subscriptionMapper.update(subscription);
    
    // 5. 更新审批记录
    recordMapper.update(record);
    
    return record;
}
```

**审批日志记录**：
```sql
INSERT INTO approval_log_t VALUES (
    10005,
    9001,
    1,              -- ✅ 当前节点索引
    'resource',
    'user001',      -- ✅ 操作人：申请人
    '张三',
    2,              -- ✅ 动作：撤销
    '申请人撤销',
    NOW(3)
);
```

**审批记录更新**：
```sql
-- ✅ 审批撤销！直接结束流程
UPDATE approval_record_t 
SET status = 3,         -- ✅ 状态：已撤销
    completed_at = NOW(3),
    last_update_time = NOW(3)
WHERE id = 9001;

-- ✅ 取消订阅
UPDATE subscription_t 
SET status = 3,         -- ✅ 已取消
    last_update_time = NOW(3)
WHERE id = 5001;
```

**最终状态**：
```
approval_record_t:
  status = 3（已撤销） ✅

审批流程：
节点0 [已通过 ✓] → 节点1 [已撤销 ⊘] → 节点2 [未执行] → 节点3 [未执行]
                    ↑ ✅ 审批流程在此停止
```

---

### 6.7 审批状态查询示例

#### 查询审批记录状态

```sql
-- 查询审批记录当前状态
SELECT 
    id,
    status,
    current_node,
    CASE status 
        WHEN 0 THEN '待审'
        WHEN 1 THEN '已通过'
        WHEN 2 THEN '已拒绝'
        WHEN 3 THEN '已撤销'
    END AS status_name,
    create_time,
    completed_at
FROM approval_record_t
WHERE id = 9001;
```

#### 查询审批流转日志

```sql
-- 查询审批流转历史（按时间顺序）
SELECT 
    l.node_index,
    l.level,
    CASE l.level 
        WHEN 'resource' THEN '资源审批'
        WHEN 'scene' THEN '场景审批'
        WHEN 'global' THEN '全局审批'
    END AS level_name,
    l.operator_name,
    CASE l.action 
        WHEN 0 THEN '同意'
        WHEN 1 THEN '拒绝'
        WHEN 2 THEN '撤销'
        WHEN 3 THEN '转交'
    END AS action_name,
    l.comment,
    l.create_time
FROM approval_log_t l
WHERE l.record_id = 9001
ORDER BY l.node_index ASC;

-- 结果示例：
-- node_index=0, level='资源审批', operator='支付团队负责人', action='同意', comment='符合业务需求'
-- node_index=1, level='资源审批', operator='财务管理员', action='同意', comment='财务合规'
-- node_index=2, level='场景审批', operator='权限管理员', action='同意', comment='同意'
-- node_index=3, level='全局审批', operator='系统管理员', action='同意', comment='同意'
```

#### 查询审批节点状态

```sql
-- 查询审批详情（包含节点状态）
SELECT 
    r.id,
    r.status,
    r.current_node,
    r.combined_nodes,
    -- ✅ 可以通过 combined_nodes 解析出所有节点信息
    -- 然后根据 current_node 和 approval_log 判断每个节点的状态
    ...
FROM approval_record_t r
WHERE r.id = 9001;

-- ✅ 节点状态判断逻辑（应用层处理）：
-- for each node in combined_nodes:
--   if node_index < current_node: node.status = '已通过'
--   if node_index == current_node: node.status = '待审' 或 '当前审批中'
--   if node_index > current_node: node.status = '未执行'
```

---

### 6.8 关键设计要点

#### 1. 状态流转规则

```
待审 (status=0)
    ├─ 同意 → 进入下一节点 或 审批通过 (status=1)
    ├─ 拒绝 → 审批拒绝 (status=2) ✅ 立即结束
    └─ 撤销 → 审批撤销 (status=3) ✅ 立即结束

已通过 (status=1) ✅ 流程结束，不可操作
已拒绝 (status=2) ✅ 流程结束，不可操作
已撤销 (status=3) ✅ 流程结束，不可操作
```

#### 2. 审批日志的作用

```
审批日志表（approval_log_t）：
1. ✅ 记录每一步审批操作的详细信息
2. ✅ 审计追溯：可以查看完整的审批历史
3. ✅ 数据完整性：即使审批记录表更新，日志表保留原始操作记录
4. ✅ 分析统计：可以统计审批人的审批效率、拒绝率等
```

#### 3. 数据一致性保证

```
审批记录表 + 审批日志表 = 完整的审批流转记录

审批记录表（approval_record_t）：
- 记录审批的整体状态（status）
- 记录当前执行位置（current_node）
- 记录审批节点配置（combined_nodes）

审批日志表（approval_log_t）：
- 记录每一步审批操作的详细信息
- 记录审批人、审批动作、审批意见
- 记录审批时间戳

两者配合，形成完整的审批流转记录：
- 审批记录表：提供审批流程的"快照"状态
- 审批日志表：提供审批流程的"历史"轨迹
```

---

### 6.9 审批节点协调机制

#### 6.9.1 串行审批机制（当前采用）

**定义**：审批节点按顺序执行，必须完成前一个节点才能进入下一个节点。

**特点**：
- ✅ 流程清晰：审批顺序固定，不会混乱
- ✅ 责任明确：每个审批人知道自己的审批顺序
- ✅ 易于管理：可以精确控制审批流程

**实现方式**：

```
审批流程：
节点0 → 节点1 → 节点2 → 节点3

执行规则：
1. 节点0审批完成后，才能进入节点1
2. 节点1审批完成后，才能进入节点2
3. 节点2审批完成后，才能进入节点3
4. 所有节点完成后，审批通过

当前节点索引控制：
current_node = 0 → 节点0审批
current_node = 1 → 节点1审批
current_node = 2 → 节点2审批
current_node = 3 → 节点3审批
```

**代码实现**：

```java
// 串行审批核心逻辑
public ApprovalRecord approve(Long recordId, String operatorId, String comment) {
    
    ApprovalRecord record = recordMapper.selectById(recordId);
    List<ApprovalNodeDto> nodes = parseNodes(record.getCombinedNodes());
    
    int currentNodeIndex = record.getCurrentNode();
    
    // ✅ 串行执行：只能审批当前节点
    if (currentNodeIndex >= nodes.size() - 1) {
        // 最后一个节点，审批通过
        record.setStatus(1);
    } else {
        // 进入下一个节点（串行）
        record.setCurrentNode(currentNodeIndex + 1);
    }
    
    recordMapper.update(record);
    return record;
}
```

---

#### 6.9.2 节点间协调规则

**规则1：审批权限校验**

```
只有当前节点的审批人才能执行审批操作

示例：
current_node = 1
节点1的审批人 = finance_admin

✅ finance_admin 可以审批
❌ payment_leader（节点0审批人）不能审批
❌ perm_admin（节点2审批人）不能审批
```

**代码实现**：

```java
public ApprovalRecord approve(Long recordId, String operatorId, String comment) {
    
    ApprovalRecord record = recordMapper.selectById(recordId);
    List<ApprovalNodeDto> nodes = parseNodes(record.getCombinedNodes());
    
    int currentNodeIndex = record.getCurrentNode();
    ApprovalNodeDto currentNode = nodes.get(currentNodeIndex);
    
    // ✅ 校验审批权限：只有当前节点的审批人才能操作
    if (!currentNode.getUserId().equals(operatorId)) {
        throw new BusinessException("403", 
            "您不是当前节点的审批人", 
            "You are not the approver of current node");
    }
    
    // 继续审批逻辑...
}
```

---

**规则2：状态互斥控制**

```
审批状态互斥，同一时间只能处于一种状态

status = 0（待审）→ 可以审批、拒绝、撤销
status = 1（已通过）→ ✅ 流程结束，不可操作
status = 2（已拒绝）→ ✅ 流程结束，不可操作
status = 3（已撤销）→ ✅ 流程结束，不可操作
```

**代码实现**：

```java
public ApprovalRecord approve(Long recordId, String operatorId, String comment) {
    
    ApprovalRecord record = recordMapper.selectById(recordId);
    
    // ✅ 状态校验：只有待审状态才能操作
    if (record.getStatus() != 0) {
        throw new BusinessException("400", 
            "审批记录状态不正确，无法操作", 
            "Approval record status is incorrect, cannot operate");
    }
    
    // 继续审批逻辑...
}

public ApprovalRecord reject(Long recordId, String operatorId, String reason) {
    
    ApprovalRecord record = recordMapper.selectById(recordId);
    
    // ✅ 状态校验：只有待审状态才能操作
    if (record.getStatus() != 0) {
        throw new BusinessException("400", 
            "审批记录状态不正确，无法操作", 
            "Approval record status is incorrect, cannot operate");
    }
    
    // 继续拒绝逻辑...
}
```

---

**规则3：并发控制（乐观锁）**

```
防止多人同时审批同一节点

并发场景：
审批人A和审批人B同时点击"同意"

解决方案：
使用版本号或时间戳进行乐观锁控制
```

**数据库表增加版本号字段**：

```sql
ALTER TABLE approval_record_t 
ADD COLUMN `version` INT DEFAULT 0 COMMENT '版本号（乐观锁）';
```

**代码实现**：

```java
public ApprovalRecord approve(Long recordId, String operatorId, String comment) {
    
    ApprovalRecord record = recordMapper.selectById(recordId);
    int currentVersion = record.getVersion();
    
    // ✅ 执行审批逻辑
    // ...
    
    // ✅ 更新审批记录（带版本号校验）
    int updated = recordMapper.updateWithVersion(record, currentVersion);
    
    if (updated == 0) {
        // 版本号不匹配，说明已被其他人修改
        throw new BusinessException("409", 
            "审批记录已被其他人处理，请刷新后重试", 
            "Approval record has been processed by others, please refresh and retry");
    }
    
    return record;
}

// Mapper 接口
int updateWithVersion(@Param("record") ApprovalRecord record, 
                       @Param("version") int version);

// Mapper XML
<update id="updateWithVersion">
    UPDATE approval_record_t 
    SET status = #{record.status},
        current_node = #{record.currentNode},
        version = version + 1,  -- ✅ 版本号自增
        last_update_time = NOW(3)
    WHERE id = #{record.id} 
      AND version = #{version}  -- ✅ 版本号校验
</update>
```

---

#### 6.9.3 审批超时与催办机制

**审批超时检测**：

```
场景：审批人长时间未处理审批请求

解决方案：
1. 记录每个节点的开始时间
2. 定时检测超时未处理的审批
3. 发送提醒通知或自动转交
```

**数据库表增加节点开始时间字段**：

```sql
ALTER TABLE approval_record_t 
ADD COLUMN `node_start_time` DATETIME(3) COMMENT '当前节点开始时间';
```

**审批节点超时配置**：

```sql
-- 审批流程模板表增加超时配置
ALTER TABLE approval_flow_t 
ADD COLUMN `timeout_hours` INT DEFAULT 72 COMMENT '超时时间（小时）';

-- 审批节点配置增加超时时间
nodes JSON:
[
  {
    "type": "approver",
    "userId": "payment_leader",
    "order": 1,
    "timeoutHours": 48  -- ✅ 节点超时时间（小时）
  }
]
```

**超时检测代码**：

```java
/**
 * 定时任务：检测审批超时
 * 执行频率：每小时执行一次
 */
@Scheduled(cron = "0 0 * * * ?")
public void checkApprovalTimeout() {
    
    // 查询所有待审的审批记录
    List<ApprovalRecord> pendingRecords = recordMapper.selectPendingRecords();
    
    for (ApprovalRecord record : pendingRecords) {
        
        // 检查当前节点是否超时
        if (isTimeout(record)) {
            
            // ✅ 发送催办通知
            sendReminderNotification(record);
            
            // 或者自动转交给上级
            // autoEscalate(record);
        }
    }
}

/**
 * 判断是否超时
 */
private boolean isTimeout(ApprovalRecord record) {
    
    // 解析审批节点
    List<ApprovalNodeDto> nodes = parseNodes(record.getCombinedNodes());
    ApprovalNodeDto currentNode = nodes.get(record.getCurrentNode());
    
    // 获取节点超时时间
    Integer timeoutHours = currentNode.getTimeoutHours();
    if (timeoutHours == null) {
        timeoutHours = 72;  // 默认72小时
    }
    
    // 计算是否超时
    Date nodeStartTime = record.getNodeStartTime();
    long hoursPassed = (System.currentTimeMillis() - nodeStartTime.getTime()) / (1000 * 60 * 60);
    
    return hoursPassed > timeoutHours;
}

/**
 * 发送催办通知
 */
private void sendReminderNotification(ApprovalRecord record) {
    
    List<ApprovalNodeDto> nodes = parseNodes(record.getCombinedNodes());
    ApprovalNodeDto currentNode = nodes.get(record.getCurrentNode());
    
    // 发送邮件/短信/站内信通知
    notificationService.send(
        currentNode.getUserId(),
        "审批催办通知",
        String.format("您有一个审批待处理，已超时%d小时", 
            calculateTimeoutHours(record))
    );
}
```

---

**催办机制**：

```
催办流程：
1. 第一次超时（48小时）：发送提醒通知
2. 第二次超时（72小时）：发送催办通知 + 通知上级
3. 第三次超时（96小时）：自动转交给上级审批
```

**代码实现**：

```java
/**
 * 催办记录表
 */
CREATE TABLE `approval_reminder_t` (
    `id` BIGINT(20) PRIMARY KEY,
    `record_id` BIGINT(20) NOT NULL COMMENT '审批记录ID',
    `node_index` INT NOT NULL COMMENT '审批节点索引',
    `reminder_count` INT DEFAULT 0 COMMENT '催办次数',
    `last_reminder_time` DATETIME(3) COMMENT '最后催办时间',
    `create_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    KEY `idx_record_id` (`record_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批催办记录表';

/**
 * 发送催办通知（支持多次催办）
 */
private void sendReminderNotification(ApprovalRecord record) {
    
    // 查询催办记录
    ApprovalReminder reminder = reminderMapper.selectByRecordId(record.getId());
    
    if (reminder == null) {
        reminder = new ApprovalReminder();
        reminder.setRecordId(record.getId());
        reminder.setNodeIndex(record.getCurrentNode());
        reminder.setReminderCount(0);
    }
    
    int reminderCount = reminder.getReminderCount();
    
    if (reminderCount == 0) {
        // 第一次催办：发送提醒通知
        sendEmail(record, "审批提醒", "您有一个审批待处理");
        
    } else if (reminderCount == 1) {
        // 第二次催办：发送催办通知 + 通知上级
        sendEmail(record, "审批催办", "审批已超时，请尽快处理");
        notifySupervisor(record);
        
    } else {
        // 第三次催办：自动转交给上级
        autoEscalate(record);
    }
    
    // 更新催办记录
    reminder.setReminderCount(reminderCount + 1);
    reminder.setLastReminderTime(NOW(3));
    reminderMapper.insertOrUpdate(reminder);
}
```

---

#### 6.9.4 审批转交机制

**场景**：审批人无法处理时，可以转交给其他人审批

**代码实现**：

```java
/**
 * 审批转交
 * 
 * @param recordId 审批记录ID
 * @param fromUserId 原审批人ID
 * @param toUserId 新审批人ID
 * @param reason 转交原因
 */
public ApprovalRecord transfer(Long recordId, String fromUserId, String toUserId, String reason) {
    
    ApprovalRecord record = recordMapper.selectById(recordId);
    
    // 1. 校验状态
    if (record.getStatus() != 0) {
        throw new BusinessException("400", "审批记录状态不正确，无法转交");
    }
    
    // 2. 校验权限：只有当前节点的审批人才能转交
    List<ApprovalNodeDto> nodes = parseNodes(record.getCombinedNodes());
    ApprovalNodeDto currentNode = nodes.get(record.getCurrentNode());
    
    if (!currentNode.getUserId().equals(fromUserId)) {
        throw new BusinessException("403", "您不是当前节点的审批人，无法转交");
    }
    
    // 3. 记录审批日志（转交）
    ApprovalLog log = new ApprovalLog();
    log.setRecordId(recordId);
    log.setNodeIndex(record.getCurrentNode());
    log.setOperatorId(fromUserId);
    log.setAction(3);  // ✅ 动作：转交
    log.setComment(String.format("转交给 %s，原因：%s", toUserId, reason));
    approvalLogMapper.insert(log);
    
    // 4. 更新审批节点配置（将审批人改为新审批人）
    currentNode.setUserId(toUserId);
    // 注意：这里需要查询新审批人的姓名
    User newUser = userService.getUserById(toUserId);
    currentNode.setUserName(newUser.getName());
    
    // 5. 更新审批记录
    record.setCombinedNodes(serializeNodes(nodes));
    record.setNodeStartTime(NOW(3));  // 重置节点开始时间
    recordMapper.update(record);
    
    // 6. 发送通知给新审批人
    notificationService.send(
        toUserId,
        "审批转交通知",
        String.format("您收到一个转交的审批请求，原审批人：%s，转交原因：%s", fromUserId, reason)
    );
    
    return record;
}
```

---

#### 6.9.5 审批冲突处理

**场景1：同一节点多人同时审批**

```
场景：审批人A和审批人B同时点击"同意"

解决方案：使用乐观锁（version字段）或数据库行锁

推荐：乐观锁（性能更好）
```

**代码实现**：

```java
// 使用数据库行锁（悲观锁）
public ApprovalRecord approveWithLock(Long recordId, String operatorId, String comment) {
    
    // ✅ 使用 SELECT FOR UPDATE 锁定记录
    ApprovalRecord record = recordMapper.selectByIdForUpdate(recordId);
    
    // 执行审批逻辑
    // ...
    
    return record;
}

// Mapper XML
<select id="selectByIdForUpdate" resultType="ApprovalRecord">
    SELECT * FROM approval_record_t 
    WHERE id = #{id} 
    FOR UPDATE  -- ✅ 行锁
</select>
```

---

**场景2：审批过程中申请人撤销**

```
场景：
- 审批人A正在审批（打开了审批页面）
- 申请人撤销了申请
- 审批人A点击"同意"

解决方案：状态校验 + 友好提示
```

**代码实现**：

```java
public ApprovalRecord approve(Long recordId, String operatorId, String comment) {
    
    ApprovalRecord record = recordMapper.selectById(recordId);
    
    // ✅ 状态校验
    if (record.getStatus() != 0) {
        String statusName = "";
        switch (record.getStatus()) {
            case 1: statusName = "已通过"; break;
            case 2: statusName = "已拒绝"; break;
            case 3: statusName = "已撤销"; break;
        }
        throw new BusinessException("400", 
            String.format("审批已%s，无法操作", statusName),
            String.format("Approval has been %s", statusName));
    }
    
    // 继续审批逻辑...
}
```

---

**场景3：审批节点配置变更**

```
场景：
- 审批进行到节点1
- 管理员修改了审批流程模板，节点1的审批人变了

解决方案：审批记录数据独立性设计
```

**设计说明**：

```
审批记录创建时：
1. ✅ 直接存储完整的审批节点配置（combined_nodes）
2. ✅ 不关联审批流程表
3. ✅ 审批流程模板的修改不影响已创建的审批记录

结果：
- 已创建的审批记录：继续按原配置执行
- 新创建的审批记录：使用新配置
- 历史审批记录：保持原始配置，便于审计追溯
```

---

#### 6.9.6 节点协调最佳实践

**实践1：审批通知机制**

```
审批节点变更时，及时通知相关人员：

1. 进入新节点：通知新审批人
2. 审批通过：通知申请人
3. 审批拒绝：通知申请人（包含拒绝原因）
4. 审批撤销：通知所有已审批的人
5. 审批转交：通知新审批人
```

**代码实现**：

```java
/**
 * 进入新节点时发送通知
 */
private void notifyNewApprover(ApprovalRecord record, ApprovalNodeDto node) {
    
    // 获取申请人信息
    String applicantName = record.getApplicantName();
    String businessType = record.getBusinessType();
    
    // 发送通知
    notificationService.send(
        node.getUserId(),
        "待审批通知",
        String.format("您有一个新的审批请求，申请人：%s，类型：%s", 
            applicantName, businessType)
    );
}

/**
 * 审批完成时发送通知
 */
private void notifyApplicant(ApprovalRecord record, boolean approved) {
    
    String title = approved ? "审批通过通知" : "审批拒绝通知";
    String message = approved 
        ? "您的申请已审批通过" 
        : "您的申请已被拒绝，请查看拒绝原因";
    
    notificationService.send(
        record.getApplicantId(),
        title,
        message
    );
}
```

---

**实践2：审批历史查询**

```
提供完整的审批历史查询功能：

1. 按申请人查询
2. 按审批人查询
3. 按审批状态查询
4. 按时间范围查询
```

**SQL示例**：

```sql
-- 查询某审批人的待审批列表
SELECT 
    r.id,
    r.business_type,
    r.business_id,
    r.applicant_name,
    r.create_time,
    n.user_name AS current_approver,
    n.level AS current_level
FROM approval_record_t r
-- 解析 combined_nodes 获取当前审批人（应用层处理）
WHERE r.status = 0
  AND r.current_node IN (
      -- 查找当前审批人是该用户的记录
  )
ORDER BY r.create_time DESC;

-- 查询某申请人的审批历史
SELECT 
    r.id,
    r.business_type,
    r.status,
    CASE r.status 
        WHEN 0 THEN '待审'
        WHEN 1 THEN '已通过'
        WHEN 2 THEN '已拒绝'
        WHEN 3 THEN '已撤销'
    END AS status_name,
    r.create_time,
    r.completed_at
FROM approval_record_t r
WHERE r.applicant_id = 'user001'
ORDER BY r.create_time DESC;
```

---

**实践3：审批统计分析**

```
审批效率统计：

1. 平均审批时长
2. 各节点审批时长
3. 审批通过率
4. 审批人效率排名
```

**SQL示例**：

```sql
-- 统计审批平均时长
SELECT 
    business_type,
    AVG(TIMESTAMPDIFF(HOUR, create_time, completed_at)) AS avg_hours,
    COUNT(*) AS total_count,
    SUM(CASE WHEN status = 1 THEN 1 ELSE 0 END) AS approved_count,
    SUM(CASE WHEN status = 2 THEN 1 ELSE 0 END) AS rejected_count
FROM approval_record_t
WHERE status IN (1, 2)
  AND create_time >= DATE_SUB(NOW(), INTERVAL 30 DAY)
GROUP BY business_type;

-- 统计各节点平均审批时长
SELECT 
    l.level,
    l.node_index,
    AVG(TIMESTAMPDIFF(HOUR, 
        (SELECT create_time FROM approval_log_t WHERE record_id = l.record_id AND node_index = l.node_index - 1),
        l.create_time
    )) AS avg_hours
FROM approval_log_t l
WHERE l.action = 0
GROUP BY l.level, l.node_index;
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
│ userId=api_admin                         │
│ API管理员登录 → 点击"同意"               │
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
    // ✅ 判断是否为权限申请场景（三种类型）
    if (isPermissionApplyType(record.getBusinessType())) {
        Subscription subscription = subscriptionMapper.selectById(record.getBusinessId());
        subscription.setStatus(2);  // 已拒绝
        subscriptionMapper.update(subscription);
    }
    
    // 6. 更新审批记录
    recordMapper.update(record);
    
    return record;
}

/**
 * 判断是否为权限申请场景
 */
private boolean isPermissionApplyType(String businessType) {
    return "api_permission_apply".equals(businessType)
        || "event_permission_apply".equals(businessType)
        || "callback_permission_apply".equals(businessType);
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

// ✅ 查询场景审批流程（三种权限申请场景）
ApprovalFlow apiPermFlow = flowMapper.selectByCode("api_permission_apply");
ApprovalFlow eventPermFlow = flowMapper.selectByCode("event_permission_apply");
ApprovalFlow callbackPermFlow = flowMapper.selectByCode("callback_permission_apply");

// 查询资源审批流程
ApprovalFlow resourceFlow = flowMapper.selectByCode("payment_api");
// 或通过ID查询
ApprovalFlow resourceFlow = flowMapper.selectById(1001L);
```

### 8.6 ApprovalEngine.BusinessType 枚举

```java
/**
 * 业务类型枚举
 */
public static class BusinessType {
    public static final String API_REGISTER = "api_register";
    public static final String EVENT_REGISTER = "event_register";
    public static final String CALLBACK_REGISTER = "callback_register";
    
    // ✅ 权限申请分为三种类型
    public static final String API_PERMISSION_APPLY = "api_permission_apply";
    public static final String EVENT_PERMISSION_APPLY = "event_permission_apply";
    public static final String CALLBACK_PERMISSION_APPLY = "callback_permission_apply";
}
```

### 8.7 PermissionService 集成

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
        
        // 1. 查询权限信息
        Permission permission = permissionMapper.selectById(permissionId);
        
        // 2. 创建订阅记录
        Subscription subscription = new Subscription();
        subscription.setId(idGenerator.nextId());
        subscription.setAppId(appId);
        subscription.setPermissionId(permissionId);
        subscription.setStatus(0);  // 待审
        subscriptionMapper.insert(subscription);
        
        // 3. ✅ 根据权限的资源类型确定场景审批编码
        String sceneCode = getSceneCodeByResourceType(permission.getResourceType());
        
        // 4. ✅ 组合三级审批流程
        List<ApprovalNodeDto> combinedNodes = approvalFlowComposer.composeApprovalFlow(
            permissionId, 
            sceneCode  // ✅ 使用对应的场景审批编码
        );
        
        // 5. 创建审批记录
        ApprovalRecord record = new ApprovalRecord();
        record.setId(idGenerator.nextId());
        record.setCombinedNodes(serializeNodes(combinedNodes));  // ✅ 存储完整审批节点配置
        record.setBusinessType(sceneCode);  // ✅ 使用对应的场景审批编码
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
     * 根据资源类型获取场景审批编码
     */
    private String getSceneCodeByResourceType(String resourceType) {
        switch (resourceType) {
            case "api":
                return "api_permission_apply";      // ✅ API 权限申请审批
            case "event":
                return "event_permission_apply";    // ✅ 事件权限申请审批
            case "callback":
                return "callback_permission_apply"; // ✅ 回调权限申请审批
            default:
                return "api_permission_apply";      // 默认使用 API 审批
        }
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
  id=3, code='api_permission_apply'  -- ✅ API权限申请场景
  nodes=[API管理员]

权限配置：
  permission_id=200
  resource_type='api'  -- ✅ API类型
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
        {"type":"approver","userId":"api_admin","userName":"API管理员","order":3,"level":"scene"},
        {"type":"approver","userId":"admin001","userName":"系统管理员","order":4,"level":"global"}
    ]',
    'api_permission_apply',  -- ✅ API权限申请场景
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
API管理员审批 → 同意 → current_node=3
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
--   {"userId":"api_admin","userName":"API管理员","order":3,"level":"scene"},
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

### v2.8.0 (2026-04-24)

**新增内容**：
- 新增第6章"审批状态流转记录机制"
- 新增审批节点协调机制详细说明（6.9子章节）

**新增原因**：
1. 详细解释审批状态如何流转记录
2. 说明不同审批节点如何协调
3. 补充审批超时、催办、转交机制
4. 提供审批冲突处理方案

**主要变更**：

| 章节 | 变更内容 |
|------|---------|
| 6.1-6.8 | 新增审批状态流转记录机制 |
| 6.9 | 新增审批节点协调机制 |

**新增章节内容**：
- 6.1 核心概念（审批记录表核心字段、审批日志表字段）
- 6.2 审批状态定义（status字段值定义、action字段值定义）
- 6.3 审批节点索引（combined_nodes与current_node的关系、流转逻辑）
- 6.4 完整审批流转示例（5个阶段：创建记录、节点0-3审批）
- 6.5 拒绝审批流转示例
- 6.6 撤销审批流转示例
- 6.7 审批状态查询示例（查询记录状态、流转日志、节点状态）
- 6.8 关键设计要点（状态流转规则、审批日志作用、数据一致性保证）
- 6.9 审批节点协调机制
  - 6.9.1 串行审批机制（当前采用）
  - 6.9.2 节点间协调规则（权限校验、状态互斥、并发控制）
  - 6.9.3 审批超时与催办机制
  - 6.9.4 审批转交机制
  - 6.9.5 审批冲突处理
  - 6.9.6 节点协调最佳实践

**向后兼容性**：
- ✅ 数据库设计无变更
- ✅ 业务逻辑无变更
- ✅ 仅文档补充，不影响现有实现

### v2.7.0 (2026-04-24)

**优化内容**：
- 将权限申请审批细分为三种场景审批流程

**优化原因**：
1. 精细化管理：不同资源类型可以配置不同的审批人
2. 业务隔离：API、事件、回调有独立的审批策略
3. 灵活性：可以针对不同资源类型设置不同的审批流程

**主要变更**：

| 变更前 | 变更后 |
|--------|--------|
| `permission_apply` | ❌ 废弃（通用权限申请审批） |
| - | ✅ 新增 `api_permission_apply` |
| - | ✅ 新增 `event_permission_apply` |
| - | ✅ 新增 `callback_permission_apply` |

**场景审批编码对照**：

| 资源类型 | 场景审批编码 | 说明 |
|---------|-------------|------|
| API | `api_permission_apply` | API 权限申请审批 |
| Event | `event_permission_apply` | 事件权限申请审批 |
| Callback | `callback_permission_apply` | 回调权限申请审批 |

**影响范围**：
- ✅ 数据库表结构：`approval_flow_t` 表的 `code` 字段注释
- ✅ 数据库表结构：`approval_record_t` 表的 `business_type` 字段注释
- ✅ 业务代码：`PermissionService.subscribePermission()` 方法
- ✅ 业务代码：`ApprovalFlowComposer.composeApprovalFlow()` 方法
- ✅ 业务代码：新增 `getSceneCodeByResourceType()` 方法
- ✅ 业务代码：新增 `isPermissionApplyType()` 方法
- ✅ 审批流程配置：需要创建三种新的场景审批流程模板

**向后兼容性**：
- ❌ 不兼容旧版本数据（需要数据迁移）
- 需要将原有 `permission_apply` 审批记录迁移到对应的三种场景审批
- 需要创建三种新的场景审批流程模板
- 需要更新业务代码中的场景编码引用

**数据迁移建议**：
1. 根据权限的资源类型（resource_type），更新审批记录的 business_type 字段
2. 创建三种新的场景审批流程模板
3. 更新所有使用 `permission_apply` 的业务代码

```sql
-- 数据迁移示例：根据权限的资源类型更新审批记录
UPDATE approval_record_t ar
JOIN subscription_t s ON ar.business_id = s.id
JOIN permission_t p ON s.permission_id = p.id
SET ar.business_type = CASE p.resource_type
    WHEN 'api' THEN 'api_permission_apply'
    WHEN 'event' THEN 'event_permission_apply'
    WHEN 'callback' THEN 'callback_permission_apply'
    ELSE 'api_permission_apply'
END
WHERE ar.business_type = 'permission_apply';
```

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