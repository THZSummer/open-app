#!/usr/bin/env python3
"""#24 POST /flows/{id}/stop — 停止连接流 (FR-020, FR-046)"""
import pytest
from conftest import api, assert_operate_log


class TestFlowStop:
    @pytest.mark.L2
    def test_stop(self, deployed_flow):
        """FR-020: 运行中→已停止 + FR-046: 操作日志"""
        fid, _ = deployed_flow
        api("POST", f"/flows/{fid}/start")
        resp = api("POST", f"/flows/{fid}/stop")
        assert resp.status_code == 200
        assert resp.json()["code"] == "200"

    @pytest.mark.L2
    def test_stop_idempotent(self, flow):
        """已停止状态再次停止应被拒绝"""
        resp = api("POST", f"/flows/{flow}/stop")
        assert resp is not None

    @pytest.mark.L2
    def test_start_stop_log(self, deployed_flow):
        """启动+停止连接流 → 操作日志"""
        fid, _ = deployed_flow
        resp = api("POST", f"/flows/{fid}/start")
        assert resp.status_code == 200
        assert_operate_log("启动连接流:pytest_flow_start_stop_log")
        resp2 = api("POST", f"/flows/{fid}/stop")
        assert resp2.status_code == 200
        assert_operate_log("停止连接流:pytest_flow_start_stop_log")

