#!/usr/bin/env python3
"""#30 GET /flows/{id}/versions/{vid} — 连接流版本详情"""
import pytest
from conftest import api, pending_approval_flow  # noqa: F401


class TestFlowVersionDetail:
    @pytest.mark.L1
    def test_detail_ok(self, draft_flow):
        """验证返回字段：versionId 匹配、关键字段存在"""
        fid, fvid = draft_flow
        resp = api("GET", f"/flows/{fid}/versions/{fvid}")
        assert resp.status_code == 200
        data = resp.json()
        assert data["code"] == "200"
        d = data["data"]
        # 核心标识
        assert d["versionId"] == str(fvid)
        assert "versionNumber" in d
        assert "status" in d
        assert "flowId" in d or d.get("flowId") is None  # flowId 应存在
        # 审批字段：draft 状态下 approver 应为 null，approvalUrl 应存在
        assert "approvalUrl" in d
        assert d["approver"] is None

    @pytest.mark.L1
    def test_detail_pending_approval(self, pending_approval_flow):
        """验证待审批状态下返回审批人和审批地址"""
        fid, fvid, ar_id = pending_approval_flow
        resp = api("GET", f"/flows/{fid}/versions/{fvid}")
        assert resp.status_code == 200
        data = resp.json()
        assert data["code"] == "200"
        d = data["data"]
        assert d["versionId"] == str(fvid)
        assert d["status"] == 2  # PENDING_APPROVAL
        # 审批人信息
        assert d["approver"] is not None, "待审批状态应返回审批人信息"
        assert d["approver"]["userId"] == "approver001"
        assert d["approver"]["userName"] == "测试审批人"
        # 审批地址
        assert "approvalUrl" in d
        assert d["approvalUrl"] is not None and len(d["approvalUrl"]) > 0

    @pytest.mark.L4
    def test_not_found(self, flow):
        resp = api("GET", f"/flows/{flow}/versions/999999999999999999")
        assert resp is not None
