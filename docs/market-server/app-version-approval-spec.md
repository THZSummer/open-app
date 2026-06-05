# market-server 通用审批管理模块 — 技术规格书（Spec）

> **版本**: v8.0 | **日期**: 2026-06-04 | **状态**: 待评审
>
> **效果图**: 浏览器打开 [`approval-page-mockup.html`](./approval-page-mockup.html) 可交互预览

---

## 1. Scope（范围）

### 1.1 模块职责

market-server 提供**通用审批管理**能力，供 open-server 发起的审批流程进行审批操作。模块设计为通用引擎 + 策略扩展，支持多种 businessType。

**包含**：
- 待审批列表查询（分页）
- 已审批列表查询（分页 + 状态筛选）
- 审批操作（通过/驳回统一接口，按 action 字段区分）
- 策略模式：`ApprovalHandler` 处理审批通过/驳回后的业务副作用
- 策略模式：`BusinessDataResolver` 解析不同 businessType 的业务展示数据
- 前端审批管理页面（列表 + 操作）

**不包含**：
- 审批流创建/编排（由 open-server 负责）
- 审批流模板 CRUD（由 open-server 负责）
- 催办/转办/撤回（后续迭代）
- 审批详情页（后续迭代）
- 搜索/过滤功能（暂时不提供，后续迭代）
- 业务表 DDL（App / Version / Capability 表结构由其他 spec 提供）`#PLACEHOLDER`

### 1.2 菜单与页面策略

当前页面固定为**一种审批类型**（如版本发布审批），列表无需审批类型筛选。后续新增其他审批类型时，**拆分为独立菜单页**，每种类型一个页面。

### 1.3 核心约束

| 约束 | 说明 | 来源 |
|------|------|------|
| 共享数据库 | market-server 与 open-server 连接同一 MySQL `openapp` 库 | `[src/market-server/resources/application.yml]` |
| Entity 同构 | ApprovalRecord / ApprovalLog / ApprovalFlow 与 open-server 字段完全一致 | `[src/open-server/.../approval/entity/]` |
| combinedNodes 快照 | ApprovalRecord 存储 combinedNodes JSON 快照，不依赖 flowId | `[src/open-server/.../approval/entity/ApprovalRecord.java:30]` |
| businessType 由 open-server 决定 | market-server 不硬编码 businessType，通过策略模式路由 | `#DESIGN_DECISION` |
| 操作人来源 | 从 `UserContextHolder` 获取（Web 端调用），非 IM 回调 | `[src/market-server/.../security/UserContextHolder.java]` |
| **SQL 禁止 SELECT \*** | 所有查询必须明确列出字段名 | `#SQL_RULE` |
| **连表不超过 3 张** | JOIN 查询不超过 3 张表（含子查询），子查询嵌套不超过 3 层 | `#SQL_RULE` |

### 1.4 SQL 编写规范

#### 规则 1：禁止 `SELECT *`

```sql
-- ✗ 禁止
SELECT * FROM openplatform_v2_approval_record_t

-- ✓ 正确
SELECT id, business_type, business_id, applicant_id, applicant_name,
       status, current_node, create_time, completed_at
FROM openplatform_v2_approval_record_t
```

#### 规则 2：连表 ≤ 3 张

```sql
-- ✓ 正确：主表 + 2 张关联表 = 3 张
SELECT r.id, r.business_type, v.version_no, a.name_cn
FROM openplatform_v2_approval_record_t r
LEFT JOIN app_version_t v ON r.business_id = v.id
LEFT JOIN app_t a ON v.app_id = a.id

-- ✗ 禁止：4 张表 JOIN
SELECT ... FROM r
LEFT JOIN v ON ...
LEFT JOIN a ON ...
LEFT JOIN app_capability_rel_t acr ON ...  -- 超出限制
```

#### 规则 3：子查询嵌套 ≤ 3 层

```sql
-- ✓ 正确：2 层嵌套
SELECT ... FROM r WHERE r.business_id IN (
    SELECT v.id FROM app_version_t v WHERE v.app_id IN (
        SELECT a.id FROM app_t a WHERE a.name_cn LIKE '%keyword%'
    )
)
```

#### 多表数据获取策略

当需要展示的数据涉及 >3 张表时，采用以下策略：

| 策略 | 说明 | 适用场景 |
|------|------|---------|
| 主查询 + 代码补查 | 主 SQL 连表 ≤3 张获取核心数据，Service 层用 Java 代码补充查询 | 能力名称列表、标签等 |
| BusinessDataResolver | 不同 businessType 各自实现 Resolver，内部按规范查询 | 业务展示数据 |

---

## 2. Interface（接口）

### 2.1 后端 API（3 个端点）

**Base Path**: `/service/open/v2/approvals`
**端口**: 18080
**认证**: `@AuthRole`（market-server 自定义权限注解）`[src/market-server/.../security/AuthRole.java]`

