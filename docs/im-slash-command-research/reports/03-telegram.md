# 03 · Telegram（Telegram Bot Platform / Bot API）竞品调研报告

> **调研主题**：IM 内 Slash 命令唤起列表 → 选中命令 → 输入参数 → 直达用户本地 HTTPServer 的「确定性命令触发」模式，是否业界主流 / 主推 / 未来趋势
> **调研日期**：2026-09-08
> **分类**：同类型竞品（提供确定性 Bot Commands 的 IM 平台）

---

## 【产品描述】

- **归属**：Telegram（Telegram Messenger Inc.，云端即时通讯平台 + Bot Platform）。
- **形态**：Bot 平台形态——Bot 是运行在 Telegram 内的「小型应用」，开发者通过 **Bot API**（HTTPS 接口）把 Bot 连接到自己的服务端；用户通过消息 / 命令 / 按钮 / 内联模式 / Mini App 与 Bot 交互。
- **定位**：平台只负责「消息管道 + 交互 UI + 生命周期」，**Bot 逻辑（含任何 AI 或非 AI 处理）完全由开发者自托管实现**。
- **目标用户**：Bot 开发者（个人 / 企业 / AI 团队）+ 普通用户。平台官方数据：托管 **超过 1000 万个 Bot**，对用户和开发者免费；Telegram 约 9.5 亿用户，其中每月 5 亿用户使用 Mini App。
- **同类型 IM 备注**：✅ **同类型竞品**——Telegram 是提供确定性 Bot Commands（`/command` 命令清单、回调、Webhook 触发）的 IM 平台，与「IM 内 `/` 唤起命令 → 参数 → 触发本地服务」形态高度接近。

---

## ① 非 AI 本地 Command 支持（维度 A~D）

> **结论先行**：Telegram 拥有成熟的**纯确定性命令通道**（Bot Commands），是平台级基础输入设施；命令可经 getUpdates 长轮询或 setWebhook 送达开发者自托管的任何服务端（含本地机器）。**全程无 AI、无 LLM 参与。**

### A. 命令入口与唤起形态

- 命令即 `/keyword`，最多 32 字符，只能用小写拉丁字母、数字、下划线；官方建议尽量「命令即动作」（`/newlocation` 优于 `/new` 再要参数）。
- **唤起方式（三种）**：
  1. 用户输入 `/` → 客户端**建议支持的命令列表（带描述）**，选中即发送；
  2. 命令在消息中**高亮**，点击立即重发；
  3. **菜单按钮**（Menu Button）：私聊输入框旁常驻，默认打开命令列表（`MenuButtonCommands`），也可配置为直接启动 Mini App（`MenuButtonWebApp`）。
- **清单渲染归属**：**客户端本地渲染**（高亮、`/` 联想、菜单），但**清单内容（命令 + 描述）由 Bot 服务端固定注册**（BotFather / `setMyCommands`），客户端按「scope 解析算法」拉取对应清单。即：**渲染在客户端，内容在服务端**。

**信源**：https://core.telegram.org/bots/features#commands 、https://core.telegram.org/bots/features#menu-button 、https://core.telegram.org/bots/api#menubutton

### B. 参数输入形态

| 输入方式 | 说明 | 结构化程度 |
|---|---|---|
| 命令后自由文本 | `/command arg1 arg2`，纯文本约定，随命令一起以 Update 文本送达 | 无 schema |
| 内联键盘按钮（Inline Keyboard） | 按钮带 `callback_data`，点击后以 `CallbackQuery` 回传，**不进入聊天记录** | 结构化（data 字段） |
| 自定义键盘（Reply Keyboard） | 预定义选项按钮，点击即发送对应文本；支持 `input_field_placeholder` | 半结构化 |
| ForceReply | 强制用户以回复形式输入，配合分步引导收集多轮参数 | 分步采集 |
| Chat / User Selection | 按钮请求用户/聊天，回传 `chat_shared` / `users_shared` 服务消息 | 结构化 |
| Mini App（Web App） | JS 表单，结构化采集后回传 `web_app_data` | 最结构化 |

- **命令如何声明参数**：**无参数 schema**。`BotCommand` 对象只有 `command`、`description`（及 `is_ephemeral`），**没有任何参数定义/声明字段**；参数输入完全靠「纯文本约定 + 回调按钮 + 键盘」等运行期机制，由开发者自行解析。官方甚至不鼓励命令带参数（建议拆成更具体的命令）。

