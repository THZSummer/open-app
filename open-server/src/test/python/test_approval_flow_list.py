#!/usr/bin/env python3
"""#45 GET /approval-flows — 审批流模板列表"""
import pytest
from conftest import api, db_val


class TestApprovalFlowList:
    @pytest.mark.L1
    def test_list_ok(self):
        resp = api("GET", "/approval-flows")
        assert resp.status_code == 200
        assert resp.json()["code"] == "200"

    @pytest.mark.L1
    def test_list_by_code(self):
        resp = api("GET", "/approval-flows?code=connector_flow_version_publish")
        assert resp.status_code == 200
        assert resp.json()["code"] == "200"
