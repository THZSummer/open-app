# Microsoft Teams（Teams Platform / Teams Developer）竞品调研报告

> 调研主题：IM 内 `/` 唤起命令 → 选中命令 → 输入参数 → 触发用户本地 HTTPServer 的「确定性命令触发」（全程无 AI/LLM 参与）
> 调研日期：2026-09-08
> 信源：全部引用微软官方信源（learn.microsoft.com 的 Teams Platform / Microsoft 365 Copilot extensibility / Copilot Studio / Adaptive Cards / Dev Tunnels 文档，以及 Microsoft Support 与 Teams SDK 官方站），未核实内容已明确标注

---

## 【产品描述】

- **归属**：Microsoft（微软）。Teams 平台开发文档属 Microsoft 365 / Teams 产品线。
- **形态**：企业级协作 IM（聊天/频道/会议）+ 开放应用平台（Teams Platform：Agents / Bots / Message Extensions / Tabs / Adaptive Cards / Task Modules / 声明式 Agent / Copilot Studio）。
- **定位**：企业协作与工作流中枢，同时是微软 AI（Microsoft 365 Copilot / Agents）面向企业的主入口之一。
- **目标用户**：企业全员（终端用户）、开发者/ISV（构建应用与 Agent）、企业 IT 管理员。
- **同类型分类**：属于「同类型 IM」——Teams 是**提供确定性 Slash 命令 / Message Extension 的 IM 平台**（在 compose box / command box 输入 `/` 唤起命令菜单 → 参数 → HTTP 送达 Bot/API 端点），与本方案形态接近，是直接对标物之一。

---

## ① 非 AI 本地 Command 支持（对应维度 A~D）

**结论先行：Teams 提供成熟的、纯确定性的命令通道（Slash 命令 + Message Extension），链路无 LLM 参与；2026 年官方仍在为该通道发布新功能（Agent Slash 命令 GA）。**

### A. 命令入口与唤起

- **支持 `/` 唤起命令菜单（autocomplete）**：在聊天/频道/会议的 compose box 或搜索栏输入 `/`，即弹出可用命令列表，可继续输入按名称/描述过滤，Enter 或鼠标选中后按提示操作（Microsoft Support 官方说明）。
- **三种命令来源叠加在同一个 `/` 菜单**：
  - **平台内置命令**：如 `/apps`、`/chat`、`/goto`、`/schedulemessage`、`/mute`、`/away`、`/gif` 等（约 30+，由微软客户端维护）；官方注明**内置 slash 命令仅桌面端可用**。
  - **App / Agent 注册的 slash 命令**：应用/Agent 通过在 **app manifest** 中声明 `triggers: ["slash"]`（`bots[].commandLists[]` 或 `composeExtensions[].commands[].triggers`）把命令加入 `/` 菜单，安装到会话后即对会话内用户可见（官方 developer preview 于 2026-05-29 发布，GA 于 2026-07-30 发布）。菜单项会显示命令描述 + Agent 名称与图标，同名命令可区分。
  - **Message Extension 命令**：由 app manifest 的 `composeExtensions[].commands[]` 声明，可从 **compose message area（输入框底部按钮）、command box（`/your-app-name`）、消息 `...` 溢出菜单** 三处唤起（`context: compose | commandBox | message`）。
- **交互形态**：输入框内的**下拉式 autocomplete 菜单**（非侧栏/面板）；选中命令后命令文本回填输入框（Agent slash 命令还会把 compose box 切换为「定向消息 targeted message」模式，私发给该 Agent）。
- **命令清单来源**：客户端本地内置 + **服务端/清单下发混合**——内置命令客户端内置；应用命令由 app manifest 声明、App 安装后被 Teams 下发到客户端菜单，非「用户本地上报」。

> 信源：
> - https://learn.microsoft.com/en-us/microsoftteams/platform/agents-in-teams/agent-slash-commands
> - https://support.microsoft.com/office/use-commands-in-microsoft-teams-88f61508-284d-417f-a53d-9e082164050b
> - https://learn.microsoft.com/en-us/microsoftteams/platform/messaging-extensions/what-are-messaging-extensions
> - https://learn.microsoft.com/en-us/microsoftteams/platform/developer-announcements

### B. 参数输入

