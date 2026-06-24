# open-server 集成测试

基于 pytest 的连接器平台 V3 端到端集成测试。

## 设计思想

### 文件划分：一个接口文件对应一组 API

每个测试文件对应 plan-api 中的一个接口分组，做到「改哪个 API 就知道改哪个测试文件」：

| 文件 | 接口 | 用例 |
|------|------|:--:|
| `test_connector_crud.py` | #1~#7 连接器 CRUD + 生命周期 | 19 |
| `test_connector_version.py` | #8~#16 连接器版本 | 9 |
| `test_flow_crud.py` | #17~#27 连接流 CRUD + 生命周期 | 34 |
| `test_flow_version.py` | #28~#36 连接流版本 | 11 |
| `test_flow_version_debug.py` | #51 版本调试 | 2 |
| `test_approval.py` | #37~#44 审批 | 8 |
| `test_security.py` | 白名单 + 操作日志 | 6 |
| `test_misc.py` | API/事件/回调删除 + JSON 校验 | 12 |

### 用例分层：L0~L4 五级金字塔

每个用例标注优先级，默认只跑最关键的 L0：

```
        L0 (4)    ← 冒烟：列表能通
       L1 (35)    ← 核心 CRUD：增删改查
      L2 (32)     ← 生命周期：发布/失效/恢复
     L3 (13)      ← 端到端：部署→启动→调试
    L4 (17)       ← 边界反向：缺参数/超长/404
```

| 级别 | 含义 | CI 触发 | 预期耗时 |
|:--:|------|------|:--:|
| L0 | 冒烟 — 最基础连通性 | 每次 commit | <5s |
| L1 | 核心 CRUD — 增删改查全字段 | PR 门禁 | <30s |
| L2 | 生命周期 — 状态流转/部署/停止 | 每日回归 | <1min |
| L3 | 端到端 — 部署→启动→调用→记录 | 每周 | <2min |
| L4 | 边界反向 — 异常/校验/极端场景 | 发布前 | <3min |

### fixtures：自动管理数据生命周期

```python
def test_detail(connector):      # fixture 自动建连接器
    resp = api("GET", f"/connectors/{connector}")
    assert resp.status_code == 200
    # 测试结束自动删除，无需 finally 清理
```

| fixture | 提供 |
|---------|------|
| `connector` | 空连接器（无版本） |
| `draft_connector` | 含草稿版本的连接器 |
| `published_connector` | 含已发布版本的连接器 |
| `flow` | 空连接流 |
| `draft_flow` | 含草稿版本的连接流 |
| `deployed_flow` | 已部署的连接流（含版本 + deployed 指针） |

### 配置：单一入口

所有基础设施配置（数据库、Redis、测试应用、用户）集中在 `inspect/client.py`：

```python
TEST_APP_ID = "202606241730488926"
# 切换环境只需改这一个文件
```

## 用法

```bash
cd open-server/src/test/python

# 默认 L0 冒烟（4 个用例，秒级反馈）
pytest

# 指定层级
pytest -m L1                     # L1 核心 CRUD
pytest -m "L0 or L1"             # PR 门禁
pytest -m "not L4"               # 每日回归

# 指定文件
pytest test_connector_crud.py -v

# 并行 + 报告
pytest -n auto --html=report.html

# 全量
pytest -m ""                     # 空字符串覆盖 -m L0 默认
```

## 目录

```
python/
├── README.md
├── pytest.ini                  ← 配置(markers/addopts)
├── conftest.py                 ← 共享 fixtures
├── test_connector_crud.py      ← #1~#7
├── test_connector_version.py   ← #8~#16
├── test_flow_crud.py           ← #17~#27
├── test_flow_version.py        ← #28~#36
├── test_flow_version_debug.py  ← #51
├── test_approval.py            ← #37~#44
├── test_security.py
├── test_misc.py
└── inspect/
    └── client.py               ← 基础设施(api/db/redis/ok)
```
