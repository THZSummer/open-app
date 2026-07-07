#!/usr/bin/env python3
"""POST /apps/{appId}/events/subscribe — 订阅事件权限

前端抽屉调用 GET /categories?categoryAlias=event 查找分类。
完整链路: 分类可见→事件上架→出现在目录→订阅→审批→已授权。
"""
import time
import pytest
from conftest import api, INTERNAL_APP_ID, _approve_capability_resource, _approve_subscription


def _uid():
    return int(time.time() * 1000) % 1000000


def _create_category():
    uid = _uid()
    r = api("POST", "/categories", {
        "nameCn": f"cat_ev_{uid}", "nameEn": f"cat_ev_{uid}",
        "categoryAlias": "event",
    })
    assert r.status_code == 200
    return r.json()["data"]["id"]


def _create_and_publish(category_id):
    uid = _uid()
    r = api("POST", "/events", {
        "nameCn": "se", "nameEn": "se",
        "categoryId": str(category_id), "topic": f"pytest.se.{uid}",
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
    def test_subscribe_and_approve(self):
        """完整链路: 分类可见→事件上架→出现在目录→订阅→审批→已授权"""
        cid = _create_category()
        pid = _create_and_publish(cid)

        r0 = api("GET", f"/categories/{cid}/events")
        cat_items = r0.json().get("data", [])
        assert len(cat_items) > 0, "已上架的事件应出现在分类订阅目录中"

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

    @pytest.mark.L1
    def test_category_discoverable_by_frontend(self):
        cid = _create_category()
        resp = api("GET", "/categories?categoryAlias=event")
        items = resp.json().get("data", [])
        assert len(items) > 0, "前端应能通过别名 event 找到分类"
