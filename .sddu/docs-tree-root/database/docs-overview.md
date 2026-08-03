# 数据库域 — 全景入口

> **文档定位**: sddu-docs-overview — 本级全景入口  
> **输出文件名**: docs-overview.md  
> **数据来源**: 代码扫描生成 — open-flyway 迁移工程  
> **创建时间**: 2026-08-03  
> **版本**: v1.0 (CODE-SCAN)  
> **生成方式**: 全量生成

---

## 1. 业务全景

### 1.1 自身概述

| 属性 | 值 |
|------|-----|
| **类型** | 数据库迁移域（Flyway 工程） |
| **职责描述** | 统一管理 open-app 全部数据库 schema：7 个迁移脚本、40 张表，覆盖能力开放平台、连接器平台、市场管理、文件存储 |
| **所属业务域** | 数据层 |
| **版本** | V1~V7 |

### 1.2 子组件

| 组件 | 类型 | 描述 | 关系说明 |
|------|------|------|---------|
| **V1 早期 Schema** | 迁移脚本 | 16 表：应用/能力/字典/运维域 | 基础 |
| **V2 能力开放平台** | 迁移脚本 | 15 表：分类/API/事件/回调/权限/订阅/审批/授权 | 依赖 V1 |
| **V3 连接器 MVP** | 迁移脚本 | 4 表：连接器/连接流 | 独立域 |
| **V4 连接器 V3** | 迁移脚本 | 3 新表 + 5 表 ALTER（多版本） | 演进 V3 |
| **V5 嵌入能力** | 迁移脚本 | ability_t 增 6 字段 | 演进 V1 |
| **V6 通用文件** | 迁移脚本 | common_file_t | 独立 |
| **V7 LookUp 文件** | 迁移脚本 | lookup_file_t | 独立 |

### 1.3 子组件分类

| 分类 | 包含组件 |
|------|---------|
| **能力开放域** | V1 (app/ability/lookup 系) + V2 (api/event/callback/permission/subscription/approval) + V5 |
| **连接器平台域** | V3 + V4 (connector/flow/execution) |
| **基础设施域** | V1 (operate_log/property/file/employee) + V6 + V7 |

---

## 2. 技术全景

### 2.1 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| **Flyway** | flyway-maven-plugin + flyway-mysql | 迁移执行 |
| **MySQL Connector/J** | 9.7.0 | JDBC 驱动 |
| **MySQL** | 5.7/8.x 兼容 | 目标数据库 `openapp` |
| **存储过程幂等** | safe_add_column 等 4 个公共过程 | V4/V5/V6 幂等 DDL |

### 2.2 迁移执行方式

```bash
cd open-flyway
mvn flyway:migrate -Dflyway.url=jdbc:mysql://<host>:3306/openapp -Dflyway.user=<user> -Dflyway.password=<pwd>
```

### 2.3 设计约定

- 表前缀：`openplatform_`（V1）/ `openplatform_v2_`（V2+）；连接器平台 `openplatform_v2_cp_`
- 表后缀：`_t`
- 主键：V1 部分自增，V2+ 应用层雪花 ID
- 无物理外键（逻辑外键）
- 审计字段 4 个：create_by / create_time / last_update_by / last_update_time
- 枚举统一 TINYINT(10) + COMMENT 映射
- V4+ 幂等设计：存储过程判断后执行，支持重复运行

---

## 修订记录

| 生成时间 | 变更 Feature | 生成方式 | 修订人 |
|---------|-------------|:--:|--------|
| 2026-08-03 | 代码扫描全量生成 | code-scan | SDDU Docs Agent |
