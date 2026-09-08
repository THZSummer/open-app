# Slack（Slack Platform）竞品调研报告

> 调研主题：IM 内 `/` 唤起命令 → 选中命令 → 输入参数 → 触发用户本地 HTTPServer 的「确定性命令触发」（全程无 AI/LLM 参与）
> 调研日期：2026-09-08
> 信源：全部引用 Slack 官方开发者文档（docs.slack.dev / api.slack.com / slack.com help center），未核实内容已明确标注

---

## 【产品描述】

- **归属**：Slack Technologies, LLC，属 **Salesforce** 旗下（官方页脚注：`© 2026 Slack Technologies, LLC, a Salesforce company`）。
- **形态**：企业级团队协作 IM（频道/私信/工作流），同时是开放平台（Slack Platform：Apps / APIs / Block Kit / Workflows / Agent / MCP）。
- **定位**：企业"工作流中枢"——沟通 + 自动化（Workflow Builder）+ 应用市场（Slack Marketplace）+ 近年的 AI Agent（Agents / Agentforce / Slack AI）。
- **目标用户**：企业团队（全体员工）、开发者/运维（通过平台构建应用）、企业 IT 管理者。
- **同类型分类**：属于「同类型 IM」——Slack 是**提供确定性 Slash 命令的 IM 平台**，与本方案形态最接近（`/` 唤起命令 → 参数 → HTTP 直达服务端）。

---

## ① 非 AI 本地 Command 支持（对应维度 A~D）

**结论先行：Slack 提供成熟、纯确定性的命令通道（无 LLM 参与），是本方案的直接对标物。**

### A. 命令入口与唤起

- **支持 `/` 唤起命令列表**：在消息输入框输入前导斜杠 `/` 或点击输入框旁的**斜杠图标**，即打开 **shortcuts menu**（快捷菜单）。菜单会展示最近使用项，并支持输入名称搜索 app 的 shortcut、slash command、workflow。
- **交互形态**：消息输入框内的**下拉式快捷菜单**（非侧栏/面板）；选中后命令文本回填到输入框，回车发送即触发。命令需要的额外信息会在菜单中提示。
- **命令清单来源**：客户端本地 + 服务端下发**混合**——
  - **平台内置命令（built-in slash commands）**：如 `/topic`、`/remind`、`/invite`、`/shrug` 等，每个 workspace 开箱即用，由 Slack 平台维护（`/shrug` 这类甚至不需要网络，属客户端内置）。
  - **应用注册命令**：由 App 通过 **App Manifest** 声明（`features.slash_commands`），App 安装到 workspace 后立即对所有成员可用（服务端下发到客户端）。
  - **全局快捷方式（global shortcuts）**：出现在输入框 shortcuts 按钮中，与 slash command 并列，作为另一种 `/` 入口。
- **限制**：开发者创建的 slash command **不能在 message thread 中调用**（内置命令和部分官方 App 例外）；Enterprise Grid 的跨 workspace 频道/DM 中调用时会让用户先选 workspace。

> 信源：
> - https://docs.slack.dev/interactivity/implementing-slash-commands
> - https://docs.slack.dev/interactivity/implementing-shortcuts
> - https://slack.com/help/articles/360057554553-Use-shortcuts-to-take-actions-in-Slack

### B. 参数输入

两种形态并存：

| 形态 | 说明 | 参数如何声明 |
|------|------|-------------|
| **自由文本（text）** | 命令后的全部文本作为单一 `text` 参数发给应用（如 `/todo ask @a to do x`） | 无强类型 schema；创建命令时填 **Usage Hint** 提示用户格式；可选开启 "escape channels/users/links"（把 @/ # 转成 ID） |
| **Modal / Block Kit 表单** | 命令内通过 `trigger_id` 打开 **Modal**，用 Block Kit 的 `input` block 收集结构化参数 | 模态表单以 JSON view 定义：`plain_text_input`、`select menu`、`multi-select`、`radio buttons`、`datepicker` 等；提交时 `view_submission` 返回结构化 `view.state.values`（block_id + action_id 索引） |

- **结构化参数声明**：Slash Command 本身只支持 `text + usage_hint`（弱声明）；真正强类型参数有两处——
  - **App Manifest 的 `functions` 段**：`input_parameters` 用 JSON Schema 风格声明参数类型（`string`、`slack#/types/channel_id`、`user_id` 等）与 `is_required`，用于自定义 Workflow 步骤/函数。
  - **Modal view 对象**：以 JSON 直接定义表单字段，是命令参数收集的主流方式。
- 命令数上限：Manifest 中 **slash_commands 最多 50 个**、**shortcuts 最多 10 个**。

> 信源：
> - https://docs.slack.dev/surfaces/modals
> - https://docs.slack.dev/reference/app-manifest
> - https://docs.slack.dev/interactivity/implementing-slash-commands

