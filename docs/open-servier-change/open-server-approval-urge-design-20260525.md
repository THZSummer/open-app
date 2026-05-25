# Open-Server 审批催办接口设计文档

**文档版本**：1.1  
**创建日期**：2026-05-25  
**接口编号**：#53

---

## 一、背景

open-server 审批模块当前支持以下操作：

| 接口编号 | 操作 | HTTP 方法 |
|:------:|------|:--------:|
| #48 | 同意审批 | POST |
| #49 | 驳回审批 | POST |
| #50 | 撤销审批 | POST |
| #51 | 批量同意 | POST |
| #52 | 批量驳回 | POST |

缺少**催办能力**。申请人提交审批后，若审批人长时间未处理，无法通过系统提醒审批人尽快处理。

**需求**：补充催办接口，查询待审批状态的审批记录 → 获取当前审批节点的审批人 → 调用第三方接口发送卡片消息 → 将返回的 cardId 持久化到审批节点的 `cardIds` 属性中。重复催办 = 重复调用本接口，每次追加一个 cardId。

---

## 二、接口定义

### 基本信息

| 项目 | 值 |
|------|-----|
| 接口编号 | #53 |
| HTTP 方法 | `POST` |
| URL | `/service/open/v2/approvals/{id}/urge` |
| 安全注解 | 无（与 approve/reject/cancel 保持一致） |
| 请求体 | 无 Body |

### 请求参数

| 参数 | 位置 | 类型 | 必填 | 说明 |
|------|------|------|:----:|------|
| `id` | Path | String | 是 | 审批记录 ID |

### 请求示例

```http
POST /service/open/v2/approvals/1234567890/urge
```

### 成功响应

```json
{
    "code": "200",
    "messageZh": "催办成功",
    "messageEn": "Urge sent successfully",
    "data": {
        "id": "1234567890",
        "status": 0,
        "message": "已通知审批人 张三 尽快处理"
    }
}
```

### 错误响应

| 场景 | code | messageZh | messageEn |
|------|:----:|-----------|-----------|
| 记录不存在或非待审状态 | 400 | 审批记录不存在 | Approval record not found or not pending |
| 非申请人催办 | 403 | 只有申请人可以催办 | Only the applicant can urge the approval |

---

## 三、业务流程

```
请求 POST /service/open/v2/approvals/{id}/urge
  │
  ├─ 1. 查询待审批状态的审批记录
  │     SQL: SELECT * FROM approval_record_t WHERE id = ? AND status = 0
  │     └─ null → 抛出 400："审批记录不存在"
  │
  ├─ 2. 校验申请人身份
  │     record.applicantId == UserContextHolder.getUserId()
  │     └─ 否 → 抛出 403："只有申请人可以催办"
  │
  ├─ 3. 解析 combinedNodes JSON → List<ApprovalNodeDto>
  │     取 nodes[currentNode] 获取当前审批节点的审批人
  │
  ├─ 4. 调用第三方发送卡片消息
  │     approvalNotifyService.sendUrgeCard(审批人信息, 审批记录信息)
  │     └─ 返回 cardId
  │
  ├─ 5. 将 cardId 追加到当前节点的 cardIds 列表
  │     └─ cardIds 为 null 则初始化为空列表
  │
  ├─ 6. 重新序列化 combinedNodes → 写回数据库
  │     updateCombinedNodes(record)
  │
  └─ 7. 返回 ApprovalActionResponse
```

---

## 四、数据结构变更

### 4.1 combinedNodes JSON 结构变更

`combinedNodes` 是 `List<ApprovalNodeDto>` 的 JSON 序列化，存储在 `approval_record_t.combined_nodes` 字段中。

#### 场景 A：currentNode=0（第一个审批节点催办）

**催办前**（DB 中 JSON）：

```json
[
  {"type":"approver","userId":"payment_leader","userName":"支付负责人","order":1,"level":"resource"},
  {"type":"approver","userId":"dept_manager","userName":"部门经理","order":2,"level":"scene"},
  {"type":"approver","userId":"admin001","userName":"系统管理员","order":3,"level":"global"}
]
```

**第 1 次催办后**（currentNode=0，cardIds 添加到第 1 个节点）：

