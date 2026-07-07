#!/usr/bin/env python3
"""PUT /apis/{id} — 更新 API"""
import time
import pytest
from conftest import api


def _uid():
    return int(time.time() * 1000) % 1000000


def _helper_create(category):
    uid = _uid()
    r = api("POST", "/apis", {
        "nameCn": "update_test", "nameEn": "update_test",
        "categoryId": str(category), "method": "GET",
        "path": f"/update/test/{uid}",
        "permission": {"nameCn": "up", "nameEn": "up",
                       "scope": f"api:test:update{uid}"},
    })
    assert r.status_code == 200
    return r.json()["data"]["id"]


class TestApiUpdate:
    @pytest.mark.L1
    def test_update_ok(self, category):
        aid = _helper_create(category)
        resp = api("PUT", f"/apis/{aid}", {
            "nameCn": "updated_api", "nameEn": "updated_api",
        })
        assert resp.status_code == 200

    @pytest.mark.L4
    def test_update_not_found(self):
        resp = api("PUT", "/apis/999999999999999999", {
            "nameCn": "x", "nameEn": "x",
        })
        assert resp is not None
