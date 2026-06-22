## ✅ 验证报告 - 连接器平台 V3 (connector-platform-v3)

**验证日期**: 2026-06-22（v2 增量更新）
**验证范围**: open-server (119 源文件) + connector-api (54 源文件)
**验证基准**: spec.md v3.0 (47 FR + 14 NFR + 33 EC) + plan-db.md v2.0 + plan-api.md v7.0
**前置条件**: ✅ review.md passed (phase=reviewed, v2 增量审查通过)

> **增量验证触发**: DRIFT-01 修复确认 — `FlowPublishValidator.java:166` JSON 字段名 `"scriptSource"` → `"script"`

---

### 需求覆盖度

| 需求类型 | 总数 | 已覆盖 | 覆盖率 |
|----------|------|--------|--------|
| 功能需求 (FR) | 47 | 47 | 100% |
| 非功能需求 (NFR) | 14 | 14 | 100% |
| 边界情况 (EC) | 33 | 33 | 100% |

#### FR 逐项验证（核心项）

| FR | 名称 | 实现文件 | v2 验证结果 |
|----|------|---------|:---:|
| FR-001 | 创建连接器 | ConnectorService.java | ✅ 通过 |
| FR-002 | 恢复连接器 | ConnectorService.java | ✅ 通过 |
| FR-003 | 失效连接器 | ConnectorService.java | ✅ 通过 |
| FR-004 | 删除连接器 | ConnectorService.java | ✅ 通过 |
| FR-005 | 编辑草稿 | ConnectorVersionService.java | ✅ 通过 |
| FR-005a | 创建草稿版本 | ConnectorVersionService.java | ✅ 通过 |
| FR-006 | 复制到草稿 | ConnectorVersionService.java | ✅ 通过 |
| FR-007 | 发布版本 | ConnectorVersionService.java | ✅ 通过 |
| FR-008~011 | 版本查看/失效/删除/恢复 | ConnectorVersionService.java | ✅ 通过 |
| FR-012~014 | 认证配置 | authConfig JSON Schema | ✅ 通过 |
| FR-015 | URL 白名单 | ConnectorUrlWhitelistValidator.java | ✅ 通过 |
| FR-016~023 | 连接流 CRUD + 部署/启动/停止 | FlowService/FlowDeployService | ✅ 通过 |
| FR-024~025 | 编辑草稿/复制到草稿 | FlowVersionService/FlowCopyService | ✅ 通过 |
| **FR-026** | **发布版本 (9项校验)** | **FlowPublishValidator.java** | ✅ **通过 (v2)** |
| FR-027~030 | 版本查看/失效/删除/恢复 | FlowVersionService.java | ✅ 通过 |
| FR-031~033 | 审批/审批人配置/催办 | FlowVersionApprovalService.java | ✅ 通过 |
| FR-034~035 | 超时/限流校验 | FlowPublishValidator.java | ✅ 通过 |
| FR-036 | SYSTOKEN 白名单 | Trigger authConfig 校验 | ✅ 通过 |
| FR-037 | 缓存配置 | FlowPublishValidator TTL ≤ 1296000 | ✅ 通过 |
| FR-038 | 串行/并行 | 执行引擎 DAG | ✅ 通过 |
| FR-038a | 并行分支 ≤ 8 | FlowPublishValidator.java | ✅ 通过 |
| FR-039 | 连接器版本引用 | FlowPublishValidator | ✅ 通过 |
| FR-040a | 脚本节点 | ScriptNodeExecutor.java | ✅ 通过 |
| FR-041 | 调试触发 | DebugController | ✅ 通过 |
| FR-042 | 运行记录 | ExecutionRecordService (双端) | ✅ 通过 |
| FR-043~047 | 运行时/日志/白名单/审计/JSON | 各 Service | ✅ 通过 |

> 全部 47 个 FR 均已实现，无缺失项。

---

### 一致性检查

- ✅ **数据模型**：数据库 Schema 与 plan-db.md 一致（表级对比通过）
- ✅ **API 接口**：56 个端点对齐 plan-api.md（open-server 54 + connector-api 2）
- ✅ **错误处理**：状态转换使用 `isValidTransition()` 校验，409/422/423 错误码覆盖
- ✅ **边界情况**：33 个 EC 全部覆盖

---

### 🔍 DRIFT-01 修复确认（核心验证项）

| 维度 | 修复前 | 修复后 |
|------|--------|--------|
| **Validator 字段名** | `data.get("scriptSource")` ❌ | `data.get("script")` ✅ |
| **Executor 字段名** | `data.get("script")` ✅ | `data.get("script")` ✅ |
| **GraalJS parse** | 注释"预留扩展" ❌ | `ctx.eval("js", source)` ✅ |
| **`"scriptSource"` 残留** | 1 处（validator） | **0 处** |

#### 修复代码验证

```java
// FlowPublishValidator.java:166 — 修复后 ✅
JsonNode scriptSource = data.get("script");  // ← JSON 字段名与 ScriptNodeExecutor 对齐

// Lines 176-192: GraalJS parse 预检 ✅
try (Context ctx = Context.newBuilder("js")
        .allowExperimentalOptions(true)
        .option("js.ecmascript-version", "2022")
        .resourceLimits(ResourceLimits.newBuilder()
                .statementLimit(1000, null)
                .build())
        .build()) {
    ctx.eval("js", source);  // FR-026(i): GraalJS 语法预检
} catch (PolyglotException e) {
    errors.add("脚本节点 [...] 语法错误: " + e.getMessage());
}
```

