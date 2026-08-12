# AI 时代开放平台业界调研

> **调研主题**: 与 open-app 定位类似的产品，在 AI 时代都做了哪些事情
> **创建日期**: 2026-08-12
> **调研状态**: ✅ 信息采集完成，报告草稿已产出（01~06），待结论对齐
> **分支**: `feature/ai-era-open-platform-research`

---

## 📑 文档导航

| 文档 | 维度 | 对应目标 | 状态 |
|------|------|:---:|:---:|
| [**01 · AI 接入协议生态**](01-mcp-protocol-ecosystem.md) | A. MCP / Function Calling / A2A | G2 | ✅ 已产出 |
| [**02 · 国内开放平台 AI 化**](02-domestic-open-platform-ai.md) | B. 飞书 / 钉钉 / 企微 | G1/G3 | ✅ 已产出 |
| [**03 · 国际协作平台 AI 化**](03-international-platform-ai.md) | C. Slack / Teams / Google | G1/G3 | ✅ 已产出 |
| [**04 · Agent 编排 / 连接器生态**](04-agent-orchestration-ecosystem.md) | D. n8n / Zapier / Make / Coze / Dify | G3/G5 | ✅ 已产出 |
| [**05 · 企业级 AI 治理**](05-enterprise-ai-governance.md) | E. 动态授权 / 审计 / 敏感度分级 | G4 | ✅ 已产出 |
| [**06 · 汇总路线图建议**](06-summary-roadmap.md) | 综合 01~05 → open-app AI 化路线图 | G5 | ✅ 已产出 |

> **阅读建议**：先读 06（汇总结论）→ 再按需深入 01~05（各维度细节）。

---

## 一、背景

### 1.1 open-app 是什么

**open-app** 是**企业通讯能力开放平台**：将 XXX 通讯系统的核心能力（IM、Meeting、CloudBox、Contact、Mail、Drive、Bot 等）通过 **API / 事件 / 回调 / 连接器** 四种形式开放给企业内业务应用和个人应用。

核心特征：

| 特征 | 说明 |
|------|------|
| 能力抽象层 | 四种开放形式 R1~R4 + 特有连接能力（IM 卡片/云盘/邮件） |
| 治理骨架 | 应用管理 + AKSK + 权限中心 + 动态审批流（40 表 / 184 API） |
| 编排引擎 | connector-api 的 DAG 调度 + GraalJS 脚本沙箱 + 5 类节点 |
| 渠道网络 | event-server 的 SSE/WebSocket/WebHook + market 能力管理 |

### 1.2 为什么做这次调研

**战略判断**：当前 open-app 的服务对象是**人类开发者**——面向"人写代码调用 API"设计。而 AI 时代，能力的主要消费者将变成 **LLM Agent**：Agent 不读文档、不签审批单、不做集成适配，需要的是**机器可发现、机器可调用、机器可组合**的能力。

因此，open-app 应从"能力开放平台"演进为"**智能体开放平台（Agent-ready Platform）**"。为了验证这一判断并找到落地路径，需要对业界类似定位的产品进行系统调研，看它们**在 AI 时代做了什么、怎么做的、效果如何**。

---

## 二、调研目标

| # | 目标 | 要回答的问题 |
|---|------|-------------|
| G1 | **验证趋势判断** | "能力开放平台 → Agent 开放平台"是否是行业共识？主流平台是否都在走这条路？ |
| G2 | **摸清 AI 接入协议生态** | MCP / Function Calling / A2A 等协议的成熟度、被主流平台采纳程度，open-app 应优先支持哪个？ |
| G3 | **对标主流平台的 AI 开放策略** | 与 open-app 类似定位的产品，如何将自身能力开放给 AI？它们的"AI 开发者门户"长什么样？ |
| G4 | **梳理企业级 AI 治理实践** | 业界如何做 Agent 时代的动态授权、细粒度审计、敏感度驱动审批？open-app 现有审批/权限地基是否可复用？ |
| G5 | **提炼 open-app AI 化路线图** | 综合调研结论，给出 open-app 从"能力开放"到"Agent-ready"的分步落地建议 |

---

## 三、对标对象（国内外全覆盖）

### 3.1 核心对标：企业通讯/协作平台的能力开放体系

与 open-app 定位最相似——把"通讯协作能力"开放给企业应用的平台：

| 平台 | 归属 | AI 时代关注点 |
|------|------|--------------|
| **飞书开放平台** | 字节跳动 | 飞书智能伙伴、飞书 MCP、AI 应用生态 |
| **钉钉开放平台** | 阿里巴巴 | 钉钉 MCP、AI 助理（Agent）、钉钉生态 |
| **企业微信开放平台** | 腾讯 | 微信生态 AI 应用、智能客服 |
| **Slack Platform** | Salesforce | Slack AI / Agentforce、MCP 支持、Workflow Builder |
| **Microsoft Teams Platform** | 微软 | Copilot Studio、Teams Toolkit、自定义引擎 Agent |
| **Google Workspace Platform** | Google | Gemini for Workspace、A2A 协议、Apps Script |

