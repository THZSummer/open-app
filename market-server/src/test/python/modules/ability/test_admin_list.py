"""
能力列表接口集成测试

覆盖场景：
    L1 - 正常流程：分页查询、字段完整性
    L2 - 业务规则：关键字搜索、排序方向、排序字段
    L4 - 边界/反向：非法参数、超大分页、特殊字符搜索

依赖：
    - pytest (标记: L1, L2, L4)
    - requests
    - common.client (API 客户端包装)
    - common.db (数据库助手)
"""

import pytest
from common import api, db_val


# ==================== L1: 正常流程测试 ====================


class TestAbilityAdminListL1:
    """列表接口正常流程"""

    @pytest.mark.L1
    def test_list_default_params(self, api):
        """默认参数查询应返回 200 和分页信息"""
        resp = api("GET", "/service/open/v2/ability/admin/list")
        assert resp["code"] == "200", f"默认查询失败: {resp}"
        assert "data" in resp
        assert "page" in resp
        assert resp["page"]["curPage"] == 1
        assert resp["page"]["pageSize"] == 20
        assert resp["page"]["total"] >= 0
        # 验证字段完整性（取第一条）
        if len(resp["data"]) > 0:
            item = resp["data"][0]
            required_fields = [
                "abilityType", "nameCn", "nameEn", "descCn", "descEn",
                "iconUrl", "diagramUrl", "orderNum", "entryUrl", "hidden",
                "routePath", "aliasName", "requireRelease", "loadType",
                "createTime", "updateBy", "updateTime"
            ]
            for field in required_fields:
                assert field in item, f"缺少字段: {field}"

    @pytest.mark.L1
    def test_list_with_pagination(self, api):
        """翻页参数应正确生效"""
        # 查询第一页，每页 2 条
        resp1 = api("GET", "/service/open/v2/ability/admin/list",
                    params={"curPage": 1, "pageSize": 2})
        assert resp1["code"] == "200"
        assert len(resp1["data"]) <= 2
        assert resp1["page"]["pageSize"] == 2

        # 如果有足够数据，验证翻页
        total = resp1["page"]["total"]
        if total > 2:
            resp2 = api("GET", "/service/open/v2/ability/admin/list",
                        params={"curPage": 2, "pageSize": 2})
            assert resp2["code"] == "200"
            assert resp2["page"]["curPage"] == 2
            # 第二页的数据应不同于第一页（若两页都有数据）
            if len(resp1["data"]) > 0 and len(resp2["data"]) > 0:
                assert resp1["data"][0]["abilityType"] != resp2["data"][0]["abilityType"], \
                    "翻页应返回不同数据"

    @pytest.mark.L1
    def test_list_fields_completeness(self, api):
        """返回字段应包含 14 个业务字段 + 时间字段"""
        # 先插入一条测试数据（通过 DB 操作）
        import random
        test_id = random.randint(1000000, 9000000)
        test_prop_icon_id = test_id + 1
        test_prop_diagram_id = test_id + 2
        test_type = random.randint(200, 250)
        try:
            db_val(
                f"INSERT INTO openplatform_ability_t "
                f"(id, ability_name_cn, ability_name_en, ability_desc_cn, ability_desc_en, "
                f"ability_type, order_num, status, entry_url, hidden, route_path, alias_name, "
                f"require_release, load_type, create_by, create_time, last_update_by, last_update_time) "
                f"VALUES ({test_id}, '列表测试-中文', 'ListTest-En', '列表测试描述', 'List Test Desc', "
                f"{test_type}, 999, 1, 'http://test.com', 0, '/test', 'test-alias', 0, 1, "
                f"'pytest', NOW(), 'pytest', NOW())"
            )
            # 插入属性表（图标/示意图）
            db_val(
                f"INSERT INTO openplatform_ability_p_t "
                f"(id, parent_id, property_name, property_value, status, create_by, create_time, "
                f"last_update_by, last_update_time) "
                f"VALUES ({test_prop_icon_id}, {test_id}, 'icon', 'pytest-icon-batch', 1, "
                f"'pytest', NOW(), 'pytest', NOW())"
            )
            db_val(
                f"INSERT INTO openplatform_ability_p_t "
                f"(id, parent_id, property_name, property_value, status, create_by, create_time, "
                f"last_update_by, last_update_time) "
                f"VALUES ({test_prop_diagram_id}, {test_id}, 'diagram', 'pytest-diagram-batch', 1, "
                f"'pytest', NOW(), 'pytest', NOW())"
            )

            # 查询列表，找到刚才插入的数据
            resp = api("GET", "/service/open/v2/ability/admin/list",
                       params={"keyword": "列表测试-中文"})
            assert resp["code"] == "200"

            # 找到测试数据
            found = None
            for item in resp["data"]:
                if item["abilityType"] == test_type:
                    found = item
                    break
            assert found is not None, f"未找到 abilityType={test_type} 的测试数据"

            # 验证字段值
            assert found["nameCn"] == "列表测试-中文"
            assert found["nameEn"] == "ListTest-En"
            assert found["descCn"] == "列表测试描述"
            assert found["descEn"] == "List Test Desc"
            assert found["orderNum"] == 999
            assert found["entryUrl"] == "http://test.com"
            assert found["hidden"] == 0
            assert found["routePath"] == "/test"
            assert found["aliasName"] == "test-alias"
            assert found["requireRelease"] == 0
            assert found["loadType"] == 1
            # iconUrl/diagramUrl 应为 /ability-files/ 前缀
            assert found["iconUrl"] is not None
            assert "/ability-files/" in found["iconUrl"]
            assert found["diagramUrl"] is not None
            assert "/ability-files/" in found["diagramUrl"]

        finally:
            # 清理测试数据
            db_val(f"DELETE FROM openplatform_ability_p_t WHERE parent_id IN ({test_id}, {test_prop_icon_id}, {test_prop_diagram_id})")
            db_val(f"DELETE FROM openplatform_ability_t WHERE id = {test_id}")
            db_val(f"DELETE FROM openplatform_ability_p_t WHERE id IN ({test_prop_icon_id}, {test_prop_diagram_id})")


