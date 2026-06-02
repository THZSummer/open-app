# 🔍 测试方案（集成）：连接器平台 — Python 真调接口测试

**Feature ID**: CONN-PLAT-001  
**创建日期**: 2026-05-25  
**对齐基线**: spec.md v5.0 / plan.md v2.8.1 / plan-api.md v2.8.0  
**测试范围**: 后端 API #1~#18（open-server 17 个 + connector-api 1 个）  
**测试类型**: 🟢 **L3 集成测试** — 真实启动服务 + 真实数据库 + 真实 HTTP 调用  
**测试工具**: Python + requests（参考现有 `test/scripts/test-all-apis-optimized.sh` 模式）

---

## 一、为什么需要这套测试？

### 现有测试的不足

| 测试类型 | 已有方案 | 局限 |
|---------|---------|------|
| L1 单元测试 | 79 个 Mockito 测试 | 不验证 HTTP 序列化、不验证数据库 |
| L2 MockMvc 测试 | 68 个 `@WebMvcTest` 测试 | **仍为 Mock**，Service 层被 mock，不连真实 DB |

### Python 真调测试的补充价值

| 验证维度 | MockMvc 测不到 | Python 真调能测 |
|---------|---------------|----------------|
| 数据库 SQL 正确性 | ❌ Service 被 mock | ✅ 真实 INSERT/SELECT/UPDATE/DELETE |
| 事务完整性 | ❌ 无真实事务 | ✅ 提交/回滚行为 |
| 雪花 ID 生成 | ❌ 硬编码 | ✅ 真实数据库自增/ID 生成 |
| Redis 缓存 | ❌ 无 Redis | ✅ 缓存命中/穿透 |
| 枚举值与数据库一致 | ❌ Mock 返回任意值 | ✅ 数据库真实枚举值 |
| 认证/权限拦截器 | ❌ 无安全上下文 | ✅ 真实拦截器过滤 |
| 参数校验(@Valid) | ⚠️ 部分可测 | ✅ 完整校验链路 |

---

## 二、测试策略

### 2.1 测试层次

| 层次 | 目标 | 工具 | 状态 |
|:----:|------|------|:----:|
| **L3 集成测试** | 全链路验证（HTTP → Controller → Service → DB） | Python + requests + PyMySQL | **⭐ 本次新增** |

### 2.2 技术选型

| 组件 | 选型 | 理由 |
|------|------|------|
| 脚本语言 | **Python 3** + `requests` | 系统自带，零安装零依赖 |
| 接口文件 | 每个接口一个独立 `.py` 文件 | 文件名即接口名，一看就懂，直接 `python3 xxx.py` 执行 |
| 公共模块 | `client.py` | 统一 BASE_URL、请求发送、响应打印 |
| 输出控制 | `--quiet` 参数 | 默认打印完整请求/响应；`--quiet` 只输出 PASS/FAIL |
### 2.3 数据策略

| 阶段 | 操作 | 说明 |
|------|------|------|
| 数据准备 (setup) | INSERT 测试数据 | 使用时间戳后缀避免冲突，记录插入的 ID |
| 用例执行 | 逐个接口调用 | 每个用例独立验证，用例间可能依赖（如先创建再查询）|
| 数据清理 (teardown) | DELETE 按 ID 清理 | 确保可重复执行，不污染数据库 |

---

## 三、文件结构

### open-server（#1~#17 + L4 契约）

```
open-server/src/test/python/
└── inspect/
    ├── client.py                   ← 公共模块：BASE_URL (localhost:18080) + 请求/响应打印
    ├── connector_create.py         ← 创建连接器
    ├── connector_list.py           ← 查询连接器列表
    ├── connector_detail.py         ← 查询连接器详情
    ├── connector_update.py         ← 更新连接器
    ├── connector_delete.py         ← 删除连接器
    ├── connector_config_get.py     ← 获取连接配置
    ├── connector_config_set.py     ← 编辑连接配置
    ├── flow_create.py              ← 创建连接流
    ├── flow_list.py                ← 查询流列表
    ├── flow_detail.py              ← 查询流详情
    ├── flow_update.py              ← 更新流
    ├── flow_delete.py              ← 删除流
    ├── flow_start.py               ← 启动流
    ├── flow_stop.py                ← 停止流
    ├── flow_config_get.py          ← 获取编排配置
    ├── flow_config_set.py          ← 保存编排配置
    ├── debug_test_run.py           ← 测试运行
    ├── contract_response.py        ← L4 响应格式校验
    └── all.py                      ← 全量回归执行器
```

