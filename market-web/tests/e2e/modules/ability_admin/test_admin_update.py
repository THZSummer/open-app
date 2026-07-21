import re
import pytest

pytest.importorskip("playwright")

from playwright.sync_api import Page, expect
from .helpers import (
    wait_for_table_ready,
    get_rows,
    delete_ability,
    delete_ability_via_api,
    create_ability,
    create_ability_via_api,
    update_ability,
    list_abilities,
    upload_icon,
    get_next_ability_type,
    generate_png,
)


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
        ability_type = get_next_ability_type()
        icon_batch_id, _ = upload_icon()
        create_ability_via_api(
            ability_type=ability_type,
            name_cn="编辑成功测试-原始",
            icon_batch_id=icon_batch_id,
        )
        page.reload()
        wait_for_table_ready(page)
        page.locator(".ant-pagination-options .ant-select-selector").click()
        page.wait_for_selector(".ant-select-dropdown", timeout=3000)
        page.locator(".ant-select-dropdown .ant-select-item-option").filter(has_text="50").click()
        wait_for_table_ready(page)
        try:
            new_name = "编辑成功测试-已修改"
            row = page.locator("tr").filter(has_text="编辑成功测试-原始")
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
                if r["abilityType"] == ability_type:
                    updated_name = r["nameCn"]
                    break
            assert updated_name == new_name, f"列表未更新: 期望 {new_name}, 实际 {updated_name}"
        finally:
            delete_ability_via_api(ability_type)

    def test_edit_non_existent(self, page: Page):
        edit_nonexist_at = get_next_ability_type()
        batch_id, _ = upload_icon()
        resp = create_ability(edit_nonexist_at, nameCn="E2E编辑不存在", nameEn="e2e-edit-nonexist",
                              iconBatchId=batch_id)
        assert resp.get("code") == "200", f"创建失败: {resp}"
        records = list_abilities().get("data", [])
        record_id = None
        for r in records:
            if r["abilityType"] == edit_nonexist_at:
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


def test_edit_newly_created_record(page: Page, ability_admin_url: str):
    """编辑新建记录（大snowflake ID）→ 验证无精度丢失"""
    ability_type = str(get_next_ability_type())

    page.goto(ability_admin_url)
    page.wait_for_selector(".ant-table-row", timeout=10000)

    page.click("button:has-text('添加能力')")
    page.wait_for_selector(".ant-modal")

    page.fill("input#nameCn", f"大ID编辑测试-{ability_type}")
    page.fill("input#nameEn", "BigIDEdit")
    page.fill("textarea#descCn", "测试大ID编辑是否有精度问题")
    page.fill("textarea#descEn", "Test big ID edit")
    page.fill("input#entryUrl", "http://example.com")
    page.fill("input#routePath", "/big-id-edit")
    page.fill("input#aliasName", "bigidedit")
    page.fill("input#orderNum", "99")
    page.fill("input#abilityType", ability_type)

    generate_png("/tmp/test-icon-40.png", 40, 40)
    page.set_input_files("input[type='file']", "/tmp/test-icon-40.png")
    page.wait_for_timeout(2000)
    # 关闭图片裁剪弹窗
    page.locator(".img-crop-modal .ant-btn-primary").click()
    page.locator(".img-crop-modal").wait_for(state="hidden", timeout=5000)

    page.click(".ant-modal-footer .ant-btn-primary")
    page.wait_for_selector(".ant-message-success", timeout=10000)

    # 刷新表格查看新记录
    page.wait_for_selector(".ant-modal", state="hidden", timeout=10000)
    page.reload()
    wait_for_table_ready(page)
    page.locator(".ant-pagination-options .ant-select-selector").click()
    page.wait_for_selector(".ant-select-dropdown", timeout=3000)
    page.locator(".ant-select-dropdown .ant-select-item-option").filter(has_text="50").click()
    wait_for_table_ready(page)
    page.locator(".ant-table-row", has_text=f"大ID编辑测试-{ability_type}").wait_for(timeout=10000)

    edit_btn = page.locator(".ant-table-row", has_text=f"大ID编辑测试-{ability_type}").locator("a:has-text('编辑')")
    edit_btn.click()
    page.wait_for_selector(".ant-modal")

    modified = f"大ID编辑测试-{ability_type}-已修改"
    page.fill("input#nameCn", modified)

    page.click(".ant-modal-footer .ant-btn-primary")
    page.wait_for_selector(".ant-message-success", timeout=10000)

    page.wait_for_timeout(1000)
    assert page.locator(".ant-table-row", has_text=modified).count() > 0

    delete_btn = page.locator(".ant-table-row", has_text=modified).locator("a:has-text('删除')")
    delete_btn.click()
    page.wait_for_selector(".ant-modal-confirm")
    page.click(".ant-modal-confirm .ant-btn-dangerous")
    page.wait_for_selector(".ant-message-success")
