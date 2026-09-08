# IM Slash 命令直达本地服务 —— 整体调研对比汇总报告

> **调研日期**：2026-09-08
> **汇总范围**：14 份产品调研报告（7 个同类型 IM + 7 个不同类型 AI/自动化对照产品）
> **调研主题**：在 IM 中通过 Slash 命令（输入 `/`）唤起命令列表 → 选中命令 → 输入结构化参数 → 触发用户本地 HTTPServer 的「确定性命令触发」模式，全程无 AI Agent（LLM）参与，是否业界主流 / 主推 / 未来趋势
> **口径说明**：本报告全部事实来自 14 份单产品报告（信源均为各厂商官方文档 / 官方渠道）；各报告中标注「未能核实 / 网络受限未访问」的内容在本汇总中如实体现，不编造任何产品行为或 URL。

---

## 一、执行摘要

**核心结论：把「用户显式选命令 → 填结构化参数 → 无 LLM 直达本地服务」做成产品形态，并非任何主流厂商的主推方向；但「确定性命令触发」作为一条独立于 AI 的通道，被全部主流 IM 平台（Slack / Discord / Telegram / 飞书 / 钉钉 / 企微 / Teams）长期保留并持续维护，属于「成熟基础设施」而非「被 AI 取代的过渡品」。** 所有厂商的战略重心都已明显向 AI Agent / MCP 倾斜，但没有任何一家官方宣称「用 AI 取代确定性命令」；相反，头部厂商普遍采用「AI 决策层 + 确定性执行/入口层」的双通道并存结构（飞书把 Slash Command 写进 Agent 最佳实践、Teams 2026 年仍 GA 确定性 slash 命令、Anthropic 用 `disable-model-invocation` 把用户显式触发设为副作用护栏）。「直达用户本地」的最可行链路已被主流验证为 **WebSocket 长连接（本地主动外连、无需公网/内网穿透）**（飞书、钉钉 Stream、企微智能机器人长连接、Telegram 长轮询），而非公网 Webhook。本方向**不是业界主推，但具有长期存在的确定性价值**——open-app 若投入，宜定位为「确定性执行面 + 入口层」，与 AI 互补，而不是与之争夺「智能触发」。

---
## 二、对标矩阵总表（对象 × 维度 A~G）

> 图例：✅ 支持/成熟　⭕ 有限/间接　❌ 无/不提供　「双通道」= 确定性通道与 AI 通道并存
> 用户核心关注列：**E 确定性 vs AI、D 本地链路、G 未来规划**。

