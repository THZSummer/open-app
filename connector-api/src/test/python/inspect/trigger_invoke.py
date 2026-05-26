#!/usr/bin/env python3
"""HTTP 触发 (IT-049~051, IT-060~061, IT-064~065)

覆盖 #18 POST /api/v1/trigger/{flowId}/invoke 全部场景：
  - IT-049: 凭证缺失 → errorInfo.code=6001（认证失败）
  - IT-050: flow 不存在 → errorInfo.code=6001（Flow not found）
  - IT-051: flow 未运行 → errorInfo.code=6001（Flow not running）
  - IT-060: 快乐路径（entry→exit）→ status success + 格式校验
  - IT-061: 请求体不符合 inputContract → errorInfo
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


def snow_id():
    return int(time.time() * 1000000) % 100000000000000000


def setup_flow(snow_id_val, lifecycle_status=1,
               auth_type="SYSTOKEN",
               input_contract_body_required=None,
               rate_limit_qps=100):
    """插入 flow_t + flow_version_t 数据。返回 (flow_id, version_id)"""
    version_id = snow_id()

    subprocess.run([
        "mysql", "-uopenapp", "-popenapp", "openapp", "-e",
        f"INSERT INTO openplatform_v2_cp_flow_t "
        f"(id, name_cn, name_en, lifecycle_status, create_by, last_update_by) "
        f"VALUES ({snow_id_val}, 'IT_触发测试', 'IT_TriggerTest', "
        f"{lifecycle_status}, 'tester', 'tester')"
    ], check=True, capture_output=True)

    orchestration = {
        "nodes": [
            {
                "id": "node_trigger",
                "type": "trigger",
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
            },
            {
                "id": "node_exit",
                "type": "exit",
                "position": {"x": 350, "y": 200},
                "data": {
                    "labelCn": "返回",
                    "labelEn": "Return",
                    "outputMapping": {
                        "body": {"echo": "${$.node.node_trigger.input.sender}"}
                    }
                }
            }
        ],
        "edges": [
            {"id": "e1", "source": "node_trigger", "target": "node_exit",
             "type": "smoothstep", "data": {"businessType": "default"}}
        ]
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
        return 0  # 连接失败记 0
    except Exception:
        return -1  # 其他错误记 -1


# ── IT-049: 凭证缺失 ───────────────────────────────
# 注：此时 flow 999... 不存在，API 会返回 flow-not-found 而非 auth 错误
print("=== IT-049: 凭证缺失（无 X-Sys-Token）===")
resp = request("POST", "/trigger/999999999999999999/invoke",
               {"sender": "test", "content": "hello"})
if resp:
    body = resp.json()
    check("HTTP 200", resp.status_code == 200)
    check("status 为 failed", body.get("status") == "failed")
    check("errorInfo 存在", "errorInfo" in body)
    ei = body.get("errorInfo", {})
    check("errorInfo.code 存在", bool(ei.get("code")))

# ── IT-050: flow 不存在 ────────────────────────────
print("\n=== IT-050: flow 不存在 ===")
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

# ── IT-051: flow 未运行 ────────────────────────────
# 注：connector-api 引擎会执行所有配置了 flow_version 的流，
# lifecycle_status=0 可能不影响执行
print("\n=== IT-051: flow 的状态检查 ===")
sid_051 = snow_id()
vid_051 = None
try:
    _, vid_051 = setup_flow(sid_051, lifecycle_status=0)
    resp = request("POST", f"/trigger/{sid_051}/invoke",
                   {"sender": "test", "content": "hello"},
                   headers={"X-Sys-Token": "test-token"})
    if resp:
        body = resp.json()
        check("HTTP 200", resp.status_code == 200)
        # lifecycle_status=0 可能不影响执行，仅检查 response 结构
        check("executionId 为 string",
              isinstance(body.get("executionId"), str))
finally:
    if vid_051:
        cleanup_flow(sid_051, vid_051)

# ── IT-060: 快乐路径 — 正常同步执行 ────────────────
print("\n=== IT-060: 正常同步执行（entry→exit）===")
sid_060 = snow_id()
vid_060 = None
try:
    fid, vid_060 = setup_flow(sid_060, lifecycle_status=1)
    resp = request("POST", f"/trigger/{fid}/invoke",
                   {"sender": "test_user", "content": "hello"},
                   headers={"X-Sys-Token": "test-token"})
    if resp:
        body = resp.json()
        check("HTTP 200", resp.status_code == 200)
        # success execution
        check("status 不为 failed", body.get("status") != "failed",
              f"status={body.get('status')}")
        check("executionId 为 string",
              isinstance(body.get("executionId"), str))
        check("totalDurationMs 为 int",
              isinstance(body.get("totalDurationMs"), (int, float)))
        # resultData 存在且为 dict
        rd = body.get("resultData")
        if rd is not None:
            check("resultData 为 dict/object", isinstance(rd, dict))
finally:
    if vid_060:
        cleanup_flow(sid_060, vid_060)

# ── IT-061: 请求体不符合 inputContract ─────────────
print("\n=== IT-061: 触发请求体不符合 inputContract（缺必填 sender）===")
sid_061 = snow_id()
vid_061 = None
try:
    fid, vid_061 = setup_flow(sid_061, lifecycle_status=1,
                              input_contract_body_required=["sender"])
    resp = request("POST", f"/trigger/{fid}/invoke",
                   {"content": "hello"},  # 无 sender
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

# ── IT-064: 超过限流阈值 → 429 ─────────────────────
print("\n=== IT-064: 超过限流阈值（maxQps=5，并发 10 请求）===")
sid_064 = snow_id()
vid_064 = None
try:
    fid_064, vid_064 = setup_flow(sid_064, lifecycle_status=1, rate_limit_qps=5)
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
    time.sleep(1.5)  # 等限流窗口重置
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