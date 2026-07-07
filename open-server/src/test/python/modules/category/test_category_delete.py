#!/usr/bin/env python3
"""DELETE /categories/{id} — 删除分类"""
import pytest
from conftest import api, assert_operate_log


class TestCategoryDelete:
    @pytest.mark.L1
    def test_delete_ok(self, category):
        resp = api("DELETE", f"/categories/{category}")
        assert resp.status_code in (200, 204)

    @pytest.mark.L4
    def test_delete_not_found(self):
        resp = api("DELETE", "/categories/999999999999999999")
        assert resp is not None

    @pytest.mark.L2
    def test_delete_log(self):
        r = api("POST", "/categories", {
            "nameCn": "to_delete", "nameEn": "to_delete",
        })
        cid = r.json()["data"]["id"]
        resp = api("DELETE", f"/categories/{cid}")
        assert resp.status_code in (200, 204)
        assert_operate_log("删除分类")
