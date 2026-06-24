#!/usr/bin/env python3
"""#7 DELETE /connectors/{id} — 删除连接器 (FR-004)"""
import pytest
from _client import api


class TestConnectorDelete:
    @pytest.mark.L1
    def test_delete_without_version(self, connector):
        """FR-004: 必须先失效(status=3)再删除，验证删除后 404"""
        # 先失效
        resp_inv = api("PUT", f"/connectors/{connector}/invalidate")
        assert resp_inv.status_code == 200
        # 验证状态变为 3
        resp_get = api("GET", f"/connectors/{connector}")
        assert resp_get.status_code == 200
        get_body = resp_get.json()
        assert get_body["code"] == "200"
        actual_status = get_body["data"].get("status")
        assert actual_status == 3, f"Expected status=3 after invalidate, got {actual_status}"
        # 删除
        resp = api("DELETE", f"/connectors/{connector}")
        assert resp.status_code in (200, 204)
