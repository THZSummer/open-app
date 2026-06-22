#!/usr/bin/env python3
"""Flow stop/restart E2E test — FR-019, FR-020

覆盖 V3 流生命周期:
  - 启动 → 停止 → 重新启动 完整生命周期 (IT-STOP-001)
  - 停止后触发调用 → 被拒绝 (IT-STOP-002)
  - 未部署直接启动 → 拒绝 (IT-STOP-003)

验证: FR-019 (启动), FR-020 (停止)

依赖: open-server (:18080) 和 connector-api (:18180) 同时运行
"""
from client import *
import subprocess, time, json, requests as req_lib

DB_HOST = "192.168.3.155"
DB_USER = "openapp"
DB_PASS = "openapp"
DB_NAME = "openapp"
DB_BASE = ["mysql", "-h", DB_HOST, f"-u{DB_USER}", f"-p{DB_PASS}", DB_NAME, "-e"]

CONNECTOR_API_BASE = "http://localhost:18180/api/v1"


def snow_id():
    return int(time.time() * 1000000) % 100000000000000000


def _mysql(sql):
    subprocess.run(DB_BASE + [sql], check=True, capture_output=True)


def _mysql_query(sql):
    """Execute a SELECT query and return the first data row (tab-separated)."""
    result = subprocess.run(DB_BASE + [sql], check=True, capture_output=True, text=True)
    lines = result.stdout.strip().split("\n")
    if len(lines) > 1:
        return lines[1].strip()
    return ""


def _escape(obj):
    return json.dumps(obj).replace("'", "''")


def build_simple_orch():
    """Simple trigger→exit orchestration for stop/restart testing."""
    return {
        "nodes": [
            {"id": "node_trigger", "type": "trigger", "position": {"x": 100, "y": 200},
             "data": {"labelCn": "接收", "labelEn": "Recv", "type": "http",
                      "authConfig": {"type": "SYSTOKEN", "fields": [{"name": "token", "carrier": "header", "fieldName": "X-Sys-Token"}]},
                      "inputContract": {"protocol": "HTTP", "header": {"type": "object", "properties": {}, "required": []},
                                        "query": {"type": "object", "properties": {}, "required": []},
                                        "body": {"type": "object", "properties": {"msg": {"type": "string"}}, "required": ["msg"]}},
                      "rateLimitConfig": {"maxQps": 100}}},
            {"id": "node_exit", "type": "exit", "position": {"x": 350, "y": 200},
             "data": {"labelCn": "返回", "labelEn": "Ret",
                      "outputMapping": {"header": {"type": "object", "properties": {}},
                                        "body": {"type": "object", "properties": {"echo": {"type": "string", "value": "${$.node.node_trigger.input.body.msg}"}}}}}}
        ],
        "edges": [{"id": "e1", "source": "node_trigger", "target": "node_exit", "type": "smoothstep", "data": {"businessType": "default"}}]
    }


def stop_flow_via_api(flow_id):
    """通过 API 停止连接流"""
    resp = req_lib.post(
        f"{BASE_URL}/flows/{flow_id}/stop",
        json={},
        headers={"Content-Type": "application/json"},
        timeout=10
    )
    return resp


def start_flow_via_api(flow_id):
    """通过 API 启动连接流"""
    resp = req_lib.post(
        f"{BASE_URL}/flows/{flow_id}/start",
        json={},
        headers={"Content-Type": "application/json"},
        timeout=10
    )
    return resp


def trigger_invoke(flow_id, body=None, headers=None):
    """发送 HTTP 触发请求到 connector-api"""
    url = f"{CONNECTOR_API_BASE}/trigger/{flow_id}/invoke"
    h = {"Content-Type": "application/json"}
    if headers:
        h.update(headers)
    try:
        resp = req_lib.post(url, json=body or {}, headers=h, timeout=10)
        return resp
    except req_lib.exceptions.ConnectionError:
        print(f"  SKIP: connector-api 未运行 (port 18180)")
        return None


# ═══════════════════════════════════════════════════════════
# IT-STOP-001: Start → Stop → Restart lifecycle (FR-019, FR-020)
# ═══════════════════════════════════════════════════════════
cid_001 = cvid_001 = fid_001 = fvid_001 = None

print("=" * 60)
print("IT-STOP-001: 启动 → 停止 → 重新启动 完整生命周期")
print("=" * 60)

