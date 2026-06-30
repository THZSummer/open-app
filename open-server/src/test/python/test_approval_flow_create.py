#!/usr/bin/env python3
"""
#47 POST /approval-flows — 创建审批流模板 (V3: appId 请求体 + code+appId 唯一约束)

V3 修复后:
  - GAP-2 已修复: countByCodeAndAppId(code, appId) 替代 countByCode(code)
  - 同 code 不同 appId 可共存（对齐 DB uk_code_app）
  - appId 非空时校验应用存在性（AppMapper.selectById）
"""
import time
import pytest
from conftest import api, db, TEST_APP_ID, INTERNAL_APP_ID


def _snow_id():
    return int(time.time() * 1000000) % 100000000000000000


class TestApprovalFlowCreate:
    # ═══════════════════════════════════════════════════════
    # L1: 基础创建
    # ═══════════════════════════════════════════════════════

    @pytest.mark.L1
    def test_create_ok(self):
        """空 body — 校验不强制必填字段的容错"""
        resp = api("POST", "/approval-flows", {
            "nameCn": "", "nameEn": "", "code": "", "nodes": []
        })
        assert resp is not None
        assert resp.status_code in (200, 201, 400, 409)

    # ═══════════════════════════════════════════════════════
    # L2: appId 创建 + 持久化验证
    # ═══════════════════════════════════════════════════════

    @pytest.mark.L2
    def test_create_with_app_id(self):
        """V3: 创建含 appId 的模板 → 应成功"""
        code = f"pytest_appid_{_snow_id()}"
        resp = api("POST", "/approval-flows", {
            "nameCn": f"测试_{code}", "nameEn": f"test_{code}",
            "code": code, "appId": INTERNAL_APP_ID,
            "nodes": [{"userId": "tester", "userName": "Test Approver"}]
        })
        assert resp.status_code in (200, 201), \
            f"创建失败: HTTP {resp.status_code}, body={resp.json()}"
        tid = resp.json()["data"]["id"]
        assert tid
        db(f"DELETE FROM openplatform_v2_approval_flow_t WHERE id = {tid}")

    @pytest.mark.L2
    def test_create_then_get_verify_app_id_persisted(self):
        """V3: 创建 → GET 验证 appId 值已持久化到 DB"""
        code = f"pytest_verify_{_snow_id()}"
        resp = api("POST", "/approval-flows", {
            "nameCn": f"验证_{code}", "nameEn": f"verify_{code}",
            "code": code, "appId": INTERNAL_APP_ID,
            "nodes": [{"userId": "tester", "userName": "Test Approver"}]
        })
        assert resp.status_code in (200, 201)
        tid = resp.json()["data"]["id"]

        resp2 = api("GET", f"/approval-flows/{tid}")
        assert resp2.status_code == 200
        d = resp2.json()["data"]
        assert "appId" in d
        assert str(d.get("appId")) == str(INTERNAL_APP_ID), \
            f"appId 不匹配: 期望 {INTERNAL_APP_ID}, 实际 {d.get('appId')}"
        db(f"DELETE FROM openplatform_v2_approval_flow_t WHERE id = {tid}")

    # ═══════════════════════════════════════════════════════
    # L2: 同 code 不同 appId 共存（GAP-2 已修复）
    # ═══════════════════════════════════════════════════════

    @pytest.mark.L2
    def test_create_same_code_different_appid_can_coexist(self):
        """V3 修复: code=X+appId=NULL 和 code=X+appId=具体值 可共存"""
        code = f"pytest_coexist_{_snow_id()}"

        # Step 1: 创建全局模板 (appId 不传 → app_id=NULL)
        r1 = api("POST", "/approval-flows", {
            "nameCn": f"全局_{code}", "nameEn": f"global_{code}",
            "code": code, "nodes": [{"userId": "tester", "userName": "Test"}]
        })
        assert r1.status_code in (200, 201), f"创建全局模板失败: HTTP {r1.status_code}"
        tid1 = r1.json()["data"]["id"]

        try:
            # Step 2: 创建同 code 但不同 appId 的模板 — 必须成功
            r2 = api("POST", "/approval-flows", {
                "nameCn": f"应用_{code}", "nameEn": f"app_{code}",
                "code": code, "appId": INTERNAL_APP_ID,
                "nodes": [{"userId": "tester", "userName": "Test"}]
            })
            assert r2.status_code in (200, 201), \
                f"GAP-2 未修复: 同 code 不同 appId 应可共存，实际被拦截: HTTP {r2.status_code}, body={r2.json()}"
            assert r2.json().get("code") == "200"
            tid2 = r2.json()["data"]["id"]
            db(f"DELETE FROM openplatform_v2_approval_flow_t WHERE id = {tid2}")
        finally:
            db(f"DELETE FROM openplatform_v2_approval_flow_t WHERE id = {tid1}")

    # ═══════════════════════════════════════════════════════
    # L4: 真正重复的冲突检测
    # ═══════════════════════════════════════════════════════

    @pytest.mark.L4
    def test_create_duplicate_code_and_appid_conflict(self):
        """V3: 相同 code+appId 重复创建 → 409"""
        code = f"pytest_dup_{_snow_id()}"
        body = {
            "nameCn": f"冲突_{code}", "nameEn": f"dup_{code}",
            "code": code, "appId": INTERNAL_APP_ID,
            "nodes": [{"userId": "tester", "userName": "Test"}]
        }
        r1 = api("POST", "/approval-flows", body)
        assert r1.status_code in (200, 201), f"首次创建失败: HTTP {r1.status_code}"
        tid = r1.json()["data"]["id"]
        try:
            r2 = api("POST", "/approval-flows", body)
            r2_body = r2.json()
            assert r2.status_code == 409 or str(r2_body.get("code")) == "409", \
                f"期望冲突，实际 HTTP {r2.status_code}, body={r2_body}"
        finally:
            db(f"DELETE FROM openplatform_v2_approval_flow_t WHERE id = {tid}")

    @pytest.mark.L4
    def test_create_duplicate_code_both_null_conflict(self):
        """V3: 相同 code + appId=NULL 重复创建 → 409"""
        code = f"pytest_dup_null_{_snow_id()}"
        body = {
            "nameCn": f"全局冲突_{code}", "nameEn": f"dup_null_{code}",
            "code": code,
            "nodes": [{"userId": "tester", "userName": "Test"}]
        }
        r1 = api("POST", "/approval-flows", body)
        assert r1.status_code in (200, 201), f"首次创建失败: HTTP {r1.status_code}"
        tid = r1.json()["data"]["id"]
        try:
            r2 = api("POST", "/approval-flows", body)
            r2_body = r2.json()
            assert r2.status_code == 409 or str(r2_body.get("code")) == "409", \
                f"期望冲突，实际 HTTP {r2.status_code}, body={r2_body}"
        finally:
            db(f"DELETE FROM openplatform_v2_approval_flow_t WHERE id = {tid}")

    # ═══════════════════════════════════════════════════════
    # L4: appId 存在性校验
    # ═══════════════════════════════════════════════════════

    @pytest.mark.L4
    def test_create_with_nonexistent_appid_rejected(self):
        """V3: appId 不存在 → 404 拒绝"""
        code = f"pytest_badapp_{_snow_id()}"
        resp = api("POST", "/approval-flows", {
            "nameCn": f"不存在应用_{code}", "nameEn": f"badapp_{code}",
            "code": code, "appId": 999999999999999999,
            "nodes": [{"userId": "tester", "userName": "Test"}]
        })
        body = resp.json()
        assert resp.status_code == 404 or str(body.get("code")) == "404", \
            f"期望 404 应用不存在，实际: HTTP {resp.status_code}, body={body}"
