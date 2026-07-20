# 技术规划：嵌入能力开放面

**Feature ID**: EMBED-OPEN-001  
**规划版本**: v1.3  
**创建日期**: 2026-07-13  
**规划作者**: SDDU Plan Agent  
**规范版本**: spec.md v1.3

---

## 1. 架构分析

### 1.1 现有架构影响

**后端 — open-server ability 模块**：

| 组件 | 现状 | 变更 |
|------|------|------|
| `AbilityController` | 3 个接口（list / subscribe / subscribed） | 接口参数和返回不变，内部逻辑调整 |
| `AbilityServiceImpl` | 列表查询硬编码排除 type=6；订阅通过 `AbilityTypeEnum.isValidCode()` 校验；版本发布硬编码排除 type=6 | 列表：改为 `hidden` 字段过滤 + 返回 `entryUrl`/`routePath`/`aliasName`/`requireRelease`；订阅：改为 DB 校验；版本发布：改为按 `require_release` 字段过滤 |
| `AbilityVO` | 无 `entryUrl` 等字段 | 新增 `entryUrl`/`routePath`/`aliasName`/`requireRelease`/`loadType` |
| `AppAbilityDetailVO` | 无 `entryUrl` 等字段 | 新增 `entryUrl`/`routePath`/`aliasName`/`requireRelease`/`loadType` |
| `AbilityMapper` | 基础 CRUD | 可能需新增按 `abilityType` 查询方法 |
| `AbilityTypeEnum` | 7 个硬编码常量 | **不变**（只用于预置类型本地化展示，不用于校验） |

**前端 — wecodesite Capabilities 页**：

| 组件 | 现状 | 变更 |
|------|------|------|
| `Capabilities.jsx` | 列表: 硬编码过滤 type=6；配置页: 占位文本 | 列表: 使用后端 `hidden` 字段；配置页: 加载嵌入子应用 |
| `constants.js` | `ABILITY_TYPE_MAP` (7种) + `ABILITY_SCENE_MAP` (1个场景) | 保留，只作为预设类型兜底 |
| `microApps.js` | 静态注册 4 个子应用，无能力相关 | 不变（配置页使用运行时动态加载） |
| `thunk.js` | 3 个 API 调用函数 | 不变（后端 API 保持向后兼容） |

### 1.2 新增/修改组件

| 组件 | 变更类型 | 说明 |
|------|---------|------|
| `AbilityVO.entryUrl` / `routePath` / `aliasName` / `requireRelease` / `loadType` | 修改 | 新增字段；`frontendEntryUrl` 改为 `entryUrl` |
| `AppAbilityDetailVO.entryUrl` / `routePath` / `aliasName` / `requireRelease` / `loadType` | 修改 | 新增字段；`frontendEntryUrl` 改为 `entryUrl` |
| `AbilityServiceImpl.getAbilityList()` | 修改 | type=6 硬编码排除 → `hidden` 字段过滤；新增 `entryUrl`/`routePath`/`aliasName`/`requireRelease` |
| `AbilityServiceImpl.addAbility()` | 修改 | 枚举校验 → DB 校验 |
| `AbilityServiceImpl.getSubscribedAbilities()` | 修改 | 新增 `entryUrl`/`routePath`/`aliasName`/`requireRelease` |
| `AbilityServiceImpl.autoSubscribeAfterAbility()` | 修改 | 空实现 → 打日志 |
| `VersionServiceImpl.createVersion()` | 修改 | 硬编码排除 type=6 → 按 `require_release` 字段过滤 |
| `Capabilities.jsx` | 修改 | type=6 过滤逻辑去掉；配置页加载子应用 |
| 动态子应用加载模块 | 新增 | 在配置页组件内运行时 `loadMicroApp` |
| `ABILITY_SCENE_MAP` | 修改（可选） | 扩展场景分组或新增"其他"场景 |

### 1.3 依赖关系

