#!/usr/bin/env python3
"""#3 GET /connectors/{id} — 连接器详情"""
import pytest
from _client import api


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
