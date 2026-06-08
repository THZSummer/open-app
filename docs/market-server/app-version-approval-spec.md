# market-server 通用审批管理模块 — 技术规格书（Spec）

> **版本**: v9.0 | **日期**: 2026-06-08 | **状态**: 待评审
>
> **效果图**: 浏览器打开 [`approval-page-mockup.html`](./approval-page-mockup.html) 可交互预览
>
> **v9.0 变更摘要**: Tab 页拆分为待审批应用 + 已上架应用两个 Tab（单页面单菜单）；已审批列表改为已上架应用列表（按应用分组展示最新已上架版本）；查看按钮改为 window.open 新开标签页；同意和拒绝均使用 Modal.confirm 二次确认；新增审批状态机设计

---

## 1. Scope（范围）

### 1.1 模块职责

market-server 提供**通用审批管理**能力，供 open-server 发起的审批流程进行审批操作。模块设计为通用引擎 + 策略扩展，支持多种 businessType。

**包含**：
- 待审批列表查询（分页）
- 已上架应用列表查询（分页，按应用分组展示最新已上架版本）
- 审批操作（通过/驳回统一接口，按 action 字段区分）
- 策略模式：`ApprovalHandler` 处理审批通过/驳回后的业务副作用
- 策略模式：`BusinessDataResolver` 解析不同 businessType 的业务展示数据
- 前端审批管理页面（待审批应用页 + 已上架应用页）
- 审批状态机定义（状态流转图 + 生命周期说明）

**不包含**：
- 审批流创建/编排（由 open-server 负责）
- 审批流模板 CRUD（由 open-server 负责）
- 催办/转办/撤回（后续迭代）
- 审批详情页（后续迭代）
- 搜索/过滤功能（暂时不提供，后续迭代）
- 业务表 DDL（App / Version / Ability 表结构见 `[docs/market-server/app.sql]`）

### 1.2 菜单与页面策略

当前固定为**应用版本发布审批**类型，采用**单页面 + 双 Tab** 结构：

| 菜单项 | 路由 | 图标 | Tab | 说明 |
|--------|------|------|-----|------|
| 审批管理 | `/approval` | `AuditOutlined` | 待审批应用 | 展示 status=PENDING(0) 的审批记录 |
| — | — | — | 已上架应用 | 展示每个应用最新已上架（APPROVED）版本 |

后续新增其他审批类型时，每种类型创建独立的审批管理页面。

### 1.3 核心约束

| 约束 | 说明 | 来源 |
|------|------|------|
| 共享数据库 | market-server 与 open-server 连接同一 MySQL `openapp` 库 | `[src/market-server/resources/application.yml]` |
| Entity 同构 | ApprovalRecord / ApprovalLog / ApprovalFlow 与 open-server 字段完全一致 | `[src/open-server/.../approval/entity/]` |
| combinedNodes 快照 | ApprovalRecord 存储 combinedNodes JSON 快照，不依赖 flowId | `[src/open-server/.../approval/entity/ApprovalRecord.java:30]` |
| businessType 由 open-server 决定 | market-server 不硬编码 businessType，通过策略模式路由 | `#DESIGN_DECISION` |
| 操作人来源 | 从 `UserContextHolder` 获取（Web 端调用），非 IM 回调 | `[src/market-server/.../security/UserContextHolder.java]` |
| **业务表已定义** | app / version / ability / relation 表结构见 app.sql | `[docs/market-server/app.sql]` |
| **单应用单待审** | 每个应用同时只能有一个 PENDING 状态的审批记录，只有审批通过或驳回后才能发起新版本 | open-server 业务规则 |
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
SELECT r.id, r.business_type, v.version_code, a.app_name_cn
FROM openplatform_v2_approval_record_t r
LEFT JOIN openplatform_app_version_t v ON r.business_id = v.id
LEFT JOIN openplatform_app_t a ON v.app_id = a.id

