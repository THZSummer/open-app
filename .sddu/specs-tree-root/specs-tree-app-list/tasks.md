# Tasks: 开放平台应用管理 (APP-MGMT-001)

> **状态**: tasked
> **生成依据**: plan.md (§ 3-§ 4) + frontend-design.md (§ 1-§ 15)
> **任务总数**: 16（后端 9 + 前端 7）
> **执行波次**: 5
> **下一步**: 运行 `@sddu-build TASK-001` 开始

---

## 📊 任务汇总

| 类别 | 编号 | 数量 |
|------|------|:----:|
| 后端 | TASK-001 ~ TASK-008, TASK-016 | 9 |
| 前端 | TASK-009 ~ TASK-015 | 7 |
| **总计** | — | **16** |

| 复杂度 | 数量 |
|--------|:----:|
| S（<50 行，单文件）| 0 |
| M（<200 行，多文件）| 5 |
| L（>200 行，复杂依赖）| 10 |

| 波次 | 任务 |
|------|------|
| Wave 1（无依赖，可并行）| TASK-001, TASK-009 |
| Wave 2（依赖 Wave 1）| TASK-002, TASK-006, TASK-010 |
| Wave 3（依赖 Wave 2）| TASK-003, TASK-004, TASK-005, TASK-007, TASK-011, TASK-012 |
| Wave 4（依赖 Wave 3）| TASK-013, TASK-014, TASK-016 |
| Wave 5（依赖 Wave 4）| TASK-008, TASK-015 |

---

## 🌊 Wave 1:基础设施（无依赖，可并行）

### TASK-001:数据库 DDL 迁移脚本

**复杂度**: L
**前置依赖**: 无
**执行波次**: 1

#### 描述
按 plan.md § 3.1.1-3.1.9 创建 9 张表的 Flyway 迁移脚本（`open-server/src/main/resources/db/migration/V2__init_app_management_schema.sql`），包括：
- `openplatform_app_t`（应用表）
- `openplatform_app_p_t`（应用属性表）
- `openplatform_app_member_t`（成员表）
- `openplatform_ability_t`（能力主表）
- `openplatform_ability_p_t`（能力属性表）
- `openplatform_app_ability_relation_t`（应用能力关联表）
- `openplatform_app_identity_t`（凭证表）
- `openplatform_app_version_t`（版本表）
- `openplatform_app_version_p_t`（版本属性表）

每张表含字段、类型、长度、是否必填、默认值、注释（COMMENT 字段标注枚举值含义）。

#### 涉及文件
- [NEW] `open-server/src/main/resources/db/migration/V2__init_app_management_schema.sql`
- [MODIFY] `open-server/src/main/resources/application.yml`（配置 Flyway 扫描路径）

#### 验收标准
- [ ] 9 张表 DDL 全部创建
- [ ] 字段类型/长度/注释与 plan.md § 3.1 完全一致
- [ ] 包含必要的索引（如 `idx_app_owner`、`idx_member_appid_accountid`）
- [ ] 外键约束正确（如 `app_id` 引用 `openplatform_app_t`）
- [ ] 启动应用后 Flyway 迁移成功，9 张表存在于数据库

#### 验证命令
```bash
cd open-server && mvn flyway:migrate
mysql -u root -p wecode_v2 -e "SHOW TABLES LIKE 'openplatform_%';" | wc -l  # 期望 ≥ 9
```

---

### TASK-009:wecodesite 公共配置追加（27 个 API 路径常量）

**复杂度**: M
**前置依赖**: 无
**执行波次**: 1

#### 描述
**wecodesite 项目已存在**（含 pages/、components/、configs/、utils/），不要新建目录。直接在 3 个已存在文件中**追加** 27 个 API 路径常量和枚举映射：
- `configs/web.config.js`（已存在）— 追加 `APP` / `APP_MEMBERS` / `APP_ABILITIES` / `APP_VERSIONS` 4 组共 27 个 API 路径常量
- `utils/constants.js`（已存在）— 追加 `APP_TYPE_MAP` / `VERIFY_TYPE_MAP`（5 种含 APIG）/ `ROLE_MAP` / `VERSION_STATUS_MAP` / `FORM_VALIDATION_RULES` 等
- `utils/common.js`（已存在）— 追加公共工具函数

#### 涉及文件
- [MODIFY] `wecodesite/src/configs/web.config.js`（追加 27 个 API 路径常量）
- [MODIFY] `wecodesite/src/utils/constants.js`（追加枚举映射）
- [MODIFY] `wecodesite/src/utils/common.js`（追加工具函数）

