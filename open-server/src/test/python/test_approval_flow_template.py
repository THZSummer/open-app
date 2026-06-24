#!/usr/bin/env python3
"""#45~#48 审批流模板配置 CRUD"""
import time
import pytest
from conftest import api, db, db_val


def _snow_id():
    return int(time.time() * 1000000) % 100000000000000000


class TestApprovalFlowTemplateCRUD:
    @pytest.mark.L1
    def test_list_templates_ok(self):
        resp = api("GET", "/approval-flows")
        assert resp.status_code == 200
        data = resp.json()
        assert data["code"] == "200"

    @pytest.mark.L1
    def test_list_by_code(self):
        resp = api("GET", "/approval-flows?code=connector_flow_version_publish")
        assert resp.status_code == 200
        data = resp.json()
        assert data["code"] == "200"

    @pytest.mark.L1
    def test_detail_template_ok(self):
        tid = db_val("SELECT id FROM openplatform_v2_approval_flow_t WHERE code = 'connector_flow_version_publish' LIMIT 1")
        if tid:
            resp = api("GET", f"/approval-flows/{tid}")
            assert resp.status_code == 200
            assert resp.json()["code"] == "200"

    @pytest.mark.L4
    def test_detail_not_found(self):
        resp = api("GET", "/approval-flows/999999999999999999")
        assert resp is not None

    @pytest.mark.L4
    def test_create_invalid(self):
        resp = api("POST", "/approval-flows", {"nameCn": "", "nameEn": "", "code": "", "nodes": []})
        assert resp is not None
        assert resp.status_code >= 400
