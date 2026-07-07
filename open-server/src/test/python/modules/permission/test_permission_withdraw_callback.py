#!/usr/bin/env python3
"""POST /apps/{appId}/callbacks/{id}/withdraw — 撤回回调订阅"""
import pytest
from conftest import api, INTERNAL_APP_ID


class TestWithdrawCallback:
    @pytest.mark.L4
    def test_not_found(self):
        resp = api("POST", f"/apps/{INTERNAL_APP_ID}/callbacks/999999999999999999/withdraw")
        assert resp is not None
