# 02 · Discord（Discord Developer Platform）技术调研报告

> 调研主题：IM 中通过 Slash 命令唤起命令列表 → 选命令 → 输参数 → 触发到用户本地 HTTP Server（全程无 AI Agent / LLM，纯程序化确定性传递）
> 调研日期：2026-09-08　·　信源：以 Discord 官方 Developer 文档为主（webfetch 实采），辅以公开报道核实 AI 动向

---

## 【产品描述】

- **归属**：Discord Inc. 自研平台，非收购产品。开发侧为 **Discord Developer Platform**（Discord 开发者平台）。
- **形态**：以 IM/语音聊天为入口的"社区 + 游戏 + 开发者"一体化平台；面向开发者提供 Bot（应用）、Activities（嵌入式应用）、Social SDK（游戏社交层）三类应用形态。
- **定位**：面向游戏玩家、社区/服务器运营者、以及 bot / 游戏 / 嵌入式应用开发者的通讯与开放平台；开发者通过 Developer Portal 注册 Application，用 HTTP API + Gateway（WebSocket）+ 出站 Webhook（Interactions Endpoint URL）与平台交互。
- **目标用户**：① 游戏/社区用户（使用 bot 与 slash 命令）；② 开发者（构建 bot、Activity、游戏社交集成）；③ 企业/团队（Discord 亦被用作团队通讯）。
- **同类型竞品备注**：**同类型 IM** —— Discord 是提供确定性 Slash 命令（`/` 唤起命令列表 → 参数输入 → HTTP 回调）的 IM 平台，与本调研方向形态高度一致。

---

## ① 非 AI 本地 Command 支持（对应维度 A~D）

**结论先行：Discord 提供业界最成熟的"确定性 Slash 命令"体系之一 —— 全程无 LLM 参与，命令/参数/触发链路全部程序化。**

### A. 命令入口与唤起

- **唤起方式**：用户在聊天输入框输入 `/`，客户端弹出 **command picker（命令选择器）**，展示可用的应用命令列表；也可直接打开命令选择器选择。见官方文档描述："Slash commands ... show up when a user types `/`"。
- **交互形态**：命令选择器为客户端 UI（下拉列表式）。共有 4 类 Application Command：
  - `CHAT_INPUT`（1，Slash 命令，`/` 唤起）
  - `USER`（2，右键用户菜单）
  - `MESSAGE`（3，右键消息菜单）
  - `PRIMARY_ENTRY_POINT`（4，用于在 App Launcher 中启动 Activity）
- **命令清单：服务端下发**。命令不是客户端本地生成，而是由应用通过 HTTP API 注册到 Discord 服务端，Discord 再下发给客户端展示；客户端不保存"本地命令清单"。

### B. 参数输入

- **声明方式（参数 schema）**：每个 `CHAT_INPUT` 命令在注册时通过 **JSON 定义 `options` 数组**（最多 25 个），即应用命令对象上的结构化 schema。字段包括 `type / name / description / required / choices / min_value / max_value / min_length / max_length / autocomplete / channel_types / options`。
- **参数类型（Option Type）**：`SUB_COMMAND`(1)、`SUB_COMMAND_GROUP`(2)、`STRING`(3)、`INTEGER`(4)、`BOOLEAN`(5)、`USER`(6)、`CHANNEL`(7)、`ROLE`(8)、`MENTIONABLE`(9)、`NUMBER`(10)、`ATTACHMENT`(11)。支持服务端/客户端输入校验（required、min/max 值、min/max 长度）。
- **输入交互**：
  - **Choices（固定选项）**：`STRING/INTEGER/NUMBER` 可声明 `choices`（≤25 个），用户只能从这些选项中选（"they are the only valid values"）。
  - **Autocomplete（动态补全）**：声明 `autocomplete: true` 后，用户输入触发 `APPLICATION_COMMAND_AUTOCOMPLETE`（交互类型 4）交互，应用返回最多 25 个建议 `choices`（响应类型 8）。选项不限于应用给定的 choices。
  - **Subcommand / Subcommand Group**：命令可嵌套子命令/子命令组，最多一层 group 嵌套，用于组织命令树。
  - **Modal（表单弹窗）**：应用可返回 `MODAL`（回调类型 9）弹窗，内含文本输入组件，收集结构化表单数据（`MODAL_SUBMIT` 交互类型 5）。这是"结构化表单"形态的官方支持。
