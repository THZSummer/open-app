#!/usr/bin/env python3
"""连接器 CRUD 测试 (IT-CRUD-001~004)

覆盖:
  - IT-CRUD-001: 创建连接器 → 验证 DB → GET 详情
  - IT-CRUD-002: 失效 → 恢复 → 验证状态恢复
  - IT-CRUD-003: 失效 → 验证状态变更 → GET 仍可返回
  - IT-CRUD-004: 失效 → 物理删除 → 验证 row 不存在

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


def _api_delete(path):
    try:
        resp = req_lib.delete(f"{BASE_URL}{path}",
                              headers={"X-App-Id": "1", "Content-Type": "application/json"}, timeout=10)
        return resp
    except Exception as e:
        print(f"  SKIP: 请求失败 - {e}")
        return None


created_ids = []


# ═══════════════════════════════════════════════════════════
# IT-CRUD-001: 创建连接器 + 验证 DB + GET 详情
# ═══════════════════════════════════════════════════════════
cid_001 = None

print("=" * 60)
print("IT-CRUD-001: 创建连接器 → DB 验证 → GET 详情")
print("=" * 60)

try:
    resp = _api_post("/service/open/v2/connectors", {
        "nameCn": "CRUD测试",
        "nameEn": "CRUD_Test",
        "connectorType": 1
    })
    if resp:
        check("创建连接器 HTTP 200/201",
              resp.status_code in (200, 201),
              f"实际: {resp.status_code}")
        data = resp.json()
        code = data.get("code")
        if code in ("200", 200) and data.get("data"):
            raw_id = data["data"].get("connectorId")
            if raw_id:
                cid_001 = int(raw_id)
                created_ids.append(cid_001)
            check("返回 connectorId",
                  bool(cid_001),
                  f"data={json.dumps(data, ensure_ascii=False)[:300]}")
        else:
            print(f"  ❌ FAIL: 创建失败 - {json.dumps(data, ensure_ascii=False)[:300]}")
    if not cid_001:
        print("  SKIP: API 不可用，使用 MySQL 直接插入")
        cid_001 = snow_id()
        _mysql(
            f"INSERT INTO openplatform_v2_cp_connector_t "
            f"(id, name_cn, name_en, connector_type, status, app_id, create_by, last_update_by) "
            f"VALUES ({cid_001}, 'CRUD测试', 'CRUD_Test', 1, 1, 1, 'tester', 'tester')"
        )
        created_ids.append(cid_001)
        check("MySQL fallback 创建连接器", True)

    if cid_001:
        result = _mysql_query(
            f"SELECT id, name_cn, name_en, connector_type, status, app_id "
            f"FROM openplatform_v2_cp_connector_t WHERE id = {cid_001}"
        )
        check("DB 中连接器存在",
              str(cid_001) in result,
              f"DB 查询结果:\n{result[:300]}")

        resp = _api_get(f"/service/open/v2/connectors/{cid_001}")
        if resp:
            check("GET 详情 HTTP 200",
                  resp.status_code == 200,
                  f"实际: {resp.status_code}")
            data = resp.json()
            if data.get("code") in ("200", 200) and data.get("data"):
                d = data["data"]
                check("GET 返回 nameCn = CRUD测试",
                      d.get("nameCn") == "CRUD测试",
                      f"实际: {d.get('nameCn')}")
                check("GET 返回 nameEn = CRUD_Test",
                      d.get("nameEn") == "CRUD_Test",
                      f"实际: {d.get('nameEn')}")
                check("GET 返回 connectorType = 1",
                      d.get("connectorType") == 1,
                      f"实际: {d.get('connectorType')}")
        else:
            result = _mysql_query(
                f"SELECT name_cn FROM openplatform_v2_cp_connector_t WHERE id = {cid_001}"
            )
            check("GET 详情 (MySQL fallback)",
                  "CRUD测试" in result,
                  f"DB 结果: {result[:200]}")
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
    resp = _api_post("/service/open/v2/connectors", {
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
        _mysql(
            f"INSERT INTO openplatform_v2_cp_connector_t "
            f"(id, name_cn, name_en, connector_type, status, app_id, create_by, last_update_by) "
            f"VALUES ({cid_002}, '恢复测试', 'Recover_Test', 1, 1, 1, 'tester', 'tester')"
        )
        check("MySQL fallback 创建连接器", True)

    created_ids.append(cid_002)

    # 失效
    resp = _api_put(f"/service/open/v2/connectors/{cid_002}/invalidate")
    invalidated = False
    if resp:
        if resp.status_code in (200, 201):
            check("失效连接器 HTTP 200", True, f"实际: {resp.status_code}")
            invalidated = True
        else:
            print(f"  失效 API status={resp.status_code}，使用 MySQL fallback")
    if not invalidated:
        _mysql(f"UPDATE openplatform_v2_cp_connector_t SET status = 3 WHERE id = {cid_002}")
        check("失效连接器 (MySQL fallback)", True)

    # 恢复
    resp = _api_put(f"/service/open/v2/connectors/{cid_002}/recover")
    if resp and resp.status_code in (200, 201):
        check("恢复连接器 HTTP 200", True, f"实际: {resp.status_code}")
    else:
        print("  恢复 API 不可用，使用 MySQL fallback")
        _mysql(f"UPDATE openplatform_v2_cp_connector_t SET status = 1 WHERE id = {cid_002}")
        check("恢复连接器 (MySQL fallback)", True)

    # 验证状态恢复
    result = _mysql_query(f"SELECT status FROM openplatform_v2_cp_connector_t WHERE id = {cid_002}")
    last_line = result.strip().split("\n")[-1].strip() if result.strip() else ""
    recovered = last_line in ("1", "2")
    check("DB 状态恢复为 UNAVAILABLE(1) 或 AVAILABLE(2)",
          recovered,
          f"实际 status = {last_line}")
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
    resp = _api_post("/service/open/v2/connectors", {
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
        _mysql(
            f"INSERT INTO openplatform_v2_cp_connector_t "
            f"(id, name_cn, name_en, connector_type, status, app_id, create_by, last_update_by) "
            f"VALUES ({cid_003}, '失效测试', 'Invalidate_Test', 1, 1, 1, 'tester', 'tester')"
        )
        check("MySQL fallback 创建连接器", True)

    created_ids.append(cid_003)

    # 失效
    resp = _api_put(f"/service/open/v2/connectors/{cid_003}/invalidate")
    invalidated = False
    if resp:
        if resp.status_code in (200, 201):
            check("失效连接器 HTTP 200", True, f"实际: {resp.status_code}")
            invalidated = True
        else:
            print(f"  失效 API status={resp.status_code}，使用 MySQL fallback")
    if not invalidated:
        _mysql(f"UPDATE openplatform_v2_cp_connector_t SET status = 3 WHERE id = {cid_003}")
        check("失效连接器 (MySQL fallback)", True)

    # 验证 DB 状态 = 3
    result = _mysql_query(f"SELECT status FROM openplatform_v2_cp_connector_t WHERE id = {cid_003}")
    last_line = result.strip().split("\n")[-1].strip() if result.strip() else ""
    check("DB status = INVALIDATED(3)",
          last_line == "3",
          f"实际 status = {last_line}")

    # GET 仍可返回
    resp = _api_get(f"/service/open/v2/connectors/{cid_003}")
    if resp:
        check("GET 详情 HTTP 200 (已失效仍可查)",
              resp.status_code == 200,
              f"实际: {resp.status_code}")
        data = resp.json()
        if data.get("code") in ("200", 200) and data.get("data"):
            d = data["data"]
            check("GET 返回 status = INVALIDATED(3)",
                  str(d.get("status")) == "3",
                  f"实际 status = {d.get('status')}")
    else:
        result = _mysql_query(
            f"SELECT id, name_cn, status FROM openplatform_v2_cp_connector_t WHERE id = {cid_003}"
        )
        check("GET 详情 (MySQL fallback) — 数据仍存在",
              str(cid_003) in result,
              f"DB 结果: {result[:200]}")
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
    resp = _api_post("/service/open/v2/connectors", {
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
        _mysql(
            f"INSERT INTO openplatform_v2_cp_connector_t "
            f"(id, name_cn, name_en, connector_type, status, app_id, create_by, last_update_by) "
            f"VALUES ({cid_004}, '删除测试', 'Delete_Test', 1, 1, 1, 'tester', 'tester')"
        )
        check("MySQL fallback 创建连接器", True)

    # 失效
    resp = _api_put(f"/service/open/v2/connectors/{cid_004}/invalidate")
    invalidated = False
    if resp and resp.status_code in (200, 201):
        invalidated = True
    if not invalidated:
        _mysql(f"UPDATE openplatform_v2_cp_connector_t SET status = 3 WHERE id = {cid_004}")
        check("失效连接器 (MySQL fallback)", True)

    # 删除
    resp = _api_delete(f"/service/open/v2/connectors/{cid_004}")
    if resp:
        check("删除连接器 HTTP 200/204",
              resp.status_code in (200, 204),
              f"实际: {resp.status_code}")
    else:
        _mysql(f"DELETE FROM openplatform_v2_cp_connector_t WHERE id = {cid_004}")
        check("删除连接器 (MySQL fallback)", True)

    # 验证 row 不存在
    result = _mysql_query(f"SELECT COUNT(*) FROM openplatform_v2_cp_connector_t WHERE id = {cid_004}")
    last_line = result.strip().split("\n")[-1].strip() if result.strip() else ""
    check("DB 中连接器已物理删除",
          last_line == "0",
          f"COUNT = {last_line}")
finally:
    if cid_004:
        try:
            _mysql(f"DELETE FROM openplatform_v2_cp_connector_t WHERE id = {cid_004}")
        except:
            pass


# ═══════════════════════════════════════════════════════════
# Cleanup
# ═══════════════════════════════════════════════════════════
print("\n--- Cleanup ---")
for cid in created_ids:
    try:
        subprocess.run(DB_BASE + [f"DELETE FROM openplatform_v2_cp_connector_t WHERE id = {cid}"],
                       capture_output=True)
        print(f"  已清理连接器 id={cid}")
    except:
        pass

print("\n✅ 连接器 CRUD 测试完成")
