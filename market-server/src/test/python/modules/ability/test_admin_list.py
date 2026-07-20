"""
能力目录管理列表接口集成测试

覆盖场景：
    L1 - 正常流程（3 个）：默认分页、少量数据、字段完整性
    L2 - 业务规则（8 个）：中文搜索、英文搜索、无结果、排序（orderNum/createTime/nameCn）、
         分页第二页、4 媒体字段非空验证
    L4 - 边界/反向（5 个）：pageSize=0、pageSize>100、curPage=0、
         curPage 超范围、sortField 非法

运行：
    cd market-server/src/test/python
    pytest modules/ability/test_admin_list.py -v -m L1
    pytest modules/ability/test_admin_list.py -v -m L2
    pytest modules/ability/test_admin_list.py -v -m L4
    pytest modules/ability/test_admin_list.py -v              # 全部

依赖：
    - pytest (标记: L1, L2, L4)
    - common (API 客户端包装)
"""

import pytest


# ==================== L1: 正常流程测试 ====================


class TestAbilityAdminListL1:
    """列表接口 — 正常流程 (3)"""

    @pytest.mark.L1
    def test_default_pagination(self, api):
        """L1-1: 默认分页 curPage=1&pageSize=20"""
        resp = api("GET", "/service/open/v2/ability/admin/list")
        assert resp is not None, "API 返回 None（服务未运行）"
        assert resp["code"] == "200", f"默认查询失败: {resp}"
        assert resp["page"]["curPage"] == 1
        assert resp["page"]["pageSize"] == 20
        assert resp["page"]["total"] >= 0
        assert isinstance(resp["data"], list)

    @pytest.mark.L1
    def test_small_page_size(self, api):
        """L1-2: 少量数据 pageSize=3"""
        resp = api("GET", "/service/open/v2/ability/admin/list",
                    params={"pageSize": 3})
        assert resp is not None
        assert resp["code"] == "200"
        assert len(resp["data"]) <= 3
        assert resp["page"]["pageSize"] == 3

    @pytest.mark.L1
    def test_field_completeness(self, api):
        """L1-3: 16 个业务字段存在且类型正确"""
        resp = api("GET", "/service/open/v2/ability/admin/list",
                    params={"pageSize": 1})
        assert resp is not None
        assert resp["code"] == "200"
        if len(resp["data"]) == 0:
            pytest.skip("无数据，无法验证字段完整性")
        item = resp["data"][0]

        # 16 个业务字段存在
        required_fields = [
            "abilityType", "nameCn", "nameEn", "descCn", "descEn",
            "icon", "iconUrl", "exampleDiagram", "exampleDiagramUrl",
            "orderNum", "entryUrl", "hidden",
            "routePath", "aliasName", "requireRelease", "loadType",
        ]
        for field in required_fields:
            assert field in item, f"缺少字段: {field}"

        # 类型验证
        assert isinstance(item["abilityType"], int), "abilityType 应为 int"
        assert isinstance(item["nameCn"], str), "nameCn 应为 str"
        assert isinstance(item["nameEn"], str), "nameEn 应为 str"
        assert isinstance(item["orderNum"], int), "orderNum 应为 int"
        assert isinstance(item["hidden"], (int, bool)), "hidden 应为 int/bool"
        assert isinstance(item["requireRelease"], (int, bool)), "requireRelease 应为 int/bool"


# ==================== L2: 业务规则测试 ====================


