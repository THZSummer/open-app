#!/usr/bin/env python3
"""L0 冒烟：connector-api 健康检查 (FR-health, plan-api §2.1)

验证服务基础连通性 — 每次 commit 运行，<5s。
"""
import pytest
import requests

CONNECTOR_API = "http://localhost:18180"


class TestHealth:
    """connector-api 健康检查"""

    @pytest.mark.L0
    def test_health_endpoint(self):
        """验证 /actuator/health 返回 UP"""
        resp = requests.get(f"{CONNECTOR_API}/actuator/health", timeout=5)
        assert resp.status_code == 200
        data = resp.json()
        assert data.get("status") == "UP"

    @pytest.mark.L0
    def test_trigger_endpoint_reachable(self):
        """验证触发端点可访问（404 = 服务在线 + flow 不存在）"""
        resp = requests.post(
            f"{CONNECTOR_API}/api/v1/trigger/999999999999999999/invoke",
            json={"sender": "test"},
            headers={"Content-Type": "application/json"},
            timeout=5
        )
        # 服务在线：返回 404 或 401（取决于认证配置）
        assert resp.status_code in (200, 401, 404), \
            f"Expected 200/401/404, got {resp.status_code}"
