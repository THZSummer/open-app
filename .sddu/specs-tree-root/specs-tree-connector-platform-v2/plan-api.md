# API 接口设计：连接器平台 V2

**Feature ID**: CONN-PLAT-002  
**关联文档**: plan.md（§4.1 管理面 + §4.2 运行时），plan-db.md（§3 表结构），plan-json-schema.md（JSON 结构定义）  
**版本**: v1.0  
**创建日期**: 2026-06-09  
**对齐基线**: spec.md v2.15-draft

---

## 0. 设计规范

> 💡 V2 全面沿用 V1 `plan-api.md §1` 已确立的 API 设计规范（基础路径 `/service/open/v2` / `/api/v1`、camelCase 命名、BIGINT→string、TINYINT 枚举返数字、ISO 8601 时间格式、kebab-case URL 路径）。本章仅列出 V2 新增接口。

### 0.1 V1→V2 端点总数

| 模块 | V1 | V2 新增 | V2 总数 |
|------|:--:|:--:|:--:|
| 连接器管理 | 5 | +8 | 13 |
| 连接流管理 | 7 | +12 | 19 |
| 审批管理 | 0 | +4 | 4 |
| 安全配置 | 0 | +3 | 3 |
| 运行时 | 2 | +3 | 5 |
| 调试 | 0 | +1 | 1 |
| **合计** | **14** | **+31** | **45** |

---

## 1. 连接器管理接口（open-server）

**基础路径**: `/service/open/v2/connectors`

### 1.1 连接器实体

| 方法 | 路径 | 说明 | 对应 FR |
|------|------|------|:---:|
| POST | `/connectors` | 创建连接器（自动生成空草稿版本） | FR-001 |
| PUT | `/connectors/{id}` | 编辑连接器基本信息 | — |
| GET | `/connectors/{id}` | 查看连接器详情 | — |
| GET | `/connectors` | 连接器列表（按 app_id 过滤） | G13 |
| PUT | `/connectors/{id}/invalidate` | 标记失效（校验无流引用） | FR-003 |
| PUT | `/connectors/{id}/restore` | 恢复连接器 | FR-002 |
| DELETE | `/connectors/{id}` | 物理删除（仅已失效状态） | FR-004 |

### 1.2 连接器版本

| 方法 | 路径 | 说明 | 对应 FR |
|------|------|------|:---:|
| GET | `/connectors/{id}/versions` | 版本列表 | FR-008 |
| GET | `/connectors/{id}/versions/{vid}` | 版本详情（只读快照） | FR-008 |
| PUT | `/connectors/{id}/versions/{vid}` | 编辑草稿保存 | FR-005 |
| PUT | `/connectors/{id}/versions/{vid}/publish` | 发布版本 | FR-007 |
| POST | `/connectors/{id}/versions/{vid}/copy-to-draft` | 复制已发布版本到草稿 | FR-006 |
| PUT | `/connectors/{id}/versions/{vid}/invalidate` | 标记版本失效（校验无流引用） | FR-009 |
| PUT | `/connectors/{id}/versions/{vid}/restore` | 恢复版本 | FR-011 |
| DELETE | `/connectors/{id}/versions/{vid}` | 删除版本（仅已失效状态） | FR-010 |

### 1.3 连接器配置（认证 + URL 白名单）

| 方法 | 路径 | 说明 | 对应 FR |
|------|------|------|:---:|
| GET | `/connectors/{id}/url-whitelist` | 查询 URL 白名单规则列表 | FR-015 |
| POST | `/connectors/{id}/url-whitelist` | 新增白名单规则 | FR-015 |
| DELETE | `/connectors/{id}/url-whitelist/{rid}` | 删除白名单规则 | FR-015 |

### 1.4 请求/响应体示例

**POST /connectors**（创建连接器）：

```json
// 请求
{
  "nameCn": "IM 发送消息",
  "nameEn": "IM Send Message",
  "descriptionCn": "通过内部 IM 发送消息",
  "connectorType": 1,
  "appId": "1234567890123456789"
}

// 响应
{
  "code": 0,
  "data": {
    "id": "9876543210987654321",
    "nameCn": "IM 发送消息",
    "status": 1,
    "draftVersion": {
      "id": "1111111111111111111",
      "versionNumber": 1,
      "status": 1
    }
  }
}
```

