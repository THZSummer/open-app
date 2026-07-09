#!/usr/bin/env python3
"""
#45 GET /approval-flows — 审批流模板列表 (V3: appId 过滤)

V3 修复后:
  - GAP-1 已修复: ?appId 参数正确传递到查询
  - appId 非空时仅返回该应用的模板
  - appId 不传时返回全部模板
"""
import time
import pytest
from conftest import api, TEST_APP_ID
from common import db


def _snow_id():
    return int(time.time() * 1000000) % 100000000000000000


def _clean_by_code(code):
    db(f"DELETE FROM openplatform_v2_approval_flow_t WHERE code = '{code}'")


class TestApprovalFlowList:
    @pytest.mark.L1
    def test_list_ok(self):
        """基础列表查询"""
        resp = api("GET", "/approval-flows")
        assert resp.status_code == 200
        data = resp.json()
        assert data["code"] == "200"
        assert isinstance(data.get("data"), list)

    @pytest.mark.L1
    def test_list_by_keyword(self):
        """?keyword 按名称/编码模糊搜索"""
        resp = api("GET", "/approval-flows?keyword=connector_flow_version_publish")
        assert resp.status_code == 200
        data = resp.json()
        assert data["code"] == "200"
        items = data.get("data", [])
        assert isinstance(items, list)
        if items:
            codes = [it.get("code") for it in items]
            assert any("connector_flow_version_publish" in (c or "") for c in codes), \
                f"keyword 过滤未返回预期模板: codes={codes}"

    @pytest.mark.L2
    def test_list_response_has_app_id_field(self):
        """V3: 列表响应每项必须含 appId 字段"""
        resp = api("GET", "/approval-flows")
        assert resp.status_code == 200
        items = resp.json().get("data", [])
        if items:
            for item in items:
                assert "appId" in item, \
                    f"列表项缺少 appId 字段，字段列表: {list(item.keys())}"

    @pytest.mark.L2
    def test_list_by_appid_filters_correctly(self):
        """V3 修复: ?appId=xxx 仅返回该应用的模板，不返回全局模板"""
        code = f"pytest_list_appid_{_snow_id()}"
        try:
            r1 = api("POST", "/approval-flows", {
                "nameCn": f"全局_{code}", "nameEn": f"global_{code}",
                "code": code, "nodes": [{"userId": "tester", "userName": "Test"}]
            })
            assert r1.status_code in (200, 201)

            r2 = api("POST", "/approval-flows", {
                "nameCn": f"应用_{code}", "nameEn": f"app_{code}",
                "code": code, "appId": TEST_APP_ID,
                "nodes": [{"userId": "tester", "userName": "Test"}]
            })
            assert r2.status_code in (200, 201), f"创建应用模板失败: {r2.json()}"

            resp = api("GET", f"/approval-flows?appId={TEST_APP_ID}")
            assert resp.status_code == 200
            items = resp.json().get("data", [])

            global_in_filtered = any(
                it.get("code") == code and it.get("appId") is None
                for it in items
            )
            assert not global_in_filtered, \
                f"GAP-1 未修复: ?appId 过滤仍返回全局模板 (code={code})"

            app_in_filtered = any(
                it.get("code") == code and str(it.get("appId")) == str(TEST_APP_ID)
                for it in items
            )
            assert app_in_filtered, \
                f"?appId={TEST_APP_ID} 过滤结果中未找到应用模板 (code={code})"
        finally:
            _clean_by_code(code)
