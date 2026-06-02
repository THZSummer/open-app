#!/usr/bin/env python3
"""获取连接配置 (IT-017~019)"""
from client import *
import subprocess, time

snow_id = int(time.time() * 1000000) % 100000000000000000
subprocess.run([
    "mysql", "-uopenapp", "-popenapp", "openapp", "-e",
    f"INSERT INTO openplatform_v2_cp_connector_t (id, name_cn, name_en, connector_type, create_by, last_update_by) VALUES ({snow_id}, 'IT_配置测试', 'IT_Config_Test', 1, 'tester', 'tester')"
], capture_output=True)

try:
    print("=== IT-017: 获取配置 ===")
    request("GET", f"/connectors/{snow_id}/config")

    print("=== IT-018: 未配置（初始）===")
    request("GET", f"/connectors/{snow_id}/config")

    print("=== IT-019: 连接器不存在 ===")
    request("GET", "/connectors/999999999999999999/config")
finally:
    subprocess.run(["mysql", "-uopenapp", "-popenapp", "openapp", "-e",
                    f"DELETE FROM openplatform_v2_cp_connector_t WHERE id = {snow_id}"], capture_output=True)
