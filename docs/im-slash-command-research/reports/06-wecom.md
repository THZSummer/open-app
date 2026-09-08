# 06 · 企业微信（WeCom / 企业微信开放平台）竞品调研报告

> **调研主题**：IM 内 Slash 命令唤起列表 → 选中命令 → 输入参数 → 直达用户本地 HTTPServer 的「确定性命令触发」模式，是否业界主流 / 主推 / 未来趋势
> **调研日期**：2026-09-08
> **信源**：全部引用企业微信开发者中心官方文档（developer.work.weixin.qq.com）与官方帮助中心（open.work.weixin.qq.com），在线核实；未能核实项已明确标注

---

## 【产品描述】

- **归属**：腾讯（Tencent Inc.），企业微信 WeCom + 企业微信开放平台（腾讯旗下企业级产品）。
- **形态**：企业级 IM + 开放平台。开放形态包括——**自建应用**（服务端 API + 客户端 API + 小程序 + JS-SDK）、**智能机器人**（知识库问答 / API 模式）、**消息推送（原「群机器人」）**（webhook 单向推送）、**数据与智能专区**（会话存档 + AI 模型专区）等。
- **定位**：连接「企业内部员工、企业与微信生态客户」的沟通协同平台 + 企业 IT 数字化底座。官方帮助中心将「智能机器人 / 智能摘要 / 数据与智能专区」归类于「智能办公（Intelligent Office）」栏目。
- **目标用户**：企业员工与管理者、企业开发者（自建应用）、第三方服务商、对接微信客户的企业。
- **同类型 IM 备注**：✅ **同类型竞品，但命令能力有限** —— 企业微信是提供「应用消息 + 回调 + 智能机器人」的 IM 开放平台，与「IM 内命令 → 参数 → 触发服务端」形态**部分同构**；但**官方文档体系中没有标准的 `/` slash 命令菜单**（详见 ①-A），其命令等价形态是「自定义菜单 click 事件 + 文本消息关键词」，故归为「同类型 but 能力有限」，兼作「对照」样本。

---

## ① 非 AI 本地 Command 支持（维度 A~D）

> **结论先行**：企业微信**没有标准 `/` slash 命令列表**。其确定性输入通道为「自建应用文本消息（关键词匹配）+ 自定义菜单按钮（click 事件 key）」，回调 URL 推送原始文本/事件到开发者服务端，**全程无 LLM 参与**；智能机器人 API 模式则提供可选的 WebSocket 长连接通道（官方支持内网/无公网 IP 部署，即「送达用户本地/内网」的官方路径）。

### A. 命令入口与唤起形态

- **无标准 `/` 命令菜单（据实标注）**：对官方开发者文档体系检索，**未发现 slash / 斜杠命令 / 命令列表**等概念；接收消息格式中文本消息仅含 `Content` 字段（原样文本），无命令标识、无参数结构字段（见 90239）。官方对「命令式交互」的表述是回调配置文档中的两句话：*「用户向应用发消息时，识别消息关键词，回复不同的消息内容」*、*「用户点击应用菜单时，转化为指令，执行自动化任务」*（见 90930）。
- **替代的确定性入口（三种）**：
  1. **应用会话文本消息**：用户在应用聊天窗口输入文本 → 以加密 XML 推送整段文本给回调 URL → 由应用自行解析关键词（自由文本，无下拉/联想）。
  2. **自定义菜单按钮**：应用在会话内配置菜单（一级 1~3 个、二级每级 1~5 个），按钮类型 `click`（推事件带开发者自定义 `key`）、`view`（跳 URL）、`scancode_push`、`pic_sysphoto`、`location_select` 等；点击后以事件推送 `key` 给回调 URL，开发者按 `key` 确定性交互（见 90231）。
  3. **智能机器人 API 模式**：用户在群聊 `@机器人` 或单聊发消息 → 推送 JSON（含 `aibotid` / `from.userid` / `msgtype` / `text.content` 等）给机器人回调，可被动/流式回复（见 101039、100719）。
- **命令清单归属**：无「命令清单下发」机制；菜单清单由**应用/管理员在企业后台配置后服务端下发、客户端渲染**（`menu/create` 接口 + 管理后台配置），非客户端本地生成。

> 信源：https://developer.work.weixin.qq.com/document/path/90930 、https://developer.work.weixin.qq.com/document/path/90238 、https://developer.work.weixin.qq.com/document/path/90239 、https://developer.work.weixin.qq.com/document/path/90231 、https://developer.work.weixin.qq.com/document/path/101039

