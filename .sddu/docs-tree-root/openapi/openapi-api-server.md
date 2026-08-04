# api-server 消费网关 — OpenAPI 契约

> **文档定位**: sddu-docs-api — API 路由文档（GitHub 可渲染包装视图）
> **原始契约**: [openapi-api-server.yaml](openapi-api-server.yaml)（机器可读 OpenAPI 3.0.3）
> **服务地址**: http://localhost:18081/api-server
> **对应全景**: [api.md](../api.md)（业务视角接口清单）
> **生成方式**: 静态分析（controller 注解 + DTO 字段），未运行服务
> **生成时间**: 2026-08-03

## 1. 服务信息

| 属性 | 值 |
|------|-----|
| **服务** | api-server |
| **标题** | api-server 消费网关 |
| **服务地址** | `http://localhost:18081/api-server` |
| **OpenAPI 版本** | 3.0.3 |
| **接口标签** | controller, 健康检查, 数据查询接口, API 网关, 内部接口 - 用户角色查询, Scope 授权管理 |
| **端点数** | 11 |
| **Schema 数** | 11 |

## 2. 认证方式

- **akskAuth**（X-AKSK-TOKEN）— 消费面 AKSK，另支持 X-SOA-TOKEN / X-APIG-APPID

## 3. 端点概览

| 方法 | 路径 | 摘要 |
|:----:|------|------|
| DELETE | `/api/v1/user-authorizations/{id}` | revokeUserAuthorization |
| GET | `/api/v1/health` | health |
| GET | `/api/v1/user-authorizations` | getUserAuthorizations |
| GET | `/gateway/permissions/check` | checkPermission |
| GET | `/gateway/permissions/detail` | getPermissionByScope |
| GET | `/gateway/permissions/subscribers` | getSubscribedApps |
| GET | `/gateway/subscriptions/config` | getSubscriptionConfig |
| POST | `/api/v1/approvals/callback` | handleCallback |
| POST | `/api/v1/user-authorizations` | createUserAuthorization |
| POST | `/gateway/assistant/callbacks/config` | getAssistantCallbackConfig |
| POST | `/service/open/v2/internal/user/roles` | queryUserRoles |

## 4. OpenAPI 规范（完整 YAML）

> 下方为完整 OpenAPI 3.0.3 契约，可直接用于 openapi-generator / redoc / Swagger UI。

