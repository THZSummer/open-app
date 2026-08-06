# SDK 版本管理方案

> 适用范围：market-server（后端接口）+ market-web（管理后台页面）
> 文档版本：v1.0

---

## 1. 背景与目标

平台对外提供多种 **SDK**（Java SDK、JS SDK、Go SDK 等），需要对其**全生命周期**进行管控：

- **创建/上架**：登记 SDK 的一个具体版本
- **编辑**：修改版本的元数据
- **废弃**：标记版本不再推荐使用，必须填写废弃原因
- **恢复**：将已废弃版本恢复为正常状态

每个 SDK 版本需承载以下信息：

| 字段 | 是否必填 | 说明 |
|------|----------|------|
| 版本名（version_name） | 必填 | VARCHAR(64)，如 `1.0.0`（上架时强制） |
| 更新说明（update_notes） | 选填 | VARCHAR(2000) |
| 构建产物下载地址（artifact_url） | 选填 | SDK jar/zip/包地址 |
| 关联权限 ID（permission_id） | 选填 | 引用 open 面的 SDK 分类下的权限 |
| 状态（status） | 系统维护 | 见下文状态机 |
| 废弃原因（deprecate_reason） | 废弃时必填 | 5-2000 字符 |

> **关于权限关联**：open 面在权限中心新增一个 "SDK 权限" 分类；market 仅做一次**只读查询**将权限信息展示给用户供其选择，**不在 market 维护**。open 面的权限分类管理属于另一个服务，本方案不展开。

---

## 2. 模块边界

| 服务 | 职责 | 本方案是否涉及 |
|------|------|----------------|
| **open-server** | 维护权限中心数据（含 SDK 权限分类） | 否（仅数据侧，不提供接口给 market） |
| **market-server** | 维护 SDK 版本生命周期（CRUD + 废弃/恢复）；**自行提供权限查询接口直接查库** | 是（主战场） |
| **wecodesite / market-web** | 管理后台页面（列表、详情、上架、编辑表单、废弃/恢复按钮） | 是（页面端） |

**market 与 open 共用同一数据库**。market **不调用 open 的任何 HTTP 接口**，权限查询由 market 自行实现：SDK 权限分类的 `category_alias` 在 market 侧**配置化**（常量/配置文件），查询时**一条关联 SQL**（permission 表 JOIN category 表，按 alias 过滤）直接返回该分类下的启用权限。用户选择权限后只保存 `permission_id`（不缓存权限名），列表展示时如需权限名再按需实时查询。

---

## 3. 数据模型

### 3.1 新表 `openplatform_sdk_version_t`

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | BIGINT | 是 | 主键，雪花算法 |
| version_name | VARCHAR(64) | 是 | 版本名，如 `1.2.3` |
| update_notes | VARCHAR(2000) | 否 | 更新说明 |
| artifact_url | VARCHAR(512) | 否 | 构建产物下载地址（非必填） |
| permission_id | BIGINT | 否 | 关联的 open 权限 ID（非必填） |
| status | TINYINT | 是 | 状态：1-已发布 2-已废弃 |
| deprecate_reason | VARCHAR(2000) | 否 | 废弃原因（仅 status=2 时有值） |
| create_by | VARCHAR(64) | 是 | 创建人 |
| create_time | DATETIME | 是 | 创建时间 |
| last_update_by | VARCHAR(64) | 是 | 最后更新人 |
| last_update_time | DATETIME | 是 | 最后更新时间 |

**说明**：
- `sdk_code`、`sdk_name` 已移除；版本直接按 `version_name` 维度管理，不在表中维护 SDK 维度的名称/编码信息
- 冗余字段 `permission_name` 已移除；权限信息不缓存到本地，列表展示时如需权限名再按需实时查询
- 废弃操作人（`deprecated_by`）、废弃时间（`deprecated_at`）均被移除
- 唯一控制由业务层校验：`(version_name, permission_id)` **联合唯一，全局生效、不区分状态**（已发布/已废弃均参与约束）；`permission_id` 为空时不参与联合约束，按 `version_name` 单独查重

### 3.2 状态机

