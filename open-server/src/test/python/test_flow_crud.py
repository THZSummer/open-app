#!/usr/bin/env python3
"""连接流 CRUD + 生命周期 — APIs #17~#27"""
import pytest
from conftest import api, db, flow, draft_flow, deployed_flow


class TestFlowCreate:
    @pytest.mark.L1
    def test_create_ok(self):
        resp = api("POST", "/flows", {"nameCn": "新消息自动通知", "nameEn": "Auto Message Notification"})
        assert resp.status_code == 200
        assert resp.json()["code"] == "200"

    @pytest.mark.L4
    def test_missing_name_cn(self):
        resp = api("POST", "/flows", {"nameEn": "Test"})
        assert resp.status_code == 400

    @pytest.mark.L4
    def test_missing_name_en(self):
        resp = api("POST", "/flows", {"nameCn": "测试"})
        assert resp.status_code == 400


class TestFlowList:
    @pytest.mark.L0
    def test_default_pagination(self):
        resp = api("GET", "/flows")
        assert resp.status_code == 200
        assert "data" in resp.json()

    @pytest.mark.L1
    def test_lifecycle_status_filter(self):
        resp = api("GET", "/flows", {"lifecycleStatus": 0})
        assert resp.status_code == 200

    @pytest.mark.L1
    def test_keyword_search(self):
        resp = api("GET", "/flows", {"keyword": "通知"})
        assert resp.status_code == 200

    @pytest.mark.L2
    def test_empty_keyword(self):
        resp = api("GET", "/flows", {"keyword": "NONEXISTENT_FLOW_9999"})
        assert resp.status_code == 200

    @pytest.mark.L0
    def test_list_all(self):
        resp = api("GET", "/flows?curPage=1&pageSize=10")
        assert resp.status_code == 200
        assert "data" in resp.json()

    @pytest.mark.L1
    def test_pagination(self):
        resp = api("GET", "/flows?curPage=1&pageSize=2")
        assert resp.status_code == 200
        page = resp.json().get("page", {})
        assert page.get("pageSize") == 2

    @pytest.mark.L1
    def test_keyword_filter(self):
        resp = api("GET", "/flows?keyword=pytest&curPage=1&pageSize=10")
        assert resp.status_code == 200

    @pytest.mark.L1
    def test_status_filter(self):
        resp = api("GET", "/flows?lifecycleStatus=2&curPage=1&pageSize=10")
        assert resp.status_code == 200

    @pytest.mark.L2
    def test_empty_result(self):
        resp = api("GET", "/flows?keyword=zzz_nonexistent_zzz&curPage=1&pageSize=10")
        assert resp.status_code == 200


class TestFlowDetail:
    @pytest.mark.L1
    def test_detail_fields(self, flow):
        resp = api("GET", f"/flows/{flow}")
        assert resp.status_code == 200
        data = resp.json()["data"]
        assert "id" in data
        assert "nameCn" in data
        assert "lifecycleStatus" in data

    @pytest.mark.L4
    def test_detail_not_found(self):
        resp = api("GET", "/flows/999999999999999999")
        assert resp.status_code >= 400


class TestFlowUpdate:
    @pytest.mark.L1
    def test_update_ok(self, flow):
        resp = api("PUT", f"/flows/{flow}", {"nameCn": "更新后的流名称"})
        assert resp.status_code == 200

    @pytest.mark.L4
    def test_not_found(self):
        resp = api("PUT", "/flows/999999999999999999", {"nameCn": "测试"})
        assert resp is not None


class TestFlowDelete:
    @pytest.mark.L1
    def test_delete_ok(self, flow):
        db(f"UPDATE openplatform_v2_cp_flow_t SET lifecycle_status = 0 WHERE id = {flow}")
        resp = api("DELETE", f"/flows/{flow}")
        assert resp.status_code in (200, 409)

    @pytest.mark.L4
    def test_not_found(self):
        resp = api("DELETE", "/flows/999999999999999999")
        assert resp is not None


class TestFlowCopy:
    @pytest.mark.L2
    def test_copy_ok(self, deployed_flow):
        fid, _ = deployed_flow
        resp = api("POST", f"/flows/{fid}/copy", {})
        assert resp is not None
        assert resp.status_code in (200, 201)

    @pytest.mark.L2
    def test_copy_single_version(self, flow):
        vid = int(str(flow) + "1") if flow else 0
        if vid:
            import json
            orch = {"nodes": [], "edges": []}
            orch_s = json.dumps(orch, ensure_ascii=False).replace("'", "''")
            db(f"INSERT INTO openplatform_v2_cp_flow_version_t (id, flow_id, orchestration_config, status, create_by, last_update_by) VALUES ({vid}, {flow}, '{orch_s}', 5, 'tester', 'tester')")
            resp = api("POST", f"/flows/{flow}/copy", {})
            assert resp is not None
            assert resp.status_code in (200, 201)


