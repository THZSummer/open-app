package com.xxx.it.works.wecode.v2.modules.trigger.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxx.it.works.wecode.v2.modules.flow.entity.FlowVersionEntity;
import com.xxx.it.works.wecode.v2.modules.flow.repository.OpFlowVersionReadRepository;
import com.xxx.it.works.wecode.v2.modules.runtime.context.ExecutionContext;
import com.xxx.it.works.wecode.v2.modules.runtime.context.NodeContext;
import com.xxx.it.works.wecode.v2.modules.runtime.executor.ReactiveSequentialExecutor;
import com.xxx.it.works.wecode.v2.modules.runtime.model.ExecutionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * HTTP 触发服务 (v5.5)
 * <p>
 * 处理外部系统的 HTTP 触发请求.
 * v5.5 变更:
 * <ul>
 *   <li>从 {@code config.nodes[].data.*} (React Flow 格式) 读取触发配置</li>
 *   <li>使用 {@code data.authConfig} 提取凭证类型声明</li>
 *   <li>使用 {@code data.rateLimitConfig.maxQps} 限流校验</li>
 *   <li>使用 {@code data.inputContract} JSON Schema 校验请求体</li>
 *   <li>触发数据存入 {@code NodeContext.input} 分区, 上游节点可通过 {@code ${$.node.node_trigger.input.xxx}} 引用</li>
 * </ul>
 * </p>
 */
@Service
public class OpTriggerService {

    private static final Logger log = LoggerFactory.getLogger(OpTriggerService.class);

    private static final String TRIGGER_NODE_ID = "node_trigger";

    private final ObjectMapper objectMapper;
    private final ReactiveSequentialExecutor executor;
    private final OpFlowVersionReadRepository flowVersionReadRepository;

    public OpTriggerService(
            ObjectMapper objectMapper,
            ReactiveSequentialExecutor executor,
            OpFlowVersionReadRepository flowVersionReadRepository) {
        this.objectMapper = objectMapper;
        this.executor = executor;
        this.flowVersionReadRepository = flowVersionReadRepository;
    }

    /**
     * 执行 HTTP 触发
     * <p>
     * 1. 查询 flow 版本配置<br>
     * 2. 解析 React Flow 格式编排配置, 查找 trigger/entry 节点<br>
     * 3. 校验 {@code data.authConfig.type} 认证声明<br>
     * 4. 校验 {@code data.rateLimitConfig.maxQps} 限流<br>
     * 5. 校验 {@code data.inputContract} 请求体 Schema<br>
     * 6. 构建 ExecutionContext (含 NodeContext 触发节点)<br>
     * 7. 执行连接流并返回结果<br>
     * </p>
     */
    @SuppressWarnings("unchecked")
    public Mono<ExecutionResult> invokeFlow(Long flowId, Map<String, Object> triggerData,
                                             Map<String, String> headers, String requestBody) {
        String executionId = UUID.randomUUID().toString().replace("-", "");

        return flowVersionReadRepository.findByFlowId(flowId)
                .switchIfEmpty(Mono.error(new RuntimeException("Flow not found: " + flowId)))
                .flatMap(flowVersion -> {
                    // 1. 解析编排配置 (React Flow 格式)
                    Map<String, Object> config = flowVersion.parseOrchestrationConfigAsMap(objectMapper);
                    List<Map<String, Object>> nodes = (List<Map<String, Object>>) config.get("nodes");

                    if (nodes == null || nodes.isEmpty()) {
                        return Mono.error(new RuntimeException("Orchestration config has no nodes"));
                    }

                    // 2. 查找 trigger/entry 节点 (兼容两种 type 命名)
                    Map<String, Object> triggerNode = findTriggerNode(nodes);
                    if (triggerNode == null) {
                        return Mono.error(new RuntimeException("No trigger/entry node found in orchestration config"));
                    }

                    // 3. 获取 data 字段 (React Flow: node.data.xxx)
                    Map<String, Object> nodeData = (Map<String, Object>) triggerNode.get("data");
                    if (nodeData == null) {
                        return Mono.error(new RuntimeException("Trigger node has no data field"));
                    }

                    String triggerNodeId = (String) triggerNode.get("id");

                    // 4. 校验 authConfig
                    Map<String, Object> authConfig = (Map<String, Object>) nodeData.get("authConfig");
                    if (authConfig != null) {
                        String authType = (String) authConfig.get("type");
                        if ("SYSTOKEN".equals(authType) && headers != null) {
                            String token = headers.get("X-Sys-Token");
                            if (token == null || token.isEmpty()) {
                                return Mono.error(new RuntimeException("Missing X-Sys-Token for SYSTOKEN auth"));
                            }
                        }
                    }

                    // 5. 校验 rateLimitConfig
                    Map<String, Object> rateLimitConfig = (Map<String, Object>) nodeData.get("rateLimitConfig");
                    if (rateLimitConfig != null) {
                        Object maxQpsObj = rateLimitConfig.get("maxQps");
                        if (maxQpsObj instanceof Number) {
                            int maxQps = ((Number) maxQpsObj).intValue();
                            if (maxQps <= 0) {
                                return Mono.error(new RuntimeException("Rate limit maxQps must be positive"));
                            }
                            // 实际限流在 OpRateLimitFilter 中通过 Redis 滑动窗口实现
                        }
                    }

                    // 6. 校验 inputContract (基础类型检查)
                    Map<String, Object> inputContract = (Map<String, Object>) nodeData.get("inputContract");
                    if (inputContract != null && triggerData != null) {
                        Map<String, Object> bodySchema = (Map<String, Object>) inputContract.get("body");
                        if (bodySchema != null) {
                            validateInputContract(bodySchema, triggerData);
                        }
                    }

                    // 7. 构建执行上下文
                    ExecutionContext context = new ExecutionContext(executionId, String.valueOf(flowId));
                    context.setTriggerData(triggerData);
                    context.setTriggerType(1); // HTTP触发
                    context.setTest(false);

                    // 8. 建立触发节点的 NodeContext (触发数据作为 input 分区)
                    NodeContext triggerNodeCtx = new NodeContext();
                    triggerNodeCtx.setNodeId(triggerNodeId);
                    triggerNodeCtx.setNodeType("entry");
                    Map<String, Object> input = new HashMap<>();
                    if (triggerData != null) {
                        input.putAll(triggerData);
                    }
                    triggerNodeCtx.setInput(input);
                    triggerNodeCtx.setOutput(new HashMap<>());
                    triggerNodeCtx.setStatus("success");
                    context.setNodeContext(triggerNodeCtx);

                    // 9. 执行连接流
                    return executor.execute(context, flowVersion.getOrchestrationConfig());
                })
                .onErrorResume(e -> {
                    log.error("Trigger invoke failed: flowId={}, error={}", flowId, e.getMessage());
                    ExecutionResult errorResult = new ExecutionResult();
                    errorResult.setExecutionId(executionId);
                    errorResult.setFlowId(String.valueOf(flowId));
                    errorResult.setStatus("failed");
                    // v5.5: 使用结构化 errorInfo
                    Map<String, Object> errInfo = new HashMap<>();
                    errInfo.put("code", "6001");
                    errInfo.put("messageZh", "触发执行失败: " + e.getMessage());
                    errInfo.put("messageEn", "Trigger execution failed: " + e.getMessage());
                    errInfo.put("cause", e.getMessage());
                    errorResult.setErrorInfo(errInfo);
                    return Mono.just(errorResult);
                });
    }

