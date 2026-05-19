# API 接口设计：连接器平台

**Feature ID**: CONN-PLAT-001  
**关联文档**: plan.md (§4.3)  
**版本**: v1.0  
**创建日期**: 2026-05-19

---

## 1. 接口规范

| 规范项 | 说明 |
|--------|------|
| 基础路径 | `/api/v1` |
| 响应格式 | 统一 `{ "code": 0, "message": "success", "data": {...} }` |
| 分页格式 | `{ "items": [...], "total": 100, "page": 1, "page_size": 20 }` |
| 认证方式 | 管理面复用现有 Cookie/SSO；执行面通过连接器配置的认证凭证 |
| 错误码 | `0`=成功, `1001`=参数错误, `1002`=未授权, `1003`=资源不存在, `1004`=状态冲突, `1005`=校验失败, `5000`=内部错误 |
| 时间格式 | ISO 8601: `yyyy-MM-dd'T'HH:mm:ss.SSSXXX` |

---

## 2. 连接器管理 API

### 2.1 连接器 CRUD

| 方法 | 路径 | 说明 | FR |
|------|------|------|----|
| `POST` | `/api/v1/connectors` | 创建连接器 | FR-001 |
| `GET` | `/api/v1/connectors` | 查询连接器列表 | FR-004 |
| `GET` | `/api/v1/connectors/{connector_id}` | 查询连接器详情 | FR-004 |
| `PUT` | `/api/v1/connectors/{connector_id}` | 更新连接器基本信息 | FR-002 |
| `DELETE` | `/api/v1/connectors/{connector_id}` | 删除连接器 | FR-003 |
| `POST` | `/api/v1/connectors/{connector_id}/list-public` | 上架为公共连接器 | FR-005 |
| `POST` | `/api/v1/connectors/{connector_id}/delist` | 下架连接器 | FR-005 |

#### POST /api/v1/connectors — 创建连接器

```json
// Request
{
  "name": "IM 发送消息",
  "icon": "https://cdn.xxx.com/icons/im.svg",
  "description": "封装 IM 消息发送能力",
  "connector_type": "HTTP",
  "visibility": "private"
}

// Response 201
{
  "connector_id": "con_a1b2c3d4",
  "latest_version": {
    "version_id": "cv_e5f6g7h8",
    "version_no": "0.0.1",
    "status": "draft"
  }
}
```

#### GET /api/v1/connectors — 查询列表

```json
// Query params
// ?page=1&page_size=20&visibility=public&connector_type=HTTP&keyword=IM

// Response
{
  "items": [
    {
      "connector_id": "con_a1b2c3d4",
      "name": "IM 发送消息",
      "icon": "https://cdn.xxx.com/icons/im.svg",
      "description": "封装 IM 消息发送能力",
      "connector_type": "HTTP",
      "visibility": "public",
      "latest_version": "1.2.0",
      "version_status": "published",
      "created_at": "2026-05-19T10:00:00.000+08:00"
    }
  ],
  "total": 1,
  "page": 1,
  "page_size": 20
}
```

### 2.2 连接器版本管理

| 方法 | 路径 | 说明 | FR |
|------|------|------|----|
| `GET` | `/api/v1/connectors/{connector_id}/versions` | 版本列表 | FR-009 |
| `GET` | `/api/v1/connectors/{connector_id}/versions/{version_id}` | 版本详情（含连接配置） | FR-007 |
| `PUT` | `/api/v1/connectors/{connector_id}/versions/{version_id}` | 编辑草稿版本配置 | FR-008 |
| `POST` | `/api/v1/connectors/{connector_id}/versions/{version_id}/publish` | 发布版本 | FR-010 |

#### PUT /api/v1/connectors/{connector_id}/versions/{version_id} — 编辑连接配置

