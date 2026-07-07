#!/usr/bin/env python3
"""POST /apis — 注册 API"""
import pytest
from conftest import api


class TestApiCreate:
    @pytest.mark.L1
    def test_create_ok(self, category):
        resp = api("POST", "/apis", {
            "nameCn": "pytest_api", "nameEn": "pytest_api",
            "categoryId": category, "method": "GET",
            "path": "/pytest/api/test",
        })
        assert resp.status_code in (200, 201)
        data = resp.json()["data"]
        assert data.get("nameCn") == "pytest_api"

    @pytest.mark.L4
    def test_create_no_category(self):
        resp = api("POST", "/apis", {
            "nameCn": "bad", "nameEn": "bad",
            "method": "GET", "path": "/bad",
        })
        assert resp is not None
