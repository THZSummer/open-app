# API 接口设计：连接器平台 V2

**Feature ID**: CONN-PLAT-002
**关联文档**: plan.md（§4.1 管理面 + §4.2 运行时），plan-db.md（§3 表结构），plan-json-schema.md（JSON 结构定义）
**版本**: v7.0
**创建日期**: 2026-06-09
**对齐基线**: spec.md v2.24-draft + plan-json-schema.md v9.11

---

## 0. 版本对齐说明

| 维度 | 说明 | 决策来源 |
|------|------|---------|
| **版本模型** | **多版本**（草稿→发布→失效→删除），最多 1000 个版本 | spec v2.15 |
| **连接流版本审批** | 三级审批（应用级→平台连接流级→全局级）+ 催办 | spec §3.6 |
| **JSON 字段结构** | 对齐 [plan-json-schema.md](./plan-json-schema.md) v9.11：React Flow 格式 / authConfigs 数组化多选认证 / input-output 协议分段 / JSON Path 值表达式 / flowConfig 限流+缓存 / FR-047 类型严格约束 / errorHandler 策略模型（retry/ignore/terminate + errorTypes + retryConfig） | plan-json-schema.md v9.10 |
| **服务归属** | open-server（管理面 54 个） + connector-api（运行时 2 个，其中 #54 采用透明穿透模式） | plan.md §1 |
| 端点总数 | **56**（open-server 54 + connector-api 2） | — |

---

## 1. 设计规范

> 💡 以下规范沿用 V1 `plan-api.md §1` 已确立的标准，V3 增量变更在子节内标注。

### 1.1 基础规范

| 规范项 | 说明 |
|--------|------|
| 基础路径 | `/service/open/v2` (open-server 管理面) / `/api/v1` (connector-api 执行面) |
| 认证方式 | 管理面复用现有 Cookie/SSO；执行面 HTTP 触发通过 SYSTOKEN 签名验证 |
| 应用隔离 | open-server 管理面接口（#1~#52）统一通过 `Header: X-App-Id` 传递，三层校验：白名单准入 → 用户权限 → 数据归属<br>connector-api 运行时（#53~#54）从 flow 自动获取 |
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

> ⚠️ **例外**：connector-api 运行时 `#54 调用连接流` 采用**透明穿透**模式，不使用标准信封，详见 §3.9 #54 设计理念。

所有 open-server 管理面接口（#1~#52）统一使用以下响应格式：

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

> 💡 连接流仅 4 状态。部署不改变状态，仅切换版本绑定。启动是将「已停止」迁移至「运行中」的独立操作。

| 数字 | 含义 |
|:--:|------|
| 1 | 已停止 |
| 2 | 运行中 |
| 3 | 已失效 |
| 4 | 物理删除 |

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

#### 1.8.9 运行记录缓存状态 (executionRecord.cacheStatus) — V3 新增

| 数字 | 含义 |
|:--:|------|
| 0 | 未命中（正常执行 DAG） |
| 1 | 全流命中（缓存直接返回） |
| 2 | 部分命中（V3 节点级缓存） |

#### 1.8.10 连接器协议类型 (connectorType)

| 数字 | 含义 |
|:--:|------|
| 1 | HTTP |

#### 1.8.11 日志采集开关状态 (logSwitch)

| 数字 | 含义 |
|:--:|------|
| 0 | 关闭 |
| 1 | 开启

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

> ⚠️ **应用隔离**：以下 open-server 管理面接口（#1~#51）统一通过 `Header: X-App-Id` 传递应用 ID，三层校验：
> ① 白名单准入（`AppWhitelistInterceptor`）：校验应用是否在连接器平台白名单内<br>
> ② 用户权限（`UserAppPermissionInterceptor`）：校验当前用户是否有该应用的操作权限<br>
> ③ 数据归属（Service 层）：校验操作的资源是否归属该应用。
> connector-api 运行时（#52~#53）从 `flow_t.app_id` 自动获取，无需传入。
>
> Path 前缀：`/service/open/v2`（open-server），`/api/v1`（connector-api）。
>
> **改动点编号**：① 三层权限校验 ② 行为变更 ③ 路径变更 ④ 新增接口 ⑤ 接口删除 ⑥ 替换旧接口

