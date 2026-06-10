# Spec：应用绑定单聊机器人账号

> **版本**: v1.0  
> **日期**: 2026-06-10  
> **状态**: Draft  
> **范围**: market-server（后端 API）+ market-web（前端 Tab 组件）

---

## 1. Scope

### 1.1 功能概述

在应用管理审批跳转的详情页（**该页面不在现有代码中，由外部系统集成**）内新增一个「机器人绑定」Tab，实现以下三个能力：

| 能力 | 说明 |
|------|------|
| **查询** | 获取当前应用已绑定的所有单聊机器人账号列表 |
| **绑定** | 为当前应用手动输入并绑定一个新的单聊机器人账号 |
| **解绑** | 从当前应用解除一个已绑定的单聊机器人账号 |

### 1.2 业务规则

| # | 规则 | 来源 |
|---|------|------|
| BR-1 | 一个应用可绑定的机器人账号数量上限由 Lookup 配置决定，**服务端在绑定时校验** | 用户确认 |
| BR-2 | 每个绑定的机器人在 `openplatform_app_p_t` 中存储为**独立一行**记录 | 用户确认 |
| BR-3 | 存储方式：`parent_id` = 应用主键 `id`，`property_name` = `single_chatbot_account`，`property_value` = 账号 ID | 用户确认 |
| BR-4 | 解绑操作执行**硬删除**（DELETE FROM），通过 `app_id` + `accountId` 定位记录 | 用户确认 |
| BR-5 | 绑定/解绑操作通过**现有 AOP 注解机制**记录审计日志，开发者仅需在接口方法上添加注解 | 用户确认 |
| BR-6 | 权限控制：仅校验登录态（`@AuthRole`），不做细粒度角色校验 | 用户确认 |
| BR-7 | 机器人账号格式：以 `p_` 开头，最大长度 200 字符 | 用户确认 |
| BR-8 | **前端不控制数量上限**，上限校验完全由服务端在绑定时负责 | 用户确认 |
| BR-9 | **DB 主键为雪花 ID（Long）**，返回前端时**必须转为 String** 防 JS 精度丢失 | 用户确认 |
| BR-10 | **DB Entity 与 API VO 严格分离**，DB Entity 用 `Long`，VO 用 `String` | 用户确认 |

### 1.3 不在范围内

- 应用详情页本身的路由和框架搭建（由外部系统集成）
- 机器人账号的创建/管理（由其他系统负责）
- 机器人账号的存在性远程校验（仅做格式校验）
- 审批流程相关的任何改动
- 前端对绑定数量上限的展示或控制
- 雪花 ID 生成器实现代码（预留人工实现）

### 1.4 系统上下文

```mermaid
graph TB
    subgraph 外部系统
        DetailPage["应用详情页<br/>(不在本仓库)"]
    end

    subgraph market-web
        ChatbotTab["机器人绑定 Tab 组件<br/>(本次新建)"]
        FetchAPI["fetchApi()<br/>webFetch.js"]
        WebConfig["web.config.js<br/>URL 常量"]
    end

    subgraph market-server
        Controller["ChatbotBindController<br/>/service/open/v2/apps/single-chatbot-accounts"]
        Service["ChatbotBindService"]
        LookupSvc["Lookup 配置读取"]
        Mapper["ChatbotBindMapper"]
    end

    subgraph MySQL["openapp 数据库"]
        AppT["openplatform_app_t"]
        AppPT["openplatform_app_p_t"]
        LookupClassify["openplatform_lookup_classify_t"]
        LookupItem["openplatform_lookup_item_t"]
    end

    DetailPage -->|"传入 appId prop"| ChatbotTab
    ChatbotTab --> FetchAPI
    FetchAPI --> WebConfig
    FetchAPI -->|"HTTP :18080"| Controller
    Controller --> Service
    Service --> LookupSvc
    Service --> Mapper
    Mapper --> AppT
    Mapper --> AppPT
    LookupSvc --> LookupClassify
    LookupSvc --> LookupItem
```

---

## 2. Interface

### 2.1 ID 处理策略

```mermaid
graph LR
    subgraph "DB 层 (Entity)"
        DB["Long id<br/>Long parentId"]
    end

    subgraph "Service 层"
        Convert["Entity → VO 转换<br/>Long → String"]
    end

    subgraph "API 层 (VO)"
        VO["String id<br/>String parentId"]
    end

    subgraph "前端 (JS)"
        JS["String 直接渲染<br/>无精度丢失"]
    end

    DB --> Convert --> VO --> JS
```

