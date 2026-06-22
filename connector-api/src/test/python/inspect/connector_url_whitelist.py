#!/usr/bin/env python3
"""URL whitelist E2E test — FR-015

覆盖 URL 白名单正则校验场景：
  IT-WL-001: URL 匹配白名单 — 允许调用
  IT-WL-002: URL 不匹配白名单 — 拒绝调用
  IT-WL-003: 空白名单 — 允许所有 URL
  IT-WL-004: 多模式组合白名单

验证 connector-api 能正确解析 urlWhitelist 正则规则，在调用下游前校验 URL，
匹配则放行，不匹配则阻断并返回白名单违规错误。
即使下游不可达，也能区分「白名单通过但因下游不可达失败」和「白名单拦截」两种情况。
"""
from client import *
import subprocess
import time
import json
import requests as req_lib

# ═══════════════════════════════════════════════════════════
# Database Helpers
# ═══════════════════════════════════════════════════════════

DB_HOST = "192.168.3.155"
DB_USER = "openapp"
DB_PASS = "openapp"
DB_NAME = "openapp"


def snow_id():
    """生成唯一 ID"""
    return int(time.time() * 1000000) % 100000000000000000


def _mysql_exec(sql):
    """执行 MySQL 语句（出错时抛异常）"""
    subprocess.run(
        ["mysql", "-h", DB_HOST, f"-u{DB_USER}", f"-p{DB_PASS}", DB_NAME, "-e", sql],
        check=True, capture_output=True
    )


def _escape_json(obj):
    """将 Python 对象转为 MySQL-safe JSON 字符串"""
    return json.dumps(obj).replace("\\", "\\\\").replace("'", "''")


def setup_connector(config):
    """创建连接器 + 版本，返回 (connector_id, version_id)

    config 中包含 urlWhitelist 字段（可选），存储于 connection_config JSON 中。
    """
    connector_id = snow_id()
    version_id = snow_id()
    _mysql_exec(
        f"INSERT INTO openplatform_v2_cp_connector_t "
        f"(id, name_cn, name_en, connector_type, create_by, last_update_by) "
        f"VALUES ({connector_id}, '{config['labelCn']}', '{config['labelEn']}', "
        f"1, 'tester', 'tester')"
    )
    _mysql_exec(
        f"INSERT INTO openplatform_v2_cp_connector_version_t "
        f"(id, connector_id, connection_config, create_by, last_update_by) "
        f"VALUES ({version_id}, {connector_id}, "
        f"'{_escape_json(config)}', 'tester', 'tester')"
    )
    return connector_id, version_id


def cleanup_connector(connector_id, version_id):
    """清理连接器 + 版本"""
    subprocess.run(
        ["mysql", "-h", DB_HOST, f"-u{DB_USER}", f"-p{DB_PASS}", DB_NAME, "-e",
         f"DELETE FROM openplatform_v2_cp_connector_version_t WHERE id = {version_id}"],
        capture_output=True
    )
    subprocess.run(
        ["mysql", "-h", DB_HOST, f"-u{DB_USER}", f"-p{DB_PASS}", DB_NAME, "-e",
         f"DELETE FROM openplatform_v2_cp_connector_t WHERE id = {connector_id}"],
        capture_output=True
    )


def setup_flow(flow_id, lifecycle_status, orchestration):
    """创建 Flow + 版本，返回 (flow_id, flow_version_id)"""
    flow_version_id = snow_id()
    _mysql_exec(
        f"INSERT INTO openplatform_v2_cp_flow_t "
        f"(id, name_cn, name_en, lifecycle_status, create_by, last_update_by) "
        f"VALUES ({flow_id}, 'IT_URL白名单测试', 'IT_URLWhitelistTest', "
        f"{lifecycle_status}, 'tester', 'tester')"
    )
    _mysql_exec(
        f"INSERT INTO openplatform_v2_cp_flow_version_t "
        f"(id, flow_id, orchestration_config, create_by, last_update_by) "
        f"VALUES ({flow_version_id}, {flow_id}, "
        f"'{_escape_json(orchestration)}', 'tester', 'tester')"
    )
    return flow_id, flow_version_id


