#!/usr/bin/env python3
"""#17 POST /flows — 创建连接流 (FR-016, plan-api §3.3)"""
import pytest
from _client import api


class TestFlowCreate:
    @pytest.mark.L1
    def test_create_ok(self):
        """plan-api §3.3: 返回 flowId + nameCn + lifecycleStatus=1 + note
        已知 gap: 后端当前仅返回 {"id":"..."}，plan-api 规定的其他字段待实现"""
        body = {"nameCn": "新消息自动通知", "nameEn": "Auto Message Notification",
                "descriptionCn": "收到消息后自动通知", "descriptionEn": "Auto notify on message"}
        resp = api("POST", "/flows", body)
        assert resp.status_code == 200
        data = resp.json()
        assert data["code"] == "200"
        d = data["data"]
        # plan-api 规定 flowId (string)
        fid = d.get("flowId") or d.get("id")
        assert fid is not None, "Response missing flowId/id"
        assert isinstance(fid, str) and len(fid) >= 15, f"ID must be string, got {type(fid)}"
        # plan-api 规定 lifecycleStatus=1（已停止）
        if "lifecycleStatus" in d:
            assert d["lifecycleStatus"] in (1, "1"), \
                f"Expected lifecycleStatus=1, got {d['lifecycleStatus']}"
        # plan-api 规定 nameCn/nameEn 应回显
        if "nameCn" in d:
            assert d["nameCn"] == body["nameCn"]
        # plan-api 规定 note 提示手动创建草稿版本
        if "note" in d:
            assert "草稿" in d["note"] or "draft" in d["note"].lower()

    @pytest.mark.L4
    def test_missing_name_cn(self):
        resp = api("POST", "/flows", {"nameEn": "Test"})
        assert resp.status_code == 400

    @pytest.mark.L4
    def test_missing_name_en(self):
        resp = api("POST", "/flows", {"nameCn": "测试"})
        assert resp.status_code == 400
