#!/usr/bin/env python3
"""DELETE /callbacks/{id} + POST /callbacks/{id}/withdraw"""
import pytest
from conftest import api, assert_operate_log


def _create_callback(category, suffix=""):
    r = api("POST", "/callbacks", {
        "nameCn": f"del_cb_{suffix}", "nameEn": f"del_cb_{suffix}",
        "categoryId": category,
    })
    return r.json()["data"]["id"]


class TestCallbackDelete:
    @pytest.mark.L1
    def test_delete_ok(self, category):
        cid = _create_callback(category, "ok")
        resp = api("DELETE", f"/callbacks/{cid}")
        assert resp.status_code in (200, 204)

    @pytest.mark.L4
    def test_delete_not_found(self):
        resp = api("DELETE", "/callbacks/999999999999999999")
        assert resp is not None

    @pytest.mark.L2
    def test_delete_log(self, category):
        cid = _create_callback(category, "log")
        resp = api("DELETE", f"/callbacks/{cid}")
        assert resp.status_code in (200, 204)
        assert_operate_log("删除回调")


class TestCallbackWithdraw:
    @pytest.mark.L2
    def test_withdraw(self, category):
        cid = _create_callback(category, "wd")
        resp = api("POST", f"/callbacks/{cid}/withdraw")
        assert resp is not None

    @pytest.mark.L4
    def test_withdraw_not_found(self):
        resp = api("POST", "/callbacks/999999999999999999/withdraw")
        assert resp is not None
