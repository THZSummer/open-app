#!/usr/bin/env python3
"""#12 PUT /connectors/{id}/versions/{vid}/publish — 发布连接器版本 (FR-007, FR-046)"""
import json
import pytest
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