| # | 方法 | 路径 | 接口名称 | V3 变更 | 改动点 |
|---|--------|------|---------|:---:|--------|
| — | — | **open-server — 连接器 CRUD** | — | — | — |
| 1 | POST | `/connectors` | 创建连接器 | 改造 | ① 三层权限校验<br>② 仅创建连接器实体，不自动生成草稿版本（需手动创建 FR-005a） |
| 2 | GET | `/connectors` | 查询连接器列表 | 改造 | ① 三层权限校验<br>② 新增 appId/status 过滤<br>（status=2 有效可用） |
| 3 | GET | `/connectors/{connectorId}` | 查询连接器详情 | 改造 | ① 三层权限校验 |
| 4 | PUT | `/connectors/{connectorId}` | 更新连接器 | 改造 | ① 三层权限校验 |
| 5 | PUT | `/connectors/{connectorId}/invalidate` | 失效连接器 | 新增 | ① 三层权限校验<br>④ 新增接口 |
| 6 | PUT | `/connectors/{connectorId}/recover` | 恢复连接器 | 新增 | ① 三层权限校验<br>④ 新增接口 |
| 7 | DELETE | `/connectors/{connectorId}` | 删除连接器 | 改造 | ① 三层权限校验 |
| — | — | **open-server — 连接器版本** | — | — | — |
| 8 | POST | `/connectors/{connectorId}/versions` | 创建连接器草稿版本 | 新增 | ① 三层权限校验<br>② 版本上限 1000 校验<br>③ 生成空草稿（FR-005a） |
| 9 | GET | `/connectors/{connectorId}/versions` | 查询连接器版本列表 | 新增 | ① 三层权限校验<br>② 新增 status 过滤<br>（status=2 已发布）<br>④ 新增接口 |
| 10 | GET | `/connectors/{connectorId}/versions/{versionId}` | 查询连接器版本详情 | 新增 | ① 三层权限校验<br>④ 新增接口<br>⑥ 替换 V1 `GET /config` |
| 11 | PUT | `/connectors/{connectorId}/versions/{versionId}` | 更新连接器版本 | 新增 | ① 三层权限校验<br>④ 新增接口<br>⑥ 替换 V1 `PUT /config` |
| 12 | PUT | `/connectors/{connectorId}/versions/{versionId}/publish` | 发布连接器版本 | 新增 | ① 三层权限校验<br>④ 新增接口 |
| 13 | POST | `/connectors/{connectorId}/versions/{versionId}/copy-to-draft` | 复制连接器版本到草稿 | 新增 | ① 三层权限校验<br>④ 新增接口 |
| 14 | PUT | `/connectors/{connectorId}/versions/{versionId}/invalidate` | 失效连接器版本 | 新增 | ① 三层权限校验<br>④ 新增接口 |
| 15 | PUT | `/connectors/{connectorId}/versions/{versionId}/recover` | 恢复连接器版本 | 新增 | ① 三层权限校验<br>④ 新增接口 |
| 16 | DELETE | `/connectors/{connectorId}/versions/{versionId}` | 删除连接器版本 | 新增 | ① 三层权限校验<br>④ 新增接口 |
| — | GET | `/connectors/{connectorId}/config` | 获取连接器配置 | 删除 | ⑤ V1 接口，V3 由 #10 替代 |
| — | PUT | `/connectors/{connectorId}/config` | 编辑连接器配置 | 删除 | ⑤ V1 接口，V3 由 #11 替代 |
| — | — | **open-server — 连接流 CRUD** | — | — | — |
| 17 | POST | `/flows` | 创建连接流 | 改造 | ① 三层权限校验<br>② 仅创建连接流实体，不自动生成草稿版本（需手动创建 FR-024a） |
| 18 | GET | `/flows` | 查询连接流列表 | 改造 | ① 三层权限校验<br>② 新增 appId/lifecycleStatus 过滤 |
| 19 | GET | `/flows/{flowId}` | 查询连接流详情 | 改造 | ① 三层权限校验 |
| 20 | PUT | `/flows/{flowId}` | 更新连接流 | 改造 | ① 三层权限校验 |
| 21 | POST | `/flows/{flowId}/copy` | 复制连接流 | 新增 | ① 三层权限校验<br>④ 新增接口 |
| 22 | POST | `/flows/{flowId}/deploy` | 部署连接流 | 新增 | ① 三层权限校验<br>④ 新增接口 |
| 23 | POST | `/flows/{flowId}/start` | 启动连接流 | 改造 | ① 三层权限校验<br>② V3 状态模型变更 |
| 24 | POST | `/flows/{flowId}/stop` | 停止连接流 | 改造 | ① 三层权限校验 |
| 25 | PUT | `/flows/{flowId}/invalidate` | 失效连接流 | 新增 | ① 三层权限校验<br>④ 新增接口 |
| 26 | PUT | `/flows/{flowId}/recover` | 恢复连接流 | 新增 | ① 三层权限校验<br>④ 新增接口 |
| 27 | DELETE | `/flows/{flowId}` | 删除连接流 | 改造 | ① 三层权限校验 |
| — | — | **open-server — 连接流版本** | — | — | — |
| 28 | POST | `/flows/{flowId}/versions` | 创建连接流草稿版本 | 新增 | ① 三层权限校验<br>② 版本上限 1000 校验<br>③ 生成空草稿（FR-024a） |
| 29 | GET | `/flows/{flowId}/versions` | 查询连接流版本列表 | 新增 | ① 三层权限校验<br>② 新增 status 过滤<br>（status=5 已发布）<br>④ 新增接口 |
| 30 | GET | `/flows/{flowId}/versions/{versionId}` | 查询连接流版本详情 | 新增 | ① 三层权限校验<br>④ 新增接口<br>⑥ 替换 V1 `GET /config` |
| 31 | PUT | `/flows/{flowId}/versions/{versionId}` | 更新连接流版本 | 新增 | ① 三层权限校验<br>④ 新增接口<br>⑥ 替换 V1 `PUT /config` |
| 32 | POST | `/flows/{flowId}/versions/{versionId}/publish` | 发布连接流版本 | 新增 | ① 三层权限校验<br>④ 新增接口 |
| 33 | POST | `/flows/{flowId}/versions/{versionId}/copy-to-draft` | 复制连接流版本到草稿 | 新增 | ① 三层权限校验<br>④ 新增接口 |
| 34 | PUT | `/flows/{flowId}/versions/{versionId}/invalidate` | 失效连接流版本 | 新增 | ① 三层权限校验<br>④ 新增接口 |
| 35 | PUT | `/flows/{flowId}/versions/{versionId}/recover` | 恢复连接流版本 | 新增 | ① 三层权限校验<br>④ 新增接口 |
| 36 | DELETE | `/flows/{flowId}/versions/{versionId}` | 删除连接流版本 | 新增 | ① 三层权限校验<br>④ 新增接口 |
| — | GET | `/flows/{flowId}/config` | 获取编排配置 | 删除 | ⑤ V1 接口，V3 由 #30 替代 |
| — | PUT | `/flows/{flowId}/config` | 保存编排配置 | 删除 | ⑤ V1 接口，V3 由 #31 替代 |
| — | POST | `/flows/{flowId}/test-run` | 测试运行 | 删除 | ⑤ V1 接口，V3 由 #51 替代 |
| — | — | **open-server — 连接流版本·审批操作** | — | — | — |
| 37 | POST | `/flows/{flowId}/versions/{versionId}/cancel` | 撤回连接流版本审批 | 新增 | ④ 新增接口 |
| 38 | POST | `/flows/{flowId}/versions/{versionId}/urge` | 催办连接流版本审批 | 新增 | ④ 新增接口 |
| — | — | **open-server — 审批记录（扩展 businessType）** | — | — | — |
| 39 | GET | `/approvals/pending` | 查询审批列表 | 改造 | ② 新增 businessType 过滤<br>（connector_flow_version_publish） |
| 40 | GET | `/approvals/{id}` | 查询审批详情 | 改造 | ② businessData 新增连接流版本信息 |
| 41 | POST | `/approvals/{id}/approve` | 审批通过 | 改造 | ② 回调中 FlowVersion 状态→已发布 |
| 42 | POST | `/approvals/{id}/reject` | 审批驳回 | 改造 | ② 回调中 FlowVersion 状态→已驳回 |
| 43 | POST | `/approvals/batch-approve` | 批量审批通过 | 改造 | ② 支持业务类型<br>connector_flow_version_publish |
| 44 | POST | `/approvals/batch-reject` | 批量审批驳回 | 改造 | ② 支持业务类型<br>connector_flow_version_publish |
| — | — | **open-server — 审批流模板配置（新增 appId 字段）** | — | — | — |
| 45 | GET | `/approval-flows` | 查询审批流模板列表 | 改造 | ② 查询参数新增 `?appId` |
| 46 | GET | `/approval-flows/{id}` | 查询审批流模板详情 | 改造 | ② 响应新增 `appId` 字段 |
| 47 | POST | `/approval-flows` | 创建审批流模板 | 改造 | ② 请求体新增 appId 字段<br>② 支持创建新业务模板<br>（connector_flow_version_publish） |
| 48 | PUT | `/approval-flows/{id}` | 更新审批流模板 | 改造 | ② 请求体新增 `appId` 字段 |
| — | — | **open-server — 运行记录** | — | — | — |
| 49 | GET | `/executions` | 查询运行记录列表 | 新增 | ① 三层权限校验<br>④ 新增接口 |
| 50 | GET | `/executions/{executionId}` | 查询运行记录详情 | 新增 | ① 三层权限校验<br>④ 新增接口 |
| — | — | **open-server — 调试代理** | — | — | — |
| 51 | POST | `/flows/{flowId}/versions/{versionId}/debug` | 调试连接流版本（代理） | 新增 | ① 三层权限校验<br>④ 新增接口<br>⑥ 替换 V1 test-run |
| — | — | **open-server — 函数列表** | — | — | — |
| 52 | GET | `/data-processor/functions` | 查询数据处理函数列表 | 新增 | ① 三层权限校验<br>④ 新增接口<br>⚠️ V3 本期不实现，函数列表在 open-server 侧静态维护，后续可通过 market-server Property 扩展为动态配置 |

> ⚠️ **#52 V3 本期不实现**：数据处理函数列表（toString/toNumber/toBoolean/formatDate）在 open-server 侧静态维护，不暴露为独立端点。后续可通过 `market-server` Property 扩展为动态配置。前端使用方直接硬编码这四个函数。
| — | — | **connector-api — 运行时** | — | — | — |
| 53 | POST | `/flows/{flowId}/versions/{versionId}/debug` | 调试执行 | 新增 | ④ 新增接口<br>（由 open-server #51 代理调用） |
| 54 | POST | `/flows/{flowId}/invoke` | 调用连接流 | 改造 | ③ 路径变更<br>⑥ 替换 V1 trigger invoke<br>⚠️ **透明穿透模式**：请求/响应均由用户自定义（触发器入参 + 出口出参），平台元数据放入 `X-` 响应头，不使用标准响应信封 |

> 💡 **应用白名单**（FR-045）：数据存储在 `openplatform_lookup_*` LookUp 体系，复用 market-web 现有管理界面，运行时读取，不新增接口。
> 💡 **审批提交** 在 #32 发布版本时由后端自动调用 `ApprovalEngine.createApproval()` 创建审批实例，不暴露为独立端点。
> 💡 **#37 撤回** 走版本侧路径，与现有 `POST /approvals/{id}/cancel`（审批中心路径）并存，未来审批中心加撤回功能后可在该路径触发。
> 💡 **#39~#44** 是现有 ApprovalController 接口，V3 扩展 `businessType=connector_flow_version_publish` 场景和业务回调（审批通过→FlowVersion 已发布，驳回→已驳回）。
> 💡 **#45~#48** 是现有审批流模板接口，V3 新增 `appId` 字段支持应用隔离。