```json
// Request
{
  "connection_config": {
    "protocol": "HTTP",
    "protocol_config": {
      "base_url": "https://openapi.xxx.com/im",
      "method": "POST",
      "headers": { "Content-Type": "application/json" }
    },
    "auth": {
      "type": "AKSK",
      "config": {
        "access_key": "ak_xxxx",
        "secret_key": "sk_xxxx"
      }
    },
    "input_schema": {
      "type": "object",
      "properties": {
        "receiver": { "type": "string" },
        "content": { "type": "string" }
      },
      "required": ["receiver", "content"]
    },
    "output_schema": {
      "type": "object",
      "properties": {
        "msg_id": { "type": "string" }
      }
    },
    "timeout_ms": 30000
  }
}

// Response 200
{ "version_id": "cv_e5f6g7h8", "status": "draft", "updated_at": "..." }
```

#### POST /api/v1/connectors/{connector_id}/versions/{version_id}/publish — 发布

```json
// Request (optional)
{ "change_log": "新增消息撤回能力" }

// Response 200 (触发审批)
{
  "version_id": "cv_e5f6g7h8",
  "status": "pending_approval",
  "approval_id": "apr_xxxx"
}
```

### 2.3 连接器使用统计

| 方法 | 路径 | 说明 | FR |
|------|------|------|----|
| `GET` | `/api/v1/connectors/{connector_id}/stats` | 连接器使用统计 | FR-006 |

```json
// Response
{
  "connector_id": "con_a1b2c3d4",
  "referenced_by_flows": [
    { "flow_id": "flow_xxx", "flow_name": "消息通知流", "status": "enabled" }
  ],
  "total_invocations": 1523,
  "invocations_by_version": [
    { "version_no": "1.0.0", "count": 800 },
    { "version_no": "1.1.0", "count": 723 }
  ],
  "stats_period": { "from": "2026-05-01", "to": "2026-05-19" }
}
```

---

## 3. 连接流管理 API

### 3.1 连接流 CRUD

| 方法 | 路径 | 说明 | FR |
|------|------|------|----|
| `POST` | `/api/v1/flows` | 创建连接流 | FR-011 |
| `GET` | `/api/v1/flows` | 查询连接流列表 | FR-014 |
| `GET` | `/api/v1/flows/{flow_id}` | 查询连接流详情 | FR-016 |
| `PUT` | `/api/v1/flows/{flow_id}` | 更新连接流基本信息 | FR-012 |
| `DELETE` | `/api/v1/flows/{flow_id}` | 删除连接流 | FR-013 |
| `POST` | `/api/v1/flows/{flow_id}/enable` | 启用连接流 | FR-015 |
| `POST` | `/api/v1/flows/{flow_id}/disable` | 停用连接流 | FR-015 |

#### POST /api/v1/flows — 创建连接流

```json
// Request
{
  "name": "新消息自动通知",
  "description": "收到 IM 消息后自动发送通知到OA系统"
}

// Response 201
{
  "flow_id": "flow_i9j0k1l2",
  "latest_version": {
    "version_id": "fv_m3n4o5p6",
    "version_no": "0.0.1",
    "status": "draft"
  }
}
```

### 3.2 连接流版本管理

| 方法 | 路径 | 说明 | FR |
|------|------|------|----|
| `GET` | `/api/v1/flows/{flow_id}/versions` | 版本列表 | FR-018 |
| `GET` | `/api/v1/flows/{flow_id}/versions/{version_id}` | 版本详情（含编排配置） | FR-016 |
| `PUT` | `/api/v1/flows/{flow_id}/versions/{version_id}` | 保存编排配置（草稿） | FR-017 |
| `POST` | `/api/v1/flows/{flow_id}/versions/{version_id}/publish` | 发布版本 | FR-019 |

#### PUT /api/v1/flows/{flow_id}/versions/{version_id} — 保存编排配置

