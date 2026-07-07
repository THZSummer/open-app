#!/usr/bin/env python3
"""GET /categories/{id} — 获取分类详情"""
import pytest
from conftest import api


class TestCategoryDetail:
    @pytest.mark.L1
    def test_detail_ok(self, category):
        resp = api("GET", f"/categories/{category}")
        assert resp.status_code == 200
        data = resp.json()["data"]
        assert int(data["id"]) == category

    @pytest.mark.L4
    def test_detail_not_found(self):
        resp = api("GET", "/categories/999999999999999999")
        assert resp is not None