**端点统计**：新增 35 + 改造 16 + 删除 5 = 56 个（open-server 54 + connector-api 2）。connector-api #54 采用透明穿透模式（请求/响应用户自定义，平台元数据 X- 响应头）。各接口 FR 对应关系见 [spec.md §A](./spec.md#a-需求追溯)。

---

## 3. 接口详细定义

> 💡 接口清单见 §2，本章为每个接口的请求/响应详细定义。所有接口的字段命名、数据类型、响应格式、状态枚举均遵循 §1 设计规范。
> 💡 **URL 白名单**（FR-015）为连接器级独立配置，不存储在 `connectionConfig` 中。
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
| createTime | string | 创建时间，格式 `yyyy-MM-dd HH:mm:ss` |
| note | string | 提示：创建连接器后需手动创建草稿版本（FR-005a）或从已发布版本复制到草稿来获得可编辑版本 |

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
    "note": "创建连接器后需手动创建草稿版本",
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

> 💡 可删除状态参见 [spec.md §1.7.1](./spec.md#171-连接器生命周期)。前端需二次确认。

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

### 3.2 连接器版本（#8~#16）

#### #9 查询连接器版本列表

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

#### #10 查询连接器版本详情

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

**`connectionConfig` 子字段**（对齐 plan-json-schema.md §4.3.7 connectorConfigDef）

| 字段 | 类型 | 说明 |
|------|------|------|
| labelCn | string | 连接器中文标签（快照） |
| labelEn | string | 连接器英文标签（快照） |
| protocol | string | 协议类型，固定 `"HTTP"` |
| protocolConfig | object | 协议配置：url / method（headers 由 input.header 承载，不在此处） |
| authConfigs | array | 认证配置列表（minItems 1），每项见下方 `authConfigs[]` 子表 |
| input | object | 入参声明（HTTP header/query/body 三段式），见 plan-json-schema.md §4.3.5 httpInputDef |
| output | object | 出参声明（HTTP header/body 两段式），见 plan-json-schema.md §4.3.6 httpOutputDef |
| timeoutMs | int | 单次调用超时（毫秒），默认 3000 |
| rateLimitConfig | object | 限流配置（maxQps / maxConcurrency），见 plan-json-schema.md §4.3.3 |

**`authConfigs[]` 子字段**（对齐 plan-json-schema.md §4.3.2 authConfigDef v2）

| 字段 | 类型 | 说明 |
|------|------|------|
| type | string | 认证类型：`SOA`/`APIG`/`SYSTOKEN`/`AKSK`/`NONE`/`COOKIE`/`SIGNATURE` |
| header | object | 放置在请求头的认证字段（jsonObjectDef）。与 query 至少二选一 |
| query | object | 放置在 Query 的认证字段（jsonObjectDef）。与 header 至少二选一 |
| sysAccountWhitelist | string[] | type=SYSTOKEN 时必填，允许触发的账号白名单。空数组=全部禁止 |
| secretKey | object | type=SIGNATURE 时必填，签名密钥定义（jsonObjectDef） |

> `header`/`query` 内每个字段的 `value` 遵循 §3 值表达式体系（如 `"${$.system.env.soaToken}"`）。`sensitive=true` 的字段落库加密存储、日志脱敏打印。

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
      "labelCn": "IM 发送消息",
      "labelEn": "IM Send Message",
      "protocol": "HTTP",
      "protocolConfig": {
        "url": "https://openapi.xxx.com/im/send",
        "method": "POST"
      },
      "authConfigs": [
        {
          "type": "SOA",
          "header": {
            "type": "object",
            "properties": {
              "X-Soa-Token": {
                "type": "string",
                "required": true,
                "sensitive": true,
                "value": "${$.system.env.soaToken}",
                "description": "SOA 认证令牌"
              }
            }
          }
        },
        {
          "type": "SIGNATURE",
          "secretKey": {
            "type": "object",
            "properties": {
              "signSecretKey": {
                "type": "string",
                "required": true,
                "sensitive": true,
                "value": "${$.constant:user-configured-secret-key}",
                "description": "签名密钥"
              }
            }
          },
          "header": {
            "type": "object",
            "properties": {
              "X-Signature": {
                "type": "string",
                "required": true,
                "sensitive": true,
                "value": "${$.system.env.signature}",
                "description": "引擎动态计算的签名值"
              }
            }
          }
        }
      ],
      "input": {
        "protocol": "HTTP",
        "body": {
          "type": "object",
          "properties": {
            "receiver": { "type": "string", "required": true, "description": "接收者 ID" },
            "content":  { "type": "string", "required": true, "description": "消息内容" }
          }
        }
      },
      "output": {
        "protocol": "HTTP",
        "body": {
          "type": "object",
          "properties": {
            "msgId": { "type": "string", "description": "消息 ID" }
          }
        }
      },
      "timeoutMs": 5000,
      "rateLimitConfig": { "maxQps": 10, "maxConcurrency": 5 }
    },
    "publishedTime": "2026-06-08 09:00:00",
    "publishedBy": "lisi",
    "createTime": "2026-06-08 08:00:00"
  },
  "page": null
}
```

#### #11 更新连接器版本

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
| connectionConfig | object | ✅ | 连接配置全文替换，结构同 #10（对齐 plan-json-schema.md §4.3.7 connectorConfigDef） |

> `authConfigs` 校验：保存时不校验 `type` 枚举值合法性、`header`/`query` 声明、`sysAccountWhitelist`/`secretKey` 必填等结构约束（spec v2.23），全部推迟到发布时（#12）执行。

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
| 400 | JSON 完全不可解析（仅此一种情况）；正则/authConfigs 等平台要求限制校验推迟到发布时（#12） |
| 409 | 非草稿状态，不可编辑 |

**示例**

```json
// 请求头
// X-App-Id: 1234567890123456789

