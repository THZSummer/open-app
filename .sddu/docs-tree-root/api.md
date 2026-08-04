# open-app 全部接口清单

> **文档定位**: sddu-docs-api — API 路由文档 — 全量 REST 端点、消费网关、管理面
> **输出文件名**: api.md
> **数据来源**: 代码扫描 — 各服务 `**/controller/*.java`（5 个服务，184 端点）
> **创建人**: SDDU Docs Agent
> **创建时间**: 2026-08-03
> **版本**: v1.0 (BIZ-ARCH)

## 1. 接口总览

| 服务 | 业务角色 | 端点数 | 基础路径 |
|------|---------|:----:|---------|
| **open-server** | 能力开放平台管理面 | 133 | /service/open/v2 |
| **market-server** | 市场管理面 | 26 | /service/open/v2 |
| **api-server** | 消费网关（数据/API/授权） | 11 | /api/v1 + /gateway |
| **event-server** | 事件/回调网关 | 9 | /gateway + /sse + /ws |
| **connector-api** | 连接流执行引擎 | 2 | /api/v1 |
| **合计** | | **184** | |

> 📖 接口按**业务能力**组织（§2~§7），服务归属见各接口表"服务"列。认证方式：管理面内部凭证 + 成员权限；消费面 SOA/APIG/AKSK 认证头。

---

## 2. 消费网关（三方消费面）

### 2.1 API 消费网关（api-server）

