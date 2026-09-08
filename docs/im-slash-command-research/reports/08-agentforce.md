# Agentforce（Salesforce）竞品调研报告

> 调研主题：IM 内 `/` 唤起命令 → 选中命令 → 输入参数 → 触发用户本地 HTTPServer 的「确定性命令触发」（全程无 AI/LLM 参与）
> 调研日期：2026-09-08
> 信源：Salesforce 官网（salesforce.com/agentforce/ 及子页）、Slack 官方帮助中心与开发者文档、Slack 官方博客。未核实内容已明确标注「未能核实 / 网络受限未访问」

---

## 【产品描述】

- **归属**：Salesforce, Inc.（**Slack 同属 Salesforce 旗下**；Agentforce 是 Salesforce 的平台产品，原生嵌入 Slack）。
- **形态**：企业级 **AI Agent 平台**（Agent Builder / Agent Studio / Agent Script / 多智能体编排 / MCP / Voice / Observability），本身不是 IM 应用；在 Slack 内以「Agent 应用（Agents tab / Agentforce Hub）」形态存在。
- **定位**：官方自述为 "the AI agent platform that delivers 24/7 autonomous support at enterprise scale"（企业级自主 AI 智能体平台）、"digital labor"（数字劳动力）；核心引擎为 **Atlas Reasoning Engine**（LLM 推理），依托 Data 360 + Einstein Trust Layer 落地。
- **目标用户**：Salesforce CRM 企业客户（客服 / 销售 / 员工服务等场景）；管理员与开发者在 Agent Builder 低代码构建 agent；最终用户在 Slack / Web / 电话 / 应用中与 agent 对话。
- **计费参考**：Agentforce for Service 约 $2/会话（why 页面口径，标准量折扣）。
- **同类型分类**：**「不同类型」对照产品** —— Agentforce 是 **AI 驱动（LLM 理解自然语言 → 决策 → 调工具）** 的核心产品，与本方案「确定性命令触发（无 LLM）」形成方向性对照。

---

## ① 非 AI 本地 Command 支持（对应维度 A~D）

**结论先行：Agentforce 不提供、也不复用 Slack 原生 Slash Command 作为自身入口。它在 Slack 里是纯对话式 AI Agent（DM / @提及 / Agents tab），没有「用户显式选命令 → 填参数 → 确定性路由」的通道；其确定性元素只存在于 agent 内部的执行层（Apex / Flow / Agent Script），不由用户直接触发。**

### A. 命令入口与唤起

- **无 `/` 唤起、无命令列表**：Agentforce 在 Slack 中的全部交互形态为自然语言对话——
  - 从侧边栏 **Agents tab / Agentforce Hub** 进入，搜索并选择 agent，然后 "choose a prompt, or send a message to start a conversation"；
  - 或在频道 / DM 中 **@提及** agent 发起对话（"mentioning them in a message to prompt them to answer"）。
  - 官方提供的辅助是**预写提示词（suggested prompts）**与可分享的 **agent prompt link**，仍是自然语言，而非命令枚举。
- **没有命令清单机制**：Agentforce 侧不存在类似 Slack `/` 快捷菜单的东西；「命令候选集」的概念被 **Topics / Subagents** 取代（agent 内部能力分区），由 LLM 判断选用，用户不可见、不可枚举。
- **Slack 原生 Slash Command 与 Agentforce 相互独立**：Slash Command 是 **Slack 平台自身**为 App 提供的确定性通道（App Manifest 注册、Request URL 回调），一直保留并正常工作；Agentforce 作为 Slack 里的 agent 应用**并不注册 / 占用任何 slash command**，二者是两套并行机制（详见 ②-F）。

> 信源：
> - https://slack.com/help/articles/36218786859667-Use-Agentforce-in-Slack（使用形态：Agents tab、DM、@提及、共享 prompt）
> - https://slack.com/help/articles/36218109305875-Set-up-and-manage-Agentforce-in-Slack（安装 / 管理、Topics/Subagents/actions 定义）
> - https://slack.com/blog/news/limitless-workforce-with-agentforce-in-slack（Agentforce 2.0 进入 Slack：Hub、@提及、pre-built Slack Actions）
> - https://slack.com/ai-agents（产品页）

### B. 参数输入