**信源**：https://core.telegram.org/bots/api#botcommand 、https://core.telegram.org/bots/features#keyboards 、https://core.telegram.org/bots/features#inline-keyboards 、https://core.telegram.org/bots/api#callbackquery 、https://core.telegram.org/bots/api#forcereply 、https://core.telegram.org/bots/features#chat-and-user-selection

### C. 命令注册与下发

- **注册方**：两条路——① @BotFather `/setcommands`（人工注册）；② Bot API `setMyCommands`（程序化，一次最多 100 条）。
- **按用户下发**：7 种 `BotCommandScope`（default / all_private_chats / all_group_chats / all_chat_administrators / chat / chat_administrators / chat_member），可叠加 `language_code`，不同用户/群/语言看到不同命令清单；客户端按官方「Determining list of commands」优先级算法取最终清单。
- **发现机制**：`/` 输入联想 + 菜单按钮 + 命令高亮；另有 **Deep Linking**（`t.me/bot?start=xxx` → bot 收到 `/start xxx`）用于带参唤起。
- **⚠️ 官方明确提示**：Update 对象**不含 scope 信息**，且可能包含 bot 未注册的命令——**后端必须自行校验命令合法性及用户授权**（确定性信任不能依赖客户端）。

**信源**：https://core.telegram.org/bots/features#command-scopes 、https://core.telegram.org/bots/api#setmycommands 、https://core.telegram.org/bots/api#botcommandscope 、https://core.telegram.org/bots/api#determining-list-of-commands

### D. 触发到本地的链路机制（关键技术难点）

- **两种互斥的更新获取方式**（任选其一，不可同时）：
  1. **`getUpdates` 长轮询（pull）**：本地 Bot 进程**主动外连** `api.telegram.org` 拉取 Update JSON——**不需要公网入站端口**，官方 webhook 指南明确认可「从自家机器跑 Bot」的场景（"running your bot from a nice machine"）。这是「命令送达本地」最零门槛的官方通道。
  2. **`setWebhook`（push）**：Telegram 主动向指定 URL POST JSON。官方硬性要求：**公网可达、IPv4、HTTPS（TLS1.2+）、端口仅 443/80/88/8443、证书 CN/SAN 与域名一致**；不支持裸 HTTP、不支持内网 IP。
- **安全鉴权**：① bot token（开发者调用 Bot API 的凭证）；② webhook 可配 `secret_token`，每请求带 `X-Telegram-Bot-Api-Secret-Token` 头供本地服务校验来源；③ FAQ 建议在 webhook URL 里塞入 secret path（如 `…/your_token`）。
- **能否送到「用户本地」**：官方提供开源 **Local Bot API Server**（telegram-bot-api），本地部署后可**对 webhook 使用 HTTP URL、任意本地 IP、任意端口**——官方显式支持把更新投递到本地地址。此外「公网 HTTPS URL + 内网穿透」是把 webhook 打回本地机器的常见工程做法（如 ngrok / frp / tailscale）——**ngrok 等具体工具未被官方文档提及，属于基于其「公网 URL + TLS」要求的行业通用实践推断，未能由官方信源直接核实**。
- **成熟先例**：是。主流 Bot 生态大量采用「本地/自托管进程 + 长轮询」或「VPS + webhook」；「命令 → 本地 HTTP Server」在 Telegram 上是成熟、官方机制完整支撑的链路。

**信源**：https://core.telegram.org/bots/api#getting-updates 、https://core.telegram.org/bots/api#setwebhook 、https://core.telegram.org/bots/api#using-a-local-bot-api-server 、https://core.telegram.org/bots/webhooks 、https://core.telegram.org/bots/faq

---

## ② AI Agent Skill 支持（维度 E~F）

> **结论先行**：Telegram 的命令触发**以确定性为主，且存在纯确定通道（无 LLM）**；Bot API **无原生 AI Agent Skill / Function Calling / MCP**——AI 全部靠第三方接入，确定性命令与 AI 是**并存互补**关系。

### E. 确定性 vs AI

- **纯确定性通道客观存在且是平台主通道**：`/command` 文本 + 结构化回调，经 Update JSON 原样送到开发者后端，由代码确定性路由。整个 Bot API 参考文档**全文不含 AI / LLM / function calling / MCP / skill 等概念**（对官方 API 文档全文检索无任何匹配），无任何「平台侧 LLM 介入命令解析」的机制。
- 官方定位表述：**「模型、记忆与 Bot 逻辑完全由开发者决定」**——平台只提供消息、多端呈现、接入与生命周期能力。即确定性由 Bot 开发者代码保证，平台不掺 AI。

**信源**：https://core.telegram.org/bots/features#ai-agents 、https://core.telegram.org/bots/api

