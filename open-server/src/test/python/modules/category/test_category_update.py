#!/usr/bin/env python3
"""PUT /categories/{id} — 更新分类"""
import pytest
from conftest import api


class TestCategoryUpdate:
    @pytest.mark.L1
    def test_update_ok(self, category):
        resp = api("PUT", f"/categories/{category}", {
            "nameCn": f"updated_{category}", "nameEn": f"updated_{category}",
        })
        assert resp.status_code == 200
        assert resp.json()["data"].get("nameCn") == f"updated_{category}"
