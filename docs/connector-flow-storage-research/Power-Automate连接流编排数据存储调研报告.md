# Microsoft Power Automate 连接流编排数据存储调研报告

**版本**：V1.0
**日期**：2026年5月
**调研维度**：连接流编排数据存储（编排定义存储 / 运行时状态 / 执行历史 / Dataverse）

---

## 一、概述

Power Automate 是微软 Power Platform 的核心组件，其工作流定义基于 **Workflow Definition Language**，以 JSON 格式序列化存储。与 Zapier/Make 不同，Power Automate 深度集成 **Microsoft Dataverse**（Common Data Service）作为统一数据平台，同时利用 Azure 基础设施（Azure SQL、Cosmos DB、Blob Storage）实现分层存储。Power Automate 的编排定义存储具有**强类型、Schema 驱动**的特点。

---

## 二、编排定义存储（Flow 配置存储）

### 2.1 核心实体模型

| 实体 | 职责 | 存储位置 |
|------|------|---------|
| **Flow**（Cloud Flow） | 工作流定义 | Dataverse `workflows` 表 |
| **Flow Definition** | 工作流 JSON 定义 | Dataverse 大字段 / Blob Storage |
| **Connection Reference** | 连接引用 | Dataverse `connectionreferences` 表 |
| **Connector** | 连接器定义 | Dataverse `connectors` 表 |
| **Environment** | 隔离环境 | Dataverse 环境级隔离 |
| **Solution** | 解决方案包 | Dataverse 解决方案层 |

### 2.2 Flow Definition 存储结构

Power Automate 的工作流定义采用 **Workflow Definition Language**，以 JSON 格式存储：

```json
{
  "definition": {
    "$schema": "https://schema.management.azure.com/.../workflowDefinition.json",
    "contentVersion": "1.0.0.0",
    "triggers": {
      "manual": {
        "type": "Request",
        "kind": "Http",
        "inputs": {
          "schema": {
            "properties": {
              "email": { "type": "string" },
              "subject": { "type": "string" }
            },
            "required": [ "email", "subject" ]
          }
        }
      }
    },
    "actions": {
      "Send_an_email": {
        "type": "ApiConnection",
        "inputs": {
          "host": {
            "connectionName": "shared_office365",
            "operationId": "SendEmail",
            "apiId": "/providers/.../office365"
          },
          "parameters": {
            "email": { "type": "string" },
            "subject": "@{triggerBody()?['subject']}",
            "body": "邮件正文内容"
          },
          "authentication": "@parameters('$authentication')"
        },
        "runAfter": {}
      },
      "Post_message_in_Teams": {
        "type": "ApiConnection",
        "inputs": {
          "host": {
            "connectionName": "shared_teams",
            "operationId": "PostMessage",
            "apiId": "/providers/.../teams"
          },
          "parameters": {
            "channelId": "general",
            "message": "@{outputs('Send_an_email')?['body']}"
          }
        },
        "runAfter": {
          "Send_an_email": [ "Succeeded" ]
        }
      }
    }
  },
  "parameters": {
    "$authentication": {
      "type": "secureobject",
      "value": "…"
    }
  }
}
```

### 2.3 Actions 类型枚举

| Action 类型 | 存储方式 | 说明 |
|-----------|---------|------|
| **ApiConnection** | JSON 对象 + Connector 引用 | 标准连接器调用 |
| **Http** | JSON 对象 + URL/Method/Headers | HTTP 请求 |
| **Compose** | JSON 表达式 | 数据组合 |
| **Condition** | 嵌套 JSON（actions + expressions） | if/else 分支 |
| **Switch** | 嵌套 JSON（cases + default） | 多分支 |
| **Apply_to_each** | 嵌套 JSON（foreach） | 循环迭代 |
| **Scope** | 嵌套 JSON（actionGroups） | 作用域分组 |
| **Parallel** | 并行分支定义 | 并行执行 |
| **Wait** | 时间配置 | 延迟/等待 |

