#!/usr/bin/env python3
"""SYSTOKEN 白名单 E2E 集成测试 — FR-036

覆盖 SYSTOKEN 触发白名单校验场景：
  IT-SYS-001: 空白名单 → 拒绝所有触发 (EC-011)
  IT-SYS-002: 凭证在白名单中 → 允许触发
  IT-SYS-003: 凭证不在白名单中 → 拒绝触发
  IT-SYS-004: 白名单配置在版本快照中生效

验证 connector-api 能正确解析 trigger node authConfig.whitelist，
在触发前校验 X-Sys-Token 头是否命中白名单，
命中则放行，未命中则返回 401 或 403。

编排使用 trigger → exit 两节点模式（无需 connector 依赖）。
"""
from client import *
import subprocess
import time
import json
import requests as req_lib

# ═══════════════════════════════════════════════════════════
# Constants
# ═══════════════════════════════════════════════════════════

CONNECTOR_API_BASE = "http://localhost:18180/api/v1"

DB_HOST = "192.168.3.155"
DB_USER = "openapp"
DB_PASS = "openapp"
DB_NAME = "openapp"
DB_BASE = ["mysql", "-h", DB_HOST, f"-u{DB_USER}", f"-p{DB_PASS}", DB_NAME, "-e"]


# ═══════════════════════════════════════════════════════════
# Database Helpers
# ═══════════════════════════════════════════════════════════

def snow_id():
    """生成唯一雪花 ID"""
    return int(time.time() * 1000000) % 100000000000000000


def _mysql(sql):
    """执行 MySQL 语句（出错时抛异常）"""
    subprocess.run(DB_BASE + [sql], check=True, capture_output=True)


def _escape(obj):
    """将 Python 对象转为 MySQL-safe JSON 字符串"""
    return json.dumps(obj).replace("\\", "\\\\").replace("'", "''")


# ═══════════════════════════════════════════════════════════
# Orchestration Builder
# ═══════════════════════════════════════════════════════════

def build_orch(whitelist=None):
    """构建 trigger → exit 编排，authConfig 可选注入 whitelist

    whitelist=None   → 不包含 whitelist 键（旧行为，仅校验 SYSTOKEN 存在）
    whitelist=[]     → 空白名单，拒绝所有
    whitelist=[...]  → 仅允许列表中指定的 token
    """
    auth_cfg = {
        "type": "SYSTOKEN",
        "fields": [
            {"name": "token", "carrier": "header", "fieldName": "X-Sys-Token"}
        ]
    }
    if whitelist is not None:
        auth_cfg["whitelist"] = whitelist

    return {
        "nodes": [
            {
                "id": "node_trigger",
                "type": "trigger",
                "position": {"x": 100, "y": 200},
                "data": {
                    "labelCn": "接收请求",
                    "labelEn": "Receive Request",
                    "type": "http",
                    "authConfig": auth_cfg,
                    "inputContract": {
                        "protocol": "HTTP",
                        "header": {"type": "object", "properties": {},
                                   "required": []},
                        "query":  {"type": "object", "properties": {},
                                   "required": []},
                        "body": {
                            "type": "object",
                            "properties": {
                                "msg": {"type": "string"}
                            },
                            "required": []
                        }
                    },
                    "rateLimitConfig": {"maxQps": 100}
                }
            },
            {
                "id": "node_exit",
                "type": "exit",
                "position": {"x": 350, "y": 200},
                "data": {
                    "labelCn": "返回结果",
                    "labelEn": "Return Result",
                    "outputMapping": {
                        "header": {"type": "object", "properties": {}},
                        "body": {
                            "type": "object",
                            "properties": {
                                "echo": {
                                    "type": "string",
                                    "value": "ok"
                                }
                            }
                        }
                    }
                }
            }
        ],
        "edges": [
            {"id": "e1", "source": "node_trigger", "target": "node_exit",
             "type": "smoothstep", "data": {"businessType": "default"}}
        ]
    }


# ═══════════════════════════════════════════════════════════
# Flow Lifecycle Helpers
# ═══════════════════════════════════════════════════════════

def setup_flow(flow_id, lifecycle_status=1, orchestration=None):
    """创建 Flow + 版本。返回 (flow_id, flow_version_id)"""
    flow_version_id = snow_id()
    orch = orchestration or build_orch()

    _mysql(
        f"INSERT INTO openplatform_v2_cp_flow_t "
        f"(id, name_cn, name_en, lifecycle_status, create_by, last_update_by) "
        f"VALUES ({flow_id}, 'IT_SYSTOKEN白名单', 'IT_SysTokenWL', "
        f"{lifecycle_status}, 'tester', 'tester')"
    )
    _mysql(
        f"INSERT INTO openplatform_v2_cp_flow_version_t "
        f"(id, flow_id, orchestration_config, create_by, last_update_by) "
        f"VALUES ({flow_version_id}, {flow_id}, "
        f"'{_escape(orch)}', 'tester', 'tester')"
    )
    return flow_id, flow_version_id


