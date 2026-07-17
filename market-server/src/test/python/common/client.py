#!/usr/bin/env python3
"""
集成测试统一入口 — 测试脚本只传业务参数，不关心任何基础设施细节。

用法:
    from common.client import api

    resp = api("GET", "/service/open/v2/apps/pending")
"""

import sys
import json
import requests

try:
    from common.config import (
        MARKET_SERVER_BASE,
        TEST_APP_ID, INTERNAL_APP_ID, TEST_COOKIE, TEST_XSRF_TOKEN,
        DB_HOST, DB_PORT, DB_USER, DB_PASS, DB_NAME,
        REQUEST_TIMEOUT,
    )
except ImportError:
    from config import (
        MARKET_SERVER_BASE,
        TEST_APP_ID, INTERNAL_APP_ID, TEST_COOKIE, TEST_XSRF_TOKEN,
        DB_HOST, DB_PORT, DB_USER, DB_PASS, DB_NAME,
        REQUEST_TIMEOUT,
    )

# ═══════════════════════════════════════════════════════════
# 派生配置
# ═══════════════════════════════════════════════════════════
_API_BASE = MARKET_SERVER_BASE
_DB = {"host": DB_HOST, "port": DB_PORT, "user": DB_USER, "passwd": DB_PASS, "db": DB_NAME}
_TIMEOUT = REQUEST_TIMEOUT

# ═══════════════════════════════════════════════════════════
# API — 默认全自动，参数可覆盖
# ═══════════════════════════════════════════════════════════
def api(method, path, body=None, *, headers=None, timeout=None):
    """
    发送 HTTP 请求。
    返回 requests.Response 或 None（连接失败）。
    """
    url = f"{_API_BASE}{path}"
    h = {"Content-Type": "application/json"}

    h["Cookie"] = TEST_COOKIE
    h["X-XSRF-TOKEN"] = TEST_XSRF_TOKEN
    if headers:
        h.update(headers)

    t = timeout if timeout is not None else _TIMEOUT
    try:
        return requests.request(method, url, json=body, headers=h, timeout=t)
    except requests.ConnectionError:
        print("  SKIP: market-server 未运行 (port 18083)")
        return None

# ═══════════════════════════════════════════════════════════
# DB — 直接执行 SQL
# ═══════════════════════════════════════════════════════════
_db_conn = None

def _get_db_conn():
    global _db_conn
    if _db_conn is None or not _db_conn.open:
        import pymysql
        _db_conn = pymysql.connect(
            host=_DB["host"],
            port=_DB.get("port", 3306),
            user=_DB["user"],
            password=_DB["passwd"],
            database=_DB["db"],
            charset="utf8mb4",
            autocommit=True
        )
    return _db_conn


def db(sql):
    """执行 SQL。"""
    try:
        conn = _get_db_conn()
        with conn.cursor() as cursor:
            cursor.execute(sql)
    except Exception as e:
        print(f"  DB ERROR: {e}")


def db_val(sql):
    """执行 SQL 并返回单个值（第一行第一列）。"""
    try:
        conn = _get_db_conn()
        with conn.cursor() as cursor:
            cursor.execute(sql)
            if cursor.description:
                row = cursor.fetchone()
                return str(row[0]) if row and row[0] is not None else None
    except Exception as e:
        print(f"  DB ERROR: {e}")
    return None


# ═══════════════════════════════════════════════════════════
# 断言辅助
# ═══════════════════════════════════════════════════════════
def ok(resp, expected_status=200, name=""):
    """
    断言 HTTP 响应状态码正确。
    """
    if resp is None:
        print(f"  SKIP: {name} — 连接失败")
        return False
    if resp.status_code == expected_status:
        print(f"  PASS: {name}")
        return True
    else:
        detail = f"HTTP {resp.status_code} (期望 {expected_status})"
        try:
            detail += f" body={json.dumps(resp.json(), ensure_ascii=False)[:200]}"
        except Exception:
            pass
        print(f"  FAIL: {name} - {detail}")
        return False