```mermaid
graph TB
    subgraph OpenServer["open-server ability 模块（修改）"]
        AbilityController["AbilityController<br/>(不变)"]
        AbilityService["AbilityServiceImpl<br/>(列表/订阅/已订阅/自动桥接)"]
        AbilityMapper["AbilityMapper<br/>(不变)"]
    end

    subgraph WeCodeSite["wecodesite（修改）"]
        Capabilities["Capabilities.jsx<br/>(列表/配置页)"]
        DynamicSubApp["动态子应用加载<br/>(新增)"]
        MicroApps["microApps.js<br/>(不变)"]
        Constants["constants.js<br/>(不变)"]
    end

    subgraph Platform["平台面产物"]
        AbilityTable["openplatform_ability_t<br/>(新增 entry_url, hidden, route_path, alias_name, require_release)"]
        AdminCRUD["AdminAbilityController<br/>(平台面新增)"]
    end

    AbilityController --> AbilityService
    AbilityService --> AbilityMapper
    AbilityMapper --> AbilityTable
    AdminCRUD --> AbilityTable
    
    Capabilities --> DynamicSubApp
    Capabilities -.->|回退兜底| Constants

    style AbilityService fill:#e1f5e1,stroke:#2e7d32
    style Capabilities fill:#e1f5e1,stroke:#2e7d32
    style DynamicSubApp fill:#e1f5e1,stroke:#2e7d32
    style AbilityTable fill:#fff3cd,stroke:#f9a825
```

## 2. 数据库设计

开放面**不新增数据库变更**。平台面已负责 `openplatform_ability_t` 新增 `entry_url`、`hidden`、`route_path`、`alias_name`、`require_release` 字段。开放面直接读取平台面写入的数据，无需独立迁移。

## 3. API设计

### 3.1 设计规范

**基础路径**：`/service/open/v2/ability`

**认证方式**：复用 open-server 现有 API 认证体系。

**响应格式**：复用 open-server 现有 `ApiResponse` 信封：

```json
{ "code": "200", "messageZh": "操作成功", "messageEn": "Success", "data": { ... }, "page": null }
```

**变更类型说明**：

| 类型 | 含义 |
|------|------|
| **修改** | 接口路径和方法不变，内部逻辑或返回字段有变化 |
| **不变** | 接口完全无变化 |

### 3.2 接口清单

| # | 方法 | 路径 | 接口名称 | 对应 FR | 变更类型 |
|---|--------|------|---------|:------:|:-------:|
| 1 | GET | `/ability/list` | 查询能力列表 | FR-001 | **修改** |
| 2 | POST | `/ability` | 订阅能力 | FR-002 | **修改** |
| 3 | GET | `/ability/subscribed` | 查询已订阅列表 | FR-004 | **修改** |

> 接口 #1~#3 均已有前端调用方，必须保持向后兼容（新增字段为 optional，不删除/修改现有字段）。

### 3.3 接口详细定义

---

#### #1 查询能力列表（修改）

`GET /service/open/v2/ability/list`

**请求参数**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| appId | string | ✅ | 应用 ID |

> 其余参数不变。

**响应变更**

| 变更 | 说明 |
|------|------|
| ✅ 新增 `entryUrl` (string) | 进入地址，有则返回，无则返回 null |
| ✅ 新增 `routePath` (string) | 路由路径 |
| ✅ 新增 `aliasName` (string) | 别名 |
| ✅ 新增 `requireRelease` (int) | 是否需要版本发布才生效（0=即时，1=需发布） |
| ✅ 新增 `loadType` (int) | 加载类型（1=路由加载，2=微前端加载） |
| ✅ 过滤逻辑变更 | 不再硬编码排除 type=6，改为按 `hidden=1` 过滤 |
| ✅ 自定义类型 | 自定义类型（≥100）正常返回，不受 `AbilityTypeEnum` 限制 |

**响应体 `data[]`**（现有字段 + 新增字段）

| 字段 | 类型 | 说明 | 变更 |
|------|------|------|:----:|
| abilityType | int | 能力编码 | — |
| nameCn | string | 中文名 | — |
| nameEn | string | 英文名 | — |
| descCn | string | 中文描述 | — |
| descEn | string | 英文描述 | — |
| iconUrl | string | 图标 URL | — |
| diagramUrl | string | 示意图 URL | — |
| subscribed | boolean | 已订阅标记 | — |
| orderNum | int | 排序号 | — |
| entryUrl | string | 进入地址 | **新增** |
| routePath | string | 路由路径 | **新增** |
| aliasName | string | 别名 | **新增** |
| requireRelease | int | 是否需要版本发布才生效 | **新增** |
| loadType | int | 加载类型（1=路由加载，2=微前端加载） | **新增** |

**数据流**：

