#!/usr/bin/env python3
"""删除连接器 (IT-015~016)"""
from client import api, db, ok, done, TEST_APP_ID
import time

snow_id = int(time.time() * 1000000) % 100000000000000000
db(f"INSERT INTO openplatform_v2_cp_connector_t (id, name_cn, name_en, connector_type, create_by, last_update_by, app_id) VALUES ({snow_id}, 'IT_删除测试', 'IT_Delete_Test', 1, 'tester', 'tester', {TEST_APP_ID})")

print("=== IT-015: 正常删除 ===")
resp = api("DELETE", f"/connectors/{snow_id}")
ok(resp, 200, "IT-015: 正常删除")

print("=== IT-016: 不存在的连接器 ===")
resp = api("DELETE", "/connectors/999999999999999999")
ok(resp is not None, name="IT-016: 不存在的连接器")

done()
