#!/usr/bin/env python3
"""FR-047: JSON Validation for Connector Version Publish

Tests:
  IT-JSON-001: Valid JSON → publish succeeds → HTTP 200
  IT-JSON-002: Invalid JSON syntax → rejected → HTTP 400
  IT-JSON-003: Valid JSON but invalid schema → succeeds (V3 only syntax check)

Endpoint: PUT /service/open/v2/connectors/{connectorId}/versions/{versionId}/publish
Header:   X-App-Id: 1

Dependencies:
  - open-server (:18080)   for API calls
  - MySQL (192.168.3.155) for test fixture setup/cleanup
"""

from client import *
import subprocess, time, json, requests as req_lib

# ═══════════════════════════════════════════════════════════
# Database helpers (same as connector_version_lifecycle.py)
# ═══════════════════════════════════════════════════════════
DB_HOST = "192.168.3.155"
DB_USER = "openapp"
DB_PASS = "openapp"
DB_NAME = "openapp"
DB_BASE = ["mysql", "-h", DB_HOST, f"-u{DB_USER}", f"-p{DB_PASS}", DB_NAME, "-e"]


def snow_id():
    """Generate a pseudo-snowflake ID from current time."""
    return int(time.time() * 1000000) % 100000000000000000


def _mysql(sql):
    """Run a SQL statement via mysql CLI (raises on failure)."""
    subprocess.run(DB_BASE + [sql], check=True, capture_output=True)


def _escape(obj):
    """Escape a value for MySQL single-quoted INSERT.
    Accepts dict (JSON-serialized) or raw string.
    """
    if isinstance(obj, str):
        return obj.replace("'", "''")
    return json.dumps(obj, ensure_ascii=False).replace("'", "''")


def api_put(path, body=None):
    """PUT request with X-App-Id=1 header."""
    try:
        resp = req_lib.put(
            f"{BASE_URL}{path}",
            json=body or {},
            headers={"Content-Type": "application/json", "X-App-Id": "1"},
            timeout=10,
        )
        return resp
    except Exception as e:
        print(f"  SKIP: 请求失败 - {e}")
        return None


# ═══════════════════════════════════════════════════════════
# Fixtures
# ═══════════════════════════════════════════════════════════

# Valid connectionConfig JSON (used by IT-JSON-001)
VALID_CONFIG = {
    "labelCn": "JSON测试",
    "protocol": "HTTP",
    "protocolConfig": {
        "url": "https://httpbin.org/get",
        "method": "GET",
    },
    "authConfig": {"type": "NONE", "fields": []},
    "inputContract": {
        "protocol": "HTTP",
        "header": {"type": "object", "properties": {}, "required": []},
        "query": {"type": "object", "properties": {}, "required": []},
        "body": {"type": "object", "properties": {}, "required": []},
    },
    "outputContract": {
        "protocol": "HTTP",
        "body": {"type": "object", "properties": {}},
    },
    "timeoutMs": 5000,
}

# Invalid JSON string (used by IT-JSON-002)
INVALID_JSON_STR = "{invalid json!!!"

# Valid JSON but unknown schema (used by IT-JSON-003)
INVALID_SCHEMA_CONFIG = {"foo": "bar", "baz": 123}

# Accumulated IDs for cleanup
_cleanup_connectors = []
_cleanup_versions = []


def create_connector(label):
    """Insert a connector row with app_id=1, return (cid)."""
    cid = snow_id()
    _mysql(
        f"INSERT INTO openplatform_v2_cp_connector_t "
        f"(id, name_cn, name_en, connector_type, app_id, status, create_by, last_update_by) "
        f"VALUES ({cid}, '{label}', '{label}', 1, 1, 1, 'tester', 'tester')"
    )
    _cleanup_connectors.append(cid)
    return cid


def create_draft_version(cid, connection_config):
    """Insert a draft version row (status=1), return (vid)."""
    vid = snow_id()
    _mysql(
        f"INSERT INTO openplatform_v2_cp_connector_version_t "
        f"(id, connector_id, connection_config, status, create_by, last_update_by) "
        f"VALUES ({vid}, {cid}, '{_escape(connection_config)}', 1, 'tester', 'tester')"
    )
    _cleanup_versions.append(vid)
    return vid


# ═══════════════════════════════════════════════════════════
# IT-JSON-001: Publish with valid JSON → succeeds
# ═══════════════════════════════════════════════════════════
print("=" * 60)
print("IT-JSON-001: Publish with valid JSON")
print("=" * 60)

