# Make 连接流编排数据存储调研报告

**版本**：V1.0
**日期**：2026年5月
**调研维度**：连接流编排数据存储（编排定义存储 / 运行时状态 / 执行历史 / Data Store）

---

## 一、概述

Make（前身为 Integromat）的核心工作流单元称为 **Scenario（场景）**。相比 Zapier 的线性 Zap 模型，Make 的场景模型支持 Router（多路分支）、Iterator（迭代器）、Aggregator（聚合器）等复杂控制流。其数据存储体系围绕"模块化场景定义"设计，编排定义的表达能力更强，对应的存储模型也更复杂。Make 运行在 Google Cloud Platform（GCP）上，使用 Cloud SQL（PostgreSQL）、Cloud Storage、Firestore 等基础设施。

---

## 二、编排定义存储（Scenario 配置存储）

### 2.1 核心实体模型

| 实体 | 职责 | 关键字段 |
|------|------|---------|
| **Scenario** | 场景整体配置 | id, name, description, status（inactive/active）, folder_id, team_id, scheduling JSON |
| **Module** | 场景中的单个模块 | id, scenario_id, module_type, app_id, operation, config JSON, output_schema JSON |
| **Connection** | 应用连接实例 | id, name, app_id, credential_data（加密）, account_id |
| **Data Structure** | 数据结构定义 | id, name, fields JSON（字段定义数组）, team_id |
| **Data Store** | 键值数据存储 | id, name, structure_id, size_bytes |

### 2.2 Scenario 存储结构

Scenario 的编排定义以 **有向无环图（DAG）** 的形式存储：

```
scenario = {
  "id": "sc_123456",
  "name": "IM 消息 → 创建工单 → 通知",
  "description": "当 IM 收到消息时自动创建工单并通知相关人员",
  "status": "active",
  "scheduling": {
    "type": "webhook" | "polling" | "scheduled" | "manual",
    "interval_minutes": null,
    "cron_expression": null,
    "webhook_url": "https://hook.make.com/xxxxx"
  },
  "modules": [
    {
      "id": "mod_1",
      "type": "trigger",
      "app": "open-app",
      "operation": "watchMessages",
      "params": {
        "chat_id": { "type": "literal", "value": "chat_123" }
      },
      "output": {
        "message_id": "string",
        "content": "string",
        "sender": "string"
      }
    },
    {
      "id": "mod_2",
      "type": "router",
      "routes": [
        {
          "id": "route_a",
          "label": "工单类型",
          "filter": {
            "operator": "eq",
            "field": "mod_1.content.type",
            "value": "bug"
          }
        },
        {
          "id": "route_b",
          "label": "默认",
          "filter": null
        }
      ]
    },
    {
      "id": "mod_3",
      "type": "action",
      "app": "jira",
      "operation": "createIssue",
      "params": {
        "project": { "type": "literal", "value": "PROJ" },
        "summary": { "type": "reference", "module": "mod_1", "path": "content.title" },
        "description": { "type": "reference", "module": "mod_1", "path": "content.body" }
      },
      "routes": ["route_a"]
    }
  ],
  "connections": [
    {
      "module_id": "mod_1",
      "connection_id": "conn_openapp_001"
    },
    {
      "module_id": "mod_3",
      "connection_id": "conn_jira_001"
    }
  ]
}
```

### 2.3 数据映射存储

Make 的数据映射支持 **内置函数**（300+）和 **条件表达式**：

```
params: {
  "email_body": {
    "type": "expression",
    "value": "formatDate(now; \"YYYY-MM-DD\") ++ \" 日报\\n\\n\" ++ mod_1.content"
  },
  "recipients": {
    "type": "expression",
    "value": "if (mod_1.priority == \"high\"; \"urgent@team.com\"; \"team@team.com\")"
  },
  "attachments": {
    "type": "array_mapping",
    "source": "mod_1.files",
    "mapping": {
      "filename": "item.name",
      "content": "item.content"
    }
  }
}
```

### 2.4 存储技术

