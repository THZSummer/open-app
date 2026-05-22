# API 接口设计：连接器平台

**Feature ID**: CONN-PLAT-001  
**关联文档**: plan.md (§4.4), plan-db.md (§3 表结构)  
**版本**: v2.7.6  
**创建日期**: 2026-05-21  
**最后更新**: 2026-05-22  
**对齐基线**: plan.md v2.7.6（含 v2.0 → v2.7.6 全部决策）

---

## 0. 版本对齐说明

| 维度 | v2.0 | v2.7.5 | 决策来源 |
|------|------|--------|---------|
| 执行接口 | 异步（202 + 轮询） | **同步**（直接返回执行结果） | v2.0 |
| 触发方式 | 事件/Webhook/定时/手动 | **HTTP / 手动 / 测试** | v2.0 |
| Scope 权限 | ✅ | ❌ 移除（NG18，V1） | v2.0 |
| 审批集成 | ✅ | ❌ 移除（NG19，V1） | v2.0 |
| **ID 表示** | `varchar(32)` 业务前缀（如 `con_xxxx`/`flow_xxxx`） | **BIGINT 雪花 ID → API 响应转 string**（如 `"1234567890123456789"`） | v2.7（数据库主键变更）+ 用户决策 |
| **枚举对外** | 字符串字面量（如 `"success"`/`"http"`） | **TINYINT 数字**（如 `0/1/2/3`，与数据库一致） | 用户决策 |
| **名称字段** | `name` 单语 | **`nameCn` / `nameEn` 双语** | v2.7 |
| **描述字段** | `description` 单语 | **`descriptionCn` / `descriptionEn` 双语 VARCHAR(1000)** | v2.7 + v2.7.2 |
| **凭证传递** | 持久化在 `cp_connector_auth_config` 表 | **不持久化**——调用方在请求 Header/Body 携带；服务端按 `auth_type_schema.sensitive` 标记脱敏 | v2.6 |
| **触发器 URL** | `/trigger/{flowId}/{triggerToken}` | **`/trigger/{flowId}/invoke`**（flowId 雪花数字 string，凭证由调用方携带） | v2.7.3 |
| **HTTP 触发认证** | 平台维护 signing_secret 签名验证 | **凭证调用方携带，平台仅按 `trigger.authTypeSchema` 校验格式** | v2.6 + v2.7.3 |
| FR 引用 | ~37 | **25** | v2.0 |

---

## 1. 设计规范

> 💡 以下 API 设计规范沿用能力开放平台（CAP-OPEN-001）已确立的标准，确保全项目 API 风格统一。详情见 `../specs-tree-capability-open-platform/plan-api.md §0`。

### 1.1 基础规范

| 规范项 | 说明 |
|--------|------|
| 基础路径 | `/api/v1` |
| 认证方式 | 管理面复用现有 Cookie/SSO；执行面 HTTP 触发通过签名验证 |
| 时间格式 | ISO 8601: `yyyy-MM-dd'T'HH:mm:ss.SSSXXX` |

### 1.2 字段命名规范

**规则**：接口入参和返回值字段统一使用驼峰命名（camelCase）。

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| `connectorId` | `connector_id` |
| `createTime` | `create_Time` |
| `versionStatus` | `version_status` |
| `nameCn` / `nameEn` | `name_cn` / `name_en` |
| `currentPublishedVersionId` | `current_published_version_id` |

**命名约定**：
- ID 字段：使用 `Id` 后缀，如 `connectorId`, `flowId`, `versionId`, `executionId`, `stepId`, `blobId`
- 时间字段：使用 `Time` 后缀，如 `createTime`, `lastUpdateTime`, `startedTime`, `completedTime`, `publishedTime`
- 布尔字段：使用 `is` 前缀，如 `isDeleted`, `isTest`
- URL 字段：使用 `Url` 后缀，如 `iconUrl`
- **双语字段**：使用 `Cn`/`En` 后缀，如 `nameCn`/`nameEn`, `descriptionCn`/`descriptionEn`, `labelCn`/`labelEn`, `versionDescriptionCn`/`versionDescriptionEn`

**数据库 snake_case → API camelCase 映射**：

| 数据库列名 | API 字段名 |
|-----------|----------|
| `name_cn` | `nameCn` |
| `description_en` | `descriptionEn` |
| `connector_version_id` | `connectorVersionId` |
| `current_published_version_id` | `currentPublishedVersionId` |
| `external_resource_id` | `externalResourceId` |
| `trigger_data_blob_id` | `triggerDataBlobId` |
| `operations_count` / `data_in_bytes` / `data_out_bytes` | `operationsCount` / `dataInBytes` / `dataOutBytes` |

### 1.3 路径命名规范

**规则**：URL 路径使用中划线分隔多个单词（kebab-case）。

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| `/api/v1/connector-versions` | `/api/v1/connector_versions` |
| `/api/v1/test-run` | `/api/v1/testRun` |

**命名约定**：
- 资源名称使用复数形式：`/connectors`, `/flows`, `/executions`
- 子资源使用中划线分隔：`/test-run`, `/connector-versions`
- 路径参数使用驼峰：`/connectors/:connectorId/versions`

### 1.4 数据类型规范

**规则**：

1. **长整数（BIGINT 雪花 ID）统一返回 string 类型**，避免前端接收精度丢失问题（v2.7 数据库主键变更为 BIGINT 雪花 ID 后必须遵守）
2. **枚举字段统一返回 TINYINT 数字**，与数据库存储一致；前端维护数字 ↔ 标签映射字典（用户决策）
3. **时间字段**返回 ISO 8601 字符串（含毫秒 + 时区）

| ✅ 正确示例 | ❌ 错误示例 | 说明 |
|------------|------------|------|
| `"connectorId": "1234567890123456789"` | `"connectorId": 1234567890123456789` | BIGINT 必须转 string |
| `"status": 2` | `"status": "success"` | 枚举用数字（参考 plan-db.md §2.6 枚举字典） |
| `"triggerType": 1` | `"triggerType": "http"` | 同上 |
| `"createTime": "2026-05-21T10:00:00.000+08:00"` | `"createTime": 1716264000000` | 时间用 ISO 8601 |

**适用范围**（ID 字段必须返回 string）：
- 所有主键 ID：`id`, `connectorId`, `flowId`, `versionId`, `executionId`, `stepId`, `blobId`
- 所有外键 ID：`connectorVersionId`, `flowVersionId`, `currentPublishedVersionId`, `triggerDataBlobId`, `resultDataBlobId`, `inputDataBlobId`, `outputDataBlobId`

**枚举数字字典**（前端需维护，详见 plan-db.md §2.6）：

| 字段 | 数字 → 含义 |
|------|------------|
| `connector_t.status` | `0=disabled, 1=active` |
| `connector_t.connectorType` | `1=HTTP`（MVP） |
| `connectorVersion.versionStatus` / `flowVersion.versionStatus` | `0=draft, 1=published` |
| `flow_t.lifecycleStatus` | `0=stopped, 1=running` |
| `executionRecord.status` | `0=pending, 1=running, 2=success, 3=failed, 4=timeout` |
| `executionRecord.triggerType` | `1=http, 2=manual, 3=test` |
| `executionStep.status` | `0=success, 1=failed` |
| `executionStep.nodeType` | `1=entry, 2=connector, 3=data_processor, 4=exit` |
| `storageBlobRef.ownerType` | `1=executionRecordTrigger, 2=executionRecordResult, 3=executionStepInput, 4=executionStepOutput` |

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
| `400` | 参数错误 |
| `401` | 未授权 |
| `403` | 无权限 |
| `404` | 资源不存在 |
| `409` | 状态冲突 |
| `422` | 校验失败 |
| `429` | 触发频率超限（FR-024 默认限流） |
| `500` | 内部错误 |

