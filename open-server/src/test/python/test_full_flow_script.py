#!/usr/bin/env python3
"""
Script node full-flow E2E test — trigger → script_prep → connector(HTTP) → script_process → exit

Simulates a real business scenario:
  Phase 0: Environment check (open-server + connector-api)
  Phase 1: Connector publish (create → draft → config → publish)
  Phase 2: Flow orchestration with script nodes
  Phase 3: Draft debug
  Phase 4: Publish + approve
  Phase 5: Post-publish debug
  Phase 6: Deploy + invoke + verify

Prerequisites:
  - open-server running on localhost:18080
  - connector-api running on localhost:18180

Usage:
  cd open-server/src/test/python
  python3 test_full_flow_script.py
  KEEP_TEST_DATA=0 python3 test_full_flow_script.py  # auto cleanup
"""
import os
import sys
import json
import time
import threading

import importlib.util
from http.server import HTTPServer, BaseHTTPRequestHandler

TEST_DIR = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(TEST_DIR, "inspect"))

# ── load open-server client ──
_spec = importlib.util.spec_from_file_location(
    "inspect_client", os.path.join(TEST_DIR, "inspect", "client.py")
)
_osm = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(_osm)
os_api = _osm.api
os_db = _osm.db
os_db_val = _osm.db_val
os_ok = _osm.ok
os_fail = _osm.fail
os_done = _osm.done
from client import _REDIS_CLUSTER_NODES, CONNECTOR_API_BASE, CONNECTOR_API_HEALTH, MOCK_SERVER_URL, OPEN_SERVER_BASE

import pytest
import requests
import random
import string

TEST_APP_ID = _osm.TEST_APP_ID
INTERNAL_APP_ID = int(os_db_val(f"SELECT id FROM openplatform_app_t WHERE app_id = '{TEST_APP_ID}' AND status = 1"))
KEEP = os.environ.get("KEEP_TEST_DATA", "1") == "1"
_RUN_ID = ''.join(random.choices(string.ascii_lowercase + string.digits, k=6))


# ═══════════════════════════════════════════════════════════
# Mock HTTP Server
# ═══════════════════════════════════════════════════════════

class MockServer:
    """Realistic downstream HTTP service for script node full-flow testing."""

    def __init__(self, port=18988):
        self.port = port
        self._server = None
        self._thread = None

    def _make_handler(self):
        state = {"call_count": 0}

        class H(BaseHTTPRequestHandler):
            def log_message(self, f, *a):
                pass

            def _respond(self, code, body, extra_headers=None):
                self.send_response(code)
                self.send_header("Content-Type", "application/json")
                self.send_header("X-Mock-Call-Count", str(state["call_count"]))
                if extra_headers:
                    for k, v in extra_headers.items():
                        self.send_header(k, v)
                self.end_headers()
                self.wfile.write(json.dumps(body, ensure_ascii=False).encode())

            def _parse_body(self):
                cl = int(self.headers.get("Content-Length", 0))
                if cl > 0:
                    try:
                        return json.loads(self.rfile.read(cl))
                    except Exception:
                        return {}
                return {}

            def _path_only(self):
                return self.path.split("?")[0]

            def do_GET(self):
                path = self._path_only()
                state["call_count"] += 1
                time.sleep(random.uniform(0.01, 0.02))
                if path == "/api/health":
                    self._respond(200, {"status": "ok", "server": "script-flow-mock"})
                else:
                    self._respond(200, {"code": 0, "data": {"method": "GET", "path": path}})

            def do_POST(self):
                path = self._path_only()
                body = self._parse_body()
                state["call_count"] += 1
                time.sleep(random.uniform(0.01, 0.03))

                if path == "/api/user":
                    name = body.get("name", "unknown")
                    email = body.get("email", "")
                    age = body.get("age", 0)
                    city = body.get("city", "")
                    self._respond(200, {
                        "code": 0,
                        "message": "user processed",
                        "data": {
                            "user_id": f"usr_{state['call_count']}",
                            "profile": {
                                "display_name": name.upper(),
                                "email_domain": email.split("@")[-1] if "@" in email else "unknown",
                                "age_group": "adult" if int(age) >= 18 else "minor",
                                "location": city or "unspecified"
                            },
                            "metadata": {
                                "processed_at": time.strftime("%Y-%m-%dT%H:%M:%S"),
                                "call_number": state["call_count"]
                            }
                        }
                    }, extra_headers={"X-User-Processed": "true"})

                elif path == "/api/echo":
                    self._respond(200, {
                        "code": 0,
                        "data": {"echo_body": body, "server_time": time.strftime("%Y-%m-%dT%H:%M:%S")}
                    })
                else:
                    self._respond(200, {
                        "code": 0, "data": {"path": path, "received": body, "method": "POST"}
                    })

        return H

    def start(self, timeout=10):
        self._server = HTTPServer(("localhost", self.port), self._make_handler())
        self._thread = threading.Thread(target=self._server.serve_forever, daemon=True)
        self._thread.start()
        for _ in range(timeout * 2):
            try:
                r = requests.get(f"http://localhost:{self.port}/api/health", timeout=1)
                if r.status_code == 200:
                    print(f"  [OK] Mock Server ready (port {self.port})")
                    return True
            except Exception:
                pass
            time.sleep(0.5)
        return False

    def stop(self):
        if self._server:
            self._server.shutdown()
        print(f"  Mock Server stopped")


