# 测试用例文档 - event-server 服务

**版本**: v1.0
**编写日期**: 2026-04-22
**服务端口**: 18082
**上下文路径**: /event-server

---

## 一、测试环境

### 1.1 服务配置

| 配置项 | 值 |
|-------|-----|
| 服务端口 | 18082 |
| 上下文路径 | /event-server |
| 数据库连接 | jdbc:mysql://localhost:3306/openplatform_v2 |
| Redis连接 | redis://localhost:6379/0 |
| 消息队列 | RabbitMQ/Kafka（企业内部消息平台） |

### 1.2 测试数据准备

#### 事件数据

```sql
INSERT INTO openplatform_v2_event_t (id, name_cn, name_en, topic, status) VALUES
(200, '消息接收事件', 'Message Received Event', 'im.message.received', 2),
(201, '消息已读事件', 'Message Read Event', 'im.message.read', 2),
(202, '用户上线事件', 'User Online Event', 'user.online', 2),
(203, '会议开始事件', 'Meeting Started Event', 'meeting.started', 1);
```

#### 回调数据

```sql
INSERT INTO openplatform_v2_callback_t (id, name_cn, name_en, status) VALUES
(300, '审批完成回调', 'Approval Completed Callback', 2),
(301, '文件上传回调', 'File Upload Callback', 2),
(302, '订单状态变更回调', 'Order Status Changed Callback', 1);
```

#### 权限数据

```sql
INSERT INTO openplatform_v2_permission_t (id, name_cn, name_en, scope, resource_type, resource_id, category_id, status) VALUES
(2000, '消息接收权限', 'Message Received Permission', 'event:im:message-received', 'event', 200, 2, 1),
(2001, '消息已读权限', 'Message Read Permission', 'event:im:message-read', 'event', 201, 2, 1),
(3000, '审批完成回调权限', 'Approval Completed Callback Permission', 'callback:approval:completed', 'callback', 300, 5, 1),
(3001, '文件上传回调权限', 'File Upload Callback Permission', 'callback:file:uploaded', 'callback', 301, 5, 1);
```

#### 订阅关系数据

```sql
INSERT INTO openplatform_v2_subscription_t (id, app_id, permission_id, status, channel_type, channel_address, auth_type, create_time) VALUES
-- 事件订阅（不同通道类型）
(401, 10, 2000, 1, 0, NULL, 0, NOW()),                          -- 内部消息队列
(402, 11, 2000, 1, 1, 'https://webhook.app11.com/events', 0, NOW()), -- WebHook
(403, 12, 2001, 1, 1, 'https://webhook.app12.com/events', 1, NOW()), -- WebHook（凭证B）
-- 回调订阅（不同通道类型）
(501, 10, 3000, 1, 0, 'https://webhook.app10.com/callback', 0, NOW()), -- WebHook
(502, 11, 3000, 1, 1, 'https://sse.app11.com/callback', 0, NOW()),     -- SSE
(503, 12, 3001, 1, 2, 'wss://ws.app12.com/callback', 0, NOW()),        -- WebSocket
-- 待审/已取消订阅（用于异常测试）
(404, 13, 2000, 0, NULL, NULL, 0, NOW()),  -- 待审
(405, 14, 2000, 3, NULL, NULL, 0, NOW());  -- 已取消
```

#### 事件消费配置数据

```sql
-- 应用10：消息队列方式消费
INSERT INTO openplatform_v2_subscription_t (id, app_id, permission_id, status, channel_type, auth_type) VALUES
(401, 10, 2000, 1, 0, 0);

-- 应用11：WebHook方式消费
INSERT INTO openplatform_v2_subscription_t (id, app_id, permission_id, status, channel_type, channel_address, auth_type) VALUES
(402, 11, 2000, 1, 1, 'https://webhook.example.com/events', 0);
```

---

## 二、接口测试用例

### 2.1 事件发布

#### TC-EVENT-PUB-001: 事件发布接口

**接口**: POST /gateway/events/publish
**优先级**: P0
**前置条件**: 
- Topic `im.message.received` 对应的事件已发布(status=2)
- 存在订阅该事件的应用

**测试步骤**:

