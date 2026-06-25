# open-server 集成测试

基于 pytest 的连接器平台 V3 端到端集成测试。

## 设计思想

### 文件划分：一个接口一个文件

每个测试文件对应 plan-api 中的一个接口，文件名 = `test_{resource}_{action}.py` 跟 API 路径一致：

| 文件 | API # | 接口 |
|------|:--:|------|
| **连接器** | | |
| `test_connector_create.py` | #1 | POST /connectors |
| `test_connector_list.py` | #2 | GET /connectors |
| `test_connector_detail.py` | #3 | GET /connectors/{id} |
| `test_connector_update.py` | #4 | PUT /connectors/{id} |
| `test_connector_invalidate.py` | #5 | PUT /connectors/{id}/invalidate |
| `test_connector_recover.py` | #6 | PUT /connectors/{id}/recover |
| `test_connector_delete.py` | #7 | DELETE /connectors/{id} |
| **连接器版本** | | |
| `test_connector_version_create.py` | #8 | POST /connectors/{id}/versions |
| `test_connector_version_list.py` | #9 | GET /connectors/{id}/versions |
| `test_connector_version_detail.py` | #10 | GET /connectors/{id}/versions/{vid} |
| `test_connector_version_update.py` | #11 | PUT /connectors/{id}/versions/{vid} |
| `test_connector_version_publish.py` | #12 | PUT /connectors/{id}/versions/{vid}/publish |
| `test_connector_version_copy.py` | #13 | POST /connectors/{id}/versions/{vid}/copy-to-draft |
| `test_connector_version_invalidate.py` | #14 | PUT /connectors/{id}/versions/{vid}/invalidate |
| `test_connector_version_recover.py` | #15 | PUT /connectors/{id}/versions/{vid}/recover |
| `test_connector_version_delete.py` | #16 | DELETE /connectors/{id}/versions/{vid} |
| **连接流** | | |
| `test_flow_create.py` | #17 | POST /flows |
| `test_flow_list.py` | #18 | GET /flows |
| `test_flow_detail.py` | #19 | GET /flows/{id} |
| `test_flow_update.py` | #20 | PUT /flows/{id} |
| `test_flow_copy.py` | #21 | POST /flows/{id}/copy |
| `test_flow_deploy.py` | #22 | POST /flows/{id}/deploy |
| `test_flow_start.py` | #23 | POST /flows/{id}/start |
| `test_flow_stop.py` | #24 | POST /flows/{id}/stop |
| `test_flow_invalidate.py` | #25 | PUT /flows/{id}/invalidate |
| `test_flow_recover.py` | #26 | PUT /flows/{id}/recover |
| `test_flow_delete.py` | #27 | DELETE /flows/{id} |
| **连接流版本** | | |
| `test_flow_version_create.py` | #28 | POST /flows/{id}/versions |
| `test_flow_version_list.py` | #29 | GET /flows/{id}/versions |
| `test_flow_version_detail.py` | #30 | GET /flows/{id}/versions/{vid} |
| `test_flow_version_update.py` | #31 | PUT /flows/{id}/versions/{vid} |
| `test_flow_version_publish.py` | #32 | POST /flows/{id}/versions/{vid}/publish |
| `test_flow_version_copy.py` | #33 | POST /flows/{id}/versions/{vid}/copy-to-draft |
| `test_flow_version_invalidate.py` | #34 | PUT /flows/{id}/versions/{vid}/invalidate |
| `test_flow_version_recover.py` | #35 | PUT /flows/{id}/versions/{vid}/recover |
| `test_flow_version_delete.py` | #36 | DELETE /flows/{id}/versions/{vid} |
| `test_flow_version_debug.py` | #51 | POST /flows/{id}/versions/{vid}/debug |
| **运行记录** | | |
| `test_execution_records.py` | #49~#50 | GET /flows/{id}/executions + 详情 |
| **审批记录** | | |
| `test_approval_records.py` | #39~#44 | 审批记录列表/详情/批量操作 |
| **审批流模板** | | |
| `test_approval_flow_template.py` | #45~#48 | 审批流模板 CRUD |
| **数据处理** | | |
| `test_data_processor_functions.py` | #52 | GET /data-processor/functions |
| **端到端** | | |
| `test_flow_deploy_invoke.py` | – | 部署→启动→调用 全链路 |
| `test_flow_stop_restart.py` | – | 停止→重启 全链路 |
| **其他** | | |
| `test_approval.py` | #37~#48 | 审批流程 |
| `test_security.py` | – | 白名单准入 + 操作日志 |
| `test_misc.py` | – | API/事件/回调删除 + JSON 校验 |

