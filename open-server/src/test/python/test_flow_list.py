#!/usr/bin/env python3
"""#18 GET /flows — 连接流列表"""
import pytest
from _client import api


class TestFlowList:
    @pytest.mark.L0
    def test_default_pagination(self):
        resp = api("GET", "/flows")
        assert resp.status_code == 200
        assert "data" in resp.json()

    @pytest.mark.L1
    def test_lifecycle_status_filter(self):
        resp = api("GET", "/flows", {"lifecycleStatus": 0})
        assert resp.status_code == 200

    @pytest.mark.L1
    def test_keyword_search(self):
        resp = api("GET", "/flows", {"keyword": "通知"})
        assert resp.status_code == 200

    @pytest.mark.L2
    def test_empty_keyword(self):
        resp = api("GET", "/flows", {"keyword": "NONEXISTENT_FLOW_9999"})
        assert resp.status_code == 200

    @pytest.mark.L0
    def test_list_all(self):
        resp = api("GET", "/flows?curPage=1&pageSize=10")
        assert resp.status_code == 200
        assert "data" in resp.json()

    @pytest.mark.L1
    def test_pagination(self):
        resp = api("GET", "/flows?curPage=1&pageSize=2")
        assert resp.status_code == 200
        page = resp.json().get("page", {})
        assert page.get("pageSize") == 2

    @pytest.mark.L1
    def test_keyword_filter(self):
        resp = api("GET", "/flows?keyword=pytest&curPage=1&pageSize=10")
        assert resp.status_code == 200

    @pytest.mark.L1
    def test_status_filter(self):
        resp = api("GET", "/flows?lifecycleStatus=2&curPage=1&pageSize=10")
        assert resp.status_code == 200

    @pytest.mark.L2
    def test_empty_result(self):
        resp = api("GET", "/flows?keyword=zzz_nonexistent_zzz&curPage=1&pageSize=10")
        assert resp.status_code == 200
