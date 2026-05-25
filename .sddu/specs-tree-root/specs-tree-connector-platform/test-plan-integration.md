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
| 脚本语言 | **Python 3** + `requests` | 断言灵活、结构化、与 Java 包结构镜像 |
| 测试框架 | **pytest** | fixture 管理、参数化、报告插件 |
| 数据库操作 | **PyMySQL** | 直连 MySQL 做数据准备/清理 |
| 报告生成 | pytest-html 或 自定义 MD | 生成可读的测试报告 |

### 2.3 数据策略

| 阶段 | 操作 | 说明 |
|------|------|------|
| 数据准备 (setup) | INSERT 测试数据 | 使用时间戳后缀避免冲突，记录插入的 ID |
| 用例执行 | 逐个接口调用 | 每个用例独立验证，用例间可能依赖（如先创建再查询）|
| 数据清理 (teardown) | DELETE 按 ID 清理 | 确保可重复执行，不污染数据库 |

---

## 三、Python 文件结构（镜像 Java 包结构）

### open-server（#1~#17）

```
open-server/src/test/python/
├── conftest.py                        # pytest 全局配置
│   ├── BASE_URL = "http://localhost:18080/open-server"
│   ├── pytest_configure() → 生成测试报告
│   └── db_helper() fixture → 数据准备/清理
│
└── modules/
    ├── connector/
    │   └── controller/
    │       └── test_connector.py      # API #1~#7  Connector CRUD + 配置
    │
    ├── flow/
    │   └── controller/
    │       └── test_flow.py           # API #8~#16 Flow CRUD + 启停 + 配置
    │
    └── debug/
        └── test_debug.py             # API #17    测试代理
```

### connector-api（#18 + 契约）

```
connector-api/src/test/python/
├── conftest.py                        # BASE_URL = "http://localhost:18180"
│
├── modules/
│   └── trigger/
│       └── controller/
│           └── test_trigger.py        # API #18    HTTP 触发
│
└── common/
    └── test_contract.py               # L4 契约：响应格式、ID 类型、枚举值
```

### Java ↔ Python 对应关系

| Java 源文件（main） | Python 测试文件 |
|---|---|
| `modules/connector/controller/ConnectorController.java` | `modules/connector/controller/test_connector.py` |
| `modules/flow/controller/FlowController.java` | `modules/flow/controller/test_flow.py` |
| `modules/debug/DebugProxyController.java` | `modules/debug/test_debug.py` |
| `modules/trigger/controller/TriggerController.java` | `modules/trigger/controller/test_trigger.py` |
| `common/ContractSchemaTest.java` | `common/test_contract.py` |

---

## 四、测试用例（按接口分组）

### 4.1 连接器 CRUD — ConnectorController（#1~#5）

#### #1 POST /api/v1/connectors — 创建连接器

| 编号 | 场景 | 输入 | 预期 | 验证点 |
|------|------|------|:----:|--------|
| IT-001 | ✅ 正常创建（完整字段） | nameCn/nameEn/iconFileId/descriptionCn/descriptionEn/connectorType=1 | 200 | `data.id` 为字符串；`createTime` ISO 8601 |
| IT-002 | ❌ 缺少必填 nameCn | body 无 nameCn | 400 | `messageZh` 含"参数错误" |
| IT-003 | ❌ connectorType 非法 | connectorType=99 | 422 | 校验失败提示 |
| IT-004 | ❌ nameCn 超长（>500 字符） | 超长字符串 | 422 | 字段长度校验 |

#### #2 GET /api/v1/connectors — 查询列表

| 编号 | 场景 | 输入 | 预期 | 验证点 |
|------|------|------|:----:|--------|
| IT-005 | ✅ 默认分页 | 无参数 | 200 | `page.curPage=1`、`pageSize=20`、`data` 为数组 |
| IT-006 | ✅ connectorType 过滤 | `?connectorType=1` | 200 | 仅返回 type=1 |
| IT-007 | ✅ keyword 搜索 | `?keyword=测试` | 200 | nameCn/nameEn 模糊匹配 |
| IT-008 | ✅ 自定义分页 | `?curPage=2&pageSize=10` | 200 | 分页参数正确 |
| IT-009 | ✅ 空结果 | `?keyword=NONEXISTENT_XYZ` | 200 | `data=[]`, `page.total=0` |

#### #3 GET /api/v1/connectors/{connectorId} — 查询详情

| 编号 | 场景 | 预期 | 验证点 |
|------|------|:----:|--------|
| IT-010 | ✅ 正常查询 | 200 | 返回完整字段：nameCn/nameEn/connectorType/createTime/lastUpdateTime |
| IT-011 | ❌ connectorId 不存在 | 404 | 资源不存在提示 |
| IT-012 | ✅ 雪花 ID 为 string 类型 | 200 | JSON 中 id 为字符串非数字 |

#### #4 PUT /api/v1/connectors/{connectorId} — 更新

| 编号 | 场景 | 预期 |
|------|------|:----:|
| IT-013 | ✅ 正常更新（nameCn） | 200 |
| IT-014 | ❌ 不存在的连接器 | 404 |

