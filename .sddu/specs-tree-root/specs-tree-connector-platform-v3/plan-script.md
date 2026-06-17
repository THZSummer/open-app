# 脚本执行引擎设计：连接器平台 V3

**Feature ID**: CONN-PLAT-003
**关联文档**: plan.md（主技术规划）、plan-json-schema.md（值表达式体系 §3）、plan-runtime.md（运行时引擎）
**版本**: v4.0-draft
**创建日期**: 2026-06-17
**对齐基线**: spec.md（继承自 V2 v2.24-draft，V3 待重写）

---

## 0. 概述

### 0.1 背景与动机

V2 的数据处理节点仅支持 4 种类型转换函数。V3 引入**脚本节点**，用户可在 JS 中直接使用 `${$.node.xxx}` 引用语法访问任意上游节点数据，运行时做**纯字符串替换**后交由 GraalJS 执行。

### 0.2 核心设计

| # | 决策 | 说明 |
|:---:|------|------|
| 1 | **无 inputMapping** | 不配置入参映射，用户在脚本内直接写 `${$.node.xxx.output.field}` |
| 2 | **字符串替换** | 运行前扫描 `${$.scope.path}`，解析值 → JS 字面量，替换到脚本中 |
| 3 | **GraalJS 引擎** | ES2022，沙箱四层防护，`HostAccess.EXPLICIT` |
| 4 | **WebFlux 非阻塞** | `Mono.fromCallable().subscribeOn(boundedElastic)` |
| 5 | **配置即存储** | 脚本源码存于 `FlowVersion.orchestrationConfig` JSON，零新表 |

### 0.3 执行模型（一行描述）

```
用户写的 JS（含 ${$.node.xxx} 引用）
      ↓ 字符串替换（解析引用 → JS 字面量）
纯 JS（所有引用已替换为具体值）
      ↓ GraalJS Context.eval()
返回值（JS object → Java Map）
```

### 0.4 Spring Boot + WebFlux + GraalJS 关系

```
Event Loop（非阻塞）            boundedElastic（阻塞隔离）
┌─────────────────┐          ┌──────────────────────────┐
│ HTTP 路由/解析   │  Mono    │ ScriptNodeExecutor       │
│ 响应序列化       │←─────── │ ① 字符串替换（引用解析）   │
│                 │  回调    │ ② GraalJS Context.eval() │
└─────────────────┘          │ ③ Value → Map            │
                             └──────────────────────────┘
```

---

## 1. 语言选型

### 1.1 决策：GraalJS

| 理由 | 说明 |
|------|------|
| 沙箱碾压 | `allowIO/allowCreateThread/allowNativeAccess(false)` + `HostAccess.EXPLICIT` + `statementLimit` — 全维度运行时管控 |
| ES2022 | 零学习成本 |
| 精细 Java 互操作 | `@HostAccess.Export` 逐方法暴露 |

```xml
<!-- 仅 connector-api -->
<dependency>
    <groupId>org.graalvm.polyglot</groupId>
    <artifactId>polyglot</artifactId>
    <version>24.1.1</version>
</dependency>
<dependency>
    <groupId>org.graalvm.js</groupId>
    <artifactId>js</artifactId>
    <version>24.1.1</version>
</dependency>
```

### 1.2 JavaScript 要求

| 维度 | 约束 |
|------|------|
| 版本 | ES2022 |
| 严格模式 | 自动启用 |
| 模块 | 禁用 `import`/`export` |
| 顶层 await | 禁用 |
| 类型映射 | `object`→`Map`、`number`→`double`、`string`→`String`、`boolean`→`boolean`、`Array`→`List` |
| 引用语法 | `${$.node.{id}.{input\|output}.path}` — 与 V2 值表达式体系统一 |

---

## 2. 字符串替换机制（核心）

### 2.1 原理

用户脚本中直接写 V2 引用语法，运行时**先替换、再执行**。

**三步流程**：

