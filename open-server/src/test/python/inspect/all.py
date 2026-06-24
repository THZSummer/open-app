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
    # V3 E2E: 删除接口
    "api_delete.py",
    "event_delete.py",
    "callback_delete.py",
    # V3 E2E: 连接器 CRUD 基础操作
    "connector_create.py",
    "connector_delete.py",
    "connector_detail.py",
    "connector_update.py",
    "connector_list.py",
    # V3 E2E: 连接流 CRUD 基础操作
    "flow_create.py",
    "flow_delete.py",
    "flow_detail.py",
    "flow_update.py",
    "flow_list.py",
    "flow_start.py",
    "flow_stop.py",
    # V3 E2E: 连接器版本生命周期 + 恢复
    "connector_version_lifecycle.py",
    "connector_recover.py",
    "connector_crud.py",
    # V3 E2E: 连接流版本生命周期 + 恢复 + 复制 + 停止重启
    "flow_version_lifecycle.py",
    "flow_recover.py",
    "flow_copy.py",
    "flow_stop_restart.py",
    # V3 E2E: 审批 + 部署启动调用
    "flow_approval_full_flow.py",
    "approval_engine_callback.py",
    "flow_deploy_start_invoke.py",
    # V3 E2E: 安全 + 审计 + 校验 + 调试
    "app_whitelist.py",
    "operation_log.py",
    "json_validation.py",
    "flow_version_debug.py",
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
    # Exclude summary lines that contain BOTH markers.
    # Fallback to exit code for unhandled crashes.
    all_lines = output.split('\n')
    fail_lines = [l for l in all_lines if "❌ FAIL:" in l and "✅ PASS:" not in l]
    has_fail = len(fail_lines) > 0
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
        all_lines = output.split('\n')
        pass_count = sum(1 for l in all_lines if "✅ PASS:" in l and "❌ FAIL:" not in l)
        fail_count = sum(1 for l in all_lines if "❌ FAIL:" in l and "✅ PASS:" not in l)
        if pass_count > 0 or fail_count > 0:
            print(f"  [脚本断言: ✅ {pass_count}  ❌ {fail_count}]")

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
