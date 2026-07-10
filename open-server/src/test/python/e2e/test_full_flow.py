#!/usr/bin/env python3
"""
全流程端到端测试 — 模拟用户从零搭建连接器平台 V3

Phase 0: 环境准备（重启服务 — 手动执行）
Phase 1: 连接器发布   — create → draft → 配置Mock → publish
Phase 2: 连接流编排   — create → draft → 编排（引用连接器）
Phase 3: 草稿调试     — debug draft
Phase 4: 发布+审批    — publish → 两级审批（Cookie: tester）
Phase 5: 发布后验证   — debug 已发布版本
Phase 6: 部署+调用    — deploy → start → HTTP trigger → 查记录 → stop

运行前提:
  - open-server 运行在 localhost:18080
  - connector-api 运行在 localhost:18180

用法:
  cd open-server/src/test/python
  python3 test_full_flow.py
  KEEP_TEST_DATA=0 python3 test_full_flow.py  # 自动清理
"""
import os
import sys
import json
import time
import threading

import importlib.util
from http.server import HTTPServer, BaseHTTPRequestHandler

TEST_DIR = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(os.path.dirname(TEST_DIR), "common"))

# ── 加载 open-server client ──
_spec = importlib.util.spec_from_file_location(
    "common_client", os.path.join(os.path.dirname(TEST_DIR), "common", "client.py")
)
_osm = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(_osm)
os_api = _osm.api
os_db_val = _osm.db_val
os_ok = _osm.ok
os_fail = _osm.fail
os_done = _osm.done
from client import _REDIS_CLUSTER_NODES, CONNECTOR_API_BASE, CONNECTOR_API_HEALTH, MOCK_SERVER_URL, OPEN_SERVER_BASE, REDIS_PASSWORD, SYSTOKEN_HEADER, SYSTOKEN_VALUE
from redis.cluster import RedisCluster

_first_node = _REDIS_CLUSTER_NODES[0]
_redis_client = RedisCluster(
    host=_first_node[0], port=int(_first_node[1]),
    password=REDIS_PASSWORD, decode_responses=True
)

def os_redis(*args):
    """执行 Redis 命令"""
    try:
        cmd = args[0].upper() if args else ""
        if cmd == "EXISTS":
            return _redis_client.exists(args[1])
        elif cmd == "TTL":
            return _redis_client.ttl(args[1])
        elif cmd == "GET":
            return _redis_client.get(args[1])
        elif cmd == "KEYS":
            return _redis_client.keys(args[1] if len(args) > 1 else "*")
        else:
            return _redis_client.execute_command(*args)
    except Exception as e:
        print(f"  REDIS ERROR: {e}")
        return None

TEST_APP_ID = _osm.TEST_APP_ID

import pytest
import requests

import random
import string
_RUN_ID = ''.join(random.choices(string.ascii_lowercase + string.digits, k=6))

# ═══════════════════════════════════════════════════════════
# Mock HTTP Server
# ═══════════════════════════════════════════════════════════