#### 验收标准
- [ ] 27 个 API 路径常量按 frontend-design.md § 15 定义
- [ ] `VERIFY_TYPE_MAP` 含 APIG=4（5 种：Cookie/SOAHeader/数字签名/SOAURL/APIG）
- [ ] 按开发规范 2.4 写 `constant.js`（§ 13.5 速查）
- [ ] **不新建** configs/ utils/ 目录（已存在）

#### 验证命令
```bash
cat wecodesite/src/configs/web.config.js | grep -E "APP_\\.|MEMBERS_\\."
# 期望看到 27 个 API 路径常量
```

---

## 🌊 Wave 2:核心服务（依赖 Wave 1）

### TASK-002:AppService 实现（12 个 1.x 接口）

**复杂度**: L
**前置依赖**: TASK-001（DDL）
**执行波次**: 2

#### 描述
按 plan.md § 4.2.1 实现 `AppService` + 12 个 1.x 接口：
- 1.1 创建应用 / 1.2 更新应用 / 1.3 获取应用基本信息 / 1.4 获取应用列表
- 1.5 EAMAP 列表 / 1.6 默认图标列表
- 1.7 更新认证方式 / 1.8 获取应用凭证 / 1.9 获取认证方式
- 1.10 绑定 EAMAP / 1.11 获取当前用户角色 / 1.12 上传图片

每个接口含 Controller + Service + Mapper + Entity + DTO + 业务校验。

**审计日志**：Controller 中 1.2 / 1.7 / 1.10 三个接口需同时声明 `@AuditLog` 注解（见 plan.md § 4.6.5）。

#### 涉及文件
- [NEW] `open-server/src/main/java/com/xxx/it/works/wecode/v2/modules/app/controller/AppController.java`
- [NEW] `open-server/src/main/java/com/xxx/it/works/wecode/v2/modules/app/service/AppService.java`
- [NEW] `open-server/src/main/java/com/xxx/it/works/wecode/v2/modules/app/service/impl/AppServiceImpl.java`
- [NEW] `open-server/src/main/java/com/xxx/it/works/wecode/v2/modules/app/mapper/AppMapper.java`
- [NEW] `open-server/src/main/java/com/xxx/it/works/wecode/v2/modules/app/entity/App.java` + 8 个 entity
- [NEW] `open-server/src/main/java/com/xxx/it/works/wecode/v2/modules/app/dto/*.java`（CreateAppRequest / UpdateAppRequest / AppResponse 等）
- [NEW] `open-server/src/main/resources/mapper/AppMapper.xml`

#### 验收标准
- [ ] 12 个 1.x 接口全部实现
- [ ] 业务校验符合 plan.md 执行逻辑（如 1.1 第 2-4 步校验 EAMAP、1.10 校验存量个人应用）
- [ ] 使用 `appContextResolver.resolveAndValidate(appId)` 做权限校验
- [ ] 1.2 / 1.7 / 1.10 三个接口标注 `@AuditLog` 注解（plan.md § 4.6.5）
- [ ] 返回 `ApiResponse<T>` 统一格式
- [ ] 业务异常抛出对应 `ResponseCodeEnum` 错误码

#### 验证命令
```bash
cd open-server && mvn test -Dtest=AppServiceTest
curl -X POST http://localhost:8080/service/open/v2/app -H "Content-Type: application/json" -d '{...}'
```

---

### TASK-006:响应码枚举 + 异常处理 + ApiResponse

**复杂度**: M
**前置依赖**: TASK-001（DDL）
**执行波次**: 2

#### 描述
按 plan.md § 4.3 实现公共响应组件：
- `ResponseCodeEnum`（§ 4.3.3 完整错误码枚举）
- `ApiResponse<T>` 统一响应格式（§ 4.3.5）
- 异常处理（§ 4.3.4 复用 V2）
- 业务异常基类 `BusinessException`

#### 涉及文件
- [NEW] `open-server/src/main/java/com/xxx/it/works/wecode/v2/common/exception/ResponseCodeEnum.java`
- [NEW] `open-server/src/main/java/com/xxx/it/works/wecode/v2/common/exception/BusinessException.java`
- [NEW] `open-server/src/main/java/com/xxx/it/works/wecode/v2/common/model/ApiResponse.java`
- [MODIFY] `open-server/src/main/java/com/xxx/it/works/wecode/v2/common/exception/GlobalExceptionHandler.java`

#### 验收标准
- [ ] `ResponseCodeEnum` 包含 plan.md § 4.3.2 完整错误码表（400100、403100、409100 等）
- [ ] `ApiResponse` 字段：`code`、`messageZh`、`messageEn`、`data`、`page`
- [ ] `BusinessException` 支持 codeEnum + message 参数
- [ ] `GlobalExceptionHandler` 统一捕获并返回 `ApiResponse`

