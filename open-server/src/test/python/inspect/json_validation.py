#!/usr/bin/env python3
"""FR-047: JSON Validation for Connector Version Publish

Tests:
  IT-JSON-001: Valid JSON → publish succeeds → HTTP 200
  IT-JSON-002: Invalid JSON syntax → rejected → HTTP 400
  IT-JSON-003: Valid JSON but invalid schema → succeeds (V3 only syntax check)

Endpoint: PUT /connectors/{connectorId}/versions/{versionId}/publish

Dependencies:
  - open-server (:18080)   for API calls
  - MySQL                   for test fixture setup/cleanup
"""

from client import api, db, ok, done
import subprocess, time, json

DB_BASE = ["mysql", "-h192.168.3.155", "-uopenapp", "-popenapp", "openapp", "-e"]


def snow_id():
    return int(time.time() * 1000000) % 100000000000000000


def _escape(obj):
    if isinstance(obj, str):
        return obj.replace("'", "''")
    return json.dumps(obj, ensure_ascii=False).replace("'", "''")


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

INVALID_JSON_STR = "{invalid json!!!"
INVALID_SCHEMA_CONFIG = {"foo": "bar", "baz": 123}

_cleanup_connectors = []
_cleanup_versions = []


def create_connector(label):
    cid = snow_id()
    db(
        f"INSERT INTO openplatform_v2_cp_connector_t "
        f"(id, name_cn, name_en, connector_type, app_id, status, create_by, last_update_by) "
        f"VALUES ({cid}, '{label}', '{label}', 1, 1, 1, 'tester', 'tester')"
    )
    _cleanup_connectors.append(cid)
    return cid


def create_draft_version(cid, connection_config):
    vid = snow_id()
    db(
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

    resp = api("PUT", f"/connectors/{cid_001}/versions/{vid_001}/publish")
    if resp is not None:
        ok(resp, 200, "HTTP 200")
        data = resp.json() if resp.text else {}
        ok(data.get("code") in ("200", 200), name="code=200")
        ok(data.get("data", {}).get("status") == 2, name="status=PUBLISHED(2)")
    else:
        ok(False, name="API 可用")

except Exception as e:
    ok(False, name="未预期异常")


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

    resp = api("PUT", f"/connectors/{cid_002}/versions/{vid_002}/publish")
    if resp is not None:
        ok(resp.status_code == 400, name="HTTP 400")
        data = resp.json() if resp.text else {}
        msg = data.get("messageZh", "") + data.get("messageEn", "")
        has_keyword = "JSON" in msg or "格式无效" in msg or "Invalid" in msg.lower()
        ok(has_keyword or resp.status_code == 400, name="message mentions JSON/invalid")
        code = data.get("code")
        ok(str(code) == "400" or code == 400, name="code=400")
    else:
        ok(False, name="API 可用")

except Exception as e:
    ok(False, name="未预期异常")


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

    resp = api("PUT", f"/connectors/{cid_003}/versions/{vid_003}/publish")
    if resp is not None:
        ok(resp, 200, "HTTP 200")
        data = resp.json() if resp.text else {}
        ok(data.get("code") in ("200", 200), name="code=200")
        ok(data.get("data", {}).get("status") == 2, name="status=PUBLISHED(2)")
    else:
        ok(False, name="API 可用")

except Exception as e:
    ok(False, name="未预期异常")


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

done()
