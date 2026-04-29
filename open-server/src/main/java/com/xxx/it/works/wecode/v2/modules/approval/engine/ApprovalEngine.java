package com.xxx.it.works.wecode.v2.modules.approval.engine;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxx.it.works.wecode.v2.common.exception.BusinessException;
import com.xxx.it.works.wecode.v2.common.id.IdGeneratorStrategy;
import com.xxx.it.works.wecode.v2.modules.approval.dto.ApprovalNodeDto;
import com.xxx.it.works.wecode.v2.modules.approval.entity.ApprovalFlow;
import com.xxx.it.works.wecode.v2.modules.approval.entity.ApprovalLog;
import com.xxx.it.works.wecode.v2.modules.approval.entity.ApprovalRecord;
import com.xxx.it.works.wecode.v2.modules.approval.mapper.ApprovalFlowMapper;
import com.xxx.it.works.wecode.v2.modules.approval.mapper.ApprovalLogMapper;
import com.xxx.it.works.wecode.v2.modules.approval.mapper.ApprovalRecordMapper;
import com.xxx.it.works.wecode.v2.modules.event.entity.Permission;  // ✅ Permission 在 event 模块
import com.xxx.it.works.wecode.v2.modules.event.mapper.PermissionMapper;  // ✅ PermissionMapper 在 event 模块
import com.xxx.it.works.wecode.v2.modules.permission.entity.Subscription;
import com.xxx.it.works.wecode.v2.modules.permission.mapper.SubscriptionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.xxx.it.works.wecode.v2.modules.api.entity.Api;
import com.xxx.it.works.wecode.v2.modules.callback.entity.Callback;
import com.xxx.it.works.wecode.v2.modules.event.entity.Event;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * 审批引擎
 *
 * <p>v2.8.0 重写版本</p>
 *
 * <p>负责处理审批流程的核心逻辑：</p>
 * <ul>
 *   <li>组合三级审批节点（资源审批 → 场景审批 → 全局审批）</li>
 *   <li>创建审批记录（存储 combinedNodes）</li>
 *   <li>执行审批操作（同意/驳回/撤销）</li>
 *   <li>更新订阅状态</li>
 *   <li>记录审批日志（含 level 字段）</li>
 * </ul>
 *
 * <p>核心设计变更（v2.8.0）：</p>
 * <ul>
 *   <li>移除 flowId 字段，审批记录直接存储 combinedNodes</li>
 *   <li>移除 isDefault 字段，用 code='global' 标识全局审批</li>
 *   <li>三级审批是"串联组合"而非"选择执行"</li>
 *   <li>审批顺序：资源审批 → 场景审批 → 全局审批（从具体到一般）</li>
 * </ul>
 *
 * @author SDDU Build Agent
 * @version 2.8.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApprovalEngine {

    private final ApprovalFlowMapper flowMapper;
    private final ApprovalRecordMapper recordMapper;
    private final ApprovalLogMapper logMapper;
    private final SubscriptionMapper subscriptionMapper;
    private final PermissionMapper permissionMapper;
    private final IdGeneratorStrategy idGenerator;
    private final ObjectMapper objectMapper;

    // 资源 Mapper
    private final com.xxx.it.works.wecode.v2.modules.api.mapper.ApiMapper apiMapper;
    private final com.xxx.it.works.wecode.v2.modules.event.mapper.EventMapper eventMapper;
    private final com.xxx.it.works.wecode.v2.modules.callback.mapper.CallbackMapper callbackMapper;

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
        public static final String API_PERMISSION_APPLY = "api_permission_apply";
        public static final String EVENT_PERMISSION_APPLY = "event_permission_apply";
        public static final String CALLBACK_PERMISSION_APPLY = "callback_permission_apply";
    }

    /**
     * 审批级别枚举（v2.8.0新增）
     */
    public static class Level {
        public static final String RESOURCE = "resource";  // 资源审批
        public static final String SCENE = "scene";        // 场景审批
        public static final String GLOBAL = "global";      // 全局审批
    }

    // ==================== 核心组合逻辑（v2.8.0重写） ====================

    /**
     * 组合审批节点
     *
     * v2.8.0核心方法：根据业务类型组合审批节点
     *
     * 审批策略：
     * 1. 资源注册审批（*_register）：两级审批
     *    - 场景审批（level='scene')
     *    - 全局审批（level='global')
     *    - ❌ 不包含资源审批节点
     *
     * 2. 权限申请审批（*_permission_apply）：三级审批
     *    - 资源审批（level='resource') - 仅当 need_approval=1 时
     *    - 场景审批（level='scene')
     *    - 全局审批（level='global')
     *
     * @param businessType 业务类型（用于确定场景审批编码和审批级别）
     * @param permissionId 权限ID（用于获取资源审批节点，权限申请时使用）
     * @return 组合后的审批节点列表
     */
    public List<ApprovalNodeDto> composeApprovalNodes(String businessType, Long permissionId) {
        List<ApprovalNodeDto> combinedNodes = new ArrayList<>();
        int order = 1;

        // ✅ 根据 businessType 判断审批级别
        boolean isRegisterApproval = businessType.endsWith("_register");
        boolean isPermissionApply = businessType.endsWith("_permission_apply");

        if (isRegisterApproval) {

            // ==================== 资源注册审批：两级审批 ====================
            log.info("Resource registration approval: businessType={}, two-level approval (scene+global)", businessType);

            // 第一级：场景审批节点
            List<ApprovalNodeDto> sceneNodes = getSceneApprovalNodes(businessType);
            for (ApprovalNodeDto node : sceneNodes) {
                node.setOrder(order++);
                node.setLevel(Level.SCENE);
                combinedNodes.add(node);
            }
            log.debug("Scene approval nodes count: {}", sceneNodes.size());

            // 第二级：全局审批节点
            List<ApprovalNodeDto> globalNodes = getGlobalApprovalNodes();
            for (ApprovalNodeDto node : globalNodes) {
                node.setOrder(order++);
                node.setLevel(Level.GLOBAL);
                combinedNodes.add(node);
            }
            log.debug("Global approval nodes count: {}", globalNodes.size());

        } else if (isPermissionApply) {

            // ==================== 权限申请审批：三级审批 ====================
            log.info("Permission application approval: businessType={}, three-level approval (resource+scene+global)", businessType);

            // 第一级：资源审批节点（从 permission_t.resource_nodes 读取）
            List<ApprovalNodeDto> resourceNodes = getResourceApprovalNodes(permissionId);
            for (ApprovalNodeDto node : resourceNodes) {
                node.setOrder(order++);
                node.setLevel(Level.RESOURCE);
                combinedNodes.add(node);
            }
            log.debug("Resource approval nodes count: {}", resourceNodes.size());

            // 第二级：场景审批节点
            List<ApprovalNodeDto> sceneNodes = getSceneApprovalNodes(businessType);
            for (ApprovalNodeDto node : sceneNodes) {
                node.setOrder(order++);
                node.setLevel(Level.SCENE);
                combinedNodes.add(node);
            }
            log.debug("Scene approval nodes count: {}", sceneNodes.size());

            // 第三级：全局审批节点
            List<ApprovalNodeDto> globalNodes = getGlobalApprovalNodes();
            for (ApprovalNodeDto node : globalNodes) {
                node.setOrder(order++);
                node.setLevel(Level.GLOBAL);
                combinedNodes.add(node);
            }
            log.debug("Global approval nodes count: {}", globalNodes.size());

        } else {
            log.warn("Unknown business type: {}, using default two-level approval (scene+global)", businessType);

            // 默认：场景审批 + 全局审批
            List<ApprovalNodeDto> sceneNodes = getSceneApprovalNodes(businessType);
            for (ApprovalNodeDto node : sceneNodes) {
                node.setOrder(order++);
                node.setLevel(Level.SCENE);
                combinedNodes.add(node);
            }

            List<ApprovalNodeDto> globalNodes = getGlobalApprovalNodes();
            for (ApprovalNodeDto node : globalNodes) {
                node.setOrder(order++);
                node.setLevel(Level.GLOBAL);
                combinedNodes.add(node);
            }
        }

        log.info("Combined approval nodes completed: businessType={}, permissionId={}, totalNodes={}, approvalLevel={}",
                businessType, permissionId, combinedNodes.size(),
                isRegisterApproval ? "two-level (scene+global)" : "three-level (resource+scene+global)");

        return combinedNodes;
    }

    /**
     * 从权限表读取资源审批节点
     *
     * v2.8.0变更：直接从 permission_t.resource_nodes 读取，无需查询审批流程表
     *
     * @param permissionId 权限ID
     * @return 资源审批节点列表
     */
    private List<ApprovalNodeDto> getResourceApprovalNodes(Long permissionId) {
        if (permissionId == null) {
            return Collections.emptyList();
        }

        Permission permission = permissionMapper.selectById(permissionId);
        if (permission == null) {
            log.warn("Permission not found: permissionId={}", permissionId);
            return Collections.emptyList();
        }

        // 检查是否需要审批
        if (permission.getNeedApproval() == null || permission.getNeedApproval() != 1) {
            log.debug("Permission does not require approval: permissionId={}", permissionId);
            return Collections.emptyList();
        }

        // 从 resource_nodes 字段解析审批节点
        String resourceNodesJson = permission.getResourceNodes();
        if (resourceNodesJson == null || resourceNodesJson.trim().isEmpty() || "[]".equals(resourceNodesJson.trim())) {
            log.debug("Permission approval nodes config is empty: permissionId={}", permissionId);
            return Collections.emptyList();
        }

        List<ApprovalNodeDto> nodes = parseNodes(resourceNodesJson);
        if (nodes == null || nodes.isEmpty()) {
            log.debug("Permission approval nodes parse result is empty: permissionId={}", permissionId);
            return Collections.emptyList();
        }

        return nodes;
    }

    /**
     * 从审批流程表读取场景审批节点
     *
     * v2.8.0变更：根据业务类型确定场景审批编码
     *
     * @param businessType 业务类型
     * @return 场景审批节点列表
     */
    private List<ApprovalNodeDto> getSceneApprovalNodes(String businessType) {

        // 根据业务类型确定场景审批编码
        String sceneCode = getSceneCodeByBusinessType(businessType);

        ApprovalFlow sceneFlow = flowMapper.selectByCode(sceneCode);
        if (sceneFlow == null) {
            log.warn("Scene approval flow not found: code={}", sceneCode);
            return Collections.emptyList();
        }

        String nodesJson = sceneFlow.getNodes();
        if (nodesJson == null || nodesJson.trim().isEmpty() || "[]".equals(nodesJson.trim())) {
            log.debug("Scene approval nodes config is empty: code={}", sceneCode);
            return Collections.emptyList();
        }

        List<ApprovalNodeDto> nodes = parseNodes(nodesJson);
        if (nodes == null || nodes.isEmpty()) {
            log.debug("Scene approval nodes parse result is empty: code={}", sceneCode);
            return Collections.emptyList();
        }

        return nodes;
    }

    /**
     * 从审批流程表读取全局审批节点
     *
     * v2.8.0变更：直接查询 code='global' 的审批流程
     *
     * @return 全局审批节点列表
     */
    private List<ApprovalNodeDto> getGlobalApprovalNodes() {
        ApprovalFlow globalFlow = flowMapper.selectByCode("global");
        if (globalFlow == null) {
            log.warn("Global approval flow not found: code=global");
            return Collections.emptyList();
        }

        String nodesJson = globalFlow.getNodes();
        if (nodesJson == null || nodesJson.trim().isEmpty() || "[]".equals(nodesJson.trim())) {
            log.debug("Global approval nodes config is empty: code=global");
            return Collections.emptyList();
        }

        List<ApprovalNodeDto> nodes = parseNodes(nodesJson);
        if (nodes == null || nodes.isEmpty()) {
            log.debug("Global approval nodes parse result is empty: code=global");
            return Collections.emptyList();
        }

        return nodes;
    }

    /**
     * 根据业务类型获取场景审批编码
     *
     * v2.8.0新增方法：根据业务类型确定场景审批流程编码
     *
     * @param businessType 业务类型
     * @return 场景审批编码
     */
    private String getSceneCodeByBusinessType(String businessType) {
        switch (businessType) {
            case BusinessType.API_REGISTER:
                return "api_register";
            case BusinessType.EVENT_REGISTER:
                return "event_register";
            case BusinessType.CALLBACK_REGISTER:
                return "callback_register";
            case BusinessType.API_PERMISSION_APPLY:
                return "api_permission_apply";
            case BusinessType.EVENT_PERMISSION_APPLY:
                return "event_permission_apply";
            case BusinessType.CALLBACK_PERMISSION_APPLY:
                return "callback_permission_apply";
            default:
                log.warn("Unknown business type: {}, using default scene code", businessType);
                return "api_permission_apply";  // 默认使用API权限申请审批
        }
    }

    // ==================== 创建审批记录（v2.8.0重写） ====================

    /**
     * 创建审批记录
     *
     * v2.8.0核心变更：
     * - 移除 flowId 参数
     * - 直接存储 combinedNodes（组合后的完整审批节点）
     * - 审批记录数据完全独立，不受审批流程模板修改影响
     *
     * @param businessType 业务类型
     * @param permissionId 权限ID（用于获取资源审批节点）
     * @param businessId 业务对象ID（订阅记录ID或资源ID）
     * @param applicantId 申请人ID
     * @param applicantName 申请人名称
     * @param operator 操作人
     * @return 审批记录
     */
    @Transactional(rollbackFor = Exception.class)
    public ApprovalRecord createApproval(String businessType, Long permissionId, Long businessId,
                                          String applicantId, String applicantName, String operator) {

        // 1. 组合三级审批节点
        List<ApprovalNodeDto> combinedNodes = composeApprovalNodes(businessType, permissionId);

        if (combinedNodes.isEmpty()) {
            throw new BusinessException("400", "审批节点配置为空，无法创建审批记录",
                    "Approval nodes configuration is empty, cannot create approval record");
        }

        // 2. 序列化审批节点为 JSON 字符串
        String combinedNodesJson = serializeNodes(combinedNodes);

        // 3. 创建审批记录
        ApprovalRecord record = new ApprovalRecord();
        record.setId(idGenerator.nextId());
        record.setCombinedNodes(combinedNodesJson);  // ✅ 直接存储组合节点
        record.setBusinessType(businessType);
        record.setBusinessId(businessId);
        record.setApplicantId(applicantId);
        record.setApplicantName(applicantName);
        record.setStatus(Status.PENDING);
        record.setCurrentNode(0);  // 当前节点索引（从第一个节点开始）
        record.setCreateTime(new Date());
        record.setLastUpdateTime(new Date());
        record.setCreateBy(operator);
        record.setLastUpdateBy(operator);

        recordMapper.insert(record);

        log.info("Created approval record: id={}, businessType={}, businessId={}, nodesCount={}, applicant={}",
                record.getId(), businessType, businessId, combinedNodes.size(), applicantId);

        return record;
    }

    // ==================== 审批执行逻辑（v2.8.0重写） ====================

    /**
     * 同意审批
     *
     * v2.8.0变更：
     * - 从 combinedNodes 解析审批节点
     * - 审批日志记录 level 字段
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

        // 1. 查询审批记录
        ApprovalRecord record = recordMapper.selectById(recordId);
        if (record == null) {
            throw new BusinessException("404", "审批记录不存在: " + recordId,
                    "Approval record not found: " + recordId);
        }

        // 2. 检查状态
        if (record.getStatus() != Status.PENDING) {
            throw new BusinessException("400", "审批记录状态不正确，无法同意",
                    "Approval record status is incorrect, cannot approve");
        }

        // 3. ✅ 从 combinedNodes 解析审批节点（v2.8.0核心变更）
        List<ApprovalNodeDto> combinedNodes = parseNodes(record.getCombinedNodes());
        if (combinedNodes.isEmpty()) {
            throw new BusinessException("400", "审批节点配置为空",
                    "Approval nodes configuration is empty");
        }

        // 4. 检查当前节点
        int currentNodeIndex = record.getCurrentNode();
        if (currentNodeIndex >= combinedNodes.size()) {
            throw new BusinessException("400", "当前节点索引超出范围",
                    "Current node index out of range");
        }

        // 5. 获取当前节点信息
        ApprovalNodeDto currentNode = combinedNodes.get(currentNodeIndex);

        // 6. ✅ 记录审批日志（含 level 字段）
        ApprovalLog approvalLog = new ApprovalLog();
        approvalLog.setId(idGenerator.nextId());
        approvalLog.setRecordId(recordId);
        approvalLog.setNodeIndex(currentNodeIndex);
        approvalLog.setLevel(currentNode.getLevel());  // ✅ v2.8.0新增：记录审批级别
        approvalLog.setOperatorId(operatorId);
        approvalLog.setOperatorName(operatorName);
        approvalLog.setAction(Action.APPROVE);
        approvalLog.setComment(comment);
        approvalLog.setCreateTime(new Date());
        approvalLog.setLastUpdateTime(new Date());
        approvalLog.setCreateBy(operator);
        approvalLog.setLastUpdateBy(operator);

        logMapper.insert(approvalLog);

        // 7. 判断是否所有节点都已审批通过
        if (currentNodeIndex >= combinedNodes.size() - 1) {

            // 最后一个节点，审批通过
            record.setStatus(Status.APPROVED);
            record.setCompletedAt(new Date());
            record.setLastUpdateTime(new Date());
            record.setLastUpdateBy(operator);

            recordMapper.update(record);

            // 更新资源状态（资源注册场景）
            updateResourceStatus(record, Status.APPROVED);

            // 更新订阅状态（权限申请场景）
            updateSubscriptionStatus(record, Status.APPROVED);

            log.info("Approval approved: recordId={}, operator={}, level={}",
                    recordId, operatorId, currentNode.getLevel());
        } else {

            // 进入下一个审批节点
            record.setCurrentNode(currentNodeIndex + 1);
            record.setLastUpdateTime(new Date());
            record.setLastUpdateBy(operator);

            recordMapper.update(record);

            log.info("Approval node passed, entering next node: recordId={}, currentNode={}, level={}",
                    recordId, record.getCurrentNode(), currentNode.getLevel());
        }

        return record;
    }

    /**
     * 驳回审批
     *
     * v2.8.0变更：
     * - 从 combinedNodes 解析审批节点
     * - 审批日志记录 level 字段
     *
     * @param recordId 审批记录ID
     * @param operatorId 操作人ID
     * @param operatorName 操作人名称
     * @param comment 审批意见（驳回时填写）
     * @param operator 操作人（用于审计字段）
     * @return 审批记录
     */
    @Transactional(rollbackFor = Exception.class)
    public ApprovalRecord reject(Long recordId, String operatorId, String operatorName,
                                  String comment, String operator) {

        // 1. 查询审批记录
        ApprovalRecord record = recordMapper.selectById(recordId);
        if (record == null) {
            throw new BusinessException("404", "审批记录不存在: " + recordId,
                    "Approval record not found: " + recordId);
        }

        // 2. 检查状态
        if (record.getStatus() != Status.PENDING) {
            throw new BusinessException("400", "审批记录状态不正确，无法驳回",
                    "Approval record status is incorrect, cannot reject");
        }

        // 3. ✅ 从 combinedNodes 解析审批节点（v2.8.0核心变更）
        List<ApprovalNodeDto> combinedNodes = parseNodes(record.getCombinedNodes());
        ApprovalNodeDto currentNode = combinedNodes.get(record.getCurrentNode());

        // 4. ✅ 记录审批日志（含 level 字段）
        ApprovalLog approvalLog = new ApprovalLog();
        approvalLog.setId(idGenerator.nextId());
        approvalLog.setRecordId(recordId);
        approvalLog.setNodeIndex(record.getCurrentNode());
        approvalLog.setLevel(currentNode.getLevel());  // ✅ v2.8.0新增：记录审批级别
        approvalLog.setOperatorId(operatorId);
        approvalLog.setOperatorName(operatorName);
        approvalLog.setAction(Action.REJECT);
        approvalLog.setComment(comment);
        approvalLog.setCreateTime(new Date());
        approvalLog.setLastUpdateTime(new Date());
        approvalLog.setCreateBy(operator);
        approvalLog.setLastUpdateBy(operator);

        logMapper.insert(approvalLog);

        // 5. 更新审批状态
        record.setStatus(Status.REJECTED);
        record.setCompletedAt(new Date());
        record.setLastUpdateTime(new Date());
        record.setLastUpdateBy(operator);

        recordMapper.update(record);

        // 更新资源状态（资源注册场景）
        updateResourceStatus(record, Status.REJECTED);

        // 更新订阅状态（权限申请场景）
        updateSubscriptionStatus(record, Status.REJECTED);

        log.info("Approval rejected: recordId={}, operator={}, comment={}, level={}",
                recordId, operatorId, comment, currentNode.getLevel());

        return record;
    }

    /**
     * 撤销审批
     *
     * v2.8.0变更：适配 combinedNodes 解析
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
            throw new BusinessException("404", "审批记录不存在: " + recordId,
                    "Approval record not found: " + recordId);
        }

        // 检查状态
        if (record.getStatus() != Status.PENDING) {
            throw new BusinessException("400", "审批记录状态不正确，无法撤销",
                    "Approval record status is incorrect, cannot cancel");
        }

        // 更新审批状态
        record.setStatus(Status.CANCELLED);
        record.setCompletedAt(new Date());
        record.setLastUpdateTime(new Date());
        record.setLastUpdateBy(operator);

        recordMapper.update(record);

        // 更新资源状态（资源注册场景）
        updateResourceStatus(record, Status.CANCELLED);

        // 更新订阅状态（权限申请场景）
        updateSubscriptionStatus(record, Status.CANCELLED);

        log.info("Approval cancelled: recordId={}", recordId);

        return record;
    }

    // ==================== 辅助方法 ====================

    /**
     * 更新资源状态（资源注册场景）
     *
     * @param record 审批记录
     * @param status 审批状态
     */
    private void updateResourceStatus(ApprovalRecord record, int status) {
        String businessType = record.getBusinessType();
        Long businessId = record.getBusinessId();

        // 审批通过，资源状态改为已发布(2)；审批拒绝/撤销，资源状态改为草稿(0)
        int resourceStatus = (status == Status.APPROVED) ? 2 : 0;

        try {
            switch (businessType) {
                case BusinessType.API_REGISTER:
                    Api api = apiMapper.selectById(businessId);
                    if (api != null) {
                        api.setStatus(resourceStatus);
                        api.setLastUpdateTime(new Date());
                        api.setLastUpdateBy(record.getApplicantId());
                        apiMapper.update(api);
                        log.info("Updated API status: apiId={}, status={}", businessId, resourceStatus);
                    }
                    break;

                case BusinessType.EVENT_REGISTER:
                    Event event = eventMapper.selectById(businessId);
                    if (event != null) {
                        event.setStatus(resourceStatus);
                        event.setLastUpdateTime(new Date());
                        event.setLastUpdateBy(record.getApplicantId());
                        eventMapper.update(event);
                        log.info("Updated event status: eventId={}, status={}", businessId, resourceStatus);
                    }
                    break;

                case BusinessType.CALLBACK_REGISTER:
                    Callback callback = callbackMapper.selectById(businessId);
                    if (callback != null) {
                        callback.setStatus(resourceStatus);
                        callback.setLastUpdateTime(new Date());
                        callback.setLastUpdateBy(record.getApplicantId());
                        callbackMapper.update(callback);
                        log.info("Updated callback status: callbackId={}, status={}", businessId, resourceStatus);
                    }
                    break;

                case BusinessType.API_PERMISSION_APPLY:
                case BusinessType.EVENT_PERMISSION_APPLY:
                case BusinessType.CALLBACK_PERMISSION_APPLY:

                    // 权限申请场景，由 updateSubscriptionStatus 处理
                    log.debug("Permission application scenario, not updating resource status");
                    break;

                default:
                    log.warn("Unknown business type: {}", businessType);
            }
        } catch (Exception e) {
            log.error("Failed to update resource status: businessType={}, businessId={}", businessType, businessId, e);
        }
    }

    /**
     * 更新订阅状态
     *
     * @param record 审批记录
     * @param status 审批状态
     */
    private void updateSubscriptionStatus(ApprovalRecord record, int status) {

        // 仅处理权限申请类型的审批
        if (!BusinessType.API_PERMISSION_APPLY.equals(record.getBusinessType()) &&
            !BusinessType.EVENT_PERMISSION_APPLY.equals(record.getBusinessType()) &&
            !BusinessType.CALLBACK_PERMISSION_APPLY.equals(record.getBusinessType())) {
            return;
        }

        // 查询订阅记录
        Subscription subscription = subscriptionMapper.selectById(record.getBusinessId());
        if (subscription == null) {
            log.warn("Subscription record not found, cannot update status: businessId={}", record.getBusinessId());
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

        log.info("Updated subscription status: subscriptionId={}, status={}", subscription.getId(), subscriptionStatus);
    }

    /**
     * 解析审批节点配置
     *
     * v2.8.0变更：适配 ApprovalNodeDto 的 level 字段
     *
     * @param nodesJson 节点 JSON 字符串
     * @return 节点列表
     */
    public List<ApprovalNodeDto> parseNodes(String nodesJson) {
        if (nodesJson == null || nodesJson.trim().isEmpty()) {
            return new ArrayList<>();
        }

        try {
            List<ApprovalNodeDto> nodes = objectMapper.readValue(nodesJson,
                    new TypeReference<List<ApprovalNodeDto>>() {});
            return nodes != null ? nodes : new ArrayList<>();
        } catch (Exception e) {
            log.error("Failed to parse approval nodes config: {}", nodesJson, e);
            return new ArrayList<>();
        }
    }

    /**
     * 序列化审批节点配置
     *
     * v2.8.0变更：适配 ApprovalNodeDto 的 level 字段
     *
     * @param nodes 节点列表
     * @return JSON 字符串
     */
    public String serializeNodes(List<ApprovalNodeDto> nodes) {
        try {
            return objectMapper.writeValueAsString(nodes);
        } catch (Exception e) {
            log.error("Failed to serialize approval nodes config", e);
            return "[]";
        }
    }
}
