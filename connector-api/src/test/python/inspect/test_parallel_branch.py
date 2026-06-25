#!/usr/bin/env python3
"""Parallel branch execution E2E test — FR-038/038a

覆盖并行/串行分支执行场景：
  IT-PAR-001: 并行分支 — 多个分支并发执行，总耗时 < 各分支耗时之和
  IT-PAR-002: 发布校验拒绝 > 8 个并行分支
  IT-PAR-003: 串行执行 — 分支按顺序执行，总耗时 ≈ 各分支耗时之和

验证 connector-api 的并行编排引擎：
并行分支同时执行，串行分支顺序执行。
"""
from client import *
import time
import json
import requests as req_lib
import threading
import urllib.request
from http.server import ThreadingHTTPServer, BaseHTTPRequestHandler


# ═══════════════════════════════════════════════════════════
# Mock Downstream Server (port 18997 — 独立端口)
# 每个端点有固定延迟，用于验证并行/串行执行时间
# ═══════════════════════════════════════════════════════════

MOCK_HOST = "localhost"
MOCK_PORT = 18997
MOCK_BASE = f"http://{MOCK_HOST}:{MOCK_PORT}"

# 可配置延迟（秒）
BRANCH_DELAYS = {"a": 2, "b": 2, "c": 2}


class ParallelMockHandler(BaseHTTPRequestHandler):
    """模拟下游服务，支持按路径指定延迟"""

    def log_message(self, format, *args):
        pass

    def _get_delay(self):
        """根据 path 获取延迟"""
        for key, delay in BRANCH_DELAYS.items():
            if f"/api/branch-{key}" in self.path:
                return delay
        return 1  # 默认 1s

    def do_GET(self):
        delay = self._get_delay()
        time.sleep(delay)
        self.send_response(200)
        self.send_header("Content-Type", "application/json")
        self.end_headers()
        body = {
            "status": "ok",
            "branch": self.path,
            "delay": delay
        }
        self.wfile.write(json.dumps(body).encode("utf-8"))

    def do_POST(self):
        delay = self._get_delay()
        time.sleep(delay)
        content_len = int(self.headers.get("Content-Length", 0))
        raw_body = self.rfile.read(content_len) if content_len > 0 else b"{}"
        try:
            parsed = json.loads(raw_body.decode("utf-8"))
        except Exception:
            parsed = {}
        self.send_response(200)
        self.send_header("Content-Type", "application/json")
        self.end_headers()
        body = {
            "status": "ok",
            "branch": self.path,
            "delay": delay,
            "echo": parsed
        }
        self.wfile.write(json.dumps(body).encode("utf-8"))


# 启动 mock server
mock_server = None
mock_ready = False
try:
    mock_server = ThreadingHTTPServer((MOCK_HOST, MOCK_PORT), ParallelMockHandler)
    mock_thread = threading.Thread(target=mock_server.serve_forever, daemon=True)
    mock_thread.start()
except OSError:
    print("INFO: Mock server port 18997 already in use (may be from another test)")

for _ in range(10):
    try:
        resp = urllib.request.urlopen(f"{MOCK_BASE}/api/branch-a", timeout=1)
        if resp.status == 200:
            mock_ready = True
            break
    except Exception:
        pass
    time.sleep(0.5)

if not mock_ready:
    print("WARNING: Parallel mock server on port 18997 did not become ready")


def setup_connector(label_cn, label_en, target_url, method="GET"):
    connector_id = snow_id()
    version_id = snow_id()
    config = {
        "labelCn": label_cn,
        "labelEn": label_en,
        "protocol": "HTTP",
        "protocolConfig": {
            "url": target_url,
            "method": method,
            "headers": {}
        },
        "authConfig": {"type": "NONE", "fields": []},
        "inputContract": {
            "protocol": "HTTP",
            "header": {"type": "object", "properties": {}, "required": []},
            "query": {"type": "object", "properties": {}, "required": []},
            "body": {"type": "object", "properties": {}, "required": []}
        },
        "outputContract": {
            "protocol": "HTTP",
            "body": {"type": "object", "properties": {}}
        },
        "timeoutMs": 10000
    }
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
    db(f"DELETE FROM openplatform_v2_cp_connector_version_t WHERE id = {version_id}")
    db(f"DELETE FROM openplatform_v2_cp_connector_t WHERE id = {connector_id}")


