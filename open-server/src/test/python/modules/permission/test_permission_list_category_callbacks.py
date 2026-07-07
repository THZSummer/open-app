#!/usr/bin/env python3
"""GET /categories/{id}/callbacks — 获取分类下回调权限列表"""
import pytest
from conftest import api


class TestListCategoryCallbacks:
    @pytest.mark.L1
    def test_ok(self, category):
        resp = api("GET", f"/categories/{category}/callbacks")
        assert resp.status_code == 200