```json
[
  {"type":"approver","userId":"payment_leader","userName":"支付负责人","order":1,"level":"resource","cardIds":["card_abc123"]},
  {"type":"approver","userId":"dept_manager","userName":"部门经理","order":2,"level":"scene"},
  {"type":"approver","userId":"admin001","userName":"系统管理员","order":3,"level":"global"}
]
```

**第 2 次催办后**（重复调用同一接口）：

```json
[
  {"type":"approver","userId":"payment_leader","userName":"支付负责人","order":1,"level":"resource","cardIds":["card_abc123","card_def456"]},
  {"type":"approver","userId":"dept_manager","userName":"部门经理","order":2,"level":"scene"},
  {"type":"approver","userId":"admin001","userName":"系统管理员","order":3,"level":"global"}
]
```

#### 场景 B：currentNode=1（第二个审批节点催办）

假设第一个节点已通过，审批流转到第二个节点（currentNode=1）：

**催办前**：

```json
[
  {"type":"approver","userId":"payment_leader","userName":"支付负责人","order":1,"level":"resource","status":1},
  {"type":"approver","userId":"dept_manager","userName":"部门经理","order":2,"level":"scene"},
  {"type":"approver","userId":"admin001","userName":"系统管理员","order":3,"level":"global"}
]
```

**催办后**（currentNode=1，cardIds 添加到第 2 个节点）：

```json
[
  {"type":"approver","userId":"payment_leader","userName":"支付负责人","order":1,"level":"resource","status":1},
  {"type":"approver","userId":"dept_manager","userName":"部门经理","order":2,"level":"scene","cardIds":["card_ghi789"]},
  {"type":"approver","userId":"admin001","userName":"系统管理员","order":3,"level":"global"}
]
```

#### 场景 C：currentNode=2（第三个审批节点催办）

假设前两个节点已通过，审批流转到第三个节点（currentNode=2）：

**催办前**：

```json
[
  {"type":"approver","userId":"payment_leader","userName":"支付负责人","order":1,"level":"resource","status":1},
  {"type":"approver","userId":"dept_manager","userName":"部门经理","order":2,"level":"scene","status":1},
  {"type":"approver","userId":"admin001","userName":"系统管理员","order":3,"level":"global"}
]
```

**催办后**（currentNode=2，cardIds 添加到第 3 个节点）：

```json
[
  {"type":"approver","userId":"payment_leader","userName":"支付负责人","order":1,"level":"resource","status":1},
  {"type":"approver","userId":"dept_manager","userName":"部门经理","order":2,"level":"scene","status":1},
  {"type":"approver","userId":"admin001","userName":"系统管理员","order":3,"level":"global","cardIds":["card_jkl012"]}
]
```

### 4.2 ApprovalNodeDto 新增字段

| 字段 | 类型 | 说明 |
|------|------|------|
| `cardIds` | `List<String>` | 催办卡片ID列表，每催办一次追加一个 |

> **兼容性**：`cardIds` 为 null 时 Jackson 自动忽略（`@JsonInclude(NON_NULL)`），不影响现有审批记录数据的兼容性。无需数据库 DDL 变更。

### 4.3 ApprovalEngine.Action 新增常量

| 常量 | 值 | 说明 |
|------|:--:|------|
| `APPROVE` | 0 | 同意（已有） |
| `REJECT` | 1 | 拒绝（已有） |
| `CANCEL` | 2 | 撤销（已有） |
| `TRANSFER` | 3 | 转交（已有） |
| **`URGE`** | **4** | **催办（新增）** |

---

## 五、涉及文件变更

| # | 文件 | 变更类型 | 说明 |
|:-:|------|:------:|------|
| 1 | `ApprovalNodeDto.java` | 修改 | 新增 `List<String> cardIds` 字段 |
| 2 | `ApprovalEngine.java` | 修改 | `Action` 类新增 `URGE = 4` 常量 |
| 3 | `ApprovalNotifyService.java` | **新建** | 催办通知接口（定义发送卡片方法签名） |
| 4 | `DefaultApprovalNotifyService.java` | **新建** | 默认实现（返回 mock cardId，预留对接实际 IM） |
| 5 | `ApprovalRecordMapper.java` | 修改 | 新增 `selectPendingById` 和 `updateCombinedNodes` 方法 |
| 6 | `ApprovalRecordMapper.xml` | 修改 | 新增对应的 SQL 语句 |
| 7 | `ApprovalService.java` | 修改 | 注入 `ApprovalNotifyService`，新增 `urge()` 方法 |
| 8 | `ApprovalController.java` | 修改 | 新增 `POST /approvals/{id}/urge` 端点 |

