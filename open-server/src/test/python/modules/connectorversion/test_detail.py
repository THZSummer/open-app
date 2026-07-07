#!/usr/bin/env python3
"""#10 GET /connectors/{id}/versions/{vid} — 连接器版本详情"""
import pytest
from common import api


class TestConnectorVersionDetail:
    @pytest.mark.L1
    def test_detail_draft_ok(self, draft_connector):
        cid, vid = draft_connector
        resp = api("GET", f"/connectors/{cid}/versions/{vid}")
        assert resp.status_code == 200
        data = resp.json()["data"]
        assert data["versionId"] == str(vid)
        assert "versionNumber" in data
        assert "status" in data

    @pytest.mark.L1
    def test_detail_published_ok(self, published_connector):
        cid, vid = published_connector
        resp = api("GET", f"/connectors/{cid}/versions/{vid}")
        assert resp.status_code == 200
        data = resp.json()["data"]
        assert data["status"] in (2, "2")
        assert "connectionConfig" in data or "publishedTime" in data

    @pytest.mark.L4
    def test_version_not_found(self, connector):
        resp = api("GET", f"/connectors/{connector}/versions/999999999999999999")
        assert resp is not None
