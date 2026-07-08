#!/usr/bin/env python3
"""
连接器与连接流并行数据查询 E2E

拓扑: trigger → script_prepare → parallel → [connector_connectors ‖ connector_flows] → script_merge → exit

并行节点分流两个 Connector 分支，分别调用 open-server 的连接器列表 API 和连接流列表 API，
合并节点汇总后统一输出。

入参按 HTTP 语义分布在 Header/Query/Body:
  Header: X-App-Id / Cookie / X-XSRF-TOKEN
  Query:  curPage / pageSize / keyword
  Body:   X-Echo-To-Header (透传回响应头)

响应: X-Echo-To-Header / X-Connector-Date / X-Flow-Date 响应头，body 含 connectors[] 和 flows[]
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
            b = {}
        if isinstance(b.get("code"), str) and b["code"] not in ("0", "200"):
            os_fail(f"{desc}: code={b['code']}, msg={b.get('messageZh','')}  {url}")
            return False
        return True
    else:
        os_fail(f"{desc}: HTTP {resp.status_code}  {url}")
        return False


def get_data(resp):
    try:
        return resp.json().get("data", {})
    except Exception:
        return {}


def create_and_publish_connector(label_cn, label_en, target_url, method="GET"):
    """通过 open-server API 创建连接器 -> 创建草稿 -> 配置 -> 发布"""
    cid = snow_id()
    r = os_api("POST", "/connectors", {
        "nameCn": label_cn, "nameEn": label_en, "connectorType": 1
    })
    d = get_data(r) if r else {}
    cid_val = d.get("connectorId") if d else None
    if cid_val:
        cid = int(cid_val)
    if not check_ok(r, f"CREATE {label_en}", "POST /connectors"):
        return None, None, None

    os_api("POST", f"/connectors/{cid}/versions", {})
    r2 = os_api("GET", f"/connectors/{cid}/versions")
    vlist = get_data(r2)
    if isinstance(vlist, dict):
        vlist = vlist.get("items", vlist.get("data", []))
    if not isinstance(vlist, list) or len(vlist) == 0:
        os_fail("版本列表为空")
        return None, None, None
    cvid = int(vlist[0].get("versionId", vlist[0].get("id", 0)))

    config = {
        "protocol": "HTTP",
        "protocolConfig": {"url": target_url, "method": method},
        "authConfigs": [{"type": "NONE"}],
        "input": {
            "protocol": "HTTP",
            "header": {
                "type": "object",
                "properties": {
                    "X-App-Id": {"type": "string"},
                    "Cookie": {"type": "string"},
                    "X-XSRF-TOKEN": {"type": "string"},
                },
            },
            "query": {
                "type": "object",
                "properties": {
                    "curPage": {"type": "number"},
                    "pageSize": {"type": "number"},
                    "keyword": {"type": "string"},
                },
            },
        },
        "output": {
            "protocol": "HTTP",
            "body": {
                "type": "object",
                "properties": {
                    "code": {"type": "string"},
                    "messageZh": {"type": "string"},
                    "data": {"type": "array", "items": {"type": "object", "properties": {}}},
                },
            },
        },
        "timeoutMs": 5000,
    }
    os_api("PUT", f"/connectors/{cid}/versions/{cvid}", {
        "connectionConfig": config
    })
    os_api("PUT", f"/connectors/{cid}/versions/{cvid}/publish")
    print(f"    {label_en}: cid={cid}, cvid={cvid}")
    return cid, cvid, config


@pytest.mark.L3
def test_resource_query_parallel():
    print("=" * 60)
    print("  连接器与连接流并行数据查询")
    print("  trigger → script_prepare → parallel → [connector ‖ connector] → script_merge → exit")
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

    # 创建两个并行分支的连接器
    print("\n-- Phase 0b: Create Parallel Connectors --")
    conn_api_url = f"{OPEN_SERVER_BASE}/service/open/v2/connectors"
    flow_api_url = f"{OPEN_SERVER_BASE}/service/open/v2/flows"

    cid_c, cvid_c, config_c = create_and_publish_connector(
        f"QueryConnectors_{_RUN_ID}", f"query_connectors_{_RUN_ID}", conn_api_url, "GET")
    cid_f, cvid_f, config_f = create_and_publish_connector(
        f"QueryFlows_{_RUN_ID}", f"query_flows_{_RUN_ID}", flow_api_url, "GET")

    if not cid_c or not cid_f:
        print("[FAIL] Failed to create connectors")
        return
    print("  [OK] connectors ready")

    fid = fvid = aid = None
    failed = False

    try:
        print("\n-- Phase 1: Create Flow + Orchestration --")

        def s1():
            nonlocal fid
            fid = snow_id()
            r = os_api("POST", "/flows", {
                "nameCn": f"ResourceQueryParallel_{_RUN_ID}",
                "nameEn": f"resource_query_parallel_{_RUN_ID}",
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
            nid_t  = "trigger"
            nid_sp = "script_prepare"
            nid_p  = "parallel"
            nid_c  = "conn_connectors"
            nid_f  = "conn_flows"
            nid_sm = "script_merge"
            nid_e  = "exit"

            # 预处理脚本: 从 trigger 提取 headers/query，供两个分支共用
            script_prepare_src = (
                "function main(ctx) {\n"
                "    var hdrs = ctx.trigger.input.header;\n"
                "    var q = ctx.trigger.input.query;\n"
                "    var body = ctx.trigger.input.body;\n"
                "    function hdr(name) {\n"
                "        return hdrs[name] || hdrs[name.toLowerCase()] || hdrs[name.toUpperCase()];\n"
                "    }\n"
                "    if (!hdrs || !hdr('X-App-Id')) {\n"
                "        hdrs = (body && body.header) ? body.header : {};\n"
                "        q = (body && body.query) ? body.query : {};\n"
                "    }\n"
                "    return {\n"
                "        appId: hdr('X-App-Id') || '',\n"
                "        cookie: hdr('Cookie') || '',\n"
                "        xsrfToken: hdr('X-XSRF-TOKEN') || '',\n"
                "        curPage: (q && q.curPage) || 1,\n"
                "        pageSize: (q && q.pageSize) || 3,\n"
                "        keyword: (q && q.keyword) || '',\n"
                "        echoTo: (body && body['X-Echo-To-Header']) || ''\n"
                "    };\n"
                "}"
            )

            # 合并脚本: 汇总两个连接器的结果
            script_merge_src = (
                "function main(ctx) {\n"
                "    var c = ctx['" + nid_c + "'].output || {};\n"
                "    var f = ctx['" + nid_f + "'].output || {};\n"
                "    var p = ctx['" + nid_sp + "'].output || {};\n"
                "    var cb = c.body || {};\n"
                "    var fb = f.body || {};\n"
                "    var ch = c.header || {};\n"
                "    var fh = f.header || {};\n"
                "    var cd = '';\n"
                "    var fd = '';\n"
                "    if (ch) { var d = ch['Date']; if (d !== undefined) cd = String(d); else cd = 'none'; }\n"
                "    if (fh) { var e = fh['Date']; if (e !== undefined) fd = String(e); else fd = 'none'; }\n"
                "    return {\n"
                "        code: '200',\n"
                "        messageZh: '\u64CD\u4F5C\u6210\u529F',\n"
                "        messageEn: 'Success',\n"
                "        connectors: cb.data || [],\n"
                "        flows: fb.data || [],\n"
                "        connectorDate: cd,\n"
                "        flowDate: fd,\n"
                "        echoTo: p.echoTo || ''\n"
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
                                    "properties": {"X-Echo-To-Header": {"type": "string"}},
                                    "required": [],
                                },
                            },
                        }},
                        {"id": nid_sp, "type": "script", "data": {
                            "type": "script", "labelCn": "预处理",
                            "script": script_prepare_src, "timeoutMs": 5000,
                            "output": {
                                "type": "object",
                                "properties": {
                                    "appId": {"type": "string"},
                                    "cookie": {"type": "string"},
                                    "xsrfToken": {"type": "string"},
                                    "curPage": {"type": "number"},
                                    "pageSize": {"type": "number"},
                                    "keyword": {"type": "string"},
                                    "echoTo": {"type": "string"},
                                },
                            },
                        }},
                        {"id": nid_p, "type": "parallel",
                            "data": {"type": "parallel"}},
                        {"id": nid_c, "type": "connector", "data": {
                            "type": "connector", "labelCn": "查询连接器",
                            "connectorId": str(cid_c),
                            "connectorVersionId": str(cvid_c),
                            "connectorVersionConfig": config_c,
                            "timeoutMs": 5000,
                            "input": {
                                "header": {
                                    "type": "object",
                                    "properties": {
                                        "X-App-Id": {"type": "string", "value": "${$.node." + nid_sp + ".output.appId}"},
                                        "Cookie": {"type": "string", "value": "${$.node." + nid_sp + ".output.cookie}"},
                                        "X-XSRF-TOKEN": {"type": "string", "value": "${$.node." + nid_sp + ".output.xsrfToken}"},
                                    },
                                },
                                "query": {
                                    "type": "object",
                                    "properties": {
                                        "curPage": {"type": "number", "value": "${$.node." + nid_sp + ".output.curPage}"},
                                        "pageSize": {"type": "number", "value": "${$.node." + nid_sp + ".output.pageSize}"},
                                        "keyword": {"type": "string", "value": "${$.node." + nid_sp + ".output.keyword}"},
                                    },
                                },
                            },
                        }},
                        {"id": nid_f, "type": "connector", "data": {
                            "type": "connector", "labelCn": "查询连接流",
                            "connectorId": str(cid_f),
                            "connectorVersionId": str(cvid_f),
                            "connectorVersionConfig": config_f,
                            "timeoutMs": 5000,
                            "input": {
                                "header": {
                                    "type": "object",
                                    "properties": {
                                        "X-App-Id": {"type": "string", "value": "${$.node." + nid_sp + ".output.appId}"},
                                        "Cookie": {"type": "string", "value": "${$.node." + nid_sp + ".output.cookie}"},
                                        "X-XSRF-TOKEN": {"type": "string", "value": "${$.node." + nid_sp + ".output.xsrfToken}"},
                                    },
                                },
                                "query": {
                                    "type": "object",
                                    "properties": {
                                        "curPage": {"type": "number", "value": "${$.node." + nid_sp + ".output.curPage}"},
                                        "pageSize": {"type": "number", "value": "${$.node." + nid_sp + ".output.pageSize}"},
                                        "keyword": {"type": "string", "value": "${$.node." + nid_sp + ".output.keyword}"},
                                    },
                                },
                            },
                        }},
                        {"id": nid_sm, "type": "script", "data": {
                            "type": "script", "labelCn": "合并结果",
                            "script": script_merge_src, "timeoutMs": 5000,
                            "output": {
                                "type": "object",
                                "properties": {
                                    "code":          {"type": "string"},
                                    "messageZh":     {"type": "string"},
                                    "messageEn":     {"type": "string"},
                                    "connectors":    {"type": "array", "items": {"type": "object", "properties": {}}},
                                    "flows":         {"type": "array", "items": {"type": "object", "properties": {}}},
                                    "connectorDate": {"type": "string"},
                                    "flowDate":      {"type": "string"},
                                    "echoTo":        {"type": "string"},
                                },
                            },
                        }},
                        {"id": nid_e, "type": "exit", "data": {
                            "type": "exit",
                            "output": {
                                "header": {
                                    "type": "object",
                                    "properties": {
                                        "X-Echo-To-Header":  {"type": "string", "value": "${$.node." + nid_sm + ".output.echoTo}"},
                                        "X-Connector-Date": {"type": "string", "value": "${$.node." + nid_sm + ".output.connectorDate}"},
                                        "X-Flow-Date":       {"type": "string", "value": "${$.node." + nid_sm + ".output.flowDate}"},
                                    },
                                },
                                "body": {
                                    "type": "object",
                                    "properties": {
                                        "code":       {"type": "string", "value": "${$.node." + nid_sm + ".output.code}"},
                                        "messageZh":  {"type": "string", "value": "${$.node." + nid_sm + ".output.messageZh}"},
                                        "messageEn":  {"type": "string", "value": "${$.node." + nid_sm + ".output.messageEn}"},
                                        "connectors": {"type": "array", "items": {"type": "object", "properties": {}}, "value": "${$.node." + nid_sm + ".output.connectors}"},
                                        "flows":      {"type": "array", "items": {"type": "object", "properties": {}}, "value": "${$.node." + nid_sm + ".output.flows}"},
                                    },
                                },
                            },
                        }},
                    ],
                    "edges": [
                        {"id": "e1", "source": nid_t, "target": nid_sp,
                         "type": "smoothstep", "data": {"businessType": "default", "connectionMode": "serial"}},
                        {"id": "e2", "source": nid_sp, "target": nid_p,
                         "type": "smoothstep", "data": {"businessType": "default", "connectionMode": "serial"}},
                        {"id": "e3", "source": nid_p, "target": nid_c,
                         "type": "smoothstep", "data": {"businessType": "default", "connectionMode": "parallel"}},
                        {"id": "e4", "source": nid_p, "target": nid_f,
                         "type": "smoothstep", "data": {"businessType": "default", "connectionMode": "parallel"}},
                        {"id": "e5", "source": nid_c, "target": nid_sm,
                         "type": "smoothstep", "data": {"businessType": "default", "connectionMode": "parallel"}},
                        {"id": "e6", "source": nid_f, "target": nid_sm,
                         "type": "smoothstep", "data": {"businessType": "default", "connectionMode": "parallel"}},
                        {"id": "e7", "source": nid_sm, "target": nid_e,
                         "type": "smoothstep", "data": {"businessType": "default", "connectionMode": "serial"}},
                    ],
                    "flowConfig": {"flowMode": "parallel", "rateLimitConfig": {"maxQps": 50, "maxConcurrency": 10}},
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
                "triggerData": {"query": {"curPage": 1, "pageSize": 3, "keyword": ""}, "header": AUTH_HEADERS, "X-Echo-To-Header": "debug-echo"},
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
            if st not in (5, "5"):
                os_fail(f"status={st}, expected=5 (published)")
                return False
            print(f"    status={st} (published)")
            return True

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

        ECHO_VAL = "echo-resource-query-parallel"

        def _invoke_and_verify(label, extra_headers, params):
            r = api_connector("POST", f"/flows/{fid}/invoke",
                              {"X-Echo-To-Header": ECHO_VAL},
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
            c_count = len(body.get("connectors", []))
            f_count = len(body.get("flows", []))
            print(f"    connectors={c_count} items, flows={f_count} items")
            ok = True
            if body.get("code") in ("200", 200) and c_count > 0 and f_count > 0:
                print(f"    [OK] code=200, connectors={c_count}, flows={f_count}")
            else:
                os_fail(f"{label}: code={body.get('code')}, connectors={c_count}, flows={f_count}")
                ok = False
            actual_echo = r.headers.get("X-Echo-To-Header", "")
            if actual_echo == ECHO_VAL:
                print(f"    [OK] X-Echo-To-Header={ECHO_VAL}")
            else:
                os_fail(f"{label}: X-Echo-To-Header={actual_echo}, expected={ECHO_VAL}")
                ok = False
            connector_date = unquote(r.headers.get("X-Connector-Date", ""))
            flow_date = unquote(r.headers.get("X-Flow-Date", ""))
            connector_date_raw = r.headers.get("X-Connector-Date", "MISSING")
            flow_date_raw = r.headers.get("X-Flow-Date", "MISSING")
            print(f"    X-Connector-Date raw=[{connector_date_raw}] decoded=[{connector_date}]")
            print(f"    X-Flow-Date       raw=[{flow_date_raw}] decoded=[{flow_date}]")
            print(f"    all-resp-headers: {dict(((k,v) for k,v in r.headers.items() if k.startswith('X-')))}")
            return ok

        def s12():
            return _invoke_and_verify(
                "并行查询 connectors+flows",
                AUTH_HEADERS,
                {"curPage": 1, "pageSize": 3, "keyword": ""},
            )

        def s13():
            time.sleep(0.5)
            return check_ok(os_api("POST", f"/flows/{fid}/stop"), "STOP")

        if not failed and not step("DEPLOY", s10): failed = True
        if not failed and not step("START", s11): failed = True
        if not failed and not step("Invoke parallel", s12): failed = True
        if not failed and not step("STOP", s13): failed = True
        print(f"  {'[OK]' if not failed else '[FAIL]'} Phase 4")

        if failed:
            print(f"\n{'!' * 60}")
            print(f"  [FAIL] Resource query parallel test FAILED!")
            print(f"{'!' * 60}")
        else:
            print(f"\n{'=' * 60}")
            print(f"  [PASS] Resource query parallel test PASSED!")
            print(f"{'=' * 60}")

    finally:
        pass

    os_done()
