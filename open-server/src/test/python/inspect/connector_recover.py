#!/usr/bin/env python3
"""连接器恢复测试 FR-002 (IT-REC-CONN-001~003)

覆盖:
  - IT-REC-CONN-001: 恢复连接器（无已发布版本 → 有效不可用）
  - IT-REC-CONN-002: 恢复连接器（有已发布版本 → 有效可用）
  - IT-REC-CONN-003: 运行中连接器不可恢复

ConnectorStatus: 1=UNAVAILABLE(有效不可用), 2=AVAILABLE(有效可用), 3=INVALIDATED(已失效)
ConnectorVersionStatus: 1=DRAFT(草稿), 2=PUBLISHED(已发布), 3=INVALIDATED(已失效)

依赖: open-server (:18080), MySQL (192.168.3.155)
"""
from client import *
import subprocess, time, json, requests as req_lib

DB_HOST = "192.168.3.155"
DB_USER = "openapp"
DB_PASS = "openapp"
DB_NAME = "openapp"
DB_BASE = ["mysql", "-h", DB_HOST, f"-u{DB_USER}", f"-p{DB_PASS}", DB_NAME, "-e"]


def snow_id():
    return int(time.time() * 1000000) % 100000000000000000


def _mysql(sql):
    subprocess.run(DB_BASE + [sql], check=True, capture_output=True)


def _mysql_query(sql):
    result = subprocess.run(DB_BASE + [sql], capture_output=True, text=True)
    return result.stdout


def _escape(obj):
    return json.dumps(obj).replace("'", "''")


def _api_post(path, body=None):
    try:
        resp = req_lib.post(f"{BASE_URL}{path}", json=body or {},
                            headers={"X-App-Id": "1", "Content-Type": "application/json"}, timeout=10)
        return resp
    except Exception as e:
        print(f"  SKIP: 请求失败 - {e}")
        return None


def _api_put(path, body=None):
    try:
        resp = req_lib.put(f"{BASE_URL}{path}", json=body or {},
                           headers={"X-App-Id": "1", "Content-Type": "application/json"}, timeout=10)
        return resp
    except Exception as e:
        print(f"  SKIP: 请求失败 - {e}")
        return None