// 请求体
{
  "connectionConfig": {
    "labelCn": "IM 发送消息 V2",
    "labelEn": "IM Send Message V2",
    "protocol": "HTTP",
    "protocolConfig": { "url": "https://openapi.xxx.com/im/send/v2", "method": "POST" },
    "authConfigs": [
      {
        "type": "SOA",
        "header": {
          "type": "object",
          "properties": {
            "X-Soa-Token": { "type": "string", "required": true, "sensitive": true, "value": "${$.system.env.soaToken}" }
          }
        }
      },
      {
        "type": "SIGNATURE",
        "secretKey": {
          "type": "object",
          "properties": {
            "signSecretKey": { "type": "string", "required": true, "sensitive": true, "value": "${$.constant:new-secret-key}" }
          }
        },
        "header": {
          "type": "object",
          "properties": {
            "X-Signature": { "type": "string", "required": true, "sensitive": true, "value": "${$.system.env.signature}" }
          }
        }
      }
    ],
    "input": {
      "protocol": "HTTP",
      "body": {
        "type": "object",
        "properties": {
          "receiver": { "type": "string", "required": true, "description": "接收者 ID" },
          "content":  { "type": "string", "required": true, "description": "消息内容" }
        }
      }
    },
    "output": {
      "protocol": "HTTP",
      "body": {
        "type": "object",
        "properties": {
          "msgId": { "type": "string", "description": "消息 ID" }
        }
      }
    },
    "timeoutMs": 5000,
    "rateLimitConfig": { "maxQps": 20, "maxConcurrency": 5 }
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

#### #12 发布连接器版本

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

#### #13 复制连接器版本到草稿

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

#### #14 失效连接器版本

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

#### #15 恢复连接器版本

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

#### #16 删除连接器版本

`DELETE /connectors/{connectorId}/versions/{versionId}`

**请求头**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| X-App-Id | string | ✅ | 应用 ID |

**路径参数**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| connectorId | string | ✅ | 连接器 ID |
| versionId | string | ✅ | 版本 ID（可删除状态见 spec §1.7.2） |

> 💡 可删除状态参见 [spec.md §1.7.2](./spec.md#172-连接器版本生命周期)。前端需二次确认，不可恢复。

**错误响应**

| code | 说明 |
|------|------|
| 409 | 非草稿/已失效状态（已发布需先标记失效） |

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

### 3.3 连接流 CRUD（#17~#27）

#### #17 创建连接流

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
| lifecycleStatus | int | 生命周期状态：创建后固定 `1`（已停止），见 §1.8.3 |
| appId | string | 归属应用 ID |
| createTime | string | 创建时间 |
| note | string | 提示：创建连接流后需手动创建草稿版本（FR-024a）或从已发布版本复制到草稿来获得可编辑版本 |

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
    "note": "创建连接流后需手动创建草稿版本",
    "createTime": "2026-06-09 10:00:00"
  },
  "page": null
}
```

#### #18 查询连接流列表

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

#### #19 查询连接流详情

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

#### #20 更新连接流

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

#### #21 复制连接流

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
| lifecycleStatus | int | 固定 `1`（已停止） |
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

#### #22 部署连接流

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
| lifecycleStatus | int | 部署后连接流的当前生命周期状态（部署不改变状态，返回原状态值。如部署前为「已停止」则返回 1，部署前为「运行中」则返回 2） |
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

// 响应体 200（部署不改变 lifecycleStatus，此例中部署前连接流处于「已停止」）
{
  "code": "200",
  "data": {
    "flowId": "4444444444444444444",
    "deployedVersionId": "6666666666666666666",
    "deployedVersionNumber": 2,
    "lifecycleStatus": 1,
    "invokeUrl": "https://xxx/api/v1/flows/4444444444444444444/invoke"
  },
  "page": null
}
```

#### #23 启动连接流

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

#### #24 停止连接流

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

#### #25 失效连接流

`PUT /flows/{flowId}/invalidate`

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

#### #26 恢复连接流

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

#### #27 删除连接流

`DELETE /flows/{flowId}`

**请求头**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| X-App-Id | string | ✅ | 应用 ID |

**路径参数**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| flowId | string | ✅ | 连接流 ID（可删除状态见 spec §1.7.3） |

> 💡 可删除状态参见 [spec.md §1.7.3](./spec.md#173-连接流生命周期)。前端需二次确认，不可恢复。

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

### 3.4 连接流版本（#28~#38）

#### #29 查询连接流版本列表

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

#### #30 查询连接流版本详情

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
| orchestrationConfig.flowConfig | object | 流级配置（限流/缓存），见 plan-json-schema.md §6.4 |
| orchestrationConfig.nodes | array | 节点列表，每项含 `id/type/position/data`，`data` 按 `node.type` 路由到 9 种 data Schema |
| orchestrationConfig.edges | array | 边列表，每项含 `id/source/target/type/data`，`data` 承载控制流语义（businessType/connectionMode/conditionExpr/isStructural/iterationVar） |
| publishedTime | string | 发布时间 |
| publishedBy | string | 发布人 |
| createTime | string | 创建时间 |

**示例**（精简，完整结构见 #31 请求体）

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
      "flowConfig": {
        "rateLimitConfig": { "maxQps": 100, "maxConcurrency": 10 },
        "cache": { "key": ["${$.node.trigger.input.body.userId}"], "ttl": 300 }
      },
      "nodes": [
        { "id":"node_trigger", "type":"trigger", "position":{"x":100,"y":200}, "data":{ "type":"trigger", "triggerType":"http", ... } },
        { "id":"node_1", "type":"connector", "position":{"x":350,"y":200}, "data":{ "type":"connector", "labelCn":"发送消息", "connectorId":"1234567890123456000", "connectorVersionId":"1234567890123456789", "connectorConfig":{/* 连接器配置快照，§4.3.7 */}, "input":{/* 字段映射 */} } },
        { "id":"node_exit", "type":"exit", "position":{"x":600,"y":200}, "data":{ "type":"exit", "output":{...} } }
      ],
      "edges": [ {"id":"e1","source":"node_trigger","target":"node_1","type":"smoothstep","data":{"connectionMode":"serial"}} ]
    },
    "publishedTime": "2026-06-07 09:00:00",
    "publishedBy": "lisi",
    "createTime": "2026-06-07 08:00:00"
  },
  "page": null
}
```

#### #31 更新连接流版本

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
| orchestrationConfig.flowConfig | object | ❌ | 流级配置（对齐 plan-json-schema.md §6.4） |
| orchestrationConfig.flowConfig.rateLimitConfig | object | ❌ | 入站限流配置 |
| orchestrationConfig.flowConfig.rateLimitConfig.maxQps | int | ❌ | 每秒最大请求数，范围 1-10000 |
| orchestrationConfig.flowConfig.rateLimitConfig.maxConcurrency | int | ❌ | 最大并发数，范围 1-1000 |
| orchestrationConfig.flowConfig.cache | object | ❌ | 缓存配置 |
| orchestrationConfig.flowConfig.cache.key | string[] | ❌ | 缓存键表达式列表（minItems 1），每个元素遵循 §3 值表达式体系 |
| orchestrationConfig.flowConfig.cache.ttl | int | ❌ | 缓存时长（秒） |
| orchestrationConfig.nodes | array | ✅ | 节点列表（9 种 node.type：trigger/connector/data_processor/exit/loop-v2/parallel/condition-branch/error-handler/text） |
| orchestrationConfig.edges | array | ✅ | 边列表，`data` 承载控制流语义（businessType/connectionMode/conditionExpr/isStructural/iterationVar） |

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
    "flowConfig": {
      "rateLimitConfig": { "maxQps": 100, "maxConcurrency": 10 },
      "cache": { "key": ["${$.node.trigger.input.body.userId}"], "ttl": 300 }
    },
    "nodes": [
      { "id":"node_trigger", "type":"trigger", "position":{"x":100,"y":200}, "data":{ "type":"trigger", "triggerType":"http", ... } },
      { "id":"node_1", "type":"connector", "position":{"x":350,"y":200}, "data":{ "type":"connector", "labelCn":"发送消息", "connectorId":"1234567890123456000", "connectorVersionId":"1234567890123456789", "connectorConfig":{/* 连接器配置快照，§4.3.7 */}, "input":{/* 字段映射 */} } },
      { "id":"node_exit", "type":"exit", "position":{"x":600,"y":200}, "data":{ "type":"exit", "output":{...} } }
    ],
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

#### #32 发布连接流版本

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
> 节点超时值 > 应用最大超时值 → 禁止提交（EC-028）。

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

#### #33 复制连接流版本到草稿

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

#### #34 失效连接流版本

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

#### #35 恢复连接流版本

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

#### #36 删除连接流版本

`DELETE /flows/{flowId}/versions/{versionId}`

**请求头**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| X-App-Id | string | ✅ | 应用 ID |

**路径参数**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| flowId | string | ✅ | 连接流 ID |
| versionId | string | ✅ | 版本 ID（可删除状态见 spec §1.7.4） |

> 💡 可删除状态参见 [spec.md §1.7.4](./spec.md#174-连接流版本生命周期)。前端需二次确认，不可恢复。

**错误响应**

| code | 说明 |
|------|------|
| 409 | 非草稿/已撤回/已驳回/已失效状态（已发布需先标记失效，待审批需先撤回或驳回） |

**示例**

```json
// 请求头
// X-App-Id: 1234567890123456789

// 响应体 200
{ "code": "200", "data": null, "page": null }
```

#### #37 撤回连接流版本审批

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

#### #38 催办连接流版本审批

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

### 3.5 审批记录（#39~#44）

> 💡 #39~#44 是现有 `ApprovalController` 接口，V3 扩展 `businessType=connector_flow_version_publish` 场景。主要改造点：查询过滤新增业务类型、审批通过/驳回回调中触发 FlowVersion 状态变更。

#### #39 查询审批列表

`GET /approvals/pending`

> 🔧 **V3 改造**：查询参数新增 `businessType=connector_flow_version_publish` 支持。

**请求头**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| X-App-Id | string | ❌ | 审批查询不强制应用隔离 |

**查询参数**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| businessType | string | ❌ | 业务类型，V3 新增 `connector_flow_version_publish` |
| status | int | ❌ | 审批状态：`0` 待审 / `1` 已通过 / `2` 已驳回 / `3` 已撤销 |
| applicantId | string | ❌ | 申请人 ID |
| approverId | string | ❌ | 审批人 ID |
| keyword | string | ❌ | 关键词搜索（业务名称） |
| curPage | int | ❌ | 页码，默认 1 |
| pageSize | int | ❌ | 每页数量，默认 20 |

**响应体 `data[]`**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | string | 审批记录 ID |
| businessType | string | 业务类型 |
| businessId | string | 业务对象 ID（连接流版本 ID） |
| businessName | string | 业务对象名称（连接流名称 + 版本号） |
| applicantId | string | 申请人 ID |
| applicantName | string | 申请人姓名 |
| status | int | 审批状态 |
| currentNode | int | 当前审批节点索引 |
| createTime | string | 创建时间 |

**示例**

```json
// Query: ?businessType=connector_flow_version_publish&status=0&curPage=1&pageSize=20

