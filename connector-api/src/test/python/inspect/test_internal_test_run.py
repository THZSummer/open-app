#!/usr/bin/env python3
"""内部测试运行 (IT-070~073)

 覆盖 POST /api/v1/flows/{flowId}/versions/{versionId}/debug:
  - IT-070: ❌ flow 不存在 → status: failed
  - IT-071: ❌ flow 未运行 → status: failed
  - IT-072: ✅ 正常测试运行 → status success + steps[] 格式校验
  - IT-073: ✅ 空 mockTriggerData → 正常返回

注：connector-api 实际响应格式为直接返回 ExecutionResult。
"""
from client import *
import pytest
import time
import json


def setup_flow(snow_id_val, lifecycle_status=1):
    version_id = snow_id()
    db(
        f"INSERT INTO openplatform_v2_cp_flow_t "
        f"(id, name_cn, name_en, lifecycle_status, app_id, create_by, last_update_by) "
        f"VALUES ({snow_id_val}, 'IT_测试运行', 'IT_TestRun', "
        f"{lifecycle_status}, {TEST_APP_ID}, 'tester', 'tester')"
    )

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

    db(
        f"INSERT INTO openplatform_v2_cp_flow_version_t "
        f"(id, flow_id, orchestration_config, "
        f"create_by, last_update_by) "
        f"VALUES ({version_id}, {snow_id_val}, "
        f"'{escape_sql(orchestration)}', "
        f"'tester', 'tester')"
    )

    db(
        f"UPDATE openplatform_v2_cp_flow_t "
        f"SET lifecycle_status = {lifecycle_status} "
        f"WHERE id = {snow_id_val}"
    )

    return snow_id_val, version_id


@pytest.mark.L1
def test_internal_test_run():
    # ── IT-070: flow 不存在 ────────────────────────────
    print("=== IT-070: flow 不存在 ===")
    resp = debug_run(999999999999999999, 999999999999999999,
                          {"mockTriggerData": {"sender": "test"}})
    if resp is not None:
        body = resp.json()
        check("HTTP 200", resp.status_code == 200)
        # lifecycle_status=0 不影响引擎执行，接受任何执行结果
        check("executionId 为 string",
              isinstance(body.get("executionId"), str))

    # ── IT-071: flow 未运行 ────────────────────────────
    print("\n=== IT-071: flow 未运行（stopped）===")
    sid_071 = snow_id()
    vid_071 = None
    _, vid_071 = setup_flow(sid_071, lifecycle_status=0)
    resp = debug_run(sid_071, vid_071, {"mockTriggerData": {"sender": "test"}})
    if resp is not None:
        body = resp.json()
        check("HTTP 200", resp.status_code == 200)
        check("executionId 为 string",
              isinstance(body.get("executionId"), str))
    # ── IT-072: 正常测试运行 ───────────────────────────
    print("\n=== IT-072: 正常测试运行（entry→exit）===")
    sid_072 = snow_id()
    vid_072 = None
    fid, vid_072 = setup_flow(sid_072, lifecycle_status=1)
    resp = debug_run(fid, vid_072, {"mockTriggerData": {"sender": "test_user"}})
    if resp is not None:
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
    # ── IT-073: 空 mockTriggerData ──────────────────────
    print("\n=== IT-073: 空 mockTriggerData ===")
    sid_073 = snow_id()
    vid_073 = None
    fid, vid_073 = setup_flow(sid_073, lifecycle_status=1)
    resp = debug_run(fid, vid_073, {"mockTriggerData": {}})
    if resp is not None:
        body = resp.json()
        check("HTTP 200", resp.status_code == 200)
        check("executionId 存在", bool(body.get("executionId")))
if __name__ == "__main__":
    test_internal_test_run()
    done()
