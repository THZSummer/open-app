#!/usr/bin/env python3
"""
能力目录管理编辑页 — Playwright E2E 测试

覆盖：
  L1: 编辑按钮点击、弹窗渲染、字段回填、提交更新
  L2: 取消编辑数据不变、abilityType 只读
"""
import json
import re
import pytest

pytest.importorskip("playwright")

from playwright.sync_api import Page, expect


def _wait_for_modal_ready(page: Page):
    """等待编辑弹窗完全打开"""
    page.wait_for_selector(".ant-modal", timeout=10000)
    page.wait_for_selector(".ant-modal .ant-form", timeout=5000)


def _mock_list_data(page: Page, records=None):
    """Mock 列表接口，返回固定数据用于测试"""
    if records is None:
        records = [
            {
                "abilityType": 100,
                "nameCn": "测试能力编辑",
                "nameEn": "test-edit",
                "descCn": "这是一个测试能力用于编辑",
                "descEn": "This is a test ability for editing",
                "iconUrl": "/ability-files/test-icon.png",
                "exampleDiagramUrl": "/ability-files/test-diagram.png",
                "orderNum": 1,
                "entryUrl": "https://example.com",
                "routePath": "/test",
                "aliasName": "test-alias",
                "loadType": 1,
                "updateTime": "2026-07-20 10:00:00",
                "updateBy": "admin",
            },
            {
                "abilityType": 101,
                "nameCn": "测试能力2",
                "nameEn": "test-ability-2",
                "descCn": "第二个测试能力的描述",
                "descEn": "Second test ability description",
                "iconUrl": "/ability-files/test-icon2.png",
                "orderNum": 2,
                "entryUrl": "https://example2.com",
                "routePath": "/test2",
                "loadType": 1,
                "updateTime": "2026-07-20 11:00:00",
                "updateBy": "admin",
            },
        ]
    body = json.dumps({"code": "200", "data": records, "page": {"total": len(records)}})
    page.route(re.compile(r"/ability/admin/list"), lambda route: route.fulfill(
        status=200, content_type="application/json",
        body=body
    ))


def _get_field(page, label):
    """根据 label 获取表单输入项"""
    return page.locator(".ant-form-item").filter(has_text=label).locator("input, textarea")


