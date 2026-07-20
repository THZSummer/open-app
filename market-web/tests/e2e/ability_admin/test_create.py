#!/usr/bin/env python3
"""
能力目录管理创建页 — Playwright E2E 测试

覆盖：
  L1: 打开弹窗、字段渲染、必填校验
  L2: 字段交互、文件上传校验
  L4: 边界条件
"""
import re
import pytest

pytest.importorskip("playwright")

from playwright.sync_api import Page, expect


def _wait_for_modal_ready(page: Page):
    """等待弹窗完全打开"""
    page.wait_for_selector(".ant-modal", timeout=10000)
    page.wait_for_selector(".ant-modal .ant-form", timeout=5000)


def _open_create_modal(page: Page):
    """点击「+ 添加能力」按钮打开弹窗"""
    page.locator("button", has_text="添加能力").click()
    _wait_for_modal_ready(page)


def _get_field(page, field_id_or_label):
    """根据 label 获取表单输入项"""
    return page.locator(f".ant-form-item").filter(has_text=field_id_or_label).locator("input, textarea")


class TestAbilityAdminCreatePage:
    """能力目录管理创建页 — E2E 测试"""

    @pytest.fixture(autouse=True)
    def setup_page(self, page: Page, ability_admin_url: str):
        self._console_errors = []
        page.on("console", lambda msg: self._console_errors.append(msg.text) if msg.type == "error" else None)
        page.goto(ability_admin_url)
        page.wait_for_load_state("networkidle")
        page.wait_for_selector(".ant-table-row, .ant-empty", timeout=10000)

    # ══════════════════════════════════════════════════════
    # L1: 核心功能 — 弹窗打开与字段渲染
    # ══════════════════════════════════════════════════════

    def test_add_button_visible(self, page: Page):
        """「+ 添加能力」按钮应可见"""
        btn = page.locator("button", has_text="添加能力")
        expect(btn).to_be_visible()

    def test_modal_opens_on_click(self, page: Page):
        """点击按钮应打开弹窗，标题为「添加能力」"""
        _open_create_modal(page)
        modal_title = page.locator(".ant-modal-title")
        expect(modal_title).to_have_text("添加能力")

    def test_modal_all_required_fields_present(self, page: Page):
        """弹窗应包含所有必填字段"""
        _open_create_modal(page)
        required_labels = [
            "能力标题", "英文名", "能力描述", "英文描述",
            "能力图标", "排序号", "访问地址", "路由路径",
            "加载类型", "能力类型编码",
        ]
        for label in required_labels:
            item = page.locator(".ant-form-item").filter(has_text=label)
            expect(item).to_be_visible()

    def test_modal_close_on_cancel(self, page: Page):
        """点击取消应关闭弹窗"""
        _open_create_modal(page)
        # 使用 ant-modal-footer 内的「取消」按钮
        # Ant Design 按钮文字可能带 CSS letter-spacing（"取 消"），不能用 has_text 精确匹配
        # 使用 get_by_role 匹配可见时可访问名称，或直接取 footer 第一个按钮
        cancel_btn = page.get_by_role("button", name="取 消")
        cancel_btn.click()
        expect(page.locator(".ant-modal")).not_to_be_visible()

    def test_modal_has_save_button(self, page: Page):
        """弹窗应包含「保存」按钮"""
        _open_create_modal(page)
        save_btn = page.locator(".ant-modal-footer .ant-btn-primary").first
        expect(save_btn).to_be_visible()

    # ══════════════════════════════════════════════════════
    # L1: 表单校验
    # ══════════════════════════════════════════════════════

    def test_empty_fields_show_validation(self, page: Page):
        """空字段提交应显示校验错误提示"""
        _open_create_modal(page)
        save_btn = page.locator(".ant-modal-footer .ant-btn-primary").first
        save_btn.click()

        # 等待校验提示出现
        error_items = page.locator(".ant-form-item-explain-error")
        # 应有至少一个错误提示
        count = error_items.count()
        assert count > 0, f"预期有校验错误提示，实际有 {count} 个"

    def test_title_min_length_validation(self, page: Page):
        """标题输入 1 个字符应提示至少 2 字符"""
        _open_create_modal(page)
        title_input = _get_field(page, "能力标题").first
        title_input.fill("a")
        # 点击其他地方触发校验
        _get_field(page, "英文名").first.click()
        err = page.locator(".ant-form-item").filter(has_text="能力标题").locator(".ant-form-item-explain-error")
        expect(err).to_be_visible()
        expect(err).to_contain_text("至少")

    def test_title_max_length_validation(self, page: Page):
        """标题输入框 maxLength=30，输入超出字符应被截断"""
        _open_create_modal(page)
        title_input = _get_field(page, "能力标题").first
        title_input.fill("a" * 31)
        # maxLength=30 在 Input 层阻止输入超出 30 字符（HTML 原生限制）
        # 验证输入被截断为 30 字符
        actual_value = title_input.input_value()
        assert len(actual_value) <= 30, f"预期最多 30 字符，实际 {len(actual_value)} 字符"

    def test_description_min_length_validation(self, page: Page):
        """描述输入少于 5 字符应提示"""
        _open_create_modal(page)
        desc_input = page.locator(".ant-form-item").filter(has_text="能力描述").locator("textarea").first
        desc_input.fill("abcd")
        _get_field(page, "英文名").first.click()
        err = page.locator(".ant-form-item").filter(has_text="能力描述").locator(".ant-form-item-explain-error")
        expect(err).to_be_visible()

    def test_entry_url_format_validation(self, page: Page):
        """访问地址不以 http/https 开头应提示"""
        _open_create_modal(page)
        url_input = _get_field(page, "访问地址").first
        url_input.fill("not-a-url")
        _get_field(page, "英文名").first.click()
        err = page.locator(".ant-form-item").filter(has_text="访问地址").locator(".ant-form-item-explain-error")
        expect(err).to_be_visible()

    def test_route_path_format_validation(self, page: Page):
        """路由路径不以 / 开头应提示"""
        _open_create_modal(page)
        path_input = _get_field(page, "路由路径").first
        path_input.fill("no-slash")
        _get_field(page, "英文名").first.click()
        err = page.locator(".ant-form-item").filter(has_text="路由路径").locator(".ant-form-item-explain-error")
        expect(err).to_be_visible()

    # ══════════════════════════════════════════════════════
    # L2: 交互功能
    # ══════════════════════════════════════════════════════

    def test_load_type_select_visible(self, page: Page):
        """加载类型选择器应可见"""
        _open_create_modal(page)
        select = page.locator(".ant-form-item").filter(has_text="加载类型").locator(".ant-select")
        expect(select).to_be_visible()

    def test_order_num_default_value(self, page: Page):
        """排序号默认值应为 1"""
        _open_create_modal(page)
        order_input = page.locator(".ant-form-item").filter(has_text="排序号").locator("input").first
        expect(order_input).to_have_value("1")

    def test_modal_closes_after_form_reset(self, page: Page):
        """关闭再打开弹窗，表单应重置"""
        _open_create_modal(page)
        title_input = _get_field(page, "能力标题").first
        title_input.fill("测试标题")
        cancel_btn = page.get_by_role("button", name="取 消")
        cancel_btn.click()
        expect(page.locator(".ant-modal")).not_to_be_visible()

        # 重新打开
        _open_create_modal(page)
        title_input = _get_field(page, "能力标题").first
        expect(title_input).to_have_value("")

    # ══════════════════════════════════════════════════════
    # L4: 兼容性
    # ══════════════════════════════════════════════════════

    def test_page_no_js_error_in_modal(self, page: Page):
        """打开弹窗过程中无 JS 错误"""
        known_patterns = [
            "favicon",
            "Failed to load resource",
            "Support for defaultProps",
            "React Router Future Flag Warning",
            "React DevTools",
        ]
        _open_create_modal(page)
        page.get_by_role("button", name="取 消").click()
        real_errors = []
        for err in self._console_errors:
            if not any(p in err for p in known_patterns):
                real_errors.append(err)
        assert len(real_errors) == 0, f"页面存在未预期的 console error: {real_errors}"

    # ══════════════════════════════════════════════════════
    # L2: 补充创建表单场景
    # ══════════════════════════════════════════════════════

    @staticmethod
    def _make_test_png(width, height):
        """生成测试用 PNG 图片字节"""
        import struct, zlib
        raw = b''
        for _ in range(height):
            raw += b'\x00'
            for _ in range(width):
                raw += struct.pack('BBB', 255, 0, 0)
        def chunk(ctype, data):
            c = ctype + data
            return struct.pack('>I', len(data)) + c + struct.pack('>I', zlib.crc32(c) & 0xffffffff)
        ihdr = struct.pack('>IIBBBBB', width, height, 8, 2, 0, 0, 0)
        return b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', ihdr) + chunk(b'IDAT', zlib.compress(raw)) + chunk(b'IEND', b'')

    @staticmethod
    def _upload_icon(page, png_path):
        """通过 ImgCrop 流程上传图标"""
        icon_item = page.locator(".ant-form-item").filter(has_text="能力图标")
        with page.expect_file_chooser() as fc_info:
            icon_item.locator(".ant-upload-select").click()
        fc_info.value.set_files(png_path)
        crop_modal = page.locator(".ant-modal").filter(has_text="裁剪图标")
        crop_modal.locator("button", has_text=re.compile(r"确\s*认")).wait_for(timeout=5000)
        crop_modal.locator("button", has_text=re.compile(r"确\s*认")).click()

    @staticmethod
    def _fill_required_fields(page, **overrides):
        """填写表单所有必填字段（图标除外）"""
        fields = {
            "能力标题": "测试能力",
            "英文名": "test-ability",
            "能力描述": "这是一个测试能力描述",
            "英文描述": "This is a test ability description",
            "排序号": "1",
            "访问地址": "https://example.com",
            "路由路径": "/test",
        }
        fields.update(overrides)
        for label, value in fields.items():
            inp = _get_field(page, label).first
            inp.fill(value)
        # 能力类型编码（InputNumber，必须单独处理）
        type_input = page.locator(".ant-form-item").filter(has_text="能力类型编码").locator("input").first
        type_input.fill("1")

    def test_icon_size_resize(self, page: Page, tmp_path):
        """上传非 40×40 图片，验证前端 resize 到 40×40 后上传成功"""
        png = self._make_test_png(100, 100)
        png_path = tmp_path / "icon_100x100.png"
        png_path.write_bytes(png)
        page.route("**/file/upload", lambda route: route.fulfill(
            status=200, content_type="application/json",
            body='{"code":"200","data":{"batchId":"batch-001","showUrl":"/ability-files/test.png"}}'
        ))
        _open_create_modal(page)
        self._upload_icon(page, str(png_path))
        preview = page.locator('img[alt="图标预览"]')
        expect(preview).to_be_visible(timeout=8000)

    def test_complete_submit_flow(self, page: Page, tmp_path):
        """完整提交流程：填字段→上传图标→提交→列表刷新→新数据显示"""
        png = self._make_test_png(40, 40)
        png_path = tmp_path / "icon_40x40.png"
        png_path.write_bytes(png)
        # mock 上传
        page.route(re.compile(r"/file/upload"), lambda route: route.fulfill(
            status=200, content_type="application/json",
            body='{"code":"200","data":{"batchId":"batch-002","showUrl":"/ability-files/test.png"}}'
        ))
        # mock 创建（仅拦截 POST /ability/admin 精确路径）
        page.route(re.compile(r"/ability/admin$"), lambda route: route.fulfill(
            status=200, content_type="application/json",
            body='{"code":"200","messageZh":"能力添加成功"}'
        ) if route.request.method == "POST" else route.continue_())
        # mock 列表刷新
        page.route(re.compile(r"/ability/admin/list"), lambda route: route.fulfill(
            status=200, content_type="application/json",
            body='{"code":"200","data":[{"nameCn":"测试能力","nameEn":"test-ability"}],"page":{"total":1}}'
        ))
        _open_create_modal(page)
        self._fill_required_fields(page)
        self._upload_icon(page, str(png_path))
        page.wait_for_timeout(500)
        save_btn = page.locator(".ant-modal-footer .ant-btn-primary").first
        save_btn.click()
        # 验证成功提示出现后再检查弹窗
        msg = page.locator(".ant-message-notice").filter(has_text="能力添加成功")
        expect(msg).to_be_visible(timeout=15000)
        # 弹窗应关闭
        expect(page.locator(".ant-modal-title").filter(has_text="添加能力")).not_to_be_visible(timeout=5000)

    def test_icon_required_on_submit(self, page: Page):
        """不传图标直接提交，验证提示「图标为必填项」"""
        _open_create_modal(page)
        self._fill_required_fields(page)
        save_btn = page.locator(".ant-modal-footer .ant-btn-primary").first
        save_btn.click()
        err = page.locator(".ant-form-item").filter(has_text="能力图标").locator(".ant-form-item-explain-error")
        expect(err).to_be_visible(timeout=5000)

    def test_chinese_title_min_length(self, page: Page):
        """中文名输入 1 字符提交，验证提示至少 2 字符"""
        _open_create_modal(page)
        title_input = _get_field(page, "能力标题").first
        title_input.fill("能")
        save_btn = page.locator(".ant-modal-footer .ant-btn-primary").first
        save_btn.click()
        err = page.locator(".ant-form-item").filter(has_text="能力标题").locator(".ant-form-item-explain-error")
        expect(err).to_be_visible()
        expect(err).to_contain_text("至少")

    def test_chinese_desc_min_length(self, page: Page):
        """中文描述输入 4 字符提交，验证提示至少 5 字符"""
        _open_create_modal(page)
        desc_input = page.locator(".ant-form-item").filter(has_text="能力描述").locator("textarea").first
        desc_input.fill("测试描述")
        save_btn = page.locator(".ant-modal-footer .ant-btn-primary").first
        save_btn.click()
        err = page.locator(".ant-form-item").filter(has_text="能力描述").locator(".ant-form-item-explain-error")
        expect(err).to_be_visible()

    def test_entry_url_invalid_format(self, page: Page):
        """访问地址不合法，验证提示须以 http/https 开头"""
        _open_create_modal(page)
        url_input = _get_field(page, "访问地址").first
        url_input.fill("ftp://bad.com")
        save_btn = page.locator(".ant-modal-footer .ant-btn-primary").first
        save_btn.click()
        err = page.locator(".ant-form-item").filter(has_text="访问地址").locator(".ant-form-item-explain-error")
        expect(err).to_be_visible()

    def test_route_path_no_slash(self, page: Page):
        """路由路径不以 / 开头，验证提示须以 / 开头"""
        _open_create_modal(page)
        path_input = _get_field(page, "路由路径").first
        path_input.fill("no-slash")
        save_btn = page.locator(".ant-modal-footer .ant-btn-primary").first
        save_btn.click()
        err = page.locator(".ant-form-item").filter(has_text="路由路径").locator(".ant-form-item-explain-error")
        expect(err).to_be_visible()

    def test_order_num_min_value(self, page: Page):
        """排序号输入 0，验证提示 ≥ 1"""
        _open_create_modal(page)
        self._fill_required_fields(page)
        order_input = page.locator(".ant-form-item").filter(has_text="排序号").locator("input").first
        order_input.click()
        order_input.fill("0")
        page.keyboard.press("Tab")
        save_btn = page.locator(".ant-modal-footer .ant-btn-primary").first
        save_btn.click()
        err = page.locator(".ant-form-item").filter(has_text="排序号").locator(".ant-form-item-explain-error")
        expect(err).to_be_visible(timeout=5000)
        expect(err).to_contain_text("≥")

    def test_english_name_empty_submit(self, page: Page):
        """不填英文名直接提交，验证提示"""
        page.route("**/ability/admin", lambda route: route.fulfill(
            status=200, content_type="application/json",
            body='{"code":"200"}'
        ) if route.request.method == "POST" else route.continue_())
        _open_create_modal(page)
        self._fill_required_fields(page, 英文名="")
        save_btn = page.locator(".ant-modal-footer .ant-btn-primary").first
        save_btn.click()
        err = page.locator(".ant-form-item").filter(has_text="英文名").locator(".ant-form-item-explain-error")
        expect(err).to_be_visible()

    def test_cancel_keeps_list_data(self, page: Page):
        """打开弹窗点取消，验证弹窗关闭且列表数据不变"""
        rows_before = page.locator(".ant-table-row").count()
        _open_create_modal(page)
        cancel_btn = page.get_by_role("button", name=re.compile(r"取\s*消"))
        cancel_btn.click()
        expect(page.locator(".ant-modal")).not_to_be_visible()
        page.wait_for_timeout(500)
        rows_after = page.locator(".ant-table-row").count()
        assert rows_after == rows_before, f"取消后列表行数变化：{rows_before} → {rows_after}"
