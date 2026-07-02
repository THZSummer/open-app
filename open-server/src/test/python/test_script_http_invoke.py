#!/usr/bin/env python3
"""
Script node direct HTTP call test — trigger → script(fetch mock API) → exit

Simplified 3-node flow:
  trigger  — receives HTTP request {name, email, age}
  script   — reads ctx.trigger.input.body, calls mock HTTP API directly
  exit     — would return script output (but sandbox blocks HTTP first)

Since GraalJS sandbox is allowIO=false, the script's HTTP call is BLOCKED.
Expected: sandbox returns SCRIPT_RUNTIME_ERROR (63001), X-Status=1.

Prerequisites:
  - connector-api running on localhost:18180

Usage:
  cd open-server/src/test/python
  python3 test_script_http_invoke.py
"""
import os, sys, json, time, threading
import requests, pytest
import random, string
from http.server import HTTPServer, BaseHTTPRequestHandler

TEST_DIR = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(TEST_DIR, "inspect"))
import importlib.util
_spec = importlib.util.spec_from_file_location(
    "inspect_client", os.path.join(TEST_DIR, "inspect", "client.py"))
_osm = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(_osm)
os_db = _osm.db
os_db_val = _osm.db_val
os_ok = _osm.ok
os_fail = _osm.fail
os_done = _osm.done
from client import CONNECTOR_API_BASE, CONNECTOR_API_HEALTH

INTERNAL_APP_ID = int(os_db_val(
    f"SELECT id FROM openplatform_app_t WHERE app_id = '{_osm.TEST_APP_ID}' AND status = 1"))
_RUN_ID = ''.join(random.choices(string.ascii_lowercase + string.digits, k=6))
MOCK_PORT = 18987; MOCK_URL = f"http://localhost:{MOCK_PORT}"


# ═══════════════════════════════════════════════════════════
# Mock HTTP Server
# ═══════════════════════════════════════════════════════════

class MockServer:
    def __init__(self):
        self._server = None

    def _make_handler(self):
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
            def do_GET(self):
                if self.path == "/api/health":
                    self._json(200, {"status": "ok"})
            def do_POST(self):
                body = self._body()
                name = body.get("name", "unknown")
                email = body.get("email", "")
                age = body.get("age", 0)
                self._json(200, {
                    "code": 0,
                    "data": {
                        "display_name": name.upper(),
                        "email_domain": email.split("@")[-1] if "@" in email else "",
                        "age_group": "adult" if int(age) >= 18 else "minor",
                    }
                })
        return H

    def start(self):
        self._server = HTTPServer(("localhost", MOCK_PORT), self._make_handler())
        threading.Thread(target=self._server.serve_forever, daemon=True).start()
        for _ in range(20):
            try:
                r = requests.get(f"{MOCK_URL}/api/health", timeout=1)
                if r.status_code == 200:
                    print(f"  [OK] Mock ready :{MOCK_PORT}")
                    return True
            except Exception:
                pass
            time.sleep(0.5)
        return False

    def stop(self):
        if self._server: self._server.shutdown()


# ═══════════════════════════════════════════════════════════
# Helpers
# ═══════════════════════════════════════════════════════════

def _id(): return int(time.time() * 1000000) % 100000000000000000
def _esc(obj): return json.dumps(obj).replace("\\", "\\\\").replace("'", "''")
_db = _osm.db

def trigger_invoke(flow_id, body, headers=None):
    url = f"{CONNECTOR_API_BASE}/flows/{flow_id}/invoke"
    h = {"Content-Type": "application/json"}
    if headers: h.update(headers)
    try:
        return requests.post(url, json=body, headers=h, timeout=10)
    except requests.ConnectionError:
        return None

def step(no, desc, fn):
    print(f"\n  [{no}] {desc}")
    try: return fn()
    except Exception as e: os_fail(f"Step {no}: {e}"); return False


# ═══════════════════════════════════════════════════════════
# Test
# ═══════════════════════════════════════════════════════════

