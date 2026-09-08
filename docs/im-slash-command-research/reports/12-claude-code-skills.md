# Claude Code Skills / MCP（Anthropic）竞品调研报告

> 调研主题：IM 内 `/` 唤起命令 → 选中命令 → 输入参数 → 触发用户本地 HTTPServer 的「确定性命令触发」（全程无 AI/LLM 参与）
> 调研日期：2026-09-08
> 信源：code.claude.com（Claude Code 官方文档：skills / commands / mcp）、modelcontextprotocol.io（MCP 官方文档与规范）、claude.com/blog（Anthropic 官方博客）、agentskills.io（Agent Skills 开放标准）、a2a-protocol.org（A2A 官方站点）。未核实内容已明确标注「未能核实 / 网络受限未访问」

---

## 【产品描述】

- **归属**：Anthropic。对象为 Claude Code（CLI/IDE/桌面 AI 编程 Agent）、Claude 平台（Claude apps + Claude Code + API）以及 Anthropic 主导推动的 Agent Skills / MCP（Model Context Protocol）开放标准生态。
- **形态**：**AI Agent 工具链 + 开放协议生态**。Claude Code 是终端型 AI 编程 Agent；Skills 是「SKILL.md 指令包」；MCP 是连接「AI 应用 ↔ 外部工具/数据」的开放协议（官方比喻「AI 的 USB-C 口」）。
- **定位**：Anthropic 官方自述 —— Skills 让 Claude "perform specialized tasks better"（打包领域知识 / 可复用工作流），MCP 是 "an open-source standard for connecting AI applications to external systems"；整体战略是「AI Agent（理解自然语言 → 决策 → 调工具）」驱动一切。
- **目标用户**：Claude Code 终端开发者 / 企业团队 / 平台开发者；Skill 作者与 MCP Server 开发者；Claude apps（Pro/Max/Team/Enterprise）用户。
- **同类型分类**：**「不同类型」对照产品（本方案的对立面）** —— Claude Code Skills / MCP 是 **AI 驱动**（LLM 理解自然语言 → 决定调用哪个 Skill / MCP 工具 → 执行）的核心产品，与本方案「确定性命令触发（无 LLM，纯程序化传递）」形成方向性对照，用于验证确定性命令 vs AI Skill 是替代、互补还是并存。

---

## ① 非 AI 本地 Command 支持（对应维度 A~D）

**结论先行：Claude Code 有 `/` slash 命令体系，且存在真正的确定性内置命令（行为被编码进 CLI、不经 LLM）；但用户自定义的「命令 / Skill」即使被用户显式选中、填了参数，最终执行仍要经过 LLM 读取指令后再行动——不存在「用户选命令 → 填参 → 无 LLM 直达本地服务」的完整确定性旁路。最接近本方案形态的是 MCP Prompts 以命令形式暴露（用户显式选 + 传参），但执行结果仍交给 LLM。** 详析如下。

### A. 命令入口与唤起

- **有 `/` 命令体系**：Claude Code 输入 `/` 即弹出命令菜单（可继续键入过滤），命令只在消息开头被识别，命令名后的文本作为其参数；支持 `/` 后连续叠放最多 6 个 Skill（`/skill-a /skill-b do XYZ`）。
- **命令分三类**（关键区分，官方原文口径）：
  1. **内置命令（built-in commands）**："Most are built-in commands whose behavior is **coded into the CLI**" —— 如 `/init` `/model` `/compact` `/context` `/mcp` `/permissions` 等，执行**固定逻辑（确定性）**，不依赖 LLM 决策。
  2. **Bundled Skills**：内置 Skill（`/doctor` `/code-review` `/batch` `/debug` `/loop` `/claude-api` 等），官方明确 "Bundled skills are **prompt-based**: they give Claude detailed instructions and **let it orchestrate the work using its tools**" —— 即**仍由 LLM 执行**，与「coded into the CLI」的内置命令形成鲜明对照。
  3. **自定义命令 = Skill**：官方明确 "**Custom commands have been merged into skills.** A file at `.claude/commands/deploy.md` and a skill at `.claude/skills/deploy/SKILL.md` both create `/deploy` and work the same way." 目录名即命令名（`.claude/skills/deploy/SKILL.md` → `/deploy`）。
