#!/usr/bin/env python3
"""#9 GET /connectors/{id}/versions — 连接器版本列表"""
import pytest
from _client import api


class TestConnectorVersionList:
    @pytest.mark.L1
    def test_list(self, published_connector):
        cid, _ = published_connector
        resp = api("GET", f"/connectors/{cid}/versions")
        assert resp.status_code == 200
