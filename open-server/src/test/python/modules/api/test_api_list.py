#!/usr/bin/env python3
"""GET /apis — 获取 API 列表"""
import pytest
from conftest import api


class TestApiList:
    @pytest.mark.L0
    def test_list_ok(self):
        resp = api("GET", "/apis")
        assert resp.status_code == 200
        assert resp.json()["code"] == "200"

    @pytest.mark.L1
    def test_list_by_category(self, category):
        resp = api("GET", f"/apis?categoryId={category}")
        assert resp.status_code == 200
