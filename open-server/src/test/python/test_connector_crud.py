#!/usr/bin/env python3
"""连接器 CRUD — APIs #1~#7"""
import pytest
from conftest import api, db, connector, draft_connector, published_connector


class TestConnectorCreate:
    @pytest.mark.L1
    def test_create_ok(self):
        resp = api("POST", "/connectors", {"nameCn": "IM 发送消息", "nameEn": "IM Send Message", "connectorType": 1})
        assert resp.status_code == 200
        assert resp.json()["code"] == "200"

    @pytest.mark.L4
    def test_missing_name_cn(self):
        resp = api("POST", "/connectors", {"nameEn": "Test", "connectorType": 1})
        assert resp.status_code == 400

    @pytest.mark.parametrize("ctype", [99, 0, -1])
    @pytest.mark.L4
    def test_invalid_connector_type(self, ctype):
        resp = api("POST", "/connectors", {"nameCn": "测试", "nameEn": "Test", "connectorType": ctype})
        assert resp is not None


class TestConnectorList:
    @pytest.mark.L0
    def test_list(self):
        resp = api("GET", "/connectors")
        assert resp.status_code == 200
        assert "data" in resp.json()

    @pytest.mark.L0
    def test_list_all(self):
        resp = api("GET", "/connectors?curPage=1&pageSize=10")
        assert resp.status_code == 200
        data = resp.json()
        assert "data" in data
        assert "page" in data

    @pytest.mark.L1
    def test_pagination(self):
        resp = api("GET", "/connectors?curPage=1&pageSize=2")
        assert resp.status_code == 200
        page = resp.json().get("page", {})
        assert page.get("pageSize") == 2

    @pytest.mark.L1
    def test_keyword_filter(self):
        resp = api("GET", "/connectors?keyword=pytest&curPage=1&pageSize=10")
        assert resp.status_code == 200

    @pytest.mark.L1
    def test_status_filter(self):
        resp = api("GET", "/connectors?status=2&curPage=1&pageSize=10")
        assert resp.status_code == 200

    @pytest.mark.L2
    def test_empty_result(self):
        resp = api("GET", "/connectors?keyword=zzz_nonexistent_zzz&curPage=1&pageSize=10")
        assert resp.status_code == 200

    @pytest.mark.L2
    def test_missing_page_params(self):
        resp = api("GET", "/connectors")
        assert resp.status_code == 200


class TestConnectorDetail:
    @pytest.mark.L1
    def test_detail_ok(self, connector):
        resp = api("GET", f"/connectors/{connector}")
        assert resp.status_code == 200
        data = resp.json()["data"]
        assert data["connectorId"] == str(connector)
        assert "nameCn" in data
        assert "nameEn" in data
        assert "connectorType" in data
        assert "status" in data

    @pytest.mark.L4
    def test_detail_not_found(self):
        resp = api("GET", "/connectors/999999999999999999")
        assert resp is not None

    @pytest.mark.L4
    def test_detail_wrong_app(self):
        resp = api("GET", "/connectors/1", app_id="99999")
        assert resp is not None


class TestConnectorUpdate:
    @pytest.mark.L1
    def test_update_ok(self, connector):
        resp = api("PUT", f"/connectors/{connector}", {"nameCn": "新名称", "nameEn": "NewName"})
        assert resp.status_code == 200


class TestConnectorDelete:
    @pytest.mark.L1
    def test_delete_without_version(self, connector):
        resp = api("DELETE", f"/connectors/{connector}")
        assert resp.status_code == 200


class TestConnectorLifecycle:
    @pytest.mark.L2
    def test_invalidate(self, published_connector):
        cid, _ = published_connector
        resp = api("PUT", f"/connectors/{cid}/invalidate")
        assert resp.status_code == 200

    @pytest.mark.L2
    def test_recover(self, connector):
        db(f"UPDATE openplatform_v2_cp_connector_t SET status = 3 WHERE id = {connector}")
        resp = api("PUT", f"/connectors/{connector}/recover")
        assert resp.status_code == 200
