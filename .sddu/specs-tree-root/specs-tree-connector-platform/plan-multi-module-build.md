# Maven 单进程合并构建方案

**Feature ID**: CONN-PLAT-001  
**关联文档**: plan.md (§1.2 技术栈确认, §4.6 目录结构规划), plan-code.md (§1~§16 代码规范)  
**版本**: v2.0  
**创建日期**: 2026-05-27  
**最后更新**: 2026-05-27  
**对齐基线**: plan.md v2.8.1

---

## 1. 背景与目标

### 1.1 背景

当前项目 `open-app` 包含 4 个独立的 Spring Boot 服务：

| 服务 | Spring Boot | IO 模型 | ORM | 端口 | GroupId |
|------|------------|---------|-----|------|---------|
| **api-server** | 3.4.6 | Servlet (Tomcat) | MyBatis/JDBC | 18081 | `...wecode` |
| **connector-api** | **3.5.14** | WebFlux (Netty) | R2DBC | 18180 | `...wecode.v2` |
| **open-server** | 3.4.6 | Servlet (Tomcat) | MyBatis/JDBC | 18080 | `...wecode.v2` |
| **event-server** | 3.4.6 | Servlet (Tomcat) | MyBatis/JDBC | 18083 | `...wecode` |

每个服务独立 Maven 工程、独立 JAR、独立 JVM 进程。这带来了运维复杂度高、依赖重复、公共代码散落等问题。

**本次方案的核心思路**：将 4 个服务合并为 **一个 Spring Boot 工程、一个 JAR、一个启动类、一个端口**，在同一个 Tomcat 进程中承载全部能力。

### 1.2 目标

1. **单工程、单 JAR**：一个 `pom.xml`，一次 `mvn package` 产出一个可执行 JAR
2. **单启动类**：一个 `@SpringBootApplication` 入口
3. **单进程、单端口**：一个 JVM 进程，统一端口 `18080`
4. **代码物理合并**：4 个服务的源码整合到一个 `src/main/java` 下，消除重复
5. **技术栈共存**：Servlet MVC + WebFlux + JDBC + R2DBC 在一个 classpath 下安全共存

---

## 2. 现状分析

### 2.1 各服务依赖矩阵

```
依赖项                   api-server  connector-api  open-server  event-server
──────────────────────────────────────────────────────────────────────────
spring-boot-starter-web      ✅           ❌             ✅            ✅
spring-boot-starter-webflux  ❌           ✅             ❌            ❌
spring-boot-starter-actuator ✅           ✅             ✅            ✅
spring-boot-starter-validation✅          ✅             ✅            ✅
spring-boot-starter-aop      ❌           ❌             ✅            ❌
spring-boot-starter-websocket❌           ❌             ❌            ✅
mybatis-spring-boot-starter  ✅           ❌             ✅            ❌
mysql-connector-j            ✅           ❌             ✅            ❌
spring-boot-starter-data-redis✅         ❌             ✅            ✅
spring-data-redis-reactive   ❌           ✅             ❌            ❌
spring-boot-starter-data-r2dbc❌         ✅             ❌            ❌
r2dbc-mysql                  ❌           ✅             ❌            ❌
bucket4j-core                ❌           ✅             ❌            ❌
springdoc-openapi            ✅ (webmvc)  ✅ (webflux)   ✅ (webmvc)   ✅ (webmvc)
lombok                       ✅           ✅             ✅            ✅
reactor-test                 ❌           ✅             ❌            ❌
```

### 2.2 合并后的依赖全集

合并后，`pom.xml` 将同时包含以上所有依赖。关键是确认哪些组合会有冲突。

---

## 3. 目标架构

### 3.1 工程结构

