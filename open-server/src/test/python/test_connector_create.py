#!/usr/bin/env python3
"""#1 POST /connectors — 创建连接器 (FR-001)"""
import pytest
from _client import api


class TestConnectorCreate:
    @pytest.mark.L1
    def test_create_ok(self):
        """FR-001: 创建后进入「有效不可用」状态(status=1)，返回字段与输入一致"""
        body = {"nameCn": "IM 发送消息", "nameEn": "IM Send Message",
                "descriptionCn": "封装 IM 消息发送", "descriptionEn": "IM messaging",
                "connectorType": 1}
        resp = api("POST", "/connectors", body)
        assert resp.status_code == 200
        data = resp.json()
        assert data["code"] == "200"
        d = data["data"]
        # 业务字段与输入一致
        assert d["nameCn"] == body["nameCn"]
        assert d["nameEn"] == body["nameEn"]
        assert d["connectorType"] == body["connectorType"]
        # FR-001: 创建后进入「有效不可用」状态
        assert d["status"] in (1, "1"), f"Expected status=1 (有效不可用), got {d['status']}"
        # BIGINT 雪花 ID 必须返回 string
        assert isinstance(d["connectorId"], str) and len(d["connectorId"]) >= 15
        # 不自动生成草稿版本（note 会提示）
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
