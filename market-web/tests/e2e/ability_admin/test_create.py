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
        cancel_btn = page.locator(".ant-modal .ant-btn").filter(has_text="取消")
        cancel_btn.click()
        expect(page.locator(".ant-modal")).not_to_be_visible()

    def test_modal_has_save_button(self, page: Page):
        """弹窗应包含「保存」按钮"""
        _open_create_modal(page)
        save_btn = page.locator(".ant-modal .ant-btn-primary").filter(has_text="保存")
        expect(save_btn).to_be_visible()

    # ══════════════════════════════════════════════════════
    # L1: 表单校验
    # ══════════════════════════════════════════════════════

    def test_empty_fields_show_validation(self, page: Page):
        """空字段提交应显示校验错误提示"""
        _open_create_modal(page)
        save_btn = page.locator(".ant-modal .ant-btn-primary").filter(has_text="保存")
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
        """标题输入超过 30 字符应提示"""
        _open_create_modal(page)
        title_input = _get_field(page, "能力标题").first
        title_input.fill("a" * 31)
        _get_field(page, "英文名").first.click()
        err = page.locator(".ant-form-item").filter(has_text="能力标题").locator(".ant-form-item-explain-error")
        expect(err).to_be_visible()

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
        cancel_btn = page.locator(".ant-modal .ant-btn").filter(has_text="取消")
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
        page.locator(".ant-modal .ant-btn").filter(has_text="取消").click()
        real_errors = []
        for err in self._console_errors:
            if not any(p in err for p in known_patterns):
                real_errors.append(err)
        assert len(real_errors) == 0, f"页面存在未预期的 console error: {real_errors}"
