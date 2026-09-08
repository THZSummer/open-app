# 04 · 飞书 / Lark（飞书开放平台，字节跳动）竞品调研报告

> **调研主题**：IM 内 `/` 唤起命令列表 → 选中命令 → 输入参数 → 触发用户本地 HTTPServer 的「确定性命令触发」（全程无 AI/LLM 参与），是否业界主流 / 主推 / 未来趋势
> **调研日期**：2026-09-08
> **分类**：同类型竞品（提供确定性命令 + 交互卡片的 IM 开放平台）

---

## 【产品描述】

- **归属**：字节跳动（飞书开放平台 open.feishu.cn / 海外版 Lark open.larksuite.com）。
- **形态**：企业级协同办公 IM + 开放平台。开放能力包括：机器人（应用机器人 / 自定义机器人）、**Slash Command（`/` 命令，OpenAPI 注册）**、消息卡片（Card，含交互组件与回调）、Bot 菜单、事件订阅（HTTP Webhook / WebSocket 长连接）、服务端 OpenAPI、**飞书 MCP**（AI 工具化接入）、**飞书智能伙伴创建平台 Aily**（AI 应用开发）。
- **定位**：企业数字化协同底座——IM 是入口，机器人 / 卡片 / 命令负责「把业务能力塞进聊天」；近年战略重心明显向「AI（Aily 智能伙伴 / MCP / Coze Bot）」倾斜，但**确定性命令与卡片通道仍在官方一等公民维护，且被官方列入 Agent 最佳实践**。
- **目标用户**：企业组织（全员）、企业开发者 / ISV（通过开放平台构建应用）、企业 IT 与数字化管理者、AI Agent 开发者（MCP / CLI）。
- **同类型分类**：✅ **同类型竞品**——飞书是提供确定性命令（输入 `/` 唤起命令面板）、交互卡片、事件回调触发通道的 IM 平台，与本方案「`/` 唤起命令 → 参数 → 触发服务端/本地」形态高度接近；其官方 Slash Command 文档甚至归入「Agent 最佳实践」，说明确定性命令同时被用作 AI 能力的发现入口。

---

## ① 非 AI 本地 Command 支持（对应维度 A~D）

> **结论先行**：飞书提供**成熟、纯确定性的命令通道**——Slash Command（OpenAPI 注册，输入 `/` 唤起命令面板）+ 消息卡片交互（结构化收参）+ 事件订阅（WebSocket 长连接 / HTTP 回调）送达开发者服务端，**全程无 LLM 参与**。其中 **WebSocket 长连接是官方主推、且明确支持「本地开发环境接收事件、无需内网穿透」的通道**，与钉钉 Stream 模式同构。

### A. 命令入口与唤起

飞书的「命令入口」是**多形态并存**：

| 入口 | 交互形态 | 是否 `/` 唤起 |
|------|---------|:---:|
| **Slash Command（快捷指令）** | 用户在聊天输入框输入 `/` → 客户端**弹出命令面板**（command panel，带命令名 + 描述 + 图标）→ 用户选择命令 → 触发机器人服务。官方原话：「Slash Command lets users quickly trigger bot services by typing `/` in the Lark chat box」 | ✅ |
| **Bot 菜单（自定义菜单）** | 机器人与用户单聊时，输入框上方**常驻菜单按钮**，点击触发菜单事件（`bot.menu` 事件） | ❌ |
| **机器人 @ 对话** | 群 / 单聊内 `@机器人` + 文本，机器人收到 `im.message.receive_v1` 事件 | ❌ |
| **消息卡片交互组件** | 卡片内按钮 / 输入框 / 下拉 / 日期等组件，点击触发卡片回调 | ❌ |
| **消息快捷操作** | 长按 / 右键消息唤起快捷操作菜单（连接消息与应用） | ❌ |

- **命令清单归属**：**服务端下发 + 客户端本地缓存渲染**——命令通过 OpenAPI 注册到飞书服务端，客户端按缓存策略拉取并渲染命令面板（缓存约 3 分钟，增删改约 5 分钟生效；Lark 客户端需 **7.70**（桌面）/ **7.71**（移动）及以上）。
- **命令上限**：每个应用最多注册 **100** 条 Slash Command。

