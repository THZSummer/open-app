# 配置设计：连接器平台 V3 — 全量配置场景

**Feature ID**: CONN-PLAT-003
**关联文档**: [spec.md](./spec.md) v3.0, [plan.md](./plan.md) v3.0, [plan-api.md](./plan-api.md), [plan-script.md](./plan-script.md)
**版本**: v1.0
**创建日期**: 2026-06-26
**说明**: 系统化记录 V3 全部配置场景，覆盖开关、白名单、名单、上限、阈值、审批人等。

---

## 目录

- [1 配置清单](#1-配置清单)
- [2 配置详情](#2-配置详情)
  - [2.1 连接器](#21-连接器)
  - [2.2 连接流](#22-连接流)
  - [2.3 平台管控](#23-平台管控)
  - [2.4 实体级配置](#24-实体级配置)
  - [2.5 校验时机策略](#25-校验时机策略)
- [附录 A：平台配置能力](#附录-a平台配置能力)
  - [A.1 Spring 配置文件](#a1-spring-配置文件)
  - [A.2 业务对象 JSON 配置](#a2-业务对象-json-配置)
  - [A.3 Lookup](#a3-lookup)
  - [A.4 Property](#a4-property)
- [附录 B：关联文档索引](#附录-b关联文档索引)
- [附录 C：修订记录](#附录-c修订记录)

---

## 1 配置清单

> 💡 **设计思路**：连接器和连接流属于动态编排平台，用户的输入内容变化性极大——可以任意编排节点拓扑、编写任意脚本、配置任意参数。为保护系统安全稳定运行，平台需要对用户的输入施加硬性限制，这些限制即为本章记录的「配置」。
>
> 至于连接器名称、认证类型、编排图结构、JSON Schema 定义等，属于开放给用户的自由输入内容，不在配置约束范畴，不列入此清单。

| # | 配置项 | 作用域 | 按应用区分 | 存储 | path | code | FR |
|---|--------|--------|:---:|:---:|------|------|----|
| 1 | 连接器版本数量上限 | 连接器 | ❌ | Property | `connector_platform` | `connector_max_versions` | FR-005a |
| 2 | 连接器URL正则规则 | 连接器版本配置 | ❌ | Property | `connector_platform` | `url_regex_pattern` | FR-015 |
| 3 | 连接器配置JSON长度上限 | 连接器版本配置 | ✅ | Property | `connector_platform` | `connector_config_max_bytes` | FR-047 |
| 4 | 连接流版本数量上限 | 连接流 | ❌ | Property | `connector_platform` | `flow_max_versions` | FR-024a |
| 5 | 运行记录条数上限 | 连接流 | ✅ | Property | `connector_platform` | `max_execution_records_per_flow` | FR-042 |
| 6 | 连接器节点超时上限 | 连接流版本配置 | ✅ | Property | `connector_platform` | `node_max_timeout_seconds` | FR-034 |
| 7 | 连接流配置JSON长度上限 | 连接流版本配置 | ✅ | Property | `connector_platform` | `flow_config_max_bytes` | FR-047 |
| 8 | 连接流最大QPS | 连接流版本配置 | ✅ | Property | `connector_platform` | `flow_max_qps` | FR-035 |
| 9 | 连接流最大并发 | 连接流版本配置 | ✅ | Property | `connector_platform` | `flow_max_concurrency` | FR-035 |
| 10 | 连接流缓存TTL上限 | 连接流版本配置 | ✅ | Property | `connector_platform` | `flow_max_cache_ttl_seconds` | FR-037 |
| 11 | 连接流并行节点分支上限 | 连接流版本配置 | ✅ | Property | `connector_platform` | `flow_max_parallel_branches` | FR-038a |
| 12 | 脚本源码长度上限 | 连接流版本配置 | ✅ | Property | `connector_platform` | `script_max_length_chars` | FR-040a |
| 13 | 脚本超时范围 | 连接流版本配置 | ✅ | Property | `connector_platform` | `script_max_timeout_seconds` | FR-040a |
| 14 | 日志采集开关 | 平台管控 | ✅ | Property | `connector_platform` | `log_collection_enabled` | FR-044 |
| 15 | 连接器平台开放应用范围清单 | 平台管控 | ❌ | Lookup | `connector_platform` | `app_whitelist` | FR-045 |
---

## 2 配置详情

> 💡 以下各配置项的**存储**、**path**、**code** 与 §1 配置清单一一对应。支持按应用区分的项遵循二层回退模型：优先取 `path=connector_platform_app_{appId}`，未命中回退 `path=connector_platform`。

### 2.1 连接器

| # | 配置项 | 存储 | path | code | 默认值 | 按应用区分 | FR | 说明 |
|---|--------|:---:|------|------|:---:|:---:|:--:|------|
| 1 | 连接器版本数量上限 | Property | `connector_platform` | `connector_max_versions` | 1000 | ❌ | FR-005a | 每个连接器最多创建 1000 个版本，达上限禁止创建/复制草稿 (EC-019) |
| 2 | 连接器URL正则规则 | Property | `connector_platform` | `url_regex_pattern` | — | ❌ | FR-015 | 平台级 URL 正则校验规则，连接器发布时按此规则校验用户填写的目标地址。正则满足"或"语法即可覆盖多域名场景。空白 = 不限制 |
| 3 | 连接器配置JSON长度上限 | Property | `connector_platform` / `connector_platform_app_{appId}` | `connector_config_max_bytes` | — | ✅ | FR-047 | 限制连接器版本配置 JSON 的最大字节数，防止超大配置导致存储/解析异常 |

### 2.2 连接流

| # | 配置项 | 存储 | path | code | 默认值 | 按应用区分 | FR | 说明 |
|---|--------|:---:|------|------|:---:|:---:|:--:|------|
| 4 | 连接流版本数量上限 | Property | `connector_platform` | `flow_max_versions` | 1000 | ❌ | FR-024a | 每个连接流最多创建 1000 个版本，达上限禁止创建/复制草稿 (EC-020) |
| 5 | 运行记录条数上限 | Property | `connector_platform` / `connector_platform_app_{appId}` | `max_execution_records_per_flow` | 1000 | ✅ | FR-042 | 每连接流运行记录最大保留条数，超出时 FIFO 清理最早记录 (EC-029)。与 30 天定期清理策略互补 |
| 6 | 连接器节点超时上限 | Property | `connector_platform` / `connector_platform_app_{appId}` | `node_max_timeout_seconds` | 5 | ✅ | FR-034 | 连接流中连接器节点的最大超时秒数。发布时校验节点值 ≤ 此上限 (EC-028)；运行时取 min(节点值, 此上限) |
| 7 | 连接流配置JSON长度上限 | Property | `connector_platform` / `connector_platform_app_{appId}` | `flow_config_max_bytes` | — | ✅ | FR-047 | 限制连接流版本编排配置 JSON 的最大字节数 |
| 8 | 连接流最大QPS | Property | `connector_platform` / `connector_platform_app_{appId}` | `flow_max_qps` | 1000 | ✅ | FR-035 | 连接流入站限流 QPS 上限。发布时校验 flowConfig QPS ≤ 此上限 (EC-025)；运行时取 min(流配置值, 此上限)，超限返回 429 |
| 9 | 连接流最大并发 | Property | `connector_platform` / `connector_platform_app_{appId}` | `flow_max_concurrency` | 1000 | ✅ | FR-035 | 连接流入站限流并发数上限。发布时校验 flowConfig 并发 ≤ 此上限 (EC-025)；运行时取 min(流配置值, 此上限) |
| 10 | 连接流缓存TTL上限 | Property | `connector_platform` / `connector_platform_app_{appId}` | `flow_max_cache_ttl_seconds` | 1296000 | ✅ | FR-037 | 连接流缓存 TTL 最大秒数（默认 15 天）。发布时校验 flowConfig TTL ≤ 此上限 (EC-026) |
| 11 | 连接流并行节点分支上限 | Property | `connector_platform` / `connector_platform_app_{appId}` | `flow_max_parallel_branches` | 8 | ✅ | FR-038a | 并行处理节点最大分支数。发布时校验分支数 ≤ 此上限 (EC-027) |
| 12 | 脚本源码长度上限 | Property | `connector_platform` / `connector_platform_app_{appId}` | `script_max_length_chars` | 10000 | ✅ | FR-040a | 单个脚本节点的源码最大字符数，防止用户注入超大脚本。发布时校验 |
| 13 | 脚本超时范围 | Property | `connector_platform` / `connector_platform_app_{appId}` | `script_max_timeout_seconds` | 30 | ✅ | FR-040a | 脚本节点最大执行超时秒数。用户可在 1~此上限之间选择，默认 5s。发布时校验脚本 timeout ≤ 此上限 |

### 2.3 平台管控

| # | 配置项 | 存储 | path | code / classify_code | 默认值 | 按应用区分 | FR | 说明 |
|---|--------|:---:|------|------|:---:|:---:|:--:|------|
| 14 | 日志采集开关 | Property | `connector_platform` / `connector_platform_app_{appId}` | `log_collection_enabled` | true | ✅ | FR-044 | 控制是否写入节点级运行日志。开启 → 写入节点 I/O 快照；关闭 → 仅保留运行记录基础信息，历史日志保留不变 (EC-030~032) |
| 15 | 连接器平台开放应用范围清单 | Lookup | `connector_platform` | `app_whitelist` | — | ❌ | FR-045 | 控制哪些应用可开通连接器平台能力。白名单内 → 全部功能可用；非白名单 → 拒绝访问。应用移出后已有数据保留，新操作拒绝 (EC-015) |

### 2.4 实体级配置

以下为连接器和连接流版本的 JSON 配置快照中，受平台限制约束的关键字段。这些字段由用户自由输入，平台通过 §2.1~§2.3 中的配置项对其施加硬性限制。

#### 2.4.1 连接器版本配置快照

| 配置字段 | 受限于 | 存储位置 |
|---------|--------|---------|
| 目标 URL | #2 连接器URL正则规则 | ConnectorVersion 配置快照 → url |
| 配置 JSON 大小 | #3 连接器配置JSON长度上限 | ConnectorVersion 配置快照（整体） |
| 版本数量 | #1 连接器版本数量上限 | 每连接器的版本总数 |

#### 2.4.2 连接流版本配置快照

| 配置字段 | 受限于 | 存储位置 |
|---------|--------|---------|
| 连接器节点超时值 | #6 连接器节点超时上限 | FlowVersion 编排快照 → 连接器节点.timeout |
| 配置 JSON 大小 | #7 连接流配置JSON长度上限 | FlowVersion 编排快照（整体） |
| flowConfig.QPS | #8 连接流最大QPS | FlowVersion 编排快照 → flowConfig.qps |
| flowConfig.并发数 | #9 连接流最大并发 | FlowVersion 编排快照 → flowConfig.concurrency |
| flowConfig.缓存TTL | #10 连接流缓存TTL上限 | FlowVersion 编排快照 → flowConfig.cacheTtl |
| 并行节点分支数 | #11 连接流并行节点分支上限 | FlowVersion 编排快照 → 并行节点.branches |
| 脚本源码长度 | #12 脚本源码长度上限 | FlowVersion 编排快照 → 脚本节点.script |
| 脚本超时值 | #13 脚本超时范围 | FlowVersion 编排快照 → 脚本节点.timeout |
| 版本数量 | #4 连接流版本数量上限 | 每连接流的版本总数 |

### 2.5 校验时机策略

V3 采用**「保存时不校验，发布时统一卡口」**的策略：

```
草稿创建/编辑/复制 ─── 仅 DB 存储约束校验（字段长度、数据类型等）
   │
   ▼
发布时 ───── 全部校验集中执行：
   ├── 业务必填字段（名称、描述等非空）
   ├── 配置非空（编排/入参出参 Schema）
   ├── URL 正则校验 ───── 对比 #2 url_regex_pattern
   ├── JSON 长度校验 ───── 对比 #3 / #7 config_max_bytes
   ├── JSON 语法合法性 (FR-047)
   ├── 脚本语法合法性 (FR-040a)
   ├── 节点超时 ≤ #6 node_max_timeout_seconds (EC-028)
   ├── flowConfig QPS ≤ #8 flow_max_qps (EC-025)
   ├── flowConfig 并发 ≤ #9 flow_max_concurrency (EC-025)
   ├── flowConfig 缓存TTL ≤ #10 flow_max_cache_ttl_seconds (EC-026)
   ├── 并行分支数 ≤ #11 flow_max_parallel_branches (EC-027)
   ├── 脚本长度 ≤ #12 script_max_length_chars
   ├── 脚本超时 ≤ #13 script_max_timeout_seconds
   └── 连接器版本可用性 (FR-039)
```

| 场景 | DB 约束 | 业务必填 | 平台限制 | JSON 语法 | 正则合法性 | 引用可用性 |
|------|:---:|:---:|:---:|:---:|:---:|:---:|
| 创建空草稿 | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ |
| 编辑草稿保存 | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ |
| 复制到草稿 | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ |
| 一键复制连接流 | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ |
| **发布时 (FR-007 / FR-026)** | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |

> 💡 "平台限制"列在校验时机策略中指代 #1~#15 中所有可配置上限项——发布时取实际值与此处 Property 值做对比，超出即拦截。

## 附录 A：平台配置能力

### A.1 Spring 配置文件

V3 涉及两个 Spring Boot 服务，均采用 `application.yml` + Profile 机制管理环境差异化配置。

#### A.1.1 open-server（管理面，端口 18080）

| 文件 | 用途 |
|------|------|
| `open-server/src/main/resources/application.yml` | 公共配置（端口、编码、日志、OpenAPI） |
| `application-dev.yml` | 开发环境（MySQL 数据源、Redis 集群、MyBatis、健康检查） |
| `application-prod.yml` | 生产环境（环境变量占位符 `${...}`、更大连接池） |

**V3 相关配置项**：

| 配置路径 | 默认值 | 说明 |
|----------|--------|------|
| `server.port` | 18080 | open-server 服务端口 |
| `server.servlet.context-path` | `/open-server` | 上下文路径 |
| `spring.datasource.url` | `jdbc:mysql://...` | MySQL 数据源（JDBC，HikariCP 连接池） |
| `spring.datasource.hikari.maximum-pool-size` | dev:20 / prod:50 | 数据库连接池最大连接数 |
| `spring.redis.cluster.nodes` | 5 节点集群 | Redis 集群节点列表 |
| `spring.redis.lettuce.pool.max-active` | dev:8 / prod:20 | Redis 连接池最大活跃连接 |
| `mybatis.mapper-locations` | `classpath:mapper/*.xml` | MyBatis XML Mapper 扫描路径 |
| `platform.approval-url-prefix` | dev:`localhost:3000` / prod:`platform.example.com` | 审批页面跳转地址前缀（FR-031~033 审批流程使用） |
| `management.health.db.enabled` | dev:false / prod:true | 健康检查是否包含数据库 |
| `logging.level.com.xxx.it.works.wecode.v2` | dev:DEBUG / prod:INFO | V3 模块日志级别 |

**Profile 激活**：`application.yml` 中 `spring.profiles.active: dev`（开发环境默认），生产通过环境变量 `SPRING_PROFILES_ACTIVE=prod` 覆盖。

#### A.1.2 connector-api（运行时，端口 18180）

| 文件 | 用途 |
|------|------|
| `connector-api/src/main/resources/application.yml` | 统一配置（端口、R2DBC、Redis 集群、缓存开关） |

**V3 相关配置项**：

| 配置路径 | 默认值 | 说明 |
|----------|--------|------|
| `server.port` | 18180 | connector-api 服务端口 |
| `spring.r2dbc.url` | `r2dbc:mysql://192.168.3.155:3306/openapp` | Reactive MySQL 数据源 |
| `spring.r2dbc.pool.max-size` | 20 | R2DBC 连接池最大连接数 |
| `spring.r2dbc.pool.initial-size` | 5 | R2DBC 连接池初始连接数 |
| `spring.data.redis.cluster.nodes` | 5 节点集群 | Redis 集群节点（同 open-server） |
| `spring.data.redis.lettuce.pool.max-active` | 8 | Redis 连接池最大活跃连接 |
| `connector.cache.enabled` | true | **缓存总开关** — 控制是否启用 Redis 缓存（`CacheToggle`），`false` 时所有查询穿透 R2DBC |
| `connector.snowflake.worker-id` | 0 | Snowflake ID 生成器 Worker ID（`IdGenerator`） |
| `connector.snowflake.datacenter-id` | 0 | Snowflake ID 生成器数据中心 ID |
| `management.health.db.enabled` | true | 健康检查是否包含 R2DBC |
| `management.health.redis.enabled` | true | 健康检查是否包含 Redis |

#### A.1.3 V3 硬编码配置（Java 常量/注解）

以下配置不在 `application.yml` 中，以 Java 代码常量或注解形式存在：

| 配置项 | 所在位置 | 值 | 说明 |
|--------|---------|-----|------|
| `@EnableScheduling` | `ConnectorApiApplication.java` | — | 启用定时任务调度（运行记录 FIFO 清理、30 天定期清理） |
| GraalJS `statementLimit` | `GraalJsContextFactory.java` | 10000 | 脚本节点执行语句数上限（沙箱安全限制） |
| GraalJS `HostAccess.EXPLICIT` | `GraalJsContextFactory.java` | — | 禁止 Java 宿主对象隐式访问（五层纵深防御第 3 层） |
| 运行记录 30 天清理 | `ExecutionCleanupJob.java` | 30 天 | `@Scheduled` 定时清理过期运行记录 |

#### A.1.4 .gitignore 排除

生产环境配置文件中的敏感信息（数据库密码、Redis 密码）通过环境变量 `${MYSQL_PASSWORD}` / `${REDIS_PASSWORD}` 注入，不写入配置文件。`application-prod.yml` 中的占位符默认值仅用于本地开发验证。

### A.2 业务对象 JSON 配置

V3 采用 **「配置即快照」** 策略——连接器和连接流的运行时配置均存储在版本快照 JSON 中，不独立建配置表。

| 配置载体 | 存储方式 | 包含的配置项 | 变更生效 |
|----------|---------|-------------|---------|
| **ConnectorVersion 快照** | `connector_version_t.config_snapshot` JSON 列 | 认证类型、凭证位置、URL 白名单规则、入参/出参 Schema | 发布后立即生效，运行时按引用版本号读取 |
| **FlowVersion 快照** | `flow_version_t.orchestration_config` JSON 列 | 编排图（nodes/edges）、flowConfig（限流/缓存）、触发器认证/SYSACCOUNT白名单、节点超时、脚本源码、并行分支 | 部署绑定版本后生效 |

**关键设计原则**：配置随版本走，而非随实体走。每个已发布版本携带完整的自包含配置快照，保证了：
- 版本切换时配置原子性（不会出现新旧配置混淆）
- 历史版本可完整追溯（任意历史版本的配置均可查看）
- 草稿编辑不影响运行中实例（草稿修改仅作用于当前工作区）

### A.3 Lookup

**用途**：存储枚举型、列表型平台配置。平台管理员通过 market-web 界面维护，数据写入 MySQL；open-server / connector-api 直读 MySQL 获取配置。

**数据库表**（2 张）：

| 表名 | 用途 | 关键列 |
|------|------|--------|
| `openplatform_lookup_classify_t` | 分类（分组） | `classify_id` (PK), `classify_code`, `classify_name`, `path`, `status` |
| `openplatform_lookup_item_t` | 分类下的具体项 | `item_id` (PK), `classify_id` (FK), `item_code`, `item_name`, `item_value`, `status` |

**ER 关系**：`classify_t` 1 ─── N `item_t`，通过 `classify_id` 关联。

**设计模型**：类比文件系统 — `path` 相当于目录，`classify_code` 相当于一个配置文件，文件内是一对多的列表数据（`classify_t` 1:N `item_t`）。`UK(path, classify_code)` 保证同一目录下文件名唯一；`UK(classify_id, item_code)` 保证同一文件内条目不重复。

**目录结构与示例数据**：

```
  /connector_platform/ (path="connector_platform")    ← 连接器平台命名空间
  ├── app_whitelist                                       ← 应用白名单（1:N 列表）
  │     ├── app_001
  │     ├── app_002
  │     └── app_003
  │
  /connector_platform_gray/ (path="connector_platform_gray")  ← 灰度目录
  └── app_whitelist                                            ← 灰度白名单（独立分组）
        ├── app_001
        └── app_005
```

| path（目录） | classify_code（文件名） | item_value（文件内容） | 说明 |
|:--:|------|------|------|
| `connector_platform` | `app_whitelist` | `app_001`, `app_002`, `app_003` | 生产环境：三个应用开通连接器平台 |
| `connector_platform_gray` | `app_whitelist` | `app_001`, `app_005` | 灰度环境：仅两个应用可见 |

**查询逻辑**：先按 `(path, classify_code)` 定位 `classify_id`，再联查 `item_t` 获取所有有效 `item_value`。


**读取方式**：
- open-server 通过 `LookupWhitelistMapper` 联查两张表：先按 `(path, classify_code)` 定位 `classify_id`，再查该分类下所有有效 item 的 `item_value`
- 本地 Caffeine 缓存（TTL 5min）
- connector-api 通过 R2DBC 直查同两张表，Redis 缓存（TTL 7d±2h），miss 回源 MySQL

**准入流程**：
```
用户请求 → 准入拦截器 → 查本地缓存
                           ├── 命中 → 判断 appId 是否在 item_value 列表中 → 放行/拒绝
                           └── 未命中 → 联查 classify_t + item_t
                                          ├── 成功 → 更新缓存 → 判断
                                          └── MySQL 不可用 → 降级放行 + 告警
```

> 💡 **path 使用约定**：`path` 用于对配置归类分组。V3 连接器平台的应用白名单放在 `path="connector_platform"` 下。若后续需要按版本、环境等维度隔离，可通过不同 `path`（如 `path="connector_platform_v4"`、`path="connector_platform_gray"`）创建独立分组，互不干扰。

### A.4 Property

**用途**：存储键值对型应用级参数。平台管理员通过 market-web 界面维护，数据写入 MySQL；open-server / connector-api 直读 MySQL 获取配置。

**数据库表**（1 张）：

| 表名 | 用途 | 关键列 |
|------|------|--------|
| `openplatform_property_t` | 键值对属性 | `id` (PK, 雪花ID), `path`, `code`, `name`, `value`, `status` |

**唯一约束**：`(path, code)` 联合唯一索引。这保证了同一个命名空间下 code 唯一，不会出现重复配置。

**设计模型**：`path` 是命名空间，所有连接器平台配置统一放在 `path=connector_platform` 下，与其他业务模块（能力开放平台、数据开放平台等）的配置隔离。`code` 是配置项名称，同一 path 下 code 唯一。`UK(path, code)` 联合唯一索引保证不会重复。

**按应用区分机制**：对于支持按应用区分的配置项，平台默认值存于 `path=connector_platform`，应用覆盖值存于 `path=connector_platform_app_{appId}`。查询时优先搜应用专属路径，找不到再回退平台默认路径。

**二层回退模型**：`应用独立值` > `平台统一默认值`

**命名空间与示例数据**：

```
openplatform_property_t
  /connector_platform/                          ← 连接器平台命名空间（平台默认值）
  ├── max_timeout_seconds               = 5
  ├── max_qps                           = 1000
  ├── max_concurrency                   = 1000
  ├── max_execution_records_per_flow    = 1000
  ├── url_regex_pattern                 = "^https://api\\.example\\.com/.*"
  ├── connector_max_versions            = 1000
  ├── flow_max_versions                 = 1000
  ├── flow_max_cache_ttl_seconds        = 1296000
  ├── flow_max_parallel_branches        = 8
  ├── script_max_length_chars           = 10000
  ├── script_max_timeout_seconds        = 30
  └── log_collection_enabled            = true

  /connector_platform_app_001/                  ← 应用 app_001 覆盖值
  ├── max_timeout_seconds               = 10
  └── script_max_length_chars           = 20000

  /connector_platform_app_002/                  ← 应用 app_002 覆盖值
  ├── max_timeout_seconds               = 8
  ├── max_qps                           = 500
  └── log_collection_enabled            = false
```

| path（命名空间） | code（配置项） | value | 说明 |
|:--:|------|:--:|------|
| `connector_platform` | `max_timeout_seconds` | `5` | 平台默认超时 5s |
| `connector_platform_app_001` | `max_timeout_seconds` | `10` | app_001 覆盖为 10s |
| `connector_platform_app_002` | `max_timeout_seconds` | `8` | app_002 覆盖为 8s |
| `connector_platform` | `max_qps` | `1000` | 平台默认 QPS |
| `connector_platform` | `log_collection_enabled` | `true` | 平台默认日志采集开启 |
| `connector_platform_app_002` | `log_collection_enabled` | `false` | app_002 关闭日志采集 |

**查询逻辑**：
```
不支持按应用区分的项（path 固定为 connector_platform）:
  SELECT value FROM openplatform_property_t
   WHERE path = 'connector_platform' AND code = 'connector_max_versions'

支持按应用区分的项（优先查应用专属路径）:
  1. SELECT value FROM openplatform_property_t
      WHERE path = 'connector_platform_app_{appId}' AND code = '{key}'
     → 命中则返回（应用覆盖值）
  2. 未命中 → SELECT value FROM openplatform_property_t
                WHERE path = 'connector_platform' AND code = '{key}'
     → 返回平台默认值
  3. 仍未命中 → 使用代码级 fallback 常量
```

**实例推演**：
- `app_001` 查 `max_timeout_seconds` → 命中 `(connector_platform_app_001, max_timeout_seconds)`，返回 `10`
- `app_003` 查 `max_timeout_seconds` → `connector_platform_app_003` 未命中 → 回退 `(connector_platform, max_timeout_seconds)`，返回 `5`
- `app_002` 查 `log_collection_enabled` → 命中 `(connector_platform_app_002, log_collection_enabled)`，返回 `false`
- 任意应用查 `connector_max_versions` → 直接查 `(connector_platform, connector_max_versions)`，返回 `1000`（不支持按应用区分）

**读取方式**：
- open-server 通过 MyBatis Mapper 直查 `openplatform_property_t`，按 `(path, code)` 查询
- connector-api 通过 R2DBC 直查同表，Redis 缓存（TTL 7d±2h），miss 回源 MySQL
- 版本发布/部署时主动清空对应应用路径的 Property 缓存

**与 Lookup 的区别**：

| 维度 | Lookup | Property |
|------|--------|----------|
| 数据库表 | `openplatform_lookup_classify_t` + `openplatform_lookup_item_t` | `openplatform_property_t` |
| 数据结构 | 分组-项 二级结构（1:N 列表） | 键值对（`path` 命名空间 + `code` 配置项） |
| 是否支持按应用覆盖 | ❌（全局值） | ✅（`path` 追加 `_app_{appId}` 后缀） |
| 回退模型 | 无 | 二层回退（应用值 > 平台默认值） |
| V3 用途 | 开放应用范围清单 | 超时/QPS/并发/缓存/分支/脚本/日志等 14 项 |
| path 约定 | `connector_platform` | `connector_platform` 或 `connector_platform_app_{appId}` |
| open-server 读取 | `LookupWhitelistMapper` 联查两张表 | MyBatis Mapper 直查 `openplatform_property_t` |

> 💡 **命名空间隔离**：`path=connector_platform` 将连接器平台的全部配置收敛到一个命名空间下，与 path 下其他业务模块（如能力开放平台 `capability_open_platform`）互不干扰。`code` 不再需要业务前缀，简洁直观。

## 附录 B：关联文档索引

| 文档 | 内容 |
|------|------|
| [spec.md §1.6](./spec.md#16-关键设计决策) | 关键设计决策：超时归属、限流归属、缓存、脚本节点、日志采集开关、引用稽核 |
| [spec.md §3.3](./spec.md#33-连接器配置g3g12) | 连接器配置 FR（认证类型、凭证位置、URL 白名单） |
| [spec.md §3.7](./spec.md#37-连接流编排--流级配置g8) | 流级配置 FR（超时、入站限流、SYSACCOUNT白名单、缓存） |
| [spec.md §3.8e](./spec.md#38e-连接流编排--脚本节点g8) | 脚本节点 FR（数量限制、源码长度、超时范围） |
| [spec.md §5.5](./spec.md#55-依赖关系) | 依赖关系：应用级参数存储于 market-server |
| [spec.md §6](./spec.md#6-边界情况) | 边界情况：EC-001~032 覆盖配置异常场景 |
| [plan-db.md](./plan-db.md) | 数据库设计：配置快照存储于 version 表的 JSON 列 |
| [plan-api.md](./plan-api.md) | API 设计：配置相关端点 |
| [plan-script.md](./plan-script.md) | 脚本执行引擎：GraalJS 沙箱配置细节 |
| [plan-cache.md](./plan-cache.md) | 缓存方案：缓存配置与版本切换一致性 |

## 附录 C：修订记录

| 版本 | 日期 | 修订内容 | 修订人 |
|------|------|---------|--------|
| v1.0 | 2026-06-26 | 初始版本：全量配置场景系统化记录（开关/白名单/上限/阈值/审批人/实体配置/校验策略） | SDDU |
| v1.2 | 2026-06-26 | 重构附录：新增 A.1 Spring 配置文件（open-server + connector-api），原 A.1 改名为 A.2 业务对象 JSON 配置 | SDDU |
| v1.1 | 2026-06-26 | 重构目录结构：拆为配置清单（§1）+ 配置详情（§2）+ 附录：平台配置能力（配置文件/Lookup/Property） | SDDU |

---

*最后更新: 2026-06-26 | 基于 spec.md v3.0 + plan.md v3.0*
