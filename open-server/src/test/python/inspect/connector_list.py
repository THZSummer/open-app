#!/usr/bin/env python3
"""查询连接器列表 (IT-005~009)"""
from client import *

print("=== IT-005: 默认分页 ===")
request("GET", "/service/open/v2/connectors")

print("=== IT-006: connectorType=1 过滤 ===")
request("GET", "/service/open/v2/connectors", {"connectorType": 1})

print("=== IT-007: keyword 搜索 ===")
request("GET", "/service/open/v2/connectors", {"keyword": "IM"})

print("=== IT-008: 自定义分页 ===")
request("GET", "/service/open/v2/connectors", {"curPage": 2, "pageSize": 10})

print("=== IT-009: 空结果 ===")
request("GET", "/service/open/v2/connectors", {"keyword": "NONEXISTENT_XYZ_9999"})
