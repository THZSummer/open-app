#!/usr/bin/env python3
"""#8 POST /connectors/{id}/versions — 创建连接器草稿版本 (FR-005a)"""
import pytest
from _client import api


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
        assert resp.status_code == 409
