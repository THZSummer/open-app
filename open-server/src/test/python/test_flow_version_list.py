#!/usr/bin/env python3
"""#29 GET /flows/{id}/versions — 连接流版本列表"""
import pytest
from _client import api


class TestFlowVersionList:
    @pytest.mark.L1
    def test_list(self, draft_flow):
        """验证列表返回 data 为数组，包含版本实体字段"""
        fid, fvid = draft_flow
        resp = api("GET", f"/flows/{fid}/versions")
        assert resp.status_code == 200
        data = resp.json()
        assert data["code"] == "200"
        items = data["data"]
        assert isinstance(items, list)
        # 至少包含我们创建的那个草稿
        found = [v for v in items if str(v.get("versionId")) == str(fvid)]
        assert len(found) >= 1, f"Fixture version {fvid} not found in list"
        # 验证字段结构
        v = found[0]
        assert "versionNumber" in v
        assert "status" in v
