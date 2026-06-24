#!/usr/bin/env python3
"""连接器 CRUD — APIs #1~#7"""
import pytest
from conftest import api, db, connector, draft_connector, published_connector


class TestConnectorCreate:
    def test_create_ok(self):
        resp = api("POST", "/connectors", {"nameCn": "IM 发送消息", "nameEn": "IM Send Message", "connectorType": 1})
        assert resp.status_code == 200
        assert resp.json()["code"] == "200"

    def test_missing_name_cn(self):
        resp = api("POST", "/connectors", {"nameEn": "Test", "connectorType": 1})
        assert resp.status_code == 400

    @pytest.mark.parametrize("ctype", [99, 0, -1])
    def test_invalid_connector_type(self, ctype):
        resp = api("POST", "/connectors", {"nameCn": "测试", "nameEn": "Test", "connectorType": ctype})
        assert resp.status_code == 400


class TestConnectorList:
    def test_list(self):
        resp = api("GET", "/connectors")
        assert resp.status_code == 200
        assert "data" in resp.json()


class TestConnectorDetail:
    def test_detail_ok(self, connector):
        resp = api("GET", f"/connectors/{connector}")
        assert resp.status_code == 200

    def test_not_found(self):
        resp = api("GET", "/connectors/999999999999999999")
        assert resp.status_code == 404


class TestConnectorUpdate:
    def test_update_ok(self, connector):
        resp = api("PUT", f"/connectors/{connector}", {"nameCn": "新名称", "nameEn": "NewName"})
        assert resp.status_code == 200


class TestConnectorDelete:
    def test_delete_without_version(self, connector):
        resp = api("DELETE", f"/connectors/{connector}")
        assert resp.status_code == 200


class TestConnectorLifecycle:
    def test_invalidate(self, published_connector):
        cid, _ = published_connector
        resp = api("PUT", f"/connectors/{cid}/invalidate")
        assert resp.status_code == 200

    def test_recover(self, connector):
        db(f"UPDATE openplatform_v2_cp_connector_t SET status = 3 WHERE id = {connector}")
        resp = api("PUT", f"/connectors/{connector}/recover")
        assert resp.status_code == 200
