# 能力开放平台 — 全景入口

> **文档定位**: sddu-docs-overview — 本级全景入口
> **输出文件名**: docs-overview.md
> **数据来源**: 业务架构聚合（specs-tree-root 设计文档）+ 代码扫描设计真相
> **创建时间**: 2026-08-03
> **版本**: v2.0 (BIZ-ARCH)
> **更新说明**: 业务视角重建 — 能力文档平铺

---

## 1. 业务全景

### 1.1 自身概述

| 属性 | 值 |
|------|-----|
| **类型** | open-app 基础设施（阶段 1） |
| **职责描述** | 统一开放底座：平台本身能力（应用/成员/AKSK/权限/审批/嵌入）+ 连接能力（API/事件/回调/连接器），支撑企业内三方平台消费 XXX 通讯系统能力 |
| **所属业务域** | open-app 能力开放平台 |
| **对应 Feature** | CAP-OPEN-001（能力开放平台）、CONN-PLAT-001/003（连接器平台 V1/V3）、APP-MGMT-001（应用管理）、EMBED-001（嵌入能力）、FR-DICTIONARY-001（数据字典）、FR-LOOKUP-001（LookUp） |

### 1.2 能力结构

> 来源：能力开放平台 discovery-report §3.2 能力分类模型（平台本身能力 + 连接能力 两层）。

```
能力开放平台（基础设施 · 阶段 1）
├── 平台本身能力（Platform Capabilities）── 公共底座，被连接能力依赖复用
│   ├── 基础能力：应用管理 / 成员管理 / AKSK 管理 / 权限管理 / 审批管理 / 嵌入能力
│   └── 辅助能力：操作日志 / 应用版本 / 文档管理
└── 连接能力（Connection Capabilities）── 依赖平台本身能力
    ├── 公共连接能力：API 开放 / 事件开放 / 回调开放 / 连接器开放
    └── 特有连接能力：IM 卡片 / 云盘 / 邮件（业务模块构建，经嵌入能力接入）
```

### 1.3 能力文档索引

| 能力 | 类型 | 对应 Feature | 状态 | 核心职责 |
|------|------|-------------|------|---------|
| [应用管理](应用管理.md) | 基础能力 | APP-MGMT-001 + CAP-OPEN-001 | ✅ validated | 应用注册/审核/凭证/状态、成员管理、AKSK |
| [权限中心](权限中心.md) | 基础能力 | CAP-OPEN-001 | ✅ validated | 权限资源创建与关联、订阅关系、Scope 授权 |
| [审批管理](审批管理.md) | 基础能力 | CAP-OPEN-001 | ✅ validated | 动态审批流配置引擎（API/事件/回调/权限申请审批） |
| [嵌入能力](嵌入能力.md) | 基础能力 | EMBED-001 | 🟡 planned（58%） | 特有连接能力接入开放平台的基础支撑（平台面/开放面/API面） |
| [数据字典](数据字典.md) | 基础数据支撑 | FR-DICTIONARY-001 | ✅ planned | 枚举/配置集中维护 |
| [LookUp管理](LookUp管理.md) | 基础数据支撑 | FR-LOOKUP-001 | ✅ planned | 标准化的枚举值和配置项集中维护 |
| [API开放](API开放.md) | 公共连接能力 R1 | CAP-OPEN-001 | ✅ validated | API 注册/订阅/消费网关 |
| [事件开放](事件开放.md) | 公共连接能力 R2 | CAP-OPEN-001 | ✅ validated | 事件注册/订阅/发布/SSE/WebSocket |
| [回调开放](回调开放.md) | 公共连接能力 R3 | CAP-OPEN-001 | ✅ validated | 回调注册/订阅/触发/WebHook |
| [连接器开放](连接器开放.md) | 公共连接能力 R4 | CONN-PLAT-001/003 | ✅ validated | 第四种开放形式：连接器/连接流编排与执行引擎 |

### 1.4 特有连接能力（业务模块构建）

| 特有能力 | 说明 | 建设策略 | 接入方式 |
|---------|------|---------|---------|
| IM 卡片能力 | IM 模块特有业务能力 | 🟡 沿用现有 | 经嵌入能力接入 |
| 云盘特有业务能力 | 云盘模块特有业务能力 | 🔵 业务模块建设 | 经嵌入能力接入 |
| 邮件特有业务能力 | 邮件模块特有业务能力 | 🔵 业务模块建设 | 经嵌入能力接入 |

---

## 2. 技术全景

### 2.1 工程实现映射

| 业务能力 | 实现服务 | 主要表 |
|---------|---------|-------|
| 应用管理 | open-server + wecodesite + market-server（审批） | app_t, app_identity_t, app_member_t, app_version_t, app_ability_relation_t, eamap_t |
| 权限中心 | open-server + api-server | v2_permission_t, v2_permission_p_t, v2_subscription_t, v2_user_authorization_t |
| 审批管理 | open-server + market-server | v2_approval_flow_t, v2_approval_record_t, v2_approval_log_t |
| 嵌入能力 | market-server + open-server + api-server + qiankunProject | ability_t, ability_p_t, app_ability_relation_t |
| 数据字典 | market-server + market-web | property_t（openplatform_property_t） |
| LookUp 管理 | market-server + market-web | lookup_classify_t, lookup_item_t, lookup_file_t |
| API 开放 | open-server（管理）+ api-server（网关） | v2_api_t, v2_api_p_t, v2_category_t |
| 事件开放 | open-server（管理）+ event-server（网关） | v2_event_t, v2_event_p_t |
| 回调开放 | open-server（管理）+ event-server（网关） | v2_callback_t, v2_callback_p_t |
| 连接器开放 | open-server（管理）+ connector-api（执行） | v2_cp_connector_t, v2_cp_flow_t, v2_cp_execution_record_t |

> 📊 **Archify 交互图**：[服务调用依赖关系](../archify/service-deps.html)（可交互）· [PNG](../archify/service-deps.png)

### 2.2 服务依赖

| 依赖 | 方向 | 说明 |
|------|------|------|
| open-server → MySQL/Redis | 数据访问 | 管理面 CRUD |
| open-server → api-server | 订阅同步 | 订阅关系下发至消费网关 |
| open-server → event-server | 订阅同步 | 订阅关系下发至事件/回调网关 |
| api-server → MySQL | 数据查询 | 数据开放网关代理 |
| event-server → Redis | 订阅列表缓存 | 发布/回调按订阅分发 |
| connector-api → MySQL (R2DBC) | 执行记录 | 响应式数据访问 |
| connector-api → GraalJS | 脚本执行 | 脚本节点沙箱 |

---

## 修订记录

| 版本 | 变更说明 | 日期 | 修订人 |
|------|---------|------|--------|
| v2.0 | 业务视角重建：能力文档平铺 | 2026-08-03 | SDDU Docs Agent |
