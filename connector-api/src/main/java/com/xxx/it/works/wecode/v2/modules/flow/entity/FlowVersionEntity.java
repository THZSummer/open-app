package com.xxx.it.works.wecode.v2.modules.flow.entity;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 连接流版本/配置 R2DBC Entity
 * <p>
 * 对应表: openplatform_v2_cp_flow_version_t
 * MVP 单版本模型: 每 flow 仅一条记录, 编辑即生效
 * 编排配置: {trigger:{authTypeSchema,inputSchema,rateLimit},
 *            nodes:[{id,type,labelCn,labelEn,position,...}],
 *            edges:[{id,sourceNodeId,targetNodeId}]}
 * </p>
 */
@Table("openplatform_v2_cp_flow_version_t")
public class FlowVersionEntity {

    @Id
    @Column("id")
    private Long id;

    @Column("flow_id")
    private Long flowId;

    @Column("orchestration_config")
    private String orchestrationConfig;

    @Column("create_time")
    private LocalDateTime createTime;

    @Column("last_update_time")
    private LocalDateTime lastUpdateTime;

    @Column("create_by")
    private String createBy;

    @Column("last_update_by")
    private String lastUpdateBy;

    public FlowVersionEntity() {}

    // ===== Getters & Setters =====

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getFlowId() { return flowId; }
    public void setFlowId(Long flowId) { this.flowId = flowId; }

    public String getOrchestrationConfig() { return orchestrationConfig; }
    public void setOrchestrationConfig(String orchestrationConfig) { this.orchestrationConfig = orchestrationConfig; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getLastUpdateTime() { return lastUpdateTime; }
    public void setLastUpdateTime(LocalDateTime lastUpdateTime) { this.lastUpdateTime = lastUpdateTime; }

    public String getCreateBy() { return createBy; }
    public void setCreateBy(String createBy) { this.createBy = createBy; }

    public String getLastUpdateBy() { return lastUpdateBy; }
    public void setLastUpdateBy(String lastUpdateBy) { this.lastUpdateBy = lastUpdateBy; }

    // ===== Transient Helpers (v5.5) =====

    /**
     * 解析 orchestrationConfig JSON 为 JsonNode
     */
    public JsonNode parseOrchestrationConfig(ObjectMapper mapper) {
        if (this.orchestrationConfig == null || this.orchestrationConfig.isBlank()) {
            return mapper.createObjectNode();
        }
        try {
            return mapper.readTree(this.orchestrationConfig);
        } catch (Exception e) {
            return mapper.createObjectNode();
        }
    }

    /**
     * 解析 orchestrationConfig JSON 为 Map
     */
    public Map<String, Object> parseOrchestrationConfigAsMap(ObjectMapper mapper) {
        if (this.orchestrationConfig == null || this.orchestrationConfig.isBlank()) {
            return Map.of();
        }
        try {
            return mapper.readValue(this.orchestrationConfig,
                    new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }

    /**
     * 获取 trigger 配置; v5.5 字段名: authConfig, inputContract, outputContract, rateLimitConfig
     */
    public Map<String, Object> getTriggerConfig(ObjectMapper mapper) {
        Map<String, Object> config = parseOrchestrationConfigAsMap(mapper);
        Object trigger = config.get("trigger");
        if (trigger instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) trigger;
            return result;
        }
        return Map.of();
    }
}