# 技术规划：嵌入能力平台面

**Feature ID**: EMBED-PLATFORM-001  
**规划版本**: v1.0  
**创建日期**: 2026-07-13  
**规划作者**: SDDU Plan Agent  
**规范版本**: spec.md v1.0

---

## 1. 架构分析

### 1.1 现有架构影响

**当前 ability 模块**（open-server）：

| 组件 | 现状 | 影响 |
|------|------|------|
| `AbilityTypeEnum` | 7 个硬编码常量 | 保持不动，新增自定义类型通过 DB 存储 |
| `AbilityController` | 3 个接口（list / subscribe / subscribed） | 不变，平台面新增独立 AdminController |
| `AbilityMapper` / `AbilityPropertyMapper` | 已有 CRUD 映射 | 平台面复用现有映射 |
| `Ability` 实体 | 映射 `openplatform_ability_t` | 新增字段 `frontendEntryUrl`、`hidden` |
| `AbilityProperty` 实体 | 映射 `openplatform_ability_p_t`（图标/示意图） | 不动，继续复用 |
| `AbilitySnapshotLoader` | 启动时加载 ability 到缓存 | 新增字段不影响 |

**关键决策**：admin CRUD controller 放在 **market-server** 还是 **open-server**？

| 选项 | 说明 |
|------|------|
| 放在 open-server | 数据表 `openplatform_ability_t` 在 open-server 的 schema 中，复用现有 Mapper/Entity/Service |
| 放在 market-server | spec 指定"服务端：market-server"，但需跨服务访问 ability 数据 |

**决策**：AdminController 放在 **open-server** 中扩展，market-web 对接。数据直接写入 open-server 的已有表 `openplatform_ability_t`/`openplatform_ability_p_t`，由开放面直接读取，符合 NFR-003"写入即对开放面可见"的要求。

### 1.2 新增组件

| 组件 | 说明 | 所属模块 |
|------|------|---------|
| `AdminAbilityController` | 管理面控制器（列表/创建/编辑/删除） | open-server ability |
| `AdminAbilityService` / `AdminAbilityServiceImpl` | 管理面业务逻辑 | open-server ability |
| `AdminAbilityListRequest` | 列表请求 DTO（分页 + 模糊搜索） | open-server ability |
| `AdminAbilityVO` | 列表响应 VO（含新增字段） | open-server ability |
| `AdminAbilityCreateRequest` | 创建请求 DTO | open-server ability |
| `AdminAbilityUpdateRequest` | 编辑请求 DTO | open-server ability |
| Flyway migration 文件 | `openplatform_ability_t` 新增 `frontend_entry_url` / `hidden` 字段 | open-server DB |
| 前端页面（market-web） | 能力目录管理页面：列表页 + 创建/编辑表单 | market-web |

### 1.3 依赖关系图

```mermaid
graph TB
    subgraph OpenServer["open-server ability 模块"]
        direction TB
        AbilityController["AbilityController<br/>(现有: 列表/订阅/已订阅)"]
        AdminController["AdminAbilityController<br/>(新增: 管理面 CRUD)"]
        AbilityService["AbilityService<br/>(现有)"]
        AdminService["AdminAbilityService<br/>(新增)"]
        AbilityMapper["AbilityMapper (现有)"]
        AbilityPropertyMapper["AbilityPropertyMapper (现有)"]
    end

    subgraph MarketWeb["market-web"]
        AdminPages["能力目录管理页面<br/>(新增)"]
    end

    subgraph DB["数据库"]
        T1["openplatform_ability_t<br/>(新增 frontend_entry_url, hidden)"]
        T2["openplatform_ability_p_t<br/>(图标/示意图, 不变)"]
    end

    AdminPages --> AdminController
    AbilityController --> AbilityService
    AdminController --> AdminService
    AdminService --> AbilityMapper
    AdminService --> AbilityPropertyMapper
    AbilityMapper --> T1
    AbilityPropertyMapper --> T2

    style AdminController fill:#e1f5e1,stroke:#2e7d32
    style AdminService fill:#e1f5e1,stroke:#2e7d32
    style AdminPages fill:#e1f5e1,stroke:#2e7d32
    style T1 fill:#fff3cd,stroke:#f9a825
```

## 2. 数据库设计

### 2.1 openplatform_ability_t（主表）