### B. 参数输入形态

| 输入方式 | 说明 | 结构化程度 |
|---|---|---|
| 文本消息（Content） | 用户输入整段文本，回调原样推送，应用自行解析参数 | 无 schema（纯文本约定） |
| 自定义菜单 click key | 按钮带开发者自定义 `key` 值，点击推送事件（EventKey） | 结构化（key 枚举） |
| 模板卡片消息（template_card） | 卡片含 `jump_list` / `card_action` / `horizontal_content_list` 等；`horizontal_content_list.type` 支持 url / 文件附件 / 成员详情三类跳转值 | 半结构化（主要为展示 + 跳转，非表单回传） |
| 智能机器人模板卡片交互 | 用户点击卡片按钮 → 事件回调 → 开发者更新卡片（`aibot_respond_update_msg`） | 半结构化（按钮回调事件） |
| 小程序 / H5 表单 | 应用打开小程序页面收集表单（JS-SDK），不在消息会话内 | 最结构化（但脱离消息链路） |

- **命令如何声明参数**：**无参数 schema / manifest**。参数传递靠「文本约定 + 按钮 key + 卡片字段」，由应用在服务端自行声明与解析。企业微信官方消息与事件结构里**没有任何「命令参数定义」字段**（对比 Slack `input_parameters` / Modal JSON 等，企业微信没有等价物）。

> 信源：https://developer.work.weixin.qq.com/document/path/90239 、https://developer.work.weixin.qq.com/document/path/90231 、https://developer.work.weixin.qq.com/document/path/91770 （群机器人消息类型含 template_card，见其卡片字段）、https://developer.work.weixin.qq.com/document/path/101463 （智能机器人卡片交互）

### C. 命令注册与下发

- **维护方**：**应用/企业管理员**。自建应用在管理后台配置「接收消息回调」与「自定义菜单」；智能机器人在后台配置知识库 / API 模式；**群机器人（消息推送）只有 webhook 单向「发」，不能注册任何命令**（见 91770，其 webhook `?key=xxx` 只能 POST 消息进群，无入站命令通道）。
- **注册方式**：管理后台 UI 配置 + 服务端 API（`menu/create`、`access_token` 等）。
- **命令发现机制**：**无 `/` 列表**。用户发现入口 = 应用会话窗口中的**菜单按钮**、以及「@机器人 问它 / 发消息」；命令可用性由后台配置和可见范围决定。

> 信源：https://developer.work.weixin.qq.com/document/path/91770 、https://developer.work.weixin.qq.com/document/path/90231 、https://developer.work.weixin.qq.com/document/path/90238

### D. 触发到本地的链路机制（关键技术难点）

- **自建应用回调（短连接 Webhook）**：
  - 机制：企业微信向开发者配置的 URL 发 **GET（URL 有效性验证：`msg_signature/timestamp/nonce/echostr`）** + **POST（业务数据：`msg_signature/timestamp/nonce` + 加密 `<Encrypt>` XML）**；5 秒内未响应自动重试 3 次（仅网络类失败）。
  - 鉴权与安全：三件套 **URL + Token（消息体签名校验，防篡改/防重放）+ EncodingAESKey（AES 内容加密）**；CorpID 在 `ToUserName` 中下发，应用 ID 在 `AgentID`；另有 `access_token`（`corpid + secret` 换取）作为调用服务端 API 凭证。官方明确 URL「支持 http 或 https（建议 https）」，即**要求公网可达**。
  - 送达「用户本地」：**无官方直连本地先例**（自建应用回调要求公网 URL）。工程上普遍用**内网穿透**（ngrok / frp 等）把公网回调地址映射到本地机器——**此做法为行业通用实践推断，官方文档未提及任何穿透工具**。
- **智能机器人 API 模式长连接（WebSocket，官方「本地/内网」通道）**：
  - 官方提供**两种互斥的 API 接收方式**：Webhook 短连接（需公网 URL、需加解密）与 **WebSocket 长连接**（`wss://openws.work.weixin.qq.com`）。
  - 官方对比表中明确 WebSocket 长连接的适用场景为：**「无公网 IP：开发者服务部署在内网环境，无法配置公网可访问的回调 URL」**、高实时性、免加解密——即**官方认可的「送达内网/本地部署服务」通道**（见 101463）。
  - 鉴权：**BotID + 长连接专用 Secret**（与回调模式的 Token/EncodingAESKey 不同），连接建立即订阅（`aibot_subscribe`），心跳保活（建议 30s），单机器人同一时间仅一个有效连接（新连接踢旧连接）。
  - 官方提供 Node.js / Python 长连接 SDK（`@wecom/aibot-node-sdk`、`wecom-aibot-python-sdk`）。
