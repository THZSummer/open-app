# API 接口设计：连接器平台

**Feature ID**: CONN-PLAT-001  
**关联文档**: plan.md (§4.3)  
**版本**: v2.0  
**创建日期**: 2026-05-21  
**更新说明**: 对齐 spec.md v4.0——移除 Scope/审批/事件/定时触发，执行改为同步，FR 重编号 1~25

---

## 0. 主要变更说明（与 v1.x 的差异）

| 变更项 | v1.x（基于 spec v3.x） | v2.0（基于 spec v4.0） |
|--------|----------------------|----------------------|
| 执行接口 | **异步**（返回 202 + 轮询状态） | **同步**（直接返回执行结果） |
| 触发方式 | 事件/Webhook/定时/手动 | HTTP/手动 |
| Scope 权限 | ✅ 有 Scope 集成章节 | ❌ 移除（NG18，V1） |
| 审批集成 | ✅ 有审批集成章节 | ❌ 移除（NG19，V1） |
| MQS 主题 | 4 个主题 | ❌ 移除（无 MQS） |
| 监控 API | 全指标仪表盘 | 执行历史查询 |
| FR 引用 | ~37 个 | 25 个 |

---

## 1. 接口规范

> 💡 以下 API 设计规范沿用能力开放平台（CAP-OPEN-001）已确立的标准，确保全项目 API 风格统一。详情见 `../specs-tree-capability-open-platform/plan-api.md §0`。

### 1.1 基础规范

| 规范项 | 说明 |
|--------|------|
| 基础路径 | `/api/v1` |
| 认证方式 | 管理面复用现有 Cookie/SSO；执行面 HTTP 触发通过签名验证 |
| 时间格式 | ISO 8601: `yyyy-MM-dd'T'HH:mm:ss.SSSXXX` |

### 1.2 字段命名规范

**规则**：接口入参和返回值字段统一使用驼峰命名（camelCase）。

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| `connectorId` | `connector_id` |
| `createTime` | `create_Time` |
| `versionStatus` | `version_status` |

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
| `429` | 触发频率超限（FR-024 默认限流） |
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

#### POST /api/v1/connectors — 创建连接器

```json
// Request
{
  "name": "IM 发送消息",
  "icon": "https://cdn.xxx.com/icons/im.svg",
  "description": "封装 IM 消息发送能力",
  "connectorType": "HTTP"
}

// Response 201
{
  "connectorId": "con_a1b2c3d4",
  "name": "IM 发送消息",
  "connectorType": "HTTP",
  "createdAt": "2026-05-21T10:00:00.000+08:00"
}
// 注意：创建后仅生成连接器基本信息，不自动生成草稿版本
```

#### GET /api/v1/connectors — 查询列表

```json
// Query params: ?curPage=1&pageSize=20&connectorType=HTTP&keyword=IM

// Response
{
  "items": [
    {
      "connectorId": "con_a1b2c3d4",
      "name": "IM 发送消息",
      "icon": "https://cdn.xxx.com/icons/im.svg",
      "description": "封装 IM 消息发送能力",
      "connectorType": "HTTP",
      "latestVersionNo": "1.2.0",
      "latestVersionStatus": "published",
      "createdAt": "2026-05-21T10:00:00.000+08:00"
    }
  ],
  "total": 1,
  "curPage": 1,
  "pageSize": 20
}
```

### 2.2 连接器版本管理

| 方法 | 路径 | 说明 | FR |
|------|------|------|----|
| `GET` | `/api/v1/connectors/{connectorId}/versions` | 版本列表 | FR-007 |
| `GET` | `/api/v1/connectors/{connectorId}/versions/{versionId}` | 版本详情（含连接配置） | FR-005 |
| `PUT` | `/api/v1/connectors/{connectorId}/versions/{versionId}` | 编辑草稿版本配置 | FR-006 |
| `POST` | `/api/v1/connectors/{connectorId}/versions/{versionId}/publish` | 发布版本 | FR-008 |

#### PUT /api/v1/connectors/{connectorId}/versions/{versionId} — 编辑连接配置

```json
// Request — connectionConfig 全文替换
{
  "connectionConfig": {
    "protocol": "HTTP",
    "protocolConfig": {
      "url": "https://openapi.xxx.com/im/send",
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
        "receiver": { "type": "string", "description": "接收者ID" },
        "content": { "type": "string", "description": "消息内容" }
      },
      "required": ["receiver", "content"]
    },
    "outputSchema": {
      "type": "object",
      "properties": {
        "msgId": { "type": "string", "description": "消息ID" }
      }
    },
    "timeoutMs": 30000,
    "rateLimit": {
      "maxPerSecond": 10,
      "maxConcurrent": 5
    }
  }
}

// Response 200
{ "versionId": "cv_e5f6g7h8", "status": "draft", "updatedAt": "2026-05-21T10:00:00.000+08:00" }
```

