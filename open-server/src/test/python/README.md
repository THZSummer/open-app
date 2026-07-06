# open-server 集成测试

基于 pytest 的连接器平台 V3 端到端集成测试，与源码目录结构对齐：

- **公共依赖** `common/` — 基础设施（API client、DB、fixtures）
- **单接口测试** `modules/` — 与源码 `modules/` 1:1 对应，L0~L4 分层
- **全流程测试** `e2e/` — 部署→启动→调用 端到端链路

## 快速开始

```bash
# L0 冒烟（每次 commit 必过，秒级反馈）
pytest

# PR 门禁
pytest -m "L0 or L1"

# 全量
pytest -m ""

# 按模块（与源码目录一一对应）
pytest modules/connector/              # 连接器
pytest modules/connectorversion/       # 连接器版本
pytest modules/flow/                   # 连接流
pytest modules/flowversion/            # 连接流版本
pytest modules/approval/               # 审批记录
pytest modules/approvalflow/           # 审批流模板
pytest modules/flowexecrecord/         # 运行记录
pytest e2e/                            # 全流程端到端
```

## 测试报告

```bash
pip install pytest-html

pytest -m "" --html=reports/report.html
echo "http://localhost:18900/report.html" && python3 -m http.server 18900 -d reports
```

---

## 目录结构

```
test/python/
├── README.md
├── pytest.ini                           # 配置 (markers / addopts / timeout)
├── conftest.py                          # session 级 fixture: lookup 初始化 + 共享 fixtures
├── .gitignore                           # __pycache__ / .pytest_cache / reports
├── common/                              # 公共依赖
│   ├── __init__.py                      # re-export: api, db, db_val, TEST_APP_ID
│   └── client.py                        # 基础设施 (api / db / 常量)
│
├── modules/                             # 与源码 modules/ 1:1 对应
│   ├── connector/                       # 对标 modules/connector
│   │   ├── test_create.py               # #1  POST /connectors
│   │   ├── test_list.py                 # #2  GET /connectors
│   │   ├── test_detail.py               # #3  GET /connectors/{id}
│   │   ├── test_update.py               # #4  PUT /connectors/{id}
│   │   ├── test_invalidate.py           # #5  PUT /connectors/{id}/invalidate
│   │   ├── test_recover.py              # #6  PUT /connectors/{id}/recover
│   │   └── test_delete.py               # #7  DELETE /connectors/{id}
│   │
│   ├── connectorversion/                # 对标 modules/connectorversion
│   │   ├── test_create.py               # #8  POST /connectors/{id}/versions
│   │   ├── test_list.py                 # #9  GET /connectors/{id}/versions
│   │   ├── test_detail.py               # #10 GET /connectors/{id}/versions/{vid}
│   │   ├── test_update.py               # #11 PUT /connectors/{id}/versions/{vid}
│   │   ├── test_publish.py              # #12 PUT /connectors/{id}/versions/{vid}/publish
│   │   ├── test_copy.py                 # #13 POST .../copy-to-draft
│   │   ├── test_invalidate.py           # #14 PUT .../invalidate
│   │   ├── test_recover.py              # #15 PUT .../recover
│   │   └── test_delete.py               # #16 DELETE /connectors/{id}/versions/{vid}
│   │
│   ├── flow/                            # 对标 modules/flow
│   │   ├── test_create.py               # #17 POST /flows
│   │   ├── test_list.py                 # #18 GET /flows
│   │   ├── test_detail.py               # #19 GET /flows/{id}
│   │   ├── test_update.py               # #20 PUT /flows/{id}
│   │   ├── test_copy.py                 # #21 POST /flows/{id}/copy
│   │   ├── test_deploy.py               # #22 POST /flows/{id}/deploy
│   │   ├── test_start.py                # #23 POST /flows/{id}/start
│   │   ├── test_stop.py                 # #24 POST /flows/{id}/stop
│   │   ├── test_invalidate.py           # #25 PUT /flows/{id}/invalidate
│   │   ├── test_recover.py              # #26 PUT /flows/{id}/recover
│   │   ├── test_delete.py               # #27 DELETE /flows/{id}
│   │   ├── test_deploy_invoke.py        # 部署→启动→调用 全链路
│   │   └── test_stop_restart.py         # 停止→重启 全链路
│   │
│   ├── flowversion/                     # 对标 modules/flowversion
│   │   ├── test_create.py               # #28 POST /flows/{id}/versions
│   │   ├── test_list.py                 # #29 GET /flows/{id}/versions
│   │   ├── test_detail.py               # #30 GET /flows/{id}/versions/{vid}
│   │   ├── test_update.py               # #31 PUT /flows/{id}/versions/{vid}
│   │   ├── test_publish.py              # #32 POST /flows/{id}/versions/{vid}/publish
│   │   ├── test_copy.py                 # #33 POST .../copy-to-draft
│   │   ├── test_invalidate.py           # #34 PUT .../invalidate
│   │   ├── test_recover.py              # #35 PUT .../recover
│   │   ├── test_delete.py               # #36 DELETE /flows/{id}/versions/{vid}
│   │   ├── test_cancel.py               # #37 POST .../cancel
│   │   ├── test_urge.py                 # #38 POST .../urge
│   │   └── test_debug.py                # #51 POST .../debug
│   │
│   ├── approval/                        # 对标 modules/approval
│   │   ├── test_approve.py              # #41 POST /approvals/{id}/approve
│   │   ├── test_reject.py               # #42 POST /approvals/{id}/reject
│   │   ├── test_cancel.py               # #43,#44 POST .../cancel
│   │   ├── test_list.py                 # #39 GET /approvals/pending
│   │   └── test_detail.py               # #40 GET /approvals/{id}
│   │
│   ├── approvalflow/                    # 对标 modules/approvalflow
│   │   ├── test_create.py               # #47 POST /approval-flows
│   │   ├── test_detail.py               # #46 GET /approval-flows/{id}
│   │   ├── test_list.py                 # #45 GET /approval-flows
│   │   └── test_update.py               # #48 PUT /approval-flows/{id}
│   │
│   └── flowexecrecord/                  # 对标 modules/flowexecrecord
│       ├── test_list.py                 # #49 GET /executions
│       └── test_detail.py               # #50 GET /executions/{id}
│
├── e2e/                                 # 全流程端到端（无源码模块对应）
│   ├── test_full_flow.py                # 串行编排全流程
│   ├── test_full_flow_parallel.py       # 并行编排全流程
│   ├── test_full_flow_script.py         # 脚本节点全流程
│   ├── test_full_flow_serial.py         # 串行多节点全流程
│   └── test_full_flow_single_node.py    # 单节点全流程
│
├── test_config_boundary.py              # 跨模块: 平台配置边界
├── test_misc.py                         # 跨模块: API/事件/回调删除 + JSON 校验
├── test_security.py                     # 跨模块: 白名单准入 + 操作日志
└── test_script_http_invoke.py           # 跨模块: 脚本 HTTP 调用
```

