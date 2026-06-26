#!/usr/bin/env python3
"""#45 GET /approval-flows — 审批流模板列表 (V3: appId 过滤)"""
import time
import pytest
from conftest import api, db, db_val, TEST_APP_ID


def _snow_id():
    return int(time.time() * 1000000) % 100000000000000000


class TestApprovalFlowList:
    @pytest.mark.L1
    def test_list_ok(self):
        resp = api("GET", "/approval-flows")
        assert resp.status_code == 200
        assert resp.json()["code"] == "200"

    @pytest.mark.L1
    def test_list_by_code(self):
        resp = api("GET", "/approval-flows?code=connector_flow_version_publish")
        assert resp.status_code == 200
        assert resp.json()["code"] == "200"

    @pytest.mark.L2
    def test_list_by_app_id(self):
        """V3: 按 ?appId 过滤"""
        resp = api("GET", f"/approval-flows?appId={TEST_APP_ID}")
        assert resp.status_code == 200
        data = resp.json()
        assert data["code"] == "200"
        items = data.get("data", [])
        assert isinstance(items, list)

    @pytest.mark.L2
    def test_list_by_app_id_and_code(self):
        """V3: ?appId + ?code 联合过滤"""
        resp = api("GET", f"/approval-flows?appId={TEST_APP_ID}&code=connector_flow_version_publish")
        assert resp.status_code == 200
        data = resp.json()
        assert data["code"] == "200"
        items = data.get("data", [])
        assert isinstance(items, list)
        # V3: 非目标 code 的项必须是全局模板 (appId=null)，应用级模板必须匹配
        for item in items:
            code = item.get("code", "")
            if code and code != "connector_flow_version_publish":
                assert item.get("appId") is None, \
                    f"联合过滤返回了非目标 code 的应用模板: code={code}, appId={item.get('appId')}"

    @pytest.mark.L2
    def test_list_response_has_app_id_field(self):
        """V3: 列表响应每项必须含 appId 字段"""
        resp = api("GET", "/approval-flows")
        assert resp.status_code == 200
        data = resp.json()
        items = data.get("data", [])
        if items:
            for item in items:
                assert "appId" in item, f"列表项缺少 appId 字段，字段列表: {list(item.keys())}"
