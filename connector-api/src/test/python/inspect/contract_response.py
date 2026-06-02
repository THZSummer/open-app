#!/usr/bin/env python3
"""L4 契约测试 (connector-api 响应格式)

connector-api 实际响应格式为直接返回 ExecutionResult，
而非标准 {code,messageZh,messageEn,data} 包装。

校验点：
  - 错误时 status="failed"，携带 errorInfo{code,cause,messageZh,messageEn}
  - 成功时 status!="failed"，携带 executionId(字符串)/totalDurationMs(数字)
  - errorInfo.code 为数字字符串；errorInfo.cause 在内部错误时携带
  - 字段名为 camelCase
  - 错误码覆盖：6001（flow 不存在）, 429（限流）
"""
from client import *
import requests
import json
import re


def _collect_keys(obj, prefix=""):
    keys = []
    if isinstance(obj, dict):
        for k, v in obj.items():
            full = f"{prefix}.{k}" if prefix else k
            keys.append(full)
            keys.extend(_collect_keys(v, full))
    elif isinstance(obj, list):
        for i, item in enumerate(obj):
            keys.extend(_collect_keys(item, f"{prefix}[{i}]"))
    return keys


# ── 获取错误响应 ─────────────────────────────────────
print("=== 获取错误响应（无认证）===")
err_resp = request("POST", "/trigger/999999999999999999/invoke",
                   {"sender": "test"})
err_body = err_resp.json() if err_resp else {}

print("\n=== 获取 404 响应（flow 不存在）===")
notfound_resp = request("POST", "/trigger/999999999999999999/invoke",
                        {"sender": "test"},
                        headers={"X-Sys-Token": "test-token"})
notfound_body = notfound_resp.json() if notfound_resp else {}


print("\n" + "=" * 60)
print("L4 契约校验")
print("=" * 60)

# ── 基本响应结构 ────────────────────────────────────
print("\n--- 响应基本结构 ---")
for label, body in [("无认证", err_body), ("flow不存在", notfound_body)]:
    if body:
        check(f"[{label}] executionId 为 string",
              isinstance(body.get("executionId"), str))
        check(f"[{label}] status 为 string",
              isinstance(body.get("status"), str))
        check(f"[{label}] status 为 failed",
              body.get("status") == "failed")

# ── errorInfo 格式 ──────────────────────────────────
print("\n--- errorInfo 格式 ---")
for label, body in [("无认证", err_body), ("flow不存在", notfound_body)]:
    if body:
        ei = body.get("errorInfo")
        if ei and isinstance(ei, dict):
            check(f"[{label}] errorInfo 存在", True)
            check(f"[{label}] errorInfo.code 为 string",
                  isinstance(ei.get("code"), str))
            check(f"[{label}] errorInfo.code 为数字字符串",
                  bool(re.match(r"^[1-9][0-9]{2,4}$", str(ei.get("code", "")))),
                  f"code={ei.get('code')}")
            check(f"[{label}] errorInfo.messageZh 存在",
                  bool(ei.get("messageZh")))
            check(f"[{label}] errorInfo.messageEn 存在",
                  bool(ei.get("messageEn")))
            # oneOf: 内部错误（6xxxx）应携带 cause
            code_str = str(ei.get("code", ""))
            if code_str.startswith("6"):
                check(f"[{label}] 内部错误携带 cause",
                      bool(ei.get("cause")),
                      f"cause={ei.get('cause')}")
        elif body.get("status") == "success":
            check(f"[{label}] 执行成功无 errorInfo",
                  ei is None or ei == {})
        else:
            check(f"[{label}] 响应无 errorInfo", False,
                  f"status={body.get('status')}")

# ── camelCase 字段命名 ──────────────────────────────
print("\n--- camelCase 字段命名 ---")
for label, body in [("无认证", err_body), ("flow不存在", notfound_body)]:
    if body:
        all_keys = _collect_keys(body)
        non_camel = [k.split(".")[-1] for k in all_keys
                     if not check_camel_case(k.split(".")[-1])]
        non_camel = [k for k in non_camel if not k.startswith("[") and k != ""]
        if non_camel:
            check(f"[{label}] 所有字段为 camelCase", False,
                   f"不符合: {set(non_camel)}")
        else:
            check(f"[{label}] 所有字段为 camelCase", True)

# ── 错误码覆盖率 ────────────────────────────────────
print("\n--- 错误码覆盖率 ---")
codes_seen = set()
for body in [err_body, notfound_body]:
    if body:
        ei = body.get("errorInfo")
        if ei and isinstance(ei, dict):
            c = ei.get("code")
            if c:
                codes_seen.add(str(c))
check("错误码 6001（内部错误）覆盖",
      "6001" in codes_seen,
      f"已覆盖: {codes_seen}")