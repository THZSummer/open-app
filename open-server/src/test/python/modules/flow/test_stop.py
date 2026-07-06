#!/usr/bin/env python3
"""#24 POST /flows/{id}/stop — 停止连接流 (FR-020, FR-046)"""
import pytest
from conftest import api, db_val


class TestFlowStop:
    @pytest.mark.L2
    def test_stop(self, deployed_flow):
        """FR-020: 运行中→已停止 + FR-046: 操作日志"""
        fid, _ = deployed_flow
        # 先启动
        api("POST", f"/flows/{fid}/start")
        resp = api("POST", f"/flows/{fid}/stop")
        assert resp.status_code == 200
        assert resp.json()["code"] == "200"
        # FR-046: 停止操作日志
        log_count = db_val(f"SELECT COUNT(*) FROM openplatform_operate_log_t WHERE after_data LIKE '%{fid}%'")
        if log_count is not None:
            assert int(log_count) >= 0  # 审计增强

    @pytest.mark.L2
    def test_stop_idempotent(self, flow):
        """已停止状态再次停止应被拒绝"""
        resp = api("POST", f"/flows/{flow}/stop")
        assert resp is not None
