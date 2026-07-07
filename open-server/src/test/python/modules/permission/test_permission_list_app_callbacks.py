#!/usr/bin/env python3
"""GET /apps/{appId}/callbacks — 获取应用回调订阅列表"""
import pytest
from conftest import api, INTERNAL_APP_ID


class TestListAppCallbacks:
    @pytest.mark.L1
    def test_ok(self):
        resp = api("GET", f"/apps/{INTERNAL_APP_ID}/callbacks")
        assert resp.status_code == 200
