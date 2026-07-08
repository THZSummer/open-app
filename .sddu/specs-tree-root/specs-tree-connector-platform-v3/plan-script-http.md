# 脚本 HTTP 调用能力设计

**Feature ID**: CONN-PLAT-003-HTTP
**关联文档**: plan-script.md（脚本执行引擎）、plan-json-schema.md（值表达式体系）、plan-runtime.md（运行时引擎）
**版本**: v1.0-draft
**创建日期**: 2026-07-08

---

## 1. 背景

脚本节点（Script Node）提供 `function main(ctx)` 标准函数声明，用户编写 JS 逻辑访问上游节点数据。但在实际业务场景中，脚本节点常需要调用外部 HTTP 接口来获取数据或触发操作，例如：

- 根据 `type` 参数条件路由到不同的开放平台 OpenAPI，查询连接器或连接流列表
- 调用外部鉴权、通知、风控等第三方服务
- 聚合多个外部接口的数据后返回

因此需要在 `ctx` 对象上暴露安全的 HTTP 客户端能力，使脚本能够发起 HTTP 请求并处理响应。

---

## 2. 设计方案

### 2.1 能力范围

按 HTTP 协议维度，`ctx.http` 的能力覆盖如下：

| 维度 | 能力项 | 说明 | 实现 |
|------|--------|------|:----:|
| **HTTP 方法** | 统一入口 | `ctx.http.request(method, url, opts)` 覆盖所有 HTTP 场景 | ✅ |
| **Query 参数** | URL 拼接 | 用户自行将 query 参数拼接到 URL 字符串中 | ✅ |
| | 自动编码 | 需用户手动 `encodeURIComponent()` 处理特殊字符 | ✅ |
| | Query 对象传入 | 不支持 `{params: {...}}` 方式自动拼接 | ❌ |
| **Body 参数** | JSON 对象 | JS 对象自动 Jackson 序列化为 JSON 字符串 | ✅ |
| | JSON 字符串 | 传入字符串直接作为 JSON body 透传 | ✅ |
| | FormData | 不支持 multipart/form-data | ❌ |
| | 二进制 | 不支持二进制 body | ❌ |
| | x-www-form-urlencoded | 不支持 form 格式编码 | ❌ |
| **Header 参数** | 自定义 Header | 平铺 `Map<String,String>` 作为方法最后一个参数传入 | ✅ |
| | Content-Type | 当前强制 `application/json`，不建议固定，需支持用户自定义 | ❌ |
| **响应头** | 获取响应头 | `resp.headers` 返回平铺 Map | ✅ |
| | 多值处理 | 同一头多个值时只取首值 | ✅ |
| **响应体** | JSON 自动解析 | `resp.body` 自动解析为 JS 对象 | ✅ |
| | 原始字符串 | 非 JSON 响应体保留原始字符串 | ✅ |
| | 状态码 | `resp.status` 返回 HTTP 状态码，失败为 0 | ✅ |
| | 流式读取 | 不支持大文件流式下载 | ❌ |

### 2.2 设计原则

`ctx.http` 定位为 **JS 调用 HTTP 的透明代理**，不做任何限制，便于未来扩展：

- **唯一入口**：`ctx.http.request(method, url, opts)`，不暴露其他方法
- **不限制 method**：任意 HTTP 方法字符串，不做白名单校验
- **不限制 header**：opts.headers 中传入什么就透传什么，不强制 Content-Type
- **不限制 body**：JS 对象 → JSON；字符串 → 直接透传；未来可扩展二进制
- **扩展点唯一**：所有新参数（timeoutMs、ssl 等）统一放入 `opts`，Java 侧只扩展一处

### 2.3 方法签名

```javascript
var resp = ctx.http.request(method, url, opts)
```

**入参：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|:---:|------|
| `method` | `string` | ✅ | HTTP 方法，不作限制，任意字符串透传 |
| `url` | `string` | ✅ | 完整 URL，含 query 参数 |
| `opts` | `object` | — | 可选配置，可缺省 |
| `opts.headers` | `object` | — | 请求头，`{name: value}` 平铺，透传到 HTTP 请求 |
| `opts.body` | `object \| string` | — | 请求体：对象 → JSON 序列化，字符串 → 直接透传 |

**返回值：**

