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
     * 按新/旧字段名双名字段提取泛型方法 (DRY 模板方法)
     *
     * @param mapper   ObjectMapper
     * @param newField v5.5 新字段名 (如 "authConfig")
     * @param oldField 旧字段名 (如 "authTypeSchema")
     * @return 提取到的 Map, 无匹配时返回空 Map
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> getConfigField(ObjectMapper mapper, String newField, String oldField) {
        Map<String, Object> config = parseConnectionConfig(mapper);
        Object val = config.get(newField);
        if (val instanceof Map) {
            return (Map<String, Object>) val;
        }
        // 向后兼容: 尝试旧字段名
        Object legacy = config.get(oldField);
        if (legacy instanceof Map) {
            return (Map<String, Object>) legacy;
        }
        return Map.of();
    }

    /**
     * 获取 authConfig (v5.5 字段, 对应旧 authTypeSchema)
     */
    public Map<String, Object> getAuthConfig(ObjectMapper mapper) {
        return getConfigField(mapper, "authConfig", "authTypeSchema");
    }

    /**
     * 获取 inputContract (v5.5 字段, 对应旧 inputSchema)
     */
    public Map<String, Object> getInputContract(ObjectMapper mapper) {
        return getConfigField(mapper, "inputContract", "inputSchema");
    }

    /**
     * 获取 outputContract (v5.5 字段, 对应旧 outputSchema)
     */
    public Map<String, Object> getOutputContract(ObjectMapper mapper) {
        return getConfigField(mapper, "outputContract", "outputSchema");
    }

    /**
     * 获取 rateLimitConfig (v5.5 字段, 对应旧 rateLimit)
     */
    public Map<String, Object> getRateLimitConfig(ObjectMapper mapper) {
        return getConfigField(mapper, "rateLimitConfig", "rateLimit");
    }
}