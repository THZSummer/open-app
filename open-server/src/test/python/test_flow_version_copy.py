#!/usr/bin/env python3
"""#33 POST /flows/{id}/versions/{vid}/copy-to-draft — 复制流版本到草稿"""
import pytest
from _client import api, db


class TestFlowVersionCopyToDraft:
    @pytest.mark.L2
    def test_copy_to_draft(self, draft_flow):
        fid, fvid = draft_flow
        db(f"UPDATE openplatform_v2_cp_flow_version_t SET status = 5 WHERE id = {fvid}")
        resp = api("POST", f"/flows/{fid}/versions/{fvid}/copy-to-draft")
        assert resp is not None
        assert resp.status_code in (200, 201)
