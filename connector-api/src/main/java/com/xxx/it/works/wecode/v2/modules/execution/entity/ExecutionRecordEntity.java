package com.xxx.it.works.wecode.v2.modules.execution.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

/**
 * 执行记录 R2DBC Entity
 *
 * <p>对应表 openplatform_v2_cp_execution_record_t</p>
 * <p>connector-api 运行时写入侧使用，与 open-server 查询侧共享表结构</p>
 *
 * @author SDDU Build Agent
 * @version 2.0.0
 */
@Table("openplatform_v2_cp_execution_record_t")
public class ExecutionRecordEntity implements Persistable<Long> {

    @Id
    @Column("id")
    private Long id;

    @Column("flow_id")
    private Long flowId;

    @Column("flow_version_id")
    private Long flowVersionId;

    @Column("app_id")
    private Long appId;

    @Column("trigger_type")
    private Integer triggerType;

    @Column("status")
    private Integer status;

    @Column("trigger_time")
    private LocalDateTime triggerTime;

    @Column("duration_ms")
    private Integer durationMs;

    @Column("error_code")
    private String errorCode;

    @Column("error_message")
    private String errorMessage;

    @Column("create_time")
    private LocalDateTime createTime;

    @Column("last_update_time")
    private LocalDateTime lastUpdateTime;

    /** 标记是否为新建实体（Persistable），true=INSERT, false=UPDATE */
    @Transient
    private boolean isNew = true;

    public ExecutionRecordEntity() {}

    // ===== Persistable =====

    @Override
    public boolean isNew() {
        return isNew;
    }

    public void markNotNew() {
        this.isNew = false;
    }

    // ===== Getters & Setters =====

    @Override
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getFlowId() { return flowId; }
    public void setFlowId(Long flowId) { this.flowId = flowId; }

    public Long getFlowVersionId() { return flowVersionId; }
    public void setFlowVersionId(Long flowVersionId) { this.flowVersionId = flowVersionId; }

    public Long getAppId() { return appId; }
    public void setAppId(Long appId) { this.appId = appId; }

    public Integer getTriggerType() { return triggerType; }
    public void setTriggerType(Integer triggerType) { this.triggerType = triggerType; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public LocalDateTime getTriggerTime() { return triggerTime; }
    public void setTriggerTime(LocalDateTime triggerTime) { this.triggerTime = triggerTime; }

    public Integer getDurationMs() { return durationMs; }
    public void setDurationMs(Integer durationMs) { this.durationMs = durationMs; }

    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getLastUpdateTime() { return lastUpdateTime; }
    public void setLastUpdateTime(LocalDateTime lastUpdateTime) { this.lastUpdateTime = lastUpdateTime; }
}