```
open-app/
├── pom.xml                                  # 唯一的 pom.xml
│   └── parent: spring-boot-starter-parent 3.5.14
│   └── 同时引入 web + webflux + jdbc + r2dbc + redis + redis-reactive
│
├── src/main/java/com/xxx/open/
│   ├── OpenAppApplication.java              # 🆕 唯一启动类
│   │
│   ├── common/                              # 公共代码（原散落各处）
│   │   ├── model/ApiResponse.java
│   │   ├── exception/BusinessException.java
│   │   ├── exception/GlobalExceptionHandler.java
│   │   ├── util/SignatureUtil.java
│   │   ├── config/JacksonConfig.java
│   │   └── constant/ErrorCode.java
│   │
│   ├── api/                                 # ← 原 api-server 代码
│   │   ├── gateway/controller/ApiGatewayController.java
│   │   ├── scope/controller/ScopeController.java
│   │   ├── data/controller/DataQueryController.java
│   │   ├── common/controller/HealthController.java
│   │   ├── .../mapper/PermissionMapper.java
│   │   └── .../service/...
│   │
│   ├── connector/                           # ← 原 connector-api 代码
│   │   ├── trigger/controller/TriggerController.java
│   │   ├── debug/controller/TestRunController.java
│   │   ├── runtime/executor/ReactiveSequentialExecutor.java
│   │   ├── .../entity/ConnectorEntity.java (R2DBC)
│   │   └── .../repository/FlowVersionReadRepository.java
│   │
│   ├── open/                                # ← 原 open-server 代码
│   │   ├── connector/controller/ConnectorController.java
│   │   ├── flow/controller/FlowController.java
│   │   ├── .../mapper/ConnectorMapper.java
│   │   └── .../service/...
│   │
│   └── event/                               # ← 原 event-server 代码
│       ├── controller/EventController.java
│       └── .../service/...
│
├── src/main/resources/
│   ├── application.yml                      # 统一配置
│   └── mapper/                              # MyBatis XML（不动）
│
├── wecodesite/                              # 前端（独立 npm，不参与）
└── open-web/                                # 前端（独立 npm）
```

### 3.2 部署拓扑

```
┌──────────────────────────────────────────────┐
│               一个 JVM 进程                    │
│  open-app-platform.jar  :18080               │
│                                              │
│  ┌────────────────────────────────────────┐  │
│  │         Tomcat (嵌入式 Web 容器)         │  │
│  │  ┌──────────┐  ┌──────────┐            │  │
│  │  │ Servlet  │  │ WebFlux  │            │  │
│  │  │ MVC      │  │ Controller│           │  │
│  │  │ (api/    │  │ (connector│           │  │
│  │  │  open/   │  │  /trigger)│           │  │
│  │  │  event)  │  │           │            │  │
│  │  └────┬─────┘  └────┬──────┘            │  │
│  └───────┼─────────────┼───────────────────┘  │
│          │             │                       │
│  ┌───────┴─────┐ ┌─────┴────────┐             │
│  │ JDBC 连接池  │ │ R2DBC 连接池 │             │
│  │ (MyBatis)   │ │ (Reactive)   │             │
│  └──────┬──────┘ └──────┬───────┘             │
│         │               │                      │
│  ┌──────┴──────┐ ┌──────┴───────┐             │
│  │ Redis 同步   │ │ Redis Reactive│            │
│  └─────────────┘ └──────────────┘             │
└──────────────────────────────────────────────┘
         │               │
         ▼               ▼
    ┌────────┐    ┌──────────┐
    │ MySQL  │    │  Redis   │
    └────────┘    └──────────┘
```

> 🔑 **核心原则**：一个 JVM、一个 Tomcat、所有 Controller（无论是 Servlet MVC 还是 WebFlux 风格）统一注册到同一个 Web 容器。JDBC 和 R2DBC 各自管理连接池，互不干扰。

---

## 4. 版本统一

### 4.1 Spring Boot 版本

**决策**：统一到 **3.5.14**（内部企业版本）

connector-api 已在 3.5.14 上稳定运行，其余 3 个服务（api-server / open-server / event-server）需从 3.4.6 升级。3.4 → 3.5 为小版本升级，向后兼容性高。

### 4.2 Java 版本

**决策**：**Java 21**

Java 21 向下兼容 Java 17 代码。3.5.14 以 Java 21 为基准。

### 4.3 公共依赖版本

```xml
<properties>
    <java.version>21</java.version>
    <mybatis-spring-boot.version>3.0.4</mybatis-spring-boot.version>
    <springdoc-openapi.version>2.5.0</springdoc-openapi.version>
    <r2dbc-mysql.version>1.3.2</r2dbc-mysql.version>
    <bucket4j.version>8.10.1</bucket4j.version>
</properties>
```

