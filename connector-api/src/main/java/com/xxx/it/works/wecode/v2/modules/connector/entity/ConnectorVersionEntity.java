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

    /** V3: 版本号 (1~1000) */
    @Column("version_number")
    private Integer versionNumber;

    /** V3: 版本状态 (1=draft, 2=published, 3=invalidated, 4=deleted) */
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

    public ConnectorVersionEntity() {}

    // ===== Getters & Setters =====

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getConnectorId() { return connectorId; }
    public void setConnectorId(Long connectorId) { this.connectorId = connectorId; }

    public String getConnectionConfig() { return connectionConfig; }
    public void setConnectionConfig(String connectionConfig) { this.connectionConfig = connectionConfig; }

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
     * 按字段名提取的泛型方法 (DRY 模板方法)
     *
     * @param mapper ObjectMapper
     * @param field  Schema 字段名 (如 "authConfigs")
     * @return 提取到的 Map, 无匹配时返回空 Map
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> getConfigField(ObjectMapper mapper, String field) {
        Map<String, Object> config = parseConnectionConfig(mapper);
        Object val = config.get(field);
        if (val instanceof Map) {
            return (Map<String, Object>) val;
        }
        return Map.of();
    }

    /**
     * 获取 authConfigs (Schema 字段)
     */
    public Map<String, Object> getAuthConfigs(ObjectMapper mapper) {
        return getConfigField(mapper, "authConfigs");
    }

    /**
     * 获取 input (Schema 字段)
     */
    public Map<String, Object> getInput(ObjectMapper mapper) {
        return getConfigField(mapper, "input");
    }

    /**
     * 获取 output (Schema 字段)
     */
    public Map<String, Object> getOutput(ObjectMapper mapper) {
        return getConfigField(mapper, "output");
    }

    /**
     * 获取 rateLimitConfig (Schema 字段)
     */
    public Map<String, Object> getRateLimitConfig(ObjectMapper mapper) {
        return getConfigField(mapper, "rateLimitConfig");
    }
}