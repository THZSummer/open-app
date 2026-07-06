#!/usr/bin/env python3
"""共享 fixtures — 通过 API 创建测试数据，不写 DB"""

import pytest
from common import api, db, db_val, TEST_APP_ID

INTERNAL_APP_ID = int(db_val(f"SELECT id FROM openplatform_app_t WHERE app_id = '{TEST_APP_ID}' AND status = 1"))

# ═══════════════════════════════════════════════════════════
# 会话级 Lookup 默认值（唯一保留的 DB 操作——无对应 API）
# ═══════════════════════════════════════════════════════════
_LOOKUP_CONFIG_ITEMS = [
    ('Connector.Max.Versions',              '1000'),
    ('Connector.Url.Regex.Pattern',         '^https?://.*'),
    ('Connector.Config.Max.Bytes',          '1048576'),
    ('Flow.Max.Versions',                   '1000'),
    ('Max.Execution.Records.Per.Flow',      '1000'),
    ('Node.Max.Timeout.Seconds',            '5'),
    ('Flow.Config.Max.Bytes',               '1048576'),
    ('Flow.Max.Qps',                        '1000'),
    ('Flow.Max.Concurrency',                '1000'),
    ('Flow.Max.Cache.Ttl.Seconds',          '1296000'),
    ('Flow.Max.Parallel.Branches',          '8'),
    ('Flow.Max.Serial.Connector.Nodes',     '3'),
    ('Script.Max.Length.Chars',             '10000'),
    ('Script.Max.Timeout.Seconds',          '30'),
    ('Log.Collection.Enabled',              'true'),
]

import os, time

_snow_seq = 0

def _snow_id():
    global _snow_seq
    _snow_seq += 1
    return (int(time.time_ns() / 1000) % 100000000000000000) + _snow_seq


@pytest.fixture(scope="session", autouse=True)
def platform_property_defaults():
    existing = db_val("SELECT classify_id FROM openplatform_lookup_classify_t WHERE classify_code = 'Connector.Platform.Config' AND path = 'CEC.Open' AND status = 1")
    if existing is None:
        _config_classify_id = _snow_id()
        db(f"INSERT INTO openplatform_lookup_classify_t (classify_id, classify_code, classify_name, path, status) VALUES ({_config_classify_id}, 'Connector.Platform.Config', '连接器平台默认配置', 'CEC.Open', 1)")
        for item_code, item_value in _LOOKUP_CONFIG_ITEMS:
            item_id = _snow_id()
            db(f"INSERT INTO openplatform_lookup_item_t (item_id, classify_id, item_code, item_name, item_value, status) VALUES ({item_id}, {_config_classify_id}, '{item_code}', '{item_code}', '{item_value}', 1)")
    existing_whitelist = db_val("SELECT classify_id FROM openplatform_lookup_classify_t WHERE classify_code = 'Connector.Platform.AppWhitelist' AND path = 'CEC.Open' AND status = 1")
    if existing_whitelist is None:
        _whitelist_classify_id = _snow_id()
        db(f"INSERT INTO openplatform_lookup_classify_t (classify_id, classify_code, classify_name, path, status) VALUES ({_whitelist_classify_id}, 'Connector.Platform.AppWhitelist', '连接器平台开放应用范围', 'CEC.Open', 1)")
        _whitelist_item_id = _snow_id()
        db(f"INSERT INTO openplatform_lookup_item_t (item_id, classify_id, item_code, item_name, item_value, status) VALUES ({_whitelist_item_id}, {_whitelist_classify_id}, '{TEST_APP_ID}', '测试应用', '{TEST_APP_ID}', 1)")
    yield


def _get_data(resp):
    return resp.json()["data"]


def _create_version(connector_id):
    api("POST", f"/connectors/{connector_id}/versions", {})
    r = api("GET", f"/connectors/{connector_id}/versions?page=1&size=1")
    versions = _get_data(r)
    return int(versions[0]["versionId"]) if isinstance(versions, list) else int(versions["data"][0]["versionId"])


def _create_flow_version(flow_id):
    r = api("POST", f"/flows/{flow_id}/versions", {})
    d = _get_data(r)
    return int(d.get("versionId", d.get("id", 0)))


# ═══════════════════════════════════════════════════════════
# 连接器 fixtures
# ═══════════════════════════════════════════════════════════

