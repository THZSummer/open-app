# 需求设计说明书 — 应用绑定单聊机器人账号

## 修订记录

| 版本 | 日期 | 修改人 | 修改说明 |
|------|------|--------|---------|
| v1.0 | 2026-06-10 | SDDU Build Agent | 初稿，基于 app-single-chatbot-bindtab-spec.md v1.0 |
| v2.0 | 2026-06-22 | SDDU Build Agent | 基于 spec v2.0 同步更新：账号校验改为通讯录 API 远程校验（userType ∈ {4,5,10}）；数量上限来源改为 `openplatform_property_t`（默认值=1）；前端布局改为标签流式布局；审计日志预留人工实现；雪花 ID dev 环境已实现 |

## 目录
- 1 需求价值和概述
- 2 上下文分析
- 3 初始需求分析
    - 3.1 初始需求场景分析
    - 3.2 结构化IR
- 4 需求影响分析
    - 4.1 特性影响分析
- 5 系统用例分析
    - 5.1 用例清单
    - 5.2 用例分析
- 6 功能设计
    - 6.1 功能实现整体设计方案
    - 6.2 功能实现
- 7 系统级非功能设计
    - 7.1 FMEA影响分析
    - 7.2 安全影响分析
    - 7.3 兼容性
    - 7.4 可运维
- 8 checkList

## Keywords 关键字：
- 中文：机器人绑定、单聊机器人、应用属性、EAV 模式、数据字典、通讯录 API
- English：Chatbot Binding, Single Chat Bot, App Property, EAV Pattern, Data Dictionary, Contact API

## Abstract 摘要：

**中文**：本需求在 market-server（后端）和 market-web（前端）中实现应用绑定单聊机器人账号功能。在应用管理审批跳转的详情页内新增「机器人绑定」Tab，支持查询当前应用已绑定的单聊机器人账号列表、绑定新的机器人账号、解绑已有机器人账号。后端复用现有 `openplatform_app_p_t` 应用属性表（EAV 模式）存储绑定关系，绑定时调用通讯录 API 校验账号有效性（userType ∈ {4=机器人, 5=业务助手, 10=个人助手}），通过 `openplatform_property_t` 数据字典控制每个应用可绑定的最大数量（默认值=1，服务端校验）；前端采用标签流式布局展示已绑定账号，绑定弹窗仅做非空校验（有效性由后端校验），解绑时二次确认提示「确认删除该机器人账号吗？」。DB 主键为雪花 ID（Long），返回前端时转为 String 防止 JS 精度丢失，DB Entity 与 API VO 严格分离。

**English**: This requirement implements the chatbot account binding feature in market-server and market-web. A new "Chatbot Binding" tab is added to the application detail page. The backend reuses `openplatform_app_p_t` (EAV pattern) for storage, validates accounts via external Contact API (userType ∈ {4,5,10}), and controls max bind count via `openplatform_property_t` data dictionary (default=1, server-side). The frontend uses a flex-wrap tag layout for bound accounts, bind modal only validates non-empty (validity checked by backend), and unbind shows confirmation "确认删除该机器人账号吗？". DB Entity (Long) and API VO (String) are strictly separated.

## List 缩略语清单

| 缩略语 | 英文全名 | 中文解释 |
|--------|---------|---------|
| IR | Initial Requirement | 初始需求 |
| US | User Story | 用户故事 |
| DFX | Design for X | 面向X的设计（X=性能/安全/可靠性等） |
| FMEA | Failure Mode and Effects Analysis | 失效模式与影响分析 |
| EAV | Entity-Attribute-Value | 实体-属性-值（属性表存储模式） |
| Lookup | LookUp Configuration | （已废弃，改用数据字典 `openplatform_property_t`） |
| VO | Value Object | 值对象（API 响应对象） |
| DTO | Data Transfer Object | 数据传输对象（请求对象） |

---

## 1 需求价值和概述

### 需求背景与来源

开放平台（OpenPlatform v2）支持第三方应用接入，部分应用需要绑定单聊机器人账号以实现消息推送、客服应答等能力。此前，应用与机器人账号的绑定关系缺乏统一管理入口，运维人员需要通过数据库直接操作或分散在多个系统中管理。

当前 market-server 已具备应用属性表（`openplatform_app_p_t`）的 EAV 存储能力和 `openplatform_property_t` 数据字典管理能力，market-web 已有成熟的前端组件模式（Tag + Modal + fetchApi）。本次需求在此基础上，为应用详情页新增「机器人绑定」Tab，实现绑定关系的统一管理。绑定时通过通讯录 API 远程校验账号有效性，防止绑定非法账号。

应用详情页本身**不在现有代码中**（由外部系统集成，通过 `window.open('/app-detail/' + appId)` 跳转），本次仅开发独立的 Tab 组件供集成，以及对应的后端 API。

### 需求价值

