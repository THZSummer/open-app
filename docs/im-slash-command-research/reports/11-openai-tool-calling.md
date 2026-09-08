# OpenAI Assistants / GPTs / Function Calling 竞品调研报告

> 调研主题：IM 内 `/` 唤起命令 → 选中命令 → 输入参数 → 触发用户本地 HTTPServer 的「确定性命令触发」（全程无 AI/LLM 参与）
> 调研日期：2026-09-08
> 信源：OpenAI 官方开发者文档（developers.openai.com：Function Calling / Using Tools / MCP / Skills / Programmatic Tool Calling / Agents SDK / Assistants 迁移 / GPT Actions / GPT Release Notes）。openai.com 博客与 help.openai.com 部分页面访问返回 403，已明确标注「网络受限未访问」，未据此编造内容。

---

## 【产品描述】

- **归属**：OpenAI（openai.com / platform.openai.com）。
- **形态**：**AI 模型 + 工具调用能力栈**（非 IM、非本地执行产品）。覆盖三层：① 底层 **Function Calling / Responses API**（开发者 API 能力）；② 中层 **Assistants API**（已停服）与 **Agents SDK / Agent Builder**（Agent 构建）；③ 消费层 **GPTs（Custom GPTs）+ GPT Actions**（ChatGPT 内的定制 AI，通过自然语言调用外部 REST API）。
- **定位**：官方自述为「让模型对接外部系统、访问训练数据之外数据的工具调用能力」；GPT Actions 自述为「把自然语言转换为 API 调用所需 JSON Schema」。核心形态是 **LLM 理解自然语言 → 决策调哪个工具 → 生成参数**。
- **目标用户**：API 开发者 / Agent 开发者（Function Calling、Responses API、Agents SDK）；ChatGPT 消费者与业务人员（GPTs / GPT Actions）。
- **同类型分类**：**「不同类型」对照产品** —— OpenAI 工具调用是 **AI 驱动（LLM 决策 → 调工具）** 的典型代表，与本方案「确定性命令触发（无 LLM）」形成方向性对照。README 对标矩阵初判「❌ 无（LLM 决策调工具）」经调研核实**成立**。

---

## ① 非 AI 本地 Command 支持（对应维度 A~D）

**结论先行：OpenAI 体系不存在「非 AI 确定性命令」概念。** 工具/函数的选择、参数生成、编排全部由模型完成；没有任何「用户显式选命令 → 显式填结构化参数 → 程序化路由」的旁路。唯一接近「确定性」的机制（`tool_choice` 强制、`allowed_tools` 白名单）是**开发者侧的约束开关，不是面向用户的命令菜单**，且参数值仍由 LLM 生成。

### A. 命令入口与唤起

- **无 `/` 唤起、无命令列表、无用户选择环节**：工具调用的唯一入口是 API 请求里的 `tools` 参数（每个函数/工具配 `name` + `description` + `parameters`）。模型「检查 prompt 后自行判断是否需要调用某个工具」（官方 How it works 原文：*"it may decide that it needs data or functionality provided by a tool"*）。官方给出的完整调用流是「开发者预置 tools → 模型发起 tool call → 开发者执行 → 回传结果 → 模型总结」，模型是决策者，用户/开发者是被动执行方。
- **「候选工具集」由开发者预置，非用户枚举**：`tools` 列表在请求中静态声明；`tool_search`（gpt-5.4+）允许**模型**按需延迟加载 `defer_loading` 的命名空间工具（`namespace`），仍是模型决策，不是用户浏览命令。
- **无客户端/服务端命令清单下发机制**：不存在命令注册中心、命令发现 UI 或命令市场；工具能力只存在于代码请求里。

> 信源：
> - https://developers.openai.com/api/docs/guides/function-calling（tool calling 五步流程、"model decides" 机制、`tools` 参数、`tool_search`）
> - https://developers.openai.com/api/docs/guides/tools（工具总览、`tools` 参数用法）
> - https://developers.openai.com/api/docs/guides/tools-tool-search（命名空间 / 延迟加载，模型决策加载）

### B. 参数输入

