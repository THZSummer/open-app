#!/usr/bin/env python3
"""
公共模块：connector-api (port 18180) 专用

提供统一的:
- BASE_URL / 请求发送 / 响应打印
- --quiet 模式控制
- is_pass() 自动判断 PASS/FAIL
- check() 契约校验断言

用法:
  from client import *
  resp = request("POST", "/trigger/{flowId}/invoke", {...}, headers={...})
  if resp:
      check("状态码", resp.status_code == 200)
      check("errorInfo.code 为 6001", resp.json().get("errorInfo", {}).get("code") == "6001")
"""
import sys
import json
import requests
import time
import re

__all__ = [
    "BASE_URL", "is_quiet", "request",
    "check", "check_field_type", "check_time_iso8601", "check_camel_case",
    "_print_request", "_print_response", "_is_pass",
]

BASE_URL = "http://localhost:18180/api/v1"


def is_quiet():
    """检查命令行是否包含 --quiet 参数"""
    return "--quiet" in sys.argv


def request(method, path, body=None, headers=None):
    """发送请求到 connector-api (port 18180)，返回 Response 对象

    连接失败时返回 None（打印 SKIP）; --quiet 时抑制请求/响应详情。
    """
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
        else:
            resp = requests.request(method, url, json=body, headers=h, timeout=10)
        elapsed = time.time() - start
    except requests.exceptions.ConnectionError:
        if not is_quiet():
            print(f"\n  SKIP: connector-api 未运行 (port 18180)")
        else:
            print(f"[SKIP] {method} {path}")
        return None

    if not is_quiet():
        _print_request(method, url, h, body)
        _print_response(resp, elapsed)
    # quiet mode: suppress all per-call output

    return resp


def _print_request(method, url, headers, body):
    """打印请求详情"""
    print(f"\n{'='*60}")
    print(f"REQUEST: {method} {url}")
    if body:
        print(f"  Body: {json.dumps(body, ensure_ascii=False)}")
    print(f"{'='*60}")


def _print_response(resp, elapsed):
    """打印响应详情 + 自动 PASS/FAIL 标签"""
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
    """判断请求是否通过：connector-api 返回 executionId 且 status!=failed 视为通过"""
    if resp.status_code != 200:
        return False
    try:
        data = resp.json()
        # connector-api 返回格式：有 executionId 且 status 不是 "failed"
        if "executionId" in data:
            return data.get("status") != "failed"
        # 兼容标准响应格式
        code = data.get("code", "unknown")
        return code == "200" or code == "201"
    except Exception:
        return resp.ok


def check(name, condition, detail=""):
    """契约校验断言：PASS / FAIL + 可选详细描述"""
    if condition:
        print(f"  ✅ PASS: {name}" + (f" - {detail}" if detail else ""))
    else:
        print(f"  ❌ FAIL: {name}" + (f" - {detail}" if detail else ""))


def check_field_type(body, field_path, expected_type):
    """校验 JSON 字段类型（支持嵌套路径如 'data.executionId'）"""
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


def check_time_iso8601(value):
    """校验时间字符串是否为 ISO 8601 格式"""
    if not isinstance(value, str):
        return False
    pattern = r"^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d{3}"
    return bool(re.match(pattern, value))


def check_camel_case(name):
    """校验字段名是否为 camelCase"""
    return bool(re.match(r"^[a-z]+[A-Za-z0-9]*$", name))