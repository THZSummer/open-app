"""
用户角色查询接口集成测试

测试 POST /service/open/v2/internal/user/roles 接口

测试层级：
  L1 - 正常流程（appId 查询、hisAppId 查询）
  L2 - 异常流程（参数缺失、应用不存在）
  L4 - 凭证校验（无凭证 → 401、不在白名单 → 403）

运行方式：
  pytest test_user_roles.py -v
"""

import requests
import os

BASE_URL = os.environ.get("API_SERVER_URL", "http://localhost:18081/api-server")
INTERNAL_PATH = "/service/open/v2/internal/user/roles"
URL = f"{BASE_URL}{INTERNAL_PATH}"

VALID_TOKEN = "dev-token-001"
INVALID_TOKEN = "invalid-token-xxx"


def _headers(token=None):
    headers = {"Content-Type": "application/json"}
    if token:
        headers["X-Internal-Token"] = token
    return headers


class TestUserRolesL1:
    """L1 - 正常流程"""

    def test_query_by_app_id(self):
        payload = {
            "appId": "202606140029550001",
            "userAccount": "admin"
        }
        resp = requests.post(URL, json=payload, headers=_headers(VALID_TOKEN))
        assert resp.status_code == 200
        body = resp.json()
        assert body["code"] == "200"
        assert body["data"] is not None
        assert body["data"]["appId"] == "202606140029550001"
        assert isinstance(body["data"]["roles"], list)
        assert len(body["data"]["roles"]) > 0
        print(f"[L1] test_query_by_app_id PASS: roles={body['data']['roles']}")

    def test_query_by_his_app_id(self):
        payload = {
            "hisAppId": "eamap_expense_006",
            "userAccount": "user_001"
        }
        resp = requests.post(URL, json=payload, headers=_headers(VALID_TOKEN))
        assert resp.status_code == 200
        body = resp.json()
        assert body["code"] == "200"
        assert body["data"] is not None
        assert isinstance(body["data"]["appId"], str)
        assert len(body["data"]["appId"]) > 0
        assert isinstance(body["data"]["roles"], list)
        assert len(body["data"]["roles"]) > 0
        print(f"[L1] test_query_by_his_app_id PASS: appId={body['data']['appId']}, roles={body['data']['roles']}")

    def test_query_user_no_roles(self):
        payload = {
            "appId": "202606140029550001",
            "userAccount": "noone@xxx.com"
        }
        resp = requests.post(URL, json=payload, headers=_headers(VALID_TOKEN))
        assert resp.status_code == 200
        body = resp.json()
        assert body["code"] == "200"
        assert body["data"] is not None
        assert body["data"]["appId"] == "202606140029550001"
        assert isinstance(body["data"]["roles"], list)
        assert len(body["data"]["roles"]) == 0
        print("[L1] test_query_user_no_roles PASS: empty roles returned")


class TestUserRolesL2:
    """L2 - 异常流程"""

    def test_missing_app_identifier(self):
        payload = {
            "userAccount": "test@xxx.com"
        }
        resp = requests.post(URL, json=payload, headers=_headers(VALID_TOKEN))
        assert resp.status_code == 200
        body = resp.json()
        assert body["code"] == "400"
        assert "appId 和 hisAppId" in body["messageZh"]
        print("[L2] test_missing_app_identifier PASS")

    def test_missing_user_account(self):
        payload = {
            "appId": "202606140029550001"
        }
        resp = requests.post(URL, json=payload, headers=_headers(VALID_TOKEN))
        assert resp.status_code == 200
        body = resp.json()
        assert body["code"] == "400"
        assert "用户账号" in body["messageZh"] or "userAccount" in body["messageZh"]
        print("[L2] test_missing_user_account PASS")

    def test_app_not_found(self):
        payload = {
            "appId": "99999999999999999999",
            "userAccount": "test@xxx.com"
        }
        resp = requests.post(URL, json=payload, headers=_headers(VALID_TOKEN))
        assert resp.status_code == 200
        body = resp.json()
        assert body["code"] == "404"
        assert "应用不存在" in body["messageZh"]
        print("[L2] test_app_not_found PASS")

    def test_both_app_id_and_his_app_id_not_found(self):
        payload = {
            "appId": "00000000000000000000",
            "hisAppId": "eamap_nonexistent_xxx",
            "userAccount": "test@xxx.com"
        }
        resp = requests.post(URL, json=payload, headers=_headers(VALID_TOKEN))
        assert resp.status_code == 200
        body = resp.json()
        assert body["code"] == "404"
        assert "应用不存在" in body["messageZh"]
        print("[L2] test_both_app_id_and_his_app_id_not_found PASS")


class TestUserRolesL4:
    """L4 - 凭证校验（对齐 connector SysToken 三阶段）"""

    def test_no_token(self):
        """无凭证 → 401"""
        payload = {
            "appId": "202606140029550001",
            "userAccount": "admin"
        }
        resp = requests.post(URL, json=payload, headers=_headers(None))
        assert resp.status_code == 200
        body = resp.json()
        assert body["code"] == "401"
        assert "凭证" in body["messageZh"]
        print("[L4] test_no_token PASS: 401")

    def test_invalid_token(self):
        """不在白名单 → 403（token 可解析但不允许）"""
        payload = {
            "appId": "202606140029550001",
            "userAccount": "admin"
        }
        resp = requests.post(URL, json=payload, headers=_headers(INVALID_TOKEN))
        assert resp.status_code == 200
        body = resp.json()
        assert body["code"] == "403"
        assert "权限" in body["messageZh"] or "白名单" in body["messageZh"] or "Access denied" in body["messageEn"]
        print("[L4] test_invalid_token PASS: 403")
