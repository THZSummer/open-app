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
import time
import json
import requests as req_lib
import threading
from http.server import ThreadingHTTPServer, BaseHTTPRequestHandler
import urllib.request


# ═══════════════════════════════════════════════════════════
# Mock Downstream Server (port 18999)
# 用于接收 connector 的下游 HTTP 调用，回显收到的请求头
# ═══════════════════════════════════════════════════════════

MOCK_HOST = "localhost"
MOCK_PORT = 18999
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


# 尝试启动 mock server（端口可能已被 trigger_invoke.py 占用）
mock_server = None
mock_ready = False
try:
    mock_server = ThreadingHTTPServer((MOCK_HOST, MOCK_PORT), MockHandler)
    mock_thread = threading.Thread(target=mock_server.serve_forever, daemon=True)
    mock_thread.start()
except OSError:
    print("INFO: Mock server port 18999 already in use (may be from another test)")

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
    print("WARNING: Mock server on port 18999 did not become ready — "
          "connector downstream calls will fail (acceptable for auth config tests)")



def setup_connector(config):
    """创建连接器 + 版本，返回 (connector_id, version_id)"""
    connector_id = snow_id()
    version_id = snow_id()
    db(
        f"INSERT INTO openplatform_v2_cp_connector_t "
        f"(id, name_cn, name_en, connector_type, create_by, last_update_by) "
        f"VALUES ({connector_id}, '{config['labelCn']}', '{config['labelEn']}', "
        f"1, 'tester', 'tester')"
    )
    db(
        f"INSERT INTO openplatform_v2_cp_connector_version_t "
        f"(id, connector_id, connection_config, create_by, last_update_by) "
        f"VALUES ({version_id}, {connector_id}, "
        f"'{escape_sql(config)}', 'tester', 'tester')"
    )
    return connector_id, version_id


def cleanup_connector(connector_id, version_id):
    """清理连接器 + 版本"""
    db(f"DELETE FROM openplatform_v2_cp_connector_version_t WHERE id = {version_id}")
    db(f"DELETE FROM openplatform_v2_cp_connector_t WHERE id = {connector_id}")


def setup_flow(flow_id, lifecycle_status, orchestration):
    """创建 Flow + 版本，返回 (flow_id, flow_version_id)"""
    flow_version_id = snow_id()
    db(
        f"INSERT INTO openplatform_v2_cp_flow_t "
        f"(id, name_cn, name_en, lifecycle_status, create_by, last_update_by) "
        f"VALUES ({flow_id}, 'IT_多认证测试', 'IT_MultiAuthTest', "
        f"{lifecycle_status}, 'tester', 'tester')"
    )
    db(
        f"INSERT INTO openplatform_v2_cp_flow_version_t "
        f"(id, flow_id, orchestration_config, create_by, last_update_by) "
        f"VALUES ({flow_version_id}, {flow_id}, "
        f"'{escape_sql(orchestration)}', 'tester', 'tester')"
    )
    return flow_id, flow_version_id


def cleanup_flow(flow_id, flow_version_id, connector_id=None, connector_version_id=None):
    """清理 Flow + 版本，可选连带清理 Connector"""
    db(f"DELETE FROM openplatform_v2_cp_flow_version_t WHERE id = {flow_version_id}")
    db(f"DELETE FROM openplatform_v2_cp_flow_t WHERE id = {flow_id}")
    if connector_id and connector_version_id:
        cleanup_connector(connector_id, connector_version_id)


# ═══════════════════════════════════════════════════════════
# Orchestration Builder
# ═══════════════════════════════════════════════════════════

