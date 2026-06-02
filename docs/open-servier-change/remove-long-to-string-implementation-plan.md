# 删除 Long→String 全局序列化策略 — 修改方案与测试用例（v2）

> 日期：2026-06-02
>
> 基于影响分析 v3 结论：项目所有响应 DTO 的 ID 字段已定义为 `String` 类型，不存在雪花 ID 精度丢失风险。实际影响仅 `PageResponse.total`（计数值）和 Sync 模块内部 DTO（旧系统自增 ID）。
>
> **v2 更新**：基于最新代码校准。open-server JacksonConfig 包含 `DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES` 配置，需在改后代码中保留。

---

## 一、修改方案

### 1.1 open-server JacksonConfig

**文件**：`open-server/src/main/java/com/xxx/it/works/wecode/v2/common/config/JacksonConfig.java`

#### 改前（当前代码）

```java
package com.xxx.it.works.wecode.v2.common.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.util.TimeZone;

/**
 * Jackson 配置
 *
 * <p>配置 JSON 序列化规则：</p>
 * <ul>
 *   <li>所有 ID 字段（Long/BigInteger）返回 string 类型，避免 JavaScript 精度丢失</li>
 *   <li>Java 8 时间类型序列化支持</li>
 *   <li>禁用日期序列化为时间戳</li>
 *   <li>忽略未知 JSON 属性，支持向后兼容（新旧字段共存）</li>
 * </ul>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
        ObjectMapper objectMapper = builder.createXmlMapper(false).build();

        // Java 8 时间模块
        objectMapper.registerModule(new JavaTimeModule());

        // 禁用日期序列化为时间戳
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // 忽略未知 JSON 属性（支持向后兼容，新旧字段共存时不会报错）
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        // 设置时区为北京时间
        objectMapper.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));

        // Long 类型序列化为 String，避免 JavaScript 精度丢失
        SimpleModule simpleModule = new SimpleModule();
        simpleModule.addSerializer(Long.class, ToStringSerializer.instance);
        simpleModule.addSerializer(Long.TYPE, ToStringSerializer.instance);
        objectMapper.registerModule(simpleModule);

        return objectMapper;
    }
}
```

#### 改后

```java
package com.xxx.it.works.wecode.v2.common.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.util.TimeZone;

/**
 * Jackson 配置
 *
 * <p>配置 JSON 序列化规则：</p>
 * <ul>
 *   <li>Java 8 时间类型序列化支持</li>
 *   <li>禁用日期序列化为时间戳</li>
 *   <li>忽略未知 JSON 属性，支持向后兼容（新旧字段共存）</li>
 * </ul>
 *
 * @author SDDU Build Agent
 * @version 1.1.0
 */
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
        ObjectMapper objectMapper = builder.createXmlMapper(false).build();

        // Java 8 时间模块
        objectMapper.registerModule(new JavaTimeModule());

        // 禁用日期序列化为时间戳
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // 忽略未知 JSON 属性（支持向后兼容，新旧字段共存时不会报错）
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        // 设置时区为北京时间
        objectMapper.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));

        return objectMapper;
    }
}
```

#### 变更说明

| 操作 | 行 | 内容 |
|------|:---:|------|
| 删除 import | 1 | `import com.fasterxml.jackson.databind.module.SimpleModule;` |
| 删除 import | 1 | `import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;` |
| 删除代码 | 4 | `SimpleModule simpleModule = new SimpleModule();` |
| 删除代码 | 4 | `simpleModule.addSerializer(Long.class, ToStringSerializer.instance);` |
| 删除代码 | 4 | `simpleModule.addSerializer(Long.TYPE, ToStringSerializer.instance);` |
| 删除代码 | 4 | `objectMapper.registerModule(simpleModule);` |
| 删除注释 | 2 | `// Long 类型序列化为 String，避免 JavaScript 精度丢失` |
| 更新注释 | — | Javadoc 移除 "所有 ID 字段返回 string 类型" 描述 |
| **保留** | — | `DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES` ✅ |
| **保留** | — | `JavaTimeModule` ✅ |
| **保留** | — | `WRITE_DATES_AS_TIMESTAMPS` 禁用 ✅ |
| **保留** | — | `setTimeZone("Asia/Shanghai")` ✅ |