---

## 5. pom.xml 设计

### 5.1 完整 pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.5.14</version>
        <relativePath/>
    </parent>

    <groupId>com.xxx.it.works.wecode</groupId>
    <artifactId>open-app-platform</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>jar</packaging>

    <name>Open App Platform</name>
    <description>WeCode 能力开放平台 - 单进程聚合服务</description>

    <properties>
        <java.version>21</java.version>
        <mybatis-spring-boot.version>3.0.4</mybatis-spring-boot.version>
        <springdoc-openapi.version>2.5.0</springdoc-openapi.version>
        <r2dbc-mysql.version>1.3.2</r2dbc-mysql.version>
        <bucket4j.version>8.10.1</bucket4j.version>
    </properties>

    <dependencies>
        <!-- ========== Web 层 ========== -->
        <!-- Servlet MVC（api-server / open-server / event-server） -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- WebFlux（connector-api） -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-webflux</artifactId>
        </dependency>

        <!-- WebSocket（event-server） -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-websocket</artifactId>
        </dependency>

        <!-- AOP（open-server） -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-aop</artifactId>
        </dependency>

        <!-- ========== 数据访问层 ========== -->
        <!-- MyBatis（api-server / open-server） -->
        <dependency>
            <groupId>org.mybatis.spring.boot</groupId>
            <artifactId>mybatis-spring-boot-starter</artifactId>
            <version>${mybatis-spring-boot.version}</version>
        </dependency>
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>

        <!-- R2DBC（connector-api） -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-r2dbc</artifactId>
        </dependency>
        <dependency>
            <groupId>io.asyncer</groupId>
            <artifactId>r2dbc-mysql</artifactId>
            <version>${r2dbc-mysql.version}</version>
        </dependency>

        <!-- ========== 缓存层 ========== -->
        <!-- Redis 同步（api-server / open-server / event-server） -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>

        <!-- Redis Reactive（connector-api） -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis-reactive</artifactId>
        </dependency>

        <!-- ========== 横切关注点 ========== -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>

        <!-- SpringDoc OpenAPI（仅保留 webmvc 版本） -->
        <dependency>
            <groupId>org.springdoc</groupId>
            <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
            <version>${springdoc-openapi.version}</version>
        </dependency>

        <!-- Bucket4j 限流（connector-api） -->
        <dependency>
            <groupId>com.bucket4j</groupId>
            <artifactId>bucket4j-core</artifactId>
            <version>${bucket4j.version}</version>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- ========== 测试 ========== -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>io.projectreactor</groupId>
            <artifactId>reactor-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                    <!-- 排除 reactor-netty，因为已选择 Tomcat 为嵌入式容器 -->
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

> ⚠️ **注意**：`spring-boot-starter-web` 和 `spring-boot-starter-webflux` 同时存在时，Spring Boot 自动配置**以 web (Tomcat) 为准**。WebFlux 的 Controller 仍正常工作，但底层跑在 Tomcat 而非 Netty。

---

## 6. 技术栈共存分析（核心）

### 6.1 Tomcat 承载 WebFlux Controller

当 `spring-boot-starter-web` 和 `spring-boot-starter-webflux` 同时存在：

```
Spring Boot 启动
      │
      ▼
检测到 BOTH web + webflux
      │
      ▼
自动配置策略：
┌────────────────────────────────────────────────┐
│  WebMvcAutoConfiguration    ← 优先             │
│  嵌入式容器 = Tomcat                            │
│                                                │
│  WebFluxAutoConfiguration   ← 退让             │
│  Netty 不被启动                                │
│  但 WebFlux 的 @RestController 仍然注册并可用   │
└────────────────────────────────────────────────┘
```

**结果**：
- api-server / open-server / event-server 的 `@RestController`（Servlet MVC）→ 原生 Tomcat 线程处理 ✅
- connector-api 的 `@RestController`（WebFlux 风格）→ 也跑在 Tomcat 线程上 ✅
- `Mono<T>` / `Flux<T>` 返回值正常被 Spring 适配为同步 HTTP 响应 ✅

