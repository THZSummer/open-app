#!/usr/bin/env python3
"""#43/44 POST /approvals/{id}/cancel + #37 POST /flows/{id}/versions/{vid}/cancel — 撤回审批"""
import pytest
from conftest import api, db, db_val, draft_flow


class TestApprovalCancelCallback:
    @pytest.mark.L3
    def test_cancel_via_approval_api(self, draft_flow):
        """通过审批接口撤回"""
        fid, fvid = draft_flow
        db(f"UPDATE openplatform_v2_cp_flow_version_t SET status = 2 WHERE id = {fvid}")
        ar_id = db_val(f"SELECT id FROM openplatform_v2_approval_record_t WHERE business_type = 'connector_flow_version_publish' AND business_id = {fvid} ORDER BY create_time DESC LIMIT 1")
        if ar_id:
            resp = api("POST", f"/approvals/{ar_id}/cancel")
            assert resp is not None

    @pytest.mark.L3
    def test_cancel_via_flow_api(self, draft_flow):
        """通过连接流版本接口撤回"""
        fid, fvid = draft_flow
        db(f"UPDATE openplatform_v2_cp_flow_version_t SET status = 2 WHERE id = {fvid}")
        resp = api("POST", f"/flows/{fid}/versions/{fvid}/cancel")
        assert resp is not None
        assert resp.status_code in (200, 201)