#### 验证命令
```bash
cd open-server && mvn test -Dtest=ResponseCodeEnumTest
mvn test -Dtest=ApiResponseTest
```

---

### TASK-010:前端 5 个页面 thunk.js（27 个 API）

**复杂度**: L
**前置依赖**: TASK-009（公共配置追加）
**执行波次**: 2

#### 描述
**wecodesite 不使用独立 services 目录**，而是按页面分发 thunk.js（项目现有模式）：
- `pages/AppList/thunk.js`（已存在）— 追加 1.4/1.5/1.6/1.1/1.12（5 个 thunk）
- `pages/BasicInfo/thunk.js`（已存在）— 追加 1.11/1.3/1.2/1.8/1.9/1.7/1.10（7 个 thunk）
- `pages/Members/thunk.js`（已存在）— 追加 2.1/2.2/2.3/2.4/2.5（5 个 thunk）
- `pages/Capabilities/thunk.js`（已存在）— 追加 3.1/3.2/3.3（3 个 thunk）
- `pages/VersionRelease/thunk.js`（已存在）— 追加 4.1/4.2/4.3/4.4/4.5/4.6/4.7（7 个 thunk）

合计 27 个 thunk。

**每个 thunk 必须遵守开发规范 2.3 try-catch**：
```js
export const fetchXxx = async (params = {}) => {
  try {
    const result = await fetchApi(API_CONFIG.XXX, params);
    return result || {};
  } catch (err) {
    return {};
  }
};
```

**错误提示按开发规范 9.2**：
```js
message.error(result?.messageZh || result?.message || '固定文案');
```

#### 涉及文件
- [MODIFY] `wecodesite/src/pages/AppList/thunk.js`（追加 5 个 thunk）
- [MODIFY] `wecodesite/src/pages/BasicInfo/thunk.js`（追加 7 个 thunk）
- [MODIFY] `wecodesite/src/pages/Members/thunk.js`（追加 5 个 thunk）
- [MODIFY] `wecodesite/src/pages/Capabilities/thunk.js`（追加 3 个 thunk）
- [MODIFY] `wecodesite/src/pages/VersionRelease/thunk.js`（追加 7 个 thunk）

#### 验收标准
- [ ] 27 个 thunk 全部实现（**追加到已存在的 5 个 thunk.js**）
- [ ] **每个 thunk 都有 try-catch**（开发规范 2.3）
- [ ] **禁止 useCallback**（开发规范 6.1）：所有函数用普通箭头函数
- [ ] 3+ 参数用对象（开发规范 6.2）
- [ ] API_CONFIG 路径与 web.config.js 一致
- [ ] 返回 `result || {}` 兜底

#### 验证命令
```bash
cd wecodesite && npx eslint src/pages/*/thunk.js
# 期望无 useCallback、无内联样式
```

---

## 🌊 Wave 3:扩展模块（依赖 Wave 2）

### TASK-003:MemberService 实现（5 个 2.x 接口）

**复杂度**: L
**前置依赖**: TASK-002（AppService）
**执行波次**: 3

#### 描述
按 plan.md § 4.2.2 实现 `MemberService` + 5 个 2.x 接口：
- 2.1 获取应用成员列表（分页）
- 2.2 添加成员（批量）
- 2.3 删除成员（受 Owner 保护）
- 2.4 转移 Owner（原 Owner 成员记录被删除，不再拥有该应用访问权限）

**审计日志**：Controller 中 2.2 / 2.3 / 2.4 三个接口需同时声明 `@AuditLog` 注解（见 plan.md § 4.6.5）。
- 2.5 搜索可添加的用户

业务校验：
- 2.2 第 3.1-3.3 步：操作人角色（Developer 无权、Admin 只能加 Developer、Owner 可加 Developer+Admin）
- 2.3 第 4 步：不能删 Owner
- 2.4 第 3-4 步：操作人必须 Owner、不能转给自己

#### 涉及文件
- [NEW] `open-server/src/main/java/.../modules/member/controller/MemberController.java`
- [NEW] `open-server/src/main/java/.../modules/member/service/MemberService.java` + impl
- [NEW] `open-server/src/main/java/.../modules/member/mapper/AppMemberMapper.java`
- [NEW] `open-server/src/main/java/.../modules/member/entity/AppMember.java`
- [NEW] `open-server/src/main/java/.../modules/member/dto/AddMemberRequest.java`
- [NEW] `open-server/src/main/java/.../modules/member/dto/TransferOwnerRequest.java`
- [NEW] `open-server/src/main/resources/mapper/AppMemberMapper.xml`

