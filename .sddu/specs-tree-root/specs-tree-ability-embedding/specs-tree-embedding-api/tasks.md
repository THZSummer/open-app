# 任务分解：嵌入能力API面

> **文档定位**: SDDU 任务清单 - 将技术方案分解为可并行执行的原子任务，作为 build 阶段的输入  
> **前置依赖**: plan.md（技术方案）、spec.md（需求规范）  
> **创建人**: SDDU Tasks Agent  
> **创建时间**: 2026-07-13  
> **版本**: v1.0  
> **更新人**: SDDU Tasks Agent  
> **更新时间**: 2026-07-13  
> **更新说明**: 初始创建

## 1. 依赖拓扑总览
> 任务依赖关系和执行顺序

```
Wave 1 ─── (无依赖，全部并行)
  TASK-001 [S]  DTO/枚举类定义
  TASK-003 [M]  内部凭证鉴权过滤器
  TASK-006 [S]  application.yml 配置项

Wave 2 ─── (依赖 Wave 1)
  TASK-002 [M]  应用标识解析器

Wave 3 ─── (依赖 Wave 2)
  TASK-004 [M]  UserRoleService 接口与实现（Mock/Real）

Wave 4 ─── (依赖 Wave 1 + Wave 3)
  TASK-005 [M]  UserRoleController
```

## 2. 任务列表
> 每个任务的详细定义

### TASK-001: DTO 与枚举类定义
> 请求/响应数据结构与标识枚举

| 属性 | 值 |
|------|-----|
| **复杂度** | S |
| **前置依赖** | 无 |
| **执行波次** | 1 |
| **对应 FR** | FR-001 |

**描述**: 新增用户角色查询的请求 DTO、响应 DTO，以及应用标识类型枚举（区分平台 appId / 外部 hisAppId）。

**涉及文件**:

| 操作 | 文件路径 |
|:--:|------|
| NEW | `api-server/src/main/java/com/xxx/api/internal/dto/UserRoleQueryRequest.java` |
| NEW | `api-server/src/main/java/com/xxx/api/internal/dto/UserRoleQueryResponse.java` |
| NEW | `api-server/src/main/java/com/xxx/api/internal/resolver/AppIdentifier.java` |

**验收标准**:
- [ ] `UserRoleQueryRequest` 含 appId(String,可选)、hisAppId(String,可选)、userAccount(String,必填)，appId/hisAppId 二选一校验注解
- [ ] `UserRoleQueryResponse` 含 appId(String)、roles(Integer[])
- [ ] `AppIdentifier` 枚举含 `APP_ID`、`HIS_APP_ID` 两种类型
- [ ] Maven 编译通过

**验证命令**:
```bash
mvn -f api-server/pom.xml compile -q
```

### TASK-002: 应用标识解析器
> AppIdentifierResolver 实现

| 属性 | 值 |
|------|-----|
| **复杂度** | M |
| **前置依赖** | TASK-001 |
| **执行波次** | 2 |
| **对应 FR** | FR-001 |

**描述**: 新增 `AppIdentifierResolver`，实现应用标识解析逻辑：优先按 appId 查 `app_t`，未匹配则按 hisAppId 查 `app_p_t.eamap_app_code`，均未匹配返回 404。

**涉及文件**:

| 操作 | 文件路径 |
|:--:|------|
| NEW | `api-server/src/main/java/com/xxx/api/internal/resolver/AppIdentifierResolver.java` |

**验收标准**:
- [ ] 优先按 `appId` 查询 `app_t.app_id`
- [ ] appId 未匹配或为空时，按 `hisAppId` 查询 `app_p_t.eamap_app_code`
- [ ] 两者均未匹配时抛出"应用不存在"异常（对应 404）
- [ ] 返回解析后的内部 appId
- [ ] Maven 编译通过

**验证命令**:
```bash
mvn -f api-server/pom.xml compile -q
```

### TASK-003: 内部凭证鉴权过滤器
> InternalTokenAuthFilter + 配置类

| 属性 | 值 |
|------|-----|
| **复杂度** | M |
| **前置依赖** | 无 |
| **执行波次** | 1 |
| **对应 FR** | FR-001、NFR-001、NFR-002 |

**描述**: 新增 `InternalTokenAuthFilter`（或 HandlerInterceptor）拦截 `/internal/**` 路径，校验 `X-Internal-Token` 请求头；新增 `InternalAuthConfig` 配置类支持多服务方独立凭证配置；开发阶段支持 bypass 开关。

**涉及文件**:

| 操作 | 文件路径 |
|:--:|------|
| NEW | `api-server/src/main/java/com/xxx/api/internal/auth/InternalTokenAuthFilter.java` |
| NEW | `api-server/src/main/java/com/xxx/api/internal/config/InternalAuthConfig.java` |

**验收标准**:
- [ ] 拦截 `/service/open/v2/internal/**` 路径
- [ ] 校验 `X-Internal-Token` 请求头，无效或缺失时返回 401
- [ ] 支持多服务方独立凭证配置（Map<serviceName, token>）
- [ ] 开发阶段支持 bypass 开关（配置项控制）
- [ ] Maven 编译通过

**验证命令**:
```bash
mvn -f api-server/pom.xml compile -q
```

