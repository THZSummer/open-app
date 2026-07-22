#!/usr/bin/env python3
"""GET /ability/list — 能力列表集成测试

测试等级:
    L0: 冒烟测试 — 基础连通性
    L1: 核心CRUD — 列表增删改查
    L2: 生命周期 — 状态流转 (本次不涉及)
    L4: 边界反向 — 异常/校验/极端场景

验收标准:
    1. 返回数据库中的所有启用能力（不再硬编码排除特定 type）
    2. 根据 hidden 字段决定是否在列表中展示（hidden=1 不出现）
    3. 新增返回 entryUrl/routePath/aliasName/requireRelease/loadType
    4. 已订阅标记逻辑不变
"""
import pytest
from conftest import api


class TestOpenAbilityList:

    # ═══════════════════════════════════════════════════════════
    # L0: 冒烟测试
    # ═══════════════════════════════════════════════════════════

    @pytest.mark.L0
    def test_l0_list_ok(self):
        """L0: 列表接口连通性"""
        resp = api("GET", "/ability/list")
        assert resp is not None, "open-server 未运行"
        assert resp.status_code == 200
        body = resp.json()
        assert body["code"] == "200"

    # ═══════════════════════════════════════════════════════════
    # L1: 列表功能验证
    # ═══════════════════════════════════════════════════════════

    @pytest.mark.L1
    def test_l1_list_returns_data(self):
        """L1: 列表返回启用且未隐藏的能力"""
        resp = api("GET", "/ability/list")
        assert resp is not None
        body = resp.json()
        assert body["code"] == "200"
        data = body.get("data", [])
        assert isinstance(data, list)
        # DB 中有预设能力数据，列表非空
        assert len(data) > 0

    @pytest.mark.L1
    def test_l1_list_contains_new_fields(self):
        """L1: 列表项包含新增 5 字段（有则返回，无则 null）"""
        resp = api("GET", "/ability/list")
        assert resp is not None
        body = resp.json()
        data = body.get("data", [])

        for item in data:
            # 新增字段应存在（可为 null）
            assert "entryUrl" in item, f"entryUrl 字段缺失: {item.get('abilityType')}"
            assert "routePath" in item, f"routePath 字段缺失: {item.get('abilityType')}"
            assert "aliasName" in item, f"aliasName 字段缺失: {item.get('abilityType')}"
            assert "requireRelease" in item, f"requireRelease 字段缺失: {item.get('abilityType')}"
            assert "loadType" in item, f"loadType 字段缺失: {item.get('abilityType')}"

            # 新字段值类型正确
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
    def test_l1_list_backward_compatible(self):
        """L1: 向后兼容 — 原有字段完整且类型不变"""
        resp = api("GET", "/ability/list")
        assert resp is not None
        body = resp.json()
        data = body.get("data", [])

        for item in data:
            # 原有字段必须存在
            assert "abilityId" in item, f"abilityId 缺失: {item}"
            assert "abilityType" in item
            assert "nameCn" in item
            assert "nameEn" in item
            assert "descCn" in item
            assert "descEn" in item
            assert "iconUrl" in item
            assert "diagramUrl" in item
            assert "subscribed" in item
            assert "orderNum" in item

            # 类型正确
            assert isinstance(item["abilityType"], int)
            assert isinstance(item["subscribed"], bool)
            assert isinstance(item["orderNum"], int)

    @pytest.mark.L1
    def test_l1_list_subscribed_mark(self):
        """L1: 已订阅标记正确"""
        resp = api("GET", "/ability/list")
        assert resp is not None
        body = resp.json()
        data = body.get("data", [])
        for item in data:
            assert isinstance(item["subscribed"], bool)

    # ═══════════════════════════════════════════════════════════
    # L4: 边界场景
    # ═══════════════════════════════════════════════════════════

    @pytest.mark.L4
    def test_l4_list_empty_app_id(self):
        """L4: 空 appId 参数应返回错误"""
        # 不传 appId（由 client.api 自动填充默认 appId）
        resp = api("GET", "/ability/list", app_id="")
        if resp is None:
            pytest.skip("open-server 未运行")
        # 空 appId 应直接报错（而不是返回空列表）
        assert resp.status_code != 200 or resp.json().get("code") != "200"

    @pytest.mark.L4
    def test_l4_list_no_hidden_abilities(self):
        """L4: 列表中不应出现 hidden=1 的能力（若 DB 中存在）"""
        from common import db_rows

        # 查询 DB 中 hidden=1 的预设能力
        hidden = db_rows("SELECT ability_type, ability_name_cn FROM openplatform_ability_t WHERE hidden = 1 AND status = 1")
        if not hidden:
            pytest.skip("DB 中无 hidden=1 的能力")

        hidden_types = {r["ability_type"] for r in hidden}

        resp = api("GET", "/ability/list")
        assert resp is not None
        body = resp.json()
        data = body.get("data", [])
        returned_types = {item["abilityType"] for item in data}

        # hidden 类型不在返回列表中
        overlap = hidden_types & returned_types
        assert len(overlap) == 0, f"hidden=1 的能力不应出现在列表中: {overlap}"

    @pytest.mark.L4
    def test_l4_list_custom_types_returned(self):
        """L4: 自定义类型（≥100）正常返回"""
        from common import db_rows

        # 查询 DB 中 status=1, hidden=0 且 ability_type >= 100 的能力
        custom = db_rows(
            "SELECT ability_type, ability_name_cn FROM openplatform_ability_t WHERE status = 1 AND hidden = 0 AND ability_type >= 100"
        )
        if not custom:
            pytest.skip("DB 中无自定义类型的能力")

        custom_types = {r["ability_type"] for r in custom}

        resp = api("GET", "/ability/list")
        assert resp is not None
        body = resp.json()
        data = body.get("data", [])
        returned_types = {item["abilityType"] for item in data}

        # 所有自定义类型都应出现在列表中
        missing = custom_types - returned_types
        assert len(missing) == 0, f"自定义类型未返回: {missing}"

    @pytest.mark.L4
    def test_l4_list_no_hardcoded_type_exclusion(self):
        """L4: 不再硬编码排除 type=6 — type=6 若 hidden=0 则正常返回"""
        from common import db_rows

        # 查询 DB 中 hidden=0 且 type=6 的能力
        type6_visible = db_rows(
            "SELECT ability_type, ability_name_cn FROM openplatform_ability_t WHERE status = 1 AND hidden = 0 AND ability_type = 6"
        )
        if not type6_visible:
            pytest.skip("DB 中无 hidden=0 且 type=6 的能力")

        resp = api("GET", "/ability/list")
        assert resp is not None
        body = resp.json()
        data = body.get("data", [])
        types = {item["abilityType"] for item in data}

        assert 6 in types, "type=6 且 hidden=0 的能力应出现在列表中（硬编码排除已移除）"

    @pytest.mark.L4
    def test_l4_list_new_fields_value_types(self):
        """L4: 新增字段值范围正确"""
        resp = api("GET", "/ability/list")
        assert resp is not None
        body = resp.json()
        data = body.get("data", [])

        for item in data:
            lt = item.get("loadType")
            if lt is not None:
                assert lt in (1, 2), f"loadType 值应为 1 或 2，实际为: {lt}"

            rr = item.get("requireRelease")
            if rr is not None:
                assert rr in (0, 1), f"requireRelease 值应为 0 或 1，实际为: {rr}"
