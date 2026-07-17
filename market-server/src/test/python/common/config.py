#!/usr/bin/env python3
"""
集成测试配置 — 部署到新环境只需改这一个文件
==============================================
所有地址、账号、凭证集中在此。
"""

# ── 被测服务 ──────────────────────────────────────────
# market-server 基地址（含 context-path）
MARKET_SERVER_BASE = "http://localhost:18083/market-server"

# ── 测试账号 ──────────────────────────────────────────
TEST_APP_ID        = "20250730213114178360970"
INTERNAL_APP_ID    = 328225464973787136   # App.id 数值 (DB 关联用)
TEST_COOKIE        = "user_id=admin"
TEST_XSRF_TOKEN    = "user_id=admin"

# ── 数据库 (MySQL) ────────────────────────────────────
DB_HOST  = "192.168.3.155"
DB_PORT  = 3306
DB_USER  = "openapp"
DB_PASS  = "openapp"
DB_NAME  = "openapp"

# ── 通用 ──────────────────────────────────────────────
REQUEST_TIMEOUT = 10
