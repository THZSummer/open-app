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

def _snow_id():
    return int(time.time() * 1000000) % 100000000000000000

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
    db(f"INSERT INTO openplatform_v2_cp_flow_version_t (id, flow_id, version_number, status, orchestration_config, create_by, last_update_by) VALUES ({vid}, {flow}, 1, 5, '{{\"trigger\":{{}},\"nodes\":[],\"edges\":[]}}', 'tester', 'tester')")
    db(f"UPDATE openplatform_v2_cp_flow_t SET deployed_version_id = {vid}, deployed_version_number = 1 WHERE id = {flow}")
    yield flow, vid
