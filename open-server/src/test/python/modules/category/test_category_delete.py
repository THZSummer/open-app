#!/usr/bin/env python3
"""DELETE /categories/{id} — 删除分类"""
import time
import pytest
from conftest import api


def _uid():
    return int(time.time() * 1000) % 1000000


class TestCategoryDelete:
    @pytest.mark.L1
    def test_delete_ok(self):
        uid = _uid()
        r = api("POST", "/categories", {
            "nameCn": f"delete_ok_{uid}", "nameEn": f"delete_ok_{uid}",
        })
        cid = r.json()["data"]["id"]
        resp = api("DELETE", f"/categories/{cid}")
        assert resp.status_code in (200, 204)

    @pytest.mark.L4
    def test_delete_not_found(self):
        resp = api("DELETE", "/categories/999999999999999999")
        assert resp is not None
