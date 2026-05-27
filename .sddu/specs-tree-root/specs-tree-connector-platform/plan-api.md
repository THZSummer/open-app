# API 接口设计：连接器平台

**Feature ID**: CONN-PLAT-001  
**关联文档**: plan.md (§4.4), plan-db.md (§3 表结构)  
**版本**: v3.1  
**创建日期**: 2026-05-21  
**最后更新**: 2026-05-27  
**对齐基线**: plan-json-schema.md v5.6 + plan-db.md v3.0

---

## 0. 版本对齐说明

| 维度 | 说明 | 决策来源 |
|------|------|---------|
| **版本模型** | **单版本**（编辑即生效，无草稿/发布） | spec v5.0 |
| **JSON 字段结构** | 对齐 [plan-json-schema.md v5.6](./plan-json-schema.md)：React Flow 格式 / 字段重命名（`authConfig`/`inputContract`/`outputContract`/`rateLimitConfig`）/ 协议感知 contract / inputMapping-outputMapping 分段 / JSON Path 表达式 | plan-json-schema.md v5.4~v5.6 |
| **JSON 示例** | 展示完整请求/响应体，冗余以独立可读；详细 Schema 见 plan-json-schema.md | 本文档 |
| 端点总数 | **18** | — |

---

## 1. 设计规范

> 💡 以下 API 设计规范沿用能力开放平台（CAP-OPEN-001）已确立的标准，确保全项目 API 风格统一。详情见 `../specs-tree-capability-open-platform/plan-api.md §0`。

### 1.1 基础规范

| 规范项 | 说明 |
|--------|------|
| 基础路径 | `/service/open/v2` (open-server 管理面) / `/api/v1` (connector-api 执行面) |
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
- 图标字段：使用 `FileId` 后缀（文件 ID，值由系统解析为可访问 URL），如 `iconFileId`
- URL 字段：使用 `Url` 后缀，如 `triggerUrl`
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
| `/service/open/v2/connector-versions` | `/service/open/v2/connector_versions` |
| `/service/open/v2/test-run` | `/service/open/v2/testRun` |

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
| `flow_t.lifecycleStatus` | `0=undeployed, 1=running, 2=stopped` |
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

> ⚠️ **区分两个维度**：编排 JSON 中 `trigger` 节点的 `node.data.type` 为字符串枚举（`"http"` / `"manual"`），不含 `test`（test 是运行时调用模式，非触发类型）。执行记录中 `execution_record_t.trigger_type` 为 TINYINT 数字枚举，包含 `test=3`（运行时记录维度）。

**编排 JSON `node.data.type`（字符串）**：

| 值 | 说明 | MVP |
|------|------|:---:|
| `"http"` | HTTP 触发（FR-021） | ✅ |
| `"manual"` | 手动触发（FR-022） | ✅ |

**执行记录 `execution_record_t.trigger_type`（TINYINT）**：

| 数字 | 含义 | 说明 | MVP |
|:----:|------|------|:---:|
| `1` | `http` | HTTP 触发（FR-021） | ✅ |
| `3` | `test` | 测试运行（FR-020） | ✅ |

> **V1 扩展**：4=event / 5=webhook / 6=scheduled（V1 阶段引入，NG14/NG15/NG17）。manual(2) 移至 NG20。

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

> 💡 **两个维度**：执行记录中 `execution_step_t.node_type` 使用 TINYINT 数字（如上表）。编排 JSON 中 `node.type` 使用字符串（`"trigger"` / `"connector"` / `"data_processor"` / `"exit"`，React Flow 框架字段），与 `entry` 的映射：`"trigger"`（配置视角）↔ `entry: 1`（运行时视角）。

#### 1.8.7 连接器状态 (connector.status)

