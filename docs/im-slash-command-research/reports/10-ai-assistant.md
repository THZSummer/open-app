# 飞书智能伙伴（Aily）与 钉钉 AI 助理 竞品调研报告

> **调研主题**：IM 内 `/` 唤起命令 → 选中命令 → 输入参数 → 触发用户本地 HTTPServer 的「确定性命令触发」（全程无 AI/LLM 参与）——以「不同类型产品」（AI 驱动的 AI 助理）为对照，验证确定性命令 vs AI 触发是替代、互补还是并存
> **调研日期**：2026-09-08
> **分类**：**不同类型**对照产品（AI 助理，AI 驱动，属「AI Agent Skill」一类，与本方案「无 AI、纯程序化确定性传递」互为对立面）
> **信源说明**：飞书侧引用飞书官网 / 飞书开放平台官方文档（经官方 `.md` 纯文版逐一核实）；钉钉侧引用钉钉开放平台官方文档（正文经官方内容源 `icms-document.oss-cn-beijing.aliyuncs.com/zh-CN/dingtalk/aipass/topics/<slug>.html` 逐一核实）。未能核实内容已明确标注。

---

## 【产品描述】

本报告对照两款国产 AI 助理产品，二者均属「**不同类型**」——核心交互由 LLM 理解自然语言、决策调用工具完成，与本方案（无 AI、纯程序化）互为对立面，用于判断「确定性命令 vs AI 触发」是替代、互补还是并存。

### 飞书智能伙伴（飞书 aily，帮助中心现为「豆包工作伙伴」）

- **归属**：字节跳动（飞书，北京飞书科技有限公司）。
- **形态**：企业级 AI 智能体 / 智能应用开发平台（原「飞书智能伙伴创建平台」，现简称「飞书 aily」；其帮助中心入口标题已显示为「豆包工作伙伴」，说明该平台已并入豆包 AI 生态）。
- **定位**：官方原话——「围绕大语言模型（LLM）提供 AI 技能编排、知识数据处理、效果调优和持续运营能力，让用户高效的开发出专业的企业级智能应用，并一键发布到飞书、Web 等多个渠道」。
- **目标用户**：企业开发者 / ISV、企业业务团队（低代码搭建智能体）、企业 IT 与数字化管理者。
- **不同类型备注**：Aily 是**纯 AI 驱动的智能体平台**，用户侧交互为自然语言对话、工具调用由 LLM 决策；但官方同时提供 **aily/v1 OpenAPI 程序化执行通道**（指定 skill_id + 结构化参数直接触发技能，详见 ① B）。

### 钉钉 AI 助理（DEAP / AI PaaS）

- **归属**：阿里巴巴集团（钉钉开放平台）。
- **形态**：钉钉企业 AI 平台（Dingtalk Enterprise AI Platform，简称 **DEAP**，官方亦称「AI 助理创建平台」）+ 钉钉 AI PaaS（模型调度 / 模型训练 / 插件开发三平台）。
- **定位**：官方原话——「打通『模型-数据-技能-应用』四大核心环节，构建覆盖海量模型管理、业务数据治理、企业技能接入、智能体轻量搭建的端到端一站式 AI 解决方案，助力企业构建效果稳定、逻辑可靠、体验流畅的高质量 AI 智能体」。
- **目标用户**：企业组织（全员使用）、企业开发者 / IT、企业 AI 资产管理者（超管 / 部门管理员 / 模型开发者 / 智能体开发者四种角色）。
- **不同类型备注**：钉钉 AI 助理是**纯 AI 驱动的智能体产品**，用户在单聊 / 群聊 / 网页版与智能体自然语言对话，工作流触发与参数提取由 LLM 决策；其确定性能力（机器人 / 互动卡片 / 快捷指令 / 酷应用）为平台独立通道（另见 05-dingtalk 报告）。

---

## ① 非 AI 本地 Command 支持（对应维度 A~D）

