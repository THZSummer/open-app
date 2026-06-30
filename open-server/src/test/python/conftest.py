#!/usr/bin/env python3
"""共享 fixtures：自动管理测试数据生命周期"""
import os, time
import pytest
from _client import api, db, db_val, TEST_APP_ID

INTERNAL_APP_ID = int(db_val(f"SELECT id FROM openplatform_app_t WHERE app_id = '{TEST_APP_ID}' AND status = 1"))

_KEEP = os.environ.get("KEEP_TEST_DATA", "") == "1"

_snow_seq = 0

def _snow_id():
    global _snow_seq
    _snow_seq += 1
    # 纳秒时间 + 自增序列，确保同进程内高频调用也唯一
    return (int(time.time_ns() / 1000) % 100000000000000000) + _snow_seq

# ═══════════════════════════════════════════════════════════
# 会话级 Lookup 默认值 (15 项配置 + App 白名单)
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

@pytest.fixture(scope="session", autouse=True)
def platform_property_defaults():
    """在全部测试开始前一次性插入 15 项 Lookup 配置 + App 白名单 Lookup。
    测试直接读 DB，不再需要 _set_property/_set_lookup_item 手动插入。"""
    # 平台默认配置 — 如果已存在则跳过（保留上次测试的数据）
    existing = db_val("SELECT classify_id FROM openplatform_lookup_classify_t WHERE classify_code = 'Connector.Platform.Config' AND path = 'CEC.Open' AND status = 1")
    if existing is None:
        _config_classify_id = _snow_id()
        db(f"INSERT INTO openplatform_lookup_classify_t (classify_id, classify_code, classify_name, path, status) VALUES ({_config_classify_id}, 'Connector.Platform.Config', '连接器平台默认配置', 'CEC.Open', 1)")
        for item_code, item_value in _LOOKUP_CONFIG_ITEMS:
            item_id = _snow_id()
            db(f"INSERT INTO openplatform_lookup_item_t (item_id, classify_id, item_code, item_name, item_value, status) VALUES ({item_id}, {_config_classify_id}, '{item_code}', '{item_code}', '{item_value}', 1)")
    else:
        _config_classify_id = existing

    # 白名单 — 同上
    existing_whitelist = db_val("SELECT classify_id FROM openplatform_lookup_classify_t WHERE classify_code = 'Connector.Platform.AppWhitelist' AND path = 'CEC.Open' AND status = 1")
    if existing_whitelist is None:
        _whitelist_classify_id = _snow_id()
        db(f"INSERT INTO openplatform_lookup_classify_t (classify_id, classify_code, classify_name, path, status) VALUES ({_whitelist_classify_id}, 'Connector.Platform.AppWhitelist', '连接器平台开放应用范围', 'CEC.Open', 1)")
        _whitelist_item_id = _snow_id()
        db(f"INSERT INTO openplatform_lookup_item_t (item_id, classify_id, item_code, item_name, item_value, status) VALUES ({_whitelist_item_id}, {_whitelist_classify_id}, '{TEST_APP_ID}', '测试应用', '{TEST_APP_ID}', 1)")

    yield


@pytest.fixture
def connector(request):
    """空连接器（无版本），name_cn 含测试名以追溯"""
    cid = _snow_id()
    tag = request.node.name.replace("test_", "")[:40]
    db(f"INSERT INTO openplatform_v2_cp_connector_t (id, name_cn, name_en, connector_type, app_id, create_by, last_update_by) VALUES ({cid}, 'pytest_{tag}', 'pytest_{tag}', 1, {INTERNAL_APP_ID}, 'tester', 'tester')")
    yield cid
    if not _KEEP:
        db(f"DELETE FROM openplatform_v2_cp_connector_version_t WHERE connector_id = {cid}")
        db(f"DELETE FROM openplatform_v2_cp_connector_t WHERE id = {cid}")

@pytest.fixture
def draft_connector(connector, request):
    """含草稿版本的连接器"""
    vid = _snow_id()
    db(f"INSERT INTO openplatform_v2_cp_connector_version_t (id, connector_id, version_number, status, create_by, last_update_by) VALUES ({vid}, {connector}, 1, 1, 'tester', 'tester')")
    yield connector, vid

