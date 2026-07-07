#!/usr/bin/env python3
"""GET /apis/{id} — 获取 API 详情"""
import pytest
from conftest import api


def _create_api(category):
    r = api("POST", "/apis", {
        "nameCn": "detail_test", "nameEn": "detail_test",
        "categoryId": category, "method": "GET",
        "path": "/detail/test",
    })
    return r.json()["data"]["id"]


class TestApiDetail:
    @pytest.mark.L1
    def test_detail_ok(self, category):
        aid = _create_api(category)
        resp = api("GET", f"/apis/{aid}")
        assert resp.status_code == 200
        assert int(resp.json()["data"]["id"]) == aid

    @pytest.mark.L4
    def test_detail_not_found(self):
        resp = api("GET", "/apis/999999999999999999")
        assert resp is not None