- **回复链路**：回调请求响应内**被动回复**（5 秒限制）、`response_url` **主动回复**（智能机器人提供 `response_url` 字段）、以及**流式消息回复**（智能机器人专属，可配合 LLM 流式输出，最长 6 分钟）。

> 信源：https://developer.work.weixin.qq.com/document/path/90930 、https://developer.work.weixin.qq.com/document/path/90238 、https://developer.work.weixin.qq.com/document/path/101463 、https://developer.work.weixin.qq.com/document/path/100719 、https://developer.work.weixin.qq.com/document/path/101039

---

## ② AI Agent Skill 支持（维度 E~F）

> **结论先行**：企业微信的**命令触发链路本身是确定性的（无 LLM 介入）**；但官方**主推的智能机器人产品定位即 AI**（知识库问答 + API 模式 + 流式回复），且 API 模式机器人已开放 **MCP 工具调用（streamable HTTP / JSON Config）与 OpenClaw 等 Agent 集成**——确定性通道与 AI 通道**并存，AI 是官方战略主推的叠加层**。

### E. 确定性 vs AI

- **纯确定性通道客观存在**：自建应用回调（文本 + 菜单 key 事件）全程程序化——企业微信只做「推送原始消息/事件 + 签名鉴权 + 加密」，**不「理解」命令语义，平台侧无 LLM 介入**。官方回调文档对应用行为的定位就是开发者自己「识别关键词 / 按 key 转指令」。
- **智能机器人 = AI 定位产品**：官方帮助中心把「智能机器人 / 智能摘要 / 数据与智能专区」归入「智能办公（Intelligent Office）」；智能机器人原生形态即**知识库问答**（机器人管理下设知识库分组/问答管理）。
- **API 模式机器人：通道确定、AI 由开发者决定**。官方接收消息文档原文：*「开发者回调 url 接收到新消息推送后：可选择生成流式消息回复，并使用用户消息内容调用大模型/AIAgent；也可直接回复模板卡片消息」*（见 100719）。即**同一条回调通道既可纯程序回复（确定性），也可接任意 LLM/AIAgent**。

> 信源：https://developer.work.weixin.qq.com/document/path/100719 、https://developer.work.weixin.qq.com/document/path/101039 、https://open.work.weixin.qq.com/help2/pc/21632 、https://developer.work.weixin.qq.com/document/path/90238

### F. 与 Agent Skill 的关系

- **智能机器人 API 模式提供「机器可调用」的 Agent 通道**，且已上 MCP：
  - 文档能力授权：成员授权机器人「文档 / 智能表格」权限后，机器人可新建、写入文档与智能表格，官方明确**「目前该能力支持以 MCP 方式调用」**，可复制 **streamableHTTP URL 或 JSON Config** 供外部 Agent/MCP 客户端接入（见 101468）。
  - **本地 Agent 集成**：官方文档提及可通过企业微信 OpenClaw 插件以**长连接方式**接入 OpenClaw（帮助中心 doc_id=21658；本次该页无法直接抓取正文，见「未能核实」）。这印证官方将智能机器人作为 **Agent/MCP 生态入口**投入。
  - 机器人侧能力工具：`create_doc`、`edit_doc_content`、`smartsheet_add_sheet / get_sheet / add_fields / update_fields / get_fields / add_records`，均带 JSON Schema `inputSchema`（标准 MCP 工具定义）。
- **关系判断 = 双通道并存 / 互补**：确定性通道（应用消息 + 菜单 + 回调）是**长期基础能力**，未被废弃；AI 通道（智能机器人 + MCP + OpenClaw）作为**叠加/主推新通道**并行存在。官方没有任何「用 AI 取代菜单/关键词命令」的表述，也未把确定性通道当作被替代对象。

> 信源：https://developer.work.weixin.qq.com/document/path/101468 、https://developer.work.weixin.qq.com/document/path/101463 、https://developer.work.weixin.qq.com/document/path/100719

---

## ③ 未来规划（维度 G）

