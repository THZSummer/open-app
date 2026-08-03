# api-server — 全景入口

> **文档定位**: sddu-docs-overview — 本级全景入口  
> **输出文件名**: docs-overview.md  
> **数据来源**: 代码扫描生成 — api-server/src/main/java + application*.yml  
> **创建时间**: 2026-08-03  
> **版本**: v1.0 (CODE-SCAN)  
> **生成方式**: 全量生成

---

## 1. 业务全景

### 1.1 自身概述

| 属性 | 值 |
|------|-----|
| **类型** | Spring Boot Web 服务（消费网关） |
| **职责描述** | 数据开放平台查询网关 + API 消费网关 + 用户授权（OAuth 模型）+ 审批回调 + 内部角色同步 |
| **所属业务域** | 数据开放平台 / 消费网关 |
| **版本** | 1.0.0-SNAPSHOT (Spring Boot 3.4.6) |

### 1.2 子组件

| 组件 | 类型 | 描述 | 关系说明 |
|------|------|------|---------|
| **data 模块** | 模块 | 数据查询网关（permissions check / subscribers / config / detail） | 核心 |
| **gateway 模块** | 模块 | API 消费网关（/gateway/api/** 代理）+ 助手回调配置 | 核心 |
| **scope 模块** | 模块 | 用户授权管理（user-authorizations CRUD） | 依赖 data |
| **approval 模块** | 模块 | 审批回调接收 | 依赖 open-server |
| **appmember 模块** | 模块 | 内部角色同步 | 依赖 open-server |
| **common 模块** | 模块 | 健康检查 | 基础 |

### 1.3 子组件分类

| 分类 | 包含组件 |
|------|---------|
| **消费网关** | data、gateway |
| **授权与回调** | scope、approval |
| **内部服务** | appmember、common |

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

### 2.2 架构决策记录（ADR）

| 编号 | 标题 | 状态 | 影响范围 |
|:--:|------|:--:|---------|
| — | 本级由代码扫描生成，无独立 ADR | — | — |

### 2.3 技术依赖总览

| 依赖项 | 类型 | 版本约束 | 说明 |
|--------|------|---------|------|
| spring-boot-starter-web | starter | 3.4.6 | Servlet Web |
| mybatis | 数据访问 | — | MyBatis |
| spring-boot-starter-data-redis | starter | — | Redis |
| internal.auth | 配置 | bypass=false | 内部凭证校验（dev/prod 白名单） |

---

## 修订记录

| 生成时间 | 变更 Feature | 生成方式 | 修订人 |
|---------|-------------|:--:|--------|
| 2026-08-03 | 代码扫描全量生成 | code-scan | SDDU Docs Agent |
