#!/usr/bin/env python3
"""GET /events — 获取事件列表"""
import pytest
from conftest import api


class TestEventList:
    @pytest.mark.L0
    def test_list_ok(self):
        resp = api("GET", "/events")
        assert resp.status_code == 200
        assert resp.json()["code"] == "200"

    @pytest.mark.L1
    def test_list_by_category(self, category):
        resp = api("GET", f"/events?categoryId={category}")
        assert resp.status_code == 200
