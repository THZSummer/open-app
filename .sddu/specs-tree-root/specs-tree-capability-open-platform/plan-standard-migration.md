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
- open-server 作为模块集成到标准环境 Spring Boot 主工程
- 主工程已有自己的全局组件（异常处理器、拦截器、配置等）
- 需要实现模块隔离，避免冲突

### 1.3 迁移原则
1. **模块隔离**：v2 模块组件不影响主工程和其他模块
2. **配置分离**：开发环境配置只在开发环境生效
3. **路径区分**：使用独立的 URL 前缀标识 v2 模块
4. **命名规范**：全局组件添加 V2 后缀，避免类名冲突

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

### 3.2 🟡 中优先级：配置类冲突评估

#### 3.2.1 JacksonConfig.java

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

**潜在冲突**：
- 主工程可能已有 ObjectMapper Bean
- 可能覆盖主工程的 Jackson 配置

**解决方案**：

**方案一：添加条件注解**（推荐）
```java
@Configuration
@ConditionalOnMissingBean(ObjectMapper.class)
public class JacksonConfig {
    // 只在主工程没有 ObjectMapper 时才创建
}
```

**方案二：限定作用范围**
```java
@Configuration
public class JacksonConfig {
    @Bean("v2ObjectMapper")
    @Primary(false)
    public ObjectMapper v2ObjectMapper(Jackson2ObjectMapperBuilder builder) {
        // 不覆盖主工程的 ObjectMapper
    }
}
```

**方案三：直接删除**
- 如果主工程已有相同配置，可以直接删除此配置类

**需要确认**：主工程是否已有 Jackson 配置？

---

#### 3.2.2 IdGeneratorConfig.java

**当前配置**：
```java
@Configuration
public class IdGeneratorConfig {
    @Bean
    public IdGeneratorStrategy idGenerator() {
        // 根据环境选择 ID 生成策略
    }
}
```

**潜在冲突**：
- 主工程可能已有 ID 生成器 Bean
- Bean 名称可能冲突

**解决方案**：

**方案一：添加条件注解**（推荐）
```java
@Configuration
public class IdGeneratorConfig {
    @Bean
    @ConditionalOnMissingBean(IdGeneratorStrategy.class)
    public IdGeneratorStrategy idGenerator() {
        // 只在主工程没有 ID 生成器时才创建
    }
}
```

**方案二：限定为开发环境**
```java
@Configuration
@Profile({"dev", "development", "local"})
public class IdGeneratorConfig {
    // 只在开发环境生效
}
```

**需要确认**：主工程是否有 ID 生成器？标准环境如何生成 ID？

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