**结论先行：两款产品自身都没有「非 AI 确定性命令」概念（无 `/` 命令清单、无用户侧结构化收参、无「命令直达本地 HTTP Server」链路），默认交互均为「自然语言 → LLM 意图识别 → 决策调工具」。唯一的「确定性」体现在两点：① 飞书 Aily 提供按 `skill_id` + 结构化参数的 OpenAPI 程序化执行通道（绕过 LLM 决策直接触发某个技能）；② 两者的工具执行层（工作流 / MCP / 连接器）本身是确定性程序，但「选哪个工具、填什么参数」由 LLM 决策。**

### A. 命令入口与唤起形态

| 产品 | 是否有 `/` 命令清单 | 用户侧入口形态 | 官方文档依据 |
|------|:---:|------|------|
| **飞书 Aily** | ❌ 无 | 智能体以 Bot 形态发布到飞书 IM / 服务台 / Web，用户**自然语言对话**唤起；无命令面板 | 飞书 aily 官方介绍页（应用场景：智能客服、质检告警、信息检索、业务系统） |
| **钉钉 AI 助理** | ❌ 无 | 智能体「全域投放」到单聊 / 群聊 / 网页版，用户**自然语言对话**唤起；创建智能体即写提示词（人设），无命令清单 | DEAP 概述（全域投放）、智能体开发文档（人设 / 提示词） |

- **飞书侧补充**：Aily 智能体在飞书 IM 内是「会话式 Bot」；飞书平台自身的 Slash Command（`/` 命令面板）是**平台级确定性能力**，与 Aily 是两条独立通道（详见 ② F）。
- **钉钉侧补充**：钉钉 AI 助理对话框中唯一带「确定性」性质的入口是欢迎页的**快捷按钮**——官方原话「快捷按钮即对话框上方的快捷按钮，支持配置链接、**锁定技能**、**预设提示词**」——点击后仍进入 AI 对话管线（预填提示词 / 锁定技能），**不是绕过 LLM 的命令触发**。

> 信源：
> - https://www.feishu.cn/content/3d5z9ttt （飞书 aily 官方介绍页：企业级智能体平台、多渠道发布、应用场景）
> - https://open.dingtalk.com/document/aipass/ai-assistant-overview （DEAP 概述：六大核心模块、全域感知 / 全域投放）
> - https://open.dingtalk.com/document/aipass/automatically-generate-an-ai-assistant-1 （智能体开发：人设 / 提示词 / 欢迎语 / 引导问题 / 快捷按钮「锁定技能、预设提示词」）

### B. 参数输入

| 产品 | 参数输入形态 | 参数 schema / 提取方式 |
|------|------|------|
| **飞书 Aily**（用户侧） | 纯自然语言对话 | 无用户侧参数 schema；由 LLM 从对话抽取并决策 |
| **飞书 Aily**（开发侧 API） | **结构化参数**：`POST /open-apis/aily/v1/apps/:app_id/skills/:skill_id/start`，请求体含 `input`（自定义技能变量 JSON）、`variables`（工作流全局变量 JSON）、可选 `query`（「用户进入对话时输入的消息」）、`files` | 技能的入参是**开发者在 Aily 中声明的技能参数**，API 以结构化 JSON 直传，**不经 LLM 解析**；同步返回技能执行结果 |
| **钉钉 AI 助理**（用户侧） | 纯自然语言对话 | 工作流执行三阶段官方明述：「**意图识别与参数提取**——大模型自动识别意图、匹配对应工作流，并从对话中提取关键参数」——**参数提取是 LLM 做的** |
| **钉钉 AI 助理**（MCP 自定义技能） | MCP 工具声明入参（面向 LLM 的工具描述） | MCP 工具描述「提供给大语言模型用于调用规划」——参数由 LLM 决策填充 |

- 关键差异：**用户侧一律无结构化收参**（对比本方案「选中命令 → 表单收参」）；只有**开发者 API 层**（飞书 Aily）或**工具描述层**（钉钉 MCP）存在结构化参数，且前者可绕过 LLM、后者服务于 LLM。

