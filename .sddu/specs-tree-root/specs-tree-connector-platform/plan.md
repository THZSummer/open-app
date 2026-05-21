# 技术规划：连接器平台（Connector Platform）

**Feature ID**: CONN-PLAT-001  
**规划版本**: v2.2  
**创建日期**: 2026-05-21  
**最近更新**: 2026-05-22（v2.2：connector-api 改用 Spring WebFlux + Reactor Netty NIO 非阻塞栈；架构图区分前期主路径与后期能力；v2.1：运行时独立部署为 `connector-api`，前端统一收归 `wecodesite`）  
**规划作者**: SDDU Plan Agent  
**规范版本**: spec.md v4.0  
**前置文档**: discovery-report.md (v3.1), spec.md v4.0, plan-v1.md (废弃), ADR-001~003（ADR-003 已于 v2.1 修订）

> ⚠️ **前端项目说明**：`open-web` 代码已全部迁移至 `wecodesite`，本规划中所有前端引用均以 `wecodesite` 为准。`wecodesite` 已内置 `@xyflow/react` 依赖，且 `ConnectPlatform/Connector`、`ConnectPlatform/ConnectorEditor`、`ConnectPlatform/Flow`、`ConnectPlatform/FlowEditor` 等页面已有实现。
>
> 🆕 **v2.1 架构变更**：连接器平台运行时（同步调度执行、HTTP 触发入口、调试接口）从 `open-server` 内嵌迁出，**独立部署为 `connector-api` 服务**。`open-server` 仅承载管理类能力（CRUD/版本/监控查询）以及调试代理（转发至 connector-api 调试接口）。详见 §1.1 和 ADR-003（v2.1 修订）。
>
> 🆕 **v2.2 技术栈变更**：`connector-api`（运行时服务）改用 **Spring WebFlux + Reactor Netty + WebClient（NIO 异步非阻塞栈）**，匹配运行时高并发同步 HTTP 调用三方业务系统的场景；`open-server`（管理服务）沿用 Spring MVC（Servlet）。MyBatis 同步 JDBC 通过统一 `DbAccessor` 封装 + `boundedElastic` 调度器隔离，测试环境启用 BlockHound 防误用。详见 §1.2 后端技术栈、§2.1 方案 A 核心设计、§5.1 技术风险。

---

## 1. 架构分析

### 1.1 系统架构设计

> 💡 以下架构**沿用**能力开放平台（`specs-tree-capability-open-platform/plan.md §方案D`）的微服务架构基础，并**新增独立的 `connector-api` 运行时服务**承载连接流的执行（同步调度、HTTP 触发入口、执行上下文等）。**管理类能力（连接器/连接流/监控的 CRUD）仍在 `open-server`，前端统一在 `wecodesite`**。**本版本不与能力开放平台集成**——Scope 权限复用（NG18）和审批流独立管理（NG19）移至 V1 阶段。
>
> 🎯 **前期定位（集成方向）**：连接器平台承担"**由内向外（同步主动调用）**"的角色，前期主路径是「**内部业务系统（触发方） → 连接器平台 → 三方业务系统（HTTP 接口目标）**」，例如内部 IM/云盘/审批等业务模块通过连接器把数据同步到三方 ERP/CRM/OA。外部消费方直接触发连接流、以及连接器调用内部业务系统作为提供方，作为后期能力（图中以虚线表示），与 api-server「由外向内」、event-server「由内向外（事件/回调）」共同构成完整的内↔外集成矩阵。

```mermaid
graph TB
    subgraph Frontend["前端层"]
        WeCodeSite["wecodesite<br/>(React SPA)<br/>统一前端<br/>连接器平台所有页面"]
    end

    subgraph Services["服务层"]
        subgraph OpenServer["open-server (Spring Boot)"]
            CapMgmt["能力开放模块<br/>分类/API/事件/回调<br/>权限/审批"]
            ConnectorMgmt["连接器平台管理模块 [新增]<br/>连接器/连接流/监控<br/>不含运行时"]
            AppMgmt["应用管理模块<br/>现有能力"]
            Member["成员管理模块<br/>现有能力"]
        end
        ConnectorApi["connector-api (Spring Boot) [新增]<br/>连接器运行时服务<br/>同步调度执行<br/>+ HTTP触发入口<br/>+ 调试接口"]
        ApiServer["api-server (Spring Boot)<br/>API认证鉴权服务<br/>由外向内"]
        EventServer["event-server (Spring Boot)<br/>事件/回调网关服务<br/>由内向外"]
    end

    subgraph DataLayer["数据层"]
        MySQL[(MySQL)]
        Redis1[(Redis<br/>open-server/api-server)]
    end

    subgraph PlatformGW["XX通讯平台网关"]
        ApiGW["内部API网关"]
    end

    subgraph InternalBiz["内部业务系统 [前期主触发方]"]
        BizSys1["内部业务系统A<br/>IM/云盘/审批等"]
        BizSys2["内部业务系统B"]
    end

    subgraph Providers["提供方"]
        ThirdParty1["三方业务系统<br/>ERP/CRM/OA等<br/>[前期主调用目标]"]
        ThirdParty2["三方业务系统"]
        InternalAsProvider["内部业务系统<br/>作为提供方<br/>[后期能力]"]
    end

    subgraph Consumers["外部消费方 [后期能力]"]
        Consumer1["外部消费方应用"]
    end

    %% 前端直接连接管理服务
    WeCodeSite -- "REST API 管理类" --> OpenServer

    %% open-server 调用 connector-api 提供的调试接口
    OpenServer -- "调试接口 手动调试/测试运行" --> ConnectorApi

    %% open-server 内部模块调用
    ConnectorMgmt -- "无依赖" --> CapMgmt

    %% 服务访问数据层
    OpenServer --> MySQL
    OpenServer --> Redis1
    ConnectorApi --> MySQL
    ConnectorApi --> Redis1
    ApiServer --> MySQL
    ApiServer --> Redis1

    %% === 前期主路径：内部业务系统 → connector-api → 三方业务系统 ===
    BizSys1 ==> |"HTTP触发 连接流 [前期主路径]"| ConnectorApi
    BizSys2 ==> |"HTTP触发 连接流 [前期主路径]"| ConnectorApi
    ConnectorApi ==> |"直接HTTP调用 [前期主路径]"| ThirdParty1
    ConnectorApi ==> |"直接HTTP调用 [前期主路径]"| ThirdParty2

    %% === 后期能力：外部消费方触发 & 调用内部业务系统 ===
    Consumer1 -. "HTTP触发 [后期能力]" .-> ConnectorApi
    ConnectorApi -. "直接HTTP调用 [后期能力]" .-> InternalAsProvider

    %% 现有 API 调用流程（不变）
    Consumer1 -- "API调用" --> ApiGW
    ApiGW -. "认证鉴权" .-> ApiServer
    ApiGW -- "转发请求" --> ThirdParty1
    ApiGW -- "转发请求" --> InternalAsProvider

    style Frontend fill:#e8f5e9,stroke:#2e7d32
    style Services fill:#e3f2fd,stroke:#1565c0
    style ConnectorApi fill:#fff9c4,stroke:#f57f17,stroke-width:2px
    style DataLayer fill:#f3e5f5,stroke:#7b1fa2
    style PlatformGW fill:#fff3e0,stroke:#ef6c00
    style Consumers fill:#e0f7fa,stroke:#00838f,stroke-dasharray:5 5
    style Providers fill:#fce4ec,stroke:#c2185b
    style InternalBiz fill:#fffde7,stroke:#f9a825,stroke-width:2px
    style InternalAsProvider stroke-dasharray:5 5
    style BizSys1 fill:#fff9c4
    style BizSys2 fill:#fff9c4
    style ThirdParty1 fill:#ffe0b2
    style ThirdParty2 fill:#ffe0b2
```