@pytest.fixture
def published_connector(connector, request):
    """含已发布版本的连接器"""
    vid = _snow_id()
    db(f"INSERT INTO openplatform_v2_cp_connector_version_t (id, connector_id, version_number, status, connection_config, create_by, last_update_by) VALUES ({vid}, {connector}, 1, 2, '{{\"protocol\":\"HTTP\",\"protocolConfig\":{{\"url\":\"https://httpbin.org/get\",\"method\":\"GET\"}},\"timeoutMs\":5000}}', 'tester', 'tester')")
    yield connector, vid

@pytest.fixture
def flow(request):
    """空连接流，name_cn 含测试名以追溯"""
    fid = _snow_id()
    tag = request.node.name.replace("test_", "")[:40]
    db(f"INSERT INTO openplatform_v2_cp_flow_t (id, name_cn, name_en, lifecycle_status, app_id, create_by, last_update_by) VALUES ({fid}, 'pytest_flow_{tag}', 'pytest_flow_{tag}', 1, {INTERNAL_APP_ID}, 'tester', 'tester')")
    yield fid
    if not _KEEP:
        db(f"DELETE FROM openplatform_v2_cp_flow_version_t WHERE flow_id = {fid}")
        db(f"DELETE FROM openplatform_v2_cp_connector_version_ref_t WHERE flow_id = {fid}")
        db(f"DELETE FROM openplatform_v2_cp_flow_t WHERE id = {fid}")

@pytest.fixture
def draft_flow(flow, request):
    """含草稿版本的连接流"""
    vid = _snow_id()
    db(f"INSERT INTO openplatform_v2_cp_flow_version_t (id, flow_id, version_number, status, create_by, last_update_by) VALUES ({vid}, {flow}, 1, 1, 'tester', 'tester')")
    yield flow, vid

@pytest.fixture
def deployed_flow(flow, request):
    """已部署的连接流（含已发布版本 + deployed_version_id 指针）"""
    vid = _snow_id()
    db(f"INSERT INTO openplatform_v2_cp_flow_version_t (id, flow_id, version_number, status, orchestration_config, create_by, last_update_by) VALUES ({vid}, {flow}, 1, 5, '{{\"nodes\":[{{\"id\":\"trigger\",\"type\":\"trigger\",\"data\":{{\"triggerType\":\"http\"}}}}],\"edges\":[]}}', 'tester', 'tester')")
    db(f"UPDATE openplatform_v2_cp_flow_t SET deployed_version_id = {vid}, deployed_version_number = 1 WHERE id = {flow}")
    yield flow, vid

@pytest.fixture
def pending_approval_flow(flow, request):
    """含待审批版本 + 审批记录的连接流（用于测试审批人/审批地址字段）"""
    vid = _snow_id()
    ar_id = _snow_id()
    tag = request.node.name.replace("test_", "")[:40]
    # 创建待审批版本 (status=2 = PENDING_APPROVAL)
    db(f"INSERT INTO openplatform_v2_cp_flow_version_t (id, flow_id, version_number, status, orchestration_config, create_by, last_update_by) VALUES ({vid}, {flow}, 1, 2, '{{\"nodes\":[{{\"id\":\"trigger\",\"type\":\"trigger\",\"data\":{{\"triggerType\":\"http\"}}}}],\"edges\":[]}}', 'tester', 'tester')")
    # 创建审批记录（含 combined_nodes + current_node=0）
    db(f"INSERT INTO openplatform_v2_approval_record_t (id, combined_nodes, business_type, business_id, applicant_id, applicant_name, status, current_node, create_by, last_update_by) VALUES ({ar_id}, '[{{\"userId\":\"approver001\",\"userName\":\"测试审批人\",\"order\":1,\"level\":\"global\"}}]', 'connector_flow_version_publish', {vid}, 'admin', '管理员', 0, 0, 'admin', 'admin')")
    yield flow, vid, ar_id
    if not _KEEP:
        db(f"DELETE FROM openplatform_v2_approval_record_t WHERE id = {ar_id}")