> **无需新建 DTO**：入参无 Body，响应复用现有 `ApprovalActionResponse`。

---

## 六、详细设计

### 6.1 ApprovalNodeDto.java — 修改

**路径**：`com.xxx.it.works.wecode.v2.modules.approval.dto.ApprovalNodeDto`

新增字段：

```java
/** 催办卡片ID列表，每催办一次追加一个 */
private List<String> cardIds;
```

### 6.2 ApprovalEngine.java — 修改

**路径**：`com.xxx.it.works.wecode.v2.modules.approval.engine.ApprovalEngine`

`Action` 内部类新增：

```java
public static final int URGE = 4;  // 催办
```

### 6.3 ApprovalNotifyService.java — 新建

**路径**：`com.xxx.it.works.wecode.v2.modules.approval.service.ApprovalNotifyService`

```java
/**
 * 审批通知服务接口
 *
 * <p>定义审批相关的通知能力，如催办卡片消息发送</p>
 */
public interface ApprovalNotifyService {

    /**
     * 发送催办卡片消息给审批人
     *
     * @param approverId    审批人用户ID
     * @param approverName  审批人姓名
     * @param recordId      审批记录ID
     * @param businessType  业务类型（api_register/event_register/...）
     * @param businessId    业务ID
     * @param applicantName 申请人姓名
     * @return 卡片ID（第三方返回）
     */
    String sendUrgeCard(String approverId, String approverName,
                        Long recordId, String businessType, Long businessId,
                        String applicantName);
}
```

### 6.4 DefaultApprovalNotifyService.java — 新建

**路径**：`com.xxx.it.works.wecode.v2.modules.approval.service.DefaultApprovalNotifyService`

```java
@Slf4j
@Service
public class DefaultApprovalNotifyService implements ApprovalNotifyService {

    @Override
    public String sendUrgeCard(String approverId, String approverName,
                               Long recordId, String businessType, Long businessId,
                               String applicantName) {
        // TODO: 对接实际 IM 系统（钉钉/飞书/企微等）发送卡片消息
        // 实际实现时替换为第三方接口调用，返回真实 cardId
        log.info("[APPROVAL_URGE] Sending urge card: approver={}, recordId={}, " +
                 "businessType={}, applicant={}",
                 approverName, recordId, businessType, applicantName);

        return "mock_card_" + System.currentTimeMillis();
    }
}
```

### 6.5 ApprovalRecordMapper.java — 修改

**路径**：`com.xxx.it.works.wecode.v2.modules.approval.mapper.ApprovalRecordMapper`

新增两个方法：

```java
/**
 * 根据ID查询待审批状态的记录
 *
 * @param id 审批记录ID
 * @return 审批记录（仅 status=0 的记录），不存在或非待审返回 null
 */
ApprovalRecord selectPendingById(@Param("id") Long id);

/**
 * 更新审批记录的 combined_nodes 字段
 *
 * @param record 审批记录（仅更新 combined_nodes 和 last_update_time）
 */
void updateCombinedNodes(ApprovalRecord record);
```

### 6.6 ApprovalRecordMapper.xml — 修改

**路径**：`resources/mapper/ApprovalRecordMapper.xml`

在现有 SQL 之后新增两条：

```xml
<!-- 根据ID查询待审批状态的记录（催办专用） -->
<select id="selectPendingById" resultMap="BaseResultMap">
    SELECT <include refid="Base_Column_List"/>
    FROM openplatform_v2_approval_record_t
    WHERE id = #{id} AND status = 0
</select>

<!-- 更新审批记录的 combined_nodes（催办持久化 cardId 专用） -->
<update id="updateCombinedNodes" parameterType="com.xxx.it.works.wecode.v2.modules.approval.entity.ApprovalRecord">
    UPDATE openplatform_v2_approval_record_t
    SET combined_nodes = #{combinedNodes},
        last_update_time = #{lastUpdateTime}
    WHERE id = #{id}
</update>
```

### 6.7 ApprovalService.java — 修改

**路径**：`com.xxx.it.works.wecode.v2.modules.approval.service.ApprovalService`

**新增依赖注入**：

