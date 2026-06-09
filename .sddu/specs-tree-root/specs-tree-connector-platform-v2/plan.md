# 技术计划：连接器平台 V2 — 多版本与增强

**Feature ID**: CONN-PLAT-002  
**状态**: planned  
**创建日期**: 2026-06-09  
**依赖**: CONN-PLAT-001（V1 MVP — 已建成并验证）  
**关联规范**: [spec.md](./spec.md)  
**关联文档**: [plan-json-schema.md](./plan-json-schema.md)（JSON Schema 设计规范，V2 沿用）

---

## 1. 架构分析

### 1.1 V1→V2 增量视角

V1 已建成以下基线能力：

| V1 组件 | 服务 | 技术栈 | V2 变更 |
|---------|------|--------|--------|
| 连接器 CRUD | open-server | Spring MVC + MyBatis | 叠加多版本管理、认证增强、URL 白名单 |
| 连接流 CRUD | open-server | Spring MVC + MyBatis | 叠加多版本管理、生命周期增强、一键复制 |
| 编排引擎 | open-server | 内存 DAG 拓扑排序 | 新增并行分支、数据处理节点、flowConfig 解析 |
| 运行时调度 | connector-api | WebFlux + R2DBC | 升级版本配置解析、运行记录、日志采集 |
| HTTP 触发器 | connector-api | WebFlux Router | 叠加 SYSTOKEN 白名单、入站限流 |
| 认证注入 | connector-api | Strategy 模式 | 新增 Cookie/DigitalSign 注入器 |
| 审批引擎 | open-server | ApprovalEngine | 新增 connector_flow_version_publish 业务类型 |
| 操作日志 | open-server | AOP + @AuditLog | 扩展 OperateEnum，新增连接器平台操作类型 |

### 1.2 新增核心组件

```
                    ┌────────────────────────────────────────┐
                    │            open-server (管理面)          │
                    │                                        │
                    │  ┌──────────┐  ┌────────────────────┐  │
                    │  │ 版本管理  │  │ 审批集成适配器       │  │
                    │  │ 服务     │  │ (复用 ApprovalEngine)│  │
                    │  └──────────┘  └────────────────────┘  │
                    │  ┌──────────┐  ┌────────────────────┐  │
                    │  │ URL白名单│  │ 应用白名单管理器     │  │
                    │  │ 校验器   │  │                    │  │
                    │  └──────────┘  └────────────────────┘  │
                    │  ┌──────────────────────────────────┐  │
                    │  │ 操作日志扩展 (OperateEnum + AOP)  │  │
                    │  └──────────────────────────────────┘  │
                    └────────────────────────────────────────┘
                                       │
                    ┌──────────────────┼──────────────────┐
                    │                  │                   │
                    ▼                  ▼                   ▼
              ┌──────────┐    ┌──────────────┐    ┌──────────┐
              │  MySQL   │    │    Redis     │    │ 审批引擎  │
              │ (版本快照 │    │ (限流令牌桶   │    │ (现有)   │
              │  运行记录 │    │  缓存存储)    │    │          │
              │  操作日志)│    └──────────────┘    └──────────┘
              └──────────┘
                                       │
                    ┌────────────────────────────────────────┐
                    │          connector-api (运行时)          │
                    │                                        │
                    │  ┌──────────┐  ┌────────────────────┐  │
                    │  │ 版本配置  │  │ 运行记录服务         │  │
                    │  │ 解析器   │  │ (写入+查询)          │  │
                    │  └──────────┘  └────────────────────┘  │
                    │  ┌──────────┐  ┌────────────────────┐  │
                    │  │ 日志采集  │  │ 调试执行器           │  │
                    │  │ 服务     │  │ (同步执行+结果回传)   │  │
                    │  └──────────┘  └────────────────────┘  │
                    │  ┌──────────┐  ┌────────────────────┐  │
                    │  │ 入站限流  │  │ 缓存服务             │  │
                    │  │ 拦截器   │  │ (Redis 键值缓存)     │  │
                    │  └──────────┘  └────────────────────┘  │
                    │  ┌──────────────────────────────────┐  │
                    │  │ 认证注入器扩展                    │  │
                    │  │ + CookieCredentialInjector       │  │
                    │  │ + DigitalSignCredentialInjector  │  │
                    │  │ + MultiAuthInjector (多选组合)    │  │
                    │  └──────────────────────────────────┘  │
                    └────────────────────────────────────────┘
```

### 1.3 数据流变更

**V1 数据流**：编辑即生效 → 运行时直接读当前版本配置 → 执行

**V2 数据流**：

```
┌─────────────┐   发布(审批)   ┌──────────────┐   部署    ┌─────────────┐
│ FlowVersion │ ──────────────▶│ FlowVersion  │ ────────▶│    Flow     │
│  (草稿)     │               │  (已发布)     │          │ deployed_   │
└─────────────┘               └──────────────┘          │ version_id  │
                                                        └──────┬──────┘
                                                               │
                                    ┌──────────────────────────┘
                                    ▼
┌─────────────┐   HTTP 触发   ┌──────────────┐   读版本快照   ┌──────────────┐
│  触发器      │ ────────────▶│  运行时引擎   │ ────────────▶│ FlowVersion  │
│ (connector-  │              │ (connector-   │              │ .orchestration│
│  api)        │              │  api)         │              │ Config       │
└─────────────┘              └──────┬───────┘              └──────────────┘
                                    │
                                    │ 读连接器版本快照
                                    ▼
                             ┌──────────────┐
                             │ConnectorVersion│
                             │ .connectionConfig│
                             └──────────────┘
```

关键变更：**运行时不再直接读写当前配置，而是通过 `deployed_version_id` 间接引用版本快照**。这是 V2 多版本架构的核心。

---

## 2. 关键技术决策

### 2.1 版本存储策略 (OQ-001)

