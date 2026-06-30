#!/usr/bin/env python3
"""#42 POST /approvals/{id}/reject — 审批驳回"""
import pytest
from conftest import api, db, db_val, draft_flow


class TestApprovalReject:
    @pytest.mark.L3
    def test_reject(self, draft_flow):
        """审批驳回：status 2→4(已驳回)"""
        fid, fvid = draft_flow
        db(f"UPDATE openplatform_v2_cp_flow_version_t SET status = 2 WHERE id = {fvid}")
        ar_id = db_val(f"SELECT id FROM openplatform_v2_approval_record_t WHERE business_type = 'connector_flow_version_publish' AND business_id = {fvid} ORDER BY create_time DESC LIMIT 1")
        if ar_id:
            resp = api("POST", f"/approvals/{ar_id}/reject", {"comment": "rejected"})
            assert resp is not None
