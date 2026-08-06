# SDK 版本管理 - 执行 Task 文档

> 依据：`SDK版本管理方案.md` + `SDK版本管理交互图.html`
> 目标：在 market-server（后端）+ market-web（前端）实现 SDK 版本全生命周期管理
> 状态：✅ 全部完成（编译通过，联调中按交互图迭代调整）

---

## 一、后端（market-server）

### Task 1：DDL + Entity + Enum + ResponseCode ✅

**1.1 DDL**
- `src/main/resources/db/migration/V3__create_sdk_version_table.sql`：新建表 `openplatform_sdk_version_t`
- 已同步到 open-flyway：`open-flyway/src/main/resources/db/migration/V8__create_sdk_version_table.sql`（版本号 V8 避免与已有 V1-V7 冲突）
- 主键 id（BIGINT，无 AUTO_INCREMENT，雪花算法）
- 唯一索引 `uk_version_permission (version_name, permission_id)`（全局唯一、不区分状态）
- 普通索引 `idx_permission_id (permission_id)`
- 字段：version_name, update_notes, artifact_url, permission_id, status(1已发布/2已废弃), deprecate_reason, create_by, create_time, last_update_by, last_update_time

**1.2 Entity**（`modules/sdk/entity/SdkVersionEntity.java`）
- `@Data` + `Serializable` + `serialVersionUID`
- 表字段：id, versionName, updateNotes, artifactUrl, permissionId, status, deprecateReason, createBy, createTime, lastUpdateBy, lastUpdateTime
- 时间字段用 `Date`（与项目同模块老代码一致）
- 联查冗余字段（非表字段）：permissionName, permissionScope（列表/详情 LEFT JOIN 权限表填充）

**1.3 状态枚举**（`modules/sdk/constant/SdkVersionStatusEnum.java`）
- `PUBLISHED(1, "已发布")`, `DEPRECATED(2, "已废弃")`

**1.4 ResponseCodeEnum 新增 5 个码**（修改 `common/enums/ResponseCodeEnum.java`）
```java
SDK_VERSION_DEPRECATED_CANNOT_EDIT("40005", "已废弃版本不可编辑", "Deprecated version cannot be edited"),
SDK_VERSION_ALREADY_DEPRECATED("40006", "该版本已是废弃状态", "Version is already deprecated"),
SDK_VERSION_ALREADY_PUBLISHED("40007", "该版本已是发布状态", "Version is already published"),
SDK_VERSION_NOT_FOUND("40403", "SDK版本不存在", "SDK version not found"),
SDK_VERSION_ALREADY_EXISTS("40903", "该版本号与权限的组合已存在", "SDK version with this permission already exists"),
```
> 注：SDK_PERMISSION_NOT_FOUND（40404）未使用，改为权限校验失败时统一走 `PARAM_ERROR` 或校验不通过提示，故未在枚举新增。

### Task 2：DTO + VO ✅

**2.1 DTO**
- `SdkVersionCreateDTO`：versionName(@NotBlank, max=64), updateNotes(max=2000), artifactUrl(max=512), permissionId
- `SdkVersionUpdateDTO`：updateNotes, artifactUrl, permissionId（无 versionName，不可改）
- `SdkVersionStatusUpdateDTO`：id(@NotNull), status(@NotNull), deprecateReason（废弃时 @NotBlank + @Size(min=5,max=2000)）
- 查询参数用 @RequestParam 接收（status, versionName, curPage, pageSize），service 内组装

**2.2 VO**
- `SdkVersionListVO`：id, versionName, updateNotes, permissionId, permissionName, permissionScope, status, createBy, createTime, lastUpdateBy, lastUpdateTime
- `SdkVersionDetailVO`：id, versionName, updateNotes, artifactUrl, permissionId, permissionName, permissionScope, status, deprecateReason, createBy, createTime, lastUpdateBy, lastUpdateTime
- `SdkPermissionVO`：id, nameCn, nameEn, scope
- 所有 Long 字段（id/permissionId）加 `@JsonSerialize(using = ToStringSerializer.class)`（避免浏览器精度丢失，列表/详情/权限接口均返回字符串 id），时间加 `@JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")`

### Task 3：Mapper 接口 + XML ✅

**3.1 SdkVersionMapper**（`modules/sdk/mapper/SdkVersionMapper.java`）
- `insert(SdkVersionEntity)` - 插入
- `updateByPrimaryKeySelective(SdkVersionEntity)` - 选择性更新（updateStatus 用）
- `updateByPrimaryKey(SdkVersionEntity)` - **全字段更新（null 也写入）**（编辑用，支持清空字段）
- `selectByPrimaryKey(Long id)` - 按主键查
- `selectDetailByPrimaryKey(Long id)` - 按主键查（LEFT JOIN 权限表取 permissionName/permissionScope）
- `selectPage(status, versionName, offset, pageSize)` - 分页列表（LEFT JOIN 权限表）
- `countByPage(status, versionName)` - 分页计数
- `countDuplicate(versionName, permissionId, excludeId)` - 查重（联合唯一；permissionId 为空按 version_name 单独查重；excludeId 编辑时排除自身）

