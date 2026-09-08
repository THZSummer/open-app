# IM Slash 命令直达本地服务调研

> **调研主题**: 在 IM 中通过 Slash 命令一键唤起命令列表、输入参数、直达用户本地 HTTPServer 的"确定性命令触发"模式，是否业务主流 / 主推 / 未来发展趋势
> **创建日期**: 2026-09-08
> **调研状态**: ✅ 调研完成 — 14 份产品报告 + 汇总报告已产出
> **分支**: `feature/im-slash-command-research`

---

## 📑 文档导航

> **阅读建议**：先读 [summary.md](summary.md)（整体对比汇总 + 结论建议）→ 再按需查看各产品报告。分类（同类型 IM / 不同类型 AI 对照）在各报告与汇总中均有标注。

| 文档 | 分类 | 说明 |
|------|:---:|------|
| [**整体对比汇总报告**](summary.md) | — | ★ 执行摘要 + 对标矩阵（对象×维度 A~G）+ 趋势判断 + 对 open-app 结论建议 |
| [01 · Slack](reports/01-slack.md) | 同类型 IM | 确定性 Slash 命令最成熟范本，双通道（Agents + MCP 双向） |
| [02 · Discord](reports/02-discord.md) | 同类型 IM | 纯确定性 Interactions，无原生 AI |
| [03 · Telegram](reports/03-telegram.md) | 同类型 IM | 纯确定性 Bot Commands，长轮询可本地 |
| [04 · 飞书 / Lark](reports/04-feishu-lark.md) | 同类型 IM | `/` 命令面板 + WebSocket 长连接本地可收 |
| [05 · 钉钉](reports/05-dingtalk.md) | 同类型 IM | 快捷指令 + Stream 反向连接「五零」本地 |
| [06 · 企业微信](reports/06-wecom.md) | 同类型 IM | 无标准 `/` 菜单，靠文本关键词 + 智能机器人长连接 |
| [07 · Microsoft Teams](reports/07-teams.md) | 同类型 IM | `/` autocomplete + manifest 强类型参数 + Dev Tunnels |
| [08 · Slack AI / Agentforce](reports/08-agentforce.md) | 不同类型 | AI 驱动，无确定性命令形态，确定性降维为内部执行层 |
| [09 · Copilot Studio](reports/09-copilot-studio.md) | 不同类型 | 纯 AI Agent 平台，AI 取代显式命令入口 |
| [10 · 飞书智能伙伴 / 钉钉 AI 助理](reports/10-ai-assistant.md) | 不同类型 | AI 助理，Aily 有 skill_id 确定性 API 旁路 |
| [11 · OpenAI Tool Calling](reports/11-openai-tool-calling.md) | 不同类型 | AI 工具调用标杆，无用户侧确定性通道 |
| [12 · Claude Code Skills / MCP](reports/12-claude-code-skills.md) | 不同类型 | 本方案对立面；MCP Prompts 命令可对标 |
| [13 · GitHub Copilot CLI](reports/13-ai-cli.md) | 不同类型 | `/` 命令混合（确定性+AI），业务确定性命令是空档 |
| [14 · n8n / Zapier / Make / Coze / Dify](reports/14-automation-ipaas.md) | 不同类型 | 确定性工作流 = AI 工具层，命令前端层空白 |

---

## 一、背景

### 1.1 系统上下文

本调研属于 **open-app**（企业通讯能力开放平台）的演进方向讨论。

- **open-app** 是 **XXX 通讯系统**（企业级统一通讯平台：IM / Meeting / CloudBox / Calendar / Bot 等）的**能力开放平台**，通过 API / 事件 / 回调 / 连接器四种形式将通讯能力开放给企业内业务应用（详见 `docs/XXX 通讯系统概述.md`）。
- 本次需求发生在 **IM 子系统** 场景内。

### 1.2 需求描述

在 IM 中，通过 **Slash 命令**（如输入 `/`）唤起一个**命令列表**，流程如下：

```
用户在 IM 输入 "/"（Slash）
   ↓
唤起 命令列表（弹出可选的命令）
   ↓
用户 选中某个命令
   ↓
输入参数（结构化表单 / 参数）
   ↓
触发到 用户本地 HTTPServer（用户自己机器上跑的 HTTP 服务）
```

### 1.3 与传统 Agent Skill 的本质差异（调研核心区分点）

