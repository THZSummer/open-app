package com.xxx.open.modules.approval.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxx.open.common.exception.BusinessException;
import com.xxx.open.common.util.SnowflakeIdGenerator;
import com.xxx.open.modules.api.entity.Api;
import com.xxx.open.modules.api.mapper.ApiMapper;
import com.xxx.open.modules.approval.dto.*;
import com.xxx.open.modules.approval.entity.ApprovalFlow;
import com.xxx.open.modules.approval.entity.ApprovalLog;
import com.xxx.open.modules.approval.entity.ApprovalRecord;
import com.xxx.open.modules.approval.engine.ApprovalEngine;
import com.xxx.open.modules.approval.mapper.ApprovalFlowMapper;
import com.xxx.open.modules.approval.mapper.ApprovalLogMapper;
import com.xxx.open.modules.approval.mapper.ApprovalRecordMapper;
import com.xxx.open.modules.callback.entity.Callback;
import com.xxx.open.modules.callback.mapper.CallbackMapper;
import com.xxx.open.modules.event.entity.Event;
import com.xxx.open.modules.event.mapper.EventMapper;
import com.xxx.open.modules.permission.entity.Subscription;
import com.xxx.open.modules.permission.mapper.SubscriptionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 审批管理 Service
 * 
 * <p>负责审批相关的业务逻辑处理</p>
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApprovalService {

    private final ApprovalFlowMapper flowMapper;
    private final ApprovalRecordMapper recordMapper;
    private final ApprovalLogMapper logMapper;
    private final ApprovalEngine approvalEngine;
    private final SnowflakeIdGenerator idGenerator;
    private final ObjectMapper objectMapper;

    // 注入资源 Mapper（用于获取业务数据）
    private final ApiMapper apiMapper;
    private final EventMapper eventMapper;
    private final CallbackMapper callbackMapper;
    private final SubscriptionMapper subscriptionMapper;

    // ==================== 审批流程模板管理 ====================

    /**
     * 查询审批流程列表
     */
    public List<ApprovalFlowListResponse> getFlowList(ApprovalFlowListRequest request) {
        int offset = (request.getCurPage() - 1) * request.getPageSize();
        List<ApprovalFlow> flows = flowMapper.selectList(request.getKeyword(), offset, request.getPageSize());

        return flows.stream().map(flow -> {
            ApprovalFlowListResponse response = new ApprovalFlowListResponse();
            response.setId(String.valueOf(flow.getId()));
            response.setNameCn(flow.getNameCn());
            response.setNameEn(flow.getNameEn());
            response.setCode(flow.getCode());
            response.setIsDefault(flow.getIsDefault());
            response.setStatus(flow.getStatus());
            return response;
        }).collect(Collectors.toList());
    }

    /**
     * 统计审批流程数量
     */
    public Long countFlowList(String keyword) {
        return flowMapper.countList(keyword);
    }

    /**
     * 获取审批流程详情
     */
    public ApprovalFlowDetailResponse getFlowDetail(Long id) {
        ApprovalFlow flow = flowMapper.selectById(id);
        if (flow == null) {
            throw new BusinessException("404", "审批流程不存在", "Approval flow not found");
        }

        ApprovalFlowDetailResponse response = new ApprovalFlowDetailResponse();
        response.setId(String.valueOf(flow.getId()));
        response.setNameCn(flow.getNameCn());
        response.setNameEn(flow.getNameEn());
        response.setCode(flow.getCode());
        response.setIsDefault(flow.getIsDefault());
        response.setStatus(flow.getStatus());
        response.setNodes(approvalEngine.parseNodes(flow.getNodes()));

        return response;
    }

    /**
     * 创建审批流程
     */
    @Transactional(rollbackFor = Exception.class)
    public ApprovalFlowDetailResponse createFlow(ApprovalFlowCreateRequest request, String operator) {
        // 检查编码唯一性
        if (flowMapper.countByCode(request.getCode()) > 0) {
            throw new BusinessException("409", "流程编码已存在", "Flow code already exists");
        }

        // 如果设置为默认流程，清除其他默认流程
        if (request.getIsDefault() != null && request.getIsDefault() == 1) {
            clearDefaultFlow();
        }

        // 创建流程
        ApprovalFlow flow = new ApprovalFlow();
        flow.setId(idGenerator.nextId());
        flow.setNameCn(request.getNameCn());
        flow.setNameEn(request.getNameEn());
        flow.setCode(request.getCode());
        flow.setIsDefault(request.getIsDefault() != null ? request.getIsDefault() : 0);
        flow.setNodes(approvalEngine.serializeNodes(request.getNodes()));
        flow.setStatus(1);
        flow.setCreateTime(new Date());
        flow.setLastUpdateTime(new Date());
        flow.setCreateBy(operator);
        flow.setLastUpdateBy(operator);

        flowMapper.insert(flow);

        log.info("创建审批流程: id={}, code={}, operator={}", flow.getId(), flow.getCode(), operator);

        return getFlowDetail(flow.getId());
    }

    /**
     * 更新审批流程
     */
    @Transactional(rollbackFor = Exception.class)
    public ApprovalFlowDetailResponse updateFlow(Long id, ApprovalFlowUpdateRequest request, String operator) {
        ApprovalFlow flow = flowMapper.selectById(id);
        if (flow == null) {
            throw new BusinessException("404", "审批流程不存在", "Approval flow not found");
        }

        // 如果设置为默认流程，清除其他默认流程
        if (request.getIsDefault() != null && request.getIsDefault() == 1) {
            clearDefaultFlow();
        }

        // 更新流程
        flow.setNameCn(request.getNameCn());
        flow.setNameEn(request.getNameEn());
        flow.setIsDefault(request.getIsDefault() != null ? request.getIsDefault() : flow.getIsDefault());
        flow.setNodes(approvalEngine.serializeNodes(request.getNodes()));
        flow.setLastUpdateTime(new Date());
        flow.setLastUpdateBy(operator);

        flowMapper.update(flow);

        log.info("更新审批流程: id={}, operator={}", id, operator);

        return getFlowDetail(id);
    }

    /**
     * 清除默认流程标记
     */
    private void clearDefaultFlow() {
        ApprovalFlow defaultFlow = flowMapper.selectDefaultFlow();
        if (defaultFlow != null) {
            defaultFlow.setIsDefault(0);
            defaultFlow.setLastUpdateTime(new Date());
            flowMapper.update(defaultFlow);
        }
    }

    // ==================== 审批执行管理 ====================

    /**
     * 查询待审批列表
     */
    public List<ApprovalPendingListResponse> getPendingList(ApprovalPendingListRequest request) {
        int offset = (request.getCurPage() - 1) * request.getPageSize();
        List<ApprovalRecord> records = recordMapper.selectPendingList(
                request.getType(), 
                request.getKeyword(), 
                request.getStatus(),
                request.getApplicantId(),
                offset, 
                request.getPageSize());

        return records.stream().map(record -> {
            ApprovalPendingListResponse response = new ApprovalPendingListResponse();
            response.setId(String.valueOf(record.getId()));
            response.setType(getApprovalType(record.getBusinessType()));
            response.setBusinessType(record.getBusinessType().replace("_register", "").replace("_apply", ""));
            response.setBusinessId(String.valueOf(record.getBusinessId()));
            response.setBusinessName(getBusinessName(record.getBusinessType(), record.getBusinessId()));
            response.setApplicantId(record.getApplicantId());
            response.setApplicantName(record.getApplicantName());
            response.setStatus(record.getStatus());
            response.setCurrentNode(record.getCurrentNode());
            response.setCreateTime(record.getCreateTime());
            return response;
        }).collect(Collectors.toList());
    }

    /**
     * 统计待审批数量
     */
    public Long countPendingList(String type, String keyword, Integer status, String applicantId) {
        return recordMapper.countPendingList(type, keyword, status, applicantId);
    }

    /**
     * 获取审批详情
     */
    public ApprovalDetailResponse getApprovalDetail(Long id) {
        ApprovalRecord record = recordMapper.selectById(id);
        if (record == null) {
            throw new BusinessException("404", "审批记录不存在: " + id, "Approval record not found: " + id);
        }

        // 查询审批流程
        ApprovalFlow flow = flowMapper.selectById(record.getFlowId());
        if (flow == null) {
            throw new BusinessException("404", "审批流程不存在: " + record.getFlowId(), "Approval flow not found: " + record.getFlowId());
        }
        List<ApprovalNodeDto> nodes = approvalEngine.parseNodes(flow.getNodes());

        // 查询审批日志
        List<ApprovalLog> logs = logMapper.selectByRecordId(id);

        // 填充节点状态
        Map<Integer, Integer> nodeStatusMap = new HashMap<>();
        for (ApprovalLog log : logs) {
            nodeStatusMap.put(log.getNodeIndex(), log.getAction() == 0 ? 1 : 2);
        }

        for (int i = 0; i < nodes.size(); i++) {
            ApprovalNodeDto node = nodes.get(i);
            if (i < record.getCurrentNode()) {
                // 已处理的节点（在当前节点之前）
                node.setStatus(nodeStatusMap.getOrDefault(i, 1));
            } else if (i == record.getCurrentNode()) {
                // 当前节点：待审批时显示0，已审批时从日志获取状态
                if (record.getStatus() == 0) {
                    node.setStatus(0);  // 待审批
                } else {
                    // 审批已通过或已拒绝，从日志获取当前节点状态
                    node.setStatus(nodeStatusMap.getOrDefault(i, 1));
                }
            } else {
                // 未处理的节点（在当前节点之后）
                node.setStatus(null);
            }
        }

        // 构建响应
        ApprovalDetailResponse response = new ApprovalDetailResponse();
        response.setId(String.valueOf(record.getId()));
        response.setType(getApprovalType(record.getBusinessType()));
        response.setBusinessType(record.getBusinessType().replace("_register", "").replace("_apply", ""));
        response.setBusinessId(String.valueOf(record.getBusinessId()));
        response.setBusinessData(getBusinessData(record.getBusinessType(), record.getBusinessId()));
        response.setApplicantId(record.getApplicantId());
        response.setApplicantName(record.getApplicantName());
        response.setStatus(record.getStatus());
        response.setFlowId(String.valueOf(record.getFlowId()));
        response.setCurrentNode(record.getCurrentNode());
        response.setNodes(nodes);
        response.setLogs(buildLogDtos(logs, nodes));
        response.setCreateTime(record.getCreateTime());

        return response;
    }

    /**
     * 同意审批
     */
    @Transactional(rollbackFor = Exception.class)
    public ApprovalActionResponse approve(Long id, ApprovalActionRequest request, String operatorId,
                                          String operatorName, String operator) {
        ApprovalRecord record = approvalEngine.approve(id, operatorId, operatorName, 
                request.getComment(), operator);

        String message = record.getStatus() == ApprovalEngine.Status.APPROVED 
                ? "审批通过" : "审批节点通过，等待下一节点审批";

        return ApprovalActionResponse.builder()
                .id(String.valueOf(record.getId()))
                .status(record.getStatus())
                .message(message)
                .build();
    }

    /**
     * 驳回审批
     */
    @Transactional(rollbackFor = Exception.class)
    public ApprovalActionResponse reject(Long id, ApprovalActionRequest request, String operatorId,
                                         String operatorName, String operator) {
        if (request.getReason() == null || request.getReason().trim().isEmpty()) {
            throw new BusinessException("400", "驳回原因不能为空", "Reject reason is required");
        }

        ApprovalRecord record = approvalEngine.reject(id, operatorId, operatorName,
                request.getReason(), operator);

        return ApprovalActionResponse.builder()
                .id(String.valueOf(record.getId()))
                .status(record.getStatus())
                .message("审批已驳回")
                .build();
    }

    /**
     * 撤销审批
     */
    @Transactional(rollbackFor = Exception.class)
    public ApprovalActionResponse cancel(Long id, String operator) {
        ApprovalRecord record = approvalEngine.cancel(id, operator);

        return ApprovalActionResponse.builder()
                .id(String.valueOf(record.getId()))
                .status(record.getStatus())
                .message("审批已撤销")
                .build();
    }

    /**
     * 批量同意审批
     * 
     * 注意：此方法不开启事务，每个审批操作在独立事务中执行。
     * 这样可以确保单个审批失败不会影响其他审批的处理。
     */
    public BatchApprovalResponse batchApprove(BatchApprovalRequest request, String operatorId,
                                              String operatorName, String operator) {
        int successCount = 0;
        List<BatchApprovalResponse.FailedItem> failedItems = new ArrayList<>();

        for (String approvalIdStr : request.getApprovalIds()) {
            try {
                Long approvalId = Long.parseLong(approvalIdStr);
                approvalEngine.approve(approvalId, operatorId, operatorName, 
                        request.getComment(), operator);
                successCount++;
            } catch (BusinessException e) {
                // 业务异常，记录失败原因
                failedItems.add(BatchApprovalResponse.FailedItem.builder()
                        .approvalId(approvalIdStr)
                        .reason(e.getMessageZh())
                        .build());
            } catch (Exception e) {
                // 其他异常，记录失败原因
                log.error("批量审批失败: approvalId={}", approvalIdStr, e);
                failedItems.add(BatchApprovalResponse.FailedItem.builder()
                        .approvalId(approvalIdStr)
                        .reason(e.getMessage())
                        .build());
            }
        }

        return BatchApprovalResponse.builder()
                .successCount(successCount)
                .failedCount(failedItems.size())
                .failedItems(failedItems.isEmpty() ? null : failedItems)
                .message(String.format("批量审批完成，成功%d条，失败%d条", successCount, failedItems.size()))
                .build();
    }

    /**
     * 批量驳回审批
     * 
     * 注意：此方法不开启事务，每个审批操作在独立事务中执行。
     * 这样可以确保单个审批失败不会影响其他审批的处理。
     */
    public BatchApprovalResponse batchReject(BatchApprovalRequest request, String operatorId,
                                             String operatorName, String operator) {
        if (request.getReason() == null || request.getReason().trim().isEmpty()) {
            throw new BusinessException("400", "驳回原因不能为空", "Reject reason is required");
        }

        int successCount = 0;
        List<BatchApprovalResponse.FailedItem> failedItems = new ArrayList<>();

        for (String approvalIdStr : request.getApprovalIds()) {
            try {
                Long approvalId = Long.parseLong(approvalIdStr);
                approvalEngine.reject(approvalId, operatorId, operatorName,
                        request.getReason(), operator);
                successCount++;
            } catch (BusinessException e) {
                // 业务异常，记录失败原因
                failedItems.add(BatchApprovalResponse.FailedItem.builder()
                        .approvalId(approvalIdStr)
                        .reason(e.getMessageZh())
                        .build());
            } catch (Exception e) {
                // 其他异常，记录失败原因
                log.error("批量驳回失败: approvalId={}", approvalIdStr, e);
                failedItems.add(BatchApprovalResponse.FailedItem.builder()
                        .approvalId(approvalIdStr)
                        .reason(e.getMessage())
                        .build());
            }
        }

        return BatchApprovalResponse.builder()
                .successCount(successCount)
                .failedCount(failedItems.size())
                .failedItems(failedItems.isEmpty() ? null : failedItems)
                .message(String.format("批量驳回完成，成功%d条，失败%d条", successCount, failedItems.size()))
                .build();
    }

    // ==================== 辅助方法 ====================

    /**
     * 获取审批类型
     */
    private String getApprovalType(String businessType) {
        if (businessType.endsWith("_register")) {
            return "resource_register";
        } else if (businessType.endsWith("_apply")) {
            return "permission_apply";
        }
        return businessType;
    }

    /**
     * 获取业务名称
     */
    private String getBusinessName(String businessType, Long businessId) {
        try {
            switch (businessType) {
                case "api_register":
                    Api api = apiMapper.selectById(businessId);
                    return api != null ? api.getNameCn() : null;
                case "event_register":
                    Event event = eventMapper.selectById(businessId);
                    return event != null ? event.getNameCn() : null;
                case "callback_register":
                    Callback callback = callbackMapper.selectById(businessId);
                    return callback != null ? callback.getNameCn() : null;
                case "permission_apply":
                    Subscription subscription = subscriptionMapper.selectById(businessId);
                    if (subscription != null) {
                        // TODO: 从权限表获取名称
                        return "权限申请";
                    }
                    return null;
                default:
                    return null;
            }
        } catch (Exception e) {
            log.error("获取业务名称失败: businessType={}, businessId={}", businessType, businessId, e);
            return null;
        }
    }

    /**
     * 获取业务数据
     */
    private Map<String, Object> getBusinessData(String businessType, Long businessId) {
        try {
            Map<String, Object> data = new HashMap<>();

            switch (businessType) {
                case "api_register":
                    Api api = apiMapper.selectById(businessId);
                    if (api != null) {
                        data.put("nameCn", api.getNameCn());
                        data.put("path", api.getPath());
                        data.put("method", api.getMethod());
                    }
                    break;
                case "event_register":
                    Event event = eventMapper.selectById(businessId);
                    if (event != null) {
                        data.put("nameCn", event.getNameCn());
                        data.put("topic", event.getTopic());
                    }
                    break;
                case "callback_register":
                    Callback callback = callbackMapper.selectById(businessId);
                    if (callback != null) {
                        data.put("nameCn", callback.getNameCn());
                    }
                    break;
                case "permission_apply":
                    Subscription subscription = subscriptionMapper.selectById(businessId);
                    if (subscription != null) {
                        data.put("appId", subscription.getAppId());
                        data.put("permissionId", subscription.getPermissionId());
                    }
                    break;
            }

            return data;
        } catch (Exception e) {
            log.error("获取业务数据失败: businessType={}, businessId={}", businessType, businessId, e);
            return new HashMap<>();
        }
    }

    /**
     * 构建日志 DTO 列表
     */
    private List<ApprovalLogDto> buildLogDtos(List<ApprovalLog> logs, List<ApprovalNodeDto> nodes) {
        return logs.stream().map(log -> {
            ApprovalLogDto dto = new ApprovalLogDto();
            dto.setOrder(log.getNodeIndex() + 1);
            dto.setOperatorId(log.getOperatorId());
            dto.setOperatorName(log.getOperatorName());
            dto.setAction(log.getAction());
            dto.setActionName(getActionName(log.getAction()));
            dto.setComment(log.getComment());
            dto.setCreateTime(log.getCreateTime());
            return dto;
        }).collect(Collectors.toList());
    }

    /**
     * 获取操作类型名称
     */
    private String getActionName(Integer action) {
        if (action == null) {
            return null;
        }
        switch (action) {
            case 0:
                return "同意";
            case 1:
                return "拒绝";
            case 2:
                return "撤销";
            case 3:
                return "转交";
            default:
                return "未知";
        }
    }
}
