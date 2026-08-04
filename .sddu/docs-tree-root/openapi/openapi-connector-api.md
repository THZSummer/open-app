# connector-api 连接流执行引擎 — OpenAPI 契约

> **文档定位**: sddu-docs-api — API 路由文档（GitHub 可渲染包装视图）
> **原始契约**: [openapi-connector-api.yaml](openapi-connector-api.yaml)（机器可读 OpenAPI 3.0.3）
> **服务地址**: http://localhost:18180/connector-api
> **对应全景**: [api.md](../api.md)（业务视角接口清单）
> **生成方式**: 静态分析（controller 注解 + DTO 字段），未运行服务
> **生成时间**: 2026-08-03

## 1. 服务信息

| 属性 | 值 |
|------|-----|
| **服务** | connector-api |
| **标题** | connector-api 连接流执行引擎 |
| **服务地址** | `http://localhost:18180/connector-api` |
| **OpenAPI 版本** | 3.0.3 |
| **接口标签** | HTTP 触发, 测试执行 |
| **端点数** | 2 |
| **Schema 数** | 2 |

## 2. 认证方式

- **akskAuth**（X-AKSK-TOKEN）— 消费面 AKSK，另支持 X-SOA-TOKEN / X-APIG-APPID

## 3. 端点概览

| 方法 | 路径 | 摘要 |
|:----:|------|------|
| POST | `/api/v1/flows/{flowId}/invoke` | HTTP 触发连接流 (v5.8 透明穿透) |
| POST | `/api/v1/flows/{flowId}/versions/{versionId}/debug` | 执行测试运行 (v5.5) |

## 4. OpenAPI 规范（完整 YAML）

> 下方为完整 OpenAPI 3.0.3 契约，可直接用于 openapi-generator / redoc / Swagger UI。

```yaml
# connector-api 连接流执行引擎 API
# 生成方式: 静态分析（controller 注解 + DTO 字段），未运行服务
# 对应全景: .sddu/docs-tree-root/api.md（业务视角接口清单）

openapi: 3.0.3
info:
  title: connector-api 连接流执行引擎 API
  version: v1.0
  description: 连接流执行引擎：HTTP 触发执行、版本调试（公共连接能力 R4 消费面，GraalJS 脚本沙箱）
servers:
  -
    url: "http://localhost:18180/connector-api"
    description: connector-api 本地服务
tags:
  - name: HTTP 触发
  - name: 测试执行
paths:
  /api/v1/flows/{flowId}/invoke:
    post:
      tags:
        - HTTP 触发
      summary: HTTP 触发连接流 (v5.8 透明穿透)
      operationId: invokeFlow_post
      security:
        - akskAuth: []
      parameters:
        -
          name: flowId
          in: path
          required: true
          schema:
            type: integer
            format: int64
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
  /api/v1/flows/{flowId}/versions/{versionId}/debug:
    post:
      tags:
        - 测试执行
      summary: 执行测试运行 (v5.5)
      operationId: executeTestRun_post
      security:
        - akskAuth: []
      parameters:
        -
          name: flowId
          in: path
          required: true
          schema:
            type: integer
            format: int64
        -
          name: versionId
          in: path
          required: true
          schema:
            type: integer
            format: int64
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: "#/components/schemas/TestRunRequest"
      responses:
        200:
          description: 成功
          content:
            application/json:
              schema:
                $ref: "#/components/schemas/ExecutionResult"
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
    ExecutionResult:
      type: object
      description: ExecutionResult（未在代码中定位到定义，见实现）
    TestRunRequest:
      type: object
      description: TestRunRequest（未在代码中定位到定义，见实现）
```
