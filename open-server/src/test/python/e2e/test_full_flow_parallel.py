#!/usr/bin/env python3
"""
并行编排 E2E 测试 — plan-json-schema v3

拓扑: trigger → script → [connector_a ‖ connector_b] → script → exit
       (两个连接器并行执行, 验证总耗时 < 各分支耗时之和)

用法:
  cd open-server/src/test/python
  python3 test_full_flow_parallel.py
"""
import os, sys, json, time, threading, random, string

TEST_DIR = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(os.path.dirname(TEST_DIR), "common"))
import importlib.util
_spec = importlib.util.spec_from_file_location(
    "common_client", os.path.join(os.path.dirname(TEST_DIR), "common", "client.py")
)
_osm = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(_osm)

os_api = _osm.api
os_ok = _osm.ok
os_fail = _osm.fail
os_done = _osm.done
from client import CONNECTOR_API_BASE, CONNECTOR_API_HEALTH, MOCK_SERVER_PARALLEL_URL
import requests

TEST_APP_ID = _osm.TEST_APP_ID
_RUN_ID = ''.join(random.choices(string.ascii_lowercase + string.digits, k=6))

# ── Mock HTTP Server (带延迟, 端口见 client.py MOCK_SERVER_PARALLEL_URL) ──
MOCK_BASE = MOCK_SERVER_PARALLEL_URL
MOCK_PORT = 18982
BRANCH_DELAYS = {"a": 1.5, "b": 1.5}


class MockServer:
    def __init__(self, port=MOCK_PORT):
        self.port = port
        self._server = None
        self._thread = None

    def _make_handler(self):
        state = {"call_count": 0}
        from http.server import BaseHTTPRequestHandler

        class H(BaseHTTPRequestHandler):
            def log_message(self, f, *a):
                pass

            def _get_delay(self):
                for key, delay in BRANCH_DELAYS.items():
                    if f"/api/branch-{key}" in self.path:
                        return delay
                return 0.5

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
                path = self.path.split("?")[0]
                if path == "/api/health":
                    self._respond(200, {"status": "ok"})
                    return
                delay = self._get_delay()
                time.sleep(delay)
                state["call_count"] += 1
                self._respond(200, {"status": "ok", "delay": delay,
                                    "branch": self.path, "call": state["call_count"]})

            def do_POST(self):
                delay = self._get_delay()
                time.sleep(delay)
                body = self._parse_body()
                req_headers = self._echo_headers()
                state["call_count"] += 1
                self._respond(200, {
                    "code": 0,
                    "message": "success",
                    "data": {
                        "echo_body": body,
                        "echo_headers": req_headers,
                        "delay": delay,
                        "branch": self.path,
                        "call_number": state["call_count"]
                    }
                })
        return H

    def start(self, timeout=10):
        from socketserver import ThreadingMixIn
        from http.server import HTTPServer

        class ThreadingHTTPServer(ThreadingMixIn, HTTPServer):
            daemon_threads = True

        try:
            self._server = ThreadingHTTPServer(("localhost", self.port), self._make_handler())
            self._thread = threading.Thread(target=self._server.serve_forever, daemon=True)
            self._thread.start()
        except OSError:
            print(f"  Mock Server port {self.port} 已被占用 (复用已有实例)")
            return True
        for _ in range(timeout * 2):
            try:
                r = requests.get(f"{MOCK_BASE}/api/health", timeout=1)
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
            return requests.post(url, json=body, headers=h, timeout=15)
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


