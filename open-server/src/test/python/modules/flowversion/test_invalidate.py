#!/usr/bin/env python3
"""#34 PUT /flows/{id}/versions/{vid}/invalidate — 停用连接流版本"""
import pytest
from common import api, db


class TestFlowVersionInvalidate:
    @pytest.mark.L2
    def test_invalidate(self, draft_flow):
        """FR-028: 已发布→已失效，验证 status 变为 6"""
        fid, fvid = draft_flow
        # 前置：通过 DB 设为已发布状态
        db(f"UPDATE openplatform_v2_cp_flow_version_t SET status = 5 WHERE id = {fvid}")
        resp0 = api("GET", f"/flows/{fid}/versions/{fvid}")
        assert resp0.status_code == 200
        assert resp0.json()["data"].get("status") in (5, "5")
        # 执行失效
        resp = api("PUT", f"/flows/{fid}/versions/{fvid}/invalidate")
        assert resp.status_code == 200
        assert resp.json()["code"] == "200"
        # 验证状态变更
        resp2 = api("GET", f"/flows/{fid}/versions/{fvid}")
        assert resp2.status_code == 200
        after = resp2.json()["data"]
        assert after.get("status") in (6, "6"), f"Expected status=6 (已失效), got {after.get('status')}"
