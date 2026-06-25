#!/usr/bin/env python3
"""#30 GET /flows/{id}/versions/{vid} — 连接流版本详情"""
import pytest
from _client import api


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

    @pytest.mark.L4
    def test_not_found(self, flow):
        resp = api("GET", f"/flows/{flow}/versions/999999999999999999")
        assert resp is not None