```mermaid
sequenceDiagram
    participant WECODE as wecodesite
    participant OPeN as open-server
    participant DB as openplatform_ability_t

    WECODE->>OPeN: GET /ability/list?appId=X
    OPeN->>DB: 查询所有 ability（hidden=0）
    OPeN->>DB: 查询应用的已订阅列表（标记订阅状态）
    DB-->>OPeN: 结果
    OPeN-->>WECODE: 列表（含 entryUrl/routePath/aliasName/requireRelease）
    WECODE->>WECODE: 渲染（预设回退常量，自定义用后端数据）
```

---

#### #2 订阅能力（修改）

`POST /service/open/v2/ability`

**请求参数**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| appId | string | ✅ | 应用 ID（Query参数） |

**请求体**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| abilityType | int | ✅ | 能力编码 |

**逻辑变更**

| 变更 | 说明 |
|------|------|
| ✅ 校验逻辑 | 从 `AbilityTypeEnum.isValidCode()` 枚举校验 → 查询 DB 校验能力类型存在且 status=1 |
| — 重复检查 | 不变（检查 `app_ability_relation_t` 是否已订阅） |
| — 关联插入 | 不变（写入 `app_ability_relation_t`） |
| — 自动桥接 | 从空实现 → 打日志（FR-003） |

**数据流**：

```mermaid
sequenceDiagram
    participant WECODE as wecodesite
    participant OPeN as open-server
    participant DB as openplatform_ability_t / app_ability_relation_t

    WECODE->>OPeN: POST /ability?appId=X { abilityType }
    OPeN->>DB: 校验 ability_t 存在且 status=1（取代枚举校验）
    OPeN->>DB: 检查 app_ability_relation_t 是否已订阅（不变）
    OPeN->>DB: 插入订阅关联记录（不变）
    OPeN->>OPeN: 触发 autoSubscribeAfterAbility（打日志）
    OPeN-->>WECODE: 订阅成功
```

---

#### #3 查询已订阅列表（修改）

`GET /service/open/v2/ability/subscribed`

**请求参数**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| appId | string | ✅ | 应用 ID |

**响应变更**

| 变更 | 说明 |
|------|------|
| ✅ 新增 `entryUrl` (string) | 进入地址 |
| ✅ 新增 `routePath` (string) | 路由路径 |
| ✅ 新增 `aliasName` (string) | 别名 |
| ✅ 新增 `requireRelease` (int) | 是否需要版本发布才生效 |
| ✅ 新增 `loadType` (int) | 加载类型（1=路由加载，2=微前端加载） |
| ✅ 过滤逻辑变更 | 不再硬编码排除 type=6 |

**响应体 `data[]`**

| 字段 | 类型 | 说明 | 变更 |
|------|------|------|:----:|
| id | string | 订阅记录 ID | — |
| abilityType | int | 能力编码 | — |
| nameCn | string | 中文名 | — |
| nameEn | string | 英文名 | — |
| iconUrl | string | 图标 URL | — |
| orderNum | int | 排序号 | — |
| entryUrl | string | 进入地址 | **新增** |
| routePath | string | 路由路径 | **新增** |
| aliasName | string | 别名 | **新增** |
| requireRelease | int | 是否需要版本发布才生效 | **新增** |
| loadType | int | 加载类型（1=路由加载，2=微前端加载） | **新增** |

### 3.4 前端变更说明

#### FR-101 动态能力目录

**Capabilities.jsx** 修改：

| 旧逻辑 | 新逻辑 |
|--------|--------|
| 列表渲染依赖 `ABILITY_TYPE_MAP` 常量获取中文名/图标 | 自定义类型直接使用后端返回的 `nameCn`/`iconUrl` |
| 硬编码过滤 `abilityType !== 6` | 使用后端 `hidden` 字段（后端已过滤） |
| 预设类型中文名从常量取 | 预设类型可回退到常量作为兜底 |

#### FR-102 配置页嵌入子应用

**Capabilities.jsx** 配置视图修改：

| 旧逻辑 | 新逻辑 |
|--------|--------|
| 配置页展示占位文本"该能力已添加，配置页面由能力方提供" | 根据 `loadType` 分支：1=路由跳转，2=微前端动态加载 |
| 无微前端加载 | loadType=2 时使用 QianKun `loadMicroApp` API 动态加载子应用 |
| - | loadType=1 时仅使用 routePath 做路由跳转 |
| - | 向子应用传递上下文：appId、abilityType、nameCn |

