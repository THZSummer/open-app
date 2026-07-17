# 任务：新增表单（前端）

> 父 Feature: 嵌入能力平台面（EMBED-PLATFORM-001）  
> 对应任务: TASK-006 | 复杂度: M | FR: FR-002  
> 前置依赖: TASK-005

## 描述

在 market-web 实现创建能力表单，含所有字段输入框、图标/示意图文件上传、前端校验（编码范围提示、URL 格式），提交后跳转列表。API 调用 market-server 的 `/service/open/v2/ability/admin` 接口。

## 涉及文件

| 操作 | 文件路径 |
|:--:|------|
| NEW | `market-web/src/router/routeRedBlue/ability-admin/components/CreateForm.tsx` |
| MODIFY | `market-web/src/router/routeRedBlue/ability-admin/thunk.ts`（追加 create API） |
| NEW | `market-web/tests/e2e/ability_admin/test_create.py` |

## 验收标准

- [ ] 表单含所有字段输入框（含新增5字段）
- [ ] abilityType 编码校验、entryUrl 格式校验
- [ ] 图标/示意图文件上传正常
- [ ] 创建成功后跳转列表页
- [ ] Playwright E2E: test_create.py L1/L2/L4 全部通过（正常创建/全字段创建/重复编码拒绝/空名称校验/非法URL拒绝）

## 验证

```bash
# 编译检查
cd market-web && npm run build

# Playwright E2E
pytest tests/e2e/ability_admin/test_create.py -m "" -v
```
