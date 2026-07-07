#!/usr/bin/env python3
"""PUT /events/{id} — 更新事件"""
import pytest
from conftest import api, assert_operate_log


def _create_event(category):
    r = api("POST", "/events", {
        "nameCn": "update_ev", "nameEn": "update_ev",
        "categoryId": category, "topic": "pytest.update.event",
    })
    return r.json()["data"]["id"]


class TestEventUpdate:
    @pytest.mark.L1
    def test_update_ok(self, category):
        eid = _create_event(category)
        resp = api("PUT", f"/events/{eid}", {
            "nameCn": "updated_ev", "nameEn": "updated_ev",
        })
        assert resp.status_code == 200

    @pytest.mark.L1
    def test_update_log(self, category):
        eid = _create_event(category)
        resp = api("PUT", f"/events/{eid}", {
            "nameCn": "logged_ev", "nameEn": "logged_ev",
        })
        assert resp.status_code == 200
        assert_operate_log("更新事件")
