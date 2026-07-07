#!/usr/bin/env python3
"""DELETE /apps/{appId}/{apis,events,callbacks}/{id} — 删除订阅"""
import pytest
from conftest import api, INTERNAL_APP_ID


class TestDeleteSubscription:
    @pytest.mark.L4
    def test_delete_api_sub_not_found(self):
        resp = api("DELETE", f"/apps/{INTERNAL_APP_ID}/apis/999999999999999999")
        assert resp is not None

    @pytest.mark.L4
    def test_delete_event_sub_not_found(self):
        resp = api("DELETE", f"/apps/{INTERNAL_APP_ID}/events/999999999999999999")
        assert resp is not None

    @pytest.mark.L4
    def test_delete_callback_sub_not_found(self):
        resp = api("DELETE", f"/apps/{INTERNAL_APP_ID}/callbacks/999999999999999999")
        assert resp is not None
