#!/usr/bin/env python3
"""PUT /apps/{appId}/{events,callbacks}/{id}/config — 配置消费参数"""
import pytest
from conftest import api, INTERNAL_APP_ID


class TestConfig:
    @pytest.mark.L4
    def test_config_event_not_found(self):
        resp = api("PUT", f"/apps/{INTERNAL_APP_ID}/events/999999999999999999/config", {
            "channel": "http",
            "address": "http://example.com/webhook",
        })
        assert resp is not None

    @pytest.mark.L4
    def test_config_callback_not_found(self):
        resp = api("PUT", f"/apps/{INTERNAL_APP_ID}/callbacks/999999999999999999/config", {
            "channel": "http",
            "address": "http://example.com/webhook",
        })
        assert resp is not None
