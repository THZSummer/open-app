#!/usr/bin/env python3
"""共享 fixtures — 集成测试共享资源"""

import pytest
from common.client import api, db, db_val

# 会话级环境检查
@pytest.fixture(scope="session", autouse=True)
def check_environment():
    """检查 DB 连通性和基础表结构"""
    result = db_val("SELECT COUNT(1) FROM openplatform_ability_t")
    assert result is not None, "DB 连接失败或 openplatform_ability_t 表不存在"
    print(f"  [环境] openplatform_ability_t 表记录数: {result}")
    yield


@pytest.fixture(scope="function")
def api():
    """API 客户端 fixture — 测试方法中直接调用 api(method, path, ...)"""
    from common.client import api as _api
    return _api
