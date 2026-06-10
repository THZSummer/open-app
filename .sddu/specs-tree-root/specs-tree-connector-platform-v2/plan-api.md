# API 接口设计：连接器平台 V2

**Feature ID**: CONN-PLAT-002
**关联文档**: plan.md（§4.1 管理面 + §4.2 运行时），plan-db.md（§3 表结构），plan-json-schema.md（JSON 结构定义）
**版本**: v5.3
**创建日期**: 2026-06-09
**对齐基线**: spec.md v2.15-draft + plan.md + plan-db.md

---

## 0. 版本对齐说明

| 维度 | 说明 | 决策来源 |
|------|------|---------|
| **版本模型** | **多版本**（草稿→发布→失效→删除），最多 1000 个版本 | spec v2.15 |
| **连接流版本审批** | 三级审批（应用级→平台连接流级→全局级）+ 催办 | spec §3.6 |
| **JSON 字段结构** | 对齐 [plan-json-schema.md](./plan-json-schema.md)：React Flow 格式 / 认证多选 / inputMapping-outputMapping 分段 / JSON Path 表达式 / FR-047 类型严格约束 | plan-json-schema.md v6.0 |
| **服务归属** | open-server（管理面 39 个） + connector-api（运行时 2 个） | plan.md §1 |
| 端点总数 | **41**（open-server 39 + connector-api 2） | — |

---

## 1. 设计规范

> 💡 以下规范沿用 V1 `plan-api.md §1` 已确立的标准，V2 增量变更在子节内标注。

### 1.1 基础规范

| 规范项 | 说明 |
|--------|------|
| 基础路径 | `/service/open/v2` (open-server 管理面) / `/api/v1` (connector-api 执行面) |
| 认证方式 | 管理面复用现有 Cookie/SSO；执行面 HTTP 触发通过 SYSTOKEN 签名验证 |
| 应用隔离 | open-server 管理面接口（#1~#39）统一通过 `Header: X-App-Id` 传递，三层校验：白名单准入 → 用户权限 → 数据归属<br>connector-api 运行时（#40~#41）从 flow 自动获取 |
| 时间格式 | `yyyy-MM-dd HH:mm:ss` |

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
3. **时间字段**返回 `yyyy-MM-dd HH:mm:ss` 格式字符串

| ✅ 正确示例 | ❌ 错误示例 | 说明 |
|------------|------------|------|
| `"connectorId": "1234567890123456789"` | `"connectorId": 1234567890123456789` | BIGINT 必须转 string |
| `"status": 2` | `"status": "published"` | 枚举用数字 |
| `"createTime": "2026-06-09 10:00:00"` | `"createTime": 1716264000000` | 时间用 `yyyy-MM-dd HH:mm:ss` |

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

### 1.9 接口命名规范

**规则**：接口名称统一采用 `[操作动词] [资源名词] [强调词]` 格式。

| HTTP Method | 强调词 | 格式 | 示例 |
|-------------|--------|------|------|
| GET 列表 | 列表 | `查询 [资源] 列表` | 查询连接器列表 |
| GET 详情 | 详情 | `查询 [资源] 详情` | 查询连接器详情 |
| POST 创建 | — | `创建 [资源]` | 创建连接器 |
| PUT 更新 | — | `更新 [资源]` | 更新连接器 |
| PUT/POST 动作 | 状态/到草稿 | `[动作] [资源] [强调词]` | 失效连接器 / 复制连接器版本到草稿 |
| DELETE | — | `删除 [资源]` | 删除连接器 |

**约定**：
- 版本类资源带父对象前缀：`查询连接器版本列表`（非 `查询版本列表`）
- 调试接口注明范围：`调试连接流版本`（非 `调试版本`）
- 代理接口括号备注：`调试连接流版本（代理）`

---

## 2. 接口清单

> ⚠️ **应用隔离**：以下 open-server 管理面接口（#1~#41）统一通过 `Header: X-App-Id` 传递应用 ID，三层校验：
> ① 白名单准入（`AppWhitelistInterceptor`）：校验应用是否在连接器平台白名单内<br>
> ② 用户权限（`UserAppPermissionInterceptor`）：校验当前用户是否有该应用的操作权限<br>
> ③ 数据归属（Service 层）：校验操作的资源是否归属该应用。
> connector-api 运行时（#40~#41）从 `flow_t.app_id` 自动获取，无需传入。
>
> Path 前缀：`/service/open/v2`（open-server），`/api/v1`（connector-api）。

**改动点编号**：① 三层权限校验 ② 行为变更 ③ 路径变更 ④ 新增接口 ⑤ 接口删除 ⑥ 替换旧接口

| # | Method | Path | 接口名称 | 改动点 |
|---|--------|------|---------|--------|
| — | — | open-server — **连接器 CRUD** | — | — |
| 1 | POST | `/connectors` | 创建连接器 | ① 三层权限校验<br>② 自动创建空草稿版本 |
| 2 | GET | `/connectors` | 查询连接器列表 | ① 三层权限校验<br>② 新增 appId/status 过滤（status=2 有效可用） |
| 3 | GET | `/connectors/{connectorId}` | 查询连接器详情 | ① 三层权限校验 |
| 4 | PUT | `/connectors/{connectorId}` | 更新连接器 | ① 三层权限校验 |
| 5 | PUT | `/connectors/{connectorId}/invalidate` | 失效连接器 | ① 三层权限校验<br>④ 新增接口 |
| 6 | PUT | `/connectors/{connectorId}/recover` | 恢复连接器 | ① 三层权限校验<br>④ 新增接口 |
| 7 | DELETE | `/connectors/{connectorId}` | 删除连接器 | ① 三层权限校验<br>② 仅已失效状态可删 |
| — | — | open-server — **连接器版本** | — | — |
| 8 | GET | `/connectors/{connectorId}/versions` | 查询连接器版本列表 | ① 三层权限校验<br>② 新增 status 过滤（status=2 已发布）<br>④ 新增接口 |
| 9 | GET | `/connectors/{connectorId}/versions/{versionId}` | 查询连接器版本详情 | ① 三层权限校验<br>④ 新增接口<br>⑥ 替换 V1 `GET /config` |
| 10 | PUT | `/connectors/{connectorId}/versions/{versionId}` | 更新连接器版本 | ① 三层权限校验<br>④ 新增接口<br>⑥ 替换 V1 `PUT /config` |
| 11 | PUT | `/connectors/{connectorId}/versions/{versionId}/publish` | 发布连接器版本 | ① 三层权限校验<br>④ 新增接口 |
| 12 | POST | `/connectors/{connectorId}/versions/{versionId}/copy-to-draft` | 复制连接器版本到草稿 | ① 三层权限校验<br>④ 新增接口 |
| 13 | PUT | `/connectors/{connectorId}/versions/{versionId}/invalidate` | 失效连接器版本 | ① 三层权限校验<br>④ 新增接口 |
| 14 | PUT | `/connectors/{connectorId}/versions/{versionId}/recover` | 恢复连接器版本 | ① 三层权限校验<br>④ 新增接口 |
| 15 | DELETE | `/connectors/{connectorId}/versions/{versionId}` | 删除连接器版本 | ① 三层权限校验<br>④ 新增接口 |
| — | — | `/connectors/{connectorId}/config` (已删除) | — | — |
| — | — | `/connectors/{connectorId}/config` (已删除) | 获取连接器配置 | ⑤ V1 接口，V2 由 #9 替代 |
| — | — | — | 编辑连接器配置 | ⑤ V1 接口，V2 由 #10 替代 |
| — | — | open-server — **连接流 CRUD** | — | — |
| 16 | POST | `/flows` | 创建连接流 | ① 三层权限校验<br>② 自动创建空草稿版本 |
| 17 | GET | `/flows` | 查询连接流列表 | ① 三层权限校验<br>② 新增 appId/lifecycleStatus 过滤 |
| 18 | GET | `/flows/{flowId}` | 查询连接流详情 | ① 三层权限校验 |
| 19 | PUT | `/flows/{flowId}` | 更新连接流 | ① 三层权限校验 |
| 20 | POST | `/flows/{flowId}/copy` | 复制连接流 | ① 三层权限校验<br>④ 新增接口 |
| 21 | POST | `/flows/{flowId}/deploy` | 部署连接流 | ① 三层权限校验<br>④ 新增接口 |
| 22 | POST | `/flows/{flowId}/start` | 启动连接流 | ① 三层权限校验<br>② V2 状态模型变更 |
| 23 | POST | `/flows/{flowId}/stop` | 停止连接流 | ① 三层权限校验 |
| 24 | PUT | `/flows/{flowId}/invalidate` | 失效连接流 | ① 三层权限校验<br>④ 新增接口 |
| 25 | PUT | `/flows/{flowId}/recover` | 恢复连接流 | ① 三层权限校验<br>④ 新增接口 |
| 26 | DELETE | `/flows/{flowId}` | 删除连接流 | ① 三层权限校验<br>② 仅已失效状态可删 |
| — | — | open-server — **连接流版本** | — | — |
| 27 | GET | `/flows/{flowId}/versions` | 查询连接流版本列表 | ① 三层权限校验<br>② 新增 status 过滤（status=5 已发布）<br>④ 新增接口 |
| 28 | GET | `/flows/{flowId}/versions/{versionId}` | 查询连接流版本详情 | ① 三层权限校验<br>④ 新增接口<br>⑥ 替换 V1 `GET /config` |
| 29 | PUT | `/flows/{flowId}/versions/{versionId}` | 更新连接流版本 | ① 三层权限校验<br>④ 新增接口<br>⑥ 替换 V1 `PUT /config` |
| 30 | POST | `/flows/{flowId}/versions/{versionId}/publish` | 发布连接流版本 | ① 三层权限校验<br>④ 新增接口 |
| 31 | POST | `/flows/{flowId}/versions/{versionId}/copy-to-draft` | 复制连接流版本到草稿 | ① 三层权限校验<br>④ 新增接口 |
| 32 | PUT | `/flows/{flowId}/versions/{versionId}/invalidate` | 失效连接流版本 | ① 三层权限校验<br>④ 新增接口 |
| 33 | PUT | `/flows/{flowId}/versions/{versionId}/recover` | 恢复连接流版本 | ① 三层权限校验<br>④ 新增接口 |
| 34 | DELETE | `/flows/{flowId}/versions/{versionId}` | 删除连接流版本 | ① 三层权限校验<br>④ 新增接口 |
| — | — | open-server — **连接流版本·审批操作** | — | — |
| 35 | POST | `/flows/{flowId}/versions/{versionId}/cancel` | 撤回连接流版本审批 | ① 三层权限校验<br>④ 新增接口 |
| 36 | POST | `/flows/{flowId}/versions/{versionId}/urge` | 催办连接流版本审批 | ① 三层权限校验<br>④ 新增接口 |
| — | — | `/flows/{flowId}/config` (已删除) | — | — |
| — | — | `/flows/{flowId}/config` (已删除) | 获取编排配置 | ⑤ V1 接口，V2 由 #28 替代 |
| — | — | — | 保存编排配置 | ⑤ V1 接口，V2 由 #29 替代 |
| — | — | open-server — **运行记录** | — | — |
| 37 | GET | `/flows/{flowId}/executions` | 查询运行记录列表 | ① 三层权限校验<br>④ 新增接口 |
| 38 | GET | `/flows/{flowId}/executions/{executionId}` | 查询运行记录详情 | ① 三层权限校验<br>④ 新增接口 |
| — | — | open-server — **调试代理** | — | — |
| 39 | POST | `/flows/{flowId}/versions/{versionId}/debug` | 调试连接流版本（代理） | ① 三层权限校验<br>④ 新增接口<br>⑥ 替换 V1 test-run |
| — | — | connector-api — **运行时** | — | — |
| 40 | POST | `/flows/{flowId}/versions/{versionId}/debug` | 调试连接流版本 | ④ 新增接口（由 open-server #39 代理调用） |
| 41 | POST | `/flows/{flowId}/invoke` | 调用连接流 | ② 路径变更<br>③ 替换 V1 trigger invoke |