### 测试 ↔ 源码 1:1 映射

| 测试目录 | 源码模块 |
|----------|----------|
| `modules/connector/` | `src/main/.../modules/connector/` |
| `modules/connectorversion/` | `src/main/.../modules/connectorversion/` |
| `modules/flow/` | `src/main/.../modules/flow/` |
| `modules/flowversion/` | `src/main/.../modules/flowversion/` |
| `modules/approval/` | `src/main/.../modules/approval/` |
| `modules/approvalflow/` | `src/main/.../modules/approvalflow/` |
| `modules/flowexecrecord/` | `src/main/.../modules/flowexecrecord/` |

---

## 第一部分：单接口测试

### 文件划分：一个接口一个测试文件

目录与源码 `modules/` 严格对应，文件名只保留 action：

| 文件 | API # | 接口 |
|------|:--:|------|
| **连接器** | | |
| `modules/connector/test_create.py` | #1 | POST /connectors |
| `modules/connector/test_list.py` | #2 | GET /connectors |
| `modules/connector/test_detail.py` | #3 | GET /connectors/{id} |
| `modules/connector/test_update.py` | #4 | PUT /connectors/{id} |
| `modules/connector/test_invalidate.py` | #5 | PUT /connectors/{id}/invalidate |
| `modules/connector/test_recover.py` | #6 | PUT /connectors/{id}/recover |
| `modules/connector/test_delete.py` | #7 | DELETE /connectors/{id} |
| **连接器版本** | | |
| `modules/connectorversion/test_create.py` | #8 | POST /connectors/{id}/versions |
| `modules/connectorversion/test_list.py` | #9 | GET /connectors/{id}/versions |
| `modules/connectorversion/test_detail.py` | #10 | GET /connectors/{id}/versions/{vid} |
| `modules/connectorversion/test_update.py` | #11 | PUT /connectors/{id}/versions/{vid} |
| `modules/connectorversion/test_publish.py` | #12 | PUT /connectors/{id}/versions/{vid}/publish |
| `modules/connectorversion/test_copy.py` | #13 | POST .../copy-to-draft |
| `modules/connectorversion/test_invalidate.py` | #14 | PUT .../invalidate |
| `modules/connectorversion/test_recover.py` | #15 | PUT .../recover |
| `modules/connectorversion/test_delete.py` | #16 | DELETE /connectors/{id}/versions/{vid} |
| **连接流** | | |
| `modules/flow/test_create.py` | #17 | POST /flows |
| `modules/flow/test_list.py` | #18 | GET /flows |
| `modules/flow/test_detail.py` | #19 | GET /flows/{id} |
| `modules/flow/test_update.py` | #20 | PUT /flows/{id} |
| `modules/flow/test_copy.py` | #21 | POST /flows/{id}/copy |
| `modules/flow/test_deploy.py` | #22 | POST /flows/{id}/deploy |
| `modules/flow/test_start.py` | #23 | POST /flows/{id}/start |
| `modules/flow/test_stop.py` | #24 | POST /flows/{id}/stop |
| `modules/flow/test_invalidate.py` | #25 | PUT /flows/{id}/invalidate |
| `modules/flow/test_recover.py` | #26 | PUT /flows/{id}/recover |
| `modules/flow/test_delete.py` | #27 | DELETE /flows/{id} |
| **连接流版本** | | |
| `modules/flowversion/test_create.py` | #28 | POST /flows/{id}/versions |
| `modules/flowversion/test_list.py` | #29 | GET /flows/{id}/versions |
| `modules/flowversion/test_detail.py` | #30 | GET /flows/{id}/versions/{vid} |
| `modules/flowversion/test_update.py` | #31 | PUT /flows/{id}/versions/{vid} |
| `modules/flowversion/test_publish.py` | #32 | POST /flows/{id}/versions/{vid}/publish |
| `modules/flowversion/test_copy.py` | #33 | POST .../copy-to-draft |
| `modules/flowversion/test_invalidate.py` | #34 | PUT .../invalidate |
| `modules/flowversion/test_recover.py` | #35 | PUT .../recover |
| `modules/flowversion/test_delete.py` | #36 | DELETE /flows/{id}/versions/{vid} |
| `modules/flowversion/test_cancel.py` | #37 | POST .../cancel |
| `modules/flowversion/test_urge.py` | #38 | POST .../urge |
| `modules/flowversion/test_debug.py` | #51 | POST .../debug |
| **审批记录** | | |
| `modules/approval/test_approve.py` | #41 | POST /approvals/{id}/approve |
| `modules/approval/test_reject.py` | #42 | POST /approvals/{id}/reject |
| `modules/approval/test_cancel.py` | #43,#44 | POST .../cancel |
| `modules/approval/test_list.py` | #39 | GET /approvals/pending |
| `modules/approval/test_detail.py` | #40 | GET /approvals/{id} |
| **审批流模板** | | |
| `modules/approvalflow/test_create.py` | #47 | POST /approval-flows |
| `modules/approvalflow/test_detail.py` | #46 | GET /approval-flows/{id} |
| `modules/approvalflow/test_list.py` | #45 | GET /approval-flows |
| `modules/approvalflow/test_update.py` | #48 | PUT /approval-flows/{id} |
| **运行记录** | | |
| `modules/flowexecrecord/test_list.py` | #49 | GET /executions |
| `modules/flowexecrecord/test_detail.py` | #50 | GET /executions/{id} |
| **全流程** | | |
| `modules/flow/test_deploy_invoke.py` | – | 部署→启动→调用 |
| `modules/flow/test_stop_restart.py` | – | 停止→重启 |
| **跨模块** | | |
| `e2e/test_full_flow*.py` | – | 编排全流程 E2E |
| `test_config_boundary.py` | – | 平台配置边界 |
| `test_security.py` | – | 白名单准入 + 操作日志 |
| `test_misc.py` | – | API/事件/回调删除 + JSON 校验 |

