#!/usr/bin/env python3
"""
集成测试配置 — 部署到新环境只需改这一个文件
==============================================
所有地址、账号、凭证集中在此。各处测试脚本通过
  from common.client import ...
获取，无需分散修改。
"""

# ── 被测服务 ──────────────────────────────────────────
# open-server 基地址
OPEN_SERVER_BASE = "http://localhost:18080/open-server"

# ── 测试账号 ──────────────────────────────────────────
TEST_APP_ID        = "20250730213114178360970"
INTERNAL_APP_ID    = 328225464973787136   # App.id 数值 (DB 关联用)
TEST_COOKIE        = "user_id=admin"
TEST_XSRF_TOKEN    = "user_id=admin"

# ── SYSTOKEN 鉴权 ─────────────────────────────────────
SYSTOKEN_HEADER = "X-Sys-Token"            # 触发器校验的 HTTP 头字段名
SYSTOKEN_VALUE  = "tester"                 # 测试用的 token 值 (同时作为 whitelist 账号)

# ── 数据库 (MySQL) ────────────────────────────────────
DB_HOST  = "192.168.3.155"
DB_PORT  = 3306
DB_USER  = "openapp"
DB_PASS  = "openapp"
DB_NAME  = "openapp"

# ── Redis 集群 ────────────────────────────────────────
REDIS_PASSWORD = "openapp"
REDIS_CLUSTER_NODES = [
    ("192.168.3.201", "6379"), ("192.168.3.202", "6379"),
    ("192.168.3.203", "6379"), ("192.168.3.204", "6379"),
    ("192.168.3.205", "6379"), ("192.168.3.206", "6379"),
]

# ── 关联服务 ──────────────────────────────────────────
CONNECTOR_API_BASE    = "http://localhost:18180/api/v1"
CONNECTOR_API_HEALTH  = "http://localhost:18180/actuator/health"
MOCK_SERVER_URL        = "http://localhost:18980"
MOCK_SERVER_PARALLEL_URL = "http://localhost:18982"

# ── 通用 ──────────────────────────────────────────────
REQUEST_TIMEOUT = 10