- **Skill 被 LLM 发现与触发**（Agent Skills 标准 = 渐进式披露 progressive disclosure，三段式）：
  - **Discovery**：启动时只把每个 Skill 的 name + description 载入上下文，LLM 据此判断何时相关；
  - **Activation**：任务命中 description 时，把完整 `SKILL.md` 指令读入上下文；
  - **Execution**：LLM 按指令执行，可执行捆绑脚本 / 加载引用文件。
  - 用户在 `/` 菜单显式选中即直接激活（见 B/C 的用户侧路径）。
- **「用户显式选命令」的确定性旁路 —— 存在但分档**：
  - **完全确定性**：只有内置命令（coded into CLI）属于此档；它们是固定集，不可由用户扩展。
  - **显式选择 + 参数化，但仍过 LLM**：Skill / 自定义命令被用户 `/name` 调用时，用户**显式选择了命令**、**显式传了参数**（这一环是确定性的，不经过 LLM 理解自然语言），但之后 Skill 内容（指令 + 参数替换结果）作为**一段 prompt 交给 LLM**，由 LLM 编排工具完成动作 —— **执行环节仍是 AI**。
  - 结论：Claude Code 里「用户显式选命令 + 填结构化参数 → 纯程序化直达本地 HTTP Server、全程零 LLM」的完整通道**不存在**；与 MCP 协议层的确定性 `tools/call`（见②-E）也不同。

> 信源：
> - https://code.claude.com/docs/en/commands（命令全表："coded into the CLI"、Skill/Workflow 标注、命令菜单匹配、技能叠放）
> - https://code.claude.com/docs/en/skills（自定义命令并入 Skills；`/` 调用；Skill 位置、命令名来源）
> - https://agentskills.io/（Discovery / Activation / Execution 渐进式披露定义）

### B. 参数输入

- **Skill 参数 = 文本替换，无类型校验**（与本方案「结构化表单填参」的分界处）：
  - frontmatter `argument-hint`（自动补全提示，如 `[issue-number]`）与 `arguments`（命名位置参数）。
  - 内容占位符：`$ARGUMENTS`（全部参数）、`$ARGUMENTS[N]` / `$N`（按位置）、`$name`（按命名）、`${CLAUDE_*}` 环境占位符。
  - 例：`/fix-issue 123` → LLM 收到 "Fix GitHub issue 123 following our coding standards..."。参数本质是**替换进 prompt 的字符串**，Claude Code 不校验类型 / 必填（位置缺参则占位符留原文），最终由 LLM 理解后执行。
