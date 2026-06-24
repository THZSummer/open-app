#!/usr/bin/env python3
"""#15 PUT /connectors/{id}/versions/{vid}/recover — 恢复连接器版本"""
import pytest
from _client import api, db


class TestConnectorVersionRecover:
    @pytest.mark.L2
    def test_recover(self, published_connector):
        cid, vid = published_connector
        db(f"UPDATE openplatform_v2_cp_connector_version_t SET status = 3 WHERE id = {vid}")
        resp = api("PUT", f"/connectors/{cid}/versions/{vid}/recover")
        assert resp.status_code == 200
