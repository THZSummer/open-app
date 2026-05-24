# SDDU 工作空间

## 目录结构

```
.sddu/
├── README.md              # 本文件 - SDDU 工作空间说明
├── state.json             # 工作空间状态
└── specs-tree-root/       # 规范文件根目录
    ├── README.md          # 目录导航
    ├── specs-tree-capability-open-platform/  # 能力开放平台（已完成全流程）
    ├── specs-tree-connector-platform/        # 连接器平台（任务分解完成）
    └── specs-tree-data-open-platform/        # 数据开放平台（规范编写完成）
```

## Feature 概览

| Feature | 目录 | 状态 | 优先级 |
|---------|------|------|--------|
| 能力开放平台 | [specs-tree-capability-open-platform/](specs-tree-root/specs-tree-capability-open-platform/) | ✅ validated（全流程完成） | P0 |
| 连接器平台 | [specs-tree-connector-platform/](specs-tree-root/specs-tree-connector-platform/) | 🔵 tasked（任务分解完成） | P1 |
| 数据开放平台 | [specs-tree-data-open-platform/](specs-tree-root/specs-tree-data-open-platform/) | ✅ specified（规范完成） | P0 |

## 快速开始

1. 使用 `@sddu 开始 [feature 名称]` 开始新 feature
2. 规范文件将自动创建在 `.sddu/specs-tree-root/` 目录
3. 文档会自动维护（`@sddu-docs`）

## Agents

- `@sddu` - 智能入口
- `@sddu-docs` - 目录导航（自动触发）
- `@sddu-roadmap` - Roadmap 规划
