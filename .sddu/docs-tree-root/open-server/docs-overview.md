# open-server — 全景入口

> **文档定位**: sddu-docs-overview — 本级全景入口  
> **输出文件名**: docs-overview.md  
> **数据来源**: 代码扫描生成 — open-server/src/main/java + application*.yml  
> **创建时间**: 2026-08-03  
> **版本**: v1.0 (CODE-SCAN)  
> **生成方式**: 全量生成

---

## 1. 业务全景

### 1.1 自身概述

| 属性 | 值 |
|------|-----|
| **类型** | Spring Boot Web 服务（管理面） |
| **职责描述** | 能力开放平台管理面核心：应用生命周期、能力管理、API/事件/回调资源注册、权限与订阅、审批流、连接器/连接流管理、成员管理、操作审计、同步 |
| **所属业务域** | 能力开放平台 |
| **版本** | 1.0.0-SNAPSHOT (Spring Boot 3.5.14) |

### 1.2 子组件（模块）

| 组件 | 类型 | 描述 | 关系说明 |
|------|------|------|---------|
| **app 模块** | 模块 | 应用 CRUD、EAMAP 绑定、身份/密钥、图标、角色查询 | 核心 |
| **ability 模块** | 模块 | 能力列表、能力订阅查询 | 依赖 app |
| **api 模块** | 模块 | API 资源注册/编辑/下线 | 依赖 category |
| **event 模块** | 模块 | 事件资源注册/编辑/下线 | 依赖 category |
| **callback 模块** | 模块 | 回调资源注册/编辑/下线 | 依赖 category |
| **category 模块** | 模块 | 分类管理（含责任人） | 基础 |
| **permission 模块** | 模块 | API/事件/回调订阅、撤回、配置 | 依赖 api/event/callback |
| **approval 模块** | 模块 | 审批待办、同意/拒绝/撤销/批量/催办 | 依赖 approvalflow |
| **approvalflow 模块** | 模块 | 审批流程模板 CRUD | 基础 |
| **connector 模块** | 模块 | 连接器 CRUD、失效/恢复 | 独立 |
| **connectorversion 模块** | 模块 | 连接器版本管理（草稿/发布/失效/恢复） | 依赖 connector |
| **flow 模块** | 模块 | 连接流 CRUD、复制/部署/启停 | 独立 |
| **flowversion 模块** | 模块 | 连接流版本管理（含审批协同/催办） | 依赖 flow |
| **flowexecrecord 模块** | 模块 | 执行记录查询 | 依赖 connector-api |
| **member 模块** | 模块 | 应用成员管理、owner 转移、用户搜索 | 依赖 app |
| **version 模块** | 模块 | 应用版本管理（发布/撤回） | 依赖 app |
| **auditlog 模块** | 模块 | 操作日志查询 | 全局 |
| **sync 模块** | 模块 | 订阅数据迁移/回滚/紧急修复 | 全局 |
| **common/file** | 模块 | 文件上传（upload-image / upload）、LookUp 白名单 | 全局 |
| **card 模块** | 模块 | 卡片设置 | 依赖 app |

### 1.3 子组件分类

| 分类 | 包含组件 |
|------|---------|
| **应用管理** | app、member、version、eamap、identity |
| **资源注册** | api、event、callback、category、ability |
| **订阅权限** | permission、subscription |
| **审批** | approval、approvalflow |
| **连接器平台** | connector、connectorversion、flow、flowversion、flowexecrecord |
| **基础设施** | auditlog、sync、common/file、card |

---

## 2. 技术全景

### 2.1 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 3.5.14 | Web 框架 |
| MyBatis | mapper XML | 数据访问 |
| MySQL | openapp | 数据库 |
| Redis Cluster | 6 节点 | 缓存 |
| SpringDoc | swagger-ui | API 文档 |

### 2.2 架构决策记录（ADR）

| 编号 | 标题 | 状态 | 影响范围 |
|:--:|------|:--:|---------|
| — | 本级由代码扫描生成，无独立 ADR | — | — |

### 2.3 技术依赖总览

| 依赖项 | 类型 | 版本约束 | 说明 |
|--------|------|---------|------|
| spring-boot-starter-web | starter | 3.5.14 | Servlet Web |
| mybatis | 数据访问 | mapper XML | MyBatis SQL |
| spring-boot-starter-data-redis | starter | — | Lettuce 集群 |
| springdoc-openapi-starter-webmvc-ui | starter | — | Swagger UI |

### 2.4 本域文档

| 文档 | 说明 |
|------|------|
| `api.md` | open-server API 端点清单（~80 端点） |
| `config.md` | open-server 配置项（端口/数据源/Redis/profile） |
| `security.md` | open-server 安全模型（内部凭证/审批/成员权限） |

---

## 修订记录

| 生成时间 | 变更 Feature | 生成方式 | 修订人 |
|---------|-------------|:--:|--------|
| 2026-08-03 | 代码扫描全量生成 | code-scan | SDDU Docs Agent |
