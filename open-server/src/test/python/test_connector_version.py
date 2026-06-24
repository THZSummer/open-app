#!/usr/bin/env python3
"""连接器版本生命周期 — APIs #8~#16"""
import json
import pytest
from conftest import api, db, connector, draft_connector, published_connector


class TestVersionCreate:
    @pytest.mark.L1
    def test_create_draft(self, connector):
        resp = api("POST", f"/connectors/{connector}/versions")
        assert resp.status_code in (200, 201)

    @pytest.mark.L4
    def test_duplicate_draft(self, draft_connector):
        cid, _ = draft_connector
        resp = api("POST", f"/connectors/{cid}/versions")
        assert resp.status_code == 409


class TestVersionList:
    @pytest.mark.L1
    def test_list(self, published_connector):
        cid, _ = published_connector
        resp = api("GET", f"/connectors/{cid}/versions")
        assert resp.status_code == 200


class TestVersionUpdate:
    CONFIG = {"protocol": "HTTP", "protocolConfig": {"url": "https://httpbin.org/get", "method": "GET"}, "timeoutMs": 5000}

    @pytest.mark.L1
    def test_update_draft(self, draft_connector):
        cid, vid = draft_connector
        resp = api("PUT", f"/connectors/{cid}/versions/{vid}", {"connectionConfig": self.CONFIG})
        assert resp.status_code == 200


class TestVersionPublish:
    @pytest.mark.L2
    def test_publish(self, draft_connector):
        cid, vid = draft_connector
        db(f"UPDATE openplatform_v2_cp_connector_version_t SET connection_config = '{json.dumps(TestVersionUpdate.CONFIG)}' WHERE id = {vid}")
        resp = api("PUT", f"/connectors/{cid}/versions/{vid}/publish")
        assert resp.status_code == 200


class TestVersionLifecycle:
    @pytest.mark.L2
    def test_copy_to_draft(self, published_connector):
        cid, vid = published_connector
        resp = api("POST", f"/connectors/{cid}/versions/{vid}/copy-to-draft")
        assert resp.status_code in (200, 201)

    @pytest.mark.L2
    def test_invalidate(self, published_connector):
        cid, vid = published_connector
        resp = api("PUT", f"/connectors/{cid}/versions/{vid}/invalidate")
        assert resp.status_code == 200

    @pytest.mark.L2
    def test_recover(self, published_connector):
        cid, vid = published_connector
        db(f"UPDATE openplatform_v2_cp_connector_version_t SET status = 3 WHERE id = {vid}")
        resp = api("PUT", f"/connectors/{cid}/versions/{vid}/recover")
        assert resp.status_code == 200

    @pytest.mark.L2
    def test_delete_invalidated(self, published_connector):
        cid, vid = published_connector
        db(f"UPDATE openplatform_v2_cp_connector_version_t SET status = 3 WHERE id = {vid}")
        resp = api("DELETE", f"/connectors/{cid}/versions/{vid}")
        assert resp.status_code in (200, 204)
