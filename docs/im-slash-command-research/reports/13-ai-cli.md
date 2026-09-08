# GitHub Copilot CLI / AI CLI（GitHub）竞品调研报告

> 调研主题：IM 内 `/` 唤起命令 → 选中命令 → 输入参数 → 触发用户本地 HTTPServer 的「确定性命令触发」（全程无 AI/LLM 参与）
> 调研日期：2026-09-08
> 信源：docs.github.com（GitHub 官方文档：About GitHub Copilot CLI / Using / Command reference / About agent skills / Comparing CLI features / ACP server）、github.com/github/copilot-cli（官方仓库 README 与 changelog.md）。GitHub Blog 公告页与正式带日期路线图页访问均返回 404，已在文中明确标注「未能核实」。

---

## 【产品描述】

- **归属**：GitHub（微软）。对象为 **GitHub Copilot CLI**（终端版 Copilot coding agent），及其背后的 **GitHub Copilot coding agent / Copilot 云 Agent** 产品线。
- **形态**：**终端型 AI 编程 Agent**，三种使用形态：① 交互式 TUI（`copilot`，输入自然语言、`/` 唤起命令菜单、工具审批）；② 程序化单发（`copilot -p "prompt"`，完成即退出）；③ **ACP（Agent Client Protocol）Server**（`copilot --acp`，把 CLI 作为 agent 暴露给 IDE / CI / 多 Agent 系统）。
- **定位**：官方自述 —— “brings the power of GitHub Copilot coding agent directly to your terminal”（把 Copilot 编程 Agent 的能力直接带到终端）；官方仓库 README 原文“Powered by the same agentic harness as GitHub's Copilot coding agent”（与 Copilot 编程 Agent 同一套 agentic 引擎）。
- **目标用户**：终端开发者 / 个人到企业团队（Copilot Business / Enterprise，受组织、企业策略控制）。
- **同类型分类**：**「不同类型」对照产品（AI CLI，本方案的对立面）** —— 产品本体是 **AI 驱动**（自然语言 → LLM 理解意图 → 决策生成/执行 shell 命令），与本方案「确定性命令触发（无 LLM，纯程序化传递）」形成方向性对照，用于验证「确定性命令 vs AI 触发」是替代、互补还是并存。

---
## ① 非 AI 本地 Command 支持（对应维度 A~D）

**结论先行：Copilot CLI 有非常庞大的 `/` 斜杠命令体系，且大部分是纯确定性命令**（行为编码进 CLI、不经 LLM，如 `/help` `/exit` `/login` `/model` `/settings` `/mcp` `/permissions` `/context` `/usage`），**少部分是 AI 驱动命令**（把意图/任务交给 LLM 或启动 agent 子任务，如 `/ask` `/plan` `/refine` `/review` `/research` `/delegate` `/autopilot`）。官方 ACP 文档给出最清晰的判定口径：“informational commands such as `/usage` or `/context` return their output without invoking the model, while action commands such as `/plan` or `/review` start the corresponding agent task. Either way, the command text is not sent to the model as a question”。但**不存在「用户显式选命令 + 结构化参数 schema → 纯程序化直达本地 HTTPServer」的确定性业务旁路**——用户可扩展的命令面（Skills / 自定义 Agent / Plugins）**全部是 AI 驱动**；确定性命令全部是 CLI 自身控制 / 自我管理命令，不是用户注册的业务命令。**最接近确定性旁路的是 `!command`（绕开模型的本地 shell 执行）与 `$`（交还交互式 shell），以及 ACP 上“命令文本不发给模型”的确定性命令**。详析如下。

### A. 命令入口与唤起

