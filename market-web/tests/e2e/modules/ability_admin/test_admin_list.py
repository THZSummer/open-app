import pytest

pytest.importorskip("playwright")

from playwright.sync_api import Page, expect
from .helpers import wait_for_table_ready, get_rows


class TestAbilityAdminListPage:
    @pytest.fixture(autouse=True)
    def setup(self, page: Page, ability_admin_url: str):
        self._console_errors = []
        page.on("console", lambda msg: self._console_errors.append(msg.text) if msg.type == "error" else None)
        page.goto(ability_admin_url)
        wait_for_table_ready(page)

    def test_page_loads(self, page: Page):
        expect(page.locator("text=能力目录管理")).to_be_visible()

    def test_table_columns(self, page: Page):
        expected = ["排序", "能力名称", "访问地址", "示意图", "更新时间", "操作账号", "操作"]
        texts = page.locator(".ant-table-thead th").all_inner_texts()
        for col in expected:
            assert col in texts, f"列「{col}」未在表头中找到"

    def test_data_rows_exist(self, page: Page):
        count = get_rows(page).count()
        assert count > 0, "表格行数应为正数"

    def test_pagination_visible(self, page: Page):
        total_el = page.locator(".ant-pagination-total-text")
        expect(total_el).to_be_visible()
        total_text = total_el.inner_text()
        assert any(c.isdigit() for c in total_text), f"总条数应包含数字, 实际: {total_text}"

    def test_pagination_page2(self, page: Page):
        wait_for_table_ready(page)
        assert get_rows(page).count() > 0, "首页应有数据"
        page2_btn = page.locator(".ant-pagination li[title='2']")
        if page2_btn.is_visible():
            page2_btn.click()
            wait_for_table_ready(page)
            assert get_rows(page).count() > 0, "第 2 页也应包含数据"
        else:
            pytest.skip("数据未超出 1 页，跳过")

    def test_pagination_page_size(self, page: Page):
        page.locator(".ant-pagination-options .ant-select-selector").click()
        page.wait_for_selector(".ant-select-dropdown", timeout=3000)
        page.locator(".ant-select-dropdown .ant-select-item-option").filter(has_text="50").click()
        wait_for_table_ready(page)
        rows = get_rows(page)
        assert rows.count() > 0

    def test_icon_renders(self, page: Page):
        rows = get_rows(page)
        found = False
        for i in range(rows.count()):
            img = rows.nth(i).locator("td").nth(1).locator("img")
            if img.count() > 0:
                src = img.get_attribute("src")
                assert src and src.strip(), f"第 {i} 行图标 src 为空"
                found = True
                break
        assert found, "未找到有图标渲染的行"

    def test_no_js_error(self, page: Page):
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
