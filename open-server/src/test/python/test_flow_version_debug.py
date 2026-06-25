#!/usr/bin/env python3
"""#51 POST /flows/{id}/versions/{vid}/debug — 调试运行"""
import pytest
from _client import api


class TestFlowVersionDebug:
    @pytest.mark.L3
    def test_debug_draft_version(self, draft_flow):
        """FR-041: 草稿版本调试触发，验证返回有效响应"""
        fid, fvid = draft_flow
        resp = api("POST", f"/flows/{fid}/versions/{fvid}/debug", {"triggerData": {"message": "hello"}})
        assert resp is not None
        assert resp.status_code in (200, 201), f"Expected 200/201, got {resp.status_code}"
        data = resp.json()
        assert data["code"] == "200"

    @pytest.mark.L3
    def test_version_not_found(self, flow):
        """调试不存在的版本返回 404"""
        resp = api("POST", f"/flows/{flow}/versions/999999999999999999/debug", {"triggerData": {}})
        assert resp is not None
        assert resp.status_code == 404
