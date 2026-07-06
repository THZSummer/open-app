#!/usr/bin/env python3
"""
Script node full-flow E2E test — trigger → script(ctx.http) → exit

3-node flow, script directly calls mock HTTP API via ctx.http:
  trigger  — receives {name, email, age}
  script   — ctx.trigger.input.body → ctx.http.post(mock/api/user) → return result
  exit     — maps script output to response

Parameter chain:
  trigger.body {name, email, age}
    → ctx.trigger.input.body
      → script reads, calls ctx.http.post(mock/api/user, {name, email, age})
        → mock returns {display_name, email_domain, age_group}
          → exit maps: {result, domain, group}

Prerequisites:
  - open-server running on localhost:18080
  - connector-api running on localhost:18180

Usage:
  cd open-server/src/test/python
  python3 test_full_flow_script.py
  KEEP_TEST_DATA=0 python3 test_full_flow_script.py  # auto cleanup
"""
import os, sys, json, time, threading
import importlib.util
from http.server import HTTPServer, BaseHTTPRequestHandler

TEST_DIR = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(os.path.dirname(TEST_DIR), "common"))

_spec = importlib.util.spec_from_file_location(
    "common_client", os.path.join(os.path.dirname(TEST_DIR), "common", "client.py"))
_osm = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(_osm)
os_api = _osm.api
os_db = _osm.db
os_db_val = _osm.db_val
os_ok = _osm.ok
os_fail = _osm.fail
os_done = _osm.done
from client import CONNECTOR_API_BASE, CONNECTOR_API_HEALTH

import pytest, requests, random, string

TEST_APP_ID = _osm.TEST_APP_ID
INTERNAL_APP_ID = int(os_db_val(f"SELECT id FROM openplatform_app_t WHERE app_id = '{TEST_APP_ID}' AND status = 1"))
KEEP = os.environ.get("KEEP_TEST_DATA", "1") == "1"
_RUN_ID = ''.join(random.choices(string.ascii_lowercase + string.digits, k=6))
MOCK_PORT = 18986; MOCK_URL = f"http://localhost:{MOCK_PORT}"


# ═══════════════════════════════════════════════════════════
# Mock HTTP Server
# ═══════════════════════════════════════════════════════════

class MockServer:
    def __init__(self):
        self._server = None

    def _make_handler(self):
        state = {"call_count": 0}

        class H(BaseHTTPRequestHandler):
            def log_message(self, f, *a): pass
            def _json(self, code, body):
                self.send_response(code); self.send_header("Content-Type", "application/json")
                self.end_headers(); self.wfile.write(json.dumps(body, ensure_ascii=False).encode())
            def _body(self):
                cl = int(self.headers.get("Content-Length", 0))
                return json.loads(self.rfile.read(cl)) if cl > 0 else {}
            def do_GET(self):
                if self.path == "/api/health": self._json(200, {"status": "ok"})
            def do_POST(self):
                body = self._body(); state["call_count"] += 1
                name = body.get("name") or "unknown"; email = body.get("email") or ""
                age = body.get("age") or 0
                self._json(200, {
                    "code": 0,
                    "data": {
                        "display_name": name.upper(),
                        "email_domain": email.split("@")[-1] if "@" in email else "",
                        "age_group": "adult" if int(age) >= 18 else "minor",
                        "call_number": state["call_count"]
                    }
                })
        return H

    def start(self):
        self._server = HTTPServer(("localhost", MOCK_PORT), self._make_handler())
        threading.Thread(target=self._server.serve_forever, daemon=True).start()
        for _ in range(20):
            try:
                if requests.get(f"{MOCK_URL}/api/health", timeout=1).status_code == 200:
                    print(f"  [OK] Mock ready :{MOCK_PORT}"); return True
            except Exception: pass
            time.sleep(0.5)
        return False

    def stop(self):
        if self._server: self._server.shutdown()


# ═══════════════════════════════════════════════════════════
# Helpers
# ═══════════════════════════════════════════════════════════

def snow_id(): return int(time.time() * 1000000) % 100000000000000000
def api_connector(method, path, body=None, headers=None):
    url = f"{CONNECTOR_API_BASE}{path}"; h = {"Content-Type": "application/json"}
    if headers: h.update(headers)
    try: return requests.request(method, url, json=body, headers=h, timeout=10)
    except requests.ConnectionError: return None