#### API-1: 待审批列表

```
GET /service/open/v2/approvals/pending
```

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|:----:|------|
| curPage | Integer | 否 | 当前页，默认 1 |
| pageSize | Integer | 否 | 每页条数，默认 10 |

> **说明**：当前不提供搜索/过滤参数，后续迭代按需增加。

**响应**（`ApiResponse`）`[src/market-server/.../model/ApiResponse.java]`：

```json
{
  "code": "200",
  "messageZh": "成功",
  "messageEn": "Success",
  "data": [
    {
      "id": 1,
      "businessType": "app_version_publish",
      "businessTypeName": "版本发布审批",
      "businessId": "1001",
      "appId": "app_third_party_001",
      "appNameCn": "订单管理应用",
      "appNameEn": "Order Management App",
      "versionNo": "v2.1.0",
      "capabilityNames": "订单查询, 订单创建",
      "applicantId": "u001",
      "status": 0,
      "currentNode": 0,
      "createTime": "2026-06-03 10:23:45"
    }
  ],
  "page": {
    "curPage": 1,
    "pageSize": 10,
    "total": 5,
    "totalPages": 1
  }
}
```

**响应字段说明**：

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 审批记录 ID |
| businessType | String | 业务类型 |
| businessTypeName | String | 业务类型中文名 |
| businessId | String | 业务 ID（版本 ID） |
| appId | String | 第三方应用 ID（从应用属性表获取）`#PLACEHOLDER` |
| appNameCn | String | 应用中文名 |
| appNameEn | String | 应用英文名 |
| versionNo | String | 版本号 |
| capabilityNames | String | 应用关联能力中文名（逗号分隔），由 Service 层补查 `#CODE_ENRICH` |
| applicantId | String | 申请人账号 ID |
| status | Integer | 审批状态 |
| currentNode | Integer | 当前节点索引 |
| createTime | String | 申请时间 |

**主查询 SQL**（3 表 JOIN：approval_record + version + app）：

```sql
SELECT
    r.id,
    r.business_type,
    r.business_id,
    r.applicant_id,
    r.status,
    r.current_node,
    r.create_time,
    v.version_no,
    a.app_id,
    a.name_cn,
    a.name_en
FROM openplatform_v2_approval_record_t r
LEFT JOIN app_version_t v ON r.business_id = v.id
LEFT JOIN app_t a ON v.app_id = a.id
WHERE r.status = 0
ORDER BY r.create_time DESC
LIMIT #{offset}, #{pageSize}
```

**能力名称补查**（Service 层 Java 代码，单独查询，不增加主查询表数）：

```sql
-- 根据 version_id 查询关联能力名称（2 表 JOIN）
SELECT c.name_cn
FROM app_capability_rel_t acr
LEFT JOIN capability_t c ON acr.capability_id = c.id
WHERE acr.version_id = #{versionId}
```

> `[src/open-server/.../mapper/ApprovalRecordMapper.xml]`: `selectPendingList` 方法参考
>
> **注意**: 表名 `app_t`, `app_version_t`, `app_capability_rel_t`, `capability_t` 为 `#PLACEHOLDER`，实际表结构由其他 spec 提供。

**计数 SQL**：

```sql
SELECT COUNT(*)
FROM openplatform_v2_approval_record_t
WHERE status = 0
```

#### API-2: 已审批列表

```
GET /service/open/v2/approvals/processed
```

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|:----:|------|
| status | Integer | 否 | 状态筛选：1=已通过, 2=已驳回, 3=已撤回 |
| curPage | Integer | 否 | 当前页，默认 1 |
| pageSize | Integer | 否 | 每页条数，默认 10 |

**响应**：同 API-1 结构，`status` 包含已通过/已驳回/已撤回记录。

**主查询 SQL**（3 表 JOIN，同 API-1 结构，条件不同）：

```sql
SELECT
    r.id,
    r.business_type,
    r.business_id,
    r.applicant_id,
    r.status,
    r.current_node,
    r.create_time,
    r.completed_at,
    v.version_no,
    a.app_id,
    a.name_cn,
    a.name_en
FROM openplatform_v2_approval_record_t r
LEFT JOIN app_version_t v ON r.business_id = v.id
LEFT JOIN app_t a ON v.app_id = a.id
WHERE r.status IN (1, 2, 3)
  AND (#{status} IS NULL OR r.status = #{status})
ORDER BY COALESCE(r.completed_at, r.last_update_time) DESC
LIMIT #{offset}, #{pageSize}
```

能力名称补查同 API-1。

**计数 SQL**：

```sql
SELECT COUNT(*)
FROM openplatform_v2_approval_record_t
WHERE status IN (1, 2, 3)
  AND (#{status} IS NULL OR status = #{status})
```

