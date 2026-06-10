# API 接口设计：连接器平台 V2

**Feature ID**: CONN-PLAT-002
**关联文档**: plan.md（§4.1 管理面 + §4.2 运行时），plan-db.md（§3 表结构），plan-json-schema.md（JSON 结构定义）
**版本**: v2.0
**创建日期**: 2026-06-09
**对齐基线**: spec.md v2.15-draft + plan.md + plan-db.md

---

## 0. 版本对齐说明

| 维度 | 说明 | 决策来源 |
|------|------|---------|
| **版本模型** | **多版本**（草稿→发布→失效→删除），最多 1000 个版本 | spec v2.15 |
| **连接流版本审批** | 三级审批（应用级→平台连接流级→全局级）+ 催办 | spec §3.6 |
| **JSON 字段结构** | 对齐 [plan-json-schema.md](./plan-json-schema.md)：React Flow 格式 / 认证多选 / inputMapping-outputMapping 分段 / JSON Path 表达式 / FR-047 类型严格约束 | plan-json-schema.md v6.0 |
| **服务归属** | open-server（管理面 40 个） + connector-api（运行时 5 个） | plan.md §1 |
| 端点总数 | **43** | — |

---

## 1. 设计规范

> 💡 以下规范沿用 V1 `plan-api.md §1` 已确立的标准，V2 增量变更在子节内标注。

### 1.1 基础规范

| 规范项 | 说明 |
|--------|------|
| 基础路径 | `/service/open/v2` (open-server 管理面) / `/api/v1` (connector-api 执行面) |
| 认证方式 | 管理面复用现有 Cookie/SSO；执行面 HTTP 触发通过 SYSTOKEN 签名验证 |
| 应用隔离 | open-server 管理面接口（#1~#41）统一通过 `Header: X-App-Id` 传递，三层校验：白名单准入 → 用户权限 → 数据归属；connector-api 运行时（#42~#43）从 flow 自动获取 |
| 时间格式 | ISO 8601: `yyyy-MM-dd'T'HH:mm:ss.SSSXXX` |

### 1.2 字段命名规范

**规则**：接口入参和返回值字段统一使用驼峰命名（camelCase）。

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| `connectorId` | `connector_id` |
| `versionStatus` | `version_status` |
| `nameCn` / `nameEn` | `name_cn` / `name_en` |
| `deployedVersionId` | `deployed_version_id` |

**命名约定**：
- ID 字段：使用 `Id` 后缀，如 `connectorId`, `flowId`, `versionId`, `executionId`
- 时间字段：使用 `Time` 后缀，如 `createTime`, `publishedTime`
- 布尔字段：使用 `is` 前缀，如 `isDeleted`
- **双语字段**：使用 `Cn`/`En` 后缀，如 `nameCn`/`nameEn`, `descriptionCn`/`descriptionEn`, `labelCn`/`labelEn`

**数据库 snake_case → API camelCase 映射**：

| 数据库列名 | API 字段名 |
|-----------|----------|
| `name_cn` | `nameCn` |
| `description_en` | `descriptionEn` |
| `deployed_version_id` | `deployedVersionId` |
| `deployed_version_number` | `deployedVersionNumber` |
| `connector_version_id` | `connectorVersionId` |
| `lifecycle_status` | `lifecycleStatus` |
| `version_number` | `versionNumber` |
| `published_time` | `publishedTime` |

### 1.3 路径命名规范

**规则**：URL 路径使用中划线分隔多个单词（kebab-case）。

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| `/connector-versions` | `/connector_versions` |
| `/copy-to-draft` | `/copyToDraft` |
| `/submit-approval` | `/submitApproval` |
| `/app-whitelist` | `/appWhitelist` |

**命名约定**：
- 资源名称使用复数形式：`/connectors`, `/flows`, `/executions`
- 子资源使用中划线分隔：`/copy-to-draft`, `/url-whitelist`
- 路径参数使用驼峰：`/connectors/{connectorId}/versions`

### 1.4 数据类型规范

**规则**：

1. **长整数（BIGINT 雪花 ID）统一返回 string 类型**，避免前端精度丢失
2. **枚举字段统一返回 TINYINT 数字**，与数据库存储一致
3. **时间字段**返回 ISO 8601 字符串（含毫秒 + 时区）

| ✅ 正确示例 | ❌ 错误示例 | 说明 |
|------------|------------|------|
| `"connectorId": "1234567890123456789"` | `"connectorId": 1234567890123456789` | BIGINT 必须转 string |
| `"status": 2` | `"status": "published"` | 枚举用数字 |
| `"createTime": "2026-06-09T10:00:00.000+08:00"` | `"createTime": 1716264000000` | 时间用 ISO 8601 |

**适用范围**（ID 字段必须返回 string）：
- 所有主键 ID：`id`, `connectorId`, `flowId`, `versionId`, `executionId`
- 所有外键 ID：`connectorVersionId`, `flowVersionId`, `deployedVersionId`

### 1.5 响应格式规范

所有接口统一使用以下响应格式：

```json
// 成功响应
{
  "code": "200",
  "messageZh": "操作成功",
  "messageEn": "Success",
  "data": { ... },
  "page": null
}

// 分页响应
{
  "code": "200",
  "messageZh": "查询成功",
  "messageEn": "Success",
  "data": [ ... ],
  "page": {
    "curPage": 1,
    "pageSize": 20,
    "total": 123
  }
}

// 错误响应
{
  "code": "400",
  "messageZh": "参数错误",
  "messageEn": "Bad Request",
  "data": null,
  "page": null
}
```

### 1.6 分页请求规范

所有列表接口统一支持分页：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| curPage | int | 否 | 当前页码，从 1 开始，默认 1 |
| pageSize | int | 否 | 每页数量，默认 20，最大 100 |

### 1.7 错误码定义

| 错误码 | 说明 |
|--------|------|
| `200` | 成功 |
| `400` | 参数错误 / 校验失败 |
| `401` | 未授权（SYSTOKEN 无效或不在白名单） |
| `403` | 无权限 / 操作被拒绝（如非白名单应用访问） |
| `404` | 资源不存在 |
| `409` | 状态冲突（如无法在当前状态下执行操作） |
| `422` | 业务校验失败（如草稿为空禁止发布、版本被引用禁止失效） |
| `423` | 资源锁定（如审批中版本禁止编辑） |
| `429` | 触发频率超限（入站限流） |
| `500` | 内部错误 |
| `503` | 连接流未部署 |

### 1.8 状态枚举字典

> 💡 对外 API 返回的枚举值统一为 TINYINT 数字，前端维护数字 → 标签映射字典。

#### 1.8.1 连接器状态 (connector.status)

| 数字 | 含义 |
|:--:|------|
| 1 | 有效不可用（无已发布版本） |
| 2 | 有效可用（有已发布版本） |
| 3 | 已失效 |
| 4 | 物理删除 |

#### 1.8.2 连接器版本状态 (connectorVersion.status)

| 数字 | 含义 |
|:--:|------|
| 1 | 草稿 |
| 2 | 已发布 |
| 3 | 已失效 |
| 4 | 物理删除 |

