package com.xxx.it.works.wecode.v2.modules.approval.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxx.it.works.wecode.v2.common.exception.BusinessException;
import com.xxx.it.works.wecode.v2.common.id.IdGeneratorStrategy;
import com.xxx.it.works.wecode.v2.modules.api.entity.Api;
import com.xxx.it.works.wecode.v2.modules.api.mapper.ApiMapper;
import com.xxx.it.works.wecode.v2.modules.approval.dto.*;
import com.xxx.it.works.wecode.v2.modules.approval.entity.ApprovalFlow;
import com.xxx.it.works.wecode.v2.modules.approval.entity.ApprovalLog;
import com.xxx.it.works.wecode.v2.modules.approval.entity.ApprovalRecord;
import com.xxx.it.works.wecode.v2.modules.approval.engine.ApprovalEngine;
import com.xxx.it.works.wecode.v2.modules.approval.mapper.ApprovalFlowMapper;
import com.xxx.it.works.wecode.v2.modules.approval.mapper.ApprovalLogMapper;
import com.xxx.it.works.wecode.v2.modules.approval.mapper.ApprovalRecordMapper;
import com.xxx.it.works.wecode.v2.modules.callback.entity.Callback;
import com.xxx.it.works.wecode.v2.modules.callback.mapper.CallbackMapper;
import com.xxx.it.works.wecode.v2.modules.event.entity.Event;
import com.xxx.it.works.wecode.v2.modules.event.mapper.EventMapper;
import com.xxx.it.works.wecode.v2.modules.event.entity.Permission;
import com.xxx.it.works.wecode.v2.modules.event.mapper.PermissionMapper;
import com.xxx.it.works.wecode.v2.modules.permission.entity.Subscription;
import com.xxx.it.works.wecode.v2.modules.permission.mapper.SubscriptionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * 审批管理 Service
 *
 * <p>v2.8.0 重写版本</p>
 *
 * <p>负责审批相关的业务逻辑处理：</p>
 * <ul>
 *   <li>审批流程模板管理</li>
 *   <li>审批执行管理</li>
 *   <li>审批详情查询</li>
 * </ul>
 *
 * <p>核心设计变更（v2.8.0）：</p>
 * <ul>
 *   <li>移除 isDefault 字段，用 code='global' 标识全局审批</li>
 *   <li>移除 flowId 字段，审批记录直接存储 combinedNodes</li>
 *   <li>审批详情从 combinedNodes 解析节点</li>
 *   <li>审批节点显示 level 标记（resource/scene/global）</li>
 * </ul>
 *
 * @author SDDU Build Agent
 * @version 2.8.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApprovalService {

    private final ApprovalFlowMapper flowMapper;
    private final ApprovalRecordMapper recordMapper;
    private final ApprovalLogMapper logMapper;
    private final ApprovalEngine approvalEngine;
    private final IdGeneratorStrategy idGenerator;
    private final ObjectMapper objectMapper;

    // 注入资源 Mapper（用于获取业务数据）
    private final ApiMapper apiMapper;
    private final EventMapper eventMapper;
    private final CallbackMapper callbackMapper;
    private final SubscriptionMapper subscriptionMapper;
    private final PermissionMapper permissionMapper;

    // ==================== 审批流程模板管理 ====================

    /**
     * 查询审批流程列表
     *
     * v2.8.0变更：移除 isDefault 字段
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

            // ✅ v2.8.0变更：移除 isDefault 字段
            response.setStatus(flow.getStatus());
            response.setNodes(approvalEngine.parseNodes(flow.getNodes()));
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
     *
     * v2.8.0变更：移除 isDefault 字段
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

        // ✅ v2.8.0变更：移除 isDefault 字段
        response.setStatus(flow.getStatus());
        response.setNodes(approvalEngine.parseNodes(flow.getNodes()));

        return response;
    }

    /**
     * 创建审批流程
     *
     * v2.8.0变更：移除 isDefault 字段，用 code='global' 标识全局审批
     */
    @Transactional(rollbackFor = Exception.class)
    public ApprovalFlowDetailResponse createFlow(ApprovalFlowCreateRequest request, String operator) {

        // 检查编码唯一性
        if (flowMapper.countByCode(request.getCode()) > 0) {
            throw new BusinessException("409", "流程编码已存在", "Flow code already exists");
        }

        // ✅ v2.8.0变更：移除 isDefault 相关逻辑
        // 创建审批流程时，通过 code 标识审批类型：
        // - code='global' 表示全局审批流程
        // - code='api_permission_apply' 表示场景审批流程

        // 创建流程
        ApprovalFlow flow = new ApprovalFlow();
        flow.setId(idGenerator.nextId());
        flow.setNameCn(request.getNameCn());
        flow.setNameEn(request.getNameEn());
        flow.setCode(request.getCode());
        flow.setNodes(approvalEngine.serializeNodes(request.getNodes()));
        flow.setStatus(1);
        flow.setCreateTime(new Date());
        flow.setLastUpdateTime(new Date());
        flow.setCreateBy(operator);
        flow.setLastUpdateBy(operator);

        flowMapper.insert(flow);

        log.info("Create approval flow: id={}, code={}, operator={}", flow.getId(), flow.getCode(), operator);

        return getFlowDetail(flow.getId());
    }

    /**
     * 更新审批流程
     *
     * v2.8.0变更：移除 isDefault 字段
     */
    @Transactional(rollbackFor = Exception.class)
    public ApprovalFlowDetailResponse updateFlow(Long id, ApprovalFlowUpdateRequest request, String operator) {
        ApprovalFlow flow = flowMapper.selectById(id);
        if (flow == null) {
            throw new BusinessException("404", "审批流程不存在", "Approval flow not found");
        }

        // ✅ v2.8.0变更：移除 isDefault 相关逻辑

        // 更新流程
        flow.setNameCn(request.getNameCn());
        flow.setNameEn(request.getNameEn());
        flow.setNodes(approvalEngine.serializeNodes(request.getNodes()));
        flow.setLastUpdateTime(new Date());
        flow.setLastUpdateBy(operator);

        flowMapper.update(flow);

        log.info("Update approval flow: id={}, operator={}", id, operator);

        return getFlowDetail(id);
    }

    /**
     * 删除审批流程
     *
     * v2.8.0变更：移除 isDefault 字段相关逻辑
     *
     * @param id 流程ID
     * @param operator 操作人
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteFlow(Long id, String operator) {
        ApprovalFlow flow = flowMapper.selectById(id);
        if (flow == null) {
            throw new BusinessException("404", "审批流程不存在", "Approval flow not found");
        }

        // ✅ v2.8.0变更：允许删除任何流程，包括 global 流程
        // 原因：通过 code 标识审批类型，删除不影响其他流程

        flowMapper.deleteById(id);

        log.info("Delete approval flow: id={}, code={}, operator={}", id, flow.getCode(), operator);
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
            response.setBusinessType(record.getBusinessType());
            response.setBusinessId(String.valueOf(record.getBusinessId()));

            // 设置业务名称（从业务数据中获取）
            Map<String, Object> businessData = getBusinessData(record.getBusinessType(), record.getBusinessId());
            if (businessData != null && businessData.get("nameCn") != null) {
                response.setBusinessName((String) businessData.get("nameCn"));
            }
            response.setApplicantId(record.getApplicantId());
            response.setApplicantName(record.getApplicantName());
            response.setStatus(record.getStatus());
            response.setCurrentNode(record.getCurrentNode());
            response.setCreateTime(record.getCreateTime());
            return response;
        })

        // 过滤审批人（如果指定了 approverId）
        .filter(response -> {
            if (request.getApproverId() == null || request.getApproverId().isEmpty()) {
                return true; // 未指定审批人，返回所有
            }


            // 获取原始记录，解析 combined_nodes 判断当前审批人
            ApprovalRecord record = records.stream()
                .filter(r -> String.valueOf(r.getId()).equals(response.getId()))
                .findFirst()
                .orElse(null);
            if (record == null || record.getCombinedNodes() == null) {
                return false;
            }
            List<ApprovalNodeDto> nodes = approvalEngine.parseNodes(record.getCombinedNodes());
            if (nodes.isEmpty() || record.getCurrentNode() >= nodes.size()) {
                return false;
            }


            // 判断当前节点的审批人是否匹配
            ApprovalNodeDto currentNode = nodes.get(record.getCurrentNode());
            return request.getApproverId().equals(currentNode.getUserId());
        })
        .collect(Collectors.toList());
    }

    /**
     * 统计待审批数量
     */
    public Long countPendingList(String type, String keyword, Integer status, String applicantId) {
        return recordMapper.countPendingList(type, keyword, status, applicantId);
    }

    /**
     * 获取审批详情
     *
     * v2.8.0核心变更：
     * - 从 combinedNodes 解析审批节点
     * - 移除 flowId 字段
     * - 审批节点显示 level 标记
     */
    public ApprovalDetailResponse getApprovalDetail(Long id) {
        ApprovalRecord record = recordMapper.selectById(id);
        if (record == null) {
            throw new BusinessException("404", "审批记录不存在: " + id, "Approval record not found: " + id);
        }

        // ✅ v2.8.0核心变更：从 combinedNodes 解析审批节点
        List<ApprovalNodeDto> nodes = approvalEngine.parseNodes(record.getCombinedNodes());
        if (nodes.isEmpty()) {
            log.warn("Failed to parse approval nodes: recordId={}", id);
        }

        // 查询审批日志
        List<ApprovalLog> logs = logMapper.selectByRecordId(id);

        // 填充节点状态和审批信息
        Map<Integer, Integer> nodeStatusMap = new HashMap<>();
        Map<Integer, String> nodeLevelMap = new HashMap<>();

        // v2.8.1新增：审批时间和意见映射
        Map<Integer, Date> nodeApproveTimeMap = new HashMap<>();
        Map<Integer, String> nodeCommentMap = new HashMap<>();

        for (ApprovalLog log : logs) {
            nodeStatusMap.put(log.getNodeIndex(), log.getAction() == 0 ? 1 : 2);
            nodeLevelMap.put(log.getNodeIndex(), log.getLevel());  // ✅ 从日志获取 level

            // v2.8.1新增：填充审批时间和意见
            nodeApproveTimeMap.put(log.getNodeIndex(), log.getCreateTime());
            nodeCommentMap.put(log.getNodeIndex(), log.getComment());
        }

        for (int i = 0; i < nodes.size(); i++) {
            ApprovalNodeDto node = nodes.get(i);

            // ✅ v2.8.0变更：节点已包含 level 信息（从 combinedNodes 解析）
            // 无需额外填充

            if (i < record.getCurrentNode()) {

                // 已处理的节点（在当前节点之前）
                node.setStatus(nodeStatusMap.getOrDefault(i, 1));

                // v2.8.1新增：填充审批时间和意见
                node.setApproveTime(nodeApproveTimeMap.get(i));
                node.setComment(nodeCommentMap.get(i));
            } else if (i == record.getCurrentNode()) {

                // 当前节点：待审批时显示0，已审批时从日志获取状态
                if (record.getStatus() == 0) {
                    node.setStatus(0);  // 待审批
                } else {

                    // 审批已通过或已拒绝，从日志获取当前节点状态
                    node.setStatus(nodeStatusMap.getOrDefault(i, 1));

                    // v2.8.1新增：填充审批时间和意见
                    node.setApproveTime(nodeApproveTimeMap.get(i));
                    node.setComment(nodeCommentMap.get(i));
                }
            } else {

                // 未处理的节点（在当前节点之后）
                node.setStatus(null);
            }
        }

        // 构建响应
        ApprovalDetailResponse response = new ApprovalDetailResponse();
        response.setId(String.valueOf(record.getId()));
        response.setBusinessType(record.getBusinessType());
        response.setBusinessId(String.valueOf(record.getBusinessId()));
        response.setBusinessData(getBusinessData(record.getBusinessType(), record.getBusinessId()));
        response.setApplicantId(record.getApplicantId());
        response.setApplicantName(record.getApplicantName());
        response.setStatus(record.getStatus());

        // ✅ v2.8.0变更：移除 flowId 字段，审批详情直接显示 combinedNodes
        response.setCombinedNodes(nodes);  // ✅ 新增：显示组合审批节点
        response.setCurrentNode(record.getCurrentNode());
        response.setNodes(nodes);  // 审批节点列表（含状态和 level）
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
        if (request.getComment() == null || request.getComment().trim().isEmpty()) {
            throw new BusinessException("400", "审批意见不能为空", "Approval comment is required");
        }

        ApprovalRecord record = approvalEngine.reject(id, operatorId, operatorName,
                request.getComment(), operator);

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
                log.error("Batch approval failed: approvalId={}", approvalIdStr, e);
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
                .message(String.format(Locale.ROOT, "批量审批完成，成功%d条，失败%d条", successCount, failedItems.size()))
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
        if (request.getComment() == null || request.getComment().trim().isEmpty()) {
            throw new BusinessException("400", "审批意见不能为空", "Approval comment is required");
        }

        int successCount = 0;
        List<BatchApprovalResponse.FailedItem> failedItems = new ArrayList<>();

        for (String approvalIdStr : request.getApprovalIds()) {
            try {
                Long approvalId = Long.parseLong(approvalIdStr);
                approvalEngine.reject(approvalId, operatorId, operatorName,
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
                log.error("Batch rejection failed: approvalId={}", approvalIdStr, e);
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
                .message(String.format(Locale.ROOT, "批量驳回完成，成功%d条，失败%d条", successCount, failedItems.size()))
                .build();
    }

    // ==================== 辅助方法 ====================

    /**
     * 获取业务数据
     */
    private Map<String, Object> getBusinessData(String businessType, Long businessId) {
        try {
            switch (businessType) {
                case "api_register":
                    return getApiRegisterData(businessId);
                case "event_register":
                    return getEventRegisterData(businessId);
                case "callback_register":
                    return getCallbackRegisterData(businessId);
                case "api_permission_apply":
                case "event_permission_apply":
                case "callback_permission_apply":
                    return getPermissionApplyData(businessId);
                default:
                    log.warn("Unknown business type: {}", businessType);
                    return new HashMap<>();
            }
        } catch (Exception e) {
            log.error("Failed to get business data: businessType={}, businessId={}", businessType, businessId, e);
            return new HashMap<>();
        }
    }

    /**
     * 获取 API 注册数据
     *
     * @param businessId 业务ID
     * @return 业务数据
     */
    private Map<String, Object> getApiRegisterData(Long businessId) {
        Map<String, Object> data = new HashMap<>();
        Api api = apiMapper.selectById(businessId);
        if (api != null) {
            data.put("nameCn", api.getNameCn());
            data.put("path", api.getPath());
            data.put("method", api.getMethod());
        }
        return data;
    }

    /**
     * 获取事件注册数据
     *
     * @param businessId 业务ID
     * @return 业务数据
     */
    private Map<String, Object> getEventRegisterData(Long businessId) {
        Map<String, Object> data = new HashMap<>();
        Event event = eventMapper.selectById(businessId);
        if (event != null) {
            data.put("nameCn", event.getNameCn());
            data.put("topic", event.getTopic());
        }
        return data;
    }

    /**
     * 获取回调注册数据
     *
     * @param businessId 业务ID
     * @return 业务数据
     */
    private Map<String, Object> getCallbackRegisterData(Long businessId) {
        Map<String, Object> data = new HashMap<>();
        Callback callback = callbackMapper.selectById(businessId);
        if (callback != null) {
            data.put("nameCn", callback.getNameCn());
        }
        return data;
    }

    /**
     * 获取权限申请数据
     *
     * @param businessId 业务ID
     * @return 业务数据
     */
    private Map<String, Object> getPermissionApplyData(Long businessId) {
        Map<String, Object> data = new HashMap<>();
        Subscription subscription = subscriptionMapper.selectById(businessId);
        if (subscription == null) {
            return data;
        }

        data.put("appId", subscription.getAppId());
        data.put("permissionId", subscription.getPermissionId());

        Permission permission = permissionMapper.selectById(subscription.getPermissionId());
        if (permission == null) {
            return data;
        }

        data.put("nameCn", permission.getNameCn());
        data.put("nameEn", permission.getNameEn());
        data.put("scope", permission.getScope());
        data.put("resourceType", permission.getResourceType());

        // 根据资源类型填充额外信息
        fillResourceData(data, permission);

        return data;
    }

    /**
     * 填充资源数据
     *
     * @param data 数据Map
     * @param permission 权限实体
     */
    private void fillResourceData(Map<String, Object> data, Permission permission) {
        String resourceType = permission.getResourceType();
        Long resourceId = permission.getResourceId();

        if ("api".equals(resourceType)) {
            Api apiResource = apiMapper.selectById(resourceId);
            if (apiResource != null) {
                data.put("path", apiResource.getPath());
                data.put("method", apiResource.getMethod());
            }
        } else if ("event".equals(resourceType)) {
            Event evt = eventMapper.selectById(resourceId);
            if (evt != null) {
                data.put("topic", evt.getTopic());
            }
        } else if ("callback".equals(resourceType)) {
            Callback cb = callbackMapper.selectById(resourceId);
            if (cb != null) {
                data.put("nameCn", cb.getNameCn());
            }
        }
    }

    /**
     * 构建日志 DTO 列表
     *
     * v2.8.0变更：新增 level 字段显示
     */
    private List<ApprovalLogDto> buildLogDtos(List<ApprovalLog> logs, List<ApprovalNodeDto> nodes) {
        return logs.stream().map(log -> {
            ApprovalLogDto dto = new ApprovalLogDto();
            dto.setOrder(log.getNodeIndex() + 1);
            dto.setLevel(log.getLevel());  // ✅ v2.8.0新增：显示审批级别
            dto.setLevelName(getLevelName(log.getLevel()));  // ✅ v2.8.0新增：审批级别名称
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
     * 获取审批级别名称
     *
     * v2.8.0新增方法
     */
    private String getLevelName(String level) {
        if (level == null) {
            return "未知";
        }
        switch (level) {
            case "resource":
                return "资源审批";
            case "scene":
                return "场景审批";
            case "global":
                return "全局审批";
            default:
                return "未知";
        }
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
