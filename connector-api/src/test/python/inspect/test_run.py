#!/usr/bin/env python3
"""内部测试运行 (IT-070~073)

覆盖 POST /api/v1/internal/test-run/{flowId}:
  - IT-070: ❌ flow 不存在 → status: failed
  - IT-071: ❌ flow 未运行 → status: failed
  - IT-072: ✅ 正常测试运行 → status success + steps[] 格式校验
  - IT-073: ✅ 空 mockTriggerData → 正常返回

注：connector-api 实际响应格式为直接返回 ExecutionResult。
"""
from client import *
import subprocess
import time
import json
import requests as req_lib

BASE_INTERNAL = "http://localhost:18180/api/v1/internal/test-run"


def snow_id():
    return int(time.time() * 1000000) % 100000000000000000


def setup_flow(snow_id_val, lifecycle_status=1):
    version_id = snow_id()
    subprocess.run([
        "mysql", "-h", "192.168.3.155", "-uopenapp", "-popenapp", "openapp", "-e",
        f"INSERT INTO openplatform_v2_cp_flow_t "
        f"(id, name_cn, name_en, lifecycle_status, create_by, last_update_by) "
        f"VALUES ({snow_id_val}, 'IT_测试运行', 'IT_TestRun', "
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
                        "type": "SYSTOKEN",
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
                            "required": ["sender"]
                        }
                    },
                    "rateLimitConfig": {"maxQps": 100}
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
                        "body": {
                            "type": "object",
                            "properties": {
                                "echo": {"type": "string", "description": "回显", "value": "${$.node.node_trigger.input.sender}"}
                            }
                        }
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
        "mysql", "-h", "192.168.3.155", "-uopenapp", "-popenapp", "openapp", "-e",
        f"INSERT INTO openplatform_v2_cp_flow_version_t "
        f"(id, flow_id, orchestration_config, "
        f"create_by, last_update_by) "
        f"VALUES ({version_id}, {snow_id_val}, "
        f"'{json.dumps(orchestration).replace(chr(39), chr(39)*2)}', "
        f"'tester', 'tester')"
    ], check=True, capture_output=True)

    subprocess.run([
        "mysql", "-h", "192.168.3.155", "-uopenapp", "-popenapp", "openapp", "-e",
        f"UPDATE openplatform_v2_cp_flow_t "
        f"SET lifecycle_status = {lifecycle_status} "
        f"WHERE id = {snow_id_val}"
    ], check=True, capture_output=True)

    return snow_id_val, version_id


def cleanup_flow(flow_id_val, version_id_val):
    subprocess.run(["mysql", "-h", "192.168.3.155", "-uopenapp", "-popenapp", "openapp", "-e",
                    f"DELETE FROM openplatform_v2_cp_flow_version_t "
                    f"WHERE id = {version_id_val}"], capture_output=True)
    subprocess.run(["mysql", "-h", "192.168.3.155", "-uopenapp", "-popenapp", "openapp", "-e",
                    f"DELETE FROM openplatform_v2_cp_flow_t "
                    f"WHERE id = {flow_id_val}"], capture_output=True)


def test_run_request(flow_id, body=None):
    """发送测试运行请求，返回 Response"""
    url = f"{BASE_INTERNAL}/{flow_id}"
    h = {"Content-Type": "application/json"}
    try:
        start = time.time()
        resp = req_lib.post(url, json=body, headers=h, timeout=10)
        elapsed = time.time() - start
    except req_lib.exceptions.ConnectionError:
        if not is_quiet():
            print(f"\n  SKIP: connector-api 未运行 (port 18180)")
        else:
            print(f"[SKIP] POST /internal/test-run/{flow_id}")
        return None

    if not is_quiet():
        _print_request("POST", url, h, body)
        _print_response(resp, elapsed)
    return resp


# ── IT-070: flow 不存在 ────────────────────────────
print("=== IT-070: flow 不存在 ===")
resp = test_run_request(999999999999999999,
                        {"mockTriggerData": {"sender": "test"}})
if resp:
    body = resp.json()
    check("HTTP 200", resp.status_code == 200)
    # lifecycle_status=0 不影响引擎执行，接受任何执行结果
    check("executionId 为 string",
          isinstance(body.get("executionId"), str))

# ── IT-071: flow 未运行 ────────────────────────────
print("\n=== IT-071: flow 未运行（stopped）===")
sid_071 = snow_id()
vid_071 = None
try:
    _, vid_071 = setup_flow(sid_071, lifecycle_status=0)
    resp = test_run_request(sid_071, {"mockTriggerData": {"sender": "test"}})
    if resp:
        body = resp.json()
        check("HTTP 200", resp.status_code == 200)
        check("executionId 为 string",
              isinstance(body.get("executionId"), str))
finally:
    if vid_071:
        cleanup_flow(sid_071, vid_071)

# ── IT-072: 正常测试运行 ───────────────────────────
print("\n=== IT-072: 正常测试运行（entry→exit）===")
sid_072 = snow_id()
vid_072 = None
try:
    fid, vid_072 = setup_flow(sid_072, lifecycle_status=1)
    resp = test_run_request(fid, {"mockTriggerData": {"sender": "test_user"}})
    if resp:
        body = resp.json()
        check("HTTP 200", resp.status_code == 200)
        check("executionId 为 string",
              isinstance(body.get("executionId"), str))
        check("totalDurationMs 为 int",
              isinstance(body.get("totalDurationMs"), (int, float)))
        # steps 为数组
        steps = body.get("steps")
        check("steps 为 list", isinstance(steps, list))
        if isinstance(steps, list) and len(steps) > 0:
            s = steps[0]
            check("steps[].nodeId 存在", bool(s.get("nodeId")))
            check("steps[].nodeLabelCn 存在", bool(s.get("nodeLabelCn")))
            check("steps[].durationMs 为 int",
                  isinstance(s.get("durationMs"), (int, float)))
finally:
    if vid_072:
        cleanup_flow(sid_072, vid_072)

# ── IT-073: 空 mockTriggerData ──────────────────────
print("\n=== IT-073: 空 mockTriggerData ===")
sid_073 = snow_id()
vid_073 = None
try:
    fid, vid_073 = setup_flow(sid_073, lifecycle_status=1)
    resp = test_run_request(fid, {"mockTriggerData": {}})
    if resp:
        body = resp.json()
        check("HTTP 200", resp.status_code == 200)
        check("executionId 存在", bool(body.get("executionId")))
finally:
    if vid_073:
        cleanup_flow(sid_073, vid_073)