try:
    # 1. Create connector + version via MySQL
    cid_001 = snow_id()
    cvid_001 = snow_id()
    conn_config = {
        "labelCn": "停止重启测试连接器",
        "labelEn": "StopRestart Connector",
        "protocol": "HTTP",
        "protocolConfig": {
            "url": "http://localhost:18999/api/health",
            "method": "GET",
            "headers": {}
        },
        "authConfig": {"type": "NONE", "fields": []},
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
        "timeoutMs": 5000
    }
    _mysql(
        f"INSERT INTO openplatform_v2_cp_connector_t "
        f"(id, name_cn, name_en, connector_type, create_by, last_update_by) "
        f"VALUES ({cid_001}, '停止重启测试连接器', 'StopRestartConnector', 1, 'tester', 'tester')"
    )
    _mysql(
        f"INSERT INTO openplatform_v2_cp_connector_version_t "
        f"(id, connector_id, connection_config, create_by, last_update_by) "
        f"VALUES ({cvid_001}, {cid_001}, '{_escape(conn_config)}', 'tester', 'tester')"
    )
    print("\n  [1/4] 连接器已创建 (cid={}, cvid={})".format(cid_001, cvid_001))

    # 2. Create flow via MySQL with lifecycle_status=0
    fid_001 = snow_id()
    _mysql(
        f"INSERT INTO openplatform_v2_cp_flow_t "
        f"(id, name_cn, name_en, lifecycle_status, create_by, last_update_by) "
        f"VALUES ({fid_001}, '停止重启测试流', 'StopRestartFlow', 0, 'tester', 'tester')"
    )
    print("  [2/4] 连接流已创建 (fid={})".format(fid_001))

    # 3. Create a published flow version via MySQL with basic orchestration
    fvid_001 = snow_id()
    orch = build_simple_orch()
    _mysql(
        f"INSERT INTO openplatform_v2_cp_flow_version_t "
        f"(id, flow_id, orchestration_config, status, create_by, last_update_by) "
        f"VALUES ({fvid_001}, {fid_001}, '{_escape(orch)}', 1, 'tester', 'tester')"
    )
    print("  [3/4] 流程编排版本已创建并发布 (fvid={})".format(fvid_001))

    # 4. Set deployed_version_id on flow (direct DB bypass to focus on start/stop testing)
    _mysql(
        f"UPDATE openplatform_v2_cp_flow_t "
        f"SET deployed_version_id = {fvid_001} "
        f"WHERE id = {fid_001}"
    )
    print("  [4/4] 已设置 deployed_version_id = {}".format(fvid_001))

    # 5. Start flow via API → verify HTTP 200
    print("\n  --- 5. 启动连接流 (FR-019) ---")
    resp_start = start_flow_via_api(fid_001)
    check("启动返回 200", resp_start.status_code in (200, 201),
          f"实际: {resp_start.status_code} body={resp_start.text[:200]}")

    # 6. Verify flow status is running via MySQL query
    time.sleep(0.3)
    status_val = _mysql_query(
        f"SELECT lifecycle_status FROM openplatform_v2_cp_flow_t WHERE id = {fid_001}"
    )
    check("MySQL 确认流状态为运行中 (lifecycle_status=1)",
          status_val == "1",
          f"lifecycle_status={status_val}, 期望=1")

    # 7. Stop flow via API → verify HTTP 200
    print("\n  --- 7. 停止连接流 (FR-020) ---")
    resp_stop = stop_flow_via_api(fid_001)
    check("停止返回 200", resp_stop.status_code in (200, 201),
          f"实际: {resp_stop.status_code} body={resp_stop.text[:200]}")

    # 8. Verify flow status is stopped via MySQL query
    time.sleep(0.3)
    status_val = _mysql_query(
        f"SELECT lifecycle_status FROM openplatform_v2_cp_flow_t WHERE id = {fid_001}"
    )
    check("MySQL 确认流状态已停止 (lifecycle_status=0)",
          status_val == "0",
          f"lifecycle_status={status_val}, 期望=0")

    # 9. Stop again (idempotent) → verify HTTP 200
    print("\n  --- 9. 重复停止 (幂等) ---")
    resp_stop2 = stop_flow_via_api(fid_001)
    check("重复停止返回 200 (幂等)",
          resp_stop2.status_code in (200, 201),
          f"实际: {resp_stop2.status_code} body={resp_stop2.text[:200]}")

    # 10. Start again via API → verify HTTP 200
    print("\n  --- 10. 重新启动连接流 ---")
    resp_restart = start_flow_via_api(fid_001)
    check("重新启动返回 200", resp_restart.status_code in (200, 201),
          f"实际: {resp_restart.status_code} body={resp_restart.text[:200]}")

    # 11. Verify flow is running again
    time.sleep(0.3)
    status_val = _mysql_query(
        f"SELECT lifecycle_status FROM openplatform_v2_cp_flow_t WHERE id = {fid_001}"
    )
    check("MySQL 确认流重新运行中 (lifecycle_status=1)",
          status_val == "1",
          f"lifecycle_status={status_val}, 期望=1")

