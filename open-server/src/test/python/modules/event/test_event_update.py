#!/usr/bin/env python3
"""PUT /events/{id} — 更新事件"""
import time
import pytest
from conftest import api


def _uid():
    return int(time.time() * 1000) % 1000000


def _create(category):
    uid = _uid()
    r = api("POST", "/events", {
        "nameCn": f"update_ok_{uid}", "nameEn": f"update_ok_{uid}",
        "categoryId": str(category), "topic": f"pytest.update.{uid}",
        "permission": {"nameCn": f"p_update_{uid}", "nameEn": f"p_update_{uid}",
                       "scope": f"event:test:v{uid}"},
    })
    assert r.status_code == 200
    return r.json()["data"]["id"]


class TestEventUpdate:
    @pytest.mark.L1
    def test_update_ok(self, category):
        eid = _create(category)
        resp = api("PUT", f"/events/{eid}", {
            "nameCn": f"updated_{eid}", "nameEn": f"updated_{eid}",
        })
        assert resp.status_code == 200
