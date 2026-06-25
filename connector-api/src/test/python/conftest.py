#!/usr/bin/env python3
"""共享 fixtures：自动管理 connector-api 测试数据生命周期

设计原则（对齐 open-server 模式）:
- fixture 自动创建测试数据，测试结束自动清理
- 设置 KEEP_TEST_DATA=1 环境变量可保留数据用于调试
- name_cn 包含测试函数名，可追溯数据来源

提供的 fixtures:
  connector          — 空连接器（无版本）
  published_connector — 含已发布版本 + connectionConfig 的连接器
  flow               — 空连接流
  deployed_flow      — 已部署的连接流（含版本 + deployed_version_id 指针 + 简单编排）
"""
import os
import time
import importlib.util

# 加载 inspect/client.py 中的 DB 基础设施
_spec = importlib.util.spec_from_file_location(
    "inspect_client",
    os.path.join(os.path.dirname(__file__), "inspect", "client.py")
)
_client = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(_client)
db = _client.db
snow_id = _client.snow_id
escape_sql = _client.escape_sql
TEST_APP_ID = _client.TEST_APP_ID

# ═══════════════════════════════════════════════════════════
# 配置
# ═══════════════════════════════════════════════════════════
_KEEP = os.environ.get("KEEP_TEST_DATA", "") == "1"

# ═══════════════════════════════════════════════════════════
# Fixtures
# ═══════════════════════════════════════════════════════════

import pytest


@pytest.fixture
def connector(request):
    """空连接器（无版本），name_cn 含测试名以追溯"""
    cid = snow_id()
    tag = request.node.name.replace("test_", "")[:40]
    db(
        f"INSERT INTO openplatform_v2_cp_connector_t "
        f"(id, name_cn, name_en, connector_type, app_id, create_by, last_update_by) "
        f"VALUES ({cid}, 'pytest_{tag}', 'pytest_{tag}', 1, {TEST_APP_ID}, 'tester', 'tester')"
    )
    yield cid
    if not _KEEP:
        db(f"DELETE FROM openplatform_v2_cp_connector_version_t WHERE connector_id = {cid}")
        db(f"DELETE FROM openplatform_v2_cp_connector_t WHERE id = {cid}")


@pytest.fixture
def published_connector(connector, request):
    """含已发布版本的连接器（含 connectionConfig），返回 (connector_id, version_id)"""
    vid = snow_id()
    config = {
        "protocol": "HTTP",
        "protocolConfig": {
            "url": "https://httpbin.org/get",
            "method": "GET"
        },
        "timeoutMs": 5000
    }
    db(
        f"INSERT INTO openplatform_v2_cp_connector_version_t "
        f"(id, connector_id, version_number, status, connection_config, create_by, last_update_by) "
        f"VALUES ({vid}, {connector}, 1, 2, '{escape_sql(config)}', 'tester', 'tester')"
    )
    yield connector, vid


@pytest.fixture
def flow(request):
    """空连接流，name_cn 含测试名以追溯"""
    fid = snow_id()
    tag = request.node.name.replace("test_", "")[:40]
    db(
        f"INSERT INTO openplatform_v2_cp_flow_t "
        f"(id, name_cn, name_en, lifecycle_status, app_id, create_by, last_update_by) "
        f"VALUES ({fid}, 'pytest_flow_{tag}', 'pytest_flow_{tag}', 1, {TEST_APP_ID}, 'tester', 'tester')"
    )
    yield fid
    if not _KEEP:
        db(f"DELETE FROM openplatform_v2_cp_flow_version_t WHERE flow_id = {fid}")
        db(f"DELETE FROM openplatform_v2_cp_connector_version_ref_t WHERE flow_id = {fid}")
        db(f"DELETE FROM openplatform_v2_cp_flow_t WHERE id = {fid}")


@pytest.fixture
def deployed_flow(flow, request):
    """已部署的连接流（含已发布版本 + deployed_version_id 指针 + 简单编排），返回 (flow_id, version_id)"""
    vid = snow_id()
    orch = {
        "nodes": [
            {
                "id": "node_trigger", "type": "trigger",
                "position": {"x": 100, "y": 200},
                "data": {
                    "labelCn": "接收", "labelEn": "Receive", "type": "http",
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
                            "properties": {
                                "sender": {"type": "string"},
                                "content": {"type": "string"}
                            },
                            "required": ["sender"]
                        }
                    },
                    "rateLimitConfig": {"maxQps": 100}
                }
            },
            {
                "id": "node_exit", "type": "exit",
                "position": {"x": 350, "y": 200},
                "data": {
                    "labelCn": "返回", "labelEn": "Return",
                    "outputMapping": {
                        "header": {"type": "object", "properties": {}},
                        "body": {
                            "type": "object",
                            "properties": {
                                "echo": {"type": "string", "value": "${$.node.node_trigger.input.body.sender}"}
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
    db(
        f"INSERT INTO openplatform_v2_cp_flow_version_t "
        f"(id, flow_id, version_number, status, orchestration_config, create_by, last_update_by) "
        f"VALUES ({vid}, {flow}, 1, 5, '{escape_sql(orch)}', 'tester', 'tester')"
    )
    db(
        f"UPDATE openplatform_v2_cp_flow_t "
        f"SET deployed_version_id = {vid}, deployed_version_number = 1 "
        f"WHERE id = {flow}"
    )
    yield flow, vid
