# 狭义嵌入能力 — 目录导航

**Feature ID**: EMBED-001
**状态**: specifying（规范编写中）

## 目录结构

```
specs-tree-ability-embedding/
├── TREE.md                        # 本文件 — 目录导航
├── discovery-report.md            # 问题挖掘报告（已完成 ✅）
├── state.json                     # 状态文件
├── spec.md                        # 父Feature轻量规范（概述）
│
├── specs-tree-embedding-platform/ # 子Feature：嵌入能力平台面
│   ├── spec.md                    # 平台面需求规范
│   └── state.json                 # 子Feature状态
│
├── specs-tree-embedding-open/     # 子Feature：嵌入能力开放面
│   ├── spec.md                    # 开放面需求规范
│   └── state.json                 # 子Feature状态
│
└── specs-tree-embedding-api/      # 子Feature：嵌入能力API面
    ├── spec.md                    # API面需求规范
    └── state.json                 # 子Feature状态
```

## 子Feature一览

| 子Feature | 目录 | 职责 |
|-----------|------|------|
| **嵌入能力平台面** | `specs-tree-embedding-platform/` | market-server + market-web：ability 类型 CRUD，能力目录管理后台 |
| **嵌入能力开放面** | `specs-tree-embedding-open/` | open-server + wecodesite：ability 查询/订阅、QianKun 前端嵌入 |
| **嵌入能力API面** | `specs-tree-embedding-api/` | api-server：应用认证、成员查询、权限校验接口，供嵌入能力方集成 |

## 依赖关系

- **前置依赖**: 能力开放平台 CAP-OPEN-001（权限模型、审批底座）
- **平行依赖**: 无
- **下游**: 各子Feature独立进入 plan → tasks → build 阶段

---

*最后更新: 2026-07-13*
