#!/usr/bin/env python3
"""GET /callbacks/{id} — 获取回调详情"""
import time
import pytest
from conftest import api


def _uid():
    return int(time.time() * 1000) % 1000000


def _helper_create(category):
    uid = _uid()
    r = api("POST", "/callbacks", {
        "nameCn": "detail_cb", "nameEn": "detail_cb",
        "categoryId": str(category),
        "permission": {"nameCn": "dp", "nameEn": "dp",
                       "scope": f"callback:test:detail{uid}"},
    })
    assert r.status_code == 200
    return r.json()["data"]["id"]


class TestCallbackDetail:
    @pytest.mark.L1
    def test_detail_ok(self, category):
        cid = _helper_create(category)
        resp = api("GET", f"/callbacks/{cid}")
        assert resp.status_code == 200
        data = resp.json()["data"]
        assert str(data["id"]) == str(cid)

    @pytest.mark.L4
    def test_detail_not_found(self):
        resp = api("GET", "/callbacks/999999999999999999")
        assert resp is not None
