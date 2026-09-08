# 自动化编排 / iPaaS / Agent 编排平台竞品调研报告（n8n / Zapier / Make / Coze / Dify）

> 调研主题：IM 内 `/` 唤起命令 → 选中命令 → 输入参数 → 触发用户本地 HTTPServer 的「确定性命令触发」（全程无 AI/LLM 参与）
> 调研日期：2026-09-08
> 调研对象：**n8n / Zapier / Make / Coze / Dify**（自动化编排 / iPaaS / Agent 编排平台，5 平台对照）
> 信源：全部来自官方文档——n8n：docs.n8n.io（Webhook 节点 / Slack Trigger / Chat Trigger / AI Agent / MCP servers / 构建与托管 Agent）；Zapier：platform.zapier.com（REST Hook 触发器 / 集成构建 / MCP / SDK / Agents）与 help.zapier.com（Webhooks by Zapier / 构建 Agent）；Make：developers.make.com（Custom Apps 文档 webhooks / Make MCP Server / AI Agents API）；Coze：docs.coze.cn（扣子 3.0 / 扣子编程 / AI 生成式工作流 / 插件-技能-MCP / 部署 API）；Dify：docs.dify.ai（Workflow & Chatflow / Agent 节点 / 工具节点 / Webhook 触发器 / 发布为 MCP Server）
> 说明：本报告为「不同类型」对照产品（自动化编排，非 IM 平台 / AI Agent Skill 类产品）。网络受限或无法核实项已明确标注「未能核实 / 网络受限未访问」，未编造任何事实或 URL。

---

## 【产品描述】

本报告对照调研 5 个自动化编排 / iPaaS / Agent 编排平台。它们都不是 IM 产品，但都具备「确定性工作流节点」+「AI 节点 / Agent 编排」双通道，与我们在 IM 中做「Slash 命令 → 参数 → 本地 HTTP Server」的**确定性命令触发**方向形成直接对照。

| 平台 | 归属 | 形态 | 定位（官方口径） | 目标用户 | 确定性通道 | AI 通道 |
|---|---|---|---|---|---|---|
| **n8n** | 德国 n8n GmbH（fair-code 开源，可自托管） | 工作流自动化引擎（节点画布） | "fair-code licensed workflow automation tool that **combines AI features with business process automation**" | 开发者 / 工程师团队 / 自托管企业 | 数百内置节点、Webhook/定时/手动/表单触发器、Code 节点 | AI Agent 节点（LangChain）、Agents（一等公民）、AI Assistant 建流、MCP |
| **Zapier** | 美国 Zapier Inc.（闭源 SaaS） | iPaaS（Zap = 触发器 + 动作 + 搜索） | 无代码自动化，连接 9000+ 应用 | 非技术业务用户 / 中小企业 / 团队 | Zap（触发器 / 动作 / 搜索）、Webhooks by Zapier（Catch Hook）、Trigger Inbox API | Zapier Agents、AI by Zapier、Chatbots、Zapier MCP（9000+ 应用 → MCP 工具） |
| **Make** | 捷克 Celonis 旗下（闭源 SaaS / 自托管） | 可视化场景（Scenario）编排 | 可视化自动化，模块化集成 | 业务用户 / 自动化专业人员 / Ops | 场景 + 模块（触发器 / 动作 / 搜索 / HTTP / 路由器）、Webhook 即时触发器 | Make AI / AI Agents（open beta，把场景当作工具）、Make MCP Server、Make Skills |
| **Coze（扣子）** | 字节跳动（国内 coze.cn / 海外 coze.com，SaaS） | **AI Agent 平台**（Agent + 工作流 + 插件 + AI 编程） | "面向 Agent 时代的新一代 AI 团队协作平台"，"AI 办公协作与编程平台" | 个人 / 团队 / 企业（含飞书生态） | 工作流（开始/结束/动作/条件/并发节点）、插件、部署为 API | 扣子 Agent（原生智能体）、云端/本地 Agent、技能商店、MCP、多人多 Agent 协作、AI 编程 |
| **Dify** | 中国 LangGenius（开源 LLMOps，可自托管） | **AI 应用开发平台**（Workflow / Chatflow / Agent） | "open-source platform for building AI applications … Create agents, agentic workflows, and chatbots" | AI 应用开发者 / 企业 | Workflow 确定性节点（HTTP/Code/If-Else/迭代）、Webhook/定时/集成触发器 | LLM / Agent / 知识检索节点、Agentic Workflow、工具（含 MCP）、发布为 MCP Server |

