# open-app 项目全景 — 全景入口

> **文档定位**: sddu-docs-overview — 本级全景入口  
> **输出文件名**: docs-overview.md  
> **数据来源**: 代码扫描生成（用户指令触发），未经 SDDU 工作流验证。不包含设计意图、业务语义和技术决策分析。  
> **创建时间**: 2026-08-03  
> **版本**: v1.0 (CODE-SCAN)  
> **生成方式**: 全量生成（SCAN_MODE=CODE）

---

> ⚠️ **数据来源**: 代码扫描生成（用户指令触发），未经 SDDU 工作流验证。不包含设计意图、业务语义和技术决策分析。

## 0. 全景速览

| 维度 | 统计 |
|------|------|
| **后端服务** | 5 个 Spring Boot 服务 + 1 个 Flyway 迁移工程 |
| **前端工程** | 4 个（wecodesite 开发者控制台、market-web 市场管理、qiankunProject 微前端、wecodesiteDemo 静态演示） |
| **数据库表** | 40 张（7 个 Flyway 迁移脚本，单库 `openapp`） |
| **API 端点** | 约 130 个（open-server ~80、market-server ~25、api-server ~15、event-server ~8、connector-api 2） |
| **前端页面** | 20+ 路由页面（wecodesite）+ 8 模块（market-web）+ 4 子应用（qiankun） |
| **事件通道** | SSE、WebSocket、WebHook 回调、内部消息（event-server） |

---

## 1. 业务全景

### 1.1 自身概述

| 属性 | 值 |
|------|-----|
| **类型** | 多服务微服务系统（能力开放平台 + 连接器平台 + 事件回调网关 + 数据开放平台） |
| **职责描述** | 面向企业内的能力开放平台：统一管理 API / 事件 / 回调 / 连接器的注册、审批、订阅、消费；提供连接流编排与执行引擎；提供数据开放查询与用户授权 |
| **所属业务域** | open-app 子平台 |
| **版本** | v1.0 (代码扫描快照) |

### 1.2 服务/工程清单

| 服务 | 类型 | 端口 | 职责 | 文档目录 |
|------|------|:----:|------|---------|
| **open-server** | Spring Boot 3.5 Web (Servlet) | 18080 | 能力开放平台管理面：应用/能力/API/事件/回调/连接器/连接流 CRUD、审批、权限、成员、同步 | `open-server/` |
| **api-server** | Spring Boot 3.4 Web | 18081 | 数据开放查询网关 + API 消费网关 + 用户授权 + 审批回调 + 内部角色同步 | `api-server/` |
| **connector-api** | Spring Boot 3.5 WebFlux + R2DBC | 18180 | 连接流执行引擎：HTTP 触发、版本调试、脚本节点（GraalJS）、执行记录 | `connector-api/` |
| **event-server** | Spring Boot 3.4 Web | 18082 | 事件发布网关 + 回调触发网关 + SSE/WebSocket 通道 | `event-server/` |
| **market-server** | Spring Boot 3.4 Web | 18083 | 市场管理面：数据字典、LookUp、能力管理（admin）、审批、文件、聊天机器人绑定 | `market-server/` |
| **open-flyway** | Flyway Maven 工程 | — | 数据库迁移（7 个脚本 / 40 张表） | `database/` |
| **wecodesite** | React 18 + Vite + qiankun | — | 开发者控制台（应用管理、API/事件/回调管理、连接器/连接流编排） | `frontend/` |
| **market-web** | React 18 + Vite | — | 市场管理后台（审批、LookUp、字典、能力管理） | `frontend/` |
| **qiankunProject** | 微前端（main-app + 4 子应用） | — | 嵌入能力微前端容器 | `frontend/` |
| **wecodesiteDemo** | 静态 HTML 演示 | — | 连接器/流编辑器原型演示 | `frontend/` |

### 1.3 数据库域索引（40 表）

| 迁移脚本 | 表数 | 表清单 |
|---------|:----:|--------|
| V1 create_early_schema | 16 | operate_log_t, property_t, file_t, employee_t, eamap_t, app_t, app_p_t, app_identity_t, app_member_t, app_version_t, app_version_p_t, app_ability_relation_t, ability_t, ability_p_t, lookup_classify_t, lookup_item_t |
| V2 init_capability_open_platform | 15 | v2_category_t, v2_category_owner_t, v2_api_t, v2_api_p_t, v2_event_t, v2_event_p_t, v2_callback_t, v2_callback_p_t, v2_permission_t, v2_permission_p_t, v2_subscription_t, v2_approval_flow_t, v2_approval_record_t, v2_approval_log_t, v2_user_authorization_t |
| V3 init_connector_platform | 4 | v2_cp_connector_t, v2_cp_connector_version_t, v2_cp_flow_t, v2_cp_flow_version_t |
| V4 connector_platform_v3 | 3 | v2_cp_connector_version_ref_t, v2_cp_execution_record_t, v2_cp_execution_step_t |
| V5 add_ability_admin_fields | 0（ALTER） | ability_t 增 6 字段（entry_url/hidden/route_path/alias_name/require_release/load_type） |
| V6 create_common_file | 1 | common_file_t |
| V7 create_lookup_file | 1 | lookup_file_t |

