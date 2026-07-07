#!/usr/bin/env python3
"""#22 POST /flows/{id}/deploy — 部署连接流 (FR-018, FR-046)"""
import pytest
from conftest import api, assert_operate_log


class TestFlowDeploy:
    @pytest.mark.L2
    def test_deploy_ok(self, deployed_flow):
        """FR-018: 部署绑定版本"""
        fid, fvid = deployed_flow
        resp = api("POST", f"/flows/{fid}/deploy", {"versionId": fvid})
        assert resp is not None
        assert resp.status_code in (200, 201)
        assert resp.json()["code"] == "200"

    @pytest.mark.L2
    def test_deploy_log(self, deployed_flow):
        """部署连接流 → 操作日志"""
        fid, fvid = deployed_flow
        resp = api("POST", f"/flows/{fid}/deploy", {"versionId": str(fvid)})
        assert resp.status_code == 200
        assert_operate_log("部署连接流:pytest_flow_deploy_log")

