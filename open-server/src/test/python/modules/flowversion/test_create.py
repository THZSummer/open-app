#!/usr/bin/env python3
"""#28 POST /flows/{id}/versions — 创建连接流草稿版本 (FR-024a)"""
import pytest
from common import api, db, set_lookup_config
from conftest import assert_operate_log


def _create_and_finalize(flow_id: int):
    """创建草稿版本并通过 DB 标记为已发布（绕过草稿互斥，堆积版本数）"""
    api("POST", f"/flows/{flow_id}/versions")
    db(f"UPDATE openplatform_v2_cp_flow_version_t SET status = 5 "
       f"WHERE flow_id = {flow_id} AND status = 1 LIMIT 1")


class TestFlowVersionCreate:
    @pytest.mark.L1
    def test_create_draft(self, flow):
        """FR-024a: 创建空草稿，版本号递增"""
        resp = api("POST", f"/flows/{flow}/versions")
        assert resp.status_code in (200, 201)
        data = resp.json()
        assert data["code"] == "200"
        d = data["data"]
        # API 返回 versionId(number), id(number), versionNumber(number)
        assert d["versionNumber"] >= 1
        assert "versionId" in d or "id" in d

    @pytest.mark.L4
    def test_duplicate_draft(self, draft_flow):
        """FR-024a③: 已有草稿时再创建返回 409"""
        fid, _ = draft_flow
        resp = api("POST", f"/flows/{fid}/versions")
        assert resp.json()["code"] == "409"

    @pytest.mark.L2
    def test_create_log(self, flow):
        """创建连接流版本草稿 → 操作日志"""
        resp = api("POST", f"/flows/{flow}/versions", {})
        assert resp.status_code in (200, 201)
        vid = resp.json().get("data", {}).get("versionId")
        if vid:
            assert_operate_log("新增版本")

    @pytest.mark.L4
    def test_version_limit(self, flow):
        """#4 Flow.Max.Versions: 设置上限=2，创建第3个版本被拒 422"""
        set_lookup_config("Flow.Max.Versions", "2")
        try:
            _create_and_finalize(flow)
            _create_and_finalize(flow)
            resp = api("POST", f"/flows/{flow}/versions")
            assert resp.json()["code"] == "422", f"Expected 422, got {resp.json()}"
        finally:
            set_lookup_config("Flow.Max.Versions", "1000")

