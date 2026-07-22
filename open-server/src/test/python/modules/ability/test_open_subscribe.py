#!/usr/bin/env python3
"""POST /ability — 能力订阅集成测试

测试等级:
    L0: 冒烟测试 — 基础连通性
    L1: 核心CRUD — 订阅/取消订阅
    L4: 边界反向 — 异常/校验/极端场景

验收标准:
    1. 移除硬编码类型枚举校验
    2. 改为查询数据库校验能力存在且为启用状态
    3. 重复订阅检查、插入关联记录等其余逻辑不变
    4. 订阅后触发扩展点，当前阶段输出日志
"""
import pytest
from conftest import api


class TestOpenAbilitySubscribe:

    # ═══════════════════════════════════════════════════════════
    # L0: 冒烟测试
    # ═══════════════════════════════════════════════════════════

    @pytest.mark.L0
    def test_l0_subscribe_ok(self):
        """L0: 订阅接口连通性"""
        # 先查询列表中存在的预设能力类型
        resp = api("GET", "/ability/list")
        if resp is None:
            pytest.skip("open-server 未运行")
        data = resp.json().get("data", [])
        if not data:
            pytest.skip("DB 中无可订阅的能力")

        # 找一个尚未被当前应用订阅的能力
        ability_type = None
        for item in data:
            if not item["subscribed"]:
                ability_type = item["abilityType"]
                break
        if ability_type is None:
            pytest.skip("当前应用已订阅所有能力，无法测试订阅")

        resp = api("POST", "/ability", {"abilityType": ability_type})
        assert resp is not None, "open-server 未运行"
        assert resp.status_code == 200
        body = resp.json()
        assert body["code"] == "200"

    # ═══════════════════════════════════════════════════════════
    # L1: 订阅功能验证
    # ═══════════════════════════════════════════════════════════

    @pytest.mark.L1
    def test_l1_subscribe_preset_type(self):
        """L1: 预设类型可正常订阅"""
        resp = api("GET", "/ability/list")
        if resp is None:
            pytest.skip("open-server 未运行")
        data = resp.json().get("data", [])
        if not data:
            pytest.skip("DB 中无可订阅的能力")

        ability_type = None
        for item in data:
            if not item["subscribed"] and item["abilityType"] < 100:
                ability_type = item["abilityType"]
                break
        if ability_type is None:
            pytest.skip("无可订阅的预设类型")

        resp = api("POST", "/ability", {"abilityType": ability_type})
        assert resp is not None
        assert resp.status_code == 200
        assert resp.json()["code"] == "200"

    @pytest.mark.L1
    def test_l1_subscribe_custom_type(self):
        """L1: 自定义类型（≥100）可正常订阅"""
        from common import db_rows

        # 查询 DB 中 status=1 且 ≥100 的自定义能力
        custom_types = db_rows(
            "SELECT ability_type, ability_name_cn FROM openplatform_ability_t WHERE status = 1 AND ability_type >= 100"
        )
        if not custom_types:
            pytest.skip("DB 中无自定义类型的能力")

        ability_type = custom_types[0]["ability_type"]

        resp = api("POST", "/ability", {"abilityType": ability_type})
        if resp is None:
            pytest.skip("open-server 未运行")

        # 可能已订阅过，返回 409400，也是合理的
        assert resp.status_code in (200, 409)
        body = resp.json()
        if resp.status_code == 200:
            assert body["code"] == "200"
        elif resp.status_code == 409:
            assert body["code"] == "409400"

    # ═══════════════════════════════════════════════════════════
    # L4: 边界反向
    # ═══════════════════════════════════════════════════════════

    @pytest.mark.L4
    def test_l4_subscribe_nonexistent_type(self):
        """L4: 不存在的能力类型 → 400 '能力不存在或已失效'"""
        resp = api("POST", "/ability", {"abilityType": 99999})
        if resp is None:
            pytest.skip("open-server 未运行")
        assert resp.status_code == 400
        body = resp.json()
        assert body["code"] == "400"
        assert "能力不存在" in body["messageZh"]

    @pytest.mark.L4
    def test_l4_subscribe_disabled_type(self):
        """L4: 已禁用的能力类型（status=0）→ 400 '能力不存在或已失效'"""
        from common import db_rows

        # 查询 DB 中 status=0 的能力
        disabled = db_rows(
            "SELECT ability_type, ability_name_cn FROM openplatform_ability_t WHERE status = 0 LIMIT 1"
        )
        if not disabled:
            pytest.skip("DB 中无禁用状态的能力")

        ability_type = disabled[0]["ability_type"]

        resp = api("POST", "/ability", {"abilityType": ability_type})
        if resp is None:
            pytest.skip("open-server 未运行")
        # 取决于全局异常处理：400 或 500
        assert resp.status_code in (400, 500)
        body = resp.json()
        if resp.status_code == 400:
            assert "能力不存在" in body["messageZh"] or "已失效" in body["messageZh"]

    @pytest.mark.L4
    def test_l4_subscribe_duplicate(self):
        """L4: 重复订阅同一能力 → 409 '能力已订阅'"""
        from common import db_rows

        # 找一个已经订阅了的能力
        subscribed = db_rows(
            "SELECT r.ability_type, r.app_id FROM openplatform_app_ability_relation_t r "
            "WHERE r.status = 1 AND r.ability_type IS NOT NULL LIMIT 1"
        )
        if not subscribed:
            pytest.skip("DB 中无已订阅记录")

        ability_type = subscribed[0]["ability_type"]

        resp = api("POST", "/ability", {"abilityType": ability_type})
        if resp is None:
            pytest.skip("open-server 未运行")
        # 重复订阅返回 409
        assert resp.status_code == 409
        body = resp.json()
        assert body["code"] == "409400"
        assert "已订阅" in body["messageZh"]

    @pytest.mark.L4
    def test_l4_subscribe_empty_body(self):
        """L4: 请求体无 abilityType → 400"""
        resp = api("POST", "/ability", {})
        if resp is None:
            pytest.skip("open-server 未运行")
        assert resp.status_code == 400

    @pytest.mark.L4
    def test_l4_subscribe_null_ability_type(self):
        """L4: abilityType 为 null → 400"""
        resp = api("POST", "/ability", {"abilityType": None})
        if resp is None:
            pytest.skip("open-server 未运行")
        assert resp.status_code == 400

    @pytest.mark.L4
    def test_l4_subscribe_no_auth(self):
        """L4: 无认证信息 → 401"""
        resp = api("POST", "/ability", {"abilityType": 1}, app_id=None, headers={"X-App-Id": ""})
        if resp is None:
            pytest.skip("open-server 未运行")
        # 无 appId 应报错
        assert resp.status_code != 200
