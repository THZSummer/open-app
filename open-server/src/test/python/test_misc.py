#!/usr/bin/env python3
"""杂项测试: API删除 + 事件删除 + 回调删除 + JSON校验"""
import time, json
import pytest
from conftest import api, db, TEST_APP_ID


def _snow_id():
    return int(time.time() * 1000000) % 100000000000000000


def _setup_category():
    cat_id = _snow_id()
    db(f"INSERT INTO openplatform_v2_category_t (id, name_cn, name_en, status, create_by, last_update_by) VALUES ({cat_id}, 'IT-Category', 'IT-Category', 1, 'tester', 'tester')")
    return cat_id


def _cleanup_category(cat_id):
    db(f"DELETE FROM openplatform_v2_category_t WHERE id = {cat_id}")


def _setup_api_test_data(cat_id, suffix):
    api_id = _snow_id()
    perm_id = _snow_id()
    scope = f"api:itdel:{suffix}"
    db(f"INSERT INTO openplatform_v2_api_t (id, name_cn, name_en, category_id, path, method, auth_type, status, create_by, last_update_by) VALUES ({api_id}, 'IT-API-{suffix}', 'IT-API-{suffix}', {cat_id}, '/it/del/{suffix}', 'GET', 1, 2, 'tester', 'tester')")
    db(f"INSERT INTO openplatform_v2_permission_t (id, name_cn, name_en, scope, resource_type, resource_id, category_id, need_approval, status, create_by, last_update_by) VALUES ({perm_id}, 'IT-Perm-{suffix}', 'IT-Perm-{suffix}', '{scope}', 'api', {api_id}, {cat_id}, 1, 1, 'tester', 'tester')")
    db(f"INSERT INTO openplatform_v2_permission_p_t (id, parent_id, property_name, property_value, status, create_by, last_update_by) VALUES ({_snow_id()}, {perm_id}, 'source', 'it-test', 1, 'tester', 'tester')")
    return api_id, perm_id


def _cleanup_api(api_id, perm_id):
    db(f"DELETE FROM openplatform_v2_permission_p_t WHERE parent_id = {perm_id}")
    db(f"DELETE FROM openplatform_v2_permission_t WHERE id = {perm_id}")
    db(f"DELETE FROM openplatform_v2_api_p_t WHERE parent_id = {api_id}")
    db(f"DELETE FROM openplatform_v2_api_t WHERE id = {api_id}")


def _setup_event_test_data(cat_id, suffix):
    event_id = _snow_id()
    perm_id = _snow_id()
    topic = f"it.del.event.{suffix}"
    scope = f"event:itdel:{suffix}"
    db(f"INSERT INTO openplatform_v2_event_t (id, name_cn, name_en, category_id, topic, status, create_by, last_update_by) VALUES ({event_id}, 'IT-Event-{suffix}', 'IT-Event-{suffix}', {cat_id}, '{topic}', 2, 'tester', 'tester')")
    db(f"INSERT INTO openplatform_v2_permission_t (id, name_cn, name_en, scope, resource_type, resource_id, category_id, need_approval, status, create_by, last_update_by) VALUES ({perm_id}, 'IT-Perm-{suffix}', 'IT-Perm-{suffix}', '{scope}', 'event', {event_id}, {cat_id}, 1, 1, 'tester', 'tester')")
    db(f"INSERT INTO openplatform_v2_permission_p_t (id, parent_id, property_name, property_value, status, create_by, last_update_by) VALUES ({_snow_id()}, {perm_id}, 'source', 'it-test', 1, 'tester', 'tester')")
    return event_id, perm_id


def _cleanup_event(event_id, perm_id):
    db(f"DELETE FROM openplatform_v2_permission_p_t WHERE parent_id = {perm_id}")
    db(f"DELETE FROM openplatform_v2_permission_t WHERE id = {perm_id}")
    db(f"DELETE FROM openplatform_v2_event_p_t WHERE parent_id = {event_id}")
    db(f"DELETE FROM openplatform_v2_event_t WHERE id = {event_id}")


def _setup_callback_test_data(cat_id, suffix):
    cb_id = _snow_id()
    perm_id = _snow_id()
    scope = f"callback:itdel:{suffix}"
    db(f"INSERT INTO openplatform_v2_callback_t (id, name_cn, name_en, category_id, status, create_by, last_update_by) VALUES ({cb_id}, 'IT-CB-{suffix}', 'IT-CB-{suffix}', {cat_id}, 2, 'tester', 'tester')")
    db(f"INSERT INTO openplatform_v2_permission_t (id, name_cn, name_en, scope, resource_type, resource_id, category_id, need_approval, status, create_by, last_update_by) VALUES ({perm_id}, 'IT-Perm-{suffix}', 'IT-Perm-{suffix}', '{scope}', 'callback', {cb_id}, {cat_id}, 1, 1, 'tester', 'tester')")
    db(f"INSERT INTO openplatform_v2_permission_p_t (id, parent_id, property_name, property_value, status, create_by, last_update_by) VALUES ({_snow_id()}, {perm_id}, 'source', 'it-test', 1, 'tester', 'tester')")
    return cb_id, perm_id


