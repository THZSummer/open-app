#!/usr/bin/env python3
"""部署→启动→调用 全链路 (L3)"""
import pytest
from _client import api


class TestFlowDeployInvoke:
    @pytest.mark.L3
    def test_deploy_start_invoke(self, deployed_flow):
        fid, vid = deployed_flow
        resp = api("POST", f"/flows/{fid}/start")
        assert resp.status_code == 200
        resp = api("POST", f"/flows/{fid}/versions/{vid}/debug", {"triggerData": {"message": "test"}})
        assert resp is not None