#### API-3: 审批操作（通过/驳回统一接口）

```
POST /service/open/v2/approvals/{id}/process
```

**路径参数**：`id` — ApprovalRecord ID

**请求 Body**：
```json
{
  "action": 0,
  "comment": "同意发布"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|:----:|------|
| action | Integer | 是 | 0=通过（APPROVE）, 1=驳回（REJECT） |
| comment | String | 驳回时必填 | 审批意见 |

**响应**：
```json
{
  "code": "200",
  "messageZh": "操作成功",
  "messageEn": "Operation successful",
  "data": null
}
```

**业务逻辑**（参考 `[src/open-server/.../engine/ApprovalEngine.java]` approve/reject 方法）：

```
1. 查询 ApprovalRecord by id
2. 校验 status == PENDING(0)，否则返回错误
3. 解析 combinedNodes JSON → List<ApprovalNodeDto>
4. 获取 currentNode 索引对应的节点
5. 校验操作人（UserContextHolder.getUserId()）== 节点 userId
6. 校验 action 值（0 或 1）
7. 插入 ApprovalLog（action=请求体 action, comment=请求体 comment）
8. 按 action 分支：
   ┌─ action=0（通过）:
   │    判断是否最后一个节点：
   │    - 是 → record.status = APPROVED(1), record.completedAt = now
   │           → handler.onApproved(record)   // 策略回调
   │    - 否 → record.currentNode += 1
   │    更新 ApprovalRecord
   │
   └─ action=1（驳回）:
        校验 comment 非空（否则返回 40005）
        record.status = REJECTED(2), record.completedAt = now
        handler.onRejected(record)   // 策略回调
        更新 ApprovalRecord
```

**错误码**：

| code | messageZh | 场景 |
|------|-----------|------|
| 40001 | 审批记录不存在 | record 查询为空 |
| 40002 | 审批记录已处理，当前状态：{status} | status != PENDING |
| 40003 | 当前节点审批人不匹配 | userId 校验失败 |
| 40004 | 不支持的业务类型：{businessType} | handler 未注册 |
| 40005 | 驳回时必须填写审批意见 | action=1 且 comment 为空 |
| 40006 | 无效的操作类型：{action} | action 不是 0 或 1 |

---

### 2.2 前端页面

#### 2.2.1 路由与菜单

**路由**: `src/router/index.tsx` `[src/market-web/router/index.tsx]`

```jsx
<Route path="approval" element={<Approval />} />
```

**菜单**: `src/components/Layout/index.js` `[src/market-web/components/Layout/index.js]`

菜单仅展示"应用管理"一级菜单项，审批管理作为子路由存在但不单独显示为菜单项。后续新增审批类型时拆分为独立菜单页。

```js
// menuItems 数组：
{ key: '/app', icon: <AppstoreOutlined />, label: '应用管理' }
// 审批管理路由存在但不作为独立菜单项
```

#### 2.2.2 页面结构

```
/approval
├── index.js            # 页面主文件（useState, useEffect, 事件处理）
├── index.module.less   # CSS Modules 样式
├── constant.js         # 常量（API 配置 key, 状态枚举, 列定义）
└── thunk.js            # API 调用封装（fetchApi + buildApiUrl）
```

> 参考模式：`[src/market-web/pages/LookUpClassify/]` 的文件结构

#### 2.2.3 API 配置

**`src/configs/web.config.js`** 新增 `[src/market-web/configs/web.config.js]`：

```js
// 审批管理
APPROVAL_PENDING_LIST: '/market-web/service/open/v2/approvals/pending',
APPROVAL_PROCESSED_LIST: '/market-web/service/open/v2/approvals/processed',
APPROVAL_PROCESS: '/market-web/service/open/v2/approvals/{id}/process',
```

#### 2.2.4 页面设计

**页面结构**：Tabs + 表格 + 分页（无搜索栏，后续迭代按需增加）

**Tab 切换**：
- **待审批**：显示 status=PENDING(0) 的记录，带红色数字角标
- **已审批**：显示 status=APPROVED(1)/REJECTED(2)/CANCELLED(3) 的记录

**表格列**：

| 列名 | 字段 | 宽度 | 渲染 |
|------|------|------|------|
| 应用名称 | appNameCn / appNameEn | 150px | 根据当前语言切换展示（中/英文）`#I18N_SWITCH` |
| 版本号 | versionNo | 90px | 文本 |
| 应用ID | appId | 120px | 文本（第三方应用 ID，`#PLACEHOLDER`） |
| 关联能力 | capabilityNames | 160px | 文本（逗号分隔，超长省略） |
| 申请人 | applicantId | 100px | 文本（账号 ID） |
| 申请时间 | createTime | 155px | yyyy-MM-dd HH:mm:ss |
| 状态 | status | 80px | Tag 组件（待审批=warning, 已通过=success, 已驳回=danger, 已撤回=default） |
| 操作 | - | 150px | 按钮组（见下） |