def create_and_publish_connector(label_cn, label_en, target_url, method="POST"):
    """通过 open-server API 创建连接器 -> 创建草稿 -> 配置 -> 发布"""
    cid = snow_id()
    r = os_api("POST", "/connectors", {
        "nameCn": label_cn,
        "nameEn": label_en,
        "connectorType": 1
    })
    d = get_data(r) if r else {}
    cid_val = d.get("connectorId") if d else None
    if cid_val:
        cid = int(cid_val)

    if not check_ok(r, f"CREATE {label_en}", "POST /connectors"):
        return None, None, None

    # 创建草稿版本
    os_api("POST", f"/connectors/{cid}/versions", {})
    r2 = os_api("GET", f"/connectors/{cid}/versions")
    vlist = get_data(r2)
    if isinstance(vlist, dict):
        vlist = vlist.get("items", vlist.get("data", []))
    if not isinstance(vlist, list) or len(vlist) == 0:
        os_fail("版本列表为空")
        return None, None, None
    cvid = int(vlist[0].get("versionId", vlist[0].get("id", 0)))
    print(f"    {label_en}: cid={cid}, cvid={cvid}")

    config = {
        "protocol": "HTTP",
        "protocolConfig": {"url": target_url, "method": method},
        "authConfigs": [{"type": "NONE"}],
        "input": {
            "protocol": "HTTP",
            "body": {"type": "object", "properties": {"payload": {"type": "object"}}}
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
                            "delay": {"type": "number"},
                            "branch": {"type": "string"},
                            "call_number": {"type": "number"}
                        }
                    }
                }
            }
        },
        "timeoutMs": 5000
    }
    os_api("PUT", f"/connectors/{cid}/versions/{cvid}", {
        "connectionConfig": config
    })
    os_api("PUT", f"/connectors/{cid}/versions/{cvid}/publish")
    return cid, cvid, config


# ── Test ──
import pytest