- **同类型分类**：**「不同类型」对照产品**。本方案是「IM 内确定性命令触发（无 LLM，纯程序化传递）」，属「用户直接发起的命令层」；这 5 个平台属于「自动化编排 / iPaaS / Agent 编排」，其「确定性工作流与 AI Agent 双通道并存」的结构，用于验证：**确定性 vs AI 是替代、互补还是并存**。

---

## ① 非 AI 本地 Command 支持（对应维度 A~D）

**结论先行：5 个平台全部具备成熟的「确定性触发 / 执行」通道**（Webhook 触发器、定时触发器、手动/测试运行、确定性工作流节点、HTTP/Code 节点）；也都能被 IM 间接触发（Slack/飞书 事件 → Webhook 或专用触发器）。但**没有一个平台把「IM 内 `/` 命令 → 参数表单 → 直达用户本地 HTTP Server」作为一等公民产品形态**——最接近的通用做法是「Webhook Catch Hook 模式」（外部系统 POST 到平台生成的 webhook URL 即触发），参数以 HTTP body/query 传入，属于**结构化参数的程序化传递**，与本方案同构。

### A. 确定性「触发器 / 命令」概念（各平台）

| 平台 | 确定性触发机制 | 手动触发 | IM 触发 |
|---|---|---|---|
| **n8n** | **Webhook 节点**（HTTP 触发器，可作 API 端点并返回结果）、Schedule、Slack/邮件等事件触发器、表单触发、手动执行 | ✅ 手动执行 / 测试 URL | ✅ Slack Trigger 节点（新消息 / @提及 / Reaction 等事件）；或 Slack 等 IM 把事件 POST 到 Webhook 节点 |
| **Zapier** | **触发器**（REST Hook 即时 / Polling 轮询）驱动 Zap；**Webhooks by Zapier「Catch Hook」**：生成 `https://hooks.zapier.com/hooks/catch/{id}/{code}`，外部 POST 即触发（URL 靠「不可猜测」保护，非认证）；Webhook 动作（POST/PUT 外发） | ✅ 测试运行 | ✅ 任意 IM 只要能 HTTP POST 到 Catch Hook 即可触发；另有 Slack 等 9000+ 应用触发器 |
| **Make** | **触发器模块**：轮询触发 / **即时触发（Webhook）** / 定时触发；Webhook 收到数据立即执行场景；Responder 模块回包 | ✅ 运行一次 / 调试 | ✅ Slack / Telegram / Discord 等 IM 模块（新消息触发）或外部 POST 到 Webhook |
| **Coze** | **工作流**：开始节点 → 动作 / 条件 / 并发节点 → 结束节点；可**部署为 API**（OpenAPI：同步 / 流式 / 异步执行），由外部 HTTP 调用触发；插件可含 MCP / 技能 / Panel | ✅ 试运行（画布手动触发） | ✅ Agent 渠道发布到**飞书 / 微信 / 企业微信**等 IM；飞书消息 / 飞书多维表格内置集成 |
| **Dify** | **Workflow**（一次性跑完）的触发器：**Webhook Trigger**（生成 URL，声明 HTTP 方法 / Content-Type / 提取参数）、Schedule Trigger、集成（插件）Trigger；Chatflow 则由用户消息驱动 | ✅ 单节点 / 单步运行 | ⚠️ 无原生 Slack slash command；通过发布为 Web App / API / MCP Server 间接接入；官方 Webhook 文档直接以 "content of a Slack message" 为请求体示例 |