| 方案 | 描述 | 优点 | 缺点 | 风险 |
|------|------|------|------|------|
| **A: 完整快照 (推荐)** | 每个版本存储完整的 `connectionConfig` / `orchestrationConfig` JSON | 版本自包含，回滚简单，读取无需重建，调试友好 | 存储空间线性增长 | 低：1000 版本上限控制 |
| B: 增量存储 | 仅存储与上一版本的 diff | 节省存储空间 | 读取需从基础版本重建，复杂度高，回滚慢，故障恢复困难 | 中：diff 算法引入 bug 风险 |
| C: 混合存储 | 每 N 个版本一个完整快照，中间版本存增量 | 兼顾空间与读取速度 | 实现复杂度最高，维护成本高 | 高：边界条件多 |

**决策：方案 A — 完整快照**（详见 ADR-004）

理由：
- V1 已在 `connectionConfig` / `orchestrationConfig` 列存储完整 JSON，V2 复用此模式
- 版本数量上限 1000 条，最坏情况存储开销可控（单版本 ~100KB，总计 ~100MB）
- 版本自包含（self-contained）是运维安全的基础：读取任意版本无需依赖其他版本数据
- 实施简单，V1→V2 数据迁移代价低（将现有单版本直接作为 v1）

### 2.2 版本号策略 (OQ-002)

| 方案 | 描述 | 优点 | 缺点 |
|------|------|------|------|
| **A: 实体内递增整数 (推荐)** | 每个 Connector/Flow 独立从 1 开始递增 | 直观，用户友好 | 跨实体不唯一 |
| B: 全局递增整数 | 全平台统一递增 | 全局唯一 | 用户不直观，连接器 v5 与连接流 v5 无关 |
| C: SemVer (x.y.z) | 语义化版本 | 表达兼容性语义 | V2 无需兼容性表达，过度设计 |

**决策：方案 A — 实体内递增整数**（详见 ADR-004）

版本号存储为 `version_number INT`，在创建草稿时分配（当前最大版本号 + 1），发布时沿用该版本号。

### 2.3 入站限流实现 (OQ-003, OQ-004)

| 方案 | 描述 | 优点 | 缺点 |
|------|------|------|------|
| **A: Redis 令牌桶 + Lua (推荐)** | Redis 执行 Lua 脚本实现原子令牌桶算法 | 原子操作，已有 Redis 基础设施，分布式友好 | 依赖 Redis 可用性 |
| B: 内存 Guava RateLimiter | 进程内存限流 | 无外部依赖，极低延迟 | 多实例不共享，不适用于 connector-api 多实例部署 |
| C: Sentinel / 专用限流服务 | 引入 Alibaba Sentinel 或自建限流服务 | 功能完善 | 引入新依赖，过度设计 |

**决策：方案 A — Redis 令牌桶 + Lua 脚本**（详见 ADR-005）

限流配置取值策略 (OQ-004)：
- 限流配置存储在 `FlowVersion.flowConfig` 中，随版本快照
- 运行时使用**已部署版本**的限流配置
- 部署新版本时，限流配置随版本切换即时生效

### 2.4 运行记录与日志存储 (OQ-007)

| 方案 | 描述 | 优点 | 缺点 |
|------|------|------|------|
| **A: MySQL 主存储 + 定时清理 (推荐)** | execution_record + execution_step 表存储，定时任务清理过期数据 | 已有表结构，事务一致，查询简单 | 大数据量下写入压力 |
| B: Elasticsearch | 日志写入 ES，运行记录留 MySQL | 日志检索能力强 | 引入新组件，运维复杂度增加 |
| C: MySQL + 对象存储归档 | 热数据 MySQL，冷数据自动迁移至对象存储 | 兼顾性能和成本 | 实现复杂度高 |

**决策：方案 A — MySQL 主存储 + 定时清理**

理由：
- V1 已预留 `execution_record_t` / `execution_step_t` 表，直接启用
- V2 运行日志量预估可控（单连接流 TPS ≤ 300）
- 定时清理策略：保留最近 30 天数据，每天凌晨执行清理任务
- 若后续数据量超预期，可平滑迁移至方案 C

### 2.5 审批集成范围 (OQ-005)

**决策：复用开放平台 ApprovalEngine，不改造审批引擎本身**

集成方式：
- 新增 `businessType = "connector_flow_version_publish"` 审批流模板
- 三级审批映射到现有 ApprovalEngine 的三级节点模型：
  - 资源级 (RESOURCE) → 应用级版本发布审批人
  - 场景级 (SCENE) → 平台级连接流统一审批人
  - 全局级 (GLOBAL) → 全局审批人
- 审批人配置存储：新增 `connector_platform_approver_config` 表
- 催办复用 `ApprovalController.urge()` 接口

### 2.6 缓存一致性策略 (OQ-006)

**决策：版本变更时主动清空 + TTL 兜底**

- 缓存键格式：`cp:flow:{flowId}:cache:{cacheKey}`，TTL 由 flowConfig 配置
- 触发清理的场景：
  - 版本发布 → 清空该 Flow 所有版本缓存（避免新旧版本缓存混用）
  - 版本失效 → 清空该版本的缓存
  - 部署新版本 → 清空该 Flow 所有缓存
  - Flow 停止 → 清空该 Flow 所有缓存
- 缓存未命中：正常执行 DAG，不中断流程（EC-012）

### 2.7 一键复制版本历史处理 (OQ-008)

**决策：完整复制所有版本（含所有状态）**

- 复制时创建新 Flow 实体 + 复制全部 FlowVersion 记录到新 Flow
- 版本号沿用（保持历史一致性），新 Flow 的草稿版本号继续在原最大版本号上递增
- 复制后新 Flow 状态 = 待部署，`deployed_version_id = NULL`
- 复制幂等：若名称后缀碰撞，自动重试（EC-017）
- 不清理任何版本历史——完整复制保证新 Flow 可以查看全部版本变更轨迹

---

## 3. 数据库变更设计

### 3.1 表结构变更概览

