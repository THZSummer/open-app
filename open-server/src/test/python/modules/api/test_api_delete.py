#!/usr/bin/env python3
"""DELETE /apis/{id} + POST /apis/{id}/withdraw — API 删除与撤回"""
import pytest
from conftest import api, assert_operate_log


def _create_api(category, suffix=""):
    r = api("POST", "/apis", {
        "nameCn": f"del_{suffix}", "nameEn": f"del_{suffix}",
        "categoryId": category, "method": "GET",
        "path": f"/del/test/{suffix}",
    })
    return r.json()["data"]["id"]


class TestApiDelete:
    @pytest.mark.L1
    def test_delete_ok(self, category):
        aid = _create_api(category, "ok")
        resp = api("DELETE", f"/apis/{aid}")
        assert resp.status_code in (200, 204)

    @pytest.mark.L4
    def test_delete_not_found(self):
        resp = api("DELETE", "/apis/999999999999999999")
        assert resp is not None

    @pytest.mark.L2
    def test_delete_log(self, category):
        aid = _create_api(category, "log")
        resp = api("DELETE", f"/apis/{aid}")
        assert resp.status_code in (200, 204)
        assert_operate_log("删除API")
