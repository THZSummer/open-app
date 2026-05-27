#!/usr/bin/env python3
"""HTTP 触发 (IT-049~051, IT-060~065)

覆盖 POST /api/v1/trigger/{flowId}/invoke 全部场景：
  - IT-049: 凭证缺失 → errorInfo.code=6001（认证失败）
  - IT-050: flow 不存在 → errorInfo.cause 含 "Flow not found"
  - IT-051: flow 未运行 → 可正常执行（lifecycle_status 不影响）
  - IT-060: 快乐路径（entry→connector→exit）→ 完整数据管道验证
  - IT-061: 请求体不符合 inputContract → errorInfo
  - IT-062: connector 下游失败 (500) → status=failed + connector step errorInfo
  - IT-063: 表达式引用链验证（constant:/$. 引用混合）
  - IT-064: 超过限流阈值 → 429
  - IT-065: 限流阈值内正常 → 200

注：connector-api 实际响应格式为直接返回 ExecutionResult，
    非标准 {code,messageZh,messageEn,data} 包装。
"""
from client import *
import subprocess
import time
import json
import requests
from concurrent.futures import ThreadPoolExecutor, as_completed
from http.server import HTTPServer, BaseHTTPRequestHandler
import threading
import urllib.request


# ═══════════════════════════════════════════════════════════
# Mock Downstream Server (port 18999)
# ═══════════════════════════════════════════════════════════

MOCK_HOST = "localhost"
MOCK_PORT = 18999
MOCK_BASE = f"http://{MOCK_HOST}:{MOCK_PORT}"


class MockHandler(BaseHTTPRequestHandler):
    def log_message(self, format, *args):
        pass  # suppress all HTTP server log output

    def _send_json(self, status_code, body):
        self.send_response(status_code)
        self.send_header("Content-Type", "application/json")
        self.end_headers()
        self.wfile.write(json.dumps(body).encode("utf-8"))

    def do_POST(self):
        content_len = int(self.headers.get("Content-Length", 0))
        raw_body = self.rfile.read(content_len) if content_len > 0 else b"{}"
        try:
            parsed_body = json.loads(raw_body.decode("utf-8"))
        except Exception:
            parsed_body = {}

        if self.path == "/api/echo":
            self._send_json(200, {
                "echo": parsed_body,
                "serverTime": int(time.time() * 1000)
            })
        elif self.path == "/api/fail":
            self._send_json(500, {
                "error": "internal_error",
                "detail": "simulated failure"
            })
        else:
            self._send_json(404, {"error": "not_found"})

    def do_GET(self):
        if self.path == "/api/health":
            self._send_json(200, {"status": "ok"})
        else:
            self._send_json(404, {"error": "not_found"})


mock_server = HTTPServer((MOCK_HOST, MOCK_PORT), MockHandler)
mock_thread = threading.Thread(target=mock_server.serve_forever, daemon=True)
mock_thread.start()

# Verify mock server is up
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
# Helpers
# ═══════════════════════════════════════════════════════════

def snow_id():
    return int(time.time() * 1000000) % 100000000000000000