---

### 1.8 状态枚举定义（对外 API 形式：TINYINT 数字）

> 💡 对外 API 返回的枚举值统一为 TINYINT 数字，与数据库存储一致；前端维护数字 → 标签映射字典。详细枚举字典见 plan-db.md §2.6。

#### 1.8.1 执行状态 (executionRecord.status)

| 数字 | 含义 | 使用场景 |
|:----:|------|---------|
| `0` | `pending` | 同步执行中刚创建记录、节点未开始（瞬时） |
| `1` | `running` | 同步执行中节点正在执行（瞬时，超时强制终止时回填） |
| `2` | `success` | 所有节点执行成功，正常返回 |
| `3` | `failed` | 某个节点执行失败（FR-023 默认错误处理） |
| `4` | `timeout` | 执行超过配置的超时时间，强制终止 |

> **与 v2.0 的差异**：v2.0 仅 3 个值（success/failed/timeout），v2.7.5 扩为 5 个值——**保留 pending/running 用于同步执行过程中的瞬时状态写入与回填**（如执行超时时上下文需记录"running 中超时"，便于排查）

#### 1.8.2 执行步骤状态 (executionStep.status)

| 数字 | 含义 |
|:----:|------|
| `0` | success（步骤执行成功） |
| `1` | failed（步骤执行失败） |

#### 1.8.3 触发方式 (triggerType)

| 数字 | 含义 | 说明 | MVP |
|:----:|------|------|:---:|
| `1` | `http` | HTTP 触发（FR-021） | ✅ |
| `2` | `manual` | 手动触发（FR-022） | ✅ |
| `3` | `test` | 测试运行（FR-020） | ✅ |

> **V1 扩展**：4=event / 5=webhook / 6=scheduled（V1 阶段引入，NG14/NG15/NG17）

#### 1.8.4 版本状态 (versionStatus)

| 数字 | 含义 |
|:----:|------|
| `0` | draft（草稿版本，可编辑） |
| `1` | published（已发布版本，只读，可被引用） |

#### 1.8.5 连接器协议类型 (connectorType)

| 数字 | 含义 | MVP |
|:----:|------|:---:|
| `1` | HTTP | ✅ |

> **V1 扩展**：2=MySQL / 3=Redis / 4=Kafka / 5=gRPC 等（NG12，V1 阶段）

#### 1.8.6 节点类型 (nodeType)

| 数字 | 含义 | MVP |
|:----:|------|:---:|
| `1` | entry（入口节点） | ✅ |
| `2` | connector（连接器节点） | ✅ |
| `3` | data_processor（数据处理节点） | ✅ |
| `4` | exit（出口节点） | ✅ |

#### 1.8.7 连接器状态 (connector.status)

| 数字 | 含义 |
|:----:|------|
| `0` | disabled（禁用） |
| `1` | active（启用） |

#### 1.8.8 连接流生命周期 (flow.lifecycleStatus)

| 数字 | 含义 |
|:----:|------|
| `0` | undeployed（未部署，`currentPublishedVersionId` 为空） |
| `1` | running（运行中，可接收 HTTP 触发） |
| `2` | stopped（已停止，挂起，HTTP 触发返回 403） |

> **流转说明**：`undeployed → running`（部署 FR-013）、`running → stopped`（停止 FR-015）、`stopped → running`（启动 FR-014）。当前不提供从 running/stopped 回到 undeployed 的 API（路径预留）。

#### 1.8.9 对象存储归属类型 (storageBlobRef.ownerType)

| 数字 | 含义 |
|:----:|------|
| `1` | executionRecordTrigger（执行记录的触发数据外置） |
| `2` | executionRecordResult（执行记录的返回值外置） |
| `3` | executionStepInput（执行步骤的输入数据外置） |
| `4` | executionStepOutput（执行步骤的输出数据外置） |

---

## 2. 接口清单

| # | 服务 | 模块 | Method | Path | 说明 | FR |
|---|------|------|--------|------|------|-----|
| 1 | **open-server** | **连接器管理** | POST | `/api/v1/connectors` | 创建连接器 | FR-001 |
| 2 | | | GET | `/api/v1/connectors` | 查询连接器列表 | FR-004 |
| 3 | | | GET | `/api/v1/connectors/{connectorId}` | 查询连接器详情 | FR-004 |
| 4 | | | PUT | `/api/v1/connectors/{connectorId}` | 更新连接器基本信息 | FR-002 |
| 5 | | | DELETE | `/api/v1/connectors/{connectorId}` | 删除连接器 | FR-003 |
| 6 | | **连接器版本** | GET | `/api/v1/connectors/{connectorId}/versions` | 获取连接器版本列表 | FR-007 |
| 7 | | | GET | `/api/v1/connectors/{connectorId}/versions/{versionId}` | 获取连接器版本详情（含连接配置） | FR-005 |
| 8 | | | PUT | `/api/v1/connectors/{connectorId}/versions/{versionId}` | 编辑草稿版本配置 | FR-006 |
| 9 | | | POST | `/api/v1/connectors/{connectorId}/versions/{versionId}/publish` | 发布连接器版本 | FR-008 |
| 10 | | **连接流管理** | POST | `/api/v1/flows` | 创建连接流 | FR-009 |
| 11 | | | GET | `/api/v1/flows` | 查询连接流列表 | FR-012 |
| 12 | | | GET | `/api/v1/flows/{flowId}` | 查询连接流详情 | FR-016 |
| 13 | | | PUT | `/api/v1/flows/{flowId}` | 更新连接流基本信息 | FR-010 |
| 14 | | | DELETE | `/api/v1/flows/{flowId}` | 删除连接流 | FR-011 |
| 15 | | | POST | `/api/v1/flows/{flowId}/deploy` | 部署连接流（undeployed → running 或热切换 currentPublishedVersionId） | FR-013 |
| 16 | | | POST | `/api/v1/flows/{flowId}/start` | 启动连接流（stopped → running） | FR-014 |
| 17 | | | POST | `/api/v1/flows/{flowId}/stop` | 停止连接流（running → stopped，保留指针） | FR-015 |
| 18 | | **连接流版本** | GET | `/api/v1/flows/{flowId}/versions` | 获取连接流版本列表 | FR-018 |
| 19 | | | GET | `/api/v1/flows/{flowId}/versions/{versionId}` | 获取连接流版本详情（含编排配置） | FR-016 |
| 20 | | | PUT | `/api/v1/flows/{flowId}/versions/{versionId}` | 保存编排配置（草稿） | FR-017 |
| 21 | | | POST | `/api/v1/flows/{flowId}/versions/{versionId}/publish` | 发布连接流版本 | FR-019 |
| 22 | | **调试代理**<br/>（转发 connector-api debug-api） | POST | `/api/v1/flows/{flowId}/executions` | 手动触发执行（同步） | FR-022 |
| 23 | | | POST | `/api/v1/flows/{flowId}/test-run` | 测试运行（同步） | FR-020 |
| 24 | **connector-api** | **HTTP 触发**<br/>（对外消费方直连） | POST | `/api/v1/trigger/{flowId}/invoke` | HTTP 触发连接流（同步执行，返回结果） | FR-021 |
| 25 | **open-server** | **监控查询** | GET | `/api/v1/flows/{flowId}/executions` | 获取执行历史列表 | FR-025 |
| 26 | | | GET | `/api/v1/executions/{executionId}` | 获取执行详情（含步骤） | FR-025 |