> 📌 **方向说明（前期定位）**：
> - **粗实线（==>）** 标识的是**前期主路径**：内部业务系统作为触发方调用连接器平台 → 连接器平台调用三方业务系统的 HTTP 接口（典型场景：内部 IM/云盘/审批等业务模块通过连接器把数据同步到三方 ERP/CRM/OA）
> - **细虚线（-.->）** 标识的是**后期能力**：外部消费方直接触发连接流、以及连接器调用内部业务系统作为提供方（仍可在 MVP 技术上支持，但前期不作为推广重点）
> - 这与 api-server「由外向内」、event-server「由内向外（事件/回调）」形成完整的"内↔外"集成矩阵，连接器平台承担"**由内向外（同步主动调用）**"的角色

**服务职责划分**：
| 服务 | 类型 | 职责 |
|------|------|------|
| **wecodesite** | 前端 | 连接器平台**所有**前端页面（连接器目录/编辑器、连接流列表/编排画布/详情、执行详情、监控面板等） |
| **open-server** | 后端管理服务 | 连接器/连接流的 CRUD 与版本管理、编排配置存储、监控查询；**手动调试/测试运行通过调用 connector-api 的调试接口完成** |
| **connector-api** | 🆕 后端运行时服务 | 同步调度执行引擎、HTTP 触发入口、执行上下文管理、节点执行器（连接器/数据处理）、**对内提供调试接口**（供 open-server 调用） |

**与连接器平台相关的现有能力**（本版本**不集成**，仅复用基础设施）：
| 现有能力 | 本版本用途 | 说明 |
|---------|----------|------|
| MySQL / Redis | 数据持久化和缓存 | open-server 与 connector-api 共享同一 MySQL/Redis 实例 |
| 三方业务系统 HTTP API（前期主目标） | 连接器的执行目标 | connector-api 直接配置目标 API 地址和认证凭证，不经 API 网关；前期重点封装 ERP/CRM/OA 等三方系统 |
| 内部业务系统 HTTP API（后期能力） | 连接器的执行目标 | 后期可扩展将内部业务系统（IM/云盘/审批等）作为提供方调用 |

> ⚠️ **本版本独立运行**：连接器平台本版本不与能力开放平台集成（§5.4）。Scope 权限复用（NG18）和审批流独立管理（NG19）移至 V1。

**现有代码引用**:
| 代码位置 | 说明 |
|---------|------|
| `open-server/src/main/java/com/xxx/open/modules/` | 现有能力开放模块（category/api/event/callback/permission/approval），连接器平台在本版本中**不依赖**这些模块 |
| `connector-api/` | 🆕 新增独立 Spring Boot 工程，承载运行时与调试接口 |
| `wecodesite/src/pages/ConnectPlatform/` | 已有连接器目录（Connector）、连接器编辑器（ConnectorEditor）、连接流列表/编排画布（Flow/FlowEditor）页面，本版本继续扩展（新增详情/执行详情/监控等页面） |

### 1.2 技术栈确认

> 沿用能力开放平台（`specs-tree-capability-open-platform/plan.md §1.4`）的技术栈标准。

#### 前端技术栈

| 层级 | 技术选型 | 版本 |
|------|----------|------|
| **框架** | React | ^18.2.0 |
| **UI 组件库** | Ant Design | ^4.x |
| **构建工具** | Vite | ^5.0.0 |
| **CSS 预处理器** | Less | ^4.2.0 |
| **样式方案** | Less Module（`.m.less` / `.less`） | - |
| **状态管理** | thunk.js 模式（现有） | - |
| **编排画布** | @xyflow/react (React Flow) | ^12.x（wecodesite 已内置） |

#### 后端技术栈

> 📌 **服务分栈策略**：`open-server`（管理服务）沿用现有的 **Spring MVC（同步 Servlet 栈）**；`connector-api`（运行时服务）采用 **Spring WebFlux + Reactor Netty（NIO 异步非阻塞栈）**，匹配运行时高并发同步 HTTP 调用场景。

| 层级 | open-server（管理服务） | connector-api（运行时服务） | 说明 |
|------|------------------------|---------------------------|------|
| **语言** | Java 21 | Java 21 | 一致 |
| **构建工具** | Maven 3.9.x | Maven 3.9.x | 一致 |
| **应用框架** | Spring Boot 3.4.6 | Spring Boot 3.4.6 | 一致 |
| **Web 栈** | **Spring MVC**（spring-boot-starter-web，Tomcat Servlet 同步） | **Spring WebFlux**（spring-boot-starter-webflux，Reactor Netty NIO 异步非阻塞） | 🆕 运行时改用 WebFlux |
| **HTTP 客户端**（调用下游 API） | RestTemplate（现有） | **WebClient**（reactive，基于 Reactor Netty） | 🆕 与 WebFlux 栈一致，端到端非阻塞 |
| **数据访问** | MyBatis（mybatis-spring-boot-starter 3.0.4，同步 JDBC） | MyBatis（同步 JDBC，包在 `boundedElastic` 调度器隔离）；执行记录的异步写入使用 `Mono.fromCallable(...).subscribeOn(Schedulers.boundedElastic())` | 🆕 WebFlux 下 JDBC 需调度器隔离避免阻塞事件循环；后续可评估迁移到 R2DBC |
| **数据库** | MySQL 5.7 | MySQL 5.7 | 共享同一实例 |
| **缓存** | Redis 6.0（spring-data-redis，同步） | Redis 6.0（**spring-data-redis-reactive**，Lettuce reactive 驱动） | 🆕 与 WebFlux 栈一致 |
| **并发模型** | 一请求一线程（Tomcat 线程池，默认 200） | 事件循环 + Reactor 调度器（少量 EventLoop 线程承接百级以上并发 HTTP 调用） | 🆕 IO 密集型场景吞吐量显著优于同步栈 |

> ❌ **本版本移除的依赖**：~~能力开放平台 Scope 权限模型~~、~~审批引擎~~、~~事件网关~~、~~Quartz 定时调度~~（触发器不在此版本内）
> ✅ **仅复用**：MySQL / Redis 基础设施
> 🆕 **运行时服务新增依赖**：`spring-boot-starter-webflux`、`spring-boot-starter-data-redis-reactive`、`reactor-core`（随 Spring Boot 自带）

> 💡 **为什么 connector-api 选 WebFlux？**
> - **场景匹配**：运行时核心动作是「同步 HTTP 调用三方业务系统」并发会等待下游响应，是典型 IO 密集型场景，NIO 非阻塞模型可用极少的线程承接高并发触发
> - **吞吐量**：同等硬件资源下，WebFlux + WebClient 较 Spring MVC + RestTemplate 在高并发 HTTP 转发场景下吞吐量提升通常在 2-5 倍，更易达到 NFR 并发指标
> - **背压（Backpressure）**：Reactor 的背压机制天然适配「上游触发速度 vs 下游响应速度」的速率匹配，避免连接器平台被慢下游打垮
> - **不影响调用方编程模型**：connector-api 对外仍暴露**同步 HTTP 端点**（请求-响应一一对应），调用方（包括 open-server debug-proxy、内部业务系统）无需感知内部 reactive 实现
> - **隔离风险**：WebFlux 仅用于 connector-api，open-server 不动，避免对现有管理后台造成栈级风险
>
> ⚠️ **WebFlux 注意事项（已在 plan-code 沉淀为强制规则）**：
> - 严禁在 reactive 链路中直接调用阻塞 API（JDBC/同步 Redis/Thread.sleep）；MyBatis 调用必须包裹在 `subscribeOn(Schedulers.boundedElastic())`
> - 节点执行器（`NodeExecutor`）签名返回 `Mono<NodeOutput>`，而非同步返回值
> - `WebClient` 必须配置超时（连接 / 读 / 写）和最大内存缓冲（防大响应体打爆堆）
> - 异常处理使用 `.onErrorResume(...)`，避免裸 try-catch 吃掉 reactive 异常信号

### 1.3 连接器平台新增组件

