# API 接口设计：连接器平台

**Feature ID**: CONN-PLAT-001  
**关联文档**: plan.md (§4.3)  
**版本**: v1.1  
**创建日期**: 2026-05-19  
**最后更新**: 2026-05-20  
**更新说明**: 全文字段名统一为 camelCase（对齐 §1.2 命名规范）；新增接口编号总表、状态枚举定义

---

## 1. 接口规范

> 💡 以下 API 设计规范沿用能力开放平台（CAP-OPEN-001）已确立的标准，确保全项目 API 风格统一。详情见 `../specs-tree-capability-open-platform/plan-api.md §0`。

### 1.1 基础规范

| 规范项 | 说明 |
|--------|------|
| 基础路径 | `/api/v1` |
| 认证方式 | 管理面复用现有 Cookie/SSO；执行面通过连接器配置的认证凭证 |
| 时间格式 | ISO 8601: `yyyy-MM-dd'T'HH:mm:ss.SSSXXX` |

### 1.2 字段命名规范

**规则**：接口入参和返回值字段统一使用驼峰命名（camelCase）。

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| `connectorId` | `connectorId` |
| `createTime` | `create_time` |
| `versionStatus` | `versionStatus` |

**命名约定**：
- ID 字段：使用 `Id` 后缀，如 `connectorId`, `flowId`, `versionId`
- 时间字段：使用 `Time` 后缀，如 `createTime`, `updateTime`；或 `At` 后缀表示时间点，如 `publishedAt`, `expiresAt`
- 布尔字段：使用 `is` 前缀，如 `isDeleted`, `isEnabled`
- URL 字段：使用 `Url` 后缀，如 `iconUrl`, `webhookUrl`

### 1.3 路径命名规范

**规则**：URL 路径使用中划线分隔多个单词（kebab-case）。

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| `/api/v1/connector-versions` | `/api/v1/connector_versions` |
| `/api/v1/test-run` | `/api/v1/testRun` |
| `/api/v1/user-authorizations` | `/api/v1/user_authorizations` |

**命名约定**：
- 资源名称使用复数形式：`/connectors`, `/flows`, `/executions`
- 子资源使用中划线分隔：`/test-run`, `/connector-versions`
- 路径参数使用驼峰：`/connectors/:connectorId/versions`

### 1.4 数据类型规范

**规则**：长整数（如主键 ID）统一返回 string 类型，避免前端接收精度丢失问题。

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| `"connectorId": "con_xxxxx"` | `"id": 100` |
| `"executionId": "exec_xxxxx"` | `"executionId": 200` |

**原因说明**：
- JavaScript 的 `Number` 类型最大安全整数是 `2^53 - 1`（即 `9007199254740991`）
- 统一使用 `string` 类型可彻底避免精度丢失问题

**适用范围**：
- 所有业务 ID 字段：`connectorId`, `flowId`, `versionId`, `executionId` 等
- 所有外键 ID 字段：`creatorAppId`, `approvalId` 等

### 1.5 响应格式规范

所有接口统一使用以下响应格式：

```json
// 成功响应
{
  "code": "200",
  "messageZh": "操作成功",
  "messageEn": "Success",
  "data": { ... },
  "page": null
}

// 分页响应
{
  "code": "200",
  "messageZh": "查询成功",
  "messageEn": "Success",
  "data": [ ... ],
  "page": {
    "curPage": 1,
    "pageSize": 20,
    "total": 123
  }
}

// 错误响应
{
  "code": "400",
  "messageZh": "参数错误",
  "messageEn": "Bad Request",
  "data": null,
  "page": null
}
```

### 1.6 分页请求规范

所有列表接口统一支持分页：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| curPage | int | 否 | 当前页码，从 1 开始，默认 1 |
| pageSize | int | 否 | 每页数量，默认 20，最大 100 |

### 1.7 错误码定义

| 错误码 | 说明 |
|--------|------|
| `200` | 成功 |
| `400` | 参数错误 |
| `401` | 未授权 |
| `403` | 无权限 |
| `404` | 资源不存在 |
| `409` | 状态冲突 |
| `422` | 校验失败 |
| `500` | 内部错误 |

---

## 2. 连接器管理 API

### 2.1 连接器 CRUD