> 📖 完整表结构见 [`database/data.md`](database/data.md)

### 1.4 子组件分类

| 分类 | 包含组件 |
|------|---------|
| **管理面** | open-server、market-server、wecodesite、market-web |
| **消费网关** | api-server（数据查询/网关）、event-server（事件/回调）、connector-api（连接流执行） |
| **数据层** | MySQL `openapp`（40 表）、Redis Cluster（6 节点） |
| **前端层** | wecodesite、market-web、qiankunProject、wecodesiteDemo |

---

## 2. 技术全景

### 2.1 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| **Java / Spring Boot** | 3.4.6 / 3.5.14 | 后端 5 服务（Web 4 个 + WebFlux 1 个） |
| **MySQL** | 5.7/8.x（迁移脚本适配 5.7 collation） | 主数据库 `openapp`（192.168.3.155:3306） |
| **Flyway** | flyway-mysql + flyway-maven-plugin | 数据库迁移（open-flyway 工程） |
| **MyBatis** | 3.x（mapper-locations 配置） | open-server / api-server / market-server 数据访问 |
| **R2DBC MySQL** | r2dbc-mysql | connector-api 响应式数据访问 |
| **Redis Cluster** | 6 节点（192.168.3.201~206:6379） | 缓存 / 限流令牌桶 / 订阅列表缓存（open-server、api-server、market-server、connector-api） |
| **Spring Data Redis** | Lettuce | 响应式 / 同步 Redis 客户端 |
| **GraalJS** | polyglot 24.2.1 + js-language | 脚本节点执行（connector-api） |
| **SpringDoc OpenAPI** | springdoc-openapi | 各服务 Swagger UI（/swagger-ui.html, /api-docs） |
| **React** | 18.2 | wecodesite / market-web / qiankun 子应用 |
| **React Router** | 6.20 | 前端路由 |
| **qiankun** | 2.10.16 | 微前端框架（wecodesite 与 qiankunProject） |
| **antd** | 4.24 | UI 组件库 |
| **zustand / Redux Toolkit** | 4.4 / 2.12 | 状态管理 |
| **Vite** | 5.x | 前端构建 |
| **AntV X6 / @xyflow/react** | 12.10 | 连接流编排画布（wecodesite） |

### 2.2 服务端口与 context-path

| 服务 | 端口 | context-path | Swagger |
|------|:----:|--------------|---------|
| open-server | 18080 | /open-server | /open-server/swagger-ui.html |
| api-server | 18081 | /api-server | /api-server/swagger-ui.html |
| event-server | 18082 | /event-server | /event-server/swagger-ui.html |
| market-server | 18083 | /market-server | /market-server/swagger-ui.html |
| connector-api | 18180 | /connector-api (webflux base-path) | /connector-api/swagger-ui.html |

### 2.3 部署拓扑

```
┌───────────────────────────── 前端层 ─────────────────────────────┐
│ wecodesite (开发者控制台, qiankun 主应用)                          │
│   ├── qiankun 子应用: sub-app-b(5174) sub-app-c(8082)             │
│   │                   sub-app-d(8083) sub-app-e(5175)             │
│ market-web (市场管理后台)                                          │
│ wecodesiteDemo (静态原型)                                          │
└──────────────┬────────────────────────────────────────────────────┘
               │ HTTP
┌──────────────▼───────────────────── 管理面 (18080/18083) ─────────┐
│ open-server  ── MyBatis ──► MySQL openapp (192.168.3.155:3306)    │
│ market-server ── MyBatis ──► MySQL openapp                         │
│               └── Redis Cluster (192.168.3.201~206)               │
└──────────────┬────────────────────────────────────────────────────┘
               │ 内部调用
┌──────────────▼──────────── 消费网关 ──────────────────────────────┐
│ api-server (18081) ──► MySQL openapp / Redis Cluster              │
│ event-server (18082) ──► Redis (单机 localhost:6379 / 集群可切)   │
│   ├── SSE 通道 /sse/connect/{id}                                   │
│   ├── WebSocket 通道 /ws                                          │
│   └── WebHook 回调网关 /gateway/callbacks/invoke                   │
│ connector-api (18180, WebFlux) ── R2DBC ──► MySQL openapp         │
│   ├── 连接流执行 /api/v1/flows/{flowId}/invoke                     │
│   ├── 脚本节点 GraalJS 沙箱 (polyglot 24.2.1)                      │
│   └── Redis Reactive 集群                                          │
└────────────────────────────────────────────────────────────────────┘
```

### 2.4 跨域数据流

| 数据流 | 方向 | 说明 |
|--------|------|------|
| 能力订阅 | open-server → api-server / event-server | 消费方订阅 API/事件/回调后，由网关承载消费 |
| 数据查询 | 三方 → api-server → MySQL | DataQueryController 网关代理数据开放 |
| 连接流执行 | 外部 → connector-api → 下游连接器 | HTTP 触发同步执行编排 DAG |
| 事件发布 | 外部 → event-server → 订阅方 | EventGateway publish → SSE/WebSocket/内部消息 |
| 回调触发 | 外部 → event-server → 订阅方 | CallbackGateway invoke → 消费方 WebHook |
| 脚本引用 | connector-api → GraalJS | 脚本节点执行复杂逻辑 |
| 审批回调 | open-server/api-server → 审批平台 | ApprovalCallback 通知审批结果 |

