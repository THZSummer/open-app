#!/usr/bin/env python3
"""GET /callbacks/{id} — 获取回调详情"""
import pytest
from conftest import api


def _create_callback(category):
    r = api("POST", "/callbacks", {
        "nameCn": "detail_cb", "nameEn": "detail_cb",
        "categoryId": category,
    })
    return r.json()["data"]["id"]


class TestCallbackDetail:
    @pytest.mark.L1
    def test_detail_ok(self, category):
        cid = _create_callback(category)
        resp = api("GET", f"/callbacks/{cid}")
        assert resp.status_code == 200
        assert int(resp.json()["data"]["id"]) == cid

    @pytest.mark.L4
    def test_detail_not_found(self):
        resp = api("GET", "/callbacks/999999999999999999")
        assert resp is not None
