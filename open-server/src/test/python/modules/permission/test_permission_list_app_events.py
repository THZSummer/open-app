#!/usr/bin/env python3
"""GET /apps/{appId}/events — 获取应用事件订阅列表"""
import pytest
from conftest import api, INTERNAL_APP_ID


class TestListAppEvents:
    @pytest.mark.L1
    def test_ok(self):
        resp = api("GET", f"/apps/{INTERNAL_APP_ID}/events")
        assert resp.status_code == 200