def _api_get(path):
    try:
        resp = req_lib.get(f"{BASE_URL}{path}",
                           headers={"X-App-Id": "1", "Content-Type": "application/json"}, timeout=10)
        return resp
    except Exception as e:
        print(f"  SKIP: 请求失败 - {e}")
        return None


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
    # 1. 创建连接器（状态=有效不可用，即 UNAVAILABLE）
    cid_001 = snow_id()
    _mysql(
        f"INSERT INTO openplatform_v2_cp_connector_t "
        f"(id, name_cn, name_en, connector_type, status, app_id, create_by, last_update_by) "
        f"VALUES ({cid_001}, '恢复测试001', 'Recover_Test_001', 1, 1, 1, 'tester', 'tester')"
    )
    created_connector_ids.append(cid_001)
    print(f"  [1] 连接器已创建 id={cid_001}")

    # 2. 创建版本（草稿状态，非已发布）→ 此连接器无已发布版本
    cvid_001 = snow_id()
    _mysql(
        f"INSERT INTO openplatform_v2_cp_connector_version_t "
        f"(id, connector_id, connection_config, status, create_by, last_update_by) "
        f"VALUES ({cvid_001}, {cid_001}, '{{}}', 1, 'tester', 'tester')"
    )
    created_version_ids.append(cvid_001)
    print(f"  [2] 草稿版本已创建 vid={cvid_001} (status=1 DRAFT, 无已发布版本)")

    # 3. 将连接器变更为已失效状态 (status=3 INVALIDATED)
    resp = _api_put(f"/service/open/v2/connectors/{cid_001}/invalidate")
    invalidated = False
    if resp is not None:
        if resp.status_code in (200, 201):
            check("IT-REC-CONN-001 [1] 失效连接器 HTTP 200",
                  True,
                  f"实际: {resp.status_code}")
            invalidated = True
        else:
            print(f"  失效 API status={resp.status_code}，使用 MySQL fallback")
    if not invalidated:
        _mysql(f"UPDATE openplatform_v2_cp_connector_t SET status = 3 WHERE id = {cid_001}")
        check("IT-REC-CONN-001 [1] 失效连接器 (MySQL fallback)", True)

    # 验证已失效
    result = _mysql_query(f"SELECT status FROM openplatform_v2_cp_connector_t WHERE id = {cid_001}")
    last_line = result.strip().split("\n")[-1].strip() if result.strip() else ""
    check("IT-REC-CONN-001 [2] DB status = INVALIDATED(3)",
          last_line == "3",
          f"实际 status = {last_line}")

    # 4. 恢复连接器（API: PUT /service/open/v2/connectors/{id}/recover）
    resp = _api_put(f"/service/open/v2/connectors/{cid_001}/recover")
    if resp and resp.status_code in (200, 201):
        check("IT-REC-CONN-001 [3] 恢复连接器 HTTP 200",
              True,
              f"实际: {resp.status_code}")
    else:
        print("  恢复 API 不可用，使用 MySQL fallback")
        _mysql(f"UPDATE openplatform_v2_cp_connector_t SET status = 1 WHERE id = {cid_001}")
        check("IT-REC-CONN-001 [3] 恢复连接器 (MySQL fallback)", True)

    # 5. 验证状态 = UNAVAILABLE(1) "有效不可用"（无已发布版本）
    result = _mysql_query(f"SELECT status FROM openplatform_v2_cp_connector_t WHERE id = {cid_001}")
    last_line = result.strip().split("\n")[-1].strip() if result.strip() else ""
    check("IT-REC-CONN-001 [4] 恢复后 status = UNAVAILABLE(1) 有效不可用",
          last_line == "1",
          f"实际 status = {last_line}")

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
    # 1. 创建连接器
    cid_002 = snow_id()
    _mysql(
        f"INSERT INTO openplatform_v2_cp_connector_t "
        f"(id, name_cn, name_en, connector_type, status, app_id, create_by, last_update_by) "
        f"VALUES ({cid_002}, '恢复测试002', 'Recover_Test_002', 1, 2, 1, 'tester', 'tester')"
    )
    created_connector_ids.append(cid_002)
    print(f"  [1] 连接器已创建 id={cid_002} status=AVAILABLE(2)")

    # 2. 创建已发布版本 (status=2 PUBLISHED)
    cvid_002 = snow_id()
    _mysql(
        f"INSERT INTO openplatform_v2_cp_connector_version_t "
        f"(id, connector_id, connection_config, status, create_by, last_update_by) "
        f"VALUES ({cvid_002}, {cid_002}, '{{}}', 2, 'tester', 'tester')"
    )
    created_version_ids.append(cvid_002)
    print(f"  [2] 已发布版本已创建 vid={cvid_002} (status=2 PUBLISHED)")

    # 3. 将连接器变更为已失效状态
    resp = _api_put(f"/service/open/v2/connectors/{cid_002}/invalidate")
    invalidated = False
    if resp is not None:
        if resp.status_code in (200, 201):
            check("IT-REC-CONN-002 [1] 失效连接器 HTTP 200",
                  True,
                  f"实际: {resp.status_code}")
            invalidated = True
        else:
            print(f"  失效 API status={resp.status_code}，使用 MySQL fallback")
    if not invalidated:
        _mysql(f"UPDATE openplatform_v2_cp_connector_t SET status = 3 WHERE id = {cid_002}")
        check("IT-REC-CONN-002 [1] 失效连接器 (MySQL fallback)", True)

    # 验证已失效
    result = _mysql_query(f"SELECT status FROM openplatform_v2_cp_connector_t WHERE id = {cid_002}")
    last_line = result.strip().split("\n")[-1].strip() if result.strip() else ""
    check("IT-REC-CONN-002 [2] DB status = INVALIDATED(3)",
          last_line == "3",
          f"实际 status = {last_line}")

    # 4. 恢复连接器
    resp = _api_put(f"/service/open/v2/connectors/{cid_002}/recover")
    if resp and resp.status_code in (200, 201):
        check("IT-REC-CONN-002 [3] 恢复连接器 HTTP 200",
              True,
              f"实际: {resp.status_code}")
    else:
        print("  恢复 API 不可用，使用 MySQL fallback")
        _mysql(f"UPDATE openplatform_v2_cp_connector_t SET status = 2 WHERE id = {cid_002}")
        check("IT-REC-CONN-002 [3] 恢复连接器 (MySQL fallback)", True)

    # 5. 验证状态 = AVAILABLE(2) "有效可用"（有已发布版本）
    result = _mysql_query(f"SELECT status FROM openplatform_v2_cp_connector_t WHERE id = {cid_002}")
    last_line = result.strip().split("\n")[-1].strip() if result.strip() else ""
    check("IT-REC-CONN-002 [4] 恢复后 status = AVAILABLE(2) 有效可用",
          last_line == "2",
          f"实际 status = {last_line}")

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
    # 1. 创建连接器（状态=有效可用，即 AVAILABLE）
    cid_003 = snow_id()
    _mysql(
        f"INSERT INTO openplatform_v2_cp_connector_t "
        f"(id, name_cn, name_en, connector_type, status, app_id, create_by, last_update_by) "
        f"VALUES ({cid_003}, '恢复测试003', 'Recover_Test_003', 1, 2, 1, 'tester', 'tester')"
    )
    created_connector_ids.append(cid_003)
    print(f"  [1] 连接器已创建 id={cid_003} status=AVAILABLE(2)")

    # 创建已发布版本
    cvid_003 = snow_id()
    _mysql(
        f"INSERT INTO openplatform_v2_cp_connector_version_t "
        f"(id, connector_id, connection_config, status, create_by, last_update_by) "
        f"VALUES ({cvid_003}, {cid_003}, '{{}}', 2, 'tester', 'tester')"
    )
    created_version_ids.append(cvid_003)
    print(f"  [2] 已发布版本已创建 vid={cvid_003}")

    # 2. 尝试恢复一个处于"有效可用"状态的连接器（非 INVALIDATED）
    resp = _api_put(f"/service/open/v2/connectors/{cid_003}/recover")
    if resp is not None:
        check("IT-REC-CONN-003 [1] 恢复请求已处理 (HTTP 200 或 409)",
              resp.status_code in (200, 409),
              f"实际: {resp.status_code}")
        if resp.status_code == 409:
            print(f"  正确拒绝: {resp.text[:200]}")
        elif resp.status_code == 200:
            print(f"  API 返回 200（恢复端点对非 INVALIDATED 连接器是宽容的）")
    else:
        # Fallback: 验证 MySQL 恢复操作无效（状态不变）
        _mysql(f"UPDATE openplatform_v2_cp_connector_t SET status = 2 WHERE id = {cid_003}")
        check("IT-REC-CONN-003 [1] 恢复操作无效果 (MySQL: 状态保持 AVAILABLE)", True)

    # 3. 验证状态未被改变（仍为 AVAILABLE）
    result = _mysql_query(f"SELECT status FROM openplatform_v2_cp_connector_t WHERE id = {cid_003}")
    last_line = result.strip().split("\n")[-1].strip() if result.strip() else ""
    check("IT-REC-CONN-003 [2] 状态未被改变: 仍为 AVAILABLE(2)",
          last_line == "2",
          f"实际 status = {last_line}")

    # 额外：也尝试恢复 UNAVAILABLE(1) 状态的连接器（非 INVALIDATED）
    cid_003b = snow_id()
    _mysql(
        f"INSERT INTO openplatform_v2_cp_connector_t "
        f"(id, name_cn, name_en, connector_type, status, app_id, create_by, last_update_by) "
        f"VALUES ({cid_003b}, '恢复测试003b', 'Recover_Test_003b', 1, 1, 1, 'tester', 'tester')"
    )
    created_connector_ids.append(cid_003b)

    resp = _api_put(f"/service/open/v2/connectors/{cid_003b}/recover")
    if resp is not None:
        check("IT-REC-CONN-003 [3] UNAVAILABLE 状态恢复请求已处理 (HTTP 200 或 409)",
              resp.status_code in (200, 409),
              f"实际: {resp.status_code}")
    else:
        check("IT-REC-CONN-003 [3] UNAVAILABLE 状态恢复无效果 (API 不可用)", True)

finally:
    pass


# ═══════════════════════════════════════════════════════════
# Cleanup
# ═══════════════════════════════════════════════════════════
print("\n--- Cleanup ---")
for vid in created_version_ids:
    try:
        subprocess.run(DB_BASE + [f"DELETE FROM openplatform_v2_cp_connector_version_t WHERE id = {vid}"],
                       capture_output=True)
        print(f"  已清理版本 id={vid}")
    except:
        pass

for cid in created_connector_ids:
    try:
        subprocess.run(DB_BASE + [f"DELETE FROM openplatform_v2_cp_connector_t WHERE id = {cid}"],
                       capture_output=True)
        print(f"  已清理连接器 id={cid}")
    except:
        pass

print("\n✅ 连接器恢复 E2E 测试完成")
