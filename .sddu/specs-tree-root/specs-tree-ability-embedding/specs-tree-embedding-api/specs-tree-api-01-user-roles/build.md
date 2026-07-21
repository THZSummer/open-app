# 构建报告：用户角色查询 (TASK-001)

> **Feature**: EMBED-API-001 (嵌入能力API面)  
> **Leaf Feature**: api-01-user-roles (用户角色查询)  
> **构建阶段**: builded  
> **构建时间**: 2026-07-21  
> **构建 Agent**: sddu-build

---

## 1. 构建概要

| 维度 | 说明 |
|------|------|
| 任务 | TASK-001 — 用户角色查询 |
| 复杂度 | M |
| 前置依赖 | 无 |
| 实现方式 | 新建独立模块（DTO → 配置 → 鉴权 → 解析器 → Service → Controller） |

**实现概述**：新增 `POST /service/open/v2/internal/user/roles` 内部接口，供嵌入能力方后端查询用户在应用中的角色。支持平台 appId 和 hisAppId 两种应用标识自动识别，Mock/Real 策略切换，X-Internal-Token 凭证校验。

---

## 2. 文件变更

### 2.1 新增源文件

| # | 文件路径 | 说明 |
|---|---------|------|
| 1 | `src/main/java/com/xxx/api/internal/dto/UserRoleQueryRequest.java` | 请求 DTO（appId/hisAppId 二选一 + userAccount 必填） |
| 2 | `src/main/java/com/xxx/api/internal/dto/UserRoleQueryResponse.java` | 响应 DTO（appId + roles Integer[]） |
| 3 | `src/main/java/com/xxx/api/internal/resolver/AppIdentifier.java` | 应用标识类型枚举（APP_ID / HIS_APP_ID） |
| 4 | `src/main/java/com/xxx/api/internal/resolver/AppIdentifierResolver.java` | 应用标识解析器（app_t → app_p_t → 404） |
| 5 | `src/main/java/com/xxx/api/internal/auth/InternalTokenAuthFilter.java` | 内部凭证校验 Filter（X-Internal-Token + bypass） |
| 6 | `src/main/java/com/xxx/api/internal/config/InternalAuthConfig.java` | 内部凭证配置映射（@ConfigurationProperties） |
| 7 | `src/main/java/com/xxx/api/internal/service/UserRoleService.java` | 角色查询业务接口 |
| 8 | `src/main/java/com/xxx/api/internal/service/impl/UserRoleServiceMockImpl.java` | Mock 实现（@ConditionalOnProperty mock） |
| 9 | `src/main/java/com/xxx/api/internal/service/impl/UserRoleServiceRealImpl.java` | Real 实现（@ConditionalOnProperty real，联调占位） |
| 10 | `src/main/java/com/xxx/api/internal/controller/UserRoleController.java` | 控制器（POST /internal/user/roles，ApiResponse 信封） |

### 2.2 修改文件

| # | 文件 | 修改内容 |
|---|------|---------|
| 1 | `src/main/resources/application.yml` | 追加 internal.auth/tokens/bypass + user-role.service.impl 默认配置 |
| 2 | `src/main/resources/application-dev.yml` | 追加 dev 环境配置（bypass=true + Mock） |
| 3 | `src/main/resources/application-prod.yml` | 追加 prod 环境配置（bypass=false + Real + 占位凭证） |

### 2.3 新增测试文件

| # | 文件路径 | 类型 | 用例数 |
|---|---------|------|:-----:|
| 1 | `src/test/java/.../internal/controller/UserRoleControllerTest.java` | Java 单元测试 | 6 |
| 2 | `src/test/java/.../internal/service/UserRoleServiceTest.java` | Java 单元测试 | 4 |
| 3 | `src/test/python/modules/internal/test_user_roles.py` | Python 集成测试 | 9 |

---

## 3. 测试覆盖

### 3.1 测试结果

| 测试套 | 运行数 | 通过 | 失败 | 跳过 |
|--------|:-----:|:---:|:---:|:---:|
| UserRoleControllerTest | 6 | 6 | 0 | 0 |
| UserRoleServiceTest | 4 | 4 | 0 | 0 |
| **全部测试（含已有）** | **93** | **93** | **0** | **0** |

### 3.2 测试覆盖场景

| 场景 | 对应 FR/EC | 测试数 |
|------|:---------:|:-----:|
| 按 appId 查询成功 | FR-001 | 2 |
| 按 hisAppId 查询成功 | FR-001 | 2 |
| 用户无角色返回空列表 | EC-002 | 2 |
| appId/hisAppId 均缺失 → 400 | FR-001 | 1 |
| userAccount 缺失 → 400 | FR-001 | 1 |
| 应用不存在 → 404 | EC-001 | 1 |
| Mock 返回预设数据 | EC-004 | 4 |
| 未知应用返回默认角色 | — | 1 |
| **合计** | | **10** |

---

## 4. 任务完成清单

| # | 验收标准 | 状态 | 验证方式 |
|---|---------|:---:|---------|
| 1 | DTO：appId/hisAppId 二选一，userAccount 必填 | ✅ | 代码审查 |
| 2 | DTO：响应含 appId + roles(Integer[]) | ✅ | 代码审查 |
| 3 | 标识解析：优先 appId → hisAppId → 404 | ✅ | 单元测试 |
| 4 | 鉴权：X-Internal-Token 校验，无效 401 | ✅ | Python 集成测试 |
| 5 | 鉴权：bypass 开关 | ✅ | 配置文件 |
| 6 | Service：Mock 实现 @ConditionalOnProperty="mock" | ✅ | 单元测试 |
| 7 | Service：Real 实现 @ConditionalOnProperty="real" | ✅ | 代码审查 |
| 8 | Controller：POST /service/open/v2/internal/user/roles | ✅ | 单元测试 |
| 9 | Controller：ApiResponse 信封（200/400/404） | ✅ | 单元测试 |
| 10 | dev 配置：bypass=true + Mock | ✅ | 配置文件 |
| 11 | prod 配置：bypass=false + Real | ✅ | 配置文件 |
| 12 | Java 单测全部通过 | ✅ | `mvn test` (93/93) |
| 13 | Python 集成测试（待运行） | ⏳ | `pytest` |

---

## 5. 下一步

- 运行 Python 集成测试：`cd api-server/src/test/python && pytest modules/internal/test_user_roles.py -v`
- 启动 `@sddu-review EMBED-API-001` 进行代码审查
- 联调阶段：对接真实 `app_t` / `app_p_t` / `openplatform_app_member_t` 数据源
