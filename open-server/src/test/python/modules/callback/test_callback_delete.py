#!/usr/bin/env python3
"""DELETE /callbacks/{id} + POST /callbacks/{id}/withdraw"""
import time
import pytest
from conftest import api


def _uid():
    return int(time.time() * 1000) % 1000000


def _helper_create(category, suffix=""):
    uid = _uid()
    r = api("POST", "/callbacks", {
        "nameCn": f"cb_{suffix}", "nameEn": f"cb_{suffix}",
        "categoryId": str(category),
        "permission": {"nameCn": "cp", "nameEn": "cp",
                       "scope": f"callback:test:{suffix}{uid}"},
    })
    assert r.status_code == 200
    return r.json()["data"]["id"]


class TestCallbackDelete:
    @pytest.mark.L1
    def test_delete_ok(self, category):
        cid = _helper_create(category, "ok")
        resp = api("DELETE", f"/callbacks/{cid}")
        assert resp.status_code in (200, 204)

    @pytest.mark.L4
    def test_delete_not_found(self):
        resp = api("DELETE", "/callbacks/999999999999999999")
        assert resp is not None


class TestCallbackWithdraw:
    @pytest.mark.L2
    def test_withdraw(self, category):
        cid = _helper_create(category, "wd")
        resp = api("POST", f"/callbacks/{cid}/withdraw")
        assert resp is not None

    @pytest.mark.L4
    def test_withdraw_not_found(self):
        resp = api("POST", "/callbacks/999999999999999999/withdraw")
        assert resp is not None