```
① 用户编写脚本（含 ${$.node.xxx} 引用）
   const users = ${$.node.conn_1.output.body.data.users};
   const name  = ${$.node.trigger.input.body.sender};

② 运行时替换 — 扫描所有 ${...}，逐个解析为具体值，转为 JS 字面量字符串后替换
   const users = [{"name":"Alice","age":28},{"name":"Bob","age":35}];
   const name  = "Alice";

③ GraalJS 执行纯 JS — 此时脚本中已无任何引用，就是标准 JS 代码
   → 返回 { total: 2, avgAge: 31.5 }
```

**关键要点**：

- 替换发生在 GraalJS 编译/执行**之前**，GraalJS 看到的永远是纯 JS
- 引用语法复用 V2 值表达式体系 `${$.scope.path}`，用户已熟悉
- 每个引用**独立解析、独立替换**，不依赖脚本上下文——即字符串层面的模板替换，而非 AST 级别的变量绑定
- 替换后的脚本可能因引用值的内容而产生语法变化（如字符串中含特殊字符），`JSON.stringify` 保证了字面量安全

### 2.2 替换规则

对脚本中每个 `${$.scope.path}` 引用，解析其运行时值，按以下规则转为 JS 字面量：

| 解析值的 Java 类型 | 转换方式 | 替换结果示例 | 说明 |
|------|------|------|------|
| `String` | `JSON.stringify(value)` | `"hello"` / `"line1\nline2"` | 自动处理引号转义、换行、Unicode |
| `Integer` / `Long` / `BigDecimal` | `value.toString()` | `42` / `3.14` | 直接输出数字 |
| `Boolean` | `true` 或 `false` | `true` | 无引号 |
| `null` | 字面量 `null` | `null` | — |
| `Map` / `List` | `JSON.stringify(value)` | `{"a":1}` / `[1,2,3]` | 嵌套对象/数组完整序列化 |
| 解析失败（字段不存在） | 字面量 `null` + WARN 日志 | `null` | 不中断执行，脚本自行兜底 |

**为什么用 `JSON.stringify`？** 它确保生成的字符串是合法的 JS 字面量。例如值为 `He said "Hi"` 时，`JSON.stringify` 输出 `"He said \"Hi\""`——合法的 JS 字符串字面量。直接拼接会导致 `const msg = "He said "Hi""` 这样的语法错误。

---

## 3. 安全沙箱

### 3.1 四层防护

```
第 1 层：Context 权限开关（全关）
  allowIO/allowNativeAccess/allowCreateThread/allowCreateProcess(false)
  allowPolyglotAccess(NONE)  allowEnvironmentAccess(NONE)

第 2 层：HostAccess 白名单
  HostAccess.EXPLICIT → 仅 @HostAccess.Export 方法可被 JS 调用
  allowHostClassLookup(→false)

第 3 层：ResourceLimits
  statementLimit(10000)
  禁用 console/print/load

第 4 层：运维熔断
  连续超时 5 次 → 版本禁用 + 告警
```

### 3.2 Context 工厂

```java
@Component
public class GraalJSContextFactory {

    private static final Engine SHARED_ENGINE = Engine.newBuilder()
        .option("engine.WarnInterpreterOnly", "false").build();

    public Context create() {
        Context ctx = Context.newBuilder("js")
            .engine(SHARED_ENGINE)

            .allowIO(false)
            .allowNativeAccess(false)
            .allowCreateThread(false)
            .allowCreateProcess(false)
            .allowHostClassLoading(false)
            .allowPolyglotAccess(PolyglotAccess.NONE)
            .allowEnvironmentAccess(EnvironmentAccess.NONE)
            .allowExperimentalOptions(false)

            .allowHostAccess(HostAccess.newBuilder(HostAccess.EXPLICIT)
                .allowAccessAnnotatedBy(HostAccess.Export.class)
                .allowListAccess(true)
                .allowMapAccess(true)
                .build())
            .allowHostClassLookup(cn -> false)

            .resourceLimits(ResourceLimits.newBuilder()
                .statementLimit(10000, null).build())
            .option("js.ecmascript-version", "2022")
            .option("js.console", "false")
            .option("js.print", "false")
            .option("js.load", "false")
            .option("js.global-arguments", "false")
            .option("js.foreign-object-prototype", "false")
            .option("js.unhandled-rejections", "throw")

            .build();

        Value js = ctx.getBindings("js");
        js.putMember("_util", new ScriptUtil());
        js.putMember("_log",  new ScriptLogger());
        return ctx;
    }
}
```