> 信源：
> - n8n Webhook 节点：https://docs.n8n.io/integrations/builtin/core-nodes/n8n-nodes-base.webhook.md
> - n8n Slack Trigger：https://docs.n8n.io/integrations/builtin/trigger-nodes/n8n-nodes-base.slacktrigger.md
> - Zapier REST Hook 触发器：https://docs.zapier.com/integrations/build/hook-trigger.md
> - Zapier Webhooks by Zapier（Catch Hook / silent 模式）：https://help.zapier.com/hc/en-us/articles/8496083355661-How-to-get-started-with-Webhooks-by-Zapier
> - Make Webhooks 与即时触发器：https://developers.make.com/custom-apps-documentation/app-components/webhooks.md ；https://developers.make.com/custom-apps-documentation/app-components/modules/instant-trigger.md
> - Coze AI 生成式工作流（节点 / 试运行）：https://docs.coze.cn/guides_ai_powered_workflow_development.md ；Coze 工作流部署为 API：https://docs.coze.cn/dev_how_to_guides_call_a_deployed_workflow_through_api.md ；Coze Agent 渠道发布（飞书/微信）：https://docs.coze.cn/cozespace_agent_management.md
> - Dify Workflow & Chatflow：https://docs.dify.ai/en/cloud/use-dify/build/workflow-chatflow.md ；Dify Webhook Trigger：https://docs.dify.ai/en/cloud/use-dify/nodes/trigger/webhook-trigger.md

### B. 参数如何输入 / 声明 / Webhook 如何接收结构化参数

- **n8n**：节点参数在节点面板声明（HTTP Method、Path 支持 `/:变量` 路由参数）；Webhook 接收 JSON / form / raw body（`Raw Body` 选项），`Only Run If` 表达式可对 `$json.body` 校验；请求数据以 body/query/headers 结构化进入工作流，用 `{{ }}` 表达式 / UI Mapper 映射给下游节点；Webhook 支持 Basic / Header / JWT 认证 + IP Allowlist。
- **Zapier**：触发器 / 动作的**输入字段在 Platform 里声明**（input fields：类型、动态下拉、必填、`bundle.inputData`），用户在 Zap 编辑器以表单填写；Webhook 接收 JSON / form-encoded / XML（自动递归解析嵌套，可 `X-Recurse-Parse: false` 关闭），嵌套属性 `__` 展平；Catch Hook 触发后自动把 payload 字段带入后续步骤映射。
- **Make**：模块**输入参数**在 Custom App / 模块 UI 声明，参数类型丰富（text / number / select / collection / array / JSON / buffer 等，派生自 JSON 数据类型），用户填表单式 mappable 参数；Webhook（即时触发器）把收到的数据整体作为 bundle 进入场景，后续模块映射字段。
- **Coze**：工作流以**开始节点定义入参**（启动工作流需要的输入信息），画布节点逐个配置输入/输出参数；部署为 API 后，入参经请求体/请求参数传入（支持同步 / 流式 / 异步三种 API 调用形态）；插件参数按插件 schema 声明。
- **Dify**：Webhook Trigger 显式声明 **HTTP Method + Content-Type + 要提取的 Query / Header / Body 参数**（每个参数成为工作流输出变量，可设类型与是否必填；支持类型随 Content-Type 受限：JSON 支持 Object/Array 等结构，form/multipart/text 受限）；Workflow 另有 Start / User Input 节点收集表单入参。

> 信源：
> - n8n Webhook 节点参数 / 认证 / Only Run If：https://docs.n8n.io/integrations/builtin/core-nodes/n8n-nodes-base.webhook.md
> - Zapier Input fields / bundle：https://docs.zapier.com/integrations/build/add-fields.md ；https://docs.zapier.com/integrations/build/bundle.md ；Zapier Webhooks 序列化与嵌套解析：https://help.zapier.com/hc/en-us/articles/8496083355661-How-to-get-started-with-Webhooks-by-Zapier
> - Make 输入参数 / 数据类型：https://developers.make.com/custom-apps-documentation/best-practices/input-parameters.md ；https://developers.make.com/custom-apps-documentation/block-elements/types.md
> - Coze 工作流开始节点入参 / API 调用：https://docs.coze.cn/guides_ai_powered_workflow_development.md ；https://docs.coze.cn/dev_how_to_guides_call_a_deployed_workflow_through_api.md
> - Dify Webhook Trigger 参数提取：https://docs.dify.ai/en/cloud/use-dify/nodes/trigger/webhook-trigger.md

### C. 是否支持「IM / 命令触发工作流」

