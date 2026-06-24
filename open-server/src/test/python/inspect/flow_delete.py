#!/usr/bin/env python3
"""删除流 (IT-034~035)"""
from client import api, db, ok, done, TEST_APP_ID
import time

snow_id = int(time.time() * 1000000) % 100000000000000000
db(f"INSERT INTO openplatform_v2_cp_flow_t (id, name_cn, name_en, lifecycle_status, create_by, last_update_by, app_id) VALUES ({snow_id}, 'IT_流删除测试', 'IT_FlowDelete', 0, 'tester', 'tester', {TEST_APP_ID})")

print("=== IT-034: 删除流 ===")
resp = api("DELETE", f"/flows/{snow_id}")
ok(resp, 200, "IT-034: 删除流")

print("=== IT-035: flowId 不存在 ===")
resp = api("DELETE", "/flows/999999999999999999")
ok(resp is not None, name="IT-035: flowId 不存在")

done()
