#!/usr/bin/env python3
"""L0 冒烟：connector-api 健康检查 (FR-health, plan-api §2.1)

验证服务基础连通性 — 每次 commit 运行，<5s。
"""
import pytest
import requests
import os, sys
sys.path.insert(0, os.path.join(os.path.dirname(__file__), "inspect"))
from client import BASE_URL, trigger, _API_HOST


class TestHealth:
    """connector-api 健康检查"""

    @pytest.mark.L0
    def test_health_endpoint(self):
        """验证 /actuator/health 返回 UP"""
        resp = requests.get(f"http://{_API_HOST}/actuator/health", timeout=5)
        assert resp.status_code == 200
        data = resp.json()
        assert data.get("status") == "UP"

    @pytest.mark.L0
    def test_trigger_endpoint_reachable(self):
        """验证触发端点可访问（403 = 服务在线 + flow 不存在/未授权）"""
        resp = trigger(999999999999999999, body={"sender": "test"})
        assert resp is not None, "connector-api 未运行"
        assert resp.status_code in (200, 401, 403), \
            f"Expected 200/401/403, got {resp.status_code}"