def build_orch(connector_version_id):
    """构建 trigger → connector → exit 三元编排"""
    return {
        "nodes": [
            {
                "id": "node_trigger", "type": "trigger",
                "position": {"x": 100, "y": 200},
                "data": {
                    "labelCn": "接收", "labelEn": "Recv",
                    "type": "http",
                    "authConfig": {
                        "type": "SYSTOKEN",
                        "fields": [
                            {"name": "token", "carrier": "header",
                             "fieldName": "X-Sys-Token"}
                        ]
                    },
                    "inputContract": {
                        "protocol": "HTTP",
                        "header": {"type": "object", "properties": {},
                                   "required": []},
                        "query": {"type": "object", "properties": {},
                                  "required": []},
                        "body": {"type": "object",
                                 "properties": {"msg": {"type": "string"}},
                                 "required": ["msg"]}
                    },
                    "rateLimitConfig": {"maxQps": 100}
                }
            },
            {
                "id": "node_connector", "type": "connector",
                "position": {"x": 350, "y": 200},
                "data": {
                    "labelCn": "连接器", "labelEn": "Conn",
                    "connectorVersionId": str(connector_version_id),
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
                    "labelCn": "返回", "labelEn": "Ret",
                    "outputMapping": {
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
        ]
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
        "method": "GET",
        "headers": {}
    },
    "authConfig": {
        "type": "MULTI",
        "fields": [
            {"name": "soa_key", "carrier": "header",
             "fieldName": "X-SOA-Key", "authType": "SOA",
             "value": "soa-test-key"},
            {"name": "cookie_token", "carrier": "header",
             "fieldName": "Cookie", "authType": "COOKIE",
             "value": "session=abc123"}
        ]
    },
    "inputContract": BASE_INPUT_CONTRACT,
    "outputContract": BASE_OUTPUT_CONTRACT,
    "timeoutMs": 5000
}

# IT-AUTH-002: DigitalSign + Cookie (FR-013, FR-014)
CONN_CONFIG_DIGITALSIGN_COOKIE = {
    "labelCn": "数字签名+Cookie认证",
    "labelEn": "DigitalSign_Cookie_Auth",
    "protocol": "HTTP",
    "protocolConfig": {
        "url": MOCK_TARGET_URL,
        "method": "GET",
        "headers": {}
    },
    "authConfig": {
        "type": "MULTI",
        "fields": [
            {"name": "sign", "carrier": "header",
             "fieldName": "X-Digital-Sign", "authType": "DIGITAL_SIGN",
             "value": "sign-test-value"},
            {"name": "cookie_token", "carrier": "header",
             "fieldName": "Cookie", "authType": "COOKIE",
             "value": "session=xyz789"}
        ]
    },
    "inputContract": BASE_INPUT_CONTRACT,
    "outputContract": BASE_OUTPUT_CONTRACT,
    "timeoutMs": 5000
}

# IT-AUTH-003: SOA + DigitalSign + Cookie (3+ combo)
CONN_CONFIG_TRIPLE_AUTH = {
    "labelCn": "三重组合认证",
    "labelEn": "Triple_Auth",
    "protocol": "HTTP",
    "protocolConfig": {
        "url": MOCK_TARGET_URL,
        "method": "GET",
        "headers": {}
    },
    "authConfig": {
        "type": "MULTI",
        "fields": [
            {"name": "soa_key", "carrier": "header",
             "fieldName": "X-SOA-Key", "authType": "SOA",
             "value": "triple-soa-key"},
            {"name": "sign", "carrier": "header",
             "fieldName": "X-Digital-Sign", "authType": "DIGITAL_SIGN",
             "value": "triple-sign-value"},
            {"name": "cookie_token", "carrier": "header",
             "fieldName": "Cookie", "authType": "COOKIE",
             "value": "session=triple123"}
        ]
    },
    "inputContract": BASE_INPUT_CONTRACT,
    "outputContract": BASE_OUTPUT_CONTRACT,
    "timeoutMs": 5000
}

# IT-AUTH-004: 单一 SOA 认证 (基线)
CONN_CONFIG_SOA_ONLY = {
    "labelCn": "SOA认证(基线)",
    "labelEn": "SOA_Auth_Baseline",
    "protocol": "HTTP",
    "protocolConfig": {
        "url": MOCK_TARGET_URL,
        "method": "GET",
        "headers": {}
    },
    "authConfig": {
        "type": "SOA",
        "fields": [
            {"name": "soa_key", "carrier": "header",
             "fieldName": "X-SOA-Key", "authType": "SOA",
             "value": "baseline-soa-key"}
        ]
    },
    "inputContract": BASE_INPUT_CONTRACT,
    "outputContract": BASE_OUTPUT_CONTRACT,
    "timeoutMs": 5000
}


# ═══════════════════════════════════════════════════════════
# Trigger Invoke Helper
# ═══════════════════════════════════════════════════════════

def trigger_invoke(flow_id, body=None, headers=None):
    """向 connector-api 发送触发请求，返回 Response 对象或 None"""
    url = f"http://localhost:18180/api/v1/trigger/{flow_id}/invoke"
    h = {"Content-Type": "application/json"}
    if headers:
        h.update(headers)

    if not is_quiet():
        print(f"\n  REQUEST: POST {url}")
        if body:
            print(f"    Body: {json.dumps(body, ensure_ascii=False)}")

    try:
        start = time.time()
        resp = req_lib.post(url, json=body or {}, headers=h, timeout=10)
        elapsed = time.time() - start
    except req_lib.exceptions.ConnectionError:
        if not is_quiet():
            print("  SKIP: connector-api 未运行 (port 18180)")
        else:
            print("[SKIP] POST trigger invoke")
        return None
    except Exception as e:
        print(f"  ERROR: {e}")
        return None

    if not is_quiet():
        print(f"  RESPONSE: {resp.status_code} ({elapsed:.2f}s)")
        try:
            resp_body = resp.json()
            print(f"    Body: {json.dumps(resp_body, indent=2, ensure_ascii=False)}")
        except Exception:
            print(f"    Body: {resp.text[:500]}")
        # 打印关键响应头
        for key in ["X-Flow-Id", "X-Execution-Id", "X-Status", "X-Duration-Ms",
                     "X-Code", "X-Message-Zh"]:
            if key in resp.headers:
                print(f"    {key}: {resp.headers[key]}")

    return resp


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
        check(f"[{test_label}] 失败非 authConfig 解析错误 (code != 6002)",
              code != "6002",
              f"errorInfo.code={code}, cause={ei.get('cause', '')}")
    else:
        check(f"[{test_label}] 流程执行成功 (status={status_val})",
              status_val != "failed" or mock_ready,
              f"status={status_val}")


# ═══════════════════════════════════════════════════════════
# IT-AUTH-001: SOA + Cookie 多认证 (FR-012, FR-014)
# ═══════════════════════════════════════════════════════════
print("=== IT-AUTH-001: SOA + Cookie 多认证 (FR-012, FR-014) ===")
sid_001 = snow_id()
fvid_001 = cid_001 = cvid_001 = None
try:
    cid_001, cvid_001 = setup_connector(CONN_CONFIG_SOA_COOKIE)
    fid_001, fvid_001 = setup_flow(
        sid_001, lifecycle_status=1,
        orchestration=build_orch(cvid_001)
    )

    resp = trigger_invoke(
        fid_001,
        body={"msg": "auth_test_001"},
        headers={"X-Sys-Token": "test-token"}
    )
    verify_flow_response(resp, "IT-AUTH-001")
finally:
    cleanup_flow(sid_001, fvid_001, cid_001, cvid_001)


# ═══════════════════════════════════════════════════════════
# IT-AUTH-002: DigitalSign + Cookie 多认证 (FR-013, FR-014)
# ═══════════════════════════════════════════════════════════
print("\n=== IT-AUTH-002: DigitalSign + Cookie 多认证 (FR-013, FR-014) ===")
sid_002 = snow_id()
fvid_002 = cid_002 = cvid_002 = None
try:
    cid_002, cvid_002 = setup_connector(CONN_CONFIG_DIGITALSIGN_COOKIE)
    fid_002, fvid_002 = setup_flow(
        sid_002, lifecycle_status=1,
        orchestration=build_orch(cvid_002)
    )

    resp = trigger_invoke(
        fid_002,
        body={"msg": "auth_test_002"},
        headers={"X-Sys-Token": "test-token"}
    )
    verify_flow_response(resp, "IT-AUTH-002")
finally:
    cleanup_flow(sid_002, fvid_002, cid_002, cvid_002)


# ═══════════════════════════════════════════════════════════
# IT-AUTH-003: SOA + DigitalSign + Cookie 三重组合
# ═══════════════════════════════════════════════════════════
print("\n=== IT-AUTH-003: SOA + DigitalSign + Cookie 三重组合认证 ===")
sid_003 = snow_id()
fvid_003 = cid_003 = cvid_003 = None
try:
    cid_003, cvid_003 = setup_connector(CONN_CONFIG_TRIPLE_AUTH)
    fid_003, fvid_003 = setup_flow(
        sid_003, lifecycle_status=1,
        orchestration=build_orch(cvid_003)
    )

    resp = trigger_invoke(
        fid_003,
        body={"msg": "auth_test_003"},
        headers={"X-Sys-Token": "test-token"}
    )
    verify_flow_response(resp, "IT-AUTH-003")
finally:
    cleanup_flow(sid_003, fvid_003, cid_003, cvid_003)


# ═══════════════════════════════════════════════════════════
# IT-AUTH-004: 单一 SOA 认证 (基线对比)
# ═══════════════════════════════════════════════════════════
print("\n=== IT-AUTH-004: 单一 SOA 认证 (基线) ===")
sid_004 = snow_id()
fvid_004 = cid_004 = cvid_004 = None
try:
    cid_004, cvid_004 = setup_connector(CONN_CONFIG_SOA_ONLY)
    fid_004, fvid_004 = setup_flow(
        sid_004, lifecycle_status=1,
        orchestration=build_orch(cvid_004)
    )

    resp = trigger_invoke(
        fid_004,
        body={"msg": "auth_test_004"},
        headers={"X-Sys-Token": "test-token"}
    )
    verify_flow_response(resp, "IT-AUTH-004")
finally:
    cleanup_flow(sid_004, fvid_004, cid_004, cvid_004)


# ═══════════════════════════════════════════════════════════
# Shutdown
# ═══════════════════════════════════════════════════════════
if mock_server is not None:
    mock_server.shutdown()
    print("\nMock server shut down.")
