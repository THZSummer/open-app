"""
能力目录管理创建接口集成测试

覆盖场景：
    L1 - 正常流程（2 个）：完整字段创建、最小必填字段创建
    L2 - 业务规则（5 个）：编码唯一性校验、loadType=2 三要素必填、
         entryUrl 格式校验、含示意图创建、排序号自动补全
    L4 - 边界/反向（4 个）：缺少必填字段、nameCn 长度不足、descCn 长度不足、
         缺少 iconBatchId

运行：
    cd market-server/src/test/python
    pytest modules/ability/test_admin_create.py -v -m L1
    pytest modules/ability/test_admin_create.py -v -m L2
    pytest modules/ability/test_admin_create.py -v -m L4
    pytest modules/ability/test_admin_create.py -v              # 全部

依赖：
    - pytest (标记: L1, L2, L4)
    - common.client (API 客户端 + DB 工具)

注意：
    - ability_type 字段为 TINYINT UNSIGNED，最大值为 255，测试使用 201~210 范围
    - nameEn/descEn 为必填字段（@NotBlank），所有期望成功的请求必须包含
"""

import pytest
from common.client import api, db_val


# ==================== 辅助函数 ====================

def create_ability(body, expected_code="200"):
    """创建能力并返回响应"""
    resp = api("POST", "/service/open/v2/ability/admin", body=body)
    assert resp is not None, "API 返回 None（服务未运行）"
    if expected_code:
        assert resp["code"] == expected_code, \
            f"期望 code={expected_code}, 实际: {resp}"
    return resp


def cleanup_ability(ability_type):
    """清理测试数据（删除主表和属性表记录）"""
    db_val(f"DELETE FROM openplatform_ability_p_t WHERE parent_id IN "
           f"(SELECT id FROM openplatform_ability_t WHERE ability_type={ability_type})")
    db_val(f"DELETE FROM openplatform_ability_t WHERE ability_type={ability_type}")


# ==================== L1: 正常流程测试 ====================


class TestAbilityAdminCreateL1:
    """创建接口 — 正常流程 (2)"""

    @pytest.mark.L1
    def test_create_full_fields(self):
        """L1-1: 完整字段创建应返回 200"""
        ability_type = 201
        try:
            body = {
                "abilityType": ability_type,
                "nameCn": "完整字段测试",
                "nameEn": "FullFieldTest",
                "descCn": "这是一个完整字段的创建测试用例",
                "descEn": "This is a full field test",
                "orderNum": 100,
                "entryUrl": "https://example.com/ability",
                "routePath": "/full-test",
                "aliasName": "full-test-app",
                "hidden": 0,
                "requireRelease": 0,
                "loadType": 1,
                "iconBatchId": "test_batch_icon_001",
                "diagramBatchId": "test_batch_diagram_001",
            }
            resp = create_ability(body)
            assert resp["code"] == "200"
        finally:
            cleanup_ability(ability_type)

    @pytest.mark.L1
    def test_create_minimal_fields(self):
        """L1-2: 最小必填字段创建应返回 200"""
        ability_type = 202
        try:
            body = {
                "abilityType": ability_type,
                "nameCn": "最小字段测试",
                "nameEn": "MinFieldTest",
                "descCn": "这是最小字段测试的描述信息",
                "descEn": "This is a minimal field test",
                "iconBatchId": "test_batch_icon_002",
            }
            resp = create_ability(body)
            assert resp["code"] == "200"
        finally:
            cleanup_ability(ability_type)


# ==================== L2: 业务规则测试 ====================


