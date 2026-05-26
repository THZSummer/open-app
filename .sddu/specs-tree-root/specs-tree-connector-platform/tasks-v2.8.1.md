# 任务分解：连接器平台

**Feature ID**: CONN-PLAT-001  
**创建日期**: 2026-05-22  
**对齐基线**: spec.md v5.0 / plan.md v2.8.1 / plan-api.md v2.8.0 / plan-db.md v2.8.1 / plan-page.md v2.8.0  
**FR 覆盖**: 全量 19 个 FR  

---

## 任务汇总

| 总计 | 当前实施任务 | 外部占位任务 | S | M | L | 波次 |
|------|:------------:|:------------:|:--:|:--:|:--:|:----:|
| 12 | 10 | 2 | 2 | 6 | 2 | 4 |

---

## Wave 1：基础设施（2 任务 · 并行）

---

## TASK-001: open-server 连接器平台 DDL 脚本

**复杂度**: L  
**前置依赖**: 无  
**执行波次**: 1  

### 描述
在 open-server 工程内按照 FlywayDB 的目录与命名风格新增连接器平台 DDL 脚本，包含 7 张表（4 张活跃 + 3 张 V1 预留）。该脚本仅作为开放平台共库 SQL 的标准化存放方式；open-server 不新增 Flyway 依赖，本任务不考虑脚本自动执行机制。connector-api 不存放 DDL，不执行数据库迁移，仅通过 R2DBC 访问已初始化的共库表。

### 涉及文件
- [NEW] `open-server/src/main/resources/db/migration/V2__init_connector_platform_schema.sql`

### 验收标准
- [ ] SQL 脚本存放在 `open-server/src/main/resources/db/migration/`
- [ ] 文件名符合 FlywayDB 风格：`V2__init_connector_platform_schema.sql`
- [ ] 不创建 `connector-platform/` 业务子目录
- [ ] open-server 不新增 Flyway 相关依赖
- [ ] connector-api 不新增任何 DDL / migration 脚本
- [ ] `openplatform_v2_cp_connector_t` 表创建（BIGINT 雪花 ID 主键，双语名称/描述，TINYINT 枚举，4 审计字段）
- [ ] `openplatform_v2_cp_connector_version_t` 表创建（1:1 关联 connector_t，MEDIUMTEXT connection_config JSON，无物理外键）
- [ ] `openplatform_v2_cp_flow_t` 表创建（lifecycle_status 默认 1=running，含 TINYINT 枚举，4 审计字段）
- [ ] `openplatform_v2_cp_flow_version_t` 表创建（1:1 关联 flow_t，MEDIUMTEXT orchestration_config JSON，触发器内嵌）
- [ ] `openplatform_v2_cp_execution_record_t` 表创建（V1 预留，MVP 不写入）
- [ ] `openplatform_v2_cp_execution_step_t` 表创建（V1 预留，MVP 不写入）
- [ ] `openplatform_v2_cp_storage_blob_ref_t` 表创建（V1 预留，MVP 不写入）
- [ ] 所有表符合 `plan-db.md §0` 规范：`openplatform_v2_cp_` 前缀 + `_t` 后缀 + 4 审计字段 + idx_xxx/uk_xxx 索引 + 无物理外键
- [ ] SQL 语法可被 MySQL 正常解析执行

### 验证命令
```bash
# 仅验证 SQL 脚本可执行与表结构存在，不验证 Flyway 自动执行
mysql -h <host> -P <port> -u <user> -p <database> < open-server/src/main/resources/db/migration/V2__init_connector_platform_schema.sql

mysql -h <host> -P <port> -u <user> -p <database> -e "SHOW TABLES LIKE 'openplatform_v2_cp_%';"
```

---

## TASK-002: connector-api 项目脚手架 + R2DBC Entity

**复杂度**: M  
**前置依赖**: 无（联调验证依赖 TASK-001 完成共库初始化）  
**执行波次**: 1  

