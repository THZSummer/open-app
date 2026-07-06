#!/usr/bin/env python3
"""#23 POST /flows/{id}/start — 启动连接流 (FR-019, FR-046)

V3 新增校验：
  - 已部署版本状态必须为已发布(5)
  - 引用的连接器版本必须为已发布(2)
  - 引用的连接器本身不能为已失效(3)
"""
import time
import pytest
from conftest import api, db, db_val, TEST_APP_ID, INTERNAL_APP_ID


def _snow_id():
    return int(time.time() * 1000000) % 100000000000000000


class TestFlowStart:
    @pytest.mark.L2
    def test_start(self, deployed_flow):
        """FR-019: 已停止→运行中"""
        fid, _ = deployed_flow
        resp = api("POST", f"/flows/{fid}/start")
        assert resp.status_code == 200
        data = resp.json()
        assert data["code"] == "200"
        if "data" in data and data["data"] is not None:
            d = data["data"]
            assert d["lifecycleStatus"] in (2, "2"), \
                f"Expected running(2), got {d.get('lifecycleStatus')}"

    @pytest.mark.L2
    def test_start_without_deploy(self, flow):
        """无部署版本时启动应被拒绝"""
        resp = api("POST", f"/flows/{flow}/start")
        assert resp.json()["code"] not in ("200",)

    @pytest.mark.L4
    def test_start_nonexistent(self):
        resp = api("POST", "/flows/999999999999999999/start")
        assert resp is not None

    @pytest.mark.L2
    def test_start_blocked_when_deployed_version_invalidated(self, flow):
        """已部署版本被失效后启动应被拦截（422）"""
        fid = flow
        # 创建已发布版本 + 部署
        fvid = _snow_id()
        db(f"INSERT INTO openplatform_v2_cp_flow_version_t (id, flow_id, version_number, status, orchestration_config, create_by, last_update_by) VALUES ({fvid}, {fid}, 1, 5, '{{\"nodes\":[],\"edges\":[]}}', 'tester', 'tester')")
        db(f"UPDATE openplatform_v2_cp_flow_t SET deployed_version_id = {fvid}, deployed_version_number = 1 WHERE id = {fid}")
        # 失效版本
        db(f"UPDATE openplatform_v2_cp_flow_version_t SET status = 6 WHERE id = {fvid}")
        try:
            resp = api("POST", f"/flows/{fid}/start")
            body = resp.json()
            assert resp.status_code == 422 or str(body.get("code")) == "422", \
                f"已失效版本应拦截启动，实际: {resp.status_code} {body}"
            assert "已失效" in (body.get("messageZh", "") or body.get("message", "")), \
                f"提示应含'已失效'，实际: {body}"
        finally:
            db(f"DELETE FROM openplatform_v2_cp_flow_version_t WHERE id = {fvid}")

    @pytest.mark.L2
    def test_start_blocked_when_connector_version_invalidated(self, flow):
        """引用的连接器版本被失效后启动应被拦截（422）"""
        fid = flow
        # 创建连接器 + 已发布版本
        cid = _snow_id()
        cv_id = _snow_id()
        db(f"INSERT INTO openplatform_v2_cp_connector_t (id, name_cn, name_en, connector_type, app_id, status, create_by, last_update_by) VALUES ({cid}, '测试连接器', 'test_conn', 1, {INTERNAL_APP_ID}, 2, 'tester', 'tester')")
        db(f"INSERT INTO openplatform_v2_cp_connector_version_t (id, connector_id, version_number, status, connection_config, create_by, last_update_by) VALUES ({cv_id}, {cid}, 1, 2, '{{\"protocol\":\"HTTP\"}}', 'tester', 'tester')")
        # 创建已发布连接流版本 + 部署 + 引用连接器版本
        fvid = _snow_id()
        db(f"INSERT INTO openplatform_v2_cp_flow_version_t (id, flow_id, version_number, status, orchestration_config, create_by, last_update_by) VALUES ({fvid}, {fid}, 1, 5, '{{\"nodes\":[]}}', 'tester', 'tester')")
        db(f"UPDATE openplatform_v2_cp_flow_t SET deployed_version_id = {fvid}, deployed_version_number = 1 WHERE id = {fid}")
        ref_id = _snow_id()
        db(f"INSERT INTO openplatform_v2_cp_connector_version_ref_t (id, flow_id, flow_version_id, node_id, connector_id, connector_version_id, create_by, last_update_by) VALUES ({ref_id}, {fid}, {fvid}, 'conn_1', {cid}, {cv_id}, 'tester', 'tester')")
        # 失效连接器版本
        db(f"UPDATE openplatform_v2_cp_connector_version_t SET status = 3 WHERE id = {cv_id}")
        try:
            resp = api("POST", f"/flows/{fid}/start")
            body = resp.json()
            assert resp.status_code == 422 or str(body.get("code")) == "422", \
                f"连接器版本已失效应拦截启动，实际: {resp.status_code} {body}"
            assert "连接器版本" in (body.get("messageZh", "") or body.get("message", "")), \
                f"提示应含'连接器版本'，实际: {body}"
        finally:
            db(f"DELETE FROM openplatform_v2_cp_connector_version_ref_t WHERE id = {ref_id}")
            db(f"DELETE FROM openplatform_v2_cp_flow_version_t WHERE id = {fvid}")
            db(f"DELETE FROM openplatform_v2_cp_connector_version_t WHERE id = {cv_id}")
            db(f"DELETE FROM openplatform_v2_cp_connector_t WHERE id = {cid}")

    @pytest.mark.L2
    def test_start_blocked_when_connector_invalidated(self, flow):
        """引用的连接器本身被失效后启动应被拦截（422）"""
        fid = flow
        # 创建连接器 + 已发布版本
        cid = _snow_id()
        cv_id = _snow_id()
        db(f"INSERT INTO openplatform_v2_cp_connector_t (id, name_cn, name_en, connector_type, app_id, status, create_by, last_update_by) VALUES ({cid}, '失效连接器测试', 'invalidated_conn_test', 1, {INTERNAL_APP_ID}, 2, 'tester', 'tester')")
        db(f"INSERT INTO openplatform_v2_cp_connector_version_t (id, connector_id, version_number, status, connection_config, create_by, last_update_by) VALUES ({cv_id}, {cid}, 1, 2, '{{\"protocol\":\"HTTP\"}}', 'tester', 'tester')")
        # 创建已发布连接流版本 + 部署 + 引用连接器版本
        fvid = _snow_id()
        db(f"INSERT INTO openplatform_v2_cp_flow_version_t (id, flow_id, version_number, status, orchestration_config, create_by, last_update_by) VALUES ({fvid}, {fid}, 1, 5, '{{\"nodes\":[]}}', 'tester', 'tester')")
        db(f"UPDATE openplatform_v2_cp_flow_t SET deployed_version_id = {fvid}, deployed_version_number = 1 WHERE id = {fid}")
        ref_id = _snow_id()
        db(f"INSERT INTO openplatform_v2_cp_connector_version_ref_t (id, flow_id, flow_version_id, node_id, connector_id, connector_version_id, create_by, last_update_by) VALUES ({ref_id}, {fid}, {fvid}, 'conn_1', {cid}, {cv_id}, 'tester', 'tester')")
        # 失效连接器本身（状态改为3）
        db(f"UPDATE openplatform_v2_cp_connector_t SET status = 3 WHERE id = {cid}")
        try:
            resp = api("POST", f"/flows/{fid}/start")
            body = resp.json()
            assert resp.status_code == 422 or str(body.get("code")) == "422", \
                f"连接器已失效应拦截启动，实际: {resp.status_code} {body}"
            assert "连接器" in (body.get("messageZh", "") or body.get("message", "")), \
                f"提示应含'连接器'，实际: {body}"
        finally:
            db(f"DELETE FROM openplatform_v2_cp_connector_version_ref_t WHERE id = {ref_id}")
            db(f"DELETE FROM openplatform_v2_cp_flow_version_t WHERE id = {fvid}")
            db(f"DELETE FROM openplatform_v2_cp_connector_version_t WHERE id = {cv_id}")
            db(f"DELETE FROM openplatform_v2_cp_connector_t WHERE id = {cid}")
