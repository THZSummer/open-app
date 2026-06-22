package com.xxx.it.works.wecode.v2.modules.approval;

import com.xxx.it.works.wecode.v2.common.enums.ConnectorPlatformConstants;
import com.xxx.it.works.wecode.v2.common.enums.FlowVersionStatus;
import com.xxx.it.works.wecode.v2.common.exception.BusinessException;
import com.xxx.it.works.wecode.v2.common.id.IdGeneratorStrategy;
import com.xxx.it.works.wecode.v2.modules.approval.dto.ApprovalNodeDto;
import com.xxx.it.works.wecode.v2.modules.approval.engine.ApprovalEngine;
import com.xxx.it.works.wecode.v2.modules.approval.entity.ApprovalFlow;
import com.xxx.it.works.wecode.v2.modules.approval.entity.ApprovalRecord;
import com.xxx.it.works.wecode.v2.modules.approval.mapper.ApprovalFlowMapper;
import com.xxx.it.works.wecode.v2.modules.approval.mapper.ApprovalRecordMapper;
import com.xxx.it.works.wecode.v2.modules.approval.service.ApprovalNotifyService;
import com.xxx.it.works.wecode.v2.modules.flow.entity.FlowVersion;
import com.xxx.it.works.wecode.v2.modules.flow.mapper.OpFlowVersionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * 连接流版本发布审批服务
 *
 * <p>负责连接流版本发布的三级审批集成（FR-031~FR-033）：
 * 提交审批、取消审批、催办审批。</p>
 *
 * <p>三级审批人查找策略（优先级从高到低）：</p>
 * <ul>
 *   <li>应用级审批人：code="connector_flow_version_publish" + app_id = 目标appId</li>
 *   <li>平台连接流级审批人：code="connector_flow_version_publish" + app_id IS NULL</li>
 *   <li>全局审批人：code="global" + app_id IS NULL</li>
 * </ul>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FlowVersionApprovalService {

    private final ApprovalFlowMapper flowMapper;
    private final ApprovalRecordMapper recordMapper;
    private final OpFlowVersionMapper flowVersionMapper;
    private final ApprovalEngine approvalEngine;
    private final ApprovalNotifyService approvalNotifyService;
    private final IdGeneratorStrategy idGenerator;

    /**
     * 提交审批
     *
     * <p>发布连接流版本时自动调用，创建审批实例并更新FlowVersion状态为待审批。
     * 按三级审批人查找策略组合审批节点。</p>
     *
     * @param flowVersionId 流版本ID
     * @param flowId        连接流ID
     * @param flowNameCn    连接流中文名称
     * @param flowNameEn    连接流英文名称
     * @param appId         归属应用ID
     * @param applicantId   申请人ID
     * @param applicantName 申请人名称
     * @return 审批记录
     */
    @Transactional(rollbackFor = Exception.class)
    public ApprovalRecord submitApproval(Long flowVersionId, Long flowId, String flowNameCn,
                                          String flowNameEn, Long appId,
                                          String applicantId, String applicantName) {

        // 1. 校验版本是否存在且为草稿状态
        FlowVersion version = flowVersionMapper.selectById(flowVersionId);
        if (version == null) {
            throw BusinessException.notFound("版本不存在: " + flowVersionId,
                    "FlowVersion not found: " + flowVersionId);
        }
        if (!FlowVersionStatus.DRAFT.getCode().equals(version.getStatus())) {
            throw BusinessException.badRequest("仅草稿状态的版本可提交审批",
                    "Only draft versions can be submitted for approval");
        }

        // 2. 组合三级审批节点
        List<ApprovalNodeDto> combinedNodes = composeApprovalNodes(appId);
        if (combinedNodes.isEmpty()) {
            throw BusinessException.badRequest("审批节点配置为空，无法创建审批",
                    "Approval nodes configuration is empty, cannot create approval");
        }

        // 3. 序列化审批节点
        String combinedNodesJson = approvalEngine.serializeNodes(combinedNodes);

        // 4. 创建审批记录
        ApprovalRecord record = new ApprovalRecord();
        record.setId(idGenerator.nextId());
        record.setCombinedNodes(combinedNodesJson);
        record.setBusinessType(ConnectorPlatformConstants.APPROVAL_BUSINESS_TYPE_FLOW_VERSION_PUBLISH);
        record.setBusinessId(flowVersionId);
        record.setApplicantId(applicantId);
        record.setApplicantName(applicantName);
        record.setStatus(ApprovalEngine.Status.PENDING);
        record.setCurrentNode(0);
        record.setCreateTime(new Date());
        record.setLastUpdateTime(new Date());
        record.setCreateBy(applicantId);
        record.setLastUpdateBy(applicantId);

        recordMapper.insert(record);

        // 5. 更新FlowVersion状态为待审批
        version.setStatus(FlowVersionStatus.PENDING_APPROVAL.getCode());
        version.setLastUpdateTime(new Date());
        version.setLastUpdateBy(applicantId);
        flowVersionMapper.update(version);

        log.info("Flow version approval submitted: recordId={}, flowVersionId={}, flowId={}, flowName={}, " +
                 "appId={}, nodesCount={}",
                 record.getId(), flowVersionId, flowId, flowNameCn, appId, combinedNodes.size());

        return record;
    }

    /**
     * 取消审批（撤回）
     *
     * <p>将待审批的版本撤回，更新FlowVersion状态为已撤回。</p>
     *
     * @param flowVersionId 流版本ID
     * @param appId         归属应用ID
     * @param operator      操作人
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancelApproval(Long flowVersionId, Long appId, String operator) {

        // 1. 查询版本并校验状态
        FlowVersion version = flowVersionMapper.selectById(flowVersionId);
        if (version == null) {
            throw BusinessException.notFound("版本不存在: " + flowVersionId,
                    "FlowVersion not found: " + flowVersionId);
        }
        if (!FlowVersionStatus.PENDING_APPROVAL.getCode().equals(version.getStatus())) {
            throw BusinessException.badRequest("仅待审批状态的版本可撤回",
                    "Only pending approval versions can be withdrawn");
        }

        // 2. 查询审批记录
        ApprovalRecord record = recordMapper.selectByBusiness(
                ConnectorPlatformConstants.APPROVAL_BUSINESS_TYPE_FLOW_VERSION_PUBLISH, flowVersionId);
        if (record == null || record.getStatus() != ApprovalEngine.Status.PENDING) {
            throw BusinessException.badRequest("未找到待审批的审批记录",
                    "No pending approval record found");
        }

        // 3. 取消审批
        approvalEngine.cancel(record.getId(), operator);

        // 4. 更新FlowVersion状态为已撤回
        version.setStatus(FlowVersionStatus.WITHDRAWN.getCode());
        version.setLastUpdateTime(new Date());
        version.setLastUpdateBy(operator);
        flowVersionMapper.update(version);

        log.info("Flow version approval cancelled: flowVersionId={}, appId={}, operator={}",
                 flowVersionId, appId, operator);
    }

    /**
     * 催办审批
     *
     * <p>向当前审批级别的审批人发送催办通知。</p>
     *
     * @param flowVersionId 流版本ID
     * @param appId         归属应用ID
     * @param operator      操作人
     */
    @Transactional(rollbackFor = Exception.class)
    public void urgeApproval(Long flowVersionId, Long appId, String operator) {

        // 1. 查找待审批的审批记录
        ApprovalRecord record = recordMapper.selectLatestPendingByBusiness(
                ConnectorPlatformConstants.APPROVAL_BUSINESS_TYPE_FLOW_VERSION_PUBLISH, flowVersionId);
        if (record == null) {
            throw BusinessException.badRequest("未找到待审批的审批记录",
                    "No pending approval record found");
        }

        // 2. 校验申请人身份
        if (!record.getApplicantId().equals(operator)) {
            throw BusinessException.forbidden("只有申请人可以催办",
                    "Only the applicant can urge the approval");
        }

        // 3. 解析当前审批节点
        List<ApprovalNodeDto> nodes = approvalEngine.parseNodes(record.getCombinedNodes());
        if (nodes.isEmpty() || record.getCurrentNode() >= nodes.size()) {
            throw BusinessException.badRequest("审批节点配置异常",
                    "Approval node configuration is invalid");
        }

        ApprovalNodeDto currentNode = nodes.get(record.getCurrentNode());

        // 4. 发送催办通知
        String cardId = approvalNotifyService.sendUrgeCard(
                currentNode.getUserId(),
                currentNode.getUserName(),
                record.getId(),
                record.getBusinessType(),
                record.getBusinessId(),
                record.getApplicantName()
        );

        // 5. 持久化cardId到combinedNodes
        if (currentNode.getCardIds() == null) {
            currentNode.setCardIds(new ArrayList<>());
        }
        currentNode.getCardIds().add(cardId);
        record.setCombinedNodes(approvalEngine.serializeNodes(nodes));
        record.setLastUpdateTime(new Date());
        recordMapper.updateCombinedNodes(record);

        log.info("Flow version approval urged: recordId={}, flowVersionId={}, appId={}, " +
                 "approver={}, cardId={}",
                 record.getId(), flowVersionId, appId, currentNode.getUserId(), cardId);
    }

    // ==================== 私有方法 ====================

    /**
     * 组合三级审批节点
     *
     * <p>按优先级查找审批流程模板，组合为三级审批节点：
     * 应用级 → 平台级 → 全局级。使用首次匹配策略。</p>
     *
     * @param appId 目标应用ID
     * @return 组合后的审批节点列表
     */
    private List<ApprovalNodeDto> composeApprovalNodes(Long appId) {
        List<ApprovalNodeDto> combinedNodes = new ArrayList<>();
        int order = 1;
        String code = ConnectorPlatformConstants.APPROVAL_BUSINESS_TYPE_FLOW_VERSION_PUBLISH;
        String flowPubCode = "connector_flow_version_publish";

        // 第一级：应用级审批人（code + appId 精确匹配）
        if (appId != null) {
            List<ApprovalNodeDto> appNodes = getApprovalNodesByCodeAndAppId(flowPubCode, appId);
            if (!appNodes.isEmpty()) {
                for (ApprovalNodeDto node : appNodes) {
                    node.setOrder(order++);
                    node.setLevel("app");
                    combinedNodes.add(node);
                }
                log.debug("App-level approval nodes found: count={}, appId={}", appNodes.size(), appId);
            }
        }

        // 第二级：平台连接流级审批人（code匹配，appId为NULL）
        List<ApprovalNodeDto> platformNodes = getApprovalNodesByCodeAndAppId(flowPubCode, null);
        if (!platformNodes.isEmpty()) {
            for (ApprovalNodeDto node : platformNodes) {
                node.setOrder(order++);
                node.setLevel("platform");
                combinedNodes.add(node);
            }
            log.debug("Platform-level approval nodes found: count={}", platformNodes.size());
        }

        // 第三级：全局审批人（code="global"，appId为NULL）
        List<ApprovalNodeDto> globalNodes = getApprovalNodesByCodeAndAppId("global", null);
        if (!globalNodes.isEmpty()) {
            for (ApprovalNodeDto node : globalNodes) {
                node.setOrder(order++);
                node.setLevel("global");
                combinedNodes.add(node);
            }
            log.debug("Global-level approval nodes found: count={}", globalNodes.size());
        }

        log.info("Combined approval nodes: appId={}, totalNodes={}, levels=app→platform→global",
                 appId, combinedNodes.size());

        return combinedNodes;
    }

    /**
     * 根据code和appId查询审批节点
     *
     * @param code  流程编码
     * @param appId 应用ID（null=查询平台级）
     * @return 审批节点列表
     */
    private List<ApprovalNodeDto> getApprovalNodesByCodeAndAppId(String code, Long appId) {
        ApprovalFlow flow = flowMapper.selectByCodeAndAppId(code, appId);
        if (flow == null) {
            log.warn("Approval flow not found: code={}, appId={}", code, appId);
            return Collections.emptyList();
        }

        String nodesJson = flow.getNodes();
        if (nodesJson == null || nodesJson.trim().isEmpty() || "[]".equals(nodesJson.trim())) {
            log.debug("Approval flow nodes config is empty: code={}, appId={}", code, appId);
            return Collections.emptyList();
        }

        List<ApprovalNodeDto> nodes = approvalEngine.parseNodes(nodesJson);
        if (nodes == null || nodes.isEmpty()) {
            log.debug("Approval flow nodes parse result is empty: code={}, appId={}", code, appId);
            return Collections.emptyList();
        }

        return nodes;
    }
}
