#!/usr/bin/env python3
"""应用白名单准入测试 (FR-045)

覆盖:
  - IT-AWL-001: 白名单应用请求成功 (X-App-Id=1, 空白名单默认放行)
  - IT-AWL-002: 缺少 X-App-Id Header → 403
  - IT-AWL-003: 空白名单 — 任意应用均放行 (X-App-Id=99999)

依赖: open-server (:18080)
"""
from client import api, db, ok, done
import time

def snow_id():
    return int(time.time() * 1000000) % 100000000000000000


cid = snow_id()
print("=" * 60)
print("应用白名单准入测试 (FR-045)")
print("=" * 60)
print(f"\n[Setup] 创建测试连接器 id={cid}")

try:
    db(
        f"INSERT INTO openplatform_v2_cp_connector_t "
        f"(id, name_cn, name_en, connector_type, app_id, create_by, last_update_by) "
        f"VALUES ({cid}, '白名单测试连接器', 'WL_Test_Connector', 1, 1, 'tester', 'tester')"
    )

    # ═══════════════════════════════════════════════════════════
    # IT-AWL-001: 白名单应用请求成功
    # ═══════════════════════════════════════════════════════════
    print(f"\n{'='*60}")
    print("IT-AWL-001: 白名单应用请求成功 (X-App-Id=1)")
    print(f"{'='*60}")

    resp = api("GET", f"/connectors/{cid}")
    if resp is not None:
        ok(resp, 200, "HTTP 200 — 白名单应用请求成功")
        data = resp.json()
        ok(data.get("code") in ("200", 200), name="response code = 200")
    else:
        print("  SKIP: open-server 未运行")

    # ═══════════════════════════════════════════════════════════
    # IT-AWL-002: 缺少 X-App-Id Header → 403
    # ═══════════════════════════════════════════════════════════
    print(f"\n{'='*60}")
    print("IT-AWL-002: 缺少 X-App-Id Header → 403")
    print(f"{'='*60}")

    resp = api("GET", f"/connectors/{cid}", app_id=None)
    if resp is not None:
        ok(resp, 403, "HTTP 403 — 缺少 X-App-Id Header")
        if resp.status_code == 403:
            body = resp.json()
            ok(body.get("code") == "403", name="响应 code = '403'")
            message_zh = body.get("messageZh", "")
            ok("缺少" in message_zh, name="messageZh 包含 '缺少'")
            message_en = body.get("messageEn", "")
            ok("Missing" in message_en, name="messageEn 包含 'Missing'")
    else:
        print("  SKIP: open-server 未运行")

    # ═══════════════════════════════════════════════════════════
    # IT-AWL-003: 空白名单 — 任意应用均放行
    # ═══════════════════════════════════════════════════════════
    print(f"\n{'='*60}")
    print("IT-AWL-003: 空白名单 — 任意应用均放行 (X-App-Id=99999)")
    print(f"{'='*60}")

    resp = api("GET", f"/connectors/{cid}", app_id="99999")
    if resp is not None:
        ok(resp, 200, "HTTP 200 — 空白名单任意应用放行")
        data = resp.json()
        ok(data.get("code") in ("200", 200), name="response code = 200")
    else:
        print("  SKIP: open-server 未运行")

finally:
    print(f"\n{'='*60}")
    print("Cleanup: 删除测试连接器")
    print(f"{'='*60}")
    db(f"DELETE FROM openplatform_v2_cp_connector_t WHERE id = {cid}")
    print(f"  已删除连接器 id={cid}")
    print("\n应用白名单准入测试完成")

done()
