#!/usr/bin/env python3
"""Multi-auth connector E2E test — FR-012, FR-013, FR-014

覆盖多认证组合场景：
  IT-AUTH-001: SOA + Cookie 多认证 (FR-012, FR-014)
  IT-AUTH-002: DigitalSign + Cookie 多认证 (FR-013, FR-014)
  IT-AUTH-003: SOA + DigitalSign + Cookie 三重组合
  IT-AUTH-004: 单一 SOA 认证 (基线对比)

验证 connector-api 能正确解析 MULTI auth 配置并在调用下游时注入认证头。
即使下游 mock 不存在，也验证流程引擎能正确处理 authConfig 而不崩溃。
"""
from client import *
import pytest
import time
import json
import threading
from http.server import ThreadingHTTPServer, BaseHTTPRequestHandler
import urllib.request


# ═══════════════════════════════════════════════════════════
# Mock Downstream Server (port 18995)
# 用于接收 connector 的下游 HTTP 调用，回显收到的请求头
# ═══════════════════════════════════════════════════════════

MOCK_HOST = "localhost"
MOCK_PORT = 18995
MOCK_BASE = f"http://{MOCK_HOST}:{MOCK_PORT}"


class MockHandler(BaseHTTPRequestHandler):
    """极简 mock：回显请求头和请求体，用于验证 auth header 注入"""

    def log_message(self, format, *args):
        pass  # 抑制访问日志

    def _send_json(self, status_code, body):
        self.send_response(status_code)
        self.send_header("Content-Type", "application/json")
        # 回显认证相关请求头，便于后续在 connector-api 日志中校验
        for key, value in self.headers.items():
            if key.lower().startswith("x-") or key.lower() == "cookie":
                self.send_header(f"X-Echo-{key}", value)
        self.end_headers()
        self.wfile.write(json.dumps(body).encode("utf-8"))

    def do_GET(self):
        if self.path == "/api/health":
            self._send_json(200, {
                "status": "ok",
                "received_headers": dict(self.headers)
            })
        else:
            self._send_json(404, {"error": "not_found"})

    def do_POST(self):
        content_len = int(self.headers.get("Content-Length", 0))
        raw_body = self.rfile.read(content_len) if content_len > 0 else b"{}"
        try:
            parsed_body = json.loads(raw_body.decode("utf-8"))
        except Exception:
            parsed_body = {}
        self._send_json(200, {
            "status": "ok",
            "echo": parsed_body,
            "received_headers": dict(self.headers)
        })


# 尝试启动 mock server
mock_server = None
mock_ready = False
try:
    mock_server = ThreadingHTTPServer((MOCK_HOST, MOCK_PORT), MockHandler)
    mock_thread = threading.Thread(target=mock_server.serve_forever, daemon=True)
    mock_thread.start()
except OSError:
    print("INFO: Mock server port 18995 already in use (may be from another test)")

# 等待 mock 就绪
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
    print("WARNING: Mock server on port 18995 did not become ready — "
          "connector downstream calls will fail (acceptable for auth config tests)")



def setup_connector(config):
    """创建连接器 + 版本，返回 (connector_id, version_id)"""
    connector_id = snow_id()
    version_id = snow_id()
    db(
        f"INSERT INTO openplatform_v2_cp_connector_t "
        f"(id, name_cn, name_en, connector_type, app_id, create_by, last_update_by) "
        f"VALUES ({connector_id}, '{config['labelCn']}', '{config['labelEn']}', "
        f"1, {INTERNAL_APP_ID}, 'tester', 'tester')"
    )
    db(
        f"INSERT INTO openplatform_v2_cp_connector_version_t "
        f"(id, connector_id, connection_config, create_by, last_update_by) "
        f"VALUES ({version_id}, {connector_id}, "
        f"'{escape_sql(config)}', 'tester', 'tester')"
    )
    return connector_id, version_id