- **参数由 LLM 对话式收集**：Agentforce 没有「命令后跟参数 / 模态表单」的输入范式。参数在对话中由 LLM 追问补齐，或由 agent 从上下文推断，再填充到 action 的入参。
- **action 的参数 schema 是结构性声明，但不是面向用户的表单**：
  - 自定义 Apex action：入参 / 出参由 Apex 类里的 `@InvocableVariable(required=true, description=...)` 声明；在 Agent Builder 中可对该 action 配置 "Agent Action Instructions" 与 Inputs / Outputs 说明，供 LLM 正确填参。
  - 标准 Slack Actions（如 `Message Channel`、`Create Canvas`、`Search`）预置在 Agent Builder 中，同样由 LLM 调用填参。
- 即：**参数存在结构化 schema（供 LLM 使用），但用户侧没有「显式填参」环节** —— 这正是与本方案（用户显式、结构化填参）的根本分界。

> 信源：
> - https://docs.slack.dev/ai/customizing-agentforce-agents-with-custom-slack-actions（Apex `@InvocableVariable` 定义 action 输入 / 输出、Agent Action Configuration、Topic 指令）
> - https://slack.com/help/articles/36218109305875-Set-up-and-manage-Agentforce-in-Slack（标准 Slack actions）

### C. 命令注册与下发

- **agent 注册在 Salesforce 侧**：管理员 / 开发者在 **Agentforce Builder / Agent Studio** 创建 agent（Topics、Subagents、Actions、Instructions、Guardrails），再通过 **Connections 添加 Slack 连接**，最后由 Slack 侧 Owner/Admin 审批**安装 agent**（Org 级 / 指定 workspace 级）。
- **只支持 Employee Agent 类型接入 Slack**（帮助文档明确："You can only build agents for Slack using the Agentforce Employee Agent type"）；安装后出现在 Slack 的 Agents / Agentforce 区域。
- **无「用户本地上报 / 注册」概念**：命令 / 能力的「注册中心」是 Salesforce 的 Agentforce Assets，非用户侧。

> 信源：
> - https://slack.com/help/articles/36218109305875-Set-up-and-manage-Agentforce-in-Slack
> - https://slack.com/help/articles/30754346665747-Connect-Salesforce-and-Slack（Slack ↔ Salesforce 连接：account mapping、审批）

### D. 触发到本地的链路

- **不依赖 Slack Request URL webhook**：Agentforce 不通过 Slack 的 Slash Command webhook 接收请求；它走 **Slack ↔ Salesforce 平台连接**（Slack 连接器 + connected app），消息由 Slack 转发给 Salesforce 的 agent 运行时（Atlas 推理）。
- **执行是出向调用，方向与本方案相反**：agent 要操作外部系统时，通过 **Named Credentials**（含 Auth. Provider / External Credential 管理 OAuth）发起**出向 HTTP callout**（官方示例即 `callout:Slack_API/chat.postMessage` 调 Slack Web API）。即「云端 → 公网服务」，**不存在「触达用户本地 HTTPServer」的产品模型**；Salesforce 平台托管在云端，不面向个人本地机器。
- **本地开发链路存在但属 SaaS 生态范式**：Apex 开发用 Salesforce CLI / VS Code / Code Builder，部署到 org 后才生效；无「命令直达用户本机」的路径。
- **确定性触发存在，但面向数据而非用户命令**：官方 FAQ 明确 agent "can be triggered by changes in data and automations"（数据变化 / 自动化触发）——这是**确定性触发源**，但触发后仍进入 LLM 推理执行，且不面向 Slack 用户手动发起。

> 信源：
> - https://docs.slack.dev/ai/customizing-agentforce-agents-with-custom-slack-actions（Named Credentials、OAuth、Apex 调 Slack Web API 全流程）
> - https://www.salesforce.com/agentforce/how-it-works/（FAQ："Agents can be triggered by changes in data and automations"；Atlas Reasoning Engine）
> - https://www.salesforce.com/agentforce/why/（Agentforce 集成渠道：CRM、WhatsApp、Messenger、网站等）

---

## ② AI Agent Skill 支持（对应维度 E~F）

### E. 工具调用（Agent Actions / Function Calling）成熟度 —— 高，且是产品核心

- **运行机制**：Atlas Reasoning Engine 将用户请求拆解、逐步推理并规划（plan）→ 决定调用哪个 action → 执行 → 校验结果；全程 LLM 决策。Action 即 Agentforce 的「工具调用（function calling）」抽象。
- **action 类型矩阵**：

