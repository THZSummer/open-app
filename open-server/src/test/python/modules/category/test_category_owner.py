#!/usr/bin/env python3
"""POST/GET/DELETE /categories/{id}/owners — 分类责任人管理"""
import pytest
from conftest import api


class TestCategoryOwner:
    @pytest.mark.L2
    def test_owner_crud(self, category):
        r = api("POST", f"/categories/{category}/owners", {
            "userId": "tester",
            "userName": "Test User",
        })
        assert r.status_code in (200, 201)

        r2 = api("GET", f"/categories/{category}/owners")
        assert r2.status_code == 200

        r3 = api("DELETE", f"/categories/{category}/owners/tester")
        assert r3.status_code in (200, 204)