- **触发后数据**：应用收到 `APPLICATION_COMMAND`（类型 2）交互，`data.options` 里是用户填的 `name → type/value` 结构化键值，程序可直接读取。

### C. 命令注册与下发

- **谁维护命令清单**：**应用通过 HTTP API 注册**（`POST /applications/{id}/commands` 或 `/guilds/{gid}/commands`），使用 Bot token 或 `applications.commands.update` scope 的 client credentials token 鉴权。
- **作用域（scope）**：
  - **Global 命令**：全局生效，所有把应用加进的服务可用，支持版本 read-repair（更新后客户端自动重载）；上限 100 个 global `CHAT_INPUT`。
  - **Guild 命令**：仅指定服务可见，更新即时生效，适合测试。
  - **安装上下文 / 交互上下文**：`integration_types`（GUILD_INSTALL / USER_INSTALL）与 `contexts`（GUILD / BOT_DM / PRIVATE_CHANNEL）决定命令在哪些位置可用。**user-installed 应用**的命令可在用户 DMs/任意服务器使用——这与"命令可达个人用户"相关。
- **发现机制**：用户在命令选择器/`/` 输入中看到命令；无权限的命令不会显示（权限由 `default_member_permissions`、命令权限覆盖、`applications.commands` scope 控制）。应用分发/发现走 App Directory / App Launcher / 社交分享。
- **限流**：每 guild 每天最多 200 次命令创建（全局限流）。

### D. 触发到本地的链路

- **如何送达应用**：交互有 **两条互斥通道**（见官方 "Receiving an Interaction"）：
  1. **Gateway（WebSocket）**：`INTERACTION_CREATE` 事件，应用需保持长连接；
  2. **出站 Webhook（HTTP）**：应用配置 **Interactions Endpoint URL**（一个**公共 URL**），Discord 以 HTTP POST 方式把交互请求发到该 URL。
- **安全鉴权（官方强制）**：HTTP 通道下每个请求带 `X-Signature-Ed25519`（ed25519 签名）与 `X-Signature-Timestamp`（时间戳）；应用必须用 Developer Portal 中应用的 **Public Key（ed25519）** 对 `timestamp + body` 做签名验证，验证失败返回 `401`。Discord 会例行发送伪造签名做安全检查。首次配置时还需响应 `PING` 请求（`type: 1` → 返回 `PONG`）。
- **响应约束**：交互 token 15 分钟有效；初始响应须在 **3 秒内**；响应与后续消息都走 `/interactions/{id}/{token}/callback`（本质是 Webhook）。
- **能否送达"用户本地"——结论：官方通道不能直连用户本地**：
  - Interactions Endpoint URL 必须是**公网可达的 URL**；Discord 主动出站，无法直接回调到用户 NAT 后的本地 HTTP Server（除非用户自己暴露端口 / 做内网穿透）。
  - **官方"本地"能力的最近形态是 RPC over IPC**（本地 IPC socket：`\\?\pipe\discord-ipc-*` / `${XDG_RUNTIME_DIR}/discord-ipc-*`），但它是**本机应用 ↔ 本机 Discord 客户端的本地集成**（Rich Presence、语音、频道订阅等），方向是"本地应用访问 Discord 客户端"，并非"IM 命令 → 用户本地服务"，且官方建议新项目改用 Social SDK。
  - **生态层本地执行先例（第三方，非官方）**：社区普遍做法是让**运行在用户本机的 agent 主动向 Discord 拨出 Gateway 长连接**（由本地侧发起，绕开"Discord 回呼本地"），从而让用户通过 Discord 消息/命令触发本地执行。可核实先例：Anthropic **Claude Code Channels** 将 Claude Code 本地 agentic harness 接到用户自己的 Discord / Telegram（第三方报道确认）。**Discord 官方未提供"命令直达用户本地 HTTP Server"的原生通道**。

