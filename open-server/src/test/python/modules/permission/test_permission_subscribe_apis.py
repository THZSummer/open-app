#!/usr/bin/env python3
"""POST /apps/{appId}/apis/subscribe — 订阅 API 权限"""
import time
import pytest
from conftest import api, INTERNAL_APP_ID, _approve_capability_resource


def _uid():
    return int(time.time() * 1000) % 1000000


def _create_and_publish(category):
    uid = _uid()
    r = api("POST", "/apis", {
        "nameCn": "sa", "nameEn": "sa",
        "categoryId": str(category), "method": "GET",
        "path": f"/sa/{uid}",
        "permission": {"nameCn": "s", "nameEn": "s",
                       "scope": f"api:test:sa{uid}"},
    })
    assert r.status_code == 200
    aid = r.json()["data"]["id"]
    pid = r.json()["data"]["permission"]["id"]
    _approve_capability_resource(aid, "api_register")
    return pid


class TestSubscribeApis:
    @pytest.mark.L2
    def test_rejected_for_draft(self, category):
        """未发布的 API 不允许订阅"""
        uid = _uid()
        r = api("POST", "/apis", {
            "nameCn": "dr", "nameEn": "dr",
            "categoryId": str(category), "method": "GET",
            "path": f"/dr/{uid}",
            "permission": {"nameCn": "d", "nameEn": "d",
                           "scope": f"api:test:dr{uid}"},
        })
        pid = r.json()["data"]["permission"]["id"]
        resp = api("POST", f"/apps/{INTERNAL_APP_ID}/apis/subscribe", {
            "permissionIds": [pid],
        })
        assert resp is not None
        body = resp.json()
        assert "已发布" in body.get("messageZh", "") or body.get("code") != "200"

    @pytest.mark.L2
    def test_subscribe_published(self, category):
        """已上架的 API 可成功订阅"""
        pid = _create_and_publish(category)
        resp = api("POST", f"/apps/{INTERNAL_APP_ID}/apis/subscribe", {
            "permissionIds": [pid],
        })
        assert resp.status_code in (200, 201)