**语言切换逻辑**：

```js
// 应用名称列根据语言环境展示
const renderAppName = (record) => {
  return currentLang === 'en' ? record.appNameEn : record.appNameCn;
};
```

**操作按钮**：

| 按钮 | 待审批 Tab | 已审批 Tab | 行为 |
|------|:----------:|:----------:|------|
| 详情 | ✓ | ✓ | `navigate('/app-detail/' + record.businessId)` — 跳转应用详情页 `#ASSUMED` |
| 同意 | ✓ | — | `Modal.confirm` 二次确认 → 调用 process API（action=0）→ 成功后 `message.success('审批通过')` + `fetchData()` |
| 拒绝 | ✓ | — | `Modal` 弹窗 + TextArea（拒绝原因必填）→ 调用 process API（action=1）→ 成功后 `message.success('已拒绝')` + `fetchData()` |

**交互流程**：

```
同意操作:
  1. 点击"同意"按钮
  2. Modal.confirm 弹出确认框
  3. 用户确认 → 调用 thunk.processApproval(id, { action: 0 })
  4. 检查 result.code === '200'
  5. 成功 → message.success('审批通过') + fetchData() 刷新列表
  6. 失败 → message.error({ content: result?.messageZh || '操作失败' })

拒绝操作:
  1. 点击"拒绝"按钮
  2. Modal 弹窗显示 TextArea
  3. 用户输入拒绝原因（必填校验）→ 点击"确认拒绝"
  4. 调用 thunk.processApproval(id, { action: 1, comment })
  5. 检查 result.code === '200'
  6. 成功 → message.success('已拒绝') + 关闭 Modal + fetchData() 刷新列表
  7. 失败 → message.error({ content: result?.messageZh || '操作失败' })
```

**关键代码模式**：

```js
// thunk.js — API 调用
import { fetchApi, buildApiUrl } from '../../utils/webFetch';
import { API_CONFIG } from './constant';

export const fetchPendingList = async (params) => {
  const url = buildApiUrl(API_CONFIG.APPROVAL_PENDING_LIST) + '?' + new URLSearchParams(params);
  try {
    return await fetchApi(url, { method: 'GET' }) || {};
  } catch (err) { return {}; }
};

export const fetchProcessedList = async (params) => {
  const url = buildApiUrl(API_CONFIG.APPROVAL_PROCESSED_LIST) + '?' + new URLSearchParams(params);
  try {
    return await fetchApi(url, { method: 'GET' }) || {};
  } catch (err) { return {}; }
};

// 审批操作（通过/驳回统一接口）
export const processApproval = async (id, data) => {
  try {
    return await fetchApi(
      buildApiUrl(API_CONFIG.APPROVAL_PROCESS, { id }),
      { method: 'POST', body: JSON.stringify(data) }
    ) || {};
  } catch (err) { return {}; }
};
```

```js
// constant.js
export const API_CONFIG = {
  APPROVAL_PENDING_LIST: 'APPROVAL_PENDING_LIST',
  APPROVAL_PROCESSED_LIST: 'APPROVAL_PROCESSED_LIST',
  APPROVAL_PROCESS: 'APPROVAL_PROCESS',
};

export const APPROVAL_STATUS = {
  PENDING: 0,
  APPROVED: 1,
  REJECTED: 2,
  CANCELLED: 3,
};

export const APPROVAL_ACTION = {
  APPROVE: 0,
  REJECT: 1,
};

export const STATUS_MAP = {
  [APPROVAL_STATUS.PENDING]:  { text: '待审批', color: '#ff9f00', bg: '#fff5e0' },
  [APPROVAL_STATUS.APPROVED]: { text: '已通过', color: '#00b578', bg: '#e6f7f0' },
  [APPROVAL_STATUS.REJECTED]: { text: '已驳回', color: '#f54a45', bg: '#fde8e8' },
  [APPROVAL_STATUS.CANCELLED]:{ text: '已撤回', color: '#999',    bg: '#f5f5f5' },
};
```

```js
// index.js — 状态管理（useState，无 Zustand）
const [activeTab, setActiveTab] = useState('pending');
const [dataSource, setDataSource] = useState([]);
const [loading, setLoading] = useState(false);
const [pagination, setPagination] = useState({ curPage: 1, pageSize: 10, total: 0 });
const [rejectModalVisible, setRejectModalVisible] = useState(false);
const [currentRecord, setCurrentRecord] = useState(null);
const [rejectComment, setRejectComment] = useState('');
const [currentLang, setCurrentLang] = useState(localStorage.getItem('lang') || 'zh');
```

---

## 3. Constraints（约束）

### 3.1 后端技术约束

