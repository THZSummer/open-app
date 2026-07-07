#!/usr/bin/env python3
"""POST /apps/{appId}/callbacks/subscribe — 订阅回调权限

前端抽屉调用 GET /categories?categoryAlias=callback 查找分类。
完整链路: 分类可见→回调上架→出现在目录→订阅→审批→已授权。
"""
import time
import pytest
from conftest import api, INTERNAL_APP_ID, _approve_capability_resource, _approve_subscription


def _uid():
    return int(time.time() * 1000) % 1000000


def _create_category():
    uid = _uid()
    r = api("POST", "/categories", {
        "nameCn": f"cat_cb_{uid}", "nameEn": f"cat_cb_{uid}",
        "categoryAlias": "callback",
    })
    assert r.status_code == 200
    return r.json()["data"]["id"]


def _create_and_publish(category_id):
    uid = _uid()
    r = api("POST", "/callbacks", {
        "nameCn": "sc", "nameEn": "sc",
        "categoryId": str(category_id),
        "permission": {"nameCn": "s", "nameEn": "s",
                       "scope": f"callback:test:sc{uid}"},
    })
    assert r.status_code == 200
    cid = r.json()["data"]["id"]
    pid = r.json()["data"]["permission"]["id"]
    _approve_capability_resource(cid, "callback_register")
    return pid


class TestSubscribeCallbacks:
    @pytest.mark.L2
    def test_rejected_for_draft(self, category):
        uid = _uid()
        r = api("POST", "/callbacks", {
            "nameCn": "dr", "nameEn": "dr",
            "categoryId": str(category),
            "permission": {"nameCn": "d", "nameEn": "d",
                           "scope": f"callback:test:dr{uid}"},
        })
        pid = r.json()["data"]["permission"]["id"]
        resp = api("POST", f"/apps/{INTERNAL_APP_ID}/callbacks/subscribe", {
            "permissionIds": [pid],
        })
        assert resp is not None
        body = resp.json()
        assert "已发布" in body.get("messageZh", "") or body.get("code") != "200"

    @pytest.mark.L2
    def test_subscribe_and_approve(self):
        """完整链路: 分类可见→回调上架→出现在目录→订阅→审批→已授权"""
        cid = _create_category()
        pid = _create_and_publish(cid)

        r0 = api("GET", f"/categories/{cid}/callbacks")
        cat_items = r0.json().get("data", [])
        assert len(cat_items) > 0, "已上架的回调应出现在分类订阅目录中"

        resp = api("POST", f"/apps/{INTERNAL_APP_ID}/callbacks/subscribe", {
            "permissionIds": [pid],
        })
        assert resp.status_code in (200, 201)
        sub_id = resp.json()["data"]["records"][0]["id"]

        _approve_subscription(sub_id, "callback_permission_apply")

        r2 = api("GET", f"/apps/{INTERNAL_APP_ID}/callbacks")
        items = r2.json().get("data", [])
        matching = [it for it in items if str(it.get("id")) == str(sub_id)]
        assert len(matching) > 0, "已授权的订阅应出现在应用列表中"

    @pytest.mark.L1
    def test_category_discoverable_by_frontend(self):
        cid = _create_category()
        resp = api("GET", "/categories?categoryAlias=callback")
        items = resp.json().get("data", [])
        assert len(items) > 0, "前端应能通过别名 callback 找到分类"
