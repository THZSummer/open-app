#!/usr/bin/env python3
"""POST /callbacks — 注册回调"""
import time
import pytest
from conftest import api


def _uid():
    return int(time.time() * 1000) % 1000000


class TestCallbackCreate:
    @pytest.mark.L1
    def test_create_ok(self, category):
        uid = _uid()
        resp = api("POST", "/callbacks", {
            "nameCn": "pytest_cb", "nameEn": "pytest_cb",
            "categoryId": str(category),
            "permission": {"nameCn": "cp", "nameEn": "cp",
                           "scope": f"callback:test:read{uid}"},
        })
        assert resp.status_code == 200
        data = resp.json()["data"]
        assert data.get("nameCn") == "pytest_cb"
