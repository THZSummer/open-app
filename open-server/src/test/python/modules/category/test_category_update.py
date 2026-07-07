#!/usr/bin/env python3
"""PUT /categories/{id} — 更新分类"""
import pytest
from conftest import api, assert_operate_log


class TestCategoryUpdate:
    @pytest.mark.L1
    def test_update_ok(self, category):
        resp = api("PUT", f"/categories/{category}", {
            "nameCn": "updated", "nameEn": "updated_en",
        })
        assert resp.status_code == 200
        data = resp.json()["data"]
        assert data.get("nameCn") == "updated"

    @pytest.mark.L1
    def test_update_log(self, category):
        resp = api("PUT", f"/categories/{category}", {
            "nameCn": "logged", "nameEn": "logged_en",
        })
        assert resp.status_code == 200
        assert_operate_log("更新分类")