| 产品 | 分类 | A 命令入口 | B 参数输入 | C 命令注册 | D 本地链路 | E 确定性vs AI | F 与AI关系 | G 未来规划 |
|---|---|---|---|---|---|---|---|---|
| **Slack** | 同类型 IM | ✅ `/` 唤起快捷菜单（下拉）；内置+应用注册 | 自由文本 + Modal/Block 表单 | App Manifest 注册，非本地上报 | HTTP POST Request URL（签名校验，3 秒）+ Socket Mode 长连接免公网；ngrok 本地开发范式 | **确定性为主**（纯无 LLM 通道最成熟） | **双通道**并存，MCP 双向 | 重心向 AI Agent 倾斜（agent_view 取代 assistant_view）；保留确定性；本地仅开发者场景；**roadmap 未核实** |
| **Discord** | 同类型 IM | ✅ `/` 唤起 command picker；服务端下发 | JSON options schema + choices + autocomplete + modal | HTTP API 注册（global/guild） | 长连接/出站 Webhook 二选一；**官方不能直连本地**；本地靠「本地侧主动拨出长连接」（第三方，如 Claude Code Channels） | **纯确定性**（平台无 LLM） | 无原生 AI Skill（Clyde 已下线），AI 全靠第三方 Bot/MCP 连接件 | 无官方 AI 路线图；重点 Activities/Social SDK；本地执行未提（**未核实**） |
| **Telegram** | 同类型 IM | ✅ `/` 唤起列表 + 菜单按钮；服务端清单+客户端渲染 | 无参数 schema；文本+内联键盘+Mini App | BotFather / setMyCommands，按 scope 下发 | 长轮询（pull，**本地零门槛**）+ Webhook（需公网 HTTPS 固定端口）+ **Local Bot API Server 可投本地** | **纯确定性**（Bot API 全文无 AI） | 无原生 AI Skill，AI 靠第三方 AI Bot；**并存互补** | 无公开 roadmap；主推 AI Bot 生态（**只做管道**）；确定性命令 2026 仍增强 |
| **飞书 / Lark** | 同类型 IM | ✅ `/` 唤起命令面板（OpenAPI 注册）；多入口（Bot 菜单/@/卡片） | 无参数 schema；自由文本 + 卡片表单（input/select/date/form） | OpenAPI CRUD 注册，100 条上限；无命令市场 | **WebSocket 长连接（官方主推，本地可收，无需公网/内网穿透）** + HTTP 回调（公网，签名+AES） | **确定性为主**（纯无 LLM） | **双通道**并存；Slash Command 归入「Agent 最佳实践」；MCP 是「AI→飞书」 | AI 战略核心（Aily/MCP/Coze）；WebSocket 本地接收是官方能力；终端用户本地 agent/roadmap **未核实** |
| **钉钉** | 同类型 IM | ✅ 快捷指令 `/` 唤起菜单（**文档正文为空，未核实**）；主打机器人@+卡片+酷应用 | @文本自解析；卡片/酷应用表单结构化；快捷指令参数**未核实** | 应用/酷应用后台注册；非本地上报 | **Stream 模式（WebSocket 反向连接，官方主推，本地可收，「五零」）** + HTTP 回调（公网，签名+AES 1500ms）；内网穿透工具已废弃 | **确定性为主** | **双通道**并存；AI PaaS/DEAP（MCP 三类）；「AI 决策、确定性执行」 | 重心向 AI 助理/智能体/MCP 倾斜；Stream 本地明确；快捷指令文档/roadmap **未核实** |
| **企业微信** | 同类型 IM·能力有限 | ⭕ **无标准 `/` 菜单**；文本关键词+菜单按钮 key+智能机器人 | 无参数 schema；文本约定+按钮 key+卡片字段 | 应用/管理员后台配置；无 `/` 列表发现 | 自建应用回调（公网，Token+EncodingAESKey 签名加密）+ **智能机器人 WebSocket 长连接（官方支持内网/无公网 IP）** + 群 Webhook 单向 | **链路确定性**（无 LLM） | **双通道**并存；智能机器人（AI 定位）+ MCP 工具暴露 + OpenClaw 集成 | AI 智能办公一级战略（MCP/长连接密集迭代）；无 slash 新能力；roadmap **未核实** |
| **Microsoft Teams** | 同类型 IM | ✅ `/` 唤起 autocomplete；内置 + App/Agent 注册（2026 GA）+ Message Extension 三入口 | manifest 静态参数 + Adaptive Card 表单 + 内嵌网页 | app manifest 声明；Teams Store | Bot Framework HTTPS invoke（JWT 鉴权）+ Dev Tunnels/ngrok 本地隧道（**开发非生产**）+ API-based ME 免 Azure bot | **确定性为主**（纯无 LLM） | **双通道**并存；Agent Slash 命令（AI 之上叠确定性入口）；官方给分工指导 | 大幅向 AI Agent 倾斜（SDK 全线 AI 化），但 2026 仍 GA 确定性命令；roadmap **未核实** |
| **Agentforce**（Salesforce） | 不同类型 | ❌ 无 `/` 无命令列表；纯对话式（Agents tab/@提及） | 无用户填参；LLM 对话收集；schema 面向 LLM | Salesforce 侧注册（Agent Builder），Slack 安装 | 走 Slack↔Salesforce 平台连接；**出向 HTTP**；无「触达本地」模型 | **AI 驱动**；Agent Script/hybrid reasoning 内嵌确定性执行层 | 与 Slack 确定性命令**并存互补**（两套并行机制）；「确定性」降维为 AI 内部执行层 | AI Agent（数字劳动力）主线；MCP/gateway **在研（将来时）**；用户面主推对话式；部分官方页 403/JS 渲染**未核实** |
| **Copilot Studio**（微软） | 不同类型 | ❌ 无 `/` 命令概念；对话/@提及 | 无表单；AI 动态填参 | Maker 配置，云端运行时；无命令下发 | 无本地链路；云端 + 公网 Connector/REST/MCP | **AI 驱动**（完全对立面） | Agent **取代手工命令入口**；与 Teams 确定性入口集成**未核实** | 全面 AI-agent-first；Scout 本地桌面 AI（AI 自主本地执行，相反路线）；无确定性命令路线图 |
| **飞书 Aily / 钉钉 AI 助理** | 不同类型 | ❌ 无 `/` 清单；自然语言对话唤起 | 用户侧纯自然语言；Aily 有 **skill_id+结构化参数 API 旁路**；钉钉参数提取是 LLM | 无命令注册；技能/MCP/工作流=LLM 工具集 | 无用户命令直达本地；**云端 LLM 决策 → 公网 HTTP/MCP** | **AI 驱动**；Aily `app-skill/start` 是确定性执行 API | AI 助理（决策层）+ 平台确定性通道（执行层/入口层）**并存互补**；AI 取代「输入命令」心智而非执行通道 | 两家 AI 战略核心（Aily 并入豆包；钉钉 DEAP/AI PaaS）；无「AI 取代平台确定性命令」表述；roadmap **未核实** |
| **OpenAI Tool Calling**（Assistants/GPTs/FC） | 不同类型 | ❌ 无 `/` 无列表；`tools` 参数声明，模型决策 | 无用户填参；LLM 生成（strict schema 约束） | 工具=代码里 `tools` 参数；GPTs 开发者配置 | 云端 API；Secure MCP Tunnel（本地**出向**隧道）+ shell local；入口是模型调用 | **AI 驱动**（行业标杆）；确定性收敛为开发者侧约束（tool_choice/strict/审批） | 明确**对立面、并存互补**；官方 Skills 安全警示（bounded product experiences+审批）反向佐证确定性护栏价值 | 更强 agentic 编排 + 更强开发者侧确定性控制；MCP 战略；**不提供用户侧确定性命令通道** |
| **Claude Code Skills / MCP**（Anthropic） | 不同类型 | ✅ `/` 命令体系（内置确定性 + Skill/自定义 AI 驱动） | Skill 参数=文本替换（无类型校验）；MCP 参数 schema 给 LLM；**MCP Prompts 命令（最接近本方案 UX）** | Skill 本地文件系统注册；MCP 配置注册；协议级确定性发现（tools/list） | MCP stdio/HTTP；**「执行确定、决策 LLM」**；`disable-model-invocation` 用户显式触发护栏 | **AI 驱动**；确定性内置命令 + 用户显式触发护栏 | **并存互补**；确定性=工具底座/UI 管道，AI=业务执行主体；MCP Prompts 命令=本方案可对标参照 | Skills（开放标准，2025-12）+ MCP 全面铺开；多厂商治理；无「AI 取代确定性」表述；MCP 捐 Linux Foundation **未核实** |
| **GitHub Copilot CLI / AI CLI** | 不同类型 | ✅ `/` 命令体系（**多数确定性内置** + 少数 AI 驱动；Skill/Agent 调用 AI 驱动） | 参数=自由文本/位置，无 schema；ACP 命令清单+`input.hint`（执行端是 agent） | 内置固定集 + 插件/Skill 动态加入；可扩展注册**全部 AI 化** | `!`/`$` 本地 shell 透传；Hooks；MCP（LLM 决策）；无「用户→命令→本地 HTTP」形态 | **AI 驱动**（业务主体）+ 确定性 CLI（基础设施）+ 审批/沙箱护栏 | **并存分层**；确定性=CLI 底座+人类显式控制，AI=业务执行主体；本方案是其**空档** | 全面 AI 化（autopilot/cloud sandbox/ACP server）；确定性 CLI 长期保留；业务确定性命令**未纳入路线图**；官方 Blog/roadmap 404 **未核实** |
| **n8n / Zapier / Make / Coze / Dify** | 不同类型 | ⭕ 无 IM 内 `/` 菜单；Webhook/定时/手动触发器；IM 间接触发 | Webhook 接收结构化参数（JSON body）；节点参数声明 | 平台侧配置注册；Coze 工作流部署为 API | **确定性执行底座**（HTTP/Code/分支/循环，默认无 LLM）；「执行端点+Webhook」，**命令前端层空白** | **确定性工作流为主** + AI Agent 节点叠加 | **并存互补**；「AI 决策在上、确定性执行在下」；确定性工作流=AI Agent 的工具箱（Dify Agentic Workflow） | AI 化主轴 + **MCP 化（5 家全 MCP）**；确定性执行层被同步强化；无「AI 取代确定性」表述 |