| 维度 | 价值 |
|------|------|
| 统一管理 | 应用管理员可在应用详情页统一查看和管理绑定的机器人账号，无需操作数据库 |
| 安全可控 | 绑定数量由数据字典配置控制（默认上限=1），防止无限绑定；账号有效性通过通讯录 API 远程校验，杜绝非法账号 |
| 操作追溯 | 绑定/解绑操作审计日志预留人工实现（market-server 当前无 @AuditLog 注解） |
| 架构复用 | 复用现有 `app_p_t` EAV 模式和 `openplatform_property_t` 数据字典，无需新建表 |
| 精度安全 | 雪花 ID 返回前端统一转 String，杜绝 JS Number 精度丢失问题 |
| 可扩展 | Tab 组件独立封装，后续可扩展绑定群聊机器人等其他类型 |

### 如果不做的影响

- 应用管理员无法通过管理界面绑定/解绑机器人账号，需依赖数据库操作或联系开发
- 绑定数量无上限控制，存在滥用风险
- 绑定/解绑操作无审计记录，无法追溯
- 前端与后端 ID 精度不一致可能导致操作错误（雪花 ID 超过 JS 安全整数范围）

---

## 2 上下文分析

### 系统上下文

```mermaid
graph TB
    subgraph 应用绑定上下文
        admin(("应用管理员"))
        detail_page["应用详情页<br/>(外部系统，不在本仓库)"]
        chatbot_tab["机器人绑定 Tab 组件<br/>(本次新建，标签流式布局)"]
        market_web["market-web<br/>(React 18)"]
        market_server["market-server<br/>(port 18080)"]
        wecontact["通讯录 API<br/>(账号有效性校验)"]
        mysql[("MySQL 共享库<br/>(openapp)")]
        prop_t["openplatform_property_t<br/>(最大绑定数量)"]
    end

    admin -- "1. 在详情页操作机器人绑定 Tab" --> detail_page
    detail_page -- "2. 传入 appId prop" --> chatbot_tab
    chatbot_tab -- "3. fetchApi (REST)" --> market_server
    market_server -- "4. 查询/插入/删除 app_p_t" --> mysql
    market_server -- "5. 读取最大绑定数量" --> prop_t
    market_server -- "6. 校验账号有效性" --> wecontact
    market_server -- "7. 查询应用信息" --> mysql
```

### 利益相关方

| 利益相关方 | 关注点 |
|-----------|--------|
| 应用管理员 | 在应用详情页快速绑定/解绑机器人账号，查看已绑定列表 |
| 外部详情页集成方 | Tab 组件接口清晰（仅传入 appId），集成成本低 |
| 运维/审计 | 绑定/解绑操作有审计日志可追溯 |
| 后端开发 | 复用现有表结构，遵循 SQL 规范，Entity/VO 分离清晰 |

---

## 3 初始需求分析

### 3.1 初始需求场景分析

| 所属场景 | 场景名称 | 场景简要说明 | 涉及角色 |
|---------|---------|------------|---------|
| 机器人绑定管理 | 查看已绑定列表 | 管理员进入机器人绑定 Tab，查看当前应用已绑定的所有机器人账号 | 管理员 |
| 机器人绑定管理 | 绑定机器人账号 | 管理员点击「绑定账号」，输入账号 ID，提交后由后端调用通讯录 API 校验有效性 | 管理员 |
| 机器人绑定管理 | 解绑机器人账号 | 管理员点击标签上的「×」，二次确认「确认删除该机器人账号吗？」后解除绑定 | 管理员 |
| 机器人绑定管理 | 绑定超限拦截 | 绑定数量已达上限时，服务端拒绝绑定并返回错误提示 | 系统 |
| 机器人绑定管理 | 重复绑定拦截 | 绑定已存在的账号时，服务端拒绝并返回错误提示 | 系统 |
| 机器人绑定管理 | 通讯录 API 校验失败 | 账号不存在于通讯录或 userType 不合法（非机器人/业务助手/个人助手），服务端拒绝并返回 40002 | 系统 |

### 3.2 结构化IR

| IR属性 | 具体信息 |
|-------|---------|
| IR标识 | IR-MARKET-CHATBOT-BIND-001 |
| 名称 | 应用绑定单聊机器人账号 |
| 描述 | 在 market-server + market-web 实现应用绑定单聊机器人账号功能，支持查询/绑定/解绑操作 |
| 优先级 | P1（高） |
| 需求描述（why） | 应用管理员需要通过管理界面统一管理应用与单聊机器人账号的绑定关系，当前缺乏统一入口；绑定数量需配置化控制，操作需审计追溯 |
| what | ① 查询已绑定列表；② 绑定机器人账号（通讯录 API 校验+数量上限+重复校验）；③ 解绑机器人账号；④ 前端 Tab 组件（标签流式布局+绑定弹窗+解绑确认）；⑤ 数据字典配置最大绑定数量；⑥ 雪花 ID 转 String 防精度丢失 |
| who | 后端：market-server 开发；前端：market-web 开发；管理员使用 |
| 对架构要素的影响 | **架构**：新增 chatbotbindtab 模块，复用 app_p_t EAV 模式；**安全**：@AuthRole 登录态校验 + 审计日志注解；**精度**：Entity(Long) → VO(String) 转换 |