> **总计**：26 个 HTTP 端点，覆盖 25 个 FR（FR-025 对应 2 个端点：执行列表 + 执行详情）
>
> **服务部署归属**（plan.md v2.0 修订后）：
>
> | 服务 | 端口 | 上下文根 | 接口数 | 接口范围 |
> |------|------|---------|:------:|---------|
> | **open-server**（管理类） | 18080 | `/open-server` | 25 | #1~#23 管理类 + 调试代理（转发 connector-api）<br/>#25~#26 监控查询 |
> | **connector-api**（运行时） | 18180 | `/connector-api` | 1（对外） | #24 HTTP 触发（对外消费方直连）<br/>+ 内部 debug-api 接口（被 #22/#23 转发调用，端点详见 §3.5 后续补充） |
>
> **路径前缀**：表中 Path 列省略了服务上下文根；完整 URL = `服务部署域名` + `上下文根` + Path（如 #1 完整路径为 `https://open-server.example.com/open-server/api/v1/connectors`）。
>
> **与 v1.x 的差异**：从 ~33 个端点精简为 26 个，移除审批集成（3 个）、Scope 集成（1 个）、MQS 主题（4 个）、监控仪表盘（2 个）、事件/定时/Webhook 触发接口（3 个）；同时**新增 1 条内部调试接口**（connector-api 暴露给 open-server 内网调用）

---

## 3. 接口详细定义

> 💡 接口清单见 §2，本章为每个接口的请求/响应详细定义。所有接口的字段命名、数据类型、响应格式、状态枚举均遵循 §1 设计规范。



### 3.1 连接器 CRUD


#### #1 POST /api/v1/connectors — 创建连接器

```json
// Request
{
  "nameCn": "IM 发送消息",
  "nameEn": "IM Send Message",
  "iconUrl": "https://cdn.xxx.com/icons/im.svg",
  "descriptionCn": "封装 IM 消息发送能力",
  "descriptionEn": "Encapsulated IM messaging capability",
  "connectorType": 1
}

// Response 201
{
  "code": "200",
  "messageZh": "创建成功",
  "messageEn": "Created",
  "data": {
    "connectorId": "1234567890123456789",
    "nameCn": "IM 发送消息",
    "nameEn": "IM Send Message",
    "connectorType": 1,
    "status": 1,
    "createTime": "2026-05-21T10:00:00.000+08:00"
  }
}
// 注意：创建后仅生成连接器基本信息，不自动生成草稿版本（需用户主动创建版本）
```

#### #2 GET /api/v1/connectors — 查询列表

```json
// Query params: ?curPage=1&pageSize=20&connectorType=1&keyword=IM

// Response
{
  "code": "200",
  "messageZh": "查询成功",
  "messageEn": "Success",
  "data": [
    {
      "connectorId": "1234567890123456789",
      "nameCn": "IM 发送消息",
      "nameEn": "IM Send Message",
      "iconUrl": "https://cdn.xxx.com/icons/im.svg",
      "descriptionCn": "封装 IM 消息发送能力",
      "descriptionEn": "Encapsulated IM messaging capability",
      "connectorType": 1,
      "status": 1,
      "latestVersionNo": "1.2.0",
      "latestVersionStatus": 1,
      "createTime": "2026-05-21T10:00:00.000+08:00"
    }
  ],
  "page": {
    "curPage": 1,
    "pageSize": 20,
    "total": 1
  }
}
```

---

#### #3 GET /api/v1/connectors/{connectorId} — 查询连接器详情

**响应示例**：

```json
{
  "code": "200",
  "messageZh": "操作成功",
  "messageEn": "Success",
  "data": {
    "connectorId": "1234567890123456789",
    "nameCn": "IM 发送消息",
    "nameEn": "IM Send Message",
    "iconUrl": "https://cdn.xxx.com/icons/im.svg",
    "descriptionCn": "封装 IM 消息发送能力",
    "descriptionEn": "Encapsulated IM messaging capability",
    "connectorType": 1,
    "status": 1,
    "createTime": "2026-05-21T10:00:00.000+08:00",
    "lastUpdateTime": "2026-05-21T11:00:00.000+08:00"
  },
  "page": null
}
```

---

#### #4 PUT /api/v1/connectors/{connectorId} — 更新连接器基本信息

```json
// Request
{
  "nameCn": "IM 发送消息（新版）",
  "nameEn": "IM Send Message (New)",
  "descriptionCn": "更新后的 IM 消息发送能力",
  "descriptionEn": "Updated IM messaging capability",
  "iconUrl": "https://cdn.xxx.com/icons/im-v2.svg",
  "connectorType": 1
}

// Response 200
{
  "code": "200",
  "messageZh": "更新成功",
  "messageEn": "Success",
  "data": {
    "connectorId": "1234567890123456789",
    "nameCn": "IM 发送消息（新版）",
    "lastUpdateTime": "2026-05-21T12:00:00.000+08:00"
  },
  "page": null
}
```

---

#### #5 DELETE /api/v1/connectors/{connectorId} — 删除连接器

> **说明**：物理删除。删除前检查是否存在已发布的版本（版本快照不可变，已发布的版本即使连接器删除仍可在已有的部署连接流中执行）。已发布的版本会标记 orphan。

**响应示例（成功）**：

```json
{
  "code": "200",
  "messageZh": "连接器删除成功",
  "messageEn": "Success",
  "data": null,
  "page": null
}
```

---

### 3.2 连接器版本管理

---

#### #6 GET /api/v1/connectors/{connectorId}/versions — 获取版本列表

```json
// Query params: ?curPage=1&pageSize=20

// Response 200
{
  "code": "200",
  "messageZh": "查询成功",
  "messageEn": "Success",
  "data": [
    {
      "versionId": "9876543210123456789",
      "versionNo": "1.0.0",
      "versionStatus": 1,
      "versionDescriptionCn": "初始版本",
      "versionDescriptionEn": "Initial version",
      "publishedTime": "2026-05-21T10:00:00.000+08:00",
      "createTime": "2026-05-21T09:00:00.000+08:00"
    },
    {
      "versionId": "9876543210123456790",
      "versionNo": "0.0.1",
      "versionStatus": 0,
      "createTime": "2026-05-21T08:00:00.000+08:00"
    }
  ],
  "page": {
    "curPage": 1,
    "pageSize": 20,
    "total": 2
  }
}
```

---

#### #7 GET /api/v1/connectors/{connectorId}/versions/{versionId} — 获取版本详情（含连接配置）

```json
// Response 200
{
  "code": "200",
  "messageZh": "操作成功",
  "messageEn": "Success",
  "data": {
    "versionId": "9876543210123456789",
    "versionNo": "1.0.0",
    "versionStatus": 1,
    "versionDescriptionCn": "初始版本",
    "versionDescriptionEn": "Initial version",
    "basicInfoSnapshot": {
      "nameCn": "IM 发送消息",
      "nameEn": "IM Send Message",
      "descriptionCn": "封装 IM 消息发送能力",
      "descriptionEn": "Encapsulated IM messaging capability",
      "iconUrl": "https://cdn.xxx.com/icons/im.svg",
      "connectorType": 1
    },
    "connectionConfig": {
      "protocol": "HTTP",
      "protocolConfig": {
        "url": "https://openapi.xxx.com/im/send",
        "method": "POST",
        "headers": { "Content-Type": "application/json" }
      },
      "authTypeSchema": {
        "type": "AKSK",
        "carrier": "header",
        "fields": [
          { "name": "accessKey", "required": true, "sensitive": true },
          { "name": "secretKey", "required": true, "sensitive": true }
        ]
      },
      "timeoutMs": 30000,
      "rateLimit": { "maxPerSecond": 10, "maxConcurrent": 5 }
    },
    "publishedTime": "2026-05-21T10:00:00.000+08:00",
    "createTime": "2026-05-21T09:00:00.000+08:00"
  },
  "page": null
}
```