def setup_flow(snow_id_val, lifecycle_status=1,
               auth_type="SYSTOKEN",
               input_contract_body_required=None,
               rate_limit_qps=100,
               connector_url=None,
               connector_method="POST",
               connector_input_mapping=None,
               exit_output_mapping=None):
    """插入 flow_t + flow_version_t 数据。返回 (flow_id, version_id)

    支持 2 节点流（entry→exit）和 3 节点流（entry→connector→exit）。
    """
    version_id = snow_id()

    subprocess.run([
        "mysql", "-uopenapp", "-popenapp", "openapp", "-e",
        f"INSERT INTO openplatform_v2_cp_flow_t "
        f"(id, name_cn, name_en, lifecycle_status, create_by, last_update_by) "
        f"VALUES ({snow_id_val}, 'IT_触发测试', 'IT_TriggerTest', "
        f"{lifecycle_status}, 'tester', 'tester')"
    ], check=True, capture_output=True)

    # ---- entry node (CRITICAL: type must be "entry", not "trigger") ----
    entry_node = {
        "id": "node_entry",
        "type": "entry",
        "position": {"x": 100, "y": 200},
        "data": {
            "labelCn": "接收",
            "labelEn": "Receive",
            "type": "http",
            "authConfig": {
                "type": auth_type,
                "fields": [{"name": "token", "carrier": "header",
                            "fieldName": "X-Sys-Token"}]
            },
            "inputContract": {
                "protocol": "HTTP",
                "header": {"type": "object", "properties": {},
                           "required": []},
                "query": {"type": "object", "properties": {},
                          "required": []},
                "body": {
                    "type": "object",
                    "properties": {
                        "sender": {"type": "string"},
                        "content": {"type": "string"}
                    },
                    "required": input_contract_body_required or ["sender"]
                }
            },
            "rateLimitConfig": {"maxQps": rate_limit_qps}
        }
    }

    nodes = [entry_node]
    edges = []

    if connector_url:
        # ---- connector node ----
        connector_node = {
            "id": "node_connector",
            "type": "connector",
            "position": {"x": 350, "y": 200},
            "data": {
                "labelCn": "连接器",
                "labelEn": "Connector",
                "url": connector_url,
                "method": connector_method,
                "inputMapping": connector_input_mapping or {
                    "body": {
                        "type": "object",
                        "properties": {
                            "user": {"type": "string", "description": "用户名", "value": "${$.node.node_entry.input.sender}"}
                        }
                    }
                }
            }
        }
        nodes.append(connector_node)
        edges.append({
            "id": "e1", "source": "node_entry", "target": "node_connector",
            "type": "smoothstep", "data": {"businessType": "default"}
        })

        # ---- exit node (with connector reference) ----
        exit_node = {
            "id": "node_exit",
            "type": "exit",
            "position": {"x": 600, "y": 200},
            "data": {
                "labelCn": "返回",
                "labelEn": "Return",
                "outputMapping": exit_output_mapping or {
                    "body": {
                        "type": "object",
                        "properties": {
                            "echo": {"type": "string", "description": "回显", "value": "${$.node.node_connector.output.echo.user}"}
                        }
                    }
                }
            }
        }
        nodes.append(exit_node)
        edges.append({
            "id": "e2", "source": "node_connector", "target": "node_exit",
            "type": "smoothstep", "data": {"businessType": "default"}
        })
    else:
        # ---- exit node (no connector) ----
        exit_node = {
            "id": "node_exit",
            "type": "exit",
            "position": {"x": 350, "y": 200},
            "data": {
                "labelCn": "返回",
                "labelEn": "Return",
                "outputMapping": exit_output_mapping or {
                    "body": {
                        "type": "object",
                        "properties": {
                            "echo": {"type": "string", "description": "回显", "value": "${$.node.node_entry.input.sender}"}
                        }
                    }
                }
            }
        }
        nodes.append(exit_node)
        edges.append({
            "id": "e1", "source": "node_entry", "target": "node_exit",
            "type": "smoothstep", "data": {"businessType": "default"}
        })

    orchestration = {
        "nodes": nodes,
        "edges": edges
    }

    subprocess.run([
        "mysql", "-uopenapp", "-popenapp", "openapp", "-e",
        f"INSERT INTO openplatform_v2_cp_flow_version_t "
        f"(id, flow_id, orchestration_config, "
        f"create_by, last_update_by) "
        f"VALUES ({version_id}, {snow_id_val}, "
        f"'{json.dumps(orchestration).replace(chr(39), chr(39)*2)}', "
        f"'tester', 'tester')"
    ], check=True, capture_output=True)

    return snow_id_val, version_id


def cleanup_flow(flow_id_val, version_id_val):
    subprocess.run(["mysql", "-uopenapp", "-popenapp", "openapp", "-e",
                    f"DELETE FROM openplatform_v2_cp_flow_version_t "
                    f"WHERE id = {version_id_val}"], capture_output=True)
    subprocess.run(["mysql", "-uopenapp", "-popenapp", "openapp", "-e",
                    f"DELETE FROM openplatform_v2_cp_flow_t "
                    f"WHERE id = {flow_id_val}"], capture_output=True)


BASE_TRIGGER = "http://localhost:18180/api/v1"


