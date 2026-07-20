# 任务：用户角色查询

> 父 Feature: 嵌入能力API面（EMBED-API-001）  
> 对应任务: TASK-001 | 复杂度: M | FR: FR-001  
> 前置依赖: 无

## 描述

新增用户角色查询接口。嵌入能力方后端通过内部凭证调用，输入应用标识（支持平台 appId 或外部 hisAppId）+ 用户账号，返回角色列表。开发阶段 Mock 实现，联调阶段对接 openplatform_app_member_t。

## 涉及文件

| 操作 | 文件路径 |
|:--:|------|
| NEW | `api-server/.../internal/dto/UserRoleQueryRequest.java` |
| NEW | `api-server/.../internal/dto/UserRoleQueryResponse.java` |
| NEW | `api-server/.../internal/resolver/AppIdentifier.java` |
| NEW | `api-server/.../internal/resolver/AppIdentifierResolver.java` |
| NEW | `api-server/.../internal/auth/InternalTokenAuthFilter.java` |
| NEW | `api-server/.../internal/config/InternalAuthConfig.java` |
| NEW | `api-server/.../internal/service/UserRoleService.java` |
| NEW | `api-server/.../internal/service/impl/UserRoleServiceMockImpl.java` |
| NEW | `api-server/.../internal/service/impl/UserRoleServiceRealImpl.java` |
| NEW | `api-server/.../internal/controller/UserRoleController.java` |
| MODIFY | `api-server/src/main/resources/application.yml` |
| MODIFY | `api-server/src/main/resources/application-dev.yml` |
| MODIFY | `api-server/src/main/resources/application-prod.yml` |
| NEW | `api-server/src/test/java/.../internal/controller/UserRoleControllerTest.java` |
| NEW | `api-server/src/test/java/.../internal/service/UserRoleServiceTest.java` |
| NEW | `api-server/src/test/python/modules/internal/test_user_roles.py` |

## 验收标准

**DTO 与枚举**：
- [ ] `UserRoleQueryRequest`：appId/hisAppId 二选一，userAccount 必填
- [ ] `UserRoleQueryResponse`：appId + roles(Integer[])
- [ ] `AppIdentifier` 枚举：APP_ID / HIS_APP_ID

**应用标识解析**：
- [ ] 优先按 appId 查 app_t，未匹配按 hisAppId 查 app_p_t.eamap_app_code
- [ ] 均未匹配返回 404 "应用不存在"

**内部凭证鉴权**：
- [ ] 拦截 /internal/** 路径，校验 X-Internal-Token
- [ ] 无效或缺失返回 401
- [ ] 支持多服务方独立凭证，开发阶段 bypass 开关

**UserRoleService**：
- [ ] Mock 实现：返回预设模拟数据
- [ ] Real 实现：查询 openplatform_app_member_t，memberType 映射为 roles(0/1/2)
- [ ] @ConditionalOnProperty 切换 Mock/Real
- [ ] 用户无角色返回空列表

**Controller**：
- [ ] POST /service/open/v2/internal/user/roles
- [ ] ApiResponse 信封：200(成功) / 400(参数) / 401(凭证) / 404(应用不存在)

**配置**：
- [ ] application-dev.yml：bypass=true + Mock + 示例凭证
- [ ] application-prod.yml：bypass=false + Real + 占位凭证

**测试**：
- [ ] Java 单测：UserRoleControllerTest + UserRoleServiceTest 通过
- [ ] Python 集成测试：test_user_roles.py L1/L2/L4 通过

## 验证

```bash
# Java 单元测试
mvn -f api-server/pom.xml test -Dtest="UserRoleControllerTest,UserRoleServiceTest"

# Python 集成测试
cd api-server/src/test/python
pytest modules/internal/test_user_roles.py -v
```