### C. 命令注册与下发

- **维护方**：`平台内置命令（Slack 维护）` + `第三方/自研 App 注册（开发者维护）`，均非"用户本地上报"。
- **注册方式**：开发者通过 **App Manifest**（YAML/JSON）声明 `features.slash_commands`、`features.shortcuts`；也支持 App 管理后台 UI 逐个创建；Manifest 可被 `apps.manifest.*` API 程序化管理。
- **发现机制**：用户输入 `/` 打开 shortcuts 菜单搜索（按名称）；slash command 安装后即被索引到菜单；shortcut 出现在 composer 按钮；App 可上架 **Slack Marketplace** 分发到更多 workspace。

> 信源：
> - https://docs.slack.dev/app-manifests/configuring-apps-with-app-manifests
> - https://docs.slack.dev/reference/app-manifest
> - https://slack.com/help/articles/360057554553-Use-shortcuts-to-take-actions-in-Slack

### D. 触发到本地的链路

- **链路机制**：Slash Command 触发时，Slack 向应用配置的 **Request URL** 发 **HTTP POST**（`application/x-www-form-urlencoded`），payload 含 `command`、`text`、`user_id`、`team_id`、`channel_id`、`response_url`（临时 webhook，用于异步/延迟回复）、`trigger_id`（3 秒内可开 Modal）等字段。**应用需在 3 秒内回 HTTP 200 确认接收**。
- **安全鉴权**：官方**签名校验（Signing Secret + `X-Slack-Signature` 头，HMAC-SHA256 + 时间戳防重放）**为当前标准；旧的 verification token 已弃用。另有可选 **mTLS**。Bolt SDK 内置该校验。
- **能否送达"用户本地"——有官方明确的先例与工程路径**：
  1. **内网穿透（ngrok）**：官方文档（Slackbot MCP Client 与 Slack MCP Server 开发教程）反复演示 `ngrok http 3000` 把本地服务器暴露为公网 HTTPS，再填进 Request URL / Manifest——这是 Slack 官方认可的**本地开发范式**。
  2. **Socket Mode**：以 WebSocket 长连接替代公网 Request URL，官方明说适合"企业防火墙后 / 不便暴露静态 HTTP 端点"的场景——**本地/内网接收命令无需公网入口**。代价：Socket Mode App 不能上 Slack Marketplace。
  3. **Slack CLI `slack run`**：本地跑 Bolt 应用的标准流程。
  - 即：Slack 支持"命令 → 用户本地 HTTPServer"的链路，但**面向开发者开发场景**；对终端用户，命令目标默认是公网托管服务。
- **回复**：可立即在 HTTP 200 body 回消息（ephemeral 仅发起人可见 / in_channel 全频道可见，支持 Block Kit），或经 `response_url` 异步回。

> 信源：
> - https://docs.slack.dev/interactivity/implementing-slash-commands
> - https://docs.slack.dev/authentication/verifying-requests-from-slack
> - https://docs.slack.dev/apis/events-api/using-socket-mode
> - https://docs.slack.dev/ai/slackbot-mcp-client（"Expose your local server with ngrok" 示例）
> - https://docs.slack.dev/ai/slack-mcp-server/developing（ngrok redirect URL 示例）

---

## ② AI Agent Skill 支持（对应维度 E~F）

### E. 确定性 vs AI

- **确定性为主**：Slash Command / Shortcut / Block Kit 交互 / Workflow Builder / Incoming Webhook 的触发链路**完全确定性**——Slack 不"理解"命令语义，只做 HTTP 转发 + 签名校验 + 3 秒超时，全程无 LLM。**存在纯净确定通道（无 AI），且是该平台最成熟的通道**。
- **AI 是叠加的新通道（均有 LLM）**：官方 AI 能力全部在确定性基础设施之上构建——
  - **Agents（新建默认）**：自主目标导向 AI 应用，"receive input → reason → call tools → stream/render" 响应循环，用 LLM（Claude SDK / OpenAI SDK / Pydantic AI 任选）驱动。
  - **Slackbot MCP Client**：外部 MCP server 通过 HTTP 接入 Slack；**Slackbot（LLM）根据用户自然语言 prompt 自动发现并调用工具**——这是"AI 决策调工具"，与确定性映射相反。
  - **Slack MCP Server**：供 Cursor / Claude 等 AI 客户端通过 `https://mcp.slack.com/mcp` 访问 Slack 数据与动作（LLM 侧调用）。
  - **Slack AI / Agentforce**：见 F。
- 官方文档在 Slash Command / Modal / Shortcut 教程末尾附"**Try it with AI**"示例（命令收集数据 → 送 LLM → 回答案），印证 AI 是**可选叠加**而非替换。

> 信源：
> - https://docs.slack.dev/ai/
> - https://docs.slack.dev/ai/agent-quickstart
> - https://docs.slack.dev/interactivity/implementing-slash-commands
> - https://docs.slack.dev/surfaces/modals

