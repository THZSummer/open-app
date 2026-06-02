#!/usr/bin/env python3
"""启动流 (IT-036~038)"""
from client import *
import subprocess, time

snow_id = int(time.time() * 1000000) % 100000000000000000
subprocess.run([
    "mysql", "-uopenapp", "-popenapp", "openapp", "-e",
    f"INSERT INTO openplatform_v2_cp_flow_t (id, name_cn, name_en, lifecycle_status, create_by, last_update_by) VALUES ({snow_id}, 'IT_流启动测试', 'IT_FlowStart', 0, 'tester', 'tester')"
], capture_output=True)

try:
    print("=== IT-036: 启动流 ===")
    request("POST", f"/flows/{snow_id}/start")

    print("=== IT-037: 重复启动 ===")
    request("POST", f"/flows/{snow_id}/start")

    print("=== IT-038: flowId 不存在 ===")
    request("POST", "/flows/999999999999999999/start")
finally:
    subprocess.run(["mysql", "-uopenapp", "-popenapp", "openapp", "-e",
                    f"DELETE FROM openplatform_v2_cp_flow_t WHERE id = {snow_id}"], capture_output=True)
