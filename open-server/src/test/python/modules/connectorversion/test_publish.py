#!/usr/bin/env python3
"""#12 PUT /connectors/{id}/versions/{vid}/publish — 发布连接器版本 (FR-007, FR-046)"""
import json
import pytest
from common import set_lookup_config
from conftest import api, assert_operate_log


class TestConnectorVersionPublish:

    @pytest.mark.L2
    def test_publish(self, draft_connector):
        """FR-007: 草稿→已发布 + FR-046: 操作日志"""
        cid, vid = draft_connector
        api("PUT", f"/connectors/{cid}/versions/{vid}", {
            "connectionConfig": {
                "protocol": "HTTP",
                "protocolConfig": {"url": "https://httpbin.org/get", "method": "GET"},
                "timeoutMs": 5000,
            }
        })
        resp = api("PUT", f"/connectors/{cid}/versions/{vid}/publish")
        assert resp.status_code == 200
        assert resp.json()["code"] == "200"
        assert_operate_log("pytest_publish")

    @pytest.mark.L4
    def test_url_regex_refused(self, draft_connector):
        """#2 Connector.Url.Regex.Pattern: 不匹配的 URL 发布被拒"""
        cid, vid = draft_connector
        set_lookup_config("Connector.Url.Regex.Pattern", "^https://allowed\\.example\\.com/.*")
        try:
            api("PUT", f"/connectors/{cid}/versions/{vid}", {
                "connectionConfig": {
                    "protocol": "HTTP",
                    "protocolConfig": {
                        "url": "https://blocked.example.com/api",
                        "method": "GET",
                    },
                    "timeoutMs": 5000,
                }
            })
            resp = api("PUT", f"/connectors/{cid}/versions/{vid}/publish")
            assert resp.json()["code"] != "200", f"URL regex should block publish, got {resp.json()}"
        finally:
            set_lookup_config("Connector.Url.Regex.Pattern", "^https?://.*")

    @pytest.mark.L4
    def test_url_regex_pass(self, draft_connector):
        """#2 Connector.Url.Regex.Pattern: 匹配的 URL 发布通过"""
        cid, vid = draft_connector
        set_lookup_config("Connector.Url.Regex.Pattern", "^https://httpbin\\.org/.*")
        try:
            api("PUT", f"/connectors/{cid}/versions/{vid}", {
                "connectionConfig": {
                    "protocol": "HTTP",
                    "protocolConfig": {"url": "https://httpbin.org/get", "method": "GET"},
                    "timeoutMs": 5000,
                }
            })
            resp = api("PUT", f"/connectors/{cid}/versions/{vid}/publish")
            assert resp.status_code in (200, 201)
        finally:
            set_lookup_config("Connector.Url.Regex.Pattern", "^https?://.*")

    @pytest.mark.L4
    def test_config_size_refused(self, draft_connector):
        """#3 Connector.Config.Max.Bytes: 超限 JSON 发布被拒"""
        cid, vid = draft_connector
        set_lookup_config("Connector.Config.Max.Bytes", "50")
        try:
            long_config = {
                "protocol": "HTTP",
                "protocolConfig": {
                    "url": "https://httpbin.org/post",
                    "method": "POST",
                    "headers": {"X-Long": "x" * 200},
                    "body": {"payload": "y" * 500},
                },
                "timeoutMs": 5000,
            }
            api("PUT", f"/connectors/{cid}/versions/{vid}", {"connectionConfig": long_config})
            resp = api("PUT", f"/connectors/{cid}/versions/{vid}/publish")
            assert resp.json()["code"] != "200", f"Config size limit should block publish, got {resp.json()}"
        finally:
            set_lookup_config("Connector.Config.Max.Bytes", "1048576")