```
                    ┌─────────────┐
        ┌──────────►│             │
        │  上架/创建 │   已发布    │
        │  (version  │   (status=1)│
        │  Name 必填)│             │
        │           └──────┬──────┘
        │                  │ 废弃（必填废弃原因）
        │                  ▼
        │           ┌─────────────┐
        └───────────│             │
           恢复      │   已废弃    │
          (清空原因) │   (status=2)│
                    └─────────────┘
```

- **已发布 (1)**：可编辑元数据；可废弃
- **已废弃 (2)**：不可编辑元数据；只可恢复（恢复后变回 1）
- 上架（创建）时 `version_name` 必填，直接以「已发布」状态入库
- 废弃必须填写废弃原因（5-2000 字符），写入 `deprecate_reason`
- 恢复会清空 `deprecate_reason`

无审批流程（与 app version 上架不同，区别详见第 6 章）。**SDK 版本管理走管理后台直接生效**，无需审批中心参与。

---

## 4. 接口设计（market-server）

### 4.1 SDK 权限查询（仅 1 个，market 自行实现）

> 用于在 market 上架/编辑表单中给用户选择"关联哪个权限"。**不调用 open 接口**，由 market-server 直接查库实现。

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/service/open/v2/sdk/version/permissions` | 直接查库返回 SDK 权限分类下的权限平铺列表 |

> **实现**：market 与 open 共库。SDK 权限分类的 `category_alias` 在 market 侧**配置化**（如 `sdk.permission-category-alias = SDK_PERMISSION`），**一条关联 SQL**（JOIN category 表按 alias 过滤）直接返回权限平铺列表：
>
> ```sql
> SELECT p.id, p.name_cn, p.name_en, p.scope
> FROM openplatform_v2_permission_t p
> INNER JOIN openplatform_v2_category_t c ON p.category_id = c.id
> WHERE c.category_alias = #{categoryAlias} AND p.status = 1
> ```
>
> **不复制、不缓存权限数据**。

```json
// response（平铺列表，字段与 openplatform_v2_permission_t 对应）
{
  "code": "200",
  "messageZh": "操作成功",
  "messageEn": "Success",
  "data": [
    { "id": 10023, "nameCn": "SDK 查询权限", "nameEn": "SDK read permission", "scope": "sdk:read" },
    { "id": 10024, "nameCn": "SDK 写入权限", "nameEn": "SDK write permission", "scope": "sdk:write" }
  ]
}
```

### 4.2 SDK 版本管理接口

> 全部位于 `/service/open/v2/sdk/version` 前缀下，统一使用现有 `@AuthRole` 鉴权（需登录即可，与现有模块一致，无角色区分）。

| # | 方法 | 路径 | 说明 |
|---|------|------|------|
| 1 | POST | `/service/open/v2/sdk/version` | **上架**新版本（versionName 必填） |
| 2 | PUT | `/service/open/v2/sdk/version/{id}` | **编辑**（仅 status=1 可编辑） |
| 3 | POST | `/service/open/v2/sdk/version/updateStatus` | **废弃/恢复**（body 传 `status`：2=废弃，1=恢复；废弃时 `deprecateReason` 必填 5-2000 字符） |
| 4 | GET | `/service/open/v2/sdk/version/{id}` | 详情 |
| 5 | GET | `/service/open/v2/sdk/version/list` | 分页列表（支持 status/versionName 筛选） |

#### 请求/响应示例

**① 上架新版本** `POST /service/open/v2/sdk/version`

```json
// request
{
  "versionName": "1.2.0",
  "updateNotes": "新增 X 接口、修复 Y bug",
  "artifactUrl": "https://artifacts.example.com/java-sdk/1.2.0/java-sdk-1.2.0.jar",
  "permissionId": 10023
}
```

```json
// response
{
  "code": "200",
  "messageZh": "操作成功",
  "messageEn": "Success",
  "data": { "id": 10001, "status": 1 }
}
```

**② 编辑** `PUT /service/open/v2/sdk/version/{id}`

```json
// request
{
  "updateNotes": "修复 Y bug",
  "artifactUrl": "https://artifacts.example.com/java-sdk/1.2.0/java-sdk-1.2.0.jar",
  "permissionId": 10023
}
```

```json
// response
{
  "code": "200",
  "messageZh": "操作成功",
  "messageEn": "Success"
}
```

**③ 废弃/恢复（状态变更）** `POST /service/open/v2/sdk/version/updateStatus`

废弃：
```json
// request
{
  "id": 10001,
  "status": 2,
  "deprecateReason": "已被 v1.3.0 取代，存在已知安全漏洞 CVE-2026-XXXX"
}
```

恢复（无需 reason）：
```json
// request
{
  "id": 10001,
  "status": 1
}
```

废弃/恢复共用响应：
```json
// response
{
  "code": "200",
  "messageZh": "操作成功",
  "messageEn": "Success"
}
```

**④ 列表** `GET /service/open/v2/sdk/version/list?status=1&versionName=1.2&curPage=1&pageSize=10`

```json
{
  "code": "200",
  "messageZh": "操作成功",
  "messageEn": "Success",
  "data": [
    {
      "id": 10001,
      "versionName": "1.2.0",
      "updateNotes": "新增 X 接口、修复 Y bug",
      "permissionId": 10023,
      "permissionName": "SDK 查询权限",
      "status": 1,
      "createBy": "admin",
      "createTime": "2026-08-05 18:30:00",
      "lastUpdateBy": "admin",
      "lastUpdateTime": "2026-08-05 18:30:00"
    }
  ],
  "page": {
    "curPage": 1,
    "pageSize": 10,
    "total": 1,
    "totalPages": 1
  }
}
```

**⑤ 详情** `GET /service/open/v2/sdk/version/{id}`

```json
// response
{
  "code": "200",
  "messageZh": "操作成功",
  "messageEn": "Success",
  "data": {
    "id": 10001,
    "versionName": "1.2.0",
    "updateNotes": "新增 X 接口、修复 Y bug",
    "artifactUrl": "https://artifacts.example.com/java-sdk/1.2.0/java-sdk-1.2.0.jar",
    "permissionId": 10023,
    "permissionName": "SDK 查询权限",
    "status": 1,
    "deprecateReason": null,
    "createBy": "admin",
    "createTime": "2026-08-05 18:30:00",
    "lastUpdateBy": "admin",
    "lastUpdateTime": "2026-08-05 18:30:00"
  }
}
```

---

## 5. 代码结构

```
market-server/src/main/java/com/xxx/it/works/wecode/v2/modules/
└── sdk/                                  ← 新建
    ├── controller/
    │   └── SdkVersionController.java
    ├── service/
    │   ├── SdkVersionService.java
    │   └── SdkVersionServiceImpl.java
    ├── dto/
    │   ├── SdkVersionCreateDTO.java      ← 上架（含 @NotBlank versionName）
    │   ├── SdkVersionUpdateDTO.java      ← 编辑
    │   ├── SdkVersionStatusUpdateDTO.java ← 废弃/恢复（id + status；废弃时 @NotBlank deprecateReason + @Size(min=5, max=2000)）
    │   └── SdkVersionQueryDTO.java       ← service 层查询条件封装（Controller 用 @RequestParam 接 URL 参数后组装，不作为入参绑定）
    ├── vo/
    │   ├── SdkVersionListVO.java         ← 列表项（含 permissionName，联查权限表）
    │   ├── SdkVersionDetailVO.java       ← 详情（含 permissionName / deprecateReason）
    │   └── SdkPermissionVO.java          ← 权限查询返回（id/nameCn/nameEn/scope）
    ├── entity/
    │   └── SdkVersionEntity.java
    ├── mapper/
    │   ├── SdkVersionMapper.java
    │   └── PermissionMapper.java        ← 一条 JOIN SQL 按配置 alias 查权限表（共库联查）
    └── constant/
        └── SdkVersionStatusEnum.java     ← 1=已发布 2=已废弃

