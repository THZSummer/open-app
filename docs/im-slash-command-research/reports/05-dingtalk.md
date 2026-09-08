# 钉钉（钉钉开放平台 / DingTalk）竞品调研报告

> 调研主题：IM 内 `/` 唤起命令 → 选中命令 → 输入参数 → 触发用户本地 HTTPServer 的「确定性命令触发」（全程无 AI/LLM 参与）
> 调研日期：2026-09-08
> 信源：全部引用钉钉开放平台官方文档（open.dingtalk.com；正文经官方内容源 icms-document.oss-cn-beijing.aliyuncs.com 逐一核实），未能核实内容已明确标注

---

## 【产品描述】

- **归属**：阿里巴巴集团（钉钉开放平台，DingTalk Open Platform）。
- **形态**：企业级协同办公 IM + 开放平台。开放能力包括：服务端 API、机器人（企业内部机器人 / 第三方机器人 / 自定义 Webhook 机器人 / 群模板机器人）、酷应用（Cool App）、互动卡片、消息菜单、快捷指令、事件订阅（HTTP 回调 / Stream 模式）、AI PaaS / 企业 AI 平台（DEAP）、MCP 等。
- **定位**：企业数字化协同平台——IM 是底座，机器人 / 酷应用 / 卡片负责「把业务功能塞进聊天」，近年战略重心向「AI 助理 / 智能体 / MCP」明显倾斜。
- **目标用户**：企业组织（全员）、企业开发者 / ISV（通过开放平台构建应用）、企业 IT 与数字化管理者。
- **同类型分类**：属于「同类型 IM」——钉钉是**提供确定性命令（快捷指令 `/`）、卡片、机器人触发通道的 IM 平台**，与本方案形态接近。但注意：其「`/`」命令能力（快捷指令）官方文档成熟度较低，主打的确定性交互入口实际是**机器人 @ 对话 + 互动卡片 + 酷应用快捷入口**（详见正文）。

---

## ① 非 AI 本地 Command 支持（对应维度 A~D）

**结论先行：钉钉提供多条成熟、纯确定性的触发通道（机器人 @、互动卡片、快捷指令、消息菜单、事件订阅），全程无 LLM 参与；其中「机器人 @ 对话」+「互动卡片回调」是最成熟范本，「/」快捷指令文档最不完整。**

### A. 命令入口与唤起

钉钉的「命令入口」是**多形态并存**的，并非只有 `/`：

| 入口 | 交互形态 | 是否 `/` 唤起 |
|------|---------|:---:|
| **快捷指令（Quick Commands）** | 在输入框（聊天输入框、搜索输入框）内输入 `/` 唤起**指令菜单**，再输入或点击特定指令完成操作——与本方案形态**最接近** | ✅ |
| **机器人 @ 对话** | 群/单聊内 `@机器人` + 文本，机器人接收文本消息回调（开发者自解析文本） | ❌ |
| **互动卡片** | 消息卡片内按钮 / 交互组件，点击触发回调 | ❌ |
| **消息菜单** | **长按消息**唤起菜单（非 slash），选择快捷操作项跳转 H5/dingtalk:// | ❌ |
| **酷应用快捷入口（会话快捷栏）** | 群内会话快捷栏 / 单聊应用栏常驻图标入口，点击打开半浮层/侧边栏 | ❌ |

- **快捷指令（`/` 入口）官方定位**：酷应用的一种扩展类型，官方原话——「快捷指令是一种高级功能，通过在输入框（如聊天输入框、搜索输入框）内输入“/”来唤起指令菜单，然后输入或点击特定指令来完成特定操作……为用户创造了一种全新的交互方式」。
- **命令清单来源**：由**应用 / 酷应用注册后服务端下发**（快捷指令来自已安装的酷应用配置），非纯客户端本地生成；机器人命令则表现为「@机器人 + 自定义文本」，无统一命令清单。

> 信源：
> - https://open.dingtalk.com/document/orgapp/coolapp-overview（酷应用类型，含「扩展到快捷指令」定义）
> - https://open.dingtalk.com/document/orgapp/shortcut-commands（「开发快捷指令酷应用」——**标题存在、正文为空**，详见「未能核实」②）
> - https://open.dingtalk.com/document/orgapp/message-menu-overview（消息菜单：长按消息唤起）
> - https://open.dingtalk.com/document/orgapp/group-chat-coolapp-overview（会话快捷栏 / 快捷入口）

### B. 参数输入