- **通用模式 = Webhook 回调（Catch Hook）**：5 平台全部支持「外部系统（含 IM）HTTP POST → 触发执行」，参数即请求体——这正是 IM slash command → 本地服务链路的「对端形态」：Slack slash command 回调、飞书自定义机器人 Webhook、企业微信群机器人 Webhook 均可作为触发源。
- **平台侧 IM 专用触发器（不用自己写 webhook 接线）**：
  - **n8n**：Slack Trigger（新消息 / App Mention / Reaction 等事件，事件订阅型，需 Slack 签名校验）；Chat Trigger（聊天界面 / webhook，面向 AI 工作流也可复用为 IM 网关）。
  - **Make**：Slack / Telegram / Discord 等 IM 模块自带「监视新消息」类触发。
  - **Zapier**：Slack 等 IM 应用有触发器 / 动作（9000+ 应用生态覆盖），配合 Webhook 动作可双向打通；Trigger Inbox API 提供「订阅 9000+ 应用实时事件」的确定性收件箱。
  - **Coze**：Agent 原生支持发布到飞书 / 微信 / 企业微信等 IM 渠道（在渠道管理里配置）；飞书消息 / 飞书多维表格为内置集成。
  - **Dify**：官方未提供一等的 IM slash command 通道；以 Web App / API / MCP Server 方式对外发布（Dify Webhook 文档示例把 "content of a Slack message" 作为请求体来源）。
- **结论**：**没有一个平台把「IM 内 `/` 唤起命令列表 → 选中 → 填参」做成一等 UX**（这是 IM 平台 / Claude Code 类产品的形态）；自动化编排平台提供的是「确定性 Webhook 端点 + 结构化参数」，由用户自己在 IM 侧另起一层去调用。**对本方案含义**：我们做的「IM `/` 菜单 → 参数 → 本地 HTTP」恰好是这些平台所缺的「前端命令层」，它们只提供了「执行端点」这一半。

> 信源：
> - n8n Slack Trigger / Chat Trigger：https://docs.n8n.io/integrations/builtin/trigger-nodes/n8n-nodes-base.slacktrigger.md ；https://docs.n8n.io/integrations/builtin/core-nodes/n8n-nodes-langchain.chattrigger.md
> - Zapier Trigger Inbox（订阅 Slack/Gmail 等实时事件）：https://docs.zapier.com/white-label/trigger-inbox/what-is-trigger-inbox-api.md ；Zapier MCP 9000+ 应用：https://docs.zapier.com/mcp/home.md
> - Make 触发器模块机制：https://developers.make.com/custom-apps-documentation/app-components/modules/trigger.md
> - Coze Agent 渠道（飞书/微信）与飞书集成：https://docs.coze.cn/cozespace_agent_management.md ；https://docs.coze.cn/guides_feishu_message_integration.md
> - Dify Webhook 触发器（Slack 消息 body 示例）：https://docs.dify.ai/en/cloud/use-dify/nodes/trigger/webhook-trigger.md

### D. 是否存在「非 AI、纯程序化」的确定性执行路径

**存在，且是各平台的核心底座。** 全部 5 平台的「主执行引擎」都是确定性的：触发器收到事件 → 节点 / 模块按声明顺序确定性地执行 HTTP 调用、代码、分支、循环、数据转换 → 返回结果；默认情况下**不含 LLM**，只有显式加入 LLM / Agent / AI 模块才引入 AI。典型确定性构件：

- **n8n**：Code 节点（JS/Python 确定性执行）、HTTP Request 节点、If/Switch/Loop/Merge/Wait 流程节点、Edit Fields / 数据表、表达式引擎（可纯用表达式而不用 AI）。
- **Zapier**：Zap 的步骤就是确定性动作（每次运行按配置执行）；Filter / Paths 做确定性条件；Code by Zapier 步骤跑脚本；REST Hook / Polling 触发器 + 去重（deduplication）。
- **Make**：模块串联 + 路由器（Router）分支 + 过滤器（Filter）+ 迭代器 + 聚合器 + 错误处理器，全部确定性执行；可加 Code 模块。
- **Coze**：工作流节点（动作 / 条件 / 并发）为确定性执行；「动作」节点可封装任意外部调用，条件节点按规则分支——即使平台主打 AI，工作流本体仍是确定性的可执行代码（扣子官方称"全代码工作流"，语义层 / 参数层 / 代码层三层驱动）。
- **Dify**：Workflow 的 HTTP Request、Code（Python/JS）、If-Else、Iteration、Loop、Template、List Operator、Variable Aggregator 等均为确定性节点；Webhook / Schedule / Integration 触发器确定性启动执行。

