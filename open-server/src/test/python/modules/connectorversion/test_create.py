#!/usr/bin/env python3
"""#8 POST /connectors/{id}/versions — 创建连接器草稿版本 (FR-005a)"""
import pytest
from common import api, db, set_lookup_config
from conftest import assert_operate_log


def _create_and_finalize(connector_id: int):
    """创建草稿版本并通过 DB 标记为已发布（绕过草稿互斥，堆积版本数）"""
    api("POST", f"/connectors/{connector_id}/versions")
    db(f"UPDATE openplatform_v2_cp_connector_version_t SET status = 5 "
       f"WHERE connector_id = {connector_id} AND status = 1 LIMIT 1")


class TestConnectorVersionCreate:
    @pytest.mark.L1
    def test_create_draft(self, connector):
        """FR-005a: 创建空草稿，版本号递增，status=1(草稿)"""
        resp = api("POST", f"/connectors/{connector}/versions")
        assert resp.status_code in (200, 201)
        data = resp.json()
        assert data["code"] == "200"
        # 该接口响应无 data 字段，通过 GET 版本列表验证
        resp2 = api("GET", f"/connectors/{connector}/versions")
        assert resp2.status_code == 200
        versions_data = resp2.json()
        assert versions_data["code"] == "200"
        vlist = versions_data.get("data", [])
        assert isinstance(vlist, list) and len(vlist) >= 1, f"Expected >=1 version, got {vlist}"
        draft = vlist[0]
        assert isinstance(draft.get("versionId"), str)
        assert draft.get("versionNumber", 0) >= 1
        assert draft.get("status") in (1, "1"), f"Expected status=1 (草稿), got {draft.get('status')}"

    @pytest.mark.L4
    def test_duplicate_draft(self, draft_connector):
        """FR-005a③: 已有草稿时再创建返回 409"""
        cid, _ = draft_connector
        resp = api("POST", f"/connectors/{cid}/versions")
        assert resp.json()["code"] == "409"

    @pytest.mark.L2
    def test_create_log(self, connector):
        """创建草稿版本 → 操作日志"""
        resp = api("POST", f"/connectors/{connector}/versions", {})
        assert resp.status_code in (200, 201)
        vid = resp.json().get("data", {}).get("versionId")
        assert_operate_log("创建")

    @pytest.mark.L4
    def test_version_limit(self, connector):
        """#1 Connector.Max.Versions: 设置上限=2，创建第3个版本被拒 422"""
        set_lookup_config("Connector.Max.Versions", "2")
        try:
            _create_and_finalize(connector)
            _create_and_finalize(connector)
            resp = api("POST", f"/connectors/{connector}/versions")
            assert resp.json()["code"] == "422", f"Expected 422, got {resp.json()}"
        finally:
            set_lookup_config("Connector.Max.Versions", "1000")