**矩阵速读**：
- **E（确定性 vs AI）**：7 个同类型 IM 全部**确定性为主**（Discord/Telegram 甚至纯确定性、平台无 LLM）；7 个不同类型对照产品全部 **AI 驱动**——但其中 4 个（Agentforce / Copilot CLI / Claude Code / OpenAI）都在内部保留确定性执行层或开发者侧确定性约束，iPaaS 5 家的执行引擎本身是确定性代码。
- **D（本地链路）**：能「官方直达本地/内网」的通道全部是**长连接/长轮询**（飞书 WebSocket、钉钉 Stream、企微智能机器人长连接、Telegram 长轮询、Slack Socket Mode）；公网 Webhook 无一例外要求公网 HTTPS 地址，本地需内网穿透（官方文档多未直接提及穿透工具，属行业实践推断，标注未核实）。
- **G（未来规划）**：无任何厂商官方宣称「用 AI 取代确定性命令」；但战略重心普遍向 AI Agent/MCP 倾斜，确定性命令被定位为「执行面/入口层/基础设施」而非主推增长点。

---

## 三、三块内容横向对比

### 1. 非 AI 本地 Command 支持（维度 A~D）：哪些有纯确定性通道、能否直达本地

**① 有成熟确定性命令通道的 IM（6/7）**：Slack（`/` 快捷菜单 + Modal）、Discord（`/` command picker + JSON options）、Telegram（`/` + 键盘）、飞书（`/` 命令面板 + 卡片表单）、钉钉（`/` 快捷指令 + 卡片/酷应用）、Teams（`/` autocomplete + Message Extension）。它们的共同形态：`/` 唤起 → 命令清单**服务端下发**（应用/平台注册，非用户本地上报）→ 结构化参数（Discord/Teams 有强类型 schema；Slack/飞书/钉钉靠自由文本或表单组件）→ 触发到应用服务端，链路全程无 LLM。

