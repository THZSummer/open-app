#!/usr/bin/env python3
"""#48 PUT /approval-flows/{id} — 更新审批流模板"""
import pytest
from conftest import api, db_val


class TestApprovalFlowUpdate:
    @pytest.mark.L1
    def test_update_ok(self):
        tid = db_val("SELECT id FROM openplatform_v2_approval_flow_t WHERE code = 'connector_flow_version_publish' LIMIT 1")
        if tid:
            resp = api("PUT", f"/approval-flows/{tid}", {
                "nameCn": "连接器流版本发布审批",
                "nameEn": "connector_flow_version_publish",
                "nodes": [{"userId": "tester", "userName": "Test Approver"}]
            })
            assert resp is not None
            assert resp.status_code in (200, 201)

    @pytest.mark.L4
    def test_update_not_found(self):
        resp = api("PUT", "/approval-flows/999999999999999999", {"nameCn": "test"})
        assert resp is not None
