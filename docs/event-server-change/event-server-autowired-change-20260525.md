# Event-Server 依赖注入方式变更说明

## 变更概述

将 event-server 服务中所有使用 Lombok `@RequiredArgsConstructor` 构造器注入的方式，统一替换为 Spring `@Autowired` 字段注入方式。

**变更日期**：2026-05-25

**变更范围**：event-server 模块，共 11 个 Java 文件

---

## 变更原因

将依赖注入方式从 Lombok 构造器注入（`@RequiredArgsConstructor`）改为 Spring 原生字段注入（`@Autowired`），以降低对 Lombok 构造器功能的依赖，使用 Spring 原生注解完成依赖注入。

---

## 变更详情

### 统一修改模式

每个文件的修改包含以下三部分：

1. **import 替换**
   - 删除：`import lombok.RequiredArgsConstructor;`
   - 新增：`import org.springframework.beans.factory.annotation.Autowired;`

2. **类注解移除**
   - 删除类上的 `@RequiredArgsConstructor` 注解

3. **字段修改**
   - 移除注入字段的 `final` 修饰符
   - 在每个注入字段上方添加 `@Autowired` 注解

**修改前示例：**
```java
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FooService {
    private final BarService barService;
    private final BazService bazService;
}
```

**修改后示例：**
```java
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class FooService {
    @Autowired
    private BarService barService;
    @Autowired
    private BazService bazService;
}
```

---

### 修改文件清单

| 序号 | 文件路径 | 注入字段数 | 修改的字段 |
|:---:|---------|:--------:|----------|
| 1 | `gateway/controller/CallbackGatewayController.java` | 1 | `callbackGatewayService` |
| 2 | `gateway/controller/EventGatewayController.java` | 1 | `eventGatewayService` |
| 3 | `gateway/service/CallbackGatewayService.java` | 5 | `apiServerClient`, `webHookChannel`, `sseChannel`, `webSocketChannel`, `redisTemplate` |
| 4 | `gateway/service/EventGatewayService.java` | 4 | `apiServerClient`, `webHookChannel`, `messageQueueChannel`, `redisTemplate` |
| 5 | `client/ApiServerClient.java` | 2 | `restTemplate`, `objectMapper` |
| 6 | `common/config/WebSocketConfig.java` | 1 | `webSocketHandler` |
| 7 | `common/controller/SseController.java` | 1 | `sseChannel` |
| 8 | `common/controller/WebSocketController.java` | 1 | `webSocketChannel` |
| 9 | `common/channel/WebHookChannel.java` | 2 | `restTemplate`, `authHandler` |
| 10 | `common/channel/WebSocketHandler.java` | 1 | `webSocketChannel` |
| 11 | `common/auth/AuthHandlerImpl.java` | 1 | `credentialProvider` |

> 所有文件路径基于 `event-server/src/main/java/com/xxx/event/`

---

### 各文件修改明细

#### 1. CallbackGatewayController.java

**路径**：`com.xxx.event.gateway.controller.CallbackGatewayController`

- 删除 `import lombok.RequiredArgsConstructor;`
- 新增 `import org.springframework.beans.factory.annotation.Autowired;`
- 删除类注解 `@RequiredArgsConstructor`
- `private final CallbackGatewayService callbackGatewayService` → `@Autowired private CallbackGatewayService callbackGatewayService`

#### 2. EventGatewayController.java

**路径**：`com.xxx.event.gateway.controller.EventGatewayController`

- 删除 `import lombok.RequiredArgsConstructor;`
- 新增 `import org.springframework.beans.factory.annotation.Autowired;`
- 删除类注解 `@RequiredArgsConstructor`
- `private final EventGatewayService eventGatewayService` → `@Autowired private EventGatewayService eventGatewayService`

#### 3. CallbackGatewayService.java

**路径**：`com.xxx.event.gateway.service.CallbackGatewayService`

- 删除 `import lombok.RequiredArgsConstructor;`
- 新增 `import org.springframework.beans.factory.annotation.Autowired;`
- 删除类注解 `@RequiredArgsConstructor`
- 5 个字段 `private final` → `@Autowired private`：
  - `ApiServerClient apiServerClient`
  - `WebHookChannel webHookChannel`
  - `SseChannel sseChannel`
  - `WebSocketChannel webSocketChannel`
  - `RedisTemplate<String, Object> redisTemplate`

#### 4. EventGatewayService.java

**路径**：`com.xxx.event.gateway.service.EventGatewayService`

- 删除 `import lombok.RequiredArgsConstructor;`
- 新增 `import org.springframework.beans.factory.annotation.Autowired;`
- 删除类注解 `@RequiredArgsConstructor`
- 4 个字段 `private final` → `@Autowired private`：
  - `ApiServerClient apiServerClient`
  - `WebHookChannel webHookChannel`
  - `MessageQueueChannel messageQueueChannel`
  - `RedisTemplate<String, Object> redisTemplate`

