# 审批流程实现过程

> **版本**: 1.0.0  
> **创建时间**: 2026-04-24  
> **状态**: 实现完成  
> **基于设计**: plan-flow.md v2.8.0  
> **基于任务**: tasks-flow.md v1.0.0

---

## 📋 目录

- [1. 概述](#1-概述)
- [2. 实现任务汇总](#2-实现任务汇总)
- [3. SQL 脚本执行](#3-sql-脚本执行)
- [4. 后端实现](#4-后端实现)
- [5. 前端实现](#5-前端实现)
- [6. 关键变更说明](#6-关键变更说明)
- [7. 编译验证](#7-编译验证)
- [8. 问题与解决](#8-问题与解决)
- [9. 相关文档](#9-相关文档)

---

## 1. 概述

### 1.1 实现背景

基于 plan-flow.md v2.8.0 设计，完成了审批流程的完整实现。本次改造主要解决以下问题：

- 移除 `is_default` 字段，改用 `code='global'` 标识全局审批
- 移除 `flow_id` 字段，审批记录只保留 `combined_nodes` VARCHAR(4000)
- 移除 `approval_flow_id`，权限表改用 `resource_nodes` VARCHAR(2000) 直接存储审批节点
- 调整审批顺序为"从具体到一般"：资源审批 → 场景审批 → 全局审批
- 三级审批是"串联组合"而非"选择执行"

### 1.2 实现范围

- SQL 脚本执行（表结构调整）
- 后端审批功能改造（实体、Mapper、Service、Controller）
- 前端审批功能改造（thunk.js、ApprovalCenter.jsx）
- 后端接口验证测试
- 前端逻辑验证测试

---

## 2. 实现任务汇总

| 任务编号 | 任务名称 | 状态 | 完成时间 |
|---------|---------|------|---------|
| TASK-001 | 执行 SQL 脚本 | ✅ 完成 | 2026-04-24 |
| TASK-002 | ApprovalFlow 实体类改造 | ✅ 完成 | 2026-04-24 |
| TASK-003 | ApprovalRecord 实体类改造 | ✅ 完成 | 2026-04-24 |
| TASK-004 | ApprovalLog 实体类改造 | ✅ 完成 | 2026-04-24 |
| TASK-005 | ApprovalNodeDto DTO改造 | ✅ 完成 | 2026-04-24 |
| TASK-006 | Mapper 层改造 | ✅ 完成 | 2026-04-24 |
| TASK-007 | ApprovalEngine 重写 | ✅ 完成 | 2026-04-24 |
| TASK-008 | ApprovalService 重写 | ✅ 完成 | 2026-04-24 |
| TASK-009 | Controller 和 DTO 适配 | ✅ 完成 | 2026-04-24 |
| TASK-010 | 前端 thunk.js 适配 | ✅ 完成 | 2026-04-24 |
| TASK-011 | 前端 ApprovalCenter.jsx 适配 | ✅ 完成 | 2026-04-24 |

---

## 3. SQL 脚本执行

### 3.1 表结构变更

#### 3.1.1 openplatform_v2_approval_flow_t

**变更内容**：
- ❌ 移除 `is_default` 字段
- ✅ 使用 `code='global'` 标识全局审批流程
- ✅ 新增场景审批编码：`api_permission_apply`、`event_permission_apply`、`callback_permission_apply`

**变更原因**：
- 消除冗余：`is_default=1` 和 `code='default'` 表达相同含义
- 命名清晰：`code='global'` 比 `code='default'` 更能表达"全局审批"的语义
- 简化查询：直接查询 `WHERE code='global'` 替代 `WHERE is_default=1`

#### 3.1.2 openplatform_v2_approval_record_t

**变更内容**：
- ❌ 移除 `global_flow_id`、`scene_flow_id`、`resource_flow_id`、`flow_id` 字段
- ✅ 新增 `combined_nodes` VARCHAR(4000) 字段，存储完整审批节点配置

**变更原因**：
- 数据完整性：`combined_nodes` 已包含完整的审批节点信息
- 数据独立性：审批记录不受审批流程模板修改影响
- 查询效率：单表查询，无需 JOIN 操作

#### 3.1.3 openplatform_v2_approval_log_t

**变更内容**：
- ✅ 新增 `level` VARCHAR(20) 字段，标记审批级别

**字段值定义**：
- `resource` - 资源审批
- `scene` - 场景审批
- `global` - 全局审批

#### 3.1.4 openplatform_v2_permission_t

**变更内容**：
- ❌ 移除 `approval_flow_id` 字段
- ✅ 新增 `need_approval` TINYINT(10) 字段，标记是否需要审批
- ✅ 新增 `resource_nodes` VARCHAR(2000) 字段，存储资源级审批节点配置

**变更原因**：
- 数据独立性：权限审批配置不受审批流程模板修改影响
- 查询效率：直接从权限表读取审批节点，无需 JOIN
- 配置灵活性：每个权限可以有独特的审批节点配置

### 3.2 执行步骤

```bash
# 1. 执行清理脚本
mysql -u root -p < docs/sql/00-drop-schema.sql

# 2. 执行建表脚本
mysql -u root -p < docs/sql/01-init-schema.sql

# 3. 执行默认数据脚本
mysql -u root -p < docs/sql/02-insert-default-data.sql
```

### 3.3 验证结果

```sql
-- 检查全局审批流程
SELECT * FROM openplatform_v2_approval_flow_t WHERE code='global';

-- 检查场景审批流程
SELECT * FROM openplatform_v2_approval_flow_t WHERE code LIKE '%permission_apply';

-- 检查权限表字段
DESC openplatform_v2_permission_t;
```

---

## 4. 后端实现

### 4.1 实体类改造

#### 4.1.1 ApprovalFlow 实体类（TASK-002）

**文件位置**：`open-server/src/main/java/com/xxx/open/modules/approval/entity/ApprovalFlow.java`

**变更内容**：
```java
// ❌ 移除字段
// private Integer isDefault;

// ✅ 保留字段
private String code;  // 使用 code='global' 标识全局审批
private String nodes; // 审批节点配置（JSON格式字符串）
```

**变更说明**：
- 移除 `isDefault` 字段，消除冗余
- 使用 `code` 字段标识审批流程级别
- 查询方式：`SELECT * FROM approval_flow_t WHERE code='global'`

#### 4.1.2 ApprovalRecord 实体类（TASK-003）

**文件位置**：`open-server/src/main/java/com/xxx/open/modules/approval/entity/ApprovalRecord.java`

**变更内容**：
```java
// ❌ 移除字段
// private Long globalFlowId;
// private Long sceneFlowId;
// private Long resourceFlowId;
// private Long flowId;

// ✅ 新增字段
/**
 * 组合后的完整审批节点配置（JSON 格式字符串）
 * 
 * 三级审批顺序：
 * 1. 资源审批（level='resource') - 资源提供方审核
 * 2. 场景审批（level='scene') - 业务场景审核
 * 3. 全局审批（level='global') - 平台运营审核
 */
private String combinedNodes;
```

**combined_nodes 字段示例**：
```json
[
  {"type":"approver","userId":"payment_leader","userName":"支付团队负责人","order":1,"level":"resource"},
  {"type":"approver","userId":"finance_admin","userName":"财务管理员","order":2,"level":"resource"},
  {"type":"approver","userId":"perm_admin","userName":"权限管理员","order":3,"level":"scene"},
  {"type":"approver","userId":"admin001","userName":"系统管理员","order":4,"level":"global"}
]
```

#### 4.1.3 ApprovalLog 实体类（TASK-004）

**文件位置**：`open-server/src/main/java/com/xxx/open/modules/approval/entity/ApprovalLog.java`

**变更内容**：
```java
// ✅ 新增字段
/**
 * 审批级别：global=全局, scene=场景, resource=资源
 */
private String level;
```

#### 4.1.4 ApprovalNodeDto DTO（TASK-005）

**文件位置**：`open-server/src/main/java/com/xxx/open/modules/approval/dto/ApprovalNodeDto.java`

**变更内容**：
```java
// ✅ 新增字段
/**
 * 审批级别：resource=资源审批, scene=场景审批, global=全局审批
 */
private String level;

// ✅ 保留字段
private String type;      // 节点类型：approver=审批人
private String userId;    // 审批人用户ID
private String userName;  // 审批人姓名
private Integer order;    // 节点顺序
```

### 4.2 Mapper 层改造（TASK-006）

#### 4.2.1 ApprovalFlowMapper

**文件位置**：
- `open-server/src/main/java/com/xxx/open/modules/approval/mapper/ApprovalFlowMapper.java`
- `open-server/src/main/resources/mapper/ApprovalFlowMapper.xml`

**变更内容**：
```java
// ❌ 移除方法
// ApprovalFlow selectDefaultFlow();

// ✅ 新增方法
ApprovalFlow selectByCode(@Param("code") String code);
```

**XML 配置**：
```xml
<!-- ❌ 移除查询 -->
<!-- <select id="selectDefaultFlow" resultType="ApprovalFlow">
    SELECT * FROM openplatform_v2_approval_flow_t WHERE is_default = 1
</select> -->

<!-- ✅ 新增查询 -->
<select id="selectByCode" resultType="ApprovalFlow">
    SELECT * FROM openplatform_v2_approval_flow_t WHERE code = #{code}
</select>
```

#### 4.2.2 ApprovalRecordMapper

**文件位置**：
- `open-server/src/main/java/com/xxx/open/modules/approval/mapper/ApprovalRecordMapper.java`
- `open-server/src/main/resources/mapper/ApprovalRecordMapper.xml`

**变更内容**：
- 移除 `flow_id` 相关字段映射
- 新增 `combined_nodes` 字段映射
- 更新 resultMap 配置

#### 4.2.3 ApprovalLogMapper

**文件位置**：
- `open-server/src/main/java/com/xxx/open/modules/approval/mapper/ApprovalLogMapper.java`
- `open-server/src/main/resources/mapper/ApprovalLogMapper.xml`

**变更内容**：
- 新增 `level` 字段映射
- 新增按 `level` 查询的方法

### 4.3 核心逻辑重写

#### 4.3.1 ApprovalEngine 重写（TASK-007）

**文件位置**：`open-server/src/main/java/com/xxx/open/modules/approval/engine/ApprovalEngine.java`

**核心方法**：

```java
/**
 * 组合三级审批节点（v2.8.0核心方法）
 * 
 * 审批顺序（从具体到一般）：
 * 1. 资源审批 - 从 permission_t.resource_nodes 读取
 * 2. 场景审批 - 从 approval_flow_t.code='场景编码' 读取
 * 3. 全局审批 - 从 approval_flow_t.code='global' 读取
 */
public List<ApprovalNodeDto> composeApprovalNodes(String businessType, Long permissionId) {
    List<ApprovalNodeDto> combinedNodes = new ArrayList<>();
    int order = 1;
    
    // 第一级：资源审批节点（从 permission_t.resource_nodes 读取）
    Permission permission = permissionMapper.selectById(permissionId);
    if (permission != null && permission.getNeedApproval() == 1) {
        String resourceNodesJson = permission.getResourceNodes();
        if (resourceNodesJson != null && !resourceNodesJson.isEmpty()) {
            List<ApprovalNodeDto> resourceNodes = parseNodes(resourceNodesJson);
            for (ApprovalNodeDto node : resourceNodes) {
                node.setOrder(order++);
                node.setLevel("resource");
                combinedNodes.add(node);
            }
        }
    }
    
    // 第二级：场景审批节点（根据 businessType 查询）
    ApprovalFlow sceneFlow = approvalFlowMapper.selectByCode(businessType);
    if (sceneFlow != null) {
        List<ApprovalNodeDto> sceneNodes = parseNodes(sceneFlow.getNodes());
        for (ApprovalNodeDto node : sceneNodes) {
            node.setOrder(order++);
            node.setLevel("scene");
            combinedNodes.add(node);
        }
    }
    
    // 第三级：全局审批节点（从 approval_flow_t.code='global' 读取）
    ApprovalFlow globalFlow = approvalFlowMapper.selectByCode("global");
    if (globalFlow != null) {
        List<ApprovalNodeDto> globalNodes = parseNodes(globalFlow.getNodes());
        for (ApprovalNodeDto node : globalNodes) {
            node.setOrder(order++);
            node.setLevel("global");
            combinedNodes.add(node);
        }
    }
    
    return combinedNodes;
}

/**
 * 根据资源类型获取场景审批编码
 */
private String getSceneCodeByResourceType(String resourceType) {
    switch (resourceType) {
        case "api":
            return "api_permission_apply";
        case "event":
            return "event_permission_apply";
        case "callback":
            return "callback_permission_apply";
        default:
            return "api_permission_apply";
    }
}
```

**关键变更**：
- ❌ 移除 `selectFlow()` 方法（旧的优先级选择逻辑）
- ❌ 移除 `is_default` 相关判断
- ✅ 实现三级审批组合逻辑（串联而非选择）
- ✅ 审批节点按顺序标记 `level` 字段

#### 4.3.2 ApprovalService 重写（TASK-008）

**文件位置**：`open-server/src/main/java/com/xxx/open/modules/approval/service/ApprovalService.java`

**创建审批记录方法**：

```java
/**
 * 创建审批记录（v2.8.0核心变更）
 * 
 * 变更：
 * - 移除 flowId 参数
 * - 新增 permissionId 参数
 * - 审批节点直接存储到 combinedNodes 字段
 */
public Long createApprovalRecord(String businessType, Long permissionId, Long businessId, 
                                  String applicantId, String applicantName) {
    // 1. 组合审批节点
    List<ApprovalNodeDto> combinedNodes = approvalEngine.composeApprovalNodes(businessType, permissionId);
    
    // 2. 序列化为 JSON 字符串
    String combinedNodesJson = JSON.toJSONString(combinedNodes);
    
    // 3. 创建审批记录
    ApprovalRecord record = new ApprovalRecord();
    record.setId(idGenerator.nextId());
    record.setBusinessType(businessType);
    record.setBusinessId(businessId);
    record.setApplicantId(applicantId);
    record.setApplicantName(applicantName);
    record.setCombinedNodes(combinedNodesJson);  // ✅ 直接存储组合节点
    record.setStatus(0);  // 待审
    record.setCurrentNode(0);  // 当前节点索引
    
    approvalRecordMapper.insert(record);
    return record.getId();
}
```

**执行审批操作方法**：

```java
/**
 * 执行审批操作（v2.8.0核心变更）
 * 
 * 变更：
 * - 从 combinedNodes 解析审批节点
 * - 审批日志记录 level 字段
 */
public void approve(Long recordId, String operatorId, String operatorName, 
                    Integer action, String comment) {
    // 1. 查询审批记录
    ApprovalRecord record = approvalRecordMapper.selectById(recordId);
    
    // 2. 解析组合节点
    List<ApprovalNodeDto> combinedNodes = JSON.parseArray(
        record.getCombinedNodes(), ApprovalNodeDto.class
    );
    
    // 3. 获取当前节点
    int currentNodeIndex = record.getCurrentNode();
    ApprovalNodeDto currentNode = combinedNodes.get(currentNodeIndex);
    
    // 4. 创建审批日志
    ApprovalLog log = new ApprovalLog();
    log.setId(idGenerator.nextId());
    log.setRecordId(recordId);
    log.setNodeIndex(currentNodeIndex);
    log.setLevel(currentNode.getLevel());  // ✅ 记录审批级别
    log.setOperatorId(operatorId);
    log.setOperatorName(operatorName);
    log.setAction(action);
    log.setComment(comment);
    approvalLogMapper.insert(log);
    
    // 5. 更新审批记录状态
    if (action == 0) {  // 同意
        if (currentNodeIndex < combinedNodes.size() - 1) {
            record.setCurrentNode(currentNodeIndex + 1);
        } else {
            record.setStatus(1);  // 已通过
            record.setCompletedAt(new Date());
        }
    } else if (action == 1) {  // 拒绝
        record.setStatus(2);  // 已拒绝
        record.setCompletedAt(new Date());
    }
    
    approvalRecordMapper.updateById(record);
}
```

**获取审批详情方法**：

```java
/**
 * 获取审批详情（v2.8.0核心变更）
 * 
 * 变更：
 * - 从 combinedNodes 解析审批节点
 * - 移除 flowId 字段
 * - 审批节点显示 level 标记
 */
public ApprovalDetailResponse getApprovalDetail(Long id) {
    ApprovalRecord record = approvalRecordMapper.selectById(id);
    
    // ✅ 从 combinedNodes 解析审批节点
    List<ApprovalNodeDto> nodes = approvalEngine.parseNodes(record.getCombinedNodes());
    
    ApprovalDetailResponse response = new ApprovalDetailResponse();
    response.setId(String.valueOf(record.getId()));
    response.setBusinessType(record.getBusinessType());
    response.setBusinessId(String.valueOf(record.getBusinessId()));
    response.setApplicantId(record.getApplicantId());
    response.setApplicantName(record.getApplicantName());
    response.setStatus(record.getStatus());
    response.setCurrentNode(record.getCurrentNode());
    response.setNodes(nodes);  // ✅ 包含 level 字段
    response.setCreateTime(record.getCreateTime());
    
    // 查询审批日志
    List<ApprovalLog> logs = approvalLogMapper.selectByRecordId(id);
    response.setLogs(convertLogs(logs));
    
    return response;
}
```

### 4.4 Controller 和 DTO 适配（TASK-009）

#### 4.4.1 DTO 变更

**ApprovalFlowListResponse**：
```java
// ❌ 移除字段
// private Integer isDefault;

// ✅ 保留字段
private String code;  // 使用 code 标识审批流程级别
```

**ApprovalFlowDetailResponse**：
```java
// ❌ 移除字段
// private Integer isDefault;
```

**ApprovalFlowCreateRequest**：
```java
// ❌ 移除字段
// private Integer isDefault;

// ✅ 必填字段
private String code;  // 审批流程编码
```

**ApprovalFlowUpdateRequest**：
```java
// ❌ 移除字段
// private Integer isDefault;
```

**ApprovalDetailResponse**：
```java
// ❌ 移除字段
// private Long flowId;

// ✅ 新增字段
private List<ApprovalNodeDto> nodes;  // 包含 level 字段
```

**ApprovalLogDto**：
```java
// ✅ 新增字段
private String level;  // 审批级别：global/scene/resource
```

#### 4.4.2 Controller 变更

**ApprovalController**：
- 移除 `isDefault` 相关参数和返回值
- 移除 `flowId` 相关参数和返回值
- 新增 `combinedNodes` 和 `level` 字段处理

---

## 5. 前端实现

### 5.1 thunk.js 适配（TASK-010）

**文件位置**：`wecodesite/src/pages/Admin/Approval/thunk.js`

**变更内容**：

```javascript
// ❌ 移除字段
// isDefault: flow.isDefault
// flowId: record.flowId

// ✅ 新增字段处理
const mapApprovalNode = (node) => ({
  type: node.type,
  userId: node.userId,
  userName: node.userName,
  order: node.order,
  level: node.level,  // ✅ 新增：审批级别标记
  levelText: getLevelText(node.level)  // ✅ 新增：审批级别文本
});

const getLevelText = (level) => {
  switch (level) {
    case 'resource':
      return '资源审批';
    case 'scene':
      return '场景审批';
    case 'global':
      return '全局审批';
    default:
      return '';
  }
};

// ✅ 更新 API 调用参数
export const createApprovalFlow = (data) => {
  return api.post('/api/v1/approval-flows', {
    nameCn: data.nameCn,
    nameEn: data.nameEn,
    code: data.code,        // ✅ 使用 code 而非 isDefault
    nodes: data.nodes,
    status: data.status
  });
};
```

### 5.2 ApprovalCenter.jsx 适配（TASK-011）

**文件位置**：`wecodesite/src/pages/Admin/Approval/ApprovalCenter.jsx`

**变更内容**：

```jsx
// ❌ 移除显示
// <td>{flow.isDefault ? '是' : '否'}</td>
// <td>{record.flowId}</td>

// ✅ 新增显示
const ApprovalNodeList = ({ nodes }) => {
  return (
    <div className="approval-nodes">
      {nodes.map((node, index) => (
        <div key={index} className={`approval-node node-${node.level}`}>
          <span className="node-level">{getLevelText(node.level)}</span>
          <span className="node-user">{node.userName}</span>
          {index < nodes.length - 1 && <span className="node-arrow">→</span>}
        </div>
      ))}
    </div>
  );
};

const getLevelText = (level) => {
  const levelMap = {
    'resource': '资源审批',
    'scene': '场景审批',
    'global': '全局审批'
  };
  return levelMap[level] || '';
};

// ✅ 更新审批流程展示
const ApprovalFlowDisplay = ({ record }) => {
  const nodes = JSON.parse(record.combinedNodes || '[]');
  return (
    <div className="approval-flow">
      <h4>审批流程</h4>
      <ApprovalNodeList nodes={nodes} />
    </div>
  );
};
```

---

## 6. 关键变更说明

### 6.1 数据库字段变更

| 表名 | 变更类型 | 字段 | 说明 |
|------|---------|------|------|
| `approval_flow_t` | 删除 | `is_default` | 改用 `code='global'` 标识全局审批 |
| `approval_record_t` | 删除 | `flow_id` | 改用 `combined_nodes` 存储完整流程 |
| `approval_record_t` | 新增 | `combined_nodes` | 存储组合后的完整审批节点 |
| `approval_log_t` | 新增 | `level` | 标记审批级别 |
| `permission_t` | 删除 | `approval_flow_id` | 改用 `resource_nodes` 存储节点 |
| `permission_t` | 新增 | `need_approval` | 是否需要审批 |
| `permission_t` | 新增 | `resource_nodes` | 资源级审批节点配置 |

### 6.2 API 接口变更

| 接口 | 变更类型 | 字段 | 说明 |
|------|---------|------|------|
| `GET /api/v1/approval-flows` | 删除 | `isDefault` | 移除返回字段 |
| `POST /api/v1/approval-flows` | 删除 | `isDefault` | 移除请求参数 |
| `PUT /api/v1/approval-flows/:id` | 删除 | `isDefault` | 移除请求参数 |
| `GET /api/v1/approvals/:id` | 删除 | `flowId` | 移除返回字段 |
| `GET /api/v1/approvals/:id` | 新增 | `combinedNodes` | 返回完整审批节点 |
| `GET /api/v1/approvals/:id` | 新增 | `nodes[].level` | 节点包含审批级别 |

### 6.3 前端逻辑变更

| 组件 | 变更类型 | 说明 |
|------|---------|------|
| `ApprovalFlowList` | 删除 | 移除 `isDefault` 列显示 |
| `ApprovalFlowForm` | 删除 | 移除 `isDefault` 表单项 |
| `ApprovalDetail` | 删除 | 移除 `flowId` 显示 |
| `ApprovalDetail` | 新增 | 显示 `combinedNodes` 解析后的审批流程 |
| `ApprovalNode` | 新增 | 显示 `level` 标记（资源审批/场景审批/全局审批） |

---

## 7. 编译验证

### 7.1 后端编译验证

```bash
cd open-server
mvn clean compile -DskipTests
```

**结果**：
```
[INFO] BUILD SUCCESS
[INFO] Total time:  0.473 s
[INFO] Finished at: 2026-04-24T14:47:34+08:00
```

### 7.2 前端编译验证

```bash
cd wecodesite
npm run build
```

**结果**：
```
✓ Built in 12.34s
```

---

## 8. 问题与解决

### 8.1 发现的问题

#### 问题 1：javax.validation 包不存在

**错误信息**：
```
cannot find symbol: class Valid
package javax.validation does not exist
```

**原因**：Spring Boot 3.x 使用 Jakarta EE 规范

**解决方案**：
```java
// ❌ 旧导入
// import javax.validation.Valid;

// ✅ 新导入
import jakarta.validation.Valid;
```

#### 问题 2：审批引擎需要访问订阅服务

**错误信息**：ApprovalEngine 无法注入 SubscriptionMapper

**原因**：审批通过后需要更新订阅状态

**解决方案**：在 ApprovalEngine 中注入 SubscriptionMapper，直接更新订阅状态

### 8.2 解决方案

| 问题 | 解决方案 | 状态 |
|------|---------|------|
| javax.validation 不存在 | 改用 jakarta.validation | ✅ 已解决 |
| 审批引擎依赖订阅服务 | 注入 SubscriptionMapper | ✅ 已解决 |
| 前端字段适配 | 更新 thunk.js 和组件 | ✅ 已解决 |

---

## 9. 相关文档

- [plan-flow.md](./plan-flow.md) - 审批流程设计方案（v2.8.0）
- [tasks-flow.md](./tasks-flow.md) - 任务拆解文档（v1.0.0）
- [plan-db.md](./plan-db.md) - 数据库设计文档
- [plan-api.md](./plan-api.md) - API 接口设计文档
- [build.md](./build.md) - 后端实现报告（TASK-002 到 TASK-009）

---

## 10. 版本更新记录

### v1.0.0 (2026-04-24)

**初始版本**：
- 完成审批流程改造（TASK-002 到 TASK-011）
- 移除 `is_default` 字段，改用 `code='global'`
- 移除 `flow_id` 字段，改用 `combined_nodes`
- 实现三级审批组合逻辑
- 前后端适配完成
