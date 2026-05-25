"""
API 响应格式契约测试（L4 — 真调验证）

验证所有 API 响应符合统一格式规范：
  { code, messageZh, messageEn, data, page }

以及字段级约束：
  - BIGINT ID → string 类型
  - 枚举字段 → TINYINT 数字
  - 时间字段 → ISO 8601 格式
  - camelCase 字段名
"""
import pytest
import re


ISO_8601_PATTERN = r"^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d{3}"
CAMEL_CASE_PATTERN = r"^[a-z]+[A-Za-z0-9]*$"


class TestContractResponseFormat:
    """L4 契约测试"""

    def test_it_052_success_response_format(self, api_client):
        """IT-052: 成功响应格式 {code, messageZh, messageEn, data, page}"""
        resp = api_client.get("/connectors")
        assert resp.status_code == 200
        body = resp.json()

        assert "code" in body, "缺少 code 字段"
        assert "messageZh" in body, "缺少 messageZh 字段"
        assert "messageEn" in body, "缺少 messageEn 字段"
        assert "data" in body, "缺少 data 字段"
        assert "page" in body, "缺少 page 字段"

        assert isinstance(body["code"], str), f"code 应为 string, 实际为 {type(body['code'])}"
        assert isinstance(body["messageZh"], str), f"messageZh 应为 string"
        assert isinstance(body["messageEn"], str), f"messageEn 应为 string"

    def test_it_053_error_response_format(self, api_client):
        """IT-053: 错误响应格式 code != 200, data: null"""
        resp = api_client.get("/connectors/999999999999999999")
        body = resp.json()

        assert body["code"] != "200", f"错误响应 code 不应为 200, got {body['code']}"
        assert body.get("data") is None or body.get("data") == "", f"错误响应 data 应为 null/空"
        assert isinstance(body["messageZh"], str) and len(body["messageZh"]) > 0

    def test_it_054_paged_response_format(self, api_client):
        """IT-054: 分页响应格式 page{curPage, pageSize, total}"""
        resp = api_client.get("/connectors")
        body = resp.json()
        page = body.get("page")

        if page is not None:
            assert "curPage" in page, "page 缺少 curPage"
            assert "pageSize" in page, "page 缺少 pageSize"
            assert "total" in page, "page 缺少 total"
            assert isinstance(page["curPage"], int), f"curPage 应为 int"
            assert isinstance(page["pageSize"], int), f"pageSize 应为 int"
            assert isinstance(page["total"], (int, float, str)), f"total 应为 number"

    def test_it_055_bigint_id_as_string(self, api_client):
        """IT-055: BIGINT ID 为 string 类型"""
        resp = api_client.get("/connectors")
        body = resp.json()
        if body["data"] and len(body["data"]) > 0:
            item = body["data"][0]
            for key in item:
                if key.endswith("id") or key == "id" or key.endswith("Id"):
                    assert isinstance(item[key], str),                         f"字段 '{key}' 值 {item[key]} 应为 string, 实际为 {type(item[key])}"

    def test_it_056_enum_as_tinyint(self, api_client):
        """IT-056: 枚举字段为 TINYINT 数字"""
        resp = api_client.get("/connectors")
        body = resp.json()
        if body["data"] and len(body["data"]) > 0:
            item = body["data"][0]
            enum_fields = ["connectorType", "lifecycleStatus", "status", "nodeType"]
            for field in enum_fields:
                if field in item:
                    assert isinstance(item[field], int),                         f"枚举字段 '{field}' 应为 int, 实际为 {type(item[field])}"

    def test_it_057_datetime_iso8601(self, api_client):
        """IT-057: 时间字段为 ISO 8601 格式"""
        resp = api_client.get("/connectors")
        body = resp.json()
        if body["data"] and len(body["data"]) > 0:
            item = body["data"][0]
            time_fields = ["createTime", "lastUpdateTime"]
            for field in time_fields:
                if field in item and item[field]:
                    value = str(item[field])
                    assert re.match(ISO_8601_PATTERN, value),                         f"时间字段 '{field}' = '{value}' 不符合 ISO 8601 格式"

    def test_it_058_camel_case_fields(self, api_client):
        """IT-058: 字段名为 camelCase 命名"""
        resp = api_client.get("/connectors")
        body = resp.json()
        if body["data"] and len(body["data"]) > 0:
            item = body["data"][0]
            for key in item.keys():
                assert re.match(CAMEL_CASE_PATTERN, key),                     f"字段名 '{key}' 不符合 camelCase 规范"

    def test_it_059_error_code_coverage(self, api_client):
        """IT-059: 错误码覆盖率验证"""
        expected_codes = ["400", "401", "403", "404", "409", "422", "429", "500"]
        resp = api_client.get("/connectors/999999999999999999")
        body = resp.json()
        assert body["code"] in expected_codes,             f"错误码 {body['code']} 不在预期集合 {expected_codes} 中"