这是本方案与传统 Agent Skill **最大的不同** —— 传统 Agent Skill 由 **AI Agent（LLM）参与处理**（理解自然语言、解析意图、决策调用哪个工具）；而本方案 **全程无 AI Agent 处理，纯程序化传递**（命令确定性映射、参数结构化传递、路由程序化）。

| 维度 | 传统 Agent Skill | 本方案（调研对象） |
|------|-----------------|------------------|
| 中间处理方 | **AI Agent（LLM）** 参与：理解自然语言、解析意图、决定调哪个工具 | **无 AI Agent**，全程纯代码/程序化处理 |
| 输入理解 | 自然语言 → LLM 意图识别 | 命令**确定性映射**，参数**结构化字段** |
| 决策方式 | 模糊语义 → 模型决策 → 执行 | 强类型：命令枚举 → 参数 schema → 触发 |
| 本质 | "聪明地猜"（语义推断/模型决策） | "确定地传"（命令/参数/路由全部程序化） |

> 一句话：**不是给 LLM 一个 prompt 让它决定"怎么做"，而是用户显式选命令、显式填参数，程序按定义好的映射把结构化数据原样转发到本地 HTTPServer。全程无语义推断、无模型决策。**

---

## 二、架构链路（当前已知信息）

```mermaid
flowchart TD
    A["IM 客户端（Slash 唤起命令列表）"]
    B["AgentConnector（中间系统）<br/>open-app 的一部分<br/>仅标准环境有，当前开发环境没有"]
    C["CommandServer（命令管理服务）<br/>计划中，暂不存在"]
    D["用户本地 HTTPServer<br/>用户通过本地 CLI / xxx 注册到 CommandServer"]

    A -->|"选中命令 + 输入参数"| B
    B --> C
    C --> D
```

### 2.1 组件说明

| 组件 | 状态 | 说明 |
|------|:---:|------|
| **AgentConnector** | 仅标准环境有 | 中间编排系统，open-app 的一部分；当前环境不具备 |
| **CommandServer** | 计划中，暂不存在 | 命令管理服务：维护命令清单 / 参数 schema / 注册关系 |
| **本地 CLI / xxx** | 待定 | 用户注册命令到 CommandServer 的入口方式（`xxx` 为未确定的占位指代） |
| **用户本地 HTTPServer** | 用户侧 | 最终接收触发，执行命令逻辑的本地服务 |

> ⚠️ **不确定性标注**：`CommandServer` 与`本地注册入口`均为规划中/未定形态；`AgentConnector` 当前环境不可用。这些组件细节待后续调研补充。

---

## 三、调研目标

| # | 目标 | 要回答的问题 |
|---|------|-------------|
| G1 | **验证趋势判断** | "IM 内 Slash 命令直达本地服务"这种**无 AI 处理、全程序传递**的确定性触发模式，是否是业界主流 / 主流厂商主推方向？ |
| G2 | **对标主流平台做法** | Slack / Discord / Telegram / 飞书 / 钉钉 / 企微 / Teams 等 IM，是否提供同类能力（Slash 命令 / 本地服务直连）？形态如何？ |
| G3 | **明确与 AI Agent Skill 的边界** | 业界是否也存在"非 AI 触发"的确定性命令通道？它与 Agent Skill（AI 触发）的分工 / 关系是什么？ |
| G4 | **判断未来趋势** | 这种模式是过渡性方案还是未来趋势？与 AI Agent 生态是替代、互补还是并存？ |
| G5 | **提炼对 open-app 的启示** | 综合结论，给出 open-app 是否应投入 / 如何投入该方向的建议 |

---

## 四、调研计划（调研对象 + 调研维度）

### 4.1 调研对象全景（四类）

#### 4.1.1 同类型竞品 —— 提供"确定性命令触发 / Slash 命令"的 IM 与通讯平台（直接竞品）

> 与本方案（IM 内 `/` 唤起命令 → 参数 → 触发本地服务）**形态最接近**的产品。统一追踪三方面：**① 非 AI 本地 Command 支持、② AI Agent Skill 支持、③ 未来规划**（是否主推本地执行 / 是否 AI 化取代确定性）。

