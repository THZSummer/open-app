"""
wecodesite E2E 测试公共模块 — Playwright 页面操作封装
====================================================

与 open-server backend 测试的 common/client.py 对应, 提供统一的:
  - 等待策略 (wait_for_table_ready, wait_for_modal_ready 等)
  - Ant Design 4 表单操作 (fill_form_item, select_option 等)
  - 连接器/连接流 UI 创建 (create_connector_via_ui, create_flow_via_ui)
  - FlowEditorV2 操作 (select_flow_mode, add_node, save_flow_draft 等)

所有数据通过前端 UI 操作创建, 不调后端 API, 不操作数据库.
"""

from playwright.sync_api import Page


# ═══════════════════════════════════════════════════════════
# 等待策略
# ═══════════════════════════════════════════════════════════

def wait_for_network_idle(page: Page, timeout: int = 30000):
    """等待网络请求完成 + 页面渲染"""
    page.wait_for_load_state("networkidle", timeout=timeout)


def wait_for_table_ready(page: Page):
    """等待 Ant Design 表格渲染完成 (有数据或显示空状态)"""
    page.wait_for_selector(".ant-table-tbody", timeout=10000)
    page.wait_for_selector(".ant-spin-spinning", state="detached", timeout=15000)
    try:
        page.locator(".ant-table-row").first.wait_for(timeout=5000)
    except Exception:
        page.wait_for_selector(".ant-empty", timeout=5000)


def wait_for_modal_ready(page: Page):
    """等待 Ant Design Modal 渲染完成"""
    page.wait_for_selector(".ant-modal:visible", timeout=5000)
    page.wait_for_load_state("networkidle", timeout=5000)


def wait_for_message_success(page: Page, timeout: int = 5000):
    """等待操作成功提示消息出现并消失"""
    page.wait_for_selector(".ant-message-success", timeout=timeout)
    page.wait_for_selector(".ant-message-success", state="detached", timeout=10000)


def get_rows(page: Page):
    """获取表格所有数据行"""
    return page.locator(".ant-table-row")


# ═══════════════════════════════════════════════════════════
# Ant Design 表单操作
# ═══════════════════════════════════════════════════════════

def fill_form_item(page: Page, label: str, value: str):
    """填写 Ant Design Form.Item (通过 label 文本定位 input/textarea)"""
    item = page.locator(".ant-form-item").filter(has_text=label)
    input_el = item.locator("input, textarea")
    if input_el.count() > 0:
        input_el.first.click()
        input_el.first.fill(value)
        return input_el.first
    return None


def select_option(page: Page, label: str, option_text: str):
    """选择 Ant Design Select 下拉选项"""
    item = page.locator(".ant-form-item").filter(has_text=label)
    selector = item.locator(".ant-select-selector")
    if selector.count() > 0:
        selector.first.click()
        page.wait_for_timeout(300)
        page.locator(".ant-select-dropdown:visible .ant-select-item-option") \
            .filter(has_text=option_text).first.click()
        page.wait_for_timeout(200)


def click_button(page: Page, text: str):
    """点击按钮 (通过文本匹配)"""
    btn = page.locator("button").filter(has_text=text)
    if btn.count() > 0:
        btn.first.click()
    return btn


# ═══════════════════════════════════════════════════════════
# 连接器管理 (UI 操作)
# ═══════════════════════════════════════════════════════════

def _name_to_en(name: str) -> str:
    """中文名称转英文名称: 小写 + 非字母数字转下划线"""
    import re
    en = name.lower()
    en = re.sub(r'[^a-z0-9]+', '_', en)
    en = en.strip('_')
    return en or name.lower()


def create_connector_via_ui(page: Page, name: str, protocol: str = "HTTP") -> str:
    """通过 UI 创建连接器, 返回连接器 ID"""
    click_button(page, "新建连接器")
    wait_for_modal_ready(page)
    modal = page.locator(".ant-modal:visible")
    # 在 modal 范围内填充表单
    modal.locator(".ant-form-item").filter(has_text="中文名称").locator("input, textarea").first.fill(name)
    modal.locator(".ant-form-item").filter(has_text="英文名称").locator("input, textarea").first.fill(_name_to_en(name))
    modal.locator("button").filter(has_text="保").first.click()
    # 等待 modal 关闭 + 表格刷新
    page.wait_for_selector(".ant-modal:visible", state="detached", timeout=10000)
    page.wait_for_timeout(500)
    wait_for_table_ready(page)
    # 在表格中查找刚创建的连接器行, 提取 ID
    import re
    row = page.locator(".ant-table-row").filter(has_text=name).first
    if row.count() > 0:
        text = row.text_content()
        m = re.match(r'(\d+)', text)
        if m:
            return m.group(1)
    return ""


