#!/usr/bin/env python3
"""DELETE /events/{id} + POST /events/{id}/withdraw"""
import pytest
from conftest import api, assert_operate_log


def _create_event(category, suffix=""):
    r = api("POST", "/events", {
        "nameCn": f"del_ev_{suffix}", "nameEn": f"del_ev_{suffix}",
        "categoryId": category, "topic": f"pytest.del.event.{suffix}",
    })
    return r.json()["data"]["id"]


class TestEventDelete:
    @pytest.mark.L1
    def test_delete_ok(self, category):
        eid = _create_event(category, "ok")
        resp = api("DELETE", f"/events/{eid}")
        assert resp.status_code in (200, 204)

    @pytest.mark.L4
    def test_delete_not_found(self):
        resp = api("DELETE", "/events/999999999999999999")
        assert resp is not None

    @pytest.mark.L2
    def test_delete_log(self, category):
        eid = _create_event(category, "log")
        resp = api("DELETE", f"/events/{eid}")
        assert resp.status_code in (200, 204)
        assert_operate_log("删除事件")


class TestEventWithdraw:
    @pytest.mark.L2
    def test_withdraw(self, category):
        eid = _create_event(category, "wd")
        resp = api("POST", f"/events/{eid}/withdraw")
        assert resp is not None

    @pytest.mark.L4
    def test_withdraw_not_found(self):
        resp = api("POST", "/events/999999999999999999/withdraw")
        assert resp is not None