---

### 1.2 api-server JacksonConfig

**文件**：`api-server/src/main/java/com/xxx/api/common/config/JacksonConfig.java`

#### 改前（当前代码）

```java
package com.xxx.api.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 * Jackson 配置
 * 
 * <p>配置 JSON 序列化规则，所有 ID 字段返回 string 类型</p>
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
        ObjectMapper objectMapper = builder.createXmlMapper(false).build();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        SimpleModule simpleModule = new SimpleModule();
        simpleModule.addSerializer(Long.class, ToStringSerializer.instance);
        simpleModule.addSerializer(Long.TYPE, ToStringSerializer.instance);
        objectMapper.registerModule(simpleModule);

        return objectMapper;
    }
}
```

#### 改后

```java
package com.xxx.api.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 * Jackson 配置
 *
 * <p>配置 JSON 序列化规则：</p>
 * <ul>
 *   <li>Java 8 时间类型序列化支持</li>
 *   <li>禁用日期序列化为时间戳</li>
 * </ul>
 *
 * @author SDDU Build Agent
 * @version 1.1.0
 */
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
        ObjectMapper objectMapper = builder.createXmlMapper(false).build();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return objectMapper;
    }
}
```

---

### 1.3 event-server JacksonConfig

**文件**：`event-server/src/main/java/com/xxx/event/common/config/JacksonConfig.java`

#### 改前（当前代码）

```java
package com.xxx.event.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 * Jackson 配置
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
        ObjectMapper objectMapper = builder.createXmlMapper(false).build();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        SimpleModule simpleModule = new SimpleModule();
        simpleModule.addSerializer(Long.class, ToStringSerializer.instance);
        simpleModule.addSerializer(Long.TYPE, ToStringSerializer.instance);
        objectMapper.registerModule(simpleModule);

        return objectMapper;
    }
}
```

#### 改后

```java
package com.xxx.event.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 * Jackson 配置
 *
 * <p>配置 JSON 序列化规则：</p>
 * <ul>
 *   <li>Java 8 时间类型序列化支持</li>
 *   <li>禁用日期序列化为时间戳</li>
 * </ul>
 *
 * @author SDDU Build Agent
 * @version 1.1.0
 */
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
        ObjectMapper objectMapper = builder.createXmlMapper(false).build();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return objectMapper;
    }
}
```

---

### 1.4 修改汇总

| # | 模块 | 文件 | 删除 import | 删除代码 | 保留配置 |
|:-:|------|------|:---:|:---:|------|
| 1 | open-server | `JacksonConfig.java` | 2 行 | 5 行（含注释） | JavaTimeModule + 时区 + **FAIL_ON_UNKNOWN_PROPERTIES** |
| 2 | api-server | `JacksonConfig.java` | 2 行 | 5 行（含注释） | JavaTimeModule |
| 3 | event-server | `JacksonConfig.java` | 2 行 | 5 行（含注释） | JavaTimeModule |

---

## 二、测试用例

### 2.1 TC-01：分页接口 `page.total` 返回 Number 类型

**目标**：验证删除 Long→String 后，`PageResponse.total` 从 `"128"` 变为 `128`

**测试接口**：`GET /service/open/v2/apis?curPage=1&pageSize=20`

| 项目 | 内容 |
|------|------|
| 前置条件 | 数据库中存在 ≥ 1 条 API 记录 |
| 请求方式 | GET |
| 请求头 | `Authorization: Bearer {token}` |

**预期响应**：

