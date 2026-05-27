# Maven 多模块构建方案

**Feature ID**: CONN-PLAT-001  
**关联文档**: plan.md (§1.2 技术栈确认, §4.6 目录结构规划), plan-code.md (§1~§16 代码规范)  
**版本**: v1.0  
**创建日期**: 2026-05-27  
**最后更新**: 2026-05-27  
**对齐基线**: plan.md v2.8.1

---

## 1. 背景与目标

### 1.1 背景

当前项目 `open-app` 包含 4 个独立的 Spring Boot 服务：

| 服务 | Spring Boot | GroupId | Java | IO 模型 | ORM | 端口 |
|------|------------|---------|------|---------|-----|------|
| **api-server** | 3.4.6 → 3.5.14 | `com.xxx.it.works.wecode` | 17+ | Servlet (Tomcat) | MyBatis/JDBC | 18081 |
| **connector-api** | **3.5.14** | `com.xxx.it.works.wecode.v2` | 21 | WebFlux (Netty) | R2DBC | 18180 |
| **open-server** | 3.4.6 → 3.5.14 | `com.xxx.it.works.wecode.v2` | 17+ | Servlet (Tomcat) | MyBatis/JDBC | 18080 |
| **event-server** | 3.4.6 → 3.5.14 | `com.xxx.it.works.wecode` | 17+ | Servlet (Tomcat) | MyBatis/JDBC | 18083 |

> 📌 connector-api 使用的 3.5.14 为内部企业定制版本（非 Spring 开源社区版本线），已在 connector-api 中验证稳定。

每个服务都是**独立 Maven 工程**，各自直接继承 `spring-boot-starter-parent`，pom.xml 中有统一注释："直接继承 Spring Boot，不再继承根 pom"——说明此前已经考虑过统一父 POM 的问题，但由于当时各服务版本差异或其他原因选择独立。

**当前痛点**：
1. **版本漂移**：3 个服务使用 Spring Boot 3.4.6，connector-api 使用 3.5.14，版本不统一导致依赖管理困难
2. **依赖重复声明**：4 个 pom.xml 重复声明 `springdoc-openapi`、`lombok`、`actuator`、`validation` 等公共依赖
3. **groupId 不统一**：`com.xxx.it.works.wecode` vs `com.xxx.it.works.wecode.v2` 两种命名
4. **公共代码散落**：`ApiResponse`、`BusinessException`、`SignatureUtil` 等公共类在各服务中重复实现
5. **无法统一构建**：`mvn clean package` 需要分别在 4 个目录下执行
6. **CI/CD 复杂**：每个服务需要独立的构建流水线配置

### 1.2 目标

1. **统一父 POM**：所有服务继承同一个 `open-app-platform` 父 POM，统一 Spring Boot 版本和公共依赖版本
2. **提取公共模块**：将跨服务共享的代码（实体、工具类、DTO、异常）提取到 `open-app-common` 模块
3. **保留独立部署**：每个服务仍产出独立 Spring Boot JAR，独立进程启动
4. **技术栈隔离**：Servlet (api-server/open-server/event-server) 和 WebFlux (connector-api) 在**进程级别**隔离，不混用
5. **一键构建**：根目录 `mvn clean package` 即可构建全部服务

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

### 2.2 关键差异项

| 差异项 | api-server | connector-api | 风险等级 |
|--------|-----------|---------------|:--------:|
| **Spring Boot 版本** | 3.4.6 | **3.5.14** | 🟡 中 |
| **Web 栈** | Servlet/Tomcat | WebFlux/Netty | 🟡 中（进程隔离可规避） |
| **数据访问** | MyBatis/JDBC | R2DBC | 🟡 中（进程隔离可规避） |
| **Redis 客户端** | Lettuce 同步 | Lettuce Reactive | 🟢 低（不同服务不同配置） |
| **Java 版本** | 17+ | 21 | 🟢 低（21 向下兼容） |
| **groupId** | `...wecode` | `...wecode.v2` | 🟢 低（语义问题） |

> 📌 3.5.14 为内部企业定制版本，且为本次统一构建的基准版本。3 个 Servlet 服务需升级至此版本并验证兼容性。

### 2.3 Spring Boot 版本说明

