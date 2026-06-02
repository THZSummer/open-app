#!/usr/bin/env python3
"""保存编排配置 (IT-044~046)"""
from client import *
import subprocess, time

snow_id = int(time.time() * 1000000) % 100000000000000000
subprocess.run([
    "mysql", "-uopenapp", "-popenapp", "openapp", "-e",
    f"INSERT INTO openplatform_v2_cp_flow_t (id, name_cn, name_en, lifecycle_status, create_by, last_update_by) VALUES ({snow_id}, 'IT_保存编排测试', 'IT_SaveConfig', 0, 'tester', 'tester')"
], capture_output=True)

try:
    print("=== IT-044: 正常保存 ===")
    request("PUT", f"/flows/{snow_id}/config",
            {"orchestrationConfig": '{"nodes":[{"id":"n1","type":"entry","position":{"x":100,"y":200},"data":{"labelCn":"入口","labelEn":"Entry"}}],"edges":[]}'})

    print("=== IT-045: orchestrationConfig 为空字符串 ===")
    request("PUT", f"/flows/{snow_id}/config", {"orchestrationConfig": ""})

    print("=== IT-046: orchestrationConfig 为null ===")
    request("PUT", f"/flows/{snow_id}/config", {})
finally:
    subprocess.run(["mysql", "-uopenapp", "-popenapp", "openapp", "-e",
                    f"DELETE FROM openplatform_v2_cp_flow_t WHERE id = {snow_id}"], capture_output=True)