### 描述
创建 `connector-api` 独立 Spring Boot 工程，配置 Spring WebFlux + R2DBC + Redis Reactive，定义 4 张活跃表的 R2DBC Entity + Repository。connector-api 不维护 DDL 脚本，不执行数据库迁移；仅配置 R2DBC 连接访问开放平台共库表。目录结构必须对齐 open-server：包名前缀统一为 `com.xxx.it.works.wecode.v2`，`common` 与 `modules` 同级；`common` 放公共能力，`modules` 仅放业务模块代码。

### 涉及文件
- [NEW] `connector-api/pom.xml`（spring-boot-starter-webflux, r2dbc-mysql, spring-data-r2dbc, spring-data-redis-reactive, reactor-core）
- [NEW] `connector-api/src/main/java/com/xxx/it/works/wecode/v2/ConnectorApiApplication.java`
- [NEW] `connector-api/src/main/resources/application.yml`
- [NEW] `connector-api/src/main/java/com/xxx/it/works/wecode/v2/common/config/R2dbcConfig.java`
- [NEW] `connector-api/src/main/java/com/xxx/it/works/wecode/v2/common/config/ReactiveRedisConfig.java`
- [NEW] `connector-api/src/main/java/com/xxx/it/works/wecode/v2/modules/connector/entity/ConnectorEntity.java`
- [NEW] `connector-api/src/main/java/com/xxx/it/works/wecode/v2/modules/connector/entity/ConnectorVersionEntity.java`
- [NEW] `connector-api/src/main/java/com/xxx/it/works/wecode/v2/modules/connector/repository/ConnectorVersionReadRepository.java`（R2DBC Repository，按 connector_id 查 connection_config）
- [NEW] `connector-api/src/main/java/com/xxx/it/works/wecode/v2/modules/flow/entity/FlowEntity.java`
- [NEW] `connector-api/src/main/java/com/xxx/it/works/wecode/v2/modules/flow/entity/FlowVersionEntity.java`
- [NEW] `connector-api/src/main/java/com/xxx/it/works/wecode/v2/modules/flow/repository/FlowVersionReadRepository.java`（R2DBC Repository，按 flow_id 查 orchestration_config）

### 验收标准
- [ ] Maven 构建成功，无 spring-boot-starter-web / mybatis / jdbc 依赖泄露
- [ ] 启动后 WebFlux Netty 正常监听端口 18180
- [ ] R2DBC 连接 MySQL 正常，可执行简单 SELECT
- [ ] ReactiveRedisTemplate 可用
- [ ] Entity 字段对齐 `plan-db.md` DDL（BIGINT ID / 双语字段 / TINYINT 枚举 / MEDIUMTEXT JSON）
- [ ] Java 包结构对齐 open-server：`com.xxx.it.works.wecode.v2/common` 与 `com.xxx.it.works.wecode.v2/modules` 同级
- [ ] `common` 不在 `modules` 内；公共配置/异常/上下文/安全/模型放 `common`，业务代码放 `modules/{module}`
- [ ] connector-api 工程内不存在 DDL / Flyway migration 脚本
- [ ] 严格遵守全 reactive 栈规则：禁止 JDBC/MyBatis/RestTemplate/synchronized

### 验证命令
```bash
cd connector-api && mvn clean package -DskipTests && java -jar target/connector-api-*.jar
curl http://localhost:18180/connector-api/actuator/health
```

---

## Wave 2：后端核心逻辑（3 任务 · 并行）

---

## TASK-003: open-server 连接器管理模块（Connector CRUD + Config）

**复杂度**: M  
**前置依赖**: TASK-001  
**执行波次**: 2  

### 描述
在 open-server 中新增 `modules/connector` 模块，实现连接器 CRUD（FR-001~004）和连接配置管理（FR-005~006），覆盖 API #1~#7。

### 涉及文件
- [NEW] `open-server/src/main/java/.../modules/connector/entity/Connector.java`
- [NEW] `open-server/src/main/java/.../modules/connector/entity/ConnectorVersion.java`
- [NEW] `open-server/src/main/java/.../modules/connector/mapper/ConnectorMapper.java`
- [NEW] `open-server/src/main/java/.../modules/connector/mapper/ConnectorVersionMapper.java`
- [NEW] `open-server/src/main/java/.../modules/connector/service/ConnectorService.java`
- [NEW] `open-server/src/main/java/.../modules/connector/controller/ConnectorController.java`

