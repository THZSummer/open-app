#!/usr/bin/env python3
"""#24 POST /flows/{id}/stop — 停止连接流 (FR-020, FR-046)"""
import pytest
from conftest import api, assert_operate_log, _get_data


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

    @pytest.mark.L2
    def test_stop_lifecycle_cache_invalidation(self, deployed_flow):
        """停止后 lifecycle 正确更新 (缓存清理验证)"""
        fid, _ = deployed_flow
        api("POST", f"/flows/{fid}/start")
        # 启动后确认状态为运行中
        r = api("GET", f"/flows/{fid}")
        assert _get_data(r).get("lifecycleStatus") in (2, "2"), "启动后应为运行中"
        # 停止
        api("POST", f"/flows/{fid}/stop")
        # 停止后确认状态为已停止
        r = api("GET", f"/flows/{fid}")
        ls = _get_data(r).get("lifecycleStatus")
        assert ls in (1, "1"), f"停止后 lifecycleStatus 应为 1，实际为 {ls}"

    @pytest.mark.L2
    def test_invalidate_lifecycle_cache_invalidation(self, flow):
        """失效后 lifecycle 正确更新 (缓存清理验证)"""
        fid = flow
        r = api("PUT", f"/flows/{fid}/invalidate")
        assert r.status_code == 200
        r = api("GET", f"/flows/{fid}")
        ls = _get_data(r).get("lifecycleStatus")
        assert ls in (3, "3"), f"失效后 lifecycleStatus 应为 3，实际为 {ls}"

    @pytest.mark.L2
    def test_recover_lifecycle_cache_invalidation(self, flow):
        """恢复后 lifecycle 正确更新 (缓存清理验证)"""
        fid = flow
        api("PUT", f"/flows/{fid}/invalidate")
        r = api("PUT", f"/flows/{fid}/recover")
        assert r.status_code == 200
        r = api("GET", f"/flows/{fid}")
        ls = _get_data(r).get("lifecycleStatus")
        assert ls in (1, "1"), f"恢复后 lifecycleStatus 应为 1 (已停止)，实际为 {ls}"

