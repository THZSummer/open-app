# Microsoft Copilot Studio 竞品调研报告

> 调研主题：IM 内 `/` 唤起命令 → 选中命令 → 输入参数 → 触发用户本地 HTTPServer 的「确定性命令触发」（全程无 AI/LLM 参与）——以「不同类型产品」（AI Agent 平台）为对照，验证确定性命令 vs AI 触发是替代、互补还是并存
> 调研日期：2026-09-08
> 信源：全部引用微软官方文档（learn.microsoft.com：Microsoft Copilot Studio / Microsoft 365 Copilot extensibility / Microsoft 365 Agents SDK / AI at Work roadmap），未核实内容已明确标注

---

## 【产品描述】

- **归属**：Microsoft（微软），属 **Power Platform / Microsoft 365 Copilot 生态**（官方文档归属 `ms.service: copilot-studio`，与 Power Platform、Dynamics 365、Microsoft 365 Copilot 深度联动）。
- **形态**：**低代码 AI Agent 构建平台**（graphical, low-code studio）——构建 AI 驱动的 Agents 与 Workflows，发布到 Teams、Microsoft 365 Copilot、网站、移动端等渠道。
- **定位**：让业务人员（低代码）与专业开发者（Pro-code）都能构建、编排、管理企业级 AI Agent（官方原话：*"design a complete business solution … without switching tools"*）。
- **目标用户**：企业内的业务制作人（maker）、低代码开发者、专业开发者（Pro-code / SDK）、IT 管理与治理人员。
- **不同类型分类**：属于「**不同类型**」对照产品——Copilot Studio 是**纯 AI 驱动的 Agent 平台**，核心交互由 LLM 理解自然语言、决策调用工具完成，与本方案（无 AI、纯程序化确定性传递）互为对立面，用于判断「确定性命令 vs AI 触发」的关系。

> **一句话定位**：Copilot Studio 是微软「一切皆 AI Agent」路线的落地平台；它不是、也不打算成为「确定性命令触发」工具。

---

## ① 非 AI 本地 Command 支持（对应维度 A~D）

**结论先行：Copilot Studio 自身不存在「非 AI 确定性命令」概念，也没有 `/` 唤起命令列表、结构化参数、触发本地 HTTP Server 这类链路。平台默认交互是「自然语言对话 → LLM 意图识别 → 编排选择 Topic/工具」。**

### A. 命令入口与唤起形态

- **无 `/` 命令列表概念**：官方文档中不存在 "slash command / command palette / command menu" 这一产品概念。所有构建物（Agents / Workflows / Agent flows）都通过**对话或事件**被唤起，不是通过 `/` 枚举命令。
- **唯一带 `/` 的「命令」是消息文本**：官方 Teams 部署指南给出的示例——用户在聊天里输入 `/debug clearstate` 来重置 agent 会话状态。这**不是客户端 slash 命令**，而是作为普通消息文本发给 bot，由 bot 的对话逻辑处理（原文：*"Add messaging that informs users that they can type a specific command … This command forces a complete conversation reset"*）。
- **唤起形态 = 对话 / @提及**：发布到 Teams 的 agent 以 bot 身份出现在左侧 agent 列表，用户直接对话；在 Microsoft 365 Copilot 中用 `@` 选择 agent 再提问（官方发布文档明述）。
- **清单来源**：无命令清单；agent 的能力由 maker 在 studio 中配置（Instructions / Knowledge / Tools），运行期由编排器决定何时使用。

### B. 参数输入

- **无结构化参数表单**：工具的输入默认由 **AI 动态填充**（"Dynamically fill with AI"）——orchestrator 从对话上下文抽取，抽不到就生成问题向用户索要（官方 `add-tools-custom-agent` 文档明述）。参数收集是**对话式**的，非表单/Modal。
- **唯一接近「声明参数」的机制**：工具配置页有 Inputs 区（名称、描述、识别为字符串或实体、重试逻辑、校验），但**运行时仍由 LLM 填值**；可改为 `Custom value`（变量 / Power Fx）固定覆盖。这不是用户侧结构化输入。
- **MCP / REST / Connector 工具**：通过 OpenAPI/MCP 描述暴露工具的 name/description/inputs/outputs，但这些描述是**给 LLM 选择工具用**的（官方原话：*"Descriptions to help the language model determine when to invoke the API"*），不是给用户填参数用的。

### C. 命令注册与下发

