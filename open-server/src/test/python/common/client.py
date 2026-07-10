#!/usr/bin/env python3
"""
集成测试统一入口 — 测试脚本只传业务参数，不关心任何基础设施细节。

用法:
    from client import api, db, ok, fail, done

    resp = api("POST", "/connectors", {"nameCn": "测试", "nameEn": "Test", "connectorType": 1})
    ok(resp, 200, "创建连接器")

    resp = api("GET", "/connectors/999", app_id="99999")  # 覆盖 app
    ok(resp, 403, "无权限访问")
"""
import sys, json, time
import requests

# 所有配置集中在 config.py — 部署到新环境只改那一个文件
# 兼容两种导入路径:
#   - pytest (from common.client import ...):     common/ 的父目录在 sys.path
#   - e2e standalone (importlib 加载):            common/ 自身在 sys.path
try:
    from common.config import (
        OPEN_SERVER_BASE,
        TEST_APP_ID, INTERNAL_APP_ID, TEST_COOKIE, TEST_XSRF_TOKEN,
        SYSTOKEN_HEADER, SYSTOKEN_VALUE,
        DB_HOST, DB_PORT, DB_USER, DB_PASS, DB_NAME,
        REDIS_PASSWORD, REDIS_CLUSTER_NODES as _REDIS_CLUSTER_NODES,
        CONNECTOR_API_BASE, CONNECTOR_API_HEALTH, MOCK_SERVER_URL, MOCK_SERVER_PARALLEL_URL,
        REQUEST_TIMEOUT,
    )
except ImportError:
    from config import (
        OPEN_SERVER_BASE,
        TEST_APP_ID, INTERNAL_APP_ID, TEST_COOKIE, TEST_XSRF_TOKEN,
        SYSTOKEN_HEADER, SYSTOKEN_VALUE,
        DB_HOST, DB_PORT, DB_USER, DB_PASS, DB_NAME,
        REDIS_PASSWORD, REDIS_CLUSTER_NODES as _REDIS_CLUSTER_NODES,
        CONNECTOR_API_BASE, CONNECTOR_API_HEALTH, MOCK_SERVER_URL, MOCK_SERVER_PARALLEL_URL,
        REQUEST_TIMEOUT,
    )

# ═══════════════════════════════════════════════════════════
# 派生配置
# ═══════════════════════════════════════════════════════════
_API_BASE = OPEN_SERVER_BASE
_DB = {"host": DB_HOST, "port": DB_PORT, "user": DB_USER, "passwd": DB_PASS, "db": DB_NAME}
_TIMEOUT = REQUEST_TIMEOUT

# 预组装的鉴权头（供 e2e 测试直接使用）
AUTH_HEADERS = {"X-App-Id": TEST_APP_ID, "Cookie": TEST_COOKIE, "X-XSRF-TOKEN": TEST_XSRF_TOKEN}

# ═══════════════════════════════════════════════════════════
# 内部状态
# ═══════════════════════════════════════════════════════════
_pass = 0
_fail = 0

# ═══════════════════════════════════════════════════════════
# API — 默认全自动，参数可覆盖
# ═══════════════════════════════════════════════════════════
def api(method, path, body=None, *, app_id=None, user=None, headers=None, timeout=None):
    """
    发送 HTTP 请求。
    - method, path, body: 业务参数
    - app_id, user: 覆盖默认，传 None 去掉对应 header
    - headers: 额外请求头，会合并进默认头
    - timeout: 覆盖默认超时（秒）
    返回 requests.Response 或 None（连接失败）。
    """
    url = f"{_API_BASE}/service/open/v2{path}"
    h = {"Content-Type": "application/json"}

    aid = TEST_APP_ID if app_id is None else app_id
    if aid is not None:
        h["X-App-Id"] = aid
    if user is not None:
        h["Cookie"] = f"user_id={user}"
        h["X-XSRF-TOKEN"] = f"user_id={user}"
    else:
        h["Cookie"] = TEST_COOKIE
        h["X-XSRF-TOKEN"] = TEST_XSRF_TOKEN
    if headers:
        h.update(headers)

    t = timeout if timeout is not None else _TIMEOUT
    try:
        return requests.request(method, url, json=body, headers=h, timeout=t)
    except requests.ConnectionError:
        print(f"  SKIP: open-server 未运行 (port 18080)")
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

# ═══════════════════════════════════════════════════════════
# Lookup 配置管理
# ═══════════════════════════════════════════════════════════

_LOOKUP_PATH = "CEC.Open"
_LOOKUP_CLASSIFY = "Connector.Platform.Config"

def set_lookup_config(item_code: str, value: str):
    """设置 Connector.Platform.Config 下指定 item_code 的值（测试用）"""
    db(f"""UPDATE openplatform_lookup_item_t i
JOIN openplatform_lookup_classify_t c ON i.classify_id = c.classify_id
SET i.item_value = '{value}'
WHERE c.path = '{_LOOKUP_PATH}' AND c.classify_code = '{_LOOKUP_CLASSIFY}'
AND i.item_code = '{item_code}'""")


# ═══════════════════════════════════════════════════════════
# 断言
# ═══════════════════════════════════════════════════════════
def ok(resp_or_cond, expected=None, name=""):
    """
    断言通过。
    - ok(resp, 200, "创建成功") → 检查 HTTP 状态码
    - ok(resp, 200, ...) 同时检查 body.code == "200"
    - ok(True, name="条件成立") → 直接布尔断言
    """
    global _pass, _fail
    if isinstance(resp_or_cond, bool):
        if isinstance(expected, str):
            name = expected
        cond, detail = resp_or_cond, ""
    elif resp_or_cond is None:
        cond, detail = False, "连接失败"
    elif isinstance(expected, int):
        ok_http = resp_or_cond.status_code == expected
        try:
            body = resp_or_cond.json()
            ok_code = body.get("code") in ("200", 200)
        except Exception:
            ok_code = True
        cond = ok_http and ok_code
        detail = f"HTTP {resp_or_cond.status_code} (期望 {expected})"
    else:
        cond, detail = bool(resp_or_cond), ""
    if cond:
        _pass += 1
        tag = f"✅ PASS: {name}" + (f" - {detail}" if detail else "")
    else:
        _fail += 1
        tag = f"❌ FAIL: {name}" + (f" - {detail}" if detail else "")
        if resp_or_cond is not None and not isinstance(resp_or_cond, bool):
            try:
                tag += f"\n       body={json.dumps(resp_or_cond.json(), ensure_ascii=False)[:200]}"
            except Exception:
                pass
    print(tag)

def fail(name, detail=""):
    """直接标记失败"""
    global _fail
    _fail += 1
    print(f"❌ FAIL: {name}" + (f" - {detail}" if detail else ""))

# ═══════════════════════════════════════════════════════════
# 收尾
# ═══════════════════════════════════════════════════════════
def done():
    """打印汇总，非零退出"""
    print(f"\n── 结果 ──")
    print(f"  ✅ PASS: {_pass}  ❌ FAIL: {_fail}")
    if _fail > 0:
        sys.exit(1)
