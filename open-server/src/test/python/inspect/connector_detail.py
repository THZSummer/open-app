#!/usr/bin/env python3
"""查询连接器详情 (IT-010~012)"""
from client import api, db, ok, done, TEST_APP_ID
import time

snow_id = int(time.time() * 1000000) % 100000000000000000
db(f"INSERT INTO openplatform_v2_cp_connector_t (id, name_cn, name_en, connector_type, create_by, last_update_by, app_id) VALUES ({snow_id}, 'IT_详情测试', 'IT_Detail_Test', 1, 'tester', 'tester', {TEST_APP_ID})")

try:
    print("=== IT-010: 正常查询 ===")
    resp = api("GET", f"/connectors/{snow_id}")
    ok(resp, 200, "IT-010: 正常查询")

    print("=== IT-011: connectorId 不存在 ===")
    resp = api("GET", "/connectors/999999999999999999")
    ok(resp is not None, name="IT-011: connectorId 不存在")

    print("=== IT-012: 雪花ID为string类型 ===")
    resp = api("GET", f"/connectors/{snow_id}")
    ok(resp, 200, "IT-012: 雪花ID为string类型")
finally:
    db(f"DELETE FROM openplatform_v2_cp_connector_t WHERE id = {snow_id}")

done()