### 用例分层：L0~L4 五级金字塔

每个用例标注优先级，默认只跑最关键的 L0：

```
         L0 (6)    ← 冒烟：列表能通
        L1 (54)    ← 核心 CRUD：增删改查
        L2 (32)     ← 生命周期：发布/失效/恢复
       L3 (13)      ← 端到端：部署→启动→调试
      L4 (23)       ← 边界反向：缺参数/超长/404
```

| 级别 | 含义 | CI 触发 | 预期耗时 |
|:--:|------|------|:--:|
| L0 | 冒烟 — 最基础连通性 | 每次 commit | <5s |
| L1 | 核心 CRUD — 增删改查全字段 | PR 门禁 | <30s |
| L2 | 生命周期 — 状态流转/部署/停止 | 每日回归 | <1min |
| L3 | 端到端 — 部署→启动→调用→记录 | 每周 | <2min |
| L4 | 边界反向 — 异常/校验/极端场景 | 发布前 | <3min |

### 验证标准（禁止「假 OK」）

> ⚠️ **红线**：禁止仅断言 `status_code == 200` 而不验证业务数据。每条用例必须校验操作的实际效果。

| 操作类型 | 最少验证项 | 示例 |
|---------|-----------|------|
| **CREATE** | ① 返回字段与输入一致 ② 业务状态正确（如 status=1 有效不可用） ③ BIGINT ID 为 string | `assert d["nameCn"] == body["nameCn"]` + `assert d["status"] == 1` |
| **GET 详情** | ① 资源 ID 与请求匹配 ② 关键业务字段存在 | `assert d["connectorId"] == str(cid)` + `assert "status" in d` |
| **LIST** | ① `data` 为数组 ② 分页参数与返回一致 ③ 列表项含关键字段 | `assert isinstance(items, list)` + `assert page["pageSize"] == 3` |
| **UPDATE** | **重新 GET 验证变更已持久化** | 先 `PUT` → 再 `GET` 同一资源 → `assert d["nameCn"] == new_name` |
| **DELETE** | **验证资源已不可访问** | 先 `DELETE` → 再 `GET` → `assert status in (4, None)` |
| **生命周期** | ① 前置状态正确 ② 操作后状态变更符合规范 | `assert status_before == 1` → 操作 → `assert status_after == 2` |

**反例（禁止）：**
```python
# ❌ 假 OK — 无论后端返回什么都是 PASS
def test_create(self):
    resp = api("POST", "/connectors", {...})
    assert resp.status_code == 200

# ✅ 真验证 — 必须返回正确业务数据
def test_create(self):
    resp = api("POST", "/connectors", body)
    assert resp.status_code == 200
    d = resp.json()["data"]
    assert d["nameCn"] == body["nameCn"]
    assert d["status"] == 1          # FR-001: 有效不可用
     assert isinstance(d["connectorId"], str) and len(d["connectorId"]) >= 15
```

**生命周期验证标准模式（前置确认 → 操作 → 后置验证）：**
```python
# ✅ 标准生命周期测试 — 三步验证
def test_invalidate(self, published_connector):
    cid, vid = published_connector
    # ① 前置确认
    resp0 = api("GET", f"/connectors/{cid}/versions/{vid}")
    assert resp0.json()["data"].get("status") in (2, "2")  # 已发布
    # ② 执行操作
    resp = api("PUT", f"/connectors/{cid}/versions/{vid}/invalidate")
    assert resp.status_code == 200
    # ③ 后置验证状态变更
    resp2 = api("GET", f"/connectors/{cid}/versions/{vid}")
    assert resp2.json()["data"].get("status") in (3, "3")  # 已失效
```

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

### 条件保留测试数据

默认测试结束后自动清理 DB 数据。需要保留数据检查时设置 `KEEP_TEST_DATA=1`：

```bash
KEEP_TEST_DATA=1 pytest -m "L0 or L1 or L2"

# 跑完后 DB 中保留全部测试数据，可直接 SQL 查询验证
mysql -h... -e "SELECT name_cn, status FROM ... WHERE name_cn LIKE 'pytest%'"
```

所有 fixture 创建的 `name_cn` 包含测试函数名，可追溯数据来源：
```
pytest_invalidate / pytest_copy_to_draft / pytest_publish / ...
pytest_flow_invalidate / pytest_flow_publish / pytest_flow_start / ...
```

### 配置：单一入口

所有基础设施配置（数据库、Redis、测试应用、用户）集中在 `inspect/client.py`：

