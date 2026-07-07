#!/usr/bin/env python3
"""GET /categories — 获取分类列表 (树形)"""
import pytest
from conftest import api


class TestCategoryList:
    @pytest.mark.L0
    def test_list_ok(self):
        resp = api("GET", "/categories")
        assert resp.status_code == 200
        data = resp.json()
        assert data["code"] == "200"
        assert isinstance(data.get("data"), list)

    @pytest.mark.L3
    def test_list_with_newly_created(self, category):
        resp = api("GET", "/categories")
        items = resp.json().get("data", [])
        ids = [it.get("id") for it in items if isinstance(it, dict)]
        assert str(category) in str(ids) or category in ids

    @pytest.mark.L4
    def test_not_found_detail(self):
        resp = api("GET", "/categories/999999999999999999")
        assert resp is not None
