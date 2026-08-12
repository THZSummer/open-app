# 01 · AI 接入协议生态调研（维度 A）

> **调研目标**: G2 —— 摸清 MCP / Function Calling / A2A 等协议的成熟度、被主流平台采纳程度，判断 open-app 应优先支持哪个
> **采集日期**: 2026-08-12
> **信息来源**: MCP 官方文档（modelcontextprotocol.io）、A2A 官方（a2a-protocol.org）、OpenAI 官方文档（platform.openai.com）+ 领域知识补充
> **方法**: 官方文档直采为主；未覆盖处标注「领域知识」，时效截至 2025 中期，需复查

---

## 一、执行摘要（先出结论）

1. **协议格局已定**：MCP（Agent↔工具）与 A2A（Agent↔Agent）形成互补双协议，均已捐入 Linux 基金会、有正式治理结构。**MCP 已是事实上的"AI 工具接入标准"，A2A v1.0 已发布并获 7 家核心厂商背书**。
2. **Function Calling 仍是各 LLM 的"内建底座"**，但正被 MCP 收敛：OpenAI 已将 MCP server 作为内置工具接入（Connectors），Anthropic/Google 全线支持 MCP。
3. **对 open-app 的直接结论**：**优先支持 MCP 是唯一合理选择**——一次能力封装，可被 Claude/ChatGPT/VS Code/Cursor 及国内主流（通义/豆包/Kimi 等）同时消费。Function Calling 是模型厂商内部能力，open-app 无需"支持"，只需把 API 描述成标准 Tool Schema（JSON Schema）即可被任何模型消费。A2A 属第二梯队，进入 Agent 间协作时再考虑。

---

## 二、MCP（Model Context Protocol）—— Agent 到工具的标准协议

### 2.1 现状与治理（2026-07-28 规范版）

| 维度 | 现状 |
|------|------|
| 定位 | "AI 的 USB-C"：标准化 AI 应用连接外部数据源/工具/工作流 |
| 治理 | 已由 Anthropic 捐赠 **Linux 基金会**，有 SEP（Specification Enhancement Proposal）治理流程、Working Groups/Interest Groups、贡献者梯队 |
| 最新规范 | 2026-07-28 版本，含 versioning 与 feature lifecycle（Active/Deprecated/Removed） |
| 官方 Registry | MCP Registry 已上线（发布/聚合/审核机制完备），有官方注册表、聚合器、GitHub Actions 自动发布 |
| SDK 体系 | 官方 SDK + **SDK Tiering 系统**（分功能完整度/协议支持/维护承诺等级） |

### 2.2 核心能力（对开放平台最关键的部分）

| 能力 | 说明 | open-app 相关性 |
|------|------|:---:|
| **Tools**（工具调用） | 服务端暴露 JSON Schema 定义的 tool，客户端 Agent 调用 | ★★★ 开放 API 的直接载体 |
| **Resources**（资源） | 暴露可读取的数据资源（类文件系统） | ★★☆ IM/云盘/文档类能力 |
| **Prompts**（提示模板） | 服务端提供可复用的 prompt 模板 | ★☆☆ |
| **Transport** | stdio（本地）+ **Streamable HTTP**（远程，取代 SSE） | ★★★ 远程 HTTP 与 open-app API 网关天然契合 |
| **Authorization** | **OAuth 2.1** + 授权服务器发现 + 客户端注册；另有 **Enterprise-Managed Authorization**（企业 IdP 集中管控） | ★★★ 与企业 AKSK/权限中心对接点 |
| **Tasks 扩展** | 异步长任务执行（SEP-1686/2663） | ★★☆ 审批流/长任务场景 |
| **MCP Apps** | 在 MCP host（如 Claude Desktop）内渲染交互 UI | ★☆☆ 未来可选 |
| **OAuth Client Credentials** | 机器到机器认证（MCP 授权扩展） | ★★★ 与 open-app AKSK 机器凭证模式对照 |

### 2.3 生态采纳（官方文档确认）

- **AI 助手客户端**：Claude、ChatGPT（OpenAI 已将 MCP server 作为内置 connector）、VS Code Copilot、Cursor 等
- **扩展生态**：Agent Skills、Authorization、Tasks、Registry 等官方扩展 + Extension Support Matrix
- **国内跟进**（领域知识，需复查）：阿里通义、字节豆包、月之暗面等均宣布兼容 MCP；多家云厂商提供 MCP 托管服务

---

## 三、Function Calling / Tool Calling —— 模型厂商的内建底座

### 3.1 最新形态（OpenAI 官方 2026-08 文档）

| 特性 | 说明 |
|------|------|
| 定义 | 模型在生成中决定调用外部工具，参数由 **JSON Schema** 描述 |
| Strict mode | `strict: true` 强制参数符合 schema（结构化输出），官方**推荐始终开启** |
| **Tool Search** | 大量工具时按需加载（`gpt-5.4+` 支持），解决"工具太多 token 放不下/选择难"问题 |
| **Namespace** | 按领域分组工具（crm/billing/shipping），帮助模型在大型工具面中选型 |
| Custom Tools | 自由文本输入（非 JSON），可配 **CFG 文法约束**（Lark/Regex） |
| 最佳实践 | **每轮初始可用工具建议 <20 个**；工具定义计入输入 token |
| 平行调用 | GPT-5 起支持 parallel tool calling |

### 3.2 与 MCP 的关系

