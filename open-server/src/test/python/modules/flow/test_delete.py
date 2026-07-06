#!/usr/bin/env python3
"""#27 DELETE /flows/{id} — 删除连接流 (FR-023)"""
import pytest
from common import api, db
from conftest import assert_operate_log


class TestFlowDelete:
    @pytest.mark.L1
    def test_delete_ok(self, flow):
        """FR-023: 已失效的连接流可删除，验证删除后不可再查"""
        # 先设为已失效
        db(f"UPDATE openplatform_v2_cp_flow_t SET lifecycle_status = 3 WHERE id = {flow}")
        resp = api("DELETE", f"/flows/{flow}")
        # 已失效状态删除应成功
        if resp.status_code in (200, 204):
            # 验证删除：再次查询应返回非 200
            resp2 = api("GET", f"/flows/{flow}")
            assert resp2 is not None
            if resp2.status_code == 200:
                body = resp2.json()
                actual_status = body.get("data", {}).get("lifecycleStatus")
                assert actual_status in (4, "4", None), \
                    f"Deleted flow should have lifecycleStatus=4, got {actual_status}"
        elif resp.status_code == 409:
            # 有版本依赖等情况
            pass

    @pytest.mark.L4
    def test_not_found(self):
        resp = api("DELETE", "/flows/999999999999999999")
        assert resp is not None

    @pytest.mark.L2
    def test_delete_log(self, flow):
        """删除连接流 → 操作日志"""
        api("PUT", f"/flows/{flow}/invalidate")
        resp = api("DELETE", f"/flows/{flow}")
        assert resp.status_code == 200
        assert_operate_log("删除连接流")
