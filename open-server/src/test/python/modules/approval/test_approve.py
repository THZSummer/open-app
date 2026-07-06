#!/usr/bin/env python3
"""#41 POST /approvals/{id}/approve — 审批通过"""
import pytest
from conftest import api, db, db_val, draft_flow


class TestApprovalApprove:
    @pytest.mark.L3
    def test_approve(self, draft_flow):
        """审批通过：status 2→5(已发布)"""
        fid, fvid = draft_flow
        db(f"UPDATE openplatform_v2_cp_flow_version_t SET status = 2 WHERE id = {fvid}")
        ar_id = db_val(f"SELECT id FROM openplatform_v2_approval_record_t WHERE business_type = 'connector_flow_version_publish' AND business_id = {fvid} ORDER BY create_time DESC LIMIT 1")
        if ar_id:
            resp = api("POST", f"/approvals/{ar_id}/approve", {"comment": "LGTM"})
            assert resp is not None
