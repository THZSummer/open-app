import re
import pytest

pytest.importorskip("playwright")

from playwright.sync_api import Page, expect
from .helpers import (
    wait_for_table_ready,
    get_rows,
    make_test_png,
    delete_ability,
    delete_ability_via_api,
    create_ability,
    upload_icon,
    get_next_ability_type,
)


def _wait_for_modal_ready(page):
    page.wait_for_selector(".ant-modal", timeout=10000)
    page.wait_for_selector(".ant-modal .ant-form", timeout=5000)


def _open_create_modal(page):
    page.locator("button", has_text="添加能力").click()
    _wait_for_modal_ready(page)


def _get_field(page, label):
    return page.locator(".ant-form-item").filter(has_text=label).locator("input, textarea")


class TestAbilityAdminCreatePage:
    @pytest.fixture(autouse=True)
    def setup(self, page: Page, ability_admin_url: str):
        self._console_errors = []
        page.on("console", lambda msg: self._console_errors.append(msg.text) if msg.type == "error" else None)
        page.goto(ability_admin_url)
        page.wait_for_load_state("networkidle")
        page.wait_for_selector(".ant-table-row, .ant-empty", timeout=10000)

    def test_open_modal(self, page: Page):
        _open_create_modal(page)
        modal_title = page.locator(".ant-modal-title")
        expect(modal_title).to_have_text("添加能力")

    def test_form_fields_present(self, page: Page):
        _open_create_modal(page)
        expected_labels = [
            "能力标题", "英文名", "能力描述", "英文描述",
            "能力图标", "示意图", "排序号", "能力类型编码",
            "访问地址", "路由路径", "别名", "加载类型",
        ]
        for label in expected_labels:
            item = page.locator(".ant-form-item").filter(has_text=label)
            expect(item).to_be_visible()

    def test_empty_submit_shows_validation(self, page: Page):
        _open_create_modal(page)
        save_btn = page.locator(".ant-modal-footer .ant-btn-primary").first
        save_btn.click()
        page.wait_for_selector(".ant-form-item-explain", timeout=5000)
        error_items = page.locator(".ant-form-item-explain")
        count = error_items.count()
        assert count > 0, f"预期有校验错误提示，实际有 {count} 个"

    def test_name_too_short(self, page: Page):
        _open_create_modal(page)
        title_input = _get_field(page, "能力标题").first
        title_input.fill("能")
        save_btn = page.locator(".ant-modal-footer .ant-btn-primary").first
        save_btn.click()
        err = page.locator(".ant-form-item").filter(has_text="能力标题").locator(".ant-form-item-explain-error")
        expect(err).to_be_visible()
        expect(err).to_contain_text("至少")

    def test_create_success(self, page: Page, tmp_path):
        _open_create_modal(page)
        fields = {
            "能力标题": "E2E创建测试",
            "英文名": "e2e-create-test",
            "能力描述": "这是E2E创建测试的能力描述",
            "英文描述": "This is E2E create test description",
            "排序号": "1",
            "访问地址": "https://e2e-create.example.com",
            "路由路径": "/e2e-create",
        }
        for label, value in fields.items():
            _get_field(page, label).first.fill(value)
        type_input = page.locator(".ant-form-item").filter(has_text="能力类型编码").locator("input").first
        at = str(get_next_ability_type()); type_input.fill(at)
        png_path = tmp_path / "icon_40x40.png"
        png_path.write_bytes(make_test_png(40, 40))
        icon_item = page.locator(".ant-form-item").filter(has_text="能力图标")
        with page.expect_file_chooser() as fc_info:
            icon_item.locator(".ant-upload-select").click()
        fc_info.value.set_files(str(png_path))
        crop_modal = page.locator(".ant-modal").filter(has_text="裁剪图标")
        crop_modal.locator("button", has_text=re.compile(r"确\s*认")).wait_for(timeout=5000)
        crop_modal.locator("button", has_text=re.compile(r"确\s*认")).click()
        page.wait_for_timeout(2000)
        save_btn = page.locator(".ant-modal-footer .ant-btn-primary").first
        save_btn.click()
        msg = page.locator(".ant-message-notice").filter(has_text="能力添加成功")
        expect(msg).to_be_visible(timeout=15000)
        expect(page.locator(".ant-modal-title").filter(has_text="添加能力")).not_to_be_visible(timeout=5000)
        delete_ability_via_api(int(at))

    def test_icon_required(self, page: Page):
        _open_create_modal(page)
        fields = {
            "能力标题": "E2E无图标测试",
            "英文名": "e2e-no-icon",
            "能力描述": "这是无图标提交的测试描述",
            "英文描述": "No icon test description",
            "排序号": "1",
            "访问地址": "https://no-icon.example.com",
            "路由路径": "/no-icon",
        }
        for label, value in fields.items():
            _get_field(page, label).first.fill(value)
        type_input = page.locator(".ant-form-item").filter(has_text="能力类型编码").locator("input").first
        at2 = str(get_next_ability_type()); type_input.fill(at2)
        save_btn = page.locator(".ant-modal-footer .ant-btn-primary").first
        save_btn.click()
        err = page.locator(".ant-form-item").filter(has_text="能力图标").locator(".ant-form-item-explain-error")
        expect(err).to_be_visible(timeout=5000)

    def test_cancel_closes_modal(self, page: Page):
        rows_before = get_rows(page).count()
        _open_create_modal(page)
        cancel_btn = page.get_by_role("button", name="取 消")
        cancel_btn.click()
        expect(page.locator(".ant-modal")).not_to_be_visible()
        page.wait_for_timeout(500)
        rows_after = get_rows(page).count()
        assert rows_after == rows_before, f"取消后列表行数变化：{rows_before} → {rows_after}"

    def test_create_duplicate_ability_type(self, page: Page, tmp_path):
        dup_at = get_next_ability_type()
        delete_ability(dup_at)
        result = create_ability(dup_at, nameCn="E2E重复测试", nameEn="e2e-duplicate", iconBatchId=upload_icon()[0])
        assert result.get("code") == "200", f"前置创建失败: {result}"
        try:
            _open_create_modal(page)
            fields = {
                "能力标题": "E2E重复测试2",
                "英文名": "e2e-duplicate-2",
                "能力描述": "这是重复能力类型的测试描述",
                "英文描述": "Duplicate type test description",
                "排序号": "1",
                "访问地址": "https://duplicate.example.com",
                "路由路径": "/duplicate",
            }
            for label, value in fields.items():
                _get_field(page, label).first.fill(value)
            type_input = page.locator(".ant-form-item").filter(has_text="能力类型编码").locator("input").first
            type_input.fill(str(dup_at))
            png_path = tmp_path / "icon_dup.png"
            png_path.write_bytes(make_test_png(40, 40))
            icon_item = page.locator(".ant-form-item").filter(has_text="能力图标")
            with page.expect_file_chooser() as fc_info:
                icon_item.locator(".ant-upload-select").click()
            fc_info.value.set_files(str(png_path))
            crop_modal = page.locator(".ant-modal").filter(has_text="裁剪图标")
            crop_modal.locator("button", has_text=re.compile(r"确\s*认")).wait_for(timeout=5000)
            crop_modal.locator("button", has_text=re.compile(r"确\s*认")).click()
            page.wait_for_timeout(2000)
            save_btn = page.locator(".ant-modal-footer .ant-btn-primary").first
            save_btn.click()
            msg = page.locator(".ant-message-notice").filter(has_text="编码已被占用")
            expect(msg).to_be_visible(timeout=15000)
        finally:
            delete_ability(dup_at)