market-server/src/main/resources/mapper/   ← MyBatis XML 统一放 resources（与现有模块一致）
    ├── SdkVersionMapper.xml
    └── PermissionMapper.xml
```

### 5.1 关键逻辑

**上架（Create）**：
1. 校验 `versionName` 非空（已在 DTO 用 `@NotBlank` 兜底）
2. 查重（`(versionName, permissionId)` 联合唯一、不区分状态）：已存在同 `versionName` 且同 `permissionId` 的记录（**无论 status**）→ 抛业务异常 `SDK_VERSION_ALREADY_EXISTS`；`permissionId` 为空时按 `versionName` 单独查重
3. `artifactUrl` 和 `permissionId` 均为选填；如 `permissionId` 有值则通过 `PermissionMapper` 用**一条关联 SQL**（按配置 alias JOIN category 表）校验该权限存在于 SDK 分类下（不存在则返回 `SDK_PERMISSION_NOT_FOUND`），**不缓存**权限名
4. INSERT，默认 `status=1`

**编辑（Update）**：
- 只允许 `status=1` 的版本编辑；`status=2` 返回 `SDK_VERSION_DEPRECATED_CANNOT_EDIT`
- `versionName` 一旦创建不可修改（业务约束，DTO 不暴露此字段）

**状态变更（废弃/恢复 updateStatus）**：
- 入参 `id` + `status`（1=恢复，2=废弃）
- 废弃（`status=2`）：
  - 校验 `deprecateReason` 至少 5 个字符、最多 2000（DTO `@Size(min=5, max=2000)` + `@NotBlank`）
  - 校验当前 `status=1`，否则返回 `SDK_VERSION_ALREADY_DEPRECATED`
  - UPDATE `status=2, deprecate_reason=?`
- 恢复（`status=1`）：
  - 校验当前 `status=2`，否则返回 `SDK_VERSION_ALREADY_PUBLISHED`
  - UPDATE `status=1`，清空 `deprecate_reason`
  - 注：联合唯一全局生效，同 `(versionName, permissionId)` 组合不可能存在第二条记录，恢复天然不会产生冲突，无需额外冲突校验

### 5.2 复用现有约定

- **响应包装**：统一用 `ApiResponse<T>`（已有）
- **鉴权**：统一 `@AuthRole`（需登录即可，现有注解无角色区分）
- **分页**：复用 `ApiResponse.PageResponse`（`curPage/pageSize/total/totalPages`），与 approval/ability 模块一致；请求参数 `curPage`/`pageSize`
- **异常**：使用 `BusinessException` + `ResponseCodeEnum` 新增 6 个码
- **当前用户**：从 `UserContextHolder.get()` 取 `createBy / lastUpdateBy`
- **时间**：使用 `LocalDateTime`（与项目内新代码一致；如老代码用 `Date`，以本模块同包已有实体为准）
- **审计字段**：每张表都带 `create_by/create_time/last_update_by/last_update_time`，由 Service 层统一 fill

---

## 6. 与现有 AppVersion 的关系

`modules/approval/AppVersionEntity`（应用版本）继续保留，**SDK 版本不与之合并**，原因：
- 业务域不同：AppVersion 关联「应用」并走审批；SdkVersion 关联「SDK」不走审批
- 表结构差异：SdkVersion 有 `permission_id` / `deprecate_reason` / `artifact_url` / `update_notes` 等 SDK 专用字段，AppVersion 没有
- 生命周期不同：SdkVersion 只有「发布/废弃」两态，AppVersion 有 5 态含审批

两者**共用** `market-server` 部署，但**模块独立**（`modules.sdk` vs `modules.approval`），互不影响。

---

## 7. 异常码（新增到 ResponseCodeEnum）

> 格式与现有 `ResponseCodeEnum` 保持一致：`NAME("code", "messageZh", "messageEn")`，按业务分组注释。

```java
// SDK 版本 - 状态流转（4000x：状态问题，与 STATUS_CANNOT_DELETE 同段）
SDK_VERSION_DEPRECATED_CANNOT_EDIT("40005", "已废弃版本不可编辑", "Deprecated version cannot be edited"),
SDK_VERSION_ALREADY_DEPRECATED("40006", "该版本已是废弃状态", "Version is already deprecated"),
SDK_VERSION_ALREADY_PUBLISHED("40007", "该版本已是发布状态", "Version is already published"),