- **`/` 唤起命令菜单**：交互会话中输入 `/` 弹出命令列表，可继续键入过滤；支持大小写不敏感匹配、别名（`/models`=`/model`、`/quit`=`/exit`、`/yolo`=`/allow-all`）、Tab 补全命令与参数、参数提示（argument hints）、错拼近似建议；`/help` 列出全部命令，`?` 打开快速帮助。
- **命令分两类（官方口径）**：
  1. **确定性内置命令（coded into CLI，不调模型）**：`/help` `/exit` `/login` `/logout` `/model` `/settings` `/mcp` `/plugin` `/skills` `/permissions` `/sandbox` `/context` `/usage` `/env` `/version` `/update` `/cwd` `/add-dir` `/copy` `/diff` `/session` `/resume` `/user` `/theme` `/voice` `/experimental` `/feedback` `/restart` `/share` `/worktree` `/init` `/instructions` `/lsp` `/limits` 等**数十个**。它们执行固定 CLI 逻辑（配置、上下文、权限、会话、认证），**不依赖 LLM 决策**。
  2. **AI 驱动命令（启动 LLM 回合或 agent 子任务）**：`/ask QUESTION`（侧问不记历史）、`/plan`（生成实现计划）、`/refine`（重写 prompt）、`/review` `/security-review` `/rubber-duck` `/research`（委托内置 agent）、`/diagnose`（分析日志）、`/delegate`（AI 生成 PR）、`/autopilot` `/goal`（agent 自主模式）、`/pr auto|automerge`（AI 驱动 PR）、`/fleet`（并行子 agent）、`/changelog summarize`（AI 摘要）、`/compact`（AI 压缩上下文）。**Skill 以 `/skill-name` 调用、自定义 Agent 以 `/agent` 选择，也属 AI 驱动**。
- **用户显式选命令的确定性旁路 —— 分档**：
  - **完全确定性**：内置 CLI 控制命令（固定集，不可用户扩展）。
  - **用户显式选择 + 传参，但执行交给 AI**：Skill / 自定义 Agent 被用户 `/name` 显式选中并带参调用（这一环确定、不经 LLM 理解意图），但 SKILL.md / agent profile 作为指令注入 agent 上下文，由 **LLM 编排工具执行** —— 执行环节仍是 AI。
  - **结论**：Copilot CLI 里“用户选命令 + 结构化参数 → 无 LLM 直达本地服务”的完整确定性业务通道**不存在**；确定性只存在于「CLI 自身控制命令」和「`!`/`$` 本地 shell 透传」。

> 信源：
> - https://docs.github.com/en/copilot/concepts/agents/about-copilot-cli （About GitHub Copilot CLI：交互/程序化模式、plan mode、允许工具、沙箱、MCP/Skills/Hooks/Custom agents/Memory、自定义 provider）
> - https://docs.github.com/en/copilot/reference/copilot-cli-reference/cli-command-reference （命令参考：全部 slash 命令、快捷键、`!`/`$`/`@`/`#`）
> - https://docs.github.com/en/copilot/reference/copilot-cli-reference/acp-server （ACP：确定性 vs AI 命令的官方判定、available_commands_update）
> - https://github.com/github/copilot-cli （官方仓库 README：默认模型、/login、/model、experimental）

### B. 参数输入

- **参数 = 自由文本 / 位置参数**，命令后空格接参：`/cwd PATH`、`/model MODEL`、`/skills info NAME`、`/every 1h run tests`、`/settings KEY VALUE`、`/add-dir /path`、`/compact focus on the auth module`。**无结构化表单 / JSON Schema 类型校验**（区别于 Discord/Slack 的 options / autocomplete）；斜杠命令可 mid-input 出现，多个 Skill 可在一条消息内连续调用。
- **MCP 工具参数 = JSON Schema（给 LLM 的工具契约，非用户表单）**：MCP server 用 `inputSchema` 声明参数供模型正确填参调用 `tools/call`，不是用户填写的表单。
- **最接近“命令 + 参数声明”的协议形态 —— ACP `available_commands_update`**：ACP server 向客户端推送命令清单，每条含 `name`（无前导 `/`）、`description`、可选 `input.hint`（描述命令参数）——“This advertised list is the authoritative, always-current set of commands you can run over ACP, and clients typically surface it in a command menu.” —— **即“命令列表 + 参数提示 + 路由执行”的交互范式在 ACP 层已存在**，但其执行端是 AI agent（action 命令启动 agent 任务），只有信息命令才是纯确定性。
- **例外：`/mcp add` 用表单式交互**（Tab 切换字段、Ctrl+S 保存），是少数“结构化填参”命令，但仍属 CLI 配置命令。

