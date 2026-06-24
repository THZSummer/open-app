#!/usr/bin/env python3
"""#7 DELETE /connectors/{id} — 删除连接器"""
import pytest
from _client import api


class TestConnectorDelete:
    @pytest.mark.L1
    def test_delete_without_version(self, connector):
        resp = api("DELETE", f"/connectors/{connector}")
        assert resp.status_code == 200