复用现有表，新增字段：

| 字段名 | 类型 | 说明 | 约束 |
|--------|------|------|------|
| `frontend_entry_url` | VARCHAR(512) | 微前端子应用入口 URL | nullable |
| `hidden` | TINYINT(1) | 是否在开放面展示（0=展示，1=隐藏） | 默认 0 |

> 其余字段（`ability_name_cn`, `ability_name_en`, `ability_desc_cn`, `ability_desc_en`, `ability_type`, `order_num`, `status` 等）保持现有结构不变。

### 2.2 openplatform_ability_p_t（属性表）

**不变**。继续存储图标（`property_name = 'icon'`）和示意图（`property_name = 'illustration'`）。

### 2.3 迁移版本

- 新增 Flyway migration: `V4__add_ability_admin_fields.sql`
- 迁移内容：`ALTER TABLE openplatform_ability_t ADD COLUMN frontend_entry_url VARCHAR(512) DEFAULT NULL COMMENT '前端入口URL'`, `ADD COLUMN hidden TINYINT(1) DEFAULT 0 COMMENT '是否在开放面展示'`

## 3. API设计

### 3.1 设计规范

**基础路径**：`/service/open/v2/ability/admin`

**认证方式**：管理面接口复用 open-server 现有 Cookie/SSO 登录态，接口内校验当前用户角色是否为平台管理员（NFR-001）。

**响应格式**：统一使用 open-server 现有 `ApiResponse` 信封：

```json
// 成功
{ "code": "200", "messageZh": "操作成功", "messageEn": "Success", "data": { ... }, "page": null }

// 分页
{ "code": "200", "messageZh": "查询成功", "messageEn": "Success", "data": [ ... ], "page": { "curPage": 1, "pageSize": 20, "total": 123 } }

// 错误
{ "code": "400", "messageZh": "参数错误", "messageEn": "Bad Request", "data": null, "page": null }
```

**错误码**：

| 错误码 | 说明 |
|--------|------|
| 200 | 成功 |
| 400 | 参数错误/校验失败 |
| 403 | 无权限（非管理员） |
| 404 | 资源不存在 |
| 409 | 状态冲突（编码重复、有订阅等） |

**字段命名**：驼峰命名（camelCase），与现有 ability 模块保持一致。

| 数据库列名 | API 字段名 |
|-----------|----------|
| `ability_type` | `abilityType` |
| `name_cn` | `nameCn` |
| `name_en` | `nameEn` |
| `ability_desc_cn` | `descCn` |
| `desc_en` | `descEn` |
| `order_num` | `orderNum` |
| `frontend_entry_url` | `frontendEntryUrl` |

### 3.2 接口清单

| # | 方法 | 路径 | 接口名称 | 对应 FR | 说明 |
|---|--------|------|---------|:------:|------|
| 1 | GET | `/ability/admin/list` | 查询能力列表 | FR-001 | 分页查询，支持关键字搜索 |
| 2 | POST | `/ability/admin` | 创建能力 | FR-002 | 创建新的能力类型 |
| 3 | PUT | `/ability/admin/{id}` | 更新能力 | FR-003 | 更新能力信息，abilityType 不可修改 |
| 4 | DELETE | `/ability/admin/{id}` | 删除能力 | FR-004 | 删除（含订阅检查） |

### 3.3 接口详细定义

---

#### #1 查询能力列表

`GET /service/open/v2/ability/admin/list`

**查询参数**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| curPage | int | ❌ | 页码，默认 1 |
| pageSize | int | ❌ | 每页数量，默认 20，最大 100 |
| keyword | string | ❌ | 按中文名/英文名模糊搜索 |
| sortField | string | ❌ | 排序字段，默认 `orderNum` |
| sortOrder | string | ❌ | 排序方向，`asc` / `desc`，默认 `asc` |

**响应体 `data[]`**

| 字段 | 类型 | 说明 |
|------|------|------|
| abilityType | int | 能力编码 |
| nameCn | string | 中文名 |
| nameEn | string | 英文名 |
| descCn | string | 中文描述 |
| descEn | string | 英文描述 |
| iconUrl | string | 图标 URL |
| diagramUrl | string | 示意图 URL（缩略图） |
| orderNum | int | 排序号 |
| frontendEntryUrl | string | 前端入口 URL |
| hidden | int | 是否在开放面展示（0=展示，1=隐藏） |
| createTime | string | 创建时间 |
| updateBy | string | 更新人 |
| updateTime | string | 更新时间 |

