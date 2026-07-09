#!/usr/bin/env python3
"""#50 GET /executions/{id} — 运行记录详情"""
import pytest
from conftest import api


class TestExecutionDetail:
    @pytest.mark.L4
    def test_detail_not_found(self):
        resp = api("GET", "/executions/999999999999999999")
        assert resp is not None

    @pytest.mark.L4
    def test_detail_unwhitelisted_app(self):
        """#16 AppWhitelist: 未开通连接器平台的应用查运行记录详情被拒 403"""
        resp = api("GET", "/executions/1", app_id="00000000000000000000")
        assert resp.status_code == 403
        assert resp.json()["code"] == "403"
