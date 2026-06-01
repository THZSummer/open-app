#!/usr/bin/env python3
"""全量回归执行器 — open-server

用法:
  python3 inspect/all.py           # 每个接口显示完整详情
  python3 inspect/all.py --quiet   # 只输出摘要
  python3 inspect/all.py --report  # 全量 + 生成测试报告
"""
import sys
import subprocess
import os
from datetime import datetime

SCRIPTS = [
    "api_delete.py",
    "event_delete.py",
    "callback_delete.py",
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
if total > 0:
    print(f"  通过率: {passed/total*100:.1f}%")
print(f"{'='*60}")

# Generate report
if generate_report:
    report_path = os.path.join(
        base_dir,
        "../../../../.sddu/specs-tree-root/specs-tree-capability-open-platform/test-report-integration-delete.md"
    )
    now = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    with open(report_path, "w") as f:
        f.write(f"# 集成测试报告：删除接口\n\n")
        f.write(f"**测试日期**: {now}  \n")
        f.write(f"**测试类型**: L3 集成测试（真实服务 + 真实数据库）  \n")
        f.write(f"**服务**: open-server (:18080)\n\n")
        f.write(f"---\n\n")
        f.write(f"## 执行结果摘要\n\n")
        f.write(f"| 指标 | 数值 |\n")
        f.write(f"|------|:----:|\n")
        f.write(f"| 总用例数 | {total} |\n")
        f.write(f"| PASS | {passed} |\n")
        f.write(f"| FAIL | {failed} |\n")
        f.write(f"| SKIP | {skipped} |\n")
        if total > 0:
            f.write(f"| 通过率 | {passed/total*100:.1f}% |\n\n")
        f.write(f"---\n\n")
        f.write(f"## 详细结果\n\n")
        for sname, status in results:
            f.write(f"- {status}: {sname}\n")
        f.write(f"\n---\n\n")
        f.write(f"*报告由 all.py 自动生成*\n")
    print(f"  报告已生成: {report_path}")
