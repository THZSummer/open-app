"""
连接流 CRUD + 启停 + 配置 集成测试（真调）

覆盖接口:
  #8  POST   /api/v1/flows                  — 创建连接流
  #9  GET    /api/v1/flows                  — 查询列表
  #10 GET    /api/v1/flows/{id}             — 查询详情
  #11 PUT    /api/v1/flows/{id}             — 更新
  #12 DELETE /api/v1/flows/{id}             — 删除
  #13 POST   /api/v1/flows/{id}/start       — 启动
  #14 POST   /api/v1/flows/{id}/stop        — 停止
  #15 GET    /api/v1/flows/{id}/config      — 获取编排配置
  #16 PUT    /api/v1/flows/{id}/config      — 保存编排配置

对应 Java: modules/flow/controller/FlowController.java
"""
import pytest


class TestCreateFlow:
    """#8 POST /api/v1/flows — 创建连接流"""

    def test_it_023_create_success(self, api_client):
        """IT-023: 正常创建"""
        body = {
            "nameCn": "集成测试-自动通知流",
            "nameEn": "Integration Test Auto Notification"
        }
        resp = api_client.post("/flows", body)
        assert resp.status_code == 200, f"Expected 200, got {resp.status_code}: {resp.text}"
        data = resp.json()
        assert data["code"] == "200"
        assert isinstance(data["data"]["id"], str), f"id should be string, got {type(data['data']['id'])}"

    def test_it_024_missing_name_cn(self, api_client):
        """IT-024: 缺少必填 nameCn → 400"""
        body = {"nameEn": "Test Missing NameCn"}
        resp = api_client.post("/flows", body)
        assert resp.status_code == 400
        data = resp.json()
        assert data["code"] == "400"

    def test_it_025_missing_name_en(self, api_client):
        """IT-025: 缺少必填 nameEn → 400"""
        body = {"nameCn": "测试缺少英文名"}
        resp = api_client.post("/flows", body)
        assert resp.status_code == 400
        data = resp.json()
        assert data["code"] == "400"


class TestListFlows:
    """#9 GET /api/v1/flows — 查询流列表"""

    def test_it_026_default_page(self, api_client):
        """IT-026: 默认分页"""
        resp = api_client.get("/flows")
        assert resp.status_code == 200
        data = resp.json()
        assert data["code"] == "200"
        assert isinstance(data["data"], list)
        assert data["page"]["curPage"] == 1
        assert data["page"]["pageSize"] == 20
        assert isinstance(data["page"]["total"], (int, float))

    def test_it_027_filter_by_status(self, api_client):
        """IT-027: lifecycleStatus 过滤"""
        resp = api_client.get("/flows", params={"lifecycleStatus": 0})
        assert resp.status_code == 200
        data = resp.json()
        assert data["code"] == "200"

    def test_it_028_search_by_keyword(self, api_client):
        """IT-028: keyword 搜索"""
        resp = api_client.get("/flows", params={"keyword": "集成测试"})
        assert resp.status_code == 200
        data = resp.json()
        assert data["code"] == "200"

    def test_it_029_empty_result(self, api_client):
        """IT-029: 空结果"""
        resp = api_client.get("/flows", params={"keyword": "NONEXISTENT_FLOW_9999"})
        assert resp.status_code == 200
        data = resp.json()
        assert data["code"] == "200"
        assert data["page"]["total"] == 0
        assert len(data["data"]) == 0


class TestGetFlowDetail:
    """#10 GET /api/v1/flows/{flowId} — 查看流详情"""

    def test_it_030_detail_success(self, api_client, test_flow):
        """IT-030: 正常查询"""
        resp = api_client.get(f"/flows/{test_flow}")
        assert resp.status_code == 200
        data = resp.json()
        assert data["code"] == "200"
        detail = data["data"]
        assert detail["id"] == str(test_flow)
        assert isinstance(detail["nameCn"], str)
        assert isinstance(detail["lifecycleStatus"], int)

    def test_it_031_detail_not_found(self, api_client):
        """IT-031: flowId 不存在 → 404"""
        resp = api_client.get("/flows/9999999999999999999")
        data = resp.json()
        assert data["code"] == "404"


class TestUpdateFlow:
    """#11 PUT /api/v1/flows/{flowId} — 更新流信息"""

    def test_it_032_update_success(self, api_client, test_flow):
        """IT-032: 正常更新"""
        body = {"nameCn": "集成测试更新后的流名称"}
        resp = api_client.put(f"/flows/{test_flow}", body)
        assert resp.status_code == 200
        data = resp.json()
        assert data["code"] == "200"

    def test_it_033_update_not_found(self, api_client):
        """IT-033: flowId 不存在 → 404"""
        body = {"nameCn": "测试更新不存在"}
        resp = api_client.put("/flows/9999999999999999999", body)
        data = resp.json()
        assert data["code"] == "404"


