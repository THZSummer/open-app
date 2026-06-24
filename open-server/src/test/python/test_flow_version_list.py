#!/usr/bin/env python3
"""#29 GET /flows/{id}/versions — 连接流版本列表"""
import pytest
from _client import api


class TestFlowVersionList:
    @pytest.mark.L1
    def test_list(self, draft_flow):
        fid, _ = draft_flow
        resp = api("GET", f"/flows/{fid}/versions")
        assert resp.status_code == 200
