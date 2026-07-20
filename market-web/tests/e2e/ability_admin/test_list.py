#!/usr/bin/env python3
"""
能力目录管理列表页 — Playwright E2E 测试

测试覆盖：
  L1: 页面加载 + 表格列字段展示
  L2: 搜索交互 + 分页控件

运行：
  pip install pytest-playwright pytest-html -q
  playwright install chromium
  pytest tests/e2e/ability_admin/test_list.py -v
"""
import pytest

# 检查 playwright 是否可用
pytest.importorskip("playwright")

from playwright.sync_api import Page, expect


class TestAbilityAdminListPage:
    """能力目录管理列表页 — 核心功能测试"""

    @pytest.fixture(autouse=True)
    def setup_page(self, page: Page, ability_admin_url: str):
        """每个测试前导航到能力管理页面"""
        page.goto(ability_admin_url)
        page.wait_for_load_state("networkidle")

    def test_page_title(self, page: Page):
        """L1: 页面标题渲染"""
        title_el = page.locator("text=能力目录管理")
        expect(title_el).to_be_visible()
        print("[PASS] 页面标题「能力目录管理」可见")

    def test_table_columns_present(self, page: Page):
        """L1: 表格列字段完整"""
        expected_columns = [
            "能力编码",
            "中文名",
            "英文名",
            "中文描述",
            "英文描述",
            "图标",
            "示意图",
            "排序号",
            "加载类型",
            "进入地址",
            "路由路径",
            "别名",
            "隐藏",
            "需版本发布",
            "创建时间",
            "更新人",
            "更新时间",
        ]
        for col in expected_columns:
            cell = page.locator(f"th:has-text('{col}')")
            expect(cell).to_be_visible()
            print(f"  [OK] 列「{col}」存在")

    def test_search_keyword_input(self, page: Page):
        """L1: 关键词搜索框可用"""
        search_input = page.locator("input[placeholder='搜索中文名/英文名']")
        expect(search_input).to_be_visible()
        search_input.fill("测试能力")
        page.locator("button:has-text('搜索')").click()
        page.wait_for_load_state("networkidle")
        print("[PASS] 关键词搜索交互正常")

    def test_search_reset(self, page: Page):
        """L1: 重置按钮可用"""
        search_input = page.locator("input[placeholder='搜索中文名/英文名']")
        search_input.fill("测试能力")
        page.locator("button:has-text('重置')").click()
        page.wait_for_load_state("networkidle")
        actual_value = search_input.input_value()
        assert actual_value == "", f"重置后搜索框应为空，实际: '{actual_value}'"
        print(f"[PASS] 重置后搜索框为空")

    def test_sort_field_select(self, page: Page):
        """L2: 排序字段下拉可用"""
        sort_select = page.locator("text=排序字段").locator("..").locator(".ant-select")
        expect(sort_select.first).to_be_visible()
        sort_select.first.click()
        page.locator(".ant-select-item-option:has-text('创建时间')").click()
        page.locator("button:has-text('搜索')").click()
        page.wait_for_load_state("networkidle")
        print("[PASS] 排序字段切换正常")

    def test_pagination_controls(self, page: Page):
        """L2: 分页控件存在并可用"""
        # 验证分页组件渲染
        pagination = page.locator(".ant-pagination")
        expect(pagination).to_be_visible()
        print("[PASS] 分页控件可见")

        # 验证分页信息
        total_info = page.locator("text=共")
        expect(total_info).to_be_visible()
        print("[PASS] 分页信息（共 X 条）可见")
