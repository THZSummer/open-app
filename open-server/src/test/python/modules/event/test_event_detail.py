#!/usr/bin/env python3
"""GET /events/{id} — 获取事件详情"""
import time
import pytest
from conftest import api


def _uid():
    return int(time.time() * 1000) % 1000000


def _helper_create(category):
    uid = _uid()
    r = api("POST", "/events", {
        "nameCn": "detail_ev", "nameEn": "detail_ev",
        "categoryId": str(category), "topic": f"pytest.detail.event.{uid}",
        "permission": {"nameCn": "dp", "nameEn": "dp",
                       "scope": f"event:test:detail{uid}"},
    })
    assert r.status_code == 200
    return r.json()["data"]["id"]


class TestEventDetail:
    @pytest.mark.L1
    def test_detail_ok(self, category):
        eid = _helper_create(category)
        resp = api("GET", f"/events/{eid}")
        assert resp.status_code == 200
        data = resp.json()["data"]
        assert str(data["id"]) == str(eid)

    @pytest.mark.L4
    def test_detail_not_found(self):
        resp = api("GET", "/events/999999999999999999")
        assert resp is not None
