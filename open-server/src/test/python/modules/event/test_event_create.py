#!/usr/bin/env python3
"""POST /events — 注册事件（注册后自动进入审批，审批通过后 status=2 已上架）"""
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
            "nameCn": "pytest_event", "nameEn": "pytest_event",
            "categoryId": str(category), "topic": f"pytest.event.test.{uid}",
            "permission": {"nameCn": "ep", "nameEn": "ep",
                           "scope": f"event:test:read{uid}"},
        })
        assert resp.status_code == 200
        data = resp.json()["data"]
        assert data.get("nameCn") == "pytest_event"

    @pytest.mark.L1
    def test_create_and_publish(self, category):
        """注册 → 审批通过 → status=2 已上架"""
        uid = _uid()
        r = api("POST", "/events", {
            "nameCn": "pub_ev", "nameEn": "pub_ev",
            "categoryId": str(category), "topic": f"pytest.pub.ev.{uid}",
            "permission": {"nameCn": "p", "nameEn": "p",
                           "scope": f"event:test:pub{uid}"},
        })
        assert r.status_code == 200
        eid = r.json()["data"]["id"]

        _approve_capability_resource(eid, "event_register")

        r2 = api("GET", f"/events/{eid}")
        assert r2.status_code == 200
        assert r2.json()["data"].get("status") in (2, "2")