# ═══════════════════════════════════════════════════════════
# Helpers
# ═══════════════════════════════════════════════════════════

def snow_id():
    return int(time.time() * 1000000) % 100000000000000000

def api_connector(method, path, body=None, headers=None):
    url = f"{CONNECTOR_API_BASE}{path}"
    h = {"Content-Type": "application/json"}
    if headers:
        h.update(headers)
    try:
        if method == "POST":
            return requests.post(url, json=body, headers=h, timeout=10)
        else:
            return requests.get(url, headers=h, timeout=10)
    except requests.ConnectionError:
        return None

step_num = [0]
def step(desc, fn):
    step_num[0] += 1
    no = step_num[0]
    print(f"\n  [{no}] {desc}")
    try:
        return fn()
    except Exception as e:
        os_fail(f"Step {no} exception: {e}")
        return False

def check_ok(resp, desc, url=""):
    if resp is None:
        os_fail(f"{desc}: connection failed {url}")
        return False
    if resp.status_code == 200:
        try:
            body = resp.json()
            if body.get("code") in ("200", 200):
                print(f"  [OK] {desc}  {url}")
                return True
            else:
                os_fail(f"{desc}: code={body.get('code')}, msg={body.get('messageZh','')}")
                return False
        except Exception:
            print(f"  [OK] {desc} (HTTP 200)  {url}")
            return True
    else:
        try:
            body = resp.json()
            detail = f"HTTP {resp.status_code}, code={body.get('code')}, msg={body.get('messageZh','')}"
        except Exception:
            detail = f"HTTP {resp.status_code}, body={resp.text[:200]}"
        os_fail(f"{desc}: {detail}  {url}")
        return False

def get_data(resp):
    try:
        return resp.json().get("data", {})
    except Exception:
        return {}


# ═══════════════════════════════════════════════════════════
# Test
# ═══════════════════════════════════════════════════════════