// SDK 版本 - 资源不存在（404xx，与 NOT_FOUND 同段）
SDK_VERSION_NOT_FOUND("40403", "SDK 版本不存在", "SDK version not found"),
SDK_PERMISSION_NOT_FOUND("40404", "关联的权限不存在", "Related permission not found"),

// SDK 版本 - 资源已存在（409xx，与 ALREADY_EXISTS 同段）
SDK_VERSION_ALREADY_EXISTS("40903", "该版本名与权限的组合已存在", "SDK version with this permission already exists"),
```

---

## 8. 数据库 DDL

```sql
CREATE TABLE openplatform_sdk_version_t (
    id                  BIGINT          NOT NULL COMMENT '主键ID',
    version_name        VARCHAR(64)     NOT NULL COMMENT '版本名',
    update_notes        VARCHAR(2000)   NULL     COMMENT '更新说明',
    artifact_url        VARCHAR(512)    NULL     COMMENT '构建产物下载地址',
    permission_id       BIGINT          NULL     COMMENT '关联权限ID(可选)',
    status              TINYINT         NOT NULL DEFAULT 1 COMMENT '1-已发布 2-已废弃',
    deprecate_reason    VARCHAR(2000)   NULL     COMMENT '废弃原因',
    create_by           VARCHAR(64)     NOT NULL,
    create_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_update_by      VARCHAR(64)     NOT NULL,
    last_update_time    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_version_permission (version_name, permission_id),
    KEY idx_permission_id (permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='SDK版本表';
```

> 注：`uk_version_permission` 为全局联合唯一索引（不区分状态）。MySQL 唯一索引对 `permission_id` 为 NULL 的组合不生效（允许多条 `(version_name, NULL)`），因此 `permission_id` 为空时的查重由业务层兜底。

---

## 9. 前端页面（market-web）

`market-web/src/router/routeRedBlue/sdk-version/`（新增，目录命名与现有 `lookup-classify` / `ability-admin` 等模块一致）：

| 文件 | 说明 |
|------|------|
| `index.js` | 列表页主组件（筛选 status，分页表格；含详情/编辑/废弃/恢复操作入口） |
| `route.js` | 路由定义（`path: '/sdk-version'`，`component: () => import('./index')`，`layout: 'inner'`） |
| `thunk.js` | 6 个接口请求封装（5 个版本管理 + 1 个权限查询） |
| `constant.js` | 常量（状态枚举映射、列表表头、默认分页） |
| `index.module.less` | 样式 |
| `components/` | 子组件：`CreateForm.js` 上架表单（versionName 必填）、`EditForm.js` 编辑表单、`DetailDrawer.js` 详情抽屉（只读 + 废弃/恢复按钮）、`DeprecateModal.js` 废弃弹窗（必填原因） |

> 说明：详情/编辑/废弃均以 **Drawer/Modal** 形式内嵌在列表页，不建独立路由页（与 `lookup-dictionary` 的 `DictionaryDetailDrawer` 风格一致）。

**路由注册**（在 `market-web/src/router/index.tsx` 中新增）：
- `import SdkVersionList from './routeRedBlue/sdk-version';`
- `<Route path="sdk-version" element={<SdkVersionList />} />`

页面路径：`/sdk-version`（列表）

---

## 10. 任务拆分（建议）

1. 后端（market-server）：
   1. DDL + Entity + Enum + Mapper XML
   2. DTO（含校验注解）+ ResponseCode 新增
   3. Service 接口与实现（含 4 个业务校验逻辑）
   4. Controller 6 个接口 + Swagger 注解（5 个版本管理 + 1 个权限查询）
   5. PermissionMapper（`category_alias` 配置化，一条 JOIN SQL 按 alias 直接查权限表）
   6. 单元测试
2. 前端（market-web）：
   1. `src/router/routeRedBlue/sdk-version/` 模块（thunk.js 封装 6 个接口 + constant + route.js）
   2. 列表页（index.js）+ 详情 Drawer + 上架/编辑表单 + 废弃弹窗
   3. 权限下拉组件（通用化，可被其他模块复用）
   4. 路由注册（index.tsx）+ 菜单
3. open-server（仅数据侧，无接口开发）：
   1. 在权限中心新增"SDK 权限"分类（数据初始化）
4. 联调与测试

---

## 11. 待确认事项

- [ ] 表内无 SDK 维度信息，如需展示 SDK 名称请确认来源（从 `artifact_url` 推断 / 关联表 / 前端录入）。
- [ ] 是否需要"批量废弃"接口？本期不做。
- [ ] 是否需要"软删除"？本期不做（废弃即可，保留历史）。
- [ ] 关联权限是否支持一个 SDK 多权限（一对多）？本期只做一对一。
- [ ] SDK 权限分类的 `category_alias` 值（如 `SDK_PERMISSION`）需与 open 侧实际数据一致，建议在 market 配置（`application-*.yml`）中统一定义，并随 open 侧分类初始化数据同步。
