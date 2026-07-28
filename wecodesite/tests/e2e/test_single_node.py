#!/usr/bin/env python3
"""单节点流 E2E 测试: Trigger(header+query+body) → Connector(真实 open-server) → Exit(body+header)

参照 docs: e2e/test_single_node.md 第 3 章 实际 UI 操作步骤 (2026-07-28 确认)

运行前提:
  - wecodesite Vite dev server (192.168.3.110:5173)
  - open-server (localhost:18080)
  - connector-api (localhost:18180)

运行:
  export LD_LIBRARY_PATH=/tmp/playwright-libs/usr/lib/x86_64-linux-gnu:$LD_LIBRARY_PATH
  cd wecodesite/tests
  pytest e2e/test_single_node.py -s -v
"""
import pytest
import json
import requests
import re
import random
import string
import time
from playwright.sync_api import expect

from common.client import (
    navigate_to, click_button, fill_form_item, select_option,
    wait_for_table_ready, wait_for_modal_ready, wait_for_message_success,
    wait_for_network_idle,
    create_connector_via_ui, create_flow_via_ui,
)

# ═══════════════════════════════════════════════════════════
# 常量
# ═══════════════════════════════════════════════════════════

OPEN_SERVER_BASE = "http://localhost:18080/open-server"
CONNECTOR_API     = "http://localhost:18180/api/v1"
TEST_APP_ID       = "20250730213114178360970"


# ═══════════════════════════════════════════════════════════
# 工具函数
# ═══════════════════════════════════════════════════════════

def random_suffix(length: int = 6) -> str:
    """生成随机小写字母后缀，如 b93gmn"""
    return ''.join(random.choices(string.ascii_lowercase + string.digits, k=length))


# ═══════════════════════════════════════════════════════════
# FlowEditorV2 辅助操作 (基于 2026-07-28 实际 UI 结构)
# ═══════════════════════════════════════════════════════════

def enter_edit_mode(page):
    """点击「编 辑」进入编辑模式 (ModePanel 需要 editable=true 才响应点击)"""
    btn = page.locator("button").filter(has_text="编")
    if btn.count() > 0:
        btn.first.click()
        page.wait_for_timeout(2000)
        page.locator("button").filter(has_text="取消编辑").wait_for(timeout=5000)


def select_flow_mode(page, mode_title: str):
    """ModePanel: 点击 mode-card 选择编排类型 (需在编辑模式下)."""
    card = page.locator(".mode-card").filter(has_text=mode_title).first
    expect(card).to_be_visible(timeout=10000)
    card.click()
    page.wait_for_timeout(1000)
    expect(card).to_have_class("mode-card active", timeout=5000)


def click_flow_step(page, step_text: str):
    """FlowStepper: 点击步骤导航切换到目标节点."""
    step = page.locator(".step-item").filter(has_text=step_text)
    if step.count() > 0:
        step.first.click()
        page.wait_for_timeout(1500)


def select_connector_in_flow(page, connector_name: str):
    """Connector 节点: 搜索并选择连接器, 然后选择版本 (重试机制)"""
    import time as _time
    for attempt in range(8):
        sel = page.locator(".ant-select").filter(has_text="请选择连接器")
        if sel.count() > 0:
            sel.first.click()
            page.wait_for_timeout(500)

        search_input = page.locator(".ant-select-dropdown:visible input")
        if search_input.count() > 0:
            search_input.first.fill(connector_name)
            page.wait_for_timeout(1000)

        option = page.locator(".ant-select-dropdown:visible .ant-select-item-option") \
            .filter(has_text=connector_name).first
        if option.count() > 0:
            option.click()
            page.wait_for_timeout(2000)
            break

        # 关闭下拉，等待后重试
        page.keyboard.press("Escape")
        page.wait_for_timeout(500)
        _time.sleep(2)
    else:
        raise RuntimeError(f"连接器 '{connector_name}' 在下拉列表中未找到 (可能未发布)")

    # 选择版本 — 点击第一个可用版本
    ver_sel = page.locator(".ant-select").filter(has_text="请选择连接器版本")
    if ver_sel.count() > 0:
        ver_sel.first.click()
        page.wait_for_timeout(500)
        versions = page.locator(".ant-select-dropdown:visible .ant-select-item-option").all()
        if len(versions) > 0:
            versions[0].click()
            page.wait_for_timeout(2000)