| 表 | 变更类型 | 说明 |
|----|---------|------|
| `openplatform_v2_cp_connector_t` | MODIFY | 启用 `status` 字段；新增 `app_id` 字段 |
| `openplatform_v2_cp_connector_version_t` | MODIFY | 1:1 → 1:N（移除 UNIQUE(connectorId)）；新增 `version_number`、`status`、`published_time`、`published_by` |
| `openplatform_v2_cp_flow_t` | MODIFY | 启用 `lifecycle_status` 扩展状态值；新增 `deployed_version_id`、`app_id` 字段 |
| `openplatform_v2_cp_flow_version_t` | MODIFY | 1:1 → 1:N；新增 `version_number`、`status`、`flow_config`、`submitted_time`、`published_time`、`published_by` |
| `openplatform_v2_cp_connector_version_ref_t` | NEW | 连接器版本引用中间表（M:N），保存编排时同步维护 |
| `openplatform_v2_cp_execution_record_t` | MODIFY | 启用；新增 `trigger_type`、`flow_version_id` 字段 |
| `openplatform_v2_cp_execution_step_t` | MODIFY | 启用；适配 V2 节点类型 |
| `openplatform_v2_cp_connector_url_whitelist_t` | NEW | URL 白名单规则存储 |
| `openplatform_v2_cp_app_whitelist_t` | NEW | 应用白名单 |
| `openplatform_v2_cp_approver_config_t` | NEW | 审批人配置 |
| `openplatform_v2_approval_flow_t` | MODIFY | 新增 businessType 模板 |
| `openplatform_operate_log_t` | MODIFY | OperateEnum 扩展新操作类型 |

### 3.2 核心 DDL 变更（关键字段）

```sql
-- 连接器表：启用 status，新增 app_id
ALTER TABLE openplatform_v2_cp_connector_t
    ADD COLUMN app_id BIGINT(20) NOT NULL DEFAULT 0 COMMENT '归属应用ID',
    MODIFY COLUMN status TINYINT(10) NOT NULL DEFAULT 1 COMMENT '状态：1=有效不可用, 2=有效可用, 3=已失效, 4=物理删除',
    ADD INDEX idx_app_id (app_id);

-- 连接器版本表：1:1 → 1:N，新增版本号+状态+发布时间
ALTER TABLE openplatform_v2_cp_connector_version_t
    DROP INDEX uk_connector_id,
    ADD COLUMN version_number INT NOT NULL DEFAULT 1 COMMENT '版本号，实体内递增',
    ADD COLUMN status TINYINT(10) NOT NULL DEFAULT 1 COMMENT '状态：1=草稿, 2=已发布, 3=已失效, 4=物理删除',
    ADD COLUMN published_time DATETIME(3) NULL COMMENT '发布时间',
    ADD COLUMN published_by VARCHAR(64) NULL COMMENT '发布人',
    ADD INDEX idx_connector_version (connector_id, version_number),
    ADD INDEX idx_connector_status (connector_id, status);

-- 连接流表：扩展生命周期状态，新增部署版本+app_id
ALTER TABLE openplatform_v2_cp_flow_t
    ADD COLUMN deployed_version_id BIGINT(20) NULL COMMENT '当前部署的版本ID',
    ADD COLUMN app_id BIGINT(20) NOT NULL DEFAULT 0 COMMENT '归属应用ID',
    MODIFY COLUMN lifecycle_status TINYINT(10) NOT NULL DEFAULT 1 COMMENT '生命周期：1=待部署, 2=运行中, 3=已停止, 4=已失效, 5=物理删除',
    ADD INDEX idx_app_id (app_id),
    ADD INDEX idx_deployed_version (deployed_version_id);

-- 连接流版本表：1:1 → 1:N，新增版本号+状态+flow_config+审批相关
ALTER TABLE openplatform_v2_cp_flow_version_t
    DROP INDEX uk_flow_id,
    ADD COLUMN version_number INT NOT NULL DEFAULT 1 COMMENT '版本号，实体内递增',
    ADD COLUMN status TINYINT(10) NOT NULL DEFAULT 1 COMMENT '状态：1=草稿, 2=待审批, 3=已撤回, 4=已驳回, 5=已发布, 6=已失效, 7=物理删除',
    ADD COLUMN flow_config MEDIUMTEXT NULL COMMENT '流级配置JSON：超时/限流/缓存',
    ADD COLUMN submitted_time DATETIME(3) NULL COMMENT '提交审批时间',
    ADD COLUMN published_time DATETIME(3) NULL COMMENT '发布时间',
    ADD COLUMN published_by VARCHAR(64) NULL COMMENT '发布人',
    ADD INDEX idx_flow_version (flow_id, version_number),
    ADD INDEX idx_flow_status (flow_id, status);

-- 新增：连接器版本引用中间表
CREATE TABLE openplatform_v2_cp_connector_version_ref_t (
    id BIGINT(20) NOT NULL COMMENT '雪花ID',
    flow_version_id BIGINT(20) NOT NULL COMMENT '连接流版本ID',
    connector_version_id BIGINT(20) NOT NULL COMMENT '连接器版本ID',
    node_id VARCHAR(64) NOT NULL COMMENT '编排节点ID',
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    INDEX idx_flow_version (flow_version_id),
    INDEX idx_connector_version (connector_version_id),
    UNIQUE KEY uk_flow_node (flow_version_id, node_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='连接器版本引用中间表';

-- 新增：URL 白名单表
CREATE TABLE openplatform_v2_cp_connector_url_whitelist_t (
    id BIGINT(20) NOT NULL COMMENT '雪花ID',
    connector_id BIGINT(20) NOT NULL COMMENT '连接器ID',
    pattern VARCHAR(512) NOT NULL COMMENT '正则表达式规则',
    description VARCHAR(256) NULL COMMENT '规则说明',
    is_deleted TINYINT(10) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    last_update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    INDEX idx_connector (connector_id, is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='连接器URL正则白名单';

-- 新增：应用白名单表
CREATE TABLE openplatform_v2_cp_app_whitelist_t (
    id BIGINT(20) NOT NULL COMMENT '雪花ID',
    app_id BIGINT(20) NOT NULL COMMENT '应用ID',
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    create_by VARCHAR(64) NOT NULL COMMENT '创建人',
    PRIMARY KEY (id),
    UNIQUE KEY uk_app_id (app_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='连接器平台应用白名单';

-- 新增：审批人配置表
CREATE TABLE openplatform_v2_cp_approver_config_t (
    id BIGINT(20) NOT NULL COMMENT '雪花ID',
    level TINYINT(10) NOT NULL COMMENT '审批级别：1=应用级, 2=平台连接流级, 3=全局级',
    app_id BIGINT(20) NULL COMMENT '应用ID（仅 level=1 时使用）',
    approver_ids JSON NOT NULL COMMENT '审批人用户ID列表',
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    last_update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_level_app (level, app_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='连接流版本发布审批人配置';

-- 运行记录表：启用并扩展
ALTER TABLE openplatform_v2_cp_execution_record_t
    ADD COLUMN trigger_type VARCHAR(32) NOT NULL DEFAULT 'http' COMMENT '触发方式：http/debug',
    ADD COLUMN flow_version_id BIGINT(20) NULL COMMENT '执行的连接流版本ID',
    ADD INDEX idx_flow_trigger_time (flow_id, trigger_time),
    ADD INDEX idx_trigger_time (trigger_time);
```