> 信源：
> - https://open.feishu.cn/document/mcp_open_tools/agent-best-practices/agent-supports-slash-commands （Lark agent supports slash commands：`/` 唤起、命令面板、版本要求、缓存延迟、100 条上限）
> - https://open.feishu.cn/document/uAjLw4CM/ukTMukTMukTM/bot-v3/bot-overview （Bot 概览：Bot 菜单、@机器人、卡片交互、AI 场景）

### B. 参数输入

- **Slash Command 的「命令清单声明」无参数 schema**：注册字段只有 `command`（命令名，不含 `/`）+ `description`（`default_value` + `i18n` 多语言）+ 可选 `icon`（图标 key）。**没有任何「参数声明 / manifest 字段」**——与 Telegram `BotCommand` 类似，参数不随命令声明。
- **参数输入三种形态**（与 Slack 思路接近：命令无强类型 schema，参数靠运行期机制）：
  1. **命令后自由文本**：用户选中命令后继续输入文本，随命令以**普通消息**发给机器人，由开发者代码自解析。
  2. **消息卡片结构化收参（主推形态）**：命令触发后机器人回一张「表单式卡片」，用卡片**交互组件**收集参数：
     - `input` 输入框（自由文本）、`select_static` / `multi_select_static` 单选 / 多选下拉、`select_person` / `multi_select_person` 选人、`date_picker` / `picker_time` / `picker_datetime` 日期时间、`select_img` 图片选择、`checker` 勾选；
     - **`form` 表单容器**：本地缓存一批表单项，点击提交按钮**一次性回调**开发者服务器，实现「异步批量提交多个表单项」；
     - 按钮 `button` 点击 → **卡片回调**（`card.action.trigger` 类）回传开发者服务端。
  3. **Bot 菜单 / 快捷指令入口跳转**：跳转式入口，无强类型参数。

> 信源：
> - https://open.feishu.cn/document/mcp_open_tools/agent-best-practices/agent-supports-slash-commands （命令 manifest：command + description + icon，无参数 schema）
> - https://open.feishu.cn/document/uAjLw4CM/ukzMukzMukzM/feishu-cards/card-json-v2-components/component-json-v2-overview （Card JSON 2.0 交互组件清单：input / select_static / select_person / date_picker / picker_time / form 表单容器 等）
> - https://open.feishu.cn/document/uAjLw4CM/uMzNwEjLzcDMx4yM3ATM/develop-a-card-interactive-bot/introduction （卡片交互机器人：输入框收参 → 点击按钮 → 回调更新卡片）

### C. 命令注册与下发

| 能力 | 维护方 | 注册方式 | 发现机制 |
|------|-------|---------|---------|
| **Slash Command** | 应用开发者 | **OpenAPI 注册**：`POST /open-apis/application/v7/app_slash_commands`（Create）+ `GET`（List）+ `PATCH`（Update）+ `DELETE`，需 `application:app_slash_command:write` / `read` 权限 + `tenant_access_token`；**服务端强制命令名唯一**（重复创建返回 `command already exists`） | 用户输入 `/` 弹出命令面板；服务端下发 + 客户端缓存 |
| **Bot 菜单** | 应用开发者 | 开发者后台 / 开放平台配置自定义菜单 | 机器人单聊输入框上方常驻 |
| **消息卡片** | 应用开发者 | 卡片构建工具 / 卡片模板 / 服务端 API 发送 | 消息内嵌卡片，随业务推送 |

- **命令发现机制**：以 `/` 输入联想 + 命令面板为主；**无「命令市场 / 命令商店」**（命令按应用隔离，非全局市场）。
- **命令清单本地还是服务端**：**服务端下发**（开发者通过 API 注册），客户端仅负责渲染与缓存——不存在「用户本地上报命令清单」的先例。

> 信源：
> - https://open.feishu.cn/document/mcp_open_tools/agent-best-practices/agent-supports-slash-commands （四个 CRUD 端点、权限 scope、`tenant_access_token`、100 条上限、命令名唯一、客户端缓存下发）
> - https://open.feishu.cn/document/uAjLw4CM/ukTMukTMukTM/bot-v3/bot-overview （Bot 菜单 / 机器人能力注册）

### D. 触发到本地的链路

飞书把「平台 → 应用服务端」的推送分为**两种订阅方式**，且官方**主推 WebSocket 长连接**：