- **无用户显式填参**：参数全部由 LLM 生成（`arguments` 为 JSON 字符串），不存在「命令后跟参数 / 表单 / Modal / Autocomplete」的面向用户输入形态。
- **但存在强类型参数 Schema 机制（开发者侧）**：每个 function 用 **JSON Schema** 声明 `parameters`，支持类型、枚举、嵌套对象、递归对象、`required`、`additionalProperties: false` 等丰富特性。
- **Strict mode（结构化输出）显著增强确定性**：`strict: true` 强制函数调用严格符合 schema（官方：*"reliably adhere to the function schema, instead of being best effort"*），要求每个 object 设 `additionalProperties: false` 且所有字段标记 `required`，可选字段用 `type: ["string","null"]` 表示；不合规的 schema 直接拒绝。Responses API 默认尝试规范化到 strict。
- **GPT Actions 是「API schema ↔ 自然语言」转换器**：开发者把第三方 REST API 的 OpenAPI/JSON Schema 编码进 Action，**模型**负责把用户自然语言「翻译」成 API 调用参数（官方原文：*"convert natural language text into the json schema required for an API call"*）——即 schema 面向 LLM，而非用户。
- **Programmatic Tool Calling 提供 `output_schema`**：可声明工具返回的结构化 JSON 形态，供模型生成的 JS 程序可靠消费，仍是模型侧契约。

> 信源：
> - https://developers.openai.com/api/docs/guides/function-calling（`parameters` JSON Schema 定义表、strict mode 规则）
> - https://developers.openai.com/api/docs/actions/introduction（GPT Actions 将自然语言转 JSON Schema、依赖 Function Calling）
> - https://developers.openai.com/api/docs/guides/tools-programmatic-tool-calling（`output_schema`）

### C. 命令注册与下发

- **工具「注册」= 代码里的 `tools` 参数**：函数定义随每次请求提交（或在 namespace / `tool_search` 中声明、由模型延迟加载），没有独立注册中心。
- **GPTs 侧为「开发者配置」**：Custom GPT 由用户在 Builder 中配置 instructions + knowledge + Actions；Actions 需配置鉴权（无鉴权 / API Key / OAuth）。
- **无「用户本地上报 / 注册」概念**：所有工具托管在 OpenAI 平台 / 公网远程服务，用户侧无注册入口。

> 信源：
> - https://developers.openai.com/api/docs/guides/function-calling
> - https://developers.openai.com/api/docs/actions/introduction（Actions 鉴权与配置）
> - https://developers.openai.com/api/docs/actions/authentication

### D. 触发到本地的链路

- **不存在「命令触达用户本地 HTTP Server」的产品模型**：OpenAI 是云端 API 服务；函数由**开发者自己的服务器**执行（模型只返回 tool call，开发者代码实现并回传结果）。本地侧能力体现为 **Secure MCP Tunnel**（私网/本地 MCP 服务器经出向隧道接入，无需暴露公网）与 **shell 工具 local 模式**（模型在开发者本机运行时执行命令）——两者入口均为**模型调用**，且面向开发者运行环境，而非「用户在 IM 里显式触发命令」。
- **MCP 调用由模型发起**：`type: "mcp"` 工具列出远端工具（`mcp_list_tools`）→ 模型调用（`mcp_call`）；默认需审批（`require_approval`），可对单个/全部工具豁免。链路方向是「OpenAI 云端 → 公网/隧道」，与本方案「IM → 用户本地」相反。
- **确定性执行存在但属「模型编排」**：Programmatic Tool Calling 让**模型生成的 JavaScript** 在托管 V8 运行时里并行/循环/条件编排工具调用，再由开发者执行客户侧函数——仍是模型写的程序，非用户显式命令。

> 信源：
> - https://developers.openai.com/api/docs/guides/tools-connectors-mcp（mcp_list_tools / mcp_call、require_approval、Secure MCP Tunnel 提及）
> - https://developers.openai.com/api/docs/guides/tools-shell（本地 shell / 托管容器执行）
> - https://developers.openai.com/api/docs/guides/tools-programmatic-tool-calling（托管 JS 运行时、客户侧函数执行）

---

## ② AI Agent Skill 支持（对应维度 E~F）

### E. 纯 AI 驱动的工具调用成熟度 —— 极高，是行业标杆

- **Function Calling（核心，全成熟）**：并行函数调用（`parallel_tool_calls`）、`tool_choice`（auto / required / 强制指定函数 / `allowed_tools` 白名单 / none）、streaming 增量参数（`response.function_call_arguments.delta`）、strict mode、命名空间 + tool search 延迟加载、异步工具调用（async tool calling）。官方建议单轮初始暴露少于 20 个函数以保证准确率。
- **内置工具矩阵（Responses API）**：

