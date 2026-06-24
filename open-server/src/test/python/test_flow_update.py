#!/usr/bin/env python3
"""#20 PUT /flows/{id} — 更新连接流"""
import pytest
from _client import api


class TestFlowUpdate:
    @pytest.mark.L1
    def test_update_ok(self, flow):
        """验证更新后字段值生效（先更新再 GET 校验）"""
        new_name = "更新后的连接流名称"
        resp = api("PUT", f"/flows/{flow}", {
            "nameCn": new_name, "nameEn": "UpdatedFlow",
            "descriptionCn": "更新描述", "descriptionEn": "Updated desc"
        })
        assert resp.status_code == 200
        assert resp.json()["code"] == "200"
        # 重新查询验证更新生效
        resp2 = api("GET", f"/flows/{flow}")
        assert resp2.status_code == 200
        d = resp2.json()["data"]
        assert d["nameCn"] == new_name, f"Expected nameCn='{new_name}', got '{d.get('nameCn')}'"
        assert d["nameEn"] == "UpdatedFlow"

    @pytest.mark.L4
    def test_update_nonexistent(self):
        resp = api("PUT", "/flows/999999999999999999", {"nameCn": "x"})
        assert resp is not None