**规则**:
- DB Entity（`AppPropertyEntity`）中 `id`、`parentId` 类型为 `Long`
- API VO（`ChatbotAccountVO`）中所有 ID 字段类型为 `String`
- Service 层负责 Entity → VO 转换，`Long.toString()` 序列化
- 雪花 ID 生成由 `IdGeneratorStrategy` 负责，**生成代码预留人工实现**，本 Spec 不生成

### 2.2 后端 API

三个接口统一路径：`/service/open/v2/apps/single-chatbot-accounts`

| # | Method | 参数位置 | 用途 |
|---|--------|----------|------|
| 1 | **GET** | QueryParam (`?appId=xxx`) | 查询已绑定列表 |
| 2 | **POST** | Body (`{ appId, accountId }`) | 绑定 |
| 3 | **DELETE** | Body (`{ appId, accountId }`) | 解绑 |

---

#### 2.2.1 查询已绑定账号列表

```
GET /service/open/v2/apps/single-chatbot-accounts?appId={appId}
```

**请求参数**:

| 参数 | 位置 | 类型 | 必填 | 说明 |
|------|------|------|------|------|
| `appId` | query | String | 是 | 应用业务 ID（`openplatform_app_t.app_id`） |

**响应** `ApiResponse<List<ChatbotAccountVO>>`:

```json
{
  "code": "200",
  "messageZh": "成功",
  "messageEn": "Success",
  "data": [
    {
      "id": "1934567890123456789",
      "accountId": "p_abc123",
      "bindTime": "2026-06-10 14:30:00",
      "bindBy": "userId_001"
    }
  ]
}
```

> `id` 为 `app_p_t.id` 的 String 形式（雪花 ID → String），防止 JS 精度丢失。

**处理逻辑**:
1. 通过 `app_id` 查询 `openplatform_app_t` 获取主键 `id`（不存在或 status=0 → 40001）
2. 查询 `openplatform_app_p_t` WHERE `parent_id` = {app主键id} AND `property_name` = 'single_chatbot_account' AND `status` = 1
3. 按 `create_time` DESC 排序
4. Entity → VO 转换（`Long` → `String`）

**错误码**:

| code | 含义 |
|------|------|
| 40001 | 应用不存在 |
| 40101 | 未登录 |

---

#### 2.2.2 绑定机器人账号

```
POST /service/open/v2/apps/single-chatbot-accounts
```

**请求体**:

```json
{
  "appId": "app001",
  "accountId": "p_abc123"
}
```

| 参数 | 类型 | 必填 | 校验规则 | 说明 |
|------|------|------|----------|------|
| `appId` | String | 是 | @NotBlank | 应用业务 ID |
| `accountId` | String | 是 | 以 `p_` 开头，长度 1~200 | 机器人账号 ID |

**响应** `ApiResponse<ChatbotAccountVO>`:

```json
{
  "code": "200",
  "messageZh": "绑定成功",
  "messageEn": "Bind successful",
  "data": {
    "id": "1934567890123456789",
    "accountId": "p_abc123",
    "bindTime": "2026-06-10 14:30:00",
    "bindBy": "userId_001"
  }
}
```

**处理逻辑**:
1. 校验 `accountId` 格式：正则 `^p_.{1,196}$`
2. 通过 `app_id` 查询应用主键 `id`（不存在 → 40001）
3. 查询 Lookup 配置获取最大可绑定数量
4. 查询当前已绑定数量
5. 已绑定 >= 上限 → 40003
6. 重复检查（同 property_value 已存在且 status=1）→ 40004
7. 通过 `IdGeneratorStrategy` 生成雪花 ID（**预留人工实现**）
8. INSERT `openplatform_app_p_t`
9. Entity → VO 转换（`Long` → `String`）
10. 添加审计日志注解 #ASSUMED

**错误码**:

| code | 含义 |
|------|------|
| 40001 | 应用不存在 |
| 40002 | accountId 格式不合法 |
| 40003 | 超过最大可绑定数量 |
| 40004 | 该账号已绑定（重复绑定） |
| 40101 | 未登录 |

---

#### 2.2.3 解绑机器人账号

```
DELETE /service/open/v2/apps/single-chatbot-accounts
```

**请求体**:

