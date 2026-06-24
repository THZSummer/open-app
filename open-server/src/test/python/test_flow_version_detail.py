#!/usr/bin/env python3
"""#30 GET /flows/{id}/versions/{vid} — 连接流版本详情"""
import pytest
from _client import api


class TestFlowVersionDetail:
    @pytest.mark.L1
    def test_detail_ok(self, draft_flow):
        fid, fvid = draft_flow
        resp = api("GET", f"/flows/{fid}/versions/{fvid}")
        assert resp.status_code == 200

    @pytest.mark.L4
    def test_not_found(self, flow):
        resp = api("GET", f"/flows/{flow}/versions/999999999999999999")
        assert resp is not None
