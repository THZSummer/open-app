#!/usr/bin/env python3
"""#34 PUT /flows/{id}/versions/{vid}/invalidate — 停用连接流版本"""
import pytest
from _client import api, db


class TestFlowVersionInvalidate:
    @pytest.mark.L2
    def test_invalidate(self, draft_flow):
        fid, fvid = draft_flow
        db(f"UPDATE openplatform_v2_cp_flow_version_t SET status = 5 WHERE id = {fvid}")
        resp = api("PUT", f"/flows/{fid}/versions/{fvid}/invalidate")
        assert resp is not None
