#!/usr/bin/env python3
"""订阅管理 — apps/{appId}/{apis,events,callbacks} 列表查询"""
import pytest
from conftest import api, INTERNAL_APP_ID


class TestApiSubscription:
    @pytest.mark.L1
    def test_list_app_apis(self):
        resp = api("GET", f"/apps/{INTERNAL_APP_ID}/apis")
        assert resp.status_code == 200
        assert resp.json()["code"] == "200"

    @pytest.mark.L1
    def test_list_category_apis(self, category):
        resp = api("GET", f"/categories/{category}/apis")
        assert resp.status_code == 200


class TestEventSubscription:
    @pytest.mark.L1
    def test_list_app_events(self):
        resp = api("GET", f"/apps/{INTERNAL_APP_ID}/events")
        assert resp.status_code == 200

    @pytest.mark.L1
    def test_list_category_events(self, category):
        resp = api("GET", f"/categories/{category}/events")
        assert resp.status_code == 200


class TestCallbackSubscription:
    @pytest.mark.L1
    def test_list_app_callbacks(self):
        resp = api("GET", f"/apps/{INTERNAL_APP_ID}/callbacks")
        assert resp.status_code == 200

    @pytest.mark.L1
    def test_list_category_callbacks(self, category):
        resp = api("GET", f"/categories/{category}/callbacks")
        assert resp.status_code == 200
