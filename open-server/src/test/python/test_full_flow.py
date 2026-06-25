#!/usr/bin/env python3
"""
全流程端到端测试 — 模拟用户从零搭建连接器平台 V3

Phase 0: 环境准备（重启服务 — 手动执行）
Phase 1: 连接器发布   — create → draft → 配置Mock → publish
Phase 2: 连接流编排   — create → draft → 编排（引用连接器）
Phase 3: 草稿调试     — debug draft
Phase 4: 发布+审批    — publish → 两级审批（Cookie: tester）
Phase 5: 发布后验证   — debug 已发布版本
Phase 6: 部署+调用    — deploy → start → HTTP trigger → 查记录 → stop

运行前提:
  - open-server 运行在 localhost:18080
  - connector-api 运行在 localhost:18180

用法:
  cd open-server/src/test/python
  python3 test_full_flow.py
  KEEP_TEST_DATA=1 python3 test_full_flow.py  # 保留数据
"""
import os
import sys
import json
import time
import threading
import importlib.util
from http.server import HTTPServer, BaseHTTPRequestHandler

TEST_DIR = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(TEST_DIR, "inspect"))

# ── 加载 open-server client ──
_spec = importlib.util.spec_from_file_location(
    "inspect_client", os.path.join(TEST_DIR, "inspect", "client.py")
)
_osm = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(_osm)
os_api = _osm.api
os_db = _osm.db
os_db_val = _osm.db_val
os_ok = _osm.ok
os_fail = _osm.fail
os_done = _osm.done
TEST_APP_ID = _osm.TEST_APP_ID

import requests
CONNECTOR_API = "http://localhost:18180/api/v1"
KEEP = os.environ.get("KEEP_TEST_DATA", "") == "1"

# ═══════════════════════════════════════════════════════════
# Mock HTTP Server
# ═══════════════════════════════════════════════════════════

class MockServer:
    def __init__(self, port=18980):
        self.port = port
        self._server = None
        self._thread = None

    def _make_handler(self):
        class H(BaseHTTPRequestHandler):
            def log_message(self, f, *a):
                pass
            def _respond(self, code, body):
                self.send_response(code)
                self.send_header("Content-Type", "application/json")
                self.end_headers()
                self.wfile.write(json.dumps(body).encode())
            def _dispatch(self, method):
                path = self.path.split("?")[0]
                body = {}
                if method == "POST":
                    cl = int(self.headers.get("Content-Length", 0))
                    if cl > 0:
                        try:
                            body = json.loads(self.rfile.read(cl))
                        except Exception:
                            body = {}
                if path == "/api/health":
                    self._respond(200, {"status": "ok", "server": "full-flow-mock"})
                elif path == "/api/echo":
                    self._respond(200, {"code": 0, "data": {"echo": body, "path": "/api/echo"}})
                else:
                    self._respond(200, {"code": 0, "data": {"path": path}})
            do_GET = lambda s: s._dispatch("GET")
            do_POST = lambda s: s._dispatch("POST")
        return H

    def start(self, timeout=10):
        self._server = HTTPServer(("localhost", self.port), self._make_handler())
        self._thread = threading.Thread(target=self._server.serve_forever, daemon=True)
        self._thread.start()
        for _ in range(timeout * 2):
            try:
                r = requests.get(f"http://localhost:{self.port}/api/health", timeout=1)
                if r.status_code == 200:
                    print(f"  ✅ Mock Server ready (port {self.port})")
                    return True
            except Exception:
                pass
            time.sleep(0.5)
        return False

    def stop(self):
        if self._server:
            self._server.shutdown()
        print(f"  🛑 Mock Server stopped")

# ═══════════════════════════════════════════════════════════
# Helpers
# ═══════════════════════════════════════════════════════════

def snow_id():
    return int(time.time() * 1000000) % 100000000000000000

def api_connector(method, path, body=None, headers=None):
    url = f"{CONNECTOR_API}{path}"
    h = {"Content-Type": "application/json"}
    if headers:
        h.update(headers)
    try:
        if method == "POST":
            return requests.post(url, json=body, headers=h, timeout=10)
        else:
            return requests.get(url, headers=h, timeout=10)
    except requests.ConnectionError:
        return None