**② 例外**：企业微信**没有标准 `/` 命令菜单**（官方文档体系检索未发现 slash 概念），等价形态是「文本关键词 + 菜单按钮 key + 智能机器人」，能力等级偏低（据实标注）。

**③ 能否「直达用户本地」——关键链路发现**：

| 链路类型 | 代表 | 能否本地/内网 | 说明 |
|---|---|---|---|
| **WebSocket 长连接（本地主动外连，反向接收）** | 飞书（官方主推）、钉钉 Stream、企微智能机器人 API 模式、Slack Socket Mode | ✅ **官方明确支持本地/内网** | 无需公网 IP/域名/内网穿透，仅需出站公网；飞书官方称「本地开发环境即可收事件」、钉钉官方「五零」表述（零公网 IP/TLS/防火墙/网关/穿透）、企微官方明确「服务部署在内网环境」场景 |
| **长轮询（pull）** | Telegram `getUpdates` | ✅ 本地零门槛 | 本地 Bot 进程主动外连，官方 webhook 指南认可「从自家机器跑 Bot」 |
| **Local API Server** | Telegram Local Bot API Server | ✅ 可投递本地地址 | 官方开源，本地部署后 webhook 可用 HTTP / 任意本地 IP / 端口 |
| **公网 Webhook（push）** | Slack Request URL、Discord 出站 Webhook、飞书/钉钉/企微 HTTP 回调、Telegram webhook | ❌ 需公网 HTTPS | 无一例外要求公网可达；本地需内网穿透（ngrok/frp 等），**官方文档多未直接提及穿透工具，属行业实践推断（未核实）** |
| **本地侧主动拨出长连接** | Discord（生态做法：Claude Code Channels 把本地 agent 接 Discord/Telegram） | ✅ 生态可行，非官方 | 由运行在用户本机的 agent 主动拨出长连接，绕开「平台回呼本地」，是 Discord 生态「命令/消息触发本地执行」的通行做法 |

> **链路结论**：业界主流且官方背书的「命令/回调直达本地」路径 = **本地服务主动外连的 WebSocket 长连接 / 长轮询**（方向是「本地 → 云端」），与「IM 云端 push 公网 Webhook 回本地」相反。Discord 官方通道甚至明确不能直连本地，只能靠本地侧主动拨出。**若 open-app 要「直达用户本地」，应优先采用本地主动外连长连接，而非依赖公网 Webhook。**

### 2. AI Agent Skill 支持（维度 E~F）：AI Skill 成熟度与双通道关系

**① 同类型 IM 的 AI 能力普遍是「叠加层」，且大多以 MCP 为接入协议**：

| 平台 | AI Skill / 工具调用形态 | 成熟度 |
|---|---|---|
| Slack | Agents（LLM 驱动）+ Slackbot MCP Client（外部工具接入）+ Slack MCP Server（AI 访问 Slack）+ Agentforce | 高（双通道并列在 App Manifest） |
| Discord | **无原生 AI Skill**（官方内置 AI 实验 Clyde 已于 2023-12 下线）；AI 全靠第三方 Bot + MCP 连接件（discord-mcp-agent、Claude Code Channels 等） | 低（官方无） |
| Telegram | **无原生 AI Skill**（Bot API 全文无 AI/LLM/MCP 字段）；AI 靠第三方 AI Bot，平台只提供流式回复等「AI 友好展示层」 | 低（官方无） |
| 飞书 | 飞书智能伙伴 Aily（AI 应用平台）+ 飞书 MCP（OpenAPI 工具化）+ Coze AI Bot | 高（官方重投入） |
| 钉钉 | AI PaaS + DEAP（智能体管理、内置/三方/自定义 MCP、19 官方技能 + 13 三方技能）+ AI 助理 | 高（官方重投入） |
| 企业微信 | 智能机器人（知识库问答 + API 模式，已开放 MCP 工具 streamableHTTP / JSON Config、OpenClaw 集成） | 中高（官方主推） |
| Teams | Agents（声明式 + 自定义引擎）+ Copilot Studio + MCP/A2A（Teams SDK v2） | 高（官方重投入） |

