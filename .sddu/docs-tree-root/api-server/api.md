# api-server API — API 路由文档

> **文档定位**: sddu-docs-api — API 路由文档 — REST 端点、请求/响应 Schema、状态码  
> **输出文件名**: api-server-api.md  
> **数据来源**: 代码扫描生成 — api-server/src/main/java/**/controller/*.java  
> **创建人**: SDDU Docs Agent  
> **创建时间**: 2026-08-03  
> **版本**: v1.0 (CODE-SCAN)  

## 1. API 概述

| 属性 | 值 |
|------|-----|
| **API 名称** | api-server 消费网关 API |
| **基础路径** | `/api/v1` + `/gateway`（context-path: /api-server） |
| **所属域** | 数据开放平台 / 消费网关 |
| **认证方式** | 内部凭证 + SOA/APIG/AKSK 认证头 |

## 2. REST 端点

### 2.1 健康检查

| 方法 | 路径 | 说明 |
|:----:|------|------|
| GET | /api/v1/health | 健康检查 |

### 2.2 数据查询（data）

| 方法 | 路径 | 说明 |
|:----:|------|------|
| GET | /gateway/permissions/check | 权限校验（是否可访问数据） |
| GET | /gateway/permissions/subscribers | 订阅方列表 |
| GET | /gateway/subscriptions/config | 订阅配置 |
| GET | /gateway/permissions/detail | 权限详情 |

### 2.3 API 消费网关（gateway）

| 方法 | 路径 | 说明 |
|:----:|------|------|
| ANY | /gateway/api/** | API 网关代理（GET/POST/PUT/DELETE，转发到下游能力） |
| POST | /gateway/assistant/callbacks/config | 助手回调配置 |

### 2.4 用户授权（scope）

| 方法 | 路径 | 说明 |
|:----:|------|------|
| GET | /api/v1/user-authorizations | 用户授权列表 |
| POST | /api/v1/user-authorizations | 授予授权 |
| DELETE | /api/v1/user-authorizations/{id} | 撤销授权 |

### 2.5 审批回调（approval）

| 方法 | 路径 | 说明 |
|:----:|------|------|
| POST | /api/v1/approvals/callback | 审批结果回调 |

### 2.6 内部角色（appmember）

| 方法 | 路径 | 说明 |
|:----:|------|------|
| POST | /service/open/v2/internal/user/roles | 内部角色同步 |

---

## 3. 请求/响应 Schema

> 网关代理端点透传下游请求/响应，无固定 Schema。授权端点请求体含 user_id / app_id / scopes[] / expires_at。审批回调含 approvalId / businessType / result。

## 4. 状态码

| 状态码 | 含义 | 场景 |
|:------:|------|------|
| 200 | 成功 | 常规 |
| 401 | 未认证 | 凭证校验失败 |
| 403 | 无权限 | 权限检查不通过 |
| 404 | 网关路由未匹配 | /gateway/api/** 无下游 |
| 429 | 限流 | 网关限流 |

---

## 修订记录

| 版本 | 变更说明 | 日期 | 修订人 |
|------|---------|------|--------|
| v1.0 | 代码扫描全量生成（~14 端点） | 2026-08-03 | SDDU Docs Agent |