#### 1.8.3 连接流生命周期 (flow.lifecycleStatus)

| 数字 | 含义 |
|:--:|------|
| 1 | 待部署 |
| 2 | 运行中 |
| 3 | 已停止 |
| 4 | 已失效 |
| 5 | 物理删除 |

#### 1.8.4 连接流版本状态 (flowVersion.status)

| 数字 | 含义 |
|:--:|------|
| 1 | 草稿 |
| 2 | 待审批 |
| 3 | 已撤回 |
| 4 | 已驳回 |
| 5 | 已发布 |
| 6 | 已失效 |
| 7 | 物理删除 |

#### 1.8.5 执行记录状态 (executionRecord.status)

| 数字 | 含义 |
|:--:|------|
| 0 | success |
| 1 | failed |
| 2 | timeout |

> 💡 同步执行模型，仅终态持久化。

#### 1.8.6 触发方式 (executionRecord.triggerType)

| 数字 | 含义 |
|:--:|------|
| 1 | http（HTTP 触发） |
| 2 | debug（调试触发） |

#### 1.8.7 节点类型 (executionStep.nodeType)

| 数字 | 含义 |
|:--:|------|
| 1 | trigger（触发器节点） |
| 2 | connector（连接器节点） |
| 3 | data_processor（数据处理节点） |
| 4 | exit（出口节点） |

#### 1.8.7b 执行步骤状态 (executionStep.status)

| 数字 | 含义 |
|:--:|------|
| 0 | success（步骤执行成功） |
| 1 | failed（步骤执行失败） |
| 2 | timeout（步骤执行超时） |
| 3 | not_executed（未执行，如分支未走到） |

#### 1.8.8 审批节点状态 (approvalNode.status)

| 数字 | 含义 |
|:--:|------|
| 0 | pending（待审批） |
| 1 | approved（已通过） |
| 2 | rejected（已驳回） |
| 3 | cancelled（已撤回） |

#### 1.8.9 连接器协议类型 (connectorType)

| 数字 | 含义 |
|:--:|------|
| 1 | HTTP |

---

## 2. 接口清单

> ⚠️ **应用隔离**：以下 open-server 管理面接口（#1~#41）统一通过 `Header: X-App-Id` 传递应用 ID，三层校验：
> ① 白名单准入（`AppWhitelistInterceptor`）：校验应用是否在连接器平台白名单内；
> ② 用户权限（`UserAppPermissionInterceptor`）：校验当前用户是否有该应用的操作权限；
> ③ 数据归属（Service 层）：校验操作的资源是否归属该应用。
> connector-api 运行时接口（#42~#43）从 `flow_t.app_id` 自动获取，无需传入。

**改动点编号说明**：

| 编号 | 含义 |
|:--:|------|
| ① | 三层权限校验（`Header: X-App-Id`） |
| ② | 行为变更（参数/返回值/业务逻辑变化） |
| ③ | 路径变更 |
| ④ | 新增接口 |
| ⑤ | 接口删除 |
| ⑥ | 替换旧接口（注明被替换的 V1 接口） |

> 💡 编号可组合，如 `①②` 表示同时涉及权限校验 + 行为变更。

| # | 服务 | 模块 | Method | Path | 变更 | 说明 | 改动点 | FR |
|:--:|------|------|--------|------|:--:|------|:--:|:--:|
| 1 | open-server | **连接器 CRUD** | POST | `/service/open/v2/connectors` | 修改 | 创建连接器 | ①② | FR-001 |
| 2 | | | GET | `/service/open/v2/connectors` | 修改 | 查询连接器列表 | ① | — |
| 3 | | | GET | `/service/open/v2/connectors/{connectorId}` | 沿用 | 查询连接器详情 | ① | — |
| 4 | | | PUT | `/service/open/v2/connectors/{connectorId}` | 沿用 | 更新连接器基本信息 | ① | — |
| 5 | | | PUT | `/service/open/v2/connectors/{connectorId}/invalidate` | 新增 | 标记连接器失效 | ①④ | FR-003 |
| 6 | | | PUT | `/service/open/v2/connectors/{connectorId}/restore` | 新增 | 恢复连接器 | ①④ | FR-002 |
| 7 | | | DELETE | `/service/open/v2/connectors/{connectorId}` | 修改 | 删除连接器 | ①② | FR-004 |
| 8 | | **连接器版本** | GET | `/service/open/v2/connectors/{connectorId}/versions` | 新增 | 版本列表 | ①④ | FR-008 |
| 9 | | | GET | `/service/open/v2/connectors/{connectorId}/versions/{versionId}` | 新增 | 版本详情，替换 V1 `GET /config` | ①④⑥ | FR-008 |
| 10 | | | PUT | `/service/open/v2/connectors/{connectorId}/versions/{versionId}` | 新增 | 编辑草稿，替换 V1 `PUT /config` | ①④⑥ | FR-005 |
| 11 | | | PUT | `/service/open/v2/connectors/{connectorId}/versions/{versionId}/publish` | 新增 | 发布版本 | ①④ | FR-007 |
| 12 | | | POST | `/service/open/v2/connectors/{connectorId}/versions/{versionId}/copy-to-draft` | 新增 | 复制已发布版本到草稿 | ①④ | FR-006 |
| 13 | | | PUT | `/service/open/v2/connectors/{connectorId}/versions/{versionId}/invalidate` | 新增 | 标记版本失效 | ①④ | FR-009 |
| 14 | | | PUT | `/service/open/v2/connectors/{connectorId}/versions/{versionId}/restore` | 新增 | 恢复版本 | ①④ | FR-011 |
| 15 | | | DELETE | `/service/open/v2/connectors/{connectorId}/versions/{versionId}` | 新增 | 删除版本 | ①④ | FR-010 |
| — | | **已删除** | GET | `/service/open/v2/connectors/{connectorId}/config` | 删除 | V1 获取连接器配置 → V2 由 #9 替代 | ⑤ | — |
| — | | | PUT | `/service/open/v2/connectors/{connectorId}/config` | 删除 | V1 编辑连接器配置 → V2 由 #10 替代 | ⑤ | — |
| 16 | | **连接流 CRUD** | POST | `/service/open/v2/flows` | 修改 | 创建连接流 | ①② | FR-016 |
| 17 | | | GET | `/service/open/v2/flows` | 修改 | 查询连接流列表 | ①② | — |
| 18 | | | GET | `/service/open/v2/flows/{flowId}` | 沿用 | 查询连接流详情 | ① | — |
| 19 | | | PUT | `/service/open/v2/flows/{flowId}` | 沿用 | 更新连接流基本信息 | ① | — |
| 20 | | | POST | `/service/open/v2/flows/{flowId}/copy` | 新增 | 一键复制连接流 | ①④ | FR-017 |
| 21 | | | POST | `/service/open/v2/flows/{flowId}/deploy` | 新增 | 部署+启动（选择已发布版本） | ①④ | FR-018 |
| 22 | | | POST | `/service/open/v2/flows/{flowId}/start` | 修改 | 启动连接流（V2 状态模型变更） | ①② | FR-019 |
| 23 | | | POST | `/service/open/v2/flows/{flowId}/stop` | 沿用 | 停止连接流 | ① | FR-020 |
| 24 | | | PUT | `/service/open/v2/flows/{flowId}/invalidate` | 新增 | 标记连接流失效 | ①④ | FR-022 |
| 25 | | | PUT | `/service/open/v2/flows/{flowId}/restore` | 新增 | 恢复连接流 | ①④ | FR-021 |
| 26 | | | DELETE | `/service/open/v2/flows/{flowId}` | 修改 | 删除连接流 | ①② | FR-023 |
| 27 | | **连接流版本** | GET | `/service/open/v2/flows/{flowId}/versions` | 新增 | 版本列表 | ①④ | FR-027 |
| 28 | | | GET | `/service/open/v2/flows/{flowId}/versions/{versionId}` | 新增 | 版本详情，替换 V1 `GET /config` | ①④⑥ | FR-027 |
| 29 | | | PUT | `/service/open/v2/flows/{flowId}/versions/{versionId}` | 新增 | 编辑草稿，替换 V1 `PUT /config` | ①④⑥ | FR-024 |
| 30 | | | POST | `/service/open/v2/flows/{flowId}/versions/{versionId}/submit-approval` | 新增 | 提交审批 | ①④ | FR-026 |
| 31 | | | POST | `/service/open/v2/flows/{flowId}/versions/{versionId}/copy-to-draft` | 新增 | 复制已发布版本到草稿 | ①④ | FR-025 |
| 32 | | | PUT | `/service/open/v2/flows/{flowId}/versions/{versionId}/invalidate` | 新增 | 标记版本失效 | ①④ | FR-028 |
| 33 | | | PUT | `/service/open/v2/flows/{flowId}/versions/{versionId}/restore` | 新增 | 恢复版本 | ①④ | FR-030 |
| 34 | | | DELETE | `/service/open/v2/flows/{flowId}/versions/{versionId}` | 新增 | 删除版本 | ①④ | FR-029 |
| — | | **已删除** | GET | `/service/open/v2/flows/{flowId}/config` | 删除 | V1 获取编排配置 → V2 由 #28 替代 | ⑤ | — |
| — | | | PUT | `/service/open/v2/flows/{flowId}/config` | 删除 | V1 保存编排配置 → V2 由 #29 替代 | ⑤ | — |
| 35 | | **运行记录** | GET | `/service/open/v2/flows/{flowId}/executions` | 新增 | 运行记录列表（分页+过滤） | ①④ | FR-042 |
| 36 | | | GET | `/service/open/v2/flows/{flowId}/executions/{executionId}` | 新增 | 运行记录详情 | ①④ | FR-042 |
| 37 | | **审批管理** | POST | `/service/open/v2/connector-platform/approvals/{versionId}/urge` | 新增 | 一键催办 | ①④ | FR-033 |
| 38 | | | GET | `/service/open/v2/connector-platform/approvals/{versionId}/status` | 新增 | 查询审批状态 | ①④ | FR-031 |
| 39 | | | GET | `/service/open/v2/approval-flows` | 修改 | 查询审批人配置 | ①② | FR-032 |
| 40 | | | PUT | `/service/open/v2/approval-flows` | 修改 | 更新审批人配置 | ①② | FR-032 |
| 41 | open-server | **调试代理** | POST | `/service/open/v2/flows/{flowId}/versions/{versionId}/debug` | 新增 | 调试指定版本（替换 V1 test-run） | ①④⑥ | FR-041 |
| 42 | connector-api | **运行时** | POST | `/api/v1/flows/{flowId}/invoke` | 修改 | 调用已部署的连接流 | ②③⑥ | G11 |
| 43 | | | POST | `/api/v1/flows/{flowId}/versions/{versionId}/debug` | 新增 | 调试指定版本 | ④ | FR-041 |

