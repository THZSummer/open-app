#!/usr/bin/env python3
"""L4 契约测试 (IT-052~059)"""
from client import *
import re


def check(name, condition, detail=""):
    """安全断言，不抛出异常"""
    if condition:
        print(f"  PASS: {name}" + (f" - {detail}" if detail else ""))
    else:
        print(f"  FAIL: {name}" + (f" - {detail}" if detail else ""))


# -- IT-052: 成功响应格式 --
print("\n=== IT-052: 成功响应格式 ===")
resp = request("GET", "/connectors")
if resp:
    body = resp.json()
    check("成功响应格式", "code" in body and "data" in body and "page" in body,
          "包含 code/data/page 字段")
    check("code为string", isinstance(body.get("code"), str))

# -- IT-053: 错误响应格式 --
print("\n=== IT-053: 错误响应格式 ===")
resp = request("GET", "/connectors/999999999999999999")
if resp:
    body = resp.json()
    check("错误code不为200", body.get("code") != "200",
          f"实际code={body.get('code')}")

# -- IT-054: 分页响应格式 --
print("\n=== IT-054: 分页响应格式 ===")
resp = request("GET", "/connectors")
if resp:
    body = resp.json()
    page = body.get("page")
    if page:
        check("分页字段完整", "curPage" in page and "pageSize" in page and "total" in page)
        check("curPage为数字", isinstance(page.get("curPage"), int))
        check("pageSize为数字", isinstance(page.get("pageSize"), int))

# -- IT-055: BIGINT ID 为 string --
print("\n=== IT-055: BIGINT ID 为 string ===")
resp = request("GET", "/connectors")
if resp:
    body = resp.json()
    if body["data"] and len(body["data"]) > 0:
        item = body["data"][0]
        for key in item:
            if key == "id" or key.endswith("Id"):
                check(f"字段{key}为string", isinstance(item[key], str),
                      f"实际类型={type(item[key]).__name__}")

# -- IT-056: 枚举为 TINYINT --
print("\n=== IT-056: 枚举为 TINYINT ===")
resp = request("GET", "/connectors")
if resp:
    body = resp.json()
    if body["data"] and len(body["data"]) > 0:
        for field in ["connectorType", "lifecycleStatus"]:
            if field in body["data"][0]:
                check(f"字段{field}为int", isinstance(body["data"][0][field], int),
                      f"实际类型={type(body['data'][0][field]).__name__}")

# -- IT-057: 时间 ISO 8601 --
print("\n=== IT-057: 时间 ISO 8601 ===")
resp = request("GET", "/connectors")
if resp:
    body = resp.json()
    if body["data"] and len(body["data"]) > 0:
        iso = r"^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}"
        for field in ["createTime", "lastUpdateTime"]:
            if field in body["data"][0] and body["data"][0][field]:
                val = str(body["data"][0][field])
                check(f"字段{field}为ISO 8601", bool(re.match(iso, val)),
                      f"值={val[:25]}")

# -- IT-058: camelCase 字段 --
print("\n=== IT-058: camelCase 字段 ===")
resp = request("GET", "/connectors")
if resp:
    body = resp.json()
    if body["data"] and len(body["data"]) > 0:
        camel = re.compile(r"^[a-z]+[A-Za-z0-9]*$")
        all_ok = all(camel.match(key) for key in body["data"][0])
        check("所有字段为camelCase", all_ok)

# -- IT-059: 错误码覆盖率 --
print("\n=== IT-059: 错误码覆盖率 ===")
resp = request("GET", "/connectors/999999999999999999")
if resp:
    body = resp.json()
    expected = ["400", "401", "403", "404", "409", "422", "429", "500"]
    check("错误码在预期范围内", body.get("code") in expected,
          f"实际code={body.get('code')}")
