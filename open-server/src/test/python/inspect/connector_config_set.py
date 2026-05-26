#!/usr/bin/env python3
"""编辑连接配置 (IT-020~022)"""
from client import *
import subprocess, time

snow_id = int(time.time() * 1000000) % 100000000000000000
subprocess.run([
    "mysql", "-uopenapp", "-popenapp", "openapp", "-e",
    f"INSERT INTO openplatform_v2_cp_connector_t (id, name_cn, name_en, connector_type, create_by, last_update_by) VALUES ({snow_id}, 'IT_配置编辑测试', 'IT_ConfigEdit_Test', 1, 'tester', 'tester')"
], capture_output=True)

try:
    print("=== IT-020: 正常编辑 ===")
    request("PUT", f"/connectors/{snow_id}/config",
            {"connectionConfig": '{"protocol":"HTTP","url":"https://api.test.com","authConfig":{"type":"none"},"inputContract":{"protocol":"HTTP","body":{"type":"json","schema":{"type":"object","properties":{}}}},"outputContract":{"protocol":"HTTP","body":{"type":"json","schema":{"type":"object","properties":{}}}},"rateLimitConfig":{"maxQps":10}}'})

    print("=== IT-021: connectionConfig 为空字符串 ===")
    request("PUT", f"/connectors/{snow_id}/config", {"connectionConfig": ""})

    print("=== IT-022: connectionConfig 为null ===")
    request("PUT", f"/connectors/{snow_id}/config", {})
finally:
    subprocess.run(["mysql", "-uopenapp", "-popenapp", "openapp", "-e",
                    f"DELETE FROM openplatform_v2_cp_connector_t WHERE id = {snow_id}"], capture_output=True)