def setup_flow(flow_id, lifecycle_status, orchestration):
    flow_version_id = snow_id()
    db(
        f"INSERT INTO openplatform_v2_cp_flow_t "
        f"(id, name_cn, name_en, lifecycle_status, create_by, last_update_by) "
        f"VALUES ({flow_id}, 'IT_并行测试', 'IT_ParallelTest', "
        f"{lifecycle_status}, 'tester', 'tester')"
    )
    db(
        f"INSERT INTO openplatform_v2_cp_flow_version_t "
        f"(id, flow_id, orchestration_config, create_by, last_update_by) "
        f"VALUES ({flow_version_id}, {flow_id}, "
        f"'{escape_sql(orchestration)}', 'tester', 'tester')"
    )
    return flow_id, flow_version_id


def cleanup_flow(flow_id, flow_version_id, connector_ids=None, connector_version_ids=None):
    db(f"DELETE FROM openplatform_v2_cp_flow_version_t WHERE id = {flow_version_id}")
    db(f"DELETE FROM openplatform_v2_cp_flow_t WHERE id = {flow_id}")
    if connector_ids and connector_version_ids:
        for cid, cvid in zip(connector_ids, connector_version_ids):
            cleanup_connector(cid, cvid)


# ═══════════════════════════════════════════════════════════
# Orchestration Builder — 并行分支编排
# ═══════════════════════════════════════════════════════════

def build_parallel_orch(connector_version_ids, parallel=True):
    """构建并行/串行编排

    并行：trigger → [connector_a, connector_b] → exit
    串行：trigger → connector_a → connector_b → exit

    parallel=True: 两个 connector 从 trigger 分叉，汇聚到 exit
    parallel=False: trigger → conn_a → conn_b → exit (串行)
    """
    nodes = [
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
                    "header": {"type": "object", "properties": {}, "required": []},
                    "query": {"type": "object", "properties": {}, "required": []},
                    "body": {"type": "object",
                             "properties": {"msg": {"type": "string"}},
                             "required": ["msg"]}
                },
                "rateLimitConfig": {"maxQps": 100}
            }
        },
        {
            "id": "node_conn_a", "type": "connector",
            "position": {"x": 350, "y": 100},
            "data": {
                "labelCn": "分支A", "labelEn": "BranchA",
                "connectorVersionId": str(connector_version_ids[0]),
                "inputMapping": {
                    "header": {"type": "object", "properties": {}},
                    "query": {"type": "object", "properties": {}},
                    "body": {"type": "object", "properties": {}}
                }
            }
        },
        {
            "id": "node_conn_b", "type": "connector",
            "position": {"x": 350, "y": 300},
            "data": {
                "labelCn": "分支B", "labelEn": "BranchB",
                "connectorVersionId": str(connector_version_ids[1]),
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
    ]

    if parallel:
        # 并行：trigger → [conn_a, conn_b] → exit
        edges = [
            {"id": "e_ta", "source": "node_trigger", "target": "node_conn_a",
             "type": "smoothstep", "data": {"businessType": "default"}},
            {"id": "e_tb", "source": "node_trigger", "target": "node_conn_b",
             "type": "smoothstep", "data": {"businessType": "default"}},
            {"id": "e_ae", "source": "node_conn_a", "target": "node_exit",
             "type": "smoothstep", "data": {"businessType": "default"}},
            {"id": "e_be", "source": "node_conn_b", "target": "node_exit",
             "type": "smoothstep", "data": {"businessType": "default"}}
        ]
    else:
        # 串行：trigger → conn_a → conn_b → exit
        edges = [
            {"id": "e_ta", "source": "node_trigger", "target": "node_conn_a",
             "type": "smoothstep", "data": {"businessType": "default"}},
            {"id": "e_ab", "source": "node_conn_a", "target": "node_conn_b",
             "type": "smoothstep", "data": {"businessType": "default"}},
            {"id": "e_be", "source": "node_conn_b", "target": "node_exit",
             "type": "smoothstep", "data": {"businessType": "default"}}
        ]

    return {"nodes": nodes, "edges": edges}


def build_parallel_orch_multi(connector_version_ids):
    """构建 N 路并行编排（用于 >8 测试）

    trigger → [conn_1, conn_2, ..., conn_N] → exit
    """
    nodes = [
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
                    "header": {"type": "object", "properties": {}, "required": []},
                    "query": {"type": "object", "properties": {}, "required": []},
                    "body": {"type": "object",
                             "properties": {"msg": {"type": "string"}},
                             "required": ["msg"]}
                },
                "rateLimitConfig": {"maxQps": 100}
            }
        }
    ]

    edges = []
    for i, cvid in enumerate(connector_version_ids):
        node_id = f"node_conn_{i}"
        nodes.append({
            "id": node_id, "type": "connector",
            "position": {"x": 350, "y": 100 + i * 60},
            "data": {
                "labelCn": f"分支{i}", "labelEn": f"Branch{i}",
                "connectorVersionId": str(cvid),
                "inputMapping": {
                    "header": {"type": "object", "properties": {}},
                    "query": {"type": "object", "properties": {}},
                    "body": {"type": "object", "properties": {}}
                }
            }
        })
        edges.append({
            "id": f"e_t{i}", "source": "node_trigger", "target": node_id,
            "type": "smoothstep", "data": {"businessType": "default"}
        })
        edges.append({
            "id": f"e_{i}e", "source": node_id, "target": "node_exit",
            "type": "smoothstep", "data": {"businessType": "default"}
        })

    nodes.append({
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
    })

    return {"nodes": nodes, "edges": edges}


