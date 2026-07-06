#!/usr/bin/env python3
"""部署→启动→调用 全链路 (L3)"""
import pytest
from common import api


class TestFlowDeployInvoke:
    @pytest.mark.L3
    def test_deploy_start_invoke(self, deployed_flow):
        """FR-018/FR-019: 部署+启动后调试调用，验证全链路"""
        fid, vid = deployed_flow
        # 启动
        resp = api("POST", f"/flows/{fid}/start")
        assert resp.status_code == 200
        assert resp.json()["code"] == "200"
        # 调试调用
        resp = api("POST", f"/flows/{fid}/versions/{vid}/debug", {"triggerData": {"message": "test"}})
        assert resp is not None
        assert resp.status_code in (200, 201), f"Expected 200/201, got {resp.status_code}"
        data = resp.json()
        assert data["code"] == "200"