**② 确定性命令与 AI 的关系：全部 14 个对象中，除 Copilot Studio（AI 对话取代显式命令入口）外，其余全部是「并存/互补」**，且分工高度一致——**确定性命令负责「高频、可重复、强约束、需表单/强类型参数」的动作，AI 通道负责「开放式对话、意图理解、自动选工具」**。代表性官方表述：
- Teams 官方指导：「Keep the command set small and focused… reserve slash commands for the most repeatable actions」（确定性命令保留给最可重复的动作）。
- 钉钉 AI PaaS 原话：「大脑理解完后交给行动系统去执行」——AI 是决策层、确定性机器人/卡片/插件是执行层。
- Slack 官方将确定性通道（slash/shortcuts/functions）与 AI 通道（agent_view/mcp）并列在 App Manifest，二者可同时启用。
- Dify 官方定义 Agentic Workflow：「The AI is still doing the heavy lifting, but within boundaries you define」——AI 在确定性边界内工作。
- Anthropic `disable-model-invocation`：把「用户显式触发」设为 AI 不能抢跑的副作用护栏——**最 AI 化的厂商也认可人类显式触发的价值**。

**③ 唯一「部分替代」信号**：Copilot Studio 的叙事是「用自然语言告诉 agent 要做什么」，AI 对话**取代**显式命令交互（但微软在更宽的 Teams 平台层仍保留确定性 slash/Message Extension，与 Copilot Studio 的集成未核实）。Agentforce 同样不提供、也不打算提供「用户显式选命令填参」的产品形态（其确定性逻辑被降维为 AI 内部执行层）。

**④ 对 AI 侧最值得注意的确定性旁路**：飞书 Aily 提供 `app-skill/start` / `aily_session-run/create` API——**指定 skill_id + 结构化参数直接触发技能、不经 LLM 决策路由**，与本方案在 API 层同构，证明即使 AI 化平台也保留「程序化指定执行」的确定性出口。

### 3. 未来规划（维度 G）：各厂商战略重心与「本地执行」路线

**① 战略重心：全部向 AI 倾斜，但没有任何一家官方声明「用 AI 取代确定性命令」**：