### 3.3 状态枚举定义

参见 spec.md §1.7 核心业务对象生命周期。关键枚举值：

| 枚举 | 表 | 列 | 值 |
|------|-----|-----|-----|
| connector.status | connector_t | status | 1=有效不可用, 2=有效可用, 3=已失效, 4=物理删除 |
| connector_version.status | connector_version_t | status | 1=草稿, 2=已发布, 3=已失效, 4=物理删除 |
| flow.lifecycle_status | flow_t | lifecycle_status | 1=待部署, 2=运行中, 3=已停止, 4=已失效, 5=物理删除 |
| flow_version.status | flow_version_t | status | 1=草稿, 2=待审批, 3=已撤回, 4=已驳回, 5=已发布, 6=已失效, 7=物理删除 |

---

## 4. 后端模块设计

### 4.1 open-server（管理面）新增/修改模块

```
open-server/src/main/java/com/xxx/it/works/wecode/v2/modules/
├── connector/
│   ├── entity/
│   │   ├── Connector.java              # [MODIFY] 新增 appId, status 字段逻辑
│   │   └── ConnectorVersion.java       # [MODIFY] 新增 versionNumber, status, publishedTime, publishedBy
│   ├── model/
│   │   ├── ConnectionConfig.java       # [MODIFY] 新增 authTypeMulti 认证多选, cookieConfig, digitalSignConfig
│   │   ├── AuthConfig.java             # [MODIFY] 支持认证多选数组
│   │   └── UrlWhitelistRule.java       # [NEW] URL 白名单规则模型
│   ├── controller/
│   │   └── OpConnectorController.java  # [MODIFY] 新增版本管理、复制到草稿、URL白名单接口
│   ├── service/
│   │   ├── ConnectorService.java       # [MODIFY] 版本生命周期管理
│   │   ├── ConnectorVersionService.java # [NEW] 版本 CRUD + 状态流转
│   │   └── ConnectorUrlWhitelistService.java # [NEW] URL 白名单规则管理
│   └── mapper/
│       ├── ConnectorMapper.java        # [MODIFY] 新增 app_id 条件
│       └── ConnectorVersionMapper.java # [MODIFY] 1:N 查询
│
├── flow/
│   ├── entity/
│   │   ├── Flow.java                   # [MODIFY] 新增 deployedVersionId, appId, lifecycleStatus 扩展
│   │   └── FlowVersion.java            # [MODIFY] 新增 versionNumber, status, flowConfig, 审批字段
│   ├── model/
│   │   ├── OrchestrationConfig.java    # [MODIFY] 新增 flowConfig, 并行边, data_processor 节点
│   │   ├── FlowConfig.java             # [NEW] 流级配置：超时/限流/缓存
│   │   ├── FlowNode.java               # [MODIFY] 新增 data_processor 节点类型
│   │   ├── FlowEdge.java               # [MODIFY] 新增 connectionMode (serial/parallel)
│   │   └── DataProcessorConfig.java    # [NEW] 数据处理节点配置
│   ├── controller/
│   │   └── OpFlowController.java       # [MODIFY] 新增版本管理、部署、复制、运行记录接口
│   ├── service/
│   │   ├── FlowService.java            # [MODIFY] 生命周期管理 + 部署逻辑
│   │   ├── FlowVersionService.java     # [NEW] 版本 CRUD + 状态流转 + 审批提交
│   │   ├── FlowCopyService.java        # [NEW] 一键复制服务
│   │   └── FlowExecutionRecordService.java # [NEW] 运行记录查询
│   └── mapper/
│       ├── FlowMapper.java             # [MODIFY] 新增 deployed_version_id, app_id
│       └── FlowVersionMapper.java      # [MODIFY] 1:N 查询
│
├── approval/
│   ├── engine/
│   │   └── ApprovalEngine.java         # [MODIFY] 支持 businessType=connector_flow_version_publish
│   └── service/
│       └── ConnectorPlatformApprovalService.java # [NEW] 审批集成适配器
│
├── security/
│   ├── AppWhitelistService.java        # [NEW] 应用白名单管理
│   └── AppWhitelistInterceptor.java    # [NEW] 白名单准入拦截器
│
├── auditlog/
│   ├── entity/
│   │   └── OperateLog.java             # [MODIFY] 扩展 operateType 可记录对象
│   ├── enums/
│   │   └── OperateEnum.java            # [MODIFY] 新增 CONNECTOR_*, FLOW_* 操作类型
│   └── interceptor/
│       └── OperateLogV2Aspect.java     # [MODIFY] 扩展 EntitySnapshotLoader 支持连接器平台实体
│
└── debug/
    └── DebugProxyController.java       # [NEW] 调试触发代理（转发到 connector-api 调试接口）
```

### 4.2 connector-api（运行时）新增/修改模块

