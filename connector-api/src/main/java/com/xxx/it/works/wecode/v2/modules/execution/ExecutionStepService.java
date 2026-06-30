package com.xxx.it.works.wecode.v2.modules.execution;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxx.it.works.wecode.v2.common.config.ConnectorApiPropertyService;
import com.xxx.it.works.wecode.v2.modules.execution.entity.ExecutionStepEntity;
import com.xxx.it.works.wecode.v2.modules.execution.repository.ExecutionStepRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 步骤日志服务（connector-api 运行时写入侧）
 *
 * <p>负责 DAG 各节点执行步骤的写入。
 * 每个节点执行完成后调用 logStep() 写入步骤日志，
 * 或批量调用 logStepsBatch() 一次性写入所有步骤。</p>
 *
 * <p>写入失败不影响业务响应（异常吞掉仅记录日志）</p>
 *
 * @author SDDU Build Agent
 * @version 2.1.0
 */
@Service
public class ExecutionStepService {

    private static final Logger log = LoggerFactory.getLogger(ExecutionStepService.class);

    private final ExecutionStepRepository repository;
    private final LogSanitizer logSanitizer;
    private final ObjectMapper objectMapper;
    private final ConnectorApiPropertyService propertyService;

    public ExecutionStepService(ExecutionStepRepository repository,
                                LogSanitizer logSanitizer,
                                ObjectMapper objectMapper,
                                ConnectorApiPropertyService propertyService) {
        this.repository = repository;
        this.logSanitizer = logSanitizer;
        this.objectMapper = objectMapper;
        this.propertyService = propertyService;
    }

    /**
     * 写入单条步骤日志
     *
     * @param stepId      步骤ID（雪花ID）
     * @param executionId 执行记录ID
     * @param nodeId      节点ID（编排配置中的节点标识）
     * @param nodeType    节点类型：1=触发器, 2=连接器, 3=脚本, 4=并行处理, 5=出口
     * @param nodeName    节点名称（中文标签）
     * @param status      执行状态：0=成功, 1=失败
     * @param input       输入数据快照（JSON Map）
     * @param output      输出数据快照（JSON Map）
     * @param error       错误信息
     * @param durationMs  耗时（毫秒）
     */
    public void logStep(Long stepId, Long executionId, String nodeId, Integer nodeType,
                        String nodeName, Integer status,
                        Map<String, Object> input, Map<String, Object> output,
                        String error, Integer durationMs) {
        // 检查日志采集开关 (#14: log_collection_enabled)
        if (!propertyService.isLogCollectionEnabled().blockOptional().orElse(true)) {
            log.warn("Step log skipped (logCollectionEnabled=false): executionId={}, nodeId={}, status={}, durationMs={}",
                    executionId, nodeId, status, durationMs);
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        ExecutionStepEntity step = new ExecutionStepEntity();
        step.setId(stepId);
        step.setExecutionId(executionId);
        step.setNodeId(nodeId);
        step.setNodeType(nodeType);
        step.setNodeLabelCn(nodeName);
        step.setStatus(status);
        step.setDurationMs(durationMs);
        step.setErrorCode(status != null && status == 1 ? "NODE_ERROR" : null);
        step.setErrorMessage(error);

        if (input != null) {
            Map<String, Object> sanitizedInput = logSanitizer.sanitize(input);
            step.setInputData(toJson(sanitizedInput));
        }
        if (output != null) {
            Map<String, Object> sanitizedOutput = logSanitizer.sanitize(output);
            step.setOutputData(toJson(sanitizedOutput));
        }

        step.setCreateTime(now);
        step.setLastUpdateTime(now);

        repository.save(step)
                .doOnSuccess(s -> log.debug("Execution step logged: stepId={}, executionId={}, nodeId={}, status={}, durationMs={}",
                        stepId, executionId, nodeId, status, durationMs))
                .doOnError(e -> log.error("Failed to log execution step: executionId={}, nodeId={}, error={}",
                        executionId, nodeId, e.getMessage(), e))
                .subscribe();
    }

    /**
     * 批量写入步骤日志
     *
     * @param executionId 执行记录ID
     * @param stepLogs    步骤日志列表
     */
    public void logStepsBatch(Long executionId, List<StepLog> stepLogs) {
        if (stepLogs == null || stepLogs.isEmpty()) {
            return;
        }

        // 检查日志采集开关 (#14: log_collection_enabled)
        if (!propertyService.isLogCollectionEnabled().blockOptional().orElse(true)) {
            log.warn("Step logs batch skipped (logCollectionEnabled=false): executionId={}, count={}",
                    executionId, stepLogs.size());
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        List<ExecutionStepEntity> steps = new ArrayList<>();

        for (StepLog sl : stepLogs) {
            ExecutionStepEntity step = new ExecutionStepEntity();
            step.setId(sl.stepId);
            step.setExecutionId(executionId);
            step.setNodeId(sl.nodeId);
            step.setNodeType(sl.nodeType);
            step.setNodeLabelCn(sl.nodeName);
            step.setStatus(sl.status);
            step.setDurationMs(sl.durationMs);
            step.setErrorCode(sl.status != null && sl.status == 1 ? "NODE_ERROR" : null);
            step.setErrorMessage(sl.error);

            if (sl.input != null) {
                Map<String, Object> sanitizedInput = logSanitizer.sanitize(sl.input);
                step.setInputData(toJson(sanitizedInput));
            }
            if (sl.output != null) {
                Map<String, Object> sanitizedOutput = logSanitizer.sanitize(sl.output);
                step.setOutputData(toJson(sanitizedOutput));
            }

            step.setCreateTime(now);
            step.setLastUpdateTime(now);
            steps.add(step);
        }

        repository.saveAll(steps)
                .doOnComplete(() -> log.debug("Execution steps batch logged: executionId={}, count={}",
                        executionId, steps.size()))
                .doOnError(e -> log.error("Failed to batch log execution steps: executionId={}, error={}",
                        executionId, e.getMessage(), e))
                .subscribe();
    }

    /**
     * Map 转 JSON 字符串
     */
    private String toJson(Map<String, Object> data) {
        if (data == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize step data to JSON: {}", e.getMessage());
            return "{}";
        }
    }

    /**
     * 步骤日志数据对象
     */
    public static class StepLog {
        public Long stepId;
        public String nodeId;
        public Integer nodeType;
        public String nodeName;
        public Integer status;
        public Map<String, Object> input;
        public Map<String, Object> output;
        public String error;
        public Integer durationMs;
    }
}
