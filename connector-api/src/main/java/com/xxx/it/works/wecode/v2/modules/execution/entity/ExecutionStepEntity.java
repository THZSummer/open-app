package com.xxx.it.works.wecode.v2.modules.execution.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

/**
 * 执行步骤详情 R2DBC Entity
 *
 * <p>对应表 openplatform_v2_cp_execution_step_t</p>
 * <p>connector-api 运行时写入侧使用，与 open-server 查询侧共享表结构</p>
 *
 * @author SDDU Build Agent
 * @version 2.0.0
 */
@Table("openplatform_v2_cp_execution_step_t")
public class ExecutionStepEntity implements Persistable<Long> {

    @Id
    @Column("id")
    private Long id;

    @Column("execution_id")
    private Long executionId;

    @Column("node_id")
    private String nodeId;

    @Column("node_type")
    private Integer nodeType;

    @Column("node_label_cn")
    private String nodeLabelCn;

    @Column("status")
    private Integer status;

    @Column("duration_ms")
    private Integer durationMs;

    @Column("error_code")
    private String errorCode;

    @Column("error_message")
    private String errorMessage;

    @Column("input_data")
    private String inputData;

    @Column("output_data")
    private String outputData;

    @Column("create_time")
    private LocalDateTime createTime;

    @Column("last_update_time")
    private LocalDateTime lastUpdateTime;

    /** 标记是否为新建实体（Persistable），true=INSERT, false=UPDATE */
    @Transient
    private boolean isNew = true;

    public ExecutionStepEntity() {}

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

    public Long getExecutionId() { return executionId; }
    public void setExecutionId(Long executionId) { this.executionId = executionId; }

    public String getNodeId() { return nodeId; }
    public void setNodeId(String nodeId) { this.nodeId = nodeId; }

    public Integer getNodeType() { return nodeType; }
    public void setNodeType(Integer nodeType) { this.nodeType = nodeType; }

    public String getNodeLabelCn() { return nodeLabelCn; }
    public void setNodeLabelCn(String nodeLabelCn) { this.nodeLabelCn = nodeLabelCn; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public Integer getDurationMs() { return durationMs; }
    public void setDurationMs(Integer durationMs) { this.durationMs = durationMs; }

    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public String getInputData() { return inputData; }
    public void setInputData(String inputData) { this.inputData = inputData; }

    public String getOutputData() { return outputData; }
    public void setOutputData(String outputData) { this.outputData = outputData; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getLastUpdateTime() { return lastUpdateTime; }
    public void setLastUpdateTime(LocalDateTime lastUpdateTime) { this.lastUpdateTime = lastUpdateTime; }
}