#### POST /api/v1/connectors/{connectorId}/versions/{versionId}/publish — 发布

```json
// Request
{
  "versionNo": "1.0.0",
  "changeLog": "初始版本，支持文本消息发送"
}

// Response 200
{
  "versionId": "cv_e5f6g7h8",
  "versionNo": "1.0.0",
  "status": "published",
  "publishedAt": "2026-05-21T10:00:00.000+08:00"
}
// 注意：本版本发布无需审批（NG19，V1 阶段引入）
```

---

## 3. 连接流管理 API

### 3.1 连接流 CRUD

| 方法 | 路径 | 说明 | FR |
|------|------|------|----|
| `POST` | `/api/v1/flows` | 创建连接流 | FR-009 |
| `GET` | `/api/v1/flows` | 查询连接流列表 | FR-012 |
| `GET` | `/api/v1/flows/{flowId}` | 查询连接流详情 | FR-016（配置查看） |
| `PUT` | `/api/v1/flows/{flowId}` | 更新连接流基本信息 | FR-010 |
| `DELETE` | `/api/v1/flows/{flowId}` | 删除连接流 | FR-011 |
| `POST` | `/api/v1/flows/{flowId}/deploy` | 部署连接流 | FR-013 |
| `POST` | `/api/v1/flows/{flowId}/start` | 启动连接流 | FR-014 |
| `POST` | `/api/v1/flows/{flowId}/stop` | 停止连接流 | FR-015 |

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
  "name": "新消息自动通知",
  "status": "disabled",
  "createdAt": "2026-05-21T10:00:00.000+08:00"
}
// 注意：创建后仅生成连接流基本信息，不自动生成草稿版本
```

#### POST /api/v1/flows/{flowId}/deploy — 部署

```json
// Request
{
  "versionId": "fv_m3n4o5p6"
}

// Response 200
{
  "flowId": "flow_i9j0k1l2",
  "deployedVersionId": "fv_m3n4o5p6",
  "status": "enabled",
  "deployedAt": "2026-05-21T10:00:00.000+08:00"
}
// 注意：部署无需审批（NG19，V1 阶段引入）
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
// Request — orchestrationConfig 全文替换
{
  "orchestrationConfig": {
    "trigger": {
      "type": "http",     
      "config": {
        "method": "POST",
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
        "label": "接收请求",
        "position": { "x": 100, "y": 200 }
      },
      {
        "nodeId": "node_1",
        "nodeType": "connector",
        "label": "发送通知",
        "connectorVersionId": "cv_e5f6g7h8",
        "inputMapping": {
          "receiver": "${trigger.sender}",
          "content": "${trigger.content}"
        },
        "position": { "x": 350, "y": 200 }
      },
      {
        "nodeId": "node_2",
        "nodeType": "data_processor",
        "label": "格式化消息",
        "config": {
          "fieldMappings": [
            { "source": "${node_1.msgId}", "target": "result.id" },
            { "source": "constant:success", "target": "result.status" }
          ]
        },
        "position": { "x": 500, "y": 200 }
      },
      {
        "nodeId": "node_exit",
        "nodeType": "exit",
        "label": "返回结果",
        "outputFields": ["result.id", "result.status"],
        "position": { "x": 650, "y": 200 }
      }
    ],
    "edges": [
      { "edgeId": "e1", "source": "node_entry", "target": "node_1" },
      { "edgeId": "e2", "source": "node_1", "target": "node_2" },
      { "edgeId": "e3", "source": "node_2", "target": "node_exit" }
    ]
  }
}

// Response 200
{ "versionId": "fv_m3n4o5p6", "status": "draft", "updatedAt": "2026-05-21T10:00:00.000+08:00" }
```

#### POST /api/v1/flows/{flowId}/versions/{versionId}/publish — 发布

```json
// Request
{
  "versionNo": "1.0.0",
  "changeLog": "初始版本"
}

