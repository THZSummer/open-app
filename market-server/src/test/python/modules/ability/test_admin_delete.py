"""
能力目录管理删除接口集成测试

覆盖场景：
    L1 - 正常流程（1 个）：正常删除能力及其属性
    L2 - 业务规则（1 个）：含属性表的删除，验证属性表同时被清理
    L4 - 边界/反向（1 个）：abilityType 不存在返回 404

运行：
    cd market-server/src/test/python
    pytest modules/ability/test_admin_delete.py -v -m L1
    pytest modules/ability/test_admin_delete.py -v -m L2
    pytest modules/ability/test_admin_delete.py -v -m L4
    pytest modules/ability/test_admin_delete.py -v              # 全部

依赖：
    - pytest (标记: L1, L2, L4)
    - common.client (API 客户端 + DB 工具)

注意：
    - ability_type 字段为 TINYINT UNSIGNED，最大值为 127，测试使用 70~79 范围
    - 删除接口按 abilityType 删除，先创建再删除
"""

import pytest
from common.client import api, db_val


# ==================== 辅助函数 ====================

_seed_id_counter = 70


def seed_ability(ability_type=None, with_properties=False):
    """预创建一条能力记录用于删除测试，返回 ability_type"""
    global _seed_id_counter
    if ability_type is None:
        _seed_id_counter += 1
        ability_type = _seed_id_counter
    body = {
        "abilityType": ability_type,
        "nameCn": f"删除测试{ability_type}",
        "nameEn": f"DeleteTest{ability_type}",
        "descCn": f"这是删除测试能力{ability_type}的详细描述信息",
        "descEn": f"This is delete test ability {ability_type} description",
        "iconBatchId": "test_batch_icon_delete",
    }
    if with_properties:
        body["diagramBatchId"] = "test_batch_diagram_delete"
    resp = api("POST", "/service/open/v2/ability/admin", body=body)
    assert resp is not None and resp["code"] == "200", f"种子创建失败: {resp}"
    return ability_type


def delete_ability(ability_type, expected_code="200"):
    """删除能力并返回响应"""
    resp = api("DELETE", f"/service/open/v2/ability/admin/{ability_type}")
    assert resp is not None, "API 返回 None（服务未运行）"
    if expected_code:
        assert resp["code"] == expected_code, \
            f"期望 code={expected_code}, 实际: {resp}"
    return resp


def get_ability_id(ability_type):
    """根据 ability_type 查询能力 ID"""
    ability_id = db_val(
        f"SELECT id FROM openplatform_ability_t WHERE ability_type={ability_type}")
    return int(ability_id) if ability_id else None


def get_property_count(parent_id):
    """查询属性表记录数"""
    count = db_val(
        f"SELECT COUNT(1) FROM openplatform_ability_p_t WHERE parent_id={parent_id}")
    return int(count) if count else 0


# ==================== L1: 正常流程测试 ====================


class TestAbilityAdminDeleteL1:
    """删除接口 — 正常流程 (1)"""

    @pytest.mark.L1
    def test_delete_normal(self):
        """L1-1: 正常删除能力应返回 200，主表记录被删除"""
        ability_type = seed_ability()
        ability_id = get_ability_id(ability_type)
        assert ability_id is not None, "种子创建后记录应在主表中"

        try:
            resp = delete_ability(ability_type)
            assert resp["code"] == "200"

            # 验证主表记录已删除
            deleted_id = get_ability_id(ability_type)
            assert deleted_id is None, f"ability_type={ability_type} 应已被删除"
        finally:
            # 清理：如果删除失败了，还得手动清理
            remaining_id = get_ability_id(ability_type)
            if remaining_id:
                db_val(f"DELETE FROM openplatform_ability_p_t WHERE parent_id={remaining_id}")
                db_val(f"DELETE FROM openplatform_ability_t WHERE ability_type={ability_type}")


# ==================== L2: 业务规则测试 ====================


class TestAbilityAdminDeleteL2:
    """删除接口 — 业务规则 (1)"""

    @pytest.mark.L2
    def test_delete_with_properties(self):
        """L2-1: 含属性记录的能力删除后，属性表也应被清理"""
        ability_type = seed_ability(with_properties=True)
        ability_id = get_ability_id(ability_type)
        assert ability_id is not None

        # 确认创建了属性
        prop_count_before = get_property_count(ability_id)
        assert prop_count_before > 0, "应该至少有一条属性记录（图标）"

        try:
            resp = delete_ability(ability_type)
            assert resp["code"] == "200"

            # 验证主表已删
            deleted_id = get_ability_id(ability_type)
            assert deleted_id is None, f"ability_type={ability_type} 应已被删除"

            # 验证属性表也已清理
            prop_count_after = get_property_count(ability_id)
            assert prop_count_after == 0, f"属性表应被清空，还剩 {prop_count_after} 条"
        finally:
            remaining_id = get_ability_id(ability_type)
            if remaining_id:
                db_val(f"DELETE FROM openplatform_ability_p_t WHERE parent_id={remaining_id}")
                db_val(f"DELETE FROM openplatform_ability_t WHERE ability_type={ability_type}")


# ==================== L4: 边界/反向测试 ====================


class TestAbilityAdminDeleteL4:
    """删除接口 — 边界/反向 (1)"""

    @pytest.mark.L4
    def test_delete_not_found(self):
        """L4-1: 不存在的 abilityType 应返回 404"""
        # 使用一个肯定不会存在的 abilityType
        resp = api("DELETE", "/service/open/v2/ability/admin/255")
        assert resp is not None
        assert resp["code"] == "404"
        assert "不存在" in resp["messageZh"]