#### #5 DELETE /api/v1/connectors/{connectorId} — 删除

| 编号 | 场景 | 预期 |
|------|------|:----:|
| IT-015 | ✅ 正常删除（刚创建，无引用） | 200 |
| IT-016 | ❌ 不存在的连接器 | 404 |

---

### 4.2 连接器配置 — ConnectorController（#6~#7）

#### #6 GET /api/v1/connectors/{connectorId}/config — 获取连接配置

| 编号 | 场景 | 预期 |
|------|------|:----:|
| IT-017 | ✅ 已配置 | 200, `hasConfig=true`, `connectionConfig` 非空 |
| IT-018 | ✅ 未配置（新建连接器的初始状态） | 200, `hasConfig=false` |
| IT-019 | ❌ 连接器不存在 | 404 |

#### #7 PUT /api/v1/connectors/{connectorId}/config — 编辑连接配置

| 编号 | 场景 | 预期 |
|------|------|:----:|
| IT-020 | ✅ 正常编辑 | 200 |
| IT-021 | ❌ connectionConfig 为空字符串 | 400 |
| IT-022 | ❌ connectionConfig 为 null | 400 |

---

### 4.3 连接流 CRUD — FlowController（#8~#14）

#### #8 POST /api/v1/flows — 创建连接流

| 编号 | 场景 | 预期 |
|------|------|:----:|
| IT-023 | ✅ 正常创建 | 200, `data.id` 字符串, `lifecycleStatus=0` |
| IT-024 | ❌ 缺少必填 nameCn | 400 |
| IT-025 | ❌ 缺少必填 nameEn | 400 |

#### #9 GET /api/v1/flows — 查询流列表

| 编号 | 场景 | 预期 |
|------|------|:----:|
| IT-026 | ✅ 默认分页 | 200, data 数组 |
| IT-027 | ✅ lifecycleStatus 过滤 | 200 |
| IT-028 | ✅ keyword 搜索 | 200 |
| IT-029 | ✅ 空结果 | data=[], total=0 |

#### #10 GET /api/v1/flows/{flowId} — 查看流详情

| 编号 | 场景 | 预期 |
|------|------|:----:|
| IT-030 | ✅ 正常 | 200, 完整字段 |
| IT-031 | ❌ 不存在 | 404 |

#### #11 PUT /api/v1/flows/{flowId} — 更新流信息

| 编号 | 场景 | 预期 |
|------|------|:----:|
| IT-032 | ✅ 正常更新 | 200 |
| IT-033 | ❌ 不存在 | 404 |

#### #12 DELETE /api/v1/flows/{flowId} — 删除流

| 编号 | 场景 | 预期 |
|------|------|:----:|
| IT-034 | ✅ stopped 状态可删除 | 200 |
| IT-035 | ❌ 不存在 | 404 |

#### #13 POST /api/v1/flows/{flowId}/start — 启动

| 编号 | 场景 | 预期 |
|------|------|:----:|
| IT-036 | ✅ stopped → running | 200 |
| IT-037 | ❌ 已是 running（重复） | 409 |
| IT-038 | ❌ 不存在 | 404 |

#### #14 POST /api/v1/flows/{flowId}/stop — 停止

| 编号 | 场景 | 预期 |
|------|------|:----:|
| IT-039 | ✅ running → stopped | 200 |
| IT-040 | ❌ 已是 stopped | 409 |
| IT-041 | ❌ 不存在 | 404 |

---

### 4.4 连接流配置 — FlowController（#15~#16）

#### #15 GET /api/v1/flows/{flowId}/config — 获取编排配置

| 编号 | 场景 | 预期 |
|------|------|:----:|
| IT-042 | ✅ 空配置（初始） | 200, `hasConfig=false` |
| IT-043 | ❌ flow 不存在 | 404 |

#### #16 PUT /api/v1/flows/{flowId}/config — 保存编排配置

| 编号 | 场景 | 预期 |
|------|------|:----:|
| IT-044 | ✅ 正常保存 | 200 |
| IT-045 | ❌ orchestrationConfig 为空字符串 | 400 |
| IT-046 | ❌ orchestrationConfig 为 null | 400 |

---

### 4.5 测试代理 — DebugProxyController（#17）

#### #17 POST /api/v1/flows/{flowId}/test-run — 测试运行

| 编号 | 场景 | 预期 |
|------|------|:----:|
| IT-047 | ❌ flow 不存在 | 404 |
| IT-048 | ❌ flow 未配置编排 | 422 |

---

### 4.6 HTTP 触发 — TriggerController（#18）

#### #18 POST /api/v1/trigger/{flowId}/invoke — HTTP 触发

| 编号 | 场景 | 预期 |
|------|------|:----:|
| IT-049 | ❌ 凭证缺失（无 X-Sys-Token） | 401 |
| IT-050 | ❌ flow 不存在 | 404 |
| IT-051 | ❌ flow 未运行（stopped） | 403 |

---

### 4.7 L4 契约测试（通用响应格式）

