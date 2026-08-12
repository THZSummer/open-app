# 03 · 国际协作平台 AI 化调研（维度 C）

> [⬆ 返回 README（调研总览）](README.md) · [📑 文档导航](README.md#-文档导航)

> **调研目标**: G1/G3 —— Slack/Teams/Google 的 Agent 开放形态、开发者生态策略
> **采集日期**: 2026-08-12
> **信息来源**: Slack 官方开发者文档（docs.slack.dev）、A2A 官方（a2a-protocol.org）、MCP 官方、OpenAI 官方 + 领域知识补充
> **方法**: 官方文档直采为主；未覆盖处标注「领域知识」

---

## 一、执行摘要（先出结论）

1. **国际平台一致走"Agent 优先"路线**：Slack 推出官方 Agent 创建能力（Slack CLI + Agent）、Salesforce Agentforce 深度整合；Microsoft 以 Copilot Studio 为 Agent 搭建中心；Google 以 Gemini + A2A 协议定义 Agent 协作标准。
2. **MCP 成为国际平台共同基础**：Slack/Teams/Google Workspace 均支持或拥抱 MCP（OpenAI/Anthropic 客户端也原生支持）——**印证 01 报告的 MCP 选型结论**。
3. **对 open-app 的含义**：国际平台验证了"Agent 开放平台"的三种开放形态——**Agent 搭建工具（Copilot Studio）、Agent 协作协议（A2A）、工具接入协议（MCP）**，open-app 应三线并进但以 MCP 先行。

---

## 二、Slack（Salesforce）—— Agent 与 MCP 双轨

### 2.1 官方现状（docs.slack.dev，2026-08 直采）

- **Agent 创建**：Slack 官方开发者文档首页提供 **"Create an agent"** 快速入口（agent quickstart），支持用 **Slack CLI** 开发 Agent 应用（`slack create` / `slack run`）。
- **平台定位**：官方将 Slack 定位为 **"AI Work Platform"**（AI 工作平台）——项目管理、工作流自动化、团队安全连接。
- **开发体系**：Slack CLI、GitHub Action、Bolt 多语言 SDK（Java/JS/Python/Node）。

### 2.2 Agentforce 与 AI 生态（领域知识，需复查）

- Salesforce **Agentforce** 是公司级 Agent 平台，Slack 作为其对话与协作前端。
- Slack 支持将第三方工具接入 Agent（MCP 支持已在生态中铺开，官方文档特定页面采集受限）。

### 2.3 对 open-app 的启示

- Slack 用 **CLI + SDK + 模板** 降低 Agent 开发门槛——open-app 可为开发者提供 **MCP Server 脚手架 + SDK**，降低接入成本。

---

## 三、Microsoft Teams / Copilot Studio —— 企业级 Agent 搭建中心

> ⚠️ 本节主要基于领域知识 + A2A 官方佐证（Microsoft 是 A2A TSC 成员），官方页面采集受限需复查。

### 3.1 Copilot Studio（领域知识）

- **定位**：Microsoft 的 Agent 搭建中心（低代码），可创建独立 Agent 并发布到 Teams/Copilot 生态。
- **能力**：自定义指令、知识来源、操作（Power Automate/Connectors/API）、MCP 工具接入（微软已宣布 Copilot Studio 支持 MCP，官方文档采集受限）。
- **企业特性**：与 Microsoft 365 Copilot 深度集成，企业治理（Data Loss Prevention、合规中心）较强。

### 3.2 Teams 平台

- Teams Toolkit 支持开发者构建 Teams 应用与 Agent（领域知识）。
- 微软是 **A2A TSC 成员**（a2a-protocol.org 官方确认）→ Agent 间协作标准上微软已参与共建。

### 3.3 对 open-app 的启示

- 微软路线是"**低代码 Agent 搭建 + 企业治理（DLP/合规）强绑定**"——open-app 的 AI 化应把**企业治理**（审批/审计/数据合规）作为一等公民，而非后补。

---

## 四、Google Workspace / Gemini —— 协议与原生 AI 双驱动

### 4.1 Gemini for Workspace（领域知识，需复查）

- Gemini 深度嵌入 Gmail/Docs/Sheets/Meet 等 Workspace 应用。
- Google 提供 **Apps Script / Workspace API** 作为开发者扩展点，AI 场景下以 Function Calling / 工具调用消费。

### 4.2 A2A 协议（官方直采，见 01 报告）

- Google 发起 **A2A**（Agent2Agent Protocol），v1.0 已发布并捐赠 **Linux 基金会**。
- TSC 成员含 **AWS、Cisco、Google、IBM、Microsoft、Salesforce、SAP、ServiceNow**。
- 官方定位：**MCP 管 Agent→工具，A2A 管 Agent→Agent**，二者互补。

### 4.3 对 open-app 的启示

- Google 用 A2A 定义"Agent 之间如何协作"，是行业对 **Agent 网络化** 的预演。open-app 未来若让多个业务 Agent 互相调用能力，A2A 是候选标准。

---

## 五、国际平台对比矩阵

| 对比项 | Slack (Salesforce) | Teams (Microsoft) | Google Workspace |
|--------|:---:|:---:|:---:|
| Agent 搭建 | Slack Agent + Slack CLI | Copilot Studio（低代码） | Gemini + Apps Script |
| 工具接入 | MCP 支持（生态） | MCP 支持（Copilot Studio） | Function Calling / MCP |
| Agent 协作协议 | Agentforce 生态 | A2A（TSC 成员） | **A2A（发起方）** |
| 企业治理 | 中（企业网格+权限） | **强（DLP/合规中心）** | 中（Workspace 管理） |
| 开发者入口 | CLI + SDK + 模板 | 低代码 + Toolkit | API + Apps Script |
| AI 原生产品 | Slack AI | Copilot（365） | Gemini |

---

## 六、结论（对应 G1/G3）

1. **趋势验证（G1）**：✅ 国际头部一致向"Agent 开放平台"演进，且**比国内更进一步**——已到"Agent 协作协议（A2A）"层级。行业共识**强确认**。
2. **AI 开发者门户形态（G3）**：国际形态 = **Agent 搭建工具/平台 + 工具接入协议（MCP）+ Agent 协作协议（A2A）+ 企业治理**。open-app 可对标四要素设计。
3. **对齐动作**：open-app 的"AI 化"应同时覆盖——MCP（工具接入，P0）、Agent 应用搭建（中台，P1）、A2A 跟踪（P2）、企业治理（贯穿）。

---

## 七、信源清单

| 信源 | 类型 | 采集时间 |
|------|------|---------|
| docs.slack.dev（Agent quickstart、CLI、SDK） | 官方直采 | 2026-08-12 |
| a2a-protocol.org（TSC 成员列表、A2A↔MCP） | 官方直采 | 2026-08-12 |
| modelcontextprotocol.io（生态采纳） | 官方直采 | 2026-08-12 |
| platform.openai.com（MCP connector 支持） | 官方直采 | 2026-08-12 |
| Copilot Studio / Slack Agentforce 细节 | 领域知识 | 需复查 |