// 响应体 200
{
  "code": "200",
  "data": [
    {
      "id": "9876543210987654321",
      "businessType": "connector_flow_version_publish",
      "businessId": "8888888888888888888",
      "businessName": "新消息通知 v3",
      "applicantId": "user_zhangsan",
      "applicantName": "张三",
      "status": 0,
      "currentNode": 1,
      "createTime": "2026-06-09 09:00:00"
    }
  ],
  "page": { "curPage": 1, "pageSize": 20, "total": 1 }
}
```

#### #40 查询审批详情

`GET /approvals/{id}`

> 🔧 **V3 改造**：`businessData` 新增连接流版本信息（版本号、编排快照摘要等）。

**路径参数**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| id | string | ✅ | 审批记录 ID |

**响应体 `data`**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | string | 审批记录 ID |
| businessType | string | 业务类型 |
| businessId | string | 业务对象 ID |
| businessData | object | 业务数据，V3 连接流版本发布场景包含 `{ flowId, flowNameCn, versionId, versionNumber, orchestrationSummary }` |
| applicantId | string | 申请人 ID |
| applicantName | string | 申请人姓名 |
| status | int | 审批状态 |
| currentNode | int | 当前审批节点索引 |
| nodes[] | array | 审批节点详情（含 `level`/`userId`/`userName`/`status`/`comment`） |
| logs[] | array | 审批操作日志 |

**示例**

```json
// 响应体 200
{
  "code": "200",
  "data": {
    "id": "9876543210987654321",
    "businessType": "connector_flow_version_publish",
    "businessId": "8888888888888888888",
    "businessData": {
      "flowId": "7777777777777777777",
      "flowNameCn": "新消息通知",
      "versionId": "8888888888888888888",
      "versionNumber": 3,
      "orchestrationSummary": "触发器→发送通知→返回结果"
    },
    "applicantId": "user_zhangsan",
    "applicantName": "张三",
    "status": 0,
    "currentNode": 1,
    "nodes": [
      { "level": "app", "userId": "user_lisi", "userName": "李四", "order": 1, "status": 1, "comment": "同意" },
      { "level": "platform_flow", "userId": "user_wangwu", "userName": "王五", "order": 2, "status": 0 },
      { "level": "global", "userId": "user_admin", "userName": "管理员", "order": 3, "status": -1 }
    ],
    "logs": [
      { "level": "app", "operatorId": "user_lisi", "operatorName": "李四", "action": 0, "actionName": "同意", "comment": "同意", "createTime": "2026-06-09 09:30:00" }
    ]
  },
  "page": null
}
```

#### #41 审批通过

`POST /approvals/{id}/approve`

> 🔧 **V3 改造**：审批通过回调中，若 `businessType=connector_flow_version_publish`，触发 FlowVersion 状态变更：待审批→已发布。

**路径参数**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| id | string | ✅ | 审批记录 ID |

**请求体**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| comment | string | ❌ | 审批意见 |

**错误响应**

| code | 说明 |
|------|------|
| 409 | 非当前审批节点操作人 |

**示例**

```json
// 请求体
{ "comment": "同意发布" }

// 响应体 200
{
  "code": "200",
  "data": { "id": "9876543210987654321", "status": 1, "message": "审批通过" },
  "page": null
}
```

#### #42 审批驳回

`POST /approvals/{id}/reject`

> 🔧 **V3 改造**：驳回回调中，若 `businessType=connector_flow_version_publish`，触发 FlowVersion 状态变更：待审批→已驳回。

**路径参数**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| id | string | ✅ | 审批记录 ID |

**请求体**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| comment | string | ✅ | 驳回原因 |

**示例**

```json
// 请求体
{ "comment": "编排配置有问题，请修改后重新提交" }

// 响应体 200
{
  "code": "200",
  "data": { "id": "9876543210987654321", "status": 2, "message": "已驳回" },
  "page": null
}
```

#### #43 批量审批通过

`POST /approvals/batch-approve`

> 🔧 **V3 改造**：支持 `connector_flow_version_publish` 业务类型批量操作。

**请求体**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| approvalIds | string[] | ✅ | 审批记录 ID 列表 |
| comment | string | ❌ | 统一审批意见 |

**响应体 `data`**

| 字段 | 类型 | 说明 |
|------|------|------|
| successCount | int | 成功数量 |
| failedCount | int | 失败数量 |
| failedItems[] | array | 失败项（含 id 和失败原因） |

**示例**

```json
// 请求体
{ "approvalIds": ["9876543210987654321", "9876543210987654322"], "comment": "批量同意" }

// 响应体 200
{
  "code": "200",
  "data": { "successCount": 2, "failedCount": 0, "failedItems": [] },
  "page": null
}
```

#### #44 批量审批驳回

`POST /approvals/batch-reject`

> 🔧 **V3 改造**：支持 `connector_flow_version_publish` 业务类型批量操作。

**请求体**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| approvalIds | string[] | ✅ | 审批记录 ID 列表 |
| comment | string | ✅ | 统一驳回原因 |

**响应体** 同 #43。

**示例**

```json
// 请求体
{ "approvalIds": ["9876543210987654321"], "comment": "批量驳回" }

// 响应体 200
{
  "code": "200",
  "data": { "successCount": 1, "failedCount": 0, "failedItems": [] },
  "page": null
}
```

---

### 3.6 审批流模板配置（#45~#48）

> 💡 #45~#48 是现有审批流模板 CRUD 接口，V3 改造：新增 `appId` 字段实现应用级审批人隔离。`appId=NULL` 为全局配置，`appId=具体值` 为指定应用配置。

#### #45 查询审批流模板列表

`GET /approval-flows`

> 🔧 **V3 改造**：查询参数新增 `?appId`。

**请求头**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| X-App-Id | string | ❌ | 模板查询不强制应用隔离 |

**查询参数**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| appId | string | ❌ | V3 新增，`NULL` 查全局模板，传入查指定应用模板 |
| code | string | ❌ | 流程编码，V3 新增 `connector_flow_version_publish` |
| keyword | string | ❌ | 关键词（名称模糊搜索） |
| curPage | int | ❌ | 页码 |
| pageSize | int | ❌ | 每页数量 |

**响应体 `data[]`**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | string | 模板 ID |
| nameCn | string | 中文名称 |
| nameEn | string | 英文名称 |
| code | string | 流程编码 |
| appId | string | V3 新增，应用 ID（`null`=全局） |
| status | int | `0` 禁用 / `1` 启用 |
| nodes[] | array | 审批节点配置 |

**示例**

```json
// Query: ?code=connector_flow_version_publish&curPage=1&pageSize=20

