#!/usr/bin/env python3
"""删除流 (IT-034~035)"""
from client import *
import subprocess, time

snow_id = int(time.time() * 1000000) % 100000000000000000
subprocess.run([
    "mysql", "-uopenapp", "-popenapp", "openapp", "-e",
    f"INSERT INTO openplatform_v2_cp_flow_t (id, name_cn, name_en, lifecycle_status, create_by, last_update_by) VALUES ({snow_id}, 'IT_流删除测试', 'IT_FlowDelete', 0, 'tester', 'tester')"
], capture_output=True)

print("=== IT-034: 删除流 ===")
resp = request("DELETE", f"/service/open/v2/flows/{snow_id}")

print("=== IT-035: flowId 不存在 ===")
request("DELETE", "/service/open/v2/flows/999999999999999999")
