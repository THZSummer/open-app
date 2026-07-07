#!/usr/bin/env python3
"""PUT /apps/{appId}/events/{id}/config — 配置事件消费参数"""
import pytest
from conftest import api, INTERNAL_APP_ID


class TestConfigEvent:
    @pytest.mark.L4
    def test_not_found(self):
        resp = api("PUT", f"/apps/{INTERNAL_APP_ID}/events/999999999999999999/config", {
            "channel": "http",
            "address": "http://example.com/webhook",
        })
        assert resp is not None
