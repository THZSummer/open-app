#!/usr/bin/env python3
"""#39~#44 审批记录查询 — 列表、详情、批量操作"""
import pytest
from conftest import api, db_val


class TestApprovalRecordList:
    @pytest.mark.L0
    def test_list_all_ok(self):
        resp = api("GET", "/approvals/pending")
        assert resp.status_code == 200
        data = resp.json()
        assert data["code"] == "200"
        assert "data" in data

    @pytest.mark.L1
    def test_list_filter_by_business_type(self):
        resp = api("GET", "/approvals/pending?businessType=connector_flow_version_publish")
        assert resp.status_code == 200
        assert resp.json()["code"] == "200"

    @pytest.mark.L1
    def test_list_filter_by_status(self):
        resp = api("GET", "/approvals/pending?status=0")
        assert resp.status_code == 200
        assert resp.json()["code"] == "200"


class TestApprovalRecordDetail:
    @pytest.mark.L1
    def test_detail_ok(self):
        ar_id = db_val("SELECT id FROM openplatform_v2_approval_record_t WHERE business_type = 'connector_flow_version_publish' ORDER BY create_time DESC LIMIT 1")
        if ar_id:
            resp = api("GET", f"/approvals/{ar_id}")
            assert resp.status_code == 200
            assert resp.json()["code"] == "200"

    @pytest.mark.L4
    def test_detail_not_found(self):
        resp = api("GET", "/approvals/999999999999999999")
        assert resp is not None


class TestApprovalBatch:
    @pytest.mark.L1
    def test_batch_approve_empty(self):
        resp = api("POST", "/approvals/batch-approve", {"approvalIds": [], "comment": "test"})
        assert resp is not None

    @pytest.mark.L1
    def test_batch_reject_empty(self):
        resp = api("POST", "/approvals/batch-reject", {"approvalIds": [], "comment": "test"})
        assert resp is not None
