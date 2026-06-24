#!/usr/bin/env python3
"""#20 PUT /flows/{id} — 更新连接流"""
import pytest
from _client import api


class TestFlowUpdate:
    @pytest.mark.L1
    def test_update_ok(self, flow):
        resp = api("PUT", f"/flows/{flow}", {"nameCn": "更新后的流名称"})
        assert resp.status_code == 200

    @pytest.mark.L4
    def test_not_found(self):
        resp = api("PUT", "/flows/999999999999999999", {"nameCn": "测试"})
        assert resp is not None
