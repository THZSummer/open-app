#!/usr/bin/env python3
"""查询流列表 (IT-026~029)"""
from client import *

print("=== IT-026: 默认分页 ===")
request("GET", "/flows")

print("=== IT-027: lifecycleStatus=0 过滤 ===")
request("GET", "/flows", {"lifecycleStatus": 0})

print("=== IT-028: keyword 搜索 ===")
request("GET", "/flows", {"keyword": "通知"})

print("=== IT-029: 空结果 ===")
request("GET", "/flows", {"keyword": "NONEXISTENT_FLOW_9999"})