```mermaid
graph TB
    subgraph Front["前端 wecodesite — 新增页面/补充"]
        ConnDir["连接器目录<br/>浏览/搜索/过滤"]
        ConnForm["连接器创建/编辑<br/>基本信息+连接配置"]
        ConnDetail["连接器详情<br/>版本历史+配置详情"]
        FlowList["连接流列表<br/>浏览/搜索/管理"]
        FlowCanvas["连接流编排画布<br/>可视化拖拽编排"]
        FlowDetail["连接流详情<br/>概览+运行状态<br/>+版本历史+执行记录"]
        ExecDetail["执行详情<br/>步骤详情+返回值"]
        MonitorPanel["监控面板<br/>执行历史查询"]
    end

    subgraph BackendMgmt["open-server — 新增管理模块"]
        Connector["connector 模块<br/>连接器 CRUD + 版本管理"]
        Flow["flow 模块<br/>连接流 CRUD + 版本管理 + 编排配置"]
        MonitorModule["monitor 模块<br/>执行记录查询 + 统计"]
        DebugProxy["debug-proxy 模块 [新增]<br/>调用 connector-api 调试接口<br/>手动调试/测试运行"]
    end

    subgraph BackendRuntime["connector-api — 独立运行时服务 [新增]"]
        Runtime["runtime 模块<br/>同步调度执行 + 执行上下文<br/>+ 节点执行器"]
        HttpTrigger["http-trigger 模块<br/>HTTP 触发入口<br/>对外同步调用端点"]
        DebugApi["debug-api 模块 [新增]<br/>调试接口<br/>供 open-server 调用"]
    end

    subgraph Existing["现有基础设施 — 复用"]
        MySQL_db[(MySQL)]
        Redis_cache[(Redis)]
    end

    HTTP_Providers["三方业务系统 HTTP API<br/>[前期主目标]<br/>内部业务系统 API 为后期能力"]

    Front -- "HTTP 管理类" --> BackendMgmt
    BackendMgmt -- "内部 HTTP 调试/测试运行" --> BackendRuntime
    BackendMgmt --> MySQL_db
    BackendMgmt --> Redis_cache
    BackendRuntime --> MySQL_db
    BackendRuntime --> Redis_cache
    Runtime -- "同步HTTP调用" --> HTTP_Providers
    HttpTrigger -- "同步调度" --> Runtime
    DebugApi -- "同步调度" --> Runtime
```

> 💡 **运行时单独部署的理由**：
> - **职责隔离**：管理类操作（CRUD）与运行时执行（高并发、长耗时同步调用）资源特征不同，独立部署便于针对性扩缩容
> - **故障隔离**：运行时阻塞或异常不影响管理后台的可用性
> - **演进友好**：V1 引入异步执行/MQS 时只需改造 connector-api，open-server 保持稳定
> - **调试接口收口**：手动调试、测试运行等流程统一通过 connector-api 提供的调试接口完成，避免运行时逻辑在 open-server 与 connector-api 重复实现

### 1.4 数据流分析

**连接器发布流程**:
```
管理员创建连接器基本信息 → 配置连接配置(协议/认证/参数Schema/超时/限流) → 
保存草稿(创建首个版本) → 发布(输入版本号) → 版本可用
发布无需审批（NG19移至V1）
```

**连接流创建与执行流程**:
```
管理员在 wecodesite 创建连接流 → 进入编排画布 → 配置HTTP/手动入口触发器 →
添加连接器节点(引用已发布连接器版本) → 添加数据处理节点(字段映射) →
配置出口节点 → 保存草稿 → 发布(输入版本号) → 部署上线
↑（编排/发布走 open-server 管理接口）

HTTP触发 → connector-api 同步执行连接流 → 返回完整结果
手动触发 → wecodesite 调用 open-server → open-server 调用 connector-api 调试接口 → 同步执行 → 展示完整结果
测试运行 → wecodesite 调用 open-server → open-server 调用 connector-api 调试接口 → 同步执行 → 展示完整结果
```

**运行时数据流（一次同步执行，发生在 connector-api 进程内）**:
```
HTTP请求 (前期：内部业务系统；后期：外部消费方) / 调试请求 (来自 open-server)
  → connector-api 调度器创建 ExecutionContext (含触发数据，当前请求线程)
  → 节点1(入口): 透传触发数据
  → 节点2(连接器): 读取上游数据 → 同步调用三方业务系统 HTTP API（前期主目标；后期可调用内部业务系统）→ 输出数据到上下文
  → 节点3(数据处理): 读取上游数据 → 字段映射转换 → 输出数据到上下文
  → 节点4(出口): 定义返回值
  → 返回完整执行结果(各步骤输入/输出/耗时/状态)
  → 异步写入执行记录到MySQL（不阻塞返回，供 open-server monitor 模块查询）
```

### 1.5 依赖关系图

```mermaid
graph LR
    subgraph WeCodeSite["wecodesite (前端)"]
        UI["连接器平台全部页面"]
    end

    subgraph OpenSvr["open-server (管理服务)"]
        ConnMgmt["连接器管理"]
        FlowMgmt["连接流管理"]
        MonitorLog["监控日志查询"]
        DebugProxy["调试代理<br/>手动调试/测试运行"]
    end

    subgraph ConnApi["connector-api (运行时服务)"]
        RuntimeExe["运行时执行<br/>同步"]
        HttpTriggerSvc["HTTP 触发入口"]
        DebugSvc["调试接口"]
    end

    subgraph TriggerSrc["触发方"]
        BizSys["内部业务系统<br/>IM/云盘/审批等<br/>[前期主触发方]"]
        Consumer["外部消费方<br/>[后期能力]"]
    end

    subgraph TargetSys["目标系统"]
        ThirdSys["三方业务系统<br/>ERP/CRM/OA等<br/>[前期主调用目标]"]
        InternalAsTarget["内部业务系统<br/>作为提供方<br/>[后期能力]"]
    end

    subgraph Infra["基础设施"]
        DB[(MySQL)]
        Cache[(Redis)]
    end

    UI -- "REST" --> ConnMgmt
    UI -- "REST" --> FlowMgmt
    UI -- "REST" --> MonitorLog
    UI -- "REST 手动调试/测试运行" --> DebugProxy
    DebugProxy -- "内部HTTP" --> DebugSvc

    %% === 前期主路径（粗实线）===
    BizSys ==> |"HTTP触发 [前期主路径]"| HttpTriggerSvc
    RuntimeExe ==> |"直接HTTP调用 [前期主路径]"| ThirdSys

    %% === 后期能力（虚线）===
    Consumer -. "HTTP触发 [后期能力]" .-> HttpTriggerSvc
    RuntimeExe -. "直接HTTP调用 [后期能力]" .-> InternalAsTarget

    HttpTriggerSvc --> RuntimeExe
    DebugSvc --> RuntimeExe
    ConnMgmt --> DB
    FlowMgmt --> DB
    MonitorLog --> DB
    RuntimeExe --> DB
    RuntimeExe --> Cache

    style BizSys fill:#fff9c4,stroke:#f9a825,stroke-width:2px
    style ThirdSys fill:#ffe0b2,stroke:#ef6c00,stroke-width:2px
    style Consumer stroke-dasharray:5 5
    style InternalAsTarget stroke-dasharray:5 5
```

> 📌 **方向说明（前期定位）**：
> - **粗实线（==>）** = 前期主路径：内部业务系统作为触发方 → 连接器平台 → 三方业务系统作为调用目标
> - **细虚线（-.->）** = 后期能力：外部消费方触发、连接器调用内部业务系统作为提供方

### 1.6 核心业务对象关系

连接器平台围绕 **8 个核心业务对象**组织，对象间关系：

| 关系 | 说明 |
|------|------|
| Connector → ConnectorVersion | 一个连接器有多个版本（1:N），发布时快照基本信息+连接配置 |
| ConnectorVersion → FlowNode | 连接器版本被连接流节点引用（1:N） |
| ConnectorVersion → ConnectorAuthConfig | 连接器版本按消费方应用存储独立认证凭证（1:N） |
| Flow → FlowVersion | 一个连接流有多个版本（1:N），发布时快照基本信息+编排配置 |
| FlowVersion → FlowNode / FlowEdge | 版本包含节点和连线（1:N），节点含 entry/connector/data_processor/exit 四类（MVP: connector + data_processor） |
| Flow → ExecutionRecord → ExecutionStep | 每次执行生成一条记录，记录含多个步骤（1:N:N） |

> 完整 ER 图（含字段定义）详见 **§4.2 数据库设计** 及 `plan-db.md`

---

## 2. 方案对比

### 2.1 方案 A：轻量同步执行引擎（推荐）

