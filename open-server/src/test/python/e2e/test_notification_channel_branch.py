#!/usr/bin/env python3
"""
多渠道通知服务 E2E — trigger → script(ctx.http 条件分支) → exit

场景: 根据 channel 字段路由到不同通知渠道:
  channel="email"   → POST /api/notify/email   → 发送邮件通知
  channel="sms"      → POST /api/notify/sms     → 发送短信通知
  channel="webhook"  → POST /api/notify/webhook → 调用外部 webhook
  无 channel         → POST /api/notify/log     → 记录通知日志

Prerequisites:
  - open-server running on localhost:18080
  - connector-api running on localhost:18180

Usage:
  cd open-server/src/test/python
  python3 e2e/test_notification_channel_branch.py
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
os_ok = _osm.ok
os_fail = _osm.fail
os_done = _osm.done
from client import CONNECTOR_API_BASE, CONNECTOR_API_HEALTH

import pytest, requests, random, string

TEST_APP_ID = _osm.TEST_APP_ID
_RUN_ID = ''.join(random.choices(string.ascii_lowercase + string.digits, k=6))
MOCK_PORT = 18987; MOCK_URL = f"http://localhost:{MOCK_PORT}"


# --- shared data transformations ---

def _email_data(recipient, subject):
    return {
        "message_id": f"MSG-{_RUN_ID}-{hash(recipient) % 10000:04d}",
        "status": "queued",
        "recipient": recipient,
        "subject_preview": (subject or "(no subject)")[:40],
    }

def _sms_data(phone, text):
    return {
        "message_id": f"SMS-{_RUN_ID}-{hash(phone) % 10000:04d}",
        "status": "sent",
        "phone": phone,
        "cost_credits": max(1, (len(text or "") // 10) + 1),
    }

WEBHOOK_STATUSES = ["pending", "delivered", "failed", "retrying"]

def _webhook_data(url):
    idx = sum(ord(c) for c in (url or "")) % 4
    http_codes = [202, 200, 500, 503]
    return {
        "call_id": f"WH-{_RUN_ID}-{idx:04d}",
        "status": WEBHOOK_STATUSES[idx],
        "target_url": url or "https://default.example.com/hook",
        "http_code": http_codes[idx],
    }

def _log_data(source, level):
    return {
        "log_id": f"LOG-{_RUN_ID}-{random.randint(1000, 9999)}",
        "status": "stored",
        "source": source or "unknown",
        "stored_at": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime()),
    }


_RUN_DATA = {
    "email": {
        "recipient": f"user-{_RUN_ID}@example.com",
        "subject": f"Test Notification {_RUN_ID}",
    },
    "sms": {
        "phone": f"+8613{random.randint(100000000, 999999999)}",
        "text": f"【验证码】您的验证码是 {random.randint(100000, 999999)}，5分钟内有效。",
    },
    "webhook": {
        "url": f"https://hook.example.com/events/{_RUN_ID}",
    },
    "default": {
        "source": f"app-{_RUN_ID}",
        "level": "info",
    },
}


class MockServer:
    """提供 4 个通知渠道端点供脚本 ctx.http 条件调用"""

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

            def do_GET(self):
                self._json(200, {"status": "ok"})

            def do_POST(self):
                state["call_count"] += 1
                body = self._body()
                path = self.path

                if path == "/api/notify/email":
                    data = _email_data(
                        body.get("recipient", ""),
                        body.get("subject", ""),
                    )
                    self._json(200, {"code": 0, "data": data})

                elif path == "/api/notify/sms":
                    data = _sms_data(
                        body.get("phone", ""),
                        body.get("text", ""),
                    )
                    self._json(200, {"code": 0, "data": data})

                elif path == "/api/notify/webhook":
                    data = _webhook_data(body.get("url", ""))
                    self._json(200, {"code": 0, "data": data})

                elif path == "/api/notify/log":
                    data = _log_data(
                        body.get("source", ""),
                        body.get("level", ""),
                    )
                    self._json(200, {"code": 0, "data": data})

                else:
                    self._json(404, {"code": 404, "message": "unknown endpoint"})

        return H

    def start(self):
        self._server = HTTPServer(("localhost", MOCK_PORT), self._make_handler())
        threading.Thread(target=self._server.serve_forever, daemon=True).start()
        for _ in range(20):
            try:
                if requests.get(f"{MOCK_URL}/api/notify/email", timeout=1).status_code == 200:
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
def test_notification_channel_branch():
    print("=" * 60)
    print("  多渠道通知: trigger → script(ctx.http 条件分支) → exit")
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
                "nameCn": f"NotifyBranch_{_RUN_ID}",
                "nameEn": f"notify_branch_{_RUN_ID}",
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
                "    var channel = input.channel || 'log';\n"
                "\n"
                "    if (channel === 'email') {\n"
                "        var resp = ctx.http.request('POST', '" + MOCK_URL + "/api/notify/email', {body: {\n"
                "            recipient: input.recipient,\n"
                "            subject: input.subject\n"
                "        }});\n"
                "        var d = resp.body.data;\n"
                "        return { result: d.message_id, domain: d.status, group: d.recipient, path: 'email' };\n"
                "    }\n"
                "\n"
                "    if (channel === 'sms') {\n"
                "        var resp = ctx.http.request('POST', '" + MOCK_URL + "/api/notify/sms', {body: {\n"
                "            phone: input.phone,\n"
                "            text: input.text\n"
                "        }});\n"
                "        var d = resp.body.data;\n"
                "        return { result: d.message_id, domain: d.status, group: String(d.cost_credits), path: 'sms' };\n"
                "    }\n"
                "\n"
                "    if (channel === 'webhook') {\n"
                "        var resp = ctx.http.request('POST', '" + MOCK_URL + "/api/notify/webhook', {body: {\n"
                "            url: input.url\n"
                "        }});\n"
                "        var d = resp.body.data;\n"
                "        return { result: d.call_id, domain: d.status, group: String(d.http_code), path: 'webhook' };\n"
                "    }\n"
                "\n"
                "    var resp = ctx.http.request('POST', '" + MOCK_URL + "/api/notify/log', {body: {\n"
                "        source: input.source || 'default',\n"
                "        level: input.level || 'info'\n"
                "    }});\n"
                "    var d = resp.body.data;\n"
                "    return { result: d.log_id, domain: d.status, group: d.source, path: 'log' };\n"
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
                                        "channel": {"type": "string"},
                                        "recipient": {"type": "string"},
                                        "subject": {"type": "string"},
                                        "phone": {"type": "string"},
                                        "text": {"type": "string"},
                                        "url": {"type": "string"},
                                        "source": {"type": "string"},
                                        "level": {"type": "string"},
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
                "triggerData": {"channel": "email", "recipient": "debug@t.com", "subject": "Debug"},
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

        print("\n-- Phase 4: Deploy + Multi-Channel Invoke --")
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

        def _expected_email_resp(recipient):
            d = _email_data(recipient, _RUN_DATA["email"]["subject"])
            return {"result": d["message_id"], "domain": d["status"], "group": d["recipient"], "path": "email"}

        def _expected_sms_resp(phone, text):
            d = _sms_data(phone, text)
            return {"result": d["message_id"], "domain": d["status"], "group": str(d["cost_credits"]), "path": "sms"}

        def _expected_webhook_resp(url):
            d = _webhook_data(url)
            return {"result": d["call_id"], "domain": d["status"], "group": str(d["http_code"]), "path": "webhook"}

        def _expected_log_resp(source):
            return {"domain": "stored", "group": source, "path": "log"}

        e = _RUN_DATA["email"]
        def s12():
            return _invoke_and_verify("Channel=email", {
                "channel": "email", "recipient": e["recipient"], "subject": e["subject"],
            }, _expected_email_resp(e["recipient"]))

        s = _RUN_DATA["sms"]
        def s13():
            return _invoke_and_verify("Channel=sms", {
                "channel": "sms", "phone": s["phone"], "text": s["text"],
            }, _expected_sms_resp(s["phone"], s["text"]))

        w = _RUN_DATA["webhook"]
        def s14():
            return _invoke_and_verify("Channel=webhook", {
                "channel": "webhook", "url": w["url"],
            }, _expected_webhook_resp(w["url"]))

        d = _RUN_DATA["default"]
        def s15():
            return _invoke_and_verify("Default→log", {
                "source": d["source"], "level": d["level"],
            }, _expected_log_resp(d["source"]))

        def s16():
            time.sleep(0.5)
            return check_ok(os_api("POST", f"/flows/{fid}/stop"), "STOP")

        if not failed and not step("DEPLOY", s10): failed = True
        if not failed and not step("START", s11): failed = True
        if not failed and not step("Invoke channel=email", s12): failed = True
        if not failed and not step("Invoke channel=sms", s13): failed = True
        if not failed and not step("Invoke channel=webhook", s14): failed = True
        if not failed and not step("Invoke default→log", s15): failed = True
        if not failed and not step("STOP", s16): failed = True
        print(f"  {'[OK]' if not failed else '[FAIL]'} Phase 4")

        print("\n" + "=" * 60)
        if failed:
            print("  [FAIL] Notification channel branch test FAILED")
        else:
            print("  [PASS] Notification channel branch test PASSED!")
        print("=" * 60)
        assert not failed

    finally:
        mock.stop()
        os_done()


if __name__ == "__main__":
    test_notification_channel_branch()