| 步骤 | 操作 | 输入数据 | 预期结果 |
|-----|------|---------|---------|
| 1 | 发布有效事件 | topic=im.message.received, 有效payload | 返回200，事件分发成功 |
| 2 | 发布待审事件 | topic=meeting.started | 返回400，事件未发布 |
| 3 | 发布不存在事件 | topic=not.exist.topic | 返回404，事件不存在 |
| 4 | 缺少必填字段 | topic为空 | 返回400，参数校验失败 |
| 5 | payload过大 | payload超过1MB | 返回413，负载过大 |

**请求示例**:
```bash
curl -X POST "http://localhost:18082/event-server/gateway/events/publish" \
  -H "Content-Type: application/json" \
  -d '{
    "topic": "im.message.received",
    "payload": {
      "messageId": "msg001",
      "content": "Hello World",
      "sender": "user001",
      "receiver": "user002",
      "timestamp": "2026-04-22T10:00:00Z"
    }
  }'
```

**预期响应（成功）**:
```json
{
  "code": "200",
  "messageZh": "操作成功",
  "messageEn": "Success",
  "data": {
    "topic": "im.message.received",
    "eventId": "evt_20260422_001",
    "subscribers": 5,
    "message": "事件已分发至5个订阅方"
  },
  "page": null
}
```

**预期响应（事件未发布）**:
```json
{
  "code": "400",
  "messageZh": "事件未发布，无法分发",
  "messageEn": "Bad Request",
  "data": {
    "topic": "meeting.started",
    "status": 1
  },
  "page": null
}
```

**预期响应（事件不存在）**:
```json
{
  "code": "404",
  "messageZh": "事件不存在",
  "messageEn": "Not Found",
  "data": null,
  "page": null
}
```

**边界测试**:

| 场景 | 输入 | 预期结果 |
|-----|------|---------|
| Topic格式错误 | topic=invalid topic | 返回400，Topic格式错误 |
| payload为空对象 | payload={} | 返回200，分发空事件 |
| payload为null | payload=null | 返回400，payload不能为空 |
| 无订阅者 | 无人订阅的Topic | 返回200，subscribers=0 |
| 订阅者全部待审 | 仅待审订阅 | 返回200，subscribers=0（过滤待审） |
| 混合订阅状态 | 部分已授权、部分待审 | 返回200，仅分发已授权订阅者 |
| payload含特殊字符 | 含Unicode/Emoji | 返回200，正确处理UTF-8 |
| payload含嵌套对象 | 多层嵌套JSON | 返回200，正确解析 |

**处理流程**:

1. 验证Topic对应的事件资源存在且已发布
2. 查询订阅该事件的应用列表（仅已授权状态）
3. 按订阅配置分发事件：
   - **企业内部消息队列（channel_type=0）**：推送到对应消息队列
   - **WebHook（channel_type=1）**：POST到channel_address
4. 记录分发日志
5. 返回分发结果

**分发策略说明**:

| 通道类型 | 分发方式 | 重试策略 | 超时时间 |
|---------|---------|---------|---------|
| 内部消息队列 | 异步推送 | 队列重试 | 不适用 |
| WebHook | 同步HTTP POST | 3次重试 | 5秒 |

---

#### TC-EVENT-PUB-002: WebHook分发测试

**接口**: POST /gateway/events/publish
**优先级**: P0
**前置条件**: 
- 应用11通过WebHook订阅事件
- WebHook地址可达

**测试步骤**:

| 步骤 | 操作 | 输入数据 | 预期结果 |
|-----|------|---------|---------|
| 1 | WebHook分发成功 | 订阅WebHook可访问 | 返回200，WebHook调用成功 |
| 2 | WebHook超时 | WebHook响应超时 | 返回200，记录失败日志，触发重试 |
| 3 | WebHook返回错误 | WebHook返回4xx/5xx | 返回200，记录失败日志，触发重试 |
| 4 | WebHook地址无效 | URL格式错误 | 返回200，记录失败日志 |

**WebHook请求格式**（发送给订阅方）:

```http
POST https://webhook.example.com/events
Content-Type: application/json
X-Event-Topic: im.message.received
X-Event-Id: evt_20260422_001
X-Event-Timestamp: 2026-04-22T10:00:00Z
X-Signature: sha256=...

{
  "messageId": "msg001",
  "content": "Hello World",
  "sender": "user001",
  "receiver": "user002",
  "timestamp": "2026-04-22T10:00:00Z"
}
```

**WebHook签名说明**:

