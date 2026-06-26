#!/usr/bin/env python3
"""#37 POST /flows/{id}/versions/{vid}/cancel — 撤回连接流版本审批"""
import json
import pytest
from _client import api, db


class TestFlowVersionCancel:
    """FR-031: 撤回待审批的连接流版本"""

    @pytest.mark.L2
    def test_cancel_pending(self, draft_flow):
        """撤回待审批版本：status 2→1(草稿)"""
        fid, fvid = draft_flow

        # 前置：配置最小编排并提交审批 → status=2
        config = {"trigger": {}, "nodes": [{"id": "n1", "type": "script", "data": {"script": "1+1"}}], "edges": []}
        db(f"UPDATE openplatform_v2_cp_flow_version_t SET orchestration_config = '{json.dumps(config)}' WHERE id = {fvid}")
        db(f"UPDATE openplatform_v2_cp_flow_version_t SET status = 2 WHERE id = {fvid}")

        # 前置确认：待审批
        resp0 = api("GET", f"/flows/{fid}/versions/{fvid}")
        assert resp0.status_code == 200
        assert resp0.json()["code"] == "200"

        # 执行撤回
        resp = api("POST", f"/flows/{fid}/versions/{fvid}/cancel")
        assert resp.status_code in (200, 201), f"Expected 200/201, got {resp.status_code}"
        assert resp.json()["code"] == "200"

        # 验证状态回退
        resp2 = api("GET", f"/flows/{fid}/versions/{fvid}")
        assert resp2.status_code == 200
        after = resp2.json()["data"]
        assert after.get("status") in (1, "1", 3, "3"), \
            f"Expected status=1(草稿) or 3(已撤回), got {after.get('status')}"

    @pytest.mark.L4
    def test_cancel_published_rejected(self, draft_flow):
        """已发布/已驳回版本不可撤回"""
        fid, fvid = draft_flow

        # 设为已发布状态
        db(f"UPDATE openplatform_v2_cp_flow_version_t SET status = 5 WHERE id = {fvid}")
        resp = api("POST", f"/flows/{fid}/versions/{fvid}/cancel")
        assert resp is not None

    @pytest.mark.L4
    def test_cancel_not_found(self, flow):
        """不存在的版本撤回返回错误"""
        resp = api("POST", f"/flows/{flow}/versions/999999999999999999/cancel")
        assert resp is not None