> 📌 `connector-api/pom.xml` 声明 `spring-boot-starter-parent` 版本 `3.5.14`。该版本为**内部企业定制版本**（非 Spring 开源社区版本线），已在 connector-api 中经过验证稳定运行（WebFlux + R2DBC + Bucket4j）。

本次统一以 **3.5.14 为基准**，api-server / open-server / event-server 从 3.4.6 升级至 3.5.14，需验证以下兼容性：

| 验证项 | 影响服务 | 关键检查点 |
|--------|---------|-----------|
| MyBatis Spring Boot Starter 3.0.4 兼容 | api-server, open-server | Mapper 扫描、XML 解析、事务管理 |
| Spring MVC 行为变更 | api-server, open-server, event-server | 拦截器、异常处理、JSON 序列化 |
| Redis (Lettuce 同步客户端) | api-server, open-server, event-server | 连接池配置、序列化方式 |
| SpringDoc OpenAPI | 全部 | Swagger UI 正常展示 |
| WebSocket (event-server) | event-server | STOMP 端点、心跳机制 |
| AOP (open-server) | open-server | 切面拦截正常 |

**connector-api 无需变更**（已在 3.5.14 上开发并验证通过）。

---

## 3. 目标架构

### 3.1 目录结构

```
open-app/
├── pom.xml                          # 根 POM（packaging: pom）
│   └── parent: spring-boot-starter-parent 3.5.14
│
├── open-app-common/                 # 🆕 公共模块（library jar，非可执行）
│   ├── pom.xml
│   └── src/main/java/com/xxx/open/common/
│       ├── model/
│       │   └── ApiResponse.java     # 统一响应体
│       ├── exception/
│       │   ├── BusinessException.java
│       │   └── GlobalExceptionHandler.java
│       ├── util/
│       │   └── SignatureUtil.java   # AKSK 签名工具
│       ├── config/
│       │   └── JacksonConfig.java   # Jackson 全局配置
│       └── constant/
│           └── Constants.java       # 公共常量
│
├── api-server/                      # API 认证鉴权服务
│   ├── pom.xml                      # parent: open-app-platform
│   └── src/...
│
├── connector-api/                   # 连接器运行时服务
│   ├── pom.xml                      # parent: open-app-platform
│   ├── src/main/java/.../v2/        # WebFlux + R2DBC
│   └── src/...
│
├── open-server/                     # 开放平台管理服务
│   ├── pom.xml                      # parent: open-app-platform
│   └── src/...
│
├── event-server/                    # 事件/回调网关服务
│   ├── pom.xml                      # parent: open-app-platform
│   └── src/...
│
├── wecodesite/                      # 前端（独立 npm 项目，不参与 Maven 构建）
├── open-web/                        # 前端（独立 npm 项目）
├── .sddu/                           # SDDU 规范与计划文档
└── docs/                            # 文档
```

### 3.2 部署拓扑（不变）

```
┌─────────────────────────────────────────────────────┐
│                    Maven 工程                         │
│  open-app-platform (root pom)                       │
│  ├── open-app-common (library)                      │
│  ├── api-server      → api-server.jar  :18081       │
│  ├── connector-api   → connector-api.jar :18180     │
│  ├── open-server     → open-server.jar  :18080      │
│  └── event-server    → event-server.jar  :18083     │
└─────────────────────────────────────────────────────┘

运行时（4 个独立 JVM 进程）：
  JVM-1: api-server      [Servlet/Tomcat + MyBatis/JDBC]
  JVM-2: connector-api   [WebFlux/Netty  + R2DBC]
  JVM-3: open-server     [Servlet/Tomcat + MyBatis/JDBC]
  JVM-4: event-server    [Servlet/Tomcat + MyBatis/JDBC]

共享基础设施：
  MySQL (同一实例，不同 database: openplatform / openapp)
  Redis (同一实例)
```

> 🔑 **核心原则**：Maven 模块化仅在**编译期**共享代码和版本管理，**运行时完全独立**。Servlet 和 WebFlux 在各自 JVM 进程中各司其职，不存在 IO 模型冲突。

---

## 4. 版本统一策略

### 4.1 Spring Boot 版本

