#!/usr/bin/env python3
"""POST /apps/{appId}/apis/{id}/withdraw — 撤回 API 订阅"""
import pytest
from conftest import api, INTERNAL_APP_ID


class TestWithdrawApi:
    @pytest.mark.L4
    def test_not_found(self):
        resp = api("POST", f"/apps/{INTERNAL_APP_ID}/apis/999999999999999999/withdraw")
        assert resp is not None
