#!/usr/bin/env python3
"""L4 契约测试 (connector-api 响应格式 - v5.8 transparent pass-through)

v5.8 透明透传格式：
  - 预执行错误（鉴权/限流/流不存在）：body 为空，错误信息在 X- 响应头
    X-Flow-Id, X-Code, X-Message-Zh, X-Message-En
  - 执行结果：body 为 exit node 输出，元数据在 X- 头
    X-Execution-Id, X-Status 等

校验点：
  - X- 响应头存在且符合格式
  - HTTP 状态码正确 (401/404)
  - body 为空
  - 错误码覆盖：401（无认证）, 404（流不存在）
"""
from client import *
import pytest
import json
import re


def _parse_header_int(name, resp):
    """从响应头获取数字字符串值"""
    v = resp.headers.get(name, "")
    return v.strip()



@pytest.mark.L1
def test_contract_response():
    # ── 创建测试 Flow（用于 401 验证）─────────────────────
    print("=== 创建测试 Flow（用于 401 验证）===")
    test_flow_id = snow_id()
    test_version_id = snow_id()

    orch = {
        "nodes": [
            {"id": "node_trigger", "type": "trigger",
             "position": {"x": 100, "y": 200},
             "data": {"labelCn": "接收", "labelEn": "Receive", "type": "trigger", "triggerType": "http",
                      "authConfigs": [{"type": "SYSTOKEN", "fields": [
                          {"name": "token", "carrier": "header", "fieldName": "X-Sys-Token"}
                      ]}],
                      "input": {"protocol": "HTTP",
                          "header": {"type": "object", "properties": {}, "required": []},
                          "query": {"type": "object", "properties": {}, "required": []},
                          "body": {"type": "object", "properties": {}, "required": []}},
                      "rateLimitConfig": {"maxQps": 100}}},
            {"id": "node_exit", "type": "exit",
             "position": {"x": 350, "y": 200},
             "data": {"labelCn": "返回", "labelEn": "Return",
                      "output": {"header": {"type": "object", "properties": {}},
                          "body": {"type": "object", "properties": {"ok": {"type": "boolean", "value": True}}}}}}
        ],
        "edges": [{"id": "e1", "source": "node_trigger", "target": "node_exit",
                   "type": "smoothstep", "data": {"businessType": "default"}}]
    }

    db(f"INSERT INTO openplatform_v2_cp_flow_t (id, name_cn, name_en, lifecycle_status, app_id, create_by, last_update_by) VALUES ({test_flow_id}, '契约测试', 'ContractTest', 1, {TEST_APP_ID}, 'tester', 'tester')")
    db(f"INSERT INTO openplatform_v2_cp_flow_version_t (id, flow_id, orchestration_config, create_by, last_update_by) VALUES ({test_version_id}, {test_flow_id}, '{escape_sql(orch)}', 'tester', 'tester')")


    # ── 获取错误响应 ─────────────────────────────────────
    print("\n=== 获取 401 响应（无认证）===")
    auth_resp = api("POST", f"/flows/{test_flow_id}/invoke",
                        {"sender": "test"})

    print("\n=== 获取 404 响应（flow 不存在）===")
    notfound_resp = api("POST", "/flows/999999999999999999/invoke",
                            {"sender": "test"},
                            headers={"X-Sys-Token": "test-token"})


    print("\n" + "=" * 60)
    print("L4 契约校验 (v5.8 transparent pass-through)")
    print("=" * 60)

    # ── X- 响应头存在性 ──────────────────────────────────
    print("\n--- X- 响应头存在性 ---")
    for label, resp in [("401-无认证", auth_resp), ("404-flow不存在", notfound_resp)]:
        if resp is None:
            continue
        check(f"[{label}] X-Flow-Id 存在", bool(resp.headers.get("X-Flow-Id")))
        check(f"[{label}] X-Code 存在", bool(resp.headers.get("X-Code")))
        check(f"[{label}] X-Message-Zh 存在", bool(resp.headers.get("X-Message-Zh")))
        check(f"[{label}] X-Message-En 存在", bool(resp.headers.get("X-Message-En")))

    # ── X-Code 格式 ──────────────────────────────────────
    print("\n--- X-Code 格式（数字字符串）---")
    for label, resp in [("401-无认证", auth_resp), ("404-flow不存在", notfound_resp)]:
        if resp is None:
            continue
        code = _parse_header_int("X-Code", resp)
        check(f"[{label}] X-Code 为数字字符串",
              bool(re.match(r"^\d+$", code)),
              f"code={code}")

    # ── HTTP 状态码 ──────────────────────────────────────
    print("\n--- HTTP 状态码 ---")
    if auth_resp is not None:
        check("HTTP 状态码为 401（无认证）",
              auth_resp.status_code == 401,
              f"status={auth_resp.status_code}")
    if notfound_resp is not None:
        check("HTTP 状态码为 404（flow 不存在）",
              notfound_resp.status_code == 404,
              f"status={notfound_resp.status_code}")

    # ── body 为空 ────────────────────────────────────────
    print("\n--- 错误响应 body 为空 ---")
    for label, resp in [("401-无认证", auth_resp), ("404-flow不存在", notfound_resp)]:
        if resp is None:
            continue
        body_text = resp.text.strip() if resp.text else ""
        check(f"[{label}] 错误响应 body 为空",
              body_text == "",
              f"body={body_text[:100]}")

    # ── 错误码覆盖率 ────────────────────────────────────
    print("\n--- 错误码覆盖率 ---")
    codes_seen = set()
    for label, resp in [("401-无认证", auth_resp), ("404-flow不存在", notfound_resp)]:
        if resp is not None:
            code = _parse_header_int("X-Code", resp)
            if code and re.match(r"^\d+$", code):
                codes_seen.add(code)
    check("错误码 401 覆盖（无认证）",
          "401" in codes_seen,
          f"已覆盖: {codes_seen}")
    check("错误码 404 覆盖（flow 不存在）",
          "404" in codes_seen,
          f"已覆盖: {codes_seen}")

    # ── 清理测试数据 ──
    db(f"DELETE FROM openplatform_v2_cp_flow_version_t WHERE id = {test_version_id}")
    db(f"DELETE FROM openplatform_v2_cp_flow_t WHERE id = {test_flow_id}")


if __name__ == "__main__":
    test_contract_response()
    done()
