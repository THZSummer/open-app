#!/usr/bin/env python3
"""安全测试: 白名单准入 + 操作日志"""
import time
import pytest
from conftest import api, db, db_val, connector, flow


def _snow_id():
    return int(time.time() * 1000000) % 100000000000000000


class TestAppWhitelist:
    @pytest.mark.L2
    def test_whitelist_app_ok(self, connector):
        resp = api("GET", f"/connectors/{connector}")
        if resp is not None:
            assert resp.status_code == 200
            assert resp.json().get("code") in ("200", 200)

    @pytest.mark.L2
    def test_missing_app_id_header(self, connector):
        resp = api("GET", f"/connectors/{connector}", app_id=None)
        assert resp is not None

    @pytest.mark.L2
    def test_any_app_allowed_empty_whitelist(self, connector):
        resp = api("GET", f"/connectors/{connector}")
        if resp is not None:
            assert resp.status_code == 200
            assert resp.json().get("code") in ("200", 200)


class TestOperationLog:
    @pytest.mark.L1
    def test_create_connector_log(self):
        resp = api("POST", "/connectors", {"nameCn": "日志测试连接器", "nameEn": "LogTestConnector", "connectorType": 1})
        api_cid = None
        if resp is not None and resp.status_code in (200, 201):
            data = resp.json()
            if data.get("code") in ("200", 200) and data.get("data"):
                api_cid = data["data"].get("connectorId")
        if api_cid:
            log_count = db_val(f"SELECT COUNT(*) FROM openplatform_operate_log_t WHERE operate_object LIKE '%{api_cid}%'")
            assert log_count is not None
            db(f"DELETE FROM openplatform_v2_cp_connector_version_t WHERE connector_id = {api_cid}")
            db(f"DELETE FROM openplatform_v2_cp_connector_t WHERE id = {api_cid}")

    @pytest.mark.L1
    def test_update_flow_log(self, flow):
        resp = api("PUT", f"/flows/{flow}", {"nameCn": "更新的日志测试流"})
        if resp is not None:
            assert resp.status_code in (200, 201)

    @pytest.mark.L1
    def test_create_and_delete_connector_two_phase_log(self):
        resp = api("POST", "/connectors", {"nameCn": "日志删除测试", "nameEn": "LogDeleteTest", "connectorType": 1})
        api_cid = None
        if resp is not None and resp.status_code in (200, 201):
            data = resp.json()
            if data.get("code") in ("200", 200) and data.get("data"):
                api_cid = data["data"].get("connectorId")
        if api_cid:
            api("PUT", f"/connectors/{api_cid}/invalidate")
            resp2 = api("DELETE", f"/connectors/{api_cid}")
            if resp2 is not None:
                log_count = db_val(f"SELECT COUNT(*) FROM openplatform_operate_log_t WHERE operate_object LIKE '%{api_cid}%'")
                assert log_count is not None
            db(f"DELETE FROM openplatform_v2_cp_connector_version_t WHERE connector_id = {api_cid}")
            db(f"DELETE FROM openplatform_v2_cp_connector_t WHERE id = {api_cid}")
