#!/usr/bin/env python3
"""#15 PUT /connectors/{id}/versions/{vid}/recover — 恢复连接器版本"""
import pytest
from common import api
from conftest import assert_operate_log


class TestConnectorVersionRecover:
    @pytest.mark.L2
    def test_recover(self, published_connector):
        """FR-011: 已失效→已发布，验证 status 变为 2"""
        cid, vid = published_connector
        api("PUT", f"/connectors/{cid}/versions/{vid}/invalidate")
        resp0 = api("GET", f"/connectors/{cid}/versions/{vid}")
        assert resp0.status_code == 200
        assert resp0.json()["data"].get("status") in (3, "3")
        resp = api("PUT", f"/connectors/{cid}/versions/{vid}/recover")
        assert resp.status_code == 200
        assert resp.json()["code"] == "200"
        resp2 = api("GET", f"/connectors/{cid}/versions/{vid}")
        assert resp2.status_code == 200
        after = resp2.json()["data"]
        assert after.get("status") in (2, "2"), f"Expected status=2 (已发布), got {after.get('status')}"

    @pytest.mark.L2
    def test_recover_log(self, published_connector):
        """恢复版本 → 操作日志"""
        cid, vid = published_connector
        api("PUT", f"/connectors/{cid}/versions/{vid}/invalidate")
        resp = api("PUT", f"/connectors/{cid}/versions/{vid}/recover")
        assert resp.status_code == 200
        assert_operate_log("恢复")