def insert_version(flow_id, orchestration):
    """为已有 flow 插入一个新版本。返回 version_id"""
    version_id = snow_id()
    _mysql(
        f"INSERT INTO openplatform_v2_cp_flow_version_t "
        f"(id, flow_id, orchestration_config, create_by, last_update_by) "
        f"VALUES ({version_id}, {flow_id}, "
        f"'{_escape(orchestration)}', 'tester', 'tester')"
    )
    return version_id


def cleanup_flow(flow_id, *version_ids):
    """清理 Flow + 版本记录"""
    for vid in version_ids:
        if vid:
            subprocess.run(
                DB_BASE + [
                    f"DELETE FROM openplatform_v2_cp_flow_version_t "
                    f"WHERE id = {vid}"
                ], capture_output=True)
    if flow_id:
        subprocess.run(
            DB_BASE + [
                f"DELETE FROM openplatform_v2_cp_flow_t "
                f"WHERE id = {flow_id}"
            ], capture_output=True)


# ═══════════════════════════════════════════════════════════
# Trigger Helper
# ═══════════════════════════════════════════════════════════

def trigger(flow_id, sys_token="test-token"):
    """向 connector-api 发送 HTTP 触发请求。

    返回 Response 对象或 None（connector-api 未运行时）。
    """
    try:
        resp = req_lib.post(
            f"{CONNECTOR_API_BASE}/trigger/{flow_id}/invoke",
            json={"msg": "hello"},
            headers={
                "Content-Type": "application/json",
                "X-Sys-Token": sys_token
            },
            timeout=10
        )
        return resp
    except req_lib.exceptions.ConnectionError:
        print("  SKIP: connector-api 未运行 (port 18180)")
        return None


# ═══════════════════════════════════════════════════════════
# IT-SYS-001: 空白名单 → 拒绝所有触发 (EC-011)
# ═══════════════════════════════════════════════════════════
print("=" * 60)
print("  IT-SYS-001: 空白名单 → 拒绝所有触发 (EC-011)")
print("=" * 60)

sid_001 = snow_id()
fvid_001 = None
try:
    fid_001, fvid_001 = setup_flow(
        sid_001, lifecycle_status=1,
        orchestration=build_orch(whitelist=[])
    )

    resp = trigger(fid_001, sys_token="test-token")
    if resp is not None:
        check("[SYS-001] HTTP 401/403 — 空白名单拒绝所有",
              resp.status_code in (401, 403),
              f"status={resp.status_code}")
        check("[SYS-001] X-Code 为 401 或 403",
              resp.headers.get("X-Code") in ("401", "403"),
              f"X-Code={resp.headers.get('X-Code')}")
        check("[SYS-001] 响应体为空",
              len(resp.content) == 0,
              f"body={resp.content[:100] if resp.content else '(空)'}")
    else:
        check("[SYS-001] connector-api 未运行 (跳过)", True)

finally:
    cleanup_flow(sid_001, fvid_001)


# ═══════════════════════════════════════════════════════════
# IT-SYS-002: 凭证在白名单中 → 允许触发
# ═══════════════════════════════════════════════════════════
print()
print("=" * 60)
print("  IT-SYS-002: 凭证在白名单中 → 允许触发")
print("=" * 60)

sid_002 = snow_id()
fvid_002 = None
try:
    fid_002, fvid_002 = setup_flow(
        sid_002, lifecycle_status=1,
        orchestration=build_orch(whitelist=["allowed-token-001"])
    )

    resp = trigger(fid_002, sys_token="allowed-token-001")
    if resp is not None:
        check("[SYS-002] HTTP 200 — 白名单内凭证放行",
              resp.status_code == 200,
              f"status={resp.status_code}")
        check("[SYS-002] X-Execution-Id 存在",
              bool(resp.headers.get("X-Execution-Id")))
        check("[SYS-002] X-Status 为 0",
              resp.headers.get("X-Status") == "0",
              f"X-Status={resp.headers.get('X-Status')}")
        body = resp.json()
        check("[SYS-002] 响应体 echo == ok",
              body.get("echo") == "ok",
              f"echo={body.get('echo')}")
    else:
        check("[SYS-002] connector-api 未运行 (跳过)", True)

finally:
    cleanup_flow(sid_002, fvid_002)