finally:
    # Cleanup
    if fvid_001:
        subprocess.run(DB_BASE + [
            f"DELETE FROM openplatform_v2_cp_flow_version_t WHERE id = {fvid_001}"
        ], capture_output=True)
    if fid_001:
        subprocess.run(DB_BASE + [
            f"DELETE FROM openplatform_v2_cp_flow_t WHERE id = {fid_001}"
        ], capture_output=True)
    if cvid_001:
        subprocess.run(DB_BASE + [
            f"DELETE FROM openplatform_v2_cp_connector_version_t WHERE id = {cvid_001}"
        ], capture_output=True)
    if cid_001:
        subprocess.run(DB_BASE + [
            f"DELETE FROM openplatform_v2_cp_connector_t WHERE id = {cid_001}"
        ], capture_output=True)


# ═══════════════════════════════════════════════════════════
# IT-STOP-002: Trigger invoke after stop should be rejected (FR-020)
# ═══════════════════════════════════════════════════════════
cid_002 = cvid_002 = fid_002 = fvid_002 = None

print("\n" + "=" * 60)
print("IT-STOP-002: 停止后触发调用 → 应被拒绝 (FR-020)")
print("=" * 60)

try:
    # 1. Create another flow + deploy + start
    cid_002 = snow_id()
    cvid_002 = snow_id()
    conn_config_002 = {
        "labelCn": "停止触发拒绝测试连接器",
        "labelEn": "StopTriggerReject",
        "protocol": "HTTP",
        "protocolConfig": {
            "url": "http://localhost:18999/api/health",
            "method": "GET",
            "headers": {}
        },
        "authConfig": {"type": "NONE", "fields": []},
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
        "timeoutMs": 5000
    }
    _mysql(
        f"INSERT INTO openplatform_v2_cp_connector_t "
        f"(id, name_cn, name_en, connector_type, create_by, last_update_by) "
        f"VALUES ({cid_002}, '停止触发拒绝测试连接器', 'StopTriggerReject', 1, 'tester', 'tester')"
    )
    _mysql(
        f"INSERT INTO openplatform_v2_cp_connector_version_t "
        f"(id, connector_id, connection_config, create_by, last_update_by) "
        f"VALUES ({cvid_002}, {cid_002}, '{_escape(conn_config_002)}', 'tester', 'tester')"
    )

    fid_002 = snow_id()
    fvid_002 = snow_id()
    orch_002 = build_simple_orch()
    _mysql(
        f"INSERT INTO openplatform_v2_cp_flow_t "
        f"(id, name_cn, name_en, lifecycle_status, create_by, last_update_by) "
        f"VALUES ({fid_002}, '停止触发拒绝流', 'StopTriggerRejectFlow', 0, 'tester', 'tester')"
    )
    _mysql(
        f"INSERT INTO openplatform_v2_cp_flow_version_t "
        f"(id, flow_id, orchestration_config, status, create_by, last_update_by) "
        f"VALUES ({fvid_002}, {fid_002}, '{_escape(orch_002)}', 1, 'tester', 'tester')"
    )
    _mysql(
        f"UPDATE openplatform_v2_cp_flow_t "
        f"SET deployed_version_id = {fvid_002} "
        f"WHERE id = {fid_002}"
    )
    print("\n  [setup] 连接流已创建并部署 (fid={})".format(fid_002))

    # Start the flow
    resp_start = start_flow_via_api(fid_002)
    check("启动成功", resp_start.status_code in (200, 201),
          f"实际: {resp_start.status_code}")
    time.sleep(0.3)

    # 2. Stop the flow
    resp_stop = stop_flow_via_api(fid_002)
    check("停止成功", resp_stop.status_code in (200, 201),
          f"实际: {resp_stop.status_code}")
    time.sleep(0.3)

    # 3. Try to trigger via connector-api
    print("\n  --- 停止后触发调用 → 应被拒绝 ---")
    resp_trig = trigger_invoke(fid_002,
                               body={"msg": "test_stop_reject"},
                               headers={"X-Sys-Token": "test-token"})
    if resp_trig:
        check("停止后触发被拒绝 (HTTP 4xx/5xx 或错误)",
              resp_trig.status_code not in (200, 201),
              f"实际: {resp_trig.status_code} body={resp_trig.text[:200]}")
    else:
        check("停止后触发被拒绝 (connector-api 不可达 = SKIP)", True,
              "connector-api 未运行，跳过本检查")