| 工具 | 说明 |
|------|------|
| Function calling | 调用开发者自定义函数（JSON Schema 声明参数） |
| Web search | 联网检索并入上下文 |
| File search | 基于向量库（vector store）检索上传文件 |
| Code Interpreter | 沙箱容器（1g/4g/16g/64g）中写并运行 Python，模型以 "python tool" 使用 |
| Computer use | 模型控制浏览器/桌面界面（agentic 工作流） |
| Image generation | 生成/编辑图片 |
| Shell（hosted / local） | 托管容器或本机运行时执行 shell 命令 |
| Skills | 上传版本化 `SKILL.md` 技能包（兼容开放 Agent Skills 标准），挂载到 shell 环境，由模型决定何时使用 |
| Remote MCP / Connectors | 连接第三方服务（详见下） |

- **MCP 支持（一等领域）**：远程 MCP 服务器（Streamable HTTP / HTTP-SSE）+ OpenAI 维护的 Connectors（Dropbox / Gmail / Google Calendar / Google Drive / Teams / Outlook / SharePoint）+ 私网 **Secure MCP Tunnel**；支持 OAuth 鉴权、`allowed_tools` 过滤、调用审批（`require_approval`：always / never / 按工具豁免）。官方明确：MCP 调用只按 token 计费、无额外费用。
- **Assistants API 已停服**：官方文档明确 **Assistants API 于 2026-08-26 正式 sunset、不再可用**，迁移到 Responses API（Assistants→Prompts、Threads→Conversations、Runs→Responses、Run steps→Items）。
- **Agent 编排层**：Agents SDK（TS/Python，官方开源）提供 agent loop、handoffs、agents-as-tools、guardrails、审批流、tracing；另有 Agent Builder（低代码节点编排）与 Responses API Multi-agent。ChatGPT 消费侧为 GPTs + GPT Actions（底层即 Function Calling）。

> 信源：
> - https://developers.openai.com/api/docs/guides/tools（工具总览矩阵）
> - https://developers.openai.com/api/docs/guides/function-calling（并行调用 / tool_choice / 20 函数建议 / streaming）
> - https://developers.openai.com/api/docs/guides/tools-code-interpreter（沙箱容器）
> - https://developers.openai.com/api/docs/guides/tools-connectors-mcp（MCP / Connectors / Tunnel / 审批）
> - https://developers.openai.com/api/docs/guides/tools-skills（Skills、开放 Agent Skills 标准）
> - https://developers.openai.com/api/docs/assistants/migration（Assistants 停服 2026-08-26）
> - https://developers.openai.com/api/docs/guides/agents（Agents SDK 概览）
> - https://developers.openai.com/api/docs/actions/introduction（GPT Actions 基于 Function Calling）

### F. 与「用户显式选命令」的关系 —— 明确的对立面；官方立场下确定性边界由「审批 + 受限产品体验」承担

- **OpenAI 是「AI 理解自然语言 → 决定调工具」的对立面样本，二者非替代关系**：OpenAI 不提供任何用户侧显式命令枚举/参数表单通道，其「确定性」全部收敛为**开发者侧约束**（tool_choice、allowed_tools、strict schema、require_approval），且这些约束控制的是「模型能不能 / 必须调哪个工具」，**参数仍由模型生成**。本方案「用户选命令 + 用户填参 + 程序化转发」在 OpenAI 体系中无对应物 → 不存在被其取代的问题，属**并存互补**：确定性通道负责「用户明确意图 + 低风险强结构任务」，AI 通道负责「意图不明 + 需理解的任务」。
- **官方安全指引反向佐证确定性选命令的价值**：Skills 文档明确警示 **「不要把开放的 Skills 仓库暴露给终端用户自由浏览、选择、挂载任意 Skill」**，要求「由开发者审查集成后，**只通过受约束的产品体验（bounded product experiences）暴露**，把 Skills 映射到具体工作流、对写/高影响动作设审批门槛」。这正对应本方案「确定性命令清单 + 显式确认 + 参数结构化」的产品思路——OpenAI 也在为 AI 工具调用加"确定性护栏"，而非消灭确定性。
- **GPTs 消费者侧同样无显式命令**：用户在 ChatGPT 里用自然语言与 GPT 交互，GPT 决定是否调 Action（官方：*"simply ask a question in natural language, and ChatGPT provides the output in natural language"*）。

> 信源：
> - https://developers.openai.com/api/docs/guides/function-calling（tool_choice / allowed_tools / strict mode）
> - https://developers.openai.com/api/docs/guides/tools-skills（Risks and safety：不暴露开放 Skills 目录、bounded product experiences、审批门槛）
> - https://developers.openai.com/api/docs/actions/introduction（自然语言 ↔ API 调用，无显式命令）

---

## ③ 未来规划（对应维度 G）

