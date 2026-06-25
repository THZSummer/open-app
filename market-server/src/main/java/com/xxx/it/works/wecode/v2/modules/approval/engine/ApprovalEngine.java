package com.xxx.it.works.wecode.v2.modules.approval.engine;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxx.it.works.wecode.v2.common.context.UserContextHolder;
import com.xxx.it.works.wecode.v2.common.exception.BusinessException;
import com.xxx.it.works.wecode.v2.common.id.IdGeneratorStrategy;
import com.xxx.it.works.wecode.v2.modules.approval.constant.ApprovalConstants;
import com.xxx.it.works.wecode.v2.modules.approval.dto.ApprovalNodeDto;
import com.xxx.it.works.wecode.v2.modules.approval.entity.ApprovalLog;
import com.xxx.it.works.wecode.v2.modules.approval.entity.ApprovalRecord;
import com.xxx.it.works.wecode.v2.modules.approval.handler.ApprovalHandler;
import com.xxx.it.works.wecode.v2.modules.approval.handler.ApprovalHandlerFactory;
import com.xxx.it.works.wecode.v2.modules.approval.mapper.ApprovalLogMapper;
import com.xxx.it.works.wecode.v2.modules.approval.mapper.ApprovalRecordMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * 审批引擎
 *
 * <p>核心审批流程处理：校验状态、匹配审批人、推进节点、触发业务回调</p>
 */
@Slf4j
@Component
public class ApprovalEngine {

    @Autowired
    private ApprovalRecordMapper recordMapper;

    @Autowired
    private ApprovalLogMapper logMapper;

    @Autowired
    private ApprovalHandlerFactory handlerFactory;

    @Autowired
    private IdGeneratorStrategy idGenerator;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 处理审批操作（通过/驳回）
     *
     * @param recordId 审批记录ID
     * @param action   操作类型（0=通过, 1=驳回）
     */
    @Transactional(rollbackFor = Exception.class)
    public void process(Long recordId, int action) {
        // 1. Query record
        ApprovalRecord record = recordMapper.selectById(recordId);
        if (record == null) {
            throw new BusinessException("40001", "审批记录不存在", "Approval record not found");
        }

        // 2. Status check
        if (record.getStatus() != ApprovalConstants.STATUS_PENDING) {
            throw new BusinessException("40002",
                    "审批记录已处理，当前状态：" + record.getStatus(),
                    "Record already processed, current status: " + record.getStatus());
        }

        // 3. Parse nodes + operator check
        List<ApprovalNodeDto> nodes = parseNodes(record.getCombinedNodes());
        ApprovalNodeDto currentNode = nodes.get(record.getCurrentNode());
        if (!currentNode.getUserId().equals(UserContextHolder.getUserId())) {
            throw new BusinessException("40003", "当前节点审批人不匹配", "Current node approver mismatch");
        }

        // 4. Get handler
        ApprovalHandler handler = handlerFactory.getHandler(record.getBusinessType());
        if (handler == null) {
            throw new BusinessException("40004",
                    "不支持的业务类型：" + record.getBusinessType(),
                    "Unsupported business type: " + record.getBusinessType());
        }

        // 5. Insert log
        insertLog(record, currentNode, action);

        // 6. Process by action
        Date now = new Date();
        if (action == ApprovalConstants.ACTION_APPROVE) {
            if (record.getCurrentNode() >= nodes.size() - 1) {
                // Last node approved → complete
                record.setStatus(ApprovalConstants.STATUS_APPROVED);
                record.setCompletedAt(now);
                record.setLastUpdateTime(now);
                record.setLastUpdateBy(UserContextHolder.getUserId());
                recordMapper.update(record);
                handler.onApproved(record);
            } else {
                // Move to next node
                record.setCurrentNode(record.getCurrentNode() + 1);
                record.setLastUpdateTime(now);
                record.setLastUpdateBy(UserContextHolder.getUserId());
                recordMapper.update(record);
            }
        } else if (action == ApprovalConstants.ACTION_REJECT) {
            record.setStatus(ApprovalConstants.STATUS_REJECTED);
            record.setCompletedAt(now);
            record.setLastUpdateTime(now);
            record.setLastUpdateBy(UserContextHolder.getUserId());
            recordMapper.update(record);
            handler.onRejected(record);
        } else {
            throw new BusinessException("40005", "无效的操作类型：" + action, "Invalid action: " + action);
        }
    }

    /**
     * 解析 JSON 格式的审批节点列表
     */
    private List<ApprovalNodeDto> parseNodes(String combinedNodes) {
        try {
            return objectMapper.readValue(combinedNodes, new TypeReference<List<ApprovalNodeDto>>() {});
        } catch (Exception e) {
            log.error("Failed to parse approval nodes JSON: {}", combinedNodes, e);
            throw new BusinessException("40006", "审批节点数据解析失败", "Failed to parse approval nodes");
        }
    }

    /**
     * 插入审批操作日志
     */
    private void insertLog(ApprovalRecord record, ApprovalNodeDto node, int action) {
        ApprovalLog approvalLog = new ApprovalLog();
        approvalLog.setId(idGenerator.nextId());
        approvalLog.setRecordId(record.getId());
        approvalLog.setNodeIndex(record.getCurrentNode());
        approvalLog.setLevel(node.getLevel());
        approvalLog.setOperatorId(UserContextHolder.getUserId());
        approvalLog.setOperatorName(UserContextHolder.getUserName());
        approvalLog.setAction(action);
        approvalLog.setCreateTime(new Date());
        logMapper.insert(approvalLog);
    }
}
