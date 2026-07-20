#!/usr/bin/env python3
"""
能力目录管理列表页 — Playwright E2E 全场景测试

覆盖 L1-L7 共 23 个用例：
  L1: 页面加载 (4)
  L2: 搜索 (5)
  L3: 排序 (4)
  L4: 分页 (4)
  L5: 重置 (2)
  L6: 数据渲染 (3)
  L7: 兼容性 (1)
"""
import re
import pytest

pytest.importorskip("playwright")

from playwright.sync_api import Page, expect


KEYWORD = "群置顶"


def _wait_for_table_ready(page):
    """等待表格数据加载完毕（行就绪或空态）"""
    page.wait_for_load_state("networkidle")
    page.wait_for_selector(".ant-table-row, .ant-empty", timeout=10000)


def _get_rows(page):
    return page.locator(".ant-table-row")


def _cell_text(page, row_index, col_index):
    return _get_rows(page).nth(row_index).locator("td").nth(col_index).inner_text()


def _search(page, keyword):
    inp = page.locator("input[placeholder='搜索中文名/英文名']")
    inp.fill(keyword)
    page.locator("button:has-text('搜索')").click()
    _wait_for_table_ready(page)


def _switch_sort(page, field_label, direction_label=None):
    sf = page.locator("text=排序字段").locator("..").locator(".ant-select")
    sf.first.click()
    page.locator(f".ant-select-item-option:has-text('{field_label}')").click()
    page.wait_for_timeout(300)
    if direction_label:
        sd = page.locator("text=排序方向").locator("..").locator(".ant-select")
        sd.first.click()
        page.locator(f".ant-select-item-option:has-text('{direction_label}')").click()
        page.wait_for_timeout(300)


