#!/usr/bin/env python3
"""删除连接器 (IT-015~016)"""
from client import *
import subprocess, time

snow_id = int(time.time() * 1000000) % 100000000000000000
subprocess.run([
    "mysql", "-uopenapp", "-popenapp", "openapp", "-e",
    f"INSERT INTO openplatform_v2_cp_connector_t (id, name_cn, name_en, connector_type, create_by, last_update_by) VALUES ({snow_id}, 'IT_删除测试', 'IT_Delete_Test', 1, 'tester', 'tester')"
], capture_output=True)

print("=== IT-015: 正常删除 ===")
resp = request("DELETE", f"/connectors/{snow_id}")

print("=== IT-016: 不存在的连接器 ===")
request("DELETE", "/connectors/999999999999999999")
