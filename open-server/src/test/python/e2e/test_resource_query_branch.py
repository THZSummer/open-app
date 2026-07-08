#!/usr/bin/env python3
"""
连接器或连接流列表数据查询 E2E — trigger → script(ctx.http 条件分支) → exit

场景: Script 内根据 type 路由到不同 OpenAPI:
  type="connectors" → 查询连接器列表
  type="flows"      → 查询连接流列表
  无 type            → 默认查连接器

Mock 作为 Auth Proxy: 补 X-App-Id/Cookie Header 后透传到真实 open-server API。
数据完全真实，不做假数据。

Prerequisites:
  - open-server running on localhost:18080
  - connector-api running on localhost:18180

Usage:
  cd open-server/src/test/python
  python3 e2e/test_resource_query_branch.py
"""
import os, sys, json, time, threading
import importlib.util
from http.server import HTTPServer, BaseHTTPRequestHandler
from urllib.parse import urlparse, parse_qs

TEST_DIR = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(os.path.dirname(TEST_DIR), "common"))

_spec = importlib.util.spec_from_file_location(
    "common_client", os.path.join(os.path.dirname(TEST_DIR), "common", "client.py"))
_osm = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(_osm)
os_api = _osm.api
os_ok = _osm.ok
os_fail = _osm.fail
os_done = _osm.done
from client import CONNECTOR_API_BASE, CONNECTOR_API_HEALTH, OPEN_SERVER_BASE, TEST_APP_ID

import pytest, requests, random, string

_RUN_ID = ''.join(random.choices(string.ascii_lowercase + string.digits, k=6))
MOCK_PORT = 18989; MOCK_URL = f"http://localhost:{MOCK_PORT}"
OPEN_API_BASE = "http://localhost:18080/open-server/service/open/v2"


class AuthProxy:
    """透传代理: 添加 X-App-Id Header 后转发到真实 open-server API"""

    def __init__(self):
        self._server = None
        self._headers = {
            "X-App-Id": TEST_APP_ID,
            "Cookie": "user_id=admin",
        }

    def _make_handler(self):
        class H(BaseHTTPRequestHandler):
            def log_message(self, f, *a): pass

            def _respond(self, code, headers, body):
                self.send_response(code)
                for k, v in headers.items():
                    self.send_header(k, v)
                self.end_headers()
                if body:
                    self.wfile.write(body)

            def _proxy(self):
                q = urlparse(self.path).query
                real_url = f"{OPEN_API_BASE}{self.path}"
                try:
                    r = requests.get(real_url, headers=self._headers, timeout=10)
                    self._respond(r.status_code, dict(r.headers), r.content)
                except Exception as e:
                    self._respond(502, {"Content-Type": "application/json"},
                                  json.dumps({"code": 502, "message": str(e)}).encode())

            def do_GET(self):
                path = urlparse(self.path).path
                if path == "/api/health":
                    self._respond(200, {"Content-Type": "application/json"},
                                  json.dumps({"status": "ok"}).encode())
                elif path.startswith("/service/open/v2/"):
                    self._proxy()
                else:
                    self._respond(404, {"Content-Type": "application/json"},
                                  json.dumps({"code": 404, "message": "unknown"}).encode())

            def do_POST(self):
                self._respond(405, {"Content-Type": "application/json"},
                              json.dumps({"code": 405, "message": "method not allowed"}).encode())

        # 将当前实例的 headers 绑定到 handler 类
        H._headers = self._headers
        return H

    def start(self):
        self._server = HTTPServer(("localhost", MOCK_PORT), self._make_handler())
        threading.Thread(target=self._server.serve_forever, daemon=True).start()
        for _ in range(20):
            try:
                if requests.get(f"{MOCK_URL}/api/health", timeout=1).status_code == 200:
                    print(f"  [OK] AuthProxy ready :{MOCK_PORT}")
                    return True
            except Exception:
                pass
            time.sleep(0.5)
        return False

    def stop(self):
        if self._server:
            self._server.shutdown()


def snow_id():
    return int(time.time() * 1000000) % 100000000000000000


def api_connector(method, path, body=None, headers=None):
    url = f"{CONNECTOR_API_BASE}{path}"
    h = {"Content-Type": "application/json"}
    if headers:
        h.update(headers)
    try:
        return requests.request(method, url, json=body, headers=h, timeout=10)
    except requests.ConnectionError:
        return None