> 预留字段，本期不定义业务语义，默认 `1`。未来可用于控制连接器是否可被连接流引用等业务。此处暂不定义具体枚举值。

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
| 1 | **open-server** | **连接器管理** | POST | `/service/open/v2/connectors` | 创建连接器 | FR-001 |
| 2 | | | GET | `/service/open/v2/connectors` | 查询连接器列表 | FR-004 |
| 3 | | | GET | `/service/open/v2/connectors/{connectorId}` | 查询连接器详情 | FR-004 |
| 4 | | | PUT | `/service/open/v2/connectors/{connectorId}` | 更新连接器基本信息 | FR-002 |
| 5 | | | DELETE | `/service/open/v2/connectors/{connectorId}` | 删除连接器 | FR-003 |
| 6 | | **连接器配置** | GET | `/service/open/v2/connectors/{connectorId}/config` | 获取连接器配置（单版本） | FR-005 |
| 7 | | | PUT | `/service/open/v2/connectors/{connectorId}/config` | 编辑连接器配置（编辑即生效） | FR-006 |
| 8 | | **连接流管理** | POST | `/service/open/v2/flows` | 创建连接流 | FR-009 |
| 9 | | | GET | `/service/open/v2/flows` | 查询连接流列表 | FR-012 |
| 10 | | | GET | `/service/open/v2/flows/{flowId}` | 查询连接流详情 | FR-016 |
| 11 | | | PUT | `/service/open/v2/flows/{flowId}` | 更新连接流基本信息 | FR-010 |
| 12 | | | DELETE | `/service/open/v2/flows/{flowId}` | 删除连接流 | FR-011 |
| 13 | | | POST | `/service/open/v2/flows/{flowId}/start` | 启动连接流 | FR-014 |
| 14 | | | POST | `/service/open/v2/flows/{flowId}/stop` | 停止连接流 | FR-015 |
| 15 | | **连接流配置** | GET | `/service/open/v2/flows/{flowId}/config` | 获取连接流编排配置（单版本） | FR-016 |
| 16 | | | PUT | `/service/open/v2/flows/{flowId}/config` | 保存编排配置（编辑即生效） | FR-017 |
| 17 | | **测试代理** | POST | `/service/open/v2/flows/{flowId}/test-run` | 测试运行（同步，转发至 connector-api） | FR-020 |
| 18 | **connector-api** | **HTTP 触发** | POST | `/api/v1/trigger/{flowId}/invoke` | HTTP 触发连接流（同步执行，返回结果） | FR-021 |

> **总计**：18 个 HTTP 端点（从 v2.7.6 的 26 个精简）
>
> **移除的端点**（8 个）：版本列表 ×2 / 版本发布 ×2 / 部署 / 手动触发 / 执行历史列表 / 执行详情
>
> **服务部署归属**：
>
> | 服务 | 端口 | 接口数 | 接口范围 |
> |------|------|:------:|---------|
> | **open-server**（管理类） | 18080 | 17 | #1~#17（含测试运行转发至 connector-api） |
> | **connector-api**（运行时） | 18180 | 1（对外）+ 1（内部） | #18 HTTP 触发（对外）+ 内部 test-api 接口（被 #17 转发调用） |

---

## 3. 接口详细定义

> 💡 接口清单见 §2，本章为每个接口的请求/响应详细定义。所有接口的字段命名、数据类型、响应格式、状态枚举均遵循 §1 设计规范。



### 3.1 连接器 CRUD


#### #1 POST /service/open/v2/connectors — 创建连接器

```json
// Request
{
  "nameCn": "IM 发送消息",
  "nameEn": "IM Send Message",
  "iconFileId": "file_im_send_message",
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

#### #2 GET /service/open/v2/connectors — 查询列表

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
      "iconFileId": "file_im_send_message",
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

#### #3 GET /service/open/v2/connectors/{connectorId} — 查询连接器详情

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
    "iconFileId": "file_im_send_message",
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

#### #4 PUT /service/open/v2/connectors/{connectorId} — 更新连接器基本信息

```json
// Request
{
  "nameCn": "IM 发送消息（新版）",
  "nameEn": "IM Send Message (New)",
  "descriptionCn": "更新后的 IM 消息发送能力",
  "descriptionEn": "Updated IM messaging capability",
  "iconFileId": "file_im_send_message_v2",
  "connectorType": 1
}

