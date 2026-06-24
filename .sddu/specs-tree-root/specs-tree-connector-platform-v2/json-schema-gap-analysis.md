# JSON Schema 缺口分析：plan-json-schema.md (v8.4) vs spec.md (v2.16)

**创建日期**: 2026-06-11  
**状态**: ✅ 全部完成 (10/10)  
**对比文档**:
- 需求规范: `spec.md` (v2.16)
- 设计方案: `plan-json-schema.md` (v8.4)
- V1 基线: `../specs-tree-connector-platform/plan-json-schema.md` (v5.6)

---

## 第一部分：功能缺口

### ✅ GAP-01: Cookie + SIGNATURE 认证类型 — 已解决 (2026-06-11)

| 维度 | 详情 |
|------|------|
| **Spec 要求** | FR-012/013: 新增 COOKIE、SIGNATURE（数字签名）认证类型 |
| **解决方案** | 随 authConfigDef v2 重构一并完成：type enum 新增 `COOKIE` / `SIGNATURE`；字段结构从自定 `fields[]` 改为 `header`/`query` 复用 jsonObjectDef |
| **影响位置** | §4.3.2 完全重写 + 7 种认证类型示例；§4.4 聚合 JSON；§2.2 枚举对应表新增 2 行；§5.4 / §6.7 示例更新 |
| **决策记录** | Cookie 字段名即 HTTP 字段名（如 `SESSION_ID`），值由编排 inputMapping 映射；SIGNATURE 的签名密钥加密落库，设计态可解密查看，运行时解密签名，密钥 + 签名值双脱敏 |

### ✅ GAP-07: 认证多选组合（authConfig → authConfigs）— 已记录方案，随 GAP-01 一并处理

| 维度 | 详情 |
|------|------|
| **解决方案** | 随 authConfigDef v2 重构，字段名 `authConfig` 保持不变（暂不改数组），多选数组化（authConfigs）标记为后续优化项，当前单认证结构已满足 V2 spec 核心需求 |
| **决策记录** | GAP-01 先落地重构方案，多选数组化可与 GAP-07 独立推进 |

### ✅ GAP-02: triggerNodeDataDef 缺少 SYSTOKEN 白名单 — 已解决 (2026-06-11)

| 维度 | 详情 |
|------|------|
| **Spec 要求** | FR-036: 触发器选 SYSTOKEN 认证后需配置凭证白名单，空白名单=全部禁止 (EC-011) |
| **解决方案** | 在 `authConfigDef` 内部新增 `sysAccountWhitelist: string[]` 字段 + allOf 条件 |
| **影响位置** | §4.3.2 authConfigDef Def / 字段说明表 / 示例；§4.4 聚合 JSON |
| **决策记录** | 用户建议将白名单放入 authConfig 内部而非 triggerNodeDataDef，语义内聚更优 |

### ✅ GAP-03: dataProcessorNodeDataDef 配置过于简化 — 已解决 (2026-06-11)

| 维度 | 详情 |
|------|------|
| **解决方案** | `config.fieldMappings` 扁平映射 → `output: jsonObjectDef`，值来源三种由 §3 表达式体系覆盖，函数递归嵌套由表达式语法原生支持。同时 connector `inputMapping` → `input`，exit `outputMapping` → `output` |
| **影响位置** | §4.3.9/10/11 Def + 示例；§4.4 聚合 JSON；组件速查表；附录 C |

### ✅ GAP-04: §3.5 缺少类型转换函数 — 已解决 (2026-06-11)

| 维度 | 详情 |
|------|------|
| **Spec 要求** | FR-040: 本期支持四种函数 `toString` / `toNumber` / `toBoolean` / `formatDate` |
| **解决方案** | 新增 2 个类别（类型转换/日期）共 4 个函数；全部 18 个函数的类路径统一对齐到实际项目包 `com.xxx.it.works.wecode.v2.modules.runtime.fn.{category}` |
| **影响位置** | §3.5 函数表（14 旧路径替换 + 4 新行）；§3.2 runtime context system.fn（6 旧路径替换 + 4 新条目） |
| **决策记录** | `formatDate` 归入独立 `date` 类别而非 `convert`（用户指出格式转换≠类型转换）；签名：`formatDate(value, fromFormat, toFormat)`；现有 14 个函数的 `com.openapp.fn.*` 占位符一并修正 |

### ✅ GAP-05: orchestrationConfig 缺少顶层 flowConfig — 已解决 (2026-06-11)

| 维度 | 详情 |
|------|------|
| **Spec 要求** | §3.7: flowConfig（入站限流、缓存）嵌入 FlowVersion 快照 |
| **解决方案** | orchestrConfig 新增可选 `flowConfig` 对象，含 `rateLimitConfig`（复用 rateLimitConfigDef）和 `cache`（key + ttl）。cache.key 为 string[]，运行时按序解析后以冒号(:)拼接 |
| **影响位置** | §6.4 orchestrationConfig 完整 Schema |
| **决策记录** | flowConfig / rateLimitConfig / cache 均为 optional（非必须流都需要限流/缓存）；cache.key 采用数组方案 B（每个元素独立表达式），分隔符由引擎默认 `:` 拼接，用户无感；明确放弃方案 A（concat 函数）和自定义分隔符 |