### F. 与 Agent Skill 的关系

- **无原生 AI Agent Skill 层**：Telegram 没有自己的模型、没有平台内置 Agent/LLM 编排能力，也不提供 MCP / Function Calling 协议端点。AI 能力以**第三方 AI Bot** 形式接入，与普通 Bot 完全同构（同一个 Update 通道、同一套 Bot API）。
- 平台为 AI Bot 提供的是**「AI 友好展示层」**：Streaming Replies（流式草稿回复）、Rich Messages（结构化富文本输出）、Topics（并行多会话）、Guest Bots（在任意聊天 @ 召唤 AI）、Bot-to-Bot 通信（多 Agent 编排）、Secretary/Business Bots（代理用户账号的自动化）。
- **与确定性命令的关系 = 并存 / 互补**：同一 Bot 既提供 `/command` 确定性入口，也可由开发者接第三方 LLM 处理自然语言；两者共用同一条命令/消息输入通道，互不取代。官方没有任何「用 AI 取代 slash 命令」的表述。

**信源**：https://core.telegram.org/bots/features#ai-agents 、https://telegram.org/blog/ai-bot-revolution-11-new-features 、https://core.telegram.org/bots/features#streaming-replies 、https://core.telegram.org/bots/features#guest-bots

---

## ③ 未来规划（维度 G）

> 官方**无正式公开路线图文档**（FAQ 对功能请求的回应是「平台会持续观察开发者用法再决定方向」）。以下基于官方 Blog 与 Bot Features 的最新口径（2026 年 5-8 月）整理，均附信源。

### 官方对「AI / 自动化」的战略投入

- **主推 AI Bot 生态（但只做管道）**：官方 Blog 以「AI Bot 革命」为题发布 10+ 项 AI 相关功能，称 Telegram 是「唯一让所有 AI 模型自由竞争、用户完全掌控的平台」；方向集中在让第三方 AI 更好接入与呈现（Guest Bots 任意聊天召唤、流式回复、富文本、Bot-to-Bot 自治 Agent、Chat Automation 让 Bot 代理用户账号处理消息）。**注意：模型与 Agent 逻辑仍 100% 由开发者自托管，官方不提供 AI 推理 / Agent 运行时。**
- **对「本地执行 / 本地 Agent」**：官方**没有**「本地 agent」路线。官方唯一的「本地」概念是开源的 **Local Bot API Server**（在开发者自己机器上跑 Bot API 网关）。「Bot 逻辑托管在哪」是开发者自决事项，官方既不托管也不排斥。

### 对「确定性命令」路线的态度

- 确定性 Bot Commands 是**持续演进的基础设施**，而非被淘汰的过渡品：2026 年 Bot API 10.x 仍在大幅增强命令/交互——如命令可声明 `is_ephemeral`（群内私密命令）、按钮/键盘增加 `force_reply`、`DisabledButton`、Rich Messages 内的交互按钮等。**无任何官方迹象要用 AI 取代 Slash 命令。**

### 综合判断（对调研目标 G 的回应）

1. **确定性命令触发 = Telegram 长期主流**：`/` 唤起命令列表 → 参数 → 触发开发者服务端，是平台第一等的官方能力，十年来持续增强。
2. **AI 是叠加层而非替代**：官方战略是「平台做管道 + 开发者自托管逻辑」，AI 与确定性命令在同一条通道上**并存互补**。
3. **与「确定性命令直达本地服务」方向兼容性强**：Telegram 提供了该方向所需的全部机制（命令清单、结构化回调、webhook/长轮询、本地 Bot API Server），且官方明确「逻辑开发者自理」——本调研方向在 Telegram 生态中是被支持、可行、且不被 AI 化路线挤压的。

**信源**：https://core.telegram.org/bots/faq#will-you-add-x-to-the-bot-api 、https://telegram.org/blog/ai-bot-revolution-11-new-features 、https://core.telegram.org/bots/features#ai-agents 、https://core.telegram.org/bots/api#recent-changes 、https://core.telegram.org/bots/api#using-a-local-bot-api-server

---

## 调研说明

- 本报告全部结论基于 **Telegram 官方信源**（core.telegram.org / telegram.org/blog）在线核实，未访问的信源未引用。
- 「ngrok / frp 内网穿透接 webhook」为基于官方「公网 HTTPS URL + 固定端口 + TLS」要求的行业通用实践推断，**官方文档未直接提及，已标注未能核实**。
- 完整 API 参考（约 26k 行）已全文检索：确认 Bot API 无 AI / LLM / function calling / MCP / skill 相关字段或方法。
