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
  resp = api("POST", "/flows/{flowId}/invoke", {...}, headers={...})

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

_pass_count = 0
_fail_count = 0

__all__ = [
    "BASE_URL", "is_quiet", "api",
    "check", "check_field_type", "check_time_iso8601", "check_camel_case",
    "done",
    "_print_request", "_print_response", "_is_pass",
    # V4: DB 基础设施
    "db", "db_val", "snow_id", "escape_sql",
    "_DB", "_API_HOST", "TEST_APP_ID", "INTERNAL_APP_ID",
    "redis",
    # HTTP 快捷方法
    "trigger", "debug_run",
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
TEST_APP_ID = "20250730213114178360970"  # 与 open-server 共用测试应用
INTERNAL_APP_ID = None  # 内部主键 ID，首次使用时从 DB 查询


def is_quiet():
    """检查命令行是否包含 --quiet 参数"""
    return "--quiet" in sys.argv


def api(method, path, body=None, headers=None):
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
    """契约校验断言：PASS / FAIL + 可选详细描述。失败时抛出 AssertionError。"""
    global _pass_count, _fail_count
    if condition:
        _pass_count += 1
        print(f"  ✅ PASS: {name}" + (f" - {detail}" if detail else ""))
    else:
        _fail_count += 1
        msg = f"  ❌ FAIL: {name}" + (f" - {detail}" if detail else "")
        print(msg)
        raise AssertionError(msg)


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

_db_conn = None

def _get_db_conn():
    global _db_conn
    if _db_conn is None or not _db_conn.open:
        import pymysql
        _db_conn = pymysql.connect(
            host=_DB["host"],
            user=_DB["user"],
            password=_DB["passwd"],
            database=_DB["db"],
            charset="utf8mb4",
            autocommit=True
        )
    return _db_conn

def db(sql, capture=False):
    """执行 SQL。capture=True 返回 TSV 格式字符串（兼容旧版 mysql CLI 输出）。"""
    try:
        conn = _get_db_conn()
        with conn.cursor() as cursor:
            cursor.execute(sql)
            if capture and cursor.description:
                cols = [d[0] for d in cursor.description]
                rows = cursor.fetchall()
                lines = ["\t".join(cols)]
                for row in rows:
                    lines.append("\t".join(str(v) if v is not None else "NULL" for v in row))
                return "\n".join(lines)
    except Exception as e:
        print(f"  DB ERROR: {e}")
    return None


def db_rows(sql):
    """执行 SQL 并返回结构化数据 list[dict]。用于需要结构化访问的场景。"""
    try:
        conn = _get_db_conn()
        with conn.cursor() as cursor:
            cursor.execute(sql)
            if cursor.description:
                cols = [d[0] for d in cursor.description]
                rows = cursor.fetchall()
                return [dict(zip(cols, row)) for row in rows]
    except Exception as e:
        print(f"  DB ERROR: {e}")
    return []


def db_val(sql):
    """执行 SQL 并返回单个值（第一行第一列）。"""
    rows = db_rows(sql)
    if rows:
        first_col = list(rows[0].values())[0]
        return str(first_col) if first_col is not None else None
    return None


def _init_internal_app_id():
    """lazy 初始化 INTERNAL_APP_ID"""
    global INTERNAL_APP_ID
    if INTERNAL_APP_ID is None:
        val = db_val(f"SELECT id FROM openplatform_app_t WHERE app_id = '{TEST_APP_ID}' AND status = 1")
        INTERNAL_APP_ID = int(val) if val else None
    return INTERNAL_APP_ID


_init_internal_app_id()


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


_redis_client = None

def redis(*args):
    """执行 Redis 命令（Cluster 模式）。"""
    global _redis_client
    try:
        from redis.cluster import RedisCluster
        if _redis_client is None:
            first = _REDIS_CLUSTER["nodes"][0]
            _redis_client = RedisCluster(
                host=first["host"],
                port=first["port"],
                password=_REDIS_CLUSTER.get("password"),
                decode_responses=True
            )
        cmd = args[0].upper() if args else ""
        if cmd == "GET":
            return _redis_client.get(args[1])
        elif cmd == "SET":
            return _redis_client.set(args[1], args[2])
        elif cmd == "DEL":
            return _redis_client.delete(args[1])
        elif cmd == "KEYS":
            return _redis_client.keys(args[1] if len(args) > 1 else "*")
        elif cmd == "FLUSHDB":
            return _redis_client.flushdb()
        else:
            return _redis_client.execute_command(*args)
    except Exception as e:
        print(f"  REDIS ERROR: {e}")
        return None


def trigger(flow_id, body=None, headers=None, query_params=None):
    """HTTP 触发连接流 — POST /api/v1/flows/{flowId}/invoke

    返回 Response 对象（连接失败返回 None）。
    query_params 示例: {"page": "1", "size": "10"}
    """
    path = f"/flows/{flow_id}/invoke"
    if query_params:
        import urllib.parse
        qs = urllib.parse.urlencode(query_params, doseq=True)
        path = f"{path}?{qs}"
    return api("POST", path, body, headers)


def debug_run(flow_id, version_id, body=None):
    """测试运行调试 — POST /api/v1/flows/{flowId}/versions/{versionId}/debug

    body 示例: {"mockTriggerData": {"sender": "test"}}
    返回 Response 对象（连接失败返回 None）。
    """
    return api("POST", f"/flows/{flow_id}/versions/{version_id}/debug", body)


def done():
    """打印汇总，返回 (pass_count, fail_count)"""
    print(f"\n── 测试结果 ──")
    print(f"  ✅ PASS: {_pass_count}  ❌ FAIL: {_fail_count}")
    return _pass_count, _fail_count
