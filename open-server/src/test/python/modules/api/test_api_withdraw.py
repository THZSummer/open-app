#!/usr/bin/env python3
"""POST /apis/{id}/withdraw — 撤回审核中的 API"""
import pytest
from conftest import api


def _create_api(category):
    r = api("POST", "/apis", {
        "nameCn": "withdraw_test", "nameEn": "withdraw_test",
        "categoryId": category, "method": "GET",
        "path": "/withdraw/test",
    })
    return r.json()["data"]["id"]


class TestApiWithdraw:
    @pytest.mark.L2
    def test_withdraw(self, category):
        aid = _create_api(category)
        resp = api("POST", f"/apis/{aid}/withdraw")
        assert resp is not None

    @pytest.mark.L4
    def test_withdraw_not_found(self):
        resp = api("POST", "/apis/999999999999999999/withdraw")
        assert resp is not None