> 信源（官方）：
> - Application Commands（命令类型/options/choices/subcommand/autocomplete/注册/权限）：https://docs.discord.com/developers/interactions/application-commands.md
> - Receiving and Responding（交互对象/两条通道/响应/token 约束）：https://docs.discord.com/developers/interactions/receiving-and-responding.md
> - Interactions Overview（Interactions Endpoint URL / PING / X-Signature-Ed25519 签名验证）：https://docs.discord.com/developers/interactions/overview.md
> - Overview of Apps（应用形态 / user-installed / 安装上下文）：https://docs.discord.com/developers/quick-start/overview-of-apps.md
> - RPC（本地 IPC 集成）：https://docs.discord.com/developers/topics/rpc.md
> - Webhook Events（出站 Webhook 的签名头/PING，与 Interactions 同套安全机制）：https://docs.discord.com/developers/events/webhook-events.md
>
> 补充说明：专门的 "Interaction Security" 独立页面旧 URL（/security/securing-requests）访问返回 404，签名验证的官方权威描述位于 Interactions Overview / Webhook Events 两页，已据此引用，未编造该独立页面 URL。

---

## ② AI Agent Skill 支持（对应维度 E~F）

### E. 确定性 vs AI

- **命令通道为纯确定性**：Slash 命令的唤起、参数解析、触发全部是程序化/结构化过程，**平台层面不存在 LLM 理解或意图决策层**。命令注册（JSON schema）、参数输入（options/choices/autocomplete）、触发（Interactions 签名回调）均无需任何 AI 参与。
- **是否存在纯确定通道（无 LLM）**：**存在且为主通道**。`/` Slash 命令即"无 AI、纯确定性"通道，与我们的调研方向完全对应。
- **官方平台是否引入 AI**：
  - 官方曾做过一次 **in-product AI 实验 "Clyde"**（基于 OpenAI 技术的对话式 AI 助手，支持在 DM/群/服务器中召唤），后于 **2023-12-01 正式下线**（多方报道证实，Engadget/TechTimes 等）。即 Discord 的官方"内置 AI 助手"尝试已终止。
  - 截至调研日（2026-09-08），官方 Developer 文档与变更日志中**未见"平台级 AI Agent / Function Calling / 原生 AI Skill"类产品**（llms.txt 索引、Change Log 均无 AI SDK/AI Agent 条目；搜索"Discord AI SDK official"无官方结果）。

### F. 与 Agent Skill 的关系

- **无原生 AI Agent Skill**：Discord 不提供"MCP / Function Calling / 自定义 Agent"的平台内置能力。**AI 对话与 AI Agent 全部由第三方实现**——它们本质仍是普通 Bot，跑在完全相同的 Bot + Interactions（Slash 命令 / 消息）API 之上。
- **生态现状（第三方为绝对主力）**：社区已涌现大量把 Discord 作为 **AI Agent 用户接口**的连接件，例如：
  - `discord-mcp-agent`（GitHub/PyPI）："MCP server for Discord-based AI agent-user communication"，让 AI agent 通过 Discord 与用户交流；
  - Composio / Google ADK / Crypto.com 等 SDK 的 Discord 插件/集成；
  - Anthropic Claude Code Channels 把本地 Claude Code harness 接到 Discord/Telegram。
  - 这些都属于**第三方基于开放 API 的自建**，非 Discord 官方能力。
- **Discord 官方对 AI 的投入（仅文档侧）**：官方在 2026-03 的 Docs 改版中新增 **"AI & MCP support"**——提供 Copy-as-Markdown、`llms.txt`、以及 docs 的 MCP server（`https://docs.discord.com/mcp`），**目的是让 AI 开发工具更容易"读取 Discord 文档"**，属于文档消费侧能力，不是平台 AI 执行通道。
- **确定性命令与 AI 的关系**：**并存、互补**。官方定位上确定性 Slash 命令是第一公民（所有 bot 的交互入口都是它）；AI 是第三方 bot 在"命令背后"叠加的能力。两者不互斥：AI bot 也通过确定性命令/消息与用户交互。