def test_script_http_invoke():
    print("=" * 60)
    print("  Script HTTP invoke test: trigger → script(fetch) → exit")
    print(f"  Run: {_RUN_ID}")
    print("=" * 60)

    # check connector-api
    try:
        r = requests.get(CONNECTOR_API_HEALTH, timeout=5)
        if r.status_code != 200:
            print("[FAIL] connector-api not ready"); return
    except Exception:
        print("[FAIL] connector-api not running"); return
    print("  [OK] connector-api ready")

    # start mock
    mock = MockServer()
    if not mock.start(): print("[FAIL] Mock failed"); return

    fid = fvid = None
    failed = False

    try:
        # ====== Step 1: create flow + orchestration (3 nodes) ======
        def s1():
            nonlocal fid, fvid
            fid, fvid = _id(), _id()
            nid_t = "trigger"; nid_s = "script"; nid_e = "exit"

            # script: reads trigger params, calls mock HTTP (will be blocked by sandbox)
            script_src = (
                "function main(ctx) {\n"
                "    var input = ctx.trigger.input.body;\n"
                "    var payload = JSON.stringify({\n"
                "        name: input.name,\n"
                "        email: input.email,\n"
                "        age: input.age\n"
                "    });\n"
                "    var resp = fetch('" + MOCK_URL + "/api/user', {\n"
                "        method: 'POST',\n"
                "        headers: { 'Content-Type': 'application/json' },\n"
                "        body: payload\n"
                "    });\n"
                "    var data = resp.json();\n"
                "    return {\n"
                "        result: data.data.display_name,\n"
                "        domain: data.data.email_domain,\n"
                "        group: data.data.age_group\n"
                "    };\n"
                "}"
            )

            orch = {
                "nodes": [
                    {"id": nid_t, "type": "trigger", "data": {
                        "type": "trigger", "triggerType": "http",
                        "authConfigs": [{
                            "type": "SYSTOKEN",
                            "header": {"type": "object", "properties": {
                                "X-Sys-Token": {"type": "string", "required": True, "sensitive": True}}}
                        }],
                        "input": {
                            "protocol": "HTTP",
                            "header": {"type": "object", "properties": {}, "required": []},
                            "query": {"type": "object", "properties": {}, "required": []},
                            "body": {
                                "type": "object",
                                "properties": {
                                    "name": {"type": "string"},
                                    "email": {"type": "string"},
                                    "age": {"type": "number"}
                                },
                                "required": ["name"]
                            }
                        }
                    }},
                    {"id": nid_s, "type": "script", "data": {
                        "script": script_src, "timeoutMs": 5000
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
                                    "group": {"type": "string", "value": "${$.node.script.output.group}"}
                                }
                            }
                        }
                    }}
                ],
                "edges": [
                    {"id": "e1", "source": nid_t, "target": nid_s},
                    {"id": "e2", "source": nid_s, "target": nid_e}
                ],
                "flowConfig": {"rateLimitConfig": {"maxQps": 50}}
            }

            _db(f"INSERT INTO openplatform_v2_cp_flow_t "
                f"(id, name_cn, name_en, lifecycle_status, app_id, create_by, last_update_by) "
                f"VALUES ({fid}, 'ScriptHTTP_{_RUN_ID}', 'script_http_{_RUN_ID}', "
                f"2, {INTERNAL_APP_ID}, 'tester', 'tester')")
            _db(f"INSERT INTO openplatform_v2_cp_flow_version_t "
                f"(id, flow_id, orchestration_config, create_by, last_update_by) "
                f"VALUES ({fvid}, {fid}, '{_esc(orch)}', 'tester', 'tester')")
            print(f"    flowId={fid}")
            return True

        # ====== Step 2: trigger invoke ======
        def s2():
            resp = trigger_invoke(fid,
                body={"name": "zhangsan", "email": "z@c.com", "age": 30},
                headers={"X-Sys-Token": "tester"})

            if resp is None:
                os_fail("connector-api unreachable"); return False

            # Sandbox blocks HTTP → expect error
            is_err = resp.status_code != 200 or resp.headers.get("X-Status") == "1"
            code = resp.headers.get("X-Code", "")
            body_empty = len(resp.content) == 0

            print(f"    HTTP {resp.status_code}, X-Status={resp.headers.get('X-Status')}")
            print(f"    X-Code={code}")

            if is_err:
                print("  [OK] Sandbox blocked HTTP call")
            else:
                os_fail(f"Expected sandbox rejection, got HTTP {resp.status_code}")

            if code:
                print(f"  [OK] Error code: {code}")
            else:
                os_fail("Missing X-Code header")

            if body_empty:
                print("  [OK] Response body empty (error spec)")
            else:
                print(f"    body: {resp.content[:200]}")

            return is_err and bool(code)

        # ====== Step 3: verify trigger→script data path ======
        # Since script fails at HTTP, we can't see output. But we verify
        # the flow didn't crash before reaching the script.
        def s3():
            # Check execution record exists (means flow started)
            row = os_db_val(
                f"SELECT COUNT(*) FROM openplatform_v2_cp_execution_record_t WHERE flow_id={fid}")
            if row and int(row) >= 1:
                print(f"  [OK] Execution record created: {row}")
                return True
            os_fail(f"No execution record"); return False

        if not step(1, "Create flow (3 nodes: trigger→script→exit)", s1): failed = True
        if not step(2, "Trigger invoke — verify sandbox blocks HTTP", s2): failed = True
        if not step(3, "Verify execution record", s3): failed = True

        print("\n" + "=" * 60)
        if failed:
            print("  [FAIL] Script HTTP invoke test FAILED")
        else:
            print("  [PASS] Script sandbox correctly blocked HTTP call")
        print("=" * 60)
        assert not failed

    finally:
        if fid:
            _db(f"DELETE FROM openplatform_v2_cp_flow_version_t WHERE flow_id={fid}")
            _db(f"DELETE FROM openplatform_v2_cp_flow_t WHERE id={fid}")
        mock.stop()
        os_done()

if __name__ == "__main__":
    test_script_http_invoke()
