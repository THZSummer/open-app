#!/usr/bin/env python3
"""#46 GET /approval-flows/{id} — 审批流模板详情 (V3: 响应含 appId)"""
import time
import pytest
from conftest import api, db, db_val, TEST_APP_ID


def _snow_id():
    return int(time.time() * 1000000) % 100000000000000000


class TestApprovalFlowDetail:
    @pytest.mark.L1
    def test_detail_ok(self):
        """V3: 详情响应必须包含 appId 字段"""
        tid = db_val("SELECT id FROM openplatform_v2_approval_flow_t WHERE code = 'connector_flow_version_publish' LIMIT 1")
        if tid:
            resp = api("GET", f"/approval-flows/{tid}")
            assert resp.status_code == 200
            data = resp.json()
            assert data["code"] == "200"
            d = data.get("data", {})
            assert "appId" in d, f"详情响应缺少 appId 字段，现有字段: {list(d.keys())}"

    @pytest.mark.L2
    def test_detail_app_id_value(self):
        """V3: 创建含 appId 的模板后 GET 验证 appId 值正确"""
        code = f"pytest_detail_appid_{_snow_id()}"
        r = api("POST", "/approval-flows", {
            "nameCn": f"详情验证_{code}",
            "nameEn": f"detail_appid_{code}",
            "code": code,
            "appId": TEST_APP_ID,
            "nodes": [{"userId": "tester", "userName": "Test Approver"}]
        })
        assert r.status_code in (200, 201), f"创建失败: HTTP {r.status_code}"
        tid = r.json()["data"]["id"]

        resp = api("GET", f"/approval-flows/{tid}")
        assert resp.status_code == 200
        d = resp.json()["data"]
        assert "appId" in d
        assert str(d.get("appId")) == str(TEST_APP_ID), \
            f"appId 不匹配: 期望 {TEST_APP_ID}, 实际 {d.get('appId')}"
        # 清理
        db(f"DELETE FROM openplatform_v2_approval_flow_t WHERE id = {tid}")

    @pytest.mark.L4
    def test_detail_not_found(self):
        resp = api("GET", "/approval-flows/999999999999999999")
        assert resp is not None