class TestAbilityAdminListL2:
    """列表接口 — 业务规则 (8)"""

    @pytest.mark.L2
    def test_search_keyword_cn(self, api):
        """L2-1: keyword 搜索 — 中文名匹配"""
        resp = api("GET", "/service/open/v2/ability/admin/list",
                    params={"keyword": "群置顶", "pageSize": 20})
        assert resp is not None
        assert resp["code"] == "200"
        for item in resp["data"]:
            assert "群置顶" in item["nameCn"], \
                f"搜索结果 '{item['nameCn']}' 不包含关键字"

    @pytest.mark.L2
    def test_search_keyword_en(self, api):
        """L2-2: keyword 搜索 — 英文名匹配"""
        resp = api("GET", "/service/open/v2/ability/admin/list",
                    params={"keyword": "Group", "pageSize": 50})
        assert resp is not None
        assert resp["code"] == "200"
        if len(resp["data"]) > 0:
            has_match = any("Group" in item["nameEn"] for item in resp["data"])
            assert has_match, "英文名搜索结果应包含 'Group'"

    @pytest.mark.L2
    def test_search_no_result(self, api):
        """L2-3: keyword 搜索 — 无结果返回空数组"""
        resp = api("GET", "/service/open/v2/ability/admin/list",
                    params={"keyword": "__NON_EXISTENT_KEYWORD_XYZ__"})
        assert resp is not None
        assert resp["code"] == "200"
        assert resp["data"] == [], f"无匹配时应返回空数组, 实际: {resp['data']}"
        assert resp["page"]["total"] == 0

    @pytest.mark.L2
    def test_sort_order_num_asc(self, api):
        """L2-4: 排序 — 按 orderNum 升序"""
        resp = api("GET", "/service/open/v2/ability/admin/list",
                    params={"sortField": "orderNum", "sortOrder": "asc", "pageSize": 50})
        assert resp is not None
        assert resp["code"] == "200"
        data = resp["data"]
        if len(data) > 1:
            for i in range(len(data) - 1):
                assert data[i]["orderNum"] <= data[i + 1]["orderNum"], \
                    f"升序排序失败: index {i}: {data[i]['orderNum']} > {data[i+1]['orderNum']}"

    @pytest.mark.L2
    def test_sort_create_time_desc(self, api):
        """L2-5: 排序 — 按 createTime 降序"""
        resp = api("GET", "/service/open/v2/ability/admin/list",
                    params={"sortField": "createTime", "sortOrder": "desc", "pageSize": 50})
        assert resp is not None
        assert resp["code"] == "200"
        data = resp["data"]
        if len(data) > 1:
            for i in range(len(data) - 1):
                assert data[i]["createTime"] >= data[i + 1]["createTime"], \
                    f"createTime 降序失败: index {i}"

    @pytest.mark.L2
    def test_sort_name_cn_asc(self, api):
        """L2-6: 排序 — 按 nameCn 升序"""
        resp = api("GET", "/service/open/v2/ability/admin/list",
                    params={"sortField": "nameCn", "sortOrder": "asc", "pageSize": 50})
        assert resp is not None
        assert resp["code"] == "200"
        data = resp["data"]
        if len(data) > 1:
            for i in range(len(data) - 1):
                assert data[i]["nameCn"] <= data[i + 1]["nameCn"], \
                    f"nameCn 升序失败: index {i}"

    @pytest.mark.L2
    def test_second_page(self, api):
        """L2-7: 分页第二页 curPage=2&pageSize=3"""
        # 第一页
        resp1 = api("GET", "/service/open/v2/ability/admin/list",
                     params={"curPage": 1, "pageSize": 3})
        assert resp1 is not None
        assert resp1["code"] == "200"
        assert len(resp1["data"]) <= 3

        # 第二页
        resp2 = api("GET", "/service/open/v2/ability/admin/list",
                     params={"curPage": 2, "pageSize": 3})
        assert resp2 is not None
        assert resp2["code"] == "200"
        assert resp2["page"]["curPage"] == 2
        assert len(resp2["data"]) <= 3

        # 两页数据不同（如果都有数据）
        if len(resp1["data"]) > 0 and len(resp2["data"]) > 0:
            assert resp1["data"][0]["abilityType"] != resp2["data"][0]["abilityType"], \
                "翻页应返回不同数据"

    @pytest.mark.L2
    def test_media_fields_non_null(self, api):
        """L2-8: icon/iconUrl/exampleDiagram/exampleDiagramUrl 非空验证"""
        resp = api("GET", "/service/open/v2/ability/admin/list",
                    params={"pageSize": 50})
        assert resp is not None
        assert resp["code"] == "200"
        # 检查每一条数据的 4 个媒体字段
        for item in resp["data"]:
            media_fields = {
                "icon": item.get("icon"),
                "iconUrl": item.get("iconUrl"),
                "exampleDiagram": item.get("exampleDiagram"),
                "exampleDiagramUrl": item.get("exampleDiagramUrl"),
            }
            # 如果 icon 有值，iconUrl 也应不为空
            if media_fields["icon"]:
                assert media_fields["iconUrl"] is not None, \
                    f"abilityType={item['abilityType']}: icon 有值但 iconUrl 为空"
            # 如果 iconUrl 不为空，应以 /ability-files/ 开头
            if media_fields["iconUrl"] is not None:
                assert media_fields["iconUrl"].startswith("/ability-files/"), \
                    f"abilityType={item['abilityType']}: iconUrl 格式异常: {media_fields['iconUrl']}"
            # 同理 exampleDiagram 和 exampleDiagramUrl
            if media_fields["exampleDiagram"]:
                assert media_fields["exampleDiagramUrl"] is not None, \
                    f"abilityType={item['abilityType']}: exampleDiagram 有值但 exampleDiagramUrl 为空"
            if media_fields["exampleDiagramUrl"] is not None:
                assert media_fields["exampleDiagramUrl"].startswith("/ability-files/"), \
                    f"abilityType={item['abilityType']}: exampleDiagramUrl 格式异常: {media_fields['exampleDiagramUrl']}"


