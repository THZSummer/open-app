#!/usr/bin/env python3
"""#52 GET /data-processor/functions — 查询数据处理函数列表"""
import pytest
from _client import api


class TestDataProcessorFunctions:
    @pytest.mark.L1
    def test_list_functions_ok(self):
        resp = api("GET", "/data-processor/functions")
        if resp is None:
            pytest.skip("open-server not running")
        if resp.status_code == 404:
            pytest.skip("Endpoint /data-processor/functions not deployed yet")
        assert resp.status_code == 200
        data = resp.json()
        assert data["code"] == "200"
        assert "data" in data
        assert isinstance(data["data"], list)

    @pytest.mark.L1
    def test_functions_contain_expected(self):
        resp = api("GET", "/data-processor/functions")
        if resp is None:
            pytest.skip("open-server not running")
        if resp.status_code == 404:
            pytest.skip("Endpoint /data-processor/functions not deployed yet")
        assert resp.status_code == 200
        funcs = resp.json()["data"]
        names = [f["name"] for f in funcs]
        for expected in ["toString", "toNumber", "toBoolean", "formatDate"]:
            assert expected in names, f"Expected function {expected} not found"