**趋势判断：OpenAI 正把「工具调用」推向更强的模型自主编排（agentic）+ 更强的开发者侧确定性控制（schema 约束 / 审批），但方向始终是 AI 决策，不提供面向用户的确定性命令通道。**

- **API 形态收敛到 Responses API**：官方明确 *"Responses is recommended for all new projects"*（Chat Completions 仍支持）。Responses 定位为「agentic 原生循环」（一个请求内可连环调用 web_search / code_interpreter / MCP / 自定义函数），并持续下放 Assistants 停服后的能力。
- **Function Calling 语法与约束持续演进**：
  - **Strict mode / Structured Outputs**：默认开启趋势（Responses 省略 `strict` 即尝试规范化到严格模式），函数参数 JSON Schema 约束越来越强。
  - **Tool search + Namespace**（gpt-5.4+）：工具定义延迟加载、按域分组，解决「大规模工具面」的 token 成本问题。
  - **Programmatic Tool Calling**：模型写 JS 编排工具（并行 / 循环 / 中间结果保留），把「编排」进一步模型化；官方给出「何时用 programmatic vs 直接调用」的分流建议，且强调**写 / 敏感动作仍需直接调用 + 审批**。
  - **Async tool calling / 审批 / allowed_tools / 提示缓存友好**等工程化控制继续完善。
- **MCP 成为连接层的战略方向**：Connectors + 远程 MCP + **Secure MCP Tunnel（私网 / 本地 / 防火墙后服务器出向接入）** + OAuth 标准 + 调用审批，显示 OpenAI 以 MCP 为工具生态互联标准；官方亦发布「构建 MCP 服务器」指引（plugins / deep research / API 集成）。
- **Skills 走开放标准 + 版本化管理**：Agent Skills（兼容 agentskills.io 开放标准）支持版本、默认版本、curated skills、inline bundle；本地 shell 模式支持在本机运行技能。官方把技能作为「托管 / 本机执行的版本化指令包」，强调开发者审查集成、不做开放仓库。
- **Agent 开发栈产品化**：Agents SDK（开源、TS/Python）、Agent Builder（低代码节点 → 导出代码）、ChatGPT Workspace Agents、Multi-agent、guardrails / approvals、tracing / evals 全面铺开——Agent 从「单次工具调用」走向「带状态的多智能体工作流」。
- **GPTs 侧**：GPT 生态持续运营（GPT Store、评分、Actions 文件收发等，见 GPT Release Notes）；其博客 / 帮助页（openai.com/blog/introducing-gpts、help.openai.com GPTs 相关）**网络受限未访问**，消费侧产品细节未进一步核实。

> 信源：
> - https://developers.openai.com/api/docs/guides/migrate-to-responses（Responses 为推荐方向、能力对照表）
> - https://developers.openai.com/api/docs/guides/function-calling（strict mode / tool search / 最佳实践）
> - https://developers.openai.com/api/docs/guides/tools-tool-search（延迟加载命名空间）
> - https://developers.openai.com/api/docs/guides/tools-programmatic-tool-calling（模式分流建议、敏感动作需审批）
> - https://developers.openai.com/api/docs/guides/tools-connectors-mcp（MCP 战略、Secure MCP Tunnel）
> - https://developers.openai.com/api/docs/guides/tools-skills（开放标准 / 版本化 / 安全边界）
> - https://developers.openai.com/api/docs/guides/agents（Agents SDK 与 Responses API 路线）
> - https://developers.openai.com/api/docs/gpts/release-notes（GPT 生态迭代）

---

## 小结（对本方案的对照意义）

| 维度 | OpenAI 工具调用 | 本方案（确定性命令） |
|------|----------------|----------------------|
| 触发方 | 模型（LLM 决策） | 用户显式选择 |
| 参数生成 | 模型生成（schema 约束） | 用户显式填写（结构化） |
| 确定性 | 开发者侧约束（tool_choice / strict / 审批） | 全程程序化，天然确定 |
| 本地链路 | 云端 → 公网/隧道（MCP / shell local） | IM → 用户本地 HTTPServer |
| 定位关系 | AI 通道，负责"理解型"任务 | 确定性通道，负责"明确型"任务 |

**结论：OpenAI 全栈只有 AI 通道，没有面向用户的确定性命令通道；其确定性收敛在开发者侧约束。确定性命令与 AI 工具调用是**并存互补**关系——确定性负责「用户意图明确、需可预期可审计」的任务，AI 负责「意图模糊、需理解」的任务。OpenAI 官方对"开放工具仓库"的安全警示（要求受约束的产品体验 + 审批），反向印证了本方案"显式命令清单 + 参数结构化 + 显式确认"的工程价值。**