> 💡 **应用白名单**（FR-045）：数据存储在 `openplatform_lookup_*` LookUp 体系，复用 market-web 现有管理界面，运行时读取，不新增接口。
> 💡 审批提交/通过/驳回/撤回复用现有 `ApprovalController` 接口（`/service/open/v2/approvals/*`），不新增专用端点。#30 提交审批时系统调用 `ApprovalEngine.createApproval()` 创建审批实例。

**端点统计**：新增 32 + 修改 8 + 沿用 5 + 删除 4 = 43 个有效端点。

**服务部署归属**：

| 服务 | 端口 | 接口数 | 接口范围 |
|------|------|:---:|---------|
| open-server（管理类） | 18080 | 41 | #1~#41 |
| connector-api（运行时） | 18180 | 2 | #42~#43 |

---

## 3. 接口详细定义

> 💡 接口清单见 §2，本章为每个接口的请求/响应详细定义。所有接口的字段命名、数据类型、响应格式、状态枚举均遵循 §1 设计规范。

### 3.1 连接器 CRUD（#1~#7）

#### #1 POST /service/open/v2/connectors — 创建连接器

```json
// Request
{
  "nameCn": "IM 发送消息",
  "nameEn": "IM Send Message",
  "descriptionCn": "封装 IM 消息发送能力",
  "descriptionEn": "Encapsulated IM messaging capability",
  "connectorType": 1,
  "appId": "1234567890123456789"
}

// Response 200
{
  "code": "200",
  "messageZh": "创建成功",
  "messageEn": "Created",
  "data": {
    "connectorId": "9876543210987654321",
    "nameCn": "IM 发送消息",
    "nameEn": "IM Send Message",
    "connectorType": 1,
    "status": 1,
    "appId": "1234567890123456789",
    "draftVersion": {
      "versionId": "1111111111111111111",
      "versionNumber": 1,
      "status": 1
    },
    "createTime": "2026-06-09T10:00:00.000+08:00"
  },
  "page": null
}
```

#### #2 GET /service/open/v2/connectors — 查询连接器列表

```json
// Query: ?curPage=1&pageSize=20&connectorType=1&keyword=IM&appId=1234567890123456789

// Response 200
{
  "code": "200",
  "messageZh": "查询成功",
  "messageEn": "Success",
  "data": [
    {
      "connectorId": "9876543210987654321",
      "nameCn": "IM 发送消息",
      "nameEn": "IM Send Message",
      "connectorType": 1,
      "status": 2,
      "latestPublishedVersionNumber": 2,
      "draftVersionNumber": 3,
      "appId": "1234567890123456789",
      "createTime": "2026-06-09T10:00:00.000+08:00"
    }
  ],
  "page": { "curPage": 1, "pageSize": 20, "total": 1 }
}
```

#### #3 GET /service/open/v2/connectors/{id} — 查询连接器详情

```json
// Response 200
{
  "code": "200",
  "messageZh": "操作成功",
  "messageEn": "Success",
  "data": {
    "connectorId": "9876543210987654321",
    "nameCn": "IM 发送消息",
    "nameEn": "IM Send Message",
    "descriptionCn": "封装 IM 消息发送能力",
    "descriptionEn": "Encapsulated IM messaging capability",
    "connectorType": 1,
    "status": 2,
    "appId": "1234567890123456789",
    "createTime": "2026-06-09T10:00:00.000+08:00",
    "lastUpdateTime": "2026-06-09T11:00:00.000+08:00"
  },
  "page": null
}
```

