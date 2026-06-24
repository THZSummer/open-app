#!/usr/bin/env python3
"""#18 GET /flows — 连接流列表"""
import pytest
from _client import api


class TestFlowList:
    @pytest.mark.L0
    def test_default_pagination(self):
        resp = api("GET", "/flows")
        assert resp.status_code == 200
        data = resp.json()
        assert data["code"] == "200"
        assert "data" in data

    @pytest.mark.L0
    def test_list_all(self):
        resp = api("GET", "/flows?curPage=1&pageSize=10")
        assert resp.status_code == 200
        data = resp.json()
        assert "data" in data
        assert "page" in data

    @pytest.mark.L1
    def test_lifecycle_status_filter(self):
        """验证 lifecycleStatus 过滤不报错"""
        resp = api("GET", "/flows?lifecycleStatus=1")
        assert resp.status_code == 200

    @pytest.mark.L1
    def test_keyword_search(self):
        """验证关键词搜索不报错"""
        resp = api("GET", "/flows?keyword=pytest")
        assert resp.status_code == 200

    @pytest.mark.L1
    def test_pagination(self):
        """验证分页参数生效"""
        resp = api("GET", "/flows?curPage=1&pageSize=3")
        assert resp.status_code == 200
        data = resp.json()
        page = data.get("page", {})
        assert page.get("pageSize") == 3
        assert page.get("curPage") == 1
        items = data.get("data", [])
        assert len(items) <= 3

    @pytest.mark.L2
    def test_keyword_no_match(self):
        resp = api("GET", "/flows?keyword=zzz_nonexistent_zzz")
        assert resp.status_code == 200

    @pytest.mark.L2
    def test_deployed_version_info(self):
        resp = api("GET", "/flows?lifecycleStatus=2")
        assert resp.status_code == 200
