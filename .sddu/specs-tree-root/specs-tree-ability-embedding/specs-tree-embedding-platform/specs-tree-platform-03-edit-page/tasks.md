# 任务：编辑表单（前端）

> 父 Feature: 嵌入能力平台面（EMBED-PLATFORM-001）  
> 对应任务: TASK-008 | 复杂度: M | 波次: 3 | FR: FR-003  
> 前置依赖: TASK-007

## 描述

在 market-web 实现编辑能力表单，从列表跳转时携带能力数据回填表单，abilityType 字段只读，支持图标/示意图替换上传，提交后跳转列表。API 调用 market-server 的 `/service/open/v2/ability/admin/{id}` 接口。

## 涉及文件

| 操作 | 文件路径 |
|:--:|------|
| NEW | `market-web/src/router/routeRedBlue/ability-admin/components/EditForm.tsx` |
| MODIFY | `market-web/src/router/routeRedBlue/ability-admin/thunk.ts`（追加 update API） |

## 验收标准

- [ ] 表单回填现有数据
- [ ] abilityType 字段只读
- [ ] 图标/示意图可替换上传
- [ ] 提交后跳转列表

## 验证

```bash
cd market-web && npm run build
```