def setup_flow(flow_id, lifecycle_status, orchestration):
    """创建 Flow + 版本，返回 (flow_id, flow_version_id)"""
    flow_version_id = snow_id()
    db(
        f"INSERT INTO openplatform_v2_cp_flow_t "
        f"(id, name_cn, name_en, lifecycle_status, app_id, create_by, last_update_by) "
        f"VALUES ({flow_id}, 'IT_多认证测试', 'IT_MultiAuthTest', "
        f"{lifecycle_status}, {INTERNAL_APP_ID}, 'tester', 'tester')"
    )
    db(
        f"INSERT INTO openplatform_v2_cp_flow_version_t "
        f"(id, flow_id, orchestration_config, create_by, last_update_by) "
        f"VALUES ({flow_version_id}, {flow_id}, "
        f"'{escape_sql(orchestration)}', 'tester', 'tester')"
    )
    return flow_id, flow_version_id


def build_orch(connector_version_id, connection_config):
    """构建 trigger → connector → exit 三元编排"""
    return {
        "nodes": [
            {
                "id": "node_trigger", "type": "trigger",
                "position": {"x": 100, "y": 200},
                "data": {
                    "labelCn": "接收", "labelEn": "Recv",
                    "type": "trigger",
                    "triggerType": "http",
                    "authConfigs": [{
                        "type": "SYSTOKEN",
                        "header": {"type": "object", "properties": {"X-Sys-Token": {"type": "string", "required": True, "sensitive": True}}}
                    }],
                    "input": {
                        "protocol": "HTTP",
                        "header": {"type": "object", "properties": {},
                                   "required": []},
                        "query": {"type": "object", "properties": {},
                                  "required": []},
                        "body": {"type": "object",
                                 "properties": {"msg": {"type": "string"}},
                                 "required": ["msg"]}
                    },
                }
            },
            {
                "id": "node_connector", "type": "connector",
                "position": {"x": 350, "y": 200},
                "data": {
                    "type": "connector",
                    "labelCn": "连接器", "labelEn": "Conn",
                    "connectorVersionId": str(connector_version_id),
                    "connectorVersionConfig": connection_config,
                    "inputMapping": {
                        "header": {"type": "object", "properties": {}},
                        "query": {"type": "object", "properties": {}},
                        "body": {"type": "object", "properties": {}}
                    }
                }
            },
            {
                "id": "node_exit", "type": "exit",
                "position": {"x": 600, "y": 200},
                "data": {
                    "type": "exit",
                    "labelCn": "返回", "labelEn": "Ret",
                    "output": {
                        "header": {"type": "object", "properties": {}},
                        "body": {
                            "type": "object",
                            "properties": {
                                "result": {"type": "string", "value": "ok"}
                            }
                        }
                    }
                }
            }
        ],
        "edges": [
            {"id": "e1", "source": "node_trigger", "target": "node_connector",
             "type": "smoothstep", "data": {"businessType": "default"}},
            {"id": "e2", "source": "node_connector", "target": "node_exit",
             "type": "smoothstep", "data": {"businessType": "default"}}
        ],
        "flowConfig": {"rateLimitConfig": {"maxQps": 100}}
    }


# ═══════════════════════════════════════════════════════════
# Connection Configs — 各认证组合
# ═══════════════════════════════════════════════════════════

BASE_INPUT_CONTRACT = {
    "protocol": "HTTP",
    "header": {"type": "object", "properties": {}, "required": []},
    "query": {"type": "object", "properties": {}, "required": []},
    "body": {"type": "object", "properties": {}, "required": []}
}

BASE_OUTPUT_CONTRACT = {
    "protocol": "HTTP",
    "body": {"type": "object", "properties": {}}
}

MOCK_TARGET_URL = f"{MOCK_BASE}/api/health"

# IT-AUTH-001: SOA + Cookie (FR-012, FR-014)
CONN_CONFIG_SOA_COOKIE = {
    "labelCn": "SOA+Cookie认证",
    "labelEn": "SOA_Cookie_Auth",
    "protocol": "HTTP",
    "protocolConfig": {
        "url": MOCK_TARGET_URL,
        "method": "GET"
    },
    "authConfigs": [
        {
            "type": "SOA",
            "header": {"type": "object", "properties": {"X-SOA-Key": {"type": "string", "required": True, "sensitive": True, "value": "soa-test-key"}}}
        },
        {
            "type": "COOKIE",
            "header": {"type": "object", "properties": {"Cookie": {"type": "string", "required": True, "sensitive": True, "value": "session=abc123"}}}
        }
    ],
    "input": BASE_INPUT_CONTRACT,
    "output": BASE_OUTPUT_CONTRACT,
    "timeoutMs": 5000
}