// Response 200
{
  "versionId": "fv_m3n4o5p6",
  "versionNo": "1.0.0",
  "status": "published",
  "publishedAt": "2026-05-21T10:00:00.000+08:00"
}
// 注意：本版本发布无需审批（NG19，V1 阶段引入）
```

---

## 4. 运行时执行 API

### 4.1 执行操作（同步）

| 方法 | 路径 | 说明 | FR |
|------|------|------|----|
| `POST` | `/api/v1/flows/{flowId}/executions` | 手动触发执行（同步） | FR-022 |
| `POST` | `/api/v1/flows/{flowId}/test-run` | 测试运行（同步） | FR-020 |
| `GET` | `/api/v1/executions/{executionId}` | 执行详情 | FR-025 |
| `GET` | `/api/v1/flows/{flowId}/executions` | 执行历史列表 | FR-025 |

#### POST /api/v1/flows/{flowId}/executions — 手动触发（同步）

```json
// Request
{
  "triggerData": {
    "sender": "user_001",
    "content": "你好，这是一条测试消息"
  }
}

// Response 200（同步返回完整执行结果）
{
  "executionId": "exec_y5z6a7b8",
  "flowId": "flow_i9j0k1l2",
  "status": "success",
  "triggerType": "manual",
  "startedAt": "2026-05-21T10:00:01.000+08:00",
  "finishedAt": "2026-05-21T10:00:03.250+08:00",
  "durationMs": 2250,
  "resultData": { "msgId": "msg_xxxx" },
  "steps": [
    {
      "stepId": "step_c9d0e1f2",
      "nodeId": "node_entry",
      "nodeName": "接收请求",
      "nodeType": "entry",
      "status": "success",
      "inputData": { "sender": "user_001", "content": "你好" },
      "outputData": { "sender": "user_001", "content": "你好" },
      "startedAt": "2026-05-21T10:00:01.000+08:00",
      "finishedAt": "2026-05-21T10:00:01.010+08:00",
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
      "startedAt": "2026-05-21T10:00:01.020+08:00",
      "finishedAt": "2026-05-21T10:00:03.230+08:00",
      "durationMs": 2210,
      "errorMessage": null
    },
    {
      "stepId": "step_e1f2g3h4",
      "nodeId": "node_exit",
      "nodeName": "返回结果",
      "nodeType": "exit",
      "status": "success",
      "inputData": { "msgId": "msg_xxxx" },
      "outputData": { "msgId": "msg_xxxx" },
      "startedAt": "2026-05-21T10:00:03.235+08:00",
      "finishedAt": "2026-05-21T10:00:03.240+08:00",
      "durationMs": 5
    }
  ]
}

// Response 200（执行超时）
{
  "executionId": "exec_y5z6a7b8",
  "status": "timeout",
  "startedAt": "2026-05-21T10:00:01.000+08:00",
  "finishedAt": "2026-05-21T10:05:01.000+08:00",
  "durationMs": 300000,
  "errorMessage": "执行超时，已强制终止"
}

// Response 200（执行失败—默认错误处理 FR-023）
{
  "executionId": "exec_y5z6a7b8",
  "status": "failed",
  "errorMessage": "节点 '发送通知' 执行失败: HTTP 503 服务不可用",
  "steps": [
    { "nodeId": "node_entry", "status": "success", ... },
    {
      "nodeId": "node_1",
      "nodeName": "发送通知",
      "status": "failed",
      "errorMessage": "HTTP 503 服务不可用",
      ...
    }
  ]
}
```

#### POST /api/v1/flows/{flowId}/test-run — 测试运行（同步）

```json
// Request
{
  "mockTriggerData": {
    "sender": "test_user",
    "content": "测试消息"
  }
}

// Response 200（同步返回）
{
  "executionId": "exec_test_xxxx",
  "status": "success",
  "isTest": true,
  "steps": [ /* 同执行详情 steps */ ],
  "durationMs": 1250
}
```

### 4.2 HTTP 触发（同步）

| 方法 | 路径 | 说明 | FR |
|------|------|------|----|
| `POST` | `/api/v1/trigger/{flowId}/invoke` | HTTP 触发连接流（同步执行，返回结果） | FR-021 |

**触发器配置位置**: 触发器配置（含触发类型、认证类型 schema、入参 Schema、限流）完整内嵌于 `flow_version_t.orchestration_config.trigger` JSON（v2.7.3 决策），**不单独维护触发端点表**。HTTP 触发查找路径：`flow_id` → `flow_t.current_published_version_id` → `flow_version_t.orchestration_config.trigger`，全程主键索引查询。

**触发前置校验**：
1. `flow_t.lifecycle_status = 1`（running 状态，FR-013~015 控制）
2. `current_published_version_id` 非空（至少有一个已发布版本）
3. 请求凭证（由调用方在 Header/Query 中携带，不存储）匹配 `trigger.auth_type_schema` 声明的类型
4. 触发频率未超 `trigger.rate_limit.qpm` 阈值

```json
// Request — 由外部系统发送
// Header: Authorization: Bearer xxxxxx （凭证由调用方携带，不在数据库存储；具体认证类型由 trigger.auth_type_schema 声明）
// Body 格式由 trigger.in_param_schema 校验
{
  "sender": "external_system",
  "content": "这是一条外部消息"
}

