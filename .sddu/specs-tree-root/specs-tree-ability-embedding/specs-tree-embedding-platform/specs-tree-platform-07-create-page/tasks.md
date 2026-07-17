# 任务：新增表单（前端）

> 父 Feature: 嵌入能力平台面（EMBED-PLATFORM-001）  
> 对应任务: TASK-007 | 复杂度: M | FR: FR-002  
> 前置依赖: TASK-006

## 描述

在 market-web 实现创建能力表单，含所有字段输入框、图标/示意图文件上传、前端校验（编码范围提示、URL 格式、loadType 联动校验），提交后跳转列表。API 调用 market-server 的 `/service/open/v2/ability/admin` 接口。

## 涉及文件

| 操作 | 文件路径 |
|:--:|------|
| NEW | `market-web/src/router/routeRedBlue/ability-admin/components/CreateForm.tsx` |
| MODIFY | `market-web/src/router/routeRedBlue/ability-admin/thunk.ts`（追加 create API） |
| NEW | `market-web/tests/e2e/ability_admin/test_create.py` |

## 验收标准

- [ ] 表单含所有字段输入框（含新增6字段，loadType 默认1=路由加载）
- [ ] abilityType 编码校验、entryUrl 格式校验
- [ ] 名称前端校验：2-30 字符，不满足时内联提示
- [ ] 描述前端校验：5-200 字符，不满足时内联提示
- [ ] 图标必填校验：未上传时阻止提交并提示"请上传图标（PNG/SVG，40×40PX，≤200KB）"
- [ ] 示意图格式校验：上传时前端校验 PNG/JPG 格式、尺寸 520×288PX、大小 ≤500KB
- [ ] 排序值：默认当前最大值+1，支持手动输入和加减按钮，校验 ≥1
- [ ] loadType 选择器联动：loadType=2 时 entryUrl/routePath/aliasName 输入框置为必填
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