**方案描述**: **运行时单独部署为 `connector-api` 服务**（与 open-server 进程隔离），采用轻量级**同步**执行引擎。连接流编排配置以 JSON 存储（由 open-server 写入 MySQL），运行时引擎接收 HTTP 触发请求（或来自 open-server 的调试请求）后，在当前线程中按节点顺序依次同步执行，执行完成后返回完整结果，之后异步写入执行记录。**open-server 的手动调试/测试运行流程通过调用 connector-api 的调试接口实现**。

```mermaid
sequenceDiagram
    participant Client as 消费方/管理员
    participant Web as wecodesite
    participant OpenSvr as open-server (管理服务)
    participant ConnApi as connector-api (运行时服务)
    participant Redis as Redis
    participant MySQL as MySQL
    participant Target as 目标 API

    alt 外部 HTTP 触发
        Client->>ConnApi: HTTP 触发请求
    else 手动调试 / 测试运行
        Client->>Web: 点击调试/测试运行
        Web->>OpenSvr: REST 调用
        OpenSvr->>ConnApi: 内部 HTTP 调用调试接口
    end

    Note over ConnApi: 创建 ExecutionContext

    ConnApi->>ConnApi: 节点1 入口：透传触发数据

    ConnApi->>Target: 节点2 连接器：同步调用 API
    Target-->>ConnApi: API 响应

    ConnApi->>ConnApi: 节点3 数据处理：字段映射

    ConnApi->>ConnApi: 节点4 出口：构造返回值

    ConnApi-->>Client: 同步返回执行结果（含各步骤详情）
    Note right of ConnApi: 调试/测试运行场景下<br/>结果沿 open-server → web 回传

    par 异步写入
        ConnApi->>Redis: 缓存执行记录（加速查询）
        ConnApi->>MySQL: 持久化执行记录/步骤
    end
```

**核心设计**:
- **服务拆分**: `connector-api` 独立 Spring Boot 工程，仅承载运行时与调试接口；`open-server` 承载所有管理类能力（CRUD/版本/监控查询）
- **编排层**: FlowVersion 的 orchestration_config 以 JSON 格式存储完整编排信息（由 open-server 写入，connector-api 只读）
- **执行引擎**: 反应式顺序执行器（`ReactiveSequentialExecutor`），从入口节点开始构造 `Mono` 链路（`flatMap` 串联各节点 `Mono<NodeOutput>`），最后聚合为 `Mono<ExecutionResult>`；对 HTTP 调用方仍呈现为**同步请求-响应语义**（一次请求等到完整结果再返回）
- **调度**: 无消息队列——HTTP 触发请求进入 connector-api 的 Reactor Netty EventLoop，由执行引擎构造 reactive 链路（`Mono<ExecutionResult>`）异步编排各节点；下游 HTTP 调用通过 WebClient 完全非阻塞，单实例百级并发触发可由少量 EventLoop 线程承接，**对调用方仍呈现为同步 HTTP 请求-响应**
- **调试通道**: connector-api 暴露内部调试接口（仅限 open-server 内网调用），open-server 收到前端调试/测试运行请求后转发到该接口，避免运行时逻辑重复实现
- **认证凭证**: 连接器版本中存储认证配置（plan-db 设计为独立表），运行时在 connector-api 进程内自动加载注入

**优点**:
- 运行时独立部署（connector-api），与管理类操作进程隔离，**资源/故障/扩缩容互不影响**
- 调试接口收口在 connector-api，避免运行时逻辑在 open-server / connector-api 两边重复
- 无额外框架依赖（无 MQS，无 Quartz），团队熟悉现有技术栈
- 同步执行模型简化了所有数据流——无需处理异步回调/状态查询
- 执行上下文清晰，调试简单——单线程顺序执行，输入输出可追踪
- 执行性能可预测（线性 O(n) 复杂度）
- 可平滑演进到 V1（增加异步分支/循环时只需改造 connector-api，open-server 保持稳定）

**缺点**:
- 新增一个独立服务（connector-api），运维实例数 +1
- open-server 与 connector-api 之间需新增一条内部 HTTP 调用链路（调试接口），需做好鉴权与重试
- 长时间运行的连接流虽然不会阻塞 EventLoop（WebFlux 非阻塞），但单条连接流执行端到端超时仍需控制（避免下游慢响应占用 WebClient 连接池资源），通过 WebClient 超时 + Reactor `.timeout(...)` 双重保障
- 高并发场景下，单实例执行器可能成为瓶颈（MVP 阶段目标：≥10 并发，可接受；可通过水平扩容 connector-api 实例缓解）
- 缺乏标准化的流程定义格式（非 BPMN 标准）
- 复杂编排场景（并行/分支/循环）需要在 V1 重构执行器

**风险评估**: 低 — MVP 范围明确（仅线性同步执行），技术复杂度可控

**预估工作量**: 8-12 周 (3-4 后端 + 2-3 前端 + 1 QA)

### 2.2 方案 B：Spring StateMachine 状态机引擎

**方案描述**: 引入 Spring StateMachine 作为流程引擎核心，将连接流执行抽象为状态转换。每个连接器节点对应一个状态，节点执行完成触发状态转换。同步模式下，状态机实例在请求线程中运行。

**优点**:
- 状态机理论成熟，状态转换清晰
- Spring StateMachine 与现有 Spring Boot 技术栈集成良好
- 可扩展性强，后续分支/循环可通过嵌套状态机实现

**缺点**:
- 对于 MVP 的线性同步编排场景，状态机**过度设计**
- 学习曲线：团队需要学习状态机概念和框架 API
- 嵌套状态机复杂度随分支/循环快速上升
- 状态机实例管理增加开发复杂度
- 调试困难：状态转换链路过长时难以追踪

**风险评估**: 中 — 框架引入增加不确定性

**预估工作量**: 10-14 周 (3-4 后端 + 2-3 前端 + 1 QA)

### 2.3 方案 C：消息驱动引擎（本版本不适用）

> ⚠️ **本版本已无异步调度需求**（spec v4.0 确定同步执行），消息驱动引擎方案不适用于本版本。

**方案描述**: 以消息队列为核心，将每个连接器节点封装为独立的消息消费者，流程执行为消息在消费者间的流转。

**不适用的理由**:
- spec v4.0 明确同步执行（FR-021/FR-022），消息驱动引入不必要的异步延迟
- 同步场景下消息队列的序列化/反序列化开销反而降低性能
- 本版本无事件/定时触发，消息队列无必要
- 增加运维复杂度

**风险评估**: 高 — 与同步执行需求不匹配

**预估工作量**: 不推荐

### 2.4 综合对比矩阵

| 对比维度 | 方案 A 轻量同步 | 方案 B 状态机 | 方案 C 消息驱动 |
|---------|:--------------:|:------------:|:--------------:|
| MVP 开发周期 | **8-12 周** ⭐ | 10-14 周 | ❌ 不适用 |
| 技术复杂度 | **低** ⭐ | 中 | 高 |
| 与现有架构兼容性 | **高** ⭐ | 中 | 中 |
| 线性同步执行支持 | **原生** ⭐ | 过度设计 | 反向设计 |
| 分支/循环扩展性 | 需重构 | **自然支持** ⭐ | 需扩展 |
| 可调试性 | **高** ⭐ | 中 | 低 |
| 运维复杂度 | **低** ⭐ | 低 | 中 |
| 团队学习成本 | **无** ⭐ | 1-2 周 | 1 周 |

---

## 3. 推荐方案

### 推荐: 方案 A - 轻量同步执行引擎

**推荐理由**:

1. **MVP 范围精确匹配**: 规范明确 MVP 仅支持**同步**线性编排（HTTP/手动触发器 → 连接器节点/数据处理节点 → 流出口节点），顺序同步执行器是满足需求的最简方案。

2. **同步执行简化架构**: 相比 spec v3.x 的异步执行模型，v4.0 的同步执行大幅降低了运行时复杂度——无需消息队列、无需事件订阅、无需状态轮询。执行结果直接通过 HTTP 响应返回，调用方无需等待异步回调。

