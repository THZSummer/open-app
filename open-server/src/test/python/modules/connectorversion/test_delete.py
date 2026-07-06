#!/usr/bin/env python3
"""#16 DELETE /connectors/{id}/versions/{vid} — 删除连接器版本"""
import pytest
from common import api, db


class TestConnectorVersionDelete:
    @pytest.mark.L2
    def test_delete_invalidated(self, published_connector):
        cid, vid = published_connector
        db(f"UPDATE openplatform_v2_cp_connector_version_t SET status = 3 WHERE id = {vid}")
        resp = api("DELETE", f"/connectors/{cid}/versions/{vid}")
        assert resp.status_code in (200, 204)