#### 验收标准
- [ ] 5 个 2.x 接口全部实现
- [ ] 业务校验符合 plan.md § 4.2.2 执行逻辑
- [ ] 删除成员保护 Owner（错误码 409201）
- [ ] Admin 只能加 Developer（错误码 403201）
- [ ] 转移 Owner 校验不能转给自己（错误码 409202）
- [ ] 2.2 / 2.3 / 2.4 三个接口标注 `@AuditLog` 注解（plan.md § 4.6.5）

#### 验证命令
```bash
cd open-server && mvn test -Dtest=MemberServiceTest
```

---

### TASK-004:AbilityService 实现（3 个 3.x 接口）

**复杂度**: M
**前置依赖**: TASK-002（AppService）
**执行波次**: 3

#### 描述
按 plan.md § 4.2.3 实现 `AbilityService` + 3 个 3.x 接口：
- 3.1 能力列表（主内容卡片）— 过滤 ability_type=6（应用入群通知）
- 3.2 添加能力

**审计日志**：Controller 中 3.2 需同时声明 `@AuditLog` 注解（见 plan.md § 4.6.5）。
- 3.3 已订阅能力列表（侧边栏）

#### 涉及文件
- [NEW] `open-server/src/main/java/.../modules/ability/controller/AbilityController.java`
- [NEW] `open-server/src/main/java/.../modules/ability/service/AbilityService.java` + impl
- [NEW] `open-server/src/main/java/.../modules/ability/mapper/AppAbilityRelationMapper.java`
- [NEW] `open-server/src/main/java/.../modules/ability/entity/AppAbilityRelation.java`
- [NEW] `open-server/src/main/resources/mapper/AppAbilityRelationMapper.xml`

#### 验收标准
- [ ] 3 个 3.x 接口全部实现
- [ ] 3.1 第 3 步过滤 ability_type=6
- [ ] 3.2 第 5 步校验 `abilityType` 合法（1-7）
- [ ] 3.2 第 6-7 步写入关联表
- [ ] 3.2 接口标注 `@AuditLog` 注解（plan.md § 4.6.5）

#### 验证命令
```bash
cd open-server && mvn test -Dtest=AbilityServiceTest
```

---

### TASK-005:VersionService 实现（7 个 4.x 接口）

**复杂度**: L
**前置依赖**: TASK-002（AppService）
**执行波次**: 3

#### 描述
按 plan.md § 4.2.4 实现 `VersionService` + 7 个 4.x 接口：
- 4.1 获取版本列表（分页）
- 4.2 创建版本（**不传 abilityIds**，后端自动从已订阅能力带出）
- 4.3 获取版本详情
- 4.4 发布版本（启动 V2 审批，status=1→2）
- 4.5 撤回版本（V2 撤销，status=2→1）
- 4.6 删除版本（status=1 或 3）
- 4.7 更新版本（仅 status=1）

**审计日志**：Controller 中 4.2 / 4.4 / 4.5 / 4.6 / 4.7 五个接口需同时声明 `@AuditLog` 注解（见 plan.md § 4.6.5）。

业务校验：
- 4.2 第 5 步：不能有 status=1、status=2 或 status=3 的版本（错误码 409301）
- 4.7 第 4 步：仅 status=1 可编辑（错误码 409303）
- 4.4/4.5/4.6：状态机约束

#### 涉及文件
- [NEW] `open-server/src/main/java/.../modules/version/controller/VersionController.java`
- [NEW] `open-server/src/main/java/.../modules/version/service/VersionService.java` + impl
- [NEW] `open-server/src/main/java/.../modules/version/mapper/AppVersionMapper.java`
- [NEW] `open-server/src/main/java/.../modules/version/entity/AppVersion.java`
- [NEW] `open-server/src/main/java/.../modules/version/dto/CreateVersionRequest.java`
- [NEW] `open-server/src/main/java/.../modules/version/dto/UpdateVersionRequest.java`
- [NEW] `open-server/src/main/resources/mapper/AppVersionMapper.xml`

#### 验收标准
- [ ] 7 个 4.x 接口全部实现
- [ ] 4.2 第 5 步校验无已存在版本
- [ ] 4.7 第 4 步仅 status=1 可编辑
- [ ] 4.4/4.5 V2 审批集成
- [ ] 4.6 第 4 步校验 status=1 或 3
- [ ] 4.2 / 4.4 / 4.5 / 4.6 / 4.7 五个接口标注 `@AuditLog` 注解（plan.md § 4.6.5）

#### 验证命令
```bash
cd open-server && mvn test -Dtest=VersionServiceTest
```

---

### TASK-007:RBAC 授权模型 + 权限校验 + 加密

**复杂度**: M
**前置依赖**: TASK-002（AppService）
**执行波次**: 3

