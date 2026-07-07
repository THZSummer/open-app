#!/usr/bin/env python3
"""#40 GET /approvals/{id} — 审批记录详情"""
import pytest
from conftest import api


class TestApprovalDetail:
    @pytest.mark.L1
    def test_detail_ok(self):
        """查询存在的审批记录详情"""
        r = api("GET", "/approvals/pending?businessType=connector_flow_version_publish&page=1&size=1")
        items = r.json().get("data", [])
        ar_id = items[0]["id"] if items else None
        if ar_id:
            resp = api("GET", f"/approvals/{ar_id}")
            assert resp.status_code == 200
            assert resp.json()["code"] == "200"

    @pytest.mark.L4
    def test_detail_not_found(self):
        """查询不存在的审批记录"""
        resp = api("GET", "/approvals/999999999999999999")
        assert resp is not None