| 平台 | 归属 | 非 AI 本地 Command | AI Agent Skill | 未来规划 |
|------|------|:---:|:---:|------|
| **Slack** | Salesforce | ✅ Slash Commands（`/command` 直达 Request URL/HTTP，参数结构化） | ✅ Slack AI + Agentforce + MCP 接入 | 【待调研】主推 Agentforce，是否仍保留确定性通道 |
| **Discord** | Discord | ✅ Slash Commands + Interactions（Webhook 回传，参数 Options/Autocomplete） | ⚠️ 无原生，靠第三方 AI Bot / Activities | 【待调研】以确定性为主 |
| **Telegram** | Telegram | ✅ Bot Commands（`/command`）+ 内联菜单按钮 | ⚠️ Bot API 无原生 AI，第三方接入 | 【待调研】确定性为主 |
| **飞书 / Lark** | 字节跳动 | ✅ 快捷指令 / `/` 命令 + 交互卡片 | ✅ 飞书智能伙伴 + 飞书 MCP | 【待调研】AI 助理与命令并存（双通道） |
| **钉钉** | 阿里巴巴 | ✅ 群机器人 `/` 命令 + 互动卡片 + 酷应用 | ✅ 钉钉 AI 助理 + 钉钉 MCP | 【待调研】AI 助理与命令并存（双通道） |
| **企业微信** | 腾讯 | ⚠️ 有限（应用消息 + 回调，无标准 `/` 命令菜单） | ✅ 企微智能机器人 | 【待调研】回调为主 |
| **Microsoft Teams** | 微软 | ✅ Slash command + Message Extensions | ✅ Copilot Studio + Teams AI Library + 自定义引擎 Agent | 【待调研】AI 与命令并存（双通道） |

> ⚠️ 上表各列初判基于公开认知，**需在信息采集阶段用官方文档核实**；`AI Agent Skill 支持` 列关注是否提供"MCP / Function Calling / 自定义 Agent"等机器可调用通道。

#### 4.1.2 不同类型产品 —— AI 驱动 / 自动化类（对照：区分"确定性 vs AI"）

> 这些是 AI Agent 参与处理的"对立面"，同样统一追踪三方面：**① 非 AI 本地 Command 支持、② AI Agent Skill 支持、③ 未来规划**，用于判断"确定性命令 vs AI 触发"是替代还是互补。

| 平台 / 产品 | 类型 | 非 AI 本地 Command | AI Agent Skill | 未来规划 |
|------------|------|:---:|:---:|------|
| **Slack AI / Agentforce** | AI Agent 整合 | ⚠️ 复用 Slack 原生 Slash Commands | ✅ 核心（Agentforce 主推） | 观察是否仍保留确定性通道 |
| **Microsoft Copilot Studio** | Agent 平台 | ⚠️ 无本地 Command 概念 | ✅ 核心（自定义 Agent） | AI 驱动为主 |
| **飞书智能伙伴 / 钉钉 AI 助理** | AI 助理 | ⚠️ 复用平台原生命令/卡片 | ✅ 核心（自然语言 + 工具调用） | AI 驱动为主 |
| **OpenAI Assistants / GPTs / Function Calling** | AI 工具调用 | ❌ 无（LLM 决策调工具） | ✅ 核心 | AI 纯驱动 |
| **Claude Code Skills / MCP** | AI Skill 生态 | ❌ 无（AI 理解后调用） | ✅ 核心（**本方案的对立面**） | AI 纯驱动 |
| **GitHub Copilot CLI / AI CLI** | AI 命令行 | ❌ 无（slash 命令仍由 AI 驱动） | ✅ 核心 | AI 驱动为主 |
| **n8n / Zapier / Make / Coze / Dify** | 编排 / 自动化 | ✅ 有（确定性工作流节点） | ✅ 有（AI 节点 / Agent 编排） | 双通道并存 |

> ⚠️ 上表初判基于公开认知，需信息采集阶段核实；本类对标的核心问题是——**它们是否也保留了"非 AI 确定性触发"这条通道，还是全面 AI 化**。

#### 4.1.3 生态基础设施 / 协议

| 对象 | 说明 |
|------|------|
| **Webhook / 事件回调机制** | 命令触发到底层服务的基本载体（Slack/Discord/飞书/钉钉均用） |
| **WebSocket / 长连接 / 断点续传** | IM → **用户本地**的通道打通方式（关键难点） |
| **MCP / Function Calling / A2A** | 与"确定性命令"定位互补/相悖的 AI 接入协议，用于划清边界 |
| **open-app 自身 connector-api / event-server** | 本平台已有能力，评估可复用度（事件回调 / 连接器） |

