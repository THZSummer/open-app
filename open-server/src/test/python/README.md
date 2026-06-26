# open-server 集成测试

基于 pytest 的连接器平台 V3 端到端集成测试，分为两大部分：
- **第一部分：单接口测试** — 每个 API 端点一个测试文件，L0~L4 分层
- **第二部分：全流程测试** — 一个脚本串联全部主流程

---

# 第一部分：单接口测试

### 设计思想

#### 文件划分：一个接口一个文件

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
| `test_flow_version_cancel.py` | #37 | POST /flows/{id}/versions/{vid}/cancel |
| `test_flow_version_urge.py` | #38 | POST /flows/{id}/versions/{vid}/urge |
| `test_flow_version_debug.py` | #51 | POST /flows/{id}/versions/{vid}/debug |
| **审批** | | |
| `test_approval_approve.py` | #41 | POST /approvals/{id}/approve |
| `test_approval_reject.py` | #42 | POST /approvals/{id}/reject |
| `test_approval_cancel.py` | #37,#43,#44 | POST .../cancel + /approvals/{id}/cancel |
| `test_approval_list.py` | #39 | GET /approvals/pending |
| `test_approval_detail.py` | #40 | GET /approvals/{id} |
| **审批流模板** | | |
| `test_approval_flow_list.py` | #45 | GET /approval-flows |
| `test_approval_flow_detail.py` | #46 | GET /approval-flows/{id} |
| `test_approval_flow_create.py` | #47 | POST /approval-flows |
| **运行记录** | | |
| `test_execution_list.py` | #49 | GET /executions |
| `test_execution_detail.py` | #50 | GET /executions/{id} |
| **端到端** | | |
| `test_flow_deploy_invoke.py` | – | 部署→启动→调用 全链路 |
| `test_flow_stop_restart.py` | – | 停止→重启 全链路 |
| **其他** | | |
| `test_approval.py` | #37~#48 | 审批流程 |
| `test_security.py` | – | 白名单准入 + 操作日志 |
| `test_misc.py` | – | API/事件/回调删除 + JSON 校验 |

#### 用例分层：L0~L4 五级金字塔

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

#### 验证标准（禁止「假 OK」）

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

#### fixtures：自动管理数据生命周期

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

#### 条件保留测试数据

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

#### 配置：单一入口

所有基础设施配置（数据库、Redis、测试应用、用户）集中在 `inspect/client.py`：

```python
TEST_APP_ID = "202606241730488926"
# 切换环境只需改这一个文件
```

### 用法

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

### 目录

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
├── test_flow_version_cancel.py          ← #37 POST .../cancel
├── test_flow_version_urge.py            ← #38 POST .../urge
├── test_flow_version_debug.py           ← #51 POST .../debug
├── test_execution_records.py              ← #49~#50 运行记录列表/详情
├── test_approval_records.py               ← #39~#44 审批记录查询/批量
├── test_approval_flow_template.py         ← #45~#48 审批流模板 CRUD
├── test_flow_deploy_invoke.py           ← 部署→启动→调用 全链路
├── test_flow_stop_restart.py            ← 停止→重启 全链路
├── test_approval.py                     ← #37~#48 审批
├── test_security.py                     ← 白名单 + 操作日志
├── test_misc.py                         ← API/事件/回调删除 + JSON 校验
└── inspect/
    └── client.py                        ← 基础设施(api/db/redis/ok)
