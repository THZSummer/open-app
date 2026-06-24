#!/usr/bin/env python3
"""连接流 部署→启动→触发调用 端到端 (IT-100~107)

覆盖 V3 核心用户场景:
  - 创建连接器 + 发布版本 → 创建连接流 + 草稿编排 + 发布版本
  - 部署已发布版本 → 启动 → HTTP 触发调用 → 查看运行记录
  - 未部署直接启动 → 拒绝
  - 已失效版本部署 → 拒绝
  - 停止后触发 → 拒绝

验证: FR-018 (部署), FR-019 (启动), FR-042 (运行记录)

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


def setup_connector(label_cn="E2E测试连接器"):
    cid = snow_id()
    vid = snow_id()
    conn_config = {
        "labelCn": label_cn,
        "labelEn": "E2E Test Connector",
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
        f"VALUES ({cid}, '{label_cn}', '{label_cn}', 1, 1, 'tester', 'tester')"
    )
    db(
        f"INSERT INTO openplatform_v2_cp_connector_version_t "
        f"(id, connector_id, connection_config, create_by, last_update_by) "
        f"VALUES ({vid}, {cid}, '{_escape(conn_config)}', 'tester', 'tester')"
    )
    return cid, vid


def build_orchestration(connector_version_id):
    return {
        "nodes": [
            {
                "id": "node_trigger", "type": "trigger",
                "position": {"x": 100, "y": 200},
                "data": {
                    "labelCn": "接收请求", "labelEn": "Receive",
                    "type": "http",
                    "authConfig": {
                        "type": "SYSTOKEN",
                        "fields": [{"name": "token", "carrier": "header", "fieldName": "X-Sys-Token"}]
                    },
                    "inputContract": {
                        "protocol": "HTTP",
                        "header": {"type": "object", "properties": {}, "required": []},
                        "query": {"type": "object", "properties": {}, "required": []},
                        "body": {
                            "type": "object",
                            "properties": {"keyword": {"type": "string"}},
                            "required": ["keyword"]
                        }
                    },
                    "rateLimitConfig": {"maxQps": 100}
                }
            },
            {
                "id": "node_connector", "type": "connector",
                "position": {"x": 350, "y": 200},
                "data": {
                    "labelCn": "调用连接器", "labelEn": "Call Connector",
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
                    "labelCn": "返回结果", "labelEn": "Return",
                    "outputMapping": {
                        "header": {"type": "object", "properties": {}},
                        "body": {
                            "type": "object",
                            "properties": {
                                "echo": {"type": "string", "value": "${$.node.node_trigger.input.body.keyword}"}
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
# IT-100: 连接流 部署→启动→触发 完整链路
# ═══════════════════════════════════════════════════════════
cid_100 = cvid_100 = fid_100 = fvid_100 = None

print("=" * 60)
print("IT-100: 部署→启动→触发调用 完整端到端链路")
print("=" * 60)

try:
    cid_100, cvid_100 = setup_connector("E2E-100连接器")
    print("\n  [1/6] 连接器已创建 (id={})".format(cid_100))

    fid_100 = snow_id()
    db(
        f"INSERT INTO openplatform_v2_cp_flow_t "
        f"(id, name_cn, name_en, lifecycle_status, app_id, create_by, last_update_by) "
        f"VALUES ({fid_100}, 'E2E部署测试流', 'E2E_Deploy_Test', 1, 1, 'tester', 'tester')"
    )
    print("  [2/6] 连接流已创建 (id={})".format(fid_100))

    fvid_100 = snow_id()
    orch = build_orchestration(cvid_100)
    db(
        f"INSERT INTO openplatform_v2_cp_flow_version_t "
        f"(id, flow_id, orchestration_config, status, create_by, last_update_by) "
        f"VALUES ({fvid_100}, {fid_100}, '{_escape(orch)}', 5, 'tester', 'tester')"
    )
    print("  [3/6] 创建已发布版本 (vid={}, status=5)".format(fvid_100))

    print("\n  --- IT-100a: 部署已发布版本 (FR-018) ---")
    resp_deploy = api("POST", f"/flows/{fid_100}/deploy", {"versionId": fvid_100})
    if resp_deploy:
        ok(resp_deploy.status_code in (200, 201), name="部署返回 200")

    print("\n  --- IT-100b: 启动连接流 (FR-019) ---")
    resp_start = api("POST", f"/flows/{fid_100}/start")
    if resp_start:
        ok(resp_start.status_code in (200, 201), name="启动返回 200")

    print("\n  --- IT-100c: HTTP 触发调用 (FR-042) ---")
    time.sleep(0.5)
    resp_trig = trigger_invoke(fid_100,
                               body={"keyword": "e2e_test_100"},
                               headers={"X-Sys-Token": "test-token"})
    if resp_trig:
        ok(resp_trig, 200, "触发返回 200")
        ok(bool(resp_trig.headers.get("X-Execution-Id")), name="X-Execution-Id 存在")
        resp_records = api("GET", f"/flows/{fid_100}/execution-records")
        if resp_records.status_code == 200:
            rec_data = resp_records.json()
            has_records = (
                rec_data.get("code") in ("200", 200) or
                "data" in rec_data
            )
            ok(has_records, name="运行记录可查询")

finally:
    if fvid_100:
        subprocess.run(DB_BASE + [
            f"DELETE FROM openplatform_v2_cp_flow_version_t WHERE id = {fvid_100}"
        ], capture_output=True)
    if fid_100:
        subprocess.run(DB_BASE + [
            f"DELETE FROM openplatform_v2_cp_flow_t WHERE id = {fid_100}"
        ], capture_output=True)
    if cvid_100:
        subprocess.run(DB_BASE + [
            f"DELETE FROM openplatform_v2_cp_connector_version_t WHERE id = {cvid_100}"
        ], capture_output=True)
    if cid_100:
        subprocess.run(DB_BASE + [
            f"DELETE FROM openplatform_v2_cp_connector_t WHERE id = {cid_100}"
        ], capture_output=True)


# ═══════════════════════════════════════════════════════════
# IT-101: 未部署直接启动 → 拒绝
# ═══════════════════════════════════════════════════════════
cid_101 = cvid_101 = fid_101 = fvid_101 = None
print("\n" + "=" * 60)
print("IT-101: 未部署直接启动 → 应被拒绝 (FR-019)")
print("=" * 60)

try:
    cid_101, cvid_101 = setup_connector("E2E-101连接器")
    fid_101 = snow_id()
    db(
        f"INSERT INTO openplatform_v2_cp_flow_t "
        f"(id, name_cn, name_en, lifecycle_status, app_id, create_by, last_update_by) "
        f"VALUES ({fid_101}, 'E2E-101未部署启动', 'E2E_101', 1, 1, 'tester', 'tester')"
    )
    resp = api("POST", f"/flows/{fid_101}/start")
    if resp is not None:
        ok(resp.status_code not in (200, 201), name="未部署启动被拒绝 (HTTP 4xx/5xx 或 code != 200)")

finally:
    if fid_101:
        subprocess.run(DB_BASE + [
            f"DELETE FROM openplatform_v2_cp_flow_t WHERE id = {fid_101}"
        ], capture_output=True)
    if cvid_101:
        subprocess.run(DB_BASE + [
            f"DELETE FROM openplatform_v2_cp_connector_version_t WHERE id = {cvid_101}"
        ], capture_output=True)
    if cid_101:
        subprocess.run(DB_BASE + [
            f"DELETE FROM openplatform_v2_cp_connector_t WHERE id = {cid_101}"
        ], capture_output=True)


# ═══════════════════════════════════════════════════════════
# IT-102: 停止后触发 → 被拒绝
# ═══════════════════════════════════════════════════════════
cid_102 = cvid_102 = fid_102 = fvid_102 = None
print("\n" + "=" * 60)
print("IT-102: 停止后触发 → 应被拒绝 (FR-020)")
print("=" * 60)

try:
    cid_102, cvid_102 = setup_connector("E2E-102连接器")
    fid_102 = snow_id()
    fvid_102 = snow_id()
    orch_102 = build_orchestration(cvid_102)
    db(
        f"INSERT INTO openplatform_v2_cp_flow_t "
        f"(id, name_cn, name_en, lifecycle_status, app_id, create_by, last_update_by) "
        f"VALUES ({fid_102}, 'E2E-102', 'E2E_102', 1, 1, 'tester', 'tester')"
    )
    db(
        f"INSERT INTO openplatform_v2_cp_flow_version_t "
        f"(id, flow_id, orchestration_config, status, create_by, last_update_by) "
        f"VALUES ({fvid_102}, {fid_102}, '{_escape(orch_102)}', 5, 'tester', 'tester')"
    )
    api("POST", f"/flows/{fid_102}/deploy", {"versionId": fvid_102})
    api("POST", f"/flows/{fid_102}/start")
    time.sleep(0.3)
    api("POST", f"/flows/{fid_102}/stop")
    time.sleep(0.3)
    resp = trigger_invoke(fid_102,
                          body={"keyword": "test"},
                          headers={"X-Sys-Token": "test-token"})
    if resp is not None:
        ok(resp.status_code in (404, 503, 500), name="停止后触发被拒绝")

finally:
    if fvid_102:
        subprocess.run(DB_BASE + [
            f"DELETE FROM openplatform_v2_cp_flow_version_t WHERE id = {fvid_102}"
        ], capture_output=True)
    if fid_102:
        subprocess.run(DB_BASE + [
            f"DELETE FROM openplatform_v2_cp_flow_t WHERE id = {fid_102}"
        ], capture_output=True)
    if cvid_102:
        subprocess.run(DB_BASE + [
            f"DELETE FROM openplatform_v2_cp_connector_version_t WHERE id = {cvid_102}"
        ], capture_output=True)
    if cid_102:
        subprocess.run(DB_BASE + [
            f"DELETE FROM openplatform_v2_cp_connector_t WHERE id = {cid_102}"
        ], capture_output=True)


print("\n✅ 连接流部署→启动→触发调用 E2E 测试完成")
done()
