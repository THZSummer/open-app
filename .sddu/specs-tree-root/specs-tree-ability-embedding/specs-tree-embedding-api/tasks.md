# 任务分解：嵌入能力API面

> **文档定位**: SDDU 任务清单 — 按接口维度拆分为 1 个后端 Task，作为 build 阶段的入口
> **前置依赖**: plan.md（技术方案）、spec.md（需求规范）
> **创建人**: SDDU 路由调度专家
> **创建时间**: 2026-07-20
> **版本**: v2.0

---

## 1. 拆分原则

API 面仅 1 个接口（POST /internal/user/roles），按接口维度聚合为 1 个 Task：

| 原则 | 说明 |
|------|------|
| 接口聚合 | 1 个 API 端点 = 1 个 Task，DTO/解析器/鉴权/Service/Controller/配置全部聚合 |
| 单分支开发 | 无多 Task 并行需求，单分支内自底向上开发 |

---

## 2. 依赖拓扑

```
单 Task，无跨 Task 依赖。

推荐开发顺序（单分支内）: DTO → 配置 → 鉴权 → 解析器 → Service → Controller
```

---

## 3. 任务索引

| # | 任务 | 子Feature 目录 | 接口 | FR | 复杂度 | 服务 |
|---|------|--------------|------|:--:|:--:|------|
| 1 | 用户角色查询 | `specs-tree-api-01-user-roles/` | `POST /internal/user/roles` | FR-001 | M | api-server |

> 详细定义见 [specs-tree-api-01-user-roles/tasks.md](./specs-tree-api-01-user-roles/tasks.md)。

---

## 4. 执行指南

**启动命令**：`@sddu-build TASK-001`

**完成标准**：13 个源码文件 + 3 个测试文件，Maven 编译通过，Java 单测 + Python 集成测试全部通过。

---

*最后更新: 2026-07-20*
