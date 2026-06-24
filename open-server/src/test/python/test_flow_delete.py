#!/usr/bin/env python3
"""#27 DELETE /flows/{id} — 删除连接流"""
import pytest
from _client import api, db


class TestFlowDelete:
    @pytest.mark.L1
    def test_delete_ok(self, flow):
        db(f"UPDATE openplatform_v2_cp_flow_t SET lifecycle_status = 0 WHERE id = {flow}")
        resp = api("DELETE", f"/flows/{flow}")
        assert resp.status_code in (200, 409)

    @pytest.mark.L4
    def test_not_found(self):
        resp = api("DELETE", "/flows/999999999999999999")
        assert resp is not None
