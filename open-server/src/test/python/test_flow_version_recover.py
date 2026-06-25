#!/usr/bin/env python3
"""#35 PUT /flows/{id}/versions/{vid}/recover — 恢复连接流版本"""
import pytest
from _client import api, db


class TestFlowVersionRecover:
    @pytest.mark.L2
    def test_recover(self, draft_flow):
        """FR-030: 已失效→已发布，验证 status 变为 5"""
        fid, fvid = draft_flow
        # 前置：通过 DB 设为已失效
        db(f"UPDATE openplatform_v2_cp_flow_version_t SET status = 6 WHERE id = {fvid}")
        resp0 = api("GET", f"/flows/{fid}/versions/{fvid}")
        assert resp0.status_code == 200
        assert resp0.json()["data"].get("status") in (6, "6")
        # 执行恢复
        resp = api("PUT", f"/flows/{fid}/versions/{fvid}/recover")
        assert resp.status_code == 200
        assert resp.json()["code"] == "200"
        # 验证状态恢复
        resp2 = api("GET", f"/flows/{fid}/versions/{fvid}")
        assert resp2.status_code == 200
        after = resp2.json()["data"]
        assert after.get("status") in (5, "5"), f"Expected status=5 (已发布), got {after.get('status')}"
