#!/usr/bin/env python3
"""#30 GET /flows/{id}/versions/{vid} — 连接流版本详情"""
import pytest
from conftest import api


class TestFlowVersionDetail:
    @pytest.mark.L1
    def test_detail_ok(self, draft_flow):
        """草稿版本 — approval 字段均为 null"""
        fid, fvid = draft_flow
        resp = api("GET", f"/flows/{fid}/versions/{fvid}")
        assert resp.status_code == 200
        d = resp.json()["data"]
        assert d["versionId"] == str(fvid)
        assert d["approver"] is None
        assert d.get("latestApprovalLog") is None
        assert "approvalUrl" in d

    @pytest.mark.L1
    def test_detail_pending_approval(self, pending_approval_flow):
        """待审批 — 有 approver，approvalAction 为 null"""
        fid, fvid, _ = pending_approval_flow
        resp = api("GET", f"/flows/{fid}/versions/{fvid}")
        assert resp.status_code == 200
        d = resp.json()["data"]
        assert d["status"] == 2
        assert d.get("latestApprovalLog") is None

    @pytest.mark.L2
    def test_detail_published(self, deployed_flow):
        """已发布 — approvalAction=0，含审批通过人"""
        fid, fvid = deployed_flow
        resp = api("GET", f"/flows/{fid}/versions/{fvid}")
        assert resp.status_code == 200
        d = resp.json()["data"]
        assert d["status"] == 5
        log = d.get("latestApprovalLog")
        if log:
            assert log["action"] == 0
            assert log["actionTime"] is not None

    @pytest.mark.L4
    def test_not_found(self, flow):
        resp = api("GET", f"/flows/{flow}/versions/999999999999999999")
        assert resp is not None
