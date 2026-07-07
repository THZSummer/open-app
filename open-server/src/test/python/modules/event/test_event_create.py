#!/usr/bin/env python3
"""POST /events — 注册事件（自动进入审批，审批通过后 status=2 已上架）"""
import time
import pytest
from conftest import api, _approve_capability_resource


def _uid():
    return int(time.time() * 1000) % 1000000


class TestEventCreate:
    @pytest.mark.L1
    def test_create_ok(self, category):
        uid = _uid()
        resp = api("POST", "/events", {
            "nameCn": f"create_ok_{uid}", "nameEn": f"create_ok_{uid}",
            "categoryId": str(category), "topic": f"pytest.create_ok.{uid}",
            "permission": {"nameCn": f"p_create_ok_{uid}", "nameEn": f"p_create_ok_{uid}",
                           "scope": f"event:test:v{uid}"},
        })
        assert resp.status_code == 200
        assert resp.json()["data"].get("status") in (1, "1")

    @pytest.mark.L1
    def test_create_and_publish(self, category):
        uid = _uid()
        r = api("POST", "/events", {
            "nameCn": f"create_publish_{uid}", "nameEn": f"create_publish_{uid}",
            "categoryId": str(category), "topic": f"pytest.publish.{uid}",
            "permission": {"nameCn": f"p_publish_{uid}", "nameEn": f"p_publish_{uid}",
                           "scope": f"event:test:v{uid}"},
        })
        assert r.status_code == 200
        eid = r.json()["data"]["id"]
        _approve_capability_resource(eid, "event_register")
        r2 = api("GET", f"/events/{eid}")
        assert r2.status_code == 200
        assert r2.json()["data"].get("status") in (2, "2")