| 链路 | 机制 | 对公网的要求 | 安全鉴权 |
|------|------|------------|---------|
| **WebSocket 长连接（官方主推，SDK 内置）** | 集成飞书 SDK（Go / Python / Java / Node.js），本地服务主动**外连** `wss://`，与开放平台建立全双工通道；事件 / 卡片回调 / 订阅回调均经此通道推送 | **无需公网 IP / 域名 / 内网穿透工具**；只需运行环境**能访问公网（出站）**。官方原话：「Events can be received in the **local development environment** through the persistent connection mode, and once the local service is deployed online, it can work immediately」 | 连接建立时以 **APP_ID + APP_SECRET** 鉴权；传输加密，**无需额外加解密 / 签名校验**；每应用最多 **50** 条连接；收到事件需 **3 秒**内处理，否则触发超时重试 |
| **HTTP 回调（发送到开发者服务器）** | 配置「事件订阅 / 回调请求地址」，飞书以 HTTP POST + JSON 推送 | 需**公网 HTTPS 请求地址** | ① **Verification Token**（请求体明文比对，简单但安全性低）；② **签名校验**（配 Encrypt Key 时）：`X-Lark-Signature = sha256(X-Lark-Request-Timestamp + X-Lark-Request-Nonce + encrypt_key + 原始请求体)`；③ 配 Encrypt Key 后事件体经 **AES-256-CBC** 加密，需先解密再解析；收到回调需 **3 秒**内返回 |

- **能否送到「用户本地」——官方有明确支持，路径与钉钉 Stream 一致**：
  1. **WebSocket 长连接**：官方显式支持「本地开发环境接收事件」，本地服务仅需出站公网，**无需内网穿透**——这是官方对「命令/回调直达本地服务」的最强背书（官方称开发周期从「约一周」缩短到「五分钟」）。
  2. **HTTP 回调 + 内网穿透**：HTTP 模式要求公网 HTTPS URL，本地机器需靠第三方内网穿透把回调打回本地；**官方文档未直接提及具体穿透工具（ngrok 等），属基于其「公网 URL」要求的行业通用实践推断，未能由官方信源直接核实**。
- 补充：**自定义机器人（群 Webhook）**为另一确定性通道，但仅支持**单向推送**（`POST https://open.feishu.cn/open-apis/bot/v2/hook/xxx`），**不能接收用户消息 / 无回调**；安全设置含**自定义关键字、IP 白名单、签名**（`HmacSHA256(timestamp + "\n" + secret)` + Base64，timestamp 距当前 ≤1 小时）。
- **成熟先例**：是。飞书官方「回显机器人 / 卡片交互机器人」教程均以**本地长连接**跑通（三分钟快速开发），大量企业应用以「本地/自托管进程 + 长连接」或「服务器 + HTTP 回调」两种方式落地——「命令/消息 → 本地服务」在飞书上是官方机制完整支撑的链路。

> 信源：
> - https://open.feishu.cn/document/server-docs/event-subscription-guide/event-subscription-configure-/request-url-configuration-case （Receive events through websocket：无需公网 IP/内网穿透、本地开发环境可收、50 连接、3 秒、APP_ID/APP_SECRET）
> - https://open.feishu.cn/document/server-docs/event-subscription-guide/event-subscription-configure-/encrypt-key-encryption-configuration-case （Step 3 接收事件：Verification Token / 签名校验 / AES-256-CBC 解密）
> - https://open.feishu.cn/document/event-subscription-guide/callback-subscription/receive-and-handle-callbacks （接收回调：两种订阅方式、安全校验、3 秒响应）
> - https://open.feishu.cn/document/client-docs/bot-v3/add-custom-bot （自定义机器人 Webhook：关键字 / IP 白名单 / 签名 / 仅单向推送）

---

## ② AI Agent Skill 支持（对应维度 E~F）

> **结论先行**：飞书的命令触发**以确定性为主，且存在纯确定通道（无 LLM）**；同时官方重投入 **AI 通道**（飞书智能伙伴 Aily、飞书 MCP、Coze AI Bot），确定性命令与 AI 是**并存互补（双通道）**关系，且官方把确定性 Slash Command 明确列为「Agent 最佳实践」作为 AI/机器人的发现与快速入口。

### E. 确定性 vs AI

