#!/usr/bin/env python3
"""#21 POST /flows/{id}/copy — 复制连接流"""
import json
import pytest
from _client import api, db


class TestFlowCopy:
    @pytest.mark.L2
    def test_copy_ok(self, deployed_flow):
        fid, _ = deployed_flow
        resp = api("POST", f"/flows/{fid}/copy", {})
        assert resp is not None
        assert resp.status_code in (200, 201)

    @pytest.mark.L2
    def test_copy_single_version(self, flow):
        vid = int(str(flow) + "1") if flow else 0
        if vid:
            orch = {
                "nodes": [
                    {"id": "t1", "type": "trigger", "position": {"x": 0, "y": 0}, "data": {"type": "http"}},
                    {"id": "exit1", "type": "exit", "position": {"x": 300, "y": 0}, "data": {"outputMapping": {}}}
                ],
                "edges": [
                    {"id": "e1", "source": "t1", "target": "exit1"}
                ]
            }
            orch_s = json.dumps(orch, ensure_ascii=False).replace("'", "''")
            db(f"INSERT INTO openplatform_v2_cp_flow_version_t (id, flow_id, orchestration_config, status, create_by, last_update_by) VALUES ({vid}, {flow}, '{orch_s}', 5, 'tester', 'tester')")
            resp = api("POST", f"/flows/{flow}/copy", {})
            assert resp is not None
            assert resp.status_code in (200, 201)
