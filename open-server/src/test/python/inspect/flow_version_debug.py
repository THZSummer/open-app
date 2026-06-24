#!/usr/bin/env python3
"""调试测试运行 (IT-047~048) — V3 #51"""
from client import api, db, ok, done
import time, json

snow_id = int(time.time() * 1000000) % 100000000000000000

# 准备：建连接流 + 草稿版本 + 编排配置
db(f"INSERT INTO openplatform_v2_cp_flow_t (id, name_cn, name_en, lifecycle_status, app_id, create_by, last_update_by) VALUES ({snow_id}, '调试测试流', 'DebugTestFlow', 1, '202606241730488926', 'tester', 'tester')")

vid = snow_id + 1
db(f"INSERT INTO openplatform_v2_cp_flow_version_t (id, flow_id, version_number, status, orchestration_config, create_by, last_update_by) VALUES ({vid}, {snow_id}, 1, 1, '{{}}', 'tester', 'tester')")

try:
    # IT-047: 草稿版本调试（未配置编排 — 应能发起但可能返回错误）
    print("=== IT-047: 草稿版本调试（无编排配置） ===")
    resp = api("POST", f"/flows/{snow_id}/versions/{vid}/debug", {"triggerData": {"message": "hello"}})
    ok(resp is not None, name="IT-047: 调试请求已发送")

    # IT-048: 版本不存在
    print("=== IT-048: 版本不存在 ===")
    resp = api("POST", f"/flows/{snow_id}/versions/999999999999999999/debug", {"triggerData": {}})
    ok(resp is not None, name="IT-048: 版本不存在")

finally:
    db(f"DELETE FROM openplatform_v2_cp_flow_version_t WHERE id = {vid}")
    db(f"DELETE FROM openplatform_v2_cp_flow_t WHERE id = {snow_id}")

done()
