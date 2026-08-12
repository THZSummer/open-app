# 06 · 汇总：open-app AI 化路线图建议

> **调研目标**: G5 —— 综合调研结论，给出 open-app 从"能力开放"到"Agent-ready"的分步落地建议
> **采集日期**: 2026-08-12
> **前置报告**: 01（协议生态）、02（国内平台）、03（国际平台）、04（编排生态）、05（企业治理）

---

## 一、核心结论（五报告汇总）

### 1.1 趋势验证（G1）：行业共识 ✅ 强确认

- **国内**：钉钉（OpenAPI MCP + MCP 广场 + AI 能力中心）、飞书（OpenAPI MCP + Aily）已官方发布 MCP 能力，走"能力开放 → Agent 开放"路线。
- **国际**：Slack（Agent + CLI）、Microsoft（Copilot Studio）、Google（A2A 协议）已进入"Agent 优先"阶段，且已推进到 Agent 协作协议层。
- **结论**：**"能力开放平台 → Agent 开放平台"是行业共识**，open-app 的战略判断正确。

### 1.2 协议选型（G2）：MCP 优先，A2A 跟踪

| 协议 | 结论 |
|------|------|
| **MCP** | **P0 立即支持**——事实标准（Linux 基金会治理 + 全客户端生态 + 国内官方跟进） |
| **Function Calling** | P0 作为规范——open-app 用 JSON Schema（Tool Schema）描述能力即可被所有模型消费 |
| **A2A** | P1 跟踪——v1.0 已发布，7 家核心厂商背书，待 Agent 协作场景成熟后进入 |

### 1.3 平台对标（G3）：AI 开发者门户四要素

国际 + 国内头部平台的"AI 开放形态"收敛为四要素：
1. **工具接入协议**（MCP）——能力开放给 AI 的统一通道
2. **Agent 搭建平台**（Copilot Studio / Aily / 钉钉 AI 助理）——低代码 Agent 创建
3. **工具/技能市场**（MCP 广场 / 应用市场）——能力分发
4. **企业治理**（授权/审计/敏感度）——企业级信任底座

### 1.4 编排演进（G5）：connector-api AI 化三阶段

| 阶段 | 方向 | 对标 |
|------|------|------|
| 短期 | DAG 工作流 MCP 化（对外暴露为工具） | n8n MCP Server Trigger |
| 中期 | 自然语言 → DAG 生成 | Zapier Agents / Make AI |
| 长期 | LLM 动态编排（Agent 节点） | LangChain Agent / Coze Bot 工作流 |

### 1.5 治理升级（G4）：先发优势确认

- MCP 规范原生内置 OAuth 2.1 / 企业托管授权；open-app 已有 AKSK + 权限中心 + 动态审批流（40 表）**高度可复用**。
- 国内三家平台 AI 治理浅——open-app 的**治理先发优势可构成差异化竞争力**。

---

## 二、open-app AI 化路线图（分阶段落地建议）

### Phase 0：地基准备（已有资产，盘点对齐）

| # | 动作 | 说明 |
|---|------|------|
| 0.1 | 盘点 184 API → Tool Schema 标准化 | 每个 API 补充 tool 级描述（name/description/parameters JSON Schema） |
| 0.2 | 治理骨架映射 | AKSK ↔ MCP Client Credentials；权限中心 ↔ MCP 授权；审批流 ↔ 敏感度审批 |
| 0.3 | 定义能力分级 | 按敏感度给 API/数据分级（公开/内部/机密/绝密） |

### Phase 1：MCP 接入（P0，立即启动）—— "能力开放给 AI"

| # | 动作 | 对标 | 产出 |
|---|------|------|------|
| 1.1 | 发布官方 OpenAPI MCP Server | 钉钉 OpenAPI MCP / 飞书 OpenAPI MCP | 远程 MCP（Streamable HTTP） |
| 1.2 | 能力渐进开放 | 飞书（先云文档，再多维表格/日历） | 先 IM/Meeting 高频场景，再 Contact/Mail/Drive/Bot |
| 1.3 | MCP 授权对接 | MCP OAuth 2.1 + Enterprise-Managed Authorization | AKSK ↔ MCP 认证打通 |
| 1.4 | 部署基建 | n8n MCP Server Trigger 部署经验 | SSE/HTTP 长连接 + 反代配置（proxy_buffering off） |
| 1.5 | 开发者工具 | Slack CLI / 钉钉开源 MCP 模式 | MCP Server 脚手架 + SDK + 接入文档 |

**验证标准**：Claude / ChatGPT / Cursor 等客户端可通过 MCP 直接调用 open-app 的 IM/Meeting 能力。

