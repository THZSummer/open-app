#!/usr/bin/env python3
"""#14 PUT /connectors/{id}/versions/{vid}/invalidate — 停用连接器版本"""
import pytest
from _client import api


class TestConnectorVersionInvalidate:
    @pytest.mark.L2
    def test_invalidate(self, published_connector):
        """FR-009: 已发布→已失效，验证 status 变为 3"""
        cid, vid = published_connector
        # 前置确认：当前为已发布
        resp0 = api("GET", f"/connectors/{cid}/versions/{vid}")
        assert resp0.status_code == 200
        before = resp0.json()["data"]
        assert before.get("status") in (2, "2"), f"Expected status=2 (已发布), got {before.get('status')}"
        # 执行失效
        resp = api("PUT", f"/connectors/{cid}/versions/{vid}/invalidate")
        assert resp.status_code == 200
        assert resp.json()["code"] == "200"
        # 验证状态变更
        resp2 = api("GET", f"/connectors/{cid}/versions/{vid}")
        assert resp2.status_code == 200
        after = resp2.json()["data"]
        assert after.get("status") in (3, "3"), f"Expected status=3 (已失效), got {after.get('status')}"
