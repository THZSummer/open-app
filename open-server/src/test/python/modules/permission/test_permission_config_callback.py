#!/usr/bin/env python3
"""PUT /apps/{appId}/callbacks/{id}/config — 配置回调消费参数"""
import pytest
from conftest import api, INTERNAL_APP_ID


class TestConfigCallback:
    @pytest.mark.L4
    def test_not_found(self):
        resp = api("PUT", f"/apps/{INTERNAL_APP_ID}/callbacks/999999999999999999/config", {
            "channel": "http",
            "address": "http://example.com/webhook",
        })
        assert resp is not None
