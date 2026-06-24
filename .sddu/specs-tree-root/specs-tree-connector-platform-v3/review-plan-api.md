# plan-api.md 实施对照审查报告

**审查日期**: 2026-06-23
**审查范围**: plan-api.md v7.0 定义的 56 个端点 vs 实际 Java Controller 代码
**审查人**: SDDU 路由调度专家

---

## 一、总体统计

| 类别 | 规划 | 实际 | 状态 |
|------|:--:|:--:|:--:|
| 连接器 CRUD (#1~#7) | 7 | 7 | ✅ |
| 连接器版本 (#8~#16) | 9 | 9 | ✅ |
| 连接流 CRUD (#17~#27) | 11 | 11 | ✅ |
| 连接流版本 (#28~#38) | 11 | 11 | ✅ |
| 审批记录 (#39~#44) | 6 | 6 | ✅ |
| 审批流模板 (#45~#48) | 4 | 4 | ✅ |
| 运行记录 (#49~#50) | 2 | 2 | ⚠️ #50 参数名偏差 |
| 调试代理 (#51) | 1 | 1 | 🔴 路径不符 |
| 调试执行 (#53) | 1 | 1 | 🔴 路径不符 |
| 调用连接流 (#54) | 1 | 1 | 🔴 路径不符 |
| 函数列表 (#52) | 1 | 0 | 🟢 无需实施 |
| 系统配置 (#54~#55) | 2 | 0 | 🟢 复用 Property |
| V1 残留 | — | 5 | 🟡 待删除 |
| **合计** | **56** | **54** | 38 匹配 + 5 待修复 + 3 无需实施 + 5 待删除 |

---

## 二、✅ 完全匹配（38 端点）

### 2.1 连接器 CRUD `ConnectorController.java` → `/service/open/v2/connectors`

| # | 方法 | 路径 | 状态 |
|---|------|------|:--:|
| 1 | POST | `/connectors` | ✅ |
| 2 | GET | `/connectors` | ✅ |
| 3 | GET | `/connectors/{connectorId}` | ✅ |
| 4 | PUT | `/connectors/{connectorId}` | ✅ |
| 5 | PUT | `/connectors/{connectorId}/invalidate` | ✅ |
| 6 | PUT | `/connectors/{connectorId}/recover` | ✅ |
| 7 | DELETE | `/connectors/{connectorId}` | ✅ |

### 2.2 连接器版本 `ConnectorVersionController.java` → `/service/open/v2/connectors`

| # | 方法 | 路径 | 状态 |
|---|------|------|:--:|
| 8 | POST | `/connectors/{connectorId}/versions` | ✅ |
| 9 | GET | `/connectors/{connectorId}/versions` | ✅ |
| 10 | GET | `/connectors/{connectorId}/versions/{versionId}` | ✅ |
| 11 | PUT | `/connectors/{connectorId}/versions/{versionId}` | ✅ |
| 12 | PUT | `/connectors/{connectorId}/versions/{versionId}/publish` | ✅ |
| 13 | POST | `/connectors/{connectorId}/versions/{versionId}/copy-to-draft` | ✅ |
| 14 | PUT | `/connectors/{connectorId}/versions/{versionId}/invalidate` | ✅ |
| 15 | PUT | `/connectors/{connectorId}/versions/{versionId}/recover` | ✅ |
| 16 | DELETE | `/connectors/{connectorId}/versions/{versionId}` | ✅ |

### 2.3 连接流 CRUD `FlowController.java` → `/service/open/v2/flows`

| # | 方法 | 路径 | 状态 |
|---|------|------|:--:|
| 17 | POST | `/flows` | ✅ |
| 18 | GET | `/flows` | ✅ |
| 19 | GET | `/flows/{flowId}` | ✅ |
| 20 | PUT | `/flows/{flowId}` | ✅ |
| 21 | POST | `/flows/{flowId}/copy` | ✅ |
| 22 | POST | `/flows/{flowId}/deploy` | ✅ |
| 23 | POST | `/flows/{flowId}/start` | ✅ |
| 24 | POST | `/flows/{flowId}/stop` | ✅ |
| 25 | PUT | `/flows/{flowId}/invalidate` | ✅ |
| 26 | PUT | `/flows/{flowId}/recover` | ✅ |
| 27 | DELETE | `/flows/{flowId}` | ✅ |

### 2.4 连接流版本 `FlowVersionController.java` → `/service/open/v2/flows`

| # | 方法 | 路径 | 状态 |
|---|------|------|:--:|
| 28 | POST | `/flows/{flowId}/versions` | ✅ |
| 29 | GET | `/flows/{flowId}/versions` | ✅ |
| 30 | GET | `/flows/{flowId}/versions/{versionId}` | ✅ |
| 31 | PUT | `/flows/{flowId}/versions/{versionId}` | ✅ |
| 32 | POST | `/flows/{flowId}/versions/{versionId}/publish` | ✅ |
| 33 | POST | `/flows/{flowId}/versions/{versionId}/copy-to-draft` | ✅ |
| 34 | PUT | `/flows/{flowId}/versions/{versionId}/invalidate` | ✅ |
| 35 | PUT | `/flows/{flowId}/versions/{versionId}/recover` | ✅ |
| 36 | DELETE | `/flows/{flowId}/versions/{versionId}` | ✅ |
| 37 | POST | `/flows/{flowId}/versions/{versionId}/cancel` | ✅ |
| 38 | POST | `/flows/{flowId}/versions/{versionId}/urge` | ✅ |

### 2.5 审批记录 `ApprovalController.java` → `/service/open/v2`

| # | 方法 | 路径 | 状态 |
|---|------|------|:--:|
| 39 | GET | `/approvals/pending` | ✅ |
| 40 | GET | `/approvals/{id}` | ✅ |
| 41 | POST | `/approvals/{id}/approve` | ✅ |
| 42 | POST | `/approvals/{id}/reject` | ✅ |
| 43 | POST | `/approvals/batch-approve` | ✅ |
| 44 | POST | `/approvals/batch-reject` | ✅ |

### 2.6 审批流模板 `ApprovalController.java` → `/service/open/v2`

| # | 方法 | 路径 | 状态 |
|---|------|------|:--:|
| 45 | GET | `/approval-flows` | ✅ |
| 46 | GET | `/approval-flows/{id}` | ✅ |
| 47 | POST | `/approval-flows` | ✅ |
| 48 | PUT | `/approval-flows/{id}` | ✅ |

---

## 三、⚠️ 小偏差（1 处）

### #50 运行记录详情 — 路径参数名不一致

| 维度 | plan-api 规划 | 实际代码 |
|------|------|------|
| 文件 | — | `ExecutionRecordController.java` |
| 路径 | `GET /flows/{flowId}/executions/{executionId}` | `GET /flows/{flowId}/executions/{recordId}` |

**影响**: 极小。仅路径参数名不同，功能语义一致。

**决定**: ✅ **A — 改为 `{executionId}`**，与 plan-api 对齐。

**涉及文件**: `ExecutionRecordController.java`

**预估工时**: 0.5h

---

## 四、🔴 路径不符（3 处）

### 4.1 #51 调试代理 — 路径与参数均不符

| 维度 | plan-api 规划 | 实际代码 |
|------|------|------|
| 文件 | — | `OpDebugProxyController.java` |
| 路径 | `POST /flows/{flowId}/versions/{versionId}/debug` | `POST /flows/{flowId}/test-run` |
| 参数 | 含 `versionId` | ❌ 无 |
| 请求体 | `triggerData` | `mockTriggerData` |

**影响**: 严重。V3 多版本模型下必须指定调试版本，当前 V1 遗留的 `/test-run` 无法满足。

**决定**: ✅ **A — 按规划重构**。路径改为 `/versions/{versionId}/debug`，新增 `versionId` 参数，`mockTriggerData` → `triggerData`。同步修改 `OpDebugProxyService` 转发逻辑。

**涉及文件**: `OpDebugProxyController.java`, `OpDebugProxyService.java`

**预估工时**: 4h

---

### 4.2 #53 调试执行（connector-api）— 路径不符

| 维度 | plan-api 规划 | 实际代码 |
|------|------|------|
| 文件 | — | `OpTestRunController.java` |
| 路径 | `POST /api/v1/flows/{flowId}/versions/{versionId}/debug` | `POST /api/v1/internal/test-run/{flowId}` |
| 参数 | 含 `versionId` | ❌ 无 |

**影响**: 严重。与 #51 联动，Service 层需按 `versionId` 加载对应版本编排配置。

**决定**: ✅ **A — 同步重构**。路径改为 `/api/v1/flows/{flowId}/versions/{versionId}/debug`，新增 `versionId` 参数。

**涉及文件**: `OpTestRunController.java`, `OpTestRunService.java`

**预估工时**: 4h

---

### 4.3 #54 调用连接流 — 路径前缀不符

| 维度 | plan-api 规划 | 实际代码 |
|------|------|------|
| 文件 | — | `OpTriggerController.java` |
| 路径 | `POST /api/v1/flows/{flowId}/invoke` | `POST /api/v1/trigger/{flowId}/invoke` |
| 透明穿透 | ✅ | ✅ 已正确实现 |

**影响**: 中等。透明穿透模式（X- 响应头）已正确实现，仅路径多了 `/trigger` 段。

**决定**: ✅ **A — 改为 `/api/v1/flows/{flowId}/invoke`**。

**涉及文件**: `OpTriggerController.java`

**预估工时**: 0.5h

---

## 五、🟢 规划外（无需实施，2 组）

### 5.1 #52 数据处理函数列表

| 规划 | 实际 |
|------|------|
| `GET /service/open/v2/data-processor/functions` | ❌ 无对应 Controller |

**决定**: ✅ **V3 无需实施**。后续更新 plan-api.md 时标注移除。

**预估工时**: 0h

---

### 5.2 #54~#55 系统配置

| 规划 | 实际 |
|------|------|
| `GET/PUT /service/open/v2/app-config/{appId}` | ❌ 无对应 Controller |

**决定**: ✅ **复用现有 LookUp/Property**。market-web 已有 CRUD 前端，连接器平台运行时直接从 Property 表读取配置，无需新增独立端点。

**预估工时**: 0h

---

## 六、🟡 V1 残留端点（5 处，待删除）

plan-api §2 明确标记为「删除」的 V1 接口，实际代码中仍然存在：

| V1 端点 | 应被替代 | 残留位置 |
|------|------|------|
| `GET /connectors/{connectorId}/config` | #10 版本详情 | `OpConnectorController.java` |
| `PUT /connectors/{connectorId}/config` | #11 版本更新 | `OpConnectorController.java` |
| `GET /flows/{flowId}/config` | #30 版本详情 | `OpFlowController.java` |
| `PUT /flows/{flowId}/config` | #31 版本更新 | `OpFlowController.java` |
| `POST /flows/{flowId}/test-run` | #51 调试代理 | `OpDebugProxyController.java` |

**决定**: ✅ **A — 直接删除** 5 个旧端点方法及对应 Service 方法。

**涉及文件**: `OpConnectorController.java`, `OpFlowController.java`, `OpDebugProxyController.java`

**预估工时**: 2h

---

## 七、汇总与执行计划

| 优先级 | 编号 | 问题 | 决定 | 工时 |
|:--:|:--:|------|------|:--:|
| P0 | 4.1 | #51 调试代理路径不符 | 重构为 `/versions/{versionId}/debug` | 4h |
| P0 | 4.2 | #53 调试执行路径不符 | 同步重构 | 4h |
| P1 | 4.3 | #54 invoke 路径不符 | 改为 `/api/v1/flows/{flowId}/invoke` | 0.5h |
| P1 | 六 | V1 旧端点清理 | 删除 5 个端点 | 2h |
| P2 | 三 | #50 参数名偏差 | 改为 `{executionId}` | 0.5h |
| — | 5.1 | #52 函数列表 | 无需实施 | 0h |
| — | 5.2 | #54~#55 系统配置 | 复用 Property | 0h |
| **合计** | | | | **11h** |

**建议执行顺序**:

1. **第一轮（P0，8h）**: #51 + #53 调试链路重构，阻塞前端联调
2. **第二轮（P1，2.5h）**: #54 invoke 路径 + V1 清理 + #50 参数名修复
3. **第三轮（文档）**: 更新 plan-api.md，标注 #52 无需实施、#54~#55 复用 Property

---

*审查完成时间: 2026-06-23*
*决定确认时间: 2026-06-23*