#### #4 PUT /service/open/v2/connectors/{id} — 更新连接器基本信息

```json
// Request
{
  "nameCn": "IM 发送消息（新版）",
  "nameEn": "IM Send Message (New)",
  "descriptionCn": "更新后的 IM 消息发送能力",
  "descriptionEn": "Updated IM messaging capability"
}

// Response 200
{
  "code": "200",
  "messageZh": "保存成功",
  "messageEn": "Saved",
  "data": {
    "connectorId": "9876543210987654321",
    "lastUpdateTime": "2026-06-09T12:00:00.000+08:00"
  },
  "page": null
}
```

#### #5 PUT /service/open/v2/connectors/{id}/invalidate — 标记连接器失效

```json
// Request: 无 body
// Response 200
{
  "code": "200",
  "messageZh": "操作成功",
  "messageEn": "Success",
  "data": {
    "connectorId": "9876543210987654321",
    "status": 3,
    "lastUpdateTime": "2026-06-09T13:00:00.000+08:00"
  },
  "page": null
}
// 错误 — 有连接流引用
{
  "code": "422",
  "messageZh": "以下连接流引用了此连接器：新消息自动通知、订单同步流程",
  "messageEn": "Connector is referenced by flows",
  "data": { "referencedFlowNames": ["新消息自动通知", "订单同步流程"] },
  "page": null
}
```

#### #6 PUT /service/open/v2/connectors/{id}/restore — 恢复连接器

```json
// Request: 无 body
// Response 200
{
  "code": "200",
  "messageZh": "恢复成功",
  "messageEn": "Restored",
  "data": {
    "connectorId": "9876543210987654321",
    "status": 1,
    "note": "无已发布版本，连接器处于有效不可用状态，请先发布版本"
  },
  "page": null
}
```

#### #7 DELETE /service/open/v2/connectors/{id} — 删除连接器（物理删除）

```json
// Request: 无 body
// Response 200
{
  "code": "200",
  "messageZh": "删除成功",
  "messageEn": "Deleted",
  "data": null,
  "page": null
}
// 错误 — 非已失效状态
{
  "code": "409",
  "messageZh": "仅已失效状态的连接器可删除",
  "messageEn": "Only invalidated connectors can be deleted",
  "data": null,
  "page": null
}
```

---

### 3.2 连接器版本（#8~#15）

#### #8 GET /service/open/v2/connectors/{id}/versions — 版本列表

```json
// Response 200
{
  "code": "200",
  "messageZh": "查询成功",
  "messageEn": "Success",
  "data": [
    {
      "versionId": "2222222222222222222",
      "versionNumber": 3,
      "status": 1,
      "createTime": "2026-06-09T10:00:00.000+08:00",
      "createBy": "zhangsan"
    },
    {
      "versionId": "1111111111111111111",
      "versionNumber": 2,
      "status": 2,
      "publishedTime": "2026-06-08T09:00:00.000+08:00",
      "publishedBy": "lisi",
      "createTime": "2026-06-08T08:00:00.000+08:00"
    },
    {
      "versionId": "0000000000000000000",
      "versionNumber": 1,
      "status": 3,
      "publishedTime": "2026-06-07T08:00:00.000+08:00",
      "createTime": "2026-06-07T07:00:00.000+08:00"
    }
  ],
  "page": null
}
```

#### #9 GET /service/open/v2/connectors/{id}/versions/{vid} — 版本详情

```json
// Response 200
{
  "code": "200",
  "messageZh": "操作成功",
  "messageEn": "Success",
  "data": {
    "versionId": "1111111111111111111",
    "connectorId": "9876543210987654321",
    "versionNumber": 2,
    "status": 2,
    "connectionConfig": {
      "protocol": "HTTP",
      "protocolConfig": {
        "url": "https://openapi.xxx.com/im/send",
        "method": "POST",
        "headers": { "Content-Type": "application/json" }
      },
      "authConfig": {
        "types": ["SOA", "DIGITAL_SIGN"],
        "fields": [
          { "name": "accessKey", "carrier": "header", "fieldName": "AK", "required": true, "sensitive": true, "sort": 1 },
          { "name": "signature", "carrier": "header", "fieldName": "X-Signature", "required": true, "sensitive": true, "sort": 2 }
        ]
      },
      "inputContract": {
        "protocol": "HTTP",
        "body": {
          "type": "object",
          "properties": {
            "receiver": { "type": "string", "description": "接收者ID" },
            "content": { "type": "string", "description": "消息内容" }
          },
          "required": ["receiver", "content"]
        }
      },
      "outputContract": {
        "protocol": "HTTP",
        "body": {
          "type": "object",
          "properties": {
            "msgId": { "type": "string" }
          }
        }
      }
    },
    "publishedTime": "2026-06-08T09:00:00.000+08:00",
    "publishedBy": "lisi",
    "createTime": "2026-06-08T08:00:00.000+08:00"
  },
  "page": null
}
```

#### #10 PUT /service/open/v2/connectors/{id}/versions/{vid} — 编辑草稿保存

```json
// Request — connectionConfig 全文替换
{
  "connectionConfig": {
    "protocol": "HTTP",
    "protocolConfig": {
      "url": "https://openapi.xxx.com/im/send/v2",
      "method": "POST",
      "headers": { "Content-Type": "application/json" }
    },
    "authConfig": {
      "types": ["SOA", "DIGITAL_SIGN"],
      "fields": [
        { "name": "accessKey", "carrier": "header", "fieldName": "AK", "required": true, "sensitive": true, "sort": 1 },
        { "name": "signature", "carrier": "header", "fieldName": "X-Signature", "required": true, "sensitive": true, "sort": 2 }
      ]
    },
    "inputContract": { "protocol": "HTTP", "body": { "type": "object", "properties": { "receiver": { "type": "string" }, "content": { "type": "string" } }, "required": ["receiver", "content"] } },
    "outputContract": { "protocol": "HTTP", "body": { "type": "object", "properties": { "msgId": { "type": "string" } } } }
  }
}

// Response 200
{
  "code": "200",
  "messageZh": "保存成功",
  "messageEn": "Saved",
  "data": {
    "versionId": "2222222222222222222",
    "versionNumber": 3,
    "status": 1,
    "lastUpdateTime": "2026-06-09T10:00:00.000+08:00"
  },
  "page": null
}
// 错误 — 非草稿状态
{
  "code": "409",
  "messageZh": "仅草稿状态可编辑",
  "messageEn": "Only draft versions can be edited",
  "data": null,
  "page": null
}
```

#### #11 PUT /service/open/v2/connectors/{id}/versions/{vid}/publish — 发布版本

```json
// Request: 无 body
// Response 200
{
  "code": "200",
  "messageZh": "发布成功",
  "messageEn": "Published",
  "data": {
    "versionId": "2222222222222222222",
    "versionNumber": 3,
    "status": 2,
    "connectorStatus": 2,
    "publishedTime": "2026-06-09T10:30:00.000+08:00"
  },
  "page": null
}
// 错误 — 草稿为空
{
  "code": "422",
  "messageZh": "请先完善连接配置",
  "messageEn": "Please complete the connection configuration first",
  "data": null,
  "page": null
}
```