| 约束 | 值 |
|------|-----|
| 框架 | Spring Boot 3.4.6 |
| JDK | Java 21 |
| ORM | MyBatis（纯 XML Mapper，无注解 SQL） |
| 构建 | Maven |
| 端口 | 18080 |
| Context Path | `/market-server` |
| JSON | Jackson `[src/market-server/.../config/JacksonConfig.java]` — 时间格式 `yyyy-MM-dd HH:mm:ss`，时区 `Asia/Shanghai` |
| **SQL 规范** | 禁止 `SELECT *`；JOIN ≤ 3 张表；子查询嵌套 ≤ 3 层 |

### 3.2 前端技术约束

| 约束 | 值 |
|------|-----|
| 框架 | React 18.2.0 |
| UI 库 | Ant Design 4.24.16（v4，非 v5） |
| 语言 | JavaScript（非 TypeScript） |
| 样式 | CSS Modules + Less |
| HTTP | webFetch.js（原生 fetch 封装），`fetchApi()` + `buildApiUrl()` |
| 状态 | useState（页面级），无 Zustand |
| 国际化 | 硬编码中文文本，应用名称列按语言切换中/英文 |
| 路由 | react-router-dom v6 |

### 3.3 数据库约束

- 共享库：`openapp`（MySQL）
- 审批三表已由 open-server 创建：`openplatform_v2_approval_record_t` / `openplatform_v2_approval_log_t` / `openplatform_v2_approval_flow_t`
- market-server 只做 SELECT + UPDATE（不 INSERT 审批记录，不 CREATE 审批流）
- combinedNodes 字段为 JSON 快照，market-server 只读取不修改

---

## 4. Data（数据）

### 4.1 审批表结构（已有，open-server 创建）

> 以下来自 `[src/open-server/.../approval/entity/]` + `[docs/sql/01-init-schema.sql]`

#### openplatform_v2_approval_record_t

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT AUTO_INCREMENT | 主键 |
| combined_nodes | JSON | 审批节点快照 `[{"type":"resource","userId":"u001","userName":"张三","order":1,"level":1}]` |
| business_type | VARCHAR(50) | 业务类型（如 `app_version_publish`） |
| business_id | VARCHAR(64) | 业务 ID |
| applicant_id | VARCHAR(64) | 申请人账号 ID |
| applicant_name | VARCHAR(100) | 申请人姓名 |
| status | TINYINT | 0=待审批, 1=已通过, 2=已驳回, 3=已撤回 |
| current_node | INT | 当前审批节点索引（从 0 开始） |
| create_time | DATETIME | 创建时间 |
| last_update_time | DATETIME | 最后更新时间 |
| create_by | VARCHAR(64) | 创建人 |
| last_update_by | VARCHAR(64) | 最后更新人 |
| completed_at | DATETIME | 完成时间 |

**索引**：
- `PRIMARY KEY (id)`
- `idx_business (business_type, business_id)` — 按业务查询
- `idx_status (status)` — 按状态查询
- `idx_applicant (applicant_id)` — 按申请人查询

#### openplatform_v2_approval_log_t

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT AUTO_INCREMENT | 主键 |
| record_id | BIGINT | 关联审批记录 |
| node_index | INT | 审批节点索引 |
| level | INT | 审批层级 |
| operator_id | VARCHAR(64) | 操作人 ID |
| operator_name | VARCHAR(100) | 操作人姓名 |
| action | TINYINT | 0=通过, 1=驳回, 2=撤回, 3=转办, 4=催办 |
| comment | TEXT | 审批意见 |
| create_time | DATETIME | 操作时间 |

#### openplatform_v2_approval_flow_t

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT AUTO_INCREMENT | 主键 |
| name_cn | VARCHAR(100) | 流程中文名 |
| name_en | VARCHAR(100) | 流程英文名 |
| code | VARCHAR(50) UNIQUE | 流程编码 |
| description_cn | VARCHAR(500) | 描述中文 |
| description_en | VARCHAR(500) | 描述英文 |
| nodes | JSON | 节点配置 |
| status | TINYINT | 0=禁用, 1=启用 |

### 4.2 业务表结构

`#PLACEHOLDER` — App / Version / Capability 表结构由其他 spec 文档提供。

预期涉及表（供 Resolver 实现参考）：

| 表名（占位） | 说明 |
|-------------|------|
| `app_t` | 应用主表（含 app_id, name_cn, name_en） |
| `app_version_t` | 版本表（含 version_no, app_id 外键） |
| `app_capability_rel_t` | 应用版本-能力关联表 |
| `capability_t` | 能力表（含 name_cn） |

---

## 5. Architecture（架构设计）

### 5.1 后端架构（策略模式）