---

## 4 需求影响分析

### 4.1 特性影响分析

**【新增】**：

| 特性 | 说明 |
|------|------|
| 机器人绑定后端模块 | market-server 新增 chatbotbindtab 包（controller / service / client / mapper / entity / dto / vo） |
| 机器人绑定 Tab 组件 | market-web 新增 app-chatbot-bindtab 模块（标签流式布局 + thunk.js + BindAccountModal.js） |
| 通讯录 API 客户端 | market-server 新增 RestTemplateConfig + WeContactClient（调用通讯录 API 校验账号） |
| 数据字典配置 | `openplatform_property_t` 新增记录（path=`CEC.Open`, code=`single.chat.robot.max.number`, value=`1`） |

**【修改】**：

| 特性 | 影响说明 |
|------|---------|
| market-web API 配置 | web.config.js 新增 1 个 API URL（APP_CHATBOT_ACCOUNTS） |

**【删除】**：不涉及

---

## 5 系统用例分析

### 5.1 用例清单

| 角色名称 | UseCase名称 | UseCase简要说明 | 是否需要细化分析 |
|---------|-----------|---------------|:-------------:|
| 管理员 | UC-01 查看已绑定列表 | 进入机器人绑定 Tab，加载已绑定账号列表 | 否 |
| 管理员 | UC-02 绑定机器人账号 | 点击「绑定账号」→ 输入账号 → 提交绑定（后端通讯录 API 校验） | 是 |
| 管理员 | UC-03 解绑机器人账号 | 点击「解绑」→ 二次确认 → 调用接口 → 刷新列表 | 是 |
| 系统 | UC-04 绑定超限拦截 | 绑定数量已达上限时，服务端拒绝并返回错误码 | 否 |
| 系统 | UC-05 重复绑定拦截 | 绑定已存在的账号时，服务端拒绝并返回错误码 | 否 |
| 系统 | UC-06 通讯录 API 校验拦截 | 账号不存在或 userType 不合法时，后端拒绝并返回 40002 | 否 |

### 5.2 用例分析

#### UC-02 绑定机器人账号

**【简要说明】**：管理员在机器人绑定 Tab 中点击「绑定账号」，输入机器人账号 ID，前端仅做非空校验后提交绑定请求，服务端调用通讯录 API 校验账号有效性（userType ∈ {4,5,10}），再执行数量上限校验和重复校验后完成绑定。

**【Actor】**：管理员

**【前置条件】**：
- 管理员已登录且具备 @AuthRole 权限
- 当前应用存在且有效（status=1）
- 已绑定数量未达到数据字典配置的上限（默认值=1）
- 通讯录 API 可用

**【最小保证】**：操作失败时，绑定关系不变，前端展示错误信息

**【成功保证】**：
- `openplatform_app_p_t` 新增一行记录：parent_id=应用主键, property_name='single_chatbot_account', property_value=账号ID
- 审计日志记录操作人、操作类型、时间（预留人工实现）

**【主成功场景】**：
1. 管理员点击「+ 绑定账号」按钮
2. 前端弹出 BindAccountModal，输入框获得焦点
3. 管理员输入账号 ID（如 `p_order_bot_001`）
4. 管理员点击「确认绑定」
5. 前端调用 `POST /service/open/v2/apps/single-chatbot-accounts`，body: `{ appId, accountId }`
6. 后端查询应用主键 → 调用通讯录 API 校验账号有效性（userType ∈ {4,5,10}）→ 查询数据字典上限 → 查询已绑定数 → 重复检查
7. 校验全部通过 → INSERT `openplatform_app_p_t`
8. 后端返回 `{ code: "200", data: { id: "1934...", accountId, bindTime, bindBy } }`
9. 前端关闭弹窗，展示 `message.success('绑定成功')`，刷新标签列表

**【扩展场景】**：
- **E1 前端必填校验失败**：未输入任何内容 → 表单红色错误提示，不发送请求
- **E2 应用不存在**：后端返回 code=40001，前端 `message.error` 提示
- **E3 通讯录 API 返回账号无效**：后端返回 code=40002，前端 `message.error('账号无效')`
- **E4 超过绑定上限**：后端返回 code=40003，前端 `message.error('已超过最大绑定数量')`
- **E5 重复绑定**：后端返回 code=40004，前端 `message.error('该账号已绑定')`
- **E6 通讯录 API 调用失败**：后端返回 code=40006，前端 `message.error('通讯录服务暂不可用')`
- **E7 管理员取消**：点击「取消」或弹窗外区域，关闭弹窗，不发送请求

