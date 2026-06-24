#!/usr/bin/env python3
"""删除 API 接口集成测试 (FR-007)

覆盖:
  - IT-DEL-API-001: 删除不存在的 API → 404
  - IT-DEL-API-002: 删除有订阅的 API → 409
  - IT-DEL-API-003: 删除无订阅的 API → 200
"""
from client import api, db, ok, done
import time


def snow_id():
    return int(time.time() * 1000000) % 100000000000000000


def setup_category():
    cat_id = snow_id()
    db(
        f"INSERT INTO openplatform_v2_category_t (id, name_cn, name_en, status, create_by, last_update_by) "
        f"VALUES ({cat_id}, 'IT-API删除测试', 'IT-API-Delete-Test', 1, 'tester', 'tester')"
    )
    return cat_id


def cleanup_category(cat_id):
    db(f"DELETE FROM openplatform_v2_category_t WHERE id = {cat_id}")


def setup_api_test_data(cat_id, suffix):
    api_id = snow_id()
    scope = f"api:itdel:{suffix}"
    perm_id = snow_id()

    db(
        f"INSERT INTO openplatform_v2_api_t (id, name_cn, name_en, category_id, path, method, auth_type, status, create_by, last_update_by) "
        f"VALUES ({api_id}, 'IT-API删除-{suffix}', 'IT-API-Del-{suffix}', {cat_id}, '/it/del/{suffix}', 'GET', 1, 2, 'tester', 'tester')"
    )
    db(
        f"INSERT INTO openplatform_v2_permission_t (id, name_cn, name_en, scope, resource_type, resource_id, category_id, need_approval, status, create_by, last_update_by) "
        f"VALUES ({perm_id}, 'IT-API权限-{suffix}', 'IT-API-Perm-{suffix}', '{scope}', 'api', {api_id}, {cat_id}, 1, 1, 'tester', 'tester')"
    )
    db(
        f"INSERT INTO openplatform_v2_permission_p_t (id, parent_id, property_name, property_value, status, create_by, last_update_by) "
        f"VALUES ({snow_id()}, {perm_id}, 'source', 'it-test', 1, 'tester', 'tester')"
    )
    return api_id, perm_id


def cleanup_api(api_id, perm_id):
    db(f"DELETE FROM openplatform_v2_permission_p_t WHERE parent_id = {perm_id}")
    db(f"DELETE FROM openplatform_v2_permission_t WHERE id = {perm_id}")
    db(f"DELETE FROM openplatform_v2_api_p_t WHERE parent_id = {api_id}")
    db(f"DELETE FROM openplatform_v2_api_t WHERE id = {api_id}")


def setup_subscription(perm_id):
    sub_id = snow_id()
    db(
        f"INSERT INTO openplatform_v2_subscription_t (id, app_id, permission_id, status, create_by, last_update_by) "
        f"VALUES ({sub_id}, {snow_id()}, {perm_id}, 1, 'tester', 'tester')"
    )
    return sub_id


def cleanup_subscription(sub_id):
    db(f"DELETE FROM openplatform_v2_subscription_t WHERE id = {sub_id}")


# ═══════════════════════════════════════════════════════════
# IT-DEL-API-001: 不存在的 API
# ═══════════════════════════════════════════════════════════
print("=== IT-DEL-API-001: 删除不存在的 API ===")
resp = api("DELETE", "/apis/999999999999999999")
if resp is not None:
    body = resp.json()
    ok(str(body.get("code")) == "404", name="code 为 404")

# ═══════════════════════════════════════════════════════════
# IT-DEL-API-002: 有订阅的 API → 409
# ═══════════════════════════════════════════════════════════
print("\n=== IT-DEL-API-002: 删除有订阅的 API → 409 ===")
cat_id_002 = api_id_002 = perm_id_002 = sub_id_002 = None
try:
    cat_id_002 = setup_category()
    api_id_002, perm_id_002 = setup_api_test_data(cat_id_002, "sub")
    sub_id_002 = setup_subscription(perm_id_002)

    resp = api("DELETE", f"/apis/{api_id_002}")
    if resp is not None:
        body = resp.json()
        ok(str(body.get("code")) == "409", name="code 为 409")
        msg = (body.get("messageZh") or "") + (body.get("messageEn") or "")
        ok("订阅" in msg or "subscribed" in msg, name="提示有订阅无法删除")
finally:
    cleanup_subscription(sub_id_002)
    cleanup_api(api_id_002, perm_id_002)
    cleanup_category(cat_id_002)

# ═══════════════════════════════════════════════════════════
# IT-DEL-API-003: 无订阅的 API → 200
# ═══════════════════════════════════════════════════════════
print("\n=== IT-DEL-API-003: 删除无订阅的 API → 200 ===")
cat_id_003 = api_id_003 = perm_id_003 = None
try:
    cat_id_003 = setup_category()
    api_id_003, perm_id_003 = setup_api_test_data(cat_id_003, "nosub")

    resp = api("DELETE", f"/apis/{api_id_003}")
    if resp is not None:
        body = resp.json()
        ok(str(body.get("code")) == "200", name="code 为 200")
        ok(body.get("data") is None, name="data 为空")
finally:
    cleanup_api(api_id_003, perm_id_003)
    cleanup_category(cat_id_003)

done()