**决策**：统一到 **3.5.14**（内部企业版本）

| 服务 | 当前版本 | 目标版本 | 操作 |
|------|---------|---------|------|
| api-server | 3.4.6 | **3.5.14** | 升级 |
| connector-api | 3.5.14 | 3.5.14 | **不变** |
| open-server | 3.4.6 | **3.5.14** | 升级 |
| event-server | 3.4.6 | **3.5.14** | 升级 |

**api-server / open-server / event-server 升级兼容性验证清单**：

| 验证项 | 方法 | 预期结果 |
|--------|------|---------|
| MyBatis 正常工作 | api-server/open-server 启动后查询任意 Mapper | 正常返回数据 |
| Spring MVC 端点正常 | 各服务核心 API 回归测试 | 200 OK |
| Redis 连接正常 | `GET /actuator/health` | health UP |
| Jackson 序列化兼容 | Long→String、日期格式化 | 格式不变 |
| WebSocket (event-server) | WebSocket 连接测试 | 正常通信 |
| SpringDoc OpenAPI | 访问 `/swagger-ui.html` | Swagger UI 正常 |

### 4.2 Java 版本

**决策**：统一到 **Java 21**

- Java 21 向下兼容 Java 17 代码，无需修改源代码
- Spring Boot 3.4.x 官方推荐 Java 21
- connector-api 已声明 `<java.version>21</java.version>`
- 其他服务未声明，继承自 `spring-boot-starter-parent` 也是 21

### 4.3 groupId 统一

**决策**：统一为 `com.xxx.it.works.wecode`

```xml
<!-- 根 POM -->
<groupId>com.xxx.it.works.wecode</groupId>
<artifactId>open-app-platform</artifactId>
```

现有的 `v2` 后缀是历史遗留的包名区分（`com.xxx.it.works.wecode.v2`），仅影响 Java 源码包路径，不影响 Maven groupId。各服务的 `src/main/java/` 下包名可保持不变。

### 4.4 公共依赖版本

```xml
<properties>
    <java.version>21</java.version>
    <spring-boot.version>3.5.14</spring-boot.version>
    <mybatis-spring-boot.version>3.0.4</mybatis-spring-boot.version>
    <springdoc-openapi.version>2.5.0</springdoc-openapi.version>
    <r2dbc-mysql.version>1.3.2</r2dbc-mysql.version>
    <bucket4j.version>8.10.1</bucket4j.version>
</properties>
```

---

## 5. 父 POM 设计

### 5.1 根 POM (`open-app/pom.xml`)

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
    <packaging>pom</packaging>

    <name>Open App Platform</name>
    <description>WeCode 能力开放平台 - 多模块聚合工程</description>

    <modules>
        <module>open-app-common</module>
        <module>api-server</module>
        <module>connector-api</module>
        <module>open-server</module>
        <module>event-server</module>
    </modules>

    <properties>
        <!-- 统一 Java 版本 -->
        <java.version>21</java.version>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>

        <!-- 第三方依赖版本 -->
        <mybatis-spring-boot.version>3.0.4</mybatis-spring-boot.version>
        <springdoc-openapi.version>2.5.0</springdoc-openapi.version>
        <r2dbc-mysql.version>1.3.2</r2dbc-mysql.version>
        <bucket4j.version>8.10.1</bucket4j.version>

        <!-- 项目版本 -->
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <dependencyManagement>
        <dependencies>
            <!-- open-app-common 模块 -->
            <dependency>
                <groupId>com.xxx.it.works.wecode</groupId>
                <artifactId>open-app-common</artifactId>
                <version>${project.version}</version>
            </dependency>

            <!-- MyBatis -->
            <dependency>
                <groupId>org.mybatis.spring.boot</groupId>
                <artifactId>mybatis-spring-boot-starter</artifactId>
                <version>${mybatis-spring-boot.version}</version>
            </dependency>

            <!-- SpringDoc OpenAPI -->
            <dependency>
                <groupId>org.springdoc</groupId>
                <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
                <version>${springdoc-openapi.version}</version>
            </dependency>
            <dependency>
                <groupId>org.springdoc</groupId>
                <artifactId>springdoc-openapi-starter-webflux-ui</artifactId>
                <version>${springdoc-openapi.version}</version>
            </dependency>

            <!-- R2DBC MySQL Driver -->
            <dependency>
                <groupId>io.asyncer</groupId>
                <artifactId>r2dbc-mysql</artifactId>
                <version>${r2dbc-mysql.version}</version>
            </dependency>

            <!-- Bucket4j -->
            <dependency>
                <groupId>com.bucket4j</groupId>
                <artifactId>bucket4j-core</artifactId>
                <version>${bucket4j.version}</version>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <build>
        <pluginManagement>
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
                    </configuration>
                </plugin>
            </plugins>
        </pluginManagement>
    </build>