**【DFX属性】**：安全（@AuthRole 登录态校验）、审计（预留人工实现）、精度安全（ID 转 String）

#### UC-03 解绑机器人账号

**【简要说明】**：管理员点击某行的「解绑」链接，系统弹出二次确认框，确认后调用解绑接口，服务端通过 appId + accountId 定位记录并硬删除。

**【Actor】**：管理员

**【前置条件】**：
- 管理员已登录
- 列表中存在已绑定的机器人账号

**【最小保证】**：操作失败时，绑定关系不变，前端展示错误信息

**【成功保证】**：
- `openplatform_app_p_t` 中对应记录被物理删除
- 审计日志记录操作

**【主成功场景】**：
1. 管理员点击标签上的「×」
2. 前端弹出 ConfirmModal，仅展示提示「确认删除该机器人账号吗？」，按钮「确认」「取消」
3. 管理员点击「确认」
4. 前端调用 `DELETE /service/open/v2/apps/single-chatbot-accounts`，body: `{ appId, accountId }`
5. 后端查询应用主键 → 查询绑定记录（parent_id + property_name + property_value + status=1）
6. 记录存在 → DELETE FROM `openplatform_app_p_t` WHERE ...
7. 后端返回 `{ code: "200" }`
8. 前端展示 `message.success('解绑成功')`，刷新列表

**【扩展场景】**：
- **E1 应用不存在**：后端返回 code=40001，前端展示错误信息
- **E2 绑定记录不存在**：后端返回 code=40005，前端展示错误信息
- **E3 管理员取消确认**：Modal 关闭，不发起请求

**【DFX属性】**：安全（@AuthRole）、审计（AOP 注解）

#### 5.2.1 影响的功能列表和需求分解

| 功能编号 | 功能名称 | 功能规格描述 | 类型 | 需求标号 | 需求名称 | 需求描述 |
|---------|---------|------------|------|---------|---------|---------|
| F-01 | 查询已绑定列表 | 根据 appId 查询 `app_p_t` 中 property_name='single_chatbot_account' 的记录，Entity → VO（Long→String） | 新增 | IR-001 | 查看已绑定列表 | 查应用主键 → 查属性记录 → 转 VO 返回 |
| F-02 | 绑定机器人账号 | 通讯录 API 校验 + 上限校验 + 重复校验 + INSERT app_p_t | 新增 | IR-001 | 绑定账号 | 通讯录校验 → 查上限(property_t) → 查重 → 插入 → 审计 |
| F-03 | 解绑机器人账号 | 根据 appId + accountId 定位记录并硬删除 | 新增 | IR-001 | 解绑账号 | 查应用主键 → 查记录 → DELETE → 审计 |
| F-04 | 前端 Tab 组件 | 列表展示 + 绑定弹窗 + 解绑确认 + 错误提示 | 新增 | IR-001 | 机器人绑定页面 | Table + Modal + fetchApi + 错误处理 |
| F-05 | API 配置 | web.config.js 新增 URL 常量 | 修改 | IR-001 | 前端配置 | 新增 1 个 API URL |
| F-06 | 数据字典配置 | `openplatform_property_t` 新增记录控制最大绑定数量 | 新增 | IR-001 | 运行时配置 | 运维在数据字典管理中创建（path=`CEC.Open`, code=`single.chat.robot.max.number`, value=`1`） |

---

## 6 功能设计

### 6.1 功能实现整体设计方案

#### 6.1.1 整体方案

**设计原则**：
- **复用优先**：复用现有 `app_p_t` EAV 存储和 `openplatform_property_t` 数据字典，不新建业务表
- **远程校验**：账号有效性通过通讯录 API 远程校验（userType ∈ {4,5,10}），不做本地正则校验
- **精度安全**：DB Entity（Long）与 API VO（String）严格分离，杜绝 JS 精度丢失
- **前端轻量**：useState 页面级状态，标签流式布局（flex-wrap），不获取不展示数量上限
- **接口统一**：三个操作共用同一 URL 路径，通过 HTTP Method 区分

**限制和约束**：
- 应用详情页由外部系统集成，本次仅开发独立 Tab 组件
- 前端不传递任何 DB 主键 ID，仅使用 appId（业务 ID）和 accountId（账号 ID）
- 雪花 ID dev 环境 `DevIdGeneratorStrategy` 已实现；标准环境 `StandardIdGeneratorStrategy` 待实现
- 审计日志预留人工实现（market-server 当前无 @AuditLog 注解）
- `tenantId` 配置到 `application.yml`；`token` 由人工通过工具类获取

#### 6.1.2 架构设计

**后端架构**：

