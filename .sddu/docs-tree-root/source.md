# open-app 全景 — 产物溯源

> **文档定位**: sddu-docs-source — 列出本文档聚合的所有原始产物文件，含文件路径、版本和最后修改时间  
> **输出文件名**: source.md  
> **数据来源**: 代码扫描生成（用户指令触发）  
> **创建人**: SDDU Docs Agent  
> **创建时间**: 2026-08-03  
> **版本**: v1.0 (CODE-SCAN)  

## 1. 扫描信息源

> ⚠️ 本全景基于 **代码扫描** 生成，信息源为项目实际代码与配置文件，非 specs-tree-root 过程产物。

### 1.1 数据库迁移（open-flyway）

| 文件 | 版本 | 说明 |
|------|:----:|------|
| open-flyway/src/main/resources/db/migration/V1__create_early_schema.sql | V1 | 早期 schema（16 表） |
| open-flyway/src/main/resources/db/migration/V2__init_capability_open_platform_schema.sql | V2 | 能力开放平台（15 表） |
| open-flyway/src/main/resources/db/migration/V3__init_connector_platform_schema.sql | V3 | 连接器 MVP（4 表） |
| open-flyway/src/main/resources/db/migration/V4__connector_platform_v3_schema.sql | V4 | 连接器 V3（3 表 + ALTER） |
| open-flyway/src/main/resources/db/migration/V5__add_ability_admin_fields.sql | V5 | 嵌入能力字段 |
| open-flyway/src/main/resources/db/migration/V6__create_common_file.sql | V6 | 通用文件表 |
| open-flyway/src/main/resources/db/migration/V7__create_lookup_file_table.sql | V7 | LookUp 文件表 |

### 1.2 后端服务代码

| 服务 | 扫描范围 | 实体数 | Controller 数 |
|------|---------|:-----:|:------------:|
| open-server | src/main/java + application*.yml | 30+ | 20 |
| api-server | src/main/java + application*.yml | 9 | 6 |
| connector-api | src/main/java + application.yml | 4 | 2 |
| event-server | src/main/java + application*.yml | 0（无实体） | 4 |
| market-server | src/main/java + application*.yml | 7 | 8 |

### 1.3 前端工程

| 工程 | 扫描范围 | 说明 |
|------|---------|------|
| wecodesite | package.json + src/pages/**/route.js | 开发者控制台（20 页面） |
| market-web | package.json + src/router/routeRedBlue/ | 市场管理（8 模块） |
| qiankunProject | main-app/src/microApps.js + sub-app-*/src/router | 微前端（4 子应用） |
| wecodesiteDemo | *.html | 静态原型（9 页） |

### 1.4 specs-tree-root 基线（§5.3 一致性检测）

| Feature | 状态 | 说明 |
|---------|------|------|
| specs-tree-capability-open-platform | — | 能力开放平台 spec（含 FR 基线） |
| specs-tree-connector-platform | validated | 连接器 V1 |
| specs-tree-connector-platform-v2 | terminated | 连接器 V2（已归档） |
| specs-tree-connector-platform-v3 | validated | 连接器 V3（当前） |
| specs-tree-data-open-platform | suspended | 数据开放平台 |
| specs-tree-app-list | planned | 应用管理 |
| specs-tree-dictionary / lookup | — | 基础配置 |
| specs-tree-ability-embedding | planned | 嵌入能力 |

## 2. 生成说明

- **生成方式**: 全量生成（覆盖重建，旧 4 域 17 文档已清理）
- **生成时间**: 2026-08-03
- **一致性检测**: 执行了 §5.3（详见 docs-overview.md §3）
- **未覆盖信息**: 数据库运行时数据、生产环境实际部署、未运行测试验证

---

## 修订记录

| 版本 | 变更说明 | 日期 | 修订人 |
|------|---------|------|--------|
| v1.0 | 代码扫描全量生成 | 2026-08-03 | SDDU Docs Agent |
