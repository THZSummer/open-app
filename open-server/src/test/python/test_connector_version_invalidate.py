#!/usr/bin/env python3
"""#14 PUT /connectors/{id}/versions/{vid}/invalidate — 停用连接器版本

FR-009 (v2.26-draft): 失效校验逻辑放宽 — 仅运行中状态的连接流引用时拦截，
非运行中状态（已停止/已失效）的流引用不拦截。
"""
import pytest
from _client import api, db


class TestConnectorVersionInvalidate:
    @pytest.mark.L2
    def test_invalidate(self, published_connector):
        """FR-009: 已发布→已失效，验证 status 变为 3"""
        cid, vid = published_connector
        # 前置确认：当前为已发布
        resp0 = api("GET", f"/connectors/{cid}/versions/{vid}")
        assert resp0.status_code == 200
        before = resp0.json()["data"]
        assert before.get("status") in (2, "2"), f"Expected status=2 (已发布), got {before.get('status')}"
        # 执行失效
        resp = api("PUT", f"/connectors/{cid}/versions/{vid}/invalidate")
        assert resp.status_code == 200
        assert resp.json()["code"] == "200"
        # 验证状态变更
        resp2 = api("GET", f"/connectors/{cid}/versions/{vid}")
        assert resp2.status_code == 200
        after = resp2.json()["data"]
        assert after.get("status") in (3, "3"), f"Expected status=3 (已失效), got {after.get('status')}"

    @pytest.mark.L2
    def test_invalidate_blocked_by_running_flow(self, published_connector, deployed_flow):
        """运行中流引用时拦截失效（422 + 流名称）"""
        cid, vid = published_connector
        fid, _ = deployed_flow
        # 写入引用关系 + 启动流（lifecycle_status=2 RUNNING）
        ref_id = 9000000000000011
        db(f"INSERT INTO openplatform_v2_cp_connector_version_ref_t "
           f"(id, flow_id, flow_version_id, node_id, connector_id, connector_version_id, create_by, last_update_by) "
           f"VALUES ({ref_id}, {fid}, {fid}, 'conn_1', {cid}, {vid}, 'tester', 'tester')")
        db(f"UPDATE openplatform_v2_cp_flow_t SET lifecycle_status = 2 WHERE id = {fid}")
        try:
            resp = api("PUT", f"/connectors/{cid}/versions/{vid}/invalidate")
            assert resp.status_code == 422
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
        ref_id = 9000000000000012
        db(f"INSERT INTO openplatform_v2_cp_connector_version_ref_t "
           f"(id, flow_id, flow_version_id, node_id, connector_id, connector_version_id, create_by, last_update_by) "
           f"VALUES ({ref_id}, {fid}, {fid}, 'conn_1', {cid}, {vid}, 'tester', 'tester')")
        try:
            resp = api("PUT", f"/connectors/{cid}/versions/{vid}/invalidate")
            assert resp.status_code == 200
            assert resp.json()["code"] == "200"
        finally:
            db(f"DELETE FROM openplatform_v2_cp_connector_version_ref_t WHERE id = {ref_id}")

    @pytest.mark.L2
    def test_invalidate_allowed_with_invalidated_flow(self, published_connector, deployed_flow):
        """已失效流引用时不拦截失效（非运行中放行）"""
        cid, vid = published_connector
        fid, _ = deployed_flow
        # 写入引用关系 + 流置为已失效（lifecycle_status=3 INVALIDATED）
        ref_id = 9000000000000013
        db(f"INSERT INTO openplatform_v2_cp_connector_version_ref_t "
           f"(id, flow_id, flow_version_id, node_id, connector_id, connector_version_id, create_by, last_update_by) "
           f"VALUES ({ref_id}, {fid}, {fid}, 'conn_1', {cid}, {vid}, 'tester', 'tester')")
        db(f"UPDATE openplatform_v2_cp_flow_t SET lifecycle_status = 3 WHERE id = {fid}")
        try:
            resp = api("PUT", f"/connectors/{cid}/versions/{vid}/invalidate")
            assert resp.status_code == 200
            assert resp.json()["code"] == "200"
        finally:
            db(f"DELETE FROM openplatform_v2_cp_connector_version_ref_t WHERE id = {ref_id}")