class TestAbilityAdminListPage:
    """能力目录管理列表页 — 全场景 E2E 测试"""

    @pytest.fixture(autouse=True)
    def setup_page(self, page: Page, ability_admin_url: str):
        self._console_errors = []
        page.on("console", lambda msg: self._console_errors.append(msg.text) if msg.type == "error" else None)
        page.goto(ability_admin_url)
        _wait_for_table_ready(page)

    # ══════════════════════════════════════════════════════
    # L1 — 页面加载
    # ══════════════════════════════════════════════════════

    def test_page_title_visible(self, page: Page):
        expect(page.locator("text=能力目录管理")).to_be_visible()

    def test_all_table_columns_present(self, page: Page):
        expected = [
            "能力编码", "中文名", "英文名", "中文描述", "英文描述",
            "图标", "示意图", "排序号", "加载类型", "进入地址",
            "路由路径", "别名", "隐藏", "需版本发布", "创建时间",
            "更新人", "更新时间",
        ]
        texts = page.locator(".ant-table-thead th").all_inner_texts()
        for col in expected:
            assert col in texts, f"列「{col}」未在表头中找到"

    def test_data_loads_with_rows(self, page: Page):
        count = _get_rows(page).count()
        assert count > 0, "表格行数应为正数"
        total_text = page.locator(".ant-pagination-total-text").inner_text()
        assert "7" in total_text, f"总条数应显示 7, 实际: {total_text}"

    def test_loading_spinner_appears(self, page: Page):
        # 拦截 API 添加 500ms 延迟确保 spinner 被观察到
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
    # L2 — 搜索
    # ══════════════════════════════════════════════════════

    def test_search_by_keyword_cn_filters_results(self, page: Page):
        initial = _get_rows(page).count()
        _search(page, KEYWORD)
        rows = _get_rows(page)
        assert rows.count() <= initial
        assert rows.count() > 0, "关键词应匹配到至少 1 行数据"
        for i in range(rows.count()):
            cell = _cell_text(page, i, 1)
            assert KEYWORD in cell, f"第 {i} 行中文名不包含关键词: {cell}"

    def test_search_by_keyword_no_match_shows_empty(self, page: Page):
        _search(page, "ZZZZ_NO_MATCH_ZZZZ")
        assert _get_rows(page).count() == 0
        total_text = page.locator(".ant-pagination-total-text").inner_text()
        assert "0" in total_text, f"总条数应显示 0, 实际: {total_text}"

    def test_search_empty_keyword_preserves_data(self, page: Page):
        initial = _get_rows(page).count()
        assert initial > 0
        page.locator("button:has-text('搜索')").click()
        _wait_for_table_ready(page)
        assert _get_rows(page).count() == initial

    def test_search_enter_key_triggers_search(self, page: Page):
        initial = _get_rows(page).count()
        inp = page.locator("input[placeholder='搜索中文名/英文名']")
        inp.fill(KEYWORD)
        inp.press("Enter")
        _wait_for_table_ready(page)
        cnt = _get_rows(page).count()
        assert cnt <= initial
        assert cnt > 0, "Enter 搜索应返回结果"

    def test_search_clear_with_allow_clear(self, page: Page):
        initial = _get_rows(page).count()
        _search(page, KEYWORD)
        rows_filtered = _get_rows(page).count()
        assert rows_filtered <= initial
        inp = page.locator("input[placeholder='搜索中文名/英文名']")
        inp.fill("")
        page.locator("button:has-text('搜索')").click()
        _wait_for_table_ready(page)
        cnt = _get_rows(page).count()
        assert cnt == initial, f"清空搜索框后行数应恢复: 期望 {initial}, 实际 {cnt}"

    # ══════════════════════════════════════════════════════
    # L3 — 排序
    # ══════════════════════════════════════════════════════

    def test_sort_by_create_time_desc(self, page: Page):
        _switch_sort(page, "创建时间", "降序")
        page.locator("button:has-text('搜索')").click()
        _wait_for_table_ready(page)
        rows = _get_rows(page)
        count = rows.count()
        if count >= 2:
            first = _cell_text(page, 0, 14)
            last = _cell_text(page, count - 1, 14)
            assert first >= last, f"降序异常: first={first}, last={last}"

    def test_sort_by_order_num_asc(self, page: Page):
        page.locator("button:has-text('搜索')").click()
        _wait_for_table_ready(page)
        rows = _get_rows(page)
        count = rows.count()
        if count >= 2:
            first = int(_cell_text(page, 0, 7))
            last = int(_cell_text(page, count - 1, 7))
            assert first <= last, f"升序异常: first={first}, last={last}"

    def test_sort_direction_asc_then_desc(self, page: Page):
        page.locator("button:has-text('搜索')").click()
        _wait_for_table_ready(page)
        first_asc = _cell_text(page, 0, 7)
        _switch_sort(page, "排序号", "降序")
        page.locator("button:has-text('搜索')").click()
        _wait_for_table_ready(page)
        first_desc = _cell_text(page, 0, 7)
        assert first_asc != first_desc, "升降序首行排序号应不同"

    def test_sort_field_change_triggers_reorder(self, page: Page):
        page.locator("button:has-text('搜索')").click()
        _wait_for_table_ready(page)
        rows = _get_rows(page)
        n = min(5, rows.count())
        order_num_order = [_cell_text(page, i, 0) for i in range(n)]
        _switch_sort(page, "能力编码")
        page.locator("button:has-text('搜索')").click()
        _wait_for_table_ready(page)
        ability_order = [_cell_text(page, i, 0) for i in range(n)]
        assert order_num_order != ability_order, "切换排序字段后行顺序应改变"

    # ══════════════════════════════════════════════════════
    # L4 — 分页
    # ══════════════════════════════════════════════════════

    def test_pagination_total_matches(self, page: Page):
        total_el = page.locator(".ant-pagination-total-text")
        expect(total_el).to_be_visible()

    def test_pagination_page2_shows_different_data(self, page: Page):
        assert _get_rows(page).count() > 0
        page2_btn = page.locator(".ant-pagination li[title='2']")
        if page2_btn.is_visible():
            page2_btn.click()
            _wait_for_table_ready(page)
            assert _get_rows(page).count() == 0, "所有数据在第 1 页，第 2 页应为空"
        else:
            pytest.skip("数据未超出 1 页，跳过")

    def test_pagination_page_size_change(self, page: Page):
        ps = page.locator(".ant-pagination .ant-select")
        if ps.is_visible():
            ps.first.click()
            opt = page.locator(".ant-select-item-option:has-text('50')")
            if opt.is_visible():
                opt.click()
                _wait_for_table_ready(page)
        total_text = page.locator(".ant-pagination-total-text").inner_text()
        assert "7" in total_text

    def test_pagination_search_resets_to_page1(self, page: Page):
        page2_btn = page.locator(".ant-pagination li[title='2']")
        if page2_btn.is_visible():
            page2_btn.click()
            _wait_for_table_ready(page)
        _search(page, KEYWORD)
        active = page.locator(".ant-pagination li.ant-pagination-item-active")
        assert active.count() > 0, "搜索后分页应有 active 项"
        assert active.inner_text() == "1", "搜索后应回到第 1 页"

    # ══════════════════════════════════════════════════════
    # L5 — 重置
    # ══════════════════════════════════════════════════════

    def test_reset_clears_keyword_and_restores_data(self, page: Page):
        initial = _get_rows(page).count()
        _search(page, KEYWORD)
        assert _get_rows(page).count() < initial
        page.locator("button:has-text('重置')").click()
        _wait_for_table_ready(page)
        inp = page.locator("input[placeholder='搜索中文名/英文名']")
        assert inp.input_value() == "", "重置后搜索框应为空"
        assert _get_rows(page).count() == initial, \
            f"重置后行数应恢复: 期望 {initial}, 实际 {_get_rows(page).count()}"

    def test_reset_after_sort_restores_default_sort(self, page: Page):
        _switch_sort(page, "创建时间", "降序")
        page.locator("button:has-text('搜索')").click()
        _wait_for_table_ready(page)
        page.locator("button:has-text('重置')").click()
        _wait_for_table_ready(page)
        sf = page.locator("text=排序字段").locator("..").locator(".ant-select .ant-select-selection-item")
        expect(sf).to_contain_text("排序号")
        sd = page.locator("text=排序方向").locator("..").locator(".ant-select .ant-select-selection-item")
        expect(sd).to_contain_text("升序")

    # ══════════════════════════════════════════════════════
    # L6 — 数据渲染
    # ══════════════════════════════════════════════════════

    def test_icon_image_renders_when_icon_url_exists(self, page: Page):
        rows = _get_rows(page)
        found = False
        for i in range(rows.count()):
            cell_img = rows.nth(i).locator("td").nth(5).locator("img")
            if cell_img.count() > 0:
                src = cell_img.get_attribute("src")
                assert src and src.strip(), f"第 {i} 行图标 src 为空"
                found = True
                break
        assert found, "未找到有图标渲染的行"

    def test_load_type_displays_correct_text(self, page: Page):
        rows = _get_rows(page)
        for i in range(rows.count()):
            text = _cell_text(page, i, 8)
            assert text in ("路由加载", "微前端加载", "-"), \
                f"第 {i} 行加载类型异常: {text}"

    def test_hidden_column_shows_yes_or_no(self, page: Page):
        rows = _get_rows(page)
        for i in range(rows.count()):
            text = _cell_text(page, i, 12)
            assert text in ("是", "否"), f"第 {i} 行 hidden 列异常: {text}"

    # ══════════════════════════════════════════════════════
    # L7 — 兼容性
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