#### 描述
按 plan.md § 4.4 实现授权和安全：
- 4.4.1 RBAC（角色 × 模块权限矩阵，§ 13.2 frontend-design）
- 4.4.2 敏感数据加密（APP Secret / apiSecret 密文存储）
- 4.4.3 防越权（`appContextResolver.resolveAndValidate(appId)`）

#### 涉及文件
- [NEW] `open-server/src/main/java/.../common/security/PermissionChecker.java`
- [NEW] `open-server/src/main/java/.../common/security/EncryptionService.java`
- [MODIFY] `open-server/src/main/java/.../modules/app/service/impl/AppServiceImpl.java`（集成权限校验）
- [MODIFY] `open-server/src/main/java/.../modules/member/service/impl/MemberServiceImpl.java`（集成权限校验）
- [MODIFY] `open-server/src/main/java/.../modules/ability/service/impl/AbilityServiceImpl.java`（集成权限校验）
- [MODIFY] `open-server/src/main/java/.../modules/version/service/impl/VersionServiceImpl.java`（集成权限校验）

#### 验收标准
- [ ] 4 角色 × 4 模块权限矩阵实现（plan.md § 4.4.1）
- [ ] `appContextResolver` 在所有 Service 入口调用
- [ ] APP Secret / apiSecret 加密存储 + 解密返回
- [ ] 未授权用户访问返回 403

#### 验证命令
```bash
cd open-server && mvn test -Dtest=PermissionCheckerTest
mvn test -Dtest=EncryptionServiceTest
```

---

### TASK-011:页面内 useEffect 守卫（替代原 RequireAppAccess/RequireBusinessApp）

**复杂度**: M
**前置依赖**: TASK-009（目录结构）
**执行波次**: 3

#### 描述
按 frontend-design.md § 3.2 和 test-cases.md § 9.1 实现**页面级 useEffect 守卫**（**非独立路由守卫组件**）：
- 5 条路由（§ 3.1 路由表：AppList/BasicInfo/Members/Capabilities/VersionRelease）
- 守卫逻辑在每个页面 `index.jsx` 的 `useEffect` 中实现
- BasicInfo：调 1.11，`role` 为 null → 非成员 → 跳 404
- Members/Capabilities/VersionRelease：调 1.3，`appType !== 1` → 非业务应用 → 跳 `/basic-info?appId=xxx`

**严禁使用 `useCallback`**（开发规范 6.1），**严禁行内样式**（开发规范 6.2）。

#### 涉及文件
- [NEW] `wecodesite/src/pages/AppList/route.js`（路由配置）
- [MODIFY] `wecodesite/src/pages/BasicInfo/index.jsx`（添加 useEffect 守卫）
- [MODIFY] `wecodesite/src/pages/Members/index.jsx`（添加 useEffect 守卫）
- [MODIFY] `wecodesite/src/pages/Capabilities/index.jsx`（添加 useEffect 守卫）
- [MODIFY] `wecodesite/src/pages/VersionRelease/index.jsx`（添加 useEffect 守卫）

#### 验收标准
- [ ] 5 条路由与 frontend-design.md § 3.1 路由表完全一致
- [ ] 守卫逻辑在页面 useEffect 中实现（**非独立组件**）
- [ ] BasicInfo 守卫：调 1.11，role=null 跳 404
- [ ] Members/Capabilities/VersionRelease 守卫：调 1.3，appType!=1 跳 `/basic-info?appId=xxx`
- [ ] 普通箭头函数（无 useCallback）
- [ ] 样式在 .m.less 文件中（无内联样式）

#### 验证命令
```bash
cd wecodesite && npx eslint src/pages/
# 期望无 useCallback、无内联样式警告
```

---

### TASK-012:5 个页面的 useState 状态管理

**复杂度**: M
**前置依赖**: TASK-009（公共配置）
**执行波次**: 3

#### 描述
**wecodesite 不使用独立 stores 目录**，状态全部用 React `useState` 在页面内管理（项目现有模式）：
- `pages/AppList/AppList.jsx`（已存在）— useState 管列表/分页/loading
- `pages/BasicInfo/BasicInfo.jsx`（已存在）— useState 管 appData/编辑态/凭证显示
- `pages/Members/Members.jsx`（已存在）— useState 管 members/角色过滤
- `pages/Capabilities/Capabilities.jsx`（已存在）— useState 管能力网格/订阅列表
- `pages/VersionRelease/VersionRelease.jsx`（已存在）— useState 管版本列表/创建表单

**严禁 useCallback**（开发规范 6.1），按需使用 useState/useEffect/useRef。

