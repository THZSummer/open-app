#!/usr/bin/env python3
"""创建连接器 (IT-001~004)"""
from client import api, ok, done

# IT-001: 正常创建
print("=== IT-001: 正常创建 ===")
resp = api("POST", "/connectors", {
    "nameCn": "IM 发送消息", "nameEn": "IM Send Message",
    "iconFileId": "file_im", "descriptionCn": "封装 IM 消息发送",
    "descriptionEn": "Encapsulated IM messaging", "connectorType": 1
})
ok(resp, 200, "正常创建连接器")

# IT-002: 缺少必填 nameCn
print("=== IT-002: 缺少 nameCn ===")
resp = api("POST", "/connectors", {"nameEn": "Test", "connectorType": 1})
ok(resp is not None and resp.status_code == 400, "缺少 nameCn 应返回 400")

# IT-003: connectorType 非法
print("=== IT-003: connectorType=99 ===")
resp = api("POST", "/connectors", {"nameCn": "测试", "nameEn": "Test", "connectorType": 99})
ok(resp is not None and resp.status_code == 400, "connectorType=99 应返回 400")

# IT-004: nameCn 超长
print("=== IT-004: nameCn >500 ===")
resp = api("POST", "/connectors", {"nameCn": "a" * 501, "nameEn": "Test", "connectorType": 1})
ok(resp is not None and resp.status_code == 400, "nameCn 超长应返回 400")

done()
