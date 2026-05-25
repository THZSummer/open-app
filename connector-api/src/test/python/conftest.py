"""
pytest 全局配置 — connector-api 集成测试

提供 BASE_URL = http://localhost:18180
"""
import pytest
import requests

BASE_URL = "http://localhost:18180"
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

    def get(self, path):
        url = f"{self.base_url}{path}"
        return self.session.get(url)


@pytest.fixture(scope="session")
def api_client():
    return ApiClient(API_PREFIX)