### connector-api（5 个文件）

```
connector-api/src/test/python/
└── inspect/
    ├── client.py                   ← 公共模块：BASE_URL (localhost:18180) + 请求/响应打印 + PASS/FAIL 判断
    ├── trigger_invoke.py           ← HTTP 触发全部场景（IT-049~051 + IT-060~061 + IT-064~065）
    ├── test_run.py                 ← 内部测试运行（IT-070~073）[NEW]
    ├── contract_response.py        ← L4 + 执行响应格式校验（IT-052~059 + IT-066~068）
    └── all.py                      ← 全量回归执行器
```

各文件独立可执行，文件名即操作名：
- `connector_create.py` = 创建连接器
- `trigger_invoke.py` = HTTP 触发
- 不需要记编号，文件名就是接口名


---


## 四、测试用例（按接口分组）

### 4.1 连接器 CRUD — ConnectorController（#1~#5）

#### #1 POST /service/open/v2/connectors — 创建连接器

| 编号 | 场景 | 输入 | 预期 | 验证点 |
|------|------|------|:----:|--------|
| IT-001 | ✅ 正常创建（完整字段） | nameCn/nameEn/iconFileId/descriptionCn/descriptionEn/connectorType=1 | 200 | `data.id` 为字符串；`createTime` ISO 8601 |
| IT-002 | ❌ 缺少必填 nameCn | body 无 nameCn | 400 | `messageZh` 含"参数错误" |
| IT-003 | ❌ connectorType 非法 | connectorType=99 | 422 | 校验失败提示 |
| IT-004 | ❌ nameCn 超长（>500 字符） | 超长字符串 | 422 | 字段长度校验 |

#### #2 GET /service/open/v2/connectors — 查询列表

| 编号 | 场景 | 输入 | 预期 | 验证点 |
|------|------|------|:----:|--------|
| IT-005 | ✅ 默认分页 | 无参数 | 200 | `page.curPage=1`、`pageSize=20`、`data` 为数组 |
| IT-006 | ✅ connectorType 过滤 | `?connectorType=1` | 200 | 仅返回 type=1 |
| IT-007 | ✅ keyword 搜索 | `?keyword=测试` | 200 | nameCn/nameEn 模糊匹配 |
| IT-008 | ✅ 自定义分页 | `?curPage=2&pageSize=10` | 200 | 分页参数正确 |
| IT-009 | ✅ 空结果 | `?keyword=NONEXISTENT_XYZ` | 200 | `data=[]`, `page.total=0` |

#### #3 GET /service/open/v2/connectors/{connectorId} — 查询详情

| 编号 | 场景 | 预期 | 验证点 |
|------|------|:----:|--------|
| IT-010 | ✅ 正常查询 | 200 | 返回完整字段：nameCn/nameEn/connectorType/createTime/lastUpdateTime |
| IT-011 | ❌ connectorId 不存在 | 404 | 资源不存在提示 |
| IT-012 | ✅ 雪花 ID 为 string 类型 | 200 | JSON 中 id 为字符串非数字 |

#### #4 PUT /service/open/v2/connectors/{connectorId} — 更新

| 编号 | 场景 | 预期 |
|------|------|:----:|
| IT-013 | ✅ 正常更新（nameCn） | 200 |
| IT-014 | ❌ 不存在的连接器 | 404 |

#### #5 DELETE /service/open/v2/connectors/{connectorId} — 删除