3. **最小化技术债务**: 不引入额外框架（Spring StateMachine / MQS），使用纯 Java 实现，与现有架构一致；同时**通过将运行时拆为独立服务 `connector-api`，避免管理与执行混在同一进程**导致后期难以拆分。

4. **开发效率最优**: 团队可在现有 open-server 中新增 connector/flow/monitor 三个管理模块，同时启动 `connector-api` 新工程（基于现有 Spring Boot 脚手架）承载 runtime/http-trigger/debug-api 模块，复用已有的 MyBatis/MySQL/Redis 基础设施；前端所有页面统一落在 `wecodesite`，无需协调多前端项目。

5. **调试友好**: 同步执行的每步输入/输出清晰，测试运行时可逐步验证，排查问题直观。

6. **渐进式演进路径**: MVP→V1 时，可通过增加节点类型处理逻辑扩展分支/循环能力，如需异步能力再引入 MQS。

### 关键架构决策概览

| 决策点 | 选择 | 理由 |
|-------|------|------|
| 流引擎 | 轻量同步执行器（自研） | MVP 仅需线性同步编排，最简方案 |
| 编排画布 | React Flow (@xyflow/react) | React-native, 轻量, TS 支持好 |
| **运行时部署** | **独立服务 `connector-api`**（与 open-server 分离） | 职责隔离、故障隔离、独立扩缩容、便于 V1 演进 |
| 调试/测试运行通道 | open-server 调用 connector-api 内部调试接口 | 运行时逻辑收口在 connector-api，避免双份实现 |
| 前端归属 | 全部页面统一放在 wecodesite | 用户侧只对接一个前端入口 |
| 执行模型 | **同步**（请求线程内执行） | spec v4.0 明确同步执行 |
| 触发方式 | HTTP + 手动（同步） | MVP 范围限定 |
| 执行上下文 | 方法调用参数传递（运行时）+ MySQL 持久化 | 同步执行无需 Redis 缓存上下文 |
| 执行记录持久化 | MySQL（异步写入，不阻塞返回） | 确保执行结果快速返回 |
| 凭证明文存储 | AES-256 加密存储, 界面脱敏 | 满足 NFR-010 安全要求 |
| HTTP 触发入口 | connector-api 新增 controller | 与运行时同进程，链路最短 |
| **connector-api Web 栈** | **Spring WebFlux + Reactor Netty + WebClient** | 运行时高并发同步 HTTP 调用三方系统，NIO 非阻塞栈吞吐量显著优于 Servlet 同步栈；对调用方仍呈现同步语义 |
| open-server Web 栈 | 沿用 Spring MVC（Servlet） | 管理类操作以 CRUD 为主，并发不高，沿用现有栈避免改造成本 |
| Scope 权限 | **不集成**（移至 NG18，V1） | 本版本独立运行 |
| 审批流程 | **不集成**（移至 NG19，V1） | 版本发布无需审批 |
| 数据处理节点 | **加入 MVP** | spec v4.0 将数据处理节点纳入 MVP 范围 |

---

## 4. 模块与文件概览

> **职责说明**：本章仅描述「有哪些内容」和「详细设计在哪个文件」。具体设计（表名/字段/索引、API 路径/参数/响应、页面路由/组件树/交互）均只在对应子文档中定义，plan.md 不重复。

### 4.1 模块划分

| 模块 | 所属项目 | 类型 | 说明 |
|------|---------|------|------|
| **connector** | open-server | 新增模块 | 连接器管理 — CRUD、版本管理、连接配置管理 |
| **flow** | open-server | 新增模块 | 连接流管理 — CRUD、版本管理、编排配置 |
| **monitor** | open-server | 新增模块 | 监控日志 — 执行历史查询 |
| **debug-proxy** | open-server | 新增模块 | 调试代理 — 接收前端手动调试/测试运行请求并转发至 connector-api 调试接口 |
| **runtime** | **connector-api** 🆕 | 新增模块 | 运行时 — 同步调度执行、执行上下文、节点执行器（连接器/数据处理） |
| **http-trigger** | **connector-api** 🆕 | 新增模块 | HTTP 触发入口 — 对外暴露同步触发端点 |
| **debug-api** | **connector-api** 🆕 | 新增模块 | 调试接口 — 供 open-server 内部调用（手动调试/测试运行） |
| **connector** | wecodesite | 已有页面组 | 连接器目录/创建编辑/详情（已有实现，需补充） |
| **flow** | wecodesite | 已有页面组 | 连接流列表/编排画布/详情/执行详情（已有实现，需补充） |
| **monitor** | wecodesite | 新增页面组 | 执行历史查询面板 |

> 各模块的**完整数据库表设计**详见 `plan-db.md`  
> 各模块的**完整 API 接口设计**详见 `plan-api.md`  
> 各页面组的**完整前端设计**详见 `plan-page.md`  
> **代码规范**（16 条强制规则，沿用能力开放平台标准）详见 `plan-code.md`

### 4.2 数据库设计

共 **9 张表**，按模块归属：connector 模块 2 张、flow 模块 4 张、runtime 模块 3 张。统一使用 `cp_` 前缀，涵盖连接器基本信息/版本、连接流基本信息/版本/节点/连线、执行记录/步骤、认证凭证。

**核心 ER 关系**（详细字段定义、索引、JSON Schema 见 `plan-db.md`）：

```mermaid
erDiagram
    Connector ||--o{ ConnectorVersion : has
    ConnectorVersion ||--o{ FlowNode : referenced_as
    ConnectorVersion ||--o{ ConnectorAuthConfig : has_per_app_credentials
    Flow ||--o{ FlowVersion : has
    FlowVersion ||--o{ FlowNode : contains
    FlowVersion ||--o{ FlowEdge : contains
    Flow ||--o{ ExecutionRecord : generates
    ExecutionRecord ||--o{ ExecutionStep : contains

    Connector {
        string id PK
        string name "连接器名称"
        string icon "图标URL"
        string description "描述"
        string connector_type "HTTP(MVP)"
        string status "active/disabled"
    }
    ConnectorVersion {
        string id PK
        string connector_id FK
        string version_no "版本号"
        string status "draft/published"
        json basic_info_snapshot "基本信息快照"
        json connection_config "连接配置(协议/认证/参数Schema/超时/限流)"
    }
    Flow {
        string id PK
        string name "连接流名称"
        string description "描述"
        string status "enabled/disabled"
    }
    FlowVersion {
        string id PK
        string flow_id FK
        string version_no "版本号"
        string status "draft/published"
        json basic_info_snapshot "基本信息快照"
        json orchestration_config "编排配置(入口/节点/连线/出口)"
    }
    FlowNode {
        string id PK
        string version_id FK
        string node_type "entry/connector/data_processor/exit"
        string connector_version_id FK "nullable"
        json config "节点配置"
    }
    FlowEdge {
        string id PK
        string version_id FK
        string source_node_id
        string target_node_id
        json data_mappings "字段映射配置"
    }
    ExecutionRecord {
        string execution_id PK
        string flow_id FK
        string version_id FK
        string trigger_type "http/manual/test"
        string status "success/failed/timeout"
        json trigger_data
        json result_data
    }
    ExecutionStep {
        string id PK
        string execution_id FK
        string node_id
        string node_name
        string status "success/failed"
        json input_data
        json output_data
    }
    ConnectorAuthConfig {
        string id PK
        string connector_version_id FK
        string app_id "消费方应用ID"
        string auth_type "AKSK/OAUTH2/BASIC_AUTH/API_KEY"
        json encrypted_credentials "AES-256-GCM加密"
        string status "active/expired/revoked"
    }
```

> 表结构定义、字段类型、索引、JSON Schema 详见 **`plan-db.md`**

### 4.3 API 接口设计

共 **14 个逻辑分组**（约 25 个 HTTP 端点），按服务归属：
- **open-server**: connector 模块 5 组、flow 模块 4 组、monitor 模块 2 组、debug-proxy 模块（对前端暴露的"手动调试/测试运行"等接口，内部转发至 connector-api）
- **connector-api**: http-trigger 模块（对外同步触发）、debug-api 模块（对内调试接口）

覆盖连接器/连接流的 CRUD、版本管理、连接配置/编排配置编辑与发布、HTTP 触发执行、手动触发执行、测试运行、执行历史查询等全部功能。

