#!/usr/bin/env python3
"""#28 POST /flows/{id}/versions — 创建连接流草稿版本 (FR-024a)"""
import pytest
from _client import api


class TestFlowVersionCreate:
    @pytest.mark.L1
    def test_create_draft(self, flow):
        """FR-024a: 创建空草稿，版本号递增"""
        resp = api("POST", f"/flows/{flow}/versions")
        assert resp.status_code in (200, 201)
        data = resp.json()
        assert data["code"] == "200"
        d = data["data"]
        # API 返回 versionId(number), id(number), versionNumber(number)
        assert d["versionNumber"] >= 1
        assert "versionId" in d or "id" in d

    @pytest.mark.L4
    def test_duplicate_draft(self, draft_flow):
        """FR-024a③: 已有草稿时再创建返回 409"""
        fid, _ = draft_flow
        resp = api("POST", f"/flows/{fid}/versions")
        assert resp.status_code == 409