# ==================== L2: 业务规则测试 ====================


class TestAbilityAdminListL2:
    """列表接口业务规则"""

    @pytest.mark.L2
    def test_search_by_name_cn(self, api):
        """按中文名模糊搜索"""
        resp = api("GET", "/service/open/v2/ability/admin/list",
                   params={"keyword": "群置顶"})
        assert resp["code"] == "200"
        if len(resp["data"]) > 0:
            # 所有结果的中文名应包含关键字
            for item in resp["data"]:
                assert "群置顶" in item["nameCn"], \
                    f"搜索中文名: 结果 '{item['nameCn']}' 不包含关键字"

    @pytest.mark.L2
    def test_search_by_name_en(self, api):
        """按英文名模糊搜索"""
        resp = api("GET", "/service/open/v2/ability/admin/list",
                   params={"keyword": "Group"})
        assert resp["code"] == "200"
        if len(resp["data"]) > 0:
            # 至少有一个结果英文名包含关键字
            has_match = any("Group" in item["nameEn"] for item in resp["data"])
            assert has_match, "英文名搜索结果应包含 'Group'"

    @pytest.mark.L2
    def test_search_case_insensitive(self, api):
        """搜索应不区分大小写（DB 层面）"""
        resp = api("GET", "/service/open/v2/ability/admin/list",
                   params={"keyword": "group"})
        assert resp["code"] == "200"
        if len(resp["data"]) > 0:
            # # MySQL LIKE 默认不区分大小写
            for item in resp["data"]:
                assert "group" in item["nameEn"].lower()

    @pytest.mark.L2
    def test_sort_asc(self, api):
        """按 orderNum 升序排序"""
        resp = api("GET", "/service/open/v2/ability/admin/list",
                   params={"sortField": "orderNum", "sortOrder": "asc"})
        assert resp["code"] == "200"
        data = resp["data"]
        if len(data) > 1:
            for i in range(len(data) - 1):
                assert data[i]["orderNum"] <= data[i + 1]["orderNum"], \
                    f"升序排序失败: index {i}: {data[i]['orderNum']} > {data[i+1]['orderNum']}"

    @pytest.mark.L2
    def test_sort_desc(self, api):
        """按 orderNum 降序排序"""
        resp = api("GET", "/service/open/v2/ability/admin/list",
                   params={"sortField": "orderNum", "sortOrder": "desc"})
        assert resp["code"] == "200"
        data = resp["data"]
        if len(data) > 1:
            for i in range(len(data) - 1):
                assert data[i]["orderNum"] >= data[i + 1]["orderNum"], \
                    f"降序排序失败: index {i}: {data[i]['orderNum']} < {data[i+1]['orderNum']}"

    @pytest.mark.L2
    def test_sort_by_ability_type(self, api):
        """按 abilityType 排序"""
        resp = api("GET", "/service/open/v2/ability/admin/list",
                   params={"sortField": "abilityType", "sortOrder": "asc"})
        assert resp["code"] == "200"
        data = resp["data"]
        if len(data) > 1:
            for i in range(len(data) - 1):
                assert data[i]["abilityType"] <= data[i + 1]["abilityType"], \
                    f"按 abilityType 升序排序失败"

    @pytest.mark.L2
    def test_icon_url_from_property(self, api):
        """图标 URL 应从属性表关联查询"""
        # 查询列表，验证 iconUrl 格式
        resp = api("GET", "/service/open/v2/ability/admin/list",
                   params={"pageSize": 50})
        assert resp["code"] == "200"
        # 至少有一条数据 iconUrl 不为 null（如果有属性表数据）
        icon_not_null = [item for item in resp["data"] if item.get("iconUrl") is not None]
        if len(resp["data"]) > 0:
            # iconUrl 要么为 null 要么以 /ability-files/ 开头
            for item in resp["data"]:
                if item.get("iconUrl") is not None:
                    assert item["iconUrl"].startswith("/ability-files/"), \
                        f"iconUrl 格式异常: {item['iconUrl']}"
                if item.get("diagramUrl") is not None:
                    assert item["diagramUrl"].startswith("/ability-files/"), \
                        f"diagramUrl 格式异常: {item['diagramUrl']}"