### Phase 2：Agent 市场与编排（P1）—— "Agent 可发现可组合"

| # | 动作 | 对标 | 产出 |
|---|------|------|------|
| 2.1 | market 升级为 MCP 工具目录 | 钉钉 MCP 广场 / OpenAI tool search | Agent 可列举/检索的工具市场 |
| 2.2 | connector-api MCP 化 | n8n MCP Server Trigger | DAG 工作流封装为 MCP 工具 |
| 2.3 | 工具级动态授权 | OpenAI allowed_tools | 运行时工具子集授权 |
| 2.4 | 自然语言 → 编排 | Zapier Agents / Make AI | 可选：AI 生成 DAG（GraalJS 沙箱执行） |

### Phase 3：Agent 原生与治理深化（P2）—— "Agent-ready 平台"

| # | 动作 | 对标 | 产出 |
|---|------|------|------|
| 3.1 | LLM 动态编排 | LangChain Agent / Coze Bot 工作流 | 编排大脑 + Agent 节点 |
| 3.2 | 敏感度驱动审批 | 05 报告治理最佳实践 | 数据分级联动动态审批（含回调/异步审批） |
| 3.3 | 工具级全链路审计 | MCP 调用可追踪性 | Agent → 工具 → 结果端到端 trace |
| 3.4 | A2A 试点 | A2A v1.0 | 多业务 Agent 互调（观察项） |

---

## 三、优先级矩阵

| 优先级 | 动作 | 价值 | 成本 | 依赖 |
|:---:|------|:---:|:---:|------|
| **P0** | OpenAPI MCP Server（Phase 1.1） | 极高（行业对标刚需） | 中 | Tool Schema 标准化 |
| **P0** | MCP 授权对接（Phase 1.3） | 极高（安全底线） | 中 | AKSK 映射 |
| **P0** | Tool Schema 标准化（Phase 0.1） | 高（一切基础） | 低 | — |
| **P1** | market → MCP 工具目录（Phase 2.1） | 高（Agent 发现入口） | 中 | Phase 1 |
| **P1** | connector-api MCP 化（Phase 2.2） | 高（编排差异化） | 中 | Phase 1 |
| **P1** | 动态授权升级（Phase 2.3） | 高（治理领先） | 中 | 能力分级 |
| **P2** | AI 生成编排 / LLM 动态编排（Phase 2.4/3.1） | 中（长期方向） | 高 | Phase 2 |
| **P2** | 敏感度审批 + 审计（Phase 3.2/3.3） | 高（差异化） | 中高 | 能力分级 |
| **P2** | A2A 试点（Phase 3.4） | 低（早期） | 中 | 生态成熟 |

---

## 四、风险与开放问题

| # | 风险/问题 | 影响 | 建议 |
|---|----------|------|------|
| R1 | 184 API 直接 MCP 化会"工具爆炸"（每轮 token 超限） | 高 | Tool Schema 用 namespace + 分级暴露；参照 OpenAI 建议单轮 <20 工具 |
| R2 | 远程 MCP 长连接部署复杂度（多副本问题） | 中 | 复用 event-server 基础设施；参考 n8n 单一副本经验 |
| R3 | 国内 MCP 生态信息部分为领域知识，需复查 | 低 | 在可访问国内站点的环境更新 02/03 报告 |
| R4 | Agent 时代的滥用风险（工具注入/越权） | 高 | MCP 安全最佳实践 + 治理深化并行 |
| Q1 | open-app 是否提供"Agent 搭建平台"（对标 Aily/Copilot Studio）？ | — | 建议 P2 后再评估（先开放，后搭建） |
| Q2 | 审批 AI 化深度：自动放行阈值、敏感度分级模型谁来定义？ | — | 需业务侧对齐 |

---

## 五、下一步行动（对齐后执行）

1. **与用户对齐**本路线图（尤其 Phase 0/1 的范围与优先级）。
2. 决定是否正式立项（如立项，走完整 SDDU 流程：discovery → spec → plan → ...）。
3. 复查补充领域知识部分（02 企微、03 Copilot Studio、04 Zapier/Make/Coze/Dify）。

---

## 六、信源汇总

| 报告 | 主要信源 |
|------|---------|
| 01 | modelcontextprotocol.io、a2a-protocol.org、platform.openai.com |
| 02 | open.dingtalk.com、mcp.dingtalk.com、open.feishu.cn、aily.feishu.cn |
| 03 | docs.slack.dev、a2a-protocol.org（+领域知识） |
| 04 | docs.n8n.io（MCP Client / MCP Server Trigger）（+领域知识） |
| 05 | modelcontextprotocol.io、a2a-protocol.org、platform.openai.com（+领域知识） |
