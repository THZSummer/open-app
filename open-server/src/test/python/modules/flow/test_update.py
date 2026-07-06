#!/usr/bin/env python3
"""#20 PUT /flows/{id} — 更新连接流 (FR-046 操作日志)"""
import pytest
from conftest import api, flow, assert_operate_log


class TestFlowUpdate:
    @pytest.mark.L1
    def test_update_ok(self, flow):
        """验证更新后字段值生效"""
        new_name = "更新后的连接流名称"
        resp = api("PUT", f"/flows/{flow}", {
            "nameCn": new_name, "nameEn": "UpdatedFlow",
            "descriptionCn": "更新描述", "descriptionEn": "Updated desc"
        })
        assert resp.status_code == 200
        assert resp.json()["code"] == "200"
        resp2 = api("GET", f"/flows/{flow}")
        assert resp2.status_code == 200
        d = resp2.json()["data"]
        actual_name = d.get("nameCn")
        assert actual_name == new_name, f"Expected nameCn='{new_name}', got '{actual_name}'"

    @pytest.mark.L4
    def test_update_nonexistent(self):
        resp = api("PUT", "/flows/999999999999999999", {"nameCn": "x"})
        assert resp is not None

    @pytest.mark.L1
    def test_update_log(self, flow):
        """更新连接流 → 操作日志"""
        import time
        new_name = "更新的日志测试流-" + str(int(time.time()))
        resp = api("PUT", f"/flows/{flow}", {"nameCn": new_name})
        assert resp.status_code in (200, 201)
        assert_operate_log("更新连接流")

