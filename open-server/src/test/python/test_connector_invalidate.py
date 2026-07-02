#!/usr/bin/env python3
"""#5 PUT /connectors/{id}/invalidate — 停用连接器

FR-003 (v2.26-draft): 失效校验逻辑放宽 — 仅运行中状态的连接流引用时拦截，
非运行中状态（已停止/已失效）的流引用不拦截。
"""
import pytest
from _client import api, db


class TestConnectorInvalidate:
    @pytest.mark.L2
    def test_invalidate(self, published_connector):
        """无引用时正常失效"""
        cid, _ = published_connector
        resp = api("PUT", f"/connectors/{cid}/invalidate")
        assert resp.status_code == 200

    @pytest.mark.L2
    def test_invalidate_blocked_by_running_flow(self, published_connector, deployed_flow):
        """运行中流引用时拦截失效（422 + 流名称）"""
        cid, vid = published_connector
        fid, _ = deployed_flow
        # 写入引用关系 + 启动流（lifecycle_status=2 RUNNING）
        ref_id = 9000000000000001
        db(f"INSERT INTO openplatform_v2_cp_connector_version_ref_t "
           f"(id, flow_id, flow_version_id, node_id, connector_id, connector_version_id, create_by, last_update_by) "
           f"VALUES ({ref_id}, {fid}, {fid}, 'conn_1', {cid}, {vid}, 'tester', 'tester')")
        db(f"UPDATE openplatform_v2_cp_flow_t SET lifecycle_status = 2 WHERE id = {fid}")
        try:
            resp = api("PUT", f"/connectors/{cid}/invalidate")
            assert resp.json()["code"] == "422"
            body = resp.json()
            assert "运行中的连接流" in body.get("message", "") or "运行中" in body.get("messageZh", "")
        finally:
            db(f"DELETE FROM openplatform_v2_cp_connector_version_ref_t WHERE id = {ref_id}")

    @pytest.mark.L2
    def test_invalidate_allowed_with_stopped_flow(self, published_connector, deployed_flow):
        """已停止流引用时不拦截失效（非运行中放行）"""
        cid, vid = published_connector
        fid, _ = deployed_flow
        # 写入引用关系，流保持已停止状态（lifecycle_status=1 STOPPED）
        ref_id = 9000000000000002
        db(f"INSERT INTO openplatform_v2_cp_connector_version_ref_t "
           f"(id, flow_id, flow_version_id, node_id, connector_id, connector_version_id, create_by, last_update_by) "
           f"VALUES ({ref_id}, {fid}, {fid}, 'conn_1', {cid}, {vid}, 'tester', 'tester')")
        try:
            resp = api("PUT", f"/connectors/{cid}/invalidate")
            assert resp.status_code == 200
        finally:
            db(f"DELETE FROM openplatform_v2_cp_connector_version_ref_t WHERE id = {ref_id}")

    @pytest.mark.L2
    def test_invalidate_allowed_with_invalidated_flow(self, published_connector, deployed_flow):
        """已失效流引用时不拦截失效（非运行中放行）"""
        cid, vid = published_connector
        fid, _ = deployed_flow
        # 写入引用关系 + 流置为已失效（lifecycle_status=3 INVALIDATED）
        ref_id = 9000000000000003
        db(f"INSERT INTO openplatform_v2_cp_connector_version_ref_t "
           f"(id, flow_id, flow_version_id, node_id, connector_id, connector_version_id, create_by, last_update_by) "
           f"VALUES ({ref_id}, {fid}, {fid}, 'conn_1', {cid}, {vid}, 'tester', 'tester')")
        db(f"UPDATE openplatform_v2_cp_flow_t SET lifecycle_status = 3 WHERE id = {fid}")
        try:
            resp = api("PUT", f"/connectors/{cid}/invalidate")
            assert resp.status_code == 200
        finally:
            db(f"DELETE FROM openplatform_v2_cp_connector_version_ref_t WHERE id = {ref_id}")