### 3.3 编译缓存

```java
@Component
public class ScriptCacheManager {

    private final Cache<String, Source> cache = Caffeine.newBuilder()
        .maximumSize(300)
        .expireAfterAccess(Duration.ofMinutes(30))
        .build();

    public Source compileOrGet(String pureJs) {  // 入参是替换后的纯 JS
        String hash = DigestUtils.md5Hex(pureJs).substring(0, 16);
        return cache.get(hash, k ->
            Source.newBuilder("js", pureJs, "script.js").build());
    }
}
```

---

## 4. 脚本节点配置

### 4.1 配置结构

存储在 `FlowVersion.orchestrationConfig.nodes[]`，无 `inputMapping`：

```json
{
  "nodeId": "script_1",
  "nodeType": "script",
  "label": "数据清洗与聚合",
  "data": {
    "scriptContent": "const users = ${$.node.conn_1.output.body.data.users};\nconst total = users.length;\nconst avgAge = users.reduce((s,u) => s + u.age, 0) / total;\n({ total, avgAge });",
    "outputSchema": {
      "total":  { "type": "number" },
      "avgAge": { "type": "number" }
    },
    "timeout": 5,
    "description": "统计用户总数和平均年龄"
  }
}
```

### 4.2 字段说明

| 字段 | 必填 | 说明 |
|------|:---:|------|
| `scriptContent` | ✅ | JS 脚本，可使用 `${$.node.{id}.{input\|output}.path}` 引用 |
| `outputSchema` | ❌ | 出参字段声明（用于发布时类型校验和下游提示） |
| `timeout` | ❌ | 超时秒数，默认 5，最大 30 |
| `description` | ❌ | 节点说明 |

### 4.3 注入到 JS 的内置变量

| 变量 | 类型 | 说明 |
|------|------|------|
| `_util` | `ScriptUtil` 实例 | `_util.md5/ uuid/ base64Encode/ formatDate/ parseJson/ toJson/ sha256/ timestamp` |
| `_log` | `ScriptLogger` 实例 | `_log.info/warn/debug/error(msg)` |

> 💡 不需要 `input` 对象——用户直接用 `${$.node.xxx}` 引用，替换后脚本中就是纯字面量值。
> 💡 不需要 `ctx` 对象——需要什么字段就在脚本中写 `${$.node.xxx}` 引用。

### 4.4 约束

| 约束 | 值 |
|------|:---:|
| 每流最多脚本节点 | 10 |
| 脚本最大长度 | 10000 字符 |
| 默认超时 | 5s（可配 1~30s） |
| 语句上限 | 10000 条 |
| 沙箱违规 | `PolyglotException` → 节点失败 |

---

## 5. 执行流程

### 5.1 运行时执行

脚本节点的执行由 DAG 调度器触发，整体流程如下：

```
DAG 调度器（Event Loop 线程）
  │
  └→ ScriptNodeExecutor.execute(scriptContent, ctx, timeout)
       │                              ↑ 返回 Mono<Map>，不阻塞 Event Loop
       │
       └→ [boundedElastic 线程池]     ← .subscribeOn(Schedulers.boundedElastic())
            │
            ├─ ① 字符串替换：扫描 ${$.node.xxx} → 解析值 → JS 字面量
            ├─ ② 编译/缓存：Source.newBuilder("js", pureJs, ...).build()
            ├─ ③ 沙箱执行：Context.eval(source)  →  Value
            └─ ④ 类型转换：Value.as(Map.class)   →  Map<String, Object>
```

**WebFlux 集成要点**：

