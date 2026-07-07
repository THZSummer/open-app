#!/usr/bin/env python3
"""POST /events — 注册事件"""
import pytest
from conftest import api


class TestEventCreate:
    @pytest.mark.L1
    def test_create_ok(self, category):
        resp = api("POST", "/events", {
            "nameCn": "pytest_event", "nameEn": "pytest_event",
            "categoryId": category, "topic": "pytest.event.test",
        })
        assert resp.status_code in (200, 201)
        data = resp.json()["data"]
        assert data.get("nameCn") == "pytest_event"