> 信源：
> - https://docs.github.com/en/copilot/reference/copilot-cli-reference/cli-command-reference （命令参数写法、Tab 补全、`/settings KEY VALUE`、mid-input 斜杠命令）
> - https://docs.github.com/en/copilot/reference/copilot-cli-reference/acp-server （available_commands_update：name/description/input.hint、命令菜单消费方）
> - https://docs.github.com/en/copilot/how-tos/copilot-cli/customize-copilot/add-skills （Skill 调用、/skills 子命令）

### C. 命令注册与下发

- **命令清单组成** = 内置固定集（编译进 CLI）+ **插件 / 扩展 / Skill 动态加入**（`/skills reload` 实时生效；ACP 通过 `available_commands_update` 全量推送最新清单，作为权威实时集合）。
- **用户可扩展的注册机制全部面向 AI**：
  - **Skills**：`SKILL.md`（YAML frontmatter：name / description）+ 可选脚本，放 `.github/skills`、`.claude/skills`、`.agents/skills`（项目级）或 `~/.copilot/skills`、`~/.agents/skills`（个人级）；命令名即 Skill 名（`/skill-name`）；`allowed-tools` 可预审批工具。
  - **自定义 Agent**：Markdown agent profile（user `~/.copilot/agents` / repo `.github/agents` / 企业 `.github-private`），`infer` 可让 LLM 自动委托。
  - **Plugins**：可打包 Skills + Agents + Hooks + MCP server 的安装包，支持 marketplace / GitHub 仓库安装；**无“第三方注册确定性业务命令 + 命令市场”的概念**。
- **命令发现**：`/` 菜单、`/help`、`/skills list`、`copilot plugins list`（机器可读 JSON 枚举 plugin/mcp/skill/instruction/lsp）、ACP `available_commands_update`。

> 信源：
> - https://docs.github.com/en/copilot/concepts/agents/about-agent-skills （Skills 位置、开放标准）
> - https://docs.github.com/en/copilot/how-tos/copilot-cli/customize-copilot/add-skills （/skills 子命令、SKILL.md、gh skill）
> - https://docs.github.com/en/copilot/concepts/agents/copilot-cli/comparing-cli-features （Custom instructions / Skills / Tools / MCP / Hooks / Subagents / Custom agents / Plugins 定位矩阵）
> - https://docs.github.com/en/copilot/reference/copilot-cli-reference/cli-command-reference （copilot plugins list --json、/skills reload）

### D. 触发到本地的链路

- **官方提供的确定性本地通道**：
  - **`!command`** —— “Execute a command in your local shell, **bypassing Copilot**”（绕开模型直接本地执行）；`!` 单独回车进入 shell 模式连续执行。
  - **`$`** —— 交还真实交互式 shell（`$SHELL`）。
  - **Hooks** —— 会话生命周期 / 工具调用前后**确定性执行用户 shell 脚本**（`preToolUse` / `postToolUse` / `sessionStart` / `sessionEnd` / `userPromptSubmitted` / `errorOccurred` 等），官方定位为 guardrails / 策略 / 观测，可阻断工具调用。
  - **MCP server**（本地 stdio / 远程 http，如 `copilot mcp add --transport http sentry <URL>`）—— 与本地/外部服务的连接靠 MCP，但**“谁决定调用”是 LLM**（`tools/call` 由模型决策触发），执行一跳才确定性。
- **“直达用户本地 HTTPServer”在本产品中的语义**：Copilot CLI 作为 MCP host，把 LLM 决策的 `tools/call` 确定性转发给 server —— **“执行”确定，“决策”是 AI**；与本方案“用户自己决策、全程无 LLM”仅在最后一跳等价。
- **结论**：不存在“用户 → 命令 → 结构化参数 → 本地 HTTPServer”的产品形态；本地链路要么是原始 shell 透传（`!`/`$`），要么是 AI 决策的 MCP 调用。

> 信源：
> - https://docs.github.com/en/copilot/reference/copilot-cli-reference/cli-command-reference （`!`/`$` 快捷键语义）
> - https://docs.github.com/en/copilot/concepts/agents/about-copilot-cli （MCP 内置 GitHub server、工具审批、沙箱）
> - https://docs.github.com/en/copilot/how-tos/copilot-cli/use-copilot-cli/overview （添加 MCP server、`!` shell 命令）
> - https://docs.github.com/en/copilot/concepts/agents/copilot-cli/comparing-cli-features （Hooks 生命周期表、MCP 定位）

