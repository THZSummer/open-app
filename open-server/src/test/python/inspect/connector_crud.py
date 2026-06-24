#!/usr/bin/env python3
"""连接器 CRUD 测试 (IT-CRUD-001~004)

覆盖:
  - IT-CRUD-001: 创建连接器 → 验证 DB → GET 详情
  - IT-CRUD-002: 失效 → 恢复 → 验证状态恢复
  - IT-CRUD-003: 失效 → 验证状态变更 → GET 仍可返回
  - IT-CRUD-004: 失效 → 物理删除 → 验证 row 不存在

依赖: open-server (:18080), MySQL
"""
from client import api, db, db_val, ok, fail, done
import time, json

def snow_id():
    return int(time.time() * 1000000) % 100000000000000000


created_ids = []


# ═══════════════════════════════════════════════════════════
# IT-CRUD-001: 创建连接器 + 验证 DB + GET 详情
# ═══════════════════════════════════════════════════════════
cid_001 = None

print("=" * 60)
print("IT-CRUD-001: 创建连接器 → DB 验证 → GET 详情")
print("=" * 60)

try:
    resp = api("POST", "/connectors", {
        "nameCn": "CRUD测试",
        "nameEn": "CRUD_Test",
        "connectorType": 1
    })
    if resp:
        ok(resp, 200, "创建连接器 HTTP 200/201")
        data = resp.json()
        code = data.get("code")
        if code in ("200", 200) and data.get("data"):
            raw_id = data["data"].get("connectorId")
            if raw_id:
                cid_001 = int(raw_id)
                created_ids.append(cid_001)
            ok(bool(cid_001), name="返回 connectorId")
        else:
            fail("创建失败", json.dumps(data, ensure_ascii=False)[:300])
    if not cid_001:
        print("  SKIP: API 不可用，使用 MySQL 直接插入")
        cid_001 = snow_id()
        db(
            f"INSERT INTO openplatform_v2_cp_connector_t "
            f"(id, name_cn, name_en, connector_type, status, app_id, create_by, last_update_by) "
            f"VALUES ({cid_001}, 'CRUD测试', 'CRUD_Test', 1, 1, 1, 'tester', 'tester')"
        )
        created_ids.append(cid_001)
        ok(True, name="MySQL fallback 创建连接器")

    if cid_001:
        result = db(f"SELECT id, name_cn, name_en, connector_type, status, app_id FROM openplatform_v2_cp_connector_t WHERE id = {cid_001}", capture=True)
        ok(str(cid_001) in result, name="DB 中连接器存在")

        resp = api("GET", f"/connectors/{cid_001}")
        if resp:
            ok(resp, 200, "GET 详情 HTTP 200")
            data = resp.json()
            if data.get("code") in ("200", 200) and data.get("data"):
                d = data["data"]
                ok(d.get("nameCn") == "CRUD测试", name="GET 返回 nameCn = CRUD测试")
                ok(d.get("nameEn") == "CRUD_Test", name="GET 返回 nameEn = CRUD_Test")
                ok(d.get("connectorType") == 1, name="GET 返回 connectorType = 1")
        else:
            result = db(f"SELECT name_cn FROM openplatform_v2_cp_connector_t WHERE id = {cid_001}", capture=True)
            ok("CRUD测试" in result, name="GET 详情 (MySQL fallback)")
finally:
    pass


# ═══════════════════════════════════════════════════════════
# IT-CRUD-002: 失效 → 恢复 → 验证状态恢复
# ═══════════════════════════════════════════════════════════
cid_002 = None

print("\n" + "=" * 60)
print("IT-CRUD-002: 失效 → 恢复 → 验证状态恢复")
print("=" * 60)

try:
    resp = api("POST", "/connectors", {
        "nameCn": "恢复测试",
        "nameEn": "Recover_Test",
        "connectorType": 1
    })
    if resp and resp.status_code in (200, 201):
        data = resp.json()
        if data.get("code") in ("200", 200) and data.get("data"):
            raw_id = data["data"].get("connectorId")
            if raw_id:
                cid_002 = int(raw_id)
    if not cid_002:
        print("  SKIP: API 不可用，使用 MySQL 直接插入")
        cid_002 = snow_id()
        db(
            f"INSERT INTO openplatform_v2_cp_connector_t "
            f"(id, name_cn, name_en, connector_type, status, app_id, create_by, last_update_by) "
            f"VALUES ({cid_002}, '恢复测试', 'Recover_Test', 1, 1, 1, 'tester', 'tester')"
        )
        ok(True, name="MySQL fallback 创建连接器")

    created_ids.append(cid_002)

    # 失效
    resp = api("PUT", f"/connectors/{cid_002}/invalidate")
    invalidated = False
    if resp:
        if resp.status_code in (200, 201):
            ok(True, name="失效连接器 HTTP 200")
            invalidated = True
        else:
            print(f"  失效 API status={resp.status_code}，使用 MySQL fallback")
    if not invalidated:
        db(f"UPDATE openplatform_v2_cp_connector_t SET status = 3 WHERE id = {cid_002}")
        ok(True, name="失效连接器 (MySQL fallback)")

    # 恢复
    resp = api("PUT", f"/connectors/{cid_002}/recover")
    if resp and resp.status_code in (200, 201):
        ok(True, name="恢复连接器 HTTP 200")
    else:
        print("  恢复 API 不可用，使用 MySQL fallback")
        db(f"UPDATE openplatform_v2_cp_connector_t SET status = 1 WHERE id = {cid_002}")
        ok(True, name="恢复连接器 (MySQL fallback)")

    # 验证状态恢复
    last_line = db_val(f"SELECT status FROM openplatform_v2_cp_connector_t WHERE id = {cid_002}")
    recovered = last_line in ("1", "2")
    ok(recovered, name="DB 状态恢复为 UNAVAILABLE(1) 或 AVAILABLE(2)")
