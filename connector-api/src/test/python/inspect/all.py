#!/usr/bin/env python3
"""
全量回归执行器 — connector-api

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
    "trigger_invoke.py",
    "test_run.py",
    "contract_response.py",
    # V3 E2E: 认证 + 安全 + 脚本 + 调试
    "connector_auth_multiple.py",
    "connector_url_whitelist.py",
    "systoken_whitelist.py",  # NEW: FR-036 SYSTOKEN白名单
    "script_node_execution.py",
    "debug_draft_invoke.py",
    # V3 E2E: Flow config — timeout + cache + parallel + version select
    "node_timeout.py",
    "flow_cache.py",
    "parallel_branch.py",
    "connector_version_select.py",
    # V3 E2E: 运行时 — 执行记录 + 日志 + 版本解析
    "execution_record_view.py",
    "execution_log.py",
    "version_config_resolve.py",
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

    # Parse stdout for actual assertion results (not exit code)
    # Individual test scripts use check() which prints "❌ FAIL:" on failure
    # and "✅ PASS:" on success. These are the source of truth.
    # Fallback to exit code for unhandled crashes (no FAIL marker but non-zero exit).
    all_lines = output.split('\n')
    has_fail = any("❌ FAIL:" in l and "✅ PASS:" not in l for l in all_lines)
    has_skip = "SKIP" in output or "[SKIP]" in output

    if has_fail:
        failed += 1
        results.append((name, "FAIL"))
    elif has_skip:
        skipped += 1
        results.append((name, "SKIP"))
    elif result.returncode != 0:
        # Crash or unhandled exception — no FAIL marker in output
        failed += 1
        results.append((name, "FAIL (crash)"))
    else:
        passed += 1
        results.append((name, "PASS"))

    if not quiet:
        print(output)
        # Show per-script assertion counts from stdout
        pass_count = sum(1 for l in all_lines if "✅ PASS:" in l and "❌ FAIL:" not in l)
        fail_count = sum(1 for l in all_lines if "❌ FAIL:" in l and "✅ PASS:" not in l)
        if pass_count > 0 or fail_count > 0:
            print(f"  [脚本断言: ✅ {pass_count}  ❌ {fail_count}]")

total = passed + failed + skipped
print(f"\n{'='*60}")
print(f"  全量回归完成")
print(f"  总用例: {total}  PASS: {passed}  FAIL: {failed}  SKIP: {skipped}")
if failed == 0:
    print(f"  通过率: 100%")
else:
    print(f"  通过率: {passed/total*100:.1f}%")
print(f"{'='*60}")

# Generate report
if generate_report:
    report_path = os.path.join(
        base_dir,
        "../../../../.sddu/specs-tree-root/specs-tree-connector-platform/test-report-integration.md"
    )
    now = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    with open(report_path, "w") as f:
        f.write(f"# 集成测试报告：连接器平台\n\n")
        f.write(f"**测试日期**: {now}  \n")
        f.write(f"**测试类型**: L3 集成测试（真实服务 + 真实数据库）  \n")
        f.write(f"**服务**: connector-api (:18180)\n\n")
        f.write(f"---\n\n")
        f.write(f"## 执行结果摘要\n\n")
        f.write(f"| 指标 | 数值 |\n")
        f.write(f"|------|:----:|\n")
        f.write(f"| 总用例数 | {total} |\n")
        f.write(f"| PASS | {passed} |\n")
        f.write(f"| FAIL | {failed} |\n")
        f.write(f"| SKIP | {skipped} |\n")
        f.write(f"| 通过率 | {passed/total*100:.1f}% |\n\n")
        f.write(f"---\n\n")
        f.write(f"## 详细结果\n\n")
        for sname, status in results:
            f.write(f"- {status}: {sname}\n")
        f.write(f"\n---\n\n")
        f.write(f"*报告由 all.py 自动生成*\n")
    print(f"  报告已生成: {report_path}")