| 字段 | 类型 | 说明 |
|------|------|------|
| `resp.status` | `int` | HTTP 状态码，请求失败时为 `0` |
| `resp.body` | `object \| string` | JSON 响应 → 自动解析为 JS 对象；非 JSON → 原始字符串 |
| `resp.headers` | `object` | 响应头，平铺 `{name: value}`，多值取首值 |

**使用示例：**

```javascript
// GET — 无 headers/body
var resp = ctx.http.request('GET', 'http://example.com/api/connectors?page=1');

// GET — 带 headers
var resp = ctx.http.request('GET', 'http://example.com/api/connectors?page=1', {
    headers: {'X-App-Id': 'xxx', 'Cookie': 'user_id=admin'}
});

// POST — JS 对象 body 自动 JSON 序列化
var resp = ctx.http.request('POST', 'http://example.com/api/create', {
    body: {name: 'test', age: 18},
    headers: {'Authorization': 'Bearer xxx'}
});

// POST — 字符串 body 直接透传
var resp = ctx.http.request('POST', 'http://example.com/api/create', {
    body: '{"name":"test"}',
    headers: {'Content-Type': 'application/json'}
});

// DELETE — 无 body
var resp = ctx.http.request('DELETE', 'http://example.com/api/item/1', {
    headers: {'X-App-Id': 'xxx'}
});

// 任意 method — 无限制透传
var resp = ctx.http.request('PATCH', 'http://example.com/api/item/1', {
    body: {status: 'active'}
});
```

### 2.4 响应对象结构

`ctx.http.request()` 返回的响应对象包含三个字段：

```javascript
var resp = ctx.http.request('GET', url);

resp.status   → HTTP 状态码（int），请求失败时为 0
resp.body     → 响应体，JSON 自动解析为 JS 对象，否则为原始字符串
resp.headers  → 响应头，平铺为 Map<String, String>
```

错误场景的响应结构：

```json
{
    "status": 0,
    "body": {"message": "HTTP call failed: ConnectException", "detail": "Connection refused"},
    "headers": {}
}
```

### 2.6 脚本中使用示例

```javascript
function main(ctx) {
    var input = ctx.trigger.input.body;
    var type = input.type || 'connectors';
    var q = input.query || {};
    var hdrs = input.header || {};

    var path = (type === 'flows') ? '/flows' : '/connectors';
    var url = 'http://localhost:18080/open-server/service/open/v2'
        + path + '?curPage=' + (q.curPage || 1)
        + '&keyword=' + encodeURIComponent(q.keyword || '')
        + '&pageSize=' + (q.pageSize || 10);

    var resp = ctx.http.request('GET', url, {headers: hdrs});
    var items = resp.body.data || [];
    var keys = (type === 'flows')
        ? ['flowId', 'nameCn', 'lifecycleStatus']
        : ['connectorId', 'nameCn', 'status'];

    return { result: formatItems(items, keys), domain: String(items.length), group: items[0]?.nameCn ?? '-', path: type === 'flows' ? 'flows' : 'connectors' };
}
```

---

## 3. 当前能力清单

### 3.1 已支持

当前 `ScriptHttpClient` 暴露了 `get(url)`、`get(url, headers)`、`post(url, body)`、`post(url, body, headers)`、`put(url, body)`、`put(url, body, headers)` 6 个方法，需迁移到统一的 `request(method, url, opts)`。

| 能力 | 说明 |
|------|------|
| GET/POST/PUT | 通过逐方法暴露（待迁移） |
| 响应 JSON 自动解析 | `resp.body` 直接为 JS 对象 |
| 响应状态码 | `resp.status` 获取 HTTP 状态码 |
| 响应头获取 | `resp.headers` 平铺 Map |
| 自动跟随重定向 | `HttpClient.Redirect.NORMAL` |
| HTTPS | Java 原生 HttpClient 支持 |

### 3.2 缺失

| 缺失能力 | 影响 | 优先级 |
|---------|------|:-----:|
| 统一 `request()` 入口 | 当前逐方法枚举，不易扩展 | 高 |
| 自定义超时 | 慢接口固定 3s 超时，用户不可配 | 中 |
| 自定义 SSL / 证书 | 无法对接自签名证书的内部 API | 低 |
| 连接池复用 | 高频调用新建连接性能差 | 低 |
| Body 支持二进制 / FormData | 仅限于 JSON Body + 字符串 | 低 |

