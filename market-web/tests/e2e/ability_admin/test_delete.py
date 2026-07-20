#!/usr/bin/env python3
"""
能力目录管理删除操作 — Playwright E2E 测试

覆盖：
  L1: 删除按钮可见、确认弹窗弹出、确认删除、取消删除
  L2: 有订阅时删除被阻止、JS 错误检查
"""
import json
import re
import pytest

pytest.importorskip("playwright")

from playwright.sync_api import Page, expect


def _mock_list_data(page: Page, records=None):
    """Mock 列表接口，返回固定数据用于测试"""
    if records is None:
        records = [
            {
                "abilityType": 100,
                "nameCn": "测试可删除能力",
                "nameEn": "test-deletable",
                "descCn": "此能力可用于删除测试",
                "descEn": "This ability is for delete testing",
                "iconUrl": "/ability-files/test-icon.png",
                "orderNum": 1,
                "entryUrl": "https://example.com",
                "updateTime": "2026-07-20 10:00:00",
                "updateBy": "admin",
            },
            {
                "abilityType": 200,
                "nameCn": "有订阅的能力",
                "nameEn": "test-subscribed",
                "descCn": "此能力有订阅，删除应被阻止",
                "descEn": "This ability has subscriptions",
                "iconUrl": "/ability-files/test-icon2.png",
                "orderNum": 2,
                "entryUrl": "https://example2.com",
                "updateTime": "2026-07-20 11:00:00",
                "updateBy": "admin",
            },
        ]
    body = json.dumps({"code": "200", "data": records, "page": {"total": len(records)}})
    page.route(re.compile(r"/ability/admin/list"), lambda route: route.fulfill(
        status=200, content_type="application/json",
        body=body
    ))


class TestAbilityAdminDeletePage:
    """能力目录管理删除操作 — E2E 测试"""

    @pytest.fixture(autouse=True)
    def setup_page(self, page: Page, ability_admin_url: str):
        self._console_errors = []
        page.on("console", lambda msg: self._console_errors.append(msg.text) if msg.type == "error" else None)
        _mock_list_data(page)
        page.goto(ability_admin_url)
        page.wait_for_load_state("networkidle")
        page.wait_for_selector(".ant-table-row", timeout=10000)

    # ══════════════════════════════════════════════════════
    # L1: 核心功能 — 删除按钮与确认弹窗
    # ══════════════════════════════════════════════════════

    def test_delete_button_visible(self, page: Page):
        """每条数据行应有「删除」按钮"""
        delete_btn = page.locator(".ant-table-row").first.locator("a", has_text="删除")
        expect(delete_btn).to_be_visible()

    def test_click_delete_opens_confirm_modal(self, page: Page):
        """点击删除按钮应弹出确认弹窗"""
        delete_btn = page.locator(".ant-table-row").first.locator("a", has_text="删除")
        delete_btn.click()
        # 确认弹窗应包含标题和确认/取消按钮
        modal = page.locator(".ant-modal-confirm")
        expect(modal).to_be_visible()
        expect(modal.locator(".ant-modal-confirm-title")).to_have_text("确认删除")
        # 应包含确认删除按钮
        ok_btn = modal.locator(".ant-btn-dangerous")
        expect(ok_btn).to_have_text("确认删除")

    def test_confirm_delete_calls_api_and_shows_success(self, page: Page):
        """确认删除，应调用 DELETE 接口并显示成功提示"""
        # Mock DELETE 接口
        page.route(re.compile(r"/ability/admin/\d+$"), lambda route: route.fulfill(
            status=200, content_type="application/json",
            body='{"code":"200","messageZh":"删除成功"}'
        ) if route.request.method == "DELETE" else route.continue_())

        delete_btn = page.locator(".ant-table-row").first.locator("a", has_text="删除")
        delete_btn.click()
        page.wait_for_selector(".ant-modal-confirm", timeout=5000)

        ok_btn = page.locator(".ant-modal-confirm .ant-btn-dangerous")
        ok_btn.click()

        # 验证成功提示
        msg = page.locator(".ant-message-notice").filter(has_text="删除成功")
        expect(msg).to_be_visible(timeout=10000)

    def test_cancel_delete_does_nothing(self, page: Page):
        """取消删除，弹窗关闭且数据不变"""
        delete_btn = page.locator(".ant-table-row").first.locator("a", has_text="删除")
        delete_btn.click()
        page.wait_for_selector(".ant-modal-confirm", timeout=5000)

        # 点击取消按钮
        cancel_btn = page.locator(".ant-modal-confirm .ant-btn").filter(has_text="取消")
        cancel_btn.click()
        expect(page.locator(".ant-modal-confirm")).not_to_be_visible()

        # 列表行数应保持不变
        rows = page.locator(".ant-table-row")
        expect(rows).to_have_count(2)

    # ══════════════════════════════════════════════════════
    # L2: 边界场景 — 订阅冲突 & JS 错误
    # ══════════════════════════════════════════════════════

    def test_delete_with_subscriptions_error_shown(self, page: Page):
        """有订阅时删除，应展示订阅数量错误提示"""
        # Mock DELETE 接口返回 400 含 subscribeCount
        def handle_delete(route):
            body = json.dumps({
                "code": "400",
                "messageZh": "该能力已被 3 个应用订阅，无法删除",
                "data": {"subscribeCount": 3},
            })
            route.fulfill(status=200, content_type="application/json", body=body)

        page.route(re.compile(r"/ability/admin/\d+$"), lambda route: handle_delete(route)
                   if route.request.method == "DELETE" else route.continue_())

        # 点击第二条数据（有订阅的能力）的删除按钮
        delete_btn = page.locator(".ant-table-row").nth(1).locator("a", has_text="删除")
        delete_btn.click()
        page.wait_for_selector(".ant-modal-confirm", timeout=5000)

        ok_btn = page.locator(".ant-modal-confirm .ant-btn-dangerous")
        ok_btn.click()

        # 验证错误提示包含订阅数量
        msg = page.locator(".ant-message-notice").filter(has_text="3 个应用订阅")
        expect(msg).to_be_visible(timeout=10000)

    def test_page_no_js_error_in_delete_flow(self, page: Page):
        """删除操作过程中无 JS 错误"""
        known_patterns = [
            "favicon",
            "Failed to load resource",
            "Support for defaultProps",
            "React Router Future Flag Warning",
            "React DevTools",
        ]

        # Mock DELETE 接口
        page.route(re.compile(r"/ability/admin/\d+$"), lambda route: route.fulfill(
            status=200, content_type="application/json",
            body='{"code":"200","messageZh":"删除成功"}'
        ) if route.request.method == "DELETE" else route.continue_())

        # 执行完整的删除流程
        delete_btn = page.locator(".ant-table-row").first.locator("a", has_text="删除")
        delete_btn.click()
        page.wait_for_selector(".ant-modal-confirm", timeout=5000)
        ok_btn = page.locator(".ant-modal-confirm .ant-btn-dangerous")
        ok_btn.click()
        page.wait_for_selector(".ant-message-notice", timeout=10000)

        real_errors = []
        for err in self._console_errors:
            if not any(p in err for p in known_patterns):
                real_errors.append(err)
        assert len(real_errors) == 0, f"页面存在未预期的 console error: {real_errors}"
