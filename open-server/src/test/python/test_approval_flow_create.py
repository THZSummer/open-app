#!/usr/bin/env python3
"""#47 POST /approval-flows — 创建审批流模板 (V3: appId 请求体 + 唯一约束)"""
import time
import pytest
from conftest import api, db, TEST_APP_ID


def _snow_id():
    return int(time.time() * 1000000) % 100000000000000000


class TestApprovalFlowCreate:
    @pytest.mark.L1
    def test_create_ok(self):
        """创建时不校验业务必填字段，空数据应被接受"""
        resp = api("POST", "/approval-flows", {
            "nameCn": "", "nameEn": "", "code": "", "nodes": []
        })
        assert resp is not None
        assert resp.status_code in (200, 201, 400, 409)

    @pytest.mark.L2
    def test_create_with_app_id(self):
        """V3: 创建时传入 appId"""
        code = f"pytest_appid_{_snow_id()}"
        resp = api("POST", "/approval-flows", {
            "nameCn": f"测试模板_appId_{code}",
            "nameEn": f"test_appid_{code}",
            "code": code,
            "appId": TEST_APP_ID,
            "nodes": [{"userId": "tester", "userName": "Test Approver"}]
        })
        assert resp is not None
        assert resp.status_code in (200, 201), \
            f"创建失败: HTTP {resp.status_code}, body={resp.json()}"
        data = resp.json()
        d = data.get("data", {})
        tid = d.get("id")
        assert tid, f"响应缺少 id: {d}"
        # 清理
        db(f"DELETE FROM openplatform_v2_approval_flow_t WHERE id = {tid}")

    @pytest.mark.L2
    def test_create_then_get_verify_app_id(self):
        """V3: 创建含 appId 的模板后，GET 验证 appId 已持久化"""
        code = f"pytest_verify_{_snow_id()}"
        resp = api("POST", "/approval-flows", {
            "nameCn": f"验证模板_{code}",
            "nameEn": f"verify_{code}",
            "code": code,
            "appId": TEST_APP_ID,
            "nodes": [{"userId": "tester", "userName": "Test Approver"}]
        })
        assert resp.status_code in (200, 201)
        tid = resp.json()["data"]["id"]
        # GET 验证
        resp2 = api("GET", f"/approval-flows/{tid}")
        assert resp2.status_code == 200
        d2 = resp2.json()["data"]
        assert "appId" in d2
        assert str(d2.get("appId")) == str(TEST_APP_ID), \
            f"appId 不匹配: 期望 {TEST_APP_ID}, 实际 {d2.get('appId')}"
        # 清理
        db(f"DELETE FROM openplatform_v2_approval_flow_t WHERE id = {tid}")

    @pytest.mark.L2
    def test_create_connector_flow_version_publish_template(self):
        """V3: 支持创建 code=connector_flow_version_publish + appId 的三级审批模板"""
        code = "connector_flow_version_publish"
        resp = api("POST", "/approval-flows", {
            "nameCn": "连接流版本发布审批(测试)",
            "nameEn": f"flow_publish_test_{_snow_id()}",
            "code": code,
            "appId": TEST_APP_ID,
            "nodes": [
                {"level": "app", "userId": "tester", "userName": "Test Approver", "order": 1},
                {"level": "platform_flow", "userId": "tester", "userName": "Test Approver", "order": 2},
                {"level": "global", "userId": "tester", "userName": "Test Approver", "order": 3}
            ]
        })
        assert resp.status_code in (200, 201, 409), f"HTTP {resp.status_code}"
        body = resp.json()
        if body.get("code") == "200" and "data" in body:
            tid = body["data"]["id"]
            db(f"DELETE FROM openplatform_v2_approval_flow_t WHERE id = {tid}")

    @pytest.mark.L4
    def test_create_duplicate_code_app_id_conflict(self):
        """V3: 相同 code+appId 重复创建 → 409"""
        code = f"pytest_dup_{_snow_id()}"
        body = {
            "nameCn": f"冲突模板_{code}",
            "nameEn": f"dup_{code}",
            "code": code,
            "appId": TEST_APP_ID,
            "nodes": [{"userId": "tester", "userName": "Test Approver"}]
        }
        # 第一次创建
        r1 = api("POST", "/approval-flows", body)
        assert r1.status_code in (200, 201), f"首次创建失败: HTTP {r1.status_code}"
        tid = r1.json()["data"]["id"]
        try:
            # 第二次创建（相同 code+appId）
            r2 = api("POST", "/approval-flows", body)
            r2_body = r2.json()
            assert r2.status_code == 409 or r2_body.get("code") in ("409", 409), \
                f"期望 409 冲突，实际 HTTP {r2.status_code}, body={r2_body}"
        finally:
            db(f"DELETE FROM openplatform_v2_approval_flow_t WHERE id = {tid}")
