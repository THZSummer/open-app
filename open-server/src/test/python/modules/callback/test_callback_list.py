#!/usr/bin/env python3
"""GET /callbacks — 获取回调列表"""
import pytest
from conftest import api


class TestCallbackList:
    @pytest.mark.L0
    def test_list_ok(self):
        resp = api("GET", "/callbacks")
        assert resp.status_code == 200
        assert resp.json()["code"] == "200"

    @pytest.mark.L1
    def test_list_by_category(self, category):
        resp = api("GET", f"/callbacks?categoryId={category}")
        assert resp.status_code == 200
