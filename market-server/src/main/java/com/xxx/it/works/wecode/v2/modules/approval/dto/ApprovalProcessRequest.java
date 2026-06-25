package com.xxx.it.works.wecode.v2.modules.approval.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "审批操作请求")
public class ApprovalProcessRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "审批记录ID不能为空")
    @Schema(description = "审批记录ID（String类型，Service层转Long）", requiredMode = Schema.RequiredMode.REQUIRED)
    private String id;

    @NotNull(message = "操作类型不能为空")
    @Schema(description = "操作类型：0=通过, 1=驳回", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer action;
}