- 脚本执行为阻塞操作（GraalJS `Context.eval()` 是同步的），必须通过 `Mono.fromCallable()` 隔离到 `boundedElastic` 线程池，释放 Event Loop 处理其他请求
- 超时通过 `Mono.timeout(Duration)` 控制，超时后 Mono 以 `TimeoutException` 终止
- 每执行完一次脚本，`Context` 立即 `close()` 释放 GraalVM 资源，不留残留状态

核心调用链（片段）：

```java
Mono.fromCallable(() -> {
    String pureJs = replacer.replace(scriptContent, ctx);      // ① 替换
    Source source = cacheManager.compileOrGet(pureJs);         // ② 编译
    try (Context c = contextFactory.create()) {                // ③ 执行
        Value v = c.eval(source);
        return v.isNull() ? Map.of() : v.as(Map.class);       // ④ 转换
    }
})
.subscribeOn(Schedulers.boundedElastic())
.timeout(timeout);
```

### 5.2 发布时校验

连接流版本发布时，对每个 `scriptContent` 进行语法校验：

1. 将所有 `${$.xxx}` 引用**替换为 `null`**（此时无运行时上下文，仅校验 JS 语法）
2. 调用 `Source.newBuilder("js", ...).build()` 做 parse
3. parse 成功 = 语法正确；`PolyglotException` = 语法错误 → 拒绝发布

> 💡 发布时不校验引用有效性（字段是否存在、路径是否正确）——这些在运行时自然暴露：引用解析失败 → 替换为 `null` + WARN 日志，不阻塞执行。

---

## 6. Java 工具类暴露给 JS

### 6.1 ScriptUtil

```java
public class ScriptUtil {

    @HostAccess.Export
    public Map<String, Object> parseJson(String json) { /* Jackson */ }

    @HostAccess.Export
    public String toJson(Map<?, ?> map) { /* Jackson */ }

    @HostAccess.Export
    public String base64Encode(String input) { /* Base64 */ }

    @HostAccess.Export
    public String base64Decode(String input) { /* Base64 */ }

    @HostAccess.Export
    public String md5(String input) { /* MessageDigest */ }

    @HostAccess.Export
    public String sha256(String input) { /* MessageDigest */ }

    @HostAccess.Export
    public String uuid() { return UUID.randomUUID().toString(); }

    @HostAccess.Export
    public long timestamp() { return System.currentTimeMillis(); }

    @HostAccess.Export
    public String formatDate(long timestamp, String pattern) { /* DateTimeFormatter */ }
}
```

### 6.2 自定义业务类

```java
public class CustomValidator {

    @HostAccess.Export
    public boolean checkPhone(String phone) {
        return phone != null && phone.matches("^1[3-9]\\d{9}$");
    }

    @HostAccess.Export
    public String orderGrade(double amount) { /* VIP/Gold/Standard */ }
}
```

注册到 Context（在 `GraalJSContextFactory.create()` 中追加）：

```java
// 自定义业务类随 bindings 注入
js.putMember("_customValidator", new CustomValidator());
```

### 6.3 JS 中调用示例

```javascript
// 工具方法
const hash  = _util.md5(${$.node.trigger.input.body.userId});
const now   = _util.formatDate(Date.now(), 'yyyy-MM-dd HH:mm:ss');

// 自定义业务类
const phone  = ${$.node.trigger.input.body.phone};
const amount = ${$.node.conn_1.output.body.totalAmount};
const grade  = _customValidator.orderGrade(amount);

// 日志
_log.info('Order: ' + phone + ', grade=' + grade);

({ hash, grade, time: now });
```

---

## 7. 错误处理与监控

### 7.1 错误类型

| 错误 | 异常 | 处理 |
|------|------|------|
| JS 语法错误 | `PolyglotException`（编译期） | 发布时拦截 |
| 引用解析失败 | `null` + WARN 日志 | 不阻塞，脚本自行判断空值 |
| 沙箱违规 | `PolyglotException: Access denied` | 节点失败 |
| 语句超限 | `ResourceLimitExceededException` | 节点失败 |
| 执行超时 | `TimeoutException` | 错误处理节点按策略处理 |

