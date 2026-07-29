#!/usr/bin/env python3
"""共享 fixtures — wecodesite E2E Playwright 测试"""

import pytest


@pytest.fixture(scope="session")
def app_id() -> str:
    """测试应用 ID"""
    return "20250730213114178360970"


@pytest.fixture(scope="session")
def base_url() -> str:
    """前端页面 base URL (Vite dev server)"""
    return "http://192.168.3.110:5173"


@pytest.fixture(scope="session")
def browser_context_args():
    """浏览器上下文参数"""
    return {
        "viewport": {"width": 1920, "height": 1080},
        "ignore_https_errors": True,
    }


@pytest.fixture(scope="function")
def flow_list_url(base_url: str, app_id: str) -> str:
    """连接流列表页面"""
    return f"{base_url}/#/flowList?appId={app_id}"


@pytest.fixture(scope="function")
def flow_editor_url(base_url: str, app_id: str) -> str:
    """连接流编辑器 V2 (需带上 ?id={flowId})"""
    return f"{base_url}/#/flowEditor?appId={app_id}"


@pytest.fixture(scope="function")
def connector_list_url(base_url: str, app_id: str) -> str:
    """连接器列表页面"""
    return f"{base_url}/#/connectorList?appId={app_id}"


@pytest.fixture(scope="function")
def connector_editor_url(base_url: str, app_id: str) -> str:
    """连接器编辑器 (需带上 ?id={connectorId})"""
    return f"{base_url}/#/connectorEditor?appId={app_id}"