**PUT /connectors/{id}/versions/{vid}/publish**（发布版本）：

```json
// 请求：无 body（或空 body）
// 响应
{
  "code": 0,
  "data": {
    "versionId": "1111111111111111111",
    "versionNumber": 1,
    "status": 2,
    "publishedTime": "2026-06-09T10:00:00.000+08:00"
  }
}
// 错误
{
  "code": 400,
  "message": "草稿配置为空，请先完善连接配置"  // EC-009
}
```

---

## 2. 连接流管理接口（open-server）

**基础路径**: `/service/open/v2/flows`

### 2.1 连接流实体

| 方法 | 路径 | 说明 | 对应 FR |
|------|------|------|:---:|
| POST | `/flows` | 创建连接流（自动生成空草稿版本） | FR-016 |
| GET | `/flows` | 连接流列表（按 app_id 过滤） | G13 |
| GET | `/flows/{id}` | 连接流详情 | — |
| PUT | `/flows/{id}` | 编辑连接流基本信息 | — |
| POST | `/flows/{id}/copy` | 一键复制连接流 | FR-017 |
| POST | `/flows/{id}/deploy` | 部署+启动（选择已发布版本） | FR-018 |
| POST | `/flows/{id}/start` | 启动（已停止→运行中） | FR-019 |
| POST | `/flows/{id}/stop` | 停止（运行中→已停止） | FR-020 |
| PUT | `/flows/{id}/invalidate` | 标记失效 | FR-022 |
| PUT | `/flows/{id}/restore` | 恢复连接流（→已停止） | FR-021 |
| DELETE | `/flows/{id}` | 物理删除（仅已失效状态） | FR-023 |

### 2.2 连接流版本

| 方法 | 路径 | 说明 | 对应 FR |
|------|------|------|:---:|
| GET | `/flows/{id}/versions` | 版本列表 | FR-027 |
| GET | `/flows/{id}/versions/{vid}` | 版本详情（只读快照，含完整编排） | FR-027 |
| PUT | `/flows/{id}/versions/{vid}` | 编辑草稿保存 | FR-024 |
| POST | `/flows/{id}/versions/{vid}/submit-approval` | 提交审批（草稿→待审批） | FR-026 |
| POST | `/flows/{id}/versions/{vid}/copy-to-draft` | 复制已发布版本到草稿 | FR-025 |
| PUT | `/flows/{id}/versions/{vid}/invalidate` | 标记版本失效（校验未部署） | FR-028 |
| PUT | `/flows/{id}/versions/{vid}/restore` | 恢复版本 | FR-030 |
| DELETE | `/flows/{id}/versions/{vid}` | 删除版本（仅已失效状态） | FR-029 |

### 2.3 运行记录

| 方法 | 路径 | 说明 | 对应 FR |
|------|------|------|:---:|
| GET | `/flows/{id}/executions` | 运行记录列表（分页+过滤） | FR-042 |
| GET | `/flows/{id}/executions/{eid}` | 运行记录详情（含各节点日志） | FR-042 |

### 2.4 请求/响应体示例

**POST /flows/{id}/copy**（一键复制）：

```json
// 请求：无 body
// 响应
{
  "code": 0,
  "data": {
    "id": "9999999999999999999",
    "nameCn": "IM 发送消息_copy_a3f2b",
    "nameEn": "IM Send Message_copy_a3f2b",
    "lifecycleStatus": 1,
    "versionsCopied": 5
  }
}
// 错误：名称碰撞重试后仍冲突
{
  "code": 409,
  "message": "复制名称冲突，请稍后重试"
}
```

**POST /flows/{id}/deploy**（部署）：

```json
// 请求
{
  "versionId": "2222222222222222222"
}
// 响应
{
  "code": 0,
  "data": {
    "flowId": "3333333333333333333",
    "deployedVersionId": "2222222222222222222",
    "lifecycleStatus": 2,
    "triggerUrl": "https://xxx/api/v1/trigger/flow/3333333333333333333"
  }
}
```

**GET /flows/{id}/executions**（运行记录列表）：

