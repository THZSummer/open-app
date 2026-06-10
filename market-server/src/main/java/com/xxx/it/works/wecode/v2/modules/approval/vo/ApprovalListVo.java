package com.xxx.it.works.wecode.v2.modules.approval.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@Schema(description = "审批列表响应")
public class ApprovalListVo implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "审批记录ID")
    private Long id;

    @Schema(description = "业务类型")
    private String businessType;

    @Schema(description = "业务ID")
    private String businessId;

    @Schema(description = "应用ID")
    private String appId;

    @Schema(description = "应用中文名称")
    private String appNameCn;

    @Schema(description = "应用英文名称")
    private String appNameEn;

    @Schema(description = "版本号")
    private String versionNo;

    @Schema(description = "能力名称")
    private String capabilityNames;

    @Schema(description = "申请人ID")
    private String applicantId;

    @Schema(description = "审批状态")
    private Integer status;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date createTime;
}
