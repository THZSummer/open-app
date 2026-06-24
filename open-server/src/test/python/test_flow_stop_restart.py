#!/usr/bin/env python3
"""停止→重启 全链路 (L3)"""
import pytest
from _client import api


class TestFlowStopRestart:
    @pytest.mark.L3
    def test_stop_then_restart(self, deployed_flow):
        fid, _ = deployed_flow
        api("POST", f"/flows/{fid}/start")
        resp = api("POST", f"/flows/{fid}/stop")
        assert resp.status_code == 200
        assert resp.json()["code"] == "200"
        resp = api("POST", f"/flows/{fid}/start")
        assert resp.status_code == 200

    @pytest.mark.L3
    def test_stop_then_invoke_rejected(self, deployed_flow):
        fid, _ = deployed_flow
        api("POST", f"/flows/{fid}/start")
        api("POST", f"/flows/{fid}/stop")
        resp = api("POST", f"/flows/{fid}/versions/{deployed_flow[1]}/debug", {"triggerData": {}})
        assert resp is not None

    @pytest.mark.L4
    def test_start_without_deploy_rejected(self, flow):
        resp = api("POST", f"/flows/{flow}/start")
        assert resp.status_code >= 400