#### #12 POST /service/open/v2/connectors/{id}/versions/{vid}/copy-to-draft — 复制到草稿

```json
// Request: 无 body
// Response 200
{
  "code": "200",
  "messageZh": "复制成功",
  "messageEn": "Copied to draft",
  "data": {
    "versionId": "3333333333333333333",
    "versionNumber": 4,
    "status": 1,
    "sourceVersionNumber": 2,
    "message": "已覆盖当前草稿内容"
  },
  "page": null
}
// 错误 — 版本数达上限
{
  "code": "422",
  "messageZh": "版本数量已达上限（1000），请清理失效版本后再试",
  "messageEn": "Version limit reached (1000)",
  "data": null,
  "page": null
}
```

#### #13 PUT /service/open/v2/connectors/{id}/versions/{vid}/invalidate — 标记版本失效

```json
// Request: 无 body
// Response 200
{
  "code": "200",
  "messageZh": "操作成功",
  "messageEn": "Success",
  "data": {
    "versionId": "1111111111111111111",
    "versionNumber": 2,
    "status": 3,
    "connectorStatus": 1,
    "lastUpdateTime": "2026-06-09T14:00:00.000+08:00"
  },
  "page": null
}
// 错误 — 有连接流引用
{
  "code": "422",
  "messageZh": "以下连接流引用了此版本：新消息自动通知",
  "messageEn": "Version is referenced by flows",
  "data": { "referencedFlowNames": ["新消息自动通知"] },
  "page": null
}
```

#### #14 PUT /service/open/v2/connectors/{id}/versions/{vid}/restore — 恢复版本

```json
// Request: 无 body
// Response 200
{
  "code": "200",
  "messageZh": "恢复成功",
  "messageEn": "Restored",
  "data": {
    "versionId": "1111111111111111111",
    "versionNumber": 2,
    "status": 2,
    "connectorStatus": 2,
    "lastUpdateTime": "2026-06-09T14:30:00.000+08:00"
  },
  "page": null
}
```

#### #15 DELETE /service/open/v2/connectors/{id}/versions/{vid} — 删除版本

```json
// Request: 无 body
// Response 200
{
  "code": "200",
  "messageZh": "删除成功",
  "messageEn": "Deleted",
  "data": null,
  "page": null
}
// 错误 — 非已失效
{
  "code": "409",
  "messageZh": "仅已失效状态的版本可删除",
  "messageEn": "Only invalidated versions can be deleted",
  "data": null,
  "page": null
}
```

---

### 3.3 URL 白名单（#16~#18）

#### #16 GET /service/open/v2/connectors/{id}/url-whitelist — 查询规则列表

```json
// Response 200
{
  "code": "200",
  "messageZh": "查询成功",
  "messageEn": "Success",
  "data": [
    {
      "ruleId": "1234567890123456789",
      "pattern": "^https://api\\.example\\.com/v1/.*",
      "description": "内部 API 网关",
      "createTime": "2026-06-09T10:00:00.000+08:00"
    }
  ],
  "page": null
}
```

#### #17 POST /service/open/v2/connectors/{id}/url-whitelist — 新增规则

```json
// Request
{
  "pattern": "^https://internal\\.xxx\\.com/.*",
  "description": "内部服务"
}

// Response 200
{
  "code": "200",
  "messageZh": "添加成功",
  "messageEn": "Added",
  "data": {
    "ruleId": "9876543210987654321",
    "pattern": "^https://internal\\.xxx\\.com/.*"
  },
  "page": null
}
// 错误 — 正则语法错误
{
  "code": "400",
  "messageZh": "正则表达式语法错误",
  "messageEn": "Invalid regex pattern",
  "data": null,
  "page": null
}
```

#### #18 DELETE /service/open/v2/connectors/{id}/url-whitelist/{rid} — 删除规则

```json
// Response 200
{
  "code": "200",
  "messageZh": "删除成功",
  "messageEn": "Deleted",
  "data": null,
  "page": null
}
```

---

### 3.4 连接流 CRUD（#19~#29）

#### #19 POST /service/open/v2/flows — 创建连接流

```json
// Request
{
  "nameCn": "新消息自动通知",
  "nameEn": "Auto Message Notification",
  "descriptionCn": "收到 IM 消息后自动发送通知到OA系统",
  "descriptionEn": "Auto notify OA system upon receiving IM messages",
  "appId": "1234567890123456789"
}

// Response 200
{
  "code": "200",
  "messageZh": "创建成功",
  "messageEn": "Created",
  "data": {
    "flowId": "4444444444444444444",
    "nameCn": "新消息自动通知",
    "nameEn": "Auto Message Notification",
    "lifecycleStatus": 1,
    "appId": "1234567890123456789",
    "draftVersion": {
      "versionId": "5555555555555555555",
      "versionNumber": 1,
      "status": 1
    },
    "createTime": "2026-06-09T10:00:00.000+08:00"
  },
  "page": null
}
```

#### #20 GET /service/open/v2/flows — 查询连接流列表

```json
// Query: ?curPage=1&pageSize=20&lifecycleStatus=2&keyword=通知&appId=1234567890123456789

// Response 200
{
  "code": "200",
  "messageZh": "查询成功",
  "messageEn": "Success",
  "data": [
    {
      "flowId": "4444444444444444444",
      "nameCn": "新消息自动通知",
      "nameEn": "Auto Message Notification",
      "lifecycleStatus": 2,
      "deployedVersionId": "6666666666666666666",
      "deployedVersionNumber": 2,
      "latestPublishedVersionNumber": 3,
      "draftVersionNumber": 4,
      "appId": "1234567890123456789",
      "createTime": "2026-06-09T10:00:00.000+08:00"
    }
  ],
  "page": { "curPage": 1, "pageSize": 20, "total": 1 }
}
```

#### #21 GET /service/open/v2/flows/{id} — 查询连接流详情

```json
// Response 200
{
  "code": "200",
  "messageZh": "操作成功",
  "messageEn": "Success",
  "data": {
    "flowId": "4444444444444444444",
    "nameCn": "新消息自动通知",
    "nameEn": "Auto Message Notification",
    "descriptionCn": "收到 IM 消息后自动发送通知到OA系统",
    "descriptionEn": "Auto notify OA system upon receiving IM messages",
    "lifecycleStatus": 2,
    "deployedVersionId": "6666666666666666666",
    "deployedVersionNumber": 2,
    "appId": "1234567890123456789",
    "invokeUrl": "https://xxx/api/v1/flows/4444444444444444444/invoke",
    "createTime": "2026-06-09T10:00:00.000+08:00",
    "lastUpdateTime": "2026-06-09T11:00:00.000+08:00"
  },
  "page": null
}
```

#### #22 PUT /service/open/v2/flows/{id} — 更新连接流基本信息

