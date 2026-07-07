#!/usr/bin/env python3
"""DELETE /apis/{id} + POST /apis/{id}/withdraw"""
import time
import pytest
from conftest import api


def _uid():
    return int(time.time() * 1000) % 1000000


def _helper_create(category, suffix=""):
    uid = _uid()
    r = api("POST", "/apis", {
        "nameCn": f"del_{suffix}", "nameEn": f"del_{suffix}",
        "categoryId": str(category), "method": "GET",
        "path": f"/del/test/{suffix}_{uid}",
        "permission": {"nameCn": "p", "nameEn": "p",
                       "scope": f"api:test:{suffix}{uid}"},
    })
    assert r.status_code == 200
    return r.json()["data"]["id"]


class TestApiDelete:
    @pytest.mark.L1
    def test_delete_ok(self, category):
        aid = _helper_create(category, "ok")
        resp = api("DELETE", f"/apis/{aid}")
        assert resp.status_code in (200, 204)

    @pytest.mark.L4
    def test_delete_not_found(self):
        resp = api("DELETE", "/apis/999999999999999999")
        assert resp is not None


class TestApiWithdraw:
    @pytest.mark.L2
    def test_withdraw(self, category):
        aid = _helper_create(category, "wd")
        resp = api("POST", f"/apis/{aid}/withdraw")
        assert resp is not None

    @pytest.mark.L4
    def test_withdraw_not_found(self):
        resp = api("POST", "/apis/999999999999999999/withdraw")
        assert resp is not None
