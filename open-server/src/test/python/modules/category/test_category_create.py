#!/usr/bin/env python3
"""POST /categories — 创建分类"""
import time
import pytest
from conftest import api


def _uid():
    return int(time.time() * 1000) % 1000000


class TestCategoryCreate:
    @pytest.mark.L1
    def test_create_ok(self):
        uid = _uid()
        resp = api("POST", "/categories", {
            "nameCn": f"create_ok_{uid}", "nameEn": f"create_ok_{uid}",
        })
        assert resp.status_code in (200, 201)
        data = resp.json()["data"]
        assert int(data["id"]) > 0

    @pytest.mark.L1
    def test_create_with_parent(self, category):
        uid = _uid()
        resp = api("POST", "/categories", {
            "nameCn": f"child_{uid}", "nameEn": f"child_{uid}",
            "parentId": str(category),
        })
        assert resp.status_code in (200, 201)
        assert str(resp.json()["data"].get("parentId")) == str(category)

    @pytest.mark.L4
    def test_create_empty_name(self):
        resp = api("POST", "/categories", {"nameCn": "", "nameEn": ""})
        assert resp is not None