- 签名算法：HMAC-SHA256
- 签名内容：请求Body
- 签名密钥：订阅配置的auth_type对应的凭证
- 请求头：`X-Signature: sha256={signature}`

---

#### TC-EVENT-PUB-003: 消息队列分发测试

**接口**: POST /gateway/events/publish
**优先级**: P0
**前置条件**: 
- 应用10通过内部消息队列订阅事件
- 消息队列服务正常

**测试步骤**:

| 步骤 | 操作 | 输入数据 | 预期结果 |
|-----|------|---------|---------|
| 1 | 消息队列分发成功 | 队列服务正常 | 返回200，消息入队成功 |
| 2 | 消息队列不可用 | 队列服务异常 | 返回500，消息入队失败 |
| 3 | 消息队列满 | 队列容量满 | 返回503，服务暂时不可用 |

**消息格式**（推送到队列）:

```json
{
  "eventId": "evt_20260422_001",
  "topic": "im.message.received",
  "timestamp": "2026-04-22T10:00:00Z",
  "subscriptionId": "401",
  "appId": "10",
  "payload": {
    "messageId": "msg001",
    "content": "Hello World",
    "sender": "user001"
  }
}
```

---

### 2.2 回调触发

#### TC-CALLBACK-INVOKE-001: 回调触发接口

**接口**: POST /gateway/callbacks/invoke
**优先级**: P0
**前置条件**: 
- 回调Scope `callback:approval:completed` 对应的回调资源存在且已发布
- 存在订阅该回调的应用

**测试步骤**:

| 步骤 | 操作 | 输入数据 | 预期结果 |
|-----|------|---------|---------|
| 1 | 触发有效回调 | callback_scope=callback:approval:completed | 返回200，回调触发成功 |
| 2 | 触发待审回调 | callback_scope=未发布回调 | 返回400，回调未发布 |
| 3 | 触发不存在回调 | callback_scope=callback:not:exist | 返回404，回调不存在 |
| 4 | 缺少必填字段 | callback_scope为空 | 返回400，参数校验失败 |
| 5 | payload过大 | payload超过1MB | 返回413，负载过大 |

**请求示例**:
```bash
curl -X POST "http://localhost:18082/event-server/gateway/callbacks/invoke" \
  -H "Content-Type: application/json" \
  -d '{
    "callbackScope": "callback:approval:completed",
    "payload": {
      "approvalId": "app001",
      "status": "approved",
      "approver": "user001",
      "approvedAt": "2026-04-22T10:00:00Z",
      "comment": "审批通过"
    }
  }'
```

**预期响应（成功）**:
```json
{
  "code": "200",
  "messageZh": "操作成功",
  "messageEn": "Success",
  "data": {
    "callbackScope": "callback:approval:completed",
    "invokeId": "inv_20260422_001",
    "subscribers": 2,
    "message": "回调已触发至2个订阅方"
  },
  "page": null
}
```

**预期响应（回调不存在）**:
```json
{
  "code": "404",
  "messageZh": "回调不存在",
  "messageEn": "Not Found",
  "data": null,
  "page": null
}
```

**边界测试**:

| 场景 | 输入 | 预期结果 |
|-----|------|---------|
| Scope格式错误 | callbackScope=invalid | 返回400，Scope格式错误 |
| payload为空对象 | payload={} | 返回200，分发空回调 |
| payload为null | payload=null | 返回400，payload不能为空 |
| 无订阅者 | 无人订阅的回调 | 返回200，subscribers=0 |
| 订阅者全部待审 | 仅待审订阅 | 返回200，subscribers=0 |
| payload含敏感信息 | 含密码等 | 返回200，建议脱敏处理 |

**处理流程**:

1. 验证callback_scope对应的回调资源存在
2. 查询订阅该回调的应用列表（仅已授权状态）
3. 按订阅配置调用消费方：
   - **WebHook（channel_type=0）**：POST到channel_address
   - **SSE（channel_type=1）**：推送到SSE连接
   - **WebSocket（channel_type=2）**：推送到WebSocket连接
4. 记录调用日志
5. 返回触发结果

---

#### TC-CALLBACK-INVOKE-002: WebHook回调测试

**接口**: POST /gateway/callbacks/invoke
**优先级**: P0
**前置条件**: 
- 应用10通过WebHook订阅回调
- WebHook地址可达

