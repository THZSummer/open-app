#!/usr/bin/env python3
"""HTTP 触发 (IT-049~051)"""
from client import *

print("=== IT-049: 凭证缺失 ===")
connector_api_request("POST", "/trigger/999999999999999999/invoke", {"payload": {"message": "test"}})

print("=== IT-050: flow 不存在 ===")
connector_api_request("POST", "/trigger/999999999999999999/invoke",
                      {"payload": {}}, headers={"X-Sys-Token": "test-token"})

print("=== IT-051: flow 未运行 ===")
connector_api_request("POST", "/trigger/999999999999999999/invoke",
                      {"payload": {}}, headers={"X-Sys-Token": "test-token"})
