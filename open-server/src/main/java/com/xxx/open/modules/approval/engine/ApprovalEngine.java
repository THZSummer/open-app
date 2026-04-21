package com.xxx.open.modules.approval.engine;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxx.open.common.util.SnowflakeIdGenerator;
import com.xxx.open.modules.approval.dto.ApprovalNodeDto;
import com.xxx.open.modules.approval.entity.ApprovalFlow;
import com.xxx.open.modules.approval.entity.ApprovalLog;
import com.xxx.open.modules.approval.entity.ApprovalRecord;
import com.xxx.open.modules.approval.mapper.ApprovalFlowMapper;
import com.xxx.open.modules.approval.mapper.ApprovalLogMapper;
import com.xxx.open.modules.approval.mapper.ApprovalRecordMapper;
import com.xxx.open.modules.permission.entity.Subscription;
import com.xxx.open.modules.permission.mapper.SubscriptionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 审批引擎
 * 
 * <p>负责处理审批流程的核心逻辑：</p>
 * <ul>
 *   <li>创建审批记录</li>
 *   <li>执行审批操作（同意/驳回/撤销）</li>
 *   <li>更新订阅状态</li>
 *   <li>记录审批日志</li>
 * </ul>
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApprovalEngine {

    private final ApprovalFlowMapper flowMapper;
    private final ApprovalRecordMapper recordMapper;
    private final ApprovalLogMapper logMapper;
    private final SubscriptionMapper subscriptionMapper;
    private final SnowflakeIdGenerator idGenerator;
    private final ObjectMapper objectMapper;

    /**
     * 审批动作枚举
     */
    public static class Action {
        public static final int APPROVE = 0;  // 同意
        public static final int REJECT = 1;   // 拒绝
        public static final int CANCEL = 2;   // 撤销
    }

    /**
     * 审批状态枚举
     */
    public static class Status {
        public static final int PENDING = 0;    // 待审
        public static final int APPROVED = 1;   // 已通过
        public static final int REJECTED = 2;   // 已拒绝
        public static final int CANCELLED = 3;  // 已撤销
    }

    /**
     * 业务类型枚举
     */
    public static class BusinessType {
        public static final String API_REGISTER = "api_register";
        public static final String EVENT_REGISTER = "event_register";
        public static final String CALLBACK_REGISTER = "callback_register";
        public static final String PERMISSION_APPLY = "permission_apply";
    }

    /**
     * 创建审批记录
     * 
     * @param flowId 审批流程ID
     * @param businessType 业务类型
     * @param businessId 业务对象ID
     * @param applicantId 申请人ID
     * @param applicantName 申请人名称
     * @param operator 操作人
     * @return 审批记录
     */
    @Transactional(rollbackFor = Exception.class)
    public ApprovalRecord createApproval(Long flowId, String businessType, Long businessId,
                                         String applicantId, String applicantName, String operator) {
        // 查询审批流程
        ApprovalFlow flow = flowMapper.selectById(flowId);
        if (flow == null) {
            throw new IllegalArgumentException("审批流程不存在: " + flowId);
        }

        // 解析审批节点
        List<ApprovalNodeDto> nodes = parseNodes(flow.getNodes());
        if (nodes == null || nodes.isEmpty()) {
            throw new IllegalArgumentException("审批流程节点配置为空");
        }

        // 创建审批记录
        ApprovalRecord record = new ApprovalRecord();
        record.setId(idGenerator.nextId());
        record.setFlowId(flowId);
        record.setBusinessType(businessType);
        record.setBusinessId(businessId);
        record.setApplicantId(applicantId);
        record.setApplicantName(applicantName);
        record.setStatus(Status.PENDING);
        record.setCurrentNode(0);
        record.setCreateTime(new Date());
        record.setLastUpdateTime(new Date());
        record.setCreateBy(operator);
        record.setLastUpdateBy(operator);

        recordMapper.insert(record);

        log.info("创建审批记录: id={}, flowId={}, businessType={}, businessId={}, applicant={}",
                record.getId(), flowId, businessType, businessId, applicantId);

        return record;
    }

    /**
     * 同意审批
     * 
     * @param recordId 审批记录ID
     * @param operatorId 操作人ID
     * @param operatorName 操作人名称
     * @param comment 审批意见
     * @param operator 操作人（用于审计字段）
     * @return 审批记录
     */
    @Transactional(rollbackFor = Exception.class)
    public ApprovalRecord approve(Long recordId, String operatorId, String operatorName,
                                  String comment, String operator) {
        // 查询审批记录
        ApprovalRecord record = recordMapper.selectById(recordId);
        if (record == null) {
            throw new IllegalArgumentException("审批记录不存在: " + recordId);
        }

        // 检查状态
        if (record.getStatus() != Status.PENDING) {
            throw new IllegalStateException("审批记录状态不正确，无法同意: " + record.getStatus());
        }

        // 查询审批流程
        ApprovalFlow flow = flowMapper.selectById(record.getFlowId());
        List<ApprovalNodeDto> nodes = parseNodes(flow.getNodes());

        // 检查当前节点
        int currentNodeIndex = record.getCurrentNode();
        if (currentNodeIndex >= nodes.size()) {
            throw new IllegalStateException("当前节点索引超出范围");
        }

        // 记录审批日志
        ApprovalLog approvalLog = new ApprovalLog();
        approvalLog.setId(idGenerator.nextId());
        approvalLog.setRecordId(recordId);
        approvalLog.setNodeIndex(currentNodeIndex);
        approvalLog.setOperatorId(operatorId);
        approvalLog.setOperatorName(operatorName);
        approvalLog.setAction(Action.APPROVE);
        approvalLog.setComment(comment);
        approvalLog.setCreateTime(new Date());
        approvalLog.setLastUpdateTime(new Date());
        approvalLog.setCreateBy(operator);
        approvalLog.setLastUpdateBy(operator);

        logMapper.insert(approvalLog);

        // 判断是否所有节点都已审批通过
        if (currentNodeIndex >= nodes.size() - 1) {
            // 最后一个节点，审批通过
            record.setStatus(Status.APPROVED);
            record.setCompletedAt(new Date());
            record.setLastUpdateTime(new Date());
            record.setLastUpdateBy(operator);

            recordMapper.update(record);

            // 更新订阅状态
            updateSubscriptionStatus(record, Status.APPROVED);

            log.info("审批通过: recordId={}, operator={}", recordId, operatorId);
        } else {
            // 进入下一个审批节点
            record.setCurrentNode(currentNodeIndex + 1);
            record.setLastUpdateTime(new Date());
            record.setLastUpdateBy(operator);

            recordMapper.update(record);

            log.info("审批节点通过，进入下一节点: recordId={}, currentNode={}", recordId, record.getCurrentNode());
        }

        return record;
    }

    /**
     * 驳回审批
     * 
     * @param recordId 审批记录ID
     * @param operatorId 操作人ID
     * @param operatorName 操作人名称
     * @param reason 驳回原因
     * @param operator 操作人（用于审计字段）
     * @return 审批记录
     */
    @Transactional(rollbackFor = Exception.class)
    public ApprovalRecord reject(Long recordId, String operatorId, String operatorName,
                                 String reason, String operator) {
        // 查询审批记录
        ApprovalRecord record = recordMapper.selectById(recordId);
        if (record == null) {
            throw new IllegalArgumentException("审批记录不存在: " + recordId);
        }

        // 检查状态
        if (record.getStatus() != Status.PENDING) {
            throw new IllegalStateException("审批记录状态不正确，无法驳回: " + record.getStatus());
        }

        // 记录审批日志
        ApprovalLog approvalLog = new ApprovalLog();
        approvalLog.setId(idGenerator.nextId());
        approvalLog.setRecordId(recordId);
        approvalLog.setNodeIndex(record.getCurrentNode());
        approvalLog.setOperatorId(operatorId);
        approvalLog.setOperatorName(operatorName);
        approvalLog.setAction(Action.REJECT);
        approvalLog.setComment(reason);
        approvalLog.setCreateTime(new Date());
        approvalLog.setLastUpdateTime(new Date());
        approvalLog.setCreateBy(operator);
        approvalLog.setLastUpdateBy(operator);

        logMapper.insert(approvalLog);

        // 更新审批状态
        record.setStatus(Status.REJECTED);
        record.setCompletedAt(new Date());
        record.setLastUpdateTime(new Date());
        record.setLastUpdateBy(operator);

        recordMapper.update(record);

        // 更新订阅状态
        updateSubscriptionStatus(record, Status.REJECTED);

        log.info("审批驳回: recordId={}, operator={}, reason={}", recordId, operatorId, reason);

        return record;
    }

    /**
     * 撤销审批
     * 
     * @param recordId 审批记录ID
     * @param operator 操作人（用于审计字段）
     * @return 审批记录
     */
    @Transactional(rollbackFor = Exception.class)
    public ApprovalRecord cancel(Long recordId, String operator) {
        // 查询审批记录
        ApprovalRecord record = recordMapper.selectById(recordId);
        if (record == null) {
            throw new IllegalArgumentException("审批记录不存在: " + recordId);
        }

        // 检查状态
        if (record.getStatus() != Status.PENDING) {
            throw new IllegalStateException("审批记录状态不正确，无法撤销: " + record.getStatus());
        }

        // 更新审批状态
        record.setStatus(Status.CANCELLED);
        record.setCompletedAt(new Date());
        record.setLastUpdateTime(new Date());
        record.setLastUpdateBy(operator);

        recordMapper.update(record);

        // 更新订阅状态
        updateSubscriptionStatus(record, Status.CANCELLED);

        log.info("审批撤销: recordId={}", recordId);

        return record;
    }

    /**
     * 更新订阅状态
     * 
     * @param record 审批记录
     * @param status 审批状态
     */
    private void updateSubscriptionStatus(ApprovalRecord record, int status) {
        // 仅处理权限申请类型的审批
        if (!BusinessType.PERMISSION_APPLY.equals(record.getBusinessType())) {
            return;
        }

        // 查询订阅记录
        Subscription subscription = subscriptionMapper.selectById(record.getBusinessId());
        if (subscription == null) {
            log.warn("订阅记录不存在，无法更新状态: businessId={}", record.getBusinessId());
            return;
        }

        // 根据审批状态更新订阅状态
        int subscriptionStatus;
        if (status == Status.APPROVED) {
            subscriptionStatus = 1; // 已授权
        } else if (status == Status.REJECTED) {
            subscriptionStatus = 2; // 已拒绝
        } else if (status == Status.CANCELLED) {
            subscriptionStatus = 3; // 已取消
        } else {
            return;
        }

        // 更新订阅状态
        subscription.setStatus(subscriptionStatus);
        subscription.setLastUpdateTime(new Date());
        subscription.setLastUpdateBy(record.getApplicantId());

        if (status == Status.APPROVED) {
            subscription.setApprovedAt(new Date());
            subscription.setApprovedBy(record.getApplicantId());
        }

        subscriptionMapper.update(subscription);

        log.info("更新订阅状态: subscriptionId={}, status={}", subscription.getId(), subscriptionStatus);
    }

    /**
     * 解析审批节点配置
     * 
     * @param nodesJson 节点 JSON 字符串
     * @return 节点列表
     */
    public List<ApprovalNodeDto> parseNodes(String nodesJson) {
        if (nodesJson == null || nodesJson.trim().isEmpty()) {
            return new ArrayList<>();
        }

        try {
            return objectMapper.readValue(nodesJson, new TypeReference<List<ApprovalNodeDto>>() {});
        } catch (Exception e) {
            log.error("解析审批节点配置失败: {}", nodesJson, e);
            return new ArrayList<>();
        }
    }

    /**
     * 序列化审批节点配置
     * 
     * @param nodes 节点列表
     * @return JSON 字符串
     */
    public String serializeNodes(List<ApprovalNodeDto> nodes) {
        try {
            return objectMapper.writeValueAsString(nodes);
        } catch (Exception e) {
            log.error("序列化审批节点配置失败", e);
            return "[]";
        }
    }
}