**交互流程**（loadType=2 时）：

```mermaid
sequenceDiagram
    participant User as 应用方用户
    participant WECODE as wecodesite
    participant SubApp as 微前端子应用

    User->>WECODE: 点击已订阅能力的"配置"按钮
    WECODE->>WECODE: 读取 loadType
    alt loadType === 2
        WECODE->>WECODE: 读取 entryUrl
        WECODE->>SubApp: 运行时加载子应用<br/>传递 { appId, abilityType, nameCn }
        SubApp-->>WECODE: 渲染配置UI
        User->>SubApp: 配置操作
        User->>WECODE: 切换/退出配置
        WECODE->>SubApp: 卸载子应用
    else loadType === 1
        WECODE->>WECODE: 根据 routePath 路由跳转
    end
```

### 3.5 VersionServiceImpl 硬编码改造

**背景**：现有 `VersionServiceImpl.createVersion()` 第173行硬编码排除 `GROUP_JOIN_NOTIFICATION`（type=6）：

```java
// 旧逻辑：硬编码排除 type=6
.filter(r -> !Objects.equals(r.getAbilityType(), AbilityTypeEnum.GROUP_JOIN_NOTIFICATION.getCode()))
```

**改造内容**：将硬编码逻辑改为按 `require_release` 字段过滤：

```java
// 新逻辑：按 require_release 字段过滤
// 仅将 require_release=1 的能力纳入版本发布检查
.filter(r -> Boolean.TRUE.equals(r.getRequireRelease()))
```

**改造位置**：`open-server/.../version/service/impl/VersionServiceImpl.java`

**影响**：
- type=6（应用入群通知）的 `require_release` 默认值为 0（即时生效），行为与改造前一致
- 自定义能力通过平台面设置 `require_release` 控制是否需要版本发布
- 现有 `VersionServiceImpl` 其他逻辑不变

## 4. 方案对比

### 方案 A：增量修改现有 ability 模块（推荐）

**描述**：在现有 AbilityController/AbilityServiceImpl 基础上做最小修改，保持接口向后兼容。

| 维度 | 评价 |
|------|------|
| 优点 | 改动最小；前端兼容（新增字段为 optional）；现有订阅流程不变；不需要新接口 |
| 缺点 | 需要修改现有类，但不影响已有功能 |
| 工作量评估 | 后端 2 天 + 前端 3 天 |

### 方案 B：新建 AbilityV2 模块

**描述**：新建 AbilityV2Controller + AbilityV2Service，废弃旧接口。

| 维度 | 评价 |
|------|------|
| 优点 | 新旧接口隔离，不影响现有调用方 |
| 缺点 | 大量重复代码；数据源同一套，增加维护成本；前端需迁移 |
| 工作量评估 | 后端 5 天 + 前端 5 天 |

## 5. 推荐方案

**选择方案 A**：增量修改现有 ability 模块。

理由：
1. 后端变更范围可控：只需修改 `AbilityServiceImpl` 中两三处逻辑 + 两个 VO 加字段
2. 接口向后兼容：新增 `entryUrl`/`routePath`/`aliasName`/`requireRelease` 为 optional 字段，不破坏现有前端
3. 前端配置页嵌入为新增功能（运行时动态加载），不修改现有导航和路由
4. 前置依赖（平台面 DB migration）后，开放面直接读取即可

## 6. 文件影响分析

### 修改文件

| 文件 | 修改内容 |
|------|---------|
| `open-server/.../ability/vo/AbilityVO.java` | 新增 `entryUrl`/`routePath`/`aliasName`/`requireRelease`/`loadType` 字段 |
| `open-server/.../ability/vo/AppAbilityDetailVO.java` | 新增 `entryUrl`/`routePath`/`aliasName`/`requireRelease`/`loadType` 字段 |
| `open-server/.../ability/service/impl/AbilityServiceImpl.java` | 列表: `hidden` 过滤 + `entryUrl`/`routePath`/`aliasName`/`requireRelease`；订阅: DB 校验；自动桥接: 日志 |
| `open-server/.../version/service/impl/VersionServiceImpl.java` | 硬编码排除 type=6 改为按 `require_release` 字段过滤 |
| `open-server/.../ability/controller/AbilityController.java` | 可能小调整（如接口注释） |
| `wecodesite/.../pages/Capabilities/Capabilities.jsx` | 去掉 type=6 硬编码；配置页加载子应用 |
| `wecodesite/.../utils/constants.js` | 可能扩展 `ABILITY_SCENE_MAP` |