// Response 200（同步执行完成）
{
  "executionId": "exec_xxxx",
  "status": "success",
  "resultData": { "msgId": "msg_xxxx" },
  "durationMs": 2250
}

// Response 401（认证失败）
{ "code": "401", "messageZh": "认证失败", "messageEn": "Authentication failed" }

// Response 403（连接流未启用或未发布）
{ "code": "403", "messageZh": "连接流未启用或无已发布版本", "messageEn": "Flow not running or no published version" }

// Response 429（超过限流阈值 FR-024）
{ "code": "429", "messageZh": "请求频率超限，请稍后重试", "messageEn": "Too many requests" }
```

**HTTP 触发认证说明**:
- 认证凭证（如 Bearer Token / API Key / OAuth2 Access Token）由调用方在请求中携带（Header 或 Query），**平台不存储任何凭证**（v2.6 决策）
- 平台仅根据 `trigger.auth_type_schema` 校验凭证格式合法性（如类型、长度、是否必填）；具体凭证有效性由下游连接器调用时验证
- 限流：基于 `trigger.rate_limit.qpm`，按 `flow_id` 维度限流（V1 可扩展为按 IP / 凭证维度）

---

## 5. 执行历史 API

| 方法 | 路径 | 说明 | FR |
|------|------|------|----|
| `GET` | `/api/v1/flows/{flowId}/executions` | 执行历史列表 | FR-025 |
| `GET` | `/api/v1/executions/{executionId}` | 执行详情（含步骤） | FR-025 |

#### GET /api/v1/flows/{flowId}/executions — 执行历史列表

```json
// Query params: ?curPage=1&pageSize=20&status=failed&from=2026-05-01&to=2026-05-21

