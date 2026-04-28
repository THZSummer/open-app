# 标准环境迁移规划

**文档版本**: v1.0  
**创建日期**: 2026-04-29  
**规划作者**: SDDU Agent  
**目标**: open-server 作为独立模块集成到现有 Spring Boot 工程

---

## 1. 迁移背景

### 1.1 当前状态
- open-server 是一个独立的 Spring Boot 应用
- 包名：`com.xxx.it.works.wecode.v2`
- 接口路径：`/api/v1/**`
- 当前环境：开发环境（dev/development/local）

### 1.2 目标状态

**核心目标**："迁过去就能用"

**具体要求**：
1. ✅ **避免冲突**：v2 模块组件不与主工程冲突
2. ✅ **最小改动**：尽可能少的代码修改
3. ✅ **开箱即用**：无需复杂配置，迁移即可运行

**实现原则**：
- 环境感知：配置类自动适配不同环境
- 条件装配：使用 Spring 条件注解避免 Bean 冲突
- 作用域隔离：通过包名、路径限定组件作用范围
- 向后兼容：不破坏现有代码结构

### 1.3 迁移原则

**核心原则："迁过去就能用"**

1. **最小改动**：优先使用配置和注解解决冲突，而非修改代码结构
   - ✅ 使用 `@ConditionalOnMissingBean` 自动适配
   - ✅ 使用 `@Profile` 环境感知
   - ✅ 使用 `basePackages` 作用域隔离
   - ❌ 不删除现有代码
   - ❌ 不重构代码结构

2. **自动适配**：代码能感知运行环境，自动选择合适的配置
   - 开发环境：使用 v2 的配置
   - 标准环境：自动使用主工程配置

3. **隔离优先**：通过 Spring 机制实现模块隔离
   - 异常处理隔离：`basePackages`
   - 拦截器隔离：`order` + `pathPatterns`
   - Bean 隔离：`@ConditionalOnMissingBean`

4. **零侵入**：对主工程零侵入
   - 不修改主工程代码
   - 不修改主工程配置
   - 不影响主工程其他模块

---

## 2. 需要迁移的组件清单

### 2.1 已完成迁移的组件 ✅

| 组件 | 文件 | 当前状态 | 迁移方案 | 状态 |
|------|------|---------|---------|------|
| 全局异常处理器 | GlobalExceptionHandlerV2.java | 已处理 | `basePackages` 限定 + V2 后缀 | ✅ 完成 |

**详细说明**：
```java
@RestControllerAdvice(basePackages = "com.xxx.it.works.wecode.v2")
public class GlobalExceptionHandlerV2 {
    // 只处理 com.xxx.it.works.wecode.v2 包下的异常
}
```

### 2.2 配置类（已适配）✅

| 配置类 | 文件 | 环境限定 | 说明 | 状态 |
|--------|------|---------|------|------|
| MyBatis 配置 | DevMyBatisConfig.java | `@Profile("dev")` | 只在开发环境生效 | ✅ 已适配 |
| Redis 配置 | DevRedisConfig.java | `@Profile("dev")` | 只在开发环境生效 | ✅ 已适配 |
| Jackson 配置 | JacksonConfig.java | 全局 | ⚠️ 需要评估是否冲突 | ⏳ 待评估 |
| ID 生成器配置 | IdGeneratorConfig.java | 全局 | ⚠️ 可能与主工程冲突 | ⏳ 待评估 |

### 2.3 拦截器（已设计优先级）✅

