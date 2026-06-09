# 技术计划：连接器平台 V2 — 多版本与增强

**Feature ID**: CONN-PLAT-002  
**状态**: planned  
**创建日期**: 2026-06-09  
**依赖**: CONN-PLAT-001（V1 MVP — 已建成并验证）  
**关联规范**: [spec.md](./spec.md)

---

## 1. 架构概览

V2 在 V1（open-server 管理面 + connector-api 运行时）架构基础上叠加七个增强层：

```
版本层（多版本管理）→ 编排层（并行/数据处理/flowConfig）→ 运行时层（限流/缓存/日志）
安全层（URL白名单/SYSTOKEN白名单/应用白名单）→ 审批层（三级审批+催办）
调试层（直接触发）→ 运维层（操作日志+运行记录）
```

### 1.1 核心架构决策

| 决策 | 方案 | ADR |
|------|------|:--:|
| 版本快照存储 | 完整 JSON 快照（非增量） | ADR-004 |
| 版本号策略 | 实体内递增整数（非 SemVer） | ADR-004 |
| 入站限流 | Redis 令牌桶 + Lua 脚本 | ADR-005 |
| 运行记录/日志存储 | MySQL + 定时清理 30 天 | ADR-006 |
| 引用稽核 | 中间表 `connector_version_ref_t` + `flow.deployed_version_id` 指针 | ADR-007 |
| 审批集成 | 复用现有 ApprovalEngine，新增 businessType 模板 | plan.md §2.5 |
| 缓存一致性 | 版本变更主动清空 + TTL 兜底 | plan.md §2.6 |
| 连接流复制 | 完整复制所有版本历史 | plan.md §2.7 |

### 1.2 数据流核心变更

```
V1: 编辑即生效 → 运行时直接读当前版本配置 → 执行
V2: 草稿 → 发布(审批) → 部署(deployed_version_id) → 运行时按指针读版本快照 → 执行
```

---

## 2. 子文档索引

| 文档 | 内容 | 行数 |
|------|------|:--:|
| [plan-db.md](./plan-db.md) | **数据库设计** — 4 表 MODIFY + 4 表 NEW + 2 表 ENABLE，完整 DDL，状态枚举，V1→V2 迁移 SQL | ~250 |
| [plan-api.md](./plan-api.md) | **API 接口设计** — 45 个端点（连接器 13 + 连接流 19 + 审批 4 + 安全 3 + 运行时 5 + 调试 1），请求/响应体示例 | ~350 |
| [plan-page.md](./plan-page.md) | **前端页面设计** — 13 新增 + 8 修改页面，路由设计，版本历史/审批/调试/运行记录/白名单页面详设 | ~400 |
| [plan-runtime.md](./plan-runtime.md) | **运行时引擎设计** — 版本配置解析、并行分支执行、flowConfig 解析、数据处理节点、限流拦截器、缓存管理、日志采集、调试执行器、认证注入器扩展 | ~450 |
| [plan-json-schema.md](./plan-json-schema.md) | **JSON Schema 设计规范**（V1 沿用 + V2 增量） — 认证多选/flowConfig/dataProcessor/FR-047 类型严格校验/并行边定义 | ~2900 |
| [plan-code.md](./plan-code.md) | **代码规范**（复用 V1） — 16 条规范（注释中文、日志英文、SQL 禁 SELECT * 等），V2 遵循 | 841 |
| [plan-cache.md](./plan-cache.md) | **缓存与限流策略**（复用 V1 + V2 增量） — Redis 配置缓存、入站限流令牌桶、版本切换失效 | 575 |

---

## 3. 文件影响统计

| 类别 | 新增 | 修改 | 说明 |
|------|:--:|:--:|------|
| open-server 后端 | ~15 | ~15 | 版本管理服务、审批集成、白名单、调试代理、操作日志扩展 |
| connector-api 运行时 | ~15 | ~8 | 版本解析、并行执行、限流、缓存、日志、调试、认证注入器 |
| 前端 wecodesite | ~14 | ~15 | 版本历史面板、审批面板、调试面板、运行记录、白名单管理、编排增强 |
| 数据库迁移 | 1 | — | `V3__connector_platform_v2_schema.sql` |
| ADR | 4 | — | ADR-004 ~ ADR-007 |

---

## 4. 风险评估

| 风险 | 等级 | 缓解措施 |
|------|:---:|---------|
| 1:N 版本模型迁移兼容 | 🔴 高 | 幂等迁移脚本；V1 数据标记为 v1「已发布」；灰度验证 |
| 审批引擎集成复杂度 | 🟡 中 | 复用现有三级节点模型；适配器封装差异；提前对齐接口 |
| 并行分支引入的执行复杂度 | 🟡 中 | Reactor `Flux.merge()`；每分支独立超时+错误不扩散 |
| 版本快照数据量增长 | 🟡 中 | 1000 版本硬上限；物理删除真删除；监控告警 |
| 设计态硬校验破坏已有配置 | 🔴 高 | 迁移脚本扫描标记不合规；新增强制校验已有警告不阻塞；批量修复工具 |
| FR-047 数据模型 | 🟡 中 | 新增配置强制校验（object 必须展开子字段）；已有配置标记警告 |
| 缓存与版本切换一致性 | 🟢 低 | 版本变更主动清空 + TTL 兜底 |

---

## 5. 数据迁移计划

1. **备份** V1 数据库
2. **测试环境**执行完整迁移 + 验证
3. **生产环境**：DDL（ADD COLUMN NULL）→ 应用部署 → 数据回填 → 加固约束

迁移脚本详见 [plan-db.md §5](./plan-db.md#5-v1v2-数据迁移-sql)。

---

## 6. 架构决策记录 (ADR)

| 编号 | 标题 | 文件 |
|:---:|------|------|
| ADR-004 | 版本完整快照存储与递增整数版本号 | [ADR-004.md](./ADR-004.md) |
| ADR-005 | Redis 令牌桶入站限流方案 | [ADR-005.md](./ADR-005.md) |
| ADR-006 | MySQL 主存储运行记录与日志 | [ADR-006.md](./ADR-006.md) |
| ADR-007 | 多版本模型下的引用稽核策略 | [ADR-007.md](./ADR-007.md) |

---

## 7. 参考文档

- V1 规范：`../specs-tree-connector-platform/spec.md`
- V1 技术计划：`../specs-tree-connector-platform/plan.md`
- V1 ADR-001 ~ ADR-003：`../specs-tree-connector-platform/ADR-*.md`
- V1 DB 迁移脚本：`open-server/src/main/resources/db/migration/V2__init_connector_platform_schema.sql`

---

## ✅ 技术规划完成

**状态**: specified → **planned**  
**8 个 OQ**: 全部决策完成  
**4 个 ADR**: ADR-004 ~ ADR-007  
**7 个子文档**: plan-db / plan-api / plan-page / plan-runtime / plan-json-schema / plan-code(引用) / plan-cache(引用)

### 下一步

👉 运行 `@sddu-tasks connector-platform-v2` 开始任务分解