    /**
     * 从 nodes 数组中查找 trigger/entry 节点
     * <p>
     * 优先匹配 type="trigger", 其次 type="entry", 最后 nodes[0]
     * </p>
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> findTriggerNode(List<Map<String, Object>> nodes) {
        Map<String, Object> firstEntry = null;
        for (Map<String, Object> node : nodes) {
            String type = (String) node.get("type");
            if ("trigger".equals(type)) {
                return node;
            }
            if ("entry".equals(type) && firstEntry == null) {
                firstEntry = node;
            }
        }
        return firstEntry != null ? firstEntry : (nodes.isEmpty() ? null : nodes.get(0));
    }

    /**
     * 校验输入数据符合 inputContract body schema
     * <p>
     * v5.5: 基础属性校验, 检查必需字段是否存在.
     * 完整 JSON Schema 校验由 ContractSchemaValidator 负责.
     * </p>
     */
    @SuppressWarnings("unchecked")
    private void validateInputContract(Map<String, Object> bodySchema, Map<String, Object> triggerData) {
        // 校验 required 字段
        List<String> required = (List<String>) bodySchema.get("required");
        if (required != null && !required.isEmpty()) {
            for (String field : required) {
                if (!triggerData.containsKey(field) || triggerData.get(field) == null) {
                    throw new IllegalArgumentException(
                            "Missing required field in trigger data: " + field);
                }
            }
        }

        // 校验 properties 类型
        Map<String, Object> properties = (Map<String, Object>) bodySchema.get("properties");
        if (properties != null && triggerData != null) {
            for (Map.Entry<String, Object> prop : properties.entrySet()) {
                String fieldName = prop.getKey();
                if (!triggerData.containsKey(fieldName)) continue;
                Object value = triggerData.get(fieldName);
                Map<String, Object> propSchema = (Map<String, Object>) prop.getValue();
                if (propSchema != null) {
                    String expectedType = (String) propSchema.get("type");
                    if (expectedType != null && value != null) {
                        switch (expectedType) {
                            case "string":
                                if (!(value instanceof String)) {
                                    throw new IllegalArgumentException(
                                            "Field '" + fieldName + "' must be string, got: " + value.getClass().getSimpleName());
                                }
                                break;
                            case "integer":
                            case "number":
                                if (!(value instanceof Number)) {
                                    throw new IllegalArgumentException(
                                            "Field '" + fieldName + "' must be number, got: " + value.getClass().getSimpleName());
                                }
                                break;
                            case "boolean":
                                if (!(value instanceof Boolean)) {
                                    throw new IllegalArgumentException(
                                            "Field '" + fieldName + "' must be boolean, got: " + value.getClass().getSimpleName());
                                }
                                break;
                            case "object":
                                if (!(value instanceof Map)) {
                                    throw new IllegalArgumentException(
                                            "Field '" + fieldName + "' must be object, got: " + value.getClass().getSimpleName());
                                }
                                break;
                            default:
                                break;
                        }
                    }
                }
            }
        }
    }
}