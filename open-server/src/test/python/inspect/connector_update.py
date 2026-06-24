#!/usr/bin/env python3
"""更新连接器 (IT-013~014)"""
from client import api, db, ok, done, TEST_APP_ID
import time

snow_id = int(time.time() * 1000000) % 100000000000000000
db(f"INSERT INTO openplatform_v2_cp_connector_t (id, name_cn, name_en, connector_type, create_by, last_update_by, app_id) VALUES ({snow_id}, 'IT_更新测试', 'IT_Update_Test', 1, 'tester', 'tester', {TEST_APP_ID})")

try:
    print("=== IT-013: 正常更新 ===")
    resp = api("PUT", f"/connectors/{snow_id}", {"nameCn": "更新后的名称"})
    ok(resp, 200, "IT-013: 正常更新")

    print("=== IT-014: 不存在的连接器 ===")
    resp = api("PUT", "/connectors/999999999999999999", {"nameCn": "测试"})
    ok(resp is not None, name="IT-014: 不存在的连接器")
finally:
    db(f"DELETE FROM openplatform_v2_cp_connector_t WHERE id = {snow_id}")

done()