- **纯确定性通道客观存在且是基础设施**：Slash Command（OpenAPI 注册 + 命令面板唤起）、消息卡片交互回调、Bot 菜单事件、事件订阅（WebSocket / HTTP）——全部是**纯程序化、无 LLM**：平台不「理解」命令语义，只做「注册命令清单 → 收到 `/` 触发 → 推送给开发者服务 → 开发者代码路由」。
- **命令注册/触发全程无 AI**：Slash Command 的创建 / 更新 / 删除是纯 REST OpenAPI（`tenant_access_token` 鉴权），与「AI 理解自然语言、决策调工具」无关。
- **AI 是叠加层**：飞书的 AI 能力做成**独立产品线**（Aily / MCP / Coze AI Bot），底层复用飞书的确定性能力（发消息、卡片、群组、回调）：
  - **Coze（扣子）AI Bot 发布到飞书**：以 AI Bot 形态接入，与普通机器人共用同一套机器人 / 卡片 / 流式消息基础设施（官方 Bot 概览的「AI 场景」）。
  - **飞书 MCP**：把飞书 OpenAPI 封装成「面向大模型的工具化能力」（详见 F）。
  - **飞书智能伙伴创建平台（Aily）**：企业级 AI 应用开发平台，围绕 LLM 提供「AI 技能编排、知识数据处理、效果调优、持续运营」，应用一键发布到飞书 / Web 等渠道（详见 F / G）。
- 判断：飞书同时存在**纯确定通道（成熟、仍在一等公民维护）**与 **AI 通道（新增、重投入）**，AI 通道**叠加**在确定性基础设施之上，未取代确定性通道。

> 信源：
> - https://open.feishu.cn/document/mcp_open_tools/agent-best-practices/agent-supports-slash-commands （纯确定性命令注册 / 触发）
> - https://open.feishu.cn/document/uAjLw4CM/ukTMukTMukTM/bot-v3/bot-overview （Bot「AI 场景」：Coze Bot 发布到飞书，复用机器人/卡片/流式能力）
> - https://open.feishu.cn/document/uAjLw4CM/ukTMukTMukTM/mcp_integration/mcp_introduction （飞书 MCP：OpenAPI 封装为工具化能力）

### F. 与 Agent Skill 的关系：并存（双通道）

- **双通道明确并存**：
  - **确定性通道**：Slash Command / Bot 菜单 / 消息卡片交互 / 事件订阅——用户显式选命令、显式填参，程序确定性转发到服务端。
  - **AI 通道**：飞书智能伙伴 Aily（自然语言交互 + AI 技能编排 + 知识 + MCP 服务接入）、飞书 MCP（AI Agent 调用飞书能力）、Coze AI Bot。
- **官方把 Slash Command 归入「Agent 最佳实践」文档**（文档路径 `/document/mcp_open_tools/agent-best-practices/agent-supports-slash-commands`），明确定位为「让用户快速触发 Bot 服务、让能力更易被发现和使用」——即**确定性命令被官方用作 AI 机器人能力的「发现入口 / 快速唤起」，而非被 AI 取代**。官方给确定性命令列出的用例包括：`/todo` 建任务、`/approval` 走审批（命令触发后机器人发卡片分步引导）、`/deploy` `/log` 等**命令行式运维工具**——与本调研方向「IM 变轻量运维入口」高度一致。
- **飞书 MCP 的方向是「AI → 飞书」**（让 AI Agent 调用飞书能力），与「用户 → 命令 → 本地服务」的确定性方向互为镜像、互补不冲突：
  - **远程调用（官方推荐）**：通过唯一服务 URL 连接飞书官方 MCP 服务，当前支持**云文档（Docs）场景**，后续将开放多维表格（Base）、日历等更多场景；面向两类用户——终端用户（个人授权 AI 访问飞书数据）与开发者（应用/用户身份集成到自有 AI Agent）。
  - **本地调用（不推荐，开发者自部署）**：开源的 **lark-openapi-mcp**（github.com/larksuite/lark-openapi-mcp），支持**飞书全部服务端 OpenAPI** 能力（消息、群组、日历、多维表格等），支持 `tenant_access_token` 与 `user_access_token`，与 Trae / Cursor / Claude 等本地 AI 工具无缝集成——这是飞书对「AI Agent 与本地工具链」的官方工程投入。
- 小结：飞书是「**AI 决策层 + 确定性执行/入口层**」并存的典型——Aily / MCP / Coze 是 AI 大脑，Slash Command / 卡片 / 机器人是确定性入口与执行通道，二者互补，官方无「用 AI 取代 slash 命令」的表述。

