#!/usr/bin/env python3
"""#21 POST /flows/{id}/copy — 复制连接流 (FR-017)"""
import pytest
from common import api, db, db_val
from conftest import assert_operate_log


class TestFlowCopy:
    @pytest.mark.L2
    def test_copy_ok(self, deployed_flow):
        """基础复制：已部署连接流复制成功"""
        fid, _ = deployed_flow
        resp = api("POST", f"/flows/{fid}/copy", {})
        assert resp is not None
        assert resp.status_code in (200, 201)
        data = resp.json().get("data", {})
        assert "_copy_" in data.get("nameCn", "")

    @pytest.mark.L2
    def test_copy_clears_deployed_and_stopped(self, deployed_flow):
        """复制后 deployed_version_id 为空，状态为已停止"""
        fid, _ = deployed_flow
        resp = api("POST", f"/flows/{fid}/copy", {})
        assert resp.status_code in (200, 201)
        new_fid = resp.json()["data"]["flowId"]
        r = api("GET", f"/flows/{new_fid}")
        d = r.json()["data"]
        assert d.get("deployedVersionId") is None
        assert d.get("lifecycleStatus") in (1, "1")

    @pytest.mark.L2
    def test_copy_pending_approval_becomes_draft(self, pending_approval_flow):
        """待审批版本复制后 → 草稿"""
        fid, fvid, _ = pending_approval_flow
        resp = api("POST", f"/flows/{fid}/copy", {})
        assert resp.status_code in (200, 201)
        new_fid = resp.json()["data"]["flowId"]
        r = api("GET", f"/flows/{new_fid}/versions?page=1&size=10")
        versions = r.json().get("data", [])
        assert len(versions) > 0
        assert versions[0].get("status") in (1, "1")

    @pytest.mark.L2
    def test_copy_rejected_becomes_draft(self, flow):
        """已驳回版本复制后 → 草稿"""
        fid = flow
        vid = int(str(fid) + "1")
        db(f"INSERT INTO openplatform_v2_cp_flow_version_t (id, flow_id, version_number, status, create_by, last_update_by) VALUES ({vid}, {fid}, 1, 4, 'tester', 'tester')")
        resp = api("POST", f"/flows/{fid}/copy", {})
        assert resp.status_code in (200, 201)
        new_fid = resp.json()["data"]["flowId"]
        new_status = db_val(f"SELECT status FROM openplatform_v2_cp_flow_version_t WHERE flow_id = {new_fid} AND version_number = 1")
        assert str(new_status) == "1"

    @pytest.mark.L2
    def test_copy_withdrawn_becomes_draft(self, flow):
        """已撤回版本复制后 → 草稿"""
        fid = flow
        vid = int(str(fid) + "1")
        db(f"INSERT INTO openplatform_v2_cp_flow_version_t (id, flow_id, version_number, status, create_by, last_update_by) VALUES ({vid}, {fid}, 1, 3, 'tester', 'tester')")
        resp = api("POST", f"/flows/{fid}/copy", {})
        assert resp.status_code in (200, 201)
        new_fid = resp.json()["data"]["flowId"]
        new_status = db_val(f"SELECT status FROM openplatform_v2_cp_flow_version_t WHERE flow_id = {new_fid} AND version_number = 1")
        assert str(new_status) == "1"

    @pytest.mark.L2
    def test_copy_published_keeps_status(self, deployed_flow):
        """已发布版本复制后 → 保持已发布"""
        fid, _ = deployed_flow
        resp = api("POST", f"/flows/{fid}/copy", {})
        assert resp.status_code in (200, 201)
        new_fid = resp.json()["data"]["flowId"]
        r = api("GET", f"/flows/{new_fid}/versions?page=1&size=10")
        versions = r.json().get("data", [])
        assert len(versions) > 0
        assert versions[0].get("status") in (5, "5")

    @pytest.mark.L2
    def test_copy_invalidated_keeps_status(self, flow):
        """已失效版本复制后 → 保持已失效"""
        fid = flow
        vid = int(str(fid) + "1")
        db(f"INSERT INTO openplatform_v2_cp_flow_version_t (id, flow_id, version_number, status, create_by, last_update_by) VALUES ({vid}, {fid}, 1, 6, 'tester', 'tester')")
        resp = api("POST", f"/flows/{fid}/copy", {})
        assert resp.status_code in (200, 201)
        new_fid = resp.json()["data"]["flowId"]
        new_status = db_val(f"SELECT status FROM openplatform_v2_cp_flow_version_t WHERE flow_id = {new_fid} AND version_number = 1")
        assert str(new_status) == "6"

    @pytest.mark.L2
    def test_copy_skips_deleted_version(self, flow):
        """已物理删除版本 → 跳过不复制"""
        fid = flow
        vid = int(str(fid) + "1")
        db(f"INSERT INTO openplatform_v2_cp_flow_version_t (id, flow_id, version_number, status, create_by, last_update_by) VALUES ({vid}, {fid}, 1, 7, 'tester', 'tester')")
        resp = api("POST", f"/flows/{fid}/copy", {})
        assert resp.status_code in (200, 201)
        new_fid = resp.json()["data"]["flowId"]
        count = db_val(f"SELECT COUNT(*) FROM openplatform_v2_cp_flow_version_t WHERE flow_id = {new_fid}")
        assert str(count) == "0"

    @pytest.mark.L2
    def test_copy_syncs_connector_version_refs(self, published_connector, deployed_flow):
        """复制时同步复制 connector_version_ref 引用关系"""
        fid, fvid = deployed_flow
        resp = api("POST", f"/flows/{fid}/copy", {})
        assert resp.status_code in (200, 201)
        new_fid = resp.json()["data"]["flowId"]
        new_ref_count = db_val(f"SELECT COUNT(*) FROM openplatform_v2_cp_connector_version_ref_t WHERE flow_id = {new_fid}")
        assert str(new_ref_count) == "1"

    @pytest.mark.L2
    def test_copy_source_not_affected(self, deployed_flow):
        """源连接流及其版本不受影响"""
        fid, fvid = deployed_flow
        r_before = api("GET", f"/flows/{fid}")
        before = r_before.json()["data"]

        resp = api("POST", f"/flows/{fid}/copy", {})
        assert resp.status_code in (200, 201)

        r_after = api("GET", f"/flows/{fid}")
        after = r_after.json()["data"]
        assert after.get("lifecycleStatus") == before.get("lifecycleStatus")
        assert after.get("deployedVersionId") == before.get("deployedVersionId")

    @pytest.mark.L2
    def test_copy_log(self, deployed_flow):
        """复制连接流 → 操作日志"""
        fid, _ = deployed_flow
        resp = api("POST", f"/flows/{fid}/copy", {})
        assert resp.status_code in (200, 201)
        new_fid = resp.json().get("data", {}).get("flowId")
        if new_fid:
            assert_operate_log("复制连接流")