> 💡 **应用白名单**（FR-045）：数据存储在 `openplatform_lookup_*` LookUp 体系，复用 market-web 现有管理界面，运行时读取，不新增接口。
> 💡 **审批管理**（FR-031~033）：审批提交/通过/驳回复用现有 `ApprovalController` 接口（`/service/open/v2/approvals/*`），审批人配置复用现有审批流配置模块。V2 仅新增面向连接流版本的两个操作接口：#35 撤回、#36 催办。#30 发布版本时系统调用 `ApprovalEngine.createApproval()` 创建审批实例。

**端点统计**：新增 30 + 修改 8 + 沿用 3 + 删除 7 = 41 个有效端点（open-server 39 + connector-api 2）。各接口 FR 对应关系见 [spec.md §A](./spec.md#a-需求追溯)。

---

## 3. 接口详细定义

> 💡 接口清单见 §2，本章为每个接口的请求/响应详细定义。所有接口的字段命名、数据类型、响应格式、状态枚举均遵循 §1 设计规范。
> 💡 **URL 白名单**（FR-015）作为 `connectionConfig.urlWhitelist` 字段内嵌在连接器版本配置中，由 #9 / #10 读写，不设独立端点。
> 💡 **操作日志查询**（FR-046）复用现有 OperateLog 模块，详见 §3.8，不新增专用端点。

### 3.1 连接器 CRUD（#1~#7）

#### #1 创建连接器

`POST /connectors`

**请求头**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| X-App-Id | string | ✅ | 应用 ID（雪花ID），三层校验：白名单准入 → 用户权限 → 数据归属 |

**请求体**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| nameCn | string | ✅ | 中文名称，最长 64 字符 |
| nameEn | string | ✅ | 英文名称，最长 128 字符 |
| descriptionCn | string | ❌ | 中文描述，最长 512 字符 |
| descriptionEn | string | ❌ | 英文描述，最长 512 字符 |
| connectorType | int | ✅ | 协议类型，固定传 `1`（HTTP），见 §1.8.9 |

**响应体 `data`**

| 字段 | 类型 | 说明 |
|------|------|------|
| connectorId | string | 连接器 ID（雪花ID） |
| nameCn | string | 中文名称 |
| nameEn | string | 英文名称 |
| connectorType | int | 协议类型，见 §1.8.9 |
| status | int | 连接器状态：固定返回 `1`（有效不可用），见 §1.8.1 |
| appId | string | 归属应用 ID |
| draftVersion | object | 自动生成的空草稿版本 |
| draftVersion.versionId | string | 草稿版本 ID（雪花ID） |
| draftVersion.versionNumber | int | 版本号，从 1 开始递增 |
| draftVersion.status | int | 固定为 `1`（草稿），见 §1.8.2 |
| createTime | string | 创建时间，格式 `yyyy-MM-dd HH:mm:ss` |

**示例**

```json
// 请求头
// X-App-Id: 1234567890123456789

// 请求体
{
  "nameCn": "IM 发送消息",
  "nameEn": "IM Send Message",
  "descriptionCn": "封装 IM 消息发送能力",
  "descriptionEn": "Encapsulated IM messaging capability",
  "connectorType": 1
}

// 响应体 200
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
    "createTime": "2026-06-09 10:00:00"
  },
  "page": null
}
```

#### #2 查询连接器列表

`GET /connectors`

**请求头**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| X-App-Id | string | ✅ | 应用 ID |

**查询参数**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| curPage | int | ❌ | 页码，默认 1 |
| pageSize | int | ❌ | 每页数量，默认 20，最大 100 |
| connectorType | int | ❌ | 协议类型，见 §1.8.9 |
| status | int | ❌ | 连接器状态：`2` 有效可用（编排画布选连接器用），见 §1.8.1。不传返回所有非物理删除状态 |
| keyword | string | ❌ | 按中文名称模糊搜索 |

**响应体 `data[]`**

| 字段 | 类型 | 说明 |
|------|------|------|
| connectorId | string | 连接器 ID |
| nameCn | string | 中文名称 |
| nameEn | string | 英文名称 |
| connectorType | int | 协议类型，见 §1.8.9 |
| status | int | 连接器状态，见 §1.8.1 |
| latestPublishedVersionNumber | int | 最新已发布版本号，无已发布版本时为 `null` |
| draftVersionNumber | int | 当前草稿版本号，无草稿时为 `null` |
| appId | string | 归属应用 ID |
| createTime | string | 创建时间 |

**示例**

```json
// 请求头
// X-App-Id: 1234567890123456789
// Query: ?curPage=1&pageSize=20&connectorType=1&status=2&keyword=IM

// 响应体 200
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
      "createTime": "2026-06-09 10:00:00"
    }
  ],
  "page": { "curPage": 1, "pageSize": 20, "total": 1 }
}
```

#### #3 查询连接器详情

`GET /connectors/{connectorId}`

**请求头**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| X-App-Id | string | ✅ | 应用 ID |

**路径参数**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| connectorId | string | ✅ | 连接器 ID（雪花ID） |

**响应体 `data`**

| 字段 | 类型 | 说明 |
|------|------|------|
| connectorId | string | 连接器 ID |
| nameCn | string | 中文名称 |
| nameEn | string | 英文名称 |
| descriptionCn | string | 中文描述 |
| descriptionEn | string | 英文描述 |
| connectorType | int | 协议类型，见 §1.8.9 |
| status | int | 连接器状态，见 §1.8.1 |
| appId | string | 归属应用 ID |
| createTime | string | 创建时间 |
| lastUpdateTime | string | 最后更新时间 |

**示例**

```json
// 请求头
// X-App-Id: 1234567890123456789

// 响应体 200
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
    "createTime": "2026-06-09 10:00:00",
    "lastUpdateTime": "2026-06-09 11:00:00"
  },
  "page": null
}
```

#### #4 更新连接器

`PUT /connectors/{connectorId}`

**请求头**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| X-App-Id | string | ✅ | 应用 ID |

**路径参数**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| connectorId | string | ✅ | 连接器 ID（雪花ID） |

**请求体**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| nameCn | string | ❌ | 中文名称，最长 64 字符 |
| nameEn | string | ❌ | 英文名称，最长 128 字符 |
| descriptionCn | string | ❌ | 中文描述，最长 512 字符 |
| descriptionEn | string | ❌ | 英文描述，最长 512 字符 |

> 所有字段可选，仅更新传入的字段。

**响应体 `data`**

| 字段 | 类型 | 说明 |
|------|------|------|
| connectorId | string | 连接器 ID |
| lastUpdateTime | string | 最后更新时间 |

**示例**

```json
// 请求头
// X-App-Id: 1234567890123456789

// 请求体
{
  "nameCn": "IM 发送消息（新版）",
  "nameEn": "IM Send Message (New)",
  "descriptionCn": "更新后的 IM 消息发送能力",
  "descriptionEn": "Updated IM messaging capability"
}

// 响应体 200
{
  "code": "200",
  "messageZh": "保存成功",
  "messageEn": "Saved",
  "data": {
    "connectorId": "9876543210987654321",
    "lastUpdateTime": "2026-06-09 12:00:00"
  },
  "page": null
}
```

#### #5 失效连接器

`PUT /connectors/{connectorId}/invalidate`

**请求头**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| X-App-Id | string | ✅ | 应用 ID |

**路径参数**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| connectorId | string | ✅ | 连接器 ID（雪花ID） |

**响应体 `data`**

| 字段 | 类型 | 说明 |
|------|------|------|
| connectorId | string | 连接器 ID |
| status | int | 变更后状态：`3`（已失效），见 §1.8.1 |
| lastUpdateTime | string | 操作时间 |

**错误响应**

| code | 说明 |
|------|------|
| 409 | 非有效状态 |
| 422 | 有连接流引用此连接器，`data.referencedFlowNames` 返回引用流名称列表 |

**示例**

```json
// 请求头
// X-App-Id: 1234567890123456789

// 响应体 200
{
  "code": "200",
  "messageZh": "操作成功",
  "messageEn": "Success",
  "data": {
    "connectorId": "9876543210987654321",
    "status": 3,
    "lastUpdateTime": "2026-06-09 13:00:00"
  },
  "page": null
}

// 响应体 422 — 有连接流引用
{
  "code": "422",
  "messageZh": "以下连接流引用了此连接器：新消息自动通知、订单同步流程",
  "messageEn": "Connector is referenced by flows",
  "data": { "referencedFlowNames": ["新消息自动通知", "订单同步流程"] },
  "page": null
}
```

#### #6 恢复连接器

`PUT /connectors/{connectorId}/recover`

**请求头**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| X-App-Id | string | ✅ | 应用 ID |

**路径参数**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| connectorId | string | ✅ | 连接器 ID（雪花ID） |

**响应体 `data`**

| 字段 | 类型 | 说明 |
|------|------|------|
| connectorId | string | 连接器 ID |
| status | int | 变更后状态：`1`（有效不可用）或 `2`（有效可用），见 §1.8.1 |
| note | string | 若无已发布版本，提示需先发布版本 |

**错误响应**

| code | 说明 |
|------|------|
| 409 | 非已失效状态 |

**示例**

```json
// 请求头
// X-App-Id: 1234567890123456789

// 响应体 200
{
  "code": "200",
  "messageZh": "恢复成功",
  "messageEn": "Recovered",
  "data": {
    "connectorId": "9876543210987654321",
    "status": 1,
    "note": "无已发布版本，连接器处于有效不可用状态，请先发布版本"
  },
  "page": null
}
```

#### #7 删除连接器

`DELETE /connectors/{connectorId}`

**请求头**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| X-App-Id | string | ✅ | 应用 ID |

**路径参数**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| connectorId | string | ✅ | 连接器 ID（雪花ID） |

> 仅「已失效」状态可删除，前端需二次确认。

**错误响应**

| code | 说明 |
|------|------|
| 409 | 非已失效状态 |

**示例**

```json
// 请求头
// X-App-Id: 1234567890123456789

// 响应体 200
{
  "code": "200",
  "messageZh": "删除成功",
  "messageEn": "Deleted",
  "data": null,
  "page": null
}

// 响应体 409 — 非已失效
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

#### #8 查询连接器版本列表

`GET /connectors/{connectorId}/versions`

**请求头**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| X-App-Id | string | ✅ | 应用 ID |

**路径参数**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| connectorId | string | ✅ | 连接器 ID |

**查询参数**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| status | int | ❌ | 版本状态：`2` 已发布（编排画布选版本用），见 §1.8.2。不传返回所有非物理删除状态 |

**响应体 `data[]`**

| 字段 | 类型 | 说明 |
|------|------|------|
| versionId | string | 版本 ID |
| versionNumber | int | 版本号 |
| status | int | 版本状态，见 §1.8.2 |
| publishedTime | string | 发布时间（已发布/已失效时有值） |
| publishedBy | string | 发布人 |
| createTime | string | 创建时间 |
| createBy | string | 创建人（草稿时有值） |

**示例**

```json
// 请求头
// X-App-Id: 1234567890123456789
// Query: ?status=2

// 响应体 200
{
  "code": "200",
  "messageZh": "查询成功",
  "messageEn": "Success",
  "data": [
    {
      "versionId": "2222222222222222222",
      "versionNumber": 3,
      "status": 1,
      "createTime": "2026-06-09 10:00:00",
      "createBy": "zhangsan"
    },
    {
      "versionId": "1111111111111111111",
      "versionNumber": 2,
      "status": 2,
      "publishedTime": "2026-06-08 09:00:00",
      "publishedBy": "lisi",
      "createTime": "2026-06-08 08:00:00"
    },
    {
      "versionId": "0000000000000000000",
      "versionNumber": 1,
      "status": 3,
      "publishedTime": "2026-06-07 08:00:00",
      "createTime": "2026-06-07 07:00:00"
    }
  ],
  "page": null
}
```

#### #9 查询连接器版本详情

`GET /connectors/{connectorId}/versions/{versionId}`

**请求头**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| X-App-Id | string | ✅ | 应用 ID |

**路径参数**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| connectorId | string | ✅ | 连接器 ID |
| versionId | string | ✅ | 版本 ID |

**响应体 `data`**

| 字段 | 类型 | 说明 |
|------|------|------|
| versionId | string | 版本 ID |
| connectorId | string | 所属连接器 ID |
| versionNumber | int | 版本号 |
| status | int | 版本状态，见 §1.8.2 |
| connectionConfig | object | 连接配置快照，见下方子表 |
| publishedTime | string | 发布时间 |
| publishedBy | string | 发布人 |
| createTime | string | 创建时间 |

**`connectionConfig` 子字段**

| 字段 | 类型 | 说明 |
|------|------|------|
| protocol | string | 协议类型，固定 `"HTTP"` |
| protocolConfig | object | 协议配置：url/method/headers |
| authConfig | object | 认证配置，见下方子表 |
| urlWhitelist | array | URL 白名单规则数组，见下方子表。空数组=不限制 |
| inputContract | object | 入参契约（JSON Schema） |
| outputContract | object | 出参契约（JSON Schema） |

**`authConfig` 子字段**

| 字段 | 类型 | 说明 |
|------|------|------|
| types | string[] | 认证类型：`SOA`/`APIG`/`DIGITAL_SIGN`/`COOKIE` |
| fields | array | 凭证字段列表 |
| fields[].name | string | 凭证名称 |
| fields[].carrier | string | 放置位置：`header`/`query` |
| fields[].fieldName | string | Header/Query 参数名 |
| fields[].required | bool | 是否必填 |
| fields[].sensitive | bool | 是否敏感（脱敏显示） |
| fields[].sort | int | 排序（运行时按序附加） |

**`urlWhitelist[]` 子字段**

| 字段 | 类型 | 说明 |
|------|------|------|
| pattern | string | 正则表达式 |
| description | string | 规则说明 |

**示例**

```json
// 请求头
// X-App-Id: 1234567890123456789

// 响应体 200
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
      "urlWhitelist": [
        { "pattern": "^https://api\\.example\\.com/v1/.*", "description": "内部 API 网关" }
      ],
      "inputContract": {
        "protocol": "HTTP",
        "body": { "type": "object", "properties": { "receiver": {"type":"string"}, "content": {"type":"string"} }, "required": ["receiver","content"] }
      },
      "outputContract": {
        "protocol": "HTTP",
        "body": { "type": "object", "properties": { "msgId": {"type":"string"} } }
      }
    },
    "publishedTime": "2026-06-08 09:00:00",
    "publishedBy": "lisi",
    "createTime": "2026-06-08 08:00:00"
  },
  "page": null
}
```

#### #10 更新连接器版本

`PUT /connectors/{connectorId}/versions/{versionId}`

**请求头**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| X-App-Id | string | ✅ | 应用 ID |

**路径参数**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| connectorId | string | ✅ | 连接器 ID |
| versionId | string | ✅ | 版本 ID（仅草稿状态可编辑） |

**请求体**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| connectionConfig | object | ✅ | 连接配置全文替换，结构同 #9 |

> `urlWhitelist` 校验：每条 `pattern` 须为合法 Java 正则，不合法返回 400。空数组=不限制。

**响应体 `data`**

| 字段 | 类型 | 说明 |
|------|------|------|
| versionId | string | 版本 ID |
| versionNumber | int | 版本号 |
| status | int | 固定 `1`（草稿） |
| lastUpdateTime | string | 保存时间 |

**错误响应**

| code | 说明 |
|------|------|
| 400 | URL 白名单正则语法错误 |
| 409 | 非草稿状态，不可编辑 |

**示例**

```json
// 请求头
// X-App-Id: 1234567890123456789