```json
{
  "code": "200",
  "messageZh": "操作成功",
  "messageEn": "Success",
  "data": [...],
  "page": {
    "curPage": 1,
    "pageSize": 20,
    "total": 5,
    "totalPages": 1
  }
}
```

| 检查点 | 期望值 | 验证方法 |
|--------|--------|---------|
| `page.total` 类型 | `number` | `typeof response.page.total === 'number'` |
| `page.total` 值 | 等于实际记录数 | 与数据库 `COUNT(*)` 比对 |
| `page.curPage` 类型 | `number` | 不变 |
| `page.totalPages` 类型 | `number` | 不变 |
| `data[].id` 类型 | `string` | DTO 定义为 String，不受影响 |

**覆盖接口**（9 个 open-server 分页 + 1 个 api-server 分页）：

| # | 模块 | 接口 | 说明 |
|:-:|------|------|------|
| 1 | open-server | `GET /service/open/v2/apis` | API 列表 |
| 2 | open-server | `GET /service/open/v2/events` | 事件列表 |
| 3 | open-server | `GET /service/open/v2/callbacks` | 回调列表 |
| 4 | open-server | `GET /service/open/v2/categories` | 分类列表 |
| 5 | open-server | `GET /service/open/v2/approval-flows` | 审批流程模板列表 |
| 6 | open-server | `GET /service/open/v2/approvals/pending` | 待审批列表 |
| 7 | open-server | `GET /service/open/v2/apps/{appId}/apis` | 应用 API 权限订阅列表 |
| 8 | open-server | `GET /service/open/v2/apps/{appId}/events` | 应用事件权限订阅列表 |
| 9 | open-server | `GET /service/open/v2/apps/{appId}/callbacks` | 应用回调权限订阅列表 |
| 10 | api-server | `GET /api/v1/user-authorizations` | 用户授权列表 |

---

### 2.2 TC-02：业务 ID 字段保持 String 类型（无精度丢失）

**目标**：验证所有响应 DTO 中的 ID 字段仍为 `string`，雪花 ID 不丢失精度

**测试接口**：`GET /service/open/v2/apis/{id}`

| 项目 | 内容 |
|------|------|
| 前置条件 | 数据库中存在指定 ID 的 API 记录（雪花 ID） |
| 请求方式 | GET |

**预期响应**：

```json
{
  "code": "200",
  "data": {
    "id": "1234567890123456789",
    "nameCn": "测试API",
    "categoryId": "9876543210",
    "status": 1
  }
}
```

| 检查点 | 期望值 | 说明 |
|--------|--------|------|
| `data.id` 类型 | `string` | DTO 定义为 `String id` |
| `data.id` 值 | 完整匹配 DB 存储值 | 雪花 ID 尾部数字不变 |
| `data.categoryId` 类型 | `string` | DTO 定义为 `String categoryId` |

**覆盖接口**（11 个返回 Detail/List Response 的接口）：

| # | 接口 | 验证的 ID 字段 |
|:-:|------|---------------|
| 1 | `GET /service/open/v2/apis/{id}` | `id`, `categoryId` |
| 2 | `POST /service/open/v2/apis` | 返回的 `id` |
| 3 | `GET /service/open/v2/events/{id}` | `id`, `categoryId` |
| 4 | `GET /service/open/v2/callbacks/{id}` | `id`, `categoryId` |
| 5 | `GET /service/open/v2/categories/{id}` | `id`, `parentId` |
| 6 | `GET /service/open/v2/categories/{id}/owners` | `id`, `categoryId` |
| 7 | `GET /service/open/v2/approval-flows/{id}` | `id` |
| 8 | `GET /service/open/v2/approvals/{id}` | `id`, `businessId` |
| 9 | `GET /service/open/v2/apps/{appId}/apis` | `data[].id`, `data[].permissionId` |
| 10 | `GET /service/open/v2/apps/{appId}/events` | `data[].id`, `data[].permissionId` |
| 11 | `GET /service/open/v2/apps/{appId}/callbacks` | `data[].id`, `data[].permissionId` |