> 信源：
> - https://open.feishu.cn/document/aily-v1/app-skill/start （Aily Start Skill API：`skill_id` + 结构化 `input`/`variables`/`files`，同步返回执行结果）
> - https://open.feishu.cn/document/aily-v1/aily_session-run/create （Aily Create Run API：`app_id` + 可选 `skill_id` + `skill_input` 结构化 JSON）
> - https://open.dingtalk.com/document/aipass/create-an-ai-assistant-workflow-1 （钉钉工作流：LLM 意图识别与参数提取 → 自动触发 → 结果反馈）
> - https://open.dingtalk.com/document/aipass/manage-ai-assistant-workflow-1 （钉钉自定义 MCP：工具描述供大模型调用规划）

### C. 命令注册与下发

- **飞书 Aily**：无「命令注册」概念；开发者构建的是「智能体 / 技能（Skill）」。技能在 Aily 平台可视化编排（节点：大模型调用 / 逻辑工具 / 企业连接器 / 飞书协同套件），发布到渠道后由 **LLM 运行期决定何时调用哪个技能**。技能清单可经 `GET /aily/v1/app-skill/list` 查询——但这是**面向程序化集成的 API**，不是用户侧命令面板。
- **钉钉 AI 助理**：无「命令注册」概念；开发者在 DEAP 后台创建「智能体」并挂载「知识集 / Skill 技能包（zip）/ MCP 工具 / 工作流」，智能体发布后同样由 **LLM 决定调用**。MCP 技能经 DEAP 技能中心注册（内置 / 三方 / 自定义三类）。
- **共同点**：命令清单 = LLM 可调用的工具集（技能 / MCP / 工作流），由平台侧配置下发；**不存在「用户本地上报命令清单」或「第三方应用向客户端注册 `/` 命令」的机制**。

> 信源：
> - https://open.feishu.cn/document/aily-v1/app-skill/list （Aily app-skill list API：技能清单程序化查询）
> - https://www.feishu.cn/content/3d5z9ttt （Aily：Agent 智能规划执行、调度上百个企业服务连接器）
> - https://open.dingtalk.com/document/aipass/ai-assistant-overview （DEAP：智能体管理 / MCP 管理 / 知识管理）
> - https://open.dingtalk.com/document/aipass/automatically-generate-an-ai-assistant-1 （DEAP：添加 Skill 技能包 / 安装 MCP 工具 / 添加工作流）

### D. 触发到本地的链路

- **两款产品均无「用户显式选命令 → 直达本地 HTTP Server」的终端链路**：
  - **飞书 Aily**：技能托管在飞书云端运行，技能内部可通过「企业服务连接器」调用企业外部服务（官方称可调度上百个连接器），但**触发决策在 LLM**；Aily 没有把命令推送到用户本机的通道（「事件/回调直达本地」是飞书开放平台机器人能力，属 04 报告范畴，与 Aily 无关）。
  - **钉钉 AI 助理**：自定义能力通过 **MCP Server（SSE / Streamable HTTP，官方明确不支持 stdio）** 接入，即企业可将自有/自建 HTTP 服务注册为 MCP 工具被智能体调用；官方要求配置 `HTTP URL`（Endpoint），**文档未提及本地/内网穿透场景**——MCP Server 需公网可达；且调用决策在 LLM。钉钉「Stream 模式（WebSocket 反向连接）直达本地」是确定性机器人通道能力（见 05 报告），不属于 AI 助理产品本身。
- **安全鉴权（AI 侧）**：钉钉自定义 MCP 支持请求头鉴权，平台自动携带 `X-DingTalk-User-Id` / `X-DingTalk-User-Job-Number`（用户身份透传）；飞书 Aily API 以 `tenant_access_token` / `user_access_token` + `X-Aily-BizUserID` 鉴权。
- **对本方案 D 维度的启示**：两者证明「AI 工具调用可以触达企业服务（含 HTTP 服务）」是成熟能力，但**链路形态是「云端 LLM 决策 → 公网 HTTP 调用」，与「用户显式命令 → 直达本地」在触发方与方向上都不相同**。

