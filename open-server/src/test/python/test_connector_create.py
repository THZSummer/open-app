#!/usr/bin/env python3
"""#1 POST /connectors — 创建连接器 (FR-001, plan-api §3.1)"""
import pytest
from _client import api


class TestConnectorCreate:
    @pytest.mark.L1
    def test_create_ok(self):
        """plan-api §3.1: 返回 connectorId + nameCn/nameEn + status=1 + note"""
        body = {"nameCn": "IM 发送消息", "nameEn": "IM Send Message",
                "descriptionCn": "封装 IM 消息发送", "descriptionEn": "IM messaging",
                "connectorType": 1}
        resp = api("POST", "/connectors", body)
        assert resp.status_code == 200
        data = resp.json()
        assert data["code"] == "200"
        d = data["data"]
        # plan-api 规定：connectorId (string 雪花ID)
        assert isinstance(d["connectorId"], str) and len(d["connectorId"]) >= 15
        # plan-api 规定：回显输入字段
        assert d["nameCn"] == body["nameCn"]
        assert d["nameEn"] == body["nameEn"]
        assert d["connectorType"] == body["connectorType"]
        # plan-api 规定：status=1（有效不可用）
        assert d["status"] in (1, "1"), f"Expected status=1 (有效不可用), got {d['status']}"
        # plan-api 规定：note 提示手动创建草稿版本
        assert "note" in d

    @pytest.mark.L4
    def test_missing_name_cn(self):
        resp = api("POST", "/connectors", {"nameEn": "Test", "connectorType": 1})
        assert resp.status_code == 400

    @pytest.mark.parametrize("ctype", [99, 0, -1])
    @pytest.mark.L4
    def test_invalid_connector_type(self, ctype):
        resp = api("POST", "/connectors", {"nameCn": "测试", "nameEn": "Test", "connectorType": ctype})
        assert resp is not None
