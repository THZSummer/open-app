# 用户角色查询 — 目录导航

**Feature ID**: EMBED-API-001
**父 Feature**: EMBED-API-001（嵌入能力API面）
**阶段**: tasked | 复杂度: M | 接口: POST /internal/user/roles

## 对应任务

- **任务ID**: TASK-001
- **关联FR**: FR-001

## 目录结构

```
specs-tree-api-01-user-roles/
├── state.json        # 任务状态
├── tasks.md          # 任务详情
├── tasks.json        # 任务配置
└── TREE.md           # 本文件 — 目录导航
```

## 职责

新增 `POST /service/open/v2/internal/user/roles` 接口：DTO 定义 → 应用标识解析(appId/hisAppId) → 内部凭证鉴权 → UserRoleService(Mock/Real) → Controller 响应。

---

*最后更新: 2026-07-20*