### 2.4 表达式存储

Power Automate 使用 **表达式语言** 进行数据映射，存储在 action 的 inputs 中：

```
表达式语法：@{ <expression> }
示例：
  @triggerBody()?['email']                    // 引用触发器的 email 字段
  @outputs('Send_an_email')?['body']          // 引用前序步骤输出
  @concat('Hello ', parameters('$name'))      // 字符串拼接
  @formatDateTime(utcnow(), 'yyyy-MM-dd')     // 日期格式化
  @if(greater(items('Apply_to_each')?['amount'], 1000), 'high', 'low')  // 条件
```

### 2.5 存储技术

| 存储对象 | 技术选型 | 说明 |
|---------|---------|------|
| Flow 元数据 | Dataverse（Azure SQL 后端） | 关系型表，含状态/所有者/创建时间等 |
| Flow Definition | Dataverse 大文本字段 / Blob | 完整 JSON 定义存储 |
| Connection Reference | Dataverse | 连接引用与环境绑定 |
| 连接器元数据 | Dataverse | 标准连接器和自定义连接器定义 |
| 环境隔离 | Dataverse Environment | 每个 Environment 逻辑隔离数据 |
| 解决方案打包 | Dataverse Solution | 支持 ALM（应用生命周期管理） |

### 2.6 版本与 ALM

Power Automate 提供企业级的版本管理和 ALM 支持：

| 能力 | 实现方式 | 存储策略 |
|------|---------|---------|
| **版本历史** | Dataverse `flowversions` 表 | 每次保存生成版本快照，保留最近 25 个版本 |
| **解决方案导出** | .zip 包（含定义 JSON + 连接引用） | 导出为 Dataverse Solution |
| **环境迁移** | 解决方案导入/导出 | 开发 → 测试 → 生产 环境间迁移 |
| **Git 集成** | 通过 Power Platform CLI + Git | 定义 JSON 可落入 Git 仓库进行源码管理 |

---

## 三、运行时状态存储

### 3.1 Flow Run 数据结构

每次 Flow 执行生成一条 **Run** 记录：

```
run = {
  "name": "run_abc123",
  "flow_id": "flow_456",
  "status": "Succeeded" | "Failed" | "Cancelled" | "Running",
  "trigger": {
    "name": "manual",
    "inputsLink": { "uri": "https://...", "contentSize": 1024 },
    "outputsLink": { "uri": "https://...", "contentSize": 512 }
  },
  "startTime": "2026-05-14T10:00:00Z",
  "endTime": "2026-05-14T10:00:05Z",
  "outputs": {
    "Send_an_email": {
      "status": "Succeeded",
      "startTime": "...",
      "endTime": "...",
      "inputsLink": { "uri": "...", "contentSize": 2048 },
      "outputsLink": { "uri": "...", "contentSize": 1024 }
    }
  },
  "properties": {
    "billingMetrics": {
      "actionExecutions": 3,
      "triggerExecutions": 1
    }
  }
}
```

### 3.2 运行时存储技术

| 存储对象 | 技术选型 | 说明 |
|---------|---------|------|
| Flow Run 元数据 | Cosmos DB / Azure Table Storage | 高吞吐写入，最终一致性 |
| Action I/O 数据 | Azure Blob Storage | 每个 Action 的输入/输出数据存储为 Blob |
| 执行队列 | Azure Service Bus | 触发消息入队，Worker 消费执行 |
| 并发锁 | Azure Redis Cache | Flow 级并发控制 |
| 重试状态 | Azure Table Storage | 记录每个 Action 的重试计数和下次重试时间 |

### 3.3 重试与错误处理

```
action: {
  "retryPolicy": {
    "type": "exponential" | "fixed" | "none",
    "interval": "PT20S",     // ISO 8601 持续时间
    "count": 4,
    "minimumInterval": "PT5S",
    "maximumInterval": "PT1H"
  },
  "runAfter": {
    "PreviousAction": [ "Succeeded", "Failed", "Skipped", "TimedOut" ]
  }
}
```

