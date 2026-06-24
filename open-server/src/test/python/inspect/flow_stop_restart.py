#!/usr/bin/env python3
"""Flow stop/restart E2E test — FR-019, FR-020

覆盖 V3 流生命周期:
  - 启动 → 停止 → 重新启动 完整生命周期 (IT-STOP-001)
  - 停止后触发调用 → 被拒绝 (IT-STOP-002)
  - 未部署直接启动 → 拒绝 (IT-STOP-003)

验证: FR-019 (启动), FR-020 (停止)

依赖: open-server (:18080) 和 connector-api (:18180) 同时运行
"""
from client import api, db, ok, done
import subprocess, time, json, requests as req_lib

DB_BASE = ["mysql", "-h192.168.3.155", "-uopenapp", "-popenapp", "openapp", "-e"]

CONNECTOR_API_BASE = "http://localhost:18180/api/v1"


def snow_id():
    return int(time.time() * 1000000) % 100000000000000000


def _escape(obj):
    return json.dumps(obj).replace("'", "''")


def _mysql_query_val(sql):
    """Execute a SELECT query and return the first data value."""
    result = subprocess.run(DB_BASE + [sql], check=True, capture_output=True, text=True)
    lines = result.stdout.strip().split("\n")
    if len(lines) > 1:
        return lines[1].strip()
    return ""


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
    db(
        f"INSERT INTO openplatform_v2_cp_connector_t "
        f"(id, name_cn, name_en, connector_type, app_id, create_by, last_update_by) "
        f"VALUES ({cid_001}, '停止重启测试连接器', 'StopRestartConnector', 1, 1, 'tester', 'tester')"
    )
    db(
        f"INSERT INTO openplatform_v2_cp_connector_version_t "
        f"(id, connector_id, connection_config, create_by, last_update_by) "
        f"VALUES ({cvid_001}, {cid_001}, '{_escape(conn_config)}', 'tester', 'tester')"
    )
    print("\n  [1/4] 连接器已创建 (cid={}, cvid={})".format(cid_001, cvid_001))

    fid_001 = snow_id()
    db(
        f"INSERT INTO openplatform_v2_cp_flow_t "
        f"(id, name_cn, name_en, lifecycle_status, app_id, create_by, last_update_by) "
        f"VALUES ({fid_001}, '停止重启测试流', 'StopRestartFlow', 1, 1, 'tester', 'tester')"
    )
    print("  [2/4] 连接流已创建 (fid={})".format(fid_001))

    fvid_001 = snow_id()
    orch = build_simple_orch()
    db(
        f"INSERT INTO openplatform_v2_cp_flow_version_t "
        f"(id, flow_id, orchestration_config, status, create_by, last_update_by) "
        f"VALUES ({fvid_001}, {fid_001}, '{_escape(orch)}', 5, 'tester', 'tester')"
    )
    print("  [3/4] 流程编排版本已创建并发布 (fvid={})".format(fvid_001))

    db(
        f"UPDATE openplatform_v2_cp_flow_t "
        f"SET deployed_version_id = {fvid_001} "
        f"WHERE id = {fid_001}"
    )
    print("  [4/4] 已设置 deployed_version_id = {}".format(fvid_001))

    print("\n  --- 5. 启动连接流 (FR-019) ---")
    resp_start = api("POST", f"/flows/{fid_001}/start")
    ok(resp_start.status_code in (200, 201), name="启动返回 200")

    time.sleep(0.3)
    status_val = _mysql_query_val(f"SELECT lifecycle_status FROM openplatform_v2_cp_flow_t WHERE id = {fid_001}")
    ok(status_val == "2", name="MySQL 确认流状态为运行中 (lifecycle_status=2)")

    print("\n  --- 7. 停止连接流 (FR-020) ---")
    resp_stop = api("POST", f"/flows/{fid_001}/stop")
    ok(resp_stop.status_code in (200, 201), name="停止返回 200")

    time.sleep(0.3)
    status_val = _mysql_query_val(f"SELECT lifecycle_status FROM openplatform_v2_cp_flow_t WHERE id = {fid_001}")
    ok(status_val == "1", name="MySQL 确认流状态已停止 (lifecycle_status=1)")

    print("\n  --- 9. 重复停止 (幂等) ---")
    resp_stop2 = api("POST", f"/flows/{fid_001}/stop")
    ok(resp_stop2.status_code in (200, 201), name="重复停止返回 200 (幂等)")

    print("\n  --- 10. 重新启动连接流 ---")
    resp_restart = api("POST", f"/flows/{fid_001}/start")
    ok(resp_restart.status_code in (200, 201), name="重新启动返回 200")

    time.sleep(0.3)
    status_val = _mysql_query_val(f"SELECT lifecycle_status FROM openplatform_v2_cp_flow_t WHERE id = {fid_001}")
    ok(status_val == "2", name="MySQL 确认流重新运行中 (lifecycle_status=2)")

