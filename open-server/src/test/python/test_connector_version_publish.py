#!/usr/bin/env python3
"""#12 PUT /connectors/{id}/versions/{vid}/publish — 发布连接器版本"""
import json
import pytest
from _client import api, db


class TestConnectorVersionPublish:
    CONFIG = {"protocol": "HTTP", "protocolConfig": {"url": "https://httpbin.org/get", "method": "GET"}, "timeoutMs": 5000}

    @pytest.mark.L2
    def test_publish(self, draft_connector):
        cid, vid = draft_connector
        db(f"UPDATE openplatform_v2_cp_connector_version_t SET connection_config = '{json.dumps(self.CONFIG)}' WHERE id = {vid}")
        resp = api("PUT", f"/connectors/{cid}/versions/{vid}/publish")
        assert resp.status_code == 200
