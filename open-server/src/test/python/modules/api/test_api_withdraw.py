#!/usr/bin/env python3
"""DELETE /apis/{id} + POST /apis/{id}/withdraw"""
import time
import pytest
from conftest import api


def _uid():
    return int(time.time() * 1000) % 1000000


def _helper_create(category):
    uid = _uid()
    r = api("POST", "/apis", {
        "nameCn": "wd_test", "nameEn": "wd_test",
        "categoryId": str(category), "method": "GET",
        "path": f"/wd/test/{uid}",
        "permission": {"nameCn": "wp", "nameEn": "wp",
                       "scope": f"api:test:wd{uid}"},
    })
    assert r.status_code == 200
    return r.json()["data"]["id"]


class TestApiWithdraw:
    @pytest.mark.L2
    def test_withdraw(self, category):
        aid = _helper_create(category)
        resp = api("POST", f"/apis/{aid}/withdraw")
        assert resp is not None

    @pytest.mark.L4
    def test_withdraw_not_found(self):
        resp = api("POST", "/apis/999999999999999999/withdraw")
        assert resp is not None
