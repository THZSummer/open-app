#!/usr/bin/env python3
"""查询流详情 (IT-030~031)"""
from client import *
import subprocess, time

snow_id = int(time.time() * 1000000) % 100000000000000000
subprocess.run([
    "mysql", "-h192.168.3.155", "-uopenapp", "-popenapp", "openapp", "-e",
    f"INSERT INTO openplatform_v2_cp_flow_t (id, name_cn, name_en, lifecycle_status, create_by, last_update_by) VALUES ({snow_id}, 'IT_流详情测试', 'IT_FlowDetail', 0, 'tester', 'tester')"
], capture_output=True)

try:
    print("=== IT-030: 正常查询 ===")
    request("GET", f"/service/open/v2/flows/{snow_id}")

    print("=== IT-031: flowId 不存在 ===")
    request("GET", "/service/open/v2/flows/999999999999999999")
finally:
    subprocess.run(["mysql", "-h192.168.3.155", "-uopenapp", "-popenapp", "openapp", "-e",
                    f"DELETE FROM openplatform_v2_cp_flow_t WHERE id = {snow_id}"], capture_output=True)
