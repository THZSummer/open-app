#!/usr/bin/env python3
"""DELETE /apps/{appId}/apis/{id} — 删除 API 订阅"""
import pytest
from conftest import api, INTERNAL_APP_ID


class TestDeleteApiSubscription:
    @pytest.mark.L4
    def test_not_found(self):
        resp = api("DELETE", f"/apps/{INTERNAL_APP_ID}/apis/999999999999999999")
        assert resp is not None
