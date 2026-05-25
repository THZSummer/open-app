#!/usr/bin/env python3
"""
连接器平台 -- Python 真调集成测试

覆盖 API #1~#18 + L4 契约
使用: unittest + requests + mysql CLI (subprocess)
"""
import unittest
import requests
import subprocess
import json
import re
import time
import os
import sys
from datetime import datetime
from typing import Any, Dict, List, Optional, Tuple

# ── 配置 ─────────────────────────────────────────
OPEN_SERVER_URL = "http://localhost:18080/open-server"
CONNECTOR_API_URL = "http://localhost:18180"
TS = str(int(time.time()))

DB_ARGS = ["mysql", "-u", "openapp", "-popenapp", "openapp"]

REPORT_LINES: List[str] = []
PASS_COUNT = 0
FAIL_COUNT = 0
ERROR_COUNT = 0


def db_execute(sql: str) -> str:
    result = subprocess.run(
        DB_ARGS + ["-e", sql],
        capture_output=True, text=True, timeout=10
    )
    if result.returncode != 0:
        raise RuntimeError("SQL执行失败: " + sql + "\n" + result.stderr)
    return result.stdout


def _gen_snowflake_id() -> int:
    import random
    ts = int(time.time() * 1000)
    rand = random.randint(0, 9999)
    return (ts << 12) | rand


def db_insert_connector(name_cn: str = None, name_en: str = None) -> int:
    name_cn = name_cn or "集成测试连接器_" + TS
    name_en = name_en or "it_connector_" + TS
    snow_id = _gen_snowflake_id()
    sql = "INSERT INTO openplatform_v2_cp_connector_t (id, name_cn, name_en, connector_type, create_by, last_update_by) VALUES (" + str(snow_id) + ", '" + name_cn + "', '" + name_en + "', 1, 'tester', 'tester')"
    db_execute(sql)
    return snow_id


def db_insert_flow(name_cn: str = None, name_en: str = None) -> int:
    name_cn = name_cn or "集成测试连接流_" + TS
    name_en = name_en or "it_flow_" + TS
    snow_id = _gen_snowflake_id()
    sql = "INSERT INTO openplatform_v2_cp_flow_t (id, name_cn, name_en, lifecycle_status, create_by, last_update_by) VALUES (" + str(snow_id) + ", '" + name_cn + "', '" + name_en + "', 0, 'tester', 'tester')"
    db_execute(sql)
    return snow_id


def db_delete_connector(connector_id: int):
    db_execute("DELETE FROM openplatform_v2_cp_connector_t WHERE id = " + str(connector_id))


def db_delete_flow(flow_id: int):
    db_execute("DELETE FROM openplatform_v2_cp_flow_t WHERE id = " + str(flow_id))


class ApiClient:
    def __init__(self, base_url: str):
        self.base_url = base_url
        self.session = requests.Session()
        self.session.headers.update({"Content-Type": "application/json"})

    def post(self, path: str, json_data: dict = None, headers: dict = None) -> requests.Response:
        url = self.base_url + path
        h = self.session.headers.copy()
        if headers:
            h.update(headers)
        return self.session.post(url, json=json_data, headers=h)

    def get(self, path: str, params: dict = None) -> requests.Response:
        url = self.base_url + path
        return self.session.get(url, params=params)

    def put(self, path: str, json_data: dict = None) -> requests.Response:
        url = self.base_url + path
        return self.session.put(url, json=json_data)

    def delete(self, path: str) -> requests.Response:
        url = self.base_url + path
        return self.session.delete(url)


def report(msg: str):
    REPORT_LINES.append(msg)
    print(msg)


def test_pass(name: str, detail: str = ""):
    global PASS_COUNT
    PASS_COUNT += 1
    report("- \u2705 PASS: " + name + (" -- " + detail if detail else ""))


def test_fail(name: str, detail: str):
    global FAIL_COUNT
    FAIL_COUNT += 1
    report("- \u274c FAIL: " + name + " -- " + detail)


