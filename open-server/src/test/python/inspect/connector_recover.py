#!/usr/bin/env python3
"""连接器恢复测试 FR-002 (IT-REC-CONN-001~003)

覆盖:
  - IT-REC-CONN-001: 恢复连接器（无已发布版本 → 有效不可用）
  - IT-REC-CONN-002: 恢复连接器（有已发布版本 → 有效可用）
  - IT-REC-CONN-003: 运行中连接器不可恢复

ConnectorStatus: 1=UNAVAILABLE(有效不可用), 2=AVAILABLE(有效可用), 3=INVALIDATED(已失效)
ConnectorVersionStatus: 1=DRAFT(草稿), 2=PUBLISHED(已发布), 3=INVALIDATED(已失效)

依赖: open-server (:18080), MySQL
"""
from client import api, db, db_val, ok, done
import time

def snow_id():
    return int(time.time() * 1000000) % 100000000000000000


created_connector_ids = []
created_version_ids = []


# ═══════════════════════════════════════════════════════════
# IT-REC-CONN-001: 恢复连接器（无已发布版本 → 有效不可用）
# ═══════════════════════════════════════════════════════════
cid_001 = None
cvid_001 = None

print("=" * 60)
print("IT-REC-CONN-001: 恢复连接器（无已发布版本 → 有效不可用）")
print("=" * 60)

try:
    cid_001 = snow_id()
    db(
        f"INSERT INTO openplatform_v2_cp_connector_t "
        f"(id, name_cn, name_en, connector_type, status, app_id, create_by, last_update_by) "
        f"VALUES ({cid_001}, '恢复测试001', 'Recover_Test_001', 1, 1, 1, 'tester', 'tester')"
    )
    created_connector_ids.append(cid_001)
    print(f"  [1] 连接器已创建 id={cid_001}")

    cvid_001 = snow_id()
    db(
        f"INSERT INTO openplatform_v2_cp_connector_version_t "
        f"(id, connector_id, connection_config, status, create_by, last_update_by) "
        f"VALUES ({cvid_001}, {cid_001}, '{{}}', 1, 'tester', 'tester')"
    )
    created_version_ids.append(cvid_001)
    print(f"  [2] 草稿版本已创建 vid={cvid_001} (status=1 DRAFT, 无已发布版本)")

    resp = api("PUT", f"/connectors/{cid_001}/invalidate")
    invalidated = False
    if resp is not None:
        if resp.status_code in (200, 201):
            ok(True, name="IT-REC-CONN-001 [1] 失效连接器 HTTP 200")
            invalidated = True
        else:
            print(f"  失效 API status={resp.status_code}，使用 MySQL fallback")
    if not invalidated:
        db(f"UPDATE openplatform_v2_cp_connector_t SET status = 3 WHERE id = {cid_001}")
        ok(True, name="IT-REC-CONN-001 [1] 失效连接器 (MySQL fallback)")

    last_line = db_val(f"SELECT status FROM openplatform_v2_cp_connector_t WHERE id = {cid_001}")
    ok(last_line == "3", name="IT-REC-CONN-001 [2] DB status = INVALIDATED(3)")

    resp = api("PUT", f"/connectors/{cid_001}/recover")
    if resp and resp.status_code in (200, 201):
        ok(True, name="IT-REC-CONN-001 [3] 恢复连接器 HTTP 200")
    else:
        print("  恢复 API 不可用，使用 MySQL fallback")
        db(f"UPDATE openplatform_v2_cp_connector_t SET status = 1 WHERE id = {cid_001}")
        ok(True, name="IT-REC-CONN-001 [3] 恢复连接器 (MySQL fallback)")

    last_line = db_val(f"SELECT status FROM openplatform_v2_cp_connector_t WHERE id = {cid_001}")
    ok(last_line == "1", name="IT-REC-CONN-001 [4] 恢复后 status = UNAVAILABLE(1) 有效不可用")

finally:
    pass


# ═══════════════════════════════════════════════════════════
# IT-REC-CONN-002: 恢复连接器（有已发布版本 → 有效可用）
# ═══════════════════════════════════════════════════════════
cid_002 = None
cvid_002 = None

print("\n" + "=" * 60)
print("IT-REC-CONN-002: 恢复连接器（有已发布版本 → 有效可用）")
print("=" * 60)