```

---

# 第二部分：全流程测试

### 设计目标

模拟真实用户从零搭建连接器平台的完整流程。**所有写操作和状态流转仅通过 open-server / connector-api 接口完成，不允许直接写 DB。**

```
Phase 0: 环境准备(附)   → 重启 open-server + connector-api，确认就绪
Phase 1: 连接器发布       → create → draft → 配置Mock → publish
Phase 2: 连接流编排       → create → draft → 编排（引用连接器）
Phase 3: 草稿调试迭代     → debug draft（可多次，可穿插更新连接器版本）
Phase 4: 发布+审批        → publish → 两级审批通过（Cookie: user_id=tester）
Phase 5: 发布后验证       → debug 已发布版本
Phase 6: 部署+调用        → deploy → start → HTTP trigger → 查记录 → stop
```

### 核心约束

| 约束 | 说明 |
|------|------|
| 只调 API | **所有写操作和状态流转**通过 open-server (:18080) 或 connector-api (:18180) 接口 |
| 允许读 DB/Redis | 只读校验（状态确认、运行记录查询）可通过 MySQL/Redis 直接查 |
| 真实 Mock Server | 本地 HTTP Mock Server (port 18980)，连接器配置指向它 |
| 模拟用户迭代 | 先调试草稿 → 通过后发布 → 发布后再验证 → 最后部署调用 |
| 主流程不通 = 必须修 | 不设 skip/xfail，任何失败都是真实用户会遇到的 bug |
| 环境准备 | 每次运行前建议重启 open-server + connector-api 确保干净状态 |
| 审批走 API | approve 接口需携带 `Cookie: user_id=tester`（与审批流模板审批人一致） |

### Mock Server 设计

```
独立线程启动 http.server.HTTPServer (port 18980)
├── GET  /api/health          → {"status":"ok","server":"full-flow-mock"}
├── POST /api/echo            → {"code":0,"data":{"echo":<请求体>,"path":"/api/echo"}}
└── GET  /api/search?q=xxx    → {"code":0,"data":{"keyword":"xxx","results":[]}}
```

启动时机：Phase 1 开始前启动，Phase 6 结束后关闭。

### 全流程详细步骤

#### Phase 0: 环境准备 & 问题修复迭代入口

> 全流程测试过程中，任何步骤失败都意味着真实用户会遇到同样问题。
> 修复代码后，通过 Phase 0 重新编译重启，再跑全流程验证。循环直到全部通过。

**修复迭代循环：**

```
  ┌─────────────────────────────────────────┐
  │                                         │
  ▼                                         │
  Phase 0: 编译重启                          │
  ├── 0a. 重启 open-server                  │
  ├── 0b. 重启 connector-api                │
  └── 0c. 确认审批流模板存在 (DB 只读)        │
  │                                         │
  ▼                                         │
  Phase 1~6: 运行全流程测试                   │
  │                                         │
  ├── ✅ 全部通过 → 结束                     │
  └── ❌ 某步失败 → 定位代码问题 → 修复 ────┘
```

**重启命令：**

```
0a. 重启 open-server (管理服务, port 18080)
    cd /home/usb/wks/open-app && bash open-server/scripts/restart.sh
    → 等待 curl http://localhost:18080/open-server/actuator/health 返回 UP (最长 60s)

0b. 重启 connector-api (运行时, port 18180)
    cd /home/usb/wks/open-app && bash connector-api/scripts/restart.sh
    → 等待 curl http://localhost:18180/actuator/health 返回 UP (最长 60s)

0c. 确认审批流模板存在 (DB 只读)
    SQL: SELECT id FROM openplatform_v2_approval_flow_t WHERE code = 'connector_flow_version_publish'
    → 当前环境已预配置 (id=1, 审批人: tester)
    → 如不存在，需通过 API 创建: POST /approval-flows
```

> **注意**: restart.sh 内置 `mvn spring-boot:run`，会自动编译最新代码。修复代码后直接执行 restart.sh 即可。

#### Phase 1: 连接器发布 (全 API)

```
1.  CREATE 连接器
    POST /open-server/service/open/v2/connectors
    body: {nameCn:"全流程测试连接器", nameEn:"full-flow-connector", connectorType:1}
    → 断言: HTTP 200, data.status=1, nameCn 一致, connectorId 为雪花ID

2.  CREATE 草稿版本
    POST /open-server/service/open/v2/connectors/{cid}/versions
    body: {}
    → 断言: HTTP 200/201, data.versionNumber=1, status=1 (草稿)

3.  UPDATE 版本配置 (指向 Mock Server)
    PUT /open-server/service/open/v2/connectors/{cid}/versions/{vid}
    body: {
        connectionConfig: {
            protocol: "HTTP",
            protocolConfig: {url: "http://localhost:18980/api/echo", method: "POST"},
            timeoutMs: 5000
        }
    }
    → 断言: HTTP 200

4.  PUBLISH 版本
    PUT /open-server/service/open/v2/connectors/{cid}/versions/{vid}/publish
    → 断言: HTTP 200
    → 后置 GET 确认版本 status=2 (已发布)
