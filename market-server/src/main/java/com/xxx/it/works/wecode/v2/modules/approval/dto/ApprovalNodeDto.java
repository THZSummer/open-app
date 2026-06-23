package com.xxx.it.works.wecode.v2.modules.approval.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "审批节点")
public class ApprovalNodeDto implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "节点类型")
    private String type;

    @Schema(description = "审批人ID")
    private String userId;

    @Schema(description = "审批人姓名")
    private String userName;

    @Schema(description = "节点顺序")
    private Integer order;

    @Schema(description = "审批层级")
    private String level;
}