---

#### #8 PUT /api/v1/connectors/{connectorId}/versions/{versionId} — 编辑连接配置

> ⚠️ **凭证不持久化**（v2.6 决策）：`connectionConfig.authTypeSchema` 仅声明认证类型与字段 schema（含 `sensitive: true` 标记），**不接受任何凭证值**；凭证由调用方在触发请求时携带，运行时注入 ExecutionContext（仅内存生命周期）。

```json
// Request — connectionConfig 全文替换
{
  "connectionConfig": {
    "protocol": "HTTP",
    "protocolConfig": {
      "url": "https://openapi.xxx.com/im/send",
      "method": "POST",
      "headers": { "Content-Type": "application/json" }
    },
    "authTypeSchema": {
      "type": "AKSK",
      "carrier": "header",
      "fields": [
        { "name": "accessKey", "required": true, "sensitive": true },
        { "name": "secretKey", "required": true, "sensitive": true }
      ]
    },
    "inputSchema": {
      "type": "object",
      "properties": {
        "receiver": { "type": "string", "description": "接收者ID" },
        "content": { "type": "string", "description": "消息内容" }
      },
      "required": ["receiver", "content"]
    },
    "outputSchema": {
      "type": "object",
      "properties": {
        "msgId": { "type": "string", "description": "消息ID" }
      }
    },
    "timeoutMs": 30000,
    "rateLimit": {
      "maxPerSecond": 10,
      "maxConcurrent": 5
    }
  }
}

// Response 200
{
  "code": "200",
  "messageZh": "保存成功",
  "messageEn": "Saved",
  "data": {
    "versionId": "9876543210123456789",
    "versionStatus": 0,
    "lastUpdateTime": "2026-05-21T10:00:00.000+08:00"
  }
}
```

**authTypeSchema.type 枚举**（仅声明类型，凭证值由调用方携带）：

| 类型 | 说明 | 调用方需携带的字段 |
|------|------|------------------|
| `NONE` | 无需认证 | — |
| `AKSK` | AccessKey/SecretKey | `accessKey`, `secretKey` |
| `OAUTH2_CLIENT` | OAuth2 Client Credentials | `accessToken`（调用方完成 OAuth2 授权流后传入） |
| `BASIC_AUTH` | HTTP Basic Auth | `username`, `password` |
| `API_KEY` | API Key (header/query) | `keyValue`（位置由 schema 的 `carrier`/`fieldName` 声明） |
| `BEARER` | Bearer Token | `token` |

#### #9 POST /api/v1/connectors/{connectorId}/versions/{versionId}/publish — 发布

```json
// Request
{
  "versionNo": "1.0.0",
  "versionDescriptionCn": "初始版本，支持文本消息发送",
  "versionDescriptionEn": "Initial version, supports text messaging"
}

// Response 200
{
  "code": "200",
  "messageZh": "发布成功",
  "messageEn": "Published",
  "data": {
    "versionId": "9876543210123456789",
    "versionNo": "1.0.0",
    "versionStatus": 1,
    "publishedTime": "2026-05-21T10:00:00.000+08:00"
  }
}
// 注意：本版本发布无需审批（NG19，V1 阶段引入）
```

**auth_type 枚举（已合并到 §"PUT 编辑连接配置"小节，本节不再重复）**

> ❌ **不再需要的 API**（v2.0 → v2.7.5）：~~`POST /connectors/{id}/versions/{vid}/credentials`~~ —— 凭证不持久化（v2.6 决策），无凭证创建/更新接口；调用方在触发请求时携带

---



### 3.3 连接流 CRUD


#### #10 POST /api/v1/flows — 创建连接流

```json
// Request
{
  "nameCn": "新消息自动通知",
  "nameEn": "Auto Message Notification",
  "descriptionCn": "收到 IM 消息后自动发送通知到OA系统",
  "descriptionEn": "Auto notify OA system upon receiving IM messages"
}

// Response 201
{
  "code": "200",
  "messageZh": "创建成功",
  "messageEn": "Created",
  "data": {
    "flowId": "1234567890123456789",
    "nameCn": "新消息自动通知",
    "nameEn": "Auto Message Notification",
    "lifecycleStatus": 0,
    "createTime": "2026-05-21T10:00:00.000+08:00"
  }
}
// 注意：创建后仅生成连接流基本信息，不自动生成草稿版本（需用户主动创建版本）
```

---

#### #11 GET /api/v1/flows — 查询连接流列表

```json
// Query params: ?curPage=1&pageSize=20&lifecycleStatus=1&keyword=通知

// Response 200
{
  "code": "200",
  "messageZh": "查询成功",
  "messageEn": "Success",
  "data": [
    {
      "flowId": "1234567890123456789",
      "nameCn": "新消息自动通知",
      "nameEn": "Auto Message Notification",
      "descriptionCn": "收到 IM 消息后自动发送通知到OA系统",
      "descriptionEn": "Auto notify OA system upon receiving IM messages",
      "lifecycleStatus": 1,
      "currentPublishedVersionId": "9876543210123456789",
      "latestVersionNo": "1.0.0",
      "createTime": "2026-05-21T10:00:00.000+08:00"
    }
  ],
  "page": {
    "curPage": 1,
    "pageSize": 20,
    "total": 1
  }
}
```

---

#### #12 GET /api/v1/flows/{flowId} — 查询连接流详情

**响应示例**：

```json
{
  "code": "200",
  "messageZh": "操作成功",
  "messageEn": "Success",
  "data": {
    "flowId": "1234567890123456789",
    "nameCn": "新消息自动通知",
    "nameEn": "Auto Message Notification",
    "descriptionCn": "收到 IM 消息后自动发送通知到OA系统",
    "descriptionEn": "Auto notify OA system upon receiving IM messages",
    "lifecycleStatus": 1,
    "currentPublishedVersionId": "9876543210123456789",
    "createTime": "2026-05-21T10:00:00.000+08:00",
    "lastUpdateTime": "2026-05-21T11:00:00.000+08:00"
  },
  "page": null
}
```

---

#### #13 PUT /api/v1/flows/{flowId} — 更新连接流基本信息

```json
// Request
{
  "nameCn": "新消息自动通知（新版）",
  "nameEn": "Auto Message Notification (New)",
  "descriptionCn": "更新后的通知流描述",
  "descriptionEn": "Updated notification flow description"
}

// Response 200
{
  "code": "200",
  "messageZh": "更新成功",
  "messageEn": "Success",
  "data": {
    "flowId": "1234567890123456789",
    "nameCn": "新消息自动通知（新版）",
    "lastUpdateTime": "2026-05-21T12:00:00.000+08:00"
  },
  "page": null
}
```

---

#### #14 DELETE /api/v1/flows/{flowId} — 删除连接流

> **说明**：物理删除。删除前检查是否处于 running 状态（需先 stop）。级联删除关联的所有版本与执行记录（应用层事务保证）。

**响应示例（成功）**：

