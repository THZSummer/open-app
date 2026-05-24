package com.xxx.it.works.wecode.v2.modules.connector.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

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
}