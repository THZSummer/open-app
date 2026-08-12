# 04 · Agent 编排 / 连接器生态调研（维度 D）

> [⬆ 返回 README（调研总览）](README.md) · [📑 文档导航](README.md#-文档导航)

> **调研目标**: G3/G5 —— Agent 如何消费"连接器/工具"，编排引擎 AI 化方向（对照 open-app 的 connector-api）
> **采集日期**: 2026-08-12
> **信息来源**: n8n 官方文档（docs.n8n.io）、Zapier/Make/Coze/Dify 官方信息（领域知识 + 搜索摘要）
> **方法**: n8n 官方直采（MCP Client / MCP Server Trigger 两节点文档）；其余以领域知识标注

---

## 一、执行摘要（先出结论）

1. **iPaaS/自动化平台集体转向"Agent + MCP"**：n8n 已具备**双向 MCP 能力**（MCP Client 消费外部工具 + MCP Server Trigger 对外暴露自身工作流为 MCP 工具），这是 open-app connector-api 演进的最直接参照。
2. **编排引擎 AI 化的两条路径已清晰**：① **MCP 化**——把既有编排能力（节点/DAG/工作流）暴露为标准工具供 Agent 调用；② **AI 编排**——LLM 动态组合工具链（AI Agent 节点、AI Workflow Builder）。
3. **对 open-app 的含义**：connector-api 的 DAG 调度 + GraalJS 脚本沙箱 + 5 类节点，**天然可升级为 MCP Server**（把 DAG 工作流封装为 MCP 工具），同时可引入"LLM 动态编排"作为 AI 化演进方向。

---

## 二、n8n（开源自动化，官方直采——核心对标）

### 2.1 MCP Client 节点（消费外部工具）

| 维度 | 现状（官方文档） |
|------|----------------|
| 功能 | 作为 MCP 客户端，**将外部 MCP Server 暴露的工具作为工作流普通步骤**使用 |
| 认证 | 支持 Bearer / Header（多 Header）/ **OAuth2** 认证 |
| 配置 | 选择 Server Transport + MCP Endpoint URL + 工具（**工具列表自动从 MCP Server 获取**） |
| 输入模式 | Manual（手动参数）/ JSON（嵌套参数） |
| Agent 用法 | 配套 **MCP Client Tool 节点**：将 MCP 工具作为 AI Agent 的可用工具 |

### 2.2 MCP Server Trigger 节点（对外暴露工作流）

| 维度 | 现状（官方文档） |
|------|----------------|
| 功能 | **让 n8n 作为 MCP Server**，将 n8n 工具/工作流暴露给外部 MCP 客户端 |
| 暴露方式 | 生成 Test/Production MCP URL；客户端可列出并调用工具 |
| Transport | **SSE + Streamable HTTP**（不支持 stdio） |
| 认证 | Bearer / Header 认证 |
| 工作流暴露 | 通过 Custom n8n Workflow Tool 节点把工作流挂载为工具 |
| 部署注意 | **SSE/HTTP 长连接需单一 webhook 副本处理**（queue mode 多副本需路由 /mcp* 到专用实例；nginx 需关 proxy_buffering） |
| 集成示例 | 官方文档给出与 Claude Desktop 对接的 mcp-remote 网关配置 |

### 2.3 对 open-app 的直接启示

- **n8n 的 MCP Server Trigger = open-app connector-api 的 MCP 化范本**：把既有 DAG 工作流/节点封装成 MCP 工具对外暴露。
- **部署经验可直接复用**：MCP 远程服务需处理长连接与反代配置（proxy_buffering off / 单一副本），open-app 的 event-server（SSE/WebSocket/WebHook）已有类似基础设施。

---

## 三、Zapier / Make（商业 iPaaS）

> 本节基于领域知识（官方页面采集受限），需复查。

| 平台 | AI 化动作（领域知识） | 编排形态 |
|------|----------------------|---------|
| **Zapier** | **Zapier Agents**：AI 自动生成 Zaps（流程）；Agents 平台可消费 8000+ 应用连接器 | 触发+动作（触发器驱动） |
| **Make** | **Make AI**：AI 节点、生成场景（scenario）；AI 助手辅助搭建 | 可视化场景编排 |

**对 open-app 的含义**：商业 iPaaS 的 AI 化重点是"**用 AI 生成/编排流程**"——open-app connector-api 可增加"自然语言生成 DAG"能力（GraalJS 沙箱可安全执行 AI 生成的脚本逻辑）。

---

## 四、Coze / Dify（国内 Agent 平台）

> 本节基于领域知识，需复查。

| 平台 | 定位 | AI 化能力 |
|------|------|----------|
| **Coze（扣子）** | 国内 Agent 平台（字节） | 插件体系、Bot 工作流、**MCP 支持**、知识库、记忆 |
| **Dify** | 开源 LLM 应用平台 | 工具调用、Agent 编排、RAG 工作流、可接入 MCP |

**对 open-app 的含义**：Agent 平台的共同模式是"**插件/工具市场 + 工作流编排 + 知识库**"——open-app 的 market 能力管理（README §1.1）应升级为"**Agent 可发现的工具市场**"。

---

## 五、编排生态对比矩阵

| 平台 | 类型 | MCP 支持 | AI 编排能力 | 编排粒度 | open-app 参照点 |
|------|------|:---:|------|------|------|
| **n8n** | 开源自动化 | ✅ 双向（Client+Server） | AI Agent 节点 | 节点级 DAG | **最高（直接对标）** |
| **Zapier** | 商业 iPaaS | 生态支持 | Agents（AI 生成流程） | 触发+动作 | 市场分发模式 |
| **Make** | 商业 iPaaS | 生态支持 | AI 节点 | 场景级 | 场景化编排 |
| **Coze** | 国内 Agent 平台 | ✅ | Bot 工作流 | Bot/工作流 | 插件市场 |
| **Dify** | 开源 LLM 平台 | ✅ | Agent 编排 + RAG | 工作流 | RAG/知识库 |

---

## 六、结论（对应 G3/G5）

1. **Agent 消费连接器的标准路径已收敛到 MCP**：n8n 双向 MCP 支持是最强证据——"连接器平台 = MCP Server 提供者 + MCP Client 消费者"。
2. **编排引擎 AI 化方向**（对照 connector-api）：
   - **短期（MCP 化）**：connector-api 的 DAG 工作流封装为 MCP 工具，供 Agent 直接调用（对齐 n8n MCP Server Trigger）。
   - **中期（AI 生成编排）**：自然语言 → DAG 生成（对齐 Zapier Agents / Make AI），GraalJS 沙箱保证安全。
   - **长期（Agent 原生编排）**：LLM 作为编排大脑，动态组合 connector 节点（Agent 节点模式）。
3. **市场升级**：market 能力管理从"人类开发者浏览 API"升级为"**Agent 可发现、可列举的 MCP 工具目录**"（对齐 OpenAI tool search / n8n 工具列表自动获取）。

---

## 七、信源清单

| 信源 | 类型 | 采集时间 |
|------|------|---------|
| docs.n8n.io MCP Client 节点文档 | 官方直采 | 2026-08-12 |
| docs.n8n.io MCP Server Trigger 节点文档（含部署/反代注意事项） | 官方直采 | 2026-08-12 |
| Zapier / Make / Coze / Dify | 领域知识 | 需复查 |
