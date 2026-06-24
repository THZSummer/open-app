#!/usr/bin/env python3
"""#2 GET /connectors — 连接器列表"""
import pytest
from _client import api


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