// Response 200
{
  "code": "200",
  "messageZh": "保存成功",
  "messageEn": "Saved",
  "data": {
    "connectorId": "1234567890123456789",
    "lastUpdateTime": "2026-05-21T10:00:00.000+08:00"
  }
}
```

> 💡 **编辑即生效**：MVP 单版本模型，保存后立即生效。已引用该连接器的运行中连接流在下次触发时使用新配置。

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
    "authConfig": {
      "type": "AKSK",
      "fields": [
        { "name": "accessKey", "carrier": "header", "fieldName": "AK", "required": true, "sensitive": true },
        { "name": "secretKey", "carrier": "header", "fieldName": "SK", "required": true, "sensitive": true }
      ]
    },
    "inputContract": {
      "protocol": "HTTP",
      "header": {
        "type": "object",
        "properties": {
          "Authorization": { "type": "string", "description": "Bearer token" }
        }
      },
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
          "msgId": { "type": "string", "description": "消息ID" }
        }
      }
    },
    "timeoutMs": 30000,
    "rateLimitConfig": {
      "maxQps": 10,
      "maxConcurrency": 5
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

**authConfig.type 枚举**（JSON 内嵌字段使用字符串；与 DB 列级 TINYINT 枚举的映射见 plan-json-schema.md §2.1）：

**连接器认证枚举**（`connectionConfig.authConfig.type`，调用下游 API 时使用）：

| JSON 字符串 | TINYINT | 说明 | 本版本优先级 |
|------------|:-------:|------|:-----------:|
| `SOA` | 1 | SOA 认证 | ⭐ **最高** |
| `APIG` | 2 | API 网关认证 | ⭐ **最高** |
| `NONE` | 4 | 无需认证 | ★★ 按需 |
| `AKSK` | 5 | AccessKey/SecretKey | ★★ 按需 |

> 💡 **说明**：仅声明认证类型与字段名，**不存储任何凭证值**；调用方在触发请求时携带。

**触发器认证枚举**（编排中 trigger 节点的 `data.authConfig.type`，外部调用方触发连接流时携带）：

| JSON 字符串 | TINYINT | 说明 | 本版本优先级 |
|------------|:-------:|------|:-----------:|
| `SYSTOKEN` | 7 | 🆕 系统 Token 认证 | ✅ **本版本支持** |

> 💡 **说明**：本版本触发器认证仅支持 SYSTOKEN(7)。`connectionConfig` 与 trigger 节点中的 `authConfig` 结构一致，但枚举值范围不同。

> ❌ **不再需要的 API**（v2.8.0）：~~`GET .../versions`~~ / ~~`POST .../publish`~~ (MVP 单版本)；~~`POST .../credentials`~~ (凭证不持久化)

---

### 3.3 连接流 CRUD


#### #8 POST /service/open/v2/flows — 创建连接流

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

#### #9 GET /service/open/v2/flows — 查询连接流列表

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

#### #10 GET /service/open/v2/flows/{flowId} — 查询连接流详情

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

#### #11 PUT /service/open/v2/flows/{flowId} — 更新连接流基本信息

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

#### #12 DELETE /service/open/v2/flows/{flowId} — 删除连接流

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

> 💡 **MVP v5.0 简化**：创建连接流后默认运行状态。编辑即运行，无需显式部署操作。

#### #13 POST /service/open/v2/flows/{flowId}/start — 启动连接流

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

#### #14 POST /service/open/v2/flows/{flowId}/stop — 停止连接流

> **说明**：将连接流 lifecycleStatus 从 `running(1)` 切换为 `stopped(2)`（FR-015）。`currentPublishedVersionId` 指针保留（不删除），可后续启动（#16）恢复运行。停止后 HTTP 触发入口返回 403。

**响应示例**：

```json
{
  "code": "200",
  "messageZh": "停止成功",
  "messageEn": "Success",
  "data": {
    "flowId": "1234567890123456789",
    "lifecycleStatus": 2,
    "lastUpdateTime": "2026-05-21T12:05:00.000+08:00"
  },
  "page": null
}
```

---

### 3.4 连接流配置管理

> 💡 **MVP v5.0 单版本模型**：每个连接流仅有一份编排配置，`GET/PUT .../config` 直接操作，编辑即生效。无版本列表、版本切换、发布操作。

#### #15 GET /service/open/v2/flows/{flowId}/config — 获取编排配置

```json
// Response 200
{
  "code": "200",
  "messageZh": "操作成功",
  "messageEn": "Success",
  "data": {
    "flowId": "1234567890123456789",
    "orchestrationConfig": {
      "nodes": [
        {
          "id": "node_trigger",
          "type": "trigger",
          "position": { "x": 100.0, "y": 200.0 },
          "data": {
            "labelCn": "接收请求",
            "labelEn": "Receive Request",
            "type": "http",
            "authConfig": {
              "type": "SYSTOKEN",
              "fields": [
                { "name": "token", "carrier": "header", "fieldName": "X-Sys-Token" }
              ]
            },
            "inputContract": {
              "protocol": "HTTP",
              "body": {
                "type": "object",
                "properties": {
                  "sender": { "type": "string" },
                  "content": { "type": "string" }
                },
                "required": ["sender", "content"]
              }
            },
            "rateLimitConfig": { "maxQps": 100 }
          }
        },
        {
          "id": "node_1",
          "type": "connector",
          "position": { "x": 350.0, "y": 200.0 },
          "data": {
            "labelCn": "发送通知",
            "labelEn": "Send Notification",
            "connectorVersionId": "9876543210123456789",
            "inputMapping": {
              "body": {
                "type": "object",
                "properties": {
                  "receiver": { "type": "string", "description": "接收者", "value": "${$.node.trigger.input.sender}" },
                  "content":  { "type": "string", "description": "消息内容", "value": "${$.node.trigger.input.content}" }
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
            "labelCn": "返回结果",
            "labelEn": "Return Result",
            "outputMapping": {
              "body": {
                "type": "object",
                "properties": {
                  "msgId": { "type": "string", "description": "消息ID", "value": "${$.node.node_1.output.msgId}" },
                  "code":  { "type": "integer", "description": "状态码", "value": "${$.constant:0}" }
                }
              }
            }
          }
        }
      ],
      "edges": [
        {
          "id": "e1",
          "source": "node_trigger",
          "target": "node_1",
          "type": "smoothstep",
          "label": "触发",
          "data": { "businessType": "default" }
        },
        {
          "id": "e2",
          "source": "node_1",
          "target": "node_exit",
          "type": "smoothstep",
          "label": "发送完成",
          "data": { "businessType": "default" }
        }
      ]
    },
    "lastUpdateTime": "2026-05-21T10:00:00.000+08:00"
  },
  "page": null
}
```

#### #16 PUT /service/open/v2/flows/{flowId}/config — 保存编排配置（编辑即生效）

```json
// Request — orchestrationConfig 全文替换（结构同 GET 响应，遵循 React Flow 格式：node.data 嵌套 + edge.source/target）
{
  "orchestrationConfig": {
    "nodes": [ /* ...同 GET 响应结构，详见 plan-json-schema.md §5.7... */ ],
    "edges": [ /* ... */ ]
  }
}

// Response 200
{
  "code": "200",
  "messageZh": "保存成功",
  "messageEn": "Saved",
  "data": {
    "flowId": "1234567890123456789",
    "lastUpdateTime": "2026-05-21T10:00:00.000+08:00"
  }
}
```

> 💡 **编辑即生效**：保存后立即生效。运行中的连接流在下次触发时使用新配置。
>
> 📐 **orchestrationConfig JSON 结构定义**：完整的 JSON Schema 和字段说明见 **[plan-json-schema.md §5 连接流编排配置](./plan-json-schema.md#5-连接流编排配置-schema)**。节点间传值映射（`inputMapping`/`outputMapping`）说明见 **[plan-json-schema.md §1.4](./plan-json-schema.md#14-节点间传值映射)**。完整示例见 **[plan-json-schema.md §5.7](./plan-json-schema.md#57-完整编排配置示例)**。

---

### 3.5 执行与触发

#### #17 POST /service/open/v2/flows/{flowId}/test-run — 测试运行（同步）

> ⚠️ **凭证由调用方携带**：若涉及连接器需要认证，在请求体 `credentials` 字段携带；运行时注入 ExecutionContext 仅内存生命周期。

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
    "steps": [
      {
        "nodeId": "node_trigger",
        "nodeNameCn": "接收请求",
        "nodeType": 1,
        "status": 0,
        "inputData": {},
        "outputData": {"sender": "test_user", "content": "test"},
        "durationMs": 10
      },
      {
        "nodeId": "node_1",
        "nodeNameCn": "发送通知",
        "nodeType": 2,
        "status": 0,
        "inputData": {},
        "outputData": {"msgId": "msg_xxxx"},
        "durationMs": 1250
      },
      {
        "nodeId": "node_exit",
        "nodeNameCn": "返回结果",
        "nodeType": 4,
        "status": 0,
        "outputData": {"msgId": "msg_xxxx"},
        "durationMs": 5
      }
    ],
    "durationMs": 1270
  }
}
```

> 💡 **注意**：测试运行结果为瞬态（不持久化到 execution_record_t，spec v5.0 FR-025→NG21），仅当次返回。

### 3.6 HTTP 触发（同步）

#### #18 POST /api/v1/trigger/{flowId}/invoke — HTTP 触发

**触发前置校验**：
1. `flow_t.lifecycleStatus = 1`（running 状态）
2. 请求凭证匹配 `trigger.authTypeSchema` 声明的类型
3. 触发频率未超 `trigger.rateLimit.maxQps` 阈值

```json
// Request — 由外部系统发送
// Header: X-Sys-Token: xxxxxx（凭证由调用方携带，不在数据库存储）
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

// Response 403（连接流未运行）
{ "code": "403", "messageZh": "连接流未运行", "messageEn": "Flow not running", "data": null, "page": null }

// Response 429（超过限流阈值 FR-024）
{ "code": "429", "messageZh": "请求频率超限", "messageEn": "Too many requests", "data": null, "page": null }
```

**HTTP 触发认证说明**:
- 认证凭证由调用方在请求中携带（Header），**平台不存储任何凭证**（v2.6 决策）
- 平台仅根据 `trigger.authTypeSchema` 校验凭证格式合法性
- 限流：基于 `trigger.rateLimit.maxQps`，按 `flowId` 维度限流

> 💡 **执行结果不持久化**（MVP v5.0）：HTTP 触发执行结果仅同步返回，不写入 `execution_record_t`（表设计保留至 V1）。

---

## 附录：修订记录

| 版本 | 日期 | 修订内容 | 修订人 |
|------|------|---------|--------|
| **v2.0** | 2026-05-21 | 初始版本——对齐 spec.md v4.0：移除 Scope/审批/事件/定时触发，执行改为同步，FR 重编号 1~25 | SDDU Plan Agent |
| **v2.7.3** | 2026-05-22 | HTTP 触发 URL 从 `/trigger/{flowId}/{triggerToken}` 改为 `/trigger/{flowId}/invoke`（flowId 雪花数字），签名验证段重写为认证说明（凭证调用方携带 + authTypeSchema 校验 + rateLimit 限流），新增 403 错误码（连接流未启用）；§6 接口编号总表 API-024 路径同步 | SDDU Plan Agent |
| **v2.7.5** | **2026-05-22** | **全面对齐 plan.md v2.7.5（含 v2.0 → v2.7.5 全部决策）**，本次为彻底对齐重写。核心变更：① **§0 重写**版本对齐说明，列出全部 v2.0 → v2.7.5 变更项；② **§1.2 字段命名规范**新增双语字段约定（`*Cn`/`*En`）+ snake_case 到 camelCase 映射表；③ **§1.4 数据类型规范**重写：BIGINT 雪花 ID 必须返回 string（避免 JS 精度丢失）+ 枚举字段统一返回 TINYINT 数字（与数据库一致）+ 完整枚举数字字典；④ **§2.1 连接器 CRUD** 所有 Request/Response 示例：ID 改用数字 string、名称改双语 `nameCn`/`nameEn`、描述改双语 `descriptionCn`/`descriptionEn`、状态改数字、`icon` → `iconUrl`、新增 `tags`、`createdAt` → `createTime`、统一响应格式（`code`/`messageZh`/`messageEn`/`data`/`page`）；⑤ **§2.2 连接器版本编辑**：`connectionConfig.auth` → `authTypeSchema`（仅声明类型不含凭证值，v2.6）、`auth.config` 凭证字段移除（改为 `fields` 数组含 `sensitive: true` 标记）；⑥ **§2.2 连接器版本发布**：`changeLog` → `versionDescriptionCn`/`versionDescriptionEn` 双语 VARCHAR(1000)；⑦ **§3.1 连接流 CRUD**：同 §2.1 命名/类型/响应统一调整，新增 `ownerGroup`、`deployedVersionId` → `currentPublishedVersionId`（与数据库对齐）；⑧ **§3.2 编排配置 PUT**：节点/连线 `nodeId`/`edgeId` → 内部 `id`（字符串 UUID）、`nodeType` → `type`、`label` → `labelCn`/`labelEn` 双语、节点连接器引用 `connectorVersionId` 用数字 string、连线 `source`/`target` → `sourceNodeId`/`targetNodeId`、触发器 `trigger.config.schema` → `trigger.inputSchema` + `trigger.authTypeSchema`（v2.7.3 + v2.6）+ `trigger.rateLimit.maxQps`；⑨ **§4.1 手动触发**：请求新增 `credentials` 顶层字段（按 `connectorVersionId` 分组凭证）、响应 ID 用数字 string、状态/触发方式/节点类型/步骤状态用 TINYINT 数字、新增 `correlationId`、`startedAt`/`finishedAt` → `startedTime`/`completedTime`、`nodeName` → `nodeNameCn`/`nodeNameEn` 双语、新增 `stepOrder`、`errorMessage` → `errorInfo` 结构化（含 code/message/downstreamStatus/downstreamBody）、脱敏示例 `"***（12）"`；⑩ **§4.2 HTTP 触发**：`auth_type_schema`/`in_param_schema`/`rate_limit` → camelCase `authTypeSchema`/`inputSchema`/`rateLimit`，统一响应格式；⑪ **§5 执行历史**：字段名/类型/响应格式统一；⑫ **§7 状态枚举**全部重写为数字字典（含 v2.0 与 v2.7.5 差异说明，特别说明执行状态枚举从 3 个扩为 5 个保留 pending/running 的理由）；⑬ 顶部版本号 v2.0 → v2.7.5；⑭ 修订记录追加 v2.7.5 条目。**未变更项**：§1.1 基础规范 / §1.3 路径命名 / §1.5 响应格式 / §1.6 分页 / §1.7 错误码 / §6 接口编号总表（v2.7.3 已对齐）/ §2.1 §2.2 §3.1 §3.2 §4.1 §4.2 §5 的 URL 路径 与 FR 编号 | SDDU Plan Agent |
| **v2.7.6** | **2026-05-22** | **章节结构重组（参考 capability-open-platform/plan-api.md 风格）**——按用户指示调整为「设计规范 → 接口清单 → 接口详细定义」三段式：① **§1 设计规范**：合并原 §1 接口规范（7 子节：基础规范/字段命名/路径命名/数据类型/响应格式/分页/错误码）+ 原 §7 状态枚举（作为 §1.8 含 9 个子节 1.8.1~1.8.9），共 8 子节；② **§2 接口清单**：提升原 §6 接口编号总表为正式 §2，含 26 个端点的完整路由表（编号 / 方法 / 路径 / 所属模块 / 所属服务 / FR）；③ **§3 接口详细定义**：合并原 §2 连接器管理 / §3 连接流管理 / §4 运行时执行 / §5 执行历史 4 个顶级章节为统一的 §3，7 个子节（3.1 连接器 CRUD / 3.2 连接器版本管理 / 3.3 连接流 CRUD / 3.4 连接流版本管理 / 3.5 执行操作（同步）/ 3.6 HTTP 触发（同步）/ 3.7 执行历史）；④ **删除各模块头部的小清单**（7 处 `| 方法 | 路径 | 说明 | FR |` 表格，合计 41 行），所有路由信息统一在 §2 接口清单中维护，避免双源；⑤ 顶部版本号 v2.7.5 → v2.7.6；⑥ 修订记录追加 v2.7.6 条目。**未变更项**：所有接口的请求/响应示例正文、URL 路径、FR 编号、字段命名（v2.7.5 已对齐）。**变更统计**：971 → 934 行（-37 行净精简，删除小清单 -41 + 新增 §3 总标题 +4） | SDDU Plan Agent |
| **v2.7.6a** | **2026-05-22** | **§2 接口清单格式优化**（参考 capability-open-platform/plan-api.md §1 风格）——按用户指示，列结构从 6 列改为 7 列：① **新列结构**：`# / 服务 / 模块 / Method / Path / 说明 / FR`（原为 `编号 / 方法 / 路径 / 所属模块 / 所属服务 / FR`）；② **编号简化**：`API-001 ~ API-026` → `1 ~ 26`（与参考文档一致）；③ **服务列简化**：纯净的 `open-server` / `connector-api` 两个值（原嵌套描述如 "open-server debug 代理 → connector-api debug-api" 移至下方汇总表）；④ **模块列重新分组**：连接器管理（#1~5）/ 连接器版本（#6~9）/ 连接流管理（#10~17）/ 连接流版本（#18~21）/ 调试代理（#22~23）/ HTTP 触发（#24）/ 监控查询（#25~26）共 7 个模块，每个模块只在首行显示名称（合并单元格效果）；⑤ **说明列优化**：补全语义化描述（如 "查询连接器列表" "部署连接流（切换 currentPublishedVersionId 并启动）"）；⑥ **新增「服务部署归属」汇总表**（替代原 5 行文字说明）：服务 / 端口 / 上下文根 / 接口数 / 接口范围 5 列；⑦ **新增「路径前缀」说明**：明确 Path 列省略上下文根，给出完整 URL 拼接公式与示例。**未变更项**：26 个端点的方法、路径、FR 编号（仅展示格式优化，无路由变更） | SDDU Plan Agent |
| **v2.7.6b** | **2026-05-22** | **补全 §3 全部缺失的接口定义 + 标题加编号**——按用户指示，为 §3 接口详细定义所有接口标题添加清单编号（#1~#26）。补全 15 个此前缺失的接口详细定义：① §3.1 连接器 CRUD — 新增 #3 GET 详情 / #4 PUT 更新 / #5 DELETE 删除；② §3.2 连接器版本管理 — 新增 #6 GET 版本列表 / #7 GET 版本详情（含 connectionConfig 完整示例）；③ §3.3 连接流 CRUD — 新增 #11 GET 列表 / #12 GET 详情 / #13 PUT 更新 / #14 DELETE 删除 / #16 POST 启动 / #17 POST 停止；④ §3.4 连接流版本管理 — 新增 #18 GET 版本列表 / #19 GET 版本详情（含 orchestrationConfig 完整示例）；⑤ §3.6 HTTP 触发 — 新增 #24 POST 标题（原为无标题直接展开，现加 `#### #24 POST /api/v1/trigger/{flowId}/invoke — HTTP 触发`）；⑥ §3.7 执行历史 — 新增 #26 GET 执行详情（含 steps 数组完整示例）。**变更统计**：941 → 1472 行（+531 行，15 个新增接口 + 编号追加），接口覆盖率 26/26 | SDDU Plan Agent |
| **v5.0** | **2026-05-22** | **对齐 spec.md v5.0 MVP 单版本模型精简**：① 端点总数 26→18；② 移除版本列表/发布/部署/手动触发/执行历史 共 8 个端点；③ 版本管理端点点改为直接 `.../config` 路径；④ 所有英文、中文标题、编号、FR 引用对齐 v5.0；⑤ §0 版本对齐表重写；⑥ 修订记录追加 v5.0 条目。**变更统计**：~1335 → 887 行（-448 行） | SDDU Plan Agent |
| **v3.0** | **2026-05-26** | **对齐 plan-json-schema.md v5.5**：① JSON 示例全面重写——连接器配置 `connectionConfig` 字段重命名（`authTypeSchema`→`authConfig` / `inputSchema`→`inputContract` / `outputSchema`→`outputContract` / `rateLimit`→`rateLimitConfig`）+ `inputContract`/`outputContract` 改为协议感知格式（HTTP: header/query/body）；② 编排配置 `orchestrationConfig` 全面重写为 React Flow 格式（`node.data` 嵌套 + `edge.source`/`target`）+ `inputMapping`/`outputMapping` 分段结构 + 表达式升级为 JSON Path（`${$.node.{id}.{input/output}.xxx}`）；③ §1.8.3 triggerType 拆为两个维度——编排 JSON（字符串 http/manual）vs 执行记录（TINYINT 含 test=3）；④ §1.8.6 nodeType 新增两个维度说明；⑤ 认证枚举段更新 `authTypeSchema`→`authConfig`；⑥ §3.4 末尾新增交叉引用块。 | SDDU Plan Agent |
| **v3.1** | **2026-05-27** | **对齐 plan-json-schema.md v5.6 映射字段结构化重构**：① #15 GET config 响应中 inputMapping/outputMapping 示例更新为 mappedJsonSchemaObjectDef 格式（每个叶子字段含 type + value）；② 表达式常量格式 `constant:xxx` → `${$.constant:xxx}` | SDDU Plan Agent |
