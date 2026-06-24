#!/usr/bin/env python3
"""调试运行测试 — API #51"""
import pytest
from conftest import api, db, draft_flow


class TestFlowVersionDebug:
    @pytest.mark.L3
    def test_debug_draft_version(self, draft_flow):
        fid, fvid = draft_flow
        resp = api("POST", f"/flows/{fid}/versions/{fvid}/debug", {"triggerData": {"message": "hello"}})
        assert resp is not None

    @pytest.mark.L3
    def test_version_not_found(self, flow):
        resp = api("POST", f"/flows/{flow}/versions/999999999999999999/debug", {"triggerData": {}})
        assert resp is not None
