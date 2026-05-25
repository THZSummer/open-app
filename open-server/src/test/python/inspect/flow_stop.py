#!/usr/bin/env python3
"""停止流 (IT-039~041)"""
from client import *
import subprocess, time

snow_id = int(time.time() * 1000000) % 100000000000000000
subprocess.run([
    "mysql", "-uopenapp", "-popenapp", "openapp", "-e",
    f"INSERT INTO openplatform_v2_cp_flow_t (id, name_cn, name_en, lifecycle_status, create_by, last_update_by) VALUES ({snow_id}, 'IT_流停止测试', 'IT_FlowStop', 0, 'tester', 'tester')"
], capture_output=True)

try:
    # 先启动
    request("POST", f"/flows/{snow_id}/start")

    print("=== IT-039: 停止流 ===")
    request("POST", f"/flows/{snow_id}/stop")

    print("=== IT-040: 重复停止 ===")
    request("POST", f"/flows/{snow_id}/stop")

    print("=== IT-041: flowId 不存在 ===")
    request("POST", "/flows/999999999999999999/stop")
finally:
    subprocess.run(["mysql", "-uopenapp", "-popenapp", "openapp", "-e",
                    f"DELETE FROM openplatform_v2_cp_flow_t WHERE id = {snow_id}"], capture_output=True)