```
ApprovalController（3 个端点）
    │
    ▼
ApprovalServiceImpl（流程编排）
    ├─ 1. 主查询审批记录（3 表 JOIN: record + version + app）
    ├─ 2. Service 层补查能力名称（单独查询，不增加主查询表数）
    ├─ 3. 组装 VO（含 appNameCn/appNameEn/versionNo/appId/capabilityNames/applicantId）
    │
    ├─ 审批操作（process 统一入口）:
    │    ├─ 状态/权限校验
    │    ├─ 解析 combinedNodes
    │    ├─ 插入 ApprovalLog
    │    ├─ 更新 ApprovalRecord（状态/节点推进）
    │    └─ 策略路由 ──→ ApprovalHandlerFactory
    │                          │
    │                ┌─────────┼──────────┐
    │                ▼         ▼          ▼
    │        AppVersion   ApiPermission  EventPermission ...
    │        PublishHandler ApplyHandler  ApplyHandler
    │                │         │          │
    │                ▼         ▼          ▼
    │          版本状态更新   subscription  subscription
    │          (未来实现)    status 更新    status 更新
```

### 5.2 核心接口设计

#### ApprovalHandler（策略接口）

```java
public interface ApprovalHandler {
    /** 返回该 Handler 支持的 businessType */
    String getBusinessType();

    /** 审批最终通过后的业务副作用 */
    void onApproved(ApprovalRecord record);

    /** 审批驳回后的业务副作用 */
    void onRejected(ApprovalRecord record);
}
```

#### ApprovalHandlerFactory（策略工厂）

```java
@Component
public class ApprovalHandlerFactory {
    private final List<ApprovalHandler> handlers;
    private final Map<String, ApprovalHandler> handlerMap = new HashMap<>();

    @PostConstruct
    public void init() {
        for (ApprovalHandler handler : handlers) {
            handlerMap.put(handler.getBusinessType(), handler);
        }
    }

    public ApprovalHandler getHandler(String businessType) {
        return handlerMap.get(businessType);
    }
}
```

#### BusinessDataResolver（业务数据解析器）

```java
public interface BusinessDataResolver {
    /** 返回支持的 businessType */
    String getBusinessType();

    /**
     * 解析业务展示数据
     *
     * <p>返回业务相关展示字段（应用中英文名、版本号、应用ID、能力名称等）。
     * 注意：内部查询需遵守 SQL 规范（禁止 SELECT *，JOIN ≤ 3 表）</p>
     *
     * @param businessId 业务 ID
     * @return 业务展示数据 Map
     */
    Map<String, Object> resolveBusinessData(String businessId);

    /** 返回 businessType 的中文名称 */
    String getBusinessTypeName();
}
```

#### BusinessDataResolverFactory

```java
@Component
public class BusinessDataResolverFactory {
    private final List<BusinessDataResolver> resolvers;
    private final Map<String, BusinessDataResolver> resolverMap = new HashMap<>();

    @PostConstruct
    public void init() {
        for (BusinessDataResolver resolver : resolvers) {
            resolverMap.put(resolver.getBusinessType(), resolver);
        }
    }

    public BusinessDataResolver getResolver(String businessType) {
        return resolverMap.get(businessType);
    }
}
```

### 5.3 ApprovalEngine（简化版）

参考 `[src/open-server/.../engine/ApprovalEngine.java]`，market-server 仅保留：

| 方法 | 说明 | 对应 open-server |
|------|------|-----------------|
| `process(recordId, action, comment)` | 统一审批操作（通过/驳回） | `approve()` + `reject()` 合并 |

**不包含**（open-server 有但 market-server 不需要）：
- `composeApprovalNodes()` — 审批流由 open-server 创建
- `createApproval()` — 审批记录由 open-server 发起
- `cancel()` — 撤回功能后续迭代
- `transfer()` — 转办功能后续迭代

**process 方法伪代码**：

```java
@Transactional(rollbackFor = Exception.class)
public void process(Long recordId, int action, String comment) {
    // 1. 查询记录
    ApprovalRecord record = recordMapper.selectById(recordId);
    if (record == null) throw new BizException(40001, "审批记录不存在");

    // 2. 状态校验
    if (record.getStatus() != STATUS_PENDING) {
        throw new BizException(40002, "审批记录已处理，当前状态：" + record.getStatus());
    }

    // 3. 解析节点 + 操作人校验
    List<ApprovalNodeDto> nodes = parseNodes(record.getCombinedNodes());
    ApprovalNodeDto currentNode = nodes.get(record.getCurrentNode());
    if (!currentNode.getUserId().equals(UserContextHolder.getUserId())) {
        throw new BizException(40003, "当前节点审批人不匹配");
    }

    // 4. 获取策略 handler
    ApprovalHandler handler = handlerFactory.getHandler(record.getBusinessType());
    if (handler == null) {
        throw new BizException(40004, "不支持的业务类型：" + record.getBusinessType());
    }

    // 5. 插入日志
    insertLog(record, currentNode, action, comment);

    // 6. 按 action 分支处理
    Date now = new Date();
    if (action == ACTION_APPROVE) {
        if (record.getCurrentNode() >= nodes.size() - 1) {
            record.setStatus(STATUS_APPROVED);
            record.setCompletedAt(now);
            recordMapper.update(record);
            handler.onApproved(record);
        } else {
            record.setCurrentNode(record.getCurrentNode() + 1);
            recordMapper.update(record);
        }
    } else if (action == ACTION_REJECT) {
        if (comment == null || comment.isBlank()) {
            throw new BizException(40005, "驳回时必须填写审批意见");
        }
        record.setStatus(STATUS_REJECTED);
        record.setCompletedAt(now);
        recordMapper.update(record);
        handler.onRejected(record);
    } else {
        throw new BizException(40006, "无效的操作类型：" + action);
    }
}
```