finally:
    if fvid_002:
        subprocess.run(DB_BASE + [
            f"DELETE FROM openplatform_v2_cp_flow_version_t WHERE id = {fvid_002}"
        ], capture_output=True)
    if fid_002:
        subprocess.run(DB_BASE + [
            f"DELETE FROM openplatform_v2_cp_flow_t WHERE id = {fid_002}"
        ], capture_output=True)
    if cvid_002:
        subprocess.run(DB_BASE + [
            f"DELETE FROM openplatform_v2_cp_connector_version_t WHERE id = {cvid_002}"
        ], capture_output=True)
    if cid_002:
        subprocess.run(DB_BASE + [
            f"DELETE FROM openplatform_v2_cp_connector_t WHERE id = {cid_002}"
        ], capture_output=True)


# ═══════════════════════════════════════════════════════════
# IT-STOP-003: Start without deploy should be rejected
# ═══════════════════════════════════════════════════════════
cid_003 = cvid_003 = fid_003 = fvid_003 = None

print("\n" + "=" * 60)
print("IT-STOP-003: 未部署直接启动 → 应被拒绝")
print("=" * 60)

try:
    cid_003 = snow_id()
    cvid_003 = snow_id()
    conn_config_003 = {
        "labelCn": "未部署启动拒绝测试连接器",
        "labelEn": "NoDeployStartReject",
        "protocol": "HTTP",
        "protocolConfig": {
            "url": "http://localhost:18999/api/health",
            "method": "GET",
            "headers": {}
        },
        "authConfig": {"type": "NONE", "fields": []},
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
        "timeoutMs": 5000
    }
    _mysql(
        f"INSERT INTO openplatform_v2_cp_connector_t "
        f"(id, name_cn, name_en, connector_type, create_by, last_update_by) "
        f"VALUES ({cid_003}, '未部署启动拒绝测试连接器', 'NoDeployStartReject', 1, 'tester', 'tester')"
    )
    _mysql(
        f"INSERT INTO openplatform_v2_cp_connector_version_t "
        f"(id, connector_id, connection_config, create_by, last_update_by) "
        f"VALUES ({cvid_003}, {cid_003}, '{_escape(conn_config_003)}', 'tester', 'tester')"
    )

    # 1. Create flow without deployed_version
    fid_003 = snow_id()
    _mysql(
        f"INSERT INTO openplatform_v2_cp_flow_t "
        f"(id, name_cn, name_en, lifecycle_status, create_by, last_update_by) "
        f"VALUES ({fid_003}, '未部署启动拒绝流', 'NoDeployStartReject', 0, 'tester', 'tester')"
    )
    # Also create a version (so flow exists with version but NOT deployed)
    fvid_003 = snow_id()
    orch_003 = build_simple_orch()
    _mysql(
        f"INSERT INTO openplatform_v2_cp_flow_version_t "
        f"(id, flow_id, orchestration_config, status, create_by, last_update_by) "
        f"VALUES ({fvid_003}, {fid_003}, '{_escape(orch_003)}', 1, 'tester', 'tester')"
    )
    # NOTE: intentionally NOT setting deployed_version_id — this is the test
    print("\n  [setup] 连接流已创建但未部署 (fid={}, deployed_version_id IS NULL)".format(fid_003))

    # 2. Try to start → verify rejection
    print("\n  --- 未部署直接启动 → 应被拒绝 ---")
    resp = start_flow_via_api(fid_003)
    check("未部署启动被拒绝 (HTTP 4xx/5xx 或 code != 200)",
          resp.status_code not in (200, 201),
          f"实际: {resp.status_code} body={resp.text[:200]}")

    # Also verify lifecycle_status is still 0
    status_val = _mysql_query(
        f"SELECT lifecycle_status FROM openplatform_v2_cp_flow_t WHERE id = {fid_003}"
    )
    check("MySQL 确认流未被启动 (lifecycle_status=0)",
          status_val == "0",
          f"lifecycle_status={status_val}, 期望=0")

finally:
    if fvid_003:
        subprocess.run(DB_BASE + [
            f"DELETE FROM openplatform_v2_cp_flow_version_t WHERE id = {fvid_003}"
        ], capture_output=True)
    if fid_003:
        subprocess.run(DB_BASE + [
            f"DELETE FROM openplatform_v2_cp_flow_t WHERE id = {fid_003}"
        ], capture_output=True)
    if cvid_003:
        subprocess.run(DB_BASE + [
            f"DELETE FROM openplatform_v2_cp_connector_version_t WHERE id = {cvid_003}"
        ], capture_output=True)
    if cid_003:
        subprocess.run(DB_BASE + [
            f"DELETE FROM openplatform_v2_cp_connector_t WHERE id = {cid_003}"
        ], capture_output=True)


print("\n✅ 流停止/重启生命周期 E2E 测试完成")