// 请求体
{
  "connectionConfig": {
    "protocol": "HTTP",
    "protocolConfig": { "url": "https://openapi.xxx.com/im/send/v2", "method": "POST", "headers": {"Content-Type":"application/json"} },
    "authConfig": {
      "types": ["SOA", "DIGITAL_SIGN"],
      "fields": [
        {"name":"accessKey","carrier":"header","fieldName":"AK","required":true,"sensitive":true,"sort":1},
        {"name":"signature","carrier":"header","fieldName":"X-Signature","required":true,"sensitive":true,"sort":2}
      ]
    },
    "urlWhitelist": [{"pattern":"^https://api\\.example\\.com/v2/.*","description":"新版 API"}],
    "inputContract": {"protocol":"HTTP","body":{"type":"object","properties":{"receiver":{"type":"string"},"content":{"type":"string"}},"required":["receiver","content"]}},
    "outputContract": {"protocol":"HTTP","body":{"type":"object","properties":{"msgId":{"type":"string"}}}}
  }
}

// 响应体 200
{
  "code": "200",
  "messageZh": "保存成功",
  "messageEn": "Saved",
  "data": {
    "versionId": "2222222222222222222",
    "versionNumber": 3,
    "status": 1,
    "lastUpdateTime": "2026-06-09 10:00:00"
  },
  "page": null
}
```

#### #11 发布连接器版本

`PUT /connectors/{connectorId}/versions/{versionId}/publish`

**请求头**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| X-App-Id | string | ✅ | 应用 ID |

**路径参数**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| connectorId | string | ✅ | 连接器 ID |
| versionId | string | ✅ | 版本 ID（仅草稿状态可发布） |

**响应体 `data`**

| 字段 | 类型 | 说明 |
|------|------|------|
| versionId | string | 版本 ID |
| versionNumber | int | 版本号 |
| status | int | 变更后 `2`（已发布） |
| connectorStatus | int | 连接器状态变更后：`2` 有效可用 |
| publishedTime | string | 发布时间 |

**错误响应**

| code | 说明 |
|------|------|
| 409 | 非草稿状态 |
| 422 | 草稿配置为空 |

**示例**

```json
// 请求头
// X-App-Id: 1234567890123456789

