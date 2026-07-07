#!/usr/bin/env python3
"""POST /categories — 创建分类"""
import pytest
from conftest import api


class TestCategoryCreate:
    @pytest.mark.L1
    def test_create_ok(self):
        resp = api("POST", "/categories", {
            "nameCn": "pytest_create", "nameEn": "pytest_create",
        })
        assert resp.status_code in (200, 201)
        data = resp.json()["data"]
        assert data.get("nameCn") == "pytest_create"
        assert int(data["id"]) > 0

    @pytest.mark.L1
    def test_create_with_parent(self, category):
        resp = api("POST", "/categories", {
            "nameCn": "child", "nameEn": "child", "parentId": category,
        })
        assert resp.status_code in (200, 201)
        data = resp.json()["data"]
        assert int(data["parentId"]) == category

    @pytest.mark.L4
    def test_create_empty_name(self):
        resp = api("POST", "/categories", {
            "nameCn": "", "nameEn": "",
        })
        assert resp is not None
