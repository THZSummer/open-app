package com.xxx.it.works.wecode.v2.modules.approval.engine;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxx.it.works.wecode.v2.common.exception.BusinessException;
import com.xxx.it.works.wecode.v2.common.id.IdGeneratorStrategy;
import com.xxx.it.works.wecode.v2.modules.approval.dto.ApprovalNodeDto;
import com.xxx.it.works.wecode.v2.modules.approvalflow.entity.ApprovalFlow;
import com.xxx.it.works.wecode.v2.modules.approval.entity.ApprovalLog;
import com.xxx.it.works.wecode.v2.modules.approval.entity.ApprovalRecord;
import com.xxx.it.works.wecode.v2.modules.approvalflow.mapper.ApprovalFlowMapper;
import com.xxx.it.works.wecode.v2.modules.approval.mapper.ApprovalLogMapper;
import com.xxx.it.works.wecode.v2.modules.approval.mapper.ApprovalRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

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
    private final IdGeneratorStrategy idGenerator;
    private final ObjectMapper objectMapper;
    private final List<ApprovalBusinessHandler> businessHandlers;

    /**
     * 审批动作枚举
     */
    public static class Action {
        public static final int APPROVE = 0;  // 同意
        public static final int REJECT = 1;   // 拒绝
        public static final int CANCEL = 2;   // 撤销
        public static final int TRANSFER = 3; // 转交
        public static final int URGE = 4;     // 催办
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
        public static final String APP_VERSION_PUBLISH = "app_version_publish";
        public static final String CONNECTOR_FLOW_VERSION_PUBLISH = "connector_flow_version_publish";

        /** 审批人可选的业务类型：未配置审批人时仍允许发起审批 */
        private static final Set<String> OPTIONAL_APPROVER_TYPES = Set.of(
                APP_VERSION_PUBLISH
        );
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
    public List<ApprovalNodeDto> composeApprovalNodes(String businessType, Long permissionId, Long appId) {
        // 版本发布等"审批人可选"业务类型：始终不组合审批节点，不受 global/scene 审批配置影响
        if (BusinessType.OPTIONAL_APPROVER_TYPES.contains(businessType)) {
            log.info("Business type [{}] allows empty approver, skip node composition (not affected by approval config)", businessType);
            return new ArrayList<>();
        }

        List<ApprovalNodeDto> combinedNodes = new ArrayList<>();
        int order = 1;

        String sceneCode = getSceneCodeByBusinessType(businessType);

        // ========== 审批优先级（范围从窄到宽） ==========
        // ⓪ 资源审批（通用槽位，默认不实现；各场景按需扩展）
        List<ApprovalNodeDto> resourceNodes = getResourceApprovalNodes(businessType, permissionId);
        for (ApprovalNodeDto node : resourceNodes) {
            node.setOrder(order++);
            node.setLevel(Level.RESOURCE);
            combinedNodes.add(node);
        }
        // ① 单应用·单场景
        if (appId != null) {
            for (ApprovalNodeDto node : loadFlowNodes(sceneCode, appId)) {
                node.setOrder(order++);
                node.setLevel(Level.SCENE);
                combinedNodes.add(node);
            }
        }
        // ② 单应用·全场景
        if (appId != null) {
            for (ApprovalNodeDto node : loadFlowNodes("global", appId)) {
                node.setOrder(order++);
                node.setLevel(Level.GLOBAL);
                combinedNodes.add(node);
            }
        }
        // ③ 全应用·单场景
        for (ApprovalNodeDto node : loadFlowNodes(sceneCode, null)) {
            node.setOrder(order++);
            node.setLevel(Level.SCENE);
            combinedNodes.add(node);
        }
        // ④ 全应用·全场景
        for (ApprovalNodeDto node : loadFlowNodes("global", null)) {
            node.setOrder(order++);
            node.setLevel(Level.GLOBAL);
            combinedNodes.add(node);
        }

        log.info("Combined approval nodes completed: businessType={}, permissionId={}, appId={}, totalNodes={}",
                businessType, permissionId, appId, combinedNodes.size());

        return combinedNodes;
    }

    /**
     * 解析资源审批节点（⓪级）。
     * 遍历已注册的 Handler，取 supports 匹配的调用 resolveResourceNodes。
     */
    private List<ApprovalNodeDto> getResourceApprovalNodes(String businessType, Long businessId) {
        for (ApprovalBusinessHandler handler : businessHandlers) {
            if (handler.supportedBusinessType().equals(businessType)) {
                return handler.resolveResourceNodes(businessId);
            }
        }
        return Collections.emptyList();
    }

    /**
     * 按 businessType 查找对应的 Handler
     */
    private ApprovalBusinessHandler findHandler(String businessType) {
        for (ApprovalBusinessHandler handler : businessHandlers) {
            if (handler.supportedBusinessType().equals(businessType)) {
                return handler;
            }
        }
        return null;
    }

    /**
     * 按 code+appId 加载审批流模板的节点列表
     */
    private List<ApprovalNodeDto> loadFlowNodes(String code, Long appId) {
        ApprovalFlow flow = flowMapper.selectByCodeAndAppId(code, appId);
        if (flow == null) return Collections.emptyList();
        return parseNodes(flow.getNodes());
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
            case BusinessType.APP_VERSION_PUBLISH:
                return "app_version_publish";
            case BusinessType.CONNECTOR_FLOW_VERSION_PUBLISH:
                return "connector_flow_version_publish";
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
                                          String applicantId, String applicantName, String operator, Long appId) {

        // 1. 组合审批节点
        List<ApprovalNodeDto> combinedNodes = composeApprovalNodes(businessType, permissionId, appId);

        // 2. 序列化审批节点为 JSON 字符串
        String combinedNodesJson = serializeNodes(combinedNodes);

        // 3. 创建审批记录
        ApprovalRecord record = new ApprovalRecord();
        record.setId(idGenerator.nextId());
        record.setCombinedNodes(combinedNodesJson);
        record.setBusinessType(businessType);
        record.setBusinessId(businessId);
        record.setApplicantId(applicantId);
        record.setApplicantName(applicantName);
        record.setCurrentNode(0);
        record.setCreateTime(new Date());
        record.setLastUpdateTime(new Date());
        record.setCreateBy(operator);
        record.setLastUpdateBy(operator);

        // 无审批人 → 免审通过
        if (combinedNodes.isEmpty()) {
            record.setStatus(Status.APPROVED);
            record.setCompletedAt(new Date());
            recordMapper.insert(record);
            dispatchApproved(record);
            log.info("No approvers configured for {}, auto-approved: recordId={}", businessType, record.getId());
            return record;
        }

        record.setStatus(Status.PENDING);
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

        // 校验：操作人必须是当前节点的审批人
        if (currentNode.getUserId() != null && !currentNode.getUserId().equals(operatorId)) {
            throw new BusinessException("403", "非当前节点审批人，无法审批",
                    "Operator is not the assigned approver for this node");
        }

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

            // 回调：各业务场景自行处理审批通过
            dispatchApproved(record);

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

        // 校验：操作人必须是当前节点的审批人
        if (currentNode.getUserId() != null && !currentNode.getUserId().equals(operatorId)) {
            throw new BusinessException("403", "非当前节点审批人，无法驳回",
                    "Operator is not the assigned approver for this node");
        }

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

        // 回调：各业务场景自行处理驳回
        dispatchRejected(record);

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

        // 写入审批操作日志
        ApprovalLog cancelLog = new ApprovalLog();
        cancelLog.setId(idGenerator.nextId());
        cancelLog.setRecordId(record.getId());
        cancelLog.setNodeIndex(record.getCurrentNode());
        List<ApprovalNodeDto> nodes = parseNodes(record.getCombinedNodes());
        if (nodes != null && record.getCurrentNode() < nodes.size()) {
            cancelLog.setLevel(nodes.get(record.getCurrentNode()).getLevel());
        }
        cancelLog.setOperatorId(operator);
        cancelLog.setOperatorName(operator);
        cancelLog.setAction(Action.CANCEL);
        cancelLog.setCreateTime(new Date());
        cancelLog.setLastUpdateTime(new Date());
        cancelLog.setCreateBy(operator);
        cancelLog.setLastUpdateBy(operator);
        logMapper.insert(cancelLog);

        // 回调：各业务场景自行处理撤回
        dispatchCancelled(record);

        log.info("Approval cancelled: recordId={}", recordId);

        return record;
    }

    // ==================== 回调分发 ====================

    private void dispatchApproved(ApprovalRecord record) {
        ApprovalBusinessHandler handler = findHandler(record.getBusinessType());
        if (handler != null) handler.onApproved(record);
    }

    private void dispatchRejected(ApprovalRecord record) {
        ApprovalBusinessHandler handler = findHandler(record.getBusinessType());
        if (handler != null) handler.onRejected(record);
    }

    private void dispatchCancelled(ApprovalRecord record) {
        ApprovalBusinessHandler handler = findHandler(record.getBusinessType());
        if (handler != null) handler.onCancelled(record);
    }

    // ==================== JSON 辅助 ====================

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

    public String serializeNodes(List<ApprovalNodeDto> nodes) {
        try {
            return objectMapper.writeValueAsString(nodes);
        } catch (Exception e) {
            log.error("Failed to serialize approval nodes config", e);
            return "[]";
        }
    }
}
