# 任务：删除操作（前端）

> 父 Feature: 嵌入能力平台面（EMBED-PLATFORM-001）  
> 对应任务: TASK-011 | 复杂度: S | FR: FR-004  
> 前置依赖: TASK-010

## 描述

在列表页内新增删除按钮，点击弹出确认弹窗，调用 market-server 的 DELETE 接口，有订阅时展示错误提示（含关联订阅数量）。

## 涉及文件

| 操作 | 文件路径 |
|:--:|------|
| MODIFY | `market-web/src/router/routeRedBlue/ability-admin/index.tsx`（新增删除按钮+确认弹窗） |
| MODIFY | `market-web/src/router/routeRedBlue/ability-admin/thunk.ts`（追加 delete API） |
| NEW | `market-web/tests/e2e/ability_admin/test_delete.py` |

## 验收标准

- [ ] 列表每行有删除按钮
- [ ] 点击弹出确认弹窗
- [ ] 确认后调用 DELETE 接口
- [ ] 有订阅时展示 "该能力已被 XX 个应用订阅，无法删除"
- [ ] Playwright E2E: test_delete.py L1/L4 全部通过（确认对话框弹出/取消删除数据不变/有订阅时删除被阻止）

## 验证

```bash
# 编译检查
cd market-web && npm run build

# Playwright E2E
pytest tests/e2e/ability_admin/test_delete.py -m "" -v
```