### 5.4 常量定义

参考 `[src/open-server/.../engine/ApprovalEngine.java]` 内部枚举：

```java
// 状态
public static final int STATUS_PENDING = 0;
public static final int STATUS_APPROVED = 1;
public static final int STATUS_REJECTED = 2;
public static final int STATUS_CANCELLED = 3;

// 操作
public static final int ACTION_APPROVE = 0;
public static final int ACTION_REJECT = 1;
public static final int ACTION_CANCEL = 2;
public static final int ACTION_TRANSFER = 3;
public static final int ACTION_URGE = 4;
```

### 5.5 扩展方式

新增 businessType 审批支持，只需：

1. 实现 `ApprovalHandler` 接口 + `@Component`
2. 实现 `BusinessDataResolver` 接口 + `@Component`
3. 工厂自动注册，Controller / Service / Engine 无需修改

新增审批类型菜单页：每种类型独立页面，独立路由，独立菜单项。

---

## 6. File Manifest（文件清单）

### 6.1 后端新建文件（19 个）

> 路径前缀：`market-server/src/main/java/com/xxx/it/works/wecode/v2/`

| # | 文件路径 | 说明 |
|:-:|---------|------|
| 1 | `approval/controller/ApprovalController.java` | 3 个端点（pending, processed, process） |
| 2 | `approval/service/ApprovalService.java` | 接口 |
| 3 | `approval/service/impl/ApprovalServiceImpl.java` | 实现（编排 Engine + HandlerFactory + ResolverFactory，含能力名称补查） |
| 4 | `approval/engine/ApprovalEngine.java` | 简化版审批引擎（process 统一方法） |
| 5 | `approval/handler/ApprovalHandler.java` | 策略接口 |
| 6 | `approval/handler/ApprovalHandlerFactory.java` | 策略工厂 |
| 7 | `approval/resolver/BusinessDataResolver.java` | 业务数据解析接口 |
| 8 | `approval/resolver/BusinessDataResolverFactory.java` | 解析器工厂 |
| 9 | `approval/entity/ApprovalRecord.java` | 实体（同构 open-server） |
| 10 | `approval/entity/ApprovalLog.java` | 实体（同构 open-server） |
| 11 | `approval/entity/ApprovalFlow.java` | 实体（同构 open-server） |
| 12 | `approval/mapper/ApprovalRecordMapper.java` | Mapper 接口 |
| 13 | `approval/mapper/ApprovalLogMapper.java` | Mapper 接口 |
| 14 | `approval/mapper/ApprovalFlowMapper.java` | Mapper 接口 |
| 15 | `approval/dto/ApprovalNodeDto.java` | 节点 DTO |
| 16 | `approval/dto/ApprovalListRequest.java` | 列表查询请求 |
| 17 | `approval/dto/ApprovalProcessRequest.java` | 审批操作请求（action + comment） |
| 18 | `approval/vo/ApprovalListVo.java` | 列表展示 VO（含 appNameCn, appNameEn, versionNo, appId, capabilityNames, applicantId） |
| 19 | `approval/constant/ApprovalConstants.java` | 状态/操作常量 |

> MyBatis XML（路径前缀 `market-server/src/main/resources/mapper/`）：

| # | 文件路径 | 说明 |
|:-:|---------|------|
| 20 | `approval/ApprovalRecordMapper.xml` | SQL（selectPendingList, selectProcessedList, selectById, update, countPending, countProcessed）— 所有查询明确列出字段，JOIN ≤ 3 表 |
| 21 | `approval/ApprovalLogMapper.xml` | SQL（insert） |

### 6.2 前端新建文件（4 个）

> 路径前缀：`market-web/src/pages/`

| # | 文件路径 | 说明 |
|:-:|---------|------|
| 1 | `Approval/index.js` | 页面主文件 |
| 2 | `Approval/index.module.less` | 样式 |
| 3 | `Approval/constant.js` | 常量 |
| 4 | `Approval/thunk.js` | API 调用 |

