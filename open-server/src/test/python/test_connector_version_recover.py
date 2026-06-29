#!/usr/bin/env python3
"""#15 PUT /connectors/{id}/versions/{vid}/recover — 恢复连接器版本"""
import pytest
from _client import api, db


class TestConnectorVersionRecover:
    @pytest.mark.L2
    def test_recover(self, published_connector):
        """FR-011: 已失效→已发布，验证 status 变为 2"""
        cid, vid = published_connector
        # 前置：标记为已失效（通过 DB 模拟前置条件）
        db(f"UPDATE openplatform_v2_cp_connector_version_t SET status = 3 WHERE id = {vid}")
        # 确认前置状态
        resp0 = api("GET", f"/connectors/{cid}/versions/{vid}")
        assert resp0.status_code == 200
        assert resp0.json()["data"].get("status") in (3, "3")
        # 执行恢复
        resp = api("PUT", f"/connectors/{cid}/versions/{vid}/recover")
        assert resp.status_code == 200
        assert resp.json()["code"] == "200"
        # 验证状态恢复
        resp2 = api("GET", f"/connectors/{cid}/versions/{vid}")
        assert resp2.status_code == 200
        after = resp2.json()["data"]
        assert after.get("status") in (2, "2"), f"Expected status=2 (已发布), got {after.get('status')}"