```
connector-api/src/main/java/com/xxx/it/works/wecode/v2/modules/
├── connector/
│   └── entity/
│       ├── ConnectorEntity.java        # [MODIFY] 新增 status, appId 字段映射
│       └── ConnectorVersionEntity.java # [MODIFY] 1:N 查询，新增 status 过滤
│
├── flow/
│   └── entity/
│       ├── FlowEntity.java             # [MODIFY] 新增 deployedVersionId, appId
│       └── FlowVersionEntity.java      # [MODIFY] 1:N 查询，新增 flowConfig 解析
│
├── runtime/
│   ├── FlowRuntimeEngine.java          # [MODIFY] 升级：版本配置解析，并行分支执行
│   ├── FlowConfigParser.java           # [NEW] flowConfig 解析（超时/限流/缓存）
│   ├── ParallelBranchExecutor.java     # [NEW] 并行分支执行器
│   ├── DataProcessorExecutor.java      # [NEW] 数据处理节点执行（字段类型转换）
│   └── VersionConfigResolver.java      # [NEW] 版本配置解析（按 deployed_version_id 读取）
│
├── ratelimit/
│   ├── InboundRateLimiter.java         # [NEW] 入站限流拦截器（Redis 令牌桶）
│   └── RateLimitConfigParser.java      # [NEW] 限流配置读取
│
├── cache/
│   ├── FlowCacheManager.java           # [NEW] 连接流缓存管理（Redis 键值缓存）
│   └── CacheInvalidator.java           # [NEW] 缓存失效处理
│
├── auth/
│   ├── credential/
│   │   ├── CookieCredentialInjector.java     # [NEW] Cookie 认证注入器
│   │   ├── DigitalSignCredentialInjector.java # [NEW] 数字签名认证注入器
│   │   └── MultiAuthCredentialInjector.java  # [NEW] 多认证组合注入器
│   └── impl/
│       └── SystokenWhitelistValidator.java   # [NEW] SYSTOKEN 白名单校验器
│
├── execution/
│   ├── ExecutionRecordService.java     # [NEW] 运行记录写入
│   ├── ExecutionStepService.java       # [NEW] 节点日志写入
│   └── ExecutionLogCleanupTask.java    # [NEW] 日志定时清理任务
│
├── debug/
│   └── DebugExecutionService.java      # [NEW] 调试执行服务（同步执行+结果回传）
│
└── security/
    └── UrlWhitelistValidator.java      # [NEW] URL 白名单运行时校验
```

### 4.3 认证注入器扩展设计

V1 已有 `SoaCredentialInjector` 和 `ApigCredentialInjector`，使用 Strategy 模式。V2 扩展：

```java
// V2 新增注入器
@Component
public class CookieCredentialInjector implements CredentialInjector {
    // 从连接器版本快照读取 cookieConfig（仅存储 name 占位）
    // 运行时从 inputMapping 读取实际 cookie 值
}

@Component
public class DigitalSignCredentialInjector implements CredentialInjector {
    // 读取 secretKey（加密存储）+ 签名算法（平台统一）
    // 根据凭证位置配置（Header/Query）注入签名
}

@Component
public class MultiAuthCredentialInjector implements CredentialInjector {
    // 按排序依次调用各个单一认证注入器
    // 使用 @Order 或配置中的排序字段
}
```

### 4.4 数据处理节点执行器设计

字段类型转换函数（本期仅四种）：

```java
public class DataProcessorExecutor {
    // 转换函数注册表
    private final Map<String, Function<Object, Object>> converters = Map.of(
        "toString",  obj -> obj == null ? null : obj.toString(),
        "toNumber",  obj -> { /* String/Integer → Number */ },
        "toBoolean", obj -> { /* 0/1/"true"/"false" → Boolean */ },
        "formatDate", obj -> { /* Date format conversion */ }
    );
    
    // 递归解析值来源（支持嵌套函数调用）
    private Object resolveValue(FieldMapping mapping, NodeContext ctx) { ... }
    
    // 执行转换
    public NodeOutput execute(DataProcessorConfig config, NodeContext ctx) { ... }
}
```

---

## 5. 前端变更设计

### 5.1 新增/修改页面

```
wecodesite/src/pages/ConnectPlatform/
├── Connector/
│   ├── index.jsx              # [MODIFY] 新增「复制到草稿」操作列
│   └── constants.jsx          # [MODIFY] 新增版本状态列
│
├── ConnectorEditor/
│   ├── index.jsx              # [MODIFY] 新增认证多选、Cookie配置、数字签名配置、URL白名单配置
│   ├── AuthConfigPanel.jsx    # [NEW] 认证配置面板（多选+排序）
│   ├── UrlWhitelistPanel.jsx  # [NEW] URL 白名单正则规则管理
│   └── VersionHistoryPanel.jsx # [NEW] 版本历史列表 + 切换查看
│
├── Flow/
│   ├── index.jsx              # [MODIFY] 新增「复制」操作按钮，生命周期状态列
│   └── constants.jsx          # [MODIFY] 新增版本状态、生命周期状态
│
├── FlowEditor/
│   ├── index.jsx              # [MODIFY] 新增并行边切换、连接器版本选择、数据处理节点
│   ├── NodeLibrary.jsx        # [MODIFY] 新增「数据处理」节点类型
│   ├── NodeProperties.jsx     # [MODIFY] 新增超时配置、SYSTOKEN白名单
│   ├── FlowConfigPanel.jsx    # [NEW] flowConfig 配置（限流/缓存）
│   ├── VersionSelectPanel.jsx # [NEW] 连接器版本选择面板
│   └── DebugPanel.jsx         # [NEW] 调试面板（输入模拟数据+结果展示）
│
├── FlowVersion/
│   ├── VersionHistory.jsx     # [NEW] 版本历史列表 + 切换查看 + 复制到草稿
│   ├── ApprovalPanel.jsx      # [NEW] 审批状态查看 + 催办按钮
│   └── ApprovalConfigModal.jsx # [NEW] 审批人配置弹窗（平台管理员）
│
├── ExecutionRecord/
│   ├── ExecutionRecordList.jsx # [NEW] 运行记录列表（过滤+分页）
│   └── ExecutionDetail.jsx    # [NEW] 运行详情（各节点执行状态+日志）
│
└── OperationLog/
    └── (复用现有)              # [EXTEND] 新增连接器平台操作类型过滤
```

