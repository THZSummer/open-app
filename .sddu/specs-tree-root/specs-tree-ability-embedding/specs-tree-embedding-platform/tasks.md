# 任务分解：嵌入能力平台面

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
  TASK-001 [S]  DB 迁移 V4 + Ability 实体新增字段

Wave 2 ─── (依赖 Wave 1)
  TASK-002 [S]  DTO/VO 类定义
  TASK-003 [M]  Mapper 层扩展

Wave 3 ─── (依赖 Wave 2)
  TASK-004 [L]  AdminAbilityService 接口 + 实现

Wave 4 ─── (依赖 Wave 3)
  TASK-005 [M]  AdminAbilityController（4 个接口）

Wave 5 ─── (依赖 Wave 4，前端）
  TASK-006 [L]  前端能力目录管理页面
  TASK-007 [S]  前端路由配置
```

## 2. 任务列表
> 每个任务的详细定义

### TASK-001: DB 迁移 + Ability 实体扩展
> 数据库新增字段 + 实体类同步

| 属性 | 值 |
|------|-----|
| **复杂度** | S |
| **前置依赖** | 无 |
| **执行波次** | 1 |
| **对应 FR** | FR-002（字段基础） |

**描述**: 新增 Flyway 迁移文件为 `openplatform_ability_t` 增加 `frontend_entry_url`、`hidden` 字段；同步在 `Ability` 实体类中新增对应属性。

**涉及文件**:

| 操作 | 文件路径 |
|:--:|------|
| NEW | `open-server/src/main/resources/db/migration/V4__add_ability_admin_fields.sql` |
| MODIFY | `open-server/src/main/java/com/xxx/it/works/wecode/v2/modules/ability/entity/Ability.java` |

**验收标准**:
- [ ] 迁移文件命名为 `V4__add_ability_admin_fields.sql`，包含两条 `ALTER TABLE ADD COLUMN` 语句
- [ ] `frontend_entry_url` 为 VARCHAR(512) DEFAULT NULL，`hidden` 为 TINYINT(1) DEFAULT 0
- [ ] `Ability` 实体新增 `frontendEntryUrl`(String)、`hidden`(Integer) 字段及 getter/setter
- [ ] Maven 编译通过

**验证命令**:
```bash
mvn -f open-server/pom.xml compile -q
```

### TASK-002: 管理面 DTO/VO 类定义
> 请求与响应数据结构

| 属性 | 值 |
|------|-----|
| **复杂度** | S |
| **前置依赖** | TASK-001 |
| **执行波次** | 2 |
| **对应 FR** | FR-001、FR-002、FR-003 |

**描述**: 新增管理面所需的请求 DTO 和响应 VO 类，含字段校验注解（JSR-303）。

**涉及文件**:

| 操作 | 文件路径 |
|:--:|------|
| NEW | `open-server/.../ability/dto/admin/AdminAbilityListRequest.java` |
| NEW | `open-server/.../ability/dto/admin/AdminAbilityCreateRequest.java` |
| NEW | `open-server/.../ability/dto/admin/AdminAbilityUpdateRequest.java` |
| NEW | `open-server/.../ability/vo/admin/AdminAbilityVO.java` |
| NEW | `open-server/.../ability/vo/admin/AdminAbilityDetailVO.java` |

**验收标准**:
- [ ] `AdminAbilityListRequest` 含 curPage/pageSize/keyword/sortField/sortOrder 字段，带默认值
- [ ] `AdminAbilityCreateRequest` 含 abilityType(≥100 校验)、nameCn、nameEn、descCn、descEn、iconUrl、diagramUrl、orderNum、frontendEntryUrl、hidden，必填字段标注 `@NotNull`
- [ ] `AdminAbilityUpdateRequest` 所有字段可选（abilityType 不出现，不可修改）
- [ ] `AdminAbilityVO` 含列表展示全部字段（含 frontendEntryUrl、hidden、createTime、updateBy、updateTime）
- [ ] `AdminAbilityDetailVO` 含详情字段
- [ ] Maven 编译通过

**验证命令**:
```bash
mvn -f open-server/pom.xml compile -q
```

### TASK-003: Mapper 层扩展
> 数据访问层新增管理面查询方法

| 属性 | 值 |
|------|-----|
| **复杂度** | M |
| **前置依赖** | TASK-001 |
| **执行波次** | 2 |
| **对应 FR** | FR-001、FR-004 |

**描述**: 在 `AbilityMapper`、`AbilityPropertyMapper` 中新增管理面所需查询方法（分页查询、按 abilityType 查询、订阅数统计、按 abilityType 删除属性）。

**涉及文件**:

| 操作 | 文件路径 |
|:--:|------|
| MODIFY | `open-server/.../ability/mapper/AbilityMapper.java` |
| MODIFY | `open-server/.../ability/mapper/AbilityPropertyMapper.java` |
| MODIFY | `open-server/.../ability/mapper/AbilityMapper.xml`（如存在） |
| MODIFY | `open-server/.../ability/mapper/AbilityPropertyMapper.xml`（如存在） |

**验收标准**:
- [ ] `AbilityMapper` 新增分页查询方法（支持 keyword 模糊搜索 + 排序）
- [ ] `AbilityMapper` 新增按 abilityType 查询方法（唯一性校验用）
- [ ] `AbilityMapper` 新增统计订阅数方法（查 `app_ability_relation_t`）
- [ ] `AbilityPropertyMapper` 新增按 abilityType 删除方法
- [ ] Maven 编译通过

**验证命令**:
```bash
mvn -f open-server/pom.xml compile -q
```

### TASK-004: AdminAbilityService 接口与实现
> 管理面业务逻辑层

| 属性 | 值 |
|------|-----|
| **复杂度** | L |
| **前置依赖** | TASK-002、TASK-003 |
| **执行波次** | 3 |
| **对应 FR** | FR-001、FR-002、FR-003、FR-004 |

**描述**: 实现 AdminAbilityService 接口及 AdminAbilityServiceImpl，包含列表查询、创建、更新、删除四个业务方法，含 abilityType 唯一性校验、权限校验、订阅检查、文件上传调用。

**涉及文件**:

| 操作 | 文件路径 |
|:--:|------|
| NEW | `open-server/.../ability/service/AdminAbilityService.java` |
| NEW | `open-server/.../ability/service/impl/AdminAbilityServiceImpl.java` |

**验收标准**:
- [ ] `list()`：分页查询，关联 ability_p_t 获取图标/示意图，返回 `AdminAbilityVO` 列表
- [ ] `create()`：校验 abilityType 唯一性（含预置编码 1-7），校验 abilityType≥100，写入主表+属性表，状态默认启用
- [ ] `update()`：abilityType 不可修改；按需更新字段；图标/示意图可替换
- [ ] `delete()`：先查 `app_ability_relation_t` 订阅数，有关联则拒绝（返回 409），无关联则删除主表+属性表
- [ ] 管理员权限校验逻辑（NFR-001）
- [ ] 审计日志记录（NFR-004）
- [ ] Maven 编译通过

**验证命令**:
```bash
mvn -f open-server/pom.xml compile -q
```

### TASK-005: AdminAbilityController
> 管理面控制器（4 个 REST 接口）

| 属性 | 值 |
|------|-----|
| **复杂度** | M |
| **前置依赖** | TASK-004 |
| **执行波次** | 4 |
| **对应 FR** | FR-001、FR-002、FR-003、FR-004 |

**描述**: 新增 AdminAbilityController，暴露 4 个管理面接口（列表/创建/更新/删除），基础路径 `/service/open/v2/ability/admin`，复用现有 `ApiResponse` 信封。

**涉及文件**:

| 操作 | 文件路径 |
|:--:|------|
| NEW | `open-server/.../ability/controller/AdminAbilityController.java` |

**验收标准**:
- [ ] `GET /ability/admin/list` - 分页列表，返回 `ApiResponse<List<AdminAbilityVO>>` + page
- [ ] `POST /ability/admin` - 创建，返回 `ApiResponse` 含 abilityType/nameCn/createTime
- [ ] `PUT /ability/admin/{id}` - 更新，返回 `ApiResponse` 含 id/updateTime
- [ ] `DELETE /ability/admin/{id}` - 删除，返回 `ApiResponse` 含 id
- [ ] 错误码正确：400(参数)、403(非管理员)、404(不存在)、409(编码冲突/有订阅)
- [ ] Maven 编译通过

**验证命令**:
```bash
mvn -f open-server/pom.xml compile -q
```

### TASK-006: 前端能力目录管理页面
> market-web 管理后台页面

| 属性 | 值 |
|------|-----|
| **复杂度** | L |
| **前置依赖** | TASK-005 |
| **执行波次** | 5 |
| **对应 FR** | FR-001、FR-002、FR-003、FR-004 |

**描述**: 在 market-web 新增能力目录管理页面，包含列表页（分页+搜索+排序展示）和创建/编辑表单（含图标/示意图上传、abilityType 编码输入、前端入口URL、hidden 切换）及删除交互（含订阅提示）。

**涉及文件**:

| 操作 | 文件路径 |
|:--:|------|
| NEW | `market-web/src/pages/AbilityAdmin/index.tsx`（列表页） |
| NEW | `market-web/src/pages/AbilityAdmin/AbilityForm.tsx`（创建/编辑表单） |
| NEW | `market-web/src/pages/AbilityAdmin/services.ts`（API 调用） |

**验收标准**:
- [ ] 列表页分页展示：abilityType、中英文名、描述、图标、示意图缩略图、排序号、前端入口URL、hidden 状态、创建/更新时间
- [ ] 列表支持关键字搜索（中英文名模糊）
- [ ] 创建表单：abilityType(int，提示≥100)、中英文名(必填)、描述、图标/示意图上传、排序号、前端入口URL(http/https校验)、hidden 开关
- [ ] 编辑表单：abilityType 只读不可改，其余可编辑
- [ ] 删除：有订阅时展示"已被 XX 个应用订阅"提示并阻止
- [ ] 前端构建通过

**验证命令**:
```bash
cd market-web && npm run build
```

### TASK-007: 前端路由配置
> 管理页面路由注册

| 属性 | 值 |
|------|-----|
| **复杂度** | S |
| **前置依赖** | TASK-006 |
| **执行波次** | 5 |
| **对应 FR** | FR-001 |

**描述**: 在 market-web 路由配置中新增能力目录管理页面的路由项。

**涉及文件**:

| 操作 | 文件路径 |
|:--:|------|
| MODIFY | `market-web/src/router/config.ts`（或等效路由配置文件） |

**验收标准**:
- [ ] 新增路由 `/ability-admin` 指向 AbilityAdmin 页面
- [ ] 路由纳入管理后台菜单（如适用）
- [ ] 前端构建通过

**验证命令**:
```bash
cd market-web && npm run build
```

## 3. 任务汇总
> 任务数量、复杂度和波次的统计总览

| 统计项 | 数值 |
|--------|:--:|
| 总任务数 | 7 |
| S 级 (简单) | 3 |
| M 级 (中等) | 2 |
| L 级 (复杂) | 2 |
| 执行波次 | 5 |

## 4. 执行策略
> 各波次的执行说明

| 波次 | 任务 | 策略 |
|:--:|------|------|
| 1 | TASK-001 | 并行执行（基础数据层） |
| 2 | TASK-002, TASK-003 | 并行执行（数据结构与访问层，互不依赖） |
| 3 | TASK-004 | 逐个执行（业务逻辑，依赖 Wave 2） |
| 4 | TASK-005 | 逐个执行（接口层，依赖 Wave 3） |
| 5 | TASK-006, TASK-007 | 并行执行（前端，依赖后端接口完成） |

## 修订记录
> 记录本文档的版本变更历史

| 版本 | 变更说明 | 日期 | 修订人 |
|------|---------|------|--------|
| v1.0 | 初始创建 - 平台面任务分解（7 任务 / 5 波次） | 2026-07-13 | SDDU Tasks Agent |
