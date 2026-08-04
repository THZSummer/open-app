# LookUp 管理

> **文档定位**: sddu-docs-object — 业务对象文档 — 标准化的枚举值和配置项集中维护
> **输出文件名**: LookUp管理.md
> **数据来源**: specs-tree-lookup (spec v1.0 / plan v1.0) + 代码扫描设计真相
> **创建时间**: 2026-08-03
> **版本**: v1.0 (BIZ-ARCH)

## 1. 业务概述

| 属性 | 值 |
|------|-----|
| **能力类型** | 基础数据支撑（能力开放平台能力之一） |
| **对应 Feature** | FR-LOOKUP-001（LookUp 管理） |
| **状态** | ✅ planned（规范 + 技术规划完成，待任务分解） |
| **业务角色** | LookUp 管理是系统的基础数据管理模块，统一管理业务系统中各类枚举值和配置项，实现业务常量的集中维护和动态配置 |

**业务说明**（spec §2.2）：LookUp 分类管理 + LookUp 项管理。通过标准化的管理机制，实现业务常量的集中维护和动态配置，是平台各模块（应用类型、认证类型、状态枚举等）的配置来源。

**核心功能**：
- LookUp 分类管理：分类 CRUD、级联删除、异步批量导入/导出
- LookUp 项管理：项 CRUD、扩展属性 1-6、排序、状态切换
- 标准化的枚举值和配置项集中维护机制

## 2. 核心功能

| 功能域 | 功能 | 说明 |
|--------|------|------|
| **分类管理** | 分类 CRUD | 级联删除、异步批量导入/导出 |
| **项管理** | 项 CRUD | 扩展属性 1-6、排序、状态切换 |
| **批量导入/导出** | Excel | 异步任务，进度跟踪 |
| **白名单** | lookup/whitelist | LookUp 白名单查询（open-server） |

## 3. 设计真相

### 3.1 相关表

| 表名 | 说明 | 关键字段 |
|------|------|---------|
| `lookup_classify_t` | LookUp 分类表 | classify_id, 分类信息 |
| `lookup_item_t` | LookUp 项表 | classify_id → 归属分类（1:N）, 扩展属性 1-6, 排序, 状态 |
| `lookup_file_t` | LookUp 文件表 | V7 新增（批量导入文件存储） |

### 3.2 相关 API

| 方法 | 路径 | 说明 |
|:----:|------|------|
| GET | /service/open/v2/lookup/classify/list | 分类列表 |
| POST | /service/open/v2/lookup/classify | 新增分类 |
| PUT | /service/open/v2/lookup/classify/{classifyId} | 编辑分类 |
| DELETE | /service/open/v2/lookup/classify/{classifyId} | 删除分类 |
| GET | /service/open/v2/lookup/classify/{classifyId}/items | 分类下项列表 |
| POST | /service/open/v2/lookup/classify/{classifyId}/items | 新增项 |
| PUT | /service/open/v2/lookup/items/{itemId} | 编辑项 |
| DELETE | /service/open/v2/lookup/items/{itemId} | 删除项 |
| GET | /service/open/v2/lookup/whitelist | LookUp 白名单（open-server） |

### 3.3 工程映射

| 服务 | 角色 |
|------|------|
| market-server | LookUp 分类/项 CRUD（模块 lookup） |
| open-server | LookUp 白名单查询 |
| market-web | 市场管理后台（LookUp 管理页面） |

---

## 修订记录

| 版本 | 变更说明 | 日期 | 修订人 |
|------|---------|------|--------|
| v1.0 | 业务视角重建 | 2026-08-03 | SDDU Docs Agent |
