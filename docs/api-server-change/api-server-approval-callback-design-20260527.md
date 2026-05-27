# API-Server 审批卡片回调接口技术方案

**文档版本**：1.0
**创建日期**：2026-05-27

---

## 概述

催办功能发送 IM 卡片消息后，审批人直接在卡片上点击"同意/驳回"，IM 平台回调 api-server 的接口完成审批操作。该接口在 api-server 中自行实现完整审批业务逻辑，直接操作同库审批表。

**核心差异**：
- open-server 审批接口：Web 端调用，操作人从 `UserContextHolder` 获取
- api-server 回调接口：IM 平台回调，操作人从请求 body 的 `accountId` 获取

---

## 接口定义

| 项目 | 值 |
|------|-----|
| HTTP 方法 | POST |
| 路径 | `/api/v1/approvals/callback` |
| 请求参数 | `businessId`（@RequestParam Long）— 业务ID，对应审批记录的 business_id；`businessType`（@RequestParam String）— 业务类型，如 api_permission_apply |
| 请求体 | `ApprovalCallbackRequest`（JSON） |
| 响应体 | `ApprovalCallbackResponse<T>`（自定义格式，非 ApiResponse） |

---

## 请求 / 响应结构

### 请求体 — ApprovalCallbackRequest

| 字段 | 类型 | 说明 |
|------|------|------|
| cardId | String | 消息卡片 ID |
| content | String | JSON 字符串，反序列化后含 url/type/title/verb/data |
| messageType | String | 消息类型 |
| type | String | 暂无业务逻辑 |
| corpId | String | 企业 ID |
| traceId | String | 链路追踪 ID |
| userId | String | IM 平台用户标识 |
| accountId | String | 审批人 ID，对应 combinedNodes 当前节点 userId |

### Content 解析结构 — ApprovalCardContent

`content` 字段是 JSON 字符串，反序列化后：

| 字段 | 类型 | 说明 |
|------|------|------|
| url | String | 卡片链接 |
| type | String | 内容类型 |
| title | String | 卡片标题 |
| verb | String | 动作描述 |
| data | String | ★ 审批动作："1"=同意, "0"=驳回 |

### 响应体 — ApprovalCallbackResponse\<T\>（人工实现构造逻辑）

| 字段 | 类型 | 说明 |
|------|------|------|
| status | Integer | 响应状态码 |
| data | T | 响应数据 |
| errorInfo | ErrorInfo | 错误信息（成功时为 null） |

#### ErrorInfo

| 字段 | 类型 | 说明 |
|------|------|------|
| code | Integer | 错误码 |
| userMessageZh | String | 中文错误描述 |
| userMessageEn | String | 英文错误描述 |

---

## 业务处理流程

```
POST /api/v1/approvals/callback?businessId=xxx&businessType=api_permission_apply
    │
    ▼
1. 权限校验（预留 TODO，手工实现）
    │
    ▼
2. 反序列化 content（JSON 字符串 → ApprovalCardContent）
    │  解析 data 字段："1" → 同意, "0" → 驳回
    │
    ▼
3. 查询审批记录
    │  SELECT * FROM openplatform_v2_approval_record_t
    │  WHERE business_type = #{businessType} AND business_id = #{businessId} AND status = 0
    │  ORDER BY create_time DESC LIMIT 1
    │  （命中 idx_business(business_type, business_id) 联合索引）
    │  → 不存在则返回错误响应
    │
    ▼
4. 操作人校验
    │  parseNodes(combinedNodes) → currentNode
    │  currentNode.userId == request.accountId ?
    │  → 不匹配则返回 403 错误响应
    │
    ▼
5a. 同意（data = "1"）               5b. 驳回（data = "0"）
    │                                    │
    ├─ 插入 ApprovalLog (action=0)       ├─ 插入 ApprovalLog (action=1)
    ├─ 判断是否最后一个节点              ├─ record.status = REJECTED(2)
    │  ├─ 是: status=APPROVED(1)        ├─ record.completedAt = now
    │  │       completedAt=now           ├─ 更新 record
    │  │       更新 record               └─ 更新 subscription.status = 2(已拒绝)
    │  └─ 更新 subscription
    │          status=1(已授权)
    │          approvedAt=now
    │          approvedBy=applicantId
    │  └─ 否: currentNode += 1
    │         更新 record
    │
    ▼
6. 构造响应（人工实现）
    │  成功: status=200, data=审批结果
    │  失败: status=错误码, errorInfo={code, userMessageZh, userMessageEn}
```