> 信源：
> - n8n 流程逻辑 / Code 节点：https://docs.n8n.io/build/flow-logic.md ；https://docs.n8n.io/integrations/builtin/core-nodes/n8n-nodes-base.code.md
> - Zapier 触发器 / 动作概念：https://docs.zapier.com/integrations/quickstart/how-zapier-works.md ；REST Hook / 去重：https://docs.zapier.com/integrations/build/hook-trigger.md ；https://docs.zapier.com/integrations/build/deduplication.md
> - Make 模块类型 / 组件：https://developers.make.com/custom-apps-documentation/app-components/modules.md ；https://developers.make.com/custom-apps-documentation/app-components/modules/action.md
> - Coze AI 生成式工作流（语义层/参数层/代码层、动作/条件/并发节点）：https://docs.coze.cn/guides_ai_powered_workflow_development.md
> - Dify 工作流节点清单（Code / HTTP Request / If-Else / Iteration / Loop 等）：https://docs.dify.ai/en/cloud/use-dify/build/workflow-chatflow.md

---

## ② AI Agent Skill 支持（对应维度 E~F）

### E. 各平台 AI 能力成熟度（是否引入 LLM 决策调工具）

| 平台 | AI 产品形态 | LLM 决策调工具？ | 成熟度 |
|---|---|---|---|
| **n8n** | **AI Agent 节点**（LangChain 底座，链 / Agent / 工具 / 记忆 / 向量库）；**Agents 一等公民**（Agent Builder：模型 / 指令 / 工具 / Skills / 知识库 / 记忆 / 子 Agent / 渠道 Slack/Telegram/Linear / 定时）；AI Assistant / AI Workflow Builder 用自然语言建流；`$fromAI()` 让 AI 填参数 | ✅ Agent 推理循环自主决策调工具（可给敏感工具设人工审批）；n8n 内置工具（节点）可 pin 参数，MCP 工具更放开 | 高（Agents 官方标为 Preview） |
| **Zapier** | **Zapier Agents**（agents.zapier.com：指令 / 动作=tools / 知识源 / 触发器 / Agent 间互调）；**AI by Zapier**（官方引导从 Agents 迁移）；Zapier Chatbots（可嵌入网站）；**Zapier MCP**（把 9000+ 应用、40000+ 动作作为 MCP 工具给任何 AI client）；Copilot 辅助改配置 | ✅ Agent 自主调用「动作」；Zapier MCP 由外部 LLM 决策调用 | 高（MCP 为其默认 AI 接入面） |
| **Make** | **Make AI / AI Agents**（API open beta：systemPrompt / defaultModel / 把场景作为工具 scenarios / MCP 工具 / RAG 上下文 / 输出解析器 / SSE 流式）；**Make MCP Server**（托管 Streamable HTTP，把 active/on-demand 场景暴露为 AI 可调工具，支持 OAuth 与 MCP token）；Make Skills（给 AI 助手的 Markdown 技能包） | ✅ Agent 推理循环调「场景 / MCP / RAG」三类工具（toolType 枚举），场景可设 auto-run 或 approval-required | 中高（AI Agents 标 open beta） |
| **Coze（扣子）** | **扣子 Agent**（原生智能体）+ 云端 Agent（第三方框架，跑在云电脑）+ 本地 Agent；**技能 / 技能商店**（Skill = 模块化能力包）；**插件**（含技能 / **MCP** / Panel 三类能力）；长期记忆 / 日程 / 会议旁听；**AI 编程（vibe coding）**：自然语言生成网页 / 移动应用 / 小程序 / 智能体 / 工作流；多人多 Agent 协作 | ✅ Agent 自主规划、决策、调技能 / 插件 / MCP | 高（平台本体就是 AI-first） |
| **Dify** | **Agent 应用**与**Agent 节点**（经典：Function Calling / ReAct 策略，工具 / 记忆 / 最大迭代 / 工具参数可 auto 由 Agent 生成；新 Agent 节点 beta：把已发布 Agent 作为完整 worker 嵌入工作流）；LLM / 知识检索 / 问题分类 / 参数抽取节点；Agentic Workflow（"把 AI 嵌入你定义的结构化边界内"）；工具系统（内置 + 自定义 + **MCP 工具**） | ✅ Agent 节点迭代推理调工具；工具参数支持 auto-generated（LLM 生成）与 manual | 高（LLMOps 为主业） |

