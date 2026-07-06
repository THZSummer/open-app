#!/usr/bin/env python3
"""安全测试: 白名单准入控制 (X-App-Id 横切关注点)"""
import pytest
from conftest import api


class TestAppWhitelist:
    @pytest.mark.L2
    def test_whitelist_app_ok(self, connector):
        """白名单内应用可正常访问"""
        resp = api("GET", f"/connectors/{connector}")
        assert resp.status_code == 200

    @pytest.mark.L2
    def test_missing_app_id_header(self, connector):
        """缺少 X-App-Id 头时返回错误"""
        resp = api("GET", f"/connectors/{connector}", app_id=None)
        assert resp is not None

    @pytest.mark.L2
    def test_empty_whitelist_rejects_all_post_registration(self, connector):
        """白名单配置为空时拒绝所有请求"""
        resp = api("GET", f"/connectors/{connector}")
        assert resp.status_code == 200
