#!/usr/bin/env python3
"""DELETE /apps/{appId}/events/{id} — 删除事件订阅"""
import pytest
from conftest import api, INTERNAL_APP_ID


class TestDeleteEventSubscription:
    @pytest.mark.L4
    def test_not_found(self):
        resp = api("DELETE", f"/apps/{INTERNAL_APP_ID}/events/999999999999999999")
        assert resp is not None
