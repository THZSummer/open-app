package com.xxx.it.works.wecode.v2.modules.approval.engine;

import com.xxx.it.works.wecode.v2.common.context.UserContextHolder;
import com.xxx.it.works.wecode.v2.common.exception.BusinessException;
import com.xxx.it.works.wecode.v2.common.id.IdGeneratorStrategy;
import com.xxx.it.works.wecode.v2.modules.approval.constant.ApprovalConstants;
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

/**
 * 审批引擎
 *
 * <p>平台级审批引擎：校验状态后直接通过/驳回，触发业务回调。无需多节点流转。</p>
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

        // 3. Get handler
        ApprovalHandler handler = handlerFactory.getHandler(record.getBusinessType());
        if (handler == null) {
            throw new BusinessException("40004",
                    "不支持的业务类型：" + record.getBusinessType(),
                    "Unsupported business type: " + record.getBusinessType());
        }

        // 4. Insert log
        insertLog(record, action);

        // 5. Process by action (platform-level: single approval, no node flow)
        Date now = new Date();
        if (action == ApprovalConstants.ACTION_APPROVE) {
            record.setStatus(ApprovalConstants.STATUS_APPROVED);
            record.setCompletedAt(now);
            record.setLastUpdateTime(now);
            record.setLastUpdateBy(UserContextHolder.getUserId());
            recordMapper.update(record);
            handler.onApproved(record);
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
     * 插入审批操作日志（平台级审批，level 固定为 global）
     */
    private void insertLog(ApprovalRecord record, int action) {
        ApprovalLog approvalLog = new ApprovalLog();
        approvalLog.setId(idGenerator.nextId());
        approvalLog.setRecordId(record.getId());
        approvalLog.setNodeIndex(record.getCurrentNode());
        approvalLog.setLevel("global");
        approvalLog.setOperatorId(UserContextHolder.getUserId());
        approvalLog.setOperatorName(UserContextHolder.getUserName());
        approvalLog.setAction(action);
        approvalLog.setCreateTime(new Date());
        logMapper.insert(approvalLog);
    }
}
