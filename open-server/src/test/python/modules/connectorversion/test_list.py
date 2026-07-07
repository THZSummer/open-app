#!/usr/bin/env python3
"""#9 GET /connectors/{id}/versions — 连接器版本列表"""
import pytest
from common import api


class TestConnectorVersionList:
    @pytest.mark.L1
    def test_list(self, published_connector):
        """验证列表返回 data 为数组，包含版本实体字段"""
        cid, vid = published_connector
        resp = api("GET", f"/connectors/{cid}/versions")
        assert resp.status_code == 200
        data = resp.json()
        assert data["code"] == "200"
        items = data["data"]
        assert isinstance(items, list)
        # 至少包含已发布版本
        found = [v for v in items if str(v.get("versionId")) == str(vid)]
        assert len(found) >= 1, f"Fixture version {vid} not found in list"
        v = found[0]
        assert "versionNumber" in v
        assert "status" in v