class TestAbilityAdminEditPage:
    """能力目录管理编辑页 — E2E 测试"""

    @pytest.fixture(autouse=True)
    def setup_page(self, page: Page, ability_admin_url: str):
        self._console_errors = []
        page.on("console", lambda msg: self._console_errors.append(msg.text) if msg.type == "error" else None)
        _mock_list_data(page)
        page.goto(ability_admin_url)
        page.wait_for_load_state("networkidle")
        page.wait_for_selector(".ant-table-row", timeout=10000)

    # ══════════════════════════════════════════════════════
    # L1: 核心功能 — 编辑按钮与弹窗
    # ══════════════════════════════════════════════════════

    def test_edit_button_visible(self, page: Page):
        """每条数据行应有「编辑」按钮"""
        edit_btn = page.locator(".ant-table-row").first.locator("a", has_text="编辑")
        expect(edit_btn).to_be_visible()

    def test_click_edit_opens_modal(self, page: Page):
        """点击编辑按钮应打开弹窗，标题为「编辑能力」"""
        edit_btn = page.locator(".ant-table-row").first.locator("a", has_text="编辑")
        edit_btn.click()
        _wait_for_modal_ready(page)
        modal_title = page.locator(".ant-modal-title")
        expect(modal_title).to_have_text("编辑能力")

    def test_edit_form_fields_prefilled(self, page: Page):
        """编辑弹窗应回填已有数据"""
        edit_btn = page.locator(".ant-table-row").first.locator("a", has_text="编辑")
        edit_btn.click()
        _wait_for_modal_ready(page)

        # 验证标题回填
        title_input = _get_field(page, "能力标题").first
        expect(title_input).to_have_value("测试能力编辑")

        # 验证英文名回填
        en_input = _get_field(page, "英文名").first
        expect(en_input).to_have_value("test-edit")

        # 验证描述回填
        desc_input = page.locator(".ant-form-item").filter(has_text="能力描述").locator("textarea").first
        expect(desc_input).to_have_value("这是一个测试能力用于编辑")

        # 验证访问地址回填
        url_input = _get_field(page, "访问地址").first
        expect(url_input).to_have_value("https://example.com")

    def test_edit_modal_has_save_button(self, page: Page):
        """编辑弹窗应包含「保存」按钮"""
        edit_btn = page.locator(".ant-table-row").first.locator("a", has_text="编辑")
        edit_btn.click()
        _wait_for_modal_ready(page)
        save_btn = page.locator(".ant-modal-footer .ant-btn-primary").first
        expect(save_btn).to_be_visible()

    def test_edit_modal_close_on_cancel(self, page: Page):
        """点击取消应关闭编辑弹窗"""
        edit_btn = page.locator(".ant-table-row").first.locator("a", has_text="编辑")
        edit_btn.click()
        _wait_for_modal_ready(page)
        cancel_btn = page.get_by_role("button", name="取 消")
        cancel_btn.click()
        expect(page.locator(".ant-modal")).not_to_be_visible()

    # ══════════════════════════════════════════════════════
    # L2: 编辑交互 — abilityType 只读 & 修改提交
    # ══════════════════════════════════════════════════════

    def test_ability_type_disabled(self, page: Page):
        """abilityType 字段应禁用（只读）"""
        edit_btn = page.locator(".ant-table-row").first.locator("a", has_text="编辑")
        edit_btn.click()
        _wait_for_modal_ready(page)
        type_input = page.locator(".ant-form-item").filter(has_text="能力类型编码").locator("input").first
        assert type_input.is_disabled(), "abilityType 输入框应禁用"

    def test_edit_name_and_submit(self, page: Page):
        """编辑中文名后提交，应显示成功提示并关闭弹窗"""
        # Mock 更新接口
        page.route(re.compile(r"/ability/admin/\d+$"), lambda route: route.fulfill(
            status=200, content_type="application/json",
            body='{"code":"200","messageZh":"能力更新成功"}'
        ) if route.request.method == "PUT" else route.continue_())

        edit_btn = page.locator(".ant-table-row").first.locator("a", has_text="编辑")
        edit_btn.click()
        _wait_for_modal_ready(page)

        # 修改标题
        title_input = _get_field(page, "能力标题").first
        title_input.fill("测试能力编辑-已修改")

        # 提交
        save_btn = page.locator(".ant-modal-footer .ant-btn-primary").first
        save_btn.click()

        # 验证成功提示
        msg = page.locator(".ant-message-notice").filter(has_text="能力更新成功")
        expect(msg).to_be_visible(timeout=10000)

        # 弹窗应关闭
        expect(page.locator(".ant-modal-title").filter(has_text="编辑能力")).not_to_be_visible(timeout=5000)

    def test_cancel_edit_no_change(self, page: Page):
        """取消编辑，列表数据不变"""
        # 获取原始列表第一条文本
        first_row = page.locator(".ant-table-row").first
        original_text = first_row.inner_text()

        edit_btn = first_row.locator("a", has_text="编辑")
        edit_btn.click()
        _wait_for_modal_ready(page)

        # 修改标题但不提交
        title_input = _get_field(page, "能力标题").first
        title_input.fill("不应保存的值")

        # 取消
        cancel_btn = page.get_by_role("button", name="取 消")
        cancel_btn.click()
        expect(page.locator(".ant-modal")).not_to_be_visible()

        # 验证第一条记录未变更
        current_text = page.locator(".ant-table-row").first.inner_text()
        assert "不应保存的值" not in current_text, "取消编辑后列表数据不应变更"

    def test_icon_preview_shown(self, page: Page):
        """编辑弹窗应显示已有图标预览"""
        edit_btn = page.locator(".ant-table-row").first.locator("a", has_text="编辑")
        edit_btn.click()
        _wait_for_modal_ready(page)

        # 验证图标预览图片
        icon_img = page.locator('img[alt="图标预览"]')
        expect(icon_img).to_be_visible(timeout=5000)
        expect(icon_img).to_have_attribute("src", "/ability-files/test-icon.png")

    def test_diagram_preview_shown(self, page: Page):
        """编辑弹窗应显示已有示意图预览"""
        edit_btn = page.locator(".ant-table-row").first.locator("a", has_text="编辑")
        edit_btn.click()
        _wait_for_modal_ready(page)

        # 验证示意图预览图片
        diagram_img = page.locator('img[alt="示意图预览"]')
        expect(diagram_img).to_be_visible(timeout=5000)
        expect(diagram_img).to_have_attribute("src", "/ability-files/test-diagram.png")

    def test_page_no_js_error_in_edit_modal(self, page: Page):
        """打开编辑弹窗过程中无 JS 错误"""
        known_patterns = [
            "favicon",
            "Failed to load resource",
            "Support for defaultProps",
            "React Router Future Flag Warning",
            "React DevTools",
        ]
        edit_btn = page.locator(".ant-table-row").first.locator("a", has_text="编辑")
        edit_btn.click()
        _wait_for_modal_ready(page)
        cancel_btn = page.get_by_role("button", name="取 消")
        cancel_btn.click()

        real_errors = []
        for err in self._console_errors:
            if not any(p in err for p in known_patterns):
                real_errors.append(err)
        assert len(real_errors) == 0, f"页面存在未预期的 console error: {real_errors}"
