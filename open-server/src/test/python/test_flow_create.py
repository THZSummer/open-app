#!/usr/bin/env python3
"""#17 POST /flows — 创建连接流 (FR-016)"""
import pytest
from _client import api, db


class TestFlowCreate:
    @pytest.mark.L1
    def test_create_ok(self):
        """FR-016: 创建后 lifecycleStatus=1(已停止)，返回字段与输入一致"""
        body = {"nameCn": "新消息自动通知", "nameEn": "Auto Message Notification",
                "descriptionCn": "收到消息后自动通知", "descriptionEn": "Auto notify on message"}
        resp = api("POST", "/flows", body)
        assert resp.status_code == 200
        data = resp.json()
        assert data["code"] == "200"
        d = data["data"]
        # 创建接口只返回 id
        fid = d.get("id")
        assert isinstance(fid, str) and len(fid) >= 15
        # 通过 GET 验证完整字段
        resp2 = api("GET", f"/flows/{fid}")
        assert resp2.status_code == 200
        detail = resp2.json()["data"]
        assert detail["nameCn"] == body["nameCn"]
        assert detail["nameEn"] == body["nameEn"]
        # FR-016: 创建后 lifecycleStatus=1 (已停止)
        assert detail["lifecycleStatus"] in (1, "1"), \
            f"Expected lifecycleStatus=1, got {detail['lifecycleStatus']}"
        # 清理
        db(f"DELETE FROM openplatform_v2_cp_flow_version_t WHERE flow_id = {fid}")
        db(f"DELETE FROM openplatform_v2_cp_connector_version_ref_t WHERE flow_id = {fid}")
        db(f"DELETE FROM openplatform_v2_cp_flow_t WHERE id = {fid}")

    @pytest.mark.L4
    def test_missing_name_cn(self):
        resp = api("POST", "/flows", {"nameEn": "Test"})
        assert resp.status_code == 400

    @pytest.mark.L4
    def test_missing_name_en(self):
        resp = api("POST", "/flows", {"nameCn": "测试"})
        assert resp.status_code == 400
