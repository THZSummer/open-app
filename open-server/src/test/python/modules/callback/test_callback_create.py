#!/usr/bin/env python3
"""POST /callbacks — 注册回调（自动进入审批，审批通过后 status=2 已上架）"""
import time
import pytest
from conftest import api, _approve_capability_resource


def _uid():
    return int(time.time() * 1000) % 1000000


class TestCallbackCreate:
    @pytest.mark.L1
    def test_create_ok(self, category):
        uid = _uid()
        resp = api("POST", "/callbacks", {
            "nameCn": f"create_ok_{uid}", "nameEn": f"create_ok_{uid}",
            "categoryId": str(category),
            "permission": {"nameCn": f"p_create_ok_{uid}", "nameEn": f"p_create_ok_{uid}",
                           "scope": f"callback:test:v{uid}"},
        })
        assert resp.status_code == 200
        assert resp.json()["data"].get("status") in (1, "1")

    @pytest.mark.L1
    def test_create_and_publish(self, category):
        uid = _uid()
        r = api("POST", "/callbacks", {
            "nameCn": f"create_publish_{uid}", "nameEn": f"create_publish_{uid}",
            "categoryId": str(category),
            "permission": {"nameCn": f"p_publish_{uid}", "nameEn": f"p_publish_{uid}",
                           "scope": f"callback:test:v{uid}"},
        })
        assert r.status_code == 200
        cid = r.json()["data"]["id"]
        _approve_capability_resource(cid, "callback_register")
        r2 = api("GET", f"/callbacks/{cid}")
        assert r2.status_code == 200
        assert r2.json()["data"].get("status") in (2, "2")