```

#### Phase 2: 连接流编排 (全 API)

```
5.  CREATE 连接流
    POST /open-server/service/open/v2/flows
    body: {nameCn:"全流程测试连接流", nameEn:"full-flow-test"}
    → 断言: HTTP 200, data.lifecycleStatus=1 (stopped), flowId 为雪花ID

6.  CREATE 草稿版本
    POST /open-server/service/open/v2/flows/{fid}/versions
    body: {}
    → 断言: HTTP 200/201, data.versionNumber=1, status=1 (草稿)

7.  UPDATE 编排配置（引用 Phase 1 的连接器版本）
    PUT /open-server/service/open/v2/flows/{fid}/versions/{fvid}
    body: {
        orchestrationConfig: {
            trigger: {type:"http"},
            nodes: [
                {id:"trigger", type:"trigger", data:{}},
                {id:"conn1",  type:"connector", data:{connectorId:<cid>, connectorVersionId:<connVid>}},
                {id:"exit",   type:"exit", data:{outputMapping:{body:{echo:"{{conn1.data}}"}}}}
            ],
            edges: [
                {source:"trigger", target:"conn1"},
                {source:"conn1", target:"exit"}
            ]
        }
    }
    → 断言: HTTP 200
```

#### Phase 3: 草稿调试迭代 (全 API)

> 模拟用户行为：编排完草稿后立即调试，验证连通性。如果调试失败，可能回头改连接器配置重新发布，再回来调试。循环直到调试通过。

```
8.  DEBUG 草稿版本
    POST /open-server/service/open/v2/flows/{fid}/versions/{fvid}/debug
    body: {triggerData: {message:"hello-draft-debug"}}
    → 断言: HTTP 200, code="200"
    → 核心验证: 响应体中包含 Mock Server 回显的 echo 数据 (data.echo.message == "hello-draft-debug")
```

> **迭代预留**: 如果步骤 8 调试失败，说明连接器配置或编排有问题。此时可以：
> - 回到 Phase 1 更新连接器配置 → 创建新草稿 → 重新发布连接器版本
> - 回到 Phase 2 更新编排中引用的 connectorVersionId
> - 再次执行步骤 8 调试
>
> 脚本应支持这种迭代：调试失败时打印清晰错误信息，方便定位问题后修改再跑。

#### Phase 4: 发布 + 审批 (全 API)

> 调试通过后，用户提交发布 → 走两级审批。

```
9.  PUBLISH 版本（提交审批）
    POST /open-server/service/open/v2/flows/{fid}/versions/{fvid}/publish
    → 断言: HTTP 200/201, code="200"
    → 后置 GET 确认 version status=2 (待审批)

10. 查询审批记录
    GET /open-server/service/open/v2/approvals/pending?businessType=connector_flow_version_publish
    → 从列表中匹配 businessId={fvid} 的记录，获取 approvalId

11. 第一级审批 (scene — 平台连接流级)
    POST /open-server/service/open/v2/approvals/{approvalId}/approve
    Cookie: user_id=tester    ← 使用审批流模板中配置的审批人
    body: {comment: "场景级审批通过"}
    → 断言: HTTP 200

12. 第二级审批 (global — 全局级)
    POST /open-server/service/open/v2/approvals/{approvalId}/approve
    Cookie: user_id=tester    ← 同上
    body: {comment: "全局级审批通过"}
    → 断言: HTTP 200
    → 末级通过 → FlowVersion.status 自动变为 5 (已发布)

13. 验证版本已发布
    GET /open-server/service/open/v2/flows/{fid}/versions/{fvid}
    → 断言: status=5 (已发布)
```

> **审批引擎说明**: `connector_flow_version_publish` 使用 2 级审批模板（scene → global），approvalId 全程不变，每次 approve 后 currentNode 自动 +1。

#### Phase 5: 发布后验证 (全 API)

> 发布+审批全部通过后，再次调试确认已发布版本功能正常。

```
14. DEBUG 已发布版本
    POST /open-server/service/open/v2/flows/{fid}/versions/{fvid}/debug
    body: {triggerData: {message:"hello-after-approval"}}
    → 断言: HTTP 200, code="200"
    → 验证: 响应含 Mock 回显，与草稿调试结果一致
