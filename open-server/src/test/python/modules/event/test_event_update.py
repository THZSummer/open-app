#!/usr/bin/env python3
"""PUT /events/{id} — 更新事件"""
import time
import pytest
from conftest import api


def _uid():
    return int(time.time() * 1000) % 1000000


def _helper_create(category):
    uid = _uid()
    r = api("POST", "/events", {
        "nameCn": "update_ev", "nameEn": "update_ev",
        "categoryId": str(category), "topic": f"pytest.update.event.{uid}",
        "permission": {"nameCn": "up", "nameEn": "up",
                       "scope": f"event:test:update{uid}"},
    })
    assert r.status_code == 200
    return r.json()["data"]["id"]


class TestEventUpdate:
    @pytest.mark.L1
    def test_update_ok(self, category):
        eid = _helper_create(category)
        resp = api("PUT", f"/events/{eid}", {
            "nameCn": "updated_ev", "nameEn": "updated_ev",
        })
        assert resp.status_code == 200