| 方法 | 路径 | 说明 | FR |
|------|------|------|----|
| `POST` | `/api/v1/connectors` | 创建连接器 | FR-001 |
| `GET` | `/api/v1/connectors` | 查询连接器列表 | FR-004 |
| `GET` | `/api/v1/connectors/{connectorId}` | 查询连接器详情 | FR-004 |
| `PUT` | `/api/v1/connectors/{connectorId}` | 更新连接器基本信息 | FR-002 |
| `DELETE` | `/api/v1/connectors/{connectorId}` | 删除连接器 | FR-003 |
| `POST` | `/api/v1/connectors/{connectorId}/list-public` | 上架为公共连接器 | FR-005 |
| `POST` | `/api/v1/connectors/{connectorId}/delist` | 下架连接器 | FR-005 |

#### POST /api/v1/connectors — 创建连接器

```json
// Request
{
  "name": "IM 发送消息",
  "icon": "https://cdn.xxx.com/icons/im.svg",
  "description": "封装 IM 消息发送能力",
  "connectorType": "HTTP",
  "visibility": "private"
}

// Response 201
{
  "connectorId": "con_a1b2c3d4",
  "latestVersion": {
    "versionId": "cv_e5f6g7h8",
    "versionNo": "0.0.1",
    "status": "draft"
  }
}
```

#### GET /api/v1/connectors — 查询列表

```json
// Query params
// ?curPage=1&pageSize=20&visibility=public&connectorType=HTTP&keyword=IM

// Response
{
  "items": [
    {
      "connectorId": "con_a1b2c3d4",
      "name": "IM 发送消息",
      "icon": "https://cdn.xxx.com/icons/im.svg",
      "description": "封装 IM 消息发送能力",
      "connectorType": "HTTP",
      "visibility": "public",
      "latestVersion": "1.2.0",
      "versionStatus": "published",
      "createdAt": "2026-05-19T10:00:00.000+08:00"
    }
  ],
  "total": 1,
  "page": 1,
  "pageSize": 20
}
```

### 2.2 连接器版本管理

| 方法 | 路径 | 说明 | FR |
|------|------|------|----|
| `GET` | `/api/v1/connectors/{connectorId}/versions` | 版本列表 | FR-009 |
| `GET` | `/api/v1/connectors/{connectorId}/versions/{versionId}` | 版本详情（含连接配置） | FR-007 |
| `PUT` | `/api/v1/connectors/{connectorId}/versions/{versionId}` | 编辑草稿版本配置 | FR-008 |
| `POST` | `/api/v1/connectors/{connectorId}/versions/{versionId}/publish` | 发布版本 | FR-010 |

#### PUT /api/v1/connectors/{connectorId}/versions/{versionId} — 编辑连接配置

```json
// Request
{
  "connectionConfig": {
    "protocol": "HTTP",
    "protocolConfig": {
      "baseUrl": "https://openapi.xxx.com/im",
      "method": "POST",
      "headers": { "Content-Type": "application/json" }
    },
    "auth": {
      "type": "AKSK",
      "config": {
        "accessKey": "ak_xxxx",
        "secretKey": "sk_xxxx"
      }
    },
    "inputSchema": {
      "type": "object",
      "properties": {
        "receiver": { "type": "string" },
        "content": { "type": "string" }
      },
      "required": ["receiver", "content"]
    },
    "outputSchema": {
      "type": "object",
      "properties": {
        "msgId": { "type": "string" }
      }
    },
    "timeoutMs": 30000
  }
}

// Response 200
{ "versionId": "cv_e5f6g7h8", "status": "draft", "updatedAt": "..." }
```

#### POST /api/v1/connectors/{connectorId}/versions/{versionId}/publish — 发布

```json
// Request (optional)
{ "changeLog": "新增消息撤回能力" }

// Response 200 (触发审批)
{
  "versionId": "cv_e5f6g7h8",
  "status": "pendingApproval",
  "approvalId": "apr_xxxx"
}
```

### 2.3 连接器使用统计

| 方法 | 路径 | 说明 | FR |
|------|------|------|----|
| `GET` | `/api/v1/connectors/{connectorId}/stats` | 连接器使用统计 | FR-006 |

