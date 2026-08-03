# market-server — 全景入口

> **文档定位**: sddu-docs-overview — 本级全景入口  
> **输出文件名**: docs-overview.md  
> **数据来源**: 代码扫描生成 — market-server/src/main/java + application*.yml  
> **创建时间**: 2026-08-03  
> **版本**: v1.0 (CODE-SCAN)  
> **生成方式**: 全量生成

---

## 1. 业务全景

### 1.1 自身概述

| 属性 | 值 |
|------|-----|
| **类型** | Spring Boot Web 服务（市场/管理面） |
| **职责描述** | 市场管理面：数据字典、LookUp 管理、能力管理（admin）、应用审批、文件上传、聊天机器人绑定 |
| **所属业务域** | 基础配置 / 市场管理 |
| **版本** | 1.0.0-SNAPSHOT (Spring Boot 3.4.6) |

### 1.2 子组件

| 组件 | 类型 | 描述 | 关系说明 |
|------|------|------|---------|
| **dictionary 模块** | 模块 | 数据字典 CRUD | 基础配置 |
| **lookup 模块** | 模块 | LookUp 分类 + 项 CRUD | 基础配置 |
| **ability 模块** | 模块 | 能力管理（admin 列表/增/改/删） | 依赖 open-server 能力 |
| **approval 模块** | 模块 | 应用审批（pending/publish/approval） | 依赖 open-server |
| **file 模块** | 模块 | 文件上传 | 基础 |
| **chatbotbindtab 模块** | 模块 | 聊天机器人绑定账号 | 独立 |
| **common 模块** | 模块 | 健康检查 | 基础 |

### 1.3 子组件分类

| 分类 | 包含组件 |
|------|---------|
| **基础配置** | dictionary、lookup、file |
| **能力管理** | ability、approval |
| **业务扩展** | chatbotbindtab、common |

---

## 2. 技术全景

### 2.1 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 3.4.6 | Web 框架 |
| MyBatis | mapper XML | 数据访问 |
| MySQL | openapp | 数据库 |
| Redis Cluster | 6 节点 | 缓存 |
| SpringDoc | swagger-ui | API 文档 |
| 静态资源 | file:${java.io.tmpdir}/ | 文件存储（dev 本地） |

### 2.2 架构决策记录（ADR）

| 编号 | 标题 | 状态 | 影响范围 |
|:--:|------|:--:|---------|
| — | 本级由代码扫描生成，无独立 ADR | — | — |

### 2.3 外部依赖

| 依赖项 | 类型 | 说明 |
|--------|------|------|
| platform.approval-url-prefix | 配置 | 审批跳转地址 |
| wecontact.api-url | 外部服务 | 通讯录 API（企业微信/WeLink 通讯录） |
| wecontact.tenant-id | 配置 | x-welink-tenantid |
| wecontact.token | 配置 | 动态获取 Authorization |

---

## 修订记录

| 生成时间 | 变更 Feature | 生成方式 | 修订人 |
|---------|-------------|:--:|--------|
| 2026-08-03 | 代码扫描全量生成 | code-scan | SDDU Docs Agent |
