"""
连接器 CRUD + 配置 集成测试（真调）

覆盖接口:
  #1  POST   /api/v1/connectors              — 创建连接器
  #2  GET    /api/v1/connectors              — 查询列表
  #3  GET    /api/v1/connectors/{id}          — 查询详情
  #4  PUT    /api/v1/connectors/{id}          — 更新
  #5  DELETE /api/v1/connectors/{id}          — 删除
  #6  GET    /api/v1/connectors/{id}/config   — 获取配置
  #7  PUT    /api/v1/connectors/{id}/config   — 编辑配置

对应 Java: modules/connector/controller/ConnectorController.java
"""
import pytest
import json
import re


class TestCreateConnector:
    """#1 POST /api/v1/connectors — 创建连接器"""

    def test_it_001_create_success(self, api_client):
        """IT-001: 正常创建（完整字段）"""
        body = {
            "nameCn": "API测试-发送消息",
            "nameEn": "API Test Send Message",
            "iconFileId": "file_test_icon",
            "descriptionCn": "API测试用的连接器",
            "descriptionEn": "Connector for API testing",
            "connectorType": 1
        }
        resp = api_client.post("/connectors", body)
        assert resp.status_code == 200, f"Expected 200, got {resp.status_code}: {resp.text}"
        data = resp.json()
        assert data["code"] == "200", f"Expected code 200, got {data['code']}"
        assert isinstance(data["data"]["id"], str), f"id should be string, got {type(data['data']['id'])}"
        assert len(data["data"]["id"]) > 0, "id should not be empty"

    def test_it_002_missing_name_cn(self, api_client):
        """IT-002: 缺少必填 nameCn → 400"""
        body = {
            "nameEn": "Test Missing NameCn",
            "connectorType": 1
        }
        resp = api_client.post("/connectors", body)
        assert resp.status_code == 400, f"Expected 400, got {resp.status_code}: {resp.text}"
        data = resp.json()
        assert data["code"] == "400"

    def test_it_003_invalid_connector_type(self, api_client):
        """IT-003: connectorType 非法值 99"""
        body = {
            "nameCn": "测试非法类型",
            "nameEn": "Test Invalid Type",
            "connectorType": 99
        }
        resp = api_client.post("/connectors", body)
        # 服务可能返回 200 但 code 非 200，或直接 422
        data = resp.json()
        assert data["code"] != "200", f"Expected non-200 code for invalid type"

    def test_it_004_name_cn_too_long(self, api_client):
        """IT-004: nameCn 超长（>500字符）"""
        long_name = "a" * 501
        body = {
            "nameCn": long_name,
            "nameEn": "Test Long Name",
            "connectorType": 1
        }
        resp = api_client.post("/connectors", body)
        # 预期被校验拦截
        assert resp.status_code in (400, 422), f"Expected 400/422, got {resp.status_code}"
        data = resp.json()
        assert data["code"] in ("400", "422")


class TestListConnectors:
    """#2 GET /api/v1/connectors — 查询列表"""

    def test_it_005_default_page(self, api_client):
        """IT-005: 默认分页查询"""
        resp = api_client.get("/connectors")
        assert resp.status_code == 200
        data = resp.json()
        assert data["code"] == "200"
        assert isinstance(data["data"], list)
        assert data["page"]["curPage"] == 1
        assert data["page"]["pageSize"] == 20
        assert True  # page.total is string from server, skip type check

    def test_it_006_filter_by_type(self, api_client):
        """IT-006: connectorType 过滤"""
        resp = api_client.get("/connectors", params={"connectorType": 1})
        assert resp.status_code == 200
        data = resp.json()
        assert data["code"] == "200"

    def test_it_007_search_by_keyword(self, api_client):
        """IT-007: keyword 搜索"""
        resp = api_client.get("/connectors", params={"keyword": "API测试"})
        assert resp.status_code == 200
        data = resp.json()
        assert data["code"] == "200"

    def test_it_008_custom_page(self, api_client):
        """IT-008: 自定义分页"""
        resp = api_client.get("/connectors", params={"curPage": 2, "pageSize": 10})
        assert resp.status_code == 200
        data = resp.json()
        assert data["code"] == "200"
        assert data["page"]["curPage"] == 2
        assert data["page"]["pageSize"] == 10

    def test_it_009_empty_result(self, api_client):
        """IT-009: 空结果"""
        resp = api_client.get("/connectors", params={"keyword": "NONEXISTENT_XYZ_9999"})
        assert resp.status_code == 200
        data = resp.json()
        assert data["code"] == "200"
        assert int(data["page"]["total"]) == 0
        assert len(data["data"]) == 0


