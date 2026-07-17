#!/usr/bin/env python3
"""
重构冒烟测试 — 审批模块集成验证

验证模块重组后，已有审批接口调用 ApprovalService → AbilityMapper.selectByIds 全链路正常。
调用已有接口 GET /service/open/v2/apps/pending 和 GET /service/open/v2/apps/publish
验证 HTTP → Controller → Service → Mapper → DB 全链路可用。

注意：如果 market-server 未启动，测试会跳过（不视为失败）。
"""

import pytest
from common.client import api, ok


class TestApprovalSmoke:
    """审批模块重构冒烟测试"""

    @pytest.mark.L0
    def test_approval_pending_list(self):
        """调用待审批列表接口，验证全链路可用"""
        resp = api("GET", "/service/open/v2/apps/pending?curPage=1&pageSize=10")
        if resp is None:
            pytest.skip("market-server 未运行，跳过测试")
        assert ok(resp, 200, "待审批列表接口"), "待审批列表接口应返回 HTTP 200"

    @pytest.mark.L0
    def test_approval_published_list(self):
        """调用已上架列表接口，验证全链路可用"""
        resp = api("GET", "/service/open/v2/apps/publish?curPage=1&pageSize=10")
        if resp is None:
            pytest.skip("market-server 未运行，跳过测试")
        assert ok(resp, 200, "已上架列表接口"), "已上架列表接口应返回 HTTP 200"