**数据流**：

```mermaid
sequenceDiagram
    participant Admin as 平台管理员
    participant Web as market-web
    participant AdminCtrl as AdminAbilityController
    participant DB as openplatform_ability_t / _p_t

    Admin->>Web: 打开能力目录页面
    Web->>AdminCtrl: GET /ability/admin/list?curPage=1&keyword=群置顶
    AdminCtrl->>DB: 分页查询（按排序号升序）
    AdminCtrl->>DB: 关联 ability_p_t 获取图标/示意图 URL
    DB-->>AdminCtrl: 结果
    AdminCtrl-->>Web: 分页 VO
    Web-->>Admin: 能力列表（分页、搜索）
```

---

#### #2 创建能力

`POST /service/open/v2/ability/admin`

**请求体**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| abilityType | int | ✅ | 能力编码（≥100），需唯一 |
| nameCn | string | ✅ | 中文名，最长 64 字符 |
| nameEn | string | ✅ | 英文名，最长 128 字符 |
| descCn | string | ❌ | 中文描述，最长 512 字符 |
| descEn | string | ❌ | 英文描述，最长 512 字符 |
| iconUrl | string | ❌ | 图标文件上传返回的 URL |
| diagramUrl | string | ❌ | 示意图文件上传返回的 URL |
| orderNum | int | ❌ | 排序号，默认 0 |
| frontendEntryUrl | string | ❌ | 前端入口 URL（http/https 协议） |
| hidden | int | ❌ | 是否隐藏（0=展示，1=隐藏），默认 0 |

**响应体 `data`**

| 字段 | 类型 | 说明 |
|------|------|------|
| abilityType | int | 创建的能力编码 |
| nameCn | string | 中文名 |
| createTime | string | 创建时间 |

**错误响应**

| code | 说明 |
|------|------|
| 400 | 参数校验失败（URL 格式、编码范围等） |
| 409 | abilityType 编码已被占用 |

**数据流**：

```mermaid
sequenceDiagram
    participant Admin as 平台管理员
    participant Web as market-web
    participant AdminCtrl as AdminAbilityController
    participant FileSvc as 文件服务
    participant DB as openplatform_ability_t / _p_t

    Admin->>Web: 填写创建表单
    Web->>AdminCtrl: POST /ability/admin { abilityType, nameCn, iconUrl, ... }
    AdminCtrl->>AdminCtrl: 校验 abilityType 唯一性
    AdminCtrl->>FileSvc: 上传图标/示意图文件（如有时）
    FileSvc-->>AdminCtrl: 返回文件 URL
    AdminCtrl->>DB: 写入 ability_t（主表）
    AdminCtrl->>DB: 写入 ability_p_t（图标/示意图）
    DB-->>AdminCtrl: 成功
    AdminCtrl-->>Web: 返回 { abilityType, nameCn, createTime }
    Web-->>Admin: 提示"创建成功"
```

---

#### #3 更新能力

`PUT /service/open/v2/ability/admin/{id}`

**路径参数**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| id | string | ✅ | 能力 ID（数据库主键） |

**请求体**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| nameCn | string | ❌ | 中文名 |
| nameEn | string | ❌ | 英文名 |
| descCn | string | ❌ | 中文描述 |
| descEn | string | ❌ | 英文描述 |
| iconUrl | string | ❌ | 图标 URL（新文件上传后替换） |
| diagramUrl | string | ❌ | 示意图 URL |
| orderNum | int | ❌ | 排序号 |
| frontendEntryUrl | string | ❌ | 前端入口 URL |
| hidden | int | ❌ | 是否隐藏 |

> 所有字段可选，仅更新传入的字段。abilityType 不可修改。

**响应体 `data`**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | string | 能力 ID |
| updateTime | string | 更新时间 |

**错误响应**

| code | 说明 |
|------|------|
| 404 | 能力不存在 |

**数据流**：