#### 4.1.4 本地执行工具（非 IM 语境，但交互形态可参考）

| 对象 | 说明 |
|------|------|
| **VS Code Command Palette** | 确定性命令列表 + 参数输入 + 本地执行（交互形态高度一致） |
| **Raycast / Alfred / Spotlight** | 本地 launcher，确定性命令 + 参数 |
| **开发者 CLI（gh / npm / git）** | 确定性子命令 + flags 参数，命令枚举的先例 |

---

### 4.2 调研维度（每个维度要回答的问题）

#### 维度 A · 命令入口与唤起形态
- 各平台是否支持 `/` slash 唤起命令列表？
- 唤起交互形态：下拉 / 弹出面板 / 侧栏 / 消息内嵌？
- 命令清单由客户端本地生成还是服务端下发？

#### 维度 B · 参数输入形态
- 参数如何输入：自由文本 / 结构化表单 / Modal / 参数选项（options）/ Autocomplete？
- 命令如何声明所需参数（参数 schema / manifest）？

#### 维度 C · 命令注册与下发
- 命令清单由谁维护：平台内置 / 第三方应用注册 / 本地服务上报？
- 命令发现机制：用户如何知道有哪些命令？

#### 维度 D · 触发到本地的链路机制（关键技术难点）
- IM 命令如何到用户本地：Webhook / 长连接 / 内网穿透 / 本地 agent / 轮询？
- 安全与鉴权：本地服务如何可信接收（token / 双向通道 / 签名）？
- 是否有成熟先例？业界主流走哪种链路？

#### 维度 E · 确定性 vs AI（核心区分维度）
- 业界做"命令触发"的主流：确定性（无 LLM）还是 AI 理解？
- 是否存在纯确定的命令通道（无 AI 参与）？是被主推还是被 AI 取代？
- **追踪每个平台的「非 AI 本地 Command」与「AI Agent Skill」两套能力的存在与成熟度**。

#### 维度 F · 与 Agent Skill 的关系
- 确定性命令与 AI Skill 是替代、互补还是并存？
- 主流厂商是否同时保留两条通道？

#### 维度 G · 生态与未来规划
- 命令市场 / 插件生态规模与活跃度？
- 该方向的未来演化：过渡 / 互补 / 长期主流？
- **主流厂商的未来规划**：对"本地执行 / 本地 agent"、以及"AI Agent Skill"两条路线的战略投入与路线图表述。

---

### 4.3 对标矩阵（对象 × 维度，S 骨架）

> 下表为计划采集的结构骨架；`【待调研】` 表示需在信息采集阶段填实。**用户核心关注列：维 E（确定性 vs AI）、维 D（本地链路）、维 G（未来规划）**。

| 对象 | A 入口 | B 参数 | C 注册 | D 本地链路 | E 确定性/AI | F 与AI关系 | G 未来规划 |
|------|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| **Slack** | 支持 | Modal/Blocks | 应用注册 | 【待调研】 | 【待调研】 | 双通道 | 【待调研】 |
| **Discord** | 支持 | Options/Autocomplete | 应用注册 | 【待调研】 | 全确定性 | 无AI入口 | 【待调研】 |
| **Telegram** | 支持 | 内联按钮 | Bot 注册 | 【待调研】 | 全确定性 | 无AI入口 | 【待调研】 |
| **飞书 / Lark** | 支持 | 交互卡片 | 应用/快捷指令 | 【待调研】 | 部分AI化 | 双通道 | 【待调研】 |
| **钉钉** | 支持 | 互动卡片 | 酷应用 | 【待调研】 | 部分AI化 | 双通道 | 【待调研】 |
| **企业微信** | 有限 | 卡片消息 | 应用注册 | 【待调研】 | 【待调研】 | 双通道 | 【待调研】 |
| **Teams** | 支持 | Message Extensions | 应用注册 | 【待调研】 | 部分AI化 | 【待调研】 | 【待调研】 |
| **VS Code** | 支持 | 参数输入 | 扩展注册 | 本地执行 | 全确定性 | 无AI | 【待调研】 |

> ⚠️ 矩阵中"确定/部分AI化"等初判基于公开认知，**需在信息采集阶段用官方文档核实**，避免结论偏差。

---

## 五、产出物规划

