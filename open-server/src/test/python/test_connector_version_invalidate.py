#!/usr/bin/env python3
"""#14 PUT /connectors/{id}/versions/{vid}/invalidate — 停用连接器版本"""
import pytest
from _client import api


class TestConnectorVersionInvalidate:
    @pytest.mark.L2
    def test_invalidate(self, published_connector):
        cid, vid = published_connector
        resp = api("PUT", f"/connectors/{cid}/versions/{vid}/invalidate")
        assert resp.status_code == 200