**3.2 SdkVersionMapper.xml**（`resources/mapper/SdkVersionMapper.xml`）
- BaseResultMap / DetailResultMap（Detail 含 permission_name、permission_scope）
- insert 含 id 列
- updateByPrimaryKeySelective：`<set>` + `<if test="xxx != null">`
- **updateByPrimaryKey：无 if 判断，update_notes/artifact_url/permission_id 传 null 也直接写入（整体更新语义）**
- 分页/详情/计数查询 LEFT JOIN openplatform_v2_permission_t 取权限名 + scope
- 模糊匹配：`LIKE #{versionName}`，`%` 由服务层拼接（参考 lookup 模块写法，避免 CONCAT 触发 SQL codecheck）
- 查重 SQL：`WHERE version_name=#{versionName}` + `<if permissionId!=null>AND permission_id=#{permissionId}` / `<if permissionId==null>AND permission_id IS NULL` + `<if excludeId!=null>AND id!=#{excludeId}`

**3.3 PermissionMapper**（`modules/sdk/mapper/PermissionMapper.java`）
- `selectByCategoryAlias(@Param categoryAlias)` - 按 alias JOIN category 表查权限列表
- `countByIdAndCategoryAlias(@Param id, @Param categoryAlias)` - 校验某权限是否在 SDK 分类下

**3.4 PermissionMapper.xml**（`resources/mapper/PermissionMapper.xml`）
- JOIN SQL：`permission p INNER JOIN category c ON p.category_id=c.id WHERE c.category_alias=#{categoryAlias} AND p.status=1`

### Task 4：Service 接口 + 实现 ✅

**4.1 SdkVersionService**（`modules/sdk/service/SdkVersionService.java`）
- 6 个方法：create, update, updateStatus, getById, list, listPermissions

**4.2 SdkVersionServiceImpl**（`modules/sdk/service/impl/SdkVersionServiceImpl.java`）
- `@Slf4j @Service`，构造器注入 SdkVersionMapper, PermissionMapper, IdGeneratorStrategy
- SDK 权限分类 alias：`@Value("${sdk.permission-category-alias:api_bussiness_app_sdk}")`，配置文件 `application.yml` 中 `sdk.permission-category-alias`（联调时按 open 侧实际分类别名调整）

**关键逻辑**：
- **create**：查重(联合唯一,不区分状态) -> 有 permissionId 则校验权限存在于 SDK 分类 -> INSERT status=1 -> **返回 `ApiResponse<String>`，data 直接为新版本 id 字符串（不再返回 Map）**
- **update**：查存在(40403) -> 校验 status=1(40005) -> **查重（(versionName,permissionId) 联合唯一，排除自身）** -> 有 permissionId 校验权限存在 -> **全字段更新（updateByPrimaryKey，null 即清空）**；versionName 不可修改
- **updateStatus**：查存在(40403) -> 废弃(status=2): 校验当前 status=1(40006) + reason 5-2000 校验 -> UPDATE status/deprecate_reason; 恢复(status=1): 校验当前 status=2(40007) -> UPDATE status=1、清空 deprecate_reason（用 selective，恢复清空传空串）
- **getById**：查存在(40403) -> 联查权限名/scope -> 返回 DetailVO
- **list**：versionName 模糊匹配服务层拼 `%` -> 分页查询 -> 返回 ListVO 列表 + `ApiResponse.PageResponse`（curPage/pageSize/total/totalPages）
- **listPermissions**：按配置 alias 查 SDK 分类下权限 -> 返回 SdkPermissionVO 列表

### Task 5：Controller ✅

**5.1 SdkVersionController**（`modules/sdk/controller/SdkVersionController.java`）
- 路径前缀 `/service/open/v2/sdk/version`
- 构造器注入 SdkVersionService
- 6 个接口：
  1. `POST /` - create（@Valid @RequestBody SdkVersionCreateDTO）→ **`ApiResponse<String>`**
  2. `PUT /{id}` - update（@PathVariable + @Valid @RequestBody SdkVersionUpdateDTO）
  3. `POST /updateStatus` - updateStatus（@Valid @RequestBody SdkVersionStatusUpdateDTO）
  4. `GET /{id}` - getById
  5. `GET /list` - list（@RequestParam status, versionName, curPage, pageSize）
  6. `GET /permissions` - listPermissions
