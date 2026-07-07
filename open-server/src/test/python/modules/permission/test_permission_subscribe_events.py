#!/usr/bin/env python3
"""POST /apps/{appId}/events/subscribe — 订阅事件权限"""
import time
import pytest
from conftest import api, INTERNAL_APP_ID, _approve_capability_resource, _approve_subscription


def _uid():
    return int(time.time() * 1000) % 1000000


def _create_and_publish(category):
    uid = _uid()
    r = api("POST", "/events", {
        "nameCn": "se", "nameEn": "se",
        "categoryId": str(category), "topic": f"pytest.se.{uid}",
        "permission": {"nameCn": "s", "nameEn": "s",
                       "scope": f"event:test:se{uid}"},
    })
    assert r.status_code == 200
    eid = r.json()["data"]["id"]
    pid = r.json()["data"]["permission"]["id"]
    _approve_capability_resource(eid, "event_register")
    return pid


class TestSubscribeEvents:
    @pytest.mark.L2
    def test_rejected_for_draft(self, category):
        uid = _uid()
        r = api("POST", "/events", {
            "nameCn": "dr", "nameEn": "dr",
            "categoryId": str(category), "topic": f"pytest.dr.{uid}",
            "permission": {"nameCn": "d", "nameEn": "d",
                           "scope": f"event:test:dr{uid}"},
        })
        pid = r.json()["data"]["permission"]["id"]
        resp = api("POST", f"/apps/{INTERNAL_APP_ID}/events/subscribe", {
            "permissionIds": [pid],
        })
        assert resp is not None
        body = resp.json()
        assert "已发布" in body.get("messageZh", "") or body.get("code") != "200"

    @pytest.mark.L2
    def test_subscribe_and_approve(self, category):
        """注册→审批→订阅→审批订阅→status=1 已授权"""
        pid = _create_and_publish(category)

        resp = api("POST", f"/apps/{INTERNAL_APP_ID}/events/subscribe", {
            "permissionIds": [pid],
        })
        assert resp.status_code in (200, 201)
        sub_id = resp.json()["data"]["records"][0]["id"]

        _approve_subscription(sub_id, "event_permission_apply")

        r2 = api("GET", f"/apps/{INTERNAL_APP_ID}/events")
        items = r2.json().get("data", [])
        matching = [it for it in items if str(it.get("id")) == str(sub_id)]
        assert len(matching) > 0, "已授权的订阅应出现在应用列表中"
