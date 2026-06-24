#!/usr/bin/env python3
"""创建连接流 (IT-023~025)"""
from client import api, ok, done

print("=== IT-023: 正常创建 ===")
resp = api("POST", "/flows", {"nameCn": "新消息自动通知", "nameEn": "Auto Message Notification"})
ok(resp, 200, "IT-023: 正常创建")

print("=== IT-024: 缺少 nameCn ===")
resp = api("POST", "/flows", {"nameEn": "Test"})
ok(resp.status_code == 400, "IT-024: 缺少 nameCn")

print("=== IT-025: 缺少 nameEn ===")
resp = api("POST", "/flows", {"nameCn": "测试"})
ok(resp.status_code == 400, "IT-025: 缺少 nameEn")

done()