class TestDeleteFlow:
    """#12 DELETE /api/v1/flows/{flowId} — 删除流"""

    def test_it_034_delete_stopped(self, api_client, db_conn):
        """IT-034: stopped 状态可删除"""
        from conftest import insert_test_flow, delete_test_flow
        flow_id = insert_test_flow(db_conn)
        try:
            resp = api_client.delete(f"/flows/{flow_id}")
            assert resp.status_code == 200
            data = resp.json()
            assert data["code"] == "200"
        finally:
            try:
                delete_test_flow(db_conn, flow_id)
            except Exception:
                pass

    def test_it_035_delete_not_found(self, api_client):
        """IT-035: flowId 不存在 → 404"""
        resp = api_client.delete("/flows/9999999999999999999")
        data = resp.json()
        assert data["code"] == "404"


class TestStartFlow:
    """#13 POST /api/v1/flows/{flowId}/start — 启动"""

    def test_it_036_start_success(self, api_client, test_flow):
        """IT-036: stopped → running"""
        resp = api_client.post(f"/flows/{test_flow}/start")
        assert resp.status_code == 200
        data = resp.json()
        assert data["code"] == "200"

    def test_it_037_start_already_running(self, api_client, test_flow):
        """IT-037: 已是 running（重复启动）"""
        # 先启动一次
        api_client.post(f"/flows/{test_flow}/start")
        # 再次启动应返回 409
        resp = api_client.post(f"/flows/{test_flow}/start")
        data = resp.json()
        assert data["code"] in ("400", "409"), f"Expected 400/409 for duplicate start, got {data['code']}"

    def test_it_038_start_not_found(self, api_client):
        """IT-038: flowId 不存在 → 404"""
        resp = api_client.post("/flows/9999999999999999999/start")
        data = resp.json()
        assert data["code"] == "404"


class TestStopFlow:
    """#14 POST /api/v1/flows/{flowId}/stop — 停止"""

    def test_it_039_stop_success(self, api_client, test_flow):
        """IT-039: running → stopped"""
        # 先启动
        api_client.post(f"/flows/{test_flow}/start")
        # 再停止
        resp = api_client.post(f"/flows/{test_flow}/stop")
        assert resp.status_code == 200
        data = resp.json()
        assert data["code"] == "200"

    def test_it_040_stop_already_stopped(self, api_client, test_flow):
        """IT-040: 已是 stopped"""
        resp = api_client.post(f"/flows/{test_flow}/stop")
        data = resp.json()
        assert data["code"] in ("400", "409"), f"Expected 400/409 for duplicate stop, got {data['code']}"

    def test_it_041_stop_not_found(self, api_client):
        """IT-041: flowId 不存在 → 404"""
        resp = api_client.post("/flows/9999999999999999999/stop")
        data = resp.json()
        assert data["code"] == "404"


class TestGetFlowConfig:
    """#15 GET /api/v1/flows/{flowId}/config — 获取编排配置"""

    def test_it_042_config_empty(self, api_client, test_flow):
        """IT-042: 空配置（初始状态）"""
        resp = api_client.get(f"/flows/{test_flow}/config")
        assert resp.status_code == 200
        data = resp.json()
        assert data["code"] == "200"
        assert "hasConfig" in data["data"]

    def test_it_043_config_not_found(self, api_client):
        """IT-043: flowId 不存在 → 404"""
        resp = api_client.get("/flows/9999999999999999999/config")
        data = resp.json()
        assert data["code"] == "404"


class TestUpdateFlowConfig:
    """#16 PUT /api/v1/flows/{flowId}/config — 保存编排配置"""

    def test_it_044_update_config_success(self, api_client, test_flow):
        """IT-044: 正常保存编排配置"""
        body = {
            "orchestrationConfig": '{"nodes":[],"edges":[]}'
        }
        resp = api_client.put(f"/flows/{test_flow}/config", body)
        assert resp.status_code == 200
        data = resp.json()
        assert data["code"] == "200"

    def test_it_045_update_config_empty_string(self, api_client, test_flow):
        """IT-045: orchestrationConfig 为空字符串 → 400"""
        body = {"orchestrationConfig": ""}
        resp = api_client.put(f"/flows/{test_flow}/config", body)
        assert resp.status_code == 400
        data = resp.json()
        assert data["code"] == "400"

    def test_it_046_update_config_null(self, api_client, test_flow):
        """IT-046: orchestrationConfig 为 null → 400"""
        body = {}
        resp = api_client.put(f"/flows/{test_flow}/config", body)
        assert resp.status_code == 400
        data = resp.json()
        assert data["code"] == "400"