---

## 文件清单

### 新建文件（12 个）

| # | 文件路径 | 说明 |
|:-:|---------|------|
| 1 | `approval/controller/ApprovalCallbackController.java` | 回调接口 Controller |
| 2 | `approval/dto/ApprovalCallbackRequest.java` | 回调请求 DTO |
| 3 | `approval/dto/ApprovalCardContent.java` | content JSON 反序列化对象 |
| 4 | `approval/dto/ApprovalCallbackResponse.java` | 自定义响应体（人工实现构造逻辑） |
| 5 | `approval/entity/ApprovalRecord.java` | 审批记录实体 → openplatform_v2_approval_record_t |
| 6 | `approval/entity/ApprovalLog.java` | 审批日志实体 → openplatform_v2_approval_log_t |
| 7 | `approval/entity/ApprovalNode.java` | 审批节点 DTO（combinedNodes JSON 解析） |
| 8 | `approval/mapper/ApprovalRecordMapper.java` | 审批记录 Mapper 接口 |
| 9 | `approval/mapper/ApprovalLogMapper.java` | 审批日志 Mapper 接口 |
| 10 | `resources/mapper/ApprovalRecordMapper.xml` | 审批记录 Mapper XML |
| 11 | `resources/mapper/ApprovalLogMapper.xml` | 审批日志 Mapper XML |
| 12 | `approval/service/ApprovalCallbackService.java` | 回调业务逻辑 Service |

> 路径前缀：`api-server/src/main/java/com/xxx/api/`

### 修改文件（2 个）

| # | 文件路径 | 变更 |
|:-:|---------|------|
| 13 | `common/mapper/SubscriptionMapper.java` | 新增 `selectById(Long id)` 和 `updateApprovalStatus(Subscription)` |
| 14 | `resources/mapper/SubscriptionMapper.xml` | 新增对应 SQL |

---

## 各文件详细设计

### 1. ApprovalCallbackController

```java
@RestController
@RequestMapping("/api/v1/approvals")
@RequiredArgsConstructor
@Slf4j
public class ApprovalCallbackController {

    private final ApprovalCallbackService approvalCallbackService;

    @PostMapping("/callback")
    public ApprovalCallbackResponse<?> handleCallback(
            @RequestParam Long businessId,
            @RequestParam String businessType,
            @RequestBody ApprovalCallbackRequest request) {
        return approvalCallbackService.handleCallback(businessId, businessType, request);
    }
}
```

**注意**：返回类型是 `ApprovalCallbackResponse<?>`，不是 `ApiResponse`。业务异常在 Service 层内部 catch 并构造自定义响应，不依赖 GlobalExceptionHandler。

### 2. ApprovalCallbackRequest

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalCallbackRequest {
    private String cardId;
    private String content;       // JSON 字符串
    private String messageType;
    private String type;          // 暂无业务逻辑
    private String corpId;
    private String traceId;
    private String userId;
    private String accountId;     // 审批人 ID
}
```

### 3. ApprovalCardContent

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalCardContent {
    private String url;
    private String type;
    private String title;
    private String verb;
    private String data;          // "1"=同意, "0"=驳回
}
```

