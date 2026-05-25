#!/usr/bin/env python3
"""
公共模块：connector-api (port 18180) 专用
"""
import sys
import json
import requests
import time

BASE_URL = "http://localhost:18180/api/v1"


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
    # quiet mode: suppress output

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
        print(f"  Body: {json.dumps(resp.json(), indent=2, ensure_ascii=False)}")
    except Exception:
        print(f"  Body: {resp.text[:500]}")
    print(f"  Time: {elapsed:.2f}s")
    print()
