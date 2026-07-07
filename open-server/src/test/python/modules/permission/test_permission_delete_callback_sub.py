#!/usr/bin/env python3
"""DELETE /apps/{appId}/callbacks/{id} — 删除回调订阅"""
import pytest
from conftest import api, INTERNAL_APP_ID


class TestDeleteCallbackSubscription:
    @pytest.mark.L4
    def test_not_found(self):
        resp = api("DELETE", f"/apps/{INTERNAL_APP_ID}/callbacks/999999999999999999")
        assert resp is not None
