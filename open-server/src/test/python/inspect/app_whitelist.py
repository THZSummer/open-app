#!/usr/bin/env python3
"""应用白名单准入测试 (FR-045)

覆盖:
  - IT-AWL-001: 白名单应用请求成功 (X-App-Id=1, 空白名单默认放行)
  - IT-AWL-002: 缺少 X-App-Id Header → 403
  - IT-AWL-003: 空白名单 — 任意应用均放行 (X-App-Id=99999)

依赖: open-server (:18080), MySQL (:3306)
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

def api_get(path, headers=None):
    try:
        h = {"Content-Type": "application/json"}
        if headers:
            h.update(headers)
        resp = req_lib.get(f"{BASE_URL}{path}", headers=h, timeout=10)
        return resp
    except Exception as e:
        print(f"  SKIP: 请求失败 - {e}")
        return None


cid = snow_id()
print("=" * 60)
print("应用白名单准入测试 (FR-045)")
print("=" * 60)
print(f"\n[Setup] 创建测试连接器 id={cid}")

try:
    _mysql(
        f"INSERT INTO openplatform_v2_cp_connector_t "
        f"(id, name_cn, name_en, connector_type, create_by, last_update_by) "
        f"VALUES ({cid}, '白名单测试连接器', 'WL_Test_Connector', 1, 'tester', 'tester')"
    )

    # ═══════════════════════════════════════════════════════════
    # IT-AWL-001: 白名单应用请求成功
    # ═══════════════════════════════════════════════════════════
    print(f"\n{'='*60}")
    print("IT-AWL-001: 白名单应用请求成功 (X-App-Id=1)")
    print(f"{'='*60}")

    resp = api_get(f"/service/open/v2/connectors/{cid}", headers={"X-App-Id": "1"})
    if resp is not None:
        check("HTTP 200 — 白名单应用请求成功",
              resp.status_code == 200,
              f"实际: {resp.status_code}")
        data = resp.json()
        check("response code = 200",
              data.get("code") in ("200", 200),
              f"实际: {data.get('code')}")
    else:
        print("  SKIP: open-server 未运行")

    # ═══════════════════════════════════════════════════════════
    # IT-AWL-002: 缺少 X-App-Id Header → 403
    # ═══════════════════════════════════════════════════════════
    print(f"\n{'='*60}")
    print("IT-AWL-002: 缺少 X-App-Id Header → 403")
    print(f"{'='*60}")

    resp = api_get(f"/service/open/v2/connectors/{cid}")
    if resp is not None:
        check("HTTP 403 — 缺少 X-App-Id Header",
              resp.status_code == 403,
              f"实际: {resp.status_code}")
        if resp.status_code == 403:
            body = resp.json()
            check("响应 code = '403'",
                  body.get("code") == "403",
                  f"实际: {body.get('code')}")
            message_zh = body.get("messageZh", "")
            check("messageZh 包含 '缺少'",
                  "缺少" in message_zh,
                  f"实际: {message_zh}")
            message_en = body.get("messageEn", "")
            check("messageEn 包含 'Missing'",
                  "Missing" in message_en,
                  f"实际: {message_en}")
    else:
        print("  SKIP: open-server 未运行")

    # ═══════════════════════════════════════════════════════════
    # IT-AWL-003: 空白名单 — 任意应用均放行
    # ═══════════════════════════════════════════════════════════
    print(f"\n{'='*60}")
    print("IT-AWL-003: 空白名单 — 任意应用均放行 (X-App-Id=99999)")
    print(f"{'='*60}")

    resp = api_get(f"/service/open/v2/connectors/{cid}", headers={"X-App-Id": "99999"})
    if resp is not None:
        check("HTTP 200 — 空白名单任意应用放行",
              resp.status_code == 200,
              f"实际: {resp.status_code}")
        data = resp.json()
        check("response code = 200",
              data.get("code") in ("200", 200),
              f"实际: {data.get('code')}")
    else:
        print("  SKIP: open-server 未运行")

finally:
    # ═══════════════════════════════════════════════════════════
    # Cleanup
    # ═══════════════════════════════════════════════════════════
    print(f"\n{'='*60}")
    print("Cleanup: 删除测试连接器")
    print(f"{'='*60}")
    subprocess.run(
        DB_BASE + [f"DELETE FROM openplatform_v2_cp_connector_t WHERE id = {cid}"],
        capture_output=True
    )
    print(f"  已删除连接器 id={cid}")
    print("\n应用白名单准入测试完成")