| 方法 | 路径 | 说明 | 服务 |
|:----:|------|------|:----:|
| ANY | /gateway/api/** | API 网关代理（转发下游能力，基于订阅鉴权） | api-server |
| GET | /gateway/permissions/check | 权限校验（是否可访问） | api-server |
| GET | /gateway/permissions/subscribers | 订阅方列表 | api-server |
| GET | /gateway/permissions/detail | 权限详情（按 scope） | api-server |
| GET | /gateway/subscriptions/config | 订阅配置 | api-server |
| POST | /gateway/assistant/callbacks/config | 助手回调配置 | api-server |

### 2.2 事件/回调网关（event-server）

| 方法 | 路径 | 说明 | 服务 |
|:----:|------|------|:----:|
| POST | /gateway/events/publish | 发布事件（topic → 推送订阅方） | event-server |
| DELETE | /gateway/events/cache/{topic} | 清除某 topic 订阅缓存 | event-server |
| POST | /gateway/callbacks/invoke | 触发回调（scope → 调用消费方 WebHook） | event-server |
| DELETE | /gateway/callbacks/cache/{scope} | 清除某 scope 订阅缓存 | event-server |
| GET | /sse/connect/{connectionId} | 建立 SSE 长连接 | event-server |
| DELETE | /sse/disconnect/{connectionId} | 断开 SSE 连接 | event-server |
| GET | /sse/status | SSE 通道状态 | event-server |
| GET | /ws/status | WebSocket 状态 | event-server |
| GET | /ws/count | WebSocket 连接数 | event-server |

### 2.3 连接流执行（connector-api）

| 方法 | 路径 | 说明 | 服务 |
|:----:|------|------|:----:|
| POST | /api/v1/flows/{flowId}/invoke | **HTTP 触发**：同步执行连接流，出口节点作为响应 | connector-api |
| POST | /api/v1/flows/{flowId}/versions/{versionId}/debug | **版本调试**：直接执行指定版本（无需发布） | connector-api |

---

## 3. 平台基础能力（open-server / market-server）

### 3.1 应用管理

| 方法 | 路径 | 说明 | 服务 |
|:----:|------|------|:----:|
| POST | /service/open/v2/app | 创建应用 | open-server |
| PUT | /service/open/v2/app | 编辑应用 | open-server |
| GET | /service/open/v2/app | 应用详情 | open-server |
| GET | /service/open/v2/app/list | 应用列表 | open-server |
| GET | /service/open/v2/app/eamap | EAMAP 列表 | open-server |
| GET | /service/open/v2/app/icons | 默认图标列表 | open-server |
| PUT | /service/open/v2/app/verify-type | 校验应用类型 | open-server |
| GET | /service/open/v2/app/identity | 应用身份（AK/SK） | open-server |
| GET | /service/open/v2/app/verify-type | 查询类型校验 | open-server |
| POST | /service/open/v2/app/bind-eamap | 绑定 EAMAP | open-server |
| GET | /service/open/v2/app/current-role | 当前用户角色 | open-server |
| GET | /service/open/v2/member/list | 成员列表 | open-server |
| POST | /service/open/v2/member | 添加成员 | open-server |
| DELETE | /service/open/v2/member | 删除成员 | open-server |
| POST | /service/open/v2/member/transfer-owner | 转移 Owner | open-server |
| GET | /service/open/v2/member/search-users | 用户搜索 | open-server |
| GET | /service/open/v2/version/list | 版本列表 | open-server |
| POST | /service/open/v2/version | 创建版本 | open-server |
| GET | /service/open/v2/version | 版本详情 | open-server |
| POST | /service/open/v2/version/publish | 发布版本 | open-server |
| POST | /service/open/v2/version/withdraw | 撤回版本 | open-server |
| DELETE | /service/open/v2/version | 删除版本 | open-server |
| PUT | /service/open/v2/version | 编辑版本 | open-server |
| GET | /service/open/v2/apps/{appId}/card-settings | 查询卡片设置 | open-server |
| PUT | /service/open/v2/apps/{appId}/card-settings | 更新卡片设置 | open-server |
| GET | /service/open/v2/apps/single-chatbot-accounts | 聊天机器人绑定账号列表 | market-server |
| POST | /service/open/v2/apps/single-chatbot-accounts | 新增绑定 | market-server |
| DELETE | /service/open/v2/apps/single-chatbot-accounts | 删除绑定 | market-server |

### 3.2 资源分类

| 方法 | 路径 | 说明 | 服务 |
|:----:|------|------|:----:|
| GET | /service/open/v2/categories | 分类列表 | open-server |
| GET | /service/open/v2/categories/{id} | 分类详情 | open-server |
| POST | /service/open/v2/categories | 创建分类 | open-server |
| PUT | /service/open/v2/categories/{id} | 编辑分类 | open-server |
| DELETE | /service/open/v2/categories/{id} | 删除分类 | open-server |
| POST | /service/open/v2/categories/{id}/owners | 添加责任人 | open-server |
| GET | /service/open/v2/categories/{id}/owners | 责任人列表 | open-server |
| DELETE | /service/open/v2/categories/{id}/owners/{userId} | 移除责任人 | open-server |

### 3.3 嵌入能力

| 方法 | 路径 | 说明 | 服务 |
|:----:|------|------|:----:|
| GET | /service/open/v2/ability/list | 能力列表 | open-server |
| POST | /service/open/v2/ability | 创建能力 | open-server |
| GET | /service/open/v2/ability/subscribed | 已订阅能力 | open-server |
| GET | /service/open/v2/ability/admin/list | 能力管理列表（平台面） | market-server |
| POST | /service/open/v2/ability/admin | 新增能力（平台面） | market-server |
| PUT | /service/open/v2/ability/admin/{id} | 编辑能力（平台面） | market-server |
| DELETE | /service/open/v2/ability/admin/{id} | 删除能力（平台面） | market-server |
| POST | /service/open/v2/internal/user/roles | 内部角色同步（API面） | api-server |

### 3.4 审批管理

| 方法 | 路径 | 说明 | 服务 |
|:----:|------|------|:----:|
| GET | /service/open/v2/approvals/pending | 我的待办审批 | open-server |
| GET | /service/open/v2/approvals/{id} | 审批详情 | open-server |
| POST | /service/open/v2/approvals/{id}/approve | 同意 | open-server |
| POST | /service/open/v2/approvals/{id}/reject | 拒绝 | open-server |
| POST | /service/open/v2/approvals/{id}/cancel | 撤销 | open-server |
| POST | /service/open/v2/approvals/batch-approve | 批量同意 | open-server |
| POST | /service/open/v2/approvals/batch-reject | 批量拒绝 | open-server |
| POST | /service/open/v2/approvals/{id}/urge | 催办 | open-server |
| GET | /service/open/v2/approval-flows | 模板列表 | open-server |
| GET | /service/open/v2/approval-flows/{id} | 模板详情 | open-server |
| POST | /service/open/v2/approval-flows | 创建模板 | open-server |
| PUT | /service/open/v2/approval-flows/{id} | 编辑模板 | open-server |
| DELETE | /service/open/v2/approval-flows/{id} | 删除模板 | open-server |
| GET | /service/open/v2/apps/pending | 待审应用列表 | market-server |
| GET | /service/open/v2/apps/publish | 已发布应用列表 | market-server |
| POST | /service/open/v2/apps/approval | 应用审批处理 | market-server |
| POST | /api/v1/approvals/callback | 审批结果回调 | api-server |

### 3.5 基础数据（数据字典 / LookUp）

| 方法 | 路径 | 说明 | 服务 |
|:----:|------|------|:----:|
| GET | /service/open/v2/dictionary/list | 字典列表 | market-server |
| POST | /service/open/v2/dictionary | 新增字典 | market-server |
| GET | /service/open/v2/dictionary/{id} | 字典详情 | market-server |
| PUT | /service/open/v2/dictionary/{id} | 编辑字典 | market-server |
| DELETE | /service/open/v2/dictionary/{id} | 删除字典 | market-server |
| GET | /service/open/v2/lookup/classify/list | LookUp 分类列表 | market-server |
| POST | /service/open/v2/lookup/classify | 新增分类 | market-server |
| PUT | /service/open/v2/lookup/classify/{classifyId} | 编辑分类 | market-server |
| DELETE | /service/open/v2/lookup/classify/{classifyId} | 删除分类 | market-server |
| GET | /service/open/v2/lookup/classify/{classifyId} | 分类详情 | market-server |
| GET | /service/open/v2/lookup/classify/{classifyId}/items | 分类下项列表 | market-server |
| POST | /service/open/v2/lookup/classify/{classifyId}/items | 新增项 | market-server |
| PUT | /service/open/v2/lookup/items/{itemId} | 编辑项 | market-server |
| DELETE | /service/open/v2/lookup/items/{itemId} | 删除项 | market-server |
| GET | /service/open/v2/lookup/items/{itemId} | 项详情 | market-server |
| GET | /service/open/v2/lookup/whitelist | LookUp 白名单 | open-server |

### 3.6 辅助能力（操作审计 / 文件）

| 方法 | 路径 | 说明 | 服务 |
|:----:|------|------|:----:|
| GET | /service/open/v2/operate-log | 操作日志分页查询 | open-server |
| GET | /service/open/v2/operate-log/filters | 日志筛选项 | open-server |
| POST | /service/open/v2/common-file/upload | 通用文件上传 | open-server |
| POST | /service/open/v2/file/upload-image | 图片上传 | open-server |
| POST | /service/open/v2/file/upload | 文件上传 | market-server |

---

## 4. 公共连接能力（资源管理）

### 4.1 API 开放（R1）

| 方法 | 路径 | 说明 | 服务 |
|:----:|------|------|:----:|
| GET | /service/open/v2/apis | API 列表 | open-server |
| GET | /service/open/v2/apis/{id} | API 详情 | open-server |
| POST | /service/open/v2/apis | 注册 API | open-server |
| PUT | /service/open/v2/apis/{id} | 编辑 API | open-server |
| DELETE | /service/open/v2/apis/{id} | 删除 API | open-server |
| POST | /service/open/v2/apis/{id}/withdraw | 撤回 API | open-server |
| GET | /service/open/v2/apps/{appId}/apis | 应用已订阅 API | open-server |
| GET | /service/open/v2/categories/{id}/apis | 分类下 API | open-server |
| POST | /service/open/v2/apps/{appId}/apis/subscribe | 订阅 API | open-server |
| POST | /service/open/v2/apps/{appId}/apis/{id}/withdraw | 撤回 API 订阅 | open-server |
| DELETE | /service/open/v2/apps/{appId}/apis/{id} | 取消 API 订阅 | open-server |

### 4.2 事件开放（R2）

| 方法 | 路径 | 说明 | 服务 |
|:----:|------|------|:----:|
| GET | /service/open/v2/events | 事件列表 | open-server |
| GET | /service/open/v2/events/{id} | 事件详情 | open-server |
| POST | /service/open/v2/events | 注册事件 | open-server |
| PUT | /service/open/v2/events/{id} | 编辑事件 | open-server |
| DELETE | /service/open/v2/events/{id} | 删除事件 | open-server |
| POST | /service/open/v2/events/{id}/withdraw | 撤回事件 | open-server |
| GET | /service/open/v2/apps/{appId}/events | 应用已订阅事件 | open-server |
| GET | /service/open/v2/categories/{id}/events | 分类下事件 | open-server |
| POST | /service/open/v2/apps/{appId}/events/subscribe | 订阅事件 | open-server |
| PUT | /service/open/v2/apps/{appId}/events/{id}/config | 配置事件通道 | open-server |
| POST | /service/open/v2/apps/{appId}/events/{id}/withdraw | 撤回事件订阅 | open-server |
| DELETE | /service/open/v2/apps/{appId}/events/{id} | 取消事件订阅 | open-server |

### 4.3 回调开放（R3）

| 方法 | 路径 | 说明 | 服务 |
|:----:|------|------|:----:|
| GET | /service/open/v2/callbacks | 回调列表 | open-server |
| GET | /service/open/v2/callbacks/{id} | 回调详情 | open-server |
| POST | /service/open/v2/callbacks | 注册回调 | open-server |
| PUT | /service/open/v2/callbacks/{id} | 编辑回调 | open-server |
| DELETE | /service/open/v2/callbacks/{id} | 删除回调 | open-server |
| POST | /service/open/v2/callbacks/{id}/withdraw | 撤回回调 | open-server |
| GET | /service/open/v2/apps/{appId}/callbacks | 应用已订阅回调 | open-server |
| GET | /service/open/v2/categories/{id}/callbacks | 分类下回调 | open-server |
| POST | /service/open/v2/apps/{appId}/callbacks/subscribe | 订阅回调 | open-server |
| PUT | /service/open/v2/apps/{appId}/callbacks/{id}/config | 配置回调通道 | open-server |
| POST | /service/open/v2/apps/{appId}/callbacks/{id}/withdraw | 撤回回调订阅 | open-server |
| DELETE | /service/open/v2/apps/{appId}/callbacks/{id} | 取消回调订阅 | open-server |

---

## 5. 连接器开放（R4 — 管理面）

| 方法 | 路径 | 说明 | 服务 |
|:----:|------|------|:----:|
| POST | /service/open/v2/connectors | 创建连接器 | open-server |
| GET | /service/open/v2/connectors | 连接器列表 | open-server |
| GET | /service/open/v2/connectors/{connectorId} | 连接器详情 | open-server |
| PUT | /service/open/v2/connectors/{connectorId} | 编辑连接器 | open-server |
| PUT | /service/open/v2/connectors/{connectorId}/invalidate | 失效连接器 | open-server |
| PUT | /service/open/v2/connectors/{connectorId}/recover | 恢复连接器 | open-server |
| DELETE | /service/open/v2/connectors/{connectorId} | 删除连接器 | open-server |
| POST | /service/open/v2/connectors/{connectorId}/versions | 创建版本 | open-server |
| GET | /service/open/v2/connectors/{connectorId}/versions | 版本列表 | open-server |
| GET | /service/open/v2/connectors/{connectorId}/versions/{versionId} | 版本详情 | open-server |
| PUT | /service/open/v2/connectors/{connectorId}/versions/{versionId} | 编辑版本 | open-server |
| PUT | /service/open/v2/connectors/{connectorId}/versions/{versionId}/publish | 发布版本 | open-server |
| POST | /service/open/v2/connectors/{connectorId}/versions/{versionId}/copy-to-draft | 复制为草稿 | open-server |
| PUT | /service/open/v2/connectors/{connectorId}/versions/{versionId}/invalidate | 失效版本 | open-server |
| PUT | /service/open/v2/connectors/{connectorId}/versions/{versionId}/recover | 恢复版本 | open-server |
| DELETE | /service/open/v2/connectors/{connectorId}/versions/{versionId} | 删除版本 | open-server |
| POST | /service/open/v2/flows | 创建连接流 | open-server |
| GET | /service/open/v2/flows | 连接流列表 | open-server |
| GET | /service/open/v2/flows/{flowId} | 连接流详情 | open-server |
| PUT | /service/open/v2/flows/{flowId} | 编辑连接流 | open-server |
| POST | /service/open/v2/flows/{flowId}/copy | 复制连接流 | open-server |
| POST | /service/open/v2/flows/{flowId}/deploy | 部署版本 | open-server |
| POST | /service/open/v2/flows/{flowId}/start | 启动 | open-server |
| POST | /service/open/v2/flows/{flowId}/stop | 停止 | open-server |
| PUT | /service/open/v2/flows/{flowId}/invalidate | 失效 | open-server |
| PUT | /service/open/v2/flows/{flowId}/recover | 恢复 | open-server |
| DELETE | /service/open/v2/flows/{flowId} | 删除 | open-server |
| POST | /service/open/v2/flows/{flowId}/versions | 创建流版本 | open-server |
| GET | /service/open/v2/flows/{flowId}/versions | 流版本列表 | open-server |
| GET | /service/open/v2/flows/{flowId}/versions/{versionId} | 流版本详情 | open-server |
| PUT | /service/open/v2/flows/{flowId}/versions/{versionId} | 编辑流版本 | open-server |
| POST | /service/open/v2/flows/{flowId}/versions/{versionId}/publish | 发布（提交审批） | open-server |
| POST | /service/open/v2/flows/{flowId}/versions/{versionId}/copy-to-draft | 复制为草稿 | open-server |
| PUT | /service/open/v2/flows/{flowId}/versions/{versionId}/invalidate | 失效流版本 | open-server |
| PUT | /service/open/v2/flows/{flowId}/versions/{versionId}/recover | 恢复流版本 | open-server |
| DELETE | /service/open/v2/flows/{flowId}/versions/{versionId} | 删除流版本 | open-server |
| POST | /service/open/v2/flows/{flowId}/versions/{versionId}/cancel | 撤回审批 | open-server |
| POST | /service/open/v2/flows/{flowId}/versions/{versionId}/urge | 审批催办 | open-server |
| GET | /service/open/v2/executions | 执行记录列表 | open-server |
| GET | /service/open/v2/executions/{executionId} | 执行记录详情 | open-server |

---

## 6. 数据开放 / 用户授权（api-server）

| 方法 | 路径 | 说明 | 服务 |
|:----:|------|------|:----:|
| GET | /api/v1/user-authorizations | 用户授权列表 | api-server |
| POST | /api/v1/user-authorizations | 授予授权 | api-server |
| DELETE | /api/v1/user-authorizations/{id} | 撤销授权 | api-server |

---

## 7. 订阅数据同步（open-server）

| 方法 | 路径 | 说明 | 服务 |
|:----:|------|------|:----:|
| POST | /service/open/v2/sync/subscription/migrate | 订阅数据迁移 | open-server |
| POST | /service/open/v2/sync/subscription/rollback | 迁移回滚 | open-server |
| POST | /service/open/v2/sync/subscription/emergency/update-old | 紧急修复旧数据 | open-server |
| POST | /service/open/v2/sync/subscription/emergency/update-new | 紧急修复新数据 | open-server |

---

## 8. 健康检查（各服务）

| 方法 | 路径 | 说明 | 服务 |
|:----:|------|------|:----:|
| GET | /service/open/v2/health | 健康检查 | open-server / market-server |
| GET | /service/open/v2/user-info | 当前用户信息 | open-server / market-server |
| GET | /api/v1/health | 健康检查 | api-server / event-server |

---

## 9. 通用约定

| 项 | 说明 |
|----|------|
| **响应包裹** | `ApiResponse<T>`（data/error 字段） |
| **状态码** | 200 成功 / 400 参数错误 / 401 未认证 / 403 无权限 / 404 资源不存在 / 409 状态冲突 / 429 限流 / 500 服务端错误 |
| **认证** | 管理面：内部凭证 + 成员权限（Owner/管理员/开发者）；消费面：SOA/APIG/AKSK 认证头 |
| **状态流转** | approve/reject/publish/withdraw/deploy/start/stop 以 POST/PUT 触发，通过 status 字段状态机推进 |
| **Swagger** | 各服务 /swagger-ui.html（open-server:18080, api-server:18081, event-server:18082, market-server:18083, connector-api:18180） |

---

## 修订记录

| 版本 | 变更说明 | 日期 | 修订人 |
|------|---------|------|--------|
| v1.0 | 全量接口清单（业务能力组织，184 端点） | 2026-08-03 | SDDU Docs Agent |