- **快捷指令**：官方仅说明「输入或点击特定指令」，**参数如何声明 / 是否有结构化 schema / 触发后回调细节，官方文档正文为空，未能核实**（见「未能核实」②）。
- **机器人 @ 对话（文本参数）**：钉钉把用户 `@机器人` 后的整段消息以 `text.content` 原样推给服务端，**参数解析完全由开发者自己的代码完成**（与 Slack 的 `text` 单一文本参数同构）——参数「声明」即为开发者的自定义解析协议。
- **互动卡片 / 酷应用表单（结构化参数）**：互动卡片支持按钮、输入框等**可交互组件**，用户点击卡片按钮 → **HTTP 回调**给开发者服务 → 开发者返回最新卡片数据完成交互；酷应用快捷入口可打开**半浮层 / 侧边栏页面**（移动端半浮层、PC 端侧边栏）承载表单。这是钉钉真正「结构化收参」的形态。
- 消息菜单的快捷操作项：仅配置「操作名称 + 桌面端/移动端访问地址（https:// 或 dingtalk://）」，跳转式，无强类型参数声明。

> 信源：
> - https://open.dingtalk.com/document/orgapp/robot-receive-message（`text.content` 文本回调）
> - https://open.dingtalk.com/document/orgapp/group-chat-coolapp-interactive-card（卡片按钮 → HTTP 回调 → 动态更新）
> - https://open.dingtalk.com/document/orgapp/develop-interactive-cards（互动卡片：模板 + 公有/私有数据）
> - https://open.dingtalk.com/document/orgapp/configuration-group-chat-quick-entry（快捷入口配置：图标 + 名称 + 桌面/移动访问地址）
> - https://open.dingtalk.com/document/orgapp/access-message-menu-coolapp（消息菜单快捷操作配置）

### C. 命令注册与下发

| 能力 | 维护方 | 注册方式 | 发现机制 |
|------|-------|---------|---------|
| 快捷指令 | 应用/酷应用开发者 | 酷应用扩展中配置「扩展到快捷指令」，随应用发布 | 用户输入 `/` 弹出指令菜单（正文未核实） |
| 机器人 | 应用开发者（企业内部机器人 / 第三方机器人） | 开发者后台创建应用 + 添加机器人能力 + 发布 + **添加机器人入群** | 用户 @机器人；或会话快捷栏图标 |
| 消息菜单 | 酷应用开发者 | 酷应用扩展中配置「扩展到消息菜单」+ 快捷操作项 | 长按消息唤起菜单 |
| 酷应用快捷入口 | 酷应用开发者 | 功能设计 → 群快捷入口/单聊入口，配置地址 | 会话快捷栏 / 单聊应用栏常驻展示 |

- 关键差异：钉钉的「命令注册」以**应用（企业内部应用 / 酷应用）**为单位，在**开发者后台 / 酷应用配置台**维护，属「应用注册 + 平台配置下发」，**没有「用户本地上报命令清单」的先例**。

> 信源：
> - https://open.dingtalk.com/document/orgapp/robot-overview（机器人类型与能力矩阵）
> - https://open.dingtalk.com/document/orgapp/add-robot-to-group（添加机器人入群流程）
> - https://open.dingtalk.com/document/orgapp/coolapp-overview、https://open.dingtalk.com/document/orgapp/configuration-group-chat-quick-entry
> - https://open.dingtalk.com/document/orgapp/access-message-menu-coolapp

### D. 触发到本地的链路

钉钉把「平台 → 应用服务端」的推送分为 **HTTP 模式** 与 **Stream 模式** 两条链路，且官方**明确推荐 Stream 模式**：

| 链路 | 机制 | 对公网的要求 | 安全鉴权 |
|------|------|------------|---------|
| **HTTP 模式** | 配置「消息接收地址 / 事件订阅 URL」，钉钉以 `HTTP POST` + JSON 推送 | 需公网 HTTPS 地址 | ①机器人消息接收：开发者后台配置接收地址；②事件订阅：`signature + timestamp + nonce` 签名校验 + `encrypt`（AES 加密，`Base64(AES(random16B+len+msg+key))`），需用 `token + aes_key(43 位) + owner_key(appKey)` 解密并在 **1500ms** 内返回加密的 `success` |
| **Stream 模式（官方推荐）** | 通过 `dingtalk-stream` SDK 与开放平台建立 **WebSocket 长连接（反向连接）**，接收机器人回调 / 事件订阅回调 / 互动卡片回调 | **零公网 IP / 域名 / TLS / 防火墙白名单 / 网关 / 内网穿透**（官方「五零」表述） | 连接建立时以 `clientId(AppKey/SuiteKey) + clientSecret(AppSecret/SuiteSecret)` 鉴权；一个应用默认最多 **50** 条连接 |

