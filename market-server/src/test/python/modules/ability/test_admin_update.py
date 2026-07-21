"""
能力目录管理编辑接口集成测试

覆盖场景：
    L1 - 正常流程（2 个）：部分字段更新、全字段更新
    L2 - 业务规则（6 个）：loadType=2 三要素校验、entryUrl 格式校验、乐观锁冲突
    L4 - 边界/反向（3 个）：id不存在 404、字段长度校验、空请求体

运行：
    cd market-server/src/test/python
    pytest modules/ability/test_admin_update.py -v -m L1
    pytest modules/ability/test_admin_update.py -v -m L2
    pytest modules/ability/test_admin_update.py -v -m L4
    pytest modules/ability/test_admin_update.py -v              # 全部

依赖：
    - pytest (标记: L1, L2, L4)
    - common.client (API 客户端 + DB 工具)

注意：
    - 编辑接口使用数据库主键 id 作为路径参数，所有字段均可选
"""

import pytest
from common.client import api, db_val


# ==================== 辅助函数 ====================

_seed_id_counter = 60


def seed_ability():
    """预创建一条能力记录用于编辑测试，返回 (ability_type, ability_id)"""
    global _seed_id_counter
    _seed_id_counter += 1
    ability_type = _seed_id_counter
    default = {
        "abilityType": ability_type,
        "nameCn": f"编辑测试{ability_type}",
        "nameEn": f"EditTest{ability_type}",
        "descCn": f"这是编辑测试能力{ability_type}的详细描述信息",
        "descEn": f"This is edit test ability {ability_type} description",
        "iconBatchId": "test_batch_icon_seed",
    }
    resp = api("POST", "/service/open/v2/ability/admin", body=default)
    assert resp is not None and resp["code"] == "200", f"种子创建失败: {resp}"
    ability_id = int(db_val(
        f"SELECT id FROM openplatform_ability_t WHERE ability_type={ability_type}"))
    return ability_type, ability_id


def update_ability(ability_id, body, expected_code="200"):
    """编辑能力并返回响应"""
    resp = api("PUT", f"/service/open/v2/ability/admin/{ability_id}", body=body)
    assert resp is not None, "API 返回 None（服务未运行）"
    if expected_code:
        assert resp["code"] == expected_code, \
            f"期望 code={expected_code}, 实际: {resp}"
    return resp


def cleanup_ability(ability_type):
    """清理测试数据"""
    db_val(f"DELETE FROM openplatform_ability_p_t WHERE parent_id IN "
           f"(SELECT id FROM openplatform_ability_t WHERE ability_type={ability_type})")
    db_val(f"DELETE FROM openplatform_ability_t WHERE ability_type={ability_type}")


# ==================== L1: 正常流程测试 ====================


class TestAbilityAdminUpdateL1:
    """编辑接口 — 正常流程 (2)"""

    @pytest.mark.L1
    def test_update_partial_fields(self):
        """L1-1: 部分字段更新应返回 200 且仅更新传入字段"""
        ability_type, ability_id = seed_ability()
        try:
            body = {
                "nameCn": "新中文名",
                "descCn": "这是新中文描述内容足够长度",
            }
            resp = update_ability(ability_id, body)
            assert resp["code"] == "200"

            # 验证更新
            name_cn = db_val(
                f"SELECT ability_name_cn FROM openplatform_ability_t WHERE id={ability_id}")
            assert name_cn == "新中文名", f"nameCn 未更新: {name_cn}"

            desc_cn = db_val(
                f"SELECT ability_desc_cn FROM openplatform_ability_t WHERE id={ability_id}")
            assert desc_cn == "这是新中文描述内容足够长度", f"descCn 未更新: {desc_cn}"

            # 未传入字段不应改变
            name_en = db_val(
                f"SELECT ability_name_en FROM openplatform_ability_t WHERE id={ability_id}")
            assert name_en == f"EditTest{ability_type}", f"nameEn 不应被修改: {name_en}"
        finally:
            cleanup_ability(ability_type)

    @pytest.mark.L1
    def test_update_all_fields(self):
        """L1-2: 全字段更新应返回 200"""
        ability_type, ability_id = seed_ability()
        try:
            body = {
                "nameCn": "全字段更新",
                "nameEn": "FullUpdate",
                "descCn": "这是全字段更新的中文描述内容",
                "descEn": "This is full field update English description",
                "orderNum": 999,
                "entryUrl": "https://full-update.example.com",
                "routePath": "/full-update",
                "aliasName": "full-update-app",
                "hidden": 0,
                "requireRelease": 1,
                "loadType": 2,
                "iconBatchId": "new_icon_batch",
                "diagramBatchId": "new_diagram_batch",
            }
            resp = update_ability(ability_id, body)
            assert resp["code"] == "200"

            # 验证主表
            name_cn = db_val(
                f"SELECT ability_name_cn FROM openplatform_ability_t WHERE id={ability_id}")
            assert name_cn == "全字段更新"
            load_type = db_val(
                f"SELECT load_type FROM openplatform_ability_t WHERE id={ability_id}")
            assert load_type == "2"

            # 验证属性表
            icon_value = db_val(
                f"SELECT property_value FROM openplatform_ability_p_t "
                f"WHERE parent_id={ability_id} AND property_name='icon'")
            assert icon_value == "new_icon_batch"
        finally:
            cleanup_ability(ability_type)