- **维护方 = Maker（制作人）**：在 Copilot Studio 中通过低代码画布（topics / flows / tools）配置，发布后由**云端 agent 运行时**承载；没有「第三方注册命令到客户端」的下发机制。
- **发现机制**：用户靠自然语言提问；agent 的 Conversation Start 话题（Greeting）提供引导；Microsoft 365 Copilot 端通过 @ 提及、建议 prompt 发现 agent。**无命令清单索引/搜索**。

### D. 触发到本地的链路

- **无「触发到用户本地 HTTP Server」链路**：所有工具执行都在**云端 agent 运行时**（Copilot Studio / Azure / Microsoft 365）内完成，通过 Connector、REST API、MCP server 等**公网可达**的服务调用；官方文档未提供任何「本机/内网/本地服务」接收命令的机制。
- **认证**：工具用 **End-user 凭据**（用户授权）或 **Maker-provided 凭据**（制作人预置），与本地鉴权无关。
- **与 Teams 确定性入口的关系**：Copilot Studio 发布文档只描述「把 agent 变成 Teams 里的会话式 bot / 出现在 app store / 加入团队频道 @ 提及」，**未提及复用 Teams 原生 slash command 或 message extension 作为 agent 的确定性入口**（详见 ② F 的「未能核实」标注）。

> 信源：
> - https://learn.microsoft.com/en-us/microsoft-copilot-studio/fundamentals-what-is-copilot-studio（产品定位：AI agents & workflows）
> - https://learn.microsoft.com/en-us/microsoft-copilot-studio/harnesses-overview（三种 harness）
> - https://learn.microsoft.com/en-us/microsoft-copilot-studio/publication-add-bot-to-microsoft-teams（发布到 Teams：bot/对话/@ 提及，无命令菜单）
> - https://learn.microsoft.com/en-us/microsoft-copilot-studio/guidance/deploy-agent-teams（`/debug clearstate` 作为消息文本处理）
> - https://learn.microsoft.com/en-us/microsoft-copilot-studio/add-tools-custom-agent（工具输入默认 "Dynamically fill with AI"、对话式收集参数）

---

## ② AI Agent Skill 支持（对应维度 E~F）

### E. 确定性 vs AI：纯 AI 驱动，工具调用成熟度高

**Copilot Studio 是完全 AI 驱动的平台**——官方核心表述：agent *"reasons through a request and deciding the best next step based on its instructions and context"*；遵循指令、基于知识源、用工具行动。对本方案（纯确定性）而言，Copilot Studio 是**完全的对立面**。

AI Agent 构建能力成熟度：

| 能力 | 形态 | 官方要点 |
|------|------|---------|
| **Agents（三种 harness）** | GitHub Copilot harness（主推，推理重、多步工作，自然语言生成 agent 配置）/ standard harness（基于规则、topics、结构化对话）/ Copilot chat harness（扩展 Microsoft 365 Copilot Chat） | 创建 agent 时选择 harness，GitHub Copilot 与 standard 两种 harness 互不可转换 |
| **编排（orchestration）** | **generative orchestration（默认）**：agent 自动选 Topic / 工具 / 检索知识 | classic 模式（关编排）则只用 topics；编排器依据工具名/描述/对话上下文/意图/输入输出/历史工具使用选择 |
| **工具（Tools）** | Connector（Power Platform 预置/自定义，数千连接器）、Agent flow、Prompt、REST API（OpenAPI）、**MCP server**、Computer use（操作 GUI）、Azure Bot Service skills、Client tools（向客户端发事件让其执行动作） | 最多 **128 个工具**/agent（官方建议 25–30 个）；工具可显式在 topic 中调用 |
| **知识库（Knowledge）** | 公共网站、上传文档（Dataverse）、SharePoint、Dataverse、企业数据连接器；`Use information from the web`（Bing grounding）；租户图语义检索 | 支持引用（citation）、权威源标记；知识源支持用户级 Entra ID 鉴权 |
| **Workflows / Agent flows** | 可视化自动化流程（GitHub Copilot harness 的 workflows 官方称 **"deterministic … same input always produces the same output"**；standard harness 的 agent flows 类 Power Automate） | 注意：官方所称 "deterministic" 指**流程执行路径固定**，但仍在 AI 平台内、由 harness 运行时驱动、按 action 计费，**不是 IM slash 命令通道** |
| **Pro-code / 自定义引擎（Custom Engine Agent）** | Microsoft 365 Copilot extensibility 明确两种 agent：**declarative agent**（用 Copilot 编排与模型）+ **custom engine agent**（自带编排与模型，可托管 Azure，支持团队协作、主动消息、外部渠道）；Microsoft 365 Agents SDK（.NET/JS/Python，AI-agnostic）用于构建跨渠道会话 agent | SDK 是「通道抽象层」，本身不含 AI、不含编排、不含低代码 |

