#!/usr/bin/env python3
"""
全量回归执行器 — connector-api

委托给 pytest 执行（对齐 open-server 模式）.
"""
import sys
import subprocess
import os
from datetime import datetime

os.chdir(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

args = ["python3", "-m", "pytest", "-v"]

if "--quiet" in sys.argv:
    args.append("-q")

if "--level" in sys.argv:
    idx = sys.argv.index("--level")
    if idx + 1 < len(sys.argv):
        args.extend(["-m", sys.argv[idx + 1]])

result = subprocess.run(args)

if "--report" in sys.argv:
    report_path = os.path.join(
        os.path.dirname(os.path.abspath(__file__)),
        "../../../../.sddu/specs-tree-root/specs-tree-connector-platform/test-report-integration.md"
    )
    now = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    with open(report_path, "w") as f:
        f.write(f"# 集成测试报告：连接器平台\n\n")
        f.write(f"**测试日期**: {now}  \n")
        f.write(f"**服务**: connector-api (:18180)\n\n")
        f.write(f"- 状态: {'PASS' if result.returncode == 0 else 'FAIL'}\n\n")
    print(f"  报告已生成: {report_path}")

sys.exit(result.returncode)
