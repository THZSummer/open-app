#!/usr/bin/env python3
"""#22 POST /flows/{id}/deploy — 部署连接流"""
import pytest
from _client import api


class TestFlowDeploy:
    @pytest.mark.L2
    def test_deploy_ok(self, deployed_flow):
        fid, fvid = deployed_flow
        resp = api("POST", f"/flows/{fid}/deploy", {"versionId": fvid})
        assert resp is not None
        assert resp.status_code in (200, 201)
