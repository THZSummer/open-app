#!/usr/bin/env python3
"""#49 GET /executions — 运行记录列表 + #50 运行记录详情"""
import pytest
from common import api


class TestExecutionRecordList:
    @pytest.mark.L0
    def test_list_empty_ok(self, flow):
        resp = api("GET", "/executions")
        assert resp.status_code == 200
        data = resp.json()
        assert "data" in data

    @pytest.mark.L1
    def test_list_with_pagination(self, flow):
        resp = api("GET", "/executions?curPage=1&pageSize=10")
        assert resp.status_code == 200
        data = resp.json()
        assert "page" in data

    @pytest.mark.L1
    def test_list_filter_by_status(self, flow):
        resp = api("GET", "/executions?status=0")
        assert resp.status_code == 200

    @pytest.mark.L1
    def test_list_filter_by_trigger_type(self, flow):
        resp = api("GET", "/executions?triggerType=1")
        assert resp.status_code == 200

    @pytest.mark.L1
    def test_list_filter_by_flow_id(self, flow):
        resp = api("GET", f"/executions?flowId={flow}")
        assert resp.status_code == 200

    @pytest.mark.L4
    def test_list_nonexistent_flow(self):
        resp = api("GET", "/executions?flowId=999999999999999999")
        assert resp is not None
        assert resp.status_code == 200  # 不存在的 flowId 返回空列表，不是 404

    @pytest.mark.L4
    def test_list_unwhitelisted_app(self):
        """#16 AppWhitelist: 未开通连接器平台的应用查询运行记录被拒 403"""
        resp = api("GET", "/executions", app_id="00000000000000000000")
        assert resp.status_code == 403
        assert resp.json()["code"] == "403"


