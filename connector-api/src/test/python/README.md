# connector-api 集成测试

基于 pytest 的连接器平台 V3 运行时端到端集成测试。

## 设计思想

### 文件划分：按场景组织

connector-api 是运行时引擎，测试以**执行场景**为单位组织，而非按 API 端点：

| 文件 | 级别 | 覆盖场景 |
|------|:--:|------|
| **基础连通** | | |
| `test_health.py` | L0 | 服务健康检查 |
| **核心执行** | | |
| `inspect/test_trigger_invoke.py` | L1 | HTTP 触发 — 凭证/404/快乐路径/参数校验/下游失败/限流 (IT-049~065) |
| `inspect/test_internal_test_run.py` | L1 | 内部测试运行 — 不存在/未运行/正常/空参数 (IT-070~073) |
| `inspect/test_contract_response.py` | L1 | 响应契约 — v5.8 透明透传格式校验 |
| **安全与认证** | | |
| `inspect/test_connector_auth_multiple.py` | L2 | 多认证组合 — SOA/Cookie/DigitalSign (FR-012~014) |
| `inspect/test_systoken_whitelist.py` | L2 | SYSTOKEN 白名单 (FR-036) |
| **脚本与调试** | | |
| `inspect/test_script_node_execution.py` | L2 | GraalJS 脚本节点执行 + 超时 + 语法错误 (FR-040a) |
| `inspect/test_debug_draft_invoke.py` | L2 | 草稿版本直接调试 (FR-041) |
| **编排引擎** | | |
| `inspect/test_node_timeout.py` | L2 | 节点超时配置 (FR-034) |
| `inspect/test_flow_cache.py` | L2 | Flow 缓存 (FR-037) |
| `inspect/test_parallel_branch.py` | L2 | 并行/串行分支执行 (FR-038) |
| `inspect/test_connector_version_select.py` | L2 | 连接器版本选择 (FR-039) |
| **运行记录** | | |
| `inspect/test_execution_record_view.py` | L3 | 执行记录查看 (FR-042) |
| `inspect/test_execution_log.py` | L3 | 执行日志采集 + 脱敏 (FR-044) |
| `inspect/test_version_config_resolve.py` | L3 | 版本配置解析 (FR-043) |

### 用例分层：L0~L4 五级金字塔

每个用例标注优先级，默认全量运行：

```
         L0 (1)    ← 冒烟：服务连通
        L1 (3)     ← 核心执行：触发/测试运行/契约
        L2 (9)     ← 场景链路：认证/白名单/脚本/超时/缓存/分支
       L3 (3)      ← 运行记录：执行记录/日志/版本解析
      L4 (0)       ← 边界反向：预留（contract_response 已并入 L1）
```

| 级别 | 含义 | CI 触发 | 预期耗时 |
|:--:|------|------|:--:|
| L0 | 冒烟 — 服务连通 | 每次 commit | <5s |
| L1 | 核心执行 — 触发/测试运行/契约 | PR 门禁 | <30s |
| L2 | 场景链路 — 全功能场景覆盖 | 每日回归 | <2min |
| L3 | 运行记录 — 端到端数据链路 | 每周 | <2min |
| L4 | 边界反向 — 异常/极端场景 | 发布前 | <3min |

### 验证标准（禁止「假 OK」）

> ⚠️ **红线**：禁止仅断言 HTTP 200 而不验证业务数据。

| 操作类型 | 最少验证项 | 示例 |
|---------|-----------|------|
| **触发执行** | ① HTTP 状态码 ② X-Status 头 ③ 响应体 exit output 字段 | `check("HTTP 200", resp.status_code == 200)` + `check("searchKeyword 透传", body.get("searchKeyword") == "expr_test")` |
| **测试运行** | ① executionId 存在 ② steps 为数组 ③ 每 step 含 nodeId+durationMs | `check("executionId 为 string", isinstance(body.get("executionId"), str))` |
| **错误场景** | ① HTTP 状态码正确 ② X-Code 头匹配 ③ 响应体为空 | `check("HTTP 401", resp.status_code == 401)` + `check("响应体为空", len(resp.content) == 0)` |
| **DB 验证** | ① 数据写入成功 ② 字段值与输入一致 | `check("执行记录已写入", db_val("SELECT COUNT(*) FROM ...") is not None)` |

