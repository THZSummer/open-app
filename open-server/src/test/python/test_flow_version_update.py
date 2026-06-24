#!/usr/bin/env python3
"""#31 PUT /flows/{id}/versions/{vid} — 更新连接流版本"""
import pytest
from _client import api


class TestFlowVersionUpdate:
    @pytest.mark.L1
    def test_update_draft(self, draft_flow):
        fid, fvid = draft_flow
        orch = '{"nodes":[],"edges":[]}'
        resp = api("PUT", f"/flows/{fid}/versions/{fvid}", {"orchestrationConfig": orch})
        assert resp.status_code == 200