| 类型 | 说明 |
|------|------|
| **Apex action**（Invocable Method） | 开发者写 Apex 类，`@InvocableMethod` 暴露为 action；可调用 Slack Web API / 任意 HTTP 服务；需授权（Einstein Agent User 的 Apex Class Access） |
| **Flow action** | 复用 Salesforce Flow 自动化作为 action |
| **Prompt action** | 复用 prompt template |
| **标准 Slack Actions** | Agent Builder 内置，如 Create Canvas、Message Channel、Search、Create Channel、Send DM、Manage Lists 等 |
| **MCP tools** | Agentforce 作为**原生 MCP client**，从 AgentExchange 装配第三方 MCP server 的工具 / 资源 / prompt；配置集中化（Centralized MCP Server Registry） |
| **MuleSoft / External API** | 通过 MuleSoft connector 或 named credentials 连接任意系统 |

- **可靠性治理**：Einstein Trust Layer（zero data retention、grounding、toxicity）、Agent Guardrails（用户自定义 + Salesforce 托管保护）、Agentforce Observability（监控 / 分析 / 单会话 debug）、多智能体编排（Subagents / Multi-Agent Orchestration）。
- **确定性补丁（关键观察）**：**Agent Script** 是官方推出的「确定性锚点」——"blends the creative problem-solving of LLMs with the certainty of a deterministic system"、"eliminating the randomness of LLMs"；**hybrid reasoning** 让确定性逻辑围绕 LLM 推理在 **Before / During / After** 三个阶段插桩执行（如「先查库存 → LLM 决策 → 再确认发提醒」），并允许 "dial up or dial down deterministic control"。即：**即便最激进的 AI 厂商，也把确定性逻辑内嵌进 agent 执行层以保障可靠** —— 但这一切仍发生在 agent 运行期，非用户可见的命令通道。

> 信源：
> - https://docs.slack.dev/ai/customizing-agentforce-agents-with-custom-slack-actions
> - https://www.salesforce.com/agentforce/how-it-works/（Atlas、actions、guardrails）
> - https://www.salesforce.com/agentforce/script/（Agent Script）
> - https://www.salesforce.com/agentforce/mcp-support/（MCP、AI agent gateway）
> - https://www.salesforce.com/agentforce/resources/reliable-ai-agents-guide/（hybrid reasoning Before/During/After 图解；Gartner「40% agentic 项目将被取消」引证）

### F. 与 Slack 确定性命令（Slash Command）的关系：**并存 + 互补，非替代**

- **Agentforce 不替代 Slash Command**：Agentforce 是 Salesforce 平台上的 agent，与 Slack 原生 Slash Command / Shortcuts / Workflow Builder / Block Kit 是**两套并行机制**。Slack 作为平台仍完整保留并持续维护确定性通道（姊妹篇 01-slack.md 已核实：Slash Command 走 Request URL 签名校验的纯程序化链路，Slack 官方未表述「AI 取代 slash 命令」）。
- **官方把两者放在不同轨道**：
  - 确定性轨道：Slack **Workflow Builder / automations**（"While Slack automations speed up work, managing these Slack actions still takes effort… Agentforce is here to help" —— 官方原文把 Agentforce 定位成**在确定性自动化之上再加一层 AI 助手**，帮用户自动 setup 和推荐下一步，而非消灭它们）。
  - AI 轨道：Agentforce / Slackbot / 第三方 AI assistant，覆盖「对话 + 自动选工具 + 自主行动」。
- **AI 行动调用的底层仍是确定性 Slack API / 自动化**：agent 的 Slack Actions（建频道、发消息、更新 canvas）本质是**由 LLM 决策、由确定性 Slack Web API / automations 执行** —— 「AI 决策、确定性执行」的混合形态。
- **判定**：对 Slack 生态而言是**互补并存**：确定性命令留给「强约束结构化交互」，AI agent 覆盖「开放对话自主行动」。但需注意 **Salesforce 的战略重心明显在 AI 轨**（见③），确定性通道是被保留的既有资产，而非被重点投入的方向。

> 信源：
> - https://slack.com/blog/news/limitless-workforce-with-agentforce-in-slack（Slack automations 与 Agentforce 的互补表述；pre-built Slack Actions）
> - https://slack.com/ai-agents（三类 agent/assistant 并存：Agentforce / 自建 AI assistant / 第三方 AI assistant）
> - https://slack.com/help/articles/36218786859667-Use-Agentforce-in-Slack

---

