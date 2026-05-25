#!/usr/bin/env python3
"""获取编排配置 (IT-042~043)"""
from client import *
import subprocess, time

snow_id = int(time.time() * 1000000) % 100000000000000000
subprocess.run([
    "mysql", "-uopenapp", "-popenapp", "openapp", "-e",
    f"INSERT INTO openplatform_v2_cp_flow_t (id, name_cn, name_en, lifecycle_status, create_by, last_update_by) VALUES ({snow_id}, 'IT_编排配置测试', 'IT_FlowConfig', 0, 'tester', 'tester')"
], capture_output=True)

try:
    print("=== IT-042: 空配置（初始）===")
    request("GET", f"/flows/{snow_id}/config")

    print("=== IT-043: flowId 不存在 ===")
    request("GET", "/flows/999999999999999999/config")
finally:
    subprocess.run(["mysql", "-uopenapp", "-popenapp", "openapp", "-e",
                    f"DELETE FROM openplatform_v2_cp_flow_t WHERE id = {snow_id}"], capture_output=True)