#### 涉及文件
- [MODIFY] `wecodesite/src/pages/AppList/AppList.jsx`（补 useState）
- [MODIFY] `wecodesite/src/pages/BasicInfo/BasicInfo.jsx`（补 useState）
- [MODIFY] `wecodesite/src/pages/Members/Members.jsx`（补 useState）
- [MODIFY] `wecodesite/src/pages/Capabilities/Capabilities.jsx`（补 useState）
- [MODIFY] `wecodesite/src/pages/VersionRelease/VersionRelease.jsx`（补 useState）

#### 验收标准
- [ ] 5 个页面都使用 useState
- [ ] 命名严格按页面文件名
- [ ] 普通箭头函数（无 useCallback）
- [ ] 不重复导出 utils/constants.js 内容（开发规范 § 13.5 速查）

#### 验证命令
```bash
grep -r 'useState' wecodesite/src/pages/ | wc -l
# 期望 > 20
```

---

## 🌊 Wave 4:审计日志 + 核心页面（依赖 Wave 3）

### TASK-016:审计日志（FR-019）

**复杂度**: M
**前置依赖**: TASK-002, TASK-003, TASK-004, TASK-005（4 个 Service + Controller 已建好）
**执行波次**: 4

#### 描述
按 plan.md § 4.6 实现 FR-019 操作审计日志，复用现有 `@AuditLog` 注解 + `OperateLogV2Aspect` 切面 + `AuditLogService` 异步持久化基础设施，本期产出：

1. **OperateEnum 追加 12 个枚举值**（§ 4.6.3）：UPDATE_APP / UPDATE_APP_VERIFY_TYPE / BIND_APP_EAMAP / ADD_APP_MEMBER / DELETE_APP_MEMBER / TRANSFER_APP_OWNER / ADD_APP_ABILITY / CREATE_APP_VERSION / UPDATE_APP_VERSION / PUBLISH_APP_VERSION / WITHDRAW_APP_VERSION / DELETE_APP_VERSION
2. **4 个 EntitySnapshotLoader 实现**（§ 4.6.4）：AppSnapshotLoader / AppMemberSnapshotLoader / AppAbilityRelationSnapshotLoader / AppVersionSnapshotLoader
3. **OperateLogV2Aspect 切面扩展**（§ 4.6.6）：
   - 注入 `AppContextResolver`，`extractResourceId()` 增加 varchar appId 回退（`parseLong` 失败时用 `resolveAndValidate` 转换）
   - `loadEntitySnapshot()` 增加 appId 上下文传递，`AppMemberSnapshotLoader` 支持 (appId, accountId) 复合键查询
4. **AppMemberSnapshotLoader 新增** `loadByAppIdAndAccountId()` 重载方法

**注**：12 个 Controller 方法的 `@AuditLog` 注解已在 TASK-002/003/004/005 中一并添加，本 TASK 不重复。

#### 涉及文件
- [MODIFY] `open-server/src/main/java/.../common/enums/OperateEnum.java`（追加 12 个枚举值）
- [MODIFY] `open-server/src/main/java/.../common/interceptor/OperateLogV2Aspect.java`（注入 AppContextResolver + extractResourceId 扩展 + loadEntitySnapshot 扩展）
- [NEW] `open-server/src/main/java/.../common/snapshot/AppSnapshotLoader.java`
- [NEW] `open-server/src/main/java/.../common/snapshot/AppMemberSnapshotLoader.java`（含 loadByAppIdAndAccountId 重载）
- [NEW] `open-server/src/main/java/.../common/snapshot/AppAbilityRelationSnapshotLoader.java`
- [NEW] `open-server/src/main/java/.../common/snapshot/AppVersionSnapshotLoader.java`

#### 验收标准
- [ ] 12 个 OperateEnum 枚举值追加，`needsBeforeData()` / `needsAfterData()` 行为正确
- [ ] 4 个 Loader 注册到 `EntitySnapshotLoaderFactory`（启动日志可验证）
- [ ] `extractResourceId()` 对纯数字 String 走原 `parseLong`，对 varchar appId 走 `appContextResolver` 回退
- [ ] `AppMemberSnapshotLoader.loadByAppIdAndAccountId()` 可按 (appId, accountId) 查到成员实体
- [ ] plan.md § 4.6.9 全部 6 个验证场景通过
- [ ] 现有 11 个权限订阅审计接口不受影响（回归测试通过）

#### 验证命令
```bash
cd open-server && mvn test -Dtest=OperateLogV2AspectTest
cd open-server && mvn test -Dtest=AppSnapshotLoaderTest,AppMemberSnapshotLoaderTest
# 集成验证：调用 PUT /app/{appId} 后检查 openplatform_operate_log_t 是否写入
```

---

### TASK-013:5 个核心组件（TopNav/EmptyState/Pagination/CardGrid/AuthMethodCard）