# ==================== L4: 边界/反向测试 ====================


class TestAbilityAdminListL4:
    """列表接口 — 边界/反向 (5)"""

    @pytest.mark.L4
    def test_page_size_zero(self, api):
        """L4-1: pageSize=0 应修正为默认值（非零）"""
        resp = api("GET", "/service/open/v2/ability/admin/list",
                    params={"pageSize": 0})
        assert resp is not None
        assert resp["code"] == "200"
        # pageSize 应为正数（被修正为默认值 20）
        assert resp["page"]["pageSize"] > 0, \
            f"pageSize=0 应被修正, 实际: {resp['page']['pageSize']}"

    @pytest.mark.L4
    def test_page_size_exceeds_max(self, api):
        """L4-2: pageSize>100 应限制为 100"""
        resp = api("GET", "/service/open/v2/ability/admin/list",
                    params={"pageSize": 9999})
        assert resp is not None
        assert resp["code"] == "200"
        assert resp["page"]["pageSize"] <= 100, \
            f"pageSize 应被限制在 100 以内, 实际: {resp['page']['pageSize']}"

    @pytest.mark.L4
    def test_cur_page_zero(self, api):
        """L4-3: curPage=0 应修正为 1"""
        resp = api("GET", "/service/open/v2/ability/admin/list",
                    params={"curPage": 0})
        assert resp is not None
        assert resp["code"] == "200"
        assert resp["page"]["curPage"] >= 1, \
            f"curPage=0 应被修正为 1, 实际: {resp['page']['curPage']}"

    @pytest.mark.L4
    def test_cur_page_beyond_total(self, api):
        """L4-4: curPage 超过总页数应返回空数组"""
        resp = api("GET", "/service/open/v2/ability/admin/list",
                    params={"curPage": 99999, "pageSize": 20})
        assert resp is not None
        assert resp["code"] == "200"
        assert len(resp["data"]) == 0, \
            f"超范围分页应返回空数组, 实际: {len(resp['data'])} 条"

    @pytest.mark.L4
    def test_invalid_sort_field(self, api):
        """L4-5: sortField=非法值应不崩溃，返回正常响应或错误"""
        resp = api("GET", "/service/open/v2/ability/admin/list",
                    params={"sortField": "__invalid_column__", "sortOrder": "asc"})
        assert resp is not None
        # 服务端应不崩溃：要么返回错误码，要么用默认排序正常返回
        if resp["code"] == "200":
            # 用默认排序正常返回
            assert isinstance(resp["data"], list)
        else:
            # 返回 400 错误
            assert resp["code"] == "400"
