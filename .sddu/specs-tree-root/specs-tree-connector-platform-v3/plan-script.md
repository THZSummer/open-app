# 脚本执行引擎设计：连接器平台 V3

**Feature ID**: CONN-PLAT-003
**关联文档**: plan.md（主技术规划）、plan-json-schema.md（值表达式体系 §3）、plan-runtime.md（运行时引擎）
**版本**: v8.1-draft
**创建日期**: 2026-06-17
**最近更新**: 2026-06-18（function main(ctx) 标准函数 + ctx 参数 + 安全审计）

---

## 0. 概述

### 0.1 背景与动机

V2 的数据处理节点仅支持 4 种类型转换函数。V3 引入**脚本节点**，用户编写标准 JavaScript（ES2022），通过函数参数 `ctx` 按 `ctx.{nodeId}.{input|output}.field` 路径访问任意上游节点数据，交由 GraalJS 执行。

### 0.2 核心设计

| # | 决策 | 说明 |
|:---:|------|------|
| 1 | **ctx 函数参数** | 上游节点数据组装为 Map，作为 `main(ctx)` 的函数参数传入，非全局注入 |
| 2 | **标准函数声明** | `function main(ctx) { ... return ... }`，纯 JS，无模板语法，IDE/Linter 零配置 |
| 3 | **GraalJS 引擎** | ES2022，沙箱五层防护，`HostAccess.EXPLICIT` |
| 4 | **WebFlux 非阻塞** | `Mono.fromCallable().subscribeOn(boundedElastic)` |
| 5 | **配置即存储** | 脚本源码存于 `FlowVersion.orchestrationConfig` JSON，零新表 |

### 0.3 执行模型

```
用户写的标准函数：function main(ctx) { ... return ... }
      ↓ GraalJS Context.eval() → 注册 main 函数到全局
      ↓ c.eval("js", "main").execute(ctx)  →  this=undefined
返回值（JS object → Java Map）
```

### 0.4 脚本格式约定

```javascript
function main(ctx) {
    const users = ctx.conn_1.output.body.data.users;
    return { total: users.length };
}
```