| 编号 | 场景 | 预期 |
|------|------|:----:|
| IT-015 | ✅ 正常删除（刚创建，无引用） | 200 |
| IT-016 | ❌ 不存在的连接器 | 404 |

---

### 4.2 连接器配置 — ConnectorController（#6~#7）

#### #6 GET /service/open/v2/connectors/{connectorId}/config — 获取连接配置

| 编号 | 场景 | 预期 |
|------|------|:----:|
| IT-017 | ✅ 已配置 | 200, `hasConfig=true`, `connectionConfig` 非空 |
| IT-018 | ✅ 未配置（新建连接器的初始状态） | 200, `hasConfig=false` |
| IT-019 | ❌ 连接器不存在 | 404 |

#### #7 PUT /service/open/v2/connectors/{connectorId}/config — 编辑连接配置

| 编号 | 场景 | 预期 |
|------|------|:----:|
| IT-020 | ✅ 正常编辑 | 200 |
| IT-021 | ❌ connectionConfig 为空字符串 | 400 |
| IT-022 | ❌ connectionConfig 为 null | 400 |

---

### 4.3 连接流 CRUD — FlowController（#8~#14）

#### #8 POST /service/open/v2/flows — 创建连接流

| 编号 | 场景 | 预期 |
|------|------|:----:|
| IT-023 | ✅ 正常创建 | 200, `data.id` 字符串, `lifecycleStatus=0` |
| IT-024 | ❌ 缺少必填 nameCn | 400 |
| IT-025 | ❌ 缺少必填 nameEn | 400 |

#### #9 GET /service/open/v2/flows — 查询流列表

| 编号 | 场景 | 预期 |
|------|------|:----:|
| IT-026 | ✅ 默认分页 | 200, data 数组 |
| IT-027 | ✅ lifecycleStatus 过滤 | 200 |
| IT-028 | ✅ keyword 搜索 | 200 |
| IT-029 | ✅ 空结果 | data=[], total=0 |

#### #10 GET /service/open/v2/flows/{flowId} — 查看流详情

| 编号 | 场景 | 预期 |
|------|------|:----:|
| IT-030 | ✅ 正常 | 200, 完整字段 |
| IT-031 | ❌ 不存在 | 404 |

#### #11 PUT /service/open/v2/flows/{flowId} — 更新流信息

| 编号 | 场景 | 预期 |
|------|------|:----:|
| IT-032 | ✅ 正常更新 | 200 |
| IT-033 | ❌ 不存在 | 404 |

#### #12 DELETE /service/open/v2/flows/{flowId} — 删除流

| 编号 | 场景 | 预期 |
|------|------|:----:|
| IT-034 | ✅ stopped 状态可删除 | 200 |
| IT-035 | ❌ 不存在 | 404 |

#### #13 POST /service/open/v2/flows/{flowId}/start — 启动

| 编号 | 场景 | 预期 |
|------|------|:----:|
| IT-036 | ✅ stopped → running | 200 |
| IT-037 | ❌ 已是 running（重复） | 409 |
| IT-038 | ❌ 不存在 | 404 |

#### #14 POST /service/open/v2/flows/{flowId}/stop — 停止

| 编号 | 场景 | 预期 |
|------|------|:----:|
| IT-039 | ✅ running → stopped | 200 |
| IT-040 | ❌ 已是 stopped | 409 |
| IT-041 | ❌ 不存在 | 404 |

---

### 4.4 连接流配置 — FlowController（#15~#16）

#### #15 GET /service/open/v2/flows/{flowId}/config — 获取编排配置

| 编号 | 场景 | 预期 |
|------|------|:----:|
| IT-042 | ✅ 空配置（初始） | 200, `hasConfig=false` |
| IT-043 | ❌ flow 不存在 | 404 |

#### #16 PUT /service/open/v2/flows/{flowId}/config — 保存编排配置

| 编号 | 场景 | 预期 |
|------|------|:----:|
| IT-044 | ✅ 正常保存 | 200 |
| IT-045 | ❌ orchestrationConfig 为空字符串 | 400 |
| IT-046 | ❌ orchestrationConfig 为 null | 400 |

