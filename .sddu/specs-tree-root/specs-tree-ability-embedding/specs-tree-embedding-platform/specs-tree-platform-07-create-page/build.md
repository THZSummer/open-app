# 构建报告：新增表单（前端）

## 1. 构建概要

| 项目 | 内容 |
|------|------|
| Feature | EMBED-PLATFORM-CREATE-PAGE-001 |
| 任务 | TASK-007 |
| 类型 | 新增表单弹窗（前端） |
| 阶段 | tasked → builded |
| 前置依赖 | TASK-006 ✅ |

## 2. 文件变更

| 操作 | 文件 | 说明 |
|:--:|------|------|
| MODIFY | `market-web/src/configs/web.config.js` | 新增 `ABILITY_CREATE`、`FILE_UPLOAD` API 端点 |
| MODIFY | `market-web/src/router/routeRedBlue/ability-admin/thunk.js` | 新增 `uploadFile()`、`createAbility()` |
| NEW | `market-web/src/router/routeRedBlue/ability-admin/components/CreateForm.js` | 弹窗表单组件（12 个字段 + 上传 + 校验） |
| MODIFY | `market-web/src/router/routeRedBlue/ability-admin/index.js` | 添加「+ 添加能力」按钮 + CreateForm 集成 |
| MODIFY | `market-web/src/router/routeRedBlue/ability-admin/index.module.less` | 新增弹窗表单相关样式 |
| NEW | `market-web/tests/e2e/ability_admin/test_create.py` | Playwright E2E 测试（L1/L2/L4） |

## 3. 测试覆盖

### E2E 测试（`test_create.py`）

| 级别 | 测试用例 | 覆盖的验收标准 |
|:--:|------|:--:|
| L1 | `test_add_button_visible` | 「+ 添加能力」按钮可见 |
| L1 | `test_modal_opens_on_click` | 点击打开弹窗，标题正确 |
| L1 | `test_modal_all_required_fields_present` | 所有必填字段渲染 |
| L1 | `test_modal_close_on_cancel` | 取消关闭弹窗 |
| L1 | `test_modal_has_save_button` | 保存按钮可见 |
| L1 | `test_empty_fields_show_validation` | 必填校验提示 |
| L1 | `test_title_min_length_validation` | 标题最小长度校验 |
| L1 | `test_title_max_length_validation` | 标题最大长度校验 |
| L1 | `test_description_min_length_validation` | 描述最小长度校验 |
| L1 | `test_entry_url_format_validation` | URL 格式校验 |
| L1 | `test_route_path_format_validation` | 路由路径 / 开头校验 |
| L2 | `test_load_type_select_visible` | 加载类型选择器 |
| L2 | `test_order_num_default_value` | 排序号默认值 = 1 |
| L2 | `test_modal_closes_after_form_reset` | 关闭再打开表单重置 |
| L4 | `test_page_no_js_error_in_modal` | 弹窗操作无 JS 错误 |

## 4. 任务完成清单

- [x] 表单含所有 12 个字段（必填带红色星号）
- [x] 图标/示意图文件上传（调用 `POST /service/open/v2/file/upload`）
- [x] 图标必填校验 + 格式/大小前端校验
- [x] 示意图非必填，上传时格式校验
- [x] 名称 2~30 字符校验
- [x] 描述 5~200 字符校验（带实时字符计数）
- [x] 排序号默认 1，正整数校验
- [x] 访问地址 http/https 开头校验
- [x] 路由路径 / 开头校验
- [x] loadType 选择器（路由加载 / 微前端加载）
- [x] 提交时调用 `POST /service/open/v2/ability/admin`
- [x] 提交成功后关闭弹窗 + 刷新列表
- [x] `npm run build` 通过

## 5. 下一步

运行 `@sddu-review` 进行代码审查，再 `@sddu-validate` 进行 E2E 验证。