---

## ② AI Agent Skill 支持（对应维度 E~F）

### E. 纯 AI 驱动（本方案的对立面）—— 高，且是产品本体的核心，成熟度很高

- **Agent 模式齐全**：默认 ask/execute（自然语言 → 生成/执行 shell 命令，逐条工具审批）、**plan mode**（先出结构化实现计划再写码，Shift+Tab 切换）、**autopilot mode**（experimental，agent 自主多步直到完成，`/goal OBJECTIVE`、`--max-ai-credits` 限额）。官方“Next steps”明确把 agentic 能力（delegate 任务 / 自定义 agent / steering / agentic code review）作为主推方向。
- **Subagent / 自定义 Agent**：6 个内置自定义 Agent（Explore / Task / General purpose / Code review / Research / Rubber duck），主 agent 可自动委托；用户可定义 agent profile（角色、允许工具、MCP 连接、`infer` 自动委托）。
- **MCP 支持**：内置 **GitHub MCP server**（默认带 github 工具集：issues/PR/actions 等）+ 自定义 MCP server（`copilot mcp add`，http/stdio，存 `~/.copilot/mcp-config.json`，`/mcp` 管理面板）；支持 org/enterprise 级 MCP 策略（allowlist / registry URL），已知个别 org 级策略 CLI 尚不支持。
- **Skills**：Agent Skills 开放标准（SKILL.md，跨多家 AI 系统），LLM 根据 name/description 自动加载或用户 `/skill-name` 手动调用；`allowed-tools` 预审批。
- **Hooks / Custom instructions / Memory**：Hooks 做确定性护栏与观测；Custom instructions（`AGENTS.md`、`.github/copilot-instructions.md`、path-specific instructions）；**Copilot Memory**（持久化仓库记忆）。
- **可扩展生态**：Plugins + marketplaces（可打包 skills/agents/hooks/MCP）、Agent Plugins / Open Plugin Spec、extensions（`com.github.copilot/extensions/`）、**ACP server**（开放协议，把 CLI 作为 agent 嵌入 IDE / CI/CD / 多 agent 系统，公测中）。
- **权限与安全**：trusted directories、工具审批三选项（Yes / Yes-and-approve-for-session / No+反馈）、`/permissions`（default / assisted / allow-all）、`--allow-all-tools` / `--deny-tool` / `--allow-tool`（含 `shell(git push)` 粒度、按 MCP server 名）、**本地沙箱**（限制文件/网络/系统能力，`/sandbox enable`）与**云沙箱**（整个会话在云端隔离环境运行，`copilot --cloud`，公测）。
- **模型与计费**：默认 Claude Sonnet 4.5，`/model` 可选多模型（Claude 系列、GPT-5 等，1M token 长上下文、可配置 reasoning effort）；支持自定义模型 provider（`COPILOT_PROVIDER_BASE_URL/TYPE/API_KEY/MODEL`，含本地 Ollama）；按 AI credits 计费。
- **程序化接口**：`copilot -p "prompt" --allow-tool=...`、stdin 管道、脚本输出选项管道；`copilot completion`、`copilot plugins list --json` 等机器可读面。

> 信源：
> - https://docs.github.com/en/copilot/concepts/agents/about-copilot-cli （模式、模型、自定义 provider、工具审批、沙箱、MCP/Skills/Hooks/Memory/ACP 全览）
> - https://docs.github.com/en/copilot/how-tos/copilot-cli/use-copilot-cli/overview （agent 模式、内置自定义 Agent 表、Skills/MCP/Hooks/自定义 agent 用法）
> - https://docs.github.com/en/copilot/concepts/agents/copilot-cli/comparing-cli-features （Custom instructions / Skills / Tools / MCP / Hooks / Subagents / Custom agents / Plugins 全矩阵）
> - https://docs.github.com/en/copilot/concepts/agents/about-agent-skills （Agent Skills 开放标准）
> - https://docs.github.com/en/copilot/reference/copilot-cli-reference/acp-server （ACP 公测、IDE/CI/多 Agent 用例）
> - https://github.com/github/copilot-cli （README：GitHub MCP server 默认、MCP-powered extensibility、experimental autopilot）