### ✅ GAP-06: connectorNodeDataDef 缺少节点超时 — 已解决 (2026-06-11)

| 维度 | 详情 |
|------|------|
| **Spec 要求** | FR-034 / §1.6: 超时属于连接流，每个 connector 节点需配置 timeoutMs |
| **解决方案** | 在 `connectorNodeDataDef` 新增 `timeoutMs: integer`（0~300000，默认 0=不限制走系统默认）。运行时取 min(该值, 系统上限) |
| **影响位置** | §4.3.9 Def / 字段说明表；§4.4 聚合 JSON connectorNodeDataDef |
| **决策记录** | 放在节点层而非 flowConfig 层——不同节点可设不同超时，符合 §1.6 "超时属于连接流" |

### ✅ GAP-07: 认证多选组合（authConfig → authConfigs 数组化）— 已解决 (2026-06-11)

| 维度 | 详情 |
|------|------|
| **解决方案** | `connectionConfig` 和 `triggerNodeDataDef` 的 `authConfig` 改为 `authConfigs: array, minItems:1, items: $ref authConfigDef`；allOf 条件同步更新 |
| **影响位置** | §5.2 connectionConfig / §4.3.8 triggerNodeDataDef / §4.4 聚合 JSON / §5.4 示例 / §6.7 示例 |

---

## 第二部分：设计原则违反

### ✅ VIOLATION-01 + VIOLATION-02: structConfig 规范化 + 枚举统一 — 已解决 (2026-06-11)

| 维度 | 详情 |
|------|------|
| **变更** | ① nodeDataBaseDef 删除 `structConfig`（非基类共有的关注点）<br>② structureNodeDataDef / textMarkerNodeDataDef 的 `config` → `structConfig`，描述明确为"React Flow DAG 拓扑配置"<br>③ 全文档枚举统一为 kebab-case: `loop-v2` / `parallel` / `condition-branch` / `error-handler`<br>④ nodeDataDef 路由 7 分支 → 6 分支（4 种结构类型共享 structureNodeDataDef） |
| **影响位置** | §4.3.7 nodeDataBaseDef / §4.3.12 structureNodeDataDef / §4.3.13 textMarkerNodeDataDef / §4.4 聚合 JSON / §6.2.1 / §6.3.1 路由表 / 附录 C |

### ✅ VIOLATION-03: nullable 说明不清 — 已解决 (2026-06-11)

| 维度 | 详情 |
|------|------|
| **根因** | 旧描述"是否允许 null 值"导致误解为类型系统中引入 null 类型 |
| **实际含义** | nullable 控制字段**值**是否可为 null，与 FR-047 禁止声明 `type="null"` 是两层概念，无冲突 |
| **解决方案** | 补充描述说明："与 FR-047 禁止 type='null' 无关，nullable 仅控制值的可选性" |

---

## 第三部分：文档错误

### ✅ DOC-01/02: 文档修正 — 随重构自然解决 (2026-06-11)

| 维度 | 详情 |
|------|------|
| **DOC-01** 章节引用号错位 | §4.3.14 路由表章节引用已随各节重构更新 |
| **DOC-02** 分支计数错误 | "7 分支" → "6 分支" 已在 nodeDataDef 描述中修正 |

---

## 讨论顺序建议

| 优先级 | 编号 | 议题 | 状态 | 需要决策 |
|:---:|------|------|:---:|:---:|
| ~~2~~ | ~~GAP-02~~ | ~~SYSTOKEN 白名单~~ | ✅ | — |
| ~~3~~ | ~~GAP-04~~ | ~~类型转换函数~~ | ✅ | — |
| ~~4~~ | ~~GAP-05~~ | ~~顶层 flowConfig~~ | ✅ | — |
| ~~5~~ | ~~GAP-06~~ | ~~连接器节点超时~~ | ✅ | — |
| — | ~~GAP-01~~ | ~~Cookie + SIGNATURE 认证~~ | ✅ | authConfigDef v2 重构完成 |
| — | ~~GAP-07~~ | ~~认证多选数组化~~ | ✅ | authConfig → authConfigs |
| — | ~~GAP-03~~ | ~~data_processor 配置~~ | ✅ | output → jsonObjectDef |
| — | ~~VIOLATION-03~~ | ~~nullable 说明~~ | ✅ | 描述澄清，非冲突 |
| — | ~~VIOLATION-01~~ | ~~预留槽 → structConfig 规范化~~ | ✅ | nodeDataBaseDef 删除；structure/textMarker 重命名为 structConfig |
| — | ~~VIOLATION-02~~ | ~~枚举命名统一~~ | ✅ | 全文档统一 kebab-case |
| — | ~~DOC-01/02~~ | ~~文档修正~~ | ✅ | 随重构自然解决 |

---

**全部 10 项议题已完成。**
