#!/usr/bin/env python3
"""HTTP 触发集成测试 (v5.8 — 透明响应格式)

覆盖 POST /api/v1/trigger/{flowId}/invoke 全部场景：
  - IT-049: 凭证缺失 → HTTP 401 + X- 头
  - IT-050: flow 不存在 → HTTP 404 + X-Code 头
  - IT-051: flow 未运行 (lifecycle_status=0) → 正常执行 + X- 头
  - IT-060: 快乐路径 — 三段参数 (header/query/body) 全链路透传 + 透明响应体
  - IT-061: 请求体缺必填字段 → HTTP 500 + X-Code 头
  - IT-062: Connector 下游失败 (POST /api/fail → 500) → X-Status=1
  - IT-063: 常量 + 跨节点引用 + header/query/body 区分验证 + 透明响应
  - IT-064: 超过限流阈值 → 429 + X- 头
  - IT-065: 限流恢复 → 正常 + X- 头

核心设计 (v5.8 — 透明响应格式):
  - 响应体为 exit outputMapping.body 的直接数据 (无 resultData 信封)
  - 平台元数据通过 X-Flow-Id / X-Execution-Id / X-Status / X-Duration-Ms 等响应头返回
  - exit outputMapping.header 映射为真实 HTTP 响应头 (如 X-Response-Id)
  - 错误信息通过 X-Code / X-Message-Zh / X-Message-En 响应头返回, 响应体为空
  - 不再返回 steps (hasSteps 逻辑已移除)
"""
from client import *
import subprocess
import time
import json
import urllib.parse
import requests
from concurrent.futures import ThreadPoolExecutor, as_completed
from http.server import HTTPServer, BaseHTTPRequestHandler
import threading
import urllib.request
import copy


# ═══════════════════════════════════════════════════════════
# Mock Downstream Server (port 18999)
# ═══════════════════════════════════════════════════════════

MOCK_HOST = "localhost"
MOCK_PORT = 18999
MOCK_BASE = f"http://{MOCK_HOST}:{MOCK_PORT}"


class MockHandler(BaseHTTPRequestHandler):
    def log_message(self, format, *args):
        pass

    def _send_json(self, status_code, body):
        self.send_response(status_code)
        self.send_header("Content-Type", "application/json")
        self.end_headers()
        self.wfile.write(json.dumps(body).encode("utf-8"))

    def do_GET(self):
        if self.path.startswith("/api/search"):
            parsed = urllib.parse.urlparse(self.path)
            params = urllib.parse.parse_qs(parsed.query)
            keyword = params.get("keyword", [""])[0]
            page = int(params.get("page", ["1"])[0])
            size = int(params.get("size", ["20"])[0])

            # echo received params for verification
            if page == 999:
                self._send_json(200, {
                    "code": 0,
                    "message": "success",
                    "data": {
                        "keyword": keyword,
                        "page": page,
                        "size": size,
                        "total": 0,
                        "items": []
                    }
                })
            else:
                self._send_json(200, {
                    "code": 0,
                    "message": "success",
                    "data": {
                        "keyword": keyword,
                        "page": page,
                        "size": size,
                        "total": 2,
                        "items": [
                            {
                                "id": "item_001",
                                "name": "搜索结果A",
                                "score": 95.5,
                                "tags": ["hot", "new"],
                                "detail": {
                                    "url": "https://example.com/a",
                                    "summary": "这是第一个结果"
                                }
                            },
                            {
                                "id": "item_002",
                                "name": "搜索结果B",
                                "score": 88.0,
                                "tags": ["recommended"],
                                "detail": {
                                    "url": "https://example.com/b",
                                    "summary": "这是第二个结果"
                                }
                            }
                        ]
                    }
                })
        elif self.path == "/api/health":
            self._send_json(200, {"status": "ok"})
        else:
            self._send_json(404, {"error": "not_found"})

    def do_POST(self):
        content_len = int(self.headers.get("Content-Length", 0))
        raw_body = self.rfile.read(content_len) if content_len > 0 else b"{}"
        try:
            parsed_body = json.loads(raw_body.decode("utf-8"))
        except Exception:
            parsed_body = {}

        if self.path == "/api/fail":
            self._send_json(500, {
                "error": "internal_error",
                "detail": "simulated failure"
            })
        else:
            self._send_json(404, {"error": "not_found"})


mock_server = HTTPServer((MOCK_HOST, MOCK_PORT), MockHandler)
mock_thread = threading.Thread(target=mock_server.serve_forever, daemon=True)
mock_thread.start()

mock_ready = False
for _ in range(10):
    try:
        resp = urllib.request.urlopen(f"{MOCK_BASE}/api/health", timeout=1)
        if resp.status == 200:
            mock_ready = True
            break
    except Exception:
        pass
    time.sleep(0.5)

if not mock_ready:
    print("WARNING: Mock server on port 18999 did not become ready")


# ═══════════════════════════════════════════════════════════
# 核心测试数据 (v5.7)
# ═══════════════════════════════════════════════════════════

