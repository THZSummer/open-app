# market-server API — API 路由文档

> **文档定位**: sddu-docs-api — API 路由文档 — REST 端点、请求/响应 Schema、状态码  
> **输出文件名**: market-server-api.md  
> **数据来源**: 代码扫描生成 — market-server/src/main/java/**/controller/*.java  
> **创建人**: SDDU Docs Agent  
> **创建时间**: 2026-08-03  
> **版本**: v1.0 (CODE-SCAN)  

## 1. API 概述

| 属性 | 值 |
|------|-----|
| **API 名称** | market-server 市场管理 API |
| **基础路径** | `/service/open/v2`（context-path: /market-server） |
| **所属域** | 基础配置 / 市场管理 |
| **认证方式** | 内部凭证 + 成员权限 |

## 2. REST 端点

### 2.1 健康检查

| 方法 | 路径 | 说明 |
|:----:|------|------|
| GET | /service/open/v2/health | 健康检查 |
| GET | /service/open/v2/user-info | 当前用户信息 |

### 2.2 数据字典（dictionary）

| 方法 | 路径 | 说明 |
|:----:|------|------|
| GET | /service/open/v2/dictionary/list | 字典列表 |
| POST | /service/open/v2/dictionary | 新增字典 |
| GET | /service/open/v2/dictionary/{id} | 字典详情 |
| PUT | /service/open/v2/dictionary/{id} | 编辑字典 |
| DELETE | /service/open/v2/dictionary/{id} | 删除字典 |

### 2.3 应用审批（approval）

| 方法 | 路径 | 说明 |
|:----:|------|------|
| GET | /service/open/v2/apps/pending | 待审应用列表 |
| GET | /service/open/v2/apps/publish | 已发布应用列表 |
| POST | /service/open/v2/apps/approval | 应用审批处理 |

### 2.4 文件上传（file）

| 方法 | 路径 | 说明 |
|:----:|------|------|
| POST | /service/open/v2/file/upload | 文件上传 |

### 2.5 能力管理（admin）

| 方法 | 路径 | 说明 |
|:----:|------|------|
| GET | /service/open/v2/ability/admin/list | 能力管理列表 |
| POST | /service/open/v2/ability/admin | 新增能力 |
| PUT | /service/open/v2/ability/admin/{id} | 编辑能力 |
| DELETE | /service/open/v2/ability/admin/{id} | 删除能力 |

### 2.6 聊天机器人绑定（chatbotbindtab）

| 方法 | 路径 | 说明 |
|:----:|------|------|
| GET | /service/open/v2/apps/single-chatbot-accounts | 绑定账号列表 |
| POST | /service/open/v2/apps/single-chatbot-accounts | 新增绑定 |
| DELETE | /service/open/v2/apps/single-chatbot-accounts | 删除绑定 |

### 2.7 LookUp 分类（lookup）

| 方法 | 路径 | 说明 |
|:----:|------|------|
| GET | /service/open/v2/lookup/classify/list | 分类列表 |
| POST | /service/open/v2/lookup/classify | 新增分类 |
| PUT | /service/open/v2/lookup/classify/{classifyId} | 编辑分类 |
| DELETE | /service/open/v2/lookup/classify/{classifyId} | 删除分类 |
| GET | /service/open/v2/lookup/classify/{classifyId} | 分类详情 |

### 2.8 LookUp 项（lookup）

| 方法 | 路径 | 说明 |
|:----:|------|------|
| GET | /service/open/v2/lookup/classify/{classifyId}/items | 分类下项列表 |
| POST | /service/open/v2/lookup/classify/{classifyId}/items | 新增项 |
| PUT | /service/open/v2/lookup/items/{itemId} | 编辑项 |
| DELETE | /service/open/v2/lookup/items/{itemId} | 删除项 |
| GET | /service/open/v2/lookup/items/{itemId} | 项详情 |

---

## 3. 请求/响应 Schema

> 标准 CRUD JSON API，响应包裹 `ApiResponse<T>`。字典/分类/项含 name/name_en/status/排序等字段；审批处理含 appId + 审批结果。

## 4. 状态码

| 状态码 | 含义 | 场景 |
|:------:|------|------|
| 200 | 成功 | 常规 |
| 400 | 参数错误 | 校验失败 |
| 401 | 未认证 | 凭证失败 |
| 403 | 无权限 | 非管理员 |
| 404 | 资源不存在 | ID 无效 |
| 500 | 服务端错误 | 异常 |

---

## 修订记录

| 版本 | 变更说明 | 日期 | 修订人 |
|------|---------|------|--------|
| v1.0 | 代码扫描全量生成（~25 端点） | 2026-08-03 | SDDU Docs Agent |
