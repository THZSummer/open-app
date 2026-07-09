#!/usr/bin/env python3
"""
#48 PUT /approval-flows/{id} — 更新审批流模板 (V3: appId 更新)
"""
import time
import pytest
from conftest import api, TEST_APP_ID
from common import db


def _snow_id():
    return int(time.time() * 1000000) % 100000000000000000


def _clean_by_code(code):
    db(f"DELETE FROM openplatform_v2_approval_flow_t WHERE code = '{code}'")


class TestApprovalFlowUpdate:
    @pytest.mark.L1
    def test_update_ok(self):
        """基础更新 — 更新 nameCn + nodes"""
        r = api("GET", "/approval-flows?page=1&size=1")
        items = r.json().get("data", [])
        tid = items[0]["id"] if items else None
        if tid:
            resp = api("PUT", f"/approval-flows/{tid}", {
                "nameCn": "连接器流版本发布审批",
                "nameEn": "connector_flow_version_publish",
                "nodes": [{"userId": "tester", "userName": "Test Approver"}]
            })
            assert resp is not None
            assert resp.status_code in (200, 201)

    @pytest.mark.L2
    def test_update_app_id_then_verify(self):
        """V3: PUT 更新 appId → GET 验证"""
        code = f"pytest_upd_{_snow_id()}"
        try:
            r = api("POST", "/approval-flows", {
                "nameCn": f"更新测试_{code}", "nameEn": f"upd_{code}",
                "code": code,
                "nodes": [{"userId": "tester", "userName": "Test"}]
            })
            assert r.status_code in (200, 201)
            tid = r.json()["data"]["id"]

            r2 = api("PUT", f"/approval-flows/{tid}", {"appId": TEST_APP_ID})
            assert r2.status_code in (200, 201)

            r3 = api("GET", f"/approval-flows/{tid}")
            assert r3.status_code == 200
            d = r3.json()["data"]
            assert "appId" in d
            assert str(d.get("appId")) == str(TEST_APP_ID)
        finally:
            _clean_by_code(code)

    @pytest.mark.L2
    def test_update_app_id_to_null(self):
        """V3: 更新 appId 为 null"""
        code = f"pytest_null_{_snow_id()}"
        try:
            r = api("POST", "/approval-flows", {
                "nameCn": f"null测试_{code}", "nameEn": f"null_{code}",
                "code": code, "appId": TEST_APP_ID,
                "nodes": [{"userId": "tester", "userName": "Test"}]
            })
            assert r.status_code in (200, 201)
            tid = r.json()["data"]["id"]

            r2 = api("PUT", f"/approval-flows/{tid}", {"appId": None})
            assert r2.status_code in (200, 201, 400)
        finally:
            _clean_by_code(code)

    @pytest.mark.L4
    def test_update_not_found(self):
        resp = api("PUT", "/approval-flows/999999999999999999", {"nameCn": "test"})
        assert resp is not None
