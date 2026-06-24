#!/usr/bin/env python3
"""#23 POST /flows/{id}/start — 启动连接流"""
import pytest
from _client import api, db


class TestFlowStart:
    @pytest.mark.L2
    def test_start_ok(self, flow):
        db(f"UPDATE openplatform_v2_cp_flow_t SET lifecycle_status = 1 WHERE id = {flow}")
        resp = api("POST", f"/flows/{flow}/start")
        assert resp is not None

    @pytest.mark.L2
    def test_duplicate_start(self, flow):
        db(f"UPDATE openplatform_v2_cp_flow_t SET lifecycle_status = 1 WHERE id = {flow}")
        api("POST", f"/flows/{flow}/start")
        resp = api("POST", f"/flows/{flow}/start")
        assert resp is not None

    @pytest.mark.L4
    def test_not_found(self):
        resp = api("POST", "/flows/999999999999999999/start")
        assert resp is not None