### fixtures：自动管理数据生命周期

```python
def test_trigger(connector, deployed_flow):
    resp = trigger_invoke(deployed_flow, body={...})
    assert resp.status_code == 200
```

| fixture | 提供 |
|---------|------|
| `connector` | 空连接器（无版本） |
| `published_connector` | 含已发布版本 + connectionConfig 的连接器 |
| `flow` | 空连接流 |
| `deployed_flow` | 已部署的连接流（含版本 + 简单 trigger→exit 编排） |

测试数据默认保留在数据库中，对齐 open-server 的行为。可通过 SQL 直接查询验证：

```bash
pytest -m "L0 or L1 or L2"

# 跑完后 DB 中保留全部测试数据，可直接查询
mysql -h 192.168.3.155 -u openapp -popenapp openapp \
  -e "SELECT name_cn, lifecycle_status FROM openplatform_v2_cp_flow_t WHERE app_id='202606241730488926'"
```

### 配置：单一入口

所有基础设施配置集中在 `inspect/client.py`：

```python
_DB = {"host": "192.168.3.155", "user": "openapp", "passwd": "openapp", "db": "openapp"}
_REDIS_CLUSTER = {"nodes": [...], "password": "openapp"}
BASE_URL = "http://localhost:18180/api/v1"
# 切换环境只需改这一个文件
```

## 用法

```bash
cd connector-api/src/test/python

# 默认全量运行
pytest

# 按级别运行
pytest -m L0                     # L0 冒烟（每次 commit）
pytest -m L1                     # L1 核心执行（PR 门禁）
pytest -m L2                     # L2 场景链路（每日回归）
pytest -m L3                     # L3 端到端（每周）

# 全量 + 报告
python3 inspect/all.py --report

# 安静模式
python3 inspect/all.py --quiet
```

## 目录

```
python/
├── README.md                    ← 本文档
├── pytest.ini                   ← 配置 (markers / addopts)
├── conftest.py                  ← 共享 fixtures
├── test_health.py               ← L0 冒烟：健康检查
└── inspect/
    ├── client.py                ← 基础设施 (api / db / db_val / snow_id / escape_sql / redis)
    ├── all.py                   ← 全量回归执行器
    ├── mock_server.py           ← 公共 Mock HTTP Server
    ├── test_trigger_invoke.py        ← HTTP 触发 (IT-049~065)
    ├── test_internal_test_run.py              ← 内部测试运行 (IT-070~073)
    ├── test_contract_response.py     ← 响应契约校验
    ├── test_connector_auth_multiple.py  ← 多认证组合
    ├── test_systoken_whitelist.py    ← SYSTOKEN 白名单
    ├── test_script_node_execution.py ← 脚本节点执行
    ├── test_debug_draft_invoke.py    ← 草稿调试
    ├── test_node_timeout.py          ← 节点超时
    ├── test_flow_cache.py            ← Flow 缓存
    ├── test_parallel_branch.py       ← 并行分支
    ├── test_connector_version_select.py ← 版本选择
    ├── test_execution_record_view.py ← 执行记录
    ├── test_execution_log.py         ← 执行日志
    └── test_version_config_resolve.py ← 版本解析
```

## 与 open-server 集成测试的关系

两个测试套件共享同一套基础设施设计理念，但测试性质不同：

| | open-server 测试 | connector-api 测试 |
|---|---|---|
| 性质 | REST 端点 CRUD | 运行时编排执行 |
| 粒度 | 一接口一文件 | 一场景一文件 |
| 框架 | pytest (全量) | inspect 脚本 + pytest (混合) |
| 共享层 | `client.py` (api/db/redis/ok) | `client.py` (request/db/snow_id/escape_sql/check) |

两者都遵循：
- 单一配置入口 (`client.py`)
- fixtures 自动管理数据生命周期
- L0~L4 五级金字塔分级
- 禁止「假 OK」验证标准
- 测试数据默认保留，可 SQL 直接验证
