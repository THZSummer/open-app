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

# 按模块
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
```

---

## 目录结构

```
test/python/
├── pytest.ini               # 配置 (markers / addopts / timeout)
├── conftest.py              # session fixture + 共享 fixtures
├── .gitignore
│
├── common/                  # 公共依赖
│   ├── __init__.py          # re-export: api, db, db_val, TEST_APP_ID
│   └── client.py            # 基础设施 (api / db / 常量)
│
├── modules/                 # 与源码 modules/ 一一对应，每个接口一个 test_{action}.py
│   ├── connector/           # → modules/connector/
│   ├── connectorversion/    # → modules/connectorversion/
│   ├── flow/                # → modules/flow/
│   ├── flowversion/         # → modules/flowversion/
│   │   ├── test_create.py   #   POST /flows/{id}/versions
│   │   ├── test_publish.py  #   POST .../publish
│   │   ├── test_debug.py    #   POST .../debug
│   │   └── ...              #   其余接口同理
│   ├── approval/            # → modules/approval/
│   ├── approvalflow/        # → modules/approvalflow/
│   └── flowexecrecord/      # → modules/flowexecrecord/
│
├── e2e/                     # 全流程端到端
│   └── test_full_flow*.py
│
├── test_config_boundary.py  # 跨模块: 平台配置边界
├── test_misc.py             # 跨模块: 删除 + JSON 校验
├── test_security.py         # 跨模块: 白名单 + 操作日志
└── test_script_http_invoke.py
```

**规则**：模块目录下每个接口一个 `test_{action}.py`，文件名去掉模块前缀。例如 `modules/connector/` 包含 `test_create.py`、`test_list.py` 等。

---

## 用例分层：L0~L4

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

---

## 验证标准

> 红线：禁止仅断言 `status_code == 200` 而不验证业务数据。

| 操作类型 | 最少验证项 |
|---------|-----------|
| CREATE | 返回字段与输入一致 + 业务状态正确 + BIGINT ID 为 string |
| GET 详情 | 资源 ID 与请求匹配 + 关键业务字段存在 |
| LIST | `data` 为数组 + 分页参数一致 |
| UPDATE | 重新 GET 验证变更已持久化 |
| DELETE | 验证资源已不可访问 |
| 生命周期 | 前置状态正确 → 操作 → 后置状态变更符合规范 |

---

## fixtures

| fixture | 提供 |
|---------|------|
| `connector` | 空连接器（无版本） |
| `draft_connector` | 含草稿版本的连接器 |
| `published_connector` | 含已发布版本的连接器 |
| `flow` | 空连接流 |
| `draft_flow` | 含草稿版本的连接流 |
| `deployed_flow` | 已部署的连接流 |

所有 fixture 创建的数据在测试结束后自动清理，设置 `KEEP_TEST_DATA=1` 保留：

```bash
KEEP_TEST_DATA=1 pytest -m "L0 or L1 or L2"
```

---

## 配置

所有基础设施配置集中在 `common/client.py`：`TEST_APP_ID`、数据库连接、Redis 节点等。切换环境只需改这一个文件。

---

## 全流程测试

模拟真实用户从零搭建连接器平台的完整流程，所有写操作仅通过 open-server / connector-api 接口。

```
Phase 0: 环境准备       → 重启 open-server + connector-api
Phase 1: 连接器发布     → create → draft → 配置 Mock → publish
Phase 2: 连接流编排     → create → draft → 编排
Phase 3: 草稿调试迭代   → debug draft
Phase 4: 发布+审批      → publish → 两级审批通过
Phase 5: 发布后验证     → debug 已发布版本
Phase 6: 部署+调用      → deploy → start → invoke → 查记录 → stop
```

### 运行

```bash
cd open-server/src/test/python

pytest e2e/test_full_flow.py -v -s
pytest e2e/ -v -s                                      # 全量

KEEP_TEST_DATA=1 pytest e2e/test_full_flow.py -v -s   # 保留数据
```