@pytest.fixture
def connector(request):
    tag = request.node.name.replace("test_", "")[:40]
    r = api("POST", "/connectors", {
        "nameCn": f"pytest_{tag}",
        "nameEn": f"pytest_{tag}",
        "connectorType": 1,
    })
    return int(_get_data(r)["connectorId"])


@pytest.fixture
def draft_connector(connector):
    vid = _create_version(connector)
    return connector, vid


@pytest.fixture
def published_connector(connector):
    vid = _create_version(connector)
    api("PUT", f"/connectors/{connector}/versions/{vid}", {
        "connectionConfig": {
            "protocol": "HTTP",
            "protocolConfig": {"url": "https://httpbin.org/get", "method": "GET"},
            "timeoutMs": 5000,
        }
    })
    api("PUT", f"/connectors/{connector}/versions/{vid}/publish")
    return connector, vid


# ═══════════════════════════════════════════════════════════
# 连接流 fixtures
# ═══════════════════════════════════════════════════════════

@pytest.fixture
def flow(request):
    tag = request.node.name.replace("test_", "")[:40]
    r = api("POST", "/flows", {
        "nameCn": f"pytest_flow_{tag}",
        "nameEn": f"pytest_flow_{tag}",
    })
    return int(_get_data(r)["flowId"])


@pytest.fixture
def draft_flow(flow):
    vid = _create_flow_version(flow)
    return flow, vid


@pytest.fixture
def deployed_flow(flow, published_connector):
    """已部署的连接流（通过 publish → deploy API），依赖已发布的连接器"""
    cid, cvid = published_connector
    vid = _create_flow_version(flow)
    api("PUT", f"/flows/{flow}/versions/{vid}", {
        "orchestrationConfig": {
            "flowConfig": {"flowMode": "serial", "timeout": 3000},
            "nodes": [
                {"id": "trigger", "type": "trigger", "data": {"type": "trigger", "triggerType": "http"}},
                {"id": "conn", "type": "connector", "data": {"type": "connector", "connectorId": cid, "connectorVersionId": cvid}},
                {"id": "exit", "type": "exit", "data": {"type": "exit"}},
            ],
            "edges": [
                {"id": "e1", "source": "trigger", "target": "conn"},
                {"id": "e2", "source": "conn", "target": "exit"},
            ],
        }
    })
    api("POST", f"/flows/{flow}/versions/{vid}/publish")
    api("POST", f"/flows/{flow}/deploy", {"versionId": vid})
    return flow, vid


@pytest.fixture
def pending_approval_flow(flow, connector):
    """含待审批版本 + 关联审批记录的连接流，依赖 connector"""
    cid = connector
    # 创建并发布一个 connector 版本用于编排
    cv = _create_version(cid)
    api("PUT", f"/connectors/{cid}/versions/{cv}", {
        "connectionConfig": {
            "protocol": "HTTP",
            "protocolConfig": {"url": "https://httpbin.org/get", "method": "GET"},
            "timeoutMs": 5000,
        }
    })
    api("PUT", f"/connectors/{cid}/versions/{cv}/publish")
    vid = _create_flow_version(flow)
    api("PUT", f"/flows/{flow}/versions/{vid}", {
        "orchestrationConfig": {
            "flowConfig": {"flowMode": "serial", "timeout": 3000},
            "nodes": [
                {"id": "trigger", "type": "trigger", "data": {"type": "trigger", "triggerType": "http"}},
                {"id": "conn", "type": "connector", "data": {"type": "connector", "connectorId": cid, "connectorVersionId": cv}},
                {"id": "exit", "type": "exit", "data": {"type": "exit"}},
            ],
            "edges": [
                {"id": "e1", "source": "trigger", "target": "conn"},
                {"id": "e2", "source": "conn", "target": "exit"},
            ],
        }
    })
    api("POST", f"/flows/{flow}/versions/{vid}/publish")
    r = api("GET", f"/approvals/pending?businessType=connector_flow_version_publish&page=1&size=10")
    items = _get_data(r)
    ar_id = 0
    for item in (items if isinstance(items, list) else []):
        if str(item.get("businessId")) == str(vid):
            ar_id = int(item["id"])
            break
    return flow, vid, ar_id