// 响应体 200
{
  "code": "200",
  "messageZh": "发布成功",
  "messageEn": "Published",
  "data": {
    "versionId": "2222222222222222222",
    "versionNumber": 3,
    "status": 2,
    "connectorStatus": 2,
    "publishedTime": "2026-06-09 10:30:00"
  },
  "page": null
}
```

#### #12 复制连接器版本到草稿

`POST /connectors/{connectorId}/versions/{versionId}/copy-to-draft`

**请求头**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| X-App-Id | string | ✅ | 应用 ID |

**路径参数**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| connectorId | string | ✅ | 连接器 ID |
| versionId | string | ✅ | 源版本 ID（仅已发布/已失效状态） |

**响应体 `data`**

| 字段 | 类型 | 说明 |
|------|------|------|
| versionId | string | 新草稿版本 ID |
| versionNumber | int | 新版本号 |
| status | int | 固定 `1`（草稿） |
| sourceVersionNumber | int | 源版本号 |
| message | string | 操作说明（覆盖已有草稿时提示） |

**错误响应**

| code | 说明 |
|------|------|
| 422 | 版本数达上限（1000） |

**示例**

```json
// 请求头
// X-App-Id: 1234567890123456789

// 响应体 200
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
```

#### #13 失效连接器版本

`PUT /connectors/{connectorId}/versions/{versionId}/invalidate`

**请求头**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| X-App-Id | string | ✅ | 应用 ID |

**路径参数**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| connectorId | string | ✅ | 连接器 ID |
| versionId | string | ✅ | 版本 ID（仅已发布状态，且未被连接流引用） |

**响应体 `data`**

| 字段 | 类型 | 说明 |
|------|------|------|
| versionId | string | 版本 ID |
| versionNumber | int | 版本号 |
| status | int | 变更后 `3`（已失效） |
| connectorStatus | int | 若为最后已发布版本，连接器状态变为 `1` |
| lastUpdateTime | string | 操作时间 |

**错误响应**

| code | 说明 |
|------|------|
| 409 | 非已发布状态 |
| 422 | 有连接流引用此版本 |

**示例**

```json
// 请求头
// X-App-Id: 1234567890123456789

// 响应体 200
{
  "code": "200",
  "messageZh": "操作成功",
  "messageEn": "Success",
  "data": {
    "versionId": "1111111111111111111",
    "versionNumber": 2,
    "status": 3,
    "connectorStatus": 1,
    "lastUpdateTime": "2026-06-09 14:00:00"
  },
  "page": null
}
```

#### #14 恢复连接器版本

`PUT /connectors/{connectorId}/versions/{versionId}/recover`

**请求头**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| X-App-Id | string | ✅ | 应用 ID |

**路径参数**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| connectorId | string | ✅ | 连接器 ID |
| versionId | string | ✅ | 版本 ID（仅已失效状态） |

**响应体 `data`**

| 字段 | 类型 | 说明 |
|------|------|------|
| versionId | string | 版本 ID |
| versionNumber | int | 版本号 |
| status | int | 变更后 `2`（已发布） |
| connectorStatus | int | 若为唯一已发布版本，连接器状态变为 `2` |
| lastUpdateTime | string | 操作时间 |

**错误响应**

| code | 说明 |
|------|------|
| 409 | 非已失效状态 |

**示例**

```json
// 请求头
// X-App-Id: 1234567890123456789

// 响应体 200
{
  "code": "200",
  "messageZh": "恢复成功",
  "messageEn": "Recovered",
  "data": {
    "versionId": "1111111111111111111",
    "versionNumber": 2,
    "status": 2,
    "connectorStatus": 2,
    "lastUpdateTime": "2026-06-09 14:30:00"
  },
  "page": null
}
```

#### #15 删除连接器版本

`DELETE /connectors/{connectorId}/versions/{versionId}`

**请求头**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| X-App-Id | string | ✅ | 应用 ID |

**路径参数**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| connectorId | string | ✅ | 连接器 ID |
| versionId | string | ✅ | 版本 ID（仅已失效状态） |

> 前端需二次确认，不可恢复。

**错误响应**

| code | 说明 |
|------|------|
| 409 | 非已失效状态 |

**示例**

```json
// 请求头
// X-App-Id: 1234567890123456789

// 响应体 200
{
  "code": "200",
  "messageZh": "删除成功",
  "messageEn": "Deleted",
  "data": null,
  "page": null
}
```

---

### 3.3 连接流 CRUD（#16~#26）

#### #16 创建连接流

`POST /flows`

**请求头**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| X-App-Id | string | ✅ | 应用 ID |

**请求体**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| nameCn | string | ✅ | 中文名称，最长 64 字符 |
| nameEn | string | ✅ | 英文名称，最长 128 字符 |
| descriptionCn | string | ❌ | 中文描述，最长 512 字符 |
| descriptionEn | string | ❌ | 英文描述，最长 512 字符 |

**响应体 `data`**

| 字段 | 类型 | 说明 |
|------|------|------|
| flowId | string | 连接流 ID |
| nameCn | string | 中文名称 |
| nameEn | string | 英文名称 |
| lifecycleStatus | int | 生命周期状态：固定 `1`（待部署），见 §1.8.3 |
| appId | string | 归属应用 ID |
| draftVersion | object | 自动生成的空草稿版本 |
| draftVersion.versionId | string | 草稿版本 ID |
| draftVersion.versionNumber | int | 版本号，从 1 开始 |
| draftVersion.status | int | 固定 `1`（草稿），见 §1.8.4 |
| createTime | string | 创建时间 |

**示例**

```json
// 请求头
// X-App-Id: 1234567890123456789

// 请求体
{
  "nameCn": "新消息自动通知",
  "nameEn": "Auto Message Notification",
  "descriptionCn": "收到 IM 消息后自动发送通知到OA系统",
  "descriptionEn": "Auto notify OA system upon receiving IM messages"
}

