package com.xxx.it.works.wecode.v2.modules.flow.entity;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 连接流版本/配置 R2DBC Entity
 * <p>
 * 对应表: openplatform_v2_cp_flow_version_t
 * MVP 单版本模型: 每 flow 仅一条记录, 编辑即生效
 * 编排配置 v5.5: {node:{id,type,position:{x,y},data:{labelCn,labelEn,authConfig,inputContract,rateLimitConfig,...}},
 *                 edges:[{id,source,target,data:{businessType}}]}
 * 旧格式兼容: 通过 @JsonAlias 和运行时 getEdgeField() 双字段名 fallback
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

    /** V3: 版本号 (1~1000) */
    @Column("version_number")
    private Integer versionNumber;

    /** V3: 版本状态 (1=draft, 2=pending_approval, 3=withdrawn, 4=rejected, 5=published, 6=invalidated, 7=deleted) */
    @Column("status")
    private Integer status;

    /** V3: 发布时间 */
    @Column("published_time")
    private LocalDateTime publishedTime;

    /** V3: 发布人 */
    @Column("published_by")
    private String publishedBy;

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

    public Integer getVersionNumber() { return versionNumber; }
    public void setVersionNumber(Integer versionNumber) { this.versionNumber = versionNumber; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public LocalDateTime getPublishedTime() { return publishedTime; }
    public void setPublishedTime(LocalDateTime publishedTime) { this.publishedTime = publishedTime; }

    public String getPublishedBy() { return publishedBy; }
    public void setPublishedBy(String publishedBy) { this.publishedBy = publishedBy; }

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
     * 获取 trigger 节点配置; 遍历 nodes[] 查找 type='trigger' 的节点
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getTriggerConfig(ObjectMapper mapper) {
        Map<String, Object> config = parseOrchestrationConfigAsMap(mapper);
        List<Map<String, Object>> nodes = (List<Map<String, Object>>) config.get("nodes");
        if (nodes != null) {
            for (Map<String, Object> node : nodes) {
                if ("trigger".equals(node.get("type"))) {
                    return node;
                }
            }
        }
        return Map.of();
    }
}