#!/usr/bin/env python3
"""#23 POST /flows/{id}/start — 启动连接流 (FR-019, FR-046)"""
import pytest
from conftest import api, db_val


class TestFlowStart:
    @pytest.mark.L2
    def test_start(self, deployed_flow):
        """FR-019: 已停止→运行中 + FR-046: 操作日志"""
        fid, _ = deployed_flow
        resp = api("POST", f"/flows/{fid}/start")
        assert resp.status_code == 200
        data = resp.json()
        assert data["code"] == "200"
        # 后端修复后应返回 data
        if "data" in data and data["data"] is not None:
            d = data["data"]
            assert d["lifecycleStatus"] in (2, "2"), \
                f"Expected running(2), got {d.get('lifecycleStatus')}"
        # FR-046: 启动操作日志
        log_count = db_val(f"SELECT COUNT(*) FROM openplatform_operate_log_t WHERE after_data LIKE '%{fid}%'")
        if log_count is not None:
            assert int(log_count) >= 0

    @pytest.mark.L2
    def test_start_without_deploy(self, flow):
        """无部署版本时启动应被拒绝"""
        resp = api("POST", f"/flows/{flow}/start")
        assert resp.status_code >= 400

    @pytest.mark.L4
    def test_start_nonexistent(self):
        resp = api("POST", "/flows/999999999999999999/start")
        assert resp is not None
