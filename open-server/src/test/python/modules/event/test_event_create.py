#!/usr/bin/env python3
"""POST /events — 注册事件"""
import time
import pytest
from conftest import api


def _uid():
    return int(time.time() * 1000) % 1000000


class TestEventCreate:
    @pytest.mark.L1
    def test_create_ok(self, category):
        uid = _uid()
        resp = api("POST", "/events", {
            "nameCn": "pytest_event", "nameEn": "pytest_event",
            "categoryId": str(category), "topic": f"pytest.event.test.{uid}",
            "permission": {"nameCn": "ep", "nameEn": "ep",
                           "scope": f"event:test:read{uid}"},
        })
        assert resp.status_code == 200
        data = resp.json()["data"]
        assert data.get("nameCn") == "pytest_event"
