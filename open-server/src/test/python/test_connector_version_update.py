#!/usr/bin/env python3
"""#11 PUT /connectors/{id}/versions/{vid} — 更新连接器版本配置"""
import json
import pytest
from _client import api


class TestConnectorVersionUpdate:
    CONFIG = {"protocol": "HTTP", "protocolConfig": {"url": "https://httpbin.org/post", "method": "POST"}, "timeoutMs": 8000}

    @pytest.mark.L1
    def test_update_draft(self, draft_connector):
        """验证更新后配置生效（先更新再 GET 校验 connectionConfig）"""
        cid, vid = draft_connector
        resp = api("PUT", f"/connectors/{cid}/versions/{vid}", {"connectionConfig": self.CONFIG})
        assert resp.status_code == 200
        assert resp.json()["code"] == "200"
        # 重新查询验证配置生效
        resp2 = api("GET", f"/connectors/{cid}/versions/{vid}")
        assert resp2.status_code == 200
        d = resp2.json()["data"]
        cfg = d.get("connectionConfig")
        # connectionConfig 可能是 null、str 或 dict，三种都应处理
        if cfg is None:
            pytest.fail(f"connectionConfig is null after update")
        if isinstance(cfg, str):
            cfg = json.loads(cfg)
        assert cfg.get("protocolConfig", {}).get("url") == self.CONFIG["protocolConfig"]["url"]
        assert cfg.get("timeoutMs") == self.CONFIG["timeoutMs"]
