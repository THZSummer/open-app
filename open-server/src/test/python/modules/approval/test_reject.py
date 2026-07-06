#!/usr/bin/env python3
"""#42 POST /approvals/{id}/reject — 审批驳回"""
import pytest
from conftest import api, pending_approval_flow  # noqa: F401


class TestApprovalReject:
    @pytest.mark.L3
    def test_reject(self, pending_approval_flow):
        """审批驳回：status 2→4(已驳回)"""
        fid, fvid, ar_id = pending_approval_flow
        if ar_id:
            resp = api("POST", f"/approvals/{ar_id}/reject", {"comment": "rejected"})
            assert resp is not None