Message Extension（Action 命令）创建表单有**三种方式**，参数声明既有 manifest 强类型 schema，也有运行时动态表单：

| 形态 | 说明 | 参数如何声明 |
|------|------|-------------|
| **Static parameter list（manifest 静态参数）** | 最简单；在 manifest 中声明参数列表，Teams 客户端直接渲染输入表单（不能自定义排版） | manifest `composeExtensions.commands[].parameters[]`：`name`、`title`、`inputType`（`text / textarea / number / date / time / toggle / choiceset`）、`isRequired`、`value`（默认值）、`choices`（最多 10 项） |
| **Adaptive Card 表单** | 在 dialog（Task Module，TeamsJS v1.x 叫 task module）中用 Adaptive Card 自定义 UI 与控件 | 由**运行时 `fetchTask` invoke 事件动态下发** Adaptive Card JSON；提交时 `task/submit`（`dialog.submit`）把结构化数据回传 Bot |
| **Embedded web view（内嵌网页）** | 在 dialog 中嵌入自托管网页，完全掌控 UI | `taskInfo.url` 指向自托管页面；提交回传由网页内 JS + invoke 处理 |

- **Action 命令**：以模态 dialog（Task Module）收集/展示信息，提交后由 web service 把结果卡片插入 compose box 或直接入会话；可链式串联多表单（复杂工作流）。`fetchTask: true` 表示动态拉取 dialog，`fetchTask: false` 用 manifest `taskInfo` 预置。
- **Search 命令**：用户在搜索框输入**自由查询词**，`initial invoke` 携带搜索串 → 服务端返回卡片列表 → 用户选中后卡片嵌入 compose box（可当「外部系统搜索」用）。
- **参数上限**：单个命令 `parameters` 最多 **5 个**；一个 message extension 最多 **10 个命令**。
- 注意：manifest schema 中另含 `semanticDescription`（面向 LLM 的参数语义描述）与 `samplePrompts`（面向 Copilot 的示例提示）——**参数 schema 本身是确定性的，AI 字段是叠加项**。

> 信源：
> - https://learn.microsoft.com/en-us/microsoft-365/extensibility/schema/root-compose-extensions-commands
> - https://learn.microsoft.com/en-us/microsoftteams/platform/messaging-extensions/how-to/action-commands/define-action-command
> - https://learn.microsoft.com/en-us/microsoftteams/platform/messaging-extensions/how-to/search-commands/define-search-command
> - https://learn.microsoft.com/en-us/adaptive-cards/
> - （Task Module 专门文档页多次访问返回 404，未单独核实；其能力已由 Action 命令文档中的 dialog/task module 描述覆盖）

### C. 命令注册与下发

- **维护方**：`平台内置命令（微软维护）` + `应用/Agent 注册命令（开发者维护）`，均非「用户本地上报」。
- **注册方式**：全部通过 **app manifest**（Microsoft 365 app manifest，前身叫 Teams app manifest）声明——
  - **Message Extension 命令**：`composeExtensions[].commands[]`（`id / type / title / description / context / triggers / parameters / fetchTask / taskInfo`）。
  - **Agent/Bot 命令菜单**：`bots[].commandLists[]`（`scopes` + `triggers: ["slash" | "mention"]` + `commands[]`）。
  - 可用 **Microsoft 365 Agents Toolkit**（原 Teams Toolkit）、**Developer Portal for Teams**（dev.teams.microsoft.com）或 **Teams Developer CLI / Teams SDK** 创建与打包。
- **发现机制**：用户输入 `/` 在 autocomplete 菜单中搜索（按名称/描述/应用名过滤）；命令安装后即被索引；App 可上架 **Teams Store** 分发到企业/公众。
- **Sideload（上传自定义应用）**：面向开发/内部测试的安装方式（需租户开启 custom app upload）。

> 信源：
> - https://learn.microsoft.com/en-us/microsoft-365/extensibility/schema/root-compose-extensions-commands
> - https://learn.microsoft.com/en-us/microsoftteams/platform/agents-in-teams/agent-slash-commands
> - https://learn.microsoft.com/en-us/microsoftteams/platform/messaging-extensions/how-to/action-commands/define-action-command
> - https://microsoft.github.io/teams-sdk/get-started/quickstart-register