```mermaid
sequenceDiagram
    participant Admin as 平台管理员
    participant Web as market-web
    participant AdminCtrl as AdminAbilityController
    participant FileSvc as 文件服务
    participant DB as openplatform_ability_t / _p_t

    Admin->>Web: 打开编辑表单（回填已有信息）
    Admin->>Web: 修改字段
    Web->>AdminCtrl: PUT /ability/admin/{id} { nameCn, orderNum, hidden, ... }
    AdminCtrl->>FileSvc: 如有新文件则上传（替换旧文件引用）
    AdminCtrl->>DB: 更新 ability_t
    AdminCtrl->>DB: 更新 ability_p_t（如有新文件）
    DB-->>AdminCtrl: 成功
    AdminCtrl-->>Web: 返回 { id, updateTime }
    Web-->>Admin: 提示"修改成功"
```

---

#### #4 删除能力

`DELETE /service/open/v2/ability/admin/{id}`

**路径参数**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| id | string | ✅ | 能力 ID |

**响应体 `data`**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | string | 被删除的能力 ID |

**错误响应**

| code | 说明 |
|------|------|
| 404 | 能力不存在 |
| 409 | 有应用订阅该能力，无法删除 |

**数据流**：

```mermaid
sequenceDiagram
    participant Admin as 平台管理员
    participant Web as market-web
    participant AdminCtrl as AdminAbilityController
    participant DB as openplatform_ability_t / app_ability_relation_t

    Admin->>Web: 点击删除
    Web->>AdminCtrl: DELETE /ability/admin/{id}
    AdminCtrl->>DB: 检查 app_ability_relation_t 是否有订阅
    alt 有关联订阅
        DB-->>AdminCtrl: 存在 XX 条订阅
        AdminCtrl-->>Web: 409 { code: 409, messageZh: "已被 XX 个应用订阅" }
        Web-->>Admin: 展示提示，删除失败
    else 无关联订阅
        AdminCtrl->>DB: 删除 ability_t + ability_p_t
        DB-->>AdminCtrl: 成功
        AdminCtrl-->>Web: 200 { id }
        Web-->>Admin: 提示"删除成功"
    end
```

## 4. 方案对比

### 方案 A：扩展 open-server ability 模块（推荐）

**描述**：在 open-server 的 ability 模块内新增 AdminAbilityController 和 AdminAbilityService，复用现有 Mapper/Entity。

| 维度 | 评价 |
|------|------|
| 优点 | 数据表在同一 schema，无需跨服务调用；复用现有能力（fileV2Service 文件上传、AbilityMapper）；写入即对开放面可见 |
| 缺点 | 与"服务端：market-server"的 spec 表述不一致 |
| 风险 | 低——只需新增 Controller+Service，不修改现有接口 |

### 方案 B：market-server 独立模块

**描述**：在 market-server 中新建独立 controller，跨服务访问 open-server 数据或直连同一 DB。

| 维度 | 评价 |
|------|------|
| 优点 | 与 spec"服务端：market-server"一致；体现职责分离 |
| 缺点 | 需要跨服务访问或重复创建 Mapper/Entity；增加部署耦合；写入操作与现有 open-server 异步问题 |
| 风险 | 中——跨服务调用的可靠性和数据一致性问题 |

## 5. 推荐方案

**选择方案 A**：在 open-server 的 ability 模块扩展 admin 能力。

理由：
1. `openplatform_ability_t` 表在 open-server 的 schema 中，直接操作最简洁
2. 复用现有 Mapper/Entity/Service，减少重复代码
3. 开放面（open-server 自身）读取同一数据源，写入即对开放面可见（NFR-003）
4. 不影响现有订阅/列表接口

> 注：spec 中"服务端：market-server"的表述在实际实现中调整为 open-server。market-web 作为前端调用 open-server 的 admin 接口。

## 6. 文件影响分析

### 新增文件

| 文件 | 说明 |
|------|------|
| `open-server/.../ability/controller/AdminAbilityController.java` | 管理面控制器 |
| `open-server/.../ability/service/AdminAbilityService.java` | 管理面业务接口 |
| `open-server/.../ability/service/impl/AdminAbilityServiceImpl.java` | 管理面业务实现 |
| `open-server/.../ability/dto/admin/AdminAbilityListRequest.java` | 列表请求 DTO |
| `open-server/.../ability/dto/admin/AdminAbilityCreateRequest.java` | 创建请求 DTO |
| `open-server/.../ability/dto/admin/AdminAbilityUpdateRequest.java` | 编辑请求 DTO |
| `open-server/.../ability/vo/admin/AdminAbilityVO.java` | 列表响应 VO |
| `open-server/.../ability/vo/admin/AdminAbilityDetailVO.java` | 详情 VO |
| `open-server/.../db/migration/V4__add_ability_admin_fields.sql` | DB 迁移 |
| `market-web/.../pages/AbilityAdmin/` | 前端管理页面（列表/创建/编辑/删除） |