---

### 2.3 TC-03：写操作响应 ID 保持 String 类型

**目标**：验证 POST 创建资源返回的 ID 为 `string`

**测试接口**：`POST /service/open/v2/categories`

| 项目 | 内容 |
|------|------|
| 请求方式 | POST |
| 请求体 | `{ "nameCn": "测试分类", "parentId": "0" }` |

**预期响应**：

```json
{
  "code": "200",
  "data": {
    "id": "1912345678901234567",
    "nameCn": "测试分类",
    "parentId": "0"
  }
}
```

| 检查点 | 期望值 | 说明 |
|--------|--------|------|
| `data.id` 类型 | `string` | DTO 定义为 String |
| 雪花 ID 精度 | 与 DB 存储一致 | 尾部数字不丢失 |

**覆盖接口**（9 个写操作接口）：

| # | 接口 | 操作 |
|:-:|------|------|
| 1 | `POST /service/open/v2/categories` | 创建分类 |
| 2 | `POST /service/open/v2/categories/{id}/owners` | 添加负责人 |
| 3 | `POST /service/open/v2/apis` | 创建 API |
| 4 | `POST /service/open/v2/events` | 创建事件 |
| 5 | `POST /service/open/v2/callbacks` | 创建回调 |
| 6 | `POST /service/open/v2/approval-flows` | 创建审批流程模板 |
| 7 | `POST /service/open/v2/apps/{appId}/apis/subscribe` | 订阅 API 权限 |
| 8 | `POST /service/open/v2/apps/{appId}/events/subscribe` | 订阅事件权限 |
| 9 | `POST /service/open/v2/apps/{appId}/callbacks/subscribe` | 订阅回调权限 |

---

### 2.4 TC-04：审批操作响应 ID 保持 String 类型

**目标**：验证审批通过/驳回/撤销/批量操作/催办返回的 ID 为 `string`

**测试接口**：`POST /service/open/v2/approvals/{id}/approve`

| 项目 | 内容 |
|------|------|
| 前置条件 | 存在待审批记录，当前用户为审批人 |
| 请求方式 | POST |

**预期响应**：

```json
{
  "code": "200",
  "data": {
    "id": "1234567890",
    "businessId": "9876543210",
    "status": 1
  }
}
```

| 检查点 | 期望值 |
|--------|--------|
| `data.id` 类型 | `string` |
| `data.businessId` 类型 | `string` |

**覆盖接口**（6 个审批操作接口）：

| # | 接口 |
|:-:|------|
| 1 | `POST /service/open/v2/approvals/{id}/approve` |
| 2 | `POST /service/open/v2/approvals/{id}/reject` |
| 3 | `POST /service/open/v2/approvals/{id}/cancel` |
| 4 | `POST /service/open/v2/approvals/batch-approve` |
| 5 | `POST /service/open/v2/approvals/batch-reject` |
| 6 | `POST /service/open/v2/approvals/{id}/urge` |

---

### 2.5 TC-05：Sync 模块接口 Long 字段返回 Number 类型

**目标**：验证 Sync 模块 DTO 中的 `Long id` 字段从字符串变为数字

**测试接口**：`POST /service/open/v2/sync/subscription/migrate`

| 项目 | 内容 |
|------|------|
| 前置条件 | 旧系统中存在待迁移的订阅数据 |
| 请求方式 | POST |

**预期响应**：

```json
{
  "code": "200",
  "data": {
    "success": 2,
    "failed": 0,
    "skipped": 0,
    "details": [
      {
        "id": 101,
        "status": "success",
        "approvalStatus": "success",
        "approvalLogStatus": "success"
      }
    ]
  }
}
```

| 检查点 | 期望值 | 说明 |
|--------|--------|------|
| `details[].id` 类型 | `number` | DTO 定义为 `Long id`，删除转换后为数字 |
| `details[].id` 值 | 等于旧系统 ID | 旧系统自增 ID，值域小，无精度问题 |