> 信源：
> - https://open.dingtalk.com/document/aipass/manage-ai-assistant-workflow-1 （自定义 MCP：仅支持 SSE / Streamable HTTP，不支持 Stdio；HTTP URL + 请求头鉴权）
> - https://open.feishu.cn/document/aily-v1/app-skill/start （Aily 技能执行 API：token + X-Aily-BizUserID 鉴权）
> - https://www.feishu.cn/content/3d5z9ttt （Aily：调度企业服务连接器）

---

## ② AI Agent Skill 支持（对应维度 E~F）

### E. 确定性 vs AI：两者均为纯 AI 驱动，工具调用成熟度高

**两款产品都是完全 AI 驱动的**——本方案（纯确定性）与它们互为对立面。AI 工具调用（Function Calling / 插件 / MCP / 工作流）成熟度对比如下：

| 能力 | 飞书 Aily | 钉钉 AI 助理（DEAP） |
|------|------|------|
| **核心构建单元** | 智能体 + **技能（Skill）** + 工作流 | 智能体 + **Skill 技能包（zip）** + **MCP 工具** + 工作流 |
| **LLM 规划决策** | 「Agent 智能规划和执行：利用大模型强大的规划能力，调度上百个企业服务连接器与飞书协同套件完成任务」 | 「意图识别与参数提取：大模型自动识别意图、匹配工作流、提取关键参数」；「全域感知钉钉产品能力，触发 Agent 执行」 |
| **工作流 / 编排** | 可视化编排（工作流白盒化：大模型调用、逻辑、连接器节点）；「单节点效果优化」 | 可视化拖拽编排；四种执行动作：逻辑/工具、AI、钉钉协作（审批/待办/消息）、其他业务（外部服务）；工作流执行引擎为确定性程序，但触发与填参由 LLM |
| **MCP / 工具协议** | 飞书 MCP（远程官方推荐：Docs 先行，Base/日历后续；本地开源 lark-openapi-mcp 全量 OpenAPI）；MCP 智能助理 Bot 教程（LLM + OpenAPI MCP 工具自动建多维表格 / 读文档） | **MCP 是官方核心技能协议**：内置 MCP（**19 个官方技能**：智能问数、签到、DING、日程、通讯录、待办、OA 审批、群聊、邮箱、机器人消息、文档、AI 表格等）+ 三方 MCP（**13 个**：必应搜索、高德、火车票、文生图/文生视频、OpenClaw 本地设备等）+ **自定义 MCP**（SSE/Streamable HTTP，JSON 导入） |
| **知识库** | 知识数据处理（多数据源接入、智能预处理打标切片、调优台、术语库、评测反馈） | 知识管理（知识集 / 评测集；5000 量级文件知识库；切片编辑、解析策略、经验/术语/FAQ 调优） |
| **模型管理** | 围绕豆包生态（帮助中心现为「豆包工作伙伴」）；AI 能力开放 API（多轮对话、图片问答、技能调用、知识创建） | 模型广场汇聚 **33 款模型**（通义、DeepSeek、GLM、混元、豆包、MiniMax 等）+ 炼丹炉训练专属模型 + 对接自有模型；支持模型切换 |
| **确定性程序化执行 API** | ✅ **有**：`app-skill/start`（指定 skill_id + 结构化 input 直调技能）、`aily_session-run/create`（指定 skill_id + skill_input 创建 Run）——**不经 LLM 决策路由** | ❌ 无公开的「指定技能直调」API；技能调用均经由智能体会话由 LLM 决策 |

- **对「机器可调用通道」的结论**：两者都是**国内头部厂商中成熟度最高的 AI 助理工具调用平台之一**（MCP 全协议支持、工作流编排、知识库、多模型）；**所有用户侧触发都经 LLM 决策**。唯一例外是**飞书 Aily 的 `app-skill/start` / `aily_session-run/create` API**——它在 AI 平台内部提供了「按 skill_id + 结构化参数程序化执行」的**确定性旁路**，与本方案「确定性命令触发」在 API 层同构（但 Aily 技能内部仍可含 LLM 节点）。

