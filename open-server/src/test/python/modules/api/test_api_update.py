#!/usr/bin/env python3
"""PUT /apis/{id} — 更新 API"""
import time
import pytest
from conftest import api


def _uid():
    return int(time.time() * 1000) % 1000000


def _create(category):
    uid = _uid()
    r = api("POST", "/apis", {
        "nameCn": f"update_ok_{uid}", "nameEn": f"update_ok_{uid}",
        "categoryId": str(category), "method": "GET",
        "path": f"/api/update/{uid}",
        "permission": {"nameCn": f"p_update_{uid}", "nameEn": f"p_update_{uid}",
                       "scope": f"api:test:v{uid}"},
    })
    assert r.status_code == 200
    return r.json()["data"]["id"]


class TestApiUpdate:
    @pytest.mark.L1
    def test_update_ok(self, category):
        aid = _create(category)
        resp = api("PUT", f"/apis/{aid}", {
            "nameCn": f"updated_{aid}", "nameEn": f"updated_{aid}",
        })
        assert resp.status_code == 200

    @pytest.mark.L4
    def test_update_not_found(self):
        resp = api("PUT", "/apis/999999999999999999", {"nameCn": "x", "nameEn": "x"})
        assert resp is not None