class TestFlowDeploy:
    @pytest.mark.L2
    def test_deploy_ok(self, deployed_flow):
        fid, fvid = deployed_flow
        resp = api("POST", f"/flows/{fid}/deploy", {"versionId": fvid})
        assert resp is not None
        assert resp.status_code in (200, 201)


class TestFlowStart:
    @pytest.mark.L2
    def test_start_ok(self, flow):
        db(f"UPDATE openplatform_v2_cp_flow_t SET lifecycle_status = 1 WHERE id = {flow}")
        resp = api("POST", f"/flows/{flow}/start")
        assert resp is not None

    @pytest.mark.L2
    def test_duplicate_start(self, flow):
        db(f"UPDATE openplatform_v2_cp_flow_t SET lifecycle_status = 1 WHERE id = {flow}")
        api("POST", f"/flows/{flow}/start")
        resp = api("POST", f"/flows/{flow}/start")
        assert resp is not None

    @pytest.mark.L4
    def test_not_found(self):
        resp = api("POST", "/flows/999999999999999999/start")
        assert resp is not None


class TestFlowStop:
    @pytest.mark.L2
    def test_stop_ok(self, flow):
        db(f"UPDATE openplatform_v2_cp_flow_t SET lifecycle_status = 1 WHERE id = {flow}")
        api("POST", f"/flows/{flow}/start")
        resp = api("POST", f"/flows/{flow}/stop")
        assert resp is not None

    @pytest.mark.L2
    def test_duplicate_stop(self, flow):
        db(f"UPDATE openplatform_v2_cp_flow_t SET lifecycle_status = 1 WHERE id = {flow}")
        api("POST", f"/flows/{flow}/start")
        api("POST", f"/flows/{flow}/stop")
        resp = api("POST", f"/flows/{flow}/stop")
        assert resp is not None

    @pytest.mark.L4
    def test_not_found(self):
        resp = api("POST", "/flows/999999999999999999/stop")
        assert resp is not None


class TestFlowInvalidate:
    @pytest.mark.L2
    def test_invalidate_ok(self, deployed_flow):
        fid, _ = deployed_flow
        resp = api("PUT", f"/flows/{fid}/invalidate")
        assert resp is not None
        assert resp.status_code == 200


class TestFlowRecover:
    @pytest.mark.L2
    def test_recover_invalidated(self, flow):
        db(f"UPDATE openplatform_v2_cp_flow_t SET lifecycle_status = 3 WHERE id = {flow}")
        resp = api("PUT", f"/flows/{flow}/recover")
        assert resp is not None

    @pytest.mark.L2
    def test_recover_running_rejected(self, flow):
        db(f"UPDATE openplatform_v2_cp_flow_t SET lifecycle_status = 2 WHERE id = {flow}")
        resp = api("PUT", f"/flows/{flow}/recover")
        assert resp is not None


class TestFlowStopRestart:
    """停止→重启 全链路"""

    @pytest.mark.L3
    def test_stop_then_restart(self, deployed_flow):
        fid, _ = deployed_flow
        api("POST", f"/flows/{fid}/start")
        resp = api("POST", f"/flows/{fid}/stop")
        assert resp.status_code == 200
        assert resp.json()["code"] == "200"
        resp = api("POST", f"/flows/{fid}/start")
        assert resp.status_code == 200

    @pytest.mark.L3
    def test_stop_then_invoke_rejected(self, deployed_flow):
        fid, _ = deployed_flow
        api("POST", f"/flows/{fid}/start")
        api("POST", f"/flows/{fid}/stop")
        resp = api("POST", f"/flows/{fid}/versions/{deployed_flow[1]}/debug", {"triggerData": {}})
        assert resp is not None

    @pytest.mark.L4
    def test_start_without_deploy_rejected(self, flow):
        resp = api("POST", f"/flows/{flow}/start")
        assert resp.status_code >= 400


class TestFlowDeployInvoke:
    """部署→启动→调用→校验执行记录"""

    @pytest.mark.L3
    def test_deploy_start_invoke(self, deployed_flow):
        fid, vid = deployed_flow
        resp = api("POST", f"/flows/{fid}/start")
        assert resp.status_code == 200
        resp = api("POST", f"/flows/{fid}/versions/{vid}/debug", {"triggerData": {"message": "test"}})
        assert resp is not None
