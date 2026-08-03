# event-server — 领域事件文档

> **文档定位**: sddu-docs-event — 领域事件文档 — 事件类型、生产者、消费者、触发条件  
> **输出文件名**: event-server-event.md  
> **数据来源**: 代码扫描生成 — event-server + v2_event_t 表结构 + specs-tree 基线  
> **创建人**: SDDU Docs Agent  
> **创建时间**: 2026-08-03  
> **版本**: v1.0 (CODE-SCAN)  

## 1. 事件模型概述

事件资源由 open-server 注册（v2_event_t：name_cn/name_en/topic/status），消费方通过 permission + subscription 订阅。event-server 承载实际推送。

## 2. 事件类型（Topic）

| Topic | 说明 | 生产者 | 消费者通道 |
|-------|------|--------|-----------|
| （动态注册） | 由能力提供方注册，topic 唯一（uk_topic） | 外部事件源 | SSE / WebSocket / 内部消息队列 |
| 应用状态变更 | 应用发布/下架通知（推测） | open-server | 订阅方 |
| 能力订阅变更 | 订阅关系变化通知（推测） | open-server | 订阅方 |

> ⚠️ 具体 topic 名称未在代码常量中发现，业务上由能力注册方在 v2_event_t 中声明，运行时按 topic 匹配订阅。

## 3. 生产者与消费者

| 角色 | 实体 | 说明 |
|------|------|------|
| 生产者 | 外部系统 / open-server | 通过 POST /gateway/events/publish 发布 |
| 消费者 | 三方平台 | 通过订阅关系（subscription_t）接收，channel_type 决定通道 |
| 事件网关 | EventGatewayService | 校验 topic → 查 Redis 订阅缓存 → 推送 |

## 4. 触发条件

| 场景 | 触发条件 | 处理 |
|------|---------|------|
| 事件发布 | POST /gateway/events/publish | 校验订阅 → 推送所有已订阅消费方 |
| SSE 订阅 | GET /sse/connect/{connectionId} | 建立长连接，接收事件推送 |
| WebSocket | /ws 通道 | 双向实时通道 |
| 回调触发 | POST /gateway/callbacks/invoke | 校验 scope → 调用消费方 WebHook |

## 5. 通道类型（Channel Type）

| channel_type | 通道 | 说明 |
|:---:|------|------|
| 0 | 内部消息队列 | 平台内部流转 |
| 1 | WebHook | HTTP 回调到消费方 |
| 2 | SSE | Server-Sent Events 长连接 |
| 3 | WebSocket | 双向通道 |

## 6. 订阅缓存

- Redis 缓存订阅列表（按 topic/scope 缓存）
- 支持主动清除缓存：DELETE /gateway/events/cache/{topic}、/gateway/callbacks/cache/{scope}
- 订阅变更（open-server 同步）后需清缓存生效

---

## 修订记录

| 版本 | 变更说明 | 日期 | 修订人 |
|------|---------|------|--------|
| v1.0 | 代码扫描全量生成 | 2026-08-03 | SDDU Docs Agent |
