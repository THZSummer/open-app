#!/usr/bin/env python3
"""#36 DELETE /flows/{id}/versions/{vid} — 删除连接流版本"""
import pytest
from _client import api, db


class TestFlowVersionDelete:
    @pytest.mark.L2
    def test_delete_invalidated(self, draft_flow):
        fid, fvid = draft_flow
        db(f"UPDATE openplatform_v2_cp_flow_version_t SET status = 6 WHERE id = {fvid}")
        resp = api("DELETE", f"/flows/{fid}/versions/{fvid}")
        assert resp is not None
        assert resp.status_code in (200, 204)