### 验收标准
- [ ] `POST /api/v1/connectors` — 创建连接器（名称/图标/描述/类型=HTTP），返回雪花 ID（string）
- [ ] `GET /api/v1/connectors` — 列表查询，支持 type 过滤 + keyword 搜索 + 分页
- [ ] `GET /api/v1/connectors/{connectorId}` — 详情查询，含基本信息
- [ ] `PUT /api/v1/connectors/{connectorId}` — 编辑基本信息（直接更新字段，不创建新版本）
- [ ] `DELETE /api/v1/connectors/{connectorId}` — 删除前校验无运行中连接流引用
- [ ] `GET /api/v1/connectors/{connectorId}/config` — 查看连接配置（请求/响应 Schema/认证类型/超时/限流）
- [ ] `PUT /api/v1/connectors/{connectorId}/config` — 编辑连接配置（编辑即生效，connectionConfig 全文替换）
- [ ] 认证类型 Schema 仅声明字段类型（含 sensitive:true 标记），不存储凭证值
- [ ] 所有 ID 返回 string，枚举返回 TINYINT 数字，时间返回 ISO 8601
- [ ] 响应格式对齐 `plan-api.md §1.5`（统一 `{code, messageZh, messageEn, data, page}` 格式）

### 验证命令
```bash
# 创建连接器
curl -X POST http://localhost:18080/open-server/api/v1/connectors \
  -H 'Content-Type: application/json' \
  -d '{"nameCn":"IM发送消息","nameEn":"IM Send","connectorType":1}'

# 编辑连接配置
curl -X PUT http://localhost:18080/open-server/api/v1/connectors/1234567890123456789/config \
  -H 'Content-Type: application/json' \
  -d '{"connectionConfig":{"protocol":"HTTP","protocolConfig":{"url":"https://api.example.com","method":"POST"},"authTypeSchema":{"type":"AKSK","fields":[{"name":"accessKey","carrier":"header","fieldName":"AK","required":true,"sensitive":true}]},"inputSchema":{"type":"object"},"outputSchema":{"type":"object"},"timeoutMs":30000,"rateLimit":{"maxQps":10}}}'
```

---

## TASK-004: open-server 连接流管理模块（Flow CRUD + Config + Lifecycle）

**复杂度**: M  
**前置依赖**: TASK-001, TASK-003（连接器数据作为连接流节点引用源）  
**执行波次**: 2  

### 描述
在 open-server 中新增 `modules/flow` 模块，实现连接流 CRUD（FR-009~012）、配置管理（FR-016~017）和 lifecycle（FR-014~015），覆盖 API #8~#16。

### 涉及文件
- [NEW] `open-server/src/main/java/.../modules/flow/entity/Flow.java`
- [NEW] `open-server/src/main/java/.../modules/flow/entity/FlowVersion.java`
- [NEW] `open-server/src/main/java/.../modules/flow/mapper/FlowMapper.java`
- [NEW] `open-server/src/main/java/.../modules/flow/mapper/FlowVersionMapper.java`
- [NEW] `open-server/src/main/java/.../modules/flow/service/FlowService.java`
- [NEW] `open-server/src/main/java/.../modules/flow/controller/FlowController.java`
- [MODIFY] `open-server/src/main/java/.../modules/connector/service/ConnectorService.java`（引用校验接口）

