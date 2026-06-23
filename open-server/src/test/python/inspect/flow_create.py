#!/usr/bin/env python3
"""创建连接流 (IT-023~025)"""
from client import *

print("=== IT-023: 正常创建 ===")
request("POST", "/service/open/v2/flows", {"nameCn": "新消息自动通知", "nameEn": "Auto Message Notification"})

print("=== IT-024: 缺少 nameCn ===")
request("POST", "/service/open/v2/flows", {"nameEn": "Test"})

print("=== IT-025: 缺少 nameEn ===")
request("POST", "/service/open/v2/flows", {"nameCn": "测试"})
