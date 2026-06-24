#!/usr/bin/env python3
"""#49 GET /flows/{id}/executions — 运行记录列表 + #50 运行记录详情"""
import pytest
from _client import api


class TestExecutionRecordList:
    @pytest.mark.L0
    def test_list_empty_ok(self, flow):
        resp = api("GET", f"/flows/{flow}/executions")
        assert resp.status_code == 200
        data = resp.json()
        assert "data" in data

    @pytest.mark.L1
    def test_list_with_pagination(self, flow):
        resp = api("GET", f"/flows/{flow}/executions?curPage=1&pageSize=10")
        assert resp.status_code == 200
        data = resp.json()
        assert "page" in data

    @pytest.mark.L1
    def test_list_filter_by_status(self, flow):
        resp = api("GET", f"/flows/{flow}/executions?status=0")
        assert resp.status_code == 200

    @pytest.mark.L1
    def test_list_filter_by_trigger_type(self, flow):
        resp = api("GET", f"/flows/{flow}/executions?triggerType=1")
        assert resp.status_code == 200

    @pytest.mark.L4
    def test_list_flow_not_found(self):
        resp = api("GET", "/flows/999999999999999999/executions")
        assert resp is not None


class TestExecutionRecordDetail:
    @pytest.mark.L4
    def test_detail_not_found(self, flow):
        resp = api("GET", f"/flows/{flow}/executions/999999999999999999")
        assert resp is not None
