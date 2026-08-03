# 嵌入能力域 — 全景入口

> **文档定位**: sddu-docs-overview — 本级全景入口  
> **输出文件名**: docs-overview.md  
> **数据来源**: 聚合自 specs-tree-root/specs-tree-ability-embedding（含 3 个子 Feature：embedding-platform / embedding-open / embedding-api）  
> **创建时间**: 2026-08-03  
> **版本**: v1.0  
> **生成方式**: 全量构建

---

## 1. 业务全景

### 1.1 自身概述

| 属性 | 值 |
|------|-----|
| **类型** | 业务域（Domain） |
| **职责描述** | 三层狭义嵌入能力体系——让 XX 通讯平台内部业务模块（IM、云盘、邮件等）的特有连接能力（群置顶、群通知、链接增强等）通过标准化机制注册、订阅并以 QianKun 微前端方式嵌入 wecodesite |
| **所属业务域** | open-app 开放平台 |
| **版本** | v1.0（子 Feature 分阶段推进） |

### 1.2 子组件

| 组件 | 类型 | 描述 | 关系说明 |
|------|------|------|---------|
| **嵌入能力平台面** | 子 Feature | market-server + market-web — ability 类型 CRUD 能力目录管理后台（录入端） | ✅ validated（11 TASK 全完成）；能力目录数据供开放面消费 |
| **嵌入能力开放面** | 子 Feature | open-server + wecodesite — ability 查询/订阅增强 + QianKun 微前端嵌入（消费端） | 🔄 builded（4 TASK 待 build）；依赖平台面目录数据 |
| **嵌入能力API面** | 子 Feature | api-server — 应用认证、成员查询、权限校验接口（校验端） | 📋 tasked（待 build）；为嵌入能力方提供标准校验接口 |

### 1.3 子组件分类

| 分类 | 包含组件 |
|------|---------|
| 录入端 | 嵌入能力平台面 |
| 消费端 | 嵌入能力开放面 |
| 校验端 | 嵌入能力API面 |

---

## 2. 技术全景

### 2.1 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Java | 21 | 后端语言 |
| Spring Boot | — | open-server / market-server / api-server |
| MyBatis | — | ORM |
| MySQL | — | ability 数据持久化 |
| Redis | — | 缓存 |
| React | — | 前端（wecodesite / market-web） |
| QianKun | — | 微前端嵌入（运行时 loadMicroApp / 路由跳转 loadType 分支） |

### 2.2 架构决策记录（ADR）

| 编号 | 标题 | 状态 | 影响范围 |
|:--:|------|:--:|---------|
| ADR-001 | Admin 能力 CRUD 放在 market-server 扩展（替代原 open-server 方案） | ACCEPTED | 平台面 |
| ADR-002 | abilityType 编码规则 | ACCEPTED | 平台面 |
| ADR-004 | require_release 字段替代硬编码能力类型排除 | ACCEPTED | 平台面 |
| ADR-001 | 增量修改现有 ability 模块而非新建模块 | ACCEPTED | 开放面 |
| ADR-002 | 配置页动态子应用使用运行时 loadMicroApp | ACCEPTED | 开放面 |
| ADR-001 | 新建独立 UserRoleController | ACCEPTED | API面 |
| ADR-002 | 应用标识解析采用识别器模式 | ACCEPTED | API面 |
| ADR-003 | 内部凭证鉴权方案 | ACCEPTED | API面 |

### 2.3 技术依赖总览

| 依赖项 | 类型 | 版本约束 | 说明 |
|--------|------|---------|------|
| 能力开放平台 | Feature 依赖 | CAP-OPEN-001 | 独立但关联 |
| 嵌入能力平台面 | 子 Feature 依赖 | EMBED-PLATFORM-001 | 开放面消费平台面能力目录数据 |
| 企业内三方应用 | 外部集成 | — | 嵌入能力方（QianKun 子应用） |

---

## 修订记录

| 生成时间 | 变更 Feature | 生成方式 | 修订人 |
|---------|-------------|:--:|--------|
| 2026-08-03 | 狭义嵌入能力（平台面/开放面/API面） | 全量构建 | SDDU Docs Agent |