class TestAbilityAdminCreateL2:
    """创建接口 — 业务规则 (5)"""

    @pytest.mark.L2
    def test_duplicate_ability_type(self):
        """L2-1: 编码唯一性校验 — 重复返回 409"""
        ability_type = 203
        try:
            # 先创建一条
            body1 = {
                "abilityType": ability_type,
                "nameCn": "首次创建",
                "nameEn": "FirstCreate",
                "descCn": "这是首次创建的测试能力描述",
                "descEn": "This is the first creation test",
                "iconBatchId": "test_batch_icon_003",
            }
            resp1 = create_ability(body1)
            assert resp1["code"] == "200"

            # 重复创建
            body2 = {
                "abilityType": ability_type,
                "nameCn": "重复创建",
                "nameEn": "DuplicateCreate",
                "descCn": "这是重复创建的测试能力描述",
                "descEn": "This is a duplicate creation test",
                "iconBatchId": "test_batch_icon_003b",
            }
            resp2 = api("POST", "/service/open/v2/ability/admin", body=body2)
            assert resp2 is not None
            assert resp2["code"] == "409"
            assert "编码已被占用" in resp2["messageZh"]
        finally:
            cleanup_ability(ability_type)

    @pytest.mark.L2
    def test_load_type_2_requires_all_three(self):
        """L2-2: loadType=2 缺少三要素应返回 400"""
        ability_type = 204
        try:
            body = {
                "abilityType": ability_type,
                "nameCn": "微前端测试",
                "nameEn": "MicroFrontendTest",
                "descCn": "这是微前端加载模式测试描述",
                "descEn": "This is a micro frontend test",
                "iconBatchId": "test_batch_icon_004",
                "loadType": 2,
                # 缺少 entryUrl/routePath/aliasName
            }
            resp = api("POST", "/service/open/v2/ability/admin", body=body)
            assert resp is not None
            assert resp["code"] == "400"
            assert "三要素必填" in resp["messageZh"]
        finally:
            cleanup_ability(ability_type)

    @pytest.mark.L2
    def test_invalid_entry_url_format(self):
        """L2-3: entryUrl 非 http/https 应返回 400"""
        ability_type = 205
        try:
            body = {
                "abilityType": ability_type,
                "nameCn": "URL格式测试",
                "nameEn": "UrlFormatTest",
                "descCn": "这是URL格式校验的测试描述",
                "descEn": "This is a URL format test",
                "entryUrl": "ftp://invalid-protocol.com",
                "iconBatchId": "test_batch_icon_005",
            }
            resp = api("POST", "/service/open/v2/ability/admin", body=body)
            assert resp is not None
            assert resp["code"] == "400"
            assert "访问地址格式不正确" in resp["messageZh"]
        finally:
            cleanup_ability(ability_type)

    @pytest.mark.L2
    def test_create_with_diagram(self):
        """L2-4: 含示意图创建应成功"""
        ability_type = 206
        try:
            body = {
                "abilityType": ability_type,
                "nameCn": "含示意图测试",
                "nameEn": "WithDiagramTest",
                "descCn": "这是含示意图的创建测试描述",
                "descEn": "This is a diagram creation test",
                "iconBatchId": "test_batch_icon_006",
                "diagramBatchId": "test_batch_diagram_006",
            }
            resp = create_ability(body)
            assert resp["code"] == "200"

            # 验证属性表写入
            ability_id = db_val(
                f"SELECT id FROM openplatform_ability_t WHERE ability_type={ability_type}")
            assert ability_id is not None

            diagram_count = db_val(
                f"SELECT COUNT(1) FROM openplatform_ability_p_t "
                f"WHERE parent_id={ability_id} AND property_name='example_diagram'")
            assert diagram_count == "1", f"示意图属性未写入: {diagram_count}"
        finally:
            cleanup_ability(ability_type)

    @pytest.mark.L2
    def test_auto_order_num(self):
        """L2-5: 未传 orderNum 应自动补全（当前最大值+1）"""
        ability_type = 207
        try:
            # 先获取当前最大 orderNum
            max_order = db_val("SELECT MAX(order_num) FROM openplatform_ability_t WHERE status=1")
            current_max = int(max_order) if max_order else 0

            body = {
                "abilityType": ability_type,
                "nameCn": "自动排序测试",
                "nameEn": "AutoOrderTest",
                "descCn": "这是自动排序号的测试描述",
                "descEn": "This is an auto order test",
                "iconBatchId": "test_batch_icon_007",
                # 不传 orderNum
            }
            resp = create_ability(body)
            assert resp["code"] == "200"

            # 验证 orderNum = current_max + 1
            actual_order = db_val(
                f"SELECT order_num FROM openplatform_ability_t WHERE ability_type={ability_type}")
            assert actual_order is not None
            assert int(actual_order) == current_max + 1, \
                f"期望 orderNum={current_max + 1}, 实际={actual_order}"
        finally:
            cleanup_ability(ability_type)


# ==================== L4: 边界/反向测试 ====================


class TestAbilityAdminCreateL4:
    """创建接口 — 边界/反向 (4)"""

    @pytest.mark.L4
    def test_missing_required_fields(self):
        """L4-1: 缺少必填字段（abilityType/nameCn/descCn/nameEn/descEn）应返回 400"""
        body = {}
        resp = api("POST", "/service/open/v2/ability/admin", body=body)
        assert resp is not None
        assert resp["code"] == "400"

    @pytest.mark.L4
    def test_name_cn_too_short(self):
        """L4-2: nameCn 长度小于2字符应返回 400"""
        body = {
            "abilityType": 208,
            "nameCn": "测",
            "nameEn": "TestEnForNameCn",
            "descCn": "这是测试nameCn长度校验的描述信息",
            "descEn": "This is for nameCn length test",
            "iconBatchId": "test_batch_icon_008",
        }
        resp = api("POST", "/service/open/v2/ability/admin", body=body)
        assert resp is not None
        assert resp["code"] == "400"

    @pytest.mark.L4
    def test_desc_cn_too_short(self):
        """L4-3: descCn 长度小于5字符应返回 400"""
        body = {
            "abilityType": 209,
            "nameCn": "描述测试",
            "nameEn": "TestEnForDescCn",
            "descCn": "描述",
            "descEn": "This is for descCn length test",
            "iconBatchId": "test_batch_icon_009",
        }
        resp = api("POST", "/service/open/v2/ability/admin", body=body)
        assert resp is not None
        assert resp["code"] == "400"

    @pytest.mark.L4
    def test_missing_icon_batch_id(self):
        """L4-4: 缺少 iconBatchId 应返回 400"""
        ability_type = 210
        try:
            body = {
                "abilityType": ability_type,
                "nameCn": "图标必填测试",
                "nameEn": "IconRequiredTest",
                "descCn": "这是图标必填测试的描述信息",
                "descEn": "This is an icon required test",
                # 缺少 iconBatchId
            }
            resp = api("POST", "/service/open/v2/ability/admin", body=body)
            assert resp is not None
            assert resp["code"] == "400"
        finally:
            cleanup_ability(ability_type)