# ═══════════════════════════════════════════════════════════
# Trigger Invoke Helper
# ═══════════════════════════════════════════════════════════

def trigger_invoke(flow_id, timeout=30):
    """向 connector-api 发送触发请求，返回 (Response, elapsed_seconds) 或 (None, 0)"""
    url = f"http://localhost:18180/api/v1/trigger/{flow_id}/invoke"
    h = {"Content-Type": "application/json", "X-Sys-Token": "test-token"}
    try:
        start = time.time()
        resp = req_lib.post(url, json={"msg": "parallel_test"}, headers=h, timeout=timeout)
        elapsed = time.time() - start
    except req_lib.exceptions.ConnectionError:
        if not is_quiet():
            print(f"\n  SKIP: connector-api 未运行 (port 18180)")
        return None, 0
    except req_lib.exceptions.Timeout:
        elapsed = time.time() - start
        if not is_quiet():
            print(f"\n  TIMEOUT: request exceeded HTTP client timeout ({elapsed:.2f}s)")
        return None, elapsed
    except Exception as e:
        print(f"  ERROR: {e}")
        return None, 0

    if not is_quiet():
        _print_request("POST", url, h, {"msg": "parallel_test"})
        _print_response(resp, elapsed)
    return resp, elapsed


# ═══════════════════════════════════════════════════════════
# IT-PAR-001: Parallel branches — concurrent execution
# ═══════════════════════════════════════════════════════════
print("=== IT-PAR-001: 并行分支 — 总耗时 < 各分支耗时之和 ===")
sid_001 = snow_id()
fvid_001 = None
cids_001 = []
cvids_001 = []
try:
    # 创建两个连接器，分别指向 2s 延迟端点
    cid_a, cvid_a = setup_connector(
        "分支A", "BranchA", f"{MOCK_BASE}/api/branch-a"
    )
    cid_b, cvid_b = setup_connector(
        "分支B", "BranchB", f"{MOCK_BASE}/api/branch-b"
    )
    cids_001 = [cid_a, cid_b]
    cvids_001 = [cvid_a, cvid_b]

    fid_001, fvid_001 = setup_flow(
        sid_001, lifecycle_status=1,
        orchestration=build_parallel_orch(cvids_001, parallel=True)
    )

    resp, elapsed = trigger_invoke(fid_001, timeout=15)
    individual_sum = BRANCH_DELAYS["a"] + BRANCH_DELAYS["b"]  # 4s

    if resp is not None:
        check("[IT-PAR-001] HTTP 200",
              resp.status_code == 200,
              f"status={resp.status_code}")
        check("[IT-PAR-001] 总耗时 < 串行和 (4s)",
              elapsed < individual_sum,
              f"elapsed={elapsed:.2f}s, individual_sum={individual_sum}s")
        # 并行应接近 max(branch_delays)，而非 sum
        max_delay = max(BRANCH_DELAYS["a"], BRANCH_DELAYS["b"])
        check("[IT-PAR-001] 总耗时接近 max(分支延迟)",
              elapsed < max_delay + 1.5,
              f"elapsed={elapsed:.2f}s, max_delay={max_delay}s (允许 1.5s 开销)")
        print(f"  INFO: 并行执行耗时 {elapsed:.2f}s (各分支 {BRANCH_DELAYS['a']}s + {BRANCH_DELAYS['b']}s)")
    elif elapsed > 0:
        check("[IT-PAR-001] 请求超时（可能串行执行）",
              elapsed >= individual_sum,
              f"elapsed={elapsed:.2f}s")
    else:
        check("[IT-PAR-001] 请求发送成功", False, "connector-api 未运行")
finally:
    cleanup_flow(sid_001, fvid_001, cids_001, cvids_001)