---

### 4.5 测试代理 — DebugProxyController（#17）

#### #17 POST /service/open/v2/flows/{flowId}/test-run — 测试运行

| 编号 | 场景 | 预期 |
|------|------|:----:|
| IT-047 | ❌ flow 不存在 | 404 |
| IT-048 | ❌ flow 未配置编排 | 422 |

---

### 4.6 HTTP 触发 — TriggerController（#18）

#### #18 POST /api/v1/trigger/{flowId}/invoke — HTTP 触发

| 编号 | 场景 | 输入/条件 | 预期 | 验证点 |
|------|------|----------|:----:|--------|
| IT-049 | ❌ 凭证缺失（无 X-Sys-Token） | 无 Header | 401 | `code="401"`, `data: null` |
| IT-050 | ❌ flow 不存在 | flowId=999... | 404 | `code="404"`, `data: null` |
| IT-051 | ❌ flow 未运行（stopped） | lifecycleStatus=0 | 403 | `code="403"`, `data: null` |
| IT-060 | ✅ 正常同步执行（entry→exit，无连接器） | 构造完整 flow + flow_version 数据；触发请求体合法 | 200 | `data.executionId` 为 string；`data.status` 为 int；`data.durationMs` 为 int > 0 |
| IT-061 | ❌ 触发请求体不符合 inputContract | 请求体缺少必填字段 | 422/400 | 校验失败提示 |
| IT-064 | ❌ 超过限流阈值 → 429 | 并发发送 10+ 请求，超过 maxQps=5 | 429 | `code="429"`; `messageZh="请求频率超限"`; `messageEn="Too many requests"` |
| IT-065 | ✅ 限流阈值内正常执行 | 单次请求，低于 maxQps | 200 | 正常返回执行结果 |

> 💡 **数据准备**：脚本内生成 snow_id → INSERT flow_t（lifecycleStatus=1）→ INSERT flow_version_t（orchestration_config 含 entry→exit 简单编排 + rateLimitConfig.maxQps）→ 发起触发 → 验证响应 → 清理数据。

---

### 4.7 L4 契约测试（通用响应格式）

| 编号 | 场景 | 验证点 |
|------|------|--------|
| IT-052 | ✅ 成功响应格式 | `{code, messageZh, messageEn, data}` |
| IT-053 | ✅ 错误响应格式 | `code != "200"`, `data: null` |
| IT-054 | ✅ 分页响应格式 | `page{curPage, pageSize, total}` |
| IT-055 | ✅ BIGINT ID 为 string 类型 | 所有 id 字段 JSON 类型为 string |
| IT-056 | ✅ 枚举为 TINYINT 数字 | connectorType/lifecycleStatus 等为 int |
| IT-057 | ✅ 时间为 ISO 8601 格式 | createTime/lastUpdateTime 格式校验 |
| IT-058 | ✅ camelCase 字段命名 | 字段名正则 `^[a-z]+[A-Za-z0-9]*$` |
| IT-059 | ✅ 错误码覆盖率 | 400/401/403/404/409/422/429/500 |

---

### 4.8 测试运行内部端点 — TestRunController

> ⚠️ 注：本端点为 connector-api 内部接口（`POST /api/v1/internal/test-run/{flowId}`），供 open-server 的 debug-proxy 转发调用。open-server 侧的 IT-047~048 测试的是转发链路，本部分直接测试 connector-api 内部端点。

| 编号 | 场景 | 条件 | 预期 | 验证点 |
|------|------|------|:----:|--------|
| IT-070 | ❌ flow 不存在 | flowId=999... | 404 | `code="404"`, `data: null` |
| IT-071 | ❌ flow 未运行（stopped） | lifecycleStatus=0 | 403 | `code="403"`, `data: null` |
| IT-072 | ✅ 正常测试运行（entry→exit） | 构造完整 flow 数据，传入 mockTriggerData | 200 | `data.executionId` 为 string；`data.isTest=true`；`data.triggerType=3`；`data.steps[]` 数组含 nodeId/nodeType/status/durationMs |
| IT-073 | ✅ 空 mockTriggerData | `mockTriggerData: {}` | 200 | 正常返回 |

