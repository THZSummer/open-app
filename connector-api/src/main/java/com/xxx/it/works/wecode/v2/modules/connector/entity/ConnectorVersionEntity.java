package com.xxx.it.works.wecode.v2.modules.connector.entity;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 连接器版本/配置 R2DBC Entity
 * <p>
 * 对应表: openplatform_v2_cp_connector_version_t
 * MVP 单版本模型: 每 connector 仅一条记录, 编辑即生效
 * 仅声明认证类型 Schema (含 sensitive:true 标记), 不存储凭证值
 * </p>
 */
@Table("openplatform_v2_cp_connector_version_t")
public class ConnectorVersionEntity {

    @Id
    @Column("id")
    private Long id;

    @Column("connector_id")
    private Long connectorId;

    @Column("connection_config")
    private String connectionConfig;

    @Column("create_time")
    private LocalDateTime createTime;

    @Column("last_update_time")
    private LocalDateTime lastUpdateTime;

    @Column("create_by")
    private String createBy;

    @Column("last_update_by")
    private String lastUpdateBy;

    public ConnectorVersionEntity() {}

    // ===== Getters & Setters =====

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getConnectorId() { return connectorId; }
    public void setConnectorId(Long connectorId) { this.connectorId = connectorId; }

    public String getConnectionConfig() { return connectionConfig; }
    public void setConnectionConfig(String connectionConfig) { this.connectionConfig = connectionConfig; }

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
     * 解析 connectionConfig JSON 为 Map; v5.5 字段名: authConfig, inputContract, outputContract, rateLimitConfig
     */
    public Map<String, Object> parseConnectionConfig(ObjectMapper mapper) {
        if (this.connectionConfig == null || this.connectionConfig.isBlank()) {
            return Map.of();
        }
        try {
            return mapper.readValue(this.connectionConfig,
                    new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }

    /**
     * 获取 authConfig (v5.5 字段, 对应旧 authTypeSchema)
     */
    public Map<String, Object> getAuthConfig(ObjectMapper mapper) {
        Map<String, Object> config = parseConnectionConfig(mapper);
        Object auth = config.get("authConfig");
        if (auth instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) auth;
            return result;
        }
        // 向后兼容: authTypeSchema
        Object legacy = config.get("authTypeSchema");
        if (legacy instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) legacy;
            return result;
        }
        return Map.of();
    }

    /**
     * 获取 inputContract (v5.5 字段, 对应旧 inputSchema)
     */
    public Map<String, Object> getInputContract(ObjectMapper mapper) {
        Map<String, Object> config = parseConnectionConfig(mapper);
        Object contract = config.get("inputContract");
        if (contract instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) contract;
            return result;
        }
        Object legacy = config.get("inputSchema");
        if (legacy instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) legacy;
            return result;
        }
        return Map.of();
    }

    /**
     * 获取 outputContract (v5.5 字段, 对应旧 outputSchema)
     */
    public Map<String, Object> getOutputContract(ObjectMapper mapper) {
        Map<String, Object> config = parseConnectionConfig(mapper);
        Object contract = config.get("outputContract");
        if (contract instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) contract;
            return result;
        }
        Object legacy = config.get("outputSchema");
        if (legacy instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) legacy;
            return result;
        }
        return Map.of();
    }

    /**
     * 获取 rateLimitConfig (v5.5 字段, 对应旧 rateLimit)
     */
    public Map<String, Object> getRateLimitConfig(ObjectMapper mapper) {
        Map<String, Object> config = parseConnectionConfig(mapper);
        Object rate = config.get("rateLimitConfig");
        if (rate instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) rate;
            return result;
        }
        Object legacy = config.get("rateLimit");
        if (legacy instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) legacy;
            return result;
        }
        return Map.of();
    }
}