**覆盖接口**（4 个 Sync 接口）：

| # | 接口 | 验证的 Long 字段 |
|:-:|------|-----------------|
| 1 | `POST /service/open/v2/sync/subscription/migrate` | `details[].id` |
| 2 | `POST /service/open/v2/sync/subscription/rollback` | `details[].id` |
| 3 | `POST /service/open/v2/sync/subscription/emergency/update-old` | `details[].id` |
| 4 | `POST /service/open/v2/sync/subscription/emergency/update-new` | `details[].id` |

---

### 2.6 TC-06：api-server 业务 ID 保持 String 类型

**目标**：验证 api-server 接口的 ID 字段不受影响

**测试接口**：`GET /api/v1/user-authorizations`

**预期响应**：

```json
{
  "code": "200",
  "data": [
    {
      "id": "1234567890",
      "appId": "9876543210",
      "scope": "read",
      "expiresAt": "2026-12-31T23:59:59.000Z"
    }
  ],
  "page": {
    "total": 3,
    "curPage": 1,
    "pageSize": 20,
    "totalPages": 1
  }
}
```

| 检查点 | 期望值 |
|--------|--------|
| `data[].id` 类型 | `string` |
| `data[].appId` 类型 | `string` |
| `page.total` 类型 | `number` |

**覆盖接口**（5 个 api-server 业务接口）：

| # | 接口 |
|:-:|------|
| 1 | `GET /api/v1/user-authorizations` |
| 2 | `POST /api/v1/user-authorizations` |
| 3 | `GET /gateway/permissions/check` |
| 4 | `POST /gateway/assistant/callbacks/config` |
| 5 | `POST /api/v1/approvals/callback` |

---

### 2.7 TC-07：event-server 接口无 Long 字段泄露

**目标**：验证 event-server 接口响应正常，无 Long 字段类型变化

**测试接口**：`GET /api/v1/health`

**预期响应**：

```json
{
  "code": "200",
  "data": {
    "status": "UP",
    "service": "event-server"
  }
}
```

| 检查点 | 期望值 | 说明 |
|--------|--------|------|
| 响应正常返回 | `code = "200"` | event-server 无分页接口，无 Long 字段 |
| 无类型变化 | 所有字段类型不变 | 所有 DTO 中无 Long 字段 |

**覆盖接口**（6 个 event-server 接口）：

| # | 接口 | 说明 |
|:-:|------|------|
| 1 | `GET /api/v1/health` | 健康检查 |
| 2 | `GET /sse/status` | SSE 状态 |
| 3 | `GET /ws/status` | WebSocket 状态 |
| 4 | `GET /ws/count` | WebSocket 连接数 |
| 5 | `POST /gateway/events/publish` | 事件发布 |
| 6 | `POST /gateway/callbacks/invoke` | 回调调用 |

---

### 2.8 TC-08：connector-api 行为无变化

**目标**：验证 connector-api 接口不受影响（原本就无 JacksonConfig）

**测试接口**：`POST /api/v1/trigger/{flowId}/invoke`

**预期响应**：

```json
{
  "success": true,
  "totalDurationMs": 523,
  "steps": [
    {
      "nodeName": "entry",
      "durationMs": 12
    }
  ]
}
```

| 检查点 | 期望值 | 说明 |
|--------|--------|------|
| `totalDurationMs` 类型 | `number` | connector-api 原本无转换，不变 |
| `steps[].durationMs` 类型 | `number` | 同上 |

**覆盖接口**（2 个 connector-api 接口）：

| # | 接口 |
|:-:|------|
| 1 | `POST /api/v1/internal/test-run/{flowId}` |
| 2 | `POST /api/v1/trigger/{flowId}/invoke` |

---

### 2.9 TC-09：FAIL_ON_UNKNOWN_PROPERTIES 保持生效

**目标**：验证 open-server 的反序列化兼容性配置未被误删