### F. 确定性命令 vs AI 生成命令的关系：**并存 + 分层，AI 是业务执行主体**

- **两类命令并存，官方从未表述“用 AI 消灭确定性命令”**：确定性命令管 **CLI 自身**（控制 / 配置 / 上下文 / 权限 / 会话 / 认证），AI 命令管 **业务执行**（写码、PR、计划、研究、审查、部署代理）；人类显式控制被刻意保留为安全层（工具审批、`/permissions`、沙箱）。
- **用户可扩展的命令面全部 AI 化**：Skills、自定义 Agent、Plugins 都是“注入指令给 LLM 或定义 agent persona”，没有一个可扩展通道能“用户选命令 + 结构化参数 → 无 LLM 本地执行”。这与 Claude Code 的 `disable-model-invocation`（保留用户显式触发）逻辑不同 —— Copilot CLI 没有把“用户显式触发”做成独立于 LLM 的执行通道。
- **确定性 `!`/`$`/Hooks 的定位**：是“人类绕开模型直接操作本机 / 程序化护栏”，不是“命令 → 参数 → 本地服务”的产品化通道。
- **对本方案的判定**：Copilot CLI 保留了确定性命令（CLI 底座）与 AI 命令（业务主体）**双通道并存，但业务侧只有 AI 通道**；「用户显式选命令 + 结构化参数、无 LLM 直达本地 HTTPServer」是它**未覆盖的空档**（其最接近形态 ACP `available_commands_update` + `input.hint`，执行端仍是 agent）。

> 信源：
> - https://docs.github.com/en/copilot/reference/copilot-cli-reference/acp-server （确定性/ AI 命令判定、available_commands_update、命令不发给模型）
> - https://docs.github.com/en/copilot/concepts/agents/copilot-cli/comparing-cli-features （各能力定位：Skills/MCP/Hooks 何时用）
> - https://docs.github.com/en/copilot/reference/copilot-cli-reference/cli-command-reference （内置命令全集：控制 vs agent 任务）

---

## ③ 未来规划（对应维度 G）

### 战略定位：全面 AI 化，GitHub Copilot coding agent 是主线，CLI 是其终端入口之一

- **官方定位表述（可核实）**：① About 文档——Copilot CLI 是“quick access to a powerful AI agent…working on your behalf”，两种 UI（交互 / 程序化）；② 仓库 README——“brings the power of GitHub Copilot coding agent directly to your terminal”，且“**We're still early in our journey, but with your feedback, we're rapidly iterating** to make the GitHub Copilot CLI the best possible companion in your terminal”；③ “Full control: Preview every action before execution — nothing happens without your explicit approval”（人类审批被作为底线护栏保留）。
- **演进方向全部围绕 AI agent 展开（changelog / 命令参考可核实）**：
  - **agent 自主度加深**：autopilot mode（experimental）、`/goal` 目标驱动 + AI credit 限额、`--plan --mode autopilot`、subagent 并行（`/fleet`）、任务调度（`/every` `/after`）。
  - **执行环境上云**：cloud sandbox（`copilot --cloud`，整个会话在云端隔离环境，公测）+ local sandboxing 成为安全基础设施。
  - **生态开放化**：Plugins + marketplace（Agent Plugins / Open Plugin Spec）、skills 开放标准、**ACP server**（把 CLI 作为 agent 嵌入 IDE / CI / 多 Agent 系统，公测）——即“让第三方把 Copilot CLI 当 agent 用”的协议化方向。
  - **平台打通**：`copilot app` 跳到 Copilot App、云 Agent 会话本地接管（`--resume`）、远程 steering（`/remote`）、Windows 任务栏/桌面集成等。
