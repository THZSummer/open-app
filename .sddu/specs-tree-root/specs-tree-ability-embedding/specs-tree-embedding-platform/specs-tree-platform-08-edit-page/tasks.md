# 任务：编辑表单（前端）

> 父 Feature: 嵌入能力平台面（EMBED-PLATFORM-001）  
> 对应任务: TASK-008 | 复杂度: M | FR: FR-003  
> 前置依赖: TASK-007

## 描述

在 market-web 实现编辑能力表单，从列表跳转时携带能力数据回填表单，abilityType 字段只读，支持图标/示意图替换上传，提交后跳转列表。API 调用 market-server 的 `/service/open/v2/ability/admin/{id}` 接口。

## 涉及文件

| 操作 | 文件路径 |
|:--:|------|
| NEW | `market-web/src/router/routeRedBlue/ability-admin/components/EditForm.tsx` |
| MODIFY | `market-web/src/router/routeRedBlue/ability-admin/thunk.ts`（追加 update API） |
| NEW | `market-web/tests/e2e/ability_admin/test_edit.py` |

## 验收标准

- [ ] 表单回填现有数据
- [ ] abilityType 字段只读
- [ ] 图标/示意图可替换上传
- [ ] 提交后跳转列表
- [ ] Playwright E2E: test_edit.py L1/L2 全部通过（编辑提交成功/abilityType只读/取消编辑数据不变）

## 验证

```bash
# 编译检查
cd market-web && npm run build

# Playwright E2E
pytest tests/e2e/ability_admin/test_edit.py -m "" -v
```