### 验收标准
- [ ] `POST /api/v1/flows` — 创建连接流（创建后默认 lifecycle_status=1 running，自动创建 flow_version 记录）
- [ ] `GET /api/v1/flows` — 列表查询，支持 lifecycleStatus 过滤 + keyword 搜索 + 分页
- [ ] `GET /api/v1/flows/{flowId}` — 详情查询（含 lifecycleStatus）
- [ ] `PUT /api/v1/flows/{flowId}` — 编辑基本信息（直接更新字段）
- [ ] `DELETE /api/v1/flows/{flowId}` — 仅 stopped 状态可删除，级联删除关联记录
- [ ] `POST /api/v1/flows/{flowId}/start` — 启动（stopped→running）
- [ ] `POST /api/v1/flows/{flowId}/stop` — 停止（running→stopped）
- [ ] `GET /api/v1/flows/{flowId}/config` — 查看编排配置（含 trigger/nodes/edges 完整 DAG）
- [ ] `PUT /api/v1/flows/{flowId}/config` — 保存编排配置（编辑即生效，含 HTTP trigger 配置/连接器节点/数据处理节点/出口节点/edges）
- [ ] 编排校验：无节点时拒绝保存
- [ ] 触发器配置内嵌于 orchestrationConfig（SYSTOKEN 认证类型 Schema + inputSchema + rateLimit）
- [ ] 同上规范：ID string / 枚举 TINYINT / 时间 ISO 8601 / 统一响应格式

### 验证命令
```bash
# 创建连接流
curl -X POST http://localhost:18080/open-server/api/v1/flows \
  -H 'Content-Type: application/json' \
  -d '{"nameCn":"新消息通知","nameEn":"New Message Notification"}'

# 保存编排配置
curl -X PUT http://localhost:18080/open-server/api/v1/flows/1234567890123456789/config \
  -H 'Content-Type: application/json' \
  -d '{"orchestrationConfig":{"nodes":[{"id":"node_trigger","type":"trigger","labelCn":"接收","labelEn":"Receive","authTypeSchema":{"type":"SYSTOKEN","fields":[{"name":"token","carrier":"header","fieldName":"X-Sys-Token"}]},"inputSchema":{"type":"object","properties":{"sender":{"type":"string"}},"required":["sender"]},"rateLimit":{"maxQps":100},"position":{"x":100,"y":200}},{"id":"node_exit","type":"exit","labelCn":"返回","labelEn":"Return","outputFields":["result"],"position":{"x":300,"y":200}}],"edges":[{"id":"e1","sourceNodeId":"node_trigger","targetNodeId":"node_exit"}]}}'

# 停止
curl -X POST http://localhost:18080/open-server/api/v1/flows/1234567890123456789/stop
```

---

## TASK-005: connector-api 运行时引擎 + 节点执行器

**复杂度**: L  
**前置依赖**: TASK-002  
**执行波次**: 2  

### 描述
在 connector-api 中实现轻量同步执行引擎：ReactiveSequentialExecutor、ExecutionContext、ConnectorNodeExecutor（HTTP 调用）、DataProcessorExecutor（字段映射）、ExitNodeExecutor。

### 涉及文件
- [NEW] `connector-api/src/main/java/com/xxx/it/works/wecode/v2/modules/runtime/executor/ReactiveSequentialExecutor.java`
- [NEW] `connector-api/src/main/java/com/xxx/it/works/wecode/v2/modules/runtime/context/ExecutionContext.java`
- [NEW] `connector-api/src/main/java/com/xxx/it/works/wecode/v2/modules/runtime/executor/NodeExecutor.java`（接口，返回 `Mono<NodeOutput>`）
- [NEW] `connector-api/src/main/java/com/xxx/it/works/wecode/v2/modules/runtime/node/ConnectorNodeExecutor.java`
- [NEW] `connector-api/src/main/java/com/xxx/it/works/wecode/v2/modules/runtime/node/DataProcessorExecutor.java`
- [NEW] `connector-api/src/main/java/com/xxx/it/works/wecode/v2/modules/runtime/node/ExitNodeExecutor.java`
- [NEW] `connector-api/src/main/java/com/xxx/it/works/wecode/v2/modules/runtime/node/EntryNodeExecutor.java`
- [NEW] `connector-api/src/main/java/com/xxx/it/works/wecode/v2/modules/runtime/model/NodeOutput.java`
- [NEW] `connector-api/src/main/java/com/xxx/it/works/wecode/v2/modules/runtime/model/ExecutionResult.java`