- **对「机器可调用通道」的结论**：Copilot Studio 是**最成熟的 AI 工具调用平台之一**（MCP、REST、Connector、Function calling 式编排、A2A 多 agent 协作、Work IQ API 均支持），但**所有触发都经由 LLM 决策**——没有「无 LLM 的确定性命令」旁路。

> 信源：
> - https://learn.microsoft.com/en-us/microsoft-copilot-studio/harnesses-overview
> - https://learn.microsoft.com/en-us/microsoft-copilot-studio/agents-experience/overview（GitHub Copilot harness：自然语言优先、增强编排）
> - https://learn.microsoft.com/en-us/microsoft-copilot-studio/add-tools-custom-agent（工具机制、128 上限、AI 填参数）
> - https://learn.microsoft.com/en-us/microsoft-copilot-studio/advanced-connectors（Connector 工具）
> - https://learn.microsoft.com/en-us/microsoft-copilot-studio/agent-extend-action-rest-api（REST API/OpenAPI 工具，描述给 LLM 用）
> - https://learn.microsoft.com/en-us/microsoft-copilot-studio/agent-extend-action-mcp（MCP，需开启 generative orchestration）
> - https://learn.microsoft.com/en-us/microsoft-copilot-studio/knowledge-copilot-studio（知识库）
> - https://learn.microsoft.com/en-us/microsoft-copilot-studio/workflows-experience/flows-overview（workflows 官方称 deterministic）
> - https://learn.microsoft.com/en-us/microsoft-365/copilot/extensibility/agents-overview（declarative vs custom engine agent）
> - https://learn.microsoft.com/en-us/microsoft-365/agents-sdk/agents-sdk-overview（Agents SDK：不含 AI/编排的低层通道抽象）

### F. 与 Teams 确定性命令的关系：Agent 作为「取代手工命令入口」的形态存在

- **Copilot Studio 不提供与 Teams 确定性命令的复用**：发布到 Teams 的官方路径是「把 agent 变成 Teams 应用（bot）→ 对话 / @提及 / 加入频道」，全程是**会话式 AI 交互**；官方文档中**未出现**「agent 复用 Teams slash command / message extension / task module」之类的说明。
- **Agent 取代的是「手工入口」**：微软的叙事是用户「用自然语言告诉 agent 要做什么」，agent 自行选工具执行——即用 AI 对话**取代**了显式命令式交互（如输入 `/命令` + 参数）；`/debug clearstate` 这类斜杠文本也只是被当作消息内容消费。
- **与 Teams 平台命令的关系（未能核实项）**：Teams 平台自身仍保留原生确定性能力（slash command、message extension、task modules 等，属 Teams 平台能力，另见 07-teams 调研报告），**但 Copilot Studio 官方文档未说明 agent 与这些入口是否打通或并存**——本报告对「agent 是否可作为 Teams 消息扩展/命令的一部分发布」标注为**未能核实**。
- **并存 vs 替代判断**：在 Copilot Studio 产品边界内，微软的立场是 **AI 对话取代显式命令**（未提供确定性命令通道供并存）；而在更宽的 Teams 平台层面，确定性入口与 AI bot 理论上可共存于同一 Teams 应用，但 Copilot Studio 官方未对此提供集成文档。

> 信源：
> - https://learn.microsoft.com/en-us/microsoft-copilot-studio/publication-add-bot-to-microsoft-teams
> - https://learn.microsoft.com/en-us/microsoft-copilot-studio/guidance/deploy-agent-teams
> - 未能核实：Copilot Studio agent 与 Teams message extension / 客户端 slash command 的集成是否存在（官方文档未见相关说明）

---

## ③ 未来规划（对应维度 G）

**结论先行：微软对 Copilot Studio / 自定义引擎 Agent 的战略全面 AI-agent-first，无任何「保留/主推非 AI 确定性命令通道」的路线图表述。**

### 微软战略定位与路线图信号（均有官方信源）

