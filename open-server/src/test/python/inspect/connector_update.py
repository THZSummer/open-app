#!/usr/bin/env python3
"""更新连接器 (IT-013~014)"""
from client import *
import subprocess, time

snow_id = int(time.time() * 1000000) % 100000000000000000
subprocess.run([
    "mysql", "-uopenapp", "-popenapp", "openapp", "-e",
    f"INSERT INTO openplatform_v2_cp_connector_t (id, name_cn, name_en, connector_type, create_by, last_update_by) VALUES ({snow_id}, 'IT_更新测试', 'IT_Update_Test', 1, 'tester', 'tester')"
], capture_output=True)

try:
    print("=== IT-013: 正常更新 ===")
    request("PUT", f"/connectors/{snow_id}", {"nameCn": "更新后的名称"})

    print("=== IT-014: 不存在的连接器 ===")
    request("PUT", "/connectors/999999999999999999", {"nameCn": "测试"})
finally:
    subprocess.run(["mysql", "-uopenapp", "-popenapp", "openapp", "-e",
                    f"DELETE FROM openplatform_v2_cp_connector_t WHERE id = {snow_id}"], capture_output=True)
