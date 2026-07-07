#!/usr/bin/env python3
"""PUT /callbacks/{id} — 更新回调"""
import time
import pytest
from conftest import api


def _uid():
    return int(time.time() * 1000) % 1000000


def _create(category):
    uid = _uid()
    r = api("POST", "/callbacks", {
        "nameCn": f"update_ok_{uid}", "nameEn": f"update_ok_{uid}",
        "categoryId": str(category),
        "permission": {"nameCn": f"p_update_{uid}", "nameEn": f"p_update_{uid}",
                       "scope": f"callback:test:v{uid}"},
    })
    assert r.status_code == 200
    return r.json()["data"]["id"]


class TestCallbackUpdate:
    @pytest.mark.L1
    def test_update_ok(self, category):
        cid = _create(category)
        resp = api("PUT", f"/callbacks/{cid}", {
            "nameCn": f"updated_{cid}", "nameEn": f"updated_{cid}",
        })
        assert resp.status_code == 200
