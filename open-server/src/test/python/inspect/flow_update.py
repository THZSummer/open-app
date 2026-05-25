#!/usr/bin/env python3
"""更新流 (IT-032~033)"""
from client import *
import subprocess, time

snow_id = int(time.time() * 1000000) % 100000000000000000
subprocess.run([
    "mysql", "-uopenapp", "-popenapp", "openapp", "-e",
    f"INSERT INTO openplatform_v2_cp_flow_t (id, name_cn, name_en, lifecycle_status, create_by, last_update_by) VALUES ({snow_id}, 'IT_流更新测试', 'IT_FlowUpdate', 0, 'tester', 'tester')"
], capture_output=True)

try:
    print("=== IT-032: 正常更新 ===")
    request("PUT", f"/flows/{snow_id}", {"nameCn": "更新后的流名称"})

    print("=== IT-033: flowId 不存在 ===")
    request("PUT", "/flows/999999999999999999", {"nameCn": "测试"})
finally:
    subprocess.run(["mysql", "-uopenapp", "-popenapp", "openapp", "-e",
                    f"DELETE FROM openplatform_v2_cp_flow_t WHERE id = {snow_id}"], capture_output=True)