**测试方法**：向 open-server POST 接口发送包含额外未知字段的请求体

**测试接口**：`POST /service/open/v2/categories`

**请求体**（包含未知字段 `unknownField`）：

```json
{
  "nameCn": "测试分类",
  "parentId": "0",
  "unknownField": "should be ignored"
}
```

**预期响应**：

```json
{
  "code": "200",
  "data": {
    "id": "...",
    "nameCn": "测试分类",
    "parentId": "0"
  }
}
```

| 检查点 | 期望值 | 说明 |
|--------|--------|------|
| 请求正常处理 | `code = "200"` | `unknownField` 被忽略，不报 400 错误 |
| 不抛异常 | 无 `UnrecognizedPropertyException` | `FAIL_ON_UNKNOWN_PROPERTIES` 仍为禁用状态 |

---

## 三、测试用例汇总

| 编号 | 模块 | 测试场景 | 覆盖接口数 | 核心断言 |
|:----:|------|---------|:---:|---------|
| TC-01 | open-server + api-server | 分页 `page.total` → Number | 10 | `typeof total === 'number'` |
| TC-02 | open-server | 业务 ID 保持 String（查询） | 11 | `typeof id === 'string'`，雪花 ID 完整 |
| TC-03 | open-server | 写操作返回 ID 保持 String | 9 | `typeof id === 'string'` |
| TC-04 | open-server | 审批操作 ID 保持 String | 6 | `typeof id === 'string'` |
| TC-05 | open-server | Sync 模块 Long → Number | 4 | `typeof id === 'number'` |
| TC-06 | api-server | 业务 ID 保持 String | 5 | `typeof id === 'string'` |
| TC-07 | event-server | 接口无 Long 泄露 | 6 | 响应正常，无 Long 字段 |
| TC-08 | connector-api | 行为无变化 | 2 | `durationMs` 仍为 number |
| TC-09 | open-server | `FAIL_ON_UNKNOWN_PROPERTIES` 保持 | 1 | 额外字段不报错 |
| | | **合计** | **54** | |

---

## 四、回归测试检查清单

### 4.1 必须通过的回归场景

| # | 场景 | 预期结果 | 优先级 |
|:-:|------|---------|:---:|
| R-01 | 前端分页组件正常渲染 | `page.total` 为 number，组件直接使用无需 `parseInt` | P0 |
| R-02 | 列表行点击跳转详情 | `id` 为 string，URL 拼接正常：`/detail/${id}` | P0 |
| R-03 | 订阅/撤回/删除操作 | 响应中 `id` 为 string，前端状态管理正常 | P0 |
| R-04 | 审批通过/驳回/撤销 | 响应中 `id` 为 string，列表刷新正常 | P0 |
| R-05 | 向后兼容（旧客户端发送额外字段） | 请求正常处理，未知字段被忽略 | P0 |
| R-06 | 分类树展开/折叠 | `id` / `parentId` 为 string，树结构正确 | P1 |
| R-07 | 批量审批操作 | 响应中所有 `id` 为 string | P1 |
| R-08 | API 权限列表筛选 | `page.total` 为 number，分页正确 | P1 |

### 4.2 前端需同步修改项

| # | 修改点 | 改前 | 改后 | 影响范围 |
|:-:|--------|------|------|---------|
| 1 | 分页 `total` 类型定义 | `total: string` | `total: number` | TypeScript 接口定义 |
| 2 | 分页组件 `total` props | `parseInt(page.total)` 或 `Number(page.total)` | 直接传 `page.total` | 分页组件调用处 |
| 3 | `total` 条件判断 | `total > "0"` (字符串比较) | `total > 0` (数字比较) | 业务逻辑层 |

> **无需修改**：所有 `id`、`appId`、`permissionId` 等业务 ID 字段的类型定义，因为 DTO 层已定义为 `String`，删除 Jackson 全局转换不影响这些字段。
