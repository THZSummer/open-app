#!/usr/bin/env python3
"""HTTP header 大小写不敏感集成测试

复现生产 bug: 标准环境 Nginx 等前置网关将 HTTP 请求头 name 小写化 (如 X-Trace-Id -> x-trace-id),
而 FlowInvokeService.buildStructuredTriggerInput 用 new HashMap + putAll 承载 header,
丢失了 Controller 层 TreeMap(CASE_INSENSITIVE_ORDER) 的大小写不敏感特性,
导致表达式 ${$.node.node_trigger.input.header.X-Trace-Id} 用原始大小写引用时解析不到.

测试场景:
  - IT-066: 标准环境模拟 - 发送小写 header (x-trace-id), 表达式用 X-Trace-Id 引用
            期望: 命中 (HTTP header 协议上大小写不敏感, RFC 7230)
            当前: FAIL (bug) - 解析为 null
  - IT-067: 开发环境对照 - 发送原始大小写 header (X-Trace-Id), 表达式用 X-Trace-Id 引用
            期望: 命中 (基准)
            当前: PASS
  - IT-068: 混合大小写 - 发送 X-TRACE-ID (全大写), 表达式用 X-Trace-Id 引用
            期望: 命中
            当前: FAIL (bug)

设计要点:
  - trigger input.header 契约中 X-Trace-Id 设为可选 (required: []),
    避开 validateInputContractSections 的 new HashMap(headers).containsKey 大小写断裂,
    使请求能到达表达式解析层, 精准复现 "解析不到" 而非 "校验失败".
"""
from client import *
import pytest
import copy


# ═══════════════════════════════════════════════════════════
# 编排配置: trigger (header 契约) -> exit (output 引用 header)
# ═══════════════════════════════════════════════════════════

TRIGGER_NODE_HEADER = {
    "id": "node_trigger",
    "type": "trigger",
    "position": {"x": 100, "y": 200},
    "data": {
        "labelCn": "接收请求",
        "labelEn": "Receive Request",
        "type": "trigger",
        "triggerType": "http",
        "authConfigs": [{
            "type": "SYSTOKEN",
            "header": {"type": "object",
                       "properties": {"X-Sys-Token": {"type": "string", "required": True, "sensitive": True}}}
        }],
        "input": {
            "protocol": "HTTP",
            "header": {
                "type": "object",
                "properties": {
                    "X-Trace-Id": {"type": "string", "description": "链路追踪ID"}
                },
                "required": []  # 可选: 避开校验层大小写断裂, 使请求到达表达式解析层
            },
            "query": {"type": "object", "properties": {}, "required": []},
            "body": {"type": "object", "properties": {}, "required": []}
        }
    }
}

EXIT_NODE_HEADER = {
    "id": "node_exit",
    "type": "exit",
    "position": {"x": 350, "y": 200},
    "data": {
        "type": "exit",
        "labelCn": "返回结果",
        "labelEn": "Return Result",
        "output": {
            "header": {"type": "object", "properties": {}},
            "body": {
                "type": "object",
                "properties": {
                    # ★ 核心表达式: 用原始大小写 X-Trace-Id 引用 trigger input.header
                    "trace": {"type": "string",
                              "value": "${$.node.node_trigger.input.header.X-Trace-Id}",
                              "description": "回显 trace id (验证 header 大小写不敏感解析)"}
                }
            }
        }
    }
}


def build_header_orchestration():
    """构建 trigger -> exit 编排 (exit output.body.trace 引用 trigger input.header.X-Trace-Id)"""
    return {
        "nodes": [copy.deepcopy(TRIGGER_NODE_HEADER), copy.deepcopy(EXIT_NODE_HEADER)],
        "flowConfig": {"rateLimitConfig": {"maxQps": 100}},
        "edges": [
            {"id": "e1", "source": "node_trigger", "target": "node_exit",
             "type": "smoothstep", "data": {"businessType": "default"}}
        ]
    }