// 响应体 200
{
  "code": "200",
  "data": [
    {
      "id": "1111111111111111111",
      "nameCn": "连接流版本发布审批",
      "nameEn": "Flow Version Publish Approval",
      "code": "connector_flow_version_publish",
      "appId": null,
      "status": 1,
      "nodes": [
        { "level": "app", "userId": "user_lisi", "userName": "李四", "order": 1 },
        { "level": "platform_flow", "userId": "user_wangwu", "userName": "王五", "order": 2 },
        { "level": "global", "userId": "user_admin", "userName": "管理员", "order": 3 }
      ]
    }
  ],
  "page": { "curPage": 1, "pageSize": 20, "total": 1 }
}
```

#### #46 查询审批流模板详情

`GET /approval-flows/{id}`

> 🔧 **V3 改造**：响应新增 `appId` 字段。

**路径参数**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| id | string | ✅ | 模板 ID |

**响应体 `data`**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | string | 模板 ID |
| nameCn | string | 中文名称 |
| nameEn | string | 英文名称 |
| code | string | 流程编码 |
| appId | string | V3 新增，应用 ID（`null`=全局） |
| descriptionCn | string | 中文描述 |
| descriptionEn | string | 英文描述 |
| status | int | `0` 禁用 / `1` 启用 |
| nodes[] | array | 审批节点（含 `level`/`userId`/`userName`/`order`） |

**示例**

```json
// 响应体 200
{
  "code": "200",
  "data": {
    "id": "1111111111111111111",
    "nameCn": "连接流版本发布审批",
    "nameEn": "Flow Version Publish Approval",
    "code": "connector_flow_version_publish",
    "appId": null,
    "descriptionCn": "连接流版本发布需经过应用级、平台连接流级、全局级三级审批",
    "descriptionEn": "Three-level approval for flow version publishing",
    "status": 1,
    "nodes": [
      { "level": "app", "userId": "user_lisi", "userName": "李四", "order": 1 },
      { "level": "platform_flow", "userId": "user_wangwu", "userName": "王五", "order": 2 },
      { "level": "global", "userId": "user_admin", "userName": "管理员", "order": 3 }
    ]
  },
  "page": null
}
```

#### #47 创建审批流模板

`POST /approval-flows`

> 🔧 **V3 改造**：请求体新增 `appId` 字段；支持创建 `code=connector_flow_version_publish` 三级审批模板。

**请求体**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| nameCn | string | ✅ | 中文名称 |
| nameEn | string | ✅ | 英文名称 |
| code | string | ✅ | 流程编码，V3 新增 `connector_flow_version_publish` |
| appId | string | ❌ | V3 新增，`null`=全局配置，非 `null`=指定应用 |
| descriptionCn | string | ❌ | 中文描述 |
| descriptionEn | string | ❌ | 英文描述 |
| nodes[] | array | ✅ | 审批节点配置 |
| nodes[].level | string | ❌ | V3 新增，审批级别：`app` / `platform_flow` / `global` |
| nodes[].userId | string | ✅ | 审批人 ID |
| nodes[].userName | string | ✅ | 审批人姓名 |
| nodes[].order | int | ✅ | 审批顺序 |

**错误响应**

| code | 说明 |
|------|------|
| 409 | `code` + `appId` 组合已存在（唯一约束冲突） |

**示例**

```json
// 请求体
{
  "nameCn": "连接流版本发布审批",
  "nameEn": "Flow Version Publish Approval",
  "code": "connector_flow_version_publish",
  "descriptionCn": "连接流版本发布需三级审批",
  "descriptionEn": "Three-level approval for flow version publishing",
  "nodes": [
    { "level": "app", "userId": "user_lisi", "userName": "李四", "order": 1 },
    { "level": "platform_flow", "userId": "user_wangwu", "userName": "王五", "order": 2 },
    { "level": "global", "userId": "user_admin", "userName": "管理员", "order": 3 }
  ]
}

// 响应体 200
{
  "code": "200",
  "data": { "id": "1111111111111111111", "code": "connector_flow_version_publish", "status": 1 },
  "page": null
}
```

#### #48 更新审批流模板

`PUT /approval-flows/{id}`

> 🔧 **V3 改造**：请求体新增 `appId` 字段。

**路径参数**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| id | string | ✅ | 模板 ID |

**请求体**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| nameCn | string | ❌ | 中文名称 |
| nameEn | string | ❌ | 英文名称 |
| appId | string | ❌ | V3 新增，应用 ID |
| status | int | ❌ | `0` 禁用 / `1` 启用 |
| nodes[] | array | ❌ | 审批节点配置 |

**示例**

```json
// 请求体
{ "nameCn": "连接流版本发布审批v2", "nodes": [
  { "level": "app", "userId": "user_zhao", "userName": "赵六", "order": 1 },
  { "level": "platform_flow", "userId": "user_wangwu", "userName": "王五", "order": 2 }
]}

// 响应体 200
{
  "code": "200",
  "data": { "id": "1111111111111111111", "status": 1 },
  "page": null
}
```

---

### 3.7 运行记录（#49~#50）

#### #49 查询运行记录列表

`GET /executions`

**请求头**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| X-App-Id | string | ✅ | 应用 ID（数据隔离） |

**查询参数**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| curPage | int | ❌ | 页码，默认 1 |
| pageSize | int | ❌ | 每页数量，默认 20 |
| keyword | string | ❌ | 按连接流名称模糊搜索（同时匹配 flowNameCn 和 flowNameEn） |
| flowId | string | ❌ | 按连接流 ID 过滤 |
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
// Query: ?curPage=1&pageSize=20&keyword=通知&status=0&flowId=4444444444444444444

// 响应体 200
{
  "code": "200",
  "data": [
    { "executionId": "1234567890123456789", "flowNameCn": "新消息自动通知", "triggerTime": "2026-06-09 10:00:01", "triggerType": 1, "triggerAccount": "token_abc123", "status": 0, "durationMs": 234, "flowVersionNumber": 2 }
  ],
  "page": { "curPage": 1, "pageSize": 20, "total": 1 }
}
```

#### #50 查询运行记录详情

`GET /executions/{executionId}`

**请求头**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| X-App-Id | string | ✅ | 应用 ID（数据隔离 + 归属校验） |

**路径参数**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| executionId | string | ✅ | 执行记录 ID（全局唯一雪花 ID） |

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
      {
        "nodeId": "n1", "nodeType": 1, "nodeLabelCn": "HTTP 触发",
        "status": 0, "durationMs": 2,
        "inputData": { "header": {...}, "query": {...}, "body": {...} },
        "outputData": { "body": { "msg": "hello" } }
      }
    ]
  },
  "page": null
}
```

---

### 3.8 调试代理（#51）

#### #51 调试连接流版本（代理）

`POST /flows/{flowId}/versions/{versionId}/debug`

前端调用 open-server，open-server 代理转发到 connector-api #53。

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
| triggerData | object | ✅ | 模拟触发数据，结构与触发器节点的 `input`（httpInputDef）一致 |

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

### 3.8a 函数列表（#52）— open-server

#### #52 查询数据处理函数列表

`GET /service/open/v2/data-processor/functions`

**请求头**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| X-App-Id | string | ✅ | 应用 ID |

> 💡 本期仅支持字段类型转换系列函数（`toString` / `toNumber` / `toBoolean` / `formatDate`），函数列表在 open-server 侧静态维护。后续可通过 market-server Property 扩展为动态配置。

**响应体 `data[]`**

| 字段 | 类型 | 说明 |
|------|------|------|
| name | string | 函数名 |
| labelCn | string | 中文名称 |
| labelEn | string | 英文名称 |
| descriptionCn | string | 中文描述 |
| descriptionEn | string | 英文描述 |
| input | object | 入参定义，jsonObjectDef 格式（见 [plan-json-schema.md §4.3.2](./plan-json-schema.md)） |
| output | object | 出参定义，jsonObjectDef 格式 |

> 💡 `input` 和 `output` 复用 dataProcessorNodeDataDef 的 output 字段结构（jsonObjectDef），前端可直接渲染为 Schema 编辑器。

**示例**

```json
// 响应体 200
{
  "code": "200",
  "messageZh": "成功",
  "messageEn": "Success",
  "data": [
    {
      "name": "toString",
      "labelCn": "转为字符串",
      "labelEn": "To String",
      "descriptionCn": "将任意类型值转为字符串",
      "descriptionEn": "Convert any value to string",
      "input": {
        "type": "object",
        "properties": {
          "value": { "type": "string", "description": "输入值" }
        },
        "required": ["value"]
      },
      "output": { "type": "string" }
    },
    {
      "name": "toNumber",
      "labelCn": "转为数字",
      "labelEn": "To Number",
      "descriptionCn": "将字符串转为数字",
      "descriptionEn": "Convert string to number",
      "input": {
        "type": "object",
        "properties": {
          "value": { "type": "string", "description": "输入值" }
        },
        "required": ["value"]
      },
      "output": { "type": "number" }
    },
    {
      "name": "toBoolean",
      "labelCn": "转为布尔",
      "labelEn": "To Boolean",
      "descriptionCn": "将字符串/数字转为布尔值",
      "descriptionEn": "Convert string/number to boolean",
      "input": {
        "type": "object",
        "properties": {
          "value": { "type": "string", "description": "输入值" }
        },
        "required": ["value"]
      },
      "output": { "type": "boolean" }
    },
    {
      "name": "formatDate",
      "labelCn": "日期格式化",
      "labelEn": "Format Date",
      "descriptionCn": "将日期字符串按指定格式转换",
      "descriptionEn": "Format date string with pattern",
      "input": {
        "type": "object",
        "properties": {
          "value": { "type": "string", "description": "日期值" },
          "fromPattern": { "type": "string", "description": "源格式", "example": "yyyy-MM-dd HH:mm:ss" },
          "toPattern": { "type": "string", "description": "目标格式", "example": "yyyy/MM/dd" }
        },
        "required": ["value", "fromPattern", "toPattern"]
      },
      "output": { "type": "string" }
    }
  ],
  "page": null
}
```

---

### 3.9 运行时（#53~#54）— connector-api

#### #53 调试连接流版本

`POST /api/v1/flows/{flowId}/versions/{versionId}/debug`

由 open-server #51 代理调用，不直接暴露给前端。

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

#### #54 调用连接流

`POST /api/v1/flows/{flowId}/invoke`

外部系统直接调用，运行时按 `flow_t.deployed_version_id` 执行。

##### 设计理念：透明穿透（Transparent Passthrough）

`#54` 接口采用**透明穿透**模式——除了接口地址（URL 路径）由平台固定外，**请求和响应的所有参数、返回值均由用户在连接流中自定义**：