- **能否送达「用户本地」——官方有明确的本地开发范式，且路径与 Slack 不同**：
  1. **内网穿透工具（曾提供，已废弃）**：钉钉曾提供 ngrok 式工具（`dingtalk-pierced-client`，映射 `*.vaiwan.cn` 到 `127.0.0.1`），但官方明示「因安全合规、服务资源和维护成本等原因，**钉钉于 2022 年 7 月 21 日起不再提供内网穿透工具服务**」。
  2. **Stream 模式（接替者，官方主推）**：官方原话「开发者无需在本地搭建内网穿透工具，**通过 Stream 模式在本地开发环境中即可接收卡片回调**」——这是钉钉官方对「命令/回调直达本地服务」的最强背书。
- 补充：机器人回复/发送消息走 `sessionWebhook`（会话级临时 Webhook）或服务端 API，均为确定性推送。

> 信源：
> - https://open.dingtalk.com/document/resourcedownload/introduction-to-stream-mode（Stream 模式原理 / 「五零」/ 50 连接限制 / 本地接收卡片回调）
> - https://open.dingtalk.com/document/resourcedownload/http-intranet-penetration（内网穿透工具已废弃，转 Stream 模式）
> - https://open.dingtalk.com/document/org/configure-event-org-subcription（事件订阅 HTTP 回调：signature/timestamp/nonce + AES 加解密 + 1500ms + DingCallbackCrypto）
> - https://open.dingtalk.com/document/orgapp/robot-receive-message（机器人接收消息：Stream 模式 topic `/v1.0/im/bot/messages/get` + HTTP 模式）
> - https://open.dingtalk.com/document/orgapp/robot-reply-and-send-messages（sessionWebhook 回复）

---

## ② AI Agent Skill 支持（对应维度 E~F）

### E. 确定性 vs AI

- **确定性为主**：钉钉的机器人 @、互动卡片回调、消息菜单、快捷指令、事件订阅，全部是**纯程序化、无 LLM** 的确定通道——平台不「理解」命令语义，只做「接收文本 → POST/WebSocket 推给开发者服务」，参数解析由开发者代码完成。
- **AI 是叠加层**：钉钉把 AI 能力做成独立的「AI 助理 / 智能体」产品，底层复用钉钉的确定性能力（发消息、卡片、回调、插件）：
  - **钉钉 AI PaaS**：模型调度平台（感知输入、调度技能）、模型训练平台（专属大模型）、**插件开发平台**（「企业可以把自有系统的服务注册成插件，从而被调度平台调用，执行具体任务」——即 AI 决策后调用确定性服务，是「AI 调工具」而非「确定性命令」），最终生成「魔法棒应用和 AI 助理」。
  - **钉钉企业 AI 平台（DEAP）**：智能体管理、**MCP 管理（内置 MCP / 三方 MCP / 自定义 MCP）**、知识管理、模型管理（33 款模型）、安全与权限；智能体「全域感知钉钉产品能力，触发 Agent 执行」。
- 判断：钉钉同时存在**纯确定通道（成熟、主推）** 与 **AI 通道（新增、重投入）**，AI 通道**叠加**在确定性基础设施之上，未取代确定性通道。

> 信源：
> - https://open.dingtalk.com/document/aipass/introduction-to-dingtalk-ai-paas-1（模型调度 / 训练 / 插件平台，插件=企业自有服务被 AI 调度）
> - https://open.dingtalk.com/document/aipass/ai-assistant-overview（DEAP：MCP 管理 / 智能体 / 知识 / 模型 / 安全）
> - https://open.dingtalk.com/document/orgapp/robot-receive-message（确定性机器人回调）
> - https://open.dingtalk.com/document/orgapp/group-chat-coolapp-interactive-card（确定性卡片回调）

### F. 与 Agent Skill 的关系：并存（双通道）

- **双通道明确并存**，官方表述的分工是：AI「大脑理解完后交给行动系统去执行」（AI PaaS 原话），即 **AI 助理是决策层、确定性机器人/卡片/插件是执行层**，二者互补而非替代。
- **MCP 是 AI 通道的标准接入协议**：DEAP 提供「内置 MCP、三方 MCP、自定义 MCP」三类，官方简介称「通过标准协议连接 AI 模型与企业数据，让钉钉 AI 助理调用私有数据构建智能应用」；官方导航列出「19 个官方技能 + 13 个三方技能（持续上新中）」，企业可自定义创建 MCP 技能。
- 机器人对话即 AI 助理的落地载体之一：开发者可将「@机器人」消息接入大模型做智能问答（确定性接收文本 → AI 处理 → 确定性回消息），印证「确定性通道承载 AI」的叠加关系。