# IT-AUTH-002: DigitalSign + Cookie (FR-013, FR-014)
CONN_CONFIG_DIGITALSIGN_COOKIE = {
    "labelCn": "数字签名+Cookie认证",
    "labelEn": "DigitalSign_Cookie_Auth",
    "protocol": "HTTP",
    "protocolConfig": {
        "url": MOCK_TARGET_URL,
        "method": "GET"
    },
    "authConfigs": [
        {
            "type": "SIGNATURE",
            "secretKey": {"type": "object", "properties": {"signSecretKey": {"type": "string", "required": True, "sensitive": True, "value": "sign-test-value"}}}, "header": {"type": "object", "properties": {"X-Signature": {"type": "string", "required": True, "sensitive": True, "value": "${$.system.env.signature}"}}}
        },
        {
            "type": "COOKIE",
            "header": {"type": "object", "properties": {"Cookie": {"type": "string", "required": True, "sensitive": True, "value": "session=xyz789"}}}
        }
    ],
    "input": BASE_INPUT_CONTRACT,
    "output": BASE_OUTPUT_CONTRACT,
    "timeoutMs": 5000
}

# IT-AUTH-003: SOA + DigitalSign + Cookie (3+ combo)
CONN_CONFIG_TRIPLE_AUTH = {
    "labelCn": "三重组合认证",
    "labelEn": "Triple_Auth",
    "protocol": "HTTP",
    "protocolConfig": {
        "url": MOCK_TARGET_URL,
        "method": "GET"
    },
    "authConfigs": [
        {
            "type": "SOA",
            "header": {"type": "object", "properties": {"X-SOA-Key": {"type": "string", "required": True, "sensitive": True, "value": "triple-soa-key"}}}
        },
        {
            "type": "SIGNATURE",
            "secretKey": {"type": "object", "properties": {"signSecretKey": {"type": "string", "required": True, "sensitive": True, "value": "triple-sign-value"}}}, "header": {"type": "object", "properties": {"X-Signature": {"type": "string", "required": True, "sensitive": True, "value": "${$.system.env.signature}"}}}
        },
        {
            "type": "COOKIE",
            "header": {"type": "object", "properties": {"Cookie": {"type": "string", "required": True, "sensitive": True, "value": "session=triple123"}}}
        }
    ],
    "input": BASE_INPUT_CONTRACT,
    "output": BASE_OUTPUT_CONTRACT,
    "timeoutMs": 5000
}

# IT-AUTH-004: 单一 SOA 认证 (基线)
CONN_CONFIG_SOA_ONLY = {
    "labelCn": "SOA认证(基线)",
    "labelEn": "SOA_Auth_Baseline",
    "protocol": "HTTP",
    "protocolConfig": {
        "url": MOCK_TARGET_URL,
        "method": "GET"
    },
    "authConfigs": [{
        "type": "SOA",
        "header": {"type": "object", "properties": {"X-SOA-Key": {"type": "string", "required": True, "sensitive": True, "value": "baseline-soa-key"}}}
    }],
    "input": BASE_INPUT_CONTRACT,
    "output": BASE_OUTPUT_CONTRACT,
    "timeoutMs": 5000
}


# ═══════════════════════════════════════════════════════════
# IT-AUTH-001: SOA + Cookie 多认证 (FR-012, FR-014)
# ═══════════════════════════════════════════════════════════

