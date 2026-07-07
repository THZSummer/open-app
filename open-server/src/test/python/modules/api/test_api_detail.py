#!/usr/bin/env python3
"""GET /apis/{id} — 获取 API 详情"""
import time
import pytest
from conftest import api


def _uid():
    return int(time.time() * 1000) % 1000000


def _create(category):
    uid = _uid()
    r = api("POST", "/apis", {
        "nameCn": f"detail_ok_{uid}", "nameEn": f"detail_ok_{uid}",
        "categoryId": str(category), "method": "GET",
        "path": f"/api/detail/{uid}",
        "permission": {"nameCn": f"p_detail_{uid}", "nameEn": f"p_detail_{uid}",
                       "scope": f"api:test:v{uid}"},
    })
    assert r.status_code == 200
    return r.json()["data"]["id"]


class TestApiDetail:
    @pytest.mark.L1
    def test_detail_ok(self, category):
        aid = _create(category)
        resp = api("GET", f"/apis/{aid}")
        assert resp.status_code == 200
        assert str(resp.json()["data"]["id"]) == str(aid)

    @pytest.mark.L4
    def test_detail_not_found(self):
        resp = api("GET", "/apis/999999999999999999")
        assert resp is not None