### 4. ApprovalCallbackResponse\<T\>（人工实现）

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalCallbackResponse<T> {
    private Integer status;
    private T data;
    private ErrorInfo errorInfo;

    // TODO: 工厂方法由人工实现构造逻辑
    // public static <T> ApprovalCallbackResponse<T> success(T data) { ... }
    // public static ApprovalCallbackResponse<?> error(int status, int code, String msgZh, String msgEn) { ... }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorInfo {
        private Integer code;
        private String userMessageZh;
        private String userMessageEn;
    }
}
```

### 5. ApprovalRecord 实体

对应表 `openplatform_v2_approval_record_t`，从 open-server 复制：

```java
@Data
public class ApprovalRecord implements Serializable {
    private Long id;
    private String combinedNodes;     // 审批节点链 JSON
    private String businessType;      // 业务类型
    private Long businessId;          // 业务 ID
    private String applicantId;       // 申请人 ID
    private String applicantName;     // 申请人姓名
    private Integer status;           // 0=待审, 1=已通过, 2=已驳回, 3=已取消
    private Integer currentNode;      // 当前审批节点索引
    private Date createTime;
    private Date lastUpdateTime;
    private String createBy;
    private String lastUpdateBy;
    private Date completedAt;         // 完成时间
}
```

### 6. ApprovalLog 实体

对应表 `openplatform_v2_approval_log_t`，从 open-server 复制：

```java
@Data
public class ApprovalLog implements Serializable {
    private Long id;                  // TODO: ID 生成策略人工实现
    private Long recordId;            // 审批记录 ID
    private Integer nodeIndex;        // 节点索引
    private String level;             // 审批级别：global/scene/resource
    private String operatorId;        // 操作人 ID
    private String operatorName;      // 操作人姓名
    private Integer action;           // 0=同意, 1=拒绝, 2=撤销, 3=转交
    private String comment;           // 审批意见
    private Date createTime;
    private Date lastUpdateTime;
    private String createBy;
    private String lastUpdateBy;
}
```

### 7. ApprovalNode

combinedNodes JSON 数组中每个元素的反序列化对象：

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalNode {
    private String type;              // "approver"
    private String userId;            // 审批人 ID
    private String userName;          // 审批人姓名
    private Integer order;            // 顺序（1-based）
    private String level;             // "resource" | "scene" | "global"
    private Integer status;           // 0=待审, 1=已通过, 2=已驳回
    private Date approveTime;         // 审批时间
    private String comment;           // 审批意见
    private List<String> cardIds;     // 催办卡片 ID 列表
}
```

### 8-9. Mapper 接口

**ApprovalRecordMapper**：
```java
@Mapper
public interface ApprovalRecordMapper {
    ApprovalRecord selectLatestPendingByBusiness(@Param("businessType") String businessType,
                                                  @Param("businessId") Long businessId);
    int update(ApprovalRecord record);
}
```

**ApprovalLogMapper**：
```java
@Mapper
public interface ApprovalLogMapper {
    int insert(ApprovalLog log);
}
```

### 10. ApprovalRecordMapper.xml

```xml
<resultMap id="BaseResultMap" type="com.xxx.api.approval.entity.ApprovalRecord">
    <id column="id" property="id"/>
    <result column="combined_nodes" property="combinedNodes"/>
    <result column="business_type" property="businessType"/>
    <result column="business_id" property="businessId"/>
    <result column="applicant_id" property="applicantId"/>
    <result column="applicant_name" property="applicantName"/>
    <result column="status" property="status"/>
    <result column="current_node" property="currentNode"/>
    <result column="create_time" property="createTime"/>
    <result column="last_update_time" property="lastUpdateTime"/>
    <result column="create_by" property="createBy"/>
    <result column="last_update_by" property="lastUpdateBy"/>
    <result column="completed_at" property="completedAt"/>
</resultMap>

<sql id="Base_Column_List">
    id, combined_nodes, business_type, business_id, applicant_id, applicant_name,
    status, current_node, create_time, last_update_time, create_by, last_update_by, completed_at
</sql>

<!-- 按 businessType + businessId 查询最新待审批记录（命中 idx_business 索引） -->
<select id="selectLatestPendingByBusiness" resultMap="BaseResultMap">
    SELECT <include refid="Base_Column_List"/>
    FROM openplatform_v2_approval_record_t
    WHERE business_type = #{businessType} AND business_id = #{businessId} AND status = 0
    ORDER BY create_time DESC
    LIMIT 1
</select>

<!-- 更新审批记录（status / currentNode / combinedNodes / completedAt） -->
<update id="update" parameterType="com.xxx.api.approval.entity.ApprovalRecord">
    UPDATE openplatform_v2_approval_record_t
    SET status = #{status},
        current_node = #{currentNode},
        combined_nodes = #{combinedNodes},
        completed_at = #{completedAt},
        last_update_time = #{lastUpdateTime},
        last_update_by = #{lastUpdateBy}
    WHERE id = #{id}
</update>
```

### 11. ApprovalLogMapper.xml

标准 INSERT 语句，写入 `openplatform_v2_approval_log_t`。

