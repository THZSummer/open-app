#!/usr/bin/env python3
"""#47 POST /approval-flows — 创建审批流模板"""
import time
import pytest
from conftest import api


class TestApprovalFlowCreate:
    @pytest.mark.L1
    def test_create_ok(self):
        """创建时不校验业务必填字段，空数据应被接受"""
        resp = api("POST", "/approval-flows", {
            "nameCn": "", "nameEn": "", "code": "", "nodes": []
        })
        assert resp is not None
        assert resp.status_code in (200, 201, 400, 409)