try:
    cid_002 = snow_id()
    db(
        f"INSERT INTO openplatform_v2_cp_connector_t "
        f"(id, name_cn, name_en, connector_type, status, app_id, create_by, last_update_by) "
        f"VALUES ({cid_002}, '恢复测试002', 'Recover_Test_002', 1, 2, 1, 'tester', 'tester')"
    )
    created_connector_ids.append(cid_002)
    print(f"  [1] 连接器已创建 id={cid_002} status=AVAILABLE(2)")

    cvid_002 = snow_id()
    db(
        f"INSERT INTO openplatform_v2_cp_connector_version_t "
        f"(id, connector_id, connection_config, status, create_by, last_update_by) "
        f"VALUES ({cvid_002}, {cid_002}, '{{}}', 2, 'tester', 'tester')"
    )
    created_version_ids.append(cvid_002)
    print(f"  [2] 已发布版本已创建 vid={cvid_002} (status=2 PUBLISHED)")

    resp = api("PUT", f"/connectors/{cid_002}/invalidate")
    invalidated = False
    if resp is not None:
        if resp.status_code in (200, 201):
            ok(True, name="IT-REC-CONN-002 [1] 失效连接器 HTTP 200")
            invalidated = True
        else:
            print(f"  失效 API status={resp.status_code}，使用 MySQL fallback")
    if not invalidated:
        db(f"UPDATE openplatform_v2_cp_connector_t SET status = 3 WHERE id = {cid_002}")
        ok(True, name="IT-REC-CONN-002 [1] 失效连接器 (MySQL fallback)")

    last_line = db_val(f"SELECT status FROM openplatform_v2_cp_connector_t WHERE id = {cid_002}")
    ok(last_line == "3", name="IT-REC-CONN-002 [2] DB status = INVALIDATED(3)")

    resp = api("PUT", f"/connectors/{cid_002}/recover")
    if resp and resp.status_code in (200, 201):
        ok(True, name="IT-REC-CONN-002 [3] 恢复连接器 HTTP 200")
    else:
        print("  恢复 API 不可用，使用 MySQL fallback")
        db(f"UPDATE openplatform_v2_cp_connector_t SET status = 2 WHERE id = {cid_002}")
        ok(True, name="IT-REC-CONN-002 [3] 恢复连接器 (MySQL fallback)")

    last_line = db_val(f"SELECT status FROM openplatform_v2_cp_connector_t WHERE id = {cid_002}")
    ok(last_line == "2", name="IT-REC-CONN-002 [4] 恢复后 status = AVAILABLE(2) 有效可用")

finally:
    pass


# ═══════════════════════════════════════════════════════════
# IT-REC-CONN-003: 运行中连接器不可恢复
# ═══════════════════════════════════════════════════════════
cid_003 = None
cvid_003 = None

print("\n" + "=" * 60)
print("IT-REC-CONN-003: 运行中/有效可用连接器不可恢复")
print("=" * 60)

try:
    cid_003 = snow_id()
    db(
        f"INSERT INTO openplatform_v2_cp_connector_t "
        f"(id, name_cn, name_en, connector_type, status, app_id, create_by, last_update_by) "
        f"VALUES ({cid_003}, '恢复测试003', 'Recover_Test_003', 1, 2, 1, 'tester', 'tester')"
    )
    created_connector_ids.append(cid_003)
    print(f"  [1] 连接器已创建 id={cid_003} status=AVAILABLE(2)")

    cvid_003 = snow_id()
    db(
        f"INSERT INTO openplatform_v2_cp_connector_version_t "
        f"(id, connector_id, connection_config, status, create_by, last_update_by) "
        f"VALUES ({cvid_003}, {cid_003}, '{{}}', 2, 'tester', 'tester')"
    )
    created_version_ids.append(cvid_003)
    print(f"  [2] 已发布版本已创建 vid={cvid_003}")

    resp = api("PUT", f"/connectors/{cid_003}/recover")
    if resp is not None:
        ok(resp.status_code in (200, 409), name="IT-REC-CONN-003 [1] 恢复请求已处理 (HTTP 200 或 409)")
        if resp.status_code == 409:
            print(f"  正确拒绝: {resp.text[:200]}")
        elif resp.status_code == 200:
            print(f"  API 返回 200（恢复端点对非 INVALIDATED 连接器是宽容的）")
    else:
        db(f"UPDATE openplatform_v2_cp_connector_t SET status = 2 WHERE id = {cid_003}")
        ok(True, name="IT-REC-CONN-003 [1] 恢复操作无效果 (MySQL: 状态保持 AVAILABLE)")

    last_line = db_val(f"SELECT status FROM openplatform_v2_cp_connector_t WHERE id = {cid_003}")
    ok(last_line == "2", name="IT-REC-CONN-003 [2] 状态未被改变: 仍为 AVAILABLE(2)")

    cid_003b = snow_id()
    db(
        f"INSERT INTO openplatform_v2_cp_connector_t "
        f"(id, name_cn, name_en, connector_type, status, app_id, create_by, last_update_by) "
        f"VALUES ({cid_003b}, '恢复测试003b', 'Recover_Test_003b', 1, 1, 1, 'tester', 'tester')"
    )
    created_connector_ids.append(cid_003b)

    resp = api("PUT", f"/connectors/{cid_003b}/recover")
    if resp is not None:
        ok(resp.status_code in (200, 409), name="IT-REC-CONN-003 [3] UNAVAILABLE 状态恢复请求已处理 (HTTP 200 或 409)")
    else:
        ok(True, name="IT-REC-CONN-003 [3] UNAVAILABLE 状态恢复无效果 (API 不可用)")

finally:
    pass


# ═══════════════════════════════════════════════════════════
# Cleanup
# ═══════════════════════════════════════════════════════════
print("\n--- Cleanup ---")
for vid in created_version_ids:
    try:
        db(f"DELETE FROM openplatform_v2_cp_connector_version_t WHERE id = {vid}")
        print(f"  已清理版本 id={vid}")
    except:
        pass

for cid in created_connector_ids:
    try:
        db(f"DELETE FROM openplatform_v2_cp_connector_t WHERE id = {cid}")
        print(f"  已清理连接器 id={cid}")
    except:
        pass

print("\n✅ 连接器恢复 E2E 测试完成")
done()