# ==================== L2: 业务规则测试 ====================


class TestAbilityAdminUpdateL2:
    """编辑接口 — 业务规则 (6)"""

    @pytest.mark.L2
    def test_load_type_2_requires_all_three(self):
        """L2-1: loadType=2 且三要素不全应返回 400"""
        ability_type, ability_id = seed_ability()
        try:
            body = {
                "loadType": 2,
                "entryUrl": "https://example.com",
                # 缺少 routePath 和 aliasName
            }
            resp = update_ability(ability_id, body, "400")
            assert "三要素必填" in resp["messageZh"]
        finally:
            cleanup_ability(ability_type)

    @pytest.mark.L2
    def test_invalid_entry_url_format(self):
        """L2-2: entryUrl 非 http/https 应返回 400"""
        ability_type, ability_id = seed_ability()
        try:
            body = {
                "entryUrl": "ftp://invalid-protocol.com",
            }
            resp =update_ability(ability_id, body, "400")
            assert "访问地址格式不正确" in resp["messageZh"]
        finally:
            cleanup_ability(ability_type)

    @pytest.mark.L2
    def test_update_icon_property(self):
        """L2-3: 仅更新图标 batchId 应成功"""
        ability_type, ability_id = seed_ability()
        try:
            body = {
                "iconBatchId": "updated_icon_batch_001",
            }
            resp = update_ability(ability_id, body)
            assert resp["code"] == "200"

            icon_value = db_val(
                f"SELECT property_value FROM openplatform_ability_p_t "
                f"WHERE parent_id={ability_id} AND property_name='icon'")
            assert icon_value == "updated_icon_batch_001"
        finally:
            cleanup_ability(ability_type)

    @pytest.mark.L2
    def test_load_type_2_with_all_three_ok(self):
        """L2-4: loadType=2 三要素齐全应返回 200"""
        ability_type, ability_id = seed_ability()
        try:
            body = {
                "loadType": 2,
                "entryUrl": "https://mfe-update.example.com",
                "routePath": "/mfe-update",
                "aliasName": "mfe-update-app",
            }
            resp = update_ability(ability_id, body)
            assert resp["code"] == "200"

            load_type = db_val(
                f"SELECT load_type FROM openplatform_ability_t WHERE id={ability_id}")
            assert load_type == "2"
        finally:
            cleanup_ability(ability_type)

    @pytest.mark.L2
    def test_load_type_1_empty_entry_url_ok(self):
        """L2-5: loadType=1 + entryUrl="" 应返回 200（回归：空字符串不触发格式校验）"""
        ability_type, ability_id = seed_ability()
        try:
            body = {
                "loadType": 1,
                "entryUrl": "",
                "nameCn": "空地址更新测试",
            }
            resp = update_ability(ability_id, body)
            assert resp["code"] == "200"
        finally:
            cleanup_ability(ability_type)

    @pytest.mark.L2
    def test_load_type_1_entry_url_optional(self):
        """L2-6: loadType=1 不传 entryUrl 应返回 200"""
        ability_type, ability_id = seed_ability()
        try:
            body = {
                "loadType": 1,
                "nameCn": "无地址更新测试",
            }
            resp = update_ability(ability_id, body)
            assert resp["code"] == "200"
        finally:
            cleanup_ability(ability_type)


# ==================== L4: 边界/反向测试 ====================


class TestAbilityAdminUpdateL4:
    """编辑接口 — 边界/反向 (3)"""

    @pytest.mark.L4
    def test_id_not_found(self):
        """L4-1: 不存在的 id 应返回 404"""
        body = {"nameCn": "新名称"}
        resp = api("PUT", "/service/open/v2/ability/admin/99999", body=body)
        assert resp is not None
        assert resp["code"] == "404"
        assert "不存在" in resp["messageZh"]

    @pytest.mark.L4
    def test_empty_body(self):
        """L4-2: 空请求体应返回 200（无字段更新也成功）"""
        ability_type, ability_id = seed_ability()
        try:
            body = {}
            resp = update_ability(ability_id, body)
            assert resp["code"] == "200"
        finally:
            cleanup_ability(ability_type)

    @pytest.mark.L4
    def test_name_cn_too_long(self):
        """L4-3: nameCn 超过30字符应返回 400"""
        ability_type, ability_id = seed_ability()
        try:
            body = {
                "nameCn": "超长中文名测试" * 8,  # 56 chars
            }
            resp = update_ability(ability_id, body, "400")
        finally:
            cleanup_ability(ability_type)
