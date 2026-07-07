#!/usr/bin/env python3
"""#16 DELETE /connectors/{id}/versions/{vid} — 删除连接器版本"""
import pytest
from common import api
from conftest import assert_operate_log


class TestConnectorVersionDelete:
    @pytest.mark.L2
    def test_delete_invalidated(self, published_connector):
        cid, vid = published_connector
        api("PUT", f"/connectors/{cid}/versions/{vid}/invalidate")
        resp = api("DELETE", f"/connectors/{cid}/versions/{vid}")
        assert resp.status_code in (200, 204)

    @pytest.mark.L2
    def test_delete_log(self, published_connector):
        """删除版本 → 操作日志"""
        cid, vid = published_connector
        api("PUT", f"/connectors/{cid}/versions/{vid}/invalidate")
        resp = api("DELETE", f"/connectors/{cid}/versions/{vid}")
        assert resp.status_code == 200
        assert_operate_log("删除")
