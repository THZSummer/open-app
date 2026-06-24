#!/usr/bin/env python3
"""#24 POST /flows/{id}/stop — 停止连接流"""
import pytest
from _client import api, db


class TestFlowStop:
    @pytest.mark.L2
    def test_stop_ok(self, flow):
        db(f"UPDATE openplatform_v2_cp_flow_t SET lifecycle_status = 1 WHERE id = {flow}")
        api("POST", f"/flows/{flow}/start")
        resp = api("POST", f"/flows/{flow}/stop")
        assert resp is not None

    @pytest.mark.L2
    def test_duplicate_stop(self, flow):
        db(f"UPDATE openplatform_v2_cp_flow_t SET lifecycle_status = 1 WHERE id = {flow}")
        api("POST", f"/flows/{flow}/start")
        api("POST", f"/flows/{flow}/stop")
        resp = api("POST", f"/flows/{flow}/stop")
        assert resp is not None

    @pytest.mark.L4
    def test_not_found(self):
        resp = api("POST", "/flows/999999999999999999/stop")
        assert resp is not None