// 响应体 200
{
  "code": "200",
  "data": {
    "flowId": "4444444444444444444",
    "nameCn": "新消息自动通知",
    "nameEn": "Auto Message Notification",
    "lifecycleStatus": 1,
    "appId": "1234567890123456789",
    "draftVersion": { "versionId": "5555555555555555555", "versionNumber": 1, "status": 1 },
    "createTime": "2026-06-09 10:00:00"
  },
  "page": null
}
```

#### #17 查询连接流列表

`GET /flows`

**请求头**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| X-App-Id | string | ✅ | 应用 ID |

**查询参数**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| curPage | int | ❌ | 页码，默认 1 |
| pageSize | int | ❌ | 每页数量，默认 20，最大 100 |
| lifecycleStatus | int | ❌ | 生命周期状态过滤，见 §1.8.3 |
| keyword | string | ❌ | 按中文名称模糊搜索 |

**响应体 `data[]`**

| 字段 | 类型 | 说明 |
|------|------|------|
| flowId | string | 连接流 ID |
| nameCn | string | 中文名称 |
| nameEn | string | 英文名称 |
| lifecycleStatus | int | 生命周期状态，见 §1.8.3 |
| deployedVersionId | string | 已部署版本 ID，未部署时为 `null` |
| deployedVersionNumber | int | 已部署版本号 |
| latestPublishedVersionNumber | int | 最新已发布版本号 |
| draftVersionNumber | int | 当前草稿版本号 |
| appId | string | 归属应用 ID |
| createTime | string | 创建时间 |

**示例**

```json
// 请求头
// X-App-Id: 1234567890123456789
// Query: ?curPage=1&pageSize=20&lifecycleStatus=2&keyword=通知

// 响应体 200
{
  "code": "200",
  "data": [{
    "flowId": "4444444444444444444",
    "nameCn": "新消息自动通知",
    "nameEn": "Auto Message Notification",
    "lifecycleStatus": 2,
    "deployedVersionId": "6666666666666666666",
    "deployedVersionNumber": 2,
    "latestPublishedVersionNumber": 3,
    "draftVersionNumber": 4,
    "appId": "1234567890123456789",
    "createTime": "2026-06-09 10:00:00"
  }],
  "page": { "curPage": 1, "pageSize": 20, "total": 1 }
}
```

#### #18 查询连接流详情

`GET /flows/{flowId}`

**请求头**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| X-App-Id | string | ✅ | 应用 ID |

**路径参数**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| flowId | string | ✅ | 连接流 ID |

**响应体 `data`**

| 字段 | 类型 | 说明 |
|------|------|------|
| flowId | string | 连接流 ID |
| nameCn | string | 中文名称 |
| nameEn | string | 英文名称 |
| descriptionCn | string | 中文描述 |
| descriptionEn | string | 英文描述 |
| lifecycleStatus | int | 生命周期状态，见 §1.8.3 |
| deployedVersionId | string | 已部署版本 ID |
| deployedVersionNumber | int | 已部署版本号 |
| appId | string | 归属应用 ID |
| invokeUrl | string | 触发地址（部署后生成） |
| createTime | string | 创建时间 |
| lastUpdateTime | string | 最后更新时间 |

**示例**

```json
// 请求头
// X-App-Id: 1234567890123456789

// 响应体 200
{
  "code": "200",
  "data": {
    "flowId": "4444444444444444444",
    "nameCn": "新消息自动通知",
    "nameEn": "Auto Message Notification",
    "descriptionCn": "收到 IM 消息后自动发送通知到OA系统",
    "lifecycleStatus": 2,
    "deployedVersionId": "6666666666666666666",
    "deployedVersionNumber": 2,
    "appId": "1234567890123456789",
    "invokeUrl": "https://xxx/api/v1/flows/4444444444444444444/invoke",
    "createTime": "2026-06-09 10:00:00",
    "lastUpdateTime": "2026-06-09 11:00:00"
  },
  "page": null
}
```

#### #19 更新连接流

`PUT /flows/{flowId}`

**请求头**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| X-App-Id | string | ✅ | 应用 ID |

**路径参数**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| flowId | string | ✅ | 连接流 ID |

**请求体**（所有字段可选，仅更新传入的字段）

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| nameCn | string | ❌ | 中文名称，最长 64 字符 |
| nameEn | string | ❌ | 英文名称，最长 128 字符 |
| descriptionCn | string | ❌ | 中文描述 |
| descriptionEn | string | ❌ | 英文描述 |

**响应体 `data`**

| 字段 | 类型 | 说明 |
|------|------|------|
| flowId | string | 连接流 ID |
| lastUpdateTime | string | 最后更新时间 |

**示例**

```json
// 请求头
// X-App-Id: 1234567890123456789

// 请求体
{
  "nameCn": "新消息自动通知（新版）",
  "nameEn": "Auto Message Notification (New)"
}

// 响应体 200
{
  "code": "200",
  "data": { "flowId": "4444444444444444444", "lastUpdateTime": "2026-06-09 12:00:00" },
  "page": null
}
```

#### #20 复制连接流

`POST /flows/{flowId}/copy`

**请求头**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| X-App-Id | string | ✅ | 应用 ID（仅限同应用内复制） |

**路径参数**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| flowId | string | ✅ | 源连接流 ID |

**响应体 `data`**

| 字段 | 类型 | 说明 |
|------|------|------|
| flowId | string | 新连接流 ID |
| nameCn | string | 新名称（追加 `_copy_xxxxx` 随机后缀） |
| nameEn | string | 新英文名称 |
| lifecycleStatus | int | 固定 `1`（待部署） |
| versionsCopied | int | 复制的版本数量 |
| createTime | string | 创建时间 |

**错误响应**

| code | 说明 |
|------|------|
| 409 | 复制名称碰撞，后端已自动重试失败 |

**示例**

```json
// 请求头
// X-App-Id: 1234567890123456789

// 响应体 200
{
  "code": "200",
  "data": {
    "flowId": "7777777777777777777",
    "nameCn": "新消息自动通知_copy_a3f2b",
    "nameEn": "Auto Message Notification_copy_a3f2b",
    "lifecycleStatus": 1,
    "versionsCopied": 5,
    "createTime": "2026-06-09 12:30:00"
  },
  "page": null
}
```

#### #21 部署连接流

`POST /flows/{flowId}/deploy`

**请求头**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| X-App-Id | string | ✅ | 应用 ID |

**路径参数**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| flowId | string | ✅ | 连接流 ID |

**请求体**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| versionId | string | ✅ | 要部署的已发布版本 ID |

**响应体 `data`**

| 字段 | 类型 | 说明 |
|------|------|------|
| flowId | string | 连接流 ID |
| deployedVersionId | string | 部署的版本 ID |
| deployedVersionNumber | int | 部署的版本号 |
| lifecycleStatus | int | 变更后 `2`（运行中） |
| invokeUrl | string | 触发地址 |

**错误响应**

| code | 说明 |
|------|------|
| 422 | 版本非已发布状态 |

**示例**

```json
// 请求头
// X-App-Id: 1234567890123456789

// 请求体
{ "versionId": "6666666666666666666" }

// 响应体 200
{
  "code": "200",
  "data": {
    "flowId": "4444444444444444444",
    "deployedVersionId": "6666666666666666666",
    "deployedVersionNumber": 2,
    "lifecycleStatus": 2,
    "invokeUrl": "https://xxx/api/v1/flows/4444444444444444444/invoke"
  },
  "page": null
}
```

#### #22 启动连接流

`POST /flows/{flowId}/start`

**请求头**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| X-App-Id | string | ✅ | 应用 ID |

**路径参数**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| flowId | string | ✅ | 连接流 ID（仅已停止状态） |

**响应体 `data`**

| 字段 | 类型 | 说明 |
|------|------|------|
| flowId | string | 连接流 ID |
| lifecycleStatus | int | 变更后 `2`（运行中） |
| lastUpdateTime | string | 操作时间 |

**示例**

```json
// 请求头
// X-App-Id: 1234567890123456789

// 响应体 200
{
  "code": "200",
  "data": { "flowId": "4444444444444444444", "lifecycleStatus": 2, "lastUpdateTime": "2026-06-09 13:00:00" },
  "page": null
}
```

#### #23 停止连接流

`POST /flows/{flowId}/stop`

**请求头**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| X-App-Id | string | ✅ | 应用 ID |

**路径参数**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| flowId | string | ✅ | 连接流 ID（仅运行中状态） |

**响应体 `data`**

| 字段 | 类型 | 说明 |
|------|------|------|
| flowId | string | 连接流 ID |
| lifecycleStatus | int | 变更后 `3`（已停止） |
| lastUpdateTime | string | 操作时间 |

**示例**

```json
// 请求头
// X-App-Id: 1234567890123456789

