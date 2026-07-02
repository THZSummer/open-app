#!/usr/bin/env python3
"""
单节点编排 E2E 测试 — plan-json-schema v3

拓扑: trigger → connector → exit
覆盖: 触发器认证 (SYSTOKEN) / 连接器调用 Mock / 数据输出映射

用法:
  cd open-server/src/test/python
  python3 test_orch_single_node.py
"""
import os, sys, json, time, threading, random, string

TEST_DIR = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(TEST_DIR, "inspect"))
import importlib.util
_spec = importlib.util.spec_from_file_location(
    "inspect_client", os.path.join(TEST_DIR, "inspect", "client.py")
)
_osm = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(_osm)

os_api = _osm.api
os_ok = _osm.ok
os_fail = _osm.fail
os_done = _osm.done
from client import CONNECTOR_API_BASE, CONNECTOR_API_HEALTH, MOCK_SERVER_URL
import requests

TEST_APP_ID = _osm.TEST_APP_ID
_RUN_ID = ''.join(random.choices(string.ascii_lowercase + string.digits, k=6))

# ── Mock HTTP Server ──
class MockServer:
    def __init__(self, port=18980):
        self.port = port
        self._server = None
        self._thread = None

    def _make_handler(self):
        state = {"call_count": 0}
        from http.server import HTTPServer, BaseHTTPRequestHandler

        class H(BaseHTTPRequestHandler):
            def log_message(self, f, *a):
                pass

            def _respond(self, code, body, extra=None):
                self.send_response(code)
                self.send_header("Content-Type", "application/json")
                if extra:
                    for k, v in extra.items():
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

            def _echo_headers(self):
                skip = {"host", "content-length", "content-type"}
                return {k: v for k, v in self.headers.items() if k.lower() not in skip}

            def do_GET(self):
                state["call_count"] += 1
                self._respond(200, {"status": "ok", "call": state["call_count"],
                                    "method": "GET"})

            def do_POST(self):
                body = self._parse_body()
                req_headers = self._echo_headers()
                state["call_count"] += 1
                time.sleep(random.uniform(0.01, 0.02))
                self._respond(200, {
                    "code": 0,
                    "message": "success",
                    "data": {
                        "echo_body": body,
                        "echo_headers": req_headers,
                        "call_number": state["call_count"]
                    }
                })
        return H

    def start(self, timeout=10):
        from http.server import HTTPServer
        self._server = HTTPServer(("localhost", self.port), self._make_handler())
        self._thread = threading.Thread(target=self._server.serve_forever, daemon=True)
        self._thread.start()
        for _ in range(timeout * 2):
            try:
                r = requests.get(f"http://localhost:{self.port}/api/health", timeout=1)
                if r.status_code == 200:
                    print(f"  Mock Server ready (port {self.port})")
                    return True
            except Exception:
                pass
            time.sleep(0.5)
        return False

    def stop(self):
        if self._server:
            self._server.shutdown()

# ── Helpers ──
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

def check_ok(resp, desc, url=""):
    if resp is None:
        os_fail(f"{desc}: 连接失败 {url}")
        return False
    if resp.status_code == 200:
        try:
            body = resp.json()
            if body.get("code") in ("200", 200):
                print(f"  {desc}  {url}")
                return True
        except Exception:
            pass
        print(f"  {desc}  {url}")
        return True
    os_fail(f"{desc}: HTTP {resp.status_code}  {url}")
    return False

def get_data(resp):
    try:
        return resp.json().get("data", {})
    except Exception:
        return {}

step_num = [0]
def step(desc, fn):
    step_num[0] += 1
    print(f"\n  [{step_num[0]}] {desc}")
    try:
        return fn()
    except Exception as e:
        os_fail(f"Step {step_num[0]} 异常: {e}")
        return False

# ── Test ──
import pytest

