#!/usr/bin/env python3
"""订阅/退订 API/Event/Callback"""
import pytest
from conftest import api, INTERNAL_APP_ID


def _create_api(category):
    r = api("POST", "/apis", {
        "nameCn": "sub_api", "nameEn": "sub_api",
        "categoryId": category, "method": "GET",
        "path": "/sub/test/api",
    })
    return r.json()["data"]["id"]


class TestSubscribe:
    @pytest.mark.L2
    def test_subscribe_apis(self, category):
        aid = _create_api(category)
        resp = api("POST", f"/apps/{INTERNAL_APP_ID}/apis/subscribe", {
            "apiIds": [aid],
        })
        assert resp.status_code in (200, 201)

    @pytest.mark.L2
    def test_withdraw_api_subscription(self, category):
        aid = _create_api(category)
        api("POST", f"/apps/{INTERNAL_APP_ID}/apis/subscribe", {
            "apiIds": [aid],
        })
        resp = api("POST", f"/apps/{INTERNAL_APP_ID}/apis/{aid}/withdraw")
        assert resp.status_code in (200, 201, 204)
