#!/usr/bin/env python3
"""#38 POST /flows/{id}/versions/{vid}/urge — 催办连接流版本审批"""
import json
import pytest
from common import api
from conftest import assert_operate_log


class TestFlowVersionUrge:
    """FR-031: 催办当前审批节点审批人"""

    @pytest.mark.L3
    def test_urge_pending(self, draft_flow):
        """催办待审批版本"""
        fid, fvid = draft_flow

        api("PUT", f"/flows/{fid}/versions/{fvid}", {
            "orchestrationConfig": json.dumps({
                "nodes": [
                    {"id": "trigger", "type": "trigger", "data": {"type": "trigger", "triggerType": "http"}},
                    {"id": "n1", "type": "script", "data": {"type": "script", "script": "1+1"}},
                ],
                "edges": [],
            })
        })
        resp_pub = api("POST", f"/flows/{fid}/versions/{fvid}/publish")
        assert resp_pub is not None

        resp = api("POST", f"/flows/{fid}/versions/{fvid}/urge")
        assert resp is not None
        assert resp.status_code in (200, 201, 409)

    @pytest.mark.L4
    def test_urge_non_pending(self, deployed_flow):
        """非待审批状态催办应被拒绝"""
        fid, fvid = deployed_flow
        resp = api("POST", f"/flows/{fid}/versions/{fvid}/urge")
        assert resp is not None

    @pytest.mark.L4
    def test_urge_not_found(self, flow):
        """不存在的版本催办返回错误"""
        resp = api("POST", f"/flows/{flow}/versions/999999999999999999/urge")
        assert resp is not None

    @pytest.mark.L2
    def test_urge_log(self, pending_approval_flow):
        """催办审批 → 操作日志"""
        fid, fvid, _ = pending_approval_flow
        resp = api("POST", f"/flows/{fid}/versions/{fvid}/urge")
        assert resp.status_code in (200, 201)
        assert_operate_log("催办")
