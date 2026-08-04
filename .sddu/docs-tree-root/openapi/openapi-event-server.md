# event-server 事件/回调网关 — OpenAPI 契约

> **文档定位**: sddu-docs-api — API 路由文档（GitHub 可渲染包装视图）
> **原始契约**: [openapi-event-server.yaml](openapi-event-server.yaml)（机器可读 OpenAPI 3.0.3）
> **服务地址**: http://localhost:18082/event-server
> **对应全景**: [api.md](../api.md)（业务视角接口清单）
> **生成方式**: 静态分析（controller 注解 + DTO 字段），未运行服务
> **生成时间**: 2026-08-03

## 1. 服务信息

| 属性 | 值 |
|------|-----|
| **服务** | event-server |
| **标题** | event-server 事件/回调网关 |
| **服务地址** | `http://localhost:18082/event-server` |
| **OpenAPI 版本** | 3.0.3 |
| **接口标签** | 健康检查, SSE 连接, WebSocket 管理, 回调网关, 事件网关 |
| **端点数** | 10 |
| **Schema 数** | 5 |

## 2. 认证方式

- **akskAuth**（X-AKSK-TOKEN）— 消费面 AKSK，另支持 X-SOA-TOKEN / X-APIG-APPID

## 3. 端点概览

| 方法 | 路径 | 摘要 |
|:----:|------|------|
| DELETE | `/gateway/callbacks/cache/{scope}` | clearCache |
| DELETE | `/gateway/events/cache/{topic}` | clearCache |
| DELETE | `/sse/disconnect/{connectionId}` | disconnect |
| GET | `/api/v1/health` | health |
| GET | `/sse/connect/{connectionId}` | connect |
| GET | `/sse/status` | status |
| GET | `/ws/count` | getCount |
| GET | `/ws/status` | getStatus |
| POST | `/gateway/callbacks/invoke` | invokeCallback |
| POST | `/gateway/events/publish` | publishEvent |

## 4. OpenAPI 规范（完整 YAML）

> 下方为完整 OpenAPI 3.0.3 契约，可直接用于 openapi-generator / redoc / Swagger UI。