> 信源：
> - n8n 构建与托管 Agent / MCP servers / 理解 AI 组件：https://docs.n8n.io/build/build-and-manage-agents.md ；https://docs.n8n.io/build/integrate-ai/mcp-servers.md ；https://docs.n8n.io/build/integrate-ai/understand-ai-components.md ；$fromAI：https://docs.n8n.io/build/integrate-ai/ai-examples/use-ai-for-parameters.md
> - Zapier 构建 Agent / 迁移到 AI by Zapier：https://help.zapier.com/hc/en-us/articles/24393442652557-Build-an-agent-in-Zapier-Agents ；https://help.zapier.com/hc/en-us/search?query=Agents ；Zapier MCP：https://docs.zapier.com/mcp/home.md
> - Make MCP Server：https://developers.make.com/mcp-server/make-mcp-server.md ；Make AI Agents API：https://developers.make.com/api-documentation/api-reference/ai-agents.md
> - Coze Agent 概述 / 插件（技能/MCP/Panel）/ 技能商店 / AI 编程：https://docs.coze.cn/cozespace_agent_overview.md ；https://docs.coze.cn/plugin.md ；https://docs.coze.cn/cozespace_skills_store.md ；https://docs.coze.cn/guides_ai_powered_workflow_development.md
> - Dify Agent 节点（Function Calling / ReAct / 新 Agent 节点）：https://docs.dify.ai/en/cloud/use-dify/nodes/agent.md ；Agentic Workflow 定位：https://docs.dify.ai/en/cloud/use-dify/build/workflow-chatflow.md ；工具与 MCP：https://docs.dify.ai/en/cloud/use-dify/workspace/tools.md

### F. 确定性工作流与 AI 节点 / Agent 的关系：**并存 + 互补（双通道），非替代**

**5 平台的统一模式是「AI 决策在上、确定性执行在下」**——确定性工作流不但没有被 AI 取代，反而被 AI Agent 当作「可调用的工具 / 执行层」来复用：

- **n8n**：官方明确 Agent "can also trigger or coordinate workflows to complete larger tasks"，工作流内也可「发消息给已发布 Agent」——双通道互调；AI Agent 的工具可以是「工作流（workflows in the same project）」；敏感工具可设人工审批（human-in-the-loop）。即**确定性工作流 = Agent 的工具箱**。
- **Zapier**：Agent 用的「动作」就是 Zap 里同一套集成动作；确定性 Zap 与 AI Agent 是两个独立产品线并行，Agent 内部靠动作（确定性执行）落地，AI 用于建 Zap（Zap Guesser / Copilot）与执行复杂任务。
- **Make**：**场景（确定性）就是 AI Agent 的工具**——AI Agents API 中 `scenarios` 字段把 Make 场景挂给 Agent，`approvalMode` 可设 `auto-run` / `approval-required`；Make MCP Server 明确"把 active/on-demand 场景变成 AI 可调工具"，并建议为场景写 inputs/outputs 描述帮助 AI 正确传参。
- **Coze**：Agent 靠插件 / 技能 / 工作流落地；工作流是「确定性可部署代码」，同时支持 AI 生成式工作流——**AI 既是执行主体（Agent），也是工作流的生成器**；工作流部署为 API 后可由任何程序确定性调用。
- **Dify**：官方把这种结构命名为 **Agentic Workflow**："Instead of relying on a single model to figure everything out, you design a flow that orchestrates models, tools, and logic step by step … **The AI is still doing the heavy lifting, but within boundaries you define**。"——AI 在确定性边界内工作；Agent 可作为一个节点嵌入确定性工作流，二者在同一画布并存。
- **结论**：**不存在「AI 取代确定性」的路径**。5 平台全部选择「确定性执行层 + AI 决策层」分层并存：确定性节点承担可复现、可审计、低成本的部分，AI 承担开放、不可预演的部分，且 AI 决策最终落在确定性节点/动作/场景上执行——这正是「互补 + 并存」的证据。

