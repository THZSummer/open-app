package com.xxx.it.works.wecode.v2.modules.execution;

import com.xxx.it.works.wecode.v2.modules.execution.entity.ExecutionRecord;
import com.xxx.it.works.wecode.v2.modules.execution.mapper.ExecutionRecordMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * 运行记录服务（connector-api 运行时写入侧）
 *
 * <p>负责执行记录的创建和状态更新。
 * 引擎执行开始时调用 startRecord() 创建记录（status=pending），
 * 引擎执行结束时调用 updateRecord() 更新状态和耗时。</p>
 *
 * <p>写入失败不影响业务响应（异常吞掉仅记录日志）</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Service
public class ExecutionRecordService {

    private static final Logger log = LoggerFactory.getLogger(ExecutionRecordService.class);

    /** 初始状态：pending */
    private static final int STATUS_PENDING = 2;

    private final ExecutionRecordMapper executionRecordMapper;

    public ExecutionRecordService(ExecutionRecordMapper executionRecordMapper) {
        this.executionRecordMapper = executionRecordMapper;
    }

    /**
     * 开始执行记录 — 创建 execution_record 行
     *
     * @param recordId       执行记录ID（雪花ID）
     * @param flowId         连接流ID
     * @param flowVersionId  连接流版本ID
     * @param appId          应用ID
     * @param triggerType    触发方式：1=HTTP触发, 2=调试触发
     * @return recordId
     */
    public Long startRecord(Long recordId, Long flowId, Long flowVersionId,
                            Long appId, Integer triggerType) {
        try {
            Date now = new Date();

            ExecutionRecord record = new ExecutionRecord();
            record.setId(recordId);
            record.setFlowId(flowId);
            record.setFlowVersionId(flowVersionId);
            record.setAppId(appId);
            record.setTriggerType(triggerType);
            record.setStatus(STATUS_PENDING);
            record.setTriggerTime(now);
            record.setCreateTime(now);
            record.setLastUpdateTime(now);

            executionRecordMapper.insert(record);

            log.debug("Execution record created: recordId={}, flowId={}, triggerType={}",
                    recordId, flowId, triggerType);
            return recordId;
        } catch (Exception e) {
            log.error("Failed to create execution record: recordId={}, flowId={}, error={}",
                    recordId, flowId, e.getMessage(), e);
            return recordId;
        }
    }

    /**
     * 更新执行记录状态和耗时
     *
     * @param recordId   执行记录ID
     * @param status     执行状态：0=成功, 1=失败
     * @param durationMs 总耗时（毫秒）
     * @param errorCode  错误码（失败时）
     * @param errorMsg   错误信息（失败时）
     */
    public void updateRecord(Long recordId, Integer status, Integer durationMs,
                             String errorCode, String errorMsg) {
        try {
            ExecutionRecord record = new ExecutionRecord();
            record.setId(recordId);
            record.setStatus(status);
            record.setDurationMs(durationMs);
            record.setErrorCode(errorCode);
            record.setErrorMessage(errorMsg);
            record.setLastUpdateTime(new Date());

            executionRecordMapper.update(record);

            log.debug("Execution record updated: recordId={}, status={}, durationMs={}",
                    recordId, status, durationMs);
        } catch (Exception e) {
            log.error("Failed to update execution record: recordId={}, error={}",
                    recordId, e.getMessage(), e);
        }
    }

    /**
     * 检查并执行 FIFO 清理（单流记录数超过上限时删除最早记录）
     *
     * @param flowId     连接流ID
     * @param limit      记录条数上限
     */
    public void checkAndCleanFifo(Long flowId, int limit) {
        try {
            Long count = executionRecordMapper.countByFlowId(flowId);
            if (count != null && count > limit) {
                int excess = (int) (count - limit);
                executionRecordMapper.deleteOldestByFlowId(flowId, excess);
                log.info("FIFO cleanup executed: flowId={}, deleted={}, remaining={}",
                        flowId, excess, limit);
            }
        } catch (Exception e) {
            log.error("FIFO cleanup failed: flowId={}, error={}", flowId, e.getMessage(), e);
        }
    }
}