### 7.2 监控指标

```
script.execution.count        tag: flowId, nodeId
script.execution.duration     P50/P95/P99
script.execution.timeout
script.execution.sandbox_error
script.template.replace_fail  引用解析失败次数
script.compilation.cache_hit
```

### 7.3 熔断

```
单节点连续超时 5 次 → 版本失效 + 告警
全平台超时率 > 10%（5min 窗口）→ 告警
```

---

## 8. 风险

| 风险 | 等级 | 缓解 |
|------|:---:|------|
| GraalJS 沙箱逃逸 CVE | 🟡 | 24.1.x LTS，跟踪 Oracle 安全公告 |
| GraalJS ~30MB 体积 | 🟢 | 仅 connector-api 引入 |
| `${}` 替换后 JS 语法错误 | 🟢 | `JSON.stringify` 保证字面量安全；脚本编辑器实时预览 |
| 引用字段不存在 | 🟢 | 替换为 `null` + 日志，脚本自行 `??` 兜底 |
| JS number ↔ double 精度 | 🟡 | 大整数用 `_util` 字符串方法 |

---

## 9. 版本规划

| 阶段 | 范围 |
|------|------|
| Phase 1 | `ScriptTemplateReplacer` + `GraalJSContextFactory` + `ScriptCacheManager` |
| Phase 2 | `ScriptNodeExecutor` + DAG 集成 + 超时 + 错误处理 |
| Phase 3 | 自定义业务类体系 + ScriptUtil 完善 + 发布时校验 |
| Phase 4 | 监控 + 熔断 + 编辑器实时预览 |

---

## 附录 A：完整 JS 脚本示例

### A.1 列表聚合

```javascript
const users = ${$.node.conn_1.output.body.data.users};
const total  = users.length;
const avgAge = users.reduce((sum, u) => sum + u.age, 0) / total;

({ total, avgAge });
```

### A.2 数据脱敏 + 条件路由

```javascript
const phone  = ${$.node.trigger.input.body.phone};
const amount = ${$.node.conn_1.output.body.totalAmount};
const name   = ${$.node.trigger.input.body.sender};

const grade = _customValidator.orderGrade(amount);
const masked = phone.substring(0,3) + '****' + phone.substring(7);
const hash   = _util.md5(phone + amount);

_log.info(`${name}: grade=${grade}`);

({ name, grade, masked, hash });
```

### A.3 分组统计（ES2022）

```javascript
const items = ${$.node.conn_1.output.body.data.items} ?? [];

const byCategory = items.reduce((groups, item) => {
    (groups[item.category] ??= []).push(item);
    return groups;
}, {});

const total = items.length;
const last  = items.at(-1);

({ total, lastItem: last?.name, byCategory });
```

### A.4 复杂计算

```javascript
const orders  = ${$.node.conn_1.output.body.orders};
const history = ${$.node.conn_2.output.body.history};

const totalRevenue = orders.reduce((s, o) => s + o.price * o.qty, 0);
const avgHistory   = history.length > 0
    ? history.reduce((s, h) => s + h, 0) / history.length
    : 0;

let tier;
if (totalRevenue >= 100000 && avgHistory >= 90) tier = 'S';
else if (totalRevenue >= 10000)                  tier = 'A';
else                                             tier = 'B';

({ totalRevenue, avgHistory, tier });
```

---

## 附录 B：修订记录

| 版本 | 日期 | 修订内容 |
|------|------|---------|
| v1.0 | 06-17 | 初始：Groovy 方案，三种形态 + 脚本库 |
| v2.0 | 06-17 | 翻转为 GraalJS；新增 WebFlux 架构、HostAccess 示例 |
| v3.0 | 06-17 | 精简：移除脚本库/数据库表/API，仅保留内联脚本节点 |
| v4.0 | 06-17 | **移除 inputMapping，改为脚本内直接写 `${$.node.xxx}` 引用，运行时字符串替换** |

---

**文档状态**: 📝 初稿（draft，v4.0）