```json
{
  "appId": "app001",
  "accountId": "p_abc123"
}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `appId` | String | 是 | 应用业务 ID |
| `accountId` | String | 是 | 机器人账号 ID |

**响应** `ApiResponse<Void>`:

```json
{
  "code": "200",
  "messageZh": "解绑成功",
  "messageEn": "Unbind successful",
  "data": null
}
```

**处理逻辑**:
1. 通过 `app_id` 查询应用主键 `id`（不存在 → 40001）
2. 查询 `openplatform_app_p_t` WHERE `parent_id` = ? AND `property_name` = 'single_chatbot_account' AND `property_value` = ? AND `status` = 1
3. 记录不存在 → 40005
4. 硬删除：`DELETE FROM openplatform_app_p_t WHERE parent_id = ? AND property_name = 'single_chatbot_account' AND property_value = ? AND status = 1`
5. 添加审计日志注解 #ASSUMED

**错误码**:

| code | 含义 |
|------|------|
| 40001 | 应用不存在 |
| 40005 | 绑定记录不存在（该应用未绑定此账号） |
| 40101 | 未登录 |

---

### 2.3 数据模型定义

#### 2.3.1 DB Entity（内部使用，Long 类型）

```java
// AppPropertyEntity.java — 对应 openplatform_app_p_t
public class AppPropertyEntity {
    private Long id;              // 雪花 ID (DB: BIGINT)
    private Long parentId;        // 应用主键 (DB: BIGINT)
    private String propertyName;  // 属性键
    private String propertyValue; // 属性值
    private String tenantId;      // 租户
    private Integer status;       // 0=失效 1=有效
    private Date createTime;
    private String createBy;
    private Date lastUpdateTime;
    private String lastUpdateBy;
}
```

#### 2.3.2 API VO（对外输出，ID 为 String）

```java
// ChatbotAccountVO.java — API 响应对象
public class ChatbotAccountVO {
    private String id;        // 雪花 ID → String（防 JS 精度丢失）
    private String accountId; // 机器人账号 ID (property_value)
    private String bindTime;  // 绑定时间 yyyy-MM-dd HH:mm:ss
    private String bindBy;    // 绑定操作人
}
```

#### 2.3.3 请求 DTO

```java
// ChatbotBindRequest.java — POST/DELETE 请求体
public class ChatbotBindRequest {
    @NotBlank
    private String appId;

    @NotBlank
    @Pattern(regexp = "^p_.{1,196}$", message = "账号格式不正确")
    private String accountId;
}
```

#### 2.3.4 Entity → VO 转换

```java
// Service 层内部转换
private ChatbotAccountVO toVO(AppPropertyEntity entity) {
    ChatbotAccountVO vo = new ChatbotAccountVO();
    vo.setId(String.valueOf(entity.getId()));           // Long → String
    vo.setAccountId(entity.getPropertyValue());
    vo.setBindTime(DateUtil.format(entity.getCreateTime(), "yyyy-MM-dd HH:mm:ss"));
    vo.setBindBy(entity.getCreateBy());
    return vo;
}
```

---

### 2.4 Lookup 配置（待新建）

| 层级 | 字段 | 值 | 说明 |
|------|------|----|------|
| **Classify** | `classify_code` | `APP_CHATBOT_CONFIG` #ASSUMED | 应用机器人配置分类 |
| **Classify** | `path` | `/app` #ASSUMED | 分类路径 |
| **Item** | `item_code` | `MAX_SINGLE_CHATBOT_BINDABLE` #ASSUMED | 最大可绑定单聊机器人数量 |
| **Item** | `item_value` | `5` #ASSUMED（示例值） | 最大数量，整数字符串 |

> 具体 classify_code、item_code、path 和默认值均为 #ASSUMED，需上线前由开发/运维确认。

---

### 2.5 前端

#### 2.5.1 组件结构

```
src/router/routeRedBlue/
└── app-chatbot-bindtab/               # 新建模块目录
    ├── index.js                       # 主组件（Tab 入口）
    ├── index.module.less              # 样式
    ├── constant.js                    # 列配置、校验规则、常量
    ├── thunk.js                       # API 调用函数
    └── components/
        └── BindAccountModal.js        # 绑定弹窗
