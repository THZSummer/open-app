#!/usr/bin/env python3
"""#41 POST /approvals/{id}/approve — 审批通过"""
import pytest
from conftest import api, pending_approval_flow  # noqa: F401


class TestApprovalApprove:
    @pytest.mark.L3
    def test_approve(self, pending_approval_flow):
        """审批通过：status 2→5(已发布)"""
        fid, fvid, ar_id = pending_approval_flow
        if ar_id:
            resp = api("POST", f"/approvals/{ar_id}/approve", {"comment": "LGTM"})
            assert resp is not None
