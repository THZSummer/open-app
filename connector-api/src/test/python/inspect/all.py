#!/usr/bin/env python3
"""
全量回归执行器 — connector-api

两步执行:
  1. pytest (test_health.py 等 L0~L4 标记的 pytest 用例)
  2. 15 个 inspect 场景脚本（test_trigger_invoke.py 等）

用法:
  python3 inspect/all.py           # 全量
  python3 inspect/all.py --quiet   # 安静模式
  python3 inspect/all.py --report  # 全量 + 生成测试报告
"""
import sys
import subprocess
import os
from datetime import datetime

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
PYTHON_DIR = os.path.dirname(BASE_DIR)

quiet = "--quiet" in sys.argv
generate_report = "--report" in sys.argv

# 15 个场景脚本（排除基础设施文件）
SCRIPTS = [
    "test_trigger_invoke.py",
    "test_internal_test_run.py",
    "test_contract_response.py",
    "test_connector_auth_multiple.py",
    "test_connector_url_whitelist.py",
    "test_systoken_whitelist.py",
    "test_script_node_execution.py",
    "test_debug_draft_invoke.py",
    "test_node_timeout.py",
    "test_flow_cache.py",
    "test_parallel_branch.py",
    "test_connector_version_select.py",
    "test_execution_record_view.py",
    "test_execution_log.py",
    "test_version_config_resolve.py",
]

passed = 0
failed = 0
skipped = 0
results = []


def run_script(script_name):
    """运行单个场景脚本，解析 PASS/FAIL"""
    global passed, failed, skipped
    path = os.path.join(BASE_DIR, script_name)
    name = script_name.replace(".py", "")

    if not quiet:
        print(f"\n{'#'*60}")
        print(f"# {name}")
        print(f"{'#'*60}")

    cmd = [sys.executable, path]
    if quiet:
        cmd.append("--quiet")

    result = subprocess.run(cmd, capture_output=True, text=True, cwd=BASE_DIR, timeout=120)
    output = result.stdout + result.stderr

    all_lines = output.split('\n')
    has_fail = any("❌ FAIL:" in l for l in all_lines)
    has_skip = any("SKIP" in l or "[SKIP]" in l for l in all_lines)

    if has_fail:
        failed += 1
        results.append((name, "FAIL"))
    elif has_skip:
        skipped += 1
        results.append((name, "SKIP"))
    elif result.returncode != 0:
        failed += 1
        results.append((name, "FAIL (crash)"))
    else:
        passed += 1
        results.append((name, "PASS"))

    if not quiet:
        print(output)
        pass_count = sum(1 for l in all_lines if "✅ PASS:" in l)
        fail_count = sum(1 for l in all_lines if "❌ FAIL:" in l)
        if pass_count > 0 or fail_count > 0:
            print(f"  [断言: ✅ {pass_count}  ❌ {fail_count}]")


# ═══════════════════════════════════════════════════════════
# Step 1: pytest (L0 冒烟)
# ═══════════════════════════════════════════════════════════
if not quiet:
    print(f"{'='*60}")
    print("  Step 1/2: pytest (test_health.py L0 冒烟)")
    print(f"{'='*60}")

os.chdir(PYTHON_DIR)
pytest_args = ["python3", "-m", "pytest", "-v"]
if quiet:
    pytest_args.append("-q")

pytest_result = subprocess.run(pytest_args, capture_output=quiet, text=quiet)
pytest_ok = pytest_result.returncode == 0

if not quiet and not pytest_ok:
    print(pytest_result.stdout[-500:] if hasattr(pytest_result, 'stdout') else "")
    print(f"  ❌ pytest FAILED (exit {pytest_result.returncode})")
elif not quiet:
    print(f"  ✅ pytest PASSED")

# ═══════════════════════════════════════════════════════════
# Step 2: 场景脚本
# ═══════════════════════════════════════════════════════════
os.chdir(BASE_DIR)
if not quiet:
    print(f"\n{'='*60}")
    print(f"  Step 2/2: 15 个场景脚本")
    print(f"{'='*60}")

for script in SCRIPTS:
    try:
        run_script(script)
    except subprocess.TimeoutExpired:
        failed += 1
        results.append((script.replace(".py", ""), "TIMEOUT"))
        print(f"  ⏱️ TIMEOUT: {script}")

# ═══════════════════════════════════════════════════════════
# 汇总
# ═══════════════════════════════════════════════════════════
total = passed + failed + skipped
script_total = len(SCRIPTS)
print(f"\n{'='*60}")
print(f"  全量回归完成")
print(f"  pytest: {'PASS' if pytest_ok else 'FAIL'}")
print(f"  场景脚本: {script_total} 个")
print(f"  PASS: {passed}  FAIL: {failed}  SKIP: {skipped}")
if failed == 0 and pytest_ok:
    print(f"  通过率: 100%")
else:
    print(f"  通过率: {passed/script_total*100:.1f}% (场景)")
print(f"{'='*60}")

# 失败列表
if failed > 0:
    print(f"\n  失败列表:")
    for name, status in results:
        if status != "PASS":
            print(f"    ❌ {name}: {status}")

# 生成报告
if generate_report:
    report_path = os.path.join(
        BASE_DIR,
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
        f.write(f"| pytest | {'PASS' if pytest_ok else 'FAIL'} |\n")
        f.write(f"| 场景脚本 | {script_total} |\n")
        f.write(f"| PASS | {passed} |\n")
        f.write(f"| FAIL | {failed} |\n")
        f.write(f"| SKIP | {skipped} |\n")
        f.write(f"| 通过率 | {passed/script_total*100:.1f}% |\n\n")
        f.write(f"---\n\n")
        f.write(f"## 详细结果\n\n")
        for sname, status in results:
            f.write(f"- {status}: {sname}\n")
        f.write(f"\n---\n\n")
        f.write(f"*报告由 all.py 自动生成*\n")
    print(f"  报告已生成: {report_path}")

# 退出码
if failed > 0 or not pytest_ok:
    sys.exit(1)
