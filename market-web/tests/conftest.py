#!/usr/bin/env python3
"""共享 fixtures — E2E Playwright 测试共享资源"""

import pytest


@pytest.fixture(scope="session")
def base_url() -> str:
    """测试页面 base URL"""
    return "http://localhost:13000/market-web"


@pytest.fixture(scope="session")
def browser_context_args():
    """浏览器上下文参数"""
    return {
        "viewport": {"width": 1920, "height": 1080},
        "ignore_https_errors": True,
    }


@pytest.fixture(scope="function")
def ability_admin_url(base_url: str) -> str:
    """能力管理页面地址"""
    return f"{base_url}/#/ability-admin"
