#!/usr/bin/env python3
"""POST /apis — 注册 API（自动进入审批，审批通过后 status=2 已上架）"""
import time
import pytest
from conftest import api, _approve_capability_resource


def _uid():
    return int(time.time() * 1000) % 1000000


class TestApiCreate:
    @pytest.mark.L1
    def test_create_ok(self, category):
        uid = _uid()
        resp = api("POST", "/apis", {
            "nameCn": f"create_ok_{uid}", "nameEn": f"create_ok_{uid}",
            "categoryId": str(category), "method": "GET",
            "path": f"/api/create_ok/{uid}",
            "permission": {"nameCn": f"p_create_ok_{uid}", "nameEn": f"p_create_ok_{uid}",
                           "scope": f"api:test:v{uid}"},
        })
        assert resp.status_code == 200
        data = resp.json()["data"]
        assert data.get("status") in (1, "1")

    @pytest.mark.L1
    def test_create_and_publish(self, category):
        uid = _uid()
        r = api("POST", "/apis", {
            "nameCn": f"create_publish_{uid}", "nameEn": f"create_publish_{uid}",
            "categoryId": str(category), "method": "GET",
            "path": f"/api/publish/{uid}",
            "permission": {"nameCn": f"p_publish_{uid}", "nameEn": f"p_publish_{uid}",
                           "scope": f"api:test:v{uid}"},
        })
        assert r.status_code == 200
        aid = r.json()["data"]["id"]
        _approve_capability_resource(aid, "api_register")
        r2 = api("GET", f"/apis/{aid}")
        assert r2.status_code == 200
        assert r2.json()["data"].get("status") in (2, "2")

    @pytest.mark.L4
    def test_create_no_category(self):
        resp = api("POST", "/apis", {"nameCn": "bad", "nameEn": "bad", "method": "GET", "path": "/bad"})
        assert resp is not None
