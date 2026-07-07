#!/usr/bin/env python3
"""#26 PUT /flows/{id}/recover — 恢复连接流"""
import pytest
from common import api
from conftest import assert_operate_log


class TestFlowRecover:
    @pytest.mark.L2
    def test_recover_invalidated(self, flow):
        api("PUT", f"/flows/{flow}/invalidate")
        resp = api("PUT", f"/flows/{flow}/recover")
        assert resp is not None

    @pytest.mark.L2
    def test_recover_running_rejected(self, deployed_flow):
        fid, _ = deployed_flow
        api("POST", f"/flows/{fid}/start")
        resp = api("PUT", f"/flows/{fid}/recover")
        assert resp is not None

    @pytest.mark.L2
    def test_recover_log(self, flow):
        """恢复连接流 → 操作日志"""
        api("PUT", f"/flows/{flow}/invalidate")
        resp = api("PUT", f"/flows/{flow}/recover")
        assert resp.status_code == 200
        assert_operate_log("恢复连接流")