> 💡 **数据准备**：同 IT-060 模式，脚本内生成 snow_id → INSERT flow_t/flow_version_t → 发起请求 → 验证 → 清理。

---

### 4.9 响应格式校验（connector-api 专用）

| 编号 | 场景 | 验证点 |
|------|------|--------|
| IT-066 | ✅ 执行成功响应格式 | `data.executionId` 为 string；`data.status` 为 int（TINYINT）；`data.resultData` 为 object；`data.durationMs` 为 int > 0 |
| IT-067 | ✅ 执行失败 errorInfo 格式 | `data` 为 null；`code` 为数字字符串；`messageZh`/`messageEn` 非空；oneOf 约束（6xxxx 含 `cause` / 4xx/5xx 含 `downstreamStatus`）|
| IT-068 | ✅ 限流拒绝 errorInfo 格式 | `code="429"`；`messageZh="请求频率超限"`；`messageEn="Too many requests"`；`data: null` |

---

## 五、使用方式

### 前置依赖

| 依赖 | 版本要求 | 安装方式 |
|------|---------|---------|
| Python 3 | >= 3.10 | 系统自带 |
| requests | 任意版本 | 系统已安装（`python3 -c "import requests"` 验证） |
| open-server | — | 已启动在 `:18080` |
| connector-api | — | 已启动在 `:18180`（可选，未启动时触发测试自动跳过） |

**零安装、零依赖** — 不需要创建 venv，不需要 pip install。

### 单接口调试

每个接口一个文件，运行后默认打印完整请求/响应：

```bash
# open-server 接口
cd open-server/src/test/python

# 创建连接器（显示完整请求 Header/Body + 响应 Header/Body/状态码）
python3 inspect/connector_create.py

# 查询连接器列表
python3 inspect/connector_list.py

# 启动连接流
python3 inspect/flow_start.py

# 静默模式（只输出 PASS/FAIL 一行）
python3 inspect/connector_create.py --quiet

# connector-api 接口
cd connector-api/src/test/python
python3 inspect/trigger_invoke.py
```

输出示例（默认）：
```
============================================================
🟢 请求
  POST http://localhost:18080/open-server/service/open/v2/connectors
  Headers: Content-Type: application/json
  Body: {"nameCn":"IM消息","nameEn":"IM Send Message","connectorType":1}
============================================================
🟢 响应 200
  Headers: Content-Type: application/json
  Body: {"code":"200","data":{"id":"317311223559356416"},"messageZh":"操作成功"}
============================================================
✅ PASS (0.23s)
```

### 全量回归

```bash
# 全部 59 个用例，每个显示详细请求/响应
python3 inspect/all.py

# 全部 59 个用例，只输出摘要
python3 inspect/all.py --quiet
```

全量回归自动执行顺序：
1. 连接器 CRUD + 配置（IT-001~022）
2. 连接流 CRUD + 启停 + 配置（IT-023~046）
3. 调试代理（IT-047~048）
4. HTTP 触发（IT-049~051，connector-api 未启动时跳过）
5. 契约校验（IT-052~059）

默认输出每条用例的完整交互详情。`--quiet` 模式汇总输出：
```
✅✅✅✅✅...✅✅ (59/59 PASS, 0 FAIL, 0 SKIP)
```

### 自动化测试 + 报告生成

```bash
# 全量回归 + 生成测试报告
python3 inspect/all.py --report

# 输出会同时写入 test-report-integration.md
```

### 清理

全量回归和单接口调试均使用独立的雪花 ID，测试数据自动清理。也可以通过以下命令清理残留：

