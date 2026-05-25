"""
pytest 全局配置 — open-server 集成测试

提供：
- BASE_URL
- DB 数据准备/清理
- HTTP 客户端封装
- 测试报告生成
"""
import pytest
import requests
import time
import os
from datetime import datetime

# ── 配置项 ──────────────────────────────────────────
BASE_URL = "http://localhost:18080/open-server"
API_PREFIX = f"{BASE_URL}/api/v1"

# 数据库配置（直连 MySQL 做数据准备/清理）
DB_CONFIG = {
    "host": "localhost",
    "port": 3306,
    "user": "openapp",
    "password": "openapp",
    "database": "openapp",
}

# 测试数据时间戳后缀（防冲突）
TS = str(int(time.time()))

# ══════════════════════════════════════════════════
# DB 帮助函数
# ══════════════════════════════════════════════════

def get_db_connection():
    """获取 MySQL 连接"""
    import pymysql
    return pymysql.connect(
        host=DB_CONFIG["host"],
        port=DB_CONFIG["port"],
        user=DB_CONFIG["user"],
        password=DB_CONFIG["password"],
        database=DB_CONFIG["database"],
        charset="utf8mb4",
    )


def insert_test_connector(conn, name_cn=None, name_en=None):
    """插入测试连接器，返回插入的 ID"""
    import pymysql
    cursor = conn.cursor()
    name_cn = name_cn or f"测试连接器_{TS}"
    name_en = name_en or f"test_connector_{TS}"
    now = datetime.now().strftime("%Y-%m-%d %H:%M:%S.%f")[:-3]
    sql = """INSERT INTO openplatform_v2_cp_connector
             (name_cn, name_en, connector_type, created_by, created_time, last_update_by, last_update_time)
             VALUES (%s, %s, 1, 'tester', %s, 'tester', %s)"""
    cursor.execute(sql, (name_cn, name_en, now, now))
    conn.commit()
    inserted_id = cursor.lastrowid
    cursor.close()
    return inserted_id


def insert_test_flow(conn, name_cn=None, name_en=None):
    """插入测试连接流，返回插入的 ID"""
    cursor = conn.cursor()
    name_cn = name_cn or f"测试连接流_{TS}"
    name_en = name_en or f"test_flow_{TS}"
    now = datetime.now().strftime("%Y-%m-%d %H:%M:%S.%f")[:-3]
    sql = """INSERT INTO openplatform_v2_cp_flow
             (name_cn, name_en, lifecycle_status, created_by, created_time, last_update_by, last_update_time)
             VALUES (%s, %s, 0, 'tester', %s, 'tester', %s)"""
    cursor.execute(sql, (name_cn, name_en, now, now))
    conn.commit()
    inserted_id = cursor.lastrowid
    cursor.close()
    return inserted_id


def delete_test_connector(conn, connector_id):
    """删除测试连接器"""
    cursor = conn.cursor()
    cursor.execute("DELETE FROM openplatform_v2_cp_connector WHERE id = %s", (connector_id,))
    conn.commit()
    cursor.close()


def delete_test_flow(conn, flow_id):
    """删除测试连接流"""
    cursor = conn.cursor()
    cursor.execute("DELETE FROM openplatform_v2_cp_flow WHERE id = %s", (flow_id,))
    conn.commit()
    cursor.close()


# ══════════════════════════════════════════════════
# HTTP 客户端封装
# ══════════════════════════════════════════════════

class ApiClient:
    """API 测试客户端，封装 requests"""

    def __init__(self, base_url):
        self.base_url = base_url
        self.session = requests.Session()
        self.session.headers.update({"Content-Type": "application/json"})

    def post(self, path, json_data=None):
        url = f"{self.base_url}{path}"
        return self.session.post(url, json=json_data)

    def get(self, path, params=None):
        url = f"{self.base_url}{path}"
        return self.session.get(url, params=params)

    def put(self, path, json_data=None):
        url = f"{self.base_url}{path}"
        return self.session.put(url, json=json_data)

    def delete(self, path):
        url = f"{self.base_url}{path}"
        return self.session.delete(url)


# ══════════════════════════════════════════════════
# pytest fixtures
# ══════════════════════════════════════════════════

@pytest.fixture(scope="session")
def api_client():
    """全局 API 客户端"""
    return ApiClient(API_PREFIX)


@pytest.fixture(scope="session")
def db_conn():
    """全局 DB 连接"""
    conn = get_db_connection()
    yield conn
    conn.close()


@pytest.fixture
def test_connector(db_conn):
    """创建一个测试连接器，测试结束后清理"""
    conn_id = insert_test_connector(db_conn)
    yield conn_id
    try:
        delete_test_connector(db_conn, conn_id)
    except Exception:
        pass


@pytest.fixture
def test_flow(db_conn):
    """创建一个测试连接流，测试结束后清理"""
    flow_id = insert_test_flow(db_conn)
    yield flow_id
    try:
        delete_test_flow(db_conn, flow_id)
    except Exception:
        pass
