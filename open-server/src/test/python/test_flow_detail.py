#!/usr/bin/env python3
"""#19 GET /flows/{id} — 连接流详情"""
import pytest
from _client import api


class TestFlowDetail:
    @pytest.mark.L1
    def test_detail_fields(self, flow):
        resp = api("GET", f"/flows/{flow}")
        assert resp.status_code == 200
        data = resp.json()["data"]
        assert "id" in data
        assert "nameCn" in data
        assert "lifecycleStatus" in data

    @pytest.mark.L4
    def test_detail_not_found(self):
        resp = api("GET", "/flows/999999999999999999")
        assert resp.status_code >= 400