> 信源：
> - n8n Agent 触发/协调工作流、用工作流作工具、工具审批：https://docs.n8n.io/build/build-and-manage-agents.md ；Human-in-the-loop：https://docs.n8n.io/build/integrate-ai/ai-examples/human-in-the-loop-for-tools.md
> - Zapier Agent 动作与知识源（动作=tools）：https://help.zapier.com/hc/en-us/articles/26028298697485-Use-actions-on-Zapier-Agents ；https://help.zapier.com/hc/en-us/articles/24569690575117-Add-your-own-data-to-an-agent
> - Make AI Agents API（scenarios 作为工具、approvalMode）：https://developers.make.com/api-documentation/api-reference/ai-agents.md ；Make MCP Server（场景作为工具、inputs/outputs）：https://developers.make.com/mcp-server/make-mcp-server.md
> - Coze 工作流生成式开发 / 部署为 API：https://docs.coze.cn/guides_ai_powered_workflow_development.md ；https://docs.coze.cn/guides_deploy_vibe_workflow.md
> - Dify Agentic Workflow 官方定义：https://docs.dify.ai/en/cloud/use-dify/build/workflow-chatflow.md ；Agent 作为工作流节点：https://docs.dify.ai/en/cloud/use-dify/nodes/agent.md

---

## ③ 未来规划（对应维度 G）

### 战略定位与路线图：AI 化为主轴，但 MCP 与确定性执行层被同步强化

- **n8n**：定位从「工作流自动化」转向「**AI 与业务流程自动化结合**」（官方首页自述）。路线图信号：① Agent 成为一等公民（Preview 中，企业版支持"即将到来"）；② **MCP 双向往**：既支持 AI Agent 一键连外部 MCP Server 注册表，也提供实例级 MCP Server 让 Claude/Cursor 等通过 MCP 构建/管理 n8n 工作流与 Agent；③ AI Workflow Builder / AI Assistant 用自然语言建流；④ Gateway credits 统一代理 AI 模型调用。**确定性节点没有退役迹象**——Code/HTTP/流程节点仍是主执行引擎，AI 以「节点 / Agent」形式叠加。
- **Zapier**：战略口号 "Refound your company for the AI era"（ZapConnect 2026）。路线图信号：① **MCP 成为默认 AI 接入面**（官方"NEVER recommend AI Actions / Natural Language Actions (NLA). Both are retired. Use Zapier MCP instead."——NLA/AI Actions 已被官方退役，替代品是 MCP）；② Zapier Agents 与 AI by Zapier 整合（官方出迁移指南）；③ Zapier SDK（TypeScript）支持编程式调用 9000+ 应用，确定性 Zap / Trigger Inbox / Workflow API 仍在扩展。**趋势：AI 接入统一到 MCP，确定性 Zap 仍是底座**。
- **Make**：路线图信号：① **Make MCP Server**（托管 Streamable HTTP，OAuth / MCP token）把场景暴露给 Claude/ChatGPT 等 AI client；② **Make Skills**（Markdown 技能包，教 AI 助手建场景 / 配模块）；③ **AI Agents API（open beta）** 把 LLM + 场景工具 + RAG + MCP 组合成可编程 Agent；④ White Label / On-Prem 自托管线持续迭代。**趋势：确定性场景 = AI 的执行工具**，官方文档明示。
- **Coze（扣子）**：字节跳动押注 **AI-first**：扣子 3.0 重定义为一站式 AI 办公 / 协作 / 编程平台，Agent + 技能 + 插件（含 MCP）+ AI 编程（vibe coding）+ 多人多 Agent 协作；企业版走火山引擎生态（SSO、私网连接、KMS 加密、IAM、内容安全）。**趋势**：确定性工作流被「AI 生成式工作流」吸收（AI 帮用户搭工作流），但工作流本体仍是可部署为 API 的确定性代码；OpenClaw 类项目 2026-06-30 起停止新建，能力并入扣子 3.0 云端 Agent。**未见「确定性节点被 AI 取代」的官方表述，反而 AI 产出的仍是确定性工作流/API**。
- **Dify**：路线图信号：① **MCP 双向往**——应用可发布为 MCP Server（给 Claude Desktop / Cursor 等当工具），工作流与 Agent 内也可用 MCP 工具；② Agent 演进为「完整 worker」（新 Agent 节点 beta，自带 sandbox），Agentic Workflow 是官方主打概念；③ 插件市场（Marketplace）、difyctl CLI、企业级监控/可观测集成。**趋势：把「AI 应用 / 工作流」做成可被外部（含 AI client）确定性调用的服务**，与 n8n/Make 的 MCP 方向一致。