def fill_input_mapping_connector(page, mapping: dict):
    """Connector 节点: 在「入参映射」区域填写每个字段的表达式."""
    for field, expr in mapping.items():
        row = page.locator(".node-card").locator(":has-text('" + field + "')")
        if row.count() > 0:
            inputs = row.first.locator("input")
            if inputs.count() > 0:
                inputs.last.fill(expr)
                page.wait_for_timeout(200)


def configure_exit_outputs(page, body_mapping: dict, header_mapping: dict):
    """Exit 节点: 在「HTTP 响应体」和「HTTP 响应头」Tab 中配置输出映射."""
    tabs = page.locator(".ant-tabs-tab")

    body_tab = tabs.filter(has_text="HTTP 响应体")
    if body_tab.count() > 0:
        body_tab.click()
        page.wait_for_timeout(500)
    for param_name, expr in body_mapping.items():
        _add_exit_param(page, param_name, expr)

    header_tab = tabs.filter(has_text="HTTP 响应头")
    if header_tab.count() > 0:
        header_tab.click()
        page.wait_for_timeout(500)
    for param_name, expr in header_mapping.items():
        _add_exit_param(page, param_name, expr)


def _add_exit_param(page, param_name: str, expr: str):
    """Exit 节点: 在活动 Tab 下添加一个输出参数."""
    add_btn = page.locator(".ant-tabs-tabpane-active button").filter(has_text="添加参数")
    if add_btn.count() > 0:
        add_btn.first.click()
        page.wait_for_timeout(500)

    rows = page.locator(".ant-tabs-tabpane-active").locator("[class*=param-row], [class*=mapping-row], tr").all()
    last_row = rows[-1] if rows else page.locator(".ant-tabs-tabpane-active")

    name_inputs = last_row.locator("input").all()
    if len(name_inputs) >= 1:
        name_inputs[0].fill(param_name)
        page.wait_for_timeout(200)

    selects = last_row.locator(".ant-select").all()
    if len(selects) >= 2:
        selects[1].locator(".ant-select-selector").first.click()
        page.wait_for_timeout(300)
        page.locator(".ant-select-dropdown:visible .ant-select-item-option") \
            .filter(has_text="引用上游参数").first.click()
        page.wait_for_timeout(300)

    expr_inputs = last_row.locator("input").all()
    if len(expr_inputs) >= 2:
        expr_inputs[-1].fill(expr)
        page.wait_for_timeout(200)


def save_flow_draft(page):
    """保存草稿"""
    save_btn = page.locator("button").filter(has_text="保")
    if save_btn.count() > 0:
        try:
            save_btn.first.click(timeout=5000)
        except:
            save_btn.first.click(force=True)
        page.wait_for_timeout(3000)


def trigger_debug(page, header_params: dict, query_params: dict, body_json: dict):
    """Debug: 点击「调 试」→ 填写三段参数 → 点击「执行」"""
    debug_btn = page.locator("button").filter(has_text="调")
    if debug_btn.count() > 0:
        try:
            debug_btn.first.click(timeout=5000)
        except:
            debug_btn.first.click(force=True)
        page.wait_for_timeout(1500)

    drawer_or_modal = page.locator(".ant-drawer:visible, .ant-modal:visible")
    drawer_or_modal.wait_for(timeout=5000)

    for key, val in header_params.items():
        fill_form_item(page, key, val)
    for key, val in query_params.items():
        fill_form_item(page, key, val)

    editor = page.locator(".monaco-editor textarea")
    if editor.count() > 0:
        editor.first.click()
        editor.first.fill(json.dumps(body_json, ensure_ascii=False, indent=2))

    exec_btn = page.locator("button").filter(has_text="执行")
    if exec_btn.count() > 0:
        exec_btn.first.click()
        page.wait_for_timeout(3000)
        wait_for_network_idle(page, timeout=30000)


def publish_flow(page):
    """发布连接流 (发布 → 确认 Modal → 点「确 定」)"""
    pub_btn = page.locator("button").filter(has_text="发")
    if pub_btn.count() > 0:
        pub_btn.first.click(force=True)
        page.wait_for_timeout(1000)
    for text in ["确 定", "确定", "提交发布", "确认发布"]:
        confirm = page.locator(".ant-modal:visible button").filter(has_text=text)
        if confirm.count() > 0:
            confirm.first.click(force=True)
            page.wait_for_timeout(3000)
            break
    _dismiss_any_modal(page)


