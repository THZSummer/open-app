#!/usr/bin/env python3
"""PUT /callbacks/{id} — 更新回调"""
import time
import pytest
from conftest import api


def _uid():
    return int(time.time() * 1000) % 1000000


def _helper_create(category):
    uid = _uid()
    r = api("POST", "/callbacks", {
        "nameCn": "update_cb", "nameEn": "update_cb",
        "categoryId": str(category),
        "permission": {"nameCn": "up", "nameEn": "up",
                       "scope": f"callback:test:update{uid}"},
    })
    assert r.status_code == 200
    return r.json()["data"]["id"]


class TestCallbackUpdate:
    @pytest.mark.L1
    def test_update_ok(self, category):
        cid = _helper_create(category)
        resp = api("PUT", f"/callbacks/{cid}", {
            "nameCn": "updated_cb", "nameEn": "updated_cb",
        })
        assert resp.status_code == 200