| 存储对象 | 技术选型 | 说明 |
|---------|---------|------|
| Scenario 元数据 | PostgreSQL | 关系表，包含调度配置 |
| Module 配置 | PostgreSQL JSONB | `params` 字段使用 JSONB，支持函数表达式和引用 |
| Module 间连线 | PostgreSQL | `connections` 表记录模块间的路由关系（DAG 边） |
| 数据结构定义 | PostgreSQL JSONB | 字段定义以 JSONB 数组存储 |
| Data Store 数据 | Firestore（NoSQL） | 键值存储，支持高并发读写，按订阅计划限制容量 |
| 场景模板 | GCS（Cloud Storage） | JSON 序列化的场景配置模板 |

### 2.5 与 Zapier 的关键差异

| 维度 | Zapier | Make |
|------|--------|------|
| **编排模型** | 线性 Step 列表 + Paths 嵌套 | DAG 图，模块间显式路由关系 |
| **存储结构** | 扁平步骤列表，分支嵌套在 Paths 内 | 模块数组 + 独立的路由表（边） |
| **参数映射** | `source + field` 引用模式 | 表达式函数 + 模块引用路径 |
| **数据存储** | 无内置持久化 | Data Store + Data Structure 作为一等公民 |
| **模板存储** | 平台侧维护 | 用户可保存为私有模板 |

---

## 三、运行时状态存储

### 3.1 执行调度状态

| 状态类型 | 存储方式 | 说明 |
|---------|---------|------|
| **Webhook 注册** | Redis + PostgreSQL | Webhook URL 与 Scenario 的映射关系，支持动态注册/注销 |
| **轮询状态** | Data Store | 每个 Trigger 的轮询进度记录在 Data Store 中 |
| **执行队列** | Redis（Pub/Sub） | Scenario 触发后进入执行队列 |
| **并发限制** | Redis | 基于 Scenario 粒度的并发数控制（按订阅计划） |

### 3.2 Bundle 数据流

Make 的模块间数据传递通过 **Bundle** 实现——一次 Scenario 执行产生多个 Bundle（特别是 Iterator 场景）：

```
Execution = {
  "id": "exec_123",
  "scenario_id": "sc_456",
  "started_at": "...",
  "completed_at": "...",
  "status": "success" | "error" | "warning",
  "cycles": [
    {
      "id": "cycle_1",
      "bundles": [
        { "module_id": "mod_1", "input": {}, "output": {}, "status": "success" },
        { "module_id": "mod_2", "input": {}, "output": {}, "status": "success" },
        { "module_id": "mod_3", "input": {}, "output": {}, "status": "success" }
      ]
    },
    // 每个 Iterator 迭代产生一个 Cycle
  ]
}
```

### 3.3 错误处理机制

Make 的 Error Handler 是**一等公民模块**，存储为特殊的路由：

```
module = {
  "id": "mod_4",
  "type": "error_handler",
  "attached_to": "mod_2",      // 附加到哪个模块
  "strategy": "resume" | "break" | "commit",
  "handler_modules": [
    {
      "id": "mod_5",
      "type": "action",
      "app": "email",
      "operation": "sendAlert",
      "params": { "to": "admin@team.com", "subject": "模块 mod_2 执行失败" }
    }
  ],
  "retry_config": {
    "max_retries": 3,
    "interval_ms": 5000,
    "backoff_multiplier": 2
  }
}
```

---

## 四、执行历史存储

### 4.1 数据结构

Make 的执行历史记录比 Zapier 更详细，包含 **Cycle** 级别和 **Module** 级别的数据：

```
execution_history = {
  "id": "eh_789",
  "scenario_id": "sc_456",
  "scenario_name": "IM → Jira 工单",
  "trigger": {
    "type": "webhook",
    "data": { /* 触发数据快照 */ }
  },
  "started_at": "2026-05-14T10:00:00Z",
  "completed_at": "2026-05-14T10:00:08Z",
  "duration_ms": 8234,
  "status": "success",
  "operations_consumed": 5,
  "data_transferred_kb": 12.5,
  "modules_executed": [
    {
      "module_id": "mod_1",
      "module_label": "Watch Messages",
      "type": "trigger",
      "status": "success",
      "input": { /* 输入快照 */ },
      "output": { /* 输出快照 */ },
      "duration_ms": 1500
    }
  ]
}
```

