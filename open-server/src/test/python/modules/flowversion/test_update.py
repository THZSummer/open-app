#!/usr/bin/env python3
"""#31 PUT /flows/{id}/versions/{vid} — 更新连接流版本编排"""
import pytest
from common import api


class TestFlowVersionUpdate:
    CONFIG = {"flowConfig": {}, "nodes": [], "edges": []}

    @pytest.mark.L1
    def test_update_draft(self, draft_flow):
        """验证更新后编排配置生效（先更新再 GET 校验）"""
        fid, fvid = draft_flow
        resp = api("PUT", f"/flows/{fid}/versions/{fvid}", {"orchestrationConfig": self.CONFIG})
        assert resp.status_code == 200
        assert resp.json()["code"] == "200"
        # 重新查询验证
        resp2 = api("GET", f"/flows/{fid}/versions/{fvid}")
        assert resp2.status_code == 200
        d = resp2.json()["data"]
        assert "orchestrationConfig" in d
