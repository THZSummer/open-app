# 回调配置查询接口

> **接口类型**：内部接口（供 XX 通讯平台内部业务模块调用）
> **提供方**：api-server
> **关联需求**：FR-030（消费网关 - 回调路由）

## 接口信息

```
POST /gateway/callbacks/config
```

## 接口说明

XX 通讯平台内部业务模块通过 AK + Scope 查询应用对某个回调的订阅配置，用于触发回调时获取消费方配置信息。

### 调用方

- event-server（平台统一回调出口）
- XX 通讯平台内部其他业务模块

### 使用场景

1. **event-server 作为平台统一回调出口**：获取配置后统一调用三方平台回调地址
2. **其他业务模块直接调用**：可直接获取回调配置，自行调用三方平台接口（保留此能力）

## 请求

### 请求头

| Header | 必填 | 说明 |
|--------|------|------|
| Authorization | 是 | 内部调用凭证（用于验证调用方身份） |

### 请求体

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| ak | string | 是 | 应用 Access Key |
| scope | string | 是 | 回调权限标识（Scope） |

### 请求示例

```json
{
  "ak": "AK123456789",
  "scope": "callback:approval:completed"
}
```

## 响应

### 响应示例（成功）

```json
{
  "code": "200",
  "messageZh": "操作成功",
  "messageEn": "Success",
  "data": {
    "ak": "AK123456789",
    "scope": "callback:approval:completed",
    "channelType": 0,
    "channelAddress": "https://webhook.example.com/callbacks",
    "authType": 0
  }
}
```

### 响应示例（未订阅）

```json
{
  "code": "200",
  "messageZh": "操作成功",
  "messageEn": "Success",
  "data": null
}
```

### 响应示例（AK 无效）

```json
{
  "code": "400",
  "messageZh": "无效的 Access Key",
  "messageEn": "Invalid Access Key",
  "data": null
}
```

### 字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| ak | string | 应用 Access Key |
| scope | string | 回调权限标识 |
| channelType | int | 通道类型（0=WebHook, 1=SSE, 2=WebSocket） |
| channelAddress | string | 通道地址 |
| authType | int | 认证类型（0=应用类凭证A, 1=应用类凭证B, 2=AKSK, 3=Bearer Token） |

## 错误码

| 错误码 | 说明 |
|--------|------|
| 400 | 无效的 Access Key 或 Scope |
| 404 | 回调资源不存在 |

## 调用流程

```
┌─────────────────────────────────────────────────────────────┐
│                    XX 通讯平台内部                           │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   ┌─────────────┐     ┌───────────────┐                    │
│   │ api-server  │────▶│ 回调配置查询  │◀────┐              │
│   │ (提供方)     │     │ POST /gateway/│     │              │
│   └─────────────┘     │ callbacks/config   │              │
│                       └───────────────┘     │              │
│                                             │              │
│   ┌─────────────────┐                       │              │
│   │  event-server   │───────────────────────┘              │
│   │ (统一回调出口)   │                                      │
│   └────────┬────────┘                                      │
│            │                                               │
│            ▼                                               │
│   ┌─────────────────┐         ┌─────────────────┐          │
│   │  其他业务模块    │────────▶│  三方平台接口   │          │
│   │ (保留直接调用能力)│         └─────────────────┘          │
│   └─────────────────┘                                      │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```