**产出思路**：**每个产品产出一份单独调研报告**（一产品一份，按产品平铺，不按类型分组），所有产品调研完成后，**汇总出一份整体调研对比汇总报告**（`summary.md`）。

### 5.1 产出结构

```
docs/im-slash-command-research/
├── README.md                          # 本文件：背景 + 调研计划 + 维度 A~G 定义
├── summary.md                         # ★ 整体调研对比汇总报告（最终产出，标记同类型/不同类型）
└── reports/                           # 各产品单独调研报告（一产品一份，不按类型分组）
    ├── 01-slack.md                    # Slack（同类型 IM）
    ├── 02-discord.md                  # Discord（同类型 IM）
    ├── 03-telegram.md                 # Telegram（同类型 IM）
    ├── 04-feishu-lark.md              # 飞书 / Lark（同类型 IM）
    ├── 05-dingtalk.md                 # 钉钉（同类型 IM）
    ├── 06-wecom.md                    # 企业微信（同类型 IM）
    ├── 07-teams.md                    # Microsoft Teams（同类型 IM）
    ├── 08-agentforce.md               # Slack AI / Agentforce（不同类型）
    ├── 09-copilot-studio.md           # Microsoft Copilot Studio（不同类型）
    ├── 10-ai-assistant.md             # 飞书智能伙伴 / 钉钉 AI 助理（不同类型）
    ├── 11-openai-tool-calling.md      # OpenAI Assistants / GPTs / Function Calling（不同类型）
    ├── 12-claude-code-skills.md       # Claude Code Skills / MCP（含 A2A）（不同类型）
    ├── 13-ai-cli.md                   # GitHub Copilot CLI / AI CLI（不同类型）
    └── 14-automation-ipaas.md         # n8n / Zapier / Make / Coze / Dify（不同类型）
```

> `reports/` 按**产品**平铺（一产品一份），不再按"同类型/不同类型"分组；产品是否同类型在每份报告的**产品描述**中备注，整体分类汇总在 `summary.md` 中统一标记。

### 5.2 单份产品报告（统一三块内容）

每份产品报告开头一段 **产品描述**（备注该产品属于 §4.1.1 同类型竞品 / §4.1.2 不同类型对照），正文为**统一的三块内容**：

| # | 块 | 内容 |
|---|-----|------|
| ① | **非 AI 本地 Command 支持** | 是否有确定性命令通道（无 LLM），形态 / 参数 / 本地触发链路（对应维度 A~D） |
| ② | **AI Agent Skill 支持** | 是否提供 MCP / Function Calling / 自定义 Agent 等机器可调用通道（对应维度 E~F） |
| ③ | **未来规划** | 厂商对未来方向（本地执行 / AI 化）的战略投入与路线图（对应维度 G） |

> **产品描述**：归属、形态、定位、目标用户，并备注「同类型 / 不同类型」分类；分类不在文件名或目录里体现，统一由 `summary.md` 汇总标记。

### 5.3 整体对比汇总报告（summary.md）

汇总所有产品报告，产出：

1. **对标矩阵总表**（填实 §4.3 骨架：对象 × 维度 A~G），**并在表中标记每个产品属于同类型 / 不同类型**
2. **三块内容横向对比**：非 AI 本地 Command / AI Agent Skill / 未来规划，各产品横向对照
3. **趋势判断**：回应调研目标 G1~G5（是否主流 / 主推 / 未来趋势）
4. **对 open-app 的结论建议**：是否投入、如何投入该方向

---

## 六、调研状态与下一步

| 事项 | 状态 | 说明 |
|------|:---:|------|
| 背景确认 | ✅ | 已与用户对齐（链路 / 组件状态 / 核心差异化） |
| 调研计划对齐 | ✅ | 目标 G1~G5、四类调研对象、维度 A~G 已定（见 §四） |
| 分支创建 | ✅ | `feature/im-slash-command-research` |
| 信息采集 | ✅ | 14 份产品报告基于官方文档 / 官方渠道采集（见 §📑 文档导航） |
| 报告撰写 | ✅ | `reports/01~14` + `summary.md` 均已产出 |
| 结论对齐 | ⏳ 待办 | 结论已产出，待与用户对齐后决定是否正式立项 |

> **下一步**：与用户对齐 `summary.md` 的结论建议（尤其「是否投入 / 如何投入 / 与 AI 的差异化切入点」），决定是否正式立项进入 SDDU 完整流程。