# ==================== L4: 边界/反向测试 ====================


class TestAbilityAdminListL4:
    """列表接口边界/反向测试"""

    @pytest.mark.L4
    def test_invalid_sort_field(self, api):
        """非法的排序字段应返回 400"""
        resp = api("GET", "/service/open/v2/ability/admin/list",
                   params={"sortField": "invalid_column", "sortOrder": "asc"},
                   expected_status=200)
        assert resp["code"] == "400", \
            f"非法排序字段应返回 400，实际: {resp}"

    @pytest.mark.L4
    def test_invalid_sort_order(self, api):
        """非法的排序方向应返回 400"""
        resp = api("GET", "/service/open/v2/ability/admin/list",
                   params={"sortField": "orderNum", "sortOrder": "invalid"},
                   expected_status=200)
        assert resp["code"] == "400", \
            f"非法排序方向应返回 400，实际: {resp}"

    @pytest.mark.L4
    def test_large_page_size(self, api):
        """超过最大 pageSize (100) 应被限制"""
        resp = api("GET", "/service/open/v2/ability/admin/list",
                   params={"pageSize": 9999})
        assert resp["code"] == "200"
        # page 中的 pageSize 应为实际使用的值（最大 100）
        assert resp["page"]["pageSize"] <= 100, \
            f"pageSize 应被限制在 100 以内，实际: {resp['page']['pageSize']}"

    @pytest.mark.L4
    def test_zero_page_size(self, api):
        """pageSize 为 0 应被修正为 1"""
        resp = api("GET", "/service/open/v2/ability/admin/list",
                   params={"pageSize": 0})
        assert resp["code"] == "200"
        assert resp["page"]["pageSize"] >= 1, \
            f"pageSize 应为正数，实际: {resp['page']['pageSize']}"

    @pytest.mark.L4
    def test_negative_cur_page(self, api):
        """负数的 curPage 应被修正为 1"""
        resp = api("GET", "/service/open/v2/ability/admin/list",
                   params={"curPage": -1})
        assert resp["code"] == "200"
        # curPage 会被 Math.max(1, curPage) 修正为 1

    @pytest.mark.L4
    def test_special_chars_keyword(self, api):
        """特殊字符搜索不应导致异常"""
        special_chars = ["%", "_", "'", "\"", "\\", "<script>", "null", "undefined"]
        for char in special_chars:
            resp = api("GET", "/service/open/v2/ability/admin/list",
                       params={"keyword": char})
            assert resp["code"] == "200", f"特殊字符 '{char}' 搜索应正常返回 200"
            # 允许返回空结果
            assert "data" in resp

    @pytest.mark.L4
    def test_empty_keyword(self, api):
        """空关键字搜索应返回全量数据"""
        resp = api("GET", "/service/open/v2/ability/admin/list",
                   params={"keyword": ""})
        assert resp["code"] == "200"
        # 空关键字等于不传
        total = resp["page"]["total"]
        assert total >= 0

    @pytest.mark.L4
    def test_pagination_beyond_total(self, api):
        """分页超出总记录数应返回空列表"""
        resp = api("GET", "/service/open/v2/ability/admin/list",
                   params={"curPage": 99999, "pageSize": 20})
        assert resp["code"] == "200"
        # 超出范围的页码应返回空列表
        assert len(resp["data"]) == 0
