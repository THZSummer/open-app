#!/usr/bin/env python3
"""Script node HTTP call sandbox restriction tests — FR-040a security

Tests that the GraalJS sandbox correctly blocks HTTP/network calls
from within script nodes. The sandbox is configured with:
  - allowIO(false)          — blocks file/network IO
  - allowHostAccess(EXPLICIT) — blocks Java type reflection
  - allowAllAccess(false)   — maximum restriction

Covers:
  - IT-SCRIPT-HTTP-001: Script uses fetch() — browser API not available
  - IT-SCRIPT-HTTP-002: Script uses XMLHttpRequest — browser API not available
  - IT-SCRIPT-HTTP-003: Script uses Java.type('java.net.URL') — host access blocked
  - IT-SCRIPT-HTTP-004: Script uses require('http') — CommonJS not available
  - IT-SCRIPT-HTTP-005: Script uses Java interop java.net.URL — host access blocked

All scenarios expect the sandbox to reject the call and return an error
response (X-Status=1, X-Code=63001 SCRIPT_RUNTIME_ERROR).

Orchestration: trigger → script → exit
v5.8 transparent response format:
  - Errors via X-Code / X-Message-Zh / X-Message-En headers, empty body
"""
from client import *
import pytest
import time
import json


# ═══════════════════════════════════════════════════════════
# Orchestration Builder — trigger → script → exit
# ═══════════════════════════════════════════════════════════

def build_script_orch(script_content, timeout_ms=5000):
    """Build a trigger → script → exit orchestration config."""
    return {
        "nodes": [
            {
                "id": "node_trigger",
                "type": "trigger",
                "position": {"x": 100, "y": 200},
                "data": {
                    "labelCn": "\u63a5\u6536",
                    "labelEn": "Recv",
                    "type": "trigger",
                    "triggerType": "http",
                    "authConfigs": [{
                        "type": "SYSTOKEN",
                        "header": {"type": "object", "properties": {"X-Sys-Token": {"type": "string", "required": True, "sensitive": True}}}
                    }],
                    "input": {
                        "protocol": "HTTP",
                        "header": {"type": "object", "properties": {}, "required": []},
                        "query": {"type": "object", "properties": {}, "required": []},
                        "body": {
                            "type": "object",
                            "properties": {
                                "name": {"type": "string"},
                                "value": {"type": "number"}
                            },
                            "required": []
                        }
                    },
                }
            },
            {
                "id": "node_script",
                "type": "script",
                "position": {"x": 350, "y": 200},
                "data": {
                    "labelCn": "\u811a\u672c\u5904\u7406",
                    "labelEn": "Script",
                    "script": script_content,
                    "timeoutMs": timeout_ms
                }
            },
            {
                "id": "node_exit",
                "type": "exit",
                "position": {"x": 600, "y": 200},
                "data": {
                    "labelCn": "\u8fd4\u56de",
                    "labelEn": "Ret",
                    "output": {
                        "header": {"type": "object", "properties": {}},
                        "body": {
                            "type": "object",
                            "properties": {
                                "result": {
                                    "type": "string",
                                    "value": "${$.node.node_script.output.result}"
                                }
                            }
                        }
                    }
                }
            }
        ],
        "edges": [
            {
                "id": "e1", "source": "node_trigger", "target": "node_script",
                "type": "smoothstep", "data": {"businessType": "default"}
            },
            {
                "id": "e2", "source": "node_script", "target": "node_exit",
                "type": "smoothstep", "data": {"businessType": "default"}
            }
        ],
        "flowConfig": {"rateLimitConfig": {"maxQps": 100}}
    }


# ═══════════════════════════════════════════════════════════
# Flow Lifecycle Helpers
# ═══════════════════════════════════════════════════════════

def setup_flow(flow_id, lifecycle_status=2, orchestration=None):
    """Create a flow and its version in the database.

    Returns (flow_id, flow_version_id).
    """
    flow_version_id = snow_id()
    orch = orchestration or {"nodes": [], "edges": []}
    db(
        f"INSERT INTO openplatform_v2_cp_flow_t "
        f"(id, name_cn, name_en, lifecycle_status, app_id, create_by, last_update_by) "
        f"VALUES ({flow_id}, '\u811a\u672cHTTP\u6d4b\u8bd5', 'ScriptHTTPTest', "
        f"{lifecycle_status}, {INTERNAL_APP_ID}, 'tester', 'tester')"
    )
    db(
        f"INSERT INTO openplatform_v2_cp_flow_version_t "
        f"(id, flow_id, orchestration_config, create_by, last_update_by) "
        f"VALUES ({flow_version_id}, {flow_id}, "
        f"'{escape_sql(orch)}', 'tester', 'tester')"
    )
    return flow_id, flow_version_id


def verify_sandbox_rejection(resp, scenario_name):
    """Verify that the sandbox correctly rejected the HTTP call attempt.

    Expected: error response with X-Status=1 and X-Code error header.
    The error code should be 63001 (SCRIPT_RUNTIME_ERROR) or similar.
    """
    if resp is not None:
        check(f"[{scenario_name}] HTTP status code indicates error",
              resp.status_code != 200 or resp.headers.get("X-Status") == "1",
              f"status={resp.status_code}, X-Status={resp.headers.get('X-Status')}")
        check(f"[{scenario_name}] X-Code error header present (sandbox rejection)",
              bool(resp.headers.get("X-Code")),
              f"X-Code={resp.headers.get('X-Code')}")
        code = resp.headers.get("X-Code", "")
        check(f"[{scenario_name}] X-Code is script runtime error",
              code in ("63001", "63003", "61033"),
              f"X-Code={code}")
        check(f"[{scenario_name}] Response body is empty",
              len(resp.content) == 0,
              f"body={resp.content[:200]}")
    else:
        check(f"[{scenario_name}] connector-api not running", False)