_step = [0]


def step(desc, fn):
    _step[0] += 1
    print(f"\n  [{_step[0]}] {desc}")
    try:
        return fn()
    except Exception as e:
        os_fail(f"Step {_step[0]} exception: {e}")
        return False


def check_ok(resp, desc, url=""):
    if resp is None:
        os_fail(f"{desc}: connection failed {url}")
        return False
    if resp.status_code == 200:
        try:
            b = resp.json()
        except Exception:
            print(f"  [OK] {desc} (HTTP 200)  {url}")
            return True
        if b.get("code") in ("200", 200):
            print(f"  [OK] {desc}  {url}")
            return True
        os_fail(f"{desc}: code={b.get('code')}  {url}")
        return False
    else:
        os_fail(f"{desc}: HTTP {resp.status_code}  {url}")
        return False


def get_data(resp):
    try:
        return resp.json().get("data", {})
    except Exception:
        return {}


@pytest.mark.L3
def test_resource_query_branch():
    print("=" * 60)
    print("  连接器或连接流列表数据查询")
    print("  trigger → script(ctx.http 条件分支 → AuthProxy → 真实API) → exit")
    print(f"  Run: {_RUN_ID}")
    print("=" * 60)

    print("\n-- Phase 0: Environment Check --")
    r = os_api("GET", "/connectors")
    if r is None:
        print("[FAIL] open-server not running!")
        return
    print("  [OK] open-server ready")
    try:
        if requests.get(CONNECTOR_API_HEALTH, timeout=5).status_code == 200:
            print("  [OK] connector-api ready")
    except Exception:
        print("[FAIL] connector-api not running!")
        return

    tpl = None
    r = os_api("GET", "/approval-flows?keyword=connector_flow_version_publish")
    if r and r.status_code == 200:
        for item in r.json().get("data", []):
            if item.get("code") == "connector_flow_version_publish":
                tpl = item.get("id")
                break
    if not tpl:
        print("  Creating approval template...")
        os_api("POST", "/approval-flows", {
            "code": "connector_flow_version_publish",
            "nameCn": "审批", "nameEn": "approval",
            "appId": TEST_APP_ID,
            "nodes": [{"userId": "tester", "userName": "Test Approver"}],
        })
    print("  [OK] approval template ready")

    proxy = AuthProxy()
    if not proxy.start():
        print("[FAIL] AuthProxy failed to start")
        return

    fid = fvid = aid = None
    failed = False

    try:
        print("\n-- Phase 1: Create Flow + Orchestration --")

        def s1():
            nonlocal fid
            fid = snow_id()
            r = os_api("POST", "/flows", {
                "nameCn": f"ResourceQuery_{_RUN_ID}",
                "nameEn": f"resource_query_{_RUN_ID}",
            })
            if not check_ok(r, "CREATE flow"):
                return False
            d = get_data(r)
            if str(d.get("flowId")) != str(fid):
                fid = int(d.get("flowId"))
            print(f"    flowId={fid}")
            return True

        def s2():
            nonlocal fvid
            r = os_api("POST", f"/flows/{fid}/versions", {})
            if not check_ok(r, "CREATE draft version"):
                return False
            fvid = int(get_data(r).get("versionId", 0))
            print(f"    versionId={fvid}")
            return True

        def s3():
            nid_t = "trigger"
            nid_s = "script"
            nid_e = "exit"

            Q = "'"
            script_src = (
                "function main(ctx) {\n"
                "    var input = ctx.trigger.input.body;\n"
                "    var type = input.type || 'connectors';\n"
                "    var keyword = input.keyword || '';\n"
                "    var curPage = input.curPage || 1;\n"
                "    var pageSize = input.pageSize || 10;\n"
                "    var baseUrl = '" + MOCK_URL + "/service/open/v2';\n"
                "\n"
                "    function formatItems(items, keys) {\n"
                "      var out = '';\n"
                "      for (var i = 0; i < items.length; i++) {\n"
                "        if (i > 0) out += '|';\n"
                "        for (var k = 0; k < keys.length; k++) {\n"
                "          if (k > 0) out += ',';\n"
                "          out += items[i][keys[k]];\n"
                "        }\n"
                "      }\n"
                "      return out;\n"
                "    }\n"
                "\n"
                "    var path = (type === 'flows') ? '/flows' : '/connectors';\n"
                "    var resp = ctx.http.get(baseUrl + path + " + Q + "?curPage=" + Q + " + curPage + " + Q + "&keyword=" + Q + " + encodeURIComponent(keyword) + " + Q + "&pageSize=" + Q + " + pageSize);\n"
                "    var items = resp.body.data || [];\n"
                "    var keys = (type === 'flows') ? ['flowId', 'nameCn', 'lifecycleStatus'] : ['connectorId', 'nameCn', 'status'];\n"
                "    return { result: formatItems(items, keys), domain: String(items.length), group: items.length > 0 ? items[0].nameCn : '-', path: type === 'flows' ? 'flows' : 'connectors' };\n"
                "}"
            )

            r = os_api("PUT", f"/flows/{fid}/versions/{fvid}", {
                "orchestrationConfig": {
                    "nodes": [
                        {"id": nid_t, "type": "trigger", "data": {
                            "type": "trigger", "triggerType": "http",
                            "authConfigs": [{
                                "type": "SYSTOKEN",
                                "header": {"type": "object", "properties": {
                                    "X-Sys-Token": {"type": "string", "required": True, "sensitive": True},
                                }},
                                "sysAccountWhitelist": ["tester"],
                            }],
                            "input": {
                                "protocol": "HTTP",
                                "header": {"type": "object", "properties": {}, "required": []},
                                "query": {"type": "object", "properties": {}, "required": []},
                                "body": {
                                    "type": "object",
                                    "properties": {
                                        "type": {"type": "string"},
                                        "keyword": {"type": "string"},
                                        "curPage": {"type": "number"},
                                        "pageSize": {"type": "number"},
                                    },
                                    "required": [],
                                },
                            },
                        }},
                        {"id": nid_s, "type": "script", "data": {
                            "type": "script",
                            "script": script_src,
                            "timeoutMs": 5000,
                            "output": {
                                "type": "object",
                                "properties": {
                                    "result": {"type": "string"},
                                    "domain": {"type": "string"},
                                    "group": {"type": "string"},
                                    "path": {"type": "string"},
                                },
                            },
                        }},
                        {"id": nid_e, "type": "exit", "data": {
                            "type": "exit",
                            "output": {
                                "header": {"type": "object", "properties": {}},
                                "body": {
                                    "type": "object",
                                    "properties": {
                                        "result": {"type": "string", "value": "${$.node.script.output.result}"},
                                        "domain": {"type": "string", "value": "${$.node.script.output.domain}"},
                                        "group": {"type": "string", "value": "${$.node.script.output.group}"},
                                        "path": {"type": "string", "value": "${$.node.script.output.path}"},
                                    },
                                },
                            },
                        }},
                    ],
                    "edges": [
                        {"id": "e1", "source": nid_t, "target": nid_s},
                        {"id": "e2", "source": nid_s, "target": nid_e},
                    ],
                    "flowConfig": {"flowMode": "serial", "rateLimitConfig": {"maxQps": 50}},
                },
            })
            return check_ok(r, "UPDATE orchestration", f"PUT /flows/{fid}/versions/{fvid}")

        if not step("CREATE flow", s1): failed = True
        if not failed and not step("CREATE draft version", s2): failed = True
        if not failed and not step("UPDATE orchestration", s3): failed = True
        print(f"  {'[OK]' if not failed else '[FAIL]'} Phase 1")

        print("\n-- Phase 2: Debug Draft --")
        def s4():
            r = os_api("POST", f"/flows/{fid}/versions/{fvid}/debug", {
                "triggerData": {"type": "connectors", "keyword": "debug"},
            })
            return check_ok(r, "DEBUG draft")

        if not failed and not step("DEBUG draft", s4): failed = True
        print(f"  {'[OK]' if not failed else '[FAIL]'} Phase 2")

        print("\n-- Phase 3: Publish + Approve --")
        def s5():
            return check_ok(os_api("POST", f"/flows/{fid}/versions/{fvid}/publish"), "PUBLISH submit")

        def s6():
            nonlocal aid
            r = os_api("GET", "/approvals/pending?businessType=connector_flow_version_publish&page=1&size=50")
            if r and r.status_code == 200:
                for item in r.json().get("data", []):
                    if str(item.get("businessId")) == str(fvid):
                        aid = item.get("id")
                        break
            if not aid:
                os_fail("Approval record not found")
                return False
            print(f"    approvalId={aid}")
            return True

        def s7():
            return check_ok(os_api("POST", f"/approvals/{aid}/approve", {"comment": "L1"}, user="tester"), "L1 approve")

        def s8():
            return check_ok(os_api("POST", f"/approvals/{aid}/approve", {"comment": "L2"}, user="tester"), "L2 approve")

        def s9():
            time.sleep(0.5)
            r = os_api("GET", f"/flows/{fid}/versions/{fvid}")
            st = get_data(r).get("status")
            if st not in (5, "5"):
                time.sleep(2)
                st = get_data(os_api("GET", f"/flows/{fid}/versions/{fvid}")).get("status")
            if st in (5, "5"):
                print("    status=5 (published)")
                return True
            os_fail(f"status={st}, expected=5")
            return False

        if not failed and not step("PUBLISH submit", s5): failed = True
        if not failed and not step("Find approval", s6): failed = True
        if not failed and not step("L1 approve", s7): failed = True
        if not failed and not step("L2 approve", s8): failed = True
        if not failed and not step("Verify published", s9): failed = True
        print(f"  {'[OK]' if not failed else '[FAIL]'} Phase 3")

        print("\n-- Phase 4: Deploy + Real-API Invoke --")
        def s10():
            return check_ok(os_api("POST", f"/flows/{fid}/deploy", {"versionId": fvid}), "DEPLOY")

        def s11():
            r = os_api("POST", f"/flows/{fid}/start")
            if not check_ok(r, "START"):
                return False
            ls = get_data(os_api("GET", f"/flows/{fid}")).get("lifecycleStatus")
            if ls not in (2, "2"):
                os_fail(f"lifecycleStatus={ls}")
                return False
            return True

        def _invoke_and_verify(label, payload, expected):
            r = api_connector("POST", f"/flows/{fid}/invoke", payload, headers={"X-Sys-Token": "tester"})
            if r is None:
                os_fail(f"{label}: connector-api unreachable")
                return False
            if r.status_code not in (200, 201):
                os_fail(f"{label}: HTTP {r.status_code}, body={r.text[:200]}")
                return False
            body = r.json()
            print(f"  [OK] {label} HTTP {r.status_code}")
            print(f"    body: {json.dumps(body, ensure_ascii=False)[:400]}")
            ok = True
            for key, val in expected.items():
                actual = body.get(key)
                if actual == val:
                    print(f"    [OK] {key}={val}")
                else:
                    os_fail(f"{label}: {key}={actual}, expected={val}")
                    ok = False
            return ok

        def _fmt(items, keys):
            return '|'.join(','.join(str(it[k]) for k in keys) for it in items[:10])

        def _expected_connectors():
            r = os_api("GET", "/connectors")
            items = r.json().get("data", []) if r else []
            return {"result": _fmt(items, ["connectorId", "nameCn", "status"]),
                    "domain": str(min(len(items), 10)),
                    "group": items[0]["nameCn"] if items else "-", "path": "connectors"}

        def _expected_flows():
            r = os_api("GET", "/flows")
            items = r.json().get("data", []) if r else []
            return {"result": _fmt(items, ["flowId", "nameCn", "lifecycleStatus"]),
                    "domain": str(min(len(items), 10)),
                    "group": items[0]["nameCn"] if items else "-", "path": "flows"}

        def s12():
            return _invoke_and_verify("Type=connectors -> 真实API", {}, _expected_connectors())

        def s13():
            return _invoke_and_verify("Type=flows -> 真实API", {"type": "flows"}, _expected_flows())

        def s14():
            time.sleep(0.5)
            return check_ok(os_api("POST", f"/flows/{fid}/stop"), "STOP")

        if not failed and not step("DEPLOY", s10): failed = True
        if not failed and not step("START", s11): failed = True
        if not failed and not step("Invoke type=connectors", s12): failed = True
        if not failed and not step("Invoke type=flows", s13): failed = True
        if not failed and not step("STOP", s14): failed = True
        print(f"  {'[OK]' if not failed else '[FAIL]'} Phase 4")

        print("\n" + "=" * 60)
        if failed:
            print("  [FAIL] Resource query branch test FAILED")
        else:
            print("  [PASS] Resource query branch test PASSED!")
        print("=" * 60)
        assert not failed

    finally:
        proxy.stop()
        os_done()


if __name__ == "__main__":
    test_resource_query_branch()