#### 5. ApiServerClient.java

**路径**：`com.xxx.event.client.ApiServerClient`

- 删除 `import lombok.RequiredArgsConstructor;`
- 新增 `import org.springframework.beans.factory.annotation.Autowired;`
- 删除类注解 `@RequiredArgsConstructor`
- 2 个字段 `private final` → `@Autowired private`：
  - `RestTemplate restTemplate`
  - `ObjectMapper objectMapper`
- 注：`@Value` 注解的字段（`apiServerUrl`、`authEnabled`）不受影响

#### 6. WebSocketConfig.java

**路径**：`com.xxx.event.common.config.WebSocketConfig`

- 删除 `import lombok.RequiredArgsConstructor;`
- 新增 `import org.springframework.beans.factory.annotation.Autowired;`
- 删除类注解 `@RequiredArgsConstructor`
- `private final WebSocketHandler webSocketHandler` → `@Autowired private WebSocketHandler webSocketHandler`

#### 7. SseController.java

**路径**：`com.xxx.event.common.controller.SseController`

- 删除 `import lombok.RequiredArgsConstructor;`
- 新增 `import org.springframework.beans.factory.annotation.Autowired;`
- 删除类注解 `@RequiredArgsConstructor`
- `private final SseChannel sseChannel` → `@Autowired private SseChannel sseChannel`

#### 8. WebSocketController.java

**路径**：`com.xxx.event.common.controller.WebSocketController`

- 删除 `import lombok.RequiredArgsConstructor;`
- 新增 `import org.springframework.beans.factory.annotation.Autowired;`
- 删除类注解 `@RequiredArgsConstructor`
- `private final WebSocketChannel webSocketChannel` → `@Autowired private WebSocketChannel webSocketChannel`

#### 9. WebHookChannel.java

**路径**：`com.xxx.event.common.channel.WebHookChannel`

- 删除 `import lombok.RequiredArgsConstructor;`
- 新增 `import org.springframework.beans.factory.annotation.Autowired;`
- 删除类注解 `@RequiredArgsConstructor`
- 2 个字段 `private final` → `@Autowired private`：
  - `RestTemplate restTemplate`
  - `AuthHandler authHandler`

#### 10. WebSocketHandler.java

**路径**：`com.xxx.event.common.channel.WebSocketHandler`

- 删除 `import lombok.RequiredArgsConstructor;`
- 新增 `import org.springframework.beans.factory.annotation.Autowired;`
- 删除类注解 `@RequiredArgsConstructor`
- `private final WebSocketChannel webSocketChannel` → `@Autowired private WebSocketChannel webSocketChannel`

#### 11. AuthHandlerImpl.java

**路径**：`com.xxx.event.common.auth.AuthHandlerImpl`

- 删除 `import lombok.RequiredArgsConstructor;`
- 新增 `import org.springframework.beans.factory.annotation.Autowired;`
- 删除类注解 `@RequiredArgsConstructor`
- `private final CredentialProvider credentialProvider` → `@Autowired private CredentialProvider credentialProvider`

---

## 依赖变更

### pom.xml 无变更

| 依赖项 | 状态 | 说明 |
|-------|------|------|
| `lombok` | **保留** | 项目中仍大量使用 `@Slf4j`(14处)、`@Data`(7处)、`@Builder`(6处)、`@NoArgsConstructor`(6处)、`@AllArgsConstructor`(7处)、`@Getter`(2处) |
| `spring-boot-starter-web` | 无变化 | `@Autowired` 已包含在此依赖中，无需额外添加 |
| `spring-boot-starter-actuator` | 无变化 | 通过 `application.yml` 中 `management` 配置使用 |
| 其他依赖 | 无变化 | 所有现有依赖均有使用，无需删除 |

### 未修改的文件说明

- **`CredentialProviderImpl.java`**：已使用手动构造器注入，未使用 `@RequiredArgsConstructor`，无需修改
- **DTO/Model 类**（如 `CallbackInvokeRequest` 等）：仅使用 `@Data`、`@Builder` 等 Lombok 数据注解，不涉及依赖注入，无需修改
- **测试类**：使用 `@Autowired`（MockMvc 测试），不涉及 `@RequiredArgsConstructor`，无需修改

---

## 统计汇总

| 项目 | 数量 |
|------|-----:|
| 修改文件数 | 11 |
| 删除 `@RequiredArgsConstructor` 注解 | 11 处 |
| 删除 `import lombok.RequiredArgsConstructor` | 11 处 |
| 新增 `@Autowired` 注解 | 20 处 |
| 新增 `import org.springframework.beans.factory.annotation.Autowired` | 11 处 |
| 移除字段 `final` 修饰符 | 20 处 |
| pom.xml 变更 | 0 |
