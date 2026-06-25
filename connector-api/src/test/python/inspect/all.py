#!/usr/bin/env python3
"""
全量回归执行器 — connector-api

委托给 pytest 执行（对齐 open-server 模式）.

用法:
  python3 inspect/all.py           # 默认 L0 冒烟（秒级反馈）
  python3 inspect/all.py --quiet   # 安静模式
  python3 inspect/all.py --report  # 全量 + 生成测试报告
  python3 inspect/all.py --all     # 全量 (L0+L1+L2+L3+L4)
  python3 inspect/all.py --level L1  # 指定级别
"""
import sys
import subprocess
import os
from datetime import datetime

# 切换到 python/ 目录（pytest root）
os.chdir(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

# 构建 pytest 参数
args = ["python3", "-m", "pytest", "-v"]

if "--quiet" in sys.argv:
    args.append("-q")

if "--all" in sys.argv:
    args.extend(["-m", ""])  # 空字符串 = 不过滤，跑全部
elif "--level" in sys.argv:
    idx = sys.argv.index("--level")
    if idx + 1 < len(sys.argv):
        args.extend(["-m", sys.argv[idx + 1]])

# 执行
result = subprocess.run(args)

# 生成报告（可选）
if "--report" in sys.argv:
    report_path = os.path.join(
        os.path.dirname(os.path.abspath(__file__)),
        "../../../../.sddu/specs-tree-root/specs-tree-connector-platform/test-report-integration.md"
    )
    now = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    passed = result.returncode == 0
    with open(report_path, "w") as f:
        f.write(f"# 集成测试报告：连接器平台\n\n")
        f.write(f"**测试日期**: {now}  \n")
        f.write(f"**测试类型**: L3 集成测试  \n")
        f.write(f"**服务**: connector-api (:18180)\n\n")
        f.write(f"---\n\n")
        f.write(f"## 执行结果\n\n")
        f.write(f"- 状态: {'✅ PASS' if passed else '❌ FAIL'}\n\n")
        f.write(f"---\n\n")
        f.write(f"*报告由 all.py 自动生成*\n")
    print(f"  报告已生成: {report_path}")

sys.exit(result.returncode)
