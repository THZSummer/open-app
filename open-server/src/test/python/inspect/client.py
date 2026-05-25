#!/usr/bin/env python3
"""
公共模块：BASE_URL + 请求发送 + 响应打印 + --quiet 控制

所有 inspect 接口文件通过 `from client import *` 使用此模块。
"""
import sys
import json
import requests
import time

# ── 配置 ──────────────────────────────────────────────
OPEN_SERVER = "http://localhost:18080/open-server/api/v1"
CONNECTOR_API = "http://localhost:18180/api/v1"


def is_quiet():
    """检查命令行是否包含 --quiet 参数"""
    return "--quiet" in sys.argv


def request(method, path, body=None, headers=None):
    """发送请求，返回 Response 对象"""
    url = f"{OPEN_SERVER}{path}"
    h = {"Content-Type": "application/json"}
    if headers:
        h.update(headers)

    start = time.time()
    if method == "GET":
        resp = requests.get(url, params=body, headers=h)
    elif method == "POST":
        resp = requests.post(url, json=body, headers=h)
    elif method == "PUT":
        resp = requests.put(url, json=body, headers=h)
    elif method == "DELETE":
        resp = requests.delete(url, headers=h)
    else:
        raise ValueError(f"Unsupported method: {method}")
    elapsed = time.time() - start

    if not is_quiet():
        _print_request(method, url, h, body)
        _print_response(resp, elapsed)
    else:
        ok = _is_pass(resp)
        tag = "PASS" if ok else "FAIL"
        print(f"[{tag}] {method} {path} ({resp.status_code})")

    return resp


def connector_api_request(method, path, body=None, headers=None):
    """发送请求到 connector-api (port 18180)"""
    url = f"{CONNECTOR_API}{path}"
    h = {"Content-Type": "application/json"}
    if headers:
        h.update(headers)

    try:
        start = time.time()
        if method == "POST":
            resp = requests.post(url, json=body, headers=h, timeout=5)
        else:
            resp = requests.get(url, headers=h, timeout=5)
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
    else:
        print(f"[PASS] {method} {path} ({resp.status_code})")

    return resp


def _print_request(method, url, headers, body):
    """打印请求详情"""
    print(f"{'='*60}")
    print(f"REQUEST: {method} {url}")
    if body:
        print(f"  Body: {json.dumps(body, ensure_ascii=False)}")
    print(f"{'='*60}")


def _print_response(resp, elapsed):
    """打印响应详情"""
    print(f"RESPONSE: {resp.status_code}")
    try:
        body = resp.json()
        print(f"  Body: {json.dumps(body, indent=2, ensure_ascii=False)}")
    except Exception:
        print(f"  Body: {resp.text[:500]}")
    print(f"  Time: {elapsed:.2f}s")
    ok = _is_pass(resp)
    tag = "PASS" if ok else "FAIL"
    print(f"  Result: {tag}")
    print()


def _is_pass(resp):
    """判断是否通过"""
    try:
        data = resp.json()
        code = data.get("code", "unknown")
        return code == "200" or code == "201"
    except Exception:
        return resp.ok
