#!/usr/bin/env python3
"""#25 PUT /flows/{id}/invalidate — 停用连接流"""
import pytest
from common import api
from conftest import assert_operate_log


class TestFlowInvalidate:
    @pytest.mark.L2
    def test_invalidate_ok(self, deployed_flow):
        fid, _ = deployed_flow
        resp = api("PUT", f"/flows/{fid}/invalidate")
        assert resp is not None
        assert resp.status_code == 200

    @pytest.mark.L2
    def test_invalidate_log(self, flow):
        """失效连接流 → 操作日志"""
        resp = api("PUT", f"/flows/{flow}/invalidate")
        assert resp.status_code == 200
        assert_operate_log("失效连接流")