- **请求侧**：完全遵循连接流中触发器节点的 `input`（httpInputDef）定义。调用方传入的 Header、Query 参数、Body 结构均由触发器节点的入参 Schema 决定。
- **响应侧**：完全遵循连接流中出口节点的 `output`（httpOutputDef）定义。平台不包装任何业务数据，调用方收到的响应 Header（用户自定义部分）和 Body 结构均由出口节点的出参 Schema 决定。
- **平台元数据**：所有平台级响应参数（执行 ID、状态、耗时、错误信息、缓存命中状态等）统一追加到响应头，采用 `X-` 前缀格式，与用户自定义的响应 Header/body 完全分离。

> 💡 此模式下，`#54` 接口**不使用**标准 `{ code, messageZh, messageEn, data, page }` 响应信封。响应 Body 即为出口节点的出参数据，响应 Header 中含平台 `X-` 前缀元数据。

##### 请求（Request）

**请求头 — 平台认证**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| X-Sys-Token | string | ✅ | SYSTOKEN 凭证，须在触发器白名单内 |

**请求头 — 用户自定义**

> 由触发器节点的 `input.header`（httpInputDef.header）定义。调用方需按此 Schema 传入对应 Header 字段（含必填/可选约束）。

**查询参数 — 用户自定义**

> 由触发器节点的 `input.query`（httpInputDef.query）定义。调用方需按此 Schema 传入对应 Query 参数（含必填/可选约束）。

**请求体 — 用户自定义**

> 由触发器节点的 `input.body`（httpInputDef.body）定义。调用方需按此 Schema 传入对应 Body（含必填/可选约束、类型约束 FR-047）。

##### 前置校验

1. `flow_t.lifecycleStatus = 2`（运行中）
2. SYSTOKEN 凭证在触发器 `authConfig.sysAccountWhitelist` 白名单内
3. 未超过入站限流阈值（`flowConfig.rateLimitConfig`）

##### 响应（Response）

**响应头 — 平台元数据（全部 `X-` 前缀）**

| Header | 类型 | 说明 | 出现条件 |
|--------|------|------|---------|
| X-Flow-Id | string | 连接流 ID（雪花ID），对应 URL 路径中的 `{flowId}` | 始终返回 |
| X-Execution-Id | string | 执行记录 ID（雪花ID） | 连接流已执行（含执行失败） |
| X-Status | int | 执行状态：`0`=成功 / `1`=失败 / `2`=超时，见 §1.8.5 | 连接流已执行 |
| X-Duration-Ms | int | 执行耗时（毫秒） | 连接流已执行 |
| X-Cache-Status | int | 缓存命中状态：`0`=未命中 / `1`=全流命中 / `2`=部分命中，见 §1.8.9 | 缓存已生效 |
| X-Code | int | 平台结果码：`200`=成功，其余见 §1.7 错误码定义 | 始终返回 |
| X-Message-Zh | string | 中文提示信息（成功/错误描述） | 始终返回 |
| X-Message-En | string | 英文提示信息 | 始终返回 |

> ⚠️ 前置校验失败（401/429/503）时，连接流未实际执行，`X-Execution-Id`、`X-Status`、`X-Duration-Ms` 不出现。`X-Flow-Id`、`X-Code`、`X-Message-Zh`、`X-Message-En` 始终返回。

**响应头 — 用户自定义**

> 由出口节点的 `output.header`（exitNodeDataDef.output.header）定义。平台按此 Schema 将对应的响应头字段值注入到 HTTP 响应头中（值来源于 §3 值表达式体系解析结果）。

**响应体 — 用户自定义**

> 由出口节点的 `output.body`（exitNodeDataDef.output.body）定义。平台按此 Schema 构造响应 Body（值来源于 §3 值表达式体系解析结果），Body 即为出口节点的出参数据，**不经过任何平台信封包装**。

**HTTP 状态码约定**

| HTTP Status | 场景 | X-Code |
|-------------|------|--------|
| `200` | 连接流执行完成（无论节点成功/失败，见 X-Status） | 200 |
| `401` | SYSTOKEN 不在白名单 | 401 |
| `429` | 请求频率超限（入站限流） | 429 |
| `503` | 连接流未部署 | 503 |
| `500` | 引擎内部错误 | 500 |

##### 示例

**示例 1：成功执行**

假设触发器节点 `httpInputDef` 定义：
```json
{ "protocol": "HTTP", "body": { "type": "object", "properties": { "sender": {"type":"string"}, "content": {"type":"string"} } } }
```

假设出口节点 `output` 定义：
```json
{ "body": { "type": "object", "properties": { "msgId": {"type":"string","value":"${$.node.conn_1.output.body.msgId}"}, "code": {"type":"number","value":"${$.node.conn_1.output.body.code}"} } } }
```

```
// 请求
POST /api/v1/flows/4444444444444444444/invoke
X-Sys-Token: token_abc123
Content-Type: application/json

{"sender": "external_system", "content": "这是一条外部消息"}

// 响应（HTTP 200）
X-Flow-Id: 4444444444444444444
X-Execution-Id: 1234567890123456789
X-Status: 0
X-Duration-Ms: 234
X-Code: 200
X-Message-Zh: 成功
X-Message-En: Success
X-Cache-Status: 0

{"msgId": "msg_xxxx", "code": 0}
```

**示例 2：SYSTOKEN 不在白名单**

```
// 请求
POST /api/v1/flows/4444444444444444444/invoke
X-Sys-Token: token_invalid

// 响应（HTTP 401）
X-Flow-Id: 4444444444444444444
X-Code: 401
X-Message-Zh: SYSTOKEN 不在白名单中
X-Message-En: SYSTOKEN not in whitelist

（空 Body）
```

**示例 3：入站限流**

```
// 响应（HTTP 429）
X-Flow-Id: 4444444444444444444
X-Code: 429
X-Message-Zh: 请求频率超限
X-Message-En: Rate limit exceeded

（空 Body）
```

**示例 4：未部署**

```
// 响应（HTTP 503）
X-Flow-Id: 4444444444444444444
X-Code: 503
X-Message-Zh: 连接流未部署
X-Message-En: Flow not deployed

（空 Body）
```

**示例 5：执行失败（连接器节点报错）**

```
// 响应（HTTP 200）
X-Flow-Id: 4444444444444444444
X-Execution-Id: 1234567890123456790
X-Status: 1
X-Duration-Ms: 5123
X-Code: 200
X-Message-Zh: 成功
X-Message-En: Success

{"msgId": null, "code": -1}
```

