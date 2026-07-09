#!/usr/bin/env python3
"""
连接器或连接流列表数据查询 E2E - trigger -> script(ctx.http 条件分支) -> exit

入参按 HTTP 语义分布在 Header/Query/Body:
  Header: X-Type (路由) / X-App-Id / Cookie / X-XSRF-TOKEN
  Query:  curPage / pageSize / keyword
  Body:   X-Echo-To-Header (透传回响应头)

响应: body 透传原始 API 数据, X-Type / X-Echo-To-Header 响应头

Prerequisites:
  - open-server running on localhost:18080
  - connector-api running on localhost:18180

Usage:
  cd open-server/src/test/python
  python3 e2e/test_resource_query_branch.py
"""
import os, sys, json, time
import importlib.util

TEST_DIR = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(os.path.dirname(TEST_DIR), "common"))

_spec = importlib.util.spec_from_file_location(
    "common_client", os.path.join(os.path.dirname(TEST_DIR), "common", "client.py"))
_osm = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(_osm)
os_api = _osm.api
os_ok = _osm.ok
os_fail = _osm.fail
os_done = _osm.done
from client import CONNECTOR_API_BASE, CONNECTOR_API_HEALTH, OPEN_SERVER_BASE, TEST_APP_ID, TEST_COOKIE, TEST_XSRF_TOKEN

import pytest, requests, random, string
from urllib.parse import unquote

_RUN_ID = ''.join(random.choices(string.ascii_lowercase + string.digits, k=6))

AUTH_HEADERS = {"X-App-Id": TEST_APP_ID, "Cookie": TEST_COOKIE, "X-XSRF-TOKEN": TEST_XSRF_TOKEN}


def snow_id():
    return int(time.time() * 1000000) % 100000000000000000


def api_connector(method, path, body=None, headers=None, params=None):
    url = f"{CONNECTOR_API_BASE}{path}"
    h = {"Content-Type": "application/json"}
    if headers:
        h.update(headers)
    try:
        return requests.request(method, url, json=body, headers=h, params=params, timeout=10)
    except requests.ConnectionError:
        return None


_step = [0]


def step(desc, fn):
    _step[0] += 1
    print(f"\n  [{_step[0]}] {desc}")
    try:
        return fn()
    except Exception as e:
        os_fail(f"Step {_step[0]} exception: {e}")
        return False


def check_ok(resp, desc, url=""):
    if resp is None:
        os_fail(f"{desc}: connection failed {url}")
        return False
    if resp.status_code == 200:
        try:
            b = resp.json()
        except Exception:
            print(f"  [OK] {desc} (HTTP 200)  {url}")
            return True
        if b.get("code") in ("200", 200):
            print(f"  [OK] {desc}  {url}")
            return True
        os_fail(f"{desc}: code={b.get('code')}  {url}")
        return False
    else:
        os_fail(f"{desc}: HTTP {resp.status_code}  {url}")
        return False


def get_data(resp):
    try:
        return resp.json().get("data", {})
    except Exception:
        return {}