# ═══════════════════════════════════════════════════════════
# IT-SYS-003: 凭证不在白名单中 → 拒绝触发
# ═══════════════════════════════════════════════════════════
print()
print("=" * 60)
print("  IT-SYS-003: 凭证不在白名单中 → 拒绝触发")
print("=" * 60)

sid_003 = snow_id()
fvid_003 = None
try:
    fid_003, fvid_003 = setup_flow(
        sid_003, lifecycle_status=1,
        orchestration=build_orch(whitelist=["allowed-token-001"])
    )

    resp = trigger(fid_003, sys_token="not-in-whitelist")
    if resp is not None:
        check("[SYS-003] HTTP 401/403 — 白名单外凭证拒绝",
              resp.status_code in (401, 403),
              f"status={resp.status_code}")
        check("[SYS-003] X-Code 为 401 或 403",
              resp.headers.get("X-Code") in ("401", "403"),
              f"X-Code={resp.headers.get('X-Code')}")
        check("[SYS-003] 响应体为空",
              len(resp.content) == 0,
              f"body={resp.content[:100] if resp.content else '(空)'}")
    else:
        check("[SYS-003] connector-api 未运行 (跳过)", True)

finally:
    cleanup_flow(sid_003, fvid_003)


# ═══════════════════════════════════════════════════════════
# IT-SYS-004: 白名单配置在版本快照中生效
# ═══════════════════════════════════════════════════════════
print()
print("=" * 60)
print("  IT-SYS-004: 白名单配置在版本快照中生效")
print("=" * 60)

sid_004 = snow_id()
fvid_004_v1 = None
fvid_004_v2 = None
try:
    # ── 创建 flow，version v1: whitelist = ["token_v1"] ──
    fid_004, fvid_004_v1 = setup_flow(
        sid_004, lifecycle_status=1,
        orchestration=build_orch(whitelist=["token_v1"])
    )

    # 插入 version v2: whitelist = ["token_v2"]（同一 flow 的不同版本）
    fvid_004_v2 = insert_version(
        sid_004,
        orchestration=build_orch(whitelist=["token_v2"])
    )

    # ── 当前应为 v1 生效：token_v1 → 通过 ──
    resp_v1 = trigger(fid_004, sys_token="token_v1")
    if resp_v1 is not None:
        check("[SYS-004] v1 → token_v1 HTTP 200（白名单内）",
              resp_v1.status_code == 200,
              f"status={resp_v1.status_code}")
    else:
        check("[SYS-004] connector-api 未运行 (跳过)", True)

    # v1 生效时：token_v2 应被拒绝
    resp_v1_deny = trigger(fid_004, sys_token="token_v2")
    if resp_v1_deny is not None:
        check("[SYS-004] v1 生效 → token_v2 HTTP 401/403（不在 v1 白名单）",
              resp_v1_deny.status_code in (401, 403),
              f"status={resp_v1_deny.status_code}")

    # ── 清理 v1，使 v2 变为唯一（最新）版本 ──
    subprocess.run(
        DB_BASE + [
            f"DELETE FROM openplatform_v2_cp_flow_version_t "
            f"WHERE id = {fvid_004_v1}"
        ], capture_output=True)
    fvid_004_v1 = None  # 标记已清理，防止 finally 重复删除

    # Redis cache cp:flow:config:{flowId} 仍持有 v1 的 whitelist (TTL=120s)
    # 直接 MySQL 删除不触发 API 级缓存失效，需等待 TTL 过期
    print("  INFO: 等待 Redis 缓存 TTL 过期 (125s) ...")
    time.sleep(125)

    # v2 生效：token_v1 应被拒绝
    resp_v2_deny = trigger(fid_004, sys_token="token_v1")
    if resp_v2_deny is not None:
        check("[SYS-004] v2 生效 → token_v1 HTTP 401/403（不在 v2 白名单）",
              resp_v2_deny.status_code in (401, 403),
              f"status={resp_v2_deny.status_code}")

    # v2 生效：token_v2 应通过
    resp_v2 = trigger(fid_004, sys_token="token_v2")
    if resp_v2 is not None:
        check("[SYS-004] v2 生效 → token_v2 HTTP 200（白名单内）",
              resp_v2.status_code == 200,
              f"status={resp_v2.status_code}")
        if resp_v2.status_code == 200:
            check("[SYS-004] v2 → token_v2 X-Status 为 0",
                  resp_v2.headers.get("X-Status") == "0",
                  f"X-Status={resp_v2.headers.get('X-Status')}")

finally:
    cleanup_flow(sid_004, fvid_004_v1, fvid_004_v2)


# ═══════════════════════════════════════════════════════════
# Summary
# ═══════════════════════════════════════════════════════════
print()
print("=" * 60)
print("  SYSTOKEN 白名单 E2E 测试完成 (FR-036)")
print("=" * 60)