```mermaid
graph TD
    web["market-web<br/>(React 标签流式布局)"]
    controller["ChatbotBindController<br/>3 个端点<br/>@AuthRole"]
    service["ChatbotBindServiceImpl<br/>业务编排 + Entity→VO"]
    wcClient["WeContactClient<br/>(RestTemplate 调用通讯录 API)"]
    dictMapper["DictionaryMapper<br/>(读取数量上限)"]
    mapper["ChatbotBindMapper<br/>MyBatis XML"]
    idgen["IdGeneratorStrategy<br/>(dev 已实现 / standard 待实现)"]

    web -- "HTTP (REST)" --> controller
    controller --> service
    service --> wcClient
    service --> dictMapper
    service --> mapper
    service --> idgen
    mapper --> db_app["app_t (查应用主键)"]
    mapper --> db_prop["app_p_t (查/插/删)"]
    dictMapper --> db_dict["openplatform_property_t (数据字典)"]
    wcClient -- "POST + tenantId/token" --> wecontact["通讯录 API<br/>(外部系统)"]
```

**前端架构**：

```mermaid
graph TD
    detail["应用详情页<br/>(外部系统)"]
    tab["app-chatbot-bindtab/index.js<br/>Tab 主组件"]
    thunk["thunk.js<br/>API 调用"]
    modal["BindAccountModal.js<br/>绑定弹窗"]
    confirm["ConfirmModal<br/>解绑确认"]
    config["web.config.js<br/>URL 常量"]

    detail -- "appId prop" --> tab
    tab --> thunk
    tab --> modal
    tab --> confirm
    thunk --> config
    thunk -- "fetchApi()" --> backend["market-server API"]
```

**对象层次**：

```mermaid
graph LR
    subgraph "DB Entity (Long)"
        E["AppPropertyEntity<br/>Long id<br/>Long parentId"]
    end
    subgraph "Service 转换"
        C["toVO()<br/>Long.toString()"]
    end
    subgraph "API VO (String)"
        V["ChatbotAccountVO<br/>String id<br/>String accountId"]
    end
    subgraph "前端 JS"
        JS["安全渲染<br/>无精度丢失"]
    end

    E --> C --> V --> JS
```

---

### 6.2 功能实现

#### F-01 查询已绑定列表

##### 实现思路

通过 `app_id` 查应用主键，再通过 `parent_id` + `property_name` 查属性表，Entity → VO 转换后返回。

##### 实现设计

```mermaid
sequenceDiagram
    participant FE as 前端 Tab
    participant MS as market-server
    participant DB as MySQL

    FE->>MS: GET /service/open/v2/apps/single-chatbot-accounts?appId=xxx
    MS->>DB: SELECT id FROM app_t WHERE app_id=? AND status=1
    DB-->>MS: appPkId (Long, null → 40001)
    MS->>DB: SELECT id, property_value, create_time, create_by<br/>FROM app_p_t<br/>WHERE parent_id=?<br/>AND property_name='single_chatbot_account'<br/>AND status=1<br/>ORDER BY create_time DESC
    DB-->>MS: List of AppPropertyEntity
    MS->>MS: Entity → VO (Long → String)
    MS-->>FE: 200 { data: [{ id: "1934...", accountId, bindTime, bindBy }] }
```

##### 接口设计

| URL | Method | 功能 | 增删改查 | 鉴权 | TPS | 时延 |
|-----|--------|------|---------|------|-----|------|
| `/service/open/v2/apps/single-chatbot-accounts` | GET | 查询已绑定列表 | 查 | @AuthRole | 50 | <200ms |

**输入参数**：

| 参数 | 类型 | 必填 | 格式 | 说明 |
|------|------|:----:|------|------|
| appId | String | 是 | QueryParam | 应用业务 ID（app_t.app_id） |

**返回值**：

| 字段 | 类型 | 说明 |
|------|------|------|
| code | String | "200"=成功 |
| messageZh | String | 中文消息 |
| messageEn | String | 英文消息 |
| data | Array | 绑定账号列表 |
| data[].id | String | 记录 ID（雪花 ID → String） |
| data[].accountId | String | 机器人账号 ID |
| data[].bindTime | String | 绑定时间 yyyy-MM-dd HH:mm:ss |
| data[].bindBy | String | 绑定操作人 |

---

#### F-02 绑定机器人账号

##### 实现思路

查应用主键 → 调用通讯录 API 校验账号有效性 → 查数据字典上限 → 查已绑定数 → 重复检查 → INSERT → Entity→VO → 审计日志（预留）。

##### 实现设计

