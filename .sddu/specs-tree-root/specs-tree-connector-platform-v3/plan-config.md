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
  - [2.1 开关](#21-开关)
  - [2.2 白名单](#22-白名单)
  - [2.3 上限/阈值](#23-上限阈值)
  - [2.4 审批人](#24-审批人)
  - [2.5 实体级配置](#25-实体级配置)
  - [2.6 校验时机策略](#26-校验时机策略)
- [附录 A：平台配置能力](#附录-a平台配置能力)
  - [A.1 Spring 配置文件](#a1-spring-配置文件)
  - [A.2 业务对象 JSON 配置](#a2-业务对象-json-配置)
  - [A.3 Lookup](#a3-lookup)
  - [A.4 Property](#a4-property)
- [附录 B：关联文档索引](#附录-b关联文档索引)
- [附录 C：修订记录](#附录-c修订记录)

---

## 1 配置清单

| # | 配置项 | 类型 | 配置者 | 存储位置 | FR |
|---|--------|------|--------|----------|----|
| 1 | 日志采集开关 | 开关 | 平台管理员+应用管理员 | market-server Property | FR-044 |
| 2 | URL 正则白名单 | 白名单 | 应用管理员 | ConnectorVersion 快照 | FR-015 |
| 3 | SYSTOKEN 凭证白名单 | 白名单 | 应用管理员 | FlowVersion 快照 | FR-036 |
| 4 | 应用白名单 | 白名单 | 平台管理员 | market-server Lookup | FR-045 |
| 5 | 节点超时上限 | 可配置上限 | 平台管理员 | market-server Property | FR-034 |
| 6 | 入站限流 QPS 上限 | 可配置上限 | 平台管理员 | market-server Property | FR-035 |
| 7 | 入站限流并发上限 | 可配置上限 | 平台管理员 | market-server Property | FR-035 |
| 8 | 运行记录条数上限 | 可配置上限 | 平台管理员 | market-server Property | FR-042 |
| 9 | 缓存 TTL 上限 | 硬编码上限 | 系统 | Java 常量 | FR-037 |
| 10 | 并行分支数上限 | 硬编码上限 | 系统 | Java 常量 | FR-038a |
| 11 | 版本数量上限 | 硬编码上限 | 系统 | Java 常量 | FR-005a, FR-024a |
| 12 | 脚本节点数量上限 | 硬编码上限 | 系统 | Java 常量 | FR-040a |
| 13 | 脚本源码长度上限 | 硬编码上限 | 系统 | Java 常量 | FR-040a |
| 14 | 脚本超时范围 | 硬编码上限 | 系统 | Java 常量 | FR-040a |
| 15 | 调试线程池上限 | 硬编码上限 | 系统 | Java 常量 | FR-041 |
| 16 | 调试超时 | 硬编码上限 | 系统 | Java 常量 | FR-041 |
| 17 | 三级审批人 | 审批人 | 平台管理员 | open-server | FR-032 |
| 18 | 连接器基本信息（名称/描述/协议） | 实体配置 | 应用管理员 | Connector + ConnectorVersion 快照 | FR-001, FR-005 |
| 19 | 认证类型（SOA/APIG/数字签名/Cookie） | 实体配置 | 应用管理员 | ConnectorVersion 快照 | FR-012, FR-014 |
| 20 | 凭证位置（Header/Query） | 实体配置 | 应用管理员 | ConnectorVersion 快照 | FR-013 |
| 21 | 入参/出参 JSON Schema | 实体配置 | 应用管理员 | ConnectorVersion 快照 | FR-047 |
| 22 | 数字签名 Secret Key | 实体配置 | 应用管理员 | 加密存储 | FR-012 |
| 23 | 连接流基本信息（名称/描述） | 实体配置 | 应用管理员 | Flow + FlowVersion 快照 | FR-016, FR-024 |
| 24 | 编排图（nodes + edges） | 实体配置 | 应用管理员 | FlowVersion 快照 | FR-024 |
| 25 | 触发器 SYSTOKEN 认证+白名单 | 实体配置 | 应用管理员 | FlowVersion 快照 | FR-036 |
| 26 | 连接器节点引用+超时 | 实体配置 | 应用管理员 | FlowVersion 快照 + 中间表 | FR-034, FR-039 |
| 27 | 脚本节点源码+超时 | 实体配置 | 应用管理员 | FlowVersion 快照 | FR-040a |
| 28 | flowConfig（限流 QPS+并发+缓存键+TTL） | 实体配置 | 应用管理员 | FlowVersion 快照 | FR-035, FR-037 |
| 29 | 并行分支数 | 实体配置 | 应用管理员 | FlowVersion 快照 | FR-038a |

---

## 2 配置详情

### 2.1 开关

#### 2.1.1 日志采集开关

| 属性 | 值 |
|------|-----|
| **配置者** | 平台管理员（设平台默认值）+ 应用管理员（按应用覆盖） |
| **作用域** | 按应用 |
| **默认值** | **开启** |
| **存储** | market-server Property（平台默认 + 按应用覆盖值）；未设独立值的应用回退使用平台默认 |
| **关联 FR** | FR-044 |
| **实现类** | `ExecutionStepService` (connector-api) |
| **行为** | |
| | 开启 → 每次节点执行写入节点级日志（输入/输出快照、耗时、错误信息） |
| | 关闭 → 不写节点日志，运行记录仅保留基础信息（触发时间、状态、耗时、触发方式） |
| | 关→开 → 立即恢复写入，关闭期间的执行不补采 |
| | 关闭期间 → 历史日志保留不变仍可查询；条数上限和保留天数清理策略对不再写入的日志无影响 |
| **边界情况** | EC-030（关闭时触发连接流）、EC-031（关→开切换）、EC-032（关闭期间查看运行记录详情） |

---

### 2.2 白名单

#### 2.2.1 URL 正则白名单

| 属性 | 值 |
|------|-----|
| **配置者** | 应用管理员（在连接器编辑页配置） |
| **作用域** | 连接器版本（快照于 ConnectorVersion） |
| **数据类型** | 多条正则表达式规则（字符串列表） |
| **空白含义** | **不限制**（允许调用任意地址）— 与 SYSTOKEN 白名单的空=禁止规则相反 |
| **存储** | ConnectorVersion 配置快照 JSON |
| **关联 FR** | FR-015 |
| **实现类** | `ConnectorUrlWhitelistValidator` (connector-api) |
| **校验时机** | |
| | 草稿保存时 — ❌ 不校验正则合法性 |
| | 发布时 (FR-007) — ✅ 校验正则语法合法性，不合法禁止发布 (EC-010) |
| | 运行时 — 每次连接器调用前逐条匹配，命中任一条→放行；全部不命中→拒绝 (EC-010) |
| **性能** | 组合正则编译 + Caffeine 缓存 5min，一次 `Matcher.matches()` |

#### 2.2.2 SYSTOKEN 凭证白名单

| 属性 | 值 |
|------|-----|
| **配置者** | 应用管理员（在连接流触发器节点认证配置中） |
| **作用域** | 连接流版本触发器节点 |
| **数据类型** | 凭证标识字符串列表 |
| **空白含义** | **全部禁止**（任何 SYSTOKEN 不可触发此连接流） |
| **存储** | FlowVersion.orchestrationConfig → 触发器节点 authConfig |
| **关联 FR** | FR-036 |
| **实现类** | `SystokenWhitelistValidator` (connector-api) |
| **运行时行为** | 请求 SYSTOKEN 不在白名单 → 返回 401 (EC-011) |

#### 2.2.3 应用白名单

| 属性 | 值 |
|------|-----|
| **配置者** | 平台管理员 |
| **作用域** | 平台全局 |
| **数据类型** | 应用 ID 列表 |
| **空白含义** | 全部禁止（非白名单应用不可使用连接器平台） |
| **存储** | market-server Lookup |
| **关联 FR** | FR-045 |
| **实现类** | `AppWhitelistService` (open-server) |
| **依赖** | 复用 market-server Lookup 能力 |
| **行为** | |
| | 白名单内应用 → 可用连接器平台全部功能 |
| | 非白名单应用 → 提示「该应用未开通连接器平台能力」 |
| | 应用被移出白名单 → 已有数据保留，新操作拒绝 (EC-015) |
| | market-server 不可用 → 白名单功能降级（放行策略由准入拦截器决定） |

---

### 2.3 上限/阈值

#### 2.3.1 可配置上限（平台管理员按应用覆盖）

以下上限为**二层取值模型**：平台管理员先设平台统一默认值，再按应用维度覆盖；应用未设独立值时回退使用平台默认。运行时取 `min(用户配置值, 应用上限)`。

| # | 配置项 | 平台默认值 | 配置者 | 存储 | 关联 FR | 边界情况 |
|:--:|--------|:---:|--------|------|:--:|------|
| 1 | **节点超时上限** | 5s | 平台管理员 / 应用管理员（用户侧配置节点超时值） | market-server Property | FR-034 | EC-028：发布时校验节点值 ≤ 应用上限，超限禁止提交 |
| 2 | **入站限流 QPS 上限** | 1000 | 同上 | market-server Property | FR-035 | EC-025：发布时校验 QPS ≤ 应用上限，超限禁止提交 |
| 3 | **入站限流并发上限** | 1000 | 同上 | market-server Property | FR-035 | 同上 |
| 4 | **运行记录条数上限** | 1000 条/流 | 平台管理员 | market-server Property | FR-042 | EC-029：FIFO 自动清理 + 30 天定期清理，两种策略互补 |
| 5 | **日志采集开关** | 开启 | 平台管理员 + 应用管理员 | market-server Property | FR-044 | 见 §2.1.1 |

#### 2.3.2 硬编码上限（不可配置）

以下上限为**系统级硬编码常量**，不可通过任何入口修改：

| # | 配置项 | 上限值 | 作用域 | 关联 FR | 边界情况 |
|:--:|--------|:---:|--------|:--:|------|
| 1 | **缓存 TTL** | 1296000 秒（15 天） | 全局 | FR-037 | EC-026：发布时校验，超限禁止提交 |
| 2 | **并行分支数** | 8 个分支 | 每并行处理节点 | FR-038a | EC-027：发布时校验，超限禁止提交 |
| 3 | **版本数量** | 1000 个 | 每连接器 / 每连接流 | FR-005a, FR-024a | EC-019/020：创建/复制草稿时校验，达上限禁止操作 |
| 4 | **脚本节点数量** | 10 个 | 每连接流 | FR-040a | 编排时前端限制 |
| 5 | **脚本源码长度** | 10000 字符 | 每个脚本节点 | FR-040a | 发布时校验 |
| 6 | **脚本超时范围** | 1~30s（默认 5s） | 每个脚本节点 | FR-040a | 用户可选 1~30s |
| 7 | **调试线程池** | max 5 线程 | connector-api 全局 | FR-041 | 独立于正常执行线程池 |
| 8 | **调试超时** | 30s | 每次调试执行 | FR-041 | 独立于正常执行超时 |

#### 2.3.3 用户侧可配置参数（发布时校验上限）

以下参数由**应用管理员**在编排连接流时配置，发布时校验不超过应用级/系统级上限：

| # | 配置项 | 用户可配范围 | 校验上限 | 关联 FR |
|:--:|--------|:---:|------|:--:|
| 1 | **连接器节点超时** | 秒，正整数（0 = 不限制） | min(节点值, 应用最大超时值) | FR-034 |
| 2 | **入站限流 QPS** | 0~∞（0 = 关闭） | min(流配置值, 应用最大 QPS) | FR-035 |
| 3 | **入站限流并发** | 0~∞（0 = 关闭） | min(流配置值, 应用最大并发) | FR-035 |
| 4 | **缓存 TTL** | 1~1296000 秒 | 1296000 秒（15 天） | FR-037 |
| 5 | **脚本超时** | 1~30 秒 | 30s | FR-040a |

---

### 2.4 审批人

#### 2.4.1 三级审批人配置

| 属性 | 值 |
|------|-----|
| **配置者** | 平台管理员 |
| **作用域** | 平台全局 |
| **存储** | open-server（改造现有审批人配置，增加应用隔离） |
| **关联 FR** | FR-032 |
| **依赖** | 复用开放平台审批引擎 |
| **变更生效** | 配置变更后对新提交的审批生效，已发起的审批不受影响 |

| 级别 | 配置粒度 | 说明 |
|:--:|------|------|
| **第一级：应用级** | 按应用单独配置 | 每个应用独立配置版本发布审批人 |
| **第二级：平台连接流级** | 一个全局值 | 所有连接流共享 |
| **第三级：全局级** | 一个全局值 | 最高级别审批 |

| 规则 | 说明 |
|------|------|
| 多人配置 | 每级可配置多人，任一审批通过即视为该级通过 |
| 审批顺序 | 应用级 → 平台连接流级 → 全局级，逐级发起 |
| 驳回 | 任意一级驳回 → 版本状态变为「已驳回」，驳回附带原因 |
| 撤回 | 提交人可在审批完成前撤回 → 版本状态变为「已撤回」 |
| 催办 | 同一节点可重复催办，无冷却限制 (FR-033) |
| 超时 | 超时未处理保持「待审批」状态，不影响催办行为 (EC-003) |

---

### 2.5 实体级配置

#### 2.5.1 连接器版本配置

| # | 配置项 | 配置者 | 存储位置 | 关联 FR |
|:--:|--------|--------|------|:--:|
| 1 | **基本信息**（名称、描述、协议等） | 应用管理员 | Connector 表 + ConnectorVersion 快照 | FR-001, FR-005 |
| 2 | **认证类型**（SOA / APIG / 数字签名 / Cookie，可多选 + 拖拽排序） | 应用管理员 | ConnectorVersion 配置快照 → authConfig | FR-012, FR-014 |
| 3 | **凭证位置**（Header / Query） | 应用管理员 | ConnectorVersion 配置快照 → authConfig | FR-013 |
| 4 | **URL 正则白名单** | 应用管理员 | ConnectorVersion 配置快照 → urlWhitelist | FR-015 |
| 5 | **入参/出参 JSON Schema** | 应用管理员 | ConnectorVersion 配置快照 | FR-047 |
| 6 | **数字签名 Secret Key** | 应用管理员 | 加密存储 | FR-012 |

#### 2.5.2 连接流版本配置

| # | 配置项 | 配置者 | 存储位置 | 关联 FR |
|:--:|--------|--------|------|:--:|
| 1 | **基本信息**（名称、描述等） | 应用管理员 | Flow 表 + FlowVersion 快照 | FR-016, FR-024 |
| 2 | **编排图**（nodes + edges） | 应用管理员 | FlowVersion.orchestrationConfig | FR-024 |
| 3 | **触发器节点** — SYSTOKEN 认证 + 白名单 | 应用管理员 | FlowVersion.orchestrationConfig → 触发器节点 | FR-036 |
| 4 | **连接器节点** — 引用的连接器版本号 | 应用管理员 | FlowVersion.orchestrationConfig → 连接器节点 + connector_version_ref 中间表 | FR-039 |
| 5 | **连接器节点** — 超时值 | 应用管理员 | FlowVersion.orchestrationConfig → 连接器节点 | FR-034 |
| 6 | **脚本节点** — 脚本源码 | 应用管理员 | FlowVersion.orchestrationConfig → 脚本节点.script | FR-040a |
| 7 | **脚本节点** — 超时值（1~30s） | 应用管理员 | FlowVersion.orchestrationConfig → 脚本节点.timeout | FR-040a |
| 8 | **flowConfig** — 入站限流（QPS + 并发） | 应用管理员 | FlowVersion.orchestrationConfig.flowConfig | FR-035 |
| 9 | **flowConfig** — 缓存键 + TTL | 应用管理员 | FlowVersion.orchestrationConfig.flowConfig | FR-037 |
| 10 | **并行处理节点** — 分支数（2~8） | 应用管理员 | FlowVersion.orchestrationConfig | FR-038a |

#### 2.5.3 应用级系统参数

以下参数由**平台管理员**在 market-server 侧维护，按应用维度配置。运行时通过 market-server Property 接口读取。未设独立值的应用回退使用**平台统一默认值**。

| # | 参数 | 平台默认值 | 运行时取值逻辑 | 关联 FR |
|:--:|------|:---:|------|:--:|
| 1 | **应用最大超时值** | 5s | `min(节点配置值, 应用最大超时值)` | FR-034 |
| 2 | **应用最大 QPS** | 1000 | `min(流配置值, 应用最大 QPS)` | FR-035 |
| 3 | **应用最大并发数** | 1000 | `min(流配置值, 应用最大并发数)` | FR-035 |
| 4 | **应用运行记录条数上限** | 1000 条/流 | FIFO 清理时使用此值 | FR-042 |
| 5 | **应用日志采集开关** | 开启 | 运行时判断是否写入节点日志 | FR-044 |

> 📌 **二层回退模型**：`应用独立值` > `平台统一默认值`。例如：平台默认超时 5s，应用 A 单独设为 10s，则应用 A 的最大超时值为 10s；应用 B 未设独立值，回退使用平台默认 5s。

---

### 2.6 校验时机策略

V3 采用**「保存时不校验，发布时统一卡口」**的策略：

```
草稿创建 ─── 仅 DB 存储约束校验（字段长度、数据类型等）
草稿编辑 ─── 仅 DB 存储约束校验
   │
   ▼
发布时 ───── 全部校验集中执行：
   ├── 业务必填字段（名称、描述等非空）
   ├── 配置非空（编排/入参出参 Schema）
   ├── URL 正则合法性 (FR-015)
   ├── JSON 语法合法性 (FR-047)
   ├── 脚本语法合法性 (FR-040a)
   ├── 入站限流 ≤ 应用上限 (FR-035)
   ├── 节点超时 ≤ 应用上限 (FR-034)
   ├── 缓存 TTL ≤ 1296000 (FR-037)
   ├── 并行分支 ≤ 8 (FR-038a)
   └── 连接器版本可用性 (FR-039)
```

| 场景 | DB 约束 | 业务必填 | 平台限制 | JSON 语法 | 正则合法性 | 引用可用性 |
|------|:---:|:---:|:---:|:---:|:---:|:---:|
| 创建空草稿 | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ |
| 编辑草稿保存 | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ |
| 复制到草稿 | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ |
| 一键复制连接流 | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ |
| **发布时 (FR-007 / FR-026)** | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |

---

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
| 调试线程池大小 | `DebugController.java` | max 5 线程 | 调试执行独立线程池 |
| 调试超时 | `DebugController.java` | 30s | 调试执行最大时长 |
| 脚本超时默认值 | `ScriptNodeExecutor.java` | 5s | 脚本节点默认超时（用户可配 1~30s） |
| 运行记录 30 天清理 | `ExecutionCleanupJob.java` | 30 天 | `@Scheduled` 定时清理过期运行记录 |

#### A.1.4 .gitignore 排除

生产环境配置文件中的敏感信息（数据库密码、Redis 密码）通过环境变量 `${MYSQL_PASSWORD}` / `${REDIS_PASSWORD}` 注入，不写入配置文件。`application-prod.yml` 中的占位符默认值仅用于本地开发验证。

### A.2 业务对象 JSON 配置

V3 采用 **「配置即快照」** 策略——连接器和连接流的运行时配置均存储在版本快照 JSON 中，不独立建配置表。

| 配置载体 | 存储方式 | 包含的配置项 | 变更生效 |
|----------|---------|-------------|---------|
| **ConnectorVersion 快照** | `connector_version_t.config_snapshot` JSON 列 | 认证类型、凭证位置、URL 白名单规则、入参/出参 Schema | 发布后立即生效，运行时按引用版本号读取 |
| **FlowVersion 快照** | `flow_version_t.orchestration_config` JSON 列 | 编排图（nodes/edges）、flowConfig（限流/缓存）、触发器认证/SYSTOKEN 白名单、节点超时、脚本源码、并行分支 | 部署绑定版本后生效 |

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
  /  (path="")                              ← 根目录
  ├── connector_platform_app_whitelist       ← 应用白名单（1:N 列表）
  │     ├── app_001
  │     ├── app_002
  │     └── app_003
  │
  /gray/ (path="gray")                      ← 灰度目录
  └── connector_platform_app_whitelist       ← 灰度白名单（独立分组）
        ├── app_001
        └── app_005
```

| path（目录） | classify_code（文件名） | item_value（文件内容） | 说明 |
|:--:|------|------|------|
| `""` | `connector_platform_app_whitelist` | `app_001`, `app_002`, `app_003` | 生产环境：三个应用开通连接器平台 |
| `gray` | `connector_platform_app_whitelist` | `app_001`, `app_005` | 灰度环境：仅两个应用可见 |

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

> 💡 **path 使用约定**：`path` 用于对配置归类分组。V3 连接器平台的应用白名单放在根目录 `path=""` 下。若后续需要按版本、环境等维度隔离，可通过不同 `path`（如 `path="v4"`、`path="gray"`）创建独立分组，互不干扰。

### A.4 Property

**用途**：存储键值对型应用级参数。平台管理员通过 market-web 界面维护，数据写入 MySQL；open-server / connector-api 直读 MySQL 获取配置。

**数据库表**（1 张）：

| 表名 | 用途 | 关键列 |
|------|------|--------|
| `openplatform_property_t` | 键值对属性 | `id` (PK, 雪花ID), `path`, `code`, `name`, `value`, `status` |

**设计模型**：类比文件系统 — `path` 相当于目录，`code` 相当于一个配置文件。这个文件内是一对一的键值数据（一行一个值），适合存储超时上限、限流上限等单值型配置。平台默认值放在根目录 `path=""`，应用覆盖值放在 `path="{appId}"` 子目录下。查询时先搜应用子目录，找不到再回退根目录。

**唯一约束**：`(path, code)` 联合唯一索引。这保证了同一个 `code` 下，`path=''` 平台默认行和 `path='{appId}'` 应用覆盖行各自唯一，不会出现同一应用的重复覆盖。

**二层回退模型**：`应用独立值` > `平台统一默认值`

**目录结构与示例数据**：

```
openplatform_property_t
  /  (path="")                              ← 根目录（平台默认值）
  ├── connector_platform.max_timeout_seconds         = 5
  ├── connector_platform.max_qps                     = 1000
  ├── connector_platform.max_concurrency             = 1000
  ├── connector_platform.max_execution_records       = 1000
  └── connector_platform.log_collection_enabled      = true
  /app_001/ (path="app_001")                ← 应用 app_001 子目录（覆盖值）
  └── connector_platform.max_timeout_seconds         = 10
  /app_002/ (path="app_002")                ← 应用 app_002 子目录（覆盖值）
  ├── connector_platform.max_timeout_seconds         = 8
  └── connector_platform.log_collection_enabled      = false
```

| path（目录） | code（文件名） | value（文件内容） | 说明 |
|:--:|------|:--:|------|
| `""` | `connector_platform.max_timeout_seconds` | `5` | 平台默认超时 5s |
| `app_001` | `connector_platform.max_timeout_seconds` | `10` | app_001 覆盖为 10s |
| `app_002` | `connector_platform.max_timeout_seconds` | `8` | app_002 覆盖为 8s |
| `""` | `connector_platform.max_qps` | `1000` | 平台默认 QPS 上限 |
| `""` | `connector_platform.max_concurrency` | `1000` | 平台默认并发上限 |
| `""` | `connector_platform.max_execution_records_per_flow` | `1000` | 平台默认运行记录条数上限 |
| `""` | `connector_platform.log_collection_enabled` | `true` | 平台默认日志采集开启 |
| `app_002` | `connector_platform.log_collection_enabled` | `false` | app_002 关闭日志采集 |

**查询逻辑**：`UK(path, code)` 保证同目录下文件名唯一。运行时取值分两步：
```
1. SELECT value FROM openplatform_property_t
    WHERE path = '{appId}' AND code = '{key}'     → 命中则返回（应用覆盖值）
2. 未命中 → SELECT value FROM openplatform_property_t
              WHERE path = '' AND code = '{key}'   → 返回平台默认值
3. 仍未命中 → 使用代码级 fallback 常量
```

**实例推演**：
- `app_001` 查 `max_timeout_seconds` → 第一步命中 `(app_001, max_timeout_seconds)`，返回 `10`
- `app_003` 查 `max_timeout_seconds` → 第一步 `(app_003, ...)` 未命中 → 第二步 `("", ...)` 命中，返回 `5`
- `app_002` 查 `log_collection_enabled` → 第一步命中 `(app_002, ...)`，返回 `false`

**读取方式**：
- open-server 通过 MyBatis Mapper 直查 `openplatform_property_t`，按 `(path, code)` 查询
- connector-api 通过 R2DBC 直查同表，Redis 缓存（TTL 7d±2h），miss 回源 MySQL
- 版本发布/部署时主动清空对应 appId 的 Property 缓存

**与 Lookup 的区别**：

| 维度 | Lookup | Property |
|------|--------|----------|
| 数据库表 | `openplatform_lookup_classify_t` + `openplatform_lookup_item_t` | `openplatform_property_t` |
| 数据结构 | 分组-项 二级结构（列表/枚举） | 键值对（`path` 区分作用域） |
| 是否支持按应用覆盖 | ❌（全局值，通过 classify_code 唯一标识） | ✅（`path=''` 平台默认 + `path='{appId}'` 应用覆盖） |
| 回退模型 | 无（只有全局值） | 二层回退（应用值 > 平台默认值） |
| V3 用途 | 应用白名单 | 超时上限、限流上限、运行记录上限、日志采集开关 |
| open-server 读取 | `LookupWhitelistMapper` 联查两张表 | MyBatis Mapper 直查 `openplatform_property_t` |

> 💡 **path 使用约定**：`path` 用于区分作用域 — 根目录 `""` 放平台默认值，子目录 `"{appId}"` 放应用覆盖值。平台管理员新增配置时，先在 `path=""` 下创建默认值行，再按需为特定应用在 `path="{appId}"` 下创建覆盖行。这样做的好处是：配置归属一目了然，不会出现"某个应用的限流值是多少来着"的困惑。

## 附录 B：关联文档索引

| 文档 | 内容 |
|------|------|
| [spec.md §1.6](./spec.md#16-关键设计决策) | 关键设计决策：超时归属、限流归属、缓存、脚本节点、日志采集开关、引用稽核 |
| [spec.md §3.3](./spec.md#33-连接器配置g3g12) | 连接器配置 FR（认证类型、凭证位置、URL 白名单） |
| [spec.md §3.7](./spec.md#37-连接流编排--流级配置g8) | 流级配置 FR（超时、入站限流、SYSTOKEN 白名单、缓存） |
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
