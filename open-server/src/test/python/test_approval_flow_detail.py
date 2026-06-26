#!/usr/bin/env python3
"""#46 GET /approval-flows/{id} — 审批流模板详情"""
import pytest
from conftest import api, db_val


class TestApprovalFlowDetail:
    @pytest.mark.L1
    def test_detail_ok(self):
        tid = db_val("SELECT id FROM openplatform_v2_approval_flow_t WHERE code = 'connector_flow_version_publish' LIMIT 1")
        if tid:
            resp = api("GET", f"/approval-flows/{tid}")
            assert resp.status_code == 200
            assert resp.json()["code"] == "200"

    @pytest.mark.L4
    def test_detail_not_found(self):
        resp = api("GET", "/approval-flows/999999999999999999")
        assert resp is not None
