"""
pytest 全局配置 — connector-api / open-server 集成测试

由于 connector-api (port 18180) 通常未运行，默认指向 open-server (port 18080)。
触发接口测试 (#18) 在 connector-api 未运行时会自动跳过。
"""
import pytest
import requests

BASE_URL = "http://localhost:18080/open-server"
API_PREFIX = f"{BASE_URL}/api/v1"


class ApiClient:
    def __init__(self, base_url):
        self.base_url = base_url
        self.session = requests.Session()
        self.session.headers.update({"Content-Type": "application/json"})

    def post(self, path, json_data=None, headers=None):
        url = f"{self.base_url}{path}"
        req_headers = self.session.headers.copy()
        if headers:
            req_headers.update(headers)
        return self.session.post(url, json=json_data, headers=req_headers)

    def get(self, path, params=None):
        url = f"{self.base_url}{path}"
        return self.session.get(url, params=params)


@pytest.fixture(scope="session")
def api_client():
    return ApiClient(API_PREFIX)


def pytest_runtest_setup(item):
    """自动跳过需要 connector-api 的测试"""
    if 'trigger' in str(item.fspath):
        try:
            r = requests.get("http://localhost:18180/actuator/health", timeout=2)
            if not r.ok:
                pytest.skip("connector-api 未运行 (port 18180)")
        except Exception:
            pytest.skip("connector-api 未运行 (port 18180)")