def _cleanup_callback(cb_id, perm_id):
    db(f"DELETE FROM openplatform_v2_permission_p_t WHERE parent_id = {perm_id}")
    db(f"DELETE FROM openplatform_v2_permission_t WHERE id = {perm_id}")
    db(f"DELETE FROM openplatform_v2_callback_p_t WHERE parent_id = {cb_id}")
    db(f"DELETE FROM openplatform_v2_callback_t WHERE id = {cb_id}")


def _setup_subscription(perm_id):
    sub_id = _snow_id()
    db(f"INSERT INTO openplatform_v2_subscription_t (id, app_id, permission_id, status, create_by, last_update_by) VALUES ({sub_id}, {_snow_id()}, {perm_id}, 1, 'tester', 'tester')")
    return sub_id


def _cleanup_subscription(sub_id):
    if sub_id:
        db(f"DELETE FROM openplatform_v2_subscription_t WHERE id = {sub_id}")


class TestApiDelete:
    @pytest.mark.L1
    def test_delete_nonexistent(self):
        resp = api("DELETE", "/apis/999999999999999999")
        if resp is not None:
            body = resp.json()
            assert str(body.get("code")) == "404"

    @pytest.mark.L1
    def test_delete_with_subscription(self):
        cat_id = _setup_category()
        try:
            api_id, perm_id = _setup_api_test_data(cat_id, "sub")
            sub_id = _setup_subscription(perm_id)
            try:
                resp = api("DELETE", f"/apis/{api_id}")
                if resp is not None:
                    body = resp.json()
                    assert str(body.get("code")) == "409"
                    msg = (body.get("messageZh") or "") + (body.get("messageEn") or "")
                    assert "订阅" in msg or "subscribed" in msg
            finally:
                _cleanup_subscription(sub_id)
                _cleanup_api(api_id, perm_id)
        finally:
            _cleanup_category(cat_id)

    @pytest.mark.L1
    def test_delete_without_subscription(self):
        cat_id = _setup_category()
        try:
            api_id, perm_id = _setup_api_test_data(cat_id, "nosub")
            try:
                resp = api("DELETE", f"/apis/{api_id}")
                if resp is not None:
                    body = resp.json()
                    assert str(body.get("code")) == "200"
            finally:
                _cleanup_api(api_id, perm_id)
        finally:
            _cleanup_category(cat_id)


class TestEventDelete:
    @pytest.mark.L1
    def test_delete_nonexistent(self):
        resp = api("DELETE", "/events/999999999999999999")
        if resp is not None:
            body = resp.json()
            assert str(body.get("code")) == "404"

    @pytest.mark.L1
    def test_delete_with_subscription(self):
        cat_id = _setup_category()
        try:
            event_id, perm_id = _setup_event_test_data(cat_id, "evsub")
            sub_id = _setup_subscription(perm_id)
            try:
                resp = api("DELETE", f"/events/{event_id}")
                if resp is not None:
                    body = resp.json()
                    assert str(body.get("code")) == "409"
                    msg = (body.get("messageZh") or "") + (body.get("messageEn") or "")
                    assert "订阅" in msg or "subscribed" in msg
            finally:
                _cleanup_subscription(sub_id)
                _cleanup_event(event_id, perm_id)
        finally:
            _cleanup_category(cat_id)

    @pytest.mark.L1
    def test_delete_without_subscription(self):
        cat_id = _setup_category()
        try:
            event_id, perm_id = _setup_event_test_data(cat_id, "evnosub")
            try:
                resp = api("DELETE", f"/events/{event_id}")
                if resp is not None:
                    body = resp.json()
                    assert str(body.get("code")) == "200"
            finally:
                _cleanup_event(event_id, perm_id)
        finally:
            _cleanup_category(cat_id)


