#!/usr/bin/env python3
"""POST /apps/{appId}/{apis,events,callbacks}/subscribe — 批量订阅"""
import time
import pytest
from conftest import api, INTERNAL_APP_ID


def _uid():
    return int(time.time() * 1000) % 1000000


def _create_api(category):
    uid = _uid()
    r = api("POST", "/apis", {
        "nameCn": "sub_a", "nameEn": "sub_a",
        "categoryId": str(category), "method": "GET",
        "path": f"/sub/a/{uid}",
        "permission": {"nameCn": "s", "nameEn": "s",
                       "scope": f"api:test:subapi{uid}"},
    })
    assert r.status_code == 200
    d = r.json()["data"]
    return d["id"], d["permission"]["id"]


def _create_event(category):
    uid = _uid()
    r = api("POST", "/events", {
        "nameCn": "sub_e", "nameEn": "sub_e",
        "categoryId": str(category), "topic": f"pytest.sub.event.{uid}",
        "permission": {"nameCn": "s", "nameEn": "s",
                       "scope": f"event:test:subev{uid}"},
    })
    assert r.status_code == 200
    d = r.json()["data"]
    return d["id"], d["permission"]["id"]


def _create_callback(category):
    uid = _uid()
    r = api("POST", "/callbacks", {
        "nameCn": "sub_c", "nameEn": "sub_c",
        "categoryId": str(category),
        "permission": {"nameCn": "s", "nameEn": "s",
                       "scope": f"callback:test:subcb{uid}"},
    })
    assert r.status_code == 200
    d = r.json()["data"]
    return d["id"], d["permission"]["id"]


class TestSubscribe:
    @pytest.mark.L2
    def test_subscribe_apis_rejected_draft(self, category):
        """未发布的 API 不允许订阅"""
        aid, pid = _create_api(category)
        resp = api("POST", f"/apps/{INTERNAL_APP_ID}/apis/subscribe", {
            "permissionIds": [pid],
        })
        assert resp is not None
        body = resp.json()
        assert "已发布" in body.get("messageZh", "") or body.get("code") != "200"

    @pytest.mark.L2
    def test_subscribe_events_rejected_draft(self, category):
        """未发布的事件不允许订阅"""
        eid, pid = _create_event(category)
        resp = api("POST", f"/apps/{INTERNAL_APP_ID}/events/subscribe", {
            "permissionIds": [pid],
        })
        assert resp is not None
        body = resp.json()
        assert "已发布" in body.get("messageZh", "") or body.get("code") != "200"

    @pytest.mark.L2
    def test_subscribe_callbacks_rejected_draft(self, category):
        """未发布的回调不允许订阅"""
        cid, pid = _create_callback(category)
        resp = api("POST", f"/apps/{INTERNAL_APP_ID}/callbacks/subscribe", {
            "permissionIds": [pid],
        })
        assert resp is not None
        body = resp.json()
        assert "已发布" in body.get("messageZh", "") or body.get("code") != "200"

    @pytest.mark.L4
    def test_subscribe_empty_permission_ids(self):
        resp = api("POST", f"/apps/{INTERNAL_APP_ID}/apis/subscribe", {
            "permissionIds": [],
        })
        assert resp is not None
