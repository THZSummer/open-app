#!/usr/bin/env python3
"""#36 DELETE /flows/{id}/versions/{vid} — 删除连接流版本"""
import pytest
from common import api, db
from conftest import assert_operate_log
import json


class TestFlowVersionDelete:
    @pytest.mark.L2
    def test_delete_invalidated(self, draft_flow):
        fid, fvid = draft_flow
        db(f"UPDATE openplatform_v2_cp_flow_version_t SET status = 6 WHERE id = {fvid}")
        resp = api("DELETE", f"/flows/{fid}/versions/{fvid}")
        assert resp is not None
        assert resp.status_code in (200, 204)

    @pytest.mark.L2
    def test_delete_log(self, draft_flow):
        """删除版本 → 操作日志"""
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
        api("POST", f"/flows/{fid}/versions/{fvid}/publish")
        api("PUT", f"/flows/{fid}/versions/{fvid}/invalidate")
        resp = api("DELETE", f"/flows/{fid}/versions/{fvid}")
        assert resp.status_code == 200
        assert_operate_log("删除")
