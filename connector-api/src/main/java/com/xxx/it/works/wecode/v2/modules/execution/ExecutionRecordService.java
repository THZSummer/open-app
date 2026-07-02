package com.xxx.it.works.wecode.v2.modules.execution;

import com.xxx.it.works.wecode.v2.modules.execution.entity.ExecutionRecordEntity;
import com.xxx.it.works.wecode.v2.modules.execution.repository.ExecutionRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

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
 * @version 2.0.0
 */
@Service
public class ExecutionRecordService {

    private static final Logger log = LoggerFactory.getLogger(ExecutionRecordService.class);

    /** 初始状态：pending */
    private static final int STATUS_PENDING = 2;

    private final ExecutionRecordRepository repository;

    public ExecutionRecordService(ExecutionRecordRepository repository) {
        this.repository = repository;
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
                            Long appId, Integer triggerType,
                            String flowNameCn, String flowNameEn) {
        LocalDateTime now = LocalDateTime.now();

        ExecutionRecordEntity record = new ExecutionRecordEntity();
        record.setId(recordId);
        record.setFlowId(flowId);
        record.setFlowVersionId(flowVersionId);
        record.setAppId(appId);
        record.setTriggerType(triggerType);
        record.setStatus(STATUS_PENDING);
        record.setTriggerTime(now);
        record.setCreateTime(now);
        record.setLastUpdateTime(now);
        record.setFlowNameCn(flowNameCn != null ? flowNameCn : "");
        record.setFlowNameEn(flowNameEn != null ? flowNameEn : "");
        record.setRateLimitStatus(0);
        record.setCacheStatus(0);
        record.setCreateBy("SYSTEM");
        record.setLastUpdateBy("SYSTEM");

        repository.save(record)
                .doOnSuccess(r -> log.debug("Execution record created: recordId={}, flowId={}, triggerType={}",
                        recordId, flowId, triggerType))
                .doOnError(e -> log.error("Failed to create execution record: recordId={}, flowId={}, error={}",
                        recordId, flowId, e.getMessage(), e))
                .subscribe();
        return recordId;
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
        LocalDateTime now = LocalDateTime.now();

        repository.updateStatus(recordId, status, durationMs, errorCode, errorMsg, now)
                .doOnSuccess(count -> log.debug("Execution record updated: recordId={}, status={}, durationMs={}",
                        recordId, status, durationMs))
                .doOnError(e -> log.error("Failed to update execution record: recordId={}, error={}",
                        recordId, e.getMessage(), e))
                .subscribe();
    }

    /**
     * 补充流元数据（在 loadFlowVersion 之后调用，更新 flowNameCn/flowNameEn/appId 等字段）
     *
     * @param recordId            执行记录ID
     * @param flowVersionId       流版本ID
     * @param flowVersionNumber   流版本号
     * @param appId               应用ID
     * @param flowNameCn          流中文名
     * @param flowNameEn          流英文名
     * @param flowVersionSnapshot 流版本编排快照
     */
    public void updateFlowMeta(Long recordId, Long flowVersionId, Integer flowVersionNumber,
                               Long appId, String flowNameCn, String flowNameEn,
                               String flowVersionSnapshot) {
        repository.updateFlowMeta(recordId, flowVersionId, flowVersionNumber, appId,
                        flowNameCn, flowNameEn, flowVersionSnapshot)
                .doOnError(e -> log.error("Failed to update flow meta: recordId={}, error={}",
                        recordId, e.getMessage(), e))
                .subscribe();
    }

    /**
     * 检查并执行 FIFO 清理（单流记录数超过上限时删除最早记录）
     *
     * @param flowId     连接流ID
     * @param limit      记录条数上限
     */
    public void checkAndCleanFifo(Long flowId, int limit) {
        repository.countByFlowId(flowId)
                .filter(count -> count > limit)
                .flatMap(count -> {
                    int excess = (int) (count - limit);
                    return repository.deleteOldestByFlowId(flowId, excess);
                })
                .doOnNext(deleted -> log.info("FIFO cleanup executed: flowId={}, deleted={}, remaining={}",
                        flowId, deleted, limit))
                .doOnError(e -> log.error("FIFO cleanup failed: flowId={}, error={}", flowId, e.getMessage(), e))
                .subscribe();
    }
}