### 5.2 前端组件变更

```
wecodesite/src/components/
├── CustomFlowNodes/
│   ├── TriggerNode.jsx         # [MODIFY] 新增 SYSTOKEN 白名单配置入口
│   ├── ActionNode.jsx          # [MODIFY] 新增版本标签显示
│   ├── DataProcessorNode.jsx   # [NEW] 数据处理节点渲染
│   └── FlowNodes.m.less        # [MODIFY] 新增节点样式
│
├── FlowCanvas/
│   └── FlowCanvasWrapper.jsx   # [MODIFY] 支持并行边渲染
│
└── SchemaEditor/
    └── SchemaEditor.jsx        # [MODIFY] 新增数据类型严格校验（FR-047）
```

### 5.3 Admin 页面

```
wecodesite/src/pages/Admin/
├── ConnectorPlatform/
│   ├── AppWhitelistManager.jsx  # [NEW] 应用白名单管理
│   └── ApprovalConfigManager.jsx # [NEW] 三级审批人配置
```

---

## 6. 文件影响分析

### 6.1 完整文件清单

```
[NEW] open-server/src/main/java/com/xxx/it/works/wecode/v2/modules/connector/service/ConnectorVersionService.java
[NEW] open-server/src/main/java/com/xxx/it/works/wecode/v2/modules/connector/service/ConnectorUrlWhitelistService.java
[NEW] open-server/src/main/java/com/xxx/it/works/wecode/v2/modules/connector/model/UrlWhitelistRule.java
[NEW] open-server/src/main/java/com/xxx/it/works/wecode/v2/modules/flow/service/FlowVersionService.java
[NEW] open-server/src/main/java/com/xxx/it/works/wecode/v2/modules/flow/service/FlowCopyService.java
[NEW] open-server/src/main/java/com/xxx/it/works/wecode/v2/modules/flow/service/FlowExecutionRecordService.java
[NEW] open-server/src/main/java/com/xxx/it/works/wecode/v2/modules/flow/model/FlowConfig.java
[NEW] open-server/src/main/java/com/xxx/it/works/wecode/v2/modules/flow/model/DataProcessorConfig.java
[NEW] open-server/src/main/java/com/xxx/it/works/wecode/v2/modules/approval/service/ConnectorPlatformApprovalService.java
[NEW] open-server/src/main/java/com/xxx/it/works/wecode/v2/modules/security/AppWhitelistService.java
[NEW] open-server/src/main/java/com/xxx/it/works/wecode/v2/modules/security/AppWhitelistInterceptor.java
[NEW] open-server/src/main/java/com/xxx/it/works/wecode/v2/modules/debug/DebugProxyController.java
[NEW] open-server/src/main/resources/db/migration/V3__connector_platform_v2_schema.sql
[NEW] connector-api/src/main/java/com/xxx/it/works/wecode/v2/modules/runtime/FlowConfigParser.java
[NEW] connector-api/src/main/java/com/xxx/it/works/wecode/v2/modules/runtime/ParallelBranchExecutor.java
[NEW] connector-api/src/main/java/com/xxx/it/works/wecode/v2/modules/runtime/DataProcessorExecutor.java
[NEW] connector-api/src/main/java/com/xxx/it/works/wecode/v2/modules/runtime/VersionConfigResolver.java
[NEW] connector-api/src/main/java/com/xxx/it/works/wecode/v2/modules/ratelimit/InboundRateLimiter.java
[NEW] connector-api/src/main/java/com/xxx/it/works/wecode/v2/modules/ratelimit/RateLimitConfigParser.java
[NEW] connector-api/src/main/java/com/xxx/it/works/wecode/v2/modules/cache/FlowCacheManager.java
[NEW] connector-api/src/main/java/com/xxx/it/works/wecode/v2/modules/cache/CacheInvalidator.java
[NEW] connector-api/src/main/java/com/xxx/it/works/wecode/v2/modules/auth/credential/CookieCredentialInjector.java
[NEW] connector-api/src/main/java/com/xxx/it/works/wecode/v2/modules/auth/credential/DigitalSignCredentialInjector.java
[NEW] connector-api/src/main/java/com/xxx/it/works/wecode/v2/modules/auth/credential/MultiAuthCredentialInjector.java
[NEW] connector-api/src/main/java/com/xxx/it/works/wecode/v2/modules/auth/impl/SystokenWhitelistValidator.java
[NEW] connector-api/src/main/java/com/xxx/it/works/wecode/v2/modules/execution/ExecutionRecordService.java
[NEW] connector-api/src/main/java/com/xxx/it/works/wecode/v2/modules/execution/ExecutionStepService.java
[NEW] connector-api/src/main/java/com/xxx/it/works/wecode/v2/modules/execution/ExecutionLogCleanupTask.java
[NEW] connector-api/src/main/java/com/xxx/it/works/wecode/v2/modules/debug/DebugExecutionService.java
[NEW] connector-api/src/main/java/com/xxx/it/works/wecode/v2/modules/security/UrlWhitelistValidator.java

[MODIFY] open-server/src/main/java/.../connector/entity/Connector.java
[MODIFY] open-server/src/main/java/.../connector/entity/ConnectorVersion.java
[MODIFY] open-server/src/main/java/.../connector/controller/OpConnectorController.java
[MODIFY] open-server/src/main/java/.../connector/service/ConnectorService.java
[MODIFY] open-server/src/main/java/.../connector/mapper/ConnectorMapper.java
[MODIFY] open-server/src/main/java/.../connector/mapper/ConnectorVersionMapper.java
[MODIFY] open-server/src/main/java/.../connector/model/ConnectionConfig.java
[MODIFY] open-server/src/main/java/.../connector/model/AuthConfig.java
[MODIFY] open-server/src/main/java/.../flow/entity/Flow.java
[MODIFY] open-server/src/main/java/.../flow/entity/FlowVersion.java
[MODIFY] open-server/src/main/java/.../flow/controller/OpFlowController.java
[MODIFY] open-server/src/main/java/.../flow/service/FlowService.java
[MODIFY] open-server/src/main/java/.../flow/mapper/FlowMapper.java
[MODIFY] open-server/src/main/java/.../flow/mapper/FlowVersionMapper.java
[MODIFY] open-server/src/main/java/.../flow/model/OrchestrationConfig.java
[MODIFY] open-server/src/main/java/.../flow/model/FlowNode.java
[MODIFY] open-server/src/main/java/.../flow/model/FlowEdge.java
[MODIFY] open-server/src/main/java/.../approval/engine/ApprovalEngine.java
[MODIFY] open-server/src/main/java/.../auditlog/entity/OperateLog.java
[MODIFY] open-server/src/main/java/.../auditlog/enums/OperateEnum.java
[MODIFY] open-server/src/main/java/.../auditlog/interceptor/OperateLogV2Aspect.java
[MODIFY] connector-api/src/main/java/.../connector/entity/ConnectorEntity.java
[MODIFY] connector-api/src/main/java/.../connector/entity/ConnectorVersionEntity.java
[MODIFY] connector-api/src/main/java/.../flow/entity/FlowEntity.java
[MODIFY] connector-api/src/main/java/.../flow/entity/FlowVersionEntity.java
[MODIFY] connector-api/src/main/java/.../runtime/FlowRuntimeEngine.java
[MODIFY] connector-api/src/main/java/.../auth/AuthValidatorRegistry.java
[MODIFY] connector-api/src/main/java/.../auth/credential/CredentialInjectorRegistry.java
[MODIFY] connector-api/src/main/java/.../trigger/controller/OpTriggerController.java

[DELETE] (无 — 不删除 V1 代码，仅叠加修改)

[NEW] wecodesite/src/pages/ConnectPlatform/ConnectorEditor/AuthConfigPanel.jsx
[NEW] wecodesite/src/pages/ConnectPlatform/ConnectorEditor/UrlWhitelistPanel.jsx
[NEW] wecodesite/src/pages/ConnectPlatform/ConnectorEditor/VersionHistoryPanel.jsx
[NEW] wecodesite/src/pages/ConnectPlatform/FlowEditor/FlowConfigPanel.jsx
[NEW] wecodesite/src/pages/ConnectPlatform/FlowEditor/VersionSelectPanel.jsx
[NEW] wecodesite/src/pages/ConnectPlatform/FlowEditor/DebugPanel.jsx
[NEW] wecodesite/src/pages/ConnectPlatform/FlowVersion/VersionHistory.jsx
[NEW] wecodesite/src/pages/ConnectPlatform/FlowVersion/ApprovalPanel.jsx
[NEW] wecodesite/src/pages/ConnectPlatform/FlowVersion/ApprovalConfigModal.jsx
[NEW] wecodesite/src/pages/ConnectPlatform/ExecutionRecord/ExecutionRecordList.jsx
[NEW] wecodesite/src/pages/ConnectPlatform/ExecutionRecord/ExecutionDetail.jsx
[NEW] wecodesite/src/pages/Admin/ConnectorPlatform/AppWhitelistManager.jsx
[NEW] wecodesite/src/pages/Admin/ConnectorPlatform/ApprovalConfigManager.jsx
[NEW] wecodesite/src/components/CustomFlowNodes/DataProcessorNode.jsx

[MODIFY] wecodesite/src/pages/ConnectPlatform/Connector/index.jsx
[MODIFY] wecodesite/src/pages/ConnectPlatform/Connector/constants.jsx
[MODIFY] wecodesite/src/pages/ConnectPlatform/ConnectorEditor/index.jsx
[MODIFY] wecodesite/src/pages/ConnectPlatform/Flow/index.jsx
[MODIFY] wecodesite/src/pages/ConnectPlatform/Flow/constants.jsx
[MODIFY] wecodesite/src/pages/ConnectPlatform/FlowEditor/index.jsx
[MODIFY] wecodesite/src/pages/ConnectPlatform/FlowEditor/NodeLibrary.jsx
[MODIFY] wecodesite/src/pages/ConnectPlatform/FlowEditor/NodeProperties.jsx
[MODIFY] wecodesite/src/pages/ConnectPlatform/FlowEditor/constants.js
[MODIFY] wecodesite/src/pages/ConnectPlatform/FlowEditor/thunk.js
[MODIFY] wecodesite/src/components/CustomFlowNodes/TriggerNode.jsx
[MODIFY] wecodesite/src/components/CustomFlowNodes/ActionNode.jsx
[MODIFY] wecodesite/src/components/FlowCanvas/FlowCanvasWrapper.jsx
[MODIFY] wecodesite/src/components/SchemaEditor/SchemaEditor.jsx
[MODIFY] wecodesite/src/utils/flowUtils.js
[MODIFY] wecodesite/src/configs/web.config.js
```

