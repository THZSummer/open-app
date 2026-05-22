# Zapier 连接流编排数据存储调研报告

**版本**：V1.0
**日期**：2026年5月
**调研维度**：连接流编排数据存储（编排定义存储 / 运行时状态 / 执行历史）

---

## 一、概述

Zapier 作为全球最大的 iPaaS 平台（7000+ 应用），其核心工作流单元称为 **Zap**。Zap 的数据存储体系分为三个层面：**编排定义存储**（Zap 配置）、**运行时状态存储**（执行上下文）、**执行历史存储**（Task History）。Zapier 采用全托管 SaaS 模式，底层基于 AWS 基础设施，数据存储以关系型数据库（PostgreSQL）为主，辅以缓存（Redis）和对象存储（S3）。

---

## 二、编排定义存储（Zap 配置存储）

### 2.1 核心实体模型

Zapier 的编排定义采用 **Zap → Step → Action/Trigger** 的分层结构：

| 实体 | 职责 | 关键字段 |
|------|------|---------|
| **Zap** | 工作流整体配置 | id, name, status（draft/paused/running）, created_at, updated_at, owner_id |
| **Step** | 工作流中的单一步骤 | id, zap_id, step_order, step_type（trigger/filter/action/path/delay）, config JSON |
| **Connection** | 认证连接实例 | id, user_id, app_id, credentials（加密存储）, auth_type, status |
| **App** | 连接器定义 | id, key, name, trigger_definitions[], action_definitions[], search_definitions[] |

### 2.2 Step Config 存储结构

每个 Step 的配置以 **JSON 对象**存储在 `steps.config` 字段中：

```
steps.config = {
  "app_id": "slack",
  "operation_id": "send_message",
  "params": {
    "channel": { "source": "previous_step", "field": "channel_id" },
    "text": { "source": "literal", "value": "Hello from Zapier!" }
  },
  "filter": {
    "conditions_operator": "and",
    "conditions": [
      { "field": "subject", "operator": "contains", "value": "紧急" }
    ]
  }
}
```

**参数引用方式**：
- `source: "previous_step"` — 引用上游步骤的输出
- `source: "literal"` — 静态值
- `source: "input_field"` — 用户配置时输入
- 支持模板语法 `{{ step_id.field_path }}` 进行字段映射

### 2.3 Paths（条件分支）存储

Paths 是 Zapier 的条件分支机制，存储为 Step 的一个子结构：

```
step = {
  "type": "paths",
  "paths_config": {
    "paths": [
      {
        "id": "path_1",
        "label": "金额 > 1000",
        "condition": {
          "operator_group": "and",
          "rules": [
            { "field": "amount", "operator": "greater_than", "value": "1000" }
          ]
        },
        "steps": [ /* 嵌套的子步骤 */ ]
      },
      {
        "id": "path_2",
        "label": "默认路径",
        "condition": null,  // 默认路径无条件
        "steps": [ /* 子步骤 */ ]
      }
    ]
  }
}
```

### 2.4 存储技术

| 存储对象 | 技术选型 | 说明 |
|---------|---------|------|
| Zap 元数据 | PostgreSQL | 关系表存储 Zap 基本信息 |
| Step Config | PostgreSQL JSONB | `steps.config` 字段使用 JSONB 类型，支持部分索引和查询 |
| 连接器定义 | PostgreSQL + S3 | App 定义存储在 PostgreSQL，大字段 Schema 定义存储在 S3 |
| 认证凭证 | AWS KMS + PostgreSQL | 敏感凭证使用 AES-256 加密后存储，密钥由 KMS 管理 |
| 版本快照 | PostgreSQL | 每次 Zap 保存生成版本快照，支持回滚 |

### 2.5 设计特点

- **扁平化 Step 列表**：Zap 的 Step 是一个有序列表，Paths 通过嵌套步骤实现分支，但整体仍是树形结构
- **JSONB 灵活存储**：Step 配置使用 JSONB 而非 EAV（Entity-Attribute-Value），兼顾灵活性与查询性能
- **版本化管理**：每次保存 Zap 自动创建版本快照，用户可随时回滚

---

## 三、运行时状态存储

### 3.1 执行调度状态

Zapier 的运行时分为**轮询驱动**（Polling Trigger）和 **Webhook 驱动**（Instant Trigger）两种模式：

| 状态类型 | 存储方式 | 关键数据 |
|---------|---------|---------|
| **轮询游标（Polling Cursor）** | Redis + PostgreSQL | 记录每个 Trigger 的最后处理时间戳或 ID，用于增量拉取 |
| **Webhook 订阅** | PostgreSQL | 记录 Webhook URL、订阅事件类型、过期时间 |
| **执行队列** | Redis（消息队列） | 待执行的 Zap 任务进入队列，Worker 消费执行 |
| **并发控制** | Redis | 基于 Redis 的分布式锁，控制单个 Zap 的并发执行数 |

### 3.2 执行上下文传递

Zap 执行时，每个 Step 的输出数据通过**内存中的 Bundle 对象**传递给下一个 Step：