| 厂商/产品 | 战略重心（官方信号） | 对确定性命令的态度 |
|---|---|---|
| Slack / Salesforce | AI Agent 主线（Slack 文档首页主打 Create an agent；`assistant_view` → `agent_view` 迁移并弃用旧版；Agentforce 定义「数字劳动力」；MCP 双向为战略支柱） | 保留确定性通道，但定位为「执行面/既有资产」，非重点投入 |
| Discord | 无官方 AI 路线图（Clyde 下线后未再出平台级 AI）；投入 Activities / Social SDK / 变现 / 文档 AI 友好化（llms.txt + MCP） | 确定性 Slash 命令是生态长期基础设施与当前主流，短期内不会被 AI 取代 |
| Telegram | 主推 AI Bot 生态但**只做管道**（官方称「唯一让所有 AI 模型自由竞争」；Guest Bots、Bot-to-Bot、Chat Automation） | 确定性命令 2026 年 Bot API 10.x 仍持续增强，无被取代迹象 |
| 飞书 | Aily（并入豆包生态，帮助中心改称「豆包工作伙伴」）+ 飞书 MCP（Docs 先行，后续开放 Base/日历）+ Coze | Slash Command 持续维护并被写入「Agent 最佳实践」 |
| 钉钉 | AI PaaS + DEAP（智能体/MCP/知识/模型）+ AI 数据本地化部署 | 确定性通道（机器人/卡片/快捷指令/酷应用）一等公民维护 |
| 企业微信 | 智能办公一级战略（智能机器人 + MCP + OpenClaw 集成，2025-2026 密集迭代） | 确定性回调/菜单仍在维护，但未见 slash/命令市场新能力 |
| Teams / 微软 | AI Agent 全面重组（Teams AI Library v1 弃用→Teams SDK v2 含 MCP/A2A；M365 Agents SDK；Copilot Studio 三种 harness） | 2026 年仍 GA Agent Slash Commands、targeted messages、prompt starters——确定性命令与 AI 同步演进 |
| Agentforce | MCP / AI agent gateway（官方将来时表述，在研）、Multi-Agent、Voice、Observability、Agent Script（确定性锚点） | 确定性被内化为 agent 内部执行层，不作为面向用户的产品形态主推 |
| Copilot Studio | 全面 AI-agent-first（GitHub Copilot harness 主推推理型；「Build with natural language」；路线图迁至 AI at Work） | Copilot Studio 产品内**无确定性命令通道，也无补上它的路线图** |
| Aily / 钉钉 AI 助理 | 两家 AI 战略核心（Aily 并入豆包、钉钉 DEAP 完整企业 AI 栈） | 无「AI 取代平台确定性命令」表述；确定性通道作为 AI 的入口/执行层被官方主动使用 |
| OpenAI | 更强的模型自主编排（agentic）+ 更强的开发者侧确定性控制（strict/审批/tool_choice）；MCP 成为连接层战略方向（含 Secure MCP Tunnel 连私网/本地） | 不提供面向用户的确定性命令通道；官方 Skills 安全警示要求「受约束的产品体验 + 审批」 |
| Claude Code / Anthropic | Skills（开放标准，全产品线铺开）+ MCP（多厂商治理，Skills over MCP WG）+ A2A（互补，Google 捐 Linux Foundation） | 无「AI 取代确定性命令」表述；确定性=工具底座，用户显式触发被设为副作用护栏 |
| Copilot CLI / GitHub | 全面 AI 化（autopilot、cloud sandbox、ACP server、Plugins/marketplace） | 确定性 CLI 命令与人类审批/沙箱长期保留；**业务确定性命令未纳入路线图** |
| n8n/Zapier/Make/Coze/Dify | AI 化 + MCP 化（5 家全部支持/主推 MCP，AI 通过 MCP 消费确定性能力）；Zapier 甚至退役 NLA/AI Actions 改为 Zapier MCP | 确定性执行层被同步强化，「AI 决策上、确定性执行下」；无「AI 取代确定性」表述 |

**② 是否出现「用 AI 取代确定性命令」的表述**：**没有**。唯一接近替代的是 Copilot Studio（AI 对话取代显式命令交互）与 Agentforce（用户面主推对话式 AI、不提供命令通道）；但二者均未「废除」所在平台的确定性能力（Teams 平台、Slack 平台仍保留），且都在内部保留确定性执行层（Copilot Studio standard harness 的固定 topics/workflows、Agentforce 的 Apex/Flow/Agent Script）。

**③ 「本地执行 / 本地 agent」路线**：
- **开发侧「回调/事件直达本地」被主流官方背书**：飞书 WebSocket（本地开发可收）、钉钉 Stream（官方「五零」，本地可收）、企微智能机器人 WebSocket（内网/无公网 IP）、Telegram 长轮询 + Local Bot API Server、Slack Socket Mode + ngrok、Teams Dev Tunnels（**官方明示开发/adhoc 用、非生产**）。
- **终端用户侧的「本机 agent / 本机执行」产品**：除微软 Scout（本地桌面 AI agent，AI 在用户本地自主工作——与本方案方向相反）外，主流厂商均未将「用户个人本机执行」作为面向终端用户的主推产品（各报告多标注「未能核实官方路线图」）。
- **AI 侧的「本地」是另一回事**：OpenAI Secure MCP Tunnel、Claude Code MCP stdio、Copilot CLI `!`/`$` 透传、Discord 生态 Claude Code Channels，都是「本地 agent 主动外连」或「LLM 调用本地工具」，与本方案「用户在 IM 显式触发 → 本地 HTTPServer」方向不同。
- **未能核实项汇总**：绝大多数厂商（Slack/Discord/Telegram/飞书/钉钉/企微/Teams/Anthropic/GitHub/两家 AI 助理）均未获取到官方公开 roadmap 页面，「战略倾斜」判断基于官方文档/产品页现状的事实推断；钉钉「快捷指令」开发文档正文为空、MCP 广场文档 404；Copilot Studio 与 Teams 确定性入口集成未核实；Salesforce help 深层条目 JS 渲染、Agentforce Developer Guide 403；ngrok/frp 内网穿透官方文档多未直接提及。

