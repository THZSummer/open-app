import pytest

pytest.importorskip("playwright")

from playwright.sync_api import Page, expect
from .helpers import (
    wait_for_table_ready,
    get_rows,
    delete_ability,
    create_ability,
    create_ability_via_api,
    upload_icon,
    list_abilities,
    get_next_ability_type,
    generate_png,
)



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
        delete_at = get_next_ability_type()
        name_cn = f"E2E删除测试-{delete_at}"
        batch_id, _ = upload_icon()
        resp = create_ability(delete_at, nameCn=name_cn, nameEn="e2e-delete-test",
                              iconBatchId=batch_id)
        assert resp.get("code") == "200", f"创建失败: {resp}"
        records = list_abilities().get("data", [])
        record_id = None
        for r in records:
            if r["abilityType"] == delete_at:
                record_id = r.get("id")
                break
        assert record_id is not None, "创建后未找到记录 id"
        page.reload()
        wait_for_table_ready(page)
        page.locator(".ant-pagination-options .ant-select-selector").click()
        page.wait_for_selector(".ant-select-dropdown", timeout=3000)
        page.locator(".ant-select-dropdown .ant-select-item-option").filter(has_text="50").click()
        wait_for_table_ready(page)
        row = page.locator("tr").filter(has_text=name_cn)
        delete_btn = row.locator("a", has_text="删除")
        delete_btn.click()
        page.wait_for_selector(".ant-modal-confirm", timeout=5000)
        ok_btn = page.locator(".ant-modal-confirm .ant-btn-dangerous")
        ok_btn.click()
        msg = page.locator(".ant-message-notice").filter(has_text="删除成功")
        expect(msg).to_be_visible(timeout=10000)

    def test_delete_non_existent(self, page: Page):
        nonexist_at = get_next_ability_type()
        name_cn = f"E2E删除不存在-{nonexist_at}"
        batch_id, _ = upload_icon()
        resp = create_ability(nonexist_at, nameCn=name_cn, nameEn="e2e-delete-nonexist",
                              iconBatchId=batch_id)
        assert resp.get("code") == "200", f"创建失败: {resp}"
        records = list_abilities().get("data", [])
        record_id = None
        for r in records:
            if r["abilityType"] == nonexist_at:
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
        row = page.locator("tr").filter(has_text=name_cn)
        delete_btn = row.locator("a", has_text="删除")
        delete_btn.click()
        page.wait_for_selector(".ant-modal-confirm", timeout=5000)
        ok_btn = page.locator(".ant-modal-confirm .ant-btn-dangerous")
        ok_btn.click()
        msg = page.locator(".ant-message-notice").filter(has_text="能力记录不存在")
        expect(msg).to_be_visible(timeout=10000)


def test_delete_newly_created_record(page: Page, ability_admin_url: str):
    """删除新建记录（大snowflake ID）→ 验证无精度丢失"""
    ability_type = str(get_next_ability_type())

    page.goto(ability_admin_url)
    page.wait_for_selector(".ant-table-row", timeout=10000)

    page.click("button:has-text('添加能力')")
    page.wait_for_selector(".ant-modal")

    page.fill("input#nameCn", f"大ID删除测试-{ability_type}")
    page.fill("input#nameEn", "BigIDDel")
    page.fill("textarea#descCn", "测试大ID删除是否有精度问题")
    page.fill("textarea#descEn", "Test big ID delete")
    page.fill("input#entryUrl", "http://example.com")
    page.fill("input#routePath", "/big-id-del")
    page.fill("input#aliasName", "bigiddel")
    page.fill("input#orderNum", "98")
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
    page.locator(".ant-table-row", has_text=f"大ID删除测试-{ability_type}").wait_for(timeout=10000)

    delete_btn = page.locator(".ant-table-row", has_text=f"大ID删除测试-{ability_type}").locator("a:has-text('删除')")
    delete_btn.click()
    page.wait_for_selector(".ant-modal-confirm")
    page.click(".ant-modal-confirm .ant-btn-dangerous")
    page.wait_for_selector(".ant-message-success")

    page.wait_for_timeout(1000)
    assert page.locator(".ant-table-row", has_text=f"大ID删除测试-{ability_type}").count() == 0