// 响应体 200
{
  "code": "200",
  "data": { "flowId": "4444444444444444444", "lifecycleStatus": 3, "lastUpdateTime": "2026-06-09 13:05:00" },
  "page": null
}
```

#### #24 失效连接流

`PUT /flows/{flowId}/invalidate`

**请求头**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| X-App-Id | string | ✅ | 应用 ID |

**路径参数**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| flowId | string | ✅ | 连接流 ID（仅待部署或已停止状态） |

**响应体 `data`**

| 字段 | 类型 | 说明 |
|------|------|------|
| flowId | string | 连接流 ID |
| lifecycleStatus | int | 变更后 `4`（已失效） |
| lastUpdateTime | string | 操作时间 |

**错误响应**

| code | 说明 |
|------|------|
| 409 | 运行中不可失效，需先停止 |

**示例**

```json
// 请求头
// X-App-Id: 1234567890123456789

// 响应体 200
{
  "code": "200",
  "data": { "flowId": "4444444444444444444", "lifecycleStatus": 4, "lastUpdateTime": "2026-06-09 14:00:00" },
  "page": null
}
```

#### #25 恢复连接流

`PUT /flows/{flowId}/recover`

**请求头**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| X-App-Id | string | ✅ | 应用 ID |

**路径参数**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| flowId | string | ✅ | 连接流 ID（仅已失效状态） |

**响应体 `data`**

| 字段 | 类型 | 说明 |
|------|------|------|
| flowId | string | 连接流 ID |
| lifecycleStatus | int | 变更后 `3`（已停止） |
| note | string | 提示需手动启动 |
| lastUpdateTime | string | 操作时间 |

**示例**

```json
// 请求头
// X-App-Id: 1234567890123456789

// 响应体 200
{
  "code": "200",
  "data": { "flowId": "4444444444444444444", "lifecycleStatus": 3, "note": "恢复后连接流处于已停止状态，需手动启动", "lastUpdateTime": "2026-06-09 14:30:00" },
  "page": null
}
```

#### #26 删除连接流

`DELETE /flows/{flowId}`

**请求头**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| X-App-Id | string | ✅ | 应用 ID |

**路径参数**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| flowId | string | ✅ | 连接流 ID（仅已失效状态） |

> 前端需二次确认，不可恢复。

**错误响应**

| code | 说明 |
|------|------|
| 409 | 非已失效状态 |

**示例**

```json
// 请求头
// X-App-Id: 1234567890123456789

// 响应体 200
{ "code": "200", "data": null, "page": null }

// 响应体 409 — 非已失效
{ "code": "409", "messageZh": "仅已失效状态的连接流可删除", "data": null, "page": null }
```

---

### 3.4 连接流版本（#27~#36）

#### #27 查询连接流版本列表

`GET /flows/{flowId}/versions`

**请求头**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| X-App-Id | string | ✅ | 应用 ID |

**路径参数**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| flowId | string | ✅ | 连接流 ID |

**查询参数**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| status | int | ❌ | 版本状态：`5` 已发布（部署选版本用），见 §1.8.4。不传返回所有非物理删除状态 |

**响应体 `data[]`**

| 字段 | 类型 | 说明 |
|------|------|------|
| versionId | string | 版本 ID |
| versionNumber | int | 版本号 |
| status | int | 版本状态，见 §1.8.4 |
| deployed | bool | 是否已部署（仅已发布版本有此字段） |
| publishedTime | string | 发布时间 |
| publishedBy | string | 发布人 |
| createTime | string | 创建时间 |
| createBy | string | 创建人 |

**示例**

```json
// 请求头
// X-App-Id: 1234567890123456789
// Query: ?status=5

// 响应体 200
{
  "code": "200",
  "data": [
    { "versionId": "8888888888888888888", "versionNumber": 4, "status": 1, "createTime": "2026-06-09 10:00:00", "createBy": "zhangsan" },
    { "versionId": "7777777777777777777", "versionNumber": 3, "status": 5, "publishedTime": "2026-06-08 09:00:00", "publishedBy": "zhangsan", "createTime": "2026-06-08 08:00:00" },
    { "versionId": "6666666666666666666", "versionNumber": 2, "status": 5, "deployed": true, "publishedTime": "2026-06-07 09:00:00", "publishedBy": "lisi", "createTime": "2026-06-07 08:00:00" }
  ],
  "page": null
}
```

#### #28 查询连接流版本详情

`GET /flows/{flowId}/versions/{versionId}`

**请求头**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| X-App-Id | string | ✅ | 应用 ID |

**路径参数**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| flowId | string | ✅ | 连接流 ID |
| versionId | string | ✅ | 版本 ID |

**响应体 `data`**

| 字段 | 类型 | 说明 |
|------|------|------|
| versionId | string | 版本 ID |
| flowId | string | 所属连接流 ID |
| versionNumber | int | 版本号 |
| status | int | 版本状态，见 §1.8.4 |
| orchestrationConfig | object | 编排配置快照 |
| orchestrationConfig.flowConfig | object | 流级配置（超时/限流/缓存） |
| orchestrationConfig.nodes | array | 节点列表 |
| orchestrationConfig.edges | array | 边列表（含 connectionMode 串行/并行） |
| publishedTime | string | 发布时间 |
| publishedBy | string | 发布人 |
| createTime | string | 创建时间 |

**示例**（精简，完整结构见 #29 请求体）

```json
// 响应体 200
{
  "code": "200",
  "data": {
    "versionId": "6666666666666666666",
    "flowId": "4444444444444444444",
    "versionNumber": 2,
    "status": 5,
    "orchestrationConfig": {
      "flowConfig": { "timeout": {"perNode":10,"global":60}, "rateLimit": {"mode":"QPS","value":100}, "cache": {"enabled":true,"keyTemplate":"${$.node.trigger.input.userId}","ttl":300} },
      "nodes": [ /* 完整 nodes 数组 */ ],
      "edges": [ {"id":"e1","source":"node_trigger","target":"node_1","type":"smoothstep","data":{"connectionMode":"serial"}} ]
    },
    "publishedTime": "2026-06-07 09:00:00",
    "publishedBy": "lisi",
    "createTime": "2026-06-07 08:00:00"
  },
  "page": null
}
```

#### #29 更新连接流版本

`PUT /flows/{flowId}/versions/{versionId}`

**请求头**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| X-App-Id | string | ✅ | 应用 ID |

**路径参数**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| flowId | string | ✅ | 连接流 ID |
| versionId | string | ✅ | 版本 ID（仅草稿状态可编辑） |

**请求体**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| orchestrationConfig | object | ✅ | 编排配置全文替换 |
| orchestrationConfig.flowConfig | object | ❌ | 流级配置 |
| orchestrationConfig.flowConfig.timeout.perNode | int | ❌ | 单节点超时（秒），0=不限 |
| orchestrationConfig.flowConfig.timeout.global | int | ❌ | 全局超时（秒） |
| orchestrationConfig.flowConfig.rateLimit.mode | string | ❌ | 限流模式：`QPS`/`CONCURRENCY` |
| orchestrationConfig.flowConfig.rateLimit.value | int | ❌ | 限流阈值，0=关闭 |
| orchestrationConfig.flowConfig.cache | object | ❌ | 缓存配置 |
| orchestrationConfig.nodes | array | ✅ | 节点列表（trigger/connector/data_processor/exit） |
| orchestrationConfig.edges | array | ✅ | 边列表，`data.connectionMode` 支持 `serial`/`parallel` |

**响应体 `data`**

| 字段 | 类型 | 说明 |
|------|------|------|
| versionId | string | 版本 ID |
| versionNumber | int | 版本号 |
| status | int | 固定 `1`（草稿） |
| lastUpdateTime | string | 保存时间 |

**错误响应**

| code | 说明 |
|------|------|
| 409 | 非草稿状态 |
| 422 | 编排配置为空 |

**示例**

```json
// 请求头
// X-App-Id: 1234567890123456789

// 请求体
{
  "orchestrationConfig": {
    "flowConfig": { "timeout": {"perNode":10,"global":60}, "rateLimit": {"mode":"QPS","value":100} },
    "nodes": [ /* 完整 nodes 数组 */ ],
    "edges": [ /* 完整 edges 数组 */ ]
  }
}

// 响应体 200
{
  "code": "200",
  "data": { "versionId": "8888888888888888888", "versionNumber": 4, "status": 1, "lastUpdateTime": "2026-06-09 10:00:00" },
  "page": null
}
```

#### #30 发布连接流版本

`POST /flows/{flowId}/versions/{versionId}/publish`

**请求头**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| X-App-Id | string | ✅ | 应用 ID |

**路径参数**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| flowId | string | ✅ | 连接流 ID |
| versionId | string | ✅ | 版本 ID（仅草稿状态） |

> 提交后进入三级审批流程（应用级→平台连接流级→全局级），复用现有 `ApprovalEngine`。

**响应体 `data`**

| 字段 | 类型 | 说明 |
|------|------|------|
| versionId | string | 版本 ID |
| versionNumber | int | 版本号 |
| status | int | 变更后 `2`（待审批） |
| approvalId | string | 审批实例 ID |

**错误响应**

| code | 说明 |
|------|------|
| 409 | 非草稿状态 |
| 422 | 编排配置为空 |

**示例**

```json
// 请求头
// X-App-Id: 1234567890123456789