```json
// Request — orcherstration_config 全文替换
{
  "orchestration_config": {
    "trigger": {
      "type": "event",
      "config": {
        "event_source": "im:message:receive",
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
        "node_id": "node_entry",
        "node_type": "entry",
        "label": "收到消息",
        "position": { "x": 100, "y": 200 }
      },
      {
        "node_id": "node_1",
        "node_type": "connector",
        "label": "发送通知",
        "connector_version_id": "cv_e5f6g7h8",
        "input_mapping": { "receiver": "${trigger.sender}", "content": "${trigger.content}" },
        "retry_policy": { "max_retries": 3, "interval_ms": 1000 },
        "position": { "x": 350, "y": 200 }
      },
      {
        "node_id": "node_exit",
        "node_type": "exit",
        "label": "输出",
        "output_fields": ["msg_id"],
        "position": { "x": 600, "y": 200 }
      }
    ],
    "edges": [
      { "edge_id": "e1", "source": "node_entry", "target": "node_1" },
      { "edge_id": "e2", "source": "node_1", "target": "node_exit" }
    ]
  }
}

// Response 200
{ "version_id": "fv_m3n4o5p6", "status": "draft", "updated_at": "..." }
```

---

## 4. 运行时执行 API

### 4.1 执行操作

| 方法 | 路径 | 说明 | FR |
|------|------|------|----|
| `POST` | `/api/v1/flows/{flow_id}/executions` | 手动触发执行（异步） | FR-025 |
| `POST` | `/api/v1/flows/{flow_id}/test-run` | 测试运行 | FR-020 |
| `GET` | `/api/v1/executions/{execution_id}/status` | 查询执行状态 | FR-025a |
| `GET` | `/api/v1/flows/{flow_id}/executions` | 执行历史列表 | FR-032 |
| `GET` | `/api/v1/executions/{execution_id}` | 执行详情（含步骤） | FR-033 |
| `POST` | `/api/v1/executions/{execution_id}/retry` | 重试失败执行 | FR-030 |

#### POST /api/v1/flows/{flow_id}/executions — 手动触发

```json
// Request
{
  "trigger_data": {
    "sender": "user_001",
    "content": "你好，这是一条测试消息"
  }
}

// Response 202 (异步执行，立即返回)
{
  "execution_id": "exec_y5z6a7b8",
  "status": "pending",
  "created_at": "2026-05-19T10:00:00.000+08:00"
}
```

#### GET /api/v1/executions/{execution_id}/status — 查询执行状态

```json
// Response
{
  "execution_id": "exec_y5z6a7b8",
  "flow_id": "flow_i9j0k1l2",
  "status": "running",
  "trigger_type": "manual",
  "started_at": "2026-05-19T10:00:01.000+08:00",
  "finished_at": null,
  "result_data": null,
  "progress": {
    "total_nodes": 3,
    "completed_nodes": 1,
    "current_node": "node_1",
    "current_node_label": "发送通知"
  }
}
```

```json
// Response (执行成功)
{
  "execution_id": "exec_y5z6a7b8",
  "status": "success",
  "started_at": "2026-05-19T10:00:01.000+08:00",
  "finished_at": "2026-05-19T10:00:03.250+08:00",
  "duration_ms": 2250,
  "result_data": {
    "msg_id": "msg_xxxx"
  }
}
```

#### GET /api/v1/executions/{execution_id} — 执行详情