**④ 综合趋势判断**：AI 化 + MCP 化是确定的产业主线，但「确定性执行」没有被 AI 消灭——它被重新定位为 **AI 生态的底层资产 / 执行层 / 护栏**。头部厂商不把「显式命令触发」作为终端用户主入口（Copilot Studio、Agentforce 是极端样本），但全部保留确定性通道并持续投资（Teams 2026 GA、Telegram 2026 增强、飞书写进 Agent 最佳实践）。

---

## 四、趋势判断（回应调研目标 G1~G5）

### G1 · 验证趋势判断：确定性命令触发是否是业界主流？

**是主流基础设施，但不是主推增长点。** 「`/` 唤起命令 → 参数 → 触发服务端」的确定性通道在全部主流 IM（Slack/Discord/Telegram/飞书/钉钉/Teams）中都是**长期存在的成熟一等能力**，且多数仍在持续迭代（Teams 2026 GA Agent Slash Commands、Telegram Bot API 10.x 增强、飞书 Slash Command 仍是较新能力）。但没有任何厂商把它作为**面向终端用户的重点主推方向**——战略焦点已转移到 AI Agent / MCP。结论：**「确定性命令触发」是被主流普遍支持、长期保留的既有主流能力，而非厂商当前主推的增量方向。**

### G2 · 对标主流平台做法：主流 IM 是否提供同类能力、形态如何？

**提供，且形态高度一致**：Slack（`/` 快捷菜单 + Modal/Block 表单 + App Manifest 注册）、Discord（`/` command picker + JSON options schema/autocomplete/modal）、Telegram（`/` 列表 + 键盘 + 无 schema）、飞书（`/` 命令面板 + 卡片表单 + OpenAPI 注册）、钉钉（`/` 快捷指令，但文档最不成熟）、Teams（`/` autocomplete + manifest 强类型参数/Adaptive Card）。共同点：命令清单由**平台/应用服务端下发**（非用户本地上报）、参数多为「自由文本 + 结构化表单组件」两类、触发走 Webhook 或长连接。**例外是企业微信（无标准 `/` 命令菜单）**。差异在参数 schema 的强弱（Discord/Teams 强类型，Telegram/飞书/钉钉弱类型或靠表单）与「直达本地」的通道（长连接 vs 公网 Webhook）。

### G3 · 与 AI Agent Skill 的边界：确定性通道 vs AI 触发的分工

**业界同时存在「非 AI 确定性命令通道」与「AI 触发通道」，二者被普遍定位为互补而非替代，分工明确**：
- **确定性通道**（无 LLM）：用户显式选命令、显式填结构化参数、程序化转发——适合**强约束、结构化、可预期、需审计、需表单**的动作（Slack/Teams/飞书官方均如此表述）。
- **AI 通道**（LLM 决策）：自然语言 → 意图理解 → 自动选工具——适合**开放对话、意图模糊、需理解**的任务。
- 边界证据：Teams 官方「slash 命令保留给最可重复的动作」；钉钉「AI 大脑理解、行动系统执行」；Slack Manifest 双通道并列；Anthropic `disable-model-invocation` 把用户显式触发设为 AI 不能抢跑的护栏；OpenAI 官方安全指引要求「受约束的产品体验 + 审批」（反向佐证确定性护栏价值）。

### G4 · 判断未来趋势：过渡性方案 or 未来趋势？替代 / 互补 / 并存？

**并存互补是确定的主流未来，不是过渡性方案。** 没有任何厂商官方宣布用 AI 取代确定性命令；确定性执行反而被 AI 生态作为**底层资产**复用（iPaaS 5 家的确定性工作流 = AI Agent 的工具箱、Dify「AI 在确定性边界内」、Zapier NLA 退役改 MCP 但 Zap 底座仍在）。「AI 取代显式命令」只出现在纯 AI 产品（Copilot Studio、Agentforce）的用户入口层，且它们也都在内部保留确定性执行层。因此：**确定性命令触发与 AI Agent 生态是「并存 + 互补」关系，确定性通道作为「执行面 / 入口层 / 护栏」具有长期价值**；本方案（无 LLM 直达本地）不是会消失的过渡品，也不会成为厂商主推的主角。

### G5 · 对 open-app 的启示

1. 本方案形态（`/` 唤起 → 表单收参 → 直达本地 HTTPServer）**在业界有充分先例支撑**（Slack/Discord/Telegram/飞书/Teams 都是该形态），但**没有任何主流厂商把它作为终端用户主推方向**——这是「被验证但未被重点投入」的空白带，open-app 若做属于差异化增量，而非跟随主推趋势。
2. 「直达用户本地」的最可行链路已被验证 = **本地主动外连的 WebSocket 长连接 / 长轮询**（飞书、钉钉 Stream、企微、Telegram 先例），公网 Webhook 需要公网入口，不适合纯本地场景。
3. 与 AI 的关系：宜定位为**确定性执行面 / 入口层**，与 AI Agent（作为决策层）互补共存，甚至可被 AI 复用为执行通道（飞书把 Slash 写进 Agent 最佳实践、MCP Prompts 命令就是先例）。
4. 各家官方 roadmap 大多未能核实，「AI 倾斜」判断基于现状事实，open-app 决策时可留出跟进空间。