### D. 触发到本地的链路

- **链路机制**：Message Extension 由「你自托管的 web service + app manifest」组成，**使用 Bot Framework 的消息 schema 与安全通信协议，须把 web service 注册为 Bot Framework 上的 bot**。用户触发命令时，Teams 经 Bot Service / Teams Bot Service 向 bot 配置的 **Messaging Endpoint（HTTPS）** 发送 **invoke activity**（`composeExtension/*`、`task/fetch`、`task/submit` 等 JSON payload），bot 以 JSON 响应驱动下一步交互。
- **安全鉴权**（官方层面）：
  - 应用注册在 **Microsoft Entra ID**（app ID + client secret / 证书 / 托管标识），消息扩展与 bot 通信基于 Bot Framework 鉴权；官方文档明确要求对收到的 activity 做 **Authorization Bearer JWT 校验**（Teams SDK 文档：keep JWT/activity validation on），另支持 **SSO**（Entra ID + Bot Framework Token Service + tokenExchange，bot 用令牌访问用户身份）。
  - 支持 TLS 1.2；`Authorization` 头 + 应用级密钥校验是本地端点的可信接收前提。
- **能否送达「用户本地」——有官方明确先例与工程路径**：
  1. **Dev Tunnels（官方推荐）**：微软自有本地隧道服务，`devtunnel host` 给 localhost 一个公网 HTTPS 地址；Teams SDK / Agents Toolkit 本地调试默认用它（`devtunnel create ... --allow-anonymous` + 把 `https://<tunnel-host>/api/messages` 注册为 endpoint）。官方定位：开发/adhoc 测试用，**非生产**。
  2. **ngrok / Cloudflare Tunnel**：Teams SDK 官方文档列为首选替代方案（start a tunnel, get a public HTTPS URL, use it as your `--endpoint`）。
  3. **Microsoft 365 Agents Toolkit 本地调试**：VS Code 一键 `Debug in Teams`，自动起本地服务 + dev tunnel + 上传 app（sideload）到 Teams web 客户端。
  4. **Teams Developer CLI**：`teams app create --endpoint https://<tunnel-host>/api/messages` 注册 bot 并拿 sideload 链接。
- **API-based Message Extension（Bot 之外的确定性新路径）**：可用 OpenAPI Description（OAD）直接把现有 API 变成 message extension（search 命令），**不依赖 Azure bot 基础设施（traffic privatized）**——更接近「纯 HTTP 直达」形态；Bot-based 版本才支持 action 命令、link unfurling 等。
- 即：Teams 支持「命令 → 用户本地 HTTPServer」的链路，但**面向开发者/自托管场景**（隧道 + sideload）；对终端用户，命令目标默认是公网托管服务。

> 信源：
> - https://learn.microsoft.com/en-us/microsoftteams/platform/messaging-extensions/what-are-messaging-extensions
> - https://learn.microsoft.com/en-us/microsoftteams/platform/bots/how-to/authentication/bot-sso-overview
> - https://microsoft.github.io/teams-sdk/developer-tools/local-tunnels
> - https://microsoft.github.io/teams-sdk/get-started/quickstart-register
> - https://learn.microsoft.com/en-us/microsoftteams/platform/toolkit/debug-local
> - https://learn.microsoft.com/en-us/azure/developer/dev-tunnels/overview
> - （Azure Bot Service 文档页本次访问返回 404，Bot Framework 鉴权细节未能从该源核实；本报告改用 Teams 官方 SSO 文档与 Teams SDK 文档表述）

---

## ② AI Agent Skill 支持（对应维度 E~F）

### E. 确定性 vs AI