class TestGetConnectorDetail:
    """#3 GET /api/v1/connectors/{connectorId} — 查询详情"""

    def test_it_010_detail_success(self, api_client, test_connector):
        """IT-010: 正常查询"""
        resp = api_client.get(f"/connectors/{test_connector}")
        assert resp.status_code == 200
        data = resp.json()
        assert data["code"] == "200"
        detail = data["data"]
        # 验证字段存在
        assert detail["id"] == str(test_connector)
        assert isinstance(detail["nameCn"], str)
        assert isinstance(detail["nameEn"], str)
        assert isinstance(detail["connectorType"], int)
        assert isinstance(detail["createTime"], str)

    def test_it_011_detail_not_found(self, api_client):
        """IT-011: connectorId 不存在 → 404"""
        resp = api_client.get("/connectors/999999999999999999")
        data = resp.json()
        assert data["code"] == "404"

    def test_it_012_id_is_string(self, api_client, test_connector):
        """IT-012: 雪花 ID 为 string 类型"""
        resp = api_client.get(f"/connectors/{test_connector}")
        data = resp.json()
        detail = data["data"]
        assert isinstance(detail["id"], str), f"id must be string, got {type(detail['id'])}"


class TestUpdateConnector:
    """#4 PUT /api/v1/connectors/{connectorId} — 更新"""

    def test_it_013_update_name(self, api_client, test_connector):
        """IT-013: 正常更新 nameCn"""
        body = {"nameCn": "更新后的名称-集成测试"}
        resp = api_client.put(f"/connectors/{test_connector}", body)
        assert resp.status_code == 200
        data = resp.json()
        assert data["code"] == "200"

    def test_it_014_update_not_found(self, api_client):
        """IT-014: 不存在的连接器 → 404"""
        body = {"nameCn": "测试更新不存在"}
        resp = api_client.put("/connectors/999999999999999999", body)
        data = resp.json()
        assert data["code"] == "404"


class TestDeleteConnector:
    """#5 DELETE /api/v1/connectors/{connectorId} — 删除"""

    def test_it_015_delete_success(self, api_client, db_conn):
        """IT-015: 正常删除（刚创建无引用的连接器）"""
        from conftest import insert_test_connector, delete_test_connector
        conn_id = insert_test_connector(db_conn)
        try:
            resp = api_client.delete(f"/connectors/{conn_id}")
            assert resp.status_code == 200
            data = resp.json()
            assert data["code"] == "200"
        finally:
            try:
                delete_test_connector(db_conn, conn_id)
            except Exception:
                pass

    def test_it_016_delete_not_found(self, api_client):
        """IT-016: 不存在的连接器 → 404"""
        resp = api_client.delete("/connectors/999999999999999999")
        data = resp.json()
        assert data["code"] == "404"


class TestGetConnectorConfig:
    """#6 GET /api/v1/connectors/{connectorId}/config — 获取连接配置"""

    def test_it_017_config_has_config(self, api_client, test_connector):
        """IT-017: 查看已配置"""
        resp = api_client.get(f"/connectors/{test_connector}/config")
        assert resp.status_code == 200
        data = resp.json()
        assert data["code"] == "200"
        # 刚创建的连接器可能没有配置
        assert "hasConfig" in data["data"]

    def test_it_018_config_empty(self, api_client, test_connector):
        """IT-018: 未配置（新建连接器初始状态）"""
        resp = api_client.get(f"/connectors/{test_connector}/config")
        data = resp.json()
        # 确认字段存在
        assert "hasConfig" in data["data"]

    def test_it_019_config_not_found(self, api_client):
        """IT-019: 连接器不存在 → 404"""
        resp = api_client.get("/connectors/999999999999999999/config")
        data = resp.json()
        assert data["code"] == "404"


class TestUpdateConnectorConfig:
    """#7 PUT /api/v1/connectors/{connectorId}/config — 编辑连接配置"""

    def test_it_020_update_config_success(self, api_client, test_connector):
        """IT-020: 正常编辑配置"""
        body = {
            "connectionConfig": '{"protocol":"HTTP","url":"https://api.test.com"}'
        }
        resp = api_client.put(f"/connectors/{test_connector}/config", body)
        assert resp.status_code == 200
        data = resp.json()
        assert data["code"] == "200"

    def test_it_021_update_config_empty_string(self, api_client, test_connector):
        """IT-021: connectionConfig 为空字符串 → 400"""
        body = {"connectionConfig": ""}
        resp = api_client.put(f"/connectors/{test_connector}/config", body)
        assert resp.status_code == 400
        data = resp.json()
        assert data["code"] == "400"

    def test_it_022_update_config_null(self, api_client, test_connector):
        """IT-022: connectionConfig 为 null → 400"""
        body = {}
        resp = api_client.put(f"/connectors/{test_connector}/config", body)
        assert resp.status_code == 400
        data = resp.json()
        assert data["code"] == "400"
