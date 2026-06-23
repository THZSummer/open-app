#!/usr/bin/env python3
"""删除回调接口集成测试 (FR-015)

覆盖:
  - IT-DEL-CALLBACK-001: 删除不存在的回调 → 404
  - IT-DEL-CALLBACK-002: 删除有订阅的回调 → 409
  - IT-DEL-CALLBACK-003: 删除无订阅的回调 → 200
"""
from client import *
import subprocess, time


def snow_id():
    return int(time.time() * 1000000) % 100000000000000000


def _mysql_exec(sql):
    subprocess.run(["mysql", "-h192.168.3.155", "-uopenapp", "-popenapp", "openapp", "-e", sql],
                   check=True, capture_output=True)


def setup_category():
    cat_id = snow_id()
    _mysql_exec(
        f"INSERT INTO openplatform_v2_category_t (id, name_cn, name_en, status, create_by, last_update_by) "
        f"VALUES ({cat_id}, 'IT-回调删除测试', 'IT-Callback-Delete-Test', 1, 'tester', 'tester')"
    )
    return cat_id


def cleanup_category(cat_id):
    subprocess.run(["mysql", "-h192.168.3.155", "-uopenapp", "-popenapp", "openapp", "-e",
                    f"DELETE FROM openplatform_v2_category_t WHERE id = {cat_id}"],
                   capture_output=True)


def setup_callback_test_data(cat_id, suffix):
    cb_id = snow_id()
    scope = f"callback:itdel:{suffix}"
    perm_id = snow_id()

    _mysql_exec(
        f"INSERT INTO openplatform_v2_callback_t (id, name_cn, name_en, category_id, status, create_by, last_update_by) "
        f"VALUES ({cb_id}, 'IT-回调删除-{suffix}', 'IT-CB-Del-{suffix}', {cat_id}, 2, 'tester', 'tester')"
    )
    _mysql_exec(
        f"INSERT INTO openplatform_v2_permission_t (id, name_cn, name_en, scope, resource_type, resource_id, category_id, need_approval, status, create_by, last_update_by) "
        f"VALUES ({perm_id}, 'IT-回调权限-{suffix}', 'IT-CB-Perm-{suffix}', '{scope}', 'callback', {cb_id}, {cat_id}, 1, 1, 'tester', 'tester')"
    )
    _mysql_exec(
        f"INSERT INTO openplatform_v2_permission_p_t (id, parent_id, property_name, property_value, status, create_by, last_update_by) "
        f"VALUES ({snow_id()}, {perm_id}, 'source', 'it-test', 1, 'tester', 'tester')"
    )
    return cb_id, perm_id


def cleanup_callback(cb_id, perm_id):
    subprocess.run(["mysql", "-h192.168.3.155", "-uopenapp", "-popenapp", "openapp", "-e",
                    f"DELETE FROM openplatform_v2_permission_p_t WHERE parent_id = {perm_id}"],
                   capture_output=True)
    subprocess.run(["mysql", "-h192.168.3.155", "-uopenapp", "-popenapp", "openapp", "-e",
                    f"DELETE FROM openplatform_v2_permission_t WHERE id = {perm_id}"],
                   capture_output=True)
    subprocess.run(["mysql", "-h192.168.3.155", "-uopenapp", "-popenapp", "openapp", "-e",
                    f"DELETE FROM openplatform_v2_callback_p_t WHERE parent_id = {cb_id}"],
                   capture_output=True)
    subprocess.run(["mysql", "-h192.168.3.155", "-uopenapp", "-popenapp", "openapp", "-e",
                    f"DELETE FROM openplatform_v2_callback_t WHERE id = {cb_id}"],
                   capture_output=True)


def setup_subscription(perm_id):
    sub_id = snow_id()
    _mysql_exec(
        f"INSERT INTO openplatform_v2_subscription_t (id, app_id, permission_id, status, create_by, last_update_by) "
        f"VALUES ({sub_id}, {snow_id()}, {perm_id}, 1, 'tester', 'tester')"
    )
    return sub_id


def cleanup_subscription(sub_id):
    subprocess.run(["mysql", "-h192.168.3.155", "-uopenapp", "-popenapp", "openapp", "-e",
                    f"DELETE FROM openplatform_v2_subscription_t WHERE id = {sub_id}"],
                   capture_output=True)


# IT-DEL-CALLBACK-001
print("=== IT-DEL-CALLBACK-001: 删除不存在的回调 ===")
resp = request("DELETE", "/service/open/v2/callbacks/999999999999999999")
if resp is not None:
    body = resp.json()
    check("code 为 404", str(body.get("code")) == "404", f"code={body.get('code')}")

# IT-DEL-CALLBACK-002
print("\n=== IT-DEL-CALLBACK-002: 删除有订阅的回调 → 409 ===")
cat_id_002 = cb_id_002 = perm_id_002 = sub_id_002 = None
try:
    cat_id_002 = setup_category()
    cb_id_002, perm_id_002 = setup_callback_test_data(cat_id_002, "cbsub")
    sub_id_002 = setup_subscription(perm_id_002)

    resp = request("DELETE", f"/service/open/v2/callbacks/{cb_id_002}")
    if resp is not None:
        body = resp.json()
        check("code 为 409", str(body.get("code")) == "409", f"code={body.get('code')}")
        msg = (body.get("messageZh") or "") + (body.get("messageEn") or "")
        check("提示有订阅无法删除", "订阅" in msg or "subscribed" in msg)
finally:
    cleanup_subscription(sub_id_002)
    cleanup_callback(cb_id_002, perm_id_002)
    cleanup_category(cat_id_002)

# IT-DEL-CALLBACK-003
print("\n=== IT-DEL-CALLBACK-003: 删除无订阅的回调 → 200 ===")
cat_id_003 = cb_id_003 = perm_id_003 = None
try:
    cat_id_003 = setup_category()
    cb_id_003, perm_id_003 = setup_callback_test_data(cat_id_003, "cbnosub")

    resp = request("DELETE", f"/service/open/v2/callbacks/{cb_id_003}")
    if resp is not None:
        body = resp.json()
        check("code 为 200", str(body.get("code")) == "200", f"code={body.get('code')}")
finally:
    cleanup_callback(cb_id_003, perm_id_003)
    cleanup_category(cat_id_003)