**统计**：
- 新增后端文件：~30 个
- 修改后端文件：~25 个
- 新增前端文件：~14 个
- 修改前端文件：~15 个
- 新增数据库迁移：1 个
- 新增 ADR：3-4 个

---

## 7. 风险评估

| 风险 | 等级 | 影响 | 缓解措施 |
|------|:---:|------|---------|
| 1:N 版本模型迁移兼容 | 🔴 高 | V1 数据（1:1 版本）需迁移到 V2 模型（1:N），迁移脚本错误可能导致数据异常 | 1) 编写幂等迁移脚本：将现有单版本标记为 v1「已发布」<br>2) 保留 V1 列结构作为兼容层，逐步切换<br>3) 灰度发布：先在测试环境完整验证迁移流程 |
| 审批引擎集成复杂度 | 🟡 中 | ApprovalEngine 现有业务类型（api_register/event_register）与 connector_flow_version_publish 的审批节点结构可能有差异 | 1) 复用现有三级节点模型，仅新增 businessType 模板<br>2) 审批集成适配器封装差异逻辑<br>3) 提前与审批引擎 owner 对齐接口 |
| 并行分支引入的执行复杂度 | 🟡 中 | 并行分支的线程安全、超时控制、错误汇聚需要仔细设计 | 1) 使用 Reactor 响应式编程（已有 Spring WebFlux 基础设施）<br>2) 并行分支通过 `Flux.merge()` 实现<br>3) 每个分支独立超时，汇聚节点等待所有分支完成 |
| 版本快照数据量增长 | 🟡 中 | 1000 版本 × 100KB ≈ 100MB/实体，高频操作可能造成存储压力 | 1) 版本上限硬限制 1000 条<br>2) 物理删除真删除（非软删除），释放存储空间<br>3) 监控版本表数据量，超过阈值告警 |
| 缓存与版本切换一致性 | 🟢 低 | 版本变更时缓存未及时清理导致脏数据 | 1) 版本发布/失效/部署时主动清空缓存<br>2) 缓存 TTL 兜底（最大 5 分钟）<br>3) 缓存未命中正常执行，不中断流程 |
| 调试执行对运行时的性能影响 | 🟢 低 | 调试请求与正常请求共用运行时线程池 | 1) 调试使用独立线程池（小池，max 5 线程）<br>2) 调试超时独立配置（30s vs 正常运行 5s）<br>3) 调试执行不计入正常运行指标 |
| 设计态数据模型硬校验破坏已有配置 | 🔴 高 | FR-047 要求递归展开到基本类型，V1 已有配置可能有未展开的 object/array | 1) 迁移脚本扫描现有配置，标记不合规项<br>2) 新增配置强制校验，已有配置警告不阻塞<br>3) 提供批量修复工具辅助迁移 |

