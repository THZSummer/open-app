#!/usr/bin/env python3
"""HTTP 触发 (IT-049~051, IT-060~065)

覆盖 POST /api/v1/trigger/{flowId}/invoke 全部场景：
  - IT-049: 凭证缺失 → errorInfo
  - IT-050: flow 不存在 → errorInfo.cause 含 "Flow not found"
  - IT-051: flow 未运行 → 可正常执行
  - IT-060: 快乐路径（trigger→connector→exit）→ GET + 2 query参数 + 列表数据 + 嵌套提取
  - IT-061: 请求体不符合 inputContract → errorInfo
  - IT-062: connector 下游失败 (500) → status=failed
  - IT-063: 表达式引用链验证（constant + $. 引用混合）
  - IT-064: 超过限流阈值 → 429
  - IT-065: 限流阈值内正常 → 200

核心设计：
  - 连接器独立配置 (connector_version_t.connection_config): CONNECTION_CONFIG
  - 连接流编排配置 (flow_version_t.orchestration_config): 由 build_orchestration() 构建
  - connector 节点 data 只含 connectorVersionId + inputMapping，不含 url/method
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

            if page == 999:
                self._send_json(200, {
                    "code": 0,
                    "message": "success",
                    "data": {
                        "keyword": keyword,
                        "page": page,
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
# 核心测试数据
# ═══════════════════════════════════════════════════════════

# ── 连接器独立配置 (存入 connector_version_t.connection_config) ──
# 描述: GET 搜索服务，2 个 query 参数，返回含列表数据的 JSON
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
        "header": {"type": "object", "properties": {}, "required": []},
        "query": {
            "type": "object",
            "properties": {
                "keyword": {"type": "string", "description": "搜索关键词"},
                "page":    {"type": "integer", "description": "页码"}
            },
            "required": ["keyword"]
        },
        "body": {"type": "object", "properties": {}, "required": []}
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


# ── 连接流编排配置骨架 (存入 flow_version_t.orchestration_config) ──
# trigger → connector(GET 搜索服务) → exit
# 构建函数: build_orchestration(connector_version_id, overrides=None)

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
            "header": {"type": "object", "properties": {}, "required": []},
            "query":  {"type": "object", "properties": {}, "required": []},
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

CONNECTOR_NODE = {
    "id": "node_connector",
    "type": "connector",
    "position": {"x": 350, "y": 200},
    "data": {
        "labelCn": "调用搜索服务",
        "labelEn": "Call Search Service",
        "connectorVersionId": None,  # 运行时替换
        "inputMapping": {
            "header": {"type": "object", "properties": {}},
            "query": {
                "type": "object",
                "properties": {
                    "keyword": {"type": "string", "value": "${$.node.node_trigger.input.keyword}"},
                    "page":    {"type": "integer", "value": "${$.constant:1}"}
                }
            },
            "body": {"type": "object", "properties": {}}
        }
    }
}

EXIT_NODE = {
    "id": "node_exit",
    "type": "exit",
    "position": {"x": 600, "y": 200},
    "data": {
        "labelCn": "返回搜索结果",
        "labelEn": "Return Search Result",
        "outputMapping": {
            "body": {
                "type": "object",
                "properties": {
                    "code":               {"type": "integer", "value": "${$.node.node_connector.output.code}"},
                    "message":            {"type": "string",  "value": "${$.node.node_connector.output.message}"},
                    "searchKeyword":      {"type": "string",  "value": "${$.node.node_connector.output.data.keyword}"},
                    "totalCount":         {"type": "integer", "value": "${$.node.node_connector.output.data.total}"},
                    "firstItemId":        {"type": "string",  "value": "${$.node.node_connector.output.data.items[0].id}"},
                    "firstItemName":      {"type": "string",  "value": "${$.node.node_connector.output.data.items[0].name}"},
                    "firstItemScore":     {"type": "number",  "value": "${$.node.node_connector.output.data.items[0].score}"},
                    "firstItemFirstTag":  {"type": "string",  "value": "${$.node.node_connector.output.data.items[0].tags[0]}"},
                    "firstItemDetailUrl": {"type": "string",  "value": "${$.node.node_connector.output.data.items[0].detail.url}"}
                }
            }
        }
    }
}


def build_orchestration(connector_version_id, overrides=None):
    """构建完整编排配置 JSON。overrides 可为 None 或 dict，用于覆盖特定字段。
    
    overrides 支持的 key:
      - trigger_auth_type:         覆盖 trigger data.authConfig.type
      - trigger_input_required:    覆盖 trigger data.inputContract.body.required
      - trigger_rate_limit_qps:    覆盖 trigger data.rateLimitConfig.maxQps
      - connector_input_mapping:   覆盖 connector data.inputMapping
      - exit_output_mapping:       覆盖 exit data.outputMapping
    """
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


# ── 无 connector 的编排配置骨架 (trigger → exit) ──

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
            "header": {"type": "object", "properties": {}, "required": []},
            "query":  {"type": "object", "properties": {}, "required": []},
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
            "body": {
                "type": "object",
                "properties": {
                    "echo": {"type": "string", "value": "${$.node.node_trigger.input.sender}"}
                }
            }
        }
    }
}


def build_orchestration_no_connector(overrides=None):
    """构建无 connector 的编排配置 (trigger → exit)"""
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


# ═══════════════════════════════════════════════════════════
# 失败场景连接器配置 (POST /api/fail, 用于 IT-062)
# ═══════════════════════════════════════════════════════════

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
                    "user": {"type": "string", "value": "${$.node.node_trigger.input.sender}"}
                }
            }
        }
    }
}


def build_fail_orchestration(connector_version_id):
    """构建失败场景编排 (trigger → connector /api/fail → exit)"""
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
    """执行 MySQL 语句"""
    subprocess.run(["mysql", "-uopenapp", "-popenapp", "openapp", "-e", sql],
                   check=True, capture_output=True)


def _escape_json(obj):
    """将 Python dict 转为 MySQL 安全的 JSON 字符串"""
    return json.dumps(obj).replace("'", "''")


def setup_connector(connection_config=None):
    """插入 connector_t + connector_version_t，返回 (connector_id, connector_version_id)"""
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
    """清理 connector_version_t + connector_t"""
    subprocess.run(["mysql", "-uopenapp", "-popenapp", "openapp", "-e",
                    f"DELETE FROM openplatform_v2_cp_connector_version_t WHERE id = {version_id}"],
                   capture_output=True)
    subprocess.run(["mysql", "-uopenapp", "-popenapp", "openapp", "-e",
                    f"DELETE FROM openplatform_v2_cp_connector_t WHERE id = {connector_id}"],
                   capture_output=True)


def setup_flow(flow_id, lifecycle_status=1, orchestration=None,
               connector_id=None, connector_version_id=None):
    """插入 flow_t + flow_version_t。返回 (flow_id, flow_version_id)"""
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
    """清理 flow + connector 数据"""
    subprocess.run(["mysql", "-uopenapp", "-popenapp", "openapp", "-e",
                    f"DELETE FROM openplatform_v2_cp_flow_version_t WHERE id = {flow_version_id}"],
                   capture_output=True)
    subprocess.run(["mysql", "-uopenapp", "-popenapp", "openapp", "-e",
                    f"DELETE FROM openplatform_v2_cp_flow_t WHERE id = {flow_id}"],
                   capture_output=True)
    if connector_id and connector_version_id:
        cleanup_connector(connector_id, connector_version_id)


BASE_TRIGGER = "http://localhost:18180/api/v1"


def _send_quiet(flow_id, idx):
    """静默发送触发请求（用于并发限流测试）"""
    try:
        resp = requests.post(
            f"{BASE_TRIGGER}/trigger/{flow_id}/invoke",
            json={"keyword": f"test_{idx}"},
            headers={"Content-Type": "application/json", "X-Sys-Token": "test-token"},
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
    resp = request("POST", f"/trigger/{fid_049}/invoke", {"keyword": "test"})
    if resp:
        body = resp.json()
        check("HTTP 200", resp.status_code == 200)
        check("status 为 failed", body.get("status") == "failed")
        check("errorInfo 存在", "errorInfo" in body)
        ei = body.get("errorInfo", {})
        check("errorInfo.code 存在", bool(ei.get("code")))
        cause = (ei.get("cause") or "").lower()
        check("errorInfo.cause 含 Missing X-Sys-Token",
              "missing x-sys-token" in cause or "x-sys-token" in cause)
finally:
    cleanup_flow(sid_049, fvid_049, cid_049, cvid_049)


# ═══════════════════════════════════════════════════════════
# IT-050: Flow 不存在
# ═══════════════════════════════════════════════════════════
print("\n=== IT-050: Flow 不存在 ===")
resp = request("POST", "/trigger/999999999999999999/invoke",
               {"keyword": "test"},
               headers={"X-Sys-Token": "test-token"})
if resp:
    body = resp.json()
    check("HTTP 200", resp.status_code == 200)
    check("status 为 failed", body.get("status") == "failed")
    ei = body.get("errorInfo", {})
    check("errorInfo.cause 含 Flow not found",
          "Flow not found" in (ei.get("cause") or ""))


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
    resp = request("POST", f"/trigger/{fid_051}/invoke",
                   {"keyword": "test"},
                   headers={"X-Sys-Token": "test-token"})
    if resp:
        body = resp.json()
        check("HTTP 200", resp.status_code == 200)
        check("executionId 为 string", isinstance(body.get("executionId"), str))
finally:
    cleanup_flow(sid_051, fvid_051, cid_051, cvid_051)


# ═══════════════════════════════════════════════════════════
# IT-060: 快乐路径 — trigger → connector (GET /api/search) → exit
# ═══════════════════════════════════════════════════════════
print("\n=== IT-060: 快乐路径（trigger→connector→exit, GET + 2 query参数 + 列表数据）===")
sid_060 = snow_id()
fvid_060 = cid_060 = cvid_060 = None
try:
    cid_060, cvid_060 = setup_connector()
    fid_060, fvid_060 = setup_flow(sid_060, lifecycle_status=1,
                                   orchestration=build_orchestration(cvid_060),
                                   connector_id=cid_060, connector_version_id=cvid_060)
    resp = request("POST", f"/trigger/{fid_060}/invoke",
                   {"keyword": "搜索关键词ABC"},
                   headers={"X-Sys-Token": "test-token"})
    if resp:
        body = resp.json()
        check("HTTP 200", resp.status_code == 200)
        check("status 不为 failed", body.get("status") != "failed",
              f"status={body.get('status')}")
        check("executionId 为 string", isinstance(body.get("executionId"), str))
        check("totalDurationMs 为 int/float",
              isinstance(body.get("totalDurationMs"), (int, float)))

        # ── resultData 验证：exit 提取的嵌套字段 ──
        rd = body.get("resultData")
        check("resultData 为 dict", isinstance(rd, dict))
        if isinstance(rd, dict):
            result_body = rd.get("body", {}) if isinstance(rd.get("body"), dict) else {}
            check("resultData.body.code == 0",
                  result_body.get("code") == 0,
                  f"code={result_body.get('code')}")
            check("resultData.body.message == success",
                  result_body.get("message") == "success")
            check("resultData.body.searchKeyword == 搜索关键词ABC",
                  result_body.get("searchKeyword") == "搜索关键词ABC",
                  f"searchKeyword={result_body.get('searchKeyword')}")
            check("resultData.body.totalCount == 2",
                  result_body.get("totalCount") == 2,
                  f"totalCount={result_body.get('totalCount')}")
            check("resultData.body.firstItemId == item_001",
                  result_body.get("firstItemId") == "item_001",
                  f"firstItemId={result_body.get('firstItemId')}")
            check("resultData.body.firstItemName == 搜索结果A",
                  result_body.get("firstItemName") == "搜索结果A",
                  f"firstItemName={result_body.get('firstItemName')}")
            check("resultData.body.firstItemScore == 95.5",
                  result_body.get("firstItemScore") == 95.5,
                  f"firstItemScore={result_body.get('firstItemScore')}")
            check("resultData.body.firstItemFirstTag == hot",
                  result_body.get("firstItemFirstTag") == "hot",
                  f"firstItemFirstTag={result_body.get('firstItemFirstTag')}")
            check("resultData.body.firstItemDetailUrl == https://example.com/a",
                  result_body.get("firstItemDetailUrl") == "https://example.com/a",
                  f"firstItemDetailUrl={result_body.get('firstItemDetailUrl')}")

        # ── steps 验证 ──
        steps = body.get("steps")
        check("steps 为 list", isinstance(steps, list))
        if isinstance(steps, list):
            check("steps 有 3 个条目", len(steps) == 3, f"实际: {len(steps)}")

            connector_step = None
            for s in steps:
                if s.get("nodeType") == "connector":
                    connector_step = s
                    break

            check("connector step 存在", connector_step is not None)
            if connector_step:
                check("connector step status 为 success",
                      connector_step.get("status") == "success",
                      f"status={connector_step.get('status')}")
                check("connector step inputData 存在",
                      connector_step.get("inputData") is not None)
                check("connector step outputData 存在",
                      connector_step.get("outputData") is not None)
finally:
    cleanup_flow(sid_060, fvid_060, cid_060, cvid_060)


# ═══════════════════════════════════════════════════════════
# IT-061: 请求体不符合 inputContract（缺必填 keyword）
# ═══════════════════════════════════════════════════════════
print("\n=== IT-061: 触发请求体不符合 inputContract（缺必填 keyword）===")
sid_061 = snow_id()
fvid_061 = cid_061 = cvid_061 = None
try:
    cid_061, cvid_061 = setup_connector()
    fid_061, fvid_061 = setup_flow(sid_061, lifecycle_status=1,
                                   orchestration=build_orchestration(cvid_061),
                                   connector_id=cid_061, connector_version_id=cvid_061)
    resp = request("POST", f"/trigger/{fid_061}/invoke",
                   {"other": "no_keyword"},
                   headers={"X-Sys-Token": "test-token"})
    if resp:
        body = resp.json()
        check("HTTP 200", resp.status_code == 200)
        check("status 为 failed 或 errorInfo 存在",
              body.get("status") == "failed" or "errorInfo" in body)
finally:
    cleanup_flow(sid_061, fvid_061, cid_061, cvid_061)


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
    resp = request("POST", f"/trigger/{fid_062}/invoke",
                   {"sender": "test_user"},
                   headers={"X-Sys-Token": "test-token"})
    if resp:
        body = resp.json()
        check("HTTP 200", resp.status_code == 200, f"实际: {resp.status_code}")
        check("status 为 failed", body.get("status") == "failed",
              f"status={body.get('status')}")
        check("errorInfo 存在", "errorInfo" in body)

        steps = body.get("steps")
        if isinstance(steps, list):
            connector_step = None
            for s in steps:
                if s.get("nodeType") == "connector":
                    connector_step = s
                    break
            check("connector step 存在", connector_step is not None)
            if connector_step:
                check("connector step status 为 failed",
                      connector_step.get("status") == "failed",
                      f"status={connector_step.get('status')}")
                check("connector step errorInfo 存在",
                      connector_step.get("errorInfo") is not None)
finally:
    cleanup_flow(sid_062, fvid_062, cid_062, cvid_062)


# ═══════════════════════════════════════════════════════════
# IT-063: 表达式引用链验证（constant + $. 引用混合 + 嵌套提取）
# ═══════════════════════════════════════════════════════════
print("\n=== IT-063: 表达式引用链验证（constant + $. 引用混合 + 嵌套提取）===")
sid_063 = snow_id()
fvid_063 = cid_063 = cvid_063 = None
try:
    cid_063, cvid_063 = setup_connector()
    fid_063, fvid_063 = setup_flow(sid_063, lifecycle_status=1,
                                   orchestration=build_orchestration(cvid_063),
                                   connector_id=cid_063, connector_version_id=cvid_063)
    resp = request("POST", f"/trigger/{fid_063}/invoke",
                   {"keyword": "expr_test"},
                   headers={"X-Sys-Token": "test-token"})
    if resp:
        body = resp.json()
        check("HTTP 200", resp.status_code == 200)
        check("status 不为 failed", body.get("status") != "failed",
              f"status={body.get('status')}")

        steps = body.get("steps")
        if isinstance(steps, list):
            connector_step = None
            for s in steps:
                if s.get("nodeType") == "connector":
                    connector_step = s
                    break

            if connector_step:
                input_data = connector_step.get("inputData", {})
                query = input_data.get("query", {}) if isinstance(input_data, dict) else {}
                check("connector inputData.query.keyword == expr_test",
                      query.get("keyword") == "expr_test",
                      f"keyword={query.get('keyword')}")
                check("connector inputData.query.page == 1 (constant)",
                      query.get("page") == 1 or str(query.get("page")) == "1",
                      f"page={query.get('page')}")

                output_data = connector_step.get("outputData", {})
                check("connector output 含 data 字段",
                      isinstance(output_data, dict) and "data" in output_data,
                      f"output keys: {list(output_data.keys()) if isinstance(output_data, dict) else type(output_data)}")

        rd = body.get("resultData")
        if isinstance(rd, dict):
            result_body = rd.get("body", {}) if isinstance(rd.get("body"), dict) else {}
            check("exit resultData.body.searchKeyword == expr_test",
                  result_body.get("searchKeyword") == "expr_test",
                  f"searchKeyword={result_body.get('searchKeyword')}")
            check("exit resultData.body.code == 0",
                  result_body.get("code") == 0)
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

    # ── IT-065: 限流阈值内正常执行（复用同一 flow）──────
    print("\n=== IT-065: 限流阈值内正常执行（单次请求）===")
    time.sleep(1.5)
    resp = request("POST", f"/trigger/{fid_064}/invoke",
                   {"keyword": "test_single"},
                   headers={"X-Sys-Token": "test-token"})
    if resp:
        body = resp.json()
        check("HTTP 200", resp.status_code == 200)
        check("executionId 存在", bool(body.get("executionId")))
finally:
    cleanup_flow(sid_064, fvid_064, cid_064, cvid_064)


# ═══════════════════════════════════════════════════════════
# Shutdown mock server
# ═══════════════════════════════════════════════════════════
mock_server.shutdown()
print("\nMock server shut down.")