def setup_header_flow(flow_id):
    """部署一个 header 回显连接流, 返回 flow_id"""
    flow_version_id = snow_id()
    orch = build_header_orchestration()
    db(
        f"INSERT INTO openplatform_v2_cp_flow_t "
        f"(id, name_cn, name_en, lifecycle_status, app_id, create_by, last_update_by) "
        f"VALUES ({flow_id}, 'IT_Header大小写', 'IT_HeaderCase', "
        f"2, {INTERNAL_APP_ID}, 'tester', 'tester')"
    )
    db(
        f"INSERT INTO openplatform_v2_cp_flow_version_t "
        f"(id, flow_id, orchestration_config, create_by, last_update_by) "
        f"VALUES ({flow_version_id}, {flow_id}, "
        f"'{escape_sql(orch)}', 'tester', 'tester')"
    )
    return flow_id


# ═══════════════════════════════════════════════════════════

@pytest.mark.L1
def test_header_case_insensitive():
    """HTTP header 大小写不敏感解析集成测试"""

    # ═══════════════════════════════════════════════════════════
    # IT-067: 开发环境对照 - 原始大小写 header (基准, 当前应 PASS)
    # ═══════════════════════════════════════════════════════════
    print("=== IT-067: 开发环境对照 (header 保留原始大小写) ===")
    fid_067 = snow_id()
    setup_header_flow(fid_067)
    resp = trigger(fid_067,
                   body={},
                   headers={"X-Sys-Token": "test-token", "X-Trace-Id": "trace-upper-067"})
    if resp is not None:
        check("HTTP 200", resp.status_code == 200, f"status={resp.status_code}")
        check("X-Code 为 20000", resp.headers.get("X-Code") == "20000",
              f"X-Code={resp.headers.get('X-Code')}")
        body = resp.json()
        check("trace == trace-upper-067 (原始大小写 header 命中, 开发环境基准)",
              body.get("trace") == "trace-upper-067",
              f"trace={body.get('trace')}")
    else:
        print("  SKIP: connector-api 未运行")

    # ═══════════════════════════════════════════════════════════
    # IT-066: 标准环境模拟 - 小写 header (复现 bug, 当前应 FAIL)
    # ═══════════════════════════════════════════════════════════
    print("\n=== IT-066: 标准环境模拟 (Nginx 小写化 header) ===")
    fid_066 = snow_id()
    setup_header_flow(fid_066)
    # 模拟 Nginx: header name 全小写
    resp = trigger(fid_066,
                   body={},
                   headers={"x-sys-token": "test-token", "x-trace-id": "trace-lower-066"})
    if resp is not None:
        check("HTTP 200", resp.status_code == 200, f"status={resp.status_code}")
        body = resp.json()
        # ★ 核心断言: HTTP header 协议上大小写不敏感 (RFC 7230),
        #   小写 x-trace-id 应能被表达式 X-Trace-Id 引用命中.
        #   当前 buildStructuredTriggerInput 用 HashMap 承载 header -> FAIL (trace=null)
        check("trace == trace-lower-066 (小写 header 经表达式 X-Trace-Id 引用应命中)",
              body.get("trace") == "trace-lower-066",
              f"trace={body.get('trace')} (预期 trace-lower-066, 实际 {'null' if body.get('trace') is None else body.get('trace')})")
    else:
        print("  SKIP: connector-api 未运行")

    # ═══════════════════════════════════════════════════════════
    # IT-068: 全大写 header (复现 bug, 当前应 FAIL)
    # ═══════════════════════════════════════════════════════════
    print("\n=== IT-068: 全大写 header (混合大小写验证) ===")
    fid_068 = snow_id()
    setup_header_flow(fid_068)
    resp = trigger(fid_068,
                   body={},
                   headers={"X-SYS-TOKEN": "test-token", "X-TRACE-ID": "trace-upper-068"})
    if resp is not None:
        check("HTTP 200", resp.status_code == 200, f"status={resp.status_code}")
        body = resp.json()
        check("trace == trace-upper-068 (全大写 header 经表达式 X-Trace-Id 引用应命中)",
              body.get("trace") == "trace-upper-068",
              f"trace={body.get('trace')}")
    else:
        print("  SKIP: connector-api 未运行")


if __name__ == "__main__":
    test_header_case_insensitive()
    done()