// Response
{
  "items": [
    {
      "executionId": "exec_y5z6a7b8",
      "flowId": "flow_i9j0k1l2",
      "triggerType": "http",
      "status": "success",
      "startedAt": "2026-05-21T10:00:01.000+08:00",
      "finishedAt": "2026-05-21T10:00:03.250+08:00",
      "durationMs": 2250,
      "resultData": { "msgId": "msg_xxxx" }
    },
    {
      "executionId": "exec_a1b2c3d4",
      "flowId": "flow_i9j0k1l2",
      "triggerType": "manual",
      "status": "failed",
      "startedAt": "2026-05-21T09:55:01.000+08:00",
      "finishedAt": "2026-05-21T09:55:05.120+08:00",
      "durationMs": 4120,
      "errorMessage": "节点 '发送通知' 执行失败: 连接超时"
    }
  ],
  "total": 2,
  "curPage": 1,
  "pageSize": 20
}
```

> 💡 **与 v1.x 的差异**：监控范围精简为执行历史查询（FR-025），移除全指标仪表盘 API。

---

## 6. 接口编号总表

| 编号 | 方法 | 路径 | 所属模块 | 所属服务 | FR |
|------|------|------|---------|---------|----|
| API-001 | POST | `/api/v1/connectors` | connector | open-server | FR-001 |
| API-002 | GET | `/api/v1/connectors` | connector | open-server | FR-004 |
| API-003 | GET | `/api/v1/connectors/{connectorId}` | connector | open-server | FR-004 |
| API-004 | PUT | `/api/v1/connectors/{connectorId}` | connector | open-server | FR-002 |
| API-005 | DELETE | `/api/v1/connectors/{connectorId}` | connector | open-server | FR-003 |
| API-006 | GET | `/api/v1/connectors/{connectorId}/versions` | connector | open-server | FR-007 |
| API-007 | GET | `/api/v1/connectors/{connectorId}/versions/{versionId}` | connector | open-server | FR-005 |
| API-008 | PUT | `/api/v1/connectors/{connectorId}/versions/{versionId}` | connector | open-server | FR-006 |
| API-009 | POST | `/api/v1/connectors/{connectorId}/versions/{versionId}/publish` | connector | open-server | FR-008 |
| API-010 | POST | `/api/v1/flows` | flow | open-server | FR-009 |
| API-011 | GET | `/api/v1/flows` | flow | open-server | FR-012 |
| API-012 | GET | `/api/v1/flows/{flowId}` | flow | open-server | FR-016 |
| API-013 | PUT | `/api/v1/flows/{flowId}` | flow | open-server | FR-010 |
| API-014 | DELETE | `/api/v1/flows/{flowId}` | flow | open-server | FR-011 |
| API-015 | POST | `/api/v1/flows/{flowId}/deploy` | flow | open-server | FR-013 |
| API-016 | POST | `/api/v1/flows/{flowId}/start` | flow | open-server | FR-014 |
| API-017 | POST | `/api/v1/flows/{flowId}/stop` | flow | open-server | FR-015 |
| API-018 | GET | `/api/v1/flows/{flowId}/versions` | flow | open-server | FR-018 |
| API-019 | GET | `/api/v1/flows/{flowId}/versions/{versionId}` | flow | open-server | FR-016 |
| API-020 | PUT | `/api/v1/flows/{flowId}/versions/{versionId}` | flow | open-server | FR-017 |
| API-021 | POST | `/api/v1/flows/{flowId}/versions/{versionId}/publish` | flow | open-server | FR-019 |
| API-022 | POST | `/api/v1/flows/{flowId}/executions` | runtime（手动调试） | **open-server** debug 代理 → **connector-api** debug-api | FR-022 |
| API-023 | POST | `/api/v1/flows/{flowId}/test-run` | runtime（测试运行） | **open-server** debug 代理 → **connector-api** debug-api | FR-020 |
| API-024 | POST | `/api/v1/trigger/{flowId}/invoke` | runtime（HTTP 触发） | **connector-api** http-trigger（对外消费方直连） | FR-021 |
| API-025 | GET | `/api/v1/flows/{flowId}/executions` | monitor（执行列表查询） | **open-server** monitor | FR-025 |
| API-026 | GET | `/api/v1/executions/{executionId}` | monitor（执行详情查询） | **open-server** monitor | FR-025 |

> **总计**：26 个 HTTP 端点，覆盖 25 个 FR（FR-025 对应 2 个端点：执行列表 + 执行详情）
>
> **端点服务归属**（plan.md v2.0 修订后）：
> - **open-server**（端口 18080，上下文根 `/open-server`）：API-001 ~ API-021（管理）+ API-022/API-023（前端调试代理）+ API-025/API-026（监控查询）
> - **connector-api**（端口 18180，上下文根 `/connector-api`）：API-024（对外 HTTP 触发）+ 内部调试接口（被 API-022/API-023 转发调用，端点路径详见 plan-api §debug-api 章节后续补充）
>
> **与 v1.x 的差异**：从 ~33 个端点精简为 26 个，移除审批集成（3 个）、Scope 集成（1 个）、MQS 主题（4 个）、监控仪表盘（2 个）、事件/定时/Webhook 触发接口（3 个）；同时**新增 1 条内部调试接口**（connector-api 暴露给 open-server 内网调用）

---

## 7. 状态枚举定义

### 执行状态 (ExecutionRecord.status)

| 枚举值 | 说明 | 使用场景 |
|--------|------|---------|
| `success` | 执行成功 | 所有节点执行成功，正常返回 |
| `failed` | 执行失败 | 某个节点执行失败（FR-023 默认错误处理） |
| `timeout` | 执行超时 | 执行超过配置的超时时间，强制终止 |

> **与 v1.x 的差异**：移除 `pending` / `running` 状态——同步执行在执行完成前不写入记录，only success/failed/timeout 三种终态。

### 执行步骤状态 (ExecutionStep.status)

| 枚举值 | 说明 |
|--------|------|
| `success` | 步骤执行成功 |
| `failed` | 步骤执行失败 |

### 触发方式 (trigger_type)

| 枚举值 | 说明 | MVP |
|--------|------|:---:|
| `http` | HTTP 触发（FR-021） | ✅ |
| `manual` | 手动触发（FR-022） | ✅ |
| `test` | 测试运行（FR-020） | ✅ |

> **与 v1.x 的差异**：移除 `event` / `webhook` / `scheduled` 类型（V1 阶段引入）

### 版本状态

| 枚举值 | 说明 |
|--------|------|
| `draft` | 草稿版本（可编辑） |
| `published` | 已发布版本（只读，可被引用） |

### 连接器协议类型

| 枚举值 | 说明 | MVP |
|--------|------|:---:|
| `HTTP` | HTTP 协议 | ✅ |

> **与 v1.x 的差异**：移除 `MySQL` / `Redis` / `Kafka` / `gRPC` 等非 HTTP 协议（NG12，V1 阶段）

### 节点类型

| 枚举值 | 说明 | MVP |
|--------|------|:---:|
| `entry` | 入口节点 | ✅ |
| `connector` | 连接器节点 | ✅ |
| `data_processor` | 数据处理节点 | ✅ |
| `exit` | 出口节点 | ✅ |

> **与 v1.x 的差异**：新增 `data_processor` 节点类型（spec v4.0 将其纳入 MVP 范围）