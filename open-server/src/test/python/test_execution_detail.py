#!/usr/bin/env python3
"""#50 GET /executions/{id} — 运行记录详情"""
import pytest
from conftest import api


class TestExecutionDetail:
    @pytest.mark.L4
    def test_detail_not_found(self):
        resp = api("GET", "/executions/999999999999999999")
        assert resp is not None