### 验收标准
- [ ] ReactiveSequentialExecutor 从入口节点开始，按 edges 拓扑顺序 flatMap 串联各节点 `Mono<NodeOutput>`
- [ ] ConnectorNodeExecutor 通过 WebClient 同步调用下游 HTTP API（读取 connectionConfig.protocolConfig.url/method/headers，注入 credentials 到请求头）
- [ ] ConnectorNodeExecutor 支持超时：WebClient timeout + `.timeout(Duration)` 双重保障
- [ ] DataProcessorExecutor 支持 3 种字段映射：源字段→目标字段 / 常量赋值 / 路径引用解析 `${node_xxx.field}`
- [ ] ExitNodeExecutor 按 outputFields 从上下文提取字段构造返回值
- [ ] EntryNodeExecutor 透传触发数据到 ExecutionContext
- [ ] ExecutionContext 支持线程安全的数据读写（Reactor Context）
- [ ] credentials 仅内存生命周期，节点执行后从上下文显式清除
- [ ] 执行结果 ExecutionResult 含：status + 各步骤 input/output/duration + 最终 resultData
- [ ] 全部返回 `Mono<T>`，无阻塞 API

### 验证命令
```bash
# 单元测试：模拟编排配置，执行线性流程
cd connector-api && mvn test -Dtest=ReactiveSequentialExecutorTest
# 验证：HTTP connector 调用成功返回响应
# 验证：字段映射正确传递数据
# 验证：超时场景下返回 timeout 状态
```

---

## Wave 3：运行时端点 + 调试代理 + 前端占位（3 个当前实施任务 + 2 个外部占位）

---

## TASK-006: connector-api HTTP 触发端点

**复杂度**: M  
**前置依赖**: TASK-005  
**执行波次**: 3  

### 描述
实现 `POST /api/v1/trigger/{flowId}/invoke` 对外 HTTP 触发端点，响应外部系统请求并同步执行连接流。

### 涉及文件
- [NEW] `connector-api/src/main/java/com/xxx/it/works/wecode/v2/modules/trigger/controller/TriggerController.java`
- [NEW] `connector-api/src/main/java/com/xxx/it/works/wecode/v2/modules/trigger/service/TriggerService.java`

### 验收标准
- [ ] `POST /api/v1/trigger/{flowId}/invoke` 接收请求后校验 lifecycleStatus=running
- [ ] 校验 trigger.authTypeSchema（SYSTOKEN）格式
- [ ] 校验 rateLimit.maxQps 限流（超过返回 429）
- [ ] 解析请求体作为 trigger.inputSchema 校验
- [ ] 创建 ExecutionContext → 调 ReactiveSequentialExecutor → 同步返回 ExecutionResult
- [ ] 返回格式含 executionId（雪花 ID string）/ status（TINYINT）/ resultData / durationMs
- [ ] 401（认证失败）/ 403（流未运行）/ 429（限流）
- [ ] URL 使用不可预测随机路径 / 支持请求签名验证（NFR-011）

### 验证命令
```bash
curl -X POST http://localhost:18180/connector-api/api/v1/trigger/1234567890123456789/invoke \
  -H 'X-Sys-Token: test-token' \
  -H 'Content-Type: application/json' \
  -d '{"sender":"ext_sys","content":"hello"}'
```

---

## TASK-007: connector-api 测试执行端点

**复杂度**: S  
**前置依赖**: TASK-005  
**执行波次**: 3  

### 描述
在 connector-api 中暴露内部测试接口，供 open-server debug-proxy 转发调用，支持传入模拟触发数据和 credentials。

### 涉及文件
- [NEW] `connector-api/src/main/java/com/xxx/it/works/wecode/v2/modules/debug/controller/TestRunController.java`
- [NEW] `connector-api/src/main/java/com/xxx/it/works/wecode/v2/modules/debug/service/TestRunService.java`

### 验收标准
- [ ] 内部端点接收 mockTriggerData + credentials（按 connectorVersionId 分组）
- [ ] 创建 ExecutionContext → 调 ReactiveSequentialExecutor → 同步返回 ExecutionResult（含各步骤详情）
- [ ] 标记 isTest=true，triggerType=3
- [ ] 仅限内网调用（open-server debug-proxy）