```yaml
# api-server 消费网关 API
# 生成方式: 静态分析（controller 注解 + DTO 字段），未运行服务
# 对应全景: .sddu/docs-tree-root/api.md（业务视角接口清单）

openapi: 3.0.3
info:
  title: api-server 消费网关 API
  version: v1.0
  description: 消费网关：API 网关代理、数据查询、用户授权（Scope）、审批回调、内部角色同步
servers:
  -
    url: "http://localhost:18081/api-server"
    description: api-server 本地服务
tags:
  - name: controller
  - name: 健康检查
  - name: 数据查询接口
  - name: API 网关
  - name: 内部接口 - 用户角色查询
  - name: Scope 授权管理
paths:
  /api/v1/approvals/callback:
    post:
      tags:
        - controller
      summary: handleCallback
      operationId: handleCallback_post
      security:
        - akskAuth: []
      parameters:
        -
          name: businessId
          in: query
          required: true
          schema:
            type: integer
            format: int64
        -
          name: businessType
          in: query
          required: true
          schema:
            type: string
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: "#/components/schemas/ApprovalCallbackRequest"
      responses:
        200:
          description: 成功
          content:
            application/json:
              schema:
                $ref: "#/components/schemas/ApprovalCallbackResponse"
        400:
          description: 参数错误
        401:
          description: 未认证
        403:
          description: 无权限
        500:
          description: 服务端错误
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
  /gateway/permissions/check:
    get:
      tags:
        - 数据查询接口
      summary: checkPermission
      operationId: checkPermission_get
      security:
        - akskAuth: []
      parameters:
        -
          name: appId
          in: query
          required: true
          schema:
            type: string
        -
          name: scope
          in: query
          required: true
          schema:
            type: string
      responses:
        200:
          description: 成功
          content:
            application/json:
              schema:
                $ref: "#/components/schemas/PermissionCheckResponse"
        400:
          description: 参数错误
        401:
          description: 未认证
        403:
          description: 无权限
        500:
          description: 服务端错误
  /gateway/permissions/subscribers:
    get:
      tags:
        - 数据查询接口
      summary: getSubscribedApps
      operationId: getSubscribedApps_get
      security:
        - akskAuth: []
      parameters:
        -
          name: scope
          in: query
          required: true
          schema:
            type: string
      responses:
        200:
          description: 成功
          content:
            application/json:
              schema:
                type: array
                items:
                  type: string
        400:
          description: 参数错误
        401:
          description: 未认证
        403:
          description: 无权限
        500:
          description: 服务端错误
  /gateway/subscriptions/config:
    get:
      tags:
        - 数据查询接口
      summary: getSubscriptionConfig
      operationId: getSubscriptionConfig_get
      security:
        - akskAuth: []
      parameters:
        -
          name: appId
          in: query
          required: true
          schema:
            type: string
        -
          name: scope
          in: query
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
  /gateway/permissions/detail:
    get:
      tags:
        - 数据查询接口
      summary: getPermissionByScope
      operationId: getPermissionByScope_get
      security:
        - akskAuth: []
      parameters:
        -
          name: scope
          in: query
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
  /gateway/assistant/callbacks/config:
    post:
      tags:
        - API 网关
      summary: getAssistantCallbackConfig
      operationId: getAssistantCallbackConfig_post
      security:
        - akskAuth: []
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: "#/components/schemas/CallbackConfigRequest"
      responses:
        200:
          description: 成功
          content:
            application/json:
              schema:
                $ref: "#/components/schemas/CallbackConfigResponse"
        400:
          description: 参数错误
        401:
          description: 未认证
        403:
          description: 无权限
        500:
          description: 服务端错误
  /service/open/v2/internal/user/roles:
    post:
      tags:
        - 内部接口 - 用户角色查询
      summary: queryUserRoles
      operationId: queryUserRoles_post
      security:
        - akskAuth: []
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: "#/components/schemas/UserRoleQueryRequest"
      responses:
        200:
          description: 成功
          content:
            application/json:
              schema:
                $ref: "#/components/schemas/UserRoleQueryResponse"
        400:
          description: 参数错误
        401:
          description: 未认证
        403:
          description: 无权限
        500:
          description: 服务端错误
  /api/v1/user-authorizations:
    get:
      tags:
        - Scope 授权管理
      summary: getUserAuthorizations
      operationId: getUserAuthorizations_get
      security:
        - akskAuth: []
      parameters:
        -
          name: userId
          in: query
          required: false
          schema:
            type: string
        -
          name: appId
          in: query
          required: false
          schema:
            type: string
        -
          name: keyword
          in: query
          required: false
          schema:
            type: string
        -
          name: curPage
          in: query
          required: true
          schema:
            type: integer
            format: int32
        -
          name: pageSize
          in: query
          required: true
          schema:
            type: integer
            format: int32
      responses:
        200:
          description: 成功
          content:
            application/json:
              schema:
                type: array
                items:
                  $ref: "#/components/schemas/UserAuthorizationListResponse"
        400:
          description: 参数错误
        401:
          description: 未认证
        403:
          description: 无权限
        500:
          description: 服务端错误
    post:
      tags:
        - Scope 授权管理
      summary: createUserAuthorization
      operationId: createUserAuthorization_post
      security:
        - akskAuth: []
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: "#/components/schemas/UserAuthorizationCreateRequest"
      responses:
        200:
          description: 成功
          content:
            application/json:
              schema:
                $ref: "#/components/schemas/UserAuthorizationResponse"
        400:
          description: 参数错误
        401:
          description: 未认证
        403:
          description: 无权限
        500:
          description: 服务端错误
  /api/v1/user-authorizations/{id}:
    delete:
      tags:
        - Scope 授权管理
      summary: revokeUserAuthorization
      operationId: revokeUserAuthorization_delete
      security:
        - akskAuth: []
      parameters:
        -
          name: id
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
    ApprovalCallbackRequest:
      type: object
      properties:
        cardId:
          type: string
        content:
          type: string
        messageType:
          type: string
        type:
          type: string
        corpId:
          type: string
        traceId:
          type: string
        userId:
          type: string
        accountId:
          type: string
    ApprovalCallbackResponse:
      type: object
      properties:
        status:
          type: integer
          format: int32
        errorInfo:
          $ref: "#/components/schemas/ErrorInfo"
        code:
          type: integer
          format: int32
        userMessageZh:
          type: string
        userMessageEn:
          type: string
    CallbackConfigRequest:
      type: object
      properties:
        ak:
          type: string
        scope:
          type: string
    CallbackConfigResponse:
      type: object
      properties:
        ak:
          type: string
        scope:
          type: string
        channelType:
          type: integer
          format: int32
        channelAddress:
          type: string
        authType:
          type: integer
          format: int32
    ErrorInfo:
      type: object
      description: ErrorInfo（未在代码中定位到定义，见实现）
    PermissionCheckResponse:
      type: object
      properties:
        authorized:
          type: boolean
        subscriptionId:
          type: string
        subscriptionStatus:
          type: integer
          format: int32
        reason:
          type: string
    UserAuthorizationCreateRequest:
      type: object
      properties:
        userId:
          type: string
        appId:
          type: string
        scopes:
          type: array
          items:
            type: string
        expiresAt:
          type: string
          format: date-time
    UserAuthorizationListResponse:
      type: object
      properties:
        id:
          type: string
        userId:
          type: string
        userName:
          type: string
        appId:
          type: string
        appName:
          type: string
        scopes:
          type: array
          items:
            type: string
        expiresAt:
          type: string
          format: date-time
        createTime:
          type: string
          format: date-time
    UserAuthorizationResponse:
      type: object
      properties:
        id:
          type: string
        userId:
          type: string
        appId:
          type: string
        scopes:
          type: array
          items:
            type: string
        expiresAt:
          type: string
          format: date-time
    UserRoleQueryRequest:
      type: object
      properties:
        appId:
          type: string
        hisAppId:
          type: string
        userAccount:
          type: string
    UserRoleQueryResponse:
      type: object
      properties:
        appId:
          type: string
        roles:
          type: integer
          format: int32
```