- **MCP 工具参数 = JSON Schema（有类型校验）**：MCP 工具通过 `inputSchema`（JSON Schema）声明参数，如 `{type:object, properties:{location:{type:string}, units:{type:string, enum:[metric,imperial,kelvin], default:metric}}, required:[location]}` —— 这是**给 LLM 看的工具契约**，供模型正确填参调用 `tools/call`，不是给用户填的表单。
- **MCP Prompts 作为命令的参数**：MCP Server 暴露的 prompt 会以 `/servername:promptname (MCP)` 出现在 `/` 菜单，参数按空格分隔传（`/mcp__github__pr_review 456`），"Arguments are parsed based on the prompt's defined parameters" —— **这是最接近本方案「命令 + 参数」的 UX**，但 prompt 内容是模板消息，执行结果仍注入对话交给 LLM。
- **动态上下文注入（最「程序化」的旁路）**：Skill 正文里的 `` !`<command>` `` / ` ```! ` 代码块会在 Skill 载入时**确定性执行 shell 命令**，输出替换占位符后再给 LLM（如 `` PR diff: !`gh pr diff` ``）。这是一条**绕过 LLM 的确定性数据获取通道**，但仅限「取数据注入上下文」，不是用户触发的命令执行器；且同步来源（claude.ai sync 的 Skill）与 `disableSkillShellExecution` 策略下会被禁用 / 替换为占位符。

> 信源：
> - https://code.claude.com/docs/en/skills（argument-hint / arguments / $ARGUMENTS / $N / named arguments；Inject dynamic context 与 `!` 命令）
> - https://modelcontextprotocol.io/docs/2026-07-28/learn/architecture（tools/list 返回 `inputSchema` JSON Schema；tools/call 带 `arguments`）
> - https://code.claude.com/docs/en/mcp#use-mcp-prompts-as-commands（MCP prompts 作为命令：发现、空格传参）

### C. 命令注册与下发

- **Skill / 自定义命令注册在本地文件系统**，按层级分布：
  - 企业级（managed settings）→ 个人级 `~/.claude/skills/<name>/SKILL.md` → 项目级 `.claude/skills/<name>/SKILL.md` → 插件级 `<plugin>/skills/<name>/SKILL.md` → claude.ai 账号同步（`~/.claude/skills/synced/`）。
  - 命令名冲突按源解析（enterprise > personal > project；Skill 优先于同名 command 文件；插件用 `plugin-name:skill-name` 命名空间）。
  - 实时变更检测：会话中新增 / 修改 SKILL.md 无需重启即可生效（仅文本；插件级需 `/reload-plugins`）。
- **MCP Server 注册在配置层**：`claude mcp add`（CLI）/ `.mcp.json`（项目级，可入库共享）/ `~/.claude.json`（local / user 级）；transport 支持 **stdio / streamable HTTP（推荐）/ SSE（已弃用）/ WebSocket**；远程 server 支持 OAuth 鉴权；`/mcp` 面板管理连接状态、工具数、动态刷新（`list_changed` 通知）。
- **MCP Server 的能力发现是协议级确定性**：客户端发 `server/discover`（版本 + capabilities 协商）→ `tools/list` / `prompts/list` / `resources/list` 枚举 —— **枚举本身确定性、可程序化**（任何 client 都能调用，不限于 LLM）；本方案「命令列表」与 MCP 的 `tools/list` / `prompts/list` 在形态上是同构的。

> 信源：
> - https://code.claude.com/docs/en/skills（Skill 存储层级、命令名冲突、实时变更检测）
> - https://code.claude.com/docs/en/mcp（`claude mcp add`、`.mcp.json`、scope、transport、`/mcp` 面板、OAuth）
> - https://modelcontextprotocol.io/docs/2026-07-28/learn/architecture（server/discover、tools/list、capabilities）

### D. 触发到本地的链路

- **MCP 原生支持本地进程与本地网络服务**：
  - **stdio transport**：MCP Server 作为本地子进程（`npx ...` / `python server.py`），Claude Code 通过 stdin/stdout 与其通信 —— **这是「AI Agent → 本机程序」的确定性传输通道**，但调用方是 LLM 决策后触发的 `tools/call`。
  - **streamable HTTP transport**：HTTP POST（可选 SSE 流）+ OAuth，支持远程 server；`${VAR}` 环境变量替换、`${CLAUDE_PROJECT_DIR}` 注入 server 环境。
- **「直达用户本地 HTTPServer」在本产品中的语义**：Claude Code 作为 MCP host，其调用 MCP 工具 = 从 LLM 拿到 `tools/call` 请求后**确定性转发给 server 执行**（server 端 `tools/call` 是纯程序化）。即：**「执行」是确定的，但「谁决定执行」是 LLM**。与本方案「用户自己就是决策者、全程无 LLM」的链路仅在最终一跳等价。
- **其他确定性本地链路**：`!` 动态上下文注入（本地 shell 执行）、Hooks（事件触发的确定性 shell 脚本）、channels（MCP server 反推消息进会话，如 Telegram / Discord / webhook 事件）。均非「用户 → 命令 → 本地 HTTP」的产品形态。

> 信源：
> - https://code.claude.com/docs/en/mcp（stdio / HTTP / SSE / ws transport、`${CLAUDE_PROJECT_DIR}`、channels）
> - https://code.claude.com/docs/en/skills（`!` 动态上下文注入、hooks）
> - https://modelcontextprotocol.io/docs/2026-07-28/learn/architecture（stdio / streamable HTTP transport、tools/call）

---

## ② AI Agent Skill 支持（对应维度 E~F）

### E. 纯 AI 驱动（本方案的对立面）—— 高，且是产品本体的核心

- **技能触发 = LLM 自主决策**：Skills 默认「both you and Claude can invoke」。官方原文："While working on tasks, Claude **scans available skills** to find relevant matches. When one matches, it loads only the minimal information…" —— LLM 从 description 判断相关性后自主加载 / 激活，用户可见于 Claude 的思考链中。MCP 工具同理："The AI application … automatically generates the appropriate tool calls during conversations"（MCP 官方架构文档）。
- **MCP 工具调用范式（本方案对立面的标准形态）**：
  - LLM 决定调用哪个工具 → host 拦截 `tools/call` → 路由到对应 MCP server 执行 → 结果作为上下文回填对话。
  - 工具元数据：`name`（唯一标识）、`title`、`description`、`inputSchema`（JSON Schema）。`tools/list` 支持分页与缓存（ttlMs / cacheScope）；`server/discover` 做版本 / 能力协商；`list_changed` 通知动态增删工具；支持**渐进式工具发现**（不一次性全量载入）。
  - Server 侧原语：**Tools**（可执行函数）、**Resources**（上下文数据）、**Prompts**（模板消息）；Client 侧原语：**Elicitation**（server 反向向用户要信息 / 确认，2026-07-28 版取代 deprecated 的 Sampling）。
- **用户侧的「防 AI 自动触发」开关（对本方案最重要的可类比机制）**：
  - `disable-model-invocation: true`：**只有用户能调用**（官方用例：带副作用 / 需要控制时机的 `/commit` `/deploy` `/send-slack-message` —— "You don't want Claude deciding to deploy because your code looks ready"）。若 LLM 试图绕过，Claude Code 会拦截并提示用户自行运行 `/deploy`。
  - `user-invocable: false`：只有 Claude 能调用（背景知识类）。
  - 表格化：

| frontmatter | 用户可调用 | Claude 可调用 | 上下文加载 |
|---|---|---|---|
| （默认） | ✅ | ✅ | description 常驻，完整内容调用时加载 |
| `disable-model-invocation: true` | ✅ | ❌ | description 不载入，用户调用时才加载完整内容 |
| `user-invocable: false` | ❌ | ✅ | description 常驻，调用时加载完整内容 |

  - **含义**：Anthropic 官方把「确定性 / 受控」作为 Skill 的显式配置维度 —— **用户显式触发被官方当作「副作用操作」的护栏**。但这仍是「用户选定 + LLM 执行」，与本方案「用户选定 + 程序化执行」差在最后一步。
- **MCP 生态成熟度 —— 高**：
  - 架构：Host（AI 应用）→ Client（每 server 一个）→ Server（本地 stdio / 远程 HTTP）。JSON-RPC 2.0；数据层（原语 + 发现 + 通知）+ 传输层（stdio / streamable HTTP）。
  - 客户端覆盖广（MCP 官方列举）：Claude、Claude Code、ChatGPT、VS Code / Copilot、Cursor、MCPJam 等；Anthropic Directory 提供已审核 connector。
  - Claude Code 作为 MCP host：`/mcp` 面板、`claude mcp` CLI、OAuth、超时 / 重连 / 后台化（长工具调用 2 分钟转后台任务）、v2 runtime（MCP SDK 2.0、协议版本 2026-07-28 协商）、channels 反推消息。
  - 开放治理：MIT 协议；`GOVERNANCE.md` + 跨厂商 Working Group（**Agents WG / Registry WG / Skills Over MCP WG / Primitive Grouping WG**；Skills Over MCP WG 成员含 Anthropic、Google、AWS、GitHub、Databricks、Bloomberg 等 maintainer）；SEP 提案流程。

> 信源：
> - https://claude.com/blog/skills（Claude 自动扫描 / 加载 Skill；Skills 跨 Claude apps / Code / API）
> - https://code.claude.com/docs/en/skills（invocation control 两字段与表格语义；Bundled skills prompt-based）
> - https://modelcontextprotocol.io/docs/2026-07-28/learn/architecture（tools/list、tools/call、inputSchema、server/discover、client/server primitives）
> - https://modelcontextprotocol.io/community/working-groups/skills-over-mcp（跨厂商 WG 治理与成员）

### F. Skill 与 MCP 的关系，及与确定性命令的关系：**并存 + 互补，非替代**

- **Skill 与 MCP 是两层不同抽象**：
  - **Skill = 内容格式（markdown 指令包）**：`SKILL.md`（frontmatter 元数据 + 指令正文）+ 可选脚本 / 引用 / 模板；被 LLM 按需加载执行。遵循 **Agent Skills 开放标准**（agentskills.io，Anthropic 发起的开放格式），被 Claude Code / Claude apps / API 及大量第三方客户端采用（官方展示含 OpenCode、Cursor、Copilot、Gemini CLI、VS Code、ChatGPT & Codex、TRAE、Goose、OpenHands 等数十家）。
  - **MCP = 传输协议（工具 / 资源 / prompt 的连接标准）**：定义 host–server 间如何发现与调用能力，服务端由程序实现，调用最终确定性执行。
  - 官方定位互补："**MCP is for agent-to-tool communication**；**A2A is for agent-to-agent communication**"。Skill 与 MCP 的关系正在标准化：MCP 官方 **"Skills over MCP" Working Group** 正在把 agent skills 做成 **MCP 一等公民原语**（SEP-2640，Resources-based，Extensions Track，"rich, structured instructions for agent workflows, discovered and consumed through MCP"），并与 Agent Skills 规范（agentskills.io）对齐 —— 即 **skills 将来可通过 MCP 分发 / 发现 / 消费**，与 tools/resources/prompts 并列。
- **确定性命令 vs AI Skill —— 并存 + 互补（对本方案最重要的判定）**：
  - Claude Code 同时保留三类：**确定性内置命令**（coded into CLI）、**AI 驱动 Skill**（prompt-based）、**MCP Prompts 命令**（确定性枚举暴露 + LLM 消费）。官方没有「用 AI 消灭确定性命令」的表述；相反，`disable-model-invocation` 机制专门**防止 AI 抢跑副作用操作**，把「用户显式触发」当护栏 —— 说明 Anthropic 认可**用户显式触发的价值**。
  - 但注意分工：确定性通道主要服务于 **CLI 交互本身**（/model、/compact、/mcp）而非用户业务动作；业务动作默认走 AI Skill（LLM 自主或用户 `/name` 触发）。即：**确定性被保留为「工具底座 / UI 管道」，AI 被推为「业务执行主体」**——与 Salesforce Agentforce「确定性内化为 agent 执行层」的方向一致（见 08 号报告）。
  - MCP Prompts 命令模式是本方案最可对标的参照：**用户 `/` 选择 → 空格传参 → 确定性路由到 server** 的 UX 已经存在；但它把 prompt 结果交给 LLM 而非确定性执行本地 HTTP —— **这恰好是本方案（无 LLM 直达本地 HTTPServer）可以差异化切入的空档**。

> 信源：
> - https://modelcontextprotocol.io/specification/latest（MCP 特性与 Skills over MCP 扩展）
> - https://modelcontextprotocol.io/community/working-groups/skills-over-mcp（WG 使命：skills 作为一等原语 / SEP-2640 / 与 agentskills.io 对齐）
> - https://agentskills.io/（Agent Skills 开放标准、客户端展示列表）
> - https://a2a-protocol.org/latest/（A2A 与 MCP 互补："not a replacement for MCP"；MCP=agent-tool，A2A=agent-agent）
> - https://code.claude.com/docs/en/skills（disable-model-invocation、自定义命令合并进 Skills）

---

## ③ 未来规划（对应维度 G）

### 战略定位：以「AI Agent + 开放协议」为主线，Skill 与 MCP 全面铺开

- **Skills 全产品线铺开**（官方 2025-10-16 发布，2025-12-18 更新）：
  - 覆盖 **Claude apps（Pro/Max/Team/Enterprise）→ Claude Code → API** 三端；API 新增 `/v1/skills` 端点（程序化管理自定义 skill 版本），Skills 需要 **Code Execution Tool** beta 沙箱运行。
  - 官方在研方向（原文 "What's next"）：简化 skill 创建流程 + **企业级部署 / 分发能力**（organization-wide deployment，跨团队分发）。
  - **Agent Skills 已发布为开放标准**（agentskills.io，2025-12-18 与 org-wide management、directory 同步公开），"build once, use across any skills-compatible agent"；生态采用方已远超 Anthropic（OpenCode、Cursor、Copilot、Gemini CLI、TRAE 等）。
- **MCP 治理开放化 / 多厂商化**：
  - MCP 是 MIT 协议、`modelcontextprotocol` 组织维护、有 `GOVERNANCE.md` / `MAINTAINERS.md` 与 SEP（specification enhancement proposal）提案流程；**Working Group 成员跨厂商**（Anthropic / Google / AWS / GitHub / Databricks / Bloomberg 等），2026 年内从 Interest Group 升级为 Working Group，进入活跃标准化阶段。
  - **Skills Over MCP WG** 目标："interoperable skill distribution across MCP servers and clients"（跨 MCP server / client 的技能互操作分发）—— 这是 MCP 路线图里最贴近「命令 / 技能生态」的前瞻项。
  - **关于「MCP 捐赠 / 加入 Linux Foundation」：未能核实（见下方标注）**；可核实的是 MCP 已采用开放式多厂商治理，且同类协议 **A2A 已明确由 Google 捐赠给 Linux Foundation**（官方站点原文），由 TSC（AWS / Cisco / Google / IBM / Microsoft / Salesforce / SAP / ServiceNow）治理——说明「协议中立治理」是该领域的确定趋势。
- **A2A（Agent2Agent）趋势对照**：
  - A2A 官方明确定位 **A2A 与 MCP 互补**："MCP standardizes agent-to-agent [sic: agent-to-tool] communication; A2A standardizes agent-to-agent communication. They are complementary… **Not a replacement for MCP**"。
  - 对「确定性命令」的含义：MCP / A2A 都在解决「AI 生态的互操作」，均以 **agent 为消费主体**；没有任何官方文本把「人类显式命令 / 确定性触发」列为替代对象——确定性命令属于 AI 生态之外的「既有人类交互层」，被当作保留的基础设施。
- **对「确定性 vs AI」的整体判断**：Anthropic 的公开路线图**没有**「用 AI 取代确定性命令触发」的表述；其叙事是「AI Skill 做业务主体 + 保留确定性底座（内置命令 / MCP 协议 / hooks）+ 用 `disable-model-invocation` 把用户显式触发作为副作用护栏」。即 **确定性命令与 AI Skill 在 Claude 生态中长期并存互补**；且「用户显式选命令 + 传参」的 UX 本身正在被 MCP Prompts / Skills 继承（并入 `/` 菜单），说明这一交互范式有生命力，只是 Claude 侧的执行终端是 LLM。

### 需要标注的「未能核实 / 网络受限未访问」项

1. **「MCP 捐赠 / 加入 Linux Foundation」官方公告**：本调研尝试访问 anthropic.com 与 linuxfoundation.org 相关 press 页面（多个疑似 URL），均返回 404，**未能核实**该具体事实与时间；本报告仅记录可核实的多厂商治理现状（MIT + GOVERNANCE.md + 跨厂商 WG）与 A2A 的 Linux Foundation 捐赠（a2a-protocol.org 官方原文可核实）。
2. **Anthropic 官方 roadmap / 战略页**：未获取到公开的、带时间表的 Claude Code / MCP 路线图页面；上文「未来规划」判断全部基于可访问的官方文档原文（"What's next" 表述、Skills Over MCP WG 在研 SEP、A2A 官方互补定位），无远期承诺性内容。
3. **agentskills.io 客户端展示列表**：获取自官网首页（含 OpenCode、Cursor、Copilot、Gemini CLI、TRAE 等数十家），但未逐一核实各厂商对标准的支持深度。
4. 未做独立数据验证（MCP server 数量、采用率等第三方统计）。

---

## 小结（对「确定性命令触发」方向的判断）

- **Claude Code 保留确定性命令通道，但只限于「CLI 内置命令」与「MCP/Prompt 枚举」**：内置命令行为被编码进 CLI、不经 LLM；Skill / 自定义命令 / MCP Prompts 即使由用户 `/name` 显式选中并传参，执行仍交给 LLM——**「用户显式触发」这一环是确定性的，但「执行主体」仍是 AI**，与本方案「全程无 LLM」只在最后一跳分界。
- **Skill 与 MCP 能力非常强**：Skill（SKILL.md，Agent Skills 开放标准，渐进式披露）+ MCP（tools / resources / prompts 原语 + stdio / HTTP 传输 + 开放多厂商治理）构成 Claude 生态两大支柱；`disable-model-invocation` 是官方把「用户显式触发」设为副作用护栏的最直接证据。
- **与确定性命令的关系 = 并存 + 互补，非替代**：官方没有任何「AI 消灭确定性命令」的表述；确定性被定位为「CLI 交互底座 / 协议执行层」，AI 被推为「业务执行主体」，用户显式触发被当成护栏机制保留。
- **对本方案的启示**：① MCP Prompts 命令（`/` 选择 → 空格传参 → 确定性路由到 server）已验证「命令列表 + 显式参数」的交互有生态价值，但其执行终端是 LLM——**本方案「无 LLM 直达本地 HTTPServer」恰是其未覆盖的空档**；② MCP 的 `tools/list` / `prompts/list` 可作为本方案命令列表的协议参照；③ 即便最 AI 化的厂商也保留确定性底座并认可人类显式触发，从侧面印证确定性命令通道具有长期存在价值。