### 用例分层：L0~L4 五级金字塔

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
| L1 | 核心 CRUD — 增删改查 | PR 门禁 | <30s |
| L2 | 生命周期 — 状态流转/部署/停止 | 每日回归 | <1min |
| L3 | 端到端 — 部署→启动→调用→记录 | 每周 | <2min |
| L4 | 边界反向 — 异常/校验/极端场景 | 发布前 | <3min |

### 验证标准（禁止「假 OK」）

> 红线：禁止仅断言 `status_code == 200` 而不验证业务数据。

| 操作类型 | 最少验证项 |
|---------|-----------|
| CREATE | 返回字段与输入一致 + 业务状态正确 + BIGINT ID 为 string |
| GET 详情 | 资源 ID 与请求匹配 + 关键业务字段存在 |
| LIST | `data` 为数组 + 分页参数一致 + 列表项含关键字段 |
| UPDATE | 重新 GET 验证变更已持久化 |
| DELETE | 验证资源已不可访问 |
| 生命周期 | 前置状态正确 + 操作后状态变更符合规范 |

### fixtures：自动管理数据生命周期

| fixture | 提供 |
|---------|------|
| `connector` | 空连接器（无版本） |
| `draft_connector` | 含草稿版本的连接器 |
| `published_connector` | 含已发布版本的连接器 |
| `flow` | 空连接流 |
| `draft_flow` | 含草稿版本的连接流 |
| `deployed_flow` | 已部署的连接流（含版本 + deployed 指针） |