**复杂度**: L
**前置依赖**: TASK-009（公共配置）
**执行波次**: 4

#### 描述
按 frontend-design.md § 4 + 开发规范 7.x 创建 5 个**应用管理特有**的核心组件：
- `TopNav`（§ 4.1）— 全站顶部导航
- `EmptyState`（§ 4.2）— 空状态
- `Pagination`（§ 4.4）— 分页
- `CardGrid`（§ 4.5）— 卡片网格
- `AuthMethodCard`（§ 8.3.3）— 认证方式卡片（5 种含 APIG + 多选）

**注**：Toast/Modal 复用 Ant Design 组件（message/Modal），不另建。SearchBar 删（按用户反馈 § 5 删搜索）。

每个组件：
- 独立目录 + `xxx.jsx` + `xxx.m.less`（开发规范 7.3.3）
- **严禁行内样式**（开发规范 6.2）
- **严禁 useCallback**（开发规范 6.1）

#### 涉及文件
- [NEW] `wecodesite/src/components/TopNav/TopNav.jsx` + `TopNav.m.less`
- [NEW] `wecodesite/src/components/EmptyState/EmptyState.jsx` + `EmptyState.m.less`
- [NEW] `wecodesite/src/components/Pagination/Pagination.jsx` + `Pagination.m.less`
- [NEW] `wecodesite/src/components/CardGrid/CardGrid.jsx` + `CardGrid.m.less`
- [NEW] `wecodesite/src/components/AuthMethodCard/AuthMethodCard.jsx` + `AuthMethodCard.m.less`

#### 验收标准
- [ ] 5 个组件创建
- [ ] AuthMethodCard 支持 5 种认证方式 + 多选
- [ ] 每个组件有独立目录和 .m.less
- [ ] 无行内样式（开发规范 6.2）
- [ ] 无 useCallback（开发规范 6.1）
- [ ] 注释按开发规范 5.3 / 7.3.4

#### 验证命令
```bash
cd wecodesite && npx eslint src/components/{TopNav,EmptyState,Pagination,CardGrid,AuthMethodCard}/
# 期望无 useCallback、无 style={{...}} 警告
```

---

### TASK-014:8 大页面

**复杂度**: L
**前置依赖**: TASK-010（services）+ TASK-011（路由）+ TASK-012（store）+ TASK-013（组件）
**执行波次**: 4

#### 描述
**wecodesite 已有 5 个独立页面**（不需新建），按 frontend-design.md § 5-§ 12 补全实现：
- `pages/AppList/AppList.jsx`（已存在）— § 5 应用列表
- `pages/BasicInfo/BasicInfo.jsx`（已存在）— § 7-§ 8 应用详情（凭证/基本信息/认证方式/升级EAMAP）
- `pages/Members/Members.jsx`（已存在）— § 9 成员管理（列表/添加/删除/转移）
- `pages/Capabilities/Capabilities.jsx`（已存在）— § 10 应用能力（网格/添加/配置）
- `pages/VersionRelease/VersionRelease.jsx`（已存在）— § 11-§ 12 版本管理（列表/创建/详情）
- `components/CreateAppModal/CreateAppModal.jsx`（已存在）— § 6 创建应用 Modal

每个页面（已存在，按规范**补全**）：
- 调用对应 thunk.js（§ 15 27 API）
- 严格遵守 § 13.4 前端必做校验清单
- 严禁 useCallback/行内样式
- 严格按 3 种应用类型（业务/普通个人/存量个人）实现头部
- 严格按 1.11 范围（仅 L1 入口 + L3 成员 Tab 按钮）实现
- apiSecret 16 位 + 必须同时含字母+数字
- 图标 128×128px / 示意图 360×200px
- 时间格式 yyyy-MM-dd HH:mm:ss

#### 涉及文件
- [MODIFY] `wecodesite/src/pages/AppList/AppList.jsx`（按 § 5 补全）
- [MODIFY] `wecodesite/src/pages/BasicInfo/BasicInfo.jsx`（按 § 7-§ 8 补全）
- [MODIFY] `wecodesite/src/pages/Members/Members.jsx`（按 § 9 补全）
- [MODIFY] `wecodesite/src/pages/Capabilities/Capabilities.jsx`（按 § 10 补全）
- [MODIFY] `wecodesite/src/pages/VersionRelease/VersionRelease.jsx`（按 § 11-§ 12 补全）
- [MODIFY] `wecodesite/src/components/CreateAppModal/CreateAppModal.jsx`（按 § 6 补全）

