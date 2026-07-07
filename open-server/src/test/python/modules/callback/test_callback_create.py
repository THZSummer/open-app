#!/usr/bin/env python3
"""POST /callbacks — 注册回调"""
import pytest
from conftest import api


class TestCallbackCreate:
    @pytest.mark.L1
    def test_create_ok(self, category):
        resp = api("POST", "/callbacks", {
            "nameCn": "pytest_cb", "nameEn": "pytest_cb",
            "categoryId": category,
        })
        assert resp.status_code in (200, 201)
        data = resp.json()["data"]
        assert data.get("nameCn") == "pytest_cb"
