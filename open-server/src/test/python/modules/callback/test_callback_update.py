#!/usr/bin/env python3
"""PUT /callbacks/{id} — 更新回调"""
import pytest
from conftest import api, assert_operate_log


def _create_callback(category):
    r = api("POST", "/callbacks", {
        "nameCn": "update_cb", "nameEn": "update_cb",
        "categoryId": category,
    })
    return r.json()["data"]["id"]


class TestCallbackUpdate:
    @pytest.mark.L1
    def test_update_ok(self, category):
        cid = _create_callback(category)
        resp = api("PUT", f"/callbacks/{cid}", {
            "nameCn": "updated_cb", "nameEn": "updated_cb",
        })
        assert resp.status_code == 200

    @pytest.mark.L1
    def test_update_log(self, category):
        cid = _create_callback(category)
        resp = api("PUT", f"/callbacks/{cid}", {
            "nameCn": "logged_cb", "nameEn": "logged_cb",
        })
        assert resp.status_code == 200
        assert_operate_log("更新回调")