### 6.3 前端修改文件（3 个）

| # | 文件路径 | 变更 |
|:-:|---------|------|
| 1 | `market-web/src/router/index.tsx` | 新增 `<Route path="approval" element={<Approval />} />` |
| 2 | `market-web/src/components/Layout/index.js` | 菜单仅展示"应用管理"，审批路由存在但不单独显示菜单项 |
| 3 | `market-web/src/configs/web.config.js` | 新增 3 个 API URL 配置（pending, processed, process） |

---

## 7. Test Cases（测试用例）

### 7.1 后端测试用例（13 条）

#### 列表查询

| # | 用例 | 预期 |
|:-:|------|------|
| T-01 | GET pending — 无参数 | 返回所有 status=0 的记录，含 appNameCn/appNameEn/versionNo/appId/capabilityNames/applicantId，按 createTime DESC 排序 |
| T-02 | GET pending — 分页 curPage=2, pageSize=5 | 返回第 6-10 条记录，page.total 正确 |
| T-03 | GET pending — 无记录 | data=[], page.total=0 |
| T-04 | GET processed — 无参数 | 返回所有 status IN (1,2,3) 的记录 |
| T-05 | GET processed — status=1 筛选 | 仅返回已通过记录 |
| T-06 | GET processed — status=2 筛选 | 仅返回已驳回记录 |

#### 审批操作

| # | 用例 | 预期 |
|:-:|------|------|
| T-07 | POST process action=0 — 正常通过（单节点） | status → APPROVED(1), completedAt 非空, handler.onApproved 执行 |
| T-08 | POST process action=0 — 多节点非最后 | currentNode += 1, status 仍为 PENDING(0), handler 不触发 |
| T-09 | POST process action=0 — 多节点最后节点 | status → APPROVED(1), handler.onApproved 执行 |
| T-10 | POST process action=1 — 正常驳回 | status → REJECTED(2), log.comment 有值, handler.onRejected 执行 |
| T-11 | POST process action=1 — comment 为空 | code=40005, messageZh="驳回时必须填写审批意见" |
| T-12 | POST process — record 不存在 | code=40001 |
| T-13 | POST process — 非 PENDING 状态 | code=40002, messageZh 含当前状态 |

### 7.2 前端测试用例（10 条）

| # | 用例 | 预期 |
|:-:|------|------|
| F-01 | 进入 /approval 页面 | 默认显示"待审批"Tab，自动加载列表 |
| F-02 | 切换到"已审批"Tab | 加载已审批数据，隐藏"同意/拒绝"按钮，仅保留"详情" |
| F-03 | 待审批 Tab 显示角标 | Tab 标题旁显示待审批数量红色角标 |
| F-04 | 应用名称列 — 切换语言 | 中文环境显示 appNameCn，英文环境展示 appNameEn |
| F-05 | 点击"详情"按钮 | 跳转到 `/app-detail/{businessId}` |
| F-06 | 点击"同意"按钮 → 确认 | Modal.confirm 弹出 → 确认 → 调用 process(action=0) → 成功 toast + 列表刷新 |
| F-07 | 点击"同意"按钮 → 取消 | Modal 关闭，无操作 |
| F-08 | 点击"拒绝"按钮 | 弹出 Modal，显示 TextArea |
| F-09 | 拒绝 Modal — TextArea 为空点击确认 | TextArea 边框变红，提示必填 |
| F-10 | 拒绝 Modal — 输入原因后确认 | 调用 process(action=1, comment) → 成功 toast + Modal 关闭 + 列表刷新 |

---

## 8. 效果图

> 浏览器打开 [`approval-page-mockup.html`](./approval-page-mockup.html) 查看可交互效果图。

效果图包含：

- **左侧导航**：仅展示"应用管理"菜单项
- **Tab 切换**：待审批（带数字角标）/ 已审批
- **语言切换**：顶部中/英文切换按钮，应用名称列随之切换
- **数据表格**：5 条待审批示例数据
  - 列：应用名称（中/英文）、版本号、应用ID、关联能力、申请人（账号ID）、申请时间、状态（彩色 Tag）、操作
- **操作按钮**：详情（蓝色链接）、同意（绿色链接）、拒绝（红色链接）
- **分页组件**：页码 + 每页条数选择
- **同意确认弹窗**：Modal.confirm 样式，显示审批信息摘要
- **拒绝弹窗**：Modal + TextArea（拒绝原因必填），确认拒绝按钮
- **Toast 提示**：操作成功后的顶部提示条

设计色值（与 market-web 一致）：
- Primary: `#2b5ff5`
- Success: `#00b578`
- Danger: `#f54a45`
- Warning: `#ff9f00`
- Font: 13px
- Border-radius: 6px
