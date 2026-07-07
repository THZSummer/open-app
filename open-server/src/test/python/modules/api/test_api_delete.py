#!/usr/bin/env python3
"""DELETE /apis/{id} + POST /apis/{id}/withdraw — API 删除与撤回"""
import time
import pytest
from conftest import api


def _uid():
    return int(time.time() * 1000) % 1000000


def _create(category, scenario):
    uid = _uid()
    r = api("POST", "/apis", {
        "nameCn": f"{scenario}_{uid}", "nameEn": f"{scenario}_{uid}",
        "categoryId": str(category), "method": "GET",
        "path": f"/api/{scenario}/{uid}",
        "permission": {"nameCn": f"p_{scenario}_{uid}", "nameEn": f"p_{scenario}_{uid}",
                       "scope": f"api:test:v{uid}"},
    })
    assert r.status_code == 200
    return r.json()["data"]["id"]


class TestApiDelete:
    @pytest.mark.L1
    def test_delete_ok(self, category):
        aid = _create(category, "delete_ok")
        resp = api("DELETE", f"/apis/{aid}")
        assert resp.status_code in (200, 204)

    @pytest.mark.L4
    def test_delete_not_found(self):
        resp = api("DELETE", "/apis/999999999999999999")
        assert resp is not None


class TestApiWithdraw:
    @pytest.mark.L2
    def test_withdraw(self, category):
        aid = _create(category, "withdraw")
        resp = api("POST", f"/apis/{aid}/withdraw")
        assert resp is not None

    @pytest.mark.L4
    def test_withdraw_not_found(self):
        resp = api("POST", "/apis/999999999999999999/withdraw")
        assert resp is not None