**测试步骤**:

| 步骤 | 操作 | 输入数据 | 预期结果 |
|-----|------|---------|---------|
| 1 | WebHook调用成功 | WebHook可访问 | 返回200，WebHook调用成功 |
| 2 | WebHook超时 | WebHook响应超时 | 返回200，记录失败日志，触发重试 |
| 3 | WebHook返回错误 | WebHook返回4xx/5xx | 返回200，记录失败日志 |
| 4 | WebHook地址无效 | URL格式错误 | 返回200，记录失败日志 |

**WebHook请求格式**（发送给订阅方）:

```http
POST https://webhook.app10.com/callback
Content-Type: application/json
X-Callback-Scope: callback:approval:completed
X-Callback-Id: inv_20260422_001
X-Callback-Timestamp: 2026-04-22T10:00:00Z
X-Signature: sha256=...

{
  "approvalId": "app001",
  "status": "approved",
  "approver": "user001",
  "approvedAt": "2026-04-22T10:00:00Z",
  "comment": "审批通过"
}
```

---

#### TC-CALLBACK-INVOKE-003: SSE回调测试

**接口**: POST /gateway/callbacks/invoke
**优先级**: P1
**前置条件**: 
- 应用11通过SSE订阅回调
- SSE连接已建立

**测试步骤**:

| 步骤 | 操作 | 输入数据 | 预期结果 |
|-----|------|---------|---------|
| 1 | SSE推送成功 | SSE连接正常 | 返回200，SSE推送成功 |
| 2 | SSE连接断开 | SSE连接已断开 | 返回200，记录失败日志 |
| 3 | SSE连接不存在 | 应用未建立SSE连接 | 返回200，记录失败日志 |

**SSE推送格式**:

```
event: callback
data: {"callbackScope":"callback:approval:completed","payload":{...}}
id: inv_20260422_001
```

---

#### TC-CALLBACK-INVOKE-004: WebSocket回调测试

**接口**: POST /gateway/callbacks/invoke
**优先级**: P1
**前置条件**: 
- 应用12通过WebSocket订阅回调
- WebSocket连接已建立

**测试步骤**:

| 步骤 | 操作 | 输入数据 | 预期结果 |
|-----|------|---------|---------|
| 1 | WebSocket推送成功 | 连接正常 | 返回200，WebSocket推送成功 |
| 2 | WebSocket连接断开 | 连接已断开 | 返回200，记录失败日志 |
| 3 | WebSocket连接不存在 | 应用未建立连接 | 返回200，记录失败日志 |

**WebSocket消息格式**:

```json
{
  "type": "callback",
  "callbackScope": "callback:approval:completed",
  "invokeId": "inv_20260422_001",
  "timestamp": "2026-04-22T10:00:00Z",
  "payload": {
    "approvalId": "app001",
    "status": "approved"
  }
}
```

---

### 2.3 性能测试

#### TC-PERF-001: 事件发布性能测试

**接口**: POST /gateway/events/publish
**优先级**: P1
**前置条件**: 
- 事件服务已部署
- 存在订阅者

**性能指标**:

| 指标 | 目标值 | 说明 |
|-----|-------|------|
| P99响应时间 | < 200ms | 从请求到返回响应 |
| P99分发延迟 | < 1s | 从发布到消费方接收 |
| 吞吐量 | > 1000 TPS | 每秒处理事件数 |
| 并发连接 | > 100 | 同时处理的请求数 |

**测试步骤**:

| 步骤 | 操作 | 预期结果 |
|-----|------|---------|
| 1 | 单次请求测试 | 响应时间 < 100ms |
| 2 | 100并发请求 | P99 < 200ms |
| 3 | 1000并发请求 | P99 < 500ms，服务稳定 |
| 4 | 持续压测5分钟 | 吞吐量稳定，无内存泄漏 |

---

#### TC-PERF-002: 回调触发性能测试

**接口**: POST /gateway/callbacks/invoke
**优先级**: P1
**前置条件**: 
- 回调服务已部署
- 存在订阅者

**性能指标**:

| 指标 | 目标值 | 说明 |
|-----|-------|------|
| P99响应时间 | < 200ms | 从请求到返回响应 |
| P99触发延迟 | < 1s | 从触发到消费方接收 |
| 吞吐量 | > 500 TPS | 每秒处理回调数 |
| 并发连接 | > 50 | 同时处理的请求数 |

