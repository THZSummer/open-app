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
import sys, json, time, subprocess
import requests

# ═══════════════════════════════════════════════════════════
# 配置 — 只改这里
# ═══════════════════════════════════════════════════════════
_API_BASE = "http://localhost:18080/open-server"
TEST_APP_ID = "202606241730488926"
_DEFAULT_USER  = "admin"
_DB = {"host": "192.168.3.155", "user": "openapp", "passwd": "openapp", "db": "openapp"}
_REDIS = {"host": "192.168.3.201", "port": 6379, "password": "openapp"}
_TIMEOUT = 10

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
    usr = _DEFAULT_USER if user is None else user
    if aid is not None:
        h["X-App-Id"] = aid
    if usr is not None:
        h["Cookie"] = f"user_id={usr}"
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
def db(sql, capture=False):
    """执行 SQL。capture=True 返回 stdout"""
    cmd = ["mysql", f"-h{_DB['host']}", f"-u{_DB['user']}", f"-p{_DB['passwd']}", _DB['db'], "-e", sql]
    r = subprocess.run(cmd, capture_output=True, text=True)
    return r.stdout if capture else None

def db_val(sql):
    """执行 SQL 并返回单个值（第一列第一行）"""
    out = db(sql, capture=True)
    lines = out.strip().split('\n')
    return lines[-1].strip() if len(lines) > 1 else None

def redis(*args):
    """执行 redis-cli 命令。如 redis("KEYS", "*") → redis("SET", "k", "v")"""
    cmd = ["redis-cli", "-h", _REDIS["host"], "-p", str(_REDIS["port"]), "-a", _REDIS["password"], "--no-auth-warning"]
    cmd.extend([str(a) for a in args])
    r = subprocess.run(cmd, capture_output=True, text=True)
    return r.stdout.strip() if r.returncode == 0 else None

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