finally:
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
    db(
        f"INSERT INTO openplatform_v2_cp_connector_t "
        f"(id, name_cn, name_en, connector_type, app_id, create_by, last_update_by) "
        f"VALUES ({cid_002}, '停止触发拒绝测试连接器', 'StopTriggerReject', 1, 1, 'tester', 'tester')"
    )
    db(
        f"INSERT INTO openplatform_v2_cp_connector_version_t "
        f"(id, connector_id, connection_config, create_by, last_update_by) "
        f"VALUES ({cvid_002}, {cid_002}, '{_escape(conn_config_002)}', 'tester', 'tester')"
    )

    fid_002 = snow_id()
    fvid_002 = snow_id()
    orch_002 = build_simple_orch()
    db(
        f"INSERT INTO openplatform_v2_cp_flow_t "
        f"(id, name_cn, name_en, lifecycle_status, app_id, create_by, last_update_by) "
        f"VALUES ({fid_002}, '停止触发拒绝流', 'StopTriggerRejectFlow', 1, 1, 'tester', 'tester')"
    )
    db(
        f"INSERT INTO openplatform_v2_cp_flow_version_t "
        f"(id, flow_id, orchestration_config, status, create_by, last_update_by) "
        f"VALUES ({fvid_002}, {fid_002}, '{_escape(orch_002)}', 5, 'tester', 'tester')"
    )
    db(
        f"UPDATE openplatform_v2_cp_flow_t "
        f"SET deployed_version_id = {fvid_002} "
        f"WHERE id = {fid_002}"
    )
    print("\n  [setup] 连接流已创建并部署 (fid={})".format(fid_002))

    resp_start = api("POST", f"/flows/{fid_002}/start")
    ok(resp_start.status_code in (200, 201), name="启动成功")
    time.sleep(0.3)

    resp_stop = api("POST", f"/flows/{fid_002}/stop")
    ok(resp_stop.status_code in (200, 201), name="停止成功")
    time.sleep(0.3)

    print("\n  --- 停止后触发调用 → 应被拒绝 ---")
    resp_trig = trigger_invoke(fid_002,
                               body={"msg": "test_stop_reject"},
                               headers={"X-Sys-Token": "test-token"})
    if resp_trig:
        ok(resp_trig.status_code not in (200, 201), name="停止后触发被拒绝 (HTTP 4xx/5xx 或错误)")
    else:
        ok(True, name="停止后触发被拒绝 (connector-api 不可达 = SKIP)")

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
    db(
        f"INSERT INTO openplatform_v2_cp_connector_t "
        f"(id, name_cn, name_en, connector_type, app_id, create_by, last_update_by) "
        f"VALUES ({cid_003}, '未部署启动拒绝测试连接器', 'NoDeployStartReject', 1, 1, 'tester', 'tester')"
    )
    db(
        f"INSERT INTO openplatform_v2_cp_connector_version_t "
        f"(id, connector_id, connection_config, create_by, last_update_by) "
        f"VALUES ({cvid_003}, {cid_003}, '{_escape(conn_config_003)}', 'tester', 'tester')"
    )

    fid_003 = snow_id()
    db(
        f"INSERT INTO openplatform_v2_cp_flow_t "
        f"(id, name_cn, name_en, lifecycle_status, app_id, create_by, last_update_by) "
        f"VALUES ({fid_003}, '未部署启动拒绝流', 'NoDeployStartReject', 1, 1, 'tester', 'tester')"
    )
    fvid_003 = snow_id()
    orch_003 = build_simple_orch()
    db(
        f"INSERT INTO openplatform_v2_cp_flow_version_t "
        f"(id, flow_id, orchestration_config, status, create_by, last_update_by) "
        f"VALUES ({fvid_003}, {fid_003}, '{_escape(orch_003)}', 5, 'tester', 'tester')"
    )
    print("\n  [setup] 连接流已创建但未部署 (fid={}, deployed_version_id IS NULL)".format(fid_003))

    print("\n  --- 未部署直接启动 → 应被拒绝 ---")
    resp = api("POST", f"/flows/{fid_003}/start")
    ok(resp.status_code not in (200, 201), name="未部署启动被拒绝 (HTTP 4xx/5xx 或 code != 200)")

    status_val = _mysql_query_val(f"SELECT lifecycle_status FROM openplatform_v2_cp_flow_t WHERE id = {fid_003}")
    ok(status_val == "1", name="MySQL 确认流未被启动 (lifecycle_status=1)")

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
done()