### 新增文件

| 文件 | 说明 |
|------|------|
| `wecodesite/.../pages/Capabilities/EmbeddedSubApp.jsx` | 配置页嵌入子应用组件（运行时动态加载） |

## 7. 风险评估

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| 现有前端已硬编码过滤 type=6 | 升级后老版本前端可能仍显示隐藏类型 | 后端控制 `hidden` 字段 + 前端升级同步去掉前端硬编码 |
| 微前端动态加载方案不成熟 | 配置页无法正常加载子应用 | 兜底：无 `entryUrl` 的能力仍展示占位文本 |
| `autoSubscribeAfterAbility` 在现有流程中已有调用 | 日志不影响功能 | 仅加日志，不改逻辑 |

## 8. ADR

### ADR-001: 增量修改现有 ability 模块而非新建模块

**状态**: ACCEPTED

**背景**：
- 现有 ability 模块有 3 个接口，功能与开放面需求高度重合
- 变更范围可被现有类结构容纳

**决策**：
直接修改 `AbilityServiceImpl` 的现有方法（`getAbilityList()`、`addAbility()`、`getSubscribedAbilities()`），不新建 Controller 或 Service。VO 类新增 optional 字段 `entryUrl`、`routePath`、`aliasName`、`requireRelease`、`loadType`。

**后果**：
- 正面：改动小、代码复用、兼容现有调用方
- 负面：旧代码中的 type=6 过滤需替换为 `hidden` 字段，需确保逻辑等价

### ADR-002: 配置页动态子应用使用运行时 `loadMicroApp`

**状态**: ACCEPTED

**背景**：
- 现有 `microApps.js` 使用静态注册方式
- 能力配置页的子应用 `entryUrl`/`routePath`/`aliasName` 由平台面录入，运行时才能确定

**决策**：
在 Capabilities 页配置视图组件内，使用 QianKun 的 `loadMicroApp` API 在运行时动态加载子应用。不修改 `microApps.js`。

**后果**：
- 正面：不污染全局注册列表，按需加载，切换能力时自动 unload
- 负面：依赖 wecodesite 的 QianKun 版本是否支持 `loadMicroApp`

---

## 9. 产物审查策略

| 审查产物 | 审查基准 |
|---------|---------|
| build.md | spec.md（规范基准） |
| AbilityServiceImpl.java | 列表 hidden 过滤逻辑、订阅 DB 校验逻辑 |
| AbilityVO / AppAbilityDetailVO | 新增字段的序列化正确性 |
| Capabilities.jsx | 配置页嵌入子应用、数据驱动渲染 |

## 10. 产物验证策略

| 验证产物 | 验证基准 |
|---------|---------|
| 能力列表查询 | hidden=1 不出现在列表；自定义类型正常展示；entryUrl/routePath/aliasName/requireRelease/loadType 正常返回 |
| 能力订阅 | 自定义类型可通过校验正常订阅 |
| 已订阅列表 | 新增 entryUrl/routePath/aliasName/requireRelease/loadType 字段 |
| 配置页 | loadType=1 路由跳转；loadType=2 加载子应用；无 entryUrl 展示占位 |

## 11. 任务分解与分支管理

> 按接口维度将 spec 的 FR-001~005 + ADR-004 映射为 4 个后端任务。前端 FR-101~103 由独立流程负责，不在本文档跟踪。详细定义见 [tasks.md](./tasks.md)。

### 11.1 任务索引

| # | 任务 | 接口/范围 | FR | 复杂度 | 服务 | 依赖 |
|---|------|----------|:--:|:--:|------|------|
| 1 | 能力列表增强 | `GET /ability/list` | FR-001, FR-005 | M | open-server | 无 |
| 2 | 能力订阅增强 + 自动桥接 | `POST /ability` | FR-002, FR-003 | M | open-server | 无 |
| 3 | 已订阅列表增强 | `GET /ability/subscribed` | FR-004 | S | open-server | 无 |
| 4 | VersionServiceImpl 改造 | `createVersion()` 过滤逻辑 | ADR-004 | S | open-server | 无 |

