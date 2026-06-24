#!/usr/bin/env python3
"""#4 PUT /connectors/{id} — 更新连接器"""
import pytest
from _client import api


class TestConnectorUpdate:
    @pytest.mark.L1
    def test_update_ok(self, connector):
        resp = api("PUT", f"/connectors/{connector}", {"nameCn": "新名称", "nameEn": "NewName"})
        assert resp.status_code == 200
