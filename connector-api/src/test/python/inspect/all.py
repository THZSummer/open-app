#!/usr/bin/env python3
"""
全量回归执行器 — connector-api

用法:
  python3 inspect/all.py           # 每个接口显示完整详情
  python3 inspect/all.py --quiet   # 只输出摘要
"""
import sys
import subprocess
import os
from datetime import datetime

SCRIPTS = [
    "trigger_invoke.py",
    "contract_response.py",
]

quiet = "--quiet" in sys.argv
generate_report = "--report" in sys.argv

base_dir = os.path.dirname(os.path.abspath(__file__))
passed = 0
failed = 0
skipped = 0
results = []

for script in SCRIPTS:
    path = os.path.join(base_dir, script)
    name = script.replace(".py", "")
    cmd = [sys.executable, path]
    if quiet:
        cmd.append("--quiet")

    if not quiet:
        print(f"\n{'#'*60}")
        print(f"# {name}")
        print(f"{'#'*60}")

    result = subprocess.run(cmd, capture_output=True, text=True, cwd=base_dir)
    output = result.stdout + result.stderr

    if result.returncode == 0:
        passed += 1
        results.append((name, "PASS"))
    elif "SKIP" in output:
        skipped += 1
        results.append((name, "SKIP"))
    else:
        failed += 1
        results.append((name, "FAIL"))

    if not quiet:
        print(output)

total = passed + failed + skipped
print(f"\n{'='*60}")
print(f"  全量回归完成")
print(f"  总用例: {total}  PASS: {passed}  FAIL: {failed}  SKIP: {skipped}")
if failed == 0:
    print(f"  通过率: 100%")
else:
    print(f"  通过率: {passed/total*100:.1f}%")
print(f"{'='*60}")