> **与 spec v3.x 版 plan 的差异**：
> - ❌ 移除 Scope 权限集成接口
> - ❌ 移除审批集成接口  
> - ❌ 移除事件/定时/Webhook 触发接口
> - 🔄 HTTP 触发改为同步返回（非异步 202）
> - 🔄 执行状态查询接口简化（同步执行无需轮询）
> - ✅ 新增同步执行接口（HTTP 触发 + 手动触发 + 测试运行均同步返回）
> - ✅ 新增数据处理节点配置接口
> - 📋 FR 编号更新为 v4.0 的 FR-001~FR-025

> 完整端点定义、请求/响应 Schema、错误码、鉴权方式详见 **`plan-api.md`**

### 4.4 前端页面设计

共 **8 个页面**，5 个已有实现（wecodesite `ConnectPlatform/` 目录下）+ 3 个需新增（连接流详情、执行详情、监控面板）。覆盖连接器目录/编辑器、连接流列表/编排画布/详情、执行详情、监控面板六大场景。

> **与 spec v3.x 版 plan 的差异**：
> - 🔄 编排画布：触发器仅支持 HTTP + 手动（移除事件/定时/Webhook 配置面板）
> - 🔄 编排画布 MVP 节点类型：连接器节点 + **数据处理节点**（新增）
> - 🔄 监控面板简化为执行历史查询（移除复杂的运行指标统计）
> - ❌ 移除审批相关页面组件
> - ❌ 移除 Scope 权限配置相关组件

> 页面布局、组件树、交互流程、路由设计、状态管理（thunk.js）详见 **`plan-page.md`**

### 4.5 目录结构规划

```
open-app/
├── open-server/                                 # 后端管理服务（现有工程扩展）
│   └── src/main/java/com/xxx/open/modules/
│       ├── category/              # 现有：分类管理
│       ├── api/                   # 现有：API 管理
│       ├── event/                 # 现有：事件管理
│       ├── callback/              # 现有：回调管理
│       ├── permission/            # 现有：权限管理（本版本不依赖）
│       ├── approval/              # 现有：审批管理（本版本不依赖）
│       ├── connector/             # 🆕 连接器管理模块
│       ├── flow/                  # 🆕 连接流管理模块
│       ├── monitor/               # 🆕 监控模块
│       └── debug/                 # 🆕 调试代理模块（转发至 connector-api 调试接口）
│
├── connector-api/                                # 🆕 连接器运行时服务（新增独立工程）
│   └── src/main/java/com/xxx/connector/
│       ├── runtime/               # 🆕 同步调度执行引擎、执行上下文、节点执行器
│       ├── trigger/               # 🆕 HTTP 触发入口（对外）
│       ├── debug/                 # 🆕 调试接口（对内，供 open-server 调用）
│       └── common/                # 🆕 公共组件（认证凭证加解密、共享 entity/mapper）
│
├── wecodesite/                                   # 前端应用（连接器平台所有页面）
│   └── src/pages/ConnectPlatform/
│       ├── Connector/             # ✅ 已有：连接器目录页面
│       ├── ConnectorEditor/       # ✅ 已有：连接器创建/编辑页面
│       ├── Flow/                  # ✅ 已有：连接流列表
│       ├── FlowEditor/            # ✅ 已有：编排画布
│       │   ├── DataMappingDialog.jsx # 🆕 需新增：数据映射弹窗
│       │   ├── TestRunDialog.jsx  # 🆕 需新增：测试运行弹窗
│       ├── FlowDetail.jsx         # 🆕 需新增：连接流详情
│       ├── ExecutionDetail.jsx    # 🆕 需新增：执行详情
│       └── Monitor/               # 🆕 需新增：监控面板
│           ├── MonitorDashboard.jsx
│           ├── index.jsx
│           ├── constants.jsx
│           └── thunk.js
```

### 4.6 服务职责详表

| 服务 | 新增模块 | 职责 | 数据存储 | 端口 | 上下文根 | 依赖 |
|------|---------|------|----------|------|----------|------|
| **open-server** | connector | 连接器 CRUD、版本管理、连接配置管理 | MySQL + Redis(共享) | 18080 | /open-server | 无（不依赖其他模块） |
| **open-server** | flow | 连接流 CRUD、版本管理、编排配置存储 | MySQL + Redis(共享) | 18080 | /open-server | connector 模块（引用连接器版本） |
| **open-server** | monitor | 执行历史查询、统计（读取 connector-api 写入的执行记录） | MySQL + Redis(共享) | 18080 | /open-server | 共享 MySQL 中由 connector-api 写入的数据 |
| **open-server** | debug | 调试代理：接收前端手动调试/测试运行请求并转发至 connector-api | — | 18080 | /open-server | connector-api（内部 HTTP） |
| **connector-api** 🆕 | runtime | 同步调度执行、执行上下文、节点执行器（连接器/数据处理） | MySQL + Redis(共享) | 18180 | /connector-api | flow 数据（只读 MySQL 中的 orchestration_config） |
| **connector-api** 🆕 | http-trigger | 对外 HTTP 触发入口，同步调用执行引擎 | — | 18180 | /connector-api | runtime 模块 |
| **connector-api** 🆕 | debug-api | 内部调试接口，供 open-server 的 debug 模块调用 | — | 18180 | /connector-api | runtime 模块 |

> **部署说明**：
> - `open-server` 部署 connector/flow/monitor/debug 四个管理类模块，端口 18080
> - `connector-api` 为**新增独立 Spring Boot 工程**，部署 runtime/http-trigger/debug-api 三个运行时模块，端口建议 18180（可调整），上下文根 `/connector-api`
> - 两个服务**共享同一 MySQL 与 Redis 实例**，通过数据库实现状态共享（编排配置、执行记录）
> - `open-server → connector-api` 走内网 HTTP，建议加共享 token 鉴权（具体方案详见 plan-api.md）

### 4.7 文件清单

#### open-server — connector 模块

| 文件 | 说明 |
|------|------|
| `modules/connector/ConnectorController.java` | 连接器 CRUD |
| `modules/connector/ConnectorService.java` | 连接器业务逻辑 |
| `modules/connector/ConnectorVersionController.java` | 版本管理（列表/详情/编辑/发布） |
| `modules/connector/ConnectorVersionService.java` | 版本业务逻辑 |
| `modules/connector/entity/Connector.java` | 连接器实体 |
| `modules/connector/entity/ConnectorVersion.java` | 连接器版本实体 |
| `modules/connector/mapper/ConnectorMapper.java` | 连接器 Mapper |
| `modules/connector/mapper/ConnectorVersionMapper.java` | 版本 Mapper |

#### open-server — flow 模块

| 文件 | 说明 |
|------|------|
| `modules/flow/FlowController.java` | 连接流 CRUD、部署/启停 |
| `modules/flow/FlowService.java` | 连接流业务逻辑 |
| `modules/flow/FlowVersionController.java` | 版本管理、编排配置保存/发布 |
| `modules/flow/FlowVersionService.java` | 版本业务逻辑 |
| `modules/flow/entity/Flow.java` | 连接流实体 |
| `modules/flow/entity/FlowVersion.java` | 连接流版本实体 |
| `modules/flow/entity/FlowNode.java` | 流节点实体 |
| `modules/flow/entity/FlowEdge.java` | 流连线实体 |
| `modules/flow/mapper/FlowMapper.java` | 连接流 Mapper |
| `modules/flow/mapper/FlowVersionMapper.java` | 版本 Mapper |

#### open-server — runtime 调试代理模块（debug）

> 运行时本体已迁移到 connector-api。open-server 仅保留**调试代理**：接收前端的手动调试/测试运行请求，转发至 connector-api 调试接口。

| 文件 | 说明 |
|------|------|
| `modules/debug/DebugController.java` | 前端入口：手动调试、测试运行 |
| `modules/debug/DebugProxyService.java` | 调用 connector-api 调试接口（内部 HTTP 客户端） |
| `modules/debug/client/ConnectorApiClient.java` | connector-api HTTP 客户端封装（含鉴权/重试/超时） |

#### connector-api — 🆕 独立运行时服务（全新工程）

