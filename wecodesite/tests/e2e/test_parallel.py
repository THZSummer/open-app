#!/usr/bin/env python3
"""并行流 E2E 测试: trigger → script → [parallel] → conn_a / conn_b → merge → exit

通过前端 UI 完成全流程 — 创建 2 个连接器 → 创建流 → FlowEditorV2 配置 6 节点并行拓扑 → 调试 → 调用。
"""
import pytest
import json
import requests
import threading
import time
from http.server import ThreadingHTTPServer, BaseHTTPRequestHandler
from urllib.parse import urlparse, parse_qs

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
# Mock Server
# ═══════════════════════════════════════════════════════════

MOCK_PORT = 18999

class MockHandler(BaseHTTPRequestHandler):
    def log_message(self, format, *args): pass

    def do_GET(self):
        parsed = urlparse(self.path)
        params = parse_qs(parsed.query)
        keyword = params.get("keyword", [""])[0]
        if parsed.path == "/api/branch-a":
            data = {"service": "branch-a", "items": [{"id": "a1", "val": 100, "keyword": keyword}],
                    "date": "Mon, 28 Jul 2026 00:00:00 GMT"}
        elif parsed.path == "/api/branch-b":
            data = {"service": "branch-b", "items": [{"id": "b1", "val": 200, "keyword": keyword}],
                    "date": "Mon, 28 Jul 2026 00:00:01 GMT"}
        else:
            data = {"code": 0}
        self._send_json(200, data)

    def _send_json(self, status, body):
        self.send_response(status)
        self.send_header("Content-Type", "application/json")
        self.end_headers()
        self.wfile.write(json.dumps(body).encode())

def setup_mock_server():
    ThreadingHTTPServer.allow_reuse_address = True
    server = ThreadingHTTPServer(("localhost", MOCK_PORT), MockHandler)
    t = threading.Thread(target=server.serve_forever, daemon=True)
    t.start()
    for _ in range(10):
        try:
            r = requests.get(f"http://localhost:{MOCK_PORT}/api/branch-a?keyword=ping", timeout=1)
            if r.status_code == 200: break
        except Exception:
            time.sleep(0.5)
    return server.shutdown


# ═══════════════════════════════════════════════════════════

CONNECTOR_API = "http://localhost:18180/api/v1"

SCRIPT_PREPARE = """function main(ctx) {
    var q = ctx.trigger.input.query || {};
    var body = ctx.trigger.input.body || {};
    return {
        keyword: q.keyword || "",
        pageSize: q.pageSize || 3,
        echoTo: body["X-Echo-To-Header"] || ""
    };
}"""

SCRIPT_MERGE = """function main(ctx) {
    var a = ctx.conn_a.output.body;
    var b = ctx.conn_b.output.body;
    var p = ctx.script_prepare.output;
    return {
        code: "200",
        a_items: a.items || [],
        b_items: b.items || [],
        a_date: a.date || "",
        b_date: b.date || "",
        echoTo: p.echoTo || ""
    };
}"""


@pytest.mark.L1
def test_parallel(page, connector_list_url, flow_list_url, flow_editor_url):
    """并行 flow: trigger → script → [parallel] → conn_a/conn_b → merge → exit"""

    shutdown = setup_mock_server()

    try:
        # ── 1. 创建两个连接器 ──
        navigate_to(page, connector_list_url)
        cid_a = create_connector_via_ui(page, "E2E_BranchA")
        page.goto(f"{connector_list_url.replace('/connectorList', '/connectorEditor')}?id={cid_a}")
        wait_for_network_idle(page)
        fill_form_item(page, "URL", f"http://localhost:{MOCK_PORT}/api/branch-a")
        select_option(page, "Method", "GET")
        click_button(page, "发布")
        wait_for_message_success(page)

        navigate_to(page, connector_list_url)
        cid_b = create_connector_via_ui(page, "E2E_BranchB")
        page.goto(f"{connector_list_url.replace('/connectorList', '/connectorEditor')}?id={cid_b}")
        wait_for_network_idle(page)
        fill_form_item(page, "URL", f"http://localhost:{MOCK_PORT}/api/branch-b")
        select_option(page, "Method", "GET")
        click_button(page, "发布")
        wait_for_message_success(page)

        # ── 2. 创建连接流 ──
        navigate_to(page, flow_list_url)
        flow_id = create_flow_via_ui(page, "E2E_Parallel_Flow")

        page.goto(f"{flow_editor_url}?id={flow_id}")
        wait_for_network_idle(page)

        # ── 3. FlowEditorV2 配置 ──
        select_flow_mode(page, "并行")

        # 3a. Trigger: query.keyword, query.pageSize, body.X-Echo-To-Header
        fill_form_item(page, "keyword", "")
        fill_form_item(page, "pageSize", "")

        # 3b. Script_prepare
        add_node(page, "script")
        fill_script_node_code(page, SCRIPT_PREPARE)

        # 3c. Parallel 分叉 + Connector A / B
        # FlowEditorV2 并行模式下, 添加节点后自动分叉
        add_node(page, "connector")
        fill_connector_node_config(page, "E2E_BranchA", {
            "keyword": "${$.node.script_prepare.output.keyword}",
            "pageSize": "${$.node.script_prepare.output.pageSize}",
        })

        add_node(page, "connector")
        fill_connector_node_config(page, "E2E_BranchB", {
            "keyword": "${$.node.script_prepare.output.keyword}",
            "pageSize": "${$.node.script_prepare.output.pageSize}",
        })

        # 3d. Script_merge
        add_node(page, "script")
        fill_script_node_code(page, SCRIPT_MERGE)

        # 3e. 保存
        save_flow_draft(page)

        # ── 4. 调试 ──
        debug_flow(page, {"X-Echo-To-Header": "echo-test"})
        wait_for_network_idle(page, timeout=30000)

        # ── 5. 发布 + 部署 + 调用 ──
        publish_flow(page)
        navigate_to(page, flow_list_url)
        deploy_and_start_flow(page)

        resp = requests.post(
            f"{CONNECTOR_API}/flows/{flow_id}/invoke?keyword=test&pageSize=3",
            json={"X-Echo-To-Header": "echo-test"},
            headers={"Content-Type": "application/json", "X-Sys-Token": "tester"},
            timeout=10,
        )
        assert resp.status_code == 200
        body = resp.json()
        assert len(body.get("a_items", [])) > 0, "a_items 应有数据"
        assert len(body.get("b_items", [])) > 0, "b_items 应有数据"
        assert resp.headers.get("X-Echo-To-Header") == "echo-test"
        assert resp.headers.get("X-Branch-A-Date")
        assert resp.headers.get("X-Branch-B-Date")

    finally:
        shutdown()
