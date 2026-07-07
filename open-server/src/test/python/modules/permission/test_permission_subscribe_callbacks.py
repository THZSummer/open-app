#!/usr/bin/env python3
"""POST /apps/{appId}/callbacks/subscribe — 订阅回调权限"""
import time
import pytest
from conftest import api, INTERNAL_APP_ID


def _create(category):
    uid = int(time.time() * 1000) % 1000000
    r = api("POST", "/callbacks", {
        "nameCn": "sc", "nameEn": "sc",
        "categoryId": str(category),
        "permission": {"nameCn": "s", "nameEn": "s",
                       "scope": f"callback:test:sc{uid}"},
    })
    assert r.status_code == 200
    return r.json()["data"]["permission"]["id"]


class TestSubscribeCallbacks:
    @pytest.mark.L2
    def test_rejected_for_draft(self, category):
        pid = _create(category)
        resp = api("POST", f"/apps/{INTERNAL_APP_ID}/callbacks/subscribe", {
            "permissionIds": [pid],
        })
        assert resp is not None
        body = resp.json()
        assert "已发布" in body.get("messageZh", "") or body.get("code") != "200"