1. **Harness 演进 = AI 化的方向信号**：GitHub Copilot harness 是 "redesigned authoring and runtime"，主打自然语言生成 agent、深度推理、文件处理、skills/memory，用 Copilot Credits 计费；standard harness（topics/规则）仍受支持，但被描述为 "dependable option for rule-based … well-understood requests"。官方明示两种 harness 互不可迁移，新形态明显向推理型 AI 倾斜。
2. **纯自然语言构建（preview）**：描述业务目标 → Copilot Studio 自动生成「agents + workflows 组合」并迭代——进一步把「配置」本身 AI 化。
3. **Roadmap 载体迁移**：微软 Dynamics 365 / Power Platform / Dataverse 的 Release Planner 将于 **2026-11-15 退役**，路线图迁至 **"AI at Work" roadmap**（aka.ms/AIatWorkroadmap）——路线图整体重组为 AI 主题，官方页面本身即以 "AI for work" 命名。
4. **AI at Work 路线图近期公告（2026 上半年）**：Copilot Cowork GA（对话→执行，"conversation to action"）、**Work IQ API**（对 M365 智能层提供 A2A / MCP / REST 编程访问）、**Microsoft Agent 365**（agent 治理控制面）GA、**Microsoft Scout**（本地桌面 AI agent 应用，含 "assistant desktop app" 在本地/网页执行工作）、实时语音 agent（Dynamics 365 Contact Center 集成）、多模型（OpenAI GPT-5.x + Anthropic Claude Opus 系列在 Copilot Studio 可选）——**全部围绕 AI agent 能力扩展，无确定性命令方向**。
5. **对「本地执行」的投入是 AI 化而非确定性命令**：Microsoft Scout（本地桌面 app）是「AI 在用户本地文件与网页上自主工作」，与本方案「用户在 IM 里显式选命令、程序化触发本地 HTTPServer」是**两条相反路线**——微软选择的是 AI 自主本地执行，不是确定性命令直达本地。

### 综合判断

- 微软**没有**在 Copilot Studio 产品内提供「非 AI 确定性命令」这条通道，也**没有**任何官方路线图显示会补上它。
- 微软对「确定性」的保留仅体现在 **standard harness 的 topics / workflows 的固定执行路径**——即「确定性流程执行」仍被保留，但**入口与触发**一律走 AI 对话/事件编排，用户侧不存在命令枚举。
- 对 open-app 的启示（对照）：Copilot Studio 代表「AI 全面接管触发」的极端；它不否定「确定性命令」的工程价值（连微软的 workflows 都强调 deterministic execution），但证明**头部厂商不把「显式命令触发」作为终端用户主入口**——确定性命令若要成立，价值点在于**可预期、零成本、无幻觉**的强约束场景，而非与 AI 平台争抢「智能触发」。

> 信源：
> - https://learn.microsoft.com/en-us/microsoft-copilot-studio/harnesses-overview
> - https://learn.microsoft.com/en-us/microsoft-copilot-studio/agents-experience/overview
> - https://learn.microsoft.com/en-us/microsoft-copilot-studio/fundamentals-what-is-copilot-studio（Build with natural language preview）
> - https://aka.ms/AIatWorkroadmap（微软官方 "AI at work" roadmap，2026 上半年公告：Copilot Cowork / Work IQ API / Agent 365 / Scout / 实时语音 agent / 多模型）
> - https://learn.microsoft.com/en-us/microsoft-365/copilot/extensibility/overview（Work IQ API、Copilot API 服务：Retrieval / Search / Chat / Interaction Export / Meeting Insights）

---

## 未能核实 / 未访问项

1. **Copilot Studio agent 与 Teams 原生确定性入口（slash command / message extension）的集成**：微软官方 Copilot Studio 文档未见相关说明，标注**未能核实**；本报告仅确认发布路径为「会话式 bot + 对话/@提及」。
2. **微软官方对「是否永久保留 standard harness / 是否允许纯无 LLM 部署」的远期承诺**：官方仅表示 standard harness 仍受支持（与 GitHub Copilot harness 并存），未给出远期时间表，标注**未能核实**。
3. **「What's new in Copilot Studio」官方页面**：本次通过官方 fwlink 仅获取到只读占位页（内容为空），未核实到逐条更新；未来规划判断以 harnesses / agents-experience / AI at Work roadmap 等可访问官方页面为准。
4. 本报告未做用量/市场份额等数据层面的二次验证。
