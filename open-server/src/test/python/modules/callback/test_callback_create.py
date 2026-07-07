#!/usr/bin/env python3
"""POST /callbacks — 注册回调（注册后自动进入审批，审批通过后 status=2 已上架）"""
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
            "nameCn": "pytest_cb", "nameEn": "pytest_cb",
            "categoryId": str(category),
            "permission": {"nameCn": "cp", "nameEn": "cp",
                           "scope": f"callback:test:read{uid}"},
        })
        assert resp.status_code == 200
        data = resp.json()["data"]
        assert data.get("nameCn") == "pytest_cb"

    @pytest.mark.L1
    def test_create_and_publish(self, category):
        """注册 → 审批通过 → status=2 已上架"""
        uid = _uid()
        r = api("POST", "/callbacks", {
            "nameCn": "pub_cb", "nameEn": "pub_cb",
            "categoryId": str(category),
            "permission": {"nameCn": "p", "nameEn": "p",
                           "scope": f"callback:test:pub{uid}"},
        })
        assert r.status_code == 200
        cid = r.json()["data"]["id"]

        _approve_capability_resource(cid, "callback_register")

        r2 = api("GET", f"/callbacks/{cid}")
        assert r2.status_code == 200
        assert r2.json()["data"].get("status") in (2, "2")