</project>
```

### 5.2 子模块 pom.xml 改造示例（以 api-server 为例）

**改造前**：
```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.4.6</version>
</parent>
<groupId>com.xxx.it.works.wecode</groupId>
<artifactId>api-server</artifactId>
<version>1.0.0-SNAPSHOT</version>
```

**改造后**：
```xml
<parent>
    <groupId>com.xxx.it.works.wecode</groupId>
    <artifactId>open-app-platform</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <relativePath>../pom.xml</relativePath>
</parent>
<artifactId>api-server</artifactId>
<!-- groupId 和 version 继承自父 POM，无需再声明 -->

<dependencies>
    <!-- 公共模块 -->
    <dependency>
        <groupId>com.xxx.it.works.wecode</groupId>
        <artifactId>open-app-common</artifactId>
    </dependency>
    
    <!-- 其它依赖 version 继承自父 POM dependencyManagement -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <!-- ... -->
</dependencies>
```

---

## 6. 公共模块设计

### 6.1 `open-app-common` 模块

```
open-app-common/
├── pom.xml
└── src/main/java/com/xxx/open/common/
    ├── model/
    │   └── ApiResponse.java          # 统一响应体
    ├── exception/
    │   ├── BusinessException.java
    │   └── GlobalExceptionHandler.java
    ├── util/
    │   └── SignatureUtil.java        # AKSK/Bearer Token 签名
    ├── config/
    │   └── JacksonConfig.java
    └── constant/
        ├── ErrorCode.java            # 错误码枚举
        └── Constants.java
```

**pom.xml**：
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project>
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.xxx.it.works.wecode</groupId>
        <artifactId>open-app-platform</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>open-app-common</artifactId>
    <packaging>jar</packaging>
    <name>Open App Common</name>
    <description>公共模块：DTO、工具类、异常、配置</description>

    <dependencies>
        <!-- 仅引入公共所需的最小依赖 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
            <!-- 需要 web 是因为 GlobalExceptionHandler 依赖 Spring MVC 注解 -->
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
    </dependencies>

    <!-- ⚠️ 注意：公共模块不需要 spring-boot-maven-plugin，不产出可执行 JAR -->
</project>
```

> ⚠️ **关键问题**：`open-app-common` 引入了 `spring-boot-starter-web`（因为 `GlobalExceptionHandler` 使用了 `@RestControllerAdvice`），而 `connector-api` 是 WebFlux 栈。

**解决方案**：将 `GlobalExceptionHandler` 拆分为两个版本，或使用更抽象的方式：

```
open-app-common/
├── model/ApiResponse.java           # ✅ 无 Web 依赖，所有服务兼容
├── exception/BusinessException.java # ✅ 无 Web 依赖
├── util/SignatureUtil.java         # ✅ 无 Web 依赖
├── config/JacksonConfig.java       # ✅ 无 Web 依赖
└── constant/ErrorCode.java         # ✅ 无 Web 依赖

# 各服务自行实现异常处理适配层：
api-server/src/.../common/exception/GlobalExceptionHandler.java   # @RestControllerAdvice (Spring MVC)
connector-api/src/.../common/exception/DefaultErrorHandler.java   # @RestControllerAdvice (Spring WebFlux)
```

**最终 `open-app-common` 的依赖**：
```xml
<dependencies>
    <!-- Jackson（ApiResponse 序列化需要） -->
    <dependency>
        <groupId>com.fasterxml.jackson.core</groupId>
        <artifactId>jackson-annotations</artifactId>
    </dependency>
    <!-- Jakarta Validation（BusinessException 可能用到） -->
    <dependency>
        <groupId>jakarta.validation</groupId>
        <artifactId>jakarta.validation-api</artifactId>
    </dependency>
    <!-- Lombok -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>
```