### 12. ApprovalCallbackService（核心逻辑）

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class ApprovalCallbackService {

    private final ApprovalRecordMapper approvalRecordMapper;
    private final ApprovalLogMapper approvalLogMapper;
    private final SubscriptionMapper subscriptionMapper;
    private final ObjectMapper objectMapper;

    @Transactional(rollbackFor = Exception.class)
    public ApprovalCallbackResponse<?> handleCallback(Long businessId,
                                                       String businessType,
                                                       ApprovalCallbackRequest request) {

        // 1. 权限校验（预留，手工实现）
        // TODO: implement permission verification
        verifyPermission(request);

        // 2. 解析 content JSON 字符串
        ApprovalCardContent content = parseContent(request.getContent());
        int action = parseAction(content.getData()); // "1"→0(APPROVE), "0"→1(REJECT)

        // 3. 查询待审批记录（businessType + businessId 命中 idx_business 索引）
        ApprovalRecord record = approvalRecordMapper
                .selectLatestPendingByBusiness(businessType, businessId);
        if (record == null) {
            // TODO: 返回错误响应（人工构造）
        }

        // 4. 解析 combinedNodes，校验操作人
        List<ApprovalNode> nodes = parseNodes(record.getCombinedNodes());
        ApprovalNode currentNode = nodes.get(record.getCurrentNode());
        if (!currentNode.getUserId().equals(request.getAccountId())) {
            // TODO: 返回 403 错误响应（人工构造）
        }

        // 5. 插入审批日志
        insertApprovalLog(record, currentNode, request, action);

        // 6. 执行审批
        if (action == 0) { // APPROVE
            handleApprove(record, nodes, currentNode, request);
        } else {           // REJECT
            handleReject(record, currentNode, request);
        }

        // 7. 构造响应（人工实现）
        // TODO: implement response construction
        return buildResponse(record);
    }
}
```

#### handleApprove 逻辑（参考 open-server ApprovalEngine.approve）

1. 插入 ApprovalLog（action=0 同意）
2. 判断 `currentNodeIndex >= nodes.size() - 1`：
   - **是（最后一个节点）**：
     - `record.status = 1(APPROVED)`
     - `record.completedAt = now`
     - 更新 record
     - 查询 subscription（by record.businessId），更新 `status=1(已授权)`, `approvedAt=now`, `approvedBy=applicantId`
   - **否（中间节点）**：
     - `record.currentNode += 1`
     - 更新 record（等待下一节点审批）

#### handleReject 逻辑（参考 open-server ApprovalEngine.reject）

1. 插入 ApprovalLog（action=1 拒绝）
2. `record.status = 2(REJECTED)`, `record.completedAt = now`
3. 更新 record
4. 查询 subscription（by record.businessId），更新 `status=2(已拒绝)`
5. 驳回是终态操作，不推进节点链

### 13-14. SubscriptionMapper 变更

**新增方法**：
```java
Subscription selectById(@Param("id") Long id);
int updateApprovalStatus(Subscription subscription);
```

**新增 SQL**：
```xml
<select id="selectById" resultMap="BaseResultMap">
    SELECT <include refid="Base_Column_List"/>
    FROM openplatform_v2_subscription_t
    WHERE id = #{id}
</select>

<update id="updateApprovalStatus">
    UPDATE openplatform_v2_subscription_t
    SET status = #{status},
        last_update_time = #{lastUpdateTime},
        last_update_by = #{lastUpdateBy},
        approved_at = #{approvedAt},
        approved_by = #{approvedBy}
    WHERE id = #{id}
</update>
```

---

## 人工实现部分（TODO）

| # | 位置 | 说明 |
|:-:|------|------|
| 1 | `verifyPermission(request)` | 权限校验逻辑（验证回调来源合法性） |
| 2 | `ApprovalCallbackResponse` 工厂方法 | 成功/失败响应的构造方式 |
| 3 | `buildResponse(record)` | 响应体构造逻辑（返回 IM 平台所需格式） |
| 4 | `generateId()` | ApprovalLog ID 生成策略 |

---

## 与 open-server 审批逻辑的差异

| 对比项 | open-server | api-server 回调 |
|--------|------------|----------------|
| 操作人来源 | `UserContextHolder.getUserId()` | `request.accountId` |
| 操作人校验 | 无（任何已认证用户可审批） | 校验 accountId == currentNode.userId |
| 响应格式 | `ApiResponse<ApprovalActionResponse>` | `ApprovalCallbackResponse<T>`（自定义） |
| 资源状态更新 | 有（api/event/callback 注册场景） | 无（仅处理权限申请场景） |
| 审批记录查询 | 按 recordId 精确查询 | 按 businessType + businessId + status=0 查询最新记录（命中 idx_business 索引） |
| 事务隔离 | 每个审批独立事务 | 整个回调在一个事务中 |

---

## 变更记录

| 版本 | 日期 | 变更内容 |
|------|------|---------|
| 1.0 | 2026-05-27 | 初始版本 |