```json
// Response
{
  "execution_id": "exec_y5z6a7b8",
  "flow_id": "flow_i9j0k1l2",
  "version_id": "fv_m3n4o5p6",
  "status": "success",
  "trigger_type": "manual",
  "trigger_data": { "sender": "user_001", "content": "你好" },
  "result_data": { "msg_id": "msg_xxxx" },
  "started_at": "2026-05-19T10:00:01.000+08:00",
  "finished_at": "2026-05-19T10:00:03.250+08:00",
  "duration_ms": 2250,
  "steps": [
    {
      "step_id": "step_c9d0e1f2",
      "node_id": "node_entry",
      "node_name": "收到消息",
      "node_type": "entry",
      "status": "success",
      "input_data": { "sender": "user_001", "content": "你好" },
      "output_data": { "sender": "user_001", "content": "你好" },
      "started_at": "2026-05-19T10:00:01.000+08:00",
      "finished_at": "2026-05-19T10:00:01.010+08:00",
      "duration_ms": 10
    },
    {
      "step_id": "step_d0e1f2g3",
      "node_id": "node_1",
      "node_name": "发送通知",
      "node_type": "connector",
      "status": "success",
      "input_data": { "receiver": "user_001", "content": "你好" },
      "output_data": { "msg_id": "msg_xxxx", "code": 0 },
      "started_at": "2026-05-19T10:00:01.020+08:00",
      "finished_at": "2026-05-19T10:00:03.230+08:00",
      "duration_ms": 2210,
      "retry_attempts": 0
    },
    {
      "step_id": "step_e1f2g3h4",
      "node_id": "node_exit",
      "node_name": "输出",
      "node_type": "exit",
      "status": "success",
      "input_data": { "msg_id": "msg_xxxx", "code": 0 },
      "output_data": { "msg_id": "msg_xxxx" },
      "started_at": "2026-05-19T10:00:03.235+08:00",
      "finished_at": "2026-05-19T10:00:03.240+08:00",
      "duration_ms": 5
    }
  ]
}
```

#### POST /api/v1/flows/{flow_id}/test-run — 测试运行

```json
// Request
{
  "mock_trigger_data": {
    "sender": "test_user",
    "content": "这是一条测试消息"
  }
}

// Response 200 (同步返回测试结果，超时自动转为异步)
{
  "execution_id": "exec_test_xxxx",
  "status": "success",
  "steps": [ /* 同执行详情 steps */ ],
  "duration_ms": 1250,
  "is_test": true
}
```

### 4.2 Webhook 触发

| 方法 | 路径 | 说明 | FR |
|------|------|------|----|
| `POST` | `/api/v1/webhook/{webhook_token}` | Webhook 触发连接流 | FR-023 |

**webhook_token 生成规则**: `wh_` + 32 位随机字符串（不可预测）

```json
// Request — 由外部系统发送，格式由 flow 的 trigger schema 定义
{
  "event": "order.created",
  "order_id": "ORD-20260519-0001",
  "amount": 99.99
}

// Response 202
{ "execution_id": "exec_xxxx", "status": "pending" }

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
| `GET` | `/api/v1/flows/{flow_id}/status` | 连接流运行状态 | FR-031 |

#### GET /api/v1/monitor/metrics — 运行指标

```json
// Query params
// ?period=7d  (1h/24h/7d/30d)

// Response
{
  "active_flows": 12,
  "total_executions": 4523,
  "success_rate": 98.5,
  "avg_duration_ms": 1850,
  "p99_duration_ms": 5200,
  "executions_by_status": {
    "success": 4455,
    "failed": 45,
    "timeout": 23
  },
  "executions_by_hour": [
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
| `cp_trigger_event` | 事件触发连接流 | event-server | FlowScheduler | `{ "event_source": "...", "event_data": {...}, "timestamp": ... }` |
| `cp_trigger_manual` | 手动触发消息 | ManualTriggerController | FlowScheduler | `{ "flow_id": "...", "trigger_data": {...}, "execution_id": "..." }` |
| `cp_trigger_webhook` | Webhook 触发消息 | WebhookTriggerController | FlowScheduler | `{ "flow_id": "...", "webhook_data": {...}, "execution_id": "..." }` |
| `cp_execution_result` | 执行完成通知 | SequentialExecutor | MonitorService | `{ "execution_id": "...", "status": "...", "duration_ms": ... }` |

---

## 8. Scope 权限集成

连接器平台复用能力开放平台的 Scope 权限模型：

| 场景 | Scope 验证方式 |
|------|---------------|
| 连接器定义引用内部 API | 连接器创建时校验当前应用是否已有该 API 的 Scope |
| 连接流入口节点使用事件触发 | 版本发布时校验是否已订阅该事件 Scope |
| 运行时连接器调用内部 API | SequentialExecutor 调用 API 网关时携带应用 Scope 凭证 |
| 运行时事件触发匹配连接流 | FlowScheduler 校验连接流对应的 Scope 订阅是否有效 |