#!/usr/bin/env python3
"""#11 PUT /connectors/{id}/versions/{vid} — 更新连接器版本"""
import pytest
from _client import api


class TestConnectorVersionUpdate:
    CONFIG = {"protocol": "HTTP", "protocolConfig": {"url": "https://httpbin.org/get", "method": "GET"}, "timeoutMs": 5000}

    @pytest.mark.L1
    def test_update_draft(self, draft_connector):
        cid, vid = draft_connector
        resp = api("PUT", f"/connectors/{cid}/versions/{vid}", {"connectionConfig": self.CONFIG})
        assert resp.status_code == 200