> 信源：
> - https://open.feishu.cn/document/mcp_open_tools/agent-best-practices/agent-supports-slash-commands （Slash Command 归入 Agent 最佳实践；`/todo` `/approval` `/deploy` `/log` 用例）
> - https://open.feishu.cn/document/uAjLw4CM/ukTMukTMukTM/mcp_integration/mcp_introduction （飞书 MCP 概述：Docs 场景先行、远程/本地两种调用、Agent 用户与开发者两类）
> - https://open.feishu.cn/document/mcp_open_tools/mcp-overview （本地 OpenAPI MCP 概述：全量 OpenAPI、双凭证、Trae/Cursor/Claude 集成）
> - https://github.com/larksuite/lark-openapi-mcp （官方开源 MCP 仓库，由官方文档引用）

---

## ③ 未来规划（对应维度 G）

### 对「AI Agent Skill / MCP」路线的投入（信号强烈）

- **Aily（飞书智能伙伴创建平台）是飞书 AI 战略的核心**：官方定位「企业级智能应用开发平台，围绕大语言模型（LLM）提供 AI 技能编排、知识数据处理、效果调优和持续运营能力，让用户可以通过自然语言方式与应用交互」，应用**一键发布到飞书 / Web 等多个渠道**；Aily 帮助中心入口现显示「豆包工作伙伴」，表明该平台已与豆包生态深度整合（官网页面为 JS 渲染，正文细节未能直接抓取，以上结论依据官方入口页/帮助页快照）。
- **MCP 是明确的官方路线**：飞书官方推出 OpenAPI MCP，官方表述「基于 OpenAPI 面向 AI 场景封装优化，将传统接口调用升级为工具化能力……当前支持云文档场景，**后续将开放多维表格、日历等更多场景**」——「AI Agent 通过 MCP 调用飞书」在官方路线图中有明确演进规划。
- **AI Bot 生态**：Coze Bot 发布到飞书为官方 Bot 概览中的标准「AI 场景」，AI 与确定性机器人/卡片共用同一基础设施。

### 对「本地执行 / 本地 agent」的投入

- **开发阶段「本地接收」是官方正式能力**：WebSocket 长连接模式官方明确支持「本地开发环境接收事件、无需公网 IP/域名/内网穿透工具」，是飞书对「回调/事件直达本地服务」的最强工程背书；但**对终端用户的「用户个人本机 agent / 本地执行」产品形态，官方公开文档未见明确路线图表述**（未能核实）。
- **面向本地 AI 工具链的官方开源 CLI**：飞书官方开源「飞书CLI」（lark-cli，官网页标题「飞书CLI｜让 AI 直接操作你的飞书」，Agent-Native 设计，可直接供 Claude Code / Cursor / Trae 等 AI Agent 调用操作飞书）——这是飞书对「本地 AI Agent 操作飞书」的官方投入信号（方向是 AI 操作飞书，而非飞书操作本地）。

### 对「确定性命令」路线的态度

- **确定性通道持续维护，且被官方推荐**：Slash Command 是较新的官方能力（依赖 Lark 7.70+/7.71+ 客户端），提供完整 CRUD OpenAPI、命令面板交互、i18n、图标，并被官方写进「Agent 最佳实践」推荐给 Bot/Agent 开发者；**无任何官方迹象要用 AI 取代 Slash 命令**。
- 需要标注：**飞书官方公开 roadmap / 路线图页面本次未获取到**，「AI 战略倾斜」判断基于官方文档、官网入口页与发布动态现状，属可核实的事实推断，非官方远期时间表承诺。

### 综合判断（对调研目标 G 的回应）

1. **确定性命令触发 = 飞书长期主流基础设施**：`/` 唤起命令面板 → 参数（自由文本或卡片表单）→ 触发开发者服务端，是官方一等能力（OpenAPI 全生命周期维护），并在 AI 时代被官方作为「Agent 能力的发现入口」继续强化。
2. **AI 是叠加层而非替代**：Aily / MCP / Coze 是飞书重投入的 AI 通道，但官方表述与文档均显示「AI 与确定性命令双通道并存、互补」，确定性命令被官方主动用于「让 AI 机器人能力可被发现」，没有任何「确定性命令被 AI 取代」的表述。
3. **与「确定性命令直达本地服务」方向兼容性强**：飞书提供该方向所需全部机制（命令面板、结构化卡片收参、WebSocket 长连接直达本地、HTTP 回调签名/AES 鉴权），且官方对「本地开发环境接收事件」有明确背书——本调研方向在飞书生态中是被官方支持、有完整文档、且不被 AI 化路线挤压的。

