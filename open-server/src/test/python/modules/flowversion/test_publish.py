#!/usr/bin/env python3
"""#32 POST /flows/{id}/versions/{vid}/publish — 发布连接流版本"""
import json
import pytest
from common import api, db
from conftest import assert_operate_log


class TestFlowVersionPublish:
    CONFIG = {"nodes": [{"id": "trigger", "type": "trigger", "data": {"type": "trigger", "triggerType": "http"}}, {"id": "n1", "type": "connector", "data": {"type": "connector"}}, {"id": "exit", "type": "exit", "data": {"type": "exit"}}], "edges": [{"id": "e1", "source": "trigger", "target": "n1"}, {"id": "e2", "source": "n1", "target": "exit"}], "flowConfig": {"flowMode": "single"}}

    @pytest.mark.L2
    def test_publish(self, draft_flow):
        """FR-026: 草稿→待审批，验证状态变更"""
        fid, fvid = draft_flow
        # 前置：写入最小编排配置（发布时校验非空）
        db(f"UPDATE openplatform_v2_cp_flow_version_t SET orchestration_config = '{json.dumps(self.CONFIG)}' WHERE id = {fvid}")
        # 前置确认：草稿状态
        resp0 = api("GET", f"/flows/{fid}/versions/{fvid}")
        assert resp0.status_code == 200
        before = resp0.json()["data"]
        assert before.get("status") in (1, "1"), f"Expected status=1 (草稿), got {before.get('status')}"
        # 执行发布（提交审批）
        resp = api("POST", f"/flows/{fid}/versions/{fvid}/publish")
        assert resp.status_code in (200, 201), f"Expected 200/201, got {resp.status_code}"
        assert resp.json()["code"] == "200"
        # 验证状态变更（草稿→待审批=2）
        resp2 = api("GET", f"/flows/{fid}/versions/{fvid}")
        assert resp2.status_code == 200
        after = resp2.json()["data"]
        assert after.get("status") in (2, "2"), f"Expected status=2 (待审批), got {after.get('status')}"

    @pytest.mark.L2
    def test_publish_log(self, draft_flow):
        """发布版本 → 操作日志"""
        fid, fvid = draft_flow
        api("PUT", f"/flows/{fid}/versions/{fvid}", {
            "orchestrationConfig": json.dumps({
                "flowConfig": {"flowMode": "serial", "timeout": 3000},
                "nodes": [
                    {"id": "trigger", "type": "trigger", "data": {"type": "trigger", "triggerType": "http"}},
                    {"id": "exit", "type": "exit", "data": {"type": "exit"}},
                ],
                "edges": [{"id": "e1", "source": "trigger", "target": "exit"}],
            })
        })
        resp = api("POST", f"/flows/{fid}/versions/{fvid}/publish")
        assert resp.status_code in (200, 201)
        assert_operate_log("提交连接流版本发布审批")
