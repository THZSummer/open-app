# Directory: .sddu/docs-tree-root/

## 目录简介
docs-tree-root 目录

## 目录结构
```
docs-tree-root/
├── TREE.md          # 本文件 - 目录导航
├── adr-index.md          # open-app — ADR 索引
├── deploy.md          # open-app 部署信息 — 部署信息
├── docs-overview.md          # open-app 项目全景 — 全景入口
├── relation-deps.md          # open-app — 依赖关系
├── relation-flow.md          # open-app — 数据流
├── security.md          # open-app 安全模型 — 安全策略文档
├── source.md          # open-app 全景 — 产物溯源
├── api-server/          # 子目录
├── connector-api/          # 子目录
├── database/          # 子目录
├── event-server/          # 子目录
├── frontend/          # 子目录
├── market-server/          # 子目录
└── open-server/          # 子目录
```

## 文件说明
| 文件 | 说明 | 状态 |
|------|------|------|
| adr-index.md | open-app — ADR 索引 — open-app — ADR 索引 | ✅ 存在 |
| deploy.md | open-app 部署信息 — 部署信息 — ┌──────────────────────────┐ | ✅ 存在 |
| docs-overview.md | open-app 项目全景 — 全景入口 — ┌───────────────────────────── 前端层 ─────────────────────────────┐ | ✅ 存在 |
| relation-deps.md | open-app — 依赖关系 — wecodesite ──► open-server ──► MySQL | ✅ 存在 |
| relation-flow.md | open-app — 数据流 — 能力提供方 ──► open-server POST /apis ──► v2_api_t（草稿） | ✅ 存在 |
| security.md | open-app 安全模型 — 安全策略文档 — API/事件/回调资源: 草稿(0) → 待审(1) → 已发布(2) → 已下线(3) | ✅ 存在 |
| source.md | open-app 全景 — 产物溯源 — open-app 全景 — 产物溯源 | ✅ 存在 |

## 上级目录
- [返回上级](../TREE.md)
- [返回首页](../../TREE.md)