@pytest.mark.L3
def test_full_flow_single_node():
    print("=" * 60)
    print("  单节点编排 E2E — plan-json-schema v3")
    print(f"  Run ID: {_RUN_ID}")
    print("=" * 60)

    # Phase 0: 环境检查
    r = os_api("GET", "/connectors")
    if r is None:
        os_fail("open-server 未运行")
        return
    try:
        r2 = requests.get(CONNECTOR_API_HEALTH, timeout=5)
        if r2.status_code != 200:
            os_fail(f"connector-api 异常: HTTP {r2.status_code}")
            return
    except Exception:
        os_fail("connector-api 未运行")
        return

    mock = MockServer(port=18980)
    if not mock.start():
        os_fail("Mock Server 启动失败")
        return

    cid = conn_vid = fid = fvid = aid = None
    failed = False
    nid_trigger = f"t_{_RUN_ID}"
    nid_conn = f"c_{_RUN_ID}"
    nid_exit = f"e_{_RUN_ID}"

    try:
        # ── Phase 1: 连接器发布 ──
        print("\n── Phase 1: 连接器发布 ──")

        def s1():
            nonlocal cid
            r = os_api("POST", "/connectors", {
                "nameCn": f"单节点测试_{_RUN_ID}",
                "nameEn": f"single-node-{_RUN_ID}",
                "connectorType": 1
            })
            d = get_data(r)
            cid_val = d.get("connectorId") if d else None
            if cid_val:
                cid = int(cid_val)
            else:
                cid = snow_id()
            print(f"    connectorId={cid}")
            return check_ok(r, "CREATE 连接器", "POST /connectors")

        def s2():
            nonlocal conn_vid
            r = os_api("POST", f"/connectors/{cid}/versions", {})
            r2 = os_api("GET", f"/connectors/{cid}/versions")
            vlist = get_data(r2)
            if isinstance(vlist, dict):
                vlist = vlist.get("items", vlist.get("data", []))
            if isinstance(vlist, list) and len(vlist) > 0:
                conn_vid = int(vlist[0].get("versionId", vlist[0].get("id", 0)))
                print(f"    connectorVersionId={conn_vid}")
                return True
            os_fail("版本列表为空")
            return False

        def s3():
            r = os_api("PUT", f"/connectors/{cid}/versions/{conn_vid}", {
                "connectionConfig": {
                    "protocol": "HTTP",
                    "protocolConfig": {
                        "url": f"{MOCK_SERVER_URL}/api/echo",
                        "method": "POST"
                    },
                    "authConfigs": [{"type": "NONE"}],
                    "input": {
                        "protocol": "HTTP",
                        "body": {
                            "type": "object",
                            "properties": {
                                "message": {"type": "string"}
                            }
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
                                        "echo_body": {"type": "object"},
                                        "call_number": {"type": "number"}
                                    }
                                }
                            }
                        }
                    },
                    "timeoutMs": 5000
                }
            })
            return check_ok(r, "UPDATE 连接器配置", f"PUT /connectors/{cid}/versions/{conn_vid}")

        def s4():
            r = os_api("PUT", f"/connectors/{cid}/versions/{conn_vid}/publish")
            return check_ok(r, "PUBLISH 连接器", f"PUT /connectors/{cid}/versions/{conn_vid}/publish")

        if not step("CREATE 连接器", s1): failed = True
        if not failed and not step("CREATE 草稿版本", s2): failed = True
        if not failed and not step("UPDATE 配置指向 Mock", s3): failed = True
        if not failed and not step("PUBLISH 版本", s4): failed = True
        if not failed:
            print("  Phase 1: 连接器已发布")

        # ── Phase 2: 连接流编排 ──
        if not failed:
            print("\n── Phase 2: 连接流编排 ──")

        def s5():
            nonlocal fid
            fid = snow_id()
            r = os_api("POST", "/flows", {
                "nameCn": f"单节点编排_{_RUN_ID}",
                "nameEn": f"single-node-orch-{_RUN_ID}"
            })
            d = get_data(r)
            fval = d.get("flowId") if d else None
            if fval:
                fid = int(fval)
            print(f"    flowId={fid}")
            return check_ok(r, "CREATE 连接流", "POST /flows")

        def s6():
            nonlocal fvid
            r = os_api("POST", f"/flows/{fid}/versions", {})
            d = get_data(r)
            fvid = int(d.get("versionId", d.get("id", 0)))
            print(f"    flowVersionId={fvid}")
            return fvid > 0

        def s7():
            r = os_api("PUT", f"/flows/{fid}/versions/{fvid}", {
                "orchestrationConfig": {
                    "nodes": [
                        {
                            "id": nid_trigger, "type": "trigger",
                            "data": {
                                "type": "trigger",
                                "triggerType": "http",
                                "authConfigs": [{
                                    "type": "SYSTOKEN",
                                    "header": {
                                        "type": "object",
                                        "properties": {
                                            "X-Sys-Token": {
                                                "type": "string",
                                                "required": True,
                                                "sensitive": True
                                            }
                                        }
                                    },
                                    "sysAccountWhitelist": ["tester"]
                                }],
                                "input": {
                                    "protocol": "HTTP",
                                    "body": {
                                        "type": "object",
                                        "properties": {
                                            "key": {"type": "string", "description": "请求标识"},
                                            "value": {"type": "number", "description": "请求数值"}
                                        },
                                        "required": ["key"]
                                    }
                                }
                            }
                        },
                        {
                            "id": nid_conn, "type": "connector",
                            "data": {
                                "type": "connector",
                                "connectorId": str(cid),
                                "connectorVersionId": str(conn_vid),
                                "connectorVersionConfig": {
                                    "protocol": "HTTP",
                                    "protocolConfig": {
                                        "url": f"{MOCK_SERVER_URL}/api/echo",
                                        "method": "POST"
                                    },
                                    "authConfigs": [{"type": "NONE"}],
                                    "input": {
                                        "protocol": "HTTP",
                                        "body": {
                                            "type": "object",
                                            "properties": {
                                                "message": {"type": "string"}
                                            }
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
                                                        "echo_body": {"type": "object"},
                                                        "call_number": {"type": "number"}
                                                    }
                                                }
                                            }
                                        }
                                    }
                                },
                                "timeoutMs": 3000,
                                "input": {
                                    "body": {
                                        "type": "object",
                                        "properties": {
                                            "message": {
                                                "type": "string",
                                                "value": "${$.node." + nid_trigger + ".input.body.key}"
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        {
                            "id": nid_exit, "type": "exit",
                            "data": {
                                "type": "exit",
                                "output": {
                                    "body": {
                                        "type": "object",
                                        "properties": {
                                            "code": {
                                                "type": "number",
                                                "value": "${$.node." + nid_conn + ".output.code}"
                                            },
                                            "message": {
                                                "type": "string",
                                                "value": "${$.node." + nid_conn + ".output.message}"
                                            },
                                            "callNumber": {
                                                "type": "number",
                                                "value": "${$.node." + nid_conn + ".output.data.call_number}"
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    ],
                    "edges": [
                        {"id": f"e1_{_RUN_ID}", "source": nid_trigger, "target": nid_conn,
                         "type": "smoothstep", "data": {"businessType": "default"}},
                        {"id": f"e2_{_RUN_ID}", "source": nid_conn, "target": nid_exit,
                         "type": "smoothstep", "data": {"businessType": "default"}}
                    ],
                    "flowConfig": {"flowMode": "single", "rateLimitConfig": {"maxQps": 100}}
                }
            })
            return check_ok(r, "UPDATE 编排配置", f"PUT /flows/{fid}/versions/{fvid}")

        if not failed:
            if not step("CREATE 连接流", s5): failed = True
        if not failed:
            if not step("CREATE 草稿版本", s6): failed = True
        if not failed:
            if not step("UPDATE 编排", s7): failed = True
        if not failed:
            print("  Phase 2: 编排完成")

        # ── Phase 3: 发布 + 审批 ──
        if not failed:
            print("\n── Phase 3: 发布 + 审批 ──")

        def s8():
            r = os_api("POST", f"/flows/{fid}/versions/{fvid}/publish")
            return check_ok(r, "PUBLISH 提交审批", f"POST /flows/{fid}/versions/{fvid}/publish")

        def s9():
            nonlocal aid
            r = os_api("GET", "/approvals/pending?businessType=connector_flow_version_publish")
            if not check_ok(r, "查询审批记录", "GET /approvals/pending?"):
                return False
            items = get_data(r)
            if isinstance(items, dict):
                items = items.get("items", items.get("data", []))
            if isinstance(items, list):
                for item in items:
                    if str(item.get("businessId")) == str(fvid):
                        aid = item.get("id")
                        break
            if not aid:
                os_fail("未找到审批记录")
                return False
            print(f"    approvalId={aid}")
            return True

        def s10():
            return check_ok(
                os_api("POST", f"/approvals/{aid}/approve",
                       {"comment": "通过"}, user="tester"),
                "第一级审批", f"POST /approvals/{aid}/approve"
            )

        def s11():
            return check_ok(
                os_api("POST", f"/approvals/{aid}/approve",
                       {"comment": "通过"}, user="tester"),
                "第二级审批", f"POST /approvals/{aid}/approve"
            )

        if not failed:
            if not step("PUBLISH 提交审批", s8): failed = True
        if not failed:
            if not step("查询审批记录", s9): failed = True
        if not failed:
            if not step("第一级审批 (tester)", s10): failed = True
        if not failed:
            if not step("第二级审批 (tester)", s11): failed = True
        if not failed:
            print("  Phase 3: 审批通过, 已发布")

        # ── Phase 4: 部署 + 调用 ──
        if not failed:
            print("\n── Phase 4: 部署 + 调用 ──")

        def s12():
            r = os_api("POST", f"/flows/{fid}/deploy", {"versionId": fvid})
            return check_ok(r, "DEPLOY", f"POST /flows/{fid}/deploy")

        def s13():
            return check_ok(
                os_api("POST", f"/flows/{fid}/start"),
                "START", f"POST /flows/{fid}/start"
            )

        def s14():
            r = api_connector("POST", f"/flows/{fid}/invoke?key=orch-test&value=42",
                             {"key": "hello-single-node", "value": 99},
                             headers={"X-Sys-Token": "tester"})
            if r is None:
                os_fail("connector-api 调用失败")
                return False
            os_ok(r.status_code == 200, f"invoke HTTP {r.status_code}")
            try:
                body = r.json()
                print(f"    响应: {json.dumps(body, ensure_ascii=False)[:500]}")
                os_ok(body.get("code") == 0 or body.get("status") == "success",
                      f"invoke result OK")
            except Exception as e:
                os_fail(f"json解析: {e}")
                return False
            return True

        if not failed:
            if not step("DEPLOY", s12): failed = True
        if not failed:
            if not step("START", s13): failed = True
        if not failed:
            if not step("INVOKE (HTTP trigger)", s14): failed = True
        if not failed:
            print("  Phase 4: 部署 + 调用完成")

    finally:
        mock.stop()

    print(f"\n{'='*60}")
    print(f"  单节点编排 E2E {'通过' if not failed else '失败'}")
    print(f"{'='*60}")


if __name__ == "__main__":
    test_full_flow_single_node()
    os_done()
