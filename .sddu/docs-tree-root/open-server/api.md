# open-server API — API 路由文档

> **文档定位**: sddu-docs-api — API 路由文档 — REST 端点、请求/响应 Schema、状态码  
> **输出文件名**: open-server-api.md  
> **数据来源**: 代码扫描生成 — open-server/src/main/java/**/controller/*.java  
> **创建人**: SDDU Docs Agent  
> **创建时间**: 2026-08-03  
> **版本**: v1.0 (CODE-SCAN)  
> **更新人**: —  
> **更新时间**: —  
> **更新说明**: 全量覆盖重建

## 1. API 概述

| 属性 | 值 |
|------|-----|
| **API 名称** | open-server 管理面 API |
| **基础路径** | `/service/open/v2`（context-path: /open-server） |
| **所属域** | 能力开放平台管理面 |
| **认证方式** | 内部凭证（internal auth）+ 成员权限（Owner/管理员/开发者） |

## 2. REST 端点

### 2.1 健康检查

| 方法 | 路径 | 说明 |
|:----:|------|------|
| GET | /service/open/v2/health | 健康检查 |
| GET | /service/open/v2/user-info | 当前用户信息 |

### 2.2 应用管理（app）

| 方法 | 路径 | 说明 |
|:----:|------|------|
| POST | /service/open/v2/app | 创建应用 |
| PUT | /service/open/v2/app | 编辑应用 |
| GET | /service/open/v2/app | 应用详情 |
| GET | /service/open/v2/app/list | 应用列表 |
| GET | /service/open/v2/app/eamap | EAMAP 列表 |
| GET | /service/open/v2/app/icons | 默认图标列表 |
| PUT | /service/open/v2/app/verify-type | 校验应用类型 |
| GET | /service/open/v2/app/identity | 应用身份（AK/SK） |
| GET | /service/open/v2/app/verify-type | 查询类型校验 |
| POST | /service/open/v2/app/bind-eamap | 绑定 EAMAP |
| GET | /service/open/v2/app/current-role | 当前用户角色 |

### 2.3 分类管理（category）

| 方法 | 路径 | 说明 |
|:----:|------|------|
| GET | /service/open/v2/categories | 分类列表 |
| GET | /service/open/v2/categories/{id} | 分类详情 |
| POST | /service/open/v2/categories | 创建分类 |
| PUT | /service/open/v2/categories/{id} | 编辑分类 |
| DELETE | /service/open/v2/categories/{id} | 删除分类 |
| POST | /service/open/v2/categories/{id}/owners | 添加责任人 |
| GET | /service/open/v2/categories/{id}/owners | 责任人列表 |
| DELETE | /service/open/v2/categories/{id}/owners/{userId} | 移除责任人 |

### 2.4 操作日志（auditlog）

| 方法 | 路径 | 说明 |
|:----:|------|------|
| GET | /service/open/v2/operate-log | 操作日志分页查询 |
| GET | /service/open/v2/operate-log/filters | 日志筛选项 |

### 2.5 执行记录（flowexecrecord）

| 方法 | 路径 | 说明 |
|:----:|------|------|
| GET | /service/open/v2/executions | 执行记录列表 |
| GET | /service/open/v2/executions/{executionId} | 执行记录详情 |

### 2.6 审批（approval）

| 方法 | 路径 | 说明 |
|:----:|------|------|
| GET | /service/open/v2/approvals/pending | 我的待办审批 |
| GET | /service/open/v2/approvals/{id} | 审批详情 |
| POST | /service/open/v2/approvals/{id}/approve | 同意 |
| POST | /service/open/v2/approvals/{id}/reject | 拒绝 |
| POST | /service/open/v2/approvals/{id}/cancel | 撤销 |
| POST | /service/open/v2/approvals/batch-approve | 批量同意 |
| POST | /service/open/v2/approvals/batch-reject | 批量拒绝 |
| POST | /service/open/v2/approvals/{id}/urge | 催办 |

### 2.7 审批流程模板（approvalflow）

| 方法 | 路径 | 说明 |
|:----:|------|------|
| GET | /service/open/v2/approval-flows | 模板列表 |
| GET | /service/open/v2/approval-flows/{id} | 模板详情 |
| POST | /service/open/v2/approval-flows | 创建模板 |
| PUT | /service/open/v2/approval-flows/{id} | 编辑模板 |
| DELETE | /service/open/v2/approval-flows/{id} | 删除模板 |

### 2.8 权限/订阅（permission）

| 方法 | 路径 | 说明 |
|:----:|------|------|
| GET | /service/open/v2/apps/{appId}/apis | 应用已订阅 API |
| GET | /service/open/v2/categories/{id}/apis | 分类下 API |
| POST | /service/open/v2/apps/{appId}/apis/subscribe | 订阅 API |
| POST | /service/open/v2/apps/{appId}/apis/{id}/withdraw | 撤回 API 订阅 |
| DELETE | /service/open/v2/apps/{appId}/apis/{id} | 取消 API 订阅 |
| GET | /service/open/v2/apps/{appId}/events | 应用已订阅事件 |
| GET | /service/open/v2/categories/{id}/events | 分类下事件 |
| POST | /service/open/v2/apps/{appId}/events/subscribe | 订阅事件 |
| PUT | /service/open/v2/apps/{appId}/events/{id}/config | 配置事件通道 |
| POST | /service/open/v2/apps/{appId}/events/{id}/withdraw | 撤回事件订阅 |
| DELETE | /service/open/v2/apps/{appId}/events/{id} | 取消事件订阅 |
| GET | /service/open/v2/apps/{appId}/callbacks | 应用已订阅回调 |
| GET | /service/open/v2/categories/{id}/callbacks | 分类下回调 |
| POST | /service/open/v2/apps/{appId}/callbacks/subscribe | 订阅回调 |
| PUT | /service/open/v2/apps/{appId}/callbacks/{id}/config | 配置回调通道 |
| POST | /service/open/v2/apps/{appId}/callbacks/{id}/withdraw | 撤回回调订阅 |
| DELETE | /service/open/v2/apps/{appId}/callbacks/{id} | 取消回调订阅 |

### 2.9 成员管理（member）

| 方法 | 路径 | 说明 |
|:----:|------|------|
| GET | /service/open/v2/member/list | 成员列表 |
| POST | /service/open/v2/member | 添加成员 |
| DELETE | /service/open/v2/member | 删除成员 |
| POST | /service/open/v2/member/transfer-owner | 转移 Owner |
| GET | /service/open/v2/member/search-users | 用户搜索 |

### 2.10 卡片设置（card）

| 方法 | 路径 | 说明 |
|:----:|------|------|
| GET | /service/open/v2/apps/{appId}/card-settings | 查询卡片设置 |
| PUT | /service/open/v2/apps/{appId}/card-settings | 更新卡片设置 |

### 2.11 连接器版本（connectorversion）

| 方法 | 路径 | 说明 |
|:----:|------|------|
| POST | /service/open/v2/connectors/{connectorId}/versions | 创建版本 |
| GET | /service/open/v2/connectors/{connectorId}/versions | 版本列表 |
| GET | /service/open/v2/connectors/{connectorId}/versions/{versionId} | 版本详情 |
| PUT | /service/open/v2/connectors/{connectorId}/versions/{versionId} | 编辑版本 |
| PUT | /service/open/v2/connectors/{connectorId}/versions/{versionId}/publish | 发布版本 |
| POST | /service/open/v2/connectors/{connectorId}/versions/{versionId}/copy-to-draft | 复制为草稿 |
| PUT | /service/open/v2/connectors/{connectorId}/versions/{versionId}/invalidate | 失效版本 |
| PUT | /service/open/v2/connectors/{connectorId}/versions/{versionId}/recover | 恢复版本 |
| DELETE | /service/open/v2/connectors/{connectorId}/versions/{versionId} | 删除版本 |

### 2.12 通用文件（commonfile）

| 方法 | 路径 | 说明 |
|:----:|------|------|
| POST | /service/open/v2/common-file/upload | 通用文件上传 |

### 2.13 事件资源（event）

| 方法 | 路径 | 说明 |
|:----:|------|------|
| GET | /service/open/v2/events | 事件列表 |
| GET | /service/open/v2/events/{id} | 事件详情 |
| POST | /service/open/v2/events | 注册事件 |
| PUT | /service/open/v2/events/{id} | 编辑事件 |
| DELETE | /service/open/v2/events/{id} | 删除事件 |
| POST | /service/open/v2/events/{id}/withdraw | 撤回事件 |

### 2.14 回调资源（callback）

| 方法 | 路径 | 说明 |
|:----:|------|------|
| GET | /service/open/v2/callbacks | 回调列表 |
| GET | /service/open/v2/callbacks/{id} | 回调详情 |
| POST | /service/open/v2/callbacks | 注册回调 |
| PUT | /service/open/v2/callbacks/{id} | 编辑回调 |
| DELETE | /service/open/v2/callbacks/{id} | 删除回调 |
| POST | /service/open/v2/callbacks/{id}/withdraw | 撤回回调 |

### 2.15 同步（sync）

| 方法 | 路径 | 说明 |
|:----:|------|------|
| POST | /service/open/v2/sync/subscription/migrate | 订阅数据迁移 |
| POST | /service/open/v2/sync/subscription/rollback | 迁移回滚 |
| POST | /service/open/v2/sync/subscription/emergency/update-old | 紧急修复旧数据 |
| POST | /service/open/v2/sync/subscription/emergency/update-new | 紧急修复新数据 |

### 2.16 连接器（connector）

| 方法 | 路径 | 说明 |
|:----:|------|------|
| POST | /service/open/v2/connectors | 创建连接器 |
| GET | /service/open/v2/connectors | 连接器列表 |
| GET | /service/open/v2/connectors/{connectorId} | 连接器详情 |
| PUT | /service/open/v2/connectors/{connectorId} | 编辑连接器 |
| PUT | /service/open/v2/connectors/{connectorId}/invalidate | 失效连接器 |
| PUT | /service/open/v2/connectors/{connectorId}/recover | 恢复连接器 |
| DELETE | /service/open/v2/connectors/{connectorId} | 删除连接器 |

### 2.17 连接流版本（flowversion）

| 方法 | 路径 | 说明 |
|:----:|------|------|
| POST | /service/open/v2/flows/{flowId}/versions | 创建版本 |
| GET | /service/open/v2/flows/{flowId}/versions | 版本列表 |
| GET | /service/open/v2/flows/{flowId}/versions/{versionId} | 版本详情 |
| PUT | /service/open/v2/flows/{flowId}/versions/{versionId} | 编辑版本 |
| POST | /service/open/v2/flows/{flowId}/versions/{versionId}/publish | 发布（提交审批） |
| POST | /service/open/v2/flows/{flowId}/versions/{versionId}/copy-to-draft | 复制为草稿 |
| PUT | /service/open/v2/flows/{flowId}/versions/{versionId}/invalidate | 失效版本 |
| PUT | /service/open/v2/flows/{flowId}/versions/{versionId}/recover | 恢复版本 |
| DELETE | /service/open/v2/flows/{flowId}/versions/{versionId} | 删除版本 |
| POST | /service/open/v2/flows/{flowId}/versions/{versionId}/cancel | 撤回审批 |
| POST | /service/open/v2/flows/{flowId}/versions/{versionId}/urge | 审批催办 |

### 2.18 能力（ability）

| 方法 | 路径 | 说明 |
|:----:|------|------|
| GET | /service/open/v2/ability/list | 能力列表 |
| POST | /service/open/v2/ability | 创建能力 |
| GET | /service/open/v2/ability/subscribed | 已订阅能力 |

### 2.19 应用版本（version）

| 方法 | 路径 | 说明 |
|:----:|------|------|
| GET | /service/open/v2/version/list | 版本列表 |
| POST | /service/open/v2/version | 创建版本 |
| GET | /service/open/v2/version | 版本详情 |
| POST | /service/open/v2/version/publish | 发布版本 |
| POST | /service/open/v2/version/withdraw | 撤回版本 |
| DELETE | /service/open/v2/version | 删除版本 |
| PUT | /service/open/v2/version | 编辑版本 |

### 2.20 连接流（flow）

| 方法 | 路径 | 说明 |
|:----:|------|------|
| POST | /service/open/v2/flows | 创建连接流 |
| GET | /service/open/v2/flows | 连接流列表 |
| GET | /service/open/v2/flows/{flowId} | 连接流详情 |
| PUT | /service/open/v2/flows/{flowId} | 编辑连接流 |
| POST | /service/open/v2/flows/{flowId}/copy | 复制连接流 |
| POST | /service/open/v2/flows/{flowId}/deploy | 部署版本 |
| POST | /service/open/v2/flows/{flowId}/start | 启动 |
| POST | /service/open/v2/flows/{flowId}/stop | 停止 |
| PUT | /service/open/v2/flows/{flowId}/invalidate | 失效 |
| PUT | /service/open/v2/flows/{flowId}/recover | 恢复 |
| DELETE | /service/open/v2/flows/{flowId} | 删除 |

### 2.21 API 资源（api）

| 方法 | 路径 | 说明 |
|:----:|------|------|
| GET | /service/open/v2/apis | API 列表 |
| GET | /service/open/v2/apis/{id} | API 详情 |
| POST | /service/open/v2/apis | 注册 API |
| PUT | /service/open/v2/apis/{id} | 编辑 API |
| DELETE | /service/open/v2/apis/{id} | 删除 API |
| POST | /service/open/v2/apis/{id}/withdraw | 撤回 API |

### 2.22 LookUp / 文件（common）

| 方法 | 路径 | 说明 |
|:----:|------|------|
| GET | /service/open/v2/lookup/whitelist | LookUp 白名单 |
| POST | /service/open/v2/file/upload-image | 图片上传 |

---

## 3. 请求/响应 Schema

> 本域为标准 CRUD + 状态流转 API，请求/响应均为 JSON。通用响应包裹结构由 `ApiResponse<T>` 提供（data/error 字段）。状态流转端点（approve/reject/publish/withdraw/deploy/start/stop）以 POST/PUT 触发，通过 `status` 字段状态机推进。

## 4. 状态码

| 状态码 | 含义 | 场景 |
|:------:|------|------|
| 200 | 成功 | 常规操作 |
| 400 | 参数错误 | 校验失败 |
| 401 | 未认证 | 内部凭证缺失/无效 |
| 403 | 无权限 | 非成员/非 Owner |
| 404 | 资源不存在 | ID 无效 |
| 409 | 状态冲突 | 非法状态流转 |
| 429 | 限流 | 连接流入站限流 |
| 500 | 服务端错误 | 异常 |

---

## 修订记录

| 版本 | 变更说明 | 日期 | 修订人 |
|------|---------|------|--------|
| v1.0 | 代码扫描全量生成（~80 端点） | 2026-08-03 | SDDU Docs Agent |
