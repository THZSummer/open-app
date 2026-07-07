#!/usr/bin/env python3
"""GET /events/{id} — 获取事件详情"""
import pytest
from conftest import api


def _create_event(category):
    r = api("POST", "/events", {
        "nameCn": "detail_ev", "nameEn": "detail_ev",
        "categoryId": category, "topic": "pytest.detail.event",
    })
    return r.json()["data"]["id"]


class TestEventDetail:
    @pytest.mark.L1
    def test_detail_ok(self, category):
        eid = _create_event(category)
        resp = api("GET", f"/events/{eid}")
        assert resp.status_code == 200
        assert int(resp.json()["data"]["id"]) == eid

    @pytest.mark.L4
    def test_detail_not_found(self):
        resp = api("GET", "/events/999999999999999999")
        assert resp is not None