def _send_quiet(flow_id, idx):
    """静默发送触发请求（用于并发限流测试）"""
    try:
        resp = requests.post(
            f"{BASE_TRIGGER}/trigger/{flow_id}/invoke",
            json={"sender": f"test_{idx}"},
            headers={"Content-Type": "application/json",
                     "X-Sys-Token": "test-token"},
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
vid_049 = None
try:
    fid_049, vid_049 = setup_flow(sid_049, lifecycle_status=1,
                                  auth_type="SYSTOKEN")
    resp = request("POST", f"/trigger/{fid_049}/invoke",
                   {"sender": "test", "content": "hello"})
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
    if vid_049:
        cleanup_flow(sid_049, vid_049)


# ═══════════════════════════════════════════════════════════
# IT-050: Flow 不存在
# ═══════════════════════════════════════════════════════════
print("\n=== IT-050: Flow 不存在 ===")
resp = request("POST", "/trigger/999999999999999999/invoke",
               {"sender": "test", "content": "hello"},
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
vid_051 = None
try:
    fid_051, vid_051 = setup_flow(sid_051, lifecycle_status=0)
    resp = request("POST", f"/trigger/{fid_051}/invoke",
                   {"sender": "test", "content": "hello"},
                   headers={"X-Sys-Token": "test-token"})
    if resp:
        body = resp.json()
        check("HTTP 200", resp.status_code == 200)
        check("executionId 为 string",
              isinstance(body.get("executionId"), str))
finally:
    if vid_051:
        cleanup_flow(sid_051, vid_051)


# ═══════════════════════════════════════════════════════════
# IT-060: 快乐路径 — entry → connector → exit
# ═══════════════════════════════════════════════════════════
print("\n=== IT-060: 快乐路径（entry→connector→exit）===")
sid_060 = snow_id()
vid_060 = None
try:
    fid_060, vid_060 = setup_flow(sid_060, lifecycle_status=1,
                                  connector_url=f"{MOCK_BASE}/api/echo")
    resp = request("POST", f"/trigger/{fid_060}/invoke",
                   {"sender": "test_user", "content": "hello"},
                   headers={"X-Sys-Token": "test-token"})
    if resp:
        body = resp.json()
        check("HTTP 200", resp.status_code == 200)
        check("status 不为 failed", body.get("status") != "failed",
              f"status={body.get('status')}")
        check("executionId 为 string",
              isinstance(body.get("executionId"), str))
        check("totalDurationMs 为 int/float",
              isinstance(body.get("totalDurationMs"), (int, float)))

        # resultData
        rd = body.get("resultData")
        check("resultData 为 dict", isinstance(rd, dict))
        if isinstance(rd, dict):
            result_body = rd.get("body", {}) if isinstance(rd.get("body"), dict) else {}
            check("resultData.body.echo == test_user",
                  result_body.get("echo") == "test_user",
                  f"actual echo={result_body.get('echo')}, full body keys={list(result_body.keys())}")

        # steps
        steps = body.get("steps")
        check("steps 为 list", isinstance(steps, list))
        if isinstance(steps, list):
            check("steps 有 3 个条目", len(steps) == 3,
                  f"实际: {len(steps)}")

            # find connector step
            connector_step = None
            for s in steps:
                if s.get("nodeType") == "connector":
                    connector_step = s
                    break

            check("connector step 存在", connector_step is not None)
            if connector_step:
                check("connector step nodeType 为 connector",
                      connector_step.get("nodeType") == "connector")
                check("connector step inputData 存在",
                      "inputData" in connector_step
                      or connector_step.get("inputData") is not None)
                check("connector step outputData 存在",
                      "outputData" in connector_step
                      or connector_step.get("outputData") is not None)
                check("connector step status 为 success",
                      connector_step.get("status") == "success",
                      f"status={connector_step.get('status')}")
                # connector output should contain echo from mock server
                out = connector_step.get("outputData", {})
                if isinstance(out, dict):
                    check("connector output 含 echo",
                          "echo" in out or any(
                              isinstance(v, dict) and "user" in v
                              for v in out.values()
                          ))
finally:
    if vid_060:
        cleanup_flow(sid_060, vid_060)


# ═══════════════════════════════════════════════════════════
# IT-061: 请求体不符合 inputContract
# ═══════════════════════════════════════════════════════════
print("\n=== IT-061: 触发请求体不符合 inputContract（缺必填 sender）===")
sid_061 = snow_id()
vid_061 = None
try:
    fid_061, vid_061 = setup_flow(sid_061, lifecycle_status=1,
                                  input_contract_body_required=["sender"])
    resp = request("POST", f"/trigger/{fid_061}/invoke",
                   {"content": "hello"},
                   headers={"X-Sys-Token": "test-token"})
    if resp:
        body = resp.json()
        check("HTTP 200", resp.status_code == 200)
        check("status 为 failed 或 errorInfo 存在",
              body.get("status") == "failed"
              or "errorInfo" in body)
finally:
    if vid_061:
        cleanup_flow(sid_061, vid_061)


# ═══════════════════════════════════════════════════════════
# IT-062: Connector 下游失败 (500)
# ═══════════════════════════════════════════════════════════
print("\n=== IT-062: Connector 下游失败（mock /api/fail → 500）===")
sid_062 = snow_id()
vid_062 = None
try:
    fid_062, vid_062 = setup_flow(sid_062, lifecycle_status=1,
                                  connector_url=f"{MOCK_BASE}/api/fail")
    resp = request("POST", f"/trigger/{fid_062}/invoke",
                   {"sender": "test_user"},
                   headers={"X-Sys-Token": "test-token"})
    if resp:
        body = resp.json()
        check("HTTP 200", resp.status_code == 200,
              f"实际: {resp.status_code}")
        check("status 为 failed", body.get("status") == "failed",
              f"status={body.get('status')}")
        check("errorInfo 存在", "errorInfo" in body)

        # steps 中 connector node 应为 failed
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
                      "errorInfo" in connector_step
                      or connector_step.get("errorInfo") is not None)
finally:
    if vid_062:
        cleanup_flow(sid_062, vid_062)


# ═══════════════════════════════════════════════════════════
# IT-063: 表达式引用链验证
# ═══════════════════════════════════════════════════════════
print("\n=== IT-063: 表达式引用链验证（constant: + $. 引用混合）===")
sid_063 = snow_id()
vid_063 = None
try:
    fid_063, vid_063 = setup_flow(
        sid_063, lifecycle_status=1,
        connector_url=f"{MOCK_BASE}/api/echo",
        connector_input_mapping={
            "body": {
                "type": "object",
                "properties": {
                    "fullName": {"type": "string", "description": "姓名", "value": "${$.constant:John Doe}"},
                    "userId": {"type": "string", "description": "用户ID", "value": "${$.node.node_entry.input.sender}"}
                }
            }
        },
        exit_output_mapping={
            "body": {
                "type": "object",
                "properties": {
                    "echoFullName": {"type": "string", "description": "回显姓名", "value": "${$.node.node_connector.output.echo.fullName}"},
                    "echoUserId": {"type": "string", "description": "回显用户ID", "value": "${$.node.node_connector.output.echo.userId}"}
                }
            }
        }
    )
    resp = request("POST", f"/trigger/{fid_063}/invoke",
                   {"sender": "user_123"},
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
                body_input = input_data.get("body", {}) if isinstance(input_data, dict) else {}

                check("connector inputData.body.userId == user_123",
                      body_input.get("userId") == "user_123",
                      f"userId={body_input.get('userId')}")
                check("connector inputData.body.fullName == John Doe",
                      body_input.get("fullName") == "John Doe",
                      f"fullName={body_input.get('fullName')}")

                output_data = connector_step.get("outputData", {})
                check("connector output 含 echo 对象",
                      isinstance(output_data, dict)
                      and "echo" in output_data,
                      f"output keys: {list(output_data.keys()) if isinstance(output_data, dict) else type(output_data)}")

        rd = body.get("resultData")
        if isinstance(rd, dict):
            result_body = rd.get("body", {}) if isinstance(rd.get("body"), dict) else {}
            check("exit resultData.body 存在 echo 字段",
                  bool(result_body.get("echoFullName") or result_body.get("echoUserId")),
                  f"echoFullName={result_body.get('echoFullName')}, echoUserId={result_body.get('echoUserId')}")
finally:
    if vid_063:
        cleanup_flow(sid_063, vid_063)


# ═══════════════════════════════════════════════════════════
# IT-064: 超过限流阈值 → 429
#
# 已知限制：RateLimitFilter.getTriggerConfig() 读取
# config["trigger"]，但实际编排配置中节点存储在
# config["nodes"] 下，type="entry"。因此 RateLimitFilter
# 可能无法正确读取 data.rateLimitConfig.maxQps。
# 限流测试结果取决于具体实现是否兼容此配置结构。
# ═══════════════════════════════════════════════════════════
print("\n=== IT-064: 超过限流阈值（maxQps=5，并发 10 请求）===")
sid_064 = snow_id()
vid_064 = None
try:
    fid_064, vid_064 = setup_flow(sid_064, lifecycle_status=1,
                                  rate_limit_qps=5,
                                  connector_url=f"{MOCK_BASE}/api/echo")
    statuses = []
    with ThreadPoolExecutor(max_workers=10) as executor:
        futures = [executor.submit(_send_quiet, fid_064, i) for i in range(10)]
        for f in as_completed(futures):
            s = f.result()
            if s is not None and s > 0:
                statuses.append(s)

    check("至少发送了 10 个请求", len(statuses) == 10,
           f"实际: {len(statuses)}")
    count_429 = sum(1 for s in statuses if s == 429)
    check("至少 1 个 429 响应", count_429 >= 1,
           f"429={count_429}, 其他={len(statuses)-count_429}")


    # ── IT-065: 限流阈值内正常执行（复用同一 flow）──────
    print("\n=== IT-065: 限流阈值内正常执行（单次请求）===")
    time.sleep(1.5)
    resp = request("POST", f"/trigger/{fid_064}/invoke",
                   {"sender": "test_single"},
                   headers={"X-Sys-Token": "test-token"})
    if resp:
        body = resp.json()
        check("HTTP 200", resp.status_code == 200)
        check("executionId 存在", bool(body.get("executionId")))
finally:
    if vid_064:
        cleanup_flow(sid_064, vid_064)


# ═══════════════════════════════════════════════════════════
# Shutdown mock server
# ═══════════════════════════════════════════════════════════
mock_server.shutdown()
print("\nMock server shut down.")
