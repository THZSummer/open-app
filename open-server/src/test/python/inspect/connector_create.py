#!/usr/bin/env python3
"""创建连接器 (IT-001~004)"""
from client import *

# 正常创建
print("=== IT-001: 正常创建 ===")
resp = request("POST", "/service/open/v2/connectors", {
    "nameCn": "IM 发送消息", "nameEn": "IM Send Message",
    "iconFileId": "file_im", "descriptionCn": "封装 IM 消息发送",
    "descriptionEn": "Encapsulated IM messaging", "connectorType": 1
})

# 缺少必填 nameCn
print("=== IT-002: 缺少 nameCn ===")
request("POST", "/service/open/v2/connectors", {"nameEn": "Test", "connectorType": 1})

# connectorType 非法
print("=== IT-003: connectorType=99 ===")
request("POST", "/service/open/v2/connectors", {"nameCn": "测试", "nameEn": "Test", "connectorType": 99})

# nameCn 超长
print("=== IT-004: nameCn >500 ===")
request("POST", "/service/open/v2/connectors", {"nameCn": "a"*501, "nameEn": "Test", "connectorType": 1})