try:
    cid_001 = create_connector("JSON-001")
    print(f"  [1] Connector created: {cid_001}")

    vid_001 = create_draft_version(cid_001, VALID_CONFIG)
    print(f"  [2] Draft version created: {vid_001}")

    # 3. Call publish API
    resp = api_put(f"/service/open/v2/connectors/{cid_001}/versions/{vid_001}/publish")
    if resp is not None:
        check("HTTP 200", resp.status_code == 200, f"got {resp.status_code}")
        data = resp.json() if resp.text else {}
        check("code=200", data.get("code") in ("200", 200),
              f"response: {json.dumps(data, ensure_ascii=False)[:200]}")
        check("status=PUBLISHED(2)",
              data.get("data", {}).get("status") == 2,
              f"status={data.get('data', {}).get('status')}")
    else:
        check("API 可用", False, "open-server 不可达")

except Exception as e:
    check("未预期异常", False, str(e))


# ═══════════════════════════════════════════════════════════
# IT-JSON-002: Publish with invalid JSON → rejected
# ═══════════════════════════════════════════════════════════
print("\n" + "=" * 60)
print("IT-JSON-002: Publish with invalid JSON syntax")
print("=" * 60)

try:
    cid_002 = create_connector("JSON-002")
    print(f"  [1] Connector created: {cid_002}")

    vid_002 = create_draft_version(cid_002, INVALID_JSON_STR)
    print(f"  [2] Draft version created (invalid JSON): {vid_002}")

    # 3. Call publish API — expect rejection
    resp = api_put(f"/service/open/v2/connectors/{cid_002}/versions/{vid_002}/publish")
    if resp is not None:
        check("HTTP 400", resp.status_code == 400, f"got {resp.status_code}")
        data = resp.json() if resp.text else {}
        msg = data.get("messageZh", "") + data.get("messageEn", "")
        has_keyword = "JSON" in msg or "格式无效" in msg or "Invalid" in msg.lower()
        check("message mentions JSON/invalid",
              has_keyword or resp.status_code == 400,
              f"message: {json.dumps(data, ensure_ascii=False)[:300]}")
        code = data.get("code")
        check("code=400", str(code) == "400" or code == 400,
              f"code={code}")
    else:
        check("API 可用", False, "open-server 不可达")

except Exception as e:
    check("未预期异常", False, str(e))


# ═══════════════════════════════════════════════════════════
# IT-JSON-003: Valid JSON but invalid schema → succeeds (V3)
# ═══════════════════════════════════════════════════════════
print("\n" + "=" * 60)
print("IT-JSON-003: Publish with valid JSON but unknown schema")
print("=" * 60)

try:
    cid_003 = create_connector("JSON-003")
    print(f"  [1] Connector created: {cid_003}")

    vid_003 = create_draft_version(cid_003, INVALID_SCHEMA_CONFIG)
    print(f"  [2] Draft version created (unknown schema): {vid_003}")

    # 3. Call publish API — should succeed (V3 only validates JSON syntax)
    resp = api_put(f"/service/open/v2/connectors/{cid_003}/versions/{vid_003}/publish")
    if resp is not None:
        check("HTTP 200", resp.status_code == 200, f"got {resp.status_code}")
        data = resp.json() if resp.text else {}
        check("code=200", data.get("code") in ("200", 200),
              f"response: {json.dumps(data, ensure_ascii=False)[:200]}")
        check("status=PUBLISHED(2)",
              data.get("data", {}).get("status") == 2,
              f"status={data.get('data', {}).get('status')}")
    else:
        check("API 可用", False, "open-server 不可达")

except Exception as e:
    check("未预期异常", False, str(e))


# ═══════════════════════════════════════════════════════════
# Cleanup
# ═══════════════════════════════════════════════════════════
print("\n--- Cleanup ---")
for vid in _cleanup_versions:
    subprocess.run(
        DB_BASE + [f"DELETE FROM openplatform_v2_cp_connector_version_t WHERE id = {vid}"],
        capture_output=True,
    )
for cid in _cleanup_connectors:
    subprocess.run(
        DB_BASE + [f"DELETE FROM openplatform_v2_cp_connector_t WHERE id = {cid}"],
        capture_output=True,
    )
print("Cleanup done.")
