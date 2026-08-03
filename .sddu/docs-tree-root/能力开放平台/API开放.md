# API 开放

> **文档定位**: sddu-docs-object — 业务对象文档 — 公共连接能力 R1
> **输出文件名**: API开放.md
> **数据来源**: 能力开放平台 spec/discovery + 代码扫描设计真相
> **创建时间**: 2026-08-03
> **版本**: v1.0 (BIZ-ARCH)

## 1. 业务概述

| 属性 | 值 |
|------|-----|
| **能力类型** | 连接能力 · 公共连接能力（R1） |
| **对应 Feature** | CAP-OPEN-001（能力开放平台） |
| **状态** | ✅ validated（全流程完成） |
| **业务角色** | 三方平台通过 API 调用消费 XXX 通讯系统能力（IM/会议/云盘等），是开放平台的核心消费形式之一 |

**业务说明**（G3）：支持 API 的**注册、订阅、取消订阅**、编辑、关联分组、分类、树形分组管理；分类维度：应用类型（业务/个人）、认证类型（应用类 A/B、开放应用凭证），多对多关系。

**消费链路**：`三方应用 → api-server 网关（/gateway/api/**）→ 基于订阅关系鉴权 → 下游能力（IM/会议/云盘）`

**认证类型**：应用类 A/B、开放应用凭证（AK/SK 鉴权头 SOA/APIG/AKSK）。

## 2. 核心功能

| 功能域 | 功能 | 说明 |
|--------|------|------|
| **API 管理** | 注册/编辑/删除/撤回 | API 资源生命周期管理（open-server） |
| **API 订阅** | 订阅/撤回/取消 | 应用订阅 API 权限（权限中心协作） |
| **API 消费网关** | 网关代理 | api-server /gateway/api/** 代理转发，基于订阅鉴权 |
| **分类治理** | 树形分组 | 分类管理 + 责任人（category_owner） |
| **API 详情** | 目录查询 | 消费方浏览 API 树 |

## 3. 设计真相

### 3.1 相关表

| 表名 | 说明 | 关键字段 |
|------|------|---------|
| `v2_api_t` | API 资源表 | 定义 API 基础信息 |
| `v2_api_p_t` | API 属性表 | parent_id → v2_api_t.id, 扩展属性 |
| `v2_category_t` | 分类表 | 树形分组，资源类型 |
| `v2_permission_t` | 权限表 | resource_type=API, resource_id → v2_api_t |
| `v2_subscription_t` | 订阅表 | 应用订阅 API 权限 |
| `v2_category_owner_t` | 分类责任人 | 责任人配置 |

### 3.2 相关 API

| 方法 | 路径 | 说明 |
|:----:|------|------|
| GET | /service/open/v2/apis | API 列表（管理面） |
| POST | /service/open/v2/apis | 注册 API |
| PUT | /service/open/v2/apis/{id} | 编辑 API |
| DELETE | /service/open/v2/apis/{id} | 删除 API |
| POST | /service/open/v2/apis/{id}/withdraw | 撤回 API |
| GET | /service/open/v2/apps/{appId}/apis | 应用已订阅 API |
| POST | /service/open/v2/apps/{appId}/apis/subscribe | 订阅 API |
| DELETE | /service/open/v2/apps/{appId}/apis/{id} | 取消 API 订阅 |
| ANY | /gateway/api/** | API 消费网关代理（api-server） |
| GET | /gateway/permissions/check | 权限校验（api-server） |

### 3.3 工程映射

| 服务 | 角色 |
|------|------|
| open-server | API 资源管理（模块 api）+ 订阅（模块 permission） |
| api-server | API 消费网关（/gateway/api/**）+ 权限校验 |
| wecodesite | 开发者控制台（API 管理/订阅页面） |

---

## 修订记录

| 版本 | 变更说明 | 日期 | 修订人 |
|------|---------|------|--------|
| v1.0 | 业务视角重建 | 2026-08-03 | SDDU Docs Agent |