> ⚠️ **前端独立实现**：FR-101（动态能力目录）、FR-102（配置页嵌入子应用）、FR-103（场景分组动态化）由前端同事在 wecodesite 独立流程中实现，不在本 Plan 的任务分解和代码管理中跟踪。后端 TASK-001~004 完成后，API 契约即对前端可用。

### 11.2 依赖拓扑

**后端（本 Plan 执行，TASK-001~004）**：四个接口各自独立修改，无代码依赖，可并行开发。

```mermaid
graph LR
    T1["TASK-001 [M]<br/>GET /ability/list<br/>列表增强 + hidden控制"] 
    T2["TASK-002 [M]<br/>POST /ability<br/>订阅增强 + 自动桥接"]
    T3["TASK-003 [S]<br/>GET /ability/subscribed<br/>已订阅列表增强"]
    T4["TASK-004 [S]<br/>VersionServiceImpl<br/>硬编码→require_release"]
```

> 前端 FR-101~103 由独立流程负责，后端 API（TASK-001/003）就绪后即可启动。

### 11.3 拆分说明

**为什么按接口维度？**

开放面是"增量修改现有代码"而非"从零构建 CRUD"。4 个后端接口各自修改 `AbilityServiceImpl` 的不同方法，代码独立、无编译依赖。细拆为 VO字段/桥接/校验等子任务反而增加管理成本，一个接口一个任务更符合实际开发单元。

**与平台面的对比**：

| 维度 | 平台面 | 开放面 |
|------|--------|--------|
| 代码模式 | 新建模块 | 增量修改 |
| 任务粒度 | 细拆（接口+前端各一 Task） | 按接口聚合（含 VO/校验/桥接） |
| 后端 Task 数 | 6 个（上传/列表/新增/编辑/删除 + 模块重组） | 4 个（3 API + 1 跨模块） |
| 并行度 | 严格串行 | 4 个后端可并行 |
| 前端 | 在本 Plan 执行链中 | 独立流程，不在本文档跟踪 |

### 11.4 执行指南

**后端**：TASK-001~004 无相互依赖，可并行开发。推荐顺序：先 ①（列表，含 VO 字段新增，为其他接口打好 VO 基础）→ ②③④ 并行。

**启动命令**：`@sddu-build TASK-001`

**前端**：后端 TASK-001/003 完成后，API 契约就绪，前端同事在独立流程中实现 FR-101~103。

**完成标准**：实现文件 + 测试文件全部通过。后端 Java 单测 + Python 集成测试；前端 Playwright E2E。

---

## 12. 代码管理与原子化合并方案

> 本章解决"4 个后端 Task 如何以原子方式合入主干 main"的问题。前端 FR-101~103 走独立流程，不在本方案覆盖范围。

### 12.1 方案概述

**核心诉求**：每个后端 Task（TASK-001~004）独立分支、独立审查、独立合入、独立回滚。

**并行分支模式**：4 个 Task 各自修改 `AbilityServiceImpl` 的不同方法（或不同模块），无代码依赖。TASK-001 基于 main 创建（含 VO 字段新增，为公共基础）；TASK-002~004 基于 TASK-001 创建，并行开发，串行合入 main（001→002→003→004）。

**核心优势**：开发可并行（4 个分支同时推进）+ 合入原子化（顺序 merge 自动提纯）+ 独立回滚（各 Task 独立 merge commit）。

**与平台面的差异**：开放面 4 个后端 Task 可并行开发（vs 平台面 11 个 Task 严格串行），分支管理从"前驱接力"简化为"公共基础 + 并行分支"。

### 12.2 分支策略

**命名规范**：
```
feature/embedding-open-<NN>-<short-desc>
```
- `NN`: 01-04，与 Task 编号对齐
- 全部小写，单词间用连字符 `-` 连接

**4 个后端 Task 分支清单**：

| # | Task | 分支名 | 基于 | 说明 |
|---|------|--------|------|------|
| 1 | 能力列表增强 | `feature/embedding-open-01-list-api` | main | 含 VO 字段新增，为公共基础 |
| 2 | 能力订阅增强 | `feature/embedding-open-02-subscribe-api` | 01 | 含自动桥接 |
| 3 | 已订阅列表增强 | `feature/embedding-open-03-subscribed-api` | 01 | — |
| 4 | VersionServiceImpl | `feature/embedding-open-04-version-service` | 01 | version 模块，与 ②③ 无冲突 |

> TASK-002~004 均基于 TASK-001（公共 VO 基础），可并行开发。合入按 001→002→003→004 顺序。

