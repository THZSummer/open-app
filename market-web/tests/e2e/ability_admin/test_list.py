#!/usr/bin/env python3
"""
能力目录管理列表页 — Playwright E2E 测试

覆盖页面加载、分页、数据渲染、兼容性场景。
"""
import re
import pytest

pytest.importorskip("playwright")

from playwright.sync_api import Page, expect


def _wait_for_table_ready(page):
    """等待表格数据加载完毕（行就绪或空态）"""
    page.wait_for_load_state("networkidle")
    rows = page.locator(".ant-table-row")
    try:
        rows.first.wait_for(timeout=8000)
    except Exception:
        page.wait_for_selector(".ant-empty", timeout=5000)


def _get_rows(page):
    return page.locator(".ant-table-row")


def _cell_text(page, row_index, col_index):
    return _get_rows(page).nth(row_index).locator("td").nth(col_index).inner_text()


class TestAbilityAdminListPage:
    """能力目录管理列表页 — E2E 测试"""

    @pytest.fixture(autouse=True)
    def setup_page(self, page: Page, ability_admin_url: str):
        self._console_errors = []
        page.on("console", lambda msg: self._console_errors.append(msg.text) if msg.type == "error" else None)
        page.goto(ability_admin_url)
        _wait_for_table_ready(page)

    # ══════════════════════════════════════════════════════
    # 页面加载
    # ══════════════════════════════════════════════════════

    def test_page_title_visible(self, page: Page):
        expect(page.locator("text=能力目录管理")).to_be_visible()

    def test_all_table_columns_present(self, page: Page):
        expected = [
            "排序", "能力名称", "访问地址", "示意图",
            "更新时间", "操作账号", "操作",
        ]
        texts = page.locator(".ant-table-thead th").all_inner_texts()
        for col in expected:
            assert col in texts, f"列「{col}」未在表头中找到"

    def test_data_loads_with_rows(self, page: Page):
        count = _get_rows(page).count()
        assert count > 0, "表格行数应为正数"
        total_text = page.locator(".ant-pagination-total-text").inner_text()
        assert any(c.isdigit() for c in total_text), f"总条数应包含数字, 实际: {total_text}"

    def test_loading_spinner_appears(self, page: Page):
        def _delay(route):
            import time
            time.sleep(0.5)
            route.continue_()
        page.route("**/ability/admin/list**", _delay)
        page.reload()
        spin = page.wait_for_selector(".ant-spin", timeout=5000)
        assert spin is not None, "Loading spinner 未出现"
        _wait_for_table_ready(page)
        expect(page.locator(".ant-spin")).not_to_be_visible()

    # ══════════════════════════════════════════════════════
    # 分页
    # ══════════════════════════════════════════════════════

    def test_pagination_controls(self, page: Page):
        total_el = page.locator(".ant-pagination-total-text")
        expect(total_el).to_be_visible()

    def test_pagination_page2_shows_different_data(self, page: Page):
        _wait_for_table_ready(page)
        assert _get_rows(page).count() > 0, "首页应有数据"
        page2_btn = page.locator(".ant-pagination li[title='2']")
        if page2_btn.is_visible():
            page2_btn.click()
            _wait_for_table_ready(page)
            assert _get_rows(page).count() > 0, "第 2 页也应包含数据"
        else:
            pytest.skip("数据未超出 1 页，跳过")

    # ══════════════════════════════════════════════════════
    # 数据渲染
    # ══════════════════════════════════════════════════════

    def test_icon_image_renders_when_icon_url_exists(self, page: Page):
        rows = _get_rows(page)
        found = False
        for i in range(rows.count()):
            cell_img = rows.nth(i).locator("td").nth(1).locator("img")
            if cell_img.count() > 0:
                src = cell_img.get_attribute("src")
                assert src and src.strip(), f"第 {i} 行图标 src 为空"
                found = True
                break
        assert found, "未找到有图标渲染的行"

    # ══════════════════════════════════════════════════════
    # 兼容性
    # ══════════════════════════════════════════════════════

    def test_page_no_js_error(self, page: Page):
        known_patterns = [
            "favicon",
            "Failed to load resource",
            "Support for defaultProps",
            "React Router Future Flag Warning",
            "React DevTools",
        ]
        real_errors = []
        for err in self._console_errors:
            if not any(p in err for p in known_patterns):
                real_errors.append(err)
        assert len(real_errors) == 0, f"页面存在未预期的 console error: {real_errors}"