> 官方无统一公开 roadmap 文档（未能核实远期时间表）；以下基于官方开发者文档与帮助中心的**产品结构 + 近期更新（2025~2026 文档最后更新时间）**整理。

### 官方对「AI / 智能机器人」路线的投入（信号明确）

- **产品结构上 AI 已成一级战略**：官方帮助中心专门开设「智能办公（Intelligent Office）」栏目，并列放置 **智能摘要、数据与智能专区、智能机器人、智能搜索**；智能机器人下细分「标准机器人 / API 模式机器人 / 开放 API 能力」。
- **API 模式机器人持续增强（2025-2026 迭代密集）**：流式消息回复、`response_url` 主动回复、**WebSocket 长连接 API 模式（2026-05 更新）**、**MCP 工具暴露（streamable HTTP / JSON Config，2026-03 更新）**、OpenClaw 等 Agent 接入——均属 AI Agent / MCP 生态方向的实质投入。
- **确定性基础设施仍然活跃但非主推**：消息回调、自定义菜单、模板卡片等确定性通道文档仍在维护（回调配置最后更新 2024/12，消息推送 2025/08），但**未见面向开发者的 slash 命令 / 命令清单 / 命令市场等新能力**。

### 官方对「本地执行 / 本地 agent」的投入

- **WebSocket 长连接 = 官方「内网/无公网 IP」通道**：官方明确其为「服务部署在内网环境、无法配置公网回调 URL」场景提供（101463），可视为「本地 / 内网 agent」的官方接入链路；叠加 OpenClaw 集成，企业微信正在为「本地 Agent ↔ 企业会话」铺设官方通道。
- **面向终端用户的「本机执行」产品**：未见于官方公开主推路线，**未能核实**。

### 需要标注的「未能核实」项

1. **无标准 `/` slash 命令**：基于对官方文档体系的检索结论（消息格式无命令字段、无 slash 概念文档），**官方并未明文声明「不支持」**，属事实性推断。
2. **内网穿透工具（ngrok/frp）接回调**：官方文档未提及，为行业通用工程实践推断。
3. **官方公开 roadmap / 路线图文档**：未获取到，未来投入判断基于文档现状与更新日志，非官方远期承诺。
4. **OpenClaw 集成正文（help doc_id=21658）**：本次访问未返回正文，仅能依据 101468 文档中的链接引用确认存在该官方指引。
5. **标准机器人（知识库问答）的配置细节**：本次未深度抓取帮助中心 21703/21704 正文，未作展开。

---

## 小结（对「确定性命令触发」方向的判断）

- **确定性命令通道：存在但形态有限**。企业微信提供的是「文本关键词 + 菜单按钮 key」的确定性回调链路（无 LLM、纯程序化），**但不是 `/` slash 命令列表形态**；相比 Slack / Discord / Telegram 的 slash 菜单，企业微信在「命令清单 / 参数 schema / 命令发现」上缺位，能力等级偏低。
- **AI 叠加层：官方主推**。智能机器人（知识库问答 + API 模式）+ MCP 工具暴露 + OpenClaw 等本地 Agent 集成，说明企业微信将「AI Agent 接入企业会话」作为战略方向，确定性通道与 AI 通道**并存互补**，且 AI 是增量重心。
- **对本方案启示**：企业微信**不是「slash 命令直达本地服务」的范本**（无 slash 菜单），但提供了**两条可借鉴的确定性链路**——① 回调 URL + Token/EncodingAESKey 签名加密（公网要求）；② 智能机器人 WebSocket 长连接（官方支持内网/无公网 IP，最接近「送达用户本地」的官方先例）。若要在企微生态实现「确定性命令触发」，需自行以「文本关键词 + 按钮 key」在回调通道上构建，**平台不会提供 `/` 命令列表基础设施**。

---

## 调研说明

- 本报告全部结论基于**企业微信官方信源**（developer.work.weixin.qq.com 开发者中心 / open.work.weixin.qq.com 帮助中心）在线核实；访问失败的页面已标注「未能核实」，未引用未核实结论。
- 「无标准 slash 命令」与「内网穿透接回调」为事实性检索/工程实践推断，已在文中明确标注非官方明文结论。
- 调研维度覆盖：A 命令入口 / B 参数输入 / C 命令注册下发 / D 本地触发链路 / E 确定性 vs AI / F 与 Agent Skill 关系 / G 未来规划。