### 验证命令
```bash
# 经 open-server 内部转发调用
curl -X POST http://localhost:18080/open-server/api/v1/flows/1234567890123456789/test-run \
  -H 'Content-Type: application/json' \
  -d '{"mockTriggerData":{"sender":"test"},"credentials":{"9876543210123456789":{"accessKey":"ak","secretKey":"sk"}}}'
```

---

## TASK-008: open-server 调试代理模块（debug-proxy）

**复杂度**: S  
**前置依赖**: TASK-004, TASK-007  
**执行波次**: 3  

### 描述
在 open-server 中新增 debug-proxy 模块，接收前端测试运行请求并转发至 connector-api 内部测试接口。

### 涉及文件
- [NEW] `open-server/src/main/java/.../modules/debug/DebugProxyController.java`
- [NEW] `open-server/src/main/java/.../modules/debug/DebugProxyService.java`

### 验收标准
- [ ] `POST /api/v1/flows/{flowId}/test-run` 接收前端请求（含 mockTriggerData + credentials）
- [ ] 内部 HTTP 转发至 connector-api 测试接口
- [ ] 透传 connector-api 返回的 ExecutionResult（含各步骤详情/耗时）
- [ ] 转发失败时返回明确错误信息

### 验证命令
```bash
curl -X POST http://localhost:18080/open-server/api/v1/flows/1234567890123456789/test-run \
  -H 'Content-Type: application/json' \
  -d '{"mockTriggerData":{"sender":"test"},"credentials":{}}'
```

---

## TASK-009: wecodesite 连接器前端页面

**类型**: 外部占位任务  
**实施范围**: 不在此 Tasks 文档实施  
**执行方式**: 由其他渠道并行完成  

> 说明：仅保留标题占位，不展开描述、涉及文件、验收标准、验证命令；不作为当前 SDDU build 阶段的实施任务或强依赖。

---

## TASK-010: wecodesite 连接流前端页面 + 编排画布

**类型**: 外部占位任务  
**实施范围**: 不在此 Tasks 文档实施  
**执行方式**: 由其他渠道并行完成  

> 说明：仅保留标题占位，不展开描述、涉及文件、验收标准、验证命令；不作为当前 SDDU build 阶段的实施任务或强依赖。

---

## Wave 4：增强与集成（2 任务 · 串行）

---

## TASK-011: 默认错误处理 + 限流 + 审计日志

**复杂度**: M  
**前置依赖**: TASK-005, TASK-006  
**执行波次**: 4  

### 描述
在 connector-api 中实现平台默认错误处理、限流拦截器，在 open-server 中实现关键操作审计日志。

### 涉及文件
- [NEW] `connector-api/src/main/java/com/xxx/it/works/wecode/v2/common/exception/DefaultErrorHandler.java`
- [NEW] `connector-api/src/main/java/com/xxx/it/works/wecode/v2/common/interceptor/RateLimitFilter.java`
- [MODIFY] `connector-api/src/main/java/com/xxx/it/works/wecode/v2/modules/trigger/controller/TriggerController.java`（接入限流 Filter）
- [NEW] `open-server/src/main/java/com/xxx/it/works/wecode/v2/common/interceptor/AuditLogAspect.java`

### 验收标准
- [ ] FR-023 默认错误处理：单节点失败标记 failed，连接流整体标记 failed，失败上下文保留
- [ ] FR-024 默认限流：HTTP 触发超过 trigger.rateLimit.maxQps → 返回 429（Too Many Requests），前端测试运行提示稍后重试
- [ ] 限流维度：按 flowId
- [ ] 错误消息格式：含错误码 + 消息 + 下游状态码
- [ ] NFR-013 审计日志：连接流启停操作记录 auditLog（createBy + action + time + flowId）