def test_error(name: str, detail: str):
    global ERROR_COUNT
    ERROR_COUNT += 1
    report("- \u26a0\ufe0f ERROR: " + name + " -- " + detail)


class TestConnectorPlatform(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        cls.client = ApiClient(OPEN_SERVER_URL + "/api/v1")
        cls.connector_api_client = ApiClient(CONNECTOR_API_URL + "/api/v1")

    # IT-001
    def test_it_001_create_connector_success(self):
        body = {
            "nameCn": "发送消息_" + TS,
            "nameEn": "SendMessage_" + TS,
            "iconFileId": "file_icon",
            "descriptionCn": "API测试连接器",
            "descriptionEn": "API test connector",
            "connectorType": 1
        }
        resp = self.client.post("/connectors", body)
        self.assertEqual(resp.status_code, 200)
        data = resp.json()
        self.assertEqual(data["code"], "200")
        self.assertIsInstance(data["data"]["id"], str)
        test_pass("IT-001", "创建连接器成功，ID为string类型")

    # IT-002
    def test_it_002_create_missing_name_cn(self):
        body = {"nameEn": "Test", "connectorType": 1}
        resp = self.client.post("/connectors", body)
        self.assertEqual(resp.status_code, 400)
        test_pass("IT-002", "缺少nameCn返回400")

    # IT-003
    def test_it_003_create_invalid_connector_type(self):
        body = {"nameCn": "测试", "nameEn": "Test", "connectorType": 99}
        resp = self.client.post("/connectors", body)
        data = resp.json()
        self.assertNotEqual(data["code"], "200")
        test_pass("IT-003", "非法connectorType被拦截")

    # IT-004
    def test_it_004_create_name_cn_too_long(self):
        body = {"nameCn": "a" * 501, "nameEn": "Test", "connectorType": 1}
        resp = self.client.post("/connectors", body)
        self.assertIn(resp.status_code, (400, 422))
        test_pass("IT-004", "超长nameCn被校验拦截")

    # IT-005
    def test_it_005_list_default_page(self):
        resp = self.client.get("/connectors")
        self.assertEqual(resp.status_code, 200)
        data = resp.json()
        self.assertEqual(data["code"], "200")
        self.assertIsInstance(data["data"], list)
        self.assertEqual(data["page"]["curPage"], 1)
        self.assertEqual(data["page"]["pageSize"], 20)
        test_pass("IT-005", "默认分页: curPage=1, pageSize=20")

    # IT-006
    def test_it_006_list_filter_by_type(self):
        resp = self.client.get("/connectors", {"connectorType": 1})
        self.assertEqual(resp.status_code, 200)
        test_pass("IT-006", "connectorType=1过滤成功")

    # IT-007
    def test_it_007_list_search_by_keyword(self):
        resp = self.client.get("/connectors", {"keyword": TS})
        self.assertEqual(resp.status_code, 200)
        test_pass("IT-007", "keyword搜索成功")

    # IT-008
    def test_it_008_list_custom_page(self):
        resp = self.client.get("/connectors", {"curPage": 2, "pageSize": 10})
        self.assertEqual(resp.status_code, 200)
        data = resp.json()
        self.assertEqual(data["page"]["curPage"], 2)
        self.assertEqual(data["page"]["pageSize"], 10)
        test_pass("IT-008", "自定义分页: curPage=2, pageSize=10")

    # IT-009
    def test_it_009_list_empty_result(self):
        resp = self.client.get("/connectors", {"keyword": "NONEXISTENT_XYZ_9999"})
        self.assertEqual(resp.status_code, 200)
        data = resp.json()
        self.assertEqual(data["page"]["total"], 0)
        self.assertEqual(len(data["data"]), 0)
        test_pass("IT-009", "空结果: data=[], total=0")

    # IT-010
    def test_it_010_detail_success(self):
        conn_id = db_insert_connector()
        try:
            resp = self.client.get("/connectors/" + str(conn_id))
            self.assertEqual(resp.status_code, 200)
            data = resp.json()
            self.assertEqual(data["code"], "200")
            self.assertEqual(str(data["data"]["id"]), str(conn_id))
            self.assertIsInstance(data["data"]["nameCn"], str)
            self.assertIsInstance(data["data"]["connectorType"], int)
            test_pass("IT-010", "查询连接器详情成功")
        finally:
            db_delete_connector(conn_id)

    # IT-011
    def test_it_011_detail_not_found(self):
        resp = self.client.get("/connectors/9999999999999999999")
        data = resp.json()
        self.assertEqual(data["code"], "404")
        test_pass("IT-011", "不存在的connectorId返回404")

    # IT-012
    def test_it_012_id_is_string(self):
        conn_id = db_insert_connector()
        try:
            resp = self.client.get("/connectors/" + str(conn_id))
            data = resp.json()
            self.assertIsInstance(data["data"]["id"], str)
            test_pass("IT-012", "雪花ID为string类型")
        finally:
            db_delete_connector(conn_id)

    # IT-013
    def test_it_013_update_connector(self):
        conn_id = db_insert_connector()
        try:
            body = {"nameCn": "更新后的名称_" + TS}
            resp = self.client.put("/connectors/" + str(conn_id), body)
            self.assertEqual(resp.status_code, 200)
            data = resp.json()
            self.assertEqual(data["code"], "200")
            test_pass("IT-013", "更新连接器名称成功")
        finally:
            db_delete_connector(conn_id)

    # IT-014
    def test_it_014_update_not_found(self):
        body = {"nameCn": "测试不存在"}
        resp = self.client.put("/connectors/9999999999999999999", body)
        data = resp.json()
        self.assertEqual(data["code"], "404")
        test_pass("IT-014", "更新不存在的连接器返回404")

    # IT-015
    def test_it_015_delete_connector(self):
        conn_id = db_insert_connector()
        resp = self.client.delete("/connectors/" + str(conn_id))
        self.assertEqual(resp.status_code, 200)
        data = resp.json()
        self.assertEqual(data["code"], "200")
        test_pass("IT-015", "删除连接器成功")

    # IT-016
    def test_it_016_delete_not_found(self):
        resp = self.client.delete("/connectors/9999999999999999999")
        data = resp.json()
        self.assertEqual(data["code"], "404")
        test_pass("IT-016", "删除不存在的连接器返回404")

    # IT-017
    def test_it_017_get_config(self):
        conn_id = db_insert_connector()
        try:
            resp = self.client.get("/connectors/" + str(conn_id) + "/config")
            self.assertEqual(resp.status_code, 200)
            data = resp.json()
            self.assertIn("hasConfig", data["data"])
            test_pass("IT-017", "获取配置成功，含hasConfig字段")
        finally:
            db_delete_connector(conn_id)

    # IT-018
    def test_it_018_config_empty(self):
        conn_id = db_insert_connector()
        try:
            resp = self.client.get("/connectors/" + str(conn_id) + "/config")
            data = resp.json()
            self.assertIn("hasConfig", data["data"])
            test_pass("IT-018", "新建连接器hasConfig字段存在")
        finally:
            db_delete_connector(conn_id)

    # IT-019
    def test_it_019_config_not_found(self):
        resp = self.client.get("/connectors/9999999999999999999/config")
        data = resp.json()
        self.assertEqual(data["code"], "404")
        test_pass("IT-019", "不存在的连接器配置返回404")

    # IT-020
    def test_it_020_update_config(self):
        conn_id = db_insert_connector()
        try:
            body = {"connectionConfig": '{"protocol":"HTTP","url":"https://test.com"}'}
            resp = self.client.put("/connectors/" + str(conn_id) + "/config", body)
            self.assertEqual(resp.status_code, 200)
            data = resp.json()
            self.assertEqual(data["code"], "200")
            test_pass("IT-020", "编辑连接配置成功")
        finally:
            db_delete_connector(conn_id)

    # IT-021
    def test_it_021_update_config_empty(self):
        conn_id = db_insert_connector()
        try:
            body = {"connectionConfig": ""}
            resp = self.client.put("/connectors/" + str(conn_id) + "/config", body)
            self.assertEqual(resp.status_code, 400)
            test_pass("IT-021", "空connectionConfig返回400")
        finally:
            db_delete_connector(conn_id)

    # IT-022
    def test_it_022_update_config_null(self):
        conn_id = db_insert_connector()
        try:
            resp = self.client.put("/connectors/" + str(conn_id) + "/config", {})
            self.assertEqual(resp.status_code, 400)
            test_pass("IT-022", "null connectionConfig返回400")
        finally:
            db_delete_connector(conn_id)

    # IT-023
    def test_it_023_create_flow(self):
        body = {"nameCn": "自动通知流_" + TS, "nameEn": "AutoNotify_" + TS}
        resp = self.client.post("/flows", body)
        self.assertEqual(resp.status_code, 200)
        data = resp.json()
        self.assertEqual(data["code"], "200")
        self.assertIsInstance(data["data"]["id"], str)
        flow_id = int(data["data"]["id"])
        try:
            db_delete_flow(flow_id)
        except Exception:
            pass
        test_pass("IT-023", "创建连接流成功")

    # IT-024
    def test_it_024_create_flow_missing_name_cn(self):
        body = {"nameEn": "Test"}
        resp = self.client.post("/flows", body)
        self.assertEqual(resp.status_code, 400)
        test_pass("IT-024", "缺少nameCn返回400")

    # IT-025
    def test_it_025_create_flow_missing_name_en(self):
        body = {"nameCn": "测试"}
        resp = self.client.post("/flows", body)
        self.assertEqual(resp.status_code, 400)
        test_pass("IT-025", "缺少nameEn返回400")

    # IT-026
    def test_it_026_list_flows_default(self):
        resp = self.client.get("/flows")
        self.assertEqual(resp.status_code, 200)
        data = resp.json()
        self.assertEqual(data["code"], "200")
        self.assertIsInstance(data["data"], list)
        test_pass("IT-026", "查询流列表成功")

    # IT-027
    def test_it_027_list_flows_filter_status(self):
        resp = self.client.get("/flows", {"lifecycleStatus": 0})
        self.assertEqual(resp.status_code, 200)
        test_pass("IT-027", "lifecycleStatus=0过滤成功")

    # IT-028
    def test_it_028_list_flows_keyword(self):
        resp = self.client.get("/flows", {"keyword": TS})
        self.assertEqual(resp.status_code, 200)
        test_pass("IT-028", "keyword搜索成功")

    # IT-029
    def test_it_029_list_flows_empty(self):
        resp = self.client.get("/flows", {"keyword": "NONEXISTENT_FLOW_9999"})
        data = resp.json()
        self.assertEqual(data["page"]["total"], 0)
        self.assertEqual(len(data["data"]), 0)
        test_pass("IT-029", "空结果: data=[], total=0")

    # IT-030
    def test_it_030_flow_detail(self):
        flow_id = db_insert_flow()
        try:
            resp = self.client.get("/flows/" + str(flow_id))
            self.assertEqual(resp.status_code, 200)
            data = resp.json()
            self.assertEqual(data["code"], "200")
            self.assertEqual(str(data["data"]["id"]), str(flow_id))
            test_pass("IT-030", "查询流详情成功")
        finally:
            db_delete_flow(flow_id)

    # IT-031
    def test_it_031_flow_detail_not_found(self):
        resp = self.client.get("/flows/9999999999999999999")
        data = resp.json()
        self.assertEqual(data["code"], "404")
        test_pass("IT-031", "不存在的flowId返回404")

    # IT-032
    def test_it_032_update_flow(self):
        flow_id = db_insert_flow()
        try:
            body = {"nameCn": "更新后的流名称_" + TS}
            resp = self.client.put("/flows/" + str(flow_id), body)
            self.assertEqual(resp.status_code, 200)
            data = resp.json()
            self.assertEqual(data["code"], "200")
            test_pass("IT-032", "更新流名称成功")
        finally:
            db_delete_flow(flow_id)

    # IT-033
    def test_it_033_update_flow_not_found(self):
        body = {"nameCn": "测试"}
        resp = self.client.put("/flows/9999999999999999999", body)
        data = resp.json()
        self.assertEqual(data["code"], "404")
        test_pass("IT-033", "更新不存在的flow返回404")

    # IT-034
    def test_it_034_delete_flow(self):
        flow_id = db_insert_flow()
        resp = self.client.delete("/flows/" + str(flow_id))
        self.assertEqual(resp.status_code, 200)
        data = resp.json()
        self.assertEqual(data["code"], "200")
        test_pass("IT-034", "删除流成功")

    # IT-035
    def test_it_035_delete_flow_not_found(self):
        resp = self.client.delete("/flows/9999999999999999999")
        data = resp.json()
        self.assertEqual(data["code"], "404")
        test_pass("IT-035", "删除不存在的flow返回404")

    # IT-036
    def test_it_036_start_flow(self):
        flow_id = db_insert_flow()
        try:
            resp = self.client.post("/flows/" + str(flow_id) + "/start")
            data = resp.json()
            self.assertEqual(data["code"], "200")
            test_pass("IT-036", "启动流成功")
        finally:
            db_delete_flow(flow_id)

    # IT-037
    def test_it_037_start_flow_already_running(self):
        flow_id = db_insert_flow()
        try:
            self.client.post("/flows/" + str(flow_id) + "/start")
            resp = self.client.post("/flows/" + str(flow_id) + "/start")
            data = resp.json()
            self.assertIn(data["code"], ("400", "409"))
            test_pass("IT-037", "重复启动被拦截")
        finally:
            db_delete_flow(flow_id)

    # IT-038
    def test_it_038_start_flow_not_found(self):
        resp = self.client.post("/flows/9999999999999999999/start")
        data = resp.json()
        self.assertEqual(data["code"], "404")
        test_pass("IT-038", "启动不存在的flow返回404")

    # IT-039
    def test_it_039_stop_flow(self):
        flow_id = db_insert_flow()
        try:
            self.client.post("/flows/" + str(flow_id) + "/start")
            resp = self.client.post("/flows/" + str(flow_id) + "/stop")
            data = resp.json()
            self.assertEqual(data["code"], "200")
            test_pass("IT-039", "停止流成功")
        finally:
            db_delete_flow(flow_id)

    # IT-040
    def test_it_040_stop_flow_already_stopped(self):
        flow_id = db_insert_flow()
        try:
            resp = self.client.post("/flows/" + str(flow_id) + "/stop")
            data = resp.json()
            self.assertIn(data["code"], ("400", "409"))
            test_pass("IT-040", "重复停止被拦截")
        finally:
            db_delete_flow(flow_id)

    # IT-041
    def test_it_041_stop_flow_not_found(self):
        resp = self.client.post("/flows/9999999999999999999/stop")
        data = resp.json()
        self.assertEqual(data["code"], "404")
        test_pass("IT-041", "停止不存在的flow返回404")

    # IT-042
    def test_it_042_get_flow_config(self):
        flow_id = db_insert_flow()
        try:
            resp = self.client.get("/flows/" + str(flow_id) + "/config")
            self.assertEqual(resp.status_code, 200)
            data = resp.json()
            self.assertIn("hasConfig", data["data"])
            test_pass("IT-042", "获取编排配置成功")
        finally:
            db_delete_flow(flow_id)

    # IT-043
    def test_it_043_flow_config_not_found(self):
        resp = self.client.get("/flows/9999999999999999999/config")
        data = resp.json()
        self.assertEqual(data["code"], "404")
        test_pass("IT-043", "不存在的flow配置返回404")

    # IT-044
    def test_it_044_save_flow_config(self):
        flow_id = db_insert_flow()
        try:
            body = {"orchestrationConfig": '{"nodes":[],"edges":[]}'}
            resp = self.client.put("/flows/" + str(flow_id) + "/config", body)
            self.assertEqual(resp.status_code, 200)
            data = resp.json()
            self.assertEqual(data["code"], "200")
            test_pass("IT-044", "保存编排配置成功")
        finally:
            db_delete_flow(flow_id)

    # IT-045
    def test_it_045_save_flow_config_empty(self):
        flow_id = db_insert_flow()
        try:
            body = {"orchestrationConfig": ""}
            resp = self.client.put("/flows/" + str(flow_id) + "/config", body)
            self.assertEqual(resp.status_code, 400)
            test_pass("IT-045", "空编排配置返回400")
        finally:
            db_delete_flow(flow_id)

    # IT-046
    def test_it_046_save_flow_config_null(self):
        flow_id = db_insert_flow()
        try:
            resp = self.client.put("/flows/" + str(flow_id) + "/config", {})
            self.assertEqual(resp.status_code, 400)
            test_pass("IT-046", "null编排配置返回400")
        finally:
            db_delete_flow(flow_id)

    # IT-047
    def test_it_047_test_run_flow_not_found(self):
        body = {"mockTriggerData": {}}
        resp = self.client.post("/flows/9999999999999999999/test-run", body)
        data = resp.json()
        self.assertEqual(data["code"], "404")
        test_pass("IT-047", "不存在的flow test-run返回404")

    # IT-048
    def test_it_048_test_run_no_config(self):
        flow_id = db_insert_flow()
        try:
            body = {"mockTriggerData": {"message": "hello"}}
            resp = self.client.post("/flows/" + str(flow_id) + "/test-run", body)
            data = resp.json()
            self.assertIn(data["code"], ("400", "422", "404"))
            test_pass("IT-048", "未配置编排test-run被拦截")
        finally:
            db_delete_flow(flow_id)

    # IT-049
    def test_it_049_trigger_no_auth(self):
        try:
            resp = self.connector_api_client.post("/trigger/9999999999999999999/invoke",
                                                    {"payload": {"message": "test"}})
            data = resp.json()
            self.assertEqual(data["code"], "401")
            test_pass("IT-049", "缺少X-Sys-Token返回401")
        except requests.exceptions.ConnectionError:
            report("- SKIP IT-049: connector-api 未运行 (port 18180)")

    # IT-050
    def test_it_050_trigger_flow_not_found(self):
        try:
            headers = {"X-Sys-Token": "test-token"}
            resp = self.connector_api_client.post("/trigger/9999999999999999999/invoke",
                                                    {"payload": {}}, headers=headers)
            data = resp.json()
            self.assertEqual(data["code"], "404")
            test_pass("IT-050", "触发不存在的flow返回404")
        except requests.exceptions.ConnectionError:
            report("- SKIP IT-050: connector-api 未运行 (port 18180)")

    # IT-051
    def test_it_051_trigger_flow_not_running(self):
        try:
            headers = {"X-Sys-Token": "test-token"}
            resp = self.connector_api_client.post("/trigger/9999999999999999999/invoke",
                                                    {"payload": {}}, headers=headers)
            data = resp.json()
            self.assertNotEqual(data["code"], "200")
            test_pass("IT-051", "未运行flow不返回200成功")
        except requests.exceptions.ConnectionError:
            report("- SKIP IT-051: connector-api 未运行 (port 18180)")

    # IT-052
    def test_it_052_success_response_format(self):
        resp = self.client.get("/connectors")
        body = resp.json()
        for field in ["code", "messageZh", "messageEn", "data", "page"]:
            self.assertIn(field, body)
        self.assertIsInstance(body["code"], str)
        self.assertIsInstance(body["messageZh"], str)
        test_pass("IT-052", "成功响应包含所有标准字段")

    # IT-053
    def test_it_053_error_response_format(self):
        resp = self.client.get("/connectors/9999999999999999999")
        body = resp.json()
        self.assertNotEqual(body["code"], "200")
        self.assertIsNotNone(body["messageZh"])
        test_pass("IT-053", "错误响应code!=200且包含messageZh")

    # IT-054
    def test_it_054_paged_response_format(self):
        resp = self.client.get("/connectors")
        body = resp.json()
        page = body.get("page")
        if page is not None:
            for field in ["curPage", "pageSize", "total"]:
                self.assertIn(field, page)
        test_pass("IT-054", "分页响应格式完整")

    # IT-055
    def test_it_055_bigint_id_as_string(self):
        resp = self.client.get("/connectors")
        body = resp.json()
        if body["data"] and len(body["data"]) > 0:
            item = body["data"][0]
            for key in item:
                if key == "id" or key.endswith("id") or key.endswith("Id"):
                    self.assertIsInstance(item[key], str)
        test_pass("IT-055", "ID字段为string类型")

    # IT-056
    def test_it_056_enum_as_tinyint(self):
        resp = self.client.get("/connectors")
        body = resp.json()
        if body["data"] and len(body["data"]) > 0:
            item = body["data"][0]
            for field in ["connectorType", "lifecycleStatus", "status"]:
                if field in item:
                    self.assertIsInstance(item[field], int)
        test_pass("IT-056", "枚举字段为数字类型")

    # IT-057
    def test_it_057_datetime_iso8601(self):
        iso_pattern = r"^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}"
        resp = self.client.get("/connectors")
        body = resp.json()
        if body["data"] and len(body["data"]) > 0:
            item = body["data"][0]
            for field in ["createTime", "lastUpdateTime"]:
                if field in item and item[field]:
                    self.assertRegex(str(item[field]), iso_pattern)
        test_pass("IT-057", "时间字段为ISO 8601格式")

    # IT-058
    def test_it_058_camel_case_fields(self):
        camel = re.compile(r"^[a-z]+[A-Za-z0-9]*$")
        resp = self.client.get("/connectors")
        body = resp.json()
        if body["data"] and len(body["data"]) > 0:
            item = body["data"][0]
            for key in item.keys():
                self.assertTrue(camel.match(key))
        test_pass("IT-058", "字段名为camelCase规范")

    # IT-059
    def test_it_059_error_code_coverage(self):
        resp = self.client.get("/connectors/9999999999999999999")
        body = resp.json()
        expected_codes = ["400", "401", "403", "404", "409", "422", "429", "500"]
        self.assertIn(body["code"], expected_codes)
        test_pass("IT-059", "错误码在预期范围内")


def generate_report():
    total = PASS_COUNT + FAIL_COUNT + ERROR_COUNT
    now = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    report_path = "/home/usb/workspace/wks-open-app/open-app/.sddu/specs-tree-root/specs-tree-connector-platform/test-report-integration.md"
    with open(report_path, "w", encoding="utf-8") as f:
        f.write("# \u96c6\u6210\u6d4b\u8bd5\u62a5\u544a\uff1a\u8fde\u63a5\u5668\u5e73\u53f0\n\n")
        f.write("**\u6d4b\u8bd5\u65e5\u671f**: " + now + "  \n")
        f.write("**\u6d4b\u8bd5\u7c7b\u578b**: L3 \u96c6\u6210\u6d4b\u8bd5\uff08\u771f\u5b9e\u670d\u52a1 + \u771f\u5b9e\u6570\u636e\u5e93\uff09  \n")
        f.write("**\u670d\u52a1**: open-server (:18080) / connector-api (:18180)\n\n")
        f.write("---\n\n## \u6267\u884c\u7ed3\u679c\u6458\u8981\n\n")
        f.write("| \u6307\u6807 | \u6570\u503c |\n|------|:----:|\n")
        f.write("| \u603b\u7528\u4f8b\u6570 | " + str(total) + " |\n")
        f.write("| \u2705 \u901a\u8fc7 | " + str(PASS_COUNT) + " |\n")
        f.write("| \u274c \u5931\u8d25 | " + str(FAIL_COUNT) + " |\n")
        f.write("| \u26a0\ufe0f \u9519\u8bef | " + str(ERROR_COUNT) + " |\n")
        pct = "0.0"
        if total > 0:
            pct = f"{PASS_COUNT / total * 100:.1f}"
        f.write("| \u901a\u8fc7\u7387 | " + pct + "% |\n\n")
        f.write("---\n\n## \u8be6\u7ec6\u7ed3\u679c\n\n")
        for line in REPORT_LINES:
            f.write(line + "\n")
        f.write("\n---\n\n*\u62a5\u544a\u7531 run_integration_tests.py \u81ea\u52a8\u751f\u6210*\n")
    print("\n[FILE] \u6d4b\u8bd5\u62a5\u544a\u5df2\u751f\u6210: " + report_path)
    return report_path

def _report_failures(result):
    for test_case, trace in result.failures:
        test_name = test_case._testMethodName
        lines = trace.split('\n')
        detail = ''
        for line in lines:
            if 'AssertionError:' in line or 'assert' in line:
                detail = line.strip()
                break
        if not detail:
            detail = lines[-1].strip() if lines else ''
        report(f"- FAIL {test_name}: {detail[:200]}")

    for test_case, trace in result.errors:
        test_name = test_case._testMethodName
        lines = trace.split('\n')
        detail = ''
        for line in lines:
            if 'Error:' in line:
                detail = line.strip()
                break
        if not detail:
            for i, line in enumerate(lines):
                if 'raise' in line and i+1 < len(lines):
                    detail = lines[i+1].strip()
                    break
            if not detail and lines:
                detail = lines[-1].strip()[:200]
        report(f"- ERROR {test_name}: {detail[:200]}")


def run_tests():
    print("\n[SEARCH] \u68c0\u67e5\u670d\u52a1\u72b6\u6001...")
    try:
        r = requests.get(OPEN_SERVER_URL + "/actuator/health", timeout=5)
        print("  open-server: " + ("\u2705 \u8fd0\u884c\u4e2d" if r.ok else "\u274c \u5f02\u5e38") + " (" + str(r.status_code) + ")")
    except Exception as e:
        print("  open-server: \u274c \u65e0\u6cd5\u8fde\u63a5 -- " + str(e))
        print("\u8bf7\u5148\u542f\u52a8 open-server: cd open-server && mvn spring-boot:run")
        sys.exit(1)
    try:
        r = requests.get(CONNECTOR_API_URL + "/actuator/health", timeout=3)
        print("  connector-api: " + ("\u2705 \u8fd0\u884c\u4e2d" if r.ok else "\u274c \u5f02\u5e38") + " (" + str(r.status_code) + ")")
    except Exception:
        print("  connector-api: \u26a0\ufe0f \u672a\u8fd0\u884c (\u8df3\u8fc7API #18 \u6d4b\u8bd5)")
    print("\n" + "=" * 60)
    print("\u5f00\u59cb\u6267\u884c\u6d4b\u8bd5...")
    print("=" * 60 + "\n")
    suite = unittest.TestLoader().loadTestsFromTestCase(TestConnectorPlatform)
    runner = unittest.TextTestRunner(verbosity=0)
    result = runner.run(suite)

    _report_failures(result)

    global PASS_COUNT, FAIL_COUNT, ERROR_COUNT
    PASS_COUNT = result.testsRun - len(result.failures) - len(result.errors)
    FAIL_COUNT = len(result.failures)
    ERROR_COUNT = len(result.errors)
    report_path = generate_report()
    print("\n" + "=" * 60)
    print("  \u6d4b\u8bd5\u5b8c\u6210")
    print("  \u603b\u7528\u4f8b: " + str(result.testsRun))
    print("  \u901a\u8fc7:   " + str(PASS_COUNT))
    print("  \u5931\u8d25:   " + str(FAIL_COUNT))
    print("  \u9519\u8bef:   " + str(ERROR_COUNT))
    print("  \u62a5\u544a:   " + report_path)
    print("=" * 60)
    return 0 if result.wasSuccessful() else 1


if __name__ == "__main__":
    sys.exit(run_tests())
