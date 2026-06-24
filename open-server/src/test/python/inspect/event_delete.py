#!/usr/bin/env python3
"""删除事件接口集成测试 (FR-011)

覆盖:
  - IT-DEL-EVENT-001: 删除不存在的事件 → 404
  - IT-DEL-EVENT-002: 删除有订阅的事件 → 409
  - IT-DEL-EVENT-003: 删除无订阅的事件 → 200
"""
from client import api, db, ok, done
import time


def snow_id():
    return int(time.time() * 1000000) % 100000000000000000


def setup_category():
    cat_id = snow_id()
    db(
        f"INSERT INTO openplatform_v2_category_t (id, name_cn, name_en, status, create_by, last_update_by) "
        f"VALUES ({cat_id}, 'IT-事件删除测试', 'IT-Event-Delete-Test', 1, 'tester', 'tester')"
    )
    return cat_id


def cleanup_category(cat_id):
    db(f"DELETE FROM openplatform_v2_category_t WHERE id = {cat_id}")


def setup_event_test_data(cat_id, suffix):
    event_id = snow_id()
    topic = f"it.del.event.{suffix}"
    scope = f"event:itdel:{suffix}"
    perm_id = snow_id()

    db(
        f"INSERT INTO openplatform_v2_event_t (id, name_cn, name_en, category_id, topic, status, create_by, last_update_by) "
        f"VALUES ({event_id}, 'IT-事件删除-{suffix}', 'IT-Event-Del-{suffix}', {cat_id}, '{topic}', 2, 'tester', 'tester')"
    )
    db(
        f"INSERT INTO openplatform_v2_permission_t (id, name_cn, name_en, scope, resource_type, resource_id, category_id, need_approval, status, create_by, last_update_by) "
        f"VALUES ({perm_id}, 'IT-事件权限-{suffix}', 'IT-Event-Perm-{suffix}', '{scope}', 'event', {event_id}, {cat_id}, 1, 1, 'tester', 'tester')"
    )
    db(
        f"INSERT INTO openplatform_v2_permission_p_t (id, parent_id, property_name, property_value, status, create_by, last_update_by) "
        f"VALUES ({snow_id()}, {perm_id}, 'source', 'it-test', 1, 'tester', 'tester')"
    )
    return event_id, perm_id


def cleanup_event(event_id, perm_id):
    db(f"DELETE FROM openplatform_v2_permission_p_t WHERE parent_id = {perm_id}")
    db(f"DELETE FROM openplatform_v2_permission_t WHERE id = {perm_id}")
    db(f"DELETE FROM openplatform_v2_event_p_t WHERE parent_id = {event_id}")
    db(f"DELETE FROM openplatform_v2_event_t WHERE id = {event_id}")


def setup_subscription(perm_id):
    sub_id = snow_id()
    db(
        f"INSERT INTO openplatform_v2_subscription_t (id, app_id, permission_id, status, create_by, last_update_by) "
        f"VALUES ({sub_id}, {snow_id()}, {perm_id}, 1, 'tester', 'tester')"
    )
    return sub_id


def cleanup_subscription(sub_id):
    db(f"DELETE FROM openplatform_v2_subscription_t WHERE id = {sub_id}")


# IT-DEL-EVENT-001
print("=== IT-DEL-EVENT-001: 删除不存在的事件 ===")
resp = api("DELETE", "/events/999999999999999999")
if resp is not None:
    body = resp.json()
    ok(str(body.get("code")) == "404", name="code 为 404")

# IT-DEL-EVENT-002
print("\n=== IT-DEL-EVENT-002: 删除有订阅的事件 → 409 ===")
cat_id_002 = event_id_002 = perm_id_002 = sub_id_002 = None
try:
    cat_id_002 = setup_category()
    event_id_002, perm_id_002 = setup_event_test_data(cat_id_002, "evsub")
    sub_id_002 = setup_subscription(perm_id_002)

    resp = api("DELETE", f"/events/{event_id_002}")
    if resp is not None:
        body = resp.json()
        ok(str(body.get("code")) == "409", name="code 为 409")
        msg = (body.get("messageZh") or "") + (body.get("messageEn") or "")
        ok("订阅" in msg or "subscribed" in msg, name="提示有订阅无法删除")
finally:
    cleanup_subscription(sub_id_002)
    cleanup_event(event_id_002, perm_id_002)
    cleanup_category(cat_id_002)

# IT-DEL-EVENT-003
print("\n=== IT-DEL-EVENT-003: 删除无订阅的事件 → 200 ===")
cat_id_003 = event_id_003 = perm_id_003 = None
try:
    cat_id_003 = setup_category()
    event_id_003, perm_id_003 = setup_event_test_data(cat_id_003, "evnosub")

    resp = api("DELETE", f"/events/{event_id_003}")
    if resp is not None:
        body = resp.json()
        ok(str(body.get("code")) == "200", name="code 为 200")
finally:
    cleanup_event(event_id_003, perm_id_003)
    cleanup_category(cat_id_003)

done()
