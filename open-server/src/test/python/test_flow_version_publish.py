#!/usr/bin/env python3
"""#32 POST /flows/{id}/versions/{vid}/publish — 发布连接流版本"""
import pytest
from _client import api


class TestFlowVersionPublish:
    @pytest.mark.L2
    def test_publish(self, draft_flow):
        fid, fvid = draft_flow
        resp = api("POST", f"/flows/{fid}/versions/{fvid}/publish")
        assert resp is not None
        assert resp.status_code in (200, 201, 422)
