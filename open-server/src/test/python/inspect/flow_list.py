#!/usr/bin/env python3
"""查询流列表 (IT-026~029)"""
from client import api, ok, done

print("=== IT-026: 默认分页 ===")
resp = api("GET", "/flows")
ok(resp, 200, "IT-026: 默认分页")

print("=== IT-027: lifecycleStatus=0 过滤 ===")
resp = api("GET", "/flows", {"lifecycleStatus": 0})
ok(resp, 200, "IT-027: lifecycleStatus=0 过滤")

print("=== IT-028: keyword 搜索 ===")
resp = api("GET", "/flows", {"keyword": "通知"})
ok(resp, 200, "IT-028: keyword 搜索")

print("=== IT-029: 空结果 ===")
resp = api("GET", "/flows", {"keyword": "NONEXISTENT_FLOW_9999"})
ok(resp, 200, "IT-029: 空结果")

done()
