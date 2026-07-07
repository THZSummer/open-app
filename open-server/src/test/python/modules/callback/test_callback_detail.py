#!/usr/bin/env python3
"""GET /callbacks/{id} — 获取回调详情"""
import time
import pytest
from conftest import api


def _uid():
    return int(time.time() * 1000) % 1000000


def _create(category):
    uid = _uid()
    r = api("POST", "/callbacks", {
        "nameCn": f"detail_ok_{uid}", "nameEn": f"detail_ok_{uid}",
        "categoryId": str(category),
        "permission": {"nameCn": f"p_detail_{uid}", "nameEn": f"p_detail_{uid}",
                       "scope": f"callback:test:v{uid}"},
    })
    assert r.status_code == 200
    return r.json()["data"]["id"]


class TestCallbackDetail:
    @pytest.mark.L1
    def test_detail_ok(self, category):
        cid = _create(category)
        resp = api("GET", f"/callbacks/{cid}")
        assert resp.status_code == 200
        assert str(resp.json()["data"]["id"]) == str(cid)

    @pytest.mark.L4
    def test_detail_not_found(self):
        resp = api("GET", "/callbacks/999999999999999999")
        assert resp is not None