@pytest.mark.L3
def test_full_flow_script():
    print("=" * 60)
    print("  Script Node Full-Flow E2E Test")
    print(f"  Run ID: {_RUN_ID}")
    print("  Flow: trigger → script_prep → connector(HTTP) → script_process → exit")
    print("=" * 60)

    # ── Phase 0 check ──
    print("\n-- Phase 0: Environment Check --")
    r = os_api("GET", "/connectors")
    if r is None:
        print("[FAIL] open-server not running!")
        return
    print("  [OK] open-server ready")

    try:
        r2 = requests.get(CONNECTOR_API_HEALTH, timeout=5)
        if r2.status_code == 200:
            print("  [OK] connector-api ready")
        else:
            print(f"  [WARN] connector-api status: HTTP {r2.status_code}")
    except Exception:
        print("[FAIL] connector-api not running!")
        return

    # V3: check approval template
    tpl = os_db_val(f"SELECT id FROM openplatform_v2_approval_flow_t WHERE code = 'connector_flow_version_publish' AND app_id = {INTERNAL_APP_ID} LIMIT 1")
    if not tpl:
        tpl = os_db_val("SELECT id FROM openplatform_v2_approval_flow_t WHERE code = 'connector_flow_version_publish' AND app_id IS NULL LIMIT 1")
    if tpl:
        print(f"  [OK] Approval template exists (id={tpl})")
    else:
        print("  [WARN] No approval template, creating...")
        r = os_api("POST", "/approval-flows", {
            "code": "connector_flow_version_publish",
            "nameCn": "Connection flow version publish approval",
            "nameEn": "connector_flow_version_publish",
            "appId": INTERNAL_APP_ID,
            "nodes": [{"userId": "tester", "userName": "Test Approver"}]
        })
        if r and r.status_code in (200, 201):
            print("  [OK] Approval template created")

    # ── Start Mock ──
    print("\n-- Mock Server --")
    mock = MockServer(port=18988)
    if not mock.start():
        print("[FAIL] Mock Server failed to start")
        return

    MOCK_URL = f"http://localhost:{mock.port}"
    cid = conn_vid = fid = fvid = aid = None
    failed = False

    try:
        # ═══════════════════════════════════════════════════
        # Phase 1: Connector publish
        # ═══════════════════════════════════════════════════
        print("\n-- Phase 1: Connector Publish --")

        def s1():
            nonlocal cid
            cid = snow_id()
            r = os_api("POST", "/connectors", {
                "nameCn": f"ScriptFlow_Conn_{_RUN_ID}",
                "nameEn": f"scriptflow-conn-{_RUN_ID}",
                "connectorType": 1
            })
            if not check_ok(r, "CREATE connector", "POST /connectors"): return False
            d = get_data(r)
            if str(d.get("connectorId")) != str(cid):
                cid = int(d.get("connectorId"))
            print(f"    connectorId={cid}")
            return True

        def s2():
            nonlocal conn_vid
            r = os_api("POST", f"/connectors/{cid}/versions", {})
            if not check_ok(r, "CREATE draft version (connector)", "POST /connectors/{cid}/versions"): return False
            r2 = os_api("GET", f"/connectors/{cid}/versions")
            if not check_ok(r2, "GET version list"): return False
            vlist = get_data(r2)
            if isinstance(vlist, dict):
                vlist = vlist.get("items", vlist.get("data", []))
            if isinstance(vlist, list) and len(vlist) > 0:
                conn_vid = int(vlist[0].get("versionId", vlist[0].get("id", 0)))
                print(f"    connectorVersionId={conn_vid}")
                return True
            os_fail("Version list empty")
            return False

        def s3():
            r = os_api("PUT", f"/connectors/{cid}/versions/{conn_vid}", {
                "connectionConfig": {
                    "protocol": "HTTP",
                    "protocolConfig": {
                        "url": f"{MOCK_URL}/api/user",
                        "method": "POST",
                        "headers": {"Content-Type": "application/json"}
                    },
                    "authConfigs": [{"type": "NONE"}],
                    "input": {
                        "protocol": "HTTP",
                        "header": {"type": "object", "properties": {}, "required": []},
                        "query": {"type": "object", "properties": {}, "required": []},
                        "body": {
                            "type": "object",
                            "properties": {
                                "name": {"type": "string"},
                                "email": {"type": "string"},
                                "age": {"type": "number"},
                                "city": {"type": "string"}
                            },
                            "required": []
                        }
                    },
                    "output": {
                        "protocol": "HTTP",
                        "body": {
                            "type": "object",
                            "properties": {
                                "code": {"type": "number"},
                                "message": {"type": "string"},
                                "data": {
                                    "type": "object",
                                    "properties": {
                                        "user_id": {"type": "string"},
                                        "profile": {
                                            "type": "object",
                                            "properties": {
                                                "display_name": {"type": "string"},
                                                "email_domain": {"type": "string"},
                                                "age_group": {"type": "string"},
                                                "location": {"type": "string"}
                                            }
                                        },
                                        "metadata": {
                                            "type": "object",
                                            "properties": {
                                                "processed_at": {"type": "string"},
                                                "call_number": {"type": "number"}
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    },
                    "timeoutMs": 5000
                }
            })
            return check_ok(r, "UPDATE connector config", f"PUT /connectors/{cid}/versions/{conn_vid}")

        def s4():
            r = os_api("PUT", f"/connectors/{cid}/versions/{conn_vid}/publish")
            if not check_ok(r, "PUBLISH connector version"): return False
            r2 = os_api("GET", f"/connectors/{cid}/versions/{conn_vid}")
            if not check_ok(r2, "Confirm published"): return False
            d = get_data(r2)
            if d.get("status") not in (2, "2"):
                os_fail(f"status={d.get('status')}, expected=2 (published)")
                return False
            return True

        if not step("CREATE connector", s1): failed = True
        if not failed and not step("CREATE draft version", s2): failed = True
        if not failed and not step("UPDATE connector config", s3): failed = True
        if not failed and not step("PUBLISH connector", s4): failed = True

        if not failed:
            print("  [OK] Phase 1 done: connector published")
        else:
            print("  [FAIL] Phase 1 failed")

        # ═══════════════════════════════════════════════════
        # Phase 2: Flow orchestration with script nodes
        # ═══════════════════════════════════════════════════
        if not failed:
            print("\n-- Phase 2: Flow Orchestration (with script nodes) --")

        def s5():
            nonlocal fid
            fid = snow_id()
            r = os_api("POST", "/flows", {
                "nameCn": f"ScriptFlow_{_RUN_ID}",
                "nameEn": f"scriptflow-{_RUN_ID}"
            })
            if not check_ok(r, "CREATE flow", "POST /flows"): return False
            d = get_data(r)
            if str(d.get("flowId")) != str(fid):
                fid = int(d.get("flowId"))
            print(f"    flowId={fid}")
            return True

        def s6():
            nonlocal fvid
            r = os_api("POST", f"/flows/{fid}/versions", {})
            if not check_ok(r, "CREATE draft version (flow)"): return False
            d = get_data(r)
            fvid = int(d.get("versionId", d.get("id", 0)))
            print(f"    flowVersionId={fvid}")
            return True

        def s7():
            nid_trigger = f"trigger_{_RUN_ID}"
            nid_script_prep = f"script_prep_{_RUN_ID}"
            nid_connector = f"conn_{_RUN_ID}"
            nid_script_process = f"script_process_{_RUN_ID}"
            nid_exit = f"exit_{_RUN_ID}"

            # script_prep: validate + construct connector payload
            script_prep = (
                "function main(ctx) {\n"
                "    // call mock health endpoint via script HTTP client\n"
                "    var health = ctx.http.get('" + MOCK_URL + "/api/health');\n"
                "    var input = ctx.trigger.input.body;\n"
                "    var name = input.name || 'anonymous';\n"
                "    var email = input.email || '';\n"
                "    var age = input.age || 0;\n"
                "    var city = input.city || '';\n"
                "    return {\n"
                "        name: name.trim(),\n"
                "        email: email.trim(),\n"
                "        age: Math.max(0, parseInt(age) || 0),\n"
                "        city: city.trim(),\n"
                "        prep_note: 'validated_by_script',\n"
                "        health_status: health.body.status\n"
                "    };\n"
                "}"
            )

            # script_process: transform connector response
            script_process = (
                "function main(ctx) {\n"
                "    var conn = ctx." + nid_connector + ".output;\n"
                "    var data = conn.data;\n"
                "    var profile = data.profile;\n"
                "    var meta = data.metadata;\n"
                "    return {\n"
                "        external_user_id: data.user_id,\n"
                "        display_name: profile.display_name,\n"
                "        email_domain: profile.email_domain,\n"
                "        age_group: profile.age_group,\n"
                "        location: profile.location,\n"
                "        is_adult: profile.age_group === 'adult',\n"
                "        call_number: meta.call_number,\n"
                "        summary: 'User ' + profile.display_name + ' (' + profile.age_group + ') from ' + profile.location\n"
                "    };\n"
                "}"
            )

            r = os_api("PUT", f"/flows/{fid}/versions/{fvid}", {
                "orchestrationConfig": {
                    "nodes": [
                        # trigger
                        {"id": nid_trigger, "type": "trigger", "data": {
                            "type": "trigger",
                            "triggerType": "http",
                            "authConfigs": [{
                                "type": "SYSTOKEN",
                                "header": {
                                    "type": "object",
                                    "properties": {"X-Sys-Token": {"type": "string", "required": True, "sensitive": True}}
                                },
                                "sysAccountWhitelist": ["tester"]
                            }],
                            "input": {
                                "protocol": "HTTP",
                                "header": {
                                    "type": "object",
                                    "properties": {"X-Request-Id": {"type": "string"}},
                                    "required": []
                                },
                                "query": {
                                    "type": "object",
                                    "properties": {"source": {"type": "string"}},
                                    "required": []
                                },
                                "body": {
                                    "type": "object",
                                    "properties": {
                                        "name": {"type": "string"},
                                        "email": {"type": "string"},
                                        "age": {"type": "number"},
                                        "city": {"type": "string"}
                                    },
                                    "required": ["name", "email"]
                                }
                            }
                        }},
                        # script_prep
                        {"id": nid_script_prep, "type": "script", "data": {
                            "script": script_prep,
                            "timeoutMs": 3000,
                            "output": {
                                "type": "object",
                                "properties": {
                                    "name": {"type": "string", "description": "清洗后的用户名"},
                                    "email": {"type": "string", "description": "清洗后的邮箱"},
                                    "age": {"type": "number", "description": "清洗后的年龄(>=0)"},
                                    "city": {"type": "string", "description": "清洗后的城市"},
                                    "prep_note": {"type": "string", "description": "预处理标记"},
                                    "health_status": {"type": "string", "description": "Mock健康检查状态(ctx.http.get)"}
                                }
                            }
                        }},
                        # connector (HTTP call)
                        {"id": nid_connector, "type": "connector", "data": {
                            "connectorId": str(cid),
                            "connectorVersionId": str(conn_vid),
                            "connectorVersionConfig": {
                                "protocol": "HTTP",
                                "protocolConfig": {
                                    "url": f"{MOCK_URL}/api/user",
                                    "method": "POST"
                                },
                                "authConfigs": [{"type": "NONE"}],
                                "input": {
                                    "protocol": "HTTP",
                                    "header": {"type": "object", "properties": {}, "required": []},
                                    "query": {"type": "object", "properties": {}, "required": []},
                                    "body": {
                                        "type": "object",
                                        "properties": {
                                            "name": {"type": "string"},
                                            "email": {"type": "string"},
                                            "age": {"type": "number"},
                                            "city": {"type": "string"}
                                        },
                                        "required": []
                                    }
                                },
                                "output": {
                                    "protocol": "HTTP",
                                    "body": {
                                        "type": "object",
                                        "properties": {
                                            "code": {"type": "number"},
                                            "message": {"type": "string"},
                                            "data": {
                                                "type": "object",
                                                "properties": {
                                                    "user_id": {"type": "string"},
                                                    "profile": {
                                                        "type": "object",
                                                        "properties": {
                                                            "display_name": {"type": "string"},
                                                            "email_domain": {"type": "string"},
                                                            "age_group": {"type": "string"},
                                                            "location": {"type": "string"}
                                                        }
                                                    },
                                                    "metadata": {
                                                        "type": "object",
                                                        "properties": {
                                                            "processed_at": {"type": "string"},
                                                            "call_number": {"type": "number"}
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            },
                            "timeoutMs": 5000,
                            "input": {
                                "header": {"type": "object", "properties": {}},
                                "query": {"type": "object", "properties": {}},
                                "body": {
                                    "type": "object",
                                    "properties": {
                                        "name": {"type": "string", "value": "${$.node." + nid_script_prep + ".output.name}"},
                                        "email": {"type": "string", "value": "${$.node." + nid_script_prep + ".output.email}"},
                                        "age": {"type": "number", "value": "${$.node." + nid_script_prep + ".output.age}"},
                                        "city": {"type": "string", "value": "${$.node." + nid_script_prep + ".output.city}"}
                                    }
                                }
                            }
                        }},
                        # script_process
                        {"id": nid_script_process, "type": "script", "data": {
                            "script": script_process,
                            "timeoutMs": 3000,
                            "output": {
                                "type": "object",
                                "properties": {
                                    "external_user_id": {"type": "string", "description": "外部用户ID"},
                                    "display_name": {"type": "string", "description": "显示名称(大写)"},
                                    "email_domain": {"type": "string", "description": "邮箱域名"},
                                    "age_group": {"type": "string", "description": "年龄段 adult/minor"},
                                    "location": {"type": "string", "description": "城市"},
                                    "is_adult": {"type": "boolean", "description": "是否成年"},
                                    "call_number": {"type": "number", "description": "调用序号"},
                                    "summary": {"type": "string", "description": "用户摘要"}
                                }
                            }
                        }},
                        # exit
                        {"id": nid_exit, "type": "exit", "data": {
                            "type": "exit",
                            "output": {
                                "header": {
                                    "type": "object",
                                    "properties": {
                                        "X-User-Processed": {
                                            "type": "string",
                                            "value": "${$.node." + nid_connector + ".output.data.user_id}"
                                        }
                                    }
                                },
                                "body": {
                                    "type": "object",
                                    "properties": {
                                        "externalUserId": {"type": "string", "value": "${$.node." + nid_script_process + ".output.external_user_id}"},
                                        "displayName": {"type": "string", "value": "${$.node." + nid_script_process + ".output.display_name}"},
                                        "emailDomain": {"type": "string", "value": "${$.node." + nid_script_process + ".output.email_domain}"},
                                        "ageGroup": {"type": "string", "value": "${$.node." + nid_script_process + ".output.age_group}"},
                                        "location": {"type": "string", "value": "${$.node." + nid_script_process + ".output.location}"},
                                        "isAdult": {"type": "boolean", "value": "${$.node." + nid_script_process + ".output.is_adult}"},
                                        "summary": {"type": "string", "value": "${$.node." + nid_script_process + ".output.summary}"},
                                        "callNumber": {"type": "number", "value": "${$.node." + nid_script_process + ".output.call_number}"}
                                    }
                                }
                            }
                        }}
                    ],
                    "edges": [
                        {"id": f"e1_{_RUN_ID}", "source": nid_trigger, "target": nid_script_prep},
                        {"id": f"e2_{_RUN_ID}", "source": nid_script_prep, "target": nid_connector},
                        {"id": f"e3_{_RUN_ID}", "source": nid_connector, "target": nid_script_process},
                        {"id": f"e4_{_RUN_ID}", "source": nid_script_process, "target": nid_exit}
                    ],
                    "flowConfig": {
                        "flowMode": "single",
                        "rateLimitConfig": {"maxQps": 50, "maxConcurrency": 10}
                    }
                }
            })
            return check_ok(r, "UPDATE orchestration (trigger→script→conn→script→exit)",
                          f"PUT /flows/{fid}/versions/{fvid}")

        if not failed:
            if not step("CREATE flow", s5): failed = True
        if not failed:
            if not step("CREATE draft version", s6): failed = True
        if not failed:
            if not step("UPDATE orchestration (5 nodes, 2 script)", s7): failed = True

        if not failed:
            print("  [OK] Phase 2 done: flow orchestrated with script nodes")
        else:
            print("  [FAIL] Phase 2 failed")

        # ═══════════════════════════════════════════════════
        # Phase 3: Draft debug
        # ═══════════════════════════════════════════════════
        if not failed:
            print("\n-- Phase 3: Draft Debug --")

        def s8():
            r = os_api("POST", f"/flows/{fid}/versions/{fvid}/debug", {
                "triggerData": {"name": "debug_user", "email": "debug@test.com", "age": 28, "city": "Shenzhen"}
            })
            ok = check_ok(r, "DEBUG draft version", f"POST /flows/{fid}/versions/{fvid}/debug")
            if ok and r is not None:
                try:
                    b = r.json()
                    print(f"    response: {json.dumps(b, ensure_ascii=False)[:400]}")
                except Exception:
                    pass
            return ok

        if not failed:
            if not step("DEBUG draft version", s8): failed = True

        if not failed:
            print("  [OK] Phase 3 done: debug passed")
        else:
            print("  [FAIL] Phase 3 failed")

        # ═══════════════════════════════════════════════════
        # Phase 4: Publish + approve
        # ═══════════════════════════════════════════════════
        if not failed:
            print("\n-- Phase 4: Publish + Approve --")

        def s9():
            r = os_api("POST", f"/flows/{fid}/versions/{fvid}/publish")
            if not check_ok(r, "PUBLISH (submit approval)"): return False
            r2 = os_api("GET", f"/flows/{fid}/versions/{fvid}")
            if not check_ok(r2, "Confirm pending"): return False
            st = get_data(r2).get("status")
            if st not in (2, "2"):
                os_fail(f"version status={st}, expected=2 (pending)")
                return False
            print(f"    status=2 (pending)")
            return True

        def s10():
            nonlocal aid
            r = os_api("GET", f"/flows/{fid}/versions/{fvid}/approvals")
            # fallback: DB
            aid = os_db_val(
                f"SELECT id FROM openplatform_v2_approval_record_t "
                f"WHERE business_type='connector_flow_version_publish' AND business_id={fvid} "
                f"ORDER BY create_time DESC LIMIT 1"
            )
            if not aid:
                os_fail("Approval record not found")
                return False
            print(f"    approvalId={aid}")
            return True

        def s11():
            r = os_api("POST", f"/approvals/{aid}/approve",
                       {"comment": "script flow L1 approve"}, user="tester")
            return check_ok(r, "Level 1 approve (scene)", f"POST /approvals/{aid}/approve")

        def s12():
            r = os_api("POST", f"/approvals/{aid}/approve",
                       {"comment": "script flow L2 approve"}, user="tester")
            return check_ok(r, "Level 2 approve (global)", f"POST /approvals/{aid}/approve")

        def s13():
            time.sleep(0.5)
            r = os_api("GET", f"/flows/{fid}/versions/{fvid}")
            if not check_ok(r, "Confirm published"): return False
            st = get_data(r).get("status")
            if st not in (5, "5"):
                time.sleep(2)
                r = os_api("GET", f"/flows/{fid}/versions/{fvid}")
                st = get_data(r).get("status")
            if st in (5, "5"):
                print(f"    status=5 (published)")
                return True
            os_fail(f"status={st}, expected=5")
            return False

        if not failed:
            if not step("PUBLISH submit approval", s9): failed = True
        if not failed:
            if not step("Find approval record", s10): failed = True
        if not failed:
            if not step("Level 1 approve (tester)", s11): failed = True
        if not failed:
            if not step("Level 2 approve (tester)", s12): failed = True
        if not failed:
            if not step("Verify version published", s13): failed = True

        if not failed:
            print("  [OK] Phase 4 done: approved")
        else:
            print("  [FAIL] Phase 4 failed")

        # ═══════════════════════════════════════════════════
        # Phase 5: Deploy + Invoke + Verify
        # ═══════════════════════════════════════════════════
        if not failed:
            print("\n-- Phase 5: Deploy + Invoke + Verify --")

        def s14():
            r = os_api("POST", f"/flows/{fid}/deploy", {"versionId": fvid})
            return check_ok(r, "DEPLOY", f"POST /flows/{fid}/deploy")

        def s15():
            r = os_api("POST", f"/flows/{fid}/start")
            if not check_ok(r, "START", f"POST /flows/{fid}/start"): return False
            r2 = os_api("GET", f"/flows/{fid}")
            ls = get_data(r2).get("lifecycleStatus")
            if ls not in (2, "2"):
                os_fail(f"lifecycleStatus={ls}, expected=2 (running)")
                return False
            return True

        def s16():
            """Happy path: invoke with valid data"""
            url = f"{CONNECTOR_API_BASE}/flows/{fid}/invoke?source=web"
            r = api_connector("POST", f"/flows/{fid}/invoke?source=web",
                             {"name": "zhang san", "email": "zhangsan@company.com", "age": 30, "city": "Beijing"},
                             headers={"X-Sys-Token": "tester", "X-Request-Id": f"req-{_RUN_ID}"})
            if r is None:
                os_fail("connector-api connection failed")
                return False
            if r.status_code not in (200, 201):
                os_fail(f"Invoke: HTTP {r.status_code}, {r.text[:200]}")
                return False

            print(f"  [OK] Invoke (HTTP {r.status_code})  {url}")
            try:
                body = r.json()
                print(f"    body: {json.dumps(body, ensure_ascii=False)[:500]}")
            except Exception:
                body = {}

            # Verify script outputs
            if body.get("displayName") == "ZHANG SAN":
                print("  [OK] displayName: ZHANG SAN")
            else:
                os_fail(f"displayName={body.get('displayName')}, expected='ZHANG SAN'")

            if body.get("ageGroup") == "adult":
                print("  [OK] ageGroup: adult")
            else:
                os_fail(f"ageGroup={body.get('ageGroup')}")

            if body.get("isAdult") is True:
                print("  [OK] isAdult: True")
            else:
                os_fail(f"isAdult={body.get('isAdult')}")

            if body.get("emailDomain") == "company.com":
                print("  [OK] emailDomain: company.com")
            else:
                os_fail(f"emailDomain={body.get('emailDomain')}")

            return True

        def s17():
            """Minor user: verify age group is minor"""
            r = api_connector("POST", f"/flows/{fid}/invoke",
                             {"name": "xiao ming", "email": "xiaoming@school.edu", "age": 15, "city": "Shanghai"},
                             headers={"X-Sys-Token": "tester"})
            if r is None:
                os_fail("connector-api connection failed")
                return False
            if r.status_code not in (200, 201):
                os_fail(f"Invoke: HTTP {r.status_code}")
                return False

            try:
                body = r.json()
            except Exception:
                body = {}

            if body.get("ageGroup") == "minor":
                print("  [OK] ageGroup: minor (15 years old)")
            else:
                os_fail(f"ageGroup={body.get('ageGroup')}, expected='minor'")

            if body.get("isAdult") is False:
                print("  [OK] isAdult: False")
            else:
                os_fail(f"isAdult={body.get('isAdult')}")

            if body.get("displayName") == "XIAO MING":
                print("  [OK] displayName: XIAO MING")
            else:
                os_fail(f"displayName={body.get('displayName')}")

            return True

        def s18():
            """Verify execution records"""
            time.sleep(1)
            row = os_db_val(
                f"SELECT COUNT(*) FROM openplatform_v2_cp_execution_record_t WHERE flow_id={fid}"
            )
            if row and int(row) >= 2:
                print(f"  [OK] Execution records: {row} records")
                return True
            print(f"  [WARN] Execution records: {row} (expected >=2)")
            return True  # non-blocking

        def s19():
            r = os_api("POST", f"/flows/{fid}/stop")
            if not check_ok(r, "STOP", f"POST /flows/{fid}/stop"): return False
            r2 = os_api("GET", f"/flows/{fid}")
            ls = get_data(r2).get("lifecycleStatus")
            if ls in (1, "1"):
                print(f"    lifecycleStatus=1 (stopped)")
                return True
            os_fail(f"lifecycleStatus={ls}, expected=1")
            return False

        if not failed:
            if not step("DEPLOY", s14): failed = True
        if not failed:
            if not step("START", s15): failed = True
        if not failed:
            if not step("Invoke (happy path)", s16): failed = True
        if not failed:
            if not step("Invoke (minor user)", s17): failed = True
        if not failed:
            if not step("Verify execution records", s18): failed = True
        if not failed:
            if not step("STOP", s19): failed = True

        if not failed:
            print("  [OK] Phase 5 done: deploy + invoke + verify passed")

        # ── Result ──
        print("\n" + "=" * 60)
        if failed:
            print("  [FAIL] Script node full-flow test FAILED")
        else:
            print("  [PASS] Script node full-flow test PASSED!")
        print("=" * 60)
        assert not failed, "Script node full-flow test FAILED"

    finally:
        # Cleanup
        if not KEEP:
            print("\n-- Cleanup Test Data --")
            if fid:
                os_db(f"DELETE FROM openplatform_v2_cp_connector_version_ref_t WHERE flow_id={fid}")
                os_db(f"DELETE FROM openplatform_v2_cp_flow_version_t WHERE flow_id={fid}")
                os_db(f"DELETE FROM openplatform_v2_cp_flow_t WHERE id={fid}")
            if cid:
                os_db(f"DELETE FROM openplatform_v2_cp_connector_version_t WHERE connector_id={cid}")
                os_db(f"DELETE FROM openplatform_v2_cp_connector_t WHERE id={cid}")
            if aid:
                os_db(f"DELETE FROM openplatform_v2_approval_record_t WHERE id={aid}")
            print("  [OK] Test data cleaned up")
        else:
            print(f"\n  KEEP_TEST_DATA=1: cid={cid}, fid={fid}")

        mock.stop()
        os_done()


if __name__ == "__main__":
    test_full_flow_script()
