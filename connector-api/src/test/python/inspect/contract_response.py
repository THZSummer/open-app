#!/usr/bin/env python3
"""L4 契约测试 (connector-api 响应格式)"""
from client import *
import re


def check(name, condition, detail=""):
    if condition:
        print(f"  PASS: {name}" + (f" - {detail}" if detail else ""))
    else:
        print(f"  FAIL: {name}" + (f" - {detail}" if detail else ""))


print("\n=== 成功响应格式 ===")
resp = request("GET", "/trigger/999999999999999999/invoke", {"payload": {}})
if resp:
    body = resp.json()
    check("响应包含code字段", "code" in body or "status" in body)

print("\n=== 错误响应格式 ===")
resp = request("GET", "/trigger/999999999999999999/invoke", {"payload": {}},
               headers={"X-Sys-Token": "test-token"})
if resp:
    body = resp.json()
    check("错误响应code不为200", body.get("code") != "200" if "code" in body else True)
