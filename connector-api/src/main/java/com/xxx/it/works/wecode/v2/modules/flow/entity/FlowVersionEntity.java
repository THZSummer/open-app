package com.xxx.it.works.wecode.v2.modules.flow.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

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
}