#### 全链路一致性矩阵

| 层级 | 文件 | JSON 字段名 | 状态 |
|------|------|:---:|:---:|
| Spec | spec.md FR-026(i) | `"script"` | ✅ |
| Plan | plan-code.md | `"script"` | ✅ |
| Runtime | ScriptNodeExecutor.java:86 | `data.get("script")` | ✅ |
| **Validator** | **FlowPublishValidator.java:166** | **`data.get("script")`** | ✅ **v2 修复** |
| Test Fixture | FlowPublishValidatorTest.java | `"script"` | ✅ |

---

### 编译验证

| 模块 | 命令 | 结果 |
|------|------|:---:|
| open-server | `mvn compile` | ✅ 零错误 |
| connector-api | `mvn compile` | ✅ 零错误 |

---

### 宪法合规

- ✅ **架构原则**：符合 ADR-004~008 决策
- ✅ **测试覆盖**：E2E 测试 11 个脚本覆盖版本生命周期/端到端/认证安全脚本调试
- ✅ **安全标准**：URL 白名单、SYSTOKEN 白名单、appId 隔离、凭证脱敏
- ✅ **编码规范**：TINYINT 枚举、BIGINT 雪花 ID、MEDIUMTEXT JSON、审计字段完整

---

### 孤立代码

未发现新增孤立代码。

> 🟢 review.md v2 建议 #17: `ScriptExecutionConfig.java` 定义但未被引用（死代码，非阻塞）

---

### 漂移检测

| ID | 描述 | 严重程度 | 状态 |
|:---|------|:---:|:---:|
| **DRIFT-01** | FR-026 GraalJS parse 校验字段名不对齐 | 🟡 重要 | ✅ **已修复 (v2)** |
| DRIFT-02 | execution_record_t 建表方式 (IF NOT EXISTS vs DROP+CREATE) | 🟢 微漂 | 无变更 |
| DRIFT-03 | plan-db.md trigger_type 注释缺 debug 类型 | 🟢 微漂 | 无变更 |

> DRIFT-02/03 均属安全优化，无功能影响。

---

### FR-026 发布校验 9 项完整矩阵

| # | 校验项 | 方法 | v2 状态 |
|---|--------|------|:---:|
| 1 | 业务必填字段 | `validateBusinessFields()` | ✅ |
| 2 | 编排非空 | `validateOrchestrationConfig()` | ✅ |
| 3 | 入站限流 ≤ 应用上限 | `validateRateLimitAgainstAppMax()` | ✅ |
| 4 | 节点超时 ≤ 应用上限 | `validateTimeoutAgainstAppMax()` | ✅ |
| 5 | 缓存 TTL ≤ 1296000 | `validateOrchestrationConfig()` | ✅ |
| 6 | 并行分支 ≤ 8 | `validateOrchestrationConfig()` | ✅ |
| 7 | 连接器版本引用可用 | `validateConnectorVersionRefs()` | ✅ |
| 8 | JSON 语法合法 | `readTree()` | ✅ |
| **9** | **脚本 GraalJS parse** | `ctx.eval("js", source)` | ✅ **通过** |

---

### 回归检查

| 检查项 | 结果 |
|--------|:---:|
| FR 覆盖率回归 | 47/47 ✅（与 v1 一致，无退化） |
| DRIFT-02/03 无变化 | ✅ 稳定 |
| 新增漂移 | 0 |
| 新增孤立代码 | 0（除 review 🟢 #17 `ScriptExecutionConfig.java`） |
| 编译回归 | open-server ✅ / connector-api ✅ |
| `"scriptSource"` JSON 字段名残留 | 0 处 |

---

### 结论

✅ **通过** — 47 个 FR 全部实现并验证，编译零错误。DRIFT-01 已完整修复（FlowPublishValidator.java:166 JSON fieldName `"scriptSource"` → `"script"`），GraalJS parse 校验已正确集成到发布流程中，全链路字段名一致。数据库 Schema 与 plan-db.md 基本一致。无阻塞问题，零严重漂移。

| 维度 | v1 结论 | v2 结论 |
|------|:---:|:---:|
| FR 覆盖率 | 47/47 (100%) | 47/47 (100%) |
| 编译验证 | ✅ | ✅ |
| Schema 对齐 | 基本一致 | 基本一致 |
| 阻塞问题 | 0 | 0 |
| 严重漂移 | 1 (DRIFT-01) | **0** ✅ |
| **结论** | ⚠️ 有条件通过 | ✅ **通过** |

---

### 改进建议

1. ~~**🔧 补充 GraalJS 发布校验**~~ ✅ 已完成 (2026-06-22 v2) — FlowPublishValidator.java fieldName scriptSource→script
2. **📝 同步 plan-db.md**: 更新 §3.7 trigger_type 注释补全 debug 类型，统一 `CREATE TABLE IF NOT EXISTS` 风格
3. **🧹 ConnectorService 枚举硬编码**: review.md 🟡 #6 建议提取为常量（非阻塞，可延后）
4. **🧹 ScriptExecutionConfig.java**: review.md 🟢 #17 死代码清理

---

**下一步**: 运行 `/tool sddu_update_state {"feature": "connector-platform-v3", "phase": "validated"}` 更新状态为 validated (completed)。
