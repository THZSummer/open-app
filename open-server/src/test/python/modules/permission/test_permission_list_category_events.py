#!/usr/bin/env python3
"""GET /categories/{id}/events — 获取分类下事件权限列表"""
import pytest
from conftest import api


class TestListCategoryEvents:
    @pytest.mark.L1
    def test_ok(self, category):
        resp = api("GET", f"/categories/{category}/events")
        assert resp.status_code == 200
