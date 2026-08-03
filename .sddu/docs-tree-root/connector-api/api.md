# connector-api API — API 路由文档

> **文档定位**: sddu-docs-api — API 路由文档 — REST 端点、请求/响应 Schema、状态码  
> **输出文件名**: connector-api-api.md  
> **数据来源**: 代码扫描生成 — connector-api/src/main/java/**/controller/*.java  
> **创建人**: SDDU Docs Agent  
> **创建时间**: 2026-08-03  
> **版本**: v1.0 (CODE-SCAN)  

## 1. API 概述

| 属性 | 值 |
|------|-----|
| **API 名称** | connector-api 连接流执行 API |
| **基础路径** | `/api/v1`（webflux base-path: /connector-api） |
| **所属域** | 连接器平台（数据面） |
| **认证方式** | AKSK/OAuth 凭证（数据面） |

## 2. REST 端点

| 方法 | 路径 | 说明 |
|:----:|------|------|
| POST | /api/v1/flows/{flowId}/invoke | **HTTP 触发**：接收外部系统请求并同步执行连接流，出口节点 body/header 直接作为 HTTP 响应 |
| POST | /api/v1/flows/{flowId}/versions/{versionId}/debug | **版本调试**：直接执行指定版本（无需发布/部署） |

## 3. 请求/响应 Schema

### POST /api/v1/flows/{flowId}/invoke

**请求**（JSON body）:

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| （动态） | object | 否 | 触发数据，按 inputContract 校验（triggerData） |
| （URL Query） | object | 否 | queryParams 透传 |

**响应**:

| 字段 | 类型 | 说明 |
|------|------|------|
| status | int | HTTP 状态（出口节点决定） |
| headers | map | 出口节点返回头 |
| body | object | 出口节点返回体 |

## 4. 状态码

| 状态码 | 含义 | 场景 |
|:------:|------|------|
| 200 | 执行成功 | 连接流正常执行 |
| 400 | 请求/校验错误 | inputContract 校验失败 |
| 401 | 未认证 | 凭证校验失败 |
| 404 | 连接流不存在 | flowId 无效 |
| 409 | 连接流未部署/已停止 | 无 deployed 版本 |
| 429 | 入站限流 | rateLimit 触发 |
| 500/5xx | 下游服务端错误 | 连接器调用失败 |
| 6xxxx | 引擎内部错误 | 执行引擎异常 |

---

## 修订记录

| 版本 | 变更说明 | 日期 | 修订人 |
|------|---------|------|--------|
| v1.0 | 代码扫描全量生成（2 端点） | 2026-08-03 | SDDU Docs Agent |