### 验证命令
```bash
# 压力测试限流
for i in {1..20}; do
  curl -X POST http://localhost:18180/connector-api/api/v1/trigger/1234567890123456789/invoke \
    -H 'X-Sys-Token: test' -H 'Content-Type: application/json' -d '{"sender":"test"}' &
done
# 预期：超过 maxQps 的请求返回 429
```

---

## TASK-012: 端到端集成测试 + E2E 冒烟

**复杂度**: M  
**前置依赖**: TASK-003, TASK-004, TASK-005, TASK-006, TASK-007, TASK-008, TASK-011  
**外部协同**: TASK-009, TASK-010（wecodesite 占位任务，由其他渠道并行完成，不作为当前实施强依赖）  
**执行波次**: 4  

### 描述
编写端到端集成测试，覆盖核心用户故事（US-01 ~ US-04）的完整链路。

### 涉及文件
- [NEW] `docs/connector-platform-test/e2e-test-cases.md`
- [NEW] `connector-api/src/test/java/.../integration/ConnectorFlowE2ETest.java`

### 验收标准
- [ ] **US-01 连接器管理**：创建 → 查看列表 → 查看详情 → 编辑 → 删除（校验引用）
- [ ] **US-02 连接配置**：编辑 connectionConfig（协议/地址/认证 Schema/入参出参）→ 查看配置 → 编辑即生效验证
- [ ] **US-03 连接流管理**：创建 → 查看列表 → 保存编排配置 → 查看配置 → 启停
- [ ] **US-04 编排与测试**：编排画布保存 → 测试运行返回完整步骤详情和结果 → HTTP 触发成功返回
- [ ] 全链路：connector-api HTTP trigger → 按 edges 顺序执行 connector + data_processor + exit → 返回结果
- [ ] 错误场景：未运行连接流触发返回 403 / 限流返回 429 / 编排为空拒绝保存
- [ ] 测试数据清理

### 验证命令
```bash
cd connector-api && mvn test -Dtest=ConnectorFlowE2ETest
# 预期：所有核心用户故事场景测试通过
```

---

## 附录

### FR 覆盖矩阵

| FR | TASK | 波次 |
|----|------|:----:|
| FR-001~004 连接器 CRUD | TASK-003；TASK-009（外部占位） | 2, 3 |
| FR-005~006 连接配置 | TASK-003；TASK-009（外部占位） | 2, 3 |
| FR-009~012 连接流 CRUD | TASK-004；TASK-010（外部占位） | 2, 3 |
| FR-013 部署（编辑即运行） | TASK-004（简化） | 2 |
| FR-014~015 启停 | TASK-004；TASK-010（外部占位） | 2, 3 |
| FR-016~017 流配置/编排 | TASK-004；TASK-010（外部占位） | 2, 3 |
| FR-020 测试执行 | TASK-005, TASK-007, TASK-008；TASK-010（外部占位） | 2, 3 |
| FR-021 HTTP 触发 | TASK-005, TASK-006 | 2, 3 |
| FR-023~024 错误/限流 | TASK-005, TASK-011 | 2, 4 |

### 依赖图

```
Wave 1:  [TASK-001 DB] ───────────────┐
         [TASK-002 connector-api脚手架]─┤
                                        │
Wave 2:  [TASK-003 连接器模块] ←────────┤─(并行)
         [TASK-004 连接流模块] ←────────┤
         [TASK-005 运行时引擎] ←────────┘
                │       │
Wave 3:  ┌──────┤       ├──────┐
         │      │       │      │
    [TASK-006  [TASK-007 [TASK-009 外部占位]
     HTTP触发]  测试端点]  [TASK-010 外部占位]
         │        │         │
    [TASK-008 debug-proxy]  │（由其他渠道并行完成）
         │        │
         └────────┤
                  │
Wave 4:  [TASK-011 错误/限流/审计]
                  │
         [TASK-012 E2E集成测试]

注：TASK-009/TASK-010 为 wecodesite 外部占位任务，不作为当前 SDDU build 实施链路的强依赖。
```

---

**任务分解状态**: ✅ 完成  
**下一步**: 运行 `@sddu-build TASK-001` 开始实现第一个任务