```python
TEST_APP_ID = "202606241730488926"
# 切换环境只需改这一个文件
```

## 用法

```bash
cd open-server/src/test/python

# 默认 L0 冒烟（6 个用例，秒级反馈）
pytest

# 指定层级
pytest -m L1                     # L1 核心 CRUD
pytest -m "L0 or L1"             # PR 门禁
pytest -m "not L4"               # 每日回归

# 指定文件
pytest test_connector_create.py -v

# 并行 + 报告
pytest -n auto --html=report.html

# 全量
pytest -m ""                     # 空字符串覆盖 -m L0 默认

# 保留数据用于调试
KEEP_TEST_DATA=1 pytest -m "L0 or L1 or L2"
```

## 目录

```
python/
├── README.md
├── pytest.ini                           ← 配置(markers/addopts)
├── conftest.py                          ← 共享 fixtures
├── test_connector_create.py             ← #1  POST /connectors
├── test_connector_list.py               ← #2  GET /connectors
├── test_connector_detail.py             ← #3  GET /connectors/{id}
├── test_connector_update.py             ← #4  PUT /connectors/{id}
├── test_connector_invalidate.py         ← #5  PUT /connectors/{id}/invalidate
├── test_connector_recover.py            ← #6  PUT /connectors/{id}/recover
├── test_connector_delete.py             ← #7  DELETE /connectors/{id}
├── test_connector_version_create.py     ← #8  POST /connectors/{id}/versions
├── test_connector_version_list.py       ← #9  GET /connectors/{id}/versions
├── test_connector_version_detail.py     ← #10 GET /connectors/{id}/versions/{vid}
├── test_connector_version_update.py     ← #11 PUT /connectors/{id}/versions/{vid}
├── test_connector_version_publish.py    ← #12 PUT /connectors/{id}/versions/{vid}/publish
├── test_connector_version_copy.py       ← #13 POST .../copy-to-draft
├── test_connector_version_invalidate.py ← #14 PUT .../invalidate
├── test_connector_version_recover.py    ← #15 PUT .../recover
├── test_connector_version_delete.py     ← #16 DELETE /connectors/{id}/versions/{vid}
├── test_flow_create.py                  ← #17 POST /flows
├── test_flow_list.py                    ← #18 GET /flows
├── test_flow_detail.py                  ← #19 GET /flows/{id}
├── test_flow_update.py                  ← #20 PUT /flows/{id}
├── test_flow_copy.py                    ← #21 POST /flows/{id}/copy
├── test_flow_deploy.py                  ← #22 POST /flows/{id}/deploy
├── test_flow_start.py                   ← #23 POST /flows/{id}/start
├── test_flow_stop.py                    ← #24 POST /flows/{id}/stop
├── test_flow_invalidate.py              ← #25 PUT /flows/{id}/invalidate
├── test_flow_recover.py                 ← #26 PUT /flows/{id}/recover
├── test_flow_delete.py                  ← #27 DELETE /flows/{id}
├── test_flow_version_create.py          ← #28 POST /flows/{id}/versions
├── test_flow_version_list.py            ← #29 GET /flows/{id}/versions
├── test_flow_version_detail.py          ← #30 GET /flows/{id}/versions/{vid}
├── test_flow_version_update.py          ← #31 PUT /flows/{id}/versions/{vid}
├── test_flow_version_publish.py         ← #32 POST /flows/{id}/versions/{vid}/publish
├── test_flow_version_copy.py            ← #33 POST .../copy-to-draft
├── test_flow_version_invalidate.py      ← #34 PUT .../invalidate
├── test_flow_version_recover.py         ← #35 PUT .../recover
├── test_flow_version_delete.py          ← #36 DELETE /flows/{id}/versions/{vid}
├── test_flow_version_debug.py           ← #51 POST .../debug
├── test_execution_records.py              ← #49~#50 运行记录列表/详情
├── test_approval_records.py               ← #39~#44 审批记录查询/批量
├── test_approval_flow_template.py         ← #45~#48 审批流模板 CRUD
├── test_data_processor_functions.py       ← #52 数据处理函数列表
├── test_flow_deploy_invoke.py           ← 部署→启动→调用 全链路
├── test_flow_stop_restart.py            ← 停止→重启 全链路
├── test_approval.py                     ← #37~#48 审批
├── test_security.py                     ← 白名单 + 操作日志
├── test_misc.py                         ← API/事件/回调删除 + JSON 校验
└── inspect/
    └── client.py                        ← 基础设施(api/db/redis/ok)
```