### 6.2 JDBC + R2DBC 共存

```
同一个 classpath:
├── mysql-connector-j (JDBC 驱动)
│   └── HikariCP 连接池 (spring-boot-starter-data-jdbc 自动配置)
│       └── api-server / open-server 的 MyBatis Mapper 使用
│
└── r2dbc-mysql (R2DBC 驱动)
    └── R2DBC ConnectionFactory (spring-boot-starter-data-r2dbc 自动配置)
        └── connector-api 的 R2DBC Repository 使用
```

**不冲突的原因**：
- JDBC 连接池和 R2DBC 连接池是**两套完全独立的组件**
- 各自配置各自的数据库连接参数（`spring.datasource.*` vs `spring.r2dbc.*`）
- 不同包路径的 Bean 互不干扰

### 6.3 Redis 同步 + Reactive 共存

```
同一个 classpath:
├── RedisTemplate / StringRedisTemplate (同步)
│   └── api-server / open-server / event-server 使用
│
└── ReactiveRedisTemplate / ReactiveStringRedisTemplate (Reactive)
    └── connector-api 使用
```

**不冲突**：两套客户端连接同一个 Redis 实例，Lettuce 底层驱动是相同的，只是上层 API 封装不同（同步包装器 vs Reactive 包装器）。

### 6.4 MyBatis + R2DBC Repository 共存

```
同一个 Spring Context:
├── @MapperScan("com.xxx.open.api.*.mapper")    # MyBatis 扫描
│   └── api-server / open-server 的 Mapper 接口
│
└── @EnableR2dbcRepositories("com.xxx.open.connector.*.repository")  # R2DBC 扫描
    └── connector-api 的 ReactiveCrudRepository
```

**不冲突**：扫描不同包路径，各自管理各自的持久化 Bean。

### 6.5 connector-api 失去什么、保留什么

| 维度 | 独立 Netty 进程（原来） | 合并到 Tomcat 进程（现在） |
|------|----------------------|--------------------------|
| **并发模型** | EventLoop × 4-8 线程 | Tomcat 线程池 × 200 线程 |
| **单请求线程占用** | 不阻塞（异步回调） | 占用一个线程直到返回 |
| **最大并发** | ~5000+（取决于内存） | ~200（取决于线程池大小） |
| **R2DBC 异步 IO** | ✅ 有效减少线程等待 | ✅ 仍然异步，但请求线程还是被占用 |
| **WebClient 异步 IO** | ✅ 有效减少线程等待 | ✅ 仍然异步，但请求线程还是被占用 |
| **Mono/Flux 链式编排** | ✅ | ✅ 完全保留 |
| **ExpressionResolver** | ✅ | ✅ 完全保留 |
| **Bucket4j 限流** | ✅ WebFilter | ✅ WebFilter（在 Tomcat 上也生效） |
| **BlockHound** | ✅ 可用 | ❌ 不再需要（本身就是同步线程模型） |

> 📌 **结论**：MVP 阶段 connector-api 并发量预估 <200，Tomcat 默认 200 线程池完全够用。未来如需高并发（>1000），可随时将 connector-api 的代码拆分出去独立部署，代码无需大改（只是切回独立 WebFlux + Netty）。

---

## 7. 需要解决的冲突

### 7.1 排除 reactor-netty

既然用 Tomcat 做嵌入式容器，不再需要 Netty 服务器。排除以减少 JAR 体积和潜在冲突：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
    <exclusions>
        <exclusion>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-reactor-netty</artifactId>
        </exclusion>
    </exclusions>
</dependency>
```

`WebClient` 在没有 Netty 时，Spring 会自动回退到 Tomcat 的 HTTP 连接器（或 Jetty），或者可以保留 Netty 仅作为 WebClient 的底层客户端（不启动服务器）。

### 7.2 SpringDoc OpenAPI 冲突

`springdoc-openapi-starter-webmvc-ui` 和 `springdoc-openapi-starter-webflux-ui` **不能同时存在**。

**决策**：**只保留 webmvc 版本**。

原因是 WebFlux Controller 在 Tomcat 上运行时，SpringDoc 的 webmvc 版本可以正常扫描和生成文档。WebFlux 版本会尝试启动 Netty 特定的文档端点，与 Tomcat 冲突。

```xml
<!-- 保留 -->
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
</dependency>