#### 验收标准
- [ ] 5 大页面全部按 frontend-design.md 补全
- [ ] § 13.4 校验清单 20+ 字段全部生效
- [ ] 3 种应用类型头部区分（业务：应用名+EAMAP / 存量个人：应用名+绑定按钮 / 普通个人：仅应用名）
- [ ] 4 个菜单显隐按 appType（业务 4 / 个人 1）
- [ ] 成员 Tab 按钮按 1.11 role 显隐
- [ ] 添加能力成功**直接跳转到对应菜单**
- [ ] 详情页仅"待发布"状态有按钮
- [ ] 所有 § 13 横切规则遵守

#### 验证命令
```bash
cd wecodesite && npx eslint src/pages/ src/components/CreateAppModal/
npm run build
```

---

## 🌊 Wave 5:测试（依赖 Wave 4）

### TASK-008:后端单元测试（4 个 Service）

**复杂度**: L
**前置依赖**: TASK-003, TASK-004, TASK-005（3 个 Service 完成后）
**执行波次**: 5

#### 描述
为 4 个 Service 写单元测试（JUnit 5 + Mockito）：
- AppServiceTest：12 个 1.x 接口
- MemberServiceTest：5 个 2.x 接口
- AbilityServiceTest：3 个 3.x 接口
- VersionServiceTest：7 个 4.x 接口

每个接口至少 2 个测试用例（正常 + 异常路径）。

**审计日志补充**：验证 12 个 `@AuditLog` 注解接口在操作后 `openplatform_operate_log_t` 写入正确记录，包括 varchar appId 自动解析、复合键定位、before/after 快照、失败 status=0 等场景（plan.md § 4.6.9）。

#### 涉及文件
- [NEW] `open-server/src/test/java/.../modules/app/service/AppServiceTest.java`
- [NEW] `open-server/src/test/java/.../modules/member/service/MemberServiceTest.java`
- [NEW] `open-server/src/test/java/.../modules/ability/service/AbilityServiceTest.java`
- [NEW] `open-server/src/test/java/.../modules/version/service/VersionServiceTest.java`

#### 验收标准
- [ ] 4 个测试类
- [ ] 27 个接口 × ≥2 用例 = ≥54 个测试方法
- [ ] 12 个 @AuditLog 接口的审计日志写入验证（plan.md § 4.6.9）
- [ ] 测试覆盖率 ≥ 80%
- [ ] 所有测试通过

#### 验证命令
```bash
cd open-server && mvn test
# 期望所有测试通过，覆盖率 ≥ 80%
mvn jacoco:report  # 查看覆盖率
```

---

### TASK-015:test-cases.md 100+ TC 全部通过

**复杂度**: L
**前置依赖**: TASK-014（前端 8 大页面）
**执行波次**: 5

#### 描述
按 `test-cases.md` 跑 100+ 测试用例（9 大章节 S1-S9）：
- S1 浏览应用列表（TC-1-01 ~ TC-1-12）
- S2 创建应用（TC-2-01 ~ TC-2-34）
- S3 进入应用详情（TC-3-01 ~ TC-3-14a）
- S4 凭证与基础信息（TC-4-01 ~ TC-4-38）
- S5 成员管理（TC-5-01 ~ TC-5-44）
- S6 能力管理（TC-6-01 ~ TC-6-13）
- S7 版本管理（TC-7-01 ~ TC-7-08）
- S8 版本详情（TC-8-01 ~ TC-8-23）
- S9 横切关注点（TC-9-01 ~ TC-9-34）

按开发规范附录 7 项检查清单自检：
1. 文件结构检查
2. 代码重复度检查
3. thunk.js 规范检查
4. constant.js 规范检查
5. 组件规范检查
6. 样式规范检查
7. 其他检查

#### 涉及文件
- [MODIFY] 各页面/组件（按测试失败项修复）

#### 验收标准
- [ ] test-cases.md S1-S9 全部 TC 通过
- [ ] 开发规范附录 7 项检查清单全部通过
- [ ] 功能完成度 100% (15/15 FR)
- [ ] ESLint 警告 0

#### 验证命令
```bash
cd wecodesite && npx tsc --noEmit
npm run lint
# 跑 test-cases.md 关键 TC
npm test
```

---

## ✅ 任务分解完成

**Feature**: 开放平台应用管理 (APP-MGMT-001)
**状态**: tasked
**文件**: `.sddu/specs-tree-root/specs-tree-app-list/tasks.md`

### 任务汇总
- **总任务数**: 16 个
- **复杂度分布**: S 级 0 个，M 级 6 个，L 级 10 个
- **执行波次**: 5 个波次（Wave 1 → Wave 5）

### 下一步
👉 运行 `@sddu-build TASK-001` 开始实现第一个任务（数据库 DDL 迁移脚本）

### 自动触发
完成后自动触发 `@sddu-docs` 扫描并更新 `.sddu/` 目录导航。
