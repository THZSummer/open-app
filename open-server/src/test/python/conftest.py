"""
pytest 全局配置 — open-server 集成测试

提供：
- BASE_URL
- DB 数据准备/清理 (使用 PyMySQL)
- HTTP 客户端封装
- 测试报告生成
"""
import pytest
import requests
import time
import random
from datetime import datetime

# ── 配置项 ──────────────────────────────────────────
BASE_URL = "http://localhost:18080/open-server"
API_PREFIX = f"{BASE_URL}/api/v1"

# 数据库配置
DB_CONFIG = {
    "host": "localhost",
    "port": 3306,
    "user": "openapp",
    "password": "openapp",
    "database": "openapp",
}

TS = str(int(time.time()))


# ══════════════════════════════════════════════════
# DB 帮助函数
# ══════════════════════════════════════════════════

def _gen_snowflake_id():
    """生成模拟雪花ID (基于时间戳+随机数)"""
    ts = int(time.time() * 1000)
    rand = random.randint(0, 9999)
    return (ts << 12) | rand


def get_db_connection():
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
    """插入测试连接器，返回生成的雪花ID"""
    cursor = conn.cursor()
    name_cn = name_cn or f"测试连接器_{TS}"
    name_en = name_en or f"test_connector_{TS}"
    snow_id = _gen_snowflake_id()
    sql = """INSERT INTO openplatform_v2_cp_connector_t
             (id, name_cn, name_en, connector_type, create_by, last_update_by)
             VALUES (%s, %s, %s, 1, 'tester', 'tester')"""
    cursor.execute(sql, (snow_id, name_cn, name_en))
    conn.commit()
    cursor.close()
    return snow_id


def insert_test_flow(conn, name_cn=None, name_en=None):
    """插入测试连接流，返回生成的雪花ID"""
    cursor = conn.cursor()
    name_cn = name_cn or f"测试连接流_{TS}"
    name_en = name_en or f"test_flow_{TS}"
    snow_id = _gen_snowflake_id()
    sql = """INSERT INTO openplatform_v2_cp_flow_t
             (id, name_cn, name_en, lifecycle_status, create_by, last_update_by)
             VALUES (%s, %s, %s, 0, 'tester', 'tester')"""
    cursor.execute(sql, (snow_id, name_cn, name_en))
    conn.commit()
    cursor.close()
    return snow_id


def delete_test_connector(conn, connector_id):
    cursor = conn.cursor()
    cursor.execute("DELETE FROM openplatform_v2_cp_connector_t WHERE id = %s", (connector_id,))
    conn.commit()
    cursor.close()


def delete_test_flow(conn, flow_id):
    cursor = conn.cursor()
    cursor.execute("DELETE FROM openplatform_v2_cp_flow_t WHERE id = %s", (flow_id,))
    conn.commit()
    cursor.close()


# ══════════════════════════════════════════════════
# HTTP 客户端封装
# ══════════════════════════════════════════════════

class ApiClient:
    def __init__(self, base_url):
        self.base_url = base_url
        self.session = requests.Session()
        self.session.headers.update({"Content-Type": "application/json"})

    def post(self, path, json_data=None):
        return self.session.post(f"{self.base_url}{path}", json=json_data)

    def get(self, path, params=None):
        return self.session.get(f"{self.base_url}{path}", params=params)

    def put(self, path, json_data=None):
        return self.session.put(f"{self.base_url}{path}", json=json_data)

    def delete(self, path):
        return self.session.delete(f"{self.base_url}{path}")


# ══════════════════════════════════════════════════
# pytest fixtures
# ══════════════════════════════════════════════════

@pytest.fixture(scope="session")
def api_client():
    return ApiClient(API_PREFIX)


@pytest.fixture(scope="session")
def db_conn():
    conn = get_db_connection()
    yield conn
    conn.close()


@pytest.fixture
def test_connector(db_conn):
    conn_id = insert_test_connector(db_conn)
    yield conn_id
    try:
        delete_test_connector(db_conn, conn_id)
    except Exception:
        pass


@pytest.fixture
def test_flow(db_conn):
    flow_id = insert_test_flow(db_conn)
    yield flow_id
    try:
        delete_test_flow(db_conn, flow_id)
    except Exception:
        pass