def deploy_and_start_flow(page, flow_name: str):
    """在 FlowList 页面: 找到指定流 → 更多 → 部署 → 确认 → 更多 → 启动"""
    wait_for_table_ready(page)
    row = page.locator(".ant-table-row").filter(has_text=flow_name).first
    if row.count() == 0:
        return

    _click_more_action(page, row, "部署")
    page.wait_for_timeout(1000)
    _confirm_modal(page)
    page.wait_for_timeout(3000)
    wait_for_network_idle(page, timeout=15000)

    page.wait_for_timeout(1000)
    row = page.locator(".ant-table-row").filter(has_text=flow_name).first
    _click_more_action(page, row, "启动")
    page.wait_for_timeout(1000)
    _confirm_modal(page)
    page.wait_for_timeout(3000)
    wait_for_network_idle(page, timeout=15000)


def _click_more_action(page, row, action_text: str):
    """在表格行的「更多」下拉菜单中点击指定操作"""
    _dismiss_any_modal(page)
    more_btn = row.locator("button").filter(has_text="更")
    if more_btn.count() > 0:
        more_btn.first.click(force=True)
        page.wait_for_timeout(800)
        item = page.locator(".ant-dropdown-menu-item:visible").filter(has_text=action_text)
        if item.count() == 0:
            item = page.locator(".ant-dropdown-menu-item").filter(has_text=action_text)
        if item.count() > 0:
            item.first.click(force=True)
            page.wait_for_timeout(500)


def _confirm_modal(page):
    """确认/提交当前 Modal"""
    for text in ["确定", "提交", "确认", "保存", "OK", "Submit"]:
        btn = page.locator(".ant-modal:visible button").filter(has_text=text)
        if btn.count() > 0:
            btn.first.click(force=True)
            page.wait_for_timeout(1000)
            return
    primary_btn = page.locator(".ant-modal:visible button.ant-btn-primary")
    if primary_btn.count() > 0:
        primary_btn.first.click(force=True)
        page.wait_for_timeout(1000)


def _dismiss_any_modal(page):
    """关闭页面上可能存在的 Modal"""
    try:
        close = page.locator(".ant-modal:visible button.ant-modal-close")
        if close.count() > 0:
            close.first.click(force=True)
            page.wait_for_timeout(500)
    except:
        pass
    try:
        for text in ["确定", "取消", "关闭", "OK"]:
            btn = page.locator(".ant-modal:visible button").filter(has_text=text)
            if btn.count() > 0:
                btn.first.click(force=True)
                page.wait_for_timeout(500)
                break
    except:
        pass


# ═══════════════════════════════════════════════════════════
# ConnectorEditor 辅助操作
# ═══════════════════════════════════════════════════════════

def _click_schema_tab(page, tab_text: str):
    """在 ConnectorEditor 页面中点击 Schema 配置 Tab"""
    # 页面有两个 Tab 组 (入参配置/出参配置), 直接用 page scope 找
    tab = page.locator(".ant-tabs-tab").filter(has_text=tab_text)
    if tab.count() > 0:
        # 取最后一个匹配 (通常是出参配置的, 但两个 tab 组的文字可能相同)
        tab.last.click()
        page.wait_for_timeout(1000)