> 信源：
> - https://www.feishu.cn/content/3d5z9ttt （Aily：AI 技能编排 / Agent 智能规划 / 工作流白盒化 / 企业连接器 / 开放能力）
> - https://open.feishu.cn/document/aily-v1/app-skill/start 、https://open.feishu.cn/document/aily-v1/aily_session-run/create （Aily 确定性执行 API）
> - https://open.feishu.cn/document/uAjLw4CM/ukTMukTMukTM/mcp_integration/develop-mcp-intelligent-assistant-bot （飞书 MCP 智能助理 Bot：LLM + OpenAPI MCP 工具自动调用飞书能力；`/clear` 作为消息文本处理）
> - https://open.dingtalk.com/document/aipass/ai-assistant-overview （DEAP：六大模块 / MCP 三类 / 19 官方 + 13 三方技能 / 33 款模型 / 知识集评测集 / 运营中心 / 安全与权限）
> - https://open.dingtalk.com/document/aipass/prompt-word-prompt-1 （内置 MCP：智能问数、签到、DING、日程、通讯录、待办、OA 审批、群聊、邮箱、机器人消息等）
> - https://open.dingtalk.com/document/aipass/role-setting-in-ai-assistant-1 （三方 MCP：必应搜索、高德、火车票、文生图/文生视频、OpenClaw 本地设备等）
> - https://open.dingtalk.com/document/aipass/manage-ai-assistant-workflow-1 （自定义 MCP：SSE / Streamable HTTP / JSON 导入 / 鉴权 Header）
> - https://open.dingtalk.com/document/aipass/create-an-ai-assistant-workflow-1 （工作流：意图识别与参数提取 / 四种执行动作 / Connector 连接多系统）

### F. 与平台确定性命令的关系：AI 助理与确定性通道并存互补

- **双通道并存**：两者都是「AI 助理（决策层）+ 平台确定性能力（执行层）」的结构，与平台确定性命令**互补而非取代**：
  - **飞书**：Aily（AI 对话）与飞书平台 Slash Command / 消息卡片 / 机器人事件（确定性）是两条独立通道。官方把 **Slash Command 文档归入「Agent 最佳实践」**，明确定位为「让用户快速触发 Bot 服务、让能力更易被发现和使用」——**确定性命令被官方主动用作 AI/Agent 能力的「发现与快速唤起入口」**，而非被 AI 取代；其官方用例含 `/todo`、`/approval`、`/deploy`、`/log` 等。飞书 MCP 方向是「AI → 飞书」，与本方案「用户 → 命令 → 本地服务」互为镜像。
  - **钉钉**：AI PaaS 官方原话「**大脑理解完后交给行动系统去执行**」——AI 助理是决策层，确定性机器人 / 互动卡片 / 插件 / 快捷指令是执行层；DEAP 智能体「全域感知钉钉产品能力，触发 Agent 执行」，且支持 MCP / Response API / H5 Copilot 等多类型企业业务系统集成。钉钉平台确定性通道（机器人 / 卡片 / 快捷指令 / 酷应用）仍在官方一等公民维护（见 05 报告）。
- **是否取代显式命令入口**：**两个 AI 助理产品自身不提供确定性命令旁路给终端用户**（用户侧全是 AI 对话）；但它们**都依赖/并存于平台原有的确定性通道**作为执行层与入口层。即：**AI 助理取代的是「用户记忆/输入命令」这一交互心智，没有取代确定性执行通道本身**。

> 信源：
> - https://open.feishu.cn/document/mcp_open_tools/agent-best-practices/agent-supports-slash-commands （Slash Command 归入 Agent 最佳实践；`/todo` `/approval` `/deploy` `/log` 用例；100 条上限、命令名唯一、客户端缓存）
> - https://open.dingtalk.com/document/aipass/introduction-to-dingtalk-ai-paas-1 （AI PaaS：插件开发平台=行动系统，「大脑理解完后交给行动系统去执行」）
> - https://open.dingtalk.com/document/aipass/ai-assistant-overview （DEAP：全域感知 / MCP、Response API、H5 Copilot 集成 / 全域投放）
> - https://open.dingtalk.com/document/aipass/create-an-ai-assistant-workflow-1 （工作流：调用钉钉原生能力审批/待办或企业自建系统接口）

