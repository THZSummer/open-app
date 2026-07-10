#!/usr/bin/env python3
"""
Serial mode script E2E — trigger → script(ctx.http 条件分支) → exit

Script 内根据入参 action 字段条件路由到不同 mock 端点:
  action="user"   → ctx.http.request('POST', /api/user)    → 返回用户信息
  action="order"  → ctx.http.request('GET', /api/order?...) → 返回订单数据
  action="status" → ctx.http.request('GET', /api/status)    → 返回服务状态
  无 action       → 走默认 user 分支

exit 统一映射脚本返回的 {result, domain, group, path} 到 HTTP 响应。

Prerequisites:
  - open-server running on localhost:18080
  - connector-api running on localhost:18180

Usage:
  cd open-server/src/test/python
  python3 e2e/test_full_flow_script.py
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
from client import CONNECTOR_API_BASE, CONNECTOR_API_HEALTH, SYSTOKEN_HEADER, SYSTOKEN_VALUE

import pytest, requests, random, string

TEST_APP_ID = _osm.TEST_APP_ID
_RUN_ID = ''.join(random.choices(string.ascii_lowercase + string.digits, k=6))
MOCK_PORT = 18986; MOCK_URL = f"http://localhost:{MOCK_PORT}"


# --- shared data transformations ---

def _user_data(name, email, age):
    return {
        "display_name": name.upper(),
        "email_domain": email.split("@")[-1] if "@" in email else "",
        "age_group": "adult" if int(age) >= 18 else "minor",
    }

ORDER_STATUSES = ["delivered", "processing", "shipped", "cancelled", "refunded"]
ORDER_AMOUNTS = [299.90, 99.90, 199.90, 0, 49.95]

def _order_data(name):
    idx = sum(ord(c) for c in name) % 5
    return {
        "status": ORDER_STATUSES[idx],
        "amount": ORDER_AMOUNTS[idx],
        "customer": name.upper(),
    }

STATUS_VERSIONS = [
    {"server": "healthy", "version": "v2.1.0", "uptime_hours": 128},
    {"server": "degraded", "version": "v2.0.9", "uptime_hours": 96},
    {"server": "maintenance", "version": "v2.2.0", "uptime_hours": 72},
]

def _status_data(name):
    return STATUS_VERSIONS[sum(ord(c) for c in name) % 3]


_RUN_DATA = {
    "user": {
        "name": ''.join(random.choices(string.ascii_lowercase, k=5)),
        "email": f"u{_RUN_ID}@company.com",
        "age": 30,
    },
    "order": {
        "name": ''.join(random.choices(string.ascii_lowercase, k=5)),
    },
    "status": {
        "name": ''.join(random.choices(string.ascii_lowercase, k=4)),
    },
    "default": {
        "name": ''.join(random.choices(string.ascii_lowercase, k=5)),
        "email": f"d{_RUN_ID}@school.edu",
        "age": 15,
    },
}


class MockServer:
    """提供 3 个端点供脚本 ctx.http 条件调用"""

    def __init__(self):
        self._server = None

    def _make_handler(self):
        state = {"call_count": 0}

        class H(BaseHTTPRequestHandler):
            def log_message(self, f, *a): pass

            def _json(self, code, body):
                self.send_response(code)
                self.send_header("Content-Type", "application/json")
                self.end_headers()
                self.wfile.write(json.dumps(body, ensure_ascii=False).encode())

            def _body(self):
                cl = int(self.headers.get("Content-Length", 0))
                return json.loads(self.rfile.read(cl)) if cl > 0 else {}

            def _query(self):
                parsed = urlparse(self.path)
                return parse_qs(parsed.query)

            def do_GET(self):
                state["call_count"] += 1
                path = urlparse(self.path).path

                if path == "/api/health":
                    self._json(200, {"status": "ok"})

                elif path == "/api/order":
                    q = self._query()
                    name = q.get("name", ["unknown"])[0]
                    data = _order_data(name)
                    data["order_id"] = f"ORD-{state['call_count']:03d}"
                    self._json(200, {"code": 0, "data": data})

                elif path == "/api/status":
                    q = self._query()
                    name = q.get("name", ["unknown"])[0]
                    self._json(200, {"code": 0, "data": _status_data(name)})

            def do_POST(self):
                state["call_count"] += 1
                body = self._body()
                data = _user_data(
                    body.get("name") or "unknown",
                    body.get("email") or "",
                    body.get("age") or 0,
                )
                data["call_number"] = state["call_count"]
                self._json(200, {"code": 0, "data": data})

        return H

    def start(self):
        self._server = HTTPServer(("localhost", MOCK_PORT), self._make_handler())
        threading.Thread(target=self._server.serve_forever, daemon=True).start()
        for _ in range(20):
            try:
                if requests.get(f"{MOCK_URL}/api/health", timeout=1).status_code == 200:
                    print(f"  [OK] Mock ready :{MOCK_PORT}")
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
def test_full_flow_script():
    print("=" * 60)
    print("  Script E2E: trigger → script(ctx.http 条件分支) → exit")
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

    mock = MockServer()
    if not mock.start():
        print("[FAIL] Mock failed")
        return

    fid = fvid = aid = None
    failed = False

    try:
        print("\n-- Phase 1: Create Flow + Orchestration --")

        def s1():
            nonlocal fid
            fid = snow_id()
            r = os_api("POST", "/flows", {
                "nameCn": f"ScriptBranch_{_RUN_ID}",
                "nameEn": f"script_branch_{_RUN_ID}",
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

            script_src = (
                "function main(ctx) {\n"
                "    var input = ctx.trigger.input.body;\n"
                "    var action = input.action || 'user';\n"
                "    var name = input.name || 'unknown';\n"
                "\n"
                "    if (action === 'order') {\n"
                "        var resp = ctx.http.request('GET', '" + MOCK_URL + "/api/order?name=' + encodeURIComponent(name));\n"
                "        var d = resp.body.data;\n"
                "        return { result: d.order_id, domain: d.status, group: String(d.amount), path: 'order' };\n"
                "    }\n"
                "\n"
                "    if (action === 'status') {\n"
                "        var resp = ctx.http.request('GET', '" + MOCK_URL + "/api/status?name=' + encodeURIComponent(name));\n"
                "        var d = resp.body.data;\n"
                "        return { result: d.server, domain: d.version, group: String(d.uptime_hours) + 'h', path: 'status' };\n"
                "    }\n"
                "\n"
                "    var resp = ctx.http.request('POST', '" + MOCK_URL + "/api/user', {body: {\n"
                "        name: name,\n"
                "        email: input.email || '',\n"
                "        age: input.age || 0\n"
                "    }});\n"
                "    var d = resp.body.data;\n"
                "    return {\n"
                "        result: d.display_name,\n"
                "        domain: d.email_domain,\n"
                "        group: d.age_group,\n"
                "        path: 'user'\n"
                "    };\n"
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
                                    SYSTOKEN_HEADER: {"type": "string", "required": True, "sensitive": True},
                                }},
                                "sysAccountWhitelist": [SYSTOKEN_VALUE],
                            }],
                            "input": {
                                "protocol": "HTTP",
                                "header": {"type": "object", "properties": {}, "required": []},
                                "query": {"type": "object", "properties": {}, "required": []},
                                "body": {
                                    "type": "object",
                                    "properties": {
                                        "action": {"type": "string"},
                                        "name": {"type": "string"},
                                        "email": {"type": "string"},
                                        "age": {"type": "number"},
                                    },
                                    "required": ["name"],
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
                "triggerData": {"action": "user", "name": "debug", "email": "d@t.com", "age": 20},
            })
            ok = check_ok(r, "DEBUG draft")
            if ok:
                print(f"    body: {r.text}")
            return ok

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
            return check_ok(os_api("POST", f"/approvals/{aid}/approve", {"comment": "L2"}, user="admin"), "L2 approve")

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

        print("\n-- Phase 4: Deploy + Multi-Branch Invoke --")
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
            r = api_connector("POST", f"/flows/{fid}/invoke", payload, headers={SYSTOKEN_HEADER: SYSTOKEN_VALUE})
            if r is None:
                os_fail(f"{label}: connector-api unreachable")
                return False
            if r.status_code not in (200, 201):
                os_fail(f"{label}: HTTP {r.status_code}, body={r.text[:200]}")
                return False
            body = r.json()
            print(f"  [OK] {label} HTTP {r.status_code}")
            print(f"    body: {json.dumps(body, ensure_ascii=False)[:300]}")
            ok = True
            for key, val in expected.items():
                actual = body.get(key)
                if actual == val:
                    print(f"    [OK] {key}={val}")
                else:
                    os_fail(f"{label}: {key}={actual}, expected={val}")
                    ok = False
            return ok

        def _expected_user_resp(name, email, age):
            u = _user_data(name, email, age)
            return {"result": u["display_name"], "domain": u["email_domain"], "group": u["age_group"], "path": "user"}

        def _expected_order_resp(name):
            o = _order_data(name)
            return {"domain": o["status"], "group": str(o["amount"]), "path": "order"}

        def _expected_status_resp(name):
            s = _status_data(name)
            return {"result": s["server"], "domain": s["version"], "group": f"{s['uptime_hours']}h", "path": "status"}

        u = _RUN_DATA["user"]
        def s12():
            return _invoke_and_verify("Branch=user POST", {
                "action": "user", "name": u["name"], "email": u["email"], "age": u["age"],
            }, _expected_user_resp(u["name"], u["email"], u["age"]))

        o = _RUN_DATA["order"]
        def s13():
            return _invoke_and_verify("Branch=order GET", {
                "action": "order", "name": o["name"],
            }, _expected_order_resp(o["name"]))

        s = _RUN_DATA["status"]
        def s14():
            return _invoke_and_verify("Branch=status GET", {
                "action": "status", "name": s["name"],
            }, _expected_status_resp(s["name"]))

        d = _RUN_DATA["default"]
        def s15():
            return _invoke_and_verify("Default→user POST", {
                "name": d["name"], "email": d["email"], "age": d["age"],
            }, _expected_user_resp(d["name"], d["email"], d["age"]))

        def s16():
            time.sleep(0.5)
            return check_ok(os_api("POST", f"/flows/{fid}/stop"), "STOP")

        if not failed and not step("DEPLOY", s10): failed = True
        if not failed and not step("START", s11): failed = True
        if not failed and not step("Invoke branch=user (POST /api/user)", s12): failed = True
        if not failed and not step("Invoke branch=order (GET /api/order)", s13): failed = True
        if not failed and not step("Invoke branch=status (GET /api/status)", s14): failed = True
        if not failed and not step("Invoke default→user (no action)", s15): failed = True
        if not failed and not step("STOP", s16): failed = True
        print(f"  {'[OK]' if not failed else '[FAIL]'} Phase 4")

        print("\n" + "=" * 60)
        if failed:
            print("  [FAIL] Script full-flow test FAILED")
        else:
            print("  [PASS] Script full-flow test PASSED!")
        print("=" * 60)
        assert not failed

    finally:
        mock.stop()
        os_done()


if __name__ == "__main__":
    test_full_flow_script()
