#!/usr/bin/env python3
"""全量回归 — 委托给 pytest"""
import sys, subprocess, os

os.chdir(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
args = ["python3", "-m", "pytest", "-v"]
if "--quiet" in sys.argv:
    args.append("-q")
sys.exit(subprocess.run(args).returncode)