### 修改文件

| 文件 | 修改内容 |
|------|---------|
| `open-server/.../ability/entity/Ability.java` | 新增 `frontendEntryUrl`、`hidden` 字段 |
| `open-server/.../ability/mapper/AbilityMapper.java` | 可能新增管理面查询方法 |
| `open-server/.../ability/mapper/AbilityPropertyMapper.java` | 可能新增按 abilityType 查询/删除方法 |
| `market-web/.../router/config.ts` | 新增能力目录管理路由 |

## 7. 风险评估

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| abilityType 编码手动输入可能不一致 | 数据混乱 | 前端提示推荐范围（≥100）；后端校验唯一性 |
| 文件上传（图标/示意图）依赖 fileV2Service | 联调阻塞 | Mock 阶段使用固定 URL 占位 |
| DB migration 与现有表结构冲突 | 部署失败 | 新增 migration V4，命名规范避免冲突 |

## 8. ADR

### ADR-001: Admin 能力 CRUD 放在 open-server 扩展

**状态**: ACCEPTED

**背景**：
- `openplatform_ability_t` 表位于 open-server 的 schema 中
- open-server ability 模块已有 AbilityMapper、AbilityPropertyMapper、fileV2Service
- spec 要求"写入即对开放面可见"
- spec 写"服务端：market-server"但实际数据在 open-server

**决策**：
在 open-server 的 ability 模块内新增 AdminAbilityController + AdminAbilityService。market-web 作为前端直接调用 open-server 的 admin 接口。

**后果**：
- 正面：复用现有 Mapper/Entity，无需跨服务，写入即时对开放面可见
- 负面：与 spec 中"服务端：market-server"的表述不符，需在 plan 中说明实际调整

### ADR-002: abilityType 编码规则

**状态**: ACCEPTED

**背景**：
- 现有 7 种预置类型使用 1-7 编码
- 自定义类型需要手动输入编码

**决策**：
- 1-7 保留给 `AbilityTypeEnum` 预置类型
- 自定义类型推荐编码 ≥ 100，后端校验唯一性（包括预置编码）
- 创建后不可修改

**后果**：
- 正面：业务字段可读性强，不依赖自增ID
- 负面：需要人工管理编码范围，无冲突时系统保障

---

## 9. 产物审查策略

| 审查产物 | 审查基准 |
|---------|---------|
| `build.md`（代码变更清单） | spec.md（规范基准） |
| AdminAbilityController.java | 接口参数校验、错误处理、权限校验 |
| AdminAbilityService.java | 业务逻辑完整性（创建/编辑/删除/列表） |
| V4 迁移文件 | 字段类型、默认值、约束 |

## 10. 产物验证策略

| 验证产物 | 验证基准 |
|---------|---------|
| AdminAbilityController 各接口 | spec.md FR-001 ~ FR-004 验收标准 |
| 数据库迁移 | 新增字段正确性、向前兼容 |
| 前端列表页面 | 字段展示完整、分页正常 |
| 创建/编辑表单 | 字段校验、文件上传、编码唯一性 |
| 删除操作 | 有订阅时禁止删除 |

## 修订记录

| 版本 | 变更说明 | 日期 | 修订人 |
|------|---------|------|--------|
| v1.0 | 初始创建 — 平台面技术规划 | 2026-07-13 | SDDU Plan Agent |
| v1.1 | 1.3 数据流重写：按4个FR场景逐一对应独立序列图 | 2026-07-13 | SDDU Plan Agent |
| v1.2 | 结构调整：独立数据库设计+API设计章节，数据流融入接口 | 2026-07-13 | SDDU Plan Agent |
| v1.3 | API设计重写：按 plan-api.md 格式，含设计规范/接口清单/请求响应JSON示例 | 2026-07-13 | SDDU Plan Agent |