- **对“确定性命令通道”的未来取向**：官方没有任何“用 AI 取代 CLI 控制命令”的表述；相反工具审批、沙箱、Hooks、`/permissions` 全部在强化“人类显式控制 + 程序化护栏”。**即：CLI 自身的确定性命令与 AI 命令长期并存；但“用户注册业务命令并确定性执行”这条通道从未出现在产品叙事中，未来也未见其纳入路线的迹象。**
- **整体判断**：GitHub 的战略是“AI coding agent 为业务主体 + 确定性命令作为 CLI 基础设施 + 人类审批/沙箱作为护栏”，与本方案“确定性命令直达本地服务”**互补但不同层**；它对“确定性触发”的价值在于证明了“CLI 命令/审批/护栏”这类确定性交互会被长期保留，但业务动作本身被全面 AI 化。

### 需要标注的「未能核实 / 网络受限未访问」项

1. **GitHub Blog 关于 Copilot CLI 的发布公告 / 官方带日期的路线图页**：本调研尝试多个 github.blog 疑似 URL（changelog 公告、product-news 文章、blog 搜索页），均返回 404，**未能核实**具体发布日期、官方路线图时间表与“是否计划用 AI 取代确定性 CLI 命令”的公开表述；上文「未来规划」判断全部基于可访问的官方文档原文与官方仓库 README / changelog（无远期承诺性内容）。
2. **Copilot CLI 的采用量 / 用户规模 / 生态数量**：未获取到官方统计数据，未做第三方数据验证。
3. **extensions（`com.github.copilot/extensions/`）注册自定义 slash 命令的执行语义**：changelog 显示“Extension slash commands run their handler exactly once per invocation”“SDK clients can register custom slash commands”，但官方文档未见对“扩展命令是否为纯确定性执行”的明确说明，**未进一步核实**；不影响上文“用户可扩展命令面整体 AI 驱动”的结论。

---

## 小结（对「确定性命令触发」方向的判断）

- **“/ 命令”到底确定性还是 AI 驱动？—— 两者都有，且官方有明确分界**：Copilot CLI 的 `/` 斜杠命令中，**绝大多数是纯确定性命令**（行为编码进 CLI，如 `/help` `/exit` `/login` `/model` `/settings` `/mcp` `/context` `/usage`，官方 ACP 文档原文：信息类命令“return their output without invoking the model”）；**少数是 AI 驱动命令**（`/ask` `/plan` `/refine` `/review` `/research` `/delegate` `/autopilot` 等“start the corresponding agent task”），且**所有用户可扩展的命令（Skill / 自定义 Agent / Plugin）都是 AI 驱动**。调研计划 README（§4.1.2）初判“slash 命令仍由 AI 驱动”需据此修正为：**确定性命令大量存在，但只服务 CLI 自身控制；面向业务动作的命令面整体 AI 化**。
- **不存在本方案的确定性业务旁路**：Copilot CLI 没有“用户注册命令 + 参数 schema → 无 LLM 直达本地 HTTPServer”的通道；最接近的是 `!command` / `$`（原始 shell 透传）与 ACP `available_commands_update`（命令清单 + `input.hint`，但执行端是 agent）。
- **AI 能力极强且是产品核心**：ask/execute + plan + autopilot 三模式、subagents/自定义 Agent、内置 GitHub MCP server + 自定义 MCP、Agent Skills 开放标准、Hooks、Plugins/marketplace、ACP server、本地/云沙箱、自定义模型 provider，成熟度在 AI CLI 中属第一梯队。
- **与确定性命令的关系 = 并存 + 分层，非替代**：确定性命令保留为「CLI 基础设施 + 人类显式控制（审批/护栏）」，AI 被推为「业务执行主体」；官方没有“AI 消灭确定性命令”的表述，但也没有“确定性业务命令通道”的产品方向。
- **对本方案的启示**：① 即便最 AI 化的厂商（GitHub）也保留并长期强化确定性 CLI 命令、工具审批、Hooks、沙箱等“人类显式控制”机制 —— 从侧面印证“显式、可预期、受控的确定性触发”有长期价值；② Copilot CLI 的 ACP 命令清单（name/description/input.hint）验证了“命令列表 + 参数声明 + 路由执行”的交互价值，但其业务执行端是 LLM —— **本方案“无 LLM 直达本地 HTTPServer”恰是包括 Copilot CLI 在内的 AI 产品未覆盖的空档**，可作为差异化切入点。