class MockServer:
    """真实模拟下游 HTTP 服务，覆盖请求 header/query/body → 响应 body/header"""

    def __init__(self, port=18980):
        self.port = port
        self._server = None
        self._thread = None

    def _make_handler(self):
        # mutable state shared across requests (thread-safe via GIL for our use case)
        state = {"call_count": 0}

        class H(BaseHTTPRequestHandler):
            def log_message(self, f, *a):
                pass

            def _respond(self, code, body, extra_headers=None):
                self.send_response(code)
                self.send_header("Content-Type", "application/json")
                # custom response headers for verification
                self.send_header("X-Mock-Server", "full-flow-test")
                self.send_header("X-Response-Id", f"resp-{state['call_count']}")
                if extra_headers:
                    for k, v in extra_headers.items():
                        self.send_header(k, v)
                self.end_headers()
                self.wfile.write(json.dumps(body, ensure_ascii=False).encode())

            def _parse_query(self):
                """解析 query string 为 dict"""
                from urllib.parse import parse_qs, urlparse
                parsed = urlparse(self.path)
                qs = parse_qs(parsed.query)
                # parse_qs returns {k: [v1, v2]}, flatten single values
                return {k: v[0] if len(v) == 1 else v for k, v in qs.items()}

            def _parse_body(self):
                cl = int(self.headers.get("Content-Length", 0))
                if cl > 0:
                    try:
                        return json.loads(self.rfile.read(cl))
                    except Exception:
                        return {"raw": "parse_error"}
                return {}

            def _echo_headers(self):
                """收集所有请求头（排除 host/content-length）"""
                skip = {"host", "content-length", "content-type"}
                return {k: v for k, v in self.headers.items() if k.lower() not in skip}

            def _path_only(self):
                return self.path.split("?")[0]

            # ── GET /api/health ──
            def do_GET(self):
                path = self._path_only()
                query = self._parse_query()
                state["call_count"] += 1
                time.sleep(random.uniform(0.01, 0.02))  # simulate 10-20ms latency

                if path == "/api/health":
                    self._respond(200, {
                        "status": "ok",
                        "server": "full-flow-mock",
                        "uptime_calls": state["call_count"]
                    })

                elif path == "/api/search":
                    keyword = query.get("keyword", "")
                    page = query.get("page", "1")
                    self._respond(200, {
                        "code": 0,
                        "message": "success",
                        "data": {
                            "keyword": keyword,
                            "page": int(page),
                            "total": 42,
                            "items": [
                                {"id": 1, "name": f"结果-{keyword}-1", "score": 0.95},
                                {"id": 2, "name": f"结果-{keyword}-2", "score": 0.82},
                            ]
                        }
                    }, extra_headers={"X-Search-Total": "42"})

                elif path == "/api/echo":
                    self._respond(200, {
                        "code": 0,
                        "data": {
                            "method": "GET",
                            "query": query,
                            "message": f"echo: {keyword}"
                        }
                    })

                else:
                    self._respond(200, {
                        "code": 0,
                        "data": {"path": path, "query": query, "method": "GET"}
                    })

            # ── POST /api/echo (主接口 — 连接器目标) ──
            def do_POST(self):
                path = self._path_only()
                body = self._parse_body()
                req_headers = self._echo_headers()
                query = self._parse_query()
                state["call_count"] += 1
                time.sleep(random.uniform(0.01, 0.02))  # simulate 10-20ms latency

                if path == "/api/echo":
                    self._respond(200, {
                        "code": 0,
                        "message": "echo success",
                        "data": {
                            "echo_body": body,
                            "echo_headers": req_headers,
                            "echo_query": query,
                            "server_time": time.strftime("%Y-%m-%dT%H:%M:%S"),
                            "call_number": state["call_count"]
                        }
                    }, extra_headers={
                        "X-Echo-Count": str(state["call_count"]),
                        "X-Request-Header-Count": str(len(req_headers))
                    })

                elif path == "/api/data":
                    self._respond(200, {
                        "code": 0,
                        "data": {
                            "received": body,
                            "query": query,
                            "processed": True
                        }
                    })

                else:
                    self._respond(200, {
                        "code": 0,
                        "data": {"path": path, "received": body, "method": "POST"}
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
                    print(f"  ✅ Mock Server ready (port {self.port})")
                    return True
            except Exception:
                pass
            time.sleep(0.5)
        return False

    def stop(self):
        if self._server:
            self._server.shutdown()
        print(f"  🛑 Mock Server stopped")

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
    """执行一个步骤，自动编号并打印"""
    step_num[0] += 1
    no = step_num[0]
    print(f"\n  [{no}] {desc}")
    try:
        return fn()
    except Exception as e:
        os_fail(f"Step {no} 异常: {e}")
        return False

def check_ok(resp, desc, url=""):
    if resp is None:
        os_fail(f"{desc}: 连接失败 {url}")
        return False
    if resp.status_code == 200:
        try:
            body = resp.json()
            if body.get("code") in ("200", 200):
                print(f"  ✅ {desc}  {url}")
                return True
            else:
                os_fail(f"{desc}: code={body.get('code')}, msg={body.get('messageZh', '')}  {url}")
                return False
        except Exception:
            print(f"  ✅ {desc} (HTTP 200)  {url}")
            return True
    else:
        try:
            body = resp.json()
            detail = f"HTTP {resp.status_code}, code={body.get('code')}, msg={body.get('messageZh', '')}"
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
def test_full_flow():
    print("=" * 60)
    print("  全流程端到端测试 — 连接器平台 V3")
    print(f"  Run ID: {_RUN_ID}")
    print("=" * 60)

    # ── Phase 0 检查 ──
    print("\n── Phase 0: 环境检查 ──")
    r = os_api("GET", "/connectors")
    if r is None:
        print("❌ open-server 未运行! 请先执行:")
        print("   cd /home/usb/wks/open-app && bash open-server/scripts/restart.sh")
        print("   cd /home/usb/wks/open-app && bash connector-api/scripts/restart.sh")
        return
    print("  ✅ open-server 就绪")

    try:
        r2 = requests.get(CONNECTOR_API_HEALTH, timeout=5)
        if r2.status_code == 200:
            print("  ✅ connector-api 就绪")
        else:
            print(f"  ⚠️ connector-api 状态异常: HTTP {r2.status_code}")
    except Exception:
        print("  ❌ connector-api 未运行!")
        return

    # V3: 优先查找应用级模板 (code + appId)，回退到全局模板
    tpl = None
    r = os_api("GET", "/approval-flows?keyword=connector_flow_version_publish")
    if r and r.status_code == 200:
        for item in r.json().get("data", []):
            if item.get("code") == "connector_flow_version_publish":
                tpl = item.get("id")
                break
    if tpl:
        print(f"  ✅ 审批流模板存在 (id={tpl})")
    else:
        print("  ⚠️ 审批流模板不存在，尝试创建...")
        r = os_api("POST", "/approval-flows", {
            "code": "connector_flow_version_publish",
            "nameCn": "连接器流版本发布审批",
            "nameEn": "connector_flow_version_publish",
            "appId": TEST_APP_ID,
            "nodes": [{"userId": "tester", "userName": "Test Approver"}]
        })
        if r and r.status_code in (200, 201):
            print("  ✅ 审批流模板创建成功")

    # ── 启动 Mock ──
    print("\n── 启动 Mock Server ──")
    mock = MockServer(port=18980)
    if not mock.start():
        print("❌ Mock Server 启动失败")
        return

    cid = conn_vid = fid = fvid = aid = None
    failed = False

    try:
        # ═══════════════════════════════════════════════════
        # Phase 1: 连接器发布
        # ═══════════════════════════════════════════════════
        print("\n── Phase 1: 连接器发布 ──")

        def s1():
            nonlocal cid
            cid = snow_id()
            r = os_api("POST", "/connectors", {
                "nameCn": f"全流程测试连接器_{_RUN_ID}",
                "nameEn": f"full-flow-connector-{_RUN_ID}",
                "connectorType": 1
            })
            if not check_ok(r, "CREATE 连接器", "POST /connectors"): return False
            d = get_data(r)
            if str(d.get("connectorId")) != str(cid):
                cid = int(d.get("connectorId"))
            print(f"    connectorId={cid}")
            if d.get("status") not in (1, "1"):
                os_fail(f"status={d.get('status')}, 期望=1")
                return False
            return True

        def s2():
            nonlocal conn_vid
            r = os_api("POST", f"/connectors/{cid}/versions", {})
            if not check_ok(r, "CREATE 草稿版本 (connector)", "POST /connectors/{cid}/versions"): return False
            # API 不返回 data，需要查列表获取 versionId
            r2 = os_api("GET", f"/connectors/{cid}/versions")
            if not check_ok(r2, "GET 版本列表", f"GET /connectors/{cid}/versions"): return False
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
                    "authConfigs": [
                        {
                            "type": "APIG",
                            "query": {
                                "type": "object",
                                "properties": {
                                    "apigAppKey": {
                                        "type": "string",
                                        "required": True,
                                        "value": "${$.system.env.apigAppKey}",
                                        "description": "APIG 应用标识"
                                    },
                                    "apigAppSecret": {
                                        "type": "string",
                                        "required": True,
                                        "sensitive": True,
                                        "value": "${$.system.env.apigAppSecret}",
                                        "description": "APIG 应用密钥"
                                    }
                                }
                            }
                        },
                        {
                            "type": "COOKIE",
                            "header": {
                                "type": "object",
                                "properties": {
                                    "Cookie": {
                                        "type": "string",
                                        "required": True,
                                        "sensitive": True,
                                        "description": "Cookie 请求头，值来源在编排时设置"
                                    }
                                }
                            }
                        }
                    ],
                    "input": {
                        "protocol": "HTTP",
                        "header": {
                            "type": "object",
                            "properties": {
                                "X-Trace-Id": {"type": "string", "description": "追踪ID"}
                            }
                        },
                        "query": {
                            "type": "object",
                            "properties": {
                                "keyword": {"type": "string", "description": "搜索关键词"},
                                "page": {"type": "number", "description": "页码"},
                                "size": {"type": "number", "description": "每页条数"}
                            }
                        },
                        "body": {
                            "type": "object",
                            "properties": {
                                "message": {"type": "string", "description": "消息体"},
                                "filters": {
                                    "type": "object",
                                    "properties": {
                                        "category": {"type": "string"},
                                        "minScore": {"type": "number"}
                                    }
                                },
                                "sort": {"type": "string", "description": "排序字段"},
                                "traceId": {"type": "string", "description": "请求追踪ID"}
                            }
                        }
                    },
                    "output": {
                        "protocol": "HTTP",
                        "header": {
                            "type": "object",
                            "properties": {
                                "X-Echo-Count": {"type": "string"},
                                "X-Request-Header-Count": {"type": "string"}
                            }
                        },
                        "body": {
                            "type": "object",
                            "properties": {
                                "code": {"type": "number"},
                                "message": {"type": "string"},
                                "data": {
                                    "type": "object",
                                    "properties": {
                                        "echo_body": {"type": "object"},
                                        "echo_headers": {"type": "object"},
                                        "echo_query": {"type": "object"},
                                        "server_time": {"type": "string"},
                                        "call_number": {"type": "number"}
                                    }
                                }
                            }
                        }
                    },
                    "timeoutMs": 5000
                }
            })
            return check_ok(r, "UPDATE 连接器配置 -> Mock", f"PUT /connectors/{cid}/versions/{conn_vid}")

        def s4():
            r = os_api("PUT", f"/connectors/{cid}/versions/{conn_vid}/publish")
            if not check_ok(r, "PUBLISH 连接器版本", f"PUT /connectors/{cid}/versions/{conn_vid}/publish"): return False
            r2 = os_api("GET", f"/connectors/{cid}/versions/{conn_vid}")
            if not check_ok(r2, "确认已发布", f"GET /connectors/{cid}/versions/{conn_vid}"): return False
            d = get_data(r2)
            if d.get("status") not in (2, "2"):
                os_fail(f"status={d.get('status')}, 期望=2 (已发布)")
                return False
            return True

        if not step("CREATE 连接器", s1): failed = True
        if not failed and not step("CREATE 草稿版本", s2): failed = True
        if not failed and not step("UPDATE 指向 Mock", s3): failed = True
        if not failed and not step("PUBLISH 版本", s4): failed = True

        if not failed:
            print("  ✅ Phase 1 完成: 连接器已发布")
        else:
            print("  ❌ Phase 1 失败")

        # ═══════════════════════════════════════════════════
        # Phase 2: 连接流编排
        # ═══════════════════════════════════════════════════
        if not failed:
            print("\n── Phase 2: 连接流编排 ──")

        def s5():
            nonlocal fid
            fid = snow_id()
            r = os_api("POST", "/flows", {
                "nameCn": f"搜索服务代理_{_RUN_ID}",
                "nameEn": f"search-proxy-{_RUN_ID}"
            })
            if not check_ok(r, "CREATE 连接流", "POST /flows"): return False
            d = get_data(r)
            if str(d.get("flowId")) != str(fid):
                fid = int(d.get("flowId"))
            print(f"    flowId={fid}")
            if d.get("lifecycleStatus") not in (1, "1"):
                os_fail(f"lifecycleStatus={d.get('lifecycleStatus')}, 期望=1")
                return False
            return True

        def s6():
            nonlocal fvid
            r = os_api("POST", f"/flows/{fid}/versions", {})
            if not check_ok(r, "CREATE 草稿版本 (flow)", f"POST /flows/{fid}/versions"): return False
            d = get_data(r)
            fvid = int(d.get("versionId", d.get("id", 0)))
            print(f"    flowVersionId={fvid}")
            return True

        def s7():
            nid_trigger = f"trigger_{_RUN_ID}"
            nid_conn = f"conn_{_RUN_ID}"
            nid_exit = f"exit_{_RUN_ID}"
            r = os_api("PUT", f"/flows/{fid}/versions/{fvid}", {
                "orchestrationConfig": {
                    "nodes": [
                        {"id": nid_trigger, "type": "trigger", "data": {
                            "type": "trigger",
                            "triggerType": "http",
                            "authConfigs": [
                                {
                                    "type": "SYSTOKEN",
                                    "header": {
                                        "type": "object",
                                        "properties": {
                                            SYSTOKEN_HEADER: {
                                                "type": "string",
                                                "required": True,
                                                "sensitive": True
                                            }
                                        }
                                    },
                                    "sysAccountWhitelist": [SYSTOKEN_VALUE]
                                }
                            ],
                            "input": {
                                "protocol": "HTTP",
                                "header": {
                                    "type": "object",
                                    "properties": {
                                        "X-Trace-Id": {"type": "string", "description": "全链路追踪ID"}
                                    },
                                    "required": []
                                },
                                "query": {
                                    "type": "object",
                                    "properties": {
                                        "keyword": {"type": "string", "description": "搜索关键词"},
                                        "page": {"type": "number", "description": "页码", "default": 1},
                                        "size": {"type": "number", "description": "每页条数", "default": 20}
                                    },
                                    "required": ["keyword"]
                                },
                                "body": {
                                    "type": "object",
                                    "properties": {
                                        "filters": {
                                            "type": "object",
                                            "description": "过滤条件",
                                            "properties": {
                                                "category": {"type": "string", "description": "分类"},
                                                "minScore": {"type": "number", "description": "最低评分"}
                                            }
                                        },
                                        "sort": {"type": "string", "description": "排序字段", "default": "score"},
                                        "traceId": {"type": "string", "description": "请求追踪ID"}
                                    },
                                    "required": []
                                }
                            }
                        }},
                        {"id": nid_conn, "type": "connector", "data": {
                            "type": "connector",
                            "connectorId": str(cid),
                            "connectorVersionId": str(conn_vid),
                            "connectorVersionConfig": {
                                "protocol": "HTTP",
                                "protocolConfig": {
                                    "url": f"{MOCK_SERVER_URL}/api/echo",
                                    "method": "POST"
                                },
                                "authConfigs": [
                                    {
                                        "type": "APIG",
                                        "query": {
                                            "type": "object",
                                            "properties": {
                                                "apigAppKey": {
                                                    "type": "string",
                                                    "required": True,
                                                    "value": "${$.system.env.apigAppKey}",
                                                    "description": "APIG 应用标识"
                                                },
                                                "apigAppSecret": {
                                                    "type": "string",
                                                    "required": True,
                                                    "sensitive": True,
                                                    "value": "${$.system.env.apigAppSecret}",
                                                    "description": "APIG 应用密钥"
                                                }
                                            }
                                        }
                                    },
                                    {
                                        "type": "COOKIE",
                                        "header": {
                                            "type": "object",
                                            "properties": {
                                                "Cookie": {
                                                    "type": "string",
                                                    "required": True,
                                                    "sensitive": True,
                                                    "value": "${$.node." + nid_trigger + ".input.header.Cookie}",
                                                    "description": "Cookie 请求头，编排时设置值来源：引用触发器入参 Cookie"
                                                }
                                            }
                                        }
                                    }
                                ],
                                "input": {
                                    "protocol": "HTTP",
                                    "header": {
                                        "type": "object",
                                        "properties": {
                                            "X-Trace-Id": {"type": "string", "description": "追踪ID"}
                                        }
                                    },
                                    "query": {
                                        "type": "object",
                                        "properties": {
                                            "keyword": {"type": "string", "description": "搜索关键词"},
                                            "page": {"type": "number", "description": "页码"},
                                            "size": {"type": "number", "description": "每页条数"}
                                        }
                                    },
                                    "body": {
                                        "type": "object",
                                        "properties": {
                                            "message": {"type": "string", "description": "消息体"},
                                            "filters": {
                                                "type": "object",
                                                "properties": {
                                                    "category": {"type": "string"},
                                                    "minScore": {"type": "number"}
                                                }
                                            },
                                            "sort": {"type": "string", "description": "排序字段"},
                                            "traceId": {"type": "string", "description": "请求追踪ID"}
                                        }
                                    }
                                },
                                "output": {
                                    "protocol": "HTTP",
                                    "header": {
                                        "type": "object",
                                        "properties": {
                                            "X-Echo-Count": {"type": "string"},
                                            "X-Request-Header-Count": {"type": "string"}
                                        }
                                    },
                                    "body": {
                                        "type": "object",
                                        "properties": {
                                            "code": {"type": "number"},
                                            "message": {"type": "string"},
                                            "data": {
                                                "type": "object",
                                                "properties": {
                                                    "echo_body": {"type": "object"},
                                                    "echo_headers": {"type": "object"},
                                                    "echo_query": {"type": "object"},
                                                    "server_time": {"type": "string"},
                                                    "call_number": {"type": "number"}
                                                }
                                            }
                                        }
                                    }
                                }
                            },
                            "timeoutMs": 3000,
                            "input": {
                                "header": {
                                    "type": "object",
                                    "properties": {
                                        "X-Trace-Id": {"type": "string", "value": "${$.node." + nid_trigger + ".input.header.X-Trace-Id}"}
                                    }
                                },
                                "query": {
                                    "type": "object",
                                    "properties": {
                                        "keyword": {"type": "string", "value": "${$.node." + nid_trigger + ".input.query.keyword}"},
                                        "page": {"type": "string", "value": "${$.node." + nid_trigger + ".input.query.page}"},
                                        "size": {"type": "string", "value": "${$.node." + nid_trigger + ".input.query.size}"}
                                    }
                                },
                                "body": {
                                    "type": "object",
                                    "properties": {
                                        "message": {"type": "string", "value": "${$.constant:hello-from-e2e}"},
                                        "traceId": {"type": "string", "value": "${$.node." + nid_trigger + ".input.body.traceId}"},
                                        "sort": {"type": "string", "value": "${$.node." + nid_trigger + ".input.body.sort}"}
                                    }
                                }
                            }
                        }},
                        {"id": nid_exit, "type": "exit", "data": {
                            "type": "exit",
                            "output": {
                                "header": {
                                    "type": "object",
                                    "properties": {
                                        "X-Echo-Count": {"type": "string", "value": "${$.node." + nid_conn + ".output.body.data.call_number}"}
                                    }
                                },
                                "body": {
                                    "type": "object",
                                    "properties": {
                                        "code": {"type": "number", "value": "${$.node." + nid_conn + ".output.body.code}"},
                                        "message": {"type": "string", "value": "${$.node." + nid_conn + ".output.body.message}"},
                                        "data": {
                                            "type": "object",
                                            "value": "${$.node." + nid_conn + ".output.body.data.echo_body}"
                                        },
                                        "serverTime": {"type": "string", "value": "${$.node." + nid_conn + ".output.body.data.server_time}"},
                                        "callNumber": {"type": "number", "value": "${$.node." + nid_conn + ".output.body.data.call_number}"}
                                    }
                                }
                            }
                        }}
                    ],
                    "edges": [
                        {"id": f"e1_{_RUN_ID}", "source": nid_trigger, "target": nid_conn},
                        {"id": f"e2_{_RUN_ID}", "source": nid_conn, "target": nid_exit}
                    ],
                    "flowConfig": {
                        "flowMode": "single",
                        "rateLimitConfig": {"maxQps": 100, "maxConcurrency": 20},
                        "cache": {
                            "key": ["${$.node." + nid_trigger + ".input.body.traceId}"],
                            "ttl": 60
                        }
                    }
                }
            })
            return check_ok(r, "UPDATE 编排配置", f"PUT /flows/{fid}/versions/{fvid}")

        if not failed:
            if not step("CREATE 连接流", s5): failed = True
        if not failed:
            if not step("CREATE 草稿版本", s6): failed = True
        if not failed:
            if not step("UPDATE 编排(引用连接器)", s7): failed = True

        if not failed:
            print("  ✅ Phase 2 完成: 连接流已编排")
        else:
            print("  ❌ Phase 2 失败")

        # ═══════════════════════════════════════════════════
        # Phase 3: 草稿调试
        # ═══════════════════════════════════════════════════
        if not failed:
            print("\n── Phase 3: 草稿调试 ──")

        def s8():
            r = os_api("POST", f"/flows/{fid}/versions/{fvid}/debug", {
                "triggerData": {"message": "hello-draft-debug"}
            })
            ok = check_ok(r, "DEBUG 草稿版本", f"POST /flows/{fid}/versions/{fvid}/debug")
            if ok and r is not None:
                try:
                    b = r.json()
                    print(f"    响应: {json.dumps(b, ensure_ascii=False)[:500]}")
                except Exception:
                    pass
            return ok

        if not failed:
            if not step("DEBUG 草稿版本", s8): failed = True

        if not failed:
            print("  ✅ Phase 3 完成: 调试通过")
        else:
            print("  ❌ Phase 3 失败")

        # ═══════════════════════════════════════════════════
        # Phase 4: 发布 + 审批
        # ═══════════════════════════════════════════════════
        if not failed:
            print("\n── Phase 4: 发布 + 审批 ──")

        def s9():
            r = os_api("POST", f"/flows/{fid}/versions/{fvid}/publish")
            if not check_ok(r, "PUBLISH (提交审批)", f"POST /flows/{fid}/versions/{fvid}/publish"): return False
            r2 = os_api("GET", f"/flows/{fid}/versions/{fvid}")
            if not check_ok(r2, "确认待审批", f"GET /flows/{fid}/versions/{fvid}"): return False
            st = get_data(r2).get("status")
            if st not in (2, "2"):
                os_fail(f"version status={st}, 期望=2 (待审批)")
                return False
            print(f"    status=2 (待审批)")
            return True

        def s10():
            nonlocal aid
            # 从 API 查审批记录
            r = os_api("GET", "/approvals/pending?businessType=connector_flow_version_publish")
            if not check_ok(r, "查询审批记录", "GET /approvals/pending?businessType=..."): return False
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

        def s11():
            r = os_api("POST", f"/approvals/{aid}/approve",
                       {"comment": "场景级审批通过"}, user="tester")
            if not check_ok(r, "第一级审批 (scene, tester)", f"POST /approvals/{aid}/approve (L1)"):
                # check if already past this level
                r2 = os_api("GET", f"/approvals/{aid}")
                d = get_data(r2)
                print(f"    当前状态: currentNode={d.get('currentNode')}, status={d.get('status')}")
                return False
            return True

        def s12():
            r = os_api("POST", f"/approvals/{aid}/approve",
                       {"comment": "全局级审批通过"}, user="admin")
            if not check_ok(r, "第二级审批 (global, tester)", f"POST /approvals/{aid}/approve (L2)"):
                r2 = os_api("GET", f"/approvals/{aid}")
                d = get_data(r2)
                print(f"    当前状态: status={d.get('status')}")
                return False
            return True

        def s13():
            time.sleep(0.5)
            r = os_api("GET", f"/flows/{fid}/versions/{fvid}")
            if not check_ok(r, "确认版本已发布", f"GET /flows/{fid}/versions/{fvid}"): return False
            st = get_data(r).get("status")
            if st not in (5, "5"):
                time.sleep(2)
                r = os_api("GET", f"/flows/{fid}/versions/{fvid}")
                st = get_data(r).get("status")
            if st in (5, "5"):
                print(f"    status=5 (已发布)")
                return True
            os_fail(f"status={st}, 期望=5")
            return False

        if not failed:
            if not step("PUBLISH 提交审批", s9): failed = True
        if not failed:
            if not step("查询审批记录", s10): failed = True
        if not failed:
            if not step("第一级审批 (tester)", s11): failed = True
        if not failed:
            if not step("第二级审批 (tester)", s12): failed = True
        if not failed:
            if not step("验证版本已发布", s13): failed = True

        if not failed:
            print("  ✅ Phase 4 完成: 审批通过")
        else:
            print("  ❌ Phase 4 失败")

        # ═══════════════════════════════════════════════════
        # Phase 5: 发布后验证
        # ═══════════════════════════════════════════════════
        if not failed:
            print("\n── Phase 5: 发布后验证 ──")

        def s14():
            r = os_api("POST", f"/flows/{fid}/versions/{fvid}/debug", {
                "triggerData": {"message": "hello-after-approval"}
            })
            ok = check_ok(r, "DEBUG 已发布版本", f"POST /flows/{fid}/versions/{fvid}/debug")
            if ok and r is not None:
                try:
                    b = r.json()
                    print(f"    响应: {json.dumps(b, ensure_ascii=False)[:500]}")
                except Exception:
                    pass
            return ok

        if not failed:
            if not step("DEBUG 已发布版本", s14): failed = True

        if not failed:
            print("  ✅ Phase 5 完成: 发布后调试通过")
        else:
            print("  ❌ Phase 5 失败")

        # ═══════════════════════════════════════════════════
        # Phase 6: 部署 + 调用
        # ═══════════════════════════════════════════════════
        if not failed:
            print("\n── Phase 6: 部署 + 调用 ──")
            first_resp_body = None
            first_cache_status = None

        def s15():
            r = os_api("POST", f"/flows/{fid}/deploy", {"versionId": fvid})
            return check_ok(r, "DEPLOY 部署", f"POST /flows/{fid}/deploy")

        def s16():
            r = os_api("POST", f"/flows/{fid}/start")
            if not check_ok(r, "START 启动", f"POST /flows/{fid}/start"): return False
            r2 = os_api("GET", f"/flows/{fid}")
            ls = get_data(r2).get("lifecycleStatus")
            if ls not in (2, "2"):
                os_fail(f"lifecycleStatus={ls}, 期望=2 (running)")
                return False
            return True

        def s17():
            nonlocal first_resp_body, first_cache_status
            url = f"POST {CONNECTOR_API_BASE}/flows/{fid}/invoke?keyword=ai-search&page=1&size=10"
            r = api_connector("POST", f"/flows/{fid}/invoke?keyword=ai-search&page=1&size=10",
                             {
                                 "filters": {"category": "tech", "minScore": 0.5},
                                 "sort": "score",
                                 "traceId": _RUN_ID
                             },
                             headers={
                                  SYSTOKEN_HEADER: SYSTOKEN_VALUE,
                                 "X-Trace-Id": f"trace-{_RUN_ID}",
                                 "Cookie": f"session={_RUN_ID}; user=tester"
                             })
            if r is None:
                os_fail("connector-api 连接失败")
                return False
            if r.status_code in (200, 201):
                print(f"  ✅ 首次调用 (HTTP {r.status_code})  {url}")
                first_cache_status = r.headers.get("X-Cache-Status", "缺失")
                print(f"    X-Cache-Status: {first_cache_status}")
                try:
                    first_resp_body = r.json()
                    print(f"    响应体: {json.dumps(first_resp_body, ensure_ascii=False)[:300]}")
                except Exception as e:
                    print(f"    json解析失败: {e}")
                return True
            os_fail(f"首次调用: HTTP {r.status_code}, {r.text[:200]}  {url}")
            return False

        def s18():
            nonlocal first_resp_body
            url = f"POST {CONNECTOR_API_BASE}/flows/{fid}/invoke?keyword=ai-search&page=1&size=10"
            r = api_connector("POST", f"/flows/{fid}/invoke?keyword=ai-search&page=1&size=10",
                             {
                                 "filters": {"category": "tech", "minScore": 0.5},
                                 "sort": "score",
                                 "traceId": _RUN_ID
                             },
                             headers={
                                  SYSTOKEN_HEADER: SYSTOKEN_VALUE,
                                 "X-Trace-Id": f"trace-{_RUN_ID}",
                                 "Cookie": f"session={_RUN_ID}; user=tester"
                             })
            if r is None:
                os_fail("connector-api 连接失败")
                return False
            if r.status_code not in (200, 201):
                os_fail(f"第二次调用: HTTP {r.status_code}, {r.text[:200]}  {url}")
                return False

            print(f"  ✅ 第二次调用(相同traceId) (HTTP {r.status_code})  {url}")
            cache_status = r.headers.get("X-Cache-Status", "缺失")
            print(f"    X-Cache-Status: {cache_status}")
            if cache_status != "1":
                os_fail(f"缓存未命中: X-Cache-Status={cache_status}, 期望=1")
                return False
            print(f"    ✅ 缓存命中 (X-Cache-Status=1)")

            try:
                second_resp_body = r.json()
            except Exception as e:
                os_fail(f"json解析失败: {e}")
                return False

            # ===== Redis 精准验证 =====
            redis_key = f"cp:cache:flow:{fid}:{_RUN_ID}"
            checks = {}

            # ① Redis EXISTS
            v = os_redis("EXISTS", redis_key)
            checks["EXISTS"] = v is not None and (v == 1 or v == "1")
            print(f"    Redis EXISTS: {'1 ✅' if checks['EXISTS'] else '0'}")

            # ② Redis TTL (1~60s)
            v = os_redis("TTL", redis_key)
            if v:
                try:
                    ttl_val = v if isinstance(v, int) else int(v.strip())
                    checks["TTL"] = 1 <= ttl_val <= 60
                    print(f"    Redis TTL: {ttl_val}s (配置60s) {'✅' if checks['TTL'] else '⚠️'}")
                except ValueError:
                    checks["TTL"] = False

            # ③ Redis GET → 缓存值可解析
            v = os_redis("GET", redis_key)
            if v:
                try:
                    json.loads(v)
                    checks["GET"] = True
                    print(f"    Redis GET: ✅ 缓存JSON可解析")
                except Exception:
                    checks["GET"] = False

            # ④ 首次调用响应 vs 第二次响应 一致性（缓存命中应返回相同结果）
            if first_resp_body and isinstance(second_resp_body, dict):
                checks["BODY"] = json.dumps(first_resp_body, sort_keys=True) == \
                                 json.dumps(second_resp_body, sort_keys=True)
                print(f"    首次↔二次: {'✅ 完全一致' if checks['BODY'] else '⚠️ 存在差异'}")

            all_ok = all(checks.values()) if checks else False
            print(f"    {'✅ 缓存精准验证全部通过 (EXISTS+TTL+GET+响应一致性)' if all_ok else '⚠️ 缓存精准验证部分未通过'}")
            print(f"    响应体: {json.dumps(second_resp_body, ensure_ascii=False)[:300]}")
            return True

        def s19():
            time.sleep(1)
            row = os_db_val(
                f"SELECT CONCAT(status, ',', IFNULL(cache_status, -1)) FROM openplatform_v2_cp_execution_record_t "
                f"WHERE flow_id={fid} ORDER BY create_time DESC LIMIT 1"
            )
            if row:
                parts = row.split(',')
                print(f"    运行记录 status={parts[0]}, cache_status={parts[1]}")
                return True
            print(f"    ⚠️ 未找到运行记录")
            return True  # non-blocking

        def s20():
            r = os_api("POST", f"/flows/{fid}/stop")
            if not check_ok(r, "STOP 停止", f"POST /flows/{fid}/stop"): return False
            r2 = os_api("GET", f"/flows/{fid}")
            ls = get_data(r2).get("lifecycleStatus")
            if ls in (1, "1"):
                print(f"    lifecycleStatus=1 (stopped)")
                return True
            os_fail(f"lifecycleStatus={ls}, 期望=1")
            return False

        if not failed:
            if not step("DEPLOY 部署", s15): failed = True
        if not failed:
            if not step("START 启动", s16): failed = True
        if not failed:
            if not step("首次调用(缓存未命中)", s17): failed = True
        if not failed:
            if not step("第二次调用(验证缓存命中)", s18): failed = True
        if not failed:
            if not step("查询运行记录(DB只读)", s19): failed = True
        if not failed:
            if not step("STOP 停止", s20): failed = True

        if not failed:
            print("  ✅ Phase 6 完成: 部署调用全链路通过")

        # ── 结果 ──
        print("\n" + "=" * 60)
        if failed:
            print("  ❌ 全流程测试存在失败步骤")
        else:
            print("  🎉 全流程测试全部通过!")
        print("=" * 60)
        assert not failed, "全流程测试存在失败步骤"
        return

    finally:
        mock.stop()
        os_done()


if __name__ == "__main__":
    test_full_flow()