```json
// 请求 Query: ?status=3&triggerType=1&startTime=xxx&endTime=xxx&page=1&size=20
// 响应
{
  "code": 0,
  "data": {
    "total": 150,
    "items": [
      {
        "id": "4444444444444444444",
        "triggerTime": "2026-06-09T10:00:01.000+08:00",
        "status": 3,
        "durationMs": 234,
        "triggerType": 1,
        "flowVersionNumber": 3
      }
    ]
  }
}
```

---

## 3. 审批管理接口（open-server）

**基础路径**: `/service/open/v2/connector-platform/approvals`

| 方法 | 路径 | 说明 | 对应 FR |
|------|------|------|:---:|
| POST | `/approvals/{versionId}/urge` | 一键催办（向当前审批节点审批人发送通知） | FR-033 |
| GET | `/approvals/{versionId}/status` | 查询审批状态（当前节点、已审批/待审批人） | FR-031 |
| GET | `/approver-configs` | 查询审批人配置列表 | FR-032 |
| PUT | `/approver-configs` | 更新审批人配置（平台管理员） | FR-032 |

**审批状态查询响应**：

```json
{
  "code": 0,
  "data": {
    "versionId": "5555555555555555555",
    "versionStatus": 2,
    "approvalNodes": [
      { "level": 1, "levelName": "应用级", "status": 1, "approvers": ["uid_a"], "approvedBy": ["uid_a"] },
      { "level": 2, "levelName": "平台连接流级", "status": 0, "approvers": ["uid_b", "uid_c"], "approvedBy": [] },
      { "level": 3, "levelName": "全局级", "status": 0, "approvers": ["uid_d"], "approvedBy": [] }
    ]
  }
}
```

> 💡 审批提交/通过/驳回/撤回复用现有 `ApprovalController` 接口（`/service/open/v2/approvals/*`），不新增专用端点。提交审批时系统调用 `ApprovalEngine.createApproval()` 创建审批实例。

---

## 4. 安全配置接口（open-server）

**基础路径**: `/service/open/v2/connector-platform/admin`

| 方法 | 路径 | 说明 | 对应 FR |
|------|------|------|:---:|
| GET | `/app-whitelist` | 查询应用白名单列表 | FR-045 |
| POST | `/app-whitelist` | 添加应用到白名单 | FR-045 |
| DELETE | `/app-whitelist/{appId}` | 将应用移出白名单 | FR-045 |

---

## 5. 操作日志接口（open-server — 复用现有）

> 💡 复用现有操作日志接口 `/service/open/v2/operate-logs`。V2 扩展 `OperateEnum` 枚举值，在连接器/连接流详情页提供「操作日志」Tab，按 `operateObject` 过滤。

---

## 6. 运行时接口（connector-api）

**基础路径**: `/api/v1`

| 方法 | 路径 | 说明 | 对应 FR |
|------|------|------|:---:|
| POST | `/trigger/flow/{flowId}` | HTTP 触发连接流执行 | G11 |
| POST | `/debug/execute` | 调试触发（同步执行） | FR-041 |
| GET | `/health` | 健康检查 | — |

### 6.1 调试触发

**POST /api/v1/debug/execute**：

```json
// 请求
{
  "flowId": "3333333333333333333",
  "versionId": "6666666666666666666",
  "triggerData": {
    "sender": "u001",
    "content": "测试消息"
  }
}
// 响应（同步，等待完整执行结果）
{
  "code": 0,
  "data": {
    "executionId": "7777777777777777777",
    "status": 2,
    "durationMs": 1450,
    "nodes": [
      {
        "nodeId": "trigger_1",
        "nodeType": "trigger",
        "status": 2,
        "durationMs": 2,
        "output": { "sender": "u001", "content": "测试消息" }
      },
      {
        "nodeId": "connector_1",
        "nodeType": "connector",
        "status": 2,
        "durationMs": 234,
        "output": { "msgId": "m001", "code": 0 }
      }
    ]
  }
}
```

### 6.2 HTTP 触发

**POST /api/v1/trigger/flow/{flowId}**：

- 请求体：触发器入参 JSON（对齐 trigger.inputContract 定义）
- 响应体：exit 节点 outputMapping 定义的结构
- 认证：请求头携带 SYSTOKEN（对齐 trigger.authConfig 定义）
- 错误响应：
  - 401 → SYSTOKEN 不在白名单
  - 429 → 入站限流
  - 500 → 执行异常
  - 503 → Flow 未部署