这样 `open-app-common` 不引入任何 Web 框架依赖，所有 4 个服务（包括 WebFlux 的 connector-api）都可以安全引用。

### 6.2 公共代码迁移来源

| 类 | 来源服务 | 目标 |
|----|---------|------|
| `ApiResponse` | api-server | open-app-common |
| `BusinessException` | api-server | open-app-common |
| `SignatureUtil` | api-server | open-app-common |
| `JacksonConfig` | api-server | open-app-common |
| `ErrorCode` 枚举 | 新建 | open-app-common |

---

## 7. 技术栈兼容性详细分析

### 7.1 IO 模型冲突分析

```
┌─────────────────────────────────────────────────────────────┐
│                    同一个 Maven 工程                         │
│                                                             │
│  ┌──────────────┐  ┌──────────────┐                         │
│  │ api-server    │  │ connector-api│                         │
│  │ (子模块)      │  │ (子模块)     │                         │
│  │              │  │              │                         │
│  │ Servlet       │  │ WebFlux      │  ← 编译期：互不干扰    │
│  │ Tomcat        │  │ Netty        │                         │
│  │ MyBatis/JDBC  │  │ R2DBC        │                         │
│  └──────┬───────┘  └──────┬───────┘                         │
│         │                  │                                │
└─────────┼──────────────────┼────────────────────────────────┘
          │                  │
          ▼                  ▼
    ┌──────────┐      ┌──────────┐
    │ JVM-1    │      │ JVM-2    │     ← 运行时：完全隔离
    │ :18081   │      │ :18180   │
    │ Servlet  │      │ WebFlux  │
    └──────────┘      └──────────┘
```

> 📌 3 个 Servlet 服务从 3.4.6 升级到 3.5.14，connector-api 保持不变。Servlet 和 WebFlux 的进程隔离策略不受版本统一影响。

### 7.2 依赖兼容性矩阵

| 公共模块 | api-server | connector-api | open-server | event-server |
|---------|:----------:|:-------------:|:-----------:|:------------:|
| open-app-common | ✅ | ✅ | ✅ | ✅ |
| spring-boot-starter-web | ✅ | ❌ | ✅ | ✅ |
| spring-boot-starter-webflux | ❌ | ✅ | ❌ | ❌ |
| mybatis-spring-boot-starter | ✅ | ❌ | ✅ | ❌ |
| spring-boot-starter-data-r2dbc | ❌ | ✅ | ❌ | ❌ |
| spring-boot-starter-data-redis | ✅ | ❌ | ✅ | ✅ |
| spring-data-redis-reactive | ❌ | ✅ | ❌ | ❌ |

> ✅ = 该模块引入此依赖 | ❌ = 该模块**不引入**此依赖

**关键约束**：
- `connector-api` 的 `pom.xml` 中必须**显式排除** `spring-boot-starter-web` 的传递引入，防止 Tomcat 被意外带上
- 由于各服务是**独立 JAR、独立运行**，不会出现同一个 classpath 下同时存在 Tomcat 和 Netty 的情况

### 7.3 connector-api 防污染配置