_step = [0]
def step(desc, fn):
    _step[0] += 1; print(f"\n  [{_step[0]}] {desc}")
    try: return fn()
    except Exception as e: os_fail(f"Step {_step[0]} exception: {e}"); return False

def check_ok(resp, desc, url=""):
    if resp is None: os_fail(f"{desc}: connection failed {url}"); return False
    if resp.status_code == 200:
        try: b = resp.json()
        except: print(f"  [OK] {desc} (HTTP 200)  {url}"); return True
        if b.get("code") in ("200", 200): print(f"  [OK] {desc}  {url}"); return True
        os_fail(f"{desc}: code={b.get('code')}  {url}"); return False
    else:
        os_fail(f"{desc}: HTTP {resp.status_code}  {url}"); return False

def get_data(resp):
    try: return resp.json().get("data", {})
    except: return {}


# ═══════════════════════════════════════════════════════════
# Test
# ═══════════════════════════════════════════════════════════

@pytest.mark.L3
def test_full_flow_script():
    print("=" * 60)
    print("  Script Full-Flow E2E: trigger → script(ctx.http) → exit")
    print(f"  Run: {_RUN_ID}")
    print("=" * 60)

    # Phase 0: env check
    print("\n-- Phase 0: Environment Check --")
    r = os_api("GET", "/connectors")
    if r is None: print("[FAIL] open-server not running!"); return
    print("  [OK] open-server ready")
    try:
        if requests.get(CONNECTOR_API_HEALTH, timeout=5).status_code == 200:
            print("  [OK] connector-api ready")
    except: print("[FAIL] connector-api not running!"); return

    # check approval template
    tpl = os_db_val(f"SELECT id FROM openplatform_v2_approval_flow_t WHERE code='connector_flow_version_publish' AND app_id={INTERNAL_APP_ID} LIMIT 1")
    if not tpl: tpl = os_db_val("SELECT id FROM openplatform_v2_approval_flow_t WHERE code='connector_flow_version_publish' AND app_id IS NULL LIMIT 1")
    if not tpl:
        print("  Creating approval template...")
        r = os_api("POST", "/approval-flows", {"code": "connector_flow_version_publish", "nameCn": "审批", "nameEn": "approval", "appId": TEST_APP_ID, "nodes": [{"userId": "tester", "userName": "Test Approver"}]})
    print("  [OK] approval template ready")

    # start mock
    mock = MockServer()
    if not mock.start(): print("[FAIL] Mock failed"); return

    fid = fvid = aid = None
    failed = False

    try:
        # ═══════════════════════════════════════════════════
        # Phase 1: Create flow + 3-node orchestration
        # ═══════════════════════════════════════════════════
        print("\n-- Phase 1: Create Flow + Orchestration (3 nodes) --")

        def s1():
            nonlocal fid; fid = snow_id()
            r = os_api("POST", "/flows", {"nameCn": f"Script3Node_{_RUN_ID}", "nameEn": f"script3n_{_RUN_ID}"})
            if not check_ok(r, "CREATE flow"): return False
            d = get_data(r)
            if str(d.get("flowId")) != str(fid): fid = int(d.get("flowId"))
            print(f"    flowId={fid}"); return True

        def s2():
            nonlocal fvid
            r = os_api("POST", f"/flows/{fid}/versions", {})
            if not check_ok(r, "CREATE draft version"): return False
            fvid = int(get_data(r).get("versionId", 0)); print(f"    versionId={fvid}"); return True

        def s3():
            nid_t = "trigger"; nid_s = "script"; nid_e = "exit"

            # Script: trigger params → ctx.http.post → mock /api/user → return result
            script_src = (
                "function main(ctx) {\n"
                "    var input = ctx.trigger.input.body;\n"
                "    var resp = ctx.http.post('" + MOCK_URL + "/api/user', {\n"
                "        name: input.name,\n"
                "        email: input.email,\n"
                "        age: input.age\n"
                "    });\n"
                "    var data = resp.body.data;\n"
                "    return {\n"
                "        result: data.display_name,\n"
                "        domain: data.email_domain,\n"
                "        group: data.age_group,\n"
                "        call_no: data.call_number\n"
                "    };\n"
                "}"
            )

            r = os_api("PUT", f"/flows/{fid}/versions/{fvid}", {"orchestrationConfig": {
                "nodes": [
                    {"id": nid_t, "type": "trigger", "data": {
                        "type": "trigger", "triggerType": "http",
                        "authConfigs": [{"type": "SYSTOKEN", "header": {"type": "object",
                            "properties": {"X-Sys-Token": {"type": "string", "required": True, "sensitive": True}}},
                            "sysAccountWhitelist": ["tester"]}],
                        "input": {
                            "protocol": "HTTP",
                            "header": {"type": "object", "properties": {}, "required": []},
                            "query": {"type": "object", "properties": {}, "required": []},
                            "body": {
                                "type": "object",
                                "properties": {"name": {"type": "string"}, "email": {"type": "string"}, "age": {"type": "number"}},
                                "required": ["name"]
                            }
                        }
                    }},
                    {"id": nid_s, "type": "script", "data": {
                        "type": "script",
                        "script": script_src, "timeoutMs": 5000,
                        "output": {
                            "type": "object",
                            "properties": {
                                "result": {"type": "string", "description": "display_name from mock"},
                                "domain": {"type": "string", "description": "email_domain from mock"},
                                "group": {"type": "string", "description": "age_group from mock"},
                                "call_no": {"type": "number", "description": "call_number from mock"}
                            }
                        }
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
                                    "callNo": {"type": "number", "value": "${$.node.script.output.call_no}"}
                                }
                            }
                        }
                    }}
                ],
                "edges": [
                    {"id": "e1", "source": nid_t, "target": nid_s},
                    {"id": "e2", "source": nid_s, "target": nid_e}
                ],
                "flowConfig": {"flowMode": "serial", "rateLimitConfig": {"maxQps": 50}}
            }})
            return check_ok(r, "UPDATE orchestration (trigger→script→exit)", f"PUT /flows/{fid}/versions/{fvid}")

        if not step("CREATE flow", s1): failed = True
        if not failed and not step("CREATE draft version", s2): failed = True
        if not failed and not step("UPDATE orchestration (3 nodes)", s3): failed = True
        print(f"  {'[OK]' if not failed else '[FAIL]'} Phase 1")

        # ═══════════════════════════════════════════════════
        # Phase 2: Debug draft
        # ═══════════════════════════════════════════════════
        if not failed:
            print("\n-- Phase 2: Debug Draft --")
        def s4():
            r = os_api("POST", f"/flows/{fid}/versions/{fvid}/debug",
                       {"triggerData": {"name": "debug_user", "email": "d@test.com", "age": 25}})
            return check_ok(r, "DEBUG draft", f"POST /flows/{fid}/versions/{fvid}/debug")
        if not failed and not step("DEBUG draft", s4): failed = True
        print(f"  {'[OK]' if not failed else '[FAIL]'} Phase 2")

        # ═══════════════════════════════════════════════════
        # Phase 3: Publish + approve
        # ═══════════════════════════════════════════════════
        if not failed:
            print("\n-- Phase 3: Publish + Approve --")
        def s5():
            r = os_api("POST", f"/flows/{fid}/versions/{fvid}/publish")
            return check_ok(r, "PUBLISH submit", f"POST /flows/{fid}/versions/{fvid}/publish")
        def s6():
            nonlocal aid
            aid = os_db_val(f"SELECT id FROM openplatform_v2_approval_record_t WHERE business_type='connector_flow_version_publish' AND business_id={fvid} ORDER BY create_time DESC LIMIT 1")
            if not aid: os_fail("Approval record not found"); return False
            print(f"    approvalId={aid}"); return True
        def s7():
            return check_ok(os_api("POST", f"/approvals/{aid}/approve", {"comment": "L1"}, user="tester"), "L1 approve")
        def s8():
            return check_ok(os_api("POST", f"/approvals/{aid}/approve", {"comment": "L2"}, user="tester"), "L2 approve")
        def s9():
            time.sleep(0.5); r = os_api("GET", f"/flows/{fid}/versions/{fvid}")
            st = get_data(r).get("status")
            if st not in (5, "5"): time.sleep(2); st = get_data(os_api("GET", f"/flows/{fid}/versions/{fvid}")).get("status")
            if st in (5, "5"): print("    status=5 (published)"); return True
            os_fail(f"status={st}, expected=5"); return False
        if not failed and not step("PUBLISH submit", s5): failed = True
        if not failed and not step("Find approval", s6): failed = True
        if not failed and not step("L1 approve", s7): failed = True
        if not failed and not step("L2 approve", s8): failed = True
        if not failed and not step("Verify published", s9): failed = True
        print(f"  {'[OK]' if not failed else '[FAIL]'} Phase 3")

        # ═══════════════════════════════════════════════════
        # Phase 4: Deploy + Invoke + Verify
        # ═══════════════════════════════════════════════════
        if not failed:
            print("\n-- Phase 4: Deploy + Invoke + Verify --")
        def s10():
            return check_ok(os_api("POST", f"/flows/{fid}/deploy", {"versionId": fvid}), "DEPLOY")
        def s11():
            r = os_api("POST", f"/flows/{fid}/start")
            if not check_ok(r, "START"): return False
            ls = get_data(os_api("GET", f"/flows/{fid}")).get("lifecycleStatus")
            if ls not in (2, "2"): os_fail(f"lifecycleStatus={ls}"); return False
            return True
        def s12():
            """Happy path: invoke and verify script HTTP call results"""
            r = api_connector("POST", f"/flows/{fid}/invoke",
                             {"name": "zhang san", "email": "z@company.com", "age": 30},
                             headers={"X-Sys-Token": "tester"})
            if r is None: os_fail("connector-api unreachable"); return False
            if r.status_code not in (200, 201): os_fail(f"HTTP {r.status_code}, body={r.text[:200]}"); return False
            print(f"  [OK] Invoke HTTP {r.status_code}")
            body = r.json(); print(f"    body: {json.dumps(body, ensure_ascii=False)[:300]}")
            ok = True
            if body.get("result") == "ZHANG SAN": print("  [OK] result=ZHANG SAN")
            else: os_fail(f"result={body.get('result')}"); ok = False
            if body.get("domain") == "company.com": print("  [OK] domain=company.com")
            else: os_fail(f"domain={body.get('domain')}"); ok = False
            if body.get("group") == "adult": print("  [OK] group=adult")
            else: os_fail(f"group={body.get('group')}"); ok = False
            return ok
        def s13():
            """Minor user: verify age_group=minor"""
            r = api_connector("POST", f"/flows/{fid}/invoke",
                             {"name": "xiao ming", "email": "xm@school.edu", "age": 15},
                             headers={"X-Sys-Token": "tester"})
            if r is None or r.status_code not in (200, 201): os_fail("Minor invoke failed"); return False
            body = r.json()
            if body.get("group") == "minor": print("  [OK] group=minor (age=15)")
            else: os_fail(f"group={body.get('group')}"); return False
            if body.get("result") == "XIAO MING": print("  [OK] result=XIAO MING")
            else: os_fail(f"result={body.get('result')}"); return False
            return True
        def s14():
            time.sleep(0.5)
            return check_ok(os_api("POST", f"/flows/{fid}/stop"), "STOP")

        if not failed and not step("DEPLOY", s10): failed = True
        if not failed and not step("START", s11): failed = True
        if not failed and not step("Invoke (happy path)", s12): failed = True
        if not failed and not step("Invoke (minor user)", s13): failed = True
        if not failed and not step("STOP", s14): failed = True
        print(f"  {'[OK]' if not failed else '[FAIL]'} Phase 4")

        # result
        print("\n" + "=" * 60)
        if failed: print("  [FAIL] Script full-flow test FAILED")
        else: print("  [PASS] Script full-flow test PASSED!")
        print("=" * 60)
        assert not failed

    finally:
        if not KEEP:
            if fid:
                os_db(f"DELETE FROM openplatform_v2_cp_flow_version_t WHERE flow_id={fid}")
                os_db(f"DELETE FROM openplatform_v2_cp_flow_t WHERE id={fid}")
            if aid: os_db(f"DELETE FROM openplatform_v2_approval_record_t WHERE id={aid}")
            print("  [OK] Test data cleaned up")
        mock.stop(); os_done()

if __name__ == "__main__":
    test_full_flow_script()