```json
// Request
{
  "nameCn": "新消息自动通知（新版）",
  "nameEn": "Auto Message Notification (New)",
  "descriptionCn": "更新后的通知流描述"
}

// Response 200
{
  "code": "200",
  "messageZh": "保存成功",
  "messageEn": "Saved",
  "data": {
    "flowId": "4444444444444444444",
    "lastUpdateTime": "2026-06-09T12:00:00.000+08:00"
  },
  "page": null
}
```

#### #23 POST /service/open/v2/flows/{id}/copy — 一键复制连接流

```json
// Request: 无 body
// Response 200
{
  "code": "200",
  "messageZh": "复制成功",
  "messageEn": "Copied",
  "data": {
    "flowId": "7777777777777777777",
    "nameCn": "新消息自动通知_copy_a3f2b",
    "nameEn": "Auto Message Notification_copy_a3f2b",
    "lifecycleStatus": 1,
    "versionsCopied": 5,
    "createTime": "2026-06-09T12:30:00.000+08:00"
  },
  "page": null
}
// 错误 — 名称碰撞
{
  "code": "409",
  "messageZh": "复制名称冲突，请稍后重试",
  "messageEn": "Copy name conflict, please retry later",
  "data": null,
  "page": null
}
```

#### #24 POST /service/open/v2/flows/{id}/deploy — 部署+启动

```json
// Request
{
  "versionId": "6666666666666666666"
}

// Response 200
{
  "code": "200",
  "messageZh": "部署成功",
  "messageEn": "Deployed",
  "data": {
    "flowId": "4444444444444444444",
    "deployedVersionId": "6666666666666666666",
    "deployedVersionNumber": 2,
    "lifecycleStatus": 2,
    "invokeUrl": "https://xxx/api/v1/flows/4444444444444444444/invoke"
  },
  "page": null
}
// 错误 — 版本未发布
{
  "code": "422",
  "messageZh": "版本非已发布状态，不可部署",
  "messageEn": "Version is not published, cannot deploy",
  "data": null,
  "page": null
}
```

#### #25 POST /service/open/v2/flows/{id}/start — 启动连接流

```json
// Request: 无 body
// Response 200
{
  "code": "200",
  "messageZh": "启动成功",
  "messageEn": "Started",
  "data": {
    "flowId": "4444444444444444444",
    "lifecycleStatus": 2,
    "lastUpdateTime": "2026-06-09T13:00:00.000+08:00"
  },
  "page": null
}
```

#### #26 POST /service/open/v2/flows/{id}/stop — 停止连接流

```json
// Request: 无 body
// Response 200
{
  "code": "200",
  "messageZh": "停止成功",
  "messageEn": "Stopped",
  "data": {
    "flowId": "4444444444444444444",
    "lifecycleStatus": 3,
    "lastUpdateTime": "2026-06-09T13:05:00.000+08:00"
  },
  "page": null
}
```

#### #27 PUT /service/open/v2/flows/{id}/invalidate — 标记连接流失效

```json
// Request: 无 body
// Response 200
{
  "code": "200",
  "messageZh": "操作成功",
  "messageEn": "Success",
  "data": {
    "flowId": "4444444444444444444",
    "lifecycleStatus": 4,
    "lastUpdateTime": "2026-06-09T14:00:00.000+08:00"
  },
  "page": null
}
// 错误 — 运行中不可失效
{
  "code": "409",
  "messageZh": "运行中的连接流不可失效，请先停止",
  "messageEn": "Running flow cannot be invalidated, please stop first",
  "data": null,
  "page": null
}
```

#### #28 PUT /service/open/v2/flows/{id}/restore — 恢复连接流

```json
// Request: 无 body
// Response 200
{
  "code": "200",
  "messageZh": "恢复成功",
  "messageEn": "Restored",
  "data": {
    "flowId": "4444444444444444444",
    "lifecycleStatus": 3,
    "note": "恢复后连接流处于已停止状态，需手动启动",
    "lastUpdateTime": "2026-06-09T14:30:00.000+08:00"
  },
  "page": null
}
```

#### #29 DELETE /service/open/v2/flows/{id} — 删除连接流

```json
// Response 200
{
  "code": "200",
  "messageZh": "删除成功",
  "messageEn": "Deleted",
  "data": null,
  "page": null
}
// 错误 — 非已失效
{
  "code": "409",
  "messageZh": "仅已失效状态的连接流可删除",
  "messageEn": "Only invalidated flows can be deleted",
  "data": null,
  "page": null
}
```

---

### 3.5 连接流版本（#30~#37）

#### #30 GET /service/open/v2/flows/{id}/versions — 版本列表

```json
// Response 200
{
  "code": "200",
  "messageZh": "查询成功",
  "messageEn": "Success",
  "data": [
    {
      "versionId": "8888888888888888888",
      "versionNumber": 4,
      "status": 1,
      "createTime": "2026-06-09T10:00:00.000+08:00",
      "createBy": "zhangsan"
    },
    {
      "versionId": "7777777777777777777",
      "versionNumber": 3,
      "status": 5,
      "publishedTime": "2026-06-08T09:00:00.000+08:00",
      "publishedBy": "zhangsan",
      "createTime": "2026-06-08T08:00:00.000+08:00"
    },
    {
      "versionId": "6666666666666666666",
      "versionNumber": 2,
      "status": 5,
      "publishedTime": "2026-06-07T09:00:00.000+08:00",
      "publishedBy": "lisi",
      "deployed": true,
      "createTime": "2026-06-07T08:00:00.000+08:00"
    },
    {
      "versionId": "5555555555555555555",
      "versionNumber": 1,
      "status": 6,
      "publishedTime": "2026-06-06T08:00:00.000+08:00",
      "createTime": "2026-06-06T07:00:00.000+08:00"
    }
  ],
  "page": null
}
```

#### #31 GET /service/open/v2/flows/{id}/versions/{vid} — 版本详情

```json
// Response 200
{
  "code": "200",
  "messageZh": "操作成功",
  "messageEn": "Success",
  "data": {
    "versionId": "6666666666666666666",
    "flowId": "4444444444444444444",
    "versionNumber": 2,
    "status": 5,
    "orchestrationConfig": {
      "flowConfig": {
        "timeout": { "perNode": 10, "global": 60 },
        "rateLimit": { "mode": "QPS", "value": 100 },
        "cache": { "enabled": true, "keyTemplate": "${$.node.trigger.input.userId}", "ttl": 300 }
      },
      "nodes": [
        {
          "id": "node_trigger",
          "type": "trigger",
          "position": { "x": 100.0, "y": 200.0 },
          "data": {
            "labelCn": "接收请求", "labelEn": "Receive Request",
            "type": "http",
            "authConfig": {
              "type": "SYSTOKEN",
              "fields": [{"name":"token","carrier":"header","fieldName":"X-Sys-Token"}],
              "systokenWhitelist": ["token_abc123", "token_xyz789"]
            },
            "inputContract": {
              "protocol": "HTTP",
              "body": { "type": "object", "properties": { "sender": {"type":"string"}, "content": {"type":"string"} }, "required": ["sender","content"] }
            }
          }
        },
        {
          "id": "node_1",
          "type": "connector",
          "position": { "x": 350.0, "y": 200.0 },
          "data": {
            "labelCn": "发送通知", "labelEn": "Send Notification",
            "connectorVersionId": "1111111111111111111",
            "inputMapping": {
              "body": {
                "type": "object",
                "properties": {
                  "receiver": {"type":"string","value":"${$.node.trigger.input.sender}"},
                  "content": {"type":"string","value":"${$.node.trigger.input.content}"}
                }
              }
            }
          }
        },
        {
          "id": "node_exit",
          "type": "exit",
          "position": { "x": 650.0, "y": 200.0 },
          "data": {
            "labelCn": "返回结果", "labelEn": "Return Result",
            "outputMapping": {
              "body": {
                "type": "object",
                "properties": {
                  "msgId": {"type":"string","value":"${$.node.node_1.output.msgId}"},
                  "code": {"type":"number","value":"${$.constant:0}"}
                }
              }
            }
          }
        }
      ],
      "edges": [
        {"id":"e1","source":"node_trigger","target":"node_1","type":"smoothstep","data":{"connectionMode":"serial"}},
        {"id":"e2","source":"node_1","target":"node_exit","type":"smoothstep","data":{"connectionMode":"serial"}}
      ]
    },
    "publishedTime": "2026-06-07T09:00:00.000+08:00",
    "publishedBy": "lisi",
    "createTime": "2026-06-07T08:00:00.000+08:00"
  },
  "page": null
}
```