@pytest.mark.L3
def test_resource_query_branch():
    print("=" * 60)
    print("  连接器或连接流列表数据查询")
    print("  trigger -> script(ctx.http 条件分支 -> 真实API) -> exit")
    print(f"  Run: {_RUN_ID}")
    print("=" * 60)

    print("\n-- Phase 0: Environment Check --")
    r = os_api("GET", "/connectors")
    if r is None:
        print("[FAIL] open-server not running!")
        return
    print("  [OK] open-server ready")
    try:
        if requests.get(CONNECTOR_API_HEALTH, timeout=5).status_code == 200:
            print("  [OK] connector-api ready")
    except Exception:
        print("[FAIL] connector-api not running!")
        return

    tpl = None
    r = os_api("GET", "/approval-flows?keyword=connector_flow_version_publish")
    if r and r.status_code == 200:
        for item in r.json().get("data", []):
            if item.get("code") == "connector_flow_version_publish":
                tpl = item.get("id")
                break
    if not tpl:
        print("  Creating approval template...")
        os_api("POST", "/approval-flows", {
            "code": "connector_flow_version_publish",
            "nameCn": "审批", "nameEn": "approval",
            "appId": TEST_APP_ID,
            "nodes": [{"userId": "tester", "userName": "Test Approver"}],
        })
    print("  [OK] approval template ready")

    fid = fvid = aid = None
    failed = False

    try:
        print("\n-- Phase 1: Create Flow + Orchestration --")

        def s1():
            nonlocal fid
            fid = snow_id()
            r = os_api("POST", "/flows", {
                "nameCn": f"ResourceQuery_{_RUN_ID}",
                "nameEn": f"resource_query_{_RUN_ID}",
            })
            if not check_ok(r, "CREATE flow"):
                return False
            d = get_data(r)
            if str(d.get("flowId")) != str(fid):
                fid = int(d.get("flowId"))
            print(f"    flowId={fid}")
            return True

        def s2():
            nonlocal fvid
            r = os_api("POST", f"/flows/{fid}/versions", {})
            if not check_ok(r, "CREATE draft version"):
                return False
            fvid = int(get_data(r).get("versionId", 0))
            print(f"    versionId={fvid}")
            return True

        def s3():
            nid_t = "trigger"
            nid_s = "script"
            nid_e = "exit"

            script_src = (
                "function main(ctx) {\n"
                "    var hdrs = ctx.trigger.input.header;\n"
                "    var q = ctx.trigger.input.query;\n"
                "    var body = ctx.trigger.input.body;\n"
                "    // case-insensitive header lookup (nginx/HTTP2 lowercases headers)\n"
                "    function hdr(name) {\n"
                "        return hdrs[name] || hdrs[name.toLowerCase()] || hdrs[name.toUpperCase()];\n"
                "    }\n"
                "    // debug mode: fallback to body when header/query are empty\n"
                "    if (!hdrs || !hdr('X-App-Id')) {\n"
                "        hdrs = (body && body.header) ? body.header : {};\n"
                "        q = (body && body.query) ? body.query : {};\n"
                "    }\n"
                "    var type = hdr('X-Type') || (body && body.type) || 'connectors';\n"
                "    var curPage = (q && q.curPage) || 1;\n"
                "    var keyword = (q && q.keyword) || '';\n"
                "    var pageSize = (q && q.pageSize) || 3;\n"
                "\n"
                "    // build plain headers object (PolyglotMap key access works, but Object.keys/enum doesn't)\n"
                "    var plainHdrs = {};\n"
                "    ['X-App-Id', 'Cookie', 'X-XSRF-TOKEN'].forEach(function(k) {\n"
                "        if (hdrs[k]) plainHdrs[k] = hdrs[k];\n"
                "    });\n"
                "\n"
                "    var path = (type === 'flows') ? '/flows' : '/connectors';\n"
                "    var url = '" + OPEN_SERVER_BASE + "/service/open/v2'\n"
                "        + path + '?curPage=' + curPage\n"
                "        + '&keyword=' + encodeURIComponent(keyword)\n"
                "        + '&pageSize=' + pageSize;\n"
                "\n"
                "    var resp = ctx.http.request('GET', url, {headers: plainHdrs});\n"
                "    return {\n"
                "        result: resp.body,\n"
                "        type: type,\n"
                "        echoTo: (body && body['X-Echo-To-Header']) || ''\n"
                "    };\n"
                "}"
            )

            r = os_api("PUT", f"/flows/{fid}/versions/{fvid}", {
                "orchestrationConfig": {
                    "nodes": [
                        {"id": nid_t, "type": "trigger", "data": {
                            "type": "trigger", "triggerType": "http",
                            "authConfigs": [{
                                "type": "SYSTOKEN",
                                "header": {"type": "object", "properties": {
                                    "X-Sys-Token": {"type": "string", "required": True, "sensitive": True},
                                }},
                                "sysAccountWhitelist": ["tester"],
                            }],
                            "input": {
                                "protocol": "HTTP",
                                "header": {"type": "object", "properties": {
                                    "X-Type": {"type": "string"},
                                    "X-App-Id": {"type": "string"},
                                    "Cookie": {"type": "string"},
                                    "X-XSRF-TOKEN": {"type": "string"},
                                }, "required": []},
                                "query": {"type": "object", "properties": {
                                    "curPage": {"type": "number"},
                                    "pageSize": {"type": "number"},
                                    "keyword": {"type": "string"},
                                }, "required": []},
                                "body": {
                                    "type": "object",
                                    "properties": {
                                        "X-Echo-To-Header": {"type": "string"},
                                    },
                                    "required": [],
                                },
                            },
                        }},
                        {"id": nid_s, "type": "script", "data": {
                            "type": "script",
                            "script": script_src,
                            "timeoutMs": 5000,
                            "output": {
                                "type": "object",
                                "properties": {
                                    "result": {
                                        "type": "object",
                                        "properties": {
                                            "code":       {"type": "string"},
                                            "messageZh":  {"type": "string"},
                                            "messageEn":  {"type": "string"},
                                            "data":       {"type": "array", "items": {"type": "object", "properties": {}}},
                                            "page": {
                                                "type": "object",
                                                "properties": {
                                                    "curPage":    {"type": "number"},
                                                    "pageSize":   {"type": "number"},
                                                    "total":      {"type": "number"},
                                                    "totalPages": {"type": "number"},
                                                },
                                            },
                                        },
                                    },
                                    "type":   {"type": "string"},
                                    "echoTo": {"type": "string"},
                                },
                            },
                        }},
                        {"id": nid_e, "type": "exit", "data": {
                            "type": "exit",
                            "output": {
                                "header": {
                                    "type": "object",
                                    "properties": {
                                        "X-Type": {"type": "string", "value": "${$.node.script.output.type}"},
                                        "X-Echo-To-Header": {"type": "string", "value": "${$.node.script.output.echoTo}"},
                                    },
                                },
                                "body": {
                                    "type": "object",
                                    "properties": {
                                        "code":       {"type": "string", "value": "${$.node.script.output.result.code}"},
                                        "messageZh":  {"type": "string", "value": "${$.node.script.output.result.messageZh}"},
                                        "messageEn":  {"type": "string", "value": "${$.node.script.output.result.messageEn}"},
                                        "data":       {"type": "array", "items": {"type": "object", "properties": {}}, "value": "${$.node.script.output.result.data}"},
                                        "page": {
                                            "type": "object",
                                            "properties": {
                                                "curPage":    {"type": "number", "value": "${$.node.script.output.result.page.curPage}"},
                                                "pageSize":   {"type": "number", "value": "${$.node.script.output.result.page.pageSize}"},
                                                "total":      {"type": "number", "value": "${$.node.script.output.result.page.total}"},
                                                "totalPages": {"type": "number", "value": "${$.node.script.output.result.page.totalPages}"},
                                            },
                                        },
                                    },
                                },
                            },
                        }},
                    ],
                    "edges": [
                        {"id": "e1", "source": nid_t, "target": nid_s},
                        {"id": "e2", "source": nid_s, "target": nid_e},
                    ],
                    "flowConfig": {"flowMode": "serial", "rateLimitConfig": {"maxQps": 50}},
                },
            })
            return check_ok(r, "UPDATE orchestration", f"PUT /flows/{fid}/versions/{fvid}")

        if not step("CREATE flow", s1): failed = True
        if not failed and not step("CREATE draft version", s2): failed = True
        if not failed and not step("UPDATE orchestration", s3): failed = True
        print(f"  {'[OK]' if not failed else '[FAIL]'} Phase 1")

        print("\n-- Phase 2: Debug Draft --")
        def s4():
            r = os_api("POST", f"/flows/{fid}/versions/{fvid}/debug", {
                "triggerData": {"type": "connectors", "query": {"curPage": 1, "pageSize": 3, "keyword": ""}, "header": AUTH_HEADERS, "X-Echo-To-Header": "debug-echo"},
            })
            ok = check_ok(r, "DEBUG draft")
            if ok:
                print(f"    body: {r.text}")
            return ok

        if not failed and not step("DEBUG draft", s4): failed = True
        print(f"  {'[OK]' if not failed else '[FAIL]'} Phase 2")

        print("\n-- Phase 3: Publish + Approve --")
        def s5():
            return check_ok(os_api("POST", f"/flows/{fid}/versions/{fvid}/publish"), "PUBLISH submit")

        def s6():
            nonlocal aid
            r = os_api("GET", "/approvals/pending?businessType=connector_flow_version_publish&page=1&size=50")
            if r and r.status_code == 200:
                for item in r.json().get("data", []):
                    if str(item.get("businessId")) == str(fvid):
                        aid = item.get("id")
                        break
            if not aid:
                os_fail("Approval record not found")
                return False
            print(f"    approvalId={aid}")
            return True

        def s7():
            return check_ok(os_api("POST", f"/approvals/{aid}/approve", {"comment": "L1"}, user="tester"), "L1 approve")

        def s8():
            return check_ok(os_api("POST", f"/approvals/{aid}/approve", {"comment": "L2"}, user="tester"), "L2 approve")

        def s9():
            time.sleep(0.5)
            r = os_api("GET", f"/flows/{fid}/versions/{fvid}")
            st = get_data(r).get("status")
            if st not in (5, "5"):
                time.sleep(2)
                st = get_data(os_api("GET", f"/flows/{fid}/versions/{fvid}")).get("status")
            if st in (5, "5"):
                print("    status=5 (published)")
                return True
            os_fail(f"status={st}, expected=5")
            return False

        if not failed and not step("PUBLISH submit", s5): failed = True
        if not failed and not step("Find approval", s6): failed = True
        if not failed and not step("L1 approve", s7): failed = True
        if not failed and not step("L2 approve", s8): failed = True
        if not failed and not step("Verify published", s9): failed = True
        print(f"  {'[OK]' if not failed else '[FAIL]'} Phase 3")

        print("\n-- Phase 4: Deploy + Real-API Invoke --")
        def s10():
            return check_ok(os_api("POST", f"/flows/{fid}/deploy", {"versionId": fvid}), "DEPLOY")

        def s11():
            r = os_api("POST", f"/flows/{fid}/start")
            if not check_ok(r, "START"):
                return False
            ls = get_data(os_api("GET", f"/flows/{fid}")).get("lifecycleStatus")
            if ls not in (2, "2"):
                os_fail(f"lifecycleStatus={ls}")
                return False
            return True

        def _invoke_and_verify(label, payload, expected_headers, extra_headers, params):
            r = api_connector("POST", f"/flows/{fid}/invoke", payload,
                              headers={**{"X-Sys-Token": "tester"}, **extra_headers},
                              params=params)
            if r is None:
                os_fail(f"{label}: connector-api unreachable")
                return False
            if r.status_code not in (200, 201):
                os_fail(f"{label}: HTTP {r.status_code}, body={r.text[:200]}")
                return False
            body = r.json() if r.text else {}
            print(f"  [OK] {label} HTTP {r.status_code}")
            data_count = len(body.get("data", []))
            print(f"    body: {json.dumps(body, ensure_ascii=False)}")
            ok = True
            if body.get("code") in ("200", 200) and data_count > 0:
                print(f"    [OK] code=200, data={data_count} items")
            else:
                os_fail(f"{label}: unexpected response code={body.get('code')}, data_count={data_count}")
                ok = False
            for key, val in expected_headers.items():
                actual = unquote(r.headers.get(key, ''))
                if actual == val:
                    print(f"    [OK] {key}={val}")
                else:
                    os_fail(f"{label}: {key}={actual}, expected={val}")
                    ok = False
            return ok

        ECHO_VAL = "echo-resource-query"

        def _expected_headers(rtype):
            return {"X-Type": rtype, "X-Echo-To-Header": ECHO_VAL}

        def s12():
            return _invoke_and_verify(
                "Type=connectors -> 真实API",
                {"X-Echo-To-Header": ECHO_VAL},
                _expected_headers("connectors"),
                {**{"X-Type": "connectors"}, **AUTH_HEADERS},
                {"curPage": 1, "pageSize": 3, "keyword": ""},
            )

        def s13():
            return _invoke_and_verify(
                "Type=flows -> 真实API",
                {"X-Echo-To-Header": ECHO_VAL},
                _expected_headers("flows"),
                {**{"X-Type": "flows"}, **AUTH_HEADERS},
                {"curPage": 1, "pageSize": 3, "keyword": ""},
            )

        def s14():
            time.sleep(0.5)
            return check_ok(os_api("POST", f"/flows/{fid}/stop"), "STOP")

        if not failed and not step("DEPLOY", s10): failed = True
        if not failed and not step("START", s11): failed = True
        if not failed and not step("Invoke type=connectors", s12): failed = True
        if not failed and not step("Invoke type=flows", s13): failed = True
        if not failed and not step("STOP", s14): failed = True
        print(f"  {'[OK]' if not failed else '[FAIL]'} Phase 4")

        print("\n" + "=" * 60)
        if failed:
            print("  [FAIL] Resource query branch test FAILED")
        else:
            print("  [PASS] Resource query branch test PASSED!")
        print("=" * 60)
        assert not failed

    finally:
        os_done()


if __name__ == "__main__":
    test_resource_query_branch()