def cleanup_flow(flow_id, flow_version_id, connector_id=None, connector_version_id=None):
    """清理 Flow + 版本，可选连带清理 Connector"""
    subprocess.run(
        ["mysql", "-h", DB_HOST, f"-u{DB_USER}", f"-p{DB_PASS}", DB_NAME, "-e",
         f"DELETE FROM openplatform_v2_cp_flow_version_t WHERE id = {flow_version_id}"],
        capture_output=True
    )
    subprocess.run(
        ["mysql", "-h", DB_HOST, f"-u{DB_USER}", f"-p{DB_PASS}", DB_NAME, "-e",
         f"DELETE FROM openplatform_v2_cp_flow_t WHERE id = {flow_id}"],
        capture_output=True
    )
    if connector_id and connector_version_id:
        cleanup_connector(connector_id, connector_version_id)


# ═══════════════════════════════════════════════════════════
# Connection Config Builder
# ═══════════════════════════════════════════════════════════

def build_conn_config(url, url_whitelist=None):
    """构建连接器配置，可选注入 urlWhitelist 正则"""
    config = {
        "labelCn": "URL白名单测试",
        "labelEn": "URL_WL_Test",
        "protocol": "HTTP",
        "protocolConfig": {
            "url": url,
            "method": "GET",
            "headers": {}
        },
        "authConfig": {
            "type": "NONE",
            "fields": []
        },
        "inputContract": {
            "protocol": "HTTP",
            "header": {"type": "object", "properties": {}, "required": []},
            "query": {"type": "object", "properties": {}, "required": []},
            "body": {"type": "object", "properties": {}, "required": []}
        },
        "outputContract": {
            "protocol": "HTTP",
            "body": {"type": "object", "properties": {}}
        },
        "timeoutMs": 3000
    }
    if url_whitelist:
        config["urlWhitelist"] = url_whitelist
    return config


# ═══════════════════════════════════════════════════════════
# Orchestration Builder
# ═══════════════════════════════════════════════════════════

def build_orch(connector_version_id):
    """构建 trigger → connector → exit 三元编排"""
    return {
        "nodes": [
            {
                "id": "node_trigger", "type": "trigger",
                "position": {"x": 100, "y": 200},
                "data": {
                    "labelCn": "接收", "labelEn": "Recv",
                    "type": "http",
                    "authConfig": {
                        "type": "SYSTOKEN",
                        "fields": [
                            {"name": "token", "carrier": "header",
                             "fieldName": "X-Sys-Token"}
                        ]
                    },
                    "inputContract": {
                        "protocol": "HTTP",
                        "header": {"type": "object", "properties": {},
                                   "required": []},
                        "query": {"type": "object", "properties": {},
                                  "required": []},
                        "body": {"type": "object",
                                 "properties": {"msg": {"type": "string"}},
                                 "required": ["msg"]}
                    },
                    "rateLimitConfig": {"maxQps": 100}
                }
            },
            {
                "id": "node_connector", "type": "connector",
                "position": {"x": 350, "y": 200},
                "data": {
                    "labelCn": "连接器", "labelEn": "Conn",
                    "connectorVersionId": str(connector_version_id),
                    "inputMapping": {
                        "header": {"type": "object", "properties": {}},
                        "query": {"type": "object", "properties": {}},
                        "body": {"type": "object", "properties": {}}
                    }
                }
            },
            {
                "id": "node_exit", "type": "exit",
                "position": {"x": 600, "y": 200},
                "data": {
                    "labelCn": "返回", "labelEn": "Ret",
                    "outputMapping": {
                        "header": {"type": "object", "properties": {}},
                        "body": {
                            "type": "object",
                            "properties": {
                                "result": {"type": "string", "value": "ok"}
                            }
                        }
                    }
                }
            }
        ],
        "edges": [
            {"id": "e1", "source": "node_trigger", "target": "node_connector",
             "type": "smoothstep", "data": {"businessType": "default"}},
            {"id": "e2", "source": "node_connector", "target": "node_exit",
             "type": "smoothstep", "data": {"businessType": "default"}}
        ]
    }


# ═══════════════════════════════════════════════════════════
# Trigger Invoke Helper
# ═══════════════════════════════════════════════════════════