step_num = [0]
def step(desc, fn):
    """执行一个步骤，自动编号并打印"""
    step_num[0] += 1
    no = step_num[0]
    print(f"\n  [{no}] {desc}")
    try:
        return fn()
    except Exception as e:
        os_fail(f"Step {no} 异常: {e}")
        return False

def check_ok(resp, desc):
    if resp is None:
        os_fail(f"{desc}: 连接失败")
        return False
    if resp.status_code == 200:
        try:
            body = resp.json()
            if body.get("code") in ("200", 200):
                print(f"  ✅ {desc}")
                return True
            else:
                os_fail(f"{desc}: code={body.get('code')}, msg={body.get('messageZh', '')}")
                return False
        except Exception:
            print(f"  ✅ {desc} (HTTP 200)")
            return True
    else:
        try:
            body = resp.json()
            detail = f"HTTP {resp.status_code}, code={body.get('code')}, msg={body.get('messageZh', '')}"
        except Exception:
            detail = f"HTTP {resp.status_code}, body={resp.text[:200]}"
        os_fail(f"{desc}: {detail}")
        return False

def get_data(resp):
    try:
        return resp.json().get("data", {})
    except Exception:
        return {}

# ═══════════════════════════════════════════════════════════
# Test
# ═══════════════════════════════════════════════════════════

