#!/usr/bin/env python3
"""公共模块：open-server (port 18080) 专用

提供统一的:
- BASE_URL / 请求发送 / 响应打印
- --quiet 模式控制
- is_pass() 自动判断 PASS/FAIL
- check() 契约校验断言

open-server 标准响应格式: {code, messageZh, messageEn, data}
"""
import atexit
import sys
import json
import requests
import time
import re

_pass_count = 0
_fail_count = 0

__all__ = [
    "BASE_URL", "is_quiet", "request",
    "check", "check_field_type", "check_camel_case",
    "_print_request", "_print_response", "_is_pass",
]

BASE_URL = "http://localhost:18080/open-server"

def is_quiet():
    return "--quiet" in sys.argv

def request(method, path, body=None, headers=None):
    url = f"{BASE_URL}{path}"
    h = {"Content-Type": "application/json"}
    if headers:
        h.update(headers)
    try:
        start = time.time()
        if method == "POST":
            resp = requests.post(url, json=body, headers=h, timeout=10)
        elif method == "GET":
            resp = requests.get(url, headers=h, timeout=10)
        elif method == "PUT":
            resp = requests.put(url, json=body, headers=h, timeout=10)
        elif method == "DELETE":
            resp = requests.delete(url, headers=h, timeout=10)
        else:
            resp = requests.request(method, url, json=body, headers=h, timeout=10)
        elapsed = time.time() - start
    except requests.exceptions.ConnectionError:
        if not is_quiet():
            print(f"\n  SKIP: open-server 未运行 (port 18080)")
        else:
            print(f"[SKIP] {method} {path}")
        return None

    if not is_quiet():
        _print_request(method, url, h, body)
        _print_response(resp, elapsed)
    return resp

def _print_request(method, url, headers, body):
    print(f"\n{'='*60}")
    print(f"REQUEST: {method} {url}")
    if body:
        print(f"  Body: {json.dumps(body, ensure_ascii=False)}")
    print(f"{'='*60}")

def _print_response(resp, elapsed):
    print(f"RESPONSE: {resp.status_code}")
    try:
        body = resp.json()
        print(f"  Body: {json.dumps(body, indent=2, ensure_ascii=False)}")
    except Exception:
        print(f"  Body: {resp.text[:500]}")
    print(f"  Time: {elapsed:.2f}s")
    ok = _is_pass(resp)
    tag = "PASS" if ok else "FAIL"
    full = "✅" if ok else "❌"
    print(f"  Result: [{full} {tag}]")
    print()

def _is_pass(resp):
    """判断请求是否通过：open-server 返回 code=200 视为通过"""
    if resp.status_code not in (200, 201):
        return False
    try:
        data = resp.json()
        code = data.get("code")
        return code == "200" or code == 200
    except Exception:
        return resp.ok

def check(name, condition, detail=""):
    global _pass_count, _fail_count
    if condition:
        _pass_count += 1
        print(f"  ✅ PASS: {name}" + (f" - {detail}" if detail else ""))
    else:
        _fail_count += 1
        print(f"  ❌ FAIL: {name}" + (f" - {detail}" if detail else ""))

def check_field_type(body, field_path, expected_type):
    parts = field_path.split(".")
    current = body
    for p in parts:
        if isinstance(current, dict) and p in current:
            current = current[p]
        else:
            return False, f"字段 {field_path} 不存在"
    actual = type(current).__name__
    ok = actual == expected_type
    detail = f"期望 {expected_type}, 实际 {actual}" if not ok else ""
    return ok, detail

def check_camel_case(name):
    return bool(re.match(r"^[a-z]+[A-Za-z0-9]*$", name))

def _print_summary():
    if is_quiet():
        return
    print(f"\n── 测试结果 ──")
    print(f"  ✅ PASS: {_pass_count}  ❌ FAIL: {_fail_count}")
    print(f"  exit code: {'1' if _fail_count > 0 else '0'}")

def _atexit_handler():
    _print_summary()
    if _fail_count > 0:
        sys.exit(1)

atexit.register(_atexit_handler)
