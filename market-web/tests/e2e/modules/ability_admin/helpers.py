import struct
import zlib
import requests
from pathlib import Path

API_BASE = "http://localhost:18083/market-server/service/open/v2"


def make_test_png(width=40, height=40):
    raw = b''
    for _ in range(height):
        raw += b'\x00'
        for _ in range(width):
            raw += struct.pack('BBB', 255, 0, 0)

    def chunk(ctype, data):
        c = ctype + data
        return struct.pack('>I', len(data)) + c + struct.pack('>I', zlib.crc32(c) & 0xffffffff)

    ihdr = struct.pack('>IIBBBBB', width, height, 8, 2, 0, 0, 0)
    return b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', ihdr) + chunk(b'IDAT', zlib.compress(raw)) + chunk(b'IEND', b'')


def upload_icon(png_bytes=None):
    if png_bytes is None:
        png_bytes = make_test_png(40, 40)
    r = requests.post(
        f"{API_BASE}/file/upload",
        files={"file": ("icon.png", png_bytes, "image/png")},
        data={"bizType": "1"},
        timeout=10,
    )
    data = r.json()
    assert data.get("code") == "200", f"图标上传失败: {data}"
    return data["data"]["batchId"], data["data"]["showUrl"]


def create_ability(ability_type, **overrides):
    payload = {
        "nameCn": "E2E测试能力",
        "nameEn": "e2e-test-ability",
        "descCn": "这是E2E测试能力的描述文字",
        "descEn": "This is an E2E test ability description",
        "iconBatchId": overrides.pop("iconBatchId", None),
        "orderNum": 1,
        "entryUrl": "https://example.com",
        "routePath": "/e2e-test",
        "aliasName": "",
        "loadType": 1,
        "abilityType": ability_type,
    }
    payload.update(overrides)
    r = requests.post(
        f"{API_BASE}/ability/admin",
        json=payload,
        headers={"Content-Type": "application/json"},
        timeout=10,
    )
    return r.json()


def update_ability(ability_id, **fields):
    r = requests.put(
        f"{API_BASE}/ability/admin/{ability_id}",
        json=fields,
        headers={"Content-Type": "application/json"},
        timeout=10,
    )
    return r.json()


def delete_ability(ability_id):
    r = requests.delete(
        f"{API_BASE}/ability/admin/{ability_id}",
        timeout=10,
    )
    return r.json()


def list_abilities(params=None):
    if params is None:
        params = {"pageSize": 50}
    r = requests.get(f"{API_BASE}/ability/admin/list", params=params, timeout=10)
    return r.json()


def wait_for_table_ready(page):
    page.wait_for_load_state("networkidle")
    rows = page.locator(".ant-table-row")
    try:
        rows.first.wait_for(timeout=10000)
    except Exception:
        page.wait_for_selector(".ant-empty", timeout=5000)


def get_rows(page):
    return page.locator(".ant-table-row")