> 信源：
> - https://www.feishu.cn/content/3d5z9ttt （飞书 aily 官方介绍页：AI 技能编排 / 知识 / 效果调优 / 一键发布到飞书、Web）
> - https://www.feishu.cn/landing/feishu_aily_sku 、https://aily.feishu.cn/hc （智能伙伴创建平台 / 豆包工作伙伴帮助中心；JS 渲染，正文未能直接抓取）
> - https://open.feishu.cn/document/uAjLw4CM/ukTMukTMukTM/mcp_integration/mcp_introduction （MCP 官方路线：Docs 先行，Base/日历后续）
> - https://www.feishu.cn/feishu-cli （官方开源飞书CLI：让 AI 直接操作飞书，支持 Claude Code / Cursor / Trae）
> - https://open.feishu.cn/document/mcp_open_tools/agent-best-practices/agent-supports-slash-commands （确定性命令持续维护 + Agent 最佳实践）
> - https://open.feishu.cn/document/server-docs/event-subscription-guide/event-subscription-configure-/request-url-configuration-case （本地开发环境接收事件）

---

## 调研说明（未能核实项清单）

1. **Slash Command 触发后的「送达事件类型」**：官方文档对「用户选定命令后，平台以什么事件/回调推给开发者服务端」未给出专门说明；按文档使用场景（「用户输入 `/approval` 后机器人发卡片分步引导」）推断为**以普通消息事件（`im.message.receive_v1`）到达机器人、由开发者自解析命令文本**，此推断未能由官方信源完全核实。
2. **Aily（飞书智能伙伴创建平台）正文**：`feishu.cn/landing/feishu_aily_sku`、`feishu.cn/content/3d5z9ttt`、`aily.feishu.cn/doc/...` 等官方页面为 JS 渲染，正文未能直接抓取；Aily 的「AI 技能编排 / 知识处理 / 一键发布」等表述来自官方搜索快照与入口页简介，细节（技能如何定义、是否含 MCP 接入配置等）未能逐一核实。
3. **飞书官方公开 roadmap / 路线图**：未获取到官方路线图文档，「AI 战略倾斜」为基于官方现状的事实推断。
4. **HTTP 回调 + 第三方内网穿透工具（ngrok / frp 等）**：官方文档未直接提及，为行业通用实践推断，未能由官方信源核实。
5. **消息快捷操作（message-shortcut）细节**：官方文档入口 `open.feishu.cn/document/client-docs/extensions/message-shortcut` 已在搜索快照中核实存在（「长按/右键消息唤起快捷操作、连接消息与应用」），但本报告未直接抓取其正文，交互细节未深究。

---

## 小结（对「确定性命令触发」方向的判断）

- 飞书提供**成熟、纯确定性的命令通道**：Slash Command（OpenAPI 注册，`/` 唤起命令面板，无参数 schema，参数靠自由文本或卡片表单收参）+ 消息卡片交互 + 事件订阅，全程无 LLM；**WebSocket 长连接是官方主推且明确支持「本地开发环境接收事件、无需内网穿透」的「直达本地」通道**，与钉钉 Stream 模式同构。
- 飞书**确实有 `/` 命令**（官方命名 Slash Command），形态与本方案最接近；且官方把该文档归入「Agent 最佳实践」，用例包括 `/todo`、`/approval`、`/deploy` `/log` 等——即确定性命令被官方同时定位为「Bot 服务入口」与「AI/Agent 能力的发现入口」。
- **AI 是叠加层而非替换**：Aily（智能伙伴）、飞书 MCP、Coze AI Bot 构成飞书 AI 战略，但确定性命令与 AI 通道**双通道并存、互补**，官方无「用 AI 取代 slash」的表述；MCP 方向是「AI → 飞书」，与「用户 → 命令 → 本地服务」互为镜像。
- 对本方案启示：飞书证明「确定性命令触发 + 事件直达本地（WebSocket 长连接）」是**被主流厂商官方支持并保留**的能力，且正被官方作为 AI 时代的入口基础设施强化；但其战略重心已明显向 Aily / MCP / Coze 倾斜，确定性命令更多承担「入口 + 执行面」角色，与 AI 互补共存。