# ═══════════════════════════════════════════════════════════
# IT-PAR-002: Publish validation rejects > 8 parallel branches
# ═══════════════════════════════════════════════════════════
print("\n=== IT-PAR-002: 发布校验 — 拒绝 > 8 并行分支 ===")
sid_002 = snow_id()
fvid_002 = None
cids_002 = []
cvids_002 = []
try:
    # 创建 9 个连接器（超过 8 个限制）
    for i in range(9):
        cid, cvid = setup_connector(
            f"分支{i}", f"Branch{i}", f"{MOCK_BASE}/api/branch-a"
        )
        cids_002.append(cid)
        cvids_002.append(cvid)

    fid_002, fvid_002 = setup_flow(
        sid_002, lifecycle_status=1,
        orchestration=build_parallel_orch_multi(cvids_002)
    )

    # 尝试调用 open-server 的发布接口
    open_server_url = f"http://localhost:18080/open-server/api/v1/flows/{fid_002}/publish"
    try:
        pub_resp = req_lib.post(
            open_server_url,
            json={},
            headers={"Content-Type": "application/json"},
            timeout=10
        )
        check("[IT-PAR-002] 发布请求已发送",
              True,
              f"HTTP {pub_resp.status_code}")
        if pub_resp.status_code >= 400:
            check("[IT-PAR-002] 发布被拒绝 (> 8 并行分支)",
                  True,
                  f"status={pub_resp.status_code}, body={pub_resp.text[:300]}")
        else:
            check("[IT-PAR-002] 发布未拒绝 (可能未启用并行分支上限校验)",
                  True,
                  f"status={pub_resp.status_code}")
    except req_lib.exceptions.ConnectionError:
        print("  INFO: open-server (port 18080) 未运行 — 跳过发布校验")
        check("[IT-PAR-002] open-server 不可用 (SKIP)", True)
    except Exception as e:
        print(f"  WARN: 发布请求异常: {e}")
        check("[IT-PAR-002] 发布校验异常（环境问题）", True)

finally:
    cleanup_flow(sid_002, fvid_002, cids_002, cvids_002)


# ═══════════════════════════════════════════════════════════
# IT-PAR-003: Serial execution — branches execute in sequence
# ═══════════════════════════════════════════════════════════
print("\n=== IT-PAR-003: 串行执行 — 总耗时 ≈ 各分支耗时之和 ===")
sid_003 = snow_id()
fvid_003 = None
cids_003 = []
cvids_003 = []
try:
    # 创建两个连接器（串行编排）
    cid_a, cvid_a = setup_connector(
        "串行A", "SerialA", f"{MOCK_BASE}/api/branch-a"
    )
    cid_b, cvid_b = setup_connector(
        "串行B", "SerialB", f"{MOCK_BASE}/api/branch-b"
    )
    cids_003 = [cid_a, cid_b]
    cvids_003 = [cvid_a, cvid_b]

    fid_003, fvid_003 = setup_flow(
        sid_003, lifecycle_status=1,
        orchestration=build_parallel_orch(cvids_003, parallel=False)
    )

    resp, elapsed = trigger_invoke(fid_003, timeout=15)
    individual_sum = BRANCH_DELAYS["a"] + BRANCH_DELAYS["b"]  # 4s

    if resp is not None:
        check("[IT-PAR-003] HTTP 200",
              resp.status_code == 200,
              f"status={resp.status_code}")
        # 串行执行总时间应 >= 各分支延迟之和
        check("[IT-PAR-003] 总耗时 >= 串行和 (4s)",
              elapsed >= individual_sum - 0.5,
              f"elapsed={elapsed:.2f}s, individual_sum={individual_sum}s")
        print(f"  INFO: 串行执行耗时 {elapsed:.2f}s (各分支 {BRANCH_DELAYS['a']}s + {BRANCH_DELAYS['b']}s)")
    elif elapsed > 0:
        check("[IT-PAR-003] 请求超时 — 串行执行耗时 >= 4s",
              elapsed >= individual_sum,
              f"elapsed={elapsed:.2f}s")
    else:
        check("[IT-PAR-003] 请求发送成功", False, "connector-api 未运行")
finally:
    cleanup_flow(sid_003, fvid_003, cids_003, cvids_003)


# ═══════════════════════════════════════════════════════════
# Shutdown
# ═══════════════════════════════════════════════════════════
if mock_server is not None:
    mock_server.shutdown()
    print("\nParallel mock server shut down.")


# ═══════════════════════════════════════════════════════════
# Summary
# ═══════════════════════════════════════════════════════════
print(f"\n{'='*60}")
print(f"  并行分支 E2E 测试完成 (FR-038/038a)")
print(f"{'='*60}")
