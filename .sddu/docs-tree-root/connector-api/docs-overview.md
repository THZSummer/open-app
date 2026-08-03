# connector-api — 全景入口

> **文档定位**: sddu-docs-overview — 本级全景入口  
> **输出文件名**: docs-overview.md  
> **数据来源**: 代码扫描生成 — connector-api/src/main/java + application.yml  
> **创建时间**: 2026-08-03  
> **版本**: v1.0 (CODE-SCAN)  
> **生成方式**: 全量生成

---

## 1. 业务全景

### 1.1 自身概述

| 属性 | 值 |
|------|-----|
| **类型** | Spring Boot WebFlux 服务（连接流执行引擎 / 数据面） |
| **职责描述** | 连接器平台执行层：接收 HTTP 触发、同步执行连接流 DAG、版本调试、脚本节点（GraalJS 沙箱）、执行记录/步骤落库 |
| **所属业务域** | 连接器平台（数据面） |
| **版本** | 1.0.0-SNAPSHOT (Spring Boot 3.5.14, WebFlux + R2DBC) |

### 1.2 子组件

| 组件 | 类型 | 描述 | 关系说明 |
|------|------|------|---------|
| **flow 模块** | 模块 | FlowInvokeService：解析编排 DAG、触发执行 | 核心 |
| **flowversion 模块** | 模块 | FlowVersionDebugController：版本调试（跳过发布直接执行） | 依赖 flow |
| **connector 模块** | 模块 | ConnectorVersionEntity：连接器版本读取 | 依赖 flow |
| **execution 模块** | 模块 | ExecutionRecord/StepEntity：执行记录持久化 | 依赖 flow |

### 1.3 子组件分类

| 分类 | 包含组件 |
|------|---------|
| **执行引擎** | flow（invoke service） |
| **调试** | flowversion |
| **持久化** | connector、execution |

---

## 2. 技术全景

### 2.1 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Spring Boot WebFlux | 3.5.14 | 响应式 Web |
| Spring Data R2DBC | r2dbc-mysql | 响应式数据访问 |
| Reactive Redis | lettuce pool | 缓存/限流 |
| GraalJS (polyglot) | 24.2.1 + js-language | 脚本节点执行（ES2022 沙箱） |
| SpringDoc WebFlux | springdoc-openapi-starter-webflux-ui | API 文档 |

### 2.2 执行流程（FlowInvokeService）

```
HTTP 触发 POST /api/v1/flows/{flowId}/invoke
  → 读取 flow_t + deployed flow_version_t（deployed_version_id 指针）
  → 解析 orchestration_config（nodes[]/edges[] DAG）
  → 查找 trigger 节点（node_trigger）
  → 按 edges 顺序执行节点：
      trigger → connector（调用下游 HTTP）→ script（GraalJS）→ parallel → exit
  → 出口节点 body/header 作为 HTTP 响应体/头返回
  → 写 execution_record_t + execution_step_t
```

### 2.3 架构决策记录（ADR）

| 编号 | 标题 | 状态 | 影响范围 |
|:--:|------|:--:|---------|
| — | 本级由代码扫描生成，无独立 ADR（设计见 specs-tree-connector-platform-v3） | — | — |

### 2.4 本域文档

| 文档 | 说明 |
|------|------|
| `api.md` | connector-api API 端点 |
| `config.md` | connector-api 配置项（R2DBC/Redis/WebFlux） |
| `security.md` | 脚本沙箱安全模型 |

---

## 修订记录

| 生成时间 | 变更 Feature | 生成方式 | 修订人 |
|---------|-------------|:--:|--------|
| 2026-08-03 | 代码扫描全量生成 | code-scan | SDDU Docs Agent |