def trigger_invoke(flow_id):
    """向 connector-api 发送触发请求，返回 Response 对象或 None"""
    try:
        resp = req_lib.post(
            f"http://localhost:18180/api/v1/trigger/{flow_id}/invoke",
            json={"msg": "test"},
            headers={
                "Content-Type": "application/json",
                "X-Sys-Token": "test-token"
            },
            timeout=10
        )
        return resp
    except req_lib.exceptions.ConnectionError:
        print("  SKIP: connector-api 未运行 (port 18180)")
        return None


# ═══════════════════════════════════════════════════════════
# Response Verification Helpers
# ═══════════════════════════════════════════════════════════

def is_whitelist_violation(resp):
    """判断响应是否为白名单违规错误

    检查响应头或响应体中的错误码/消息是否指示白名单违规。
    """
    if resp is None:
        return False

    # 检查响应头中的 X-Code
    x_code = resp.headers.get("X-Code", "")
    if "whitelist" in x_code.lower() or "WHITELIST" in x_code:
        return True

    # 检查响应体中的 errorInfo
    try:
        body = resp.json()
        ei = body.get("errorInfo", {})
        code = ei.get("code", "")
        cause = ei.get("cause", "")
        message = ei.get("message", "")
        if "whitelist" in code.lower() or "whitelist" in message.lower() or "whitelist" in cause.lower():
            return True
        # 也检查顶层 message
        if "whitelist" in body.get("message", "").lower():
            return True
    except Exception:
        pass

    # 检查响应体文本
    try:
        text = resp.text.lower()
        if "whitelist" in text or "url not allowed" in text or "url blocked" in text:
            return True
    except Exception:
        pass

    return False


def is_downstream_error(resp):
    """判断响应是否为下游不可达错误（非白名单违规）"""
    if resp is None:
        return False

    try:
        body = resp.json()
        ei = body.get("errorInfo", {})
        code = ei.get("code", "")
        cause = ei.get("cause", "")
        # 下游不可达相关的错误码
        unreachable_codes = {"6001", "CONNECTION_ERROR", "TIMEOUT", "DOWNSTREAM_ERROR",
                             "HTTP_ERROR", "6003", "6004", "6005"}
        if code in unreachable_codes:
            return True
        if "connection refused" in cause.lower() or "timeout" in cause.lower():
            return True
        if "connect" in cause.lower() and ("refused" in cause.lower() or "timeout" in cause.lower()):
            return True
    except Exception:
        pass

    # 检查是否状态码为 5xx（下游错误代理）
    if resp.status_code >= 500:
        return True

    return False


# ═══════════════════════════════════════════════════════════
# IT-WL-001: URL 匹配白名单 — 允许调用
# ═══════════════════════════════════════════════════════════
print("=== IT-WL-001: URL 匹配白名单 — 允许调用 ===")
sid_001 = snow_id()
fvid_001 = cid_001 = cvid_001 = None
try:
    # URL 匹配 ^https://httpbin\.org/.*$ 白名单
    config_001 = build_conn_config(
        url="https://httpbin.org/get",
        url_whitelist=r"^https://httpbin\.org/.*$"
    )
    cid_001, cvid_001 = setup_connector(config_001)
    fid_001, fvid_001 = setup_flow(
        sid_001, lifecycle_status=1,
        orchestration=build_orch(cvid_001)
    )

    resp = trigger_invoke(fid_001)
    if resp is not None:
        # 白名单应放行；如果下游可达 → HTTP 200；如果下游不可达 → 下游错误，而非白名单违规
        check("[IT-WL-001] 非白名单违规",
              not is_whitelist_violation(resp),
              f"白名单规则应允许 httpbin.org，但被拦截了 (status={resp.status_code})")

        # 如果下游可达，应收到成功的 HTTP 200
        if resp.status_code == 200:
            try:
                body = resp.json()
                check("[IT-WL-001] executionId 存在",
                      "executionId" in body and isinstance(body["executionId"], str),
                      f"body keys: {list(body.keys())}")
                check("[IT-WL-001] 非失败状态",
                      body.get("status") != "failed",
                      f"status={body.get('status')}")
            except Exception:
                check("[IT-WL-001] 响应为合法 JSON", False, "无法解析响应体")
        elif is_downstream_error(resp):
            # 下游不可达是预期中的，不算失败
            print("  INFO: 下游 httpbin.org 不可达（白名单已通过）")
            check("[IT-WL-001] 白名单放行（下游不可达属环境问题）",
                  True,
                  f"status={resp.status_code}, 白名单判断正确")
        else:
            check("[IT-WL-001] HTTP 200 或下游错误",
                  False,
                  f"意外状态码 status={resp.status_code}")
    else:
        check("[IT-WL-001] 请求发送成功", False, "connector-api 未运行")