---

## 五、对 open-app 的结论建议

### ① 是否值得投入

**值得投入，但定位要校准：不是「追 AI 主推方向」，而是「补 AI 时代的确定性执行空档」。** 依据：
- 确定性命令通道在主流 IM 中**普遍存在、长期保留、仍在迭代**（非过渡品、非被淘汰方向）；
- 但厂商战略重心都在 AI，**没有人把「无 LLM 直达本地 HTTPServer」做成面向终端用户的主推产品**——这正是 open-app 的差异化空档（iPaaS 5 家报告明确：命令前端层是空白，它们只提供执行端点）；
- 越激进的 AI 厂商越在内部保留确定性执行层（Agentforce Agent Script、Anthropic disable-model-invocation、Aily app-skill/start、Copilot Studio 固定 workflows），印证「确定性执行」是企业级刚需。
- 建议结论：**投入方向成立，建议以「确定性执行面 + 入口层」身份投入，与 AI 通道并列而非对立。**

### ② 若投入：建议的产品形态与关键设计要点

- **命令注册/下发**：采用「服务端 CommandServer 维护命令清单 + 参数 schema，客户端 `/` 唤起下发渲染」的成熟模式（对标 Slack App Manifest / 飞书 OpenAPI / Discord HTTP API 注册）；建议命令清单支持强类型参数 schema（对标 Discord/Teams），弥补 Telegram/飞书/钉钉弱类型的短板。
- **直达本地链路（关键）**：采用**本地侧主动外连的 WebSocket 长连接 / 长轮询**（飞书/钉钉 Stream/企微/Telegram 先例），本地 CLI 注册后与 CommandServer 建立反向长连接，**不依赖公网 Webhook / 内网穿透**；安全上用连接建立时的凭证鉴权（对标飞书 APP_ID/APP_SECRET、钉钉 Stream clientId/clientSecret）。
- **安全与鉴权**：参考主流「签名校验 + 时间戳防重放 + 加密」（Slack HMAC-SHA256、Discord ed25519、飞书/钉钉 AES + 签名）；长连接模式可用连接级凭证简化。
- **发现与生态**：`/` 唤起 + 命令面板 + 描述/图标（对标 Slack/飞书/Teams）；暂不需要命令市场（主流 IM 也都没有针对本地命令的市场，钉钉快捷指令/飞书命令均按应用隔离）。

### ③ 与 AI 的差异化切入点（AI 产品覆盖不到的空档）

1. **「无 LLM 的确定性执行」本身**：AI 产品（Copilot Studio、Agentforce、Aily/DEAP、OpenAI、Claude Code、Copilot CLI）的通用形态是「用户选命令/对话 → LLM 执行」；本方案「用户显式选命令 + 结构化参数 → 程序化转发」在它们体系中**没有对应物**（多个报告明示这是空档），适合**零成本、可预期、可审计、无幻觉**的强约束场景。
2. **用户显式触发作为「副作用操作护栏」**：Anthropic `disable-model-invocation`（AI 不得抢跑 `/deploy` 类命令）、OpenAI「受约束产品体验 + 审批」、Teams「slash 留给最可重复动作」——确定性命令天然充当 AI 生态的**人工确认 / 审批层**，可设计为「AI 决策后仍需用户在 IM 显式点选命令确认执行」的混合流程。
3. **「命令/执行端点」与 AI 双向打通**：学 Slack MCP 双向、n8n/Make「确定性工作流=AI 的工具箱」——把 CommandServer 的确定性命令同时暴露为 **MCP 工具**（供 AI 调用），使 open-app 的确定性命令成为 AI Agent 生态可消费的执行端点（飞书 Slash 归入 Agent 最佳实践、MCP Prompts 命令即此先例），形成「确定性命令 = AI 的执行层」的定位。
4. **避免正面竞争**：不要与 Copilot Studio / Agentforce 争「智能触发」，不要声称取代 AI；差异化叙事应为「**确定地传**（无幻觉、可审计、结构化的命令直达本地）」，并主动与 AI 决策层互补。

---

*（本汇总基于 14 份单产品报告；各产品详细信源与「未能核实」清单见 `reports/` 对应文件。）*
