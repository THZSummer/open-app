#!/usr/bin/env python3
"""POST /apps/{appId}/callbacks/subscribe — 订阅回调权限

前端抽屉: GET /categories?categoryAlias=callback
完整链路: 分类可见→上架→出现在目录→订阅→审批订阅→已授权
"""
import time
import pytest
from conftest import api, INTERNAL_APP_ID, _approve_capability_resource, _approve_subscription


def _uid():
    return int(time.time() * 1000) % 1000000


def _create_category():
    uid = _uid()
    r = api("POST", "/categories", {
        "nameCn": f"cat_cb_sub_{uid}", "nameEn": f"cat_cb_sub_{uid}",
        "categoryAlias": "callback",
    })
    assert r.status_code == 200
    return r.json()["data"]["id"]


def _create_and_publish(category_id, scenario):
    uid = _uid()
    r = api("POST", "/callbacks", {
        "nameCn": f"{scenario}_{uid}", "nameEn": f"{scenario}_{uid}",
        "categoryId": str(category_id),
        "permission": {"nameCn": f"p_{scenario}_{uid}", "nameEn": f"p_{scenario}_{uid}",
                       "scope": f"callback:test:v{uid}"},
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
            "nameCn": f"draft_reject_{uid}", "nameEn": f"draft_reject_{uid}",
            "categoryId": str(category),
            "permission": {"nameCn": f"p_draft_reject_{uid}", "nameEn": f"p_draft_reject_{uid}",
                           "scope": f"callback:test:v{uid}"},
        })
        pid = r.json()["data"]["permission"]["id"]
        resp = api("POST", f"/apps/{INTERNAL_APP_ID}/callbacks/subscribe", {"permissionIds": [pid]})
        assert resp is not None
        body = resp.json()
        assert "已发布" in body.get("messageZh", "") or body.get("code") != "200"

    @pytest.mark.L2
    def test_subscribe_and_approve(self):
        cid = _create_category()
        pid = _create_and_publish(cid, "sub_approve")

        r0 = api("GET", f"/categories/{cid}/callbacks")
        assert len(r0.json().get("data", [])) > 0, "已上架的回调应出现在分类订阅目录中"

        resp = api("POST", f"/apps/{INTERNAL_APP_ID}/callbacks/subscribe", {"permissionIds": [pid]})
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
        assert len(resp.json().get("data", [])) > 0, "前端应能通过别名 callback 找到分类"
