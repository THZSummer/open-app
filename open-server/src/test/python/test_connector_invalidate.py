#!/usr/bin/env python3
"""#5 PUT /connectors/{id}/invalidate — 停用连接器"""
import pytest
from _client import api


class TestConnectorInvalidate:
    @pytest.mark.L2
    def test_invalidate(self, published_connector):
        cid, _ = published_connector
        resp = api("PUT", f"/connectors/{cid}/invalidate")
        assert resp.status_code == 200