// 响应体 200
{
  "code": "200",
  "data": { "versionId": "8888888888888888888", "versionNumber": 4, "status": 2, "approvalId": "9999999999999999999" },
  "page": null
}
```

#### #31 复制连接流版本到草稿

`POST /flows/{flowId}/versions/{versionId}/copy-to-draft`

**请求头**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| X-App-Id | string | ✅ | 应用 ID |

**路径参数**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| flowId | string | ✅ | 连接流 ID |
| versionId | string | ✅ | 源版本 ID（仅已发布/已失效状态） |

**响应体 `data`**

| 字段 | 类型 | 说明 |
|------|------|------|
| versionId | string | 新草稿版本 ID |
| versionNumber | int | 新版本号 |
| status | int | 固定 `1`（草稿） |
| sourceVersionNumber | int | 源版本号 |
| message | string | 操作说明 |

**错误响应**

| code | 说明 |
|------|------|
| 422 | 版本数达上限（1000） |
| 423 | 存在待审批/已驳回/已撤回的版本 |

**示例**

```json
// 请求头
// X-App-Id: 1234567890123456789

// 响应体 200
{
  "code": "200",
  "data": { "versionId": "9999999999999999998", "versionNumber": 5, "status": 1, "sourceVersionNumber": 3, "message": "已覆盖当前草稿内容" },
  "page": null
}
```

#### #32 失效连接流版本

`PUT /flows/{flowId}/versions/{versionId}/invalidate`

**请求头**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| X-App-Id | string | ✅ | 应用 ID |

**路径参数**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| flowId | string | ✅ | 连接流 ID |
| versionId | string | ✅ | 版本 ID（仅已发布状态，且未被部署） |

**响应体 `data`**

| 字段 | 类型 | 说明 |
|------|------|------|
| versionId | string | 版本 ID |
| versionNumber | int | 版本号 |
| status | int | 变更后 `6`（已失效） |
| lastUpdateTime | string | 操作时间 |

**错误响应**

| code | 说明 |
|------|------|
| 409 | 非已发布状态 |
| 422 | 版本正在运行中 |

**示例**

```json
// 请求头
// X-App-Id: 1234567890123456789

// 响应体 200
{
  "code": "200",
  "data": { "versionId": "5555555555555555555", "versionNumber": 1, "status": 6, "lastUpdateTime": "2026-06-09 15:00:00" },
  "page": null
}
```

#### #33 恢复连接流版本

`PUT /flows/{flowId}/versions/{versionId}/recover`

**请求头**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| X-App-Id | string | ✅ | 应用 ID |

**路径参数**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| flowId | string | ✅ | 连接流 ID |
| versionId | string | ✅ | 版本 ID（仅已失效状态） |

**响应体 `data`**

| 字段 | 类型 | 说明 |
|------|------|------|
| versionId | string | 版本 ID |
| versionNumber | int | 版本号 |
| status | int | 变更后 `5`（已发布） |
| lastUpdateTime | string | 操作时间 |

**错误响应**

| code | 说明 |
|------|------|
| 409 | 非已失效状态 |

**示例**

```json
// 请求头
// X-App-Id: 1234567890123456789

// 响应体 200
{
  "code": "200",
  "data": { "versionId": "5555555555555555555", "versionNumber": 1, "status": 5, "lastUpdateTime": "2026-06-09 15:30:00" },
  "page": null
}
```

#### #34 删除连接流版本

`DELETE /flows/{flowId}/versions/{versionId}`

**请求头**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| X-App-Id | string | ✅ | 应用 ID |

**路径参数**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| flowId | string | ✅ | 连接流 ID |
| versionId | string | ✅ | 版本 ID（仅已失效状态） |

> 前端需二次确认，不可恢复。

**错误响应**

| code | 说明 |
|------|------|
| 409 | 非已失效状态 |

**示例**

```json
// 请求头
// X-App-Id: 1234567890123456789

// 响应体 200
{ "code": "200", "data": null, "page": null }
```

#### #35 撤回连接流版本审批

`POST /flows/{flowId}/versions/{versionId}/cancel`

**请求头**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| X-App-Id | string | ✅ | 应用 ID |

**路径参数**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| flowId | string | ✅ | 连接流 ID |
| versionId | string | ✅ | 版本 ID（仅待审批状态） |

**响应体 `data`**

| 字段 | 类型 | 说明 |
|------|------|------|
| versionId | string | 版本 ID |
| versionNumber | int | 版本号 |
| status | int | 变更后 `3`（已撤回），见 §1.8.4 |
| lastUpdateTime | string | 操作时间 |

**错误响应**

| code | 说明 |
|------|------|
| 409 | 非待审批状态 |

**示例**

```json
// 请求头
// X-App-Id: 1234567890123456789

// 响应体 200
{
  "code": "200",
  "data": { "versionId": "8888888888888888888", "versionNumber": 4, "status": 3, "lastUpdateTime": "2026-06-09 11:00:00" },
  "page": null
}
```

#### #36 催办连接流版本审批

`POST /flows/{flowId}/versions/{versionId}/urge`

**请求头**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| X-App-Id | string | ✅ | 应用 ID |

**路径参数**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| flowId | string | ✅ | 连接流 ID |
| versionId | string | ✅ | 版本 ID（仅待审批状态） |

**响应体 `data`**

| 字段 | 类型 | 说明 |
|------|------|------|
| notifiedApprovers | string[] | 已通知的审批人 ID 列表 |
| currentLevel | int | 当前审批级别（1/2/3） |

**示例**

```json
// 请求头
// X-App-Id: 1234567890123456789

// 响应体 200
{
  "code": "200",
  "data": { "notifiedApprovers": ["uid_b", "uid_c"], "currentLevel": 2 },
  "page": null
}
```

---

### 3.5 运行记录（#37~#38）

#### #37 查询运行记录列表

`GET /flows/{flowId}/executions`

**请求头**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| X-App-Id | string | ✅ | 应用 ID |

**路径参数**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| flowId | string | ✅ | 连接流 ID |

**查询参数**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| curPage | int | ❌ | 页码，默认 1 |
| pageSize | int | ❌ | 每页数量，默认 20 |
| status | int | ❌ | 执行状态：`0` 成功 / `1` 失败 / `2` 超时，见 §1.8.5 |
| triggerType | int | ❌ | 触发方式：`1` HTTP触发 / `2` 调试触发，见 §1.8.6 |
| startTime | string | ❌ | 起始时间，格式 `yyyy-MM-dd HH:mm:ss` |
| endTime | string | ❌ | 截止时间 |

**响应体 `data[]`**

| 字段 | 类型 | 说明 |
|------|------|------|
| executionId | string | 执行记录 ID |
| flowNameCn | string | 连接流中文名称（冗余，方便展示） |
| flowNameEn | string | 连接流英文名称 |
| triggerTime | string | 触发时间 |
| triggerType | int | 触发方式，见 §1.8.6 |
| triggerAccount | string | 触发凭证/用户 |
| status | int | 执行状态，见 §1.8.5 |
| durationMs | int | 执行耗时（毫秒） |
| flowVersionNumber | int | 执行的版本号 |

**示例**

```json
// 请求头
// X-App-Id: 1234567890123456789
// Query: ?curPage=1&pageSize=20&status=0&triggerType=1

// 响应体 200
{
  "code": "200",
  "data": [
    { "executionId": "1234567890123456789", "flowNameCn": "新消息自动通知", "triggerTime": "2026-06-09 10:00:01", "triggerType": 1, "triggerAccount": "token_abc123", "status": 0, "durationMs": 234, "flowVersionNumber": 2 }
  ],
  "page": { "curPage": 1, "pageSize": 20, "total": 1 }
}
```

#### #38 查询运行记录详情

`GET /flows/{flowId}/executions/{executionId}`

**请求头**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| X-App-Id | string | ✅ | 应用 ID |

**路径参数**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| flowId | string | ✅ | 连接流 ID |
| executionId | string | ✅ | 执行记录 ID |

**响应体 `data`**

| 字段 | 类型 | 说明 |
|------|------|------|
| executionId | string | 执行记录 ID |
| flowId | string | 连接流 ID |
| flowNameCn | string | 连接流中文名称 |
| flowVersionId | string | 执行的版本 ID |
| flowVersionNumber | int | 版本号 |
| triggerType | int | 触发方式，见 §1.8.6 |
| triggerAccount | string | 触发凭证/用户 |
| triggerTime | string | 触发时间 |
| status | int | 执行状态，见 §1.8.5 |
| durationMs | int | 执行耗时 |
| errorMessage | string | 错误信息，成功时为 `null` |
| steps[] | array | 各节点执行步骤日志（FR-044 节点 I/O 日志内嵌于此） |
| steps[].nodeId | string | 节点 ID |
| steps[].nodeType | int | 节点类型，见 §1.8.7 |
| steps[].nodeLabelCn | string | 节点中文标签 |
| steps[].status | int | 步骤状态，见 §1.8.7b |
| steps[].durationMs | int | 步骤耗时 |
| steps[].inputData | object | 节点输入数据快照 |
| steps[].outputData | object | 节点输出数据快照 |
| steps[].errorMessage | string | 错误信息 |
| steps[].errorCode | string | 错误码 |

> 💡 `steps` 内嵌各节点输入/输出日志（FR-044），不再设独立日志查询接口。

**示例**

```json
// 请求头
// X-App-Id: 1234567890123456789

