# open-server 集成测试

## 目录

```
python/
├── README.md
├── pytest.ini                  ← pytest 配置
├── conftest.py                 ← 共享 fixtures（自动管理测试数据生命周期）
├── test_connector.py           ← 连接器 CRUD (#1~#7)
├── test_connector_version.py   ← 连接器版本生命周期 (#8~#16)
├── test_flow.py                ← 连接流 CRUD
├── test_flow_version.py        ← 连接流版本生命周期
├── test_flow_debug.py          ← 调试 (#51)
├── test_approval.py            ← 审批
├── test_security.py            ← 安全/审计
├── test_misc.py                ← 其他
└── inspect/                    ← 旧版脚本（逐步迁移中）
    ├── client.py               ← 统一入口（api / db / redis / ok）
    └── ...
```

## 用法

```bash
cd open-server/src/test/python

# 全量
pytest -v

# 指定模块
pytest test_connector.py -v

# 指定 marker
pytest -m connector -v

# 并行 + 报告
pytest -n auto --html=report.html
```

## 编写测试

```python
from conftest import api, db, connector, published_connector

def test_create_connector():
    resp = api("POST", "/connectors", {"nameCn": "测试", "nameEn": "Test", "connectorType": 1})
    assert resp.status_code == 200
    assert resp.json()["data"]["connectorId"]

def test_detail(connector):         # fixture 自动建/删
    resp = api("GET", f"/connectors/{connector}")
    assert resp.status_code == 200

@pytest.mark.parametrize("ctype", [99, 0, -1])
def test_invalid_type(ctype):       # 参数化
    resp = api("POST", "/connectors", {"nameCn": "T", "nameEn": "T", "connectorType": ctype})
    assert resp.status_code == 400
```

## 配置

所有基础设施集中在 `inspect/client.py` 顶部：

```python
TEST_APP_ID = "202606241730488926"
_API_BASE   = "http://localhost:18080/open-server"
_DB         = {"host": "192.168.3.155", "user": "openapp", "passwd": "openapp", "db": "openapp"}
_REDIS      = {"host": "192.168.3.201", "port": 6379, "password": "openapp"}
```

## 旧版脚本

`inspect/` 目录保留原有单文件脚本，通过 `python3 inspect/all.py` 运行。pytest 框架逐步覆盖后可按需清理。