```mermaid
sequenceDiagram
    participant FE as 前端 Tab
    participant MS as market-server
    participant WC as WeContactClient<br/>(通讯录 API)
    participant DB as MySQL

    FE->>MS: POST /service/open/v2/apps/single-chatbot-accounts<br/>{ appId, accountId }
    MS->>DB: SELECT id FROM app_t WHERE app_id=? AND status=1
    DB-->>MS: appPkId
    MS->>WC: POST /wecontact-relation/v1/relation/personPublicInfo<br/>{ users: [accountId] }
    WC-->>MS: { code:"0", users:[{userType:10,...}] }
    alt 通讯录 API 异常 或 users 为空 或 userType ∉ {4,5,10}
        MS-->>FE: 40002 账号无效 / 40006 API 调用失败
    end
    MS->>DB: SELECT value FROM openplatform_property_t<br/>WHERE path='CEC.Open' AND code='single.chat.robot.max.number'
    DB-->>MS: maxCount (默认 1)
    MS->>DB: SELECT COUNT(*) FROM app_p_t WHERE parent_id=? AND property_name='single_chatbot_account' AND status=1
    DB-->>MS: currentCount
    alt currentCount >= maxCount
        MS-->>FE: 40003 超过上限
    else 未超限
        MS->>DB: SELECT COUNT(*) FROM app_p_t WHERE parent_id=? AND property_name=? AND property_value=? AND status=1
        DB-->>MS: dupCount
        alt dupCount > 0
            MS-->>FE: 40004 重复绑定
        else 通过
            MS->>MS: 生成雪花 ID (DevIdGeneratorStrategy.nextId())
            MS->>DB: INSERT INTO app_p_t (id, parent_id, property_name, property_value, status, create_by, create_time, tenant_id)
            DB-->>MS: OK
            Note over MS: 审计日志（预留人工实现）
            MS->>MS: Entity → VO
            MS-->>FE: 200 { data: { id, accountId, bindTime, bindBy } }
        end
    end
```

##### 接口设计

| URL | Method | 功能 | 增删改查 | 鉴权 | TPS | 时延 |
|-----|--------|------|---------|------|-----|------|
| `/service/open/v2/apps/single-chatbot-accounts` | POST | 绑定机器人账号 | 增 | @AuthRole | 20 | <300ms |

**请求 Body**：

| 字段 | 类型 | 必填 | 格式 | 说明 |
|------|------|:----:|------|------|
| appId | String | 是 | @NotBlank | 应用业务 ID |
| accountId | String | 是 | @NotBlank（后端通过通讯录 API 校验有效性） | 机器人账号 ID |

**返回值**：

| code | messageZh | 场景 |
|------|-----------|------|
| 200 | 绑定成功 | 正常 |
| 40001 | 应用不存在 | app_id 查询为空 |
| 40002 | 账号无效 | 通讯录 API 返回空 users 或 userType ∉ {4,5,10} |
| 40003 | 超过最大可绑定数量 | 已绑定数 >= 数据字典上限 |
| 40004 | 该账号已绑定 | 重复绑定检查 |
| 40006 | 通讯录 API 调用失败 | 网络超时 / 服务不可用 |

---

#### F-03 解绑机器人账号

##### 实现思路

通过 appId 查应用主键，再通过 parent_id + property_name + property_value 定位记录，硬删除。

##### 实现设计

```mermaid
sequenceDiagram
    participant FE as 前端 Tab
    participant MS as market-server
    participant DB as MySQL

    FE->>MS: DELETE /service/open/v2/apps/single-chatbot-accounts<br/>{ appId, accountId }
    MS->>DB: SELECT id FROM app_t WHERE app_id=? AND status=1
    DB-->>MS: appPkId (null → 40001)
    MS->>DB: SELECT id FROM app_p_t WHERE parent_id=? AND property_name='single_chatbot_account' AND property_value=? AND status=1
    DB-->>MS: record (null → 40005)
    MS->>DB: DELETE FROM app_p_t WHERE parent_id=? AND property_name='single_chatbot_account' AND property_value=? AND status=1
    DB-->>MS: affected rows
    Note over MS: 审计日志注解
    MS-->>FE: 200 { data: null }
    FE->>FE: 刷新列表
```

##### 接口设计

| URL | Method | 功能 | 增删改查 | 鉴权 | TPS | 时延 |
|-----|--------|------|---------|------|-----|------|
| `/service/open/v2/apps/single-chatbot-accounts` | DELETE | 解绑机器人账号 | 删 | @AuthRole | 20 | <300ms |

**请求 Body**：

| 字段 | 类型 | 必填 | 格式 | 说明 |
|------|------|:----:|------|------|
| appId | String | 是 | @NotBlank | 应用业务 ID |
| accountId | String | 是 | @NotBlank | 机器人账号 ID |

**返回值**：

| code | messageZh | 场景 |
|------|-----------|------|
| 200 | 解绑成功 | 正常 |
| 40001 | 应用不存在 | app_id 查询为空 |
| 40005 | 绑定记录不存在 | 该应用未绑定此账号 |

---

#### F-04 前端 Tab 组件

##### 实现思路

遵循 market-web 现有页面模式：index.js（useState 状态管理）+ index.module.less（CSS Modules）+ constant.js（常量）+ thunk.js（API 调用）。通过 webFetch.js 的 `fetchApi()` 发起请求。独立 Tab 组件供外部详情页集成。

