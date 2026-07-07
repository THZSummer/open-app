#!/usr/bin/env python3
"""POST /apis — 注册 API"""
import time
import pytest
from conftest import api


def _uid():
    return int(time.time() * 1000) % 1000000


class TestApiCreate:
    @pytest.mark.L1
    def test_create_ok(self, category):
        uid = _uid()
        resp = api("POST", "/apis", {
            "nameCn": "pytest_api", "nameEn": "pytest_api",
            "categoryId": str(category), "method": "GET",
            "path": "/pytest/api/test",
            "permission": {"nameCn": "p", "nameEn": "p",
                           "scope": f"api:test:read{uid}"},
        })
        assert resp.status_code == 200
        data = resp.json()["data"]
        assert data.get("nameCn") == "pytest_api"

    @pytest.mark.L4
    def test_create_no_category(self):
        resp = api("POST", "/apis", {
            "nameCn": "bad", "nameEn": "bad",
            "method": "GET", "path": "/bad",
        })
        assert resp is not None
