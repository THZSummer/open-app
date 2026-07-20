# 已订阅列表增强 — 目录导航

**Feature ID**: EMBED-OPEN-001
**父 Feature**: EMBED-OPEN-001（嵌入能力开放面）
**阶段**: tasked | 复杂度: S | 接口: GET /ability/subscribed

## 对应任务

- **任务ID**: TASK-003
- **关联FR**: FR-004

## 目录结构

```
specs-tree-open-03-subscribed-api/
├── state.json        # 任务状态
├── tasks.md          # 任务详情
├── tasks.json        # 任务配置
└── TREE.md           # 本文件 — 目录导航
```

## 职责

修改 `AbilityServiceImpl.getSubscribedAbilities()`：AppAbilityDetailVO 新增 5 字段映射 + 移除硬编码排除 type=6。

---

*最后更新: 2026-07-20*