> 信源：
> - 官方（文档/变更日志证实无平台级 AI 执行能力；docs 的 AI & MCP 支持）：https://docs.discord.com/llms.txt ；https://docs.discord.com/developers/change-log.md
> - Clyde 下线（第三方报道核实）：https://www.engadget.com/discord-is-already-killing-clyde-its-experimental-openai-chatbot-155231238.html （以及 TechTimes 等）
> - 生态第三方 AI 连接件（核实存在性）：https://github.com/zebbern/discord-mcp-agent ；https://pypi.org/project/discord-mcp-agent/ ；Claude Code Channels 接 Discord/Telegram：https://venturebeat.com/orchestration/anthropic-just-shipped-an-openclaw-killer-called-claude-code-channels
> - **未能核实**：Discord 是否存在任何官方在研的 AI Agent Skill / AI SDK 路线（官方文档与变更日志均未披露，公开搜索亦无官方信源），故按"未发现官方该产品"记录，不臆测。

---

## ③ 未来规划（对应维度 G）

### 对"本地执行 / 本地 agent"的官方投入

- **官方无"IM 命令 → 用户本地执行"路线图表述（未能核实到官方路线图）**。官方文档与变更日志中**未出现**"local execution / 本地 agent 直达"相关规划。
- 最接近"本地"的官方能力是 **RPC over IPC**（本机应用 ↔ 本机客户端，2026-04 变更日志还新增了 RPC over IPC 文档），方向仍是"本地应用访问 Discord 客户端"，且官方明确建议新项目改用 Social SDK（重心在游戏社交层），说明该通道并非 Discord 主推方向。
- **Activities（嵌入式应用）** 是 Discord 在"应用形态"上的重点投入：嵌入式 Web 应用跑在 Discord 频道内（游戏/协作工具），通过 `PRIMARY_ENTRY_POINT` 命令 + `LAUNCH_ACTIVITY` 回调启动——它强调"在 Discord 内执行"，**不是"在用户本机执行"**。

### 对"AI / 自动化"路线的官方投入

- **AI 态度谨慎、以"生态赋能"为主**：内置 AI 助手 Clyde 下线后，官方未再推出平台级 AI 执行产品；公开可见的 AI 投入集中在"让第三方更容易对接"与"文档 AI 友好化"：
  - docs 提供 `llms.txt` + MCP server（2026-03 变更日志）；
  - 平台重点仍放在 **Activities / Social SDK / 变现（Premium Apps）/ 发现（App Directory）** 等确定性、平台化能力上（2024–2026 变更日志绝大多数条目属此类）。
- **趋势判断（基于现有证据）**：确定性 Slash 命令是 Discord 开发者生态的**长期基础设施与当前主流**，短期内不会被 AI 取代；AI/Agent 以第三方 bot 形态与确定性命令**并存互补**；Discord 官方并未公开承诺"AI Agent Skill"路线（**未能核实任何官方 AI 路线图**）。

> 信源：
> - 官方 Change Log（Activities / RPC over IPC / Social SDK / 变现 / docs AI & MCP 等投入轨迹）：https://docs.discord.com/developers/change-log.md
> - Activities 定位：https://docs.discord.com/developers/activities/overview.md 与 https://docs.discord.com/developers/quick-start/overview-of-apps.md
> - RPC over IPC：https://docs.discord.com/developers/topics/rpc.md
> - **未能核实**：Discord 官方对"本地执行 / 本地 agent"及"AI Agent Skill"的任何正式路线图/官方公告（官方渠道未披露）。

---

## 附：一句话结论

**Discord 拥有业界最成熟的确定性 Slash 命令通道（`/` 唤起 → options/choices/autocomplete/modal 参数 → 签名 Webhook/长连接触发），全程无 LLM，是本调研方向最强的"同类型对标"；官方无原生 AI Agent Skill（内置 AI 实验 Clyde 已下线，AI 全靠第三方 bot + MCP 连接件叠加），且官方通道无法直连用户本地服务，本地执行需靠"本地侧主动拨出长连接"的生态做法。**