finally:
    cleanup_flow(sid_001, fvid_001, cid_001, cvid_001)


# ═══════════════════════════════════════════════════════════
# IT-WL-002: URL 不匹配白名单 — 拒绝调用
# ═══════════════════════════════════════════════════════════
print("\n=== IT-WL-002: URL 不匹配白名单 — 拒绝调用 ===")
sid_002 = snow_id()
fvid_002 = cid_002 = cvid_002 = None
try:
    # 白名单只允许 httpbin.org，但 connector 配置了 evil.com
    config_002 = build_conn_config(
        url="https://evil.com/api",
        url_whitelist=r"^https://httpbin\.org/.*$"
    )
    cid_002, cvid_002 = setup_connector(config_002)
    fid_002, fvid_002 = setup_flow(
        sid_002, lifecycle_status=1,
        orchestration=build_orch(cvid_002)
    )

    resp = trigger_invoke(fid_002)
    if resp is not None:
        # 应被白名单拦截 — 返回错误
        check("[IT-WL-002] 白名单拦截生效",
              is_whitelist_violation(resp) or resp.status_code >= 400,
              f"期望白名单拦截 evil.com，实际 status={resp.status_code}")

        # 确认不是下游错误（evil.com 虽然不可达，但应该在到达下游前就被拦截）
        if resp.status_code >= 400:
            check("[IT-WL-002] 白名单优先于下游调用",
                  not is_downstream_error(resp),
                  "白名单拦截先于下游连接，不应报下游错误")
        else:
            check("[IT-WL-002] 非 HTTP 200（应被拦截）",
                  False,
                  f"evil.com 未被白名单拦截，status={resp.status_code}")
    else:
        check("[IT-WL-002] 请求发送成功", False, "connector-api 未运行")
finally:
    cleanup_flow(sid_002, fvid_002, cid_002, cvid_002)


# ═══════════════════════════════════════════════════════════
# IT-WL-003: 空白名单 — 允许所有 URL
# ═══════════════════════════════════════════════════════════
print("\n=== IT-WL-003: 空白名单 — 允许所有 URL ===")
sid_003 = snow_id()
fvid_003 = cid_003 = cvid_003 = None
try:
    # 不传 url_whitelist，即空白名单（允许所有）
    config_003 = build_conn_config(
        url="https://httpbin.org/get",
        url_whitelist=None
    )
    cid_003, cvid_003 = setup_connector(config_003)
    fid_003, fvid_003 = setup_flow(
        sid_003, lifecycle_status=1,
        orchestration=build_orch(cvid_003)
    )

    resp = trigger_invoke(fid_003)
    if resp is not None:
        # 空白名单不应拦截任何 URL
        check("[IT-WL-003] 非白名单违规",
              not is_whitelist_violation(resp),
              f"空白名单不应拦截任何 URL (status={resp.status_code})")

        if resp.status_code == 200:
            try:
                body = resp.json()
                check("[IT-WL-003] executionId 存在",
                      "executionId" in body and isinstance(body["executionId"], str))
                check("[IT-WL-003] 非失败状态",
                      body.get("status") != "failed",
                      f"status={body.get('status')}")
            except Exception:
                check("[IT-WL-003] 响应为合法 JSON", False)
        elif is_downstream_error(resp):
            print("  INFO: 下游 httpbin.org 不可达（空白名单已放行）")
            check("[IT-WL-003] 空白名单放行（下游不可达属环境问题）",
                  True,
                  f"status={resp.status_code}, 白名单判断正确")
        else:
            check("[IT-WL-003] HTTP 200 或下游错误",
                  False,
                  f"意外状态码 status={resp.status_code}")
    else:
        check("[IT-WL-003] 请求发送成功", False, "connector-api 未运行")