```json
// Response
{
  "connectorId": "con_a1b2c3d4",
  "referencedByFlows": [
    { "flowId": "flow_xxx", "flowName": "消息通知流", "status": "enabled" }
  ],
  "totalInvocations": 1523,
  "invocationsByVersion": [
    { "versionNo": "1.0.0", "count": 800 },
    { "versionNo": "1.1.0", "count": 723 }
  ],
  "statsPeriod": { "from": "2026-05-01", "to": "2026-05-19" }
}
```

---

## 3. 连接流管理 API

### 3.1 连接流 CRUD

| 方法 | 路径 | 说明 | FR |
|------|------|------|----|
| `POST` | `/api/v1/flows` | 创建连接流 | FR-011 |
| `GET` | `/api/v1/flows` | 查询连接流列表 | FR-014 |
| `GET` | `/api/v1/flows/{flowId}` | 查询连接流详情 | FR-016 |
| `PUT` | `/api/v1/flows/{flowId}` | 更新连接流基本信息 | FR-012 |
| `DELETE` | `/api/v1/flows/{flowId}` | 删除连接流 | FR-013 |
| `POST` | `/api/v1/flows/{flowId}/enable` | 启用连接流 | FR-015 |
| `POST` | `/api/v1/flows/{flowId}/disable` | 停用连接流 | FR-015 |

#### POST /api/v1/flows — 创建连接流

```json
// Request
{
  "name": "新消息自动通知",
  "description": "收到 IM 消息后自动发送通知到OA系统"
}

// Response 201
{
  "flowId": "flow_i9j0k1l2",
  "latestVersion": {
    "versionId": "fv_m3n4o5p6",
    "versionNo": "0.0.1",
    "status": "draft"
  }
}
```

### 3.2 连接流版本管理

| 方法 | 路径 | 说明 | FR |
|------|------|------|----|
| `GET` | `/api/v1/flows/{flowId}/versions` | 版本列表 | FR-018 |
| `GET` | `/api/v1/flows/{flowId}/versions/{versionId}` | 版本详情（含编排配置） | FR-016 |
| `PUT` | `/api/v1/flows/{flowId}/versions/{versionId}` | 保存编排配置（草稿） | FR-017 |
| `POST` | `/api/v1/flows/{flowId}/versions/{versionId}/publish` | 发布版本 | FR-019 |

#### PUT /api/v1/flows/{flowId}/versions/{versionId} — 保存编排配置

```json
// Request — orcherstration_config 全文替换
{
  "orchestrationConfig": {
    "trigger": {
      "type": "event",
      "config": {
        "eventSource": "im:message:receive",
        "scope": "im:message:receive",
        "schema": {
          "type": "object",
          "properties": {
            "sender": { "type": "string" },
            "content": { "type": "string" }
          }
        }
      }
    },
    "nodes": [
      {
        "nodeId": "node_entry",
        "nodeType": "entry",
        "label": "收到消息",
        "position": { "x": 100, "y": 200 }
      },
      {
        "nodeId": "node_1",
        "nodeType": "connector",
        "label": "发送通知",
        "connectorVersionId": "cv_e5f6g7h8",
        "inputMapping": { "receiver": "${trigger.sender}", "content": "${trigger.content}" },
        "retryPolicy": { "maxRetries": 3, "intervalMs": 1000 },
        "position": { "x": 350, "y": 200 }
      },
      {
        "nodeId": "node_exit",
        "nodeType": "exit",
        "label": "输出",
        "outputFields": ["msgId"],
        "position": { "x": 600, "y": 200 }
      }
    ],
    "edges": [
      { "edgeId": "e1", "source": "node_entry", "target": "node_1" },
      { "edgeId": "e2", "source": "node_1", "target": "node_exit" }
    ]
  }
}

// Response 200
{ "versionId": "fv_m3n4o5p6", "status": "draft", "updatedAt": "..." }
```

---

## 4. 运行时执行 API

### 4.1 执行操作

| 方法 | 路径 | 说明 | FR |
|------|------|------|----|
| `POST` | `/api/v1/flows/{flowId}/executions` | 手动触发执行（异步） | FR-025 |
| `POST` | `/api/v1/flows/{flowId}/test-run` | 测试运行 | FR-020 |
| `GET` | `/api/v1/executions/{executionId}/status` | 查询执行状态 | FR-025a |
| `GET` | `/api/v1/flows/{flowId}/executions` | 执行历史列表 | FR-032 |
| `GET` | `/api/v1/executions/{executionId}` | 执行详情（含步骤） | FR-033 |
| `POST` | `/api/v1/executions/{executionId}/retry` | 重试失败执行 | FR-030 |

