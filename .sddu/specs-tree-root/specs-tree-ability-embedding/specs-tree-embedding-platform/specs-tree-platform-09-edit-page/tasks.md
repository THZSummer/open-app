# 任务：编辑表单（前端）

> 父 Feature: 嵌入能力平台面（EMBED-PLATFORM-001）  
> 对应任务: TASK-009 | 复杂度: M | FR: FR-003  
> 前置依赖: TASK-008

## 描述

在 market-web 实现编辑能力表单，从列表跳转时携带能力数据回填表单（含 loadType），abilityType 字段只读，支持图标/示意图替换上传，loadType 联动显示三要素必填状态，提交后跳转列表。API 调用 market-server 的 `/service/open/v2/ability/admin/{id}` 接口。

## 涉及文件

| 操作 | 文件路径 |
|:--:|------|
| NEW | `market-web/src/router/routeRedBlue/ability-admin/components/EditForm.tsx` |
| MODIFY | `market-web/src/router/routeRedBlue/ability-admin/thunk.ts`（追加 update API） |
| NEW | `market-web/tests/e2e/ability_admin/test_edit.py` |

## 验收标准

- [ ] 表单回填现有数据（含 loadType 回填）
- [ ] abilityType 字段只读
- [ ] loadType 选择器联动：loadType=2 时 entryUrl/routePath/aliasName 输入框显示为必填
- [ ] 名称前端校验：若修改，2-30 字符，不满足时内联提示
- [ ] 描述前端校验：若修改，5-200 字符，不满足时内联提示
- [ ] 排序值：≥1，支持手动输入和加减按钮
- [ ] 示意图替换上传校验：PNG/JPG、520×288PX、≤500KB
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
