#!/usr/bin/env python3
"""订阅管理 — apps/{appId}/{apis,events,callbacks}/subscribe/withdraw"""
import pytest
from conftest import api, INTERNAL_APP_ID


def _create_api(category):
    r = api("POST", "/apis", {
        "nameCn": "perm_api", "nameEn": "perm_api",
        "categoryId": category, "method": "GET",
        "path": "/perm/test/api",
    })
    return r.json()["data"]["id"]


def _create_event(category):
    r = api("POST", "/events", {
        "nameCn": "perm_ev", "nameEn": "perm_ev",
        "categoryId": category, "topic": "pytest.perm.event",
    })
    return r.json()["data"]["id"]


def _create_callback(category):
    r = api("POST", "/callbacks", {
        "nameCn": "perm_cb", "nameEn": "perm_cb",
        "categoryId": category,
    })
    return r.json()["data"]["id"]


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