```java
private final ApprovalNotifyService approvalNotifyService;
```

**新增方法**：

```java
/**
 * #53 催办审批
 *
 * <p>查询待审批记录 → 获取当前审批节点审批人 → 调用第三方发送卡片消息 →
 * 将 cardId 持久化到 combinedNodes 对应节点的 cardIds 列表</p>
 *
 * @param id       审批记录ID
 * @param operator 当前操作用户ID（需为申请人）
 * @return 催办结果
 */
@Transactional
public ApprovalActionResponse urge(Long id, String operator) {
    // 1. 查询待审批状态的记录（SQL: WHERE id=? AND status=0）
    ApprovalRecord record = recordMapper.selectPendingById(id);
    if (record == null) {
        throw BusinessException.badRequest(
            "审批记录不存在", "Approval record not found or not pending");
    }

    // 2. 校验身份（仅申请人可催办）
    if (!record.getApplicantId().equals(operator)) {
        throw BusinessException.forbidden(
            "只有申请人可以催办",
            "Only the applicant can urge the approval");
    }

    // 3. 解析 combinedNodes，获取当前审批节点审批人
    List<ApprovalNodeDto> nodes = approvalEngine.parseNodes(
        record.getCombinedNodes());
    ApprovalNodeDto currentNode = nodes.get(record.getCurrentNode());

    // 4. 调用第三方发送催办卡片消息
    String cardId = approvalNotifyService.sendUrgeCard(
        currentNode.getUserId(),
        currentNode.getUserName(),
        id,
        record.getBusinessType(),
        record.getBusinessId(),
        record.getApplicantName()
    );

    // 5. 将 cardId 追加到当前节点的 cardIds 列表
    if (currentNode.getCardIds() == null) {
        currentNode.setCardIds(new ArrayList<>());
    }
    currentNode.getCardIds().add(cardId);

    // 6. 重新序列化 combinedNodes 并更新数据库
    record.setCombinedNodes(approvalEngine.serializeNodes(nodes));
    record.setLastUpdateTime(new Date());
    recordMapper.updateCombinedNodes(record);

    // 7. 返回结果
    return ApprovalActionResponse.builder()
        .id(String.valueOf(id))
        .status(record.getStatus())
        .message("已通知审批人 " + currentNode.getUserName() + " 尽快处理")
        .build();
}
```

### 6.8 ApprovalController.java — 修改

**路径**：`com.xxx.it.works.wecode.v2.modules.approval.controller.ApprovalController`

在 `batchReject` 方法之后新增：

```java
/**
 * #53 催办审批
 *
 * <p>申请人催办当前审批人，调用第三方发送卡片消息通知</p>
 */
@PostMapping("/approvals/{id}/urge")
@Operation(summary = "#53 催办审批",
           description = "申请人催办当前审批人，发送卡片消息通知")
public ApiResponse<ApprovalActionResponse> urge(@PathVariable String id) {
    log.info("Urge approval: id={}", id);

    String operator = UserContextHolder.getUserId();
    ApprovalActionResponse data = approvalService.urge(
        Long.parseLong(id), operator);
    return ApiResponse.success(data);
}
```

---

## 七、验证用例

| # | 场景 | 预期结果 |
|:-:|------|---------|
| 1 | 对待审记录调用催办（currentNode=0） | 返回 200，DB 中 `combined_nodes` 第 1 个节点含 `cardIds: ["mock_card_xxx"]` |
| 2 | 对同一记录再次催办 | 返回 200，`cardIds` 列表追加第二个元素 |
| 3 | 对已通过记录催办 | 返回 400："审批记录不存在"（SQL 查不到 status=0 的记录） |
| 4 | 对已拒绝记录催办 | 返回 400："审批记录不存在" |
| 5 | 对已撤销记录催办 | 返回 400："审批记录不存在" |
| 6 | 对不存在的 ID 催办 | 返回 400："审批记录不存在" |
| 7 | 非申请人身份催办 | 返回 403："只有申请人可以催办" |
| 8 | 催办后查询审批详情 | `combinedNodes` 中 currentNode 对应节点包含 `cardIds` 列表 |
| 9 | currentNode=1 时催办 | cardIds 添加到第 2 个节点，其他节点不受影响 |
| 10 | currentNode=2 时催办 | cardIds 添加到第 3 个节点，其他节点不受影响 |