```
Bundle = {
  "inputData": {},        // 当前步骤的输入参数
  "outputData": {},       // 当前步骤的执行结果
  "authData": {},         // 认证凭证（运行时从加密存储中解密加载）
  "meta": {
    "zap_id": "...",
    "execution_id": "...",
    "step_id": "...",
    "attempt_number": 3,  // 重试次数
    "cursor": "..."       // 轮询游标
  },
  "environment": {}       // 环境变量
}
```

### 3.3 失败重试状态

| 重试层级 | 存储机制 | 策略 |
|---------|---------|------|
| **单步骤重试** | 执行队列 + Redis 计数 | 最多重试 3 次，指数退避间隔（10s → 30s → 60s） |
| **整体 Zap 重试** | 定时任务表 | 最终失败后，根据 Zap 配置决定是否禁用 Zap |

---

## 四、执行历史存储（Task History）

### 4.1 数据结构

每个 Zap 执行一次产生一条 **Task History** 记录：

```
task_history = {
  "id": "th_abc123",
  "zap_id": "zap_456",
  "status": "success" | "error" | "filtered",
  "triggered_at": "2026-05-14T10:00:00Z",
  "completed_at": "2026-05-14T10:00:05Z",
  "duration_ms": 5234,
  "steps": [
    {
      "step_id": "step_1",
      "status": "success",
      "input": { /* 输入数据快照 */ },
      "output": { /* 输出数据快照 */ },
      "error": null,
      "duration_ms": 1200
    },
    {
      "step_id": "step_2",
      "status": "error",
      "input": { /* 输入数据快照 */ },
      "output": null,
      "error": { "code": "API_ERROR", "message": "Rate limited" },
      "duration_ms": 800,
      "retry_count": 2
    }
  ]
}
```

### 4.2 存储策略

| 存储对象 | 技术选型 | 保留策略 |
|---------|---------|---------|
| **Task History 元数据** | PostgreSQL | 按计划等级保留（Free: 1个月，Pro: 3个月，Team: 6个月） |
| **步骤输入/输出数据** | S3（冷存储） | 与元数据同保留期，超过后自动归档删除 |
| **执行日志** | AWS CloudWatch Logs | 保留 30 天 |
| **错误详情** | PostgreSQL + S3 | 保留与 Task History 相同 |

### 4.3 数据量级

| 指标 | 典型值 | 存储影响 |
|------|-------|---------|
| 单条 Task History 大小 | ~5-50 KB（含步骤数据） | 每月百万级执行约 50GB |
| 步骤输入/输出数据 | ~1-10 KB/步 | 大输出（如文件内容）存 S3 引用 |
| 索引 | zap_id + status + triggered_at | 复合索引支持快速查询 |

---

## 五、连接器凭证存储

### 5.1 凭证生命周期

```
OAuth 2.0 凭证流程：
注册 → Authorization Code → Access Token + Refresh Token → 加密存储 → 定期刷新

存储阶段：
1. 授权码：临时存储（5分钟过期），成功后删除
2. Access Token：加密后存储于 credentials 表，字段级加密
3. Refresh Token：加密后存储，与 Access Token 同表
4. Token 元数据：明文存储（expires_at, scopes, token_type）
```

### 5.2 安全存储方案

| 凭证类型 | 加密方式 | 存储位置 |
|---------|---------|---------|
| OAuth Access Token | AES-256-GCM + KMS 主密钥 | `connections.credentials` 字段 |
| API Key | AES-256-GCM + KMS 主密钥 | `connections.credentials` 字段 |
| Session Cookie | AES-256-GCM + KMS 主密钥 | `connections.credentials` 字段 |
| Basic Auth 密码 | AES-256-GCM + KMS 主密钥 | `connections.credentials` 字段 |

---

## 六、关键设计模式总结

| 设计模式 | Zapier 做法 | 对我们的启示 |
|---------|-----------|------------|
| **编排定义存储** | 步骤配置使用 JSONB 存储在关系数据库中，层次化结构 | JSONB 足够灵活，无需图数据库即可存储编排定义 |
| **参数引用** | 通过 `source + field` 模式引用上游输出，而非硬编码路径 | 声明式参数映射便于序列化和版本管理 |
| **执行历史分离** | 元数据在 PostgreSQL，大字段在 S3，冷热分离 | 按数据访问频率分层存储，控制成本 |
| **版本管理** | 每次保存自动创建快照，支持回滚 | 编排定义不可变版本是核心特性 |
| **凭证加密** | 字段级加密 + KMS 主密钥 + 加密上下文隔离 | 凭证与用户/连接绑定，隔离不同租户的加密上下文 |
| **轮询游标** | Redis + PostgreSQL 双写，保证不丢不错 | 游标持久化是增量处理的核心保障 |

---

## 七、关键数据量估算参考

| 实体 | 日增数据量 | 总数据量（1年） | 查询模式 |
|------|----------|--------------|---------|
| Zap 定义 | — | ~1000 条/企业 | 按用户/应用查询 |
| Task History | ~1000-10000 条/企业 | ~300 万条/年 | 按时间+状态查询 |
| Step 数据快照 | ~5000-50000 条/企业 | ~1500 万条/年 | 按 Task ID 查询 |
| 连接凭证 | — | ~500 条/企业 | 按用户/应用精确查询 |