##### 界面原型

> 浏览器打开 [`docs/market-server/app-chatbot-bindtab-mockup.html`](./app-chatbot-bindtab-mockup.html) 查看可交互效果图。

**页面结构**：

```mermaid
graph TD
    subgraph "机器人绑定 Tab（标签流式布局）"
        toolbar["工具栏<br/>「+ 绑定账号」按钮"]
        tags["Tag 标签容器<br/>flex-wrap 自动换行"]
        tag1["p_order_bot_001 ×"]
        tag2["p_order_notify_bot ×"]
        tag3["p_cs_assistant ×"]
        empty["Empty 空状态<br/>暂无绑定"]
    end

    subgraph "弹窗"
        bindmodal["BindAccountModal<br/>输入框 + 非空校验 + 确认/取消"]
        unbindconfirm["ConfirmModal<br/>仅提示：确认删除该机器人账号吗？<br/>按钮：确认 / 取消"]
        errormodal["错误提示<br/>message.error"]
    end

    toolbar --> tags
    tags --> tag1
    tags --> tag2
    tags --> tag3
    toolbar --> bindmodal
    tag1 --> unbindconfirm
    tag2 --> unbindconfirm
    tag3 --> unbindconfirm
    bindmodal --> errormodal
```

**交互说明**：

| 操作 | 触发 | 行为 |
|------|------|------|
| 初始化 | Tab 挂载 | 自动调用 GET 接口加载列表 → 渲染标签 |
| 绑定 | 点击「+ 绑定账号」 | 展开内联输入框（作为 tag 跟在账号标签后）→ 输入账号 → POST 绑定 → 刷新标签；输入状态下点击页面其他区域自动触发保存 |
| 解绑 | 点击标签「×」 | ConfirmModal 仅展示「确认删除该机器人账号吗？」+ 按钮「确认」「取消」→ DELETE 解绑 → 刷新标签 |
| 账号无效 | 服务端返回 40002 | message.error('账号无效') |
| 超限 | 服务端返回 40003 | message.error('已超过最大绑定数量') |
| 重复 | 服务端返回 40004 | message.error('该账号已绑定') |
| 通讯录异常 | 服务端返回 40006 | message.error('通讯录服务暂不可用') |
| 空状态 | 标签为空 | 显示 Empty 组件 |

##### 架构元素影响列表

| 元素 | 变更类型 | 说明 |
|------|---------|------|
| `web.config.js` | 修改 | 新增 APP_CHATBOT_ACCOUNTS URL 常量 |
| `routeRedBlue/app-chatbot-bindtab/` | 新增 | 5 个文件（index.js, index.module.less, constant.js, thunk.js, components/BindAccountModal.js） |

---

#### F-05 API 配置

web.config.js 新增 1 个常量：

```javascript
APP_CHATBOT_ACCOUNTS: '/market-web/service/open/v2/apps/single-chatbot-accounts',
```

三个接口共用同一 URL，通过 HTTP Method（GET/POST/DELETE）区分。

---

#### F-06 数据字典配置

从 `openplatform_property_t` 读取绑定数量上限，复用现有 `DictionaryMapper.selectByPathAndCode()`：

| 字段 | 值 | 说明 |
|------|----|------|
| `path` | `CEC.Open` | 数据字典路径 |
| `code` | `single.chat.robot.max.number` | 最大可绑定数量编码 |
| `value` | `1`（默认值，运维可调整） | 最大数量 |

> **数据字典记录新增由人工实现，不在本次 Spec 范围。**

---

#### 3.7 功能实现分解分配清单

| # | Task 名称 | 模块 | 职责描述 |
|:-:|----------|------|---------|
| 1 | 绑定常量定义 | market-server | ChatbotBindConstants — property_name、错误码等 |
| 2 | DB 实体类 | market-server | AppPropertyEntity.java — 对应 app_p_t（Long id，新建不复用 open-server） |
| 3 | 请求 DTO | market-server | ChatbotBindRequest.java — appId + accountId |
| 4 | 响应 VO | market-server | ChatbotAccountVO.java — id(String) + accountId + bindTime + bindBy |
| 5 | 通讯录 DTO | market-server | WeContactRequest.java + WeContactResponse.java #ASSUMED |
| 6 | RestTemplate 配置 | market-server | RestTemplateConfig.java（参考 event-server） |
| 7 | 通讯录客户端 | market-server | WeContactClient.java — 调用通讯录 API 校验账号 |
| 8 | Mapper 接口 | market-server | ChatbotBindMapper.java |
| 9 | Mapper XML | market-server | SQL 实现（SELECT/INSERT/DELETE，显式字段） |
| 10 | Service 接口 + 实现 | market-server | ChatbotBindService + Impl — 业务编排 + 通讯录校验 + Entity→VO 转换 |
| 11 | Controller | market-server | ChatbotBindController — 3 个端点 |
| 12 | 前端常量 + API 配置 | market-web | constant.js + web.config.js 修改 |
| 13 | 前端 API 调用层 | market-web | thunk.js — fetchBoundAccounts / bindAccount / unbindAccount |
| 14 | 前端 Tab 主组件 | market-web | index.js + index.module.less — 标签流式布局 + 工具栏 + 空状态 |
| 15 | 前端绑定弹窗 | market-web | BindAccountModal.js — 输入框 + 非空校验 |

