#!/usr/bin/env python3
"""#33 POST /flows/{id}/versions/{vid}/copy-to-draft — 复制流版本到草稿"""
import pytest
from _client import api, db


class TestFlowVersionCopyToDraft:
    @pytest.mark.L2
    def test_copy_to_draft(self, draft_flow):
        """FR-025: 已发布→复制到草稿，验证生成新草稿"""
        fid, fvid = draft_flow
        # 前置：通过 DB 设为已发布
        db(f"UPDATE openplatform_v2_cp_flow_version_t SET status = 5 WHERE id = {fvid}")
        resp = api("POST", f"/flows/{fid}/versions/{fvid}/copy-to-draft")
        assert resp.status_code in (200, 201)
        assert resp.json()["code"] == "200"
        # 验证版本列表中存在新草稿
        resp2 = api("GET", f"/flows/{fid}/versions")
        assert resp2.status_code == 200
        vlist = resp2.json().get("data", [])
        drafts = [v for v in vlist if v.get("status") in (1, "1")]
        assert len(drafts) >= 1, f"Expected >=1 draft after copy, got {vlist}"