```json
{
  "code": "200",
  "messageZh": "连接流删除成功",
  "messageEn": "Success",
  "data": null,
  "page": null
}
```

---

#### #15 POST /api/v1/flows/{flowId}/deploy — 部署

```json
// Request
{
  "versionId": "9876543210123456789"
}

// Response 200
{
  "code": "200",
  "messageZh": "部署成功",
  "messageEn": "Deployed",
  "data": {
    "flowId": "1234567890123456789",
    "currentPublishedVersionId": "9876543210123456789",
    "lifecycleStatus": 1,
    "lastUpdateTime": "2026-05-21T10:00:00.000+08:00"
  }
}
// 注意：部署无需审批（NG19，V1 阶段引入）
// 部署即更新 flow_t.current_published_version_id 指针，HTTP 触发即生效
// 若当前状态为 undeployed(0)，部署同时切换到 running(1)；若已是 running(1) 或 stopped(2)，
// 部署新版本仅热切换指针，状态保持不变
```

---

#### #16 POST /api/v1/flows/{flowId}/start — 启动连接流

> **说明**：将连接流 lifecycleStatus 从 `stopped(2)` 切换为 `running(1)`（FR-014）。需 `currentPublishedVersionId` 非空（`undeployed(0)` 状态需先 #15 部署）。启动后该流可接收 HTTP 触发请求。

**响应示例**：

```json
{
  "code": "200",
  "messageZh": "启动成功",
  "messageEn": "Success",
  "data": {
    "flowId": "1234567890123456789",
    "lifecycleStatus": 1,
    "lastUpdateTime": "2026-05-21T12:00:00.000+08:00"
  },
  "page": null
}
```

---

#### #17 POST /api/v1/flows/{flowId}/stop — 停止连接流

> **说明**：将连接流 lifecycleStatus 从 `running(1)` 切换为 `stopped(2)`（FR-015）。`currentPublishedVersionId` 指针保留（不删除），可后续启动（#16）恢复运行。停止后 HTTP 触发入口返回 403。

**响应示例**：

```json
{
  "code": "200",
  "messageZh": "停止成功",
  "messageEn": "Success",
  "data": {
    "flowId": "1234567890123456789",
    "lifecycleStatus": 0,
    "lastUpdateTime": "2026-05-21T12:05:00.000+08:00"
  },
  "page": null
}
```

---

### 3.4 连接流版本管理

---

#### #18 GET /api/v1/flows/{flowId}/versions — 获取版本列表

```json
// Query params: ?curPage=1&pageSize=20

// Response 200
{
  "code": "200",
  "messageZh": "查询成功",
  "messageEn": "Success",
  "data": [
    {
      "versionId": "9876543210123456789",
      "versionNo": "1.0.0",
      "versionStatus": 1,
      "versionDescriptionCn": "初始版本",
      "versionDescriptionEn": "Initial version",
      "publishedTime": "2026-05-21T10:00:00.000+08:00",
      "createTime": "2026-05-21T09:00:00.000+08:00"
    }
  ],
  "page": {
    "curPage": 1,
    "pageSize": 20,
    "total": 1
  }
}
```

---

#### #19 GET /api/v1/flows/{flowId}/versions/{versionId} — 获取版本详情（含编排配置）

```json
// Response 200
{
  "code": "200",
  "messageZh": "操作成功",
  "messageEn": "Success",
  "data": {
    "versionId": "9876543210123456789",
    "versionNo": "1.0.0",
    "versionStatus": 1,
    "versionDescriptionCn": "初始版本",
    "versionDescriptionEn": "Initial version",
    "basicInfoSnapshot": {
      "nameCn": "新消息自动通知",
      "nameEn": "Auto Message Notification",
      "descriptionCn": "收到 IM 消息后自动发送通知到OA系统",
      "descriptionEn": "Auto notify OA system upon receiving IM messages"
    },
    "orchestrationConfig": {
      "trigger": {
        "type": "http",
        "authTypeSchema": {
          "type": "BEARER",
          "carrier": "header",
          "fieldName": "Authorization",
          "required": true
        },
        "inParamSchema": {
          "type": "object",
          "properties": {
            "sender": { "type": "string" },
            "content": { "type": "string" }
          },
          "required": ["sender", "content"]
        },
        "rateLimit": { "qpm": 100 }
      },
      "nodes": [
        {
          "id": "node_entry",
          "type": "entry",
          "labelCn": "接收请求",
          "labelEn": "Receive Request",
          "position": { "x": 100, "y": 200 }
        },
        {
          "id": "node_1",
          "type": "connector",
          "labelCn": "发送通知",
          "labelEn": "Send Notification",
          "connectorVersionId": "9876543210123456789",
          "inputMapping": {
            "receiver": "${trigger.sender}",
            "content": "${trigger.content}"
          },
          "position": { "x": 350, "y": 200 }
        },
        {
          "id": "node_exit",
          "type": "exit",
          "labelCn": "返回结果",
          "labelEn": "Return Result",
          "outputFields": ["result.msgId", "result.code"],
          "position": { "x": 650, "y": 200 }
        }
      ],
      "edges": [
        { "id": "e1", "sourceNodeId": "node_entry", "targetNodeId": "node_1" },
        { "id": "e2", "sourceNodeId": "node_1", "targetNodeId": "node_exit" }
      ]
    },
    "publishedTime": "2026-05-21T10:00:00.000+08:00",
    "createTime": "2026-05-21T09:00:00.000+08:00"
  },
  "page": null
}
```

---

#### #20 PUT /api/v1/flows/{flowId}/versions/{versionId} — 保存编排配置

> ⚠️ **触发器配置内嵌于编排 JSON**（v2.7.3 决策，不单独建表）：`orchestrationConfig.trigger` 包含触发类型、认证类型 schema（**仅声明类型，不含凭证值**）、入参 Schema、限流。

```json
// Request — orchestrationConfig 全文替换
{
  "orchestrationConfig": {
    "trigger": {
      "type": "http",
      "authTypeSchema": {
        "type": "BEARER",
        "carrier": "header",
        "fieldName": "Authorization",
        "required": true
      },
      "inParamSchema": {
        "type": "object",
        "properties": {
          "sender": { "type": "string" },
          "content": { "type": "string" }
        },
        "required": ["sender", "content"]
      },
      "rateLimit": {
        "qpm": 100
      }
    },
    "nodes": [
      {
        "id": "node_entry",
        "type": "entry",
        "labelCn": "接收请求",
        "labelEn": "Receive Request",
        "position": { "x": 100, "y": 200 }
      },
      {
        "id": "node_1",
        "type": "connector",
        "labelCn": "发送通知",
        "labelEn": "Send Notification",
        "connectorVersionId": "9876543210123456789",
        "inputMapping": {
          "receiver": "${trigger.sender}",
          "content": "${trigger.content}"
        },
        "position": { "x": 350, "y": 200 }
      },
      {
        "id": "node_2",
        "type": "data_processor",
        "labelCn": "格式化消息",
        "labelEn": "Format Message",
        "config": {
          "fieldMappings": [
            { "source": "${node_1.msgId}", "target": "result.id" },
            { "source": "constant:success", "target": "result.status" }
          ]
        },
        "position": { "x": 500, "y": 200 }
      },
      {
        "id": "node_exit",
        "type": "exit",
        "labelCn": "返回结果",
        "labelEn": "Return Result",
        "outputFields": ["result.id", "result.status"],
        "position": { "x": 650, "y": 200 }
      }
    ],
    "edges": [
      { "id": "e1", "sourceNodeId": "node_entry", "targetNodeId": "node_1" },
      { "id": "e2", "sourceNodeId": "node_1", "targetNodeId": "node_2" },
      { "id": "e3", "sourceNodeId": "node_2", "targetNodeId": "node_exit" }
    ]
  }
}

// Response 200
{
  "code": "200",
  "messageZh": "保存成功",
  "messageEn": "Saved",
  "data": {
    "versionId": "9876543210123456789",
    "versionStatus": 0,
    "lastUpdateTime": "2026-05-21T10:00:00.000+08:00"
  }
}
```