| 文件 | 说明 |
|------|------|
| `runtime/ReactiveSequentialExecutor.java` | 反应式顺序执行引擎（基于 `Mono` 链路串联节点；对外仍同步返回 HTTP 响应） |
| `runtime/ExecutionContext.java` | 执行上下文管理（不可变快照 + 步骤累加） |
| `runtime/NodeExecutor.java` | 节点执行器接口，签名 `Mono<NodeOutput> execute(NodeInput, ExecutionContext)` |
| `runtime/ConnectorNodeExecutor.java` | 连接器节点执行器（基于 WebClient 异步调用下游 HTTP API） |
| `runtime/DataProcessorNodeExecutor.java` | 数据处理节点执行器（纯 CPU 计算，直接返回 `Mono.just(...)`） |
| `runtime/entity/ExecutionRecord.java` | 执行记录实体 |
| `runtime/entity/ExecutionStep.java` | 执行步骤实体 |
| `runtime/entity/ConnectorAuthConfig.java` | 认证凭证实体 |
| `runtime/mapper/ExecutionRecordMapper.java` | 执行记录 Mapper（MyBatis 同步 JDBC，调用方需 `subscribeOn(boundedElastic)`） |
| `runtime/mapper/FlowVersionReadMapper.java` | FlowVersion 只读 Mapper（读取 orchestration_config） |
| `runtime/db/DbAccessor.java` | 🆕 统一封装 MyBatis 调用 → `Mono<T>`，强制 `boundedElastic` 调度器隔离 |
| `runtime/client/WebClientFactory.java` | 🆕 WebClient 工厂：连接池、超时、最大缓冲、TLS 等配置统一收口 |
| `trigger/HttpTriggerController.java` | HTTP 触发入口（WebFlux `@RestController`，返回 `Mono<TriggerResponse>`，对调用方呈现同步语义） |
| `debug/DebugApiController.java` | 调试接口（WebFlux `@RestController`，供 open-server 调用：手动调试/测试运行） |
| `common/CredentialCipher.java` | 认证凭证 AES-256-GCM 加解密 |
| `common/InternalAuthFilter.java` | 内部接口鉴权 WebFilter（基于 `WebFilter`，仅允许 open-server 访问 debug-api） |
| `ConnectorApiApplication.java` | Spring Boot 启动类（WebFlux 模式） |
| `pom.xml` / `application.yml` | 工程配置（启用 webflux + data-redis-reactive，测试 profile 启用 BlockHound） |

#### open-server — monitor 模块

| 文件 | 说明 |
|------|------|
| `modules/monitor/MetricsController.java` | 执行历史查询 |
| `modules/monitor/MetricsService.java` | 统计查询服务 |

#### wecodesite — 新增/扩展页面

| 文件 | 说明 | 状态 |
|------|------|:---:|
| `ConnectPlatform/FlowDetail.jsx` | 连接流详情页 | 🆕 |
| `ConnectPlatform/ExecutionDetail.jsx` | 执行详情页 | 🆕 |
| `ConnectPlatform/FlowEditor/DataMappingDialog.jsx` | 数据映射弹窗 | 🆕 |
| `ConnectPlatform/FlowEditor/TestRunDialog.jsx` | 测试运行弹窗 | 🆕 |
| `ConnectPlatform/Monitor/MonitorDashboard.jsx` | 监控面板 | 🆕 |
| `ConnectPlatform/Monitor/index.jsx` | 监控入口 | 🆕 |
| `ConnectPlatform/Connector/thunk.js` | 扩展：fetchVersions 等 | 📝 扩展 |
| `ConnectPlatform/Flow/thunk.js` | 扩展：saveCanvas/testRun 等 | 📝 扩展 |
| `ConnectPlatform/FlowEditor/thunk.js` | 扩展：编排配置保存 | 📝 扩展 |
| `App.jsx` | 注册新路由 | 📝 修改 |

### 4.8 新增依赖

| 依赖 | 版本 | 用途 | 所属项目 |
|------|------|------|---------|
| `@xyflow/react` (React Flow) | ^12.x | 可视化编排画布 | wecodesite（已内置） |
| `spring-boot-starter-webflux` | 随 Spring Boot 3.4.6 | Reactor Netty + WebFlux Web 栈 | connector-api（🆕 运行时服务） |
| `spring-boot-starter-data-redis-reactive` | 随 Spring Boot 3.4.6 | Redis reactive 驱动（Lettuce） | connector-api |
| `reactor-core` | 随 Spring Boot 3.4.6（reactor-bom） | reactive 编程核心 | connector-api（传递依赖） |
| `mybatis-spring-boot-starter` | 3.0.4 | MyBatis（同步 JDBC，需配合 `boundedElastic` 调度器使用） | connector-api（共享 entity/mapper） |
| `blockhound`（测试 scope） | ^1.0.x | reactive 链路中阻塞调用检测 | connector-api（仅 test/staging 环境启用） |

> ❌ **本版本不再引入的依赖**（与 spec v3.x 版 plan 的差异）：
> - ~~Quartz Scheduler~~（定时触发移至 NG17，V1 阶段）
> - ~~MQS 消息队列~~（同步执行无需消息队列）
>
> 🆕 **后续可评估升级**：将 connector-api 的 MyBatis 替换为 **R2DBC**（端到端 reactive），彻底消除 `boundedElastic` 调度器隔离的开销，V1 阶段再决策。

### 4.9 文件影响统计

| 项目 | 新增文件 | 修改文件 | 删除文件 |
|------|:--------:|:--------:|:--------:|
| open-server (connector + flow + monitor + debug 4 个模块) | 约 35 | 0 | 0 |
| connector-api 🆕（全新工程：runtime + trigger + debug + common） | 约 30（含工程脚手架） | 0 | 0 |
| wecodesite（已有页面 + 新增补充） | 6（新增） + 3（已有需扩展） | 2 | 0 |
| **合计** | **约 74** | **2** | **0** |

> 📌 新增 connector-api 独立工程后，原"60 个 open-server 新增文件"被重新划分为：约 35 个留在 open-server（管理 + 调试代理），约 30 个迁移/新建到 connector-api（运行时 + 工程脚手架）。

---

## 5. 风险评估

### 5.1 技术风险

| 风险 | 影响 | 概率 | 缓解措施 |
|------|------|:----:|---------|
| 同步执行下游慢响应占用 WebClient 连接池 | connector-api 吞吐量下降 | 中 | connector-api 基于 **WebFlux + Reactor Netty** NIO 非阻塞栈，少量 EventLoop 线程即可承接百级并发；WebClient 配置连接池上限 + 连接/读/写超时 + Reactor `.timeout(默认 30s，最大 5min)` 双重保障；超时强制终止；可水平扩容 connector-api 实例 |
| WebFlux 反应式编程团队学习成本 | 工期延误 / 代码质量 | 中 | plan-code 沉淀强制规则（禁阻塞调用、JDBC 必须 `boundedElastic`、必配超时等）；迭代 0 安排 2-3 天技术演练；Code Review 重点把关 reactive 链路；保留 `BlockHound` 在测试环境检测意外阻塞调用 |
| MyBatis 同步 JDBC 在 reactive 链路中被误用 | 阻塞 EventLoop 导致全局吞吐崩塌 | 中 | 统一封装 `DbAccessor`，内部强制 `Mono.fromCallable(...).subscribeOn(Schedulers.boundedElastic())`；测试环境启用 BlockHound 拦截裸调用；后续可评估迁移到 R2DBC 彻底消除 |
| 可视化编排画布前端复杂度高 | 工期延误 | 中 | MVP 限制线性编排（禁用分支/循环连接），使用 React Flow 的受限模式 |
| 认证凭证加密存储和传输 | 安全漏洞 | 低 | 使用 AES-256-GCM 加密 + HTTPS + 界面脱敏显示 |
| HTTP 触发 URL 安全 | 非法调用 | 低 | 随机不可预测路径 + 请求签名验证 + 限流（FR-024） |
| 同步执行下目标 API 延迟高 | 请求超时 | 中 | 可配置超时（最小 1s，最大 5min），超时返回「执行超时」状态 |
| **open-server ↔ connector-api 内部通信失败** | 手动调试/测试运行不可用 | 低 | 内部 HTTP 加共享 token 鉴权 + 超时重试（默认 1 次）+ 熔断降级（调试不可用时前端提示重试，不影响管理面 CRUD） |
| **connector-api 独立工程引入运维负担** | 部署/监控成本增加 | 低 | 复用 open-server 的部署流水线模板；MySQL/Redis 复用同一实例；接入现有监控告警 |

