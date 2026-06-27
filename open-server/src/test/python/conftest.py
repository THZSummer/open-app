#!/usr/bin/env python3
"""共享 fixtures：自动管理测试数据生命周期"""
import os, time, importlib.util
import pytest

_spec = importlib.util.spec_from_file_location(
    "inspect_client",
    os.path.join(os.path.dirname(__file__), "inspect", "client.py")
)
_client = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(_client)
api = _client.api
db = _client.db
db_val = _client.db_val
TEST_APP_ID = _client.TEST_APP_ID

_KEEP = os.environ.get("KEEP_TEST_DATA", "") == "1"

_snow_seq = 0

def _snow_id():
    global _snow_seq
    _snow_seq += 1
    # 纳秒时间 + 自增序列，确保同进程内高频调用也唯一
    return (int(time.time_ns() / 1000) % 100000000000000000) + _snow_seq

# ═══════════════════════════════════════════════════════════
# 会话级 Property 默认值 (14 项 + App 白名单)
# ═══════════════════════════════════════════════════════════
_PLATFORM_PROPS = [
    ('connector_max_versions',          '连接器版本数量上限',       '1000'),
    ('url_regex_pattern',               '连接器URL正则规则',       '^https?://.*'),
    ('connector_config_max_bytes',      '连接器配置JSON长度上限',   '1048576'),
    ('flow_max_versions',               '连接流版本数量上限',       '1000'),
    ('max_execution_records_per_flow',  '运行记录条数上限',         '1000'),
    ('node_max_timeout_seconds',        '连接器节点超时上限',       '5'),
    ('flow_config_max_bytes',           '连接流配置JSON长度上限',   '1048576'),
    ('flow_max_qps',                    '连接流最大QPS',           '1000'),
    ('flow_max_concurrency',            '连接流最大并发',           '1000'),
    ('flow_max_cache_ttl_seconds',      '连接流缓存TTL上限',       '1296000'),
    ('flow_max_parallel_branches',      '连接流并行节点分支上限',   '8'),
    ('script_max_length_chars',         '脚本源码长度上限',         '10000'),
    ('script_max_timeout_seconds',      '脚本超时范围',             '30'),
    ('log_collection_enabled',          '日志采集开关',             'true'),
]

@pytest.fixture(scope="session", autouse=True)
def platform_property_defaults():
    """在全部测试开始前一次性插入 14 项 connector_platform Property + App 白名单 Lookup。
    测试直接读 DB，不再需要 _set_property 手动插入。"""
    _prop_ids = [_snow_id() for _ in _PLATFORM_PROPS]
    _classify_id = _snow_id()
    _item_id = _snow_id()

    for i, (code, name, value) in enumerate(_PLATFORM_PROPS):
        db(f"INSERT INTO openplatform_property_t (id, path, code, name, value, status) "
           f"VALUES ({_prop_ids[i]}, 'connector_platform', '{code}', '{name}', '{value}', 1)")

    db(f"INSERT INTO openplatform_lookup_classify_t (classify_id, classify_code, classify_name, path, status) "
       f"VALUES ({_classify_id}, 'app_whitelist', '连接器平台开放应用范围', 'connector_platform', 1)")

    db(f"INSERT INTO openplatform_lookup_item_t (item_id, classify_id, item_code, item_name, item_value, status) "
       f"VALUES ({_item_id}, {_classify_id}, '001', '测试应用', '{TEST_APP_ID}', 1)")

    yield

    if not _KEEP:
        db(f"DELETE FROM openplatform_lookup_item_t WHERE item_id = {_item_id}")
        db(f"DELETE FROM openplatform_lookup_classify_t WHERE classify_id = {_classify_id}")
        for pid in _prop_ids:
            db(f"DELETE FROM openplatform_property_t WHERE id = {pid}")


@pytest.fixture
def connector(request):
    """空连接器（无版本），name_cn 含测试名以追溯"""
    cid = _snow_id()
    tag = request.node.name.replace("test_", "")[:40]
    db(f"INSERT INTO openplatform_v2_cp_connector_t (id, name_cn, name_en, connector_type, app_id, create_by, last_update_by) VALUES ({cid}, 'pytest_{tag}', 'pytest_{tag}', 1, {TEST_APP_ID}, 'tester', 'tester')")
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
    db(f"INSERT INTO openplatform_v2_cp_flow_t (id, name_cn, name_en, lifecycle_status, app_id, create_by, last_update_by) VALUES ({fid}, 'pytest_flow_{tag}', 'pytest_flow_{tag}', 1, {TEST_APP_ID}, 'tester', 'tester')")
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