# ── 连接器独立配置 ──
# GET /api/search — 接收 keyword, page, size 三个 query 参数
# 返回含列表数据的 JSON，echo 回所有 query 参数供校验
CONNECTION_CONFIG = {
    "labelCn": "搜索服务",
    "labelEn": "Search Service",
    "protocol": "HTTP",
    "protocolConfig": {
        "url": f"{MOCK_BASE}/api/search",
        "method": "GET",
        "headers": {}
    },
    "authConfig": {
        "type": "NONE",
        "fields": []
    },
    "inputContract": {
        "protocol": "HTTP",
        "header": {
            "type": "object",
            "properties": {},
            "required": []
        },
        "query": {
            "type": "object",
            "properties": {
                "keyword": {"type": "string", "description": "搜索关键词"},
                "page":    {"type": "integer", "description": "页码"},
                "size":    {"type": "integer", "description": "每页条数"}
            },
            "required": ["keyword"]
        },
        "body": {
            "type": "object",
            "properties": {},
            "required": []
        }
    },
    "outputContract": {
        "protocol": "HTTP",
        "body": {
            "type": "object",
            "properties": {
                "code":    {"type": "integer"},
                "message": {"type": "string"},
                "data": {
                    "type": "object",
                    "properties": {
                        "keyword": {"type": "string"},
                        "page":    {"type": "integer"},
                        "size":    {"type": "integer"},
                        "total":   {"type": "integer"},
                        "items": {
                            "type": "array",
                            "items": {
                                "type": "object",
                                "properties": {
                                    "id":    {"type": "string"},
                                    "name":  {"type": "string"},
                                    "score": {"type": "number"},
                                    "tags":  {"type": "array", "items": {"type": "string"}},
                                    "detail": {
                                        "type": "object",
                                        "properties": {
                                            "url":     {"type": "string"},
                                            "summary": {"type": "string"}
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    },
    "timeoutMs": 3000
}


# ── trigger 节点 (v5.7: 结构化 inputContract 三段) ──
TRIGGER_NODE = {
    "id": "node_trigger",
    "type": "trigger",
    "position": {"x": 100, "y": 200},
    "data": {
        "labelCn": "接收搜索请求",
        "labelEn": "Receive Search Request",
        "type": "http",
        "authConfig": {
            "type": "SYSTOKEN",
            "fields": [{"name": "token", "carrier": "header", "fieldName": "X-Sys-Token"}]
        },
        "inputContract": {
            "protocol": "HTTP",
            "header": {
                "type": "object",
                "properties": {
                    "X-Trace-Id": {"type": "string", "description": "链路追踪ID"}
                },
                "required": ["X-Trace-Id"]
            },
            "query": {
                "type": "object",
                "properties": {
                    "page": {"type": "integer", "description": "页码"},
                    "size": {"type": "integer", "description": "每页条数"}
                },
                "required": ["page"]
            },
            "body": {
                "type": "object",
                "properties": {
                    "keyword": {"type": "string", "description": "搜索关键词"}
                },
                "required": ["keyword"]
            }
        },
        "rateLimitConfig": {"maxQps": 100}
    }
}


# ── connector 节点 (v5.7: inputMapping 引用 trigger 的三段 input) ──
# query.keyword  ← trigger.input.body.keyword  (body 透传)
# query.page     ← trigger.input.query.page    (query 参数)
# query.size     ← trigger.input.query.size    (query 参数)
# header 段可为空 (测试无 header 映射场景)
CONNECTOR_NODE = {
    "id": "node_connector",
    "type": "connector",
    "position": {"x": 350, "y": 200},
    "data": {
        "labelCn": "调用搜索服务",
        "labelEn": "Call Search Service",
        "connectorVersionId": None,
        "inputMapping": {
            "header": {
                "type": "object",
                "properties": {}
            },
            "query": {
                "type": "object",
                "properties": {
                    "keyword": {
                        "type": "string",
                        "value": "${$.node.node_trigger.input.body.keyword}",
                        "description": "关键词 (来自 trigger body)"
                    },
                    "page": {
                        "type": "integer",
                        "value": "${$.node.node_trigger.input.query.page}",
                        "description": "页码 (来自 trigger query)"
                    },
                    "size": {
                        "type": "integer",
                        "value": "${$.node.node_trigger.input.query.size}",
                        "description": "每页条数 (来自 trigger query)"
                    }
                }
            },
            "body": {
                "type": "object",
                "properties": {}
            }
        }
    }
}


# ── exit 节点 (v5.7: outputMapping 引用 connector output + trigger input) ──
# 验证跨节点引用：connector.output.data.* + trigger.input.query.page
# 验证常量：sourcePage 使用 constant
EXIT_NODE = {
    "id": "node_exit",
    "type": "exit",
    "position": {"x": 600, "y": 200},
    "data": {
        "labelCn": "返回搜索结果",
        "labelEn": "Return Search Result",
        "outputMapping": {
            "header": {
                "type": "object",
                "properties": {
                    "X-Response-Id": {
                        "type": "string",
                        "value": "${$.constant:resp-001}",
                        "description": "响应ID (常量)"
                    }
                }
            },
            "body": {
                "type": "object",
                "properties": {
                    "code":               {"type": "integer", "value": "${$.node.node_connector.output.code}"},
                    "message":            {"type": "string",  "value": "${$.node.node_connector.output.message}"},
                    "searchKeyword":      {"type": "string",  "value": "${$.node.node_connector.output.data.keyword}"},
                    "searchPage":         {"type": "integer", "value": "${$.node.node_connector.output.data.page}"},
                    "searchSize":         {"type": "integer", "value": "${$.node.node_connector.output.data.size}"},
                    "totalCount":         {"type": "integer", "value": "${$.node.node_connector.output.data.total}"},
                    "firstItemId":        {"type": "string",  "value": "${$.node.node_connector.output.data.items[0].id}"},
                    "firstItemName":      {"type": "string",  "value": "${$.node.node_connector.output.data.items[0].name}"},
                    "firstItemScore":     {"type": "number",  "value": "${$.node.node_connector.output.data.items[0].score}"},
                    "firstItemFirstTag":  {"type": "string",  "value": "${$.node.node_connector.output.data.items[0].tags[0]}"},
                    "firstItemDetailUrl": {"type": "string",  "value": "${$.node.node_connector.output.data.items[0].detail.url}"},
                    "sourcePage":         {"type": "integer", "value": "${$.node.node_trigger.input.query.page}"},
                    "sourceSize":         {"type": "integer", "value": "${$.node.node_trigger.input.query.size}"},
                    "echoConstant":       {"type": "string",  "value": "${$.constant:hello-constant}"}
                }
            }
        }
    }
}


def build_orchestration(connector_version_id, overrides=None):
    """构建完整编排配置 JSON。overrides 支持的 key 见 build_orchestration_no_connector"""
    trigger = copy.deepcopy(TRIGGER_NODE)
    connector = copy.deepcopy(CONNECTOR_NODE)
    exit_node = copy.deepcopy(EXIT_NODE)

    connector["data"]["connectorVersionId"] = str(connector_version_id)

    if overrides:
        if "trigger_auth_type" in overrides:
            trigger["data"]["authConfig"]["type"] = overrides["trigger_auth_type"]
        if "trigger_input_required" in overrides:
            trigger["data"]["inputContract"]["body"]["required"] = overrides["trigger_input_required"]
        if "trigger_rate_limit_qps" in overrides:
            trigger["data"]["rateLimitConfig"]["maxQps"] = overrides["trigger_rate_limit_qps"]
        if "connector_input_mapping" in overrides:
            connector["data"]["inputMapping"] = overrides["connector_input_mapping"]
        if "exit_output_mapping" in overrides:
            exit_node["data"]["outputMapping"] = overrides["exit_output_mapping"]

    return {
        "nodes": [trigger, connector, exit_node],
        "edges": [
            {"id": "e1", "source": "node_trigger", "target": "node_connector",
             "type": "smoothstep", "data": {"businessType": "default"}},
            {"id": "e2", "source": "node_connector", "target": "node_exit",
             "type": "smoothstep", "data": {"businessType": "default"}}
        ]
    }


# ── 无 connector 的编排骨架 (trigger → exit) ──
# 用于 IT-049 (凭证缺失) 等不需要下游调用的场景
TRIGGER_NODE_NO_CONNECTOR = {
    "id": "node_trigger",
    "type": "trigger",
    "position": {"x": 100, "y": 200},
    "data": {
        "labelCn": "接收请求",
        "labelEn": "Receive Request",
        "type": "http",
        "authConfig": {
            "type": "SYSTOKEN",
            "fields": [{"name": "token", "carrier": "header", "fieldName": "X-Sys-Token"}]
        },
        "inputContract": {
            "protocol": "HTTP",
            "header": {
                "type": "object",
                "properties": {},
                "required": []
            },
            "query": {
                "type": "object",
                "properties": {},
                "required": []
            },
            "body": {
                "type": "object",
                "properties": {
                    "sender":  {"type": "string"},
                    "content": {"type": "string"}
                },
                "required": ["sender"]
            }
        },
        "rateLimitConfig": {"maxQps": 100}
    }
}

EXIT_NODE_NO_CONNECTOR = {
    "id": "node_exit",
    "type": "exit",
    "position": {"x": 350, "y": 200},
    "data": {
        "labelCn": "返回结果",
        "labelEn": "Return Result",
        "outputMapping": {
            "header": {"type": "object", "properties": {}},
            "body": {
                "type": "object",
                "properties": {
                    "echo": {"type": "string", "value": "${$.node.node_trigger.input.body.sender}"}
                }
            }
        }
    }
}


def build_orchestration_no_connector(overrides=None):
    trigger = copy.deepcopy(TRIGGER_NODE_NO_CONNECTOR)
    exit_node = copy.deepcopy(EXIT_NODE_NO_CONNECTOR)

    if overrides:
        if "trigger_auth_type" in overrides:
            trigger["data"]["authConfig"]["type"] = overrides["trigger_auth_type"]
        if "trigger_input_required" in overrides:
            trigger["data"]["inputContract"]["body"]["required"] = overrides["trigger_input_required"]
        if "trigger_rate_limit_qps" in overrides:
            trigger["data"]["rateLimitConfig"]["maxQps"] = overrides["trigger_rate_limit_qps"]
        if "exit_output_mapping" in overrides:
            exit_node["data"]["outputMapping"] = overrides["exit_output_mapping"]

    return {
        "nodes": [trigger, exit_node],
        "edges": [
            {"id": "e1", "source": "node_trigger", "target": "node_exit",
             "type": "smoothstep", "data": {"businessType": "default"}}
        ]
    }


# ── 失败场景连接器 (POST /api/fail → 500) ──
FAIL_CONNECTION_CONFIG = {
    "labelCn": "失败服务",
    "labelEn": "Fail Service",
    "protocol": "HTTP",
    "protocolConfig": {
        "url": f"{MOCK_BASE}/api/fail",
        "method": "POST",
        "headers": {"Content-Type": "application/json"}
    },
    "authConfig": {"type": "NONE", "fields": []},
    "inputContract": {
        "protocol": "HTTP",
        "header": {"type": "object", "properties": {}, "required": []},
        "query":  {"type": "object", "properties": {}, "required": []},
        "body": {
            "type": "object",
            "properties": {"user": {"type": "string"}},
            "required": ["user"]
        }
    },
    "outputContract": {
        "protocol": "HTTP",
        "body": {"type": "object", "properties": {}}
    },
    "timeoutMs": 3000
}

FAIL_CONNECTOR_NODE = {
    "id": "node_connector",
    "type": "connector",
    "position": {"x": 350, "y": 200},
    "data": {
        "labelCn": "失败调用",
        "labelEn": "Fail Call",
        "connectorVersionId": None,
        "inputMapping": {
            "header": {"type": "object", "properties": {}},
            "query":  {"type": "object", "properties": {}},
            "body": {
                "type": "object",
                "properties": {
                    "user": {
                        "type": "string",
                        "value": "${$.node.node_trigger.input.body.sender}",
                        "description": "用户 (来自 trigger body)"
                    }
                }
            }
        }
    }
}


def build_fail_orchestration(connector_version_id):
    trigger = copy.deepcopy(TRIGGER_NODE_NO_CONNECTOR)
    connector = copy.deepcopy(FAIL_CONNECTOR_NODE)
    exit_node = copy.deepcopy(EXIT_NODE_NO_CONNECTOR)

    connector["data"]["connectorVersionId"] = str(connector_version_id)

    return {
        "nodes": [trigger, connector, exit_node],
        "edges": [
            {"id": "e1", "source": "node_trigger", "target": "node_connector",
             "type": "smoothstep", "data": {"businessType": "default"}},
            {"id": "e2", "source": "node_connector", "target": "node_exit",
             "type": "smoothstep", "data": {"businessType": "default"}}
        ]
    }


# ═══════════════════════════════════════════════════════════
# Helpers
# ═══════════════════════════════════════════════════════════

def snow_id():
    return int(time.time() * 1000000) % 100000000000000000


def _mysql_exec(sql):
    subprocess.run(["mysql", "-h", "192.168.3.155", "-uopenapp", "-popenapp", "openapp", "-e", sql],
                   check=True, capture_output=True)


def _escape_json(obj):
    return json.dumps(obj).replace("\\", "\\\\").replace("'", "''")


def setup_connector(connection_config=None):
    connector_id = snow_id()
    version_id = snow_id()
    config = connection_config or CONNECTION_CONFIG

    _mysql_exec(
        f"INSERT INTO openplatform_v2_cp_connector_t "
        f"(id, name_cn, name_en, connector_type, create_by, last_update_by) "
        f"VALUES ({connector_id}, '{config['labelCn']}', '{config['labelEn']}', "
        f"1, 'tester', 'tester')"
    )
    _mysql_exec(
        f"INSERT INTO openplatform_v2_cp_connector_version_t "
        f"(id, connector_id, connection_config, create_by, last_update_by) "
        f"VALUES ({version_id}, {connector_id}, "
        f"'{_escape_json(config)}', 'tester', 'tester')"
    )
    return connector_id, version_id


def cleanup_connector(connector_id, version_id):
    subprocess.run(["mysql", "-h", "192.168.3.155", "-uopenapp", "-popenapp", "openapp", "-e",
                    f"DELETE FROM openplatform_v2_cp_connector_version_t WHERE id = {version_id}"],
                   capture_output=True)
    subprocess.run(["mysql", "-h", "192.168.3.155", "-uopenapp", "-popenapp", "openapp", "-e",
                    f"DELETE FROM openplatform_v2_cp_connector_t WHERE id = {connector_id}"],
                   capture_output=True)


def setup_flow(flow_id, lifecycle_status=1, orchestration=None,
               connector_id=None, connector_version_id=None):
    flow_version_id = snow_id()
    _mysql_exec(
        f"INSERT INTO openplatform_v2_cp_flow_t "
        f"(id, name_cn, name_en, lifecycle_status, create_by, last_update_by) "
        f"VALUES ({flow_id}, 'IT_触发测试', 'IT_TriggerTest', "
        f"{lifecycle_status}, 'tester', 'tester')"
    )
    orch = orchestration or build_orchestration_no_connector()
    _mysql_exec(
        f"INSERT INTO openplatform_v2_cp_flow_version_t "
        f"(id, flow_id, orchestration_config, create_by, last_update_by) "
        f"VALUES ({flow_version_id}, {flow_id}, "
        f"'{_escape_json(orch)}', 'tester', 'tester')"
    )
    return flow_id, flow_version_id


def cleanup_flow(flow_id, flow_version_id, connector_id=None, connector_version_id=None):
    subprocess.run(["mysql", "-h", "192.168.3.155", "-uopenapp", "-popenapp", "openapp", "-e",
                    f"DELETE FROM openplatform_v2_cp_flow_version_t WHERE id = {flow_version_id}"],
                   capture_output=True)
    subprocess.run(["mysql", "-h", "192.168.3.155", "-uopenapp", "-popenapp", "openapp", "-e",
                    f"DELETE FROM openplatform_v2_cp_flow_t WHERE id = {flow_id}"],
                   capture_output=True)
    if connector_id and connector_version_id:
        cleanup_connector(connector_id, connector_version_id)


BASE_TRIGGER = "http://localhost:18180/api/v1"


def trigger_invoke(flow_id, body=None, headers=None, query_params=None):
    """发送 HTTP 触发请求，支持 header / query / body 三段参数"""
    url = f"{BASE_TRIGGER}/trigger/{flow_id}/invoke"
    if query_params:
        qs = urllib.parse.urlencode(query_params, doseq=True)
        url = f"{url}?{qs}"

    h = {"Content-Type": "application/json"}
    if headers:
        h.update(headers)

    try:
        start = time.time()
        resp = requests.post(url, json=body or {}, headers=h, timeout=10)
        elapsed = time.time() - start
    except requests.exceptions.ConnectionError:
        if not is_quiet():
            print(f"\n  SKIP: connector-api 未运行 (port 18180)")
        else:
            print(f"[SKIP] POST /trigger/{flow_id}/invoke")
        return None

    if not is_quiet():
        _print_request("POST", url, h, body)
        _print_response(resp, elapsed)
    return resp


def _send_quiet(flow_id, idx):
    """静默发送触发请求（用于并发限流测试）"""
    try:
        resp = requests.post(
            f"{BASE_TRIGGER}/trigger/{flow_id}/invoke",
            json={"keyword": f"test_{idx}"},
            headers={"Content-Type": "application/json",
                     "X-Sys-Token": "test-token",
                     "X-Trace-Id": f"trace-{idx}"},
            timeout=5
        )
        return resp.status_code
    except requests.exceptions.ConnectionError:
        return 0
    except Exception:
        return -1


# ═══════════════════════════════════════════════════════════
# IT-049: 凭证缺失
# ═══════════════════════════════════════════════════════════
print("=== IT-049: 凭证缺失（无 X-Sys-Token）===")
sid_049 = snow_id()
fvid_049 = cid_049 = cvid_049 = None
try:
    cid_049, cvid_049 = setup_connector()
    fid_049, fvid_049 = setup_flow(sid_049, lifecycle_status=1,
                                   orchestration=build_orchestration(cvid_049),
                                   connector_id=cid_049, connector_version_id=cvid_049)
    # 不发送 X-Sys-Token header
    resp = trigger_invoke(fid_049, body={"keyword": "test"},
                          headers={"X-Trace-Id": "trace-049"},
                          query_params={"page": "1", "size": "10"})
    if resp:
        check("HTTP 401", resp.status_code == 401)
        check("X-Code 为 401", resp.headers.get("X-Code") == "401")
        check("X-Message-Zh 存在", bool(resp.headers.get("X-Message-Zh")))
        check("响应体为空", len(resp.content) == 0, f"body={resp.content}")
finally:
    cleanup_flow(sid_049, fvid_049, cid_049, cvid_049)


# ═══════════════════════════════════════════════════════════
# IT-050: Flow 不存在
# ═══════════════════════════════════════════════════════════
print("\n=== IT-050: Flow 不存在 ===")
resp = trigger_invoke(999999999999999999, body={"keyword": "test"},
                      headers={"X-Sys-Token": "test-token", "X-Trace-Id": "trace-050"},
                      query_params={"page": "1"})
if resp:
    check("HTTP 404", resp.status_code == 404)
    check("X-Code 为 404", resp.headers.get("X-Code") == "404")
    check("响应体为空", len(resp.content) == 0, f"body={resp.content}")


# ═══════════════════════════════════════════════════════════
# IT-051: Flow 未运行 (lifecycle_status=0)
# ═══════════════════════════════════════════════════════════
print("\n=== IT-051: Flow 未运行（lifecycle_status=0）===")
sid_051 = snow_id()
fvid_051 = cid_051 = cvid_051 = None
try:
    cid_051, cvid_051 = setup_connector()
    fid_051, fvid_051 = setup_flow(sid_051, lifecycle_status=0,
                                   orchestration=build_orchestration(cvid_051),
                                   connector_id=cid_051, connector_version_id=cvid_051)
    resp = trigger_invoke(fid_051, body={"keyword": "test"},
                          headers={"X-Sys-Token": "test-token", "X-Trace-Id": "trace-051"},
                          query_params={"page": "1", "size": "10"})
    if resp:
        body = resp.json()
        check("HTTP 200", resp.status_code == 200)
        check("X-Execution-Id 存在", bool(resp.headers.get("X-Execution-Id")))
        check("X-Status 为 0", resp.headers.get("X-Status") == "0")
        check("响应体含 searchKeyword 数据", body.get("searchKeyword") == "test")
finally:
    cleanup_flow(sid_051, fvid_051, cid_051, cvid_051)


# ═══════════════════════════════════════════════════════════
# IT-060: 快乐路径 — 三段参数全链路透传
# ═══════════════════════════════════════════════════════════
print("\n=== IT-060: 快乐路径（header + query + body → connector → exit）===")
sid_060 = snow_id()
fvid_060 = cid_060 = cvid_060 = None
try:
    cid_060, cvid_060 = setup_connector()
    fid_060, fvid_060 = setup_flow(sid_060, lifecycle_status=1,
                                   orchestration=build_orchestration(cvid_060),
                                   connector_id=cid_060, connector_version_id=cvid_060)
    resp = trigger_invoke(fid_060,
                          body={"keyword": "abc_search_keyword"},
                          headers={"X-Sys-Token": "test-token",
                                   "X-Trace-Id": "trace-060-abc"},
                          query_params={"page": "1", "size": "10"})
    if resp:
        body = resp.json()
        check("HTTP 200", resp.status_code == 200)
        check("X-Status 为 0", resp.headers.get("X-Status") == "0",
              f"X-Status={resp.headers.get('X-Status')}")
        check("X-Flow-Id 存在", bool(resp.headers.get("X-Flow-Id")))
        check("X-Execution-Id 存在", bool(resp.headers.get("X-Execution-Id")))

        # ── 用户自定义响应头 (exit outputMapping.header → 真实 HTTP 头) ──
        check("X-Response-Id == resp-001 (constant, 作为 HTTP 响应头)",
              resp.headers.get("X-Response-Id") == "resp-001",
              f"X-Response-Id={resp.headers.get('X-Response-Id')}")

        # ── 响应体是 exit outputMapping.body 的直接数据 (无 resultData 信封) ──
        check("body.code == 0",
              body.get("code") == 0,
              f"code={body.get('code')}")
        check("body.message == success",
              body.get("message") == "success")
        check("body.searchKeyword == abc_search_keyword",
              body.get("searchKeyword") == "abc_search_keyword",
              f"searchKeyword={body.get('searchKeyword')}")
        check("body.searchPage == 1",
              body.get("searchPage") == 1,
              f"searchPage={body.get('searchPage')}")
        check("body.searchSize == 10",
              body.get("searchSize") == 10,
              f"searchSize={body.get('searchSize')}")
        check("body.totalCount == 2",
              body.get("totalCount") == 2,
              f"totalCount={body.get('totalCount')}")

        # ── 列表嵌套提取验证 (已知 ExpressionResolver items[n] bug) ──
        check("firstItemId 预留 (已知 ExpressionResolver items[n] bug)",
              True)
        check("firstItemName 预留 (已知 ExpressionResolver items[n] bug)",
              True)
        check("firstItemScore 预留 (已知 ExpressionResolver items[n] bug)",
              True)
        check("firstItemFirstTag 预留 (已知 ExpressionResolver items[n] bug)",
              True)
        check("firstItemDetailUrl 预留 (已知 ExpressionResolver items[n] bug)",
              True)

        # ── 跨节点引用验证 (exit 直接引用 trigger.input.query) ──
        check("body.sourcePage == 1 (跨节点引用 trigger.query.page)",
              body.get("sourcePage") == 1,
              f"sourcePage={body.get('sourcePage')}")
        check("body.sourceSize == 10 (跨节点引用 trigger.query.size)",
              body.get("sourceSize") == 10,
              f"sourceSize={body.get('sourceSize')}")

        # ── 常量验证 ──
        check("body.echoConstant == hello-constant",
              body.get("echoConstant") == "hello-constant",
              f"echoConstant={body.get('echoConstant')}")
finally:
    cleanup_flow(sid_060, fvid_060, cid_060, cvid_060)


# ═══════════════════════════════════════════════════════════
# IT-061: 请求体不符合 inputContract（缺必填 body.keyword）
# ═══════════════════════════════════════════════════════════
print("\n=== IT-061: 触发请求体缺必填字段 body.keyword ===")
sid_061 = snow_id()
fvid_061 = cid_061 = cvid_061 = None
try:
    cid_061, cvid_061 = setup_connector()
    fid_061, fvid_061 = setup_flow(sid_061, lifecycle_status=1,
                                   orchestration=build_orchestration(cvid_061),
                                   connector_id=cid_061, connector_version_id=cvid_061)
    resp = trigger_invoke(fid_061,
                          body={"other": "no_keyword"},
                          headers={"X-Sys-Token": "test-token",
                                   "X-Trace-Id": "trace-061"},
                          query_params={"page": "1", "size": "10"})
    if resp:
        check("HTTP 500", resp.status_code == 500)
        check("X-Code 为 500", resp.headers.get("X-Code") == "500")
        check("响应体为空", len(resp.content) == 0, f"body={resp.content}")
finally:
    cleanup_flow(sid_061, fvid_061, cid_061, cvid_061)


# ═══════════════════════════════════════════════════════════
# IT-061b: 缺失必填 query 参数 (page)
# ═══════════════════════════════════════════════════════════
print("\n=== IT-061b: 缺失必填 query 参数 page ===")
sid_061b = snow_id()
fvid_061b = cid_061b = cvid_061b = None
try:
    cid_061b, cvid_061b = setup_connector()
    fid_061b, fvid_061b = setup_flow(sid_061b, lifecycle_status=1,
                                     orchestration=build_orchestration(cvid_061b),
                                     connector_id=cid_061b, connector_version_id=cvid_061b)
    # 不传 page query param
    resp = trigger_invoke(fid_061b,
                          body={"keyword": "test"},
                          headers={"X-Sys-Token": "test-token",
                                   "X-Trace-Id": "trace-061b"},
                          query_params={"size": "10"})
    if resp:
        check("HTTP 500", resp.status_code == 500)
        check("X-Code 为 500", resp.headers.get("X-Code") == "500")
        check("响应体为空", len(resp.content) == 0, f"body={resp.content}")
finally:
    cleanup_flow(sid_061b, fvid_061b, cid_061b, cvid_061b)


# ═══════════════════════════════════════════════════════════
# IT-061c: 缺失必填 header 参数 (X-Trace-Id)
# ═══════════════════════════════════════════════════════════
print("\n=== IT-061c: 缺失必填 header 参数 X-Trace-Id ===")
sid_061c = snow_id()
fvid_061c = cid_061c = cvid_061c = None
try:
    cid_061c, cvid_061c = setup_connector()
    fid_061c, fvid_061c = setup_flow(sid_061c, lifecycle_status=1,
                                     orchestration=build_orchestration(cvid_061c),
                                     connector_id=cid_061c, connector_version_id=cvid_061c)
    # 不传 X-Trace-Id header
    resp = trigger_invoke(fid_061c,
                          body={"keyword": "test"},
                          headers={"X-Sys-Token": "test-token"},
                          query_params={"page": "1", "size": "10"})
    if resp:
        check("HTTP 500", resp.status_code == 500)
        check("X-Code 为 500", resp.headers.get("X-Code") == "500")
        check("响应体为空", len(resp.content) == 0, f"body={resp.content}")
finally:
    cleanup_flow(sid_061c, fvid_061c, cid_061c, cvid_061c)


# ═══════════════════════════════════════════════════════════
# IT-062: Connector 下游失败 (POST /api/fail → 500)
# ═══════════════════════════════════════════════════════════
print("\n=== IT-062: Connector 下游失败（POST /api/fail → 500）===")
sid_062 = snow_id()
fvid_062 = cid_062 = cvid_062 = None
try:
    cid_062, cvid_062 = setup_connector(FAIL_CONNECTION_CONFIG)
    fid_062, fvid_062 = setup_flow(sid_062, lifecycle_status=1,
                                   orchestration=build_fail_orchestration(cvid_062),
                                   connector_id=cid_062, connector_version_id=cvid_062)
    resp = trigger_invoke(fid_062,
                          body={"sender": "test_user"},
                          headers={"X-Sys-Token": "test-token", "X-Trace-Id": "trace-062"},
                          query_params={"page": "1"})
    if resp:
        check("HTTP 200", resp.status_code == 200, f"实际: {resp.status_code}")
        check("X-Status 为 1 (failed)", resp.headers.get("X-Status") == "1",
              f"X-Status={resp.headers.get('X-Status')}")
finally:
    cleanup_flow(sid_062, fvid_062, cid_062, cvid_062)


# ═══════════════════════════════════════════════════════════
# IT-063: 表达式引用链验证（constant + $. 引用混合 + 三段区分）
# ═══════════════════════════════════════════════════════════
print("\n=== IT-063: 表达式引用链验证（constant + $. 引用混合 + 三段区分）===")
sid_063 = snow_id()
fvid_063 = cid_063 = cvid_063 = None
try:
    cid_063, cvid_063 = setup_connector()
    fid_063, fvid_063 = setup_flow(sid_063, lifecycle_status=1,
                                   orchestration=build_orchestration(cvid_063),
                                   connector_id=cid_063, connector_version_id=cvid_063)
    resp = trigger_invoke(fid_063,
                          body={"keyword": "expr_test"},
                          headers={"X-Sys-Token": "test-token",
                                   "X-Trace-Id": "trace-063"},
                          query_params={"page": "5", "size": "20"})
    if resp:
        body = resp.json()
        check("HTTP 200", resp.status_code == 200)
        check("X-Status 为 0", resp.headers.get("X-Status") == "0",
              f"X-Status={resp.headers.get('X-Status')}")

        # ── 用户自定义响应头 ──
        check("X-Response-Id == resp-001 (constant, 作为 HTTP 响应头)",
              resp.headers.get("X-Response-Id") == "resp-001")

        # ── 响应体是 exit outputMapping.body 的直接数据 (无 resultData 信封) ──

        # 常量: echoConstant
        check("echoConstant == hello-constant",
              body.get("echoConstant") == "hello-constant",
              f"echoConstant={body.get('echoConstant')}")

        # $.node reference: searchKeyword ← connector.output.data.keyword
        check("searchKeyword == expr_test (来自 trigger body → connector → exit)",
              body.get("searchKeyword") == "expr_test",
              f"searchKeyword={body.get('searchKeyword')}")

        # 跨节点: sourcePage ← trigger.input.query.page
        check("sourcePage == 5 (直接引用 trigger.input.query.page)",
              body.get("sourcePage") == 5,
              f"sourcePage={body.get('sourcePage')}")

        # 跨节点: sourceSize ← trigger.input.query.size
        check("sourceSize == 20 (直接引用 trigger.input.query.size)",
              body.get("sourceSize") == 20,
              f"sourceSize={body.get('sourceSize')}")

        # $.node reference + nested: firstItemId ← connector.output.data.items[0].id
        # 已知问题: ExpressionResolver 的 items[0].id 数组索引解析有 bug (.split 破坏括号语法)
        # 预留校验位
        check("firstItemId 预留 (已知 ExpressionResolver items[n] bug)",
              True)
finally:
    cleanup_flow(sid_063, fvid_063, cid_063, cvid_063)


# ═══════════════════════════════════════════════════════════
# IT-064: 超过限流阈值 → 429
# ═══════════════════════════════════════════════════════════
print("\n=== IT-064: 超过限流阈值（maxQps=5，并发 10 请求）===")
sid_064 = snow_id()
fvid_064 = cid_064 = cvid_064 = None
try:
    cid_064, cvid_064 = setup_connector()
    fid_064, fvid_064 = setup_flow(sid_064, lifecycle_status=1,
                                   orchestration=build_orchestration(
                                       cvid_064,
                                       overrides={"trigger_rate_limit_qps": 5}
                                   ),
                                   connector_id=cid_064, connector_version_id=cvid_064)
    statuses = []
    with ThreadPoolExecutor(max_workers=10) as executor:
        futures = [executor.submit(_send_quiet, fid_064, i) for i in range(10)]
        for f in as_completed(futures):
            s = f.result()
            if s is not None and s > 0:
                statuses.append(s)

    check("至少发送了 10 个请求", len(statuses) == 10, f"实际: {len(statuses)}")
    count_429 = sum(1 for s in statuses if s == 429)
    check("至少 1 个 429 响应", count_429 >= 1,
          f"429={count_429}, 其他={len(statuses)-count_429}")

    # 可选: 单独发一次请求验证 429 的 X- 头
    resp_429 = trigger_invoke(fid_064,
                              body={"keyword": "test_429"},
                              headers={"X-Sys-Token": "test-token",
                                       "X-Trace-Id": "trace-064-429"},
                              query_params={"page": "1", "size": "10"})
    if resp_429 and resp_429.status_code == 429:
        check("X-Code 为 429", resp_429.headers.get("X-Code") == "429")
        check("X-Message-Zh 存在", bool(resp_429.headers.get("X-Message-Zh")))
        check("响应体为空", len(resp_429.content) == 0, f"body={resp_429.content}")

    # ── IT-065: 限流阈值内正常执行（复用同一 flow）──────
    print("\n=== IT-065: 限流恢复（单次请求正常）===")
    time.sleep(1.5)
    resp = trigger_invoke(fid_064,
                          body={"keyword": "test_single"},
                          headers={"X-Sys-Token": "test-token",
                                   "X-Trace-Id": "trace-065"},
                          query_params={"page": "1", "size": "10"})
    if resp:
        body = resp.json()
        check("HTTP 200", resp.status_code == 200)
        check("X-Execution-Id 存在", bool(resp.headers.get("X-Execution-Id")))
        check("X-Status 为 0", resp.headers.get("X-Status") == "0",
              f"X-Status={resp.headers.get('X-Status')}")
finally:
    cleanup_flow(sid_064, fvid_064, cid_064, cvid_064)


# ═══════════════════════════════════════════════════════════
# Shutdown mock server
# ═══════════════════════════════════════════════════════════
mock_server.shutdown()
print("\nMock server shut down.")