### 5.2 依赖风险

| 风险 | 影响 | 概率 | 缓解措施 |
|------|------|:----:|---------|
| 内部业务系统 API 不稳定 | 连接流执行失败 | 中 | 默认错误处理（FR-023）标记节点失败，不中断整个流（V1 可配置错误处理策略） |
| React Flow 库兼容性问题 | 前端问题 | 低 | 选择稳定版本，做好降级方案（表单配置模式兜底） |

### 5.3 时间风险

| 风险 | 影响 | 概率 | 缓解措施 |
|------|------|:----:|---------|
| MVP 范围 25 个 FR | 开发周期较长 | 中 | 按模块优先级迭代：连接器管理→连接流管理→运行时→监控 |
| 可视化编排画布研发耗时长 | 前端成为瓶颈 | 高 | 先做基础拖拽+节点配置，高级交互后延；用 React Flow 的受控模式减少自研量 |

### 5.4 开放问题处理

| # | 问题 | 建议方案 | 决策时间点 |
|---|------|---------|-----------|
| OQ-001 | MVP 连接器范围 | 优先封装 IM 消息能力（发送消息）作为首个连接器 | Tasks 阶段开始前 |
| OQ-002 | 流编排引擎选型 | **已决策** → 轻量同步执行器（方案 A） | 当前 |
| OQ-003 | 可视化编排画布选型 | **已决策** → React Flow (@xyflow/react) | 当前 |
| OQ-004 | 执行历史保留策略 | 默认保留 30 天（可配置），超过自动清理 | Tasks 阶段 |
| OQ-005 | 限流阈值设定 | HTTP 触发默认 100 次/分钟，手动触发默认 10 次/分钟 | Tasks 阶段 |

---

## 6. 版本规划

### 迭代建议

| 迭代 | 范围 | FR 范围 | 周期 | 交付价值 |
|------|------|---------|:----:|---------|
| **迭代 0** | 🆕 connector-api 工程脚手架（**Spring WebFlux 工程初始化、WebClient 工厂、DbAccessor、BlockHound 接入**、共享 entity/mapper、内部鉴权、部署流水线）+ **团队 WebFlux/Reactor 技术演练** | — | 1-1.5 周 | connector-api 可启动、可被 open-server 调通；团队掌握 reactive 强制规则 |
| **迭代 1** | 连接器管理模块（open-server） | FR-001 ~ FR-008 | 2-3 周 | 可创建/编辑/发布连接器，浏览连接器目录 |
| **迭代 2** | 连接流管理模块（open-server） | FR-009 ~ FR-020 | 3-4 周 | 可创建连接流、拖拽编排、保存草稿、发布版本 |
| **迭代 3** | 运行时模块（connector-api：runtime + http-trigger + debug-api）+ open-server 调试代理（debug） | FR-021 ~ FR-024 | 2-3 周 | 可同步执行连接流（HTTP 触发+手动触发），错误处理+限流 |
| **迭代 4** | 监控模块（open-server） | FR-025 | 1-2 周 | 可查看执行历史、执行详情、步骤输入输出 |
| **集成测试** | 全链路联调 + E2E（含 open-server ↔ connector-api 通信） | 全部 | 1-2 周 | 端到端验证 |
| **合计** | | | **9-13.5 周** | |

### 关键里程碑

| 里程碑 | 时间点 | 验收标准 |
|-------|--------|---------|
| M0: connector-api 可联调 | 迭代 0 完成 | connector-api 工程启动成功，open-server 可通过内部 HTTP 调通调试接口（mock 实现） |
| M1: 连接器可用 | 迭代 1 完成 | 可创建/编辑/发布连接器，连接配置完整 |
| M2: 连接流可编排 | 迭代 2 完成 | 可拖拽创建连接流（含数据处理节点），保存草稿，发布版本 |
| M3: 连接流可执行 | 迭代 3 完成 | 连接流可被 HTTP（connector-api 直连）/手动调试（open-server → connector-api 调试接口）同步执行，返回完整结果 |
| M4: 可运维 | 迭代 4 完成 | 可查看执行历史、步骤详情 |
| M5: MVP 就绪 | 集成测试完成 | 完成端到端验证，满足所有 MVP 验收标准 |

---

## ✅ 技术规划完成

**Feature**: 连接器平台 (CONN-PLAT-001)  
**状态**: planned  
**文件**:

| 文档 | 说明 |
|------|------|
| `.sddu/.../specs-tree-connector-platform/plan.md` | **技术总纲**（架构·方案·风险评估·版本规划） |
| `.sddu/.../specs-tree-connector-platform/plan-db.md` | **数据库设计**（表结构·索引·JSON Schema） |
| `.sddu/.../specs-tree-connector-platform/plan-api.md` | **API 接口设计**（端点·请求/响应·鉴权·命名规范） |
| `.sddu/.../specs-tree-connector-platform/plan-page.md` | **前端页面设计**（组件树·交互流程·路由） |
| `.sddu/.../specs-tree-connector-platform/plan-code.md` | **代码规范**（注释·日志·SQL·安全·16 条强制规则） |

### 生成的 ADR

| ADR | 标题 | 状态 |
|-----|------|------|
| `ADR-001.md` | 轻量顺序执行引擎技术方案 | ACCEPTED |
| `ADR-002.md` | React Flow 可视化编排画布 | ACCEPTED |
| `ADR-003.md` | 运行时架构：**独立部署 `connector-api` 服务**（v2.0 修订，原"单体嵌入 + 模块化隔离"已 SUPERSEDED） | ACCEPTED（v2.0） |

> 💡 现有 ADR-001 / ADR-002 的核心决策（轻量顺序引擎 / React Flow）仍然有效，无需更改。**ADR-003 已在 v2.0 修订**：运行时从「嵌入 open-server」改为「独立部署为 `connector-api` 服务」，open-server 通过内部调试接口调用 connector-api 完成手动调试/测试运行，前端统一收归 wecodesite。其它变更点：执行模型从异步改为同步（已在方案和架构中更新）。

### 与 spec v3.x 版 plan 的主要变更

| 变更项 | v1.x（基于 spec v3.x） | v2.0（基于 spec v4.0） |
|--------|----------------------|----------------------|
| 执行模型 | **异步**（MQS 消息队列） | **同步**（请求线程执行） |
| 调度方式 | 消息驱动 + 轮询 | 直接同步调用 |
| 触发器 | 事件/Webhook/定时/手动 | HTTP/手动 |
| 节点类型（MVP） | 连接器节点 | 连接器节点 + **数据处理节点** |
| Scope/审批集成 | ✅ 集成 | ❌ 移至 V1 |
| FR 数量 | ~37 | 25 |
| 监控范围 | 全指标仪表盘 | 执行历史查询 |
| 错误处理 | 审批流程处理 | 默认错误处理（FR-023） |
| 限流 | 无 | 默认限流（FR-024） |
| 预估工期 | 10-14 周 | **9-13.5 周** |
| 新增依赖 | Quartz + MQS | 无额外依赖 |
| **运行时部署** | 嵌入 open-server | **独立服务 `connector-api`**（与 open-server 拆分） |
| **服务数量** | 1（open-server 单体） | 2（open-server + connector-api） |
| **调试/测试运行链路** | open-server 内部调用 | open-server → connector-api 调试接口（内部 HTTP） |
| **前端归属** | 散落（含 open-web） | 统一在 wecodesite |
| **connector-api Web 栈** | （v1.x 嵌入 open-server，沿用 Spring MVC） | **Spring WebFlux + Reactor Netty + WebClient**（NIO 非阻塞，匹配高并发 HTTP 转发场景）|

### 下一步
👉 运行 `@sddu-tasks 连接器平台` 开始任务分解