#### #32 PUT /service/open/v2/flows/{id}/versions/{vid} — 编辑草稿保存

```json
// Request — 编排配置全文替换（flowConfig 嵌套在 orchestrationConfig 内）
{
  "orchestrationConfig": {
    "flowConfig": {
      "timeout": { "perNode": 10, "global": 60 },
      "rateLimit": { "mode": "QPS", "value": 100 }
    },
    "nodes": [ /* ... 完整 nodes 数组 ... */ ],
    "edges": [ /* ... 完整 edges 数组 ... */ ]
  }
}

// Response 200
{
  "code": "200",
  "messageZh": "保存成功",
  "messageEn": "Saved",
  "data": {
    "versionId": "8888888888888888888",
    "versionNumber": 4,
    "status": 1,
    "lastUpdateTime": "2026-06-09T10:00:00.000+08:00"
  },
  "page": null
}
// 错误 — 编排为空
{
  "code": "422",
  "messageZh": "请先完成编排配置",
  "messageEn": "Please complete orchestration configuration",
  "data": null,
  "page": null
}
```

#### #33 POST /service/open/v2/flows/{id}/versions/{vid}/submit-approval — 提交审批

```json
// Request: 无 body
// Response 200
{
  "code": "200",
  "messageZh": "提交成功",
  "messageEn": "Submitted",
  "data": {
    "versionId": "8888888888888888888",
    "versionNumber": 4,
    "status": 2,
    "approvalId": "9999999999999999999"
  },
  "page": null
}
// 错误 — 非草稿
{
  "code": "409",
  "messageZh": "仅草稿状态可提交审批",
  "messageEn": "Only draft versions can be submitted for approval",
  "data": null,
  "page": null
}
```

#### #34 POST /service/open/v2/flows/{id}/versions/{vid}/copy-to-draft — 复制到草稿

```json
// Request: 无 body
// Response 200
{
  "code": "200",
  "messageZh": "复制成功",
  "messageEn": "Copied to draft",
  "data": {
    "versionId": "9999999999999999998",
    "versionNumber": 5,
    "status": 1,
    "sourceVersionNumber": 3,
    "message": "已覆盖当前草稿内容"
  },
  "page": null
}
// 错误 — 存在审批中版本
{
  "code": "423",
  "messageZh": "存在审批中的版本，请等待审批完成后再操作",
  "messageEn": "There is a version pending approval",
  "data": null,
  "page": null
}
```

#### #35 PUT /service/open/v2/flows/{id}/versions/{vid}/invalidate — 标记版本失效

```json
// Request: 无 body
// Response 200
{
  "code": "200",
  "messageZh": "操作成功",
  "messageEn": "Success",
  "data": {
    "versionId": "5555555555555555555",
    "versionNumber": 1,
    "status": 6,
    "lastUpdateTime": "2026-06-09T15:00:00.000+08:00"
  },
  "page": null
}
// 错误 — 版本正在运行中
{
  "code": "422",
  "messageZh": "该版本正在运行中，请先停止连接流",
  "messageEn": "Version is running, please stop the flow first",
  "data": null,
  "page": null
}
```

#### #36 PUT /service/open/v2/flows/{id}/versions/{vid}/restore — 恢复版本

```json
// Request: 无 body
// Response 200
{
  "code": "200",
  "messageZh": "恢复成功",
  "messageEn": "Restored",
  "data": {
    "versionId": "5555555555555555555",
    "versionNumber": 1,
    "status": 5,
    "lastUpdateTime": "2026-06-09T15:30:00.000+08:00"
  },
  "page": null
}
```

#### #37 DELETE /service/open/v2/flows/{id}/versions/{vid} — 删除版本

```json
// Request: 无 body
// Response 200
{
  "code": "200",
  "messageZh": "删除成功",
  "messageEn": "Deleted",
  "data": null,
  "page": null
}
```

---

### 3.6 运行记录（#38~#39）

#### #38 GET /service/open/v2/flows/{id}/executions — 运行记录列表

```json
// Query: ?curPage=1&pageSize=20&status=2&triggerType=1&startTime=2026-06-08T00:00:00.000+08:00&endTime=2026-06-09T23:59:59.000+08:00

// Response 200
{
  "code": "200",
  "messageZh": "查询成功",
  "messageEn": "Success",
  "data": [
    {
      "executionId": "1234567890123456789",
      "flowNameCn": "新消息自动通知",
      "flowNameEn": "Auto Message Notification",
      "triggerTime": "2026-06-09T10:00:01.000+08:00",
      "triggerType": 1,
      "triggerAccount": "token_abc123",
      "status": 0,
      "durationMs": 234,
      "flowVersionNumber": 2
    },
    {
      "executionId": "1234567890123456780",
      "flowNameCn": "新消息自动通知",
      "flowNameEn": "Auto Message Notification",
      "triggerTime": "2026-06-09T10:00:00.000+08:00",
      "triggerType": 2,
      "triggerAccount": "zhangsan",
      "status": 1,
      "durationMs": 5023,
      "flowVersionNumber": 4
    },
    {
      "executionId": "1234567890123456779",
      "flowNameCn": "新消息自动通知",
      "flowNameEn": "Auto Message Notification",
      "triggerTime": "2026-06-09T09:59:58.000+08:00",
      "triggerType": 1,
      "triggerAccount": "token_xyz789",
      "status": 2,
      "flowVersionNumber": 4
    }
  ],
  "page": { "curPage": 1, "pageSize": 20, "total": 150 }
}
```

#### #39 GET /service/open/v2/flows/{id}/executions/{eid} — 运行记录详情

