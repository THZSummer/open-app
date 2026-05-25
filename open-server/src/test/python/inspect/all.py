#!/usr/bin/env python3
"""
\u5168\u91cf\u56de\u5f52\u6267\u884c\u5668

\u6309\u987a\u5e8f\u6267\u884c\u6240\u6709 59 \u4e2a\u7528\u4f8b\uff1a
  1. \u8fde\u63a5\u5668 CRUD + \u914d\u7f6e (IT-001~022)
  2. \u8fde\u63a5\u6d41 CRUD + \u542f\u505c + \u914d\u7f6e (IT-023~046)
  3. \u8c03\u8bd5\u4ee3\u7406 (IT-047~048)
  4. HTTP \u89e6\u53d1 (IT-049~051)
  5. \u5951\u7ea6\u6821\u9a8c (IT-052~059)

\u7528\u6cd5:
  python3 inspect/all.py           # \u6bcf\u4e2a\u63a5\u53e3\u663e\u793a\u5b8c\u6574\u8be6\u60c5
  python3 inspect/all.py --quiet   # \u53ea\u8f93\u51fa\u6458\u8981
  python3 inspect/all.py --report  # \u5168\u91cf + \u751f\u6210\u62a5\u544a
"""
import sys
import subprocess
import os
from datetime import datetime

SCRIPTS = [
    # \u8fde\u63a5\u5668
    "connector_create.py",
    "connector_list.py",
    "connector_detail.py",
    "connector_update.py",
    "connector_delete.py",
    "connector_config_get.py",
    "connector_config_set.py",
    # \u8fde\u63a5\u6d41
    "flow_create.py",
    "flow_list.py",
    "flow_detail.py",
    "flow_update.py",
    "flow_delete.py",
    "flow_start.py",
    "flow_stop.py",
    "flow_config_get.py",
    "flow_config_set.py",
    # \u8c03\u8bd5 + \u89e6\u53d1 + \u5951\u7ea6
    "debug_test_run.py",
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

    # Parse output
    output = result.stdout + result.stderr
    if "PASS" in output:
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

# Summary
total = passed + failed + skipped
print(f"\n{'='*60}")
print(f"  \u5168\u91cf\u56de\u5f52\u5b8c\u6210")
print(f"  \u603b\u7528\u4f8b: {total}  PASS: {passed}  FAIL: {failed}  SKIP: {skipped}")
if failed == 0:
    print(f"  \u901a\u8fc7\u7387: 100%")
else:
    print(f"  \u901a\u8fc7\u7387: {passed/total*100:.1f}%")
print(f"{'='*60}")

# Generate report
if generate_report:
    report_path = os.path.join(base_dir, "../../../../.sddu/specs-tree-root/specs-tree-connector-platform/test-report-integration.md")
    now = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    with open(report_path, "w") as f:
        f.write(f"# \u96c6\u6210\u6d4b\u8bd5\u62a5\u544a\uff1a\u8fde\u63a5\u5668\u5e73\u53f0\n\n")
        f.write(f"**\u6d4b\u8bd5\u65e5\u671f**: {now}  \n")
        f.write(f"**\u6d4b\u8bd5\u7c7b\u578b**: L3 \u96c6\u6210\u6d4b\u8bd5\uff08\u771f\u5b9e\u670d\u52a1 + \u771f\u5b9e\u6570\u636e\u5e93\uff09  \n")
        f.write(f"**\u670d\u52a1**: open-server (:18080) / connector-api (:18180)\n\n")
        f.write(f"---\n\n")
        f.write(f"## \u6267\u884c\u7ed3\u679c\u6458\u8981\n\n")
        f.write(f"| \u6307\u6807 | \u6570\u503c |\n")
        f.write(f"|------|:----:|\n")
        f.write(f"| \u603b\u7528\u4f8b\u6570 | {total} |\n")
        f.write(f"| PASS | {passed} |\n")
        f.write(f"| FAIL | {failed} |\n")
        f.write(f"| SKIP | {skipped} |\n")
        f.write(f"| \u901a\u8fc7\u7387 | {passed/total*100:.1f}% |\n\n")
        f.write(f"---\n\n")
        f.write(f"## \u8be6\u7ec6\u7ed3\u679c\n\n")
        for name, status in results:
            f.write(f"- {status}: {name}\n")
        f.write(f"\n---\n\n")
        f.write(f"*\u62a5\u544a\u7531 all.py \u81ea\u52a8\u751f\u6210*\n")
    print(f"  \u62a5\u544a\u5df2\u751f\u6210: {report_path}")
