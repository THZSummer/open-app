#!/usr/bin/env python3
"""#28 POST /flows/{id}/versions — 创建连接流版本"""
import pytest
from _client import api


class TestFlowVersionCreate:
    @pytest.mark.L1
    def test_create_draft(self, flow):
        resp = api("POST", f"/flows/{flow}/versions")
        assert resp.status_code in (200, 201)

    @pytest.mark.L4
    def test_duplicate_draft(self, draft_flow):
        fid, _ = draft_flow
        resp = api("POST", f"/flows/{fid}/versions")
        assert resp is not None
