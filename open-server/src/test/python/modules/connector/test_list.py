#!/usr/bin/env python3
"""#2 GET /connectors — 连接器列表"""
import pytest
from common import api


class TestConnectorList:
    @pytest.mark.L0
    def test_list(self):
        resp = api("GET", "/connectors")
        assert resp.status_code == 200
        data = resp.json()
        assert "data" in data
        assert data["code"] == "200"

    @pytest.mark.L0
    def test_list_all(self):
        resp = api("GET", "/connectors?curPage=1&pageSize=10")
        assert resp.status_code == 200
        data = resp.json()
        assert "data" in data
        assert "page" in data

    @pytest.mark.L1
    def test_pagination(self):
        """验证分页参数生效：pageSize 与返回一致"""
        resp = api("GET", "/connectors?curPage=1&pageSize=3")
        assert resp.status_code == 200
        data = resp.json()
        page = data.get("page", {})
        assert page.get("pageSize") == 3
        assert page.get("curPage") == 1
        # 返回数据量不超过 pageSize
        items = data.get("data", [])
        assert len(items) <= 3

    @pytest.mark.L1
    def test_keyword_filter(self):
        """验证关键词过滤不报错"""
        resp = api("GET", "/connectors?keyword=pytest&curPage=1&pageSize=10")
        assert resp.status_code == 200

    @pytest.mark.L1
    def test_status_filter(self):
        """验证 status 过滤不报错"""
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