## ③ 未来规划（对应维度 G）

### 战略定位：以 AI Agent（数字劳动力）为核心主线

- **官方原话级信号**：
  - "Agentforce is the AI agent platform… Let humans do what they do best, and let Agentforce do the rest."（salesforce.com/agentforce 首页）
  - "unlock a limitless digital labor force by leveraging autonomous AI agents"（FAQ）
  - Agentforce 2.0 官方称为 "**the first digital labor platform for enterprises**"，并把 **Slack 作为首发渠道**（2024-12-17 公告，GA 于 2025-01）；Slack 侧定位为 "Slack is a central hub for all your agents and assistants"。
- **路线图重点投入方向**（官方在售 / 在研，均为 AI 侧）：
  - **MCP / AI agent gateway**：官方用**将来时**表述——"Agentforce **will provide** secure, enterprise-grade agentic interoperability"、"**will deliver** enterprise-grade agentic interoperability through … the AI agent gateway"（Centralized MCP Server Registry、治理、限流）——属明确的在研路线图项。
  - **Multi-Agent Orchestration**（多 agent 协作）、**Agentforce Voice**、**Agentforce Observability**、**Agentforce Labs**（实验特性社区）。
  - **Agent Script / hybrid reasoning**：把「确定性控制」做成 agent 能力的可调旋钮，官方主张**确定性 + 概率性混合**，但**始终是 agent 内部机制**，不是面向用户的命令通道。
- **对「确定性 vs AI」的整体判断**：Salesforce 没有「用 AI 取代手工命令」的公开路线图表述；其官方叙事是**双轨并存**——用户面主推对话式 AI agent（替代「填表 / 点按钮 / 跑 workflow」这类手工操作的心智），执行面用 Apex / Flow / Agent Script 的确定性逻辑保证可靠。换言之：**「确定性」被降维为 AI agent 内部的可控执行层，而不再作为面向最终用户的产品形态去主推**。

### 需要标注的「未能核实 / 网络受限未访问」项

1. **Salesforce 官方帮助中心（help.salesforce.com）Agentforce 深层条目**（如 agentforce_slack.htm、Actions 参考 ai.copilot_actions_ref.htm）：页面为 JS 渲染，webfetch 仅返回加载壳，**未能核实正文**；相关内容改由 docs.slack.dev 指南与 Slack 帮助中心间接核实。
2. **Agentforce Developer Guide（developer.salesforce.com）**：访问返回 **403**，**网络受限未访问**；因此「Agentforce API 外部程序化调用（如 REST 触发 agent）」的细节**未能核实**，本报告不展开该点。
3. **官方 roadmap 页（salesforce.com/company/roadmap）**：重定向至通用资源页，**未获取到 Agentforce 的具体时间表**；上文「路线图」判断均基于官方在售 / 在研页面的原文表述（MCP 将来时、Labs、Voice、Multi-Agent），属可核实事实，非远期承诺。
4. 未做独立数据验证（对话量、市场份额等）。

---

## 小结（对「确定性命令触发」方向的判断）

- **Agentforce 不保留确定性命令通道**：它作为纯 AI 产品，在 Slack 内只有对话式入口（Agents tab / DM / @提及），不注册、不复用、也不依赖 Slack 原生 Slash Command / Request URL webhook；「用户显式选命令 → 填参 → 确定性路由到本地服务」这条路在 Agentforce 产品形态中**不存在**。
- **确定性元素被内化而非暴露**：Agentforce 的 Apex / Flow Actions 与 **Agent Script** 提供「确定性执行 / 逻辑编排」，但都由 LLM 决策触发、在云端运行，且以出向 HTTP 连接外部服务——与本方案「无 LLM、命令直达用户本地 HTTPServer」恰好方向相反。
- **对趋势的含义**：即便最坚定的 AI 厂商，也把确定性逻辑（Agent Script / hybrid reasoning）内嵌进 agent 执行层来保障可靠性——这从侧面印证「确定性执行」是企业级刚需，但它被作为 AI 的内部能力，而非面向用户的命令通道。
- **对本方案的定位参考**：Agentforce（AI 驱动）与 Slack 原生 Slash Command（确定性驱动）**在 Slack 生态内并存互补**，说明「确定性命令触发」并未被 AI 消灭，仍有明确存在空间；而 Agentforce 本身不提供、也不打算提供「用户显式选命令填参」的产品形态，与本方案形成差异化对照。
