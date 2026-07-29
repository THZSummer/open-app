#!/usr/bin/env python3
"""串行流 E2E 测试: trigger → script₁ → connector → script₂ → exit

通过前端 UI 完成全流程 — 创建连接器 → 创建流 → FlowEditorV2 配置 5 节点串行拓扑 → 调试 → 调用。
"""
import pytest
import json
import requests
import threading
import time
from http.server import ThreadingHTTPServer, BaseHTTPRequestHandler
from urllib.parse import urlparse

from common.client import (
    navigate_to, click_button, fill_form_item, select_option,
    wait_for_table_ready, wait_for_modal_ready, wait_for_message_success,
    wait_for_network_idle,
    create_connector_via_ui, create_flow_via_ui,
    select_flow_mode, add_node, fill_connector_node_config,
    fill_script_node_code, save_flow_draft, debug_flow,
    publish_flow, deploy_and_start_flow,
)

# ═══════════════════════════════════════════════════════════
# Mock Server — 复用 test_single_node 的 MockHandler
# ═══════════════════════════════════════════════════════════

MOCK_PORT = 18999

class MockHandler(BaseHTTPRequestHandler):
    def log_message(self, format, *args): pass

    def do_POST(self):
        content_len = int(self.headers.get("Content-Length", 0))
        body = json.loads(self.rfile.read(content_len)) if content_len > 0 else {}
        self.send_response(200)
        self.send_header("Content-Type", "application/json")
        self.end_headers()
        self.wfile.write(json.dumps({"echo_body": body}).encode())

    def do_GET(self):
        self.send_response(200)
        self.send_header("Content-Type", "application/json")
        self.end_headers()
        self.wfile.write(json.dumps({"code": 0}).encode())

def setup_mock_server():
    ThreadingHTTPServer.allow_reuse_address = True
    server = ThreadingHTTPServer(("localhost", MOCK_PORT), MockHandler)
    t = threading.Thread(target=server.serve_forever, daemon=True)
    t.start()
    for _ in range(10):
        try:
            r = requests.get(f"http://localhost:{MOCK_PORT}/api/echo?keyword=ping", timeout=1)
            if r.status_code == 200: break
        except Exception:
            time.sleep(0.5)
    return server.shutdown


# ═══════════════════════════════════════════════════════════

CONNECTOR_API = "http://localhost:18180/api/v1"

SCRIPT_1 = """function main(ctx) {
    var body = ctx.trigger.input.body;
    return {
        message: "Hello, " + body.name + "!",
        doubled: body.value * 2,
        tags: ["a", "b"]
    };
}"""

SCRIPT_2 = """function main(ctx) {
    var echo = ctx.conn.output.body.echo_body;
    return {
        echoedMessage: echo.message,
        echoedDoubled: echo.doubled
    };
}"""


@pytest.mark.L1
def test_serial(page, connector_list_url, flow_list_url, flow_editor_url):
    """串行 flow: trigger → script₁ → connector → script₂ → exit"""

    shutdown = setup_mock_server()

    try:
        # ── 1. 创建连接器 ──
        navigate_to(page, connector_list_url)
        connector_id = create_connector_via_ui(page, "E2E_EchoBody")

        page.goto(f"{connector_list_url.replace('/connectorList', '/connectorEditor')}?id={connector_id}")
        wait_for_network_idle(page)
        fill_form_item(page, "URL", f"http://localhost:{MOCK_PORT}/api/echo_body")
        select_option(page, "Method", "POST")
        click_button(page, "发布")
        wait_for_message_success(page)

        # ── 2. 创建连接流 ──
        navigate_to(page, flow_list_url)
        flow_id = create_flow_via_ui(page, "E2E_Serial_Flow")

        page.goto(f"{flow_editor_url}?id={flow_id}")
        wait_for_network_idle(page)

        # ── 3. FlowEditorV2 配置 ──
        select_flow_mode(page, "串行")

        # 3a. Trigger: body.name (string), body.value (number)
        fill_form_item(page, "name", "")
        fill_form_item(page, "value", "")

        # 3b. Script₁: 格式转换
        add_node(page, "script")
        fill_script_node_code(page, SCRIPT_1)

        # 3c. Connector
        add_node(page, "connector")
        fill_connector_node_config(page, "E2E_EchoBody", {
            "msg": "${$.node.script_1.output.message}",
            "doubled": "${$.node.script_1.output.doubled}",
            "tags": "${$.node.script_1.output.tags}",
        })

        # 3d. Script₂: 结果抽取
        add_node(page, "script")
        fill_script_node_code(page, SCRIPT_2)

        # 3e. 保存
        save_flow_draft(page)

        # ── 4. 调试 ──
        debug_flow(page, {"name": "OpenApp", "value": 7})
        wait_for_network_idle(page, timeout=30000)

        # ── 5. 发布 + 部署 + 调用 ──
        publish_flow(page)
        navigate_to(page, flow_list_url)
        deploy_and_start_flow(page)

        resp = requests.post(
            f"{CONNECTOR_API}/flows/{flow_id}/invoke",
            json={"name": "OpenApp", "value": 7},
            headers={"Content-Type": "application/json", "X-Sys-Token": "tester"},
            timeout=10,
        )
        assert resp.status_code == 200
        body = resp.json()
        assert body.get("echoedMessage") == "Hello, OpenApp!"
        assert body.get("echoedDoubled") == 14

    finally:
        shutdown()