#### POST /api/v1/flows/{flowId}/executions — 手动触发

```json
// Request
{
  "triggerData": {
    "sender": "user_001",
    "content": "你好，这是一条测试消息"
  }
}

// Response 202 (异步执行，立即返回)
{
  "executionId": "exec_y5z6a7b8",
  "status": "pending",
  "createdAt": "2026-05-19T10:00:00.000+08:00"
}
```

#### GET /api/v1/executions/{executionId}/status — 查询执行状态

```json
// Response
{
  "executionId": "exec_y5z6a7b8",
  "flowId": "flow_i9j0k1l2",
  "status": "running",
  "triggerType": "manual",
  "startedAt": "2026-05-19T10:00:01.000+08:00",
  "finishedAt": null,
  "resultData": null,
  "progress": {
    "totalNodes": 3,
    "completedNodes": 1,
    "currentNode": "node_1",
    "currentNodeLabel": "发送通知"
  }
}
```

```json
// Response (执行成功)
{
  "executionId": "exec_y5z6a7b8",
  "status": "success",
  "startedAt": "2026-05-19T10:00:01.000+08:00",
  "finishedAt": "2026-05-19T10:00:03.250+08:00",
  "durationMs": 2250,
  "resultData": {
    "msgId": "msg_xxxx"
  }
}
```

#### GET /api/v1/executions/{executionId} — 执行详情

```json
// Response
{
  "executionId": "exec_y5z6a7b8",
  "flowId": "flow_i9j0k1l2",
  "versionId": "fv_m3n4o5p6",
  "status": "success",
  "triggerType": "manual",
  "triggerData": { "sender": "user_001", "content": "你好" },
  "resultData": { "msgId": "msg_xxxx" },
  "startedAt": "2026-05-19T10:00:01.000+08:00",
  "finishedAt": "2026-05-19T10:00:03.250+08:00",
  "durationMs": 2250,
  "steps": [
    {
      "stepId": "step_c9d0e1f2",
      "nodeId": "node_entry",
      "nodeName": "收到消息",
      "nodeType": "entry",
      "status": "success",
      "inputData": { "sender": "user_001", "content": "你好" },
      "outputData": { "sender": "user_001", "content": "你好" },
      "startedAt": "2026-05-19T10:00:01.000+08:00",
      "finishedAt": "2026-05-19T10:00:01.010+08:00",
      "durationMs": 10
    },
    {
      "stepId": "step_d0e1f2g3",
      "nodeId": "node_1",
      "nodeName": "发送通知",
      "nodeType": "connector",
      "status": "success",
      "inputData": { "receiver": "user_001", "content": "你好" },
      "outputData": { "msgId": "msg_xxxx", "code": 0 },
      "startedAt": "2026-05-19T10:00:01.020+08:00",
      "finishedAt": "2026-05-19T10:00:03.230+08:00",
      "durationMs": 2210,
      "retryAttempts": 0
    },
    {
      "stepId": "step_e1f2g3h4",
      "nodeId": "node_exit",
      "nodeName": "输出",
      "nodeType": "exit",
      "status": "success",
      "inputData": { "msgId": "msg_xxxx", "code": 0 },
      "outputData": { "msgId": "msg_xxxx" },
      "startedAt": "2026-05-19T10:00:03.235+08:00",
      "finishedAt": "2026-05-19T10:00:03.240+08:00",
      "durationMs": 5
    }
  ]
}
```

#### POST /api/v1/flows/{flowId}/test-run — 测试运行

```json
// Request
{
  "mock_triggerData": {
    "sender": "test_user",
    "content": "这是一条测试消息"
  }
}

// Response 200 (同步返回测试结果，超时自动转为异步)
{
  "executionId": "exec_test_xxxx",
  "status": "success",
  "steps": [ /* 同执行详情 steps */ ],
  "durationMs": 1250,
  "isTest": true
}
```

### 4.2 Webhook 触发

| 方法 | 路径 | 说明 | FR |
|------|------|------|----|
| `POST` | `/api/v1/webhook/{webhookToken}` | Webhook 触发连接流 | FR-023 |

**webhookToken 生成规则**: `wh_` + 32 位随机字符串（不可预测）