**runAfter 语义**：
- `"Succeeded"`：前序成功才执行（默认）
- `"Failed"`：前序失败时执行（错误处理分支）
- `"Skipped"`：前序被跳过时执行
- `"TimedOut"`：前序超时时执行
- 可组合：`["Succeeded", "Failed"]` 表示无论成功失败都执行

---

## 四、执行历史存储

### 4.1 Run History 结构

| 存储对象 | 技术选型 | 保留策略 |
|---------|---------|---------|
| Run 元数据 | Cosmos DB | 28 天（默认），可配置延长到 90 天 |
| Action I/O 详情 | Azure Blob Storage | 与 Run 元数据相同保留期 |
| 聚合统计 | Azure SQL Database | 按月汇总，永久保留 |
| 审计日志 | Office 365 Audit Log | 90 天（含 Power Platform 操作审计） |
| Analytics 数据 | Dataverse Analytics | 自定义报告和分析 |

### 4.2 数据引用机制

Power Automate 使用 **Link 模式** 避免嵌入式大字段：

```
// Run 记录中不直接存储输入/输出数据
// 而是存储 URI 引用
trigger: {
  "inputsLink": {
    "uri": "https://.../runs/run_123/actions/trigger/inputs",
    "contentSize": 512,
    "contentVersion": "..."  // 用于一致性校验
  },
  "outputsLink": {
    "uri": "https://.../runs/run_123/actions/trigger/outputs",
    "contentSize": 2048,
    "contentVersion": "..."
  }
}
```

---

## 五、连接器凭证存储

### 5.1 凭证管理体系

| 层级 | 存储 | 说明 |
|------|------|------|
| **Connection** | Dataverse `connections` | 连接引用，包含加密后的凭证 |
| **Gateway** | On-Premises Data Gateway | 本地数据网关凭证管理 |
| **Key Vault** | Azure Key Vault | 自定义连接器的敏感配置 |
| **Environment Variables** | Dataverse环境变量 | 非敏感配置参数 |

### 5.2 安全机制

| 安全维度 | 实现方式 |
|---------|---------|
| **传输加密** | TLS 1.2+ 所有 API 调用 |
| **静态加密** | Azure Storage Service Encryption (AES-256) |
| **凭证加密** | Azure Key Vault 托管密钥，字段级加密 |
| **DLP 策略** | 数据防泄漏策略，控制连接器间数据流动 |
| **条件访问** | Azure AD 条件访问策略控制 Flow 执行上下文 |
| **审计** | Office 365 审计日志记录所有管理操作 |

---

## 六、关键设计模式总结

| 设计模式 | Power Automate 做法 | 对我们的启示 |
|---------|-------------------|------------|
| **Workflow Definition JSON** | 标准化的工作流定义语言，采用声明式 JSON Schema | 定义语言标准化是实现 Flow 互操作和版本管理的基础 |
| **runAfter 依赖模型** | 显式声明 Action 间的执行依赖和条件 | 比隐式步骤顺序更灵活，支持条件执行和错误路由 |
| **Link 模式（I/O 分离）** | 输入/输出数据通过 URI 引用，不嵌入 Run 记录 | 大字段外置是执行历史存储的核心模式 |
| **Solution ALM** | 解决方案包支持环境迁移和版本管理 | 企业级编排必须从设计初支持 CI/CD |
| **表达式语言** | 内嵌表达式在 Action 定义中，支持动态数据映射 | 表达式序列化存储是声明式编排的关键能力 |
| **Dataverse 统一数据平台** | Flow 定义 + 连接引用 + 环境配置统一存储 | 统一数据模型降低跨实体查询复杂度 |
| **分层计量** | Run、Action、Trigger 三级计量，支持精细化计费 | 计量粒度决定后续收费和数据统计能力 |