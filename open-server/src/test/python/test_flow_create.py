#!/usr/bin/env python3
"""#17 POST /flows — 创建连接流"""
import pytest
from _client import api


class TestFlowCreate:
    @pytest.mark.L1
    def test_create_ok(self):
        resp = api("POST", "/flows", {"nameCn": "新消息自动通知", "nameEn": "Auto Message Notification"})
        assert resp.status_code == 200
        assert resp.json()["code"] == "200"

    @pytest.mark.L4
    def test_missing_name_cn(self):
        resp = api("POST", "/flows", {"nameEn": "Test"})
        assert resp.status_code == 400

    @pytest.mark.L4
    def test_missing_name_en(self):
        resp = api("POST", "/flows", {"nameCn": "测试"})
        assert resp.status_code == 400
