#!/usr/bin/env python3
"""#13 POST /connectors/{id}/versions/{vid}/copy-to-draft — 复制版本到草稿"""
import pytest
from common import api
from conftest import assert_operate_log


class TestConnectorVersionCopy:
    @pytest.mark.L2
    def test_copy_to_draft(self, published_connector):
        """FR-006: 已发布→复制到草稿，验证生成新草稿版本"""
        cid, vid = published_connector
        resp = api("POST", f"/connectors/{cid}/versions/{vid}/copy-to-draft")
        assert resp.status_code in (200, 201)
        assert resp.json()["code"] == "200"
        # 验证版本列表中存在新草稿
        resp2 = api("GET", f"/connectors/{cid}/versions")
        assert resp2.status_code == 200
        vlist = resp2.json().get("data", [])
        drafts = [v for v in vlist if v.get("status") in (1, "1")]
        assert len(drafts) >= 1, f"Expected >=1 draft version after copy, got {vlist}"

    @pytest.mark.L2
    def test_copy_log(self, draft_connector):
        """复制版本至草稿 → 操作日志"""
        cid, vid = draft_connector
        resp = api("POST", f"/connectors/{cid}/versions/{vid}/copy-to-draft")
        assert resp.status_code in (200, 201)
        assert_operate_log("复制")