- Function Calling 是"模型如何表达工具调用"的**协议内机制**，MCP 是"工具如何被发现/暴露"的**生态协议**，二者是不同层。
- **业界收敛路径**（领域知识+官方文档佐证）：OpenAI 支持通过 Connectors 接入 MCP server → 各厂商模型在"消费能力"上都向 MCP 靠拢；但模型自身能力的**暴露**仍靠 Function Calling/Tool Schema。
- **对 open-app 的含义**：open-app 的 184 个 API 应被描述成 **JSON Schema 工具定义**（天然兼容所有模型），而**接入载体**用 MCP server 实现即可一鱼多吃。

---

## 四、A2A（Agent2Agent）—— Agent 到 Agent 的标准协议

### 4.1 现状（v1.0 已发布）

| 维度 | 现状 |
|------|------|
| 定位 | 不同厂商/框架构建的 Agent 之间互操作的标准通信协议 |
| 治理 | Google 开发并捐赠 **Linux 基金会**；TSC 含 **AWS、Cisco、Google、IBM Research、Microsoft、Salesforce、SAP、ServiceNow** |
| 版本 | **v1.0 已发布**（2026），含正式规范 + 扩展/自定义绑定治理 |
| SDK | Python / JS / Java / .NET / Go / Rust 六种官方 SDK |
| 生态 | 官方 Samples、DeepLearning.AI 课程、Partner 计划 |

### 4.2 与 MCP 的官方分工（A2A 官方原文结论）

> "MCP 与 A2A 不是竞争者，而是高度互补的两个问题解决方案：**MCP 标准化 Agent→工具**，**A2A 标准化 Agent→Agent**。用 MCP 装备单个 Agent 的工具，用 A2A 让不同框架的 Agent 安全协作。"

| 对比维度 | MCP | A2A |
|---------|-----|-----|
| 通信方向 | Agent → Tool（工具/API/数据源） | Agent ↔ Agent（委派、协作、结果共享） |
| 核心原语 | Tools / Resources / Prompts | Task（Agent Card 发现 + 任务生命周期） |
| 生态成熟度 | 更成熟（客户端/Registry/SDK 全面） | 快速上升（v1.0 + 7 家核心厂商） |
| 企业特性 | OAuth 2.1 + 企业托管授权 | Enterprise Features（多租户、流式、异步） |
| 对 open-app | **当前优先级最高** | 第二梯队（Agent 协作场景再考虑） |

---

## 五、OpenAPI → Tool Schema（社区实践）

- **做法**（领域知识）：将现有 OpenAPI 资产转换为 LLM 可消费的 tool description（`name`/`description`/`parameters` JSON Schema）。
- **工具**：已有多种开源转换器（如 openapi-to-tool、各厂商提供的 OpenAPI 转换支持）。
- **对 open-app**：这是**零成本起步路径**——open-app 治理骨架已有 184 API（OpenAPI 描述），先做"API → Tool Schema"标准化即可被所有模型消费，无需改动后端。

---

## 六、对比矩阵与选型建议

| 协议 | 定位 | 成熟度 | 治理 | open-app 优先级 | 理由 |
|------|------|:---:|:---:|:---:|------|
| **MCP** | Agent→工具 | ★★★★★ | Linux 基金会 + SEP | **P0（立即）** | 事实标准；一次封装多端消费；OAuth/授权模型可复用现有治理 |
| **Function Calling** | 模型内建机制 | ★★★★★ | 各家私有 | P0（作为 schema 规范） | 不是"协议"而是规范：open-app 用 JSON Schema 描述能力即可，无需专门"支持" |
| **A2A** | Agent→Agent | ★★★☆☆ | Linux 基金会 | P1（跟踪/试点） | v1.0 已发布，但企业场景落地尚早；待 Agent 生态规模化后进入 |
| OpenAPI→Tool Schema | 转换实践 | ★★★☆☆ | 社区 | P0（作为实施步骤） | 零成本起步，作为 MCP 的前置准备 |

### 选型结论（对应 G2）

1. **open-app 应优先支持 MCP**：把 IM/Meeting/CloudBox/Contact/Mail/Drive/Bot 能力封装为 MCP server（远程 Streamable HTTP transport），配合 OAuth 2.1/Enterprise-Managed Authorization 与现有 AKSK/权限中心对接。
2. **同步输出 Tool Schema**：将现有 184 API 描述为 JSON Schema 工具（含 namespace/tool search 意识：避免单轮 >20 工具），保证非 MCP 客户端（直接用模型 API 的开发者）也能消费。
3. **A2A 列为观察项**：待 open-app 具备多 Agent 协作场景（如多个业务 Agent 通过 open-app 互相调用）时启动。

---

## 七、信源清单

| 信源 | 类型 | 采集时间 |
|------|------|---------|
| modelcontextprotocol.io（官方文档 + 规范 2026-07-28 + Registry + SEP 索引） | 官方直采 | 2026-08-12 |
| a2a-protocol.org（v1.0 发布、A2A↔MCP、治理） | 官方直采 | 2026-08-12 |
| platform.openai.com/docs/guides/function-calling（tool search/namespace/strict/custom tools） | 官方直采 | 2026-08-12 |
| 国内厂商 MCP 兼容情况 | 领域知识 | 需复查 |

> **注**：国内厂商（通义/豆包/Kimi 等）MCP 支持细节以领域知识补充，建议后续在可访问国内站点的环境复查更新。
