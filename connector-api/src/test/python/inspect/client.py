#!/usr/bin/env python3
"""
公共模块：connector-api (port 18180) 专用

提供统一的:
- BASE_URL / 请求发送 / 响应打印
- --quiet 模式控制
- is_pass() 自动判断 PASS/FAIL
- check() 契约校验断言
- db() / db_val() / snow_id() / escape_sql() — 数据库操作 + 测试数据管理

配置：
  切换环境只需修改本文件顶部的 _DB / BASE_URL 字典。

用法:
  from client import *

  # HTTP
  resp = request("POST", "/trigger/{flowId}/invoke", {...}, headers={...})

  # DB
  fid = snow_id()
  db(f"INSERT INTO openplatform_v2_cp_flow_t (...) VALUES ({fid}, ...)")
  rows = db_val("SELECT COUNT(*) FROM openplatform_v2_cp_flow_t")

  # 断言
  check("状态码", resp.status_code == 200)
"""
import sys
import json
import requests
import time
import re
import atexit
import subprocess

_pass_count = 0
_fail_count = 0

__all__ = [
    "BASE_URL", "is_quiet", "request",
    "check", "check_field_type", "check_time_iso8601", "check_camel_case",
    "_print_request", "_print_response", "_is_pass",
    # V4: DB 基础设施
    "db", "db_val", "snow_id", "escape_sql",
    "_DB", "_API_HOST",
    "redis",
]

BASE_URL = "http://localhost:18180/api/v1"

# ═══════════════════════════════════════════════════════════
# 数据库配置 — 切换环境只需改这里
# ═══════════════════════════════════════════════════════════
_DB = {
    "host": "192.168.3.155",
    "user": "openapp",
    "passwd": "openapp",
    "db": "openapp",
}
_REDIS_CLUSTER = {
    "nodes": [
        {"host": "192.168.3.201", "port": 6379},
        {"host": "192.168.3.202", "port": 6379},
        {"host": "192.168.3.203", "port": 6379},
        {"host": "192.168.3.204", "port": 6379},
        {"host": "192.168.3.205", "port": 6379},
    ],
    "password": "openapp",
}
_API_HOST = "localhost:18180"


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
    global _pass_count, _fail_count
    if condition:
        _pass_count += 1
        print(f"  ✅ PASS: {name}" + (f" - {detail}" if detail else ""))
    else:
        _fail_count += 1
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


# ═══════════════════════════════════════════════════════════
# V4: 数据库基础设施 — 所有脚本共享
# ═══════════════════════════════════════════════════════════

def db(sql, capture=False):
    """执行 MySQL SQL 语句。

    Args:
        sql: 要执行的 SQL 语句
        capture: 如果为 True，返回 stdout 字符串；否则返回 None
    """
    cmd = [
        "mysql", f"-h{_DB['host']}", f"-u{_DB['user']}",
        f"-p{_DB['passwd']}", _DB['db'], "-e", sql
    ]
    r = subprocess.run(cmd, capture_output=True, text=True)
    return r.stdout if capture else None


def db_val(sql):
    """执行 SQL 并返回单个值（第一列第一行）。

    用于 COUNT(*)、获取某个字段值等场景。
    """
    out = db(sql, capture=True)
    if out is None:
        return None
    lines = out.strip().split('\n')
    return lines[-1].strip() if len(lines) > 1 else None


def snow_id():
    """生成唯一雪花 ID（基于微秒时间戳）。

    所有测试数据 ID 必须通过此函数生成，确保不冲突。
    """
    return int(time.time() * 1000000) % 100000000000000000


def escape_sql(obj):
    """将 Python 对象转为 MySQL-safe JSON 字符串。

    用于将 orchestration_config / connection_config 等 JSON 字段插入 SQL。
    """
    return json.dumps(obj).replace("\\", "\\\\").replace("'", "''")


def redis(*args):
    """执行 redis-cli 命令（Cluster 模式）。

    例: redis("KEYS", "*") → redis("SET", "k", "v") → redis("GET", "k")
    """
    first = _REDIS_CLUSTER["nodes"][0]
    cmd = [
        "redis-cli", "-c",
        "-h", first["host"], "-p", str(first["port"]),
        "-a", _REDIS_CLUSTER["password"], "--no-auth-warning"
    ]
    cmd.extend([str(a) for a in args])
    r = subprocess.run(cmd, capture_output=True, text=True)
    return r.stdout.strip() if r.returncode == 0 else None


def _print_summary():
    """打印测试结果汇总（仅在非 quiet 模式下输出）"""
    if is_quiet():
        return
    print(f"\n── 测试结果 ──")
    print(f"  ✅ PASS: {_pass_count}  ❌ FAIL: {_fail_count}")
    print(f"  exit code: {'1' if _fail_count > 0 else '0'}")


def _atexit_handler():
    """atexit 处理函数：打印汇总并在失败时设置非零退出码"""
    _print_summary()
    if _fail_count > 0:
        sys.exit(1)


atexit.register(_atexit_handler)