class TestCallbackDelete:
    @pytest.mark.L1
    def test_delete_nonexistent(self):
        resp = api("DELETE", "/callbacks/999999999999999999")
        if resp is not None:
            body = resp.json()
            assert str(body.get("code")) == "404"

    @pytest.mark.L1
    def test_delete_with_subscription(self):
        cat_id = _setup_category()
        try:
            cb_id, perm_id = _setup_callback_test_data(cat_id, "cbsub")
            sub_id = _setup_subscription(perm_id)
            try:
                resp = api("DELETE", f"/callbacks/{cb_id}")
                if resp is not None:
                    body = resp.json()
                    assert str(body.get("code")) == "409"
                    msg = (body.get("messageZh") or "") + (body.get("messageEn") or "")
                    assert "订阅" in msg or "subscribed" in msg
            finally:
                _cleanup_subscription(sub_id)
                _cleanup_callback(cb_id, perm_id)
        finally:
            _cleanup_category(cat_id)

    @pytest.mark.L1
    def test_delete_without_subscription(self):
        cat_id = _setup_category()
        try:
            cb_id, perm_id = _setup_callback_test_data(cat_id, "cbnosub")
            try:
                resp = api("DELETE", f"/callbacks/{cb_id}")
                if resp is not None:
                    body = resp.json()
                    assert str(body.get("code")) == "200"
            finally:
                _cleanup_callback(cb_id, perm_id)
        finally:
            _cleanup_category(cat_id)


class TestJsonValidation:
    VALID_CONFIG = {
        "labelCn": "JSON测试", "protocol": "HTTP",
        "protocolConfig": {"url": "https://httpbin.org/get", "method": "GET"},
        "authConfig": {"type": "NONE", "fields": []},
        "inputContract": {"protocol": "HTTP", "header": {"type": "object", "properties": {}, "required": []}, "query": {"type": "object", "properties": {}, "required": []}, "body": {"type": "object", "properties": {}, "required": []}},
        "outputContract": {"protocol": "HTTP", "body": {"type": "object", "properties": {}}},
        "timeoutMs": 5000,
    }

    @pytest.mark.L2
    def test_publish_valid_json(self):
        cid = _snow_id()
        vid = _snow_id()
        try:
            cfg = json.dumps(self.VALID_CONFIG).replace("'", "''")
            db(f"INSERT INTO openplatform_v2_cp_connector_t (id, name_cn, name_en, connector_type, app_id, status, create_by, last_update_by) VALUES ({cid}, 'JSON-001', 'JSON-001', 1, {TEST_APP_ID}, 1, 'tester', 'tester')")
            db(f"INSERT INTO openplatform_v2_cp_connector_version_t (id, connector_id, connection_config, status, create_by, last_update_by) VALUES ({vid}, {cid}, '{cfg}', 1, 'tester', 'tester')")
            resp = api("PUT", f"/connectors/{cid}/versions/{vid}/publish")
            if resp is not None:
                assert resp.status_code == 200
                assert resp.json().get("code") in ("200", 200)
        finally:
            db(f"DELETE FROM openplatform_v2_cp_connector_version_t WHERE id = {vid}")
            db(f"DELETE FROM openplatform_v2_cp_connector_t WHERE id = {cid}")

    @pytest.mark.L2
    def test_publish_invalid_json(self):
        cid = _snow_id()
        vid = _snow_id()
        try:
            db(f"INSERT INTO openplatform_v2_cp_connector_t (id, name_cn, name_en, connector_type, app_id, status, create_by, last_update_by) VALUES ({cid}, 'JSON-002', 'JSON-002', 1, {TEST_APP_ID}, 1, 'tester', 'tester')")
            db(f"INSERT INTO openplatform_v2_cp_connector_version_t (id, connector_id, connection_config, status, create_by, last_update_by) VALUES ({vid}, {cid}, '{{invalid json!!!', 1, 'tester', 'tester')")
            resp = api("PUT", f"/connectors/{cid}/versions/{vid}/publish")
            if resp is not None:
                assert resp.status_code == 400
        finally:
            db(f"DELETE FROM openplatform_v2_cp_connector_version_t WHERE id = {vid}")
            db(f"DELETE FROM openplatform_v2_cp_connector_t WHERE id = {cid}")

    @pytest.mark.L2
    def test_publish_valid_json_unknown_schema(self):
        cid = _snow_id()
        vid = _snow_id()
        try:
            cfg = json.dumps({"foo": "bar", "baz": 123}).replace("'", "''")
            db(f"INSERT INTO openplatform_v2_cp_connector_t (id, name_cn, name_en, connector_type, app_id, status, create_by, last_update_by) VALUES ({cid}, 'JSON-003', 'JSON-003', 1, {TEST_APP_ID}, 1, 'tester', 'tester')")
            db(f"INSERT INTO openplatform_v2_cp_connector_version_t (id, connector_id, connection_config, status, create_by, last_update_by) VALUES ({vid}, {cid}, '{cfg}', 1, 'tester', 'tester')")
            resp = api("PUT", f"/connectors/{cid}/versions/{vid}/publish")
            if resp is not None:
                assert resp.status_code == 200
                assert resp.json().get("code") in ("200", 200)
        finally:
            db(f"DELETE FROM openplatform_v2_cp_connector_version_t WHERE id = {vid}")
            db(f"DELETE FROM openplatform_v2_cp_connector_t WHERE id = {cid}")
