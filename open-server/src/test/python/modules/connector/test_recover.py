#!/usr/bin/env python3
"""#6 PUT /connectors/{id}/recover — 恢复连接器"""
import pytest
from common import api, db


class TestConnectorRecover:
    @pytest.mark.L2
    def test_recover(self, connector):
        db(f"UPDATE openplatform_v2_cp_connector_t SET status = 3 WHERE id = {connector}")
        resp = api("PUT", f"/connectors/{connector}/recover")
        assert resp.status_code == 200