### TASK-004: UserRoleService 接口与实现
> 角色查询业务逻辑（Mock/Real 策略切换）

| 属性 | 值 |
|------|-----|
| **复杂度** | M |
| **前置依赖** | TASK-002 |
| **执行波次** | 3 |
| **对应 FR** | FR-001 |

**描述**: 新增 `UserRoleService` 接口及 `UserRoleServiceMockImpl`（开发阶段内置模拟数据）、`UserRoleServiceRealImpl`（联调阶段查询 `openplatform_app_member_t` 或调用 open-server），通过配置开关切换。

**涉及文件**:

| 操作 | 文件路径 |
|:--:|------|
| NEW | `api-server/src/main/java/com/xxx/api/internal/service/UserRoleService.java` |
| NEW | `api-server/src/main/java/com/xxx/api/internal/service/impl/UserRoleServiceMockImpl.java` |
| NEW | `api-server/src/main/java/com/xxx/api/internal/service/impl/UserRoleServiceRealImpl.java` |

**验收标准**:
- [ ] `UserRoleService` 接口定义 `queryUserRoles(UserRoleQueryRequest)` 方法
- [ ] `UserRoleServiceMockImpl`：返回预设固定结构模拟数据（不依赖外部系统）
- [ ] `UserRoleServiceRealImpl`：按 appId + accountId 查询成员角色，memberType 映射为 roles（0/1/2 对应 MemberTypeEnum）
- [ ] 通过 `@ConditionalOnProperty` 或配置开关切换 Mock/Real 实现
- [ ] 用户无角色时返回空列表
- [ ] Maven 编译通过

**验证命令**:
```bash
mvn -f api-server/pom.xml compile -q
```

### TASK-005: UserRoleController
> 用户角色查询接口控制器

| 属性 | 值 |
|------|-----|
| **复杂度** | M |
| **前置依赖** | TASK-003、TASK-004 |
| **执行波次** | 4 |
| **对应 FR** | FR-001 |

**描述**: 新增 `UserRoleController`，暴露 `POST /service/open/v2/internal/user/roles` 接口，调用 UserRoleService 完成角色查询，复用 api-server 现有 `ApiResponse` 信封。

**涉及文件**:

| 操作 | 文件路径 |
|:--:|------|
| NEW | `api-server/src/main/java/com/xxx/api/internal/controller/UserRoleController.java` |

**验收标准**:
- [ ] `POST /internal/user/roles` 接口，接收 `UserRoleQueryRequest`（含 X-Internal-Token 头）
- [ ] 返回 `ApiResponse<UserRoleQueryResponse>`，含 appId + roles(Integer[])
- [ ] 错误码正确：400(参数)、401(凭证无效)、404(应用不存在)
- [ ] 凭证校验由 InternalTokenAuthFilter 拦截（Controller 不重复校验）
- [ ] Maven 编译通过

**验证命令**:
```bash
mvn -f api-server/pom.xml compile -q
```

### TASK-006: application.yml 配置项
> 内部凭证与策略开关配置

| 属性 | 值 |
|------|-----|
| **复杂度** | S |
| **前置依赖** | 无 |
| **执行波次** | 1 |
| **对应 FR** | FR-001、NFR-002、NFR-004 |

**描述**: 在 `application.yml`（及 dev/prod profile）中新增内部凭证配置、Mock/Real 策略开关、bypass 开关。

**涉及文件**:

| 操作 | 文件路径 |
|:--:|------|
| MODIFY | `api-server/src/main/resources/application.yml` |
| MODIFY | `api-server/src/main/resources/application-dev.yml` |
| MODIFY | `api-server/src/main/resources/application-prod.yml` |

**验收标准**:
- [ ] `application.yml` 新增 `internal.token.bypass`(bool)、`internal.token.services`(Map) 配置项
- [ ] `application-dev.yml` 配置 bypass=true + Mock 策略 + 示例凭证
- [ ] `application-prod.yml` 配置 bypass=false + Real 策略 + 占位凭证
- [ ] 新增 `user-role.strategy`(mock/real) 切换配置项
- [ ] Maven 编译通过

**验证命令**:
```bash
mvn -f api-server/pom.xml compile -q
```

## 3. 任务汇总
> 任务数量、复杂度和波次的统计总览

| 统计项 | 数值 |
|--------|:--:|
| 总任务数 | 6 |
| S 级 (简单) | 2 |
| M 级 (中等) | 4 |
| L 级 (复杂) | 0 |
| 执行波次 | 4 |

## 4. 执行策略
> 各波次的执行说明

| 波次 | 任务 | 策略 |
|:--:|------|------|
| 1 | TASK-001, TASK-003, TASK-006 | 并行执行（数据结构、鉴权、配置互不依赖） |
| 2 | TASK-002 | 逐个执行（依赖 TASK-001 的 DTO/枚举） |
| 3 | TASK-004 | 逐个执行（依赖 TASK-002 的解析器） |
| 4 | TASK-005 | 逐个执行（依赖 TASK-003 鉴权 + TASK-004 业务） |

## 修订记录
> 记录本文档的版本变更历史

| 版本 | 变更说明 | 日期 | 修订人 |
|------|---------|------|--------|
| v1.0 | 初始创建 - API面任务分解（6 任务 / 4 波次） | 2026-07-13 | SDDU Tasks Agent |