### 3.2 补充对标：连接器 / iPaaS / Agent 编排平台

open-app 拥有 connector-api（连接流编排引擎），以下平台是编排能力的直接对标：

| 平台 | 类型 | AI 时代关注点 |
|------|------|--------------|
| **Zapier** | iPaaS | Zapier Agents、AI 自动生成流程 |
| **Make** | iPaaS | Make AI、AI 节点 |
| **n8n** | 开源自动化 | AI Agent 节点、MCP Server/Client 支持、AI Workflow Builder |
| **Coze** | 国内 Agent 平台 | 插件体系、Bot 工作流、MCP 支持 |
| **Dify** | 开源 LLM 应用平台 | 工具调用、Agent 编排、RAG 工作流 |

### 3.3 基础设施：AI 接入协议生态

| 协议/标准 | 发布方 | 定位 |
|-----------|--------|------|
| **MCP** (Model Context Protocol) | Anthropic（已捐赠 Linux 基金会） | 连接 AI 应用到外部数据源/工具的标准协议，"AI 的 USB-C" |
| **Function Calling** | OpenAI | LLM 结构化工具调用能力 |
| **A2A** (Agent2Agent) | Google | Agent 间通信协议 |
| **OpenAPI → Tool Schema** | 社区实践 | 把现有 API 资产转换为 LLM 可消费的工具描述 |

---

## 四、调研维度

| 维度 | 调研重点 | 对应目标 |
|------|---------|:---:|
| **A. AI 接入协议** | MCP/Function Calling/A2A 成熟度、生态、国内可用性、接入成本 | G2 |
| **B. 国内开放平台 AI 化** | 飞书/钉钉/企微的 AI 开放架构、对第三方能力接入方式、审批权限如何 AI 化 | G1/G3 |
| **C. 国际协作平台 AI 化** | Slack/Teams/Google 的 Agent 开放形态、开发者生态策略 | G1/G3 |
| **D. Agent 编排/连接器生态** | Agent 如何消费"连接器/工具"，编排引擎 AI 化方向（对照 connector-api） | G3/G5 |
| **E. 企业级 AI 治理** | 动态授权、Agent 审计、数据敏感度分级的最佳实践 | G4 |

---

## 五、调研方法与产出

### 5.1 方法

- **中等深度，先出结论**：每维度抓 3~5 个权威信源（官方文档、公告、技术博客）+ 对比矩阵 + 结论
- 信息采集依赖外部网站访问（当前环境受限，需换环境执行）

### 5.2 产出物规划

```
docs/ai-era-open-platform-research/
├── README.md                          # 本文件：背景 + 调研计划
├── 01-mcp-protocol-ecosystem.md       # 维度 A：AI 接入协议生态调研
├── 02-domestic-open-platform-ai.md    # 维度 B：国内开放平台 AI 化
├── 03-international-platform-ai.md    # 维度 C：国际协作平台 AI 化
├── 04-agent-orchestration-ecosystem.md# 维度 D：Agent 编排/连接器生态
├── 05-enterprise-ai-governance.md     # 维度 E：企业级 AI 治理
└── 06-summary-roadmap.md              # 汇总：open-app AI 化路线图建议
```

### 5.3 预期结论形态

1. **趋势验证**：行业是否共识（"能力开放 → Agent 开放"）
2. **协议选型建议**：open-app 优先支持 MCP 还是自研 Tool Schema
3. **平台对标矩阵**：各平台 AI 开放策略对比
4. **编排演进方向**：connector-api 从静态 DAG 到 LLM 动态编排的路径
5. **治理升级建议**：审批/权限体系 AI 化方案
6. **路线图**：分阶段落地建议（Tool Schema → MCP → 动态编排 → 治理升级）

---

## 六、调研状态与下一步

| 事项 | 状态 | 说明 |
|------|:---:|------|
| 调研计划对齐 | ✅ | 已与用户对齐（范围/对象/产出/深度） |
| 分支创建 | ✅ | `feature/ai-era-open-platform-research` |
| README 撰写 | ✅ | 本文件 |
| 信息采集 | ✅ | 2026-08-12 完成（MCP/A2A/OpenAI/n8n/钉钉/飞书/Slack 官方信源直采） |
| 报告撰写 | ✅ | 01~06 草稿已产出（详见 5.2 目录） |
| 结论对齐 | ⏳ 待办 | 报告草稿完成后与用户对齐，再决定是否正式立项 |

> **下一步**：与用户对齐 06 报告中的路线图建议（尤其 Phase 0/1 范围与优先级）；复查补充领域知识部分（02 企微、03 Copilot Studio、04 Zapier/Make/Coze/Dify）；决定是否正式立项。
