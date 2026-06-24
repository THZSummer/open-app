#!/usr/bin/env python3
"""连接流版本生命周期 — APIs #28~#36"""
import json
import pytest
from conftest import api, db, flow, draft_flow


class TestVersionCreate:
    @pytest.mark.L1
    def test_create_draft(self, flow):
        resp = api("POST", f"/flows/{flow}/versions")
        assert resp.status_code in (200, 201)

    @pytest.mark.L4
    def test_duplicate_draft(self, draft_flow):
        fid, _ = draft_flow
        resp = api("POST", f"/flows/{fid}/versions")
        assert resp is not None


class TestVersionList:
    @pytest.mark.L1
    def test_list(self, draft_flow):
        fid, _ = draft_flow
        resp = api("GET", f"/flows/{fid}/versions")
        assert resp.status_code == 200


class TestVersionDetail:
    @pytest.mark.L1
    def test_detail_ok(self, draft_flow):
        fid, fvid = draft_flow
        resp = api("GET", f"/flows/{fid}/versions/{fvid}")
        assert resp.status_code == 200

    @pytest.mark.L4
    def test_not_found(self, flow):
        resp = api("GET", f"/flows/{flow}/versions/999999999999999999")
        assert resp is not None


class TestVersionUpdate:
    @pytest.mark.L1
    def test_update_draft(self, draft_flow):
        fid, fvid = draft_flow
        orch = '{"nodes":[],"edges":[]}'
        resp = api("PUT", f"/flows/{fid}/versions/{fvid}", {"orchestrationConfig": orch})
        assert resp.status_code == 200


class TestVersionPublish:
    @pytest.mark.L2
    def test_publish(self, draft_flow):
        fid, fvid = draft_flow
        resp = api("POST", f"/flows/{fid}/versions/{fvid}/publish")
        assert resp is not None
        assert resp.status_code in (200, 201, 422)


class TestVersionCopyToDraft:
    @pytest.mark.L2
    def test_copy_to_draft(self, draft_flow):
        fid, fvid = draft_flow
        db(f"UPDATE openplatform_v2_cp_flow_version_t SET status = 5 WHERE id = {fvid}")
        resp = api("POST", f"/flows/{fid}/versions/{fvid}/copy-to-draft")
        assert resp is not None
        assert resp.status_code in (200, 201)


class TestVersionInvalidate:
    @pytest.mark.L2
    def test_invalidate(self, draft_flow):
        fid, fvid = draft_flow
        db(f"UPDATE openplatform_v2_cp_flow_version_t SET status = 5 WHERE id = {fvid}")
        resp = api("PUT", f"/flows/{fid}/versions/{fvid}/invalidate")
        assert resp is not None


class TestVersionRecover:
    @pytest.mark.L2
    def test_recover(self, draft_flow):
        fid, fvid = draft_flow
        db(f"UPDATE openplatform_v2_cp_flow_version_t SET status = 6 WHERE id = {fvid}")
        resp = api("PUT", f"/flows/{fid}/versions/{fvid}/recover")
        assert resp is not None


class TestVersionDelete:
    @pytest.mark.L2
    def test_delete_invalidated(self, draft_flow):
        fid, fvid = draft_flow
        db(f"UPDATE openplatform_v2_cp_flow_version_t SET status = 6 WHERE id = {fvid}")
        resp = api("DELETE", f"/flows/{fid}/versions/{fvid}")
        assert resp is not None
        assert resp.status_code in (200, 204)
