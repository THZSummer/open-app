#!/usr/bin/env python3
"""GET /apps/{appId}/apis — 获取应用 API 权限列表"""
import pytest
from conftest import api, INTERNAL_APP_ID


class TestListAppApis:
    @pytest.mark.L1
    def test_ok(self):
        resp = api("GET", f"/apps/{INTERNAL_APP_ID}/apis")
        assert resp.status_code == 200
        assert resp.json()["code"] == "200"