- **确定性为主**：Slash 命令 / Message Extension（action + search）/ Adaptive Card / Task Module / link unfurling 的触发链路**完全确定性**——Teams 不做语义理解，只做 invoke activity 转发 + 鉴权 + 渲染，全程无 LLM。**存在纯净确定通道（无 AI），且是平台最成熟的通道**。
- **AI 是叠加的新通道（均有 LLM）**，构建在确定性基础设施之上：
  - **Agents in Teams（Teams SDK）**：官方定义「intelligent and conversational apps built with Teams SDK... interact through natural language (LLM), connect to business data, and perform actions」。
  - **声明式 Agent（declarative agent）**：配置 instructions + actions（插件）+ knowledge，跑在 M365 Copilot 的 orchestrator / 模型上（无额外托管）。
  - **自定义引擎 Agent（custom engine agent）**：自带 orchestrator + 模型，托管在 Azure 等外部；可用 Copilot Studio（低代码）或 Agents Toolkit + Semantic Kernel / LangChain（专业代码）构建。
  - **Copilot Studio**：统一 Agent 构建平台，分三种 harness——**standard harness（规则型：确定性 topics/paths，可预期行为）**、**GitHub Copilot harness（推理型 agent）**、**Copilot chat harness（扩展 M365 Copilot）**。
  - **Teams AI Library**：v1 已弃用，官方 2025-11 宣布 **v2 更名为 Teams SDK**（含 MCP、A2A 支持）——AI 开发栈全面并入 Teams SDK。
- 官方对确定性命令与 AI 的取舍有明确指导（agent-slash-commands 最佳实践）：**「Keep the command set small and focused. Support natural-language prompts when users need a more conversational experience, but reserve slash commands for the most repeatable actions.」**——即确定性命令用于高频、可重复、强约束动作；自然语言用于开放式对话。

> 信源：
> - https://learn.microsoft.com/en-us/microsoftteams/platform/agents-in-teams/overview
> - https://learn.microsoft.com/en-us/microsoft-365-copilot/extensibility/agents-overview
> - https://learn.microsoft.com/en-us/microsoft-365-copilot/extensibility/overview-declarative-agent
> - https://learn.microsoft.com/en-us/microsoftteams/platform/agents-in-teams/agent-slash-commands
> - https://learn.microsoft.com/en-us/microsoft-copilot-studio/harnesses-overview
> - https://learn.microsoft.com/en-us/microsoftteams/platform/developer-announcements

### F. 与 Agent Skill 的关系：**并存（双通道）**

- **同一份 app manifest 同时承载两套能力**：
  - 确定性通道：`bots[].commandLists[]`（slash / mention 命令菜单）、`composeExtensions[].commands[]`（action / search）、`parameters`（参数 schema）。
  - AI 通道：`declarativeAgents` / `copilotAgents`（instructions / actions / knowledge）、`samplePrompts`、`semanticDescription`（给 LLM 用的语义描述）。
- **命令与 Agent 可互相转化 / 叠加**：
  - 官方主推「**把 bot-based Message Extension 扩展为 Agent**」（build-bot-based-agent），让现有确定性命令同时服务 M365 Copilot（Copilot 经插件/消息扩展调用你的 API）。
  - **Agent Slash 命令**（2026 GA）：给 LLM Agent 也暴露 `triggers: ["slash"]` 的命名命令——**AI Agent 之上照样叠加确定性命令入口**。
  - Copilot Studio standard harness = 规则/话题驱动（确定性），GitHub Copilot harness = AI 推理驱动；同平台内两种形态并存。
- **替代/互补判断**：官方未表述「AI 取代 slash 命令」，而是**双通道互补**——确定性命令负责「高频、可重复、需要表单/强类型参数」的动作；AI 通道负责「开放式对话、意图理解、自动调工具」。微软整体战略偏向 AI，但 2026 年仍新发布并 GA 了 Agent slash commands、targeted messages、prompt starters 等**确定性命令/确定性入口**功能，说明确定性通道与 AI 通道**同时被投资、并存**。

> 信源：
> - https://learn.microsoft.com/en-us/microsoftteams/platform/messaging-extensions/what-are-messaging-extensions（"Agents provide a more flexible, intelligent, and future-ready experience... We recommend that you explore and build agents"）
> - https://learn.microsoft.com/en-us/microsoft-365-copilot/extensibility/agents-overview
> - https://learn.microsoft.com/en-us/microsoftteams/platform/agents-in-teams/agent-slash-commands
> - https://learn.microsoft.com/en-us/microsoft-copilot-studio/harnesses-overview

---

## ③ 未来规划（对应维度 G）

### 微软对「AI Agent」路线的战略投入（官方信号非常明确）