# ═══════════════════════════════════════════════════════════
# Tests
# ═══════════════════════════════════════════════════════════

@pytest.mark.L2
def test_script_http_call_sandbox():
    """Test that GraalJS sandbox blocks HTTP/network calls from scripts."""

    # ═══════════════════════════════════════════════════════════
    # IT-SCRIPT-HTTP-001: fetch() — browser API not available
    # ═══════════════════════════════════════════════════════════
    print("\n=== IT-SCRIPT-HTTP-001: Script uses fetch() ===")
    print("    Expected: fetch is not a GraalJS standard API, ReferenceError")
    script_001 = (
        "function main(ctx) {\n"
        "    var resp = fetch('http://localhost:18999/api/test');\n"
        '    return { result: "ok" };\n'
        "}"
    )
    fid_001, fvid_001 = setup_flow(
        snow_id(), lifecycle_status=2,
        orchestration=build_script_orch(script_001)
    )
    resp = trigger(fid_001, body={"name": "test", "value": 1},
                   headers={"X-Sys-Token": "test-token"})
    verify_sandbox_rejection(resp, "fetch()")

    # ═══════════════════════════════════════════════════════════
    # IT-SCRIPT-HTTP-002: XMLHttpRequest — browser API not available
    # ═══════════════════════════════════════════════════════════
    print("\n=== IT-SCRIPT-HTTP-002: Script uses XMLHttpRequest ===")
    print("    Expected: XMLHttpRequest is not a GraalJS standard API, ReferenceError")
    script_002 = (
        "function main(ctx) {\n"
        "    var xhr = new XMLHttpRequest();\n"
        "    xhr.open('GET', 'http://localhost:18999/api/test', false);\n"
        "    xhr.send();\n"
        '    return { result: xhr.responseText };\n'
        "}"
    )
    fid_002, fvid_002 = setup_flow(
        snow_id(), lifecycle_status=2,
        orchestration=build_script_orch(script_002)
    )
    resp = trigger(fid_002, body={"name": "test", "value": 1},
                   headers={"X-Sys-Token": "test-token"})
    verify_sandbox_rejection(resp, "XMLHttpRequest")

    # ═══════════════════════════════════════════════════════════
    # IT-SCRIPT-HTTP-003: Java.type('java.net.URL') — host access blocked
    # ═══════════════════════════════════════════════════════════
    print("\n=== IT-SCRIPT-HTTP-003: Script uses Java.type('java.net.URL') ===")
    print("    Expected: allowHostAccess(EXPLICIT) blocks Java.type, PolyglotException")
    script_003 = (
        "function main(ctx) {\n"
        "    var URL = Java.type('java.net.URL');\n"
        "    var url = new URL('http://localhost:18999/api/test');\n"
        "    var conn = url.openConnection();\n"
        "    conn.connect();\n"
        '    return { result: "ok" };\n'
        "}"
    )
    fid_003, fvid_003 = setup_flow(
        snow_id(), lifecycle_status=2,
        orchestration=build_script_orch(script_003)
    )
    resp = trigger(fid_003, body={"name": "test", "value": 1},
                   headers={"X-Sys-Token": "test-token"})
    verify_sandbox_rejection(resp, "Java.type URL")

    # ═══════════════════════════════════════════════════════════
    # IT-SCRIPT-HTTP-004: require('http') — CommonJS not available
    # ═══════════════════════════════════════════════════════════
    print("\n=== IT-SCRIPT-HTTP-004: Script uses require('http') ===")
    print("    Expected: CommonJS require not available in GraalJS, ReferenceError")
    script_004 = (
        "function main(ctx) {\n"
        "    var http = require('http');\n"
        '    return { result: "ok" };\n'
        "}"
    )
    fid_004, fvid_004 = setup_flow(
        snow_id(), lifecycle_status=2,
        orchestration=build_script_orch(script_004)
    )
    resp = trigger(fid_004, body={"name": "test", "value": 1},
                   headers={"X-Sys-Token": "test-token"})
    verify_sandbox_rejection(resp, "require http")

    # ═══════════════════════════════════════════════════════════
    # IT-SCRIPT-HTTP-005: Java interop java.net.HttpURLConnection
    # ═══════════════════════════════════════════════════════════
    print("\n=== IT-SCRIPT-HTTP-005: Script uses java.net.HttpURLConnection ===")
    print("    Expected: allowHostAccess(EXPLICIT) blocks Java interop, PolyglotException")
    script_005 = (
        "function main(ctx) {\n"
        "    var url = new java.net.URL('http://localhost:18999/api/test');\n"
        "    var conn = url.openConnection();\n"
        "    conn.connect();\n"
        '    return { result: "ok" };\n'
        "}"
    )
    fid_005, fvid_005 = setup_flow(
        snow_id(), lifecycle_status=2,
        orchestration=build_script_orch(script_005)
    )
    resp = trigger(fid_005, body={"name": "test", "value": 1},
                   headers={"X-Sys-Token": "test-token"})
    verify_sandbox_rejection(resp, "Java interop HttpURLConnection")


if __name__ == "__main__":
    test_script_http_call_sandbox()
    done()