```xml
<!-- connector-api/pom.xml -->
<dependencies>
    <!-- 如果 open-app-common 不引入 web，这里自然安全 -->
    <dependency>
        <groupId>com.xxx.it.works.wecode</groupId>
        <artifactId>open-app-common</artifactId>
        <!-- 确保不会传递引入 spring-boot-starter-web -->
    </dependency>
    
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-webflux</artifactId>
    </dependency>
    
    <!-- ⚠️ 如果其他模块意外传递引入了 spring-boot-starter-web，
         可以显式排除 -->
</dependencies>

<!-- 可选：使用 maven-enforcer-plugin 强制检查 -->
<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-enforcer-plugin</artifactId>
            <executions>
                <execution>
                    <id>ban-tomcat</id>
                    <goals><goal>enforce</goal></goals>
                    <configuration>
                        <rules>
                            <bannedDependencies>
                                <excludes>
                                    <exclude>org.apache.tomcat.embed:*</exclude>
                                    <exclude>org.springframework.boot:spring-boot-starter-web</exclude>
                                    <exclude>org.mybatis.spring.boot:*</exclude>
                                </excludes>
                            </bannedDependencies>
                        </rules>
                    </configuration>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

---

## 8. 实施计划

### 8.1 分阶段实施

```
Phase 1         Phase 2           Phase 3           Phase 4
(1-2天)         (2-3天)           (1-2天)           (1天)
┌──────────┐   ┌──────────────┐   ┌──────────────┐   ┌──────────┐
│ 创建父POM │ → │ 改造子模块    │ → │ 提取公共模块  │ → │ 验证联调  │
│ 版本统一  │   │ pom.xml      │   │ open-app-    │   │ 全量构建  │
│ dependency│   │ 逐个迁移     │   │ common       │   │ 启动测试  │
│ Management│   │              │   │              │   │ CI 适配   │
└──────────┘   └──────────────┘   └──────────────┘   └──────────┘
```

### 8.2 Phase 1：创建父 POM 并验证升级兼容性（1-2 天）

1. 在根目录创建 `pom.xml`（参考 §5.1），Spring Boot 版本设为 `3.5.14`
2. **验证 3 个 Servlet 服务在 3.5.14 下的兼容性**（按优先级）：
   1. **event-server**（最简单）: 改 parent 为 3.5.14 → `mvn clean test`
   2. **api-server**: 改 parent → `mvn clean test`
   3. **open-server**（含 AOP）: 改 parent → `mvn clean test`
3. connector-api **无需验证**（已在 3.5.14 上稳定运行）
4. 如有兼容性问题，记录并评估修复成本

### 8.3 Phase 2：改造子模块（2-3 天）

**按顺序逐个迁移**（从最简单到最复杂）：

| 顺序 | 服务 | 操作 | 验证方式 |
|:----:|------|------|---------|
| 1 | **event-server** | 改 parent，去掉独立声明的 Spring Boot parent | `mvn clean package -pl event-server` |
| 2 | **open-server** | 同上 | `mvn clean package -pl open-server` |
| 3 | **api-server** | 同上 | `mvn clean package -pl api-server` |
| 4 | **connector-api** | 改 parent + 验证兼容性（版本不变，已在 3.5.14） | `mvn clean package -pl connector-api && ./scripts/start.sh` |

每个服务的 pom.xml 改造步骤：
1. 修改 `<parent>` 指向 `open-app-platform`
2. 删除 `<groupId>` 和 `<version>`（继承父 POM）
3. 删除 `<properties>` 中已在父 POM 管理的版本号
4. 删除 `<dependency>` 中的 `<version>`（由父 POM `dependencyManagement` 管理）
5. 运行 `mvn clean package` 验证

### 8.4 Phase 3：提取公共模块（1-2 天）

1. 创建 `open-app-common` 模块目录和 pom.xml
2. 从 api-server 迁移公共类（`ApiResponse`、`BusinessException`、`SignatureUtil`、`JacksonConfig`）
3. 各服务添加 `open-app-common` 依赖，删除本地的重复实现
4. 处理 `GlobalExceptionHandler` 分离（各服务自行实现适配层）
5. 逐服务编译验证

### 8.5 Phase 4：验证与联调（1 天）

1. **全量构建**：
   ```bash
   cd open-app
   mvn clean package -DskipTests
   # 验证 target/ 下 4 个独立 JAR 均生成
   ls api-server/target/*.jar
   ls connector-api/target/*.jar
   ls open-server/target/*.jar
   ls event-server/target/*.jar
   ```

2. **逐服务启动验证**：
   ```bash
   # 终端 1
   cd api-server && ./scripts/start.sh
   # 终端 2
   cd connector-api && ./scripts/start.sh
   # 终端 3
   cd open-server && ./scripts/start.sh
   # 终端 4
   cd event-server && ./scripts/start.sh
   
   # 健康检查
   curl http://localhost:18081/api-server/actuator/health
   curl http://localhost:18180/actuator/health
   curl http://localhost:18080/open-server/actuator/health
   curl http://localhost:18083/event-server/actuator/health
   ```

3. **选择性构建**（加速日常开发）：
   ```bash
   # 仅构建 api-server
   mvn clean package -pl api-server -am
   # -pl: 指定模块
   # -am: also-make，自动构建依赖模块（open-app-common）
   
   # 仅构建 connector-api
   mvn clean package -pl connector-api -am
   ```

---

## 9. 风险评估

| 风险 | 影响 | 概率 | 缓解措施 |
|------|------|:----:|---------|
| **3 个 Servlet 服务从 3.4.6 升级到 3.5.14 出现兼容性问题** | 服务无法启动或功能异常 | 低-中 | Phase 1 先行逐个服务验证；3.5.14 为内部版本，connector-api 已验证稳定；Spring Boot 3.4→3.5 为小版本升级，API 兼容性高 |
| **公共模块依赖污染 connector-api** | connector-api classpath 混入 Tomcat/Servlet 类 | 低 | open-app-common 不引入 web starter；maven-enforcer-plugin 强制检查 |
| **Maven 坐标变更导致 CI/CD 脚本失效** | 构建流水线中断 | 中 | Phase 4 同步更新 CI 配置；分阶段提交便于回滚 |
| **模块间循环依赖** | 构建失败 | 低 | 明确依赖方向：子模块 → open-app-common（单向）；子模块之间不互相依赖 |
| **团队学习成本** | 短期效率下降 | 低 | 多模块 Maven 是 Java 生态标准实践；提供一键构建脚本和 IDE 导入指南 |
| **启动脚本路径变化** | 运维脚本需调整 | 低 | `scripts/start.sh` 在子模块目录下不变；根目录新增 `start-all.sh` |

---

## 10. 架构决策记录 (ADR)

### ADR-004：Maven 多模块统一构建

- **状态**：提议中
- **决策**：采用 Maven 多模块聚合工程，统一 `spring-boot-starter-parent` 至 **3.5.14**（内部企业版本），各服务保持独立部署
- **备选方案**：
  - 方案 A（当前）：4 个独立 Maven 工程 —— 版本漂移、重复配置
  - 方案 B（推荐）：Maven 多模块 + 父 POM —— 统一版本管理、保留独立部署
  - 方案 C：Gradle 多模块 —— 学习成本高，团队不熟悉
- **后果**：
  - ✅ 统一版本管理，消除版本漂移
  - ✅ 提取公共代码，减少重复
  - ✅ 一键构建，简化 CI/CD
  - ⚠️ api-server / open-server / event-server 需升级并验证 MyBatis / Redis / WebSocket 兼容性
  - ⚠️ 需更新 CI/CD 流水线配置

### ADR-005：公共模块不引入 Web 框架依赖

- **状态**：提议中
- **决策**：`open-app-common` 仅引入 Jackson Annotations + Jakarta Validation API，**不引入任何 Web 框架依赖**（spring-boot-starter-web 或 spring-boot-starter-webflux）
- **理由**：connector-api 使用 WebFlux，api-server/open-server/event-server 使用 Servlet MVC。公共模块引入任何一种 Web 框架都会污染另一种栈的服务
- **后果**：
  - ✅ 所有服务均可安全引用 open-app-common
  - ⚠️ `GlobalExceptionHandler`（依赖 `@RestControllerAdvice`）不能放入 common，各服务自行实现适配层

---

## 11. 修订记录

| 版本 | 日期 | 修订内容 | 修订人 |
|------|------|---------|--------|
| v1.0 | 2026-05-27 | 初始版本 —— 当前状态分析、目标架构设计、分 4 阶段实施计划、2 个 ADR | SDDU |

---

## 附录 A：api-server / open-server / event-server 升级验证命令

```bash
# 1. 对每个 Servlet 服务，临时改 parent 版本验证
cd api-server
# 临时修改 pom.xml 中 spring-boot-starter-parent version 为 3.5.14
mvn clean test

# 2. 重点检查 MyBatis 兼容性
cd open-server
mvn clean test -Dtest="*Mapper*Test"

# 3. 检查 Redis 连接
cd api-server
mvn clean test -Dtest="*Redis*Test"

# 4. 检查 event-server WebSocket
cd event-server
mvn clean test
```
