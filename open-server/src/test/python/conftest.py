#!/usr/bin/env python3
"""共享 fixtures — 通过 API 创建测试数据，模拟用户操作"""

import pytest
from common import api, db, db_val, TEST_APP_ID, INTERNAL_APP_ID

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


def assert_operate_log(keyword, expected_count=1):
    """通过 API 验证操作日志是否包含指定关键词"""
    import time
    time.sleep(0.5)
    r = api("GET", f"/operate-log?curPage=1&pageSize=50")
    items = r.json().get("data", [])
    count = 0
    for item in (items if isinstance(items, list) else []):
        text = str(item)
        if str(keyword) in text:
            count += 1
    assert count >= expected_count, f"操作日志未找到关键词 '{keyword}' (预期>={expected_count}，实际={count})"


def _find_approval(flow_version_id):
    """查询指定版本的最新审批记录 ID"""
    r = api("GET", f"/approvals/pending?businessType=connector_flow_version_publish&page=1&size=50")
    items = r.json().get("data", [])
    for item in (items if isinstance(items, list) else []):
        if str(item.get("businessId")) == str(flow_version_id):
            return int(item["id"])
    return 0


def _find_capability_approval(business_id, business_type):
    """查询能力开放平台资源的审批记录 ID"""
    r = api("GET", f"/approvals/pending?businessType={business_type}&page=1&size=50")
    items = r.json().get("data", [])
    if isinstance(items, dict):
        items = items.get("items", items.get("data", []))
    for item in (items if isinstance(items, list) else []):
        if str(item.get("businessId")) == str(business_id):
            return int(item["id"])
    return 0


def _approve_capability_resource(resource_id, business_type):
    """审批准能力开放平台资源（api_register / event_register / callback_register）"""
    aid = _find_capability_approval(resource_id, business_type)
    if aid:
        api("POST", f"/approvals/{aid}/approve", {"comment": "auto"})
    aid2 = _find_capability_approval(resource_id, business_type)
    if aid2:
        api("POST", f"/approvals/{aid2}/approve", {"comment": "auto"})


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


def _create_connector_version(connector_id):
    api("POST", f"/connectors/{connector_id}/versions", {})
    r = api("GET", f"/connectors/{connector_id}/versions?page=1&size=1")
    versions = _get_data(r)
    return int(versions[0]["versionId"])


@pytest.fixture
def draft_connector(connector):
    vid = _create_connector_version(connector)
    return connector, vid


@pytest.fixture
def published_connector(connector):
    vid = _create_connector_version(connector)
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
# 分类 fixture（api/event/callback/permission 依赖）
# ═══════════════════════════════════════════════════════════

@pytest.fixture
def category(request):
    tag = request.node.name.replace("test_", "")[:40]
    r = api("POST", "/categories", {
        "nameCn": f"pytest_{tag}",
        "nameEn": f"pytest_{tag}",
    })
    return int(_get_data(r)["id"])


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


def _create_flow_version(flow_id):
    r = api("POST", f"/flows/{flow_id}/versions", {})
    return int(_get_data(r)["versionId"])


@pytest.fixture
def draft_flow(flow):
    vid = _create_flow_version(flow)
    return flow, vid


def _set_orchestration(flow_id, version_id, connector_id, connector_version_id):
    """设置编排配置并提交发布（status=2 待审批）"""
    api("PUT", f"/flows/{flow_id}/versions/{version_id}", {
        "orchestrationConfig": {
            "flowConfig": {"flowMode": "serial", "timeout": 3000},
            "nodes": [
                {"id": "trigger", "type": "trigger", "data": {"type": "trigger", "triggerType": "http"}},
                {"id": "conn", "type": "connector", "data": {"type": "connector", "connectorId": connector_id, "connectorVersionId": connector_version_id}},
                {"id": "exit", "type": "exit", "data": {"type": "exit"}},
            ],
            "edges": [
                {"id": "e1", "source": "trigger", "target": "conn"},
                {"id": "e2", "source": "conn", "target": "exit"},
            ],
        }
    })
    api("POST", f"/flows/{flow_id}/versions/{version_id}/publish")


def _approve_and_deploy(flow_id, version_id):
    """两级审批通过 + 部署"""
    aid = _find_approval(version_id)
    if aid:
        api("POST", f"/approvals/{aid}/approve", {"comment": "L1 approve"})
        api("POST", f"/approvals/{aid}/approve", {"comment": "L2 approve"})
    api("POST", f"/flows/{flow_id}/deploy", {"versionId": version_id})


@pytest.fixture
def deployed_flow(flow, published_connector):
    """已部署的连接流：create → draft → 编排 → publish → approve → deploy"""
    cid, cvid = published_connector
    vid = _create_flow_version(flow)
    _set_orchestration(flow, vid, cid, cvid)
    _approve_and_deploy(flow, vid)
    return flow, vid


@pytest.fixture
def pending_approval_flow(flow, published_connector):
    """含待审批版本（status=2）+ 关联审批记录的连接流"""
    cid, cvid = published_connector
    vid = _create_flow_version(flow)
    _set_orchestration(flow, vid, cid, cvid)
    aid = _find_approval(vid)
    return flow, vid, aid
