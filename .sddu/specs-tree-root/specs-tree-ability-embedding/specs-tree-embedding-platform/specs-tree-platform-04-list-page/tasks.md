# 任务：列表页面（前端）

> 父 Feature: 嵌入能力平台面（EMBED-PLATFORM-001）  
> 对应任务: TASK-004 | 复杂度: L | FR: FR-001 | 前置依赖: TASK-003  
> 前置依赖: TASK-003

## 描述

在 market-web 实现能力目录管理列表页，含分页表格、关键词搜索、排序功能，注册路由。API 调用 market-server 的 `/service/open/v2/ability/admin/list` 接口。

## 涉及文件

| 操作 | 文件路径 |
|:--:|------|
| NEW | `market-web/src/router/routeRedBlue/ability-admin/index.tsx` |
| NEW | `market-web/src/router/routeRedBlue/ability-admin/thunk.ts` |
| MODIFY | `market-web/src/router/index.tsx`（新增 import + Route） |

## 验收标准

- [ ] 列表展示所有字段（编码/中英文名/描述/图标/排序号/entryUrl/routePath/aliasName/hidden/requireRelease/时间）
- [ ] 分页正常，搜索可用
- [ ] 路由已注册

## 验证

```bash
cd market-web && npm run build
```