> 💡 **节点/连线 id 字段说明**：编排内部使用字符串 UUID（如 `node_entry`/`node_1`/`e1`），由前端编排画布生成，仅在单个 orchestrationConfig 内部唯一（参考 plan-db.md §6.2）。

> 💡 **连接器版本引用 `connectorVersionId`**：使用 BIGINT 雪花 ID 转 string，对应 `connector_version_t.id`。

#### #21 POST /api/v1/flows/{flowId}/versions/{versionId}/publish — 发布

```json
// Request
{
  "versionNo": "1.0.0",
  "versionDescriptionCn": "初始版本",
  "versionDescriptionEn": "Initial version"
}

// Response 200
{
  "code": "200",
  "messageZh": "发布成功",
  "messageEn": "Published",
  "data": {
    "versionId": "9876543210123456789",
    "versionNo": "1.0.0",
    "versionStatus": 1,
    "publishedTime": "2026-05-21T10:00:00.000+08:00"
  }
}
// 注意：本版本发布无需审批（NG19，V1 阶段引入）
```

---



### 3.5 执行操作（同步）


#### #22 POST /api/v1/flows/{flowId}/executions — 手动触发（同步）

> ⚠️ **凭证由调用方携带**（v2.6 决策）：手动触发时若涉及连接器需要认证，在请求体 `credentials` 字段携带；运行时注入 ExecutionContext 仅内存生命周期，节点执行完成后清除；写入 execution_step/record 时按 `connectionConfig.authTypeSchema.sensitive` 自动脱敏（值 → `***`）。

```json
// Request
{
  "triggerData": {
    "sender": "user_001",
    "content": "你好，这是一条测试消息"
  },
  "credentials": {
    "9876543210123456789": {
      "accessKey": "ak_xxxx",
      "secretKey": "sk_xxxx"
    }
  }
}
// credentials 顶层键为 connectorVersionId（string），值为该连接器认证字段键值对

// Response 200（同步返回完整执行结果）
{
  "code": "200",
  "messageZh": "执行成功",
  "messageEn": "Success",
  "data": {
    "executionId": "1122334455667788990",
    "flowId": "1234567890123456789",
    "flowVersionId": "9876543210123456789",
    "status": 2,
    "triggerType": 2,
    "correlationId": "corr_abc123",
    "startedTime": "2026-05-21T10:00:01.000+08:00",
    "completedTime": "2026-05-21T10:00:03.250+08:00",
    "durationMs": 2250,
    "resultData": { "msgId": "msg_xxxx" },
    "steps": [
      {
        "stepId": "2233445566778899001",
        "stepOrder": 1,
        "nodeId": "node_entry",
        "nodeNameCn": "接收请求",
        "nodeNameEn": "Receive Request",
        "nodeType": 1,
        "status": 0,
        "inputData": { "sender": "user_001", "content": "你好" },
        "outputData": { "sender": "user_001", "content": "你好" },
        "startedTime": "2026-05-21T10:00:01.000+08:00",
        "completedTime": "2026-05-21T10:00:01.010+08:00",
        "durationMs": 10
      },
      {
        "stepId": "2233445566778899002",
        "stepOrder": 2,
        "nodeId": "node_1",
        "nodeNameCn": "发送通知",
        "nodeNameEn": "Send Notification",
        "nodeType": 2,
        "status": 0,
        "inputData": { "receiver": "user_001", "content": "你好", "accessKey": "***(12)", "secretKey": "***(32)" },
        "outputData": { "msgId": "msg_xxxx", "code": 0 },
        "startedTime": "2026-05-21T10:00:01.020+08:00",
        "completedTime": "2026-05-21T10:00:03.230+08:00",
        "durationMs": 2210,
        "errorInfo": null
      },
      {
        "stepId": "2233445566778899003",
        "stepOrder": 3,
        "nodeId": "node_exit",
        "nodeNameCn": "返回结果",
        "nodeNameEn": "Return Result",
        "nodeType": 4,
        "status": 0,
        "inputData": { "msgId": "msg_xxxx" },
        "outputData": { "msgId": "msg_xxxx" },
        "startedTime": "2026-05-21T10:00:03.235+08:00",
        "completedTime": "2026-05-21T10:00:03.240+08:00",
        "durationMs": 5
      }
    ]
  }
}

// Response 200（执行超时）
{
  "code": "200",
  "messageZh": "执行超时",
  "messageEn": "Timeout",
  "data": {
    "executionId": "1122334455667788990",
    "status": 4,
    "startedTime": "2026-05-21T10:00:01.000+08:00",
    "completedTime": "2026-05-21T10:05:01.000+08:00",
    "durationMs": 300000,
    "errorMessage": "执行超时，已强制终止"
  }
}

// Response 200（执行失败 — 默认错误处理 FR-023）
{
  "code": "200",
  "messageZh": "执行失败",
  "messageEn": "Failed",
  "data": {
    "executionId": "1122334455667788990",
    "status": 3,
    "errorMessage": "节点 '发送通知' 执行失败: HTTP 503 服务不可用",
    "steps": [
      { "nodeId": "node_entry", "status": 0, "...": "..." },
      {
        "nodeId": "node_1",
        "nodeNameCn": "发送通知",
        "status": 1,
        "errorInfo": {
          "code": "DOWNSTREAM_UNAVAILABLE",
          "message": "HTTP 503 服务不可用",
          "downstreamStatus": 503,
          "downstreamBody": "Service Unavailable"
        },
        "...": "..."
      }
    ]
  }
}
```

> 💡 **脱敏示例**：`accessKey: "***(12)"` 表示原值长度为 12 字符的 access_key 被脱敏；前端展示时去掉数字保留 `***`。

#### #23 POST /api/v1/flows/{flowId}/test-run — 测试运行（同步）

```json
// Request
{
  "mockTriggerData": {
    "sender": "test_user",
    "content": "测试消息"
  },
  "credentials": {
    "9876543210123456789": {
      "accessKey": "test_ak",
      "secretKey": "test_sk"
    }
  }
}

// Response 200（同步返回）
{
  "code": "200",
  "messageZh": "测试运行成功",
  "messageEn": "Success",
  "data": {
    "executionId": "1122334455667788990",
    "status": 2,
    "triggerType": 3,
    "isTest": true,
    "steps": [ "..."],
    "durationMs": 1250
  }
}
```

### 3.6 HTTP 触发（同步）


#### #24 POST /api/v1/trigger/{flowId}/invoke — HTTP 触发

**触发器配置位置**: 触发器配置（含触发类型、认证类型 schema、入参 Schema、限流）完整内嵌于 `flow_version_t.orchestration_config.trigger` JSON（v2.7.3 决策），**不单独维护触发端点表**。HTTP 触发查找路径：`flowId` → `flow_t.currentPublishedVersionId` → `flow_version_t.orchestrationConfig.trigger`，全程主键索引查询。

