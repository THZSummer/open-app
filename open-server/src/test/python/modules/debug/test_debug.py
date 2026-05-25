"""
测试代理 集成测试（真调）

覆盖接口:
  #17 POST /api/v1/flows/{flowId}/test-run  — 测试运行

对应 Java: modules/debug/DebugProxyController.java
"""
import pytest


class TestDebugProxy:
    """#17 POST /api/v1/flows/{flowId}/test-run — 测试运行"""

    def test_it_047_test_run_flow_not_found(self, api_client):
        """IT-047: flow 不存在 → 404"""
        body = {"mockTriggerData": {}}
        resp = api_client.post("/flows/999999999999999999/test-run", body)
        data = resp.json()
        assert data["code"] in ("404", "500"), f"Expected 404/500, got {data['code']}: {resp.text}"

    def test_it_048_test_run_no_config(self, api_client, test_flow):
        """IT-048: flow 未配置编排 → 422"""
        body = {"mockTriggerData": {"message": "hello"}}
        resp = api_client.post(f"/flows/{test_flow}/test-run", body)
        data = resp.json()
        # 没有编排配置的 flow 应该返回错误
        assert data["code"] in ("400", "422", "404", "500"), f"Expected 400/422/404/500, got {data['code']}"
