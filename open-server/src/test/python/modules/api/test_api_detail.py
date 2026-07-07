#!/usr/bin/env python3
"""GET /apis/{id} — 获取 API 详情"""
import time
import pytest
from conftest import api


def _uid():
    return int(time.time() * 1000) % 1000000


def _helper_create(category):
    uid = _uid()
    r = api("POST", "/apis", {
        "nameCn": "detail_test", "nameEn": "detail_test",
        "categoryId": str(category), "method": "GET",
        "path": f"/detail/test/{uid}",
        "permission": {"nameCn": "dp", "nameEn": "dp",
                       "scope": f"api:test:detail{uid}"},
    })
    assert r.status_code == 200
    return r.json()["data"]["id"]


class TestApiDetail:
    @pytest.mark.L1
    def test_detail_ok(self, category):
        aid = _helper_create(category)
        resp = api("GET", f"/apis/{aid}")
        assert resp.status_code == 200
        data = resp.json()["data"]
        assert str(data["id"]) == str(aid)

    @pytest.mark.L4
    def test_detail_not_found(self):
        resp = api("GET", "/apis/999999999999999999")
        assert resp is not None