> 💡 脚本固定为 `function main(ctx) { ... return ... }` 格式。`ctx` 是上游节点数据的函数参数，`return` 显式输出。完整格式说明见 [§3.1](#31-配置结构)。

### 0.5 Spring Boot + WebFlux + GraalJS 关系

```
Event Loop（非阻塞）            boundedElastic（阻塞隔离）
┌─────────────────┐          ┌──────────────────────────┐
│ HTTP 路由/解析   │  Mono    │ ScriptNodeExecutor       │
│ 响应序列化       │←─────── │ ① 组装 ctx Map            │
│                 │  回调    │ ② eval 注册 main 函数      │
└─────────────────┘          │ ③ eval("js","main").execute(ctx) │
                             │ ④ Value → Map            │
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
| 数据访问 | `ctx.{nodeId}.{input\|output}.field` — 标准 JS 属性访问，`ctx` 为 `main` 函数参数 |

---

## 2. 数据访问 — ctx 函数参数

### 2.1 原理

运行时将上游所有节点的输入/输出组装为一个嵌套 `Map<String, Object>`，作为 `main` 函数的 `ctx` 参数传入。用户的 `scriptContent` 是标准函数声明 `function main(ctx) { ... }`，运行时 eval 后通过 `c.eval("js", "main")` 安全获取并调用——`this = undefined`。

**两步流程**：

```
① 运行时组装 ctx（Java 层，指针引用，零深拷贝）
   ctx = {
     "trigger": { "input": { "body": { "phone": "138...", "sender": "Alice" } } },
     "conn_1":  { "output": { "body": { "data": { "users": [...] } } } }
   }

② 运行时 eval 脚本 → 安全获取 main → 调用 main(ctx)
   scriptContent = "function main(ctx) { ... return ... }"  ← 用户显式定义
       ↓ Context.eval(source)                                ← 注册 main 到全局
       ↓ c.eval("js", "main").execute(ctx)                   ← this=undefined，安全
   const users = ctx.conn_1.output.body.data.users;          ← 标准 JS 属性访问
   return { total: users.length };                           ← return 显式输出
```

**关键要点**：

- `ctx` 作为**函数参数**传入——函数签名在源码中显式可见，读代码即知数据来源
- `return` 语句显式输出——语义明确，IDE 可校验返回类型
- 函数作用域隔离——脚本内变量不污染全局
- `_util` 和 `_log` 保持全局绑定（`js.putMember()`），在函数内通过作用域链自然可用

### 2.2 ctx 结构规范

```
ctx
├── {nodeId}              ← 上游节点 ID（trigger、conn_1、script_1 等）
│   ├── input             ← 节点的入参（input 分支始终存在，可能为 {}）
│   │   ├── body          ← HTTP body / 消息体
│   │   ├── headers       ← HTTP headers / 元数据
│   │   ├── params        ← URL params / 路径参数
│   │   └── ...           ← 其他协议特定字段
│   └── output            ← 节点的出参（仅已执行节点有，未执行节点不存在）
│       ├── body          ← 响应体
│       ├── statusCode    ← HTTP 状态码 / 执行状态
│       └── ...           ← 其他协议特定字段
└── ...
```

**访问示例**：

```javascript
// 基础属性访问
const users = ctx.conn_1.output.body.data.users;
const phone = ctx.trigger.input.body.phone;

// 可选链（字段可能不存在时）
const sender = ctx?.trigger?.input?.body?.sender ?? "unknown";

// 解构
const { orders, totalAmount } = ctx.conn_1.output.body;

// 默认值
const items = ctx.conn_1.output.body.data?.items ?? [];
```

### 2.3 类型映射

GraalJS 自动完成 Java → JS 类型映射，用户无感知：

| Java 类型 | JS 类型 | 示例 |
|------|:---:|------|
| `java.util.Map` | `object` | `ctx.conn_1.output.body` |
| `java.util.List` / `Array` | `Array` | `ctx.conn_1.output.body.data.users` |
| `String` | `string` | `ctx.trigger.input.body.phone` |
| `Integer` / `Long` / `BigDecimal` | `number` | `ctx.conn_1.output.statusCode` |
| `Boolean` | `boolean` | `ctx.conn_1.output.body.success` |
| `null`（字段不存在） | `undefined` | `ctx.conn_1.output.body.nonexistent` → `undefined` |

> 💡 字段不存在时返回 `undefined`（而非抛异常），用户用可选链 `?.` 和空值合并 `??` 兜底。

### 2.4 ctx 组装实现

```java
// ScriptNodeExecutor 中，执行前组装 ctx（作为函数参数传入，非 putMember 全局绑定）
Map<String, Object> ctx = new LinkedHashMap<>();
for (Map.Entry<String, NodeResult> entry : upstreamResults.entrySet()) {
    String nodeId = entry.getKey();
    NodeResult result = entry.getValue();
    Map<String, Object> nodeView = new LinkedHashMap<>();
    nodeView.put("input",  result.getInput());   // Map — 可能为 Collections.emptyMap()
    nodeView.put("output", result.getOutput());  // Map — 可能为 Collections.emptyMap()
    ctx.put(nodeId, nodeView);
}
// ctx 通过 main.execute(ctx) 传递，详见 §4.1
```

> 💡 不深拷贝——`ctx` 中的 Map/List 直接引用上游节点的原始数据对象。GraalJS `allowMapAccess(true)` + `allowListAccess(true)` 使其在 JS 中表现为只读语义。`ctx` 是 `main` 的函数参数，非 `putMember` 全局注入。

---

## 3. 脚本节点配置

### 3.1 配置结构

脚本节点配置存储在 `FlowVersion.orchestrationConfig.nodes[]`，无 `inputMapping` 字段：

```json
{
  "nodeId": "script_1",
  "nodeType": "script",
  "label": "数据清洗与聚合",
  "data": {
    "scriptContent": "function main(ctx) {\n  const users = ctx.conn_1.output.body.data.users;\n  const total = users.length;\n  const avgAge = users.reduce((s,u) => s + u.age, 0) / total;\n  return { total, avgAge };\n}",
    "outputSchema": {
      "total":  { "type": "number" },
      "avgAge": { "type": "number" }
    },
    "timeout": 5,
    "description": "统计用户总数和平均年龄"
  }
}
```

> 💡 JSON 中 `scriptContent` 为单行（`\n` 转义），编辑器渲染为多行格式化展示。存储与展示分离。

### 3.1.1 脚本格式（编辑器展示）

上述 `scriptContent` 在编辑器中格式化为：

```javascript
// 固定格式：function main(ctx) { ... return ... }
function main(ctx) {

    // ═══ 读取上游节点数据 ═══
    // ctx.{nodeId}.{input|output}.{field...}
    const users = ctx.conn_1.output.body.data.users;
    const phone = ctx.trigger.input.body.phone;

    // ═══ 业务处理 ═══
    const total  = users.length;
    const avgAge = users.reduce((sum, u) => sum + u.age, 0) / total;
    const hash   = _util.md5(phone);

    // ═══ 日志（可选） ═══
    _log.info(`处理 ${total} 条数据`);

    // ═══ return 输出 ═══
    // 返回的对象将作为节点 output，供下游节点通过 ctx.script_1.output.xxx 访问
    return { total, avgAge, hash };

}
```

**结构约定**：

| 组成部分 | 说明 |
|---------|------|
| `function main(ctx)` | **固定入口签名**。`ctx` 是上游节点数据，`main` 为约定入口名 |
| 函数体 | 任意标准 JS（ES2022），可定义变量、辅助函数、使用 `_util`/`_log` |
| `return { ... }` | **必须有 return 语句**，返回值为节点 output |
| `_util` | 全局工具集：`md5`/`uuid`/`base64Encode`/`sha256`/`timestamp`/`formatDate`/`parseJson`/`toJson` |
| `_log` | 全局日志：`info`/`warn`/`debug`/`error(msg)` |
| `ctx` 路径规则 | `ctx.{nodeId}.{input\|output}.{field.subfield...}`，节点 ID 见画布 |

### 3.2 字段说明

| 字段 | 必填 | 说明 |
|------|:---:|------|
| `scriptContent` | ✅ | 标准函数声明 `function main(ctx) { ... }`，通过 `ctx.{nodeId}.{input\|output}.field` 读数据，`return` 输出 |
| `outputSchema` | ❌ | 出参字段声明（用于发布时类型校验和下游提示） |
| `timeout` | ❌ | 超时秒数，默认 5，最大 30 |
| `description` | ❌ | 节点说明 |

### 3.3 注入到 JS 的内置变量

| 变量 | 类型 | 说明 |
|------|------|------|
| `ctx` | `Map<String, Object>`（**函数参数**） | 上游所有节点的 input/output 数据。用户在 `function(ctx)` 签名中显式声明 |
| `_util` | `ScriptUtil` 实例（全局绑定） | `_util.md5/ uuid/ base64Encode/ formatDate/ parseJson/ toJson/ sha256/ timestamp` |
| `_log` | `ScriptLogger` 实例（全局绑定） | `_log.info/warn/debug/error(msg)` |

> 💡 `ctx` 是函数参数（非全局），`_util`/`_log`/业务类是全局绑定。通过 `c.eval("js","main")` 获取函数引用确保 `this=undefined`，脚本无法通过 `this` 访问全局绑定。

### 3.4 约束

| 约束 | 值 |
|------|:---:|
| 每流最多脚本节点 | 10 |
| 脚本最大长度 | 10000 字符 |
| 默认超时 | 5s（可配 1~30s） |
| 语句上限 | 10000 条 |
| 沙箱违规 | `PolyglotException` → 节点失败 |

---

## 4. 执行流程

### 4.1 运行时执行

脚本节点的执行由 DAG 调度器触发，整体流程如下：

```
DAG 调度器（Event Loop 线程）
  │
  └→ ScriptNodeExecutor.execute(scriptContent, upstreamResults, timeout)
       │                              ↑ 返回 Mono<Map>，不阻塞 Event Loop
       │
       └→ [boundedElastic 线程池]     ← .subscribeOn(Schedulers.boundedElastic())
            │
            ├─ ① 组装 ctx：遍历 upstreamResults → 嵌套 Map
            ├─ ② 编译/缓存：Source.newBuilder("js", scriptContent, ...).build()
            ├─ ③ eval 注册函数：Context.eval(source)  →  注册 main 到全局
            ├─ ④ 安全获取并调用：c.eval("js", "main").execute(ctx)  →  Value
            └─ ⑤ 类型转换：Value.as(Map.class)         →  Map<String, Object>
```

> 💡 **安全要点**：用 `c.eval("js", "main")` 而非 `getMember("main")` 获取函数引用。前者是顶层表达式求值，`this` 在 strict mode 下为 `undefined`；后者是成员访问，`this` 为全局对象——会导致脚本通过 `this._util`/`this._log` 访问到不应暴露的内部绑定。

**WebFlux 集成要点**：

- 脚本执行为阻塞操作（GraalJS `Context.eval()` 是同步的），必须通过 `Mono.fromCallable()` 隔离到 `boundedElastic` 线程池，释放 Event Loop 处理其他请求
- 超时通过 `Mono.timeout(Duration)` 控制，超时后 Mono 以 `TimeoutException` 终止
- 每执行完一次脚本，`Context` 立即 `close()` 释放 GraalVM 资源，不留残留状态

核心调用链（片段）：

```java
Mono.fromCallable(() -> {
    // ① 组装 ctx
    Map<String, Object> ctx = buildCtx(upstreamResults);

    // ② 编译/缓存（scriptContent 已是标准函数声明，无需包装）
    Source source = cacheManager.compileOrGet(scriptContent);

    try (Context c = contextFactory.create()) {
        // ③ 注入全局工具（_util/_log 在函数作用域链上自然可见）
        c.getBindings("js").putMember("_util", scriptUtil);
        c.getBindings("js").putMember("_log", scriptLogger);

        // ④ eval 注册函数 → 安全获取 main → 调用 main(ctx)
        c.eval(source);                                            // 注册 function main 到全局
        Value result = c.eval("js", "main").execute(ctx);          // eval 获取（this=undefined），非 getMember
        // ⑤ 转换
        return result.isNull() ? Map.of() : result.as(Map.class);
    }
})
.subscribeOn(Schedulers.boundedElastic())
.timeout(timeout);
```

### 4.2 发布时校验

连接流版本发布时，对每个 `scriptContent` 进行两层校验：

**第一层：语法校验**

1. 调用 `Source.newBuilder("js", scriptContent, "script.js").build()` 做 parse
2. parse 成功 = 语法正确；`PolyglotException` = 语法错误 → 拒绝发布

**第二层：结构校验（顶层代码检查）**

`function main(ctx) { ... }` 模式的一个安全风险：用户在 `main` 之外写的顶层代码会在 `c.eval(source)` 阶段就执行，先于 `main(ctx)` 调用——可绕过入口管控。

校验规则：

```java
public class ScriptStructureValidator {

    // 正则：整个脚本必须恰好是一个 function main(ctx) { ... } 声明，前后无其他语句
    private static final Pattern MAIN_FUNC_PATTERN = Pattern.compile(
        "^\\s*function\\s+main\\s*\\(\\s*ctx\\s*\\)\\s*\\{.*\\}\\s*$", Pattern.DOTALL);

    public static void validate(String scriptContent) {
        // 1. 必须是合法的 function main 声明
        if (!MAIN_FUNC_PATTERN.matcher(scriptContent.trim()).matches()) {
            throw new ValidationException(
                "脚本格式错误：必须为 'function main(ctx) { ... }' 函数声明，" +
                "不允许在函数外写任何代码（包括变量声明、表达式、注释外的语句）");
        }
        // 2. 检查括号嵌套深度，防止 } 后拼接恶意代码
        if (!hasBalancedBraces(scriptContent)) {
            throw new ValidationException("脚本格式错误：花括号不匹配");
        }
    }

    private static boolean hasBalancedBraces(String code) {
        int depth = 0;
        for (char c : code.toCharArray()) {
            if (c == '{') depth++;
            if (c == '}') depth--;
            if (depth < 0) return false;
        }
        return depth == 0;
    }
}
```

**拦截示例**：

| 脚本 | 结果 |
|------|:---:|
| `function main(ctx) { return {}; }` | ✅ 通过 |
| `var x = 1; function main(ctx) { return {}; }` | ❌ 拦截——函数外有变量声明 |
| `function main(ctx) { return {}; } while(true){}` | ❌ 拦截——函数后有代码 |
| `function main(ctx) { return {}; } // ok` | ✅ 通过——注释允许 |
| `function main(ctx) { ... } function evil(){}` | ❌ 拦截——额外函数声明 |

> 💡 此校验在**发布时**执行（非运行时），零性能开销。如脚本中确实需要辅助函数，写在 `main` **内部**即可。

---

## 5. Java 工具类暴露给 JS

### 5.1 ScriptUtil

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

### 5.2 自定义业务类

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

### 5.3 JS 中调用示例

```javascript
function main(ctx) {
    // 工具方法
    const hash  = _util.md5(ctx.trigger.input.body.userId);
    const now   = _util.formatDate(Date.now(), 'yyyy-MM-dd HH:mm:ss');

    // 自定义业务类
    const phone  = ctx.trigger.input.body.phone;
    const amount = ctx.conn_1.output.body.totalAmount;
    const grade  = _customValidator.orderGrade(amount);

    // 日志
    _log.info('Order: ' + phone + ', grade=' + grade);

    return { hash, grade, time: now };
}
```

---

## 6. 版本规划

| 阶段 | 范围 |
|------|------|
| Phase 1 | `ScriptCtxBuilder`（ctx 组装） + `GraalJSContextFactory` + `ScriptCacheManager` |
| Phase 2 | `ScriptNodeExecutor` + DAG 集成 + 超时 + 错误处理 |
| Phase 3 | 自定义业务类体系 + ScriptUtil 完善 + 发布时校验（含 `ScriptStructureValidator`） |
| Phase 4 | 监控 + 熔断 + 编辑器实时预览 |

---

## 7. 安全执行 JavaScript — Spring Boot + WebFlux + GraalJS 全链路纵深防御

### 7.1 威胁模型与攻击面

在多租户连接器平台中，脚本节点是**用户可控代码的唯一入口**。攻击面分析：

| 攻击向量 | 风险 | 影响范围 |
|---------|------|:---:|
| 恶意无限循环 / 死循环（函数内或函数外） | 资源耗尽（CPU） | 整个 JVM 响应变慢，影响其他租户 |
| **函数外顶层代码执行**（`eval` 阶段，`main` 调用前） | 🔴 绕过 `main` 入口管控 | 脚本中 `function main` 外的代码在 eval 时即执行，可先于 main 消耗资源 |
| 内存炸弹（大数组/大字符串） | 资源耗尽（Heap） | OOM，JVM 崩溃 |
| Java 互操作逃逸（`Java.type`） | 沙箱突破 | 执行系统命令、读写文件、网络外连 |
| **`this` 绑定泄露**（`getMember("main").execute()` 模式） | 暴露全局作用域 | `this._util`、`this._log` 可被脚本内任意访问，扩大攻击面 |
| 原型链污染（`__proto__`/`constructor`） | JS 引擎级漏洞 | 污染全局对象，影响后续脚本执行 |
| CVE 沙箱逃逸（GraalJS 已知漏洞） | 完整沙箱突破 | 等同于以应用身份执行任意代码 |
| 脚本过大（编译期 DoS） | GraalJS 解析器 CPU 尖峰 | 单次请求阻塞 boundedElastic 线程数秒 |
| 超长执行时间 | 线程池耗尽 | boundedElastic 所有线程阻塞，后续请求被拒绝 |

> **安全目标**：即使脚本由恶意用户精心构造，也不影响平台其他租户的**可用性**和**数据安全**。

### 7.2 纵深防御架构（五层）

```
┌──────────────────────────────────────────────────────────────────┐
│  第 5 层：运行时监控与自适应熔断                                     │
│  → Prometheus 指标 + 连续超时熔断 + 全平台异常率告警                  │
├──────────────────────────────────────────────────────────────────┤
│  第 4 层：WebFlux 线程隔离与 Context 生命周期安全                      │
│  → boundedElastic 阻塞隔离 + try-with-resources 释放 + 泄漏检测       │
├──────────────────────────────────────────────────────────────────┤
│  第 3 层：ResourceLimits 资源配额                                   │
│  → statementLimit(10000) + 超时双层控制 + 禁用内置函数                │
├──────────────────────────────────────────────────────────────────┤
│  第 2 层：HostAccess 白名单（Java 互操作控制）                        │
│  → HostAccess.EXPLICIT + allowHostClassLookup(false) + 无反射       │
├──────────────────────────────────────────────────────────────────┤
│  第 1 层：Context 权限开关（OS 级能力阻断）                            │
│  → IO / 线程 / 进程 / Native / Polyglot / Environment 全部关闭       │
└──────────────────────────────────────────────────────────────────┘
```

> **设计原则**：每一层独立失效不影响其他层。攻击者必须**同时突破所有五层**才能造成实际危害。

### 7.3 输入安全 — ctx 数据完整性

与 `${}` 模板替换方案不同，`ctx` 方案中数据全程以 Java 对象流转（Map/List/String），不经字符串拼接，**天然免疫注入**：

| 对比维度 | `${}` 模板替换 | `ctx` 函数参数 |
|---------|:---:|:---:|
| 数据传输方式 | 值 → JSON 字符串 → 拼入脚本源码 → GraalJS 重新解析 | Java 对象 → GraalJS 类型映射 → JS object |
| 注入风险 | ⚠️ 需 `JSON.stringify` 防注入 + 路径正则白名单 | ✅ 零——对象引用不经字符串 |
| 路径遍历 | ⚠️ 需 `ScriptRefValidator` 正则拦截 | ✅ 路径即 JS 属性访问，由 GraalJS 拦截非法字符 |
| 脚本长度限制 | 需要（替换后可能膨胀） | 需要（防编译 DoS）——脚本最大 10000 字符 |
| `this` 泄露 | 不适用 | ✅ 通过 `c.eval("js","main")` 获取函数，`this=undefined` |

**ctx 组装安全**：
- `ctx` 中的数据来自上游节点的 `Map<String, Object>` output，已在节点执行时校验
- 组装过程只做 `put` 引用传递，不引入新数据源
- GraalJS `allowMapAccess(true)` 提供只读语义的对象访问，JS 无法修改原始 Java Map
- 跨租户数据隔离在上游调度层保证——同一脚本只能访问同一连接流版本内的节点输出

### 7.4 第 1 层：Context 权限开关（OS 级能力阻断）

GraalJS `Context` 提供八维权限开关，在最外层直接阻断危险能力。即使后续防线全部失效，这些开关也会在 GraalVM 内部拦截：

```java
Context ctx = Context.newBuilder("js")
    .allowIO(false)                              // 文件系统、网络 IO — 全禁
    .allowNativeAccess(false)                    // JNI / Native 接口
    .allowCreateThread(false)                    // new Thread() / Web Worker
    .allowCreateProcess(false)                   // Runtime.exec() / ProcessBuilder
    .allowPolyglotAccess(PolyglotAccess.NONE)    // 跨语言调用（Python/R/LLVM）
    .allowEnvironmentAccess(EnvironmentAccess.NONE)  // System.getenv()
    .allowHostClassLoading(false)                // Class.forName() 动态加载
    .allowExperimentalOptions(false)             // 非稳定 API
    .build();
```

**逐项攻防分析**：

| 权限开关 | 如果打开 | 攻击后果 | 决策 |
|---------|---------|---------|:---:|
| `allowIO` | 可读写文件系统、网络连接 | 读取 `/etc/passwd`、写入 WebShell、SSRF 外连 | **必须关闭** |
| `allowNativeAccess` | 调用 JNI 本地代码 | 绕过所有 Java 层安全控制，直接执行 native code | **必须关闭** |
| `allowCreateThread` | `new Thread()`、Worker | 耗尽 OS 线程；绕过 `statementLimit`（每个线程独立计数） | **必须关闭** |
| `allowCreateProcess` | `Runtime.exec()` | `rm -rf /`、反弹 shell、`curl evil.com` | **必须关闭** |
| `allowPolyglotAccess` | 调用 Python/R/LLVM 引擎 | 通过其他语言 Runtime 间接访问系统资源 | **必须关闭** |
| `allowHostClassLoading` | `Class.forName()` | 加载任意 Java 类，反射调用 `java.lang.Runtime` | **必须关闭** |
| `allowEnvironmentAccess` | 读取环境变量 | 泄露 `DATABASE_URL`、`AWS_SECRET_KEY`、`SPRING_PROFILES` | **必须关闭** |
| `allowExperimentalOptions` | 启用实验性 API | 行为未定义，可能包含未审计的安全缺陷 | **必须关闭** |

> 💡 **Shared Engine 的安全性**：`Engine` 实例是线程安全且可共享的（JIT 编译产物跨请求复用），但所有安全策略定义在 `Context` 层面。共享 Engine **不会**导致安全策略泄漏——每个 `Context` 有完全独立的权限边界。

### 7.5 第 2 层：HostAccess 白名单（Java 互操作控制）

即使第 1 层被绕过（例如 GraalJS CVE），第 2 层确保 JS 代码拿到的任何 Java 对象都无法调用危险方法。

#### 7.5.1 HostAccess.EXPLICIT 模式

```java
// 关键配置
.allowHostAccess(HostAccess.newBuilder(HostAccess.EXPLICIT)  // 白名单模式
    .allowAccessAnnotatedBy(HostAccess.Export.class)         // 仅 @HostAccess.Export 方法可调用
    .allowListAccess(true)    // 允许 JS 以 item[0] 访问 Java List（只读语义）
    .allowMapAccess(true)     // 允许 JS 以 map.key 访问 Java Map（只读语义）
    .build())

.allowHostClassLookup(cn -> false)  // 禁止 JS 中通过 "Java.type()" 查找任意 Java 类

.option("js.foreign-object-prototype", "false")  // 禁止 JS 修改 Java 对象的 prototype
```

**HostAccess.EXPLICIT vs ALL 对比**：

| 模式 | JS 可调用 | 风险 |
|------|----------|------|
| `ALL`（默认） | **任何** Java 对象的 **所有** public 方法 | JS 拿到任意对象即可链式调用 `.getClass().forName("java.lang.Runtime").getMethod("exec")...` |
| `EXPLICIT` | 仅 `@HostAccess.Export` 标记的方法 | 白名单严格受控，攻击者无法调用未标记的方法 |

#### 7.5.2 注入到 JS 的变量安全性审计

| 变量 | 注入方式 | 暴露内容 | 风险评估 |
|---------|:---:|---------|:---:|
| `ctx` | **函数参数** `main(ctx)` | 上游节点 input/output 的 Map 引用（只读） | ✅ 参数传递，不进全局。`allowMapAccess` 提供只读语义 |
| `_util` | 全局绑定 `putMember` | `md5/uuid/base64/...` 纯函数 | ✅ 纯函数，无副作用，无 I/O |
| `_log` | 全局绑定 `putMember` | `info/warn/debug/error(msg)` | ✅ 仅写日志，无数据外泄 |
| `CustomValidator` | 全局绑定 `putMember` | 业务自定义方法 | ⚠️ **必须审计**：不暴露数据库连接、HTTP 客户端、内部服务端点 |

**`this` 绑定安全**：

`getMember("main").execute(ctx)` 会导致 `this` = 全局对象，脚本可通过 `this._util` 访问全局绑定。**必须使用** `c.eval("js", "main").execute(ctx)`——顶层 eval 获取函数引用，strict mode 下 `this = undefined`。

**绝对禁止向 JS Context 暴露的 Java 类型**：
- ❌ `Class` / `Method` / `Field` / `Constructor`（反射入口）
- ❌ `Thread` / `Runnable` / `ExecutorService` / `ThreadPoolExecutor`
- ❌ `File` / `InputStream` / `OutputStream` / `Socket` / `URL`
- ❌ `System` / `Runtime` / `ProcessBuilder`
- ❌ `ClassLoader` / `Thread.currentThread().getContextClassLoader()`
- ❌ `DataSource` / `JdbcTemplate` / `RestTemplate` / `WebClient`
- ❌ Spring `ApplicationContext` / `BeanFactory`
- ❌ 任何能访问文件系统、网络、系统属性的对象

> 即使 GraalJS 沙箱被完全突破，攻击者能接触的 Java 对象列表也只有 `_util` + `_log` + 已审计的业务类，其 `@HostAccess.Export` 方法均为纯函数或日志输出——**零系统级操作能力**。

### 7.6 第 3 层：ResourceLimits 资源配额

防御资源耗尽型攻击（DoS）。

#### 7.6.1 语句计数限制

```java
.resourceLimits(ResourceLimits.newBuilder()
    .statementLimit(10000, null)  // 达到上限 → ResourceLimitExceededException
    .build())
```

`statementLimit` 的计数范围：

| 计数 | 不计数 |
|------|-------|
| 普通语句、表达式 | 注释 |
| 循环每次迭代（`for`/`while`/`do-while`） | 空行 |
| 函数调用（包括递归每次调用） | 已声明但未调用的函数体 |
| 条件分支（`if`/`switch` 每个 case） | — |

> **为什么是 10000？** 正常数据处理脚本通常 < 500 条语句。10000 给 20x 安全余量，同时保证：即使 `while(true){}` 也会在 10000 次迭代后终止，单次执行通常 < 50ms。

#### 7.6.2 选项级限制

```java
.option("js.ecmascript-version", "2022")          // 版本锁定，禁止实验性语法
.option("js.console", "false")                     // 禁用 console.log/warn/error
.option("js.print", "false")                       // 禁用 print()
.option("js.load", "false")                        // 禁用 load() 加载外部文件
.option("js.global-arguments", "false")            // 禁用 arguments 对象（严格模式隐含）
.option("js.foreign-object-prototype", "false")    // 禁止修改 Java 对象原型链
.option("js.unhandled-rejections", "throw")        // Promise 未处理拒绝 → 抛异常（避免静默吞错）
```

#### 7.6.3 双层时间超时

```
第 1 层：statementLimit → 限制 CPU 时间（执行步数上限）
第 2 层：Mono.timeout(Duration) → 限制挂钟时间（WebFlux 层面）
```

两层互补：`statementLimit` 防止 CPU 密集型死循环，`Mono.timeout` 防止 GraalJS 内部 hang 或 GC 暂停导致的长时间不返回。

```java
Mono.fromCallable(() -> context.eval(source))
    .subscribeOn(Schedulers.boundedElastic())
    .timeout(Duration.ofSeconds(timeout));  // 单脚本 1~30s，默认 5s
```

#### 7.6.4 堆内存间接限制

Context 级别的堆内存硬限制在 GraalJS 24.1 中为实验性功能。当前通过以下组合手段间接防御内存炸弹：

| 手段 | 作用 |
|------|------|
| `boundedElastic` 线程池上限（`size=10`） | 同时执行脚本数有硬上限 |
| 脚本长度 ≤ 10000 字符 | 防止超大脚本编译阶段 OOM |
| 每次执行后 `Context.close()` | 立即释放 Context 占用的堆内存（AST、IR、JIT 缓存） |
| JVM `-XX:MaxHeapFreeRatio=30` | 防止堆内存碎片化导致 GC 效率下降 |

### 7.7 第 4 层：WebFlux 线程隔离与 Context 生命周期安全

Spring Boot WebFlux 基于 Netty Event Loop（非阻塞 I/O 线程）。GraalJS 的 `Context.eval()` 是**同步阻塞**调用——如果在 Event Loop 线程执行，会直接阻塞整个 Reactor Netty，导致所有 HTTP 请求超时。

#### 7.7.1 线程隔离原理

```
Event Loop（非阻塞）                   boundedElastic（阻塞隔离）
┌─────────────────────┐              ┌──────────────────────────────┐
│ reactor-http-nio-X  │  发布 Mono   │ boundedElastic-X             │
│                     │─────────────→│ ① 组装 ctx Map               │
│ 绝不执行阻塞操作      │  订阅回调     │ ② Source 编译/缓存命中        │
│ 处理 HTTP 路由、序列化 │←─────────────│ ③ Context.eval(source) 执行  │
│                     │              │ ④ eval("js","main").execute(ctx) │
└─────────────────────┘              │ ⑤ Context.close() 释放资源    │
                                     └──────────────────────────────┘
```

```java
// ✅ 正确：阻塞操作通过 subscribeOn 隔离到 boundedElastic
Mono.fromCallable(() -> executorService.execute(pureJs))
    .subscribeOn(Schedulers.boundedElastic())
    .timeout(Duration.ofSeconds(timeout));

// ❌ 错误：直接在 Event Loop 线程执行
Mono.just(executorService.execute(pureJs));
// → Event Loop 阻塞 → 整个服务不可用
```

#### 7.7.2 boundedElastic 安全配置

```yaml
# application.yml — connector-api
spring:
  reactor:
    boundedElastic:
      size: 10              # 最大并发脚本执行数
      queueSize: 100        # 有界等待队列；超出抛 RejectedExecutionException
      ttl: 60s              # 空闲线程存活时间
```

**为什么需要有界队列？** 无界队列下，1000 个并发请求会全部入队，最终耗尽内存。有界队列 + 固定线程数 = 天然背压，保护 JVM。

#### 7.7.3 Context 生命周期安全

```java
// ✅ 正确：try-with-resources 保证 Context 必然 close
try (Context ctx = contextFactory.create()) {
    Value result = ctx.eval(source);
    return result.as(Map.class);
}
// ← 此处自动调用 ctx.close()，释放 GraalVM IR 缓存、JIT 产物、绑定对象引用

// ❌ 错误：忘记 close → GraalVM 内存持续泄漏
Context ctx = contextFactory.create();
Value result = ctx.eval(source);
return result.as(Map.class);  // ctx 未关闭！
```

**close() 释放什么？**
- GraalVM IR 中间表示缓存
- JIT 编译产物（Code Cache）
- 绑定的 Java 对象引用（解除 GC Root）
- Polyglot 内部状态（语言间通信管道）
- 本线程的 ThreadLocal 缓存（GraalJS 内部用于编译优化）

**ThreadLocal 泄漏防护**：GraalJS 内部使用 ThreadLocal 缓存编译和优化数据。`boundedElastic` 的线程复用不会导致跨请求数据泄漏，因为：
1. 每个请求创建**新的** `Context` 实例（不共享 Context）
2. `Context.close()` 清理当前线程的 ThreadLocal 缓存
3. `Source` 编译结果缓存在无状态的 `ScriptCacheManager` 中（线程安全）

#### 7.7.4 Context 泄漏检测

```java
@Component
public class GraalJSContextFactory {

    private final AtomicInteger activeContexts = new AtomicInteger(0);

    public Context create() {
        activeContexts.incrementAndGet();
        Context ctx = Context.newBuilder("js")
            // ... 安全配置 ...
            .build();
        // 包装 close 方法追踪泄漏
        return new ContextWrapper(ctx, () -> {
            activeContexts.decrementAndGet();
            ctx.close();
        });
    }

    public int getActiveContextCount() {
        return activeContexts.get();
    }
}
```

暴露健康端点 `/actuator/health/graaljs`：
- `activeContexts = 0~2` → 正常
- `activeContexts = 10` 且持续不降 → **Context 泄漏** → 告警

#### 7.7.5 编译缓存安全

```java
@Component
public class ScriptCacheManager {

    private final Cache<String, Source> cache = Caffeine.newBuilder()
        .maximumSize(300)
        .expireAfterAccess(Duration.ofMinutes(30))
        .build();

    public Source compileOrGet(String pureJs) {  // 入参是 scriptContent（纯 JS，无替换）
        String hash = DigestUtils.md5Hex(pureJs).substring(0, 16);
        return cache.get(hash, k ->
            Source.newBuilder("js", pureJs, "script.js").build());
    }
}
```

> 💡 缓存 `Source` 对象而非 `Context`：`Source` 是编译产物（AST/IR），无状态、线程安全、可跨请求共享。`Context` 是有状态沙箱实例，必须每次新建。

### 7.8 第 5 层：运行时监控与自适应熔断

#### 7.8.1 监控指标体系

所有指标通过 Micrometer → Prometheus → Grafana 暴露：

```
# 执行统计
script.execution.count            tag: flowId, nodeId, status(success|failure|timeout)
script.execution.duration_ms      P50/P95/P99（Micrometer Timer）

# 安全事件
script.execution.sandbox_error    tag: errorType(PolyglotException|ResourceLimitExceededException)
script.context.active            当前活跃 Context 数（Gauge，持续增长 = 泄漏）

# 资源健康
script.execution.rejected         boundedElastic 线程池拒绝次数（Counter）
script.compilation.cache_hit      编译缓存命中率（Gauge）
```

#### 7.8.2 错误分类与响应

| 错误类型 | 异常 | 安全级别 | 处理策略 |
|---------|------|:---:|---------|
| JS 语法错误 | `PolyglotException`（编译期） | 低 | **发布时拦截**（§4.2），不允许进入运行时 |
| 脚本结构违规 | `ValidationException`（发布时） | 低 | **发布时拦截**——非 `function main(ctx){}` 格式或函数外有代码 |
| 沙箱违规 | `PolyglotException: Access denied` | 🔴 **高** | 节点失败 + **立即安全告警**（可能为攻击行为） |
| 语句超限 | `ResourceLimitExceededException` | 🟡 中 | 节点失败 + 限流告警 |
| 执行超时 | `TimeoutException` | 🟡 中 | 节点失败 + 触发熔断计数 |
| 线程池拒绝 | `RejectedExecutionException` | 🟡 中 | 返回 HTTP 503 + 背压告警 |
| Context 泄漏 | 堆内存持续增长 / OOM | 🔴 **高** | 运维告警 + 自动重启（K8s liveness probe） |

#### 7.8.3 自适应熔断规则

| 规则 | 触发条件 | 窗口 | 动作 |
|------|---------|:---:|------|
| 单节点熔断 | 连续超时 **5** 次 | 30s | 该版本自动失效 + 企微/邮件告警 |
| 全平台告警 | 超时率 **> 10%** | 5min | 运维群告警，排查是否有恶意脚本攻击 |
| 线程池饱和 | `RejectedExecutionException` **> 50/min** | 1min | 限流 + 扩容告警 |
| 沙箱违规 | 单次事件 | 即时 | P0 安全告警，人工介入分析 |

```java
// 基于 resilience4j 的熔断器
@Component
public class ScriptCircuitBreaker {

    // 单节点级别熔断器缓存
    private final Cache<String, CircuitBreaker> nodeBreakers = Caffeine.newBuilder()
        .expireAfterAccess(Duration.ofHours(1))
        .build();

    public CircuitBreaker forNode(String flowVersionId, String nodeId) {
        String key = flowVersionId + ":" + nodeId;
        return nodeBreakers.get(key, k -> CircuitBreaker.ofDefaults(k));
    }
}
```

### 7.9 供应链安全

#### 7.9.1 GraalJS 版本管理策略

```xml
<!-- connector-api/pom.xml -->
<dependency>
    <groupId>org.graalvm.polyglot</groupId>
    <artifactId>polyglot</artifactId>
    <version>24.1.1</version>  <!-- LTS 分支 -->
</dependency>
```

| 策略 | 措施 |
|------|------|
| **LTS 优先** | 使用 GraalVM 24.1.x LTS 分支，不追最新社区版。LTS 提供关键补丁回溯 |
| **CVE 监控** | 订阅 [Oracle Critical Patch Updates](https://www.oracle.com/security-alerts/) 邮件列表 |
| **自动扫描** | CI 流水线集成 OWASP Dependency-Check Maven Plugin |
| **升级 SLA** | CVE ≥ 7.0 → 24h 内评估升级；CVE ≥ 9.0 → 紧急热修复 + 回滚方案 |
| **体积控制** | GraalJS ~30MB，仅 `connector-api` 引入，不影响其他模块 |

#### 7.9.2 已知 CVE 跟踪

| CVE | 影响版本 | 风险 | 状态 |
|-----|:---:|------|:---:|
| CVE-2024-XXXX | < 24.0 | Polyglot 沙箱逃逸（跨语言调用） | 24.1.1 已修复 ✅ |
| *(按 Oracle 季度安全公告持续更新)* | | | |

### 7.10 风险评估与缓解

| 风险 | 等级 | 攻击面 | 防线覆盖 | 缓解措施 | 残留风险 |
|------|:---:|------|:---:|---------|---------|
| GraalJS 0-day 沙箱逃逸 | 🔴 高 | Polyglot API | 第1+2+5层 | LTS 版本 + CVE 监控 + `HostAccess.EXPLICIT` 降级攻击收益 + 即时告警 | 即使突破，JS 拿到的 Java 对象仅纯函数，零系统操作能力 |
| 恶意死循环 | 🟡 中 | 脚本内容 | 第3层 | `statementLimit(10000)` + `Mono.timeout(5s)` 双层 | 最多 10000 条语句，执行 < 50ms |
| 内存炸弹 | 🟡 中 | 超大数组/字符串 | 第3+4层 | 脚本长度限制 + `boundedElastic` 隔离 + `Context.close()` 释放 | 仅影响单线程，Event Loop 免受影响 |
| 线程池耗尽 | 🟡 中 | 大量并发脚本 | 第4层 | `boundedElastic.size=10, queueSize=100` | 超队列 → `503`，拒绝而非崩溃 |
| 数据注入 | 🟢 低 | ctx 对象引用 | — | `ctx` 数据全程以 Java 对象流转，不经字符串拼接，天然免疫注入 | 无——对象引用不存在注入语义 |
| Context 泄漏 | 🟢 低 | try-with-resources 遗漏 | 第4层 | 泄漏检测端点 + Code Review 强制检查 | 检测到 → 告警 → K8s 自动重启 |
| JS 精度丢失 | 🟢 低 | 大整数（> 2^53） | — | 大整数场景使用 `_util` 字符串方法 | 用户需了解 JS number 语义 |

### 7.11 安全检查清单

#### 发布时检查（CI/CD + 发布审批）

- [ ] `Context` 所有 8 个权限开关均为 `false`（IO / Native / Thread / Process / Polyglot / Environment / HostClassLoading / ExperimentalOptions）
- [ ] `HostAccess` 为 `EXPLICIT`，`allowHostClassLookup` 返回 `false`
- [ ] `allowListAccess` / `allowMapAccess` 仅用于只读语义
- [ ] 注入到 JS 的 Java 对象方法全部审计：无系统调用、无文件操作、无网络访问、无数据库访问、无反射
- [ ] `statementLimit ≤ 10000`
- [ ] `js.console` / `js.print` / `js.load` 均为 `false`
- [ ] `js.foreign-object-prototype` 为 `false`
- [ ] `js.unhandled-rejections` 为 `throw`
- [ ] `boundedElastic.size` 有上限（≤ 20），`queueSize` 有界（≤ 200）
- [ ] GraalJS 版本为 LTS（24.1.x），OWASP Dependency-Check 通过
- [ ] 所有 `Context` 创建处使用 `try-with-resources`
- [ ] `ctx` 组装仅引用上游节点数据，不引入外部数据源
- [ ] `main` 函数通过 `c.eval("js", "main")` 获取（非 `getMember`），确保 `this = undefined`
- [ ] 发布时 `ScriptStructureValidator` 校验通过——仅允许 `function main(ctx){}`，拦截函数外代码

#### 运行时检查（运维巡检）

- [ ] `script.execution.sandbox_error` 指标 > 0 → **立即安全告警**
- [ ] `script.context.active` 持续增长 → **Context 泄漏告警**（可能需 K8s 重启）
- [ ] `script.execution.rejected` 增长 → **线程池饱和告警**（需扩容或限流）
- [ ] 单节点连续超时 ≥ 5 次 → **版本自动失效** + 通知
- [ ] 全平台 5min 超时率 > 10% → **恶意脚本攻击告警**，人工介入排查
- [ ] `script.compilation.cache_hit` < 50% → 缓存可能被绕过，检查脚本动态生成逻辑

---

## 附录 A：完整 JS 脚本示例

### A.1 列表聚合

```javascript
function main(ctx) {
    const users = ctx.conn_1.output.body.data.users;
    const total  = users.length;
    const avgAge = users.reduce((sum, u) => sum + u.age, 0) / total;
    return { total, avgAge };
}
```

### A.2 数据脱敏 + 条件路由

```javascript
function main(ctx) {
    const phone  = ctx.trigger.input.body.phone;
    const amount = ctx.conn_1.output.body.totalAmount;
    const name   = ctx.trigger.input.body.sender;

    const grade = _customValidator.orderGrade(amount);
    const masked = phone.substring(0,3) + '****' + phone.substring(7);
    const hash   = _util.md5(phone + amount);

    _log.info(`${name}: grade=${grade}`);

    return { name, grade, masked, hash };
}
```

### A.3 分组统计（ES2022）

```javascript
function main(ctx) {
    const items = ctx.conn_1.output.body.data.items ?? [];

    const byCategory = items.reduce((groups, item) => {
        (groups[item.category] ??= []).push(item);
        return groups;
    }, {});

    const total = items.length;
    const last  = items.at(-1);

    return { total, lastItem: last?.name, byCategory };
}
```

### A.4 复杂计算

```javascript
function main(ctx) {
    const orders  = ctx.conn_1.output.body.orders;
    const history = ctx.conn_2.output.body.history;

    const totalRevenue = orders.reduce((s, o) => s + o.price * o.qty, 0);
    const avgHistory   = history.length > 0
        ? history.reduce((s, h) => s + h, 0) / history.length
        : 0;

    let tier;
    if (totalRevenue >= 100000 && avgHistory >= 90) tier = 'S';
    else if (totalRevenue >= 10000)                  tier = 'A';
    else                                             tier = 'B';

    return { totalRevenue, avgHistory, tier };
}
```

---

## 附录 B：修订记录

| 版本 | 日期 | 修订内容 |
|------|------|---------|
| v1.0 | 06-17 | 初始：Groovy 方案，三种形态 + 脚本库 |
| v2.0 | 06-17 | 翻转为 GraalJS；新增 WebFlux 架构、HostAccess 示例 |
| v3.0 | 06-17 | 精简：移除脚本库/数据库表/API，仅保留内联脚本节点 |
| v4.0 | 06-17 | **移除 inputMapping，改为脚本内直接写 `${$.node.xxx}` 引用，运行时字符串替换** |
| v5.0 | 06-18 | **安全章节整合：合并原 §3/§7/§8 为纵深防御章节，威胁模型 + 五层防线 + 检查清单** |
| v6.0 | 06-18 | **ctx 对象方案：移除 `${}` 模板替换，改用 `ctx` 对象引用上游数据，天然免疫注入** |
| v7.0 | 06-18 | **function main(ctx) 标准函数声明：ctx 为函数参数，return 显式输出，IDE/Linter 完整支持** |
| v8.1 | 06-18 | **安全审计：① getMember→c.eval("js","main") 消除 this 泄露；② ScriptStructureValidator 拦截函数外顶层代码；③ 安全章节移至 §7** |

---

**文档状态**: 📝 初稿（draft，v8.1）