```

#### 2.5.2 主组件行为（index.js）

**Props 接口**:

| Prop | 类型 | 必填 | 说明 |
|------|------|------|------|
| `appId` | String | 是 | 当前应用的业务 ID，由父页面传入 |

**页面布局**:

```
┌─────────────────────────────────────────────────────────────┐
│  机器人绑定                                      [+ 绑定账号] │
├──────┬──────────────┬──────────────┬────────────────────────┤
│ 序号 │   账号 ID     │   绑定时间    │       操作             │
├──────┼──────────────┼──────────────┼────────────────────────┤
│  1   │ p_abc123     │ 06-10 14:30  │       解绑             │
│  2   │ p_def456     │ 06-10 13:00  │       解绑             │
│  3   │ p_ghi789     │ 06-09 10:22  │       解绑             │
└──────┴──────────────┴──────────────┴────────────────────────┘
```

**行为说明**:

1. **初始化**: 组件挂载 → 调用查询接口获取已绑定列表
2. **绑定流程**:
   - 点击「+ 绑定账号」→ 弹出 `BindAccountModal`
   - 前端格式校验（`p_` 开头，≤200 字符）
   - 确认 → 调用绑定接口 → 成功刷新列表
   - code=40003 → `message.error('已超过最大绑定数量')`
   - code=40004 → `message.error('该账号已绑定')`
3. **解绑流程**:
   - 点击「解绑」→ `ConfirmModal` 二次确认（含账号 ID）
   - 确认 → 调用解绑接口（body: `{ appId, accountId }`）→ 成功刷新列表
4. **空状态**: Ant Design `Empty` 组件

> **前端不获取、不展示数量上限**，完全依赖服务端返回的错误信息。  
> **前端不传递任何 DB 主键 ID**，仅使用 `appId`（业务 ID）和 `accountId`（账号 ID）操作。

#### 2.5.3 绑定弹窗（BindAccountModal.js）

**表单校验规则**:

| 规则 | 正则/条件 | 提示信息 |
|------|-----------|----------|
| 必填 | `required: true` | 请输入机器人账号 |
| 格式 | `pattern: /^p_.{1,196}$/` | 账号格式不正确，需以 p_ 开头，最长200字符 |

#### 2.5.4 API 配置（web.config.js 新增）

```javascript
APP_CHATBOT_ACCOUNTS: '/market-web/service/open/v2/apps/single-chatbot-accounts',
```

> 三个接口共用同一 URL，通过 HTTP Method（GET/POST/DELETE）区分。

#### 2.5.5 thunk.js API 函数

| 函数名 | HTTP | 参数位置 | 调用 |
|--------|------|----------|------|
| `fetchBoundAccounts(appId)` | GET | QueryParam | `fetchApi(URL + '?appId=' + encodeURIComponent(appId))` |
| `bindAccount(appId, accountId)` | POST | Body | `fetchApi(URL, { method:'POST', body: JSON.stringify({ appId, accountId }) })` |
| `unbindAccount(appId, accountId)` | DELETE | Body | `fetchApi(URL, { method:'DELETE', body: JSON.stringify({ appId, accountId }) })` |

---

## 3. Constraints

### 3.1 技术约束

| # | 约束 | 来源 |
|---|------|------|
| C-1 | 后端 SQL 不使用 `SELECT *`，显式列出字段 | [app-version-approval-spec.md] |
| C-2 | JOIN ≤ 3 张表，子查询嵌套 ≤ 3 层 | [app-version-approval-spec.md] |
| C-3 | 响应统一使用 `ApiResponse<T>` 包装 | [common/model/ApiResponse.java] |
| C-4 | DB 主键为雪花 ID（Long/BigInt），由 `IdGeneratorStrategy` 生成，**生成代码预留人工实现** | 用户确认 |
| C-5 | **所有 ID 字段返回前端时必须转为 String**，防 JS Number 精度丢失（雪花 ID > 2^53） | 用户确认 |
| C-6 | **DB Entity 与 API VO 严格分离**：Entity 用 `Long`，VO 用 `String` | 用户确认 |
| C-7 | 安全注解 `@AuthRole` 标记所有 Controller 方法 | [common/security/AuthRole.java] |
| C-8 | 审计日志通过 AOP 注解方式添加 #ASSUMED | 用户确认 |
| C-9 | 前端 React 18 + Ant Design 4.x + CSS Modules (Less) | [market-web/package.json] |
| C-10 | 前端 HTTP 使用 `fetchApi()`，不使用 axios | [market-web/src/utils/webFetch.js] |
| C-11 | 前端状态使用 `useState`（页面级） | 现有代码模式 |
| C-12 | 时间格式：`yyyy-MM-dd HH:mm:ss`，时区 `Asia/Shanghai` | [app-version-approval-spec.md] |
| C-13 | 国际化：响应 messageZh/messageEn 双语 | 现有代码模式 |
| C-14 | DELETE 请求参数放 Body（非 QueryParam） | 用户确认 |
| C-15 | 前端不获取也不展示绑定数量上限 | 用户确认 |
| C-16 | 前端不传递任何 DB 主键 ID，仅使用业务 ID（appId）和账号 ID（accountId） | 用户确认 |

### 3.2 数据约束

| # | 约束 | 说明 |
|---|------|------|
| D-1 | `property_value` VARCHAR(2000) ≥ 200 字符 accountId | [docs/app.sql:23-36] |
| D-2 | 同一应用下可存在多条 `single_chatbot_account` 记录 | EAV 模式 |
| D-3 | 解绑定位：`parent_id` + `property_name` + `property_value` + `status=1` | 业务规则 |
| D-4 | 绑定重复检查：同 D-3 条件 COUNT > 0 | 业务规则 |

---

## 4. Data

### 4.1 数据模型

```mermaid
erDiagram
    openplatform_app_t {
        bigint id PK "雪花ID"
        varchar app_id UK "业务ID"
        varchar tenant_id "租户ID"
        varchar app_name_cn "应用名称-中"
        tinyint app_type "0=个人 1=业务"
        tinyint status "0=失效 1=有效"
    }

    openplatform_app_p_t {
        bigint id PK "雪花ID"
        bigint parent_id FK "app_t.id"
        varchar property_name "属性键"
        varchar property_value "属性值-max200"
        varchar tenant_id "租户ID"
        tinyint status "0=失效 1=有效"
        datetime create_time "创建时间"
        varchar create_by "创建人"
    }

    openplatform_lookup_classify_t {
        bigint classifyId PK "雪花ID"
        varchar classify_code "分类编码"
        varchar path "分类路径"
        tinyint status "0=失效 1=有效"
    }

    openplatform_lookup_item_t {
        bigint itemId PK "雪花ID"
        bigint classifyId FK "classify_t.classifyId"
        varchar item_code "项编码"
        varchar item_value "项值-最大数量"
        tinyint status "0=失效 1=有效"
    }

    openplatform_app_t ||--o{ openplatform_app_p_t : "1:N parent_id"
    openplatform_lookup_classify_t ||--o{ openplatform_lookup_item_t : "1:N classifyId"
```

**`app_p_t` 记录示例（本次功能）**:

| id (BIGINT) | parent_id (BIGINT) | property_name | property_value | status |
|---|---|---|---|---|
| 1934567890123456789 | 1934000000000000500 | single_chatbot_account | p_abc123 | 1 |
| 1934567890123456790 | 1934000000000000500 | single_chatbot_account | p_def456 | 1 |
| 1934567890123456791 | 1934000000000000500 | single_chatbot_account | p_ghi789 | 1 |

> DB 层 id/parent_id 均为雪花 Long；API 层返回时转为 String。

### 4.2 数据操作矩阵

| 操作 | 表 | SQL | 条件 |
|------|-----|-----|------|
| 查询已绑定列表 | `app_p_t` | SELECT | `parent_id`=?, `property_name`='single_chatbot_account', `status`=1 |
| 绑定（插入） | `app_p_t` | INSERT | 新行（id 由 IdGeneratorStrategy 生成） |
| 解绑（删除） | `app_p_t` | DELETE | `parent_id`=?, `property_name`='single_chatbot_account', `property_value`=?, `status`=1 |
| 重复检查 | `app_p_t` | SELECT COUNT | 同解绑条件 |
| 数量上限检查 | `app_p_t` | SELECT COUNT | `parent_id`=?, `property_name`='single_chatbot_account', `status`=1 |
| 查应用主键 | `app_t` | SELECT id | `app_id`=?, `status`=1 |
| 查最大数量 | `lookup_item_t` | SELECT | JOIN `lookup_classify_t` |

### 4.3 对象层次关系

```mermaid
graph TD
    subgraph "DB 层 (Entity — Long ID)"
        Entity["AppPropertyEntity<br/>Long id<br/>Long parentId<br/>String propertyName<br/>String propertyValue"]
    end

    subgraph "Service 层 (转换)"
        Mapper2["ChatbotBindMapper<br/>MyBatis XML"]
        Converter["Entity → VO<br/>Long.toString()"]
    end

    subgraph "API 层 (VO — String ID)"
        VO["ChatbotAccountVO<br/>String id<br/>String accountId<br/>String bindTime<br/>String bindBy"]
        ReqDTO["ChatbotBindRequest<br/>String appId<br/>String accountId"]
    end

    Mapper2 -->|"SELECT/INSERT/DELETE"| Entity
    Entity --> Converter --> VO
    ReqDTO -->|"入参"| Controller
```

### 4.4 新增文件清单

#### 后端 (market-server)

```
modules/
└── chatbotbindtab/                          # 新模块（包名 #ASSUMED）
    ├── controller/
    │   └── ChatbotBindController.java       # 3 个接口
    ├── service/
    │   ├── ChatbotBindService.java          # 接口
    │   └── impl/ChatbotBindServiceImpl.java # 实现（含 Entity→VO 转换）
    ├── mapper/
    │   └── ChatbotBindMapper.java           # MyBatis Mapper 接口
    ├── entity/
    │   └── AppPropertyEntity.java           # DB 实体（Long id）
    ├── dto/
    │   └── ChatbotBindRequest.java          # 请求 DTO
    └── vo/
        └── ChatbotAccountVO.java            # 响应 VO（String id）

resources/mapper/
└── chatbotbindtab/
    └── ChatbotBindMapper.xml                # SQL 映射
```

#### 前端 (market-web)

```
src/router/routeRedBlue/
└── app-chatbot-bindtab/
    ├── index.js
    ├── index.module.less
    ├── constant.js
    ├── thunk.js
    └── components/
        └── BindAccountModal.js
```

#### 配置变更

| 文件 | 变更 |
|------|------|
| `web.config.js` | 新增 1 个 URL 常量 |
| Lookup 管理（运行时） | 新建 classify + item（§2.4） |

---

## 5. Test Cases

### 5.1 后端接口测试

#### TC-01: 查询已绑定账号 — 正常

| 项 | 内容 |
|----|------|
| 前置 | 应用 A（app_id=app001）已绑定 2 个账号 |
| 操作 | `GET /apps/single-chatbot-accounts?appId=app001` |
| 期望 | code=200，data 长度=2，id 字段为 String 类型（如 `"1934567890123456789"`），按 create_time DESC |

#### TC-02: 查询 — 应用不存在

| 项 | 内容 |
|----|------|
| 操作 | `GET /apps/single-chatbot-accounts?appId=nonexistent` |
| 期望 | code=40001 |

#### TC-03: 查询 — 无绑定记录

| 项 | 内容 |
|----|------|
| 前置 | 应用 B 存在但未绑定 |
| 操作 | `GET /apps/single-chatbot-accounts?appId=app002` |
| 期望 | code=200，data=[] |

#### TC-04: 绑定 — 正常

| 项 | 内容 |
|----|------|
| 前置 | 已绑定 2 个，上限=5 |
| 操作 | `POST /apps/single-chatbot-accounts` body: `{"appId":"app001","accountId":"p_newbot001"}` |
| 期望 | code=200，data.id 为 String 类型，app_p_t 新增一行 |

#### TC-05: 绑定 — 格式不合法

| 项 | 内容 |
|----|------|
| 操作 | POST body: `{"accountId":"invalid_no_prefix"}` |
| 期望 | code=40002 |

#### TC-06: 绑定 — 超过上限

| 项 | 内容 |
|----|------|
| 前置 | 已绑定 5 个，上限=5 |
| 操作 | POST body: `{"accountId":"p_overflow"}` |
| 期望 | code=40003 |

#### TC-07: 绑定 — 重复

| 项 | 内容 |
|----|------|
| 前置 | 已绑定 p_abc123 |
| 操作 | POST body: `{"accountId":"p_abc123"}` |
| 期望 | code=40004 |

#### TC-08: 解绑 — 正常

| 项 | 内容 |
|----|------|
| 前置 | 应用 A 绑定了 p_abc123 |
| 操作 | `DELETE /apps/single-chatbot-accounts` body: `{"appId":"app001","accountId":"p_abc123"}` |
| 期望 | code=200，对应行物理删除 |

#### TC-09: 解绑 — 记录不存在

| 项 | 内容 |
|----|------|
| 前置 | 应用 A 未绑定 p_nonexist |
| 操作 | `DELETE /apps/single-chatbot-accounts` body: `{"appId":"app001","accountId":"p_nonexist"}` |
| 期望 | code=40005 |

#### TC-10: 解绑 — 应用不存在

| 项 | 内容 |
|----|------|
| 操作 | `DELETE /apps/single-chatbot-accounts` body: `{"appId":"nonexistent","accountId":"p_abc123"}` |
| 期望 | code=40001 |

#### TC-11: 未登录

| 项 | 内容 |
|----|------|
| 前置 | 无登录态 |
| 操作 | 任意接口 |
| 期望 | HTTP 401 |

#### TC-12: ID 精度验证

| 项 | 内容 |
|----|------|
| 前置 | DB 中 app_p_t.id = 1934567890123456789 |
| 操作 | 查询接口返回 |
| 期望 | JSON 中 id 字段值为 `"1934567890123456789"`（String），非 `1934567890123456800`（Number 精度丢失） |

### 5.2 前端交互测试

#### TC-F01: 初始化加载

| 项 | 内容 |
|----|------|
| 操作 | 组件挂载，传入有效 appId |
| 期望 | 自动查询，表格渲染列表 |

#### TC-F02: 绑定 — 前端校验失败

| 项 | 内容 |
|----|------|
| 操作 | 输入 `invalid_account` → 确认 |
| 期望 | 校验拦截，不发请求 |

#### TC-F03: 绑定 — 服务端超限错误

| 项 | 内容 |
|----|------|
| 操作 | 提交合法 accountId，服务端返回 40003 |
| 期望 | `message.error` 提示超限 |

#### TC-F04: 绑定 — 服务端重复错误

| 项 | 内容 |
|----|------|
| 操作 | 提交已存在 accountId，服务端返回 40004 |
| 期望 | `message.error` 提示已绑定 |

#### TC-F05: 解绑 — 二次确认

| 项 | 内容 |
|----|------|
| 操作 | 点击「解绑」 |
| 期望 | ConfirmModal → 确认 → 删除 → 刷新；取消 → 无操作 |

#### TC-F06: 空状态

| 项 | 内容 |
|----|------|
| 操作 | 无绑定记录时挂载 |
| 期望 | 显示 Empty 组件 |

---

## 附录 A: #ASSUMED 标记汇总

| # | 标记内容 | 需确认方 | 影响 |
|---|----------|----------|------|
| A-1 | Lookup classify_code = `APP_CHATBOT_CONFIG` | 开发/运维 | 配置路径 |
| A-2 | Lookup item_code = `MAX_SINGLE_CHATBOT_BINDABLE` | 开发/运维 | 配置路径 |
| A-3 | Lookup path = `/app` | 开发/运维 | 分类归属 |
| A-4 | Lookup 默认值 = 5 | 产品/运维 | 业务上限 |
| A-5 | 审计日志注解名称 | 开发 | 代码实现 |
| A-6 | 新模块包名 `chatbotbindtab` | 开发 | 代码组织 |
| A-7 | tenant_id 从应用记录继承 | 开发 | 多租户 |
| A-8 | Lookup 读取失败降级策略 | 产品/开发 | 容错 |
| A-9 | 雪花 ID 生成代码（预留人工实现，本 Spec 不生成） | 开发 | ID 生成 |

---

## 附录 B: 时序图

### B.1 查询已绑定账号

```mermaid
sequenceDiagram
    participant FE as 前端 Tab
    participant API as ChatbotBindController
    participant Svc as ChatbotBindService
    participant DB as MySQL

    FE->>API: GET /apps/single-chatbot-accounts?appId=xxx
    API->>Svc: getBoundAccounts(appId)

    Svc->>DB: SELECT id FROM app_t WHERE app_id=? AND status=1
    DB-->>Svc: appPkId (Long, null → 40001)

    Svc->>DB: SELECT id, property_value, create_time, create_by FROM app_p_t WHERE parent_id=? AND property_name='single_chatbot_account' AND status=1 ORDER BY create_time DESC
    DB-->>Svc: List of AppPropertyEntity (id: Long)

    Svc->>Svc: Entity → VO 转换 (Long id → String id)

    Svc-->>API: List of ChatbotAccountVO (id: String)
    API-->>FE: 200 { data: [{ id: "1934...", accountId: "p_abc123", ... }] }
```

### B.2 绑定机器人账号

```mermaid
sequenceDiagram
    participant FE as 前端 Tab
    participant API as ChatbotBindController
    participant Svc as ChatbotBindService
    participant Lk as Lookup
    participant DB as MySQL

    FE->>FE: 前端校验: /^p_.{1,196}$/
    FE->>API: POST /apps/single-chatbot-accounts<br/>{ appId, accountId }
    API->>Svc: bindAccount(appId, accountId)

    Svc->>DB: SELECT id FROM app_t WHERE app_id=? AND status=1
    DB-->>Svc: appPkId (Long)

    Svc->>Lk: 查询 MAX_SINGLE_CHATBOT_BINDABLE
    Lk-->>Svc: maxCount (如 5)

    Svc->>DB: SELECT COUNT(*) FROM app_p_t WHERE parent_id=? AND property_name='single_chatbot_account' AND status=1
    DB-->>Svc: currentCount

    alt currentCount >= maxCount
        Svc-->>API: throw 40003 超过上限
    else 未超限
        Svc->>DB: SELECT COUNT(*) FROM app_p_t WHERE parent_id=? AND property_name='single_chatbot_account' AND property_value=? AND status=1
        DB-->>Svc: dupCount

        alt dupCount > 0
            Svc-->>API: throw 40004 重复绑定
        else 不重复
            Svc->>Svc: snowflakeId = IdGeneratorStrategy.nextId() 预留人工实现
            Svc->>DB: INSERT INTO app_p_t (id, parent_id, property_name, property_value, status, create_by, create_time, tenant_id)
            DB-->>Svc: OK
            Note over Svc: 审计日志注解
            Svc->>Svc: Entity → VO (Long id → String id)
            Svc-->>API: ChatbotAccountVO (id: String)
        end
    end

    API-->>FE: 200 { data: { id: "1934...", accountId: "p_abc123", ... } }
```

### B.3 解绑机器人账号

```mermaid
sequenceDiagram
    participant FE as 前端 Tab
    participant API as ChatbotBindController
    participant Svc as ChatbotBindService
    participant DB as MySQL

    FE->>API: DELETE /apps/single-chatbot-accounts<br/>{ appId, accountId }
    API->>Svc: unbindAccount(appId, accountId)

    Svc->>DB: SELECT id FROM app_t WHERE app_id=? AND status=1
    DB-->>Svc: appPkId (Long, null → 40001)

    Svc->>DB: SELECT id FROM app_p_t WHERE parent_id=? AND property_name='single_chatbot_account' AND property_value=? AND status=1
    DB-->>Svc: record (null → 40005)

    Svc->>DB: DELETE FROM app_p_t WHERE parent_id=? AND property_name='single_chatbot_account' AND property_value=? AND status=1
    DB-->>Svc: affected rows

    Note over Svc: 审计日志注解

    Svc-->>API: void
    API-->>FE: 200 { data: null }

    FE->>FE: 刷新列表
```

---

## 附录 C: 组件交互流程

```mermaid
flowchart TD
    A["外部详情页<br/>传入 appId"] --> B["index.js 主组件"]

    B -->|"useEffect 挂载"| C["fetchBoundAccounts(appId)"]
    C -->|"GET ?appId=xxx"| D["后端查询"]
    D -->|"List of ChatbotAccountVO<br/>id: String"| E["setDataSource"]
    E --> F["Table 渲染"]

    F -->|"点击 绑定账号"| G["BindAccountModal"]
    G -->|"输入 accountId"| H{"前端校验<br/>p_ 开头 / 200字符"}
    H -->|"通过"| I["bindAccount(appId, accountId)"]
    H -->|"失败"| J["表单错误提示"]
    I -->|"POST body: appId, accountId"| K{"后端响应"}
    K -->|"200"| L["关闭弹窗 / 刷新列表"]
    K -->|"40003"| M["message.error 超限"]
    K -->|"40004"| N["message.error 已绑定"]
    K -->|"其他"| O["message.error 通用错误"]

    F -->|"点击 解绑"| P["ConfirmModal"]
    P -->|"确认"| Q["unbindAccount(appId, accountId)"]
    P -->|"取消"| R["关闭"]
    Q -->|"DELETE body: appId, accountId"| S{"后端响应"}
    S -->|"200"| T["刷新列表"]
    S -->|"40005"| U["message.error 不存在"]

    E -->|"data 为空"| V["Empty 组件"]
```
