#!/usr/bin/env python3
"""停止流 (IT-039~041)"""
from client import api, db, ok, done, TEST_APP_ID
import time

snow_id = int(time.time() * 1000000) % 100000000000000000
db(f"INSERT INTO openplatform_v2_cp_flow_t (id, name_cn, name_en, lifecycle_status, create_by, last_update_by, app_id) VALUES ({snow_id}, 'IT_流停止测试', 'IT_FlowStop', 0, 'tester', 'tester', {TEST_APP_ID})")

try:
    api("POST", f"/flows/{snow_id}/start")

    print("=== IT-039: 停止流 ===")
    resp = api("POST", f"/flows/{snow_id}/stop")
    ok(resp, 200, "IT-039: 停止流")

    print("=== IT-040: 重复停止 ===")
    resp = api("POST", f"/flows/{snow_id}/stop")
    ok(resp is not None, name="IT-040: 重复停止")

    print("=== IT-041: flowId 不存在 ===")
    resp = api("POST", "/flows/999999999999999999/stop")
    ok(resp is not None, name="IT-041: flowId 不存在")
finally:
    db(f"DELETE FROM openplatform_v2_cp_flow_t WHERE id = {snow_id}")

done()