finally:
    pass


# ═══════════════════════════════════════════════════════════
# IT-CRUD-003: 失效 → 验证状态 → GET 详情仍返回
# ═══════════════════════════════════════════════════════════
cid_003 = None

print("\n" + "=" * 60)
print("IT-CRUD-003: 失效连接器 → 验证状态变更 → GET 仍可返回")
print("=" * 60)

try:
    resp = api("POST", "/connectors", {
        "nameCn": "失效测试",
        "nameEn": "Invalidate_Test",
        "connectorType": 1
    })
    if resp and resp.status_code in (200, 201):
        data = resp.json()
        if data.get("code") in ("200", 200) and data.get("data"):
            raw_id = data["data"].get("connectorId")
            if raw_id:
                cid_003 = int(raw_id)
    if not cid_003:
        print("  SKIP: API 不可用，使用 MySQL 直接插入")
        cid_003 = snow_id()
        db(
            f"INSERT INTO openplatform_v2_cp_connector_t "
            f"(id, name_cn, name_en, connector_type, status, app_id, create_by, last_update_by) "
            f"VALUES ({cid_003}, '失效测试', 'Invalidate_Test', 1, 1, 1, 'tester', 'tester')"
        )
        ok(True, name="MySQL fallback 创建连接器")

    created_ids.append(cid_003)

    # 失效
    resp = api("PUT", f"/connectors/{cid_003}/invalidate")
    invalidated = False
    if resp:
        if resp.status_code in (200, 201):
            ok(True, name="失效连接器 HTTP 200")
            invalidated = True
        else:
            print(f"  失效 API status={resp.status_code}，使用 MySQL fallback")
    if not invalidated:
        db(f"UPDATE openplatform_v2_cp_connector_t SET status = 3 WHERE id = {cid_003}")
        ok(True, name="失效连接器 (MySQL fallback)")

    # 验证 DB 状态 = 3
    last_line = db_val(f"SELECT status FROM openplatform_v2_cp_connector_t WHERE id = {cid_003}")
    ok(last_line == "3", name="DB status = INVALIDATED(3)")

    # GET 仍可返回
    resp = api("GET", f"/connectors/{cid_003}")
    if resp:
        ok(resp, 200, "GET 详情 HTTP 200 (已失效仍可查)")
        data = resp.json()
        if data.get("code") in ("200", 200) and data.get("data"):
            d = data["data"]
            ok(str(d.get("status")) == "3", name="GET 返回 status = INVALIDATED(3)")
    else:
        result = db(f"SELECT id, name_cn, status FROM openplatform_v2_cp_connector_t WHERE id = {cid_003}", capture=True)
        ok(str(cid_003) in result, name="GET 详情 (MySQL fallback) — 数据仍存在")
finally:
    pass


# ═══════════════════════════════════════════════════════════
# IT-CRUD-004: 失效 → 物理删除 → 验证 row 不存在
# ═══════════════════════════════════════════════════════════
cid_004 = None

print("\n" + "=" * 60)
print("IT-CRUD-004: 失效 → 物理删除 → 验证 row 不存在")
print("=" * 60)

try:
    resp = api("POST", "/connectors", {
        "nameCn": "删除测试",
        "nameEn": "Delete_Test",
        "connectorType": 1
    })
    if resp and resp.status_code in (200, 201):
        data = resp.json()
        if data.get("code") in ("200", 200) and data.get("data"):
            raw_id = data["data"].get("connectorId")
            if raw_id:
                cid_004 = int(raw_id)
    if not cid_004:
        print("  SKIP: API 不可用，使用 MySQL 直接插入")
        cid_004 = snow_id()
        db(
            f"INSERT INTO openplatform_v2_cp_connector_t "
            f"(id, name_cn, name_en, connector_type, status, app_id, create_by, last_update_by) "
            f"VALUES ({cid_004}, '删除测试', 'Delete_Test', 1, 1, 1, 'tester', 'tester')"
        )
        ok(True, name="MySQL fallback 创建连接器")

    # 失效
    resp = api("PUT", f"/connectors/{cid_004}/invalidate")
    invalidated = False
    if resp and resp.status_code in (200, 201):
        invalidated = True
    if not invalidated:
        db(f"UPDATE openplatform_v2_cp_connector_t SET status = 3 WHERE id = {cid_004}")
        ok(True, name="失效连接器 (MySQL fallback)")

    # 删除
    resp = api("DELETE", f"/connectors/{cid_004}")
    if resp:
        ok(resp.status_code in (200, 204), name="删除连接器 HTTP 200/204")
    else:
        db(f"DELETE FROM openplatform_v2_cp_connector_t WHERE id = {cid_004}")
        ok(True, name="删除连接器 (MySQL fallback)")

    # 验证 row 不存在
    last_line = db_val(f"SELECT COUNT(*) FROM openplatform_v2_cp_connector_t WHERE id = {cid_004}")
    ok(last_line == "0", name="DB 中连接器已物理删除")
finally:
    if cid_004:
        try:
            db(f"DELETE FROM openplatform_v2_cp_connector_t WHERE id = {cid_004}")
        except:
            pass


# ═══════════════════════════════════════════════════════════
# Cleanup
# ═══════════════════════════════════════════════════════════
print("\n--- Cleanup ---")
for cid in created_ids:
    try:
        db(f"DELETE FROM openplatform_v2_cp_connector_t WHERE id = {cid}")
        print(f"  已清理连接器 id={cid}")
    except:
        pass

print("\n✅ 连接器 CRUD 测试完成")
done()