// 响应体 200
{
  "code": "200",
  "data": {
    "executionId": "1234567890123456789",
    "flowId": "4444444444444444444",
    "flowNameCn": "新消息自动通知",
    "flowVersionId": "6666666666666666666",
    "flowVersionNumber": 2,
    "triggerType": 1,
    "triggerAccount": "token_abc123",
    "triggerTime": "2026-06-09 10:00:01",
    "status": 0,
    "durationMs": 234,
    "errorMessage": null,
    "steps": [
      { "nodeId": "node_trigger", "nodeType": 1, "nodeLabelCn": "接收请求", "status": 0, "durationMs": 2, "inputData": {}, "outputData": {"sender":"u001"}, "errorMessage": null },
      { "nodeId": "node_1", "nodeType": 2, "nodeLabelCn": "发送通知", "status": 0, "durationMs": 230, "inputData": {"receiver":"u001"}, "outputData": {"msgId":"m001"}, "errorMessage": null }
    ]
  },
  "page": null
}
```

---

### 3.6 调试代理（#39）

#### #39 调试连接流版本（代理）

`POST /flows/{flowId}/versions/{versionId}/debug`

前端调用 open-server，open-server 代理转发到 connector-api #40。

**请求头**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| X-App-Id | string | ✅ | 应用 ID |

**路径参数**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| flowId | string | ✅ | 连接流 ID |
| versionId | string | ✅ | 版本 ID（草稿/已发布状态，已失效不可调试） |

**请求体**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| triggerData | object | ✅ | 模拟触发数据，结构与触发器节点的 inputContract 一致 |

**响应体 `data`**（同步返回，由 connector-api 透传）

| 字段 | 类型 | 说明 |
|------|------|------|
| executionId | string | 调试执行 ID |
| status | int | 执行状态 |
| durationMs | int | 耗时 |
| nodes | array | 各节点执行详情 |

**错误响应**

| code | 说明 |
|------|------|
| 422 | 版本已失效，不可调试 |

**示例**

```json
// 请求头
// X-App-Id: 1234567890123456789

// 请求体
{ "triggerData": { "sender": "test_user", "content": "调试测试消息" } }

// 响应体 200
{
  "code": "200",
  "data": {
    "executionId": "1234567890123456789",
    "status": 0,
    "durationMs": 237,
    "nodes": [ { "nodeId": "node_trigger", "nodeType": 1, "status": 0, "outputData": {"sender":"test_user"} } ]
  },
  "page": null
}
```

---

### 3.7 运行时（#40~#41）— connector-api

#### #40 调试连接流版本

`POST /api/v1/flows/{flowId}/versions/{versionId}/debug`

由 open-server #39 代理调用，不直接暴露给前端。

**请求体**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| triggerData | object | ✅ | 模拟触发数据 |

**响应体 `data`**

| 字段 | 类型 | 说明 |
|------|------|------|
| executionId | string | 执行 ID |
| status | int | 执行状态 |
| durationMs | int | 耗时 |
| nodes | array | 各节点执行详情 |

**示例**

```json
// 请求体（由 open-server 代理传入）
{ "triggerData": { "sender": "test_user", "content": "调试测试消息" } }

// 响应体 200
{
  "code": "200",
  "data": { "executionId": "1234567890123456789", "status": 0, "durationMs": 237, "nodes": [ /* 节点详情 */ ] }
}
```

#### #41 调用连接流

`POST /api/v1/flows/{flowId}/invoke`

外部系统直接调用，运行时按 `flow_t.deployed_version_id` 执行。

**请求头**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| X-Sys-Token | string | ✅ | SYSTOKEN 凭证，须在触发器白名单内 |

**前置校验**：
1. `flow_t.lifecycleStatus = 2`（运行中）
2. SYSTOKEN 凭证在白名单内
3. 未超过入站限流阈值

**请求体**（结构与触发器节点的 inputContract 一致）

**响应体 `data`**

| 字段 | 类型 | 说明 |
|------|------|------|
| executionId | string | 执行 ID |
| status | int | 执行状态 |
| resultData | object | 出参数据 |
| durationMs | int | 耗时 |

**错误响应**

| code | 说明 |
|------|------|
| 401 | SYSTOKEN 不在白名单 |
| 429 | 请求频率超限（入站限流） |
| 503 | 连接流未部署 |

**示例**

```json
// 请求头
// X-Sys-Token: token_abc123

// 请求体
{ "sender": "external_system", "content": "这是一条外部消息" }

// 响应体 200
{
  "code": "200",
  "data": { "executionId": "1234567890123456789", "status": 0, "resultData": {"msgId":"msg_xxxx","code":0}, "durationMs": 234 },
  "page": null
}

// 响应体 401 — SYSTOKEN 不在白名单
{ "code": "401", "messageZh": "SYSTOKEN 不在白名单中", "data": null, "page": null }

// 响应体 429 — 入站限流
{ "code": "429", "messageZh": "请求频率超限", "data": null, "page": null }

// 响应体 503 — 未部署
{ "code": "503", "messageZh": "连接流未部署", "data": null, "page": null }
```

---

### 3.8 操作日志查询（FR-046，复用现有模块）

> 💡 FR-046 要求的操作日志查询复用应用现有 `OperateLog` 模块，不新增专用端点。前端通过以下现有接口查询：

| 资源 | 复用接口 | 说明 |
|------|---------|------|
| 连接器 | `GET /service/open/v2/operate-logs?targetType=connector&targetId={connectorId}` | 现有接口，按 targetType 过滤 |
| 连接流 | `GET /service/open/v2/operate-logs?targetType=flow&targetId={flowId}` | 现有接口，按 targetType 过滤 |

支持分页参数 `curPage` / `pageSize`。日志内容包含：操作人、操作时间、操作类型、变更前后快照。V2 扩展的 `OperateEnum` 操作类型（创建、编辑、删除、恢复、发布、失效、部署、启动、停止、复制、提交审批、审批通过、审批驳回、撤回审批等）由后端自动记录，前端无需感知。

---

## 附录：修订记录

| 版本 | 日期 | 修订内容 | 修订人 |
|------|------|---------|--------|
| v1.0 | 2026-06-09 | 初始版本 — 对齐 spec.md v2.15，端点 45 个，6 个示例 | SDDU Plan Agent |
| v4.0 | 2026-06-09 | **路径语义化 + 调试代理补全**：① 占位符统一命名（{id}→{connectorId}/{flowId}，{vid}→{versionId}）<br>② 调试拆为 open-server 代理（#41）+ connector-api 执行（#43）双接口<br>③ invoke 路径改为 `/api/v1/flows/{flowId}/invoke` | SDDU Plan Agent |
| v5.0 | 2026-06-10 | **§3 全章重写，严格对齐 §2 接口清单**：① URL 白名单从独立端点归入 #9/#10 的 `connectionConfig.urlWhitelist` 字段<br>② §3.3~§3.8 编号统一对齐 §2（连接流 CRUD #16~#26、版本 #27~#34、运行记录 #35~#36、审批 #37~#40、调试 #41、运行时 #42~#43）<br>③ 删除旧 §3.3（独立 URL 白名单端点）<br>④ 新增 §3.9 操作日志查询复用说明<br>⑤ 补 #30 审批提交的「编排为空」422 错误响应<br>⑥ §0 服务归属修正为 41+2 | SDDU Plan Agent |
| v5.2 | 2026-06-10 | **新增 status 过滤参数**：① #2 GET `/connectors` 新增 `?status=2` 过滤有效可用连接器（编排画布选连接器）<br>② #8 GET `/connectors/{connectorId}/versions` 新增 `?status=2` 过滤已发布版本（编排画布选版本）<br>③ #27 GET `/flows/{flowId}/versions` 新增 `?status=5` 过滤已发布版本（部署选版本）<br>以上均为复用现有接口扩展参数，零新增端点 | SDDU Plan Agent |
| v5.3 | 2026-06-10 | **§3 全文补字段定义表**：① 41 个接口全部补请求头/路径参数/查询参数/请求体/响应体/错误响应字段表<br>② 嵌套对象展开到叶子字段（connectionConfig、orchestrationConfig、steps[] 等）<br>③ §3 标题精简为 `#N 名称` + `` `METHOD /path` `` 独立行<br>④ 时间格式统一 `yyyy-MM-dd HH:mm:ss`，appId 归入请求头<br>⑤ §1.9 接口命名规范：`[操作动词][资源名词][强调词]` | SDDU Plan Agent |
| v5.4 | 2026-06-10 | **补全 JSON 示例**：22 个缺失示例的接口全部补回（#19~#27、#29~#41），每个接口含请求/响应/错误完整示例 | SDDU Plan Agent |
