#!/usr/bin/env python3
"""#17 POST /flows — 创建连接流 (FR-016, plan-api §3.3)"""
import pytest
from common import api
from conftest import assert_operate_log


class TestFlowCreate:
    @pytest.mark.L1
    def test_create_ok(self):
        """plan-api §3.3: 返回 flowId + nameCn + lifecycleStatus=1 + note"""
        body = {"nameCn": "新消息自动通知", "nameEn": "Auto Message Notification",
                "descriptionCn": "收到消息后自动通知", "descriptionEn": "Auto notify on message"}
        resp = api("POST", "/flows", body)
        assert resp.status_code == 200
        data = resp.json()
        assert data["code"] == "200"
        d = data["data"]
        # plan-api 规定 flowId (string 雪花ID)
        assert "flowId" in d, f"Expected flowId in response, got keys: {list(d.keys())}"
        assert isinstance(d["flowId"], str) and len(d["flowId"]) >= 15
        # plan-api 规定 lifecycleStatus=1（已停止）
        assert d["lifecycleStatus"] in (1, "1"), \
            f"Expected lifecycleStatus=1 (已停止), got {d.get('lifecycleStatus')}"
        # plan-api 规定 nameCn/nameEn 应回显
        assert d["nameCn"] == body["nameCn"]
        assert d["nameEn"] == body["nameEn"]
        # plan-api 规定 note 提示手动创建草稿版本
        assert "note" in d
        assert "草稿" in d["note"]

    @pytest.mark.L4
    def test_missing_name_cn(self):
        resp = api("POST", "/flows", {"nameEn": "Test"})
        assert resp.json()["code"] == "400"

    @pytest.mark.L4
    def test_missing_name_en(self):
        resp = api("POST", "/flows", {"nameCn": "测试"})
        assert resp.json()["code"] == "400"

    @pytest.mark.L4
    def test_name_cn_exactly_128(self):
        """边界值：nameCn 恰好 128 字符应创建成功（对齐 DB VARCHAR(128)）"""
        name = "测" * 128
        body = {"nameCn": name, "nameEn": "FlowBoundary128"}
        resp = api("POST", "/flows", body)
        assert resp.status_code == 200
        assert resp.json()["code"] == "200"
        assert resp.json()["data"]["nameCn"] == name

    @pytest.mark.L4
    def test_name_cn_exceed_128(self):
        """边界值：nameCn 129 字符应被拦截（400）"""
        name = "测" * 129
        body = {"nameCn": name, "nameEn": "FlowBoundary129"}
        resp = api("POST", "/flows", body)
        assert resp.json()["code"] == "400"

    @pytest.mark.L4
    def test_name_en_exactly_128(self):
        """边界值：nameEn 恰好 128 字符应创建成功"""
        name = "a" * 128
        body = {"nameCn": "连接流英文名边界128", "nameEn": name}
        resp = api("POST", "/flows", body)
        assert resp.status_code == 200
        assert resp.json()["code"] == "200"
        assert resp.json()["data"]["nameEn"] == name

    @pytest.mark.L4
    def test_name_en_exceed_128(self):
        """边界值：nameEn 129 字符应被拦截（400）"""
        name = "a" * 129
        body = {"nameCn": "连接流英文名边界129", "nameEn": name}
        resp = api("POST", "/flows", body)
        assert resp.json()["code"] == "400"

    @pytest.mark.L1
    def test_create_log(self):
        """创建连接流 → 操作日志"""
        resp = api("POST", "/flows", {"nameCn": "日志测试流", "nameEn": "LogTestFlow"})
        assert resp.status_code == 200
        assert_operate_log("创建连接流:日志测试流")
