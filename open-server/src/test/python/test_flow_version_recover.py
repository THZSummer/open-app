#!/usr/bin/env python3
"""#35 PUT /flows/{id}/versions/{vid}/recover — 恢复连接流版本"""
import pytest
from _client import api, db


class TestFlowVersionRecover:
    @pytest.mark.L2
    def test_recover(self, draft_flow):
        fid, fvid = draft_flow
        db(f"UPDATE openplatform_v2_cp_flow_version_t SET status = 6 WHERE id = {fvid}")
        resp = api("PUT", f"/flows/{fid}/versions/{fvid}/recover")
        assert resp is not None
