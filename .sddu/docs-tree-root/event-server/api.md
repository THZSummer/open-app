# event-server API — API 路由文档

> **文档定位**: sddu-docs-api — API 路由文档 — REST 端点、请求/响应 Schema、状态码  
> **输出文件名**: event-server-api.md  
> **数据来源**: 代码扫描生成 — event-server/src/main/java/**/controller/*.java  
> **创建人**: SDDU Docs Agent  
> **创建时间**: 2026-08-03  
> **版本**: v1.0 (CODE-SCAN)  

## 1. API 概述

| 属性 | 值 |
|------|-----|
| **API 名称** | event-server 事件/回调网关 API |
| **基础路径** | `/gateway` + `/sse` + `/ws`（context-path: /event-server） |
| **所属域** | 能力开放平台（消费网关） |
| **认证方式** | 认证头：X-SOA-TOKEN / X-APIG-APPID / X-APIG-APPKEY / X-AKSK-TOKEN |

## 2. REST 端点

### 2.1 健康检查

| 方法 | 路径 | 说明 |
|:----:|------|------|
| GET | /api/v1/health | 健康检查 |

### 2.2 SSE 通道（common）

| 方法 | 路径 | 说明 |
|:----:|------|------|
| GET | /sse/connect/{connectionId} | 建立 SSE 长连接（text/event-stream） |
| DELETE | /sse/disconnect/{connectionId} | 断开 SSE 连接 |
| GET | /sse/status | SSE 通道状态 |

### 2.3 WebSocket 通道（common）

| 方法 | 路径 | 说明 |
|:----:|------|------|
| GET | /ws/status | WebSocket 状态 |
| GET | /ws/count | WebSocket 连接数 |

### 2.4 事件网关（gateway）

| 方法 | 路径 | 说明 |
|:----:|------|------|
| POST | /gateway/events/publish | 发布事件（topic → 推送订阅方） |
| DELETE | /gateway/events/cache/{topic} | 清除某 topic 的订阅列表缓存 |

### 2.5 回调网关（gateway）

| 方法 | 路径 | 说明 |
|:----:|------|------|
| POST | /gateway/callbacks/invoke | 触发回调（scope → 调用消费方 WebHook） |
| DELETE | /gateway/callbacks/cache/{scope} | 清除某 scope 的订阅列表缓存 |

## 3. 请求/响应 Schema

### POST /gateway/events/publish

**请求**:

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| topic | string | ✅ | 事件主题（对应 v2_event_t.topic） |
| payload | object | ✅ | 事件数据 |
| （认证头） | header | — | X-SOA-TOKEN 等 |

**响应**: `{ code, message, data }`（ApiResponse）

### POST /gateway/callbacks/invoke

**请求**:

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| callbackScope | string | ✅ | 回调 scope |
| payload | object | ✅ | 回调数据 |

**响应**: `CallbackInvokeResponse`（包含各消费方调用结果）

## 4. 状态码

| 状态码 | 含义 | 场景 |
|:------:|------|------|
| 200 | 成功 | 发布/触发成功 |
| 400 | 参数错误 | topic/scope 缺失 |
| 401 | 未认证 | 认证头校验失败 |
| 404 | 资源不存在 | topic/scope 未注册 |
| 500 | 服务端错误 | 推送/调用异常 |

---

## 修订记录

| 版本 | 变更说明 | 日期 | 修订人 |
|------|---------|------|--------|
| v1.0 | 代码扫描全量生成（8 端点） | 2026-08-03 | SDDU Docs Agent |