| 组件 | 文件 | 当前配置 | 说明 | 状态 |
|------|------|---------|------|------|
| 用户解析拦截器 | UserResolveInterceptor.java | order=10 | 在主工程认证拦截器之后执行 | ✅ 已设计 |
| WebMVC 配置 | WebMvcConfig.java | 路径: /api/** | ⚠️ 路径需要修改 | ⏳ 待修改 |

---

## 3. 需要修改的内容

### 3.0 接口路径修改评估

**核心原则**："最小改动，迁过去就能用"

#### 方案对比

| 方案 | 改动量 | 冲突风险 | 使用建议 |
|------|--------|---------|---------|
| **方案一：保持原路径，前端网关隔离** | ✅ 无改动 | 中等 | 推荐用于快速集成 |
| **方案二：修改为统一路径** | ❌ 较大改动 | 低 | 推荐用于长期维护 |

#### 推荐方案：保持原路径，前端网关隔离

**实现方式**：
- 不修改接口路径
- 通过前端网关（Nginx/Gateway）做路径映射
- 主工程通过网关区分不同模块

**优点**：
- ✅ **零代码改动**
- ✅ **迁移即可使用**
- ✅ 不影响前端已有的 API 调用
- ✅ 快速集成

**配置示例**：
```nginx
# Nginx 配置
location /service/open/v2/ {
    proxy_pass http://open-server/api/v1/;
}
```

**或者 Spring Cloud Gateway**：
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: open-server-v2
          uri: lb://open-server
          predicates:
            - Path=/service/open/v2/**
          filters:
            - RewritePath=/service/open/v2/(?<segment>.*), /api/v1/$\{segment}
```

#### 备选方案：修改接口路径

**仅在以下情况使用**：
- 主工程没有网关层
- 接口路径必须统一规范
- 长期维护需求

**详细方案**：（见下节）

---

### 3.1 🔴 高优先级：接口路径统一修改

**当前问题**：
- 所有接口路径为 `/api/v1/**`
- 与主工程的 API 路径可能冲突
- 路径中没有明确的版本和模块标识

**修改方案**：

#### 方案一：改为 /service/open/v2/** （推荐）
```
/service/open/v2/apis
/service/open/v2/events
/service/open/v2/callbacks
/service/open/v2/categories
/service/open/v2/approval/...
/service/open/v2/sync
/service/open/v2/health
```

**优点**：
- ✅ 明确的服务标识：`service/open`
- ✅ 明确的版本标识：`v2`
- ✅ 与主工程 API 路径完全隔离
- ✅ 符合 RESTful 规范

**需要修改的文件**（共 8 个）：

| 文件 | 原路径 | 新路径 |
|------|--------|--------|
| ApiController.java | `/api/v1/apis` | `/service/open/v2/apis` |
| EventController.java | `/api/v1/events` | `/service/open/v2/events` |
| CallbackController.java | `/api/v1/callbacks` | `/service/open/v2/callbacks` |
| CategoryController.java | `/api/v1/categories` | `/service/open/v2/categories` |
| ApprovalController.java | `/api/v1` | `/service/open/v2` |
| SyncController.java | `/api/v1/sync` | `/service/open/v2/sync` |
| HealthController.java | `/api/v1` | `/service/open/v2` |
| WebMvcConfig.java | `/api/**` | `/service/open/v2/**` |

---

### 3.2 配置类自动适配（推荐方案）

**核心思路**：使用 Spring 条件注解，"迁过去就能用"

#### 3.2.1 JacksonConfig.java - 自动适配

**当前配置**：
```java
@Configuration
public class JacksonConfig {
    @Bean
    public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
        // Long 序列化为 String
        // Java 8 时间支持
        // 时区设置为 Asia/Shanghai
    }
}
```

**推荐方案：添加条件注解（最小改动）** ✅

```java
@Configuration
public class JacksonConfig {
    
    @Bean
    @ConditionalOnMissingBean(ObjectMapper.class)
    // ✅ 主工程没有 ObjectMapper 时才创建
    // ✅ 主工程有 ObjectMapper 时自动使用主工程的
    // ✅ 零侵入，自动适配
    public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
        ObjectMapper objectMapper = builder.createXmlMapper(false).build();
        
        // Java 8 时间模块
        objectMapper.registerModule(new JavaTimeModule());
        
        // 禁用日期序列化为时间戳
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
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

**优点**：
- ✅ 只需添加一行注解 `@ConditionalOnMissingBean`
- ✅ 迁过去就能用，无需删除代码
- ✅ 主工程有配置时自动使用主工程的
- ✅ 主工程无配置时自动创建

#### 3.2.2 IdGeneratorConfig.java - 自动适配

**推荐方案：添加条件注解（最小改动）** ✅

```java
@Configuration
public class IdGeneratorConfig {

    private final List<IdGeneratorStrategy> strategies;

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    @Bean
    @ConditionalOnMissingBean(IdGeneratorStrategy.class)
    // ✅ 主工程没有 ID 生成器时才创建
    // ✅ 主工程有 ID 生成器时自动使用主工程的
    // ✅ 零侵入，自动适配
    public IdGeneratorStrategy idGenerator() {
        IdGeneratorStrategy strategy = strategies.stream()
                .filter(s -> s.supports(activeProfile))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        String.format(Locale.ROOT, "No ID generator strategy found for current environment [%s]", activeProfile)));
        
        log.info("ID generator strategy loaded: {}, active profile: {}", 
                strategy.getClass().getSimpleName(), activeProfile);
        
        return strategy;
    }
}
```

**优点**：
- ✅ 只需添加一行注解 `@ConditionalOnMissingBean`
- ✅ 迁过去就能用，无需删除代码
- ✅ 自动适配不同环境

---

### 3.3 🟢 低优先级：其他优化

#### 3.3.1 全局异常处理器包名

**当前配置**：
```java
@RestControllerAdvice(basePackages = "com.xxx.it.works.wecode.v2")
```

**验证点**：
- ✅ 已限定包名，不会影响其他模块
- ✅ 类名已添加 V2 后缀

**无需修改**

---

## 4. 迁移执行计划

### 核心思路

**遵循"迁过去就能用"原则**：

```
开发环境（独立运行）
    ↓ 直接迁移
标准环境（集成到主工程）
    ↓ 自动适配
✅ 正常运行
```

**最小改动方案**：

| 组件 | 改动内容 | 改动量 |
|------|---------|--------|
| 异常处理器 | 已完成（basePackages 限定） | ✅ 无需改动 |
| Jackson 配置 | 添加 `@ConditionalOnMissingBean` | ✅ 1 行 |
| ID 生成器配置 | 添加 `@ConditionalOnMissingBean` | ✅ 1 行 |
| 接口路径 | 保持不变，网关层隔离 | ✅ 0 行 |
| **总计** | | **2 行代码** |

**迁移步骤**：
1. ✅ 添加 2 行条件注解
2. ✅ 迁移代码到主工程
3. ✅ 启动测试
4. ✅ 完成

---

### 4.1 准备阶段

| 序号 | 任务 | 负责人 | 状态 |
|------|------|--------|------|
| 1 | 确认主工程是否已有 Jackson 配置 | 架构组 | ⏳ 待确认 |
| 2 | 确认主工程是否已有 ID 生成器 | 架构组 | ⏳ 待确认 |
| 3 | 确认标准环境的接口路径规范 | 架构组 | ⏳ 待确认 |
| 4 | 确认主工程的包名和模块结构 | 架构组 | ⏳ 待确认 |

---

### 4.2 执行阶段

#### 阶段一：接口路径修改（必须执行）

**执行步骤**：
1. 修改所有 Controller 的 `@RequestMapping` 路径
2. 修改 WebMvcConfig 的拦截器路径
3. 更新 API 文档
4. 更新前端调用路径
5. 运行单元测试验证

**回滚方案**：
- 创建新分支 `feature/change-api-path`
- 如果出现问题，回退到原分支

**影响范围**：
- 前端需要同步修改 API 调用路径
- API 文档需要更新
- 可能影响已有的集成测试

---

#### 阶段二：配置类适配（根据确认结果执行）

**场景一：主工程已有相同配置**

| 配置类 | 处理方式 |
|--------|---------|
| JacksonConfig.java | 删除或添加 `@ConditionalOnMissingBean` |
| IdGeneratorConfig.java | 删除或添加 `@ConditionalOnMissingBean` |

**场景二：主工程没有相关配置**

| 配置类 | 处理方式 |
|--------|---------|
| JacksonConfig.java | 保持不变 |
| IdGeneratorConfig.java | 添加 `@Profile` 限定开发环境 |

---

### 4.3 验证阶段

| 验证项 | 验证方法 | 预期结果 |
|--------|---------|---------|
| 异常处理隔离 | 在 v2 模块抛出异常 | 由 GlobalExceptionHandlerV2 处理 |
| 拦截器隔离 | 访问 /service/open/v2/** | UserResolveInterceptor 生效 |
| 配置不冲突 | 启动主工程 | 无 Bean 冲突错误 |
| 接口可访问 | 调用 v2 接口 | 正常返回数据 |

---

## 5. 风险评估

### 5.1 高风险项 🔴

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| 接口路径修改影响前端 | 前端调用失败 | 1. 与前端同步修改<br/>2. 提供过渡期兼容方案<br/>3. 充分测试 |
| 配置类冲突 | 启动失败或功能异常 | 1. 使用条件注解<br/>2. 充分测试集成环境 |

### 5.2 中风险项 🟡

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| 拦截器优先级问题 | 用户认证异常 | 已设计 order=10，需在标准环境验证 |
| 异常处理范围 | 异常被错误处理器捕获 | 已限定 basePackages，需验证 |

### 5.3 低风险项 🟢

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| 类名冲突 | Bean 名称冲突 | 已添加 V2 后缀 |
| 包名冲突 | 扫描范围冲突 | 已使用统一的包名前缀 |

---

## 6. 需要确认的问题

### 🔴 必须确认

1. **接口路径规范**
   - ❓ 主工程的 API 路径规范是什么？
   - ❓ 是否接受 `/service/open/v2/**` 路径？
   - ❓ 是否需要其他前缀（如 `/api/v2/**`）？

2. **Jackson 配置**
   - ❓ 主工程是否已有 ObjectMapper Bean？
   - ❓ 主工程的 Jackson 配置是什么样的？
   - ❓ 是否需要 Long 序列化为 String？

3. **ID 生成器**
   - ❓ 主工程是否已有 ID 生成器？
   - ❓ 标准环境如何生成 ID？
   - ❓ 雪花算法配置（workerId、datacenterId）？

### 🟡 建议确认

4. **拦截器优先级**
   - ❓ 主工程的认证拦截器优先级是多少？
   - ❓ UserResolveInterceptor 的 order=10 是否合适？

5. **包扫描范围**
   - ❓ 主工程的包扫描范围是什么？
   - ❓ 是否会扫描到 `com.xxx.it.works.wecode.v2`？

6. **Bean 命名规范**
   - ❓ 主工程的 Bean 命名规范是什么？
   - ❓ 是否需要统一的命名前缀？

---

## 7. 回滚计划

### 7.1 代码回滚

**场景**：修改后发现严重问题

**步骤**：
```bash
# 回退到迁移前的分支
git checkout main

# 删除迁移分支
git branch -D feature/migration-to-standard

# 重新创建迁移分支
git checkout -b feature/migration-to-standard-v2
```

### 7.2 配置回滚

**场景**：配置类导致启动失败

**步骤**：
1. 删除有问题的配置类
2. 使用主工程的配置
3. 重新启动应用

---

## 8. 时间计划

| 阶段 | 任务 | 预计时间 | 负责人 |
|------|------|---------|--------|
| 准备 | 确认主工程配置 | 1 天 | 架构组 |
| 执行 | 修改接口路径 | 0.5 天 | 开发组 |
| 执行 | 适配配置类 | 0.5 天 | 开发组 |
| 验证 | 集成测试 | 1 天 | 测试组 |
| 文档 | 更新 API 文档 | 0.5 天 | 开发组 |
| **总计** | | **3.5 天** | |

---

## 9. 附录

### 9.1 当前文件结构

```
open-server/
├── src/main/java/com/xxx/it/works/wecode/v2/
│   ├── common/
│   │   ├── config/
│   │   │   ├── DevMyBatisConfig.java
│   │   │   ├── DevRedisConfig.java
│   │   │   ├── IdGeneratorConfig.java
│   │   │   ├── JacksonConfig.java
│   │   │   └── WebMvcConfig.java
│   │   ├── exception/
│   │   │   └── GlobalExceptionHandlerV2.java
│   │   ├── interceptor/
│   │   │   └── UserResolveInterceptor.java
│   │   └── ...
│   └── modules/
│       ├── api/controller/ApiController.java
│       ├── event/controller/EventController.java
│       ├── callback/controller/CallbackController.java
│       ├── category/controller/CategoryController.java
│       ├── approval/controller/ApprovalController.java
│       ├── sync/controller/SyncController.java
│       └── ...
└── ...
```

### 9.2 接口路径映射表

| 模块 | 当前路径 | 建议路径 | Controller |
|------|---------|---------|-----------|
| API 管理 | /api/v1/apis/** | /service/open/v2/apis/** | ApiController |
| 事件管理 | /api/v1/events/** | /service/open/v2/events/** | EventController |
| 回调管理 | /api/v1/callbacks/** | /service/open/v2/callbacks/** | CallbackController |
| 分类管理 | /api/v1/categories/** | /service/open/v2/categories/** | CategoryController |
| 审批管理 | /api/v1/approval/** | /service/open/v2/approval/** | ApprovalController |
| 数据同步 | /api/v1/sync/** | /service/open/v2/sync/** | SyncController |
| 健康检查 | /api/v1/health | /service/open/v2/health | HealthController |

---

## 10. 确认签字

| 角色 | 确认内容 | 签字 | 日期 |
|------|---------|------|------|
| 架构师 | 迁移方案可行 | | |
| 开发负责人 | 开发工作量评估 | | |
| 测试负责人 | 测试计划确认 | | |
| 产品负责人 | 接口路径变更确认 | | |

---

**文档状态**: ⏳ 待确认

**下一步**: 等待架构组确认主工程配置后，开始执行迁移