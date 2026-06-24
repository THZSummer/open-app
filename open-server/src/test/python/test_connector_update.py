#!/usr/bin/env python3
"""#4 PUT /connectors/{id} — 更新连接器"""
import pytest
from _client import api


class TestConnectorUpdate:
    @pytest.mark.L1
    def test_update_ok(self, connector):
        """验证更新后字段值生效（先更新再 GET 校验）"""
        new_name = "更新后的连接器名称"
        resp = api("PUT", f"/connectors/{connector}", {
            "nameCn": new_name, "nameEn": "UpdatedConnector",
            "descriptionCn": "更新描述", "descriptionEn": "Updated desc"
        })
        assert resp.status_code == 200
        assert resp.json()["code"] == "200"
        # 重新查询验证更新生效
        resp2 = api("GET", f"/connectors/{connector}")
        assert resp2.status_code == 200
        d = resp2.json()["data"]
        assert d["nameCn"] == new_name, f"Expected nameCn='{new_name}', got '{d.get('nameCn')}'"
        assert d["nameEn"] == "UpdatedConnector"