### 配置：单一入口

所有基础设施配置集中在 `common/client.py`：

```python
TEST_APP_ID = "20250730213114178360970"
# 切换环境只需改这一个文件
```

### 用法

```bash
cd open-server/src/test/python

# 默认 L0 冒烟
pytest

# 按模块
pytest modules/connector/
pytest modules/flow/
pytest modules/approval/
pytest e2e/

# 指定层级
pytest -m L1                  # L1 核心 CRUD
pytest -m "L0 or L1"          # PR 门禁
pytest -m "not L4"            # 每日回归

# 指定文件
pytest modules/connector/test_create.py -v

# 全量
pytest -m ""

# 保留数据用于调试
KEEP_TEST_DATA=1 pytest -m "L0 or L1 or L2"
```

---

## 第二部分：全流程测试

### 设计目标

模拟真实用户从零搭建连接器平台的完整流程。所有写操作和状态流转仅通过 open-server / connector-api 接口完成。

```
Phase 0: 环境准备       → 重启 open-server + connector-api，确认就绪
Phase 1: 连接器发布     → create → draft → 配置 Mock → publish
Phase 2: 连接流编排     → create → draft → 编排（引用连接器）
Phase 3: 草稿调试迭代   → debug draft
Phase 4: 发布+审批      → publish → 两级审批通过
Phase 5: 发布后验证     → debug 已发布版本
Phase 6: 部署+调用      → deploy → start → HTTP trigger → 查记录 → stop
```

### 核心约束

| 约束 | 说明 |
|------|------|
| 只调 API | 所有写操作和状态流转通过 open-server (:18080) 或 connector-api (:18180) |
| 允许读 DB/Redis | 只读校验可通过 MySQL/Redis 直接查 |
| 真实 Mock Server | 本地 HTTP Mock Server，各测试独立端口避免冲突 |
| 主流程不通 = 必须修 | 不设 skip/xfail |

### 运行方式

```bash
cd open-server/src/test/python

# 单独跑
pytest e2e/test_full_flow.py -v -s

# 全量 e2e
pytest e2e/ -v -s

# 保留测试数据
KEEP_TEST_DATA=1 pytest e2e/test_full_flow.py -v -s
```

### 数据清理

- 测试结束时自动清理所有 DB 数据（连接器/版本/连接流/版本/审批记录/运行记录）
- Mock Server 随测试结束自动停止
- 设置 `KEEP_TEST_DATA=1` 可保留全部数据用于手动排查