```json
// Response 200
{
  "code": "200",
  "messageZh": "查询成功",
  "messageEn": "Success",
  "data": {
    "executionId": "1234567890123456789",
    "flowId": "4444444444444444444",
    "flowNameCn": "新消息自动通知",
    "flowNameEn": "Auto Message Notification",
    "flowVersionId": "6666666666666666666",
    "flowVersionNumber": 2,
    "triggerType": 1,
    "triggerAccount": "token_abc123",
    "triggerTime": "2026-06-09T10:00:01.000+08:00",
    "status": 0,
    "durationMs": 234,
    "errorMessage": null,
    "steps": [
      {
        "nodeId": "node_trigger",
        "nodeType": 1,
        "nodeLabelCn": "接收请求",
        "nodeLabelEn": "Receive Request",
        "iteration": 0,
        "status": 0,
        "durationMs": 2,
        "inputData": {},
        "outputData": { "sender": "u001", "content": "测试消息" },
        "errorMessage": null,
        "errorCode": null
      },
      {
        "nodeId": "node_1",
        "nodeType": 2,
        "nodeLabelCn": "发送通知",
        "nodeLabelEn": "Send Notification",
        "iteration": 0,
        "status": 0,
        "durationMs": 230,
        "inputData": { "receiver": "u001", "content": "测试消息" },
        "outputData": { "msgId": "m001", "code": 0 },
        "errorMessage": null,
        "errorCode": null
      },
      {
        "nodeId": "node_exit",
        "nodeType": 4,
        "nodeLabelCn": "返回结果",
        "nodeLabelEn": "Return Result",
        "iteration": 0,
        "status": 0,
        "durationMs": 2,
        "outputData": { "msgId": "m001", "code": 0 },
        "errorMessage": null,
        "errorCode": null
      }
    ]
  },
  "page": null
}
```

---

### 3.8 审批管理（#37~#40）

#### #37 POST /service/open/v2/connector-platform/approvals/{versionId}/urge — 一键催办

```json
// Request: 无 body
// Response 200
{
  "code": "200",
  "messageZh": "催办成功",
  "messageEn": "Urged",
  "data": {
    "notifiedApprovers": ["uid_b", "uid_c"],
    "currentLevel": 2
  },
  "page": null
}
```

#### #38 GET /service/open/v2/connector-platform/approvals/{versionId}/status — 查询审批状态

```json
// Response 200
{
  "code": "200",
  "messageZh": "查询成功",
  "messageEn": "Success",
  "data": {
    "versionId": "8888888888888888888",
    "versionStatus": 2,
    "approvalId": "9999999999999999999",
    "nodes": [
      { "level": 1, "levelName": "应用级", "status": 1, "approvers": ["uid_a"], "approvedBy": ["uid_a"], "approvedTime": "2026-06-09T11:00:00.000+08:00" },
      { "level": 2, "levelName": "平台连接流级", "status": 0, "approvers": ["uid_b", "uid_c"], "approvedBy": [] },
      { "level": 3, "levelName": "全局级", "status": 0, "approvers": ["uid_d"], "approvedBy": [] }
    ]
  },
  "page": null
}
```

#### #39 GET /service/open/v2/approval-flows — 查询审批人配置

> 💡 复用现有审批流配置接口。查询 `code=connector_flow_version_publish` 的模板即可获取连接流版本发布的三级审批人配置。

#### #40 PUT /service/open/v2/approval-flows — 更新审批人配置

> 💡 复用现有审批流配置接口。更新 `code=connector_flow_version_publish` 模板的 `nodes` JSON 即可修改审批人。

---

### 3.9 运行时·触发与调试（#41~#43）

#### #41 POST /service/open/v2/flows/{flowId}/versions/{versionId}/debug — 调试代理（open-server）

前端调用 open-server，open-server 代理转发到 connector-api。

```json
// Request
{
  "triggerData": {
    "sender": "test_user",
    "content": "调试测试消息"
  }
}

// Response 200（同步返回完整执行结果，由 connector-api 透传）
{
  "code": "200",
  "messageZh": "调试执行成功",
  "messageEn": "Debug execution success",
  "data": {
    "executionId": "1234567890123456789",
    "status": 0,
    "durationMs": 237,
    "nodes": [
      {
        "nodeId": "node_trigger",
        "nodeType": 1,
        "nodeLabelCn": "接收请求",
        "nodeLabelEn": "Receive Request",
        "iteration": 0,
        "status": 0,
        "durationMs": 2,
        "inputData": {},
        "outputData": { "sender": "test_user", "content": "调试测试消息" }
      }
    ]
  },
  "page": null
}
// 错误 — 已失效版本
{
  "code": "422",
  "messageZh": "该版本已失效，不可调试",
  "messageEn": "Invalidated version cannot be debugged",
  "data": null,
  "page": null
}
```

#### #42 POST /api/v1/flows/{flowId}/invoke — 调用已部署的连接流（connector-api）

外部系统直接调用，运行时按 `flow_t.deployed_version_id` 执行。

**前置校验**：
1. `flow_t.lifecycleStatus = 2`（运行中）
2. SYSTOKEN 凭证在白名单内
3. 未超过入站限流阈值

```json
// Request — 外部系统触发
// Header: X-Sys-Token: token_abc123
{
  "sender": "external_system",
  "content": "这是一条外部消息"
}

// Response 200（同步执行成功）
{
  "code": "200",
  "messageZh": "执行成功",
  "messageEn": "Success",
  "data": {
    "executionId": "1234567890123456789",
    "status": 0,
    "resultData": { "msgId": "msg_xxxx", "code": 0 },
    "durationMs": 234
  },
  "page": null
}

// 401 — SYSTOKEN 不在白名单
{ "code": "401", "messageZh": "SYSTOKEN 不在白名单中", "messageEn": "SYSTOKEN not in whitelist", "data": null, "page": null }

// 429 — 入站限流
{ "code": "429", "messageZh": "请求频率超限", "messageEn": "Too many requests", "data": null, "page": null }

// 503 — 未部署
{ "code": "503", "messageZh": "连接流未部署", "messageEn": "Flow not deployed", "data": null, "page": null }
```

#### #43 POST /api/v1/flows/{flowId}/versions/{versionId}/debug — 调试执行（connector-api）

由 open-server #41 代理调用，不直接暴露给前端。

```json
// Request（由 open-server 代理传入）
{
  "triggerData": {
    "sender": "test_user",
    "content": "调试测试消息"
  }
}

// Response 200（同步返回完整执行结果，同 #41 响应格式）
{
  "code": "200",
  "data": {
    "executionId": "1234567890123456789",
    "status": 0,
    "durationMs": 237,
    "nodes": [ /* ... 各节点执行详情 ... */ ]
  }
}
```

---

## 附录：修订记录

| 版本 | 日期 | 修订内容 | 修订人 |
|------|------|---------|--------|
| v1.0 | 2026-06-09 | 初始版本 — 对齐 spec.md v2.15，端点 45 个，6 个示例 | SDDU Plan Agent |
| v4.0 | 2026-06-09 | **路径语义化 + 调试代理补全**：① 占位符统一命名（{id}→{connectorId}/{flowId}，{vid}→{versionId}）；② 调试拆为 open-server 代理（#41）+ connector-api 执行（#43）双接口；③ invoke 路径改为 `/api/v1/flows/{flowId}/invoke` | SDDU Plan Agent |