| 编号 | 场景 | 验证点 |
|------|------|--------|
| IT-052 | ✅ 成功响应格式 | `{code, messageZh, messageEn, data, page}` |
| IT-053 | ✅ 错误响应格式 | `code != "200"`, `data: null` |
| IT-054 | ✅ 分页响应格式 | `page{curPage, pageSize, total}` |
| IT-055 | ✅ BIGINT ID 为 string 类型 | 所有 id 字段 JSON 类型为 string |
| IT-056 | ✅ 枚举为 TINYINT 数字 | connectorType/lifecycleStatus 等为 int |
| IT-057 | ✅ 时间为 ISO 8601 格式 | createTime/lastUpdateTime 格式校验 |
| IT-058 | ✅ camelCase 字段命名 | 字段名正则 `^[a-z]+[A-Za-z0-9]*$` |
| IT-059 | ✅ 错误码覆盖率 | 400/401/403/404/409/422/429/500 |

---

## 五、执行流程

### 步骤

```
┌──────────────────────────────────────────────┐
│  1. 启动后端服务                               │
│     ├── open-server  →  mvn spring-boot:run   │
│     └── connector-api →  mvn spring-boot:run  │
├──────────────────────────────────────────────┤
│  2. 数据准备                                   │
│     ├── 连接 PyMySQL 直连 MySQL               │
│     ├── 插入测试连接器（时间戳后缀防冲突）       │
│     └── 记录所有插入 ID 用于后续引用             │
├──────────────────────────────────────────────┤
│  3. 执行 pytest                                │
│     ├── 每个模块独立文件                        │
│     ├── 用例间按顺序执行（CRUD 依赖）            │
│     └── 实时输出 pass/fail                      │
├──────────────────────────────────────────────┤
│  4. 数据清理                                   │
│     └── DELETE 按 ID 范围清理所有测试数据        │
├──────────────────────────────────────────────┤
│  5. 生成测试报告                               │
│     └── test-report-integration.md             │
└──────────────────────────────────────────────┘
```

### 启动方式

```bash
# 启动 open-server（后台）
cd open-server && nohup mvn spring-boot:run > logs/server.log 2>&1 &

# 启动 connector-api（后台）
cd connector-api && nohup mvn spring-boot:run > logs/server.log 2>&1 &

# 等待服务就绪
curl -s http://localhost:18080/open-server/actuator/health
curl -s http://localhost:18180/actuator/health

# 安装 Python 依赖
pip install requests PyMySQL pytest

# 执行测试
cd open-server/src/test/python && python -m pytest -v 2>&1 | tee test-output.log
cd connector-api/src/test/python && python -m pytest -v 2>&1 | tee test-output.log
```

---

## 六、与现有测试的关系

| 维度 | 已有 MockMvc 测试（test-plan.md） | 本次真调集成测试（本方案） |
|------|----------------------------------|--------------------------|
| 测试层次 | L2 接口层 | L3 集成测试 |
| 依赖 | 无（全 Mock） | 需 MySQL + Redis + 服务启动 |
| 执行速度 | 毫秒级（~3s 全部） | 秒级（~30s~2min） |
| 执行频率 | 每次提交 CI 运行 | 每日构建 / 预发布运行 |
| 发现问题 | 序列化、校验注解遗漏 | SQL 错误、事务问题、真实数据流 |
| 文件位置 | `src/test/java/` | `src/test/python/` |
| 工具 | JUnit + MockMvc + Mockito | pytest + requests + PyMySQL |

---

## 七、风险与假设

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| 测试数据残留 | 影响后续测试或生产数据 | 每次执行强制 cleanup，失败时也执行 |
| 服务启动失败 | 所有用例阻塞 | 启动前 health check，失败时快速反馈 |
| 数据库表结构变更 | 测试 SQL 脚本失效 | 测试 SQL 与迁移脚本同步维护 |
| 并发执行冲突 | 数据相互干扰 | 使用时间戳后缀隔离，CI 串行执行 |
| 网络/端口冲突 | 服务无法启动 | 先检查端口占用，kill 已有进程 |

---

## 八、文件清单

| # | 文件路径 | 覆盖接口 | 用例数 |
|---|---------|---------|:------:|
| 1 | `open-server/src/test/python/modules/connector/controller/test_connector.py` | #1~#7 | ~22 |
| 2 | `open-server/src/test/python/modules/flow/controller/test_flow.py` | #8~#16 | ~24 |
| 3 | `open-server/src/test/python/modules/debug/test_debug.py` | #17 | ~2 |
| 4 | `connector-api/src/test/python/modules/trigger/controller/test_trigger.py` | #18 | ~3 |
| 5 | `connector-api/src/test/python/common/test_contract.py` | L4 契约 | ~8 |
| 6 | `open-server/src/test/python/conftest.py` | 全局配置 + DB 工具 | — |
| 7 | `connector-api/src/test/python/conftest.py` | 全局配置 | — |
| **合计** | **7 个文件** | **#1~#18 + 契约** | **~59 个用例** |
