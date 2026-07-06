#!/usr/bin/env python3
"""#6 PUT /connectors/{id}/recover — 恢复连接器"""
import pytest
from common import api
from conftest import assert_operate_log


class TestConnectorRecover:
    @pytest.mark.L2
    def test_recover(self, connector):
        api("PUT", f"/connectors/{connector}/invalidate")
        resp = api("PUT", f"/connectors/{connector}/recover")
        assert resp.status_code == 200

    @pytest.mark.L2
    def test_recover_log(self, connector):
        """恢复连接器 → 操作日志"""
        api("PUT", f"/connectors/{connector}/invalidate")
        resp = api("PUT", f"/connectors/{connector}/recover")
        assert resp.status_code == 200
        assert_operate_log("pytest_recover")

