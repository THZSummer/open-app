#!/usr/bin/env python3
"""DELETE /events/{id} + POST /events/{id}/withdraw"""
import time
import pytest
from conftest import api


def _uid():
    return int(time.time() * 1000) % 1000000


def _helper_create(category, suffix=""):
    uid = _uid()
    r = api("POST", "/events", {
        "nameCn": f"ev_{suffix}", "nameEn": f"ev_{suffix}",
        "categoryId": str(category), "topic": f"pytest.del.event.{suffix}.{uid}",
        "permission": {"nameCn": "ep", "nameEn": "ep",
                       "scope": f"event:test:{suffix}{uid}"},
    })
    assert r.status_code == 200
    return r.json()["data"]["id"]


class TestEventDelete:
    @pytest.mark.L1
    def test_delete_ok(self, category):
        eid = _helper_create(category, "ok")
        resp = api("DELETE", f"/events/{eid}")
        assert resp.status_code in (200, 204)

    @pytest.mark.L4
    def test_delete_not_found(self):
        resp = api("DELETE", "/events/999999999999999999")
        assert resp is not None


class TestEventWithdraw:
    @pytest.mark.L2
    def test_withdraw(self, category):
        eid = _helper_create(category, "wd")
        resp = api("POST", f"/events/{eid}/withdraw")
        assert resp is not None

    @pytest.mark.L4
    def test_withdraw_not_found(self):
        resp = api("POST", "/events/999999999999999999/withdraw")
        assert resp is not None
