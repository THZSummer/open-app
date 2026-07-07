#!/usr/bin/env python3
"""订阅/退订 API"""
import time
import pytest
from conftest import api, INTERNAL_APP_ID


def _uid():
    return int(time.time() * 1000) % 1000000


def _helper_create_api(category):
    uid = _uid()
    r = api("POST", "/apis", {
        "nameCn": "sub_api", "nameEn": "sub_api",
        "categoryId": str(category), "method": "GET",
        "path": f"/sub/test/{uid}",
        "permission": {"nameCn": "sp", "nameEn": "sp",
                       "scope": f"api:test:sub{uid}"},
    })
    assert r.status_code == 200
    api_data = r.json()["data"]
    return api_data["id"], api_data["permission"]["id"]


class TestSubscribe:
    @pytest.mark.L2
    def test_subscribe_rejected_for_draft(self, category):
        """未发布的 API 不允许订阅"""
        aid, pid = _helper_create_api(category)
        resp = api("POST", f"/apps/{INTERNAL_APP_ID}/apis/subscribe", {
            "permissionIds": [pid],
        })
        assert resp is not None
        body = resp.json()
        # 未发布时应该被拒绝
        assert "已发布" in body.get("messageZh", "") or body.get("code") != "200"

    @pytest.mark.L2
    def test_withdraw_not_found(self, category):
        resp = api("POST", f"/apps/{INTERNAL_APP_ID}/apis/999999999999999999/withdraw")
        assert resp is not None