- 每个方法标 `@AuthRole` + `@Operation`(Swagger)

---

## 二、前端（market-web）

### Task 6：sdk-version 模块 ✅

目录：`src/router/routeRedBlue/sdk-version/`

**6.1 constant.js**
- 状态枚举映射（1=已发布, 2=已废弃）、筛选选项
- 列表表头：版本号/关联权限（**仅名称**）/更新说明/状态/创建人/创建时间/更新人/更新时间/操作
- 默认分页

**6.2 thunk.js**（6 个接口请求封装）
- getSdkVersionList, getSdkVersionDetail, createSdkVersion, updateSdkVersion, updateSdkVersionStatus, getSdkPermissions

**6.3 route.js**
- `path: '/sdk-version'`，`component: () => import('./index')`

**6.4 index.js**（列表页主组件）
- 筛选（状态下拉 + 版本号输入）+ 搜索/重置 + 分页表格
- 操作：详情（Drawer）、编辑（Modal）、废弃（Modal 必填原因）、恢复（确认弹窗）
- **编辑：先调用详情接口获取完整数据再回填**（列表接口不返回 artifactUrl，直接回填列表记录会丢旧值）
- 列表关联权限列只展示名称（scope 不展示）

**6.5 components/**
- `SdkVersionFormModal.js` - **上架/编辑共用表单**（editingId 存在时隐藏版本号字段；版本号必填；关联权限下拉默认"请选择关联权限"、选项显示 `名称（scope）`；更新说明 textarea 多行；构建产物地址 Input）
- `SdkVersionDetailDrawer.js` - 详情抽屉（只读；关联权限显示 `名称（scope）`；更新说明/废弃原因 `white-space: pre-wrap` 多行展示；底部按状态显示废弃/恢复按钮）
- `DeprecateModal.js` - 废弃弹窗（必填原因 textarea 多行，5-2000 字符校验；确认文案"确定废弃版本 X 吗？"）

**6.6 index.module.less**
- 复用 lookup-classify 风格

**6.7 web.config.js**（修改，`src/configs/web.config.js`）
- 新增 6 个 API 配置：SDK_VERSION_LIST / SDK_VERSION_DETAIL / SDK_VERSION_CREATE / SDK_VERSION_UPDATE / SDK_VERSION_UPDATE_STATUS / SDK_PERMISSIONS

### Task 7：路由 + 菜单注册 ✅

**7.1 路由注册**（`src/router/index.tsx`）
- import SdkVersionList
- `<Route path="sdk-version" element={<SdkVersionList />} />`

**7.2 菜单注册**（`src/components/Layout/index.js`）
- menuItems 新增 `{ key: '/sdk-version', icon: <ToolOutlined />, label: 'SDK版本管理' }`

---

## 三、测试

### Task 8：编译 + 测试 ✅
- 后端：`mvn compile` 通过
- 前端：`npm run build` 通过
- 联调验证：
  - SDK 权限查询接口正常返回（配置 alias 生效）
  - 上架/编辑/废弃/恢复/详情/列表接口已按需联调

---

## 四、联调期按交互图迭代的调整记录

1. **版本名 → 版本号**：全站字段文案统一（表格列头、搜索、表单、详情）
2. **列表新增更新人、更新时间列**
3. **关联权限下拉默认项**："请选择关联权限"（选填，不选即不关联）
4. **废弃/恢复确认弹窗**：仅保留"确定废弃/恢复版本 X 吗？"主文案
5. **关联权限展示**：详情抽屉、上架/编辑表单下拉显示 `名称（scope）`；**列表只显示名称**
6. **多行支持**：更新说明、废弃原因填写用 textarea，详情展示用 pre-wrap
7. **create 接口出参**：由 Map（id+status）改为只返回 id 字符串
8. **id 序列化**：所有接口 Long id 以字符串返回（ToStringSerializer），避免前端精度丢失
9. **编辑回填修复**：编辑改用详情接口数据回填
10. **编辑唯一性校验 + 全字段更新**：编辑改权限时校验联合唯一（排除自身）；更新改用全字段 updateByPrimaryKey，传 null 即清空字段
11. **SQL codecheck**：LIKE 模糊匹配改为服务层拼 `%` + `LIKE #{param}`（参考 lookup）
12. **alias 配置**：默认值 `api_bussiness_app_sdk`，application.yml 可按 open 侧实际分类别名覆盖（当前 `api_personal_user_aksk`）

## 执行顺序
1 -> 2 -> 3 -> 4 -> 5（后端）-> 6 -> 7（前端）-> 8（测试）-> 联调迭代调整
