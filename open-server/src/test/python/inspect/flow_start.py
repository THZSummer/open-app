#!/usr/bin/env python3
"""启动流 (IT-036~038)"""
from client import api, db, ok, done, TEST_APP_ID
import time

snow_id = int(time.time() * 1000000) % 100000000000000000
db(f"INSERT INTO openplatform_v2_cp_flow_t (id, name_cn, name_en, lifecycle_status, create_by, last_update_by, app_id) VALUES ({snow_id}, 'IT_流启动测试', 'IT_FlowStart', 0, 'tester', 'tester', {TEST_APP_ID})")

try:
    print("=== IT-036: 启动流 ===")
    resp = api("POST", f"/flows/{snow_id}/start")
    ok(resp, 200, "IT-036: 启动流")

    print("=== IT-037: 重复启动 ===")
    resp = api("POST", f"/flows/{snow_id}/start")
    ok(resp is not None, name="IT-037: 重复启动")

    print("=== IT-038: flowId 不存在 ===")
    resp = api("POST", "/flows/999999999999999999/start")
    ok(resp is not None, name="IT-038: flowId 不存在")
finally:
    db(f"DELETE FROM openplatform_v2_cp_flow_t WHERE id = {snow_id}")

done()