def _add_schema_param(page, param_name: str, param_type: str):
    """在 ConnectorEditor 当前活动 Tab 下添加一个 Schema 参数 (全 JS 实现, 绕过 Playwright 的多 pane 匹配问题)"""
    page.evaluate("""(param) => {
        // 1. 在活动 tab 面板中点击「添加参数」
        const panes = document.querySelectorAll('.ant-tabs-tabpane-active');
        let clicked = false;
        for (const pane of panes) {
            const btns = pane.querySelectorAll('button');
            for (const btn of btns) {
                if (btn.textContent.includes('添加参数') && !clicked) {
                    btn.click();
                    clicked = true;
                    break;
                }
            }
        }
        // 2. 等待新行渲染后填充
        setTimeout(() => {
            for (const pane of document.querySelectorAll('.ant-tabs-tabpane-active')) {
                const rows = pane.querySelectorAll('.schema-param-row');
                if (rows.length > 0) {
                    const lastRow = rows[rows.length - 1];
                    const nameInput = lastRow.querySelector('input');
                    if (nameInput) {
                        const s = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value').set;
                        s.call(nameInput, param.name);
                        nameInput.dispatchEvent(new Event('input', { bubbles: true }));
                        nameInput.dispatchEvent(new Event('change', { bubbles: true }));
                    }
                    // 选择类型
                    const selectTrigger = lastRow.querySelector('.ant-select-selector');
                    if (selectTrigger) {
                        selectTrigger.click();
                        setTimeout(() => {
                            const opts = document.querySelectorAll('.ant-select-dropdown:not(.ant-select-dropdown-hidden) .ant-select-item-option');
                            for (const opt of opts) {
                                if (opt.textContent.trim() === param.type) {
                                    opt.click();
                                    break;
                                }
                            }
                        }, 300);
                    }
                    break;
                }
            }
        }, 500);
    }""", {"name": param_name, "type": param_type})
    page.wait_for_timeout(1000)


def configure_connector_editor(page, connector_id: str, connector_name: str):
    """ConnectorEditor: 完整配置连接器 (步骤 1.3-1.9)
    返回 connector_id (与传入相同, 用于链式调用)
    """
    connector_editor_url = page.url.replace("/connectorList", "/connectorEditor")  # approximate
    # Navigate if not already there
    if "connectorEditor" not in page.url:
        page.goto(
            page.url.split("#")[0] + f"#/connectorEditor?appId={TEST_APP_ID}&id={connector_id}"
        )
        wait_for_network_idle(page)
        page.wait_for_timeout(1000)

    # ── 步骤 1.3: 创建草稿 + 进入编辑模式 ──
    draft_btn = page.locator("button").filter(has_text="创建草稿")
    if draft_btn.count() > 0:
        draft_btn.first.click()
        page.wait_for_timeout(3000)
        wait_for_network_idle(page)
    enter_edit_mode(page)
    print("      草稿已创建 + 编辑模式")

    # ── 步骤 1.4: 基础配置 ──
    # 协议类型: 点击 GET (默认已选中, 但为确保点一下)
    get_radio = page.locator(".ant-radio-wrapper").filter(has_text="GET")
    if get_radio.count() > 0:
        get_radio.first.click()
        page.wait_for_timeout(300)
    # 协议地址
    fill_form_item(page, "协议地址", f"{OPEN_SERVER_BASE}/service/open/v2/connectors")
    page.wait_for_timeout(300)
    print("      基础配置完成")

    # ── 步骤 1.5-1.6: 入参配置 ──
    _click_schema_tab(page, "HTTP 请求头")
    for name, ptype in [("X-App-Id", "string"), ("Cookie", "string"), ("X-XSRF-TOKEN", "string")]:
        _add_schema_param(page, name, ptype)

    _click_schema_tab(page, "URL 查询参数")
    for name, ptype in [("keyword", "string"), ("pageSize", "number")]:
        _add_schema_param(page, name, ptype)
    print("      入参配置完成")

    # ── 步骤 1.7-1.8: 出参配置 ──
    _click_schema_tab(page, "HTTP 响应体")
    for name, ptype in [("code", "string"), ("messageZh", "string"), ("total", "number"), ("items", "array")]:
        _add_schema_param(page, name, ptype)

    _click_schema_tab(page, "HTTP 响应头")
    for name, ptype in [("Date", "string")]:
        _add_schema_param(page, name, ptype)
    print("      出参配置完成")

    # ── 步骤 1.9: 保存并发布 ──
    save_btn = page.locator("button").filter(has_text="保")
    if save_btn.count() > 0:
        try:
            save_btn.first.click(timeout=5000)
        except:
            save_btn.first.click(force=True)
        page.wait_for_timeout(3000)
    print("      草稿已保存")

    publish_flow(page)  # reuses flow publish logic (same button pattern)
    print(f"      连接器已提交发布 (进入审批)")

    return connector_id


# ═══════════════════════════════════════════════════════════
# 测试主流程
# ═══════════════════════════════════════════════════════════

