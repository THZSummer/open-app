#!/usr/bin/env python3
"""#30 GET /flows/{id}/versions/{vid} — 连接流版本详情

V3 新增审批操作字段:
  - approvalOperatorId/Name: 来自 approval_log_t.operator_id/operator_name
  - approvalAction: 0=通过, 1=驳回, 2=撤回
  - approvalComment: 审批意见
  - approvalActionTime: 操作时间
"""
import time
import pytest
from conftest import api, db, pending_approval_flow  # noqa: F401


def _snow_id():
    return int(time.time() * 1000000) % 100000000000000000


class TestFlowVersionDetail:
    @pytest.mark.L1
    def test_detail_ok(self, draft_flow):
        """草稿版本 — approval 字段均为 null"""
        fid, fvid = draft_flow
        resp = api("GET", f"/flows/{fid}/versions/{fvid}")
        assert resp.status_code == 200
        d = resp.json()["data"]
        assert d["versionId"] == str(fvid)
        assert d["approver"] is None
        assert d.get("latestApprovalLog") is None
        assert "approvalUrl" in d

    @pytest.mark.L1
    def test_detail_pending_approval(self, pending_approval_flow):
        """待审批 — 有 approver，approvalAction 为 null（尚无操作）"""
        fid, fvid, _ = pending_approval_flow
        resp = api("GET", f"/flows/{fid}/versions/{fvid}")
        assert resp.status_code == 200
        d = resp.json()["data"]
        assert d["status"] == 2
        assert d["approver"] is not None
        assert d["approver"]["userId"] == "approver001"
        assert d.get("latestApprovalLog") is None

    @pytest.mark.L2
    def test_detail_rejected(self, flow):
        """已驳回 — approvalAction=1，含驳回人和驳回原因"""
        fid = flow
        fvid = _snow_id()
        ar_id = _snow_id()
        log_id = _snow_id()
        # 创建已驳回版本
        db(f"INSERT INTO openplatform_v2_cp_flow_version_t (id, flow_id, version_number, status, create_by, last_update_by) VALUES ({fvid}, {fid}, 1, 4, 'tester', 'tester')")
        # 创建审批记录
        db(f"INSERT INTO openplatform_v2_approval_record_t (id, combined_nodes, business_type, business_id, applicant_id, applicant_name, status, current_node, create_by, last_update_by) VALUES ({ar_id}, '[{{\"userId\":\"lisi\",\"userName\":\"李四\",\"order\":1,\"level\":\"global\"}}]', 'connector_flow_version_publish', {fvid}, 'zhangsan', '张三', 2, 0, 'zhangsan', 'zhangsan')")
        # 创建驳回日志
        db(f"INSERT INTO openplatform_v2_approval_log_t (id, record_id, node_index, level, operator_id, operator_name, action, comment, create_by, last_update_by) VALUES ({log_id}, {ar_id}, 0, 'global', 'lisi', '李四', 1, '输入参数缺少必填字段', 'lisi', 'lisi')")
        try:
            resp = api("GET", f"/flows/{fid}/versions/{fvid}")
            assert resp.status_code == 200
            d = resp.json()["data"]
            assert d["status"] == 4
            log = d["latestApprovalLog"]
            assert log["action"] == 1
            assert log["operatorId"] == "lisi"
            assert log["operatorName"] == "李四"
            assert "输入参数缺少必填字段" in (log.get("comment") or "")
            assert log["actionTime"] is not None
        finally:
            db(f"DELETE FROM openplatform_v2_approval_log_t WHERE id = {log_id}")
            db(f"DELETE FROM openplatform_v2_approval_record_t WHERE id = {ar_id}")
            db(f"DELETE FROM openplatform_v2_cp_flow_version_t WHERE id = {fvid}")

    @pytest.mark.L2
    def test_detail_published(self, flow):
        """已发布 — approvalAction=0，含审批通过人"""
        fid = flow
        fvid = _snow_id()
        ar_id = _snow_id()
        log_id = _snow_id()
        # 创建已发布版本
        db(f"INSERT INTO openplatform_v2_cp_flow_version_t (id, flow_id, version_number, status, create_by, last_update_by) VALUES ({fvid}, {fid}, 1, 5, 'tester', 'tester')")
        # 创建审批记录（已通过）
        db(f"INSERT INTO openplatform_v2_approval_record_t (id, combined_nodes, business_type, business_id, applicant_id, applicant_name, status, current_node, create_by, last_update_by) VALUES ({ar_id}, '[{{\"userId\":\"wangwu\",\"userName\":\"王五\",\"order\":1,\"level\":\"global\"}}]', 'connector_flow_version_publish', {fvid}, 'zhangsan', '张三', 1, 1, 'zhangsan', 'zhangsan')")
        # 创建审批通过日志
        db(f"INSERT INTO openplatform_v2_approval_log_t (id, record_id, node_index, level, operator_id, operator_name, action, comment, create_by, last_update_by) VALUES ({log_id}, {ar_id}, 0, 'global', 'wangwu', '王五', 0, '审批通过', 'wangwu', 'wangwu')")
        try:
            resp = api("GET", f"/flows/{fid}/versions/{fvid}")
            assert resp.status_code == 200
            d = resp.json()["data"]
            assert d["status"] == 5
            log = d["latestApprovalLog"]
            assert log["action"] == 0
            assert log["operatorId"] == "wangwu"
            assert log["operatorName"] == "王五"
            assert log["actionTime"] is not None
        finally:
            db(f"DELETE FROM openplatform_v2_approval_log_t WHERE id = {log_id}")
            db(f"DELETE FROM openplatform_v2_approval_record_t WHERE id = {ar_id}")
            db(f"DELETE FROM openplatform_v2_cp_flow_version_t WHERE id = {fvid}")

    @pytest.mark.L2
    def test_detail_withdrawn(self, flow):
        """已撤回 — approvalAction=2，含撤回人"""
        fid = flow
        fvid = _snow_id()
        ar_id = _snow_id()
        log_id = _snow_id()
        # 创建已撤回版本
        db(f"INSERT INTO openplatform_v2_cp_flow_version_t (id, flow_id, version_number, status, create_by, last_update_by) VALUES ({fvid}, {fid}, 1, 3, 'tester', 'tester')")
        # 创建审批记录（已撤销）
        db(f"INSERT INTO openplatform_v2_approval_record_t (id, combined_nodes, business_type, business_id, applicant_id, applicant_name, status, current_node, create_by, last_update_by) VALUES ({ar_id}, '[{{\"userId\":\"lisi\",\"userName\":\"李四\",\"order\":1,\"level\":\"global\"}}]', 'connector_flow_version_publish', {fvid}, 'zhangsan', '张三', 3, 0, 'zhangsan', 'zhangsan')")
        # 创建撤回日志
        db(f"INSERT INTO openplatform_v2_approval_log_t (id, record_id, node_index, level, operator_id, operator_name, action, comment, create_by, last_update_by) VALUES ({log_id}, {ar_id}, 0, 'global', 'zhangsan', '张三', 2, null, 'zhangsan', 'zhangsan')")
        try:
            resp = api("GET", f"/flows/{fid}/versions/{fvid}")
            assert resp.status_code == 200
            d = resp.json()["data"]
            assert d["status"] == 3
            log = d["latestApprovalLog"]
            assert log["action"] == 2
            assert log["operatorId"] == "zhangsan"
            assert log["operatorName"] == "张三"
            assert log["actionTime"] is not None
        finally:
            db(f"DELETE FROM openplatform_v2_approval_log_t WHERE id = {log_id}")
            db(f"DELETE FROM openplatform_v2_approval_record_t WHERE id = {ar_id}")
            db(f"DELETE FROM openplatform_v2_cp_flow_version_t WHERE id = {fvid}")

    @pytest.mark.L4
    def test_not_found(self, flow):
        resp = api("GET", f"/flows/{flow}/versions/999999999999999999")
        assert resp is not None
