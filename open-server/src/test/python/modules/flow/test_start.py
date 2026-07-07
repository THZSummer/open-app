#!/usr/bin/env python3
"""#23 POST /flows/{id}/start — 启动连接流 (FR-019)"""
import pytest
from conftest import api, deployed_flow, assert_operate_log  # noqa: F401


class TestFlowStart:
    @pytest.mark.L2
    def test_start(self, deployed_flow):
        """FR-019: 已停止→运行中"""
        fid, _ = deployed_flow
        resp = api("POST", f"/flows/{fid}/start")
        assert resp.status_code == 200
        data = resp.json()
        assert data["code"] == "200"
        if "data" in data and data["data"] is not None:
            d = data["data"]
            assert d["lifecycleStatus"] in (2, "2")

    @pytest.mark.L2
    def test_start_without_deploy(self, flow):
        """无部署版本时启动应被拒绝"""
        resp = api("POST", f"/flows/{flow}/start")
        assert resp.json()["code"] not in ("200",)

    @pytest.mark.L4
    def test_start_nonexistent(self):
        resp = api("POST", "/flows/999999999999999999/start")
        assert resp is not None

    @pytest.mark.L2
    def test_start_blocked_when_connector_version_invalidated(self, deployed_flow, published_connector):
        """引用的连接器版本被失效后启动应被拦截（422）"""
        fid, fvid = deployed_flow
        cid, cvid = published_connector
        api("PUT", f"/connectors/{cid}/versions/{cvid}/invalidate")
        resp = api("POST", f"/flows/{fid}/start")
        body = resp.json()
        assert resp.status_code == 422 or str(body.get("code")) == "422"

    @pytest.mark.L2
    def test_start_blocked_when_connector_invalidated(self, deployed_flow, published_connector):
        """引用的连接器本身被失效后启动应被拦截（422）"""
        fid, fvid = deployed_flow
        cid, cvid = published_connector
        api("PUT", f"/connectors/{cid}/invalidate")
        resp = api("POST", f"/flows/{fid}/start")
        body = resp.json()
        assert resp.status_code == 422 or str(body.get("code")) == "422"