### 4.2 存储策略

| 存储对象 | 技术选型 | 保留策略 |
|---------|---------|---------|
| 执行历史元数据 | PostgreSQL（按时间分区） | Free: 30天, Core: 60天, Pro/Teams: 90天 |
| 完整执行详情 | BigQuery（分析型） | 长期保留（企业版） |
| 模块输入/输出 | PostgreSQL（近期）+ GCS（归档） | 与执行历史相同保留期 |
| 操作量统计 | PostgreSQL（聚合表） | 按月汇总，永久保留 |
| 执行日志 | Cloud Logging | 30天，可导出 |

### 4.3 数据量估算

| 指标 | 典型值 | 存储策略 |
|------|-------|---------|
| 单次执行记录 | ~2-20 KB（含模块详情） | 热存储（PostgreSQL） |
| 含 Iterator 的执行 | ~50-500 KB（N次迭代 × 模块输出） | 大执行自动归档到 GCS |
| 月聚合统计 | ~100 字节/场景/月 | 永久保留 |

---

## 五、Data Store（内置数据存储）

### 5.1 架构设计

Make 的 Data Store 是其独特的数据持久化能力，本质上是一个 **NoSQL 键值存储**：

| 特性 | 说明 |
|------|------|
| 存储引擎 | Firestore（GCP 托管 NoSQL） |
| 数据结构 | 通过 Data Structure 定义字段 Schema |
| 容量限制 | Free: 1MB, Pro: 50MB, Teams: 500MB, Enterprise: 可扩展 |
| 访问方式 | 通过 Data Store 模块的 CRUD 操作 |
| 跨场景共享 | 同一 Organization 内的场景可访问同一个 Data Store |

### 5.2 典型使用场景

| 场景 | 实现方式 | 存储数据 |
|------|---------|---------|
| 增量同步游标 | Data Store 记录 `{key: "last_poll_time", value: "2026-05-14T10:00:00Z"}` | 每场景 1 条记录 |
| 数据去重 | Data Store 存储已处理记录 ID 集合 | 每次处理增删 |
| 状态共享 | 不同场景间通过 Data Store 共享业务状态 | 跨场景状态同步 |
| ID 映射表 | 存储外部系统 ID ↔ 内部 ID 的映射关系 | 双向映射 |
| 临时缓存 | 缓存频繁查询的 API 响应 | 带过期时间的缓存记录 |

---

## 六、连接器凭证存储

| 凭证类型 | 加密方案 | 存储位置 |
|---------|---------|---------|
| OAuth 2.0 Token | AES-256-GCM + Cloud KMS | PostgreSQL `connections.credential_data` |
| API Key | AES-256-GCM + Cloud KMS | PostgreSQL `connections.credential_data` |
| Basic Auth | AES-256-GCM + Cloud KMS | PostgreSQL `connections.credential_data` |
| 自定义认证 | AES-256-GCM + Cloud KMS | 配置定义的凭证字段加密存储 |

---

## 七、关键设计模式总结

| 设计模式 | Make 做法 | 对我们的启示 |
|---------|----------|------------|
| **DAG 存储** | 模块数组 + 路由表显式存储边关系 | 复杂编排需存储图结构，模块数组 + 独立路由表优于嵌套 |
| **Data Store** | 内置 NoSQL 键值存储作为一等公民 | 提供轻量级内置存储可解决增量/去重/状态共享三大场景 |
| **表达式函数** | 300+ 内置函数直接存储在 params 表达式中 | 函数表达式序列化为 JSON 是声明式配置的最佳实践 |
| **操作量计量** | 每次执行记录 operations_consumed | 从设计第一天就要考虑计量与计费数据的存储 |
| **历史分层** | 热数据 PostgreSQL + 温数据 BigQuery + 冷数据 GCS | 三层冷热分离是执行历史存储的标准模式 |
| **错误处理器模块化** | Error Handler 是一等公民模块，附着在目标模块上 | 错误处理配置与业务模块的关联关系需显式存储 |