```json
// Request — 由外部系统发送，格式由 flow 的 trigger schema 定义
{
  "event": "order.created",
  "orderId": "ORD-20260519-0001",
  "amount": 99.99
}

// Response 202
{ "executionId": "exec_xxxx", "status": "pending" }

// Response 401（签名验证失败）
{ "code": 1002, "message": "signature verification failed" }
```

**Webhook 签名验证**:
- 请求头: `X-Webhook-Signature: {hmac_sha256(secret, body)}`
- 平台使用预共享 secret 验证签名
- 支持限流：单 token 每分钟最多 100 次请求

### 4.3 定时触发配置

**定时触发在编排配置中声明**，运行时由 `ScheduledTriggerService` 管理：

```json
{
  "trigger": {
    "type": "scheduled",
    "config": {
      "cron": "0 0 9 * * ?",
      "timezone": "Asia/Shanghai",
      "description": "每天上午9点执行"
    }
  }
}
```

| Cron 区域 | 说明 |
|-----------|------|
| 启用/停用 | 连接流启用时自动注册定时任务，停用时自动取消 |
| 可视化配置 | 前端提供 Cron 表达式辅助配置（每天/每周/每月/自定义） |
| 高可用 | 定时任务通过分布式锁确保单次触发仅执行一次 |

---

## 5. 监控 API

| 方法 | 路径 | 说明 | FR |
|------|------|------|----|
| `GET` | `/api/v1/monitor/metrics` | 平台运行指标 | FR-034 |
| `GET` | `/api/v1/monitor/metrics/by-connector` | 按连接器统计 | FR-034 |
| `GET` | `/api/v1/flows/{flowId}/status` | 连接流运行状态 | FR-031 |

#### GET /api/v1/monitor/metrics — 运行指标

```json
// Query params
// ?period=7d  (1h/24h/7d/30d)

// Response
{
  "activeFlows": 12,
  "totalExecutions": 4523,
  "successRate": 98.5,
  "avg_durationMs": 1850,
  "p99_durationMs": 5200,
  "executionsByStatus": {
    "success": 4455,
    "failed": 45,
    "timeout": 23
  },
  "executionsByHour": [
    { "time": "2026-05-19T08:00", "count": 120 },
    { "time": "2026-05-19T09:00", "count": 350 }
  ]
}
```

---

## 6. 审批集成 API

复用能力开放平台现有审批接口，新增审批场景类型：

| 现有接口 | 连接器平台使用方式 |
|---------|------------------|
| `POST /api/v1/approvals` | 创建审批单（场景: `connector_publish` / `flow_deploy`） |
| `GET /api/v1/approvals/{id}` | 查询审批状态 |
| `POST /api/v1/approvals/{id}/approve` | 审批通过 → 自动更新版本状态为 `published` |
| `POST /api/v1/approvals/{id}/reject` | 审批驳回 → 版本保持 `draft` 状态 |

---

## 7. MQS 主题定义

| 主题 | 用途 | 生产者 | 消费者 | 消息格式 |
|------|------|--------|--------|---------|
| `cp_trigger_event` | 事件触发连接流 | event-server | FlowScheduler | `{ "eventSource": "...", "eventData": {...}, "timestamp": ... }` |
| `cp_trigger_manual` | 手动触发消息 | ManualTriggerController | FlowScheduler | `{ "flowId": "...", "triggerData": {...}, "executionId": "..." }` |
| `cp_trigger_webhook` | Webhook 触发消息 | WebhookTriggerController | FlowScheduler | `{ "flowId": "...", "webhookData": {...}, "executionId": "..." }` |
| `cp_execution_result` | 执行完成通知 | SequentialExecutor | MonitorService | `{ "executionId": "...", "status": "...", "durationMs": ... }` |

---

## 8. Scope 权限集成

连接器平台复用能力开放平台的 Scope 权限模型：

| 场景 | Scope 验证方式 |
|------|---------------|
| 连接器定义引用内部 API | 连接器创建时校验当前应用是否已有该 API 的 Scope |
| 连接流入口节点使用事件触发 | 版本发布时校验是否已订阅该事件 Scope |
| 运行时连接器调用内部 API | SequentialExecutor 调用 API 网关时携带应用 Scope 凭证 |
| 运行时事件触发匹配连接流 | FlowScheduler 校验连接流对应的 Scope 订阅是否有效 |