---

## 7 系统级非功能设计

### 7.1 FMEA影响分析

| 失效模式 | 影响 | 缓解措施 |
|---------|------|---------|
| 并发绑定导致超限 | 实际绑定数超过数据字典配置上限 | Service 层查询+插入在同一事务中；如需更强保证可增加 DB 层 UNIQUE 约束或乐观锁 |
| 通讯录 API 不可用 | 绑定操作无法校验账号有效性 | 返回 40006 错误，前端展示「通讯录服务暂不可用」 |
| 数据字典配置不存在或读取失败 | 绑定操作无法获取上限值 | 使用默认值 1（#ASSUMED） |
| JS 精度丢失 | 前端渲染或传递错误的 ID 值 | Entity→VO 层统一 Long→String 转换，前端全程使用 String |
| 解绑时 accountId 含特殊字符 | URL 编码问题导致找不到记录 | 解绑参数放 Body（非 URL path），避免编码问题 |

### 7.2 安全影响分析

| 安全项 | 措施 |
|-------|------|
| 接口鉴权 | @AuthRole 注解，未登录返回 401 |
| 输入校验 | accountId 通过通讯录 API 远程校验（userType ∈ {4,5,10}），@NotBlank 必填校验 |
| 越权防护 | 解绑时校验 parent_id 与 appId 对应的主键一致，防止操作其他应用的绑定 |
| 审计追溯 | 审计日志预留人工实现（market-server 当前无 @AuditLog） |

### 7.3 兼容性

#### 后向兼容性确认

- 复用现有 `app_p_t` 表，不修改表结构
- 新增 property_name='single_chatbot_account'，不影响现有属性
- API 新增端点，不影响现有接口

#### 前向兼容性确认

- Tab 组件独立封装，通过 appId prop 接入，集成方只需传一个参数
- EAV 模式天然支持扩展：后续可新增其他 property_name 存储不同类型的绑定关系
- 数据字典可扩展：后续可增加其他 property_t 记录控制更多参数

### 7.4 可运维

| 运维项 | 说明 |
|-------|------|
| 日志 | 绑定/解绑操作审计日志预留人工实现 |
| 配置 | 数据字典可在管理界面动态修改最大绑定数量，无需发版 |
| 数据查询 | 可通过 `SELECT * FROM openplatform_app_p_t WHERE property_name='single_chatbot_account'` 直接查询所有绑定关系 |
| 数据修复 | 绑定关系为简单 KV 记录，可通过 SQL 直接增删修复 |

---

## 8 checkList

### 8.1 设计自检清单要求

| check点 | 是否达标 | 说明 |
|--------|:-------:|------|
| 需求背景和价值清晰 | ✅ | 第1章已说明 |
| 用例场景完整覆盖 | ✅ | 6 个用例覆盖查询、绑定、解绑、超限、重复、通讯录校验 |
| 接口定义明确（输入/输出/错误码） | ✅ | 3 个 API 均定义完整参数、返回值、6 个错误码（含 40006） |
| 数据模型清晰 | ✅ | 复用 app_p_t 表 + 数据字典 property_t |
| SQL 规范（禁止 SELECT *，JOIN ≤ 3 表） | ✅ | 所有 SQL 显式列出字段，无 JOIN（单表操作） |
| 安全设计（鉴权 + 越权防护） | ✅ | @AuthRole + 解绑时 parent_id 校验 |
| ID 精度安全 | ✅ | Entity(Long) → VO(String) 严格分离，前端无 Long ID |
| 前端界面原型 | ✅ | HTML 效果图可交互预览（标签流式布局） |
| 可扩展性设计 | ✅ | EAV 模式 + 数据字典配置，后续可扩绑定类型和参数 |
| 前后端技术栈一致 | ✅ | 后端 Spring Boot 3.4.6 / Java 21 / MyBatis；前端 React 18 / AntD v4 / JS |
| 测试用例覆盖 | ✅ | 后端 14 条 + 前端 7 条 |
| 文件清单完整 | ✅ | 后端 11 个新文件（含 RestTemplateConfig + WeContactClient）+ 前端 5 个新文件 + 1 个配置修改 |
| 业务表结构 | ✅ | 复用 openplatform_app_p_t，DDL 见 docs/market-server/app.sql |
| #ASSUMED 标记 | ✅ | 8 项 #ASSUMED 已标注，需上线前确认 |