---

## ③ 未来规划（对应维度 G）

### 两家厂商对「AI 化」路线的投入（信号强烈）

- **飞书 Aily = 飞书 AI 战略核心，且持续品牌升维**：官方介绍页将 Aily 定位为企业级智能体平台，「让大模型在企业场景中更好落地」，应用场景覆盖智能客服 / 智能质检与告警 / 智能信息检索 / 智能业务系统；核心叙事是「Agent 智能规划和执行」+「调度上百个企业服务连接器与飞书协同套件」。**品牌演进信号**：Aily 帮助中心入口现显示「豆包工作伙伴」，飞书官网「热门产品」也直接挂出「豆包工作伙伴」入口——Aily 已并入字节豆包 AI 生态，走「AI 工作平台」战略。
- **飞书 MCP 是明确的官方路线**：飞书官方推出 OpenAPI MCP（远程模式官方推荐），官方表述「当前支持云文档场景，**后续将开放多维表格、日历等更多场景**」；另有官方开源 lark-openapi-mcp（全量 OpenAPI）与 MCP 智能助理 Bot 教程——「AI Agent 通过 MCP 调用飞书」是官方路线图中持续演进的一环。
- **钉钉 AI PaaS + DEAP = 钉钉 AI 战略核心**：官方把 DEAP 定义为「新一代企业级 AI 平台」，围绕「模型-数据-技能-应用」构建端到端体系；AI PaaS（模型调度 / 训练 / 插件三平台）+ DEAP（智能体 / MCP / 知识 / 模型 / 运营 / 安全六大模块）+ 炼丹炉大模型平台构成完整企业 AI 栈。官方宣称智能体「效果稳定、逻辑可靠、体验流畅的高质量（90 分以上）」。
- **MCP 持续上新、本地化部署是官方投入点**：DEAP 内置 MCP「19 个官方技能 + 13 个三方技能（**持续上新中**）」；官方明述「支持 **AI 数据本地化部署**，满足高合规要求」。

### 对「确定性命令 / 本地执行」路线的态度

- **两款产品均无「用 AI 取代平台确定性命令」的表述**：飞书官方文档将确定性 Slash Command 持续维护并列入「Agent 最佳实践」；钉钉确定性通道（机器人 / 互动卡片 / 快捷指令 / 酷应用）仍在官方一等公民维护，没有任何「确定性命令将被 AI 取代」的官方表述。
- **「直达用户本地」不在 AI 助理产品边界内**：飞书「事件/回调直达本地」是开放平台机器人能力（WebSocket 长连接，见 04 报告）；钉钉「Stream 模式直达本地」是确定性机器人能力（见 05 报告）。两款 AI 助理产品自身均未提供「终端用户 → 本机」的执行路线；其 AI 侧工具目标只能是公网可达的 HTTP/MCP 端点。
- **需要标注**：两家官方均未获取到公开 roadmap / 路线图页面，「AI 全面倾斜」判断基于官方产品页、帮助中心与官方文档现状，属可核实的事实推断，非官方远期时间表承诺。

### 综合判断（对调研目标 G 的回应）

1. **AI 助理与确定性命令=并存互补，非替代**：两家头部厂商都在「AI 决策层 + 确定性执行/入口层」上双线投入——AI 助理负责「理解与决策」，确定性命令 / 卡片 / 机器人负责「执行与发现」；官方没有「用 AI 取代 `/` 命令」的表述，飞书甚至把 Slash Command 写进 Agent 最佳实践。
2. **AI 助理自身是「纯自然语言入口」**：用户侧没有 `/` 命令面板、没有表单收参；所有触发与参数提取由 LLM 完成。这印证：**AI 助理接管的是「入口交互」而非「执行通道」**。
3. **对 open-app 的启示**：即使是最激进的 AI 化平台（Aily / DEAP），也在内部保留了「按 skill_id + 结构化参数程序化执行」的确定性执行层（Aily 的 `app-skill/start` API 即证明），且都复用平台确定性通道作为入口/执行面——「确定性命令触发」与「AI 触发」是**互补并存的执行层与入口层关系**，而不是非此即彼。

