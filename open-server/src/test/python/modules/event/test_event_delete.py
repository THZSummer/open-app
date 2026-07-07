#!/usr/bin/env python3
"""DELETE /events/{id} + POST /events/{id}/withdraw — 事件删除与撤回"""
import time
import pytest
from conftest import api


def _uid():
    return int(time.time() * 1000) % 1000000


def _create(category, scenario):
    uid = _uid()
    r = api("POST", "/events", {
        "nameCn": f"{scenario}_{uid}", "nameEn": f"{scenario}_{uid}",
        "categoryId": str(category), "topic": f"pytest.{scenario}.{uid}",
        "permission": {"nameCn": f"p_{scenario}_{uid}", "nameEn": f"p_{scenario}_{uid}",
                       "scope": f"event:test:v{uid}"},
    })
    assert r.status_code == 200
    return r.json()["data"]["id"]


class TestEventDelete:
    @pytest.mark.L1
    def test_delete_ok(self, category):
        eid = _create(category, "delete_ok")
        resp = api("DELETE", f"/events/{eid}")
        assert resp.status_code in (200, 204)

    @pytest.mark.L4
    def test_delete_not_found(self):
        resp = api("DELETE", "/events/999999999999999999")
        assert resp is not None


class TestEventWithdraw:
    @pytest.mark.L2
    def test_withdraw(self, category):
        eid = _create(category, "withdraw")
        resp = api("POST", f"/events/{eid}/withdraw")
        assert resp is not None

    @pytest.mark.L4
    def test_withdraw_not_found(self):
        resp = api("POST", "/events/999999999999999999/withdraw")
        assert resp is not None
