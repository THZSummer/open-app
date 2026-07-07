#!/usr/bin/env python3
"""PUT /apis/{id} — 更新 API"""
import pytest
from conftest import api, assert_operate_log


def _create_api(category):
    r = api("POST", "/apis", {
        "nameCn": "update_test", "nameEn": "update_test",
        "categoryId": category, "method": "GET",
        "path": "/update/test",
    })
    return r.json()["data"]["id"]


class TestApiUpdate:
    @pytest.mark.L1
    def test_update_ok(self, category):
        aid = _create_api(category)
        resp = api("PUT", f"/apis/{aid}", {
            "nameCn": "updated_api", "nameEn": "updated_api",
        })
        assert resp.status_code == 200

    @pytest.mark.L1
    def test_update_log(self, category):
        aid = _create_api(category)
        resp = api("PUT", f"/apis/{aid}", {
            "nameCn": "logged_api", "nameEn": "logged_api",
        })
        assert resp.status_code == 200
        assert_operate_log("更新API")