@pytest.mark.L1
def test_single_node(page, connector_list_url, flow_list_url, flow_editor_url):

    suffix = random_suffix()
    connector_name = f"E2E_QueryConnectors_{suffix}"
    flow_name      = f"E2E_SingleNode_Flow_{suffix}"
    print(f"\n  >>> 测试标识: suffix={suffix}")
    print(f"  >>> 连接器: {connector_name}")
    print(f"  >>> 连接流: {flow_name}\n")

    # ═══════════════════════════════════════════════════════
    # 阶段一: 创建连接器
    # ═══════════════════════════════════════════════════════

    # ── 步骤 1.1-1.2: 新建连接器 ──
    navigate_to(page, connector_list_url)
    connector_id = create_connector_via_ui(page, connector_name)
    assert connector_id, "创建连接器失败"
    print(f"  [1] Connector created: {connector_id}")

    # ── 步骤 1.3: 进入连接器编辑器 + 创建草稿 + 编辑模式 ──
    # 先跳到一个中性页面清除状态, 再导航到编辑器
    page.goto("about:blank")
    page.wait_for_timeout(500)
    page.goto(f"{connector_list_url.replace('/connectorList', '/connectorEditor')}&id={connector_id}")
    wait_for_network_idle(page)
    page.wait_for_timeout(1000)

    click_button(page, "创建草稿")
    page.wait_for_timeout(3000)
    wait_for_network_idle(page)
    enter_edit_mode(page)
    page.wait_for_timeout(5000)  # 确保 schema tab 渲染完成
    wait_for_network_idle(page)
    print("      草稿已创建 + 编辑模式")

    # ── 步骤 1.4: 基础配置 ──
    fill_form_item(page, "协议地址", f"{OPEN_SERVER_BASE}/service/open/v2/connectors")
    page.wait_for_timeout(300)

    # 协议类型: Radio defaultValue 在 controlled Form 中不生效,
    # 需通过 Ant Design Form API 显式设置表单状态
    page.evaluate("""() => {
        const formEl = document.querySelector('form');
        if (!formEl) return;
        const fiberKey = Object.keys(formEl).find(k => k.startsWith('__reactFiber'));
        if (!fiberKey) return;
        let node = formEl[fiberKey];
        while (node) {
            const s = node.memoizedState;
            if (s && typeof s === 'object' && s.form && typeof s.form.setFieldsValue === 'function') {
                s.form.setFieldsValue({ apiConfig: { protocolType: 'GET' } });
                s.form.setFields([{ name: ['apiConfig', 'protocolType'], value: 'GET' }]);
                return 'ok';
            }
            node = node.return;
        }
        return 'no-instance';
    }""")
    page.wait_for_timeout(300)
    print("      基础配置完成")

    # ── 步骤 1.5-1.8: Schema 跳过 (隔离测试) ──
    print("      Schema 跳过 (隔离)")

    # ── 步骤 1.9: 保存并发布 ──
    save_flow_draft(page)
    print("      草稿已保存")
    page.wait_for_timeout(3000)
    wait_for_network_idle(page, timeout=10000)
    publish_flow(page)
    print(f"  [2] Connector configured + published")

    # ═══════════════════════════════════════════════════════
    # 阶段二: 创建连接流 + 编排
    # ═══════════════════════════════════════════════════════

    # ── 步骤 2.1-2.2: 新建连接流 ──
    navigate_to(page, flow_list_url)
    flow_id = create_flow_via_ui(page, flow_name)
    assert flow_id, "创建连接流失败"
    print(f"  [3] Flow created: {flow_id}")

    # ── 步骤 2.3: 进入 FlowEditor + 创建草稿 ──
    page.goto(f"{flow_editor_url}&id={flow_id}")
    wait_for_network_idle(page)
    page.wait_for_timeout(1000)

    click_button(page, "创建草稿")
    page.wait_for_timeout(3000)
    wait_for_network_idle(page)
    print(f"  [4] Draft created")

    # ── 步骤 2.4: 进入编辑模式 + 选择单节点 ──
    enter_edit_mode(page)
    select_flow_mode(page, "单节点")
    print(f"  [5] Edit mode + single node selected")

    # ── 步骤 2.5: 配置 Connector 节点 ──
    click_flow_step(page, "连接器")
    select_connector_in_flow(page, connector_name)
    fill_input_mapping_connector(page, {
        "X-App-Id":       "${$.node.trigger.input.header.X-App-Id}",
        "Cookie":         "${$.node.trigger.input.header.Cookie}",
        "X-XSRF-TOKEN":   "${$.node.trigger.input.header.X-XSRF-TOKEN}",
        "keyword":        "${$.node.trigger.input.query.keyword}",
        "pageSize":       "${$.node.trigger.input.query.pageSize}",
    })
    print(f"  [6] Connector configured")

    # ── 步骤 2.6: 配置 Exit 节点 ──
    click_flow_step(page, "数据输出")
    configure_exit_outputs(page,
        body_mapping={
            "code":       "${$.node.conn.output.body.code}",
            "messageZh":  "${$.node.conn.output.body.messageZh}",
            "total":      "${$.node.conn.output.body.page.total}",
            "items":      "${$.node.conn.output.body.data}",
        },
        header_mapping={
            "X-Echo-To-Header": "${$.node.trigger.input.body.X-Echo-To-Header}",
            "X-Connector-Date": "${$.node.conn.output.header.Date}",
        },
    )
    print(f"  [7] Exit configured")

    # ── 步骤 2.7: 保存草稿 ──
    save_flow_draft(page)
    print(f"  [8] Draft saved")

    # ── 步骤 2.8: 调试 ──
    trigger_debug(page,
        header_params={
            "X-App-Id": TEST_APP_ID,
            "Cookie": "user_id=admin",
            "X-XSRF-TOKEN": "user_id=admin",
        },
        query_params={"keyword": "", "pageSize": "3"},
        body_json={"X-Echo-To-Header": "echo-test"},
    )
    print(f"  [9] Debug executed")

    # ── 步骤 2.9: 发布 ──
    publish_flow(page)
    print(f"  [10] Published")

    # ═══════════════════════════════════════════════════════
    # 阶段三: 部署 + 启动 + HTTP 调用验证
    # ═══════════════════════════════════════════════════════

    # ── 步骤 2.10: 部署 + 启动 ──
    navigate_to(page, flow_list_url)
    deploy_and_start_flow(page, flow_name)
    print(f"  [11] Deployed + started")

    # ── 步骤 3.1-3.2: HTTP 调用验证 (重试直到 flow 启动) ──
    max_wait = 30
    started_at = time.time()
    last_status = None
    resp = None
    while time.time() - started_at < max_wait:
        resp = requests.post(
            f"{CONNECTOR_API}/flows/{flow_id}/invoke?keyword=&pageSize=3",
            json={"X-Echo-To-Header": "echo-test"},
            headers={
                "Content-Type": "application/json",
                "X-Sys-Token": "tester",
                "X-App-Id": TEST_APP_ID,
                "Cookie": "user_id=admin",
                "X-XSRF-TOKEN": "user_id=admin",
            },
            timeout=10,
        )
        if resp.status_code == 200:
            break
        last_status = resp.status_code
        time.sleep(3)

    assert resp.status_code == 200, f"V1 FAIL: status={resp.status_code} (last={last_status})"
    body = resp.json()

    assert body.get("code") == "200",       f"V2 FAIL: code={body.get('code')}"
    assert body.get("messageZh") == "操作成功", f"V3 FAIL: messageZh={body.get('messageZh')}"
    assert body.get("total", 0) > 0,        f"V4 FAIL: total={body.get('total')}"
    items = body.get("items", [])
    assert isinstance(items, list) and len(items) > 0, f"V5 FAIL: items empty"

    assert resp.headers.get("X-Echo-To-Header") == "echo-test", \
        f"V6 FAIL: X-Echo-To-Header={resp.headers.get('X-Echo-To-Header')}"
    assert resp.headers.get("X-Connector-Date"), \
        "V7 FAIL: X-Connector-Date missing"

    print(f"\n{'='*60}")
    print(f"  单节点流 E2E 全部 7 项验证通过!")
    print(f"{'='*60}")
    print(f"  flowId = {flow_id}")
    print(f"  code   = {body['code']}")
    print(f"  total  = {body['total']}")
    print(f"  items# = {len(items)}")
    print(f"  X-Echo-To-Header  = {resp.headers.get('X-Echo-To-Header')}")
    print(f"  X-Connector-Date   = {resp.headers.get('X-Connector-Date')}")