def test_full_flow():
    print("=" * 60)
    print("  全流程端到端测试 — 连接器平台 V3")
    print("=" * 60)

    # ── Phase 0 检查 ──
    print("\n── Phase 0: 环境检查 ──")
    r = os_api("GET", "/connectors")
    if r is None:
        print("❌ open-server 未运行! 请先执行:")
        print("   cd /home/usb/wks/open-app && bash open-server/scripts/restart.sh")
        print("   cd /home/usb/wks/open-app && bash connector-api/scripts/restart.sh")
        return 1
    print("  ✅ open-server 就绪")

    try:
        r2 = requests.get("http://localhost:18180/actuator/health", timeout=5)
        if r2.status_code == 200:
            print("  ✅ connector-api 就绪")
        else:
            print(f"  ⚠️ connector-api 状态异常: HTTP {r2.status_code}")
    except Exception:
        print("  ❌ connector-api 未运行!")
        return 1

    tpl = os_db_val("SELECT id FROM openplatform_v2_approval_flow_t WHERE code = 'connector_flow_version_publish' LIMIT 1")
    if tpl:
        print(f"  ✅ 审批流模板存在 (id={tpl})")
    else:
        print("  ⚠️ 审批流模板不存在，尝试创建...")
        r = os_api("POST", "/approval-flows", {
            "code": "connector_flow_version_publish",
            "nameCn": "连接器流版本发布审批",
            "nameEn": "connector_flow_version_publish",
            "nodes": [{"userId": "tester", "userName": "Test Approver"}]
        })
        if r and r.status_code in (200, 201):
            print("  ✅ 审批流模板创建成功")

    # ── 启动 Mock ──
    print("\n── 启动 Mock Server ──")
    mock = MockServer(port=18980)
    if not mock.start():
        print("❌ Mock Server 启动失败")
        return 1

    cid = conn_vid = fid = fvid = aid = None
    failed = False

    try:
        # ═══════════════════════════════════════════════════
        # Phase 1: 连接器发布
        # ═══════════════════════════════════════════════════
        print("\n── Phase 1: 连接器发布 ──")

        def s1():
            nonlocal cid
            cid = snow_id()
            r = os_api("POST", "/connectors", {
                "nameCn": "全流程测试连接器",
                "nameEn": "full-flow-connector",
                "connectorType": 1
            })
            if not check_ok(r, "CREATE 连接器"): return False
            d = get_data(r)
            if str(d.get("connectorId")) != str(cid):
                cid = int(d.get("connectorId"))
            print(f"    connectorId={cid}")
            if d.get("status") not in (1, "1"):
                os_fail(f"status={d.get('status')}, 期望=1")
                return False
            return True

        def s2():
            nonlocal conn_vid
            r = os_api("POST", f"/connectors/{cid}/versions", {})
            if not check_ok(r, "CREATE 草稿版本 (connector)"): return False
            # API 不返回 data，需要查列表获取 versionId
            r2 = os_api("GET", f"/connectors/{cid}/versions")
            if not check_ok(r2, "GET 版本列表"): return False
            vlist = get_data(r2)
            if isinstance(vlist, dict):
                vlist = vlist.get("items", vlist.get("data", []))
            if isinstance(vlist, list) and len(vlist) > 0:
                conn_vid = int(vlist[0].get("versionId", vlist[0].get("id", 0)))
                print(f"    connectorVersionId={conn_vid}")
                return True
            os_fail("版本列表为空")
            return False

        def s3():
            r = os_api("PUT", f"/connectors/{cid}/versions/{conn_vid}", {
                "connectionConfig": {
                    "protocol": "HTTP",
                    "protocolConfig": {"url": "http://localhost:18980/api/echo", "method": "POST"},
                    "timeoutMs": 5000
                }
            })
            return check_ok(r, "UPDATE 连接器配置 -> Mock")

        def s4():
            r = os_api("PUT", f"/connectors/{cid}/versions/{conn_vid}/publish")
            if not check_ok(r, "PUBLISH 连接器版本"): return False
            r2 = os_api("GET", f"/connectors/{cid}/versions/{conn_vid}")
            if not check_ok(r2, "确认已发布"): return False
            d = get_data(r2)
            if d.get("status") not in (2, "2"):
                os_fail(f"status={d.get('status')}, 期望=2 (已发布)")
                return False
            return True

        if not step("CREATE 连接器", s1): failed = True
        if not failed and not step("CREATE 草稿版本", s2): failed = True
        if not failed and not step("UPDATE 指向 Mock", s3): failed = True
        if not failed and not step("PUBLISH 版本", s4): failed = True

        if not failed:
            print("  ✅ Phase 1 完成: 连接器已发布")
        else:
            print("  ❌ Phase 1 失败")

        # ═══════════════════════════════════════════════════
        # Phase 2: 连接流编排
        # ═══════════════════════════════════════════════════
        if not failed:
            print("\n── Phase 2: 连接流编排 ──")

        def s5():
            nonlocal fid
            fid = snow_id()
            r = os_api("POST", "/flows", {
                "nameCn": "全流程测试连接流",
                "nameEn": "full-flow-test"
            })
            if not check_ok(r, "CREATE 连接流"): return False
            d = get_data(r)
            if str(d.get("flowId")) != str(fid):
                fid = int(d.get("flowId"))
            print(f"    flowId={fid}")
            if d.get("lifecycleStatus") not in (1, "1"):
                os_fail(f"lifecycleStatus={d.get('lifecycleStatus')}, 期望=1")
                return False
            return True

        def s6():
            nonlocal fvid
            r = os_api("POST", f"/flows/{fid}/versions", {})
            if not check_ok(r, "CREATE 草稿版本 (flow)"): return False
            d = get_data(r)
            fvid = int(d.get("versionId", d.get("id", 0)))
            print(f"    flowVersionId={fvid}")
            return True

        def s7():
            r = os_api("PUT", f"/flows/{fid}/versions/{fvid}", {
                "orchestrationConfig": {
                    "trigger": {},
                    "nodes": [
                        {"id": "trigger", "type": "trigger", "data": {
                            "type": "http",
                            "subType": "http",
                            "inputContract": {
                                "protocol": "HTTP",
                                "header": {"type": "object", "properties": {}, "required": []},
                                "query": {"type": "object", "properties": {}, "required": []},
                                "body": {"type": "object", "properties": {"message": {"type": "string"}}, "required": []}
                            },
                            "authConfig": {
                                "type": "SYSTOKEN",
                                "fields": [{"name": "token", "carrier": "header", "fieldName": "X-Sys-Token"}]
                            }
                        }},
                        {"id": "conn1", "type": "connector", "data": {
                            "connectorId": str(cid),
                            "connectorVersionId": str(conn_vid)
                        }},
                        {"id": "exit", "type": "exit", "data": {
                            "outputMapping": {"body": {"echo": "{{conn1.data}}"}}
                        }}
                    ],
                    "edges": [
                        {"source": "trigger", "target": "conn1"},
                        {"source": "conn1", "target": "exit"}
                    ]
                }
            })
            return check_ok(r, "UPDATE 编排配置")

        if not failed:
            if not step("CREATE 连接流", s5): failed = True
        if not failed:
            if not step("CREATE 草稿版本", s6): failed = True
        if not failed:
            if not step("UPDATE 编排(引用连接器)", s7): failed = True

        if not failed:
            print("  ✅ Phase 2 完成: 连接流已编排")
        else:
            print("  ❌ Phase 2 失败")

        # ═══════════════════════════════════════════════════
        # Phase 3: 草稿调试
        # ═══════════════════════════════════════════════════
        if not failed:
            print("\n── Phase 3: 草稿调试 ──")

        def s8():
            r = os_api("POST", f"/flows/{fid}/versions/{fvid}/debug", {
                "triggerData": {"message": "hello-draft-debug"}
            })
            return check_ok(r, "DEBUG 草稿版本")

        if not failed:
            if not step("DEBUG 草稿版本", s8): failed = True

        if not failed:
            print("  ✅ Phase 3 完成: 调试通过")
        else:
            print("  ❌ Phase 3 失败")

        # ═══════════════════════════════════════════════════
        # Phase 4: 发布 + 审批
        # ═══════════════════════════════════════════════════
        if not failed:
            print("\n── Phase 4: 发布 + 审批 ──")

        def s9():
            r = os_api("POST", f"/flows/{fid}/versions/{fvid}/publish")
            if not check_ok(r, "PUBLISH (提交审批)"): return False
            r2 = os_api("GET", f"/flows/{fid}/versions/{fvid}")
            if not check_ok(r2, "确认待审批"): return False
            st = get_data(r2).get("status")
            if st not in (2, "2"):
                os_fail(f"version status={st}, 期望=2 (待审批)")
                return False
            print(f"    status=2 (待审批)")
            return True

        def s10():
            nonlocal aid
            # 从 API 查审批记录
            r = os_api("GET", "/approvals/pending?businessType=connector_flow_version_publish")
            if not check_ok(r, "查询审批记录"): return False
            items = get_data(r)
            if isinstance(items, dict):
                items = items.get("items", items.get("data", []))
            if isinstance(items, list):
                for item in items:
                    if str(item.get("businessId")) == str(fvid):
                        aid = item.get("id")
                        break
            # fallback: DB
            if not aid:
                aid = os_db_val(
                    f"SELECT id FROM openplatform_v2_approval_record_t "
                    f"WHERE business_type='connector_flow_version_publish' AND business_id={fvid} "
                    f"ORDER BY create_time DESC LIMIT 1"
                )
            if not aid:
                os_fail("未找到审批记录")
                return False
            print(f"    approvalId={aid}")
            return True

        def s11():
            r = os_api("POST", f"/approvals/{aid}/approve",
                       {"comment": "场景级审批通过"}, user="tester")
            if not check_ok(r, "第一级审批 (scene, tester)"):
                # check if already past this level
                r2 = os_api("GET", f"/approvals/{aid}")
                d = get_data(r2)
                print(f"    当前状态: currentNode={d.get('currentNode')}, status={d.get('status')}")
                return False
            return True

        def s12():
            r = os_api("POST", f"/approvals/{aid}/approve",
                       {"comment": "全局级审批通过"}, user="tester")
            if not check_ok(r, "第二级审批 (global, tester)"):
                r2 = os_api("GET", f"/approvals/{aid}")
                d = get_data(r2)
                print(f"    当前状态: status={d.get('status')}")
                return False
            return True

        def s13():
            time.sleep(0.5)
            r = os_api("GET", f"/flows/{fid}/versions/{fvid}")
            if not check_ok(r, "确认版本已发布"): return False
            st = get_data(r).get("status")
            if st not in (5, "5"):
                time.sleep(2)
                r = os_api("GET", f"/flows/{fid}/versions/{fvid}")
                st = get_data(r).get("status")
            if st in (5, "5"):
                print(f"    status=5 (已发布)")
                return True
            os_fail(f"status={st}, 期望=5")
            return False

        if not failed:
            if not step("PUBLISH 提交审批", s9): failed = True
        if not failed:
            if not step("查询审批记录", s10): failed = True
        if not failed:
            if not step("第一级审批 (tester)", s11): failed = True
        if not failed:
            if not step("第二级审批 (tester)", s12): failed = True
        if not failed:
            if not step("验证版本已发布", s13): failed = True

        if not failed:
            print("  ✅ Phase 4 完成: 审批通过")
        else:
            print("  ❌ Phase 4 失败")

        # ═══════════════════════════════════════════════════
        # Phase 5: 发布后验证
        # ═══════════════════════════════════════════════════
        if not failed:
            print("\n── Phase 5: 发布后验证 ──")

        def s14():
            r = os_api("POST", f"/flows/{fid}/versions/{fvid}/debug", {
                "triggerData": {"message": "hello-after-approval"}
            })
            return check_ok(r, "DEBUG 已发布版本")

        if not failed:
            if not step("DEBUG 已发布版本", s14): failed = True

        if not failed:
            print("  ✅ Phase 5 完成: 发布后调试通过")
        else:
            print("  ❌ Phase 5 失败")

        # ═══════════════════════════════════════════════════
        # Phase 6: 部署 + 调用
        # ═══════════════════════════════════════════════════
        if not failed:
            print("\n── Phase 6: 部署 + 调用 ──")

        def s15():
            r = os_api("POST", f"/flows/{fid}/deploy", {"versionId": fvid})
            return check_ok(r, "DEPLOY 部署")

        def s16():
            r = os_api("POST", f"/flows/{fid}/start")
            if not check_ok(r, "START 启动"): return False
            r2 = os_api("GET", f"/flows/{fid}")
            ls = get_data(r2).get("lifecycleStatus")
            if ls not in (2, "2"):
                os_fail(f"lifecycleStatus={ls}, 期望=2 (running)")
                return False
            return True

        def s17():
            r = api_connector("POST", f"/flows/{fid}/invoke",
                             {"message": "hello-from-production"},
                             headers={"X-Sys-Token": "test-token"})
            if r is None:
                os_fail("connector-api 连接失败")
                return False
            if r.status_code in (200, 201):
                print(f"  ✅ TRIGGER (HTTP {r.status_code})")
                try:
                    b = r.json()
                    print(f"    响应: {json.dumps(b, ensure_ascii=False)[:200]}")
                except Exception:
                    print(f"    响应: {r.text[:200]}")
                return True
            os_fail(f"TRIGGER: HTTP {r.status_code}, {r.text[:200]}")
            return False

        def s18():
            time.sleep(1)
            st = os_db_val(
                f"SELECT status FROM openplatform_v2_execution_record_t "
                f"WHERE flow_id={fid} ORDER BY create_time DESC LIMIT 1"
            )
            if st:
                print(f"    运行记录 status={st}")
                return True
            print(f"    ⚠️ 未找到运行记录")
            return True  # non-blocking

        def s19():
            r = os_api("POST", f"/flows/{fid}/stop")
            if not check_ok(r, "STOP 停止"): return False
            r2 = os_api("GET", f"/flows/{fid}")
            ls = get_data(r2).get("lifecycleStatus")
            if ls in (1, "1"):
                print(f"    lifecycleStatus=1 (stopped)")
                return True
            os_fail(f"lifecycleStatus={ls}, 期望=1")
            return False

        if not failed:
            if not step("DEPLOY 部署", s15): failed = True
        if not failed:
            if not step("START 启动", s16): failed = True
        if not failed:
            if not step("HTTP TRIGGER 调用", s17): failed = True
        if not failed:
            if not step("查询运行记录(DB只读)", s18): failed = True
        if not failed:
            if not step("STOP 停止", s19): failed = True

        if not failed:
            print("  ✅ Phase 6 完成: 部署调用全链路通过")

        # ── 结果 ──
        print("\n" + "=" * 60)
        if failed:
            print("  ❌ 全流程测试存在失败步骤")
        else:
            print("  🎉 全流程测试全部通过!")
        print("=" * 60)
        return 1 if failed else 0

    finally:
        # Cleanup
        if not KEEP:
            print("\n── 清理测试数据 ──")
            if fid:
                os_db(f"DELETE FROM openplatform_v2_cp_connector_version_ref_t WHERE flow_id={fid}")
                os_db(f"DELETE FROM openplatform_v2_cp_flow_version_t WHERE flow_id={fid}")
                os_db(f"DELETE FROM openplatform_v2_cp_flow_t WHERE id={fid}")
            if cid:
                os_db(f"DELETE FROM openplatform_v2_cp_connector_version_t WHERE connector_id={cid}")
                os_db(f"DELETE FROM openplatform_v2_cp_connector_t WHERE id={cid}")
            if aid:
                os_db(f"DELETE FROM openplatform_v2_approval_record_t WHERE id={aid}")
            print("  ✅ 测试数据已清理")
        else:
            print(f"\n  📝 KEEP_TEST_DATA=1: cid={cid}, fid={fid}")

        mock.stop()
        os_done()


if __name__ == "__main__":
    sys.exit(test_full_flow())
