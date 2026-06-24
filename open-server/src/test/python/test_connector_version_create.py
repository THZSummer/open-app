#!/usr/bin/env python3
"""#8 POST /connectors/{id}/versions — 创建连接器版本"""
import pytest
from _client import api


class TestConnectorVersionCreate:
    @pytest.mark.L1
    def test_create_draft(self, connector):
        resp = api("POST", f"/connectors/{connector}/versions")
        assert resp.status_code in (200, 201)

    @pytest.mark.L4
    def test_duplicate_draft(self, draft_connector):
        cid, _ = draft_connector
        resp = api("POST", f"/connectors/{cid}/versions")
        assert resp.status_code == 409