```

#### Phase 6: 部署 + 调用 (全 API)

> 全流程最后一环：部署上线 → 真实 HTTP 触发 → 验证运行记录。

```
15. DEPLOY 部署
    POST /open-server/service/open/v2/flows/{fid}/deploy
    body: {versionId: <fvid>}
    → 断言: HTTP 200

16. START 启动
    POST /open-server/service/open/v2/flows/{fid}/start
    → 断言: HTTP 200
    → 后置 GET 确认 lifecycleStatus=2 (running)

17. HTTP TRIGGER 真实调用
    POST http://localhost:18180/api/v1/flows/{fid}/invoke
    body: {message:"hello-from-production"}
    headers: {X-App-Id: <TEST_APP_ID>}
    → 断言: HTTP 200/201
    → 核心验证: 响应体包含 Mock 回显 → 全链路打通
    → 验证: X-Flow-Id / X-Execution-Id / X-Status 响应头存在

18. 查询运行记录 (DB 只读)
    SQL: SELECT status FROM execution_record_t WHERE flow_id={fid} ORDER BY create_time DESC LIMIT 1
    → 断言: 存在 success 记录

19. STOP 停止
    POST /open-server/service/open/v2/flows/{fid}/stop
    → 断言: HTTP 200
    → 后置 GET 确认 lifecycleStatus=1 (stopped)
```

### 关键断言矩阵

| # | API 操作 | 断言项 | FR |
|:--:|---------|--------|:--:|
| 0a | (可选) restart open-server | actuator/health UP (最长 60s) | – |
| 0b | (可选) restart connector-api | actuator/health UP (最长 60s) | – |
| 0c | (可选) DB SELECT 审批流模板 | id 存在 (code='connector_flow_version_publish') | – |
| 1 | POST /connectors | status==1, nameCn 一致, connectorId 雪花ID | FR-001 |
| 2 | POST /connectors/{id}/versions | versionNumber==1, status==1 | FR-005a |
| 4 | PUT .../publish | version status 1→2 (已发布) | FR-007 |
| 5 | POST /flows | lifecycleStatus==1, flowId 雪花ID | FR-016 |
| 6 | POST /flows/{id}/versions | versionNumber==1, status==1 | FR-024a |
| 7 | PUT .../versions/{vid} | HTTP 200 (编排写入成功) | – |
| 8 | POST .../debug (草稿) | code=="200", 响应含 Mock 回显 | FR-041 |
| 9 | POST .../publish | version status 1→2 (待审批) | FR-026 |
| 11 | POST /approvals/{id}/approve | L1 审批通过 | FR-031 |
| 12 | POST /approvals/{id}/approve | L2 通过 → status auto=5 | FR-031 |
| 13 | GET .../versions/{vid} | status==5 (已发布) | – |
| 14 | POST .../debug (已发布) | code=="200", 与草稿调试一致 | FR-041 |
| 15 | POST /flows/{id}/deploy | HTTP 200 | FR-018 |
| 16 | POST /flows/{id}/start | lifecycleStatus 1→2 (running) | FR-019 |
| 17 | POST connector-api /invoke | HTTP 200, 响应含 Mock 回显 | 全链路 |
| 18 | DB SELECT (只读) | 存在 success 记录 | FR-042 |
| 19 | POST /flows/{id}/stop | lifecycleStatus 2→1 (stopped) | FR-020 |

### 主流程不通 = 必须修

全流程测试不设 `skip` 或 `xfail`。任何步骤失败都意味着**用户在实际使用中会遇到相同问题**。

### 文件结构

```
open-server/src/test/python/
├── README.md              ← 本文档
├── test_full_flow.py      ← 【新增】全流程端到端测试
├── conftest.py            ← 共享 fixtures（复用现有）
├── _client.py             ← open-server API client（复用现有）
└── inspect/
    └── client.py          ← 基础设施（复用现有）
```

### 运行方式

```bash
cd open-server/src/test/python

# 单独运行全流程测试
pytest test_full_flow.py -v -s

# 保留测试数据用于人工检查
KEEP_TEST_DATA=1 pytest test_full_flow.py -v -s
```

### 数据清理

- 测试结束时自动清理所有 DB 数据（连接器/版本/连接流/版本/审批记录/运行记录）
- Mock Server 随测试结束自动停止
- 设置 `KEEP_TEST_DATA=1` 可保留全部数据用于手动排查
