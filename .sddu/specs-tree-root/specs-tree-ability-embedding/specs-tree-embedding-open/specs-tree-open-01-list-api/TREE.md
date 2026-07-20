# 能力列表增强 — 目录导航

**Feature ID**: EMBED-OPEN-001
**父 Feature**: EMBED-OPEN-001（嵌入能力开放面）
**阶段**: tasked | 复杂度: M | 接口: GET /ability/list

## 对应任务

- **任务ID**: TASK-001
- **关联FR**: FR-001, FR-005

## 目录结构

```
specs-tree-open-01-list-api/
├── state.json        # 任务状态
├── tasks.md          # 任务详情
├── tasks.json        # 任务配置
└── TREE.md           # 本文件 — 目录导航
```

## 职责

修改 `AbilityServiceImpl.getAbilityList()`：hidden=1 过滤替换硬编码 type=6 排除 → AbilityVO 新增 5 字段映射 → 自定义类型正常返回。

---

*最后更新: 2026-07-20*