### F. 与 Agent Skill 的关系：**并存（双通道）**

- **双通道明确并存**：App Manifest 同时承载两套能力——
  - 确定性通道：`features.slash_commands`、`features.shortcuts`、`functions`、`workflows`。
  - AI 通道：`features.agent_view` / `assistant_view`、`mcp_servers`、`external_auth_providers`。
- **MCP 双向打通**（AI 与命令互相引用）：
  - 外部工具 → Slack：Slackbot MCP Client（AI 基于 prompt 调用你的 MCP 工具）。
  - Slack → AI 客户端：Slack MCP Server（你的 App/Agent 作为 MCP 客户端读取 Slack 数据、发消息、操作 Canvas/List）。
- **Agentforce（Salesforce）**：Slack 内以 **Agent Actions**（标准 Slack Agent Actions + 自定义 Apex Invocable 调 Slack Web API）把 agent 能力接入；Agentforce agent 安装后出现在 Slack 侧栏/Agentforce tab。本质是"AI Agent 调用 Slack API/命令"的又一层叠加。
- **替代/互补判断**：官方未表述"AI 取代 slash 命令"。分工上是**互补**：确定性命令适合**强约束、结构化、可预期**的交互（表单收集、确定性动作）；AI 通道适合**开放对话 + 自动选工具**。两者在同一 App 内可同时启用。

> 信源：
> - https://docs.slack.dev/ai/
> - https://docs.slack.dev/reference/app-manifest
> - https://docs.slack.dev/ai/slackbot-mcp-client
> - https://docs.slack.dev/ai/slack-mcp-server
> - https://docs.slack.dev/ai/customizing-agentforce-agents-with-custom-slack-actions

---

## ③ 未来规划（对应维度 G）

### Slack 对"AI Agent Skill / MCP"路线的投入（官方信号明确）

- **战略重心明显向 AI Agent 倾斜**：
  - 开发者文档首页主打入口是 "**Create an agent**"（agent-quickstart）。
  - Agents 被官方定义为 Slack 的下一代 AI 产品形态；**Agent messaging experience（`agent_view`）取代旧的 Assistant（`assistant_view`），官方明确 `assistant_view` 最终将被废弃并要求迁移**——这是官方路线图里最明确的"AI 化"信号。
  - **MCP 双通道**被列为官方 AI 战略支柱：Slackbot MCP Client（外部工具接入 Slack）+ Slack MCP Server（AI 访问 Slack），配合 Agentforce 集成、Agent 专属 surface（split-view 容器、流式输出、Plan/Task 展示）。

### Slack 对"本地执行 / 本地 agent"的投入

- **开发链路层面**：官方持续投入"本地运行"工程能力——Slack CLI `slack run`、**ngrok 内网穿透**本地开发范式、**Socket Mode**（WebSocket 免公网 URL，官方定位为"防火墙后/不便暴露端点"场景）仍是一等公民文档能力。说明"命令/回调送达本地服务"的链路是官方支持的工程范式。
- **产品层面**：面向终端用户的"本地 agent / 本机执行"**未见于官方公开主推路线**；Slack 主推的是托管在云端的 App / Agent / MCP server。这一点官方未作明确产品路线图表述。

### 需要标注的「未能核实」项

1. **官方公开 roadmap 页面/博客对"确定性命令 vs AI"的远期时间表**：本次未获取到 Slack 官方公开的路线图文档（如 roadmap 页面）。上述"战略倾斜"判断**基于官方文档现状与版本迁移信号（assistant_view → agent_view 的废弃声明）**，属可核实的事实推断，非官方远期承诺。
2. **Slack 官方对"本地 agent"作为终端用户产品方向**的表述：未找到官方信源，标注**未能核实**；仅确认了开发场景的本地链路先例（ngrok + Socket Mode + Slack CLI）。
3. 本报告未做深度数据验证（如命令量、市场份额统计）。

---

## 小结（对"确定性命令触发"方向的判断）

- Slack 是**确定性 Slash 命令的最成熟范本**：`/` 唤起 → text/Modal 参数 → 签名校验的 HTTP POST 直达服务端，链路纯程序化、无 LLM，且至今完整维护。
- Slack **同时**以双通道并存的姿态主推 AI（Agents + MCP 双向 + Agentforce），AI 叠加在确定性基础设施之上，二者官方定位为互补而非替代。
- "命令 → 用户本地"在 Slack 有明确工程先例（ngrok / Socket Mode / Slack CLI），但**仅限开发者场景**，非面向终端用户的主推产品方向。
- 对本方案启示：Slack 证明"纯确定性命令触发"是**已被主流验证、仍被保留**的通道；但其战略重心已明确移向 AI Agent/MCP，确定性通道作为"确定性执行面"与 AI 通道互补共存，二者并非零和。