<!-- ❌ 删除 -->
<!-- <dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webflux-ui</artifactId>
</dependency> -->
```

### 7.3 端口统一

原来 4 个服务各用各的端口，合并后只需要一个：

```yaml
server:
  port: 18080
```

原来的 context-path（如 `/api-server`）不再需要，Controller 的 `@RequestMapping` 路径直接生效。如有路径冲突，用包级别的路径前缀区分。

### 7.4 数据库配置

两个数据库（`openplatform` 和 `openapp`）的配置需要共存：

```yaml
spring:
  # JDBC 数据源（api-server / open-server / event-server 使用）
  datasource:
    url: jdbc:mysql://localhost:3306/openplatform?...
    username: root
    password: root
    hikari:
      maximum-pool-size: 30

  # R2DBC 数据源（connector-api 使用）
  r2dbc:
    url: r2dbc:mysql://localhost:3306/openapp?...
    username: openapp
    password: openapp
    pool:
      max-size: 20
      initial-size: 5
```

### 7.5 启动类

```java
@SpringBootApplication
@MapperScan("com.xxx.open.api.*.mapper")           // MyBatis 扫描
@EnableR2dbcRepositories("com.xxx.open.connector") // R2DBC 扫描
public class OpenAppApplication {
    public static void main(String[] args) {
        SpringApplication.run(OpenAppApplication.class, args);
    }
}
```

---

## 8. 代码迁移清单

### 8.1 包路径映射

| 原服务 | 原包路径 | 新包路径 |
|--------|---------|---------|
| api-server | `com.xxx.api.*` | `com.xxx.open.api.*` |
| connector-api | `com.xxx.it.works.wecode.v2.*` | `com.xxx.open.connector.*` |
| open-server | `com.xxx.it.works.wecode.v2.*` | `com.xxx.open.open.*` |
| event-server | `com.xxx.event.*` | `com.xxx.open.event.*` |

> ⚠️ connector-api 和 open-server 原来共享 `com.xxx.it.works.wecode.v2` 包前缀，合并后需拆分到不同的子包以避免类名冲突。

### 8.2 需要合并的重复类

| 类 | 原位置 | 新位置（合并为一份） |
|----|--------|---------------------|
| `ApiResponse` | api-server, open-server | `com.xxx.open.common.model.ApiResponse` |
| `BusinessException` | api-server, open-server | `com.xxx.open.common.exception.BusinessException` |
| `SignatureUtil` | api-server | `com.xxx.open.common.util.SignatureUtil` |
| `JacksonConfig` | api-server, connector-api | `com.xxx.open.common.config.JacksonConfig` |
| `GlobalExceptionHandler` | api-server（`@RestControllerAdvice`） | `com.xxx.open.common.exception.GlobalExceptionHandler` |

### 8.3 各服务需保留的独立配置

| 配置项 | 说明 |
|--------|------|
| `RedisConfig`（同步） | api-server 的自定义 RedisTemplate 配置 |
| `ReactiveRedisConfig` | connector-api 的 ReactiveRedisTemplate 配置 |
| `R2dbcConfig` | connector-api 的 R2DBC ConnectionFactory 配置 |
| `RateLimitFilter` | connector-api 的 Bucket4j WebFilter |
| `WebSocketConfig` | event-server 的 WebSocket STOMP 配置 |
| `MyBatis Mapper XML` | api-server + open-server 的 XML mapper，路径不变 |

---

## 9. 实施计划

### 9.1 分阶段实施

```
Phase 1         Phase 2           Phase 3           Phase 4
(1天)           (2-3天)           (2-3天)           (1-2天)
┌──────────┐   ┌──────────────┐   ┌──────────────┐   ┌──────────┐
│ 创建工程  │ → │ 迁移 api-    │ → │ 迁移 connector│ → │ 合并配置  │
│ pom.xml  │   │ server/open/  │   │ -api 代码    │   │ 全量联调  │
│ 启动类   │   │ event 代码    │   │ 解决冲突     │   │ 测试验证  │
└──────────┘   └──────────────┘   └──────────────┘   └──────────┘
```

### 9.2 Phase 1：创建统一工程（1 天）

1. 创建新的 `pom.xml`（参考 §5.1）
2. 创建 `OpenAppApplication.java` 启动类
3. 创建 `application.yml` 统一配置（合并各服务的配置）
4. 引入全部依赖，验证能成功编译（空 `src/main/java` 下 `mvn clean compile`）

### 9.3 Phase 2：迁移 Servlet 服务代码（2-3 天）

按复杂度从低到高：

| 顺序 | 服务 | 迁移内容 | 验证 |
|:----:|------|---------|------|
| 1 | **event-server** | 拷贝 WebSocket + Controller | 启动无报错 |
| 2 | **api-server** | 拷贝 gateway/scope/data 三模块 + MyBatis Mapper | 逐 Controller 健康检查 |
| 3 | **open-server** | 拷贝 connector/flow/monitor/debug 四模块 + MyBatis Mapper | 逐 Controller 健康检查 |

每步验证：
- 编译通过
- 启动无 Bean 冲突
- `GET /actuator/health` 正常

### 9.4 Phase 3：迁移 connector-api 代码并解决冲突（2-3 天）

1. 拷贝 connector-api 的全部代码到 `com.xxx.open.connector` 包
2. 排除 `reactor-netty`（如 §7.1）
3. 删除 `springdoc-openapi-starter-webflux-ui`，统一用 webmvc 版本（如 §7.2）
4. 合并 `JacksonConfig` 到 `common.config` 包（只保留一份）
5. 验证 R2DBC + JDBC 双数据源同时启动
6. 验证限流 WebFilter 在 Tomcat 容器上生效

### 9.5 Phase 4：合并配置与全量联调（1-2 天）

1. 合并 4 个服务的 `application.yml` 为一份
2. 处理 profile（dev/prod）配置
3. 全量启动，逐端口验证原各服务的核心 API：
   ```
   # 原 api-server 的健康检查
   curl http://localhost:18080/api/v1/health
   
   # 原 connector-api 的 HTTP 触发
   curl -X POST http://localhost:18080/api/v1/trigger/{flowId}/invoke
   
   # 原 open-server 的管理接口
   curl http://localhost:18080/service/open/v2/connector/list
   
   # 原 event-server 的 WebSocket
   ws://localhost:18080/ws/...
   ```
4. 执行全量测试：`mvn clean test`

---

## 10. 风险评估

| 风险 | 影响 | 概率 | 缓解措施 |
|------|------|:----:|---------|
| **3 个 Servlet 服务从 3.4.6 升级到 3.5.14 出现兼容性问题** | 服务部分功能异常 | 低-中 | Phase 2 逐个服务迁移，每步验证；open-server 含 AOP 需重点验证 |
| **WebFlux Controller 在 Tomcat 上行为异常** | connector-api 功能不可用 | 低 | Spring 官方支持 WebFlux on Tomcat；Phase 3 优先验证 trigger/debug 端点 |
| **SpringDoc webmvc 无法扫描 WebFlux 端点** | Swagger UI 缺少 connector-api 文档 | 中 | 手动配置 `GroupedOpenApi` 分组；必要时保留独立 swagger 端点 |
| **JDBC + R2DBC 连接池耗尽** | 数据库连接不足 | 低 | 合理配置两套连接池上限，总和不超过 MySQL max_connections |
| **合并后类名冲突** | 编译失败 | 中 | connector-api 和 open-server 原来共用 `v2` 包，需拆分（见 §8.1） |
| **单点故障** | 一个模块异常导致整个进程崩溃 | 中 | 增强异常隔离（`@RestControllerAdvice` 按包路径区分）；未来可拆分回去 |
| **WebSocket 与 Tomcat 兼容** | event-server WebSocket 不可用 | 低 | `spring-boot-starter-websocket` 原生支持 Tomcat |
| **启动时间变长** | 所有 Bean 一起初始化 | 低 | 合理使用 `@Lazy` 注解懒加载非核心 Bean |

---

## 11. 架构决策记录 (ADR)

### ADR-004：单进程聚合部署

- **状态**：提议中
- **决策**：将 api-server、connector-api、open-server、event-server 合并为一个 Spring Boot 工程，产出单一 JAR，在单个 Tomcat 进程中运行。同时引入 `spring-boot-starter-web` + `spring-boot-starter-webflux`，嵌入式容器选 Tomcat
- **备选方案**：
  - 方案 A（当前）：4 个独立工程、独立进程 —— 运维复杂、依赖重复
  - 方案 B（本方案）：单工程、单 JAR、单进程 —— 简化部署、消除重复
  - 方案 C：多模块 Maven + 独立部署 —— 统一版本但不合并进程
- **后果**：
  - ✅ 部署极简：一个 JAR、一个端口、一个命令启动
  - ✅ 消除公共代码重复（ApiResponse / BusinessException / JacksonConfig 等）
  - ✅ 统一依赖版本管理（一个 pom.xml）
  - ⚠️ connector-api 失去 Netty EventLoop 高并发优势（MVP 阶段 <200 并发可接受）
  - ⚠️ 单点故障风险增加（缓解：未来可按需拆分回独立进程）
  - ⚠️ WebFlux Controller 在 Tomcat 上运行，需验证行为一致性

### ADR-005：容器选型 —— Tomcat（放弃 Netty）

- **状态**：提议中
- **决策**：当 `web` 和 `webflux` 同时存在时，选择 **Tomcat** 作为嵌入式容器（Spring Boot 默认行为），排除 `reactor-netty`
- **理由**：3 个 Servlet 服务（api-server / open-server / event-server）对 Tomcat 有强依赖（Interceptor、WebSocket STOMP、Filter 链），迁移到 Netty 成本高；而 connector-api 的 WebFlux Controller 在 Tomcat 上完全可用
- **备选方案**：选择 Netty 作为容器，3 个 Servlet 服务改用 WebFlux 重写 —— 工作量过大，不现实
- **后果**：
  - ✅ api-server / open-server / event-server 零改动
  - ⚠️ connector-api 的 BlockHound 不再适用（移除）
  - ⚠️ 需排除 `spring-boot-starter-reactor-netty` 避免无用依赖

### ADR-006：SpringDoc 统一使用 webmvc 版本

- **状态**：提议中
- **决策**：仅保留 `springdoc-openapi-starter-webmvc-ui`，删除 `springdoc-openapi-starter-webflux-ui`
- **理由**：容器是 Tomcat，WebFlux Controller 在 Tomcat 上时 webmvc 版本的 SpringDoc 可以正常扫描
- **后果**：
  - ✅ 避免两个 SpringDoc starter 冲突
  - ⚠️ 如果部分 WebFlux 端点无法被自动扫描到，需手动配置 `GroupedOpenApi`

---

## 12. 修订记录

| 版本 | 日期 | 修订内容 | 修订人 |
|------|------|---------|--------|
| v1.0 | 2026-05-27 | 初始版本 —— Maven 多模块独立进程方案 | SDDU |
| **v2.0** | **2026-05-27** | **方案重构**：从多模块独立进程改为单进程聚合部署。新增 §6 技术栈共存分析（Tomcat+WebFlux/JDBC+R2DBC/Redis共存）、§7 冲突解决方案、ADR-005/006 | SDDU |

---

## 附录 A：启动验证命令

```bash
# 1. 编译打包
cd open-app
mvn clean package -DskipTests

# 2. 启动（单 JAR）
java -jar target/open-app-platform-1.0.0-SNAPSHOT.jar

# 3. 健康检查
curl http://localhost:18080/actuator/health

# 4. 验证各模块端点
# api-server: 健康检查
curl http://localhost:18080/api/v1/health

# connector-api: HTTP 触发（需有效 flowId）
curl -X POST http://localhost:18080/api/v1/trigger/test-flow/invoke \
  -H "Content-Type: application/json" \
  -d '{"test": true}'

# open-server: 管理接口
curl http://localhost:18080/service/open/v2/connector/list

# 5. Swagger UI
open http://localhost:18080/swagger-ui.html
```
