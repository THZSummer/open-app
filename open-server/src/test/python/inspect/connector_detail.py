#!/usr/bin/env python3
"""查询连接器详情 (IT-010~012)"""
from client import *
import subprocess, json

# 先创建一个连接器用于查询
snow_id = int(time.time() * 1000000) % 100000000000000000
subprocess.run([
    "mysql", "-uopenapp", "-popenapp", "openapp", "-e",
    f"INSERT INTO openplatform_v2_cp_connector_t (id, name_cn, name_en, connector_type, create_by, last_update_by) VALUES ({snow_id}, 'IT_详情测试', 'IT_Detail_Test', 1, 'tester', 'tester')"
], capture_output=True)

try:
    print("=== IT-010: 正常查询 ===")
    request("GET", f"/connectors/{snow_id}")

    print("=== IT-011: connectorId 不存在 ===")
    request("GET", "/connectors/999999999999999999")

    print("=== IT-012: 雪花ID为string类型 ===")
    resp = request("GET", f"/connectors/{snow_id}")
finally:
    subprocess.run(["mysql", "-uopenapp", "-popenapp", "openapp", "-e",
                    f"DELETE FROM openplatform_v2_cp_connector_t WHERE id = {snow_id}"], capture_output=True)
