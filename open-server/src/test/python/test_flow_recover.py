#!/usr/bin/env python3
"""#26 PUT /flows/{id}/recover — 恢复连接流"""
import pytest
from _client import api, db


class TestFlowRecover:
    @pytest.mark.L2
    def test_recover_invalidated(self, flow):
        db(f"UPDATE openplatform_v2_cp_flow_t SET lifecycle_status = 3 WHERE id = {flow}")
        resp = api("PUT", f"/flows/{flow}/recover")
        assert resp is not None

    @pytest.mark.L2
    def test_recover_running_rejected(self, flow):
        db(f"UPDATE openplatform_v2_cp_flow_t SET lifecycle_status = 2 WHERE id = {flow}")
        resp = api("PUT", f"/flows/{flow}/recover")
        assert resp is not None
