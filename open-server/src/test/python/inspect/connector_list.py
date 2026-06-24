#!/usr/bin/env python3
"""查询连接器列表 (IT-005~009)"""
from client import api, ok, done

print("=== IT-005: 默认分页 ===")
resp = api("GET", "/connectors")
ok(resp, 200, "IT-005: 默认分页")

print("=== IT-006: connectorType=1 过滤 ===")
resp = api("GET", "/connectors", {"connectorType": 1})
ok(resp, 200, "IT-006: connectorType=1 过滤")

print("=== IT-007: keyword 搜索 ===")
resp = api("GET", "/connectors", {"keyword": "IM"})
ok(resp, 200, "IT-007: keyword 搜索")

print("=== IT-008: 自定义分页 ===")
resp = api("GET", "/connectors", {"curPage": 2, "pageSize": 10})
ok(resp, 200, "IT-008: 自定义分页")

print("=== IT-009: 空结果 ===")
resp = api("GET", "/connectors", {"keyword": "NONEXISTENT_XYZ_9999"})
ok(resp, 200, "IT-009: 空结果")

done()