```yaml
# event-server 事件/回调网关 API
# 生成方式: 静态分析（controller 注解 + DTO 字段），未运行服务
# 对应全景: .sddu/docs-tree-root/api.md（业务视角接口清单）

openapi: 3.0.3
info:
  title: event-server 事件/回调网关 API
  version: v1.0
  description: 事件/回调网关：事件发布、回调触发、SSE/WebSocket 通道（公共连接能力 R2/R3 消费面）
servers:
  -
    url: "http://localhost:18082/event-server"
    description: event-server 本地服务
tags:
  - name: 健康检查
  - name: SSE 连接
  - name: WebSocket 管理
  - name: 回调网关
  - name: 事件网关
paths:
  /api/v1/health:
    get:
      tags:
        - 健康检查
      summary: health
      operationId: health_get
      security:
        - akskAuth: []
      responses:
        200:
          description: 成功
          content:
            application/json:
              schema:
                type: object
        400:
          description: 参数错误
        401:
          description: 未认证
        403:
          description: 无权限
        500:
          description: 服务端错误
  /sse/connect/{connectionId}:
    get:
      tags:
        - SSE 连接
      summary: connect
      operationId: connect_get
      security:
        - akskAuth: []
      parameters:
        -
          name: connectionId
          in: path
          required: true
          schema:
            type: string
      responses:
        200:
          description: 成功
          content:
            application/json:
              schema:
                $ref: "#/components/schemas/SseEmitter"
        400:
          description: 参数错误
        401:
          description: 未认证
        403:
          description: 无权限
        500:
          description: 服务端错误
  /sse/status:
    get:
      tags:
        - SSE 连接
      summary: status
      operationId: status_get
      security:
        - akskAuth: []
      responses:
        200:
          description: 成功
          content:
            application/json:
              schema:
                type: object
        400:
          description: 参数错误
        401:
          description: 未认证
        403:
          description: 无权限
        500:
          description: 服务端错误
  /sse/disconnect/{connectionId}:
    delete:
      tags:
        - SSE 连接
      summary: disconnect
      operationId: disconnect_delete
      security:
        - akskAuth: []
      parameters:
        -
          name: connectionId
          in: path
          required: true
          schema:
            type: string
      responses:
        200:
          description: 成功
          content:
            application/json:
              schema:
                type: object
        400:
          description: 参数错误
        401:
          description: 未认证
        403:
          description: 无权限
        500:
          description: 服务端错误
  /ws/status:
    get:
      tags:
        - WebSocket 管理
      summary: getStatus
      operationId: getStatus_get
      security:
        - akskAuth: []
      responses:
        200:
          description: 成功
          content:
            application/json:
              schema:
                type: object
        400:
          description: 参数错误
        401:
          description: 未认证
        403:
          description: 无权限
        500:
          description: 服务端错误
  /ws/count:
    get:
      tags:
        - WebSocket 管理
      summary: getCount
      operationId: getCount_get
      security:
        - akskAuth: []
      responses:
        200:
          description: 成功
          content:
            application/json:
              schema:
                type: object
        400:
          description: 参数错误
        401:
          description: 未认证
        403:
          description: 无权限
        500:
          description: 服务端错误
  /gateway/callbacks/invoke:
    post:
      tags:
        - 回调网关
      summary: invokeCallback
      operationId: invokeCallback_post
      security:
        - akskAuth: []
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: "#/components/schemas/CallbackInvokeRequest"
      responses:
        200:
          description: 成功
          content:
            application/json:
              schema:
                $ref: "#/components/schemas/CallbackInvokeResponse"
        400:
          description: 参数错误
        401:
          description: 未认证
        403:
          description: 无权限
        500:
          description: 服务端错误
  /gateway/callbacks/cache/{scope}:
    delete:
      tags:
        - 回调网关
      summary: clearCache
      operationId: clearCache_delete
      security:
        - akskAuth: []
      parameters:
        -
          name: scope
          in: path
          required: true
          schema:
            type: string
      responses:
        200:
          description: 成功
          content:
            application/json:
              schema:
                type: object
        400:
          description: 参数错误
        401:
          description: 未认证
        403:
          description: 无权限
        500:
          description: 服务端错误
  /gateway/events/publish:
    post:
      tags:
        - 事件网关
      summary: publishEvent
      operationId: publishEvent_post
      security:
        - akskAuth: []
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: "#/components/schemas/EventPublishRequest"
      responses:
        200:
          description: 成功
          content:
            application/json:
              schema:
                $ref: "#/components/schemas/EventPublishResponse"
        400:
          description: 参数错误
        401:
          description: 未认证
        403:
          description: 无权限
        500:
          description: 服务端错误
  /gateway/events/cache/{topic}:
    delete:
      tags:
        - 事件网关
      summary: clearCache
      operationId: clearCache_delete
      security:
        - akskAuth: []
      parameters:
        -
          name: topic
          in: path
          required: true
          schema:
            type: string
      responses:
        200:
          description: 成功
          content:
            application/json:
              schema:
                type: object
        400:
          description: 参数错误
        401:
          description: 未认证
        403:
          description: 无权限
        500:
          description: 服务端错误
components:
  securitySchemes:
    internalAuth:
      type: http
      scheme: bearer
      description: 管理面内部凭证（open-server/market-server）
    akskAuth:
      type: apiKey
      in: header
      name: X-AKSK-TOKEN
      description: 消费面 AKSK 认证（api-server/event-server/connector-api），另支持 X-SOA-TOKEN / X-APIG-APPID
  schemas:
    CallbackInvokeRequest:
      type: object
      properties:
        callbackScope:
          type: string
        payload:
          type: object
    CallbackInvokeResponse:
      type: object
      properties:
        callbackScope:
          type: string
        subscribers:
          type: integer
          format: int32
        message:
          type: string
    EventPublishRequest:
      type: object
      properties:
        topic:
          type: string
        payload:
          type: object
    EventPublishResponse:
      type: object
      properties:
        topic:
          type: string
        subscribers:
          type: integer
          format: int32
        message:
          type: string
    SseEmitter:
      type: object
      description: SseEmitter（未在代码中定位到定义，见实现）
```