**示例 6：缓存命中（全流）**

```
// 响应（HTTP 200）
X-Flow-Id: 4444444444444444444
X-Execution-Id: 1234567890123456791
X-Status: 0
X-Duration-Ms: 2
X-Code: 200
X-Message-Zh: 成功
X-Message-En: Success
X-Cache-Status: 1

{"msgId": "msg_cached", "code": 0}
```

**示例 7：含用户自定义响应头**

假设出口节点 `output` 定义：
```json
{
  "header": { "type": "object", "properties": { "X-Request-Id": {"type":"string","value":"${$.execution.id}"} } },
  "body": { "type": "object", "properties": { "result": {"type":"string","value":"${$.constant:ok}"} } }
}
```

```
// 响应（HTTP 200）
X-Flow-Id: 4444444444444444444
X-Execution-Id: 1234567890123456792
X-Status: 0
X-Duration-Ms: 156
X-Code: 200
X-Message-Zh: 成功
X-Message-En: Success
X-Request-Id: 1234567890123456792      ← 用户自定义响应头（来自出口节点 output.header）

{"result": "ok"}
```

---

### 3.9a 系统配置（#54~#55）— open-server（复用 market-server Property）

#### open-server — 系统配置（复用 market-server Property）

| # | 方法 | 路径 | 说明 |
|---|------|------|------|
| 54 | GET | /service/open/v2/app-config/{appId} | 查询应用级系统配置（超时上限/限流上限/记录条数上限/日志开关） |
| 55 | PUT | /service/open/v2/app-config/{appId} | 更新应用级系统配置 |

---

### 3.10 操作日志查询（FR-046，复用现有模块）

> 💡 FR-046 要求的操作日志查询复用应用现有 `OperateLog` 模块，不新增专用端点。前端通过以下现有接口查询：

| 资源 | 复用接口 | 说明 |
|------|---------|------|
| 连接器 | `GET /service/open/v2/operate-logs?targetType=connector&targetId={connectorId}` | 现有接口，按 targetType 过滤 |
| 连接流 | `GET /service/open/v2/operate-logs?targetType=flow&targetId={flowId}` | 现有接口，按 targetType 过滤 |

支持分页参数 `curPage` / `pageSize`。日志内容包含：操作人、操作时间、操作类型、变更前后快照。V3 扩展的 `OperateEnum` 操作类型（创建、编辑、删除、恢复、发布、失效、部署、启动、停止、复制、提交审批、审批通过、审批驳回、撤回审批等）由后端自动记录，前端无需感知。

---

## 附录：修订记录

| 版本 | 日期 | 修订内容 | 修订人 |
|------|------|---------|--------|
| v1.0 | 2026-06-09 | 初始版本 — 对齐 spec.md v2.15，端点 45 个，6 个示例 | SDDU Plan Agent |
| v4.0 | 2026-06-09 | **路径语义化 + 调试代理补全**：① 占位符统一命名（{id}→{connectorId}/{flowId}，{vid}→{versionId}）<br>② 调试拆为 open-server 代理（#43）+ connector-api 执行（#45）双接口<br>③ invoke 路径改为 `/api/v1/flows/{flowId}/invoke` | SDDU Plan Agent |
| v5.0 | 2026-06-10 | **§3 全章重写，严格对齐 §2 接口清单**：① URL 白名单从独立端点归入 #10/#11 的 `connectionConfig.urlWhitelist` 字段<br>② §3.3~§3.8 编号统一对齐 §2（连接流 CRUD #17~#27、版本 #29~#36、运行记录 #37~#38、审批 #39~#42、调试 #43、运行时 #44~#45）<br>③ 删除旧 §3.3（独立 URL 白名单端点）<br>④ 新增 §3.9 操作日志查询复用说明<br>⑤ 补 #32 审批提交的「编排为空」422 错误响应<br>⑥ §0 服务归属修正为 41+2 | SDDU Plan Agent |
| v5.2 | 2026-06-10 | **新增 status 过滤参数**：① #2 GET `/connectors` 新增 `?status=2` 过滤有效可用连接器（编排画布选连接器）<br>② #9 GET `/connectors/{connectorId}/versions` 新增 `?status=2` 过滤已发布版本（编排画布选版本）<br>③ #29 GET `/flows/{flowId}/versions` 新增 `?status=5` 过滤已发布版本（部署选版本）<br>以上均为复用现有接口扩展参数，零新增端点 | SDDU Plan Agent |
| v5.3 | 2026-06-10 | **§3 全文补字段定义表**：① 41 个接口全部补请求头/路径参数/查询参数/请求体/响应体/错误响应字段表<br>② 嵌套对象展开到叶子字段（connectionConfig、orchestrationConfig、steps[] 等）<br>③ §3 标题精简为 `#N 名称` + `` `METHOD /path` `` 独立行<br>④ 时间格式统一 `yyyy-MM-dd HH:mm:ss`，appId 归入请求头<br>⑤ §1.9 接口命名规范：`[操作动词][资源名词][强调词]` | SDDU Plan Agent |
| v5.4 | 2026-06-10 | **补全 JSON 示例**：22 个缺失示例的接口全部补回（#20~#29、#31~#43），每个接口含请求/响应/错误完整示例 | SDDU Plan Agent |
| v5.5 | 2026-06-10 | **补全审批域接口**：① §2 新增「审批记录」（#39~#44，扩展 businessType）和「审批流模板配置」（#45~#48，新增 appId 字段）两个分组，共 10 个接口<br>② 端点从 41→51 重新编号（#39~#53），V1 已删除接口在表中独立成行不占编号<br>③ 改动点列恢复 ①②③④⑤⑥ 编号格式，与 V3 变更列（新增/改造/删除）独立<br>④ §3 新增 §3.5（#39~#44）、§3.6（#45~#48），§3.5→§3.7~§3.10 顺延重新编号<br>⑤ 跨引用编号同步更新（§0/#51/#52 等）<br>⑥ plan.md 接口数同步更新为 49+2 | SDDU Plan Agent |
| v7.0 | 2026-06-16 | **#54 透明穿透模式重构**：① 请求侧完全遵循触发器节点 `httpInputDef`（header/query/body 均由用户自定义）<br>② 响应侧完全遵循出口节点 `output`（httpOutputDef header/body 均由用户自定义），不使用标准响应信封<br>③ 平台元数据（executionId/status/durationMs/code/message/cacheStatus）统一追加到 `X-` 前缀响应头<br>④ §1.5 新增 #54 例外说明<br>⑤ §2 接口清单 #54 行新增透明穿透标注<br>⑥ 新增 ADR-008 记录设计决策 | SDDU Plan Agent |
| v8.2 | 2026-06-25 | **#49~#50 运行记录接口设计**：① #49 列表 `GET /executions?keyword=&flowId=&status=&triggerType=&startTime=&endTime=`，keyword 模糊匹配 flowNameCn/flowNameEn ② #50 详情 `GET /executions/{executionId}` | Summer |
| v6.2 | 2026-06-15 | **接口重新编号**：① 创建草稿端点补编号 #8（连接器）、#28（连接流）② 全表 #8~#51 依次顺延 +1/+2 ③ §3 章标题范围同步 ④ 全文 `#N` 交叉引用同步更新 | SDDU Plan Agent |
| v6.1 | 2026-06-15 | **对齐 spec v2.22**：① §0 对齐基线更新为 spec v2.22 + plan-json-schema v9.10 ② errorHandler 字段描述从 successCondition+failureResponse 更新为策略模型（retry/ignore/terminate + errorTypes + retryConfig）③ #1 创建连接器不再自动生成草稿（FR-005a）④ #17 创建连接流不再自动生成草稿（FR-024a）⑤ 新增 `POST /connectors/{connectorId}/versions` 创建连接器草稿端点 ⑥ 新增 `POST /flows/{flowId}/versions` 创建连接流草稿端点 ⑦ #16 删除连接器版本补充草稿可直接删除 ⑧ #36 删除连接流版本补充草稿/已撤回/已驳回可直接删除 ⑨ 端点总数 51→53 | SDDU Plan Agent |
