package com.xxx.it.works.wecode.v2.modules.approval.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "审批列表查询请求")
public class ApprovalListRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "当前页码", defaultValue = "1")
    private Integer curPage = 1;

    @Schema(description = "每页条数", defaultValue = "10")
    private Integer pageSize = 10;

    @Schema(description = "偏移量（由Service层计算）", hidden = true)
    private Integer offset;
}