---

### 2.4 可靠性测试

#### TC-RELIABILITY-001: 分发重试机制

**场景**: WebHook分发失败后重试
**优先级**: P0
**前置条件**: 
- WebHook订阅者存在
- WebHook首次调用失败

**重试策略**:

| 重试次数 | 间隔时间 | 说明 |
|---------|---------|------|
| 第1次 | 1秒 | 立即重试 |
| 第2次 | 5秒 | 短间隔重试 |
| 第3次 | 30秒 | 长间隔重试 |

**测试步骤**:

| 步骤 | 操作 | 预期结果 |
|-----|------|---------|
| 1 | 模拟WebHook首次失败 | 记录失败，触发重试 |
| 2 | 模拟WebHook持续失败 | 重试3次后标记失败 |
| 3 | 模拟WebHook恢复 | 重试成功，标记成功 |

---

#### TC-RELIABILITY-002: 死信队列处理

**场景**: 分发持续失败进入死信队列
**优先级**: P1
**前置条件**: 
- WebHook持续失败3次

**处理流程**:

1. 分发失败3次后，消息进入死信队列
2. 记录失败日志
3. 发送告警通知
4. 管理员可手动重试

**测试步骤**:

| 步骤 | 操作 | 预期结果 |
|-----|------|---------|
| 1 | 持续失败3次 | 消息进入死信队列 |
| 2 | 检查死信队列 | 消息存在，状态为failed |
| 3 | 手动重试 | 消息重新分发 |

---

## 三、测试总结

### 3.1 测试覆盖率

| 模块 | 接口数 | 测试用例数 | 覆盖率 |
|-----|-------|----------|-------|
| 事件发布 | 1 | 3 | 100% |
| 回调触发 | 1 | 4 | 100% |
| 性能测试 | - | 2 | 100% |
| 可靠性测试 | - | 2 | 100% |
| **总计** | **2** | **11** | **100%** |

### 3.2 测试统计

| 测试类型 | 用例数 | 占比 |
|---------|-------|------|
| 功能测试 | 7 | 50% |
| 边界测试 | 6 | 30% |
| 性能测试 | 2 | 10% |
| 可靠性测试 | 2 | 10% |
| **总计** | **17** | **100%** |

### 3.3 状态码说明

| 状态码 | 说明 |
|--------|------|
| 200 | 成功 |
| 400 | 请求参数错误 |
| 404 | 事件/回调不存在 |
| 413 | 负载过大 |
| 500 | 服务器内部错误 |
| 503 | 服务暂时不可用 |

### 3.4 事件状态枚举

| 值 | 说明 |
|-----|------|
| 0 | 草稿 |
| 1 | 待审 |
| 2 | 已发布 |
| 3 | 已下线 |

### 3.5 订阅状态枚举

| 值 | 说明 |
|-----|------|
| 0 | 待审 |
| 1 | 已授权 |
| 2 | 已拒绝 |
| 3 | 已取消 |

### 3.6 通道类型枚举

**事件通道类型**:

| 值 | 说明 | 分发方式 |
|-----|------|---------|
| 0 | 企业内部消息队列 | 异步推送 |
| 1 | WebHook | 同步HTTP POST |

**回调通道类型**:

| 值 | 说明 | 分发方式 |
|-----|------|---------|
| 0 | WebHook | 同步HTTP POST |
| 1 | SSE | 服务端推送 |
| 2 | WebSocket | 双向通信 |

### 3.7 性能要求

| 指标 | 目标值 | 说明 |
|-----|-------|------|
| 事件发布P99延迟 | < 200ms | 含分发时间 |
| 回调触发P99延迟 | < 200ms | 含调用时间 |
| 事件分发P99延迟 | < 1s | 从发布到消费方接收 |
| 系统可用性 | ≥ 99.9% | 年度可用性 |

### 3.8 安全测试要点

1. **身份验证**
   - 内部服务调用需验证来源
   - 防止未授权的事件发布

2. **数据安全**
   - Payload敏感信息脱敏
   - WebHook签名验证
   - 传输加密（HTTPS）

3. **流量控制**
   - 限流保护
   - 熔断机制
   - 降级策略

---

**文档状态**: ✅ 测试用例编写完成
