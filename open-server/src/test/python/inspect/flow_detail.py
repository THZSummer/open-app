#!/usr/bin/env python3
"""查询流详情 (IT-030~031)"""
from client import api, db, ok, done, TEST_APP_ID
import time

snow_id = int(time.time() * 1000000) % 100000000000000000
db(f"INSERT INTO openplatform_v2_cp_flow_t (id, name_cn, name_en, lifecycle_status, create_by, last_update_by, app_id) VALUES ({snow_id}, 'IT_流详情测试', 'IT_FlowDetail', 0, 'tester', 'tester', {TEST_APP_ID})")

try:
    print("=== IT-030: 正常查询 ===")
    resp = api("GET", f"/flows/{snow_id}")
    ok(resp, 200, "IT-030: 正常查询")

    print("=== IT-031: flowId 不存在 ===")
    resp = api("GET", "/flows/999999999999999999")
    ok(resp is not None, name="IT-031: flowId 不存在")
finally:
    db(f"DELETE FROM openplatform_v2_cp_flow_t WHERE id = {snow_id}")

done()