# ═══════════════════════════════════════════════════════════
# 连接流管理 (UI 操作)
# ═══════════════════════════════════════════════════════════

def create_flow_via_ui(page: Page, name: str) -> str:
    """通过 UI 创建连接流, 返回连接流 ID"""
    click_button(page, "新建连接流")
    wait_for_modal_ready(page)
    modal = page.locator(".ant-modal:visible")
    # 在 modal 范围内填充表单
    modal.locator(".ant-form-item").filter(has_text="中文名称").locator("input, textarea").first.fill(name)
    modal.locator(".ant-form-item").filter(has_text="英文名称").locator("input, textarea").first.fill(_name_to_en(name))
    modal.locator("button").filter(has_text="保").first.click()
    # 等待 modal 关闭 + 表格刷新
    page.wait_for_selector(".ant-modal:visible", state="detached", timeout=10000)
    page.wait_for_timeout(500)
    wait_for_table_ready(page)
    # 在表格中查找刚创建的连接流行, 提取 ID
    import re
    row = page.locator(".ant-table-row").filter(has_text=name).first
    if row.count() > 0:
        text = row.text_content()
        m = re.match(r'(\d+)', text)
        if m:
            return m.group(1)
    return ""


# ═══════════════════════════════════════════════════════════
# FlowEditorV2 专用操作
# ═══════════════════════════════════════════════════════════

def select_flow_mode(page: Page, mode: str):
    """选择流模式 (单节点 / 串行 / 并行)"""
    radio = page.locator(".ant-radio-wrapper").filter(has_text=mode)
    if radio.count() > 0:
        radio.first.click()
    else:
        # fallback: use mode-card
        card = page.locator(".mode-card").filter(has_text=mode).first
        if card.count() > 0:
            card.click()


def add_node(page: Page, node_type: str):
    """添加节点 (connector / script / exit)"""
    click_button(page, "添加节点")
    wait_for_modal_ready(page)
    page.locator(".ant-modal:visible") \
        .locator(".ant-card, .ant-list-item") \
        .filter(has_text=node_type).first.click()
    wait_for_network_idle(page)


def fill_connector_node_config(page: Page, connector_name: str, input_mapping: dict = None):
    """配置连接器节点: 选择连接器 + 填写输入映射"""
    select_option(page, "连接器", connector_name)
    wait_for_network_idle(page)
    if input_mapping:
        for field, expr in input_mapping.items():
            fill_form_item(page, field, expr)


def fill_script_node_code(page: Page, code: str):
    """在 Script 节点 Monaco Editor 中输入 JS 代码"""
    page.wait_for_selector(".monaco-editor", timeout=5000)
    page.locator(".monaco-editor textarea").first.click()
    page.locator(".monaco-editor textarea").first.fill(code)


def save_flow_draft(page: Page):
    """保存编排草稿"""
    click_button(page, "保存")
    wait_for_message_success(page)


def debug_flow(page: Page, trigger_data: dict):
    """调试流: 输入触发数据并执行"""
    import json
    click_button(page, "调试")
    wait_for_modal_ready(page)
    editor = page.locator(".monaco-editor textarea")
    if editor.count() > 0:
        editor.first.click()
        editor.first.fill(json.dumps(trigger_data, ensure_ascii=False))
    click_button(page, "执行")
    wait_for_network_idle(page, timeout=30000)


def publish_flow(page: Page):
    """提交发布"""
    click_button(page, "发布")
    wait_for_modal_ready(page)
    click_button(page, "提交发布")
    wait_for_message_success(page)


def deploy_and_start_flow(page: Page):
    """部署并启动连接流 (在 FlowList 页面操作)"""
    click_button(page, "部署")
    wait_for_message_success(page)
    click_button(page, "启动")
    wait_for_message_success(page)


# ═══════════════════════════════════════════════════════════
# 导航
# ═══════════════════════════════════════════════════════════

def navigate_to(page: Page, url: str):
    """导航到指定页面并等待渲染完成"""
    page.goto(url)
    wait_for_network_idle(page)
    wait_for_table_ready(page)