### 跨平台共性判断

1. **MCP 成为 5 家共同的技术方向**（n8n 双向、Zapier 默认、Make 托管 server、Coze 插件内、Dify 双向）——AI client 统一通过 MCP 消费「确定性能力」；这反过来印证：**确定性执行能力是 AI 生态的底层资产，不是被替代对象**。
2. **AI 化集中在「建流 / 决策 / 编排」层**（AI 建工作流、Agent 决策调工具、自然语言生成），**执行层保持确定性**（节点 / 动作 / 场景 / 工作流 / API）。
3. **没有任何一家官方宣布用 AI 取代确定性触发/命令通道**；相反，各平台都给「人工显式触发」留了护栏（n8n 工具审批、Make approval-required、Dify 人工输入节点等）。
4. 对本方案：5 家都只做到「确定性执行端点 + Webhook」，**「IM 内 `/` 命令菜单 → 参数 → 直达本地 HTTP」的「命令前端层」是空白**——与本方案「无 LLM、纯程序化传递」互补而非冲突。

### 需要标注的「未能核实 / 网络受限未访问」项

1. **Make 产品官网 www.make.com**：HTTP 403，**网络受限未访问**（产品级 UI 文档未核实）；已改从官方开发者中心 developers.make.com 核实 Custom Apps / MCP / AI Agents，并以其为准。
2. **Coze 旧版「经典工作流节点」明细**（如 HTTP/代码/条件/迭代节点逐个清单）：coze.cn 文档已整体迁移到扣子 3.0 / 扣子编程口径，旧版节点清单**未能逐项核实**；本报告以当前官方文档的「动作 / 条件 / 并发节点 + 部署为 API」为准。
3. **Zapier「Slack 触发器事件列表」**：未逐条核实（9000+ 应用目录未逐项访问）；本报告仅断言「Slack 等 IM 应用有触发器/动作」，并以 Webhook / Trigger Inbox / MCP 等已核实文档为准。
4. **Coze 海外版（coze.com）与国际版差异、以及 n8n Agents 企业版时间点**：官方只给"coming soon"，**未能核实**具体排期。
5. 各平台套餐 / 定价 / 额度数据未核实（不在本调研维度内）。

---

## 小结（对「确定性命令触发」方向的判断）

- **5 平台全部拥有成熟的确定性触发/执行通道**（Webhook / 定时 / 手动触发 + HTTP / Code / 分支 / 循环节点），默认执行不含 LLM；也都能被 IM 间接触发（Slack/飞书 事件 → Webhook 或专用触发器）——但**没有一家把「IM 内 `/` 命令 → 参数表单 → 直达本地 HTTP」做成产品**，它们只提供「执行端点」，命令前端层是空白。
- **AI 能力全部是高强度投入**：n8n Agent 节点/一等公民 Agent、Zapier Agents + AI by Zapier、Make AI Agents、Coze Agent 平台本体、Dify Agentic Workflow，全部引入 LLM 决策调工具。
- **确定性 vs AI 的关系 = 并存 + 互补，非替代**：确定性工作流被 AI Agent 当作「工具 / 执行层」复用（n8n Agent 调工作流、Make Agent 调场景、Zapier Agent 用动作、Dify Agent 节点嵌工作流、Coze 插件/工作流给 Agent 用），Dify 官方明确定义「AI 在确定性边界内干活」。
- **未来趋势 = AI 化 + MCP 化，但确定性执行层被同步强化**：5 家全部支持或主推 MCP（AI client 通过 MCP 调用确定性能力）；AI 化集中在「建流 / 决策」层，执行层仍是确定性代码；无人宣布取代确定性命令通道，反而普遍保留人工显式触发护栏。
- **对本方案启示**：①「IM `/` 命令 → 填参 → 无 LLM 直达本地 HTTP」在 5 家产品中都没有对应形态，是差异化空档；② 可用「Webhook + 结构化参数」作为我们触发端的对端协议参照；③ 确定性 vs AI 双通道并存已被头部 iPaaS / 编排平台验证，确定性命令触发（无 LLM）具有独立的、被 AI 消费的长期价值。


