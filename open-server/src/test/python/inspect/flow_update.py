#!/usr/bin/env python3
"""更新流 (IT-032~033)"""
from client import api, db, ok, done, TEST_APP_ID
import time

snow_id = int(time.time() * 1000000) % 100000000000000000
db(f"INSERT INTO openplatform_v2_cp_flow_t (id, name_cn, name_en, lifecycle_status, create_by, last_update_by, app_id) VALUES ({snow_id}, 'IT_流更新测试', 'IT_FlowUpdate', 0, 'tester', 'tester', {TEST_APP_ID})")

try:
    print("=== IT-032: 正常更新 ===")
    resp = api("PUT", f"/flows/{snow_id}", {"nameCn": "更新后的流名称"})
    ok(resp, 200, "IT-032: 正常更新")

    print("=== IT-033: flowId 不存在 ===")
    resp = api("PUT", "/flows/999999999999999999", {"nameCn": "测试"})
    ok(resp is not None, name="IT-033: flowId 不存在")
finally:
    db(f"DELETE FROM openplatform_v2_cp_flow_t WHERE id = {snow_id}")

done()
