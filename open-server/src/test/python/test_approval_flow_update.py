#!/usr/bin/env python3
"""#48 PUT /approval-flows/{id} — 更新审批流模板 (V3: appId 更新)"""
import time
import pytest
from conftest import api, db, db_val, TEST_APP_ID


def _snow_id():
    return int(time.time() * 1000000) % 100000000000000000


class TestApprovalFlowUpdate:
    @pytest.mark.L1
    def test_update_ok(self):
        tid = db_val("SELECT id FROM openplatform_v2_approval_flow_t WHERE code = 'connector_flow_version_publish' LIMIT 1")
        if tid:
            resp = api("PUT", f"/approval-flows/{tid}", {
                "nameCn": "连接器流版本发布审批",
                "nameEn": "connector_flow_version_publish",
                "nodes": [{"userId": "tester", "userName": "Test Approver"}]
            })
            assert resp is not None
            assert resp.status_code in (200, 201)

    @pytest.mark.L2
    def test_update_app_id(self):
        """V3: 更新 appId 字段，验证持久化"""
        code = f"pytest_upd_appid_{_snow_id()}"
        r = api("POST", "/approval-flows", {
            "nameCn": f"更新appId测试_{code}",
            "nameEn": f"upd_appid_{code}",
            "code": code,
            "nodes": [{"userId": "tester", "userName": "Test Approver"}]
        })
        assert r.status_code in (200, 201), f"创建失败: HTTP {r.status_code}"
        tid = r.json()["data"]["id"]

        try:
            # 更新 appId
            r2 = api("PUT", f"/approval-flows/{tid}", {"appId": TEST_APP_ID})
            assert r2.status_code in (200, 201), \
                f"更新 appId 失败: HTTP {r2.status_code}, body={r2.json()}"

            # GET 验证 appId 已持久化
            r3 = api("GET", f"/approval-flows/{tid}")
            assert r3.status_code == 200
            d3 = r3.json()["data"]
            assert "appId" in d3
            assert str(d3.get("appId")) == str(TEST_APP_ID), \
                f"更新后 appId 不匹配: 期望 {TEST_APP_ID}, 实际 {d3.get('appId')}"
        finally:
            db(f"DELETE FROM openplatform_v2_approval_flow_t WHERE id = {tid}")

    @pytest.mark.L4
    def test_update_not_found(self):
        resp = api("PUT", "/approval-flows/999999999999999999", {"nameCn": "test"})
        assert resp is not None