**触发前置校验**：
1. `flow_t.lifecycleStatus = 1`（running 状态，FR-013~015 控制）
2. `currentPublishedVersionId` 非空（至少有一个已发布版本）
3. 请求凭证（由调用方在 Header/Query 中携带，不存储）匹配 `trigger.authTypeSchema` 声明的类型
4. 触发频率未超 `trigger.rateLimit.qpm` 阈值

```json
// Request — 由外部系统发送
// Header: Authorization: Bearer xxxxxx （凭证由调用方携带，不在数据库存储；具体认证类型由 trigger.authTypeSchema 声明）
// Body 格式由 trigger.inParamSchema 校验
{
  "sender": "external_system",
  "content": "这是一条外部消息"
}

// Response 200（同步执行完成）
{
  "code": "200",
  "messageZh": "执行成功",
  "messageEn": "Success",
  "data": {
    "executionId": "1122334455667788990",
    "status": 2,
    "resultData": { "msgId": "msg_xxxx" },
    "durationMs": 2250
  }
}

// Response 401（认证失败）
{ "code": "401", "messageZh": "认证失败", "messageEn": "Authentication failed", "data": null, "page": null }

// Response 403（连接流未启用或未发布）
{ "code": "403", "messageZh": "连接流未启用或无已发布版本", "messageEn": "Flow not running or no published version", "data": null, "page": null }

// Response 429（超过限流阈值 FR-024）
{ "code": "429", "messageZh": "请求频率超限，请稍后重试", "messageEn": "Too many requests", "data": null, "page": null }
```

**HTTP 触发认证说明**:
- 认证凭证（如 Bearer Token / API Key / OAuth2 Access Token）由调用方在请求中携带（Header 或 Query），**平台不存储任何凭证**（v2.6 决策）
- 平台仅根据 `trigger.authTypeSchema` 校验凭证格式合法性（如类型、字段名、carrier 位置、是否必填）；具体凭证有效性由下游连接器调用时验证
- 限流：基于 `trigger.rateLimit.qpm`，按 `flowId` 维度限流（V1 可扩展为按 IP / 凭证维度）

---

### 3.7 执行历史


#### #25 GET /api/v1/flows/{flowId}/executions — 执行历史列表

```json
// Query params: ?curPage=1&pageSize=20&status=3&from=2026-05-01&to=2026-05-21

// Response
{
  "code": "200",
  "messageZh": "查询成功",
  "messageEn": "Success",
  "data": [
    {
      "executionId": "1122334455667788990",
      "flowId": "1234567890123456789",
      "flowVersionId": "9876543210123456789",
      "triggerType": 1,
      "status": 2,
      "correlationId": "corr_abc123",
      "startedTime": "2026-05-21T10:00:01.000+08:00",
      "completedTime": "2026-05-21T10:00:03.250+08:00",
      "durationMs": 2250,
      "resultData": { "msgId": "msg_xxxx" }
    },
    {
      "executionId": "1122334455667788991",
      "flowId": "1234567890123456789",
      "flowVersionId": "9876543210123456789",
      "triggerType": 2,
      "status": 3,
      "startedTime": "2026-05-21T09:55:01.000+08:00",
      "completedTime": "2026-05-21T09:55:05.120+08:00",
      "durationMs": 4120,
      "errorMessage": "节点 '发送通知' 执行失败: 连接超时"
    }
  ],
  "page": {
    "curPage": 1,
    "pageSize": 20,
    "total": 2
  }
}
```

> 💡 **与 v1.x 的差异**：监控范围精简为执行历史查询（FR-025），移除全指标仪表盘 API。

---

#### #26 GET /api/v1/executions/{executionId} — 执行详情（含步骤）

**响应示例**：

```json
{
  "code": "200",
  "messageZh": "操作成功",
  "messageEn": "Success",
  "data": {
    "executionId": "1122334455667788990",
    "flowId": "1234567890123456789",
    "flowVersionId": "9876543210123456789",
    "triggerType": 1,
    "status": 2,
    "correlationId": "corr_abc123",
    "triggerData": { "sender": "user_001", "content": "你好" },
    "resultData": { "msgId": "msg_xxxx" },
    "operationsCount": 0,
    "dataInBytes": 1024,
    "dataOutBytes": 512,
    "durationMs": 2250,
    "startedTime": "2026-05-21T10:00:01.000+08:00",
    "completedTime": "2026-05-21T10:00:03.250+08:00",
    "steps": [
      {
        "stepId": "2233445566778899001",
        "stepOrder": 1,
        "nodeId": "node_entry",
        "nodeNameCn": "接收请求",
        "nodeNameEn": "Receive Request",
        "nodeType": 1,
        "status": 0,
        "inputData": { "sender": "user_001", "content": "你好" },
        "outputData": { "sender": "user_001", "content": "你好" },
        "startedTime": "2026-05-21T10:00:01.000+08:00",
        "completedTime": "2026-05-21T10:00:01.010+08:00",
        "durationMs": 10
      },
      {
        "stepId": "2233445566778899002",
        "stepOrder": 2,
        "nodeId": "node_1",
        "nodeNameCn": "发送通知",
        "nodeNameEn": "Send Notification",
        "nodeType": 2,
        "status": 0,
        "inputData": { "receiver": "user_001", "content": "你好" },
        "outputData": { "msgId": "msg_xxxx", "code": 0 },
        "startedTime": "2026-05-21T10:00:01.020+08:00",
        "completedTime": "2026-05-21T10:00:03.230+08:00",
        "durationMs": 2210
      },
      {
        "stepId": "2233445566778899003",
        "stepOrder": 3,
        "nodeId": "node_exit",
        "nodeNameCn": "返回结果",
        "nodeNameEn": "Return Result",
        "nodeType": 4,
        "status": 0,
        "inputData": { "msgId": "msg_xxxx" },
        "outputData": { "msgId": "msg_xxxx" },
        "startedTime": "2026-05-21T10:00:03.235+08:00",
        "completedTime": "2026-05-21T10:00:03.240+08:00",
        "durationMs": 5
      }
    ]
  },
  "page": null
}
```

---

## 附录 A：修订记录