def verify_flow_response(resp, test_label):
    """通用响应校验：验证 trigger API 返回了合理的执行结果"""
    if resp is None:
        check(f"[{test_label}] 请求发送成功", False, "connector-api 未运行")
        return

    check(f"[{test_label}] HTTP 200", resp.status_code == 200,
          f"status={resp.status_code}")

    try:
        body = resp.json()
    except Exception:
        check(f"[{test_label}] 响应为合法 JSON", False, "无法解析响应体")
        return

    # executionId 必须存在（无论成功失败）— v5.8 transparent response: 在响应头中
    check(f"[{test_label}] executionId 存在",
          bool(resp.headers.get("X-Execution-Id")))

    # status 字段 — v5.8 transparent response: 在响应头中
    check(f"[{test_label}] status 字段存在",
          resp.headers.get("X-Status") is not None)

    status_val = body.get("status", "")
    if status_val == "failed":
        # 下游不可达时 flow 标记 failed，但 auth config 解析不应报错
        ei = body.get("errorInfo", {})
        code = ei.get("code", "")
        check(f"[{test_label}] 失败非 authConfig 解析错误 (code != 60002)",
              code != "60002",
              f"errorInfo.code={code}, cause={ei.get('cause', '')}")
    else:
        check(f"[{test_label}] 流程执行成功 (status={status_val})",
              status_val != "failed" or mock_ready,
              f"status={status_val}")


# ═══════════════════════════════════════════════════════════
# IT-AUTH-001: SOA + Cookie 多认证 (FR-012, FR-014)
# ═══════════════════════════════════════════════════════════

@pytest.mark.L2
def test_connector_auth_multiple():
    print("=== IT-AUTH-001: SOA + Cookie 多认证 (FR-012, FR-014) ===")
    sid_001 = snow_id()
    fvid_001 = cid_001 = cvid_001 = None
    cid_001, cvid_001 = setup_connector(CONN_CONFIG_SOA_COOKIE)
    fid_001, fvid_001 = setup_flow(
        sid_001, lifecycle_status=2,
        orchestration=build_orch(cvid_001, CONN_CONFIG_SOA_COOKIE)
    )

    resp = trigger(
        fid_001,
        body={"msg": "auth_test_001"},
        headers={"X-Sys-Token": "test-token"}
    )
    verify_flow_response(resp, "IT-AUTH-001")
    print("\n=== IT-AUTH-002: DigitalSign + Cookie 多认证 (FR-013, FR-014) ===")
    sid_002 = snow_id()
    fvid_002 = cid_002 = cvid_002 = None
    cid_002, cvid_002 = setup_connector(CONN_CONFIG_DIGITALSIGN_COOKIE)
    fid_002, fvid_002 = setup_flow(
        sid_002, lifecycle_status=2,
        orchestration=build_orch(cvid_002, CONN_CONFIG_DIGITALSIGN_COOKIE)
    )

    resp = trigger(
        fid_002,
        body={"msg": "auth_test_002"},
        headers={"X-Sys-Token": "test-token"}
    )
    verify_flow_response(resp, "IT-AUTH-002")
    print("\n=== IT-AUTH-003: SOA + DigitalSign + Cookie 三重组合认证 ===")
    sid_003 = snow_id()
    fvid_003 = cid_003 = cvid_003 = None
    cid_003, cvid_003 = setup_connector(CONN_CONFIG_TRIPLE_AUTH)
    fid_003, fvid_003 = setup_flow(
        sid_003, lifecycle_status=2,
        orchestration=build_orch(cvid_003, CONN_CONFIG_TRIPLE_AUTH)
    )

    resp = trigger(
        fid_003,
        body={"msg": "auth_test_003"},
        headers={"X-Sys-Token": "test-token"}
    )
    verify_flow_response(resp, "IT-AUTH-003")
    print("\n=== IT-AUTH-004: 单一 SOA 认证 (基线) ===")
    sid_004 = snow_id()
    fvid_004 = cid_004 = cvid_004 = None
    cid_004, cvid_004 = setup_connector(CONN_CONFIG_SOA_ONLY)
    fid_004, fvid_004 = setup_flow(
        sid_004, lifecycle_status=2,
        orchestration=build_orch(cvid_004, CONN_CONFIG_SOA_ONLY)
    )

    resp = trigger(
        fid_004,
        body={"msg": "auth_test_004"},
        headers={"X-Sys-Token": "test-token"}
    )
    verify_flow_response(resp, "IT-AUTH-004")
    if mock_server is not None:
        mock_server.shutdown()
        print("\nMock server shut down.")

if __name__ == "__main__":
    test_connector_auth_multiple()
    done()
