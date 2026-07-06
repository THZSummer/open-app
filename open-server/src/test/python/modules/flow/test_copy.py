#!/usr/bin/env python3
"""#21 POST /flows/{id}/copy — 复制连接流 (FR-017)

V3 修复:
  - 待审批/已驳回/已撤回版本复制后 → 草稿
  - 已物理删除版本 → 跳过
  - connector_version_ref 引用关系同步复制
  - 部署版本清空，状态为已停止
  - 源连接流不受影响
"""
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
        """复制后 deployed_version_id 为空，状态为已停止(1)"""
        fid, _ = deployed_flow
        resp = api("POST", f"/flows/{fid}/copy", {})
        assert resp.status_code in (200, 201)
        new_fid = resp.json()["data"]["flowId"]
        deployed = db_val(f"SELECT deployed_version_id FROM openplatform_v2_cp_flow_t WHERE id = {new_fid}")
        status = db_val(f"SELECT lifecycle_status FROM openplatform_v2_cp_flow_t WHERE id = {new_fid}")
        assert deployed is None, f"复制后 deployed_version_id 应为空，实际: {deployed}"
        assert str(status) == "1", f"复制后状态应为已停止(1)，实际: {status}"

    @pytest.mark.L2
    def test_copy_pending_approval_becomes_draft(self, flow):
        """待审批(2)版本复制后 → 草稿(1)"""
        fid = flow
        vid = int(str(fid) + "1")
        db(f"INSERT INTO openplatform_v2_cp_flow_version_t (id, flow_id, version_number, status, create_by, last_update_by) VALUES ({vid}, {fid}, 1, 2, 'tester', 'tester')")
        resp = api("POST", f"/flows/{fid}/copy", {})
        assert resp.status_code in (200, 201)
        new_fid = resp.json()["data"]["flowId"]
        new_status = db_val(f"SELECT status FROM openplatform_v2_cp_flow_version_t WHERE flow_id = {new_fid} AND version_number = 1")
        assert str(new_status) == "1", f"待审批版本复制后应为草稿(1)，实际: {new_status}"

    @pytest.mark.L2
    def test_copy_rejected_becomes_draft(self, flow):
        """已驳回(4)版本复制后 → 草稿(1)"""
        fid = flow
        vid = int(str(fid) + "1")
        db(f"INSERT INTO openplatform_v2_cp_flow_version_t (id, flow_id, version_number, status, create_by, last_update_by) VALUES ({vid}, {fid}, 1, 4, 'tester', 'tester')")
        resp = api("POST", f"/flows/{fid}/copy", {})
        assert resp.status_code in (200, 201)
        new_fid = resp.json()["data"]["flowId"]
        new_status = db_val(f"SELECT status FROM openplatform_v2_cp_flow_version_t WHERE flow_id = {new_fid} AND version_number = 1")
        assert str(new_status) == "1", f"已驳回版本复制后应为草稿(1)，实际: {new_status}"

    @pytest.mark.L2
    def test_copy_withdrawn_becomes_draft(self, flow):
        """已撤回(3)版本复制后 → 草稿(1)"""
        fid = flow
        vid = int(str(fid) + "1")
        db(f"INSERT INTO openplatform_v2_cp_flow_version_t (id, flow_id, version_number, status, create_by, last_update_by) VALUES ({vid}, {fid}, 1, 3, 'tester', 'tester')")
        resp = api("POST", f"/flows/{fid}/copy", {})
        assert resp.status_code in (200, 201)
        new_fid = resp.json()["data"]["flowId"]
        new_status = db_val(f"SELECT status FROM openplatform_v2_cp_flow_version_t WHERE flow_id = {new_fid} AND version_number = 1")
        assert str(new_status) == "1", f"已撤回版本复制后应为草稿(1)，实际: {new_status}"

    @pytest.mark.L2
    def test_copy_published_keeps_status(self, deployed_flow):
        """已发布(5)版本复制后 → 保持已发布(5)"""
        fid, _ = deployed_flow
        resp = api("POST", f"/flows/{fid}/copy", {})
        assert resp.status_code in (200, 201)
        new_fid = resp.json()["data"]["flowId"]
        new_status = db_val(f"SELECT status FROM openplatform_v2_cp_flow_version_t WHERE flow_id = {new_fid} AND version_number = 1")
        assert str(new_status) == "5", f"已发布版本复制后应保持(5)，实际: {new_status}"

    @pytest.mark.L2
    def test_copy_invalidated_keeps_status(self, flow):
        """已失效(6)版本复制后 → 保持已失效(6)"""
        fid = flow
        vid = int(str(fid) + "1")
        db(f"INSERT INTO openplatform_v2_cp_flow_version_t (id, flow_id, version_number, status, create_by, last_update_by) VALUES ({vid}, {fid}, 1, 6, 'tester', 'tester')")
        resp = api("POST", f"/flows/{fid}/copy", {})
        assert resp.status_code in (200, 201)
        new_fid = resp.json()["data"]["flowId"]
        new_status = db_val(f"SELECT status FROM openplatform_v2_cp_flow_version_t WHERE flow_id = {new_fid} AND version_number = 1")
        assert str(new_status) == "6", f"已失效版本复制后应保持(6)，实际: {new_status}"

    @pytest.mark.L2
    def test_copy_skips_deleted_version(self, flow):
        """已物理删除(7)版本 → 跳过不复制"""
        fid = flow
        vid = int(str(fid) + "1")
        db(f"INSERT INTO openplatform_v2_cp_flow_version_t (id, flow_id, version_number, status, create_by, last_update_by) VALUES ({vid}, {fid}, 1, 7, 'tester', 'tester')")
        resp = api("POST", f"/flows/{fid}/copy", {})
        assert resp.status_code in (200, 201)
        new_fid = resp.json()["data"]["flowId"]
        count = db_val(f"SELECT COUNT(*) FROM openplatform_v2_cp_flow_version_t WHERE flow_id = {new_fid}")
        assert str(count) == "0", f"已物理删除版本应跳过，实际复制了 {count} 个版本"

    @pytest.mark.L2
    def test_copy_syncs_connector_version_refs(self, published_connector, deployed_flow):
        """复制时同步复制 connector_version_ref 引用关系"""
        cid, vid = published_connector
        fid, fvid = deployed_flow
        resp = api("POST", f"/flows/{fid}/copy", {})
        assert resp.status_code in (200, 201)
        new_fid = resp.json()["data"]["flowId"]
        new_ref_count = db_val(f"SELECT COUNT(*) FROM openplatform_v2_cp_connector_version_ref_t WHERE flow_id = {new_fid}")
        assert str(new_ref_count) == "1", f"新流应有1条引用记录，实际: {new_ref_count}"

    @pytest.mark.L2
    def test_copy_source_not_affected(self, deployed_flow):
        """源连接流及其版本不受影响"""
        fid, fvid = deployed_flow
        # 记录源数据
        src_flow_status = db_val(f"SELECT lifecycle_status FROM openplatform_v2_cp_flow_t WHERE id = {fid}")
        src_deployed = db_val(f"SELECT deployed_version_id FROM openplatform_v2_cp_flow_t WHERE id = {fid}")
        src_ver_count = db_val(f"SELECT COUNT(*) FROM openplatform_v2_cp_flow_version_t WHERE flow_id = {fid}")

        resp = api("POST", f"/flows/{fid}/copy", {})
        assert resp.status_code in (200, 201)

        # 验证源流状态不变
        after_flow_status = db_val(f"SELECT lifecycle_status FROM openplatform_v2_cp_flow_t WHERE id = {fid}")
        after_deployed = db_val(f"SELECT deployed_version_id FROM openplatform_v2_cp_flow_t WHERE id = {fid}")
        after_ver_count = db_val(f"SELECT COUNT(*) FROM openplatform_v2_cp_flow_version_t WHERE flow_id = {fid}")

        assert str(after_flow_status) == str(src_flow_status), "源流状态被修改了"
        assert str(after_deployed) == str(src_deployed), "源流部署版本被修改了"
        assert str(after_ver_count) == str(src_ver_count), "源流版本数量变了"

    @pytest.mark.L2
    def test_copy_log(self, deployed_flow):
        """复制连接流 → 操作日志"""
        fid, _ = deployed_flow
        resp = api("POST", f"/flows/{fid}/copy", {})
        assert resp.status_code in (200, 201)
        new_fid = resp.json().get("data", {}).get("flowId")
        if new_fid:
            assert_operate_log("复制连接流")

