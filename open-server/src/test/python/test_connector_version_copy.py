#!/usr/bin/env python3
"""#13 POST /connectors/{id}/versions/{vid}/copy-to-draft — 复制版本到草稿"""
import pytest
from _client import api


class TestConnectorVersionCopy:
    @pytest.mark.L2
    def test_copy_to_draft(self, published_connector):
        cid, vid = published_connector
        resp = api("POST", f"/connectors/{cid}/versions/{vid}/copy-to-draft")
        assert resp.status_code in (200, 201)