---

## 4. 技术实现

### 4.1 实现位置

`connector-api` 模块的 `ScriptHttpClient.java`，通过 `@HostAccess.Export` 暴露给 GraalJS 沙箱。

### 4.2 线程隔离

HTTP 调用在独立线程池（`script-http-worker`，4 个守护线程）中执行，绕过 GraalJS `allowIO(false)` 的 IO 限制：

```
GraalJS 沙箱线程（IO 全禁）
  ↓ 提交 Callable 到 script-http-worker 线程池
独立线程（允许 IO）
  ↓ 执行 java.net.http.HttpClient 请求
  ↓ 返回结果到沙箱线程
```

### 4.3 超时机制

| 层级 | 超时值 | 控制方式 |
|------|:------:|---------|
| 连接超时 | 10s | `HttpClient.connectTimeout()` |
| 请求超时 | 10s | `HttpRequest.timeout()` |
| Future.get() | **3s** | `Future.get(3, TimeUnit.SECONDS)` — 硬编码 |
| 脚本节点 | 1~30s | 编排配置 `timeoutMs` |

> 注意：`Future.get()` 的 3s 超时是当前最大瓶颈，即使节点配置更长超时，HTTP 调用也会在 3s 后被截断。

### 4.4 安全约束

- 在 `script-http-worker` 线程池中执行，不走 GraalJS IO 权限（`allowIO(false)` 对此线程无效）
- 仅暴露 `@HostAccess.Export` 标记的 `request()` 方法
- 遵循同 5 层纵深防御体系（见 plan-script.md §7）

---

## 5. 版本规划

| 阶段 | 范围 | 优先级 |
|------|------|:-----:|
| Phase 1 | 迁移到统一 `request(method, url, opts)`，移除逐方法枚举 | P0 |
| Phase 2 | `opts` 扩展 timeoutMs、ssl 等参数 | P1 |
| Phase 3 | 连接池优化 + 二进制 body 支持 | P2 |

---

## 6. 边界与约束

| 约束 | 值 |
|------|:---:|
| 支持的方法 | 不作限制，任意字符串透传 |
| Content-Type | 不作限制，用户通过 `opts.headers` 自由设置 |
| Headers 参数 | `opts.headers` 平铺 `{name: value}` |
| 连接超时 | 10s（硬编码） |
| 请求超时 | 3s（`Future.get()`，硬编码） |
| 最大响应体 | 未显式限制（受 JVM Heap 间接约束） |
| 重定向 | 自动跟随（`Redirect.NORMAL`） |
| HTTPS | 支持，使用 JVM 默认信任库 |
| 请求 Body | JS 对象 → JSON 序列化；字符串 → 直接透传 |

---

## 附录 A：实现参考 — ScriptHttpClient.java 核心逻辑

```java
@HostAccess.Export
public Map<String, Object> request(String method, String url, Map<String, Object> opts) {
    @SuppressWarnings("unchecked")
    Map<String, String> headers = opts != null && opts.containsKey("headers")
        ? (Map<String, String>) opts.get("headers") : Map.of();
    Object body = opts != null ? opts.get("body") : null;

    Future<Map<String, Object>> future = executor.submit(() -> {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofSeconds(DEFAULT_TIMEOUT_SECONDS));

        // headers — 透明透传，不做任何限制
        headers.forEach((k, v) -> builder.header(k, v));

        // body — 对象 → JSON，字符串 → 透传，空 → noBody
        if (body != null) {
            String bodyStr = (body instanceof String)
                ? (String) body
                : objectMapper.writeValueAsString(body);
            builder.method(method, HttpRequest.BodyPublishers.ofString(bodyStr));
        } else {
            builder.method(method, HttpRequest.BodyPublishers.noBody());
        }

        HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(DEFAULT_TIMEOUT_SECONDS))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

        HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());

        Map<String, Object> result = new HashMap<>();
        result.put("status", response.statusCode());
        result.put("body", tryParseJson(response.body()));
        result.put("headers", flattenHeaders(response.headers()));
        return result;
    });

    return future.get(3, TimeUnit.SECONDS);
}
```

---

**文档状态**: 初稿（draft）
