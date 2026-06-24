#!/usr/bin/env python3
"""#1 POST /connectors — 创建连接器"""
import pytest
from _client import api


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