### 12.3 标签策略

**基准标签**：开发开始前在 main HEAD 打 `embed-open-base`（annotated），作为整个开放面所有子Feature 变更的固定基准点。

两层语义：

| 标签 | 时机 | 位置 | 含义 |
|------|------|------|------|
| `embed-open-NNN-v1` | 子Feature 验证通过、准备合入 | Feature 分支 HEAD | "可以合了" |
| `embed-open-NNN-merged` | 合入 main 完成后 | main 上的 merge commit | "已合入主干" |

### 12.4 差异获取

| 场景 | 命令 | 说明 |
|------|------|------|
| 单子Feature 增量（审查用） | `git diff embed-open-(N-1)-v1..embed-open-N-v1` | 001 用 `git diff embed-open-base..embed-open-001-v1` |
| 开放面后端累计差异 | `git diff embed-open-base..embed-open-004-merged` | 后端 4 个 Task 总变更 |

> ⚠️ 接力分支下 `git diff main...feature/embedding-open-NN-xxx` 显示累计变更，**不是**单子Feature 增量。审查时必须用相邻标签 diff。

### 12.5 合入与回滚

**合入方式**：`merge --no-ff`（每个子Feature 有独立 merge commit，revert 可精准定位）。

**顺序合入自动提纯**：合入 002 时，git 以 001 合入点为 merge-base，自动只合入 002 增量。

**回滚**：`git revert -m 1 <merge-commit-hash>` 精准回滚单个子Feature。

### 12.6 命令速查表

| 操作 | 命令 |
|------|------|
| 开接力分支 | `git checkout feature/embedding-open-NN-xxx && git checkout -b feature/embedding-open-(N+1)-yyy && git push -u origin feature/embedding-open-(N+1)-yyy` |
| 打验证标签 | `git tag -a embed-open-NNN-v1 -m "msg" feature/branch && git push origin embed-open-NNN-v1` |
| 打合入标签 | `git tag -a embed-open-NNN-merged -m "msg" && git push origin embed-open-NNN-merged` |
| 查看单子Feature 增量 | `git diff embed-open-(N-1)-v1..embed-open-N-v1 --stat` |
| 查看累计变更 | `git diff embed-open-base..embed-open-004-merged --stat` |
| 合入 main | `git checkout main && git pull && git merge --no-ff feature/embedding-open-NN-xxx -m "feat(embedding-open): TASK-NNN" && git push origin main` |
| 回滚子Feature | `git revert -m 1 <merge-commit-hash> && git push origin main` |

---

## 修订记录

| 版本 | 变更说明 | 日期 | 修订人 |
|------|---------|------|--------|
| v1.0 | 初始创建 — 开放面技术规划 | 2026-07-13 | SDDU Plan Agent |
| v1.1 | 对齐平台面格式：独立数据库设计+API设计章节；接口详细定义含设计规范/接口清单/请求响应/数据流 | 2026-07-13 | SDDU Plan Agent |
| v1.2 | DB 字段更新：新增 route_path/alias_name/require_release 字段；frontendEntryUrl 改为 entryUrl；新增 §3.5 VersionServiceImpl 硬编码改造（按 require_release 过滤）；文件影响分析同步更新 | 2026-07-16 | SDDU Plan Agent |
| v1.3 | 同步平台面 load_type 字段：§1.1/§1.2 VO 扩展加 loadType；§3.3 接口响应加 loadType；§3.4 FR-102 配置页嵌入增加 loadType 分支（路由跳转 vs QianKun 加载）；§6/§8/§10 同步更新 | 2026-07-17 | SDDU Plan Agent |
| v1.4 | 新增 §11 任务分解（9 任务/串行主链+TASK-006旁路）+ §12 代码管理与原子化合并方案（前驱分支接力模式、双层标签、差异获取、merge --no-ff 合入/回滚） | 2026-07-20 | SDDU 路由调度专家 |
| v1.5 | 前后端分离：TASK-007~009（前端）从执行链剥离，标注为独立流程；§11/§12 调整为仅覆盖后端 TASK-001~006 | 2026-07-20 | SDDU 路由调度专家 |
| v1.6 | §11 重构为按接口维度拆分（4 个后端 Task，可并行）；前端 FR-101~103 移出本文档，标注为独立流程 | 2026-07-20 | SDDU 路由调度专家 |
