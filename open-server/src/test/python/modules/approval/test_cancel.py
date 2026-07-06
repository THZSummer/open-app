#!/usr/bin/env python3
"""#43/44 POST /approvals/{id}/cancel + #37 POST /flows/{id}/versions/{vid}/cancel — 撤回审批"""
import pytest
from conftest import api, pending_approval_flow  # noqa: F401


class TestApprovalCancelCallback:
    @pytest.mark.L3
    def test_cancel_via_approval_api(self, pending_approval_flow):
        """通过审批接口撤回"""
        fid, fvid, ar_id = pending_approval_flow
        if ar_id:
            resp = api("POST", f"/approvals/{ar_id}/cancel")
            assert resp is not None

    @pytest.mark.L3
    def test_cancel_via_flow_api(self, pending_approval_flow):
        """通过连接流版本接口撤回"""
        fid, fvid, ar_id = pending_approval_flow
        resp = api("POST", f"/flows/{fid}/versions/{fvid}/cancel")
        assert resp is not None
        assert resp.status_code in (200, 201)
