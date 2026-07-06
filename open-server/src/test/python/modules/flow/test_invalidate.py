#!/usr/bin/env python3
"""#25 PUT /flows/{id}/invalidate — 停用连接流"""
import pytest
from common import api


class TestFlowInvalidate:
    @pytest.mark.L2
    def test_invalidate_ok(self, deployed_flow):
        fid, _ = deployed_flow
        resp = api("PUT", f"/flows/{fid}/invalidate")
        assert resp is not None
        assert resp.status_code == 200
