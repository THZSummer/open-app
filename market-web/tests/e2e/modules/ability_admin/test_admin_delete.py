import pytest

pytest.importorskip("playwright")

from playwright.sync_api import Page, expect
from .helpers import (
    wait_for_table_ready,
    get_rows,
    delete_ability,
    create_ability,
    upload_icon,
    list_abilities,
)


TEST_AT_DELETE = 206


class TestAbilityAdminDeletePage:
    @pytest.fixture(autouse=True)
    def setup(self, page: Page, ability_admin_url: str):
        self._console_errors = []
        page.on("console", lambda msg: self._console_errors.append(msg.text) if msg.type == "error" else None)
        page.goto(ability_admin_url)
        page.wait_for_load_state("networkidle")
        page.wait_for_selector(".ant-table-row", timeout=10000)

    def test_delete_button_exists(self, page: Page):
        delete_btn = get_rows(page).first.locator("a", has_text="删除")
        expect(delete_btn).to_be_visible()

    def test_delete_cancel(self, page: Page):
        rows_before = get_rows(page).count()
        delete_btn = get_rows(page).first.locator("a", has_text="删除")
        delete_btn.click()
        page.wait_for_selector(".ant-modal-confirm", timeout=5000)
        cancel_btn = page.get_by_role("button", name="取 消")
        cancel_btn.click()
        expect(page.locator(".ant-modal-confirm")).not_to_be_visible()
        rows_after = get_rows(page).count()
        assert rows_after == rows_before, f"取消删除后行数变化：{rows_before} → {rows_after}"

    def test_delete_confirm(self, page: Page):
        batch_id, _ = upload_icon()
        resp = create_ability(TEST_AT_DELETE, nameCn="E2E删除测试", nameEn="e2e-delete-test",
                              iconBatchId=batch_id)
        assert resp.get("code") == "200", f"创建失败: {resp}"
        records = list_abilities().get("data", [])
        record_id = None
        for r in records:
            if r["abilityType"] == TEST_AT_DELETE:
                record_id = r.get("id")
                break
        assert record_id is not None, "创建后未找到记录 id"
        page.reload()
        wait_for_table_ready(page)
        page.locator(".ant-pagination-options .ant-select-selector").click()
        page.wait_for_selector(".ant-select-dropdown", timeout=3000)
        page.locator(".ant-select-dropdown .ant-select-item-option").filter(has_text="50").click()
        wait_for_table_ready(page)
        row = page.locator("tr").filter(has_text="E2E删除测试")
        delete_btn = row.locator("a", has_text="删除")
        delete_btn.click()
        page.wait_for_selector(".ant-modal-confirm", timeout=5000)
        ok_btn = page.locator(".ant-modal-confirm .ant-btn-dangerous")
        ok_btn.click()
        msg = page.locator(".ant-message-notice").filter(has_text="删除成功")
        expect(msg).to_be_visible(timeout=10000)

    def test_delete_non_existent(self, page: Page):
        batch_id, _ = upload_icon()
        resp = create_ability(207, nameCn="E2E删除不存在", nameEn="e2e-delete-nonexist",
                              iconBatchId=batch_id)
        assert resp.get("code") == "200", f"创建失败: {resp}"
        records = list_abilities().get("data", [])
        record_id = None
        for r in records:
            if r["abilityType"] == 207:
                record_id = r.get("id")
                break
        assert record_id is not None, "创建后未找到记录 id"
        page.reload()
        wait_for_table_ready(page)
        page.locator(".ant-pagination-options .ant-select-selector").click()
        page.wait_for_selector(".ant-select-dropdown", timeout=3000)
        page.locator(".ant-select-dropdown .ant-select-item-option").filter(has_text="50").click()
        wait_for_table_ready(page)
        delete_ability(record_id)
        row = page.locator("tr").filter(has_text="E2E删除不存在")
        delete_btn = row.locator("a", has_text="删除")
        delete_btn.click()
        page.wait_for_selector(".ant-modal-confirm", timeout=5000)
        ok_btn = page.locator(".ant-modal-confirm .ant-btn-dangerous")
        ok_btn.click()
        msg = page.locator(".ant-message-notice").filter(has_text="能力记录不存在")
        expect(msg).to_be_visible(timeout=10000)