---

## 8. 数据迁移计划

### 8.1 V1 → V2 数据迁移

```sql
-- 步骤 1：添加新列（允许 NULL 过渡）
-- (DDL 见 §3.2)

-- 步骤 2：回填数据
-- 连接器：现有版本标记为 v1「已发布」
UPDATE openplatform_v2_cp_connector_version_t
SET version_number = 1,
    status = 2,  -- 已发布
    published_time = create_time,
    published_by = create_by
WHERE status IS NULL;

-- 连接器：根据是否已有版本设置状态
UPDATE openplatform_v2_cp_connector_t c
SET status = CASE
    WHEN EXISTS (SELECT 1 FROM openplatform_v2_cp_connector_version_t v
                 WHERE v.connector_id = c.id AND v.status = 2)
    THEN 2  -- 有效可用
    ELSE 1  -- 有效不可用
END
WHERE status IS NULL OR status = 0;

-- 连接流：现有版本标记为 v1「已发布」
UPDATE openplatform_v2_cp_flow_version_t
SET version_number = 1,
    status = 5,  -- 已发布
    published_time = create_time,
    published_by = create_by
WHERE status IS NULL;

-- 连接流：现有运行中的流设置 deployed_version_id
UPDATE openplatform_v2_cp_flow_t f
SET deployed_version_id = (
    SELECT id FROM openplatform_v2_cp_flow_version_t v
    WHERE v.flow_id = f.id AND v.status = 5
    LIMIT 1
),
lifecycle_status = CASE
    WHEN lifecycle_status = 1 THEN 2  -- 运行中
    WHEN lifecycle_status = 2 THEN 3  -- 已停止
    ELSE lifecycle_status
END
WHERE lifecycle_status IN (1, 2);

-- 步骤 3：设置 NOT NULL 约束
-- (在验证数据完整性后执行)
```

### 8.2 迁移执行顺序

1. 备份 V1 数据库
2. 在测试环境执行完整迁移 + 验证
3. 生产环境：先执行 DDL（ADD COLUMN NULL），应用部署后执行数据回填，最后加固约束

---

## 9. 架构决策记录 (ADR)

| 编号 | 标题 | 文件 |
|:---:|------|------|
| ADR-004 | 版本完整快照存储与递增整数版本号 | [ADR-004.md](./ADR-004.md) |
| ADR-005 | Redis 令牌桶入站限流方案 | [ADR-005.md](./ADR-005.md) |
| ADR-006 | MySQL 主存储运行记录与日志 | [ADR-006.md](./ADR-006.md) |
| ADR-007 | 多版本模型下的引用稽核策略 | [ADR-007.md](./ADR-007.md) |

---

## 10. 参考文档

- V1 规范文档：`../specs-tree-connector-platform/spec.md`
- V1 技术计划：`../specs-tree-connector-platform/plan-code.md`
- V1 ADR-001 ~ ADR-003：`../specs-tree-connector-platform/ADR-*.md`
- JSON Schema 设计规范（V2 沿用）：`./plan-json-schema.md`
- V1 DB 迁移脚本：`open-server/src/main/resources/db/migration/V2__init_connector_platform_schema.sql`

---

## ✅ 技术规划完成

**Feature**: connector-platform-v2  
**状态**: planned  
**文件**: `.sddu/specs-tree-root/specs-tree-connector-platform-v2/plan.md`

### 开放问题全部已决策

| OQ | 决策 |
|----|------|
| OQ-001 版本快照存储 | 完整快照（ADR-004） |
| OQ-002 版本号策略 | 实体内递增整数（ADR-004） |
| OQ-003 限流实现 | Redis 令牌桶 + Lua（ADR-005） |
| OQ-004 限流配置取值 | 使用已部署版本的 flowConfig |
| OQ-005 审批集成范围 | 复用 ApprovalEngine，新增 businessType 模板 |
| OQ-006 缓存一致性 | 版本变更主动清空 + TTL 兜底 |
| OQ-007 运行记录存储 | MySQL 主存储 + 定时清理（ADR-006） |
| OQ-008 复制版本历史 | 完整复制所有版本（所有状态） |

### 生成的 ADR
- ADR-004 — 版本完整快照存储与递增整数版本号
- ADR-005 — Redis 令牌桶入站限流方案
- ADR-006 — MySQL 主存储运行记录与日志
- ADR-007 — 多版本模型下的引用稽核策略

### 下一步
👉 运行 `@sddu-tasks connector-platform-v2` 开始任务分解
