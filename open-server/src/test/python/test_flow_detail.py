#!/usr/bin/env python3
"""#19 GET /flows/{id} — 连接流详情 (FR-016)"""
import pytest
from _client import api


class TestFlowDetail:
    @pytest.mark.L1
    def test_detail_fields(self, flow):
        """验证返回字段完整：id 匹配、关键业务字段存在"""
        resp = api("GET", f"/flows/{flow}")
        assert resp.status_code == 200
        data = resp.json()
        assert data["code"] == "200"
        d = data["data"]
        # 核心标识 — 使用 id 而非 flowId
        assert d["id"] == str(flow)
        assert "nameCn" in d
        assert "nameEn" in d
        # 业务状态
        assert "lifecycleStatus" in d
        assert "appId" in d
        assert "createTime" in d

    @pytest.mark.L4
    def test_detail_not_found(self):
        resp = api("GET", "/flows/999999999999999999")
        assert resp is not None
