#!/usr/bin/env python3
"""GET /categories/{id}/apis — 获取分类下 API 权限列表"""
import pytest
from conftest import api


class TestListCategoryApis:
    @pytest.mark.L1
    def test_ok(self, category):
        resp = api("GET", f"/categories/{category}/apis")
        assert resp.status_code == 200
