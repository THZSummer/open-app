import re
import pytest

pytest.importorskip("playwright")

from playwright.sync_api import Page, expect
from .helpers import (
    wait_for_table_ready,
    get_rows,
    delete_ability,
    create_ability,
    update_ability,
    list_abilities,
    upload_icon,
)


PRESEEDED_AT = 1


def _wait_for_modal_ready(page):
    page.wait_for_selector(".ant-modal", timeout=10000)
    page.wait_for_selector(".ant-modal .ant-form", timeout=5000)


def _get_field(page, label):
    return page.locator(".ant-form-item").filter(has_text=label).locator("input, textarea")


class TestAbilityAdminEditPage:
    @pytest.fixture(autouse=True)
    def setup(self, page: Page, ability_admin_url: str):
        self._console_errors = []
        page.on("console", lambda msg: self._console_errors.append(msg.text) if msg.type == "error" else None)
        page.goto(ability_admin_url)
        page.wait_for_load_state("networkidle")
        page.wait_for_selector(".ant-table-row", timeout=10000)

    def test_edit_button_exists(self, page: Page):
        edit_btn = get_rows(page).first.locator("a", has_text="编辑")
        expect(edit_btn).to_be_visible()

    def test_open_edit_modal(self, page: Page):
        edit_btn = get_rows(page).first.locator("a", has_text="编辑")
        edit_btn.click()
        _wait_for_modal_ready(page)
        modal_title = page.locator(".ant-modal-title")
        expect(modal_title).to_have_text("编辑能力")

    def test_form_prefills_data(self, page: Page):
        records = list_abilities().get("data", [])
        edit_btn = get_rows(page).first.locator("a", has_text="编辑")
        edit_btn.click()
        _wait_for_modal_ready(page)
        first_record = records[0]
        title_input = _get_field(page, "能力标题").first
        expect(title_input).to_have_value(first_record["nameCn"])
        type_input = page.locator(".ant-form-item").filter(has_text="能力类型编码").locator("input").first
        assert type_input.is_disabled(), "能力类型编码输入框应禁用"

    def test_edit_success(self, page: Page):
        records = list_abilities().get("data", [])
        original_name = None
        original_entry_url = None
        original_route_path = None
        for r in records:
            if r["abilityType"] == PRESEEDED_AT:
                original_name = r["nameCn"]
                original_entry_url = r.get("entryUrl") or ""
                original_route_path = r.get("routePath") or ""
                break
        assert original_name is not None, f"未找到预置记录 abilityType={PRESEEDED_AT}"
        update_ability(PRESEEDED_AT, nameCn=original_name,
                       entryUrl="https://e2e-edit.example.com", routePath="/e2e-edit")
        try:
            page.reload()
            wait_for_table_ready(page)
            new_name = "E2E编辑测试-已修改"
            row = page.locator("tr").filter(has_text=original_name)
            row.locator("a", has_text="编辑").click()
            _wait_for_modal_ready(page)
            title_input = _get_field(page, "能力标题").first
            title_input.fill(new_name)
            save_btn = page.locator(".ant-modal-footer .ant-btn-primary").first
            save_btn.click()
            msg = page.locator(".ant-message-notice").filter(has_text="能力更新成功")
            expect(msg).to_be_visible(timeout=10000)
            expect(page.locator(".ant-modal-title").filter(has_text="编辑能力")).not_to_be_visible(timeout=5000)
            wait_for_table_ready(page)
            updated_records = list_abilities().get("data", [])
            updated_name = None
            for r in updated_records:
                if r["abilityType"] == PRESEEDED_AT:
                    updated_name = r["nameCn"]
                    break
            assert updated_name == new_name, f"列表未更新: 期望 {new_name}, 实际 {updated_name}"
        finally:
            update_ability(PRESEEDED_AT, nameCn=original_name,
                           entryUrl=original_entry_url or None,
                           routePath=original_route_path or None)

    def test_edit_non_existent(self, page: Page):
        batch_id, _ = upload_icon()
        create_ability(205, nameCn="E2E编辑不存在", nameEn="e2e-edit-nonexist",
                       iconBatchId=batch_id)
        page.reload()
        wait_for_table_ready(page)
        page.locator(".ant-pagination-options .ant-select-selector").click()
        page.wait_for_selector(".ant-select-dropdown", timeout=3000)
        page.locator(".ant-select-dropdown .ant-select-item-option").filter(has_text="50").click()
        wait_for_table_ready(page)
        delete_ability(205)
        row = page.locator("tr").filter(has_text="E2E编辑不存在")
        edit_btn = row.locator("a", has_text="编辑")
        edit_btn.click()
        _wait_for_modal_ready(page)
        title_input = _get_field(page, "能力标题").first
        title_input.fill("不应保存")
        save_btn = page.locator(".ant-modal-footer .ant-btn-primary").first
        save_btn.click()
        msg = page.locator(".ant-message-notice").filter(has_text="能力记录不存在")
        expect(msg).to_be_visible(timeout=10000)

    def test_cancel_preserves_data(self, page: Page):
        first_row = get_rows(page).first
        original_text = first_row.inner_text()
        edit_btn = first_row.locator("a", has_text="编辑")
        edit_btn.click()
        _wait_for_modal_ready(page)
        title_input = _get_field(page, "能力标题").first
        title_input.fill("不应保存的值")
        cancel_btn = page.get_by_role("button", name="取 消")
        cancel_btn.click()
        expect(page.locator(".ant-modal")).not_to_be_visible()
        current_text = get_rows(page).first.inner_text()
        assert "不应保存的值" not in current_text, "取消编辑后列表数据不应变更"