> 信源：
> - https://open.dingtalk.com/document/aipass/introduction-to-dingtalk-ai-paas-1
> - https://open.dingtalk.com/document/aipass/ai-assistant-overview（MCP 三类管理 / 技能数量 / 本地化部署表述）
> - https://open.dingtalk.com/document/aipass/mcp-square-introduction（标题与简介来自官方导航；**正文 404，见「未能核实」③**）

---

## ③ 未来规划（对应维度 G）

### 对「本地执行 / 本地 agent」的投入

- **本地接收的工程路线是明确的**：钉钉把「回调送达本地」的官方主推方案定为 **Stream 模式**（WebSocket 反向连接，官方明确「本地开发环境即可接收卡片回调」），并**正式废弃**了自研内网穿透工具（2022-07-21 停服）——这是官方对「开发阶段本地服务接收」的最强投入信号；但对**终端用户的「本地 agent / 本机执行」产品形态，官方公开文档中未见明确路线图表述**（未能核实）。
- **AI 数据本地化**：DEAP 官方提及「支持 AI 数据本地化部署，满足高合规要求」，指向企业私有化/本地化部署 AI 能力，但属部署形态，非「用户个人本机 agent」。

### 对「AI Agent Skill / MCP」路线的投入（信号强烈）

- 钉钉的战略重心**明显向 AI 助理 / 智能体 / MCP 倾斜**：AI PaaS + DEAP（智能体管理、MCP 管理、知识管理、模型管理、运营中心、安全与权限）+ MCP 广场，构成完整的「模型—数据—技能—应用」企业 AI 平台；官方宣称智能体支持单聊、群聊、网页版等「全域投放」，并「触发 Agent 执行」。
- 确定性通道（机器人 / 互动卡片 / 快捷指令 / 消息菜单 / 酷应用）**仍在一等公民维护**，官方文档持续更新（如酷应用、互动卡片、事件订阅），**没有任何「确定性命令将被 AI 取代」的官方表述**。

### 需要标注的「未能核实」项

1. **钉钉官方公开 roadmap / 路线图页面**：本次未获取到官方路线图文档，「AI 倾斜」判断基于官方文档与产品页现状，属可核实的事实推断，非官方远期时间表承诺。
2. **「快捷指令」详细开发文档**：`orgapp/shortcut-commands`（标题「开发快捷指令酷应用」）在官方内容源中**正文为空**，因此快捷指令的**参数声明 schema、指令菜单的清单下发机制、触发后的回调细节均未能核实**——只能确认该能力存在且被官方定义为「输入 `/` 唤起指令菜单」。
3. **MCP 广场详细文档**：`https://open.dingtalk.com/document/aipass/mcp-square-introduction` 在官方内容源返回 **404/NoSuchKey**，仅能在官方导航中核实标题与简介（「通过标准协议连接 AI 模型与企业数据，让钉钉 AI 助理调用私有数据构建智能应用」），详情未能核实。
4. **自定义 Webhook 群机器人的安全设置细节**（加签 / 关键字 / IP 白名单）：本次未抓到该专门文档正文，仅确认 Webhook 机器人发送链路（GitLab / GitHub / JIRA 等推送到群）。

---

## 小结（对「确定性命令触发」方向的判断）

- 钉钉提供**成熟、纯确定性的触发通道**（机器人 @ 对话、互动卡片回调、事件订阅），全程无 LLM，链路与安全（签名 + AES 加解密 + 1500ms 回调）均有官方完整文档，其中**Stream 模式（WebSocket 反向连接）是官方主推的「直达本地」路径**——这是钉钉对「命令/回调送到用户本地服务」的明确工程背书。
- 钉钉**确实有 `/` 快捷指令**（酷应用扩展，输入 `/` 唤起指令菜单），形态与本方案最接近；但其**开发文档正文为空**，参数 schema / 触发回调等细节未能核实，成熟度远低于机器人/卡片通道。
- **AI 是叠加层而非替换**：AI PaaS / DEAP / MCP 广场构成钉钉的 AI 战略，但官方表述为「AI 决策、确定性服务执行」，确定性通道与 AI 通道**双通道并存、互补**。
- 对本方案启示：钉钉证明「确定性命令触发 + 直达本地（Stream）」是**被主流厂商认可并保留**的能力；但其战略重心已明显转向 AI 助理 / 智能体 / MCP，且其「/」命令通道自身文档尚不成熟，确定性命令作为「执行面」与 AI 互补共存。
