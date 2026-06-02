#!/usr/bin/env python3
"""测试运行 (IT-047~048)"""
from client import *
import subprocess, time

print("=== IT-047: flow 不存在 ===")
request("POST", "/flows/999999999999999999/test-run", {"mockTriggerData": {}})

snow_id = int(time.time() * 1000000) % 100000000000000000
subprocess.run([
    "mysql", "-uopenapp", "-popenapp", "openapp", "-e",
    f"INSERT INTO openplatform_v2_cp_flow_t (id, name_cn, name_en, lifecycle_status, create_by, last_update_by) VALUES ({snow_id}, 'IT_\u8c03\u8bd5\u6d4b\u8bd5', 'IT_DebugTest', 0, 'tester', 'tester')"
], capture_output=True)

try:
    print("=== IT-048: \u672a\u914d\u7f6e\u7f16\u6392 ===")
    request("POST", f"/flows/{snow_id}/test-run", {"mockTriggerData": {"message": "hello"}})
finally:
    subprocess.run(["mysql", "-uopenapp", "-popenapp", "openapp", "-e",
                    f"DELETE FROM openplatform_v2_cp_flow_t WHERE id = {snow_id}"], capture_output=True)
