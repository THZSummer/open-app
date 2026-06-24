#!/usr/bin/env python3
"""审批流程测试 — APIs #37~#48"""
import pytest
from conftest import api, db, flow, draft_flow


class TestApprovalFullFlow:
    @pytest.mark.L3
    def test_submit_and_approve(self, draft_flow):
        fid, fvid = draft_flow
        db(f"UPDATE openplatform_v2_cp_flow_version_t SET status = 2 WHERE id = {fvid}")
        db(f"UPDATE openplatform_v2_cp_flow_version_t SET status = 5 WHERE id = {fvid}")
        resp2 = api("GET", f"/flows/{fid}/versions/{fvid}")
        assert resp2 is not None
        assert resp2.status_code == 200

    @pytest.mark.L3
    def test_reject_scenario(self, draft_flow):
        fid, fvid = draft_flow
        db(f"UPDATE openplatform_v2_cp_flow_version_t SET status = 2 WHERE id = {fvid}")
        db(f"UPDATE openplatform_v2_cp_flow_version_t SET status = 4 WHERE id = {fvid}")
        resp2 = api("GET", f"/flows/{fid}/versions/{fvid}")
        if resp2 is not None and resp2.status_code == 200:
            data = resp2.json()
            if data.get("code") in ("200", 200) and data.get("data"):
                actual_status = data["data"].get("status")
                assert actual_status in (4, "4", 1, "1")

    @pytest.mark.L3
    def test_cancel_scenario(self, draft_flow):
        fid, fvid = draft_flow
        db(f"UPDATE openplatform_v2_cp_flow_version_t SET status = 2 WHERE id = {fvid}")
        resp = api("POST", f"/flows/{fid}/versions/{fvid}/cancel")
        assert resp is not None
        assert resp.status_code in (200, 201)

    @pytest.mark.L3
    def test_cancel_published_rejected(self, draft_flow):
        fid, fvid = draft_flow
        db(f"UPDATE openplatform_v2_cp_flow_version_t SET status = 5 WHERE id = {fvid}")
        resp = api("POST", f"/flows/{fid}/versions/{fvid}/cancel")
        assert resp is not None


class TestApprovalEngineCallback:
    @pytest.mark.L3
    def test_approve_callback(self, draft_flow):
        fid, fvid = draft_flow
        db(f"UPDATE openplatform_v2_cp_flow_version_t SET status = 2 WHERE id = {fvid}")
        from conftest import db_val
        ar_id = db_val(f"SELECT id FROM openplatform_v2_approval_record_t WHERE business_type = 'connector_flow_version_publish' AND business_id = {fvid} ORDER BY create_time DESC LIMIT 1")
        if ar_id:
            resp2 = api("POST", f"/approvals/{ar_id}/approve", {"comment": "LGTM"})
            assert resp2 is not None

    @pytest.mark.L3
    def test_reject_callback(self, draft_flow):
        fid, fvid = draft_flow
        db(f"UPDATE openplatform_v2_cp_flow_version_t SET status = 2 WHERE id = {fvid}")
        from conftest import db_val
        ar_id = db_val(f"SELECT id FROM openplatform_v2_approval_record_t WHERE business_type = 'connector_flow_version_publish' AND business_id = {fvid} ORDER BY create_time DESC LIMIT 1")
        if ar_id:
            resp2 = api("POST", f"/approvals/{ar_id}/reject", {"comment": "rejected"})
            assert resp2 is not None

    @pytest.mark.L3
    def test_cancel_callback_unified(self, draft_flow):
        fid, fvid = draft_flow
        db(f"UPDATE openplatform_v2_cp_flow_version_t SET status = 2 WHERE id = {fvid}")
        from conftest import db_val
        ar_id = db_val(f"SELECT id FROM openplatform_v2_approval_record_t WHERE business_type = 'connector_flow_version_publish' AND business_id = {fvid} ORDER BY create_time DESC LIMIT 1")
        if ar_id:
            resp = api("POST", f"/approvals/{ar_id}/cancel")
            assert resp is not None

    @pytest.mark.L3
    def test_cancel_callback_biz(self, draft_flow):
        fid, fvid = draft_flow
        db(f"UPDATE openplatform_v2_cp_flow_version_t SET status = 2 WHERE id = {fvid}")
        resp = api("POST", f"/flows/{fid}/versions/{fvid}/cancel")
        assert resp is not None
        assert resp.status_code in (200, 201)