```bash
mysql -u openapp -popenapp openapp -e "
DELETE FROM openplatform_v2_cp_connector_t WHERE name_cn LIKE '%IT_%';
DELETE FROM openplatform_v2_cp_flow_t WHERE name_cn LIKE '%IT_%';
"
```

---

## 六、与现有测试的关系

| 维度 | 已有 Java 测试（test-plan.md） | inspect 真调测试（本方案） |
|------|-------------------------------|--------------------------|
| 测试层次 | L2 接口层（MockMvc） | L3 集成测试 |
| 依赖 | 无（全 Mock） | 需 MySQL + Redis + 服务启动 |
| 执行速度 | 毫秒级 | 秒级 |
| 使用方式 | IDE 运行 JUnit | `python3 inspect/xxx.py` 命令行 |
| 输出 | IDE 测试报告 | 完整请求/响应（默认）/ PASS/FAIL（--quiet）|
| 文件位置 | `src/test/java/` | `src/test/python/inspect/` |
| 工具 | JUnit + MockMvc + Mockito | Python 3 + requests（零安装）|

---

## 八、文件清单

### open-server（20 个文件）

| # | 文件名 | 覆盖接口 | 说明 |
|---|--------|---------|------|
| 1 | `client.py` | — | 公共模块：BASE_URL (18080) + 请求/响应打印 |
| 2 | `connector_create.py` | #1 | 创建连接器（含异常场景） |
| 3 | `connector_list.py` | #2 | 查询列表（分页/过滤/搜索/空结果） |
| 4 | `connector_detail.py` | #3 | 查询详情（正常/不存在/ID类型） |
| 5 | `connector_update.py` | #4 | 更新（正常/不存在） |
| 6 | `connector_delete.py` | #5 | 删除（正常/不存在） |
| 7 | `connector_config_get.py` | #6 | 获取配置（已配置/未配置/不存在） |
| 8 | `connector_config_set.py` | #7 | 编辑配置（正常/空/null） |
| 9 | `flow_create.py` | #8 | 创建流（正常/缺nameCn/缺nameEn） |
| 10 | `flow_list.py` | #9 | 流列表（分页/过滤/搜索/空结果） |
| 11 | `flow_detail.py` | #10 | 流详情（正常/不存在） |
| 12 | `flow_update.py` | #11 | 更新流（正常/不存在） |
| 13 | `flow_delete.py` | #12 | 删除流（正常/不存在） |
| 14 | `flow_start.py` | #13 | 启动流（正常/重复/不存在） |
| 15 | `flow_stop.py` | #14 | 停止流（正常/重复/不存在） |
| 16 | `flow_config_get.py` | #15 | 编排配置（正常/不存在） |
| 17 | `flow_config_set.py` | #16 | 保存编排（正常/空/null） |
| 18 | `debug_test_run.py` | #17 | 测试运行（flow不存在/未配置） |
| 19 | `contract_response.py` | L4 | 响应格式/BIGINT ID/枚举/时间/camelCase |
| 20 | `all.py` | #1~#17+L4 | 全量回归执行器 |

### connector-api（5 个文件）

| # | 文件名 | 覆盖接口 | 说明 |
|---|--------|---------|------|
| 1 | `client.py` | — | 公共模块：BASE_URL (18180) + 请求/响应打印 + PASS/FAIL 判断 |
| 2 | `trigger_invoke.py` | #18 | HTTP触发（凭证缺失/flow不存在/未运行 + 快乐路径 + inputContract校验 + 限流测试）|
| 3 | `test_run.py` | 内部测试运行 | 测试运行内部端点（flow不存在/未运行 + 快乐路径含steps格式 + 空mockTriggerData）|
| 4 | `contract_response.py` | L4 + 执行响应 | 响应格式/BIGINT ID/枚举/时间/camelCase + 执行响应格式 + errorInfo |
| 5 | `all.py` | 全量 | 全量回归执行器 |

### 合计
- open-server: **20 个文件**，覆盖 **#1~#17 + L4**
- connector-api: **5 个文件**，覆盖 **#18 + 内部 test-run + L4**
