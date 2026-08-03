# event-server — 全景入口

> **文档定位**: sddu-docs-overview — 本级全景入口  
> **输出文件名**: docs-overview.md  
> **数据来源**: 代码扫描生成 — event-server/src/main/java + application*.yml  
> **创建时间**: 2026-08-03  
> **版本**: v1.0 (CODE-SCAN)  
> **生成方式**: 全量生成

---

## 1. 业务全景

### 1.1 自身概述

| 属性 | 值 |
|------|-----|
| **类型** | Spring Boot Web 服务（事件/回调网关） |
| **职责描述** | 事件发布网关 + 回调触发网关 + SSE / WebSocket 实时通道，将平台事件推送给订阅消费方 |
| **所属业务域** | 能力开放平台（消费网关） |
| **版本** | 1.0.0-SNAPSHOT (Spring Boot 3.4.6) |

### 1.2 子组件

| 组件 | 类型 | 描述 | 关系说明 |
|------|------|------|---------|
| **gateway 模块** | 模块 | EventGateway（发布事件）+ CallbackGateway（触发回调） | 核心 |
| **common 模块** | 模块 | SSE 通道、WebSocket 通道、健康检查 | 通道层 |

### 1.3 子组件分类

| 分类 | 包含组件 |
|------|---------|
| **事件网关** | EventGatewayController / EventGatewayService |
| **回调网关** | CallbackGatewayController / CallbackGatewayService |
| **实时通道** | SseController、WebSocketController |

---

## 2. 技术全景

### 2.1 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 3.4.6 | Web 框架 |
| Spring Data Redis | Lettuce | 订阅列表缓存（单机 dev / 集群可切） |
| SSE | MediaType.TEXT_EVENT_STREAM | 服务端推送 |
| WebSocket | Spring WS | 双向通道 |
| SpringDoc | swagger-ui | API 文档 |

### 2.2 架构决策记录（ADR）

| 编号 | 标题 | 状态 | 影响范围 |
|:--:|------|:--:|---------|
| — | 本级由代码扫描生成，无独立 ADR | — | — |

### 2.3 通道模型

```
外部事件源 ──► POST /gateway/events/publish
                 ├── 校验 topic 订阅关系（Redis 缓存）
                 ├── 推送至 SSE 连接（/sse/connect/{connectionId}）
                 ├── 推送至 WebSocket（/ws）
                 └── 内部消息队列（channel_type=0）

外部回调请求 ──► POST /gateway/callbacks/invoke
                 ├── 校验 callback_scope 资源存在
                 ├── 校验订阅关系 + 通道配置
                 └── 调用消费方 WebHook（channel_address）
```

### 2.4 本域文档

| 文档 | 说明 |
|------|------|
| `api.md` | event-server API 端点 |
| `config.md` | event-server 配置项 |
| `event.md` | 事件/通道模型详述 |

---

## 修订记录

| 生成时间 | 变更 Feature | 生成方式 | 修订人 |
|---------|-------------|:--:|--------|
| 2026-08-03 | 代码扫描全量生成 | code-scan | SDDU Docs Agent |
