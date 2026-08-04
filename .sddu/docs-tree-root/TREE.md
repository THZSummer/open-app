# Directory: .sddu/docs-tree-root/

## 目录简介
open-app 项目全景 — 业务架构视角：能力开放平台（基础设施）+ 数据开放平台（上层应用）+ 跨域技术视图

## 目录结构
```
docs-tree-root/
├── TREE.md                  # 本文件 - 目录导航
├── docs-overview.md         # 项目全景入口（业务架构图 + 能力地图 + 技术全景）
├── data.md                  # 全部 40 张表结构（字段/索引/关联）
├── api.md                   # 全部接口清单（184 端点，按业务能力组织）
├── openapi/                 # OpenAPI 契约（5 服务 YAML，186 端点）
├── adr-index.md             # ADR 索引
├── deploy.md                # 部署信息（拓扑/端口/环境变量）
├── security.md              # 全系统安全模型
├── relation-deps.md         # 服务依赖关系
├── relation-flow.md         # 跨域数据流
├── source.md                # 产物溯源（扫描信息源）
└── 能力开放平台/            # 业务域：能力开放平台（基础设施 · 阶段 1）
    ├── docs-overview.md     # 域入口（能力结构 + 工程映射）
    ├── 应用管理.md          # 基础能力：应用/成员/AKSK/版本
    ├── 权限中心.md          # 基础能力：权限资源创建与关联
    ├── 审批管理.md          # 基础能力：动态审批流引擎
    ├── 嵌入能力.md          # 基础能力：特有连接能力接入支撑
    ├── 数据字典.md          # 基础数据支撑
    ├── LookUp管理.md        # 基础数据支撑
    ├── API开放.md           # 公共连接能力 R1
    ├── 事件开放.md          # 公共连接能力 R2
    ├── 回调开放.md          # 公共连接能力 R3
    ├── 连接器开放.md        # 公共连接能力 R4（第四种开放形式）
    └── 数据开放平台.md      # 上层应用（阶段 2 · 搁置）
```

## 文件说明
| 文件 | 说明 | 状态 |
|------|------|------|
| docs-overview.md | 项目全景入口 — 业务架构图 + 能力地图 + 技术全景 | ✅ 存在 |
| data.md | 40 张表完整结构 | ✅ 存在 |
| api.md | 全部接口清单（184 端点） | ✅ 存在 |
| openapi/README.md | OpenAPI 规范索引 | ✅ 存在 |
| openapi/openapi-open-server.yaml | open-server 135 ops / 82 schemas | ✅ 存在 |
| openapi/openapi-market-server.yaml | market-server 28 ops / 20 schemas | ✅ 存在 |
| openapi/openapi-api-server.yaml | api-server 11 ops / 10 schemas | ✅ 存在 |
| openapi/openapi-event-server.yaml | event-server 10 ops / 5 schemas | ✅ 存在 |
| openapi/openapi-connector-api.yaml | connector-api 2 ops / 2 schemas | ✅ 存在 |
| adr-index.md | ADR 索引（能力开放平台 / 连接器平台） | ✅ 存在 |
| deploy.md | 部署信息（拓扑/端口/环境变量） | ✅ 存在 |
| security.md | 全系统安全模型 | ✅ 存在 |
| relation-deps.md | 服务依赖关系 | ✅ 存在 |
| relation-flow.md | 跨域数据流 | ✅ 存在 |
| source.md | 产物溯源 | ✅ 存在 |
| 能力开放平台/docs-overview.md | 能力开放平台域入口 | ✅ 存在 |
| 能力开放平台/应用管理.md | 基础能力：应用管理 | ✅ 存在 |
| 能力开放平台/权限中心.md | 基础能力：权限中心 | ✅ 存在 |
| 能力开放平台/审批管理.md | 基础能力：审批管理 | ✅ 存在 |
| 能力开放平台/嵌入能力.md | 基础能力：嵌入能力 | ✅ 存在 |
| 能力开放平台/数据字典.md | 基础数据支撑 | ✅ 存在 |
| 能力开放平台/LookUp管理.md | 基础数据支撑 | ✅ 存在 |
| 能力开放平台/API开放.md | 公共连接能力 R1 | ✅ 存在 |
| 能力开放平台/事件开放.md | 公共连接能力 R2 | ✅ 存在 |
| 能力开放平台/回调开放.md | 公共连接能力 R3 | ✅ 存在 |
| 能力开放平台/连接器开放.md | 公共连接能力 R4 | ✅ 存在 |
| 能力开放平台/数据开放平台.md | 上层应用（阶段 2 · 搁置） | ✅ 存在 |

## 上级目录
- [返回上级](../TREE.md)
- [返回首页](../../TREE.md)
