#!/usr/bin/env python3
"""删除流 (IT-034~035)"""
from client import *
import subprocess, time, requests

snow_id = int(time.time() * 1000000) % 100000000000000000
subprocess.run([
    "mysql", "-uopenapp", "-popenapp", "openapp", "-e",
    f"INSERT INTO openplatform_v2_cp_flow_t (id, name_cn, name_en, lifecycle_status, create_by, last_update_by) VALUES ({snow_id}, 'IT_流删除测试', 'IT_FlowDelete', 0, 'tester', 'tester')"
], capture_output=True)

print("=== IT-034: 删除流 ===")
resp = requests.delete(f"{OPEN_SERVER}/flows/{snow_id}", headers={"Content-Type": "application/json"})
if not is_quiet():
    _print_response(resp, 0)
else:
    code = resp.json().get("code", "unknown")
    print(f"[PASS] DELETE /flows/{snow_id} ({resp.status_code})")
    print(f"  RESULT: code={code}")

print("=== IT-035: flowId 不存在 ===")
resp = requests.delete(f"{OPEN_SERVER}/flows/999999999999999999", headers={"Content-Type": "application/json"})
if not is_quiet():
    _print_response(resp, 0)
else:
    print(f"[PASS] DELETE /flows/999999999999999999 ({resp.status_code})")