-- ✗ 禁止：4 张表 JOIN
SELECT ... FROM r
LEFT JOIN v ON ...
LEFT JOIN a ON ...
LEFT JOIN openplatform_app_ability_relation_t acr ON ...  -- 超出限制
```

#### 规则 3：子查询嵌套 ≤ 3 层

```sql
-- ✓ 正确：2 层嵌套
SELECT ... FROM r WHERE r.business_id IN (
    SELECT v.id FROM openplatform_app_version_t v WHERE v.app_id IN (
        SELECT a.id FROM openplatform_app_t a WHERE a.app_name_cn LIKE '%keyword%'
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
| pageSize | Integer | 否 | 每页条数，默认 10，前端可选 10/20/50 |

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
      "businessId": "1001",
      "appId": "app_third_party_001",
      "appNameCn": "订单管理应用",
      "appNameEn": "Order Management App",
      "versionNo": "v2.1.0",
      "capabilityNames": "订单查询, 订单创建",
      "applicantId": "u001",
      "status": 0,
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
| businessId | String | 业务 ID（版本 ID） |
| appId | String | 第三方应用 ID（从 `openplatform_app_p_t` 属性表获取，Service 层补查） |
| appNameCn | String | 应用中文名 |
| appNameEn | String | 应用英文名 |
| versionNo | String | 版本号 |
| capabilityNames | String | 应用关联能力中文名（逗号分隔），由 Service 层补查 `#CODE_ENRICH` |
| applicantId | String | 申请人账号 ID |
| status | Integer | 审批状态（待审批页固定为 0） |
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
    v.version_code,
    a.app_id,
    a.app_name_cn,
    a.app_name_en
FROM openplatform_v2_approval_record_t r
LEFT JOIN openplatform_app_version_t v ON r.business_id = v.id
LEFT JOIN openplatform_app_t a ON v.app_id = a.id
WHERE r.status = 0
ORDER BY r.create_time DESC
LIMIT #{offset}, #{pageSize}
```

**能力名称补查**（Service 层 Java 代码，单独查询，不增加主查询表数）：

```sql
-- 根据 app_id 查询关联能力名称（2 表 JOIN）
SELECT ab.ability_name_cn
FROM openplatform_app_ability_relation_t acr
LEFT JOIN openplatform_ability_t ab ON acr.ability_id = ab.id
WHERE acr.app_id = #{appId}
```

**第三方应用ID补查**（Service 层 Java 代码，从属性表获取）：

```sql
-- 根据应用主键 ID 查询第三方应用 ID（KV 属性表）
SELECT property_value
FROM openplatform_app_p_t
WHERE parent_id = #{appId}
  AND property_name = 'third_party_app_id'
```

> `[src/open-server/.../mapper/ApprovalRecordMapper.xml]`: `selectPendingList` 方法参考
>
> **注意**: `appId` 响应字段 = `openplatform_app_p_t.property_value`（`property_name = 'third_party_app_id'`），由 Service 层在主查询之后单独补查，不在主 SQL 中 JOIN（避免超出 3 表限制）。

**计数 SQL**：

```sql
SELECT COUNT(*)
FROM openplatform_v2_approval_record_t
WHERE status = 0
```

#### API-2: 已上架应用列表

```
GET /service/open/v2/approvals/published
```

> **v9.0 变更**: 替换原"已审批列表"。按应用分组，每个应用仅展示**最新已上架（APPROVED）版本**。若应用 v1 已通过、v2 被驳回，仍展示 v1。

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|:----:|------|
| curPage | Integer | 否 | 当前页，默认 1 |
| pageSize | Integer | 否 | 每页条数，默认 10，前端可选 10/20/50 |

> **注意**: 无 status 筛选参数，固定查询 APPROVED(1) 状态，按应用去重取最新。

**响应**：同 API-1 结构。

**主查询 SQL**（子查询按 app_id 分组取最新已上架版本）：

```sql
SELECT
    r.id,
    r.business_type,
    r.business_id,
    r.applicant_id,
    r.status,
    r.current_node,
    r.create_time,
    v.version_code,
    a.app_id,
    a.app_name_cn,
    a.app_name_en
FROM openplatform_v2_approval_record_t r
INNER JOIN (
    SELECT MAX(r2.id) AS max_id
    FROM openplatform_v2_approval_record_t r2
    INNER JOIN openplatform_app_version_t v2 ON r2.business_id = v2.id
    WHERE r2.status = 1
      AND r2.business_type = 'app_version_publish'
    GROUP BY v2.app_id
) latest ON r.id = latest.max_id
LEFT JOIN openplatform_app_version_t v ON r.business_id = v.id
LEFT JOIN openplatform_app_t a ON v.app_id = a.id
ORDER BY r.create_time DESC
LIMIT #{offset}, #{pageSize}
```

**SQL 合规验证**：

| 规则 | 结果 | 说明 |
|------|:----:|------|
| 无 SELECT * | ✓ | 所有字段明确列出 |
| JOIN ≤ 3 表 | ✓ | 外层: r + v + a = 3 表；子查询: r2 + v2 = 2 表 |
| 子查询嵌套 ≤ 3 层 | ✓ | 仅 1 层 FROM 子查询 |

> **为何使用 MAX(id) 而非 MAX(create_time)**：id 为 AUTO_INCREMENT，天然有序，避免 DATETIME 精度问题，且可利用主键索引提升性能。

**能力名称补查**（Service 层 Java 代码，同 API-1）：

```sql
SELECT ab.ability_name_cn
FROM openplatform_app_ability_relation_t acr
LEFT JOIN openplatform_ability_t ab ON acr.ability_id = ab.id
WHERE acr.app_id = #{appId}
```

**计数 SQL**：

```sql
SELECT COUNT(*)
FROM (
    SELECT MAX(r2.id) AS max_id
    FROM openplatform_v2_approval_record_t r2
    INNER JOIN openplatform_app_version_t v2 ON r2.business_id = v2.id
    WHERE r2.status = 1
      AND r2.business_type = 'app_version_publish'
    GROUP BY v2.app_id
) cnt
```

#### API-3: 审批操作（通过/驳回统一接口）

```
POST /service/open/v2/approvals/{id}/process
```

**路径参数**：`id` — ApprovalRecord ID

**请求 Body**：
```json
{
  "action": 0
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|:----:|------|
| action | Integer | 是 | 0=通过（APPROVE）, 1=驳回（REJECT） |

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
7. 插入 ApprovalLog（action=请求体 action）
8. 按 action 分支：
   ┌─ action=0（通过）:
   │    判断是否最后一个节点：
   │    - 是 → record.status = APPROVED(1), record.completedAt = now
   │           → handler.onApproved(record)   // 策略回调
   │    - 否 → record.currentNode += 1
   │    更新 ApprovalRecord
   │
   └─ action=1（驳回）:
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
| 40005 | 无效的操作类型：{action} | action 不是 0 或 1 |

---

### 2.2 前端页面

#### 2.2.1 路由与菜单

**路由**: `src/router/index.tsx` `[src/market-web/router/index.tsx]`

```jsx
<Route path="approval" element={<Approval />} />
```

**菜单**: `src/components/Layout/index.js` `[src/market-web/components/Layout/index.js]`

单个菜单项：

```js
// menuItems 数组新增：
{ key: '/approval', icon: <AuditOutlined />, label: '审批管理' },
```

#### 2.2.2 页面结构

单模块，遵循 `routeRedBlue/` 目录约定 `[src/market-web/router/routeRedBlue/]`：

```
src/router/routeRedBlue/
└── approval/
    ├── index.js            # 页面主文件（Tabs + 表格 + 弹窗）
    ├── index.module.less   # CSS Modules 样式
    ├── constant.js         # 常量（API 配置 key, 操作枚举, 列定义）
    └── thunk.js            # API 调用封装（fetchApi + buildApiUrl）
```

> 参考模式：`[src/market-web/router/routeRedBlue/lookup-classify/]` 的文件结构

#### 2.2.3 API 配置

**`src/configs/web.config.js`** 新增 `[src/market-web/configs/web.config.js]`：

```js
// 审批管理
APPROVAL_PENDING_LIST: '/market-web/service/open/v2/approvals/pending',
APPROVAL_PUBLISHED_LIST: '/market-web/service/open/v2/approvals/published',
APPROVAL_PROCESS: '/market-web/service/open/v2/approvals/{id}/process',
```

#### 2.2.4 页面设计

单页面 + Tabs 切换 + 表格 + 分页（无搜索栏，后续迭代按需增加）。

**Tab 切换**：
- **待审批应用**：显示 status=PENDING(0) 的记录
- **已上架应用**：显示每个应用最新已上架（APPROVED）版本

**待审批应用 Tab 表格列**：

| 列名 | 字段 | 宽度 | 渲染 |
|------|------|------|------|
| 应用名称 | appNameCn / appNameEn | 150px | 根据当前语言切换展示（中/英文）`#I18N_SWITCH` |
| 应用能力 | capabilityNames | 160px | 文本（逗号分隔，超长省略） |
| 版本号 | versionNo | 90px | 文本 |
| 应用ID | appId | 120px | 文本（第三方应用 ID，从 `openplatform_app_p_t` 补查） |
| 申请账号 | applicantId | 100px | 文本（账号 ID） |
| 申请时间 | createTime | 155px | yyyy-MM-dd HH:mm:ss |
| 操作 | - | 180px | 按钮组（查看 + 同意 + 拒绝） |

**已上架应用 Tab 表格列**：

| 列名 | 字段 | 宽度 | 渲染 |
|------|------|------|------|
| 应用名称 | appNameCn / appNameEn | 150px | 根据当前语言切换展示（中/英文）`#I18N_SWITCH` |
| 应用能力 | capabilityNames | 160px | 文本（逗号分隔，超长省略） |
| 版本号 | versionNo | 90px | 文本 |
| 应用ID | appId | 120px | 文本（第三方应用 ID，从 `openplatform_app_p_t` 补查） |
| 申请账号 | applicantId | 100px | 文本（账号 ID） |
| 创建时间 | createTime | 155px | yyyy-MM-dd HH:mm:ss |
| 操作 | - | 100px | 仅查看按钮 |

**操作按钮**：

| 按钮 | 待审批 Tab | 已上架 Tab | 行为 |
|------|:----------:|:----------:|------|
| 查看 | ✓ | ✓ | `window.open('/app-detail/' + record.appId, '_blank')` — 新开浏览器标签页跳转应用详情 |
| 同意 | ✓ | — | `Modal.confirm` 二次确认 → 调用 process API（action=0）→ 成功后 `message.success('审批通过')` + `fetchData()` |
| 拒绝 | ✓ | — | `Modal.confirm` 二次确认 → 调用 process API（action=1）→ 成功后 `message.success('已拒绝')` + `fetchData()` |

**语言切换逻辑**：

```js
const renderAppName = (text, record) => {
  return currentLang === 'en' ? record.appNameEn : record.appNameCn;
};
```

**交互流程**：

```
同意操作:
  1. 点击"同意"按钮
  2. Modal.confirm 弹出确认框，展示应用名称、版本号、申请账号
  3. 用户确认 → 调用 thunk.processApproval(id, { action: 0 })
  4. 检查 result.code === '200'
  5. 成功 → message.success('审批通过') + fetchData() 刷新列表
  6. 失败 → message.error({ content: result?.messageZh || '操作失败' })
  7. 取消 → Modal 关闭，无操作

拒绝操作:
  1. 点击"拒绝"按钮
  2. Modal.confirm 弹出确认框，展示应用名称、版本号、申请账号
  3. 用户确认 → 调用 thunk.processApproval(id, { action: 1 })
  4. 检查 result.code === '200'
  5. 成功 → message.success('已拒绝') + fetchData() 刷新列表
  6. 失败 → message.error({ content: result?.messageZh || '操作失败' })
  7. 取消 → Modal 关闭，无操作

查看操作:
  1. 点击"查看"按钮
  2. window.open('/app-detail/' + record.appId, '_blank') 新开浏览器标签页
```

**关键代码模式**：

```js
// approval/thunk.js — API 调用
import { fetchApi, buildApiUrl } from '../../../utils/webFetch';
import { API_CONFIG } from './constant';

export const fetchPendingList = async (params = {}) => {
  try {
    const result = await fetchApi(API_CONFIG.PENDING_LIST, { method: 'GET', params });
    return result || {};
  } catch (err) { return {}; }
};

export const fetchPublishedList = async (params = {}) => {
  try {
    const result = await fetchApi(API_CONFIG.PUBLISHED_LIST, { method: 'GET', params });
    return result || {};
  } catch (err) { return {}; }
};

export const processApproval = async (id, data) => {
  try {
    return await fetchApi(
      buildApiUrl(API_CONFIG.PROCESS, { id }),
      { method: 'POST', body: JSON.stringify(data) }
    ) || {};
  } catch (err) { return {}; }
};
```

```js
// approval/constant.js
export const API_CONFIG = {
  PENDING_LIST: 'APPROVAL_PENDING_LIST',
  PUBLISHED_LIST: 'APPROVAL_PUBLISHED_LIST',
  PROCESS: 'APPROVAL_PROCESS',
};

export const APPROVAL_ACTION = {
  APPROVE: 0,
  REJECT: 1,
};

export const PAGE_SIZE_OPTIONS = [
  { label: '10 条/页', value: 10 },
  { label: '20 条/页', value: 20 },
  { label: '50 条/页', value: 50 },
];

// 待审批 Tab 列定义
export const getPendingColumns = ({ renderAppName, renderAction }) => [
  { title: '应用名称', dataIndex: 'appNameCn', width: 150, render: renderAppName },
  { title: '应用能力', dataIndex: 'capabilityNames', width: 160, ellipsis: true },
  { title: '版本号', dataIndex: 'versionNo', width: 90 },
  { title: '应用ID', dataIndex: 'appId', width: 120 },
  { title: '申请账号', dataIndex: 'applicantId', width: 100 },
  { title: '申请时间', dataIndex: 'createTime', width: 155 },
  { title: '操作', width: 180, fixed: 'right', render: renderAction },
];

// 已上架 Tab 列定义
export const getPublishedColumns = ({ renderAppName, renderAction }) => [
  { title: '应用名称', dataIndex: 'appNameCn', width: 150, render: renderAppName },
  { title: '应用能力', dataIndex: 'capabilityNames', width: 160, ellipsis: true },
  { title: '版本号', dataIndex: 'versionNo', width: 90 },
  { title: '应用ID', dataIndex: 'appId', width: 120 },
  { title: '申请账号', dataIndex: 'applicantId', width: 100 },
  { title: '创建时间', dataIndex: 'createTime', width: 155 },
  { title: '操作', width: 100, fixed: 'right', render: renderAction },
];
```

```js
// approval/index.js — 状态管理
const [activeTab, setActiveTab] = useState('pending');
const [dataSource, setDataSource] = useState([]);
const [loading, setLoading] = useState(false);
const [pagination, setPagination] = useState({ curPage: 1, pageSize: 10, total: 0 });
const [currentLang, setCurrentLang] = useState(localStorage.getItem('lang') || 'zh');

// Tab 切换时重置分页并加载对应数据
const handleTabChange = (key) => {
  setActiveTab(key);
  setPagination({ curPage: 1, pageSize: 10, total: 0 });
};

// 根据 activeTab 调用不同 API
const fetchData = async () => {
  setLoading(true);
  const fetchFn = activeTab === 'pending' ? fetchPendingList : fetchPublishedList;
  const result = await fetchFn({ curPage: pagination.curPage, pageSize: pagination.pageSize });
  if (result.code === '200') {
    setDataSource(result.data || []);
    setPagination(prev => ({ ...prev, total: result.page?.total || 0 }));
  }
  setLoading(false);
};

// 事件处理
const handleView = (record) => {
  window.open('/app-detail/' + record.appId, '_blank');
};

const handleApprove = (record) => {
  Modal.confirm({
    title: '确认审批通过',
    content: `应用：${record.appNameCn}，版本：${record.versionNo}，申请账号：${record.applicantId}`,
    onOk: async () => {
      const result = await processApproval(record.id, { action: APPROVAL_ACTION.APPROVE });
      if (result.code === '200') {
        message.success('审批通过');
        fetchData();
      } else {
        message.error(result?.messageZh || '操作失败');
      }
    },
  });
};

const handleReject = (record) => {
  Modal.confirm({
    title: '确认审批拒绝',
    content: `应用：${record.appNameCn}，版本：${record.versionNo}，申请账号：${record.applicantId}`,
    okType: 'danger',
    onOk: async () => {
      const result = await processApproval(record.id, { action: APPROVAL_ACTION.REJECT });
      if (result.code === '200') {
        message.success('已拒绝');
        fetchData();
      } else {
        message.error(result?.messageZh || '操作失败');
      }
    },
  });
};
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

> 以下 DDL 来自 `[docs/market-server/app.sql]`

#### openplatform_app_t（应用主表）

```sql
CREATE TABLE `openplatform_app_t` (
  `id` bigint(20) NOT NULL COMMENT '主键',
  `app_id` varchar(100) NOT NULL COMMENT '应用ID',
  `tenant_id` varchar(64) NOT NULL DEFAULT '' COMMENT '租户id',
  `icon_id` varchar(64) NOT NULL DEFAULT '' COMMENT '图标id',
  `app_name_cn` varchar(255) NOT NULL COMMENT '应用中文名',
  `app_name_en` varchar(255) NOT NULL COMMENT '应用英文名',
  `app_desc_cn` varchar(2000) NOT NULL DEFAULT '' COMMENT '应用中文描述',
  `app_desc_en` varchar(2000) NOT NULL DEFAULT '' COMMENT '应用英文描述',
  `app_type` tinyint(1) DEFAULT '0' COMMENT '应用类型：0-个人应用 1-业务应用',
  `app_sub_type` tinyint(10) DEFAULT NULL COMMENT '应用子类型',
  `status` tinyint DEFAULT '1' COMMENT '状态：0=失效, 1=有效',
  `create_by` varchar(100) DEFAULT NULL COMMENT '创建人',
  `create_time` datetime(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `last_update_by` varchar(100) DEFAULT NULL COMMENT '最后更新人',
  `last_update_time` datetime(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '最后更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_app_id` (`app_id`) USING BTREE,
  UNIQUE KEY `uniq_name_cn` (`app_name_cn`) USING BTREE,
  UNIQUE KEY `uniq_name_en` (`app_name_en`) USING BTREE
) ENGINE=InnoDB COMMENT='应用表';
```

#### openplatform_app_version_t（应用版本表）

```sql
CREATE TABLE `openplatform_app_version_t` (
  `id` bigint(20) NOT NULL COMMENT '主键',
  `app_id` bigint(20) NOT NULL COMMENT '应用主键id',
  `version_desc_cn` varchar(2000) DEFAULT NULL COMMENT '版本中文描述',
  `version_desc_en` varchar(2000) DEFAULT NULL COMMENT '版本英文描述',
  `version_code` varchar(100) NOT NULL COMMENT '版本号',
  `tenant_id` varchar(64) NOT NULL DEFAULT '' COMMENT '租户id',
  `status` tinyint DEFAULT '1' COMMENT '状态：0=失效, 1=有效',
  `create_by` varchar(100) DEFAULT NULL COMMENT '创建人',
  `create_time` datetime(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `last_update_by` varchar(100) DEFAULT NULL COMMENT '最后更新人',
  `last_update_time` datetime(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '最后更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_app_id` (`app_id`) USING BTREE
) ENGINE=InnoDB COMMENT='应用版本表';
```

#### openplatform_ability_t（能力表）

```sql
CREATE TABLE `openplatform_ability_t` (
  `id` bigint(20) NOT NULL COMMENT '主键',
  `ability_name_cn` varchar(255) NOT NULL COMMENT '能力中文名',
  `ability_name_en` varchar(255) NOT NULL COMMENT '能力英文名',
  `ability_desc_cn` varchar(2000) NOT NULL DEFAULT '' COMMENT '能力中文描述',
  `ability_desc_en` varchar(2000) NOT NULL DEFAULT '' COMMENT '能力英文描述',
  `ability_type` TINYINT(1) NOT NULL DEFAULT '0' COMMENT '能力类型',
  `order_num` int(11) NOT NULL COMMENT '序号',
  `status` tinyint DEFAULT '1' COMMENT '状态：0=失效, 1=有效',
  `create_by` varchar(100) DEFAULT NULL COMMENT '创建人',
  `create_time` datetime(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `last_update_by` varchar(100) DEFAULT NULL COMMENT '最后更新人',
  `last_update_time` datetime(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '最后更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_ability_type` (`ability_type`) USING BTREE
) ENGINE=InnoDB COMMENT='能力表';
```

#### openplatform_app_ability_relation_t（应用能力关联表）

```sql
CREATE TABLE `openplatform_app_ability_relation_t` (
  `id` bigint(20) NOT NULL COMMENT '主键',
  `app_id` bigint(20) NOT NULL COMMENT '应用主键id',
  `ability_id` bigint(20) NOT NULL COMMENT '能力主键id',
  `ability_type` tinyint(1) NOT NULL DEFAULT '0' COMMENT '能力类型',
  `tenant_id` varchar(64) NOT NULL DEFAULT '' COMMENT '租户id',
  `status` tinyint DEFAULT '1' COMMENT '状态：0=失效, 1=有效',
  `create_by` varchar(100) DEFAULT NULL COMMENT '创建人',
  `create_time` datetime(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `last_update_by` varchar(100) DEFAULT NULL COMMENT '最后更新人',
  `last_update_time` datetime(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '最后更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_app_ability_id` (`app_id`,`ability_id`) USING BTREE,
  KEY `idx_app_id` (`app_id`) USING BTREE
) ENGINE=InnoDB COMMENT='应用能力关联表';
```

#### openplatform_app_p_t（应用属性表 — KV 结构）

```sql
CREATE TABLE `openplatform_app_p_t` (
  `id` bigint(20) NOT NULL COMMENT '主键',
  `parent_id` bigint(20) NOT NULL COMMENT '应用主键ID',
  `property_name` varchar(255) NOT NULL COMMENT '属性名',
  `property_value` varchar(2000) NOT NULL DEFAULT '' COMMENT '属性值',
  `tenant_id` varchar(64) NOT NULL DEFAULT '' COMMENT '租户id',
  `status` tinyint DEFAULT '1' COMMENT '状态：0=失效, 1=有效',
  `create_by` varchar(100) DEFAULT NULL COMMENT '创建人',
  `create_time` datetime(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `last_update_by` varchar(100) DEFAULT NULL COMMENT '最后更新人',
  `last_update_time` datetime(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '最后更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_parent_id` (`parent_id`) USING BTREE
) ENGINE=InnoDB COMMENT='应用属性表';
```

> **关键说明**：
> - `openplatform_app_p_t` 为 KV 属性表，第三方应用 ID 通过 `property_name = 'third_party_app_id'` 获取
> - `openplatform_app_ability_relation_t.app_id` 关联的是 `openplatform_app_t.id`（主键），不是 `app_id` 字段
> - 响应 VO 中的 `appId` = `openplatform_app_p_t.property_value`（Service 层补查）
> - 响应 VO 中的 `versionNo` 对应数据库字段 `openplatform_app_version_t.version_code`

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
| `process(recordId, action)` | 统一审批操作（通过/驳回） | `approve()` + `reject()` 合并 |

**不包含**（open-server 有但 market-server 不需要）：
- `composeApprovalNodes()` — 审批流由 open-server 创建
- `createApproval()` — 审批记录由 open-server 发起
- `cancel()` — 撤回功能后续迭代
- `transfer()` — 转办功能后续迭代

**process 方法伪代码**：

```java
@Transactional(rollbackFor = Exception.class)
public void process(Long recordId, int action) {
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
    insertLog(record, currentNode, action);

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
        record.setStatus(STATUS_REJECTED);
        record.setCompletedAt(now);
        recordMapper.update(record);
        handler.onRejected(record);
    } else {
        throw new BizException(40005, "无效的操作类型：" + action);
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

### 5.6 审批状态机

#### 状态流转图

```mermaid
stateDiagram-v2
    [*] --> PENDING: open-server 创建审批记录
    PENDING --> PENDING: 非最终节点通过（currentNode + 1）
    PENDING --> APPROVED: 最终节点通过
    PENDING --> REJECTED: 任意节点驳回
    PENDING --> CANCELLED: 撤回（后续迭代）
    APPROVED --> [*]
    REJECTED --> [*]
    CANCELLED --> [*]
```

#### 状态定义

| 状态 | 值 | 说明 | 终态? |
|------|:--:|------|:-----:|
| PENDING | 0 | 等待当前节点操作人处理 | 否 |
| APPROVED | 1 | 全部节点通过，审批完成 | 是 |
| REJECTED | 2 | 任意节点驳回，流程终止 | 是 |
| CANCELLED | 3 | 预留，market-server 暂未实现 | 是 |

#### 状态转移条件

| 转移 | 触发条件 | 执行方 | 副作用 |
|------|---------|--------|--------|
| `[*] → PENDING` | 开发者提交版本发布审批 | open-server `ApprovalEngine.createApproval()` | 创建 ApprovalRecord，status=0 |
| `PENDING → PENDING` | 管理员审批通过（非最终节点） | market-server `ApprovalEngine.process()` | currentNode += 1，插入 ApprovalLog |
| `PENDING → APPROVED` | 管理员审批通过（最终节点） | market-server `ApprovalEngine.process()` | status=1，completedAt=now，`handler.onApproved()` |
| `PENDING → REJECTED` | 管理员驳回（任意节点） | market-server `ApprovalEngine.process()` | status=2，completedAt=now，`handler.onRejected()` |
| `PENDING → CANCELLED` | 撤回（后续迭代） | — | 暂未实现 |

#### 审批生命周期

```
开发者在 open-server 创建应用版本
    │
    ▼
提交版本发布审批申请 (open-server)
    │
    ▼
创建 PENDING 审批记录 (open-server ApprovalEngine.createApproval)
    │  ├── 解析审批流节点 → combinedNodes JSON 快照
    │  └── status = PENDING(0), currentNode = 0
    │
    ▼
管理员在 market-server 查看待审批列表
    │
    ├── 通过（最终节点）──→ APPROVED(1)
    │       ├── handler.onApproved(record)  // 策略回调：更新版本状态为已上架
    │       └── 应用进入已上架应用列表
    │
    ├── 通过（非最终节点）──→ PENDING(0)
    │       └── currentNode += 1，等待下一节点操作人
    │
    └── 驳回 ──→ REJECTED(2)
            ├── handler.onRejected(record)  // 策略回调：更新版本状态为已驳回
            └── 应用仍展示在已上架列表（如有历史已上架版本）
```

> **说明**：market-server 仅负责审批操作（process），不负责审批记录创建和流程编排（由 open-server 负责）。

---

## 6. File Manifest（文件清单）

### 6.1 后端新建文件（19 个）

> 路径前缀：`market-server/src/main/java/com/xxx/it/works/wecode/v2/modules/`

| # | 文件路径 | 说明 |
|:-:|---------|------|
| 1 | `approval/controller/ApprovalController.java` | 3 个端点（pending, published, process） |
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
| 16 | `approval/dto/ApprovalListRequest.java` | 列表查询请求（curPage, pageSize） |
| 17 | `approval/dto/ApprovalProcessRequest.java` | 审批操作请求（action） |
| 18 | `approval/vo/ApprovalListVo.java` | 列表展示 VO（含 appNameCn, appNameEn, versionNo, appId, capabilityNames, applicantId, createTime） |
| 19 | `approval/constant/ApprovalConstants.java` | 状态/操作常量 |

> MyBatis XML（路径前缀 `market-server/src/main/resources/mapper/`）：

| # | 文件路径 | 说明 |
|:-:|---------|------|
| 20 | `approval/ApprovalRecordMapper.xml` | SQL（selectPendingList, selectPublishedList, selectById, update, countPending, countPublished）— 所有查询明确列出字段，JOIN ≤ 3 表 |
| 21 | `approval/ApprovalLogMapper.xml` | SQL（insert） |

### 6.2 前端新建文件（4 个）

> 路径前缀：`market-web/src/router/routeRedBlue/`

| # | 文件路径 | 说明 |
|:-:|---------|------|
| 1 | `approval/index.js` | 页面主文件（Tabs + 表格 + 弹窗） |
| 2 | `approval/index.module.less` | 样式 |
| 3 | `approval/constant.js` | 常量（API 配置, 操作枚举, 两个 Tab 列定义） |
| 4 | `approval/thunk.js` | API 调用（fetchPendingList, fetchPublishedList, processApproval） |

### 6.3 前端修改文件（3 个）

| # | 文件路径 | 变更 |
|:-:|---------|------|
| 1 | `market-web/src/router/index.tsx` | 新增 `<Route path="approval" element={<Approval />} />` |
| 2 | `market-web/src/components/Layout/index.js` | menuItems 新增 `{ key: '/approval', icon: <AuditOutlined />, label: '审批管理' }` |
| 3 | `market-web/src/configs/web.config.js` | 新增 3 个 API URL 配置（PENDING_LIST, PUBLISHED_LIST, PROCESS） |

---

## 7. Test Cases（测试用例）

### 7.1 后端测试用例（15 条）

#### 列表查询

| # | 用例 | 预期 |
|:-:|------|------|
| T-01 | GET pending — 无参数 | 返回所有 status=0 的记录，含 appNameCn/appNameEn/versionNo/appId/capabilityNames/applicantId，按 createTime DESC 排序 |
| T-02 | GET pending — 分页 curPage=2, pageSize=5 | 返回第 6-10 条记录，page.total 正确 |
| T-03 | GET pending — 无记录 | data=[], page.total=0 |
| T-04 | GET published — 无参数 | 返回每个应用最新 APPROVED 版本，按 createTime DESC 排序 |
| T-05 | GET published — 同一应用多个 APPROVED 版本 | 仅返回 create_time 最大的那条记录 |
| T-06 | GET published — 同一应用 v1(APPROVED) + v2(REJECTED) | 返回 v1（最新已上架版本） |
| T-07 | GET published — 应用仅有 REJECTED 记录 | 该应用不出现在结果中 |
| T-08 | GET published — 分页 | 去重后分页数据正确 |

#### 审批操作

| # | 用例 | 预期 |
|:-:|------|------|
| T-09 | POST process action=0 — 正常通过（单节点） | status → APPROVED(1), completedAt 非空, handler.onApproved 执行 |
| T-10 | POST process action=0 — 多节点非最后 | currentNode += 1, status 仍为 PENDING(0), handler 不触发 |
| T-11 | POST process action=0 — 多节点最后节点 | status → APPROVED(1), handler.onApproved 执行 |
| T-12 | POST process action=1 — 正常驳回 | status → REJECTED(2), handler.onRejected 执行 |
| T-13 | POST process — record 不存在 | code=40001 |
| T-14 | POST process — 非 PENDING 状态 | code=40002, messageZh 含当前状态 |
| T-15 | POST process — 无效 action | code=40005, messageZh 含 action 值 |

### 7.2 前端测试用例（15 条）

| # | 用例 | 预期 |
|:-:|------|------|
| F-01 | 进入 `/approval` 页面 | 默认显示"待审批应用"Tab，自动加载列表 |
| F-02 | 切换到"已上架应用"Tab | 加载已上架数据，隐藏"同意/拒绝"按钮，仅保留"查看" |
| F-03 | 切换回"待审批应用"Tab | 重新加载待审批数据，显示"查看/同意/拒绝"按钮 |
| F-04 | 点击"审批管理"菜单项 | 正确导航到审批页面，菜单高亮 |
| F-05 | 应用名称列 — 切换语言 | 中文环境显示 appNameCn，英文环境展示 appNameEn |
| F-06 | 待审批 Tab 点击"查看"按钮 | `window.open` 新开浏览器标签页，URL 为 `/app-detail/{appId}` |
| F-07 | 已上架 Tab 点击"查看"按钮 | `window.open` 新开浏览器标签页，URL 为 `/app-detail/{appId}` |
| F-08 | 待审批 Tab 点击"同意"按钮 → 确认 | Modal.confirm 弹出 → 确认 → 调用 process(action=0) → success toast + 列表刷新 |
| F-09 | 待审批 Tab 点击"同意"按钮 → 取消 | Modal 关闭，无操作 |
| F-10 | 待审批 Tab 点击"拒绝"按钮 | Modal.confirm 弹出确认框，展示应用信息摘要 |
| F-11 | 待审批 Tab 点击"拒绝"按钮 → 确认 | 调用 process(action=1) → success toast + 列表刷新 |
| F-12 | 待审批 Tab 点击"拒绝"按钮 → 取消 | Modal 关闭，无操作 |
| F-13 | 已上架 Tab 操作列 | 仅显示"查看"按钮，无同意/拒绝按钮 |
| F-14 | 待审批 Tab 时间列标题 | 显示"申请时间" |
| F-15 | 已上架 Tab 时间列标题 | 显示"创建时间" |

---

## 8. 效果图

> 浏览器打开 [`approval-page-mockup.html`](./approval-page-mockup.html) 查看可交互效果图。

效果图为单页面 + Tabs 结构：

### 8.1 整体布局

- **左侧导航**：展示"审批管理"菜单项（一个菜单项，点击进入审批页面）
- **页面标题**：审批管理 + 语言切换（中文/EN）
- **Tab 切换**：待审批应用 / 已上架应用（无角标）

### 8.2 待审批应用 Tab

- **数据表格**：示例待审批数据
  - 列：应用名称（中/英文）、应用能力、版本号、应用ID、申请账号、申请时间、操作
  - 操作按钮：查看（蓝色链接）、同意（绿色链接）、拒绝（红色链接）
- **分页组件**：页码 + 每页条数选择
- **同意确认弹窗**：Modal.confirm 样式，展示应用信息摘要
- **拒绝确认弹窗**：Modal.confirm 样式（红色确认按钮），展示应用信息摘要
- **Toast 提示**：操作成功后的顶部提示条

### 8.3 已上架应用 Tab

- **数据表格**：示例已上架数据
  - 列：应用名称（中/英文）、应用能力、版本号、应用ID、申请账号、创建时间、操作
  - 操作按钮：仅查看（蓝色链接）
- **分页组件**：页码 + 每页条数选择

### 8.3 设计色值（与 market-web 一致）

- Primary: `#2b5ff5`
- Success: `#00b578`
- Danger: `#f54a45`
- Warning: `#ff9f00`
- Font: 13px
- Border-radius: 6px