### 2.5 架构决策记录（ADR）索引

| 编号 | 标题 | 状态 | 影响范围 |
|:--:|------|:--:|---------|
| ADR-001~008 | 能力开放平台 / 连接器平台 V1~V3 架构决策 | 已记录 | specs-tree-root 过程文档（详见 `adr-index.md`） |

---

## 3. 设计-实现一致性报告

> 代码扫描 vs specs-tree-root 设计文档（spec.md / plan.md / ADR），检测四类偏差（C1~C4）。

### 3.1 一致性摘要

| 冲突类型 | 数量 | 严重度 |
|---------|:----:|:-----:|
| C1 技术选型漂移 | 2 | 中 |
| C2 模块增删 | 1 | 中 |
| C3 API 差异 | 0 | — |
| C4 架构偏离 | 1 | 低 |

### 3.2 冲突清单

| 冲突类型 | specs-tree 记录 | 代码实际实现 | 建议操作 |
|---------|---------------|------------|---------|
| C1 技术选型漂移 | spec: 连接器平台 V3 使用 **React Flow** 编排画布（@xyflow/react） | wecodesite 依赖含 `@xyflow/react ^12.10.1`（一致）；但 qiankunProject 与 wecodesiteDemo 中存在 **AntV X6 / 独立 HTML 编辑器原型**（flow-editor.html, connector-editor.html） | 🔧 确认生产编辑器以 @xyflow 为准，原型 HTML 归为演示资产 |
| C1 技术选型漂移 | spec: 数据面认证 **AKSK/OAuth** | api-server 认证头支持 SOA/APIG/AKSK（`X-SOA-TOKEN`, `X-APIG-APPID`, `X-AKSK-TOKEN`），未发现 OAuth 授权码流程落地；FR-031 用户授权已落地为 `user_authorization` 表 + ScopeController | 🔧 OAuth 流程仅为授权模型，实际以 AKSK/SOA 凭证为主 |
| C2 模块增删 | spec: 连接器平台 V1 编排层 3 节点（触发器/连接器/数据输出），V3 新增脚本节点 | 代码 connector-api 支持 trigger/connector/script/parallel/exit 5 类节点（execution_step_t node_type），**数据输出节点并入 exit** | 🔧 更新 spec 或确认 exit 承载数据输出职责 |
| C4 架构偏离 | spec: 管理面 Spring MVC，数据面 WebFlux | open-server/api-server/market-server/event-server 均 Spring Web（一致）；connector-api 使用 WebFlux + R2DBC（数据面响应式，一致） | ✅ 与设计一致，无偏离 |

### 3.3 结论

代码实现与设计文档整体一致，未发现阻塞性偏差。存在 3 处需人工确认的差异（C1×2、C2×1），建议由设计团队裁决是否更新 specs-tree 基线。

---

## 4. 目录导航

| 文档 | 说明 |
|------|------|
| `database/docs-overview.md` | 数据库域入口（40 表索引） |
| `database/data.md` | 全部 40 张表结构（字段/索引/关联） |
| `open-server/docs-overview.md` | 能力开放平台管理面入口 |
| `open-server/api.md` | open-server API 端点清单（~80） |
| `open-server/config.md` | open-server 配置项 |
| `open-server/security.md` | 认证/安全模型 |
| `api-server/docs-overview.md` | 数据开放 + API 消费网关入口 |
| `api-server/api.md` | api-server API 端点清单 |
| `api-server/config.md` | api-server 配置项 |
| `connector-api/docs-overview.md` | 连接流执行引擎入口 |
| `connector-api/api.md` | connector-api API 端点 |
| `connector-api/config.md` | connector-api 配置项 |
| `connector-api/security.md` | 脚本沙箱安全模型 |
| `event-server/docs-overview.md` | 事件/回调网关入口 |
| `event-server/api.md` | event-server API 端点 |
| `event-server/event.md` | 事件/通道模型 |
| `market-server/docs-overview.md` | 市场管理面入口 |
| `market-server/api.md` | market-server API 端点 |
| `market-server/config.md` | market-server 配置项 |
| `frontend/docs-overview.md` | 前端工程入口 |
| `frontend/page.md` | 前端页面/路由清单 |
| `frontend/integration.md` | 微前端集成（qiankun） |
| `deploy.md` | 部署信息（拓扑/端口/环境变量） |
| `security.md` | 全系统安全模型 |
| `relation-deps.md` | 服务依赖关系 |
| `relation-flow.md` | 跨域数据流 |
| `adr-index.md` | ADR 索引 |
| `source.md` | 产物溯源（扫描信息源） |

---

## 修订记录

| 生成时间 | 变更 Feature | 生成方式 | 修订人 |
|---------|-------------|:--:|--------|
| 2026-08-03 | 全量覆盖重建（代码扫描模式） | code-scan | SDDU Docs Agent |