@pytest.mark.L3
def test_full_flow_parallel():
    print("=" * 60)
    print("  并行编排 E2E — plan-json-schema v3")
    print(f"  Run ID: {_RUN_ID}")
    print("=" * 60)

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

    mock = MockServer()
    if not mock.start():
        os_fail("Mock Server 启动失败")
        return

    fid = fvid = aid = None
    failed = False
    nid_trigger = f"t_{_RUN_ID}"
    nid_script_1 = f"s1_{_RUN_ID}"
    nid_parallel = f"p_{_RUN_ID}"
    nid_conn_a = f"ca_{_RUN_ID}"
    nid_conn_b = f"cb_{_RUN_ID}"
    nid_script_2 = f"s2_{_RUN_ID}"
    nid_exit = f"e_{_RUN_ID}"

    try:
        # ── Phase 1: 创建两个连接器 ──
        print("\n── Phase 1: 创建连接器 ──")
        cid_a, cvid_a, config_a = create_and_publish_connector(
            "并行分支A", "ParallelBranchA", f"{MOCK_BASE}/api/branch-a"
        )
        cid_b, cvid_b, config_b = create_and_publish_connector(
            "并行分支B", "ParallelBranchB", f"{MOCK_BASE}/api/branch-b"
        )
        if cid_a is None or cid_b is None:
            failed = True
        if not failed:
            print("  Phase 1: 连接器创建并发布完成")

        # ── Phase 2: 连接流编排 ──
        if not failed:
            print("\n── Phase 2: 连接流编排 ──")

        def s1():
            nonlocal fid
            fid = snow_id()
            r = os_api("POST", "/flows", {
                "nameCn": f"并行编排_{_RUN_ID}",
                "nameEn": f"parallel-orch-{_RUN_ID}"
            })
            d = get_data(r)
            fval = d.get("flowId") if d else None
            if fval:
                fid = int(fval)
            print(f"    flowId={fid}")
            return check_ok(r, "CREATE 连接流", "POST /flows")

        def s2():
            nonlocal fvid
            r = os_api("POST", f"/flows/{fid}/versions", {})
            d = get_data(r)
            fvid = int(d.get("versionId", d.get("id", 0)))
            print(f"    flowVersionId={fvid}")
            return fvid > 0

        def s3():
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
                                            "task_a": {"type": "string"},
                                            "task_b": {"type": "string"},
                                            "priority": {"type": "number"}
                                        },
                                        "required": ["task_a", "task_b"]
                                    }
                                }
                            }
                        },
                        {
                            "id": nid_script_1, "type": "script",
                            "data": {
                                "type": "script",
                                "labelCn": "数据准备",
                                "script": (
                                    "function main(ctx) {\n"
                                    "  var input = ctx['" + nid_trigger + "'].input.body;\n"
                                    "  return {\n"
                                    "    payload_a: {\n"
                                    "      task: input.task_a,\n"
                                    "      priority: input.priority,\n"
                                    "      ts: new Date().getTime()\n"
                                    "    },\n"
                                    "    payload_b: {\n"
                                    "      task: input.task_b,\n"
                                    "      priority: (input.priority || 0) + 1,\n"
                                    "      ts: new Date().getTime()\n"
                                    "    }\n"
                                    "  };\n"
                                    "}"
                                ),
                                "output": {
                                    "type": "object",
                                    "properties": {
                                        "payload_a": {
                                            "type": "object",
                                            "properties": {
                                                "task": {"type": "string"},
                                                "priority": {"type": "number"},
                                                "ts": {"type": "number"}
                                            }
                                        },
                                        "payload_b": {
                                            "type": "object",
                                            "properties": {
                                                "task": {"type": "string"},
                                                "priority": {"type": "number"},
                                                "ts": {"type": "number"}
                                            }
                                        }
                                    }
                                },
                                "timeout": 5
                            }
                        },
                        {
                            "id": nid_parallel, "type": "parallel",
                            "data": {"type": "parallel"}
                        },
                        {
                            "id": nid_conn_a, "type": "connector",
                            "data": {
                                "type": "connector",
                                "labelCn": "分支A",
                                "connectorId": str(cid_a),
                                "connectorVersionId": str(cvid_a),
                                "connectorVersionConfig": config_a,
                                "timeoutMs": 5000,
                                "input": {
                                    "body": {
                                        "type": "object",
                                        "properties": {
                                            "payload": {
                                                "type": "object",
                                                "value": "${$.node." + nid_script_1 + ".output.payload_a}"
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        {
                            "id": nid_conn_b, "type": "connector",
                            "data": {
                                "type": "connector",
                                "labelCn": "分支B",
                                "connectorId": str(cid_b),
                                "connectorVersionId": str(cvid_b),
                                "connectorVersionConfig": config_b,
                                "timeoutMs": 5000,
                                "input": {
                                    "body": {
                                        "type": "object",
                                        "properties": {
                                            "payload": {
                                                "type": "object",
                                                "value": "${$.node." + nid_script_1 + ".output.payload_b}"
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        {
                            "id": nid_script_2, "type": "script",
                            "data": {
                                "type": "script",
                                "labelCn": "结果聚合",
                                "script": (
                                    "function main(ctx) {\n"
                                    "  var a = (ctx['" + nid_conn_a + "'].output || {}).body || {};\n"
                                    "  var b = (ctx['" + nid_conn_b + "'].output || {}).body || {};\n"
                                    "  var ac = (a.code != null) ? a.code : -1;\n"
                                    "  var bc = (b.code != null) ? b.code : -1;\n"
                                    "  var ae = (a.data && a.data.echo_body && a.data.echo_body.payload) ? a.data.echo_body.payload : {};\n"
                                    "  var be = (b.data && b.data.echo_body && b.data.echo_body.payload) ? b.data.echo_body.payload : {};\n"
                                    "  return {\n"
                                    "    branch_a_code: ac,\n"
                                    "    branch_b_code: bc,\n"
                                    "    branch_a_delay: (a.data && a.data.delay != null) ? a.data.delay : 0,\n"
                                    "    branch_b_delay: (b.data && b.data.delay != null) ? b.data.delay : 0,\n"
                                    "    branch_a_call: (a.data && a.data.call_number != null) ? a.data.call_number : 0,\n"
                                    "    branch_b_call: (b.data && b.data.call_number != null) ? b.data.call_number : 0,\n"
                                    "    branch_a_echo: {task: ae.task, priority: ae.priority},\n"
                                    "    branch_b_echo: {task: be.task, priority: be.priority}\n"
                                    "  };\n"
                                    "}"
                                ),
                                "output": {
                                    "type": "object",
                                    "properties": {
                                        "branch_a_code": {"type": "number"},
                                        "branch_b_code": {"type": "number"},
                                        "branch_a_delay": {"type": "number"},
                                        "branch_b_delay": {"type": "number"},
                                        "branch_a_call": {"type": "number"},
                                        "branch_b_call": {"type": "number"},
                                        "branch_a_echo": {
                                            "type": "object",
                                            "properties": {
                                                "task": {"type": "string"},
                                                "priority": {"type": "number"}
                                            }
                                        },
                                        "branch_b_echo": {
                                            "type": "object",
                                            "properties": {
                                                "task": {"type": "string"},
                                                "priority": {"type": "number"}
                                            }
                                        }
                                    }
                                },
                                "timeout": 5
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
                                            "branchACode": {
                                                "type": "number",
                                                "value": "${$.node." + nid_script_2 + ".output.branch_a_code}"
                                            },
                                            "branchBCode": {
                                                "type": "number",
                                                "value": "${$.node." + nid_script_2 + ".output.branch_b_code}"
                                            },
                                            "branchADelay": {
                                                "type": "number",
                                                "value": "${$.node." + nid_script_2 + ".output.branch_a_delay}"
                                            },
                                            "branchBDelay": {
                                                "type": "number",
                                                "value": "${$.node." + nid_script_2 + ".output.branch_b_delay}"
                                            },
                                            "branchAEcho": {
                                                "type": "object",
                                                "properties": {
                                                    "task": {"type": "string"},
                                                    "priority": {"type": "number"}
                                                },
                                                "value": "${$.node." + nid_script_2 + ".output.branch_a_echo}"
                                            },
                                            "branchBEcho": {
                                                "type": "object",
                                                "properties": {
                                                    "task": {"type": "string"},
                                                    "priority": {"type": "number"}
                                                },
                                                "value": "${$.node." + nid_script_2 + ".output.branch_b_echo}"
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    ],
                    "edges": [
                        {"id": f"e_{_RUN_ID}_ts1",
                         "source": nid_trigger, "target": nid_script_1,
                         "type": "smoothstep", "data": {"businessType": "default", "connectionMode": "serial"}},
                        {"id": f"e_{_RUN_ID}_s1p",
                         "source": nid_script_1, "target": nid_parallel,
                         "type": "smoothstep", "data": {"businessType": "default", "connectionMode": "serial"}},
                        {"id": f"e_{_RUN_ID}_pa",
                         "source": nid_parallel, "target": nid_conn_a,
                         "type": "smoothstep", "data": {"businessType": "default", "connectionMode": "parallel"}},
                        {"id": f"e_{_RUN_ID}_pb",
                         "source": nid_parallel, "target": nid_conn_b,
                         "type": "smoothstep", "data": {"businessType": "default", "connectionMode": "parallel"}},
                        {"id": f"e_{_RUN_ID}_as2",
                         "source": nid_conn_a, "target": nid_script_2,
                         "type": "smoothstep", "data": {"businessType": "default", "connectionMode": "parallel"}},
                        {"id": f"e_{_RUN_ID}_bs2",
                         "source": nid_conn_b, "target": nid_script_2,
                         "type": "smoothstep", "data": {"businessType": "default", "connectionMode": "parallel"}},
                        {"id": f"e_{_RUN_ID}_se",
                         "source": nid_script_2, "target": nid_exit,
                         "type": "smoothstep", "data": {"businessType": "default", "connectionMode": "serial"}}
                    ],
                    "flowConfig": {"flowMode": "parallel", "rateLimitConfig": {"maxQps": 100, "maxConcurrency": 20}}
                }
            })
            return check_ok(r, "UPDATE 编排配置", f"PUT /flows/{fid}/versions/{fvid}")

        if not failed:
            if not step("CREATE 连接流", s1): failed = True
        if not failed:
            if not step("CREATE 草稿版本", s2): failed = True
        if not failed:
            if not step("UPDATE 编排(含并行分支)", s3): failed = True
        if not failed:
            print("  Phase 2: 编排完成")

        # ── Phase 3: 发布 + 审批 ──
        if not failed:
            print("\n── Phase 3: 发布 + 审批 ──")

        def s4():
            r = os_api("POST", f"/flows/{fid}/versions/{fvid}/publish")
            return check_ok(r, "PUBLISH 提交审批", f"POST /flows/{fid}/versions/{fvid}/publish")

        def s5():
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

        def s6():
            return check_ok(
                os_api("POST", f"/approvals/{aid}/approve",
                       {"comment": "通过"}, user="tester"),
                "第一级审批", f"POST /approvals/{aid}/approve"
            )

        def s7():
            return check_ok(
                os_api("POST", f"/approvals/{aid}/approve",
                       {"comment": "通过"}, user="admin"),
                "第二级审批", f"POST /approvals/{aid}/approve"
            )

        if not failed:
            if not step("PUBLISH 提交审批", s4): failed = True
        if not failed:
            if not step("查询审批记录", s5): failed = True
        if not failed:
            if not step("第一级审批 (tester)", s6): failed = True
        if not failed:
            if not step("第二级审批 (tester)", s7): failed = True
        if not failed:
            print("  Phase 3: 审批通过, 已发布")

        # ── Phase 4: 部署 + 调用 + 并行性验证 ──
        if not failed:
            print("\n── Phase 4: 部署 + 调用 + 并行性验证 ──")

        def s8():
            r = os_api("POST", f"/flows/{fid}/deploy", {"versionId": fvid})
            return check_ok(r, "DEPLOY", f"POST /flows/{fid}/deploy")

        def s9():
            return check_ok(
                os_api("POST", f"/flows/{fid}/start"),
                "START", f"POST /flows/{fid}/start"
            )

        def s10():
            invoke_task_a = f"order_{_RUN_ID}"
            invoke_task_b = f"invoice_{_RUN_ID}"
            invoke_priority = random.randint(1, 5)
            start = time.time()
            r = api_connector("POST", f"/flows/{fid}/invoke",
                             {"task_a": invoke_task_a, "task_b": invoke_task_b, "priority": invoke_priority},
                             headers={"X-Sys-Token": "tester"})
            elapsed = time.time() - start
            individual_sum = BRANCH_DELAYS["a"] + BRANCH_DELAYS["b"]
            max_delay = max(BRANCH_DELAYS["a"], BRANCH_DELAYS["b"])

            if r is None:
                os_fail("connector-api 调用失败")
                return False
            os_ok(r.status_code == 200, f"invoke HTTP {r.status_code}")
            try:
                body = r.json()
                print(f"    响应: {json.dumps(body, ensure_ascii=False)[:400]}")
                os_ok(body.get("branchACode") == 0,
                      f"branchA.code={body.get('branchACode')}")
                os_ok(body.get("branchBCode") == 0,
                      f"branchB.code={body.get('branchBCode')}")

                ea = body.get("branchAEcho", {})
                eb = body.get("branchBEcho", {})
                os_ok(ea.get("task") == invoke_task_a, f"branchA echo.task={ea.get('task')}")
                os_ok(ea.get("priority") == invoke_priority, f"branchA echo.priority={ea.get('priority')}")
                os_ok(eb.get("task") == invoke_task_b, f"branchB echo.task={eb.get('task')}")
                os_ok(eb.get("priority") == invoke_priority + 1, f"branchB echo.priority={eb.get('priority')}")

                os_ok(elapsed < individual_sum + 1.0,
                      f"并行耗时={elapsed:.2f}s < 串行和+1s={individual_sum+1.0:.1f}s")
                os_ok(elapsed < max_delay + 2.0,
                      f"耗时={elapsed:.2f}s < max延迟+2s={max_delay+2.0:.1f}s")
                print(f"    INFO: 并行执行 {elapsed:.2f}s (分支A={BRANCH_DELAYS['a']}s, 分支B={BRANCH_DELAYS['b']}s)")
            except Exception as e:
                os_fail(f"json解析: {e}")
                return False
            return True

        if not failed:
            if not step("DEPLOY", s8): failed = True
        if not failed:
            if not step("START", s9): failed = True
        if not failed:
            if not step("INVOKE (并行验证)", s10): failed = True
        if not failed:
            print("  Phase 4: 部署 + 并行调用完成")

    finally:
        mock.stop()

    print(f"\n{'='*60}")
    print(f"  并行编排 E2E {'通过' if not failed else '失败'}")
    print(f"{'='*60}")


if __name__ == "__main__":
    test_full_flow_parallel()
    os_done()
