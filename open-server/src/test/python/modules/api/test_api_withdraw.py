#!/usr/bin/env python3
"""POST /apis/{id}/withdraw — 撤回 API"""
import time
import pytest
from conftest import api


def _uid():
    return int(time.time() * 1000) % 1000000


def _create(category):
    uid = _uid()
    r = api("POST", "/apis", {
        "nameCn": f"withdraw_{uid}", "nameEn": f"withdraw_{uid}",
        "categoryId": str(category), "method": "GET",
        "path": f"/api/withdraw/{uid}",
        "permission": {"nameCn": f"p_withdraw_{uid}", "nameEn": f"p_withdraw_{uid}",
                       "scope": f"api:test:v{uid}"},
    })
    assert r.status_code == 200
    return r.json()["data"]["id"]


class TestApiWithdraw:
    @pytest.mark.L2
    def test_withdraw(self, category):
        aid = _create(category)
        resp = api("POST", f"/apis/{aid}/withdraw")
        assert resp is not None

    @pytest.mark.L4
    def test_withdraw_not_found(self):
        resp = api("POST", "/apis/999999999999999999/withdraw")
        assert resp is not None
