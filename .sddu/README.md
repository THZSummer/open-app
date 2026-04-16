# SDD Workspace

## 目录结构

```
.sddu/
├── README.md              # 本文件 - SDD 工作空间说明
├── ROADMAP.md             # 版本路线图
├── config.json            # SDD 配置（可选）
└── specs-tree-root/       # 规范文件目录
    ├── README.md          # 目录说明
    ├── specs-tree-capability-open-platform/  # 能力开放平台
    │   ├── README.md      # 目录导航
    │   ├── discovery-report.md
    │   ├── discovery-analysis.md
    │   ├── discovery-session-log.md
    │   └── state.json
    └── specs-tree-data-open-platform/  # 数据开放平台
        ├── README.md      # 目录导航
        ├── discovery-report.md
        ├── discovery-analysis.md
        ├── discovery-session-log.md
        ├── spec.md
        └── state.json
```

## 快速开始

1. 使用 `@sdd 开始 [feature 名称]` 开始新 feature
2. 规范文件将自动创建在 `.sddu/specs-tree-root/` 目录
3. 文档会自动维护，无需手动创建 README

## Agents

- `@sdd` - 智能入口
- `@sdd-docs` - 目录导航（自动触发）
- `@sdd-roadmap` - Roadmap 规划