> 信源：
> - https://www.feishu.cn/content/3d5z9ttt （Aily 官方介绍页：企业级智能体平台 / 应用场景 / Agent 智能规划 / 一键发布到飞书、Web）
> - https://aily.feishu.cn/hc （Aily 帮助中心，标题「欢迎使用『豆包工作伙伴』」——品牌并入豆包生态；JS 渲染，正文未能抓取）
> - https://open.feishu.cn/document/uAjLw4CM/ukTMukTMukTM/mcp_integration/mcp_introduction （飞书 MCP 官方路线：Docs 先行，Base/日历后续）
> - https://open.dingtalk.com/document/aipass/ai-assistant-overview （DEAP 定位 / 六大模块 / 本地化部署 / 持续上新）
> - https://open.dingtalk.com/document/aipass/introduction-to-dingtalk-ai-paas-1 （AI PaaS 三平台：模型调度 / 训练 / 插件开发）
> - https://open.dingtalk.com/document/aipass/switch-the-model-of-the-ai-assistant-1 （炼丹炉大模型平台：对接自有模型 / 切换 AI 助理底模型）

---

## 未能核实 / 未访问项

1. **飞书 Aily 帮助中心正文**（`aily.feishu.cn/hc`）：为 JS 渲染站点，正文无法直接抓取；仅核实到标题「欢迎使用『豆包工作伙伴』」。Aily 技能的详细定义、是否支持「技能直发命令」等产品细节未逐一核实。
2. **飞书官网 AI 产品页**（`www.feishu.cn/product/ai`）：JS 渲染，仅获取到标题「Feishu AI | Enterprise AI Assistant for Real Business Use」，正文未能核实。
3. **两家官方公开 roadmap / 路线图页面**：均未获取到，AI 化趋势判断基于官方文档与产品页现状，属可核实的事实推断。
4. **钉钉 AI 助理的「发布 / 分发到具体渠道」文档细节**：`distribution-of-ai-assistant` slug 在官方内容源实际返回的是「模型列表」，与导航标题（发布 AI 助理）不一致，发布到 IM 的具体形态细节未能按该 slug 核实；发布信息以 DEAP 概述「全域投放（单聊、群聊、网页版）」与智能体开发文档「智能体可在钉钉内和钉钉外使用」为准。
5. **钉钉 AI 助理是否支持 `/` 快捷指令复用**：在 DEAP / AI PaaS 全部官方文档中未检索到相关表述（快捷指令属钉钉酷应用确定性能力，见 05 报告），据此判断为「未提供」，此判断基于「文档无此能力描述」的反向证据，非官方明确否定。

---

## 小结（对「确定性命令触发」方向的判断）

- 飞书 Aily 与钉钉 AI 助理都是**纯 AI 驱动的 AI 助理/智能体平台**：用户侧无 `/` 命令清单、无结构化收参、无「命令直达本地」链路，交互一律为「自然语言 → LLM 意图识别 → 决策调工具」；工具调用成熟度高（MCP 全协议、工作流编排、知识库、多模型）。
- 两款产品与各自平台的确定性命令（飞书 Slash Command、钉钉快捷指令/机器人/卡片）是**双通道并存互补**关系：官方把确定性命令定位为 AI/Agent 能力的「发现入口 + 执行层」，无任何「AI 取代确定性命令」表述。
- 值得注意：飞书 Aily 官方在 AI 平台内部提供了 **`app-skill/start` / `aily_session-run/create` 确定性执行 API**（指定 skill_id + 结构化参数直调技能），证明即使 AI 化平台也保留「程序化指定执行」的确定性旁路，与本方案在 API 层同构。
- 对本方案启示：国内两大头部 AI 助理证明「AI 触发」接管的是**用户交互入口**，而**确定性执行通道仍然必要**（既作为 AI 的执行层，也作为独立的高可控入口）——「确定性命令 vs AI 触发」是**互补并存**，而非替代；本方案定位「无 AI、可预期、零幻觉」的强约束触发有其独立价值空间。
