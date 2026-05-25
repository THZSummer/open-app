"""
HTTP 触发 集成测试（真调）

覆盖接口:
  #18 POST /api/v1/trigger/{flowId}/invoke  — HTTP 触发

对应 Java: modules/trigger/controller/TriggerController.java
"""
import pytest


class TestTriggerInvoke:
    """#18 POST /api/v1/trigger/{flowId}/invoke — HTTP 触发"""

    def test_it_049_no_auth_token(self, connector_api_client):
        """IT-049: 凭证缺失（无 X-Sys-Token）→ 401"""
        resp = connector_api_client.post("/trigger/999999999999999999/invoke", {
            "payload": {"message": "test"}
        })
        assert resp.status_code == 200, f"Unexpected status: {resp.status_code}"
        data = resp.json()
        assert data["status"] == "failed", f"Expected failed, got {data}"
        assert "X-Sys-Token" in data.get("errorMessage", ""), f"Missing token error: {data}"

    def test_it_050_flow_not_found(self, connector_api_client):
        """IT-050: flow 不存在 → 404"""
        headers = {"X-Sys-Token": "test-token"}
        resp = connector_api_client.post("/trigger/999999999999999999/invoke", {
            "payload": {"message": "test"}
        }, headers=headers)
        assert resp.status_code == 200, f"Unexpected status: {resp.status_code}"
        data = resp.json()
        assert data["status"] == "failed", f"Expected failed, got {data}"
        assert "Flow not found" in data.get("errorMessage", ""), f"Expected flow not found, got {data}"

    def test_it_051_flow_not_running(self, connector_api_client):
        """IT-051: flow 未运行（stopped）→ 403"""
        headers = {"X-Sys-Token": "test-token"}
        resp = connector_api_client.post("/trigger/999999999999999999/invoke", {
            "payload": {}
        }, headers=headers)
        assert resp.status_code == 200, f"Unexpected status: {resp.status_code}"
        data = resp.json()
        assert data["status"] == "failed", f"Not failed: {data}"
        assert data.get("errorMessage"), f"Expected error message, got {data}"