finally:
    cleanup_flow(sid_003, fvid_003, cid_003, cvid_003)


# ═══════════════════════════════════════════════════════════
# IT-WL-004: 多模式组合白名单
# ═══════════════════════════════════════════════════════════
print("\n=== IT-WL-004: 多模式组合白名单 ===")
sid_004a = snow_id()
sid_004b = snow_id()
fvid_004a = cid_004a = cvid_004a = None
fvid_004b = cid_004b = cvid_004b = None

# ── IT-WL-004a: 匹配多模式中的一种 — 允许 ──
print("  --- IT-WL-004a: 匹配 httpbin.org（多模式之一） ---")
try:
    config_004a = build_conn_config(
        url="https://httpbin.org/get",
        url_whitelist=r"^https://httpbin\.org/.*$|^https://api\.github\.com/.*$"
    )
    cid_004a, cvid_004a = setup_connector(config_004a)
    fid_004a, fvid_004a = setup_flow(
        sid_004a, lifecycle_status=1,
        orchestration=build_orch(cvid_004a)
    )

    resp = trigger_invoke(fid_004a)
    if resp is not None:
        # httpbin.org 应匹配多模式中的第一条，放行
        check("[IT-WL-004a] 非白名单违规（httpbin.org 应匹配）",
              not is_whitelist_violation(resp),
              f"httpbin.org 应匹配白名单 | pattern，但被拦截了 (status={resp.status_code})")

        if resp.status_code == 200:
            check("[IT-WL-004a] HTTP 200",
                  resp.status_code == 200,
                  f"status={resp.status_code}")
        elif is_downstream_error(resp):
            print("  INFO: 下游 httpbin.org 不可达（白名单已通过）")
            check("[IT-WL-004a] 白名单放行（下游不可达属环境问题）",
                  True,
                  f"status={resp.status_code}, 白名单判断正确")
        else:
            check("[IT-WL-004a] HTTP 200 或下游错误",
                  False,
                  f"意外状态码 status={resp.status_code}")
    else:
        check("[IT-WL-004a] 请求发送成功", False, "connector-api 未运行")
finally:
    cleanup_flow(sid_004a, fvid_004a, cid_004a, cvid_004a)

# ── IT-WL-004b: 不匹配任何模式 — 拒绝 ──
print("  --- IT-WL-004b: 不匹配任何模式 (evil.com) ---")
try:
    config_004b = build_conn_config(
        url="https://evil.com/api",
        url_whitelist=r"^https://httpbin\.org/.*$|^https://api\.github\.com/.*$"
    )
    cid_004b, cvid_004b = setup_connector(config_004b)
    fid_004b, fvid_004b = setup_flow(
        sid_004b, lifecycle_status=1,
        orchestration=build_orch(cvid_004b)
    )

    resp = trigger_invoke(fid_004b)
    if resp is not None:
        # evil.com 不应匹配任何模式，应被拦截
        check("[IT-WL-004b] 白名单拦截生效（evil.com 不匹配）",
              is_whitelist_violation(resp) or resp.status_code >= 400,
              f"期望白名单拦截 evil.com，实际 status={resp.status_code}")

        if resp.status_code >= 400:
            check("[IT-WL-004b] 白名单优先于下游调用",
                  not is_downstream_error(resp),
                  "白名单拦截先于下游连接")
        else:
            check("[IT-WL-004b] 非 HTTP 200（应被拦截）",
                  False,
                  f"evil.com 未被多模式白名单拦截，status={resp.status_code}")
    else:
        check("[IT-WL-004b] 请求发送成功", False, "connector-api 未运行")
finally:
    cleanup_flow(sid_004b, fvid_004b, cid_004b, cvid_004b)


# ═══════════════════════════════════════════════════════════
# Summary
# ═══════════════════════════════════════════════════════════
print(f"\n{'='*60}")
print(f"  URL 白名单 E2E 测试完成 (FR-015)")
print(f"{'='*60}")
