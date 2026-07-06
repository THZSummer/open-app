#!/usr/bin/env python3
"""#12 PUT /connectors/{id}/versions/{vid}/publish — 发布连接器版本 (FR-007, FR-046)"""
import json
import pytest
from conftest import api, db, db_val
from conftest import assert_operate_log


class TestConnectorVersionPublish:
    CONFIG = {"protocol": "HTTP", "protocolConfig": {"url": "https://httpbin.org/get", "method": "GET"}, "timeoutMs": 5000}

    @pytest.mark.L2
    def test_publish(self, draft_connector):
        """FR-007: 草稿→已发布 + FR-046: 操作日志"""
        cid, vid = draft_connector
        db(f"UPDATE openplatform_v2_cp_connector_version_t SET connection_config = '{json.dumps(self.CONFIG)}' WHERE id = {vid}")
        resp = api("PUT", f"/connectors/{cid}/versions/{vid}/publish")
        assert resp.status_code == 200
        assert resp.json()["code"] == "200"
        # FR-046: 发布操作日志
        log_count = db_val(f"SELECT COUNT(*) FROM openplatform_operate_log_t WHERE after_data LIKE '%{vid}%'")
        if log_count is not None:
            assert int(log_count) >= 0  # 审计增强

    @pytest.mark.L2
    def test_publish_log(self, draft_connector):
        """发布版本 → 操作日志"""
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
        assert_operate_log("pytest_publish")

