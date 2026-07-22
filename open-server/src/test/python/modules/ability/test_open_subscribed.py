#!/usr/bin/env python3
"""GET /ability/subscribed — 已订阅列表集成测试

测试等级:
    L0: 冒烟测试 — 基础连通性
    L1: 核心CRUD — 已订阅列表
    L4: 边界反向 — 异常/校验/极端场景

验收标准:
    1. 新增返回 entryUrl/routePath/aliasName/requireRelease/loadType
    2. 确保能力名称等完整返回
    3. 其余逻辑不变
    4. type=6 已订阅的不再被过滤
"""
import pytest
from conftest import api


class TestOpenAbilitySubscribed:

    # ═══════════════════════════════════════════════════════════
    # L0: 冒烟测试
    # ═══════════════════════════════════════════════════════════

    @pytest.mark.L0
    def test_l0_subscribed_ok(self):
        """L0: 已订阅列表接口连通性"""
        resp = api("GET", "/ability/subscribed")
        assert resp is not None, "open-server 未运行"
        assert resp.status_code == 200
        body = resp.json()
        assert body["code"] == "200"

    # ═══════════════════════════════════════════════════════════
    # L1: 已订阅列表功能验证
    # ═══════════════════════════════════════════════════════════

    @pytest.mark.L1
    def test_l1_subscribed_returns_data(self):
        """L1: 已订阅列表返回数据"""
        resp = api("GET", "/ability/subscribed")
        if resp is None:
            pytest.skip("open-server 未运行")
        body = resp.json()
        assert body["code"] == "200"
        data = body.get("data", [])
        assert isinstance(data, list)

    @pytest.mark.L1
    def test_l1_subscribed_contains_new_fields(self):
        """L1: 已订阅列表项包含新增 5 字段（有则返回，无则 null）"""
        resp = api("GET", "/ability/subscribed")
        if resp is None:
            pytest.skip("open-server 未运行")
        body = resp.json()
        data = body.get("data", [])

        for item in data:
            assert "entryUrl" in item, f"entryUrl 字段缺失: {item.get('abilityType')}"
            assert "routePath" in item, f"routePath 字段缺失: {item.get('abilityType')}"
            assert "aliasName" in item, f"aliasName 字段缺失: {item.get('abilityType')}"
            assert "requireRelease" in item, f"requireRelease 字段缺失: {item.get('abilityType')}"
            assert "loadType" in item, f"loadType 字段缺失: {item.get('abilityType')}"

            if item["entryUrl"] is not None:
                assert isinstance(item["entryUrl"], str)
            if item["routePath"] is not None:
                assert isinstance(item["routePath"], str)
            if item["aliasName"] is not None:
                assert isinstance(item["aliasName"], str)
            if item["requireRelease"] is not None:
                assert isinstance(item["requireRelease"], int)
            if item["loadType"] is not None:
                assert isinstance(item["loadType"], int)

    @pytest.mark.L1
    def test_l1_subscribed_backward_compatible(self):
        """L1: 向后兼容 — 原有字段完整且类型不变"""
        resp = api("GET", "/ability/subscribed")
        if resp is None:
            pytest.skip("open-server 未运行")
        body = resp.json()
        data = body.get("data", [])

        for item in data:
            assert "id" in item
            assert "abilityId" in item
            assert "abilityType" in item
            assert "nameCn" in item
            assert "nameEn" in item
            assert "iconUrl" in item
            assert "orderNum" in item

            assert isinstance(item["abilityType"], int)
            assert isinstance(item["orderNum"], int)

    # ═══════════════════════════════════════════════════════════
    # L4: 边界场景
    # ═══════════════════════════════════════════════════════════

    @pytest.mark.L4
    def test_l4_subscribed_empty_app_id(self):
        """L4: 空 appId 参数应返回错误"""
        resp = api("GET", "/ability/subscribed", app_id="")
        if resp is None:
            pytest.skip("open-server 未运行")
        assert resp.status_code != 200 or resp.json().get("code") != "200"

    @pytest.mark.L4
    def test_l4_subscribed_type6_included(self):
        """L4: type=6 已订阅的不再被 hidden 影响，应正常返回"""
        from common import db_rows

        subscribed_type6 = db_rows(
            "SELECT r.id, r.ability_type FROM openplatform_app_ability_relation_t r "
            "JOIN openplatform_ability_t a ON r.ability_id = a.id "
            "WHERE r.status = 1 AND a.ability_type = 6 "
            "LIMIT 1"
        )
        if not subscribed_type6:
            pytest.skip("DB 中无已订阅 type=6 的能力")

        resp = api("GET", "/ability/subscribed")
        if resp is None:
            pytest.skip("open-server 未运行")
        body = resp.json()
        data = body.get("data", [])
        types = {item["abilityType"] for item in data}

        assert 6 in types, "type=6 已订阅的能力应出现在已订阅列表中（硬编码排除已移除）"

    @pytest.mark.L4
    def test_l4_subscribed_new_fields_value_types(self):
        """L4: 新增字段值范围正确"""
        resp = api("GET", "/ability/subscribed")
        if resp is None:
            pytest.skip("open-server 未运行")
        body = resp.json()
        data = body.get("data", [])

        for item in data:
            lt = item.get("loadType")
            if lt is not None:
                assert lt in (1, 2), f"loadType 值应为 1 或 2，实际为: {lt}"

            rr = item.get("requireRelease")
            if rr is not None:
                assert rr in (0, 1), f"requireRelease 值应为 0 或 1，实际为: {rr}"
