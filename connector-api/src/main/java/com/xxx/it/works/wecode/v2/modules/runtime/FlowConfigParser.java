package com.xxx.it.works.wecode.v2.modules.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxx.it.works.wecode.v2.modules.runtime.model.FlowConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 连接流运行时配置解析器
 * <p>
 * 从编排配置的 flowConfig 字段解析超时/限流/缓存等运行时行为配置.
 * 解析失败时返回默认值 (无超时/无限流/无缓存) 并记录告警.
 * </p>
 */
@Slf4j
@Component
public class FlowConfigParser {

    private final ObjectMapper objectMapper;

    public FlowConfigParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 解析 flowConfig JSON 字符串
     *
     * @param flowConfigJson flowConfig JSON 字符串 (可为 null)
     * @return FlowConfig 解析结果, 解析失败返回默认值
     */
    public FlowConfig parseFlowConfig(String flowConfigJson) {
        if (flowConfigJson == null || flowConfigJson.isBlank()) {
            log.debug("flowConfig is empty, using defaults");
            return FlowConfig.defaults();
        }

        try {
            JsonNode root = objectMapper.readTree(flowConfigJson);

            Integer timeoutMs = getIntOrNull(root, "timeoutMs");

            // rate limit: nested under rateLimitConfig
            Integer maxQps = null;
            Integer maxConcurrency = null;
            JsonNode rateLimitConfig = root.get("rateLimitConfig");
            if (rateLimitConfig != null) {
                maxQps = getIntOrNull(rateLimitConfig, "maxQps");
                maxConcurrency = getIntOrNull(rateLimitConfig, "maxConcurrency");
            }

            Integer cacheTtl = getIntOrNull(root, "cacheTtl");
            String cacheKeyTemplate = root.has("cacheKeyTemplate")
                    ? root.get("cacheKeyTemplate").asText() : null;

            return new FlowConfig(timeoutMs, maxQps, maxConcurrency,
                    cacheTtl, cacheKeyTemplate);
        } catch (Exception e) {
            log.warn("Failed to parse flowConfig, using defaults: {}", e.getMessage());
            return FlowConfig.defaults();
        }
    }

    /**
     * 从 JsonNode 读取整数, 不存在或非数字时返回 null
     */
    private Integer getIntOrNull(JsonNode root, String field) {
        JsonNode node = root.get(field);
        if (node != null && node.isNumber()) {
            return node.asInt();
        }
        return null;
    }
}
