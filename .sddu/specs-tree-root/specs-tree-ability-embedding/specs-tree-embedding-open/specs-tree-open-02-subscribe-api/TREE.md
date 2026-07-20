# 能力订阅增强 + 自动桥接 — 目录导航

**Feature ID**: EMBED-OPEN-001
**父 Feature**: EMBED-OPEN-001（嵌入能力开放面）
**阶段**: tasked | 复杂度: M | 接口: POST /ability

## 对应任务

- **任务ID**: TASK-002
- **关联FR**: FR-002, FR-003

## 目录结构

```
specs-tree-open-02-subscribe-api/
├── state.json        # 任务状态
├── tasks.md          # 任务详情
├── tasks.json        # 任务配置
└── TREE.md           # 本文件 — 目录导航
```

## 职责

修改 `AbilityServiceImpl.addAbility()`：枚举校验 `AbilityTypeEnum.isValidCode()` → DB 查询校验；修改 `autoSubscribeAfterAbility()`：空实现 → 日志记录 + 预留钩子。

---

*最后更新: 2026-07-20*