- **平台文档首页已把「Build Teams agents」作为头号入口**（Agents / Teams SDK / Quickstart），官方把 Agents 定义为 Teams 的下一代开发形态（"Agents provide a more flexible, intelligent, and future-ready experience... We recommend that you explore and build agents"，Message Extension 文档显著位置）。
- **Teams AI Library v1 弃用 → v2 更名 Teams SDK**（2025-09 GA、2025-11 更名，含 MCP / A2A 支持）；**Teams Toolkit 更名 Microsoft 365 Agents Toolkit**；**M365 Agents SDK**（跨 Teams / M365 Copilot / Copilot Studio / WebChat 的 agent 运行时）成为一等公民——AI 开发栈全面重组。
- **声明式 Agent / 自定义引擎 Agent** 双轨制官方主推（agents-overview 决策指南），Copilot Studio 低代码 + Agents Toolkit 专业代码；Agent 可发布到 Teams Store / M365。
- **2026 年确定性命令通道同步获得新 GA**：Agent slash commands、targeted messages（定向消息）、prompt starters（清单配置、零代码）、emoji reactions——说明**命令入口并未被 AI 取代，而是作为 Agent 与 App 的确定性 UI 一并演进**。

### 微软对「本地执行 / 本地 agent」的投入

- **开发链路层面**：官方持续投入「本地运行」工程能力——**Dev Tunnels**（本地隧道，官方推荐、Teams SDK / Agents Toolkit 内置）、**Microsoft 365 Agents Toolkit 本地调试**（自动隧道 + sideload）、**ngrok / Cloudflare Tunnel** 为官方认可替代——「命令/回调送达本地服务」是官方支持的工程范式。
- **产品层面**：面向终端用户的「本地 agent / 本机执行」**未见于官方公开主推路线**；Dev Tunnels 官方明确标注为 public preview、仅用于开发测试、**不用于生产**。终端用户侧的 Agent / App 目标默认托管在云端（Azure / M365 / 自托管公网服务）。

### 需要标注的「未能核实」项

1. **Task Module 专门文档页**（`task-modules-and-cards/task-modules/...`）多次访问返回 404，未单独核实；其能力已由 Action 命令官方文档（define-action-command）中的 dialog / task module 描述覆盖，结论不受影响。
2. **Azure Bot Service 文档**（`azure/bot-service/...`）本次访问返回 404，Bot Framework 的服务端鉴权细节未能从该源核实；本报告改用 Teams 官方 SSO 文档与 Teams SDK 文档表述（JWT 校验、Entra ID、tokenExchange）。
3. **微软官方 roadmap 页面**：未获取到面向公众的远期路线图文档；「AI 倾斜 + 命令通道并存」判断基于官方文档现状与 2026 发布/弃用公告（developer-announcements）这一可核实事实，非官方远期承诺。
4. 本报告未做深度数据验证（如 Teams Store 命令量、市场份额统计）。

---

## 小结（对「确定性命令触发」方向的判断）

- Teams 是**确定性 Slash 命令 + Message Extension 的主流范本之一**：`/` 唤起 autocomplete 菜单 → manifest 强类型参数（text/number/date/toggle/choiceset）或 Adaptive Card/Task Module 表单 → Bot Framework/Teams Bot Service 的 HTTPS invoke activity 直达服务端，链路纯程序化、无 LLM，且持续维护（2026 年 Agent Slash 命令 GA 就是新证据）。
- **双通道明确并存**：确定性命令（slash / message extension）与 AI 通道（Agents / declarative agent / custom engine agent / Copilot Studio）由同一份 app manifest 承载、官方均主推；官方给两者的分工是互补而非替代——确定性命令用于高频可重复的强约束动作，AI 负责开放式对话与自动调工具。
- 「命令 → 用户本地」在 Teams 有官方明确的工程先例（Dev Tunnels / ngrok / Agents Toolkit 本地调试 + sideload），但**仅限开发者/自托管场景**，Dev Tunnels 明示非生产；面向终端用户的本地执行不是主推方向。
- **对本方案启示**：Teams 证明「纯确定性命令触发」是**被主流长期保留并仍在加码的通道**；其战略重心确实向 AI Agent 大幅倾斜（SDK 全线 AI 化、Copilot 生态），但确定性命令作为「确定性执行面」与 AI 互补共存，验证了该方向既非过渡、也非会被 AI 取代的短期形态。

