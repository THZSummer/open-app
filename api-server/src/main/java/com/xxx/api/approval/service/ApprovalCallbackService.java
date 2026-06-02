package com.xxx.api.approval.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxx.api.approval.dto.ApprovalCallbackRequest;
import com.xxx.api.approval.dto.ApprovalCallbackResponse;
import com.xxx.api.approval.dto.ApprovalCardContent;
import com.xxx.api.approval.entity.ApprovalLog;
import com.xxx.api.approval.entity.ApprovalNode;
import com.xxx.api.approval.entity.ApprovalRecord;
import com.xxx.api.approval.handler.ApprovalCallbackHandler;
import com.xxx.api.approval.handler.ApprovalCallbackHandlerFactory;
import com.xxx.api.approval.mapper.ApprovalLogMapper;
import com.xxx.api.approval.mapper.ApprovalRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 审批卡片回调业务逻辑 Service
 *
 * <p>处理 IM 平台卡片回调，执行审批通过/驳回操作。
 * 参考 open-server ApprovalEngine 的 approve/reject 逻辑实现。</p>
 *
 * <p>核心流程：
 * <ol>
 *   <li>权限校验（预留 TODO）</li>
 *   <li>解析 content JSON → 提取审批动作（data: "1"=同意, "0"=驳回）</li>
 *   <li>查询待审批记录（businessType + businessId 命中 idx_business 索引）</li>
 *   <li>操作人校验（accountId == currentNode.userId）</li>
 *   <li>获取策略处理器（ApprovalCallbackHandlerFactory）</li>
 *   <li>插入审批日志</li>
 *   <li>执行审批通过/驳回 + 策略回调</li>
 *   <li>构造响应（人工实现）</li>
 * </ol>
 * </p>
 *
 * @author SDDU Build Agent
 * @version 2.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ApprovalCallbackService {

    private final ApprovalRecordMapper approvalRecordMapper;
    private final ApprovalLogMapper approvalLogMapper;
    private final ObjectMapper objectMapper;
    private final ApprovalCallbackHandlerFactory handlerFactory;

    /** 审批动作常量 */
    private static final int ACTION_APPROVE = 0;
    private static final int ACTION_REJECT = 1;

    /** 审批状态常量 */
    private static final int STATUS_APPROVED = 1;
    private static final int STATUS_REJECTED = 2;

    /**
     * 处理审批卡片回调
     *
     * @param businessId   业务ID（对应审批记录的 business_id）
     * @param businessType 业务类型（如 api_permission_apply / event_permission_apply / callback_permission_apply）
     * @param request      回调请求体
     * @return 自定义回调响应
     */
    @Transactional(rollbackFor = Exception.class)
    public ApprovalCallbackResponse<?> handleCallback(Long businessId,
                                                       String businessType,
                                                       ApprovalCallbackRequest request) {

        // 1. 权限校验（预留，手工实现）
        // TODO: implement permission verification
        verifyPermission(request);

        // 2. 解析 content JSON 字符串，提取审批动作
        ApprovalCardContent content = parseContent(request.getContent());
        int action = parseAction(content.getData());

        // 3. 查询待审批记录（businessType + businessId 命中 idx_business 索引）
        ApprovalRecord record = approvalRecordMapper
                .selectLatestPendingByBusiness(businessType, businessId);
        if (record == null) {
            log.warn("No pending approval record found: businessType={}, businessId={}", businessType, businessId);
            return buildErrorResponse(400, 40001,
                    "审批记录不存在或已处理", "Approval record not found or already processed");
        }

        // 4. 解析 combinedNodes，校验操作人
        List<ApprovalNode> nodes = parseNodes(record.getCombinedNodes());
        if (nodes.isEmpty()) {
            log.error("Approval nodes configuration is empty: recordId={}", record.getId());
            return buildErrorResponse(500, 50001,
                    "审批节点配置为空", "Approval nodes configuration is empty");
        }

        int currentNodeIndex = record.getCurrentNode();
        if (currentNodeIndex >= nodes.size()) {
            log.error("Current node index out of range: recordId={}, currentNode={}", record.getId(), currentNodeIndex);
            return buildErrorResponse(500, 50002,
                    "当前节点索引超出范围", "Current node index out of range");
        }

        ApprovalNode currentNode = nodes.get(currentNodeIndex);
        if (!currentNode.getUserId().equals(request.getAccountId())) {
            log.warn("Operator mismatch: expected={}, actual={}, recordId={}",
                    currentNode.getUserId(), request.getAccountId(), record.getId());
            return buildErrorResponse(403, 40301,
                    "只有当前审批人可以操作", "Only the current approver can perform this action");
        }

        // 5. 获取策略处理器
        ApprovalCallbackHandler handler = handlerFactory.getHandler(businessType);
        if (handler == null) {
            log.error("No handler registered for businessType: {}", businessType);
            return buildErrorResponse(400, 40002,
                    "不支持的业务类型: " + businessType,
                    "Unsupported business type: " + businessType);
        }

        // 6. 插入审批日志
        insertApprovalLog(record, currentNode, request, action);

        // 7. 执行审批 + 策略回调
        if (action == ACTION_APPROVE) {
            handleApprove(record, nodes, currentNodeIndex, handler);
        } else {
            handleReject(record, handler);
        }

        // 8. 构造响应（人工实现）
        // TODO: implement response construction
        return buildSuccessResponse(record);
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 权限校验（预留，手工实现）
     */
    private void verifyPermission(ApprovalCallbackRequest request) {
        // TODO: implement permission verification
        log.debug("Permission verification skipped (TODO)");
    }

    /**
     * 解析 content JSON 字符串
     */
    private ApprovalCardContent parseContent(String contentJson) {
        if (contentJson == null || contentJson.trim().isEmpty()) {
            throw new IllegalArgumentException("content 不能为空");
        }
        try {
            return objectMapper.readValue(contentJson, ApprovalCardContent.class);
        } catch (Exception e) {
            log.error("Failed to parse content JSON: {}", contentJson, e);
            throw new IllegalArgumentException("content 格式错误");
        }
    }

    /**
     * 解析审批动作
     *
     * @param data "1"=同意, "0"=驳回
     * @return ACTION_APPROVE(0) 或 ACTION_REJECT(1)
     */
    private int parseAction(String data) {
        if (data == null || data.trim().isEmpty()) {
            throw new IllegalArgumentException("审批动作(data)不能为空");
        }
        switch (data.trim()) {
            case "1":
                return ACTION_APPROVE;
            case "0":
                return ACTION_REJECT;
            default:
                throw new IllegalArgumentException("无效的审批动作: " + data);
        }
    }

    /**
     * 解析审批节点 JSON
     */
    private List<ApprovalNode> parseNodes(String nodesJson) {
        if (nodesJson == null || nodesJson.trim().isEmpty()) {
            return new ArrayList<>();
        }
        try {
            List<ApprovalNode> nodes = objectMapper.readValue(nodesJson,
                    new TypeReference<List<ApprovalNode>>() {});
            return nodes != null ? nodes : new ArrayList<>();
        } catch (Exception e) {
            log.error("Failed to parse approval nodes: {}", nodesJson, e);
            return new ArrayList<>();
        }
    }

    /**
     * 插入审批日志
     */
    private void insertApprovalLog(ApprovalRecord record, ApprovalNode currentNode,
                                    ApprovalCallbackRequest request, int action) {
        ApprovalLog approvalLog = new ApprovalLog();
        // TODO: ID 生成策略人工实现
        approvalLog.setId(generateId());
        approvalLog.setRecordId(record.getId());
        approvalLog.setNodeIndex(record.getCurrentNode());
        approvalLog.setLevel(currentNode.getLevel());
        approvalLog.setOperatorId(request.getAccountId());
        approvalLog.setOperatorName(currentNode.getUserName());
        approvalLog.setAction(action);
        approvalLog.setComment(null);
        approvalLog.setCreateTime(new Date());
        approvalLog.setLastUpdateTime(new Date());
        approvalLog.setCreateBy(request.getAccountId());
        approvalLog.setLastUpdateBy(request.getAccountId());

        approvalLogMapper.insert(approvalLog);
    }

    /**
     * 处理审批通过
     *
     * <p>参考 open-server ApprovalEngine.approve：</p>
     * <ul>
     *   <li>最后一个节点：审批通过 → 策略回调 handler.onApproved()</li>
     *   <li>非最后节点：推进到下一节点（无策略回调）</li>
     * </ul>
     */
    private void handleApprove(ApprovalRecord record, List<ApprovalNode> nodes,
                                int currentNodeIndex, ApprovalCallbackHandler handler) {
        Date now = new Date();

        if (currentNodeIndex >= nodes.size() - 1) {
            // 最后一个节点，审批通过
            record.setStatus(STATUS_APPROVED);
            record.setCompletedAt(now);
            record.setLastUpdateTime(now);
            record.setLastUpdateBy(record.getApplicantId());
            approvalRecordMapper.update(record);

            // 策略回调：审批通过后的业务副作用
            handler.onApproved(record);

            log.info("Approval approved: recordId={}, businessType={}, businessId={}",
                    record.getId(), record.getBusinessType(), record.getBusinessId());
        } else {
            // 进入下一个审批节点
            record.setCurrentNode(currentNodeIndex + 1);
            record.setLastUpdateTime(now);
            record.setLastUpdateBy(record.getApplicantId());
            approvalRecordMapper.update(record);

            log.info("Approval node passed: recordId={}, nextNode={}", record.getId(), record.getCurrentNode());
        }
    }

    /**
     * 处理审批驳回
     *
     * <p>参考 open-server ApprovalEngine.reject：
     * 驳回是终态操作，直接标记为已拒绝，不推进节点链。
     * 驳回后调用策略回调 handler.onRejected()。</p>
     */
    private void handleReject(ApprovalRecord record, ApprovalCallbackHandler handler) {
        Date now = new Date();

        record.setStatus(STATUS_REJECTED);
        record.setCompletedAt(now);
        record.setLastUpdateTime(now);
        record.setLastUpdateBy(record.getApplicantId());
        approvalRecordMapper.update(record);

        // 策略回调：审批驳回后的业务副作用
        handler.onRejected(record);

        log.info("Approval rejected: recordId={}, businessType={}, businessId={}",
                record.getId(), record.getBusinessType(), record.getBusinessId());
    }

    /**
     * 生成 ID（TODO: 人工实现）
     */
    private Long generateId() {
        // TODO: implement ID generation strategy
        return System.currentTimeMillis();
    }

    // ==================== 响应构造（人工实现） ====================

    /**
     * 构造成功响应（TODO: 人工实现）
     */
    private ApprovalCallbackResponse<?> buildSuccessResponse(ApprovalRecord record) {
        // TODO: implement success response construction for IM platform
        return ApprovalCallbackResponse.<Void>builder()
                .status(200)
                .build();
    }

    /**
     * 构造错误响应（TODO: 人工实现）
     */
    private ApprovalCallbackResponse<?> buildErrorResponse(int status, int code,
                                                            String userMessageZh, String userMessageEn) {
        // TODO: implement error response construction for IM platform
        return ApprovalCallbackResponse.<Void>builder()
                .status(status)
                .errorInfo(ApprovalCallbackResponse.ErrorInfo.builder()
                        .code(code)
                        .userMessageZh(userMessageZh)
                        .userMessageEn(userMessageEn)
                        .build())
                .build();
    }
}
