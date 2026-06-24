package com.xxx.it.works.wecode.v2.modules.flow.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 连接流版本发布响应
 * <p>
 * API #32: POST /service/open/v2/flows/{flowId}/versions/{versionId}/publish
 * 发布后进入待审批状态
 * </p>
 */
@Data
@Builder
public class FlowPublishResponse {

    /** 版本ID（String 格式） */
    private String versionId;

    /** 版本号 */
    private Integer versionNumber;

    /** 发布后状态（2=待审批） */
    private Integer status;

    /** 提交审批时间 */
    private String submittedTime;

    /** 提示信息 */
    private String message;
}
