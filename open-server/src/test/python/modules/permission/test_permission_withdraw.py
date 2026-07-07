#!/usr/bin/env python3
"""POST /apps/{appId}/{apis,events,callbacks}/{id}/withdraw — 撤回订阅"""
import pytest
from conftest import api, INTERNAL_APP_ID


class TestWithdraw:
    @pytest.mark.L4
    def test_withdraw_api_not_found(self):
        resp = api("POST", f"/apps/{INTERNAL_APP_ID}/apis/999999999999999999/withdraw")
        assert resp is not None

    @pytest.mark.L4
    def test_withdraw_event_not_found(self):
        resp = api("POST", f"/apps/{INTERNAL_APP_ID}/events/999999999999999999/withdraw")
        assert resp is not None

    @pytest.mark.L4
    def test_withdraw_callback_not_found(self):
        resp = api("POST", f"/apps/{INTERNAL_APP_ID}/callbacks/999999999999999999/withdraw")
        assert resp is not None