| 版本 | 日期 | 修订内容 | 修订人 |
|------|------|---------|--------|
| **v2.0** | 2026-05-21 | 初始版本——对齐 spec.md v4.0：移除 Scope/审批/事件/定时触发，执行改为同步，FR 重编号 1~25 | SDDU Plan Agent |
| **v2.7.3** | 2026-05-22 | HTTP 触发 URL 从 `/trigger/{flowId}/{triggerToken}` 改为 `/trigger/{flowId}/invoke`（flowId 雪花数字），签名验证段重写为认证说明（凭证调用方携带 + authTypeSchema 校验 + rateLimit 限流），新增 403 错误码（连接流未启用）；§6 接口编号总表 API-024 路径同步 | SDDU Plan Agent |
| **v2.7.5** | **2026-05-22** | **全面对齐 plan.md v2.7.5（含 v2.0 → v2.7.5 全部决策）**，本次为彻底对齐重写。核心变更：① **§0 重写**版本对齐说明，列出全部 v2.0 → v2.7.5 变更项；② **§1.2 字段命名规范**新增双语字段约定（`*Cn`/`*En`）+ snake_case 到 camelCase 映射表；③ **§1.4 数据类型规范**重写：BIGINT 雪花 ID 必须返回 string（避免 JS 精度丢失）+ 枚举字段统一返回 TINYINT 数字（与数据库一致）+ 完整枚举数字字典；④ **§2.1 连接器 CRUD** 所有 Request/Response 示例：ID 改用数字 string、名称改双语 `nameCn`/`nameEn`、描述改双语 `descriptionCn`/`descriptionEn`、状态改数字、`icon` → `iconUrl`、新增 `tags`、`createdAt` → `createTime`、统一响应格式（`code`/`messageZh`/`messageEn`/`data`/`page`）；⑤ **§2.2 连接器版本编辑**：`connectionConfig.auth` → `authTypeSchema`（仅声明类型不含凭证值，v2.6）、`auth.config` 凭证字段移除（改为 `fields` 数组含 `sensitive: true` 标记）；⑥ **§2.2 连接器版本发布**：`changeLog` → `versionDescriptionCn`/`versionDescriptionEn` 双语 VARCHAR(1000)；⑦ **§3.1 连接流 CRUD**：同 §2.1 命名/类型/响应统一调整，新增 `ownerGroup`、`deployedVersionId` → `currentPublishedVersionId`（与数据库对齐）；⑧ **§3.2 编排配置 PUT**：节点/连线 `nodeId`/`edgeId` → 内部 `id`（字符串 UUID）、`nodeType` → `type`、`label` → `labelCn`/`labelEn` 双语、节点连接器引用 `connectorVersionId` 用数字 string、连线 `source`/`target` → `sourceNodeId`/`targetNodeId`、触发器 `trigger.config.schema` → `trigger.inParamSchema` + `trigger.authTypeSchema`（v2.7.3 + v2.6）+ `trigger.rateLimit.qpm`；⑨ **§4.1 手动触发**：请求新增 `credentials` 顶层字段（按 `connectorVersionId` 分组凭证）、响应 ID 用数字 string、状态/触发方式/节点类型/步骤状态用 TINYINT 数字、新增 `correlationId`、`startedAt`/`finishedAt` → `startedTime`/`completedTime`、`nodeName` → `nodeNameCn`/`nodeNameEn` 双语、新增 `stepOrder`、`errorMessage` → `errorInfo` 结构化（含 code/message/downstreamStatus/downstreamBody）、脱敏示例 `"***（12）"`；⑩ **§4.2 HTTP 触发**：`auth_type_schema`/`in_param_schema`/`rate_limit` → camelCase `authTypeSchema`/`inParamSchema`/`rateLimit`，统一响应格式；⑪ **§5 执行历史**：字段名/类型/响应格式统一；⑫ **§7 状态枚举**全部重写为数字字典（含 v2.0 与 v2.7.5 差异说明，特别说明执行状态枚举从 3 个扩为 5 个保留 pending/running 的理由）；⑬ 顶部版本号 v2.0 → v2.7.5；⑭ 修订记录追加 v2.7.5 条目。**未变更项**：§1.1 基础规范 / §1.3 路径命名 / §1.5 响应格式 / §1.6 分页 / §1.7 错误码 / §6 接口编号总表（v2.7.3 已对齐）/ §2.1 §2.2 §3.1 §3.2 §4.1 §4.2 §5 的 URL 路径 与 FR 编号 | SDDU Plan Agent |
| **v2.7.6** | **2026-05-22** | **章节结构重组（参考 capability-open-platform/plan-api.md 风格）**——按用户指示调整为「设计规范 → 接口清单 → 接口详细定义」三段式：① **§1 设计规范**：合并原 §1 接口规范（7 子节：基础规范/字段命名/路径命名/数据类型/响应格式/分页/错误码）+ 原 §7 状态枚举（作为 §1.8 含 9 个子节 1.8.1~1.8.9），共 8 子节；② **§2 接口清单**：提升原 §6 接口编号总表为正式 §2，含 26 个端点的完整路由表（编号 / 方法 / 路径 / 所属模块 / 所属服务 / FR）；③ **§3 接口详细定义**：合并原 §2 连接器管理 / §3 连接流管理 / §4 运行时执行 / §5 执行历史 4 个顶级章节为统一的 §3，7 个子节（3.1 连接器 CRUD / 3.2 连接器版本管理 / 3.3 连接流 CRUD / 3.4 连接流版本管理 / 3.5 执行操作（同步）/ 3.6 HTTP 触发（同步）/ 3.7 执行历史）；④ **删除各模块头部的小清单**（7 处 `\| 方法 \| 路径 \| 说明 \| FR \|` 表格，合计 41 行），所有路由信息统一在 §2 接口清单中维护，避免双源；⑤ 顶部版本号 v2.7.5 → v2.7.6；⑥ 修订记录追加 v2.7.6 条目。**未变更项**：所有接口的请求/响应示例正文、URL 路径、FR 编号、字段命名（v2.7.5 已对齐）。**变更统计**：971 → 934 行（-37 行净精简，删除小清单 -41 + 新增 §3 总标题 +4） | SDDU Plan Agent |
| **v2.7.6a** | **2026-05-22** | **§2 接口清单格式优化**（参考 capability-open-platform/plan-api.md §1 风格）——按用户指示，列结构从 6 列改为 7 列：① **新列结构**：`# / 服务 / 模块 / Method / Path / 说明 / FR`（原为 `编号 / 方法 / 路径 / 所属模块 / 所属服务 / FR`）；② **编号简化**：`API-001 ~ API-026` → `1 ~ 26`（与参考文档一致）；③ **服务列简化**：纯净的 `open-server` / `connector-api` 两个值（原嵌套描述如 "open-server debug 代理 → connector-api debug-api" 移至下方汇总表）；④ **模块列重新分组**：连接器管理（#1~5）/ 连接器版本（#6~9）/ 连接流管理（#10~17）/ 连接流版本（#18~21）/ 调试代理（#22~23）/ HTTP 触发（#24）/ 监控查询（#25~26）共 7 个模块，每个模块只在首行显示名称（合并单元格效果）；⑤ **说明列优化**：补全语义化描述（如 "查询连接器列表" "部署连接流（切换 currentPublishedVersionId 并启动）"）；⑥ **新增「服务部署归属」汇总表**（替代原 5 行文字说明）：服务 / 端口 / 上下文根 / 接口数 / 接口范围 5 列；⑦ **新增「路径前缀」说明**：明确 Path 列省略上下文根，给出完整 URL 拼接公式与示例。**未变更项**：26 个端点的方法、路径、FR 编号（仅展示格式优化，无路由变更） | SDDU Plan Agent |
| **v2.7.6b** | **2026-05-22** | **补全 §3 全部缺失的接口定义 + 标题加编号**——按用户指示，为 §3 接口详细定义所有接口标题添加清单编号（#1~#26）。补全 15 个此前缺失的接口详细定义：① §3.1 连接器 CRUD — 新增 #3 GET 详情 / #4 PUT 更新 / #5 DELETE 删除；② §3.2 连接器版本管理 — 新增 #6 GET 版本列表 / #7 GET 版本详情（含 connectionConfig 完整示例）；③ §3.3 连接流 CRUD — 新增 #11 GET 列表 / #12 GET 详情 / #13 PUT 更新 / #14 DELETE 删除 / #16 POST 启动 / #17 POST 停止；④ §3.4 连接流版本管理 — 新增 #18 GET 版本列表 / #19 GET 版本详情（含 orchestrationConfig 完整示例）；⑤ §3.6 HTTP 触发 — 新增 #24 POST 标题（原为无标题直接展开，现加 `#### #24 POST /api/v1/trigger/{flowId}/invoke — HTTP 触发`）；⑥ §3.7 执行历史 — 新增 #26 GET 执行详情（含 steps 数组完整示例）。**变更统计**：941 → 1472 行（+531 行，15 个新增接口 + 编号追加），接口覆盖率 26/26 | SDDU Plan Agent |
